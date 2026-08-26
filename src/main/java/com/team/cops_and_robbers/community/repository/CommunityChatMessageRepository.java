package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.domain.CommunityChatMessage;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunityChatMessageRepository extends JpaRepository<CommunityChatMessage, Long> {

    default CommunityChatMessage getById(Long id) {
        return findById(id)
                .orElseThrow(() -> new ApplicationException(CommunityChatException.CHAT_MESSAGE_NOT_FOUND));
    }

    /**
     * 커서 페이징. cursor가 null이면 최신부터 조회한다.
     * idx_chat_messages_post_id_id에 그대로 얹히므로 테이블이 커져도 읽는 양이 늘지 않는다.
     */
    @Query("""
            select m from CommunityChatMessage m
            where m.communityPostId = :postId
              and (:cursor is null or m.id < :cursor)
            order by m.id desc
            """)
    List<CommunityChatMessage> findPageByPostId(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    /**
     * 방마다 마지막 메시지 한 건씩. id가 단조 증가하므로 max(id)가 곧 최신 메시지다.
     * 마지막 메시지를 컬럼으로 들고 있지 않은 이유는, 그러면 채팅을 보낼 때마다
     * UPDATE가 따라붙어 쓰기 부하가 두 배가 되기 때문이다.
     */
    @Query("""
            select m from CommunityChatMessage m
            where m.id in (
                select max(latest.id) from CommunityChatMessage latest
                where latest.communityPostId in :postIds
                group by latest.communityPostId
            )
            """)
    List<CommunityChatMessage> findLatestByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from CommunityChatMessage m where m.communityPostId = :postId")
    void deleteAllByCommunityPostId(@Param("postId") Long postId);
}
