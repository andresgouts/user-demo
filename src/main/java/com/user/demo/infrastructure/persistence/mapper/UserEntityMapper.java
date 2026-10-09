package com.user.demo.infrastructure.persistence.mapper;

import com.user.demo.domain.model.Phone;
import com.user.demo.domain.model.User;
import com.user.demo.infrastructure.persistence.PhoneEntity;
import com.user.demo.infrastructure.persistence.UserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserEntityMapper {

    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = UserEntity.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .password(user.getPassword())
                .created(user.getCreated())
                .modified(user.getModified())
                .lastLogin(user.getLastLogin())
                .token(user.getToken())
                .active(user.isActive())
                .phones(new ArrayList<>())
                .build();

        if (user.getPhones() != null) {
            for (Phone phone : user.getPhones()) {
                PhoneEntity phoneEntity = PhoneEntity.builder()
                        .id(phone.getId())
                        .number(phone.getNumber())
                        .cityCode(phone.getCityCode())
                        .countryCode(phone.getCountryCode())
                        .build();
                entity.addPhone(phoneEntity);
            }
        }

        return entity;
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        List<Phone> phones = new ArrayList<>();
        if (entity.getPhones() != null) {
            for (PhoneEntity phoneEntity : entity.getPhones()) {
                phones.add(Phone.builder()
                        .id(phoneEntity.getId())
                        .number(phoneEntity.getNumber())
                        .cityCode(phoneEntity.getCityCode())
                        .countryCode(phoneEntity.getCountryCode())
                        .build());
            }
        }

        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .created(entity.getCreated())
                .modified(entity.getModified())
                .lastLogin(entity.getLastLogin())
                .token(entity.getToken())
                .active(entity.isActive())
                .phones(phones)
                .build();
    }
}
