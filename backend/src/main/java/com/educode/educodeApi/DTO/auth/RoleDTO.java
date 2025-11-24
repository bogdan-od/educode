package com.educode.educodeApi.DTO.auth;

import com.educode.educodeApi.enums.RoleScope;

public record RoleDTO(
        String name,
        String description,
        Long rank,
        RoleScope roleScope
) {}
