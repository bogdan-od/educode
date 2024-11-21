<!-- Компонент для редактора тексту Quill -->
<template>
    <div>
        <!-- Контейнер для редактора Quill -->
        <div ref="editor" class="quill_text_editor"></div>
    </div>
</template>

<script scoped>
import Quill from 'quill';
import 'quill/dist/quill.snow.css';

export default {
    name: 'QuillEditor',
    // Вхідні параметри компонента
    props: {
        // Значення тексту редактора
        value: {
            type: String,
            default: '',
        },
        // Функція, яка викликається при оновленні тексту
        onUpdate: {
            type: Function,
            default: () => {},
        }
    },
    // Ініціалізація редактора після монтування компонента
    mounted() {
    this.quill = new Quill(this.$refs.editor, {
            theme: 'snow',
            // Налаштування панелі інструментів
            modules: {
                toolbar: [
                    ['bold', 'italic', 'underline', 'strike'], // Кнопки форматування тексту
                    [{'list': 'ordered'}, {'list': 'bullet'}], // Кнопки списків
                    [{'align': []}], // Кнопки вирівнювання
                    ['link'], // Кнопка додавання посилання
                ],
            },
        });

        // Встановлення початкового значення
        this.quill.root.innerHTML = this.value;

        // Відслідковування змін тексту
        this.quill.on('text-change', () => {
            const html = this.quill.root.innerHTML;
            this.onUpdate(html);
        });
    },
    // Спостереження за змінами вхідного значення
    watch: {
        value(newValue) {
            if (newValue !== this.quill.root.innerHTML) {
                this.quill.root.innerHTML = newValue;
            }
        },
    },
}
</script>

<style lang="scss" scoped>
/* Стилі для контейнера редактора */
div {
    height: 500px;
    width: 100%;
    margin-bottom: 50px;
}

/* Стилі для панелі інструментів */
:deep(.ql-toolbar) {
    background-color: #fff6;
}

/* Стилі для поля вводу в спливаючій підказці */
:deep(.ql-tooltip > input:nth-child(2)) {
    color: #333;
}
</style>