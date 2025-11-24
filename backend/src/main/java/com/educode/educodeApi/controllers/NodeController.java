package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.node.*;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.lazyinit.NodeInclude;
import com.educode.educodeApi.lazyinit.TreeNodeInclude;
import com.educode.educodeApi.mappers.NodeMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.NodeRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Контроллер для Node - только специфичные для Node операции
 * Общая логика (участники, задачи, иерархия) вынесена в TreeNodeController
 */
@RestController
@RequestMapping("/api/node")
public class NodeController {

    private static final Logger log = LoggerFactory.getLogger(NodeController.class);

    private final NodeService nodeService;
    private final UserService userService;
    private final NodeMapper nodeMapper;
    private final NodeRepository nodeRepository;
    private final TreeNodeService treeNodeService;
    private final TreeNodeMemberService memberService;
    private final RoleService roleService;

    public NodeController(NodeService nodeService,
                          UserService userService,
                          NodeMapper nodeMapper,
                          NodeRepository nodeRepository,
                          TreeNodeService treeNodeService,
                          TreeNodeMemberService memberService,
                          RoleService roleService) {
        this.nodeService = nodeService;
        this.userService = userService;
        this.nodeMapper = nodeMapper;
        this.nodeRepository = nodeRepository;
        this.treeNodeService = treeNodeService;
        this.memberService = memberService;
        this.roleService = roleService;
    }

    /**
     * Создать новый Node
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createNode(@RequestBody @Valid NodeCreateDTO nodeCreateDTO) {
        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(() -> TextUtils.ellipsize(nodeCreateDTO.title(), 30))
                .addArgument(nodeCreateDTO::parentId)
                .log("User {} @{} creating node '{}' under parentNodeId={}");

        TreeNode parentTreeNode = null;
        if (nodeCreateDTO.parentId() != null) {
            Node parentNode = nodeService.findById(nodeCreateDTO.parentId());
            if (parentNode == null) {
                throw new BadRequestError("Не знайдено батьківський елемент за таким id");
            }
            parentTreeNode = parentNode.getTreeNode();
        }

        // Проверяем право на создание узлов
        if (parentTreeNode == null) {
            // Создание корневого узла - проверяем глобальное право
            if (!authUser.hasPermission(PermissionType.CREATE_NODES)) {
                throw new ForbiddenError("Вам не дозволено створювати корневі вузли");
            }
        } else {
            // Создание дочернего узла - проверяем право в родителе
            if (!treeNodeService.hasPermission(parentTreeNode, authUser, PermissionType.CREATE_NODES)) {
                throw new ForbiddenError("Вам не дозволено створювати тут вузли");
            }
        }

        // Создаем Node с TreeNode
        Node node = nodeService.createNode(nodeCreateDTO.title(), nodeCreateDTO.description(), parentTreeNode);

        // Добавляем создателя как владельца
        TreeNodeMember member = memberService.addMember(
                authUser,
                node.getTreeNode(),
                Set.of(roleService.findByType("NODE_OWNER")),
                true
        );

        log.debug("Node created with id={}, treeNodeId={}. Creator {} added as NODE_OWNER.",
                node.getId(), node.getTreeNode().getId(), authUser.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Вузол успішно створено",
                "nodeId", node.getId(),
                "treeNodeId", node.getTreeNode().getId()
        ));
    }

    /**
     * Обновить Node
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNode(
            @PathVariable Long id,
            @RequestBody @Valid NodeUpdateDTO nodeUpdateDTO) {

        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(id)
                .addArgument(() -> TextUtils.ellipsize(nodeUpdateDTO.title(), 30))
                .addArgument(nodeUpdateDTO::parentId)
                .log("User {} @{} updating node id={}. New title: '{}', New parent: {}");

        Node node = nodeService.findById(id, Set.of(NodeInclude.TREE_NODE_PARENT));
        if (node == null) {
            throw new BadRequestError("Не знайдено вузол за таким id");
        }

        TreeNode currentTreeNode = node.getTreeNode();
        TreeNode newParentTreeNode = null;

        if (nodeUpdateDTO.parentId() != null) {
            Node newParentNode = nodeService.findById(nodeUpdateDTO.parentId());
            if (newParentNode == null) {
                throw new BadRequestError("Не знайдено батьківський елемент за таким id");
            }
            newParentTreeNode = newParentNode.getTreeNode();
        }

        // Проверка прав на редактирование
        if (!treeNodeService.hasPermission(currentTreeNode, authUser, PermissionType.EDIT_NODES)) {
            throw new ForbiddenError("Вам не дозволено редагувати цей вузол");
        }

        // Если меняем родителя - проверяем права на перемещение
        if (!Objects.equals(currentTreeNode.getParent(), newParentTreeNode)) {
            log.debug("Parent change detected for node id={}. Old: {}, New: {}",
                    id, currentTreeNode.getParentId(), nodeUpdateDTO.parentId());
            if (!treeNodeService.hasPermission(currentTreeNode, authUser, PermissionType.MOVE_NODES)) {
                throw new ForbiddenError("Вам не дозволено переміщувати цей вузол");
            }

            if (newParentTreeNode != null &&
                    !treeNodeService.hasPermission(newParentTreeNode, authUser, PermissionType.CREATE_NODES)) {
                throw new ForbiddenError("Вам не дозволено додавати вузли до цільового батька");
            }

            // Перемещаем узел
            nodeService.moveNode(node, newParentTreeNode);
        }

        // Обновляем Node
        node.setTitle(nodeUpdateDTO.title());
        node.setDescription(nodeUpdateDTO.description());
        nodeService.update(node);

        return ResponseEntity.ok(Map.of(
                "message", "Вузол успішно оновлено",
                "nodeId", node.getId()
        ));
    }

    /**
     * Удалить Node
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNode(@PathVariable Long id) {
        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} deleting node id={}", authUser.getId(), authUser.getLogin(), id);

        Node node = nodeService.findById(id);
        if (node == null) {
            throw new BadRequestError("Не знайдено вузол за таким id");
        }

        TreeNode treeNode = node.getTreeNode();

        // Проверяем наличие дочерних элементов
        if (treeNodeService.hasChildren(treeNode)) {
            throw new BadRequestError("Неможливо видалити вузол, що містить дочірні елементи");
        }

        // Проверяем право на удаление
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.DELETE_NODES)) {
            throw new ForbiddenError("Вам не дозволено видаляти цей вузол");
        }

        nodeService.delete(node); // TreeNode удалится автоматически через cascade

        return ResponseEntity.ok(Map.of(
                "message", "Вузол успішно видалено"
        ));
    }

    /**
     * Получить Node по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NodeViewDTO> getNode(@PathVariable Long id) {
        log.debug("Request to get node id={}", id);
        Node node = nodeService.findById(id, Set.of(NodeInclude.TREE_NODE));

        if (node == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        return ResponseEntity.ok(nodeMapper.toViewDTO(node));
    }

    @GetMapping("/by-tree-node/{treeNodeId}")
    public ResponseEntity<NodeViewDTO> getNodeByTreeNode(@PathVariable Long treeNodeId) {
        log.debug("Request to get node treeNodeId={}", treeNodeId);
        Node node = nodeService.findByTreeNodeId(treeNodeId, Set.of(NodeInclude.TREE_NODE));

        if (node == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        return ResponseEntity.ok(nodeMapper.toViewDTO(node));
    }

    /**
     * Получить список всех Node
     */
    @GetMapping("/list")
    public ResponseEntity<AllNodeViewDTO> getNodeList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        log.debug("Request to get node list. Page: {}, Limit: {}", page, limit);
        page = PaginationUtils.validatePage(page);

        Page<Node> nodes = nodeRepository.findAll(
                PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<NodeViewDTO> nodeViewDTOS = nodes.map(nodeMapper::toViewDTO);

        return ResponseEntity.ok(new AllNodeViewDTO(nodeViewDTOS, nodeViewDTOS.hasNext()));
    }

    /**
     * Получить корневые узлы (без родителя)
     */
    @GetMapping("/roots")
    public ResponseEntity<Map<String, Object>> getRootNodes() {
        log.debug("Request to get root nodes.");
        // Находим все Node, у которых TreeNode.parent == null
        List<Node> rootNodes = nodeService.findAllByTreeNode_ParentIsNull(Set.of(NodeInclude.TREE_NODE));

        return ResponseEntity.ok(Map.of(
                "nodes", rootNodes.stream().map(nodeMapper::toViewDTO).toList(),
                "total", rootNodes.size()
        ));
    }
}
