package com.educode.educodeApi.events;

import com.educode.educodeApi.DTO.checker.CheckerDTO;
import org.springframework.context.ApplicationEvent;

public class CheckerDeleteEvent extends ApplicationEvent {
    private final CheckerDTO checkerDTO;

    public CheckerDeleteEvent(Object source, CheckerDTO checkerDTO) {
        super(source);
        this.checkerDTO = checkerDTO;
    }

    public CheckerDTO getCheckerDTO() {
        return checkerDTO;
    }
}
