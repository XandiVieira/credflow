package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    void create_whenUserHasAccountId_usesGivenAccount_encodesPassword_andSaves() {
        var acc = new Account(); acc.setId(5L);
        var u = new User();
        u.setEmail("a@b.com");
        u.setPassword("raw");
        u.setAccount(acc);

        when(passwordEncoder.encode("raw")).thenReturn("enc");
        var saved = new User(); saved.setId(1L);
        when(userRepository.save(same(u))).thenReturn(saved);

        var res = service.create(u);

        assertSame(saved, res);
        assertEquals("enc", u.getPassword());
        verify(accountService, never()).createDefaultFor(any());
        verify(passwordEncoder, times(1)).encode("raw");
        verify(userRepository, times(1)).save(same(u));
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void create_whenNoAccountProvided_createsDefaultAccount_setsIt_encodes_andSaves() {
        var u = new User();
        u.setEmail("x@y.com");
        u.setPassword("p");

        var createdAcc = new Account(); createdAcc.setId(9L);
        when(accountService.createDefaultFor(same(u))).thenReturn(createdAcc);
        when(passwordEncoder.encode("p")).thenReturn("encP");
        var saved = new User(); saved.setId(2L);
        when(userRepository.save(same(u))).thenReturn(saved);

        var res = service.create(u);

        assertSame(saved, res);
        assertSame(createdAcc, u.getAccount());
        assertEquals("encP", u.getPassword());
        verify(accountService, times(1)).createDefaultFor(same(u));
        verify(passwordEncoder, times(1)).encode("p");
        verify(userRepository, times(1)).save(same(u));
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void create_methodHasTransactionalAnnotation() throws NoSuchMethodException {
        Method m = UserService.class.getDeclaredMethod("create", User.class);
        assertTrue(m.isAnnotationPresent(Transactional.class));
    }

    @Test
    void create_whenAccountPresentButIdNull_createsDefaultAccount_setsIt_encodes_andSaves() {
        var u = new User();
        u.setEmail("x@y.com");
        u.setPassword("p");
        u.setAccount(new Account()); // id null

        var createdAcc = new Account(); createdAcc.setId(77L);
        when(accountService.createDefaultFor(same(u))).thenReturn(createdAcc);
        when(passwordEncoder.encode("p")).thenReturn("enc");
        var saved = new User(); saved.setId(5L);
        when(userRepository.save(same(u))).thenReturn(saved);

        var res = service.create(u);

        assertSame(saved, res);
        assertSame(createdAcc, u.getAccount());
        assertEquals("enc", u.getPassword());
        verify(accountService, times(1)).createDefaultFor(same(u));
        verify(passwordEncoder, times(1)).encode("p");
        verify(userRepository, times(1)).save(same(u));
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void findAll_returnsRepositoryList() {
        var u1 = new User(); var u2 = new User();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        var res = service.findAll();

        assertEquals(2, res.size());
        assertSame(u1, res.get(0));
        assertSame(u2, res.get(1));
        verify(userRepository, times(1)).findAll();
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void findById_whenFound_returnsUser() {
        var u = new User();
        when(userRepository.findById(7L)).thenReturn(Optional.of(u));

        var res = service.findById(7L);

        assertSame(u, res);
        verify(userRepository, times(1)).findById(7L);
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void findById_whenMissing_throws() {
        when(userRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(8L));
        verify(userRepository, times(1)).findById(8L);
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void update_whenFound_updatesFields_andSaves() {
        var id = 3L;
        var existing = new User(); existing.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        var patch = new User();
        patch.setName("N");
        patch.setEmail("e@x.com");
        var acc = new Account(); acc.setId(1L);
        patch.setAccount(acc);

        var saved = new User(); saved.setId(id);
        when(userRepository.save(same(existing))).thenReturn(saved);

        var res = service.update(id, patch);

        assertSame(saved, res);
        assertEquals("N", existing.getName());
        assertEquals("e@x.com", existing.getEmail());
        assertSame(acc, existing.getAccount());
        verify(userRepository, times(1)).findById(id);
        verify(userRepository, times(1)).save(same(existing));
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void update_whenMissing_throws_andDoesNotSave() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());
        var patch = new User();
        assertThrows(ResourceNotFoundException.class, () -> service.update(4L, patch));
        verify(userRepository, times(1)).findById(4L);
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void delete_whenExists_deletes() {
        when(userRepository.existsById(10L)).thenReturn(true);
        assertDoesNotThrow(() -> service.delete(10L));
        verify(userRepository, times(1)).existsById(10L);
        verify(userRepository, times(1)).deleteById(10L);
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void delete_whenMissing_throws() {
        when(userRepository.existsById(11L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(11L));
        verify(userRepository, times(1)).existsById(11L);
        verify(userRepository, never()).deleteById(anyLong());
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void findByEmail_whenFound_returnsUser() {
        var u = new User();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        var res = service.findByEmail("a@b.com");

        assertSame(u, res);
        verify(userRepository, times(1)).findByEmail("a@b.com");
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }

    @Test
    void findByEmail_whenMissing_throws() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findByEmail("x@y.com"));
        verify(userRepository, times(1)).findByEmail("x@y.com");
        verifyNoMoreInteractions(userRepository, accountService, passwordEncoder);
    }
}
