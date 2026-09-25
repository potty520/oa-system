package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.DocActionRequest;
import com.oa.oa.domain.OaDocArchive;
import com.oa.oa.service.IOaDocProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/doc")
public class OaDocProcessController extends BaseController {
    @Autowired
    private IOaDocProcessService docProcessService;

    @PreAuthorize("@ss.hasPermi('oa:doc:list')")
    @GetMapping("/timeline/{docType}/{docId}")
    public AjaxResult timeline(@PathVariable String docType, @PathVariable Long docId) {
        return AjaxResult.success(docProcessService.getTimeline(docType, docId));
    }

    @PreAuthorize("@ss.hasPermi('oa:doc:action')")
    @PostMapping("/action/{docType}/{docId}")
    public AjaxResult action(@PathVariable String docType, @PathVariable Long docId,
                             @RequestBody DocActionRequest request) {
        docProcessService.execute(docType, docId, request);
        return AjaxResult.success("操作成功");
    }

    @PreAuthorize("@ss.hasPermi('oa:doc:list')")
    @GetMapping("/my/circulation")
    public AjaxResult myCirculation() {
        return AjaxResult.success(docProcessService.getMyCirculation());
    }

    @PreAuthorize("@ss.hasPermi('oa:doc:action')")
    @PutMapping("/circulation/read/{circulationId}")
    public AjaxResult markRead(@PathVariable Long circulationId) {
        return toAjax(docProcessService.markCirculationRead(circulationId));
    }

    @PreAuthorize("@ss.hasPermi('oa:doc:list')")
    @GetMapping("/my/done")
    public AjaxResult myDone() {
        return AjaxResult.success(docProcessService.getMyDone());
    }

    // 归档台账是全单位的公文清单，只开放给收发文管理岗
    @PreAuthorize("@ss.hasAnyPermi('oa:incomingDoc:list,oa:outgoingDoc:list')")
    @GetMapping("/archive/list")
    public TableDataInfo archiveList(OaDocArchive archive) {
        startPage();
        List<OaDocArchive> list = docProcessService.getArchiveList(archive);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:doc:list')")
    @GetMapping("/reminder/{docType}/{docId}")
    public AjaxResult reminders(@PathVariable String docType, @PathVariable Long docId) {
        return AjaxResult.success(docProcessService.getReminderList(docType, docId));
    }
}