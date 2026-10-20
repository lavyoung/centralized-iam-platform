package com.lavyoung.iam.application.clientregistration.service;

import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.domain.clientregistration.model.*;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证客户端注册应用服务的查重、密钥散列和持久化编排。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@ExtendWith(MockitoExtension.class)
class RegisterClientServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");
    private static final CharSequence RAW_SECRET = "client-secret-value";

    @Mock
    private ClientRegistrationRepository repository;

    @Mock
    private ClientSecretHasher secretHasher;

    private RegisterClientService service;

    /**
     * 使用固定时钟创建待测服务，确保注册时间断言不依赖运行环境。
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(REGISTERED_AT, ZoneOffset.UTC);
        service = new RegisterClientService(repository, secretHasher, clock);
    }

    /**
     * Given 未注册的公共客户端，When 执行注册，Then 应跳过散列并保存公共客户端聚合。
     */
    @Test
    void shouldRegisterPublicClientWithoutHashingSecret() {
        // Given
        ClientRegistrationSpec specification = publicSpecification();
        when(repository.existsById(specification.tenantId(), specification.clientId())).thenReturn(false);

        // When
        ClientRegistration registration = service.registerPublic(specification);

        // Then
        assertAll(
                () -> assertEquals(specification, registration.specification()),
                () -> assertEquals(REGISTERED_AT, registration.registeredAt()),
                () -> assertTrue(registration.secretHash().isEmpty())
        );
        InOrder order = inOrder(repository);
        order.verify(repository).existsById(specification.tenantId(), specification.clientId());
        order.verify(repository).save(registration);
        verifyNoInteractions(secretHasher);
    }

    /**
     * Given 未注册的机密客户端，When 执行注册，Then 应按查重、散列、保存的顺序完成编排。
     */
    @Test
    void shouldRegisterConfidentialClientInExpectedOrder() {
        // Given
        ClientRegistrationSpec specification = confidentialSpecification();
        ClientSecretHash secretHash = new ClientSecretHash("a".repeat(60));
        when(repository.existsById(specification.tenantId(), specification.clientId())).thenReturn(false);
        when(secretHasher.hash(RAW_SECRET)).thenReturn(secretHash);

        // When
        ClientRegistration registration = service.registerConfidential(specification, RAW_SECRET);

        // Then
        assertAll(
                () -> assertEquals(secretHash, registration.secretHash().orElseThrow()),
                () -> assertEquals(REGISTERED_AT, registration.registeredAt())
        );
        InOrder order = inOrder(repository, secretHasher);
        order.verify(repository).existsById(specification.tenantId(), specification.clientId());
        order.verify(secretHasher).hash(RAW_SECRET);
        order.verify(repository).save(registration);
    }

    /**
     * Given 已存在的客户端标识，When 执行注册，Then 应在散列和保存之前终止用例。
     */
    @Test
    void shouldRejectDuplicateClientBeforeHashingOrSaving() {
        // Given
        ClientRegistrationSpec specification = confidentialSpecification();
        when(repository.existsById(specification.tenantId(), specification.clientId())).thenReturn(true);

        // When
        IamException exception = assertThrows(IamException.class,
                () -> service.registerConfidential(specification, RAW_SECRET));

        // Then
        assertEquals(IamErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(secretHasher);
    }

    /**
     * Given 缺失的注册配置，When 执行注册，Then 应在访问任何输出端口之前终止用例。
     */
    @Test
    void shouldRejectMissingSpecificationBeforeCallingPorts() {
        // When
        IamException exception = assertThrows(IamException.class, () -> service.registerPublic(null));

        // Then
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
        verifyNoInteractions(repository, secretHasher);
    }

    /**
     * Given 密钥散列组件拒绝明文，When 注册机密客户端，Then 不得保存半完整聚合。
     */
    @Test
    void shouldNotSaveClientWhenSecretHashingFails() {
        // Given
        ClientRegistrationSpec specification = confidentialSpecification();
        when(repository.existsById(specification.tenantId(), specification.clientId())).thenReturn(false);
        when(secretHasher.hash(RAW_SECRET)).thenThrow(new IllegalArgumentException("invalid secret"));

        // When
        assertThrows(IllegalArgumentException.class, () -> service.registerConfidential(specification, RAW_SECRET));

        // Then
        verify(repository, never()).save(any());
    }

    private ClientRegistrationSpec publicSpecification() {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("public-web-app"),
                "Public Web App",
                Set.of(ClientAuthenticationMethod.NONE),
                Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN),
                Set.of(new RedirectUri("https://app.example.com/callback")),
                Set.of("openid", "profile"),
                ClientTokenPolicy.standard()
        );
    }

    private ClientRegistrationSpec confidentialSpecification() {
        return new ClientRegistrationSpec(
                new TenantId("default"),
                new ClientId("service-app"),
                "Service App",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Set.of(),
                Set.of("service.read"),
                ClientTokenPolicy.standard()
        );
    }
}
