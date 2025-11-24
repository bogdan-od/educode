package com.educode.educodeApi.exceptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public abstract class ContainerException extends RuntimeException {
    private final String errorCode;       // machine-friendly
    private final String logTemplate;     // SLF4J-style, e.g. "Exec failed: exit={}, id={}"
    private final Object[] logArgs;
    private final Map<String,Object> details; // small data ok to return to UI

    protected ContainerException(String errorCode, String technicalMessage,
                                 String logTemplate, Object[] logArgs, Map<String,Object> details) {
        super(technicalMessage);
        this.errorCode = errorCode == null ? "CONTAINER_ERROR" : errorCode;
        this.logTemplate = logTemplate == null ? technicalMessage : logTemplate;
        this.logArgs = logArgs == null ? new Object[0] : Arrays.copyOf(logArgs, logArgs.length);
        this.details = details == null ? Collections.emptyMap() : Collections.unmodifiableMap(details);
    }

    public String getErrorCode() { return errorCode; }
    public String getLogTemplate() { return logTemplate; }
    public Object[] getLogArgs() { return Arrays.copyOf(logArgs, logArgs.length); }
    public Map<String,Object> getDetails() { return details; }
}
