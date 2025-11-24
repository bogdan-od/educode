<template>
    <h1>Створення вузла</h1>
    <form @submit.prevent="createNode" class="main-form">
        <div class="form-group">
            <div><i class="fa-solid fa-signature"></i></div>
            <input type="text" v-model="values.title" id="title" placeholder="Назва вузла" required>
            <label for="title">Назва вузла</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>

        <div class="form-group">
            <div><i class="fa-solid fa-align-left"></i></div>
            <textarea v-model="values.description" id="description" placeholder="Опис"></textarea>
            <label for="description">Опис</label>
            <p class="hint error">{{ errors.description }}</p>
        </div>
        
        <EntitySelector 
            v-model="values.parentId"
            entity-type="node"
            label="Батьківський елемент (необов'язково)"
        />
        <p class="hint error">{{ errors.parentId }}</p>

        <br>
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!isFormValid || loading">
            <i class="fa-regular fa-square-plus"></i> {{ loading ? 'Створення...' : 'Створити' }}
        </LinkBtn>
    </form>
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>
<style lang="scss" scoped> form.main-form, h1 { width: 100%; max-width: 800px; } </style>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter, useRoute } from 'vue-router';
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '@/axios.js';
import EntitySelector from '@/components/EntitySelector.vue';
import { handleApiError } from '@/services/errorHandler.js';
import { hasPermission, hasGlobalPermission, clearPermissionCache } from '@/services/permissionService.js';

const store = useStore();
const router = useRouter();
const route = useRoute();

const values = reactive({ title: '', description: '', parentId: null });
const errors = reactive({ title: '', description: '', parentId: '' });
const isFormValid = ref(false);
const loading = ref(false);

const createNode = async () => {
    validateForm();
    if (!isFormValid.value || loading.value) return;
    loading.value = true;
    try {
        const response = await apiClient.post('/node/create', { ...values });
        store.dispatch('addSuccessMessage', 'Вузол успішно створено');
        // Очищаем кэш разрешений для родителя, т.к. мы могли получить к нему доступ
        if(values.parentId) {
            const parentNode = await apiClient.get(`/node/${values.parentId}`);
            clearPermissionCache(parentNode.data.treeNodeId);
        }
        router.push({ name: 'NodeBrowser', params: { id: response.data.nodeId } });
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        loading.value = false;
    }
};

const validateForm = () => {
    let valid = true;
    if (!values.title || values.title.length < 5 || values.title.length > 100) {
        errors.title = 'Назва вузла має бути від 5 до 100 символів';
        valid = false;
    } else { errors.title = ''; }

    if (values.description && values.description.length > 1000) {
        errors.description = 'Опис вузла має бути до 1000 символів';
        valid = false;
    } else { errors.description = ''; }
    isFormValid.value = valid;
};

watch(values, validateForm, { deep: true });

onMounted(async () => {
    const parentIdFromQuery = route.query.parentId;
    if (parentIdFromQuery) {
        values.parentId = Number(parentIdFromQuery);
    }

    // Проверка прав
    let canCreate = false;
    if (values.parentId) {
        const parentNode = await apiClient.get(`/node/${values.parentId}`);
        canCreate = await hasPermission(parentNode.data.treeNodeId, 'CREATE_NODES');
    } else {
        canCreate = hasGlobalPermission('CREATE_NODES');
    }

    if (!canCreate) {
        store.dispatch('addErrorMessage', 'У вас недостатньо прав для створення вузла тут.');
        router.push({ name: 'NodeBrowser' });
    }

    validateForm();
});
</script>
