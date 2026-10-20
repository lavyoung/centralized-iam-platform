package com.archie.sso.authorize.server.authorization.pwd.converter;

import com.archie.sso.authorize.common.granter.CustomAuthorizationGrantType;
import com.archie.sso.authorize.common.utils.OAuth2EndpointUtils;
import com.archie.sso.authorize.server.authorization.pwd.token.OAuth2PasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!CustomAuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
            log.debug("not supported grant type: {}", CustomAuthorizationGrantType.PASSWORD.getValue());
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getFormParameters(request);

        String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
        if (!StringUtils.hasText(username) || parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
            OAuth2EndpointUtils.throwError(OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.USERNAME,
                    "");
        }
        String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.USERNAME) && !key.equals(OAuth2ParameterNames.PASSWORD) && !key.equals(
                    OAuth2ParameterNames.CLIENT_ID)) {
                additionalParameters.put(key, value.get(0));
            }
        });
        return new OAuth2PasswordAuthenticationToken(username, password, authentication, additionalParameters);
    }

}
