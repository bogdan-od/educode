<template>
    <div v-if="loading"><Preloader/></div>
    <div v-if="!loading && homework" class="homework-page">
        <!-- BREADCRUMBS -->
        <BreadcrumbNavigation 
            :homeRoute="{ name: `До групи '${homework.groupTitle}'`, to: `/group/${homework.groupId}` }" 
            :crumbs="[{ name: homework.title }]"
        />

        <div class="header">
            <h1>{{ homework.title }}</h1>
            <LinkBtn v-if="canEdit" :to="`/edit/homework/${homework.id}`" anim="go">
                <i class="fa-solid fa-pen"></i> Редагувати
            </LinkBtn>
        </div>
        <p class="description">{{ homework.content }}</p>
        <div class="details">
            <p><strong>Група:</strong> <router-link :to="`/group/${homework.groupId}`">{{ homework.groupTitle }}</router-link></p>
            <p v-if="homework.puzzleTitle">
                <strong>Задача: </strong> 
                <router-link :to="`/puzzle/${homework.puzzleId}?homeworkId=${homework.id}`">{{ homework.puzzleTitle }}</router-link>
            </p>
            <p v-if="homework.deadline" :class="{ 'expired-deadline': homework.isExpired }">
                <strong>Дедлайн:</strong> {{ new Date(homework.deadline).toLocaleString() }}
                <span v-if="homework.isExpired">(час вийшов)</span>
            </p>
        </div>
        <hr>
        
        <div v-if="canViewSubmissions" class="submissions-section">
            <h2>Рішення студентів ({{ totalSubmissions }})</h2>
             <div v-if="submissionsLoading"><Preloader/></div>
             <div v-else-if="submissions.length === 0" class="empty-message">Рішень ще немає.</div>
             <div v-else class="submissions-list">
                 <div v-for="sub in submissions" :key="sub.id" class="submission-item">
                    <div class="sub-header">
                        <p class="user"><strong>Студент:</strong> {{ sub.user.name }} (@{{ sub.user.login }})</p>
                        <p class="lang"><i class="fa-solid fa-code"></i> {{ getDisplayLanguageText(sub.language) }}</p>
                    </div>
                    <div class="sub-details">
                        <span :class="['status-badge', sub.correct ? 'success' : 'fail']">
                            {{ sub.correct ? 'Зараховано' : 'Не зараховано' }}
                        </span>
                        <span class="score-badge">
                            Бали: {{ sub.score }}
                        </span>
                    </div>
                     <p class="code-block" v-if="sub.code">
                        <MonacoCodeViewer 
                            :code="sub.code"
                            :language="sub.language"
                            height="300px"
                            :showHeader="true"
                            :showCopyButton="true"
                        />
                    </p>
                 </div>
                 <!-- PAGINATION -->
                 <button v-if="hasNextPage" @click="loadMoreSubmissions" class="load-more-btn">
                    Завантажити ще
                </button>
             </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import BreadcrumbNavigation from '@/components/BreadcrumbNavigation.vue'; // IMPORT BREADCRUMBS
import { handleApiError } from '@/services/errorHandler.js';
import { hasPermission } from '@/services/permissionService.js';
import MonacoCodeViewer from '../components/MonacoCodeViewer.vue';
import { getDisplayLanguageText, loadProgrammingLanguages } from '../services/programmingLanguageService';

const route = useRoute();
const router = useRouter();

const homework = ref(null);
const loading = ref(true);
const submissionsLoading = ref(false);

// --- PAGINATION STATE ---
const submissions = ref([]);
const currentPage = ref(1);
const hasNextPage = ref(false);
const totalSubmissions = ref(0);
// --- END PAGINATION ---

const canEdit = ref(false);
const canViewSubmissions = ref(false);

const homeworkId = computed(() => route.params.id);
const treeNodeId = ref(null);

const checkPermissions = async () => {
    if (!treeNodeId.value) return;
    canEdit.value = await hasPermission(treeNodeId.value, 'EDIT_HOMEWORKS');
    canViewSubmissions.value = await hasPermission(treeNodeId.value, 'VIEW_HOMEWORK_SUBMISSIONS');
};

const fetchHomework = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/homework/${homeworkId.value}`);
        homework.value = response.data;
        
        // This extra call is necessary because HW_DTO doesn't have treeNodeId
        const groupRes = await apiClient.get(`/group/${homework.value.groupId}`);
        treeNodeId.value = groupRes.data.treeNodeId;

        await checkPermissions();
        if (canViewSubmissions.value) {
            fetchSubmissions(1);
        }
    } catch (error) {
        handleApiError(error);
        router.push('/my-homework'); // Assuming this is a valid route
    } finally {
        loading.value = false;
    }
};

// --- UPDATED FOR PAGINATION ---
const fetchSubmissions = async (page = 1) => {
    submissionsLoading.value = true;
    try {
        const response = await apiClient.get(`/homework/${homeworkId.value}/submissions`, {
            params: {
                page: page,
                limit: 10 // Page size
            }
        });
        if (page === 1) {
            submissions.value = response.data.content;
        } else {
            submissions.value.push(...response.data.content);
        }
        hasNextPage.value = response.data.hasNextPage; // Assuming 'hasNextPage'
        totalSubmissions.value = response.data.totalElements;
        currentPage.value = page;
    } catch (error) {
        handleApiError(error);
    } finally {
        submissionsLoading.value = false;
    }
};

const loadMoreSubmissions = () => {
    if (hasNextPage.value) {
        fetchSubmissions(currentPage.value + 1);
    }
};
// --- END PAGINATION ---

onMounted(async () => {
    await loadProgrammingLanguages();
    fetchHomework();
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.homework-page {
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

.description {
    font-family: $form-font;
    color: var(--secondary-color);
    margin-top: 0.5rem;
}

.details {
    p { 
        margin: 0.5rem 0; 
        font-family: $form-font;
        color: var(--text-color);
    }
    a { 
        color: var(--text-color); 
        text-decoration: underline;
        &:hover {
            color: var(--info-color);
        }
    }
    .expired-deadline {
        color: var(--error-color);
        font-weight: 600;
        span {
            font-size: 0.9em;
            color: var(--secondary-color);
            font-weight: 400;
        }
    }
}

.submissions-section { 
    margin-top: 2rem; 
    h2 {
        font-family: $secondary-font;
        color: var(--text-color);
        margin-bottom: 1rem;
    }
}
.submissions-list { 
    display: flex; 
    flex-direction: column; 
    gap: 1rem; 
}
.submission-item {
    background-color: var(--weak-color);
    padding: 1rem; 
    border-radius: 5px;
    border: 1px solid var(--secondary-color);

    .sub-header {
        display: flex;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 10px;
        margin-bottom: 10px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--secondary-color);
    }

    .user {
        font-family: $form-font;
        font-weight: 600;
        margin: 0;
    }
    
    .lang {
        font-family: $form-font;
        font-size: 0.9em;
        color: var(--secondary-color);
        margin: 0;
    }
    
    .sub-details {
        display: flex;
        gap: 10px;
        align-items: center;
        margin-bottom: 10px;
    }

    .status-badge, .score-badge {
        font-family: $form-font;
        font-weight: 600;
        font-size: 0.9em;
        padding: 4px 8px;
        border-radius: 3px;
        color: white;
    }

    .status-badge.success { background-color: var(--success-color); }
    .status-badge.fail { background-color: var(--error-color); }
    .score-badge { background-color: var(--info-color); }

    .code-block {
        margin-top: 10px;
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
        background-color: var(--weak-color-hover);
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
