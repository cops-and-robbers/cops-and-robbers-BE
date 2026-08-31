package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeDeleteCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeTranslationCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeTranslationsResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.NoticeFixture.JA_TRANSLATION;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.KO_TRANSLATION;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.NOTICE;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.PINNED_NOTICE;
import static com.team.cops_and_robbers.common.fixture.UserFixture.ADMIN;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class NoticeServiceTest extends ServiceUnitTest {

    @InjectMocks
    private NoticeService noticeService;

    private static NoticeTranslationCommand koTranslation() {
        return new NoticeTranslationCommand(NoticeLanguage.KO, "공지사항 제목", "공지사항 내용");
    }

    private static NoticeTranslationCommand jaTranslation() {
        return new NoticeTranslationCommand(NoticeLanguage.JA, "お知らせのタイトル", "お知らせの内容");
    }

    @Nested
    @DisplayName("공지사항 생성")
    class Create {

        @Test
        void 관리자는_번역과_함께_공지사항을_생성하고_NoticeResult를_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.save(any())).willReturn(notice);
            given(noticeTranslationRepository.saveAll(any())).willReturn(List.of(KO_TRANSLATION(1L)));

            NoticeResult result = noticeService.createNotice(new NoticeCreateCommand(
                    1L, false, NoticeCategory.NOTICE, NoticeLanguage.KO, List.of(koTranslation())));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("공지사항 제목");
            assertThat(result.content()).isEqualTo("공지사항 내용");
            assertThat(result.language()).isEqualTo("ko");
            assertThat(result.requestedLanguage()).isEqualTo("ko");
            assertThat(result.pinned()).isFalse();
            assertThat(result.category()).isEqualTo(NoticeCategory.NOTICE);
        }

        @Test
        void 같은_언어의_번역이_두_번_오면_DUPLICATE_TRANSLATION_LANGUAGE_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());

            assertThatThrownBy(() -> noticeService.createNotice(new NoticeCreateCommand(
                    1L, false, NoticeCategory.NOTICE, NoticeLanguage.KO,
                    List.of(koTranslation(), koTranslation()))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.DUPLICATE_TRANSLATION_LANGUAGE.getDetail());
        }

        @Test
        void 원문_언어의_번역이_없으면_MISSING_ORIGINAL_TRANSLATION_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());

            assertThatThrownBy(() -> noticeService.createNotice(new NoticeCreateCommand(
                    1L, false, NoticeCategory.NOTICE, NoticeLanguage.KO, List.of(jaTranslation()))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.MISSING_ORIGINAL_TRANSLATION.getDetail());
        }

        @Test
        void 일반_사용자가_생성하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.createNotice(new NoticeCreateCommand(
                    1L, false, NoticeCategory.NOTICE, NoticeLanguage.KO, List.of(koTranslation()))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.FORBIDDEN_ADMIN_ONLY.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 목록 조회")
    class GetAll {

        @Test
        void pinned_공지가_먼저_정렬되어_반환된다() {
            Notice normal = NOTICE();
            Notice pinned = PINNED_NOTICE();
            setId(normal, 2L);
            setId(pinned, 1L);
            given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(any()))
                    .willReturn(new PageImpl<>(List.of(pinned, normal)));
            given(noticeTranslationRepository.findAllByNoticeIdIn(any()))
                    .willReturn(List.of(KO_TRANSLATION(1L), KO_TRANSLATION(2L)));

            Page<NoticeResult> result = noticeService.getNoticeList(NoticeListCommand.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).pinned()).isTrue();
            assertThat(result.getContent().get(1).pinned()).isFalse();
        }

        @Test
        void 요청한_언어의_번역이_있으면_그_언어로_내려간다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(any()))
                    .willReturn(new PageImpl<>(List.of(notice)));
            given(noticeTranslationRepository.findAllByNoticeIdIn(any()))
                    .willReturn(List.of(KO_TRANSLATION(1L), JA_TRANSLATION(1L)));

            Page<NoticeResult> result = noticeService.getNoticeList(
                    NoticeListCommand.of(0, 10, null, NoticeLanguage.JA));

            assertThat(result.getContent().get(0).title()).isEqualTo("お知らせのタイトル");
            assertThat(result.getContent().get(0).language()).isEqualTo("ja");
            assertThat(result.getContent().get(0).requestedLanguage()).isEqualTo("ja");
        }

        @Test
        void 요청한_언어의_번역이_없으면_원문_언어로_대체된다() {
            Notice notice = NOTICE(); // originalLanguage = KO
            setId(notice, 1L);
            given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(any()))
                    .willReturn(new PageImpl<>(List.of(notice)));
            given(noticeTranslationRepository.findAllByNoticeIdIn(any()))
                    .willReturn(List.of(KO_TRANSLATION(1L)));

            Page<NoticeResult> result = noticeService.getNoticeList(
                    NoticeListCommand.of(0, 10, null, NoticeLanguage.JA));

            assertThat(result.getContent().get(0).title()).isEqualTo("공지사항 제목");
            assertThat(result.getContent().get(0).language()).isEqualTo("ko");
            assertThat(result.getContent().get(0).requestedLanguage()).isEqualTo("ja");
        }

        @Test
        void category_필터를_지정하면_해당_카테고리만_반환된다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.findAllByCategoryOrderByPinnedDescCreatedAtDesc(
                    eq(NoticeCategory.NOTICE), any()))
                    .willReturn(new PageImpl<>(List.of(notice)));
            given(noticeTranslationRepository.findAllByNoticeIdIn(any()))
                    .willReturn(List.of(KO_TRANSLATION(1L)));

            Page<NoticeResult> result = noticeService.getNoticeList(
                    NoticeListCommand.of(0, 10, NoticeCategory.NOTICE));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category()).isEqualTo(NoticeCategory.NOTICE);
        }
    }

    @Nested
    @DisplayName("공지사항 단건 조회")
    class GetOne {

        @Test
        void 존재하는_ID로_조회하면_요청_언어를_반영한_NoticeResult를_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);
            given(noticeTranslationRepository.findAllByNoticeId(1L))
                    .willReturn(List.of(KO_TRANSLATION(1L), JA_TRANSLATION(1L)));

            NoticeResult result = noticeService.getNotice(1L, NoticeLanguage.JA);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.language()).isEqualTo("ja");
        }

        @Test
        void 존재하지_않는_ID로_조회하면_NOTICE_NOT_FOUND_예외가_발생한다() {
            given(noticeRepository.getByNoticeId(999L))
                    .willThrow(new ApplicationException(NoticeException.NOTICE_NOT_FOUND));

            assertThatThrownBy(() -> noticeService.getNotice(999L, NoticeLanguage.KO))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.NOTICE_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 번역 전체 조회")
    class GetTranslations {

        @Test
        void 관리자는_대체_없이_저장된_번역_전체를_받는다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);
            given(noticeTranslationRepository.findAllByNoticeId(1L))
                    .willReturn(List.of(KO_TRANSLATION(1L), JA_TRANSLATION(1L)));

            NoticeTranslationsResult result = noticeService.getNoticeTranslations(1L, 1L);

            assertThat(result.noticeId()).isEqualTo(1L);
            assertThat(result.originalLanguage()).isEqualTo("ko");
            assertThat(result.translations()).hasSize(2);
        }

        @Test
        void 일반_사용자가_조회하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.getNoticeTranslations(1L, 1L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.FORBIDDEN_ADMIN_ONLY.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 수정")
    class Update {

        @Test
        void 관리자는_번역을_통째로_교체하고_변경된_값을_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);
            given(noticeTranslationRepository.saveAll(any())).willReturn(List.of(JA_TRANSLATION(1L)));

            NoticeResult result = noticeService.updateNotice(new NoticeUpdateCommand(
                    1L, 1L, true, NoticeCategory.MAINTENANCE, NoticeLanguage.JA, List.of(jaTranslation())));

            then(noticeTranslationRepository).should().deleteAllByNoticeId(1L);
            assertThat(result.title()).isEqualTo("お知らせのタイトル");
            assertThat(result.language()).isEqualTo("ja");
            assertThat(result.pinned()).isTrue();
            assertThat(result.category()).isEqualTo(NoticeCategory.MAINTENANCE);
        }

        @Test
        void category가_null이면_기존_카테고리가_유지된다() {
            Notice notice = NOTICE(); // category = NOTICE
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);
            given(noticeTranslationRepository.saveAll(any())).willReturn(List.of(KO_TRANSLATION(1L)));

            NoticeResult result = noticeService.updateNotice(new NoticeUpdateCommand(
                    1L, 1L, true, null, NoticeLanguage.KO, List.of(koTranslation())));

            assertThat(result.category()).isEqualTo(NoticeCategory.NOTICE);
        }

        @Test
        void 일반_사용자가_수정하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.updateNotice(new NoticeUpdateCommand(
                    1L, 1L, true, NoticeCategory.NOTICE, NoticeLanguage.KO, List.of(koTranslation()))))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.FORBIDDEN_ADMIN_ONLY.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 삭제")
    class Delete {

        @Test
        void 관리자가_삭제하면_번역과_공지가_함께_삭제된다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            noticeService.deleteNotice(new NoticeDeleteCommand(1L, 1L));

            then(noticeTranslationRepository).should().deleteAllByNoticeId(1L);
            then(noticeRepository).should().deleteByNoticeId(1L);
        }

        @Test
        void 일반_사용자가_삭제하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.deleteNotice(new NoticeDeleteCommand(1L, 1L)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.FORBIDDEN_ADMIN_ONLY.getDetail());
        }

        @Test
        void 존재하지_않는_ID로_삭제하면_NOTICE_NOT_FOUND_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(999L))
                    .willThrow(new ApplicationException(NoticeException.NOTICE_NOT_FOUND));

            assertThatThrownBy(() -> noticeService.deleteNotice(new NoticeDeleteCommand(1L, 999L)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.NOTICE_NOT_FOUND.getDetail());
        }
    }
}
