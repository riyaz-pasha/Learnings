from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs
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

        parsed = urlparse(self.path)

        path = parsed.path
        params = parse_qs(parsed.query)

        print("=====Parsed Path=====")
        print("parsed: ", parsed)
        print("Path: ", path)
        print("params: ",params)
        print("=================")

        users = {
            "1": {"id": 1, "name": "Alice"},
            "2": {"id": 2, "name": "Bob"},
            "3": {"id": 3, "name": "Riyaz"},
        }


        if path.startswith("/users/"):

            user_id = path.split("/")[-1]

            user = users.get(user_id)

            if user is None:

                self.send_response(404)
                self.end_headers()
                return

            body = json.dumps(user).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()

            self.wfile.write(body)
            return

        elif path == "/users":

            body = json.dumps(users).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()

            self.wfile.write(body)
            return

        elif path=="/":
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

    def do_PUT(self):

        parsed=urlparse(self.path)
        path=parsed.path

        if not path.startswith("/users/"):
            self.send_response(404)
            self.end_headers()
            return

        user_id = path.split("/")[-1]

        content_length = int(self.headers.get("Content-Length", 0))

        body = self.rfile.read(content_length)

        try:
            data = json.loads(body.decode("utf-8"))
        except json.JSONDecodeError:

            response = b'{"error":"Invalid JSON"}'

            self.send_response(400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()

            self.wfile.write(response)
            return

        print("User ID: ", user_id)
        print("Replacement: ", data)

        response = json.dumps({"id": user_id, **data }).encode("utf-8")

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response)))
        self.end_headers()

        self.wfile.write(response)
        return


server = HTTPServer(("localhost",8080),Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
