package com.educode.educodeApi.interfaces;

import java.io.IOException;

public interface StaticJsonProvider {
    /**
     * Если этот провайдер может отдать файл с таким именем
     * (например, лежит в classpath или в external-dir),
     * возвращает true.
     */
    boolean supports(String name);

    /**
     * Возвращает тело + ETag.
     * Реализация сама кэширует данные и обновляет при изменении.
     */
    CachedJson get(String name) throws IOException;

    class CachedJson {
        private final String json;
        private final String etag;

        public CachedJson(String json, String etag) {
            this.json = json;
            this.etag = etag;
        }
        public String getJson() { return json; }
        public String getEtag() { return etag; }
    }
}

