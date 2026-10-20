package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 客户端认证方式关联表持久化对象。
 *
 * <p>每条记录表示客户端注册聚合允许使用的一种客户端认证方式。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("iam_client_authentication_method")
public class ClientAuthenticationMethodPO {

    /**
     * 数据库技术主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 数据所属租户标识。
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 客户端注册主表技术主键。
     */
    @TableField("client_registration_id")
    private Long clientRegistrationId;

    /**
     * 客户端认证方式。
     */
    @TableField("authentication_method")
    private String authenticationMethod;
}
