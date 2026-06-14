package com.puzzlemovies.export.config;

import org.drugov.lingua.morph.Lemmatizer;
import org.drugov.lingua.morph.LuceneLemmatizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LemmatizerConfig {
    @Bean
    public Lemmatizer lemmatizer() {
        return new LuceneLemmatizer();
    }
}
