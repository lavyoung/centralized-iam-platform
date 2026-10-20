package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 客户端注册主表持久化对象。
 *
 * <p>只负责映射 {@code iam_client_registration} 表中的单值字段，
 * 不承载客户端注册规则，也不直接作为领域模型或接口响应使用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("iam_client_registration")
public class ClientRegistrationPO {

    /**
     * 数据库技术主键，由 MyBatis-Plus 在应用侧生成。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 客户端所属租户标识。
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * OAuth2/OIDC 客户端标识。
     */
    @TableField("client_id")
    private String clientId;

    /**
     * 客户端展示名称。
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 机密客户端的密钥摘要，公共客户端为空。
     */
    @TableField("client_secret_hash")
    private String clientSecretHash;

    /**
     * 客户端生命周期状态。
     */
    @TableField("status")
    private String status;

    /**
     * Access Token 有效秒数。
     */
    @TableField("access_token_ttl_seconds")
    private Long accessTokenTtlSeconds;

    /**
     * Refresh Token 有效秒数。
     */
    @TableField("refresh_token_ttl_seconds")
    private Long refreshTokenTtlSeconds;

    /**
     * Authorization Code 有效秒数。
     */
    @TableField("authorization_code_ttl_seconds")
    private Long authorizationCodeTtlSeconds;

    /**
     * 客户端首次注册时间。
     */
    @TableField("registered_at")
    private Instant registeredAt;

    /**
     * 客户端最近更新时间。
     */
    @TableField("updated_at")
    private Instant updatedAt;
}
