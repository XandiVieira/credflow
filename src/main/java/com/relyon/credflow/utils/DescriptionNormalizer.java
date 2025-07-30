package com.relyon.credflow.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public class DescriptionNormalizer {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{2}/\\d{2}\\b");
    private static final Pattern TRAILING_NUMBERS = Pattern.compile("\\s*\\d{2}/\\d{2}$");

    public static String normalize(String description) {
        if (description == null) return null;
        var normalized = description.toLowerCase(Locale.ROOT).trim();
        normalized = normalized.replaceAll("[^\\p{L}\\p{Nd}\\s]", ""); // remove non-alphanumeric
        normalized = normalized.replaceAll("\\s+", " "); // collapse spaces
        normalized = TRAILING_NUMBERS.matcher(normalized).replaceAll(""); // remove "01/02"
        return normalized.trim();
    }
}