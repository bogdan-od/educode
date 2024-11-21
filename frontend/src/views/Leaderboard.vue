<!-- Шаблон компонента для відображення таблиці лідерів -->
<template>
    <h1><i class="fa-solid fa-medal"></i> Таблиця лідерів</h1>
    <hr>
    <!-- Контейнер для списку користувачів -->
    <div class="users">
        <!-- Відображення прелоадера під час завантаження даних -->
        <Preloader v-if="isLoading" />
        <!-- Цикл для відображення кожного користувача -->
        <div v-for="(user, i) in users" :key="i" class="user" :class="{ 'currentUser': user.login == currentUser }">
            <div>
                <span class="place">{{ i + 1 }}</span>
                <i class="fa-solid fa-user"></i>
                <Link :to="`/user/${user.login}`" line="true">@{{ user.login }}</Link>
            </div>
            <div>
                <i class="fa-solid fa-star"></i>
                <span>{{ user.rating }}</span>
            </div>
        </div>
    </div>
</template>

<!-- Стилі компонента -->
<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';

/* Стилізація заголовка */
h1 {
    i {
        font-size: 24px;
        margin-right: 10px;
    }
}

/* Стилізація контейнера користувачів */
.users {
    @include display-flex($flex-direction: column, $justify-content: start);
    position: relative;
    min-height: 500px;

    /* Стилізація блоку користувача */
    .user {
        @include display-flex($justify-content: space-between);
        padding: 10px 20px;
        width: 100%;

        &.currentUser {
            background-color: #5fca996b;
        }
        
        /* Розділювальна лінія між користувачами */
        &:not(:last-of-type) {
            border-bottom: 1px solid var(--weak-color);
        }

        /* Загальні стилі для дочірніх елементів */
        > * {
            display: flex;
            gap: 10px;
        }

        /* Стилізація іконок */
        > div i {
            position: relative;
            top: 5px;
            height: min-content;
        }

        /* Стилізація іконки користувача */
        > div:nth-child(1) {
            i {
                font-size: 24px;
            }
        }

        /* Стилізація іконки рейтингу */
        > div:nth-child(2) {
            i {
                font-size: 18px;
            }
        }
    }
}
</style>

<script scoped>
// Імпорт необхідних компонентів та модулів
import Link from '@/components/Link.vue';
import apiClient from '../axios';
import store from '../store';
import Preloader from '@/components/Preloader.vue';

export default {
    name: 'Leaderboard',
    // Реєстрація використовуваних компонентів
    components: {
        Link,
        Preloader,
    },
    // Визначення початкового стану даних
    data() {
        return {
            users: [], // Масив користувачів
            currentUser: '', // Поточний користувач
            isLoading: true, // Прапорець завантаження
        }
    },
    // Хук життєвого циклу, викликається після монтування компонента
    mounted() {
        this.getUsers();
        this.currentUser = store.getters.getCurrentUsername;
    },
    // Методи компонента
    methods: {
        // Отримання списку користувачів з API
        async getUsers() {
            await apiClient.get('/user/leaderboard')
                .then((response) => {
                    this.users = response.data;
                    this.isLoading = false;
                })
                .catch((error) => {
                    console.log(error);
                })
        }
    }
}
</script>