package com.oa.oa.controller;

import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaAnnouncement;
import com.oa.oa.service.IOaAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/oa/announcement")
public class OaAnnouncementController extends BaseController {

    @Autowired
    private IOaAnnouncementService announcementService;

    @PreAuthorize("@ss.hasPermi('oa:announcement:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaAnnouncement announcement) {
        startPage();
        List<OaAnnouncement> list = announcementService.selectOaAnnouncementList(announcement);
        return getDataTable(list);
    }

    @GetMapping("/published")
    public TableDataInfo publishedList(OaAnnouncement announcement) {
        startPage();
        List<OaAnnouncement> list = announcementService.selectPublishedAnnouncementList(announcement);
        return getDataTable(list);
    }

    /** 当前生效且对当前用户可见的公告数（系统未记录逐人已读状态，不能叫未读数） */
    @GetMapping("/effectiveCount")
    public AjaxResult effectiveCount() {
        return AjaxResult.success(announcementService.countEffectiveAnnouncements());
    }

    @PreAuthorize("@ss.hasPermi('oa:announcement:query')")
    @GetMapping("/{annId}")
    public AjaxResult getInfo(@PathVariable Long annId) {
        announcementService.incrementViewCount(annId);
        return AjaxResult.success(announcementService.selectOaAnnouncementById(annId));
    }

    @PreAuthorize("@ss.hasPermi('oa:announcement:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaAnnouncement announcement) {
        return toAjax(announcementService.insertOaAnnouncement(announcement));
    }

    @PreAuthorize("@ss.hasPermi('oa:announcement:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody OaAnnouncement announcement) {
        return toAjax(announcementService.updateOaAnnouncement(announcement));
    }

    @PreAuthorize("@ss.hasPermi('oa:announcement:remove')")
    @DeleteMapping("/{annIds}")
    public AjaxResult remove(@PathVariable Long[] annIds) {
        return toAjax(announcementService.deleteOaAnnouncementByIds(annIds));
    }
}