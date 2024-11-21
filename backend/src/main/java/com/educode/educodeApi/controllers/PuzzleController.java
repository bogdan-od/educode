package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.DecisionDTO;
import com.educode.educodeApi.DTO.PuzzleDTO;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.PuzzleData;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.PuzzleDataRepository;
import com.educode.educodeApi.repositories.PuzzleRepository;
import com.educode.educodeApi.services.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.educode.educodeApi.models.Decision;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Контролер для управління завданнями
 */
@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {

    @Autowired
    private UserService userService;
    @Autowired
    private PuzzleRepository puzzleRepository;
    @Autowired
    private PuzzleDataRepository puzzleDataRepository;

    /**
     * Додає нове завдання до системи
     * @param puzzle об'єкт завдання для додавання
     * @return відповідь з результатом операції
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addPuzzle(@RequestBody Puzzle puzzle) {
        Map<String, Object> response = new HashMap<>();
        User loggedUser = userService.getAuthUser();

        // Перевірка авторизації користувача
        if (loggedUser == null)
            return ResponseEntity.status(401).build();
        else if (loggedUser.getRoles().stream().noneMatch(role -> role.getName().equals("ROLE_PUZZLE_CREATOR")))
            return ResponseEntity.status(403).build();

        // Розрахунок загального балу за завдання
        Float score = 0.0f;

        for (PuzzleData puzzleData : puzzle.getPuzzleData())
            score += puzzleData.getScore();

        puzzle.setScore(score);

        // Валідація даних завдання
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<Puzzle>> violations = validator.validate(puzzle);

        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            violations.forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
            response.put("errors", errors);
            return ResponseEntity.status(400).body(response);
        }

        puzzle.setUser(loggedUser);

        // Збереження завдання та його даних
        Set<PuzzleData> puzzleDataSet = puzzle.getPuzzleData();
        puzzle.setPuzzleData(new HashSet<>());
        puzzleRepository.save(puzzle);

        for (PuzzleData puzzleData : puzzleDataSet) {
            puzzleData.setPuzzle(puzzle);
            puzzleDataRepository.save(puzzleData);
        }

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
        page -= 1;

        Pageable pageable = PageRequest.of(page, limit);
        Page<Puzzle> puzzlePage;
        Sort.Direction direction = Objects.equals(sortOrder, "asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Перевірка допустимих полів для сортування
        List<String> sortValues = Arrays.asList("id", "title", "description", "content", "timeLimit", "score");

        if (sortValues.contains(sortBy)) {
            puzzlePage = puzzleRepository.findAll(PageRequest.of(page, limit, Sort.by(direction, sortBy)));
        } else {
            puzzlePage = puzzleRepository.findAll(pageable);
        }

        List<PuzzleDTO> puzzleDTOS = puzzlePage.stream().map(puzzle -> new PuzzleDTO (puzzle, true)).collect(Collectors.toList());

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
    @GetMapping("/get/{id}")
    public ResponseEntity<PuzzleDTO> getPuzzle(@PathVariable Long id) {
        Puzzle puzzle = puzzleRepository.findById(id).orElse(null);

        if (puzzle == null)
            return ResponseEntity.notFound().build();

        PuzzleDTO puzzleDTO = new PuzzleDTO(puzzle);
        User loggedUser = userService.getAuthUser();

        // Якщо користувач є автором завдання, додаємо інформацію про рішення
        if (loggedUser != null && Objects.equals(loggedUser.getId(), puzzle.getUser().getId())) {
            List<DecisionDTO> puzzleDecisions = puzzle.getDecisions().stream()
                .collect(Collectors.groupingBy(Decision::getUser))
                .values().stream()
                .map(decisions -> decisions.stream().max(Comparator.comparing(Decision::getScore)).orElse(null))
                .filter(Objects::nonNull)
                .map(DecisionDTO::new)
                .sorted(Comparator.comparing(DecisionDTO::getId).reversed())
                .collect(Collectors.toList());

            puzzleDTO.setDecisions(puzzleDecisions);
        }

        return ResponseEntity.ok(puzzleDTO);
    }
}