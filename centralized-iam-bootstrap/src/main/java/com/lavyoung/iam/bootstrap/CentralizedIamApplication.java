package com.lavyoung.iam.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 启动 centralized IAM 模块化单体并装配全部六边形架构组件。
 *
 * <p>该类型只承担进程启动和组件扫描入口职责，不承载业务规则、
 * 安全策略、持久化逻辑或协议端点实现。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@SpringBootApplication(scanBasePackages = "com.lavyoung.iam")
public class CentralizedIamApplication {

    /**
     * 启动 IAM 服务进程。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CentralizedIamApplication.class, args);
    }
}
