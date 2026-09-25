package com.oa.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/system/log")
public class SysOperLogController extends BaseController {

    /** 导出行数上限：日志表只会一直长，不设上限迟早把内存打满 */
    private static final int EXPORT_MAX_ROWS = 50000;

    /** 清理天数上限，避免传一个荒谬的值把日志全清掉 */
    private static final int CLEAN_MAX_DAYS = 3650;

    /** Excel/CSV 会把以这些字符开头的单元格当公式执行 */
    private static final String CSV_FORMULA_PREFIXES = "=+-@\t\r";

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Autowired
    private SysOperLogMapper operLogMapper;

    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysOperLog log) {
        startPage();
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(log.getTitle() != null && !log.getTitle().isEmpty(), SysOperLog::getTitle, log.getTitle())
                .eq(log.getStatus() != null && !log.getStatus().isEmpty(), SysOperLog::getStatus, log.getStatus())
                .orderByDesc(SysOperLog::getOperTime);
        List<SysOperLog> list = operLogMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:log:list')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        PageHelper.startPage(1, EXPORT_MAX_ROWS, false);
        List<SysOperLog> logs = operLogMapper.selectList(new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getOperTime));
        StringBuilder csv = new StringBuilder(Math.min(logs.size() * 128 + 64, 1 << 20));
        csv.append("logId,title,operator,method,url,status,time,error\n");
        for (SysOperLog log : logs) {
            csv.append(csv(log.getLogId())).append(',')
                    .append(csv(log.getTitle())).append(',')
                    .append(csv(log.getOperatorName())).append(',')
                    .append(csv(log.getMethod())).append(',')
                    .append(csv(log.getOperUrl())).append(',')
                    .append(csv(log.getStatus())).append(',')
                    .append(csv(log.getOperTime() == null ? "" : log.getOperTime().toString())).append(',')
                    .append(csv(log.getErrorMsg())).append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        // Excel 打开无 BOM 的 UTF-8 CSV 会把中文全显成乱码
        ByteBuffer buffer = ByteBuffer.allocate(UTF8_BOM.length + body.length);
        buffer.put(UTF8_BOM).put(body);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=oper-log.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(buffer.array());
    }

    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean(@RequestParam(value = "days", defaultValue = "30") Integer days) {
        int safeDays = Math.min(Math.max(days == null ? 30 : days, 1), CLEAN_MAX_DAYS);
        Date before = new Date(System.currentTimeMillis() - safeDays * 86400000L);
        int count = operLogMapper.delete(new LambdaQueryWrapper<SysOperLog>()
                .lt(SysOperLog::getOperTime, before));
        return AjaxResult.success("清理完成", count);
    }

    /**
     * CSV 单元格转义：除了引号转义，还要去掉换行并阻断公式注入。
     * 日志里的请求参数、错误信息都是用户可控内容，直接拼接等于把任意公式写进管理员的 Excel。
     */
    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        if (!text.isEmpty() && CSV_FORMULA_PREFIXES.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @DeleteMapping("/{logIds}")
    public AjaxResult remove(@PathVariable Long[] logIds) {
        int count = 0;
        for (Long id : logIds) {
            count += operLogMapper.deleteById(id);
        }
        return toAjax(count);
    }
}
