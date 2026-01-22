package com.relyon.credflow.specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionSpecFactoryTest {

    @Mock
    private Root<Transaction> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<Object> path;

    @Mock
    private Path<String> stringPath;

    @Mock
    private Path<LocalDate> datePath;

    @Mock
    private Path<BigDecimal> bigDecimalPath;

    @Mock
    private Path<Boolean> booleanPath;

    @Mock
    private Join<Object, Object> join;

    @Mock
    private Predicate predicate;

    @Mock
    private Expression<String> lowerExpression;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(path.get(anyString())).thenReturn(path);
    }

    @Test
    void from_allNullFilters_returnsSpecificationThatDoesNotAddPredicates() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        var spec = TransactionSpecFactory.from(filter);

        assertNotNull(spec);
    }

    @Test
    void from_withAccountId_createsAccountIdPredicate() {
        var filter = new TransactionFilter(
                1L, null, null, null, null, null, null, null, null, null, null, null, null
        );

        when(root.get("account")).thenReturn(path);
        when(path.get("id")).thenReturn(path);
        when(criteriaBuilder.equal(any(), eq(1L))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(any(), eq(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withFromDate_createsGreaterThanOrEqualPredicate() {
        var fromDate = LocalDate.of(2024, 1, 1);
        var filter = new TransactionFilter(
                null, fromDate, null, null, null, null, null, null, null, null, null, null, null
        );

        when(root.get("date")).thenReturn((Path) datePath);
        when(criteriaBuilder.greaterThanOrEqualTo(any(Path.class), eq(fromDate))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(any(Path.class), eq(fromDate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withToDate_createsLessThanOrEqualPredicate() {
        var toDate = LocalDate.of(2024, 12, 31);
        var filter = new TransactionFilter(
                null, null, toDate, null, null, null, null, null, null, null, null, null, null
        );

        when(root.get("date")).thenReturn((Path) datePath);
        when(criteriaBuilder.lessThanOrEqualTo(any(Path.class), eq(toDate))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).lessThanOrEqualTo(any(Path.class), eq(toDate));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withDescriptionContains_createsLikePredicate() {
        var filter = new TransactionFilter(
                null, null, null, "grocery", null, null, null, null, null, null, null, null, null
        );

        when(root.get("description")).thenReturn((Path) stringPath);
        when(criteriaBuilder.lower(any())).thenReturn(lowerExpression);
        when(criteriaBuilder.like(eq(lowerExpression), eq("%grocery%"))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(eq(lowerExpression), eq("%grocery%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withSimplifiedContains_createsLikePredicate() {
        var filter = new TransactionFilter(
                null, null, null, null, "food", null, null, null, null, null, null, null, null
        );

        when(root.get("simplifiedDescription")).thenReturn((Path) stringPath);
        when(criteriaBuilder.lower(any())).thenReturn(lowerExpression);
        when(criteriaBuilder.like(eq(lowerExpression), eq("%food%"))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(eq(lowerExpression), eq("%food%"));
    }

    @Test
    void from_withBlankDescriptionContains_doesNotCreatePredicate() {
        var filter = new TransactionFilter(
                null, null, null, "   ", null, null, null, null, null, null, null, null, null
        );

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder, never()).like(any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withMinAmount_createsGreaterThanOrEqualPredicate() {
        var minAmount = BigDecimal.valueOf(100);
        var filter = new TransactionFilter(
                null, null, null, null, null, minAmount, null, null, null, null, null, null, null
        );

        when(root.get("value")).thenReturn((Path) bigDecimalPath);
        when(criteriaBuilder.greaterThanOrEqualTo(any(Path.class), eq(minAmount))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).greaterThanOrEqualTo(any(Path.class), eq(minAmount));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withMaxAmount_createsLessThanOrEqualPredicate() {
        var maxAmount = BigDecimal.valueOf(500);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, maxAmount, null, null, null, null, null, null
        );

        when(root.get("value")).thenReturn((Path) bigDecimalPath);
        when(criteriaBuilder.lessThanOrEqualTo(any(Path.class), eq(maxAmount))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).lessThanOrEqualTo(any(Path.class), eq(maxAmount));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withResponsibleUserIds_createsJoinAndInPredicate() {
        var userIds = List.of(1L, 2L);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, userIds, null, null, null, null, null
        );

        when(root.join("responsibleUsers", JoinType.LEFT)).thenReturn(join);
        when(join.get("id")).thenReturn(path);
        when(path.in(userIds)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(query).distinct(true);
        verify(root).join("responsibleUsers", JoinType.LEFT);
    }

    @Test
    void from_withEmptyResponsibleUserIds_doesNotCreateJoin() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, List.of(), null, null, null, null, null
        );

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withCategoryIds_createsJoinAndInPredicate() {
        var categoryIds = List.of(1L, 2L, 3L);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, categoryIds, null, null, null, null
        );

        when(root.join("category", JoinType.LEFT)).thenReturn(join);
        when(join.get("id")).thenReturn(path);
        when(path.in(categoryIds)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(query).distinct(true);
        verify(root).join("category", JoinType.LEFT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_withCreditCardIds_createsJoinAndInPredicate() {
        var creditCardIds = List.of(10L, 20L);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, creditCardIds, null, null, null
        );

        when(root.join("creditCard", JoinType.LEFT)).thenReturn(join);
        when(join.get("id")).thenReturn(path);
        when(path.in(creditCardIds)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(query).distinct(true);
        verify(root).join("creditCard", JoinType.LEFT);
    }

    @Test
    void from_withTransactionTypes_createsInPredicate() {
        var types = List.of(TransactionType.ONE_TIME, TransactionType.RECURRING);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, types, null, null
        );

        when(root.get("transactionType")).thenReturn(path);
        when(path.in(types)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(root).get("transactionType");
    }

    @Test
    void from_withEmptyTransactionTypes_doesNotCreatePredicate() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, List.of(), null, null
        );

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(root, never()).get("transactionType");
    }

    @Test
    void from_withTransactionSources_createsInPredicate() {
        var sources = List.of(TransactionSource.MANUAL, TransactionSource.CSV_IMPORT);
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, null, sources, null
        );

        when(root.get("source")).thenReturn(path);
        when(path.in(sources)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(root).get("source");
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_includeReversalsTrue_doesNotFilterReversals() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, null, null, true
        );

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder, never()).equal(any(), eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_includeReversalsFalse_filtersOutReversals() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, null, null, false
        );

        when(root.get("isReversal")).thenReturn((Path) booleanPath);
        when(criteriaBuilder.equal(booleanPath, false)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(any(), eq(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_includeReversalsNull_filtersOutReversals() {
        var filter = new TransactionFilter(
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        when(root.get("isReversal")).thenReturn((Path) booleanPath);
        when(criteriaBuilder.equal(booleanPath, false)).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(any(), eq(false));
    }

    @Test
    void from_descriptionWithWhitespace_trimsBeforeLike() {
        var filter = new TransactionFilter(
                null, null, null, "  test  ", null, null, null, null, null, null, null, null, null
        );

        when(root.get("description")).thenReturn((Path) stringPath);
        when(criteriaBuilder.lower(any())).thenReturn(lowerExpression);
        when(criteriaBuilder.like(eq(lowerExpression), eq("%test%"))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).like(eq(lowerExpression), eq("%test%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void from_multipleFilters_combinesAllPredicates() {
        var filter = new TransactionFilter(
                1L,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "grocery",
                null,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(1000),
                null,
                List.of(1L),
                null,
                List.of(TransactionType.ONE_TIME),
                null,
                false
        );

        lenient().when(root.get("account")).thenReturn(path);
        lenient().when(path.get("id")).thenReturn(path);
        lenient().when(root.get("date")).thenReturn((Path) datePath);
        lenient().when(root.get("description")).thenReturn((Path) stringPath);
        lenient().when(root.get("value")).thenReturn((Path) bigDecimalPath);
        lenient().when(root.get("transactionType")).thenReturn(path);
        lenient().when(root.get("isReversal")).thenReturn((Path) booleanPath);
        lenient().when(root.join("category", JoinType.LEFT)).thenReturn(join);
        lenient().when(join.get("id")).thenReturn(path);
        lenient().when(criteriaBuilder.lower(any())).thenReturn(lowerExpression);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
        lenient().when(criteriaBuilder.greaterThanOrEqualTo(any(Path.class), any(LocalDate.class))).thenReturn(predicate);
        lenient().when(criteriaBuilder.lessThanOrEqualTo(any(Path.class), any(LocalDate.class))).thenReturn(predicate);
        lenient().when(criteriaBuilder.greaterThanOrEqualTo(any(Path.class), any(BigDecimal.class))).thenReturn(predicate);
        lenient().when(criteriaBuilder.lessThanOrEqualTo(any(Path.class), any(BigDecimal.class))).thenReturn(predicate);
        lenient().when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        lenient().when(path.in(any(List.class))).thenReturn(predicate);

        var spec = TransactionSpecFactory.from(filter);
        assertNotNull(spec);
        spec.toPredicate(root, query, criteriaBuilder);
    }
}
