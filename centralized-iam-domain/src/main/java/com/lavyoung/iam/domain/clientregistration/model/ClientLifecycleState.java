package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import lombok.Builder;

import java.time.Instant;

/**
 * 客户端生命周期状态。
 *
 * <p>封装客户端状态及其生命周期时间，用于注册新客户端和从持久化存储恢复客户端聚合。</p>
 *
 * @param status       客户端状态
 * @param registeredAt 注册时间
 * @param updatedAt    最后更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Builder
public record ClientLifecycleState(ClientStatus status, Instant registeredAt, Instant updatedAt) {

    /**
     * 校验客户端生命周期状态。
     *
     * @throws IamException 当状态、时间为空或更新时间早于注册时间时抛出
     */
    public ClientLifecycleState {
        if (status == null || registeredAt == null || updatedAt == null) {
            throw new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
        }
        if (updatedAt.isBefore(registeredAt)) {
            throw new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
        }
    }

    /**
     * 创建新注册客户端的初始生命周期状态。
     *
     * @param registeredAt 注册时间
     * @return 初始生命周期状态
     * @throws IamException 当注册时间为空时抛出
     */
    public static ClientLifecycleState initial(Instant registeredAt) {
        return new ClientLifecycleState(ClientStatus.ACTIVE, registeredAt, registeredAt);
    }
}
