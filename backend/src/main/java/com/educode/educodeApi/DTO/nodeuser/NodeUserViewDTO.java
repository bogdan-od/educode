package com.educode.educodeApi.DTO.nodeuser;

import java.util.Set;

public record NodeUserViewDTO(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Long nodeId,
        String nodeTitle,
        Set<String> roles
) {}
