package com.lavyoung.iam.adapter.outbound.persistence.clientregistration.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.assembler.ClientRegistrationPersistenceAssembler;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.dao.*;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.model.ClientRegistrationPersistenceSnapshot;
import com.lavyoung.iam.adapter.outbound.persistence.clientregistration.po.*;
import com.lavyoung.iam.domain.clientregistration.model.ClientRegistration;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.ClientId;
import com.lavyoung.iam.shared.identifier.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 使用 MyBatis-Plus 持久化客户端注册聚合的出站适配器。
 *
 * <p>本适配器维护客户端主表及四类关联表的一致快照，并在所有查询和写入中
 * 显式携带租户条件。它不处理客户端注册规则，也不负责生成协议响应。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Repository
@RequiredArgsConstructor
public class ClientRegistrationPersistenceRepository implements ClientRegistrationRepository {

    private final ClientRegistrationDao registrationDao;
    private final ClientAuthenticationMethodDao authenticationMethodDao;
    private final ClientAuthorizationGrantTypeDao grantTypeDao;
    private final ClientRedirectUriDao redirectUriDao;
    private final ClientScopeDao scopeDao;

    private final ClientRegistrationPersistenceAssembler assembler;

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientRegistration> findById(TenantId tenantId, ClientId clientId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(clientId, "clientId");

        return findRegistration(tenantId, clientId).map(this::restore);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(TenantId tenantId, ClientId clientId) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(clientId, "clientId");

        return registrationDao.exists(registrationQuery(tenantId, clientId));
    }

    /**
     * 在一个事务中保存主表和全部关联表快照。
     *
     * <p>应用层查重不能消除并发注册竞态，因此新增时仍依赖数据库唯一键完成最终防护。</p>
     *
     * @param registration 待持久化的客户端注册聚合
     * @throws NullPointerException 当聚合为空时抛出
     * @throws IamException         当同一租户已并发注册相同客户端标识时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ClientRegistration registration) {
        Objects.requireNonNull(registration, "registration");

        ClientRegistrationPO target = assembler.toRegistrationPO(registration);
        Optional<ClientRegistrationPO> persisted = findRegistration(registration.specification().tenantId(), registration.specification().clientId());

        if (persisted.isEmpty()) {
            insertRegistration(target);
        } else {
            target.setId(persisted.orElseThrow().getId());
            updateRegistration(target);
            deleteAssociations(target.getTenantId(), target.getId());
        }

        insertAssociations(registration, target.getId());
    }

    private Optional<ClientRegistrationPO> findRegistration(TenantId tenantId, ClientId clientId) {
        return Optional.ofNullable(registrationDao.selectOne(registrationQuery(tenantId, clientId)));
    }

    private LambdaQueryWrapper<ClientRegistrationPO> registrationQuery(TenantId tenantId, ClientId clientId) {
        return Wrappers.lambdaQuery(ClientRegistrationPO.class)
                .eq(ClientRegistrationPO::getTenantId, tenantId.value())
                .eq(ClientRegistrationPO::getClientId, clientId.value());
    }

    private ClientRegistration restore(ClientRegistrationPO registration) {
        Long registrationId = Objects.requireNonNull(registration.getId(), "clientRegistrationId");
        String tenantId = registration.getTenantId();

        ClientRegistrationPersistenceSnapshot snapshot = new ClientRegistrationPersistenceSnapshot(
                registration,
                authenticationMethodDao.selectList(authenticationMethodQuery(tenantId, registrationId)),
                grantTypeDao.selectList(grantTypeQuery(tenantId, registrationId)),
                redirectUriDao.selectList(redirectUriQuery(tenantId, registrationId)),
                scopeDao.selectList(scopeQuery(tenantId, registrationId))
        );
        return assembler.toDomain(snapshot);
    }

    private void insertRegistration(ClientRegistrationPO registration) {
        try {
            requireSingleRow(registrationDao.insert(registration), "insert client registration");
        } catch (DuplicateKeyException exception) {
            throw new IamException(IamErrorCode.RESOURCE_CONFLICT);
        }

        if (registration.getId() == null) {
            throw new IncorrectUpdateSemanticsDataAccessException("Missing assigned client registration id");
        }
    }

    private void updateRegistration(ClientRegistrationPO registration) {
        requireSingleRow(registrationDao.updateById(registration), "update client registration");
    }

    private void deleteAssociations(String tenantId, Long registrationId) {
        authenticationMethodDao.delete(authenticationMethodQuery(tenantId, registrationId));
        grantTypeDao.delete(grantTypeQuery(tenantId, registrationId));
        redirectUriDao.delete(redirectUriQuery(tenantId, registrationId));
        scopeDao.delete(scopeQuery(tenantId, registrationId));
    }

    private void insertAssociations(ClientRegistration registration, Long registrationId) {
        insertIfNotEmpty(assembler.toAuthenticationMethodPOs(registration, registrationId), rows -> authenticationMethodDao.insert(rows));
        insertIfNotEmpty(assembler.toGrantTypePOs(registration, registrationId), rows -> grantTypeDao.insert(rows));
        insertIfNotEmpty(assembler.toRedirectUriPOs(registration, registrationId), rows -> redirectUriDao.insert(rows));
        insertIfNotEmpty(assembler.toScopePOs(registration, registrationId), rows -> scopeDao.insert(rows));
    }

    private LambdaQueryWrapper<ClientAuthenticationMethodPO> authenticationMethodQuery(String tenantId, Long registrationId) {
        return Wrappers.lambdaQuery(ClientAuthenticationMethodPO.class)
                .eq(ClientAuthenticationMethodPO::getTenantId, tenantId)
                .eq(ClientAuthenticationMethodPO::getClientRegistrationId, registrationId);
    }

    private LambdaQueryWrapper<ClientAuthorizationGrantTypePO> grantTypeQuery(String tenantId, Long registrationId) {
        return Wrappers.lambdaQuery(ClientAuthorizationGrantTypePO.class)
                .eq(ClientAuthorizationGrantTypePO::getTenantId, tenantId)
                .eq(ClientAuthorizationGrantTypePO::getClientRegistrationId, registrationId);
    }

    private LambdaQueryWrapper<ClientRedirectUriPO> redirectUriQuery(String tenantId, Long registrationId) {
        return Wrappers.lambdaQuery(ClientRedirectUriPO.class)
                .eq(ClientRedirectUriPO::getTenantId, tenantId)
                .eq(ClientRedirectUriPO::getClientRegistrationId, registrationId);
    }

    private LambdaQueryWrapper<ClientScopePO> scopeQuery(String tenantId, Long registrationId) {
        return Wrappers.lambdaQuery(ClientScopePO.class)
                .eq(ClientScopePO::getTenantId, tenantId)
                .eq(ClientScopePO::getClientRegistrationId, registrationId);
    }

    private <T> void insertIfNotEmpty(List<T> rows, Consumer<List<T>> batchInsert) {
        if (!rows.isEmpty()) {
            batchInsert.accept(rows);
        }
    }

    private void requireSingleRow(int affectedRows, String operation) {
        if (affectedRows != 1) {
            throw new IncorrectUpdateSemanticsDataAccessException(operation);
        }
    }
}
