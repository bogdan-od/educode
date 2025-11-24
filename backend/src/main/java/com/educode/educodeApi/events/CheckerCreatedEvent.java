package com.educode.educodeApi.events;

import com.educode.educodeApi.DTO.checker.CheckerCreateDTO;
import org.springframework.context.ApplicationEvent;

import java.nio.file.Path;

public class CheckerCreatedEvent extends ApplicationEvent {
    private final Long id;
    private final Path compiledFilePath;
    private final CheckerCreateDTO dto;

    public CheckerCreatedEvent(Object source, Long id, Path compiledFilePath, CheckerCreateDTO dto) {
        super(source);
        this.id = id;
        this.compiledFilePath = compiledFilePath;
        this.dto = dto;
    }

    public Long getId() {
        return id;
    }

    public Path getCompiledFilePath() {
        return compiledFilePath;
    }

    public CheckerCreateDTO getDto() {
        return dto;
    }
}
