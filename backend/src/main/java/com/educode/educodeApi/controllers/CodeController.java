package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.checker.CheckerDTO;
import com.educode.educodeApi.DTO.code.CodeExecuteDTO;
import com.educode.educodeApi.DTO.code.CodeTestDTO;
import com.educode.educodeApi.DTO.code.InteractiveResult;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.exceptions.*;
import com.educode.educodeApi.lazyinit.HomeworkInclude;
import com.educode.educodeApi.lazyinit.PuzzleInclude;
import com.educode.educodeApi.mappers.CheckerMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.properties.ContainerProperties;
import com.educode.educodeApi.repositories.DecisionRepository;
import com.educode.educodeApi.repositories.PuzzleDataRepository;
import com.educode.educodeApi.repositories.PuzzleRepository;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.DoubleUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Контролер для управління кодом та виконанням тестів
 */
@RestController // Вказуємо, що це контролер REST API, тоді відповідь буде в форматі JSON
@RequestMapping("/api/code")
public class CodeController {

    private static final Logger log = LoggerFactory.getLogger(CodeController.class);

    // Сервіс для роботи з користувачами
    @Autowired
    private UserService userService;
    // Репозиторій для роботи з задачами
    @Autowired
    private PuzzleRepository puzzleRepository;
    // Репозиторій для роботи з даними задач
    @Autowired
    private PuzzleDataRepository puzzleDataRepository;
    // Репозиторій для роботи з користувачами
    @Autowired
    private UserRepository userRepository;
    // Сервіс для виконання коду
    @Autowired
    private ContainerExecutionService containerExecutionService;
    // Репозиторій для роботи з рішеннями
    @Autowired
    private DecisionRepository decisionRepository;
    // Сервіс для роботи з чергою компіляції
    @Autowired
    private CompileQueueService compileQueueService;
    // Сервіс для виконання завдань
    @Autowired
    private TaskExecutor taskExecutor;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PuzzleService puzzleService;
    @Autowired
    private ContainerProperties containerProperties;
    @Autowired
    private CheckerMapper checkerMapper;
    @Autowired
    private ContainerExceptionMapper containerExceptionMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private HomeworkService homeworkService;
    @Autowired
    private TreeNodeService treeNodeService;
    @Autowired
    private TreeNodeHierarchyService treeNodeHierarchyService;

    /**
      * Виконує код, надісланий користувачем
      *
      * @param codeExecuteDTO DTO об'єкт, що містить код для виконання, мову програмування та вхідні дані
      * @return ResponseEntity з результатами виконання коду
      *         - У разі успіху повертає вихідні дані програми
      *         - У разі помилки повертає відповідне повідомлення про помилку
      *         - Код відповіді 401 якщо користувач не авторизований
      *         - Код відповіді 422 якщо мова програмування не підтримується
      *         - Код відповіді 500 у випадку внутрішньої помилки сервера
      */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeCode(@RequestBody @Valid CodeExecuteDTO codeExecuteDTO) {
        // Перевіряємо чи користувач авторизований
        if (!userService.isAuth()) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> response = new HashMap<>();

        // Генеруємо унікальний ідентифікатор для виконання
        Long executionId = new Random().nextLong();

        // Додаємо ідентифікатор до черги компіляції
        compileQueueService.addToQueue(executionId);

        try {
            // Очікуємо поки звільниться місце в черзі
            compileQueueService.waitForQueue(executionId);
        } catch (InterruptedException e) {
            // Видаляємо ідентифікатор з черги у випадку перевищення часу очікування
            compileQueueService.removeFromQueue(executionId);
            response.put("error", "Вихід за межі часу очікування");
            return ResponseEntity.ok(response);
        }

        String output = null;
        try {
            // Розділяємо мову програмування та версію
            String[] spVals = codeExecuteDTO.getLanguage().split(":");
            // Виконуємо код з обмеженнями пам'яті (256 МБ) та часу (5 секунд)
            output = containerExecutionService.runCode(spVals[0], spVals[1], codeExecuteDTO.getCode(), codeExecuteDTO.getInput(), containerProperties.getMemoryLimit(), containerProperties.getTimeLimit());
        } catch (ContainerException e) {
            var mapping = containerExceptionMapper.map(e);
            response.putAll(mapping.body());
            log.warn(e.getLogTemplate(), e.getLogArgs());
            return ResponseEntity.status(mapping.status()).body(response);
        } catch (Exception e) {
            // Обробка непередбачених помилок
            response.put("message", "Несподівана помилка");
            return ResponseEntity.status(500).body(response);
        } finally {
            // Видаляємо завдання з черги після завершення
            compileQueueService.removeFromQueue(executionId);
        }

        // Додаємо результат виконання до відповіді, якщо він ще не був доданий
        if (!response.containsKey("output"))
            response.put("output", output);
        return ResponseEntity.ok(response);
    }

    /**
      * Тестує код користувача на задачі
      *
      * @param codeTestDTO DTO з кодом, мовою програмування та ідентифікатором задачі
      * @return SseEmitter для відправки результатів тестування в реальному часі
      *         - Відправляє повідомлення про проходження кожного тесту
      *         - Відправляє загальний результат тестування
      *         - Оновлює рейтинг користувача при успішному проходженні тестів
      *         - Код відповіді 401 якщо користувач не авторизований
      * @throws IOException якщо виникла помилка при відправці даних
      */
    @PostMapping(value = "/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter testCode(@RequestBody @Valid CodeTestDTO codeTestDTO) throws IOException {
        SseEmitter emitter = new SseEmitter(180000L);

        // Перевіряємо авторизацію користувача
        User loggedUser = userService.getAuthUser();
        if (loggedUser == null) {
            emitter.send(SseEmitter.event().name("message").data("Щоб виконувати тестування ваших розв'язків, потрібно зареєструватися.").reconnectTime(3000));
            emitter.complete();
            return emitter;
        }

        // Перевіряємо наявність задачі та необхідних даних
        Puzzle puzzle = puzzleService.findById(codeTestDTO.getPuzzleId(), Set.of(PuzzleInclude.PUZZLE_DATA, PuzzleInclude.CHECKER));
        if (puzzle == null || codeTestDTO.getPuzzleId() == null || codeTestDTO.getCode() == null || codeTestDTO.getLanguage() == null) {
            emitter.complete();
            return emitter;
        }

        if (codeTestDTO.getTreeNodeId() != null && codeTestDTO.getHomeworkId() != null) {
            throw new BadRequestException("Ви не можете одночасно дивитися задачу з вузла та домашнього завдання");
        }

        Homework homework = null;
        if (codeTestDTO.getHomeworkId() != null) {
            homework = homeworkService.findById(codeTestDTO.getHomeworkId(), Set.of(HomeworkInclude.GROUP));

            if (homework == null)
                throw new BadRequestError("Домашнє завдання за таким ID не знайдено");
            if (!Objects.equals(homework.getPuzzleId(), codeTestDTO.getPuzzleId()) || !homeworkService.isMember(homework, loggedUser))
                throw new BadRequestError("Ви не можете відправити цю задачу в це домашнє завдання");
            if (homeworkService.hasUserSubmitted(homework, loggedUser))
                throw new BadRequestError("Ви не можете знову відправити цю задачу в це домашнє завдання");
            if (homework.isDeadlinePassed())
                throw new BadRequestError("Строк здачі цього завдання минув");
            if (!homeworkService.hasPermission(homework, loggedUser, PermissionType.SUBMIT_DECISIONS))
                throw new ForbiddenError("В цій групі вам не дозволено відправляти рішення");
        }

        if (codeTestDTO.getTreeNodeId() != null) {
            TreeNode treeNode = treeNodeService.findById(codeTestDTO.getTreeNodeId());

            if (treeNode == null)
                throw new BadRequestException("Вузол за таким ID не знайдено");
            if (!treeNodeService.hasResource(treeNode, puzzle))
                throw new BadRequestException("Задача не належить цьому вузлу");
            if (!treeNodeService.hasPermission(treeNode, loggedUser, PermissionType.SUBMIT_DECISIONS))
                throw new ForbiddenError("В цій групі вам не дозволено відправляти рішення");
        }

        if (!puzzle.getEnabled() && codeTestDTO.getHomeworkId() == null && codeTestDTO.getTreeNodeId() == null) {
            emitter.send(SseEmitter.event().name("message").data("Ця задача недоступна для нових рішень.").reconnectTime(3000));
            emitter.complete();
            return emitter;
        }

        // Перевіряємо чи немає незавершених рішень
        List<Decision> oldDecisions = decisionRepository.findAllByUserAndPuzzle(loggedUser, puzzle);

        LocalDateTime now = LocalDateTime.now();
        if (oldDecisions.stream().anyMatch(decision -> !decision.isFinished() && decision.getCreatedAt().isAfter(now.minusMinutes(10)))) {
            emitter.send(SseEmitter.event().name("message").data("Ви вже виконуєте тест").reconnectTime(3000));
            emitter.complete();
            return emitter;
        }

        // Створюємо нове рішення та додаємо його в чергу компіляції
        Decision[] thisDecision = new Decision[1];
        thisDecision[0] = new Decision(null, codeTestDTO.getCode(), codeTestDTO.getLanguage(), 0.0f, loggedUser, puzzle, LocalDateTime.now(), false, false);
        thisDecision[0].setHomework(homework);
        thisDecision[0] = decisionRepository.save(thisDecision[0]);

        // Додаємо рішення в чергу компіляції
        compileQueueService.addToQueue(thisDecision[0].getId());

        try {
            // Очікуємо черги
            compileQueueService.waitForQueue(thisDecision[0].getId());
        } catch (InterruptedException e) {
            // Видаляємо ідентифікатор з черги у випадку перевищення часу очікування
            decisionRepository.delete(thisDecision[0]);
            compileQueueService.removeFromQueue(thisDecision[0].getId());
            emitter.send(SseEmitter.event().name("message").data("Вихід за межі часу очікування").reconnectTime(3000));
            emitter.complete();
            return emitter;
        }

        // Запускаємо тестування
        taskExecutor.execute(() -> {
            try {
                String[] spVals = codeTestDTO.getLanguage().split(":");
                float totalScore = 0.0f;
                if (puzzle.getTaskType() == TaskType.NON_INTERACTIVE || puzzle.getTaskType() == TaskType.OUTPUT_CHECKING) {
                    int i = 1;
                    List<PuzzleData> passedTests = new ArrayList<>();
                    List<Pair<PuzzleData, Float>> halfPassedTests = new ArrayList<>();
                    // Проходимо по всім тестам задачі

                    CheckerDTO checkerDTO = checkerMapper.toTechDTO(puzzle.getChecker());
                    try (DockerContainer userContainer = containerExecutionService.prepareForRun(spVals[0], spVals[1], codeTestDTO.getCode(), containerProperties.getMemoryLimit(), TaskType.OUTPUT_CHECKING);
                         DockerContainer checkerContainer = puzzle.getTaskType() == TaskType.NON_INTERACTIVE ? null : userContainer.prepareChecker(checkerDTO, containerProperties.getCheckerMemoryLimit())) {
                        for (PuzzleData puzzleData : puzzle.getPuzzleData()) {
                            try {
                                InteractiveResult result = null;
                                boolean isCorrect = false;
                                boolean isIncorrect = true;
                                if (puzzle.getTaskType() == TaskType.NON_INTERACTIVE || (puzzle.getTaskType() == TaskType.OUTPUT_CHECKING && puzzleData.getOutput() != null)) {
                                    String output = containerExecutionService.runCode(userContainer, puzzleData.getInput(), puzzle.getTimeLimit());
                                    isCorrect = output.equals(puzzleData.getOutput());
                                    isIncorrect = !isCorrect;
                                } else if (puzzleData.getOutput() == null) {
                                    result = containerExecutionService.runCodeCheckingOutput(userContainer, checkerContainer, puzzleData.getInput(), puzzle.getTimeLimit(), checkerDTO);
                                    containerExceptionMapper.checkInteractiveResult(log, result, puzzle, thisDecision[0], loggedUser);
                                    isCorrect = result.getScore() == 100.0d;
                                    isIncorrect = result.getScore() == 0.0d;
                                }
                                if (isCorrect) {
                                    emitter.send(SseEmitter.event().name("message").data("Test #" + i + " пройдено").reconnectTime(3000));
                                    passedTests.add(puzzleData);
                                } else {
                                    emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено" + (result != null ? " (" + DoubleUtils.format(result.getScore(), 2) + "%)" : "")).reconnectTime(3000));
                                    if (!isIncorrect) {
                                        halfPassedTests.add(Pair.of(puzzleData, result.getRealScore(puzzleData.getScore())));
                                    }
                                }
                                if (result != null && result.getMessage() != null && !result.getMessage().isBlank()) {
                                    emitter.send(SseEmitter.event().name("message").data("Повідомлення від викладача: " + result.getMessage()).reconnectTime(3000));
                                }
                            } catch (ContainerException | InteractiveResultParsingException e) {
                                var mapper = containerExceptionMapper.map(e);
                                containerExceptionMapper.act(e, thisDecision[0], puzzle.getChecker());
                                emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено. " + mapper.message()).reconnectTime(3000));
                            } catch (Exception e) {
                                emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через несподівану помилку").reconnectTime(3000));
                            } finally {
                                i++;
                            }
                        }
                    } catch (ContainerCreateException e) {
                        emitter.send(SseEmitter.event().name("message").data("Тестування не пройдено через те, що на сервері сталася технічна помилка").reconnectTime(3000));
                        throw e;
                    } catch (UnsupportedLanguageException e) {
                        emitter.send(SseEmitter.event().name("message").data("Тестування не пройдено через те, що мова не підтримується на сервері").reconnectTime(3000));
                        throw e;
                    }

                    // Підраховуємо загальний результат
                    totalScore = passedTests.stream().map(PuzzleData::getScore).reduce(0f, Float::sum);
                    totalScore += halfPassedTests.stream().map(Pair::getSecond).reduce(0f, Float::sum);
                    if (!passedTests.isEmpty() || halfPassedTests.isEmpty()) {
                        emitter.send(SseEmitter.event().name("message").data("Тести пройдено: " + passedTests.size() + "/" + puzzle.getPuzzleData().size()).reconnectTime(3000));
                    }
                    if (!halfPassedTests.isEmpty()) {
                        emitter.send(SseEmitter.event().name("message").data("Тести пройдено частково: " + halfPassedTests.size() + "/" + puzzle.getPuzzleData().size()).reconnectTime(3000));
                    }

                    // Оновлюємо дані про рішення
                    thisDecision[0].setCorrect(passedTests.size() == puzzle.getPuzzleData().size());
                    thisDecision[0].setScore(totalScore);
                    thisDecision[0].setFinished(true);
                    decisionRepository.save(thisDecision[0]);
                } else if (puzzle.getTaskType() == TaskType.FULL_INTERACTIVE) {
                    InteractiveResult result;
                    try {
                        result = containerExecutionService.runCodeInteractive(spVals[0], spVals[1], codeTestDTO.getCode(), containerProperties.getMemoryLimit(), puzzle.getTimeLimit(), checkerMapper.toTechDTO(puzzle.getChecker()));
                    } catch (ContainerException | InteractiveResultParsingException e) {
                        var mapper = containerExceptionMapper.map(e);
                        containerExceptionMapper.act(e, thisDecision[0], puzzle.getChecker());
                        emitter.send(SseEmitter.event().name("message").data("Тестування не пройдено. " + mapper.message()).reconnectTime(3000));
                        throw e;
                    }

                    if (result.getMessage() != null && !result.getMessage().isBlank()) {
                        emitter.send(SseEmitter.event().name("message").data("Повідомлення від вчителя: " + result.getMessage()).reconnectTime(3000));
                    }

                    containerExceptionMapper.checkInteractiveResult(log, result, puzzle, thisDecision[0], loggedUser);

                    totalScore = result.getRealScore(puzzle.getScore());

                    // Оновлюємо дані про рішення
                    thisDecision[0].setCorrect(totalScore == puzzle.getScore());
                    thisDecision[0].setScore(totalScore);
                    thisDecision[0].setFinished(true);
                    decisionRepository.save(thisDecision[0]);
                }

                emitter.send(SseEmitter.event().name("message").data("Загальна оцінка: " + DoubleUtils.format(totalScore, 1) + "/" + DoubleUtils.format(puzzle.getScore(), 1)).reconnectTime(3000));

                // Підраховуємо скільки раніше користувачу додавали за цю задачу
                int maxRatingPlus = Math.round(oldDecisions.stream().map(Decision::getScore).max(Comparator.naturalOrder()).orElse(0.0f));

                // Оновлюємо рейтинг користувача
                Integer addRating = Math.round(totalScore) - maxRatingPlus;
                if (addRating > 0) {
                    loggedUser.setRating(loggedUser.getRating() + addRating);
                    userRepository.save(loggedUser);
                    emitter.send(SseEmitter.event().name("message").data("До рейтингу додано: " + addRating).reconnectTime(3000));
                    emitter.send(SseEmitter.event().name("message").data("Новий рейтинг: " + loggedUser.getRating()).reconnectTime(3000));
                }

                emitter.complete();
            } catch (IOException e) {
                decisionRepository.delete(thisDecision[0]);
                emitter.completeWithError(e);
            } catch (Exception e) {
                decisionRepository.delete(thisDecision[0]);
                emitter.complete();
                if (e instanceof ContainerException ce)
                    log.warn(ce.getLogTemplate(), ce.getLogArgs());
                else
                    log.error("Error while testing: {}", e.getMessage(), e);
            } finally {
                // Видаляємо завдання з черги
                compileQueueService.removeFromQueue(thisDecision[0].getId());
            }
        });

        return emitter;
    }
}