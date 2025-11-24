package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.node.NodeCreateDTO;
import com.educode.educodeApi.DTO.node.NodeUpdateDTO;
import com.educode.educodeApi.DTO.node.NodeViewDTO;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.TreeNode;
import org.springframework.stereotype.Component;

@Component
public class NodeMapper {
    public Node fromDTO(NodeCreateDTO nodeCreateDTO, TreeNode treeNode) {
        return new Node(nodeCreateDTO.title(), nodeCreateDTO.description(), treeNode);
    }

    public void setFromUpdateDTO(Node group, NodeUpdateDTO groupUpdateDTO, TreeNode treeNode) {
        group.setTitle(groupUpdateDTO.title());
        group.setDescription(groupUpdateDTO.description());
        group.setTreeNode(treeNode);
    }

    public NodeViewDTO toViewDTO(Node node) {
        return new NodeViewDTO(node.getId(), node.getTitle(), node.getDescription(), node.getTreeNode() != null && node.getTreeNode().getParent() != null
                ? node.getTreeNode().getParent().getId()
                : null,
                node.getTreeNode() != null
                        ? node.getTreeNode().getId()
                        : null);
    }
}
