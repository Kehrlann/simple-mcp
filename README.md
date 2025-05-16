# Spring AI MCP Security demo

This repository showcases Spring AI MCP security integration.

## Usage

1. Run the authorization-server project, it starts on port 9000
2. Run the MCP Server (mcp-weather-webmvc-server)
3. Run the MCP client **Servlet** app (mcp-sse-http-client)
4. Navigate to http://127.0.0.1:8080/
    - Don't use localhost!
5. When you try to get the temperature for, say, Paris, you will be redirected to the auth-server on port 9000. Log in
   with `user` / `password`. Tadaa 🎉
    - there will be a nasty error in the stack trace. This is expected, it's from Spring Security's OAuth2 internals.
6. Subsequent calls will not require a login.

## Notes on WebFlux

If you would like to build a fully reactive Spring AI app, you need to use WebFlux. Some changes to Spring AI are
required to get it to work.

You MUST use this exact build of Spring
AI: https://github.com/Kehrlann/spring-ai/tree/dgarnier/poc-propagate-reactive-context-to-mcp-async

1. Run the authorization-server project, it starts on port 9000
2. Run the MCP Server (mcp-weather-webmvc-server)
3. Run the MCP client **Reactive** app (mcp-sse-webflux-client)
4. Navigate to http://127.0.0.1:8080/
   - Don't use localhost!
5. When you try to get the temperature for, say, Paris, you will be redirected to the auth-server on port 9000. Log in
   with `user` / `password`. Tadaa 🎉
   - there will be a nasty error in the stack trace. This is expected, it's from Spring Security's OAuth2 internals.
6. Subsequent calls will not require a login.