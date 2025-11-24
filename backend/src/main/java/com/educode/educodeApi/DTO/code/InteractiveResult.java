package com.educode.educodeApi.DTO.code;

public class InteractiveResult {
    private double score;
    private String message;

    InteractiveResult() {}

    public InteractiveResult(double score, String message) {
        this.score = score;
        this.message = message;
    }

    public double getScore() {
        return score;
    }

    public float getRealScore(float maxScore) {
        return (float) ((score * maxScore) / 100.0f);
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "InteractiveResult{" +
                "score=" + score +
                ", message='" + message + '\'' +
                '}';
    }
}
