package com.educode.educodeApi.DTO.treenode;

import com.educode.educodeApi.models.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * DTO для обновления ролей участника в узле дерева
 * Заменяет UpdateUserNodeRolesDTO и UpdateUserGroupRolesDTO
 */
public record UpdateMemberRolesDTO(
        @NotEmpty(message = "Список ролей не може бути порожнім")
        Set<String> roles
) {}
