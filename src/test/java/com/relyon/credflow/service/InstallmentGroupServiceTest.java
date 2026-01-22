package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.InstallmentGroupRequestDTO;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstallmentGroupServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private InstallmentGroupService installmentGroupService;

    @Test
    void createInstallmentGroup_whenValidRequest_shouldCreateAllInstallments() {
        var accountId = 1L;
        var categoryId = 10L;
        var account = Account.builder().id(accountId).build();
        var category = Category.builder().id(categoryId).account(account).build();

        var request = new InstallmentGroupRequestDTO();
        request.setDescription("Laptop Purchase");
        request.setTotalAmount(new BigDecimal("1200.00"));
        request.setTransactionType(TransactionType.INSTALLMENT);
        request.setCategoryId(categoryId);
        request.setTotalInstallments(12);
        request.setFirstInstallmentDate(LocalDate.of(2025, 1, 15));

        when(categoryRepository.findByIdAndAccountId(categoryId, accountId))
                .thenReturn(Optional.of(category));

        var captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        var savedTransactions = List.of(
                Transaction.builder().id(1L).value(new BigDecimal("100.00")).totalInstallments(12).build(),
                Transaction.builder().id(2L).value(new BigDecimal("100.00")).totalInstallments(12).build()
        );
        when(transactionRepository.findByInstallmentGroupIdAndAccountId(any(), eq(accountId)))
                .thenReturn(savedTransactions);

        var result = installmentGroupService.createInstallmentGroup(request, accountId);

        verify(transactionRepository, times(12)).save(any(Transaction.class));

        var savedInstallments = captor.getAllValues();
        assertThat(savedInstallments).hasSize(12);

        assertThat(savedInstallments.getFirst().getValue()).isEqualByComparingTo("100.00");
        assertThat(savedInstallments.getFirst().getCurrentInstallment()).isEqualTo(1);
        assertThat(savedInstallments.getFirst().getTotalInstallments()).isEqualTo(12);
        assertThat(savedInstallments.getFirst().getDate()).isEqualTo(LocalDate.of(2025, 1, 15));

        assertThat(savedInstallments.get(11).getCurrentInstallment()).isEqualTo(12);
        assertThat(savedInstallments.get(11).getDate()).isEqualTo(LocalDate.of(2025, 12, 15));

        assertThat(savedInstallments.getFirst().getInstallmentGroupId())
                .isEqualTo(savedInstallments.get(11).getInstallmentGroupId());
    }

    @Test
    void createInstallmentGroup_whenCategoryNotFound_shouldThrowException() {
        var accountId = 1L;
        var categoryId = 999L;

        var request = new InstallmentGroupRequestDTO();
        request.setCategoryId(categoryId);
        request.setTotalInstallments(12);
        request.setTotalAmount(new BigDecimal("1200.00"));
        request.setFirstInstallmentDate(LocalDate.now());

        when(categoryRepository.findByIdAndAccountId(categoryId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> installmentGroupService.createInstallmentGroup(request, accountId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findByIdAndAccountId(categoryId, accountId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createInstallmentGroup_withCreditCard_shouldSetCreditCardOnAllInstallments() {
        var accountId = 1L;
        var categoryId = 10L;
        var creditCardId = 5L;

        var account = Account.builder().id(accountId).build();
        var category = Category.builder().id(categoryId).account(account).build();
        var creditCard = CreditCard.builder().id(creditCardId).build();

        var request = new InstallmentGroupRequestDTO();
        request.setDescription("Purchase");
        request.setTotalAmount(new BigDecimal("600.00"));
        request.setCategoryId(categoryId);
        request.setCreditCardId(creditCardId);
        request.setTotalInstallments(6);
        request.setFirstInstallmentDate(LocalDate.now());

        when(categoryRepository.findByIdAndAccountId(categoryId, accountId))
                .thenReturn(Optional.of(category));
        when(creditCardRepository.findByIdAndAccountId(creditCardId, accountId))
                .thenReturn(Optional.of(creditCard));

        var captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        var savedTransactions = List.of(
                Transaction.builder().id(1L).value(new BigDecimal("100.00")).totalInstallments(6).build(),
                Transaction.builder().id(2L).value(new BigDecimal("100.00")).totalInstallments(6).build()
        );
        when(transactionRepository.findByInstallmentGroupIdAndAccountId(any(), eq(accountId)))
                .thenReturn(savedTransactions);

        installmentGroupService.createInstallmentGroup(request, accountId);

        var savedInstallments = captor.getAllValues();
        assertThat(savedInstallments).hasSize(6);
        assertThat(savedInstallments.getFirst().getCreditCard()).isEqualTo(creditCard);
    }

    @Test
    void getInstallmentGroup_whenGroupExists_shouldReturnSummary() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;

        var transactions = List.of(
                Transaction.builder()
                        .id(1L)
                        .description("Test")
                        .value(new BigDecimal("100.00"))
                        .currentInstallment(1)
                        .totalInstallments(3)
                        .installmentGroupId(installmentGroupId)
                        .wasEditedAfterImport(false)
                        .build(),
                Transaction.builder()
                        .id(2L)
                        .description("Test")
                        .value(new BigDecimal("100.00"))
                        .currentInstallment(2)
                        .totalInstallments(3)
                        .installmentGroupId(installmentGroupId)
                        .wasEditedAfterImport(true)
                        .build()
        );

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(transactions);

        var result = installmentGroupService.getInstallmentGroup(installmentGroupId, accountId);

        assertThat(result.getInstallmentGroupId()).isEqualTo(installmentGroupId);
        assertThat(result.getTotalInstallments()).isEqualTo(3);
        assertThat(result.getPaidInstallments()).isEqualTo(1);
        assertThat(result.getPendingInstallments()).isEqualTo(2);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(result.getTotalPaid()).isEqualByComparingTo("100.00");
    }

    @Test
    void getInstallmentGroup_whenGroupNotFound_shouldThrowException() {
        var installmentGroupId = "non-existent";
        var accountId = 1L;

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> installmentGroupService.getInstallmentGroup(installmentGroupId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteInstallmentGroup_whenGroupExists_shouldDeleteAllInstallments() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;

        var transactions = List.of(
                Transaction.builder().id(1L).installmentGroupId(installmentGroupId).build(),
                Transaction.builder().id(2L).installmentGroupId(installmentGroupId).build(),
                Transaction.builder().id(3L).installmentGroupId(installmentGroupId).build()
        );

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(transactions);

        installmentGroupService.deleteInstallmentGroup(installmentGroupId, accountId);

        verify(transactionRepository).deleteById(1L);
        verify(transactionRepository).deleteById(2L);
        verify(transactionRepository).deleteById(3L);
    }

    @Test
    void updateInstallmentGroupDescription_whenGroupExists_shouldUpdateAllDescriptions() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;
        var newDescription = "Updated Description";

        var transaction1 = Transaction.builder()
                .id(1L)
                .description("Old")
                .installmentGroupId(installmentGroupId)
                .value(new BigDecimal("100"))
                .totalInstallments(2)
                .build();
        var transaction2 = Transaction.builder()
                .id(2L)
                .description("Old")
                .installmentGroupId(installmentGroupId)
                .value(new BigDecimal("100"))
                .totalInstallments(2)
                .build();

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of(transaction1, transaction2));

        installmentGroupService.updateInstallmentGroupDescription(installmentGroupId, accountId, newDescription);

        assertThat(transaction1.getDescription()).isEqualTo(newDescription);
        assertThat(transaction2.getDescription()).isEqualTo(newDescription);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void updateInstallmentGroup_whenValidRequest_shouldUpdateAllInstallments() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;
        var categoryId = 10L;
        var newCategoryId = 20L;

        var account = Account.builder().id(accountId).build();
        var oldCategory = Category.builder().id(categoryId).account(account).build();
        var newCategory = Category.builder().id(newCategoryId).account(account).build();

        var transaction1 = Transaction.builder()
                .id(1L)
                .description("Old Description")
                .value(new BigDecimal("100.00"))
                .category(oldCategory)
                .installmentGroupId(installmentGroupId)
                .totalInstallments(3)
                .build();
        var transaction2 = Transaction.builder()
                .id(2L)
                .description("Old Description")
                .value(new BigDecimal("100.00"))
                .category(oldCategory)
                .installmentGroupId(installmentGroupId)
                .totalInstallments(3)
                .build();

        var request = new InstallmentGroupRequestDTO();
        request.setDescription("Updated Description");
        request.setTotalAmount(new BigDecimal("600.00"));
        request.setCategoryId(newCategoryId);
        request.setTotalInstallments(3);
        request.setFirstInstallmentDate(LocalDate.now());

        when(categoryRepository.findByIdAndAccountId(newCategoryId, accountId))
                .thenReturn(Optional.of(newCategory));
        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of(transaction1, transaction2))
                .thenReturn(List.of(transaction1, transaction2));

        installmentGroupService.updateInstallmentGroup(installmentGroupId, accountId, request);

        assertThat(transaction1.getDescription()).isEqualTo("Updated Description");
        assertThat(transaction1.getValue()).isEqualByComparingTo("200.00");
        assertThat(transaction1.getCategory()).isEqualTo(newCategory);

        assertThat(transaction2.getDescription()).isEqualTo("Updated Description");
        assertThat(transaction2.getValue()).isEqualByComparingTo("200.00");
        assertThat(transaction2.getCategory()).isEqualTo(newCategory);

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void updateInstallmentGroup_withCreditCard_shouldUpdateAllInstallments() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;
        var categoryId = 10L;
        var creditCardId = 5L;

        var account = Account.builder().id(accountId).build();
        var category = Category.builder().id(categoryId).account(account).build();
        var creditCard = CreditCard.builder().id(creditCardId).build();

        var transaction1 = Transaction.builder()
                .id(1L)
                .description("Old")
                .value(new BigDecimal("100.00"))
                .category(category)
                .installmentGroupId(installmentGroupId)
                .totalInstallments(2)
                .build();

        var request = new InstallmentGroupRequestDTO();
        request.setDescription("Updated");
        request.setTotalAmount(new BigDecimal("400.00"));
        request.setCategoryId(categoryId);
        request.setCreditCardId(creditCardId);
        request.setTotalInstallments(2);
        request.setFirstInstallmentDate(LocalDate.now());

        when(categoryRepository.findByIdAndAccountId(categoryId, accountId))
                .thenReturn(Optional.of(category));
        when(creditCardRepository.findByIdAndAccountId(creditCardId, accountId))
                .thenReturn(Optional.of(creditCard));
        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of(transaction1))
                .thenReturn(List.of(transaction1));

        installmentGroupService.updateInstallmentGroup(installmentGroupId, accountId, request);

        assertThat(transaction1.getCreditCard()).isEqualTo(creditCard);
        assertThat(transaction1.getValue()).isEqualByComparingTo("200.00");
    }

    @Test
    void updateInstallmentGroup_whenGroupNotFound_shouldThrowException() {
        var installmentGroupId = "non-existent";
        var accountId = 1L;

        var request = new InstallmentGroupRequestDTO();
        request.setTotalAmount(new BigDecimal("600.00"));
        request.setTotalInstallments(3);

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> installmentGroupService.updateInstallmentGroup(installmentGroupId, accountId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository).findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateInstallmentGroup_whenCategoryNotFound_shouldThrowException() {
        var installmentGroupId = "test-group-id";
        var accountId = 1L;
        var categoryId = 999L;

        var transaction1 = Transaction.builder()
                .id(1L)
                .installmentGroupId(installmentGroupId)
                .build();

        var request = new InstallmentGroupRequestDTO();
        request.setTotalAmount(new BigDecimal("600.00"));
        request.setCategoryId(categoryId);
        request.setTotalInstallments(3);

        when(transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId))
                .thenReturn(List.of(transaction1));
        when(categoryRepository.findByIdAndAccountId(categoryId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> installmentGroupService.updateInstallmentGroup(installmentGroupId, accountId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(categoryRepository).findByIdAndAccountId(categoryId, accountId);
    }

    @Test
    void getAllInstallmentGroups_whenGroupsExist_shouldReturnSummaries() {
        var accountId = 1L;
        var groupId1 = "group-1";
        var groupId2 = "group-2";

        var category = Category.builder().id(1L).name("Electronics").build();
        var creditCard = CreditCard.builder().id(1L).nickname("Visa").build();

        var transactions = List.of(
                Transaction.builder()
                        .id(1L)
                        .description("Laptop")
                        .value(new BigDecimal("100.00"))
                        .date(LocalDate.of(2025, 1, 15))
                        .currentInstallment(1)
                        .totalInstallments(3)
                        .installmentGroupId(groupId1)
                        .wasEditedAfterImport(true)
                        .category(category)
                        .creditCard(creditCard)
                        .build(),
                Transaction.builder()
                        .id(2L)
                        .description("Laptop")
                        .value(new BigDecimal("100.00"))
                        .date(LocalDate.of(2025, 2, 15))
                        .currentInstallment(2)
                        .totalInstallments(3)
                        .installmentGroupId(groupId1)
                        .wasEditedAfterImport(false)
                        .category(category)
                        .creditCard(creditCard)
                        .build(),
                Transaction.builder()
                        .id(3L)
                        .description("Laptop")
                        .value(new BigDecimal("100.00"))
                        .date(LocalDate.of(2025, 3, 15))
                        .currentInstallment(3)
                        .totalInstallments(3)
                        .installmentGroupId(groupId1)
                        .wasEditedAfterImport(false)
                        .category(category)
                        .creditCard(creditCard)
                        .build(),
                Transaction.builder()
                        .id(4L)
                        .description("Phone")
                        .value(new BigDecimal("50.00"))
                        .date(LocalDate.of(2025, 2, 1))
                        .currentInstallment(1)
                        .totalInstallments(2)
                        .installmentGroupId(groupId2)
                        .wasEditedAfterImport(false)
                        .category(category)
                        .build(),
                Transaction.builder()
                        .id(5L)
                        .description("Phone")
                        .value(new BigDecimal("50.00"))
                        .date(LocalDate.of(2025, 3, 1))
                        .currentInstallment(2)
                        .totalInstallments(2)
                        .installmentGroupId(groupId2)
                        .wasEditedAfterImport(false)
                        .category(category)
                        .build()
        );

        when(transactionRepository.findAllInstallmentsByAccountId(accountId))
                .thenReturn(transactions);

        var result = installmentGroupService.getAllInstallmentGroups(accountId);

        assertThat(result).hasSize(2);

        var group1 = result.stream().filter(g -> g.getInstallmentGroupId().equals(groupId1)).findFirst().orElseThrow();
        assertThat(group1.getDescription()).isEqualTo("Laptop");
        assertThat(group1.getTotalAmount()).isEqualByComparingTo("300.00");
        assertThat(group1.getTotalInstallments()).isEqualTo(3);
        assertThat(group1.getPaidInstallments()).isEqualTo(1);
        assertThat(group1.getPendingInstallments()).isEqualTo(2);
        assertThat(group1.getTotalPaid()).isEqualByComparingTo("100.00");
        assertThat(group1.getTotalPending()).isEqualByComparingTo("200.00");
        assertThat(group1.getFirstInstallmentDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(group1.getLastInstallmentDate()).isEqualTo(LocalDate.of(2025, 3, 15));
        assertThat(group1.getCategoryName()).isEqualTo("Electronics");
        assertThat(group1.getCreditCardNickname()).isEqualTo("Visa");

        var group2 = result.stream().filter(g -> g.getInstallmentGroupId().equals(groupId2)).findFirst().orElseThrow();
        assertThat(group2.getDescription()).isEqualTo("Phone");
        assertThat(group2.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(group2.getTotalInstallments()).isEqualTo(2);
        assertThat(group2.getPaidInstallments()).isEqualTo(0);
        assertThat(group2.getPendingInstallments()).isEqualTo(2);
        assertThat(group2.getCreditCardNickname()).isNull();
    }

    @Test
    void getAllInstallmentGroups_whenNoGroups_shouldReturnEmptyList() {
        var accountId = 1L;

        when(transactionRepository.findAllInstallmentsByAccountId(accountId))
                .thenReturn(List.of());

        var result = installmentGroupService.getAllInstallmentGroups(accountId);

        assertThat(result).isEmpty();
    }
}
