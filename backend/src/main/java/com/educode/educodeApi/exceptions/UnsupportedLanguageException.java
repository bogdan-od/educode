package com.educode.educodeApi.exceptions;

import java.util.Map;

public class UnsupportedLanguageException extends ContainerException {
    public UnsupportedLanguageException(String lang) {
        super("LANG_NOT_SUPPORTED",
                "Unsupported language: " + lang,
                "Unsupported language: {}", new Object[]{lang},
                Map.of("language", lang));
    }
}
