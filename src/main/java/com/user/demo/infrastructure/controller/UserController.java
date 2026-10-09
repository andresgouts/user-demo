package com.user.demo.infrastructure.controller;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.application.service.CreateUserService;
import com.user.demo.infrastructure.controller.dto.ErrorResponseDto;
import com.user.demo.infrastructure.controller.dto.UserRequestDto;
import com.user.demo.infrastructure.controller.dto.UserResponseDto;
import com.user.demo.infrastructure.controller.mapper.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para operaciones de registro y gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "API RESTful de creación y registro de usuarios")
public class UserController {

    private final CreateUserService createUserService;
    private final UserDtoMapper userDtoMapper;

    public UserController(CreateUserService createUserService, UserDtoMapper userDtoMapper) {
        this.createUserService = createUserService;
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * Registra un nuevo usuario en el sistema.
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
}
