<!-- Основний шаблон компонента задачі -->
<template>
    <section class="puzzle">
        <!-- Ліва колонка з інформацією про задачу -->
        <div v-if="isLoading" class="col" :class="{'loading': isLoading}">
            <Preloader />
        </div>
        <div v-else class="col">
            <!-- Заголовок головоломки -->
            <h1>{{ puzzle.title }}</h1>
            <!-- Інформація про час та бали -->
            <div class="about">
                <p><i class="fa-solid fa-stopwatch"></i> Ліміт часу: {{ puzzle.timeLimit }} с</p>
                <p><i class="fa-solid fa-star"></i> Кількість балів: {{ puzzle.score }}</p>
            </div>
             <template v-if="isOwner || canManage">
                <hr>
                <div class="actions-panel">
                    <LinkBtn v-if="isOwner" :to="`/edit/puzzle/${puzzle.id}`" click="load" anim="go" img="wave1.svg">
                        <i class="fa-solid fa-file-pen"></i> Редагувати
                    </LinkBtn>
                    <LinkBtn v-if="canManage" :to="`/manage/puzzle/${puzzle.id}`" click="load" anim="go" img="wave2.svg">
                        <i class="fa-solid fa-user-shield"></i> Адміністрування
                    </LinkBtn>
                </div>
            </template>
            <hr>
            <!-- Опис головоломки -->
            <p>{{ puzzle.description }}</p>
            <hr>
            <!-- HTML-контент задачі -->
            <p v-html="puzzle.content"></p>
            <hr>
            <!-- Секція з прикладами -->
            <template v-if="puzzle.puzzleData != null && puzzle.puzzleData.length > 0">
                <h2>Приклади:</h2>
                <div class="data">
                    <!-- Цикл по тестовим даним -->
                    <div v-for="data in puzzle.puzzleData" class="item" :key="data.id">
                        <div class="input">
                            <p>Вхідні дані</p>
                            <pre>{{ data.input }}</pre>
                        </div>
                        <div class="output">
                            <p>Вихідні дані</p>
                            <pre>{{ data.output }}</pre>
                        </div>
                    </div>
                </div>
                <hr>
            </template>

            <template v-if="puzzle.taskType == 'FULL_INTERACTIVE'">
                <div>
                    <h3><strong class="alert-text">Увага:</strong> це <em>інтерактивна задача</em></h3>
                    <p>
                        Ви спілкуєтесь із перевіряючою системою:</br>
                        надсилаєте запит &rarr; <strong>негайно</strong> скидаєте буфер &rarr; читаєте відповідь &rarr; повторюєте.
                        Не друкуйте зайвого, дотримуйтесь протоколу з умови.
                    </p>
                    <h3>Як скидати буфер:</h3>
                    <details>
                        <summary>C++, Python, C, Java, Rust, ...</summary>
                        <ul>
                            <li>C++: 
                                <ul>
                                    <li>cout << ... << flush;</li>
                                    <li>cout << ... << endl;</li>
                                    <li>cout.flush();</li>
                                </ul>
                            </li>
                            <li>Python:
                                <ul>
                                    <li>print(..., flush=True)</li>
                                    <li>sys.stdout.flush()</li>
                                </ul>
                            </li>
                            <li>C: fflush(stdout);</li>
                            <li>Java: System.out.flush();</li>
                            <li>Rust: io::stdout().flush().unwrap();</li>
                        </ul>
                        <br>
                        <p>Для інших мов рекомендуємо дивитись відповідну документацію</p>
                    </details>
                </div>
                <hr>
            </template>
            <!-- Інформація про автора -->
            <p>Автор: <Link :to="`/user/${puzzle.author ?? ''}`"><i class="fa-solid fa-at"></i>{{ puzzle.author ?? '' }}</Link></p>
        </div>

        <!-- Права колонка з редактором коду -->
        <div class="col">
            <CodeEditor 
                :puzzle-id="puzzle.id" 
                @error="addErrorMessage" 
                @success="addSuccessMessage"
                :homeworkId="contextHomeworkId"
                :treeNodeId="contextTreeNodeId"
                ref="codeEditorRef"
            />
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

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '@/axios'
import { hasGlobalPermission } from '@/services/permissionService.js'
import { loadJsonStaticResource } from '@/services/resourceService.js'
import LinkBtn from '@/components/LinkBtn.vue'
import Preloader from '@/components/Preloader.vue'
import Link from '@/components/Link.vue'
import CodeEditor from '@/components/CodeEditor.vue'

// Композабли
const store = useStore()
const route = useRoute()
const router = useRouter()

// Реф на дочірній компонент
const codeEditorRef = ref(null)

// Реактивні дані
const isLoading = ref(true)
const puzzle = ref({
    decisions: [], // Масив рішень користувачів
})
const contextHomeworkId = ref(null)
const contextTreeNodeId = ref(null)

// Обчислювані властивості
const programmingLanguages = computed(() => store.getters.programmingLanguages)
const userRoles = computed(() => store.getters.getUserPermissions)
const currentUsername = computed(() => store.getters.getCurrentUsername)

const isOwner = computed(() => puzzle.value.author === currentUsername.value)
const canManage = computed(() => 
    !isOwner.value &&
    (hasGlobalPermission('MAKE_PUZZLE_INVISIBLE') || hasGlobalPermission('MAKE_PUZZLE_VISIBLE'))
)

// Методи
const addErrorMessage = (message) => {
    store.dispatch('addErrorMessage', message)
}

const addSuccessMessage = (message) => {
    store.dispatch('addSuccessMessage', message)
}

// Отримання даних задачі з сервера
const updatePuzzle = async () => {
    isLoading.value = true;
    
    contextHomeworkId.value = route.query.homeworkId || null;    
    contextTreeNodeId.value = route.query.treeNodeId || null;

    try {
        await loadJsonStaticResource("defaultCodeExamples")
        await loadJsonStaticResource("programmingLanguages")         

        const response = await apiClient.get(`/puzzles/get/${route.params.id}`, {
            params: {
                homeworkId: contextHomeworkId.value,
                treeNodeId: contextTreeNodeId.value
            }
        })
        puzzle.value = response.data

        const dateOptions = {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        }

        // Форматування дати та мови для кожного рішення
        puzzle.value.decisions.forEach((decision) => {
            const date = new Date(decision.createdAt)
            decision.createdAt = date.toLocaleString('uk-UA', dateOptions)

            const currentLanguage = programmingLanguages.value.find((language) => language.server_id === decision.language)
            if (currentLanguage) {
                decision.language = `${currentLanguage.name} (${currentLanguage.version})`
            }
        })

        await nextTick()
        
        // Ініціалізуємо редактор після завантаження даних
        if (codeEditorRef.value) {
            codeEditorRef.value.initializeEditor()
        }
        
        isLoading.value = false
    } catch (error) {
        if (error.response?.status === 404 || error.response?.status === 403) {
            store.dispatch('addErrorMessage', 'Задачу не знайдено або вона недоступна в даному контексті.');
            router.push('/puzzles');
            return;
        } else {
            handleApiError(error, 'Не вдалося завантажити задачу.');
        }
    }
}

// Спостерігачі
watch(() => route.params.id, () => {
    updatePuzzle()
})

// Хуки життєвого циклу
onMounted(() => {
    updatePuzzle()
})
</script>

<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';

.alert-text {
    color: rgba(236, 68, 68, 0.781);
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

        &.loading {
            display: flex;
            align-items: center;
            justify-content: center;
        }

        :deep(ul), :deep(ol) {
            margin-left: 35px;
        }

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