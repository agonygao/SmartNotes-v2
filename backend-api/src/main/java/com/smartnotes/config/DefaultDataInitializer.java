package com.smartnotes.config;

import com.smartnotes.service.WordBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDataInitializer implements CommandLineRunner {

    private final WordBookService wordBookService;

    @Override
    public void run(String... args) {
        log.info("Initializing default word books (CET4/CET6)...");
        try {
            wordBookService.initializeDefaultWordBooks(null);
            log.info("Default word books initialization completed.");
        } catch (Exception e) {
            log.warn("Default word books may already exist or initialization failed: {}", e.getMessage());
        }
    }
}
