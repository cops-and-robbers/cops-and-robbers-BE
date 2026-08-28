package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeDeleteCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeTranslationCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeTranslationsResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import com.team.cops_and_robbers.notice.domain.NoticeTranslation;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import com.team.cops_and_robbers.notice.repository.NoticeRepository;
import com.team.cops_and_robbers.notice.repository.NoticeTranslationRepository;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeTranslationRepository noticeTranslationRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoticeResult createNotice(NoticeCreateCommand command) {
        validateAdminRole(command.userId());
        validateTranslations(command.originalLanguage(), command.translations());
        Notice notice = noticeRepository.save(Notice.createNotice(command));
        List<NoticeTranslation> translations = saveTranslations(notice.getId(), command.translations());
        return toResult(notice, translations, command.originalLanguage());
    }

    public Page<NoticeResult> getNoticeList(NoticeListCommand command) {
        Page<Notice> notices = findNotices(command);
        // 번역을 공지마다 따로 조회하면 목록 크기만큼 쿼리가 반복되므로 한 번에 불러 묶는다
        List<Long> noticeIds = notices.getContent().stream().map(Notice::getId).toList();
        Map<Long, List<NoticeTranslation>> translationsByNoticeId = noticeIds.isEmpty()
                ? Map.of()
                : noticeTranslationRepository.findAllByNoticeIdIn(noticeIds).stream()
                        .collect(Collectors.groupingBy(NoticeTranslation::getNoticeId));
        return notices.map(notice -> toResult(
                notice,
                translationsByNoticeId.getOrDefault(notice.getId(), List.of()),
                command.language()));
    }

    public NoticeResult getNotice(Long noticeId, NoticeLanguage language) {
        Notice notice = noticeRepository.getByNoticeId(noticeId);
        List<NoticeTranslation> translations = noticeTranslationRepository.findAllByNoticeId(noticeId);
        return toResult(notice, translations, language);
    }

    /** 어드민 편집 화면용. 대체 없이 저장된 번역 전체를 그대로 내려준다. */
    public NoticeTranslationsResult getNoticeTranslations(Long userId, Long noticeId) {
        validateAdminRole(userId);
        Notice notice = noticeRepository.getByNoticeId(noticeId);
        List<NoticeTranslation> translations = noticeTranslationRepository.findAllByNoticeId(noticeId);
        return NoticeTranslationsResult.from(notice, translations);
    }

    /**
     * 번역은 통째로 교체한다. 부분 수정을 두면 "원문 언어 번역을 지웠는지" 같은
     * 경계 검증이 늘어나는데, 공지는 글이 짧고 수가 적어 통 교체로 충분하다.
     */
    @Transactional
    public NoticeResult updateNotice(NoticeUpdateCommand command) {
        validateAdminRole(command.userId());
        validateTranslations(command.originalLanguage(), command.translations());
        Notice notice = noticeRepository.getByNoticeId(command.noticeId());
        notice.updateNotice(command);
        noticeTranslationRepository.deleteAllByNoticeId(notice.getId());
        List<NoticeTranslation> translations = saveTranslations(notice.getId(), command.translations());
        return toResult(notice, translations, command.originalLanguage());
    }

    @Transactional
    public void deleteNotice(NoticeDeleteCommand command) {
        validateAdminRole(command.userId());
        noticeRepository.getByNoticeId(command.noticeId());
        noticeTranslationRepository.deleteAllByNoticeId(command.noticeId());
        noticeRepository.deleteByNoticeId(command.noticeId());
    }

    private Page<Notice> findNotices(NoticeListCommand command) {
        if (command.category() == null) {
            return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(command.toPageable());
        }
        return noticeRepository.findAllByCategoryOrderByPinnedDescCreatedAtDesc(
                command.category(),
                command.toPageable());
    }

    /** 같은 언어가 두 번 오거나 원문 언어의 번역이 빠진 요청은 저장 전에 거른다. */
    private void validateTranslations(NoticeLanguage originalLanguage, List<NoticeTranslationCommand> translations) {
        long distinctLanguages = translations.stream()
                .map(NoticeTranslationCommand::language)
                .distinct()
                .count();
        if (distinctLanguages != translations.size()) {
            throw new ApplicationException(NoticeException.DUPLICATE_TRANSLATION_LANGUAGE);
        }
        boolean hasOriginal = translations.stream()
                .anyMatch(translation -> translation.language() == originalLanguage);
        if (!hasOriginal) {
            throw new ApplicationException(NoticeException.MISSING_ORIGINAL_TRANSLATION);
        }
    }

    private List<NoticeTranslation> saveTranslations(Long noticeId, List<NoticeTranslationCommand> commands) {
        List<NoticeTranslation> translations = commands.stream()
                .map(command -> NoticeTranslation.createTranslation(noticeId, command))
                .toList();
        return noticeTranslationRepository.saveAll(translations);
    }

    /**
     * 요청 언어 → 원문 언어 → 아무 번역 순으로 내려줄 번역을 고른다.
     * 요청 언어와 실제 내려간 언어를 함께 응답해, 앱이 "요청한 언어의 번역이
     * 아직 없다"를 구분할 수 있게 한다.
     */
    private NoticeResult toResult(Notice notice, List<NoticeTranslation> translations, NoticeLanguage requested) {
        Map<NoticeLanguage, NoticeTranslation> byLanguage = translations.stream()
                .collect(Collectors.toMap(NoticeTranslation::getLanguage, Function.identity()));
        NoticeTranslation resolved = byLanguage.get(requested);
        if (resolved == null) {
            resolved = byLanguage.get(notice.getOriginalLanguage());
        }
        if (resolved == null) {
            resolved = translations.stream()
                    .findFirst()
                    .orElseThrow(() -> new ApplicationException(NoticeException.TRANSLATION_NOT_FOUND));
        }
        return NoticeResult.from(notice, resolved, requested);
    }

    private void validateAdminRole(Long userId) {
        User user = userRepository.getByUserId(userId);
        if (!user.isAdmin()) {
            throw new ApplicationException(NoticeException.FORBIDDEN_ADMIN_ONLY);
        }
    }
}
