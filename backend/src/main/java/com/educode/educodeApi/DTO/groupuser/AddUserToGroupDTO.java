package com.educode.educodeApi.DTO.groupuser;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AddUserToGroupDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID групи не може бути порожнім")
        Long groupId,

        Set<String> roles
) {}
