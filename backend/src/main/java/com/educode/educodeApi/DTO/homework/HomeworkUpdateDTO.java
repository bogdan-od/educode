package com.educode.educodeApi.DTO.homework;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record HomeworkUpdateDTO(
        @NotBlank(message = "Заголовок не може бути порожнім")
        String title,

        String content,

        Long puzzleId,

        LocalDateTime deadline
) {}
