package com.user.demo.application.service;

import com.user.demo.application.dto.UserResult;
import com.user.demo.application.mapper.UserCommandMapper;
import com.user.demo.domain.exception.InvalidUserDataException;
import com.user.demo.domain.exception.UserNotFoundException;
import com.user.demo.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserService {

    private static final Logger log = LoggerFactory.getLogger(GetUserService.class);

    private final UserRepository userRepository;
    private final UserCommandMapper userCommandMapper;

    public GetUserService(UserRepository userRepository, UserCommandMapper userCommandMapper) {
        this.userRepository = userRepository;
        this.userCommandMapper = userCommandMapper;
    }

    @Transactional(readOnly = true)
    public UserResult execute(String id) {
        log.info("INI: inicio busqueda usuario por id {}", id);
        if (id == null || id.isBlank()) {
            throw new InvalidUserDataException("El ID de usuario es obligatorio");
        }
        var user =  userRepository.findById(id)
                .map(userCommandMapper::toResult)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
        log.info("FIN_OK: usuario encontrado {}", user.getId());
        return user;
    }
}
