package com.user.demo.application.service;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.domain.exception.EmailAlreadyRegisteredException;
import com.user.demo.domain.exception.InvalidUserDataException;
import com.user.demo.domain.model.Phone;
import com.user.demo.domain.model.User;
import com.user.demo.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CreateUserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final String passwordRegex;
    private final String emailRegex;

    public CreateUserService(
            UserRepository userRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.password-regex}") String passwordRegex,
            @Value("${app.validation.email-regex}") String emailRegex) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.passwordRegex = passwordRegex;
        this.emailRegex = emailRegex;
    }

    public UserResult execute(CreateUserCommand command) {
        if (command == null) {
            throw new InvalidUserDataException("Los datos del usuario son requeridos");
        }

        validateEmail(command.getEmail());
        validatePassword(command.getPassword());

        if (userRepository.existsByEmail(command.getEmail())) {
            throw new EmailAlreadyRegisteredException();
        }

        String encodedPassword = passwordEncoder.encode(command.getPassword());
        String token = tokenService.generateToken(command.getEmail());
        LocalDateTime now = LocalDateTime.now();

        List<Phone> phones = command.getPhones() != null
                ? command.getPhones().stream()
                .map(p -> Phone.builder()
                        .id(UUID.randomUUID().toString())
                        .number(p.getNumber())
                        .cityCode(p.getCityCode())
                        .countryCode(p.getCountryCode())
                        .build())
                .toList()
                : Collections.emptyList();

        User user = User.builder()
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

        User savedUser = userRepository.save(user);

        return UserResult.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phones(command.getPhones())
                .created(savedUser.getCreated())
                .modified(savedUser.getModified())
                .lastLogin(savedUser.getLastLogin())
                .token(savedUser.getToken())
                .active(savedUser.isActive())
                .build();
    }

    private void validateEmail(String email) {
        if (email == null || !Pattern.matches(emailRegex, email)) {
            throw new InvalidUserDataException("El formato del correo es inválido");
        }
    }

    private void validatePassword(String password) {
        if (password == null || !Pattern.matches(passwordRegex, password)) {
            throw new InvalidUserDataException("El formato de la contraseña es inválido");
        }
    }
}
