<template>
    <h1>Керування користувачами групи: {{ groupTitle }}</h1>
    <hr>
    
    <div v-if="canInvite" class="management-section">
        <h2>Додати або запросити користувача</h2>
        <form @submit.prevent="addUser" class="user-form">
            <ApiSearchSelector v-model="form.userId" apiUrl="/user/autocomplete" label="Користувач" placeholder="Пошук за логіном..." />
            <MultiSelect v-model="form.roles" :options="availableRoles" label="Ролі" placeholder="Виберіть ролі" />
            <LinkBtn role="btn" type="submit" anim="go" class="submit-btn">Додати</LinkBtn>
        </form>
         <form @submit.prevent="inviteUser" class="user-form invite-form">
            <input v-model="inviteEmail" type="email" placeholder="Або запросити за Email..." class="email-input">
            <LinkBtn role="btn" type="submit" anim="go" class="submit-btn">Надіслати запрошення</LinkBtn>
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
        <p v-else>У групі немає користувачів.</p>
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
const groupId = computed(() => route.params.id);

const users = ref([]);
const groupTitle = ref('');
const isLoading = ref(true);
const error = ref('');
const availableRoles = ref([]);
const form = reactive({ userId: null, roles: [] });
const inviteEmail = ref('');

const canViewMembers = ref(false), canInvite = ref(false), canRemoveUsers = ref(false), canAssignRoles = ref(false);

const checkPermissions = async () => {
    canViewMembers.value = await hasPermission('group', groupId.value, 'VIEW_GROUP_MEMBERS');
    canInvite.value = await hasPermission('group', groupId.value, 'INVITE_USERS');
    canRemoveUsers.value = await hasPermission('group', groupId.value, 'REMOVE_USERS');
    canAssignRoles.value = await hasPermission('group', groupId.value, 'ASSIGN_ROLES');
};

const fetchInitialData = async () => {
    isLoading.value = true;
    error.value = '';
    await checkPermissions();
    
    if (!canViewMembers.value) {
        error.value = "У вас немає прав для перегляду користувачів цієї групи.";
        isLoading.value = false;
        return;
    }
    
    try {
        const [usersRes, groupRes, rolesRes] = await Promise.all([
            apiClient.get(`/group-user/group/${groupId.value}`),
            apiClient.get(`/group/${groupId.value}`),
            getRoles("GROUP")
        ]);
        
        users.value = usersRes.data.users.content;
        groupTitle.value = groupRes.data.title;

        // Filter roles based on user's max rank
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
        await apiClient.post('/group-user/add', { userId: form.userId, groupId: groupId.value, roles: form.roles });
        store.dispatch('addSuccessMessage', 'Користувача додано!');
        form.userId = null;
        form.roles = [];
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

const inviteUser = async () => {
    if (!inviteEmail.value) return;
    try {
        await apiClient.post('/group-user/invite', { email: inviteEmail.value, groupId: groupId.value, roles: form.roles });
        store.dispatch('addSuccessMessage', 'Запрошення надіслано!');
        inviteEmail.value = '';
        form.roles = [];
    } catch (err) { handleApiError(err); }
};

const removeUser = async (userId) => {
    try {
        await apiClient.delete('/group-user/remove', { data: { userId, groupId: groupId.value } });
        store.dispatch('addSuccessMessage', 'Користувача видалено!');
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

const promptRemoveUser = (userId) => showQuestionModal('Видалити користувача?', `Ви впевнені?`, () => removeUser(userId));

const updateRoles = async (userId, roles) => {
    try {
        await apiClient.put('/group-user/roles', { userId, groupId: groupId.value, roles });
        store.dispatch('addSuccessMessage', 'Ролі оновлено!');
        fetchInitialData();
    } catch (err) { handleApiError(err); }
};

// This is a simplified version. A proper modal for role editing would be better UX.
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
.invite-form { margin-top: 1rem; .email-input { flex-grow: 2; } }
.users-list { display: flex; flex-direction: column; gap: 10px; }
.user-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; background-color: var(--main-color); border-radius: 3px; flex-wrap: wrap; }
.user-actions { display: flex; gap: 10px; button { padding: 5px 10px; border: none; color: white; border-radius: 3px; cursor: pointer; &:hover { opacity: 0.8; } } .btn-edit { background-color: #ffc107; } .btn-delete { background-color: #f44336; } }
.error { color: #f44336; }
</style>
