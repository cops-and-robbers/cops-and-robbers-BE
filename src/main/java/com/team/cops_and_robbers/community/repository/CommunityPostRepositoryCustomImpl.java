package com.team.cops_and_robbers.community.repository;

import static com.team.cops_and_robbers.community.domain.QCommunityPost.communityPost;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.application.dto.CommunityPostSearchCondition;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommunityPostRow> findPage(
            CommunityPostSearchCondition condition, CommunityPostCursor cursor, int size) {
        NumberExpression<Integer> closedRank = closedRank(LocalDateTime.now());
        NumberExpression<Double> distance = distance(condition);

        return queryFactory
                .select(Projections.constructor(CommunityPostRow.class, communityPost, distance))
                .from(communityPost)
                .where(
                        communityPost.countryCode.eq(condition.countryCode()),
                        afterCursor(closedRank, distance, condition.sort(), cursor)
                )
                .orderBy(orderBy(closedRank, distance, condition.sort()))
                .limit(size + 1L)
                .fetch();
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

    private OrderSpecifier<?>[] orderBy(
            NumberExpression<Integer> closedRank, NumberExpression<Double> distance, CommunityPostSort sort) {
        return switch (sort) {
            case DEADLINE -> new OrderSpecifier<?>[]{
                    closedRank.asc(), communityPost.meetingAt.asc(), communityPost.id.asc()};
            case DISTANCE -> new OrderSpecifier<?>[]{
                    closedRank.asc(), distance.asc(), communityPost.id.asc()};
            default -> new OrderSpecifier<?>[]{
                    closedRank.asc(), communityPost.createdAt.desc(), communityPost.id.desc()};
        };
    }

    private BooleanExpression afterCursor(
            NumberExpression<Integer> closedRank,
            NumberExpression<Double> distance,
            CommunityPostSort sort,
            CommunityPostCursor cursor
    ) {
        if (cursor == null) {
            return null;
        }
        return closedRank.gt(cursor.closed())
                .or(closedRank.eq(cursor.closed()).and(afterSortKey(distance, sort, cursor)));
    }

    private BooleanExpression afterSortKey(
            NumberExpression<Double> distance, CommunityPostSort sort, CommunityPostCursor cursor) {
        return switch (sort) {
            case DEADLINE -> communityPost.meetingAt.gt(cursor.sortAt())
                    .or(communityPost.meetingAt.eq(cursor.sortAt())
                            .and(communityPost.id.gt(cursor.id())));
            case DISTANCE -> distance.gt(cursor.distance())
                    .or(distance.eq(cursor.distance()).and(communityPost.id.gt(cursor.id())));
            default -> communityPost.createdAt.lt(cursor.sortAt())
                    .or(communityPost.createdAt.eq(cursor.sortAt())
                            .and(communityPost.id.lt(cursor.id())));
        };
    }
}
