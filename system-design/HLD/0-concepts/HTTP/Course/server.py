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

    def do_POST(self):

        print("\n")
        print("===== POST REQUEST =====")
        print("Method", self.command)
        print("Path", self.path)
        print("Headers:")
        print(self.headers)

        content_type = self.headers.get("Content-Type")
        content_length=int(self.headers.get("Content-Length", 0))

        print("Content-Type:", content_type)
        print("Content-Length:", content_length)

        body = self.rfile.read(content_length)

        print("\nRaw Body:")
        print(body.decode())

        if content_type == "application/json":
            data = json.loads(body.decode("utf-8"))

            print("Parsed JSON:", data)

            response = json.dumps({
                "received": data
            }).encode("utf-8")

            self.send_response(201)
            self.send_header("Content-Type","application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()

            self.wfile.write(response)

        else:
            self.send_response(415)
            self.end_headers()


server = HTTPServer(("localhost",8080),Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
