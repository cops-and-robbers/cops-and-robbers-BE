package com.team.cops_and_robbers.community.repository;

import static com.team.cops_and_robbers.community.domain.QCommunityPost.communityPost;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
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
    public List<CommunityPost> findPage(
            String countryCode, CommunityPostSort sort, CommunityPostCursor cursor, int size) {
        NumberExpression<Integer> closedRank = closedRank(LocalDateTime.now());

        return queryFactory
                .selectFrom(communityPost)
                .where(
                        communityPost.countryCode.eq(countryCode),
                        afterCursor(closedRank, sort, cursor)
                )
                .orderBy(orderBy(closedRank, sort))
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

    private OrderSpecifier<?>[] orderBy(NumberExpression<Integer> closedRank, CommunityPostSort sort) {
        if (sort == CommunityPostSort.DEADLINE) {
            return new OrderSpecifier<?>[]{
                    closedRank.asc(), communityPost.meetingAt.asc(), communityPost.id.asc()};
        }
        return new OrderSpecifier<?>[]{
                closedRank.asc(), communityPost.createdAt.desc(), communityPost.id.desc()};
    }

    private BooleanExpression afterCursor(
            NumberExpression<Integer> closedRank, CommunityPostSort sort, CommunityPostCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return closedRank.gt(cursor.closed())
                .or(closedRank.eq(cursor.closed()).and(afterSortKey(sort, cursor)));
    }

    private BooleanExpression afterSortKey(CommunityPostSort sort, CommunityPostCursor cursor) {
        if (sort == CommunityPostSort.DEADLINE) {
            DateTimePath<LocalDateTime> meetingAt = communityPost.meetingAt;
            return meetingAt.gt(cursor.sortAt())
                    .or(meetingAt.eq(cursor.sortAt()).and(communityPost.id.gt(cursor.id())));
        }
        DateTimePath<LocalDateTime> createdAt = communityPost.createdAt;
        return createdAt.lt(cursor.sortAt())
                .or(createdAt.eq(cursor.sortAt()).and(communityPost.id.lt(cursor.id())));
    }
}
