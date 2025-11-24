<template>
    <div v-if="checker" class="checker">
        <div class="top">
            <span class="id">#{{ checker.id }}</span>
            <h1>{{ checker.name }}</h1>
        </div>
        <div class="info-block">
            <LinkBtn :to="`/edit/checker/${checker.id}`" click="load" anim="bg-scale" img="wave1.svg"><i class="fa-solid fa-file-pen"></i> Редагувати</LinkBtn>
            <div class="flex-between gap-10">
                <p class="language"><i class="fa-solid fa-screwdriver-wrench"></i> {{ checker.language }}</p>
                <p class="size"><i class="fa-solid fa-weight-hanging"></i> {{ checker.size }}</p>
            </div>
        </div>
        <div class="log">
            <div class="panel">
                <div>
                    <div><i class="fa-solid fa-weight-hanging"></i> {{ checkerLog.size }}</div>
                    <div v-if="checkerLog.log && checkerLog.log.length > 0" class="timestamp">Час завантаження логу: {{ checkerLog.timestamp }}</div>
                </div>
                <button class="reload-button" @click="loadLog"><i class="fa-solid fa-arrows-rotate"></i></button>
            </div>
            <div class="content" :class="{'empty': !checkerLog.log || checkerLog.log.length == 0}">
                <Preloader v-if="isLoadingLog"/>
                <pre v-else>{{ checkerLog.log && checkerLog.log.length > 0 ? checkerLog.log : 'Порожній лог' }}</pre>
            </div>
        </div>
        <template v-if="checker.puzzles">
            <p class="puzzles-count">Кількість задач: <strong>{{ checker.puzzles.length }}</strong> <i class="fa-solid fa-puzzle-piece"></i></p>
            <template v-if="checker.puzzles.length > 0">
                <h2>Задачі:</h2>
                <div class="puzzles">
                    <div class="puzzle" v-for="(puzzle, i) in checker.puzzles" :key="i">
                        <div class="puzzle-header">
                            <h2>{{ puzzle.title }}</h2>
                            <div class="puzzle-header-right">
                                <p class="puzzle-header-right-time">Ліміт часу: {{ puzzle.timeLimit }} с</p>
                                <p class="puzzle-header-right-points">{{ puzzle.score }} балів</p>
                            </div>
                        </div>
                        <div class="puzzle-body">
                            <p>{{ puzzle.description }}</p>
                        </div>
                        <div class="puzzle-footer">
                            <LinkBtn img="wave2.svg" click="load" :to="`/puzzle/${puzzle.id}`"><i class="fa fa-eye"></i> Переглянути</LinkBtn>
                        </div>
                    </div>
                </div>
            </template>
        </template>
    </div>
    <Preloader v-else width="100%" height="600px"/>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { loadJsonStaticResource } from '@/services/resourceService.js';
import { useRouter, useRoute } from 'vue-router';
import { useStore } from 'vuex';
import apiClient from '../axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';

const route = useRoute()
const router = useRouter()
const store = useStore()

const checker = ref(null)
const checkerLanguages = computed(() => store.getters.checkerLanguages || [])
const id = computed(() => route.params.id || null)
const checkerLog = ref({log: "", timestamp: null});
const isLoadingLog = ref(true);

// Fetch checker metadata and details from backend
const loadChecker = async () => {
    if (id.value == null) {
        router.push("/checkers");
        return;
    }

    apiClient.get(`/checker/get/${id.value}`).then(response => {
        checker.value = response.data;
        checker.value.size = formatBytes(checker.value.size);
        // Format language display with version information
        const lang = checkerLanguages.value.find(l => l.server_id == checker.value.language);
        checker.value.language = lang ? `${lang.name} ${lang.version}` : checker.value.language;
    }).catch(err => {
        console.error(err)
        router.push("/checkers")
    })
}

// Fetch compilation log for the checker from backend
const loadLog = async () => {
    if (id.value == null) {
        router.push("/checkers");
        return;
    }

    isLoadingLog.value = true;

    apiClient.get(`/checker/log/${id.value}`).then(response => {
        checkerLog.value = response.data;
        checkerLog.value.size = formatBytes(checkerLog.value.size);
        isLoadingLog.value = false;
    }).catch(err => {
        if (err && err.response && err.response.status == 404) {
            isLoadingLog.value = false;
            return;
        }

        console.error(err)
        router.push("/checkers")
    })
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

// Reload checker data when route param changes
watch(id, async (newVal) => {
    await loadChecker();
}, { immediate: true })

onMounted(async () => {
    await loadJsonStaticResource("checkerLanguages");
    loadChecker();
    loadLog();
});
</script>

<style lang="scss" scoped>
.checker {
    display: flex;
    flex-direction: column;
    gap: 20px;

    .top {
        display: flex;
        align-items: flex-start;
        justify-content: start;
        gap: 10px;

        .id {
            color: var(--secondary-color);
        }
    }

    .info-block {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        flex-wrap: wrap;
    }

    .log {
        border: 1px solid var(--secondary-color);
        border-radius: 5px;

        .panel {
            padding: 10px;
            border-bottom: 1px solid var(--secondary-color);
            display: flex;
            justify-content: space-between;

            > div {
                width: 100%;
                display: flex;
                gap: 20px;
            }

            .reload-button {
                cursor: pointer;
                border-radius: 50%;
                background-color: var(--weak-color);
                border: 0;
                display: flex;
                justify-content: center;
                align-items: center;
                width: 30px;
                height: 30px;
                transition: all .15s ease-in-out;

                &:hover {
                    transform: rotate(15deg);
                }

                &:active {
                    transform: rotate(360deg);
                }
            }
        }

        .content {
            overflow: auto;
            height: 350px;
            padding: 10px;

            &.empty {
                height: auto;
                text-align: center;
            }
        }
    }
}

.puzzles {
    display: flex;
    flex-direction: column;
    gap: 20px;
    
    .puzzle {
        width: 100%;
        padding: 15px;
        background-color: var(--weak-color);
        border-radius: 3px;
        box-shadow: var(--weak-box-shadow);

        :deep(a) {
            margin-top: 10px;
        }
    }
}
</style>
