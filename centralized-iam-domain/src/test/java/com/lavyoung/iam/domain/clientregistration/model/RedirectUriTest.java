package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端重定向地址的传输安全和 URI 结构边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class RedirectUriTest {

    /**
     * Given 安全的生产与本地回调地址，When 创建值对象，Then 应保留精确原始值。
     */
    @Test
    void shouldAcceptSecureAndLoopbackRedirectUris() {
        assertAll(
                () -> assertAccepted("https://iam.example.com/oauth2/callback"),
                () -> assertAccepted("https://iam.example.com/oauth2/callback?channel=web"),
                () -> assertAccepted("http://localhost:8080/oauth2/callback"),
                () -> assertAccepted("http://127.0.0.1:49152/oauth2/callback"),
                () -> assertAccepted("http://[::1]:49152/oauth2/callback")
        );
    }

    /**
     * Given 不安全或结构非法的地址，When 创建值对象，Then 应抛出稳定领域错误。
     */
    @Test
    void shouldRejectUnsafeOrMalformedRedirectUris() {
        assertAll(
                () -> assertInvalid(null),
                () -> assertInvalid(""),
                () -> assertInvalid("/oauth2/callback"),
                () -> assertInvalid("https:/oauth2/callback"),
                () -> assertInvalid("http://iam.example.com/oauth2/callback"),
                () -> assertInvalid("https://user@iam.example.com/oauth2/callback"),
                () -> assertInvalid("https://iam.example.com/oauth2/callback#result"),
                () -> assertInvalid("custom://iam.example.com/oauth2/callback"),
                () -> assertInvalid("https://iam.example.com/" + "a".repeat(2048))
        );
    }

    private void assertAccepted(String value) {
        assertEquals(value, new RedirectUri(value).value());
    }

    private void assertInvalid(String value) {
        IamException exception = assertThrows(IamException.class, () -> new RedirectUri(value));

        assertAll(
                () -> assertEquals(IamErrorCode.CLIENT_REDIRECT_URI_INVALID, exception.getErrorCode()),
                () -> assertEquals(List.of(2048), exception.getMessageArguments())
        );
    }
}
