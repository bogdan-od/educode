<template>
    <!-- BREADCRUMBS -->
    <BreadcrumbNavigation :homeRoute="{ name: 'Оглядач вузлів', to: '/nodes' }" :crumbs="breadcrumbs"/>

    <h1>Керування запрошеннями: {{ title }}</h1>
    <hr>
    
    <!-- Секция создания приглашения -->
    <div class="management-section">
        <h2>Створити запрошення</h2>
        
        <SegmentedControl :options="inviteTypeOptions" v-model="form.invitationType" />

        <form @submit.prevent="createInvitation" class="invite-form">
            <!-- Общие поля -->
            <div class="form-grid">
                <MultiSelect 
                    v-model="form.roles" 
                    :options="availableRoles" 
                    labelKey="description"
                    valueKey="name" 
                    label="Ролі для запрошених" 
                    placeholder="Виберіть ролі" 
                    :invalid="!!validationErrors.roles"
                    :errorMessage="validationErrors.roles"
                />
                
                <div class="form-group">
                    <label for="expires-at">Дійсне до (необов'язково)</label>
                    <input 
                        type="datetime-local" 
                        v-model="form.expiresAt" 
                        id="expires-at"
                        class="form-input"
                    >
                </div>
            </div>

            <!-- Контекстные поля -->
            <div class="context-fields">
                <ApiSearchSelector 
                    v-if="form.invitationType === 'LIMITED_LIST' || form.invitationType === 'SINGLE_USE'"
                    v-model="selectedUsers" 
                    apiUrl="/user/autocomplete" 
                    :label="form.invitationType === 'SINGLE_USE' ? 'Дозволений користувач (тільки 1)' : 'Дозволені користувачі'"
                    placeholder="Пошук за логіном..."
                    :multiple="form.invitationType === 'LIMITED_LIST'"
                    :invalid="!!validationErrors.userIds"
                    :errorMessage="validationErrors.userIds"
                />
                
                <EntitySelector
                    v-if="form.invitationType === 'NODE_BASED'"
                    v-model="form.allowedTreeNodeId"
                    entityType="tree-node"
                    label="Дозволити тільки для учасників вузла"
                    :invalid="!!validationErrors.allowedTreeNodeId"
                    :errorMessage="validationErrors.allowedTreeNodeId"
                />
                
                <div v-if="form.invitationType === 'PUBLIC'" class="info-box">
                    <i class="fa-solid fa-globe"></i>
                    <p>Публічне запрошення буде доступне будь-кому, хто має посилання.</p>
                </div>
            </div>
            
            <div class="form-group checkbox-group">
                <input type="checkbox" v-model="form.canLeave" id="can-leave-invite" />
                <label for="can-leave-invite">Дозволити користувачам виходити з вузла (після приєднання)</label>
            </div>

            <LinkBtn 
                role="button" 
                type="submit" 
                anim="go" 
                :disabled="submitting || form.roles.length === 0"
                class="submit-btn"
            >
                <Preloader v-if="submitting" :scale="0.5" color="white" />
                <span v-else>Створити</span>
            </LinkBtn>
        </form>
    </div>

    <hr>
    
    <!-- Секция активных приглашений -->
    <div class="management-section">
        <h2>Активні запрошення ({{ totalInvites }})</h2>
        <div v-if="isLoading"><Preloader/></div>
        <div v-else-if="error" class="error-message">{{ error }}</div>
        <div v-else-if="invitations.length > 0" class="invites-list">
            <div v-for="invite in invitations" :key="invite.id" class="invite-item">
                <div class="invite-info">
                    <p class="invite-type">
                        <i :class="getIconForType(invite.invitationType)"></i>
                        {{ getLabelForType(invite.invitationType) }}
                    </p>
                    <div class="invite-code">
                        <span>{{ invite.code }}</span>
                        <button @click="copyCode(invite.code)" class="copy-btn" title="Копіювати посилання">
                            <i class="fa-regular fa-copy"></i>
                        </button>
                    </div>
                    <p class="invite-roles">
                        <strong>Ролі:</strong> {{ invite.roles.map(role => role.description).join(', ') }}
                    </p>
                    <p v-if="invite.expiresAt" class="invite-expiry">
                        <strong>Дійсне до:</strong> {{ new Date(invite.expiresAt).toLocaleString() }}
                    </p>
                    <p v-if="invite.invitationType === 'NODE_BASED'" class="invite-details">
                        <strong>Для вузла:</strong> ID {{ invite.allowedTreeNodeId }}
                    </p>
                    <p v-if="invite.invitationType === 'LIMITED_LIST' || invite.invitationType === 'SINGLE_USE'" class="invite-details">
                        <strong>Для {{ invite.userCount }} користувачів</strong>
                    </p>
                    <p class="invite-details">
                        <strong>Можна вийти:</strong> {{ invite.canLeave ? 'Так' : 'Ні' }}
                    </p>
                </div>
                <div class="invite-actions">
                    <LinkBtn @click="deactivateInvite(invite)" color="warning" size="sm">
                        <i class="fa-solid fa-power-off"></i>
                        Деактивувати
                    </LinkBtn>
                    <LinkBtn @click="deleteInvite(invite)" color="danger" size="sm">
                        <i class="fa-solid fa-trash"></i>
                        Видалити
                    </LinkBtn>
                </div>
            </div>
            <!-- PAGINATION -->
            <button v-if="hasNextPage" @click="loadMoreInvites" class="load-more-btn">
                Завантажити ще
            </button>
        </div>
        <p v-else class="empty-message">Активних запрошень немає.</p>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import EntitySelector from '@/components/EntitySelector.vue';
import MultiSelect from '@/components/MultiSelect.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import SegmentedControl from '@/components/SegmentedControl.vue';
import BreadcrumbNavigation from '@/components/BreadcrumbNavigation.vue'; // IMPORT BREADCRUMBS
import { handleApiError } from '@/services/errorHandler.js';
import { showQuestionModal } from '@/services/modalService.js';
import { hasPermission } from '@/services/permissionService.js';
import { getRoles } from '@/services/resourceService.js';

const route = useRoute();
const router = useRouter();
const store = useStore();
const treeNodeId = computed(() => route.params.id);

const invitations = ref([]);
const title = ref('');
const entityType = ref('node');
const isLoading = ref(true);
const submitting = ref(false);
const error = ref('');
const availableRoles = ref([]);
const breadcrumbs = ref([]);

// --- PAGINATION STATE ---
const currentPage = ref(1);
const hasNextPage = ref(false);
const totalInvites = ref(0);
// --- END PAGINATION ---

const inviteTypeOptions = [
    { label: 'Публічне', value: 'PUBLIC', icon: 'fa-solid fa-globe' },
    { label: 'За списком', value: 'LIMITED_LIST', icon: 'fa-solid fa-list-check' },
    { label: 'Одноразове', value: 'SINGLE_USE', icon: 'fa-solid fa-user' },
    { label: 'Для вузла', value: 'NODE_BASED', icon: 'fa-solid fa-folder-tree' }
];

const getLabelForType = (type) => inviteTypeOptions.find(opt => opt.value === type)?.label || type;
const getIconForType = (type) => inviteTypeOptions.find(opt => opt.value === type)?.icon || 'fa-solid fa-link';

const form = reactive({
    roles: [],
    expiresAt: '',
    invitationType: 'PUBLIC',
    allowedTreeNodeId: null,
    userIds: [],
    canLeave: true // New field from DTO
});

watch(() => form.invitationType, () => {
    form.allowedTreeNodeId = null;
    form.userIds = [];
    selectedUsers.value = [];
});

const validationErrors = reactive({
    roles: '',
    userIds: '',
    allowedTreeNodeId: ''
});

const selectedUsers = ref([]); 

watch(selectedUsers, (newVal) => {
    if (Array.isArray(newVal)) {
        form.userIds = newVal;
    } else {
        form.userIds = newVal ? [newVal] : [];
    }
});

const fetchTitleAndPath = async () => {
    try {
        let entityTitle = '';
        try {
            const nodeRes = await apiClient.get(`/node/by-tree-node/${treeNodeId.value}`);
            entityTitle = nodeRes.data.title;
            entityType.value = 'node';
        } catch (e) {
            const groupRes = await apiClient.get(`/group/by-tree-node/${treeNodeId.value}`);
            entityTitle = groupRes.data.title;
            entityType.value = 'group';
        }
        title.value = entityTitle;
        
        const pathRes = await apiClient.get(`/tree-node/${treeNodeId.value}/path`);
        breadcrumbs.value = pathRes.data.map(crumb => ({
            name: crumb.title,
            to: crumb.treeNodeId === treeNodeId.value ? null : (entityType.value === 'node' ? `/node/${crumb.nodeId}` : `/group/${crumb.nodeId}`)
        }));
        breadcrumbs.value.push({ name: 'Запрошення' });

    } catch (e) {
        handleApiError(e);
        title.value = `ID ${treeNodeId.value}`;
        breadcrumbs.value = [{ name: 'Запрошення' }];
    }
};

// --- UPDATED FOR PAGINATION ---
const fetchInvites = async (page = 1) => {
    isLoading.value = true;
    error.value = '';
    
    if (!await hasPermission(treeNodeId.value, 'INVITE_USERS')) {
         error.value = "У вас немає прав для керування запрошеннями.";
         isLoading.value = false;
         router.push(`/tree-node/${treeNodeId.value}`); // This route does not exist, redirect to group/node
         return;
    }

    try {
        const invitesRes = await apiClient.get(`/tree-node/${treeNodeId.value}/invitations`, {
            params: {
                page: page,
                limit: 10
            }
        });
        
        if (page === 1) {
            invitations.value = invitesRes.data.invitations;
        } else {
            invitations.value.push(...invitesRes.data.invitations);
        }
        hasNextPage.value = invitesRes.data.hasNext;
        totalInvites.value = invitesRes.data.totalElements; // Assuming backend provides this
        currentPage.value = page;

    } catch (err) {
        handleApiError(err);
        error.value = "Не вдалося завантажити дані.";
    } finally {
        isLoading.value = false;
    }
};

const loadMoreInvites = () => {
    if (hasNextPage.value) {
        fetchInvites(currentPage.value + 1);
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
// --- END PAGINATION ---

const getCorrectDatetime = (datetimeValue) => {
    const date = new Date(datetimeValue);
    const offsetMinutes = date.getTimezoneOffset();
    date.setMinutes(date.getMinutes() - offsetMinutes);
    return date.toISOString().slice(0, 16);
}

const createInvitation = async () => {
    submitting.value = true;
    Object.keys(validationErrors).forEach(key => validationErrors[key] = '');

    const payload = {
        roles: form.roles,
        expiresAt: getCorrectDatetime(form.expiresAt),
        invitationType: form.invitationType,
        allowedTreeNodeId: null,
        userIds: [],
        canLeave: form.canLeave // Send new field
    };

    switch (form.invitationType) {
        case 'NODE_BASED':
            payload.allowedTreeNodeId = form.allowedTreeNodeId;
            break;
        case 'SINGLE_USE':
        case 'LIMITED_LIST':
            payload.userIds = form.userIds;
            break;
    }

    try {
        await apiClient.post(`/tree-node/${treeNodeId.value}/invitations`, payload);
        store.dispatch('addSuccessMessage', 'Запрошення створено!');
        fetchInvites(1); // Reload list
        Object.assign(form, { roles: [], expiresAt: '', invitationType: 'PUBLIC', allowedTreeNodeId: null, userIds: [], canLeave: true });
        selectedUsers.value = [];
    } catch (err) {
        handleApiError(err, validationErrors);
    } finally {
        submitting.value = false;
    }
};

const deactivateInvite = async (invite) => {
    try {
        await apiClient.put(`/tree-node/${treeNodeId.value}/invitations/${invite.id}`, { active: false });
        store.dispatch('addSuccessMessage', 'Запрошення деактивовано.');
        fetchInvites(1); // Reload list
    } catch (err) { handleApiError(err); }
};

const deleteInvite = async (invite) => {
    showQuestionModal('Видалити запрошення?', `Код ${invite.code} буде видалено назавжди.`, async () => {
        try {
            await apiClient.delete(`/tree-node/${treeNodeId.value}/invitations/${invite.id}`);
            store.dispatch('addSuccessMessage', 'Запрошення видалено.');
            fetchInvites(1); // Reload list
        } catch (err) { handleApiError(err); }
    });
};

const copyCode = (code) => {
    // Copy the full join URL
    navigator.clipboard.writeText(`${window.location.origin}/join/${code}`).then(() => {
        store.dispatch('addSuccessMessage', 'Посилання-запрошення скопійовано!');
    }).catch(err => {
        console.error('Failed to copy code: ', err);
        store.dispatch('addErrorMessage', 'Не вдалося скопіювати посилання.');
    });
};

onMounted(async () => {
    isLoading.value = true;
    await fetchTitleAndPath();
    await fetchRoles();
    await fetchInvites(1);
    isLoading.value = false;
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.management-section { 
    background-color: var(--weak-color); 
    padding: 20px; 
    border-radius: 5px; 
    margin-bottom: 20px;
    border: 1px solid var(--secondary-color);
}

h1, h2 {
    font-family: $secondary-font;
    color: var(--text-color);
}

// --- Стили формы ---
.invite-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    margin-top: 1.5rem;
}

.form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    
    label {
        font-family: $form-font;
        font-weight: 600;
        font-size: 0.9em;
    }
    
    .form-input {
        height: 50px;
        padding: 0 1rem;
        border: 1px solid var(--secondary-color);
        background-color: var(--main-color);
        border-radius: 3px;
        font-family: $form-font;
        font-size: 1em;
        color: var(--text-color);
        transition: border-color 0.2s ease;

        &:focus {
            border-color: var(--info-color);
            outline: none;
        }
    }
}

.checkbox-group {
    flex-direction: row;
    align-items: center;
    height: auto;
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

.context-fields {
    padding: 1rem;
    background-color: var(--main-color);
    border-radius: 3px;
}

.info-box {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--secondary-color);
    i { font-size: 1.2em; }
    p { margin: 0; font-family: $form-font; }
}

.submit-btn {
    align-self: flex-start;
    min-width: 200px;
}

// --- Стили списка ---
.invites-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    gap: 1rem;
}

.invite-item {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    background-color: var(--main-color);
    border-radius: 5px;
    border: 1px solid var(--secondary-color);
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
        border-color: var(--info-color);
    }
}

.invite-info {
    padding: 1rem;
    p { 
        margin: 0 0 0.5rem; 
        font-family: $form-font;
        color: var(--text-color);
        font-size: 0.95em;
    }
}

.invite-type {
    font-family: $secondary-font;
    font-size: 1.1em !important;
    font-weight: 600;
    margin-bottom: 1rem !important;
    i { margin-right: 8px; color: var(--info-color); }
}

.invite-code {
    display: flex;
    align-items: center;
    gap: 10px;
    background-color: var(--weak-color);
    padding: 8px 12px;
    border-radius: 3px;
    margin-bottom: 1rem;
    
    span {
        font-family: 'Courier New', Courier, monospace;
        font-weight: bold;
        color: var(--text-color);
        flex-grow: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

.copy-btn {
    background: none;
    border: none;
    color: var(--secondary-color);
    cursor: pointer;
    font-size: 1.1em;
    padding: 5px;
    border-radius: 3px;
    
    &:hover {
        color: var(--text-color);
        background-color: var(--main-color);
    }
}

.invite-roles, .invite-expiry, .invite-details {
    color: var(--secondary-color) !important;
    font-size: 0.9em !important;
    strong { color: var(--text-color); }
}

.invite-actions {
    display: flex;
    gap: 0.5rem;
    padding: 1rem;
    background-color: var(--weak-color);
    border-top: 1px solid var(--secondary-color);
    border-bottom-left-radius: 5px;
    border-bottom-right-radius: 5px;

    i {
        font-size: 0.9em;
    }
}

.error-message, .empty-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 20px;
    text-align: center;
    background-color: var(--weak-color);
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
    width: 100%;

    &:hover {
        background-color: var(--weak-color-hover);
    }
}

</style>
