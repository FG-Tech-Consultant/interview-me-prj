package com.interviewme.util;

import java.util.Set;
import java.util.regex.Pattern;

public final class SlugValidator {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]*[a-z0-9]$");
    private static final Pattern CONSECUTIVE_HYPHENS = Pattern.compile("--");
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    private static final Set<String> RESERVED_SLUGS = Set.of(
        "admin", "api", "login", "register", "dashboard", "profile",
        "billing", "settings", "help", "about", "p", "public", "skills",
        "chat", "export", "linkedin"
    );

    private SlugValidator() {}

    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.length() < MIN_LENGTH || slug.length() > MAX_LENGTH) {
            return false;
        }
        if (CONSECUTIVE_HYPHENS.matcher(slug).find()) {
            return false;
        }
        return SLUG_PATTERN.matcher(slug).matches();
    }

    public static boolean isReservedSlug(String slug) {
        if (slug == null) {
            return false;
        }
        return RESERVED_SLUGS.contains(slug.toLowerCase());
    }

    public static String normalizeSlug(String slug) {
        if (slug == null) {
            return null;
        }
        return slug.toLowerCase().trim();
    }
}
