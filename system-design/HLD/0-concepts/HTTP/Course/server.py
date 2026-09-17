from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class Handler(BaseHTTPRequestHandler):

    def do_GET(self):

        print("\n")
        print("=====REQUEST=====")
        print("Method:", self.command)
        print("Path", self.path)
        print("Headers:")
        print(self.headers)
        print("=================")

        if self.path=="/":
            response = {
                "message": "Hello HTTP",
                "course": "HTTP fundamentals"
            }

            body = json.dumps(response).encode()

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()

            self.wfile.write(body)
            return

        elif self.path=="/users":
            body="List of users"

        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")
            return


        self.send_response(200)

        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        # self.send_header("Content-Length", str(6))
        # self.send_header("Content-Length", str(100))
        self.end_headers()

        self.wfile.write(body.encode())


server = HTTPServer(("localhost",8080),Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
