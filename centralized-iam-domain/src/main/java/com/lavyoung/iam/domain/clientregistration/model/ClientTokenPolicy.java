package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.time.Duration;

/**
 * 表示单个客户端申请的令牌生命周期策略。
 *
 * @param accessTokenTimeToLive       访问令牌有效期
 * @param refreshTokenTimeToLive      刷新令牌有效期
 * @param authorizationCodeTimeToLive 授权码有效期
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record ClientTokenPolicy(
        Duration accessTokenTimeToLive,
        Duration refreshTokenTimeToLive,
        Duration authorizationCodeTimeToLive
) {

    /**
     * 创建令牌生命周期策略。
     *
     * @param accessTokenTimeToLive       访问令牌有效期
     * @param refreshTokenTimeToLive      刷新令牌有效期
     * @param authorizationCodeTimeToLive 授权码有效期
     * @throws IamException 当生命周期关系不合法时抛出
     */
    public ClientTokenPolicy {
        if (!isPositive(accessTokenTimeToLive)
                || !isPositive(refreshTokenTimeToLive)
                || !isPositive(authorizationCodeTimeToLive)
                || refreshTokenTimeToLive.compareTo(accessTokenTimeToLive) <= 0
                || authorizationCodeTimeToLive.compareTo(accessTokenTimeToLive) >= 0) {
            throw new IamException(IamErrorCode.CLIENT_TOKEN_POLICY_INVALID);
        }
    }

    /**
     * 创建项目首期采用的安全默认策略。
     *
     * @return 默认令牌生命周期策略
     */
    public static ClientTokenPolicy standard() {
        return new ClientTokenPolicy(
                Duration.ofMinutes(15),
                Duration.ofHours(8),
                Duration.ofMinutes(5)
        );
    }

    private static boolean isPositive(Duration duration) {
        return duration != null
                && !duration.isZero()
                && !duration.isNegative();
    }
}