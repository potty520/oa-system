package com.oa.security;

import com.oa.common.constant.Constants;
import com.oa.security.utils.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 保证仓库里不再存在任何可用的内置口令。
 */
class InitialAccountPasswordTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void seededPlaceholderShouldNotMatchAnyPassword() {
        assertFalse(encoder.matches("admin123", Constants.PENDING_PASSWORD));
        assertFalse(encoder.matches("Admin@123456", Constants.PENDING_PASSWORD));
        assertFalse(encoder.matches(Constants.PENDING_PASSWORD, Constants.PENDING_PASSWORD));
        // 占位符必须不是 BCrypt 串，否则就有被猜中的可能
        assertFalse(Constants.PENDING_PASSWORD.startsWith("$2"));
    }

    @Test
    void passwordPolicyShouldRejectWeakPasswords() {
        assertNotNull(PasswordPolicy.validate(null));
        assertNotNull(PasswordPolicy.validate(""));
        assertNotNull(PasswordPolicy.validate("Ab1"));
        assertNotNull(PasswordPolicy.validate("abcdefgh"));
        assertNotNull(PasswordPolicy.validate("12345678"));
        StringBuilder tooLong = new StringBuilder("a1");
        while (tooLong.length() <= PasswordPolicy.MAX_LENGTH) {
            tooLong.append("a1");
        }
        assertNotNull(PasswordPolicy.validate(tooLong.toString()));
    }

    @Test
    void passwordPolicyShouldAcceptCompliantPasswords() {
        assertNull(PasswordPolicy.validate("Abcdef12"));
        assertNull(PasswordPolicy.validate("oa-System-2026"));
        StringBuilder shortest = new StringBuilder();
        while (shortest.length() < PasswordPolicy.MIN_LENGTH) {
            shortest.append("a1");
        }
        assertEquals(PasswordPolicy.MIN_LENGTH, shortest.length());
        assertNull(PasswordPolicy.validate(shortest.toString()));
    }
}
