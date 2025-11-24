<template>
    <div class="join-hub-page">
        <div class="card">
            <div class="icon-wrapper">
                <i class="fa-solid fa-door-open"></i>
            </div>
            <h1>Приєднатися до вузла</h1>
            <p>Введіть код запрошення, щоб отримати доступ до навчальних матеріалів або групи.</p>
            
            <form @submit.prevent="goToJoin" class="join-form">
                <div class="input-wrapper" :class="{ 'has-error': error }">
                    <input 
                        v-model="code" 
                        type="text" 
                        placeholder="Введіть код " 
                        autofocus
                        @input="error = ''"
                    />
                </div>
                <p v-if="error" class="error-text">{{ error }}</p>
                
                <LinkBtn 
                    role="button" 
                    type="submit" 
                    anim="go" 
                    img="wave2.svg" 
                    :disabled="!code.trim()"
                    class="join-btn"
                >
                    Перейти
                </LinkBtn>
            </form>
            
            <div class="divider">або</div>
            
            <LinkBtn to="/my-invitations" color="secondary" size="sm">
                Переглянути мої запрошення
            </LinkBtn>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import LinkBtn from '@/components/LinkBtn.vue';

const router = useRouter();
const code = ref('');
const error = ref('');

const goToJoin = () => {
    const cleanCode = code.value.trim();
    if (!cleanCode) {
        error.value = 'Будь ласка, введіть код.';
        return;
    }
    // Простая валидация формата (опционально)
    if (cleanCode.length < 5) {
        error.value = 'Код занадто короткий.';
        return;
    }
    
    router.push(`/join/${cleanCode}`);
};
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.join-hub-page {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 60vh;
    padding: 20px;
}

.card {
    width: 100%;
    max-width: 450px;
    background-color: var(--weak-color);
    padding: 2.5rem;
    border-radius: 10px;
    box-shadow: var(--strong-box-shadow);
    text-align: center;
    border: 1px solid var(--secondary-color);
    
    h1 {
        font-family: $secondary-font;
        color: var(--text-color);
        margin-bottom: 0.5rem;
    }
    
    p {
        font-family: $form-font;
        color: var(--secondary-color);
        margin-bottom: 2rem;
        font-size: 0.95em;
    }
}

.icon-wrapper {
    width: 80px;
    height: 80px;
    background-color: var(--main-color);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1.5rem;
    border: 2px solid var(--secondary-color);
    
    i {
        font-size: 35px;
        color: var(--info-color);
    }
}

.join-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.input-wrapper {
    input {
        width: 100%;
        height: 55px;
        font-size: 1.2em;
        text-align: center;
        border: 2px solid var(--secondary-color);
        border-radius: 5px;
        background-color: var(--main-color);
        color: var(--text-color);
        font-family: 'Courier New', Courier, monospace;
        transition: border-color 0.3s ease;
        
        &:focus {
            border-color: var(--info-color);
            outline: none;
        }
    }
    
    &.has-error input {
        border-color: var(--error-color);
    }
}

.error-text {
    color: var(--error-color) !important;
    font-size: 0.9em !important;
    margin: -0.5rem 0 0 !important;
}

.join-btn {
    width: 100%;
    justify-content: center;
}

.divider {
    margin: 1.5rem 0;
    display: flex;
    align-items: center;
    color: var(--secondary-color);
    font-size: 0.9em;
    
    &::before, &::after {
        content: "";
        flex: 1;
        border-bottom: 1px solid var(--secondary-color);
        opacity: 0.3;
    }
    &::before { margin-right: 10px; }
    &::after { margin-left: 10px; }
}
</style>
