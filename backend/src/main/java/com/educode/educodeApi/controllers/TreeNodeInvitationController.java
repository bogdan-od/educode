package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.treenode.invitation.*;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.mappers.TreeNodeInvitationMapper;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.TreeNodeInvitation;
import com.educode.educodeApi.models.TreeNodeMember;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.services.TreeNodeInvitationService;
import com.educode.educodeApi.services.TreeNodeService;
import com.educode.educodeApi.services.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TreeNodeInvitationController {

    private static final Logger log = LoggerFactory.getLogger(TreeNodeInvitationController.class);

    private final TreeNodeInvitationService invitationService;
    private final TreeNodeService treeNodeService;
    private final UserService userService;
    private final TreeNodeInvitationMapper mapper;

    public TreeNodeInvitationController(TreeNodeInvitationService invitationService,
                                        TreeNodeService treeNodeService,
                                        UserService userService,
                                        TreeNodeInvitationMapper mapper) {
        this.invitationService = invitationService;
        this.treeNodeService = treeNodeService;
        this.userService = userService;
        this.mapper = mapper;
    }

    /**
     * Создать новое приглашение для узла
     */
    @PostMapping("/tree-node/{treeNodeId}/invitations")
    public ResponseEntity<InvitationViewDTO> createInvitation(
            @PathVariable Long treeNodeId,
            @RequestBody @Valid InvitationCreateDTO dto) {

        User authUser = userService.getAuthUserElseThrow();
        TreeNode treeNode = treeNodeService.findById(treeNodeId);
        if (treeNode == null) {
            throw new NotFoundError("Вузол не знайдено");
        }

        log.atInfo().addArgument(authUser::getId)
                .addArgument(authUser::getLogin)
                .addArgument(treeNodeId)
                .addArgument(dto::invitationType)
                .addArgument(dto::expiresAt)
                .log("User {} @{} creating invitation for treeNodeId={}. Type: {}, Expires: {}");

        // Логика создания и проверки прав находится в сервисе
        TreeNodeInvitation invitation = invitationService.createInvitation(authUser, treeNode, dto);

        return ResponseEntity.ok(mapper.toViewDTO(invitation));
    }

    /**
     * Получить список приглашений для узла (с пагинацией)
     */
    @GetMapping("/tree-node/{treeNodeId}/invitations")
    public ResponseEntity<PaginatedInvitationDTO> getInvitationsForNode(
            @PathVariable Long treeNodeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} fetching invitations for treeNodeId={}. Page: {}, Limit: {}",
                authUser.getId(), authUser.getLogin(), treeNodeId, page, limit);

        TreeNode treeNode = treeNodeService.findById(treeNodeId);
        if (treeNode == null) {
            throw new NotFoundError("Вузол не знайдено");
        }

        // Только участники с правом INVITE_USERS могут видеть список
        if (!treeNodeService.hasPermission(treeNode, authUser, PermissionType.INVITE_USERS)) {
            throw new ForbiddenError("У вас немає прав для перегляду запрошень");
        }

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.ASC, "id"));
        Page<TreeNodeInvitation> invitationPage = invitationService.findByTreeNode(treeNode, pageable);

        List<InvitationViewDTO> dtos = invitationPage.getContent().stream()
                .map(mapper::toViewDTO)
                .toList();

        return ResponseEntity.ok(new PaginatedInvitationDTO(dtos, invitationPage.hasNext()));
    }

    /**
     * Обновить приглашение (изменить срок, роли, активность)
     */
    @PutMapping("/tree-node/{treeNodeId}/invitations/{invitationId}")
    public ResponseEntity<InvitationViewDTO> updateInvitation(
            @PathVariable Long treeNodeId,
            @PathVariable Long invitationId,
            @RequestBody @Valid InvitationUpdateDTO dto) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} updating invitation id={} (for treeNodeId={})",
                authUser.getId(), authUser.getLogin(), invitationId, treeNodeId);

        // (Сервис проверит, что treeNodeId совпадает и права есть)

        TreeNodeInvitation updatedInvitation = invitationService.updateInvitation(authUser, invitationId, dto);

        return ResponseEntity.ok(mapper.toViewDTO(updatedInvitation));
    }

    /**
     * Удалить приглашение
     */
    @DeleteMapping("/tree-node/{treeNodeId}/invitations/{invitationId}")
    public ResponseEntity<Map<String, String>> deleteInvitation(
            @PathVariable Long treeNodeId,
            @PathVariable Long invitationId) {

        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} deleting invitation id={} (from treeNodeId={})",
                authUser.getId(), authUser.getLogin(), invitationId, treeNodeId);

        // (Сервис проверит права)

        invitationService.deleteInvitation(authUser, invitationId);

        return ResponseEntity.ok(Map.of("message", "Запрошення успішно видалено"));
    }

    /**
     * Просмотреть детали приглашения по коду (перед принятием)
     */
    @GetMapping("/invitations/view/{code}")
    public ResponseEntity<InvitationDetailsDTO> viewInvitation(@PathVariable String code) {
        User authUser = userService.getAuthUserElseThrow();
        log.debug("User {} @{} viewing invitation code={}",
                authUser.getId(), authUser.getLogin(), code);

        // validateInvitation проверит все, включая "уже участник"
        // Нам нужна "мягкая" проверка
        TreeNodeInvitation invitation = invitationService.findByCode(code);
        if (invitation == null || !invitation.isActive()) {
            throw new NotFoundError("Запрошення не знайдено або воно не активне");
        }

        return ResponseEntity.ok(mapper.toDetailsDTO(invitation));
    }

    /**
     * Принять приглашение по коду
     */
    @PostMapping("/invitations/accept/{code}")
    public ResponseEntity<Map<String, String>> acceptInvitation(@PathVariable String code) {
        User authUser = userService.getAuthUserElseThrow();
        log.info("User {} @{} accepting invitation code={}",
                authUser.getId(), authUser.getLogin(), code);

        // Вся логика валидации и принятия находится в сервисе
        TreeNodeMember newMember = invitationService.acceptInvitation(authUser, code);

        log.info("User {} @{} successfully joined treeNodeId={}",
                authUser.getId(), authUser.getLogin(), newMember.getTreeNode().getId());

        return ResponseEntity.ok(Map.of(
                "message", "Ви успішно приєдналися!",
                "treeNodeId", newMember.getTreeNode().getId().toString()
        ));
    }
}
