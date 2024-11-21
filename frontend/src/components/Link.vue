<!-- Компонент для створення посилань з можливістю використання RouterLink або звичайного тегу <a> -->
<template>
    <!-- Використовуємо RouterLink, якщо властивість 'to' не пуста -->
    <RouterLink v-if="to != ''" :to="to" :class="{'active': active, 'underline': line}">
        <slot></slot>
    </RouterLink>
    <!-- Використовуємо звичайне посилання, якщо властивість 'to' пуста -->
    <a v-else :href="href" :class="{'active': active, 'underline': line}">
        <slot></slot>
    </a>
</template>

<style lang="scss" scoped>
    /* Стилі для посилань */
    a {
        color: var(--text-color);
        position: relative;
        text-decoration: none;

        /* Підкреслення для посилань при наведенні та активному стані */
        &::after, &.underline:hover::after {
            content: '';
            position: absolute;
            display: block;
            height: 3px;
            background-color: var(--text-color);
            border-radius: 10px;
            transition: all 0.2s cubic-bezier(.67,.02,.67,1.52);
            bottom: -3px;
            width: 0%;
            left: 50%;
        }

        /* Стиль для активного посилання */
        &.active::after {
            width: 20%;
            left: 40%;
        }

        /* Стиль при наведенні та для підкресленого посилання */
        &:hover::after, &.underline::after {
            width: 100%;
            left: 0%;
        }
    }
</style>

<script scoped>
export default {
    name: "Link",
    // Властивості компонента
    props: {
        to: { type: String, required: false, default: "" }, // Шлях для RouterLink
        href: { type: String, required: false, default: "" }, // URL для звичайного посилання
        active: { type: Boolean, default: false }, // Прапорець активного стану
        line: { type: Boolean, default: false }, // Прапорець підкреслення
    },
}
</script>