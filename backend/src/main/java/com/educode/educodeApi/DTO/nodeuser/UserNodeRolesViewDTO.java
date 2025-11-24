package com.educode.educodeApi.DTO.nodeuser;

import com.educode.educodeApi.DTO.auth.RoleDTO;

import java.util.Set;

public record UserNodeRolesViewDTO(
        Long userId,
        String userName,
        Long nodeId,
        String nodeTitle,
        Set<RoleDTO> roles
) {}
