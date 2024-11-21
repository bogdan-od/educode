<!-- Компонент для анімації тексту, який друкується -->
<template>
    <!-- Заголовок з анімацією, який використовує задані шрифти та класи -->
    <h2 class="writer" :style="`--ff: ${fontFamily};`" :class="{'animate': animate}">
        <!-- Цикл по кожній літері тексту з анімацією -->
        <span v-for="letter, i in letters" :key="i" :style="`--i: ${i};`" :class="{'space': letter == ' '}">{{ letter }}</span>
    </h2>
</template>

<script scoped>
export default {
    name: "PrintingText",
    // Вхідні параметри компонента
    props: {
        text: {
            type: String,
            required: true // Обов'язковий текст для анімації
        },
        animate: {
            type: Boolean,
            required: false,
            default: false, // Чи потрібна анімація
        },
        writeTime: {
            type: Number,
            required: false,
            default: 100, // Час затримки між появою літер
        },
        fontFamily: {
            type: String,
            required: false,
            default: 'Rubik Spray Paint', // Стандартний шрифт
        },
    },
    // Локальні дані компонента
    data() {
        return {
            letters: [], // Масив літер для анімації
        }
    },
    // Хук життєвого циклу, викликається після монтування компонента
    mounted() {
        var letters = this.text.split(''); // Розбиваємо текст на літери
        var i = 0;
        // Додаємо кожну літеру з затримкою
        for (let letter of letters) {
            setTimeout(() => {
                this.letters.push(letter);
            }, this.writeTime * i);
            i++;
        }
    }
}
</script>

<style lang="scss" scoped>
/* Імпортуємо шрифт з Google Fonts */
@import url('https://fonts.googleapis.com/css2?family=Rubik+Spray+Paint&display=swap');

/* Стилі для контейнера тексту */
.writer {
    margin-top: 70px;

    > * {
        display: inline-block;
        font-size: 50px;
        font-family: var(--ff);

        /* Стиль для пробілів */
        &.space {
            width: 20px;
        }

        /* Анімація для літер */
        &.animate {
            animation: animation-letters 1s ease-in-out infinite;
            animation-delay: calc(var(--i) * 0.1s);
        }
    }
}

/* Ключові кадри анімації */
@keyframes animation-letters {
    0%, 100% {
        transform: scale(1);
    }
    50% {
        transform: scale(1.2);
    }
}
</style>