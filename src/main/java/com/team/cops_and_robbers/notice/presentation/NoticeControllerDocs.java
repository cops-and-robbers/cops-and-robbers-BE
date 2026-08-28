package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.exception.NoticeException;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeListResponse;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeResponse;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeTranslationsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notice", description = "공지사항 관리 API")
public interface NoticeControllerDocs {

    @Operation(summary = "공지사항 생성",
            description = "새로운 공지사항을 생성합니다. 언어별 제목·내용을 translations 배열로 함께 저장하며, "
                    + "originalLanguage 언어의 번역이 반드시 포함되어야 합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"DUPLICATE_TRANSLATION_LANGUAGE", "MISSING_ORIGINAL_TRANSLATION"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공")
    })
    ResponseEntity<NoticeResponse> createNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid NoticeCreateRequest request
    );

    @Operation(summary = "공지사항 목록 조회",
            description = "공지사항 목록을 조회합니다. 고정 공지가 먼저, 이후 최신순으로 정렬됩니다. "
                    + "요청한 언어의 번역이 없으면 원문 언어 → 아무 번역 순으로 대체되며, "
                    + "실제 내려간 언어는 응답의 language 필드로 확인할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<NoticeListResponse> getNoticeList(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1부터~)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "카테고리 필터 (생략 시 전체 조회)") @RequestParam(required = false) NoticeCategory category,
            @Parameter(description = "응답 언어 코드 (ko·ja·en, 생략 또는 미지원 값이면 ko)", example = "ja") @RequestParam(defaultValue = "ko") String language
    );

    @Operation(summary = "공지사항 단건 조회",
            description = "특정 공지사항을 조회합니다. 언어 대체 규칙은 목록 조회와 같습니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<NoticeResponse> getNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId,
            @Parameter(description = "응답 언어 코드 (ko·ja·en, 생략 또는 미지원 값이면 ko)", example = "ja") @RequestParam(defaultValue = "ko") String language
    );

    @Operation(summary = "공지사항 번역 전체 조회 (관리자)",
            description = "어드민 편집 화면용. 언어 대체 없이 저장된 번역 전체를 내려줍니다. 관리자만 호출할 수 있습니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND", "FORBIDDEN_ADMIN_ONLY"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<NoticeTranslationsResponse> getNoticeTranslations(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId
    );

    @Operation(summary = "공지사항 수정",
            description = "특정 공지사항 전체를 수정합니다. translations 는 기존 번역을 통째로 대체합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND", "DUPLICATE_TRANSLATION_LANGUAGE", "MISSING_ORIGINAL_TRANSLATION"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    ResponseEntity<NoticeResponse> updateNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
    );

    @Operation(summary = "공지사항 삭제", description = "특정 공지사항을 번역과 함께 삭제합니다.")
    @ApiErrorCode(value = NoticeException.class, codes = {"NOTICE_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> deleteNotice(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long noticeId
    );
}
