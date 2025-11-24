package com.educode.educodeApi.DTO.auth;

import com.educode.educodeApi.enums.RoleScope;

import java.util.List;

public record RoleDetailDTO(
        String name,
        String description,
        RoleScope roleScope,
        Long rank,
        List<PermissionDTO> permissions
) {
}
