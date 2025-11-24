package com.educode.educodeApi.DTO.treenode.invitation;

import com.educode.educodeApi.DTO.auth.MinimalRoleDTO;

import java.time.LocalDateTime;
import java.util.List;

public record InvitationViewDTO(
        Long id,
        String code,
        Long treeNodeId,
        List<MinimalRoleDTO> roles,
        LocalDateTime expiresAt,
        boolean active,
        String invitationType,
        Long allowedTreeNodeId,
        int userCount,
        Boolean canLeaveOnJoin
) {}
