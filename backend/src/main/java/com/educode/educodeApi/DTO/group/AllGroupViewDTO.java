package com.educode.educodeApi.DTO.group;


import org.springframework.data.domain.Page;

public record AllGroupViewDTO(
        Page<GroupViewDTO> groups,
        boolean hasNextPage
) {
}
