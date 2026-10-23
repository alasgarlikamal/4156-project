package com.perihelionApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Perihelion API.
 */
@SpringBootApplication
public final class PerihelionApiApplication {

    static {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not create the SQLite data directory", exception);
        }
    }

    /** Private constructor to prevent instantiation. */
    private PerihelionApiApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(PerihelionApiApplication.class, args);
    }

}
