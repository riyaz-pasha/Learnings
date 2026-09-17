from http.server import BaseHTTPRequestHandler, HTTPServer

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
            message="Home Page"

        elif self.path=="/users":
            message="List of users"

        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")
            return


        self.send_response(200)

        self.send_header("Content-Type", "text/plain")
        self.end_headers()

        self.wfile.write(message.encode())


server = HTTPServer(("localhost",8080),Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
