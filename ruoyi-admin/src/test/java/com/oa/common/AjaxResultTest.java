package com.oa.common;

import com.oa.common.core.result.AjaxResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AjaxResultTest {

    @Test
    void successAndFailureShouldUseExpectedCodes() {
        assertEquals(200, AjaxResult.success().get(AjaxResult.CODE_TAG));
        assertEquals(500, AjaxResult.error().get(AjaxResult.CODE_TAG));
        assertEquals(200, AjaxResult.toAjax(true).get(AjaxResult.CODE_TAG));
        assertEquals(500, AjaxResult.toAjax(0).get(AjaxResult.CODE_TAG));
    }
}