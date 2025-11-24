package com.educode.educodeApi.DTO.nodeuser;

import org.springframework.data.domain.Page;

public record AllNodeUserViewDTO(
        Page<NodeUserViewDTO> users,
        boolean hasNext
) {}
