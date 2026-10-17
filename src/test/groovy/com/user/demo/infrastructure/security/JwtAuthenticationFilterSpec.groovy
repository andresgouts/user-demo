package com.user.demo.infrastructure.security

import com.user.demo.application.service.TokenService
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class JwtAuthenticationFilterSpec extends Specification {

    TokenService tokenService = Mock(TokenService)
    FilterChain filterChain = Mock(FilterChain)
    JwtAuthenticationFilter filter

    def setup() {
        filter = new JwtAuthenticationFilter(tokenService)
        SecurityContextHolder.clearContext()
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "debe autenticar en el contexto de seguridad cuando el token es valido"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()
        request.addHeader("Authorization", "Bearer valid.jwt.token")

        tokenService.validateToken("valid.jwt.token") >> true
        tokenService.extractSubject("valid.jwt.token") >> "user@test.com"

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() != null
        SecurityContextHolder.getContext().getAuthentication().getName() == "user@test.com"
        request.getAttribute(JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE) == null
    }

    def "debe marcar error cuando el token es invalido"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()
        request.addHeader("Authorization", "Bearer invalid.jwt.token")

        tokenService.validateToken("invalid.jwt.token") >> false

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() == null
        request.getAttribute(JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE) == "Token JWT inválido o expirado"
    }

    def "debe marcar error cuando no se proporciona cabecera Authorization"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        SecurityContextHolder.getContext().getAuthentication() == null
        request.getAttribute(JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE) == "Token de autenticación no proporcionado"
    }
}
