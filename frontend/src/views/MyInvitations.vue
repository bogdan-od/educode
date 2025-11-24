<template>
    <h1>Мої запрошення</h1>
    <p>Тут відображаються персональні запрошення, надіслані вам безпосередньо.</p>
    <hr>

    <!-- Лоадер первой загрузки -->
    <div v-if="loading && currentPage === 1"><Preloader /></div>
    
    <div v-else-if="error" class="error-message">{{ error }}</div>
    
    <div v-else-if="invitations.length === 0" class="empty-message">
        У вас немає активних персональних запрошень.
        <div class="empty-action">
            <LinkBtn to="/join" size="sm">Ввести код вручну</LinkBtn>
        </div>
    </div>
    
    <div v-else class="invitations-list">
        <div v-for="invite in invitations" :key="invite.id" class="invite-card" :class="{ 'inactive': !invite.active }">
            <div class="invite-icon">
                <i class="fa-solid fa-envelope-open-text"></i>
            </div>
            <div class="invite-content">
                <div class="invite-header">
                    <h3>
                        Запрошення до: 
                        <span class="entity-name">{{ invite.entityTitle || 'Невідомий вузол' }}</span>
                    </h3>
                    <span v-if="!invite.active" class="status-badge inactive">Неактивне</span>
                </div>
                
                <div class="invite-meta">
                    <span class="type-badge">{{ formatInviteType(invite.invitationType) }}</span>
                </div>

                <div class="details-grid">
                    <div class="detail-row">
                        <span class="label">Ролі:</span>
                        <div class="roles-list">
                            <span 
                                v-for="role in invite.roles" 
                                :key="role.name" 
                                class="role-tag"
                                @click="showRoleInfo(role.name)"
                                title="Дізнатися більше про роль"
                            >
                                {{ role.description }}
                            </span>
                        </div>
                    </div>
                    
                    <div class="detail-row">
                        <span class="label">Можливість вийти:</span>
                        <span class="value" :class="invite.canLeaveOnJoin ? 'text-success' : 'text-warning'">
                            <i :class="invite.canLeaveOnJoin ? 'fa-solid fa-check' : 'fa-solid fa-xmark'"></i>
                            {{ invite.canLeaveOnJoin ? 'Так' : 'Ні (Вихід заборонено)' }}
                        </span>
                    </div>

                    <div v-if="invite.expiresAt" class="detail-row">
                        <span class="label">Дійсне до:</span>
                        <span class="value expiry">{{ new Date(invite.expiresAt).toLocaleString() }}</span>
                    </div>
                </div>
            </div>
            
            <div class="invite-actions">
                <LinkBtn :to="`/join/${invite.code}`" anim="go" img="wave1.svg" :disabled="!invite.active">
                    Переглянути
                </LinkBtn>
            </div>
        </div>
    </div>

    <!-- Кнопка пагинации -->
    <div class="pagination" v-if="!loading && (hasNext || loadingMore)">
        <button @click="loadMore" :disabled="!hasNext || loadingMore" class="load-more-btn">
            <Preloader v-if="loadingMore" :scale="0.5" />
            <span v-else>Завантажити ще</span>
        </button>
    </div>

    <!-- ROLE INFO MODAL -->
    <RoleInfoModal v-model:visible="isRoleModalVisible" :roleName="selectedRoleName" />
</template>

<script setup>
import { ref, onMounted } from 'vue';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import RoleInfoModal from '@/components/RoleInfoModal.vue'; // ИМПОРТ МОДАЛКИ
import { handleApiError } from '@/services/errorHandler.js';

const invitations = ref([]);
const loading = ref(true);
const loadingMore = ref(false);
const error = ref('');

// State пагинации
const currentPage = ref(1);
const hasNext = ref(false);

// --- ROLE MODAL STATE ---
const isRoleModalVisible = ref(false);
const selectedRoleName = ref(null);

const showRoleInfo = (roleName) => {
    selectedRoleName.value = roleName;
    isRoleModalVisible.value = true;
};
// --- END ROLE MODAL ---

const fetchMyInvitations = async (page = 1) => {
    if (page === 1) {
        loading.value = true;
    } else {
        loadingMore.value = true;
    }
    error.value = '';

    try {
        const response = await apiClient.get('/tree-node/my-invitations', {
            params: {
                page: page,
                limit: 10
            }
        });
        
        const newItems = response.data.content || [];
        
        if (page === 1) {
            invitations.value = newItems;
        } else {
            invitations.value.push(...newItems);
        }
        
        hasNext.value = response.data.hasNext;
        currentPage.value = page;

    } catch (err) {
        handleApiError(err);
        error.value = 'Не вдалося завантажити запрошення.';
    } finally {
        loading.value = false;
        loadingMore.value = false;
    }
};

const loadMore = () => {
    if (hasNext.value) {
        fetchMyInvitations(currentPage.value + 1);
    }
};

const formatInviteType = (type) => {
    const map = {
        'LIMITED_LIST': 'За списком користувачів',
        'SINGLE_USE': 'Для одного користувача',
        'PUBLIC': 'Публічне',
        'NODE_BASED': 'Для учасників вузла'
    };
    return map[type] || type;
};

onMounted(() => fetchMyInvitations(1));
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

h1 {
    font-family: $secondary-font;
    color: var(--text-color);
}

p {
    font-family: $form-font;
    color: var(--secondary-color);
}

.invitations-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin-top: 1.5rem;
}

.invite-card {
    display: flex;
    align-items: flex-start;
    background-color: var(--weak-color);
    padding: 1.5rem;
    border-radius: 5px;
    border: 1px solid var(--secondary-color);
    gap: 1.5rem;
    transition: border-color 0.2s ease, transform 0.2s ease, opacity 0.2s ease;
    
    &:hover {
        border-color: var(--info-color);
        transform: translateY(-2px);
        box-shadow: var(--weak-box-shadow);
    }
    
    &.inactive {
        opacity: 0.7;
        border-color: transparent;
        &:hover {
            border-color: var(--secondary-color);
            transform: none;
        }
    }
    
    @media (max-width: 600px) {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
    }
}

.invite-icon {
    width: 50px;
    height: 50px;
    background-color: var(--main-color);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border: 1px solid var(--secondary-color);
    margin-top: 5px;
    
    i {
        font-size: 20px;
        color: var(--info-color);
    }
}

.invite-content {
    flex-grow: 1;
    width: 100%;
}

.invite-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 0.5rem;

    h3 {
        margin: 0;
        font-family: $secondary-font;
        font-size: 1.2em;
        
        .entity-name {
            color: var(--info-color);
            font-weight: 600;
        }
    }
}

.status-badge {
    font-size: 0.8em;
    padding: 2px 8px;
    border-radius: 4px;
    font-family: $form-font;
    font-weight: 600;
    
    &.inactive {
        background-color: var(--secondary-color);
        color: white;
    }
}

.invite-meta {
    margin-bottom: 1rem;
}

.type-badge {
    background-color: var(--main-color);
    padding: 2px 8px;
    border-radius: 10px;
    border: 1px solid var(--secondary-color);
    color: var(--secondary-color);
    font-size: 0.85em;
    font-family: $form-font;
}

.details-grid {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    font-family: $form-font;
    font-size: 0.95em;
}

.detail-row {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
    
    .label {
        color: var(--secondary-color);
        font-weight: 600;
    }
    
    .value {
        color: var(--text-color);
    }
    
    .text-success { color: var(--success-color); }
    .text-warning { color: var(--warn-color); }
    .expiry { color: var(--warn-color); }
}

.roles-list {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    
    .role-tag {
        display: inline-block;
        background-color: var(--secondary-color);
        color: white;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 0.85em;
        cursor: pointer; // Кликабельно
        transition: background-color 0.2s ease;

        &:hover {
            background-color: var(--info-color);
        }
    }
}

.invite-actions {
    align-self: center;
    @media (max-width: 600px) {
        width: 100%;
        :deep(a) { width: 100%; justify-content: center; }
    }
}

.empty-message, .error-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 3rem;
    text-align: center;
    background-color: var(--weak-color);
    border-radius: 5px;
    margin-top: 1rem;
    
    .empty-action {
        margin-top: 1rem;
    }
}
.error-message { color: var(--error-color); }

.pagination { 
    margin-top: 1.5rem; 
    display: flex; 
    justify-content: center; 
    align-items: center; 
    gap: 1rem; 
}

.load-more-btn {
    width: 100%;
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
