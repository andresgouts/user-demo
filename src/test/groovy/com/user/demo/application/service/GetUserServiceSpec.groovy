package com.user.demo.application.service

import com.user.demo.application.mapper.UserCommandMapper
import com.user.demo.domain.exception.InvalidUserDataException
import com.user.demo.domain.exception.UserNotFoundException
import com.user.demo.domain.model.Phone
import com.user.demo.domain.model.User
import com.user.demo.domain.repository.UserRepository
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class GetUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    UserCommandMapper userCommandMapper = new UserCommandMapper()

    @Subject
    GetUserService service = new GetUserService(userRepository, userCommandMapper)

    def "debe retornar UserResult cuando el usuario existe por ID"() {
        given: "un usuario en el repositorio"
        String userId = "550e8400-e29b-41d4-a716-446655440000"
        def now = LocalDateTime.now()
        def user = User.builder()
                .id(userId)
                .name("Ana Martinez")
                .email("ana@martinez.com")
                .password("encodedPassword")
                .phones([
                        Phone.builder().id("p1").number("123456").cityCode("1").countryCode("57").build()
                ])
                .created(now)
                .modified(now)
                .lastLogin(now)
                .token("jwt.token.test")
                .active(true)
                .build()

        and: "el mock responde con el usuario"
        userRepository.findById(userId) >> Optional.of(user)

        when: "se ejecuta el servicio"
        def result = service.execute(userId)

        then: "los datos corresponden al usuario esperado"
        result != null
        result.id == userId
        result.name == "Ana Martinez"
        result.email == "ana@martinez.com"
        result.token == "jwt.token.test"
        result.active
        result.phones.size() == 1
        result.phones[0].number == "123456"
    }

    def "debe lanzar UserNotFoundException cuando el ID no existe en el repositorio"() {
        given: "un id que no existe"
        String nonExistentId = "inexistente-uuid"
        userRepository.findById(nonExistentId) >> Optional.empty()

        when: "se ejecuta el servicio"
        service.execute(nonExistentId)

        then: "se lanza UserNotFoundException"
        def ex = thrown(UserNotFoundException)
        ex.message == "Usuario no encontrado con id: inexistente-uuid"
    }

    def "debe lanzar InvalidUserDataException cuando el ID es nulo o vacio"() {
        when:
        service.execute(idInvalido)

        then:
        def ex = thrown(InvalidUserDataException)
        ex.message == "El ID de usuario es obligatorio"

        where:
        idInvalido << [null, "", "   "]
    }
}
