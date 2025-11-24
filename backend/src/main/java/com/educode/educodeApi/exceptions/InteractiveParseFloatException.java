package com.educode.educodeApi.exceptions;

public class InteractiveParseFloatException extends InteractiveResultParsingException {
    private final String scoreValue;

    public InteractiveParseFloatException(String message, Throwable cause, String scoreValue) {
        super(message, cause);
        this.scoreValue = scoreValue;
    }

    @Override
    public String getLocalMessage() {
        return "Помилка при конвертації оцінки \"%s\" у числове значення".formatted(scoreValue);
    }
}
