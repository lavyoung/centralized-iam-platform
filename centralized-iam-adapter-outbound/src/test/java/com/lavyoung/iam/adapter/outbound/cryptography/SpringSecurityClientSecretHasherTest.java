package com.lavyoung.iam.adapter.outbound.cryptography;

import com.lavyoung.iam.domain.clientregistration.model.ClientSecretHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证客户端密钥散列适配器与 Spring Security 密码编码器之间的防腐转换。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@ExtendWith(MockitoExtension.class)
class SpringSecurityClientSecretHasherTest {

    private static final String RAW_SECRET = "client-secret-value";
    private static final String ENCODED_SECRET = "{bcrypt}" + "a".repeat(60);

    @Mock
    private PasswordEncoder passwordEncoder;

    private SpringSecurityClientSecretHasher secretHasher;

    /**
     * 使用密码编码器 mock 创建待测出站适配器。
     */
    @BeforeEach
    void setUp() {
        secretHasher = new SpringSecurityClientSecretHasher(passwordEncoder);
    }

    /**
     * Given 合法明文和编码结果，When 生成摘要，Then 应委托编码器并包装领域值对象。
     */
    @Test
    void shouldEncodeRawSecretAndReturnDomainHash() {
        // Given
        when(passwordEncoder.encode(RAW_SECRET)).thenReturn(ENCODED_SECRET);

        // When
        ClientSecretHash secretHash = secretHasher.hash(RAW_SECRET);

        // Then
        assertAll(
                () -> assertEquals(ENCODED_SECRET, secretHash.value()),
                () -> assertFalse(secretHash.value().contains(RAW_SECRET))
        );
        verify(passwordEncoder).encode(RAW_SECRET);
    }

    /**
     * Given 明文和已有摘要，When 校验密钥，Then 应使用编码器返回匹配结果。
     */
    @Test
    void shouldDelegateSecretMatching() {
        // Given
        ClientSecretHash secretHash = new ClientSecretHash(ENCODED_SECRET);
        when(passwordEncoder.matches(RAW_SECRET, ENCODED_SECRET)).thenReturn(true);

        // When
        boolean matched = secretHasher.matches(RAW_SECRET, secretHash);

        // Then
        assertTrue(matched);
        verify(passwordEncoder).matches(RAW_SECRET, ENCODED_SECRET);
    }

    /**
     * Given 空值或空白明文，When 散列或匹配，Then 应在调用密码编码器前拒绝输入。
     */
    @Test
    void shouldRejectBlankRawSecretBeforeCallingEncoder() {
        ClientSecretHash secretHash = new ClientSecretHash(ENCODED_SECRET);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> secretHasher.hash(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> secretHasher.hash("")),
                () -> assertThrows(IllegalArgumentException.class, () -> secretHasher.hash("   ")),
                () -> assertThrows(IllegalArgumentException.class, () -> secretHasher.matches(" ", secretHash))
        );
        verifyNoInteractions(passwordEncoder);
    }

    /**
     * Given 缺失的已有摘要，When 校验密钥，Then 应在调用密码编码器前拒绝输入。
     */
    @Test
    void shouldRejectMissingSecretHashBeforeCallingEncoder() {
        assertThrows(IllegalArgumentException.class, () -> secretHasher.matches(RAW_SECRET, null));

        verifyNoInteractions(passwordEncoder);
    }
}
