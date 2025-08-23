package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescriptionMappingServiceTest {

    @Mock
    private DescriptionMappingRepository repository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private DescriptionMappingService service;

    @Test
    void createAll_savesOnlyNewMappings_setsAccount_normalizes_andAppliesToTransactions() {
        var accountId = 10L;

        var m1 = new DescriptionMapping();
        m1.setOriginalDescription("Loja X 12/03 #123");
        m1.setSimplifiedDescription("Loja X");
        var category = new Category();
        category.setId(123L);
        m1.setCategory(category);

        var m2 = new DescriptionMapping();
        m2.setOriginalDescription("Super Y 04/05");

        var norm1 = NormalizationUtils.normalizeDescription(m1.getOriginalDescription());
        var norm2 = NormalizationUtils.normalizeDescription(m2.getOriginalDescription());

        when(repository.findByNormalizedDescriptionAndAccountId(norm1, accountId)).thenReturn(Optional.empty());
        when(repository.findByNormalizedDescriptionAndAccountId(norm2, accountId)).thenReturn(Optional.of(new DescriptionMapping()));

        var account = new Account();
        account.setId(accountId);
        when(categoryService.findById(123L, accountId)).thenReturn(category);
        when(accountService.findById(accountId)).thenReturn(account);

        var saved1 = new DescriptionMapping();
        saved1.setId(111L);
        saved1.setOriginalDescription(m1.getOriginalDescription());
        saved1.setSimplifiedDescription(m1.getSimplifiedDescription());
        saved1.setCategory(m1.getCategory());
        when(repository.save(same(m1))).thenReturn(saved1);

        var result = service.createAll(List.of(m1, m2), accountId);

        assertEquals(1, result.size());
        assertSame(saved1, result.getFirst());

        assertEquals(norm1, m1.getNormalizedDescription());
        assertSame(account, m1.getAccount());

        verify(accountService, times(2)).findById(accountId);

        verify(repository, times(1)).findByNormalizedDescriptionAndAccountId(norm1, accountId);
        verify(repository, times(1)).findByNormalizedDescriptionAndAccountId(norm2, accountId);

        verify(repository, times(1)).save(same(m1));

        verify(transactionService, times(1)).applyMappingToExistingTransactions(
                eq(accountId),
                eq(saved1.getOriginalDescription()),
                eq(saved1.getSimplifiedDescription()),
                eq(saved1.getCategory())
        );

        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void update_whenFound_appliesChanges_setsAccount_saves_andAppliesToTransactions() {
        var id = 20L;
        var accountId = 10L;

        var updated = new DescriptionMapping();
        updated.setOriginalDescription("Mercado Z 09/09");
        updated.setSimplifiedDescription("Mercado Z");
        var category = new Category();
        category.setId(123L);
        updated.setCategory(category);

        var existing = new DescriptionMapping();
        existing.setId(id);
        existing.setOriginalDescription("Old");
        var account = new Account();
        account.setId(accountId);

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(categoryService.findById(123L, accountId)).thenReturn(category);
        when(accountService.findById(accountId)).thenReturn(account);

        var saved = new DescriptionMapping();
        saved.setId(id);
        saved.setOriginalDescription(updated.getOriginalDescription());
        saved.setSimplifiedDescription(updated.getSimplifiedDescription());
        saved.setCategory(updated.getCategory());
        when(repository.save(same(existing))).thenReturn(saved);

        var result = service.update(id, updated, accountId);

        assertSame(saved, result);
        assertEquals("Mercado Z 09/09", existing.getOriginalDescription());
        assertEquals("Mercado Z", existing.getSimplifiedDescription());
        assertSame(updated.getCategory(), existing.getCategory());
        assertSame(account, existing.getAccount());

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, times(1)).findById(accountId);
        verify(repository, times(1)).save(same(existing));
        verify(transactionService, times(1)).applyMappingToExistingTransactions(
                eq(accountId),
                eq(saved.getOriginalDescription()),
                eq(saved.getSimplifiedDescription()),
                eq(saved.getCategory())
        );
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void update_whenNotFound_throwsResourceNotFound_andDoesNotSave() {
        var id = 21L;
        var accountId = 10L;

        var updated = new DescriptionMapping();
        updated.setOriginalDescription("Anything");

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, updated, accountId));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(repository, never()).save(any());
        verify(transactionService, never()).applyMappingToExistingTransactions(anyLong(), anyString(), anyString(), any());
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void update_whenUpdatedOriginalDescriptionIsNull_doesNotRecomputeNormalizedDescription_butUpdatesOtherFields() {
        var id = 22L;
        var accountId = 10L;

        var existing = new DescriptionMapping();
        existing.setId(id);
        existing.setOriginalDescription("Old desc");
        existing.setSimplifiedDescription("Old simp");
        existing.setNormalizedDescription("old-normalized");
        var account = new Account();
        account.setId(accountId);

        var updated = new DescriptionMapping();
        updated.setOriginalDescription(null);
        updated.setSimplifiedDescription("New simp");
        var category = new Category();
        category.setId(123L);
        updated.setCategory(category);

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(categoryService.findById(123L, accountId)).thenReturn(category);
        when(accountService.findById(accountId)).thenReturn(account);

        when(repository.save(same(existing))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.update(id, updated, accountId);

        assertNull(existing.getOriginalDescription());
        assertEquals("New simp", existing.getSimplifiedDescription());
        assertSame(updated.getCategory(), existing.getCategory());
        assertSame(account, existing.getAccount());
        assertEquals("old-normalized", existing.getNormalizedDescription(),
                "normalizedDescription must NOT be recomputed when originalDescription is null");

        assertSame(existing, result);

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, times(1)).findById(accountId);
        verify(repository, times(1)).save(same(existing));

        verify(transactionService, times(1)).applyMappingToExistingTransactions(
                eq(accountId),
                isNull(),
                eq(existing.getSimplifiedDescription()),
                eq(existing.getCategory())
        );
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }


    @Test
    void findAll_whenOnlyIncompleteTrue_filtersByIsIncomplete() {
        var accountId = 10L;

        var incomplete = mock(DescriptionMapping.class);
        when(incomplete.isIncomplete()).thenReturn(true);

        var complete = mock(DescriptionMapping.class);
        when(complete.isIncomplete()).thenReturn(false);

        when(repository.findAllByAccountId(accountId)).thenReturn(List.of(incomplete, complete));

        var result = service.findAll(accountId, true);

        assertEquals(1, result.size());
        assertSame(incomplete, result.getFirst());

        verify(repository, times(1)).findAllByAccountId(accountId);
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void findAll_whenOnlyIncompleteFalse_returnsAll() {
        var accountId = 10L;
        var m1 = new DescriptionMapping();
        var m2 = new DescriptionMapping();

        when(repository.findAllByAccountId(accountId)).thenReturn(List.of(m1, m2));

        var result = service.findAll(accountId, false);

        assertEquals(2, result.size());
        assertSame(m1, result.get(0));
        assertSame(m2, result.get(1));

        verify(repository, times(1)).findAllByAccountId(accountId);
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void findById_delegatesToRepository() {
        var id = 30L;
        var accountId = 10L;
        var mapping = new DescriptionMapping();
        mapping.setId(id);

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(mapping));

        var result = service.findById(id, accountId);

        assertTrue(result.isPresent());
        assertSame(mapping, result.get());

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void findByNormalizedDescription_normalizes_thenDelegates() {
        var accountId = 10L;
        var input = "  Lojão ABC 10/12  ";
        var normalized = NormalizationUtils.normalizeDescription(input);

        var mapping = new DescriptionMapping();
        when(repository.findByNormalizedDescriptionAndAccountId(normalized, accountId)).thenReturn(Optional.of(mapping));

        var result = service.findByNormalizedDescription(input, accountId);

        assertTrue(result.isPresent());
        assertSame(mapping, result.get());

        verify(repository, times(1)).findByNormalizedDescriptionAndAccountId(normalized, accountId);
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void delete_whenFound_deletesMapping() {
        var id = 40L;
        var accountId = 10L;

        var mapping = new DescriptionMapping();
        mapping.setId(id);

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(mapping));

        assertDoesNotThrow(() -> service.delete(id, accountId));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(repository, times(1)).delete(same(mapping));
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFound_andDoesNotDelete() {
        var id = 41L;
        var accountId = 10L;

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id, accountId));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(repository, never()).delete(any());
        verifyNoMoreInteractions(repository, accountService, transactionService);
    }
}