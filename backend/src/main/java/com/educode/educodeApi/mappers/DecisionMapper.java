package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.DecisionDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDTO;
import com.educode.educodeApi.DTO.user.UserDTO;
import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.stereotype.Component;

@Component
public class DecisionMapper {
    private final PuzzleMapper puzzleMapper;
    private final UserMapper userMapper;

    public DecisionMapper(PuzzleMapper puzzleMapper, UserMapper userMapper) {
        this.puzzleMapper = puzzleMapper;
        this.userMapper = userMapper;
    }

    public DecisionDTO toDTO(Decision decision, StdOptional<UserDTO> userDTO, StdOptional<PuzzleDTO> puzzleDTO, boolean showCode) {
        return new DecisionDTO(
            decision.getId(),
            decision.getLanguage(),
            decision.getScore(),
            decision.isCorrect(),
            decision.isFinished(),
            puzzleDTO.orElseGet(() -> puzzleMapper.toMinDTO(decision.getPuzzle())),
            decision.getCreatedAt(),
            userDTO.orElseGet(() -> userMapper.toMinViewDTO(decision.getUser())),
            showCode ? decision.getCode() : null
        );
    }

    public DecisionDTO toDTO(Decision decision, boolean showCode) {
        return toDTO(decision, StdOptional.empty(), StdOptional.empty(), showCode);
    }
}
