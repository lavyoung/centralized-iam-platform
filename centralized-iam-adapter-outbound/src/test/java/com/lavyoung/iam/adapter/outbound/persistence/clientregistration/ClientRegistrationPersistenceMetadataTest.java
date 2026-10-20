package com.lavyoung.iam.adapter.outbound.persistence.clientregistration;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.dao.*;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证客户端注册持久化对象与 MyBatis DAO 的静态映射元数据。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class ClientRegistrationPersistenceMetadataTest {

    /**
     * Given 五个持久化对象，When 读取表映射，Then 应指向约定的 IAM 表。
     */
    @Test
    void shouldMapPersistenceObjectsToExpectedTables() {
        assertAll(
                () -> assertTable(ClientRegistrationPO.class, "iam_client_registration"),
                () -> assertTable(ClientAuthenticationMethodPO.class, "iam_client_authentication_method"),
                () -> assertTable(ClientAuthorizationGrantTypePO.class, "iam_client_authorization_grant_type"),
                () -> assertTable(ClientRedirectUriPO.class, "iam_client_redirect_uri"),
                () -> assertTable(ClientScopePO.class, "iam_client_scope")
        );
    }

    /**
     * Given 五个持久化对象，When 读取主键映射，Then 应统一使用应用侧分配标识。
     */
    @Test
    void shouldUseAssignedTechnicalIds() {
        assertAll(
                () -> assertAssignedId(ClientRegistrationPO.class),
                () -> assertAssignedId(ClientAuthenticationMethodPO.class),
                () -> assertAssignedId(ClientAuthorizationGrantTypePO.class),
                () -> assertAssignedId(ClientRedirectUriPO.class),
                () -> assertAssignedId(ClientScopePO.class)
        );
    }

    /**
     * Given 主表和关联表字段，When 读取列映射，Then 应匹配数据库下划线列名。
     */
    @Test
    void shouldMapFieldsToExpectedColumns() {
        assertAll(
                () -> assertColumn(ClientRegistrationPO.class, "tenantId", "tenant_id"),
                () -> assertColumn(ClientRegistrationPO.class, "clientSecretHash", "client_secret_hash"),
                () -> assertColumn(ClientRegistrationPO.class, "authorizationCodeTtlSeconds", "authorization_code_ttl_seconds"),
                () -> assertColumn(ClientAuthenticationMethodPO.class, "authenticationMethod", "authentication_method"),
                () -> assertColumn(ClientAuthorizationGrantTypePO.class, "grantType", "grant_type"),
                () -> assertColumn(ClientRedirectUriPO.class, "redirectUri", "redirect_uri"),
                () -> assertColumn(ClientScopePO.class, "scope", "scope")
        );
    }

    /**
     * Given 五个 DAO，When 读取接口定义，Then 应声明 Mapper 并绑定正确的 PO 泛型。
     */
    @Test
    void shouldBindDaosToExpectedPersistenceObjects() {
        assertAll(
                () -> assertDao(ClientRegistrationDao.class, ClientRegistrationPO.class),
                () -> assertDao(ClientAuthenticationMethodDao.class, ClientAuthenticationMethodPO.class),
                () -> assertDao(ClientAuthorizationGrantTypeDao.class, ClientAuthorizationGrantTypePO.class),
                () -> assertDao(ClientRedirectUriDao.class, ClientRedirectUriPO.class),
                () -> assertDao(ClientScopeDao.class, ClientScopePO.class)
        );
    }

    private void assertTable(Class<?> type, String expectedTable) {
        TableName tableName = type.getAnnotation(TableName.class);

        assertNotNull(tableName);
        assertEquals(expectedTable, tableName.value());
    }

    private void assertAssignedId(Class<?> type) throws NoSuchFieldException {
        TableId tableId = type.getDeclaredField("id").getAnnotation(TableId.class);

        assertNotNull(tableId);
        assertEquals("id", tableId.value());
        assertEquals(IdType.ASSIGN_ID, tableId.type());
    }

    private void assertColumn(Class<?> type, String fieldName, String expectedColumn) throws NoSuchFieldException {
        Field field = type.getDeclaredField(fieldName);
        TableField tableField = field.getAnnotation(TableField.class);

        assertNotNull(tableField);
        assertEquals(expectedColumn, tableField.value());
    }

    private void assertDao(Class<?> daoType, Class<?> poType) {
        Type mapperType = daoType.getGenericInterfaces()[0];

        assertTrue(daoType.isAnnotationPresent(Mapper.class));
        assertTrue(mapperType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) mapperType;
        assertEquals(BaseMapper.class, parameterizedType.getRawType());
        assertEquals(poType, parameterizedType.getActualTypeArguments()[0]);
    }
}
