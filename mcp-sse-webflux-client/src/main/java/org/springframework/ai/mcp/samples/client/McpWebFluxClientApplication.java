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

import java.util.List;

import io.modelcontextprotocol.client.McpAsyncClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
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

	@Bean
	WebClient.Builder webClientBuilder(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
				new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth2Client.setDefaultClientRegistrationId("authserver");
		return WebClient.builder()
				.filter(oauth2Client);
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