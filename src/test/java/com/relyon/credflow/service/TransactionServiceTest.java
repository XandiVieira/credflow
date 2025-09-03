package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;
    @Mock
    private DescriptionMappingRepository mappingRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService service;

    private static User user(long id, long accountId, String name) {
        var u = new User();
        u.setId(id);
        u.setName(name);
        var acc = new Account();
        acc.setId(accountId);
        u.setAccount(acc);
        return u;
    }

    private static LinkedHashSet<User> set(User... users) {
        return new LinkedHashSet<>(Arrays.asList(users));
    }

    private static Transaction txStubWithResponsibles(Set<User> stubs) {
        var t = new Transaction();
        t.setDate(LocalDate.of(2025, 7, 28));
        t.setDescription("Supermercado Zaffari");
        t.setSimplifiedDescription("Mercado");
        t.setCategory(new Category());
        t.getCategory().setId(10L);
        t.getCategory().setName("Alimentação");
        t.setValue(new BigDecimal("230.50"));
        t.setResponsibles(stubs);
        return t;
    }

    @Test
    void create_setsAccount_resolvesResponsibles_saves_andReturnsSaved() {
        var accountId = 1L;

        var stubA = new User();
        stubA.setId(2L);
        var stubB = new User();
        stubB.setId(3L);
        var input = txStubWithResponsibles(set(stubA, stubB));

        var account = new Account();
        account.setId(accountId);
        when(accountService.findById(accountId)).thenReturn(account);

        var u2 = user(2L, accountId, "Ana");
        var u3 = user(3L, accountId, "Beto");
        when(userService.findAllByIds(List.of(2L, 3L))).thenReturn(List.of(u2, u3));

        when(mappingRepository.findByNormalizedDescriptionAndAccountId(anyString(), eq(accountId)))
                .thenReturn(Optional.empty());

        var saved = new Transaction();
        saved.setId(99L);
        when(repository.save(any(Transaction.class))).thenReturn(saved);

        var result = service.create(input, accountId);

        assertSame(saved, result);
        assertSame(account, input.getAccount());
        assertEquals(2, input.getResponsibles().size());

        verify(accountService).findById(accountId);
        verify(userService).findAllByIds(List.of(2L, 3L));
        verify(mappingRepository).findByNormalizedDescriptionAndAccountId(anyString(), eq(accountId));
        verify(mappingRepository).save(any(DescriptionMapping.class)); // ✅ added
        verify(repository).save(input);
        verifyNoMoreInteractions(repository, mappingRepository, accountService, userService);
    }

    @Test
    void create_whenResponsibleNotFound_throwsIllegalArgument() {
        var accountId = 1L;

        var s = new User();
        s.setId(42L);
        var input = txStubWithResponsibles(set(s));

        when(accountService.findById(accountId)).thenReturn(new Account() {{
            setId(accountId);
        }});
        when(userService.findAllByIds(List.of(42L))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.create(input, accountId));

        verify(userService).findAllByIds(List.of(42L));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void create_whenResponsibleBelongsToAnotherAccount_throwsIllegalArgument() {
        var accountId = 1L;

        var s = new User();
        s.setId(5L);
        var input = txStubWithResponsibles(set(s));

        when(accountService.findById(accountId)).thenReturn(new Account() {{
            setId(accountId);
        }});
        var foreign = user(5L, 999L, "Other");
        when(userService.findAllByIds(List.of(5L))).thenReturn(List.of(foreign));

        assertThrows(IllegalArgumentException.class, () -> service.create(input, accountId));
    }

    @Test
    void update_whenFound_updatesFields_resolvesResponsibles_andSaves() {
        var accountId = 1L;
        var id = 7L;

        var existing = new Transaction();
        existing.setId(id);
        existing.setAccount(new Account() {{
            setId(accountId);
        }});

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(existing.getAccount());

        var stub = new User();
        stub.setId(2L);
        var updated = txStubWithResponsibles(set(stub));

        var resolved = user(2L, accountId, "Ana");
        when(userService.findAllByIds(List.of(2L))).thenReturn(List.of(resolved));

        when(mappingRepository.findByNormalizedDescriptionAndAccountId(anyString(), eq(accountId)))
                .thenReturn(Optional.empty());
        when(repository.save(existing)).thenReturn(existing);

        var result = service.update(id, updated, accountId);

        assertSame(existing, result);
        assertEquals(updated.getDate(), existing.getDate());
        assertEquals(updated.getDescription(), existing.getDescription());
        assertEquals(1, existing.getResponsibles().size());

        verify(repository).findByIdAndAccountId(id, accountId);
        verify(userService).findAllByIds(List.of(2L));
        verify(accountService).findById(accountId);
        verify(mappingRepository).findByNormalizedDescriptionAndAccountId(anyString(), eq(accountId));
        verify(mappingRepository).save(any(DescriptionMapping.class)); // <--- ADD THIS
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository, mappingRepository, accountService, userService);
    }


    @Test
    void update_whenMissing_throwsResourceNotFound() {
        when(repository.findByIdAndAccountId(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, new Transaction(), 1L));
    }

    @Test
    void findByFilters_buildsPatterns_defaultsSort_andDelegatesToRepository() {
        var accountId = 1L;

        var capturedSort = ArgumentCaptor.forClass(Sort.class);
        when(repository.search(
                eq(accountId),
                any(), any(),
                any(), any(),
                any(), any(),
                any(), any(),
                capturedSort.capture()))
                .thenReturn(List.of());

        var result = service.findByFilters(
                accountId,
                null, null,
                " Zaf ", null,
                null, null,
                null, null,
                null
        );

        assertNotNull(result);
        var usedSort = capturedSort.getValue();
        assertNotNull(usedSort);
        var order = usedSort.getOrderFor("date");
        assertNotNull(order);
        assertTrue(order.isDescending());

        verify(repository).search(
                eq(accountId),
                eq("%zaf%"),
                isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                isNull(), isNull(),
                any(Sort.class)
        );
    }

    @Test
    void delete_whenFound_deletes() {
        var tx = new Transaction();
        when(repository.findByIdAndAccountId(10L, 1L)).thenReturn(Optional.of(tx));

        assertDoesNotThrow(() -> service.delete(10L, 1L));

        verify(repository).findByIdAndAccountId(10L, 1L);
        verify(repository).delete(tx);
    }

    @Test
    void delete_whenMissing_throwsNotFound() {
        when(repository.findByIdAndAccountId(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(10L, 1L));
    }

    @Test
    void importFromBanrisulCSV_savesNonDuplicateRows_andPersistsNewMappings() throws Exception {
        long accountId = 3L;
        var account = new Account();
        account.setId(accountId);
        when(accountService.findById(accountId)).thenReturn(account);

        when(mappingRepository.findAllByAccountId(accountId)).thenReturn(List.of());

        var csv = """
                AAA;HEADER;TO;SKIP
                28/07/2025;"Supermercado Zaffari";230,50;ignored
                28/07/2025;"Supermercado Zaffari";230,50;ignored
                """;
        var file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("banrisul.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(csv.getBytes()));

        when(repository.existsByChecksum(anyString()))
                .thenReturn(false)
                .thenReturn(true);

        when(repository.save(any(Transaction.class))).thenAnswer(inv -> {
            var t = (Transaction) inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        var result = service.importFromBanrisulCSV(file, accountId);

        assertEquals(1, result.size(), "only non-duplicate row should be saved");

        verify(mappingRepository).saveAll(anyCollection());
        verify(repository, times(1)).save(any(Transaction.class));
    }
}