package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.*;
import com.educode.educodeApi.DTO.puzzle.*;
import com.educode.educodeApi.enums.LogLevel;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.exceptions.UnauthorizedError;
import com.educode.educodeApi.lazyinit.GroupInclude;
import com.educode.educodeApi.lazyinit.PuzzleInclude;
import com.educode.educodeApi.mappers.DecisionMapper;
import com.educode.educodeApi.mappers.PuzzleMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.CheckerRepository;
import com.educode.educodeApi.repositories.HomeworkRepository;
import com.educode.educodeApi.repositories.PuzzleDataRepository;
import com.educode.educodeApi.repositories.PuzzleRepository;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.LogDescriptor;
import com.educode.educodeApi.utils.PaginationUtils;
import com.educode.educodeApi.utils.Ref;
import com.educode.educodeApi.utils.TextUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Контролер для управління завданнями
 */
@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {

    private static final Logger log = LoggerFactory.getLogger(PuzzleController.class);

    private final UserService userService;
    private final PuzzleRepository puzzleRepository;
    private final PuzzleMapper puzzleMapper;
    private final CheckerRepository checkerRepository;
    private final PuzzleService puzzleService;
    private final HomeworkRepository homeworkRepository;
    private final TreeNodeService treeNodeService;
    private final TreeNodeResourceService resourceService;
    private final GroupService groupService;

    public PuzzleController(UserService userService,
                            PuzzleRepository puzzleRepository,
                            PuzzleMapper puzzleMapper,
                            CheckerRepository checkerRepository,
                            PuzzleService puzzleService,
                            HomeworkRepository homeworkRepository,
                            TreeNodeService treeNodeService,
                            TreeNodeResourceService resourceService, GroupService groupService) {
        this.userService = userService;
        this.puzzleRepository = puzzleRepository;
        this.puzzleMapper = puzzleMapper;
        this.checkerRepository = checkerRepository;
        this.puzzleService = puzzleService;
        this.homeworkRepository = homeworkRepository;
        this.treeNodeService = treeNodeService;
        this.resourceService = resourceService;
        this.groupService = groupService;
    }

    /**
     * Додає нове завдання до системи
     * @param puzzleDTO об'єкт завдання для додавання
     * @return відповідь з результатом операції
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addPuzzle(@RequestBody PuzzleCreateDTO puzzleDTO) {
        Map<String, Object> response = new HashMap<>();
        User loggedUser = userService.getAuthUser();

        log.debug("Someone is trying to create puzzle");

        // Перевірка авторизації користувача
        if (loggedUser == null)
            return ResponseEntity.status(401).build();
        else if (!loggedUser.hasPermission(PermissionType.CREATE_PUZZLES))
            return ResponseEntity.status(403).build();
        else if (!loggedUser.hasPermission(PermissionType.PUBLISH_PUZZLE) && puzzleDTO.getVisible())
            throw new ForbiddenError("Ви не можете створити видиму глобальну задачу, в вас недостатньо прав");

        log.atInfo().addArgument(loggedUser::getId)
                .addArgument(loggedUser::getLogin)
                .addArgument(() -> TextUtils.ellipsize(puzzleDTO.getTitle(), 30))
                .addArgument(puzzleDTO::getTaskType)
                .addArgument(() -> {
                    String part1 = (puzzleDTO.getTaskType() == TaskType.NON_INTERACTIVE || puzzleDTO.getTaskType() == TaskType.OUTPUT_CHECKING)
                            ? "puzzleData.size = " + (puzzleDTO.getPuzzleData() != null ? puzzleDTO.getPuzzleData().size() : null) : "";
                    String part2 = (puzzleDTO.getTaskType() == TaskType.FULL_INTERACTIVE || puzzleDTO.getTaskType() == TaskType.OUTPUT_CHECKING)
                            ? "checkerId = " + puzzleDTO.getCheckerId() : "";
                    return (part1 + " " + part2).trim();
                })
                .log("User {} @{} is creating puzzle \"{}\" (TaskType.{}) {}");

        Ref<Checker> checker = new Ref<>();

        // Розрахунок загального балу за завдання
        if (puzzleDTO.getTaskType() == TaskType.NON_INTERACTIVE || puzzleDTO.getTaskType() == TaskType.OUTPUT_CHECKING) {
            if (puzzleDTO.getPuzzleData() == null || puzzleDTO.getPuzzleData().size() < 2) {
                throw new BadRequestError(
                        "Тип задачі обраний як звичайний, тому кількість тест-кейсів не може бути менше двох",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with taskType {} and puzzleData is null or has size less 2",
                                List.of(puzzleDTO::getTaskType)
                        )
                );
            } else if (puzzleDTO.getPuzzleData() != null && puzzleDTO.getPuzzleData().size() > 10000) {
                throw new BadRequestError(
                        "Тест-кейсів не може бути більше ніж 10000",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with taskType {} and puzzleData size {}",
                                List.of(puzzleDTO::getTaskType, () -> puzzleDTO.getPuzzleData().size())
                        )
                );
            }

            Float score = 0.0f;

            for (PuzzleDataCreateDTO puzzleDataDTO : puzzleDTO.getPuzzleData())
                score += puzzleDataDTO.getScore();

            if (!Objects.equals(puzzleDTO.getScore(), score)) {
                log.warn("Puzzle score from DTO was {}, but real score is {}", puzzleDTO.getScore(), score);
            }

            puzzleDTO.setScore(score);

            int zeroScoreCount = 0;
            for (PuzzleDataCreateDTO puzzleDataDTO : puzzleDTO.getPuzzleData()) {
                if (puzzleDataDTO.getScore() == 0)
                    ++zeroScoreCount;
            }

            if (zeroScoreCount == 0) {
                log.debug("Each puzzleData has score more than 0");
                response.put("errors", Map.of("puzzleData", "Хоча б один тест кейс повинен мати 0 балів"));
                return ResponseEntity.status(400).body(response);
            }
        }

        if (puzzleDTO.getTaskType() == TaskType.OUTPUT_CHECKING || puzzleDTO.getTaskType() == TaskType.FULL_INTERACTIVE) {
            if (puzzleDTO.getCheckerId() == null) {
                throw new BadRequestError(
                        "Обраний тип задачі потребує наявного checker'а",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with taskType {}, but checkerId is null",
                                List.of(() -> puzzleDTO.getTaskType().toString())
                        )
                );
            }

            if (puzzleDTO.getTaskType() == TaskType.FULL_INTERACTIVE && puzzleDTO.getPuzzleData() != null && !puzzleDTO.getPuzzleData().isEmpty()) {
                throw new BadRequestError(
                        "Обраний тип задачі потребує відсутності тест-кейсів",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with taskType {}, but puzzleData.size is {}",
                                List.of(() -> puzzleDTO.getTaskType().toString(), () -> puzzleDTO.getPuzzleData().size())
                        )
                );
            }

            checker.set(checkerRepository.findById(puzzleDTO.getCheckerId()).orElse(null));
            if (checker.get() == null) {
                throw new BadRequestError(
                        "Checker за таким id не знайдено",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with checkerId={}, but checker not found in database",
                                List.of(puzzleDTO::getCheckerId)
                        )
                );
            }

            if (!Objects.equals(checker.get().getUser().getId(), loggedUser.getId())) {
                throw new BadRequestError(
                        "Checker має належати вам, але він належить іншому користувачеві",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with checkerId={}, but its owner is {} @{}",
                                List.of(puzzleDTO::getCheckerId, () -> checker.get().getUser().getId(), () -> checker.get().getUser().getLogin())
                        )
                );
            }
        }

        // Валідація даних завдання
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<PuzzleCreateDTO>> violations = validator.validate(puzzleDTO);

        if (!violations.isEmpty()) {
            log.debug("Validation failed");
            Map<String, String> errors = new HashMap<>();
            violations.forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
            response.put("errors", errors);
            return ResponseEntity.status(400).body(response);
        }

        // Збереження завдання та його даних

        Puzzle puzzle = puzzleMapper.fromCreateDTO(puzzleDTO, loggedUser, checker.get());
        puzzleRepository.save(puzzle);

        response.put("success", "Завдання успішно додано!");

        return ResponseEntity.ok(response);
    }

    /**
     * Отримує список всіх завдань з пагінацією та сортуванням
     * @param page номер сторінки
     * @param limit кількість елементів на сторінці
     * @param sortBy поле для сортування
     * @param sortOrder напрямок сортування
     * @return список завдань та інформація про пагінацію
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllPuzzles(@RequestParam Integer page, @RequestParam Integer limit, @RequestParam String sortBy, @RequestParam String sortOrder) {
        log.debug("Fetching all visible puzzles. Page: {}, Limit: {}, SortBy: {}, SortOrder: {}", page, limit, sortBy, sortOrder);
        page = PaginationUtils.validatePage(page);

        Pageable pageable = PageRequest.of(page, limit);
        Page<Puzzle> puzzlePage;
        Sort.Direction direction = Objects.equals(sortOrder, "asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Перевірка допустимих полів для сортування
        List<String> sortValues = Arrays.asList("id", "title", "description", "content", "timeLimit", "score");

        if (sortValues.contains(sortBy)) {
            puzzlePage = puzzleRepository.findAllByVisibleTrue(PageRequest.of(page, limit, Sort.by(direction, sortBy)));
        } else {
            puzzlePage = puzzleRepository.findAllByVisibleTrue(pageable);
        }

        List<PuzzleDTO> puzzleDTOS = puzzlePage.stream().map(puzzleMapper::toMinDTO).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("puzzles", puzzleDTOS);
        response.put("hasNext", puzzlePage.hasNext());

        return ResponseEntity.ok(response);
    }

    /**
     * Отримує деталі конкретного завдання за його ідентифікатором
     * @param id ідентифікатор завдання
     * @return деталі завдання та рішення, якщо користувач є автором
     */
    @Transactional
    @GetMapping("/get/{id}")
    public ResponseEntity<PuzzleDTO> getPuzzle(
            @PathVariable Long id,
            @RequestParam(name = "homeworkId", required = false) Long homeworkId,
            @RequestParam(name = "treeNodeId", required = false) Long treeNodeId) { // ИЗМЕНЕНО: теперь treeNodeId вместо groupId/nodeId

        Puzzle puzzle = puzzleRepository.findById(id).orElse(null);

        if (puzzle == null)
            throw new NotFoundError();

        User loggedUser = userService.getAuthUser();
        log.debug("User {} fetching puzzle id={} with context: homeworkId={}, treeNodeId={}",
                loggedUser != null ? loggedUser.getId() : "GUEST", id, homeworkId, treeNodeId);

        PuzzleDTO puzzleDTO = puzzleMapper.toDTO(puzzle);

        boolean isPuzzleVisible = puzzle.getVisible();

        if (homeworkId != null) {
            log.debug("Homework context provided: {}", homeworkId);
            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);

            if (homework != null && Objects.equals(homework.getPuzzleId(), id)) {
                TreeNode homeworkTreeNode = homework.getGroup().getTreeNode(); // Group теперь имеет TreeNode

                if (treeNodeService.isMember(homeworkTreeNode, loggedUser)) {
                    puzzleDTO.setEnabled(!homework.isDeadlinePassed());
                    isPuzzleVisible = resourceService.isPuzzleAccessible(homeworkTreeNode, puzzle) || isPuzzleVisible;
                } else {
                    // Пользователь не участник группы, к которой относится ДЗ
                    throw new NotFoundError();
                }
            } else {
                // ID ДЗ не совпадает с ID задачи
                throw new NotFoundError();
            }
        }

        // ОБНОВЛЕНО: работа через TreeNode
        if (treeNodeId != null) {
            log.debug("TreeNode context provided: {}", treeNodeId);
            TreeNode treeNode = treeNodeService.findById(treeNodeId);

            if (treeNode != null && treeNodeService.isMember(treeNode, loggedUser)) {
                isPuzzleVisible = resourceService.isPuzzleAccessible(treeNode, puzzle) || isPuzzleVisible;
            } else {
                // Пользователь не участник узла
                throw new NotFoundError();
            }
        }

        if (loggedUser != null && (Objects.equals(puzzle.getUser(), loggedUser) || loggedUser.hasPermission(PermissionType.VIEW_ALL_PUZZLES))) {
            log.trace("User is owner or admin, overriding visibility rules for puzzle id={}", id);
            isPuzzleVisible = true;
        }

        if (!isPuzzleVisible) {
            throw new NotFoundError();
        }

        return ResponseEntity.ok(puzzleDTO);
    }

    @GetMapping("/get-edit/{id}")
    public ResponseEntity<PuzzleDTO> getFullPuzzle(@PathVariable Long id) {
        Puzzle puzzle = puzzleService.findById(id, Set.of(PuzzleInclude.PUZZLE_DATA, PuzzleInclude.CHECKER));

        if (puzzle == null)
            return ResponseEntity.notFound().build();

        User authUser = userService.getAuthUser();
        if (authUser == null)
            throw new UnauthorizedError("Ви не авторизовані");

        log.debug("User {} @{} fetching full puzzle for edit id={}", authUser.getId(), authUser.getLogin(), id);

        if (!puzzle.getVisible() && !Objects.equals(puzzle.getUser(), authUser) && !authUser.hasPermission(PermissionType.VIEW_ALL_PUZZLES)) {
            throw new NotFoundError();
        }

        if (!Objects.equals(authUser.getId(), puzzle.getUser().getId()) && !authUser.hasPermission(PermissionType.VIEW_ALL_PUZZLES))
            throw new ForbiddenError("Ви не можете дивитися цю задачу цілком");

        PuzzleDTO puzzleDTO = puzzleMapper.toDTO(puzzle, puzzle.getChecker(), !Objects.equals(authUser.getId(), puzzle.getUser().getId()));

        return ResponseEntity.ok(puzzleDTO);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<Map<String, String>> editPuzzle(@PathVariable Long id, @RequestBody @Valid PuzzleUpdateDTO puzzleUpdateDTO) {
        User loggedUser = userService.getAuthUser();

        // Перевірка авторизації користувача
        if (loggedUser == null)
            return ResponseEntity.status(401).build();

        log.atInfo().addArgument(loggedUser::getId)
                .addArgument(loggedUser::getLogin)
                .addArgument(id)
                .addArgument(() -> TextUtils.ellipsize(puzzleUpdateDTO.getTitle(), 30))
                .log("User {} @{} is editing puzzle id={}. New title: '{}'");


        if (id == null) {
            throw new BadRequestError(
                    "Id не може бути порожнім",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = null"
                    )
            );
        }

        Puzzle original = puzzleService.findById(id, Set.of(PuzzleInclude.PUZZLE_DATA));

        if (original == null) {
            throw new BadRequestError(
                    "Задачу з таким ID не знайдено",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but puzzle is null",
                            List.of(() -> id)
                    )
            );
        }

        if (!Objects.equals(original.getUser().getId(), loggedUser.getId())) {
            throw new BadRequestError(
                    "Ви не можете змінити задачу іншого користувача",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but puzzle owner is {} @{}",
                            List.of(() -> id, () -> original.getUser().getId(), () -> original.getUser().getLogin())
                    )
            );
        }

        Set<Long> puzzleDataIds = original.getPuzzleData().stream().map(PuzzleData::getId).collect(Collectors.toSet());
        Set<Long> puzzleDataDTOIds = puzzleUpdateDTO.getPuzzleData() == null ? Set.of() :
                puzzleUpdateDTO.getPuzzleData().stream().map(PuzzleDataUpdateDTO::getId).collect(Collectors.toSet());

        log.atTrace().addArgument(() -> puzzleDataIds.stream().map(Object::toString).collect(Collectors.joining(","))).log("PuzzleDataIds: {}");
        log.atTrace().addArgument(() -> puzzleDataDTOIds.stream().map(Object::toString).collect(Collectors.joining(","))).log("PuzzleDataDTOIds: {}");

        if (!Objects.equals(puzzleDataIds, puzzleDataDTOIds)) {
            throw new BadRequestError(
                    "При редагуванні задачі кількість тест-кейсів повинна не змінюватись",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but changed length or content of test-cases",
                            List.of(() -> id)
                    )
            );
        }

        Ref<Checker> checker = new Ref<>();

        if (original.getTaskType() == TaskType.FULL_INTERACTIVE || original.getTaskType() == TaskType.OUTPUT_CHECKING) {
            if (puzzleUpdateDTO.getCheckerId() == null) {
                throw new BadRequestError(
                        "Обраний тип задачі потребує наявного checker'а",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with taskType {}, but checkerId is null",
                                List.of(() -> original.getTaskType().toString())
                        )
                );
            }

            checker.set(checkerRepository.findById(puzzleUpdateDTO.getCheckerId()).orElse(null));
            if (checker.get() == null) {
                throw new BadRequestError(
                        "Checker за таким id не знайдено",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with checkerId={}, but checker not found in database",
                                List.of(puzzleUpdateDTO::getCheckerId)
                        )
                );
            }

            if (!Objects.equals(checker.get().getUser().getId(), loggedUser.getId())) {
                throw new BadRequestError(
                        "Checker має належати вам, але він належить іншому користувачеві",
                        LogDescriptor.of(
                                LogLevel.WARN,
                                "User tried to create puzzle with checkerId={}, but its owner is {} @{}",
                                List.of(puzzleUpdateDTO::getCheckerId, () -> checker.get().getUser().getId(), () -> checker.get().getUser().getLogin())
                        )
                );
            }
        }

        if (puzzleUpdateDTO.getVisible() && !original.getVisible() && !loggedUser.hasPermission(PermissionType.PUBLISH_PUZZLE)) {
            throw new ForbiddenError("Ви не маєте достатньо прав, щоб опублікувати цю задачу");
        } else if (!puzzleUpdateDTO.getVisible() && original.getVisible()) {
            Set<Homework> homeworks = homeworkRepository.findByPuzzleAndDeadlineAfterNow(original);

            original.setVisible(false);
            puzzleRepository.save(original);

            for (Homework homework : homeworks) {
                if (!resourceService.isPuzzleAccessible(homework.getGroup().getTreeNode(), original)) {
                    throw new BadRequestError("Ви не можете зробити цю задачу невидимою, " +
                            "бо на неї існують домашні завдання в групах, яким ви не дали окремого права використовувати вашу задачу. " +
                            "Однак ви можете зробити її неактивною або надати групам окремі права. ", "CHANGE_VISIBILITY_ERROR");
                }
            }
        }

        puzzleMapper.setFromUpdateDTO(original, puzzleUpdateDTO, checker.get());

        puzzleRepository.save(original);

        return ResponseEntity.ok(Map.of("message", "success"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> deletePuzzle(@PathVariable Long id) {
        User loggedUser = userService.getAuthUser();

        // Перевірка авторизації користувача
        if (loggedUser == null)
            return ResponseEntity.status(401).build();

        log.info("User {} @{} is deleting puzzle id={}", loggedUser.getId(), loggedUser.getLogin(), id);

        if (id == null) {
            throw new BadRequestError(
                    "Id не може бути порожнім",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = null"
                    )
            );
        }

        Puzzle original = puzzleService.findById(id, Set.of(PuzzleInclude.PUZZLE_DATA, PuzzleInclude.DECISIONS));

        if (original == null) {
            throw new BadRequestError(
                    "Задачу з таким ID не знайдено",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but puzzle is null",
                            List.of(() -> id)
                    )
            );
        }

        if (!Objects.equals(original.getUser().getId(), loggedUser.getId())) {
            throw new BadRequestError(
                    "Ви не можете видалити задачу іншого користувача",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but puzzle owner is {} @{}",
                            List.of(() -> id, () -> original.getUser().getId(), () -> original.getUser().getLogin())
                    )
            );
        }

        if (!original.getDecisions().isEmpty()) {
            throw new BadRequestError(
                    "Ви не можете видалити задачу, яку вже хтось вирішив. ",
                    LogDescriptor.of(
                            LogLevel.WARN,
                            "User tried to edit puzzle with id = {}, but puzzle it has decisions, size = {}",
                            List.of(() -> id, () -> original.getDecisions().size())
                    )
            );
        }

        long homeworksCount = homeworkRepository.countByPuzzle(original);
        if (homeworksCount > 0) {
            throw new BadRequestError("На цю задачу вже створили домашні завдання, ви не можете її видалити. " +
                    "Однак ви можете зробити її неактивною. ");
        }

        puzzleRepository.delete(original);

        return ResponseEntity.ok(Map.of("message", "deleted"));
    }


    /**
     * Зробити задачу невидимою (адміністративна функція)
     */
    @PutMapping("/{puzzleId}/make-invisible")
    public ResponseEntity<Map<String, Object>> makePuzzleInvisible(@PathVariable Long puzzleId) {
        User authUser = userService.getAuthUserElseThrow();

        Puzzle puzzle = puzzleService.findById(puzzleId);
        if (puzzle == null) {
            throw new BadRequestError("Не знайдено задачу за таким id");
        }

        log.info("Admin {} @{} setting puzzle id={} visibility to FALSE. Current: {}",
                authUser.getId(), authUser.getLogin(), puzzleId, puzzle.getVisible());

        // Перевіряємо глобальне право
        if (!authUser.hasPermission(PermissionType.MAKE_PUZZLE_INVISIBLE)) {
            throw new ForbiddenError("Вам не дозволено робити задачі невидимими");
        }

        // Автор не може зробити свою задачу невидимою через цей ендпоінт
        // (це адміністративна функція для модерації)
        if (puzzle.getUser().getId().equals(authUser.getId())) {
            throw new BadRequestError("Використовуйте звичайний ендпоінт редагування для своїх задач");
        }

        if (!puzzle.getVisible()) {
            throw new BadRequestError("Задача вже є невидимою");
        }

        puzzle.setVisible(false);
        puzzleRepository.save(puzzle);

        return ResponseEntity.ok(Map.of(
                "message", "Задачу зроблено невидимою",
                "puzzleId", puzzleId,
                "visible", false,
                "note", "Задача залишається видимою в групах/вузлах, де вона була явно додана"
        ));
    }

    /**
     * Зробити задачу видимою знову (адміністративна функція)
     */
    @PutMapping("/{puzzleId}/make-visible")
    public ResponseEntity<Map<String, Object>> makePuzzleVisible(@PathVariable Long puzzleId) {
        User authUser = userService.getAuthUserElseThrow();

        Puzzle puzzle = puzzleService.findById(puzzleId);
        if (puzzle == null) {
            throw new BadRequestError("Не знайдено задачу за таким id");
        }

        log.info("Admin {} @{} setting puzzle id={} visibility to TRUE. Current: {}",
                authUser.getId(), authUser.getLogin(), puzzleId, puzzle.getVisible());

        // Перевіряємо глобальне право MAKE_PUZZLE_VISIBLE
        if (!authUser.hasPermission(PermissionType.MAKE_PUZZLE_VISIBLE)) {
            throw new ForbiddenError("Вам не дозволено публікувати задачі");
        }

        // Автор не може зробити свою задачу видимою через цей ендпоінт
        // (це адміністративна функція для модерації)
        if (puzzle.getUser().getId().equals(authUser.getId())) {
            throw new BadRequestError("Використовуйте звичайний ендпоінт редагування для своїх задач");
        }

        if (puzzle.getVisible()) {
            throw new BadRequestError("Задача вже є видимою");
        }

        puzzle.setVisible(true);
        puzzleRepository.save(puzzle);

        return ResponseEntity.ok(Map.of(
                "message", "Задачу опубліковано",
                "puzzleId", puzzleId,
                "visible", true
        ));
    }

    /**
     * Пошук задач за запитом з урахуванням видимості
     * GET /api/puzzles/search?q=...&page=1&limit=20&groupId=...&nodeId=...
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<Map<String, String>>> searchPuzzles(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) Long treeNodeId) { // ИЗМЕНЕНО: один treeNodeId вместо groupId/nodeId

        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        log.debug("User {} @{} searching puzzles with q='{}', treeNodeId={}, page={}, limit={}",
                authUser.getId(), authUser.getLogin(), q, treeNodeId, page, limit);

        TreeNode treeNode = null;

        // ОБНОВЛЕНО: единая логика для любого TreeNode
        if (treeNodeId != null) {
            treeNode = treeNodeService.findById(treeNodeId);
            if (treeNode == null) {
                throw new BadRequestError("Не знайдено вузол за таким id");
            }

            // Проверяем доступ
            if (!treeNodeService.isMember(treeNode, authUser) &&
                    !treeNodeService.hasPermission(treeNode, authUser, PermissionType.VIEW_NODE_MEMBERS)) {
                throw new ForbiddenError("Вам не дозволено переглядати задачі цього вузла");
            }
        }

        Page<Puzzle> puzzlesPage;

        if (treeNode != null) {
            // Поиск в контексте TreeNode
            log.trace("Searching in treeNodeId={}", treeNodeId);
            puzzlesPage = puzzleRepository.searchInTreeNode(
                    q.toLowerCase(),
                    treeNode.getId(),
                    authUser.getId(),
                    PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"))
            );
        } else {
            // Глобальный поиск
            log.trace("Searching globally");
            puzzlesPage = puzzleRepository.searchGlobal(
                    q.toLowerCase(),
                    authUser.getId(),
                    PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"))
            );
        }

        return ResponseEntity.ok(puzzlesPage.getContent().stream().map(puzzle -> Map.of(
                "id", puzzle.getId().toString(),
                "title", puzzle.getTitle(),
                "details", String.format("@%s %s", puzzle.getUser().getLogin(),
                        puzzle.getEnabled() ? "Активна задача" : "Неактивна задача")
        )).toList());
    }
}
