<!-- Шаблон сторінки 404 (Сторінка не знайдена) -->
<template>
    <!-- Головний контейнер з динамічними стилями для анімації та відступів -->
    <div class="not-found" :style="`--main-paddings: ${mainPaddingsVertical}px ${mainPaddingsHorizontal}px;`">
        <!-- Заголовок з анімованими цифрами 404 -->
        <h1>
            <span style="--i:0">4</span><span style="--i:1">0</span><span style="--i:2">4</span>
        </h1>
        <!-- Компонент з текстом, що друкується -->
        <PrintingText text="Сторінка не знайдена"></PrintingText>
        <!-- Додатковий відступ -->
        <div class="height"></div>
        <!-- Кнопка для повернення на головну сторінку -->
        <LinkBtn to="/" img="waves1.svg" anim="bg-scale center-s" bold="true">На головну</LinkBtn>
    </div>
</template>

<script scoped>
// Імпорт необхідних компонентів
import PrintingText from '@/components/PrintingText.vue';
import LinkBtn from '../components/LinkBtn.vue';

export default {
    name: "NotFound",
    // Реєстрація використовуваних компонентів
    components: {
        PrintingText,
        LinkBtn,
    },
    // Початкові дані компонента
    data() {
        return {
            mainPaddingsVertical: 0, // Вертикальні відступи
            mainPaddingsHorizontal: 0, // Горизонтальні відступи
        }
    },
    // Хук життєвого циклу, викликається після монтування компонента
    mounted() {
        // Отримання та встановлення відступів з головного елементу
        this.mainPaddingsHorizontal = -1 * Number(getComputedStyle(document.querySelector('main')).paddingLeft.replace('px', ''));
        this.mainPaddingsVertical = -1 * Number(getComputedStyle(document.querySelector('main')).paddingTop.replace('px', ''));

        // Оновлення відступів при зміні розміру вікна
        window.addEventListener('resize', () => {
            this.mainPaddingsHorizontal = -1 * Number(getComputedStyle(document.querySelector('main')).paddingLeft.replace('px', ''));
            this.mainPaddingsVertical = -1 * Number(getComputedStyle(document.querySelector('main')).paddingTop.replace('px', ''));
        });
    }
}
</script>

<style lang="scss" scoped>
// Імпорт шрифту для цифр 404
@import url('https://fonts.googleapis.com/css2?family=Rubik+Spray+Paint&display=swap');
// Імпорт міксинів SCSS
@import '@/assets/scss/mixins.scss';

// Стилі для головного контейнера
.not-found {
    @include display-flex($flex-direction: column);
    padding: 100px 0;
    background: linear-gradient(45deg, #60d37d54, #9fad524d);
    width: 100vw;
    margin: var(--main-paddings);
    min-height: calc(100vh - 300px);
    
    // Стилі для заголовка 404
    h1 {
        position: relative;
        text-align: center;
        font-size: 260px;
        height: 75px;
        top: -185px;

        // Стилі для окремих цифр
        span {
            font-family: 'Rubik Spray Paint', cursive;
            display: inline-block;
            font-size: 100px;
            position: relative;
            animation: animation-numbers 1s ease-in-out infinite;
            animation-delay: calc(var(--i) * 0.1s);
        }
    }

    // Клас для додаткового відступу
    .height {
        height: 50px;
    }
}

// Анімація для цифр 404
@keyframes animation-numbers {
    0%, 100% {
        top: 0;
    }
    50% {
        top: -30px;
    }
}

</style>