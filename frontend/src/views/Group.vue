<template>
    <div v-if="loading"><Preloader/></div>
    <div v-if="!loading && group" class="group-page">
        <!-- BREADCRUMBS -->
        <BreadcrumbNavigation :homeRoute="{ name: 'Мої вузли', to: '/my-nodes-groups' }" :crumbs="[{ name: group.title }]"/>

        <div class="header">
            <h1>(Група) {{ group.title }}</h1>
            <div class="actions">
                 <LinkBtn v-if="canCreateHomework" :to="`/create/homework?groupId=${group.id}`" anim="bg-scale" img="wave2.svg">
                    <i class="fa-solid fa-plus"></i> Створити ДЗ
                </LinkBtn>
                <LinkBtn v-if="canEdit" :to="`/edit/group/${group.id}`" anim="bg-scale" img="wave1.svg">
                    <i class="fa-solid fa-pen"></i> Редагувати
                </LinkBtn>
                 <LinkBtn v-if="canManageUsers" :to="`/tree-node/${group.treeNodeId}/members`" anim="bg-scale">
                    <i class="fa-solid fa-user-gear"></i> Користувачі
                </LinkBtn>
                 <LinkBtn v-if="canInvite" :to="`/tree-node/${group.treeNodeId}/invitations`" anim="bg-scale">
                    <i class="fa-solid fa-user-plus"></i> Запрошення
                </LinkBtn>
            </div>
        </div>
        <p class="description">{{ group.description }}</p>
        <hr>

        <!-- LAZY LOADED CONTENT TABS -->
        <SegmentedControl :options="contentTabs" v-model="activeView" />

        <!-- HOMEWORK SECTION -->
        <div v-if="activeView === 'homeworks'" class="content-section">
             <div class="section-header">
                <h2>Домашні завдання</h2>
                <div class="header-controls">
                    <label class="checkbox-label">
                        <input type="checkbox" v-model="homeworkIncludeExpired" @change="reloadHomeworks"/>
                        Показати прострочені
                    </label>
                </div>
            </div>
            <div v-if="contentLoading"><Preloader /></div>
            <div v-else-if="homeworks.length > 0" class="homeworks-list">
                <div v-for="hw in homeworks" :key="hw.id" class="content-item hw-item">
                    <div class="item-info">
                        <span :class="['hw-status-icon', hw.isExpired ? 'expired' : (hw.hasSubmitted ? 'submitted' : 'active')]">
                            <i :class="hw.isExpired ? 'fa-solid fa-calendar-times' : (hw.hasSubmitted ? 'fa-solid fa-check-double' : 'fa-solid fa-clock')"></i>
                        </span>
                        <div class="item-text">
                            <span class="item-title">{{ hw.title }}</span>
                            <span v-if="hw.deadline" class="item-details">
                                До: {{ new Date(hw.deadline).toLocaleString() }}
                            </span>
                        </div>
                    </div>
                    <div class="item-actions">
                        <LinkBtn :to="`/homework/${hw.id}`" size="sm" anim="go"><i class="fa-solid fa-eye"></i></LinkBtn>
                    </div>
                </div>
                <button v-if="homeworkHasNext" @click="loadMoreHomeworks" class="load-more-btn">
                    Завантажити ще
                </button>
            </div>
            <p v-else class="empty-message">Домашніх завдань немає.</p>
        </div>
        
        <!-- PUZZLES SECTION -->
        <div v-if="activeView === 'puzzles'" class="content-section">
             <div class="section-header">
                <h2>Задачі групи</h2>
                <ApiSearchSelector
                    v-if="canAssignPuzzle"
                    v-model="puzzleToAssign"
                    :apiUrl="`/puzzles/autocomplete?treeNodeId=${treeNodeId}`"
                    placeholder="Додати задачу до групи..."
                    class="content-search"
                    @update:modelValue="assignPuzzle"
                />
            </div>
            <div v-if="contentLoading"><Preloader /></div>
            <div v-else-if="puzzles.length > 0" class="puzzles-list">
                <div v-for="puzzle in puzzles" :key="puzzle.id" class="content-item puzzle-item">
                    <span class="item-title">{{ puzzle.title }}</span>
                    <div class="item-actions">
                        <LinkBtn :to="`/puzzle/${puzzle.id}?treeNodeId=${group.treeNodeId}`" size="sm" anim="go"><i class="fa-solid fa-eye"></i></LinkBtn>
                        <LinkBtn v-if="canDeletePuzzle" @click="promptRemovePuzzle(puzzle)" size="sm" color="danger"><i class="fa-solid fa-trash"></i></LinkBtn>
                    </div>
                </div>
                 <button v-if="puzzleHasNext" @click="loadMorePuzzles" class="load-more-btn">
                    Завантажити ще
                </button>
            </div>
            <p v-else class="empty-message">У цій групі ще немає задач.</p>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import BreadcrumbNavigation from '@/components/BreadcrumbNavigation.vue'; // IMPORT BREADCRUMBS
import SegmentedControl from '@/components/SegmentedControl.vue'; // IMPORT SEGMENTED CONTROL
import { handleApiError } from '@/services/errorHandler.js';
import { showQuestionModal } from '@/services/modalService.js';
import { hasPermission, clearPermissionCache } from '@/services/permissionService.js';
import { useStore } from 'vuex';

const route = useRoute();
const router = useRouter();
const store = useStore();

const group = ref(null);
const loading = ref(true);
const contentLoading = ref(false); // Separate loader for content
const puzzleToAssign = ref(null);

// --- REFACTORED FOR LAZY LOADING ---
const activeView = ref(null); // 'puzzles', 'homeworks', or null
const contentTabs = [
    { label: 'Домашні завдання', value: 'homeworks', icon: 'fa-solid fa-house-laptop' },
    { label: 'Задачі', value: 'puzzles', icon: 'fa-solid fa-puzzle-piece' }
];

// Puzzles state
const puzzles = ref([]);
const puzzlePage = ref(1);
const puzzleHasNext = ref(false);

// Homeworks state
const homeworks = ref([]);
const homeworkPage = ref(1);
const homeworkHasNext = ref(false);
const homeworkIncludeExpired = ref(false);
// --- END REFACTOR ---

const canEdit = ref(false), canManageUsers = ref(false), canInvite = ref(false);
const canAssignPuzzle = ref(false), canDeletePuzzle = ref(false), canCreateHomework = ref(false);

const groupId = computed(() => route.params.id);
const treeNodeId = computed(() => group.value?.treeNodeId);

const checkPermissions = async () => {
    if (!treeNodeId.value) return;
    canEdit.value = await hasPermission(treeNodeId.value, 'EDIT_GROUPS');
    canManageUsers.value = await hasPermission(treeNodeId.value, 'VIEW_NODE_MEMBERS');
    canInvite.value = await hasPermission(treeNodeId.value, 'INVITE_USERS');
    canAssignPuzzle.value = await hasPermission(treeNodeId.value, 'ASSIGN_PUZZLE_TO_GROUP');
    canDeletePuzzle.value = await hasPermission(treeNodeId.value, 'DELETE_PUZZLE_FROM_GROUP');
    canCreateHomework.value = await hasPermission(treeNodeId.value, 'CREATE_HOMEWORKS');
};

const fetchGroup = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/group/${groupId.value}`);
        group.value = response.data;
        await checkPermissions();
    } catch (error) {
        handleApiError(error);
        router.push({ name: 'NodeBrowser' }); // Assuming this is the fallback
    } finally {
        loading.value = false;
    }
};

// --- UPDATED FOR PAGINATION ---
const fetchPuzzles = async (page = 1) => {
    if (!treeNodeId.value) return;
    contentLoading.value = true;
    try {
        const response = await apiClient.get(`/tree-node/${treeNodeId.value}/puzzles`, {
            params: { 
                includeInherited: false, // Only direct puzzles for the group
                page: page,
                limit: 10 // Page size
            }
        });
        if (page === 1) {
            puzzles.value = response.data.puzzles;
        } else {
            puzzles.value.push(...response.data.puzzles);
        }
        puzzleHasNext.value = response.data.hasNext;
        puzzlePage.value = page;
    } catch (error) {
        handleApiError(error);
    } finally {
        contentLoading.value = false;
    }
};

const loadMorePuzzles = () => {
    if (puzzleHasNext.value) {
        fetchPuzzles(puzzlePage.value + 1);
    }
};
// --- END PUZZLE PAGINATION ---

// --- NEW HOMEWORK FUNCTIONS ---
const fetchHomeworks = async (page = 1) => {
    if (!groupId.value) return;
    contentLoading.value = true;
    try {
        const response = await apiClient.get(`/homework/group/${groupId.value}`, {
            params: {
                page: page,
                limit: 10,
                includeExpired: homeworkIncludeExpired.value
            }
        });
        if (page === 1) {
            homeworks.value = response.data.homeworks.content;
        } else {
            homeworks.value.push(...response.data.homeworks.content);
        }
        homeworkHasNext.value = response.data.hasNext;
        homeworkPage.value = page;
    } catch (error) {
        handleApiError(error);
    } finally {
        contentLoading.value = false;
    }
};

const loadMoreHomeworks = () => {
    if (homeworkHasNext.value) {
        fetchHomeworks(homeworkPage.value + 1);
    }
};

const reloadHomeworks = () => {
    homeworkPage.value = 1;
    fetchHomeworks(1);
};
// --- END HOMEWORK FUNCTIONS ---


const assignPuzzle = async (puzzleId) => {
    if (!puzzleId) return;
    try {
        await apiClient.post(`/tree-node/${treeNodeId.value}/puzzles/${puzzleId}/assign`);
        store.dispatch('addSuccessMessage', 'Задачу успішно додано до групи!');
        fetchPuzzles(1); // Reload puzzles
    } catch (error) {
        handleApiError(error);
    } finally {
        puzzleToAssign.value = null; // Reset selector
    }
};

const removePuzzle = async (puzzle) => {
     try {
        await apiClient.delete(`/tree-node/${treeNodeId.value}/puzzles/${puzzle.id}`);
        store.dispatch('addSuccessMessage', 'Задачу видалено з групи!');
        fetchPuzzles(1); // Reload puzzles
    } catch (error) {
        handleApiError(error);
    }
};

const promptRemovePuzzle = (puzzle) => {
    showQuestionModal(
        'Видалити задачу?',
        `Ви впевнені, що хочете видалити задачу "${puzzle.title}" з цієї групи?`,
        () => removePuzzle(puzzle)
    );
};

// --- LAZY LOADING WATCHER ---
watch(activeView, (newView) => {
    if (newView === 'puzzles' && puzzles.value.length === 0) {
        fetchPuzzles(1);
    } else if (newView === 'homeworks' && homeworks.value.length === 0) {
        fetchHomeworks(1);
    }
});
// --- END WATCHER ---

onMounted(async () => {
    clearPermissionCache(treeNodeId.value);
    await fetchGroup();
    // Puzzles and Homeworks are now loaded on demand by the watcher
});

watch(groupId, async (newId) => {
    if (newId) {
        clearPermissionCache(treeNodeId.value);
        await fetchGroup();
        // Reset state on navigation
        activeView.value = null;
        puzzles.value = [];
        homeworks.value = [];
        puzzlePage.value = 1;
        homeworkPage.value = 1;
    }
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.group-page { 
    max-width: 900px; 
    margin: 0 auto; 
}

.header { 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    flex-wrap: wrap; 
    gap: 1rem; 
    
    h1 {
        font-family: $secondary-font;
        color: var(--text-color);
    }
}

.actions { 
    display: flex; 
    flex-wrap: wrap; 
    gap: 1rem; 
}

.description { 
    color: var(--secondary-color); 
    margin-top: 0.5rem; 
    font-family: $form-font;
}

.content-section { 
    margin-top: 1.5rem; 
}

.section-header { 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    margin-bottom: 1rem; 
    flex-wrap: wrap; 
    gap: 1rem; 

    h2 {
        font-family: $secondary-font;
        color: var(--text-color);
    }
}

.content-search { 
    min-width: 300px; 
    max-width: 400px;
    flex-grow: 1;
}

.puzzles-list, .homeworks-list { 
    display: flex; 
    flex-direction: column; 
    gap: 0.75rem; 
}

.content-item { 
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    background: var(--weak-color); 
    padding: 0.75rem 1rem; 
    border-radius: 5px;
    font-family: $form-font;
    color: var(--text-color);

    .item-title {
        font-weight: 600;
    }
    
    .item-actions { 
        display: flex; 
        gap: 0.5rem; 
    }
}

.hw-item {
    .item-info {
        display: flex;
        align-items: center;
        gap: 1rem;
    }
    .item-text {
        display: flex;
        flex-direction: column;
    }
    .item-details {
        font-size: 0.85em;
        color: var(--secondary-color);
    }
    .hw-status-icon {
        font-size: 1.2em;
        &.active { color: var(--info-color); }
        &.submitted { color: var(--success-color); }
        &.expired { color: var(--secondary-color); }
    }
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

    &:hover {
        background-color: var(--weak-color);
    }
}

.empty-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 20px;
    text-align: center;
    background-color: var(--weak-color);
    border-radius: 3px;
}

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-family: $form-font;
    font-size: 0.9em;
    color: var(--secondary-color);
    cursor: pointer;
    
    input {
        width: 16px;
        height: 16px;
    }
}

</style>
