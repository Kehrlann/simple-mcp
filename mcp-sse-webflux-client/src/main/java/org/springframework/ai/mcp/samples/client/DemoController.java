package org.springframework.ai.mcp.samples.client;

import javax.validation.constraints.NotNull;

import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DemoController {
    private final ChatClient chatClient;

    public DemoController(ChatClient chatClient) {
        this.chatClient = chatClient;
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
                </form>
                """;
        return Mono.justOrEmpty(query)
                .flatMap(this::getChatResponse)
                .switchIfEmpty(Mono.just(""))
                .map(questionForm::formatted);
    }

    private Mono<String> getChatResponse(@NotNull String query) {
        return this.chatClient.prompt("What is the weather in %s right now?".formatted(query))
                .stream()
                .content()
                .collect(Collectors.joining());
    }

}