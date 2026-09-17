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

# Lesson 2 — Build an HTTP Request Yourself

Now we're going to remove `curl` from the picture.

Until now:

```text
curl
  ↓
creates HTTP request
  ↓
server
```

You haven't actually *constructed* the HTTP request yourself.

Let's do that.

---

## 1. Keep your server running

Your `server.py` should still be running:

```bash
python3 server.py
```

If you stopped it:

```bash
cd http-course
python3 server.py
```

---

## 2. Use `nc`

macOS comes with `nc` (netcat).

Open another Terminal and run:

```bash
nc localhost 8080
```

You won't see much.

That's expected.

`nc` has simply opened a TCP connection to:

```text
localhost:8080
```

Now **you are talking directly to the server**.

---

# 3. Type the HTTP request manually

Type exactly this:

```http
GET / HTTP/1.1
Host: localhost:8080

```

There is an important detail here:

### Press Enter after `Host: localhost:8080`

Then press **Enter one more time**.

So there are actually **two newlines after the Host header**.

You should get something like:

```text
Hello HTTP!
```

You just sent an HTTP request **without curl**.

That's a big milestone.

---

# 4. What did you actually send?

You sent:

```http
GET / HTTP/1.1
Host: localhost:8080

```

Let's break it down.

```text
GET / HTTP/1.1
```

This is the **request line**.

Then:

```text
Host: localhost:8080
```

This is a **header**.

Then:

```text
<empty line>
```

This tells the server:

> "The headers are finished."

So the structure is:

```text
┌─────────────────────────────┐
│ GET / HTTP/1.1              │
│                             │
│ Host: localhost:8080        │
│                             │
│                             │ ← empty line
│                             │
│ [optional request body]     │
└─────────────────────────────┘
```

---

# 5. Why do we need the empty line?

This is one of the most important concepts in HTTP/1.x.

Suppose you have:

```http
GET / HTTP/1.1
Host: localhost:8080
Accept: text/plain
User-Agent: my-client
```

How does the server know whether:

```text
User-Agent: my-client
```

is the last header?

It doesn't know that another header isn't coming:

```http
Authorization: ...
```

or:

```http
Cookie: ...
```

So HTTP defines a delimiter.

The blank line means:

```text
HEADERS ARE DONE.
```

Therefore:

```http
GET / HTTP/1.1
Host: localhost:8080
Accept: text/plain

Hello
```

can be understood as:

```text
Request line
      ↓
GET / HTTP/1.1

Headers
      ↓
Host: localhost:8080
Accept: text/plain

      ↓
EMPTY LINE

Body
      ↓
Hello
```

---

# 6. Try adding another header

Run:

```bash
nc localhost 8080
```

Then:

```http
GET / HTTP/1.1
Host: localhost:8080
User-Agent: MyBrowser
Accept: text/plain

```

Your server should still respond.

Notice that **you decided what the headers were**.

`curl` didn't.

---

# 7. Let's inspect the request on the server

Modify your Python server:

```python
def do_GET(self):

    print("\n--- REQUEST ---")
    print("Method:", self.command)
    print("Path:", self.path)
    print("Headers:")
    print(self.headers)

    self.send_response(200)
    self.send_header("Content-Type", "text/plain")
    self.end_headers()

    self.wfile.write(b"Hello HTTP!")
```

Restart:

```bash
Ctrl+C
python3 server.py
```

Now connect:

```bash
nc localhost 8080
```

Send:

```http
GET / HTTP/1.1
Host: localhost:8080
User-Agent: MyBrowser
Accept: text/plain

```

Look at your **server terminal**.

You should see something like:

```text
--- REQUEST ---
Method: GET
Path: /
Headers:
Host: localhost:8080
User-Agent: MyBrowser
Accept: text/plain
```

This is a great experiment because you can now see both sides:

```text
                 TCP connection
                      │
                      ▼
┌────────────────────────────────────┐
│              CLIENT                │
│                                    │
│ GET / HTTP/1.1                     │
│ Host: localhost:8080               │
│ User-Agent: MyBrowser              │
│ Accept: text/plain                 │
│                                    │
└────────────────┬───────────────────┘
                 │
                 │ HTTP request
                 ▼
┌────────────────────────────────────┐
│              SERVER                │
│                                    │
│ self.command → GET                 │
│ self.path    → /                   │
│ self.headers → headers             │
│                                    │
└────────────────────────────────────┘
```

---

# 8. Now let's break the request

Try this:

```http
GET /users HTTP/1.1
Host: localhost:8080

```

What changed?

Only:

```text
/
```

became:

```text
/users
```

So the server receives:

```text
Method = GET
Path   = /users
```

The HTTP protocol doesn't care what `/users` means.

That's the **application's responsibility**.

HTTP essentially says:

> "Here's a request for this target."

Your application decides:

> "Ah, `/users` means return users."

---

# 9. Let's make the server respond differently

Change your server:

```python
def do_GET(self):

    if self.path == "/":
        message = "Home page"

    elif self.path == "/users":
        message = "List of users"

    else:
        self.send_response(404)
        self.end_headers()
        self.wfile.write(b"Not Found")
        return

    self.send_response(200)
    self.send_header("Content-Type", "text/plain")
    self.end_headers()

    self.wfile.write(message.encode())
```

Now:

```bash
nc localhost 8080
```

Send:

```http
GET /users HTTP/1.1
Host: localhost:8080

```

Response:

```text
List of users
```

And:

```http
GET /something HTTP/1.1
Host: localhost:8080

```

Response:

```text
Not Found
```

---

# 10. Here's an important separation

At this point, distinguish **HTTP** from your **application**.

HTTP defines things like:

```text
GET
POST
HTTP/1.1
headers
status codes
request body
response body
```

Your application defines:

```text
/users
/orders
/products
/login
/create-user
```

For example:

```http
GET /users HTTP/1.1
```

HTTP understands:

```text
GET
/users
```

But HTTP doesn't know what a "user" is.

That's your application's meaning.

This distinction becomes extremely important when you start learning REST APIs and frameworks.

---

# 11. One more experiment

Try removing the `Host` header.

```bash
nc localhost 8080
```

Send:

```http
GET / HTTP/1.1

```

Depending on the Python server implementation, you may get a `400 Bad Request`.

Why?

Because for HTTP/1.1, the `Host` header is required in normal requests.

We'll explore **why `Host` exists** in a later lesson.

---

# 12. Your mental model after Lesson 2

You should now think of an HTTP request as:

```text
┌───────────────────────────────┐
│ Request Line                   │
│ GET /users HTTP/1.1            │
├───────────────────────────────┤
│ Headers                        │
│ Host: localhost:8080           │
│ Accept: application/json       │
│ User-Agent: MyClient           │
├───────────────────────────────┤
│ EMPTY LINE                     │
├───────────────────────────────┤
│ Body                           │
│ ...                            │
└───────────────────────────────┘
```

And the response:

```text
┌───────────────────────────────┐
│ Status Line                    │
│ HTTP/1.1 200 OK                │
├───────────────────────────────┤
│ Headers                        │
│ Content-Type: text/plain       │
│ Content-Length: 12             │
├───────────────────────────────┤
│ EMPTY LINE                     │
├───────────────────────────────┤
│ Body                           │
│ Hello HTTP!                    │
└───────────────────────────────┘
```

---

# 🎯 Exercises

Before moving on, try these manually with `nc`.

### Exercise 1

Send:

```http
GET / HTTP/1.1
Host: localhost:8080
```

Add the blank line.

Observe the response.

### Exercise 2

Change the path:

```http
GET /users HTTP/1.1
Host: localhost:8080

```

### Exercise 3

Add two custom headers:

```http
GET / HTTP/1.1
Host: localhost:8080
X-Name: Riyaz
X-Course: HTTP

```

Look at what appears in your server terminal.

### Exercise 4 — deliberately break it

Try:

```http
/users GET HTTP/1.1
Host: localhost:8080

```

Observe what happens.

Then try:

```http
GET / HTTP/1.1
```

without `Host`.

These experiments are more valuable than memorizing the HTTP grammar.

---

### The key idea from this lesson

You have now crossed an important line:

**`curl` → convenient HTTP client**

versus

**`nc` → raw TCP connection where you manually type the HTTP message**

Next we'll look at the **response in the same way**, including why `Content-Length` matters and how the client knows **where the response body ends**.


---

# Lesson 3 — HTTP Response: How Does the Client Know Where the Body Ends?

This lesson is important because we're going to answer a deceptively simple question:

> **When the server sends a response, how does the client know where the response ends?**

This leads us directly into **headers, `Content-Length`, and the HTTP message structure**.

---

## 1. Let's first look at a response

Run:

```bash
curl -v http://localhost:8080/
```

You'll see something like:

```http
< HTTP/1.0 200 OK
< Server: BaseHTTP/...
< Date: ...
< Content-Type: text/plain
<
Hello HTTP!
```

There are three major parts:

```text
HTTP/1.0 200 OK          ← Status line

Content-Type: text/plain ← Headers

                          ← Empty line

Hello HTTP!              ← Body
```

The response structure is:

```text
┌──────────────────────────────┐
│ Status Line                  │
├──────────────────────────────┤
│ Headers                      │
├──────────────────────────────┤
│ Empty Line                   │
├──────────────────────────────┤
│ Body                         │
└──────────────────────────────┘
```

---

# 2. Let's add Content-Length

Change your server to:

```python
def do_GET(self):

    body = b"Hello HTTP!"

    self.send_response(200)

    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", str(len(body)))

    self.end_headers()

    self.wfile.write(body)
```

Restart the server.

Now:

```bash
curl -v http://localhost:8080/
```

You should see:

```http
< HTTP/1.0 200 OK
< Content-Type: text/plain
< Content-Length: 11
<
Hello HTTP!
```

Let's count:

```text
Hello HTTP!
```

has 11 bytes.

```text
H e l l o   H T T P !
1 2 3 4 5 6 7 8 9 10 11
```

So:

```http
Content-Length: 11
```

means:

> The message body contains exactly 11 bytes.

---

# 3. Why does this matter?

Imagine the server sends:

```text
Hello HTTP!
```

How does the client know whether this is:

```text
Hello HTTP!
```

or:

```text
Hello HTTP!More data...
```

or:

```text
Hello HTTP!
Another response?
```

The client needs some way to determine:

> **Where does the body end?**

`Content-Length` is one way.

```http
Content-Length: 11
```

The client can now say:

```text
I need to read exactly 11 bytes.
```

---

# 4. A very important distinction

`Content-Length` does **not** mean:

> "The entire HTTP response is 11 bytes."

It means:

> "The message body is 11 bytes."

So:

```http
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 11

Hello HTTP!
```

The entire network message is larger than 11 bytes because the status line and headers are also present.

---

# 5. Let's make the body longer

Change:

```python
body = b"Hello HTTP!"
```

to:

```python
body = b"Hello! Welcome to the HTTP course."
```

Then:

```python
self.send_header("Content-Length", str(len(body)))
```

automatically calculates the correct size.

Run:

```bash
curl -v http://localhost:8080/
```

You'll see something like:

```http
< Content-Length: 35
<
Hello! Welcome to the HTTP course.
```

The important thing is:

```text
body length
     ↓
Content-Length
```

---

# 6. What if Content-Length is wrong?

This is a great experiment.

Change:

```python
self.send_header("Content-Length", str(len(body)))
```

to:

```python
self.send_header("Content-Length", "5")
```

But continue sending:

```python
body = b"Hello HTTP!"
```

Now you've told the client:

```text
Body length = 5
```

but you're actually sending:

```text
11 bytes
```

Run:

```bash
curl -v http://localhost:8080/
```

You'll see behavior indicating that only the declared amount belongs to the response body, with the exact behavior depending on the client/server handling.

The important lesson is:

> **HTTP metadata must agree with the actual message.**

---

# 7. What if Content-Length is too large?

Try:

```python
self.send_header("Content-Length", "100")
```

while only sending:

```text
Hello HTTP!
```

Now the client has a problem.

The server effectively says:

```text
"I'm sending you 100 bytes."
```

but only sends 11.

The client may wait for more data.

This is one reason message framing is so important in HTTP.

---

# 8. But Content-Length isn't the only way

This is where HTTP becomes more interesting.

HTTP/1.x has multiple mechanisms for determining message boundaries.

For example:

### Content-Length

```http
Content-Length: 100
```

means:

```text
Read exactly 100 body bytes.
```

Another mechanism is:

### Transfer-Encoding: chunked

For example:

```http
HTTP/1.1 200 OK
Transfer-Encoding: chunked

5
Hello
6
 World
0

```

The body is transferred in chunks.

We'll study this later.

For now, remember:

```text
HTTP needs a way to determine
where the message body ends.
```

---

# 9. Let's look at the response using `nc`

This time we want to see what the server actually sends.

Run:

```bash
nc localhost 8080
```

Send:

```http
GET / HTTP/1.1
Host: localhost:8080

```

You'll get something similar to:

```http
HTTP/1.0 200 OK
Server: BaseHTTP/...
Date: ...
Content-Type: text/plain
Content-Length: 11

Hello HTTP!
```

Now notice something beautiful.

The response is itself just structured data:

```text
STATUS LINE
     ↓
HTTP/1.0 200 OK

HEADERS
     ↓
Content-Type: text/plain
Content-Length: 11

EMPTY LINE
     ↓

BODY
     ↓
Hello HTTP!
```

---

# 10. Request and response are symmetrical

This is a useful mental model.

### Request

```http
GET /users HTTP/1.1
Host: localhost:8080
Accept: application/json

```

### Response

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: ...

{"id":1,"name":"Alice"}
```

Notice the pattern:

```text
REQUEST                      RESPONSE

Request line                 Status line
     ↓                            ↓
GET /users HTTP/1.1          HTTP/1.1 200 OK

Headers                      Headers
     ↓                            ↓
Host: ...                    Content-Type: ...

Empty line                   Empty line
     ↓                            ↓

Body                         Body
     ↓                            ↓
(optional)                   (optional)
```

---

# 11. One subtle but important thing

You might now think:

> "Every response has a body."

No.

Some HTTP responses don't have one.

For example:

```http
HTTP/1.1 204 No Content
```

means the response has no content body.

Similarly, certain responses and requests have special rules around whether a body is allowed or expected.

We'll learn these rules later.

---

# 12. Let's make our server return JSON

Change the server:

```python
import json
```

Then:

```python
def do_GET(self):

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
```

Now:

```bash
curl -v http://localhost:8080/
```

Response:

```http
HTTP/1.0 200 OK
Content-Type: application/json
Content-Length: ...

{"message": "Hello HTTP", "course": "HTTP fundamentals"}
```

Notice:

```http
Content-Type: application/json
```

This header tells the client:

> "Interpret the body as JSON."

This leads to our next major topic.

---

# 13. `Content-Type` vs `Content-Length`

These two headers answer completely different questions.

### Content-Type

```http
Content-Type: application/json
```

answers:

> **What kind of data is this?**

Examples:

```http
Content-Type: application/json
Content-Type: text/plain
Content-Type: text/html
Content-Type: image/jpeg
```

### Content-Length

```http
Content-Length: 42
```

answers:

> **How many bytes are in the body?**

So:

```text
Content-Type
      ↓
WHAT is the body?

Content-Length
      ↓
HOW MUCH body is there?
```

Don't mix these two concepts.

---

# 14. Your exercises

### Exercise 1

Make your server return:

```text
HTTP is a protocol.
```

and correctly calculate:

```http
Content-Length
```

Don't manually calculate it; let Python do it.

---

### Exercise 2

Return this JSON:

```json
{
  "name": "Riyaz",
  "topic": "HTTP"
}
```

with:

```http
Content-Type: application/json
```

---

### Exercise 3

Use:

```bash
curl -i http://localhost:8080/
```

Notice the difference between:

```bash
curl
```

and:

```bash
curl -i
```

`-i` tells curl to include the response headers in its output.

---

### Exercise 4

Use:

```bash
curl -v http://localhost:8080/
```

and identify:

```text
1. Status line
2. Content-Type
3. Content-Length
4. Empty line
5. Body
```

---

# The big idea from Lesson 3

We're slowly building the complete picture:

```text
                 HTTP MESSAGE
                      │
          ┌───────────┴───────────┐
          │                       │
       REQUEST                 RESPONSE
          │                       │
    Request Line              Status Line
          │                       │
       Headers                  Headers
          │                       │
      Empty Line              Empty Line
          │                       │
        Body                    Body
```

And now you understand something deeper:

> **HTTP isn't only about endpoints and methods. It also defines how the bytes of a message are structured and how the receiver determines where different parts of the message begin and end.**

Next we'll focus entirely on **headers**. We'll create our own headers, inspect them on the server, understand why `Host`, `Accept`, `Content-Type`, `User-Agent`, `Authorization`, etc. exist, and—most importantly—learn the difference between **request headers and response headers**.

---

# Lesson 4 — HTTP Headers

Now let's focus entirely on **headers**.

Headers are one of the most important parts of HTTP because they allow the client and server to communicate information **about the request or response**, without putting that information into the main body.

---

# 1. Start with the simplest request

Run:

```bash
curl -v http://localhost:8080/
```

You'll see something similar to:

```http
> GET / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.x.x
> Accept: */*
>
```

There are three headers here:

```text
Host
User-Agent
Accept
```

These are not arbitrary Python things.

They are HTTP header fields defined/used by the HTTP specifications and the wider HTTP ecosystem.

---

# 2. What exactly is a header?

At its simplest:

```text
Header-Name: Header-Value
```

For example:

```http
Host: localhost:8080
```

Think:

```text
┌──────────────┬──────────────────┐
│ Header Name  │ Header Value     │
├──────────────┼──────────────────┤
│ Host         │ localhost:8080   │
└──────────────┴──────────────────┘
```

Another:

```http
Content-Type: application/json
```

means:

```text
name  = Content-Type
value = application/json
```

---

# 3. Request headers vs response headers

This distinction is extremely important.

## Request headers

Sent:

```text
CLIENT → SERVER
```

Example:

```http
GET /users HTTP/1.1
Host: example.com
Accept: application/json
Authorization: Bearer abc123
```

The client is telling the server things about the request.

---

## Response headers

Sent:

```text
SERVER → CLIENT
```

Example:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42
Cache-Control: max-age=60
```

The server is telling the client things about the response.

So:

```text
               REQUEST
CLIENT ─────────────────────> SERVER
       Host
       Accept
       Authorization
       ...


               RESPONSE
CLIENT <───────────────────── SERVER
       Content-Type
       Content-Length
       Cache-Control
       ...
```

---

# 4. Let's inspect the headers ourselves

Modify your server:

```python
def do_GET(self):

    print("\n--- REQUEST ---")

    print("Method:", self.command)
    print("Path:", self.path)

    print("\nHeaders:")

    for name, value in self.headers.items():
        print(f"{name} = {value}")

    body = b"Hello HTTP!"

    self.send_response(200)
    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", str(len(body)))
    self.end_headers()

    self.wfile.write(body)
```

Restart your server.

Then:

```bash
curl http://localhost:8080/
```

Look at the **server terminal**.

You'll see something similar to:

```text
--- REQUEST ---
Method: GET
Path: /

Headers:
Host = localhost:8080
User-Agent = curl/8.x.x
Accept = */*
```

This is useful because now you can see:

```text
curl
 │
 │ constructs headers
 ▼
HTTP request
 │
 ▼
Python server
 │
 │ parses headers
 ▼
self.headers
```

---

# 5. Let's create our own header

Use curl:

```bash
curl \
  -H "X-Student: Riyaz" \
  http://localhost:8080/
```

On the server:

```text
Headers:

Host = localhost:8080
User-Agent = curl/...
Accept = */*
X-Student = Riyaz
```

You just created a custom HTTP header.

The syntax is simply:

```text
-H "Name: Value"
```

---

# 6. Why would custom headers exist?

Suppose you have an application that needs to tell the server:

```text
Which mobile app version am I using?
```

You could send:

```http
X-App-Version: 5.2.1
```

Or perhaps:

```http
X-Request-ID: abc-123
```

for request tracing.

Historically, headers beginning with `X-` were commonly used for unofficial/custom fields.

Today, the HTTP ecosystem generally doesn't recommend `X-` as a special convention for new fields, but you'll still encounter many legacy `X-*` headers.

For learning, the important point is:

> HTTP provides a mechanism for sending metadata as header fields.

---

# 7. The `Host` header

Let's examine:

```http
Host: localhost:8080
```

Why does the server need this?

Imagine a single server machine has multiple websites:

```text
example.com
google.com
mycompany.com
```

They could potentially all resolve to the same IP address.

For example:

```text
example.com   ─┐
google.com    ─┼──> 203.0.113.10
mycompany.com ─┘
```

The server receives a connection to:

```text
203.0.113.10:80
```

But which website did the client actually request?

The `Host` header helps answer that:

```http
Host: example.com
```

versus:

```http
Host: google.com
```

This mechanism is especially important for **virtual hosting**.

---

# 8. Let's test it

Run:

```bash
curl \
  -H "Host: example.com" \
  http://localhost:8080/
```

Your TCP connection is still going to:

```text
localhost:8080
```

but the HTTP request says:

```http
Host: example.com
```

So:

```text
TCP destination
      ↓
localhost:8080

HTTP Host
      ↓
example.com
```

These are different concepts.

That's a **very important distinction**.

---

# 9. `User-Agent`

You've probably seen:

```http
User-Agent: curl/8.x.x
```

The client uses this to identify itself.

A browser might send something much longer.

For example, browsers commonly send information identifying the browser and platform.

Why?

Servers may use it for things such as:

* analytics
* compatibility behavior
* debugging
* logging
* content selection

But don't think of `User-Agent` as trustworthy identity.

A client can simply send:

```bash
curl -H "User-Agent: MySuperBrowser"
```

So the server should generally treat it as **client-provided metadata**, not authentication.

---

# 10. `Accept`

Consider:

```http
Accept: application/json
```

This means roughly:

> "I can accept a response represented as JSON."

Compare:

```http
Accept: text/html
```

versus:

```http
Accept: application/json
```

This is about the **response representation the client wants/accepts**.

---

# 11. `Accept` vs `Content-Type`

This is one of the most common HTTP confusions.

Compare:

```http
Accept: application/json
```

and:

```http
Content-Type: application/json
```

They mean different things.

### Accept

```text
What representation do I want/accept?
```

### Content-Type

```text
What representation is this body?
```

For example:

```http
POST /users HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json

{"name":"Riyaz"}
```

Here:

```text
Accept
   ↓
"I want the response as JSON."

Content-Type
   ↓
"The request body I'm sending is JSON."
```

This distinction will become extremely important when we build a POST API.

---

# 12. Response headers

So far we've mostly looked at request headers.

Let's examine response headers.

Our server sends:

```python
self.send_response(200)

self.send_header(
    "Content-Type",
    "text/plain"
)

self.send_header(
    "Content-Length",
    str(len(body))
)

self.end_headers()
```

Those become:

```http
HTTP/1.0 200 OK
Content-Type: text/plain
Content-Length: 11
```

These are **response headers**.

The server is providing metadata about the response.

---

# 13. Headers aren't only about the body

This is an important mental shift.

Headers can describe many different things:

```text
Request information
        ↓
Host
User-Agent
Accept
Authorization

Body information
        ↓
Content-Type
Content-Length
Content-Encoding

Caching
        ↓
Cache-Control
ETag
If-None-Match

Security
        ↓
Authorization
Cookie
...

Redirection
        ↓
Location
```

We'll encounter these gradually.

---

# 14. One HTTP request can have many headers

For example:

```http
POST /users HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.7.1
Accept: application/json
Content-Type: application/json
Authorization: Bearer abc123
Content-Length: 16

{"name":"Riyaz"}
```

Don't think of this as:

```text
GET has 3 headers
POST has 5 headers
```

There isn't a fixed number.

The request can contain multiple header fields depending on what the client and server need.

---

# 15. Headers are not the same as arguments

This distinction will save you a lot of confusion later.

Consider:

```http
GET /users?page=2 HTTP/1.1
Host: example.com
Authorization: Bearer abc
```

There are three different concepts:

### Path

```text
/users
```

### Query parameter

```text
page=2
```

### Header

```text
Authorization: Bearer abc
```

They all travel as part of the HTTP request, but they serve different purposes.

Conceptually:

```text
URL
 │
 ├── Path
 │     /users
 │
 └── Query
       page=2

Headers
 │
 ├── Host
 ├── Authorization
 └── Accept
```

We'll spend an entire lesson on URLs later.

---

# 16. Try this experiment

Run:

```bash
curl \
  -H "Accept: application/json" \
  -H "X-Student: Riyaz" \
  -H "X-Course: HTTP" \
  http://localhost:8080/users
```

Look at the server output.

You should now be able to identify:

```text
Host
User-Agent
Accept
X-Student
X-Course
```

without needing a framework to explain them.

---

# 17. A useful rule

Whenever you encounter an HTTP header, ask:

> **Who is sending it, and what information is it describing?**

For example:

```text
                    Who sends it?
                         │
          ┌──────────────┴──────────────┐
          │                             │
       Client                         Server
          │                             │
          ▼                             ▼
       Accept                     Content-Type
       User-Agent                 Content-Length
       Authorization              Location
       Cookie                     Cache-Control
```

It's not an absolute division—some headers can appear in different contexts—but this is a useful learning model.

---

# 🎯 Exercises

### Exercise 1 — Custom headers

Send:

```http
GET / HTTP/1.1
Host: localhost:8080
X-Student: Riyaz
X-Lesson: Headers
```

using `nc`.

Look at your server.

---

### Exercise 2 — Change User-Agent

```bash
curl \
  -H "User-Agent: MyHTTPClient/1.0" \
  http://localhost:8080/
```

Verify it on the server.

---

### Exercise 3 — Accept

Try:

```bash
curl \
  -H "Accept: application/json" \
  http://localhost:8080/
```

Then:

```bash
curl \
  -H "Accept: text/html" \
  http://localhost:8080/
```

Notice that **the client expressing a preference doesn't automatically make the server change its response**.

The server has to implement behavior based on that header.

---

### Exercise 4 — Host

Try:

```bash
curl \
  -H "Host: example.com" \
  http://localhost:8080/
```

Verify what your Python server receives.

---

# One important thing before the next lesson

We've now encountered:

```text
Host
User-Agent
Accept
Content-Type
Content-Length
Authorization
Cookie
```

**Don't try to memorize them.**

Instead, understand the general mechanism:

```text
Header = metadata attached to an HTTP message
```

and:

```text
Header-Name: Header-Value
```

The next lesson is where this becomes much more practical:

# Lesson 5 — HTTP Methods

We'll take:

```text
GET
POST
PUT
PATCH
DELETE
```

and instead of memorizing definitions, we'll **build endpoints and make actual requests**.

You'll see why:

```http
GET /users
```

is fundamentally different from:

```http
POST /users
```

and we'll introduce the concept of **request body** for the first time.

---

# Lesson 5 — HTTP Methods

Now we're going to understand one of the most important pieces of an HTTP request:

```http
GET /users HTTP/1.1
```

Specifically:

```text
GET
```

What does it actually mean?

And why do we have:

```text
GET
POST
PUT
PATCH
DELETE
```

instead of simply having URLs like:

```text
/users/get
/users/create
/users/update
/users/delete
```

---

# 1. The basic idea

Remember our request line:

```http
GET /users HTTP/1.1
```

The first part is the **HTTP method**.

It tells the server what kind of operation the client is requesting.

A simplified mental model:

```text
GET       → retrieve
POST      → submit/create
PUT       → replace
PATCH     → partially modify
DELETE    → delete
```

But don't memorize that table yet.

Let's actually build an API.

---

# 2. Our tiny User API

Let's say our server manages users.

We want these operations:

```text
GET    /users
GET    /users/123

POST   /users

PUT    /users/123

PATCH  /users/123

DELETE /users/123
```

Notice something interesting:

The **URL alone isn't enough** to determine the operation.

For example:

```http
/users/123
```

could mean:

```text
GET    → retrieve user
PUT    → replace user
PATCH  → modify user
DELETE → delete user
```

The method gives the request its intended semantics.

---

# 3. GET

Let's start with:

```http
GET /users HTTP/1.1
Host: localhost:8080

```

This essentially says:

> Give me the representation of `/users`.

Our server might respond:

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
    {"id": 1, "name": "Alice"},
    {"id": 2, "name": "Bob"}
]
```

Notice something:

### GET can have a response body.

Absolutely.

The misconception that "GET has no body" is common.

What's more important is that **GET requests are not normally used to send the main data of the operation in a request body**. HTTP semantics allow a content on a GET in the modern specification, but its meaning is not generally defined and many implementations don't support it as an ordinary API mechanism.

For our course, use:

```text
GET → retrieve
```

and put filtering/input in the URL/query parameters.

---

# 4. Try GET manually

```bash
nc localhost 8080
```

Then:

```http
GET / HTTP/1.1
Host: localhost:8080

```

You already did this.

Now use curl:

```bash
curl -v http://localhost:8080/users
```

The important part is:

```http
GET /users HTTP/1.1
```

---

# 5. POST

Now imagine we want to create a user.

We need to send information:

```json
{
  "name": "Riyaz"
}
```

We can use:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

Now notice what changed.

We have a **request body**:

```text
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json

              ↓ empty line

{"name":"Riyaz"}
              ↑
           body
```

---

# 6. Let's actually send it

Using curl:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

The `-d` means:

> Send this data as the request body.

Conceptually curl constructs:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

---

# 7. Let's see the body on the server

Add this to your server:

```python
def do_POST(self):

    print("\n--- POST REQUEST ---")

    print("Method:", self.command)
    print("Path:", self.path)

    print("\nHeaders:")
    print(self.headers)

    content_length = int(
        self.headers.get("Content-Length", 0)
    )

    body = self.rfile.read(content_length)

    print("\nBody:")
    print(body.decode())

    response = b"User received"

    self.send_response(201)
    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", str(len(response)))
    self.end_headers()

    self.wfile.write(response)
```

Restart the server.

Then:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

Look at your server terminal.

You should see:

```text
--- POST REQUEST ---

Method: POST
Path: /users

Headers:
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

Body:
{"name":"Riyaz"}
```

This is the first time we're dealing with the complete request:

```text
┌──────────────────────────────────┐
│ POST /users HTTP/1.1             │
├──────────────────────────────────┤
│ Host: localhost:8080             │
│ Content-Type: application/json   │
│ Content-Length: 16               │
├──────────────────────────────────┤
│                                  │
├──────────────────────────────────┤
│ {"name":"Riyaz"}                 │
└──────────────────────────────────┘
```

---

# 8. Why POST instead of GET?

Compare:

```http
GET /users
```

with:

```http
POST /users
```

The URL is identical.

The **method changes the semantics**.

Think:

```text
GET /users
    ↓
"I want to retrieve users."

POST /users
    ↓
"I want to submit data to /users."
```

The server decides what that means according to the API's design.

For a typical REST API:

```text
POST /users
    ↓
Create a new user
```

---

# 9. PUT

Suppose user `123` already exists:

```text
/users/123
```

We want to replace the user's representation.

We might send:

```http
PUT /users/123 HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

The important word is:

> **replace**

Conceptually:

```text
Existing:

{
    "id": 123,
    "name": "Alice",
    "email": "alice@example.com"
}

        ↓ PUT

New:

{
    "id": 123,
    "name": "Riyaz",
    "email": "riyaz@example.com"
}
```

PUT traditionally represents replacing the target resource's representation.

---

# 10. PATCH

Now suppose we only want to change the name.

We could use:

```http
PATCH /users/123 HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "name": "Riyaz"
}
```

Conceptually:

```text
PATCH
  ↓
"Apply this modification."
```

So:

```text
PUT

"Here is the new representation."

PATCH

"Here is a partial modification."
```

There are more nuances to PATCH semantics, which we'll cover later.

---

# 11. DELETE

To delete user 123:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080

```

Notice:

**No request body is necessary.**

The server can understand:

```text
Method = DELETE
Target = /users/123
```

and perform the operation.

A successful response might be:

```http
HTTP/1.1 204 No Content
```

We'll discuss `204` later.

---

# 12. The same URL, different meaning

This is probably the most important idea in this lesson.

Consider:

```text
/users/123
```

Now:

```http
GET /users/123
```

means:

```text
Retrieve user 123
```

While:

```http
PUT /users/123
```

means:

```text
Replace user 123
```

While:

```http
PATCH /users/123
```

means:

```text
Modify user 123
```

While:

```http
DELETE /users/123
```

means:

```text
Delete user 123
```

So the HTTP request target isn't the complete operation.

It's:

```text
        METHOD + TARGET
             ↓
         semantics
```

---

# 13. Why not just use URLs like this?

You might ask:

Why don't we do:

```text
POST /users/create
POST /users/update
POST /users/delete
```

You *can* design APIs that way.

But HTTP already provides standardized method semantics.

Using them gives clients, servers, proxies, caches, browsers, and other infrastructure useful information about what the request means.

Compare:

```text
POST /users/delete/123
```

with:

```text
DELETE /users/123
```

The second explicitly communicates:

```text
HTTP method = DELETE
```

The protocol itself understands the general semantic category.

---

# 14. A very important property: Safe methods

Now we're getting into proper HTTP semantics.

Some methods are considered **safe**.

The important ones for us:

```text
GET
HEAD
OPTIONS
TRACE
```

"Safe" doesn't mean:

> The server does absolutely nothing.

It means the client isn't requesting a state-changing action as the purpose of the request.

For example:

```http
GET /users
```

shouldn't mean:

```text
GET users
↓
delete all users
```

That would violate the expected semantics of GET.

The server might still update things such as:

```text
analytics counters
access logs
metrics
```

while processing a GET.

So:

> **Safe ≠ literally no side effects anywhere.**

---

# 15. Idempotency

Here's another very important HTTP concept.

Some methods are **idempotent**.

Roughly:

> Performing the same request multiple times has the same intended effect on the server's state as performing it once.

For example:

```http
PUT /users/123
```

with the same representation:

```json
{
  "name": "Riyaz"
}
```

Sending it:

```text
once
twice
10 times
```

should leave the resource in the same intended state.

Compare that with:

```http
POST /orders
```

Sending the same POST multiple times might create:

```text
Order #1
Order #2
Order #3
```

So POST is generally **not idempotent**.

We'll spend more time on this when we discuss retries and distributed systems.

---

# 16. Don't confuse idempotent with safe

These are different concepts.

```text
Safe
  ↓
Doesn't request a state-changing operation

Idempotent
  ↓
Repeating the same request has the same intended state effect
```

For example:

| Method | Safe? | Idempotent?     |
| ------ | ----- | --------------- |
| GET    | Yes   | Yes             |
| PUT    | No    | Yes             |
| DELETE | No    | Yes             |
| POST   | No    | No              |
| PATCH  | No    | Not necessarily |

These are general HTTP semantics; individual applications still need to implement behavior consistently.

---

# 17. Your exercises

Let's make this hands-on.

### Exercise 1

Send:

```bash
curl -v http://localhost:8080/users
```

Identify:

```text
Method
Target
Headers
Body
```

---

### Exercise 2

Send:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

Look at the server terminal.

Answer yourself:

```text
What is the method?
What is the target?
Which headers did curl add?
What is the body?
```

---

### Exercise 3

Try manually with `nc`:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

**Notice that `Content-Length` is 16.**

The body:

```text
{"name":"Riyaz"}
```

is 16 bytes in UTF-8/ASCII.

You are now manually constructing a POST request.

---

### Exercise 4

Try:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080

```

You don't need a body.

---

# The mental model to keep

At this point, don't think:

```text
GET = fetch
POST = create
PUT = update
PATCH = update
DELETE = delete
```

That's too simplistic.

Instead think:

```text
HTTP METHOD
     ↓
defines standardized semantics

HTTP TARGET
     ↓
identifies what the request is about

HEADERS
     ↓
metadata about the message/request

BODY
     ↓
optional content being transferred
```

And the full request:

```text
┌──────────────────────────────────────┐
│ POST /users HTTP/1.1                 │ ← method + target + version
├──────────────────────────────────────┤
│ Host: localhost:8080                 │
│ Content-Type: application/json       │ ← headers
│ Content-Length: 16                   │
├──────────────────────────────────────┤
│                                      │ ← empty line
├──────────────────────────────────────┤
│ {"name":"Riyaz"}                     │ ← body
└──────────────────────────────────────┘
```

Next, we'll tackle **URL structure** properly:

```text
http://example.com:8080/users/123?active=true&page=2#profile
│    │           │    │         │
│    │           │    │         └── fragment
│    │           │    └──────────── query
│    │           └───────────────── path
│    └───────────────────────────── host/port
└────────────────────────────────── scheme
```

We'll break down **scheme, host, port, path, query parameters, and fragments**, and you'll manually construct requests to different targets.

----

# Lesson 6 — URLs: How HTTP Identifies a Resource

So far, we've seen requests like:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

Now let's understand **exactly what `/users/123` means**, and how URLs are structured.

---

## 1. A URL has multiple parts

Consider:

```text
https://example.com:8080/users/123?active=true&page=2#profile
```

Break it down:

```text
https://example.com:8080/users/123?active=true&page=2#profile
  │         │       │       │              │
scheme     host    port    path           query
                                             │
                                          fragment
```

More precisely:

```text
https://
   ↓
scheme

example.com
   ↓
host

:8080
   ↓
port

/users/123
   ↓
path

?active=true&page=2
   ↓
query

#profile
   ↓
fragment
```

Let's understand each one.

---

# 2. Scheme

```text
https://
```

The scheme tells the client **which protocol/access mechanism to use**.

Common examples:

```text
http://
https://
```

For example:

```text
http://example.com
https://example.com
```

The important distinction:

```text
HTTP
```

is the application protocol.

```text
HTTPS
```

is essentially HTTP carried over a secure TLS connection.

We'll get into HTTPS and TLS much later.

---

# 3. Host

```text
example.com
```

The host identifies the destination server/service.

For example:

```text
http://example.com
http://api.example.com
http://localhost
```

When using HTTP/1.1, this becomes the `Host` header:

```http
GET /users HTTP/1.1
Host: example.com
```

This is something important we already encountered.

The URL:

```text
http://example.com/users
```

results in a request conceptually like:

```http
GET /users HTTP/1.1
Host: example.com
```

Notice that the **host is not normally repeated in the request target** for an origin-form HTTP/1.1 request.

---

# 4. Port

Consider:

```text
http://example.com:8080/users
```

The port is:

```text
8080
```

It tells the client which TCP port to connect to.

For example:

```text
http://localhost:8080
```

means:

```text
TCP connection → localhost:8080
```

Our Python server is listening here:

```python
HTTPServer(("localhost", 8080), Handler)
```

So:

```bash
curl http://localhost:8080/
```

connects to:

```text
localhost:8080
```

### Default ports

HTTP normally uses:

```text
http  → 80
https → 443
```

Therefore these are equivalent in terms of the default port:

```text
http://example.com
http://example.com:80
```

and:

```text
https://example.com
https://example.com:443
```

---

# 5. Path

Now:

```text
/users/123
```

is the **path**.

It identifies a resource within the server's namespace.

Examples:

```text
/
```

```text
/users
```

```text
/users/123
```

```text
/products/42/reviews
```

HTTP itself doesn't know what these resources actually mean.

Your application decides.

For example:

```text
/users/123
```

might mean:

> User whose ID is 123.

While:

```text
/products/42
```

might mean:

> Product whose ID is 42.

---

# 6. Path segments

A path can contain multiple segments.

For example:

```text
/users/123/orders/456
```

can be viewed as:

```text
users
  ↓
123
  ↓
orders
  ↓
456
```

These are called **path segments**.

A common REST API design is:

```text
/users
/users/123
/users/123/orders
/users/123/orders/456
```

Conceptually:

```text
/users
    → collection of users

/users/123
    → user 123

/users/123/orders
    → orders belonging to user 123

/users/123/orders/456
    → order 456 belonging to user 123
```

Again, these meanings are application-level conventions, not something HTTP itself enforces.

---

# 7. Query parameters

Now add:

```text
?active=true&page=2
```

Example:

```text
/users?active=true&page=2
```

The query contains parameters:

```text
active=true
page=2
```

Typically:

```text
?key=value&key=value
```

For example:

```text
/products?category=books&page=2&limit=20
```

Conceptually:

```text
category → books
page     → 2
limit    → 20
```

---

# 8. Path vs Query

This distinction is extremely important for API design.

Compare:

```text
/users/123
```

and:

```text
/users?id=123
```

Both could potentially be implemented by an application to retrieve user 123.

But they communicate different structures.

Typically:

```text
/users/123
```

means:

> The specific resource identified as user 123.

While:

```text
/users?id=123
```

means something more like:

> Query the `/users` resource using `id=123` as a parameter.

A common API pattern is:

```text
GET /users/123
```

for one specific user.

And:

```text
GET /users?page=2&limit=20
```

for querying/filtering the collection.

---

# 9. Fragment

Now consider:

```text
/users/123#profile
```

Everything after:

```text
#
```

is the **fragment**:

```text
profile
```

Here's the surprising part:

### The fragment is generally NOT sent to the HTTP server.

If you enter:

```text
https://example.com/users/123#profile
```

the browser uses:

```text
/users/123
```

for the HTTP request.

The:

```text
#profile
```

part is handled client-side.

This is particularly common with web pages:

```text
https://example.com/docs#authentication
```

The browser can use `#authentication` to navigate to a particular section of the document.

---

# 10. Let's see this ourselves

Start our server:

```bash
python3 server.py
```

Now:

```bash
curl -v "http://localhost:8080/users/123?active=true&page=2"
```

Your server should see something like:

```text
GET /users/123?active=true&page=2 HTTP/1.1
```

Notice:

```text
/users/123?active=true&page=2
```

is what arrives as the request target.

---

## Now try a fragment

Run:

```bash
curl -v "http://localhost:8080/users/123?active=true#profile"
```

Your server should see:

```text
GET /users/123?active=true HTTP/1.1
```

Not:

```text
GET /users/123?active=true#profile HTTP/1.1
```

The fragment never made it into the HTTP request.

This is a **very useful thing to remember**.

---

# 11. Let's inspect the URL in Python

Modify our handler:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        print("\n--- REQUEST ---")
        print("Method:", self.command)
        print("Path:", self.path)

        print("Headers:")
        for name, value in self.headers.items():
            print(f"{name}: {value}")

        body = b"Hello HTTP!"

        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()

        self.wfile.write(body)


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")
server.serve_forever()
```

Then:

```bash
curl "http://localhost:8080/products?category=books&page=2"
```

You'll see:

```text
Method: GET
Path: /products?category=books&page=2
```

Our server gets the **path + query**, but not the fragment.

---

# 12. One important terminology correction

Earlier we simplified the request target as "the path."

Strictly speaking, that's not always correct.

HTTP/1.1 defines a **request-target**, which can have different forms.

The common one you've been using is:

```text
/users/123
```

or:

```text
/users/123?active=true
```

This is called **origin-form**.

There are other forms used for specific situations, such as proxies and the `CONNECT` method.

You don't need those yet.

For now, think:

```text
URL
 ↓
scheme + host + port + path + query + fragment
```

and:

```text
HTTP request
 ↓
method + request-target + HTTP version
```

with the host typically represented separately by:

```http
Host: example.com
```

---

# 13. Your mental model

Keep this picture:

```text
URL

https://example.com:8080/users/123?active=true&page=2#profile
│       │           │       │              │
│       │           │       │              └── Fragment
│       │           │       └───────────────── Query
│       │           └───────────────────────── Path
│       └───────────────────────────────────── Host + Port
└───────────────────────────────────────────── Scheme
```

HTTP request:

```http
GET /users/123?active=true&page=2 HTTP/1.1
Host: example.com
```

Notice:

```text
#profile
```

is missing.

That's because the fragment is not part of the HTTP request sent to the server.

---

## 🧪 Exercise

Try these one by one:

```bash
curl -v "http://localhost:8080/"
```

```bash
curl -v "http://localhost:8080/users/123"
```

```bash
curl -v "http://localhost:8080/users/123?active=true"
```

```bash
curl -v "http://localhost:8080/users/123?active=true&page=2"
```

```bash
curl -v "http://localhost:8080/users/123?active=true#profile"
```

For each one, look specifically at the line:

```http
GET ...
```

and ask yourself:

**Which parts of the URL actually became part of the HTTP request?**

Once that's clear, the next lesson is **URL encoding** — why spaces, `&`, `?`, Unicode, and characters like `+` and `%20` can completely change how a URL is interpreted.

---

# Lesson 7 — URL Encoding

Now we know that a URL can contain:

```text
scheme + host + port + path + query + fragment
```

But there is a problem.

**What if the data itself contains characters that have special meaning in a URL?**

For example:

```text
hello world
```

or:

```text
John & Sons
```

or:

```text
C++
```

This is where **URL encoding** comes in.

---

## 1. The problem

Suppose we want to search for:

```text
hello world
```

A naïve URL might be:

```text
/users?search=hello world
```

But URLs have syntax rules, and a literal space isn't valid in the URL's normal form.

So we encode the space:

```text
/users?search=hello%20world
```

Here:

```text
%20
```

means the byte representing a space.

So:

```text
hello world
```

becomes:

```text
hello%20world
```

This process is called **percent-encoding**.

---

# 2. Why `%`

The general form is:

```text
%XX
```

where `XX` is the hexadecimal representation of a byte.

Some common examples:

| Character | Encoding |
| --------- | -------- |
| space     | `%20`    |
| `&`       | `%26`    |
| `?`       | `%3F`    |
| `=`       | `%3D`    |
| `#`       | `%23`    |
| `%`       | `%25`    |
| `/`       | `%2F`    |
| `+`       | `%2B`    |

For example:

```text
hello world
```

→

```text
hello%20world
```

And:

```text
John & Sons
```

→

```text
John%20%26%20Sons
```

---

# 3. Why does `&` matter?

Consider:

```text
/users?name=John&age=30
```

The `&` has special meaning.

It separates query parameters:

```text
name = John
age  = 30
```

But suppose the actual name is:

```text
John & Sons
```

If we send:

```text
/users?name=John & Sons
```

the `&` could be interpreted as the beginning of another parameter.

Instead:

```text
/users?name=John%20%26%20Sons
```

Now the server can interpret the value as:

```text
name = "John & Sons"
```

---

# 4. Let's experiment with curl

Start your server:

```bash
python3 server.py
```

Try:

```bash
curl -v "http://localhost:8080/search?query=hello%20world"
```

Your server will receive:

```text
/search?query=hello%20world
```

The URL contains the encoded representation.

---

# 5. Python can decode it

Python provides utilities for URL parsing.

Try this separately:

```bash
python3
```

Then:

```python
from urllib.parse import unquote

unquote("hello%20world")
```

Result:

```text
'hello world'
```

And:

```python
unquote("John%20%26%20Sons")
```

Result:

```text
'John & Sons'
```

So conceptually:

```text
Client
  |
  | encoded URL
  v
/search?name=John%20%26%20Sons
  |
  v
Server
  |
  | decode
  v
John & Sons
```

---

# 6. Query parameters have another important concept

Let's say we have:

```text
/products?category=books&page=2
```

There are two layers:

### URL syntax

```text
?       → starts query
&       → separates parameters
=       → separates key and value
```

### Application data

```text
category = books
page     = 2
```

That's why encoding is important.

If the value itself contains a special character, we encode it.

---

# 7. A very common example: search

Suppose the user searches:

```text
java http tutorial
```

We could construct:

```text
/search?q=java%20http%20tutorial
```

The server decodes:

```text
java http tutorial
```

Another example:

```text
/search?q=machine%20learning%20%26%20AI
```

decodes to:

```text
machine learning & AI
```

---

# 8. `+` vs `%20`

You'll often see spaces represented as:

```text
%20
```

But sometimes you'll see:

```text
+
```

For example:

```text
/search?q=hello+world
```

This is particularly common with **HTML form/query encoding** (`application/x-www-form-urlencoded`).

Python demonstrates the distinction nicely:

```python
from urllib.parse import unquote, unquote_plus

unquote("hello+world")
```

gives:

```text
'hello+world'
```

while:

```python
unquote_plus("hello+world")
```

gives:

```text
'hello world'
```

So don't blindly assume:

```text
+
```

always means a space everywhere in URLs.

Its interpretation depends on the encoding context.

---

# 9. The interesting case: `/`

Consider:

```text
/users/123
```

The `/` separates path segments:

```text
users
123
```

But suppose the actual ID contains `/`.

For example, imagine an ID:

```text
abc/123
```

If we put it directly into the path:

```text
/users/abc/123
```

the server sees two path segments:

```text
users
abc
123
```

But perhaps we wanted **one ID**:

```text
"abc/123"
```

We can percent-encode the slash:

```text
/users/abc%2F123
```

Now the encoded slash represents data rather than the path separator.

This distinction becomes important when designing APIs.

---

# 10. Path encoding vs query encoding

This is a subtle but important point.

Consider:

```text
/users/abc%2F123
```

versus:

```text
/users?name=abc%2F123
```

The same `%2F` sequence appears, but it occurs in different URL components.

URL encoding isn't simply:

> "Replace weird characters."

It is about **representing data without accidentally changing the syntax of the URL component containing that data.**

As you get deeper into web development, you'll encounter different encoding rules for:

* path segments
* query parameters
* form data
* fragments

Don't memorize every rule yet. Understand the principle.

---

# 11. Let's parse URLs properly

Python has a useful tool:

```python
from urllib.parse import urlparse
```

Try:

```python
url = "https://example.com:8080/users/123?active=true&page=2#profile"

result = urlparse(url)

print(result)
```

You'll get something conceptually like:

```text
scheme   = https
netloc   = example.com:8080
path     = /users/123
query    = active=true&page=2
fragment = profile
```

And:

```python
print(result.hostname)
```

gives:

```text
example.com
```

while:

```python
print(result.port)
```

gives:

```text
8080
```

---

# 12. Parse query parameters

Python also has:

```python
from urllib.parse import parse_qs
```

Try:

```python
parse_qs("active=true&page=2")
```

You'll get:

```python
{
    "active": ["true"],
    "page": ["2"]
}
```

Notice something interesting:

**values are lists.**

That's because a query parameter can occur multiple times.

For example:

```text
/products?tag=java&tag=http&tag=backend
```

can represent:

```text
tag = ["java", "http", "backend"]
```

This is one reason you shouldn't assume that every query parameter is always a single key/value pair.

---

# 13. A practical API example

Imagine:

```text
GET /products?category=books&minPrice=100&maxPrice=500&page=2
```

We can conceptually parse it as:

```text
Path:
    /products

Query:
    category = books
    minPrice = 100
    maxPrice = 500
    page     = 2
```

The HTTP layer transports this request.

Your application decides what those parameters mean.

This distinction is worth remembering:

```text
HTTP
 ↓
transports request

Application
 ↓
interprets path/query/body
```

---

# 14. One important security lesson

Never assume that encoded and decoded URLs are interchangeable strings.

For example:

```text
/users/a%2Fb
```

and:

```text
/users/a/b
```

are **not necessarily the same request target**.

The encoded version may represent one path segment containing `/`, while the decoded version contains two path segments.

This matters for:

* routing
* authorization
* caching
* proxies
* security checks

We'll revisit this when we get to real-world HTTP behavior.

---

# 🧪 Exercise

Try these:

```bash
curl "http://localhost:8080/search?q=hello%20world"
```

```bash
curl "http://localhost:8080/search?q=John%20%26%20Sons"
```

```bash
curl "http://localhost:8080/search?q=java%20%26%20spring&page=2"
```

Then inspect what your server receives.

### Challenge

Without using a browser, figure out what this query represents:

```text
/search?q=HTTP%20%26%20URLs&page=2
```

It should represent:

```text
q = ?
page = ?
```

---

## Mental model

At this point, think of a URL like this:

```text
https://example.com:8080/users/123?active=true#profile
│       │           │       │              │
│       │           │       │              └─ fragment
│       │           │       └──────────────── query
│       │           └──────────────────────── path
│       └──────────────────────────────────── host + port
└──────────────────────────────────────────── scheme
```

And when data conflicts with URL syntax:

```text
raw data
   ↓
percent-encoding
   ↓
safe URL representation
   ↓
HTTP request
   ↓
decode
   ↓
original data
```

**Next: HTTP status codes** — we'll go beyond just `200` and `404` and understand why `201`, `204`, `301`, `302`, `400`, `401`, `403`, `409`, `422`, `429`, and `500` exist and when each is appropriate.


---

# Lesson 8 — HTTP Status Codes

We've already seen:

```http
HTTP/1.0 200 OK
```

and:

```http
HTTP/1.0 404 Not Found
```

Now let's understand **what status codes actually mean and why there are so many of them**.

---

## 1. The basic structure

Every HTTP response starts with a status line:

```http
HTTP/1.1 200 OK
```

It contains:

```text
HTTP-version
    ↓
HTTP/1.1  200  OK
         │    │
         │    └── reason phrase
         └─────── status code
```

The important part is:

```text
200
```

The reason phrase:

```text
OK
```

is mainly human-readable.

The **status code is what matters to software**.

---

# 2. Status codes are grouped into 5 classes

The first digit tells you the general category:

```text
1xx → Informational
2xx → Success
3xx → Redirection
4xx → Client error
5xx → Server error
```

Think:

```text
1xx  "I'm still processing / here's information"
2xx  "The request succeeded"
3xx  "Look somewhere else / use another representation"
4xx  "The request can't be fulfilled as submitted"
5xx  "The server failed to fulfill a valid request"
```

We'll go through the important ones.

---

# 3. 2xx — Success

## 200 OK

The most common successful response.

Example:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

Response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 123,
  "name": "Riyaz"
}
```

Meaning:

> The request succeeded.

---

## 201 Created

Used when a request successfully creates a resource.

For example:

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":"Riyaz"}
```

Response:

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 123,
  "name": "Riyaz"
}
```

Often you'll also see:

```http
Location: /users/123
```

So the response might be:

```http
HTTP/1.1 201 Created
Location: /users/123
Content-Type: application/json
```

This communicates:

> The resource was created, and this is its location.

---

# 4. 204 No Content

This means:

> The request succeeded, but there is no response content to return.

For example:

```http
DELETE /users/123 HTTP/1.1
Host: example.com
```

Response:

```http
HTTP/1.1 204 No Content
```

There is no response body.

This is common for:

```text
DELETE
```

and sometimes:

```text
PUT
PATCH
```

when the server doesn't need to return the updated resource.

---

# 5. 3xx — Redirection

3xx responses generally tell the client:

> The requested resource or representation requires another action/location.

The most famous ones are:

```text
301
302
303
307
308
```

We'll focus on the important concepts first.

---

## 301 Moved Permanently

Suppose:

```text
http://example.com/old
```

has permanently moved to:

```text
https://example.com/new
```

The server can respond:

```http
HTTP/1.1 301 Moved Permanently
Location: https://example.com/new
```

The important header is:

```http
Location: ...
```

The client can then make another request to that location.

---

# 6. 302 Found

Historically, `302` has been used for temporary redirects.

Example:

```http
HTTP/1.1 302 Found
Location: /login
```

The client may then request:

```http
GET /login HTTP/1.1
Host: example.com
```

There is an important historical complication around how clients handle methods across redirects.

That's why HTTP also defines:

```text
303 See Other
307 Temporary Redirect
308 Permanent Redirect
```

We'll return to this when we study redirects properly.

For now:

```text
301 → permanent redirect
302 → temporary redirect (with historical behavior)
```

is enough.

---

# 7. 4xx — Client-side/request problems

This category is extremely important for APIs.

It generally means:

> The server received the request, but there is something wrong with the request or the request cannot be fulfilled as submitted.

---

## 400 Bad Request

The request is malformed or invalid.

For example:

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":
```

That's invalid JSON.

The server might respond:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": "Invalid JSON"
}
```

Think:

```text
400
↓
"I can't properly process this request as submitted."
```

---

# 8. 401 Unauthorized

This one causes a lot of confusion.

`401` generally means:

> Authentication is required or the supplied authentication credentials are not acceptable.

For example:

```http
GET /profile HTTP/1.1
Host: example.com
```

Server:

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer
```

Notice the important header:

```text
WWW-Authenticate
```

We'll study this deeply in the authentication section.

### Important terminology

Despite the name:

```text
401 Unauthorized
```

it is fundamentally about **authentication**.

Think:

```text
401 → "Who are you?"
```

---

# 9. 403 Forbidden

Now suppose the client is authenticated:

```text
User = Riyaz
```

but doesn't have permission to access something.

Example:

```http
DELETE /admin/users/123 HTTP/1.1
Authorization: Bearer ...
```

Server:

```http
HTTP/1.1 403 Forbidden
```

Think:

```text
401 → "You haven't successfully authenticated."

403 → "I know who you are, but this request isn't permitted."
```

This distinction is extremely useful.

---

# 10. 404 Not Found

You've already seen this.

```http
HTTP/1.1 404 Not Found
```

Typically means the server cannot find a current representation/resource corresponding to the request target.

Example:

```http
GET /users/999999 HTTP/1.1
```

If user 999999 doesn't exist:

```http
HTTP/1.1 404 Not Found
```

---

# 11. 405 Method Not Allowed

This is different from 404.

Suppose:

```text
/users/123
```

exists, but the API only supports:

```text
GET
PATCH
DELETE
```

and you send:

```http
POST /users/123 HTTP/1.1
```

The server can return:

```http
HTTP/1.1 405 Method Not Allowed
Allow: GET, PATCH, DELETE
```

Notice:

```text
Allow: ...
```

This header tells the client which methods are supported for that resource.

---

# 12. 409 Conflict

This is used when the request conflicts with the current state of the resource.

Classic example:

```text
POST /users
```

with:

```json
{
  "email": "riyaz@example.com"
}
```

Suppose that email must be unique and already exists.

The server might return:

```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "error": "Email already exists"
}
```

Another example is concurrent resource state conflicts.

The key idea:

```text
409
↓
"The request conflicts with the current state."
```

---

# 13. 422 Unprocessable Content

You may encounter:

```http
HTTP/1.1 422 Unprocessable Content
```

This is commonly used when:

> The request is syntactically valid, but its content cannot be processed because it violates application-level validation/semantic rules.

For example:

```json
{
  "email": "not-an-email",
  "age": -5
}
```

The JSON itself is valid.

So this isn't necessarily:

```text
400 → malformed JSON
```

Instead, the content may fail validation.

A server could respond:

```http
HTTP/1.1 422 Unprocessable Content
Content-Type: application/json

{
  "errors": {
    "email": "Invalid email",
    "age": "Must be positive"
  }
}
```

There is some variation among API designs over when to use `400` versus `422`; consistency within an API matters.

---

# 14. 429 Too Many Requests

Imagine a client sends:

```text
10,000 requests/second
```

while the API allows:

```text
100 requests/minute
```

The server may respond:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
```

Meaning:

> You are being rate limited.

This becomes important when we study:

* rate limiting
* APIs
* retries
* distributed systems

---

# 15. 5xx — Server problems

Now we reach:

```text
5xx
```

These generally mean:

> The server failed to fulfill a request that it otherwise received appropriately.

---

## 500 Internal Server Error

The generic server failure.

For example:

```python
def do_GET(self):
    result = 10 / 0
```

If your server doesn't handle the exception properly, the request may result in a server error.

Conceptually:

```http
HTTP/1.1 500 Internal Server Error
```

Think:

```text
500
↓
"Something went wrong on the server."
```

---

# 16. 502 Bad Gateway

This becomes particularly important in distributed systems.

Imagine:

```text
Client
   ↓
Load Balancer / API Gateway
   ↓
Backend Service
```

Suppose the gateway receives an invalid/unusable response from the upstream service.

It might return:

```http
HTTP/1.1 502 Bad Gateway
```

So:

```text
502
↓
"I am acting as a gateway/proxy and got a bad response from upstream."
```

---

# 17. 503 Service Unavailable

Used when the server is currently unable to handle the request, often temporarily.

Examples include:

```text
server overloaded
maintenance
temporarily unavailable
```

Response:

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 30
```

This can tell the client:

> Try again later.

---

# 18. 504 Gateway Timeout

Again, imagine:

```text
Client
   ↓
API Gateway
   ↓
Backend
```

The gateway waits for the backend:

```text
10 seconds...
20 seconds...
30 seconds...
```

and the upstream doesn't respond in time.

The gateway may return:

```http
HTTP/1.1 504 Gateway Timeout
```

Think:

```text
502 → upstream response was bad

504 → upstream didn't respond in time
```

---

# 19. The most useful status-code map

For backend/API development, this is a good initial mental map:

```text
SUCCESS
------
200  OK
201  Created
204  No Content


REDIRECTION
-----------
301  Moved Permanently
302  Found
303  See Other
307  Temporary Redirect
308  Permanent Redirect


CLIENT / REQUEST
----------------
400  Bad Request
401  Authentication required / failed
403  Forbidden
404  Not Found
405  Method Not Allowed
409  Conflict
422  Unprocessable Content
429  Too Many Requests


SERVER / INFRASTRUCTURE
-----------------------
500  Internal Server Error
502  Bad Gateway
503  Service Unavailable
504  Gateway Timeout
```

Don't try to memorize every HTTP status code.

Understand the **categories and situations**.

---

# 20. Let's make our server return different codes

Modify your server:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):

        if self.path == "/":
            body = b"Hello HTTP!"
            self.send_response(200)

        elif self.path == "/created":
            body = b"Created"
            self.send_response(201)

        elif self.path == "/empty":
            body = b""
            self.send_response(204)

        elif self.path == "/bad":
            body = b"Bad request"
            self.send_response(400)

        elif self.path == "/missing":
            body = b"Not found"
            self.send_response(404)

        else:
            body = b"Internal server error"
            self.send_response(500)

        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()

        if body:
            self.wfile.write(body)


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")
server.serve_forever()
```

Restart it and try:

```bash
curl -i http://localhost:8080/
```

Then:

```bash
curl -i http://localhost:8080/created
```

Then:

```bash
curl -i http://localhost:8080/empty
```

Then:

```bash
curl -i http://localhost:8080/missing
```

Observe the first line of each response.

---

# 21. One very important misconception

Don't think:

```text
HTTP status code = whether my business operation succeeded
```

It's slightly more nuanced.

HTTP status codes describe the **HTTP-level outcome** of the request.

Your application then has its own semantics.

For example, this is possible:

```http
HTTP/1.1 200 OK

{
    "success": false,
    "error": "Payment declined"
}
```

Technically the HTTP request succeeded and the server successfully communicated the application result.

Whether an API **should** design responses this way is a separate API-design question.

A common API design instead might use an appropriate HTTP status for the application outcome.

We'll revisit this when we build our API.

---

# 🧠 The bigger picture

You now have:

```text
HTTP Request
│
├── Method
├── Request-target
├── HTTP version
├── Headers
├── Blank line
└── Body
          │
          ▼
       Server
          │
          ▼
HTTP Response
│
├── Status code
├── Headers
├── Blank line
└── Body
```

And the status code gives the client a standardized signal about what happened.

For example:

```text
GET /users/123

        ↓

200 → here is the user
404 → no such resource
401 → authentication required
403 → request isn't permitted
500 → server failed
```

---

## 🧪 Small challenge

Without looking back, what status code would you choose for each?

1. `GET /users/123` and user exists
2. `POST /users` successfully creates a user
3. `DELETE /users/123` successfully deletes the user and there's nothing to return
4. User doesn't exist
5. User isn't authenticated
6. User is authenticated but doesn't have permission
7. JSON request body is malformed
8. API rate limit exceeded
9. Backend service behind an API gateway timed out
10. Server encountered an unexpected exception

Once you've got these, say **next**.

Next we'll move into **request headers in much more depth**: `Accept`, `Content-Type`, `Authorization`, `Cookie`, `User-Agent`, `Host`, `Connection`, and why headers are essentially the **metadata/control plane of HTTP**.


---

