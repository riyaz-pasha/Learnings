Absolutely. Let's make this **hands-on rather than a theory course**.

We'll use only:

* **macOS Terminal**
* **Python's standard library**
* `curl`
* `nc` (netcat)
* A tiny server we build ourselves

No Spring, Express, FastAPI, Postman, etc. initially.

The goal is to eventually make HTTP feel like something you can **see and construct yourself**, rather than something hidden behind a framework.

# Hands-on HTTP Course

### Course structure

```text
PART 1 — HTTP Fundamentals
  1. Start our first HTTP server
  2. Understand a raw HTTP request
  3. Understand a raw HTTP response
  4. HTTP methods
  5. HTTP status codes

PART 2 — Headers & Data
  6. Request headers
  7. Response headers
  8. Request body
  9. Content-Type
 10. JSON
 11. Content-Length

PART 3 — URLs
 12. Path parameters
 13. Query parameters
 14. URL encoding

PART 4 — REST-style APIs
 15. GET
 16. POST
 17. PUT
 18. PATCH
 19. DELETE

PART 5 — Authentication
 20. Authorization header
 21. Bearer tokens
 22. Cookies
 23. Sessions

PART 6 — Real HTTP Behavior
 24. Redirects
 25. Caching
 26. Conditional requests
 27. Keep-Alive
 28. Connection lifecycle

PART 7 — Going Deeper
 29. HTTP/1.1
 30. HTTP/2
 31. HTTPS
 32. TLS
 33. HTTP through a browser
 34. What frameworks actually do

PART 8 — Build Something
 35. Build a tiny HTTP API from scratch
 36. Add authentication
 37. Add error handling
 38. Add logging
 39. Inspect requests manually
 40. Final HTTP debugging exercises
```

We'll do it **one lesson at a time**. You can say `next` when you're ready.

---

# Lesson 1 — Our First HTTP Server

The first thing I want you to understand is:

> An HTTP server is fundamentally just a program that receives HTTP requests and sends HTTP responses.

We're going to create the smallest possible server.

## Step 1 — Create a directory

Open Terminal:

```bash
mkdir http-course
cd http-course
```

Create:

```bash
touch server.py
```

Open it in your editor.

Put this inside:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)

        self.send_header("Content-Type", "text/plain")
        self.end_headers()

        self.wfile.write(b"Hello HTTP!")


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
```

Run it:

```bash
python3 server.py
```

You should see:

```text
Server running on http://localhost:8080
```

Don't close this terminal.

---

# Step 2 — Make an HTTP request

Open a **second Terminal window**.

Run:

```bash
curl http://localhost:8080
```

You should get:

```text
Hello HTTP!
```

Congratulations.

You just made an HTTP request.

But `curl` hid almost everything from you.

Let's expose it.

---

# Step 3 — See the request and response

Run:

```bash
curl -v http://localhost:8080
```

You'll see something similar to:

```text
* Connected to localhost
> GET / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.x.x
> Accept: */*
>
< HTTP/1.0 200 OK
< Server: BaseHTTP/...
< Date: ...
< Content-Type: text/plain
<
Hello HTTP!
```

**Stop here and look at this carefully.**

There are two separate things happening.

---

## The request

The client sends:

```http
GET / HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.x.x
Accept: */*
```

Think:

```text
        REQUEST
           │
           ▼
┌─────────────────────────┐
│ GET / HTTP/1.1          │  ← Request line
│ Host: localhost:8080    │  ← Header
│ User-Agent: curl        │  ← Header
│ Accept: */*             │  ← Header
│                         │
│                         │
│ Request body (optional) │
└─────────────────────────┘
```

---

# Step 4 — Understand the first line

This:

```http
GET / HTTP/1.1
```

contains three pieces:

```text
GET     /       HTTP/1.1
 │      │          │
 │      │          └── HTTP version
 │      │
 │      └───────────── Resource/path
 │
 └──────────────────── Method
```

So:

```text
GET
```

means:

> I want to retrieve something.

And:

```text
/
```

means:

> I want the root resource.

And:

```text
HTTP/1.1
```

means:

> I'm speaking HTTP/1.1.

---

# Step 5 — What is a header?

Look at:

```http
Host: localhost:8080
```

A header is basically:

```text
name: value
```

For example:

```http
Host: localhost:8080
User-Agent: curl/8.x.x
Accept: */*
```

You can think of headers as **metadata describing the request**.

The actual data may come later as a body.

For example:

```http
Content-Type: application/json
```

means:

> The body I'm sending is JSON.

We'll spend a lot of time on headers later.

---

# Step 6 — The response

Your server sends something like:

```http
HTTP/1.0 200 OK
Server: BaseHTTP/...
Date: ...
Content-Type: text/plain

Hello HTTP!
```

The first line:

```http
HTTP/1.0 200 OK
```

is the **status line**.

It contains:

```text
HTTP/1.0    200    OK
   │         │      │
   │         │      └── Reason phrase
   │         │
   │         └───────── Status code
   │
   └─────────────────── HTTP version
```

The important part is:

```text
200
```

That's the status code.

It means:

> The request succeeded.

---

# Step 7 — Notice something important

You might be wondering:

> Why does my Python code say HTTP server, but the response says HTTP/1.0?

That's intentional.

We're using Python's very simple built-in server, and the implementation handles the protocol details for us.

**Don't worry about that yet.**

Our focus right now is understanding the structure.

Later we'll use `nc` and construct HTTP messages ourselves so you can see what's really happening.

---

# Step 8 — Let's modify the server

Change:

```python
self.wfile.write(b"Hello HTTP!")
```

to:

```python
self.wfile.write(b"Hello Riyaz!")
```

Restart the server:

```bash
Ctrl+C
```

then:

```bash
python3 server.py
```

And:

```bash
curl http://localhost:8080
```

You'll get:

```text
Hello Riyaz!
```

This demonstrates a very important concept:

```text
curl
 │
 │ HTTP request
 ▼
server.py
 │
 │ HTTP response
 ▼
curl
```

---

# Your first mental model

Forget frameworks for now.

Think of HTTP as:

```text
              HTTP
               │
       ┌───────┴────────┐
       │                │
    REQUEST          RESPONSE
       │                │
       ▼                ▼
┌─────────────┐   ┌─────────────┐
│ Request line│   │ Status line │
│ Headers     │   │ Headers     │
│ Body        │   │ Body        │
└─────────────┘   └─────────────┘
```

And the server's job is essentially:

```text
receive request
       ↓
understand request
       ↓
do some work
       ↓
construct response
       ↓
send response
```

---

# 🎯 Your first exercise

Don't just read the explanation.

Run these:

### Exercise 1

```bash
curl -v http://localhost:8080/
```

Find these three things in the output:

```text
1. HTTP method
2. Request path
3. Response status code
```

### Exercise 2

Change your server to return:

```text
Welcome to HTTP!
```

### Exercise 3

Change:

```python
self.send_response(200)
```

to:

```python
self.send_response(404)
```

Then:

```bash
curl -v http://localhost:8080/
```

Observe what changes.

The body will still say whatever you wrote, but the response now says:

```http
404 Not Found
```

This is an important lesson:

> **HTTP status code and response body are separate things.**

---

## One thing to remember from Lesson 1

Don't memorize everything yet.

Just remember this:

```text
HTTP Request

METHOD + PATH + VERSION
HEADERS
BLANK LINE
BODY (optional)
```

and:

```text
HTTP Response

VERSION + STATUS CODE
HEADERS
BLANK LINE
BODY (optional)
```

Next, we'll **construct an HTTP request ourselves instead of letting `curl` do it**, and you'll see why that blank line is important and what actually travels over the TCP connection.


---

Yes — **for HTTP/1.x, that order is part of the protocol syntax**. It's not something Python or `curl` invented.

But there's an important distinction: **HTTP defines the syntax**, while an organization such as IETF publishes the specification that defines it.

### 1. Who decided this?

HTTP is an open Internet standard. Its specifications are developed and standardized through the **IETF (Internet Engineering Task Force)**.

For HTTP/1.1, the modern specification is defined primarily in **RFC 9112 — HTTP/1.1**.

Historically, HTTP/1.0 was specified in RFC 1945 and HTTP/1.1 evolved through RFC 2616 and later the RFC 7230 series, before the current HTTP specifications.

So when you write:

```http
GET /users HTTP/1.1
```

you're following a standardized grammar.

---

## 2. Is `METHOD + PATH + VERSION` fixed?

For a normal HTTP/1.x request, yes.

The first line is called the **request-line**:

```text
request-line = method SP request-target SP HTTP-version CRLF
```

So conceptually:

```text
METHOD    SPACE    PATH    SPACE    VERSION
  ↓                 ↓              ↓
GET       /users    HTTP/1.1
```

More accurately:

```http
GET /users HTTP/1.1
```

There are spaces between the three components.

You **cannot arbitrarily rearrange it**:

```http
/users GET HTTP/1.1
```

or:

```http
HTTP/1.1 GET /users
```

Those don't conform to the HTTP/1.x request-line syntax.

---

# 3. Is `VERSION + STATUS CODE` also fixed?

Yes.

The response starts with a **status-line**:

```text
status-line = HTTP-version SP status-code SP [reason-phrase] CRLF
```

So:

```http
HTTP/1.1 200 OK
```

is:

```text
VERSION       STATUS CODE       REASON
   ↓              ↓               ↓
HTTP/1.1          200             OK
```

Again, the order matters.

This isn't valid HTTP/1.x syntax:

```http
200 HTTP/1.1 OK
```

because the protocol specifies the status line in the other order.

---

# 4. But WHY this particular order?

This is the more interesting question.

The designers needed a **simple, predictable grammar** that machines could parse.

Imagine receiving this:

```http
GET /users HTTP/1.1
Host: example.com
Accept: application/json

```

The parser can essentially do:

```text
Read first line
     ↓
Split into components
     ↓
METHOD       = GET
TARGET       = /users
VERSION      = HTTP/1.1
```

Then:

```text
Read headers
     ↓
Host
Accept
...
```

Then:

```text
Blank line
     ↓
Headers finished
```

Then potentially:

```text
Read body
```

It's a deliberately simple textual protocol.

---

# 5. Why put the METHOD first?

Think about what the client is trying to tell the server:

> **What operation do I want to perform?**

For example:

```http
GET /users HTTP/1.1
```

```http
POST /users HTTP/1.1
```

```http
DELETE /users/123 HTTP/1.1
```

The first thing is the **method**.

Then:

> **What resource/target is the operation about?**

```text
/users
/users/123
/orders/456
```

Then:

> **Which HTTP version are you speaking?**

```text
HTTP/1.1
```

So you can think of it as:

```text
WHAT DO I WANT?
       ↓
GET

WHAT DO I WANT IT ON?
       ↓
/users

WHICH PROTOCOL VERSION?
       ↓
HTTP/1.1
```

This is a useful mental model, although the exact historical design involved compatibility and protocol evolution considerations too.

---

# 6. Why is VERSION first in the response?

The server isn't performing an operation in the same way the client is.

The server is essentially saying:

> "Here's my HTTP response, and here's the result."

So:

```http
HTTP/1.1 200 OK
```

starts with:

```text
HTTP/1.1
```

meaning:

> This is an HTTP/1.1 response.

Then:

```text
200
```

meaning:

> Here's the result/status.

Then historically:

```text
OK
```

provided a human-readable explanation.

So conceptually:

```text
WHAT PROTOCOL?
      ↓
HTTP/1.1

WHAT HAPPENED?
      ↓
200

HUMAN DESCRIPTION?
      ↓
OK
```

---

# 7. One important correction to our previous lesson

I simplified this as:

```text
METHOD + PATH + VERSION
```

But **PATH isn't technically the exact term**.

It's:

```text
METHOD + REQUEST-TARGET + VERSION
```

For example:

```http
GET /users?id=123 HTTP/1.1
```

The request-target is:

```text
/users?id=123
```

It can take different forms depending on the kind of HTTP request.

For our learning purposes, we'll mostly encounter:

```text
origin-form

/users
/users/123
/users?id=123
```

But later we'll look at why HTTP sometimes has things like:

```http
CONNECT example.com:443 HTTP/1.1
```

and why proxies introduce another form.

---

# 8. And there's an even deeper point

The order isn't just a convention.

It's a **grammar**.

Think about programming languages.

Java says:

```java
if (condition) {
    ...
}
```

You can't randomly write:

```java
(condition) if {
    ...
}
```

because the language grammar doesn't allow it.

HTTP/1.x has grammar too.

Conceptually:

```text
HTTP Request

request-line
headers
blank line
optional body
```

And:

```text
request-line
    =
method
space
request-target
space
HTTP-version
CRLF
```

That's why clients and servers made by completely different companies can communicate.

```text
curl
   │
   │ standardized HTTP syntax
   ▼
Apache
   │
   │
Nginx
   │
   │
Spring
   │
   │
Python
```

They don't need to agree on implementation.

They only need to agree on the **protocol**.

---

## And this is exactly what we'll explore next

Instead of using:

```bash
curl http://localhost:8080
```

we'll eventually use:

```bash
nc localhost 8080
```

and manually type:

```http
GET / HTTP/1.1
Host: localhost:8080

```

At that point you'll be able to see:

**"Oh! HTTP isn't a magical API call. I'm literally sending these bytes/text to another machine according to a standardized grammar."**

That's the point where HTTP starts to really click.


---

