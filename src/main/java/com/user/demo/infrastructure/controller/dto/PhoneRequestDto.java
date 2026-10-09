package com.user.demo.infrastructure.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PhoneRequestDto {

    @NotBlank(message = "El número de teléfono es obligatorio")
    private String number;

    @NotBlank(message = "El código de ciudad es obligatorio")
    @JsonProperty("citycode")
    @JsonAlias({"cityCode", "citycode"})
    private String cityCode;

    @NotBlank(message = "El código de país es obligatorio")
    @JsonProperty("contrycode")
    @JsonAlias({"countryCode", "countrycode", "contrycode"})
    private String countryCode;
}
