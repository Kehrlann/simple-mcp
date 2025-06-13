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
package org.springframework.ai.mcp.samples.servlet.webfluxclient;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class McpServletWebfluxClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServletWebfluxClientApplication.class, args);
	}

	@Bean
	ChatClient chatClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
		return chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpClients)).build();
	}

	/**
	 * Overload Boot's default {@link WebClient.Builder}, so that we can inject an
	 * oauth2-enabled {@link ExchangeFilterFunction} that adds OAuth2 tokens to requests
	 * sent to the MCP server.
	 */
	@Bean
	WebClient.Builder webClientBuilder(McpSyncClientExchangeFilterFunction filterFunction) {
		return WebClient.builder().apply(filterFunction.configuration());
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.oauth2Client(Customizer.withDefaults())
			.csrf(CsrfConfigurer::disable)
			.build();
	}

}