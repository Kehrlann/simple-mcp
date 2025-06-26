package org.springframework.ai.mcp.samples.webflux.httpclient;

import io.modelcontextprotocol.client.McpAsyncTokenSupplier;
import java.time.Instant;
import reactor.core.publisher.Mono;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
class AsyncTokenSupplier implements McpAsyncTokenSupplier {

	private final ReactiveClientRegistrationRepository clientRegistrationRepository;

	private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

	private final ClientCredentialsReactiveOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();

	AsyncTokenSupplier(ReactiveClientRegistrationRepository clientRegistrationRepository,
			ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository,
				authorizedClientRepository);
	}

	@Override
	public Mono<String> getToken() {
		return isWebContext().flatMap(isWebContext -> {
			// Don't use switchIfEmpty because getClientCredentialsAccessToken can return
			// an empty mono
			// TODO: what happens if empty mono?
			if (!isWebContext) {
				return getClientCredentialsAccessToken()
					.doOnSubscribe(s -> System.out.println("🤖 requesting token client_credentials"));
			}
			else {
				return getAuthorizationCodeAccessToken()
					.doOnSubscribe(s -> System.out.println("👩 requesting token authorization_code"));
			}
		}).doOnSubscribe(s -> System.out.println("Wrapping subscribe " + Instant.now()));
	}

	private static Mono<Boolean> isWebContext() {
		return Mono.deferContextual(Mono::just).map(c -> c.hasKey(ServerWebExchange.class));
	}

	public Mono<String> getClientCredentialsAccessToken() {
		return this.clientRegistrationRepository.findByRegistrationId("authserver-client-credentials") // TODO
			.map(clientRegistration -> OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
				.principal(new AnonymousAuthenticationToken("client-credentials-client", "client-credentials-client",
						AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
				.build())
			.flatMap(clientCredentialTokenProvider::authorize)
			.map(OAuth2AuthorizedClient::getAccessToken)
			.map(AbstractOAuth2Token::getTokenValue);
	}

	public Mono<String> getAuthorizationCodeAccessToken() {
		return ReactiveSecurityContextHolder.getContext()
			.map(SecurityContext::getAuthentication)
			.map(auth -> OAuth2AuthorizeRequest.withClientRegistrationId("authserver").principal(auth).build())
			.flatMap(this.authorizedClientManager::authorize)
			.map(OAuth2AuthorizedClient::getAccessToken)
			.map(AbstractOAuth2Token::getTokenValue);
	}

}
