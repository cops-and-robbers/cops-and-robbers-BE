package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.notice.application.NoticeService;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeDeleteCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeListCommand;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeTranslationsResult;
import com.team.cops_and_robbers.notice.domain.NoticeCategory;
import com.team.cops_and_robbers.notice.domain.NoticeLanguage;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeListResponse;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeResponse;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeTranslationsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController implements NoticeControllerDocs {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid NoticeCreateRequest request
    ) {
        NoticeResult result = noticeService.createNotice(request.toCommand(loginUser.userId()));
        NoticeResponse response = NoticeResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<NoticeListResponse> getNoticeList(
            @AuthUser LoginUser loginUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) NoticeCategory category,
            @RequestParam(defaultValue = "ko") String language
    ) {
        NoticeListCommand command = NoticeListCommand.of(page, size, category, toLanguage(language));
        Page<NoticeResult> result = noticeService.getNoticeList(command);
        NoticeListResponse response = NoticeListResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId,
            @RequestParam(defaultValue = "ko") String language
    ) {
        NoticeResult result = noticeService.getNotice(noticeId, toLanguage(language));
        NoticeResponse response = NoticeResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{noticeId}/translations")
    public ResponseEntity<NoticeTranslationsResponse> getNoticeTranslations(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId
    ) {
        NoticeTranslationsResult result = noticeService.getNoticeTranslations(loginUser.userId(), noticeId);
        NoticeTranslationsResponse response = NoticeTranslationsResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
    ) {
        NoticeResult result = noticeService.updateNotice(request.toCommand(loginUser.userId(), noticeId));
        NoticeResponse response = NoticeResponse.from(result);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(new NoticeDeleteCommand(loginUser.userId(), noticeId));
        return ResponseEntity.noContent().build();
    }

    /** 지원하지 않는 언어는 400 대신 ko 로 처리한다. 언어를 보내지 않는 기존 앱의 하위 호환이기도 하다. */
    private NoticeLanguage toLanguage(String language) {
        return NoticeLanguage.from(language).orElse(NoticeLanguage.KO);
    }
}
