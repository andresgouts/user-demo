package com.user.demo.application.mapper;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.PhoneCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.domain.model.Phone;
import com.user.demo.domain.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class UserCommandMapper {

    public User toDomain(CreateUserCommand command, String encodedPassword, String token) {
        if (command == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Phone> phones = command.getPhones() != null
                ? command.getPhones().stream()
                .map(this::toPhoneDomain)
                .toList()
                : Collections.emptyList();

        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(command.getName())
                .email(command.getEmail())
                .password(encodedPassword)
                .phones(phones)
                .created(now)
                .modified(now)
                .lastLogin(now)
                .token(token)
                .active(true)
                .build();
    }

    public Phone toPhoneDomain(PhoneCommand phoneCommand) {
        if (phoneCommand == null) {
            return null;
        }
        return Phone.builder()
                .id(UUID.randomUUID().toString())
                .number(phoneCommand.getNumber())
                .cityCode(phoneCommand.getCityCode())
                .countryCode(phoneCommand.getCountryCode())
                .build();
    }

    public UserResult toResult(User user, List<PhoneCommand> phones) {
        if (user == null) {
            return null;
        }
        return UserResult.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phones(phones != null ? phones : Collections.emptyList())
                .created(user.getCreated())
                .modified(user.getModified())
                .lastLogin(user.getLastLogin())
                .token(user.getToken())
                .active(user.isActive())
                .build();
    }
}
