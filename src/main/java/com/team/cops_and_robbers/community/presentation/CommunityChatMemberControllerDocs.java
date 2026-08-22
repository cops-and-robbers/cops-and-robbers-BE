package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "CommunityChatMember", description = "커뮤니티 채팅방 참여 API")
public interface CommunityChatMemberControllerDocs {

    @Operation(summary = "채팅방 참여하기", description = "모집 게시글의 채팅방에 참여합니다. 모집 중인 게시글에만 참여할 수 있습니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class,
            codes = {"ALREADY_JOINED", "CHAT_ROOM_FULL", "RECRUITMENT_CLOSED", "JOINED_CHAT_ROOM_LIMIT_EXCEEDED"})
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

    @Operation(summary = "채팅 내역 조회",
            description = "커서 기반으로 최신 메시지부터 조회합니다. cursor를 생략하면 가장 최근부터 조회합니다.")
    @ApiErrorCode(value = CommunityChatException.class, codes = {"NOT_A_CHAT_MEMBER"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityChatHistoryResponse> getChatHistory(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            @Parameter(description = "이전 응답의 nextCursor", example = "1200") @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (1~50)", example = "20") @RequestParam(defaultValue = "20") int size
    );
}