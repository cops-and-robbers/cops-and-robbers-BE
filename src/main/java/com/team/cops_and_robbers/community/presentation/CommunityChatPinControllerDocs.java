package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityChatPinRegisterRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityChatPinUpdateRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityChatPinResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "CommunityChatPin", description = "커뮤니티 채팅방 고정 채팅(공지) API")
public interface CommunityChatPinControllerDocs {

    @Operation(summary = "고정 채팅 등록",
            description = """
                    채팅방 상단에 고정할 공지를 등록합니다. 방장만 가능합니다.

                    방마다 고정 채팅은 최대 1개이며, 다시 등록하면 이전 내용은 이력 없이 사라집니다.
                    등록·수정·삭제는 방 참여자에게 실시간(웹소켓)으로 반영되고, 채팅 대화창에도 시스템 메시지로 남으며, 참여자에게 푸시 알림이 발송됩니다.
                    """)
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class, codes = {"FORBIDDEN_NOT_CHAT_PIN_HOST"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "등록 성공")
    })
    ResponseEntity<CommunityChatPinResponse> register(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            CommunityChatPinRegisterRequest request
    );

    @Operation(summary = "고정 채팅 수정", description = "등록된 고정 채팅 내용을 수정합니다. 방장만 가능합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class, codes = {"FORBIDDEN_NOT_CHAT_PIN_HOST", "CHAT_PIN_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    ResponseEntity<CommunityChatPinResponse> update(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId,
            CommunityChatPinUpdateRequest request
    );

    @Operation(summary = "고정 채팅 삭제", description = "등록된 고정 채팅을 삭제합니다. 방장만 가능합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = CommunityChatException.class, codes = {"FORBIDDEN_NOT_CHAT_PIN_HOST", "CHAT_PIN_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    );

    @Operation(summary = "고정 채팅 조회",
            description = "채팅방에 고정된 공지를 조회합니다. 해당 방 멤버만 조회할 수 있습니다. "
                    + "등록된 고정 채팅이 없으면 200과 함께 content 등이 null인 응답을 내려줍니다(예외 아님).")
    @ApiErrorCode(value = CommunityChatException.class, codes = {"NOT_A_CHAT_MEMBER"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityChatPinResponse> get(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") @PathVariable Long postId
    );
}
