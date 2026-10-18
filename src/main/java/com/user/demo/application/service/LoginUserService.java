package com.user.demo.application.service;

import com.user.demo.application.dto.LoginCommand;
import com.user.demo.application.dto.UserResult;
import com.user.demo.application.mapper.UserCommandMapper;
import com.user.demo.domain.exception.InvalidCredentialsException;
import com.user.demo.domain.exception.InvalidUserDataException;
import com.user.demo.domain.model.User;
import com.user.demo.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginUserService {

    private static final Logger log = LoggerFactory.getLogger(LoginUserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserCommandMapper userCommandMapper;

    public LoginUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            UserCommandMapper userCommandMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userCommandMapper = userCommandMapper;
    }

    public UserResult execute(LoginCommand command) {
        log.info("INI: Inicio login usuario con email {}", command != null ? command.getEmail() : null);
        if (command == null || command.getEmail() == null || command.getPassword() == null) {
            log.error("FIN_ERROR: El correo y la contraseña son requeridos");
            throw new InvalidUserDataException("El correo y la contraseña son requeridos");
        }

        User user = userRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> {
                    log.error("FIN_ERROR: Credenciales inválidas");
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            log.error("FIN_ERROR: Credenciales inválidas");
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            log.error("FIN_ERROR: El usuario se encuentra inactivo");
            throw new InvalidCredentialsException("El usuario se encuentra inactivo");
        }

        String token = tokenService.generateToken(user.getEmail());
        user.setLastLogin(LocalDateTime.now());
        user.setToken(token);

        User updatedUser = userRepository.save(user);

        log.info("FIN_OK: fin login usuario con email {}", command.getEmail());
        return userCommandMapper.toResult(updatedUser);
    }
}
