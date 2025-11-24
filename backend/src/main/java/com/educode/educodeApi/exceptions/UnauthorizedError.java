package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnauthorizedError extends ResponseError {
    public UnauthorizedError() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Ви не авторизовані", Map.of(), LogDescriptor.disabled());
    }

    public UnauthorizedError(String clientMessage) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public UnauthorizedError(String clientMessage, LogDescriptor logDescriptor) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", clientMessage, Map.of(), logDescriptor);
    }

    public UnauthorizedError(String clientMessage, String code, Map<String,Object> details, LogDescriptor logDescriptor) {
        super(HttpStatus.UNAUTHORIZED, code, clientMessage, details, logDescriptor);
    }
}