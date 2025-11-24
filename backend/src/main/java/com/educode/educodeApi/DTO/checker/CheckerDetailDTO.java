package com.educode.educodeApi.DTO.checker;

import com.educode.educodeApi.DTO.puzzle.PuzzleDTO;

import java.util.List;

public record CheckerDetailDTO(Long id, String language, String name, Long size, List<PuzzleDTO> puzzles) {
}
