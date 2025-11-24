package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.treenode.TreeNodeEntityDTO;
import com.educode.educodeApi.models.TreeNodeEntity;
import com.educode.educodeApi.models.TreeNodeMember;
import org.springframework.stereotype.Component;

@Component
public class TreeNodeEntityMapper {
    private final RoleMapper roleMapper;

    public TreeNodeEntityMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public TreeNodeEntityDTO toViewDTO(TreeNodeEntity tne, TreeNodeMember treeNodeMember) {
        return new TreeNodeEntityDTO(tne.getId(), tne.getTitle(), tne.getDescription(), tne.getTreeNode() != null && tne.getTreeNode().getParent() != null
                ? tne.getTreeNode().getParent().getId()
                : null,
                tne.getTreeNode() != null
                        ? tne.getTreeNode().getId()
                        : null,
                tne.getTreeNode().getType(),
                treeNodeMember.getRoles().stream().map(roleMapper::toMinimalDTO).toList(),
                treeNodeMember.isCanLeave());
    }
}
