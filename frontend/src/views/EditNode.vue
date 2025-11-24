<template>
    <h1>Редагування вузла</h1>
    <div v-if="loading"><Preloader /></div>
    <form @submit.prevent="updateNode" class="main-form" v-if="!loading && canEdit">
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
        
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!isFormValid || submitting">
            <i class="fa-regular fa-save"></i> {{ submitting ? 'Зберегти опис' : 'Зберегти опис' }}
        </LinkBtn>
    </form>
    
    <div class="main-form" v-if="!loading && canMove">
        <hr>
        <h2>Перемістити вузол</h2>
         <EntitySelector 
            v-model="newParentId"
            entity-type="node"
            label="Новий батьківський елемент (необов'язково)"
        />
        <br>
        <LinkBtn @click="moveNode" img="wave2.svg" anim="go" :disabled="submitting">
            <i class="fa-solid fa-truck-moving"></i> Перемістити
        </LinkBtn>
    </div>

    <div class="main-form" v-if="!loading && canDelete">
        <hr>
        <h2>Видалення</h2>
        <LinkBtn role="btn" type="button" color="danger" anim="bg-scale" @click="promptDelete">
            <i class="fa-solid fa-trash"></i> Видалити вузол
        </LinkBtn>
    </div>

    <div v-if="!loading && !canEdit">
        <p class="error">У вас недостатньо прав для редагування цього вузла.</p>
    </div>
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>
<style lang="scss" scoped> 
form.main-form, h1, .main-form { width: 100%; max-width: 800px; margin-left: auto; margin-right: auto; } 
.error { color: #f44336; text-align: center; margin-top: 2rem; }
hr { margin: 2rem 0; }
</style>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue';
import { useStore } from 'vuex';
import { useRouter, useRoute } from 'vue-router';
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '@/axios.js';
import Preloader from "@/components/Preloader.vue";
import EntitySelector from '@/components/EntitySelector.vue';
import { handleApiError } from '@/services/errorHandler.js';
import { showQuestionModal } from '@/services/modalService.js';
import { hasPermission, clearPermissionCache } from '@/services/permissionService.js';

const store = useStore();
const router = useRouter();
const route = useRoute();

const values = reactive({ title: '', description: '' });
const errors = reactive({ title: '', description: '' });
const newParentId = ref(null);
const originalParentId = ref(null);
const treeNodeId = ref(null);

const isFormValid = ref(false);
const loading = ref(true);
const submitting = ref(false);

const canEdit = ref(false), canDelete = ref(false), canMove = ref(false);
const nodeId = route.params.id;

const loadNode = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/node/${nodeId}`);
        const node = response.data;
        values.title = node.title;
        values.description = node.description;
        newParentId.value = node.parentId;
        originalParentId.value = node.parentId;
        treeNodeId.value = node.treeNodeId;

        // Check permissions
        canEdit.value = await hasPermission(treeNodeId.value, 'EDIT_NODES');
        canDelete.value = await hasPermission(treeNodeId.value, 'DELETE_NODES');
        canMove.value = await hasPermission(treeNodeId.value, 'MOVE_NODES');

        if (!canEdit.value) {
            store.dispatch('addErrorMessage', 'У вас немає прав редагувати цей вузол.');
        }

    } catch (error) {
        handleApiError(error);
        router.push({ name: 'NodeBrowser' });
    } finally {
        loading.value = false;
    }
};

const updateNode = async () => {
    if (!isFormValid.value || submitting.value) return;
    submitting.value = true;
    try {
        await apiClient.put(`/node/${nodeId}`, {
            title: values.title,
            description: values.description
        });
        store.dispatch('addSuccessMessage', 'Вузол успішно оновлено');
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        submitting.value = false;
    }
};

const moveNode = async () => {
    if (newParentId.value == originalParentId.value) {
        store.dispatch('addInfoMessage', 'Батьківський вузол не змінився.');
        return;
    }
    submitting.value = true;
    try {
        await apiClient.put(`/tree-node/${treeNodeId.value}/move`, null, {
            params: { newParentId: newParentId.value || null }
        });
        store.dispatch('addSuccessMessage', 'Вузол успішно переміщено');
        originalParentId.value = newParentId.value; // Обновляем "оригинал"
        clearPermissionCache(treeNodeId.value); // Права могли измениться
    } catch (error) {
        handleApiError(error);
    } finally {
        submitting.value = false;
    }
};

const deleteNode = async () => {
    try {
        await apiClient.delete(`/node/${nodeId}`);
        store.dispatch('addSuccessMessage', 'Вузол успішно видалено');
        router.push({ name: 'NodeBrowser' });
    } catch (error) {
        handleApiError(error);
    }
};

const promptDelete = () => {
    showQuestionModal(
        'Підтвердження видалення',
        'Ви впевнені? Цю дію неможливо скасувати. Вузол буде видалено, тільки якщо він не містить дочірніх елементів.',
        deleteNode
    );
};

const validateForm = () => {
    let valid = true;
    if (!values.title || values.title.length < 5 || values.title.length > 100) {
        errors.title = 'Назва вузла має бути від 5 до 100 символів'; valid = false;
    } else { errors.title = ''; }
    if (values.description && values.description.length > 1000) {
        errors.description = 'Опис вузла має бути до 1000 символів'; valid = false;
    } else { errors.description = ''; }
    isFormValid.value = valid;
};

watch(values, validateForm, { deep: true });
onMounted(loadNode);
</script>
