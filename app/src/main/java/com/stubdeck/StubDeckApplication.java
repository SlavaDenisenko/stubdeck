package com.stubdeck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class StubDeckApplication {

    static void main(String[] args) {
        SpringApplication.run(StubDeckApplication.class, args);
    }
}
