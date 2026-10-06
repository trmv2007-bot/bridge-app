# Hermes Bridge Server

A local HTTP server that connects the Bridge App to Hermes AI assistant.

## How it works

1. The Bridge App sends messages to `http://localhost:8080/chat`
2. The server writes the message to `/sdcard/bridge/ai_request.json`
3. Hermes checks for new messages and writes responses to `/sdcard/bridge/ai_response.json`
4. The Bridge App polls for responses and displays them

## Setup

1. Start the server:
   ```bash
   hermes-bridge start
   ```

2. Check for messages:
   ```bash
   hermes-bridge check
   ```

3. Respond to messages:
   ```bash
   hermes-bridge respond "your response"
   ```

4. Stop the server:
   ```bash
   hermes-bridge stop
   ```

## Files

- `hermes_bridge_server.py` - Main server script
- `hermes-bridge` - Control script
- `hermes-bridge-start` - Start server
- `hermes-bridge-stop` - Stop server
- `hermes-bridge-check` - Check for messages
- `hermes-bridge-respond` - Respond to messages

## API Endpoints

- `GET /health` - Health check
- `GET /status` - Server status
- `POST /chat` - Send chat message
- `POST /command` - Execute bridge command

## Configuration

- Port: 8080
- Request file: `/sdcard/bridge/ai_request.json`
- Response file: `/sdcard/bridge/ai_response.json`
- Status file: `/sdcard/bridge/ai_status.json`

## Troubleshooting

- Check server status: `hermes-bridge status`
- Check server log: `cat /sdcard/bridge/server.log`
- Test health: `curl http://localhost:8080/health`
