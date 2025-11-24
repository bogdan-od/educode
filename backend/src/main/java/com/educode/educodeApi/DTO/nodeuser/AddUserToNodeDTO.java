package com.educode.educodeApi.DTO.nodeuser;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AddUserToNodeDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID вузла не може бути порожнім")
        Long nodeId,

        Set<String> roles
) {}
