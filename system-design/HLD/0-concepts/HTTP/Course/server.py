from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)

        self.send_header("Content-Type", "text/plain")
        self.end_headers()

        self.wfile.write(b"Hello HTTP!")


server = HTTPServer(("localhost",8080),Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
