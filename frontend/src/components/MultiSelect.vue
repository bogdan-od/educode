<template>
  <div class="multi-select">
    <label v-if="label" class="form-label">{{ label }}</label>
    <div class="select-container" ref="container">
      <!-- ДОБАВЛЕНО: .invalid класс -->
      <div class="selected-items" @click="toggleDropdown" :class="{ 'invalid': invalid }">
        <span v-if="selectedOptions.length === 0" class="placeholder">{{ placeholder }}</span>
        <span v-for="option in selectedOptions" :key="option[valueKey]" class="selected-item">
          {{ option[labelKey] }}
          <button @click.stop="removeItem(option)">&times;</button>
        </span>
      </div>
      <div v-if="isOpen" class="dropdown">
        <div v-for="option in options" :key="option[valueKey]" 
             class="option" 
             :class="{ selected: isSelected(option), disabled: option.disabled }"
             @click="!option.disabled && toggleOption(option)"
             :title="option.disabled ? 'У вас недостатньо прав, щоб призначити цю роль' : option.description">
          {{ option[labelKey] }}
        </div>
      </div>
    </div>
    <!-- ДОБАВЛЕНО: Сообщение об ошибке -->
    <div v-if="invalid && errorMessage" class="error-message">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { onClickOutside } from '@vueuse/core';

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  options: { type: Array, required: true },
  label: String,
  placeholder: { type: String, default: 'Виберіть...' },
  labelKey: { type: String, default: 'label' }, // Исправлено на 'label'
  valueKey: { type: String, default: 'value' }, // Исправлено на 'value'
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
const container = ref(null);

onClickOutside(container, () => isOpen.value = false);

const selectedOptions = computed(() => 
  props.options.filter(opt => props.modelValue.includes(opt[props.valueKey]))
);

const isSelected = (option) => props.modelValue.includes(option[props.valueKey]);
const toggleDropdown = () => { isOpen.value = !isOpen.value; };

const toggleOption = (option) => {
  const selectedValues = [...props.modelValue];
  const index = selectedValues.indexOf(option[props.valueKey]);

  if (index > -1) {
    selectedValues.splice(index, 1);
  } else {
    selectedValues.push(option[props.valueKey]);
  }
  emit('update:modelValue', selectedValues);
};

const removeItem = (option) => {
  const selectedValues = props.modelValue.filter(val => val !== option[props.valueKey]);
  emit('update:modelValue', selectedValues);
};
</script>

<style scoped lang="scss">
@import "@/assets/scss/variables.scss";

.multi-select { 
    width: 100%; 
    font-family: $form-font;
}
.form-label { 
    display: block; 
    margin-bottom: 0.5rem; 
    font-weight: 600;
    font-size: 0.9em;
}
.select-container { 
    position: relative; 
}
.selected-items {
  display: flex; flex-wrap: wrap; gap: 0.5rem;
  min-height: 50px; padding: 0.5rem;
  border: 1px solid var(--secondary-color); border-radius: 3px;
  cursor: pointer; background-color: var(--main-color);
  transition: border-color 0.2s ease;

  // --- СТИЛИ ВАЛИДАЦИИ ---
  &.invalid {
    border-color: var(--error-color);
  }
  // --- КОНЕЦ СТИЛЕЙ ---
}
.placeholder { 
    color: var(--secondary-color); 
    align-self: center; 
    padding-left: 0.5rem; 
}
.selected-item {
  display: flex; align-items: center; gap: 0.5rem;
  background-color: var(--info-color); 
  color: white;
  padding: 0.25rem 0.5rem; border-radius: 3px;
  font-size: 0.9em;
  
  button { 
      background: none; 
      border: none; 
      color: white; 
      cursor: pointer; 
      font-size: 1.2em; 
      padding: 0;
      line-height: 1;
      
      &:hover {
          opacity: 0.7;
      }
  }
}
.dropdown {
  position: absolute; top: 105%; left: 0; width: 100%;
  max-height: 200px; overflow-y: auto; z-index: 100;
  background-color: var(--main-color); border: 1px solid var(--secondary-color);
  border-radius: 3px; box-shadow: var(--strong-box-shadow);
}
.option {
  padding: 0.75rem 1rem; cursor: pointer;
  transition: background-color 0.2s ease;
  
  &:hover { background-color: var(--weak-color); }
  &.selected { background-color: var(--info-color); color: white; }
  &.disabled { 
      background-color: var(--main-color); 
      color: var(--secondary-color); 
      cursor: not-allowed; 
      opacity: 0.6;
  }
}

// --- СТИЛИ ВАЛИДАЦИИ ---
.error-message {
  font-family: $form-font;
  color: var(--error-color);
  font-size: 0.9em;
  margin-top: 5px;
}
// --- КОНЕЦ СТИЛЕЙ ---
</style>
