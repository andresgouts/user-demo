package com.user.demo.infrastructure.controller;

import com.user.demo.application.dto.LoginCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.application.service.LoginUserService;
import com.user.demo.infrastructure.controller.dto.LoginRequestDto;
import com.user.demo.infrastructure.controller.dto.UserResponseDto;
import com.user.demo.infrastructure.controller.mapper.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para operaciones de autenticación y acceso.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "API RESTful para inicio de sesión y gestión de credenciales")
public class AuthController {

    private final LoginUserService loginUserService;
    private final UserDtoMapper userDtoMapper;

    public AuthController(LoginUserService loginUserService, UserDtoMapper userDtoMapper) {
        this.loginUserService = loginUserService;
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * Autentica a un usuario registrado mediante su correo y contraseña.
     *
     * @param request Datos de acceso (correo y contraseña).
     * @return {@link ResponseEntity} con la información completa del usuario autenticado, nuevo token y HTTP 200 OK.
     */
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica al usuario con correo y contraseña, renueva el token JWT, actualiza la fecha de último acceso (last_login) y retorna el perfil completo del usuario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o faltantes"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo")
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginCommand command = userDtoMapper.toLoginCommand(request);
        UserResult result = loginUserService.execute(command);
        UserResponseDto response = userDtoMapper.toDetailResponse(result);

        return ResponseEntity.ok(response);
    }
}
