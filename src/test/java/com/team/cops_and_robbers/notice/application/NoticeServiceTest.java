package com.team.cops_and_robbers.notice.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.domain.Notice;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.List;

import static com.team.cops_and_robbers.common.fixture.NoticeFixture.NOTICE;
import static com.team.cops_and_robbers.common.fixture.NoticeFixture.PINNED_NOTICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class NoticeServiceTest extends ServiceUnitTest {

    @InjectMocks
    private NoticeService noticeService;

    @Nested
    @DisplayName("공지사항 생성")
    class Create {

        @Test
        void 저장_후_NoticeResult를_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.save(any())).willReturn(notice);

            NoticeResult result = noticeService.createNotice(new NoticeCreateCommand("공지사항 제목", "공지사항 내용", false));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("공지사항 제목");
            assertThat(result.content()).isEqualTo("공지사항 내용");
            assertThat(result.pinned()).isFalse();
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
            given(noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc()).willReturn(List.of(pinned, normal));

            List<NoticeResult> results = noticeService.getNoticeList();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).pinned()).isTrue();
            assertThat(results.get(1).pinned()).isFalse();
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
        void update_호출_후_변경된_값을_반환한다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            NoticeResult result = noticeService.updateNotice(new NoticeUpdateCommand(1L, "수정된 제목", "수정된 내용", true));

            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("수정된 내용");
            assertThat(result.pinned()).isTrue();
        }
    }

    @Nested
    @DisplayName("공지사항 삭제")
    class Delete {

        @Test
        void 존재하는_ID로_삭제하면_deleteByNoticeId가_호출된다() {
            Notice notice = NOTICE();
            setId(notice, 1L);
            given(noticeRepository.getByNoticeId(1L)).willReturn(notice);

            noticeService.deleteNotice(1L);

            then(noticeRepository).should().deleteByNoticeId(1L);
        }

        @Test
        void 존재하지_않는_ID로_삭제하면_NOTICE_NOT_FOUND_예외가_발생한다() {
            given(noticeRepository.getByNoticeId(999L))
                    .willThrow(new ApplicationException(NoticeException.NOTICE_NOT_FOUND));

            assertThatThrownBy(() -> noticeService.deleteNotice(999L))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(NoticeException.NOTICE_NOT_FOUND.getDetail());
        }
    }
}
