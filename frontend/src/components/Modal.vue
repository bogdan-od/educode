<template>
  <Transition name="modal-backdrop" appear>
    <div 
      v-if="visible" 
      class="modal-backdrop" 
      :class="theme"
      @click="handleBackdropClick"
    >
      <Transition name="modal-content" appear>
        <div 
          class="modal-content" 
          :class="[modalType, theme]"
          @click.stop
        >
          <!-- Header -->
          <div class="modal-header">
            <div class="modal-icon">
              <i :class="iconClass"></i>
            </div>
            <h3 class="modal-title">{{ title }}</h3>
            <button 
              v-if="showCloseButton" 
              class="modal-close-btn"
              @click="closeModal"
            >
              ×
            </button>
          </div>

          <!-- Body -->
          <div v-if="text || $slots.default" class="modal-body">
            <pre v-if="text" class="modal-text">{{ text }}</pre>
            <slot />
          </div>

          <!-- Footer -->
          <div class="modal-footer">
            <button 
              v-if="showCancelButton"
              class="modal-btn modal-btn-cancel"
              @click="handleCancel"
            >
              {{ cancelButtonText }}
            </button>
            <button 
              class="modal-btn modal-btn-primary"
              :class="`modal-btn-${modalType}`"
              @click="handleConfirm"
            >
              {{ confirmButtonText }}
            </button>
          </div>
        </div>
      </Transition>
    </div>
  </Transition>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'Modal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    type: {
      type: String,
      default: 'info',
      validator: (value) => ['success', 'error', 'warning', 'info', 'question'].includes(value)
    },
    title: {
      type: String,
      required: true
    },
    text: {
      type: String,
      default: ''
    },
    confirmButtonText: {
      type: String,
      default: 'OK'
    },
    cancelButtonText: {
      type: String,
      default: 'Скасувати'
    },
    showCancelButton: {
      type: Boolean,
      default: false
    },
    showCloseButton: {
      type: Boolean,
      default: true
    },
    closeOnBackdrop: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:visible', 'confirm', 'cancel', 'close'],
  setup(props, { emit }) {
    const store = useStore()
    
    const theme = computed(() => store.getters.getTheme)
    const modalType = computed(() => props.type)
    
    const iconClasses = {
      success: 'fa-solid fa-circle-check',
      error: 'fa-solid fa-circle-exclamation',
      warning: 'fa-solid fa-triangle-exclamation',
      info: 'fa-solid fa-circle-info',
      question: 'fa-solid fa-circle-question'
    }
    
    const iconClass = computed(() => iconClasses[props.type])
    
    const closeModal = () => {
      emit('update:visible', false)
      emit('close')
    }
    
    const handleConfirm = () => {
      emit('confirm')
      closeModal()
    }
    
    const handleCancel = () => {
      emit('cancel')
      closeModal()
    }
    
    const handleBackdropClick = () => {
      if (props.closeOnBackdrop) {
        closeModal()
      }
    }
    
    return {
      theme,
      modalType,
      iconClass,
      closeModal,
      handleConfirm,
      handleCancel,
      handleBackdropClick
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  
  &.dark {
    background-color: rgba(0, 0, 0, 0.7);
  }
}

.modal-content {
  background: var(--main-color);
  border-radius: 12px;
  box-shadow: var(--strong-box-shadow);
  max-width: 90vw;
  max-height: 90vh;
  width: 100%;
  max-width: 480px;
  overflow: hidden;
  border: 1px solid var(--weak-color);
  overflow-y: auto;
}

.modal-header {
  display: flex;
  align-items: center;
  padding: 24px 24px 16px;
  position: relative;
  
  .modal-icon {
    width: 24px;
    height: 24px;
    margin-right: 12px;
    flex-shrink: 0;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .modal-title {
    font-family: $secondary-font;
    font-size: 20px;
    font-weight: 600;
    color: var(--text-color);
    margin: 0;
    flex: 1;
  }
  
  .modal-close-btn {
    position: absolute;
    top: 16px;
    right: 16px;
    background: none;
    border: none;
    font-size: 24px;
    color: var(--secondary-color);
    cursor: pointer;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    transition: all 0.2s ease;
    
    &:hover {
      background-color: rgba(var(--button-bg-cover), 0.1);
      color: var(--text-color);
    }
  }
}

.modal-body {
  padding: 0 24px 24px;
  
  .modal-text {
    font-family: $form-font;
    font-size: 16px;
    line-height: 1.5;
    color: var(--text-color);
    margin: 0;
    text-wrap: wrap;
  }
}

.modal-footer {
  padding: 0 24px 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.modal-btn {
  font-family: $form-font;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 80px;
  
  &.modal-btn-cancel {
    background-color: rgba(var(--button-bg-cover), 0.1);
    color: var(--secondary-color);
    border: 1px solid var(--weak-color);
    
    &:hover {
      background-color: rgba(var(--button-bg-cover), 0.2);
      color: var(--text-color);
    }
  }
  
  &.modal-btn-primary {
    color: white;
    font-weight: 600;
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: var(--weak-box-shadow);
    }
  }
}

// Цветовые схемы для разных типов
.modal-content {
  &.success {
    .modal-icon {
      color: #22c55e;
      background-color: rgba(34, 197, 94, 0.1);
      border-radius: 50%;
      padding: 6px;

      i {
        color: #22c55e;
      }
    }
    
    .modal-btn-success {
      background-color: #22c55e;
      
      &:hover {
        background-color: #16a34a;
      }
    }
  }
  
  &.error {
    .modal-icon {
      color: #ef4444;
      background-color: rgba(239, 68, 68, 0.1);
      border-radius: 50%;
      padding: 6px;

      i {
        color: #ef4444;
      }
    }
    
    .modal-btn-error {
      background-color: #ef4444;
      
      &:hover {
        background-color: #dc2626;
      }
    }
  }
  
  &.warning {
    .modal-icon {
      color: #f59e0b;
      background-color: rgba(245, 158, 11, 0.1);
      border-radius: 50%;
      padding: 6px;

      i {
        color: #f59e0b;
      }
    }
    
    .modal-btn-warning {
      background-color: #f59e0b;
      
      &:hover {
        background-color: #d97706;
      }
    }
  }
  
  &.info {
    .modal-icon {
      color: #3b82f6;
      background-color: rgba(59, 130, 246, 0.1);
      border-radius: 50%;
      padding: 6px;

      i {
        color: #3b82f6;
      }
    }
    
    .modal-btn-info {
      background-color: #3b82f6;
      
      &:hover {
        background-color: #2563eb;
      }
    }
  }
  
  &.question {
    .modal-icon {
      color: #8b5cf6;
      background-color: rgba(139, 92, 246, 0.1);
      border-radius: 50%;
      padding: 6px;

      i {
        color: #8b5cf6;
      }
    }
    
    .modal-btn-question {
      background-color: #8b5cf6;
      
      &:hover {
        background-color: #7c3aed;
      }
    }
  }
}

// Анимации
.modal-backdrop-enter-active,
.modal-backdrop-leave-active {
  transition: opacity 0.3s ease;
}

.modal-backdrop-enter-from,
.modal-backdrop-leave-to {
  opacity: 0;
}

.modal-content-enter-active {
  transition: all 0.3s ease;
}

.modal-content-leave-active {
  transition: all 0.2s ease;
}

.modal-content-enter-from {
  opacity: 0;
  transform: scale(0.9) translateY(-20px);
}

.modal-content-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-10px);
}

// Адаптивность
@media (max-width: 600px) {
  .modal-content {
    margin: 16px;
    max-width: calc(100vw - 32px);
  }
  
  .modal-header {
    padding: 20px 20px 12px;
    
    .modal-title {
      font-size: 18px;
    }
  }
  
  .modal-body {
    padding: 0 20px 20px;
  }
  
  .modal-footer {
    padding: 0 20px 20px;
    flex-direction: column-reverse;
    
    .modal-btn {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>