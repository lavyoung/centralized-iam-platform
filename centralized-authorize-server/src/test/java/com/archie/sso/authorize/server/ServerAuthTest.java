package com.archie.sso.authorize.server;

import com.archie.sso.authorize.server.service.AuthorizationService;
import com.archie.sso.authorize.server.service.ClientService;
import com.archie.sso.authorize.server.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "iam.messaging.rocketmq.enabled=false")
@AutoConfigureMockMvc
public class ServerAuthTest {

    private static final String CLIENT_ID = "CAM";

    private static final String CLIENT_SECRET = "secret";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ClientService clientService;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private UserService userService;

    /**
     * Keeps protocol tests on the real security filter chain while replacing environment-bound persistence ports
     * with the official Spring Authorization Server in-memory implementations.
     */
    @BeforeEach
    void setUpInMemoryStores() {
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(CLIENT_ID)
                .clientSecret(this.passwordEncoder.encode(CLIENT_SECRET))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("profile")
                .scope("cam:read")
                .build();
        InMemoryRegisteredClientRepository clientStore =
                new InMemoryRegisteredClientRepository(registeredClient);
        InMemoryOAuth2AuthorizationService authorizationStore = new InMemoryOAuth2AuthorizationService();

        when(this.clientService.findById(anyString())).thenAnswer(invocation ->
                clientStore.findById(invocation.getArgument(0)));
        when(this.clientService.findByClientId(anyString())).thenAnswer(invocation ->
                clientStore.findByClientId(invocation.getArgument(0)));
        doAnswer(invocation -> {
            authorizationStore.save(invocation.getArgument(0, OAuth2Authorization.class));
            return null;
        }).when(this.authorizationService).save(any(OAuth2Authorization.class));
        when(this.authorizationService.findById(anyString())).thenAnswer(invocation ->
                authorizationStore.findById(invocation.getArgument(0)));
        when(this.authorizationService.findByToken(anyString(), nullable(OAuth2TokenType.class)))
                .thenAnswer(invocation -> authorizationStore.findByToken(
                        invocation.getArgument(0), invocation.getArgument(1)));
    }

    @Test
    void performTokenRequestWhenValidClientCredentialsThenOk() throws Exception {
        // @formatter:off
        this.mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "client_credentials")
                        .param("scope", "profile")
                        .with(basicAuth(CLIENT_ID, CLIENT_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andExpect(jsonPath("$.scope").value("profile"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
        // @formatter:on
    }


    /**
     * 无授权客户端
     *
     * @throws Exception
     */
    @Test
    void performTokenRequestWhenInvalidClientCredentialsThenUnauthorized() throws Exception {
        // @formatter:off
        this.mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "client_credentials")
                        .param("scope", "cam:read")
                        .with(basicAuth("CAM", "password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
        // @formatter:on
    }

    /**
     * Test case to perform an introspection request with a valid token and expect an OK response.
     *
     * @throws Exception if an error occurs during the test
     */

    @Test
    void performIntrospectionRequestWhenValidTokenThenOk() throws Exception {
        // @formatter:off
        this.mockMvc.perform(post("/oauth2/introspect")
                        .param("token", getAccessToken())
                        .with(basicAuth(CLIENT_ID, CLIENT_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value("true"))
                .andExpect(jsonPath("$.aud[0]").value(CLIENT_ID))
                .andExpect(jsonPath("$.client_id").value(CLIENT_ID))
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.iat").isNumber())
                .andExpect(jsonPath("$.iss").value("http://localhost"))
                .andExpect(jsonPath("$.nbf").isNumber())
                .andExpect(jsonPath("$.scope").value("cam:read"))
                .andExpect(jsonPath("$.sub").value(CLIENT_ID))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
        // @formatter:on
    }


    private String getAccessToken() throws Exception {
        // @formatter:off
        MvcResult mvcResult = this.mockMvc.perform(post("/oauth2/token")
                        .param("grant_type", "client_credentials")
                        .param("scope", "cam:read")
                        .with(basicAuth(CLIENT_ID, CLIENT_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();
        // @formatter:on

        String tokenResponseJson = mvcResult.getResponse().getContentAsString();
        Map<String, Object> tokenResponse = this.objectMapper.readValue(tokenResponseJson, new TypeReference<Map<String, Object>>() {
        });

        return tokenResponse.get("access_token").toString();
    }


    /**
     * Creates a BasicAuthenticationRequestPostProcessor with the given username and password.
     *
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @return A BasicAuthenticationRequestPostProcessor object.
     */

    private static BasicAuthenticationRequestPostProcessor basicAuth(String username, String password) {
        return new BasicAuthenticationRequestPostProcessor(username, password);
    }

    private static final class BasicAuthenticationRequestPostProcessor implements RequestPostProcessor {

        private final String username;

        private final String password;

        private BasicAuthenticationRequestPostProcessor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        /**
         * Adds basic authentication headers to the request.
         *
         * @param request The original request object.
         * @return The modified request object with basic authentication headers.
         */
        @Override
        public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(this.username, this.password);
            request.addHeader("Authorization", Objects.requireNonNull(headers.getFirst("Authorization")));
            return request;
        }

    }

}
