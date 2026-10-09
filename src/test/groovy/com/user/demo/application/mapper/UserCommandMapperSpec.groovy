package com.user.demo.application.mapper

import com.user.demo.application.dto.CreateUserCommand
import com.user.demo.application.dto.PhoneCommand
import com.user.demo.domain.model.Phone
import com.user.demo.domain.model.User
import spock.lang.Specification

import java.time.LocalDateTime

class UserCommandMapperSpec extends Specification {

    UserCommandMapper mapper = new UserCommandMapper()

    def "debe transformar CreateUserCommand a entidad User de dominio correctamente"() {
        given: "un comando con datos y telefonos"
        def command = CreateUserCommand.builder()
                .name("Pedro Perez")
                .email("pedro@perez.com")
                .password("Password123")
                .phones([
                        new PhoneCommand("9876543", "2", "56")
                ])
                .build()

        when: "se mapea a dominio con clave codificada y token"
        def user = mapper.toDomain(command, "hashedPass", "jwt.token.sample")

        then: "los campos y colecciones se mapean fielmente"
        user != null
        user.id != null
        user.name == "Pedro Perez"
        user.email == "pedro@perez.com"
        user.password == "hashedPass"
        user.token == "jwt.token.sample"
        user.active
        user.created != null
        user.modified != null
        user.lastLogin != null
        user.phones.size() == 1
        user.phones[0].id != null
        user.phones[0].number == "9876543"
        user.phones[0].cityCode == "2"
        user.phones[0].countryCode == "56"
    }

    def "debe transformar entidad User a UserResult correctamente"() {
        given: "un User de dominio"
        def now = LocalDateTime.now()
        def user = User.builder()
                .id("sample-uuid")
                .name("Pedro Perez")
                .email("pedro@perez.com")
                .token("jwt.token.sample")
                .active(true)
                .created(now)
                .modified(now)
                .lastLogin(now)
                .phones([
                        Phone.builder().id("phone-uuid").number("9876543").cityCode("2").countryCode("56").build()
                ])
                .build()

        when: "se mapea a UserResult"
        def result = mapper.toResult(user)

        then: "el UserResult contiene los datos esperados"
        result != null
        result.id == "sample-uuid"
        result.name == "Pedro Perez"
        result.email == "pedro@perez.com"
        result.token == "jwt.token.sample"
        result.active
        result.created == now
        result.modified == now
        result.lastLogin == now
        result.phones.size() == 1
        result.phones[0].id == "phone-uuid"
        result.phones[0].number == "9876543"
        result.phones[0].cityCode == "2"
        result.phones[0].countryCode == "56"
    }

    def "debe retornar null cuando los argumentos de entrada son nulos"() {
        expect:
        mapper.toDomain(null, "pass", "token") == null
        mapper.toResult(null) == null
        mapper.toPhoneDomain(null) == null
        mapper.toPhoneResult(null) == null
    }
}
