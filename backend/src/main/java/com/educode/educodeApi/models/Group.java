package com.educode.educodeApi.models;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users_groups")
public class Group implements TreeNodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_node_id", nullable = false, unique = true)
    private TreeNode treeNode;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Homework> homeworks = new HashSet<>();

    public Group() {}

    public Group(String title, String description, TreeNode treeNode) {
        this.title = title;
        this.description = description;
        this.treeNode = treeNode;
        // Группы не могут иметь детей
        if (treeNode != null) {
            treeNode.setCanHaveChildren(false);
        }
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
    public Set<Homework> getHomeworks() { return homeworks; }
    public void setHomeworks(Set<Homework> homeworks) { this.homeworks = homeworks; }

    public void addHomework(Homework homework) {
        homeworks.add(homework);
        homework.setGroup(this);
    }

    public void removeHomework(Homework homework) {
        homeworks.remove(homework);
        homework.setGroup(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Group that = (Group) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
