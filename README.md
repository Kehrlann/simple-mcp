# Usage

1. Run the authorization-server; it starts on port 9000
2. Run the MCP Server
3. Run `mcp-sse-http-client`
4. Navigate to http://127.0.0.1:8081/ 
   - Don't use localhost!
5. When you try to call the LLM, you will be redirected to the auth-server on port 9000. Log in with `user` / `password`. Tadaa!

Note that the MCP server is _not_ secured yet. It receives token but does not validate them.