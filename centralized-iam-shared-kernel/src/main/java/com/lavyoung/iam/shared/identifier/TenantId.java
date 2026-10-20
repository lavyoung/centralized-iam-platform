package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.util.regex.Pattern;

/**
 * IAM 平台中的租户唯一标识。
 *
 * <p>首期采用单租户模式，但所有租户归属数据仍通过该值对象显式携带
 * 租户边界，避免后续扩展多租户时重新修改核心领域模型。</p>
 *
 * @param value 租户标识原始值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record TenantId(String value) {

    private static final Pattern VALUE_PATTERN = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?");
    /**
     * 首期单租户模式使用的默认租户。
     */
    public static final TenantId DEFAULT_ID = new TenantId("default");

    /**
     * 校验并创建租户标识。
     *
     * @param value 租户标识原始值
     * @throws IamException 当标识为空、格式非法或超过长度限制时抛出
     */
    public TenantId {
        if (value == null || !VALUE_PATTERN.matcher(value).matches()) {
            throw new IamException(IamErrorCode.TENANT_ID_INVALID, 1, 64);
        }
    }
}
