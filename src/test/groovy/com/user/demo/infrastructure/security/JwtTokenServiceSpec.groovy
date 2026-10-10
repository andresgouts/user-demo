package com.user.demo.infrastructure.security

import spock.lang.Specification

class JwtTokenServiceSpec extends Specification {

    String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    long expirationMs = 3600000

    def "debe generar un token JWT valido no nulo para un subject dado"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)

        when:
        def token = jwtService.generateToken("usuario@dominio.cl")

        then:
        token != null
        !token.isEmpty()
        token.split("\\.").length == 3
    }

    def "debe validar exitosamente un token JWT valido"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)
        def token = jwtService.generateToken("usuario@dominio.cl")

        when:
        def isValid = jwtService.validateToken(token)

        then:
        isValid
    }

    def "debe retornar false al validar token nulo, vacio o con espacios"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)

        expect:
        !jwtService.validateToken(null)
        !jwtService.validateToken("")
        !jwtService.validateToken("   ")
    }

    def "debe retornar false al validar token mal formado"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)

        when:
        def isValid = jwtService.validateToken("token.invalido.malformado")

        then:
        !isValid
    }

    def "debe retornar false al validar token firmado con un secreto diferente"() {
        given:
        String otherSecret = "1234567890123456789012345678901234567890123456789012345678901234"
        def jwtService = new JwtTokenService(secret, expirationMs)
        def otherJwtService = new JwtTokenService(otherSecret, expirationMs)
        def tokenFromOther = otherJwtService.generateToken("usuario@dominio.cl")

        when:
        def isValid = jwtService.validateToken(tokenFromOther)

        then:
        !isValid
    }

    def "debe retornar false al validar token expirado"() {
        given:
        def expiredJwtService = new JwtTokenService(secret, -5000)
        def token = expiredJwtService.generateToken("usuario@dominio.cl")

        when:
        def isValid = expiredJwtService.validateToken(token)

        then:
        !isValid
    }

    def "debe extraer el subject correctamente de un token valido"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)
        def token = jwtService.generateToken("carlos@empresa.com")

        when:
        def subject = jwtService.extractSubject(token)

        then:
        subject == "carlos@empresa.com"
    }

    def "debe retornar null al extraer subject de token invalido"() {
        given:
        def jwtService = new JwtTokenService(secret, expirationMs)

        expect:
        jwtService.extractSubject(null) == null
        jwtService.extractSubject("token-invalido") == null
    }
}
