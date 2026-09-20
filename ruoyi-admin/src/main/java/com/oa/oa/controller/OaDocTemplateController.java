package com.oa.oa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocTemplate;
import org.apache.commons.lang3.StringUtils;
import com.oa.oa.mapper.OaDocTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/docTemplate")
public class OaDocTemplateController extends BaseController {
    @Autowired
    private OaDocTemplateMapper templateMapper;

    @PreAuthorize("@ss.hasPermi('oa:docTemplate:list')")
    @GetMapping("/list")
    public AjaxResult list(OaDocTemplate template) {
        LambdaQueryWrapper<OaDocTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(template.getDocType() != null && !template.getDocType().isEmpty(), OaDocTemplate::getDocType, template.getDocType())
                .eq(template.getStatus() != null && !template.getStatus().isEmpty(), OaDocTemplate::getStatus, template.getStatus())
                .orderByAsc(OaDocTemplate::getTemplateId);
        List<OaDocTemplate> list = templateMapper.selectList(wrapper);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:docTemplate:query')")
    @GetMapping("/{templateId}")
    public AjaxResult getInfo(@PathVariable Long templateId) {
        return AjaxResult.success(templateMapper.selectById(templateId));
    }

    @PreAuthorize("@ss.hasPermi('oa:docTemplate:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaDocTemplate template) {
        if (StringUtils.isBlank(template.getTemplateName()) || template.getDocTypeId() == null) {
            return AjaxResult.error(400, "模板名称和公文类型不能为空");
        }
        return toAjax(templateMapper.insert(template));
    }

    @PreAuthorize("@ss.hasPermi('oa:docTemplate:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaDocTemplate template) {
        if (template.getTemplateId() == null || StringUtils.isBlank(template.getTemplateName()) || template.getDocTypeId() == null) {
            return AjaxResult.error(400, "模板ID、模板名称和公文类型不能为空");
        }
        return toAjax(templateMapper.updateById(template));
    }

    @PreAuthorize("@ss.hasPermi('oa:docTemplate:remove')")
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds) {
        int count = 0;
        for (Long id : templateIds) {
            count += templateMapper.deleteById(id);
        }
        return toAjax(count);
    }
}