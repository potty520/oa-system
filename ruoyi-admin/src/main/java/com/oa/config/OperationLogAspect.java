package com.oa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class OperationLogAspect {
    @Autowired
    private SysOperLogMapper operLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("execution(public * com.oa..controller..*(..)) && !execution(* com.oa.system.controller.SysOperLogController.*(..))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog log = new SysOperLog();
        log.setTitle(joinPoint.getSignature().getDeclaringType().getSimpleName());
        log.setMethod(joinPoint.getSignature().toShortString());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setOperTime(new Date());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setOperUrl(request.getRequestURI());
            log.setOperIp(request.getRemoteAddr());
        }
        try {
            Object result = joinPoint.proceed();
            log.setOperParam(serializeArgs(joinPoint.getArgs()));
            log.setJsonResult(truncate(objectMapper.writeValueAsString(result), 2000));
            log.setStatus("0");
            log.setBusinessType("OTHER");
            operLogMapper.insert(log);
            return result;
        } catch (Throwable ex) {
            log.setOperParam(serializeArgs(joinPoint.getArgs()));
            log.setStatus("1");
            log.setErrorMsg(truncate(ex.getMessage(), 2000));
            operLogMapper.insert(log);
            throw ex;
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            List<Object> values = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
                    continue;
                }
                values.add(sanitize(arg));
            }
            return truncate(objectMapper.writeValueAsString(values), 4000);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Object sanitize(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> target = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (key.toLowerCase().contains("password") || key.toLowerCase().contains("token") || key.toLowerCase().contains("secret")) {
                    target.put(key, "***");
                } else {
                    target.put(key, sanitize(entry.getValue()));
                }
            }
            return target;
        }
        if (value instanceof Collection) {
            List<Object> target = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                target.add(sanitize(item));
            }
            return target;
        }
        return value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}