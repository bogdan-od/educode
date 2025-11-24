package com.educode.educodeApi.DTO.user;

import com.educode.educodeApi.DTO.auth.RoleDTO;

import java.util.Set;

public record UserRolesViewDTO(
        Long userId,
        String userName,
        Long groupId,
        String groupTitle,
        Set<RoleDTO> roles
) {}
