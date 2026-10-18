package com.user.demo.application.service

import com.user.demo.application.dto.LoginCommand
import com.user.demo.application.mapper.UserCommandMapper
import com.user.demo.domain.exception.InvalidCredentialsException
import com.user.demo.domain.exception.InvalidUserDataException
import com.user.demo.domain.model.Phone
import com.user.demo.domain.model.User
import com.user.demo.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class LoginUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    PasswordEncoder passwordEncoder = Mock()
    TokenService tokenService = Mock()
    UserCommandMapper userCommandMapper = new UserCommandMapper()

    @Subject
    LoginUserService service = new LoginUserService(
            userRepository,
            passwordEncoder,
            tokenService,
            userCommandMapper
    )

    def "debe autenticar exitosamente y retornar usuario con nuevo token y lastLogin actualizado"() {
        given: "un comando de login con credenciales válidas"
        def command = LoginCommand.builder()
                .email("juan@rodriguez.org")
                .password("Hunter2")
                .build()

        and: "un usuario existente en la base de datos"
        def oldLastLogin = LocalDateTime.now().minusDays(1)
        def existingUser = User.builder()
                .id("user-uuid-123")
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .password("encodedPassword123")
                .created(LocalDateTime.now().minusDays(10))
                .modified(LocalDateTime.now().minusDays(5))
                .lastLogin(oldLastLogin)
                .token("old.jwt.token")
                .active(true)
                .phones([
                        Phone.builder().id("p1").number("1234567").cityCode("1").countryCode("57").build()
                ])
                .build()

        and: "comportamiento de los mocks"
        userRepository.findByEmail("juan@rodriguez.org") >> Optional.of(existingUser)
        passwordEncoder.matches("Hunter2", "encodedPassword123") >> true
        tokenService.generateToken("juan@rodriguez.org") >> "new.jwt.token"

        when: "se ejecuta el login"
        def result = service.execute(command)

        then: "se persiste la actualización y se retorna el resultado con datos completos y actualizados"
        1 * userRepository.save({ User u ->
            u.token == "new.jwt.token" && u.lastLogin != oldLastLogin
        }) >> { User u -> u }
        result != null
        result.id == "user-uuid-123"
        result.name == "Juan Rodriguez"
        result.email == "juan@rodriguez.org"
        result.token == "new.jwt.token"
        result.active
        result.lastLogin != oldLastLogin
        result.phones.size() == 1
        result.phones[0].number == "1234567"
    }

    def "debe lanzar InvalidCredentialsException si el usuario no existe"() {
        given: "un comando con correo inexistente"
        def command = LoginCommand.builder()
                .email("noexiste@empresa.com")
                .password("Hunter2")
                .build()

        and: "el repositorio no encuentra el correo"
        userRepository.findByEmail("noexiste@empresa.com") >> Optional.empty()

        when: "se ejecuta el login"
        service.execute(command)

        then: "se lanza InvalidCredentialsException"
        def ex = thrown(InvalidCredentialsException)
        ex.message == "Credenciales inválidas"
        0 * passwordEncoder.matches(_, _)
        0 * tokenService.generateToken(_)
        0 * userRepository.save(_)
    }

    def "debe lanzar InvalidCredentialsException si la contraseña no coincide"() {
        given: "un comando con contraseña errónea"
        def command = LoginCommand.builder()
                .email("juan@rodriguez.org")
                .password("ClaveErronea")
                .build()

        and: "un usuario existente en base de datos"
        def existingUser = User.builder()
                .id("user-uuid-123")
                .email("juan@rodriguez.org")
                .password("encodedPassword123")
                .active(true)
                .build()

        userRepository.findByEmail("juan@rodriguez.org") >> Optional.of(existingUser)
        passwordEncoder.matches("ClaveErronea", "encodedPassword123") >> false

        when: "se ejecuta el login"
        service.execute(command)

        then: "se lanza InvalidCredentialsException"
        def ex = thrown(InvalidCredentialsException)
        ex.message == "Credenciales inválidas"
        0 * tokenService.generateToken(_)
        0 * userRepository.save(_)
    }

    def "debe lanzar InvalidCredentialsException si el usuario está inactivo"() {
        given: "un usuario inactivo"
        def command = LoginCommand.builder()
                .email("inactivo@empresa.com")
                .password("Hunter2")
                .build()

        def inactiveUser = User.builder()
                .id("user-uuid-999")
                .email("inactivo@empresa.com")
                .password("encodedPassword123")
                .active(false)
                .build()

        userRepository.findByEmail("inactivo@empresa.com") >> Optional.of(inactiveUser)
        passwordEncoder.matches("Hunter2", "encodedPassword123") >> true

        when: "se ejecuta el login"
        service.execute(command)

        then: "se lanza InvalidCredentialsException indicando que está inactivo"
        def ex = thrown(InvalidCredentialsException)
        ex.message == "El usuario se encuentra inactivo"
        0 * tokenService.generateToken(_)
        0 * userRepository.save(_)
    }

    def "debe lanzar InvalidUserDataException si el comando o sus campos son nulos"() {
        when:
        service.execute(command)

        then:
        thrown(InvalidUserDataException)

        where:
        command << [
                null,
                LoginCommand.builder().email(null).password("pass").build(),
                LoginCommand.builder().email("email@test.com").password(null).build()
        ]
    }
}
