package org.springframework.ai.mcp.sample.authserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SpringBootApplication
public class AuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServerApplication.class, args);
    }

    @Bean
    UserDetailsService UserDetailsService() {
        var builder = User.withDefaultPasswordEncoder().password("password").roles("user");
        return new InMemoryUserDetailsManager(
                builder.username("user").build(),
                builder.username("alice").build(),
                builder.username("bob").build()
        );
	}

}
