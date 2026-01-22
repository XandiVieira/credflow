package com.relyon.credflow.constant;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessConstants {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Budget {
        public static final int ROLLOVER_MAX_MONTHS_DEFAULT = 2;
        public static final int ROLLOVER_MAX_MONTHS_LIMIT = 12;
        public static final int ROLLOVER_MAX_PERCENTAGE_DEFAULT = 50;
        public static final int ROLLOVER_MAX_PERCENTAGE_LIMIT = 200;
        public static final int YELLOW_WARNING_THRESHOLD_DEFAULT = 80;
        public static final int YELLOW_WARNING_THRESHOLD_LIMIT = 200;
        public static final int ORANGE_WARNING_THRESHOLD_DEFAULT = 100;
        public static final int ORANGE_WARNING_THRESHOLD_LIMIT = 200;
        public static final int RED_WARNING_THRESHOLD_DEFAULT = 120;
        public static final int RED_WARNING_THRESHOLD_LIMIT = 300;
        public static final int PROJECTED_WARNING_MIN_DAYS_DEFAULT = 5;
        public static final int PROJECTED_WARNING_MIN_DAYS_LIMIT = 31;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Transaction {
        public static final int MIN_INSTALLMENTS = 2;
        public static final int MAX_INSTALLMENTS = 120;
        public static final int MAX_INSTALLMENTS_EXTENDED = 360;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class CreditCard {
        public static final int MAX_DAY_OF_MONTH = 31;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Dashboard {
        public static final int TOP_CATEGORIES_LIMIT = 5;
        public static final int UPCOMING_BILLS_WINDOW_DAYS = 30;
        public static final int UPCOMING_BILLS_LIMIT = 10;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Export {
        public static final int PDF_TITLE_FONT_SIZE = 20;
        public static final int PDF_SUBTITLE_FONT_SIZE = 12;
        public static final int PDF_TABLE_WIDTH = 550;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Detection {
        public static final int REVERSAL_SEARCH_WINDOW_DAYS = 90;
        public static final double DESCRIPTION_SIMILARITY_THRESHOLD = 0.6;
        public static final int DUPLICATE_SEARCH_WINDOW_DAYS = 3;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Pagination {
        public static final String DEFAULT_PAGE_SIZE = "20";
        public static final int DEFAULT_PAGE_SIZE_INT = 20;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Decimal {
        public static final int SCALE = 2;
        public static final int PRECISION = 14;
        public static final BigDecimal PERCENTAGE_MULTIPLIER = BigDecimal.valueOf(100);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Security {
        public static final int JWT_SECRET_MIN_LENGTH = 32;
    }
}
