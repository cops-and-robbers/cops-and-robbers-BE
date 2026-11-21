package com.team.cops_and_robbers.play.radio.presentation;

import com.team.cops_and_robbers.play.radio.application.RadioService;
import com.team.cops_and_robbers.play.radio.presentation.dto.request.RadioOverloadRequest;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/radio")
@RequiredArgsConstructor
public class InternalRadioController {

    private final RadioService radioService;

    /**
     *  Grafana의 무전 서버 과부하 알림을 받아 신규 무전 입장 차단을 on/off
     */
    @PostMapping("/overload")
    public ResponseEntity<Void> updateOverload(@Valid @RequestBody RadioOverloadRequest request) {
        if (request.isFiring()) {
            radioService.startOverload();
        } else {
            radioService.endOverload();
        }
        return ResponseEntity.noContent().build();
    }
}
