package com.lavyoung.iam.adapter.inbound.exception;

import com.lavyoung.iam.contract.platform.response.ResultResponse;
import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 将普通 REST 管理接口抛出的异常转换为稳定的 HTTP 与业务错误响应。
 *
 * <p>本处理器不处理 OAuth2/OIDC 标准端点异常，也不向调用方暴露异常消息、
 * 堆栈、数据库信息或其他内部实现细节。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.lavyoung.iam.adapter.inbound.rest")
@RequiredArgsConstructor
public class IamRestExceptionHandler {

    private final MessageSource messageSource;

    /**
     * 处理领域或应用层返回的稳定 IAM 业务异常。
     *
     * @param exception IAM 业务异常
     * @param locale    当前请求语言
     * @return 包含稳定错误码和本地化提示的响应
     */
    @ExceptionHandler(IamException.class)
    public ResponseEntity<ResultResponse<Void>> handleIamException(IamException exception, Locale locale) {
        IamErrorCode errorCode = exception.getErrorCode();
        String message = resolve(errorCode, exception.getMessageArguments(), locale);
        return failure(statusOf(errorCode), errorCode, message);
    }

    /**
     * 处理请求体 Jakarta Validation 校验失败。
     *
     * @param exception 请求参数校验异常
     * @param locale    当前请求语言
     * @return HTTP 400 参数错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, Locale locale) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> resolve(IamErrorCode.INVALID_ARGUMENT, List.of(), locale));
        return failure(HttpStatus.BAD_REQUEST, IamErrorCode.INVALID_ARGUMENT, message);
    }

    /**
     * 处理方法参数和路径参数约束校验失败。
     *
     * @param exception 方法参数约束异常
     * @param locale    当前请求语言
     * @return HTTP 400 参数错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception, Locale locale) {
        String message = exception.getConstraintViolations().stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> resolve(IamErrorCode.INVALID_ARGUMENT, List.of(), locale));
        return failure(HttpStatus.BAD_REQUEST, IamErrorCode.INVALID_ARGUMENT, message);
    }

    /**
     * 处理 JSON 损坏、参数缺失或参数类型不匹配。
     *
     * @param exception HTTP 请求解析异常
     * @param locale    当前请求语言
     * @return HTTP 400 参数错误响应
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ResultResponse<Void>> handleMalformedRequest(Exception exception, Locale locale) {
        log.debug("Rejected malformed REST request: {}", exception.getClass().getSimpleName());
        String message = resolve(IamErrorCode.INVALID_ARGUMENT, List.of(), locale);
        return failure(HttpStatus.BAD_REQUEST, IamErrorCode.INVALID_ARGUMENT, message);
    }

    /**
     * 处理未预期异常并隐藏内部实现信息。
     *
     * @param exception 未预期异常
     * @param locale    当前请求语言
     * @return HTTP 500 内部错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultResponse<Void>> handleUnexpectedException(Exception exception, Locale locale) {
        log.error("Unhandled exception in IAM REST endpoint", exception);
        String message = resolve(IamErrorCode.INTERNAL_ERROR, List.of(), locale);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, IamErrorCode.INTERNAL_ERROR, message);
    }

    private ResponseEntity<ResultResponse<Void>> failure(
            HttpStatus status, IamErrorCode errorCode, String message) {
        return ResponseEntity.status(status).body(ResultResponse.failure(errorCode.getCode(), message));
    }

    private String resolve(IamErrorCode errorCode, List<Object> arguments, Locale locale) {
        return messageSource.getMessage(errorCode.getMessageKey(), arguments.toArray(), locale);
    }

    private HttpStatus statusOf(IamErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_ARGUMENT, TENANT_ID_INVALID, USER_ID_INVALID, CLIENT_ID_INVALID,
                 CLIENT_REDIRECT_URI_INVALID, CLIENT_TOKEN_POLICY_INVALID,
                 CLIENT_SECRET_HASH_INVALID, CLIENT_REGISTRATION_INVALID -> HttpStatus.BAD_REQUEST;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RESOURCE_CONFLICT -> HttpStatus.CONFLICT;
            case AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}