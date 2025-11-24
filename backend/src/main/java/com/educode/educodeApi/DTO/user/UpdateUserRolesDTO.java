package com.educode.educodeApi.DTO.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID групи не може бути порожнім")
        Long groupId,

        @NotEmpty(message = "Список ролей не може бути порожнім")
        Set<String> roles
) {}
