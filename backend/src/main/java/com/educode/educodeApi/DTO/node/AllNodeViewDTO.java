package com.educode.educodeApi.DTO.node;

import org.springframework.data.domain.Page;

public record AllNodeViewDTO(
        Page<NodeViewDTO> nodes,
        boolean hasNextPage
) {
}
