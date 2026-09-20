package com.oa.system.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysPost;
import com.oa.system.service.ISysPostService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/post")
public class SysPostController extends BaseController {
    @Autowired
    private ISysPostService postService;

    @PreAuthorize("@ss.hasPermi('system:post:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPost post) {
        startPage();
        List<SysPost> list = postService.selectPostList(post);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/{postId}")
    public AjaxResult getInfo(@PathVariable Long postId) {
        return AjaxResult.success(postService.getById(postId));
    }

    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public AjaxResult add(@RequestBody SysPost post) {
        if (StringUtils.isBlank(post.getPostCode()) || StringUtils.isBlank(post.getPostName())) {
            return AjaxResult.error(400, "岗位编码和岗位名称不能为空");
        }
        return toAjax(postService.save(post));
    }

    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody SysPost post) {
        return toAjax(postService.updateById(post));
    }

    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{postIds}")
    public AjaxResult remove(@PathVariable Long[] postIds) {
        boolean success = true;
        for (Long postId : postIds) {
            success = postService.removeById(postId) && success;
        }
        return toAjax(success);
    }
}