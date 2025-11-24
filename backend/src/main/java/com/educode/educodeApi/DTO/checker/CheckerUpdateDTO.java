package com.educode.educodeApi.DTO.checker;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckerUpdateDTO(
        @NotNull
        @Size(min = 1, max = 100, message = "Довжина назви має бути не менше 1 та не більше 100 символів")
        String name,
        @NotNull
        String languageId
) {
}
