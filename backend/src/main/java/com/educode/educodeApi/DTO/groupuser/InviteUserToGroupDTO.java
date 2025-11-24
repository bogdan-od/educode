package com.educode.educodeApi.DTO.groupuser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record InviteUserToGroupDTO(
        @Email(message = "Некоректний формат електронної пошти")
        @NotBlank(message = "Email не може бути порожнім")
        String email,

        @NotNull(message = "ID групи не може бути порожнім")
        Long groupId,

        Set<String> roles
) {}
