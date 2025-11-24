package com.educode.educodeApi.functional;

import com.educode.educodeApi.enums.LogLevel;
import com.educode.educodeApi.exceptions.ResponseError;
import com.educode.educodeApi.utils.LogDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Глобальний обробник винятків для всього додатку.
 * Відповідає за централізовану обробку помилок та формування відповідей при виникненні винятків.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обробляє виняток невідповідності типів аргументів методу.
     *
     * @param ex Виняток, що містить інформацію про невідповідність типів
     * @return ResponseEntity з повідомленням про помилку та статусом BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Створюємо Map для зберігання відповіді
        Map<String, String> response = new HashMap<>();

        // Формуємо текст помилки з назвою параметра та його значенням
        String errorText = "Невірний параметр " + ex.getName() + "=" + ex.getValue();
        response.put("error", errorText);

        // Повертаємо відповідь з статусом 400 Bad Request
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseError.class)
    @ResponseBody
    public ResponseEntity<Map<String,Object>> handleResponseError(ResponseError ex) {
        LogDescriptor ld = ex.getLogDescriptor();

        if (ld != null && ld.enabled()) {
            var builder = switch (ld.level()) {
                case TRACE -> log.atTrace();
                case DEBUG -> log.atDebug();
                case INFO -> log.atInfo();
                case WARN -> log.atWarn();
                default -> log.atError();
            };

            if (ld.marker() != null) builder.addMarker(ld.marker());

            if (ld.messageTemplate() != null && !ld.messageTemplate().isEmpty()) {
            // prefer supplier-capable setMessage
                builder.setMessage(() -> ld.messageTemplate());
            }

            // add placeholder args lazily - LoggingEventBuilder.addArgument(Supplier...)
            List<Supplier<?>> args = ld.args();
            if (args != null && !args.isEmpty()) {
                for (Supplier<?> arg : args)
                    builder.addArgument(arg);
            }

            // add structured key-values lazily
            if (ld.keyValues() != null && !ld.keyValues().isEmpty()) {
                ld.keyValues().forEach((k, sup) -> builder.addKeyValue(k, sup));
            }

            if (ld.cause() != null) builder.setCause(ld.cause());

            builder.log();
        } else if (ld == null) {
            log.error("ResponseError without LogDescriptor: {} / {}", ex.getCode(), ex.getClientMessage());
        }

        return ResponseEntity.status(ex.getStatus()).body(ex.toBody());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b) -> a + "; " + b));

        LogDescriptor ld = LogDescriptor.builder()
                .level(LogLevel.INFO)
                .messageTemplate("Validation failed: {}")
                .arg(() -> fieldErrors.toString())
                .kv("validationErrors", () -> fieldErrors)
                .cause(ex)
                .build();

        ResponseError re = new ResponseError(HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed",
                Map.of("fields", fieldErrors),
                ld);

        return handleResponseError(re);
    }


    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String,Object>> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error", "code", "INTERNAL_ERROR"));
    }
}