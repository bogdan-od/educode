package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.puzzle.PuzzleDataCreateDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDataDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDataUpdateDTO;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.PuzzleData;
import org.springframework.stereotype.Component;

@Component
public class PuzzleDataMapper {
    public PuzzleData fromCreateDTO(PuzzleDataCreateDTO puzzleDataCreateDTO, Puzzle puzzle) {
        return new PuzzleData(null, puzzle, puzzleDataCreateDTO.getInput(), puzzleDataCreateDTO.getOutput(), puzzleDataCreateDTO.getScore());
    }

    public PuzzleDataDTO toViewDTO(PuzzleData puzzleData) {
        return new PuzzleDataDTO(puzzleData.getId(), puzzleData.getInput(), puzzleData.getOutput(), puzzleData.getScore());
    }

    public void setFromUpdateDTO(PuzzleData puzzleData, PuzzleDataUpdateDTO puzzleDataUpdateDTO) {
        puzzleData.setInput(puzzleDataUpdateDTO.getInput());
        puzzleData.setOutput(puzzleData.getOutput());
    }
}
