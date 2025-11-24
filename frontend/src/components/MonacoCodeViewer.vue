<template>
    <div class="code-viewer">
        <div v-if="showHeader" class="viewer-header">
            <div class="language-badge">
                <i class="fa-solid fa-code"></i>
                {{ displayText }}
            </div>
            <button v-if="showCopyButton" @click="copyCode" class="copy-btn" :class="{ copied: isCopied }">
                <i :class="isCopied ? 'fa-solid fa-check' : 'fa-regular fa-copy'"></i>
                {{ isCopied ? 'Скопійовано!' : 'Копіювати' }}
            </button>
        </div>
        
        <div ref="viewerContainer" class="monaco-viewer" :class="{ loading: isLoading }">
            <Preloader v-if="isLoading" />
        </div>
    </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import { useStore } from 'vuex'
import Preloader from '@/components/Preloader.vue'
import { getMonacoEditorLanguage, getDisplayLanguageText, loadProgrammingLanguages } from '@/services/programmingLanguageService'

const props = defineProps({
    code: {
        type: String,
        required: true,
        default: ''
    },
    language: {
        type: String,
        default: 'plaintext'
    },
    height: {
        type: String,
        default: '400px'
    },
    showHeader: {
        type: Boolean,
        default: true
    },
    showCopyButton: {
        type: Boolean,
        default: true
    },
    minimap: {
        type: Boolean,
        default: false
    },
    lineNumbers: {
        type: Boolean,
        default: true
    }
})

const store = useStore()
const viewerContainer = ref(null)
const isLoading = ref(true)
const isCopied = ref(false)

let editor = null
let monaco = null

const themeMode = computed(() => store.getters.getThemeMode)
const displayText = computed(() => getDisplayLanguageText(props.language))
const monacoLanguage = computed(() => getMonacoEditorLanguage(props.language))

const initializeMonaco = async () => {
    if (!viewerContainer.value) return

    if (monaco == null) {
        monaco = await import('monaco-editor')
    }
    
    isLoading.value = false

    if (editor != null) {
        editor.dispose()
        editor = null
    }

    editor = monaco.editor.create(viewerContainer.value, {
        value: props.code,
        language: monacoLanguage.value,
        theme: themeMode.value === 'dark' ? 'vs-dark' : 'vs',
        automaticLayout: true,
        readOnly: true, // Заборона редагування
        minimap: { enabled: props.minimap },
        scrollBeyondLastLine: false,
        wordWrap: 'on',
        fontSize: 14,
        fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', 'Monaco', 'Courier New', monospace",
        fontLigatures: true,
        lineNumbers: props.lineNumbers ? 'on' : 'off',
        roundedSelection: false,
        scrollbar: {
            vertical: 'auto',
            horizontal: 'auto',
            verticalScrollbarSize: 10,
            horizontalScrollbarSize: 10
        },
        folding: true,
        lineNumbersMinChars: 3,
        glyphMargin: false,
        overviewRulerLanes: 0,
        hideCursorInOverviewRuler: true,
        overviewRulerBorder: false,
        renderLineHighlight: 'none', // Вимикаємо підсвічування рядка
        contextmenu: false,
        quickSuggestions: false,
        suggestOnTriggerCharacters: false,
        acceptSuggestionOnEnter: 'off',
        tabCompletion: 'off',
        wordBasedSuggestions: false,
        parameterHints: { enabled: false },
        hover: { enabled: false },
        links: false,
        colorDecorators: false,
        cursorStyle: 'line-thin', // Тонкий курсор
        cursorBlinking: 'solid', // Курсор не блимає
        selectionHighlight: false,
        occurrencesHighlight: false,
        renderWhitespace: 'none'
    })
}

const copyCode = async () => {
    try {
        await navigator.clipboard.writeText(props.code)
        isCopied.value = true
        setTimeout(() => {
            isCopied.value = false
        }, 2000)
    } catch (err) {
        console.error('Помилка копіювання:', err)
    }
}

watch(() => props.code, (newCode) => {
    if (editor && newCode !== editor.getValue()) {
        editor.setValue(newCode)
    }
})

watch(() => props.language, async (newLanguage) => {
    if (editor && monaco) {
        const model = editor.getModel()
        const language = getMonacoEditorLanguage(newLanguage)
        monaco.editor.setModelLanguage(model, language)
    }
})

watch(themeMode, () => {
    if (editor) {
        editor.dispose()
        initializeMonaco()
    }
})

onMounted(async () => {
    await loadProgrammingLanguages()
    initializeMonaco()
})

onBeforeUnmount(() => {
    if (editor) {
        editor.dispose()
    }
})
</script>

<style lang="scss" scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&display=swap');

.code-viewer {
    width: 100%;
    border: 1px solid var(--border-color, #ccc);
    border-radius: 8px;
    overflow: hidden;
    background-color: var(--weak-color);
}

.viewer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background-color: var(--weak-color);
    border-bottom: 1px solid var(--secondary-color);
    
    .language-badge {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 500;
        color: var(--text-color);
        
        i {
            opacity: 0.7;
        }
    }
    
    .copy-btn {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 5px 12px;
        border: none;
        border-radius: 4px;
        background-color: rgba(var(--button-bg-cover), 0.3);
        color: var(--text-color);
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
            background-color: rgba(var(--button-bg-cover), 0.5);
        }
        
        &.copied {
            background-color: rgba(76, 175, 80, 0.3);
            
            &:hover {
                background-color: rgba(76, 175, 80, 0.4);
            }
        }
        
        i {
            font-size: 12px;
        }
    }
}

.monaco-viewer {
    position: relative;
    height: v-bind(height);
    
    &.loading {
        display: flex;
        align-items: center;
        justify-content: center;
    }
    
    :deep(.monaco-editor) {
        width: 100%;
        height: 100%;
    }
}

.monaco-viewer :deep(.monaco-editor) :not(.codicon[class*="codicon-"]) {
    font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'SF Mono', 'Consolas', 'Monaco', 'Liberation Mono', 'Courier New', monospace;
    font-feature-settings: 'liga', 'calt';
    font-variant-ligatures: common-ligatures;
    font-size: 14px;
}

.monaco-viewer :deep(.line-numbers) {
    color: var(--secondary-color) !important;
    opacity: 0.6;
}

.monaco-viewer :deep(.monaco-scrollable-element > .scrollbar) {
    background-color: transparent !important;
}

.monaco-viewer :deep(.monaco-scrollable-element > .scrollbar > .slider) {
    background-color: rgba(128, 128, 128, 0.3) !important;
    border-radius: 4px;
}

.monaco-viewer :deep(.monaco-scrollable-element > .scrollbar > .slider:hover) {
    background-color: rgba(128, 128, 128, 0.5) !important;
}

.monaco-viewer :deep(.monaco-editor .cursors-layer) {
    display: none !important;
}
</style>
