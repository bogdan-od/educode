package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ServerError extends ResponseError {
    public ServerError(String clientMessage) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", clientMessage, Map.of(), LogDescriptor.disabled());
    }

    public ServerError(String clientMessage, LogDescriptor logDescriptor) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", clientMessage, Map.of(), logDescriptor);
    }


    public ServerError(String clientMessage, String code, Map<String,Object> details, LogDescriptor logDescriptor) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, clientMessage, details, logDescriptor);
    }
}
