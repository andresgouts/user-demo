package com.user.demo.application.service

import com.user.demo.application.dto.CreateUserCommand
import com.user.demo.application.dto.PhoneCommand
import com.user.demo.application.mapper.UserCommandMapper
import com.user.demo.domain.exception.EmailAlreadyRegisteredException
import com.user.demo.domain.exception.InvalidUserDataException
import com.user.demo.domain.model.User
import com.user.demo.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject

class CreateUserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    TokenService tokenService = Mock()
    PasswordEncoder passwordEncoder = Mock()
    UserCommandMapper userCommandMapper = new UserCommandMapper()

    String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}\$"
    String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"

    @Subject
    CreateUserService service = new CreateUserService(
            userRepository,
            tokenService,
            passwordEncoder,
            userCommandMapper,
            passwordRegex,
            emailRegex
    )

    def "debe registrar un usuario exitosamente cuando los datos son válidos"() {
        given: "un comando válido con teléfonos"
        def command = CreateUserCommand.builder()
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .password("Hunter2")
                .phones([
                        new PhoneCommand("1234567", "1", "57")
                ])
                .build()

        and: "mocks configurados"
        userRepository.existsByEmail("juan@rodriguez.org") >> false
        passwordEncoder.encode("Hunter2") >> "hashedPassword123"
        tokenService.generateToken("juan@rodriguez.org") >> "jwt.token.mock"
        userRepository.save(_ as User) >> { User u -> u }

        when: "se ejecuta el servicio"
        def result = service.execute(command)

        then: "el resultado contiene los datos esperados"
        result != null
        result.id != null
        result.name == "Juan Rodriguez"
        result.email == "juan@rodriguez.org"
        result.token == "jwt.token.mock"
        result.active
        result.created != null
        result.modified != null
        result.lastLogin != null
        result.phones.size() == 1
        result.phones[0].number == "1234567"
    }

    def "debe lanzar EmailAlreadyRegisteredException si el correo ya existe en base de datos"() {
        given: "un comando con correo existente"
        def command = CreateUserCommand.builder()
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .password("Hunter2")
                .build()

        and: "el repositorio indica que el correo existe"
        userRepository.existsByEmail("juan@rodriguez.org") >> true

        when: "se ejecuta el servicio"
        service.execute(command)

        then: "se lanza EmailAlreadyRegisteredException con el mensaje requerido"
        def ex = thrown(EmailAlreadyRegisteredException)
        ex.message == "El correo ya se ha registrado previamente"
        0 * userRepository.save(_)
    }

    def "debe lanzar InvalidUserDataException si el correo no cumple con la regex"() {
        given: "un comando con correo con formato inválido"
        def command = CreateUserCommand.builder()
                .name("Juan Rodriguez")
                .email(emailInvalido)
                .password("Hunter2")
                .build()

        when: "se ejecuta el servicio"
        service.execute(command)

        then: "se lanza InvalidUserDataException"
        def ex = thrown(InvalidUserDataException)
        ex.message == "El formato del correo es inválido"

        where:
        emailInvalido << ["correo-invalido", "juan@", "@rodriguez.org", "juan@rodriguez"]
    }

    def "debe lanzar InvalidUserDataException si la contraseña no cumple con la regex configurable"() {
        given: "un comando con contraseña inválida según la expresión regular"
        def command = CreateUserCommand.builder()
                .name("Juan Rodriguez")
                .email("juan@rodriguez.org")
                .password(passwordInvalida)
                .build()

        when: "se ejecuta el servicio"
        service.execute(command)

        then: "se lanza InvalidUserDataException"
        def ex = thrown(InvalidUserDataException)
        ex.message == "El formato de la contraseña es inválido"

        where:
        passwordInvalida << ["hunter", "12345", "HUNTER2", "hunter2", ""]
    }
}
