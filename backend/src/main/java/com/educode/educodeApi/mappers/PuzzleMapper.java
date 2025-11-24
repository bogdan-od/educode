package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.*;
import com.educode.educodeApi.DTO.puzzle.*;
import com.educode.educodeApi.models.Checker;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.PuzzleData;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.utils.MappingUtils;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class PuzzleMapper {
    private final PuzzleDataMapper puzzleDataMapper;
    private final DecisionMapper decisionMapper;

    public PuzzleMapper(PuzzleDataMapper puzzleDataMapper, @Lazy DecisionMapper decisionMapper) {
        this.puzzleDataMapper = puzzleDataMapper;
        this.decisionMapper = decisionMapper;
    }

    public PuzzleDTO toDTO(Puzzle p, StdOptional<List<DecisionDTO>> decisionDTOs, StdOptional<Long> checkerId, boolean filterPuzzleData) {
        Set<PuzzleData> puzzleData = filterPuzzleData
                ? p.getPuzzleData().stream().filter(o -> o.getScore() == 0).collect(Collectors.toSet())
                : p.getPuzzleData();

        return new PuzzleDTO(
            p.getId(),
            p.getTitle(),
            p.getDescription(),
            p.getContent(),
            p.getTimeLimit(),
            p.getScore(),
            MappingUtils.toDtoList(puzzleData, puzzleDataMapper::toViewDTO, PuzzleDataDTO::getId),
            p.getUser().getLogin(),
            p.getTaskType(),
            decisionDTOs.orElseGet(() -> MappingUtils.toDtoList(p.getDecisions(), d -> decisionMapper.toDTO(d, StdOptional.empty(), StdOptional.ofNull(), false), DecisionDTO::getId)),
            p.getEnabled(),
            checkerId.orElseGet(() -> p.getChecker().getId()),
            p.getVisible()
        );
    }

    public PuzzleDTO toDTO(Puzzle p) {
        return toDTO(p, StdOptional.ofEmptyList(), StdOptional.ofNull(), true);
    }

    public PuzzleDTO toDTO(Puzzle p, Checker checker, boolean filterPuzzleData) {
        return toDTO(p, StdOptional.ofEmptyList(), StdOptional.of(checker != null ? checker.getId() : null), filterPuzzleData);
    }

    public PuzzleDTO toMinDTO(Puzzle puzzle) {
        return new PuzzleDTO(
            puzzle.getId(),
            puzzle.getTitle(),
            puzzle.getDescription(),
            null,
            puzzle.getTimeLimit(),
            puzzle.getScore(),
            new ArrayList<>(),
            puzzle.getUser().getLogin(),
            puzzle.getTaskType(),
            new ArrayList<>(),
            puzzle.getEnabled(),
            null,
            puzzle.getVisible()
        );
    }

    public Puzzle fromCreateDTO(PuzzleCreateDTO p, User user, Checker checker) {
        Puzzle puzzle = new Puzzle(
            null,
            p.getTitle(),
            p.getDescription(),
            p.getContent(),
            user,
            p.getTimeLimit(),
            null,
            p.getScore(),
            new HashSet<>(),
            checker,
            p.getTaskType(),
            true,
            p.getVisible()
        );

        puzzle.setPuzzleData(MappingUtils.toDtoCollection(p.getPuzzleData(), pd -> puzzleDataMapper.fromCreateDTO(pd, puzzle), null, false, Collectors.toSet()));

        return puzzle;
    }

    public void setFromUpdateDTO(Puzzle puzzle, PuzzleUpdateDTO puzzleUpdateDTO, Checker checker) {
        puzzle.setTitle(puzzleUpdateDTO.getTitle());
        puzzle.setDescription(puzzleUpdateDTO.getDescription());
        puzzle.setContent(puzzleUpdateDTO.getContent());
        puzzle.setTimeLimit(puzzleUpdateDTO.getTimeLimit());
        puzzle.setEnabled(puzzleUpdateDTO.getEnabled());
        puzzle.setVisible(puzzleUpdateDTO.getVisible());

        if (checker != null)
            puzzle.setChecker(checker);

        List<PuzzleData> puzzleDataList = puzzle.getPuzzleData().stream().toList();
        List<PuzzleDataUpdateDTO> puzzleDataUpdateDTOList = puzzleUpdateDTO.getPuzzleData() != null ? puzzleUpdateDTO.getPuzzleData().stream().toList() : Collections.emptyList();

        if (puzzleDataList.size() != puzzleDataUpdateDTOList.size()) {
            throw new IllegalArgumentException("Mismatch between existing puzzleData size and update DTO size");
        }

        IntStream.range(0, puzzleDataList.size()).forEach(i -> puzzleDataMapper.setFromUpdateDTO(puzzleDataList.get(i), puzzleDataUpdateDTOList.get(i)));
    }
}
