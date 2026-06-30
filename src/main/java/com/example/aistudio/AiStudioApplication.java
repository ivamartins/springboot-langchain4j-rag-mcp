package com.example.aistudio;

// Spring Boot entry point — só isso, o resto é configurado via @Configuration.

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiStudioApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiStudioApplication.class, args);
    }
}
