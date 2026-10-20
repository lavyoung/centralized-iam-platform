package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.assembler;

import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.model.ClientRegistrationPersistenceSnapshot;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;
import com.lavyoung.iam.domain.clientregistration.model.*;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户端注册领域模型与持久化对象之间的转换器。
 *
 * <p>负责技术字段展开、协议值转换和聚合重建，不执行数据库查询，
 * 也不修改客户端注册领域规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Component
public class ClientRegistrationPersistenceAssembler {

    /**
     * 将客户端聚合转换为主表持久化对象。
     *
     * @param registration 客户端注册聚合
     * @return 主表持久化对象
     * @throws NullPointerException 当客户端注册聚合为空时抛出
     */
    public ClientRegistrationPO toRegistrationPO(ClientRegistration registration) {
        Objects.requireNonNull(registration);

        ClientRegistrationSpec specification = registration.specification();
        ClientTokenPolicy tokenPolicy = specification.tokenPolicy();
        ClientRegistrationPO result = new ClientRegistrationPO();

        result.setTenantId(specification.tenantId().value());
        result.setClientId(specification.clientId().value());
        result.setDisplayName(specification.displayName());
        result.setClientSecretHash(registration.secretHash().map(ClientSecretHash::value).orElse(null));
        result.setStatus(registration.status().name());
        result.setAccessTokenTtlSeconds(tokenPolicy.accessTokenTimeToLive().toSeconds());
        result.setRefreshTokenTtlSeconds(tokenPolicy.refreshTokenTimeToLive().toSeconds());
        result.setAuthorizationCodeTtlSeconds(tokenPolicy.authorizationCodeTimeToLive().toSeconds());
        result.setRegisteredAt(registration.registeredAt());
        result.setUpdatedAt(registration.updatedAt());
        return result;
    }

    /**
     * 将认证方式转换为关联表持久化对象。
     *
     * @param registration         客户端注册聚合
     * @param clientRegistrationId 主表技术主键
     * @return 认证方式持久化对象列表
     * @throws NullPointerException 当聚合或主表技术主键为空时抛出
     */
    public List<ClientAuthenticationMethodPO> toAuthenticationMethodPOs(ClientRegistration registration, Long clientRegistrationId) {
        Objects.requireNonNull(registration);
        Objects.requireNonNull(clientRegistrationId);

        return registration.specification().authenticationMethods().stream()
                .sorted()
                .map(method -> authenticationMethodPO(registration, clientRegistrationId, method))
                .toList();
    }

    /**
     * 将授权类型转换为关联表持久化对象。
     *
     * @param registration         客户端注册聚合
     * @param clientRegistrationId 主表技术主键
     * @return 授权类型持久化对象列表
     * @throws NullPointerException 当聚合或主表技术主键为空时抛出
     */
    public List<ClientAuthorizationGrantTypePO> toGrantTypePOs(ClientRegistration registration, Long clientRegistrationId) {
        Objects.requireNonNull(registration);
        Objects.requireNonNull(clientRegistrationId);

        return registration.specification().grantTypes().stream()
                .sorted()
                .map(grantType -> grantTypePO(registration, clientRegistrationId, grantType))
                .toList();
    }

    /**
     * 将回调地址转换为关联表持久化对象。
     *
     * @param registration         客户端注册聚合
     * @param clientRegistrationId 主表技术主键
     * @return 回调地址持久化对象列表
     * @throws NullPointerException 当聚合或主表技术主键为空时抛出
     */
    public List<ClientRedirectUriPO> toRedirectUriPOs(ClientRegistration registration, Long clientRegistrationId) {
        Objects.requireNonNull(registration);
        Objects.requireNonNull(clientRegistrationId);

        return registration.specification().redirectUris().stream()
                .sorted((left, right) -> left.value().compareTo(right.value()))
                .map(redirectUri -> redirectUriPO(registration, clientRegistrationId, redirectUri))
                .toList();
    }

    /**
     * 将 scope 转换为关联表持久化对象。
     *
     * @param registration         客户端注册聚合
     * @param clientRegistrationId 主表技术主键
     * @return scope 持久化对象列表
     * @throws NullPointerException 当聚合或主表技术主键为空时抛出
     */
    public List<ClientScopePO> toScopePOs(ClientRegistration registration, Long clientRegistrationId) {
        Objects.requireNonNull(registration);
        Objects.requireNonNull(clientRegistrationId);

        return registration.specification().scopes().stream()
                .sorted()
                .map(scope -> scopePO(registration, clientRegistrationId, scope))
                .toList();
    }

    /**
     * 将完整持久化快照恢复为客户端注册聚合。
     *
     * @param snapshot 客户端注册持久化快照
     * @return 恢复后的客户端注册聚合
     * @throws NullPointerException          当持久化快照为空时抛出
     * @throws DataRetrievalFailureException 当数据库中的枚举协议值无法识别时抛出
     */
    public ClientRegistration toDomain(ClientRegistrationPersistenceSnapshot snapshot) {
        Objects.requireNonNull(snapshot);

        ClientRegistrationPO registrationPO = snapshot.registration();
        ClientRegistrationSpec specification = toSpecification(snapshot);
        ClientLifecycleState lifecycleState = new ClientLifecycleState(
                parseStatus(registrationPO.getStatus()),
                registrationPO.getRegisteredAt(),
                registrationPO.getUpdatedAt()
        );

        String secretHash = registrationPO.getClientSecretHash();
        if (secretHash == null) {
            return ClientRegistration.restorePublic(specification, lifecycleState);
        }
        return ClientRegistration.restoreConfidential(specification, new ClientSecretHash(secretHash), lifecycleState);
    }

    private ClientRegistrationSpec toSpecification(ClientRegistrationPersistenceSnapshot snapshot) {
        ClientRegistrationPO registrationPO = snapshot.registration();
        ClientTokenPolicy tokenPolicy = new ClientTokenPolicy(
                Duration.ofSeconds(registrationPO.getAccessTokenTtlSeconds()),
                Duration.ofSeconds(registrationPO.getRefreshTokenTtlSeconds()),
                Duration.ofSeconds(registrationPO.getAuthorizationCodeTtlSeconds())
        );

        return new ClientRegistrationSpec(
                new TenantId(registrationPO.getTenantId()),
                new ClientId(registrationPO.getClientId()),
                registrationPO.getDisplayName(),
                toAuthenticationMethods(snapshot.authenticationMethods()),
                toGrantTypes(snapshot.grantTypes()),
                toRedirectUris(snapshot.redirectUris()),
                toScopes(snapshot.scopes()),
                tokenPolicy
        );
    }

    private Set<ClientAuthenticationMethod> toAuthenticationMethods(List<ClientAuthenticationMethodPO> rows) {
        return rows.stream()
                .map(ClientAuthenticationMethodPO::getAuthenticationMethod)
                .map(this::parseAuthenticationMethod)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<AuthorizationGrantType> toGrantTypes(List<ClientAuthorizationGrantTypePO> rows) {
        return rows.stream()
                .map(ClientAuthorizationGrantTypePO::getGrantType)
                .map(this::parseGrantType)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<RedirectUri> toRedirectUris(List<ClientRedirectUriPO> rows) {
        return rows.stream()
                .map(ClientRedirectUriPO::getRedirectUri)
                .map(RedirectUri::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> toScopes(List<ClientScopePO> rows) {
        return rows.stream()
                .map(ClientScopePO::getScope)
                .collect(Collectors.toUnmodifiableSet());
    }

    private ClientAuthenticationMethod parseAuthenticationMethod(String value) {
        return ClientAuthenticationMethod.fromMethod(value).orElseThrow(this::corruptedData);
    }

    private AuthorizationGrantType parseGrantType(String value) {
        return AuthorizationGrantType.fromProtocolValue(value).orElseThrow(this::corruptedData);
    }

    private ClientStatus parseStatus(String value) {
        try {
            return ClientStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw corruptedData();
        }
    }

    private ClientAuthenticationMethodPO authenticationMethodPO(ClientRegistration registration, Long clientRegistrationId, ClientAuthenticationMethod method) {
        ClientAuthenticationMethodPO result = new ClientAuthenticationMethodPO();
        result.setTenantId(tenantIdOf(registration));
        result.setClientRegistrationId(clientRegistrationId);
        result.setAuthenticationMethod(method.getMethod());
        return result;
    }

    private ClientAuthorizationGrantTypePO grantTypePO(ClientRegistration registration, Long clientRegistrationId, AuthorizationGrantType grantType) {
        ClientAuthorizationGrantTypePO result = new ClientAuthorizationGrantTypePO();
        result.setTenantId(tenantIdOf(registration));
        result.setClientRegistrationId(clientRegistrationId);
        result.setGrantType(grantType.getCode());
        return result;
    }

    private ClientRedirectUriPO redirectUriPO(ClientRegistration registration, Long clientRegistrationId, RedirectUri redirectUri) {
        ClientRedirectUriPO result = new ClientRedirectUriPO();
        result.setTenantId(tenantIdOf(registration));
        result.setClientRegistrationId(clientRegistrationId);
        result.setRedirectUri(redirectUri.value());
        return result;
    }

    private ClientScopePO scopePO(ClientRegistration registration, Long clientRegistrationId, String scope) {
        ClientScopePO result = new ClientScopePO();
        result.setTenantId(tenantIdOf(registration));
        result.setClientRegistrationId(clientRegistrationId);
        result.setScope(scope);
        return result;
    }

    private String tenantIdOf(ClientRegistration registration) {
        return registration.specification().tenantId().value();
    }

    private DataRetrievalFailureException corruptedData() {
        return new DataRetrievalFailureException("Invalid persisted client registration");
    }
}
