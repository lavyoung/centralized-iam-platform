package com.lavyoung.iam.bootstrap.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 装配 IAM 关系型持久化适配器使用的 MyBatis DAO。
 *
 * <p>扫描范围覆盖全部持久化上下文，但只注册显式标记
 * {@link Mapper} 的接口，避免把 assembler 或 repository 误识别为 DAO。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Configuration(proxyBeanMethods = false)
@MapperScan(
        basePackages = "com.lavyoung.iam.adapter.outbound.persistence",
        annotationClass = Mapper.class
)
public class IamPersistenceConfiguration {
}
