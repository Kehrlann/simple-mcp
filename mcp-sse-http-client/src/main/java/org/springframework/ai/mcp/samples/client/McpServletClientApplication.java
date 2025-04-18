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

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootApplication
public class McpServletClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServletClientApplication.class, args);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpClients) {
        return chatClientBuilder
                .defaultTools(new SyncMcpToolCallbackProvider(mcpClients))
                .build();
    }

    @Bean
    TokenProvider tokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        return () -> {
            Authentication principal = SecurityContextHolder.getContext().getAuthentication();
            if (principal == null) {
                principal = new AnonymousAuthenticationToken("anonymous",
                        "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            }

            var requestAttributes = RequestContextHolder.getRequestAttributes();
            if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
                return null;
            }
            HttpServletRequest request = servletRequestAttributes.getRequest();
            HttpServletResponse response = servletRequestAttributes.getResponse();

            // @formatter:off
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    // matches spring.security.oauth2.client.registration.<REGISTRATION-ID>
                    // from application.properties
                    .withClientRegistrationId("authserver")
                    .principal(principal)
                    .attribute(HttpServletRequest.class.getName(), request)
                    .attribute(HttpServletResponse.class.getName(), response)
                    .build();
            // @formatter:on

            return authorizedClientManager.authorize(authorizeRequest)
                    .getAccessToken()
                    .getTokenValue();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Client(Customizer.withDefaults())
                .csrf(CsrfConfigurer::disable)
                .build();
    }
}