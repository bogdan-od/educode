package com.educode.educodeApi.DTO.node;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NodeCreateDTO(
        @NotNull(message = "Назва групи не може бути порожньою")
        @Size(min = 5, max = 100, message = "Назва групи має бути від 5 до 100 символів")
        String title,
        @NotNull(message = "Опис групи не може бути порожнім")
        @Size(min = 0, max = 1000, message = "Опис групи має бути до 1000 символів")
        String description,
        Long parentId
) {
}
