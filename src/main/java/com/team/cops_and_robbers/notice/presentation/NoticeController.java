package com.team.cops_and_robbers.notice.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.notice.application.NoticeService;
import com.team.cops_and_robbers.notice.application.dto.result.NoticeResult;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeCreateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.request.NoticeUpdateRequest;
import com.team.cops_and_robbers.notice.presentation.dto.response.NoticeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
        NoticeResult result = noticeService.createNotice(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoticeResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNoticeList(@AuthUser LoginUser loginUser) {
        List<NoticeResponse> responses = noticeService.getNoticeList()
                .stream()
                .map(NoticeResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId
    ) {
        NoticeResult result = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(NoticeResponse.from(result));
    }

    @PatchMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
    ) {
        NoticeResult result = noticeService.updateNotice(request.toCommand(noticeId));
        return ResponseEntity.ok(NoticeResponse.from(result));
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @AuthUser LoginUser loginUser,
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
