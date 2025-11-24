<template>
    <h1>Керування користувачами вузла: {{ nodeTitle }}</h1>
    <hr>
    
    <div v-if="canInvite" class="management-section">
        <h2>Додати користувача</h2>
        <form @submit.prevent="addUser" class="user-form">
            <ApiSearchSelector v-model="form.userId" apiUrl="/user/autocomplete" label="Користувач" placeholder="Пошук за логіном..." />
            <MultiSelect v-model="form.roles" :options="availableRoles" label="Ролі" placeholder="Виберіть ролі" />
            <LinkBtn role="btn" type="submit" anim="go" class="submit-btn">Додати</LinkBtn>
        </form>
    </div>

    <hr>
    
    <div class="management-section">
        <h2>Список користувачів</h2>
        <div v-if="isLoading"><Preloader/></div>
        <div v-else-if="error" class="error">{{ error }}</div>
        <div v-else-if="users.length > 0" class="users-list">
            <div v-for="user in users" :key="user.id" class="user-item">
                <div class="user-info">
                    <p><strong>{{ user.userName }}</strong> (ID: {{ user.userId }})</p>
                    <p>Ролі: {{ user.roles.join(', ') }}</p>
                </div>
                <div class="user-actions">
                    <button v-if="canAssignRoles" @click="promptUpdateRoles(user)" class="btn-edit">Змінити ролі</button>
                    <button v-if="canRemoveUsers" @click="promptRemoveUser(user.userId)" class="btn-delete">Видалити</button>
                </div>
            </div>
        </div>
        <p v-else>У вузлі немає користувачів.</p>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import MultiSelect from '@/components/MultiSelect.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import { handleApiError } from '@/services/errorHandler.js';
import { showQuestionModal } from '@/services/modalService.js';
import { hasPermission } from '@/services/permissionService.js';
import { getRoles } from '@/services/resourceService.js';

const route = useRoute();
const store = useStore();
const nodeId = computed(() => route.params.id);

const users = ref([]);
const nodeTitle = ref('');
const isLoading = ref(true);
const error = ref('');
const availableRoles = ref([]);
const form = reactive({ userId: null, roles: [] });

const canViewMembers = ref(false), canInvite = ref(false), canRemoveUsers = ref(false), canAssignRoles = ref(false);

const checkPermissions = async () => {
    canViewMembers.value = await hasPermission('node', nodeId.value, 'VIEW_NODE_MEMBERS');
    canInvite.value = await hasPermission('node', nodeId.value, 'INVITE_USERS');
    canRemoveUsers.value = await hasPermission('node', nodeId.value, 'REMOVE_USERS');
    canAssignRoles.value = await hasPermission('node', nodeId.value, 'ASSIGN_ROLES');
};

const fetchInitialData = async () => {
    isLoading.value = true;
    error.value = '';
    await checkPermissions();
    
    if (!canViewMembers.value) {
        error.value = "У вас немає прав для перегляду користувачів цього вузла.";
        isLoading.value = false;
        return;
    }
    
    try {
        const [usersRes, nodeRes, rolesRes] = await Promise.all([
            apiClient.get(`/node-user/node/${nodeId.value}`),
            apiClient.get(`/node/${nodeId.value}`),
            getRoles("NODE")
        ]);
        
        users.value = usersRes.data.users.content;
        nodeTitle.value = nodeRes.data.title;

        const userMaxRank = store.getters.getMaxRoleRank;
        availableRoles.value = rolesRes.map(role => ({...role, disabled: role.rank > userMaxRank }));

    } catch (err) {
        handleApiError(err);
        error.value = "Не вдалося завантажити дані.";
    } finally {
        isLoading.value = false;
    }
};

const addUser = async () => {
    if (!form.userId || form.roles.length === 0) return;
    try {
        await apiClient.post('/node-user/add', { userId: form.userId, nodeId: nodeId.value, roles: form.roles });
        store.dispatch('addSuccessMessage', 'Користувача додано!');
        form.userId = null;
        form.roles = [];
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

const removeUser = async (userId) => {
    try {
        await apiClient.delete('/node-user/remove', { data: { userId, nodeId: nodeId.value } });
        store.dispatch('addSuccessMessage', 'Користувача видалено!');
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

const promptRemoveUser = (userId) => showQuestionModal('Видалити користувача?', `Ви впевнені?`, () => removeUser(userId));

const updateRoles = async (userId, roles) => {
    try {
        await apiClient.put('/node-user/roles', { userId, nodeId: nodeId.value, roles });
        store.dispatch('addSuccessMessage', 'Ролі оновлено!');
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

const promptUpdateRoles = (user) => {
    const newRolesStr = prompt("Введіть нові ролі (через кому):", user.roles.join(', '));
    if (newRolesStr !== null) {
        updateRoles(user.userId, newRolesStr.split(',').map(r => r.trim().toUpperCase()).filter(Boolean));
    }
};

onMounted(fetchInitialData);
</script>

<style lang="scss" scoped>
.management-section { background-color: var(--weak-color); padding: 20px; border-radius: 5px; margin-bottom: 20px; }
.user-form { display: flex; flex-wrap: wrap; gap: 1rem; align-items: flex-end; > * { flex: 1 1 200px; } }
.submit-btn { max-width: 150px; }
.users-list { display: flex; flex-direction: column; gap: 10px; }
.user-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background-color: var(--main-color); border-radius: 3px; flex-wrap: wrap; }
.user-actions { display: flex; gap: 10px; button { padding: 5px 10px; border: none; color: white; border-radius: 3px; cursor: pointer; &:hover { opacity: 0.8; } } .btn-edit { background-color: #ffc107; } .btn-delete { background-color: #f44336; } }
.error { color: #f44336; }
</style>
