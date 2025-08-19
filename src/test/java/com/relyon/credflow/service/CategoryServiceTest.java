package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.repository.CategoryRepository;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private CategoryService service;

    // ------- findAll -------
    @Test
    void findAll_returnsRepositoryResult() {
        var accountId = 10L;

        var c1 = new Category();
        c1.setName("Food");
        var c2 = new Category();
        c2.setName("Transport");

        when(repository.findAllByAccountId(accountId)).thenReturn(List.of(c1, c2));

        var result = service.findAll(accountId);

        assertEquals(2, result.size());
        assertSame(c1, result.get(0));
        assertSame(c2, result.get(1));

        verify(repository, times(1)).findAllByAccountId(accountId);
        verifyNoMoreInteractions(repository, accountService);
    }

    // ------- findById -------
    @Test
    void findById_whenPresent_returnsOptionalWithSameInstance() {
        var id = 5L;
        var accountId = 10L;
        var cat = new Category();
        cat.setId(id);
        cat.setName("Food");

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(cat));

        var result = service.findById(id, accountId);

        assertTrue(result.isPresent());
        assertSame(cat, result.get());

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void findById_whenMissing_returnsEmptyOptional() {
        var id = 6L;
        var accountId = 10L;

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        var result = service.findById(id, accountId);

        assertTrue(result.isEmpty());

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verifyNoMoreInteractions(repository, accountService);
    }

    // ------- create -------
    @Test
    void create_whenUnique_setsAccount_saves_andReturnsSaved() {
        var accountId = 20L;

        var input = new Category();
        input.setName("  Food  "); // whitespace to exercise trim()
        var account = new Account();
        account.setId(accountId);

        var saved = new Category();
        saved.setId(99L);
        saved.setName("  Food  ");

        when(repository.findByNameIgnoreCaseAndAccountId("food", accountId)).thenReturn(Optional.empty());
        when(accountService.findById(accountId)).thenReturn(account);
        when(repository.save(same(input))).thenReturn(saved);

        var result = service.create(input, accountId);

        assertSame(saved, result);
        assertEquals(99L, result.getId());

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("food", accountId);
        verify(accountService, times(1)).findById(accountId);
        // ensure account was set on the same instance being saved
        assertSame(account, input.getAccount());
        verify(repository, times(1)).save(same(input));
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void create_whenNameAlreadyExists_throws_andDoesNotSave() {
        var accountId = 20L;

        var input = new Category();
        input.setName("Food");

        when(repository.findByNameIgnoreCaseAndAccountId("food", accountId))
                .thenReturn(Optional.of(new Category()));

        var ex = assertThrows(ResourceAlreadyExistsException.class, () -> service.create(input, accountId));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("food", accountId);
        verify(accountService, never()).findById(anyLong());
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository, accountService);
    }

    // ------- update -------
    @Test
    void update_whenNoNameConflict_updatesFields_setsAccount_andSaves() {
        var id = 50L;
        var accountId = 30L;

        var updated = new Category();
        updated.setName("  Transport  ");
        updated.setDefaultResponsible("John");

        var existing = new Category();
        existing.setId(id);
        existing.setName("Old");
        existing.setDefaultResponsible("Old");
        var account = new Account();
        account.setId(accountId);

        when(repository.findByNameIgnoreCaseAndAccountId("transport", accountId)).thenReturn(Optional.empty());
        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(account);

        var saved = new Category();
        saved.setId(id);
        saved.setName("Transport");
        saved.setDefaultResponsible("John");
        when(repository.save(same(existing))).thenReturn(saved);

        var result = service.update(id, updated, accountId);

        assertSame(saved, result);
        assertEquals("Transport", existing.getName()); // trimmed applied before save
        assertEquals("John", existing.getDefaultResponsible());
        assertSame(account, existing.getAccount());

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("transport", accountId);
        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, times(1)).findById(accountId);
        verify(repository, times(1)).save(same(existing));
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void update_whenNameBelongsToSameEntity_doesNotTreatAsConflict_andSaves() {
        var id = 51L;
        var accountId = 30L;

        var updated = new Category();
        updated.setName("Home");
        updated.setDefaultResponsible("Jane");

        var existingSame = new Category();
        existingSame.setId(id);
        existingSame.setName("Home");

        var account = new Account();
        account.setId(accountId);

        when(repository.findByNameIgnoreCaseAndAccountId("home", accountId))
                .thenReturn(Optional.of(existingSame)); // same id -> no conflict
        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existingSame));
        when(accountService.findById(accountId)).thenReturn(account);
        when(repository.save(same(existingSame))).thenReturn(existingSame);

        var result = service.update(id, updated, accountId);

        assertSame(existingSame, result);
        assertEquals("Home", existingSame.getName());
        assertEquals("Jane", existingSame.getDefaultResponsible());
        assertSame(account, existingSame.getAccount());

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("home", accountId);
        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, times(1)).findById(accountId);
        verify(repository, times(1)).save(same(existingSame));
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void update_whenNameConflictWithDifferentEntity_throws_andDoesNotSave() {
        var id = 52L;
        var accountId = 30L;

        var updated = new Category();
        updated.setName("Bills");

        var conflicting = new Category();
        conflicting.setId(999L); // different id -> conflict

        when(repository.findByNameIgnoreCaseAndAccountId("bills", accountId))
                .thenReturn(Optional.of(conflicting));

        var ex = assertThrows(ResourceAlreadyExistsException.class, () -> service.update(id, updated, accountId));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("bills", accountId);
        verify(repository, never()).findByIdAndAccountId(anyLong(), anyLong());
        verify(accountService, never()).findById(anyLong());
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void update_whenCategoryNotFound_throwsResourceNotFound() {
        var id = 53L;
        var accountId = 30L;

        var updated = new Category();
        updated.setName("Travel");

        when(repository.findByNameIgnoreCaseAndAccountId("travel", accountId)).thenReturn(Optional.empty());
        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.update(id, updated, accountId));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("travel", accountId);
        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, never()).findById(anyLong());
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository, accountService);
    }

    // ------- delete -------
    @Test
    void delete_whenFound_deletesCategory() {
        var id = 70L;
        var accountId = 40L;

        var cat = new Category();
        cat.setId(id);
        cat.setName("Delete me");

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(cat));

        assertDoesNotThrow(() -> service.delete(id, accountId));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(repository, times(1)).delete(same(cat));
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFound_andDoesNotDelete() {
        var id = 71L;
        var accountId = 40L;

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> service.delete(id, accountId));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(repository, never()).delete(any());
        verifyNoMoreInteractions(repository, accountService);
    }
}
