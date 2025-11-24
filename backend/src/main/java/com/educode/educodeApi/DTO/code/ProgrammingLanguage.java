package com.educode.educodeApi.DTO.code;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProgrammingLanguage(
        String id,
        @JsonProperty("server_id")
        String serverId,
        String name,
        String version,
        @JsonProperty("monaco_editor_language")
        String monacoEditorLanguage
) {
}
