<template>
    <!-- BREADCRUMBS -->
    <BreadcrumbNavigation :homeRoute="{ name: 'Оглядач вузлів', to: '/nodes' }" :crumbs="breadcrumbs"/>

    <h1>Керування користувачами: {{ title }}</h1>
    <hr>
    
    <div v-if="canInvite" class="management-section">
        <h2>Додати користувача</h2>
        <form @submit.prevent="addUser" class="user-form">
            <ApiSearchSelector 
                v-model="form.userId" 
                :apiUrl="`/tree-node/${treeNodeId}/user-autocomplete`" 
                label="Користувач" 
                placeholder="Пошук за логіном..." 
            />
            <MultiSelect 
                v-model="form.roles" 
                :options="availableRoles" 
                label="Ролі" 
                placeholder="Виберіть ролі"
                labelKey="description"
                valueKey="name"
            />
             <div class="form-group checkbox-group">
                <input type="checkbox" v-model="form.canLeave" id="can-leave" />
                <label for="can-leave">Дозволити користувачу виходити з вузла</label>
            </div>
            <LinkBtn 
                role="button" 
                type="submit" 
                anim="go" 
                class="submit-btn" 
                :disabled="!form.userId || form.roles.length === 0"
            >
                Додати
            </LinkBtn>
        </form>
    </div>

    <hr v-if="canInvite">
    
    <div class="management-section">
        <h2>Список користувачів ({{ totalUsers }})</h2>
        <div v-if="isLoading"><Preloader/></div>
        <div v-else-if="error" class="error-message">{{ error }}</div>
        <div v-else-if="users.length > 0" class="users-list">
            <div v-for="user in users" :key="user.id" class="user-item">
                <div class="user-info">
                    <p><strong>{{ user.user.name }}</strong> (@{{ user.user.login }})</p>
                    <!-- ИЗМЕНЕНО: Роли теперь кликабельные -->
                    <p class="roles">
                        Ролі:
                        <span 
                            v-for="role in user.roles" 
                            :key="role.name" 
                            class="role-badge"
                            @click="showRoleInfo(role.name)"
                            title="Дізнатися більше про роль"
                        >
                            {{ role.description }}
                        </span>
                    </p>
                </div>
                <div class="user-actions">
                    <button v-if="canAssignRoles" @click="openRoleEditor(user)" class="btn-edit" title="Змінити ролі">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button v-if="canRemoveUsers" @click="promptRemoveUser(user)" class="btn-delete" title="Видалити">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
            <!-- PAGINATION -->
            <button v-if="hasNextPage" @click="loadMoreUsers" class="load-more-btn">
                <Preloader v-if="loadingMore" :scale="0.5" />
                <span v-else>Завантажити ще</span>
            </button>
        </div>
        <p v-else class="empty-message">У цьому вузлі немає користувачів.</p>
    </div>

    <!-- Модальное окно для редактирования ролей -->
    <Modal v-model:visible="isRoleModalVisible" title="Редагувати ролі" :showCancelButton="true" @confirm="saveRoles" @cancel="isRoleModalVisible = false">
        <div v-if="editingUser">
            <p>Користувач: <strong>{{ editingUser.user.name }}</strong></p>
            <MultiSelect 
                v-model="editingRoles" 
                :options="availableRoles" 
                label="Ролі" 
                placeholder="Виберіть ролі"
                labelKey="description"
                valueKey="name"
            />
        </div>
    </Modal>

    <!-- ROLE INFO MODAL -->
    <RoleInfoModal v-model:visible="isRoleInfoModalVisible" :roleName="selectedRoleName" />
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import MultiSelect from '@/components/MultiSelect.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import Modal from '@/components/Modal.vue';
import BreadcrumbNavigation from '@/components/BreadcrumbNavigation.vue'; 
import RoleInfoModal from '@/components/RoleInfoModal.vue'; // ИМПОРТ МОДАЛКИ
import { handleApiError } from '@/services/errorHandler.js';
import { showQuestionModal } from '@/services/modalService.js';
import { hasPermission } from '@/services/permissionService.js';
import { getRoles } from '@/services/resourceService.js';

const route = useRoute();
const router = useRouter();
const store = useStore();
const treeNodeId = computed(() => route.params.id);

const users = ref([]);
const title = ref('');
const entityType = ref('node');
const isLoading = ref(true);
const loadingMore = ref(false);
const error = ref('');
const availableRoles = ref([]);
const breadcrumbs = ref([]);

// --- PAGINATION STATE ---
const currentPage = ref(1);
const hasNextPage = ref(false);
const totalUsers = ref(0);
// --- END PAGINATION ---

// --- ROLE INFO MODAL STATE ---
const isRoleInfoModalVisible = ref(false);
const selectedRoleName = ref(null);
const showRoleInfo = (roleName) => {
    selectedRoleName.value = roleName;
    isRoleInfoModalVisible.value = true;
};
// --- END ROLE MODAL ---

const form = reactive({ 
    userId: null, 
    roles: [],
    canLeave: true // Default to true
});
const isRoleModalVisible = ref(false);
const editingUser = ref(null);
const editingRoles = ref([]);

const canViewMembers = ref(false), canInvite = ref(false), canRemoveUsers = ref(false), canAssignRoles = ref(false);

const checkPermissions = async () => {
    canViewMembers.value = await hasPermission(treeNodeId.value, 'VIEW_NODE_MEMBERS') || await hasPermission(treeNodeId.value, 'VIEW_GROUP_MEMBERS');
    canInvite.value = await hasPermission(treeNodeId.value, 'INVITE_USERS');
    canRemoveUsers.value = await hasPermission(treeNodeId.value, 'REMOVE_USERS');
    canAssignRoles.value = await hasPermission(treeNodeId.value, 'ASSIGN_ROLES');
};

const fetchTitleAndPath = async () => {
    try {
        let entityTitle = '';
        let entityId = null;
        try {
            const nodeRes = await apiClient.get(`/node/by-tree-node/${treeNodeId.value}`);
            entityTitle = nodeRes.data.title;
            entityId = nodeRes.data.id;
            entityType.value = 'node';
        } catch (e) {
            const groupRes = await apiClient.get(`/group/by-tree-node/${treeNodeId.value}`);
            entityTitle = groupRes.data.title;
            entityId = groupRes.data.id;
            entityType.value = 'group';
        }
        title.value = entityTitle;
        
        const pathRes = await apiClient.get(`/tree-node/${treeNodeId.value}/path`);
        breadcrumbs.value = pathRes.data.map(crumb => ({
            name: crumb.title,
            to: crumb.treeNodeId === treeNodeId.value ? null : (crumb.nodeId ? (entityType.value === 'node' ? `/node/${crumb.nodeId}` : `/group/${crumb.nodeId}`) : null)
        })).filter(c => c.to); // Убираем текущую страницу из пути
        
        // breadcrumbs.value.push({ name: entityTitle, to: `/${entityType.value}/${entityId}` });
        breadcrumbs.value.push({ name: 'Учасники' }); // Add current page

    } catch (e) {
        handleApiError(e);
        title.value = `ID ${treeNodeId.value}`;
        breadcrumbs.value = [{ name: 'Учасники' }];
    }
};

const fetchUsers = async (page = 1) => {
    if (page === 1) isLoading.value = true;
    else loadingMore.value = true;
    
    error.value = '';
    
    if (!canViewMembers.value) {
        error.value = "У вас немає прав для перегляду користувачів.";
        isLoading.value = false;
        return;
    }
    
    try {
        const usersRes = await apiClient.get(`/tree-node/${treeNodeId.value}/members`, {
            params: {
                page: page,
                limit: 20 
            }
        });
        
        if (page === 1) {
            users.value = usersRes.data.content;
        } else {
            users.value.push(...usersRes.data.content);
        }
        hasNextPage.value = usersRes.data.hasNext;
        totalUsers.value = usersRes.data.totalElements;
        currentPage.value = page;

    } catch (err) {
        handleApiError(err);
        error.value = "Не вдалося завантажити дані.";
    } finally {
        isLoading.value = false;
        loadingMore.value = false;
    }
};

const loadMoreUsers = () => {
    if (hasNextPage.value) {
        fetchUsers(currentPage.value + 1);
    }
};

const fetchRoles = async () => {
    try {
        const rolesRes = await getRoles(entityType.value === 'node' ? 'NODE' : 'GROUP');
        const userMaxRank = store.getters.getMaxRoleRank;
        availableRoles.value = rolesRes.map(role => ({
            ...role,
            disabled: role.rank > userMaxRank 
        }));
    } catch (err) {
        handleApiError(err);
    }
};

const addUser = async () => {
    if (!form.userId || form.roles.length === 0) return;
    try {
        await apiClient.post(`/tree-node/${treeNodeId.value}/members/add`, { 
            userId: form.userId, 
            roles: form.roles,
            canLeave: form.canLeave
        });
        store.dispatch('addSuccessMessage', 'Користувача додано!');
        form.userId = null;
        form.roles = [];
        form.canLeave = true;
        fetchUsers(1); // Reload list from page 1
    } catch (err) { handleApiError(err); }
};

const removeUser = async (user) => {
    try {
        await apiClient.delete(`/tree-node/${treeNodeId.value}/members/${user.user.id}`);
        store.dispatch('addSuccessMessage', 'Користувача видалено!');
        fetchUsers(1); // Reload list
    } catch (err) { handleApiError(err); }
};

const promptRemoveUser = (user) => {
    showQuestionModal(
        'Видалити користувача?', 
        `Ви впевнені, що хочете видалити ${user.user.name} з цього вузла?`, 
        () => removeUser(user)
    );
};

const openRoleEditor = (user) => {
    editingUser.value = user;
    editingRoles.value = user.roles.map(r => r.name);
    isRoleModalVisible.value = true;
};

const saveRoles = async () => {
    if (!editingUser.value) return;
    try {
        await apiClient.put(`/tree-node/${treeNodeId.value}/members/${editingUser.value.user.id}/roles`, { 
            roles: editingRoles.value 
        });
        store.dispatch('addSuccessMessage', 'Ролі оновлено!');
        isRoleModalVisible.value = false;
        editingUser.value = null;
        fetchUsers(1); // Reload list
    } catch (err) { handleApiError(err); }
};

onMounted(async () => {
    isLoading.value = true;
    await checkPermissions();
    await fetchTitleAndPath();
    await fetchRoles();
    await fetchUsers(1);
    isLoading.value = false;
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

h1, h2 {
    font-family: $secondary-font;
    color: var(--text-color);
}

.management-section { 
    background-color: var(--weak-color); 
    padding: 20px; 
    border-radius: 5px; 
    margin-bottom: 20px; 
    border: 1px solid var(--secondary-color);
}

.user-form { 
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem; 
    align-items: flex-end; 
}

.submit-btn {
    height: 50px;
}

.checkbox-group {
    display: flex;
    align-items: center;
    height: 50px;
    gap: 10px;
    
    label {
        font-family: $form-font;
        font-size: 0.9em;
        color: var(--secondary-color);
        cursor: pointer;
    }
    input[type="checkbox"] {
        width: 18px;
        height: 18px;
    }
}

.users-list { 
    display: flex; 
    flex-direction: column; 
    gap: 10px; 
}

.user-item { 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    padding: 15px; 
    background-color: var(--main-color); 
    border-radius: 3px; 
    flex-wrap: wrap; 
    gap: 10px;
    border: 1px solid var(--secondary-color);
}

.user-info {
    font-family: $form-font;
    p {
        margin: 0;
        color: var(--text-color);
    }
    .roles {
        font-size: 0.9em;
        color: var(--secondary-color);
        margin-top: 0.5rem;
    }
}

// СТИЛИ ДЛЯ КЛИКАБЕЛЬНЫХ РОЛЕЙ
.role-badge { 
    background-color: var(--secondary-color); 
    color: white; 
    padding: 0.2rem 0.6rem; 
    border-radius: 5px; 
    font-size: 0.9em; 
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s ease;
    display: inline-block;
    margin-right: 5px;

    &:hover {
        background-color: var(--info-color);
    }
}
// -----------------------------

.user-actions { 
    display: flex; 
    gap: 10px; 
    
    button { 
        padding: 8px 10px; 
        border: none; 
        color: white; 
        border-radius: 3px; 
        cursor: pointer; 
        font-size: 1em;
        transition: opacity 0.2s ease;
        
        &:hover { 
            opacity: 0.8; 
        } 
    } 
    
    .btn-edit { 
        background-color: var(--warn-color); 
    } 
    .btn-delete { 
        background-color: var(--error-color); 
    } 
}

.error-message, .empty-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 20px;
    text-align: center;
    background-color: var(--main-color);
    border: 1px solid var(--secondary-color);
    border-radius: 3px;
}
.error-message {
    color: var(--error-color);
}

.load-more-btn {
    margin-top: 1rem;
    padding: 10px 15px;
    background-color: var(--main-color);
    border: 1px solid var(--secondary-color);
    color: var(--text-color);
    border-radius: 5px;
    cursor: pointer;
    font-family: $form-font;
    font-weight: 600;
    transition: background-color 0.2s ease;
    min-height: 40px;

    &:hover:not(:disabled) {
        background-color: var(--weak-color);
    }
    &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
    }
}
</style>
