package com.user.demo.application.service;

import com.user.demo.application.dto.UserResult;
import com.user.demo.application.mapper.UserCommandMapper;
import com.user.demo.domain.exception.InvalidUserDataException;
import com.user.demo.domain.exception.UserNotFoundException;
import com.user.demo.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserService {

    private final UserRepository userRepository;
    private final UserCommandMapper userCommandMapper;

    public GetUserService(UserRepository userRepository, UserCommandMapper userCommandMapper) {
        this.userRepository = userRepository;
        this.userCommandMapper = userCommandMapper;
    }

    @Transactional(readOnly = true)
    public UserResult execute(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidUserDataException("El ID de usuario es obligatorio");
        }
        return userRepository.findById(id)
                .map(userCommandMapper::toResult)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con id: " + id));
    }
}
