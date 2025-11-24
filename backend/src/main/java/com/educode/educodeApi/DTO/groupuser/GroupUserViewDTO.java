package com.educode.educodeApi.DTO.groupuser;

import java.util.Set;

public record GroupUserViewDTO(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Long groupId,
        String groupTitle,
        Set<String> roles
) {}
