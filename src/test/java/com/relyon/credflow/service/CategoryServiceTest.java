package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryService service;

    private static User user(Long id, String name) {
        var u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private static Set<User> set(User... users) {
        return new HashSet<>(Arrays.asList(users));
    }

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

    @Test
    void findById_whenPresent_returnsOptionalWithSameInstance() {
        var id = 5L;
        var accountId = 10L;
        var cat = new Category();
        cat.setId(id);
        cat.setName("Food");

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(cat));

        var result = service.findById(id, accountId);

        assertNotNull(result);
        assertSame(cat, result);

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void findById_whenMissing_throwsResourceNotFound() {
        var id = 6L;
        var accountId = 10L;

        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> service.findById(id, accountId));

        assertTrue(ex.getMessage().contains("Category with ID " + id + " not found."));

        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verifyNoMoreInteractions(repository, accountService);
    }

    @Test
    void create_whenUnique_setsAccount_saves_andReturnsSaved() {
        var accountId = 20L;

        var input = new Category();
        input.setName("  Food  ");
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

    @Test
    void update_whenNoNameConflict_updatesFields_setsAccount_andSaves() {
        var id = 50L;
        var accountId = 30L;

        // incoming update (users in the payload are stubs with only ids)
        var updated = new Category();
        updated.setName("  Transport  ");
        var john = user(1L, "John");                 // helper that creates a stub user (no account)
        var updatedDefaults = set(john);
        updated.setDefaultResponsibles(updatedDefaults);

        var existing = new Category();
        existing.setId(id);
        existing.setName("Old");
        var oldUser = user(2L, "Old");
        existing.setDefaultResponsibles(set(oldUser));

        var account = new Account();
        account.setId(accountId);

        when(repository.findByNameIgnoreCaseAndAccountId("transport", accountId)).thenReturn(Optional.empty());
        when(repository.findByIdAndAccountId(id, accountId)).thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(account);

        // repository save
        var saved = new Category();
        saved.setId(id);
        saved.setName("Transport");
        saved.setDefaultResponsibles(updatedDefaults);
        when(repository.save(same(existing))).thenReturn(saved);

        // IMPORTANT: users resolved by the service must carry the same account
        var resolvedJohn = new User();
        resolvedJohn.setId(1L);
        resolvedJohn.setName("John");
        resolvedJohn.setAccount(account);
        when(userService.findById(1L)).thenReturn(resolvedJohn);

        // (optional: only needed if your service resolves existing users too)
        var resolvedOld = new User();
        resolvedOld.setId(2L);
        resolvedOld.setName("Old");
        resolvedOld.setAccount(account);

        var result = service.update(id, updated, accountId);

        assertSame(saved, result);
        assertEquals("Transport", existing.getName());
        assertSame(account, existing.getAccount());

        // assert content, not identity (service may create a new Set instance)
        var resultingIds = existing.getDefaultResponsibles().stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(java.util.Set.of(1L), resultingIds);

        verify(repository).findByNameIgnoreCaseAndAccountId("transport", accountId);
        verify(repository).findByIdAndAccountId(id, accountId);
        verify(accountService).findById(accountId);
        verify(userService).findById(1L);
        verify(repository).save(same(existing));
        verifyNoMoreInteractions(repository, accountService, userService);
    }

    @Test
    void update_whenNameBelongsToSameEntity_doesNotTreatAsConflict_andSaves() {
        var id = 51L;
        var accountId = 30L;

        // incoming update (stub users carry only IDs)
        var updated = new Category();
        updated.setName("Home");
        var janeStub = new User();
        janeStub.setId(3L);
        var updatedDefaults = new java.util.HashSet<User>();
        updatedDefaults.add(janeStub);
        updated.setDefaultResponsibles(updatedDefaults);

        var existingSame = new Category();
        existingSame.setId(id);
        existingSame.setName("Home");

        var account = new Account();
        account.setId(accountId);

        when(repository.findByNameIgnoreCaseAndAccountId("home", accountId))
                .thenReturn(Optional.of(existingSame)); // same entity -> no conflict
        when(repository.findByIdAndAccountId(id, accountId))
                .thenReturn(Optional.of(existingSame));
        when(accountService.findById(accountId)).thenReturn(account);

        // userService must return a user that belongs to the same account
        var resolvedJane = new User();
        resolvedJane.setId(3L);
        resolvedJane.setName("Jane");
        resolvedJane.setAccount(account); // <-- IMPORTANT: prevent NPE in service
        when(userService.findById(3L)).thenReturn(resolvedJane);

        when(repository.save(same(existingSame))).thenReturn(existingSame);

        var result = service.update(id, updated, accountId);

        // same entity returned
        assertSame(existingSame, result);
        assertEquals("Home", existingSame.getName());
        assertSame(account, existingSame.getAccount());

        // the service likely replaces the set instance; assert content, not identity
        assertEquals(1, existingSame.getDefaultResponsibles().size());
        assertTrue(existingSame.getDefaultResponsibles()
                .stream().anyMatch(u -> u.getId().equals(3L)));

        verify(repository, times(1)).findByNameIgnoreCaseAndAccountId("home", accountId);
        verify(repository, times(1)).findByIdAndAccountId(id, accountId);
        verify(accountService, times(1)).findById(accountId);
        verify(userService, times(1)).findById(3L);
        verify(repository, times(1)).save(same(existingSame));
        verifyNoMoreInteractions(repository, accountService, userService);
    }

    @Test
    void update_whenNameConflictWithDifferentEntity_throws_andDoesNotSave() {
        var id = 52L;
        var accountId = 30L;

        var updated = new Category();
        updated.setName("Bills");

        var conflicting = new Category();
        conflicting.setId(999L);

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