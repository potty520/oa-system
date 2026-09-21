package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaOutgoingDoc;
import com.oa.oa.service.IOaOutgoingDocService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/outgoingDoc")
public class OaOutgoingDocController extends BaseController {

    @Autowired
    private IOaOutgoingDocService outgoingDocService;

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaOutgoingDoc doc) {
        startPage();
        List<OaOutgoingDoc> list = outgoingDocService.selectOaOutgoingDocList(doc);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:query')")
    @GetMapping("/{docId}")
    public AjaxResult getInfo(@PathVariable Long docId) {
        return AjaxResult.success(outgoingDocService.selectOaOutgoingDocById(docId));
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaOutgoingDoc doc) {
        if (StringUtils.isBlank(doc.getDocTitle()) || StringUtils.isBlank(doc.getDocNumber())) {
            return AjaxResult.error(400, "发文标题和发文编号不能为空");
        }
        if (outgoingDocService.existsByDocNumber(doc.getDocNumber(), null)) {
            return AjaxResult.error(409, "发文编号已存在");
        }
        return toAjax(outgoingDocService.insertOaOutgoingDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaOutgoingDoc doc) {
        if (doc.getDocId() == null) {
            return AjaxResult.error(400, "发文ID不能为空");
        }
        if (StringUtils.isBlank(doc.getDocTitle()) || StringUtils.isBlank(doc.getDocNumber())) {
            return AjaxResult.error(400, "发文标题和发文编号不能为空");
        }
        if (outgoingDocService.existsByDocNumber(doc.getDocNumber(), doc.getDocId())) {
            return AjaxResult.error(409, "发文编号已存在");
        }
        return toAjax(outgoingDocService.updateOaOutgoingDoc(doc));
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:remove')")
    @DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds) {
        return toAjax(outgoingDocService.deleteOaOutgoingDocByIds(docIds));
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:edit')")
    @PutMapping("/submit/{docId}")
    public AjaxResult submit(@PathVariable Long docId) {
        return toAjax(outgoingDocService.submitOaOutgoingDoc(docId));
    }

    @PreAuthorize("@ss.hasPermi('oa:outgoingDoc:edit')")
    @PutMapping("/sign/{docId}")
    public AjaxResult sign(@PathVariable Long docId) {
        return toAjax(outgoingDocService.signOaOutgoingDoc(docId));
    }
}