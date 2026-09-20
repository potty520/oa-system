package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocFlow;
import com.oa.oa.service.IOaDocFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/docFlow")
public class OaDocFlowController extends BaseController {

    @Autowired
    private IOaDocFlowService docFlowService;

    @GetMapping("/list")
    public TableDataInfo list(OaDocFlow docFlow) {
        startPage();
        List<OaDocFlow> list = docFlowService.selectDocFlowList(docFlow);
        return getDataTable(list);
    }

    @GetMapping("/{flowId}")
    public AjaxResult getInfo(@PathVariable Long flowId) {
        return AjaxResult.success(docFlowService.selectDocFlowById(flowId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody OaDocFlow docFlow) {
        return toAjax(docFlowService.insertDocFlow(docFlow));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody OaDocFlow docFlow) {
        return toAjax(docFlowService.updateDocFlow(docFlow));
    }

    @DeleteMapping("/{flowIds}")
    public AjaxResult remove(@PathVariable Long[] flowIds) {
        return toAjax(docFlowService.deleteDocFlowByIds(flowIds));
    }
}