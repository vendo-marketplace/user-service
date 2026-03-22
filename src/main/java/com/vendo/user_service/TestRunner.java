package com.vendo.user_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRunner implements ApplicationRunner {
    @Value("${spring.data.mongodb.uri}")
    private String URL;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("URL lol: " + URL);
    }
}
