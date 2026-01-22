package com.relyon.credflow.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        var mailSender = mock(JavaMailSender.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        return mailSender;
    }
}
