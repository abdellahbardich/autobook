package com.autobook.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrchestrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestrationApplication.class, args);
    }
}