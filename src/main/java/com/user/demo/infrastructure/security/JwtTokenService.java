package com.user.demo.infrastructure.security;

import com.user.demo.application.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        log.debug("Generating JWT token for subject: '{}', expires at: {}", subject, expiryDate);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("JWT validation failed: token is null or blank");
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            log.debug("JWT token validated successfully");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed: token has expired - {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT validation failed: malformed token - {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            log.warn("JWT validation failed: signature validation failed - {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT validation failed: unsupported token format - {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT validation failed: empty or illegal claims - {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT validation failed: unexpected error - {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String extractSubject(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Cannot extract subject: token is null or blank");
            return null;
        }
        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.debug("Subject extracted from JWT: '{}'", subject);
            return subject;
        } catch (ExpiredJwtException e) {
            log.warn("Cannot extract subject: token has expired - {}", e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Cannot extract subject from JWT: {}", e.getMessage());
            return null;
        }
    }
}
