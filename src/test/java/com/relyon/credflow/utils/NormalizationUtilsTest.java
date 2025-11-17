package com.relyon.credflow.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationUtilsTest {

    @Test
    void normalizeDescription_whenNull_shouldReturnEmptyString() {
        var result = NormalizationUtils.normalizeDescription(null);
        assertThat(result).isEmpty();
    }

    @Test
    void normalizeDescription_whenEmpty_shouldReturnEmptyString() {
        var result = NormalizationUtils.normalizeDescription("");
        assertThat(result).isEmpty();
    }

    @Test
    void normalizeDescription_shouldConvertToLowercase() {
        var result = NormalizationUtils.normalizeDescription("NETFLIX SUBSCRIPTION");
        assertThat(result).isEqualTo("netflix subscription");
    }

    @Test
    void normalizeDescription_shouldRemoveDatesInDDMMFormat() {
        var result = NormalizationUtils.normalizeDescription("Payment 12/11 Restaurant");
        assertThat(result).isEqualTo("payment restaurant");
    }

    @Test
    void normalizeDescription_shouldRemoveDatesInDDMMYYYYFormat() {
        var result = NormalizationUtils.normalizeDescription("Purchase 15/03/2025 Store");
        assertThat(result).isEqualTo("purchase store");
    }

    @Test
    void normalizeDescription_shouldRemoveDatesInYYYYMMDDFormat() {
        var result = NormalizationUtils.normalizeDescription("Order 2025-03-15 Amazon");
        assertThat(result).isEqualTo("order amazon");
    }

    @Test
    void normalizeDescription_shouldRemoveTimeInHourFormat() {
        var result = NormalizationUtils.normalizeDescription("Transaction 14h30 Coffee Shop");
        assertThat(result).isEqualTo("transaction coffee shop");
    }

    @Test
    void normalizeDescription_shouldRemoveTimeWithoutMinutes() {
        var result = NormalizationUtils.normalizeDescription("Payment 9h Store");
        assertThat(result).isEqualTo("payment store");
    }

    @Test
    void normalizeDescription_shouldRemoveSpecialCharacters() {
        var result = NormalizationUtils.normalizeDescription("PAG*NETFLIX#STREAMING@123");
        assertThat(result).isEqualTo("pag netflix streaming 123");
    }

    @Test
    void normalizeDescription_shouldCollapseMultipleSpaces() {
        var result = NormalizationUtils.normalizeDescription("Multiple    spaces   here");
        assertThat(result).isEqualTo("multiple spaces here");
    }

    @Test
    void normalizeDescription_shouldTrimWhitespace() {
        var result = NormalizationUtils.normalizeDescription("  trimmed  ");
        assertThat(result).isEqualTo("trimmed");
    }

    @Test
    void normalizeDescription_shouldHandleComplexRealWorldExample() {
        var result = NormalizationUtils.normalizeDescription(
                "PAG*NETFLIX.COM 15/11 14h30 ASSINATURA@STREAMING"
        );
        assertThat(result).isEqualTo("pag netflix com assinatura streaming");
    }

    @Test
    void normalizeDescription_shouldPreserveNumbersAndLetters() {
        var result = NormalizationUtils.normalizeDescription("Store123 Product ABC");
        assertThat(result).isEqualTo("store123 product abc");
    }

    @Test
    void normalizeDescription_shouldHandleOnlySpecialCharacters() {
        var result = NormalizationUtils.normalizeDescription("@#$%^&*()");
        assertThat(result).isEmpty();
    }

    @Test
    void normalizeDescription_shouldHandleMultipleDatesAndTimes() {
        var result = NormalizationUtils.normalizeDescription(
                "Purchase 12/11 10h30 and 15/12 14h45 Store"
        );
        assertThat(result).isEqualTo("purchase and store");
    }

    @Test
    void normalizeDescription_shouldKeepAlphanumericWithSpaces() {
        var result = NormalizationUtils.normalizeDescription("Amazon Prime 2024");
        assertThat(result).isEqualTo("amazon prime 2024");
    }
}
