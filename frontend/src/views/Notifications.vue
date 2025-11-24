<template>
    <h1>Повідомлення</h1>
    <hr>
    <div class="preloader" v-if="isLoading">
        <Preloader/>
    </div>
    <p class="error" v-if="error && error.length > 0">{{ error }}</p>
    <div class="notifications">
        <div class="notification" v-for="(notification, i) in notifications" :key="i">
            <div>
                <h4 class="title">{{ notification.title }}</h4>
                <pre class="content">{{ notification.content }}</pre>
            </div>
            <div class="info-block">
                <span class="date">{{ notification.date }}</span>
                <i v-if="notification.level == 'INFO'" class="fa-solid fa-circle-info info"></i>
                <i v-if="notification.level == 'WARN'" class="fa-solid fa-triangle-exclamation warn"></i>
                <i v-if="notification.level == 'CRITICAL'" class="fa-solid fa-circle-exclamation critical"></i>
            </div>
        </div>
    </div>
    <hr>
    <div v-if="notifications && notifications.length > 0" class="pannel">
        <!-- Блок пагінації -->
        <div class="pagination">
            <button 
                class="pagination-btn" 
                :disabled="page <= 1" 
                @click="router.push(`/notifications/${(page-=1)}`)"
            >
                <i class="fa fa-chevron-left"></i>
            </button>
            <span class="pagination-info">Сторінка {{ page }}</span>
            <button 
                class="pagination-btn" 
                :disabled="!hasNextPage" 
                @click="router.push(`/notifications/${(page=Number(page) + 1)}`)"
            >
                <i class="fa fa-chevron-right"></i>
            </button>
            <!-- Вибір кількості елементів на сторінці -->
            <div class="items-per-page">
                <label for="pageLimitInput">Кількість на сторінці: </label>
                <input 
                    type="number" 
                    v-model="pageLimit" 
                    min="1" 
                    max="100" 
                    id="pageLimitInput"
                    @change="loadNotifications"
                >
            </div> 
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import apiClient from '@/axios'
import { useRoute, useRouter } from 'vue-router';
import Preloader from '@/components/Preloader.vue';

const route = useRoute();
const router = useRouter();

const notifications = ref([]);
const page = computed(() => route.params.page || 1)
const pageLimit = ref(20);
const hasNextPage = ref(false);
const error = ref("")
const isLoading = ref(true);

const loadNotifications = async () => {
    apiClient.get("/notifications/get", {
        params: {
            page: page.value,
            limit: pageLimit.value,
        }
    }).then(response => {
        if (response.data) {
            notifications.value = response.data['notifications'];
            hasNextPage.value = response.data['hasNextPage'];

            notifications.value.forEach(notif => {
                notif.date = new Date(notif.date).toLocaleString('uk-UA', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: false
                });
            });

            isLoading.value = false;

            if (notifications.value.length == 0 && page.value != 1) {
                router.push("/notifications");
            }
        }
    }).catch(err => {
        console.error(err);
        error.value = "Нажаль, ми не змогли загрузити повідомлення - сталася помилка";        
    });
}

watch(page, async (newVal) => {
    await loadNotifications();
}, { immediate: true })

onMounted(() => {
    loadNotifications();
});
</script>

<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';

.preloader {
    width: 100%;
    height: 300px;
    position: relative;
}

// Стилі для блоку пагінації
.pagination {
    @include display-flex($align-items: center);
    gap: 10px;

    // Стилі для кнопок пагінації
    button.pagination-btn {
        @include display-flex($justify-content: center);
        display: inline-flex;
        width: 30px;
        height: 30px;
        border-radius: 3px;
        border: none;
        background-color: var(--weak-color);
        box-shadow: var(--weak-box-shadow);
        cursor: pointer;
        transition: background-color 0.2s ease-in-out;

        &:not(:disabled):hover {
            background-color: var(--weak-color-hover);
        }

        &:disabled {
            cursor: not-allowed;
        }
    }

    // Стилі для блоку вибору кількості елементів
    .items-per-page {
        @include display-flex($align-items: center);
        gap: 10px;

        // Стилі для поля введення
        input {
            width: 50px;
            height: 30px;
            border-radius: 3px;
            border: 1px solid var(--secondary-color);
            background-color: var(--weak-color);
            padding: 5px;
            transition: background-color 0.2s ease-in-out;

            // Видалення кнопок спінера для Chrome, Safari, Edge, Opera
            &::-webkit-outer-spin-button,
            &::-webkit-inner-spin-button {
                -webkit-appearance: none;
                margin: 0;
            }
            
            // Видалення кнопок спінера для Firefox
            &[type="number"] {
                -moz-appearance: textfield;
                appearance: textfield;
            }
        }
    }
    
    // Відступи між елементами пагінації
    *:not(:last-child) {
        margin-right: 10px;
    }
}

.notifications {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;

    .notification {
        position: relative;
        padding: 10px;
        background-color: var(--weak-color);
        border-radius: 3px;
        display: flex;
        justify-content: space-between;
        flex-direction: column;

        hr {
            width: calc(100% + 20px);
            margin-left: -10px;
        }

        .title {
            font-size: 1.1em;
        }

        .content {
            margin: 8px 0;
        }

        * {
            text-wrap: wrap;
        }

        .info-block {
            display: flex;
            flex-wrap: wrap;
            justify-content: space-between;

            > i {
                font-size: 25px;

                &.info {
                    color: var(--info-color);
                }

                &.warn {
                    color: var(--warn-color)
                }

                &.critical {
                    color: var(--error-color)
                }
            }
        }
    }
}
</style>
