package com.educode.educodeApi.models;

import jakarta.persistence.*;

@Entity
@Table(name = "nodes")
public class Node implements TreeNodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_node_id", nullable = false, unique = true)
    private TreeNode treeNode;

    public Node() {}

    public Node(String title, String description, TreeNode treeNode) {
        this.title = title;
        this.description = description;
        this.treeNode = treeNode;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TreeNode getTreeNode() { return treeNode; }
    public void setTreeNode(TreeNode treeNode) { this.treeNode = treeNode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Node that = (Node) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
