package com.team.cops_and_robbers.notice.repository;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    @Modifying(clearAutomatically = true)
    @Query("delete from Notice n where n.id = :noticeId")
    void deleteByNoticeId(@Param("noticeId") Long noticeId);

    default Notice getByNoticeId(Long noticeId) {
        return findById(noticeId)
                .orElseThrow(() -> new ApplicationException(NoticeException.NOTICE_NOT_FOUND));
    }
}
