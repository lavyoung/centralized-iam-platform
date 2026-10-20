package com.lavyoung.iam.domain.clientregistration.model;

import com.lavyoung.iam.shared.error.IamErrorCode;
import com.lavyoung.iam.shared.error.IamException;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 表示经过安全校验的 OAuth2/OIDC 客户端重定向地址。
 *
 * @param value 保持原样的重定向地址
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
public record RedirectUri(String value) {

    private static final int MAX_LENGTH = 2048;

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    /**
     * 校验并创建重定向地址。
     *
     * @param value 重定向地址原始值
     * @throws IamException 当地址为空、格式非法或违反安全约束时抛出
     */
    public RedirectUri {
        URI uri = parse(value);

        if (value.length() > MAX_LENGTH
                || !uri.isAbsolute()
                || uri.isOpaque()
                || uri.getUserInfo() != null
                || uri.getFragment() != null
                || !usesAllowedTransport(uri)) {
            throw invalidRedirectUri();
        }
    }

    private static URI parse(String value) {
        if (value == null || value.isBlank()) {
            throw invalidRedirectUri();
        }

        try {
            return URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalidRedirectUri();
        }
    }

    private static boolean usesAllowedTransport(URI uri) {
        String host = uri.getHost();

        if (host == null) {
            return false;
        }

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }

        return "http".equalsIgnoreCase(uri.getScheme()) && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    }

    private static IamException invalidRedirectUri() {
        return new IamException(IamErrorCode.CLIENT_REDIRECT_URI_INVALID, MAX_LENGTH);
    }
}
