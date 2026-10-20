package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端令牌策略的默认值与生命周期相对关系。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientTokenPolicyTest {

    /**
     * Given 系统默认策略，When 读取生命周期，Then 应采用首期安全默认值。
     */
    @Test
    void shouldProvideStandardTokenPolicy() {
        ClientTokenPolicy policy = ClientTokenPolicy.standard();

        assertAll(
                () -> assertEquals(Duration.ofMinutes(15), policy.accessTokenTimeToLive()),
                () -> assertEquals(Duration.ofHours(8), policy.refreshTokenTimeToLive()),
                () -> assertEquals(Duration.ofMinutes(5), policy.authorizationCodeTimeToLive())
        );
    }

    /**
     * Given 满足相对关系的生命周期，When 创建策略，Then 应保留全部配置。
     */
    @Test
    void shouldAcceptValidCustomTokenPolicy() {
        ClientTokenPolicy policy = new ClientTokenPolicy(Duration.ofMinutes(30), Duration.ofDays(1), Duration.ofMinutes(10));

        assertAll(
                () -> assertEquals(Duration.ofMinutes(30), policy.accessTokenTimeToLive()),
                () -> assertEquals(Duration.ofDays(1), policy.refreshTokenTimeToLive()),
                () -> assertEquals(Duration.ofMinutes(10), policy.authorizationCodeTimeToLive())
        );
    }

    /**
     * Given 空值、非正数或次序错误的生命周期，When 创建策略，Then 应拒绝非法策略。
     */
    @Test
    void shouldRejectInvalidTokenPolicy() {
        Duration accessTokenTimeToLive = Duration.ofMinutes(15);
        Duration refreshTokenTimeToLive = Duration.ofHours(8);
        Duration authorizationCodeTimeToLive = Duration.ofMinutes(5);

        assertAll(
                () -> assertInvalid(null, refreshTokenTimeToLive, authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, null, authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, refreshTokenTimeToLive, null),
                () -> assertInvalid(Duration.ZERO, refreshTokenTimeToLive, authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, Duration.ZERO, authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, refreshTokenTimeToLive, Duration.ZERO),
                () -> assertInvalid(accessTokenTimeToLive, accessTokenTimeToLive, authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, Duration.ofMinutes(10), authorizationCodeTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, refreshTokenTimeToLive, accessTokenTimeToLive),
                () -> assertInvalid(accessTokenTimeToLive, refreshTokenTimeToLive, Duration.ofMinutes(20))
        );
    }

    private void assertInvalid(Duration accessTokenTimeToLive, Duration refreshTokenTimeToLive, Duration authorizationCodeTimeToLive) {
        Executable creation = () -> new ClientTokenPolicy(accessTokenTimeToLive, refreshTokenTimeToLive, authorizationCodeTimeToLive);
        IamException exception = assertThrows(IamException.class, creation);

        assertAll(
                () -> assertEquals(IamErrorCode.CLIENT_TOKEN_POLICY_INVALID, exception.getErrorCode()),
                () -> assertEquals(List.of(), exception.getMessageArguments())
        );
    }
}
