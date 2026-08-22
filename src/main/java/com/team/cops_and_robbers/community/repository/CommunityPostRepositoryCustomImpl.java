package com.team.cops_and_robbers.community.repository;

import static com.team.cops_and_robbers.community.domain.QCommunityPost.communityPost;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommunityPostRepositoryCustomImpl implements CommunityPostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CommunityPost> findPage(String countryCode, CommunityPostCursor cursor, int size) {
        NumberExpression<Integer> closedRank = closedRank(LocalDateTime.now());

        return queryFactory
                .selectFrom(communityPost)
                .where(
                        communityPost.countryCode.eq(countryCode),
                        afterCursor(closedRank, cursor)
                )
                .orderBy(closedRank.asc(), communityPost.createdAt.desc(), communityPost.id.desc())
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

    private BooleanExpression afterCursor(NumberExpression<Integer> closedRank, CommunityPostCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return closedRank.gt(cursor.closed())
                .or(closedRank.eq(cursor.closed())
                        .and(communityPost.createdAt.lt(cursor.createdAt())
                                .or(communityPost.createdAt.eq(cursor.createdAt())
                                        .and(communityPost.id.lt(cursor.id())))));
    }
}
