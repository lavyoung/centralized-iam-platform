package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.util.UUID;

/**
 * 表示 IAM 平台内部生成的用户唯一标识。
 *
 * <p>用户标识不承载用户名、邮箱等业务含义，避免用户属性变化影响关联关系。</p>
 *
 * @param value UUID 标识值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record UserId(UUID value) {

    /**
     * 校验并创建用户标识。
     *
     * @param value UUID 标识值
     * @throws IamException 当 UUID 为空时抛出
     */
    public UserId {
        if (value == null) {
            throw invalidUserId();
        }
    }

    /**
     * 生成新的随机用户标识。
     *
     * @return 新用户标识
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * 解析规范的小写 UUID 字符串。
     *
     * @param value UUID 字符串
     * @return 用户标识
     * @throws IamException 当字符串为空或不是规范 UUID 时抛出
     */
    public static UserId parse(String value) {
        if (value == null) {
            throw invalidUserId();
        }

        try {
            UUID parsedValue = UUID.fromString(value);
            if (!parsedValue.toString().equals(value)) {
                throw invalidUserId();
            }
            return new UserId(parsedValue);
        } catch (IllegalArgumentException exception) {
            throw invalidUserId();
        }
    }

    private static IamException invalidUserId() {
        return new IamException(IamErrorCode.USER_ID_INVALID);
    }
}