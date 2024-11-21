<!-- Компонент для відображення прелоадера (індикатора завантаження) -->
<template>
    <!-- Основний контейнер прелоадера з динамічними класами та фоном -->
    <div class="preloader" :class="{'full-screen': fullScreen, 'dark-theme': this.$store.getters.getThemeMode == 'dark'}" :style="`--bg: ${bg}`">
        <!-- Зображення прелоадера -->
        <img :src="img" alt="Loading...">
    </div>
</template>

<style lang="scss" scoped>
/* Стилі для прелоадера */
.preloader {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    width: 100%;
    max-width: 100vw;
    max-height: 100vh;
    background-color: var(--bg);
    top: 0;
    left: 0;
    z-index: 10000;
    position: absolute;

    /* Стилі для повноекранного режиму */
    &.full-screen {
        position: fixed;
        height: 100vh;
        width: 100vw;
    }
    
    /* Стилі для зображення прелоадера */
    img {
        min-height: 30%;
        min-width: 30%;
        object-fit: contain;
        max-height: 240px;
        max-width: 240px;
    }

    /* Інвертування кольорів зображення для темної теми */
    &.dark-theme img {
        filter: invert(100%);
    }
}
</style>

<script scoped>
import store from '../store';

export default {
    name: "Preloader",
    // Вхідні параметри компонента
    props: {
        fullScreen: {type: Boolean, required: false, default: false}, // Повноекранний режим
        size: {type: String, required: false, default: "big"}, // Розмір прелоадера
        bg: {type: String, required: false, default: "var(--main-color)"}, // Колір фону
    },
    // Дані компонента
    data() {
        return {
            img: require("@/assets/img/preloader_" + this.size + ".svg") // Шлях до зображення прелоадера
        }
    }
}
</script>