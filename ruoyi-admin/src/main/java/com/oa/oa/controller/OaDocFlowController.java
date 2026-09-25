package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocFlow;
import com.oa.oa.service.IOaDocFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/docFlow")
public class OaDocFlowController extends BaseController {

    @Autowired
    private IOaDocFlowService docFlowService;

    @PreAuthorize("@ss.hasPermi('oa:docFlow:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaDocFlow docFlow) {
        startPage();
        List<OaDocFlow> list = docFlowService.selectDocFlowList(docFlow);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:docFlow:query')")
    @GetMapping("/{flowId}")
    public AjaxResult getInfo(@PathVariable Long flowId) {
        return AjaxResult.success(docFlowService.selectDocFlowById(flowId));
    }

    @PreAuthorize("@ss.hasPermi('oa:docFlow:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaDocFlow docFlow) {
        return toAjax(docFlowService.insertDocFlow(docFlow));
    }

    @PreAuthorize("@ss.hasPermi('oa:docFlow:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaDocFlow docFlow) {
        return toAjax(docFlowService.updateDocFlow(docFlow));
    }

    @PreAuthorize("@ss.hasPermi('oa:docFlow:remove')")
    @DeleteMapping("/{flowIds}")
    public AjaxResult remove(@PathVariable Long[] flowIds) {
        return toAjax(docFlowService.deleteDocFlowByIds(flowIds));
    }
}