<template>
    <div class="editor-container">
        <!-- Панель інструментів редактора -->
        <div class="editor-header">
            <div class="left">
                <!-- Вибір мови програмування -->
                <select @change="onLanguageChange">
                    <option v-for="lang in programmingLanguages" :key="lang.server_id" :value="lang.server_id" :selected="lang.server_id === selectedServerLanguage">
                        {{ lang.name }} ({{ lang.version }})
                    </option>
                </select>
                <!-- Завантаження файлу з кодом -->
                <input type="file" id="uploadCode" @change="onFileUpload" accept="*/*">
                <label for="uploadCode"><i class="fa-regular fa-folder-open"></i> Завантажити код з комп'ютеру</label>
            </div>
            <div class="right">
                <!-- Кнопки керування -->
                <button @click="executeCode" class="run-btn"><i class="fa-solid fa-person-running"></i> Запустити</button>
                <button @click="testCode" class="test-btn"><i class="fa-solid fa-microscope"></i> Тестувати</button>
            </div>
        </div>
        
        <!-- Monaco Editor контейнер -->
        <div ref="editorContainer" class="monaco-editor" :class="{'loading': monacoLoading}">
            <Preloader v-if="monacoLoading" />
        </div>

        <!-- Текстовий редактор коду -->
        <!-- <textarea 
            v-model="userCode" 
            spellcheck="false" 
            class="codeEditor" 
            id="codeEditor" 
            ref="editorContainer"
        ></textarea> -->
        
        <!-- Панель результатів -->
        <div class="panel-toggle">
            <!-- Перемикачі між різними режимами відображення -->
            <div class="toggle-buttons">
                <button @click="activePanel = 'input'" :class="{ active: activePanel === 'input' }">
                    <i class="fa-solid fa-keyboard"></i> Input
                </button>
                <button @click="activePanel = 'output'" :class="{ active: activePanel === 'output' }">
                    <i class="fa-solid fa-display"></i> Output
                </button>
                <button @click="activePanel = 'test'" :class="{ active: activePanel === 'test' }">
                    <i class="fa-solid fa-vial"></i> Test
                </button>
            </div>
            
            <!-- Вміст панелей -->
            <div class="panel-content">
                <!-- Панель вхідних даних -->
                <div v-if="activePanel === 'input'" class="input-panel">
                    <h4>Вхідні дані</h4>
                    <textarea v-model="inputData" placeholder="Вхідні дані..."></textarea>
                </div>
                <!-- Панель вихідних даних -->
                <div v-if="activePanel === 'output'" class="output-panel">
                    <h4>Вихідні дані</h4>
                    <pre>{{ outputData }}</pre>
                    <Preloader bg="var(--weak-color)" v-if="outputLoading"/>
                </div>
                <!-- Панель результатів тестування -->
                <div v-if="activePanel === 'test'" class="test-panel">
                    <h4>Тести</h4>
                    <div class="testOutputBlock">
                        <p v-for="(testOut, i) in testOutput" :key="i" class="testOutput">{{ testOut }}</p>
                    </div>
                    <Preloader bg="var(--weak-color)" v-if="testLoading"/>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import apiClient from '@/axios'
import Preloader from '@/components/Preloader.vue'

// Пропси
const props = defineProps({
    puzzleId: {
        type: [String, Number],
        required: true
    },
    homeworkId: {
        type: [String, Number],
        required: false
    },
    treeNodeId: {
        type: [String, Number],
        required: false
    }
})

// Емітери
const emit = defineEmits(['error', 'success'])

// Композабли
const store = useStore()
const route = useRoute()

// Реактивні дані
const activePanel = ref('input')
const outputData = ref('')
const selectedServerLanguage = ref('')
const inputData = ref('')
const testOutput = ref([])
const outputLoading = ref(false)
const testLoading = ref(false)
const editorContainer = ref(null)
const watchSelectedServerLanguage = ref(true)
const monacoLoading = ref(true);

// Monaco Editor
let editor = null

// Обчислювані властивості
const programmingLanguages = computed(() => store.getters.programmingLanguages)
const defaultCodeExamples = computed(() => store.getters.defaultCodeExamples)
const themeMode = computed(() => store.getters.getThemeMode)

// Получение Monaco языка по server_id
const getMonacoLanguage = (serverId) => {
    const language = programmingLanguages.value.find(lang => lang.server_id == serverId) || {monaco_editor_language: 'plaintext'}
    return language.monaco_editor_language|| 'plaintext'
}

let monaco = null;
// Инициализация Monaco Editor
const initializeMonaco = async () => {
    if (!editorContainer.value) return

    // Ленивая загрузка Monaco только при первом использовании
    if (monaco == null) {  
        monaco = await import('monaco-editor')
    }
    monacoLoading.value = false;

    if (editor != null)
        editor.dispose();
    editor = null;

    editor = monaco.editor.create(editorContainer.value, {
        value: '',
        language: 'plaintext',
        theme: themeMode.value == 'dark' ? 'vs-dark' : 'vs',
        automaticLayout: true,
        minimap: { enabled: false }, // Отключаем миникарту
        scrollBeyondLastLine: false,
        wordWrap: 'on',
        fontSize: 12,
        fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', 'Monaco', 'Courier New', monospace",
        fontLigatures: true, // Включаем лигатуры для современных моноширинных шрифтов
        lineNumbers: 'on',
        roundedSelection: false,
        scrollbar: {
            vertical: 'auto', // Показываем только при необходимости
            horizontal: 'auto'
        },
        folding: true,
        lineNumbersMinChars: 3,
        glyphMargin: false, // Убираем левый отступ
        overviewRulerLanes: 0,
        hideCursorInOverviewRuler: true,
        overviewRulerBorder: false,
        renderLineHighlight: 'line', // Упрощенная подсветка строки
        contextmenu: false, // Отключаем контекстное меню (можно включить если нужно)
        quickSuggestions: {
            other: true,
            comments: false,
            strings: false
        }
    })

    // Обработчик изменений
    editor.onDidChangeModelContent(() => {
        updateEditorInfo()
    })
}

// Получение кода из редактора
const getUserCode = () => {
    return editor ? editor.getValue() : ''
}

// Установка кода в редактор
const setUserCode = (code) => {
    if (editor) {
        editor.setValue(code)
    }
}

// Установка языка в редактор
const setEditorLanguage = async (language) => {
    if (editor) {
        const monaco = await import('monaco-editor')
        const model = editor.getModel()
        monaco.editor.setModelLanguage(model, getMonacoLanguage(language))
    }
}

// Спостерігачі
watch(selectedServerLanguage, () => {
    if (watchSelectedServerLanguage.value) {
        const code = defaultCodeExamples.value[selectedServerLanguage.value] ?? ''
        setUserCode(code)
        setEditorLanguage(selectedServerLanguage.value)
        updateEditorInfo()
    } else {
        watchSelectedServerLanguage.value = true
    }
})

watch(themeMode, () => {
    initializeMonaco();
    initializeEditor();
})

// Методы
const updateEditorInfo = () => {
    if (props.puzzleId) {
        localStorage.setItem(`puzzle-${props.puzzleId}`, JSON.stringify({
            'language': selectedServerLanguage.value,
            'code': getUserCode(),
        }))
        localStorage.setItem("previosProgrammingLanguage", selectedServerLanguage.value)
    }
}

const executeCode = async () => {
    activePanel.value = 'output'
    outputLoading.value = true
    
    try {
        const response = await apiClient.post(`/code/execute`, {
            code: getUserCode(),
            language: selectedServerLanguage.value,
            input: inputData.value
        })
        
        outputData.value = response.data['output'] ?? ""
        outputLoading.value = false

        if (response.data && response.data['error']) {
            emit('error', response.data['error'])
        }
    } catch (error) {
        handleApiError(error, "Помилка виконання коду");
        outputLoading.value = false
    }
}

const testCode = () => {
    testOutput.value = []
    activePanel.value = 'test'
    
    const ctrl = new AbortController()
    setTimeout(() => ctrl.abort(), 60000)
    testLoading.value = true

    fetchEventSource(`${process.env.VUA_APP_API_URL}/code/test`, {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + store.getters.getAccessToken,
            'ngrok-skip-browser-warning': true,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            code: getUserCode(),
            language: selectedServerLanguage.value,
            puzzleId: props.puzzleId,
            homeworkId: props.homeworkId,
            treeNodeId: props.treeNodeId
        }),
        onmessage(ev) {
            testLoading.value = false
            testOutput.value.push(ev.data)
        },
        signal: ctrl.signal,
        onerror(err) {
            if (err.name === 'AbortError') {
                console.log('Request timed out')
                emit('error', "Запит зайняв більше 60 секунд. Будь ласка, спробуйте ще раз.")
            } else {
                emit('error', 'Помилка тестування. Можливо, недостатньо прав для відправки.');
            }
            testLoading.value = false
        }
    })
}

const onLanguageChange = (event) => {
    selectedServerLanguage.value = event.target.value
}

const onFileUpload = async (event) => {
    try {
        const file = event.target.files[0]
        
        if (file.size > 5 * 1024 * 1024) {
            throw new Error('Файл завеликий. Максимальний розмір файлу - 5MB.')
        }

        if (!file.type.includes('text')) {
            throw new Error('Такий тип файлу не підтримується.')
        }
        
        const fileContent = await new Promise((resolve, reject) => {
            const reader = new FileReader()
            
            reader.onload = () => resolve(reader.result)
            reader.onerror = () => reject(new Error('Помилка при считуванні файлу'))
            
            reader.readAsText(file)
        })

        setUserCode(fileContent)

    } catch (error) {
        console.error('File uploading error:', error)
        emit('error', error.message)
    }
}

// Ініціалізація редактора
const initializeEditor = async () => {
    if (!programmingLanguages.value || !defaultCodeExamples.value) return
    
    // Відновлення збережених даних з локального сховища
    const savedData = localStorage.getItem(`puzzle-${props.puzzleId}`)

    // Ждем инициализации Monaco Editor, если он еще не готов
    if (!editor) {
        await nextTick()
        // Если редактор все еще не готов, ждем его инициализации
        while (!editor) {
            await new Promise(resolve => setTimeout(resolve, 50))
        }
    }

    if (savedData) {
        const savedDataObject = JSON.parse(savedData)
        watchSelectedServerLanguage.value = false
        selectedServerLanguage.value = savedDataObject.language
        setUserCode(savedDataObject.code)
        await setEditorLanguage(savedDataObject.language)
    } else {
        selectedServerLanguage.value = localStorage.getItem("previosProgrammingLanguage") ?? Object.keys(defaultCodeExamples.value)[0]
        const code = defaultCodeExamples.value[selectedServerLanguage.value] ?? ''
        setUserCode(code)
        await setEditorLanguage(selectedServerLanguage.value)
    }
}

// Lifecycle hooks
onMounted(() => {
    console.log('Base URL:', window.location.origin);
  console.log('Current publicPath:', __webpack_public_path__);
    nextTick(() => {
        initializeMonaco()
    })
})

onBeforeUnmount(() => {
    if (editor) {
        editor.dispose()
    }
})

// Экспорт методов
defineExpose({
    initializeEditor
})
</script>

<style lang="scss" scoped>
/* Стили только для Monaco Editor */
.monaco-editor {
    flex: 1;
    border: 1px solid var(--border-color, #ccc);
    border-radius: 5px;
    margin: 10px 0;
    padding-top: 10px;
}

/* Импортируем популярные моноширинные шрифты для программирования */
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&display=swap');

.monaco-editor {
    height: 400px;
    
    :deep(.monaco-editor) {
        width: 100%;
    }
}

/* Кастомные стили для Monaco Editor */
.monaco-editor :deep(.monaco-editor) :not(.codicon[class*="codicon-"]) {
    font-family: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'SF Mono', 'Consolas', 'Monaco', 'Liberation Mono', 'Courier New', monospace;
    font-feature-settings: 'liga', 'calt'; /* Включаем лигатуры */
    font-variant-ligatures: common-ligatures;
    font-size: 14px;

    .line-numbers {
        color: var(--secondary-color)!important;
    }
}

/* Улучшенная прозрачность для автокомплита */
.monaco-editor :deep(.suggest-widget) {
    backdrop-filter: blur(8px);
    background-color: rgba(30, 30, 30, 0.95) !important;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* Стилизация номеров строк */
.monaco-editor :deep(.line-numbers) {
    color: rgba(255, 255, 255, 0.4) !important;
    font-weight: 400;
}

/* Активная строка */
.monaco-editor :deep(.current-line) {
    background-color: rgba(255, 255, 255, 0.04) !important;
}

/* Курсор */
.monaco-editor :deep(.cursor) {
    background-color: #ffffff !important;
    width: 2px !important;
}

/* Выделение */
.monaco-editor :deep(.selected-text) {
    background-color: rgba(64, 128, 255, 0.3) !important;
}

/* Скроллбары */
.monaco-editor :deep(.monaco-scrollable-element > .scrollbar) {
    background-color: transparent !important;
}

.monaco-editor :deep(.monaco-scrollable-element > .scrollbar > .slider) {
    background-color: rgba(255, 255, 255, 0.2) !important;
    border-radius: 4px;
}

.monaco-editor :deep(.monaco-scrollable-element > .scrollbar > .slider:hover) {
    background-color: rgba(255, 255, 255, 0.3) !important;
}

/* Фолдинг (свертывание кода) */
.monaco-editor :deep(.folding-decoration) {
    color: rgba(255, 255, 255, 0.5) !important;
}

.monaco-editor :deep(.folding-decoration:hover) {
    color: rgba(255, 255, 255, 0.8) !important;
}

/* Подсказки при наведении */
.monaco-editor :deep(.monaco-hover) {
    backdrop-filter: blur(8px);
    background-color: rgba(30, 30, 30, 0.95) !important;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}

/* Стилі для редактора коду */
/*#codeEditor {
    height: 300px;
    width: 100%;
    border: 1px solid var(--secondary-color);
    border-radius: 5px;
    font-size: 14px;
    font-family: 'Courier New', Courier, monospace;
    padding: 10px;
    resize: none;
    background-color: rgba(var(--button-bg-cover), 0.2);
    color: var(--text-color);
}/*

.editor-container {
    width: 100%;
}

/* Стилі для випадаючих списків */
select {
    background-color: var(--weak-color);
    border: none;
    border-radius: 3px;
    box-shadow: var(--weak-box-shadow);
    padding: 5px;
    cursor: pointer;
    transition: background-color 0.2s ease-in-out;
    
    &:not(:last-child) {
        margin-right: 10px;
    }
}

/* Шапка редактора коду */
.editor-header {
    display: flex;
    justify-content: space-between;
    padding: 10px;
    background-color: var(--weak-color);
    border-radius: 5px;
    margin-bottom: 10px;
    gap: 10px;
    flex-direction: column;

    /* Права частина шапки */
    .right {
        *:not(:last-child) {
            margin-right: 10px;
        }
    }

    /* Стилі для завантаження файлів */
    input[type="file"] {
        display: none;
    }

    input[type="file"] + label {
        background-color: var(--weak-color);
        border: none;
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);
        padding: 5px 10px;
        cursor: pointer;
        transition: background-color 0.2s ease-in-out;
        height: 100%;
    }

    label:hover {
        background-color: var(--weak-color);
    }

    /* Кнопки запуску та тестування */
    button.run-btn, button.test-btn {
        background-color: var(--weak-color);
        border: none;
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);
        padding: 5px 10px;
        cursor: pointer;
        transition: background-color 0.2s ease-in-out;
    }
}

/* Панель перемикання між вхідними/вихідними даними та тестами */
.panel-toggle {
    /* Кнопки перемикання */
    .toggle-buttons {
        display: flex;
        justify-content: start;
        background-color: var(--weak-color);
        border-radius: 5px;
        margin: 10px 0;
        
        button {
            height: 100%;
            border: 0;
            border-right: 1px solid var(--secondary-color);
            background-color: var(--weak-color);
            padding: 5px 10px;
            transition: all 0.2s ease-in-out;
            cursor: pointer;

            &:first-of-type {
                border-top-left-radius: 5px;
                border-bottom-left-radius: 5px;
            }

            &:hover {
                filter: brightness(1.2);
            }

            &.active {
                filter: brightness(1.4);
            }
        }
    }
    
    /* Вміст панелей */
    .panel-content {
        background-color: var(--weak-color);
        border-radius: 5px;
        padding: 10px;
        min-height: 250px;
        max-height: 350px;
        overflow: auto;

        > * {
            width: 100%;
            height: 100%;
            min-height: 250px;
            max-height: 350px;
            position: relative;
            
            textarea {
                resize: none;
                width: 100%;
                height: 100%;
                border: none;
                border-radius: 5px;
                background-color: transparent;
                min-height: 250px;
                max-height: 350px;
            }
        }
    }
}
</style>