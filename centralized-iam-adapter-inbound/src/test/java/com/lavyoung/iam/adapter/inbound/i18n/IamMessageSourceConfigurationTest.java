package com.lavyoung.iam.adapter.inbound.i18n;

import com.lavyoung.iam.shared.error.IamErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 IAM 入站国际化配置的默认语言和请求语言解析策略。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class IamMessageSourceConfigurationTest {

    private final IamMessageSourceConfiguration configuration = new IamMessageSourceConfiguration();

    /**
     * Given 中英文 Locale，When 解析相同错误码，Then 应返回对应语言的安全提示。
     */
    @Test
    void shouldResolveLocalizedErrorMessages() {
        // Given
        MessageSource messageSource = configuration.messageSource();
        String messageKey = IamErrorCode.INVALID_ARGUMENT.getMessageKey();

        // When
        String englishMessage = messageSource.getMessage(messageKey, null, Locale.US);
        String chineseMessage = messageSource.getMessage(messageKey, null, Locale.SIMPLIFIED_CHINESE);

        // Then
        assertAll(
                () -> assertEquals("Invalid request argument", englishMessage),
                () -> assertEquals("请求参数不合法", chineseMessage)
        );
    }

    /**
     * Given 不支持的语言，When 解析消息，Then 应回退到显式配置的美式英语而非系统 Locale。
     */
    @Test
    void shouldFallBackToConfiguredDefaultLocale() {
        // Given
        MessageSource messageSource = configuration.messageSource();

        // When
        String message = messageSource.getMessage(IamErrorCode.ACCESS_DENIED.getMessageKey(), null, Locale.FRANCE);

        // Then
        assertEquals("Access denied", message);
    }

    /**
     * Given 中文和不支持语言的请求头，When 解析请求 Locale，Then 应分别返回中文和默认英语。
     */
    @Test
    void shouldResolveSupportedLocaleFromAcceptLanguageHeader() {
        // Given
        LocaleResolver localeResolver = configuration.localeResolver();
        MockHttpServletRequest chineseRequest = requestWithLanguage("zh-CN");
        MockHttpServletRequest unsupportedRequest = requestWithLanguage("fr-FR");

        // When / Then
        assertAll(
                () -> assertEquals(Locale.SIMPLIFIED_CHINESE, localeResolver.resolveLocale(chineseRequest)),
                () -> assertEquals(Locale.US, localeResolver.resolveLocale(unsupportedRequest))
        );
    }

    private MockHttpServletRequest requestWithLanguage(String language) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", language);
        return request;
    }
}
