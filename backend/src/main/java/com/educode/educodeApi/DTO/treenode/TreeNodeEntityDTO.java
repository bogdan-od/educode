package com.educode.educodeApi.DTO.treenode;

import com.educode.educodeApi.DTO.auth.MinimalRoleDTO;
import com.educode.educodeApi.enums.TreeNodeType;

import java.util.List;

public record TreeNodeEntityDTO(
    Long id,
    String title,
    String description,
    Long parentId,
    Long treeNodeId,
    TreeNodeType type,
    List<MinimalRoleDTO> roles,
    Boolean canLeave
) {
}
