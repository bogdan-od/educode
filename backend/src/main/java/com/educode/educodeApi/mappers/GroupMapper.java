package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.group.GroupCreateDTO;
import com.educode.educodeApi.DTO.group.GroupUpdateDTO;
import com.educode.educodeApi.DTO.group.GroupViewDTO;
import com.educode.educodeApi.models.Group;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.TreeNode;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {
    public Group fromDTO(GroupCreateDTO groupCreateDTO, TreeNode treeNode) {
        return new Group(groupCreateDTO.title(), groupCreateDTO.description(), treeNode);
    }

    public void setFromUpdateDTO(Group group, GroupUpdateDTO groupUpdateDTO, TreeNode treeNode) {
        group.setTitle(groupUpdateDTO.title());
        group.setDescription(groupUpdateDTO.description());
        group.setTreeNode(treeNode);
    }

    public GroupViewDTO toViewDTO(Group group) {
        return new GroupViewDTO(group.getId(), group.getTitle(), group.getDescription(), group.getTreeNode() != null && group.getTreeNode().getParent() != null
                ? group.getTreeNode().getParent().getId()
                : null,
                group.getTreeNode() != null
                        ? group.getTreeNode().getId()
                        : null);
    }
}
