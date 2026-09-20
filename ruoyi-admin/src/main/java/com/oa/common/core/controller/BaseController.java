package com.oa.common.core.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.security.utils.SecurityUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class BaseController {

    protected void startPage() {
        PageHelper.startPage(getPageNum(), getPageSize());
    }

    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo<>();
        PageInfo<?> pageInfo = new PageInfo<>(list);
        rspData.setTotal(pageInfo.getTotal());
        rspData.setRows(pageInfo.getList());
        rspData.setCode(200);
        rspData.setMsg("查询成功");
        return rspData;
    }

    protected AjaxResult toAjax(int rows) {
        return AjaxResult.toAjax(rows);
    }

    protected AjaxResult toAjax(boolean flag) {
        return AjaxResult.toAjax(flag);
    }

    public int getPageNum() {
        return getIntParameter("pageNum", 1);
    }

    public int getPageSize() {
        return getIntParameter("pageSize", 10);
    }

    private int getIntParameter(String name, int defaultValue) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return defaultValue;
        }
        HttpServletRequest request = attributes.getRequest();
        String value = request.getParameter(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    protected Long getUserId() {
        return SecurityUtils.getUserId();
    }

    protected String getUsername() {
        return SecurityUtils.getUsername();
    }
}