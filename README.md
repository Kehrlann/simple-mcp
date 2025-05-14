## Pre-requisite

You MUST use this exact build of Spring
AI: https://github.com/spring-projects/spring-ai/compare/main...Kehrlann:spring-ai:dgarnier/poc-propagate-reactive-context-to-mcp-async

## Usage

1. Run the authorization-server; it starts on port 9000
2. Run the MCP Server (mcp-simple-weather-server)
3. Run the MCP client app (mcp-sse-webflux-client)
4. Navigate to http://127.0.0.1:8080/
    - Don't use localhost!
5. When you try to get the temperature for, say, Paris, you will be redirected to the auth-server on port 9000. Log in
   with `user` / `password`. Tadaa 🎉
    - there will be a nasty error in the stack trace. This is expected, it's from Spring Security's OAuth2 internals.
6. Subsequent calls will not require a login.

Note that the MCP server is _not_ secured yet. It receives token but does not validate them.