import { loadJsonStaticResource } from './resourceService';
import store from '@/store';

export async function loadProgrammingLanguages() {
    await loadJsonStaticResource("programmingLanguages");
}

export function getMonacoEditorLanguage(languageServerId) {
    const programmingLanguages = store.getters.programmingLanguages;
    if (!programmingLanguages) return 'plaintext';
    
    const programmingLanguage = programmingLanguages.find(lang => lang.server_id == languageServerId);
    return programmingLanguage?.monaco_editor_language || 'plaintext';
}

export function getDisplayLanguageText(languageServerId) {
    const programmingLanguages = store.getters.programmingLanguages;
    if (!programmingLanguages) return "Loading...";
    
    const programmingLanguage = programmingLanguages.find(lang => lang.server_id == languageServerId);
    if (!programmingLanguage?.name) return null;
    
    return `${programmingLanguage.name} ${programmingLanguage.version || ''}`.trim();
}

export function getProgrammingLanguage(languageServerId) {
    const programmingLanguages = store.getters.programmingLanguages;
    if (!programmingLanguages) return null;
    
    return programmingLanguages.find(lang => lang.server_id == languageServerId);
}
