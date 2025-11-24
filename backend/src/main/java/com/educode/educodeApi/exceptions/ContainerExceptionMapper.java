package com.educode.educodeApi.exceptions;

import com.educode.educodeApi.DTO.code.InteractiveResult;
import com.educode.educodeApi.events.NotificationEvent;
import com.educode.educodeApi.enums.NotificationLevel;
import com.educode.educodeApi.models.Checker;
import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.utils.DoubleUtils;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ContainerExceptionMapper {
    private final ApplicationEventPublisher eventPublisher;

    public ContainerExceptionMapper(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void checkInteractiveResult(Logger log, InteractiveResult result, Puzzle puzzle, Decision decision, User user) {
        if (result.getScore() > 100.0d) {
            result.setScore(100.0d);
            log.warn("Checker #{} gave to the puzzle more than 100% - {}/{} for puzzle #{}", puzzle.getChecker().getId(), result.getRealScore(puzzle.getScore()), puzzle.getScore(), puzzle.getId());
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    "Помилка checker'а #%d у рішенні від користувача @%s #%d".formatted(puzzle.getChecker().getId(), user.getLogin(), decision.getId()),
                    ("""
Ваш checker дав користувачеві @%s %.2f%% від всіх балів, тому кількість балів була встановлена на 100%%.
Будь ласка, виправте checker або заморозьте задачу.
ID задачі: %d
ID рішення: %d""").formatted(user.getLogin(), result.getScore(), puzzle.getId(), decision.getId()),
                    puzzle.getChecker().getUser(),
                    NotificationLevel.CRITICAL
            ));
        }
    }

    public record Mapping(HttpStatus status, String message, Map<String, Object> body) {}

    public Mapping map(Exception ex) {
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Несподівана помилка на сервері";
        Map<String, Object> body = new HashMap<>();

        // decide status and default user message
        if (ex instanceof UnsupportedLanguageException) {
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
            message = "Мова програмування не підтримується на сервері";
        } else if (ex instanceof ContainerCreateException) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Помилка при створенні контейнеру";
        } else if (ex instanceof ContainerBuildingException ce) {
            httpStatus = HttpStatus.OK;
            message = "Помилка при компіляції";
            body.put("output", ce.getOutput());
        } else if (ex instanceof ContainerExecutionException ce) {
            if (ce.getCode() == 137) {
                httpStatus = HttpStatus.OK;
                message = "Перевищено ліміт пам'яті";
            } else if (ce.getCode() == 1) {
                httpStatus = HttpStatus.OK;
                message = "Помилка при виконанні";
                body.put("output", ce.getOutput());
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                message = "Несподівана помилка при виконанні";
            }
        } else if (ex instanceof ContainerTimeoutException ce) {
            httpStatus = HttpStatus.OK;
            message = "Час (%sс) вийшов".formatted(DoubleUtils.format(ce.getTime() / 1000.0d, 2));
        } else if (ex instanceof InteractiveResultParsingException) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "У перевіряючій програмі наявні помилки";
        }

        body.put("message", message);
        return new Mapping(httpStatus, message, body);
    }

    public void act(Exception ex, Decision decision, Checker checker) {
        if (ex instanceof InteractiveParseFloatException e) {
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    "Помилка checker'а #%d у рішенні від користувача @%s".formatted(checker.getId(), decision.getUser().getLogin()),
                    e.getLocalMessage(),
                    checker.getUser(),
                    NotificationLevel.WARN
            ));
        } else if (ex instanceof InteractiveScoreFileDoesNotExistsException e) {
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    "Помилка checker'а #%d у рішенні від користувача @%s".formatted(checker.getId(), decision.getUser().getLogin()),
                    e.getLocalMessage(),
                    checker.getUser(),
                    NotificationLevel.CRITICAL
            ));
        }
    }
}
