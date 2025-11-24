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
        
      }
    },
    {
      path: '/notifications/:page?',
      name: "Notifications",
      component: () => import('@/views/Notifications.vue'),
      meta: {
        title: "Повідомлення - Educode",
        requireAuth: true
      }
    },
    {
      path: '/edit/puzzle/:id?',
      name: "EditPuzzle",
      component: () => import('@/views/EditPuzzle.vue'),
      meta: {
        title: "Редагування задачі - Educode",
        requireAuth: true,
        
      }
    },
    {
      path: '/create/checker/:id?',
      name: "CreateChecker",
      component: () => import('@/views/CreateChecker.vue'),
      meta: {
        title: "Створення checker'а - Educode",
        requireAuth: true,
        
      }
    },
    {
      path: '/edit/checker/:id?',
      name: "EditChecker",
      component: () => import('@/views/EditChecker.vue'),
      meta: {
        title: "Редагування checker'а - Educode",
        requireAuth: true,
        
      }
    },
    {
      path: '/checkers/:page?',
      name: "Checkers",
      component: () => import('@/views/Checkers.vue'),
      meta: {
        title: "Перевіряючі програми - Educode",
        requireAuth: true,
        
      }
    },
    {
      path: '/checker/:id?',
      name: "Checker",
      component: () => import('@/views/Checker.vue'),
      meta: {
        title: "Перевіряюча програма - Educode",
        requireAuth: true,
        
      }
    },
    {
      path: '/groups/:page?',
      name: "Groups",
      component: () => import('@/views/Groups.vue'),
      meta: {
        title: "Групи - Educode",
      }
    },
    {
      path: '/group/:id',
      name: "Group",
      component: () => import('@/views/Group.vue'),
      meta: {
        title: "Група - Educode",
      }
    },
    {
      path: '/create/group',
      name: "CreateGroup",
      component: () => import('@/views/CreateGroup.vue'),
      meta: {
        title: "Створити групу - Educode",
      }
    },
    {
      path: '/edit/group/:id',
      name: "EditGroup",
      component: () => import('@/views/EditGroup.vue'),
      meta: {
        title: "Редагувати групу - Educode",
      }
    },
    {
      path: '/homeworks',
      name: "Homeworks",
      component: () => import('@/views/Homeworks.vue'),
      meta: {
        title: "Домашні завдання - Educode",
      }
    },
    {
      path: '/homework/:id',
      name: "Homework",
      component: () => import('@/views/Homework.vue'),
      meta: {
        title: "Домашнє завдання - Educode",
      }
    },
    {
      path: '/create/homework',
      name: 'CreateHomework',
      component: () => import('@/views/CreateHomework.vue'),
      meta: {
        title: "Створити домашнє завдання - Educode",
      }
    },
    {
      path: '/edit/homework/:id',
      name: 'EditHomework',
      component: () => import('@/views/EditHomework.vue'),
      meta: {
        title: "Редагувати домашнє завдання - Educode",
      }
    },
    {
      path: '/nodes/:id?',
      name: 'NodeBrowser',
      component: () => import('@/views/NodeBrowser.vue'),
      props: true,
      meta: {
        title: "Узли - Educode",
      },
    },
    {
      path: '/create/node',
      name: 'CreateNode',
      component: () => import('@/views/CreateNode.vue'),
      meta: {
        title: "Створити вузел - Educode",
      },
    },
    {
      path: '/edit/node/:id',
      name: 'EditNode',
      component: () => import('@/views/EditNode.vue'),
      meta: {
        title: "Редагувати вузел - Educode",
      }
    },
    {
      path: '/group/:id/users',
      name: 'GroupUserManagement',
      component: () => import('@/views/GroupUserManagement.vue'),
      meta: {
        title: "Управління користувачами у групах - Educode",
      }
    },
    {
      path: '/node/:id/users',
      name: 'NodeUserManagement',
      component: () => import('@/views/NodeUserManagement.vue'),
      meta: {
        title: "Управління користувачами у вузлах - Educode",
      }
    },
    {
      path: '/node/:id',
      name: 'Node',
      component: () => import('@/views/Node.vue'),
      meta: {
        title: "Вузел - Educode",
      }
    },
    {
      path: '/manage/puzzle/:id',
      name: 'PuzzleManagement',
      component: () => import('@/views/PuzzleManagement.vue'),
      meta: {
        title: "Управління задачами - Educode",
      }
    },
    {
      path: '/my-nodes-groups',
      name: 'MyNodesAndGroups',
      component: () => import('../views/MyNodesAndGroups.vue')
    },
    {
      path: '/my-homework',
      name: 'MyHomework',
      component: () => import('../views/MyHomework.vue')
    },
    {
      path: '/tree-node/:id/members',
      name: 'MemberManagement',
      component: () => import('@/views/MemberManagement.vue'),
      meta: {
        title: "Управління учасниками"
      }
    },
    {
      path: '/tree-node/:id/invitations',
      name: 'InvitationManagement',
      component: () => import('@/views/InvitationManagement.vue'),
      meta: {
        title: "Управління запрошеннями"
      }
    },
    {
      path: '/join/:code',
      name: 'AcceptInvitation',
      component: () => import('@/views/AcceptInvitation.vue'),
      meta: {
        title: "Перехід за запрошенням"
      }
    },
    {
      path: '/join',
      name: 'JoinHub',
      component: () => import('@/views/JoinHub.vue'),
      meta: {
        title: "Перехід за запрошенням"
      }
    },
    {
      path: '/my-invitations',
      name: 'MyInvitations',
      component: () => import('@/views/MyInvitations.vue'),
      meta: {
        title: "Мої запрошення"
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
  if ((to.meta.requireAuth && !store.state.accessToken) || (to.meta.requireRole && !store.getters.getUserPermissions.find(el => el == to.meta.requireRole))) {
    next('/login');
  } else {
    next();
  }
  // Встановлюємо заголовок сторінки
  document.title = to.meta.title || 'Educode';
  next();
});

export default router;