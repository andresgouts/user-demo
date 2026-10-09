package com.user.demo.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserCommand {
    private String name;
    private String email;
    private String password;
    @Builder.Default
    private List<PhoneCommand> phones = new ArrayList<>();
}
