<template>
  <div class="segmented-control">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="control-button"
      :class="{ active: modelValue === option.value }"
      @click="selectOption(option.value)"
    >
      <i v-if="option.icon" :class="option.icon"></i>
      <span>{{ option.label }}</span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: [String, Number],
    required: true,
  },
  options: {
    type: Array,
    required: true,
    // Ожидаемый формат: [{ label: 'Text', value: 'val', icon: 'fa-solid fa-icon' }]
  },
});

const emit = defineEmits(['update:modelValue']);

const selectOption = (value) => {
  emit('update:modelValue', value);
};
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.segmented-control {
  display: flex;
  width: 100%;
  border: 1px solid var(--secondary-color);
  border-radius: 5px;
  overflow: hidden;
}

.control-button {
  flex: 1 1 0;
  padding: 12px 10px;
  background-color: var(--main-color);
  border: none;
  border-right: 1px solid var(--secondary-color);
  color: var(--secondary-color);
  font-family: $form-font;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 0.9em;

  &:last-child {
    border-right: none;
  }

  i {
    font-size: 0.95em;
    color: var(--secondary-color);
    transition: color 0.2s ease-in-out;
  }

  &:hover {
    background-color: var(--weak-color);
    color: var(--text-color);
  }

  &.active {
    background-color: var(--info-color);
    &, > * {
      color: white;
    }
    box-shadow: inset 0 2px 4px rgba(0,0,0,0.1);

    i {
      color: white;
    }
  }
}
</style>
