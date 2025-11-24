package com.educode.educodeApi.DTO.treenode.invitation;

import com.educode.educodeApi.enums.InvitationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record InvitationCreateDTO(
        @NotEmpty(message = "Список ролей не може бути порожнім")
        Set<String> roles,
        LocalDateTime expiresAt, // Nullable - вечное
        @NotNull(message = "Тип запрошення є обов'язковим")
        InvitationType invitationType,
        Long allowedTreeNodeId, // Обязательно для NODE_BASED
        Set<Long> userIds, // Обязательно для SINGLE_USE и LIMITED_LIST
        Boolean canLeave
) {}
