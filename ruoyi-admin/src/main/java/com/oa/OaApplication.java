package com.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.oa.**.mapper")
public class OaApplication {
    public static void main(String[] args) {
        SpringApplication.run(OaApplication.class, args);
        System.out.println("==================================");
        System.out.println("   OA办公系统启动成功！端口: 8080");
        System.out.println("==================================");
    }
}
