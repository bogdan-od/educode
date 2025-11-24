package com.educode.educodeApi.DTO.node;

/**
 * DTO для просмотра узла
 */
public record NodeViewDTO(
        Long id,
        String title,
        String description,
        Long parentId,
        Long treeNodeId
) {}
