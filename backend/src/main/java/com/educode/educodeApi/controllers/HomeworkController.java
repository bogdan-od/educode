package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.DecisionDTO;
import com.educode.educodeApi.DTO.homework.AllHomeworkViewDTO;
import com.educode.educodeApi.DTO.homework.HomeworkCreateDTO;
import com.educode.educodeApi.DTO.homework.HomeworkUpdateDTO;
import com.educode.educodeApi.DTO.homework.HomeworkViewDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.lazyinit.HomeworkInclude;
import com.educode.educodeApi.mappers.DecisionMapper;
import com.educode.educodeApi.mappers.HomeworkMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.DecisionRepository;
import com.educode.educodeApi.repositories.HomeworkRepository;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.PaginationUtils;
import com.educode.educodeApi.utils.StdOptional;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    private static final Logger log = LoggerFactory.getLogger(HomeworkController.class);

    private final HomeworkService homeworkService;
    private final GroupService groupService;
    private final PuzzleService puzzleService;
    private final UserService userService;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkRepository homeworkRepository;
    private final DecisionMapper decisionMapper;
    private final DecisionRepository decisionRepository;
    private final TreeNodeService treeNodeService;
    private final TreeNodeResourceService treeNodeResourceService;

    public HomeworkController(HomeworkService homeworkService,
                              GroupService groupService,
                              PuzzleService puzzleService,
                              UserService userService,
                              HomeworkMapper homeworkMapper,
                              HomeworkRepository homeworkRepository, DecisionMapper decisionMapper, DecisionRepository decisionRepository, TreeNodeService treeNodeService, TreeNodeResourceService treeNodeResourceService) {
        this.homeworkService = homeworkService;
        this.groupService = groupService;
        this.puzzleService = puzzleService;
        this.userService = userService;
        this.homeworkMapper = homeworkMapper;
        this.homeworkRepository = homeworkRepository;
        this.decisionMapper = decisionMapper;
        this.decisionRepository = decisionRepository;
        this.treeNodeService = treeNodeService;
        this.treeNodeResourceService = treeNodeResourceService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createHomework(@RequestBody @Valid HomeworkCreateDTO homeworkCreateDTO) {
        User authUser = userService.getAuthUserElseThrow();

        Group group = groupService.findById(homeworkCreateDTO.groupId());
        if (group == null) {
            throw new BadRequestError("Не знайдено групу за таким id");
        }

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(homeworkCreateDTO::title)
                .addArgument(homeworkCreateDTO::groupId)
                .addArgument(homeworkCreateDTO::puzzleId)
                .log("User {} @{} creating homework '{}' for groupId={}. PuzzleId: {}");

        if (!treeNodeService.hasPermission(group::getTreeNode, authUser, PermissionType.CREATE_HOMEWORKS)) {
            throw new ForbiddenError("Вам не дозволено створювати домашні завдання в цій групі");
        }

        Puzzle puzzle = null;
        if (homeworkCreateDTO.puzzleId() != null) {
            puzzle = puzzleService.findById(homeworkCreateDTO.puzzleId());
            if (puzzle == null) {
                throw new BadRequestError("Не знайдено задачу за таким id");
            }
        }

        Homework homework = homeworkMapper.fromDTO(homeworkCreateDTO, group, puzzle);
        homeworkRepository.save(homework);

        return ResponseEntity.ok(Map.of("id", homework.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateHomework(@PathVariable Long id,
                                                              @RequestBody @Valid HomeworkUpdateDTO homeworkUpdateDTO) {
        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(id)
                .addArgument(homeworkUpdateDTO::title)
                .addArgument(homeworkUpdateDTO::puzzleId)
                .log("User {} @{} updating homework id={}. New title: '{}', New puzzleId: {}");

        Homework homework = homeworkService.findById(id, Set.of(HomeworkInclude.GROUP));
        if (homework == null) {
            throw new BadRequestError("Не знайдено домашнє завдання за таким id");
        }

        if (!homeworkService.hasPermission(homework, authUser, PermissionType.EDIT_HOMEWORKS)) {
            throw new ForbiddenError("Вам не дозволено редагувати це домашнє завдання");
        }

        Puzzle puzzle = null;
        if (homeworkUpdateDTO.puzzleId() != null) {
            puzzle = puzzleService.findById(homeworkUpdateDTO.puzzleId());
            if (puzzle == null) {
                throw new BadRequestError("Не знайдено задачу за таким id");
            }
        }

        homeworkMapper.setFromUpdateDTO(homework, homeworkUpdateDTO, puzzle);
        homeworkRepository.save(homework);

        return ResponseEntity.ok(Map.of());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteHomework(@PathVariable Long id) {
        User authUser = userService.getAuthUserElseThrow();

        log.info("User {} @{} deleting homework id={}", authUser.getId(), authUser.getLogin(), id);

        Homework homework = homeworkService.findById(id, Set.of(HomeworkInclude.GROUP, HomeworkInclude.DECISIONS));
        if (homework == null) {
            throw new BadRequestError("Не знайдено домашнє завдання за таким id");
        }

        if (!homeworkService.hasPermission(homework, authUser, PermissionType.DELETE_HOMEWORKS)) {
            throw new ForbiddenError("Вам не дозволено видаляти це домашнє завдання");
        }

        // Проверяем, есть ли уже сданные решения
        if (homeworkService.getSubmissionsCount(homework) > 0) {
            throw new BadRequestError("Неможливо видалити домашнє завдання, яке вже має здані рішення");
        }

        homeworkRepository.delete(homework);

        return ResponseEntity.ok(Map.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HomeworkViewDTO> view(@PathVariable Long id) {
        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} viewing homework id={}", authUser.getId(), authUser.getLogin(), id);

        Homework homework = homeworkService.findById(id, Set.of(HomeworkInclude.GROUP, HomeworkInclude.PUZZLE));
        if (homework == null) {
            throw new NotFoundError();
        }

        // Проверяем, что пользователь является участником группы или имеет права на просмотр
        if (!homeworkService.isMember(homework, authUser) &&
                !homeworkService.hasPermission(homework, authUser, PermissionType.VIEW_ALL_HOMEWORKS)) {
            throw new ForbiddenError("Вам не дозволено переглядати це домашнє завдання");
        }

        HomeworkViewDTO dto = homeworkMapper.toViewDTO(homework);

        // Добавляем информацию о том, сдал ли пользователь задание
        boolean hasSubmitted = homeworkService.hasUserSubmitted(homework, authUser);

        return ResponseEntity.ok(homeworkMapper.enrichWithSubmissionInfo(dto, hasSubmitted));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<AllHomeworkViewDTO> viewByGroup(@PathVariable Long groupId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer limit,
                                                          @RequestParam(defaultValue = "false") Boolean includeExpired) {
        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        log.debug("User {} @{} viewing homeworks for groupId={}. Page: {}, Limit: {}, IncludeExpired: {}",
                authUser.getId(), authUser.getLogin(), groupId, page, limit, includeExpired);

        Group group = groupService.findById(groupId);
        if (group == null) {
            throw new BadRequestError("Не знайдено групу за таким id");
        }

        // Проверяем права на просмотр домашних заданий группы
        if (!treeNodeService.isMember(group::getTreeNode, authUser) &&
                !treeNodeService.hasPermission(group::getTreeNode, authUser, PermissionType.VIEW_ALL_HOMEWORKS)) {
            throw new ForbiddenError("Вам не дозволено переглядати домашні завдання цієї групи");
        }

        Page<Homework> homeworks = homeworkService.findByGroupAndDeadline(
                group,
                includeExpired,
                PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<HomeworkViewDTO> homeworkViewDTOS = homeworks.map(homework -> homeworkMapper.toViewDTO(homework, StdOptional.of(group)));

        return ResponseEntity.ok(new AllHomeworkViewDTO(homeworkViewDTOS, homeworkViewDTOS.hasNext()));
    }

    @GetMapping("/my")
    public ResponseEntity<AllHomeworkViewDTO> viewMyHomeworks(@RequestParam(defaultValue = "1") Integer page,
                                                              @RequestParam(defaultValue = "20") Integer limit,
                                                              @RequestParam(defaultValue = "false") Boolean includeExpired) {
        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        log.debug("User {} @{} viewing 'my' homeworks. Page: {}, Limit: {}, IncludeExpired: {}",
                authUser.getId(), authUser.getLogin(), page, limit, includeExpired);

        Page<Homework> homeworks;
        if (includeExpired) {
            homeworks = homeworkService.findByUserMembership(
                    authUser,
                    PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        } else {
            homeworks = homeworkService.findActiveByUserMembership(
                    authUser,
                    PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        Page<HomeworkViewDTO> homeworkViewDTOS = homeworks.map(hw -> {
            HomeworkViewDTO dto = homeworkMapper.toViewDTO(hw);
            boolean hasSubmitted = homeworkService.hasUserSubmitted(hw, authUser);
            return homeworkMapper.enrichWithSubmissionInfo(dto, hasSubmitted);
        });

        return ResponseEntity.ok(new AllHomeworkViewDTO(homeworkViewDTOS, homeworkViewDTOS.hasNext()));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<Page<DecisionDTO>> getSubmissions(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "20") Integer limit) {
        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        log.debug("User {} @{} viewing submissions for homework id={}. Page: {}",
                authUser.getId(), authUser.getLogin(), id, page);

        Homework homework = homeworkService.findById(id, Set.of(HomeworkInclude.GROUP));
        if (homework == null) {
            throw new BadRequestError("Не знайдено домашнє завдання за таким id");
        }

        // Только преподаватели могут смотреть все решения
        if (!homeworkService.hasPermission(homework, authUser, PermissionType.VIEW_HOMEWORK_SUBMISSIONS)) {
            throw new ForbiddenError("Вам не дозволено переглядати здані рішення цього завдання");
        }

        Page<Decision> decisions = decisionRepository.findAllByHomework(homework, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        boolean hasPermissionViewCode = homeworkService.hasPermission(homework, authUser, PermissionType.VIEW_HOMEWORK_DECISIONS_CODE);
        log.trace("User hasPermissionViewCode: {}", hasPermissionViewCode);
        Page<DecisionDTO> decisionDTOs = decisions.map(decision -> decisionMapper.toDTO(decision, hasPermissionViewCode));

        return ResponseEntity.ok(decisionDTOs);
    }
}
