package com.user.demo.infrastructure.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de solicitud para inicio de sesión")
public class LoginRequestDto {

    @NotBlank(message = "El correo es obligatorio")
    @Schema(description = "Correo electrónico del usuario", example = "juan@rodriguez.org", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Schema(description = "Contraseña de acceso del usuario", example = "Hunter2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
