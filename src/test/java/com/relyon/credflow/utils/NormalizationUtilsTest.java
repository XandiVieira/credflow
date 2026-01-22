package com.relyon.credflow.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

    @Nested
    class GenerateNormalizedChecksum {

        @Test
        void shouldGenerateSameChecksumForPdfAndCsvFormats() {
            var date = LocalDate.of(2025, 7, 31);
            var value = new BigDecimal("196.50");
            var accountId = 1L;

            var pdfDescription = "FeFloresCostura 02/02";
            var csvDescription = "FeFloresCostura     02/02";

            var pdfChecksum = NormalizationUtils.generateNormalizedChecksum(date, pdfDescription, value, accountId);
            var csvChecksum = NormalizationUtils.generateNormalizedChecksum(date, csvDescription, value, accountId);

            assertThat(pdfChecksum).isEqualTo(csvChecksum);
        }

        @Test
        void shouldGenerateSameChecksumRegardlessOfWhitespace() {
            var date = LocalDate.of(2025, 8, 26);
            var value = new BigDecimal("25.97");
            var accountId = 1L;

            var desc1 = "ADHomeMarketLtda PORTO ALEGRE BRA";
            var desc2 = "ADHomeMarketLtda   PORTO ALEGRE   BRA  ";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, desc1, value, accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, desc2, value, accountId);

            assertThat(checksum1).isEqualTo(checksum2);
        }

        @Test
        void shouldGenerateSameChecksumRegardlessOfCase() {
            var date = LocalDate.of(2025, 9, 1);
            var value = new BigDecimal("12855.13");
            var accountId = 1L;

            var desc1 = "PGTO HOME/OFFICE BANKING";
            var desc2 = "pgto home/office banking";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, desc1, value, accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, desc2, value, accountId);

            assertThat(checksum1).isEqualTo(checksum2);
        }

        @Test
        void shouldGenerateDifferentChecksumForDifferentDates() {
            var value = new BigDecimal("100.00");
            var accountId = 1L;
            var description = "Same description";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(LocalDate.of(2025, 1, 1), description, value, accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(LocalDate.of(2025, 1, 2), description, value, accountId);

            assertThat(checksum1).isNotEqualTo(checksum2);
        }

        @Test
        void shouldGenerateDifferentChecksumForDifferentValues() {
            var date = LocalDate.of(2025, 1, 1);
            var accountId = 1L;
            var description = "Same description";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("100.00"), accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("100.01"), accountId);

            assertThat(checksum1).isNotEqualTo(checksum2);
        }

        @Test
        void shouldGenerateDifferentChecksumForDifferentAccounts() {
            var date = LocalDate.of(2025, 1, 1);
            var value = new BigDecimal("100.00");
            var description = "Same description";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, description, value, 1L);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, description, value, 2L);

            assertThat(checksum1).isNotEqualTo(checksum2);
        }

        @Test
        void shouldUsAbsoluteValueForNegativeAmounts() {
            var date = LocalDate.of(2025, 1, 1);
            var accountId = 1L;
            var description = "Payment";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("100.00"), accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("-100.00"), accountId);

            assertThat(checksum1).isEqualTo(checksum2);
        }

        @Test
        void shouldHandleDecimalPrecisionDifferences() {
            var date = LocalDate.of(2025, 1, 1);
            var accountId = 1L;
            var description = "Payment";

            var checksum1 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("100"), accountId);
            var checksum2 = NormalizationUtils.generateNormalizedChecksum(date, description, new BigDecimal("100.00"), accountId);

            assertThat(checksum1).isEqualTo(checksum2);
        }
    }
}
