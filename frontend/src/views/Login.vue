<!-- Шаблон компонента для сторінки входу -->
<template>
    <h1>Вхід до аккаунту</h1>
    <Messages :mobile="true"/>
    <!-- Форма входу з обробником подій loginEvent -->
    <form @submit.prevent="loginEvent">
        <!-- Група полів для логіну -->
        <div class="form-group">
            <div><i class="fa-regular fa-circle-user"></i></div>
            <input type="text" name="login" id="login" placeholder="Логін" v-model="userLogin" required>
            <label for="login">Логін</label>
            <p class="hint error">{{ userLoginError }}</p>
        </div>
        <!-- Група полів для паролю -->
        <div class="form-group password-group">
            <div><i class="fa fa-lock"></i></div>
            <input type="password" v-model="userPassword" name="password" id="password" placeholder="Пароль" required>
            <label for="password">Пароль</label>
            <p class="hint error">{{ userPasswordError }}</p>
        </div>
        <!-- Кнопка входу -->
        <LinkBtn role="btn" img="wave2.svg" bold="true" type="submit" :disabled="!formValid"><i class="fa fa-sign-in"></i> Увійти</LinkBtn>
        <!-- Посилання на реєстрацію -->
        <p>Ще не маєте аккаунта? <Link to="/register" line="true">Зареєструватися</Link></p>
    </form>
</template>

<script>
// Імпорт необхідних компонентів та модулів
import Link from '@/components/Link.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import apiClient from '@/axios.js';
import { mapActions } from 'vuex';
import Detect from 'detect.js'

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
    // Визначення локального стану компонента
    data() {
        return {
            userLogin: '',
            userPassword: '',
            userLoginError: '',
            userPasswordError: '',
            formValid: false,
        };
    },
    // Спостерігачі за змінами полів форми
    watch: {
        // Спостерігач за зміною паролю
        userPassword: {
            handler(val) {
                this.validateForm();
            },
            immediate: true,
        },
        // Спостерігач за зміною логіну з валідацією
        userLogin: {
            handler(val) {
                this.userLoginError = '';
                if (val.length < 3 && val != '') {
                    this.userLoginError = 'Логін повинен містити не менше 3 символів';
                }
                this.validateForm();
            },
            immediate: true,
        },
    },
    methods: {
        // Підключення дій з Vuex
        ...mapActions(['login', 'addSuccessMessage', 'addErrorMessage']),
        
        // Метод валідації форми
        validateForm() {
            this.formValid = this.userLogin.length >= 0 && this.userPassword.length > 0;
        },
        
        // Метод обробки події входу
        async loginEvent() {
            this.userLoginError = '';
            this.userPasswordError = '';

            this.showPreloader();

            const self = this;
            // Визначення інформації про пристрій користувача
            const detector = Detect.parse(window.navigator.userAgent);

            // Відправка запиту на авторизацію
            apiClient.post('/user/auth', {
                login: this.userLogin,
                password: this.userPassword,
                deviceName: `${detector.os.name} - ${detector.browser.name}`,
                deviceType: detector.device.type,
            })
            .then((response) => {
                self.closePreloader();

                // Обробка успішної відповіді
                if (response.status == 200 && response.data['success']) {
                    self.addSuccessMessage(response.data['success']);
                    self.login({
                        accessToken: response.data['access_token'],
                        refreshToken: response.data['refresh_token'],
                    });

                    self.$router.push('/user');
                }
            })
            .catch((error) => {
                console.log("Error in request: ", error);
                
                self.closePreloader();

                let data = [];
                
                // Обробка помилок
                try {
                    data = error.response.data;
                } catch (err) {
                    data = {
                        to: 'password',
                        error: 'Не вдалося зв\'язатися з сервером.',
                    }
                }
                // Відображення помилок у відповідних полях
                if (data['to'] != undefined && data['to'] != '' && data['to'] != null && 
                data['error'] != undefined && data['error'] != '' && data['error'] != null) {
                    if (data['to'] == 'login')
                        self.userLoginError = data['error'];
                    else if (data['to'] == 'password')
                        self.userPasswordError = data['error'];
                }
            });
        },
    },
};
</script>

<!-- Підключення стилів форми -->
<style lang="scss" src="@/assets/scss/std_form.scss" scoped></style>