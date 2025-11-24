package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.*;
import com.educode.educodeApi.DTO.auth.SessionDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDTO;
import com.educode.educodeApi.DTO.user.UserCreateDTO;
import com.educode.educodeApi.DTO.user.UserDTO;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.utils.MappingUtils;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    private final PuzzleMapper puzzleMapper;
    private final SessionMapper sessionMapper;
    private final DecisionMapper decisionMapper;

    public UserMapper(PuzzleMapper puzzleMapper, SessionMapper sessionMapper, @Lazy DecisionMapper decisionMapper) {
        this.puzzleMapper = puzzleMapper;
        this.sessionMapper = sessionMapper;
        this.decisionMapper = decisionMapper;
    }

    public UserDTO toMaxViewDTO(User user, StdOptional<List<SessionDTO>> sessionDTOs, StdOptional<List<DecisionDTO>> decisionDTOs, StdOptional<List<PuzzleDTO>> puzzleDTOs, String accessToken) {
        return new UserDTO(
            user.getId(),
            user.getName(),
            user.getLogin(),
            user.getEmail(),
            user.getRating(),
            sessionDTOs.orElseGet(() -> MappingUtils.toDtoList(user.getSessions(), s -> sessionMapper.toDTO(s, accessToken), SessionDTO::getId)),
            decisionDTOs.orElseGet(() -> MappingUtils.toDtoList(user.getDecisions(), d -> decisionMapper.toDTO(d, StdOptional.ofNull(), StdOptional.empty(), false), DecisionDTO::getId)),
            puzzleDTOs.orElseGet(() -> MappingUtils.toDtoList(user.getPuzzles(), puzzleMapper::toDTO, PuzzleDTO::getId))
        );
    }

    public UserDTO toMaxViewDTO(User user, StdOptional<List<SessionDTO>> sessionDTOs, StdOptional<List<DecisionDTO>> decisionDTOs, StdOptional<List<PuzzleDTO>> puzzleDTOs) {
        return toMaxViewDTO(user, sessionDTOs, decisionDTOs, puzzleDTOs, null);
    }

    public UserDTO toMinViewDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getName(),
            user.getLogin(),
            user.getEmail(),
            user.getRating()
        );
    }

    public User fromCreateDTO(UserCreateDTO userDTO) {
        return new User(
            userDTO.getName(),
            userDTO.getLogin(),
            userDTO.getEmail(),
            userDTO.getPassword()
        );
    }
}
