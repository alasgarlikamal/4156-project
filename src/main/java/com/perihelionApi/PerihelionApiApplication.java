package com.perihelionApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PerihelionApiApplication {

	static {
		try {
			Files.createDirectories(Path.of("data"));
		} catch (IOException exception) {
			throw new IllegalStateException("Could not create the SQLite data directory", exception);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(PerihelionApiApplication.class, args);
	}

}
