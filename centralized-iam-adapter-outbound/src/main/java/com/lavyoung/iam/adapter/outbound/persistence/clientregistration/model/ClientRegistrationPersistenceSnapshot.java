package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.model;

import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;

import java.util.List;
import java.util.Objects;

/**
 * 客户端注册聚合的完整持久化快照。
 *
 * <p>用于在 Repository Adapter 与持久化转换器之间传递主表记录及其关联记录，
 * 不作为领域模型或接口 DTO 使用。</p>
 *
 * @param registration          主表记录
 * @param authenticationMethods 客户端认证方式记录
 * @param grantTypes            客户端授权类型记录
 * @param redirectUris          客户端回调地址记录
 * @param scopes                客户端 scope 记录
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientRegistrationPersistenceSnapshot(
        ClientRegistrationPO registration,
        List<ClientAuthenticationMethodPO> authenticationMethods,
        List<ClientAuthorizationGrantTypePO> grantTypes,
        List<ClientRedirectUriPO> redirectUris,
        List<ClientScopePO> scopes
) {

    /**
     * 创建不可变持久化快照。
     *
     * @throws NullPointerException 当主表记录或任一关联记录集合为空时抛出
     */
    public ClientRegistrationPersistenceSnapshot {
        registration = Objects.requireNonNull(registration);
        authenticationMethods = List.copyOf(Objects.requireNonNull(authenticationMethods));
        grantTypes = List.copyOf(Objects.requireNonNull(grantTypes));
        redirectUris = List.copyOf(Objects.requireNonNull(redirectUris));
        scopes = List.copyOf(Objects.requireNonNull(scopes));
    }
}
