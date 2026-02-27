package com.interviewme.util;

import java.text.Normalizer;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class SlugGenerator {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9-]");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-|-$");
    private static final int MAX_LENGTH = 50;

    private SlugGenerator() {}

    public static String generateSlug(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "user-" + System.currentTimeMillis() % 100000;
        }

        String normalized = Normalizer.normalize(fullName, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        String slug = normalized
                .toLowerCase()
                .trim()
                .replace(' ', '-');

        slug = NON_ALPHANUMERIC.matcher(slug).replaceAll("");
        slug = CONSECUTIVE_HYPHENS.matcher(slug).replaceAll("-");
        slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");

        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH);
            slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");
        }

        if (slug.length() < 3) {
            slug = "user-" + System.currentTimeMillis() % 100000;
        }

        return slug;
    }

    public static String generateUniqueSlug(String fullName, Function<String, Boolean> existsChecker) {
        String baseSlug = generateSlug(fullName);

        if (!existsChecker.apply(baseSlug) && !SlugValidator.isReservedSlug(baseSlug)) {
            return baseSlug;
        }

        for (int i = 1; i <= 100; i++) {
            String candidate = baseSlug + "-" + i;
            if (candidate.length() <= MAX_LENGTH && !existsChecker.apply(candidate) && !SlugValidator.isReservedSlug(candidate)) {
                return candidate;
            }
        }

        String fallback = baseSlug.substring(0, Math.min(baseSlug.length(), 35)) + "-" + System.currentTimeMillis() % 100000;
        return fallback;
    }
}
