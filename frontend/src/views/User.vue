<!-- Головний компонент для відображення інформації про користувача -->
<template>
    <section class="user">
        <!-- Відображення прелоадера під час завантаження -->
        <Preloader v-if="isLoading" />
        <div class="user-info">
            <!-- Аватар користувача -->
            <div class="user-avatar">
                <i class="fa-solid fa-user"></i>
            </div>
            <!-- Основна інформація про користувача -->
            <div class="user-name">
                <span class="name">{{ user.name }}</span>
                <span class="login">@{{ user.login }}</span>
                <span class="email"><Link :href="`mailto:${user.email}`" line="true">{{ user.email }}</Link></span>
                <span class="rating"><i class="fa-solid fa-star"></i> Кількість балів: {{ user.rating ?? 0 }}</span>
                <br>
                <!-- Кнопка виходу з поточного сеансу -->
                <LinkBtn v-if="currentSessionId != null" role="btn" @click="closeCurrentSession"><i class="fa-solid fa-right-from-bracket"></i> Вийти</LinkBtn>
            </div>
        </div>
        <div class="user-details">
            <!-- Секція з останніми рішеннями користувача -->
            <div v-if="user.decisions.length > 0" class="decisions">
                <h3>Останні найкращі рішення</h3>
                <div v-for="(decision, i) in user.decisions" :key="i" class="decision">
                    <!-- Індикатори статусу рішення -->
                    <i v-if="!decision.finished" class="isRunning fa-solid fa-person-running"></i>
                    <i v-if="decision.correct" class="isCorrect fa-solid fa-square-check"></i>
                    <Link :to="`/puzzle/${decision.puzzle.id}`">{{ decision.puzzle.title }}</Link>
                    <p>{{ decision.createdAt }}</p>
                    <!-- Інформація про мову програмування та бали -->
                    <div class="decision-info">
                        <span class="language"><i class="fa-solid fa-screwdriver-wrench"></i> {{ decision.language }}</span>
                        <span class="score"><i :class="`fa-${decision.score == decision.puzzle.score ? 'solid' : 'regular'} fa-star`"></i> {{ decision.score }}/{{ decision.puzzle.score }}</span>
                    </div>
                </div>
            </div>
            <hr>
            <!-- Секція з активними сеансами користувача -->
            <div v-if="user.sessions.length > 0" class="sessions">
                <h3>Сеанси</h3>
                <div v-for="(session, i) in user.sessions" :key="i" class="session">
                    <!-- Іконка типу пристрою -->
                    <div class="device-type">
                        <i v-if="session.deviceType == 'desktop'" class="fa-solid fa-desktop"></i>
                        <i v-else class="fa-solid fa-mobile-screen-button"></i>
                    </div>
                    <!-- Детальна інформація про сеанс -->
                    <div class="session-info">
                        <span class="device-name">{{ session.deviceName }}{{ session.currentSession ? ' (Цей пристрій)' : '' }}</span>
                        <span class="device-ip">IP: {{ session.deviceIP ?? "невизначено" }}</span>
                        <span class="session-time">Перший вхід: {{ session.createdAt }}</span>
                        <Link @click="closeSession(session.id)" href="#" line="true">Завершити сеанс{{ session.currentSession ? ' (Вийти)' : '' }}</Link>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Секція зі створеними користувачем задачами -->
    <div v-if="user.puzzles.length > 0" class="puzzles">
        <hr>
        <h2>Задачі @{{ user.login }}</h2>
        <div class="puzzle" v-for="puzzle in user.puzzles" :key="puzzle.id">
            <!-- Заголовок задачі -->
            <div class="puzzle-header">
                <h2>{{ puzzle.title }}</h2>
                <div class="puzzle-header-right">
                    <p class="puzzle-header-right-time">Ліміт часу: {{ puzzle.timeLimit }} с</p>
                    <p class="puzzle-header-right-points">{{ puzzle.score }} балів</p>
                </div>
            </div>
            <!-- Опис задачі -->
            <div class="puzzle-body">
                <p>{{ puzzle.description }}</p>
            </div>
            <!-- Кнопка переходу до задачі -->
            <div class="puzzle-footer">
                <LinkBtn img="wave2.svg" click="load" :to="`/puzzle/${puzzle.id}`"><i class="fa fa-eye"></i> Переглянути</LinkBtn>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';
@import '@/assets/scss/variables.scss';

// Головний контейнер для інформації про користувача
.user {
    @include display-flex();
    width: 100%;
    gap: 20px;
    position: relative;
    
    // Контейнери для інформації та деталей користувача
    .user-info, .user-details {
        height: 100%;
        @include display-flex($flex-direction: column);
    }

    // Блок з основною інформацією про користувача
    .user-info {
        align-self: flex-start;
        align-items: flex-start;
        
        // Блок з інформацією про користувача
        .user-name {
            @include display-flex($flex-direction: column, $align-items: flex-start);
            width: 280px;
            gap: 5px;

            // Стилі для імені
            .name {
                font-size: 28px;
                font-weight: 600;
            }

            // Стилі для рейтингу
            .rating {
                margin-top: 10px;
            }
        }

        // Аватар користувача
        .user-avatar {
            height: 280px;
            width: 280px;
            border-radius: 50%;
            background-color: var(--weak-color);
            @include display-flex();

            // Іконка за замовчуванням
            i {
                font-size: 120px;
                color: var(--text-color);
            }
        }
    }

    // Блок з детальною інформацією
    .user-details {
        // Секція активних сеансів
        .sessions {
            @include display-flex($flex-direction: column, $align-items: flex-start);
            padding: 20px;
            gap: 15px;

            // Загальні стилі для тексту
            *:not(i) {
                font-family: $form-font;
                font-size: 14px;
            }

            // Заголовок секції
            h3 {
                font-size: 28px;
            }

            // Стилі для окремого сеансу
            .session {
                @include display-flex($justify-content: space-between);
                background-color: var(--weak-color);
                padding: 15px;
                border-radius: 5px;

                // Іконка типу пристрою
                .device-type {
                    width: 100px;
                    height: 100px;
                    border-radius: 5px;
                    @include display-flex();
                    margin-right: 20px;
                    border: 1px solid var(--secondary-color);
                    border-radius: 5px;

                    i {
                        font-size: 50px;
                    }
                }

                // Інформація про сеанс
                .session-info {
                    @include display-flex($flex-direction: column, $align-items: flex-start);

                    .device-ip {
                        color: var(--text-color);
                    }
                }
            }
        }

        // Секція з рішеннями користувача
        .decisions {
            @include display-flex($flex-direction: column, $align-items: flex-start);
            padding: 20px;
            gap: 15px;
            width: 100%;

            // Заголовок секції
            h3 {
                font-size: 28px;
            }

            // Стилі для окремого рішення
            .decision {
                @include display-flex($flex-direction: column, $align-items: flex-start);
                background-color: var(--weak-color);
                padding: 15px;
                border-radius: 5px;
                position: relative;
                width: 100%;
                gap: 10px;

                // Індикатори стану рішення
                .isRunning, .isCorrect {
                    position: absolute;
                    top: -7.5px;
                    width: min-content;
                    height: min-content;
                    padding: 5px;
                    border-radius: 5px;
                    background-color: var(--weak-color);
                }

                // Індикатор виконання
                .isRunning {
                    right: -7.5px;
                }

                // Індикатор правильності
                .isCorrect {
                    left: -7.5px;
                }

                // Інформація про рішення
                .decision-info {
                    @include display-flex($justify-content: space-between);
                    gap: 10px;
                    width: 100%;
                }
            }
        }
    }
}

// Секція зі створеними задачами
.puzzles {
    @include display-flex($flex-direction: column);
    gap: 20px;

    // Загальні стилі для заголовка та роздільника
    h2, hr {
        width: 100%;
    }
    
    // Стилі для окремої задачі
    .puzzle {
        width: 100%;
        padding: 15px;
        background-color: var(--weak-color);
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);

        // Стилі для посилань всередині задачі
        :deep(a) {
            margin-top: 10px;
        }
    }
}
</style>

<script scoped>
import Link from '@/components/Link.vue';
import apiClient from '../axios';
import { mapGetters, mapActions } from 'vuex';
import store from '../store';
import Preloader from '../components/Preloader.vue';
import LinkBtn from '../components/LinkBtn.vue';

export default {
    name: 'User',
    // Реєстрація компонентів
    components: {
        Link,
        Preloader,
        LinkBtn,
    },
    // Початковий стан компонента
    data() {
        return {
            // Дані користувача
            user: {
                name: 'Loading...',
                login: 'Loading...',
                email: 'Loading...',
                rating: 'Loading...',
                sessions: [],
                decisions: [],
                puzzles: [],
            },
            // Логін користувача з параметрів маршруту або поточного користувача
            login: this.$route.params.login == "" ? store.getters.getCurrentUsername : this.$route.params.login,
            isLoading: true,
            // Список доступних мов програмування
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
            // ID поточного сеансу
            currentSessionId: null,
        }
    },
    // Обчислювані властивості
    computed: {
        ...mapGetters(['getCurrentUsername']),
    },
    // Спостерігачі за змінами
    watch: {
        // Оновлення даних користувача при зміні логіну
        login() {
            this.updateUser();
        }
    },
    methods: {
        ...mapActions(['addSuccessMessage', 'addErrorMessage']),
        // Закриття поточного сеансу
        async closeCurrentSession() {
            await this.closeSession(this.currentSessionId);
        },
        // Закриття сеансу за ID
        async closeSession(id) {
            const self = this;
            await apiClient.delete(`/user/session/${id}`)
                .then(() => {
                    self.addSuccessMessage('Сеанс успішно завершено');
                    const deletedSession = self.user.sessions.find((session) => session.id === id);
                    if (deletedSession.currentSession) {
                        self.$router.push('/login');
                        return;
                    }
                    self.user.sessions = self.user.sessions.filter((session) => session.id !== id);
                })
                .catch((error) => {
                    if (error.response) {
                        self.addErrorMessage(error.response.data.message);
                    } else {
                        self.addErrorMessage('Помилка сервера');
                    }
                });
        },
        // Оновлення даних користувача
        async updateUser() {
            const self = this;
            this.isLoading = true;

            await apiClient.get(`/user/get/${this.login}`)
                .then((response) => {
                    // Оновлення даних користувача з відповіді сервера
                    this.user = {
                        name: response.data['name'],
                        login: response.data['login'],
                        email: response.data['email'],
                        rating: response.data['rating'],
                        sessions: response.data['sessions'],
                        decisions: response.data['decisions'],
                        puzzles: response.data['puzzles'],
                    };

                    // Налаштування формату дати
                    const dateOptions = {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                        hour12: false
                    };

                    // Форматування дат для сеансів
                    this.user.sessions.forEach((session) => {
                        const date = new Date(session.createdAt);
                        session.createdAt = date.toLocaleString('uk-UA', dateOptions);

                        if (session.currentSession) {
                            this.currentSessionId = session.id;
                        }
                    });

                    // Форматування дат та мов програмування для рішень
                    this.user.decisions.forEach((decision) => {
                        const date = new Date(decision.createdAt);
                        decision.createdAt = date.toLocaleString('uk-UA', dateOptions);

                        const currentLanguage = this.languages.find((language) => language.server_id === decision.language);
                        decision.language = `${currentLanguage.name} (${currentLanguage.version})`;
                    });

                    this.isLoading = false;
                }).catch((error) => {
                    this.isLoading = false;

                    if (error.status == 404)
                        this.$router.replace('/404');
                });
        },
    },
    // Хук життєвого циклу при монтуванні компонента
    async mounted() {
        // Перевірка наявності логіну
        if (this.login == "" || this.login == null) {
            this.$router.push("/login");
            return;
        }

        this.updateUser();
    },
}
</script>