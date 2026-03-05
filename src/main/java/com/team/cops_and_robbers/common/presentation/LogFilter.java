package com.team.cops_and_robbers.common.presentation;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
@Profile({"dev", "prod"})
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        long start = System.currentTimeMillis();

        MDC.put("request_id", requestId);

        log.info("REQUEST | {} {}", method, uri);

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("RESPONSE | {} {} | STATUS: {} | {}ms", method, uri, httpResponse.getStatus(), elapsed);
            MDC.clear();
        }
    }
}
