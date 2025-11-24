<template>
    <h1>Створити домашнє завдання</h1>
    <form @submit.prevent="createHomework" class="main-form">
        <div class="form-group">
            <div><i class="fa-solid fa-heading"></i></div>
            <input type="text" v-model="values.title" id="title" placeholder="Заголовок" required>
            <label for="title">Заголовок</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>

        <div class="form-group">
            <div><i class="fa-solid fa-file-alt"></i></div>
            <textarea v-model="values.content" id="content" placeholder="Опис"></textarea>
            <label for="content">Опис</label>
            <p class="hint error">{{ errors.content }}</p>
        </div>

        <EntitySelector 
            v-model="values.groupId"
            entity-type="group"
            label="Група"
        />
        <p class="hint error">{{ errors.groupId }}</p>
        
        <ApiSearchSelector
            v-model="values.puzzleId"
            apiUrl="/puzzles/autocomplete"
            label="Задача (необов'язково)"
            placeholder="Пошук задачі за назвою..."
        />
        <p class="hint error">{{ errors.puzzleId }}</p>

        <div class="form-group">
            <div><i class="fa-solid fa-calendar-alt"></i></div>
            <input type="datetime-local" v-model="values.deadline" id="deadline">
            <label for="deadline">Дедлайн</label>
            <p class="hint error">{{ errors.deadline }}</p>
        </div>

        <br>
        <LinkBtn role="btn" type="submit" :disabled="!isFormValid || loading" anim="go">
            <i class="fa-regular fa-square-plus"></i> {{ loading ? 'Створення...' : 'Створити' }}
        </LinkBtn>
    </form>
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>
<style lang="scss" scoped> form.main-form, h1 { width: 100%; max-width: 800px; } .form-group, .api-search-selector, .entity-selector-wrapper { margin-bottom: 1rem; } </style>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '@/axios';
import LinkBtn from '@/components/LinkBtn.vue';
import EntitySelector from '@/components/EntitySelector.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import { handleApiError } from '@/services/errorHandler.js';

const store = useStore();
const router = useRouter();
const route = useRoute();

const values = reactive({ title: '', content: '', groupId: null, puzzleId: null, deadline: '' });
const errors = reactive({ title: '', content: '', groupId: '', puzzleId: '', deadline: '' });
const isFormValid = ref(false);
const loading = ref(false);

const getCorrectDatetime = (datetimeValue) => {
    const date = new Date(datetimeValue);
    const offsetMinutes = date.getTimezoneOffset();
    date.setMinutes(date.getMinutes() - offsetMinutes);
    return date.toISOString().slice(0, 16);
}

const createHomework = async () => {
    validateForm();
    if (!isFormValid.value || loading.value) return;
    loading.value = true;
    try {
        const payload = { ...values, deadline: getCorrectDatetime(values.deadline) };
        const response = await apiClient.post('/homework/create', payload);
        store.dispatch('addSuccessMessage', 'Домашнє завдання успішно створено!');
        router.push(`/homework/${response.data.id}`);
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        loading.value = false;
    }
};

const validateForm = () => {
    let valid = true;
    if (!values.title || values.title.trim().length < 1) {
        errors.title = 'Заголовок не може бути порожнім.'; valid = false;
    } else { errors.title = ''; }
    if (!values.groupId) {
        errors.groupId = "Вибір групи є обов'язковим."; valid = false;
    } else { errors.groupId = ''; }
    isFormValid.value = valid;
};

watch(values, validateForm, { deep: true });

onMounted(() => {
    const groupIdFromQuery = route.query.groupId;
    if (groupIdFromQuery) {
        values.groupId = Number(groupIdFromQuery);
    }
    validateForm();
});
</script>
