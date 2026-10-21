package com.lavyoung.iam.adapter.inbound.rest.clientregistration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lavyoung.iam.adapter.inbound.exception.IamRestExceptionHandler;
import com.lavyoung.iam.adapter.inbound.i18n.IamMessageSourceConfiguration;
import com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler.ClientRegistrationRequestAssembler;
import com.lavyoung.iam.adapter.inbound.rest.clientregistration.assembler.ClientRegistrationResponseAssembler;
import com.lavyoung.iam.application.clientregistration.command.RegisterClientCommand;
import com.lavyoung.iam.application.clientregistration.port.in.RegisterClientUseCase;
import com.lavyoung.iam.application.clientregistration.result.RegisteredClientResult;
import com.lavyoung.iam.contract.clientregistration.request.RegisterConfidentialClientRequest;
import com.lavyoung.iam.contract.clientregistration.request.RegisterPublicClientRequest;
import com.lavyoung.iam.contract.clientregistration.response.ClientRegistrationResponse;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证客户端注册 REST 入口的 HTTP 契约、用例编排与敏感信息边界。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@ExtendWith(MockitoExtension.class)
class ClientRegistrationControllerTest {

    private static final String DEFAULT_TENANT_ID = "default";
    private static final String TEST_SECRET = "x".repeat(32);
    private static final Instant REGISTERED_AT = Instant.parse("2026-10-21T08:00:00Z");

    @Mock
    private RegisterClientUseCase registerClientUseCase;

    @Mock
    private ClientRegistrationRequestAssembler requestAssembler;

    @Mock
    private ClientRegistrationResponseAssembler responseAssembler;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    /**
     * 创建不依赖 Spring 容器的 Controller 测试环境。
     */
    @BeforeEach
    void setUp() {
        IamMessageSourceConfiguration configuration = new IamMessageSourceConfiguration();
        MessageSource messageSource = configuration.messageSource();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        validator.afterPropertiesSet();
        ClientRegistrationController controller = new ClientRegistrationController(registerClientUseCase, requestAssembler, responseAssembler);
        objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IamRestExceptionHandler(messageSource))
                .setLocaleResolver(configuration.localeResolver())
                .setValidator(validator)
                .build();
    }

    /**
     * Given 合法公共客户端请求，When 调用注册接口，Then 应注入默认租户并返回 201 成功响应。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRegisterPublicClient() throws Exception {
        // Given
        RegisterPublicClientRequest request = publicRequest();
        RegisterClientCommand command = command(request);
        RegisteredClientResult result = registeredResult(command);
        ClientRegistrationResponse response = response(result);
        when(requestAssembler.toCommand(request, DEFAULT_TENANT_ID)).thenReturn(command);
        when(registerClientUseCase.registerPublic(command)).thenReturn(result);
        when(responseAssembler.toResponse(result)).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IAM-0000"))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.data.tenantId").value(DEFAULT_TENANT_ID))
                .andExpect(jsonPath("$.data.clientId").value(request.clientId()))
                .andExpect(jsonPath("$.data.clientSecret").doesNotExist());

        verify(requestAssembler).toCommand(request, DEFAULT_TENANT_ID);
        verify(registerClientUseCase).registerPublic(command);
        verify(responseAssembler).toResponse(result);
    }

    /**
     * Given 合法机密客户端请求，When 调用注册接口，Then 明文密钥只应传入用例且不得出现在响应中。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRegisterConfidentialClientWithoutExposingSecret() throws Exception {
        // Given
        RegisterConfidentialClientRequest request = confidentialRequest();
        RegisterClientCommand command = command(request);
        RegisteredClientResult result = registeredResult(command);
        ClientRegistrationResponse response = response(result);
        when(requestAssembler.toCommand(request, DEFAULT_TENANT_ID)).thenReturn(command);
        when(registerClientUseCase.registerConfidential(command, TEST_SECRET)).thenReturn(result);
        when(responseAssembler.toResponse(result)).thenReturn(response);

        // When
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/clients/confidential")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("IAM-0000"))
                .andExpect(jsonPath("$.data.clientId").value(request.clientId()))
                .andExpect(jsonPath("$.data.clientSecret").doesNotExist())
                .andReturn();

        // Then
        assertFalse(mvcResult.getResponse().getContentAsString().contains(TEST_SECRET));
        verify(requestAssembler).toCommand(request, DEFAULT_TENANT_ID);
        verify(registerClientUseCase).registerConfidential(command, TEST_SECRET);
        verify(responseAssembler).toResponse(result);
    }

    /**
     * Given 缺少必要字段的中文请求，When 调用注册接口，Then 应返回本地化参数错误且不进入用例。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidRequestBeforeCallingUseCase() throws Exception {
        // Given
        String invalidRequest = """
                {
                  "clientId": "",
                  "displayName": "",
                  "authenticationMethods": [],
                  "grantTypes": [],
                  "redirectUris": null,
                  "scopes": [],
                  "accessTokenTtlSeconds": 0,
                  "refreshTokenTtlSeconds": 0,
                  "authorizationCodeTtlSeconds": 0
                }
                """;

        // When / Then
        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM-1000"))
                .andExpect(jsonPath("$.message").value("令牌有效期必须大于零"))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(registerClientUseCase, requestAssembler, responseAssembler);
    }

    /**
     * Given 损坏的 JSON 请求，When 调用注册接口，Then 应返回默认英文参数错误。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectMalformedJsonWithDefaultEnglishMessage() throws Exception {
        // Given
        String malformedJson = "{\"clientId\":\"broken\"";

        // When / Then
        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM-1000"))
                .andExpect(jsonPath("$.message").value("Invalid request argument"))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(registerClientUseCase, requestAssembler, responseAssembler);
    }

    /**
     * Given 已存在的客户端标识，When 应用用例拒绝注册，Then 应返回中文 409 冲突响应。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldReturnLocalizedConflictResponse() throws Exception {
        // Given
        RegisterPublicClientRequest request = publicRequest();
        RegisterClientCommand command = command(request);
        when(requestAssembler.toCommand(request, DEFAULT_TENANT_ID)).thenReturn(command);
        when(registerClientUseCase.registerPublic(command)).thenThrow(new IamException(IamErrorCode.RESOURCE_CONFLICT));

        // When / Then
        mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IAM-1002"))
                .andExpect(jsonPath("$.message").value("请求的资源已存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(responseAssembler);
    }

    /**
     * Given 包含敏感内部细节的未预期异常，When 调用注册接口，Then 应返回通用 500 响应且不泄露细节。
     *
     * @throws Exception 当 MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldHideUnexpectedExceptionDetails() throws Exception {
        // Given
        String internalDetail = "internal persistence diagnostic";
        RegisterPublicClientRequest request = publicRequest();
        RegisterClientCommand command = command(request);
        when(requestAssembler.toCommand(request, DEFAULT_TENANT_ID)).thenReturn(command);
        when(registerClientUseCase.registerPublic(command)).thenThrow(new IllegalStateException(internalDetail));

        // When
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/clients/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("IAM-9000"))
                .andExpect(jsonPath("$.message").value("An internal server error occurred"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        // Then
        assertFalse(mvcResult.getResponse().getContentAsString().contains(internalDetail));
        verifyNoInteractions(responseAssembler);
    }

    /**
     * 创建合法的公共客户端注册请求。
     *
     * @return 公共客户端注册请求
     */
    private RegisterPublicClientRequest publicRequest() {
        return new RegisterPublicClientRequest(
                "public-web-app",
                "Public Web App",
                Set.of("none"),
                Set.of("authorization_code", "refresh_token"),
                Set.of("https://app.example.com/callback"),
                Set.of("openid", "profile"),
                900,
                28_800,
                300
        );
    }

    /**
     * 创建合法且使用测试密钥的机密客户端注册请求。
     *
     * @return 机密客户端注册请求
     */
    private RegisterConfidentialClientRequest confidentialRequest() {
        return new RegisterConfidentialClientRequest(
                "service-app",
                "Service App",
                Set.of("client_secret_basic"),
                Set.of("client_credentials"),
                Set.of(),
                Set.of("service.read"),
                900,
                28_800,
                300,
                TEST_SECRET
        );
    }

    /**
     * 创建与公共请求对应的应用命令。
     *
     * @param request 公共客户端注册请求
     * @return 客户端注册命令
     */
    private RegisterClientCommand command(RegisterPublicClientRequest request) {
        return new RegisterClientCommand(
                DEFAULT_TENANT_ID,
                request.clientId(),
                request.displayName(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.scopes(),
                Duration.ofSeconds(request.accessTokenTtlSeconds()),
                Duration.ofSeconds(request.refreshTokenTtlSeconds()),
                Duration.ofSeconds(request.authorizationCodeTtlSeconds())
        );
    }

    /**
     * 创建与机密请求对应且不包含明文密钥的应用命令。
     *
     * @param request 机密客户端注册请求
     * @return 客户端注册命令
     */
    private RegisterClientCommand command(RegisterConfidentialClientRequest request) {
        return new RegisterClientCommand(
                DEFAULT_TENANT_ID,
                request.clientId(),
                request.displayName(),
                request.authenticationMethods(),
                request.grantTypes(),
                request.redirectUris(),
                request.scopes(),
                Duration.ofSeconds(request.accessTokenTtlSeconds()),
                Duration.ofSeconds(request.refreshTokenTtlSeconds()),
                Duration.ofSeconds(request.authorizationCodeTtlSeconds())
        );
    }

    /**
     * 创建与应用命令对应的注册成功结果。
     *
     * @param command 客户端注册命令
     * @return 注册成功结果
     */
    private RegisteredClientResult registeredResult(RegisterClientCommand command) {
        return new RegisteredClientResult(
                command.tenantId(),
                command.clientId(),
                command.displayName(),
                command.authenticationMethods(),
                command.grantTypes(),
                command.redirectUris(),
                command.scopes(),
                command.accessTokenTimeToLive(),
                command.refreshTokenTimeToLive(),
                command.authorizationCodeTimeToLive(),
                "ACTIVE",
                REGISTERED_AT,
                REGISTERED_AT
        );
    }

    /**
     * 创建与应用结果对应的公开响应模型。
     *
     * @param result 注册成功结果
     * @return 客户端注册响应
     */
    private ClientRegistrationResponse response(RegisteredClientResult result) {
        return new ClientRegistrationResponse(
                result.tenantId(),
                result.clientId(),
                result.displayName(),
                result.authenticationMethods(),
                result.grantTypes(),
                result.redirectUris(),
                result.scopes(),
                result.accessTokenTimeToLive().toSeconds(),
                result.refreshTokenTimeToLive().toSeconds(),
                result.authorizationCodeTimeToLive().toSeconds(),
                result.status(),
                result.registeredAt(),
                result.updatedAt()
        );
    }
}
