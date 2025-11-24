package com.educode.educodeApi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "homeworks")
public class Homework implements BeforeRealUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @OneToMany(mappedBy = "homework", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Decision> decisions = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_id")
    private Puzzle puzzle;

    @Column(columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @Column(columnDefinition = "TIMESTAMP(6) DEFAULT NULL")
    private LocalDateTime updatedAt;
    @Column(columnDefinition = "TIMESTAMP(6) DEFAULT NULL")
    private LocalDateTime deadline;

    public Homework() {
    }

    public Homework(Long id, String title, String content, Group group, Set<Decision> decisions, Puzzle puzzle, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deadline) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.group = group;
        this.decisions = decisions;
        this.puzzle = puzzle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deadline = deadline;
    }

    public Homework(String title, String content, Group group, Puzzle puzzle, LocalDateTime deadline) {
        this.title = title;
        this.content = content;
        this.group = group;
        this.puzzle = puzzle;
        this.deadline = deadline;

        if (group != null) {
            group.addHomework(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Set<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(Set<Decision> decisions) {
        this.decisions = decisions;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Long getPuzzleId() {
        return puzzle != null ? puzzle.getId() : null;
    }

    public boolean isDeadlinePassed() {
        if (deadline == null) return false;
        return deadline.isBefore(LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public void preRealUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Homework that = (Homework) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
