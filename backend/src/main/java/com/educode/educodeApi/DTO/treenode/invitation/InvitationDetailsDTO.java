package com.educode.educodeApi.DTO.treenode.invitation;

import com.educode.educodeApi.DTO.auth.MinimalRoleDTO;

import java.util.List;

public record InvitationDetailsDTO(
        Long treeNodeId,
        List<MinimalRoleDTO> roles,
        String invitationType,
        Boolean canLeaveOnJoin
) {}
