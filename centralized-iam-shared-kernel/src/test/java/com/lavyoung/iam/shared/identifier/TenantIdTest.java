package com.lavyoung.iam.shared.identifier;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证租户标识的默认值、值语义与格式边界。
 *
 * <p>该测试确保租户边界在进入领域模型前完成校验，并通过稳定错误码和
 * 国际化参数报告非法输入。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class TenantIdTest {

    /**
     * Given 单租户部署，When 读取默认租户，Then 应得到稳定的 default 标识。
     */
    @Test
    void shouldProvideDefaultTenant() {
        assertEquals("default", TenantId.DEFAULT_ID.value());
    }

    /**
     * Given 最短、常用和最长租户标识，When 创建值对象，Then 均应保留原始值。
     */
    @Test
    void shouldAcceptValidTenantIdentifiers() {
        assertAll(
                () -> assertEquals("a", new TenantId("a").value()),
                () -> assertEquals("tenant-001", new TenantId("tenant-001").value()),
                () -> assertEquals("a".repeat(64), new TenantId("a".repeat(64)).value())
        );
    }

    /**
     * Given 相同或不同原始值，When 比较租户标识，Then 应遵循 record 值语义。
     */
    @Test
    void shouldUseValueEquality() {
        assertAll(
                () -> assertEquals(new TenantId("tenant-001"), new TenantId("tenant-001")),
                () -> assertNotEquals(new TenantId("tenant-001"), new TenantId("tenant-002"))
        );
    }

    /**
     * Given 非法租户标识，When 创建值对象，Then 应统一抛出租户标识错误。
     */
    @Test
    void shouldRejectInvalidTenantIdentifiers() {
        assertAll(
                () -> assertInvalid(null),
                () -> assertInvalid(""),
                () -> assertInvalid("Tenant-001"),
                () -> assertInvalid("tenant_001"),
                () -> assertInvalid("-tenant"),
                () -> assertInvalid("tenant-"),
                () -> assertInvalid("a".repeat(65))
        );
    }

    /**
     * 断言非法租户值携带稳定错误码和国际化边界参数。
     *
     * @param value 待校验租户值
     */
    private void assertInvalid(String value) {
        IamException exception = assertThrows(IamException.class, () -> new TenantId(value));

        assertAll(
                () -> assertEquals(IamErrorCode.TENANT_ID_INVALID, exception.getErrorCode()),
                () -> assertEquals(List.of(1, 64), exception.getMessageArguments())
        );
    }
}
