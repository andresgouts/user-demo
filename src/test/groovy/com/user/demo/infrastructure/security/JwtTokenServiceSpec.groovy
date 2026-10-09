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
}
