package com.educode.educodeApi.models;

public interface TreeNodeEntity {
    Long getId();
    void setId(Long id);
    String getTitle();
    void setTitle(String title);
    String getDescription();
    void setDescription(String description);
    TreeNode getTreeNode();
    void setTreeNode(TreeNode treeNode);
}
