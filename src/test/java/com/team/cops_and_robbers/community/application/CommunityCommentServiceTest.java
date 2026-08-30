package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.ServiceUnitTest;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentCreateCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentDeleteCommand;
import com.team.cops_and_robbers.community.application.dto.command.CommunityCommentListCommand;
import com.team.cops_and_robbers.community.application.dto.result.CommunityCommentListResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityCommentResult;
import com.team.cops_and_robbers.community.domain.CommunityComment;
import com.team.cops_and_robbers.community.exception.CommunityCommentException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team.cops_and_robbers.common.fixture.CommunityPostFixture.POST;
import static com.team.cops_and_robbers.common.fixture.UserFixture.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CommunityCommentServiceTest extends ServiceUnitTest {

    private static final Long POST_ID = 1L;
    private static final Long WRITER_ID = 100L;

    @InjectMocks
    private CommunityCommentService communityCommentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private void givenPost() {
        given(communityPostRepository.getByPostId(POST_ID)).willReturn(POST(WRITER_ID));
    }

    private void givenWriter() {
        given(userRepository.getByUserId(WRITER_ID)).willReturn(userWithNickname(WRITER_ID, "작성자"));
    }

    private User userWithNickname(Long userId, String nickname) {
        User user = USER(nickname);
        setId(user, userId);
        return user;
    }

    private User userWithNickname(Long userId, String nickname, int profileIcon) {
        User user = userWithNickname(userId, nickname);
        ReflectionTestUtils.setField(user, "profileIcon", profileIcon);
        return user;
    }

    private CommunityComment comment(Long id, Long parentId, Long writerId, String content) {
        CommunityComment comment = CommunityComment.createComment(POST_ID, parentId, writerId, content);
        setId(comment, id);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.now());
        return comment;
    }

    private CommunityComment captureSavedComment() {
        ArgumentCaptor<CommunityComment> captor = ArgumentCaptor.forClass(CommunityComment.class);
        then(communityCommentRepository).should().save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("댓글 작성")
    class Create {

        @Test
        void 댓글을_작성하면_저장하고_생성_이벤트를_발행한다() {
            givenWriter();
            givenPost();
            given(communityCommentRepository.save(any())).willReturn(comment(10L, null, WRITER_ID, "내용"));

            CommunityCommentResult result = communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, null, "내용"));

            assertThat(result.content()).isEqualTo("내용");
            assertThat(result.writerNickname()).isEqualTo("작성자");
            assertThat(result.writerProfileIcon()).isEqualTo(User.DEFAULT_PROFILE_ICON);
            then(eventPublisher).should().publishEvent(any(Object.class));
        }

        @Test
        void 존재하지_않는_게시글에는_작성할_수_없다() {
            givenWriter();
            given(communityPostRepository.getByPostId(POST_ID))
                    .willThrow(new ApplicationException(CommunityPostException.POST_NOT_FOUND));

            assertThatThrownBy(() -> communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, null, "내용")))
                    .isInstanceOf(ApplicationException.class);

            then(communityCommentRepository).should(never()).save(any());
        }

        @Test
        void parentId가_없으면_1depth_댓글로_저장된다() {
            givenWriter();
            givenPost();
            given(communityCommentRepository.save(any())).willReturn(comment(10L, null, WRITER_ID, "내용"));

            communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, null, "내용"));

            assertThat(captureSavedComment().getParentId()).isNull();
        }

        @Test
        void 존재하지_않는_부모_댓글에는_답글을_달_수_없다() {
            givenWriter();
            givenPost();
            given(communityCommentRepository.findByCommentIdForUpdate(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, 999L, "답글")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityCommentException.PARENT_COMMENT_NOT_FOUND.getDetail());
        }

        @Test
        void 다른_게시글의_댓글에는_답글을_달_수_없다() {
            givenWriter();
            givenPost();
            given(communityCommentRepository.findByCommentIdForUpdate(50L))
                    .willReturn(Optional.of(CommunityComment.createComment(999L, null, WRITER_ID, "다른 글 댓글")));

            assertThatThrownBy(() -> communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, 50L, "답글")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityCommentException.PARENT_COMMENT_POST_MISMATCH.getDetail());
        }

        @Test
        void 답글에는_답글을_달_수_없다() {
            givenWriter();
            givenPost();
            CommunityComment reply = CommunityComment.createComment(POST_ID, 10L, WRITER_ID, "답글");
            given(communityCommentRepository.findByCommentIdForUpdate(20L)).willReturn(Optional.of(reply));

            assertThatThrownBy(() -> communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, 20L, "대답글")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityCommentException.INVALID_COMMENT_DEPTH.getDetail());
        }

        @Test
        void 삭제된_댓글에는_답글을_달_수_없다() {
            givenWriter();
            givenPost();
            CommunityComment deletedParent = CommunityComment.createComment(POST_ID, null, WRITER_ID, "삭제될 댓글");
            deletedParent.markDeleted();
            given(communityCommentRepository.findByCommentIdForUpdate(10L)).willReturn(Optional.of(deletedParent));

            assertThatThrownBy(() -> communityCommentService.createComment(
                    CommunityCommentCreateCommand.of(WRITER_ID, POST_ID, 10L, "답글")))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityCommentException.DELETED_COMMENT_CANNOT_REPLY.getDetail());
        }
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        void 요청_크기보다_한_건_더_있으면_hasNext가_true다() {
            List<CommunityComment> threeRoots = List.of(
                    comment(10L, null, WRITER_ID, "1"),
                    comment(11L, null, WRITER_ID, "2"),
                    comment(12L, null, WRITER_ID, "3"));
            given(communityCommentRepository.findRootPageByCursor(any(), any(), any())).willReturn(threeRoots);
            given(communityCommentRepository.findRepliesByParentIds(anyList())).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of(userWithNickname(WRITER_ID, "작성자")));

            CommunityCommentListResult result =
                    communityCommentService.getComments(CommunityCommentListCommand.of(POST_ID, null, 2));

            assertThat(result.content()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(11L);
        }

        @Test
        void 마지막_페이지면_hasNext가_false이고_nextCursor는_null이다() {
            List<CommunityComment> twoRoots = List.of(
                    comment(10L, null, WRITER_ID, "1"),
                    comment(11L, null, WRITER_ID, "2"));
            given(communityCommentRepository.findRootPageByCursor(any(), any(), any())).willReturn(twoRoots);
            given(communityCommentRepository.findRepliesByParentIds(anyList())).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of(userWithNickname(WRITER_ID, "작성자")));

            CommunityCommentListResult result =
                    communityCommentService.getComments(CommunityCommentListCommand.of(POST_ID, null, 2));

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        void 답글은_부모_댓글의_replies에_함께_담긴다() {
            CommunityComment root = comment(10L, null, WRITER_ID, "루트");
            CommunityComment reply = comment(20L, 10L, 200L, "답글");
            given(communityCommentRepository.findRootPageByCursor(any(), any(), any())).willReturn(List.of(root));
            given(communityCommentRepository.findRepliesByParentIds(anyList())).willReturn(List.of(reply));
            given(userRepository.findAllById(anyList())).willReturn(List.of(
                    userWithNickname(WRITER_ID, "루트작성자"), userWithNickname(200L, "답글작성자", 2)));

            CommunityCommentListResult result =
                    communityCommentService.getComments(CommunityCommentListCommand.of(POST_ID, null, 10));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().replies()).hasSize(1);
            CommunityCommentResult replyResult = result.content().getFirst().replies().getFirst();
            assertThat(replyResult.writerNickname()).isEqualTo("답글작성자");
            assertThat(replyResult.writerProfileIcon()).isEqualTo(2);
        }

        @Test
        void 삭제된_댓글은_내용과_작성자가_가려진_채_남는다() {
            CommunityComment deletedRoot = comment(10L, null, WRITER_ID, "지워질 댓글");
            deletedRoot.markDeleted();
            given(communityCommentRepository.findRootPageByCursor(any(), any(), any())).willReturn(List.of(deletedRoot));
            given(communityCommentRepository.findRepliesByParentIds(anyList())).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());

            CommunityCommentListResult result =
                    communityCommentService.getComments(CommunityCommentListCommand.of(POST_ID, null, 10));

            CommunityCommentResult content = result.content().getFirst();
            assertThat(content.deleted()).isTrue();
            assertThat(content.content()).isNull();
            assertThat(content.writerNickname()).isNull();
            assertThat(content.writerProfileIcon()).isNull();
        }

        @Test
        void 탈퇴한_유저의_댓글은_닉네임이_알수없음으로_내려온다() {
            given(communityCommentRepository.findRootPageByCursor(any(), any(), any()))
                    .willReturn(List.of(comment(10L, null, WRITER_ID, "댓글")));
            given(communityCommentRepository.findRepliesByParentIds(anyList())).willReturn(List.of());
            given(userRepository.findAllById(anyList())).willReturn(List.of());

            CommunityCommentListResult result =
                    communityCommentService.getComments(CommunityCommentListCommand.of(POST_ID, null, 10));

            CommunityCommentResult content = result.content().getFirst();
            assertThat(content.deleted()).isFalse();
            assertThat(content.writerId()).isEqualTo(WRITER_ID);
            assertThat(content.writerNickname()).isEqualTo("알수없음");
            assertThat(content.writerProfileIcon()).isEqualTo(User.DEFAULT_PROFILE_ICON);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class Delete {

        @Test
        void 작성자가_아니면_삭제할_수_없다() {
            given(communityCommentRepository.getByCommentId(10L)).willReturn(comment(10L, null, WRITER_ID, "내용"));

            assertThatThrownBy(() -> communityCommentService.deleteComment(
                    CommunityCommentDeleteCommand.of(999L, 10L)))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(CommunityCommentException.FORBIDDEN_NOT_COMMENT_AUTHOR.getDetail());
        }

        @Test
        void 답글이_없는_루트_댓글은_행이_삭제된다() {
            CommunityComment root = comment(10L, null, WRITER_ID, "내용");
            given(communityCommentRepository.getByCommentId(10L)).willReturn(root);
            given(communityCommentRepository.getByCommentIdForUpdate(10L)).willReturn(root);
            given(communityCommentRepository.countByParentId(10L)).willReturn(0);

            communityCommentService.deleteComment(CommunityCommentDeleteCommand.of(WRITER_ID, 10L));

            then(communityCommentRepository).should().delete(root);
        }

        @Test
        void 답글이_남아있는_루트_댓글은_삭제_표시만_된다() {
            CommunityComment root = comment(10L, null, WRITER_ID, "내용");
            given(communityCommentRepository.getByCommentId(10L)).willReturn(root);
            given(communityCommentRepository.getByCommentIdForUpdate(10L)).willReturn(root);
            given(communityCommentRepository.countByParentId(10L)).willReturn(1);

            communityCommentService.deleteComment(CommunityCommentDeleteCommand.of(WRITER_ID, 10L));

            assertThat(root.isDeleted()).isTrue();
            then(communityCommentRepository).should(never()).delete(any());
        }

        @Test
        void 답글을_삭제해도_부모가_살아있으면_부모는_지워지지_않는다() {
            CommunityComment reply = comment(20L, 10L, WRITER_ID, "답글");
            CommunityComment parent = comment(10L, null, 999L, "부모");
            given(communityCommentRepository.getByCommentId(20L)).willReturn(reply);
            given(communityCommentRepository.getByCommentIdForUpdate(10L)).willReturn(parent);
            given(communityCommentRepository.countByParentId(10L)).willReturn(2);

            communityCommentService.deleteComment(CommunityCommentDeleteCommand.of(WRITER_ID, 20L));

            then(communityCommentRepository).should().delete(reply);
            then(communityCommentRepository).should(never()).delete(parent);
        }

        @Test
        void 삭제_표시된_부모의_마지막_답글을_지우면_부모도_함께_지워진다() {
            CommunityComment reply = comment(20L, 10L, WRITER_ID, "마지막 답글");
            CommunityComment deletedParent = comment(10L, null, 999L, "부모");
            deletedParent.markDeleted();
            given(communityCommentRepository.getByCommentId(20L)).willReturn(reply);
            given(communityCommentRepository.getByCommentIdForUpdate(10L)).willReturn(deletedParent);
            given(communityCommentRepository.countByParentId(10L)).willReturn(1);

            communityCommentService.deleteComment(CommunityCommentDeleteCommand.of(WRITER_ID, 20L));

            then(communityCommentRepository).should().delete(reply);
            then(communityCommentRepository).should().delete(deletedParent);
        }

        @Test
        void 살아있는_부모의_마지막_답글을_지워도_부모는_지워지지_않는다() {
            CommunityComment reply = comment(20L, 10L, WRITER_ID, "마지막 답글");
            CommunityComment livingParent = comment(10L, null, 999L, "부모");
            given(communityCommentRepository.getByCommentId(20L)).willReturn(reply);
            given(communityCommentRepository.getByCommentIdForUpdate(10L)).willReturn(livingParent);
            given(communityCommentRepository.countByParentId(10L)).willReturn(1);

            communityCommentService.deleteComment(CommunityCommentDeleteCommand.of(WRITER_ID, 20L));

            then(communityCommentRepository).should().delete(reply);
            then(communityCommentRepository).should(never()).delete(livingParent);
        }
    }
}
