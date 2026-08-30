package com.team.cops_and_robbers.community.post.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.post.domain.CommunityPost;
import com.team.cops_and_robbers.community.post.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.post.domain.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.team.cops_and_robbers.community.chat.member.domain.QCommunityChatMember.communityChatMember;
import static com.team.cops_and_robbers.community.post.domain.QCommunityPost.communityPost;
import static com.team.cops_and_robbers.community.reaction.domain.QCommunityPostLike.communityPostLike;
import static com.team.cops_and_robbers.community.reaction.domain.QCommunityPostScrap.communityPostScrap;

@RequiredArgsConstructor
public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    /**
     * 인기순 대상은 최근 N일 이내 작성글로 제한한다.
     */
    private static final long POPULAR_WINDOW_DAYS = 7;
    private static final long LIKE_WEIGHT = 1L;
    private static final long SCRAP_WEIGHT = 2L;
    private static final long CHAT_MEMBER_WEIGHT = 3L;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommunityPostRow> findPage(
            CommunityPostSearchCondition condition, CommunityPostCursor cursor, int size) {
        LocalDateTime now = LocalDateTime.now();
        NumberExpression<Integer> closedRank = closedRank(now);
        NumberExpression<Double> distance = distance(condition);
        NumberExpression<Long> score = popularityScore(condition);

        return queryFactory
                .select(Projections.constructor(CommunityPostRow.class, communityPost, distance, score))
                .from(communityPost)
                .where(
                        countryMatches(condition),
                        keywordContains(condition.keyword()),
                        withinPopularWindow(condition.sort(), now),
                        afterCursor(closedRank, distance, score, condition.sort(), cursor)
                )
                .orderBy(orderBy(closedRank, distance, score, condition.sort()))
                .limit(size + 1L)
                .fetch();
    }

    /** 웹의 영어 목록처럼 "특정 국가를 뺀 전부"가 필요한 경우에만 제외 조건으로 간다. */
    private BooleanExpression countryMatches(CommunityPostSearchCondition condition) {
        if (condition.countryCode() != null) {
            return communityPost.countryCode.eq(condition.countryCode());
        }
        return communityPost.countryCode.notIn(condition.excludeCountryCodes());
    }

    /** 화면에 보이는 건 제목과 지역·장소명 조합이라 그 셋만 훑는다. 지번 주소는 복사용이라 뺀다. */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String normalized = keyword.replaceAll("\\s+", "");
        return communityPost.title.containsIgnoreCase(normalized)
                .or(communityPost.placeName.containsIgnoreCase(normalized))
                .or(communityPost.region.containsIgnoreCase(normalized));
    }

    /** {@link CommunityPost#isClosed()}와 같은 규칙이다. */
    private NumberExpression<Integer> closedRank(LocalDateTime now) {
        return new CaseBuilder()
                .when(communityPost.status.ne(RecruitmentStatus.RECRUITING)
                        .or(communityPost.meetingAt.lt(now)))
                .then(1)
                .otherwise(0);
    }

    private NumberExpression<Double> distance(CommunityPostSearchCondition condition) {
        if (condition.sort() != CommunityPostSort.DISTANCE) {
            return Expressions.asNumber(0.0).nullif(0.0);
        }
        return Expressions.numberTemplate(Double.class,
                "st_distancesphere({0}, st_setsrid(st_makepoint({1}, {2}), 4326))",
                communityPost.location, condition.longitude(), condition.latitude());
    }

    private NumberExpression<Long> popularityScore(CommunityPostSearchCondition condition) {
        if (condition.sort() != CommunityPostSort.POPULAR) {
            return Expressions.asNumber(0L).nullif(0L);
        }
        NumberExpression<Long> likeCount = Expressions.numberTemplate(Long.class, "({0})",
                JPAExpressions.select(communityPostLike.count())
                        .from(communityPostLike)
                        .where(communityPostLike.communityPostId.eq(communityPost.id)));
        NumberExpression<Long> scrapCount = Expressions.numberTemplate(Long.class, "({0})",
                JPAExpressions.select(communityPostScrap.count())
                        .from(communityPostScrap)
                        .where(communityPostScrap.communityPostId.eq(communityPost.id)));
        NumberExpression<Long> memberCount = Expressions.numberTemplate(Long.class, "({0})",
                JPAExpressions.select(communityChatMember.count())
                        .from(communityChatMember)
                        .where(communityChatMember.communityPostId.eq(communityPost.id)));

        return likeCount.multiply(LIKE_WEIGHT)
                .add(scrapCount.multiply(SCRAP_WEIGHT))
                .add(memberCount.multiply(CHAT_MEMBER_WEIGHT));
    }

    private BooleanExpression withinPopularWindow(CommunityPostSort sort, LocalDateTime now) {
        if (sort != CommunityPostSort.POPULAR) {
            return null;
        }
        return communityPost.createdAt.goe(now.minusDays(POPULAR_WINDOW_DAYS));
    }

    private OrderSpecifier<?>[] orderBy(
            NumberExpression<Integer> closedRank,
            NumberExpression<Double> distance,
            NumberExpression<Long> score,
            CommunityPostSort sort
    ) {
        return switch (sort) {
            case DEADLINE -> new OrderSpecifier<?>[]{
                    closedRank.asc(), communityPost.meetingAt.asc(), communityPost.id.asc()};
            case DISTANCE -> new OrderSpecifier<?>[]{
                    closedRank.asc(), distance.asc(), communityPost.id.asc()};
            case POPULAR -> new OrderSpecifier<?>[]{
                    closedRank.asc(), score.desc(), communityPost.id.desc()};
            default -> new OrderSpecifier<?>[]{
                    closedRank.asc(), communityPost.createdAt.desc(), communityPost.id.desc()};
        };
    }

    private BooleanExpression afterCursor(
            NumberExpression<Integer> closedRank,
            NumberExpression<Double> distance,
            NumberExpression<Long> score,
            CommunityPostSort sort,
            CommunityPostCursor cursor
    ) {
        if (cursor == null) {
            return null;
        }
        int cursorClosedRank = cursor.isClosed() ? 1 : 0;
        return closedRank.gt(cursorClosedRank)
                .or(closedRank.eq(cursorClosedRank).and(afterSortKey(distance, score, sort, cursor)));
    }

    private BooleanExpression afterSortKey(
            NumberExpression<Double> distance, NumberExpression<Long> score, CommunityPostSort sort, CommunityPostCursor cursor) {
        return switch (sort) {
            case DEADLINE -> communityPost.meetingAt.gt(cursor.sortAt())
                    .or(communityPost.meetingAt.eq(cursor.sortAt())
                            .and(communityPost.id.gt(cursor.id())));
            case DISTANCE -> distance.gt(cursor.distance())
                    .or(distance.eq(cursor.distance()).and(communityPost.id.gt(cursor.id())));
            case POPULAR -> score.lt(cursor.score())
                    .or(score.eq(cursor.score()).and(communityPost.id.lt(cursor.id())));
            default -> communityPost.createdAt.lt(cursor.sortAt())
                    .or(communityPost.createdAt.eq(cursor.sortAt())
                            .and(communityPost.id.lt(cursor.id())));
        };
    }
}
