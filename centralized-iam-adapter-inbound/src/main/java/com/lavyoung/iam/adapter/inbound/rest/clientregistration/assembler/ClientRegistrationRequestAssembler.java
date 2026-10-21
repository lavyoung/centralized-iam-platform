package com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler;

import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.contract.clientregistration.request.RegisterConfidentialClientRequest;
import com.lavyoung.iam.contract.clientregistration.request.RegisterPublicClientRequest;
import org.mapstruct.*;

import java.time.Duration;
import java.util.Objects;

/**
 * 在 REST 入站边界将客户端注册请求转换为应用层命令。
 *
 * <p>本装配器只负责 Contract 与 Application 模型转换，不执行领域校验、
 * 客户端注册或密钥散列。租户标识必须由可信入站上下文提供。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ClientRegistrationRequestAssembler {

    /**
     * 将公共客户端注册请求转换为应用命令。
     *
     * @param request  公共客户端注册请求
     * @param tenantId 可信入站上下文提供的租户标识
     * @return 非空客户端注册命令
     * @throws NullPointerException 当请求或租户标识为空时抛出
     */
    public final RegisterClientCommand toCommand(RegisterPublicClientRequest request, String tenantId) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(tenantId, "tenantId");
        return mapPublicRequest(request, tenantId);
    }

    /**
     * 将机密客户端注册请求转换为应用命令。
     *
     * <p>客户端明文密钥不进入命令对象，由 Controller 单独传递给用例。</p>
     *
     * @param request  机密客户端注册请求
     * @param tenantId 可信入站上下文提供的租户标识
     * @return 非空客户端注册命令
     * @throws NullPointerException 当请求或租户标识为空时抛出
     */
    public final RegisterClientCommand toCommand(RegisterConfidentialClientRequest request, String tenantId) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(tenantId, "tenantId");
        return mapConfidentialRequest(request, tenantId);
    }

    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "accessTokenTimeToLive", source = "request.accessTokenTtlSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "refreshTokenTimeToLive", source = "request.refreshTokenTtlSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "authorizationCodeTimeToLive", source = "request.authorizationCodeTtlSeconds", qualifiedByName = "secondsToDuration")
    protected abstract RegisterClientCommand mapPublicRequest(RegisterPublicClientRequest request, String tenantId);

    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "accessTokenTimeToLive", source = "request.accessTokenTtlSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "refreshTokenTimeToLive", source = "request.refreshTokenTtlSeconds", qualifiedByName = "secondsToDuration")
    @Mapping(target = "authorizationCodeTimeToLive", source = "request.authorizationCodeTtlSeconds", qualifiedByName = "secondsToDuration")
    protected abstract RegisterClientCommand mapConfidentialRequest(RegisterConfidentialClientRequest request, String tenantId);

    /**
     * 将接口层秒数转换为应用层时间长度。
     *
     * @param seconds 有效秒数
     * @return 对应的非空时间长度
     */
    @Named("secondsToDuration")
    protected Duration secondsToDuration(long seconds) {
        return Duration.ofSeconds(seconds);
    }
}