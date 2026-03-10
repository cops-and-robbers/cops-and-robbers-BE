package com.team.cops_and_robbers.common;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;

public class StompFrameHandlerImpl<T> implements StompFrameHandler {

    private final Class<T> type;
    private final BlockingQueue<T> queue;

    public StompFrameHandlerImpl(Class<T> type, BlockingQueue<T> queue) {
        this.type = type;
        this.queue = queue;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return type;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        queue.offer((T) payload);
    }
}
