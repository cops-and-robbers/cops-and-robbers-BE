package com.team.cops_and_robbers.report.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.report.application.ReportService;
import com.team.cops_and_robbers.report.application.dto.command.ReportCommand;
import com.team.cops_and_robbers.report.presentation.dto.request.ReportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

    private final ReportService reportService;

    @PostMapping("/chat")
    public ResponseEntity<Void> reportChat(
            @AuthUser LoginUser loginUser,
            @RequestBody @Valid ReportRequest request
    ) {
        ReportCommand command = ReportCommand.of(loginUser.userId(), request);
        reportService.reportChat(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
