package com.user.demo.infrastructure.controller.mapper;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.PhoneCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.infrastructure.controller.dto.PhoneRequestDto;
import com.user.demo.infrastructure.controller.dto.UserRequestDto;
import com.user.demo.infrastructure.controller.dto.UserResponseDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserDtoMapper {

    public CreateUserCommand toCommand(UserRequestDto request) {
        if (request == null) {
            return null;
        }

        List<PhoneCommand> phoneCommands = request.getPhones() != null
                ? request.getPhones().stream()
                .map(this::toPhoneCommand)
                .toList()
                : Collections.emptyList();

        return CreateUserCommand.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phones(phoneCommands)
                .build();
    }

    public PhoneCommand toPhoneCommand(PhoneRequestDto phoneDto) {
        if (phoneDto == null) {
            return null;
        }
        return PhoneCommand.builder()
                .number(phoneDto.getNumber())
                .cityCode(phoneDto.getCityCode())
                .countryCode(phoneDto.getCountryCode())
                .build();
    }

    public UserResponseDto toResponse(UserResult result) {
        if (result == null) {
            return null;
        }
        return UserResponseDto.builder()
                .id(result.getId())
                .created(result.getCreated())
                .modified(result.getModified())
                .lastLogin(result.getLastLogin())
                .token(result.getToken())
                .active(result.isActive())
                .build();
    }
}
