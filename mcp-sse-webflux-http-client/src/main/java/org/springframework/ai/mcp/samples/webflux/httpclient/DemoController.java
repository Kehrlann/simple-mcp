package org.springframework.ai.mcp.samples.webflux.httpclient;

import javax.validation.constraints.NotNull;

import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DemoController {

	private final ChatClient chatClient;

	private final AsyncTokenSupplier asyncTokenPublisher;

	DemoController(ChatClient chatClient, AsyncTokenSupplier asyncTokenPublisher) {
		this.chatClient = chatClient;
		this.asyncTokenPublisher = asyncTokenPublisher;
	}

	@GetMapping("/")
	Mono<String> index(String query) {
		String questionForm = """
				<h1>Demo controller</h1>
				%s
				<h2>Ask for the weather</h2>
				<p>In which city would you like to see the weather?</p>
				<form action="" method="GET">
				    <input type="text" name="query" value="" placeholder="Paris" />
					<button type="submit">Ask the LLM</button>
				</form>
				""";
		return Mono.justOrEmpty(query)
			.flatMap(this::getChatResponseDiscardingOutputBeforeToolCall)
			.switchIfEmpty(Mono.just(""))
			.map(questionForm::formatted);
	}

	// The response we get from the .stream() API is too long. It contains the first
	// output of the LLM ("I need to use this and that tool"), as well as the second
	// output with the tool result itself.
	// This is a hack, which discards everything the chat client generated before a tool
	// was called.
	private Mono<String> getChatResponseDiscardingOutputBeforeToolCall(@NotNull String query) {
		return this.chatClient.prompt("What is the weather in %s right now?".formatted(query))
			.stream()
			.chatResponse()
			// Discard everything before the first "tool call"
			.skipUntil(cr -> cr.getResults().stream().anyMatch(r -> r.getOutput().hasToolCalls()))
			.flatMapIterable(ChatResponse::getResults)
			// Only keep textual output
			.mapNotNull(Generation::getOutput)
			.mapNotNull(AssistantMessage::getText)
			.collect(Collectors.joining(" "))
			.contextWrite(ctx -> ctx.put("tokenPublisher", asyncTokenPublisher.getToken()));
	}

	// Ideally we want to use this
	private Mono<String> getChatResponse(@NotNull String query) {
		return this.chatClient.prompt("What is the weather in %s right now?".formatted(query))
			.stream()
			.content()
			.collect(Collectors.joining(" "));
	}

}