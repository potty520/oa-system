package com.oa.common.core.result;

import java.util.HashMap;
import java.util.Map;

public class AjaxResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "msg";
    public static final String DATA_TAG = "data";
    public static final String COUNT_TAG = "count";

    public AjaxResult() {}

    public AjaxResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    public AjaxResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (data != null) {
            super.put(DATA_TAG, data);
        }
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功");
    }

    public static AjaxResult success(String msg) {
        return new AjaxResult(200, msg);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult error() {
        return new AjaxResult(500, "操作失败");
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg);
    }

    public static AjaxResult toAjax(boolean flag) {
        return flag ? success() : error();
    }

    public static AjaxResult toAjax(int rows) {
        return rows > 0 ? success("操作成功") : error("操作失败");
    }
}
