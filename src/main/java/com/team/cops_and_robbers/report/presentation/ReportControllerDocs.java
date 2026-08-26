package com.team.cops_and_robbers.report.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityChatException;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.game.game.exception.GameException;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.report.exception.ReportException;
import com.team.cops_and_robbers.report.presentation.dto.request.CommunityChatReportRequest;
import com.team.cops_and_robbers.report.presentation.dto.request.CommunityPostReportRequest;
import com.team.cops_and_robbers.report.presentation.dto.request.ReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Report", description = "신고 API")
public interface ReportControllerDocs {

    @Operation(summary = "채팅 신고", description = "게임 채팅 메시지를 신고합니다.")
    @ApiErrorCode(value = GameParticipantException.class, codes = {"PARTICIPANT_NOT_FOUND"})
    @ApiErrorCode(value = GameException.class, codes = {"GAME_NOT_IN_PROGRESS"})
    @ApiErrorCode(value = ReportException.class, codes = {"SELF_REPORT", "DUPLICATE_REPORT", "REPORT_TARGET_NOT_FOUND", "ETC_REASON_REQUIRED"})
    @ApiResponses(@ApiResponse(responseCode = "201", description = "신고 성공"))
    ResponseEntity<Void> reportChat(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid ReportRequest request
    );

    @Operation(summary = "커뮤니티 모집글 신고", description = "커뮤니티 모집 게시글을 신고합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiErrorCode(value = ReportException.class, codes = {"SELF_REPORT", "DUPLICATE_REPORT", "ETC_REASON_REQUIRED"})
    @ApiResponses(@ApiResponse(responseCode = "201", description = "신고 성공"))
    ResponseEntity<Void> reportCommunityPost(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid CommunityPostReportRequest request
    );

    @Operation(summary = "커뮤니티 채팅 신고", description = "커뮤니티 모집 채팅방의 메시지를 신고합니다.")
    @ApiErrorCode(value = CommunityChatException.class, codes = {"CHAT_MESSAGE_NOT_FOUND"})
    @ApiErrorCode(value = ReportException.class, codes = {"SELF_REPORT", "DUPLICATE_REPORT", "ETC_REASON_REQUIRED"})
    @ApiResponses(@ApiResponse(responseCode = "201", description = "신고 성공"))
    ResponseEntity<Void> reportCommunityChat(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid CommunityChatReportRequest request
    );
}
