<template>
    <div class="node-browser">
        <div class="browser-panel">
            <div class="header">
                <button @click="goBack" :disabled="history.length === 0" class="nav-btn">
                    <i class="fa-solid fa-arrow-left"></i> Назад
                </button>
                <div class="breadcrumbs">
                    <span @click="goTo(null)" class="crumb root">Корінь</span>
                    <span v-for="item in history" :key="item.id" @click="goTo(item.id)" class="crumb">
                        / {{ item.title }}
                    </span>
                </div>
                 <LinkBtn v-if="canCreateNode" :to="`/create/node?parentId=${currentNodeId}`" anim="bg-scale" img="wave1.svg" class="header-action-btn">
                    <i class="fa-solid fa-plus"></i> Створити вузол
                </LinkBtn>
                 <LinkBtn v-if="canCreateGroup" :to="`/create/group?parentId=${currentNodeId}`" anim="bg-scale" img="wave2.svg" class="header-action-btn">
                    <i class="fa-solid fa-plus"></i> Створити групу
                </LinkBtn>
            </div>

            <div v-if="isLoading" class="preloader-container"><Preloader /></div>
            <div v-else class="content">
                <div v-if="error" class="error-message"><i class="fa-solid fa-exclamation-triangle"></i> {{ error }}</div>
                <div v-if="currentItems.length === 0 && !isLoading && !error" class="empty-message">
                    Тут порожньо.
                </div>
                <div v-for="item in currentItems" :key="item.type + '-' + item.id" class="item-card">
                    <div class="item-icon">
                        <i :class="item.type === 'node' ? 'fa-solid fa-folder' : 'fa-solid fa-users'"></i>
                    </div>
                    <div class="item-info">
                        <h3>{{ item.title }}</h3>
                        <p>{{ item.description }}</p>
                    </div>
                    <div class="item-actions">
                         <LinkBtn :to="`/tree-node/${item.treeNodeId}/members`" anim="go" title="Керувати користувачами">
                            <i class="fa-solid fa-user-gear"></i>
                        </LinkBtn>
                         <LinkBtn v-if="item.type === 'node'" :to="{ name: 'NodeBrowser', params: { id: item.id } }" anim="go" title="Перейти">
                            <i class="fa-solid fa-arrow-right"></i>
                        </LinkBtn>
                         <LinkBtn v-else :to="`/group/${item.id}`" anim="go" title="Переглянути">
                             <i class="fa-solid fa-eye"></i>
                        </LinkBtn>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import apiClient from '@/axios';
import Preloader from '@/components/Preloader.vue';
import LinkBtn from '@/components/LinkBtn.vue';
import { hasPermission, hasGlobalPermission } from '@/services/permissionService.js';
import { handleApiError } from '@/services/errorHandler.js';

const route = useRoute();
const router = useRouter();

const isLoading = ref(true);
const currentItems = ref([]);
const history = ref([]); // { id (nodeId), title, treeNodeId }
const error = ref('');

const canCreateNode = ref(false);
const canCreateGroup = ref(false);

const currentNodeId = computed(() => history.value.length > 0 ? history.value[history.value.length - 1].id : null);
const currentTreeNodeId = computed(() => history.value.length > 0 ? history.value[history.value.length - 1].treeNodeId : null);

const checkPermissions = async () => {
    if (currentTreeNodeId.value) {
        canCreateNode.value = await hasPermission(currentTreeNodeId.value, 'CREATE_NODES');
        canCreateGroup.value = await hasPermission(currentTreeNodeId.value, 'CREATE_GROUPS');
    } else {
        // We are at root, check global permissions
        canCreateNode.value = hasGlobalPermission('CREATE_NODES');
        canCreateGroup.value = hasGlobalPermission('CREATE_GROUPS');
    }
};

const fetchChildren = async (node) => {
    isLoading.value = true;
    error.value = '';
    currentItems.value = [];
    
    // history.push is handled by loadDataForRoute
    
    try {
        const response = await apiClient.get(`/tree-node/${node.treeNodeId}/children`);
        const { childNodes = [], childGroups = [] } = response.data;
        // The DTOs from /children are NodeViewDTO and GroupViewDTO
        const items = [
            ...childNodes.map(n => ({ ...n, type: 'node' })), // DTO already has id, treeNodeId
            ...childGroups.map(g => ({ ...g, type: 'group' })) // DTO already has id, treeNodeId
        ];
        items.sort((a, b) => a.title.localeCompare(b.title));
        currentItems.value = items;
    } catch (err) {
        handleApiError(err);
        error.value = 'Помилка завантаження';
    } finally {
        isLoading.value = false;
        await checkPermissions();
    }
};

const fetchRootItems = async () => {
    isLoading.value = true;
    error.value = '';
    currentItems.value = [];
    history.value = [];
    try {
        const res = await apiClient.get('/node/roots');
        currentItems.value = res.data.nodes.map(n => ({ ...n, type: 'node' }));
        currentItems.value.sort((a, b) => a.title.localeCompare(b.title));
    } catch(err) {
        handleApiError(err);
        error.value = 'Помилка завантаження';
    } finally {
        isLoading.value = false;
        await checkPermissions();
    }
};

const goBack = () => {
    const parentId = history.value.length > 1 ? history.value[history.value.length - 2].id : undefined;
    router.push({ name: 'NodeBrowser', params: { id: parentId } });
};

const goTo = (nodeId) => {
    router.push({ name: 'NodeBrowser', params: { id: nodeId || undefined } });
};

const loadDataForRoute = async (nodeId) => {
    if (nodeId) {
        try {
            // Загружаем сам узел, чтобы получить его treeNodeId
            const res = await apiClient.get(`/node/${nodeId}`);
            const nodeData = res.data; // This is NodeViewDTO: { id, title, description, parentId, treeNodeId }
            
            // Загружаем предков для breadcrumbs
            const pathRes = await apiClient.get(`/tree-node/${nodeData.treeNodeId}/path`);
            // path returns: { nodeId, treeNodeId, title }
            history.value = pathRes.data.map(tn => ({
                id: tn.nodeId, // Use nodeId for navigation
                title: tn.title,
                treeNodeId: tn.treeNodeId
            }));
            
            await fetchChildren(nodeData);

        } catch (error) {
            handleApiError(error);
            error.value = 'Не вдалося завантажити вузол. Можливо, його не існує, або у вас немає доступу.';
            history.value = [];
            currentItems.value = [];
            isLoading.value = false;
        }
    } else {
        await fetchRootItems();
    }
};

watch(() => route.params.id, (newId) => {
    loadDataForRoute(newId);
}, { immediate: true });
</script>

<style lang="scss" scoped>
@import "@/assets/scss/variables.scss";

.node-browser { 
    width: 100%; 
}

.browser-panel { 
    background-color: var(--main-color); 
    border-radius: 5px; 
    border: 1px solid var(--secondary-color);
    box-shadow: var(--weak-box-shadow); 
    overflow: hidden; 
}

.header { 
    display: flex; 
    align-items: center; 
    padding: 10px 15px; 
    border-bottom: 1px solid var(--secondary-color); 
    gap: 15px; 
    flex-wrap: wrap; 
    background-color: var(--weak-color);

    .header-action-btn { 
        margin-left: auto; 
    } 
}

.nav-btn { 
    padding: 8px 12px; 
    background-color: var(--main-color); 
    border: 1px solid var(--secondary-color); 
    border-radius: 3px; 
    cursor: pointer; 
    font-family: $form-font;
    font-weight: 600;
    color: var(--text-color);
    transition: background-color 0.2s ease;

    &:hover {
        background-color: var(--weak-color-hover);
    }
    
    &:disabled { 
        opacity: 0.5; 
        cursor: not-allowed; 
        background-color: var(--secondary-color);
    } 
}

.breadcrumbs { 
    font-family: $form-font; 
    display: flex; 
    align-items: center; 
    white-space: nowrap; 
    overflow-x: auto; 
    
    .crumb { 
        cursor: pointer; 
        padding: 5px; 
        color: var(--secondary-color);
        
        &:hover { 
            text-decoration: underline; 
            color: var(--text-color);
        } 
        
        &.root { 
            font-weight: bold; 
            color: var(--text-color);
        } 
    } 
}

.content { 
    padding: 20px; 
    display: flex; 
    flex-direction: column; 
    gap: 15px; 
}

.preloader-container, .empty-message, .error-message { 
    padding: 40px 20px; 
    text-align: center; 
    color: var(--secondary-color); 
    font-family: $form-font;
}

.error-message { 
    color: var(--error-color); 
    font-weight: 600;
    background-color: var(--weak-color);
    border-radius: 5px;
    i { margin-right: 8px; }
}
.empty-message {
    background-color: var(--weak-color);
    border-radius: 5px;
}

.item-card { 
    display: flex; 
    align-items: center; 
    gap: 15px; 
    background-color: var(--weak-color); 
    padding: 15px; 
    border-radius: 5px; 
    border: 1px solid var(--secondary-color);
    transition: border-color 0.2s ease;

    &:hover {
        border-color: var(--info-color);
    }
}

.item-icon { 
    font-size: 1.8em; 
    color: var(--info-color);
    width: 30px;
    text-align: center;
}

.item-info { 
    flex-grow: 1; 
    h3 { 
        margin: 0 0 5px; 
        font-family: $secondary-font;
        color: var(--text-color);
    } 
    p { 
        margin: 0; 
        color: var(--secondary-color); 
        font-size: 0.9em; 
        font-family: $form-font;
    } 
}

.item-actions { 
    display: flex; 
    gap: 10px; 
}
</style>
