package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeDeleteCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoticeResult createNotice(NoticeCreateCommand command) {
        validateAdminRole(command.userId());
        Notice notice = noticeRepository.save(Notice.createNotice(command));
        return NoticeResult.from(notice);
    }

    public Page<NoticeResult> getNoticeList(NoticeListCommand command) {
        if (command.category() == null) {
            return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(command.toPageable())
                    .map(NoticeResult::from);
        }
        return noticeRepository.findAllByCategoryOrderByPinnedDescCreatedAtDesc(
                command.category(),
                command.toPageable())
                .map(NoticeResult::from);
    }

    public NoticeResult getNotice(Long noticeId) {
        return NoticeResult.from(noticeRepository.getByNoticeId(noticeId));
    }

    @Transactional
    public NoticeResult updateNotice(NoticeUpdateCommand command) {
        validateAdminRole(command.userId());
        Notice notice = noticeRepository.getByNoticeId(command.noticeId());
        notice.updateNotice(command);
        return NoticeResult.from(notice);
    }

    @Transactional
    public void deleteNotice(NoticeDeleteCommand command) {
        validateAdminRole(command.userId());
        noticeRepository.getByNoticeId(command.noticeId());
        noticeRepository.deleteByNoticeId(command.noticeId());
    }

    private void validateAdminRole(Long userId) {
        User user = userRepository.getByUserId(userId);
        if (!user.isAdmin()) {
            throw new ApplicationException(NoticeException.FORBIDDEN_ADMIN_ONLY);
        }
    }
}
