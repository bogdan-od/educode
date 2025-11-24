<template>
  <div class="api-search-selector">
    <label v-if="label" class="form-label">{{ label }}</label>
    
    <!-- Контейнер для тегов (в режиме multiple) -->
    <div v-if="props.multiple && selectedTags.length > 0" class="tags-container">
      <span v-for="tag in selectedTags" :key="tag.id" class="tag">
        {{ tag.title }}
        <button @click.stop="removeTag(tag)" class="remove-tag-btn" type="button" title="Видалити">
          <i class="fa-solid fa-times"></i>
        </button>
      </span>
    </div>

    <div class="search-container" ref="searchContainer">
      <div class="input-wrapper" :class="{ 'invalid': props.invalid }">
        <i class="fa-solid fa-search search-icon"></i>
        <input
          type="text"
          :placeholder="placeholder"
          v-model="searchQuery"
          @input="onInput"
          @focus="isDropdownVisible = true"
        />
        <button 
          v-if="!props.multiple && localSingleSelectedId" 
          @click.stop="clearSelection" 
          class="clear-btn" 
          type="button" 
          title="Очистити вибір"
        >
            <i class="fa-solid fa-times"></i>
        </button>
      </div>
      <div v-if="isDropdownVisible && (isLoading || suggestions.length > 0)" class="suggestions-dropdown">
        <div v-if="isLoading" class="suggestion-item loading">
          <Preloader :scale="0.5" />
          <span>Пошук...</span>
        </div>
        <div
          v-for="suggestion in suggestions"
          :key="suggestion.id"
          class="suggestion-item"
          @click="selectSuggestion(suggestion)"
        >
          <p class="suggestion-title">{{ suggestion.title }}</p>
          <p v-if="suggestion.details" class="suggestion-details">{{ suggestion.details }}</p>
        </div>
      </div>
    </div>
    
    <!-- Сообщение об ошибке валидации -->
    <div v-if="props.invalid && props.errorMessage" class="error-message">
      {{ props.errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import { onClickOutside } from '@vueuse/core';

const props = defineProps({
  modelValue: {
    type: [String, Number, Array], // Может быть ID или массив ID
    default: null,
  },
  apiUrl: {
    type: String,
    required: true,
  },
  label: String,
  placeholder: {
    type: String,
    default: 'Почніть вводити для пошуку...',
  },
  minChars: {
    type: Number,
    default: 2,
  },
  initialText: {
    type: String,
    default: ''
  },
  // --- НОВЫЕ PROPS ---
  multiple: {
    type: Boolean,
    default: false,
  },
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

const searchQuery = ref('');
const localSingleSelectedId = ref(null);
const selectedTags = ref([]); // Для UI в режиме multiple
const suggestions = ref([]);
const isLoading = ref(false);
const isDropdownVisible = ref(false);
const debounceTimer = ref(null);
const searchContainer = ref(null);

onClickOutside(searchContainer, () => isDropdownVisible.value = false);

const onInput = () => {
  clearTimeout(debounceTimer.value);

  // Если не в режиме multiple, очищаем selection при вводе
  if (!props.multiple) {
    localSingleSelectedId.value = null;
    emit('update:modelValue', null);
  }

  if (searchQuery.value.length < props.minChars) {
    suggestions.value = [];
    return;
  }

  debounceTimer.value = setTimeout(() => {
    fetchSuggestions();
  }, 500);
};

const fetchSuggestions = async () => {
  isLoading.value = true;
  try {
    const response = await apiClient.get(props.apiUrl, {
      params: { q: searchQuery.value },
    });
    suggestions.value = response.data;
  } catch (error) {
    console.error('Error fetching suggestions:', error);
    suggestions.value = [];
  } finally {
    isLoading.value = false;
  }
};

const selectSuggestion = (suggestion) => {
  if (props.multiple) {
    // --- Логика для multiple ---
    const currentIds = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
    if (!currentIds.includes(suggestion.id)) {
      // Добавляем тег в UI
      selectedTags.value.push(suggestion);
      // Эмитим новый массив ID
      emit('update:modelValue', [...currentIds, suggestion.id]);
    }
    searchQuery.value = ''; // Очищаем поиск
    suggestions.value = [];
  } else {
    // --- Логика для single (как и была) ---
    searchQuery.value = suggestion.title;
    localSingleSelectedId.value = suggestion.id;
    emit('update:modelValue', suggestion.id);
    isDropdownVisible.value = false;
    suggestions.value = [];
  }
};

// --- НОВАЯ ФУНКЦИЯ ---
// Удаление тега в режиме multiple
const removeTag = (tagToRemove) => {
  selectedTags.value = selectedTags.value.filter(tag => tag.id !== tagToRemove.id);
  const currentIds = Array.isArray(props.modelValue) ? props.modelValue : [];
  const newIds = currentIds.filter(id => id !== tagToRemove.id);
  emit('update:modelValue', newIds);
};
// --- КОНЕЦ НОВОЙ ФУНКЦИИ ---

// --- ОБНОВЛЕННАЯ ФУНКЦИЯ ---
// Очистка в режиме single
const clearSelection = () => {
    searchQuery.value = '';
    localSingleSelectedId.value = null;
    emit('update:modelValue', null);
    suggestions.value = [];
};
// --- КОНЕЦ ОБНОВЛЕНИЯ ---


// --- ОБНОВЛЕННЫЙ WATCH ---
watch(() => props.modelValue, (newVal) => {
  if (props.multiple) {
    // В режиме multiple, если v-model очищается извне, чистим теги
    if (Array.isArray(newVal) && newVal.length === 0) {
      selectedTags.value = [];
    } else if (!newVal) {
      selectedTags.value = [];
    }
    // Прим: Мы не можем восстановить `title` тегов из `modelValue` (массива ID)
    // без доп. API-запроса, поэтому `selectedTags` отражает
    // только то, что было выбрано в *текущей сессии*.
  } else {
    // Логика для single-режима
    localSingleSelectedId.value = newVal;
    if (!newVal) {
      searchQuery.value = '';
    }
  }
});

// Синхронизация initialText (для single-режима)
watch(() => props.initialText, (newVal) => {
  if(newVal && !props.multiple) {
    searchQuery.value = newVal;
  }
}, { immediate: true });

// Инициализация searchQuery для single-режима, если есть modelValue
if (!props.multiple && props.modelValue && props.initialText) {
  searchQuery.value = props.initialText;
  localSingleSelectedId.value = props.modelValue;
}
// --- КОНЕЦ ОБНОВЛЕНИЯ WATCH ---

</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.api-search-selector {
  width: 100%;
  .form-label {
    display: block;
    margin-bottom: 0.5rem;
    font-family: $form-font;
    font-size: 1rem;
    color: var(--text-color);
  }
}

.search-container {
  position: relative;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;

  .search-icon {
    position: absolute;
    left: 15px;
    color: var(--secondary-color);
    z-index: 1;
  }
  
  input {
    width: 100%;
    height: 50px;
    border: 1px solid var(--secondary-color);
    background-color: var(--main-color);
    border-radius: 3px;
    padding: 0 45px 0 40px;
    font-family: $form-font;
    font-size: 1em;
    transition: border-color 0.2s ease;

    &:focus {
        outline: none;
        border-color: var(--info-color);
    }
  }
  
  // --- СТИЛИ ВАЛИДАЦИИ ---
  &.invalid {
    input {
      border-color: var(--error-color);
    }
    .search-icon {
      color: var(--error-color);
    }
  }
  // --- КОНЕЦ СТИЛЕЙ ВАЛИДАЦИИ ---

  .clear-btn {
    position: absolute;
    right: 15px;
    background: none;
    border: none;
    cursor: pointer;
    color: var(--secondary-color);
    font-size: 1.1em;
    padding: 0;
    &:hover {
        color: #f44336;
    }
  }
}

// --- СТИЛИ ТЕГОВ (MULTIPLE) ---
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: var(--weak-color);
  color: var(--text-color);
  padding: 5px 10px;
  border-radius: 3px;
  font-family: $form-font;
  font-size: 0.9em;
  border: 1px solid var(--secondary-color);
}
.remove-tag-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--secondary-color);
  padding: 0;
  display: inline-flex;
  &:hover {
    color: var(--error-color);
  }
  i { font-size: 0.9em; }
}
// --- КОНЕЦ СТИЛЕЙ ТЕГОВ ---


.suggestions-dropdown {
  position: absolute;
  top: 105%;
  left: 0;
  width: 100%;
  max-height: 250px;
  overflow-y: auto;
  background-color: var(--main-color);
  border: 1px solid var(--secondary-color);
  border-radius: 3px;
  z-index: 100;
  box-shadow: var(--strong-box-shadow);
}

.suggestion-item {
  padding: 10px 15px;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: var(--weak-color);
  }

  &.loading {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: default;
    justify-content: center;
  }

  .suggestion-title {
    font-weight: bold;
    margin: 0 0 5px 0;
  }
  .suggestion-details {
    font-size: 0.9em;
    color: var(--secondary-color);
    margin: 0;
  }
}

// --- СТИЛИ ВАЛИДАЦИИ ---
.error-message {
  font-family: $form-font;
  color: var(--error-color);
  font-size: 0.9em;
  margin-top: 5px;
}
// --- КОНЕЦ СТИЛЕЙ ВАЛИДАЦИИ ---
</style>
