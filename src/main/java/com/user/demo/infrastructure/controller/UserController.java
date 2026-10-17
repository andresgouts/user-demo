package com.user.demo.infrastructure.controller;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.application.service.CreateUserService;
import com.user.demo.application.service.GetUserService;
import com.user.demo.infrastructure.controller.dto.UserRequestDto;
import com.user.demo.infrastructure.controller.dto.UserResponseDto;
import com.user.demo.infrastructure.controller.mapper.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para operaciones de registro y gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "API RESTful de creación, registro y consulta de usuarios")
public class UserController {

    private final CreateUserService createUserService;
    private final GetUserService getUserService;
    private final UserDtoMapper userDtoMapper;

    public UserController(
            CreateUserService createUserService,
            GetUserService getUserService,
            UserDtoMapper userDtoMapper) {
        this.createUserService = createUserService;
        this.getUserService = getUserService;
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * No requiere token de autenticación.
     *
     * @param request Datos del usuario a registrar (nombre, correo, contraseña y teléfonos).
     * @return {@link ResponseEntity} con los datos del usuario registrado y código HTTP 201 Created.
     */
    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Registra un usuario en la base de datos, valida formato de correo y contraseña, genera su token JWT de autenticación y persiste los datos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o formato incorrecto"),
            @ApiResponse(responseCode = "409", description = "El correo ya se ha registrado previamente")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        CreateUserCommand command = userDtoMapper.toCommand(request);
        UserResult result = createUserService.execute(command);
        UserResponseDto response = userDtoMapper.toResponse(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene los datos de un usuario por su identificador único (ID).
     * Requiere un token JWT válido enviado en la cabecera 'Authorization' (Bearer token),
     * validado previamente por los filtros de Spring Security.
     *
     * @param id Identificador único del usuario (UUID).
     * @return {@link ResponseEntity} con los datos detallados del usuario y código HTTP 200 OK.
     */
    @Operation(
            summary = "Obtener usuario por ID",
            description = "Recupera la información detallada de un usuario a partir de su ID único, validando previamente el token JWT proporcionado en la cabecera Authorization.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado exitosamente"),
            @ApiResponse(responseCode = "401", description = "Token JWT no suministrado, inválido o expirado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable String id) {
        UserResult result = getUserService.execute(id);
        UserResponseDto response = userDtoMapper.toDetailResponse(result);

        return ResponseEntity.ok(response);
    }
}
