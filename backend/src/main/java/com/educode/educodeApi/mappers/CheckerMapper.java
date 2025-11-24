package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.checker.*;
import com.educode.educodeApi.DTO.code.ProgrammingLanguage;
import com.educode.educodeApi.models.Checker;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.properties.CheckerProperties;
import com.educode.educodeApi.repositories.CheckerRepository;
import com.educode.educodeApi.services.ProgrammingLanguageService;
import com.educode.educodeApi.utils.FileManagementUtil;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CheckerMapper {
    private final CheckerProperties checkerProperties;
    private final ProgrammingLanguageService programmingLanguageService;
    private final PuzzleMapper puzzleMapper;
    private final FileManagementUtil fileManagementUtil;
    private final CheckerRepository checkerRepository;

    public CheckerMapper(CheckerProperties checkerProperties, ProgrammingLanguageService programmingLanguageService, PuzzleMapper puzzleMapper, FileManagementUtil fileManagementUtil, CheckerRepository checkerRepository) {
        this.checkerProperties = checkerProperties;
        this.programmingLanguageService = programmingLanguageService;
        this.puzzleMapper = puzzleMapper;
        this.fileManagementUtil = fileManagementUtil;
        this.checkerRepository = checkerRepository;
    }

    public CheckerDTO toTechDTO(Checker checker) {
        if (checker == null) return null;

        Optional<ProgrammingLanguage> programmingLanguage = programmingLanguageService.getProgrammingLanguage(checker.getLanguageId());
        String lang = programmingLanguage.map(ProgrammingLanguage::id).orElse(null);
        String langVersion = programmingLanguage.map(ProgrammingLanguage::version).orElse(null);

        return new CheckerDTO(
                Paths.get(checkerProperties.getBasePath()).resolve(checker.getFilename()).toString(),
                Paths.get(checkerProperties.getBaseLogPath()).resolve(checker.getFilename()).toString(),
                lang, langVersion);
    }

    public CheckerViewDTO toViewDTO(Checker checker) {
        return new CheckerViewDTO(checker.getId(), checker.getName(), programmingLanguageService.getProgrammingLanguage(checker.getLanguageId()).map(ProgrammingLanguage::name).orElse(null));
    }

    public CheckerDetailDTO toDetailDTO(Checker checker) {
        return toDetailDTO(checker, StdOptional.empty());
    }

    public CheckerDetailDTO toDetailDTO(Checker checker, StdOptional<Set<Puzzle>> puzzles) {
        return new CheckerDetailDTO(checker.getId(), checker.getLanguageId(), checker.getName(), checker.getSizeBytes(), puzzles.orElseGet(checker::getPuzzles).stream().map(puzzleMapper::toMinDTO).collect(Collectors.toList()));
    }

    public CheckerCreateDTO toCreateDTO(CheckerUpdateDTO checkerUpdateDTO) {
        return new CheckerCreateDTO(checkerUpdateDTO.name(), checkerUpdateDTO.languageId());
    }

    public void setFromUpdateDTO(Checker checker, CheckerUpdateDTO dto, Path checkerPath) {
        checker.setName(dto.name());
        checker.setLanguageId(dto.languageId());
        checker.setFilename(checkerPath.getFileName().toString());
        checker.setSizeBytes(fileManagementUtil.getDirectorySize(checkerPath));
    }

    public Checker fromCreateDTO(CheckerCreateDTO dto, User user, Path compiledFilePath) {
        return new Checker(null, user, compiledFilePath.getFileName().toString(), fileManagementUtil.getDirectorySize(compiledFilePath), dto.languageId(), dto.name());
    }

    public CheckerOnPageDTO toPageDTO(Checker checker) {
        return new CheckerOnPageDTO(checker.getId(), checker.getName(), checker.getSizeBytes(), checker.getLanguageId(), checkerRepository.countPuzzlesByChecker(checker));
    }
}
