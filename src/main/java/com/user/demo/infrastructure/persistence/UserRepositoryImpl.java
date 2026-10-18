package com.user.demo.infrastructure.persistence;

import com.user.demo.application.service.CreateUserService;
import com.user.demo.domain.model.User;
import com.user.demo.domain.repository.UserRepository;
import com.user.demo.infrastructure.persistence.mapper.UserEntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryImpl implements UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepositoryImpl.class);

    private final UserJpaRepository userJpaRepository;
    private final UserEntityMapper userEntityMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserEntityMapper userEntityMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public User save(User user) {
        log.debug("INI: guardado de usuario iniciado con correo {}", user.getEmail());
        UserEntity entity = userEntityMapper.toEntity(user);
        UserEntity saved = userJpaRepository.save(entity);
        log.debug("FIN: guardado de usuario finalizado con correo {}", saved.getEmail());
        return userEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(String id) {
        return userJpaRepository.findById(id).map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(userEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }
}
