import { reactive, computed } from 'vue';
import apiClient from '@/axios';
import store from '@/store';

// Кэш для разрешений пользователя в контексте узла
// Ключ: 'node-12-user-bob'
const permissionCache = reactive({});

/**
 * Получает и кэширует разрешения *текущего* пользователя для указанного узла.
 * @param {number} treeNodeId - ID узла дерева
 * @returns {Promise<Set<String>>} - Набор имен разрешений (напр. "CREATE_NODES")
 */
const getPermissions = async (treeNodeId) => {
    const userId = computed(() => store.getters.getCurrentUserId).value;
    if (!userId || !treeNodeId) return new Set();

    const cacheKey = `node-${treeNodeId}-user-${userId}`;
    if (permissionCache[cacheKey]) {
        return permissionCache[cacheKey];
    }

    try {
        // Этот эндпоинт получает права ТЕКУЩЕГО пользователя (из токена) для узла
        const response = await apiClient.get(`/tree-node/${treeNodeId}/members/${userId}/permissions`);
        const permissions = new Set(response.data);
        permissionCache[cacheKey] = permissions;
        return permissions;
    } catch (error) {
        console.error(`Failed to fetch permissions for ${cacheKey}:`, error);
        return new Set(); // Возвращаем пустой набор в случае ошибки
    }
};

/**
 * Проверяет, имеет ли текущий пользователь указанное разрешение в узле.
 * @param {number} treeNodeId - ID узла дерева
 * @param {string} requiredPermission - Имя разрешения (напр. "EDIT_NODES")
 * @returns {Promise<boolean>}
 */
export const hasPermission = async (treeNodeId, requiredPermission) => {
    if (!treeNodeId || !requiredPermission) return false;
    const userPermissions = await getPermissions(treeNodeId);
    return userPermissions.has(requiredPermission);
};

/**
 * Получает глобальные разрешения пользователя из store (JWT).
 * @returns {Set<String>}
 */
export const getGlobalPermissions = () => {
    const perms = computed(() => store.getters.getUserPermissions).value;
    return new Set(perms || []);
};

/**
 * Проверяет, имеет ли пользователь глобальное разрешение.
 * @param {string} permission - Имя разрешения
 * @returns {boolean}
 */
export const hasGlobalPermission = (permission) => {
    return getGlobalPermissions().has(permission);
};

/**
 * Очищает кэш разрешений.
 * @param {number} [treeNodeId] - (Опционально) Очистить кэш только для этого узла.
 */
export const clearPermissionCache = (treeNodeId = null) => {
    const userId = computed(() => store.getters.getCurrentUserId).value;
    if (treeNodeId && userId) {
        const cacheKey = `node-${treeNodeId}-user-${userId}`;
        delete permissionCache[cacheKey];
    } else {
        Object.keys(permissionCache).forEach(key => delete permissionCache[key]);
    }
};
