package com.angellos.portfolio.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Portfolio Service Application
 * 
 * Tracks user portfolios, positions, and performance in real-time.
 * Consumes order events from Trading Service and updates portfolios accordingly.
 */
@SpringBootApplication
@EnableKafka
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}
