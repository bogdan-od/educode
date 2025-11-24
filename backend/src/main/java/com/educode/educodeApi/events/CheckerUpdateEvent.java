package com.educode.educodeApi.events;

import com.educode.educodeApi.DTO.checker.CheckerUpdateDTO;
import org.springframework.context.ApplicationEvent;

import java.nio.file.Path;

public class CheckerUpdateEvent extends ApplicationEvent {
    private final Long id;
    private final CheckerUpdateDTO dto;
    private final Path compiledFilePath;
    private final Path oldCompiledFilePath;

    public CheckerUpdateEvent(Object source, Long id, CheckerUpdateDTO dto, Path compiledFilePath, Path oldCompiledFilePath) {
        super(source);
        this.id = id;
        this.dto = dto;
        this.compiledFilePath = compiledFilePath;
        this.oldCompiledFilePath = oldCompiledFilePath;
    }

    public Long getId() {
        return id;
    }

    public CheckerUpdateDTO getDto() {
        return dto;
    }

    public Path getCompiledFilePath() {
        return compiledFilePath;
    }

    public Path getOldCompiledFilePath() {
        return oldCompiledFilePath;
    }
}
