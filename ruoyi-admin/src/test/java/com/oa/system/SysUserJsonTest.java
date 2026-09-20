package com.oa.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.system.domain.SysUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysUserJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseFrontendFieldNamesWithoutLeakingPassword() throws Exception {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setNickname("超级管理员");
        user.setPhone("13800000000");
        user.setPassword("secret");

        String json = objectMapper.writeValueAsString(user);
        assertTrue(json.contains("\"userName\":\"admin\""));
        assertTrue(json.contains("\"nickName\":\"超级管理员\""));
        assertTrue(json.contains("\"phonenumber\":\"13800000000\""));
        assertFalse(json.contains("\"password\""));

        SysUser parsed = objectMapper.readValue(
                "{\"userName\":\"tester\",\"nickName\":\"测试用户\",\"phonenumber\":\"13900000000\"}",
                SysUser.class
        );
        assertEquals("tester", parsed.getUsername());
        assertEquals("测试用户", parsed.getNickname());
        assertEquals("13900000000", parsed.getPhone());
    }
}