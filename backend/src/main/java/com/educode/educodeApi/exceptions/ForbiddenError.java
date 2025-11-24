package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ForbiddenError extends ResponseError {
    public ForbiddenError(String clientMessage) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public ForbiddenError(String clientMessage, LogDescriptor logDescriptor) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", clientMessage, Map.of(), logDescriptor);
    }

    public ForbiddenError(String clientMessage, String code, Map<String,Object> details, LogDescriptor logDescriptor) {
        super(HttpStatus.FORBIDDEN, code, clientMessage, details, logDescriptor);
    }
}
