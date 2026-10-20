package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * 客户端注册聚合根，负责客户端类型、生命周期状态和密钥轮换规则。
 *
 * <p>聚合不负责密码学散列、数据库持久化或 OAuth2 HTTP 响应转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public final class ClientRegistration {

    private final ClientRegistrationSpec specification;
    private final Instant registeredAt;
    private ClientSecretHash secretHash;
    private ClientStatus status;
    private Instant updatedAt;

    private ClientRegistration(ClientRegistrationSpec specification, ClientSecretHash secretHash, ClientLifecycleState lifecycleState) {
        this.specification = specification;
        this.secretHash = secretHash;
        this.status = lifecycleState.status();
        this.registeredAt = lifecycleState.registeredAt();
        this.updatedAt = lifecycleState.updatedAt();
    }

    /**
     * 注册不持有客户端密钥的公共客户端。
     *
     * @param specification 客户端注册配置
     * @param registeredAt  注册时间
     * @return 新的公共客户端聚合
     * @throws IamException 当认证方式或授权类型不适用于公共客户端时抛出
     */
    public static ClientRegistration registerPublic(ClientRegistrationSpec specification, Instant registeredAt) {
        requireRegistrationArguments(specification, registeredAt);
        validatePublicSpecification(specification);
        return new ClientRegistration(specification, null, ClientLifecycleState.initial(registeredAt));
    }

    /**
     * 注册持有密钥摘要的机密客户端。
     *
     * @param specification 客户端注册配置
     * @param secretHash    客户端密钥摘要
     * @param registeredAt  注册时间
     * @return 新的机密客户端聚合
     * @throws IamException 当密钥缺失或认证方式包含 {@code none} 时抛出
     */
    public static ClientRegistration registerConfidential(ClientRegistrationSpec specification, ClientSecretHash secretHash, Instant registeredAt) {
        requireRegistrationArguments(specification, registeredAt);
        validateConfidentialSpecification(specification, secretHash);
        return new ClientRegistration(specification, secretHash, ClientLifecycleState.initial(registeredAt));
    }

    /**
     * 从持久化状态恢复公共客户端。
     *
     * @param specification  客户端注册规格
     * @param lifecycleState 客户端生命周期状态
     * @return 恢复后的公共客户端
     * @throws IamException 当注册规格或生命周期状态无效时抛出
     */
    public static ClientRegistration restorePublic(ClientRegistrationSpec specification, ClientLifecycleState lifecycleState) {
        requireRestoreArguments(specification, lifecycleState);
        validatePublicSpecification(specification);
        return new ClientRegistration(specification, null, lifecycleState);
    }

    /**
     * 从持久化状态恢复机密客户端。
     *
     * @param specification  客户端注册规格
     * @param secretHash     客户端密钥哈希
     * @param lifecycleState 客户端生命周期状态
     * @return 恢复后的机密客户端
     * @throws IamException 当注册规格、密钥哈希或生命周期状态无效时抛出
     */
    public static ClientRegistration restoreConfidential(ClientRegistrationSpec specification, ClientSecretHash secretHash,
                                                         ClientLifecycleState lifecycleState) {
        requireRestoreArguments(specification, lifecycleState);
        validateConfidentialSpecification(specification, secretHash);
        return new ClientRegistration(specification, secretHash, lifecycleState);
    }

    private static void validatePublicSpecification(ClientRegistrationSpec specification) {
        Set<ClientAuthenticationMethod> methods = specification.authenticationMethods();
        if (!methods.equals(Set.of(ClientAuthenticationMethod.NONE))
                || specification.grantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
            throw invalidRegistration();
        }
    }

    private static void validateConfidentialSpecification(ClientRegistrationSpec specification, ClientSecretHash secretHash) {
        if (secretHash == null || specification.authenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw invalidRegistration();
        }
    }

    private static void requireRestoreArguments(ClientRegistrationSpec specification, ClientLifecycleState lifecycleState) {
        if (specification == null || lifecycleState == null) {
            throw invalidRegistration();
        }
    }

    /**
     * 停用客户端并拒绝其后续认证请求，重复停用不会改变更新时间。
     *
     * @param occurredAt 状态变更发生时间
     * @throws IamException 当时间为空或早于聚合最后更新时间时抛出
     */
    public void disable(Instant occurredAt) {
        changeStatus(ClientStatus.DISABLED, occurredAt);
    }

    /**
     * 重新启用客户端，重复启用不会改变更新时间。
     *
     * @param occurredAt 状态变更发生时间
     * @throws IamException 当时间为空或早于聚合最后更新时间时抛出
     */
    public void activate(Instant occurredAt) {
        changeStatus(ClientStatus.ACTIVE, occurredAt);
    }

    /**
     * 替换机密客户端的密钥摘要。
     *
     * @param newSecretHash 新的客户端密钥摘要
     * @param occurredAt    密钥轮换发生时间
     * @throws IamException 当客户端为公共客户端、摘要为空或时间非法时抛出
     */
    public void rotateSecret(ClientSecretHash newSecretHash, Instant occurredAt) {
        validateChangeTime(occurredAt);
        if (secretHash == null || newSecretHash == null) {
            throw invalidRegistration();
        }

        secretHash = newSecretHash;
        updatedAt = occurredAt;
    }

    /**
     * 判断客户端当前状态是否允许参与认证。
     *
     * @return 客户端处于有效状态时返回 {@code true}
     */
    public boolean allowsAuthentication() {
        return status.allowsAuthentication();
    }

    /**
     * 判断客户端是否已注册指定授权类型。
     *
     * @param grantType 待判断的授权类型
     * @return 已注册该授权类型时返回 {@code true}
     */
    public boolean supportsGrantType(AuthorizationGrantType grantType) {
        return grantType != null && specification.grantTypes().contains(grantType);
    }

    /**
     * 获取客户端不可变注册配置。
     *
     * @return 客户端注册配置
     */
    public ClientRegistrationSpec specification() {
        return specification;
    }

    /**
     * 获取机密客户端的密钥摘要，公共客户端返回空结果。
     *
     * @return 客户端密钥摘要
     */
    public Optional<ClientSecretHash> secretHash() {
        return Optional.ofNullable(secretHash);
    }

    /**
     * 获取客户端当前生命周期状态。
     *
     * @return 客户端状态
     */
    public ClientStatus status() {
        return status;
    }

    /**
     * 获取客户端首次注册时间。
     *
     * @return 注册时间
     */
    public Instant registeredAt() {
        return registeredAt;
    }

    /**
     * 获取客户端最近一次有效变更时间。
     *
     * @return 最近更新时间
     */
    public Instant updatedAt() {
        return updatedAt;
    }

    private void changeStatus(ClientStatus targetStatus, Instant occurredAt) {
        validateChangeTime(occurredAt);
        if (status != targetStatus) {
            status = targetStatus;
            updatedAt = occurredAt;
        }
    }

    private void validateChangeTime(Instant occurredAt) {
        if (occurredAt == null || occurredAt.isBefore(updatedAt)) {
            throw invalidRegistration();
        }
    }

    private static void requireRegistrationArguments(ClientRegistrationSpec specification, Instant registeredAt) {
        if (specification == null || registeredAt == null) {
            throw invalidRegistration();
        }
    }

    private static IamException invalidRegistration() {
        return new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
    }
}
