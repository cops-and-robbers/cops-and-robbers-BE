package com.team.cops_and_robbers.community.chat.pin.presentation;

import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.community.chat.pin.application.CommunityChatPinService;
import com.team.cops_and_robbers.community.chat.pin.application.dto.command.CommunityChatPinDeleteCommand;
import com.team.cops_and_robbers.community.chat.pin.application.dto.result.CommunityChatPinResult;
import com.team.cops_and_robbers.community.chat.pin.presentation.dto.request.CommunityChatPinRegisterRequest;
import com.team.cops_and_robbers.community.chat.pin.presentation.dto.request.CommunityChatPinUpdateRequest;
import com.team.cops_and_robbers.community.chat.pin.presentation.dto.response.CommunityChatPinResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community-posts/{postId}/chat/pin")
@RequiredArgsConstructor
public class CommunityChatPinController implements CommunityChatPinControllerDocs {

    private final CommunityChatPinService communityChatPinService;

    @PostMapping
    public ResponseEntity<CommunityChatPinResponse> register(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityChatPinRegisterRequest request
    ) {
        CommunityChatPinResult result = communityChatPinService.register(request.toCommand(loginUser.userId(), postId));
        return ResponseEntity.status(HttpStatus.CREATED).body(CommunityChatPinResponse.from(result));
    }

    @PutMapping
    public ResponseEntity<CommunityChatPinResponse> update(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityChatPinUpdateRequest request
    ) {
        CommunityChatPinResult result = communityChatPinService.update(request.toCommand(loginUser.userId(), postId));
        return ResponseEntity.ok(CommunityChatPinResponse.from(result));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        communityChatPinService.delete(CommunityChatPinDeleteCommand.of(loginUser.userId(), postId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CommunityChatPinResponse> get(
            @AuthUser LoginUser loginUser,
            @PathVariable Long postId
    ) {
        CommunityChatPinResult result = communityChatPinService.get(postId, loginUser.userId());
        return ResponseEntity.ok(CommunityChatPinResponse.from(result));
    }
}
