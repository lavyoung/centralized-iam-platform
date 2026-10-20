package com.lavyoung.iam.domain.clientregistration.model;

/**
 * 表示 OAuth2/OIDC 客户端的生命周期状态。
 *
 * <p>状态属于客户端聚合的业务规则，不对应数据库删除标记，
 * 也不负责决定具体的认证协议响应。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public enum ClientStatus {

    /**
     * 客户端有效，允许参与认证和授权。
     */
    ACTIVE,

    /**
     * 客户端已停用，保留配置但拒绝新的认证请求。
     */
    DISABLED;

    /**
     * 判断当前状态是否允许客户端参与认证。
     *
     * @return 允许认证时返回 {@code true}
     */
    public boolean allowsAuthentication() {
        return this == ACTIVE;
    }
}
