package com.oa.common.exception;

import com.oa.common.core.result.AjaxResult;
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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public AjaxResult handleBusinessException(RuntimeException ex) {
        return AjaxResult.error(400, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDenied(AccessDeniedException ex) {
        return AjaxResult.error(403, ex.getMessage());
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
