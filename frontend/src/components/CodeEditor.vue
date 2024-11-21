<template>
    <div ref="editorElement" class="code-editor"></div>
</template>

<script>
import { defineComponent, ref, watch, onMounted, onBeforeUnmount } from 'vue'
// import { EditorState } from '@codemirror/state'
import { EditorView } from '@codemirror/view'
import { basicSetup } from '@codemirror/basic-setup'
import { javascript } from '@codemirror/lang-javascript'
import { python } from '@codemirror/lang-python'
import { cpp } from '@codemirror/lang-cpp'
import { java } from '@codemirror/lang-java'
import { rust } from '@codemirror/lang-rust'
import { php } from '@codemirror/lang-php'
import { githubDark, githubLight } from '@uiw/codemirror-themes-all'

export default defineComponent({
name: 'CodeEditor',
props: {
    modelValue: String,
    language: String,
    isDarkTheme: {
    type: Boolean,
    default: false
    }
},
emits: ['update:modelValue'],
setup(props, { emit }) {
    const editorElement = ref(null)
    let view = null

    // Отримуємо розширення теми
    const getThemeExtension = () => {
    return props.isDarkTheme ? githubDark : githubLight
    }

    // Отримуємо розширення мови програмування
    const getLanguageExtension = (lang) => {
    switch(lang?.toLowerCase()) {
        case 'python':
        case 'pypy':
        return python()
        case 'node':
        return javascript()
        case 'cpp':
        case 'cpp-with-gmp':
        case 'c':
        return cpp()
        case 'java':
        return java()
        case 'rust':
        return rust()
        case 'php':
        return php()

        // Для інших мов використовуємо базову підсвітку
        case 'ruby':
        case 'swift':
        case 'kotlin':
        case 'perl':
        case 'go':
        case 'lua':
        case 'pascal':
        case 'haskell':
        case 'dart':
        case 'dotnet':
        case 'mono':
        case 'assembler':
        case 'd-gdc':
        return cpp()

        default:
        return cpp()
    }
    }

    // Слідкуємо за зміною теми
    watch(() => props.isDarkTheme, (newTheme) => {
    if (view) {
        view.dispatch({
        effects: EditorView.reconfigure.of([
            basicSetup,
            getLanguageExtension(props.language),
            getThemeExtension()
        ])
        })
    }
    })

    // Слідкуємо за зміною мови
    watch(() => props.language, (newLang) => {
    if (view) {
        view.dispatch({
        effects: EditorView.reconfigure.of([
            basicSetup,
            getLanguageExtension(newLang),
            getThemeExtension()
        ])
        })
    }
    })

    // Ініціалізація редактора при монтуванні компонента
    onMounted(() => {
    view = new EditorView({
        parent: editorElement.value,
        doc: props.modelValue,
        extensions: [
        basicSetup,
        getLanguageExtension(props.language),
        getThemeExtension(),
        EditorView.updateListener.of(update => {
            if (update.docChanged) {
            emit('update:modelValue', update.state.doc.toString())
            }
        })
        ]
    })
    })

    // Очищення ресурсів перед видаленням компонента
    onBeforeUnmount(() => {
    if (view) {
        view.destroy()
    }
    })

    return {
    editorElement
    }
}
})
</script>

<style scoped>
.code-editor {
height: 400px;
border: 1px solid #ddd;
}
</style>