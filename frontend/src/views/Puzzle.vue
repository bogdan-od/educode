<!-- Основний шаблон компонента задачі -->
<template>
    <section class="puzzle">
        <!-- Ліва колонка з інформацією про задачу -->
        <div class="col">
            <Preloader v-if="isLoading" />
            <!-- Заголовок головоломки -->
            <h1>{{ puzzle.title }}</h1>
            <!-- Інформація про час та бали -->
            <div class="about">
                <p><i class="fa-solid fa-stopwatch"></i> Ліміт часу: {{ puzzle.timeLimit }} с</p>
                <p><i class="fa-solid fa-star"></i> Кількість балів: {{ puzzle.score }}</p>
            </div>
            <hr>
            <!-- Опис головоломки -->
            <p>{{ puzzle.description }}</p>
            <hr>
            <!-- HTML-контент задачі -->
            <p v-html="puzzle.content"></p>
            <hr>
            <!-- Секція з прикладами -->
            <h2>Приклади:</h2>
            <div class="data">
                <!-- Цикл по тестовим даним -->
                <div v-for="data in puzzle.puzzleData" class="item">
                    <div class="input">
                        <p>Вхідні дані</p>
                        <pre>{{ data.input }}</pre>
                    </div>
                    <div class="output">
                        <p>Вихідні дані</p>
                        <pre>{{ data.output }}</pre>
                    </div>
                    <p><i class="fa-solid fa-star"></i> Кількість балів: {{ data.score }}</p>
                </div>
            </div>
            <hr>
            <!-- Інформація про автора -->
            <p>Автор: <Link :to="`/user/${puzzle.author ?? ''}`"><i class="fa-solid fa-at"></i>{{ puzzle.author ?? '' }}</Link></p>
        </div>

        <!-- Права колонка з редактором коду -->
        <div class="col">
            <div class="editor-container">
                <!-- Панель інструментів редактора -->
                <div class="editor-header">
                    <div class="left">
                        <!-- Вибір мови програмування -->
                        <select @change="onLanguageChange">
                            <option v-for="lang in languages" :key="lang.server_id" :value="lang.server_id" :selected="lang.server_id === selectedServerLanguage">
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
                <!-- Текстовий редактор коду -->
                <textarea v-model="userCode" spellcheck="false" class="codeEditor" id="codeEditor" ref="editorContainer"></textarea>
                
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
        </div>
    </section>

    <!-- Секція з розв'язками інших користувачів, її баче лише автор задачі -->
    <section v-if="puzzle.decisions.length > 0" class="decisions">
        <h3>Розв'язки</h3>
        <div v-for="(decision, i) in puzzle.decisions" :key="i" class="decision">
            <i v-if="!decision.finished" class="isRunning fa-solid fa-person-running"></i>
            <i v-if="decision.correct" class="isCorrect fa-solid fa-square-check"></i>
            <p><Link :to="`/user/${decision.user.login}`">@{{ decision.user.login }}</Link> ({{ decision.user.rating }})</p>
            <p>{{ decision.createdAt }}</p>
            <div class="decision-info">
                <span class="language"><i class="fa-solid fa-screwdriver-wrench"></i> {{ decision.language }}</span>
                <span class="score"><i :class="`fa-${decision.score == decision.puzzle.score ? 'solid' : 'regular'} fa-star`"></i> {{ decision.score }}/{{ decision.puzzle.score }}</span>
            </div>
        </div>
    </section>
</template>

<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';

/* Стилі для редактора коду */
#codeEditor {
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
}

/* Головний контейнер для головоломки з двома колонками */
.puzzle {
    @include display-flex($justify-content: space-between, $align-items: flex-start);
    width: 100%;
    
    /* Стилі для колонок */
    .col {
        width: 48%;
        position: relative;
        min-height: 500px;

        /* Адаптивність для мобільних пристроїв */
        @media (max-width: 1024px) {
            width: 100%;

            &:nth-child(2) {
                order: 1;
            }

            &:nth-child(1) {
                order: 2;
                margin-top: 50px;
            }
        }
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
        @include display-flex($justify-content: space-between);
        padding: 10px;
        background-color: var(--weak-color);
        border-radius: 5px;
        margin-bottom: 10px;
        gap: 10px;

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
            @include display-flex($justify-content: start);
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

    /* Секція з вхідними та вихідними даними */
    .data {
        @include display-flex($flex-direction: column);
        gap: 20px;

        .item {
            width: 100%;
            @include display-flex($justify-content: space-between);
            gap: 5px;

            /* Блоки вхідних та вихідних даних */
            .input, .output {
                width: calc(50% - 20px);

                pre {
                    margin-top: 5px;
                    background-color: var(--weak-color);
                    border-radius: 5px;
                    padding: 10px;
                    min-height: 120px;
                    overflow-y: auto;
                    overflow-x: hidden;
                    white-space: pre-wrap;
                    word-wrap: break-word;
                    word-break: break-all;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: 14px;
                    color: var(--text-color);
                }
            }
        }
    }
}

/* Секція з розв'язками інших користувачів */
.decisions {
    @include display-flex($flex-direction: column, $align-items: flex-start);
    margin-top: 30px;
    gap: 15px;
    width: 100%;

    h3 {
        font-size: 28px;
    }

    /* Стилі для окремого розв'язку */
    .decision {
        @include display-flex($flex-direction: column, $align-items: flex-start);
        background-color: var(--weak-color);
        padding: 15px;
        border-radius: 5px;
        position: relative;
        width: 100%;
        gap: 10px;

        /* Індикатори статусу розв'язку */
        .isRunning, .isCorrect {
            position: absolute;
            top: -7.5px;
            width: min-content;
            height: min-content;
            padding: 5px;
            border-radius: 5px;
            background-color: var(--weak-color);
        }

        .isRunning {
            right: -7.5px;
        }

        .isCorrect {
            left: -7.5px;
        }

        /* Інформація про розв'язок */
        .decision-info {
            @include display-flex($justify-content: space-between);
            gap: 10px;
            width: 100%;
        }
    }
}
</style>

<script scoped>
// Імпорт необхідних компонентів та модулів
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '../axios';
import store from '../store';
import Preloader from '@/components/Preloader.vue';
import { mapActions, mapGetters } from 'vuex';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import Link from '@/components/Link.vue';

export default {
    name: 'Puzzle',
    // Реєстрація компонентів
    components: {
        LinkBtn,
        Preloader,
        Link,
    },
    // Початковий стан компонента
    data() {
        return {
            isLoading: true,
            puzzle: {
                decisions: [], // Масив рішень користувачів
            },
            // Список доступних мов програмування з їх версіями
            languages: [
                {
                    id: 'assembler',
                    server_id: 'assembler:latest',
                    name: 'Assembler Compiler',
                    version: 'latest'
                },
                {
                    id: 'cpp-with-gmp',
                    server_id: 'cpp-with-gmp:14.2',
                    name: 'C++ with GMP',
                    version: '14.2'
                },
                {
                    id: 'dotnet',
                    server_id: 'dotnet:8.0',
                    name: '.NET',
                    version: '8.0'
                },
                {
                    id: 'java',
                    server_id: 'java:23',
                    name: 'Java',
                    version: '23'
                },
                {
                    id: 'node',
                    server_id: 'node:22.11.0',
                    name: 'Node.js',
                    version: '22.11.0'
                },
                {
                    id: 'pypy',
                    server_id: 'pypy:3.10',
                    name: 'PyPy',
                    version: '3.10'
                },
                {
                    id: 'rust',
                    server_id: 'rust:1.82.0',
                    name: 'Rust',
                    version: '1.82.0'
                },
                {
                    id: 'c',
                    server_id: 'c:10.2',
                    name: 'C',
                    version: '10.2'
                },
                {
                    id: 'dart',
                    server_id: 'dart:3.5',
                    name: 'Dart',
                    version: '3.5'
                },
                {
                    id: 'go',
                    server_id: 'go:1.20',
                    name: 'Go',
                    version: '1.20'
                },
                {
                    id: 'kotlin',
                    server_id: 'kotlin:2.0.21',
                    name: 'Kotlin',
                    version: '2.0.21'
                },
                {
                    id: 'perl',
                    server_id: 'perl:5.32',
                    name: 'Perl',
                    version: '5.32'
                },
                {
                    id: 'python',
                    server_id: 'python:3.11',
                    name: 'Python',
                    version: '3.11'
                },
                {
                    id: 'swift',
                    server_id: 'swift:5.6',
                    name: 'Swift',
                    version: '5.6'
                },
                {
                    id: 'cpp',
                    server_id: 'cpp:14.2',
                    name: 'C++',
                    version: '14.2'
                },
                {
                    id: 'd-gdc',
                    server_id: 'd-gdc:14.2',
                    name: 'D (GDC)',
                    version: '14.2'
                },
                {
                    id: 'haskell',
                    server_id: 'haskell:8.8',
                    name: 'Haskell',
                    version: '8.8'
                },
                {
                    id: 'mono',
                    server_id: 'mono:6.12',
                    name: 'Mono',
                    version: '6.12'
                },
                {
                    id: 'php',
                    server_id: 'php:8.2',
                    name: 'PHP',
                    version: '8.2'
                },
                {
                    id: 'ruby',
                    server_id: 'ruby:3.3',
                    name: 'Ruby',
                    version: '3.3'
                },
                {
                    id: 'lua',
                    server_id: 'lua:latest',
                    name: 'Lua',
                    version: 'latest'
                },
                {
                    id: 'pascal',
                    server_id: 'pascal:latest',
                    name: 'Pascal',
                    version: 'latest'
                }
            ],
            activePanel: 'input', // Активна панель (введення/вивід/тест)
            outputData: '', // Дані виводу програми
            selectedServerLanguage: 'python:3.11', // Обрана мова програмування
            inputData: '', // Вхідні дані для програми
            userCode: '', // Код користувача
            testOutput: [], // Результати тестування
            outputLoading: false, // Індикатор завантаження виводу
            testLoading: false, // Індикатор завантаження тестів
        }
    },
    // Обчислювані властивості
    computed: {
        ...mapGetters(['getThemeMode']),
    },
    // Спостерігачі за змінами
    watch: {
        '$route.params.id': function() {
            this.updatePuzzle();
        },
        userCode() {
            this.updateEditorInfo();
        },
        selectedServerLanguage() {
            this.updateEditorInfo();
        },
    },
    methods: {
        ...mapActions(['addSuccessMessage', 'addErrorMessage']),
        // Оновлення інформації редактора в локальному сховищі
        updateEditorInfo() {
            if (this.puzzle.id) {
                localStorage.setItem(`puzzle-${this.puzzle.id}`, JSON.stringify({
                    'language': this.selectedServerLanguage,
                    'code': this.userCode,
                }));
            }
        },
        // Отримання даних задачі з сервера
        async updatePuzzle() {
            const self = this;

            await apiClient.get(`/puzzles/get/${this.$route.params.id}`)
                .then((response) => {
                    this.puzzle = response.data;

                    const dateOptions = {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                        hour12: false
                    };

                    // Форматування дати та мови для кожного рішення
                    this.puzzle.decisions.forEach((decision) => {
                        const date = new Date(decision.createdAt);
                        decision.createdAt = date.toLocaleString('uk-UA', dateOptions);

                        const currentLanguage = this.languages.find((language) => language.server_id === decision.language);
                        decision.language = `${currentLanguage.name} (${currentLanguage.version})`;
                    });

                    // Відновлення збережених даних з локального сховища
                    const savedData = localStorage.getItem(`puzzle-${this.puzzle.id}`);
                    if (savedData) {
                        const savedDataObject = JSON.parse(savedData);
                        this.selectedServerLanguage = savedDataObject.language;
                        this.userCode = savedDataObject.code;
                    }

                    this.isLoading = false;
                })
                .catch((error) => {
                    if (error.response.status === 404) {
                        this.$router.push('/puzzles');
                    }
                });
        },
        // Виконання коду користувача
        async executeCode() {
            const self = this;
            this.activePanel = 'output';
            this.outputLoading = true;
            
            await apiClient.post(`/code/execute`, {
                code: this.userCode,
                language: this.selectedServerLanguage,
                input: this.inputData
            })
                .then((response) => {
                    this.outputData = response.data['output'] ?? "";
                    this.outputLoading = false;

                    if (response.data && response.data['error']) {
                        self.addErrorMessage(response.data['error']);
                    }
                })
                .catch((error) => {
                    if (error.response && error.response.data && error.response.data['message']) {
                        this.addErrorMessage(error.response.data['message']);
                    }
                    if (error.response && error.response.status == 401) {
                        this.addErrorMessage("Потрібно авторизуватися");
                    }

                    this.outputLoading = false;
                });
        },
        // Тестування коду користувача
        testCode() {
            this.testOutput = [];
            this.activePanel = 'test';
            
            // Використовуємо AbortController для відміни запиту, якщо він не завершиться протягом 60 секунд
            const ctrl = new AbortController();
            setTimeout(() => ctrl.abort(), 60000);
            const self = this;
            this.testLoading = true;

            // Запит на сервер для тестування коду за допомогою SSE - Server-Sent Events
            fetchEventSource(`${process.env.VUA_APP_API_URL}/code/test`, {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + store.getters.getAccessToken,
                    'ngrok-skip-browser-warning': true,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    code: this.userCode,
                    language: this.selectedServerLanguage,
                    puzzleId: this.puzzle.id,
                }),
                onmessage(ev) {
                    self.testLoading = false;
                    self.testOutput.push(ev.data);
                },
                signal: ctrl.signal,
                onerror(err) {
                    if (err.name === 'AbortError') {
                        console.log('Request timed out');
                        self.addErrorMessage("Запит зайняв більше 60 секунд. Будь ласка, спробуйте ще раз.");
                    }
                    self.testLoading = false;
                }
            });
        },
        // Обробник зміни мови програмування
        onLanguageChange(event) {
            this.selectedServerLanguage = event.target.value;
        },
        // Обробник завантаження файлу з кодом
        async onFileUpload(event) {
            try {
                const file = event.target.files[0];
                
                if (file.size > 5 * 1024 * 1024) {
                    throw new Error('Файл завеликий. Максимальний розмір файлу - 5MB.');
                }

                if (!file.type.includes('text')) {
                    throw new Error('Такий тип файлу не підтримується.');
                }
                
                const fileContent = await new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    
                    reader.onload = () => resolve(reader.result);
                    reader.onerror = () => reject(new Error('Помилка при считуванні файлу'));
                    
                    reader.readAsText(file);
                });

                this.userCode = fileContent;

            } catch (error) {
                console.error('File uploading error:', error);
                this.addErrorMessage(error.message);
            }
        },
    },
    // Хук життєвого циклу: монтування компонента
    mounted() {
        this.updatePuzzle();
    },
}
</script>