package com.lavyoung.iam.application.clientregistration.service;

import com.lavyoung.iam.application.clientregistration.port.in.RegisterClientUseCase;
import com.lavyoung.iam.application.clientregistration.port.out.ClientSecretHasher;
import com.lavyoung.iam.domain.clientregistration.model.ClientRegistration;
import com.lavyoung.iam.domain.clientregistration.model.ClientRegistrationSpec;
import com.lavyoung.iam.domain.clientregistration.model.ClientSecretHash;
import com.lavyoung.iam.domain.clientregistration.repository.ClientRegistrationRepository;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * 编排客户端注册用例。
 *
 * <p>本服务负责查重、调用密码学端口、创建聚合并通过仓储端口保存。
 * 领域规则仍由客户端聚合维护。本类使用 Spring 管理容器和事务，
 * 但不依赖数据库、HTTP 或安全协议的具体实现。</p> *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class RegisterClientService implements RegisterClientUseCase {

    private final ClientRegistrationRepository repository;
    private final ClientSecretHasher secretHasher;
    private final Clock clock;

    @Override
    public ClientRegistration registerPublic(ClientRegistrationSpec spec) {
        ensureClientIdAvailable(spec);
        ClientRegistration registration = ClientRegistration.registerPublic(spec, clock.instant());
        repository.save(registration);
        return registration;
    }

    @Override
    public ClientRegistration registerConfidential(ClientRegistrationSpec spec, CharSequence rawSecret) {
        ensureClientIdAvailable(spec);
        ClientSecretHash secretHash = secretHasher.hash(rawSecret);
        ClientRegistration registration = ClientRegistration.registerConfidential(spec, secretHash, clock.instant());
        repository.save(registration);
        return registration;
    }

    private void ensureClientIdAvailable(ClientRegistrationSpec specification) {
        if (specification == null) {
            throw new IamException(IamErrorCode.CLIENT_REGISTRATION_INVALID);
        }

        boolean exists = repository.existsById(specification.tenantId(), specification.clientId());
        if (exists) {
            throw new IamException(IamErrorCode.RESOURCE_CONFLICT);
        }
    }
}
