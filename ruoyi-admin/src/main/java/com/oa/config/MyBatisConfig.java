package com.oa.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.oa.security.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
public class MyBatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Date now = new Date();
                String username = currentUsername();
                // 审计字段一律以服务端为准。
                // strictInsertFill 只在字段为 null 时填充，客户端只要在请求体里带上
                // createBy / createTime / updateBy，就会被原样写库，等于可以伪造创建人；
                // 这里改为强制覆盖，使审计字段无法由客户端决定。
                forceFill(metaObject, "createTime", now);
                forceFill(metaObject, "updateTime", now);
                forceFill(metaObject, "createBy", username);
                forceFill(metaObject, "updateBy", username);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                forceFill(metaObject, "updateTime", new Date());
                forceFill(metaObject, "updateBy", currentUsername());
            }

            /** 仅在实体确实存在该属性时写入，避免对不含审计字段的实体报错 */
            private void forceFill(MetaObject metaObject, String field, Object value) {
                if (metaObject.hasSetter(field)) {
                    metaObject.setValue(field, value);
                }
            }

            private String currentUsername() {
                String username = SecurityUtils.getUsername();
                return username != null ? username : "system";
            }
        };
    }
}