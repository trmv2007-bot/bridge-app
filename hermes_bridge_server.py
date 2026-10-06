#!/usr/bin/env python3
"""
Hermes Bridge Server
A local HTTP server that bridges the Bridge App and Hermes AI assistant.
The Bridge App sends messages here, and Hermes processes them.
"""

import http.server
import json
import os
import threading
import time
from datetime import datetime

# Configuration
PORT = 8080
REQUEST_FILE = "/sdcard/bridge/ai_request.json"
RESPONSE_FILE = "/sdcard/bridge/ai_response.json"
STATUS_FILE = "/sdcard/bridge/ai_status.json"
LOCK_FILE = "/sdcard/bridge/ai_lock"

# Ensure bridge directory exists
os.makedirs("/sdcard/bridge", exist_ok=True)


class BridgeHandler(http.server.BaseHTTPRequestHandler):
    """HTTP request handler for the Bridge App."""

    def log_message(self, format, *args):
        """Suppress default logging."""
        pass

    def do_GET(self):
        """Handle GET requests."""
        if self.path == "/status":
            self.send_status()
        elif self.path == "/health":
            self.send_health()
        else:
            self.send_error(404)

    def do_POST(self):
        """Handle POST requests."""
        if self.path == "/chat":
            self.handle_chat()
        elif self.path == "/command":
            self.handle_command()
        else:
            self.send_error(404)

    def send_status(self):
        """Send server status."""
        status = {
            "status": "running",
            "timestamp": datetime.now().isoformat(),
            "port": PORT,
            "request_file": REQUEST_FILE,
            "response_file": RESPONSE_FILE
        }
        self.send_json(200, status)

    def send_health(self):
        """Send health check response."""
        self.send_json(200, {"status": "ok"})

    def handle_chat(self):
        """Handle chat message from Bridge App."""
        try:
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)
            data = json.loads(body.decode("utf-8"))

            message = data.get("message", "")
            if not message:
                self.send_json(400, {"error": "No message provided"})
                return

            # Write request file
            request = {
                "message": message,
                "timestamp": datetime.now().isoformat(),
                "source": "bridge_app"
            }

            # Wait for lock to be released
            self.wait_for_lock()

            # Write request
            with open(REQUEST_FILE, "w") as f:
                json.dump(request, f)

            # Write status
            with open(STATUS_FILE, "w") as f:
                json.dump({"status": "waiting", "timestamp": datetime.now().isoformat()}, f)

            # Wait for response (up to 120 seconds)
            response = self.wait_for_response()

            if response:
                self.send_json(200, {"response": response, "status": "success"})
            else:
                self.send_json(200, {"response": "No response yet. Try again later.", "status": "timeout"})

        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def handle_command(self):
        """Handle command from Bridge App."""
        try:
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length)
            data = json.loads(body.decode("utf-8"))

            command = data.get("command", "")
            if not command:
                self.send_json(400, {"error": "No command provided"})
                return

            # Write command to bridge command file
            with open("/sdcard/bridge/cmd.txt", "w") as f:
                f.write(command)

            # Wait for result
            result = self.wait_for_bridge_result()

            if result:
                self.send_json(200, {"result": result, "status": "success"})
            else:
                self.send_json(200, {"result": "Timeout", "status": "timeout"})

        except Exception as e:
            self.send_json(500, {"error": str(e)})

    def wait_for_lock(self):
        """Wait for any existing lock to be released."""
        for _ in range(50):
            if not os.path.exists(LOCK_FILE):
                return
            time.sleep(0.1)

    def wait_for_response(self):
        """Wait for Hermes to write a response."""
        for _ in range(1200):  # 120 seconds
            if os.path.exists(RESPONSE_FILE):
                try:
                    with open(RESPONSE_FILE, "r") as f:
                        data = json.load(f)
                    os.remove(RESPONSE_FILE)
                    return data.get("response", "")
                except:
                    pass
            time.sleep(0.1)
        return None

    def wait_for_bridge_result(self):
        """Wait for bridge command result."""
        result_file = "/sdcard/bridge/result.txt"
        for _ in range(500):  # 50 seconds
            if os.path.exists(result_file):
                try:
                    with open(result_file, "r") as f:
                        result = f.read()
                    os.remove(result_file)
                    return result
                except:
                    pass
            time.sleep(0.1)
        return None

    def send_json(self, code, data):
        """Send JSON response."""
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(data).encode("utf-8"))


def run_server():
    """Run the HTTP server."""
    server = http.server.HTTPServer(("0.0.0.0", PORT), BridgeHandler)
    print(f"Hermes Bridge Server running on port {PORT}")
    print(f"Request file: {REQUEST_FILE}")
    print(f"Response file: {RESPONSE_FILE}")
    print(f"Status file: {STATUS_FILE}")
    print("Press Ctrl+C to stop")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped")
        server.server_close()


if __name__ == "__main__":
    run_server()
