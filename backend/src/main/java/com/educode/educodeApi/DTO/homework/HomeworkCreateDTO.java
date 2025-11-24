package com.educode.educodeApi.DTO.homework;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record HomeworkCreateDTO(
        @NotBlank(message = "Заголовок не може бути порожнім")
        String title,

        String content,

        @NotNull(message = "ID групи не може бути порожнім")
        Long groupId,

        Long puzzleId,

        LocalDateTime deadline
) {}
