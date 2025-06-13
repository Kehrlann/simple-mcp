/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.mcp.samples.client;

import io.modelcontextprotocol.client.McpAsyncClient;
import java.util.List;
import reactor.core.publisher.Mono;

import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicConnectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableWebFluxSecurity
public class McpWebFluxClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpWebFluxClientApplication.class, args);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, List<McpAsyncClient> mcpClients) {
        return chatClientBuilder
                .defaultToolCallbacks(new AsyncMcpToolCallbackProvider(mcpClients))
                .build();
    }

    /**
     * Overload Boot's default {@link WebClient.Builder}, so that we can inject an oauth2-enabled
     * {@link ExchangeFilterFunction} that adds OAuth2 tokens to requests sent to the MCP server.
     */
    @Bean
    WebClient.Builder webClientBuilder(McpAsyncClientExchangeFilterFunction filter) {
        return WebClient.builder().filter(filter);
    }

    /**
     * Override configuration from {@link AnthropicChatAutoConfiguration}.
     *
     * This ensures we're not using the same {@link WebClient.Builder} that what's used to talk to the MCP server,
     * because we do not want to request OAuth2 tokens to talk to Anthropic.
     */
    @Bean
    public AnthropicApi anthropicApi(AnthropicConnectionProperties connectionProperties, ResponseErrorHandler responseErrorHandler) {
        return AnthropicApi.builder()
                .baseUrl(connectionProperties.getBaseUrl())
                .completionsPath(connectionProperties.getCompletionsPath())
                .apiKey(connectionProperties.getApiKey())
                .anthropicVersion(connectionProperties.getVersion())
                .anthropicBetaFeatures(connectionProperties.getBetaVersion())
                .responseErrorHandler(responseErrorHandler)
                .restClientBuilder(
                        RestClient.builder().requestInterceptor((request, body, execution) -> {
                            // Show that we are NOT using the RestClient, this should never come up
                            System.out.println("⛔️ Anthropic RestClient Intercept");
                            return execution.execute(request, body);
                        })
                )
                .webClientBuilder(WebClient.builder()
                        .filter((req, next) -> Mono.empty()
                                // Show that we are indeed using the WebClient
                                .doOnSubscribe(s -> System.out.println("✅ Anthropic WebClient Intercept"))
                                .then(next.exchange(req))))
                .build();
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(authorize -> authorize
                        .anyExchange().permitAll()
                )
                .oauth2Client(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

}