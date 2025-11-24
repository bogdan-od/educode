package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.utils.LogDescriptor;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;


import java.util.Map;


/**
 * Base exception for controllable HTTP responses. Wraps client-visible message + log descriptor.
 */
public class ResponseError extends RuntimeException {
    private final HttpStatus status;
    private final String clientMessage;
    private final String code;
    private final Map<String, Object> details;
    private final LogDescriptor logDescriptor;


    public ResponseError(HttpStatus status,
                         String code,
                         String clientMessage,
                         Map<String, Object> details,
                         LogDescriptor logDescriptor) {
        super(clientMessage, logDescriptor != null ? logDescriptor.cause() : null);
        this.status = status;
        this.clientMessage = clientMessage == null ? "" : clientMessage;
        this.code = code == null ? "ERROR" : code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
        this.logDescriptor = logDescriptor;
    }


    public HttpStatus getStatus() { return status; }
    public String getClientMessage() { return clientMessage; }
    public String getCode() { return code; }
    public Map<String, Object> getDetails() { return details; }
    public LogDescriptor getLogDescriptor() { return logDescriptor; }


    public Map<String, Object> toBody() {
        return Map.of(
                "error", clientMessage,
                "code", code,
                "details", details
        );
    }
}
