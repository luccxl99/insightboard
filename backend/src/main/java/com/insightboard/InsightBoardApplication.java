package com.insightboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class InsightBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsightBoardApplication.class, args);
    }
}
