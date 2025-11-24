<template>
    <h1>Домашні завдання</h1>
    <LinkBtn to="/create/homework" anim="go">
        <i class="fa-regular fa-square-plus"></i> Створити домашнє завдання
    </LinkBtn>
    <hr>
    
    <div v-if="loading"><Preloader/></div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="homeworks.length === 0" class="empty-message">Немає доступних домашніх завдань.</div>
    <div v-else class="homeworks-list">
        <div v-for="hw in homeworks" :key="hw.id" class="homework-item">
            <div class="hw-info">
                <h3><router-link :to="`/homework/${hw.id}`">{{ hw.title }}</router-link></h3>
                <p><strong>Група:</strong> {{ hw.groupTitle }}</p>
                <p v-if="hw.puzzleTitle"><strong>Задача:</strong> {{ hw.puzzleTitle }}</p>
                <p v-if="hw.deadline"><strong>Дедлайн:</strong> {{ new Date(hw.deadline).toLocaleString() }}</p>
            </div>
            <div class="hw-status">
                <span v-if="hw.hasSubmitted" class="status submitted">Здано</span>
                <span v-else-if="hw.isExpired" class="status expired">Прострочено</span>
                <span v-else class="status active">Активно</span>
            </div>
        </div>
    </div>
    
     <div class="pagination" v-if="!loading && homeworks.length > 0">
        <button @click="changePage(page - 1)" :disabled="page <= 1">Попередня</button>
        <span>Сторінка {{ page }}</span>
        <button @click="changePage(page + 1)" :disabled="!hasNext">Наступна</button>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import { handleApiError } from '@/services/errorHandler.js';

const router = useRouter();
const route = useRoute();

const homeworks = ref([]);
const loading = ref(true);
const error = ref('');
const hasNext = ref(false);

const page = computed(() => Number(route.query.page) || 1);

const fetchHomeworks = async () => {
    loading.value = true;
    error.value = '';
    try {
        const response = await apiClient.get('/homework/my', {
            params: {
                page: page.value,
                limit: 20,
                includeExpired: true
            }
        });
        homeworks.value = response.data.homeworks.content;
        hasNext.value = response.data.hasNext;
    } catch (err) {
        handleApiError(err);
        error.value = 'Не вдалося завантажити домашні завдання.';
    } finally {
        loading.value = false;
    }
};

const changePage = (newPage) => {
    router.push({ query: { ...route.query, page: newPage } });
};

watch(page, fetchHomeworks, { immediate: true });
</script>

<style lang="scss" scoped>
.homeworks-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}
.homework-item {
    background: var(--weak-color);
    padding: 1rem;
    border-radius: 5px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
}
.hw-info h3 a {
    color: var(--text-color);
    text-decoration: none;
    &:hover {
        text-decoration: underline;
    }
}
.hw-status .status {
    padding: .25em .6em;
    border-radius: 10px;
    color: white;
    font-size: 0.9em;
    &.submitted { background-color: #28a745; }
    &.expired { background-color: #dc3545; }
    &.active { background-color: #007bff; }
}
.pagination {
    margin-top: 1rem;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 1rem;
}
</style>
