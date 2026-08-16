package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "CommunityChatMember", description = "커뮤니티 채팅방 참여 API")
public interface CommunityChatMemberControllerDocs {

    @Operation(summary = "채팅방 참여하기", description = "모집 게시글의 채팅방에 참여합니다. 모집 중인 게시글에만 참여할 수 있습니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class, codes = {"ALREADY_JOINED", "CHAT_ROOM_FULL", "RECRUITMENT_CLOSED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "참여 성공")
    })
    ResponseEntity<Void> join(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    );

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 나갑니다. 나가면 대화 내역을 볼 수 없으며, 작성자는 나갈 수 없습니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class, codes = {"NOT_A_CHAT_MEMBER", "AUTHOR_CANNOT_LEAVE"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "나가기 성공")
    })
    ResponseEntity<Void> leave(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    );
}