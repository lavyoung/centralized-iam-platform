package com.lavyoung.iam.domain.clientregistration.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证客户端生命周期状态对认证行为的领域约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientStatusTest {

    /**
     * Given 有效客户端状态，When 判断是否允许认证，Then 应允许进入认证流程。
     */
    @Test
    void shouldAllowAuthenticationForActiveClient() {
        // Given
        ClientStatus status = ClientStatus.ACTIVE;

        // When
        boolean allowed = status.allowsAuthentication();

        // Then
        assertTrue(allowed);
    }

    /**
     * Given 停用客户端状态，When 判断是否允许认证，Then 应拒绝进入认证流程。
     */
    @Test
    void shouldRejectAuthenticationForDisabledClient() {
        // Given
        ClientStatus status = ClientStatus.DISABLED;

        // When
        boolean allowed = status.allowsAuthentication();

        // Then
        assertFalse(allowed);
    }
}
