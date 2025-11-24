package com.educode.educodeApi.models;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "checkers")
public class Checker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String languageId;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "checker", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Puzzle> puzzles = new HashSet<>();

    public Checker() {
    }

    public Checker(Long id, User user, String filename, long sizeBytes, String languageId, String name) {
        this.id = id;
        this.user = user;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.languageId = languageId;
        this.name = name;

        if (user != null) {
            user.addChecker(this);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public Set<Puzzle> getPuzzles() {
        return puzzles;
    }

    public void setPuzzles(Set<Puzzle> puzzles) {
        this.puzzles = puzzles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addPuzzle(Puzzle puzzle) {
        puzzles.add(puzzle);
        puzzle.setChecker(this);
    }

    public void removePuzzle(Puzzle puzzle) {
        puzzles.remove(puzzle);
        puzzle.setChecker(null);
    }

    public void prepareDelete() {
        user.getCheckers().remove(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Checker that = (Checker) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
