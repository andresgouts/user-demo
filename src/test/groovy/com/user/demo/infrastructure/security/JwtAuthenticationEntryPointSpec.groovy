package com.user.demo.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import spock.lang.Specification

class JwtAuthenticationEntryPointSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()
    JwtAuthenticationEntryPoint entryPoint

    def setup() {
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper)
    }

    def "debe retornar 401 con mensaje por defecto cuando no hay atributo de error"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()

        when:
        entryPoint.commence(request, response, new BadCredentialsException("Sin credenciales"))

        then:
        response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
        response.getContentType().startsWith("application/json")
        response.getCharacterEncoding() == "UTF-8"
        def json = objectMapper.readTree(response.getContentAsString())
        json.get("mensaje").asText() == "Token de autenticación no proporcionado"
    }

    def "debe retornar 401 con mensaje personalizado si se establecio en el request"() {
        given:
        def request = new MockHttpServletRequest()
        def response = new MockHttpServletResponse()
        request.setAttribute(JwtAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE, "Token JWT inválido o expirado")

        when:
        entryPoint.commence(request, response, new BadCredentialsException("Token invalido"))

        then:
        response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED
        response.getContentType().startsWith("application/json")
        def json = objectMapper.readTree(response.getContentAsString())
        json.get("mensaje").asText() == "Token JWT inválido o expirado"
    }
}
