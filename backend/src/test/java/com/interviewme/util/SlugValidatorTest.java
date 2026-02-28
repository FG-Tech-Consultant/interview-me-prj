package com.interviewme.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class SlugValidatorTest {

    @Nested
    @DisplayName("isValidSlug")
    class IsValidSlug {

        @Test
        @DisplayName("should accept valid slug")
        void shouldAcceptValidSlug() {
            assertThat(SlugValidator.isValidSlug("john-doe")).isTrue();
        }

        @Test
        @DisplayName("should accept slug with numbers")
        void shouldAcceptSlugWithNumbers() {
            assertThat(SlugValidator.isValidSlug("john-doe-123")).isTrue();
        }

        @Test
        @DisplayName("should accept minimum length slug (3 chars)")
        void shouldAcceptMinLengthSlug() {
            assertThat(SlugValidator.isValidSlug("abc")).isTrue();
        }

        @Test
        @DisplayName("should accept maximum length slug (50 chars)")
        void shouldAcceptMaxLengthSlug() {
            String slug = "a" + "b".repeat(48) + "c";
            assertThat(slug.length()).isEqualTo(50);
            assertThat(SlugValidator.isValidSlug(slug)).isTrue();
        }

        @Test
        @DisplayName("should reject null slug")
        void shouldRejectNull() {
            assertThat(SlugValidator.isValidSlug(null)).isFalse();
        }

        @Test
        @DisplayName("should reject slug shorter than 3 characters")
        void shouldRejectTooShort() {
            assertThat(SlugValidator.isValidSlug("ab")).isFalse();
        }

        @Test
        @DisplayName("should reject slug longer than 50 characters")
        void shouldRejectTooLong() {
            String slug = "a".repeat(51);
            assertThat(SlugValidator.isValidSlug(slug)).isFalse();
        }

        @Test
        @DisplayName("should reject slug with consecutive hyphens")
        void shouldRejectConsecutiveHyphens() {
            assertThat(SlugValidator.isValidSlug("john--doe")).isFalse();
        }

        @Test
        @DisplayName("should reject slug starting with hyphen")
        void shouldRejectStartingHyphen() {
            assertThat(SlugValidator.isValidSlug("-john-doe")).isFalse();
        }

        @Test
        @DisplayName("should reject slug ending with hyphen")
        void shouldRejectEndingHyphen() {
            assertThat(SlugValidator.isValidSlug("john-doe-")).isFalse();
        }

        @Test
        @DisplayName("should reject slug with uppercase letters")
        void shouldRejectUppercase() {
            assertThat(SlugValidator.isValidSlug("John-Doe")).isFalse();
        }

        @Test
        @DisplayName("should reject slug with special characters")
        void shouldRejectSpecialChars() {
            assertThat(SlugValidator.isValidSlug("john_doe")).isFalse();
            assertThat(SlugValidator.isValidSlug("john.doe")).isFalse();
            assertThat(SlugValidator.isValidSlug("john@doe")).isFalse();
        }

        @Test
        @DisplayName("should reject empty string")
        void shouldRejectEmpty() {
            assertThat(SlugValidator.isValidSlug("")).isFalse();
        }

        @Test
        @DisplayName("should accept slug that is only numbers")
        void shouldAcceptOnlyNumbers() {
            assertThat(SlugValidator.isValidSlug("123")).isTrue();
        }
    }

    @Nested
    @DisplayName("isReservedSlug")
    class IsReservedSlug {

        @ParameterizedTest
        @ValueSource(strings = {"admin", "api", "login", "register", "dashboard", "profile",
                "billing", "settings", "help", "about", "p", "public", "skills",
                "chat", "export", "linkedin"})
        @DisplayName("should identify reserved slugs")
        void shouldIdentifyReservedSlugs(String slug) {
            assertThat(SlugValidator.isReservedSlug(slug)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "Api", "LOGIN"})
        @DisplayName("should be case-insensitive for reserved slugs")
        void shouldBeCaseInsensitive(String slug) {
            assertThat(SlugValidator.isReservedSlug(slug)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-reserved slug")
        void shouldReturnFalseForNonReserved() {
            assertThat(SlugValidator.isReservedSlug("john-doe")).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            assertThat(SlugValidator.isReservedSlug(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizeSlug")
    class NormalizeSlug {

        @Test
        @DisplayName("should convert to lowercase")
        void shouldConvertToLowercase() {
            assertThat(SlugValidator.normalizeSlug("JOHN-DOE")).isEqualTo("john-doe");
        }

        @Test
        @DisplayName("should trim whitespace")
        void shouldTrimWhitespace() {
            assertThat(SlugValidator.normalizeSlug("  john-doe  ")).isEqualTo("john-doe");
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNull() {
            assertThat(SlugValidator.normalizeSlug(null)).isNull();
        }
    }
}
