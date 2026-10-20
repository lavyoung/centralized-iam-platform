package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 OAuth2/OIDC 客户端标识的格式边界与大小写敏感值语义。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientIdTest {

    /**
     * Given 最短、常用和最长客户端标识，When 创建值对象，Then 应保留原始值。
     */
    @Test
    void shouldAcceptValidClientIdentifiers() {
        assertAll(
                () -> assertEquals("app", new ClientId("app").value()),
                () -> assertEquals("Client.App_01~prod", new ClientId("Client.App_01~prod").value()),
                () -> assertEquals("a".repeat(128), new ClientId("a".repeat(128)).value())
        );
    }

    /**
     * Given 仅大小写不同的标识，When 比较客户端标识，Then 两者必须保持不同。
     */
    @Test
    void shouldKeepCaseSensitiveValueSemantics() {
        assertNotEquals(new ClientId("client-app"), new ClientId("Client-app"));
    }

    /**
     * Given 非法客户端标识，When 创建值对象，Then 应携带长度参数抛出稳定错误。
     */
    @Test
    void shouldRejectInvalidClientIdentifiers() {
        assertAll(
                () -> assertInvalid(null),
                () -> assertInvalid("ab"),
                () -> assertInvalid("-client"),
                () -> assertInvalid("client app"),
                () -> assertInvalid("client/app"),
                () -> assertInvalid("a".repeat(129))
        );
    }

    private void assertInvalid(String value) {
        IamException exception = assertThrows(IamException.class, () -> new ClientId(value));

        assertAll(
                () -> assertEquals(IamErrorCode.CLIENT_ID_INVALID, exception.getErrorCode()),
                () -> assertEquals(List.of(3, 128), exception.getMessageArguments())
        );
    }
}
