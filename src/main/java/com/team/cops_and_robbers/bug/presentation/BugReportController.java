package com.team.cops_and_robbers.bug.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.bug.application.BugReportService;
import com.team.cops_and_robbers.bug.presentation.dto.request.BugReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bugs")
@RequiredArgsConstructor
public class BugReportController implements BugReportControllerDocs {

    private final BugReportService bugReportService;

    @PostMapping
    public ResponseEntity<Void> createBugReport(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid BugReportRequest request
    ) {
        bugReportService.createBugReport(request.toCommand(loginUser.userId()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
