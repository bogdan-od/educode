package com.educode.educodeApi.DTO.homework;

import org.springframework.data.domain.Page;

public record AllHomeworkViewDTO(
        Page<HomeworkViewDTO> homeworks,
        boolean hasNext
) {}
