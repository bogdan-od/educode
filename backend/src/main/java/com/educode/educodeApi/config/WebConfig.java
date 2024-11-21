package com.educode.educodeApi.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // Конфігурація для Spring Boot додатку
@EnableJpaRepositories(basePackages = "com.educode.educodeApi.repositories") // Вказує на пакети, де знаходяться JPA репозиторії
@EntityScan(basePackages = "com.educode.educodeApi.models") // Вказує на пакети, де знаходяться JPA моделі
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) { // Налаштування CORS для всього додатку
        registry.addMapping("/**") // Дозволити всі шляхи
                .allowedOrigins("*") // Дозволити всі джерела
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Дозволити всі основні HTTP методи
                .allowedHeaders("*"); // Дозволити всі заголовки
    }
}