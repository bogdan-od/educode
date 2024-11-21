// Імпортуємо необхідні функції з vue-router та сховище
import { createRouter, createWebHistory } from 'vue-router';
import store from '@/store';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // Головна сторінка
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
      meta: {
        title: 'Головна сторінка - Educode',
      }
    },
    // Сторінка "Про нас"
    {
      path: '/about',
      name: 'About',
      component: () => import('@/views/About.vue'),
      meta: {
        title: 'Про нас - Educode',
      }
    },
    // Сторінка входу
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: {
        title: 'Увійти у аккаунт на Educode',
      }
    },
    // Сторінка реєстрації
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: {
        title: 'Створити аккаунт на Educode',
      }
    },
    // Сторінка зі списком задач
    {
      path: '/puzzles/:page?', // Використовуємо динамічний параметр page, він не обов'язковий, тому ставимо знак питання
      name: 'Puzzles',
      component: () => import('@/views/Puzzles.vue'),
      meta: {
        title: 'Задачі на Educode',
      }
    },
    // Сторінка окремої задачі
    {
      path: '/puzzle/:id', // Використовуємо динамічний параметр id
      name: 'Puzzle',
      component: () => import('@/views/Puzzle.vue'),
      meta: {
        title: 'Перегляд задачі на Educode',
      }
    },
    // Сторінка профілю користувача
    {
      path: '/user/:login?', // Використовуємо динамічний параметр login, він не обов'язковий, тому ставимо знак питання
      name: 'User',
      component: () => import('@/views/User.vue'),
      meta: {
        title: 'Профіль користувача - Educode',
      }
    },
    // Таблиця лідерів
    {
      path: '/leaderboard',
      name: 'Leaderboard',
      component: () => import('@/views/Leaderboard.vue'),
      meta: {
        title: 'Таблиця лідерів - Educode',
      }
    },
    // Сторінка додавання нової задачі
    {
      path: '/add/puzzle',
      name: 'AddPuzzle',
      component: () => import('@/views/AddPuzzle.vue'),
      meta: {
        title: 'Додавання задачі - Educode',
        requireAuth: true,
        requireRole: 'ROLE_PUZZLE_CREATOR',
      }
    },
    // Сторінка 404 (не знайдено)
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFound.vue'),
      meta: {
        title: 'Сторінка не знайдена - Educode',
      }
    },
  ],
});


// Перехоплювач навігації для перевірки прав доступу та встановлення заголовку сторінки
router.beforeEach((to, from, next) => {
  // Перевіряємо чи потрібна авторизація та відповідна роль для доступу до сторінки
  if ((to.meta.requireAuth && !store.state.accessToken) || (to.meta.requireRole && !store.getters.getUserRoles.find(el => el == to.meta.requireRole))) {
    next('/login');
  } else {
    next();
  }
  // Встановлюємо заголовок сторінки
  document.title = to.meta.title || 'Educode';
  next();
});

export default router;