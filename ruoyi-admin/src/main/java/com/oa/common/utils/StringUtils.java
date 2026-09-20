package com.oa.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\.(jpg|jpeg|png|gif|bmp|webp|svg)$", Pattern.CASE_INSENSITIVE);

    /** 空字符串 */
    private static final String NULL_STR = "";

    /** 下划线 */
    private static final char SEPARATOR = '_';

    /**
     * 判断一个对象是否为空
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * 判断一个对象是否非空
     */
    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    /**
     * 判断一个 CharSequence 是否为空
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * 判断一个 CharSequence 是否非空
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * 判断一个对象是否为空字符串
     */
    public static boolean isEmptyString(Object str) {
        return isNull(str) || NULL_STR.equals(str.toString().trim());
    }

    /**
     * 截取字符串
     */
    public static String substring(final String str, int start, int end) {
        if (str == null) {
            return NULL_STR;
        }
        if (end < 0) {
            end = str.length() + end;
        }
        if (start < 0) {
            start = str.length() + start;
        }
        if (end > str.length()) {
            end = str.length();
        }
        if (start > end) {
            return NULL_STR;
        }
        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }
        return str.substring(start, end);
    }

    /**
     * 格式化文本，使用 {} 占位符
     */
    public static String format(String template, Object... params) {
        if (isEmpty(template) || params == null || params.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template);
        for (Object param : params) {
            int index = sb.indexOf("{}");
            if (index == -1) {
                break;
            }
            sb.replace(index, index + 2, String.valueOf(param));
        }
        return sb.toString();
    }

    /**
     * 驼峰转下划线命名
     */
    public static String toUnderScoreCase(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean preCharIsUpperCase;
        boolean curCharIsUpperCase;
        boolean nextCharIsUpperCase;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (i > 0) {
                preCharIsUpperCase = Character.isUpperCase(str.charAt(i - 1));
            } else {
                preCharIsUpperCase = false;
            }
            curCharIsUpperCase = Character.isUpperCase(c);
            if (i < (str.length() - 1)) {
                nextCharIsUpperCase = Character.isUpperCase(str.charAt(i + 1));
            } else {
                nextCharIsUpperCase = false;
            }
            if (preCharIsUpperCase && curCharIsUpperCase && !nextCharIsUpperCase) {
                sb.append(SEPARATOR);
            } else if ((i != 0 && !preCharIsUpperCase) && curCharIsUpperCase) {
                sb.append(SEPARATOR);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰
     */
    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }
        if (!s.contains("_")) {
            return s;
        }
        s = s.toLowerCase();
        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 是否包含图片后缀
     */
    public static boolean isImageFile(String fileName) {
        if (isEmpty(fileName)) {
            return false;
        }
        Matcher matcher = IMAGE_PATTERN.matcher(fileName);
        return matcher.find();
    }
}
