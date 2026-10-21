package com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler;

import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.contract.clientregistration.response.ClientRegistrationResponse;
import org.mapstruct.*;

import java.time.Duration;
import java.util.Objects;

/**
 * 在 REST 入站边界将客户端注册应用结果转换为响应契约。
 *
 * <p>本装配器只执行机械字段转换，不负责统一响应包装或异常映射。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ClientRegistrationResponseAssembler {

    /**
     * 将应用层结果转换为客户端注册响应。
     *
     * @param result 客户端注册应用结果
     * @return 非空客户端注册响应
     * @throws NullPointerException 当应用结果为空时抛出
     */
    public final ClientRegistrationResponse toResponse(RegisteredClientResult result) {
        Objects.requireNonNull(result, "result");
        return mapResponse(result);
    }

    @Mapping(target = "accessTokenTtlSeconds", source = "accessTokenTimeToLive", qualifiedByName = "durationToSeconds")
    @Mapping(target = "refreshTokenTtlSeconds", source = "refreshTokenTimeToLive", qualifiedByName = "durationToSeconds")
    @Mapping(target = "authorizationCodeTtlSeconds", source = "authorizationCodeTimeToLive", qualifiedByName = "durationToSeconds")
    protected abstract ClientRegistrationResponse mapResponse(RegisteredClientResult result);

    /**
     * 将应用层时间长度转换为接口层秒数。
     *
     * @param duration 非空时间长度
     * @return 对应秒数
     * @throws NullPointerException 当时间长度为空时抛出
     */
    @Named("durationToSeconds")
    protected long durationToSeconds(Duration duration) {
        return Objects.requireNonNull(duration, "duration").toSeconds();
    }
}