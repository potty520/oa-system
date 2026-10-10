package com.oa.oa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.common.constant.Constants;
import com.oa.common.core.controller.BaseController;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocType;
import com.oa.oa.mapper.OaDocTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/docType")
public class OaDocTypeController extends BaseController {
    @Autowired
    private OaDocTypeMapper docTypeMapper;

    @PreAuthorize("@ss.hasPermi('oa:docType:list')")
    @GetMapping("/list")
    public AjaxResult list(OaDocType docType) {
        LambdaQueryWrapper<OaDocType> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(docType.getTypeName() != null && !docType.getTypeName().isEmpty(), OaDocType::getTypeName, docType.getTypeName())
                .eq(docType.getStatus() != null && !docType.getStatus().isEmpty(), OaDocType::getStatus, docType.getStatus())
                .orderByAsc(OaDocType::getSortOrder);
        List<OaDocType> list = docTypeMapper.selectList(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 公文类型下拉选项。任何已登录用户都可读：收文/发文列表要把 docTypeId 显示成类型名称，
     * 而 /list 带 oa:docType:list 权限，普通用户调用必然 403。
     *
     * <p>与 /system/user/options 同一思路：不加 @PreAuthorize（仍由 SecurityConfig 的
     * anyRequest().authenticated() 保证必须登录），但只返回下拉/标签所需的最小字段，
     * 已停用的类型不返回 —— 前端下拉原本也是用 :disabled 把这些项挡掉的。</p>
     */
    @GetMapping("/options")
    public AjaxResult options() {
        LambdaQueryWrapper<OaDocType> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(OaDocType::getDocTypeId, OaDocType::getTypeName, OaDocType::getTypeCode)
                .eq(OaDocType::getStatus, Constants.STATUS_NORMAL)
                .orderByAsc(OaDocType::getSortOrder);
        return AjaxResult.success(docTypeMapper.selectList(wrapper));
    }

    @PreAuthorize("@ss.hasPermi('oa:docType:query')")
    @GetMapping("/{docTypeId}")
    public AjaxResult getInfo(@PathVariable Long docTypeId) {
        return AjaxResult.success(docTypeMapper.selectById(docTypeId));
    }

    @PreAuthorize("@ss.hasPermi('oa:docType:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaDocType docType) {
        return toAjax(docTypeMapper.insert(docType));
    }

    @PreAuthorize("@ss.hasPermi('oa:docType:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaDocType docType) {
        return toAjax(docTypeMapper.updateById(docType));
    }

    @PreAuthorize("@ss.hasPermi('oa:docType:remove')")
    @DeleteMapping("/{docTypeIds}")
    public AjaxResult remove(@PathVariable Long[] docTypeIds) {
        int count = 0;
        for (Long id : docTypeIds) {
            count += docTypeMapper.deleteById(id);
        }
        return toAjax(count);
    }
}