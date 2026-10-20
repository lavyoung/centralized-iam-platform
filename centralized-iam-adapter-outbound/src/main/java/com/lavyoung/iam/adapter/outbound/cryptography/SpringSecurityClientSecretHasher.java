package com.lavyoung.iam.adapter.outbound.cryptography;

import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.domain.clientregistration.model.ClientSecretHash;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 使用 Spring Security 密码学能力实现客户端密钥散列端口。
 *
 * <p>本适配器只接触明文密钥完成单向散列或匹配，
 * 不持久化、不记录也不返回客户端明文密钥。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Component
@RequiredArgsConstructor
public class SpringSecurityClientSecretHasher implements ClientSecretHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public ClientSecretHash hash(CharSequence rawSecret) {
        requireRawSecret(rawSecret);
        return new ClientSecretHash(passwordEncoder.encode(rawSecret));
    }

    @Override
    public boolean matches(CharSequence rawSecret, ClientSecretHash secretHash) {
        requireRawSecret(rawSecret);
        if (secretHash == null) {
            throw new IllegalArgumentException("secretHash");
        }
        return passwordEncoder.matches(rawSecret, secretHash.value());
    }

    private static void requireRawSecret(CharSequence rawSecret) {
        if (!StringUtils.hasText(rawSecret)) {
            throw new IllegalArgumentException("rawSecret");
        }
    }
}
