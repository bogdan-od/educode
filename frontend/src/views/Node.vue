<template>
    <div v-if="loading"><Preloader/></div>
    <div v-if="!loading && node" class="node-page">
        <!-- BREADCRUMBS -->
        <BreadcrumbNavigation :homeRoute="{ name: 'Оглядач вузлів', to: '/nodes' }" :crumbs="breadcrumbs"/>

        <div class="header">
            <h1>(Вузол) {{ node.title }}</h1>
            <div class="actions">
                <LinkBtn v-if="canEdit" :to="`/edit/node/${node.id}`" anim="bg-scale" img="wave1.svg">
                    <i class="fa-solid fa-pen"></i> Редагувати
                </LinkBtn>
                 <LinkBtn v-if="canManageUsers" :to="`/tree-node/${node.treeNodeId}/members`" anim="bg-scale">
                    <i class="fa-solid fa-user-gear"></i> Користувачі
                </LinkBtn>
                 <LinkBtn v-if="canInvite" :to="`/tree-node/${node.treeNodeId}/invitations`" anim="bg-scale">
                    <i class="fa-solid fa-user-plus"></i> Запрошення
                </LinkBtn>
                 <LinkBtn :to="{ name: 'NodeBrowser', params: { id: node.id } }" anim="go">
                    <i class="fa-solid fa-folder-open"></i> Перейти до вузла
                </LinkBtn>
            </div>
        </div>
        <p class="description">{{ node.description }}</p>
        <hr>
        
        <!-- LAZY LOADED CONTENT TABS -->
        <SegmentedControl :options="contentTabs" v-model="activeView" />

        <!-- PUZZLES SECTION -->
        <div v-if="activeView === 'puzzles'" class="content-section">
             <div class="section-header">
                <h2>Задачі вузла</h2>
                <ApiSearchSelector
                    v-if="canAssignPuzzle"
                    v-model="puzzleToAssign"
                    :apiUrl="`/puzzles/autocomplete?treeNodeId=${treeNodeId}`"
                    placeholder="Додати задачу до вузла..."
                    class="content-search"
                    @update:modelValue="assignPuzzle"
                />
            </div>
            <div v-if="contentLoading"><Preloader /></div>
            <div v-else-if="puzzles.length > 0" class="puzzles-list">
                <div v-for="puzzle in puzzles" :key="puzzle.id" class="content-item puzzle-item">
                    <span class="item-title">{{ puzzle.title }}</span>
                    <div class="item-actions">
                        <LinkBtn :to="`/puzzle/${puzzle.id}?treeNodeId=${node.treeNodeId}`" size="sm" anim="go"><i class="fa-solid fa-eye"></i></LinkBtn>
                        <LinkBtn v-if="canDeletePuzzle" @click="promptRemovePuzzle(puzzle)" size="sm" color="danger"><i class="fa-solid fa-trash"></i></LinkBtn>
                    </div>
                </div>
                <button v-if="puzzleHasNext" @click="loadMorePuzzles" class="load-more-btn">
                    Завантажити ще
                </button>
            </div>
            <p v-else class="empty-message">У цьому вузлі ще немає задач.</p>
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

const node = ref(null);
const loading = ref(true);
const contentLoading = ref(false);
const puzzleToAssign = ref(null);
const breadcrumbs = ref([]);

// --- REFACTORED FOR LAZY LOADING ---
const activeView = ref(null); // 'puzzles' or null
const contentTabs = [
    { label: 'Задачі', value: 'puzzles', icon: 'fa-solid fa-puzzle-piece' }
];

// Puzzles state
const puzzles = ref([]);
const puzzlePage = ref(1);
const puzzleHasNext = ref(false);
// --- END REFACTOR ---


const canEdit = ref(false), canManageUsers = ref(false), canInvite = ref(false);
const canAssignPuzzle = ref(false), canDeletePuzzle = ref(false);

const nodeId = computed(() => route.params.id);
const treeNodeId = computed(() => node.value?.treeNodeId);

const checkPermissions = async () => {
    if (!treeNodeId.value) return;
    canEdit.value = await hasPermission(treeNodeId.value, 'EDIT_NODES');
    canManageUsers.value = await hasPermission(treeNodeId.value, 'VIEW_NODE_MEMBERS');
    canInvite.value = await hasPermission(treeNodeId.value, 'INVITE_USERS');
    canAssignPuzzle.value = await hasPermission(treeNodeId.value, 'ASSIGN_PUZZLE_TO_NODE');
    canDeletePuzzle.value = await hasPermission(treeNodeId.value, 'DELETE_PUZZLE_FROM_NODE');
};

const fetchNodeAndPath = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/node/${nodeId.value}`);
        node.value = response.data;
        
        // Fetch path for breadcrumbs
        const pathRes = await apiClient.get(`/tree-node/${node.value.treeNodeId}/path`);
        // API returns { nodeId, treeNodeId, title }
        breadcrumbs.value = pathRes.data.map(crumb => ({
            name: crumb.title,
            // Only add 'to' if it's not the current page
            to: crumb.nodeId !== node.value.id ? `/node/${crumb.nodeId}` : null
        }));

        await checkPermissions();
    } catch (error) {
        handleApiError(error);
        router.push({ name: 'NodeBrowser' });
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
                includeInherited: false, // Only direct puzzles
                page: page,
                limit: 10
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
// --- END PAGINATION ---

const assignPuzzle = async (puzzleId) => {
    if (!puzzleId) return;
    try {
        await apiClient.post(`/tree-node/${treeNodeId.value}/puzzles/${puzzleId}/assign`);
        store.dispatch('addSuccessMessage', 'Задачу успішно додано до вузла!');
        fetchPuzzles(1);
    } catch (error) {
        handleApiError(error);
    } finally {
        puzzleToAssign.value = null;
    }
};

const removePuzzle = async (puzzle) => {
     try {
        await apiClient.delete(`/tree-node/${treeNodeId.value}/puzzles/${puzzle.id}`);
        store.dispatch('addSuccessMessage', 'Задачу видалено з вузла!');
        fetchPuzzles(1);
    } catch (error) {
        handleApiError(error);
    }
};

const promptRemovePuzzle = (puzzle) => {
    showQuestionModal(
        'Видалити задачу?',
        `Ви впевнені, що хочете видалити задачу "${puzzle.title}" з цього вузла?`,
        () => removePuzzle(puzzle)
    );
};

// --- LAZY LOADING WATCHER ---
watch(activeView, (newView) => {
    if (newView === 'puzzles' && puzzles.value.length === 0) {
        fetchPuzzles(1);
    }
});
// --- END WATCHER ---

onMounted(async () => {
    clearPermissionCache(treeNodeId.value);
    await fetchNodeAndPath();
    // Puzzles are now loaded on demand
});

watch(nodeId, async (newId) => {
    if (newId) {
        clearPermissionCache(treeNodeId.value); // Очищаем старый кэш
        await fetchNodeAndPath();
        // Reset state
        activeView.value = null;
        puzzles.value = [];
        puzzlePage.value = 1;
    }
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.node-page { 
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

.puzzles-list { 
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
</style>
