<template>
    <h1>Створення групи</h1>
    <form @submit.prevent="createGroup" class="main-form">
        <!-- Group name input field -->
        <div class="form-group">
            <div><i class="fa-solid fa-signature"></i></div>
            <input type="text" v-model="values.title" id="title" placeholder="Назва групи" required>
            <label for="title">Назва групи</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>

        <!-- Group description textarea -->
        <div class="form-group">
            <div><i class="fa-solid fa-align-left"></i></div>
            <textarea v-model="values.description" id="description" placeholder="Опис" required></textarea>
            <label for="description">Опис</label>
            <p class="hint error">{{ errors.description }}</p>
        </div>
        
        <!-- Optional parent node selector - group inherits permissions from parent -->
        <EntitySelector 
            v-model="values.parentId"
            entity-type="node"
            label="Батьківський вузол (необов'язково)"
        />
        <p class="hint error">{{ errors.parentId }}</p>

        <br>
        <!-- Form submission button -->
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

// Form data for group creation
const values = reactive({ title: '', description: '', parentId: null });

// Validation errors corresponding to form fields
const errors = reactive({ title: '', description: '', parentId: '' });

const isFormValid = ref(false);
const loading = ref(false);

// Submits group creation form to backend and redirects to new group page
const createGroup = async () => {
    validateForm();
    if (!isFormValid.value || loading.value) return;
    loading.value = true;
    try {
        const response = await apiClient.post('/group/create', { ...values });
        store.dispatch('addSuccessMessage', 'Групу успішно створено');
        // Refresh parent node permissions cache if group has parent
        if(values.parentId) {
             const parentNode = await apiClient.get(`/node/${values.parentId}`);
             clearPermissionCache(parentNode.data.treeNodeId);
        }
        router.push(`/group/${response.data.groupId}`);
    } catch (error) {
        handleApiError(error, errors);
    } finally {
        loading.value = false;
    }
};

// Validates form fields and updates isFormValid flag accordingly
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

// Re-validate on every reactive value change
watch(values, validateForm, { deep: true });

// Check user permissions and pre-populate parentId from URL query if provided
onMounted(async () => {
    const parentIdFromQuery = route.query.parentId;
    if (parentIdFromQuery) {
        values.parentId = Number(parentIdFromQuery);
    }
    
    let canCreate = false;
    if (values.parentId) {
        // Check permissions in parent node context
        const parentNode = await apiClient.get(`/node/${values.parentId}`);
        canCreate = await hasPermission(parentNode.data.treeNodeId, 'CREATE_GROUPS');
    } else {
        // Check global permissions for root-level group creation
        canCreate = hasGlobalPermission('CREATE_GROUPS');
    }

    if (!canCreate) {
        store.dispatch('addErrorMessage', 'У вас недостатньо прав для створення групи тут.');
        router.push({ name: 'NodeBrowser' });
    }

    validateForm();
});
</script>
