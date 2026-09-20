package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaIncomingDoc;
import com.oa.oa.service.IOaIncomingDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/incomingDoc")
public class OaIncomingDocController extends BaseController {

    @Autowired
    private IOaIncomingDocService incomingDocService;

    @GetMapping("/list")
    public TableDataInfo list(OaIncomingDoc doc) {
        startPage();
        List<OaIncomingDoc> list = incomingDocService.selectOaIncomingDocList(doc);
        return getDataTable(list);
    }

    @GetMapping("/{docId}")
    public AjaxResult getInfo(@PathVariable Long docId) {
        return AjaxResult.success(incomingDocService.selectOaIncomingDocById(docId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody OaIncomingDoc doc) {
        return toAjax(incomingDocService.insertOaIncomingDoc(doc));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody OaIncomingDoc doc) {
        return toAjax(incomingDocService.updateOaIncomingDoc(doc));
    }

    @DeleteMapping("/{docIds}")
    public AjaxResult remove(@PathVariable Long[] docIds) {
        return toAjax(incomingDocService.deleteOaIncomingDocByIds(docIds));
    }

    @PutMapping("/sign/{docId}")
    public AjaxResult sign(@PathVariable Long docId) {
        return toAjax(incomingDocService.signOaIncomingDoc(docId));
    }

    @PutMapping("/complete/{docId}")
    public AjaxResult complete(@PathVariable Long docId) {
        return toAjax(incomingDocService.completeOaIncomingDoc(docId));
    }
}