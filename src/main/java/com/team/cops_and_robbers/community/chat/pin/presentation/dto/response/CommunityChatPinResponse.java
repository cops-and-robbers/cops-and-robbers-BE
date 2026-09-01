package com.team.cops_and_robbers.community.chat.pin.presentation.dto.response;

import com.team.cops_and_robbers.community.chat.pin.application.dto.result.CommunityChatPinResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityChatPinResponse(
        @Schema(description = "고정 채팅 ID (없으면 null)", example = "1")
        Long id,
        @Schema(description = "게시글(채팅방) ID", example = "1")
        Long postId,
        @Schema(description = "등록한 방장의 유저 ID (없으면 null)", example = "7")
        Long writerId,
        @Schema(description = "등록한 방장의 닉네임 (없으면 null)", example = "무서운경찰관")
        String writerNickname,
        @Schema(description = "등록한 방장의 프로필 아이콘 번호 (없으면 0)", example = "1")
        int writerProfileIcon,
        @Schema(description = "고정 채팅 내용 (없으면 null)", example = "오늘 오후 7시 정문에서 만나요!")
        String content,
        @Schema(description = "등록 시각 (없으면 null)")
        String createdAt,
        @Schema(description = "수정 시각 (없으면 null)")
        String updatedAt
) {
    public static CommunityChatPinResponse from(CommunityChatPinResult result) {
        return new CommunityChatPinResponse(
                result.id(),
                result.postId(),
                result.writerId(),
                result.writerNickname(),
                result.writerProfileIcon(),
                result.content(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
