package com.educode.educodeApi.DTO.nodeuser;

import jakarta.validation.constraints.NotNull;

public record RemoveUserFromNodeDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID вузла не може бути порожнім")
        Long nodeId
) {}
