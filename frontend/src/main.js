// Імпортуємо необхідні модулі та бібліотеки
import { createApp } from "vue";
import App from './App.vue';
import router from "./router";
import store from "./store";
import axios from "axios";
import VueAxios from 'vue-axios';

window.addEventListener('error', e => {
  console.log('global error event:', e);
  console.log('  e.message:', e.message);
  console.log('  e.error (real Error obj, if any):', e.error);
});

window.addEventListener('unhandledrejection', e => {
  console.log('unhandledrejection:', e);
  console.log('  reason:', e.reason);
});

// Створюємо екземпляр додатку Vue
// Використовуємо необхідні плагіни (router, store, axios)
// Монтуємо додаток до елементу з id "app"
createApp(App)
    .use(router)
    .use(store)
    .use(VueAxios, axios)
    .mount("#app");