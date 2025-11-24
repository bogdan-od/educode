<template>
  <Modal v-model:visible="isModalVisible" :title="`Інформація про роль`" :showConfirmButton="false" cancelButtonText="Закрити">
    <div class="role-info-content">
      <div v-if="loading" class="preloader-container">
        <Preloader />
      </div>
      <div v-else-if="error" class="error-message">
        {{ error }}
      </div>
      <div v-else-if="roleDetails">
        <p class="role-description">{{ roleDetails.description }}</p>
        
        <h3>Дозволи ({{ roleDetails.permissions.length }}):</h3>
        <ul class="permissions-list">
          <li v-for="perm in roleDetails.permissions" :key="perm.name">
            <i class="fa-solid fa-check"></i>
            <span>{{ perm.description }}</span>
          </li>
        </ul>
      </div>
    </div>
  </Modal>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import apiClient from '@/axios';
import Modal from '@/components/Modal.vue';
import Preloader from '@/components/Preloader.vue';
import { handleApiError } from '@/services/errorHandler.js';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  roleName: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(['update:visible']);

const loading = ref(false);
const error = ref('');
const roleDetails = ref(null);

const isModalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
});

const fetchRoleDetails = async (name) => {
  if (!name) return;
  loading.value = true;
  error.value = '';
  roleDetails.value = null;
  try {
    // Используем новый эндпоинт
    const response = await apiClient.get(`/roles/name/${name}`);
    roleDetails.value = response.data; // RoleDetailDTO
  } catch (err) {
    handleApiError(err);
    error.value = "Не вдалося завантажити інформацію про роль.";
  } finally {
    loading.value = false;
  }
};

watch(() => props.visible, (newVal) => {
  if (newVal && props.roleName) {
    fetchRoleDetails(props.roleName);
  }
});
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.role-info-content {
  font-family: $form-font;
  min-height: 150px;
}

.preloader-container, .error-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 150px;
  font-family: $form-font;
  color: var(--secondary-color);
}
.error-message {
  color: var(--error-color);
}

.role-description {
  font-size: 1.1em;
  color: var(--text-color);
  background-color: var(--weak-color);
  padding: 10px 15px;
  border-radius: 3px;
}

h3 {
  font-family: $secondary-font;
  color: var(--text-color);
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--secondary-color);
  padding-bottom: 5px;
}

.permissions-list {
  list-style: none;
  padding: 0;
  max-height: 300px;
  overflow-y: auto;

  li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid var(--weak-color);
    
    i {
      color: var(--success-color);
      font-size: 0.9em;
    }
    
    span {
      color: var(--text-color);
      flex-grow: 1;
    }
    
    .perm-name {
      font-family: 'Courier New', Courier, monospace;
      font-size: 0.9em;
      color: var(--secondary-color);
      background-color: var(--weak-color);
      padding: 2px 4px;
      border-radius: 3px;
    }
  }
}
</style>
