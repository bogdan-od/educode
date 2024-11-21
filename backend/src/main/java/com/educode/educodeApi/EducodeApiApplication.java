package com.educode.educodeApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Головний клас додатку EduCode API
 * Відповідає за запуск Spring Boot застосунку та налаштування часової зони
 */
@SpringBootApplication
public class EducodeApiApplication {

	/**
	 * Головний метод, який запускає Spring Boot застосунок
	 * @param args аргументи командного рядка
	 */
	public static void main(String[] args) {
		// Встановлюємо часову зону для Києва
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
		// Запускаємо Spring Boot застосунок
		SpringApplication.run(EducodeApiApplication.class, args);
	}

}
