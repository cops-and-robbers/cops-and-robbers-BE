package com.team.cops_and_robbers.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompExceptionHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper;

    /**
     * 클라이언트 메시지 처리 중 에러가 발생했을 때 호출되는 메서드
     */
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable exception = ex;
        if (ex instanceof MessageDeliveryException) {
            exception = ex.getCause();
        }

        if (exception instanceof ApplicationException appEx) {
            log.warn("[WebSocket Error] Status: {}, Title: {}, Detail: {}",
                    appEx.getCode().getHttpStatus().value(),
                    appEx.getCode().getTitle(),
                    appEx.getCode().getDetail());

            return prepareErrorMessage(appEx.getCode());
        }

        log.error("[WebSocket Unknown Error]", ex);
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    /**
     * 클라이언트에게 보낼 STOMP ERROR 프레임 생성
     */
    private Message<byte[]> prepareErrorMessage(ExceptionCode exceptionCode) {
        ErrorResponse errorResponse = ErrorResponse.of(exceptionCode, "STOMP");

        // 1. JSON 직렬화
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            log.error("[WebSocket Error] JSON Serialization Failed", e);
            jsonBody = String.format(
                    "{\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"STOMP\"}",
                    exceptionCode.getTitle(),
                    exceptionCode.getHttpStatus().value(),
                    exceptionCode.getDetail()
            );
        }

        // 2. STOMP Header 설정
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(exceptionCode.getTitle());
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(
                jsonBody.getBytes(StandardCharsets.UTF_8),
                accessor.getMessageHeaders()
        );
    }
}
