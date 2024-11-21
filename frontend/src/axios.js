// Імпорт необхідних модулів
import axios from 'axios';
import store from './store';
import router from './router';

// Створення екземпляру axios з базовими налаштуваннями
const apiClient = axios.create({
  baseURL: process.env.VUA_APP_API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Прапорець для відстеження процесу оновлення токену
var isTokenRefreshing = false;

// Перехоплювач запитів
apiClient.interceptors.request.use((config) => {
  const token = store.state.accessToken;

  // Якщо токен оновлюється, чекаємо завершення процесу
  if (isTokenRefreshing) {
    return new Promise((resolve) => {
          const interval = setInterval(() => {
            const newToken = store.state.accessToken;
            if (!isTokenRefreshing && newToken != token) {
              clearInterval(interval);
              if (newToken) {
                config.headers.Authorization = `Bearer ${newToken}`;
              }
              resolve(config);
            }
          }, 100);
        });  
  }

  // Встановлюємо прапорець, якщо це запит на оновлення токену
  if (config.url == '/user/auth/refresh-token') {
    isTokenRefreshing = true;
  }

  // Додаємо токен до заголовків запиту
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});

// Функція перевірки стандартної відповіді від сервера
const checkForStandardResponse = (response) => {
  if (!response.data) {
    return;
  }

  // Перевірка на необхідність виходу з системи
  if (response.data['not_auth'] != undefined && response.data['not_auth'] == "1") {
    store.dispatch('logout');
    router.push('/login');
  }
  // Перевірка на необхідність перенаправлення
  if (response.data['redirect_to'] != undefined && response.data['redirect_to'] != "") {
    router.push(response.data['redirect_to']);
  }
  // Перевірка на необхідність видалення токену доступу
  if (response.data['del_access_token'] != undefined && response.data['del_access_token'] == "1") {
    store.commit('clearAccessToken');
  }
  // Перевірка на необхідність видалення токену оновлення
  if (response.data['del_refresh_token'] != undefined && response.data['del_refresh_token'] == "1") {
    store.commit('clearRefreshToken');
  }
}

// Перехоплювач відповідей
apiClient.interceptors.response.use(response => {
  // Скидаємо прапорець після оновлення токену
  if (response.config && response.config.url === '/user/auth/refresh-token') {
    isTokenRefreshing = false;
  }

  if (!response.data) {
    return response;
  }

  // Оновлення токенів у сховищі
  if (response.data['new_access_token']) {
    store.commit('setAccessToken', response.data['new_access_token']);
  }
  if (response.data['new_refresh_token']) {
    store.commit('setRefreshToken', response.data['new_refresh_token']);
  }

  checkForStandardResponse(response);
  
  return response;
}, error => {
  // Обробка помилок
  if (error.response && error.response.config && error.response.config.url === '/user/auth/refresh-token') {
    isTokenRefreshing = false;
  }

  // Перенаправлення на сторінку входу при помилці авторизації
  if (error.response && error.response.status === 401 && router.currentRoute.value.meta.requireAuth) {
    store.dispatch('logout');
    router.push('/login');
  }

  checkForStandardResponse(error.response);

  return Promise.reject(error);
});


export default apiClient;