package com.lavyoung.iam.adapter.inbound.exception;

import com.lavyoung.iam.adapter.inbound.i18n.IamMessageSourceConfiguration;
import com.lavyoung.iam.contract.platform.response.ResultResponse;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证普通 REST 异常到 HTTP 状态、稳定错误码和本地化提示的映射。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class IamRestExceptionHandlerTest {

    private IamRestExceptionHandler exceptionHandler;

    /**
     * 使用真实国际化消息源创建异常处理器。
     */
    @BeforeEach
    void setUp() {
        IamMessageSourceConfiguration configuration = new IamMessageSourceConfiguration();
        exceptionHandler = new IamRestExceptionHandler(configuration.messageSource());
    }

    /**
     * Given 任一稳定 IAM 错误码，When 映射为 REST 响应，Then 应返回约定的 HTTP 状态和原错误码。
     *
     * @param errorCode      IAM 错误码
     * @param expectedStatus 预期 HTTP 状态
     */
    @ParameterizedTest
    @MethodSource("statusMappings")
    void shouldMapIamErrorCodeToHttpStatus(IamErrorCode errorCode, HttpStatus expectedStatus) {
        // Given
        IamException exception = new IamException(errorCode);

        // When
        ResponseEntity<ResultResponse<Void>> response = exceptionHandler.handleIamException(exception, Locale.US);

        // Then
        ResultResponse.Failure<?> failure = failureBody(response);
        assertAll(
                () -> assertEquals(expectedStatus, response.getStatusCode()),
                () -> assertEquals(errorCode.getCode(), failure.code()),
                () -> assertFalse(failure.message().isBlank())
        );
    }

    /**
     * Given 带安全占位参数的业务异常，When 解析英文提示，Then 应正确插入参数。
     */
    @Test
    void shouldResolveIamExceptionArguments() {
        // Given
        IamException exception = new IamException(IamErrorCode.CLIENT_ID_INVALID, 3, 64);

        // When
        ResponseEntity<ResultResponse<Void>> response = exceptionHandler.handleIamException(exception, Locale.US);

        // Then
        ResultResponse.Failure<?> failure = failureBody(response);
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("IAM-1005", failure.code()),
                () -> assertEquals(
                        "Client ID must start with an alphanumeric character, contain only letters, digits, dots, underscores, tildes, or hyphens, and be 3-64 characters long",
                        failure.message())
        );
    }

    /**
     * Given 方法参数约束失败，When 映射异常，Then 应选择稳定顺序中的首个安全提示。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldMapConstraintViolation() {
        // Given
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("request.clientId");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("Client ID is required");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        // When
        ResponseEntity<ResultResponse<Void>> response = exceptionHandler.handleConstraintViolation(exception, Locale.US);

        // Then
        ResultResponse.Failure<?> failure = failureBody(response);
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("IAM-1000", failure.code()),
                () -> assertEquals("Client ID is required", failure.message())
        );
    }

    /**
     * Given 包含内部细节的未预期异常，When 映射异常，Then 响应不得泄露异常消息。
     */
    @Test
    void shouldHideUnexpectedExceptionDetails() {
        // Given
        String internalDetail = "internal persistence diagnostic";

        // When
        ResponseEntity<ResultResponse<Void>> response = exceptionHandler.handleUnexpectedException(
                new IllegalStateException(internalDetail), Locale.US);

        // Then
        ResultResponse.Failure<?> failure = failureBody(response);
        assertAll(
                () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()),
                () -> assertEquals("IAM-9000", failure.code()),
                () -> assertEquals("An internal server error occurred", failure.message()),
                () -> assertFalse(failure.message().contains(internalDetail))
        );
    }

    private ResultResponse.Failure<?> failureBody(ResponseEntity<ResultResponse<Void>> response) {
        ResultResponse<Void> body = Objects.requireNonNull(response.getBody(), "response body");
        return assertInstanceOf(ResultResponse.Failure.class, body);
    }

    private static Stream<Arguments> statusMappings() {
        return Stream.of(
                Arguments.of(IamErrorCode.INVALID_ARGUMENT, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(IamErrorCode.RESOURCE_CONFLICT, HttpStatus.CONFLICT),
                Arguments.of(IamErrorCode.TENANT_ID_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.USER_ID_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.CLIENT_ID_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.CLIENT_REDIRECT_URI_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.CLIENT_TOKEN_POLICY_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.CLIENT_SECRET_HASH_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.CLIENT_REGISTRATION_INVALID, HttpStatus.BAD_REQUEST),
                Arguments.of(IamErrorCode.AUTHENTICATION_FAILED, HttpStatus.UNAUTHORIZED),
                Arguments.of(IamErrorCode.ACCESS_DENIED, HttpStatus.FORBIDDEN),
                Arguments.of(IamErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }
}
