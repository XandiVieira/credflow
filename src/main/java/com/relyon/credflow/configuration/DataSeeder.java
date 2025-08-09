package com.relyon.credflow.configuration;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Seed already ran, skipping.");
            return;
        }

        var account = new Account();
        account.setName("Conta Familiar");
        account.setDescription("Conta compartilhada da família");
        accountRepository.save(account);

        var user1 = new User();
        user1.setName("Alexandre");
        user1.setEmail("xandivieira@outlook.com");
        user1.setPassword(passwordEncoder.encode("123456"));
        user1.setAccount(account);

        var user2 = new User();
        user2.setName("Polyana");
        user2.setEmail("polyana@gmail.com");
        user2.setPassword(passwordEncoder.encode("123456"));
        user2.setAccount(account);

        userRepository.saveAll(List.of(user1, user2));

        var categories = Stream.of(
                "Mercadinho",
                "Mercado",
                "Pets",
                "Assinaturas",
                "Lazer",
                "Saúde",
                "Delivery",
                "Transporte",
                "Viagem",
                "Compras Online",
                "Impostos ou Taxas",
                "Profissional",
                "Bares e Restaurantes",
                "Vestuário",
                "Vestimenta",
                "Beleza e Cuidados Pessoais",
                "Compras",
                "Festas e Rolês",
                "Não Identificado"
        ).map(name -> new Category(name, account)).toList();

        categoryRepository.saveAll(categories);

        log.info("Initial seed created successfully.");
    }
}