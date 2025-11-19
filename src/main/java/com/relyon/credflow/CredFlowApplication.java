package com.relyon.credflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CredFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CredFlowApplication.class, args);
    }
}