package com.oa.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InitialAccountInitializerTest {

    @Test
    void envKeyShouldBeDerivedFromUsername() {
        assertEquals("OA_INIT_ADMIN_PASSWORD", InitialAccountInitializer.envKeyFor("admin"));
        assertEquals("OA_INIT_ZHANGSAN_PASSWORD", InitialAccountInitializer.envKeyFor("zhangsan"));
        assertEquals("OA_INIT_LISI_PASSWORD", InitialAccountInitializer.envKeyFor("LiSi"));
        assertEquals("OA_INIT_OA_USER_1_PASSWORD", InitialAccountInitializer.envKeyFor("oa-user#1"));
    }
}
