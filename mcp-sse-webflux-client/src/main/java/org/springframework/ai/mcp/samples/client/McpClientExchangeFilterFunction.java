package org.springframework.ai.mcp.samples.client;

import reactor.core.publisher.Mono;

import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.server.ServerWebExchange;
import static org.springframework.ai.mcp.samples.client.McpWebFluxClientApplication.getExchangeFromContext;

/**
 * A wrapper around Spring Security's {@link ServerOAuth2AuthorizedClientExchangeFilterFunction}.
 *
 * We need to avoid getting tokens when the app comes up, and only get tokens for calling tools,
 * hence the wrapping.
 */
@Component
public class McpClientExchangeFilterFunction implements ExchangeFilterFunction {

    private final ServerOAuth2AuthorizedClientExchangeFilterFunction delegate;

    private static final String SERVER_WEB_EXCHANGE_ATTR_NAME = ServerWebExchange.class.getName();

    public McpClientExchangeFilterFunction(ReactiveOAuth2AuthorizedClientManager clientManager) {
        this.delegate = new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientManager);
        // TODO: parameterize this
        this.delegate.setDefaultClientRegistrationId("authserver");
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.empty()
                .doOnSubscribe(s -> {
                    System.out.println("McpClientExchangeFilterFunction intercept");
                    System.out.println("McpClientExchangeFilterFunction request.attributes.get(SERVER_WEB_EXCHANGE_ATTR_NAME): " + request.attributes().get(SERVER_WEB_EXCHANGE_ATTR_NAME));
                })
                .then(getExchangeFromContext("McpClientExchangeFilterFunction intercept"))
                .then(next.exchange(request));
        // Below is what we are ultimately trying to achieve:
//                .filter(Objects::nonNull)
//                .flatMap((ignored) -> this.delegate.filter(request, next))
//                .switchIfEmpty(next.exchange(request));
    }


}
