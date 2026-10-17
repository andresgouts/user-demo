package com.user.demo.infrastructure.security;

import com.user.demo.application.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        log.debug("Evaluating security for request: {} {}", method, requestUri);

        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
                log.debug("Authorization header does not start with '{}' for URI: {}", BEARER_PREFIX, requestUri);
            }

            String token = authorizationHeader.startsWith(BEARER_PREFIX)
                    ? authorizationHeader.substring(BEARER_PREFIX.length()).trim()
                    : authorizationHeader.trim();

            if (!token.isEmpty() && tokenService.validateToken(token)) {
                String subject = tokenService.extractSubject(token);
                log.debug("Authentication successful for subject '{}' on {} {}", subject, method, requestUri);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("Authentication failed: invalid or expired token for request {} {}", method, requestUri);
                request.setAttribute(
                        JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE,
                        "Token JWT inválido o expirado"
                );
            }
        } else {
            log.debug("No Authorization header provided for request {} {}", method, requestUri);
            request.setAttribute(
                    JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE,
                    "Token de autenticación no proporcionado"
            );
        }

        filterChain.doFilter(request, response);
    }
}
