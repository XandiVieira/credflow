package com.relyon.credflow.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_whenAccountsExist_skipsSeeding() {
        when(accountRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(accountRepository).count();
        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void run_whenNoAccountsExist_seedsData() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            var account = invocation.getArgument(0, Account.class);
            account.setId(1L);
            return account;
        });

        dataSeeder.run();

        verify(accountRepository).count();
        verify(accountRepository).save(any(Account.class));
        verify(userRepository).saveAll(anyList());
        verify(categoryRepository).saveAll(anyList());
    }

    @Test
    void run_createsAccountWithCorrectData() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        var accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(accountCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        var savedAccount = accountCaptor.getValue();
        assertEquals("Conta Familiar", savedAccount.getName());
        assertEquals("Conta compartilhada da família", savedAccount.getDescription());
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_createsTwoUsers() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        var usersCaptor = ArgumentCaptor.forClass(List.class);
        when(userRepository.saveAll(usersCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        List<User> savedUsers = usersCaptor.getValue();
        assertEquals(2, savedUsers.size());

        var user1 = savedUsers.get(0);
        assertEquals("Alexandre", user1.getName());
        assertEquals("xandivieira@outlook.com", user1.getEmail());
        assertEquals("encoded-password", user1.getPassword());

        var user2 = savedUsers.get(1);
        assertEquals("Polyana", user2.getName());
        assertEquals("polyana@gmail.com", user2.getEmail());
        assertEquals("encoded-password", user2.getPassword());
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_createsExpectedCategories() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        var categoriesCaptor = ArgumentCaptor.forClass(List.class);
        when(categoryRepository.saveAll(categoriesCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        List<Category> savedCategories = categoriesCaptor.getValue();
        assertEquals(19, savedCategories.size());

        var categoryNames = savedCategories.stream().map(Category::getName).toList();
        assertTrue(categoryNames.contains("Mercadinho"));
        assertTrue(categoryNames.contains("Mercado"));
        assertTrue(categoryNames.contains("Pets"));
        assertTrue(categoryNames.contains("Assinaturas"));
        assertTrue(categoryNames.contains("Lazer"));
        assertTrue(categoryNames.contains("Saúde"));
        assertTrue(categoryNames.contains("Delivery"));
        assertTrue(categoryNames.contains("Transporte"));
        assertTrue(categoryNames.contains("Viagem"));
        assertTrue(categoryNames.contains("Compras Online"));
        assertTrue(categoryNames.contains("Impostos ou Taxas"));
        assertTrue(categoryNames.contains("Profissional"));
        assertTrue(categoryNames.contains("Bares e Restaurantes"));
        assertTrue(categoryNames.contains("Vestuário"));
        assertTrue(categoryNames.contains("Vestimenta"));
        assertTrue(categoryNames.contains("Beleza e Cuidados Pessoais"));
        assertTrue(categoryNames.contains("Compras"));
        assertTrue(categoryNames.contains("Festas e Rolês"));
        assertTrue(categoryNames.contains("Não Identificado"));
    }

    @Test
    void run_usersAreLinkedToAccount() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        var savedAccount = new Account();
        savedAccount.setId(1L);
        savedAccount.setName("Test");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        var usersCaptor = ArgumentCaptor.forClass(List.class);
        when(userRepository.saveAll(usersCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        @SuppressWarnings("unchecked")
        List<User> users = usersCaptor.getValue();
        for (var user : users) {
            assertNotNull(user.getAccount());
        }
    }

    @Test
    void run_categoriesAreLinkedToAccount() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        var categoriesCaptor = ArgumentCaptor.forClass(List.class);
        when(categoryRepository.saveAll(categoriesCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        @SuppressWarnings("unchecked")
        List<Category> categories = categoriesCaptor.getValue();
        for (var category : categories) {
            assertNotNull(category.getAccount());
        }
    }

    @Test
    void run_passwordEncoderIsCalledForEachUser() {
        when(accountRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        verify(passwordEncoder, times(2)).encode("123456");
    }

    @Test
    void run_withArgs_ignoresArgs() {
        when(accountRepository.count()).thenReturn(5L);

        dataSeeder.run("arg1", "arg2", "arg3");

        verify(accountRepository).count();
        verify(accountRepository, never()).save(any());
    }
}
