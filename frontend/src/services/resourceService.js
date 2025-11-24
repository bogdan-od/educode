import apiClient from '@/axios';
import store from '@/store'

const resourceOneTimeCache = new Set();

export async function loadJsonStaticResource(resource_name) {
    if (resourceOneTimeCache.has(resource_name)) return;

    let lsKey = `resource_${resource_name}:content`;
    let lsEtag = `resource_${resource_name}:etag`;
    let etag = localStorage.getItem(lsEtag)
    try {
        const res = await apiClient.get(`/static/${resource_name}.json`, {
            headers: etag ? { 'If-None-Match': etag } : {}
        })

        if (res.status === 200) {
            localStorage.setItem(lsKey, JSON.stringify(res.data))
            if (res.headers.etag) {
                localStorage.setItem(lsEtag, res.headers.etag)
            }
            store.commit('setStaticResource', {key: resource_name, value: res.data})
            resourceOneTimeCache.add(resource_name);
        }
    } catch (err) {
        if (err.response && err.response.status === 304) {
            const cached = localStorage.getItem(lsKey)
            if (cached) {
                store.commit('setStaticResource', {key: resource_name, value: JSON.parse(cached)})
            }
        } else {
            throw err
        }
    }

    console.log("Returning", resource_name);    
}

const rolesCache = {};

export async function getRoles(roleScope=null) {
    if (rolesCache[roleScope]) {
        return rolesCache[roleScope];
    }

    try {
        const response = await apiClient.get(`/roles`, {
            params: {
                scope: roleScope
            }
        });

        if (Array.isArray(response.data)) {
            response.data.sort((a, b) => (a.rank || 0) - (b.rank || 0));
        }

        rolesCache[roleScope] = response.data;
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch static resource '${resourceName}':`, error);
        return [];
    }
}
