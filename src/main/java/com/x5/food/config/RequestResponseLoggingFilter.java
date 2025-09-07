package com.x5.food.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;

@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Логируем входящий запрос
        logRequest(req);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(req);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(res);

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // Логируем исходящий ответ
            logResponse(wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String path = request.getRequestURI() + (queryString != null ? "?" + queryString : "");

        log.info("REQUEST [{} {}] Headers: {}",
                request.getMethod(),
                path,
                getHeaders(request));
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        log.info("RESPOND [{} {}] Time: {}ms Headers: {}",
                response.getStatus(),
                getStatusMessage(response.getStatus()),
                duration,
                getResponseHeaders(response));
    }

    private String getHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames())
                .stream()
                .map(headerName -> headerName + ": " + request.getHeader(headerName))
                .reduce((a, b) -> a + "; " + b)
                .orElse("none");
    }

    private String getResponseHeaders(ContentCachingResponseWrapper response) {
        return response.getHeaderNames()
                .stream()
                .map(headerName -> headerName + ": " + response.getHeader(headerName))
                .reduce((a, b) -> a + "; " + b)
                .orElse("none");
    }

    private String getStatusMessage(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Status " + status;
        };
    }
}