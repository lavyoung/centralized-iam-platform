package com.lavyoung.iam.application.clientregistration.port.out;

import com.lavyoung.iam.domain.clientregistration.model.ClientSecretHash;

/**
 * 客户端密钥散列与验证输出端口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public interface ClientSecretHasher {

    /**
     * 将客户端明文密钥转换为不可逆摘要。
     *
     * @param rawSecret 客户端明文密钥，只允许在调用期间短暂存在
     * @return 可安全持久化的客户端密钥摘要
     * @throws IllegalArgumentException 当明文密钥为空时抛出
     */
    ClientSecretHash hash(CharSequence rawSecret);

    /**
     * 使用密码学实现校验明文密钥是否与已有摘要匹配。
     *
     * @param rawSecret  客户端提交的明文密钥
     * @param secretHash 已持久化的客户端密钥摘要
     * @return 密钥匹配时返回 {@code true}
     * @throws IllegalArgumentException 当明文密钥或摘要为空时抛出
     */
    boolean matches(CharSequence rawSecret, ClientSecretHash secretHash);
}
