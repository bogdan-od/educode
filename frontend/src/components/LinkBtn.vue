<!-- Компонент для створення кнопок та посилань з різними анімаціями та стилями -->
<template>
    <!-- RouterLink для навігації між сторінками -->
    <RouterLink @click="handleClick" v-if="role == 'link'" :to="to" class="btn" :class="{'bold': bold == 'true', [anim]: true, 'loading': isLoading}" :style="'--img: url(\'' + image + '\'); --op: ' + (1 - op) + ';'">
        <!-- Індикатор завантаження -->
        <img v-if="isLoading" src="@/assets/img/preloader_small.svg" alt="" class="preloader" :class="{'dark-theme': this.$store.getters.getThemeMode == 'dark'}">
        <!-- Стрілка для анімації "go" -->
        <i v-if="anim == 'go'" class="go-arrow fa-solid fa-right-long"></i>
        <!-- Контейнер для вмісту -->
        <div class="content">
            <slot></slot>
        </div>
    </RouterLink>

    <!-- Звичайна кнопка -->
    <button @click="handleClick" v-if="role == 'btn' || role == 'button'" :type="type" class="btn" :class="{'bold': bold == 'true', [anim]: true, 'loading': isLoading}" :style="'--img: url(\'' + image + '\'); --op: ' + (1 - op) + ';'" :disabled="disabled">
        <!-- Індикатор завантаження -->
        <img v-if="isLoading" src="@/assets/img/preloader_small.svg" alt="" class="preloader" :class="{'dark-theme': this.$store.getters.getThemeMode == 'dark'}">
        <!-- Стрілка для анімації "go" -->
        <i v-if="anim == 'go'" class="go-arrow fa-solid fa-right-long"></i>
        <!-- Контейнер для вмісту -->
        <div class="content">
            <slot></slot>
        </div>
    </button>
</template>

<style lang="scss" scoped>
    /* Основні стилі для кнопки */
    .btn {
        font-family: inherit;
        overflow: hidden;
        border: 1px solid #777777;
        border-radius: 5px;
        position: relative;
        display: inline-block;
        text-decoration: none;
        transition: all 0.2s cubic-bezier(.42,0,.76,1.99);
        background-image: var(--img);
        background-position: center;
        background-repeat: no-repeat;
        background-size: cover;
        color: var(--text-color);
        background-color: rgba(var(--button-bg-cover), var(--op));
        background-blend-mode: overlay;
        cursor: pointer;
        font-size: 1em;

        /* Стилі для контейнера вмісту */
        .content {
            padding: 10px 20px;
            height: 100%;
            width: 100%;
            transition: all 0.3s ease;
        }

        /* Стилі для анімації "go" */
        &.go {
            .go-arrow {
                position: absolute;
                top: 50%;
                transform: translate(-100%, -50%);
                font-size: 1.2em;
                transition: all 0.3s cubic-bezier(.42,0,.45,1.09);
                left: 0;
                font-size: 28px;
                transition-delay: 0.15s;
            }

            > .content {
                transition: padding 0.3s ease 0.35s, transform 0.5s cubic-bezier(.42,0,.45,1.09) 0.1s, margin-left 0s linear 0.35s;
            }

            /* Ефекти при наведенні для анімації "go" */
            &:hover {
                .go-arrow {
                    transform: translate(-100%, -50%);
                    left: 94%;
                    transition-delay: 0.15s;
                }

                > .content {
                    transform: translateX(200%);
                    margin-left: -200%;
                    padding-right: 48px;
                    transition-delay: transform 0ms margin-left 0.35s;
                }
            }
        }

        /* Стилі для індикатора завантаження */
        .preloader {
            object-fit: contain;
            height: 30px;
            position: absolute;
            left: 50%;
            transform: translate(-50%, -50%);
            top: 50%;

            &.dark-theme {
                filter: invert(100%);
            }
        }

        /* Стилі для стану завантаження */
        &.loading {
            *:not(.preloader) {
                opacity: 0;
            }

            color: transparent;
            position: relative;
        }

        /* Стилі для анімації масштабування */
        &.scale:hover {
            transform: scale(1.1);
        }

        /* Стилі для жирного тексту */
        &.bold {
            font-weight: bold;
        }

        /* Стилі для анімації масштабування фону */
        &.bg-scale {
            transition: all 0.35s cubic-bezier(.65,.05,.36,1);
            background-size: 100%;

            &:not(.center-s):hover {
                background-size: 180%;
                background-position-y: 100%;
                background-position-x: 50%;
            }

            &.center-s {
                background-size: 220%;

                &:hover {
                    background-size: 100%;
                }
            }
        }

        /* Стилі для вимкненого стану */
        &:disabled {
            opacity: 0.8;
            cursor: not-allowed;

            &:hover {
                transform: none;
            }
        }
    }
</style>

<script scoped>
export default {
    name: "LinkBtn",
    // Властивості компонента
    props: {
        to: {type: String, required: false, default: "#"}, // URL для переходу
        img: {type: String, required: false, default: "wave1.svg"}, // Фонове зображення
        role: {type: String, required: false, default: "link"}, // Тип елементу (посилання чи кнопка)
        bold: {type: String, required: false, default: "false"}, // Жирний текст
        type: {type: String, required: false, default: "button"}, // Тип кнопки
        disabled: {type: Boolean, required: false, default: false}, // Вимкнений стан
        op: {type: String, required: false, default: "0.65"}, // Прозорість фону
        anim: {type: String, required: false, default: "scale"}, // Тип анімації
        isLoadingParam: {type: Boolean, required: false, default: false}, // Параметр завантаження
        click: {type: String, required: false, default: ""}, // що визивається при кліку
    },
    // Методи компонента
    methods: {
        // Обробник кліку
        handleClick() {
            if (!this.disabled) {
                if (this.click == 'load')
                    this.isLoading = true;

                this.$emit('click');
            }
        },
    },
    // Дані компонента
    data() {
        return {
            image: "", // Шлях до зображення
            isLoading: this.isLoadingParam, // Стан завантаження
        }
    },
    // Хук життєвого циклу
    mounted() {
        const self = this;
        // Завантаження зображення
        import(`@/assets/img/${this.img}`)
        .then(module => {
            self.image = module.default;
        })
        .catch(err => {
            console.error('Image loading error: ', err);
        });
    }
}
</script>