package com.educode.educodeApi.DTO.nodeuser;

import java.util.Set;

public record UserNodeViewDTO(
        Long nodeId,
        String nodeTitle,
        String nodeDescription,
        Set<String> userRoles
) {}
