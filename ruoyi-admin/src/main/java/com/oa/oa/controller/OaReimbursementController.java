package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaReimbursement;
import com.oa.oa.service.IOaReimbursementService;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/reimbursement")
public class OaReimbursementController extends BaseController {

    @Autowired
    private IOaReimbursementService reimbursementService;

    @GetMapping("/list")
    public TableDataInfo list(OaReimbursement reimbursement) {
        startPage();
        List<OaReimbursement> list = reimbursementService.selectOaReimbursementList(reimbursement);
        return getDataTable(list);
    }

    @GetMapping("/myList")
    public TableDataInfo myList() {
        startPage();
        Long userId = SecurityUtils.getUserId();
        List<OaReimbursement> list = reimbursementService.selectMyReimbursementList(userId);
        return getDataTable(list);
    }

    @GetMapping("/{reimbId}")
    public AjaxResult getInfo(@PathVariable Long reimbId) {
        return AjaxResult.success(reimbursementService.selectOaReimbursementById(reimbId));
    }

    @PostMapping
    public AjaxResult saveWithDetails(@RequestBody OaReimbursement reimbursement) {
        return toAjax(reimbursementService.saveWithDetails(reimbursement));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody OaReimbursement reimbursement) {
        return toAjax(reimbursementService.updateOaReimbursement(reimbursement));
    }

    @DeleteMapping("/{reimbIds}")
    public AjaxResult remove(@PathVariable Long[] reimbIds) {
        return toAjax(reimbursementService.deleteOaReimbursementByIds(reimbIds));
    }

    @PutMapping("/submit/{reimbId}")
    public AjaxResult submit(@PathVariable Long reimbId) {
        return toAjax(reimbursementService.submitOaReimbursement(reimbId));
    }

    @PutMapping("/withdraw/{reimbId}")
    public AjaxResult withdraw(@PathVariable Long reimbId) {
        return toAjax(reimbursementService.withdrawOaReimbursement(reimbId));
    }

    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody OaReimbursement reimbursement) {
        return toAjax(reimbursementService.approveOaReimbursement(reimbursement));
    }

    @GetMapping("/details/{reimbId}")
    public AjaxResult getDetails(@PathVariable Long reimbId) {
        return AjaxResult.success(reimbursementService.getReimbursementDetails(reimbId));
    }

    @GetMapping("/approveLogs/{reimbId}")
    public AjaxResult getLogs(@PathVariable Long reimbId) {
        return AjaxResult.success(reimbursementService.getApproveLogs(reimbId));
    }
}