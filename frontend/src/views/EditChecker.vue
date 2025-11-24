<template>
    <h1>Редагування checker'а</h1>
    <form @submit.prevent="updateChecker" class="main-form" v-if="!loading">
        <!-- Поле для назви checker'а -->
        <div class="form-group">
            <div><i class="fa-solid fa-signature"></i></div>
            <input type="text" v-model="values.name" name="name" id="name" placeholder="Назва checker'а" required>
            <label for="name">Назва checker'а</label>
            <p class="hint error">{{ errors.name }}</p>
        </div>

        <!-- Вибір мови програмування -->
        <div class="form-group">
            <div><i class="fa-solid fa-code"></i></div>
            <select v-model="values.languageId" name="languageId" id="languageId" required>
                <option value="">Оберіть мову програмування</option>
                <option v-for="lang in checkerLanguages" :key="lang.server_id" :value="lang.server_id">{{ lang.name }}</option>
            </select>
            <label for="languageId">Мова програмування</label>
            <p class="hint error">{{ errors.languageId }}</p>
        </div>

        <!-- Показуємо поточний файл -->
        <div class="form-group-custom" v-if="currentFileName">
            <p class="label">Поточний файл: <strong>{{ currentFileName }}</strong></p>
        </div>

        <!-- Поле для завантаження нового файлу з кодом -->
        <div class="form-group-custom">
            <p class="label">Новий файл з кодом checker'а (залишіть порожнім, щоб зберегти поточний)</p>
            <FileUploader v-model="values.file" placeholder="Виберіть новий файл з кодом" button-text="Завантажити" :multiple="false" :show-previews="true"/>
            <p class="hint error">{{ errors.file }}</p>
        </div>

        <div class="form-group-custom">
            <CheckerCreateNote/>
        </div>

        <br>
        <!-- Кнопка для відправки форми -->
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!form_valid || submitting">
            <i class="fa-regular fa-save"></i> {{ submitting ? 'Збереження...' : 'Зберегти' }}
        </LinkBtn>

        <br><br>
        
        <!-- Кнопка для видалення задачі -->
        <LinkBtn role="btn" type="button" img="wave2.svg" anim="bg-scale" @click="showDeleteConfirmation">
            <i class="fa-solid fa-trash"></i> Видалити checker
        </LinkBtn>
    </form>

    <Preloader v-else/>

    <Modal 
        v-model:visible="modalInfo.visible"
        :type="modalInfo.type" 
        :title="modalInfo.title"
        :text="modalInfo.text"
        :show-cancel-button="modalInfo.showCancelButton"
        @confirm="modalInfo.confirmAction"
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

.loading {
    text-align: center;
    padding: 2rem;
    font-size: 1.2rem;
}

.checker-info {
    background: var(--bg-secondary, #f5f5f5);
    padding: 1rem;
    border-radius: 8px;
    margin: 1rem 0;
    
    p {
        margin: 0.5rem 0;
    }
    
    .status {
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        font-weight: bold;
        text-transform: uppercase;
        
        &.active {
            background: #d4edda;
            color: #155724;
        }
        
        &.pending {
            background: #fff3cd;
            color: #856404;
        }
        
        &.failed {
            background: #f8d7da;
            color: #721c24;
        }
        
        &.inactive {
            background: #e2e3e5;
            color: #383d41;
        }
    }
    
    .compilation-error {
        code {
            display: block;
            background: #f8f9fa;
            padding: 0.5rem;
            border-radius: 4px;
            font-family: monospace;
            white-space: pre-wrap;
            max-height: 200px;
            overflow-y: auto;
            margin-top: 0.5rem;
        }
    }
}
</style>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import LinkBtn from '@/components/LinkBtn.vue'
import apiClient from '@/axios.js'
import FileUploader from '../components/FileUploader.vue'
import Modal from '../components/Modal.vue'
import CheckerCreateNote from '../components/CheckerCreateNote.vue'
import { loadJsonStaticResource } from '@/services/resourceService.js'
import Preloader from "@/components/Preloader.vue"

const store = useStore()
const router = useRouter()
const route = useRoute()

// Реактивні дані
const values = reactive({
    name: '',
    languageId: '',
    file: null
})

const errors = reactive({
    name: '',
    languageId: '',
    file: ''
})

const modalInfo = reactive({
    title: '',
    text: '',
    visible: false,
    type: 'info',
    showCancelButton: false,
    confirmAction: null,
})

const form_valid = ref(false)
const loading = ref(true)
const submitting = ref(false)
const currentFileName = ref('')

// Computed properties
const checkerLanguages = computed(() => store.getters.checkerLanguages || [])

// Methods
const loadChecker = async () => {
    try {
        loading.value = true
        const checkerId = route.params.id
        const response = await apiClient.get(`/checker/get-edit/${checkerId}`)
        
        const checker = response.data
        values.name = checker.name
        values.languageId = checker.language
        currentFileName.value = checker.fileName || ''
        
    } catch (error) {
        handleError(error)
    } finally {
        loading.value = false
    }
}

const updateChecker = async () => {
    if (!form_valid.value || submitting.value) return

    try {
        submitting.value = true
        const checkerId = route.params.id
        
        const formData = new FormData()
        formData.append('name', values.name)
        formData.append('languageId', values.languageId)
        
        // Додаємо файл тільки якщо він був обраний
        if (values.file) {
            formData.append('file', values.file)
        }

        const response = await apiClient.put(`/checker/edit/${checkerId}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })

        store.dispatch('addSuccessMessage', 'Checker успішно оновлено')
        
        router.push(`/checker/${checkerId}`)
    } catch (error) {
        handleError(error)
    } finally {
        submitting.value = false
    }
}

const handleError = (error) => {
    // Очищуємо попередні помилки
    Object.keys(errors).forEach(key => {
        errors[key] = ''
    })

    if (error.response && error.response.data) {
        const errorData = error.response.data
        
        if (errorData.code === 'VALIDATION_ERROR' && errorData.details && errorData.details.fields) {
            // Обробка помилок валідації
            const fields = errorData.details.fields
            
            Object.keys(fields).forEach(fieldName => {
                if (errors.hasOwnProperty(fieldName)) {
                    errors[fieldName] = fields[fieldName]
                }
            })
        } else {
            // Інші помилки
            modalInfo.title = "Помилка"
            modalInfo.text = errorData.error || 'Сталася помилка при оновленні checker\'а'
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

const validateForm = () => {
    form_valid.value = true
    
    // Перевірка назви checker'а
    if (values.name.length < 1 || values.name.length > 100) {
        errors.name = 'Назва checker\'а повинна містити від 1 до 100 символів'
        form_valid.value = false
    } else {
        errors.name = ''
    }

    // Перевірка мови програмування
    if (!values.languageId) {
        errors.languageId = 'Оберіть мову програмування'
        form_valid.value = false
    } else {
        errors.languageId = ''
    }

    // Файл не є обов'язковим при редагуванні
    errors.file = ''
}

const showDeleteConfirmation = () => {
    modalInfo.title = "Підтвердження видалення"
    modalInfo.text = "Ви впевнені, що хочете видалити цей checker? Цю дію неможливо буде скасувати."
    modalInfo.type = "warning"
    modalInfo.showCancelButton = true
    modalInfo.confirmAction = async () => await deleteChecker();
    modalInfo.visible = true
}

const deleteChecker = async () => {
    try {
        await apiClient.delete(`/checker/delete/${route.params.id}`)
        
        store.dispatch('addSuccessMessage', 'Checker успішно видалено')
        router.push('/checkers')
    
        modalInfo.visible = false
    } catch (error) {
        console.error("Error deleting checker: ", error)
        if (error.response && error.response.data && error.response.data['error']) {
            modalInfo.title = "Помилка видалення checker"
            modalInfo.text = error.response.data['error']
            modalInfo.showCancelButton = false
            modalInfo.confirmAction = null;
            modalInfo.visible = true;
            modalInfo.type = "error";
        }
    }
}

// Watchers
watch(values, () => {
    validateForm()
}, { deep: true })

// Lifecycle hooks
onMounted(async () => {
    await loadJsonStaticResource("checkerLanguages")
    await loadChecker()
    validateForm()
})
</script>
