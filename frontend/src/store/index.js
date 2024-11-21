import Vuex from 'vuex';
import * as jose from 'jose'

// Експорт сховища Vuex з налаштуваннями
export default new Vuex.Store({
  // Початковий стан додатку
  state: {
    accessToken: localStorage.getItem('access_token') || '',
    refreshToken: localStorage.getItem('refresh_token') || '',
    messages: [],
    theme: localStorage.getItem('theme') || 'light',
  },
  // Мутації для зміни стану
  mutations: {
    // Встановлення токену доступу
    setAccessToken(state, token) {
      state.accessToken = token;
      localStorage.setItem('access_token', token);
    },
    // Очищення токену доступу
    clearAccessToken(state) {
      state.accessToken = null;
      localStorage.removeItem('access_token');
    },
    // Встановлення токену оновлення
    setRefreshToken(state, token) {
      state.refreshToken = token;
      localStorage.setItem('refresh_token', token);
    },
    // Очищення токену оновлення
    clearRefreshToken(state) {
      state.refreshToken = null;
      localStorage.removeItem('refresh_token');
    },
    // Додавання повідомлення про успіх
    addSuccessMessage(state, message) {
      state.messages.push({
        type: 'success',
        message: message,
        until: new Date().getTime() + 10000,
      });
    },
    // Очищення повідомлень про успіх
    clearSuccessMessage(state) {
      state.messages = state.messages.filter(message => message.type !== 'success');
    },
    // Додавання повідомлення про помилку
    addErrorMessage(state, message) {
      state.messages.push({
        type: 'error',
        message: message,
        until: new Date().getTime() + 10000,
      });
    },
    // Очищення повідомлень про помилку
    clearErrorMessage(state) {
      state.messages = state.messages.filter(message => message.type !== 'error');
    },
    // Очищення прострочених повідомлень
    clearExpiredMessages(state) {
      state.messages = state.messages.filter(message => message.until > new Date().getTime());
    },
    // Встановлення теми
    setTheme(state, theme) {
      state.theme = theme;
      localStorage.setItem('theme', theme);
    },
    // Очищення теми
    clearTheme(state) {
      state.theme = null;
      localStorage.removeItem('theme');
    }
  },
  // Дії для виклику мутацій
  actions: {
    // Вхід користувача
    login({ commit }, payload) {
      try {
        const { accessToken, refreshToken } = payload;

        if (!accessToken || !refreshToken) {
          console.error('Invalid login payload: ', payload);
          return;
        }

        commit('setAccessToken', accessToken);
        commit('setRefreshToken', refreshToken);
      } catch (error) {
        console.error('Login error:', error);
      }
    },
    // Вихід користувача
    logout({ commit }) {
      commit('clearAccessToken');
      commit('clearRefreshToken');
    },
    // Встановлення теми
    setTheme({ commit }, theme) {
      commit('setTheme', theme);
    },
    // Додавання повідомлення про успіх
    addSuccessMessage({ commit }, message) {
      commit('addSuccessMessage', message);
    },
    // Додавання повідомлення про помилку
    addErrorMessage({ commit }, message) {
      commit('addErrorMessage', message);
    },
    // Очищення повідомлення про успіх
    clearSuccessMessage({ commit }) {
      commit('clearSuccessMessage');
    },
    // Очищення повідомлення про помилку
    clearErrorMessage({ commit }) {
      commit('clearErrorMessage');
    },
    // Очищення прострочених повідомлень
    clearExpiredMessages({ commit }) {
      commit('clearExpiredMessages');
    },
    // Очищення токену доступу
    clearAccessToken({ commit }) {
      commit('clearAccessToken');
    },
    // Очищення токену оновлення
    clearRefreshToken({ commit }) {
      commit('clearRefreshToken');
    }
  },
  // Геттери для отримання даних зі стану
  getters: {
    // Перевірка автентифікації користувача
    isAuthenticated(state) {
      if (state.accessToken == null || state.accessToken === '')
        return false;

      return !!state.accessToken;
    },
    // Отримання токену доступу
    getAccessToken(state) {
      return state.accessToken;
    },
    // Отримання токену оновлення
    getRefreshToken(state) {
      return state.refreshToken;
    },
    // Отримання поточної теми
    getTheme(state) {
      return state.theme;
    },
    // Отримання режиму теми
    getThemeMode(state) {
      return state.theme === 'light' ? 'light' : state.theme === 'dark' ? 'dark' : window.matchMedia("(prefers-color-scheme: dark)").matches ? 'dark' : 'light';
    },
    // Отримання всіх повідомлень
    getMessages(state) {
      return state.messages;
    },
    // Отримання даних з jwt токену за допомогою jose
    getTokenClaims(state) {
      try {
        const claims = jose.decodeJwt(state.accessToken);
        return claims ? claims : null;
      } catch (error) {
        return null;
      }
    },
    // Отримання імені поточного користувача
    getCurrentUsername(state, getters) {
      const claims = getters.getTokenClaims;
      return claims ? claims.username : null;
    },
    // Отримання часу закінчення дії токену
    getAccessTokenExpiry(state, getters) {
      const claims = getters.getTokenClaims;
      return claims ? claims.exp * 1000 : null;
    },
    // Отримання ролей користувача
    getUserRoles(state, getters) {
      const claims = getters.getTokenClaims;
      return claims ? claims.roles : [];
    }
  }
});
