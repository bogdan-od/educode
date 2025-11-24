package com.educode.educodeApi.exceptions;

public abstract class InteractiveResultParsingException extends RuntimeException {
    public InteractiveResultParsingException(String message) {
        super(message);
    }

    public InteractiveResultParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract String getLocalMessage();
}
