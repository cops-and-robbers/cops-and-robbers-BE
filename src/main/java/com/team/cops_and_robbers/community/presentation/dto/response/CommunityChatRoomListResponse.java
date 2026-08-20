package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityChatRoomResult;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityChatRoomListResponse(
        @Schema(description = "참여 중인 채팅방 목록 (최근 대화순)")
        List<ChatRoomResponse> chatRooms
) {
    public record ChatRoomResponse(
            @Schema(description = "게시글 ID", example = "42")
            Long postId,
            @Schema(description = "게시글 제목", example = "같이 경찰과 도둑 하실 분!")
            String title,
            @Schema(description = "모집 상태", example = "RECRUITING")
            RecruitmentStatus status,
            @Schema(description = "모임 날짜/시간", example = "2026-08-24T19:00:00+09:00")
            String meetingAt,
            @Schema(description = "참여 인원", example = "8")
            Long memberCount,
            @Schema(description = "마지막 메시지 (대화가 없으면 null)")
            LastMessageResponse lastMessage
    ) {
        public static ChatRoomResponse from(CommunityChatRoomResult result) {
            return new ChatRoomResponse(
                    result.postId(),
                    result.title(),
                    result.status(),
                    result.meetingAt(),
                    result.memberCount(),
                    LastMessageResponse.from(result.lastMessage())
            );
        }
    }

    public record LastMessageResponse(
            @Schema(description = "메시지 ID", example = "1234")
            Long id,
            @Schema(description = "본문. SYSTEM / GAME_INVITE는 JSON 문자열", example = "다들 고생하셨어요")
            String message,
            @Schema(description = "메시지 타입", example = "TEXT")
            CommunityChatMessageType messageType,
            @Schema(description = "작성일시", example = "2026-08-20T14:30:00+09:00")
            String createdAt
    ) {
        public static LastMessageResponse from(CommunityChatRoomResult.LastMessageResult result) {
            if (result == null) {
                return null;
            }
            return new LastMessageResponse(result.id(), result.message(), result.messageType(), result.createdAt());
        }
    }

    public static CommunityChatRoomListResponse from(List<CommunityChatRoomResult> results) {
        return new CommunityChatRoomListResponse(results.stream().map(ChatRoomResponse::from).toList());
    }
}
