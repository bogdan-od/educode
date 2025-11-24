package com.educode.educodeApi.DTO.nodeuser;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserNodeRolesDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID вузла не може бути порожнім")
        Long nodeId,

        @NotEmpty(message = "Список ролей не може бути порожнім")
        Set<String> roles
) {}
