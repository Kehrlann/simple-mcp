package org.springframework.ai.mcp.samples.servlet.webfluxclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DemoController {
    private final ChatClient chatClient;

    public DemoController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/")
    String index(String query) {
        var currentWeatherBlock = "";
        if (StringUtils.hasText(query)) {
            var chatResponse = chatClient.prompt("What is the weather in %s right now?".formatted(query))
                    .call()
                    .content();

            currentWeatherBlock = """
                    <h2>Weather in %s</h2>
                    <p>%s</p>
                    <form action="" method="GET">
                    <button type="submit">Clear</button>
                    </form>
                    """.formatted(query, chatResponse);
        }
        return """
                <h1>Demo controller</h1>
                %s
                <h2>Ask for the weather</h2>
                <p>In which city would you like to see the weather?</p>
                <form action="" method="GET">
                    <input type="text" name="query" value="" placeholder="Paris" />
                    <button type="submit">Ask the LLM</button>
                </form>
                """.formatted(currentWeatherBlock);
    }
}
