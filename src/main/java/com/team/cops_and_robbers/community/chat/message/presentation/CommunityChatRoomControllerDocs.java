package com.team.cops_and_robbers.community.chat.message.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.community.chat.message.presentation.dto.response.CommunityChatRoomListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityChatRoom", description = "커뮤니티 채팅방 목록 API")
public interface CommunityChatRoomControllerDocs {

    @Operation(summary = "내 채팅방 목록 조회",
            description = "참여 중인 채팅방을 마지막 대화가 최근인 순으로 조회합니다. 참여 방 수에 상한이 있으므로 굳이 페이징하지 않습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityChatRoomListResponse> getChatRooms(
            @Parameter(hidden = true) LoginUser loginUser
    );
}
