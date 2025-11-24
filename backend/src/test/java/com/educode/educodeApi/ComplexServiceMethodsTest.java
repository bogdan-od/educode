package com.educode.educodeApi;

import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.enums.TreeNodeType;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.*;
import com.educode.educodeApi.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для сложной сервисной логики, особенно для наследования.
 * Использует @SpringBootTest для доступа к репозиториям и реальной БД (H2),
 * но не использует MockMvc.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class ComplexServiceMethodsTest {

    @Autowired
    private UserService userService; // Используем реальный (или мок) из Abstract

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TreeNodeRepository treeNodeRepository;

    @Autowired
    private TreeNodeMemberRepository memberRepository;

    @Autowired
    private TreeNodeService treeNodeService;

    @Autowired
    private TreeNodePermissionService permissionService;

    @Autowired
    private TreeNodeHierarchyService hierarchyService;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Node root, child, grandChild;
    private Role nodeRoleView, nodeRoleEdit, childRoleManage;

    @BeforeEach
    void setUp() {
        roleService.initializeDefaultRoles(); // Убедимся, что роли есть

        user = new User("testUser", "test@user.com", "Test User", "pass");
        user = userRepository.save(user);

        // Создаем тестовые роли
        nodeRoleView = roleRepository.save(new Role("TEST_VIEW", "View", Set.of(PermissionType.VIEW_NODE_MEMBERS), 10L, com.educode.educodeApi.enums.RoleScope.NODE));
        nodeRoleEdit = roleRepository.save(new Role("TEST_EDIT", "Edit", Set.of(PermissionType.EDIT_NODES), 20L, com.educode.educodeApi.enums.RoleScope.NODE));
        childRoleManage = roleRepository.save(new Role("TEST_MANAGE", "Manage", Set.of(PermissionType.INVITE_USERS), 30L, com.educode.educodeApi.enums.RoleScope.NODE));

        // Создаем иерархию
        TreeNode rootTn = treeNodeService.createTreeNode(null, TreeNodeType.NODE, true);
        root = nodeRepository.save(new Node("Root", "Desc", rootTn));

        TreeNode childTn = treeNodeService.createTreeNode(rootTn, TreeNodeType.NODE, true);
        child = nodeRepository.save(new Node("Child", "Desc", childTn));

        TreeNode grandChildTn = treeNodeService.createTreeNode(childTn, TreeNodeType.NODE, true);
        grandChild = nodeRepository.save(new Node("GrandChild", "Desc", grandChildTn));
    }

    // --- Тесты для TreeNodeService.hasPermission ---

    @Test
    void testHasPermission_Direct() {
        // Даем право в самом узле
        memberRepository.save(new TreeNodeMember(user, child.getTreeNode(), Set.of(nodeRoleEdit)));

        assertTrue(treeNodeService.hasPermission(child.getTreeNode(), user, PermissionType.EDIT_NODES));
    }

    @Test
    void testHasPermission_Inherited() {
        // Даем право в родительском узле (root)
        memberRepository.save(new TreeNodeMember(user, root.getTreeNode(), Set.of(nodeRoleView)));

        // Проверяем право в дочернем (child)
        assertTrue(treeNodeService.hasPermission(child.getTreeNode(), user, PermissionType.VIEW_NODE_MEMBERS));
        // Проверяем право в (grandChild)
        assertTrue(treeNodeService.hasPermission(grandChild.getTreeNode(), user, PermissionType.VIEW_NODE_MEMBERS));
    }

    @Test
    void testHasPermission_Fail_NoPermission() {
        // Не даем прав нигде
        assertFalse(treeNodeService.hasPermission(grandChild.getTreeNode(), user, PermissionType.EDIT_NODES));
    }

    @Test
    void testHasPermission_Fail_PermissionDoesNotFlowUp() {
        // Даем право в дочернем узле
        memberRepository.save(new TreeNodeMember(user, grandChild.getTreeNode(), Set.of(nodeRoleEdit)));

        // Проверяем в родительском (не должно быть)
        assertFalse(treeNodeService.hasPermission(child.getTreeNode(), user, PermissionType.EDIT_NODES));
    }

    // --- Тесты для TreeNodePermissionService.getAllEffectivePermissions ---

    @Test
    void testGetAllEffectivePermissions_Combined() {
        // Даем права на разных уровнях
        memberRepository.save(new TreeNodeMember(user, root.getTreeNode(), Set.of(nodeRoleView))); // VIEW
        memberRepository.save(new TreeNodeMember(user, child.getTreeNode(), Set.of(nodeRoleEdit))); // EDIT
        memberRepository.save(new TreeNodeMember(user, grandChild.getTreeNode(), Set.of(childRoleManage))); // MANAGE

        // Проверяем права на самом нижнем узле (должны собраться все)
        Set<PermissionType> permissions = permissionService.getAllEffectivePermissions(grandChild.getTreeNode(), user);

        assertTrue(permissions.contains(PermissionType.VIEW_NODE_MEMBERS)); // От root
        assertTrue(permissions.contains(PermissionType.EDIT_NODES)); // От child
        assertTrue(permissions.contains(PermissionType.INVITE_USERS)); // От grandChild
        assertEquals(3, permissions.size());
    }

    @Test
    void testGetAllEffectivePermissions_OnlyRoot() {
        memberRepository.save(new TreeNodeMember(user, root.getTreeNode(), Set.of(nodeRoleView))); // VIEW

        Set<PermissionType> permissions = permissionService.getAllEffectivePermissions(grandChild.getTreeNode(), user);

        assertTrue(permissions.contains(PermissionType.VIEW_NODE_MEMBERS));
        assertEquals(1, permissions.size());
    }

    // --- Тесты для TreeNodeHierarchyService.canMoveTo ---

    @Test
    void testCanMoveTo_Success() {
        Node otherRoot = createNode("Other Root", null);
        // newParent (otherRoot) может иметь детей (getCanHaveChildren=true)
        assertTrue(hierarchyService.canMoveTo(child.getTreeNode(), otherRoot.getTreeNode()));
    }

    @Test
    void testCanMoveTo_Fail_MoveToSelf() {
        assertFalse(hierarchyService.canMoveTo(child.getTreeNode(), child.getTreeNode()));
    }

    @Test
    void testCanMoveTo_Fail_MoveToDescendant() {
        // Пытаемся переместить root (предок) в grandChild (потомок)
        assertFalse(hierarchyService.canMoveTo(root.getTreeNode(), grandChild.getTreeNode()));
    }

    @Test
    void testCanMoveTo_Fail_TargetCannotHaveChildren() {
        // Создаем Group (у ее TreeNode canHaveChildren=false)
        TreeNode groupTn = treeNodeService.createTreeNode(null, TreeNodeType.GROUP, false);

        // Пытаемся переместить узел в группу
        assertFalse(hierarchyService.canMoveTo(child.getTreeNode(), groupTn));
    }

    protected Node createNode(String title, TreeNode parent) {
        TreeNode treeNode = treeNodeService.createTreeNode(parent, TreeNodeType.NODE, true);
        Node node = new Node(title, "Description for " + title, treeNode);
        return nodeRepository.save(node);
    }
}
