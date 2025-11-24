package com.educode.educodeApi.DTO.treenode.invitation;

import java.time.LocalDateTime;
import java.util.Set;

public record InvitationUpdateDTO(
        Set<String> roles, // Если null - не обновлять
        LocalDateTime expiresAt, // Если null - не обновлять
        Boolean active // Если null - не обновлять
) {}
