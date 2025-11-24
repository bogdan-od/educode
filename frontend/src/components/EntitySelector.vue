<template>
    <div class="entity-selector-wrapper">
        <label v-if="label" class="form-label">{{ label }}</label>
        <div class="entity-selector" ref="container">
            <!-- ОБНОВЛЕНО: Добавлен :class="{ invalid: props.invalid }" -->
            <div class="selected-display" @click="toggleDropdown" :class="{ invalid: props.invalid }">
                <span v-if="selectedItem">{{ selectedItem.title }}</span>
                <span v-else-if="modelValue" class="placeholder">Завантаження...</span>
                <span v-else class="placeholder">Натисніть, щоб обрати...</span>
                <i class="fa-solid fa-chevron-down"></i>
            </div>
            
            <div v-if="isOpen" class="dropdown">
                <div class="header">
                    <button @click="goBack" :disabled="history.length === 0" class="nav-btn">
                        <i class="fa-solid fa-arrow-left"></i>
                    </button>
                    <div class="breadcrumbs">
                        <span @click="goTo(null)" class="crumb">Корінь</span>
                        <span v-for="(crumb, index) in history" :key="crumb.id" @click="goTo(crumb.id)" class="crumb">
                            / {{ crumb.title }}
                        </span>
                    </div>
                </div>

                <div v-if="isLoading" class="preloader-container"><Preloader /></div>
                <div v-else class="content">
                    <div v-if="error" class="error-message">{{ error }}</div>
                    <div v-if="currentItems.length === 0 && !error" class="empty-message">Порожньо</div>
                    <div v-for="item in currentItems" :key="item.type + '-' + item.id" class="item-row">
                        <!-- Кликабельная область для навигации (только для узлов) -->
                        <div class="item-info" @click="item.type === 'node' ? navigateTo(item) : null" :class="{ 'navigable': item.type === 'node' }">
                            <i :class="item.type === 'node' ? 'fa-solid fa-folder' : 'fa-solid fa-users'"></i>
                            <span>{{ item.title }}</span>
                        </div>
                        <!-- Кнопка выбора -->
                        <button v-if="isSelectable(item)" @click="selectItem(item)" class="select-btn">
                            Обрати
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <!-- ДОБАВЛЕНО: Сообщение об ошибке валидации -->
        <div v-if="props.invalid && props.errorMessage" class="error-message validation-error">
          {{ props.errorMessage }}
        </div>
    </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import { onClickOutside } from '@vueuse/core';
import { handleApiError } from '@/services/errorHandler.js';

const props = defineProps({
  modelValue: [Number, String], // Может быть nodeId, groupId или treeNodeId
  label: String,
  entityType: { type: String, default: 'node' }, // 'node', 'group', or 'tree-node'
  // --- НОВЫЕ PROPS ---
  invalid: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  }
  // --- КОНЕЦ НОВЫХ PROPS ---
});
const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);
const isLoading = ref(true);
const currentItems = ref([]); // { id, title, type, treeNodeId, ... }
const history = ref([]); // { id, title, treeNodeId }
const error = ref('');
const selectedItem = ref(null);
const container = ref(null);

onClickOutside(container, () => isOpen.value = false);

// Загрузка корневых элементов
const fetchRootItems = async () => {
    isLoading.value = true;
    error.value = '';
    currentItems.value = [];
    history.value = [];
    try {
        // Используем /api/node/roots, который возвращает только корневые *узлы*
        const res = await apiClient.get('/node/roots');
        // --- ИСПРАВЛЕНИЕ: API возвращает { "nodes": [...] } ---
        currentItems.value = res.data.nodes.map(n => ({ ...n, type: 'node' }));
    } catch(err) {
        handleApiError(err);
        error.value = 'Помилка завантаження';
    } finally {
        isLoading.value = false;
    }
};

// Загрузка дочерних элементов по treeNodeId
const fetchChildren = async (node) => {
    isLoading.value = true;
    error.value = '';
    currentItems.value = [];
    
    // Ищем узел в истории, чтобы не дублировать
    if (!history.value.find(h => h.id === node.id)) {
        history.value.push({ id: node.id, title: node.title, treeNodeId: node.treeNodeId });
    }
    
    try {
        const response = await apiClient.get(`/tree-node/${node.treeNodeId}/children`);
        const { childNodes = [], childGroups = [] } = response.data;
        
        // --- ИСПРАВЛЕНИЕ: Маппинг ID из DTO ---
        // NodeViewDTO имеет `id`, `treeNodeId`
        // GroupViewDTO имеет `id`, `treeNodeId`
        const items = [
            ...childNodes.map(n => ({ ...n, type: 'node' })), 
            ...childGroups.map(g => ({ ...g, type: 'group' }))
        ];
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

        items.sort((a, b) => a.title.localeCompare(b.title));
        currentItems.value = items;
    } catch (err) {
        handleApiError(err);
        error.value = 'Помилка завантаження дочірніх елементів';
    } finally {
        isLoading.value = false;
    }
};

const navigateTo = (item) => {
    if (item.type !== 'node') return;
    fetchChildren(item);
};

const goBack = () => {
    history.value.pop();
    if (history.value.length === 0) {
        fetchRootItems();
    } else {
        const parent = history.value[history.value.length - 1];
        // Нужно перезагрузить, т.к. history.pop() удалил текущий
        fetchChildren(parent);
    }
};

const goTo = (nodeId) => {
    if (nodeId === null) {
        fetchRootItems();
    } else {
        const index = history.value.findIndex(h => h.id === nodeId);
        if (index > -1) {
            const node = history.value[index];
            history.value.splice(index); // Обрезаем историю
            fetchChildren(node);
        }
    }
};

const isSelectable = (item) => {
    if (props.entityType === 'tree-node') return true; // Выбираем любой
    return props.entityType === item.type; // Выбираем 'node' или 'group'
};

const selectItem = (item) => {
    selectedItem.value = item;
    let emittedValue = null;
    if (props.entityType === 'node') emittedValue = item.id;
    else if (props.entityType === 'group') emittedValue = item.id;
    else if (props.entityType === 'tree-node') emittedValue = item.treeNodeId;
    
    emit('update:modelValue', emittedValue);
    isOpen.value = false;
};

const toggleDropdown = () => {
    if (isOpen.value) {
        isOpen.value = false;
    } else {
        fetchRootItems();
        isOpen.value = true;
    }
};

// Загрузка инфо о выбранном элементе при инициализации
const loadInitialItem = async (id) => {
    if (!id) {
        selectedItem.value = null;
        return;
    }
    
    let itemType = props.entityType;
    let endpointId = id;

    if (itemType === 'tree-node') {
        // Если тип 'tree-node', нам нужно узнать, 'node' это или 'group'
        try {
            const nodeRes = await apiClient.get(`/node/by-tree-node/${id}`);
            selectedItem.value = { ...nodeRes.data, type: 'node', id: nodeRes.data.nodeId };
            return;
        } catch (e) {
             try {
                const groupRes = await apiClient.get(`/group/by-tree-node/${id}`);
                selectedItem.value = { ...groupRes.data, type: 'group', id: groupRes.data.groupId };
                return;
             } catch (e2) {
                 error.value = "Не вдалося завантажити обраний елемент.";
                 selectedItem.value = null;
             }
        }
    } else {
        // Если тип 'node' или 'group', ID - это ID самого 'node' или 'group'
         try {
            const res = await apiClient.get(`/${itemType}/${endpointId}`);
            selectedItem.value = { ...res.data, type: itemType };
         } catch(e) {
            console.error("Failed to load initial entity", e);
            selectedItem.value = null;
         }
    }
};

watch(() => props.modelValue, (newVal, oldVal) => {
    if (newVal !== oldVal) {
        loadInitialItem(newVal);
    }
}, { immediate: true });

</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";
.entity-selector-wrapper { width: 100%; }
.form-label { 
    display: block; 
    margin-bottom: 0.5rem; 
    font-family: $form-font;
    font-size: 1rem;
    color: var(--text-color);
}
.entity-selector { position: relative; }
.selected-display {
    display: flex; justify-content: space-between; align-items: center;
    height: 50px; padding: 0 1rem;
    border: 1px solid var(--secondary-color); border-radius: 3px;
    background-color: var(--main-color); cursor: pointer;
    transition: border-color 0.2s ease;
    
    .placeholder { color: var(--secondary-color); font-family: $form-font; }
    span { font-family: $form-font; }
    
    // --- СТИЛИ ВАЛИДАЦИИ ---
    &.invalid {
      border-color: var(--error-color);
    }
    // --- КОНЕЦ СТИЛЕЙ ВАЛИДАЦИИ ---
}
.dropdown {
    position: absolute; top: 105%; left: 0; width: 100%;
    z-index: 100; background-color: var(--main-color);
    border: 1px solid var(--secondary-color); border-radius: 3px;
    box-shadow: var(--strong-box-shadow);
}
.header {
    display: flex; align-items: center; padding: 5px 10px;
    border-bottom: 1px solid var(--secondary-color); gap: 10px;
}
.nav-btn {
    padding: 8px 10px; background-color: var(--weak-color);
    border: none; border-radius: 3px; cursor: pointer;
    &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.breadcrumbs {
    font-family: $form-font; font-size: 0.9em;
    white-space: nowrap; overflow-x: auto;
    .crumb { cursor: pointer; padding: 5px; &:hover { text-decoration: underline; } }
}
.content {
    max-height: 250px; overflow-y: auto; padding: 10px;
}
.preloader-container, .empty-message, .error-message {
    padding: 40px 20px; text-align: center; color: var(--secondary-color);
    font-family: $form-font;
}
.error-message { color: var(--error-color); }
// --- СТИЛИ ВАЛИДАЦИИ ---
.validation-error {
  font-family: $form-font;
  color: var(--error-color);
  font-size: 0.9em;
  margin-top: 5px;
  padding: 0;
  text-align: left;
}
// --- КОНЕЦ СТИЛЕЙ ВАЛИДАЦИИ ---

.item-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 8px 10px; border-radius: 3px;
    font-family: $form-font;
    &:hover { background-color: var(--weak-color-hover); }
}
.item-info {
    display: flex; align-items: center; gap: 10px;
    flex-grow: 1;
    &.navigable { cursor: pointer; }
    i { color: var(--secondary-color); }
}
.select-btn {
    padding: 5px 10px; border: none; background-color: var(--info-color);
    color: white; border-radius: 3px; cursor: pointer;
    font-family: $form-font;
    font-weight: 600;
}
</style>
