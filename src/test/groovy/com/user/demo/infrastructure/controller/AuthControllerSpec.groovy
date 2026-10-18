package com.user.demo.infrastructure.controller

import com.user.demo.application.dto.LoginCommand
import com.user.demo.application.dto.PhoneResult
import com.user.demo.application.dto.UserResult
import com.user.demo.application.service.LoginUserService
import com.user.demo.infrastructure.controller.dto.LoginRequestDto
import com.user.demo.infrastructure.controller.dto.PhoneResponseDto
import com.user.demo.infrastructure.controller.dto.UserResponseDto
import com.user.demo.infrastructure.controller.mapper.UserDtoMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification

import java.time.LocalDateTime

class AuthControllerSpec extends Specification {

    LoginUserService loginUserService = Mock(LoginUserService)
    UserDtoMapper userDtoMapper = Mock(UserDtoMapper)
    AuthController authController

    def setup() {
        authController = new AuthController(loginUserService, userDtoMapper)
    }

    def "debe autenticar al usuario y retornar 200 OK con respuesta completa del usuario"() {
        given: "una solicitud de login"
        def request = LoginRequestDto.builder()
                .email("juan@rodriguez.org")
                .password("Hunter2")
                .build()

        def command = Mock(LoginCommand)
        def result = UserResult.builder()
                .id("uuid-123")
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .token("jwt.new.token")
                .active(true)
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .phones([PhoneResult.builder().number("1234567").cityCode("1").countryCode("57").build()])
                .build()

        def expectedResponse = UserResponseDto.builder()
                .id("uuid-123")
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .token("jwt.new.token")
                .active(true)
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .phones([PhoneResponseDto.builder().number("1234567").cityCode("1").countryCode("57").build()])
                .build()

        userDtoMapper.toLoginCommand(request) >> command
        loginUserService.execute(command) >> result
        userDtoMapper.toDetailResponse(result) >> expectedResponse

        when: "se ejecuta el método login del controlador"
        def response = authController.login(request)

        then: "el status es 200 OK y el cuerpo contiene los datos completos"
        response.statusCode == HttpStatus.OK
        response.body == expectedResponse
        response.body.token == "jwt.new.token"
        response.body.name == "Juan Rodriguez"
        response.body.phones.size() == 1
    }
}
