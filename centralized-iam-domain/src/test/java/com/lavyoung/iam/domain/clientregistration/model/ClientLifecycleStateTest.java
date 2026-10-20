package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端生命周期状态的初始化与时间顺序约束。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientLifecycleStateTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");

    /**
     * Given 客户端注册时间，When 创建初始生命周期，Then 状态应启用且两个时间一致。
     */
    @Test
    void shouldCreateInitialLifecycleState() {
        ClientLifecycleState state = ClientLifecycleState.initial(REGISTERED_AT);

        assertAll(
                () -> assertEquals(ClientStatus.ACTIVE, state.status()),
                () -> assertEquals(REGISTERED_AT, state.registeredAt()),
                () -> assertEquals(REGISTERED_AT, state.updatedAt())
        );
    }

    /**
     * Given 已停用客户端的历史时间，When 重建生命周期，Then 应保留状态和时间。
     */
    @Test
    void shouldRetainPersistedLifecycleState() {
        Instant updatedAt = REGISTERED_AT.plusSeconds(60);

        ClientLifecycleState state = new ClientLifecycleState(ClientStatus.DISABLED, REGISTERED_AT, updatedAt);

        assertAll(
                () -> assertEquals(ClientStatus.DISABLED, state.status()),
                () -> assertEquals(REGISTERED_AT, state.registeredAt()),
                () -> assertEquals(updatedAt, state.updatedAt())
        );
    }

    /**
     * Given 缺失的生命周期属性，When 创建生命周期，Then 应拒绝不完整状态。
     */
    @Test
    void shouldRejectMissingLifecycleAttributes() {
        assertAll(
                () -> assertInvalid(() -> new ClientLifecycleState(null, REGISTERED_AT, REGISTERED_AT)),
                () -> assertInvalid(() -> new ClientLifecycleState(ClientStatus.ACTIVE, null, REGISTERED_AT)),
                () -> assertInvalid(() -> new ClientLifecycleState(ClientStatus.ACTIVE, REGISTERED_AT, null))
        );
    }

    /**
     * Given 更新时间早于注册时间，When 创建生命周期，Then 应拒绝倒序时间。
     */
    @Test
    void shouldRejectUpdatedTimeBeforeRegisteredTime() {
        assertInvalid(() -> new ClientLifecycleState(ClientStatus.ACTIVE, REGISTERED_AT, REGISTERED_AT.minusSeconds(1)));
    }

    private void assertInvalid(Runnable action) {
        IamException exception = assertThrows(IamException.class, action::run);

        assertEquals(IamErrorCode.CLIENT_REGISTRATION_INVALID, exception.getErrorCode());
    }
}
