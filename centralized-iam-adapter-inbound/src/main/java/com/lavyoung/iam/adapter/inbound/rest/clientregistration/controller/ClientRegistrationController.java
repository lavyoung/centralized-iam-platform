package com.lavyoung.iam.adapter.inbound.rest.clientregistration.controller;

import com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler.ClientRegistrationRequestAssembler;
import com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler.ClientRegistrationResponseAssembler;
import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.application.clientregistration.port.in.RegisterClientUseCase;
import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.contract.clientregistration.request.RegisterConfidentialClientRequest;
import com.lavyoung.iam.contract.clientregistration.request.RegisterPublicClientRequest;
import com.lavyoung.iam.contract.clientregistration.response.ClientRegistrationResponse;
import com.lavyoung.iam.contract.platform.response.ResultResponse;
import com.lavyoung.iam.shared.error.IamException;
import com.lavyoung.iam.shared.identifier.TenantId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 暴露客户端注册管理 API，并连接 HTTP 契约与应用输入端口。
 *
 * <p>Controller 只负责参数接收、可信租户注入、模型装配和结果返回，
 * 不执行客户端查重、密钥散列、领域校验或数据库访问。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientRegistrationController {

    private static final String DEFAULT_TENANT_ID = TenantId.DEFAULT_ID.value();

    private final RegisterClientUseCase registerClientUseCase;
    private final ClientRegistrationRequestAssembler requestAssembler;
    private final ClientRegistrationResponseAssembler responseAssembler;

    /**
     * 注册不持有客户端密钥的公共客户端。
     *
     * @param request 已通过 Jakarta Validation 校验的公共客户端请求
     * @return 包含已注册客户端公开配置的统一成功响应
     * @throws IamException 当客户端配置非法或客户端标识已存在时抛出
     */
    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultResponse<ClientRegistrationResponse> registerPublic(@Valid @RequestBody RegisterPublicClientRequest request) {
        RegisterClientCommand command = requestAssembler.toCommand(request, DEFAULT_TENANT_ID);
        RegisteredClientResult result = registerClientUseCase.registerPublic(command);
        return ResultResponse.success(responseAssembler.toResponse(result));
    }

    /**
     * 注册持有客户端密钥的机密客户端。
     *
     * @param request 已通过 Jakarta Validation 校验的机密客户端请求
     * @return 包含已注册客户端公开配置的统一成功响应
     * @throws IamException             当客户端配置非法或客户端标识已存在时抛出
     * @throws IllegalArgumentException 当客户端明文密钥无法进行安全散列时抛出
     */
    @PostMapping("/confidential")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultResponse<ClientRegistrationResponse> registerConfidential(@Valid @RequestBody RegisterConfidentialClientRequest request) {
        RegisterClientCommand command = requestAssembler.toCommand(request, DEFAULT_TENANT_ID);
        RegisteredClientResult result = registerClientUseCase.registerConfidential(command, request.clientSecret());
        return ResultResponse.success(responseAssembler.toResponse(result));
    }
}