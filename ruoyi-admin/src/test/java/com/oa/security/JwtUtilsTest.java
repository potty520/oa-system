package com.oa.security;

import com.oa.security.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilsTest {

    @Test
    void tokenShouldContainUsernameAsSubject() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "OA-SYSTEM-TEST-SECRET-KEY-1234567890");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 60000L);
        jwtUtils.init();

        String token = jwtUtils.createToken("admin", 1L);

        assertEquals("admin", jwtUtils.getUsernameFromToken(token));
        assertEquals(1L, jwtUtils.getUserIdFromToken(token));
    }
}