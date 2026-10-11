package com.oa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.common.utils.ClientIpUtils;
import com.oa.security.utils.SecurityUtils;
import com.oa.system.domain.SysOperLog;
import com.oa.system.mapper.SysOperLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

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

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 脱敏递归上限，防止循环引用或超深嵌套把栈打爆 */
    private static final int MAX_SANITIZE_DEPTH = 8;

    private static final String REDACTED = "***";

    @Autowired
    private SysOperLogMapper operLogMapper;

    /** 只有显式信任反向代理时才采用 X-Forwarded-For，否则客户端可以伪造审计 IP */
    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("execution(public * com.oa..controller..*(..)) && !execution(* com.oa.system.controller.SysOperLogController.*(..))")
    public Object record(ProceedingJoinPoint joinPoint) throws Throwable {
        SysOperLog operLog = new SysOperLog();
        operLog.setTitle(joinPoint.getSignature().getDeclaringType().getSimpleName());
        operLog.setMethod(joinPoint.getSignature().toShortString());
        operLog.setOperatorName(SecurityUtils.getUsername());
        operLog.setOperTime(new Date());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(request.getRequestURI());
            operLog.setOperIp(resolveClientIp(request));
        }
        operLog.setOperParam(serializeArgs(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            operLog.setJsonResult(truncate(writeJson(sanitize(result)), 2000));
            operLog.setStatus("0");
            operLog.setBusinessType("OTHER");
            saveQuietly(operLog);
            return result;
        } catch (Throwable ex) {
            operLog.setStatus("1");
            operLog.setErrorMsg(truncate(ex.getMessage(), 2000));
            saveQuietly(operLog);
            throw ex;
        }
    }

    /** 审计写库失败不能影响业务请求，也不能盖住业务本身抛出的异常 */
    private void saveQuietly(SysOperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception ex) {
            log.warn("写入操作日志失败: {}", ex.getMessage());
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        return ClientIpUtils.resolve(request, trustForwardedHeaders);
    }

    private String serializeArgs(Object[] args) {
        try {
            List<Object> values = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof ServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                    continue;
                }
                values.add(sanitize(arg));
            }
            return truncate(writeJson(values), 4000);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Object sanitize(Object value) {
        return sanitize(value, 0);
    }

    private Object sanitize(Object value, int depth) {
        if (value == null || depth > MAX_SANITIZE_DEPTH || isScalar(value)) {
            return value;
        }
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> target = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                target.put(key, isSensitive(key) ? REDACTED : sanitize(entry.getValue(), depth + 1));
            }
            return target;
        }
        if (value instanceof Collection) {
            List<Object> target = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                target.add(sanitize(item, depth + 1));
            }
            return target;
        }
        if (value instanceof Object[]) {
            List<Object> target = new ArrayList<>();
            for (Object item : (Object[]) value) {
                target.add(sanitize(item, depth + 1));
            }
            return target;
        }
        // 普通 DTO/实体要先转成 Map 才能按字段名脱敏，否则口令之类的字段会原样落库
        try {
            Object converted = objectMapper.convertValue(value, Object.class);
            if (converted instanceof Map || converted instanceof Collection) {
                return sanitize(converted, depth + 1);
            }
            return converted;
        } catch (IllegalArgumentException ex) {
            return String.valueOf(value);
        }
    }

    private boolean isScalar(Object value) {
        return value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Character || value instanceof Date || value instanceof Enum
                || value instanceof Class;
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("token") || lower.contains("secret");
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
