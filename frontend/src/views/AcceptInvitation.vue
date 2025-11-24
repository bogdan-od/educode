<template>
    <div class="accept-invite-page">
        <div v-if="loading" class="card"><Preloader /></div>
        <div v-else-if="error" class="card error-card">
            <h2>Помилка</h2>
            <p>{{ error }}</p>
            <LinkBtn to="/" anim="go">На головну</LinkBtn>
        </div>
        <div v-else-if="details" class="card">
            <h2>Прийняти запрошення?</h2>
            <p>Вас запрошують приєднатися до:</p>
            <h3 class="entity-title">
                <i :class="details.entityType === 'node' ? 'fa-solid fa-folder' : 'fa-solid fa-users'"></i>
                {{ details.title }}
            </h3>
            <p>Вам будуть надані наступні ролі:</p>
            <ul class="roles-list">
                <li v-for="role in details.roles" :key="role.name">{{ role.description }}</li>
            </ul>
            <LinkBtn @click="accept" :disabled="submitting" anim="go" img="wave1.svg">
                <Preloader v-if="submitting" :scale="0.5" color="white" />
                <span v-else>Прийняти</span>
            </LinkBtn>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import { handleApiError } from '@/services/errorHandler';
import { useStore } from 'vuex';

const route = useRoute();
const router = useRouter();
const store = useStore();

const code = route.params.code;
const loading = ref(true);
const submitting = ref(false);
const error = ref('');
const details = ref(null);

const fetchInviteDetails = async () => {
    loading.value = true;
    error.value = '';
    try {
        // Fetch invitation metadata including roles and tree node reference
        const response = await apiClient.get(`/invitations/view/${code}`);
        const data = response.data;
        
        // Determine if the invitation refers to a Node or Group and fetch corresponding entity details
        let title = `Вузол #${data.treeNodeId}`;
        let entityType = 'node';
        let entityId = null;

        try {
            const nodeRes = await apiClient.get(`/node/by-tree-node/${data.treeNodeId}`);
            title = `(Вузол) ${nodeRes.data.title}`;
            entityType = 'node';
            entityId = nodeRes.data.id;
        } catch(e) {
            try {
                const groupRes = await apiClient.get(`/group/by-tree-node/${data.treeNodeId}`);
                title = `(Група) ${groupRes.data.title}`;
                entityType = 'group';
                entityId = groupRes.data.id;
            } catch (e2) {
                 error.value = "Не вдалося визначити вузол, до якого вас запрошують.";
                 return;
            }
        }

        details.value = {
            ...data,
            title: title,
            entityType: entityType,
            entityId: entityId
        };

    } catch (err) {
        handleApiError(err);
        error.value = err.response?.data?.error || "Запрошення не знайдено або не дійсне.";
    } finally {
        loading.value = false;
    }
};

const accept = async () => {
    submitting.value = true;
    try {
        const response = await apiClient.post(`/invitations/accept/${code}`);
        store.dispatch('addSuccessMessage', response.data.message);
        
        // Redirect to the appropriate entity (node or group) after successful acceptance
        if (details.value.entityType && details.value.entityId) {
             router.push(`/${details.value.entityType}/${details.value.entityId}`);
        } else {
            router.push('/my-nodes-groups');
        }

    } catch (err) {
        handleApiError(err);
        error.value = err.response?.data?.error || "Не вдалося прийняти запрошення.";
    } finally {
        submitting.value = false;
    }
};

onMounted(fetchInviteDetails);
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.accept-invite-page {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding-top: 5vh;
    min-height: 50vh;
}
.card {
    width: 100%;
    max-width: 500px;
    padding: 2rem;
    background: var(--weak-color);
    border-radius: 5px;
    box-shadow: var(--strong-box-shadow);
    text-align: center;
    border: 1px solid var(--secondary-color);
    
    h2 {
        font-family: $secondary-font;
        color: var(--text-color);
    }
    p {
        font-family: $form-font;
        color: var(--secondary-color);
    }
}

.entity-title {
    font-family: $secondary-font;
    font-size: 1.5em;
    color: var(--text-color);
    margin: 1rem 0;
    i {
        margin-right: 10px;
        color: var(--info-color);
    }
}

.roles-list {
    list-style: none;
    padding: 0;
    margin: 1rem 0;
    li {
        background: var(--main-color);
        padding: 0.75rem;
        border-radius: 3px;
        margin-bottom: 0.5rem;
        font-family: $form-font;
        color: var(--text-color);
        border: 1px solid var(--secondary-color);
    }
}

.error-card {
    background-color: var(--weak-color);
    border-color: var(--error-color);
    h2, p {
        color: var(--error-color);
    }
}
</style>
