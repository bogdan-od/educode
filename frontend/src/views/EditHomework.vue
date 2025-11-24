<template>
    <h1>Редагувати домашнє завдання</h1>
    <div v-if="loading"><Preloader/></div>
    <form @submit.prevent="updateHomework" class="main-form" v-if="!loading && canEdit">
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

        <p class="info-text">Групу домашнього завдання змінити не можна.</p>
        
        <ApiSearchSelector
            v-model="values.puzzleId"
            apiUrl="/puzzles/autocomplete"
            label="Задача (необов'язково)"
            placeholder="Пошук задачі за назвою..."
            :initial-id="initialPuzzleId"
            :initial-text="initialPuzzleText"
        />
        <p class="hint error">{{ errors.puzzleId }}</p>

        <div class="form-group">
            <div><i class="fa-solid fa-calendar-alt"></i></div>
            <input type="datetime-local" v-model="values.deadline" id="deadline">
            <label for="deadline">Дедлайн</label>
            <p class="hint error">{{ errors.deadline }}</p>
        </div>

        <br>
        <LinkBtn role="btn" type="submit" :disabled="!isFormValid || submitting" anim="go">
            <i class="fa-regular fa-save"></i> {{ submitting ? 'Збереження...' : 'Зберегти' }}
        </LinkBtn>
        
        <br><br>
         <LinkBtn v-if="canDelete" role="btn" type="button" color="danger" anim="bg-scale" @click="promptDelete">
            <i class="fa-solid fa-trash"></i> Видалити ДЗ
        </LinkBtn>
    </form>
    <div v-if="!loading && !canEdit">
        <p class="error">У вас недостатньо прав для редагування цього завдання.</p>
    </div>
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>
<style lang="scss" scoped> 
form.main-form, h1 { width: 100%; max-width: 800px; } 
.form-group, .api-search-selector { margin-bottom: 1rem; }
.info-text { font-style: italic; color: var(--secondary-color); }
</style>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue';
import { useStore } from 'vuex';
import { useRouter, useRoute } from 'vue-router';
import apiClient from '@/axios.js';
import LinkBtn from '@/components/LinkBtn.vue';
import ApiSearchSelector from '@/components/ApiSearchSelector.vue';
import Preloader from '@/components/Preloader.vue';
import { handleApiError } from '@/services/errorHandler.js';
import { hasPermission } from '@/services/permissionService.js';
import { showQuestionModal } from '@/services/modalService.js';

const store = useStore();
const router = useRouter();
const route = useRoute();
const homeworkId = route.params.id;

const values = reactive({ title: '', content: '', puzzleId: null, deadline: '' });
const errors = reactive({ title: '', content: '', puzzleId: '', deadline: '' });
const initialPuzzleId = ref(null);
const initialPuzzleText = ref('');

const isFormValid = ref(false);
const loading = ref(true);
const submitting = ref(false);
const canEdit = ref(false);
const canDelete = ref(false);

const getCorrectDatetime = (datetimeValue) => {
    const date = new Date(datetimeValue);
    const offsetMinutes = date.getTimezoneOffset();
    date.setMinutes(date.getMinutes() - offsetMinutes);
    return date.toISOString().slice(0, 16);
}

const loadHomework = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/homework/${homeworkId}`);
        const hw = response.data;
        values.title = hw.title;
        values.content = hw.content;
        values.puzzleId = hw.puzzleId;
        values.deadline = hw.deadline;
        
        initialPuzzleId.value = hw.puzzleId;
        initialPuzzleText.value = hw.puzzleTitle;
        
        // Check permissions
        const groupRes = await apiClient.get(`/group/${hw.groupId}`);
        const treeNodeId = groupRes.data.treeNodeId;
        canEdit.value = await hasPermission(treeNodeId, 'EDIT_HOMEWORKS');
        canDelete.value = await hasPermission(treeNodeId, 'DELETE_HOMEWORKS');

        if (!canEdit.value) {
            store.dispatch('addErrorMessage', 'У вас немає прав редагувати це завдання.');
        }

    } catch (error) {
        handleApiError(error);
        router.push('/my-homework');
    } finally {
        loading.value = false;
    }
};

const updateHomework = async () => {
    validateForm();
    if (!isFormValid.value || submitting.value) return;
    submitting.value = true;
    try {
        const payload = { ...values, deadline: getCorrectDatetime(values.deadline) };
        await apiClient.put(`/homework/${homeworkId}`, payload);
        store.dispatch('addSuccessMessage', 'Домашнє завдання оновлено!');
        router.push(`/homework/${homeworkId}`);
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        submitting.value = false;
    }
};

const deleteHomework = async () => {
    try {
        await apiClient.delete(`/homework/${homeworkId}`);
        store.dispatch('addSuccessMessage', 'Домашнє завдання видалено.');
        router.push('/my-homework');
    } catch (error) {
        handleApiError(error);
    }
};

const promptDelete = () => {
    showQuestionModal(
        'Підтвердження видалення',
        'Ви впевнені? Цю дію неможливо скасувати. Видалення не вдасться, якщо по завданню вже є здані роботи.',
        deleteHomework
    );
};

const validateForm = () => {
    let valid = true;
    if (!values.title || values.title.trim().length < 1) {
        errors.title = 'Заголовок не може бути порожнім.'; valid = false;
    } else { errors.title = ''; }
    isFormValid.value = valid;
};

watch(values, validateForm, { deep: true });
onMounted(loadHomework);
</script>
