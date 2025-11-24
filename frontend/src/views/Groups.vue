<template>
    <h1>Групи</h1>
    <hr>
    <div class="preloader" v-if="isLoading">
        <Preloader/>
    </div>
    <p class="error" v-if="error && error.length > 0">{{ error }}</p>
    <div class="groups">
        <div class="group" v-for="(group, i) in groups" :key="i">
            <div>
                <h4 class="name">{{ group.title }}</h4>
                <p>{{ group.description }}</p>
            </div>
            <hr>
            <div class="link-block">
                <LinkBtn :to="`/group/${group.id}`" anim="go" click="load">Переглянути</LinkBtn>
            </div>
        </div>
    </div>
    <hr>
    <div v-if="groups && groups.length > 0" class="pannel">
        <div class="pagination">
            <button
                class="pagination-btn"
                :disabled="page <= 1"
                @click="router.push(`/groups/${(page-=1)}`)"
            >
                <i class="fa fa-chevron-left"></i>
            </button>
            <span class="pagination-info">Сторінка {{ page }}</span>
            <button
                class="pagination-btn"
                :disabled="!hasNextPage"
                @click="router.push(`/groups/${(page=Number(page) + 1)}`)"
            >
                <i class="fa fa-chevron-right"></i>
            </button>
            <div class="items-per-page">
                <label for="pageLimitInput">Кількість на сторінці: </label>
                <input
                    type="number"
                    v-model="pageLimit"
                    min="1"
                    max="100"
                    id="pageLimitInput"
                    @change="loadGroups"
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
import LinkBtn from '@/components/LinkBtn.vue';

const route = useRoute();
const router = useRouter();

const groups = ref([]);
const page = computed(() => route.params.page || 1)
const pageLimit = ref(20);
const hasNextPage = ref(false);
const error = ref("")
const isLoading = ref(true);

const loadGroups = async () => {
    isLoading.value = true;
    error.value = "";
    apiClient.get("/group/get", {
        params: {
            page: page.value,
            limit: pageLimit.value,
        }
    }).then(response => {
        if (response.data) {
            groups.value = response.data['groups'].content;
            hasNextPage.value = response.data['hasNextPage'];
            isLoading.value = false;

            if (groups.value.length == 0 && page.value != 1) {
                router.push("/groups");
            }
        }
    }).catch(err => {
        console.error(err);
        error.value = "На жаль, ми не змогли завантажити групи - сталася помилка";
        isLoading.value = false;
    });
}

watch(page, async () => {
    await loadGroups();
}, { immediate: true })

onMounted(async () => {
    loadGroups();
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

            &::-webkit-outer-spin-button,
            &::-webkit-inner-spin-button {
                -webkit-appearance: none;
                margin: 0;
            }

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

.groups {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;

    .group {
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

        .link-block {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            flex-wrap: wrap;
            height: 50px;
        }
    }
}
</style>
