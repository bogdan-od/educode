package com.educode.educodeApi.DTO.groupuser;

import org.springframework.data.domain.Page;

public record AllGroupUserViewDTO(
        Page<GroupUserViewDTO> users,
        boolean hasNext
) {}
