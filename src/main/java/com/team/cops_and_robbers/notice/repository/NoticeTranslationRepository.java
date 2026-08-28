package com.team.cops_and_robbers.notice.repository;

import com.team.cops_and_robbers.notice.domain.NoticeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NoticeTranslationRepository extends JpaRepository<NoticeTranslation, Long> {

    List<NoticeTranslation> findAllByNoticeId(Long noticeId);

    List<NoticeTranslation> findAllByNoticeIdIn(Collection<Long> noticeIds);

    // 수정 흐름에서 공지 필드 변경이 clear 로 유실되지 않게, 삭제 전에 flush 를 먼저 한다
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NoticeTranslation t where t.noticeId = :noticeId")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}
