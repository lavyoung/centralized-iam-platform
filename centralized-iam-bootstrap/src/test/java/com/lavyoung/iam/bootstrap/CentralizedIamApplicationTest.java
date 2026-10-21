package com.lavyoung.iam.bootstrap;

import com.lavyoung.iam.adapter.inbound.exception.IamRestExceptionHandler;
import com.lavyoung.iam.adapter.inbound.rest.clientregistration.controller.ClientRegistrationController;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.dao.ClientRegistrationDao;
import com.lavyoung.iam.application.clientregistration.port.in.RegisterClientUseCase;
import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 centralized IAM 启动入口能够装配完整的客户端注册六边形链路。
 *
 * <p>本测试使用内存数据源且禁用 Flyway，只检查 Spring Bean 与 MyBatis
 * 代理装配；MySQL/PostgreSQL SQL 契约继续由 Testcontainers 集成测试负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@SpringBootTest(
        classes = CentralizedIamApplication.class,
        properties = {
                "spring.profiles.active=context-test",
                "spring.datasource.url=jdbc:h2:mem:iam-context;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=false",
                "spring.main.banner-mode=off"
        }
)
class CentralizedIamApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Given 完整应用上下文，When 查询核心端口与入站组件，Then 每种职责都应唯一装配。
     */
    @Test
    void shouldWireClientRegistrationHexagon() {
        // When / Then
        assertAll(
                () -> assertSingleBean(RegisterClientUseCase.class),
                () -> assertSingleBean(ClientRegistrationRepository.class),
                () -> assertSingleBean(ClientSecretHasher.class),
                () -> assertSingleBean(ClientRegistrationController.class),
                () -> assertSingleBean(IamRestExceptionHandler.class)
        );
    }

    /**
     * Given bootstrap 基础设施配置，When 启动上下文，Then 环境依赖与 MyBatis DAO 应可用。
     */
    @Test
    void shouldWireInfrastructureBeans() {
        // When / Then
        assertAll(
                () -> assertSingleBean(Clock.class),
                () -> assertSingleBean(PasswordEncoder.class),
                () -> assertSingleBean(MessageSource.class),
                () -> assertNotNull(applicationContext.getBean(ClientRegistrationDao.class))
        );
    }

    private <T> void assertSingleBean(Class<T> beanType) {
        assertEquals(1, applicationContext.getBeansOfType(beanType).size(), beanType.getName());
    }
}
