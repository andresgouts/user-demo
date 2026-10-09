package com.user.demo.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.demo.infrastructure.controller.GlobalExceptionHandler;
import com.user.demo.infrastructure.controller.UserController;
import com.user.demo.infrastructure.controller.dto.PhoneRequestDto;
import com.user.demo.infrastructure.controller.dto.UserRequestDto;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class UserRegistrationStepDefinitions {

    @Autowired
    private UserController userController;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UserRequestDto currentRequest;
    private MvcResult currentResult;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Dado("que tengo los datos válidos de un usuario con nombre {string}, correo {string}, clave {string} y teléfonos")
    public void queTengoLosDatosValidosDeUnUsuario(String name, String email, String password) {
        List<PhoneRequestDto> phones = new ArrayList<>();
        phones.add(PhoneRequestDto.builder()
                .number("1234567")
                .cityCode("1")
                .countryCode("57")
                .build());

        currentRequest = UserRequestDto.builder()
                .name(name)
                .email(email)
                .password(password)
                .phones(phones)
                .build();
    }

    @Cuando("envío una solicitud POST a {string} con los datos del usuario")
    public void envioUnaSolicitudPostConLosDatos(String endpoint) throws Exception {
        currentResult = mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(currentRequest)))
                .andReturn();
    }

    @Entonces("el código de respuesta debe ser {int}")
    public void elCodigoDeRespuestaDebeSer(int expectedStatusCode) {
        assertNotNull(currentResult);
        assertEquals(expectedStatusCode, currentResult.getResponse().getStatus());
    }

    @Y("la respuesta debe contener un id generado en formato UUID")
    public void laRespuestaDebeContenerUnIdEnFormatoUUID() throws Exception {
        JsonNode root = objectMapper.readTree(currentResult.getResponse().getContentAsString());
        assertTrue(root.has("id"));
        String id = root.get("id").asText();
        assertNotNull(id);
        assertFalse(id.isBlank());
        assertEquals(36, id.length());
    }

    @Y("la respuesta debe contener un token JWT no vacío")
    public void laRespuestaDebeContenerUnTokenJwtNoVacio() throws Exception {
        JsonNode root = objectMapper.readTree(currentResult.getResponse().getContentAsString());
        assertTrue(root.has("token"));
        String token = root.get("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(3, token.split("\\.").length);
    }

    @Y("el campo isactive debe ser verdadero")
    public void elCampoIsactiveDebeSerVerdadero() throws Exception {
        JsonNode root = objectMapper.readTree(currentResult.getResponse().getContentAsString());
        assertTrue(root.has("isactive"));
        assertTrue(root.get("isactive").asBoolean());
    }

    @Y("los campos created, modified y last_login deben estar presentes")
    public void losCamposDeAuditoriaDebenEstarPresentes() throws Exception {
        JsonNode root = objectMapper.readTree(currentResult.getResponse().getContentAsString());
        assertTrue(root.has("created"));
        assertTrue(root.has("modified"));
        assertTrue(root.has("last_login"));
    }

    @Dado("que ya existe un usuario registrado con correo {string}")
    public void queYaExisteUnUsuarioRegistradoConCorreo(String email) throws Exception {
        UserRequestDto request = UserRequestDto.builder()
                .name("Usuario Previo")
                .email(email)
                .password("Clave123")
                .phones(List.of(new PhoneRequestDto("999999", "1", "57")))
                .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Cuando("intento registrar otro usuario con el mismo correo {string} y clave {string}")
    public void intentoRegistrarOtroUsuarioConElMismoCorreo(String email, String password) throws Exception {
        currentRequest = UserRequestDto.builder()
                .name("Segundo Usuario")
                .email(email)
                .password(password)
                .phones(List.of(new PhoneRequestDto("888888", "1", "57")))
                .build();

        currentResult = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(currentRequest)))
                .andReturn();
    }

    @Y("el mensaje de error debe ser {string}")
    public void elMensajeDeErrorDebeSer(String mensajeEsperado) throws Exception {
        JsonNode root = objectMapper.readTree(currentResult.getResponse().getContentAsString());
        assertTrue(root.has("mensaje"), "La respuesta debe contener la clave 'mensaje': " + currentResult.getResponse().getContentAsString());
        assertEquals(mensajeEsperado, root.get("mensaje").asText());
    }

    @Dado("que tengo un usuario con correo inválido {string} y clave {string}")
    public void queTengoUnUsuarioConCorreoInvalido(String email, String password) {
        currentRequest = UserRequestDto.builder()
                .name("Usuario Email Invalido")
                .email(email)
                .password(password)
                .phones(new ArrayList<>())
                .build();
    }

    @Dado("que tengo un usuario con correo {string} y contraseña inválida {string}")
    public void queTengoUnUsuarioConContrasenaInvalida(String email, String password) {
        currentRequest = UserRequestDto.builder()
                .name("Usuario Clave Invalida")
                .email(email)
                .password(password)
                .phones(new ArrayList<>())
                .build();
    }
}
