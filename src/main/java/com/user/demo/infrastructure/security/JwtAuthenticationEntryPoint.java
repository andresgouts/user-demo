package com.user.demo.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.demo.infrastructure.controller.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error_message";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        String message = (String) request.getAttribute(AUTH_ERROR_ATTRIBUTE);
        if (message == null || message.isBlank()) {
            message = "Token de autenticación no proporcionado";
        }

        log.warn("Access denied (401 Unauthorized) on {} {}. Reason: '{}'. Exception: {}",
                request.getMethod(),
                request.getRequestURI(),
                message,
                authException != null ? authException.getMessage() : "none");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponseDto errorResponse = new ErrorResponseDto(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
