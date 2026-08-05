package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeDeleteCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

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

    @Nested
    @DisplayName("공지사항 생성")
    class Create {

        @Test
        void 관리자는_공지사항을_생성하고_NoticeResult를_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.save(any())).willReturn(notice);

            NoticeResult result = noticeService.createNotice(
                    new NoticeCreateCommand(1L, "공지사항 제목", "공지사항 내용", false, NoticeCategory.NOTICE));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("공지사항 제목");
            assertThat(result.content()).isEqualTo("공지사항 내용");
            assertThat(result.pinned()).isFalse();
            assertThat(result.category()).isEqualTo(NoticeCategory.NOTICE);
        }

        @Test
        void 일반_사용자가_생성하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.createNotice(
                    new NoticeCreateCommand(1L, "공지사항 제목", "공지사항 내용", false, NoticeCategory.NOTICE)))
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

            Page<NoticeResult> result = noticeService.getNoticeList(NoticeListCommand.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).pinned()).isTrue();
            assertThat(result.getContent().get(1).pinned()).isFalse();
        }

        @Test
        void category_필터를_지정하면_해당_카테고리만_반환된다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.findAllByCategoryOrderByPinnedDescCreatedAtDesc(
                    eq(NoticeCategory.NOTICE), any()))
                    .willReturn(new PageImpl<>(List.of(notice)));

            Page<NoticeResult> result = noticeService.getNoticeList(
                    NoticeListCommand.of(0, 10, NoticeCategory.NOTICE));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).category()).isEqualTo(NoticeCategory.NOTICE);
        }

        @Test
        void category가_null이면_전체_조회한다() {
            Notice normal = NOTICE();
            Notice pinned = PINNED_NOTICE();
            setId(normal, 2L);
            setId(pinned, 1L);
            given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(any()))
                    .willReturn(new PageImpl<>(List.of(pinned, normal)));

            Page<NoticeResult> result = noticeService.getNoticeList(NoticeListCommand.of(0, 10, null));

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("공지사항 단건 조회")
    class GetOne {

        @Test
        void 존재하는_ID로_조회하면_NoticeResult를_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            NoticeResult result = noticeService.getNotice(1L);

            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        void 존재하지_않는_ID로_조회하면_NOTICE_NOT_FOUND_예외가_발생한다() {
            given(noticeRepository.getByNoticeId(999L))
                    .willThrow(new ApplicationException(NoticeException.NOTICE_NOT_FOUND));

            assertThatThrownBy(() -> noticeService.getNotice(999L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.NOTICE_NOT_FOUND.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 수정")
    class Update {

        @Test
        void 관리자는_공지사항을_수정하고_변경된_값을_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            NoticeResult result = noticeService.updateNotice(
                    new NoticeUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용", true, NoticeCategory.MAINTENANCE));

            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("수정된 내용");
            assertThat(result.pinned()).isTrue();
            assertThat(result.category()).isEqualTo(NoticeCategory.MAINTENANCE);
        }

        @Test
        void category가_null이면_기존_카테고리가_유지된다() {
            Notice notice = NOTICE(); // category = NOTICE
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            NoticeResult result = noticeService.updateNotice(
                    new NoticeUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용", true, null));

            assertThat(result.category()).isEqualTo(NoticeCategory.NOTICE);
        }

        @Test
        void 일반_사용자가_수정하면_FORBIDDEN_ADMIN_ONLY_예외가_발생한다() {
            given(userRepository.getByUserId(1L)).willReturn(USER());

            assertThatThrownBy(() -> noticeService.updateNotice(
                    new NoticeUpdateCommand(1L, 1L, "수정된 제목", "수정된 내용", true, NoticeCategory.NOTICE)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.FORBIDDEN_ADMIN_ONLY.getDetail());
        }
    }

    @Nested
    @DisplayName("공지사항 삭제")
    class Delete {

        @Test
        void 관리자는_공지사항을_삭제하면_deleteByNoticeId가_호출된다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(userRepository.getByUserId(1L)).willReturn(ADMIN());
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            noticeService.deleteNotice(new NoticeDeleteCommand(1L, 1L));

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
