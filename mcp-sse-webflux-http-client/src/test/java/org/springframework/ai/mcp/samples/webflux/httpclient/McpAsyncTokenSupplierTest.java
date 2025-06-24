package org.springframework.ai.mcp.samples.webflux.httpclient;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class McpAsyncTokenSupplierTest {

	@Test
	void thingy() {
		Mono.just("start")
				.doOnSubscribe(s -> System.out.println("start subscribed"))
				.<String>handle((value, sink) -> {
			sink.error(new RuntimeException("nope"));
		})
			.switchIfEmpty(Mono.just("switched").doOnSubscribe(s -> System.out.println("empty subscribed")))
			.as(StepVerifier::create)
			.expectError()
			.verify();
	}

}
