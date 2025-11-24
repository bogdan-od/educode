package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.TreeNodeMemberDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDTO;
import com.educode.educodeApi.DTO.treenode.AddMemberDTO;
import com.educode.educodeApi.DTO.treenode.UpdateMemberRolesDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.exceptions.ServerError;
import com.educode.educodeApi.mappers.*;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.NodeRepository;
import com.educode.educodeApi.repositories.TreeNodeInvitationRepository;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.PaginationUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Контроллер для работы с иерархией TreeNode (общая логика для Node и Group)
 */
@RestController
@RequestMapping("/api/tree-node")
public class TreeNodeController {

    private static final Logger log = LoggerFactory.getLogger(TreeNodeController.class);

    private final TreeNodeService treeNodeService;
    private final TreeNodeMemberService memberService;
    private final TreeNodeHierarchyService hierarchyService;
    private final TreeNodeResourceService resourceService;
    private final TreeNodePermissionService permissionService;
    private final UserService userService;
    private final PuzzleService puzzleService;
    private final RoleService roleService;
    private final PuzzleMapper puzzleMapper;
    private final NodeRepository nodeRepository;
    private final GroupService groupService;
    private final NodeService nodeService;
    private final NodeMapper nodeMapper;
    private final GroupMapper groupMapper;
    private final TreeNodeMapper treeNodeMapper;
    private final TreeNodeMemberMapper memberMapper;
    private final TreeNodeEntityMapper treeNodeEntityMapper;
    private final TreeNodeInvitationRepository treeNodeInvitationRepository;
    private final TreeNodeInvitationMapper treeNodeInvitationMapper;

    public TreeNodeController(TreeNodeService treeNodeService,
                              TreeNodeMemberService memberService,
                              TreeNodeHierarchyService hierarchyService,
                              TreeNodeResourceService resourceService,
                              TreeNodePermissionService permissionService,
                              UserService userService,
                              PuzzleService puzzleService, RoleService roleService, PuzzleMapper puzzleMapper, NodeRepository nodeRepository, GroupService groupService, NodeService nodeService, NodeMapper nodeMapper, GroupMapper groupMapper, TreeNodeMapper treeNodeMapper, TreeNodeMemberMapper memberMapper, TreeNodeEntityMapper treeNodeEntityMapper, TreeNodeInvitationRepository treeNodeInvitationRepository, TreeNodeInvitationMapper treeNodeInvitationMapper) {
        this.treeNodeService = treeNodeService;
        this.memberService = memberService;
        this.hierarchyService = hierarchyService;
        this.resourceService = resourceService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.puzzleService = puzzleService;
        this.roleService = roleService;
        this.puzzleMapper = puzzleMapper;
        this.nodeRepository = nodeRepository;
        this.groupService = groupService;
        this.nodeService = nodeService;
        this.nodeMapper = nodeMapper;
        this.groupMapper = groupMapper;
        this.treeNodeMapper = treeNodeMapper;
        this.memberMapper = memberMapper;
        this.treeNodeEntityMapper = treeNodeEntityMapper;
        this.treeNodeInvitationRepository = treeNodeInvitationRepository;
        this.treeNodeInvitationMapper = treeNodeInvitationMapper;
    }

    /**
     * Добавить пользователя к узлу дерева
     */
    @PostMapping("/{treeNodeId}/members/add")
    public ResponseEntity<Map<String, Object>> addMemberToTreeNode(
            @PathVariable Long treeNodeId,
            @RequestBody @Valid AddMemberDTO addMemberDTO) {

        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(addMemberDTO::userId)
                .addArgument(treeNodeId)
                .addArgument(() -> String.join(",", addMemberDTO.roles()))
                .log("User {} @{} adding user id={} to treeNodeId={} with roles: {}");

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        User targetUser = userService.findById(addMemberDTO.userId());
        if (targetUser == null) {
            throw new BadRequestError("Не знайдено користувача за таким id");
        }

        boolean isAlreadyInHierarchy = false;
        if (treeNode.getParent() != null) {
            isAlreadyInHierarchy = treeNodeService.isMember(treeNode.getParent(), targetUser);
        }

        // Проверяем право на добавление пользователей
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.INVITE_USERS)) {
            throw new ForbiddenError("Вам не дозволено додавати користувачів до цього вузла");
        }

        if (!isAlreadyInHierarchy) {
            throw new ForbiddenError(
                    "Неможливо примусово додати користувача. " +
                            "Користувач ще не є учасником батьківського вузла. " +
                            "Користувач має спершу самостійно прийняти запрошення до одного з батьківських вузлів."
            );
        }

        // Проверяем, не является ли пользователь уже участником
        if (memberService.isMemberOfNode(targetUser, treeNode)) {
            throw new BadRequestError("Користувач вже є учасником цього вузла");
        }

        // Получаем роли
        Set<Role> roles = roleService.findRolesByTypes(addMemberDTO.roles()); // Предполагается что DTO содержит Set<Role>
        if (roles == null || roles.isEmpty()) {
            throw new BadRequestError("Потрібно вказати принаймні одну роль");
        }

        Long maxPriority = treeNodeService.getMaxRolePriorityInNodeOrAncestors(treeNode, authUser);
        if (roles.stream().anyMatch(role -> role.getPriority() > maxPriority)) {
            throw new BadRequestError("Ви не можете призначити роль, що має пріоритет вище за ваш");
        }

        boolean canLeave = addMemberDTO.canLeave() != null && addMemberDTO.canLeave();

        TreeNodeMember member = memberService.addMember(targetUser, treeNode, roles, canLeave);

        return ResponseEntity.ok(Map.of(
                "message", "Користувача успішно додано",
                "userId", targetUser.getId(),
                "treeNodeId", treeNode.getId(),
                "roles", roles.stream().map(Role::getName).toList()
        ));
    }

    /**
     * Позволяет пользователю самостоятельно выйти из узла.
     */
    @DeleteMapping("/{treeNodeId}/members/leave")
    public ResponseEntity<Map<String, Object>> leaveTreeNode(@PathVariable Long treeNodeId) {
        User authUser = userService.getAuthUserElseThrow();
        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        TreeNodeMember member = memberService.findByUserAndTreeNode(authUser, treeNode);
        if (member == null) {
            throw new BadRequestError("Ви не є учасником цього вузла");
        }

        if (!member.isCanLeave()) {
            throw new ForbiddenError("Вам не дозволено залишати цей вузол. Зверніться до адміністратора.");
        }

        if (treeNodeService.isLastTreeNodeAdmin(treeNode, member)) {
            throw new BadRequestError("Неможливо вийти, оскільки ви останній адміністратор. Спочатку передайте права іншому учаснику.");
        }

        memberService.removeMember(member);

        return ResponseEntity.ok(Map.of("message", "Ви успішно залишили вузол"));
    }

    /**
     * Удалить пользователя из узла дерева
     */
    @DeleteMapping("/{treeNodeId}/members/{userId}")
    public ResponseEntity<Map<String, Object>> removeMemberFromTreeNode(
            @PathVariable Long treeNodeId,
            @PathVariable Long userId) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} removing user id={} from treeNodeId={}",
                authUser.getId(), authUser.getLogin(), userId, treeNodeId);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        User targetUser = userService.findById(userId);
        if (targetUser == null) {
            throw new BadRequestError("Не знайдено користувача за таким id");
        }

        // Проверяем право на удаление пользователей
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.REMOVE_USERS)) {
            throw new ForbiddenError("Вам не дозволено видаляти користувачів з цього вузла");
        }

        TreeNodeMember member = memberService.findByUserAndTreeNode(targetUser, treeNode);
        if (member == null) {
            throw new BadRequestError("Користувач не є учасником цього вузла");
        }

        if (treeNodeService.isLastTreeNodeAdmin(treeNode, member)) {
            throw new BadRequestError("Неможливо видалити останнього адміністратора");
        }

        memberService.removeMember(member);

        return ResponseEntity.ok(Map.of(
                "message", "Користувача успішно видалено з вузла"
        ));
    }

    @GetMapping("/{treeNodeId}/path")
    public ResponseEntity<List<Map<String, Object>>> getTreeNodePath(@PathVariable Long treeNodeId) {
        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        List<TreeNode> breadcrumbs = treeNodeService.getPath(treeNode);
        List<TreeNodeEntity> treeNodeEntities = treeNodeService.findAllByTreeNodes(breadcrumbs);
        Map<Long, TreeNodeEntity> treeNodeNodeMap = treeNodeEntities.stream().collect(Collectors.toMap(tne -> tne.getTreeNode().getId(), Function.identity()));

        return ResponseEntity.ok(breadcrumbs.stream().map(tn -> new HashMap<String, Object>(Map.of(
            "nodeId", treeNodeNodeMap.get(tn.getId()).getId(),
            "treeNodeId", tn.getId(),
            "title", treeNodeNodeMap.get(tn.getId()).getTitle()
        ))).collect(Collectors.toList()));
    }

    private ResponseEntity<Map<String, Object>> getMemberships(User user, Integer page, Integer limit) {
        Page<TreeNodeMember> treeNodeMembers = memberService.findByUser(user, PageRequest.of(page, limit));
        List<TreeNodeEntity> treeNodeEntities = treeNodeService.findAllByTreeNodes(treeNodeMembers.map(TreeNodeMember::getTreeNode).stream().toList());
        Map<Long, TreeNodeMember> treeNodeRoleMap = treeNodeMembers.getContent().stream()
                .collect(Collectors.toMap(
                        tnm -> tnm.getTreeNode().getId(),
                        Function.identity()
                ));

        return ResponseEntity.ok(Map.of(
                "content", treeNodeEntities.stream().map(tne -> treeNodeEntityMapper.toViewDTO(tne, treeNodeRoleMap.get(tne.getTreeNode().getId()))).toList(),
                "hasNext", treeNodeMembers.hasNext()
        ));
    }

    @GetMapping("/my-memberships")
    public ResponseEntity<Map<String, Object>> getUserMemberships(@RequestParam(defaultValue = "1") Integer page,
                                                                  @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);
        if (limit > 200)
            throw new BadRequestError("Кількість на сторінці не може бути більше за 200");

        User authUser = userService.getAuthUserElseThrow();

        return getMemberships(authUser, page, limit);
    }

    @GetMapping("/memberships/{login}")
    public ResponseEntity<Map<String, Object>> getMembershipsByUser(@PathVariable String login,
                                                                    @RequestParam(defaultValue = "1") Integer page,
                                                                    @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);
        if (limit > 200)
            throw new BadRequestError("Кількість на сторінці не може бути більше за 200");

        User user = userService.findByLogin(login, Set.of()).orElse(null);
        if (user == null) throw new NotFoundError("Користувача не знайдено");

        return getMemberships(user, page, limit);
    }

    @GetMapping("/my-invitations")
    public ResponseEntity<Map<String, Object>> getMyInvitations(@RequestParam(defaultValue = "1") Integer page,
                                                                @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);
        User authUser = userService.getAuthUserElseThrow();

        Page<TreeNodeInvitation> treeNodeInvitations = treeNodeInvitationRepository.findAllByUser(authUser.getId(), PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id")));
        List<TreeNodeEntity> treeNodeEntities = treeNodeService.findAllByTreeNodes(treeNodeInvitations.map(TreeNodeInvitation::getTreeNode).getContent());
        Map<Long, TreeNodeEntity> treeNodeMap = treeNodeEntities.stream()
                .collect(Collectors.toMap(
                        tne -> tne.getTreeNode().getId(),
                        Function.identity()
                ));

        return ResponseEntity.ok(Map.of(
                "content", treeNodeInvitations.map(invitation -> treeNodeInvitationMapper.toSelfViewDTO(
                        invitation,
                        treeNodeMap.get(invitation.getTreeNode().getId()).getTitle()
                    )).getContent(),
                "hasNext", treeNodeInvitations.hasNext()
        ));
    }

    /**
     * Изменить роли пользователя в узле
     */
    @PutMapping("/{treeNodeId}/members/{userId}/roles")
    public ResponseEntity<Map<String, Object>> updateMemberRoles(
            @PathVariable Long treeNodeId,
            @PathVariable Long userId,
            @RequestBody @Valid UpdateMemberRolesDTO updateRolesDTO) {

        User authUser = userService.getAuthUserElseThrow();

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(userId)
                .addArgument(treeNodeId)
                .addArgument(() -> String.join(",", updateRolesDTO.roles()))
                .log("User {} @{} updating roles for user id={} in treeNodeId={} to: {}");

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        User targetUser = userService.findById(userId);
        if (targetUser == null) {
            throw new BadRequestError("Не знайдено користувача за таким id");
        }

        // Проверяем право на управление ролями
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.ASSIGN_ROLES)) {
            throw new ForbiddenError("Вам не дозволено змінювати ролі користувачів у цьому вузлі");
        }

        TreeNodeMember member = memberService.findByUserAndTreeNode(targetUser, treeNode);
        if (member == null) {
            throw new BadRequestError("Користувач не є учасником цього вузла");
        }

        Set<Role> newRoles = roleService.findRolesByTypes(updateRolesDTO.roles());
        if (newRoles == null || newRoles.isEmpty()) {
            throw new BadRequestError("Потрібно вказати принаймні одну роль");
        }

        Long maxPriority = treeNodeService.getMaxRolePriorityInNodeOrAncestors(treeNode, authUser);
        if (newRoles.stream().anyMatch(role -> role.getPriority() > maxPriority)) {
            throw new BadRequestError("Ви не можете призначити роль, що має пріоритет вище за ваш");
        }

        if (!Objects.equals(targetUser.getId(), authUser.getId())) {
            Long maxTargetUserPriority = treeNodeService.getMaxRolePriorityInNodeOrAncestors(treeNode, targetUser);
            if (maxTargetUserPriority > maxPriority) {
                throw new ForbiddenError("Ви не можете змінювати ролі користувача, що має ролі з пріоритетом, вищим за ваш");
            } else if (maxTargetUserPriority.equals(maxPriority) && !newRoles.containsAll(member.getRoles())) {
                throw new ForbiddenError("Ви не можете віднімати ролі у користувача, що має ролі з таким самим пріоритетом як ваш");
            }
        }

        // Проверяем, что не удаляем последнего администратора
        if (authUser.getId().equals(targetUser.getId())) {
            String adminRoleName = treeNodeService.getAdminRoleName(treeNode);

            boolean willBeOwner = newRoles.stream().anyMatch(r -> r.getName().equals(adminRoleName));

            if (treeNodeService.isLastTreeNodeAdmin(treeNode, member) && !willBeOwner) {
                throw new BadRequestError("Неможливо прибрати права адміністратора в останнього адміністратора");
            }
        }

        memberService.updateMemberRoles(member, newRoles);

        return ResponseEntity.ok(Map.of(
                "message", "Ролі користувача успішно оновлено",
                "userId", targetUser.getId(),
                "treeNodeId", treeNode.getId(),
                "roles", newRoles.stream().map(Role::getName).toList()
        ));
    }

    /**
     * Получить список участников узла
     */
    @GetMapping("/{treeNodeId}/members")
    public ResponseEntity<Page<TreeNodeMemberDTO>> getTreeNodeMembers(
            @PathVariable Long treeNodeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);

        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} fetching members for treeNodeId={}. Page: {}, Limit: {}",
                authUser.getId(), authUser.getLogin(), treeNodeId, page, limit);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        // Проверяем право на просмотр участников
        if (!treeNodeService.isMember(treeNode, authUser) &&
                !treeNodeService.hasPermission(treeNode, authUser, PermissionType.VIEW_NODE_MEMBERS)) {
            throw new ForbiddenError("Вам не дозволено переглядати учасників цього вузла");
        }

        Page<TreeNodeMember> members = memberService.findByTreeNode(treeNode, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id")));

        return ResponseEntity.ok(members.map(memberMapper::toViewDTO));
    }

    /**
     * Получить разрешения пользователя в узле (с учетом наследования)
     */
    @GetMapping("/{treeNodeId}/members/{userId}/permissions")
    public ResponseEntity<Set<PermissionType>> getMemberPermissions(
            @PathVariable Long treeNodeId,
            @PathVariable Long userId) {

        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} fetching permissions for user id={} in treeNodeId={}",
                authUser.getId(), authUser.getLogin(), userId, treeNodeId);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        User targetUser = userService.findById(userId);
        if (targetUser == null) {
            throw new BadRequestError("Не знайдено користувача за таким id");
        }

        // Проверяем право на просмотр прав
        if (!authUser.getId().equals(userId) &&
                !treeNodeService.hasPermission(treeNode, authUser, PermissionType.VIEW_USER_ROLES)) {
            throw new ForbiddenError("Вам не дозволено переглядати права цього користувача");
        }

        // Получаем все эффективные разрешения (с учетом наследования)
        Set<PermissionType> permissions = permissionService.getAllEffectivePermissions(treeNode, targetUser);

        return ResponseEntity.ok(permissions);
    }

    /**
     * Добавить Puzzle к узлу дерева
     */
    @PostMapping("/{treeNodeId}/puzzles/{puzzleId}/assign")
    public ResponseEntity<Map<String, Object>> assignPuzzleToTreeNode(
            @PathVariable Long treeNodeId,
            @PathVariable Long puzzleId) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} assigning puzzle id={} to treeNodeId={}",
                authUser.getId(), authUser.getLogin(), puzzleId, treeNodeId);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        Puzzle puzzle = puzzleService.findById(puzzleId);
        if (puzzle == null) {
            throw new BadRequestError("Не знайдено задачу за таким id");
        }

        // Проверяем право на добавление задач
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.ASSIGN_PUZZLE_TO_NODE)) {
            throw new ForbiddenError("Вам не дозволено додавати задачі до цього вузла");
        }

        // Проверяем, является ли пользователь автором задачи
        if (!puzzle.getUser().getId().equals(authUser.getId()) &&
                !authUser.hasPermission(PermissionType.PUBLISH_PUZZLE)) {
            throw new ForbiddenError("Ви можете додавати до вузла тільки свої задачі");
        }

        // Проверяем, не добавлена ли уже задача
        if (resourceService.isPuzzleAccessible(treeNode, puzzle)) {
            throw new BadRequestError("Задача вже додана до цього вузла");
        }

        resourceService.addPuzzleToTreeNode(puzzle, treeNode);

        return ResponseEntity.ok(Map.of(
                "message", "Задачу успішно додано до вузла (буде видима у всіх нащадках)",
                "puzzleId", puzzleId,
                "treeNodeId", treeNodeId,
                "puzzleVisible", puzzle.getVisible(),
                "visibleInNode", true
        ));
    }

    /**
     * Удалить Puzzle из узла дерева
     */
    @DeleteMapping("/{treeNodeId}/puzzles/{puzzleId}")
    public ResponseEntity<Map<String, Object>> removePuzzleFromTreeNode(
            @PathVariable Long treeNodeId,
            @PathVariable Long puzzleId) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} removing puzzle id={} from treeNodeId={}",
                authUser.getId(), authUser.getLogin(), puzzleId, treeNodeId);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        Puzzle puzzle = puzzleService.findById(puzzleId);
        if (puzzle == null) {
            throw new BadRequestError("Не знайдено задачу за таким id");
        }

        // Проверяем право на удаление задач
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.DELETE_PUZZLE_FROM_NODE)) {
            throw new ForbiddenError("Вам не дозволено видаляти задачі з цього вузла");
        }

        // Проверяем, есть ли задача в узле
        Page<Puzzle> directPuzzles = resourceService.getDirectPuzzles(treeNode, Pageable.unpaged());
        if (directPuzzles.stream().noneMatch(p -> p.getId().equals(puzzleId))) {
            throw new BadRequestError("Задача не прикріплена безпосередньо до этого вузла");
        }

        resourceService.removePuzzleFromTreeNode(puzzle, treeNode);

        return ResponseEntity.ok(Map.of(
                "message", "Задачу успішно видалено з вузла",
                "puzzleId", puzzleId,
                "treeNodeId", treeNodeId
        ));
    }

    /**
     * Получить все Puzzle узла (с учетом наследования или без)
     */
    @GetMapping("/{treeNodeId}/puzzles")
    public ResponseEntity<Map<String, Object>> getTreeNodePuzzles(
            @PathVariable Long treeNodeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "false") Boolean includeInherited) {
        page = PaginationUtils.validatePage(page);

        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} fetching puzzles for treeNodeId={}. Page: {}, Limit: {}, Inherited: {}",
                authUser.getId(), authUser.getLogin(), treeNodeId, page, limit, includeInherited);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        // Проверяем право на просмотр задач
        if (!treeNodeService.isMember(treeNode, authUser) &&
                !treeNodeService.hasPermission(treeNode, authUser, PermissionType.VIEW_NODE_PUZZLES)) {
            throw new ForbiddenError("Вам не дозволено переглядати задачі цього вузла");
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));

        Page<Puzzle> puzzlePage;
        if (includeInherited) {
            // Все доступные задачи (включая унаследованные)
            log.trace("Fetching inherited puzzles for treeNodeId={}", treeNodeId);
            puzzlePage = resourceService.getAllAccessiblePuzzles(treeNode, pageable);
        } else {
            // Только задачи, напрямую привязанные к узлу
            log.trace("Fetching direct puzzles for treeNodeId={}", treeNodeId);
            puzzlePage = resourceService.getDirectPuzzles(treeNode, pageable);
        }

        // Маппим страницу в DTO
        Page<PuzzleDTO> dtoPage = puzzlePage.map(puzzleMapper::toMinDTO);

        // ИСПРАВЛЕНО: Возвращаем стандартизированный ответ с пагинацией
        return ResponseEntity.ok(Map.of(
                "puzzles", dtoPage.getContent(),
                "totalPages", dtoPage.getTotalPages(),
                "totalElements", dtoPage.getTotalElements(),
                "currentPage", dtoPage.getNumber(),
                "hasNext", dtoPage.hasNext(),
                "includeInherited", includeInherited
        ));
    }

    /**
     * Получить дочерние узлы
     */
    @GetMapping("/{treeNodeId}/children")
    public ResponseEntity<Map<String, Object>> getChildren(@PathVariable Long treeNodeId) {
        log.debug("Request to get children for treeNodeId={}", treeNodeId);
        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        return ResponseEntity.ok(Map.of(
                "childNodes", hierarchyService.getChildNodes(treeNode).stream().map(nodeMapper::toViewDTO).toList(),
                "childGroups", hierarchyService.getChildGroups(treeNode).stream().map(groupMapper::toViewDTO).toList()
        ));
    }

    @GetMapping("/{treeNodeId}")
    public ResponseEntity<Object> getEntityByTreeNode(@PathVariable Long treeNodeId) {
        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        TreeNodeEntity treeNodeEntity = treeNodeService.findByTreeNode(treeNode);
        if (treeNodeEntity instanceof Node node) {
            return ResponseEntity.ok(nodeMapper.toViewDTO(node));
        } else if (treeNodeEntity instanceof Group group) {
            return ResponseEntity.ok(groupMapper.toViewDTO(group));
        } else {
            throw new ServerError("Не можемо впізнати що це за тип");
        }
    }

    /**
     * Переместить узел к новому родителю
     */
    @PutMapping("/{treeNodeId}/move")
    public ResponseEntity<Map<String, Object>> moveTreeNode(
            @PathVariable Long treeNodeId,
            @RequestParam(required = false) Long newParentId) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} moving treeNodeId={} to newParentId={}",
                authUser.getId(), authUser.getLogin(), treeNodeId, newParentId);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        TreeNode newParent = newParentId != null ? treeNodeService.findById(newParentId) : null;

        // Проверяем возможность перемещения
        if (!hierarchyService.canMoveTo(treeNode, newParent)) {
            throw new BadRequestError("Неможливо перемістити вузол до вказаного батька");
        }

        // Проверяем права
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.MOVE_NODES)) {
            throw new ForbiddenError("Вам не дозволено переміщувати цей вузол");
        }

        if (newParent != null && !treeNodeService.hasPermission(newParent, authUser, PermissionType.CREATE_NODES)) {
            throw new ForbiddenError("Вам не дозволено додавати вузли до цільового батька");
        }

        hierarchyService.moveTreeNode(treeNode, newParent);

        return ResponseEntity.ok(Map.of(
                "message", "Вузол успішно переміщено",
                "treeNodeId", treeNodeId,
                "newParentId", newParentId
        ));
    }

    @GetMapping("/{treeNodeId}/user-autocomplete")
    public ResponseEntity<List<Map<String, String>>> searchUsers(
            @PathVariable Long treeNodeId,
            @RequestParam String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        // Мінімальна довжина пошукового запиту (щоб уникнути надто широких результатів)
        if (q.length() < 3) {
            throw new BadRequestError("Пошуковий запит повинен містити щонайменше 3 символи");
        }

        TreeNode treeNode = treeNodeService.findById(treeNodeId);

        if (treeNode == null) {
            throw new NotFoundError("Не знайдено вузол за таким id");
        }

        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.VIEW_NODE_MEMBERS))
            throw new ForbiddenError("Вам не дозволено бачити учасників цього вузла");

        // Виконуємо пошук
        Page<User> usersPage = treeNodeService.findAccessibleUsersByLogin(
                treeNode,
                q.toLowerCase(),
                PageRequest.of(page, limit)
        );

        return ResponseEntity.ok(usersPage.getContent().stream().map(user -> Map.of(
                "id", user.getId().toString(),
                "title", user.getLogin(),
                "details", user.getName()
        )).toList());
    }
}
