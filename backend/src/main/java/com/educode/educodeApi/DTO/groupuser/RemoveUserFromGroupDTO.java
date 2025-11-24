package com.educode.educodeApi.DTO.groupuser;

import jakarta.validation.constraints.NotNull;

public record RemoveUserFromGroupDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotNull(message = "ID групи не може бути порожнім")
        Long groupId
) {}

