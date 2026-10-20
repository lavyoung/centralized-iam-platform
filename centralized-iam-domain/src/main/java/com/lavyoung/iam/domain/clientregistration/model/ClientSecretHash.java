package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

/**
 * 表示由密码学端口生成的客户端密钥摘要。
 *
 * @param value 编码后的摘要，不得传入客户端明文密钥
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientSecretHash(String value) {

    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 512;

    /**
     * 校验并创建客户端密钥摘要。
     *
     * @param value 编码后的摘要
     * @throws IamException 当摘要为空或长度越界时抛出
     */
    public ClientSecretHash {
        if (value == null || value.isBlank() || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IamException(IamErrorCode.CLIENT_SECRET_HASH_INVALID, MIN_LENGTH, MAX_LENGTH);
        }
    }
}