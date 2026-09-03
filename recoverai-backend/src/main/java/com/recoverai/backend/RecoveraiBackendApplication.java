package com.recoverai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecoveraiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecoveraiBackendApplication.class, args);
    }
}