package org.springframework.ai.mcp.samples.client;

import io.modelcontextprotocol.client.SyncTokenSupplier;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.SecurityContextHolderPrincipalResolver;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Spring-specific implementation of MCP's {@link SyncTokenSupplier}. Gets an OAuth2 token
 * from the Authorization Server, and makes it available.
 * <p>
 * The end goal is to use access_token that represent the end-user's permissions. Those
 * tokens are obtained using the {@code authorization_code} OAuth2 flow, but it requires a
 * user to be present and using their browser.
 * <p>
 * By default, the MCP tools are initialized on app startup, so some requests to the MCP
 * server happen, to establish the session (/sse), and to send the {@code initialize} and
 * e.g. {@code tools/list} requests. For this to work, we need an access_token, but we
 * cannot get one using the authorization_code flow (no user is present). Instead, we rely
 * on the OAuth2 {@code client_credentials} flow for machine-to-machine communication.
 *
 */
public class SpringSyncTokenSupplier implements SyncTokenSupplier {

	private final ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();

	private final ClientRegistrationRepository clientRegistrationRepository;

	private final DefaultOAuth2AuthorizedClientManager oauth2AuthorizedClientManager;

	private final OAuth2ClientHttpRequestInterceptor.PrincipalResolver principalResolver = new SecurityContextHolderPrincipalResolver();

	// Must match registration id in property
	// spring.security.oauth2.client.registration.<REGISTRATION-ID>.authorization-grant-type=authorization_code
	private static final String AUTHORIZATION_CODE_CLIENT_REGISTRATION_ID = "authserver";

	// Must match registration id in property
	// spring.security.oauth2.client.registration.<REGISTRATION-ID>.authorization-grant-type=client_credentials
	private static final String CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID = "authserver-client-credentials";

	// Hardcoded "anonymous" authentication, to match the Spring Security API.
	private static final AnonymousAuthenticationToken ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken(
			"client-credentials-client", "client-credentials-client",
			AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

	public SpringSyncTokenSupplier(ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository clientRepository) {
		this.oauth2AuthorizedClientManager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository,
				clientRepository);
		this.oauth2AuthorizedClientManager.setAuthorizedClientProvider(
				OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build());
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public Optional<String> getToken() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
			// In the context of a web request, there is an end-user interacting with the
			// system, so we can use authorization code flow.
			return Optional.of(authorizationCodeToken());
		}
		// When outside of a web request, use client credentials flow.
		return Optional.of(clientCredentialsToken());
	}

	private String authorizationCodeToken() {
		var authRequest = OAuth2AuthorizeRequest.withClientRegistrationId(AUTHORIZATION_CODE_CLIENT_REGISTRATION_ID)
			// TODO: get authentication from SecurityContext
			.principal(ANONYMOUS_AUTHENTICATION)
			.build();
		return oauth2AuthorizedClientManager.authorize(authRequest).getAccessToken().getTokenValue();
	}

	private String clientCredentialsToken() {
		var clientRegistration = this.clientRegistrationRepository
			.findByRegistrationId(CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID);
		var authRequest = OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
			.principal(ANONYMOUS_AUTHENTICATION)
			.build();
		return clientCredentialTokenProvider.authorize(authRequest).getAccessToken().getTokenValue();
	}

}
