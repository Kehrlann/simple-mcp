package org.springframework.ai.mcp.samples.client;

import reactor.core.publisher.Mono;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;

/**
 * A wrapper around Spring Security's
 * {@link ServerOAuth2AuthorizedClientExchangeFilterFunction}, which adds OAuth2
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
public class McpClientExchangeFilterFunction implements ExchangeFilterFunction {

	private final ClientCredentialsReactiveOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();

	private final ServerOAuth2AuthorizedClientExchangeFilterFunction delegate;

	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	public McpClientExchangeFilterFunction(ReactiveOAuth2AuthorizedClientManager clientManager,
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		this.delegate = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientManager);
		this.delegate.setDefaultClientRegistrationId("authserver"); // TODO
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	/**
	 * Add an {@code access_token}
	 */
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		//@formatter:off
		return verifyContextHasServerWebExchange()
			// If we are in a request context, i.e. a user interaction, use authorization_code flow
			.flatMap((ignored) -> this.delegate.filter(request, next).doOnSubscribe(s -> System.out.println("👩 requesting token authorization_code")))
			// Else, use the client_credentials grant
			.switchIfEmpty(clientCredentials(request, next).doOnSubscribe(s -> System.out.println("🤖 requesting token client_credentials")));
		//@formatter:on
	}

	private static Mono<ServerWebExchange> verifyContextHasServerWebExchange() {
		return Mono.deferContextual(Mono::just)
			.filter(c -> c.hasKey(ServerWebExchange.class))
			.map(c -> c.get(ServerWebExchange.class));
	}

	private Mono<ClientResponse> clientCredentials(ClientRequest request, ExchangeFunction next) {
		return getClientCredentialsAccessToken()
			.map(token -> ClientRequest.from(request).headers(headers -> headers.setBearerAuth(token)).build())
			.flatMap(next::exchange);
	}

	private Mono<String> getClientCredentialsAccessToken() {
		return this.clientRegistrationRepository.findByRegistrationId("authserver-client-credentials") // TODO
			.map(clientRegistration -> OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
				.principal(new AnonymousAuthenticationToken("client-credentials-client", "client-credentials-client",
						AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
				.build())
			.flatMap(clientCredentialTokenProvider::authorize)
			.map(OAuth2AuthorizedClient::getAccessToken)
			.map(AbstractOAuth2Token::getTokenValue);
	}

}
