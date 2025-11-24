<template>
    <h1>Редагування групи</h1>
    <div v-if="loading"><Preloader /></div>
    <form @submit.prevent="updateGroup" class="main-form" v-if="!loading && canEdit">
        <div class="form-group">
            <div><i class="fa-solid fa-signature"></i></div>
            <input type="text" v-model="values.title" id="title" placeholder="Назва групи" required>
            <label for="title">Назва групи</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>

        <div class="form-group">
            <div><i class="fa-solid fa-align-left"></i></div>
            <textarea v-model="values.description" id="description" placeholder="Опис" required></textarea>
            <label for="description">Опис</label>
            <p class="hint error">{{ errors.description }}</p>
        </div>
        
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!isFormValid || submitting">
            <i class="fa-regular fa-save"></i> {{ submitting ? 'Збереження...' : 'Зберегти опис' }}
        </LinkBtn>
    </form>
    
    <div class="main-form" v-if="!loading && canMove">
        <hr>
        <h2>Перемістити групу</h2>
         <EntitySelector 
            v-model="newParentId"
            entity-type="node"
            label="Новий батьківський вузол (необов'язково)"
        />
        <br>
        <LinkBtn @click="moveGroup" img="wave2.svg" anim="go" :disabled="submitting">
            <i class="fa-solid fa-truck-moving"></i> Перемістити
        </LinkBtn>
    </div>

    <div class="main-form" v-if="!loading && canDelete">
        <hr>
        <h2>Видалення</h2>
        <LinkBtn role="btn" type="button" color="danger" anim="bg-scale" @click="promptDelete">
            <i class="fa-solid fa-trash"></i> Видалити групу
        </LinkBtn>
    </div>

    <div v-if="!loading && !canEdit">
        <p class="error">У вас недостатньо прав для редагування цієї групи.</p>
    </div>
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>
<style lang="scss" scoped> 
form.main-form, h1, .main-form { width: 100%; max-width: 800px; margin-left: auto; margin-right: auto; } 
.error { color: #f44336; text-align: center; margin-top: 2rem; }
hr { margin: 2rem 0; }
</style>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
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
const groupId = route.params.id;

const loadGroup = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/group/${groupId}`);
        const group = response.data;
        values.title = group.title;
        values.description = group.description;
        newParentId.value = group.parentId;
        originalParentId.value = group.parentId;
        treeNodeId.value = group.treeNodeId;

        // Check permissions
        canEdit.value = await hasPermission(treeNodeId.value, 'EDIT_GROUPS');
        canDelete.value = await hasPermission(treeNodeId.value, 'DELETE_GROUPS');
        canMove.value = await hasPermission(treeNodeId.value, 'MOVE_GROUPS');

        if (!canEdit.value) {
            store.dispatch('addErrorMessage', 'У вас немає прав редагувати цю групу.');
        }

    } catch (error) {
        handleApiError(error);
        router.push({ name: 'NodeBrowser' });
    } finally {
        loading.value = false;
    }
};

const updateGroup = async () => {
    if (!isFormValid.value || submitting.value) return;
    submitting.value = true;
    try {
        await apiClient.put(`/group/${groupId}`, {
            title: values.title,
            description: values.description
        });
        store.dispatch('addSuccessMessage', 'Групу успішно оновлено');
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        submitting.value = false;
    }
};

const moveGroup = async () => {
    if (newParentId.value == originalParentId.value) {
        store.dispatch('addInfoMessage', 'Батьківський вузол не змінився.');
        return;
    }
    submitting.value = true;
    try {
        await apiClient.put(`/tree-node/${treeNodeId.value}/move`, null, {
            params: { newParentId: newParentId.value || null }
        });
        store.dispatch('addSuccessMessage', 'Групу успішно переміщено');
        originalParentId.value = newParentId.value; // Обновляем "оригинал"
        clearPermissionCache(treeNodeId.value);
    } catch (error) {
        handleApiError(error);
    } finally {
        submitting.value = false;
    }
};

const deleteGroup = async () => {
    try {
        await apiClient.delete(`/group/${groupId}`);
        store.dispatch('addSuccessMessage', 'Групу успішно видалено');
        router.push({ name: 'NodeBrowser' });
    } catch (error) {
        handleApiError(error);
    }
};

const promptDelete = () => {
    showQuestionModal(
        'Підтвердження видалення',
        'Ви впевнені? Цю дію неможливо скасувати.',
        deleteGroup
    );
};

const validateForm = () => {
    let valid = true;
    if (!values.title || values.title.length < 5 || values.title.length > 100) {
        errors.title = 'Назва групи має бути від 5 до 100 символів'; valid = false;
    } else { errors.title = ''; }
    if (!values.description || values.description.length < 1) {
        errors.description = 'Опис не може бути порожнім'; valid = false;
    } else if (values.description.length > 1000) {
        errors.description = 'Опис групи має бути до 1000 символів'; valid = false;
    } else { errors.description = ''; }
    isFormValid.value = valid;
};

watch(values, validateForm, { deep: true });
onMounted(loadGroup);
</script>
