package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.TreeNode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {

    /**
     * Находит Node по TreeNode
     */
    Node findByTreeNode(TreeNode treeNode);

    @EntityGraph(attributePaths = "treeNode")
    List<Node> findAllByTreeNodeIdIn(Iterable<Long> treeNodeIds);

    Node findByTreeNodeId(Long treeNodeId);

    List<Node> findAllByTreeNode_ParentIsNull();
}
