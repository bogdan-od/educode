package com.educode.educodeApi.exceptions;

public class InteractiveScoreReadingException extends InteractiveResultParsingException {
    public InteractiveScoreReadingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getLocalMessage() {
        return "Помилка при зчитувані файлу з оцінкою рішення користувача";
    }
}
