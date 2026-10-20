package com.lavyoung.iam.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/**
 * 装配 IAM 核心用例依赖的时间与密码学基础设施。
 *
 * <p>该配置只负责提供环境相关 Bean，不承载客户端注册或认证业务规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Configuration(proxyBeanMethods = false)
public class IamCoreConfiguration {

    /**
     * 提供统一的 UTC 系统时钟，业务代码不得直接绑定系统默认时区。
     *
     * @return UTC 系统时钟
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 提供带算法标识的委托式密码编码器，以支持摘要算法平滑升级。
     *
     * @return 客户端密钥和用户密码可复用的密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
