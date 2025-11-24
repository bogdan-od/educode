package com.educode.educodeApi.DTO.group;

/**
 * DTO для просмотра группы
 */
public record GroupViewDTO(
        Long id,
        String title,
        String description,
        Long parentId,
        Long treeNodeId
) {}
