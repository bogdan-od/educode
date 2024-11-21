<!-- Головний компонент реєстрації користувача -->
<template>
    <!-- Заголовок форми -->
    <h1>Реєстрація</h1>
    <form @submit.prevent="register">
        <!-- Група полів для введення імені -->
        <div class="form-group">
            <div><i class="fa fa-user"></i></div>
            <input type="text" v-model="user_name" name="name" id="name" placeholder="Ваше ім'я" required>
            <label for="name">Ваше ім'я</label>
            <p class="hint error">{{ user_name_error }}</p>
        </div>
        <!-- Група полів для введення логіну -->
        <div class="form-group">
            <div><i class="fa-regular fa-circle-user"></i></div>
            <input type="text" name="login" id="login" placeholder="Логін" v-model="user_login" required>
            <label for="login">Логін</label>
            <p class="hint error">{{ user_login_error }}</p>
        </div>
        <!-- Група полів для введення email -->
        <div class="form-group">
            <div><i class="fa-regular fa-envelope"></i></div>
            <input type="email" name="email" id="email" placeholder="Email" v-model="user_email" required>
            <label for="email">Email</label>
            <p class="hint error">{{ user_email_error }}</p>
        </div>
        <!-- Група полів для введення пароля -->
        <div class="form-group password-group">
            <div><i class="fa fa-lock"></i></div>
            <input type="password" v-model="password" name="password" id="password" placeholder="Пароль" required>
            <label for="password">Пароль</label>
            <p class="hint error">{{ user_password_error }}</p>
        </div>
        <!-- Група полів для підтвердження пароля -->
        <div class="form-group password-group">
            <div><i class="fa fa-lock"></i></div>
            <input type="password" id="password_repeat" v-model="password_repeat" placeholder="Підтвердіть пароль" required>
            <label for="password_repeat">Підтвердіть пароль</label>
            <p class="hint error">{{ user_password_repeat_error }}</p>
        </div>
        <!-- Список правил для створення пароля -->
        <ul class="password-rules" v-if="password.length > 0">
            <li :class="{'checked': password_data.length >= 8}">Пароль повинен містити не менше 8 символів</li>
            <li :class="{'checked': password_data.upper_case >= 1}">Пароль повинен містити хоча б одну велику літеру</li>
            <li :class="{'checked': password_data.digit >= 1}">Пароль повинен містити хоча б одну цифру</li>
            <li :class="{'checked': password_data.special_char >= 1}">Пароль повинен містити хоча б один спеціальний символ</li>
            <li :class="{'checked': password_data.lower_case >= 1}">Пароль повинен містити хоча б одну маленьку літеру</li>
        </ul>
        <!-- Повідомлення про співпадіння паролів -->
        <p v-if="password.length > 0 && password_repeat.length > 0" :class="{'hint': true, 'error': !password_repeat_valid, 'success': password_repeat_valid}">{{ password_repeat_valid ? 'Паролі збігаються' : 'Паролі не збігаються' }}</p>
        <!-- Кнопка відправки форми -->
        <LinkBtn role="btn" type="submit" img="wave2.svg" bold="true" :disabled="!form_valid"><i class="fa fa-sign-in"></i> Зареєструватися</LinkBtn>
        <!-- Посилання на сторінку входу -->
        <p>Вже маєте акаунт? <Link to="/login" line="true">Увійти</Link></p>
    </form>
</template>

<script>
// Імпорт необхідних компонентів та модулів
import Link from '@/components/Link.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '@/axios.js';
import { mapActions } from 'vuex';

export default {
    // Реєстрація компонентів
    components: {
        LinkBtn,
        Link,
    },
    // Визначення пропсів для керування прелоадером
    props: {
        showPreloader: {
            type: Function,
            required: true,
        },
        closePreloader: {
            type: Function,
            required: true,
        },
    },
    // Початковий стан компонента
    data() {
        return {
            password: '',
            password_repeat: '',
            // Об'єкт для зберігання характеристик пароля
            password_data: {
                length: 0,
                upper_case: 0,
                lower_case: 0,
                digit: 0,
                special_char: 0,
            },
            user_name: '',
            user_login: '',
            user_email: '',
            form_valid: false,
            // Повідомлення про помилки
            user_name_error: '',
            user_login_error: '',
            user_email_error: '',
            user_password_error: '',
            user_password_repeat_error: '',
        }
    },
    // Обчислювані властивості
    computed: {
        // Перевірка співпадіння паролів
        password_repeat_valid() {
            return this.password_repeat == this.password;
        },
    },
    // Спостерігачі за змінами полів форми
    watch: {
        // Спостерігач за паролем
        password: {
            handler(val) {
                this.user_password_error = '';
                
                // Аналіз характеристик пароля
                this.password_data.length = val.length;
                this.password_data.upper_case = val.match(/[A-Z]/g)?.length || 0;
                this.password_data.lower_case = val.match(/[a-z]/g)?.length || 0;
                this.password_data.digit = val.match(/[0-9]/g)?.length || 0;
                this.password_data.special_char = val.match(/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/g)?.length || 0;

                this.validateForm();
            },
            immediate: true,
        },
        // Спостерігач за ім'ям користувача
        user_name: {
            handler(val) {
                this.user_name_error = '';
                if (val.length < 3 && val != '') {
                    this.user_name_error = 'Ім\'я повинно містити не менше 3 символів';
                }
                this.validateForm();
            },
            immediate: true,
        },
        // Спостерігач за логіном
        user_login: {
            handler(val) {
                this.user_login_error = '';
                if (val.length < 3 && val != '') {
                    this.user_login_error = 'Логін повинен містити не менше 3 символів';
                } else if (!this.isLoginValid(val) && val != '') {
                    this.user_login_error = 'Логін повинен містити лише латинські літери, цифри та символ "_"';
                }
                this.validateForm();
            },
            immediate: true,
        },
        // Спостерігач за email
        user_email: {
            handler(val) {
                this.user_email_error = '';
                if (!this.isValidEmail(this.user_email) && val != '') {
                    this.user_email_error = 'Неправильний формат електронної пошти';
                }
                this.validateForm();
            },
            immediate: true,
        },
        // Спостерігач за повтором пароля
        password_repeat: {
            handler(val) {
                this.validateForm();
            },
            immediate: true,
        },
    },
    methods: {
        // Підключення дій з Vuex
        ...mapActions(['addSuccessMessage', 'addErrorMessage']),
        // Перевірка валідності логіна
        isLoginValid(login) {
            const regex = /[^a-zA-Z0-9_]/;
            return !regex.test(login) && login.length >= 3;
        },
        // Перевірка валідності email
        isValidEmail(email) {
            const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            return regex.test(email);
        },
        // Валідація всієї форми
        validateForm() {
            this.form_valid = this.password_repeat_valid && 
            this.password_data.length >= 8 &&
            this.password_data.upper_case >= 1 &&
            this.password_data.lower_case >= 1 &&
            this.password_data.digit >= 1 &&
            this.password_data.special_char >= 1 &&
            this.user_name.length >= 3 && 
            this.isLoginValid(this.user_login) &&
            this.isValidEmail(this.user_email);
        },
        // Метод реєстрації користувача
        async register() {
            this.showPreloader();

            const self = this;
            // Відправка запиту на реєстрацію з використанням axios
            apiClient.post('/user/register', {
                name: this.user_name,
                login: this.user_login,
                email: this.user_email,
                password: this.password,
            })
            .then((response) => {
                self.closePreloader();

                if (response.status == 200) {
                    self.addSuccessMessage(response.data['success']);
                    self.$router.push('/login');
                }
            })
            .catch((error) => {
                console.log("Error in request: ", error);
                
                self.closePreloader();

                // Обробка помилок від сервера
                let data = [];
                try {
                    data = error.response.data;
                } catch (err) {
                    data = {
                        to: 'password_repeat',
                        error: 'Не вдалося зв\'язатися з сервером.',
                    }
                }
                if (data['to'] != undefined && data['to'] != '' && data['to'] != null && 
                data['error'] != undefined && data['error'] != '' && data['error'] != null) {
                    if (data['to'] == 'name')
                        self.user_name_error = data['error'];
                    else if (data['to'] == 'login')
                        self.user_login_error = data['error'];
                    else if (data['to'] == 'email')
                        self.user_email_error = data['error'];
                    else if (data['to'] == 'password')
                        self.user_password_error = data['error'];
                    else if (data['to'] == 'password_repeat')
                        self.user_password_repeat_error = data['error'];
                }
            });
        },
    },
}
</script>

<!-- Додавання стилів для форми -->
<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>