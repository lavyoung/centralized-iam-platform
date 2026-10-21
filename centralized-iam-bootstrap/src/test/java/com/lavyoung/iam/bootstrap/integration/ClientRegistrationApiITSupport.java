package com.lavyoung.iam.bootstrap.integration;

import com.lavyoung.iam.bootstrap.CentralizedIamApplication;
import com.lavyoung.iam.domain.clientregistration.model.ClientRegistration;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 复用客户端注册 API 在不同关系型数据库上的完整链路契约。
 *
 * <p>子类只负责提供数据库容器和动态连接属性，本类型验证 REST、应用、
 * 领域、MyBatis 仓储、Flyway 表结构及国际化异常响应能够协同工作。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@SpringBootTest(classes = CentralizedIamApplication.class, properties = "spring.main.banner-mode=off")
@AutoConfigureMockMvc
abstract class ClientRegistrationApiITSupport {

    private static final String CLIENT_ID = "integration-public-client";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRegistrationRepository repository;

    /**
     * Given 已完成迁移的真实数据库，When 注册并重复注册公共客户端，Then 应持久化聚合并返回稳定冲突响应。
     *
     * @throws Exception 当 HTTP 请求或数据库验证失败时抛出
     */
    @Test
    void shouldRegisterAndRejectDuplicatePublicClient() throws Exception {
        // Given
        String request = """
                {
                  "clientId": "integration-public-client",
                  "displayName": "Integration Public Client",
                  "authenticationMethods": ["none"],
                  "grantTypes": ["authorization_code", "refresh_token"],
                  "redirectUris": ["https://integration.example.com/callback"],
                  "scopes": ["openid", "profile"],
                  "accessTokenTtlSeconds": 900,
                  "refreshTokenTtlSeconds": 28800,
                  "authorizationCodeTtlSeconds": 300
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IAM-0000"))
                .andExpect(jsonPath("$.data.tenantId").value(TenantId.DEFAULT_ID.value()))
                .andExpect(jsonPath("$.data.clientId").value(CLIENT_ID))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Optional<ClientRegistration> persisted = repository.findById(TenantId.DEFAULT_ID, new ClientId(CLIENT_ID));
        assertTrue(persisted.isPresent());
        assertAll(
                () -> assertEquals("Integration Public Client", persisted.orElseThrow().specification().displayName()),
                () -> assertEquals(2, persisted.orElseThrow().specification().grantTypes().size()),
                () -> assertTrue(persisted.orElseThrow().secretHash().isEmpty())
        );

        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IAM-1002"))
                .andExpect(jsonPath("$.message").value("请求的资源已存在"));
    }
}
