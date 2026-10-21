package com.lavyoung.iam.adapter.inbound.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 配置 REST 入站边界使用的国际化资源与请求语言解析策略。
 *
 * <p>默认使用美式英语，并禁止回退到服务器操作系统 Locale，
 * 避免相同部署包在不同服务器上产生不一致的错误提示。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Configuration(proxyBeanMethods = false)
public class IamMessageSourceConfiguration {

    /**
     * 创建 IAM 国际化消息源。
     *
     * @return 使用 UTF-8 编码且不回退系统 Locale 的消息源
     */
    @Bean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setDefaultLocale(Locale.US);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * 创建基于 {@code Accept-Language} 请求头的语言解析器。
     *
     * @return 支持简体中文和美式英语的语言解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setSupportedLocales(List.of(Locale.SIMPLIFIED_CHINESE, Locale.US));
        localeResolver.setDefaultLocale(Locale.US);
        return localeResolver;
    }
}
