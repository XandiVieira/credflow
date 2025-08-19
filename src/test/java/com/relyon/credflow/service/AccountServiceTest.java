package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void findAll_whenRepositoryReturnsAccounts_returnsSameAccountsAndOnlyReadsOnce() {
        var a1 = new Account();
        a1.setName("Main");
        a1.setDescription("Primary");

        var a2 = new Account();
        a2.setName("Savings");
        a2.setDescription("Secondary");

        var repoResult = List.of(a1, a2);
        when(accountRepository.findAll()).thenReturn(repoResult);

        var result = accountService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(a1, result.get(0));
        assertSame(a2, result.get(1));

        verify(accountRepository, times(1)).findAll();
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void findAll_whenRepositoryReturnsEmpty_returnsEmptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        var result = accountService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(accountRepository, times(1)).findAll();
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void findById_whenAccountExists_returnsAccount() {
        var id = 42L;
        var account = new Account();
        account.setName("Existing");
        account.setDescription("Exists in repo");

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        var result = accountService.findById(id);

        assertNotNull(result);
        assertSame(account, result);

        verify(accountRepository, times(1)).findById(id);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void findById_whenAccountDoesNotExist_throwsResourceNotFound() {
        var id = 99L;
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> accountService.findById(id));
        assertTrue(ex.getMessage().contains("Account not found with ID " + id));

        verify(accountRepository, times(1)).findById(id);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void findByIdOptional_whenAccountExists_returnsPresentOptionalWithSameInstance() {
        var id = 7L;
        var account = new Account();
        account.setName("Opt");
        account.setDescription("Present");

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        var result = accountService.findByIdOptional(id);

        assertTrue(result.isPresent());
        assertSame(account, result.get());

        verify(accountRepository, times(1)).findById(id);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void findByIdOptional_whenAccountDoesNotExist_returnsEmptyOptional() {
        var id = 8L;
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        var result = accountService.findByIdOptional(id);

        assertTrue(result.isEmpty());

        verify(accountRepository, times(1)).findById(id);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void create_whenValidAccount_callsSaveWithSameInstance_andReturnsSavedWithId() {
        var input = new Account();
        input.setName("New");
        input.setDescription("To create");

        var saved = new Account();
        saved.setName("New");
        saved.setDescription("To create");
        // emulate persistence assigning an ID
        saved.setId(1L);

        when(accountRepository.save(same(input))).thenReturn(saved);

        var result = accountService.create(input);

        assertNotNull(result);
        assertSame(saved, result);
        assertEquals(1L, result.getId());
        assertEquals("New", result.getName());
        assertEquals("To create", result.getDescription());

        verify(accountRepository, times(1)).save(same(input));
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void create_whenRepositoryThrows_propagatesException() {
        var input = new Account();
        input.setName("Break me");
        input.setDescription("Should throw");

        when(accountRepository.save(same(input))).thenThrow(new RuntimeException("DB down"));

        var ex = assertThrows(RuntimeException.class, () -> accountService.create(input));
        assertTrue(ex.getMessage().contains("DB down"));

        verify(accountRepository, times(1)).save(same(input));
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void createDefaultFor_whenUserHasName_buildsExpectedFields_andSavesOnce_andReturnsSaved() {
        var user = new User();
        user.setName("Alex");
        user.setEmail("alex@example.com");

        var captor = ArgumentCaptor.forClass(Account.class);

        var saved = new Account();
        saved.setId(123L);
        saved.setName("Alex Finanças");
        saved.setDescription("Finanças do(a) Alex");

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        var result = accountService.createDefaultFor(user);

        verify(accountRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(accountRepository);

        var built = captor.getValue();
        assertNotNull(built);
        assertEquals("Alex Finanças", built.getName());
        assertEquals("Finanças do(a) Alex", built.getDescription());

        assertSame(saved, result);
        assertEquals(123L, result.getId());
    }

    @Test
    void createDefaultFor_whenUserNameNullOrBlank_usesGenericLabels() {
        var user = new User();
        user.setName("   ");
        user.setEmail("no-name@example.com");

        var captor = ArgumentCaptor.forClass(Account.class);

        var saved = new Account();
        saved.setId(10L);
        saved.setName("Finanças");
        saved.setDescription("Finanças");

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        var result = accountService.createDefaultFor(user);

        verify(accountRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(accountRepository);

        var built = captor.getValue();
        assertEquals("Finanças", built.getName());
        assertEquals("Finanças", built.getDescription());

        assertSame(saved, result);
        assertEquals(10L, result.getId());
    }

    @Test
    void createDefaultFor_methodHasTransactionalAnnotation() throws NoSuchMethodException {
        Method m = AccountService.class.getDeclaredMethod("createDefaultFor", User.class);
        assertTrue(m.isAnnotationPresent(Transactional.class),
                "@Transactional is expected on createDefaultFor(User)");
    }

    @Test
    void createDefaultFor_whenUserNameIsNull_usesGenericLabels() {
        var user = new User();
        user.setName(null);
        user.setEmail("null-name@example.com");

        var captor = ArgumentCaptor.forClass(Account.class);

        var saved = new Account();
        saved.setId(11L);
        saved.setName("Finanças");
        saved.setDescription("Finanças");

        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        var result = accountService.createDefaultFor(user);

        verify(accountRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(accountRepository);

        var built = captor.getValue();
        assertEquals("Finanças", built.getName());
        assertEquals("Finanças", built.getDescription());

        assertSame(saved, result);
        assertEquals(11L, result.getId());
    }


    @Test
    void update_whenAccountExists_updatesFields_andSavesOnce_returnsSaved() {
        var id = 5L;

        var existing = new Account();
        existing.setId(id);
        existing.setName("Old");
        existing.setDescription("Old desc");

        var patch = new Account();
        patch.setName("New");
        patch.setDescription("New desc");

        var saved = new Account();
        saved.setId(id);
        saved.setName("New");
        saved.setDescription("New desc");

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        var result = accountService.update(id, patch);

        // ensure repo interactions
        verify(accountRepository, times(1)).findById(id);
        var captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(accountRepository);

        // ensure we updated the same entity and preserved the id
        var toSave = captor.getValue();
        assertSame(existing, toSave);
        assertEquals(id, toSave.getId());
        assertEquals("New", toSave.getName());
        assertEquals("New desc", toSave.getDescription());

        // ensure return is repo's saved instance
        assertSame(saved, result);
    }

    @Test
    void update_whenAccountDoesNotExist_throwsResourceNotFound_andDoesNotSave() {
        var id = 404L;

        var patch = new Account();
        patch.setName("New");
        patch.setDescription("New desc");

        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class, () -> accountService.update(id, patch));
        assertTrue(ex.getMessage().contains("Account not found with ID " + id));

        verify(accountRepository, times(1)).findById(id);
        verify(accountRepository, never()).save(any());
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void update_whenUpdatedHasNulls_allowsNullOverwrite() {
        var id = 9L;

        var existing = new Account();
        existing.setId(id);
        existing.setName("Keep?");
        existing.setDescription("Will be nulled");

        var patch = new Account();
        patch.setName("Keep?");
        patch.setDescription(null); // explicitly null

        var saved = new Account();
        saved.setId(id);
        saved.setName("Keep?");
        saved.setDescription(null);

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        var result = accountService.update(id, patch);

        verify(accountRepository, times(1)).findById(id);
        var captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(accountRepository);

        var toSave = captor.getValue();
        assertEquals("Keep?", toSave.getName());
        assertNull(toSave.getDescription());
        assertSame(saved, result);
    }

    @Test
    void delete_whenAccountExists_callsDeleteById_once_inOrder() {
        var id = 3L;
        when(accountRepository.existsById(id)).thenReturn(true);

        assertDoesNotThrow(() -> accountService.delete(id));

        var inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).existsById(id);
        inOrder.verify(accountRepository).deleteById(id);
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void delete_whenAccountDoesNotExist_throwsResourceNotFound_andDoesNotDelete() {
        var id = 404L;
        when(accountRepository.existsById(id)).thenReturn(false);

        var ex = assertThrows(ResourceNotFoundException.class, () -> accountService.delete(id));
        assertTrue(ex.getMessage().contains("Account not found with ID " + id));

        verify(accountRepository, times(1)).existsById(id);
        verify(accountRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void delete_whenRepositoryDeleteFails_propagatesException() {
        var id = 7L;
        when(accountRepository.existsById(id)).thenReturn(true);
        doThrow(new RuntimeException("DB failure")).when(accountRepository).deleteById(id);

        var ex = assertThrows(RuntimeException.class, () -> accountService.delete(id));
        assertTrue(ex.getMessage().contains("DB failure"));

        verify(accountRepository, times(1)).existsById(id);
        verify(accountRepository, times(1)).deleteById(id);
        verifyNoMoreInteractions(accountRepository);
    }
}