package com.educode.educodeApi.DTO.treenode.invitation;

import com.educode.educodeApi.DTO.auth.MinimalRoleDTO;

import java.time.LocalDateTime;
import java.util.List;

public record SelfInvitationViewDTO (
        Long id,
        String code,
        String invitationType,
        List<MinimalRoleDTO> roles,
        LocalDateTime expiresAt,
        boolean active,
        Boolean canLeaveOnJoin,
        String entityTitle
) {
}
