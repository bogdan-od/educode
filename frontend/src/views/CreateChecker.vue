<template>
    <h1>Створення checker'а</h1>
    <form @submit.prevent="createChecker" class="main-form">
        <!-- Checker name input field -->
        <div class="form-group">
            <div><i class="fa-solid fa-signature"></i></div>
            <input type="text" v-model="values.name" name="name" id="name" placeholder="Назва checker'а" required>
            <label for="name">Назва checker'а</label>
            <p class="hint error">{{ errors.name }}</p>
        </div>

        <!-- Programming language selection for compilation -->
        <div class="form-group">
            <div><i class="fa-solid fa-code"></i></div>
            <select v-model="values.languageId" name="languageId" id="languageId" required>
                <option value="">Оберіть мову програмування</option>
                <option v-for="lang in checkerLanguages" :key="lang.server_id" :value="lang.server_id">{{ lang.name }}</option>
            </select>
            <label for="languageId">Мова програмування</label>
            <p class="hint error">{{ errors.languageId }}</p>
        </div>

        <!-- File upload for checker source code -->
        <div class="form-group-custom">
            <p class="label">Файл з кодом checker'а</p>
            <FileUploader v-model="values.file" placeholder="Виберіть файл з кодом" button-text="Завантажити" :multiple="false" :show-previews="true"/>
            <p class="hint error">{{ errors.file }}</p>
        </div>

        <!-- Informational component about checker requirements and constraints -->
        <div class="form-group-custom">
            <CheckerCreateNote/>
        </div>

        <br>
        <!-- Form submission button -->
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!form_valid || loading">
            <i class="fa-regular fa-square-plus"></i> {{ loading ? 'Створення...' : 'Створити' }}
        </LinkBtn>
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

.label {
    font-family: $form-font;
    font-size: 1rem;
    margin-bottom: 0.5rem;
    color: var(--text-color);
}
</style>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import LinkBtn from '@/components/LinkBtn.vue'
import apiClient from '@/axios.js'
import FileUploader from '../components/FileUploader.vue'
import Modal from '../components/Modal.vue'
import CheckerCreateNote from '../components/CheckerCreateNote.vue'
import { loadJsonStaticResource } from '@/services/resourceService.js'

const store = useStore()
const router = useRouter()

// Form data for checker creation
const values = reactive({
    name: '',
    languageId: '',
    file: null
})

// Validation errors corresponding to form fields
const errors = reactive({
    name: '',
    languageId: '',
    file: ''
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
const loading = ref(false)

// Available programming languages for checker compilation
const checkerLanguages = computed(() => store.getters.checkerLanguages || [])

// Sends checker file to backend for compilation and creates checker resource
const createChecker = async () => {
    if (!form_valid.value || loading.value) return

    try {
        loading.value = true
        
        if (!values.file) {
            throw new Error('Файл checker\'а не вибрано')
        }

        const formData = new FormData()
        formData.append('file', values.file)
        formData.append('languageId', values.languageId)
        formData.append('name', values.name)

        const response = await apiClient.post('/checker/create', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })

        store.dispatch('addSuccessMessage', 'Checker успішно створено')
        router.push('/checkers')
    } catch (error) {
        handleError(error)
    } finally {
        loading.value = false
    }
}

// Handles API errors and populates corresponding error messages in the form
const handleError = (error) => {
    // Clear previous errors
    Object.keys(errors).forEach(key => {
        errors[key] = ''
    })

    if (error.response && error.response.data) {
        const errorData = error.response.data
        
        if (errorData.code === 'VALIDATION_ERROR' && errorData.details && errorData.details.fields) {
            // Populate field-specific validation errors
            const fields = errorData.details.fields
            
            Object.keys(fields).forEach(fieldName => {
                if (errors.hasOwnProperty(fieldName)) {
                    errors[fieldName] = fields[fieldName]
                }
            })
        } else {
            // Display generic error in modal
            modalInfo.title = "Помилка"
            modalInfo.text = errorData.error || 'Сталася помилка при створенні checker\'а'
            modalInfo.type = "error"
            modalInfo.visible = true
        }
    } else if (error.response && error.response.status === 500) {
        store.dispatch('addErrorMessage', 'Помилка сервера')
    } else {
        store.dispatch('addErrorMessage', error.message || 'Невідома помилка')
    }
    console.error("Error in request: ", error)
}

// Validates form fields and updates form_valid flag accordingly
const validateForm = () => {
    form_valid.value = true
    
    if (values.name.length < 1 || values.name.length > 100) {
        errors.name = 'Назва checker\'а повинна містити від 1 до 100 символів'
        form_valid.value = false
    } else {
        errors.name = ''
    }

    if (!values.languageId) {
        errors.languageId = 'Оберіть мову програмування'
        form_valid.value = false
    } else {
        errors.languageId = ''
    }

    if (!values.file) {
        errors.file = 'Завантажте файл з кодом checker\'а'
        form_valid.value = false
    } else {
        errors.file = ''
    }
}

// Re-validate on every reactive value change
watch(values, () => {
    validateForm()
}, { deep: true })

// Load available programming languages on component mount
onMounted(async () => {
    await loadJsonStaticResource("checkerLanguages")
    validateForm()
})
</script>
