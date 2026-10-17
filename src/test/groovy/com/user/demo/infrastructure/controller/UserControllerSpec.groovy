package com.user.demo.infrastructure.controller

import com.user.demo.application.dto.CreateUserCommand
import com.user.demo.application.dto.PhoneResult
import com.user.demo.application.dto.UserResult
import com.user.demo.application.service.CreateUserService
import com.user.demo.application.service.GetUserService
import com.user.demo.infrastructure.controller.dto.PhoneRequestDto
import com.user.demo.infrastructure.controller.dto.UserRequestDto
import com.user.demo.infrastructure.controller.dto.UserResponseDto
import com.user.demo.infrastructure.controller.mapper.UserDtoMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification

import java.time.LocalDateTime

class UserControllerSpec extends Specification {

    CreateUserService createUserService = Mock(CreateUserService)
    GetUserService getUserService = Mock(GetUserService)
    UserDtoMapper userDtoMapper = Mock(UserDtoMapper)
    UserController userController

    def setup() {
        userController = new UserController(createUserService, getUserService, userDtoMapper)
    }

    def "debe registrar usuario y retornar 201 Created sin requerir token"() {
        given:
        def request = UserRequestDto.builder()
                .name("Carlos Gomez")
                .email("carlos@empresa.com")
                .password("Clave123")
                .phones([new PhoneRequestDto("1234567", "1", "57")])
                .build()

        def command = Mock(CreateUserCommand)
        def result = Mock(UserResult)
        def expectedResponse = UserResponseDto.builder()
                .id("some-uuid")
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .token("jwt.token")
                .active(true)
                .build()

        userDtoMapper.toCommand(request) >> command
        createUserService.execute(command) >> result
        userDtoMapper.toResponse(result) >> expectedResponse

        when:
        def response = userController.createUser(request)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body == expectedResponse
    }

    def "debe retornar usuario por ID y 200 OK delegando al servicio"() {
        given:
        String userId = "user-uuid-123"
        def userResult = UserResult.builder()
                .id(userId)
                .name("Carlos Gomez")
                .email("carlos@empresa.com")
                .token("jwt.token")
                .created(LocalDateTime.now())
                .modified(LocalDateTime.now())
                .lastLogin(LocalDateTime.now())
                .active(true)
                .phones([PhoneResult.builder().number("1234567").cityCode("1").countryCode("57").build()])
                .build()

        def expectedResponse = UserResponseDto.builder()
                .id(userId)
                .name("Carlos Gomez")
                .email("carlos@empresa.com")
                .build()

        getUserService.execute(userId) >> userResult
        userDtoMapper.toDetailResponse(userResult) >> expectedResponse

        when:
        def response = userController.getUserById(userId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == expectedResponse
    }
}
