package com.lavyoung.iam.domain.clientregistration.repository;

import com.lavyoung.iam.domain.clientregistration.model.ClientRegistration;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;

import java.util.Optional;

/**
 * 客户端注册聚合的持久化输出端口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public interface ClientRegistrationRepository {

    /**
     * 根据租户标识和客户端标识查询客户端注册聚合。
     *
     * @param tenantId 客户端所属租户标识
     * @param clientId OAuth2/OIDC 客户端标识
     * @return 找到时返回客户端注册聚合，否则返回空结果
     */
    Optional<ClientRegistration> findById(TenantId tenantId, ClientId clientId);

    /**
     * 判断指定租户下是否已经存在相同客户端标识。
     *
     * @param tenantId 客户端所属租户标识
     * @param clientId OAuth2/OIDC 客户端标识
     * @return 已存在时返回 {@code true}
     */
    boolean existsById(TenantId tenantId, ClientId clientId);

    /**
     * 保存新增或发生状态变化的客户端注册聚合。
     *
     * @param registration 待持久化的客户端注册聚合
     */
    void save(ClientRegistration registration);
}
