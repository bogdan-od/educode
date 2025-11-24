<template>
  <div
    class="file-uploader"
    :class="{ 'is-dragover': isDragOver, 'is-invalid': error }"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
  >
    <label
      class="fu-control"
      :for="inputId"
      @keydown.enter.prevent="openPicker"
      @keydown.space.prevent="openPicker"
      tabindex="0"
      role="button"
      :aria-describedby="error ? errorId : null"
    >
      <input
        ref="input"
        :id="inputId"
        class="fu-input"
        type="file"
        :accept="accept"
        :multiple="multiple"
        @change="onChange"
        @focus="onFocus"
        @blur="onBlur"
        aria-hidden="true"
      />
      <div class="fu-meta">
        <span class="fu-button">{{ buttonText }}</span>
        <span class="fu-names" v-if="filesDisplay.length">{{ filesDisplay }}</span>
        <span class="fu-placeholder" v-else>{{ placeholder }}</span>
      </div>
    </label>

    <!-- previews -->
    <ul class="fu-previews" v-if="showPreviews && previews.length">
      <li v-for="p in previews" :key="p.id" class="fu-preview">
        <img v-if="p.type.startsWith('image/')" :src="p.url" :alt="p.name" />
        <div class="fu-preview-info">
          <div class="fu-preview-name">{{ p.name }}</div>
          <div class="fu-preview-size">{{ formatSize(p.size) }}</div>
        </div>
        <button type="button" class="fu-remove" @click="removeById(p.id)" aria-label="Remove file">✕</button>
      </li>
    </ul>

    <p class="fu-error" v-if="error" :id="errorId" role="alert">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue';

const props = defineProps({
  modelValue: { type: [File, Array, null], default: null }, // v-model: File | File[] | null
  multiple: { type: Boolean, default: false },
  accept: { type: String, default: '' },
  maxSize: { type: Number, default: Infinity }, // bytes
  maxFiles: { type: Number, default: Infinity },
  placeholder: { type: String, default: 'No file chosen' },
  buttonText: { type: String, default: 'Choose file' },
  showPreviews: { type: Boolean, default: true },
  id: { type: String, default: null }
});

const emit = defineEmits(['update:modelValue', 'change', 'error']);

const input = ref(null);
const isDragOver = ref(false);
const error = ref('');
const previews = ref([]);
const nextId = ref(1);

// Генерация уникального ID для input
const inputId = computed(() => {
  if (props.id) return props.id;
  return `fu_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
});

const errorId = computed(() => `${inputId.value}_error`);

function clearPreviews() {
  previews.value.forEach(p => {
    if (p.url && p.url.startsWith('blob:')) {
      URL.revokeObjectURL(p.url);
    }
  });
  previews.value = [];
}

onUnmounted(clearPreviews);

// Отслеживание изменений modelValue
watch(() => props.modelValue, (newValue) => {
  clearPreviews();
  if (!newValue) return;
  
  const files = Array.isArray(newValue) ? newValue : [newValue];
  files.forEach(file => {
    if (file instanceof File) {
      makePreview(file);
    }
  });
}, { immediate: true });

// Отображение имен файлов
const filesDisplay = computed(() => {
  const val = props.modelValue;
  if (!val) return '';
  
  if (Array.isArray(val)) {
    return val.map(f => f.name).join(', ');
  }
  
  return val.name || '';
});

function formatSize(bytes) {
  if (bytes === 0) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
}

function makePreview(file) {
    if (file == null) return;

  const id = nextId.value++;
  const preview = { 
    id, 
    name: file.name, 
    size: file.size, 
    type: file.type, 
    url: '' 
  };
  
  if (props.showPreviews && file.type.startsWith('image/')) {
    preview.url = URL.createObjectURL(file);
  }
  
  previews.value.push(preview);
  return preview;
}

function validateFiles(fileList) {
  error.value = '';
  
  if (!fileList || fileList.length === 0) {
    return { ok: true, files: [] };
  }
  
  const files = Array.from(fileList);
  
  // Проверка количества файлов
  if (!props.multiple && files.length > 1) {
    error.value = 'Можна вибрати лише один файл';
    emit('error', error.value);
    return { ok: false };
  }
  
  if (files.length > props.maxFiles) {
    error.value = `Максимум ${props.maxFiles} файлів`;
    emit('error', error.value);
    return { ok: false };
  }

  // Проверка каждого файла
  for (const file of files) {
    // Проверка типа файла
    if (props.accept && props.accept.trim()) {
      const acceptedTypes = props.accept.split(',').map(s => s.trim()).filter(Boolean);
      const isValidType = acceptedTypes.some(acceptType => {
        if (acceptType.startsWith('.')) {
          return file.name.toLowerCase().endsWith(acceptType.toLowerCase());
        }
        if (acceptType.endsWith('/*')) {
          const baseType = acceptType.split('/')[0];
          return file.type.split('/')[0] === baseType;
        }
        return file.type === acceptType;
      });
      
      if (!isValidType) {
        error.value = `Неприпустимий тип файлу: ${file.name}`;
        emit('error', error.value);
        return { ok: false };
      }
    }
    
    // Проверка размера файла
    if (file.size > props.maxSize) {
      error.value = `Файл "${file.name}" занадто великий (${formatSize(file.size)})`;
      emit('error', error.value);
      return { ok: false };
    }
  }

  return { ok: true, files };
}

function setFiles(files) {
  const newValue = props.multiple ? files : (files[0] || null);
  
  emit('update:modelValue', newValue);
  emit('change', newValue);
  
  // Обновление превью
  clearPreviews();
  if (files && files.length > 0) {
    files.forEach(file => makePreview(file));
  }
}

function onChange(e) {
  const inputEl = e.target;
  const validation = validateFiles(inputEl.files);
  
  if (!validation.ok) {
    // Очищаем input для возможности повторного выбора того же файла
    inputEl.value = '';
    if (props.multiple) {
      setFiles([]);
    } else {
      setFiles([null]);
    }
    return;
  }
  
  setFiles(validation.files);
}

function onDragOver(e) {
  e.preventDefault();
  isDragOver.value = true;
}

function onDragLeave(e) {
  e.preventDefault();
  // Проверяем, что мы действительно покинули область
  if (!e.currentTarget.contains(e.relatedTarget)) {
    isDragOver.value = false;
  }
}

function onDrop(e) {
  e.preventDefault();
  isDragOver.value = false;
  
  const dt = e.dataTransfer;
  if (!dt || !dt.files) return;
  
  const validation = validateFiles(dt.files);
  if (!validation.ok) return;
  
  setFiles(validation.files);
  
  // Обновляем input элемент
  nextTick(() => {
    if (input.value) {
      input.value.value = '';
    }
  });
}

function openPicker() {
  if (input.value) {
    input.value.click();
  }
}

function onFocus() {
  // Можно добавить логику для стилей фокуса
}

function onBlur() {
  // Можно добавить логику для потери фокуса
}

function removeById(id) {
  const previewIndex = previews.value.findIndex(p => p.id === id);
  if (previewIndex === -1) return;
  
  // Удаляем превью и освобождаем URL
  const [removedPreview] = previews.value.splice(previewIndex, 1);
  if (removedPreview && removedPreview.url && removedPreview.url.startsWith('blob:')) {
    URL.revokeObjectURL(removedPreview.url);
  }
  
  const currentValue = props.modelValue;
  if (!currentValue) return;
  
  if (!Array.isArray(currentValue)) {
    // Единственный файл - очищаем полностью
    emit('update:modelValue', null);
    emit('change', null);
    
    // Очищаем input
    if (input.value) {
      input.value.value = '';
    }
    return;
  }
  
  // Множественные файлы - удаляем конкретный
  const newFiles = currentValue.slice();
  newFiles.splice(previewIndex, 1);
  
  emit('update:modelValue', newFiles);
  emit('change', newFiles);
}
</script>

<style lang="scss" scoped>
.file-uploader { 
  font-family: inherit; 
  width: 100%;

  // Переменные для тем
  --accent: #0b74de;
  --error-color: #ef4444;
  --success-color: #10b981;
}

// Светлая тема
:global(body.light) .file-uploader {
  --fu-bg: rgba(var(--button-bg-cover), 0.5);
  --fu-border: var(--weak-color);
  --fu-text: var(--text-color);
  --fu-secondary: var(--secondary-color);
  --fu-preview-bg: rgba(var(--button-bg-cover), 1);
  --fu-preview-border: var(--weak-color);
  --fu-shadow: var(--weak-box-shadow);
  --fu-dragover-bg: rgba(11, 116, 222, 0.05);
}

// Темная тема  
:global(body.dark) .file-uploader {
  --fu-bg: rgba(var(--button-bg-cover), 0.3);
  --fu-border: var(--weak-color);
  --fu-text: var(--text-color);
  --fu-secondary: var(--secondary-color);
  --fu-preview-bg: rgba(var(--button-bg-cover), 0.5);
  --fu-preview-border: var(--weak-color);
  --fu-shadow: var(--weak-box-shadow);
  --fu-dragover-bg: rgba(11, 116, 222, 0.1);
}

.fu-control {
  display: inline-flex; 
  align-items: center; 
  gap: 0.5rem;
  padding: 0.5rem 0.75rem; 
  border: 1px solid var(--fu-border); 
  border-radius: 8px; 
  background: var(--fu-bg);
  cursor: pointer; 
  user-select: none; 
  outline: none;
  transition: all 0.2s ease;
  width: 100%;
  min-height: 44px;

  &:hover {
    border-color: var(--accent);
    box-shadow: var(--fu-shadow);
  }

  &:focus {
    box-shadow: 0 0 0 3px rgba(11, 116, 222, 0.15);
    border-color: var(--accent);
  }
}

.fu-input { 
  position: absolute; 
  left: -9999px;
  width: 1px;
  height: 1px;
  opacity: 0; 
}

.fu-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.fu-button { 
  background: var(--accent); 
  color: #fff; 
  padding: 0.25rem 0.5rem; 
  border-radius: 6px; 
  font-size: 0.9rem;
  flex-shrink: 0;
  font-weight: 500;
  transition: background-color 0.2s ease;

  &:hover {
    background: rgba(11, 116, 222, 0.9);
  }
}

.fu-placeholder { 
  color: var(--fu-secondary); 
  font-size: 0.9rem; 
}

.fu-names { 
  font-size: 0.9rem; 
  color: var(--fu-text); 
  flex: 1;
  overflow: hidden; 
  text-overflow: ellipsis; 
  white-space: nowrap; 
}

.is-dragover .fu-control { 
  border-color: var(--accent); 
  box-shadow: 0 6px 18px rgba(11, 116, 222, 0.15);
  background: var(--fu-dragover-bg);
  transform: scale(1.02);
}

.is-invalid .fu-control {
  border-color: var(--error-color);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.fu-previews { 
  margin: 0.75rem 0 0 0; 
  padding: 0; 
  list-style: none; 
  display: flex; 
  gap: 0.5rem; 
  flex-wrap: wrap; 
}

.fu-preview { 
  display: flex; 
  align-items: center; 
  gap: 0.75rem; 
  background: var(--fu-preview-bg); 
  border: 1px solid var(--fu-preview-border); 
  padding: 0.5rem; 
  border-radius: 8px;
  min-width: 0;
  box-shadow: var(--fu-shadow);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  img { 
    width: 48px; 
    height: 48px; 
    object-fit: cover; 
    border-radius: 6px;
    flex-shrink: 0;
    border: 1px solid var(--fu-preview-border);
  }
}

.fu-preview-info { 
  display: flex; 
  flex-direction: column;
  min-width: 0;
  flex: 1;
  gap: 0.25rem;
}

.fu-preview-name { 
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--fu-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fu-preview-size { 
  font-size: 0.75rem; 
  color: var(--fu-secondary);
  font-weight: 400;
}

.fu-remove { 
  background: transparent; 
  border: none; 
  color: var(--fu-secondary); 
  cursor: pointer; 
  padding: 0.25rem; 
  font-size: 1rem;
  line-height: 1;
  flex-shrink: 0;
  border-radius: 4px;
  transition: all 0.2s ease;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover { 
    color: var(--error-color); 
    background: rgba(239, 68, 68, 0.1);
    transform: scale(1.1);
  }

  &:focus {
    outline: 2px solid var(--error-color);
    outline-offset: 2px;
  }
}

.fu-error { 
  color: var(--error-color); 
  margin-top: 0.5rem; 
  font-size: 0.875rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.25rem;

  &::before {
    content: '⚠';
    font-size: 1rem;
  }
}

// Анимации
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.is-invalid {
  animation: shake 0.3s ease-in-out;
}

// Адаптивность
@media (max-width: 768px) {
  .fu-control {
    padding: 0.75rem;
    min-height: 48px;
  }

  .fu-preview {
    padding: 0.75rem;
    
    img {
      width: 40px;
      height: 40px;
    }
  }

  .fu-preview-name {
    font-size: 0.8rem;
  }

  .fu-preview-size {
    font-size: 0.7rem;
  }
}
</style>