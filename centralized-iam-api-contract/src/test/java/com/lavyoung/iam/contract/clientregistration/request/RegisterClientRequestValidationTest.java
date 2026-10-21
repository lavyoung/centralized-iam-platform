package com.lavyoung.iam.contract.clientregistration.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册请求的 Jakarta Validation 约束和密钥脱敏行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class RegisterClientRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    /**
     * 创建不依赖 Jakarta EL 实现的测试 Validator。
     */
    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    /**
     * 关闭 ValidatorFactory 持有的资源。
     */
    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    /**
     * Given 合法公共客户端请求，When 执行契约校验，Then 不应产生约束错误。
     */
    @Test
    void shouldAcceptValidPublicClientRequest() {
        // Given
        RegisterPublicClientRequest request = validPublicRequest();

        // When
        Set<ConstraintViolation<RegisterPublicClientRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    /**
     * Given 缺失字段和非法有效期，When 校验公共客户端请求，Then 应报告全部字段路径。
     */
    @Test
    void shouldRejectIncompletePublicClientRequest() {
        // Given
        RegisterPublicClientRequest request = new RegisterPublicClientRequest(
                " ",
                "",
                Set.of(),
                Set.of(),
                null,
                Set.of(),
                0,
                -1,
                0
        );

        // When
        Set<String> invalidProperties = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());

        // Then
        assertEquals(Set.of(
                "clientId",
                "displayName",
                "authenticationMethods",
                "grantTypes",
                "redirectUris",
                "scopes",
                "accessTokenTtlSeconds",
                "refreshTokenTtlSeconds",
                "authorizationCodeTtlSeconds"
        ), invalidProperties);
    }

    /**
     * Given 长度不足的客户端密钥，When 校验机密客户端请求，Then 应返回密钥长度错误。
     */
    @Test
    void shouldRejectShortConfidentialClientSecret() {
        // Given
        RegisterConfidentialClientRequest request = confidentialRequest("short-client-secret");

        // When
        Set<ConstraintViolation<RegisterConfidentialClientRequest>> violations = validator.validate(request);

        // Then
        assertEquals(1, violations.size());
        assertEquals("clientSecret", violations.iterator().next().getPropertyPath().toString());
        assertEquals("{validation.client-secret.size}", violations.iterator().next().getMessageTemplate());
    }

    /**
     * Given 合法机密请求，When 输出调试文本，Then 不得包含客户端明文密钥。
     */
    @Test
    void shouldRedactClientSecretFromStringRepresentation() {
        // Given
        String clientSecret = "a".repeat(32);
        RegisterConfidentialClientRequest request = confidentialRequest(clientSecret);

        // When
        String representation = request.toString();

        // Then
        assertTrue(validator.validate(request).isEmpty());
        assertFalse(representation.contains(clientSecret));
        assertTrue(representation.contains("[REDACTED]"));
    }

    private RegisterPublicClientRequest validPublicRequest() {
        return new RegisterPublicClientRequest(
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid", "profile"),
                900,
                28_800,
                300
        );
    }

    private RegisterConfidentialClientRequest confidentialRequest(String clientSecret) {
        return new RegisterConfidentialClientRequest(
                "service-app",
                "Service App",
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of("service.read"),
                900,
                28_800,
                300,
                clientSecret
        );
    }
}
