package com.oa.oa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oa.common.core.controller.BaseController;
import com.oa.common.core.page.TableDataInfo;
import com.oa.common.core.result.AjaxResult;
import com.oa.oa.domain.OaDocument;
import com.oa.oa.mapper.OaDocumentMapper;
import com.oa.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/oa/document")
public class OaDocumentController extends BaseController {
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "txt",
            "png", "jpg", "jpeg", "gif", "bmp", "zip", "rar", "7z"
    ));

    @Autowired
    private OaDocumentMapper documentMapper;

    @Value("${app.upload.path:./oa-upload}")
    private String uploadPath;

    @PreAuthorize("@ss.hasPermi('oa:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaDocument document) {
        startPage();
        LambdaQueryWrapper<OaDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(document.getDocumentName() != null && !document.getDocumentName().isEmpty(), OaDocument::getDocumentName, document.getDocumentName())
                .eq(document.getDocumentType() != null && !document.getDocumentType().isEmpty(), OaDocument::getDocumentType, document.getDocumentType());
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            wrapper.eq(OaDocument::getUserId, SecurityUtils.getUserId());
        }
        wrapper.orderByDesc(OaDocument::getCreateTime);
        List<OaDocument> list = documentMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('oa:document:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "documentType", required = false) String documentType) throws Exception {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error(400, "请选择文件");
        }
        Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String original = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String suffix = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            suffix = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        }
        String extension = suffix.startsWith(".") ? suffix.substring(1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return AjaxResult.error(400, "不支持的文件类型");
        }
        Path target = base.resolve(UUID.randomUUID().toString().replace("-", "") + suffix).normalize();
        if (!target.startsWith(base)) {
            return AjaxResult.error(400, "非法文件路径");
        }
        file.transferTo(target.toFile());
        OaDocument document = new OaDocument();
        document.setDocumentName(original);
        document.setDocumentType(documentType);
        document.setFilePath(target.toString());
        document.setFileSize(file.getSize());
        document.setUserId(SecurityUtils.getUserId());
        document.setUserName(SecurityUtils.getUsername());
        document.setStatus("0");
        return toAjax(documentMapper.insert(document));
    }

    @PreAuthorize("@ss.hasPermi('oa:document:query')")
    @GetMapping("/download/{documentId}")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long documentId) throws Exception {
        OaDocument document = documentMapper.selectById(documentId);
        if (document == null || !canAccess(document)) {
            return ResponseEntity.notFound().build();
        }
        Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path file = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
        if (!file.startsWith(base) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(document.getDocumentName(), StandardCharsets.UTF_8.name()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @PreAuthorize("@ss.hasPermi('oa:document:remove')")
    @DeleteMapping("/{documentId}")
    public AjaxResult remove(@PathVariable Long documentId) throws Exception {
        OaDocument document = documentMapper.selectById(documentId);
        if (document == null || !canAccess(document)) {
            return AjaxResult.error(403, "无权删除该文件");
        }
        Path base = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path file = Paths.get(document.getFilePath()).toAbsolutePath().normalize();
        if (file.startsWith(base)) {
            Files.deleteIfExists(file);
        }
        return toAjax(documentMapper.deleteById(documentId));
    }

    private boolean canAccess(OaDocument document) {
        return SecurityUtils.isAdmin(SecurityUtils.getUserId()) || document.getUserId().equals(SecurityUtils.getUserId());
    }
}