package com.lavyoung.iam.adapter.inbound.i18n;

import com.lavyoung.iam.shared.error.IamErrorCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 IAM 国际化资源的完整性与占位符一致性。
 *
 * <p>测试直接读取每个资源文件，避免 ResourceBundle 的父级回退掩盖
 * 某种语言缺少翻译的问题。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
class IamMessageBundleTest {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)}");

    private static final List<String> RESOURCE_PATHS = List.of(
            "i18n/messages.properties",
            "i18n/messages_zh_CN.properties",
            "i18n/messages_en_US.properties"
    );

    /**
     * Given 全部错误码与语言资源，When 检查资源键，Then 每种语言都必须提供非空文案。
     *
     * @throws IOException 当测试资源无法读取时抛出
     */
    @Test
    void shouldProvideEveryErrorMessageInEveryBundle() throws IOException {
        for (String resourcePath : RESOURCE_PATHS) {
            Properties messages = load(resourcePath);

            assertAll(Arrays.stream(IamErrorCode.values()).map(errorCode -> () -> {
                String message = messages.getProperty(errorCode.getMessageKey(), "");
                assertFalse(message.isBlank(), resourcePath + " 缺少 " + errorCode.getMessageKey());
            }));
        }
    }

    /**
     * Given 同一资源键的多语言文案，When 提取占位符，Then 参数序号必须完全一致。
     *
     * @throws IOException 当测试资源无法读取时抛出
     */
    @Test
    void shouldKeepPlaceholderIndexesAlignedAcrossBundles() throws IOException {
        Properties baseline = load(RESOURCE_PATHS.get(0));

        for (String resourcePath : RESOURCE_PATHS.subList(1, RESOURCE_PATHS.size())) {
            Properties localized = load(resourcePath);

            for (IamErrorCode errorCode : IamErrorCode.values()) {
                String messageKey = errorCode.getMessageKey();
                assertEquals(
                        placeholders(baseline.getProperty(messageKey)),
                        placeholders(localized.getProperty(messageKey)),
                        resourcePath + " 的占位符与默认资源不一致：" + messageKey
                );
            }
        }
    }

    /**
     * 以 UTF-8 读取指定资源文件。
     *
     * @param resourcePath 类路径资源位置
     * @return 资源键值集合
     * @throws IOException 当资源无法读取时抛出
     */
    private Properties load(String resourcePath) throws IOException {
        ClassLoader classLoader = IamMessageBundleTest.class.getClassLoader();

        try (InputStream input = Objects.requireNonNull(
                classLoader.getResourceAsStream(resourcePath),
                resourcePath
        )) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return properties;
        }
    }

    /**
     * 提取文案使用的 MessageFormat 参数序号。
     *
     * @param message 国际化文案
     * @return 不可变参数序号集合
     */
    private Set<String> placeholders(String message) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        Set<String> indexes = new HashSet<>();

        while (matcher.find()) {
            indexes.add(matcher.group(1));
        }

        return Set.copyOf(indexes);
    }
}
