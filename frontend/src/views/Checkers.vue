<template>
    <h1>Перевіряючі програми</h1>
    <hr>
    <div class="preloader" v-if="isLoading">
        <Preloader/>
    </div>
    <p class="error" v-if="error && error.length > 0">{{ error }}</p>
    <div class="checkers">
        <div class="checker" v-for="(checker, i) in checkers" :key="i">
            <div>
                <h4 class="name">{{ checker.name }}</h4>
            </div>
            <div class="info-block">
                <p class="language"><i class="fa-solid fa-screwdriver-wrench"></i> {{ checker.language }}</p>
                <p class="size"><i class="fa-solid fa-weight-hanging"></i> {{ checker.size }}</p>
            </div>
            <hr>
            <div class="link-block">
                <LinkBtn :to="`/checker/${checker.id}`" anim="go" click="load">Переглянути</LinkBtn>
                <p class="puzzles">Кількість задач: <strong>{{ checker.puzzlesCount }}</strong> <i class="fa-solid fa-puzzle-piece"></i></p>
            </div>
        </div>
    </div>
    <hr>
    <div v-if="checkers && checkers.length > 0" class="pannel">
        <!-- Pagination controls with previous/next buttons -->
        <div class="pagination">
            <button 
                class="pagination-btn" 
                :disabled="page <= 1" 
                @click="router.push(`/checkers/${(page-=1)}`)"
            >
                <i class="fa fa-chevron-left"></i>
            </button>
            <span class="pagination-info">Сторінка {{ page }}</span>
            <button 
                class="pagination-btn" 
                :disabled="!hasNextPage" 
                @click="router.push(`/checkers/${(page=Number(page) + 1)}`)"
            >
                <i class="fa fa-chevron-right"></i>
            </button>
            <!-- Items per page selector -->
            <div class="items-per-page">
                <label for="pageLimitInput">Кількість на сторінці: </label>
                <input 
                    type="number" 
                    v-model="pageLimit" 
                    min="1" 
                    max="100" 
                    id="pageLimitInput"
                    @change="loadCheckers"
                >
            </div> 
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import apiClient from '@/axios'
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { loadJsonStaticResource } from '@/services/resourceService.js';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';

const route = useRoute();
const router = useRouter();
const store = useStore();

const checkers = ref([]);
const page = computed(() => route.params.page || 1)
const pageLimit = ref(20);
const hasNextPage = ref(false);
const error = ref("")
const isLoading = ref(true);

const checkerLanguages = computed(() => store.getters.checkerLanguages || [])

// Fetch paginated list of checkers from backend with language formatting
const loadCheckers = async () => {
    apiClient.get("/checker/all", {
        params: {
            page: page.value,
            limit: pageLimit.value,
        }
    }).then(response => {
        if (response.data) {
            checkers.value = response.data['checkers'];
            hasNextPage.value = response.data['hasNextPage'];

            // Format file sizes and language information for display
            checkers.value.forEach(checker => {
                checker.size = formatBytes(checker.size);
                const lang = checkerLanguages.value.find(l => l.server_id == checker.language);
                checker.language = lang ? `${lang.name} ${lang.version}` : checker.language;
            });

            isLoading.value = false;

            // If current page is empty and not first page, redirect to first page
            if (checkers.value.length == 0 && page.value != 1) {
                router.push("/checkers");
            }
        }
    }).catch(err => {
        console.error(err);
        error.value = "Нажаль, ми не змогли загрузити checker'и - сталася помилка";        
    });
}

// Converts bytes to human-readable format (B, KB, MB, GB, etc.)
const formatBytes = (bytes, decimals = 2) => {
  if (bytes === 0) return '0 B';
  if (!isFinite(bytes) || bytes < 0) return String(bytes);

  const k = 1024;
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const value = bytes / Math.pow(k, i);
  const fixed = value.toFixed(Math.max(0, decimals));
  const normalized = parseFloat(fixed).toString();
  return `${normalized} ${units[i]}`;
}

// Reload checkers when pagination page changes
watch(page, async (newVal) => {
    await loadCheckers();
}, { immediate: true })

onMounted(async () => {
    await loadJsonStaticResource("checkerLanguages");
    loadCheckers();
});
</script>

<style lang="scss" scoped>
@import '@/assets/scss/mixins.scss';

.preloader {
    width: 100%;
    height: 300px;
    position: relative;
}

.pagination {
    @include display-flex($align-items: center);
    gap: 10px;

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

    .items-per-page {
        @include display-flex($align-items: center);
        gap: 10px;

        input {
            width: 50px;
            height: 30px;
            border-radius: 3px;
            border: 1px solid var(--secondary-color);
            background-color: var(--weak-color);
            padding: 5px;
            transition: background-color 0.2s ease-in-out;

            // Remove spinner buttons for Chrome, Safari, Edge, Opera
            &::-webkit-outer-spin-button,
            &::-webkit-inner-spin-button {
                -webkit-appearance: none;
                margin: 0;
            }
            
            // Remove spinner buttons for Firefox
            &[type="number"] {
                -moz-appearance: textfield;
                appearance: textfield;
            }
        }
    }
    
    *:not(:last-child) {
        margin-right: 10px;
    }
}

.checkers {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;

    .checker {
        position: relative;
        padding: 10px;
        background-color: var(--weak-color);
        border-radius: 3px;
        display: flex;
        justify-content: space-between;
        flex-direction: column;
        min-width: 300px;

        hr {
            width: calc(100% + 20px);
            margin-left: -10px;
        }

        .name {
            font-size: 1.1em;
            margin-bottom: 10px;
        }

        * {
            text-wrap: wrap;
        }

        .info-block, .link-block {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            flex-wrap: wrap;
        }

        .link-block {
            height: 50px;
        }
    }
}
</style>
