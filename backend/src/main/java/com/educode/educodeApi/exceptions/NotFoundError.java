package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;

import java.util.Map;


public class NotFoundError extends ResponseError {
    public NotFoundError() {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", "Не знайдено", Map.of(), LogDescriptor.disabled());
    }

    public NotFoundError(String clientMessage) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public NotFoundError(String clientMessage, LogDescriptor logDescriptor) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", clientMessage, Map.of(), logDescriptor);
    }

    public NotFoundError(String clientMessage, String code, Map<String,Object> details, LogDescriptor logDescriptor) {
        super(HttpStatus.NOT_FOUND, code, clientMessage, details, logDescriptor);
    }
}
