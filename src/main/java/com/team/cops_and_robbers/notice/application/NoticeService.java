package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public NoticeResult createNotice(NoticeCreateCommand command) {
        Notice notice = noticeRepository.save(Notice.createNotice(command));
        return NoticeResult.from(notice);
    }

    public Page<NoticeResult> getNoticeList(NoticeListCommand command) {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(command.toPageable())
                .map(NoticeResult::from);
    }

    public NoticeResult getNotice(Long noticeId) {
        return NoticeResult.from(noticeRepository.getByNoticeId(noticeId));
    }

    @Transactional
    public NoticeResult updateNotice(NoticeUpdateCommand command) {
        Notice notice = noticeRepository.getByNoticeId(command.noticeId());
        notice.updateNotice(command);
        return NoticeResult.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.getByNoticeId(noticeId);
        noticeRepository.deleteByNoticeId(noticeId);
    }
}
