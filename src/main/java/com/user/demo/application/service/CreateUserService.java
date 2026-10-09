package com.user.demo.application.service;

import com.user.demo.application.dto.CreateUserCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.application.mapper.UserCommandMapper;
import com.user.demo.domain.exception.EmailAlreadyRegisteredException;
import com.user.demo.domain.exception.InvalidUserDataException;
import com.user.demo.domain.model.User;
import com.user.demo.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class CreateUserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserCommandMapper userCommandMapper;
    private final String passwordRegex;
    private final String emailRegex;

    public CreateUserService(
            UserRepository userRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            UserCommandMapper userCommandMapper,
            @Value("${app.security.password-regex}") String passwordRegex,
            @Value("${app.validation.email-regex}") String emailRegex) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.userCommandMapper = userCommandMapper;
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

        User user = userCommandMapper.toDomain(command, encodedPassword, token);
        User savedUser = userRepository.save(user);

        return userCommandMapper.toResult(savedUser);
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
