package org.springframework.ai.mcp.samples.client;

import io.modelcontextprotocol.client.SyncTokenSupplier;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class SpringSyncTokenSupplier implements SyncTokenSupplier {

	private final ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();

	private final ClientRegistrationRepository clientRegistrationRepository;

	public SpringSyncTokenSupplier(ClientRegistrationRepository clientRegistrationRepository) {
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public Optional<String> getToken() {
		var clientRegistration = this.clientRegistrationRepository
			.findByRegistrationId("authserver-client-credentials");
		var authRequest = OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
			.principal(new AnonymousAuthenticationToken("client-credentials-client", "client-credentials-client",
					AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
			.build();
		var tokenResponse = clientCredentialTokenProvider.authorize(authRequest);
		return Optional.of(tokenResponse.getAccessToken().getTokenValue());
	}

}
