package com.team.cops_and_robbers.community.presentation.dto.response;

import com.team.cops_and_robbers.community.application.dto.result.CommunityChatHistoryResult;
import com.team.cops_and_robbers.community.application.dto.result.CommunityChatMessageResult;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CommunityChatHistoryResponse(
        @Schema(description = "메시지 목록 (최신순)")
        List<MessageResponse> messages,
        @Schema(description = "다음 조회에 사용할 커서", example = "1200")
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
    public record MessageResponse(
            @Schema(description = "메시지 ID", example = "1234")
            Long id,
            @Schema(description = "클라이언트 생성 키", example = "3f9a1c02-5b7e-4d61-9a83-0c2e4f8b1d67")
            String messageKey,
            @Schema(description = "발신자 ID", example = "7")
            Long senderId,
            @Schema(description = "발신자 닉네임 (탈퇴 시 발신 시점 닉네임)", example = "홍길동")
            String senderNickname,
            @Schema(description = "본문. SYSTEM / GAME_INVITE는 JSON 문자열", example = "안녕하세요!")
            String message,
            @Schema(description = "메시지 타입", example = "TEXT")
            CommunityChatMessageType messageType,
            @Schema(description = "작성일시", example = "2026-08-20T14:30:00+09:00")
            String createdAt
    ) {
        public static MessageResponse from(CommunityChatMessageResult result) {
            return new MessageResponse(
                    result.id(),
                    result.messageKey(),
                    result.senderId(),
                    result.senderNickname(),
                    result.message(),
                    result.messageType(),
                    result.createdAt()
            );
        }
    }

    public static CommunityChatHistoryResponse from(CommunityChatHistoryResult result) {
        return new CommunityChatHistoryResponse(
                result.messages().stream().map(MessageResponse::from).toList(),
                result.nextCursor(),
                result.hasNext()
        );
    }
}
