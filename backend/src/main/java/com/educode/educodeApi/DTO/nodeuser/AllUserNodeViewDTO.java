package com.educode.educodeApi.DTO.nodeuser;

import org.springframework.data.domain.Page;

public record AllUserNodeViewDTO(
        Page<UserNodeViewDTO> nodes,
        boolean hasNext
) {}
