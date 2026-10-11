package com.oa.common.exception;

import com.oa.common.core.result.AjaxResult;
import com.oa.common.utils.ClientIpUtils;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 权限不足时的固定提示：不回显 Spring Security 的本地化 message，避免提示随 Accept-Language 漂移 */
    private static final String DEFAULT_ACCESS_DENIED_MESSAGE = "没有操作权限";

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public AjaxResult handleBusinessException(RuntimeException ex) {
        return AjaxResult.error(400, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AjaxResult> handleAccessDenied(AccessDeniedException ex) {
        // 响应体的 code 与 HTTP 状态码保持一致：此前只设 body code=403 而 HTTP 仍是 200，
        // 网关、监控与客户端无法按协议判定"无权限"，只能解析响应体。
        // 用 ResponseEntity 表达状态码，避免向异常处理方法注入 HttpServletResponse。
        //
        // 文案分两类：业务规则拒绝（BusinessAccessDeniedException）带自己的中文提示，原样返回；
        // 权限不足时 Spring Security 的 ex.getMessage() 取自本地化资源包，会随 Accept-Language
        // 在"不允许访问"与"Access is denied"之间漂移，因此换成固定文案。
        String message = ex instanceof BusinessAccessDeniedException
                ? ex.getMessage()
                : DEFAULT_ACCESS_DENIED_MESSAGE;
        recordDeniedRequest(message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AjaxResult.error(403, message));
    }

    /**
     * 记录一次越权访问。
     *
     * <p>{@code @PreAuthorize} 的判定发生在控制器方法之前，OperationLogAspect 的切点不会执行，
     * 于是"被拒绝的请求"在审计里完全不可见 —— 而这恰恰是安全审计最关心的失败类型之一。
     * 这里在异常处理阶段补一条 status=1 的记录；写库失败只告警，绝不影响响应。</p>
     */
    private void recordDeniedRequest(String message) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes == null ? null : attributes.getRequest();
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle("AccessDenied");
            operLog.setBusinessType("OTHER");
            operLog.setRequestMethod(request == null ? null : request.getMethod());
            operLog.setMethod(request == null ? "denied" : request.getMethod() + " " + request.getRequestURI());
            operLog.setOperUrl(request == null ? null : request.getRequestURI());
            operLog.setOperatorName(SecurityUtils.getUsername());
            operLog.setOperIp(ClientIpUtils.resolve(request, trustForwardedHeaders));
            operLog.setStatus("1");
            operLog.setErrorMsg(message);
            operLog.setOperTime(new Date());
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.warn("记录越权访问日志失败: {}", e.getMessage());
        }
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public AjaxResult handleValidationException(Exception ex) {
        return AjaxResult.error(400, "请求参数不完整或格式错误");
    }

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public AjaxResult handleDuplicateKey(RuntimeException ex) {
        return AjaxResult.error(409, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public AjaxResult handleMyBatisSystemException(MyBatisSystemException ex) {
        if (isUniqueConstraint(ex)) {
            return AjaxResult.error(409, "数据已存在，请勿重复提交");
        }
        log.error("MyBatis application exception", ex);
        return AjaxResult.error(500, "系统处理失败");
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception ex) {
        log.error("Unhandled application exception", ex);
        return AjaxResult.error(500, "系统处理失败");
    }

    private boolean isUniqueConstraint(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("UNIQUE constraint failed")
                    || message.contains("Duplicate entry")
                    || message.contains("duplicate key")
                    || message.contains("唯一约束"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
