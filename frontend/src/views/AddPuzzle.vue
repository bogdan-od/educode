<template>
    <h1>Додавання задачі</h1>
    <form @submit.prevent="addPuzzle" class="main-form">
        <!-- Puzzle type selection -->
        <div class="form-group">
            <div><i class="fa-solid fa-list"></i></div>
            <select v-model="values.puzzleType" name="puzzleType" id="puzzleType" required>
                <option value="">Оберіть тип задачі</option>
                <option value="NON_INTERACTIVE">Задача з тест-кейсами</option>
                <option value="FULL_INTERACTIVE">Інтерактивна задача</option>
                <option value="OUTPUT_CHECKING">Задача з динамічною перевіркою відповідей</option>
            </select>
            <label for="puzzleType">Тип задачі</label>
            <p class="hint error">{{ errors.puzzleType }}</p>
        </div>
        
        <!-- Puzzle title input -->
        <div class="form-group">
            <div><i class="fa-solid fa-magnifying-glass"></i></div>
            <input type="text" v-model="values.title" name="title" id="title" placeholder="Назва" required>
            <label for="title">Назва</label>
            <p class="hint error">{{ errors.title }}</p>
        </div>

        <!-- Puzzle description textarea -->
        <div class="form-group">
            <div><i class="fa-solid fa-align-left"></i></div>
            <textarea v-model="values.description" name="description" id="description" placeholder="Опис" required></textarea>
            <label for="description">Опис</label>
            <p class="hint error">{{ errors.description }}</p>
        </div>

        <!-- Rich text editor for puzzle content using QuillEditor component -->
        <div class="form-group">
            <div><i class="fa-solid fa-file-lines"></i></div>
            <QuillEditor :onUpdate="(val) => {values.content = val}"/>
            <label for="content">Текст задачі</label>
            <p class="hint error">{{ errors.content }}</p>
        </div>

        <!-- Time limit in seconds (supports decimal values) -->
        <div class="form-group">
            <div><i class="fa-solid fa-clock"></i></div>
            <input type="text" v-model="values.timeLimit" name="timeLimit" id="timeLimit" placeholder="Обмеження часу" required>
            <label for="timeLimit">Обмеження часу</label>
            <p class="hint error">{{ errors.timeLimit }}</p>
        </div>

        <!-- Visibility toggle - only available if user has PUBLISH_PUZZLE permission -->
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
        
        <!-- Test cases section for NON_INTERACTIVE and OUTPUT_CHECKING puzzle types -->
        <template v-if="values.puzzleType === 'NON_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING'">
            <div v-for="(testCase, index) in values.testCases" :key="index" class="test-case-group">
                <br>
                <h3>Тест кейс #{{ index + 1 }}</h3>
                <br>

                <!-- Test input data -->
                <div class="form-group">
                    <div><i class="fa-solid fa-keyboard"></i></div>
                    <textarea v-model="testCase.input" :name="'input' + index" :id="'input' + index" placeholder="Вхідні дані" required></textarea>
                    <label :for="'input' + index">Вхідні дані</label>
                    <p class="hint error">{{ errors.testCases && errors.testCases[index]?.input }}</p>
                </div>

                <!-- For OUTPUT_CHECKING: option to use checker instead of static expected output -->
                <template v-if="values.puzzleType === 'OUTPUT_CHECKING'">
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

                <!-- Expected output (hidden if OUTPUT_CHECKING uses checker for this test) -->
                <div class="form-group" v-if="values.puzzleType === 'NON_INTERACTIVE' || (values.puzzleType === 'OUTPUT_CHECKING' && !testCase.useChecker)">
                    <div><i class="fa-solid fa-keyboard"></i></div>
                    <textarea v-model="testCase.output" :name="'output' + index" :id="'output' + index" placeholder="Вихідні дані" :required="values.puzzleType === 'NON_INTERACTIVE'"></textarea>
                    <label :for="'output' + index">Вихідні дані</label>
                    <p class="hint error">{{ errors.testCases && errors.testCases[index]?.output }}</p>
                </div>

                <!-- Points awarded for this test case -->
                <div class="form-group">
                    <div><i class="fa-solid fa-star"></i></div>
                    <input type="text" v-model="testCase.score" :name="'score' + index" :id="'score' + index" placeholder="Бали за тест" required>
                    <label :for="'score' + index">Бали за тест</label>
                    <p class="hint error">{{ errors.testCases && errors.testCases[index]?.score }}</p>
                </div>
                <br>

                <!-- Delete test case button -->
                <LinkBtn role="btn" type="button" img="wave2.svg" @click="removeTestCase(index)"><i class="fa-solid fa-trash"></i> Видалити</LinkBtn>
            </div>
            <br>
            <p class="hint error">{{ errors.testCasesErr }}</p>
            <br>

            <!-- Add new test case button -->
            <LinkBtn role="btn" type="button" @click="addTestCase"><i class="fa-regular fa-plus"></i> Додати тест</LinkBtn>
            <p class="hint hint-message">Підказка: Встановіть 0 балів за тест кейс, щоб він був показан як приклад</p>
            <br>
            <br>
            <br>

            <!-- Display total points across all non-example tests -->
            <div class="form-group">
                <div><i class="fa-solid fa-star"></i></div>
                <input type="text" v-model="total_score" id="total_score" placeholder="Бали за задачу" disabled>
                <label for="total_score">Бали за задачу</label>
            </div>
            <br>

            <!-- Display count of example tests (tests with 0 points) -->
            <div class="form-group">
                <div><i class="fa-solid fa-chalkboard-user"></i></div>
                <input type="text" v-model="values.testCases.filter(testCase => parseFloat(testCase.score) == 0).length" id="example_testcases_count" placeholder="Кількіть тест кейсів, що будуть показані як приклади" disabled>
                <label for="example_testcases_count">Кількіть тест кейсів, що будуть показані як приклади (це всі тести з 0 балами)</label>
            </div>
        </template>

        <!-- Checker configuration for FULL_INTERACTIVE and OUTPUT_CHECKING puzzles -->
        <template v-if="values.puzzleType === 'FULL_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING'">
            <br>
            <h3>Налаштування {{ values.puzzleType === 'FULL_INTERACTIVE' ? 'інтерактивної задачі' : 'динамічної перевірки' }}</h3>
            <br>
            
            <!-- Choose between creating new checker or reusing existing one -->
            <div class="form-group">
                <div><i class="fa-solid fa-toggle-on"></i></div>
                <select v-model="values.checkerMode" name="checkerMode" id="checkerMode" required>
                    <option value="">Оберіть режим</option>
                    <option value="new">Створити новий checker</option>
                    <option value="existing">Використати існуючий checker</option>
                </select>
                <label for="checkerMode">Режим checker'а</label>
                <p class="hint error">{{ errors.checkerMode }}</p>
            </div>

            <!-- New checker creation form -->
            <template v-if="values.checkerMode === 'new'">
                <!-- Checker name input -->
                <div class="form-group">
                    <div><i class="fa-solid fa-signature"></i></div>
                    <input type="text" v-model="values.checkerName" name="checkerName" id="checkerName" placeholder="Назва checker'а" required>
                    <label for="checkerName">Назва checker'а</label>
                    <p class="hint error">{{ errors.checkerName }}</p>
                </div>

                <!-- Programming language selection -->
                <div class="form-group">
                    <div><i class="fa-solid fa-code"></i></div>
                    <select v-model="values.language" name="language" id="language" required>
                        <option value="">Оберіть мову програмування</option>
                        <option v-for="lang in checkerLanguages" :key="lang.server_id" :value="lang.server_id">{{ lang.name }}</option>
                    </select>
                    <label for="language">Мова програмування</label>
                    <p class="hint error">{{ errors.language }}</p>
                </div>

                <!-- File upload for checker source code -->
                <div class="form-group-custom">
                    <p class="label">Файл з кодом checker'а</p>
                    <FileUploader v-model="values.checkerFile" placeholder="Виберіть файл з кодом" button-text="Завантажити" :multiple="false" :show-previews="true"/>
                    <p class="hint error">{{ errors.checkerFile }}</p>
                </div>

                <div class="form-group-custom">
                    <CheckerCreateNote/>
                </div>
            </template>

            <!-- Existing checker selection -->
            <template v-if="values.checkerMode === 'existing'">
                <ApiSearchSelector
                    v-model="values.existingChecker"
                    apiUrl="/checker/autocomplete"
                    label="Існуючі checker'и"
                    placeholder="Пошук checker..."
                />
                <p class="hint error">{{ errors.existingChecker }}</p>
            </template>

            <!-- Maximum score for FULL_INTERACTIVE puzzles only -->
            <div class="form-group" v-if="values.puzzleType === 'FULL_INTERACTIVE'">
                <div><i class="fa-solid fa-star"></i></div>
                <input type="text" v-model="values.maxScore" name="maxScore" id="maxScore" placeholder="Максимальні бали за задачу" required>
                <label for="maxScore">Максимальні бали за задачу</label>
                <p class="hint error">{{ errors.maxScore }}</p>
            </div>
        </template>

        <br>
        <!-- Submit form button -->
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!form_valid"><i class="fa-regular fa-square-plus"></i> Додати</LinkBtn>
    </form>

    <Modal 
        v-model:visible="modalInfo.visible"
        :type="modalInfo.type" 
        :title="modalInfo.title"
        :text="modalInfo.text"
        :show-cancel-button="modalInfo.showCancelButton"
    />
</template>

<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

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
import { useRouter } from 'vue-router'
import LinkBtn from '@/components/LinkBtn.vue'
import apiClient from '@/axios.js'
import QuillEditor from '@/components/QuillEditor.vue'
import FileUploader from '../components/FileUploader.vue'
import Modal from '../components/Modal.vue'
import { loadJsonStaticResource } from '@/services/resourceService.js'
import CheckerCreateNote from '../components/CheckerCreateNote.vue'
import { hasGlobalPermission } from '../services/permissionService'
import ApiSearchSelector from '@/components/ApiSearchSelector.vue'
import { handleApiError } from '@/services/errorHandler.js'

const store = useStore()
const router = useRouter()

// Form data structure with all puzzle configuration options
const values = reactive({
    puzzleType: '',
    title: '',
    description: '',
    content: '',
    timeLimit: '',
    testCases: [
        {
            input: '',
            output: '',
            score: 0,
            useChecker: true,
        }
    ],
    checkerMode: '',
    checkerName: '',
    language: '',
    checkerFile: null,
    existingChecker: '',
    maxScore: '',
    visible: hasGlobalPermission('PUBLISH_PUZZLE')
})

// Validation errors object, mirroring the structure of values
const errors = reactive({
    puzzleType: '',
    title: '',
    description: '',
    content: '',
    timeLimit: '',
    testCases: [
        {
            input: '',
            output: '',
            score: '',
            type: '',
        }
    ],
    testCasesErr: '',
    checkerMode: '',
    checkerName: '',
    language: '',
    checkerFile: '',
    existingChecker: '',
    maxScore: '',
    visible: ''
})

// Modal dialog state for displaying validation or error messages
const modalInfo = reactive({
    title: '',
    text: '',
    visible: false,
    type: 'info',
    showCancelButton: false
})

const form_valid = ref(false)

// Available programming languages for checker compilation
const checkerLanguages = computed(() => store.getters.checkerLanguages || [])

// Calculates total score across all non-example test cases
const total_score = computed(() => {
    if (values.puzzleType === 'FULL_INTERACTIVE') return 0
    let score = 0
    for (let i = 0; i < values.testCases.length; i++) {
        if (isFloat(values.testCases[i].score))
            score += parseFloat(values.testCases[i].score)
    }
    return score
})

// Validates if a string represents a valid floating-point number
const isFloat = (str) => {
    return !isNaN(str) && !isNaN(parseFloat(str))
}

// Adds a new empty test case to the form
const addTestCase = () => {
    if (!values.testCases) {
        values.testCases = []
    }
    values.testCases.push({
        input: '',
        output: '',
        score: 0,
        useChecker: true
    })
    errors.testCases.push({
        input: '',
        output: '',
        score: ''
    })
}

// Removes a test case by index (minimum 1 test case must remain)
const removeTestCase = (index) => {
    if (values.testCases.length > 1) {
        values.testCases.splice(index, 1)
        errors.testCases.splice(index, 1)
    }
}

// Sends checker file to backend for compilation and returns the compiled checker ID
const compileChecker = async () => {
    if (!values.checkerFile) {
        throw new Error('Файл checker\'а не вибрано')
    }

    const formData = new FormData()
    formData.append('file', values.checkerFile)
    formData.append('languageId', values.language)
    formData.append('name', values.checkerName)

    try {
        const response = await apiClient.post('/checker/create', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })

        return response.data
    } catch (error) {
        handleApiError(error, "Помилка компіляції checker'а");
        return undefined;
    }
}

// Main form submission handler - compiles checker if needed and creates puzzle with test cases
const addPuzzle = async () => {
    if (!form_valid.value) return

    try {
        let checkerId = null
        
        // For interactive or dynamic-checking puzzles, compile or select checker
        if (values.puzzleType === 'FULL_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING') {
            if (values.checkerMode === 'new') {
                // Compile new checker from uploaded source file
                const compileResult = await compileChecker()
                if (compileResult == null) {
                    return;
                }

                checkerId = compileResult.checkerId
                // Refresh checker list after compilation
                await loadJsonStaticResource("checkerLanguages")
            } else {
                checkerId = values.existingChecker
            }
        }

        const requestData = {
            title: values.title,
            description: values.description,
            content: values.content,
            timeLimit: values.timeLimit,
            taskType: values.puzzleType,
            visible: values.visible
        }

        // For test-based puzzles, prepare test case data
        if (values.puzzleType === 'NON_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING') {
            // Output is null for OUTPUT_CHECKING if useChecker is enabled (checker validates output)
            requestData.puzzleData = values.testCases.map(testCase => ({
                input: testCase.input,
                output: (values.puzzleType === 'OUTPUT_CHECKING' && testCase.useChecker) ? null : testCase.output,
                score: parseFloat(testCase.score)
            }))
        }

        // Set score based on puzzle type
        if (values.puzzleType === 'FULL_INTERACTIVE') {
            requestData.score = parseFloat(values.maxScore)
        } else if (values.puzzleType === 'OUTPUT_CHECKING') {
            requestData.score = total_score.value
        }

        if (checkerId) {
            requestData.checkerId = checkerId
        }

        const response = await apiClient.post('/puzzles/add', requestData)
        
        if (response.data['success']) {
            store.dispatch('addSuccessMessage', response.data['success'])
            router.push('/puzzles')
        }
    } catch (error) {
        handleApiError(error, "Не вдалося додати задачу", errors);
    }
}

// Comprehensive form validation - runs on every reactive value change
const validateForm = () => {
    form_valid.value = true
    
    if (!values.puzzleType) {
        errors.puzzleType = 'Оберіть тип задачі'
        form_valid.value = false
    } else {
        errors.puzzleType = ''
    }

    if (values.title.length < 5 || values.title.length > 100) {
        errors.title = 'Назва повинна містити від 5 до 100 символів'
        form_valid.value = false
    } else {
        errors.title = ''
    }

    if (values.description.length < 5 || values.description.length > 300) {
        errors.description = 'Опис повинен містити від 5 до 300 символів'
        form_valid.value = false
    } else {
        errors.description = ''
    }

    if (values.content.length < 20 || values.content.length > 7000) {
        errors.content = 'Текст задачі повинен містити від 20 до 7000 символів'
        form_valid.value = false
    } else {
        errors.content = ''
    }

    const timeLimit = parseFloat(values.timeLimit)
    if (!isFloat(values.timeLimit) || timeLimit < 0.2 || timeLimit > 10) {
        errors.timeLimit = 'Обмеження часу повинно бути від 0.2 до 10'
        form_valid.value = false
    } else {
        errors.timeLimit = ''
    }

    // Validate test cases for test-based puzzle types
    if (values.puzzleType === 'NON_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING') {
        if (values.testCases.length < 2) {
            errors.testCasesErr = 'Тести повинні бути принаймні 2'
            form_valid.value = false
        } else if (values.testCases.filter(testCase => testCase.score == 0).length > 30) {
            errors.testCasesErr = 'Ви не можете мати більше 30 тестів з нульовою оцінкою'
            form_valid.value = false
        } else {
            errors.testCasesErr = ''
        }

        // Validate each test case
        for (let i = 0; i < values.testCases.length; i++) {
            if (values.testCases[i].input.length > 200) {
                errors.testCases[i].input = 'Вхідні дані повинні містити від 0 до 200 символів'
                form_valid.value = false
            } else {
                errors.testCases[i].input = ''
            }

            const needsOutput = values.puzzleType === 'NON_INTERACTIVE' || (values.puzzleType === 'OUTPUT_CHECKING' && !values.testCases[i].useChecker)
            if (needsOutput && values.testCases[i].output.length > 200) {
                errors.testCases[i].output = 'Вихідні дані повинні містити від 0 до 200 символів'
                form_valid.value = false
            } else {
                errors.testCases[i].output = ''
            }

            const score = parseFloat(values.testCases[i].score)
            if (!isFloat(values.testCases[i].score) || score < 0 || score > 10) {
                errors.testCases[i].score = 'Оцінка повинна бути від 0 до 10'
                form_valid.value = false
            } else if (isFloat(values.testCases[i].score) && String(values.testCases[i].score).split(".")[1]?.length > 5) {
                errors.testCases[i].score = 'Оцінка не може містити більше 5 знаків після коми'
                form_valid.value = false
            } else if (i == 0 && score != 0) {
                errors.testCases[i].score = 'Оцінка першого тесту повинна бути 0, тому що він буде показаний як приклад'
                form_valid.value = false
            } else {
                errors.testCases[i].score = ''
            }
        }
    }

    // Validate checker configuration for interactive/dynamic-checking puzzles
    if (values.puzzleType === 'FULL_INTERACTIVE' || values.puzzleType === 'OUTPUT_CHECKING') {
        if (!values.checkerMode) {
            errors.checkerMode = 'Оберіть режим checker\'а'
            form_valid.value = false
        } else {
            errors.checkerMode = ''
        }

        if (values.checkerMode === 'new') {
            if (values.checkerName.length < 1 || values.checkerName.length > 100) {
                errors.checkerName = 'Назва checker\'а повинна містити від 1 до 100 символів'
                form_valid.value = false
            } else {
                errors.checkerName = ''
            }

            if (!values.language) {
                errors.language = 'Оберіть мову програмування'
                form_valid.value = false
            } else {
                errors.language = ''
            }

            if (!values.checkerFile) {
                errors.checkerFile = 'Завантажте файл з кодом checker\'а'
                form_valid.value = false
            } else {
                errors.checkerFile = ''
            }
        } else if (values.checkerMode === 'existing') {
            if (!values.existingChecker) {
                errors.existingChecker = 'Оберіть існуючий checker'
                form_valid.value = false
            } else {
                errors.existingChecker = ''
            }
        }

        if (values.puzzleType === 'FULL_INTERACTIVE') {
            const maxScore = parseFloat(values.maxScore)
            if (!isFloat(values.maxScore) || maxScore <= 0 || maxScore > 100) {
                errors.maxScore = 'Максимальні бали повинні бути від 0.1 до 100'
                form_valid.value = false
            } else {
                errors.maxScore = ''
            }
        }
    }
}

// Re-validate on every reactive value change
watch(values, () => {
    validateForm()
}, { deep: true })

// Load available programming languages on component mount
onMounted(async () => {
    await loadJsonStaticResource("checkerLanguages")
})
</script>
