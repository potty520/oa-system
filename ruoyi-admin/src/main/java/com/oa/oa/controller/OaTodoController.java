package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaTodo;
import com.oa.oa.service.ITodoService;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/todo")
public class OaTodoController extends BaseController {

    @Autowired
    private ITodoService todoService;

    @PreAuthorize("@ss.hasPermi('oa:todo:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaTodo todo) {
        startPage();
        Long userId = SecurityUtils.getUserId();
        List<OaTodo> list = todoService.selectTodoList(userId, SecurityUtils.getUsername(), todo.getTodoTitle(), todo.getStatus());
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:todo:list')")
    @GetMapping("/count")
    public AjaxResult count() {
        Long userId = SecurityUtils.getUserId();
        int count = todoService.selectTodoCount(userId);
        return AjaxResult.success(count);
    }

    @PreAuthorize("@ss.hasPermi('oa:todo:complete')")
    @PutMapping("/complete")
    public AjaxResult complete(@RequestParam String todoType, @RequestParam Long relatedId) {
        todoService.completeTodo(todoType, relatedId);
        return AjaxResult.success();
    }
}