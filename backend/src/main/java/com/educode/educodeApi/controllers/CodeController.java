package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.CodeExecuteDTO;
import com.educode.educodeApi.DTO.CodeTestDTO;
import com.educode.educodeApi.exceptions.ContainerBuildingException;
import com.educode.educodeApi.exceptions.ContainerExecutionException;
import com.educode.educodeApi.exceptions.ContainerTimeoutException;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.*;
import com.educode.educodeApi.services.CodeService;
import com.educode.educodeApi.services.CompileQueueService;
import com.educode.educodeApi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    private CodeService codeService;
    // Репозиторій для роботи з рішеннями
    @Autowired
    private DecisionRepository decisionRepository;
    // Сервіс для роботи з чергою компіляції
    @Autowired
    private CompileQueueService compileQueueService;
    // Сервіс для виконання завдань
    @Autowired
    private TaskExecutor taskExecutor;

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
    public ResponseEntity<Map<String, Object>> executeCode(@RequestBody CodeExecuteDTO codeExecuteDTO) {
        // Отримуємо авторизованого користувача
        User loggedUser = userService.getAuthUser();

        // Перевіряємо чи користувач авторизований
        if (loggedUser == null) {
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
            output = codeService.executeCode(spVals[0], spVals[1], codeExecuteDTO.getCode(), codeExecuteDTO.getInput(), 256, 5.0f);
        } catch (IllegalArgumentException e) {
            // Помилка якщо мова програмування не підтримується
            response.put("message", "Мова не підтримується на сервері");
            return ResponseEntity.status(422).body(response);
        } catch (ContainerBuildingException e) {
            // Помилка при компіляції коду
            response.put("error", "Помилка при компіляції");
            response.put("output", e.getMessage());
        } catch (ContainerExecutionException e) {
            // Обробка різних помилок виконання
            if (e.getCode() == 137) {
                response.put("error", "Перевищено ліміт пам'яті");
            } else if (e.getCode() == 1) {
                response.put("error", "Помилка при виконанні");
                response.put("output", e.getMessage());
            } else {
                response.put("message", "Несподівана помилка при виконанні");
                return ResponseEntity.status(500).body(response);
            }
            return ResponseEntity.status(200).body(response);
        } catch (ContainerTimeoutException e) {
            // Помилка перевищення часу виконання
            response.put("error", "Час (5 секунд) вийшов");
            return ResponseEntity.status(200).body(response);
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
    public SseEmitter testCode(@RequestBody CodeTestDTO codeTestDTO) throws IOException {
        SseEmitter emitter = new SseEmitter(180000L);

        // Перевіряємо авторизацію користувача
        User loggedUser = userService.getAuthUser();
        if (loggedUser == null) {
            emitter.send(SseEmitter.event().name("message").data("Щоб виконувати тестування ваших розв'язків, потрібно зареєструватися.").reconnectTime(3000));
            emitter.complete();
            return emitter;
        }

        // Перевіряємо наявність задачі та необхідних даних
        Puzzle puzzle = puzzleRepository.findById(codeTestDTO.getPuzzleId()).orElse(null);
        if (puzzle == null || codeTestDTO.getPuzzleId() == null || codeTestDTO.getCode() == null || codeTestDTO.getLanguage() == null) {
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
        thisDecision[0] = decisionRepository.save(thisDecision[0]);

        // Додаємо рішення в чергу компіляції
        compileQueueService.addToQueue(thisDecision[0].getId());

        try {
            // Очікуємо завершення компіляції
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
                int i = 1;
                List<PuzzleData> passedTests = new ArrayList<>();
                // Проходимо по всім тестам задачі
                for (PuzzleData puzzleData : puzzle.getPuzzleData()) {
                    String[] spVals = codeTestDTO.getLanguage().split(":");
                    String output;
                    try {
                        output = codeService.executeCode(spVals[0], spVals[1], codeTestDTO.getCode(), puzzleData.getInput(), 256, puzzle.getTimeLimit());
                        if (output.equals(puzzleData.getOutput())) {
                            emitter.send(SseEmitter.event().name("message").data("Test #" + i + " пройдено").reconnectTime(3000));
                            passedTests.add(puzzleData);
                        } else {
                            emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено").reconnectTime(3000));
                        }
                    } catch (IllegalArgumentException e) {
                        emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через те що мова не підтримується на сервері").reconnectTime(3000));
                    } catch (ContainerExecutionException e) {
                        if (e.getCode() == 137) {
                            emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через перевищення ліміту пам'яті (256MB)").reconnectTime(3000));
                        } else if (e.getCode() == 1) {
                            emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через помилку при виконанні").reconnectTime(3000));
                        } else {
                            emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через несподівану помилку при виконанні.").reconnectTime(3000));
                        }
                    } catch (ContainerTimeoutException e) {
                        emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через перевищення ліміту часу").reconnectTime(3000));
                    } catch (Exception e) {
                        emitter.send(SseEmitter.event().name("message").data("Test #" + i + " не пройдено, через несподівану помилку").reconnectTime(3000));
                    } finally {
                        i++;
                    }
                }

                // Підраховуємо загальний результат
                Float totalScore = passedTests.stream().map(PuzzleData::getScore).reduce(0f, Float::sum);
                emitter.send(SseEmitter.event().name("message").data("Тести пройдено: " + passedTests.size() + "/" + puzzle.getPuzzleData().size()).reconnectTime(3000));
                emitter.send(SseEmitter.event().name("message").data("Загальна оцінка: " + (Math.round(totalScore * 10) / 10.0f) + "/" + puzzle.getScore()).reconnectTime(3000));

                // Підраховуємо скільки раніше користувачу додавали за цю задачу
                int maxRatingPlus = Math.round(oldDecisions.stream().map(Decision::getScore).max(Comparator.naturalOrder()).orElse(0.0f));

                // Оновлюємо дані про рішення
                thisDecision[0].setCorrect(passedTests.size() == puzzle.getPuzzleData().size());
                thisDecision[0].setScore(totalScore);
                thisDecision[0].setFinished(true);
                decisionRepository.save(thisDecision[0]);

                // Оновлюємо рейтинг користувача
                Integer addRating = Math.round(totalScore) - maxRatingPlus;
                if (addRating > 0) {
                    loggedUser.setRating(loggedUser.getRating() + addRating);
                    userRepository.save(loggedUser);
                    emitter.send(SseEmitter.event().name("message").data("До рейтингу додано: " + addRating + "\nНовий рейтинг: " + loggedUser.getRating()).reconnectTime(3000));
                }

                // Видаляємо завдання з черги
                compileQueueService.removeFromQueue(thisDecision[0].getId());

                emitter.complete();
            } catch (IOException e) {
                compileQueueService.removeFromQueue(thisDecision[0].getId());
                decisionRepository.delete(thisDecision[0]);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}