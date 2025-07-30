package com.relyon.credflow.utils;

public class NormalizationUtils {

    public static String normalizeDescription(String rawDescription) {
        if (rawDescription == null) return "";

        var desc = rawDescription.toLowerCase().trim();
        desc = desc.replaceAll("\\b\\d{1,2}/\\d{2}\\b", "");
        desc = desc.replaceAll("\\d{2}/\\d{2}/\\d{4}", "");
        desc = desc.replaceAll("\\d{4}-\\d{2}-\\d{2}", "");
        desc = desc.replaceAll("\\b\\d{1,2}h(?:\\d{2})?\\b", "");
        desc = desc.replaceAll("[^a-z0-9 ]", " ");
        return desc.replaceAll("\\s+", " ").trim();
    }
}