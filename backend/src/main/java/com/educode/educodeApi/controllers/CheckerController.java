package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.checker.*;
import com.educode.educodeApi.DTO.code.ProgrammingLanguage;
import com.educode.educodeApi.enums.LogLevel;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.ServerError;
import com.educode.educodeApi.exceptions.UnauthorizedError;
import com.educode.educodeApi.lazyinit.CheckerInclude;
import com.educode.educodeApi.lazyinit.UserInclude;
import com.educode.educodeApi.mappers.CheckerMapper;
import com.educode.educodeApi.models.Checker;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.CheckerRepository;
import com.educode.educodeApi.services.CheckerService;
import com.educode.educodeApi.services.ProgrammingLanguageService;
import com.educode.educodeApi.services.UserService;
import com.educode.educodeApi.utils.LogDescriptor;
import com.educode.educodeApi.utils.PaginationUtils;
import com.educode.educodeApi.utils.StdOptional;
import com.educode.educodeApi.utils.TextUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/checker")
public class CheckerController {

    private final UserService userService;
    private final CheckerMapper checkerMapper;
    private final CheckerService checkerService;
    private final CheckerRepository checkerRepository;
    private final ProgrammingLanguageService programmingLanguageService;

    public CheckerController(UserService userService, CheckerMapper checkerMapper, CheckerService checkerService, CheckerRepository checkerRepository, ProgrammingLanguageService programmingLanguageService) {
        this.userService = userService;
        this.checkerMapper = checkerMapper;
        this.checkerService = checkerService;
        this.checkerRepository = checkerRepository;
        this.programmingLanguageService = programmingLanguageService;
    }

    private static final Logger log = LoggerFactory.getLogger(CheckerController.class);

    @GetMapping("/my")
    public ResponseEntity<List<CheckerViewDTO>> getCheckers() {
        log.debug("Request for user checkers");

        User loggedUser = userService.getAuthUser(Set.of(UserInclude.CHECKERS));
        if (loggedUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(loggedUser.getCheckers().stream().map(checkerMapper::toViewDTO).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<AllCheckersDTO> getAll(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);

        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизовані");
        }

        if (limit > 1000) {
            throw new BadRequestError("На одній сторінці не може бути більш ніж 1000 checker'ів");
        }

        Page<Checker> notificationsPage = checkerService.findAll(PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id")));
        Page<CheckerOnPageDTO> dtoPage = notificationsPage.map(checkerMapper::toPageDTO);

        return ResponseEntity.ok(new AllCheckersDTO(dtoPage.getContent(), dtoPage.hasNext()));
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> create(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute @Valid CheckerCreateDTO checkerDTO) {

        User authUser = userService.getAuthUser(Set.of(UserInclude.CHECKERS));
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else if (!authUser.hasPermission(PermissionType.CREATE_PUZZLES)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.atInfo().addArgument(authUser::getId).addArgument(authUser::getLogin)
                .addArgument(() -> TextUtils.ellipsize(checkerDTO.name(), 20))
                .addArgument(checkerDTO.languageId()).log("User {} @{} is trying to create new Checker \"{}\" with language {}");

        Path compiledFilePath = checkerService.validateChecker(authUser, checkerDTO, file);

        log.debug("Saving checker to DB");
        Checker checker = checkerService.createChecker(checkerDTO, compiledFilePath, authUser);

        if (checker.getId() == null) {
            throw new ServerError("Несподівана помилка при збережені checker", LogDescriptor.of(
                    LogLevel.WARN,
                    "Failed to save checker"
            ));
        }

        return ResponseEntity.ok(Map.of("checkerId", checker.getId()));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<CheckerDetailDTO> get(@PathVariable Long id) {
        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизувались", LogDescriptor.of(LogLevel.DEBUG, "Unauthorized attempt to get checker #{}", List.of(() -> id)));
        }

        Checker checker = checkerService.findById(id, Set.of(CheckerInclude.PUZZLES));

        if (!Objects.equals(authUser.getId(), checker.getUser().getId())) {
            throw new ForbiddenError("Ви не можете дивитись цей checker", LogDescriptor.of(
                    LogLevel.DEBUG, "User {} @{} tried to get checker #{}, but its owner is {} @{}",
                    List.of(authUser::getId, authUser::getLogin, () -> id, () -> checker.getUser().getId(), () -> checker.getUser().getLogin())));
        }

        return ResponseEntity.ok(checkerMapper.toDetailDTO(checker));
    }

    @GetMapping("/get-edit/{id}")
    public ResponseEntity<CheckerDetailDTO> getEditVersion(@PathVariable Long id) {
        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизувались", LogDescriptor.of(LogLevel.DEBUG, "Unauthorized attempt to get checker #{}", List.of(() -> id)));
        }

        Checker checker = checkerService.findById(id);

        if (!Objects.equals(authUser.getId(), checker.getUser().getId())) {
            throw new ForbiddenError("Ви не можете дивитись цей checker", LogDescriptor.of(
                    LogLevel.DEBUG, "User {} @{} tried to get checker #{}, but its owner is {} @{}",
                    List.of(authUser::getId, authUser::getLogin, () -> id, () -> checker.getUser().getId(), () -> checker.getUser().getLogin())));
        }

        return ResponseEntity.ok(checkerMapper.toDetailDTO(checker, StdOptional.ofEmptySet()));
    }

    @GetMapping("/log/{id}")
    public ResponseEntity<CheckerLogDTO> getLog(@PathVariable Long id) {
        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизувались", LogDescriptor.of(LogLevel.DEBUG, "Unauthorized attempt to get checker #{}", List.of(() -> id)));
        }

        Checker checker = checkerService.findById(id);

        if (!Objects.equals(authUser.getId(), checker.getUser().getId())) {
            throw new ForbiddenError("Ви не можете дивитись логи цього checker'а", LogDescriptor.of(
                    LogLevel.DEBUG, "User {} @{} tried to get checker logs #{}, but its owner is another user", List.of(authUser::getId, authUser::getLogin, () -> id)));
        }

        CheckerDTO checkerTechDTO = checkerMapper.toTechDTO(checker);
        StringBuilder logContentBuilder = new StringBuilder();

        Path realPath = Paths.get(checkerTechDTO.logPath());
        if (!Files.exists(realPath)) {
            return ResponseEntity.notFound().build();
        }

        try (var stream = Files.lines(realPath)) {
            stream.forEach(s -> logContentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            throw new ServerError("Помилка при читанні логу checker'а", LogDescriptor.of(
                    LogLevel.WARN,
                    "Error reading file {} for checker #{}",
                    List.of(checkerTechDTO::logPath, () -> id)
            ));
        }

        long sizeBytes = 0L;

        try {
            sizeBytes = Files.size(realPath);
        } catch (IOException ignored) {
        }

        return ResponseEntity.ok(new CheckerLogDTO(logContentBuilder.toString(), DateTimeFormatter.ISO_INSTANT.format(Instant.now()), sizeBytes));
    }

    @PutMapping(path = "/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> editChecker(
            @PathVariable Long id,
            @ModelAttribute @Valid CheckerUpdateDTO checkerUpdateDTO,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        User authUser = userService.getAuthUser(Set.of(UserInclude.CHECKERS));

        if (authUser == null)
            throw new UnauthorizedError("Ви маєте бути авторизовані");

        Checker checker = checkerService.findById(id);

        if (checker == null)
            throw new BadRequestError("Checker за таким id не знайдено");

        if (!Objects.equals(authUser.getId(), checker.getUser().getId())) {
            throw new ForbiddenError("Ви не можете редагувати не свій checker", LogDescriptor.of(
                    LogLevel.WARN,
                    "User {} @{} tried to edit checker #{} owned by user {} @{}",
                    List.of(authUser::getId, authUser::getLogin, checker::getId, () -> checker.getUser().getId(), () -> checker.getUser().getLogin())
            ));
        }

        Path compiledFilePath = checkerService.validateChecker(authUser, checkerUpdateDTO, file, checker);
        checkerService.updateChecker(checker, checkerUpdateDTO, compiledFilePath);

        return ResponseEntity.ok(Map.of("success", "Checker успішно змінено"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteChecker(@PathVariable Long id) {
        User authUser = userService.getAuthUser();

        if (authUser == null)
            throw new UnauthorizedError("Ви маєте бути авторизовані");

        Checker checker = checkerService.findById(id, Set.of(CheckerInclude.PUZZLES));

        if (checker == null)
            throw new BadRequestError("Checker за таким id не знайдено");

        if (!Objects.equals(authUser.getId(), checker.getUser().getId())) {
            throw new ForbiddenError("Ви не можете видалити не свій checker", LogDescriptor.of(
                    LogLevel.WARN,
                    "User {} @{} tried to delete checker #{} owned by user {} @{}",
                    List.of(authUser::getId, authUser::getLogin, checker::getId, () -> checker.getUser().getId(), () -> checker.getUser().getLogin())
            ));
        }

        if (!checker.getPuzzles().isEmpty()) {
            throw new BadRequestError("Ви не можете видалити checker, що має задачі");
        }

        checker.prepareDelete();
        checkerService.delete(checker);

        return ResponseEntity.ok(Map.of("success", "Checker видалено"));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<Map<String, String>>> searchCheckers(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        log.debug("User {} @{} searching puzzles with q='{}', page={}, limit={}",
                authUser.getId(), authUser.getLogin(), q, page, limit);

        Page<Checker> checkers = checkerRepository.searchByUserAndQuery(q, authUser.getId(), PageRequest.of(page, limit));

        return ResponseEntity.ok(checkers.getContent().stream().map(checker -> Map.of(
                "id", checker.getId().toString(),
                "title", checker.getName(),
                "details", programmingLanguageService.getProgrammingLanguage(checker.getLanguageId()).map(ProgrammingLanguage::name).orElse("")
        )).toList());
    }
}
