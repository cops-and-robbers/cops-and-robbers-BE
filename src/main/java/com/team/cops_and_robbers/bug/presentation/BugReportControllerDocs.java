package com.team.cops_and_robbers.bug.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.bug.presentation.dto.request.BugReportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Bug", description = "버그 제보 API")
public interface BugReportControllerDocs {

    @Operation(summary = "버그 제보", description = "버그 내용을 제보합니다.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "버그 제보 성공"))
    ResponseEntity<Void> createBugReport(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid BugReportRequest request
    );
}
