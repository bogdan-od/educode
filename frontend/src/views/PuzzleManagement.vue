<template>
    <h1>Адміністрування задачі</h1>
    <div v-if="loading"><Preloader /></div>
    <div v-else-if="puzzle" class="puzzle-management-page">
        <h2>{{ puzzle.title }}</h2>
        <p>Автор: @{{ puzzle.author }}</p>
        <hr>
        
        <div class="admin-actions" v-if="canMakeVisible || canMakeInvisible">
            <h3>Видимість задачі</h3>
            <p>Поточний статус: <strong>{{ puzzle.visible ? 'Видима' : 'Прихована' }}</strong></p>
            
             <LinkBtn v-if="canMakeInvisible && puzzle.visible" @click="makeInvisible" color="danger" anim="bg-scale" :disabled="submitting">
                {{ submitting ? '...' : 'Зробити невидимою (для всіх)' }}
            </LinkBtn>
             <LinkBtn v-if="canMakeVisible && !puzzle.visible" @click="makeVisible" color="success" anim="bg-scale" :disabled="submitting">
                {{ submitting ? '...' : 'Зробити видимою (для всіх)' }}
            </LinkBtn>
             <p class="hint">Ця дія не вплине на видимість задачі в вузлах, куди її додали вручну.</p>
        </div>
        
        <div v-else>
            <p>У вас немає прав для адміністрування цієї задачі, або ви є її автором (використовуйте сторінку редагування).</p>
        </div>
    </div>
     <div v-else class="error-page">
         <h1>Помилка</h1>
         <p>Задачу не знайдено.</p>
         <LinkBtn to="/puzzles" anim="go">До списку задач</LinkBtn>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import { handleApiError } from '@/services/errorHandler';
import { hasGlobalPermission } from '@/services/permissionService';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';

const route = useRoute();
const router = useRouter();
const store = useStore();

const puzzle = ref(null);
const loading = ref(true);
const submitting = ref(false);
const puzzleId = route.params.id;

const currentUser = computed(() => store.getters.getCurrentUser);
const isOwner = computed(() => puzzle.value && currentUser.value && puzzle.value.author === currentUser.value.login);

// Административные действия доступны НЕ владельцам с глобальными правами
const canMakeInvisible = computed(() => !isOwner.value && hasGlobalPermission('MAKE_PUZZLE_INVISIBLE'));
const canMakeVisible = computed(() => !isOwner.value && hasGlobalPermission('MAKE_PUZZLE_VISIBLE'));

const fetchPuzzle = async () => {
    loading.value = true;
    try {
        // Используем get-edit, чтобы получить данные даже невидимой задачи
        const response = await apiClient.get(`/puzzles/get-edit/${puzzleId}`);
        puzzle.value = response.data;
    } catch (error) {
        handleApiError(error);
        router.push('/puzzles');
    } finally {
        loading.value = false;
    }
};

const makeInvisible = async () => {
    submitting.value = true;
    try {
        await apiClient.put(`/puzzles/${puzzleId}/make-invisible`);
        store.dispatch('addSuccessMessage', 'Задачу зроблено невидимою.');
        fetchPuzzle();
    } catch (error) { handleApiError(error); } finally { submitting.value = false; }
};

const makeVisible = async () => {
    submitting.value = true;
    try {
        await apiClient.put(`/puzzles/${puzzleId}/make-visible`);
        store.dispatch('addSuccessMessage', 'Задачу зроблено видимою.');
        fetchPuzzle();
    } catch (error) { handleApiError(error); } finally { submitting.value = false; }
};

onMounted(fetchPuzzle);
</script>

<style lang="scss" scoped>
.puzzle-management-page {
    max-width: 800px;
    margin: 0 auto;
    padding: 2rem;
    background: var(--weak-color);
    border-radius: 5px;
}
.admin-actions {
    margin-top: 2rem;
    p { margin: 1rem 0; }
    .hint { font-size: 0.9em; color: var(--secondary-color); }
}
.error-page { text-align: center; }
</style>
