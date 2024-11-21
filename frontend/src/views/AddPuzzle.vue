<!-- Основна форма для додавання нової задачі -->
<template>
    <h1>Додавання задачі</h1>
    <form @submit.prevent="addPuzzle" class="main-form">
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
            <QuillEditor :onUpdate="(val) => {this.values.content = val}"/>
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
        
        <!-- Секція для тест кейсів -->
        <div v-for="(testCase, index) in values.testCases" :key="index" class="test-case-group">
            <br>
            <h3>Тест кейс #{{ index + 1 }}</h3>
            <br>
            <!-- Поле для введення вхідних даних тесту -->
            <div class="form-group">
                <div><i class="fa-solid fa-keyboard"></i></div>
                <textarea v-model="testCase.input" :name="'input' + index" :id="'input' + index" placeholder="Вхідні дані" required></textarea>
                <label :for="'input' + index">Вхідні дані</label>
                <p class="hint error">{{ errors.testCases && errors.testCases[index]?.input }}</p>
            </div>
            <!-- Поле для введення очікуваних вихідних даних -->
            <div class="form-group">
                <div><i class="fa-solid fa-keyboard"></i></div>
                <textarea v-model="testCase.output" :name="'output' + index" :id="'output' + index" placeholder="Вихідні дані" required></textarea>
                <label :for="'output' + index">Вихідні дані</label>
                <p class="hint error">{{ errors.testCases && errors.testCases[index]?.output }}</p>
            </div>
            <!-- Поле для встановлення балів за тест -->
            <div class="form-group">
                <div><i class="fa-solid fa-star"></i></div>
                <input type="text" v-model="testCase.score" :name="'score' + index" :id="'score' + index" placeholder="Бали за тест" required>
                <label :for="'score' + index">Бали за тест</label>
                <p class="hint error">{{ errors.testCases && errors.testCases[index]?.score }}</p>
            </div>
            <br>
            <!-- Кнопка для видалення тест кейсу -->
            <LinkBtn role="btn" type="button" img="wave2.svg" @click="removeTestCase(index)"><i class="fa-solid fa-trash"></i> Видалити</LinkBtn>
        </div>
        <br>
        <p class="hint error">{{ errors.testCasesErr }}</p>
        <br>
        <!-- Кнопка для додавання нового тест кейсу -->
        <LinkBtn role="btn" type="button" @click="addTestCase"><i class="fa-regular fa-plus"></i> Додати тест</LinkBtn>
        <br>
        <br>
        <br>
        <!-- Поле для відображення загальної кількості балів -->
        <div class="form-group">
            <div><i class="fa-solid fa-star"></i></div>
            <input type="text" v-model="total_score" id="total_score" placeholder="Бали за задачу" disabled>
            <label for="total_score">Бали за задачу</label>
        </div>
        <br>
        <!-- Кнопка для відправки форми -->
        <LinkBtn role="btn" type="submit" img="waves1.svg" bold="true" anim="go" :disabled="!form_valid"><i class="fa-regular fa-square-plus"></i> Додати</LinkBtn>
    </form>
</template>

<!-- Імпортуємо стандартні стилі форми -->
<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>

<style lang="scss" scoped>
// Встановлюємо ширину форми та заголовка
form.main-form, h1 {
    width: 100%;
    max-width: 800px;
}
</style>

<script scoped>
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '@/axios.js';
import { mapActions } from 'vuex';
import QuillEditor from '@/components/QuillEditor.vue';

export default {
    name: 'AddPuzzle',
    // Реєструємо компоненти, які використовуються в шаблоні
    components: {
        LinkBtn,
        QuillEditor,
    },
    // Визначаємо початковий стан компонента
    data() {
        return {
            // Об'єкт для зберігання значень форми
            values: {
                title: '',
                description: '',
                content: '',
                timeLimit: '',
                testCases: [
                    {
                        input: '',
                        output: '',
                        score: 1,
                    }
                ]
            },
            // Об'єкт для зберігання помилок валідації
            errors: {
                title: '',
                description: '',
                content: '',
                timeLimit: '',
                testCases: [
                    {
                        input: '',
                        output: '',
                        score: '',
                    }
                ],
                testCasesErr: '',
            },
            form_valid: false,
        }
    },
    // Обчислювані властивості
    computed: {
        // Підрахунок загальної кількості балів за всі тести
        total_score() {
            let score = 0;
            for (let i = 0; i < this.values.testCases.length; i++) {
                if (this.isFloat(this.values.testCases[i].score))
                    score += parseFloat(this.values.testCases[i].score);
            }
            return score;
        }
    },
    methods: {
        // Підключаємо actions з Vuex
        ...mapActions(['addSuccessMessage', 'addErrorMessage']),
        // Перевірка чи є рядок числом з плаваючою крапкою
        isFloat(str) {
            return !isNaN(str) && !isNaN(parseFloat(str));
        },
        // Додавання нового тест кейсу
        addTestCase() {
            if (!this.values.testCases) {
                this.values.testCases = [];
            }
            this.values.testCases.push({
                input: '',
                output: ''
            });
            this.errors.testCases.push({
                input: '',
                output: '',
                score: ''
            });
        },
        // Видалення тест кейсу за індексом
        removeTestCase(index) {
            if (this.values.testCases.length > 1) {
                this.values.testCases = this.values.testCases.filter((_, i) => i !== index);
                this.errors.testCases = this.errors.testCases.filter((_, i) => i !== index);
            }
        },
        // Відправка форми на сервер
        async addPuzzle() {
            if (this.form_valid) {
                await apiClient.post('/puzzles/add', {
                    title: this.values.title,
                    description: this.values.description,
                    content: this.values.content,
                    timeLimit: this.values.timeLimit,
                    puzzleData: this.values.testCases,
                })
                    .then((response) => {
                        if (response.data['success']) {
                            this.addSuccessMessage(response.data['success']);
                            this.$router.push('/puzzles');
                        }
                    })
                    .catch((error) => {
                        if (error.response && error.response.data && error.response.data['errors']) {
                            for (let key in error.response.data['errors']) {
                                this.addErrorMessage(error.response.data['errors'][key]);
                            }
                        }
                        if (error.response.status === 500) {
                            this.addErrorMessage('Помилка сервера');
                        }

                        console.error("Error in request: ", error);
                    });
            }
        },
        // Валідація форми
        validateForm() {
            this.form_valid = true;
            
            // Перевірка назви
            if (this.values.title.length < 5 || this.values.title.length > 100) {
                this.errors.title = 'Назва повинна містити від 5 до 100 символів';
                this.form_valid = false;
            } else {
                this.errors.title = '';
            }

            // Перевірка опису
            if (this.values.description.length < 5 || this.values.description.length > 300) {
                this.errors.description = 'Опис повинен містити від 5 до 300 символів';
                this.form_valid = false;
            } else {
                this.errors.description = '';
            }

            // Перевірка тексту задачі
            if (this.values.content.length < 20 || this.values.content.length > 7000) {
                this.errors.content = 'Текст задачі повинен містити від 20 до 7000 символів';
                this.form_valid = false;
            } else {
                this.errors.content = '';
            }

            // Перевірка обмеження часу
            const timeLimit = parseFloat(this.values.timeLimit);
            if (!this.isFloat(this.values.timeLimit) || timeLimit < 0.2 || timeLimit > 10) {
                this.errors.timeLimit = 'Обмеження часу повинно бути від 0.2 до 10';
                this.form_valid = false;
            } else {
                this.errors.timeLimit = '';
            }

            // Перевірка кількості тест кейсів
            if (this.values.testCases.length < 2) {
                this.errors.testCasesErr = 'Тести повинні бути принаймні 2';
                this.form_valid = false;
            } else {
                this.errors.testCasesErr = '';
            }

            // Перевірка кожного тест кейсу
            for (let i = 0; i < this.values.testCases.length; i++) {
                if (this.values.testCases[i].input.length > 200) {
                    this.errors.testCases[i].input = 'Вхідні дані повинні містити від 0 до 200 символів';
                    this.form_valid = false;
                } else {
                    this.errors.testCases[i].input = '';
                }
                if (this.values.testCases[i].output.length > 200) {
                    this.errors.testCases[i].output = 'Вихідні дані повинні містити від 0 до 200 символів';
                    this.form_valid = false;
                } else {
                    this.errors.testCases[i].output = '';
                }
                const score = parseFloat(this.values.testCases[i].score);
                if (!this.isFloat(this.values.testCases[i].score) || score < 0.1 || score > 10) {
                    this.errors.testCases[i].score = 'Оцінка повинна бути від 0.1 до 10';
                    this.form_valid = false;
                } else {
                    this.errors.testCases[i].score = '';
                }
            }
        }
    },
    // Спостерігач за змінами у формі
    watch: {
        'values': {
            handler() {
                this.validateForm();
            },
            deep: true
        }
    },
}
</script>