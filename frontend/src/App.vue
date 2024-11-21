<!-- Основний скрипт компоненту -->
<script setup>
import { RouterView } from 'vue-router'
</script>

<!-- Шаблон компоненту -->
<template>
    <!-- Шапка сайту -->
    <header>
        <!-- Логотип та назва сайту -->
        <div class="logo">
            <img alt="logo" src="@/assets/img/logo.webp" />
            <div class="text">
                <span class="title">Educode</span>
                <span class="description">Програмне забезпечення для навчання</span>
            </div>
        </div>
        <!-- Навігаційні посилання та кнопка меню -->
        <div class="links">
            <!-- Посилання для десктопної версії (при ширині екрана більше 780px) -->
            <Link v-if="screenWidth > 780" to="/" :active="current_path == '/'">Головна</Link>
            <Link v-if="screenWidth > 780" to="/about" :active="current_path == '/about'">Про нас</Link>
            
            <button class="toggle" :class="{'opened': navOpened}" @click="toggleNav">
                <div class="wrapper">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </button>
        </div>
        <!-- Бокове навігаційне меню -->
        <nav :class="{'opened': navOpened}">
            <span class="nav-title">Educode</span>
            <!-- Посилання для мобільної версії -->
            <LinkBtn v-if="screenWidth <= 780" to="/" anim="bg-scale"><i class="fa fa-home"></i> Головна</LinkBtn>
            <LinkBtn v-if="screenWidth <= 780" to="/about" anim="bg-scale"><i class="fa-regular fa-address-card"></i> Про нас</LinkBtn>
            <!-- Посилання для авторизованих користувачів -->
            <LinkBtn v-if="authorized" to="/user" anim="bg-scale"><i class="fa fa-user"></i> Особистий кабінет</LinkBtn>
            <LinkBtn v-if="authorized && userRoles.find(el => el == 'ROLE_PUZZLE_CREATOR')" to="/add/puzzle" anim="bg-scale"><i class="fa fa-puzzle-piece"></i> Додати задачу</LinkBtn>
            <!-- Посилання для неавторизованих користувачів -->
            <LinkBtn v-if="!authorized" to="/login" anim="bg-scale"><i class="fa fa-sign-in"></i> Увійти</LinkBtn>
            <LinkBtn v-if="!authorized" to="/register" anim="bg-scale"><i class="fa fa-user-plus"></i> Зареєструватися</LinkBtn>
            <!-- Загальні посилання -->
            <LinkBtn to="/leaderboard" anim="bg-scale"><i class="fa-solid fa-medal"></i> Таблиця лідерів</LinkBtn>
            <LinkBtn to="/puzzles" anim="bg-scale"><i class="fa-solid fa-puzzle-piece"></i> Задачі</LinkBtn>
        </nav>
        <!-- Кнопка перемикання теми -->
        <button class="toggle-theme" @click="toggleTheme">
            <div class="content" :style="`--pos: ${toggleThemePos}`">
                <i class="fa-solid fa-sun"></i>
                <i class="fa-solid fa-moon"></i>
                <i class="fa-solid fa-tablet-screen-button"></i>
            </div>
        </button>
    </header>

    <!-- Затемнення фону при відкритому меню -->
    <div class="black-background" :class="{'visible': navOpened}" @click="toggleNav"></div>

    <!-- Індикатор завантаження -->
    <Preloader v-if="isLoading" fullScreen="true" />

    <!-- Блок повідомлень -->
    <div class="messages">
        <div v-for="(message, i) in getMessages" :key="i" class="message" :class="{'success': message.type == 'success', 'error': message.type == 'error'}" :style="`--i: ${i}`">
            <div class="wrapper">
                <span class="text">{{ message.message }}</span>
                <button @click="deleteMessage(i)" class="close"><i class="fa-regular fa-circle-xmark"></i></button>
            </div>
        </div>
    </div>

    <!-- Основний контент -->
    <main>
        <RouterView :showPreloader="showPreloader" :closePreloader="closePreloader" />
    </main>

    <!-- Підвал сайту -->
    <footer>
        <div class="cols">
            <!-- Контактна інформація -->
            <div class="col">
                <h3>Контакти</h3>
                <p>З питань дозволу додавання задач писати на:</p>
                <Link href="mailto:bogdan040275@ukr.net" line="true"><i class="fa-regular fa-envelope"></i> bogdan040275@ukr.net</Link>
            </div>
            <!-- Інформація про проект -->
            <div class="col">
                <h3>Про нас</h3>
                <p>Це проект для навчання програмуванню, у якому можна вирішувати задачі та тестувати ваші розв'язки.</p>
            </div>
            <!-- Додаткові посилання -->
            <div class="col">
                <h3>Посилання</h3>
                <Link to="/puzzles">Задачі з програмування</Link>
            </div>
        </div>
        <!-- Копірайт та атрибуція -->
        <p>© Всі права захищені 2024</p>
        <p>Part of the images and graphics on this site were created using resources from <a rel="nofollow" href="https://www.freepik.com" target="_blank">Freepik</a>.</p>
    </footer>
</template>

<script>
    // Імпорт необхідних компонентів та модулів
    import LinkBtn from '@/components/LinkBtn.vue';
    import Link from '@/components/Link.vue';
    import Preloader from '@/components/Preloader.vue';
    import { mapState, mapGetters, mapMutations, mapActions } from 'vuex';
    import apiClient from './axios';
    import store from './store';
    import Detect from 'detect.js';

    export default {
        // Реєстрація компонентів
        components: {
            LinkBtn,
            Link,
            Preloader,
        },
        // Локальний стан компонента
        data() {
            return {
                current_path: this.$route.path,
                isLoading: false,
                navOpened: false,
                toggleThemePos: 0,
                authorized: false,
                userRoles: [],
                screenWidth: 1920,
            };
        },
        // Обчислювані властивості з Vuex
        computed: {
            ...mapGetters(['getAccessToken', 'getTheme', 'isAuthenticated', 'getMessages', 'getUserRoles']),
        },
        // Спостерігачі за змінами
        watch: {
            // Слідкування за зміною токена доступу
            getAccessToken(newToken) {
                this.updateAuth();
            },
            // Слідкування за зміною маршруту
            '$route'(to, from) {
                this.current_path = to.path;
                this.updateAuth();
                this.navOpened = false;
            },
            // Слідкування за зміною ролей користувача
            getUserRoles(newRoles) {
                this.userRoles = newRoles;
            },
        },
        methods: {
            // Імпорт методів з Vuex
            ...mapActions(['setTheme', 'clearExpiredMessages', 'addSuccessMessage', 'addErrorMessage']),
            
            // Оновлення статусу авторизації
            updateAuth() {
                this.authorized = this.isAuthenticated;
            },
            // Керування прелоадером
            closePreloader() {
                this.isLoading = false;
            },
            showPreloader() {
                this.isLoading = true;
            },
            // Перемикання навігаційного меню
            toggleNav() {
                this.navOpened = !this.navOpened;
            },
            // Перемикання теми оформлення
            toggleTheme() {
                if (this.toggleThemePos <= -2)
                    this.toggleThemePos = 0;
                else 
                    this.toggleThemePos -= 1;

                this.updateThemeInfo();
            },
            // Перевірка наявності ролі у користувача
            userHasRole(role) {
                return this.userRoles.find(el => el == role) != undefined;
            },
            // Оновлення інформації про тему
            updateThemeInfo() {
                var theme = 'light';

                if (this.toggleThemePos == -1) {
                    theme = 'dark';
                }
                else if (this.toggleThemePos == -2 && window.matchMedia) {
                    theme = window.matchMedia("(prefers-color-scheme: dark)").matches ? 'dark' : 'light';
                }
                    
                document.body.classList.remove('dark');
                document.body.classList.remove('light');
                document.body.classList.add(theme);
                this.setTheme(this.toggleThemePos == -1 ? 'dark' : this.toggleThemePos == -2 && window.matchMedia ? 'auto' : 'light');
            },
            // Видалення повідомлення
            deleteMessage(i) {
                store.state.messages = store.state.messages.filter((message, index) => index != i);
            },
            // Оновлення токена доступу
            async refreshToken() {
                const self = this;
                const accessTokenExpiry = store.getters.getAccessTokenExpiry;
                const currentTime = new Date().getTime();

                if (accessTokenExpiry && currentTime + 60 * 10 * 1000 > accessTokenExpiry) {
                    const detector = Detect.parse(window.navigator.userAgent);
                    await apiClient.post('/user/auth/refresh-token', {
                        refreshToken: store.state.refreshToken,
                        accessToken: store.state.accessToken,
                        deviceName: `${detector.os.name} - ${detector.browser.name}`
                    })
                    .then(response => {
                        store.dispatch('login', {
                            accessToken: response.data['access_token'],
                            refreshToken: response.data['refresh_token'],
                        });
                        
                        self.updateAuth();
                    })
                    .catch(err => {
                        self.updateAuth();
                    });
                }
            },
        },
        // Хук створення компонента
        created() {
            this.toggleThemePos = this.getTheme == 'dark' ? -1 : this.getTheme == 'auto' ? -2 : 0;
            this.updateAuth();
            this.updateThemeInfo();
        },
        // Хук монтування компонента
        mounted() {
            const self = this;

            // Відслідковування розміру екрану
            this.screenWidth = window.innerWidth;
            window.addEventListener('resize', () => {
                self.screenWidth = window.innerWidth;
            });

            // Ініціалізація ролей користувача
            this.userRoles = store.getters.getUserRoles;
            
            // Запуск періодичного оновлення токена
            this.refreshToken();
            setInterval(this.refreshToken, 600000); // 10 хвилин

            // Періодична очистка застарілих повідомлень
            setInterval(() => {
                self.clearExpiredMessages();
            }, 200);       
        }
    };
</script>

<!-- Імпорт основних стилів -->
<style lang="scss" src="@/assets/scss/main.scss"></style>

<!-- Локальні стилі компонента -->
<style lang="scss">
    /* Імпорт змінних та міксинів */
    @import '@/assets/scss/variables.scss';
    @import '@/assets/scss/mixins.scss';

    /* Основний контейнер сторінки */
    main {
        width: 100vw;
        padding: 50px 10%;

        /* Адаптивні відступи для різних розмірів екрану */
        @media (max-width: 1200px) {
            padding: 50px 5%;
        }

        @media (max-width: 400px) {
            padding: 50px 2.5%;
        }

        @media (max-width: 340px) {
            padding: 50px 1%;
        }
    }

    /* Шапка сайту */
    header {
        @include display-flex($justify-content: space-between);
        width: 100%;
        padding: 20px 10%;
        box-shadow: var(--weak-box-shadow);
        border-bottom-left-radius: 10px;
        border-bottom-right-radius: 10px;

        /* Блок з логотипом */
        .logo {
            @include display-flex($justify-content: space-between);

            /* Зображення логотипу */
            img {
                height: 70px;
            }

            /* Текстова частина логотипу */
            div.text {
                @include display-flex($align-items: flex-start, $flex-direction: column);
                margin-left: 15px;

                /* Заголовок логотипу */
                span.title {
                    font-size: 32px;
                    font-weight: 600;
                    color: var(--text-color);
                    font-family: $secondary-font;
                }

                /* Опис під логотипом */
                span.description {
                    font-size: 14px;
                    font-weight: 400;
                    color: var(--text-color);
                    margin-top: -10px;
                }
            }

            /* Адаптивні стилі для логотипу */
            @media (max-width: 1300px) {
                margin-left: -80px;
            }
            @media (max-width: 1000px) {
                margin-left: -70px;
            }
            @media (max-width: 780px) {
                margin-left: -50px;
            }
            @media (max-width: 580px) {
                margin-left: -35px;
            }
            @media (max-width: 500px) {
                .description {
                    max-width: 180px;
                }
            }
            @media (max-width: 400px) {
                img {
                    height: 50px;
                }
                div.text {
                    span.title {
                        font-size: 24px;
                    }
                    span.description {
                        font-size: 12px;
                    }
                }
            }
            @media (max-width: 350px) {
                .description {
                    max-width: 150px;
                }
            }
        }

        /* Контейнер для посилань */
        div.links {
            @include display-flex($justify-content: space-between);
            width: 50%;
            max-width: 300px;

            /* Адаптивні стилі для посилань */
            @media (max-width: 780px) {
                justify-content: end;
                width: min-content;
            }
            @media (max-width: 700px) {
                margin-right: 10px;
            }
            @media (max-width: 580px) {
                margin-right: 20px;
            }
        }

        /* Кнопка переключення мобільного меню */
        .toggle {
            position: relative;
            width: 40px;
            height: 40px;
            border: 0;
            background-color: #ffffff4f;
            border: 1.5px solid var(--text-color);
            border-radius: 3px;
            padding: 5px;
            cursor: pointer;

            /* Обгортка для смужок меню */
            .wrapper {
                @include display-flex($justify-content: space-between, $flex-direction: column, $flex-wrap: nowrap);
                transition: all 0.2s ease-in-out;
                height: 100%;
                width: 100%;

                /* Смужки меню */
                span {
                    display: block;
                    width: 100%;
                    height: 5px;
                    background-color: var(--text-color);
                    transition: all 0.2s ease-in-out;
                }
            }

            /* Стилі для відкритого стану меню */
            &.opened {
                .wrapper {
                    transform: rotate(360deg);

                    span:nth-child(1) {
                        transform: translateY(11px) rotate(45deg);
                    }
                    span:nth-child(2) {
                        opacity: 0;
                    }
                    span:nth-child(3) {
                        transform: translateY(-11px) rotate(-45deg);
                    }
                }
            }
        }

        /* Навігаційне меню */
        nav {
            position: absolute;
            @include display-flex($flex-direction: column, $justify-content: start);
            height: 100vh;
            position: fixed;
            top: 0;
            background-color: var(--main-color);
            padding: 20px;
            z-index: 100002;
            box-sizing: border-box;
            transition: all 0.3s ease-in-out;
            box-shadow: var(--strong-box-shadow);
            width: 300px;
            left: -100%;

            > * {
                width: 100%;
                text-align: center;
            }

            > *:not(:last-child) {
                margin-bottom: 20px;
            }

            /* Заголовок навігаційного меню */
            > span.nav-title {
                font-size: 2em;
                font-weight: 700;
                align-self: flex-start;
                text-align: left;
                margin-bottom: 35px;
                user-select: none;
            }

            /* Клас для відкритого стану меню */
            &.opened {
                left: 0;
            }
        }

        /* Кнопка переключення теми */
        .toggle-theme {
            position: absolute;
            right: 50px;
            top: 35px;
            width: 40px;
            height: 40px;
            background-color: var(--main-color);
            border: 1.5px solid var(--text-color);
            overflow: hidden;
            cursor: pointer;
            border-radius: 3px;

            /* Контент кнопки теми */
            .content {
                width: max-content;
                transition: all 0.2s ease-in-out;
                transform: translateX(calc(var(--pos) * 40px));

                i {
                    font-size: 20px;
                    width: 40px;
                }
            }

            /* Адаптивні стилі для кнопки теми */
            @media (max-width: 1120px) {
                right: 20px;
            }
            @media (max-width: 500px) {
                right: 12px;
                top: 37px;
            }
            @media (max-width: 400px) {
                top: 27px;
            }
            @media (max-width: 350px) {
                top: 37px;
            }
        }
    }

    /* Затемнений фон при відкритому меню */
    .black-background {
        background-color: rgba(0, 0, 0, 0.432);
        width: 100vw;
        height: 100vh;
        z-index: 100001;
        position: fixed;
        top: 0;
        left: 0;
        visibility: hidden;
        opacity: 0;
        transition: all 0.3s ease-in-out 0.0005s;

        /* Клас для видимого стану */
        &.visible {
            opacity: 1;
            visibility: visible;
            transition-delay: 0ms;
        }
    }

    /* Контейнер для повідомлень */
    .messages {
        position: fixed;
        top: 120px;
        right: 50px;
        z-index: 100000000;

        /* Стилі окремого повідомлення */
        .message {
            max-width: 330px;
            min-width: 200px;
            padding: 10px;
            background-color: var(--main-color);
            border-radius: 3px;
            border: 1px solid var(--secondary-color);
            font-size: 0.9em;
            font-family: $form-font;
            padding-right: 45px;

            &:not(:last-child) {
                margin-bottom: 10px;
            }

            /* Стилі для успішного повідомлення */
            &.success {
                background-color: #5fca996b;
                border: 1px solid #a3cfbb;
            }

            /* Стилі для повідомлення про помилку */
            &.error {
                background-color: #c634415e;
                border: 1px solid #f1aeb5;
            }

            /* Обгортка повідомлення */
            .wrapper {  
                position: relative;
                width: 100%;
                height: 100%;

                /* Кнопка закриття повідомлення */
                .close {
                    position: absolute;
                    right: -35px;
                    background-color: transparent;
                    cursor: pointer;
                    border: 0;
                    padding: 2px;

                    i {
                        font-size: 24px;
                        transition: all 0.2s ease-in-out;

                        &:hover {
                            transform: rotate(180deg);
                        }
                    }
                }
            }
        }
    }

    /* Підвал сайту */
    footer {
        position: relative;
        min-height: 300px;
        width: 100%;
        padding: 40px 10%;
        background-color: var(--main-color);
        border-top: 1px solid var(--text-color);

        > p:first-of-type {
            margin-top: 35px;
        }

        > p, > p > * {
            font-size: 14px;
        }

        /* Колонки в підвалі */
        .cols {
            @include display-flex($justify-content: space-between, $align-items: flex-start);
            width: 100%;
            height: 80%;
            gap: 30px;

            .col {
                width: 30%;
                min-width: 260px;
            }
        }
    }
</style>