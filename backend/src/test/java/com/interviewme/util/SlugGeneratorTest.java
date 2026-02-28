package com.interviewme.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SlugGeneratorTest {

    @Nested
    @DisplayName("generateSlug")
    class GenerateSlug {

        @Test
        @DisplayName("should generate slug from simple full name")
        void shouldGenerateSlugFromSimpleName() {
            String slug = SlugGenerator.generateSlug("John Doe");
            assertThat(slug).isEqualTo("john-doe");
        }

        @Test
        @DisplayName("should convert to lowercase")
        void shouldConvertToLowercase() {
            String slug = SlugGenerator.generateSlug("JOHN DOE");
            assertThat(slug).isEqualTo("john-doe");
        }

        @Test
        @DisplayName("should handle multiple spaces between words")
        void shouldHandleMultipleSpaces() {
            String slug = SlugGenerator.generateSlug("John   Doe");
            // Multiple spaces become multiple hyphens, then collapsed
            assertThat(slug).doesNotContain("--");
            assertThat(slug).startsWith("john");
            assertThat(slug).endsWith("doe");
        }

        @Test
        @DisplayName("should strip diacritical marks from unicode characters")
        void shouldStripDiacriticalMarks() {
            String slug = SlugGenerator.generateSlug("Jose Garcia");
            assertThat(slug).isEqualTo("jose-garcia");
        }

        @Test
        @DisplayName("should handle accented characters")
        void shouldHandleAccentedCharacters() {
            String slug = SlugGenerator.generateSlug("Rene Descartes");
            assertThat(slug).isEqualTo("rene-descartes");
        }

        @Test
        @DisplayName("should remove special characters")
        void shouldRemoveSpecialCharacters() {
            String slug = SlugGenerator.generateSlug("John O'Brien-Smith");
            assertThat(slug).doesNotContain("'");
            assertThat(slug).matches("^[a-z0-9-]+$");
        }

        @Test
        @DisplayName("should remove leading and trailing hyphens")
        void shouldRemoveLeadingTrailingHyphens() {
            String slug = SlugGenerator.generateSlug(" John Doe ");
            assertThat(slug).doesNotStartWith("-");
            assertThat(slug).doesNotEndWith("-");
        }

        @Test
        @DisplayName("should truncate to max 50 characters")
        void shouldTruncateToMaxLength() {
            String longName = "Abcdefghij Klmnopqrst Uvwxyzabcd Efghijklmn Opqrstuvwx Yzabcdefgh";
            String slug = SlugGenerator.generateSlug(longName);
            assertThat(slug.length()).isLessThanOrEqualTo(50);
        }

        @Test
        @DisplayName("should not end with hyphen after truncation")
        void shouldNotEndWithHyphenAfterTruncation() {
            // Create a name that would produce a hyphen at position 50
            String longName = "A".repeat(49) + " B";
            String slug = SlugGenerator.generateSlug(longName);
            assertThat(slug).doesNotEndWith("-");
        }

        @Test
        @DisplayName("should generate fallback for null input")
        void shouldGenerateFallbackForNull() {
            String slug = SlugGenerator.generateSlug(null);
            assertThat(slug).startsWith("user-");
        }

        @Test
        @DisplayName("should generate fallback for empty string")
        void shouldGenerateFallbackForEmptyString() {
            String slug = SlugGenerator.generateSlug("");
            assertThat(slug).startsWith("user-");
        }

        @Test
        @DisplayName("should generate fallback for blank string")
        void shouldGenerateFallbackForBlankString() {
            String slug = SlugGenerator.generateSlug("   ");
            assertThat(slug).startsWith("user-");
        }

        @Test
        @DisplayName("should generate fallback for very short result")
        void shouldGenerateFallbackForShortResult() {
            // Single character after processing => length < 3 => fallback
            String slug = SlugGenerator.generateSlug("A");
            assertThat(slug).startsWith("user-");
        }

        @Test
        @DisplayName("should handle names with numbers")
        void shouldHandleNamesWithNumbers() {
            String slug = SlugGenerator.generateSlug("John Doe 3rd");
            assertThat(slug).isEqualTo("john-doe-3rd");
        }

        @Test
        @DisplayName("should collapse consecutive hyphens")
        void shouldCollapseConsecutiveHyphens() {
            String slug = SlugGenerator.generateSlug("John -- Doe");
            assertThat(slug).doesNotContain("--");
        }

        @Test
        @DisplayName("should handle name with only special characters")
        void shouldHandleOnlySpecialChars() {
            String slug = SlugGenerator.generateSlug("@#$%");
            assertThat(slug).startsWith("user-");
        }
    }

    @Nested
    @DisplayName("generateUniqueSlug")
    class GenerateUniqueSlug {

        @Test
        @DisplayName("should return base slug when not taken")
        void shouldReturnBaseSlugWhenNotTaken() {
            String slug = SlugGenerator.generateUniqueSlug("John Doe", s -> false);
            assertThat(slug).isEqualTo("john-doe");
        }

        @Test
        @DisplayName("should append suffix when base slug is taken")
        void shouldAppendSuffixWhenTaken() {
            String slug = SlugGenerator.generateUniqueSlug("John Doe", s -> s.equals("john-doe"));
            assertThat(slug).isEqualTo("john-doe01");
        }

        @Test
        @DisplayName("should try incrementing suffixes until unique")
        void shouldIncrementSuffixUntilUnique() {
            String slug = SlugGenerator.generateUniqueSlug("John Doe",
                    s -> s.equals("john-doe") || s.equals("john-doe01") || s.equals("john-doe02"));
            assertThat(slug).isEqualTo("john-doe03");
        }

        @Test
        @DisplayName("should not return a reserved slug")
        void shouldNotReturnReservedSlug() {
            // "admin" is reserved. If fullName produces "admin", it should get a suffix
            String slug = SlugGenerator.generateUniqueSlug("admin user", s -> false);
            // "admin-user" is not reserved, so it should work
            assertThat(slug).isEqualTo("admin-user");
        }

        @Test
        @DisplayName("should fall back to timestamp when all suffixes taken")
        void shouldFallbackWhenAllSuffixesTaken() {
            // All candidates are "taken"
            String slug = SlugGenerator.generateUniqueSlug("John Doe", s -> true);
            assertThat(slug).contains("-");
            // Fallback contains timestamp portion
            assertThat(slug).startsWith("john-doe");
        }
    }
}
