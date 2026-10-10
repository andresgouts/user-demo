package com.user.demo.infrastructure.controller.mapper

import com.user.demo.application.dto.PhoneResult
import com.user.demo.application.dto.UserResult
import com.user.demo.infrastructure.controller.dto.PhoneRequestDto
import com.user.demo.infrastructure.controller.dto.UserRequestDto
import spock.lang.Specification

import java.time.LocalDateTime

class UserDtoMapperSpec extends Specification {

    UserDtoMapper mapper = new UserDtoMapper()

    def "debe mapear UserRequestDto a CreateUserCommand correctamente"() {
        given: "un UserRequestDto con telefonos"
        def request = UserRequestDto.builder()
                .name("Carlos Gomez")
                .email("carlos@gomez.com")
                .password("Clave123")
                .phones([
                        new PhoneRequestDto("1234567", "1", "57")
                ])
                .build()

        when: "se transforma a CreateUserCommand"
        def command = mapper.toCommand(request)

        then: "los campos coinciden con los del DTO de entrada"
        command != null
        command.name == "Carlos Gomez"
        command.email == "carlos@gomez.com"
        command.password == "Clave123"
        command.phones.size() == 1
        command.phones[0].number == "1234567"
        command.phones[0].cityCode == "1"
        command.phones[0].countryCode == "57"
    }

    def "debe mapear UserResult a UserResponseDto correctamente"() {
        given: "un UserResult con datos de usuario"
        def now = LocalDateTime.now()
        def result = UserResult.builder()
                .id("123e4567-e89b-12d3-a456-426614174000")
                .name("Carlos Gomez")
                .email("carlos@gomez.com")
                .created(now)
                .modified(now)
                .lastLogin(now)
                .token("jwt.token.test")
                .active(true)
                .build()

        when: "se transforma a UserResponseDto"
        def response = mapper.toResponse(result)

        then: "los campos del DTO de respuesta coinciden"
        response != null
        response.id == "123e4567-e89b-12d3-a456-426614174000"
        response.created == now
        response.modified == now
        response.lastLogin == now
        response.token == "jwt.token.test"
        response.active
    }

    def "debe mapear UserResult a UserResponseDto con detalle completo"() {
        given: "un UserResult con teléfonos"
        def now = LocalDateTime.now()
        def result = UserResult.builder()
                .id("123e4567-e89b-12d3-a456-426614174000")
                .name("Carlos Gomez")
                .email("carlos@gomez.com")
                .phones([
                        PhoneResult.builder().id("p1").number("1234567").cityCode("1").countryCode("57").build()
                ])
                .created(now)
                .modified(now)
                .lastLogin(now)
                .token("jwt.token.test")
                .active(true)
                .build()

        when: "se transforma a detalle"
        def response = mapper.toDetailResponse(result)

        then: "contiene todos los campos detallados"
        response != null
        response.id == "123e4567-e89b-12d3-a456-426614174000"
        response.name == "Carlos Gomez"
        response.email == "carlos@gomez.com"
        response.phones.size() == 1
        response.phones[0].number == "1234567"
        response.phones[0].cityCode == "1"
        response.phones[0].countryCode == "57"
        response.created == now
        response.modified == now
        response.lastLogin == now
        response.token == "jwt.token.test"
        response.active
    }

    def "debe retornar null cuando los objetos de entrada son nulos"() {
        expect:
        mapper.toCommand(null) == null
        mapper.toResponse(null) == null
        mapper.toDetailResponse(null) == null
        mapper.toPhoneCommand(null) == null
        mapper.toPhoneResponseDto(null) == null
    }
}
