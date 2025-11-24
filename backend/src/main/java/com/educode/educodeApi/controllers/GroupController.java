package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.group.*;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.lazyinit.GroupInclude;
import com.educode.educodeApi.mappers.GroupMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.GroupRepository;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.PaginationUtils;
import com.educode.educodeApi.utils.TextUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Контроллер для Group - только специфичные для Group операции
 * Общая логика (участники, задачи) вынесена в TreeNodeController
 */
@RestController
@RequestMapping("/api/group")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    private final GroupService groupService;
    private final NodeService nodeService;
    private final UserService userService;
    private final GroupMapper groupMapper;
    private final GroupRepository groupRepository;
    private final TreeNodeService treeNodeService;
    private final TreeNodeMemberService memberService;
    private final RoleService roleService;

    public GroupController(GroupService groupService,
                           NodeService nodeService,
                           UserService userService,
                           GroupMapper groupMapper,
                           GroupRepository groupRepository,
                           TreeNodeService treeNodeService,
                           TreeNodeMemberService memberService,
                           RoleService roleService) {
        this.groupService = groupService;
        this.nodeService = nodeService;
        this.userService = userService;
        this.groupMapper = groupMapper;
        this.groupRepository = groupRepository;
        this.treeNodeService = treeNodeService;
        this.memberService = memberService;
        this.roleService = roleService;
    }

    /**
     * Создать новую Group
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody @Valid GroupCreateDTO groupCreateDTO) {
        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(() -> TextUtils.ellipsize(groupCreateDTO.title(), 30))
                .addArgument(groupCreateDTO::parentId)
                .log("User {} @{} creating group '{}' under parentNodeId={}");

        TreeNode parentTreeNode = null;
        if (groupCreateDTO.parentId() != null) {
            Node parentNode = nodeService.findById(groupCreateDTO.parentId());
            if (parentNode == null) {
                throw new BadRequestError("Не знайдено верхній елемент за таким id");
            }
            parentTreeNode = parentNode.getTreeNode();
        }

        // Проверяем право на создание групп
        if (parentTreeNode == null) {
            // Создание "плавающей" группы без родителя - проверяем глобальное право
            if (!authUser.hasPermission(PermissionType.CREATE_GROUPS)) {
                throw new ForbiddenError("Вам не дозволено створювати корневі групи");
            }
        } else {
            // Создание группы в узле - проверяем право в родителе
            if (!treeNodeService.hasPermission(parentTreeNode, authUser, PermissionType.CREATE_GROUPS)) {
                throw new ForbiddenError("Вам не дозволено створювати тут групи");
            }
        }

        // Создаем Group с TreeNode
        Group group = groupService.createGroup(groupCreateDTO.title(), groupCreateDTO.description(), parentTreeNode);

        // Добавляем создателя как владельца
        TreeNodeMember member = memberService.addMember(
                authUser,
                group.getTreeNode(),
                Set.of(roleService.findByType("GROUP_OWNER")),
                true
        );

        log.debug("Group created with id={}, treeNodeId={}. Creator {} added as GROUP_OWNER.",
                group.getId(), group.getTreeNode().getId(), authUser.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Групу успішно створено",
                "groupId", group.getId(),
                "treeNodeId", group.getTreeNode().getId()
        ));
    }

    /**
     * Обновить Group
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable Long id,
            @RequestBody @Valid GroupUpdateDTO groupUpdateDTO) {

        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(id)
                .addArgument(() -> TextUtils.ellipsize(groupUpdateDTO.title(), 30))
                .addArgument(groupUpdateDTO::parentId)
                .log("User {} @{} updating group id={}. New title: '{}', New parent: {}");

        Group group = groupService.findById(id, Set.of(GroupInclude.TREE_NODE_PARENT));
        if (group == null) {
            throw new BadRequestError("Не знайдено групу за таким id");
        }

        TreeNode currentTreeNode = group.getTreeNode();
        TreeNode newParentTreeNode = null;

        if (groupUpdateDTO.parentId() != null) {
            Node newParentNode = nodeService.findById(groupUpdateDTO.parentId());
            if (newParentNode == null) {
                throw new BadRequestError("Не знайдено верхній елемент за таким id");
            }
            newParentTreeNode = newParentNode.getTreeNode();
        }

        // Проверка прав на редактирование
        if (!treeNodeService.hasPermission(currentTreeNode, authUser, PermissionType.EDIT_GROUPS)) {
            throw new ForbiddenError("Вам не дозволено редагувати цю групу");
        }

        // Если меняем родителя - проверяем права на перемещение
        if (!Objects.equals(currentTreeNode.getParent(), newParentTreeNode)) {
            log.debug("Parent change detected for group id={}. Old: {}, New: {}",
                    id, currentTreeNode.getParentId(), groupUpdateDTO.parentId());
            if (!treeNodeService.hasPermission(currentTreeNode, authUser, PermissionType.MOVE_GROUPS)) {
                throw new ForbiddenError("Вам не дозволено переміщувати цю групу");
            }

            if (newParentTreeNode != null &&
                    !treeNodeService.hasPermission(newParentTreeNode, authUser, PermissionType.CREATE_GROUPS)) {
                throw new ForbiddenError("Вам не дозволено додавати групи до цільового батька");
            }

            // Перемещаем группу
            groupService.moveGroup(group, newParentTreeNode);
        }

        // Обновляем Group
        group.setTitle(groupUpdateDTO.title());
        group.setDescription(groupUpdateDTO.description());
        groupService.update(group);

        return ResponseEntity.ok(Map.of(
                "message", "Групу успішно оновлено",
                "groupId", group.getId()
        ));
    }

    /**
     * Удалить Group
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long id) {
        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} deleting group id={}", authUser.getId(), authUser.getLogin(), id);

        Group group = groupService.findById(id);
        if (group == null) {
            throw new BadRequestError("Не знайдено групу за таким id");
        }

        TreeNode treeNode = group.getTreeNode();

        // Проверяем право на удаление
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.DELETE_GROUPS)) {
            throw new ForbiddenError("Вам не дозволено видаляти цю групу");
        }

        groupService.delete(group); // TreeNode удалится автоматически через cascade

        return ResponseEntity.ok(Map.of(
                "message", "Групу успішно видалено"
        ));
    }

    /**
     * Получить Group по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupViewDTO> getGroup(@PathVariable Long id) {
        log.debug("Request to get group id={}", id);
        Group group = groupService.findById(id, Set.of(GroupInclude.TREE_NODE));

        if (group == null) {
            throw new NotFoundError("Не знайдено групу за таким id");
        }

        return ResponseEntity.ok(groupMapper.toViewDTO(group));
    }

    @GetMapping("/by-tree-node/{treeNodeId}")
    public ResponseEntity<GroupViewDTO> getGroupByTreeNode(@PathVariable Long treeNodeId) {
        log.debug("Request to get group treeNodeId={}", treeNodeId);
        Group group = groupService.findByTreeNodeId(treeNodeId, Set.of(GroupInclude.TREE_NODE));

        if (group == null) {
            throw new NotFoundError("Не знайдено групу за таким treeNodeId");
        }

        return ResponseEntity.ok(groupMapper.toViewDTO(group));
    }

    /**
     * Получить список всех Group
     */
    @GetMapping("/list")
    public ResponseEntity<AllGroupViewDTO> getGroupList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        log.debug("Request to get group list. Page: {}, Limit: {}", page, limit);
        page = PaginationUtils.validatePage(page);

        Page<Group> groups = groupRepository.findAll(
                PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<GroupViewDTO> groupViewDTOS = groups.map(groupMapper::toViewDTO);

        return ResponseEntity.ok(new AllGroupViewDTO(groupViewDTOS, groupViewDTOS.hasNext()));
    }
}
