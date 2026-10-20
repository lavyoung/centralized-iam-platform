package com.lavyoung.iam.application.clientregistration.service;

import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.domain.clientregistration.model.ClientRegistration;
import com.lavyoung.iam.domain.clientregistration.model.ClientSecretHash;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证客户端注册应用服务的命令转换、查重、密钥散列和结果转换。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@ExtendWith(MockitoExtension.class)
class RegisterClientServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");
    private static final CharSequence RAW_SECRET = "client-secret-value";
    private static final TenantId TENANT_ID = new TenantId("default");

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
     * Given 未注册的公共客户端命令，When 执行注册，Then 应保存公共聚合并返回完整非敏感结果。
     */
    @Test
    void shouldRegisterPublicClientAndReturnResult() {
        // Given
        RegisterClientCommand command = publicCommand();
        ClientId clientId = new ClientId(command.clientId());
        when(repository.existsById(TENANT_ID, clientId)).thenReturn(false);
        ArgumentCaptor<ClientRegistration> registrationCaptor = ArgumentCaptor.forClass(ClientRegistration.class);

        // When
        RegisteredClientResult result = service.registerPublic(command);

        // Then
        verify(repository).save(registrationCaptor.capture());
        ClientRegistration savedRegistration = registrationCaptor.getValue();
        assertAll(
                () -> assertEquals("default", result.tenantId()),
                () -> assertEquals("public-web-app", result.clientId()),
                () -> assertEquals(Set.of("none"), result.authenticationMethods()),
                () -> assertEquals(Set.of("authorization_code", "refresh_token"), result.grantTypes()),
                () -> assertEquals(Set.of("https://app.example.com/callback"), result.redirectUris()),
                () -> assertEquals("ACTIVE", result.status()),
                () -> assertEquals(REGISTERED_AT, result.registeredAt()),
                () -> assertTrue(savedRegistration.secretHash().isEmpty())
        );
        InOrder order = inOrder(repository);
        order.verify(repository).existsById(TENANT_ID, clientId);
        order.verify(repository).save(savedRegistration);
        verifyNoInteractions(secretHasher);
    }

    /**
     * Given 未注册的机密客户端命令，When 执行注册，Then 应按查重、散列、保存顺序执行且不返回密钥。
     */
    @Test
    void shouldRegisterConfidentialClientWithoutExposingSecret() {
        // Given
        RegisterClientCommand command = confidentialCommand();
        ClientId clientId = new ClientId(command.clientId());
        ClientSecretHash secretHash = new ClientSecretHash("a".repeat(60));
        when(repository.existsById(TENANT_ID, clientId)).thenReturn(false);
        when(secretHasher.hash(RAW_SECRET)).thenReturn(secretHash);
        ArgumentCaptor<ClientRegistration> registrationCaptor = ArgumentCaptor.forClass(ClientRegistration.class);

        // When
        RegisteredClientResult result = service.registerConfidential(command, RAW_SECRET);

        // Then
        verify(repository).save(registrationCaptor.capture());
        ClientRegistration savedRegistration = registrationCaptor.getValue();
        assertAll(
                () -> assertEquals(Set.of("client_secret_basic"), result.authenticationMethods()),
                () -> assertEquals(Set.of("client_credentials"), result.grantTypes()),
                () -> assertEquals(secretHash, savedRegistration.secretHash().orElseThrow()),
                () -> assertFalse(result.toString().contains(RAW_SECRET))
        );
        InOrder order = inOrder(repository, secretHasher);
        order.verify(repository).existsById(TENANT_ID, clientId);
        order.verify(secretHasher).hash(RAW_SECRET);
        order.verify(repository).save(savedRegistration);
    }

    /**
     * Given 已存在的客户端标识，When 注册机密客户端，Then 应在散列和保存之前终止用例。
     */
    @Test
    void shouldRejectDuplicateClientBeforeHashingOrSaving() {
        // Given
        RegisterClientCommand command = confidentialCommand();
        ClientId clientId = new ClientId(command.clientId());
        when(repository.existsById(TENANT_ID, clientId)).thenReturn(true);

        // When
        IamException exception = assertThrows(IamException.class,
                () -> service.registerConfidential(command, RAW_SECRET));

        // Then
        assertEquals(IamErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(secretHasher);
    }

    /**
     * Given 缺失注册命令，When 执行注册，Then 应在访问输出端口之前返回稳定错误。
     */
    @Test
    void shouldRejectMissingCommandBeforeCallingPorts() {
        // When
        IamException exception = assertThrows(IamException.class, () -> service.registerPublic(null));

        // Then
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
        verifyNoInteractions(repository, secretHasher);
    }

    /**
     * Given 不支持的认证方式，When 转换命令，Then 应拒绝请求且不访问输出端口。
     */
    @Test
    void shouldRejectUnsupportedAuthenticationMethod() {
        // Given
        RegisterClientCommand command = command(
                "unsupported_method",
                "authorization_code",
                Set.of("https://app.example.com/callback")
        );

        // When
        IamException exception = assertThrows(IamException.class, () -> service.registerPublic(command));

        // Then
        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
        verifyNoInteractions(repository, secretHasher);
    }

    /**
     * Given 不支持的授权类型，When 转换命令，Then 应拒绝请求且不访问输出端口。
     */
    @Test
    void shouldRejectUnsupportedGrantType() {
        // Given
        RegisterClientCommand command = command("none", "unsupported_grant", Set.of());

        // When
        IamException exception = assertThrows(IamException.class, () -> service.registerPublic(command));

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
        RegisterClientCommand command = confidentialCommand();
        ClientId clientId = new ClientId(command.clientId());
        when(repository.existsById(TENANT_ID, clientId)).thenReturn(false);
        when(secretHasher.hash(RAW_SECRET)).thenThrow(new IllegalArgumentException("invalid secret"));

        // When
        assertThrows(IllegalArgumentException.class, () -> service.registerConfidential(command, RAW_SECRET));

        // Then
        verify(repository, never()).save(any());
    }

    private RegisterClientCommand publicCommand() {
        return new RegisterClientCommand(
                "default",
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid", "profile"),
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        );
    }

    private RegisterClientCommand confidentialCommand() {
        return new RegisterClientCommand(
                "default",
                "service-app",
                "Service App",
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of("service.read"),
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        );
    }

    private RegisterClientCommand command(String authenticationMethod, String grantType, Set<String> redirectUris) {
        return new RegisterClientCommand(
                "default",
                "test-client",
                "Test Client",
                Set.of(authenticationMethod),
                Set.of(grantType),
                redirectUris,
                Set.of("openid"),
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        );
    }
}
