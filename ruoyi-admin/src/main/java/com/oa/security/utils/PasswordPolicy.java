package com.oa.security.utils;

import java.util.regex.Pattern;

/**
 * 口令强度校验：长度 8-64 位，且必须同时包含字母与数字。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");

    private PasswordPolicy() {
    }

    /** 校验通过返回 null，否则返回可直接展示给用户的中文原因。 */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return "密码长度需为 " + MIN_LENGTH + "-" + MAX_LENGTH + " 位";
        }
        if (!LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
            return "密码需同时包含字母和数字";
        }
        return null;
    }
}
