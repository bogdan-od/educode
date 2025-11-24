package com.educode.educodeApi.exceptions;

public class InteractiveScoreFileDoesNotExistsException extends InteractiveResultParsingException {
    public InteractiveScoreFileDoesNotExistsException(String message) {
        super(message);
    }

    @Override
    public String getLocalMessage() {
        return "Перевіряюча програма не створила файл /result/score";
    }
}
