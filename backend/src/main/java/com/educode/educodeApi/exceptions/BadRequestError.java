package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;

import java.util.Map;


public class BadRequestError extends ResponseError {
    public BadRequestError(String clientMessage) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public BadRequestError(String clientMessage, String code) {
        super(HttpStatus.BAD_REQUEST, code, clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public BadRequestError(String clientMessage, LogDescriptor logDescriptor) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", clientMessage, Map.of(), logDescriptor);
    }

    public BadRequestError(String clientMessage, String code, Map<String,Object> details, LogDescriptor logDescriptor) {
        super(HttpStatus.BAD_REQUEST, code, clientMessage, details, logDescriptor);
    }
}
