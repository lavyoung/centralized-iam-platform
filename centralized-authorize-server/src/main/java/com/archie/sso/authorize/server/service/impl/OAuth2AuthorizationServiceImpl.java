package com.archie.sso.authorize.server.service.impl;

import com.archie.sso.authorize.server.entity.OAuth2AuthorizationEntity;
import com.archie.sso.authorize.server.mapper.OAuth2AuthorizationMapper;
import com.archie.sso.authorize.server.service.AuthorizationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author : lavyoung1325
 * @create 2023/9/28
 */
@Service
public class OAuth2AuthorizationServiceImpl extends ServiceImpl<OAuth2AuthorizationMapper, OAuth2AuthorizationEntity>
        implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthorizationServiceImpl.class);

    @Resource
    private RegisteredClientRepository registeredClientRepository;

    @Resource
    private OAuth2AuthorizationMapper oAuth2AuthorizationMapper;

    private final ObjectMapper objectMapper;

    public OAuth2AuthorizationServiceImpl() {
        this.objectMapper = new ObjectMapper();
        ClassLoader classLoader = OAuth2AuthorizationServiceImpl.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        this.objectMapper.registerModules(securityModules);
        this.objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        OAuth2Authorization auth2Authorization = findById(authorization.getId());
        if (auth2Authorization == null) {
            insertAuthorization(authorization);
        } else {
            updateAuthorization(authorization);
        }
    }

    private void updateAuthorization(OAuth2Authorization authorization) {
        this.baseMapper.updateById(dataToRow(authorization));
    }

    private void insertAuthorization(OAuth2Authorization authorization) {
        this.baseMapper.insert(dataToRow(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");
        logger.info("remove authorization: name: {}, ", authorization.getPrincipalName());
        oAuth2AuthorizationMapper.deleteById(authorization.getId());
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        Assert.notNull(id, "id  cannot be null");
        OAuth2AuthorizationEntity entity = oAuth2AuthorizationMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return findBy(Collections.singletonList(entity));
    }


    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");
        if (tokenType == null) {
            List<OAuth2AuthorizationEntity> sqlData = this.baseMapper.selectList(
                    Wrappers.lambdaQuery(OAuth2AuthorizationEntity.class)
                            .eq(OAuth2AuthorizationEntity::getState, token)
                            .or()
                            .eq(OAuth2AuthorizationEntity::getAuthorizationCodeValue, token)
                            .or()
                            .eq(OAuth2AuthorizationEntity::getAccessTokenValue, token)
                            .or()
                            .eq(OAuth2AuthorizationEntity::getRefreshTokenValue, token)
            );
            return findBy(sqlData);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            List<OAuth2AuthorizationEntity> sqlData = this.baseMapper.selectList(
                    Wrappers.lambdaQuery(OAuth2AuthorizationEntity.class)
                            .eq(OAuth2AuthorizationEntity::getState, token));
            return findBy(sqlData);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            List<OAuth2AuthorizationEntity> sqlData = this.baseMapper.selectList(
                    Wrappers.lambdaQuery(OAuth2AuthorizationEntity.class)
                            .eq(OAuth2AuthorizationEntity::getAuthorizationCodeValue, token));
            return findBy(sqlData);
        } else if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenType.getValue())) {
            List<OAuth2AuthorizationEntity> sqlData = this.baseMapper.selectList(
                    Wrappers.lambdaQuery(OAuth2AuthorizationEntity.class)
                            .eq(OAuth2AuthorizationEntity::getAccessTokenValue, token));
            return findBy(sqlData);
        } else if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenType.getValue())) {
            List<OAuth2AuthorizationEntity> sqlData = this.baseMapper.selectList(
                    Wrappers.lambdaQuery(OAuth2AuthorizationEntity.class)
                            .eq(OAuth2AuthorizationEntity::getRefreshTokenValue, token));
            return findBy(sqlData);
        }
        return null;
    }

    private OAuth2Authorization findBy(Collection<OAuth2AuthorizationEntity> data) {
        List<OAuth2Authorization> authorizations = mapRow(data);
        return !authorizations.isEmpty() ? authorizations.get(0) : null;
    }

    private List<OAuth2Authorization> mapRow(Collection<OAuth2AuthorizationEntity> data) {
        List<OAuth2Authorization> mapResult = new ArrayList<>();
        data.forEach(item -> {
            String registeredClientId = item.getRegisteredClientId();
            RegisteredClient client = registeredClientRepository.findById(registeredClientId);
            if (client == null) {
                throw new DataRetrievalFailureException(
                        "The RegisteredClient with id '" + registeredClientId + "' was not found in the RegisteredClientRepository.");
            }
            OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client);
            Set<String> authorizationScopes = Collections.emptySet();
            if (item.getAuthorizedScopes() != null) {
                authorizationScopes = StringUtils.commaDelimitedListToSet(item.getAuthorizedScopes());
            }
            Map<String, Object> attributes = parseMap(item.getAttributes());
            builder.id(item.getId())
                    .principalName(item.getPrincipalName())
                    .authorizationGrantType(new AuthorizationGrantType(item.getAuthorizationGrantType()))
                    .authorizedScopes(authorizationScopes)
                    .attributes(attrs -> attrs.putAll(attributes));
            if (StringUtils.hasText(item.getState())) {
                builder.attribute(OAuth2ParameterNames.STATE, item.getState());
            }
            if (StringUtils.hasText(item.getAuthorizationCodeValue())) {
                Map<String, Object> auhorizationCodeMetadataMap = parseMap(item.getAuthorizationCodeMetadata());
                OAuth2AuthorizationCode authorizationCodeMetadata = new OAuth2AuthorizationCode(
                        item.getAuthorizationCodeValue(), item.getAuthorizationCodeIssuedAt(), item.getAuthorizationCodeExpiresAt()
                );
                builder.token(authorizationCodeMetadata, metadata -> metadata.putAll(auhorizationCodeMetadataMap));
            }
            if (StringUtils.hasText(item.getAccessTokenValue())) {
                Map<String, Object> accessTokenMetadata = parseMap(item.getAccessTokenMetadata());
                OAuth2AccessToken.TokenType tokenType = null;
                if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(item.getAccessTokenType())) {
                    tokenType = OAuth2AccessToken.TokenType.BEARER;
                }

                Set<String> scopes = Collections.emptySet();
                if (item.getAccessTokenScopes() != null) {
                    scopes = StringUtils.commaDelimitedListToSet(item.getAccessTokenScopes());
                }
                OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, item.getAccessTokenValue(), item.getAccessTokenIssuedAt(), item.getAccessTokenExpiresAt(), scopes);
                builder.token(accessToken, (metadata) -> metadata.putAll(accessTokenMetadata));
            }
            if (StringUtils.hasText(item.getOidcIdTokenValue())) {
                Map<String, Object> oidcTokenMetadata = parseMap(item.getOidcIdTokenMetadata());
                OidcIdToken oidcIdToken = new OidcIdToken(
                        item.getOidcIdTokenValue(), item.getOidcIdTokenIssuedAt(), item.getOidcIdTokenExpiresAt(), (Map<String, Object>) oidcTokenMetadata.get(OAuth2Authorization.Token.CLAIMS_METADATA_NAME)
                );
                builder.token(oidcIdToken, metadata -> metadata.putAll(oidcTokenMetadata));
            }
            if (StringUtils.hasText(item.getRefreshTokenValue())) {
                Map<String, Object> refreshTokenMetadata = parseMap(item.getRefreshTokenMetadata());
                OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                        item.getRefreshTokenValue(), item.getRefreshTokenIssuedAt(), item.getRefreshTokenExpiresAt());
                builder.token(refreshToken, metadata -> metadata.putAll(refreshTokenMetadata));
            }
            if (StringUtils.hasText(item.getUserCodeValue())) {
                Map<String, Object> userCodeMetadata = parseMap(item.getUserCodeMetadata());
                OAuth2UserCode oAuth2UserCode = new OAuth2UserCode(item.getUserCodeValue(), item.getUserCodeIssuedAt(),
                        item.getUserCodeExpiresAt());
                builder.token(oAuth2UserCode, metadata -> metadata.putAll(userCodeMetadata));
            }
            if (StringUtils.hasText(item.getDeviceCodeValue())) {
                Map<String, Object> deviceCodeMetadata = parseMap(item.getDeviceCodeMetadata());
                OAuth2DeviceCode oAuth2DeviceCode = new OAuth2DeviceCode(item.getDeviceCodeValue(),
                        item.getDeviceCodeIssuedAt(), item.getDeviceCodeExpiresAt());
                builder.token(oAuth2DeviceCode, metadata -> metadata.putAll(deviceCodeMetadata));
            }
            mapResult.add(builder.build());
        });
        return mapResult;
    }

    private Map<String, Object> parseMap(String data) {
        try {
            return this.objectMapper.readValue(data, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }


    private OAuth2AuthorizationEntity dataToRow(OAuth2Authorization authorization) {
        OAuth2AuthorizationEntity entity = new OAuth2AuthorizationEntity();
        entity.setId(authorization.getId());
        entity.setRegisteredClientId(authorization.getRegisteredClientId());
        entity.setPrincipalName(authorization.getPrincipalName());
        entity.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
        if (!CollectionUtils.isEmpty(authorization.getAuthorizedScopes())) {
            entity.setAuthorizedScopes(StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ","));
        }
        String attributes = writeMap(authorization.getAttributes());
        entity.setAttributes(attributes);
        String authorizationState = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (StringUtils.hasText(authorizationState)) {
            entity.setState(attributes);
        }
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCode != null) {
            String tokenValue = authorizationCode.getToken().getTokenValue();
            if (tokenValue != null) {
                entity.setAuthorizationCodeValue(tokenValue);
            }
            if (authorizationCode.getToken().getIssuedAt() != null) {
                entity.setAuthorizationCodeIssuedAt(authorizationCode.getToken().getIssuedAt());
            }
            if (authorizationCode.getToken().getExpiresAt() != null) {
                entity.setAuthorizationCodeExpiresAt(authorizationCode.getToken().getExpiresAt());
            }
            if (authorizationCode.getMetadata() != null) {
                entity.setAuthorizationCodeMetadata(writeMap(authorizationCode.getMetadata()));
            }
        }
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getToken(OAuth2AccessToken.class);
        if (accessToken != null) {
            String tokenValue = accessToken.getToken().getTokenValue();
            if (tokenValue != null) {
                entity.setAccessTokenValue(tokenValue);
            }
            if (accessToken.getToken().getIssuedAt() != null) {
                entity.setAccessTokenIssuedAt(accessToken.getToken().getIssuedAt());
            }
            if (accessToken.getToken().getExpiresAt() != null) {
                entity.setAccessTokenExpiresAt(accessToken.getToken().getExpiresAt());
            }
            if (accessToken.getMetadata() != null) {
                entity.setAccessTokenMetadata(writeMap(accessToken.getMetadata()));
            }
            String accessTokenType = accessToken.getToken().getTokenType().getValue();
            entity.setAccessTokenType(accessTokenType);
            if (!CollectionUtils.isEmpty(accessToken.getToken().getScopes())) {
                String accessTokenScopes = StringUtils.collectionToDelimitedString(accessToken.getToken().getScopes(), ",");
                entity.setAccessTokenScopes(accessTokenScopes);
            }
        }
        OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
        if (oidcIdToken != null) {
            String tokenValue = oidcIdToken.getToken().getTokenValue();
            if (tokenValue != null) {
                entity.setOidcIdTokenValue(tokenValue);
            }
            if (oidcIdToken.getToken().getIssuedAt() != null) {
                entity.setOidcIdTokenIssuedAt(oidcIdToken.getToken().getIssuedAt());
            }
            if (oidcIdToken.getToken().getExpiresAt() != null) {
                entity.setOidcIdTokenExpiresAt(oidcIdToken.getToken().getExpiresAt());
            }
            if (oidcIdToken.getMetadata() != null) {
                entity.setOidcIdTokenMetadata(writeMap(oidcIdToken.getMetadata()));
            }
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            String tokenValue = refreshToken.getToken().getTokenValue();
            if (tokenValue != null) {
                entity.setRefreshTokenValue(tokenValue);
            }
            if (refreshToken.getToken().getIssuedAt() != null) {
                entity.setRefreshTokenIssuedAt(refreshToken.getToken().getIssuedAt());
            }
            if (refreshToken.getToken().getExpiresAt() != null) {
                entity.setRefreshTokenExpiresAt(refreshToken.getToken().getExpiresAt());
            }
            if (refreshToken.getMetadata() != null) {
                entity.setRefreshTokenMetadata(writeMap(refreshToken.getMetadata()));
            }
        }
        OAuth2Authorization.Token<OAuth2UserCode> userCodeToken = authorization.getToken(OAuth2UserCode.class);
        if (userCodeToken != null) {
            entity.setUserCodeValue(userCodeToken.getToken().getTokenValue());
            entity.setUserCodeIssuedAt(userCodeToken.getToken().getIssuedAt());
            entity.setUserCodeExpiresAt(userCodeToken.getToken().getExpiresAt());
            entity.setUserCodeMetadata(writeMap(userCodeToken.getMetadata()));
        }
        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCodeToken = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCodeToken != null) {
            entity.setDeviceCodeValue(deviceCodeToken.getToken().getTokenValue());
            entity.setDeviceCodeIssuedAt(deviceCodeToken.getToken().getIssuedAt());
            entity.setDeviceCodeExpiresAt(deviceCodeToken.getToken().getExpiresAt());
            entity.setDeviceCodeMetadata(writeMap(deviceCodeToken.getMetadata()));
        }
        return entity;
    }

    private String writeMap(Map<String, Object> data) {
        try {
            return this.objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
