package org.springframework.ai.mcp.samples.servlet.webfluxclient;

import java.util.function.Consumer;
import reactor.core.publisher.Mono;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A wrapper around Spring Security's
 * {@link ServletOAuth2AuthorizedClientExchangeFilterFunction}, which adds OAuth2
 * {@code access_token}s to requests sent to the MCP server.
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
 */

@Component
public class McpSyncClientExchangeFilterFunction implements ExchangeFilterFunction {

	private final ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();

	private final ServletOAuth2AuthorizedClientExchangeFilterFunction delegate;

	private final ClientRegistrationRepository clientRegistrationRepository;

	// Must match registration id in property
	// spring.security.oauth2.client.registration.<REGISTRATION-ID>.authorization-grant-type=authorization_code
	private static final String AUTHORIZATION_CODE_CLIENT_REGISTRATION_ID = "authserver";

	// Must match registration id in property
	// spring.security.oauth2.client.registration.<REGISTRATION-ID>.authorization-grant-type=client_credentials
	private static final String CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID = "authserver-client-credentials";

	public McpSyncClientExchangeFilterFunction(OAuth2AuthorizedClientManager clientManager,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.delegate = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientManager);
		this.delegate.setDefaultClientRegistrationId(AUTHORIZATION_CODE_CLIENT_REGISTRATION_ID);
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	/**
	 * Add an {@code access_token} to the request sent to the MCP server.
	 * <p>
	 * If we are in the context of a ServletRequest, this means a user is currently
	 * involved, and we should add a token on behalf of the user, using the
	 * {@code authorization_code} grant. This typically happens when doing an MCP
	 * {@code tools/call}.
	 * <p>
	 * If we are NOT in the context of a ServletRequest, this means we are in the startup
	 * phases of the application, where the MCP client is initialized. We use the
	 * {@code client_credentials} grant in that case, and add a token on behalf of the
	 * application itself.
	 */
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
			return this.delegate.filter(request, next);
		}
		else {
			var accessToken = getClientCredentialsAccessToken();
			var requestWithToken = ClientRequest.from(request)
				.headers(headers -> headers.setBearerAuth(accessToken))
				.build();
			return next.exchange(requestWithToken);
		}
	}

	private String getClientCredentialsAccessToken() {
		var clientRegistration = this.clientRegistrationRepository
			.findByRegistrationId(CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID);

		var authRequest = OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
			.principal(new AnonymousAuthenticationToken("client-credentials-client", "client-credentials-client",
					AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
			.build();
		return this.clientCredentialTokenProvider.authorize(authRequest).getAccessToken().getTokenValue();
	}

	/**
	 * Configure a {@link WebClient} to use this exchange filter function.
	 */
	public Consumer<WebClient.Builder> configuration() {
		return builder -> builder.defaultRequest(this.delegate.defaultRequest()).filter(this);
	}

}
