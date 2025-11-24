package com.educode.educodeApi.DTO.checker;

import java.util.List;

public record AllCheckersDTO(List<CheckerOnPageDTO> checkers, boolean hasNextPage) {
}
