<template>
    <h1>Мої вузли та групи</h1>
    <p>Тут показані всі вузли та групи, в яких ви є учасником.</p>
    <hr>
    <div v-if="loading && currentPage === 1"><Preloader /></div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="items.length === 0" class="empty-message">Ви не є учасником жодного вузла чи групи.</div>
    <div v-else class="items-list">
        <div v-for="item in items" :key="item.type + item.treeNodeId" class="item-card">
             <div class="item-icon">
                <i :class="item.type === 'node' ? 'fa-solid fa-folder' : 'fa-solid fa-users'"></i>
            </div>
            <div class="item-info">
                <h3>{{ item.title }}</h3>
                <p>{{ item.description }}</p>
                <div class="roles">
                    <!-- РОЛИ ТЕПЕРЬ КЛИКАБЕЛЬНЫ -->
                    <span 
                        v-for="role in item.roles" 
                        :key="role.name" 
                        class="role-badge"
                        @click="showRoleInfo(role.name)"
                        title="Дізнатися більше про роль"
                    >
                        {{ role.description }}
                    </span>
                </div>
            </div>
            <div class="item-actions">
                <LinkBtn :to="`/${item.type}/${item.id}`" anim="go" title="Переглянути">
                    <i class="fa-solid fa-eye"></i>
                </LinkBtn>

                <LinkBtn 
                  v-if="item.canLeave" 
                  @click="promptLeave(item)" 
                  color="danger" 
                  title="Вийти"
                  class="leave-btn"
                >
                    <i class="fa-solid fa-arrow-right-from-bracket"></i>
                </LinkBtn>
            </div>
        </div>
    </div>
    
    <!-- PAGINATION -->
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
import { ref, onMounted, computed } from 'vue';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import RoleInfoModal from '@/components/RoleInfoModal.vue'; // ИМПОРТ МОДАЛКИ
import { handleApiError } from '@/services/errorHandler';
import { showQuestionModal } from '@/services/modalService';

const store = useStore();
const items = ref([]);
const loading = ref(true);
const loadingMore = ref(false);
const error = ref('');
const currentUserId = computed(() => store.getters.getCurrentUserId);

// --- PAGINATION STATE ---
const currentPage = ref(1);
const hasNext = ref(false);
// --- END PAGINATION ---

// --- ROLE MODAL STATE ---
const isRoleModalVisible = ref(false);
const selectedRoleName = ref(null);

const showRoleInfo = (roleName) => {
    selectedRoleName.value = roleName;
    isRoleModalVisible.value = true;
};
// --- END ROLE MODAL ---

const fetchItems = async (page = 1) => {
    if (!currentUserId.value) {
        error.value = "Потрібна авторизація.";
        loading.value = false;
        return;
    }
    
    if (page === 1) {
        loading.value = true;
    } else {
        loadingMore.value = true;
    }
    error.value = '';

    try {
        const response = await apiClient.get(`/tree-node/my-memberships`, {
            params: {
                page: page,
                limit: 10 // Page size
            }
        });
        
        let fetchedItems = [];
        if (response.data && response.data.content) {
            fetchedItems = response.data.content.map(item => ({
                id: item.id,
                treeNodeId: item.treeNodeId,
                title: item.title,
                description: item.description,
                roles: item.roles, // item.roles - это Set<RoleDTO>
                type: item.type.toLowerCase(),
                canLeave: item.canLeave
            }));
        }
        
        if (page === 1) {
            items.value = fetchedItems;
        } else {
            items.value.push(...fetchedItems);
        }
        
        items.value.sort((a,b) => a.title.localeCompare(b.title));
        hasNext.value = response.data.hasNext;
        currentPage.value = page;

    } catch (err) {
        handleApiError(err);
        error.value = 'Не вдалося завантажити список.';
    } finally {
        loading.value = false;
        loadingMore.value = false;
    }
};

const loadMore = () => {
    if (hasNext.value) {
        fetchItems(currentPage.value + 1);
    }
};

const promptLeave = (item) => {
    showQuestionModal(
        'Вийти з вузла?',
        `Ви впевнені, що хочете вийти з "${item.title}"? Цю дію неможливо буде скасувати.`,
        () => leaveNode(item)
    );
};

const leaveNode = async (item) => {
    if (!currentUserId.value) {
        handleApiError({ response: { data: { error: "Не вдалося ідентифікувати користувача." } } });
        return;
    }
    try {
        await apiClient.delete(`/tree-node/${item.treeNodeId}/members/leave`);
        store.dispatch('addSuccessMessage', `Ви успішно вийшли з "${item.title}".`);
        items.value = items.value.filter(i => i.treeNodeId !== item.treeNodeId);
    } catch (err) {
        handleApiError(err);
    }
};

onMounted(() => fetchItems(1));
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

.items-list { 
    display: flex; 
    flex-direction: column; 
    gap: 1rem; 
    margin-top: 1.5rem;
}
.item-card { 
    display: flex; 
    align-items: center; 
    gap: 15px; 
    background-color: var(--weak-color); 
    padding: 15px; 
    border-radius: 5px; 
    border: 1px solid var(--secondary-color);
    transition: border-color 0.2s ease;

    &:hover {
        border-color: var(--info-color);
    }
}
.item-icon { 
    font-size: 1.8em; 
    color: var(--info-color); 
    width: 30px;
    text-align: center;
}
.item-info { 
    flex-grow: 1;
    font-family: $form-font;
    
    h3 { 
        margin: 0 0 5px; 
        font-family: $secondary-font;
        color: var(--text-color);
    }
    p { 
        margin: 0; 
        color: var(--secondary-color); 
        font-size: 0.9em; 
    }
}
.roles { 
    display: flex; 
    flex-wrap: wrap; 
    gap: 0.5rem; 
    margin-top: 0.75rem; 
}
.role-badge { 
    background-color: var(--secondary-color); 
    color: white; 
    padding: 0.2rem 0.6rem; 
    border-radius: 5px; 
    font-size: 0.8em; 
    font-weight: 600;
    cursor: pointer; // ДОБАВЛЕНО
    transition: background-color 0.2s ease; // ДОБАВЛЕНО

    &:hover { // ДОБАВЛЕНО
        background-color: var(--info-color);
    }
}
.item-actions { 
    display: flex; 
    gap: 10px; 
}

.leave-btn {
    i {
        font-size: 0.9em;
    }
}

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

.empty-message, .error-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 20px;
    text-align: center;
    background-color: var(--weak-color);
    border-radius: 3px;
    margin-top: 1.5rem;
}
.error-message {
    color: var(--error-color);
}
</style>
