package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Group;
import com.educode.educodeApi.models.TreeNode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    /**
     * Находит Group по TreeNode
     */
    Group findByTreeNode(TreeNode treeNode);

    Group findByTreeNodeId(Long treeNodeId);

    @EntityGraph(attributePaths = "treeNode")
    List<Group> findAllByTreeNodeIdIn(Iterable<Long> treeNodeIds);
}
