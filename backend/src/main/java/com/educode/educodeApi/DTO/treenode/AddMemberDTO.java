package com.educode.educodeApi.DTO.treenode;

import com.educode.educodeApi.models.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * DTO для добавления участника к узлу дерева
 * Заменяет AddUserToNodeDTO и AddUserToGroupDTO
 */
public record AddMemberDTO(
        @NotNull(message = "ID користувача не може бути порожнім")
        Long userId,

        @NotEmpty(message = "Список ролей не може бути порожнім")
        Set<String> roles,

        Boolean canLeave
) {}
