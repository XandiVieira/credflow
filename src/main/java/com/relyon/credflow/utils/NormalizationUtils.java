package com.relyon.credflow.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.codec.digest.DigestUtils;

public class NormalizationUtils {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String normalizeDescription(String rawDescription) {
        if (rawDescription == null) return "";

        var desc = rawDescription.toLowerCase().trim();
        desc = desc.replaceAll("\\d{2}/\\d{2}/\\d{4}", "");
        desc = desc.replaceAll("\\d{4}-\\d{2}-\\d{2}", "");
        desc = desc.replaceAll("\\b\\d{1,2}/\\d{2}\\b", "");
        desc = desc.replaceAll("\\b\\d{1,2}h(?:\\d{2})?\\b", "");
        desc = desc.replaceAll("[^a-z0-9 ]", " ");
        return desc.replaceAll("\\s+", " ").trim();
    }

    public static String generateNormalizedChecksum(LocalDate date, String description, BigDecimal value, Long accountId) {
        var normalizedDate = date.format(ISO_DATE);
        var normalizedDesc = normalizeForChecksum(description);
        var normalizedValue = value.abs().stripTrailingZeros().toPlainString();

        var checksumInput = String.join("|", normalizedDate, normalizedDesc, normalizedValue, accountId.toString());
        return DigestUtils.sha256Hex(checksumInput);
    }

    private static String normalizeForChecksum(String description) {
        if (description == null) return "";
        return description
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private NormalizationUtils() {
    }
}
