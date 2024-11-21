package com.educode.educodeApi.models;

import jakarta.persistence.*;

/**
 * Клас, що представляє чергу компіляції коду.
 * Використовується для зберігання та управління чергою на компіляцію.
 */
@Entity
@Table(name = "compile_queue")
public class CompileQueue {

    // Унікальний ідентифікатор запису в черзі
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Зв'язок з рішенням, яке потребує компіляції
    @ManyToOne
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту черги компіляції.
     */
    public CompileQueue() {}

    /**
     * Конструктор для створення нового запису в черзі компіляції.
     * @param id Унікальний ідентифікатор запису
     * @param decision Рішення, яке потребує компіляції
     */
    public CompileQueue(Long id, Decision decision) {
        this.id = id;
        this.decision = decision;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
