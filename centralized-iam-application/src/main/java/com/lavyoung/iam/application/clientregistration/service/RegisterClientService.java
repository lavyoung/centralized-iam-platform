package com.lavyoung.iam.application.clientregistration.service;

import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.application.clientregistration.port.in.RegisterClientUseCase;
import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.domain.clientregistration.model.*;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 编排客户端注册用例。
 *
 * <p>本服务负责命令转换、客户端查重、密钥散列、聚合创建和持久化。
 * 客户端配置与生命周期不变量仍由领域模型维护。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RegisterClientService implements RegisterClientUseCase {

    private final ClientRegistrationRepository repository;
    private final ClientSecretHasher secretHasher;
    private final Clock clock;

    @Override
    public RegisteredClientResult registerPublic(RegisterClientCommand command) {
        ClientRegistrationSpec specification = toSpecification(command);
        ensureClientIdAvailable(specification);

        ClientRegistration registration = ClientRegistration.registerPublic(specification, clock.instant());
        repository.save(registration);
        return toResult(registration);
    }

    @Override
    public RegisteredClientResult registerConfidential(RegisterClientCommand command, CharSequence rawSecret) {
        ClientRegistrationSpec specification = toSpecification(command);
        ensureClientIdAvailable(specification);

        ClientSecretHash secretHash = secretHasher.hash(rawSecret);
        ClientRegistration registration = ClientRegistration.registerConfidential(
                specification,
                secretHash,
                clock.instant()
        );

        repository.save(registration);
        return toResult(registration);
    }

    /**
     * 将应用命令转换为经过领域类型校验的注册配置。
     *
     * @param command 客户端注册命令
     * @return 客户端注册领域配置
     * @throws IamException 当命令为空或协议值无法识别时抛出
     */
    private ClientRegistrationSpec toSpecification(RegisterClientCommand command) {
        if (command == null) {
            throw invalidRegistration();
        }

        ClientTokenPolicy tokenPolicy = new ClientTokenPolicy(
                command.accessTokenTimeToLive(),
                command.refreshTokenTimeToLive(),
                command.authorizationCodeTimeToLive()
        );

        return new ClientRegistrationSpec(
                new TenantId(command.tenantId()),
                new ClientId(command.clientId()),
                command.displayName(),
                toAuthenticationMethods(command.authenticationMethods()),
                toGrantTypes(command.grantTypes()),
                toRedirectUris(command.redirectUris()),
                command.scopes(),
                tokenPolicy
        );
    }

    private Set<ClientAuthenticationMethod> toAuthenticationMethods(Set<String> values) {
        return values.stream()
                .map(value -> ClientAuthenticationMethod.fromMethod(value)
                        .orElseThrow(RegisterClientService::invalidRegistration))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<AuthorizationGrantType> toGrantTypes(Set<String> values) {
        return values.stream()
                .map(value -> AuthorizationGrantType.fromProtocolValue(value)
                        .orElseThrow(RegisterClientService::invalidRegistration))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<RedirectUri> toRedirectUris(Set<String> values) {
        return values.stream()
                .map(RedirectUri::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void ensureClientIdAvailable(ClientRegistrationSpec specification) {
        boolean exists = repository.existsById(specification.tenantId(), specification.clientId());
        if (exists) {
            throw new IamException(IamErrorCode.RESOURCE_CONFLICT);
        }
    }

    /**
     * 将领域聚合转换为不包含敏感字段的应用结果。
     *
     * @param registration 已保存的客户端注册聚合
     * @return 可返回给入站适配器的应用结果
     */
    private RegisteredClientResult toResult(ClientRegistration registration) {
        ClientRegistrationSpec specification = registration.specification();

        return new RegisteredClientResult(
                specification.tenantId().value(),
                specification.clientId().value(),
                specification.displayName(),
                specification.authenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getMethod)
                        .collect(Collectors.toUnmodifiableSet()),
                specification.grantTypes().stream()
                        .map(AuthorizationGrantType::getCode)
                        .collect(Collectors.toUnmodifiableSet()),
                specification.redirectUris().stream()
                        .map(RedirectUri::value)
                        .collect(Collectors.toUnmodifiableSet()),
                specification.scopes(),
                specification.tokenPolicy().accessTokenTimeToLive(),
                specification.tokenPolicy().refreshTokenTimeToLive(),
                specification.tokenPolicy().authorizationCodeTimeToLive(),
                registration.status().name(),
                registration.registeredAt(),
                registration.updatedAt()
        );
    }

    private static IamException invalidRegistration() {
        return new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
    }
}