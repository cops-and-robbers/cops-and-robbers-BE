package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatHistoryResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatMemberListResponse;
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

    @Operation(summary = "채팅방 멤버 목록 조회",
            description = "채팅방 사이드바용 멤버 목록을 조회합니다. 해당 방 멤버만 조회할 수 있습니다. "
                    + "닉네임·프로필 아이콘은 조회 시점 현재 값을 내려주며, 탈퇴한 유저는 닉네임 \"알수없음\"과 기본 아이콘 번호로 내려줍니다. "
                    + "채팅 내역과 달리 발신 시점 스냅샷은 없습니다.")
    @ApiErrorCode(value = CommunityChatException.class, codes = {"NOT_A_CHAT_MEMBER"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityChatMemberListResponse> getMembers(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    );

    @Operation(summary = "채팅방 멤버 강퇴",
            description = "방장이 특정 멤버를 채팅방에서 내보냅니다. 강퇴된 유저는 다시 참여할 수 있습니다(재입장 제한 없음). "
                    + "서버가 웹소켓 세션을 직접 끊지는 않으며, 강퇴 시스템 메시지를 받은 앱이 스스로 구독을 해제해야 합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class,
            codes = {"FORBIDDEN_NOT_CHAT_HOST", "CANNOT_KICK_SELF", "CHAT_MEMBER_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "강퇴 성공")
    })
    ResponseEntity<Void> kick(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            @Parameter(description = "강퇴할 유저 ID", example = "7") @PathVariable Long userId
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