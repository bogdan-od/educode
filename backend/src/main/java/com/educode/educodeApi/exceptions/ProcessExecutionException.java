package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.DTO.code.ProcessResult;

public class ProcessExecutionException extends RuntimeException {
    private ProcessResult processResult;

    public ProcessExecutionException(String message, ProcessResult processResult) {
        super(message);
        this.processResult = processResult;
    }

    public ProcessExecutionException(String message, Throwable cause, ProcessResult processResult) {
        super(message, cause);
        this.processResult = processResult;
    }

    public ProcessResult getProcessResult() {
        return processResult;
    }

    public void setProcessResult(ProcessResult processResult) {
        this.processResult = processResult;
    }
}
