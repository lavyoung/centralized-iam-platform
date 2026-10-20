package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.assembler.ClientRegistrationPersistenceAssembler;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.dao.*;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.model.ClientRegistrationPersistenceSnapshot;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;
import com.lavyoung.iam.domain.clientregistration.model.*;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * 验证客户端注册持久化仓储的租户查询、快照恢复和事务写入编排。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@ExtendWith(MockitoExtension.class)
class ClientRegistrationPersistenceRepositoryTest {

    private static final TenantId TENANT_ID = new TenantId("default");
    private static final ClientId CLIENT_ID = new ClientId("console-client");
    private static final Long REGISTRATION_ID = 1001L;
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-20T08:00:00Z");

    @Mock
    private ClientRegistrationDao registrationDao;

    @Mock
    private ClientAuthenticationMethodDao authenticationMethodDao;

    @Mock
    private ClientAuthorizationGrantTypeDao grantTypeDao;

    @Mock
    private ClientRedirectUriDao redirectUriDao;

    @Mock
    private ClientScopeDao scopeDao;

    @Mock
    private ClientRegistrationPersistenceAssembler assembler;

    private ClientRegistrationPersistenceRepository repository;

    /**
     * 初始化 MyBatis-Plus Lambda 查询所需的表元数据。
     */
    @BeforeAll
    static void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initializeTable(configuration, ClientRegistrationPO.class);
        initializeTable(configuration, ClientAuthenticationMethodPO.class);
        initializeTable(configuration, ClientAuthorizationGrantTypePO.class);
        initializeTable(configuration, ClientRedirectUriPO.class);
        initializeTable(configuration, ClientScopePO.class);
    }

    /**
     * 使用 DAO 与装配器 mock 创建待测仓储。
     */
    @BeforeEach
    void setUp() {
        repository = new ClientRegistrationPersistenceRepository(
                registrationDao,
                authenticationMethodDao,
                grantTypeDao,
                redirectUriDao,
                scopeDao,
                assembler
        );
    }

    /**
     * Given 客户端不存在，When 查询聚合，Then 返回空且不加载关联表。
     */
    @Test
    void shouldReturnEmptyWithoutLoadingAssociations() {
        when(registrationDao.selectOne(any())).thenReturn(null);

        Optional<ClientRegistration> result = repository.findById(TENANT_ID, CLIENT_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(authenticationMethodDao, grantTypeDao, redirectUriDao, scopeDao, assembler);
        assertRegistrationSelectQuery();
    }

    /**
     * Given 完整持久化记录，When 查询聚合，Then 按租户加载全部关联并恢复聚合。
     */
    @Test
    void shouldRestoreCompleteSnapshotWithTenantQueries() {
        ClientRegistration domain = registration();
        ClientRegistrationPO registrationPO = registrationPO(REGISTRATION_ID);
        List<ClientAuthenticationMethodPO> methods = List.of(new ClientAuthenticationMethodPO());
        List<ClientAuthorizationGrantTypePO> grants = List.of(new ClientAuthorizationGrantTypePO());
        List<ClientRedirectUriPO> redirectUris = List.of(new ClientRedirectUriPO());
        List<ClientScopePO> scopes = List.of(new ClientScopePO());
        when(registrationDao.selectOne(any())).thenReturn(registrationPO);
        when(authenticationMethodDao.selectList(any())).thenReturn(methods);
        when(grantTypeDao.selectList(any())).thenReturn(grants);
        when(redirectUriDao.selectList(any())).thenReturn(redirectUris);
        when(scopeDao.selectList(any())).thenReturn(scopes);
        when(assembler.toDomain(any())).thenReturn(domain);

        Optional<ClientRegistration> result = repository.findById(TENANT_ID, CLIENT_ID);

        assertEquals(Optional.of(domain), result);
        ArgumentCaptor<ClientRegistrationPersistenceSnapshot> captor = ArgumentCaptor.forClass(ClientRegistrationPersistenceSnapshot.class);
        verify(assembler).toDomain(captor.capture());
        assertAll(
                () -> assertEquals(registrationPO, captor.getValue().registration()),
                () -> assertEquals(methods, captor.getValue().authenticationMethods()),
                () -> assertEquals(grants, captor.getValue().grantTypes()),
                () -> assertEquals(redirectUris, captor.getValue().redirectUris()),
                () -> assertEquals(scopes, captor.getValue().scopes())
        );
        assertAssociationSelectQuery(authenticationMethodDao);
        assertAssociationSelectQuery(grantTypeDao);
        assertAssociationSelectQuery(redirectUriDao);
        assertAssociationSelectQuery(scopeDao);
    }

    /**
     * Given 新客户端聚合，When 保存，Then 先新增主表再批量写入非空关联集合。
     */
    @Test
    void shouldInsertRegistrationAndNonEmptyAssociations() {
        ClientRegistration domain = registration();
        ClientRegistrationPO target = registrationPO(null);
        List<ClientAuthenticationMethodPO> methods = List.of(new ClientAuthenticationMethodPO());
        List<ClientAuthorizationGrantTypePO> grants = List.of(new ClientAuthorizationGrantTypePO());
        List<ClientRedirectUriPO> redirectUris = List.of(new ClientRedirectUriPO());
        when(assembler.toRegistrationPO(domain)).thenReturn(target);
        when(registrationDao.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            target.setId(REGISTRATION_ID);
            return 1;
        }).when(registrationDao).insert(target);
        when(assembler.toAuthenticationMethodPOs(domain, REGISTRATION_ID)).thenReturn(methods);
        when(assembler.toGrantTypePOs(domain, REGISTRATION_ID)).thenReturn(grants);
        when(assembler.toRedirectUriPOs(domain, REGISTRATION_ID)).thenReturn(redirectUris);
        when(assembler.toScopePOs(domain, REGISTRATION_ID)).thenReturn(List.of());

        repository.save(domain);

        InOrder order = inOrder(registrationDao, authenticationMethodDao, grantTypeDao, redirectUriDao);
        order.verify(registrationDao).insert(target);
        order.verify(authenticationMethodDao).insert(methods);
        order.verify(grantTypeDao).insert(grants);
        order.verify(redirectUriDao).insert(redirectUris);
        verify(scopeDao, never()).insert(anyCollection());
    }

    /**
     * Given 已存在的客户端，When 保存状态变化，Then 更新主表并替换关联快照。
     */
    @Test
    void shouldUpdateRegistrationAndReplaceAssociations() {
        ClientRegistration domain = registration();
        ClientRegistrationPO target = registrationPO(null);
        ClientRegistrationPO persisted = registrationPO(REGISTRATION_ID);
        List<ClientAuthenticationMethodPO> methods = List.of(new ClientAuthenticationMethodPO());
        List<ClientAuthorizationGrantTypePO> grants = List.of(new ClientAuthorizationGrantTypePO());
        List<ClientScopePO> scopes = List.of(new ClientScopePO());
        when(assembler.toRegistrationPO(domain)).thenReturn(target);
        when(registrationDao.selectOne(any())).thenReturn(persisted);
        when(registrationDao.updateById(target)).thenReturn(1);
        when(assembler.toAuthenticationMethodPOs(domain, REGISTRATION_ID)).thenReturn(methods);
        when(assembler.toGrantTypePOs(domain, REGISTRATION_ID)).thenReturn(grants);
        when(assembler.toRedirectUriPOs(domain, REGISTRATION_ID)).thenReturn(List.of());
        when(assembler.toScopePOs(domain, REGISTRATION_ID)).thenReturn(scopes);

        repository.save(domain);

        assertEquals(REGISTRATION_ID, target.getId());
        verify(authenticationMethodDao).delete(any());
        verify(grantTypeDao).delete(any());
        verify(redirectUriDao).delete(any());
        verify(scopeDao).delete(any());
        verify(authenticationMethodDao).insert(methods);
        verify(grantTypeDao).insert(grants);
        verify(redirectUriDao, never()).insert(anyCollection());
        verify(scopeDao).insert(scopes);
    }

    /**
     * Given 并发请求已占用客户端标识，When 新增主表，Then 转换为稳定冲突错误。
     */
    @Test
    void shouldTranslateConcurrentDuplicateRegistration() {
        ClientRegistration domain = registration();
        ClientRegistrationPO target = registrationPO(null);
        when(assembler.toRegistrationPO(domain)).thenReturn(target);
        when(registrationDao.selectOne(any())).thenReturn(null);
        when(registrationDao.insert(target)).thenThrow(new DuplicateKeyException("duplicate client"));

        IamException exception = assertThrows(IamException.class, () -> repository.save(domain));

        assertEquals(IamErrorCode.RESOURCE_CONFLICT, exception.getErrorCode());
        verifyNoInteractions(authenticationMethodDao, grantTypeDao, redirectUriDao, scopeDao);
    }

    private void assertRegistrationSelectQuery() {
        ArgumentCaptor<Wrapper<ClientRegistrationPO>> captor = wrapperCaptor();
        verify(registrationDao).selectOne(captor.capture());
        assertQuery(captor.getValue(), "tenant_id", TENANT_ID.value(), "client_id", CLIENT_ID.value());
    }

    private <T> void assertAssociationSelectQuery(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> dao) {
        ArgumentCaptor<Wrapper<T>> captor = wrapperCaptor();
        verify(dao).selectList(captor.capture());
        assertQuery(captor.getValue(), "tenant_id", TENANT_ID.value(), "client_registration_id", REGISTRATION_ID);
    }

    private void assertQuery(Wrapper<?> wrapper, String firstColumn, Object firstValue, String secondColumn, Object secondValue) {
        String sql = wrapper.getSqlSegment();
        Map<String, Object> parameters = ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
        assertAll(
                () -> assertTrue(sql.contains(firstColumn)),
                () -> assertTrue(sql.contains(secondColumn)),
                () -> assertTrue(parameters.containsValue(firstValue)),
                () -> assertTrue(parameters.containsValue(secondValue))
        );
    }

    private ClientRegistration registration() {
        ClientRegistrationSpec specification = new ClientRegistrationSpec(
                TENANT_ID,
                CLIENT_ID,
                "Console Client",
                Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
                Set.of(AuthorizationGrantType.CLIENT_CREDENTIALS),
                Set.of(),
                Set.of("service.read"),
                ClientTokenPolicy.standard()
        );
        return ClientRegistration.registerConfidential(specification, new ClientSecretHash("a".repeat(60)), REGISTERED_AT);
    }

    private ClientRegistrationPO registrationPO(Long id) {
        ClientRegistrationPO result = new ClientRegistrationPO();
        result.setId(id);
        result.setTenantId(TENANT_ID.value());
        result.setClientId(CLIENT_ID.value());
        return result;
    }

    private static void initializeTable(MybatisConfiguration configuration, Class<?> type) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, type.getName()), type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
