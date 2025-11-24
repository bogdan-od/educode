<template>
    <h1>Мої домашні завдання</h1>
    <p>Тут зібрані всі завдання з груп, у яких ви берете участь.</p>
    <hr>
    
    <div v-if="loading && currentPage === 1"><Preloader/></div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="homeworks.length === 0" class="empty-message">У вас немає активних домашніх завдань.</div>
    <div v-else class="homeworks-list">
        <div v-for="hw in homeworks" :key="hw.id" class="homework-item">
            <div class="hw-info">
                <h3><router-link :to="`/homework/${hw.id}`">{{ hw.title }}</router-link></h3>
                <p><strong>Група:</strong> {{ hw.groupTitle }}</p>
                <p v-if="hw.puzzleTitle"><strong>Задача:</strong> <router-link :to="`/puzzle/${hw.puzzleId}?homeworkId=${hw.id}`">{{ hw.puzzleTitle }}</router-link></p>
                <p v-if="hw.deadline" :class="{'expired': hw.isExpired}">
                    <strong>Дедлайн:</strong> {{ new Date(hw.deadline).toLocaleString() }}
                </p>
            </div>
            <div class="hw-status">
                <span v-if="hw.hasSubmitted" class="status submitted">
                    <i class="fa-solid fa-check-double"></i> Здано
                </span>
                <span v-else-if="hw.isExpired" class="status expired">
                    <i class="fa-solid fa-calendar-times"></i> Прострочено
                </span>
                <span v-else class="status active">
                    <i class="fa-solid fa-clock"></i> Активно
                </span>
            </div>
        </div>
    </div>
    
     <div class="pagination" v-if="!loading && (hasNext || loadingMore)">
        <button @click="loadMore" :disabled="!hasNext || loadingMore" class="load-more-btn">
            <Preloader v-if="loadingMore" :scale="0.5" />
            <span v-else>Завантажити ще</span>
        </button>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import { handleApiError } from '@/services/errorHandler.js';

const router = useRouter();
const route = useRoute();

const homeworks = ref([]);
const loading = ref(true);
const loadingMore = ref(false); // Loader for "load more"
const error = ref('');
const hasNext = ref(false);
const currentPage = ref(1); // Internal page tracking

const fetchHomeworks = async (page = 1) => {
    if (page === 1) {
        loading.value = true;
    } else {
        loadingMore.value = true;
    }
    error.value = '';
    
    try {
        const response = await apiClient.get('/homework/my', {
            // API uses 1-based indexing for page
            params: { page: page, limit: 10, includeExpired: true } 
        });
        
        if (page === 1) {
            homeworks.value = response.data.homeworks.content;
        } else {
            homeworks.value.push(...response.data.homeworks.content);
        }
        
        hasNext.value = response.data.hasNext;
        currentPage.value = page;
    } catch (err) {
        handleApiError(err);
        error.value = 'Не вдалося завантажити домашні завдання.';
    } finally {
        loading.value = false;
        loadingMore.value = false;
    }
};

const loadMore = () => {
    if (hasNext.value) {
        fetchHomeworks(currentPage.value + 1);
    }
};

// Initial load
onMounted(() => {
    fetchHomeworks(1);
});
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

.homeworks-list { 
    display: flex; 
    flex-direction: column; 
    gap: 1rem; 
    margin-top: 1.5rem;
}
.homework-item { 
    background: var(--weak-color); 
    padding: 1.25rem; 
    border-radius: 5px; 
    border: 1px solid var(--secondary-color);
    display: flex; 
    justify-content: space-between; 
    align-items: flex-start; 
    gap: 1rem;
    transition: border-color 0.2s ease;

    &:hover {
        border-color: var(--info-color);
    }
}

.hw-info {
    font-family: $form-font;
    h3 {
        font-family: $secondary-font;
        font-size: 1.25em;
        margin-top: 0;
        margin-bottom: 0.5rem;
        
        a { 
            color: var(--text-color); 
            text-decoration: none; 
            &:hover { text-decoration: underline; } 
        }
    }
    p {
        font-size: 0.9em;
        margin: 0.25rem 0;
        color: var(--secondary-color);
        strong {
            color: var(--text-color);
        }
        a {
            color: var(--text-color);
            text-decoration: underline;
            &:hover {
                color: var(--info-color);
            }
        }
        &.expired {
            color: var(--error-color);
            font-weight: 600;
        }
    }
}

.hw-status {
    flex-shrink: 0;
    .status { 
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: .3em .7em; 
        border-radius: 15px; 
        color: white; 
        font-size: 0.9em;
        font-family: $form-font;
        font-weight: 600;
        
        i {
            font-size: 0.9em;
        }

        &.submitted { background-color: var(--success-color); }
        &.expired { background-color: var(--error-color); }
        &.active { background-color: var(--info-color); }
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
