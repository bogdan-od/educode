// Імпортуємо необхідні модулі та бібліотеки
import { createApp } from "vue";
import App from './App.vue';
import router from "./router";
import store from "./store";
import axios from "axios";
import VueAxios from 'vue-axios';

// Створюємо екземпляр додатку Vue
// Використовуємо необхідні плагіни (router, store, axios)
// Монтуємо додаток до елементу з id "app"
createApp(App)
    .use(router)
    .use(store)
    .use(VueAxios, axios)
    .mount("#app");