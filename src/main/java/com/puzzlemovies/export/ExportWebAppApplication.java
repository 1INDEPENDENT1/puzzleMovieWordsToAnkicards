package com.puzzlemovies.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExportWebAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExportWebAppApplication.class, args);
    }
}
