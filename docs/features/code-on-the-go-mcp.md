# Local MCP connection

The editor toolbar includes a **Start MCP** action. It opens the integrated terminal and runs:

```sh
tunnel-client run --profile code-on-the-go
```

The action uses the open project as the terminal working directory. Repeated taps select the
existing `Code On The Go MCP` terminal session instead of starting duplicate tunnel clients. Once
the command exits, a later tap starts a fresh session.

## Prerequisites

1. Install an Android-compatible `tunnel-client` binary in the integrated Termux `PATH`.
2. Create the `code-on-the-go` tunnel profile with `tunnel-client init`.
3. Configure that profile with the OpenAI tunnel id and either the local MCP server command or its
   Streamable HTTP URL.
4. Keep credentials in the tunnel-client configuration or environment. The app does not embed or
   pass credentials in the toolbar action.

If `tunnel-client` is unavailable, the terminal explains what is missing and remains open so the
user can finish setup. The toolbar action starts an already-configured MCP connection; it does not
install `tunnel-client` or ship a Code On The Go MCP server.

## ChatGPT setup

Enable developer mode in ChatGPT, add a plugin connection, choose **Tunnel**, and select the tunnel
associated with the target ChatGPT workspace. The tunnel client needs outbound HTTPS access, but
the local MCP server does not need public inbound access.

The terminal activity and Termux command service remain non-exported. Only Code On The Go can send
the internal command intent that starts the tunnel session.
