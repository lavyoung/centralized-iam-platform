package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.util.regex.Pattern;

/**
 * 表示 OAuth2/OIDC 客户端公开标识。
 *
 * <p>客户端标识区分大小写并保持原始值，不包含客户端密钥。</p>
 *
 * @param value 客户端标识原始值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientId(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 128;
    private static final Pattern VALUE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._~-]{2,127}");

    /**
     * 校验并创建客户端标识。
     *
     * @param value 客户端标识原始值
     * @throws IamException 当标识为空、格式非法或长度越界时抛出
     */
    public ClientId {
        if (value == null || !VALUE_PATTERN.matcher(value).matches()) {
            throw new IamException(IamErrorCode.CLIENT_ID_INVALID, MIN_LENGTH, MAX_LENGTH);
        }
    }
}