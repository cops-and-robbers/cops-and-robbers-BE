package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Notice", description = "공지사항 관리 API")
public interface NoticeControllerDocs {

    @Operation(summary = "공지사항 생성", description = "새로운 공지사항을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공")
    })
    ResponseEntity<NoticeResponse> createNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid NoticeCreateRequest request
    );

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 조회합니다. 고정 공지가 먼저, 이후 최신순으로 정렬됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<List<NoticeResponse>> getNoticeList(@Parameter(hidden = true) LoginUser loginUser);

    @Operation(summary = "공지사항 단건 조회", description = "특정 공지사항을 조회합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<NoticeResponse> getNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId
    );

    @Operation(summary = "공지사항 수정", description = "특정 공지사항을 수정합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    ResponseEntity<NoticeResponse> updateNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
    );

    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 삭제합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> deleteNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId
    );
}
