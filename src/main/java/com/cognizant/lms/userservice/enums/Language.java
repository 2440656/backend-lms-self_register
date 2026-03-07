package com.cognizant.lms.userservice.enums;

import java.util.Arrays;
import java.util.Optional;

    /**
     * Enum representing supported languages in the application.
     * Each language has a code (ISO 639-1) and display name.
     */
    public enum Language {
        ENGLISH("en", "English"),
        FRENCH("fr", "French"),
        SPANISH("es", "Spanish"),
        GERMAN("de", "German"),
        ITALIAN("it", "Italian"),
        PORTUGUESE("pt", "Portuguese"),
        CHINESE("zh", "Chinese"),
        JAPANESE("ja", "Japanese"),
        KOREAN("ko", "Korean"),
        ARABIC("ar", "Arabic");

        private final String lang_code;
        private final String language;

        Language(String lang_code, String language) {
            this.lang_code = lang_code;
            this.language = language;
        }

        public String getLang_code() {
            return lang_code;
        }

        public String getLanguage() {
            return language;
        }

        /**
         * Find Language enum by code (case-insensitive)
         * @param code the language code (e.g., "en", "fr")
         * @return Optional containing the Language if found, empty otherwise
         */
        public static Optional<com.cognizant.lms.userservice.enums.Language> fromCode(String code) {
            if (code == null || code.isBlank()) {
                return Optional.empty();
            }
            return Arrays.stream(values())
                    .filter(lang -> lang.lang_code.equalsIgnoreCase(code.trim()))
                    .findFirst();
        }
    }


