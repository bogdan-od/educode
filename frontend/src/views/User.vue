<template>
    <section class="user-page">
        <Preloader v-if="isLoading" />
        
        <div v-else-if="error" class="error-message">
            <h2>Помилка</h2>
            <p>{{ error }}</p>
            <LinkBtn to="/" anim="go">На головну</LinkBtn>
        </div>

        <template v-else-if="user">
            <!-- BREADCRUMBS -->
            <BreadcrumbNavigation :homeRoute="{ name: 'Головна', to: '/' }" :crumbs="[{ name: user.name }]" />

            <!-- ШАПКА ПРОФИЛЯ -->
            <div class="user-header-card">
                <div class="user-avatar">
                    <i class="fa-solid fa-user"></i>
                </div>
                <div class="user-info">
                    <span class="name">{{ user.name }}</span>
                    <span class="login">@{{ user.login }}</span>
                    <span class="email">
                        <i class="fa-solid fa-envelope"></i>
                        <a :href="`mailto:${user.email}`">{{ user.email }}</a>
                    </span>
                    <span class="rating">
                        <i class="fa-solid fa-star"></i>
                        Кількість балів: {{ user.rating ?? 0 }}
                    </span>
                    <LinkBtn v-if="isOwnProfile && currentSessionId" role="btn" @click="closeCurrentSession" color="danger" size="sm" class="logout-btn">
                        <i class="fa-solid fa-right-from-bracket"></i> Вийти
                    </LinkBtn>
                </div>
            </div>

            <!-- ПЕРЕКЛЮЧАТЕЛЬ КОНТЕНТА -->
            <SegmentedControl :options="contentTabs" v-model="activeView" class="content-tabs" />

            <!-- ЛЕНИВО ЗАГРУЖАЕМЫЙ КОНТЕНТ -->
            <div class="user-content">
                
                <!-- РЕШЕНИЯ -->
                <div v-if="activeView === 'decisions'" class="content-section">
                    <h2>Останні найкращі рішення</h2>
                    <div v-if="user.decisions && user.decisions.length > 0" class="decisions-list">
                        <div v-for="decision in user.decisions" :key="decision.id" class="decision-item">
                            <div class="decision-header">
                                <router-link :to="`/puzzle/${decision.puzzle.id}`">{{ decision.puzzle.title }}</router-link>
                                <span class="language">{{ getDisplayLanguageText(decision.language) }}</span>
                            </div>
                            <div class="decision-footer">
                                <span class="score" :class="{ 'full-score': decision.score === decision.puzzle.score }">
                                    <i :class="`fa-${decision.score == decision.puzzle.score ? 'solid' : 'regular'} fa-star`"></i> 
                                    {{ decision.score }}/{{ decision.puzzle.score }}
                                </span>
                                <span class="status">
                                    <i :class="decision.correct ? 'isCorrect fa-solid fa-square-check' : 'isWrong fa-solid fa-square-xmark'"></i>
                                    {{ decision.correct ? 'Зараховано' : 'Не зараховано' }}
                                </span>
                                <span class="date">{{ new Date(decision.createdAt).toLocaleString() }}</span>
                            </div>
                        </div>
                    </div>
                    <p v-else class="empty-message">Користувач ще не має рішень.</p>
                    <!-- Примечание: Пагинация для решений не реализована, т.к. API /user/get/{login} возвращает их все сразу -->
                </div>

                <!-- ЗАДАЧИ -->
                <div v-if="activeView === 'puzzles'" class="content-section">
                    <h2>Створені задачі ({{ puzzleTotal }})</h2>
                    <div v-if="contentLoading"><Preloader/></div>
                    <div v-else-if="puzzles.length > 0" class="puzzles-list">
                        <div v-for="puzzle in puzzles" :key="puzzle.id" class="puzzle-item">
                            <div class="item-info">
                                <span class="item-title">{{ puzzle.title }}</span>
                                <hr>
                                <div class="item-details">
                                    {{ puzzle.description }}
                                    <LinkBtn :to="`/puzzle/${puzzle.id}`" anim="go" size="sm">
                                        <i class="fa fa-eye"></i> Переглянути
                                    </LinkBtn>
                                </div>
                            </div>
                        </div>
                        <button v-if="puzzleHasNext" @click="loadMorePuzzles" class="load-more-btn">
                            Завантажити ще
                        </button>
                    </div>
                    <p v-else class="empty-message">Користувач не створив жодної задачі.</p>
                </div>
                
                <!-- ВУЗЛЫ И ГРУППЫ -->
                <div v-if="activeView === 'memberships'" class="content-section">
                    <h2>Вузли та групи ({{ membershipTotal }})</h2>
                     <div v-if="contentLoading"><Preloader/></div>
                    <div v-else-if="memberships.length > 0" class="items-list">
                         <div v-for="item in memberships" :key="item.type + item.treeNodeId" class="item-card">
                            <div class="item-icon">
                                <i :class="item.type === 'node' ? 'fa-solid fa-folder' : 'fa-solid fa-users'"></i>
                            </div>
                            <div class="item-info">
                                <h3>{{ item.title }}</h3>
                                <p>{{ item.description }}</p>
                                <div class="roles">
                                    <span 
                                        v-for="role in item.roles" 
                                        :key="role.name" 
                                        class="role-badge"
                                        @click="showRoleInfo(role.name)"
                                        title="Дізнатися більше про роль"
                                    >
                                        {{ role.description }}
                                    </span>
                                </div>
                            </div>
                            <div class="item-actions">
                                <LinkBtn :to="`/${item.type}/${item.id}`" anim="go" title="Переглянути">
                                    <i class="fa-solid fa-eye"></i>
                                </LinkBtn>
                            </div>
                        </div>
                        <button v-if="membershipHasNext" @click="loadMoreMemberships" class="load-more-btn">
                            Завантажити ще
                        </button>
                    </div>
                    <p v-else class="empty-message">Користувач не є учасником жодного вузла чи групи.</p>
                </div>

                <!-- СЕССИИ (Только для владельца) -->
                <div v-if="activeView === 'sessions' && isOwnProfile" class="content-section">
                    <h2>Активні сеанси</h2>
                    <div v-if="user.sessions && user.sessions.length > 0" class="sessions-list">
                        <div v-for="session in user.sessions" :key="session.id" class="session-item">
                            <div class="device-type">
                                <i v-if="session.deviceType == 'desktop'" class="fa-solid fa-desktop"></i>
                                <i v-else class="fa-solid fa-mobile-screen-button"></i>
                            </div>
                            <div class="session-info">
                                <span class="device-name">{{ session.deviceName }}</span>
                                <span class="device-ip">IP: {{ session.deviceIP ?? "невизначено" }}</span>
                                <span class="session-time">Перший вхід: {{ new Date(session.createdAt).toLocaleString() }}</span>
                            </div>
                            <LinkBtn @click="closeSession(session.id)" color="danger" size="sm">
                                <i class="fa-solid fa-trash"></i>
                                {{ session.currentSession ? ' (Вийти)' : 'Завершити' }}
                            </LinkBtn>
                        </div>
                    </div>
                    <p v-else class="empty-message">Активних сеансів немає.</p>
                </div>

            </div>
        </template>

        <!-- ROLE INFO MODAL -->
        <RoleInfoModal v-model:visible="isRoleModalVisible" :roleName="selectedRoleName" />
    </section>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import SegmentedControl from '@/components/SegmentedControl.vue';
import BreadcrumbNavigation from '@/components/BreadcrumbNavigation.vue';
import RoleInfoModal from '@/components/RoleInfoModal.vue';
import { handleApiError } from '@/services/errorHandler.js';
import { getDisplayLanguageText, loadProgrammingLanguages } from '@/services/programmingLanguageService';

// --- ИНИЦИАЛИЗАЦИЯ ---
const route = useRoute();
const router = useRouter();
const store = useStore();

const user = ref(null);
const isLoading = ref(true);
const contentLoading = ref(false); // Loader for tabs
const contentLoadingMore = ref(false); // Loader for "load more"
const error = ref('');
const activeView = ref('decisions'); // View по умолчанию

const login = computed(() => route.params.login || store.getters.getCurrentUsername);
const currentSessionId = ref(null);
const isOwnProfile = computed(() => login.value === store.getters.getCurrentUsername);

// --- ДАННЫЕ ДЛЯ ВКЛАДОК ---
const puzzles = ref([]);
const puzzlePage = ref(1);
const puzzleHasNext = ref(false);
const puzzleTotal = ref(0);

const memberships = ref([]);
const membershipPage = ref(1);
const membershipHasNext = ref(false);
const membershipTotal = ref(0);

// --- ROLE MODAL STATE ---
const isRoleModalVisible = ref(false);
const selectedRoleName = ref(null);
const showRoleInfo = (roleName) => {
    selectedRoleName.value = roleName;
    isRoleModalVisible.value = true;
};
// --- END ROLE MODAL ---

// Опции для SegmentedControl
const contentTabs = computed(() => {
    const tabs = [
        { label: 'Рішення', value: 'decisions', icon: 'fa-solid fa-check' },
        { label: 'Задачі', value: 'puzzles', icon: 'fa-solid fa-puzzle-piece' },
        { label: 'Вузли/Групи', value: 'memberships', icon: 'fa-solid fa-folder-tree' },
    ];
    if (isOwnProfile.value) {
        tabs.push({ label: 'Сеанси', value: 'sessions', icon: 'fa-solid fa-display' });
    }
    return tabs;
});

// --- API: ОСНОВНОЙ ПРОФИЛЬ ---
const fetchUser = async () => {
    isLoading.value = true;
    error.value = '';
    if (!login.value) {
        router.push("/login");
        return;
    }
    try {
        const response = await apiClient.get(`/user/get/${login.value}`);
        user.value = response.data; // UserDTO
        
        // Сессии (если есть)
        if (user.value.sessions) {
            const current = user.value.sessions.find(s => s.currentSession);
            if (current) {
                currentSessionId.value = current.id;
            }
        }
    } catch (err) {
        handleApiError(err);
        error.value = "Не вдалося завантажити профіль.";
        if (err.response?.status === 404) {
             router.replace('/404');
        }
    } finally {
        isLoading.value = false;
    }
};

// --- API: ЗАГРУЗКА ВКЛАДОК ---
const fetchPuzzles = async (page = 1) => {
    if (page === 1) contentLoading.value = true;
    else contentLoadingMore.value = true;
    
    try {
        // API из `controllers.java` (старый `User.vue` имел `user.puzzles`)
        // Предполагаем, что бэкенд был обновлен для поддержки пагинации здесь
        //
        // **ПРЕДПОЛОЖЕНИЕ**: API `GET /user/get/{login}/puzzles` существует
        // (Как мы обсуждали, для реализации ленивой загрузки)
        const response = await apiClient.get(`/user/get/${login.value}/puzzles`, {
            params: { page: page, limit: 10 }
        });
        if (page === 1) {
            puzzles.value = response.data.content;
        } else {
            puzzles.value.push(...response.data.content);
        }
        puzzleHasNext.value = response.data.hasNext;
        puzzleTotal.value = response.data.totalElements;
        puzzlePage.value = page;
    } catch (err) { 
        handleApiError(err);
        // Если API не существует, показываем ошибку в консоли
        console.error(`API /user/get/${login.value}/puzzles не найдено. Данные не загружены.`);
    } 
    finally { 
        contentLoading.value = false; 
        contentLoadingMore.value = false;
    }
};
const loadMorePuzzles = () => puzzleHasNext.value && !contentLoadingMore.value && fetchPuzzles(puzzlePage.value + 1);

const fetchMemberships = async (page = 1) => {
    if (page === 1) contentLoading.value = true;
    else contentLoadingMore.value = true;

    try {
        // Предполагаемый API, по аналогии с /my-memberships
        const response = await apiClient.get(`/tree-node/memberships/${login.value}`, {
            params: { page: page, limit: 10 }
        });
        if (page === 1) {
            memberships.value = response.data.content;
        } else {
            memberships.value.push(...response.data.content);
        }
        membershipHasNext.value = response.data.hasNext;
        membershipTotal.value = response.data.totalElements;
        membershipPage.value = page;
    } catch (err) { 
        handleApiError(err);
        console.error(`API /user/get/${login.value}/memberships не найдено. Данные не загружены.`);
    } 
    finally { 
        contentLoading.value = false; 
        contentLoadingMore.value = false;
    }
};
const loadMoreMemberships = () => membershipHasNext.value && !contentLoadingMore.value && fetchMemberships(membershipPage.value + 1);


// --- API: СЕССИИ ---
const closeSession = async (id) => {
    try {
        await apiClient.delete(`/user/session/${id}`);
        store.dispatch('addSuccessMessage', 'Сеанс успішно завершено');
        user.value.sessions = user.value.sessions.filter((session) => session.id !== id);
    } catch (err) { handleApiError(err); }
};

const closeCurrentSession = async () => {
    if (currentSessionId.value) {
        await closeSession(currentSessionId.value);
        store.dispatch('logout'); // Выход из Vuex
        router.push('/login'); // Редирект на логин
    }
};

// --- WATCHERS И MOUNT ---
watch(activeView, (newView) => {
    if (newView === 'puzzles' && puzzles.value.length === 0) {
        fetchPuzzles(1);
    } else if (newView === 'memberships' && memberships.value.length === 0) {
        fetchMemberships(1);
    }
});

watch(login, (newLogin) => {
    if (newLogin) {
        // Сброс состояния при просмотре другого профиля
        user.value = null;
        puzzles.value = [];
        memberships.value = [];
        puzzlePage.value = 1;
        membershipPage.value = 1;
        activeView.value = 'decisions';
        fetchUser();
    }
});

onMounted(async () => {
    await loadProgrammingLanguages(); // Для отображения названий языков
    await fetchUser();
});
</script>

<style lang="scss" scoped>
@import '@/assets/scss/variables.scss';

.user-page {
    max-width: 900px;
    margin: 0 auto;
}

.user-header-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    gap: 1.5rem;
    padding: 2rem;
    background-color: var(--weak-color);
    border-radius: 5px;
    border: 1px solid var(--secondary-color);

    @media (min-width: 600px) {
        flex-direction: row;
        text-align: left;
    }
}

.user-avatar {
    height: 150px;
    width: 150px;
    flex-shrink: 0;
    border-radius: 50%;
    background-color: var(--main-color);
    border: 2px solid var(--secondary-color);
    display: flex;
    align-items: center;
    justify-content: center;
    
    i {
        font-size: 80px;
        color: var(--secondary-color);
    }
}

.user-info {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    font-family: $form-font;

    .name {
        font-family: $secondary-font;
        font-size: 2em;
        font-weight: 600;
        color: var(--text-color);
    }
    .login {
        font-size: 1.2em;
        color: var(--secondary-color);
    }
    .email, .rating {
        font-size: 1em;
        color: var(--text-color);
        display: flex;
        align-items: center;
        gap: 8px;
        
        i { color: var(--secondary-color); }
        a { 
            color: var(--text-color); 
            text-decoration: none; 
            &:hover { text-decoration: underline; }
        }
    }
    .rating i { color: var(--warn-color); }

    .logout-btn {
        margin-top: 10px;
        align-self: flex-start;
        @media (max-width: 599px) {
            align-self: center;
        }
    }
}

.content-tabs {
    margin-top: 1.5rem;
}

.content-section {
    margin-top: 1.5rem;
    h2 {
        font-family: $secondary-font;
        color: var(--text-color);
        margin-bottom: 1rem;
    }
}

// Стили для всех списков (Задачи, Узлы)
.puzzles-list, .items-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}

.puzzle-item, .item-card {
    display: flex; 
    justify-content: space-between; 
    align-items: center; 
    gap: 15px; 
    background-color: var(--weak-color); 
    padding: 1rem; 
    border-radius: 5px; 
    border: 1px solid var(--secondary-color);
    font-family: $form-font;

    .item-info { flex-grow: 1; }
    .item-title { font-weight: 600; color: var(--text-color); }
    .item-details {
        font-size: 0.9em;
        color: var(--secondary-color);
        display: flex; 
        justify-content: space-between;
        align-items: flex-end;
    }
}

// Специфично для Узлов/Групп
.item-card {
    .item-icon { 
        font-size: 1.8em; 
        color: var(--info-color); 
        width: 30px;
        text-align: center;
    }
    .item-info {
        h3 { font-family: $secondary-font; margin: 0 0 5px; color: var(--text-color); }
        p { font-size: 0.9em; margin: 0; color: var(--secondary-color); }
    }
    .roles { 
        display: flex; 
        flex-wrap: wrap; 
        gap: 0.5rem; 
        margin-top: 0.75rem; 
    }
    .role-badge { 
        background-color: var(--secondary-color); 
        color: white; 
        padding: 0.2rem 0.6rem; 
        border-radius: 5px; 
        font-size: 0.8em; 
        font-weight: 600;
        cursor: pointer;
        transition: background-color 0.2s ease;
        &:hover {
            background-color: var(--info-color);
        }
    }
    .item-actions { display: flex; gap: 10px; }
}


// Стили для Решений
.decisions-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}
.decision-item {
    background-color: var(--weak-color);
    border-radius: 5px;
    border: 1px solid var(--secondary-color);
    padding: 1rem;
    font-family: $form-font;

    .decision-header {
        display: flex;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 10px;
        padding-bottom: 0.5rem;
        border-bottom: 1px solid var(--secondary-color);
        
        a {
            font-family: $secondary-font;
            font-size: 1.1em;
            font-weight: 600;
            color: var(--text-color);
            text-decoration: none;
            &:hover { text-decoration: underline; }
        }
        .language {
            font-size: 0.9em;
            color: var(--secondary-color);
        }
    }
    
    .decision-footer {
        display: flex;
        flex-wrap: wrap;
        gap: 15px;
        margin-top: 0.75rem;
        font-size: 0.9em;
        
        .score, .status, .date {
            display: flex;
            align-items: center;
            gap: 6px;
        }
        
        .score {
            font-weight: 600;
            color: var(--text-color);
            i { color: var(--secondary-color); }
            &.full-score i { color: var(--warn-color); }
        }
        
        .status {
            font-weight: 600;
            .isCorrect { color: var(--success-color); }
            .isWrong { color: var(--error-color); }
        }
        
        .date {
            color: var(--secondary-color);
            margin-left: auto;
        }
    }
}


// Стили для Сессий
.sessions-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}
.session-item {
    display: flex;
    align-items: center;
    gap: 15px;
    background-color: var(--weak-color);
    padding: 1rem;
    border-radius: 5px;
    border: 1px solid var(--secondary-color);

    .device-type {
        font-size: 2em;
        color: var(--secondary-color);
        width: 40px;
        text-align: center;
    }
    
    .session-info {
        flex-grow: 1;
        font-family: $form-font;
        
        .device-name {
            font-weight: 600;
            color: var(--text-color);
        }
        .device-ip, .session-time {
            display: block;
            font-size: 0.9em;
            color: var(--secondary-color);
        }
    }
}


.load-more-btn {
    width: 100%;
    margin-top: 1rem;
    padding: 10px 15px;
    background-color: var(--main-color);
    border: 1px solid var(--secondary-color);
    color: var(--text-color);
    border-radius: 5px;
    cursor: pointer;
    font-family: $form-font;
    font-weight: 600;
    transition: background-color 0.2s ease;
    min-height: 40px;

    &:hover:not(:disabled) {
        background-color: var(--weak-color);
    }
    &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
    }
}

.empty-message, .error-message {
    font-family: $form-font;
    color: var(--secondary-color);
    padding: 20px;
    text-align: center;
    background-color: var(--weak-color);
    border-radius: 3px;
    margin-top: 1.5rem;
}
.error-message {
    color: var(--error-color);
    h2 {
        color: var(--error-color);
    }
}
</style>
