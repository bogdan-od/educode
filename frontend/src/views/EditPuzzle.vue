<!-- Основна форма для редагування задачі -->
<template>
    <h1>Редагування задачі</h1>
    <form @submit.prevent="updatePuzzle" class="main-form" v-if="!loading && isOwner">
        <!-- Поле для введення назви задачі -->
        <div class="form-group">
            <div><i class="fa-solid fa-magnifying-glass"></i></div>
            <input type="text" v-model="values.title" name="title" id="title" placeholder="Назва" required>
            <label for="title">Назва</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>
        
        <!-- Поле для введення опису задачі -->
        <div class="form-group">
            <div><i class="fa-solid fa-align-left"></i></div>
            <textarea v-model="values.description" name="description" id="description" placeholder="Опис" required></textarea>
            <label for="description">Опис</label>
            <p class="hint error">{{ errors.description }}</p>
        </div>
        
        <!-- Редактор тексту задачі з використанням QuillEditor -->
        <div class="form-group">
            <div><i class="fa-solid fa-file-lines"></i></div>
            <QuillEditor :onUpdate="(val) => {values.content = val}" :value="values.content"/>
            <label for="content">Текст задачі</label>
            <p class="hint error">{{ errors.content }}</p>
        </div>
        
        <!-- Поле для встановлення обмеження часу -->
        <div class="form-group">
            <div><i class="fa-solid fa-clock"></i></div>
            <input type="text" v-model="values.timeLimit" name="timeLimit" id="timeLimit" placeholder="Обмеження часу" required>
            <label for="timeLimit">Обмеження часу</label>
            <p class="hint error">{{ errors.timeLimit }}</p>
        </div>

        <!-- Поле для включення/виключення задачі -->
        <div class="form-group-checkbox">
            <input type="checkbox" v-model="values.enabled" name="enabled" id="enabled">
            <label for="enabled">
                <span>Задача активна</span>
                <div class="checkbox">
                    <div class="checked"><i class="fa-solid fa-check"></i></div>
                    <div class="unchecked"><i class="fa-solid fa-xmark"></i></div>
                </div>
            </label>
        </div>

        <!-- Поле для visible/invisible задачі -->
        <div class="form-group-checkbox">
            <input type="checkbox" v-model="values.visible" name="visible" id="visible" :disabled="!hasGlobalPermission('PUBLISH_PUZZLE')">
            <label for="visible" :title="hasGlobalPermission('PUBLISH_PUZZLE') ? 'Опублікувати задачу?' : 'Ви не можете опублікувати задачу - тільки створити'">
                <span>Задача видима</span>
                <div class="checkbox">
                    <div class="checked"><i class="fa-solid fa-check"></i></div>
                    <div class="unchecked"><i class="fa-solid fa-xmark"></i></div>
                </div>
            </label>
        </div>

        <!-- Секція для тест кейсів (показується для NON_INTERACTIVE та OUTPUT_CHECKING) -->
        <template v-if="puzzleType === 'NON_INTERACTIVE' || puzzleType === 'OUTPUT_CHECKING'">
            <div v-for="(testCase, index) in values.puzzleData" :key="testCase.id || index" class="test-case-group">
                <br>
                <h3>Тест кейс #{{ index + 1 }}</h3>
                <br>
                
                <!-- Поле для введення вхідних даних тесту -->
                <div class="form-group">
                    <div><i class="fa-solid fa-keyboard"></i></div>
                    <textarea v-model="testCase.input" :name="'input' + index" :id="'input' + index" placeholder="Вхідні дані" required></textarea>
                    <label :for="'input' + index">Вхідні дані</label>
                    <p class="hint error">{{ errors.puzzleData && errors.puzzleData[index]?.input }}</p>
                </div>

                <!-- Галочка для OUTPUT_CHECKING: використовувати checker замість статичного output -->
                <template v-if="puzzleType === 'OUTPUT_CHECKING'">
                    <div class="form-group-checkbox">
                        <input type="checkbox" v-model="testCase.useChecker" :name="'useChecker' + index" :id="'useChecker' + index">
                        <label :for="'useChecker' + index">
                            <span>Використовувати checker для перевірки (замість статичних вихідних даних)</span>
                            <div class="checkbox">
                                <div class="checked"><i class="fa-solid fa-check"></i></div>
                                <div class="unchecked"><i class="fa-solid fa-xmark"></i></div>
                            </div>
                        </label>
                    </div>
                </template>

                <!-- Поле для введення очікуваних вихідних даних -->
                <div class="form-group" v-if="puzzleType === 'NON_INTERACTIVE' || (puzzleType === 'OUTPUT_CHECKING' && !testCase.useChecker)">
                    <div><i class="fa-solid fa-keyboard"></i></div>
                    <textarea v-model="testCase.output" :name="'output' + index" :id="'output' + index" placeholder="Вихідні дані" :required="puzzleType === 'NON_INTERACTIVE'"></textarea>
                    <label :for="'output' + index">Вихідні дані</label>
                    <p class="hint error">{{ errors.puzzleData && errors.puzzleData[index]?.output }}</p>
                </div>

                <!-- Поле для відображення балів за тест (disabled) -->
                <div class="form-group">
                    <div><i class="fa-solid fa-star"></i></div>
                    <input type="text" :value="testCase.score" :name="'score' + index" :id="'score' + index" placeholder="Бали за тест" disabled>
                    <label :for="'score' + index">Бали за тест</label>
                </div>
            </div>
            <br>
            <p class="hint error">{{ errors.puzzleData }}</p>
            <br>
            
            <!-- Поле для відображення загальної кількості балів -->
            <div class="form-group">
                <div><i class="fa-solid fa-star"></i></div>
                <input type="text" :value="totalScore" id="total_score" placeholder="Бали за задачу" disabled>
                <label for="total_score">Бали за задачу</label>
            </div>
            <br>
            
            <!-- Поле для відображення кількості прикладів -->
            <div class="form-group">
                <div><i class="fa-solid fa-chalkboard-user"></i></div>
                <input type="text" :value="exampleTestCasesCount" id="example_testcases_count" placeholder="Кількість тест кейсів, що будуть показані як приклади" disabled>
                <label for="example_testcases_count">Кількість тест кейсів, що будуть показані як приклади (це всі тести з 0 балами)</label>
            </div>
        </template>

        <!-- Секція для інтерактивних задач та задач з динамічною перевіркою -->
        <template v-if="puzzleType === 'FULL_INTERACTIVE' || puzzleType === 'OUTPUT_CHECKING'">
            <br>
            <h3>Налаштування {{ puzzleType === 'FULL_INTERACTIVE' ? 'інтерактивної задачі' : 'динамічної перевірки' }}</h3>
            <br>

            <!-- Вибір нового checker'а з існуючих -->
            <ApiSearchSelector
                v-model="values.checkerId"
                apiUrl="/checker/autocomplete" 
                label="Змінити checker"
                placeholder="Пошук checker..."
                :initial-id="values.checkerId"
                :initial-text="initialCheckerText"
            />
            <p class="hint error">{{ errors.checkerId }}</p>

            <div class="form-group-custom">
                <LinkBtn v-if="values.checkerId" anim="go" click="go" :to="`/checker/${values.checkerId}`">До checker'у</LinkBtn>
            </div>
        </template>

        <!-- Поле для відображення балів за задачу -->
        <div class="form-group" v-if="puzzleType === 'FULL_INTERACTIVE'">
            <div><i class="fa-solid fa-star"></i></div>
            <input type="text" :value="maxScore" name="maxScore" id="maxScore" placeholder="Максимальні бали за задачу" disabled>
            <label for="maxScore">Максимальні бали за задачу</label>
        </div>

        <br>
        <!-- Кнопка для відправки форми -->
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!form_valid">
            <i class="fa-regular fa-pen-to-square"></i> Оновити
        </LinkBtn>
        
        <br><br>
        
        <!-- Кнопка для видалення задачі -->
        <LinkBtn role="btn" type="button" img="wave2.svg" anim="bg-scale" @click="showDeleteConfirmation">
            <i class="fa-solid fa-trash"></i> Видалити задачу
        </LinkBtn>
    </form>

    <!-- Індикатор завантаження -->
    <Preloader v-else-if="loading"/>

    <div v-else class="error-page">
         <h1>Помилка</h1>
         <p>Ви не є автором цієї задачі. Ви можете керувати нею на <router-link :to="`/manage/puzzle/${puzzleId}`">сторінці адміністрування</router-link>, якщо у вас є права.</p>
         <LinkBtn to="/puzzles" anim="go">До списку задач</LinkBtn>
    </div>

    <Modal 
        v-model:visible="modalInfo.visible"
        :type="modalInfo.type" 
        :title="modalInfo.title"
        :text="modalInfo.text"
        :show-cancel-button="modalInfo.showCancelButton"
        @confirm="modalInfo.confirmAction"
    />
</template>

<!-- Імпортуємо стандартні стилі форми -->
<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

// Встановлюємо ширину форми та заголовка
form.main-form, h1 {
    width: 100%;
    max-width: 800px;
}

.hint.hint-message {
    font-family: $form-font;
}
</style>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import LinkBtn from '@/components/LinkBtn.vue'
import apiClient from '@/axios.js'
import QuillEditor from '@/components/QuillEditor.vue'
import checkerService from '@/checkerService.js'
import ApiSearchSelector from '@/components/ApiSearchSelector.vue'
import Modal from '../components/Modal.vue'
import { loadJsonStaticResource } from '@/services/resourceService.js'
import Preloader from "@/components/Preloader.vue"
import { hasGlobalPermission } from '../services/permissionService'
import { handleApiError } from '@/services/errorHandler.js'
import { showQuestionModal } from '@/services/modalService.js'

const store = useStore()
const router = useRouter()
const route = useRoute()

// Реактивні дані
const values = reactive({
    title: '',
    description: '',
    content: '',
    timeLimit: '',
    enabled: true,
    puzzleData: [],
    checkerId: null,
    visible: hasGlobalPermission('PUBLISH_PUZZLE')
})

const errors = reactive({
    title: '',
    description: '',
    content: '',
    timeLimit: '',
    puzzleData: '',
    checkerId: ''
})

const modalInfo = reactive({
    title: '',
    text: '',
    visible: false,
    type: 'info',
    showCancelButton: false,
    confirmAction: null
})

const form_valid = ref(false)
const loading = ref(true)
const submitting = ref(false)
const puzzleType = ref('')
const availableCheckers = ref([])
const maxScore = ref(0)
const initialCheckerText = ref('')

const puzzleId = route.params.id;
const isOwner = ref(false);
const currentUsername = computed(() => store.getters.getCurrentUsername);
const canPublish = computed(() => isOwner.value && hasGlobalPermission('PUBLISH_PUZZLE'));

// Computed properties
const totalScore = computed(() => {
    if (puzzleType.value === 'FULL_INTERACTIVE') return maxScore.value
    let score = 0
    for (let testCase of values.puzzleData) {
        if (testCase.score && !isNaN(parseFloat(testCase.score))) {
            score += parseFloat(testCase.score)
        }
    }
    return score
})

const exampleTestCasesCount = computed(() => {
    return values.puzzleData.filter(testCase => 
        testCase.score !== undefined && parseFloat(testCase.score) === 0
    ).length
})

// Methods
const isFloat = (str) => {
    return !isNaN(str) && !isNaN(parseFloat(str))
}

const loadPuzzle = async () => {
    loading.value = true;
    try {
        const response = await apiClient.get(`/puzzles/get-edit/${puzzleId}`)
        
        const puzzle = response.data

        isOwner.value = puzzle.author === currentUsername.value;

        if (!isOwner.value) {
            // Если не владелец, не загружаем данные в форму
            loading.value = false;
            return; 
        }
        
        values.title = puzzle.title
        values.description = puzzle.description
        values.content = puzzle.content
        values.timeLimit = puzzle.timeLimit.toString()
        values.enabled = puzzle.enabled
        values.visible  = puzzle.visible
        
        puzzleType.value = puzzle.taskType
        maxScore.value = puzzle.score || 0
        
        if (puzzle.puzzleData) {
            values.puzzleData = puzzle.puzzleData.map(testCase => ({
                ...testCase,
                useChecker: testCase.output === null
            }))
        }
        
        if (puzzle.checkerId) {
            values.checkerId = puzzle.checkerId

            // Загружаем текст для ApiSearchSelector
            try {
                 const checkerRes = await apiClient.get(`/checker/get/${puzzle.checkerId}`);
                 initialCheckerText.value = checkerRes.data.name;
            } catch(e) { console.warn("Could not load checker name"); }
        }
        
        loading.value = false
    } catch (error) {
        console.error('Error loading puzzle:', error)
        handleApiError(error, 'Помилка завантаження задачі')
        router.push('/puzzles')
    }
}

const updatePuzzle = async () => {
    validateForm();
    if (!form_valid.value) return

    submitting.value = true;
    try {        
        const requestData = {
            title: values.title,
            description: values.description,
            content: values.content,
            timeLimit: values.timeLimit,
            enabled: values.enabled,
            visible: values.visible
        }

        if (values.checkerId) {
            requestData.checkerId = values.checkerId
        }

        if (puzzleType.value === 'NON_INTERACTIVE' || puzzleType.value === 'OUTPUT_CHECKING') {
            requestData.puzzleData = values.puzzleData.map(testCase => ({
                id: testCase.id,
                input: testCase.input,
                output: (puzzleType.value === 'OUTPUT_CHECKING' && testCase.useChecker) ? null : testCase.output
            }))
        }

        const response = await apiClient.put(`/puzzles/edit/${puzzleId}`, requestData)
        
        store.dispatch('addSuccessMessage', 'Задачу успішно оновлено')
        router.push(`/puzzle/${puzzleId}`)
    } catch (error) {
        console.error("Error updating puzzle: ", error)
        handleApiError(error, 'Помилка оновлення задачі', errors)
    } finally {
        submitting.value = false;
    }
}

const showDeleteConfirmation = () => {
    showQuestionModal(
        "Підтвердження видалення",
        "Ви впевнені, що хочете видалити цю задачу? Цю дію неможливо буде скасувати.",
        deletePuzzle
    )
}

const deletePuzzle = async () => {
    try {
        await apiClient.delete(`/puzzles/delete/${puzzleId}`)
        
        store.dispatch('addSuccessMessage', 'Задачу успішно видалено')
        router.push('/puzzles')
    
        modalInfo.visible = false
    } catch (error) {
        console.error("Error deleting puzzle: ", error)
        handleApiError(error, 'Помилка видалення задачі')
    }
}

const validateForm = () => {
    // Скидаємо помилки
    Object.keys(errors).forEach(key => {
        if (typeof errors[key] === 'string') {
            errors[key] = ''
        }
    })
    
    form_valid.value = true
    
    // Перевірка назви
    if (values.title.length < 5 || values.title.length > 100) {
        errors.title = 'Назва повинна містити від 5 до 100 символів'
        form_valid.value = false
    }

    // Перевірка опису
    if (values.description.length < 5 || values.description.length > 300) {
        errors.description = 'Опис повинен містити від 5 до 300 символів'
        form_valid.value = false
    }

    // Перевірка тексту задачі
    if (values.content.length < 20 || values.content.length > 7000) {
        errors.content = 'Текст задачі повинен містити від 20 до 7000 символів'
        form_valid.value = false
    }

    // Перевірка обмеження часу
    const timeLimit = parseFloat(values.timeLimit)
    if (!isFloat(values.timeLimit) || timeLimit < 0.2 || timeLimit > 10) {
        errors.timeLimit = 'Обмеження часу повинно бути від 0.2 до 10'
        form_valid.value = false
    }

    // Валідація тест кейсів
    if (puzzleType.value === 'NON_INTERACTIVE' || puzzleType.value === 'OUTPUT_CHECKING') {
        for (let i = 0; i < values.puzzleData.length; i++) {
            const testCase = values.puzzleData[i]
            
            if (testCase.input.length > 200) {
                errors.puzzleData = 'Вхідні дані повинні містити від 0 до 200 символів'
                form_valid.value = false
                break
            }

            const needsOutput = puzzleType.value === 'NON_INTERACTIVE' || 
                               (puzzleType.value === 'OUTPUT_CHECKING' && !testCase.useChecker)
            
            if (needsOutput && testCase.output && testCase.output.length > 200) {
                errors.puzzleData = 'Вихідні дані повинні містити від 0 до 200 символів'
                form_valid.value = false
                break
            }
        }
    }
}

// Watchers
watch(values, () => {
    validateForm()
}, { deep: true })

// Lifecycle hooks
onMounted(async () => {
    await Promise.all([
        loadPuzzle(),
        loadJsonStaticResource("checkerLanguages")
    ])
})
</script>
