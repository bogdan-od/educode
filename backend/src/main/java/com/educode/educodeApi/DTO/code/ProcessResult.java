package com.educode.educodeApi.DTO.code;

import com.educode.educodeApi.exceptions.ProcessExecutionException;

public record ProcessResult(int exitCode, String stdout, String stderr) {
    public String getErrorMessage() {
        return stderr != null && !stderr.isBlank() ? stderr : stdout;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public void throwIfFailed() {
        if (!isSuccess()) {
            throw new ProcessExecutionException(getErrorMessage(), this);
        }
    }
}
