package com.oa.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPasswordTest {

    @Test
    void initializationPasswordShouldMatchReadme() {
        String hash = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";
        assertTrue(new BCryptPasswordEncoder().matches("admin123", hash));
    }
}