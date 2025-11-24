package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.TreeNodeType;
import com.educode.educodeApi.enums.converters.TreeNodeTypeConverter;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tree_nodes")
public class TreeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TreeNode parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TreeNode> children = new HashSet<>();

    @OneToMany(mappedBy = "treeNode", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<TreeNodeMember> members = new HashSet<>();

    @Column(nullable = false)
    private Boolean canHaveChildren = true;

    @Convert(converter = TreeNodeTypeConverter.class)
    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private TreeNodeType type = TreeNodeType.FREE;

    @ManyToMany
    @JoinTable(
            name = "tree_node_puzzles",
            joinColumns = @JoinColumn(name = "tree_node_id"),
            inverseJoinColumns = @JoinColumn(name = "puzzle_id")
    )
    private Set<Puzzle> puzzles = new HashSet<>();

    public TreeNode() {}

    public TreeNode(TreeNode parent, Boolean canHaveChildren) {
        this.parent = parent;
        this.canHaveChildren = canHaveChildren;
    }

    public TreeNode(TreeNode parent, Boolean canHaveChildren, TreeNodeType type) {
        this.parent = parent;
        this.canHaveChildren = canHaveChildren;
        this.type = type;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TreeNode getParent() { return parent; }
    public void setParent(TreeNode parent) { this.parent = parent; }
    public Long getParentId() { return parent != null ? parent.getId() : null; }
    public Set<TreeNode> getChildren() { return children; }
    public void setChildren(Set<TreeNode> children) { this.children = children; }
    public Set<TreeNodeMember> getMembers() { return members; }
    public void setMembers(Set<TreeNodeMember> members) { this.members = members; }
    public Boolean getCanHaveChildren() { return canHaveChildren; }
    public void setCanHaveChildren(Boolean canHaveChildren) { this.canHaveChildren = canHaveChildren; }
    public Set<Puzzle> getPuzzles() { return puzzles; }
    public void setPuzzles(Set<Puzzle> puzzles) { this.puzzles = puzzles; }

    public TreeNodeType getType() {
        return type;
    }

    public void setType(TreeNodeType type) {
        this.type = type;
    }

    public void addMember(TreeNodeMember member) {
        members.add(member);
        member.setTreeNode(this);
    }

    public void removeMember(TreeNodeMember member) {
        members.remove(member);
        member.setTreeNode(null);
    }

    public void addPuzzle(Puzzle puzzle) {
        puzzles.add(puzzle);
    }

    public void removePuzzle(Puzzle puzzle) {
        puzzles.remove(puzzle);
    }

    public void addChild(TreeNode child) {
        if (!canHaveChildren) {
            throw new IllegalStateException("This tree node cannot have children");
        }
        children.add(child);
        child.setParent(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        TreeNode that = (TreeNode) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
