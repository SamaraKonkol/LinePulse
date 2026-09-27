package com.linepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinePulseApplication {
    public static void main(String[] args) {
        SpringApplication.run(LinePulseApplication.class, args);
    }
}
