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

# Lesson 9 — Request Headers in Depth

We've already seen headers like:

```http
GET /users HTTP/1.1
Host: localhost:8080
User-Agent: curl/8.0
Accept: */*
```

Now let's answer a deeper question:

> **Why do we need headers at all?**

The HTTP request already has a method, target, and possibly a body. Why add another mechanism?

---

# 1. Headers are metadata about the message

Think about this request:

```http
POST /users HTTP/1.1
Host: example.com
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

The body contains the actual data:

```json
{"name":"Riyaz"}
```

But the server needs additional information:

```text
What format is this?
How many bytes are there?
Who is sending this?
What response format does the client want?
Is the client authenticated?
```

That's what headers communicate.

So a useful mental model is:

```text
HTTP message
│
├── Control information
│      ├── method
│      ├── target
│      └── status
│
├── Metadata
│      └── headers
│
└── Data
       └── body
```

---

# 2. Header syntax

The basic syntax is:

```http
Header-Name: value
```

For example:

```http
Host: example.com
Accept: application/json
Content-Type: application/json
```

Header names are case-insensitive.

These refer to the same header:

```text
Content-Type
content-type
CONTENT-TYPE
CoNtEnT-TyPe
```

But conventionally we write:

```text
Content-Type
```

---

# 3. `Host`

You've already encountered this one.

```http
GET /users HTTP/1.1
Host: example.com
```

It identifies the host being addressed by the HTTP request.

This becomes particularly important when many websites share the same server/IP.

For example:

```text
                    Server
                 203.0.113.10
                  /         \
                 /           \
        example.com       api.example.com
```

The TCP connection could reach:

```text
203.0.113.10:443
```

while HTTP tells the server:

```http
Host: api.example.com
```

The server can therefore route the request to the appropriate virtual host.

---

# 4. `User-Agent`

Try:

```bash
curl -v http://localhost:8080/
```

You'll probably see something like:

```http
User-Agent: curl/8.x.x
```

This tells the server information about the client software.

A browser might send something much more complicated:

```http
User-Agent: Mozilla/5.0 ...
```

The server can use this information for things like:

* logging
* analytics
* compatibility behavior
* debugging

But:

> **Never treat User-Agent as trustworthy authentication.**

A client can easily change it:

```bash
curl -H "User-Agent: MyFakeBrowser" http://localhost:8080/
```

---

# 5. `Accept`

This is one of the most important headers.

Suppose the client says:

```http
Accept: application/json
```

It's communicating:

> I can accept a JSON representation.

For example:

```http
GET /users/123 HTTP/1.1
Host: example.com
Accept: application/json
```

The server might return:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"id":123,"name":"Riyaz"}
```

Notice the relationship:

```text
Request:
Accept: application/json
       ↓
"What response format can I accept?"

Response:
Content-Type: application/json
       ↓
"What format am I actually sending?"
```

---

# 6. `Accept` vs `Content-Type`

This distinction is worth memorizing.

### `Accept`

Describes the **desired/acceptable representation of the response**.

```http
Accept: application/json
```

Think:

> "Please give me JSON if possible."

### `Content-Type`

Describes the **actual representation of the message body**.

Request:

```http
Content-Type: application/json
```

means:

> "The body I'm sending is JSON."

Response:

```http
Content-Type: application/json
```

means:

> "The body I'm returning is JSON."

So:

```text
REQUEST
------------------------
Accept       → response
Content-Type → request body


RESPONSE
------------------------
Content-Type → response body
```

---

# 7. Let's see this with curl

Run:

```bash
curl -v \
  -H "Accept: application/json" \
  http://localhost:8080/
```

Your server can inspect:

```python
print("Accept:", self.headers.get("Accept"))
```

You should see something like:

```text
Accept: application/json
```

Now change it:

```bash
curl -v \
  -H "Accept: text/plain" \
  http://localhost:8080/
```

The server receives:

```text
Accept: text/plain
```

HTTP itself doesn't automatically convert your response.

Your application has to decide what to do with this information.

---

# 8. `Content-Type`

Now imagine:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

The server sees:

```text
Content-Type: application/json
```

and knows how the body is intended to be interpreted.

Other examples:

```text
text/plain
text/html
application/json
application/xml
application/octet-stream
multipart/form-data
application/x-www-form-urlencoded
```

We'll study these properly when we get to request bodies.

---

# 9. `Content-Length`

We've already discussed this, but now put it into the header mental model:

```http
Content-Length: 16
```

means:

> The body contains 16 bytes.

For example:

```http
POST /users HTTP/1.1
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

The server can use the length to know how many bytes to read.

This is especially important when HTTP/1.1 connections remain open and multiple requests/responses may use the same connection.

---

# 10. `Authorization`

Now we're getting into authentication.

A common request looks like:

```http
GET /profile HTTP/1.1
Host: example.com
Authorization: Bearer eyJhbGciOi...
```

The header communicates credentials/authentication information.

The common structure is:

```text
Authorization: <scheme> <credentials>
```

For example:

```text
Authorization: Bearer abc123
```

Here:

```text
Bearer
   ↓
authentication scheme

abc123
   ↓
credential/token
```

We'll spend an entire section on authentication later.

For now, remember:

```text
Authorization
      ↓
credentials associated with the request
```

---

# 11. `Cookie`

Browsers commonly send:

```http
Cookie: session_id=abc123
```

This lets the server associate the request with previously established client state.

For example:

```text
Browser
   │
   │ Cookie: session_id=abc123
   ▼
Server
   │
   └── finds session abc123
          ↓
       User = Riyaz
```

Cookies become especially interesting when we study:

* sessions
* authentication
* browser behavior
* security

---

# 12. `Connection`

With HTTP/1.1, you'll sometimes encounter:

```http
Connection: keep-alive
```

This relates to the underlying TCP connection and whether it should remain usable for additional HTTP exchanges.

For example:

```text
TCP connection
│
├── HTTP request 1
├── HTTP response 1
├── HTTP request 2
├── HTTP response 2
└── ...
```

Instead of:

```text
TCP connection 1
└── HTTP request/response

TCP connection 2
└── HTTP request/response

TCP connection 3
└── ...
```

We'll study connection reuse in detail later.

---

# 13. `Cache-Control`

A client can send:

```http
Cache-Control: no-cache
```

and a server can respond with:

```http
Cache-Control: max-age=3600
```

These headers participate in HTTP caching behavior.

For example:

```text
Cache-Control: max-age=3600
```

can communicate that a response may be considered fresh for 3600 seconds under the applicable caching rules.

Caching is much deeper than simply:

> "Don't call the server again."

We'll study it later.

---

# 14. `Referer`

You may encounter:

```http
Referer: https://example.com/products
```

It can indicate the page/resource from which a request was initiated.

For example:

```text
User viewing:
/products

      ↓ click

/login

Request:
Referer: https://example.com/products
```

There are privacy/security considerations around this header, and browsers control/refine what gets sent based on referrer policy.

One interesting detail:

The HTTP header is historically spelled:

```text
Referer
```

not:

```text
Referrer
```

---

# 15. Custom headers

Applications can define their own headers.

For example:

```http
X-Request-ID: abc-123
```

or modern application-specific names such as:

```http
Trace-Id: abc-123
```

These can be used for things like:

```text
request tracing
correlation IDs
internal metadata
feature information
```

However, don't assume every header beginning with `X-` is automatically special.

The `X-` convention was historically popular, but it isn't a requirement for custom HTTP fields.

---

# 16. Headers aren't necessarily trusted

This is extremely important for backend engineering.

Suppose a client sends:

```http
X-User-Id: 123
```

Can your server conclude:

> "This request is from user 123"?

**No.**

The client controls its own request headers.

Anyone can run:

```bash
curl \
  -H "X-User-Id: 123" \
  http://example.com/admin
```

Headers are just data supplied by the requester unless some trusted infrastructure guarantees otherwise.

This is why:

```text
Authorization
```

must be validated rather than blindly trusted.

Similarly:

```text
User-Agent
X-Forwarded-For
X-User-Id
```

should not automatically be treated as trustworthy identity information.

---

# 17. Let's inspect everything ourselves

Run:

```bash
curl -v \
  -H "Accept: application/json" \
  -H "X-Student: Riyaz" \
  -H "X-Request-ID: abc123" \
  http://localhost:8080/users
```

Your server should receive something similar to:

```http
GET /users HTTP/1.1
Host: localhost:8080
User-Agent: curl/...
Accept: application/json
X-Student: Riyaz
X-Request-ID: abc123
```

Notice something fundamental:

**HTTP doesn't have a fixed small list of headers that every request must contain.**

There are standardized headers, optional headers, and application-defined fields.

---

# 18. Header order

Here's another subtle point.

You might see:

```http
Host: example.com
Accept: application/json
User-Agent: curl/...
```

or:

```http
User-Agent: curl/...
Accept: application/json
Host: example.com
```

The application should **not depend on header ordering**.

Unlike the request line:

```text
METHOD SP REQUEST-TARGET SP HTTP-VERSION
```

which has a defined structure, headers are a collection of fields.

So don't think:

```text
Host must be first
Accept must be second
Content-Type must be third
```

That's not how headers work.

---

# 19. Headers + body

Let's put everything together:

```http
POST /users HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json
Authorization: Bearer abc123
Content-Length: 16

{"name":"Riyaz"}
```

Break it down:

```text
Request line
    ↓
POST /users HTTP/1.1

Headers
    ↓
Host
Accept
Content-Type
Authorization
Content-Length

Blank line
    ↓

Body
    ↓
{"name":"Riyaz"}
```

This is the complete HTTP message structure you've been learning piece by piece.

---

# 🧠 The key mental model

Think of HTTP headers as **metadata/control information surrounding the message**.

```text
                 HTTP REQUEST
                       │
        ┌──────────────┴──────────────┐
        │                             │
   Request line                    Headers
        │                             │
   "What operation?"        "Additional information"
        │                             │
        └──────────────┬──────────────┘
                       │
                     Body
                       │
                "Actual payload"
```

Examples:

```text
Host            → who is being addressed?
Accept          → what response representation is acceptable?
Content-Type    → what is the body format?
Content-Length  → how many body bytes?
Authorization   → authentication credentials
Cookie          → client state
Cache-Control   → caching directives
User-Agent      → client software information
```

---

## 🧪 Challenge

Use `curl` to send this request:

```http
POST /users HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json
X-Request-ID: abc123

{"name":"Riyaz"}
```

You can do it with:

```bash
curl -v \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "X-Request-ID: abc123" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

Then look at **every header curl generated automatically** in addition to the ones you explicitly supplied.

Pay special attention to:

```text
Content-Type
Content-Length
Host
User-Agent
Accept
```

Ask yourself:

> **Which headers did I explicitly provide, and which ones did curl add because it knows how HTTP works?**

Next we'll go deeper into **request bodies and `Content-Type`**, including the difference between JSON, form data, plain text, and binary data—and we'll manually construct POST requests with `nc` so you can see exactly how the body is transmitted.


----

# Lesson 10 — HTTP Request Bodies & `Content-Type`

We've now seen that an HTTP request can contain:

```text
Request line
Headers
Blank line
Body
```

The body is where the client can send **data to the server**.

This lesson is important because it connects raw HTTP to things you'll constantly encounter as a backend engineer:

* JSON APIs
* HTML forms
* file uploads
* binary data
* `Content-Type`
* `Content-Length`

---

# 1. Does every HTTP request have a body?

No.

For example, a typical GET:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

has no body.

But a POST might have:

```http
POST /users HTTP/1.1
Host: example.com
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

So:

```text
GET
 └── usually no body

POST
 └── commonly has a body

PUT
 └── commonly has a body

PATCH
 └── commonly has a body

DELETE
 └── may or may not have a body
```

The important point is:

> **HTTP does not define "POST = JSON". POST is a method; JSON is a representation format.**

---

# 2. The blank line is extremely important

Look at this:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

There is an empty line between:

```http
Content-Length: 16
```

and:

```text
{"name":"Riyaz"}
```

That empty line marks the end of the header section.

Conceptually:

```text
┌────────────────────────────┐
│ Request line               │
├────────────────────────────┤
│ Headers                    │
│                            │
│ Content-Type: ...          │
│ Content-Length: ...        │
├────────────────────────────┤
│ BLANK LINE                 │
├────────────────────────────┤
│ Body                       │
│                            │
│ {"name":"Riyaz"}           │
└────────────────────────────┘
```

This is why when using `nc`, you need that blank line.

---

# 3. `Content-Type` tells us what the body represents

Suppose the body is:

```text
Hello HTTP
```

We could send:

```http
Content-Type: text/plain
```

If the body is JSON:

```json
{"name":"Riyaz"}
```

we send:

```http
Content-Type: application/json
```

If the body is HTML:

```html
<h1>Hello</h1>
```

we could send:

```http
Content-Type: text/html
```

So:

```text
Content-Type
     ↓
"What format is the message body in?"
```

---

# 4. JSON

JSON is probably the most familiar representation for modern APIs.

Request:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

The body is:

```json
{"name":"Riyaz"}
```

The server can parse it as JSON.

For example:

```python
import json

content_length = int(self.headers.get("Content-Length", 0))

body = self.rfile.read(content_length)

data = json.loads(body)

print(data)
print(data["name"])
```

The important sequence is:

```text
raw bytes
   ↓
read body
   ↓
decode bytes → text
   ↓
parse JSON
   ↓
Python object
```

For example:

```text
b'{"name":"Riyaz"}'
        ↓
'{"name":"Riyaz"}'
        ↓
{"name": "Riyaz"}
```

---

# 5. JSON is NOT HTTP

This distinction is fundamental.

You might hear:

> "The API uses JSON."

That doesn't mean HTTP itself is JSON.

HTTP transports the bytes.

JSON is simply one possible representation of those bytes.

Think:

```text
HTTP
│
├── request line
├── headers
├── body
│     └── JSON
│
└── response
```

You could instead send:

```text
HTTP
│
└── body
      ├── JSON
      ├── text
      ├── HTML
      ├── XML
      ├── form data
      └── binary data
```

---

# 6. Let's manually send JSON

This is a great experiment.

Start your server:

```bash
python3 server.py
```

Then in another terminal:

```bash
nc localhost 8080
```

Type:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

Remember:

**After `Content-Length: 16`, press Enter twice.**

The server reads exactly 16 bytes:

```text
{"name":"Riyaz"}
```

Let's verify the count.

```text
{
"
n
a
m
e
"
:
"
R
i
y
a
z
"
}
```

That's 16 bytes because these are all ASCII characters, where each character occupies one byte in UTF-8.

---

# 7. Why `Content-Length` matters

Suppose the server receives:

```http
POST /users HTTP/1.1
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

The server knows:

```text
Read 16 bytes from the body.
```

Without knowing where the body ends, the server would have to rely on another HTTP framing mechanism.

This becomes particularly important when multiple HTTP messages use the same connection.

We'll eventually get into:

```text
Content-Length
Transfer-Encoding: chunked
connection persistence
HTTP/1.1 message framing
```

For now:

> **`Content-Length` tells the recipient how many bytes belong to the body when that framing mechanism is used.**

---

# 8. Let's try plain text

Change the request:

```http
POST /message HTTP/1.1
Host: localhost:8080
Content-Type: text/plain
Content-Length: 11

Hello World
```

The body is simply:

```text
Hello World
```

Your application could do:

```python
body = self.rfile.read(content_length)

text = body.decode("utf-8")

print(text)
```

No JSON parsing is necessary.

---

# 9. `application/x-www-form-urlencoded`

Before JSON APIs became dominant, HTML forms commonly submitted data like:

```text
name=Riyaz&age=30
```

with:

```http
Content-Type: application/x-www-form-urlencoded
```

Example:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/x-www-form-urlencoded
Content-Length: 16

name=Riyaz&age=30
```

Conceptually:

```text
name → Riyaz
age  → 30
```

Notice how this looks somewhat like a URL query:

```text
?name=Riyaz&age=30
```

That's not an accident. Form URL encoding uses similar encoding rules.

---

# 10. `multipart/form-data`

Now imagine uploading a profile picture.

You might send:

```text
name = Riyaz
profile_picture = photo.jpg
```

A common format is:

```http
Content-Type: multipart/form-data; boundary=----XYZ
```

The body is divided into multiple parts:

```text
------XYZ
Content-Disposition: form-data; name="name"

Riyaz
------XYZ
Content-Disposition: form-data; name="profile_picture"; filename="photo.jpg"
Content-Type: image/jpeg

...binary image data...
------XYZ--
```

Conceptually:

```text
HTTP body
│
├── Part 1
│    └── name = Riyaz
│
└── Part 2
     └── photo.jpg
```

This is how many traditional web forms and file-upload APIs work.

Don't worry about manually parsing multipart yet.

Just understand:

> **The body can contain structured data, and `Content-Type` tells the receiver how that body is structured.**

---

# 11. Binary data

HTTP doesn't require the body to be text.

You can send:

```text
image
PDF
video
ZIP
audio
```

For example:

```http
Content-Type: application/pdf
```

or:

```http
Content-Type: image/jpeg
```

or:

```http
Content-Type: application/octet-stream
```

The body is ultimately just:

```text
bytes
```

This is an important mental model.

At the network level:

```text
HTTP body
   ↓
bytes
```

Then `Content-Type` tells the application how those bytes should be interpreted.

---

# 12. The same HTTP mechanism can carry completely different data

Imagine these three requests:

### JSON

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":"Riyaz"}
```

### Plain text

```http
POST /message HTTP/1.1
Content-Type: text/plain

Hello HTTP
```

### PDF

```http
POST /documents HTTP/1.1
Content-Type: application/pdf

...PDF bytes...
```

The HTTP mechanism is essentially the same:

```text
POST
headers
blank line
body
```

Only the representation of the body changes.

---

# 13. `Accept` comes back into the picture

Now combine what we learned earlier.

Suppose the client sends:

```http
GET /users/123 HTTP/1.1
Host: example.com
Accept: application/json
```

The client is communicating:

```text
"I can accept JSON as the response representation."
```

The server could respond:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 27

{"id":123,"name":"Riyaz"}
```

Now we have:

```text
REQUEST
-------------------------
Accept: application/json
        ↓
What response representation is acceptable?


RESPONSE
-------------------------
Content-Type: application/json
        ↓
What representation am I sending?
```

This distinction is one of the most important HTTP concepts.

---

# 14. What if `Content-Type` is wrong?

Suppose you send:

```http
Content-Type: application/json

Hello World
```

That's a problem.

You've told the server:

> "The body is JSON."

But:

```text
Hello World
```

is not valid JSON.

A proper server may reject the request.

For example:

```http
HTTP/1.1 400 Bad Request
```

The problem isn't that HTTP can't transport the bytes.

It can.

The problem is that the **representation doesn't match its declared type**.

---

# 15. What happens in a typical backend?

Suppose your Java/Spring API receives:

```http
POST /users HTTP/1.1
Content-Type: application/json

{"name":"Riyaz","age":30}
```

A framework might make this feel magical:

```java
@PostMapping("/users")
public User createUser(@RequestBody CreateUserRequest request) {
    ...
}
```

But underneath, conceptually:

```text
TCP
 ↓
HTTP parser
 ↓
headers
 ↓
Content-Type: application/json
 ↓
body bytes
 ↓
JSON parser
 ↓
Java object
 ↓
controller
```

The framework isn't inventing a new protocol.

It is doing the HTTP parsing and representation handling for you.

This is exactly why learning raw HTTP is valuable.

---

# 16. Let's make our tiny server understand JSON

Try this version:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer
import json


class Handler(BaseHTTPRequestHandler):

    def do_POST(self):

        print("\n--- REQUEST ---")
        print("Method:", self.command)
        print("Path:", self.path)

        content_type = self.headers.get("Content-Type")
        content_length = int(self.headers.get("Content-Length", 0))

        print("Content-Type:", content_type)
        print("Content-Length:", content_length)

        body = self.rfile.read(content_length)

        print("Raw body:", body)

        if content_type == "application/json":
            data = json.loads(body.decode("utf-8"))

            print("Parsed JSON:", data)

            response = json.dumps({
                "received": data
            }).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(response)))
            self.end_headers()

            self.wfile.write(response)

        else:
            self.send_response(415)
            self.end_headers()


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")
server.serve_forever()
```

Now run:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz","age":30}' \
  http://localhost:8080/users
```

You should get something like:

```json
{"received": {"name": "Riyaz", "age": 30}}
```

---

# 17. What's `415`?

We just introduced another status code:

```http
HTTP/1.1 415 Unsupported Media Type
```

It means, roughly:

> The server doesn't support the media type of the request content for this request.

Our tiny server only understands:

```text
application/json
```

So if you send:

```bash
curl \
  -X POST \
  -H "Content-Type: text/plain" \
  -d "Hello" \
  http://localhost:8080/users
```

our server returns:

```text
415
```

because we explicitly programmed it that way.

---

# 18. Important distinction: bytes → representation → application object

This is the deeper concept I want you to remember.

Suppose the client sends:

```json
{"name":"Riyaz"}
```

The journey is conceptually:

```text
                 NETWORK
                    │
                    ▼
              raw HTTP bytes
                    │
                    ▼
             HTTP parser
                    │
           ┌────────┴────────┐
           │                 │
        headers             body
           │                 │
           │          bytes: {...}
           │                 │
           │                 ▼
           │       Content-Type tells us
           │       how to interpret it
           │                 │
           │                 ▼
           │            JSON parser
           │                 │
           │                 ▼
           │          application object
           │
           └───────────────────────
```

That is essentially what frameworks hide from you.

---

# 🧪 Hands-on exercise

Try all three.

### JSON

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

### Plain text

```bash
curl -v \
  -X POST \
  -H "Content-Type: text/plain" \
  -d "Hello HTTP" \
  http://localhost:8080/users
```

### No Content-Type

```bash
curl -v \
  -X POST \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

Compare what the server receives.

Pay attention to:

```text
Content-Type
Content-Length
body
response status
```

---

# 🧠 The mental model

You should now be able to look at this:

```http
POST /users HTTP/1.1
Host: example.com
Accept: application/json
Content-Type: application/json
Content-Length: 16

{"name":"Riyaz"}
```

and understand every piece:

```text
POST
 ↓
What operation is being requested?

/users
 ↓
What resource is being targeted?

Accept: application/json
 ↓
What response representation can the client accept?

Content-Type: application/json
 ↓
What representation is the request body?

Content-Length: 16
 ↓
How many bytes belong to the body?

{"name":"Riyaz"}
 ↓
The actual request payload
```

### Next

We'll learn **JSON and HTTP request/response bodies more deeply**, including **serialization vs deserialization**, UTF-8/bytes, malformed JSON, and why `Content-Length` is measured in **bytes rather than characters**. Then we'll move into **path parameters and query parameters** and start building a real REST-style API.


---

# Lesson 11 — Bytes, Encoding, JSON, and `Content-Length`

This lesson connects several things we've seen separately:

```text
HTTP
 ↓
bytes
 ↓
encoding
 ↓
JSON
 ↓
application object
```

The goal is to understand what is **actually travelling over the network**.

---

## 1. The most important fact: HTTP transports bytes

At the lowest level, the HTTP body isn't:

```text
"Hello"
```

It is bytes.

For example:

```text
Hello
```

in ASCII/UTF-8 becomes:

```text
72 101 108 108 111
```

or in hexadecimal:

```text
48 65 6c 6c 6f
```

So conceptually:

```text
Application data
      ↓
encode
      ↓
bytes
      ↓
HTTP
      ↓
network
```

At the receiving side:

```text
network
   ↓
HTTP
   ↓
bytes
   ↓
decode
   ↓
application data
```

---

# 2. Strings aren't bytes

In Python:

```python
text = "Hello"
```

is a string.

You can convert it to bytes:

```python
text.encode("utf-8")
```

Result:

```python
b'Hello'
```

And convert it back:

```python
b"Hello".decode("utf-8")
```

Result:

```text
'Hello'
```

So:

```text
String
  ↓ encode UTF-8
Bytes
  ↓ decode UTF-8
String
```

---

# 3. Why UTF-8 matters

Try:

```python
text = "Hello 世界"
```

Now:

```python
len(text)
```

and:

```python
len(text.encode("utf-8"))
```

will give **different numbers**.

Why?

Because:

```text
Characters ≠ bytes
```

For example, ASCII characters typically occupy one UTF-8 byte:

```text
A → 1 byte
B → 1 byte
```

while many Unicode characters require multiple bytes.

For example:

```python
text = "世界"

print(len(text))
print(len(text.encode("utf-8")))
```

Conceptually:

```text
2 characters
6 bytes
```

because each of these Chinese characters takes 3 bytes in UTF-8.

---

# 4. This explains `Content-Length`

Suppose the body is:

```text
Hello
```

Then:

```http
Content-Length: 5
```

because there are 5 bytes.

But consider:

```text
世界
```

There are:

```text
2 characters
```

but UTF-8 requires:

```text
6 bytes
```

So:

```http
Content-Length: 6
```

not:

```http
Content-Length: 2
```

This is why `Content-Length` is fundamentally a **byte count**.

---

# 5. Let's verify it ourselves

Run:

```bash
python3
```

Then:

```python
text = "世界"

print(len(text))
print(len(text.encode("utf-8")))
print(text.encode("utf-8"))
```

You'll see the difference.

Now:

```python
text = "Hello 世界"

print(len(text))
print(len(text.encode("utf-8")))
```

Again:

```text
characters != bytes
```

---

# 6. JSON is text, but travels as bytes

Consider:

```json
{
  "name": "Riyaz"
}
```

JSON is a textual data format.

Before it travels through HTTP, it ultimately becomes bytes.

Conceptually:

```text
Python object
      ↓
JSON serialization
      ↓
JSON text
      ↓
UTF-8 encoding
      ↓
bytes
      ↓
HTTP body
```

---

# 7. Serialization

Suppose your application has:

```python
user = {
    "id": 123,
    "name": "Riyaz"
}
```

This is a Python object.

We can serialize it:

```python
import json

json_text = json.dumps(user)

print(json_text)
```

Result:

```json
{"id": 123, "name": "Riyaz"}
```

Then:

```python
body = json_text.encode("utf-8")
```

Now we have bytes.

So:

```text
Python dictionary
       ↓
json.dumps()
       ↓
JSON string
       ↓
.encode("utf-8")
       ↓
HTTP bytes
```

---

# 8. Deserialization

The reverse happens on the server.

Suppose the server receives:

```python
body = b'{"id":123,"name":"Riyaz"}'
```

First:

```python
text = body.decode("utf-8")
```

Then:

```python
user = json.loads(text)
```

Now:

```python
user["name"]
```

gives:

```text
Riyaz
```

So:

```text
HTTP bytes
     ↓
.decode()
     ↓
JSON text
     ↓
json.loads()
     ↓
Python object
```

This is **deserialization**.

---

# 9. Serialization vs deserialization

Keep this simple:

```text
Serialization
-------------
Object → JSON → bytes
```

```text
Deserialization
---------------
bytes → JSON → Object
```

In a Java backend, this is what happens conceptually when a framework turns:

```json
{"name":"Riyaz","age":30}
```

into:

```java
CreateUserRequest
```

and later turns:

```java
User
```

back into:

```json
{"id":123,"name":"Riyaz"}
```

---

# 10. What if JSON is malformed?

Consider:

```json
{"name":
```

This isn't valid JSON.

The bytes themselves can still be transmitted perfectly.

The problem happens when the application tries:

```python
json.loads(...)
```

and the JSON parser fails.

So there are different layers of failure:

```text
TCP/network
   ↓
HTTP parsing
   ↓
Content-Type interpretation
   ↓
JSON parsing
   ↓
application validation
   ↓
business logic
```

For example:

```text
Malformed HTTP
      ↓
400-ish HTTP-level problem

Valid HTTP
but malformed JSON
      ↓
application/parser error

Valid JSON
but invalid application data
      ↓
validation error

Valid data
but business rule violated
      ↓
application-level conflict/error
```

This layered model will become extremely useful later.

---

# 11. JSON syntax vs JSON semantics

Consider:

```json
{"age": -100}
```

This is valid JSON.

The JSON parser has no problem with it.

But your application might say:

```text
age must be >= 0
```

That's not a JSON problem.

It's an **application validation** problem.

So:

```text
JSON validity
      ≠
application validity
```

This distinction is why status codes such as `400`, `409`, and `422` can have different roles depending on the API's design.

---

# 12. Let's inspect the actual bytes

Modify our server temporarily:

```python
def do_POST(self):

    content_length = int(
        self.headers.get("Content-Length", 0)
    )

    body = self.rfile.read(content_length)

    print("Raw bytes:", body)
    print("Byte count:", len(body))

    text = body.decode("utf-8")

    print("Decoded text:", text)
    print("Character count:", len(text))
```

Now send:

```bash
curl \
  -X POST \
  -H "Content-Type: text/plain; charset=utf-8" \
  --data "Hello 世界" \
  http://localhost:8080/test
```

You should see the distinction between:

```text
Raw bytes
Byte count
Decoded text
Character count
```

This is a very useful experiment.

---

# 13. What does `charset=utf-8` mean?

You may encounter:

```http
Content-Type: text/plain; charset=utf-8
```

The first part:

```text
text/plain
```

describes the media type.

The parameter:

```text
charset=utf-8
```

provides character encoding information.

Conceptually:

```text
Content-Type
│
├── media type
│     └── text/plain
│
└── parameter
      └── charset=utf-8
```

For JSON, you'll commonly see:

```http
Content-Type: application/json
```

Modern JSON is defined around Unicode/UTF-8 handling, so you generally don't need to add a `charset` parameter to `application/json`.

---

# 14. Why does `curl -d` seem magical?

When you run:

```bash
curl \
  -X POST \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

curl constructs the HTTP request for you.

Conceptually something like:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Length: 16
Content-Type: application/x-www-form-urlencoded

{"name":"Riyaz"}
```

Notice something interesting:

If you use `-d` without specifying a content type, curl commonly uses:

```text
application/x-www-form-urlencoded
```

even though your data happens to look like JSON.

If you actually want JSON, explicitly say:

```bash
curl \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

That's why `Content-Type` matters.

---

# 15. `curl` calculates things for you

When you use:

```bash
-d '{"name":"Riyaz"}'
```

curl can calculate:

```text
Content-Length
```

automatically.

That's convenient.

But when we used `nc`:

```bash
nc localhost 8080
```

we had to manually construct:

```http
Content-Length: 16
```

because `nc` doesn't understand HTTP semantics.

This illustrates the difference beautifully:

```text
nc
 ↓
TCP connection
 ↓
YOU construct HTTP


curl
 ↓
TCP connection
 ↓
curl constructs HTTP
 ↓
YOU provide higher-level intent
```

---

# 16. This is exactly what frameworks do

You can now see the layers behind something like:

```java
@PostMapping("/users")
public User create(@RequestBody CreateUserRequest request) {
    ...
}
```

A framework roughly handles:

```text
TCP
 ↓
HTTP parsing
 ↓
request line
 ↓
headers
 ↓
Content-Length
 ↓
body bytes
 ↓
Content-Type
 ↓
JSON deserialization
 ↓
Java object
 ↓
your method
```

And on the way back:

```text
Java object
 ↓
JSON serialization
 ↓
UTF-8 bytes
 ↓
Content-Length
 ↓
HTTP response
 ↓
TCP
```

What looks like:

```java
return user;
```

can actually involve a substantial amount of HTTP machinery underneath.

---

# 🧠 The big mental model

This is the model I want you to keep:

```text
CLIENT
  │
  │ Application object
  │
  ▼
Serialize
  │
  ▼
JSON
  │
  ▼
UTF-8 encode
  │
  ▼
BYTES
  │
  ▼
HTTP BODY
  │
  ▼
NETWORK
  │
  ▼
HTTP BODY
  │
  ▼
BYTES
  │
  ▼
UTF-8 decode
  │
  ▼
JSON
  │
  ▼
Deserialize
  │
  ▼
SERVER APPLICATION OBJECT
```

And:

```text
Content-Type
     ↓
"What representation are these bytes?"

Content-Length
     ↓
"How many bytes belong to this body?"
```

---

## 🧪 Final experiment

Send Unicode:

```bash
curl \
  -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"世界"}' \
  http://localhost:8080/users
```

Then inspect your server's:

```text
Content-Length
Raw bytes
Decoded text
Character count
```

You should now be able to explain **why `Content-Length` and the number of characters can be different**.

---

### Next → Path Parameters vs Query Parameters

We'll start turning what we've learned into a real API:

```text
GET /users
GET /users/123
GET /users?active=true
GET /users/123/orders
```

We'll answer a very practical backend question:

> **When should data go into the path, and when should it go into the query string?**

And we'll implement both manually in our Python HTTP server.

---

# Lesson 12 — Path Parameters vs Query Parameters

This is an important distinction because you'll use both constantly when building APIs.

---

## 1. Look at these two requests

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
```

and:

```http
GET /users?active=true&page=2 HTTP/1.1
Host: localhost:8080
```

They look similar, but they communicate different things.

### Path parameter

```text
/users/123
       ^^^
       ID
```

Usually means:

> "I want the user identified by `123`."

### Query parameter

```text
/users?active=true&page=2
       ^^^^^^^^^^^^^^^^^
       options/criteria
```

Usually means:

> "I want users, filtered/paginated according to these options."

A useful mental model:

```text
PATH
  → Which resource?

QUERY
  → How should I search/filter/modify the representation?
```

---

# 2. Path parameters

Consider:

```http
GET /users/123
```

Here:

```text
/users     → collection
/123       → particular resource
```

Typical examples:

```text
/users/123
/products/456
/orders/789
/users/123/orders
/users/123/orders/456
```

The path identifies **which resource or sub-resource** you're talking about.

For example:

```http
GET /users/123/orders
```

can mean:

> Get the orders belonging to user 123.

---

# 3. Query parameters

Now consider:

```http
GET /users?active=true
```

The resource is still:

```text
/users
```

The query gives additional criteria:

```text
active=true
```

Other examples:

```text
/users?page=2
/users?limit=20
/users?sort=name
/users?active=true
/users?role=admin
/users?name=riyaz
```

You can combine them:

```http
GET /users?active=true&role=admin&page=2
```

The server can interpret this as:

> Give me users who are active, have the admin role, and are on page 2.

---

# 4. The important conceptual difference

Imagine you have:

```text
/users/123
```

You're identifying a **specific resource**.

But:

```text
/users?role=admin
```

You're asking for a **collection matching some criteria**.

So:

```text
/users/123
```

→ resource identity

while:

```text
/users?role=admin
```

→ resource selection/filtering

This isn't an absolute rule enforced by HTTP. HTTP doesn't say:

> "Query parameters must mean filtering."

That's application-level semantics.

HTTP simply gives us the request target.

Your API defines what the target means.

---

# 5. Let's see what the server actually receives

Modify your Python server:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):

        print("\n--- REQUEST ---")

        print("Raw target:", self.path)

        parsed = urlparse(self.path)

        print("Path:", parsed.path)
        print("Query:", parsed.query)

        params = parse_qs(parsed.query)

        print("Query parameters:", params)

        body = f"""
Path: {parsed.path}
Query: {params}
""".encode()

        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()

        self.wfile.write(body)


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
```

Now:

```bash
curl "http://localhost:8080/users/123"
```

You should see something like:

```text
Raw target: /users/123
Path: /users/123
Query:
Query parameters: {}
```

Now:

```bash
curl "http://localhost:8080/users?active=true&page=2"
```

You'll get:

```text
Raw target: /users?active=true&page=2
Path: /users
Query: active=true&page=2
Query parameters: {'active': ['true'], 'page': ['2']}
```

Notice something important:

```text
/users?active=true&page=2
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        request target

/users
^^^^^^
path

active=true&page=2
^^^^^^^^^^^^^^^^^
query
```

---

# 6. Why does `parse_qs()` return lists?

This is interesting.

Try:

```python
from urllib.parse import parse_qs

print(parse_qs("page=2"))
```

Result:

```python
{'page': ['2']}
```

Why a list?

Because URLs can contain repeated parameters:

```text
/users?id=10&id=20&id=30
```

Then:

```python
parse_qs("id=10&id=20&id=30")
```

gives:

```python
{
    'id': ['10', '20', '30']
}
```

So query parameters are technically capable of having multiple values.

---

# 7. Path parameters aren't automatically understood either

This:

```http
GET /users/123
```

doesn't magically give Python a variable called `id`.

Your application has to interpret:

```text
/users/123
```

For example:

```python
path = "/users/123"

parts = path.strip("/").split("/")

print(parts)
```

Result:

```text
['users', '123']
```

You could then interpret:

```python
resource = parts[0]
user_id = parts[1]
```

giving:

```text
resource = "users"
user_id   = "123"
```

Frameworks such as Spring, Express, FastAPI, etc. automate this routing/parsing.

But underneath, they still received:

```http
GET /users/123 HTTP/1.1
```

---

# 8. A very useful example

Consider an ecommerce API.

### Get product 123

```http
GET /products/123
```

Meaning:

```text
Product identity = 123
```

### Search products

```http
GET /products?category=shoes
```

Meaning:

```text
Resource = products
Filter = category=shoes
```

### Pagination

```http
GET /products?page=2&limit=20
```

### Sorting

```http
GET /products?sort=price&order=asc
```

### Filtering + pagination + sorting

```http
GET /products?category=shoes&minPrice=1000&page=2&limit=20&sort=price
```

And a nested resource:

```http
GET /users/123/orders
```

Meaning:

```text
User = 123
Resource = orders belonging to that user
```

---

# 9. One subtle but important point

Don't think:

> Path = mandatory, Query = optional.

That's not the real distinction.

The deeper distinction is **semantics**.

For example, an API could technically have:

```text
/users?userId=123
```

instead of:

```text
/users/123
```

HTTP doesn't prevent it.

But API designers generally use:

```text
/users/123
```

when `123` identifies the specific user, because the URL structure communicates resource identity more clearly.

---

# 10. One more experiment with `nc`

Start:

```bash
nc localhost 8080
```

Then manually send:

```http
GET /users/123?active=true&page=2 HTTP/1.1
Host: localhost:8080

```

Your server sees one request target:

```text
/users/123?active=true&page=2
```

Python separates it into:

```text
path  = /users/123

query = active=true&page=2
```

So the flow is:

```text
Raw HTTP
   ↓
GET /users/123?active=true&page=2 HTTP/1.1
   ↓
HTTP parser
   ↓
Request target
   ↓
URL parsing
   ├── path  → /users/123
   └── query → active=true&page=2
```

---

## 11. Remember this mental model

When you see:

```text
/users/123/orders?status=paid&page=2
```

break it down as:

```text
/users
   ↓
collection

/123
   ↓
specific user

/orders
   ↓
user's orders

?status=paid
   ↓
filter

&page=2
   ↓
pagination
```

So:

> **Path generally identifies the resource hierarchy. Query parameters generally provide additional instructions/criteria for the request.**

And importantly, **HTTP provides the syntax; your application defines the semantics.**

---

### Next: Lesson 13 — URL Encoding in real APIs

We'll go one level deeper and see why these can behave differently:

```text
/users/John%20Doe
/users?name=John%20Doe
```

and especially why characters like:

```text
/ ? & = # %
```

need special treatment.

---

# Lesson 13 — URL Encoding in Real APIs

We already saw URL encoding briefly. Now let's understand **why it exists**, how it works at the HTTP level, and where it matters in real APIs.

---

## 1. The problem: URLs have special characters

Suppose you want to search for:

```text
John & Sons
```

You might naturally write:

```text
/users?name=John & Sons
```

But `&` has a special meaning in a query string:

```text
/users?name=John&company=Sons
```

Now the server may interpret that as **two parameters**:

```text
name = John
company = Sons
```

But that's not what you meant.

You meant:

```text
name = "John & Sons"
```

So we need a way to distinguish:

> "This character is data."

from:

> "This character is part of URL syntax."

That's what **percent-encoding** solves.

---

# 2. Percent-encoding

The basic format is:

```text
%XX
```

where `XX` is a hexadecimal byte value.

For example:

```text
space → %20
&     → %26
?     → %3F
=     → %3D
#     → %23
%     → %25
+     → %2B
/     → %2F
```

So:

```text
John & Sons
```

can become:

```text
John%20%26%20Sons
```

---

# 3. Try it with curl

Run:

```bash
curl "http://localhost:8080/users?name=John%20%26%20Sons"
```

Your server should see:

```text
Path: /users
Query: name=John%20%26%20Sons
```

Then:

```python
parse_qs("name=John%20%26%20Sons")
```

produces approximately:

```python
{
    "name": ["John & Sons"]
}
```

Notice the two stages:

```text
HTTP request
      ↓
name=John%20%26%20Sons
      ↓
URL/query parsing
      ↓
name = "John & Sons"
```

The encoded representation travels over HTTP.

Your application works with the decoded value.

---

# 4. Why can't we just send spaces?

Consider:

```http
GET /users?name=John Doe HTTP/1.1
Host: localhost:8080
```

The space has special significance in HTTP request syntax.

Remember the request line:

```text
METHOD SP REQUEST-TARGET SP HTTP-VERSION
```

For example:

```text
GET /users HTTP/1.1
```

There are spaces separating:

```text
GET
/users
HTTP/1.1
```

A literal space inside the request target would make parsing ambiguous.

So URL encoding allows us to represent the data safely:

```text
John%20Doe
```

---

# 5. Query parameter example

Suppose your API supports:

```text
GET /users?name=Riyaz
```

No encoding is necessary.

But:

```text
GET /users?name=Riyaz Mohammed
```

should be encoded as:

```text
GET /users?name=Riyaz%20Mohammed
```

Similarly:

```text
GET /search?q=hello world
```

becomes:

```text
GET /search?q=hello%20world
```

---

# 6. `&` is especially important

Consider:

```text
/users?name=John&age=30
```

This means:

```text
name = John
age  = 30
```

Now suppose the name itself contains `&`:

```text
John & Sons
```

We must encode the `&`:

```text
/users?name=John%20%26%20Sons
```

Now the parser knows:

```text
parameter name
        ↓
John%20%26%20Sons
        ↓
John & Sons
```

instead of interpreting `&` as a parameter separator.

---

# 7. `=` has the same idea

Normally:

```text
/users?id=123
```

means:

```text
key = id
value = 123
```

But suppose the actual value is:

```text
a=b
```

You need:

```text
/users?value=a%3Db
```

Otherwise:

```text
value=a=b
```

could become ambiguous depending on the parser/application.

---

# 8. What about `?`

The `?` separates the path from the query:

```text
/users?name=Riyaz
      ^
```

But suppose your search value itself contains `?`:

```text
what?
```

Encode it:

```text
/search?q=what%3F
```

Otherwise the character could be interpreted as URL syntax rather than data.

---

# 9. What about `#`?

This one is particularly interesting.

Consider:

```text
/users/123#profile
```

The `#profile` portion is a **fragment**.

Browsers generally don't send the fragment to the HTTP server.

So the server receives:

```http
GET /users/123 HTTP/1.1
```

not:

```http
GET /users/123#profile HTTP/1.1
```

But if `#` is actually part of your data, it needs to be encoded:

```text
/search?q=C%23
```

Here:

```text
C%23
```

decodes to:

```text
C#
```

This is why searching for C# can produce URLs such as:

```text
/search?q=C%23
```

---

# 10. Python makes this easy

Python provides:

```python
from urllib.parse import quote, unquote
```

Try:

```python
from urllib.parse import quote

value = "John & Sons"

encoded = quote(value)

print(encoded)
```

Result:

```text
John%20%26%20Sons
```

Decode it:

```python
from urllib.parse import unquote

print(unquote("John%20%26%20Sons"))
```

Result:

```text
John & Sons
```

So:

```text
quote()
   ↓
encode

unquote()
   ↓
decode
```

---

# 11. Query parameters have a convenient helper

Instead of manually constructing:

```text
/users?name=John%20%26%20Sons&page=2
```

Python can construct it:

```python
from urllib.parse import urlencode

params = {
    "name": "John & Sons",
    "page": 2
}

query = urlencode(params)

print(query)
```

You'll get:

```text
name=John+%26+Sons&page=2
```

Notice something interesting:

```text
space → +
```

instead of:

```text
space → %20
```

That's because `urlencode()` uses the conventions of **form-style query encoding**.

Both forms commonly represent a space in query/form contexts:

```text
John%20Doe
John+Doe
```

But don't make the mistake of thinking:

> `+` always means space everywhere in a URL.

It doesn't.

---

# 12. Path encoding is slightly different

Consider:

```text
/users/John%20Doe
```

After decoding:

```text
/users/John Doe
```

That's straightforward.

But consider:

```text
/files/a%2Fb
```

`%2F` represents `/`.

So the decoded value is:

```text
a/b
```

This creates an important routing issue.

Compare:

```text
/files/a/b
```

with:

```text
/files/a%2Fb
```

Conceptually:

```text
/files/a/b
       ↓
two path segments

a
b
```

whereas:

```text
/files/a%2Fb
       ↓
one segment whose value is "a/b"
```

Whether a particular server/framework preserves that distinction during routing is implementation-dependent.

This is one reason URL decoding and routing need to be handled carefully.

---

# 13. Encoding happens at the representation boundary

A very useful mental model is:

```text
Application value
      │
      │ encode
      ↓
URL representation
      │
      │ HTTP
      ↓
Server
      │
      │ decode
      ↓
Application value
```

For example:

```text
Application:

name = "John & Sons"

       ↓ encode

URL:

/users?name=John%20%26%20Sons

       ↓ HTTP

Server

       ↓ decode

name = "John & Sons"
```

---

# 14. Hands-on exercise

Use your current server and run these:

### Exercise 1

```bash
curl "http://localhost:8080/search?q=hello%20world"
```

What does Python's `parse_qs()` produce?

---

### Exercise 2

```bash
curl "http://localhost:8080/search?q=John%20%26%20Sons"
```

Expected query value:

```text
John & Sons
```

---

### Exercise 3

Try:

```bash
curl "http://localhost:8080/search?q=C%23"
```

Expected:

```text
C#
```

---

### Exercise 4

Compare:

```bash
curl "http://localhost:8080/search?q=hello+world"
```

and:

```bash
curl "http://localhost:8080/search?q=hello%20world"
```

With `parse_qs()`, both will normally decode to:

```text
hello world
```

because query parsing follows form-style `+` handling.

---

# The key thing to remember

Don't memorize dozens of encoded characters.

Understand the problem:

```text
URL syntax
    +
user data
    ↓
potential ambiguity
    ↓
percent-encoding
```

For example:

```text
&  → %26
?  → %3F
#  → %23
=  → %3D
%  → %25
/  → %2F
```

And the big picture we've learned so far is now:

```text
HTTP request
│
├── Method
│
├── Request target
│   ├── Path
│   └── Query
│
├── Headers
│
├── Blank line
│
└── Body
```

Next we'll move into **REST-style API design with GET**, and connect everything we've learned so far into an actual API endpoint.


---

# Lesson 14 — GET: Reading Resources

Now we start connecting the HTTP pieces we've learned into **real API behavior**.

We've already seen:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
```

But now let's understand what GET actually means, why it exists, and what makes a good GET API.

---

## 1. The problem GET solves

Imagine a client wants information from a server.

The simplest idea would be:

```text
Client → Server
         "Give me user 123"
```

HTTP gives us a standardized way to express that:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

The important part is:

```text
GET
```

It tells the server:

> I want to retrieve the current representation of this resource.

The server might respond:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42

{"id":123,"name":"Riyaz","active":true}
```

---

# 2. GET doesn't mean "database SELECT"

This distinction is important for backend development.

You might internally implement:

```sql
SELECT * FROM users WHERE id = 123;
```

But HTTP doesn't know anything about SQL.

HTTP only knows:

```http
GET /users/123
```

Your application could retrieve the data from:

```text
PostgreSQL
Redis
MongoDB
another API
a file
memory
```

HTTP doesn't care.

So think:

```text
HTTP
  ↓
GET /users/123
  ↓
Application
  ↓
Whatever mechanism is necessary
  ↓
Representation of user
```

---

# 3. GET normally has no request body

A typical GET request is:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Accept: application/json

```

Notice:

```text
headers
   ↓
blank line
   ↓
no body
```

You *can* encounter GET requests with bodies in some systems, but their semantics and interoperability are problematic/undefined enough that you shouldn't design ordinary APIs around GET request bodies.

For normal API design:

> Put GET inputs in the request target — path and/or query parameters.

For example:

```http
GET /users/123
```

or:

```http
GET /users?active=true&page=2
```

---

# 4. GET collection vs individual resource

A very common pattern is:

### Collection

```http
GET /users
```

Meaning:

> Retrieve a representation of the users collection.

### Individual resource

```http
GET /users/123
```

Meaning:

> Retrieve a representation of user 123.

These are different resources.

```text
/users
   │
   ├── /123
   ├── /456
   └── /789
```

You can think of it as:

```text
/users       → collection
/users/123   → one member
```

---

# 5. Query parameters with GET

Suppose there are millions of users.

You probably don't want:

```http
GET /users
```

to return all of them.

You can provide query parameters:

```http
GET /users?page=2&limit=20
```

Or filtering:

```http
GET /users?active=true
```

Or searching:

```http
GET /users?name=Riyaz
```

Or sorting:

```http
GET /users?sort=name&order=asc
```

The important idea:

```text
Path
 ↓
Which resource?

Query
 ↓
Which subset / representation / retrieval options?
```

---

# 6. Let's build this into our Python server

Use:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs
import json


class Handler(BaseHTTPRequestHandler):

    def do_GET(self):

        parsed = urlparse(self.path)

        path = parsed.path
        params = parse_qs(parsed.query)

        # GET /users
        if path == "/users":
            users = [
                {"id": 1, "name": "Alice"},
                {"id": 2, "name": "Bob"},
                {"id": 3, "name": "Riyaz"},
            ]

            body = json.dumps(users).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()

            self.wfile.write(body)
            return

        # Anything else
        self.send_response(404)
        self.end_headers()


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
```

Now:

```bash
curl -i http://localhost:8080/users
```

You should receive:

```http
HTTP/1.0 200 OK
Content-Type: application/json
Content-Length: ...

[{"id": 1, "name": "Alice"}, ...]
```

---

# 7. Add `/users/{id}`

Now let's make the server understand:

```http
GET /users/2
```

Add:

```python
if path.startswith("/users/"):

    user_id = path.split("/")[-1]

    users = {
        "1": {"id": 1, "name": "Alice"},
        "2": {"id": 2, "name": "Bob"},
        "3": {"id": 3, "name": "Riyaz"},
    }

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
```

Now:

```bash
curl -i http://localhost:8080/users/2
```

returns something like:

```json
{"id": 2, "name": "Bob"}
```

But:

```bash
curl -i http://localhost:8080/users/999
```

returns:

```http
404 Not Found
```

That's a real API behavior:

```text
GET /users/2
     ↓
Does resource exist?
     ↓
YES → 200

GET /users/999
     ↓
Does resource exist?
     ↓
NO → 404
```

---

# 8. GET is safe

HTTP defines GET as a **safe** method.

That means the request semantics are intended for retrieval rather than asking the server to change state.

For example:

```http
GET /users/123
```

shouldn't mean:

```text
delete user 123
```

or:

```text
change user's name
```

The server may still have incidental side effects.

For example, processing a GET might:

```text
write logs
update metrics
populate a cache
```

That's okay.

"Safe" doesn't mean:

> absolutely nothing anywhere on the server changes.

It means:

> The client isn't requesting a state-changing operation through the method's defined semantics.

---

# 9. GET is idempotent

GET is also **idempotent**.

Suppose:

```http
GET /users/123
```

is sent:

```text
once
twice
100 times
```

The intended effect on the resource is the same: retrieve it.

Compare that with:

```http
POST /users
```

which may create a new user each time.

So:

```text
GET
 ├── safe
 └── idempotent

POST
 ├── not safe
 └── generally not idempotent
```

We'll explore POST next.

---

# 10. GET and caching

Because GET is intended for retrieval and is safe, GET responses are particularly suitable for HTTP caching.

For example:

```http
GET /products/123
```

could return:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: max-age=60

{"id":123,"name":"Laptop"}
```

A cache could potentially reuse the response for the permitted period rather than contacting the server every time.

This eventually leads to:

```text
GET
 ↓
Cache
 ↓
Server
```

instead of always:

```text
GET
 ↓
Server
```

We'll study caching properly later.

---

# 11. A common API mistake

Don't do this:

```http
GET /deleteUser?id=123
```

with the expectation that it deletes the user.

Why?

Because you're using GET semantics for a state-changing operation.

Instead:

```http
DELETE /users/123
```

communicates the operation through the HTTP method.

Similarly, avoid:

```http
GET /createUser?name=Riyaz
```

Prefer:

```http
POST /users
```

This separation is one of the foundations of REST-style API design.

---

# 12. GET request anatomy

Let's put everything we've learned together:

```http
GET /users/123?details=true HTTP/1.1
Host: localhost:8080
Accept: application/json
User-Agent: curl/...

```

Breakdown:

```text
GET
 ↓
method

/users/123
 ↓
path

?details=true
 ↓
query

HTTP/1.1
 ↓
HTTP version

Host
 ↓
target host

Accept
 ↓
desired response representation

blank line
 ↓
end of headers

(no body)
```

And the response:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 42

{"id":123,"name":"Riyaz","active":true}
```

Breakdown:

```text
200
 ↓
request succeeded

Content-Type
 ↓
body is JSON

Content-Length
 ↓
body contains N bytes

blank line
 ↓
headers finished

JSON
 ↓
response body
```

---

# 13. Your mental model now

You can now look at:

```http
GET /users/123?active=true HTTP/1.1
Host: api.example.com
Accept: application/json
```

and understand the whole thing:

```text
                  HTTP REQUEST
                       │
                       ▼
                    GET
                       │
            ┌──────────┴──────────┐
            ▼                     ▼
       /users/123            active=true
            │                     │
            ▼                     ▼
      resource identity       query criteria
            │
            └──────────┬──────────┘
                       ▼
                   HTTP server
                       │
                       ▼
                  application
                       │
                       ▼
                  JSON response
```

---

## Exercise

Before moving on, try these manually with `curl`:

```bash
curl -i http://localhost:8080/users
```

```bash
curl -i http://localhost:8080/users/1
```

```bash
curl -i http://localhost:8080/users/999
```

```bash
curl -v "http://localhost:8080/users?active=true&page=2"
```

Then use:

```bash
nc localhost 8080
```

and manually send:

```http
GET /users/2 HTTP/1.1
Host: localhost:8080
Accept: application/json

```

At this point, you're no longer just learning syntax—you've manually constructed a real HTTP request and built a server that interprets it.

**Next: Lesson 15 — POST: Sending Data to the Server**, where we'll follow the complete journey:

```text
JSON object
   ↓
JSON serialization
   ↓
UTF-8 bytes
   ↓
HTTP request body
   ↓
Content-Type
   ↓
Content-Length
   ↓
server
   ↓
JSON parsing
   ↓
create resource
   ↓
201 Created
```

---

# Lesson 15 — POST: Sending Data to the Server

Now we move from:

```text
GET → "Give me something"
```

to:

```text
POST → "Here is some data; process it."
```

This is where HTTP starts becoming much more interesting because we're combining:

* method
* URL
* headers
* body
* Content-Type
* Content-Length
* JSON
* status codes

---

# 1. The problem POST solves

Suppose a client wants to create a user.

With GET, you might be tempted to do:

```http
GET /create-user?name=Riyaz&email=riyaz@example.com
```

But that's a poor fit.

We want a request that says:

> I'm submitting some data to the server.

HTTP provides:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{"name":"Riyaz","email":"riyaz@example.com"}
```

Notice the important difference from GET:

```text
GET
 ↓
usually no request body

POST
 ↓
request body commonly carries submitted data
```

---

# 2. POST doesn't mean "insert into database"

Just like GET doesn't mean SQL `SELECT`, POST doesn't literally mean SQL `INSERT`.

HTTP defines POST around submitting a representation to a resource for processing.

For an API, one common use is:

```http
POST /users
```

→ create a new user.

But POST can also be used for other operations such as:

```text
POST /orders
POST /payments
POST /search
POST /users/123/reset-password
```

The application defines the exact semantics.

---

# 3. Let's construct the request

Suppose we want to create:

```json
{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

The HTTP request could be:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 46

{"name":"Riyaz","email":"riyaz@example.com"}
```

There are several layers here.

### Method

```text
POST
```

### Target

```text
/users
```

### Content-Type

```text
application/json
```

Tells the server:

> Interpret the body as JSON.

### Content-Length

```text
46
```

Tells the server:

> Read 46 bytes for the body.

### Body

```json
{"name":"Riyaz","email":"riyaz@example.com"}
```

---

# 4. What actually happens on the wire?

Let's slow this down.

Your Python object might start as:

```python
user = {
    "name": "Riyaz",
    "email": "riyaz@example.com"
}
```

That's an application object.

It can't simply be sent directly over HTTP.

First:

```text
Python object
      ↓
JSON serialization
      ↓
'{"name":"Riyaz","email":"riyaz@example.com"}'
      ↓
UTF-8 encoding
      ↓
bytes
      ↓
HTTP request body
```

So:

```python
json.dumps(user)
```

produces JSON text.

Then:

```python
.encode("utf-8")
```

produces bytes.

Those bytes are what actually go into the HTTP body.

---

# 5. Let's use curl

Run:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz","email":"riyaz@example.com"}' \
  http://localhost:8080/users
```

Curl constructs the HTTP request for you.

Conceptually it sends:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{"name":"Riyaz","email":"riyaz@example.com"}
```

This is why `curl` is so useful for learning HTTP.

You're not hiding the HTTP concepts behind Postman or a framework.

---

# 6. Let's build the POST endpoint

Modify the server:

```python
from http.server import BaseHTTPRequestHandler, HTTPServer
import json


class Handler(BaseHTTPRequestHandler):

    def do_POST(self):

        print("\n--- POST REQUEST ---")

        print("Method:", self.command)
        print("Path:", self.path)

        content_type = self.headers.get("Content-Type")
        content_length = int(
            self.headers.get("Content-Length", 0)
        )

        print("Content-Type:", content_type)
        print("Content-Length:", content_length)

        # Read exactly Content-Length bytes
        body = self.rfile.read(content_length)

        print("Raw body:", body)

        # Convert bytes → string
        text = body.decode("utf-8")

        print("Body text:", text)

        # Convert JSON → Python object
        data = json.loads(text)

        print("Parsed JSON:", data)

        response = json.dumps({
            "message": "User created",
            "user": data
        }).encode("utf-8")

        self.send_response(201)

        self.send_header(
            "Content-Type",
            "application/json"
        )

        self.send_header(
            "Content-Length",
            str(len(response))
        )

        self.end_headers()

        self.wfile.write(response)


server = HTTPServer(("localhost", 8080), Handler)

print("Server running on http://localhost:8080")

server.serve_forever()
```

---

# 7. Now follow the request through the server

When you send:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 46

{"name":"Riyaz","email":"riyaz@example.com"}
```

the server does approximately:

```text
HTTP request
     │
     ▼
HTTP parser
     │
     ├── method = POST
     ├── path = /users
     ├── headers
     └── body
            │
            ▼
      Content-Length
            │
            ▼
       read N bytes
            │
            ▼
          bytes
            │
            ▼
      UTF-8 decoding
            │
            ▼
          string
            │
            ▼
       JSON parsing
            │
            ▼
      Python dictionary
```

That final dictionary is what your application can work with.

---

# 8. Why Content-Type matters

Suppose the client sends:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"name":"Riyaz"}
```

The server knows:

```text
body representation = JSON
```

So it can do:

```python
json.loads(...)
```

But imagine:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: text/plain

{"name":"Riyaz"}
```

The bytes happen to look like JSON.

But the client has explicitly said:

```text
Content-Type: text/plain
```

So the server shouldn't blindly assume it's JSON just because the content *looks* like JSON.

This is an important principle:

> **Content-Type describes the representation of the body.**

---

# 9. What if Content-Length is wrong?

This is one of the most useful experiments from our earlier lessons.

Suppose the actual body is:

```text
Hello
```

That's 5 bytes in ASCII/UTF-8.

But you claim:

```http
Content-Length: 100
```

The server tries:

```python
self.rfile.read(100)
```

It may wait for more bytes because it was told that the body is 100 bytes long.

Conversely, if you say:

```http
Content-Length: 2
```

the server reads only:

```text
He
```

The remaining bytes can remain unread and interfere with subsequent parsing on a persistent connection.

So:

```text
Content-Length
       ↓
framing information
       ↓
where does the body end?
```

This is much more fundamental than just metadata.

---

# 10. Why does POST commonly return 201?

If the POST creates a resource, a common response is:

```http
HTTP/1.1 201 Created
```

For example:

```http
HTTP/1.1 201 Created
Content-Type: application/json
Content-Length: ...

{"id":123,"name":"Riyaz"}
```

Compare:

```text
200 OK
```

with:

```text
201 Created
```

`200` means:

> The request succeeded.

`201` communicates something more specific:

> The request succeeded and resulted in a new resource being created.

---

# 11. Location header

A particularly useful header with `201 Created` is:

```http
Location: /users/123
```

For example:

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /users/123
Content-Length: 35

{"id":123,"name":"Riyaz"}
```

Now the response communicates:

```text
POST /users
     │
     ▼
created user
     │
     ▼
/users/123
```

The client knows where the newly created resource can be retrieved.

Then:

```http
GET /users/123
```

can retrieve it.

This creates a nice HTTP flow:

```text
POST /users
     │
     │ create
     ▼
201 Created
Location: /users/123
     │
     ▼
GET /users/123
```

---

# 12. POST is generally not idempotent

Suppose:

```http
POST /users

{"name":"Riyaz"}
```

creates user `101`.

Send the same request again:

```http
POST /users

{"name":"Riyaz"}
```

It could create user `102`.

Again:

```http
POST /users
```

could create user `103`.

So:

```text
POST
  ↓
same request repeated
  ↓
potentially multiple effects
```

That's why POST is generally **not idempotent**.

This becomes extremely important when dealing with:

* network retries
* payment APIs
* order creation
* message submission
* distributed systems

Later we'll see how **idempotency keys** can address some of these problems.

---

# 13. POST vs GET

Put them side by side.

### GET

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Accept: application/json

```

Conceptually:

```text
"I want this resource."
```

### POST

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"name":"Riyaz"}
```

Conceptually:

```text
"Here is some data. Process it against this resource."
```

Notice:

```text
GET
 ↓
input often in URL

POST
 ↓
input commonly in body
```

---

# 14. Error handling

Real APIs also need to handle invalid requests.

Suppose the client sends:

```json
{"name": "Riyaz"}
```

but your application requires an email.

You might return:

```http
HTTP/1.1 422 Unprocessable Content
Content-Type: application/json

{
  "error": "email is required"
}
```

Or your API might choose `400 Bad Request`.

The exact API error convention varies, but the important concept is:

```text
HTTP parsing
     ↓
valid HTTP?
     │
     ├── no → HTTP-level error
     │
     ▼
JSON parsing
     │
     ├── invalid JSON → client error
     │
     ▼
validation
     │
     ├── invalid data → client error
     │
     ▼
business logic
     │
     ▼
resource creation
```

There are multiple layers of failure.

---

# 15. Try malformed JSON

Run:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name": "Riyaz"' \
  http://localhost:8080/users
```

This JSON is missing:

```text
}
```

So:

```python
json.loads(text)
```

will raise an exception.

Our tiny server will currently produce an ugly server error.

That's intentional for the learning exercise.

Later we'll improve it to return a proper:

```http
400 Bad Request
```

instead of crashing.

---

# 16. The complete POST pipeline

This is the part I want you to remember:

```text
                 CLIENT
                   │
                   │ Python object
                   ▼
              JSON serialize
                   │
                   ▼
              UTF-8 encode
                   │
                   ▼
            HTTP request body
                   │
                   │
                   ▼
        ┌─────────────────────┐
        │     HTTP SERVER      │
        └─────────────────────┘
                   │
                   ▼
             read headers
                   │
                   ▼
           Content-Length
                   │
                   ▼
             read N bytes
                   │
                   ▼
            UTF-8 decode
                   │
                   ▼
              JSON parse
                   │
                   ▼
          application object
                   │
                   ▼
            business logic
                   │
                   ▼
            create resource
                   │
                   ▼
             HTTP response
                   │
                   ▼
              201 Created
```

This is basically the foundation underneath what Spring, Express, FastAPI, etc. eventually automate for you.

---

## Hands-on challenge

Without looking at the answer, try to design the request for:

> Create a product named `MacBook Pro` with price `1999`.

You should produce something like:

```http
POST /products HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ???

{"name":"MacBook Pro","price":1999}
```

Then send it with:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"MacBook Pro","price":1999}' \
  http://localhost:8080/products
```

And ask yourself:

1. What is the **method**?
2. What is the **request target**?
3. Which headers are present?
4. Where does the body begin?
5. Who determines the body's interpretation?
6. Who determines how many bytes to read?
7. Why is `201` appropriate when creation succeeds?
8. Why might repeating the POST create multiple resources?

---

### Next: Lesson 16 — PUT

We'll compare:

```http
POST /users
```

with:

```http
PUT /users/123
```

and answer the deceptively important question:

> **Why does PUT exist if POST can also send data to the server?**

That's where **replacement semantics and idempotency** become much clearer.

---

# Lesson 16 — PUT: Replacing a Resource

Now we have:

```text
GET
 → retrieve

POST
 → submit/process, commonly create

PUT
 → replace/update a resource at a known target
```

The interesting question is:

> **Why do we need PUT if POST can also send data?**

The answer is mainly about **semantics, resource identity, and idempotency**.

---

# 1. Start with the difference

Suppose we have:

```http
POST /users
```

The client says:

> "Process this new user."

The server typically decides the new user's identity:

```text
POST /users
      ↓
server creates ID
      ↓
/users/123
```

But with:

```http
PUT /users/123
```

the client is saying:

> "Store this representation at this specific resource."

The resource identity is already known:

```text
/users/123
       ↑
    known target
```

That's the fundamental difference.

---

# 2. Example

Suppose the current user is:

```json
{
  "id": 123,
  "name": "Riyaz",
  "email": "riyaz@example.com",
  "active": true
}
```

You want to replace it with:

```json
{
  "id": 123,
  "name": "Riyaz Mohammed",
  "email": "riyaz@example.com",
  "active": true
}
```

You could send:

```http
PUT /users/123 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{
  "id": 123,
  "name": "Riyaz Mohammed",
  "email": "riyaz@example.com",
  "active": true
}
```

Notice:

```text
PUT
 ↓
/users/123
 ↓
specific target
 ↓
new representation
```

---

# 3. Why not POST?

Compare:

```http
POST /users
```

with:

```http
PUT /users/123
```

### POST

```text
POST /users
      ↓
server chooses/assigns identity
      ↓
new resource
```

### PUT

```text
PUT /users/123
          ↓
client specifies target
          ↓
replace/create representation at that target
```

The server isn't choosing which resource the PUT is targeting.

The URL already identifies it.

---

# 4. The most important property: idempotency

PUT is **idempotent**.

Suppose:

```http
PUT /users/123

{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

You send it once.

The resource becomes:

```json
{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

Send exactly the same request again:

```http
PUT /users/123

{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

The intended final state is still:

```json
{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

Send it 100 times:

```text
PUT /users/123
PUT /users/123
PUT /users/123
...
```

The intended resource state remains the same.

That's idempotency.

---

# 5. Compare that with POST

Suppose:

```http
POST /users

{
  "name": "Riyaz"
}
```

Server creates:

```text
/users/123
```

Send it again:

```http
POST /users

{
  "name": "Riyaz"
}
```

Server might create:

```text
/users/124
```

Again:

```text
/users/125
```

So:

```text
POST
 ↓
repeating the request can create additional resources
```

while:

```text
PUT /users/123
 ↓
repeating the same request targets the same resource
```

This distinction becomes extremely important when networks fail and clients retry requests.

---

# 6. "Replace" is important

PUT is commonly described as:

> Replace the current representation of the target resource with the supplied representation.

Suppose the current resource is:

```json
{
  "name": "Riyaz",
  "email": "riyaz@example.com",
  "age": 30
}
```

You send:

```http
PUT /users/123

{
  "name": "Riyaz",
  "email": "new@example.com"
}
```

Conceptually, PUT says:

> This is the representation I want at `/users/123`.

So after replacement, depending on your API's representation model, `age` may no longer be present.

```json
{
  "name": "Riyaz",
  "email": "new@example.com"
}
```

This is why PUT and PATCH should not be casually treated as synonyms.

We'll study PATCH next.

---

# 7. PUT does not necessarily mean "update"

This is a subtle but important point.

PUT can potentially **create** a resource if the target resource doesn't exist and the server permits creation at that URI.

For example:

```http
PUT /users/123

{
  "name": "Riyaz"
}
```

If `/users/123` doesn't exist, the server could create it.

The response might be:

```http
201 Created
```

If it existed and was replaced:

```http
200 OK
```

or:

```http
204 No Content
```

could be appropriate.

So don't memorize:

```text
PUT = UPDATE
```

Instead think:

```text
PUT = PUT this representation at this target
```

That's much closer to the actual HTTP semantics.

---

# 8. Let's implement PUT

Add this to our Python server:

```python
def do_PUT(self):

    parsed = urlparse(self.path)
    path = parsed.path

    if not path.startswith("/users/"):
        self.send_response(404)
        self.end_headers()
        return

    user_id = path.split("/")[-1]

    content_length = int(
        self.headers.get("Content-Length", 0)
    )

    body = self.rfile.read(content_length)

    try:
        data = json.loads(body.decode("utf-8"))
    except json.JSONDecodeError:

        response = b'{"error":"Invalid JSON"}'

        self.send_response(400)
        self.send_header(
            "Content-Type",
            "application/json"
        )
        self.send_header(
            "Content-Length",
            str(len(response))
        )
        self.end_headers()

        self.wfile.write(response)
        return

    print("User ID:", user_id)
    print("Replacement:", data)

    response = json.dumps({
        "id": user_id,
        **data
    }).encode("utf-8")

    self.send_response(200)

    self.send_header(
        "Content-Type",
        "application/json"
    )

    self.send_header(
        "Content-Length",
        str(len(response))
    )

    self.end_headers()

    self.wfile.write(response)
```

---

# 9. Test it

Run:

```bash
curl -v \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz Mohammed","email":"riyaz@example.com"}' \
  http://localhost:8080/users/123
```

Conceptually:

```text
curl
 ↓
PUT /users/123
 ↓
JSON body
 ↓
server
 ↓
parse JSON
 ↓
identify resource 123
 ↓
replace/update representation
 ↓
200 OK
```

---

# 10. PUT vs POST through resource identity

This is perhaps the easiest way to remember the difference.

### POST

```http
POST /users
```

The target is:

```text
/users
```

The server processes the submission and may create:

```text
/users/123
```

The server commonly assigns the identity.

---

### PUT

```http
PUT /users/123
```

The target itself is:

```text
/users/123
```

The client already knows the resource identity.

---

Visualize:

```text
POST

/users
  │
  │ "create/process this"
  ▼
server
  │
  └────→ /users/123


PUT

/users/123
     │
     │ "put this representation here"
     ▼
  resource 123
```

---

# 11. PUT and retries

This is where HTTP semantics become useful in distributed systems.

Imagine:

```text
Client
   │
   │ PUT /users/123
   ▼
Server
   │
   │ updates user
   ▼
response
```

But the response gets lost:

```text
Client
   │
   │ PUT /users/123
   ▼
Server
   │
   │ update succeeded
   │
   X──── response lost
```

The client doesn't know whether the operation succeeded.

It can retry:

```text
Client
   │
   │ PUT /users/123
   ▼
Server
```

Because PUT is idempotent, repeating the same intended operation should not create another user or another independent update effect on the resource.

This doesn't mean retries are always free of all side effects—logging, timestamps, counters, notifications, or poorly designed application behavior can complicate things.

But the **HTTP method semantics** provide an idempotent contract for the requested resource state.

---

# 12. PUT does not mean "send only changed fields"

Suppose you only want to change:

```json
{
  "name": "New Name"
}
```

With PUT, that can be problematic if your API interprets PUT as replacement.

You might accidentally replace:

```json
{
  "name": "Old Name",
  "email": "riyaz@example.com",
  "active": true
}
```

with:

```json
{
  "name": "New Name"
}
```

Now `email` and `active` may disappear.

If your intention is:

> Change only this field.

that's where **PATCH** becomes useful.

---

# 13. GET → POST → PUT

At this point, you should see a progression:

### Retrieve

```http
GET /users/123
```

```text
"Give me user 123."
```

### Create/process

```http
POST /users
```

```text
"Process this new submission against /users."
```

### Replace

```http
PUT /users/123
```

```text
"Use this representation for resource 123."
```

And the important properties:

| Method | Typical API use           | Safe | Idempotent |
| ------ | ------------------------- | ---: | ---------: |
| GET    | Retrieve                  |  Yes |        Yes |
| POST   | Create/process submission |   No |         No |
| PUT    | Replace target resource   |   No |        Yes |

These properties come from HTTP semantics, not from Spring or REST frameworks.

---

# 14. One final experiment

Send the exact same request twice:

```bash
curl -v \
  -X PUT \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users/123
```

Run it again.

Then compare with:

```bash
curl -v \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

Run that multiple times too.

The interesting part isn't simply what our toy server prints.

Ask:

> **What resource does each request target, and what should happen if the exact same request arrives again?**

That's the core of understanding PUT.

---

## Next: Lesson 17 — PATCH

We'll answer:

> If PUT replaces the resource, how do I change **only one field**?

We'll build:

```http
PATCH /users/123

{
  "name": "New Name"
}
```

and then dig into the important difference:

```text
PUT   → replacement semantics
PATCH → partial modification semantics
```

including why **PATCH is not automatically idempotent**.

----

# Lesson 17 — PATCH: Partial Modification

We now have:

```text
GET    → retrieve
POST   → submit/create
PUT    → replace
PATCH  → partially modify
```

PATCH exists because sometimes you **don't want to replace the entire resource**.

---

## 1. The problem with PUT

Suppose our user currently looks like:

```json
{
  "id": 123,
  "name": "Riyaz",
  "email": "riyaz@example.com",
  "active": true
}
```

We only want to change the name.

With PUT, we'd typically send the complete replacement representation:

```http
PUT /users/123
Content-Type: application/json

{
  "id": 123,
  "name": "Riyaz Mohammed",
  "email": "riyaz@example.com",
  "active": true
}
```

But that's unnecessarily large if all we want to change is:

```text
name
```

PATCH provides a way to express a **partial modification**:

```http
PATCH /users/123
Content-Type: application/json

{
  "name": "Riyaz Mohammed"
}
```

---

# 2. The mental model

Think about the difference like this:

### PUT

> "Here is the representation I want at this resource."

```text
Current:

{
  name: "Riyaz",
  email: "old@example.com",
  active: true
}

        PUT

{
  name: "Riyaz Mohammed",
  email: "new@example.com",
  active: true
}
```

The supplied representation represents the desired replacement.

---

### PATCH

> "Apply this modification to the existing resource."

```text
Current:

{
  name: "Riyaz",
  email: "old@example.com",
  active: true
}

        PATCH

{
  name: "Riyaz Mohammed"
}

        ↓

Result:

{
  name: "Riyaz Mohammed",
  email: "old@example.com",
  active: true
}
```

The unspecified fields remain unchanged.

---

# 3. PATCH is not "PUT but smaller"

This is an important distinction.

You shouldn't define PATCH simply as:

> PUT with fewer fields.

PATCH has different semantics.

PUT generally provides a **replacement representation**.

PATCH provides a **set of modifications/instructions** to apply to the target resource.

For example:

```http
PATCH /users/123

{
  "name": "Riyaz"
}
```

Your application might interpret that as:

```text
change name → Riyaz
```

Another PATCH format could represent an explicit operation:

```json
[
  {
    "op": "replace",
    "path": "/name",
    "value": "Riyaz"
  }
]
```

This is associated with **JSON Patch**.

So PATCH itself doesn't require one particular JSON structure.

The server and API define what patch document format is accepted.

---

# 4. A simple PATCH API

Let's add PATCH to our Python server.

```python
def do_PATCH(self):

    parsed = urlparse(self.path)
    path = parsed.path

    if not path.startswith("/users/"):
        self.send_response(404)
        self.end_headers()
        return

    user_id = path.split("/")[-1]

    content_type = self.headers.get("Content-Type")

    if content_type != "application/json":
        self.send_response(415)
        self.end_headers()
        return

    content_length = int(
        self.headers.get("Content-Length", 0)
    )

    body = self.rfile.read(content_length)

    try:
        patch = json.loads(body.decode("utf-8"))
    except json.JSONDecodeError:

        response = b'{"error":"Invalid JSON"}'

        self.send_response(400)
        self.send_header(
            "Content-Type",
            "application/json"
        )
        self.send_header(
            "Content-Length",
            str(len(response))
        )
        self.end_headers()

        self.wfile.write(response)
        return

    print("User:", user_id)
    print("Patch:", patch)

    response = json.dumps({
        "id": user_id,
        "message": "User partially updated",
        "changes": patch
    }).encode("utf-8")

    self.send_response(200)

    self.send_header(
        "Content-Type",
        "application/json"
    )

    self.send_header(
        "Content-Length",
        str(len(response))
    )

    self.end_headers()

    self.wfile.write(response)
```

---

# 5. Test it

Run:

```bash
curl -v \
  -X PATCH \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz Mohammed"}' \
  http://localhost:8080/users/123
```

The request is:

```http
PATCH /users/123 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{"name":"Riyaz Mohammed"}
```

Notice:

```text
/users/123
     ↑
target resource

{"name":"Riyaz Mohammed"}
     ↑
modification
```

---

# 6. PATCH can modify multiple fields

You aren't limited to one field.

```http
PATCH /users/123
Content-Type: application/json

{
  "name": "Riyaz Mohammed",
  "active": false
}
```

Conceptually:

```text
existing resource
       │
       ▼
apply name change
       │
       ▼
apply active change
       │
       ▼
updated resource
```

Fields that aren't included remain unchanged, assuming that's how your patch format is defined.

---

# 7. PATCH and `null`

Here's a subtle API-design question.

Suppose:

```json
{
  "name": null
}
```

Does that mean:

```text
set name to null
```

or:

```text
remove name
```

Usually, for a simple JSON merge-style PATCH design:

```text
field absent
    → don't change it

field present with null
    → set it to null
```

But this is an **API/application convention**, not something you should assume universally.

For example:

```json
{}
```

might mean:

> Make no changes.

While:

```json
{
  "email": null
}
```

might mean:

> Clear the email.

This is one reason patch formats need to be documented precisely.

---

# 8. Is PATCH idempotent?

Here's where things get interesting.

**PATCH is not inherently idempotent.**

That means the HTTP method itself does not guarantee that repeating the same PATCH has the same intended effect.

Consider this PATCH:

```http
PATCH /users/123

{
  "loginCountIncrement": 1
}
```

If your API interprets it as:

```text
loginCount = loginCount + 1
```

then:

```text
PATCH once
→ +1

PATCH twice
→ +2

PATCH three times
→ +3
```

Clearly not idempotent.

---

# 9. But PATCH can be idempotent

You could design:

```http
PATCH /users/123

{
  "name": "Riyaz Mohammed"
}
```

as:

```text
name = "Riyaz Mohammed"
```

Repeatedly applying it:

```text
name = "Riyaz Mohammed"
name = "Riyaz Mohammed"
name = "Riyaz Mohammed"
```

produces the same final state.

So this particular PATCH operation can be idempotent.

But:

> PATCH doesn't guarantee idempotency by definition.

That's different from PUT.

---

# 10. PUT vs PATCH

This table is worth remembering:

|                               | PUT                                        | PATCH                        |
| ----------------------------- | ------------------------------------------ | ---------------------------- |
| Target                        | Specific resource                          | Specific resource            |
| Typical meaning               | Replace representation                     | Apply partial modification   |
| Body                          | Usually complete representation            | Modification/patch document  |
| Can create?                   | Potentially, depending on target semantics | Not generally used as create |
| Idempotent by HTTP semantics? | Yes                                        | Not inherently               |
| Typical example               | Replace user                               | Change user's email          |

Example PUT:

```http
PUT /users/123

{
  "name": "Riyaz",
  "email": "new@example.com",
  "active": true
}
```

Example PATCH:

```http
PATCH /users/123

{
  "email": "new@example.com"
}
```

---

# 11. A very common real-world API pattern

Imagine a user profile:

```json
{
  "id": 123,
  "name": "Riyaz",
  "email": "riyaz@example.com",
  "phone": "1234567890",
  "active": true
}
```

### Change everything

```http
PUT /users/123
```

with the complete representation.

### Change only phone

```http
PATCH /users/123

{
  "phone": "9876543210"
}
```

### Change only active state

```http
PATCH /users/123

{
  "active": false
}
```

### Retrieve

```http
GET /users/123
```

Now our little API has a coherent set of operations:

```text
             /users/123
                  │
       ┌──────────┼──────────┐
       │          │          │
      GET        PUT       PATCH
       │          │          │
    retrieve    replace    modify
```

---

# 12. Where does DELETE fit?

We're almost done with the basic CRUD-style HTTP methods.

The natural next operation is:

```http
DELETE /users/123
```

which communicates:

> Delete the resource identified by `/users/123`.

A successful response might be:

```http
HTTP/1.1 204 No Content
```

Notice something interesting:

```text
DELETE /users/123
```

is also **idempotent**.

If the user is deleted:

```text
first DELETE
→ user deleted

second DELETE
→ user is already gone
```

The second request doesn't undo the first deletion.

However, the server may return different status codes on subsequent attempts—for example `404`—without violating the method's idempotency.

That's a subtle but very important distinction:

> **Idempotent does not mean every response must be identical.**

It concerns the intended effect on the resource state.

---

# 13. The CRUD mapping you've built

You can now visualize a typical API:

```text
GET /users
    ↓
List users

GET /users/123
    ↓
Get user 123

POST /users
    ↓
Create a user

PUT /users/123
    ↓
Replace user 123

PATCH /users/123
    ↓
Partially modify user 123

DELETE /users/123
    ↓
Delete user 123
```

This is the foundation of the REST-style API we'll build.

But remember:

> REST is much broader than simply mapping CRUD operations to HTTP methods.

We're intentionally starting with the HTTP semantics first.

---

## One important interview question

Suppose someone asks:

> **"What's the difference between PUT and PATCH?"**

A strong answer is:

> **PUT is used to replace the representation of a resource at a known target, and it is idempotent. PATCH is used to apply partial modifications to a resource; it is not inherently idempotent because the effect depends on the patch operation.**

That's much better than:

> "PUT updates everything and PATCH updates one field."

The second answer is a useful beginner shortcut, but it isn't the full HTTP semantics.

---

## Next: Lesson 18 — DELETE

We'll implement:

```http
DELETE /users/123
```

and explore a surprisingly interesting question:

> **If the resource is already deleted, why can a second DELETE still be considered idempotent?**

Then we'll have covered the core HTTP methods and move into **authentication headers and Bearer tokens**.

---

# Lesson 18 — DELETE: Removing a Resource

We've seen:

```text
GET     → retrieve
POST    → submit/create
PUT     → replace
PATCH   → partially modify
DELETE  → remove
```

Now let's understand **DELETE properly**, especially one concept that often confuses people:

> **How can DELETE be idempotent if the second DELETE might return 404?**

---

## 1. What problem does DELETE solve?

Suppose our API has:

```text
/users/123
```

and user `123` exists.

We want to tell the server:

> Remove this resource.

HTTP gives us the `DELETE` method for exactly this purpose.

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080

```

Notice something important:

**There is no request body.**

For a simple delete, the target itself tells the server what should be removed.

---

# 2. What does DELETE actually mean?

Conceptually:

```text
DELETE /users/123
       ↓
"Remove the resource identified by this target"
```

But don't think:

```text
DELETE = SQL DELETE
```

HTTP doesn't know anything about SQL.

The server could implement deletion as:

```text
DELETE /users/123
        ↓
Database DELETE
```

or:

```text
DELETE /users/123
        ↓
Mark deleted = true
```

or:

```text
DELETE /users/123
        ↓
Remove object from storage
        ↓
Publish deletion event
        ↓
Invalidate cache
```

HTTP only defines the **semantics of the request**.

The application decides how to implement those semantics.

---

# 3. Raw DELETE request

Using `nc`:

```bash
nc localhost 8080
```

Then:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080

```

The structure is:

```text
DELETE /users/123 HTTP/1.1
│      │          │
│      │          └── HTTP version
│      └───────────── target
└──────────────────── method

Host: localhost:8080
│
└── request header

blank line
│
└── headers are finished

(no body)
```

---

# 4. What should the server return?

A very common successful response is:

```http
HTTP/1.1 204 No Content
```

Why `204`?

Because we successfully performed the operation, but there is nothing to return.

For example:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080


HTTP/1.1 204 No Content
```

There is intentionally no response body.

---

# 5. Let's implement it

Add this to our Python server:

```python
def do_DELETE(self):
    parsed = urlparse(self.path)
    path = parsed.path

    if not path.startswith("/users/"):
        self.send_response(404)
        self.end_headers()
        return

    user_id = path.split("/")[-1]

    print("Deleting user:", user_id)

    self.send_response(204)
    self.end_headers()
```

Now:

```bash
curl -v -X DELETE http://localhost:8080/users/123
```

You should see something like:

```text
> DELETE /users/123 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/...
> Accept: */*

< HTTP/1.0 204 No Content
```

Again, our toy server isn't actually storing users yet.

It's simply demonstrating the HTTP interaction.

---

# 6. What if the resource doesn't exist?

Suppose:

```text
/users/999
```

doesn't exist.

We might return:

```http
HTTP/1.1 404 Not Found
```

For example:

```python
if user_id not in users:
    self.send_response(404)
    self.end_headers()
    return
```

So we might get:

### First request

```http
DELETE /users/123
```

Response:

```http
204 No Content
```

### Second request

```http
DELETE /users/123
```

Response:

```http
404 Not Found
```

This seems strange at first.

---

# 7. Then how is DELETE idempotent?

This is an **important interview concept**.

Idempotency does **not** mean:

> Repeating the request must produce exactly the same HTTP response.

Instead, it means roughly:

> Repeating the same request should have the same intended effect on the resource state as making it once.

Consider:

```text
Initial state:

User 123 exists
```

First:

```text
DELETE /users/123
```

State becomes:

```text
User 123 does not exist
```

Repeat:

```text
DELETE /users/123
```

State remains:

```text
User 123 does not exist
```

So:

```text
DELETE once:
    exists → doesn't exist

DELETE twice:
    exists → doesn't exist → doesn't exist
```

The **final resource state is the same**.

Therefore DELETE is idempotent.

---

# 8. This is different from POST

Consider:

```http
POST /users
```

with:

```json
{
  "name": "Riyaz"
}
```

First request:

```text
Create user #101
```

Repeat:

```text
Create user #102
```

Now the state is different.

```text
POST once:
    0 users → 1 user

POST twice:
    0 users → 2 users
```

Therefore POST is generally **not idempotent**.

Compare:

| Method | Example             | Idempotent?    |
| ------ | ------------------- | -------------- |
| GET    | `GET /users/123`    | Yes            |
| POST   | `POST /users`       | No             |
| PUT    | `PUT /users/123`    | Yes            |
| PATCH  | `PATCH /users/123`  | Not inherently |
| DELETE | `DELETE /users/123` | Yes            |

---

# 9. Why does idempotency matter in real systems?

Imagine a client sends:

```http
DELETE /users/123
```

The server deletes the user.

But then something goes wrong:

```text
Server → client response
              X
         network failure
```

The client doesn't know whether the request succeeded.

It can safely retry:

```http
DELETE /users/123
```

If the first request succeeded, the second request doesn't recreate or modify the user.

This is one reason idempotent operations are useful when dealing with retries.

---

# 10. Important nuance: side effects

Don't interpret idempotency as:

> Absolutely nothing happens when I repeat DELETE.

Suppose:

```text
DELETE /users/123
```

also causes:

```text
delete database record
write audit log
increment metrics
send event
invalidate cache
```

Those surrounding side effects can happen again.

HTTP idempotency is primarily about the **intended effect on the target resource**, not about every observable side effect inside your infrastructure.

---

# 11. DELETE doesn't always mean permanent deletion

This is something you'll encounter frequently in backend systems.

An API might expose:

```http
DELETE /users/123
```

but internally do:

```sql
UPDATE users
SET deleted_at = CURRENT_TIMESTAMP
WHERE id = 123;
```

This is commonly called a **soft delete**.

Instead of:

```text
Database
────────────────
id | name
123| Riyaz
```

becoming:

```text
Database
────────────────
(empty)
```

it becomes:

```text
id | name  | deleted_at
123| Riyaz | 2026-09-18...
```

The API can still treat the resource as deleted.

So:

```text
HTTP DELETE
      ↓
does not necessarily mean
      ↓
physical database deletion
```

Again, HTTP defines the operation's semantics; the implementation is up to the application.

---

# 12. DELETE with a request body?

You may encounter:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{
    "reason": "requested by user"
}
```

HTTP does not make the basic DELETE method equivalent to "DELETE must have no body."

But APIs generally shouldn't invent body semantics casually. If extra information is needed, the API may instead use a dedicated operation/resource design depending on the use case.

For ordinary deletion:

```http
DELETE /users/123
```

is the cleanest model.

---

# 13. DELETE and status codes

Common possibilities:

### Successfully deleted

```http
204 No Content
```

### Successfully deleted and returning something

```http
200 OK
Content-Type: application/json

{
    "message": "User deleted"
}
```

### Resource doesn't exist

```http
404 Not Found
```

Whether a repeated DELETE returns `404`, `204`, or another response is an **API design choice**.

The important distinction is:

```text
HTTP idempotency
        ≠
same response every time
```

---

# 14. One more important distinction

Compare these:

```http
DELETE /users/123
```

and:

```http
POST /users/123/delete
```

Both could potentially cause deletion.

But they communicate different things.

With:

```http
DELETE /users/123
```

the HTTP method itself tells us:

```text
"This request is asking to delete the target resource."
```

That's one of the reasons HTTP methods are more than just arbitrary strings.

They provide **standardized semantics** that clients, servers, proxies, caches, documentation tools, and developers can understand.

---

# 15. Your mental model so far

You now have the core HTTP methods:

```text
                  Resource
                     │
       ┌─────────────┼─────────────┐
       │             │             │
      GET           PUT          DELETE
       │             │             │
    retrieve      replace        remove
       │
       └────── POST / PATCH ──────┐
                                  │
                              modify/create
```

More accurately:

```text
GET
  → retrieve representation

POST
  → submit data for processing

PUT
  → replace representation at known target

PATCH
  → apply partial modification

DELETE
  → remove the target resource
```

---

## 🧪 Exercise

Try these with `curl`:

```bash
curl -v http://localhost:8080/users/123
```

```bash
curl -v -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users
```

```bash
curl -v -X PUT \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz"}' \
  http://localhost:8080/users/123
```

```bash
curl -v -X PATCH \
  -H "Content-Type: application/json" \
  -d '{"name":"Riyaz Mohammed"}' \
  http://localhost:8080/users/123
```

```bash
curl -v -X DELETE \
  http://localhost:8080/users/123
```

And finally, using `nc`:

```bash
nc localhost 8080
```

manually send:

```http
DELETE /users/123 HTTP/1.1
Host: localhost:8080

```

Pay attention to the fact that **you are literally constructing the HTTP request yourself**.

---

# Next: Authentication

We've now covered the basic request/response mechanics and HTTP methods.

The next big question is:

> **How does the server know who is making the request?**

We'll start with:

**Lesson 19 — `Authorization` header**

and build this from the raw HTTP level:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer abc123
```

Then we'll progressively understand **authentication → tokens → Bearer tokens → cookies → sessions → JWTs**, without jumping straight into framework abstractions.


---

# Lesson 19 — The `Authorization` Header

We've reached an important transition.

So far, our requests have effectively been:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
```

The server knows **what** we want.

But it doesn't know **who** is asking.

---

## 1. The problem

Imagine:

```text
Alice → GET /users/123
Bob   → GET /users/123
```

The HTTP requests could be identical:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

How can the server distinguish them?

We need a way for the client to provide **credentials**.

That's where the `Authorization` header comes in.

```http
Authorization: <credentials>
```

For example:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer abc123
```

---

# 2. What is `Authorization`?

It's an HTTP request header used to provide **authentication credentials** to the server.

The general structure is:

```text
Authorization: <authentication-scheme> <credentials>
```

For example:

```http
Authorization: Bearer abc123
```

Break it down:

```text
Authorization
      │
      ├── Bearer
      │     └── authentication scheme
      │
      └── abc123
            └── credentials
```

The important part is:

> `Bearer` is not the token itself. It is the authentication scheme.

---

# 3. What does "Bearer" mean?

The word **bearer** has a very simple idea behind it:

> Whoever bears/possesses this credential can use it.

Imagine the server gives you:

```text
abc123
```

You send:

```http
Authorization: Bearer abc123
```

The server verifies the credential.

If it's valid:

```text
Authenticated ✓
```

If it's invalid:

```text
Authentication failed ✗
```

The token acts somewhat like a **temporary credential**.

---

# 4. Important: HTTP doesn't create the token

This is a common misconception.

HTTP defines the header:

```http
Authorization: ...
```

But HTTP doesn't magically generate:

```text
abc123
```

Your authentication system might generate it.

For example:

```text
Login
  ↓
Authentication service
  ↓
Generate credential
  ↓
Client receives credential
  ↓
Client sends it in Authorization header
```

We'll build the login/token story later.

For now, we're only concerned with how the credential travels over HTTP.

---

# 5. Let's manually send one

Start our server:

```bash
python3 server.py
```

Then:

```bash
nc localhost 8080
```

Send:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer abc123

```

Notice that nothing about the HTTP request structure changed.

We simply added another header:

```text
Request line
    ↓
GET /users/123 HTTP/1.1

Headers
    ↓
Host: localhost:8080
Authorization: Bearer abc123

Blank line
    ↓
```

---

# 6. Let's inspect it in Python

Our handler can access it through:

```python
authorization = self.headers.get("Authorization")

print("Authorization:", authorization)
```

So:

```http
Authorization: Bearer abc123
```

becomes:

```python
authorization
```

with value:

```text
Bearer abc123
```

Python's `http.server` has already parsed the HTTP headers for us.

---

# 7. Let's create a tiny authentication check

For learning purposes, let's pretend:

```text
valid token = abc123
```

Our handler:

```python
def do_GET(self):
    authorization = self.headers.get("Authorization")

    if authorization != "Bearer abc123":
        self.send_response(401)
        self.end_headers()
        return

    body = b"Hello authenticated user!"

    self.send_response(200)
    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", str(len(body)))
    self.end_headers()
    self.wfile.write(body)
```

Now:

```bash
curl -v http://localhost:8080/
```

No authorization header.

Result:

```http
HTTP/1.0 401 Unauthorized
```

Now:

```bash
curl -v \
  -H "Authorization: Bearer abc123" \
  http://localhost:8080/
```

Result:

```http
HTTP/1.0 200 OK
```

---

# 8. Why `401`?

Recall the distinction from our status-code lesson:

```text
401 → authentication problem
403 → authorization/permission problem
```

So:

```http
Authorization: Bearer invalid
```

might produce:

```http
401 Unauthorized
```

because the server cannot authenticate the caller.

But imagine:

```text
Alice is authenticated
        ↓
Alice requests /admin
        ↓
Alice isn't allowed to access admin
```

Then:

```http
403 Forbidden
```

could be appropriate.

Mental model:

```text
Authentication
    ↓
"Who are you?"

Authorization
    ↓
"What are you allowed to do?"
```

---

# 9. Authentication vs Authorization

This distinction is extremely important.

Imagine you're entering an office.

### Authentication

You show your employee badge:

```text
"I am Riyaz."
```

The security system verifies the badge.

That's:

```text
Authentication
```

Then you try to enter the server room.

The system checks:

```text
"Is Riyaz allowed into the server room?"
```

That's:

```text
Authorization
```

In HTTP:

```http
Authorization: Bearer abc123
```

Despite the confusing name, this header is commonly used to **authenticate** the request.

---

# 10. Why isn't the header called `Authentication`?

Good question.

HTTP standardized the header as:

```http
Authorization
```

rather than:

```http
Authentication
```

The header carries credentials used in an authentication scheme.

The distinction between authentication and authorization is mostly about **what the server does with the verified identity/credentials**.

So don't read:

```http
Authorization: Bearer abc123
```

as:

> "This is definitely authorization information."

Think:

> "Here are my credentials according to this authentication scheme."

---

# 11. The Bearer token is opaque to HTTP

Suppose we send:

```http
Authorization: Bearer abc123
```

HTTP doesn't know what `abc123` means.

It doesn't know whether it is:

```text
JWT
opaque random token
session identifier
API key
something else
```

The authentication system decides.

HTTP simply transports the header.

This is an important layering concept:

```text
HTTP
 │
 └── transports Authorization header
              │
              ↓
       Authentication system
              │
              ↓
       interprets credential
```

---

# 12. A real-world request

A typical API request might look like:

```http
GET /api/orders/123 HTTP/1.1
Host: api.example.com
Accept: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Notice that we have multiple independent headers:

```text
Accept
    ↓
"What response representation can I accept?"

Authorization
    ↓
"Here are my credentials."

Host
    ↓
"Which host am I addressing?"
```

Each header has a different responsibility.

---

# 13. Don't confuse `Authorization` with `Cookie`

We'll soon learn cookies, but here's the high-level distinction.

### Authorization header

Client explicitly sends:

```http
Authorization: Bearer abc123
```

Common for APIs.

### Cookie

Client sends:

```http
Cookie: session_id=abc123
```

Common in browser-based authentication.

Both can carry authentication-related state.

But the mechanisms and browser behavior are different.

We'll get into that later.

---

# 14. What if the token is missing?

Our server might return:

```http
HTTP/1.1 401 Unauthorized
```

A more informative response can include:

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer
```

`WWW-Authenticate` tells the client which authentication challenge/scheme is applicable.

For example:

```http
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Bearer
Content-Length: 0
```

This is another example of HTTP providing standardized protocol mechanisms around authentication.

---

# 15. What if the token is wrong?

Request:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer wrong-token

```

Server:

```http
HTTP/1.1 401 Unauthorized
```

Conceptually:

```text
No credential
      ↓
     401

Invalid credential
      ↓
     401

Valid credential
      ↓
 authenticated
```

Then authorization checks happen afterward.

---

# 16. The complete request pipeline

We're starting to get an important picture:

```text
Client
  │
  │ HTTP request
  ↓
┌──────────────────────────────┐
│ GET /users/123 HTTP/1.1      │
│ Host: api.example.com        │
│ Accept: application/json     │
│ Authorization: Bearer abc123 │
│                              │
│                              │
└──────────────────────────────┘
              │
              ↓
       HTTP parser
              │
              ↓
       Authentication
              │
        ┌─────┴─────┐
        │           │
      invalid      valid
        │           │
       401          ↓
              Authorization
                   │
             ┌─────┴─────┐
             │           │
          forbidden    allowed
             │           │
            403          ↓
                    Application
                         │
                         ↓
                       200
```

This distinction becomes extremely useful when debugging APIs.

---

# 17. One important security point

Never treat:

```http
X-User-Id: 123
```

as proof that the caller is user `123`.

A client can simply send:

```bash
curl \
  -H "X-User-Id: 999" \
  http://localhost:8080/users
```

Headers supplied by the client are **not inherently trustworthy**.

Authentication credentials need to be verified.

For example:

```text
Authorization: Bearer abc123
                    ↓
              verify token
                    ↓
              user_id = 123
```

Only after verification should the application trust the resulting identity.

---

# 18. One subtle but important point

`Authorization` isn't encryption.

This:

```http
Authorization: Bearer abc123
```

is still just data being sent over the HTTP connection.

With plain HTTP:

```text
http://example.com
```

the request isn't protected against network observers in the way HTTPS provides.

With HTTPS:

```text
https://example.com
```

TLS protects the HTTP traffic while it travels over the network.

We'll study:

```text
HTTP
 ↓
HTTPS
 ↓
TLS
```

later.

For now:

> **A Bearer token should generally be sent over HTTPS, not plain HTTP.**

---

# 🧪 Hands-on exercise

### 1. No token

```bash
curl -v http://localhost:8080/
```

### 2. Correct token

```bash
curl -v \
  -H "Authorization: Bearer abc123" \
  http://localhost:8080/
```

### 3. Wrong token

```bash
curl -v \
  -H "Authorization: Bearer wrong" \
  http://localhost:8080/
```

### 4. Inspect with `nc`

```bash
nc localhost 8080
```

Then:

```http
GET / HTTP/1.1
Host: localhost:8080
Authorization: Bearer abc123

```

Look at your Python server.

You should see:

```text
Authorization: Bearer abc123
```

---

# 🧠 The key idea to remember

Don't jump directly to:

```text
Bearer token = JWT
```

That's **not true**.

The hierarchy is:

```text
HTTP
 │
 └── Authorization header
          │
          └── authentication scheme
                    │
                    └── Bearer
                          │
                          └── credential/token
                                │
                                ├── opaque token
                                └── JWT
```

JWT is just **one possible kind of token** that can be carried using the Bearer scheme.

We'll eventually understand why JWT exists and what problem it solves.

---

## Next → Lesson 20: What actually happens during Login?

We'll go from:

```text
Authorization: Bearer abc123
```

backward and ask the more important question:

> **Where did `abc123` come from in the first place?**

We'll build the flow:

```text
POST /login
      ↓
username + password
      ↓
server verifies credentials
      ↓
server creates token
      ↓
client stores token
      ↓
Authorization: Bearer <token>
      ↓
future requests
```

Then we'll uncover the next problem:

> **If the server creates a token, how does it know whether that token is still valid?**

That leads naturally into **opaque tokens, server-side sessions, JWTs, access tokens, and refresh tokens**.

---

# Lesson 20 — What Happens During Login?

So far we have this:

```http
GET /users/123 HTTP/1.1
Host: example.com
Authorization: Bearer abc123
```

But we haven't answered the most important question:

> **Where did `abc123` come from?**

Let's build the authentication story from scratch.

---

## 1. The problem

A user initially has something like:

```text
username: riyaz
password: secret123
```

They can't send their password with **every API request**:

```http
GET /users/123 HTTP/1.1
Host: example.com
Authorization: secret123
```

That would be a terrible design.

We want:

```text
Login once
   ↓
prove identity
   ↓
receive a credential
   ↓
use credential for future requests
```

So we introduce a **login endpoint**.

---

# 2. The login request

Typically:

```http
POST /login HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{
    "username": "riyaz",
    "password": "secret123"
}
```

Why `POST`?

Because we're submitting credentials to the server for processing.

The server receives:

```text
username = riyaz
password = secret123
```

and verifies them.

---

# 3. What does the server do?

Conceptually:

```text
POST /login
     │
     ↓
Extract username/password
     │
     ↓
Find user
     │
     ↓
Verify password
     │
   ┌─┴──────────┐
   │            │
invalid       valid
   │            │
 401            ↓
          Create credential
               │
               ↓
        Return credential
```

For example:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "access_token": "abc123"
}
```

Now the client has:

```text
abc123
```

---

# 4. Future requests

The client doesn't send the password anymore.

Instead:

```http
GET /users/123 HTTP/1.1
Host: localhost:8080
Authorization: Bearer abc123
```

So the overall flow is:

```text
                  LOGIN

Client ──────── username/password ────────→ Server
Client ←────────── token ────────────────── Server


                 API REQUEST

Client ──────── Bearer token ─────────────→ Server
Client ←────────── response ──────────────── Server
```

This is the fundamental idea behind token-based authentication.

---

# 5. But now we have a new problem

We created:

```text
abc123
```

The client sends:

```http
Authorization: Bearer abc123
```

How does the server know whether:

```text
abc123
```

is valid?

There are several possibilities.

The first approach is very simple:

> **Store the token on the server.**

---

# 6. Approach 1 — Server-side token storage

Suppose login creates:

```text
token = abc123
user = Riyaz
```

The server stores:

```text
Token Store
────────────────────────
abc123 → user_id=123
xyz789 → user_id=456
```

Then the client sends:

```http
GET /users/123 HTTP/1.1
Authorization: Bearer abc123
```

The server does:

```text
abc123
  ↓
Token Store lookup
  ↓
user_id = 123
  ↓
Authenticated
```

This is a very useful model to understand because it leads directly to **sessions and opaque tokens**.

---

# 7. What does an opaque token mean?

Suppose the token is:

```text
abc123xyz789
```

Look at it.

Can you tell:

```text
user ID?
expiration?
role?
email?
```

No.

It's just an identifier.

That's an **opaque token**.

Conceptually:

```text
Client:
    abc123xyz789

Server:
    "I know what this means."
```

The client doesn't need to understand its contents.

---

# 8. Why is this useful?

The server can maintain:

```text
Token
   ↓
User
   ↓
Permissions
   ↓
Expiration
   ↓
Session state
```

For example:

```text
abc123
    │
    ├── user_id: 123
    ├── expires_at: 14:00
    ├── role: admin
    └── revoked: false
```

Then every request can perform a lookup.

---

# 9. The request lifecycle

Let's make this concrete.

### Step 1 — Login

```http
POST /login HTTP/1.1
Content-Type: application/json

{
    "username": "riyaz",
    "password": "secret123"
}
```

Server:

```text
Verify credentials
      ↓
Generate random token
      ↓
Store token → user mapping
```

Response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "access_token": "abc123"
}
```

---

### Step 2 — Client calls API

```http
GET /orders HTTP/1.1
Authorization: Bearer abc123
```

Server:

```text
abc123
   ↓
lookup
   ↓
user 123
   ↓
check permissions
   ↓
return orders
```

---

# 10. Logout becomes interesting

Suppose the user logs out.

With server-side token storage, we can simply invalidate the token:

```text
abc123 → user 123
```

becomes:

```text
abc123 → revoked
```

or gets deleted.

Then:

```http
GET /orders HTTP/1.1
Authorization: Bearer abc123
```

produces:

```http
401 Unauthorized
```

This is one major advantage of server-side state:

> The server can immediately revoke a credential.

---

# 11. But there's a problem

Imagine we have:

```text
10 million users
```

and each user has an active token.

Now every API request might require:

```text
Request
   ↓
Authentication service
   ↓
Token database/cache
   ↓
User information
   ↓
API
```

That creates infrastructure and latency considerations.

And with multiple servers:

```text
              Load Balancer
             /      |      \
            ↓       ↓       ↓
         Server A Server B Server C
            \       |       /
             \      |      /
               Token Store
```

All application servers need access to the shared authentication state.

This isn't necessarily bad—Redis or another shared store can handle this very well—but it introduces **server-side state and an additional lookup**.

This motivates another approach.

---

# 12. Approach 2 — Put information inside the token

Instead of:

```text
abc123
```

imagine the credential itself contains information such as:

```text
user_id = 123
role = admin
expires = ...
```

Now the server can potentially inspect the credential itself rather than looking up a token record for every request.

This leads us toward:

# JWT

**JSON Web Token**

But don't jump there yet.

There's an important distinction.

---

# 13. Opaque token vs JWT

Think of them like this:

### Opaque token

```text
abc123
```

Server:

```text
abc123
   ↓
lookup server-side
   ↓
user = 123
```

### JWT

Conceptually:

```text
<header>.<payload>.<signature>
```

The token carries claims such as:

```json
{
    "sub": "123",
    "role": "admin",
    "exp": 1789732800
}
```

and is cryptographically signed.

The server can verify the signature and inspect the claims.

We'll learn exactly how this works later.

---

# 14. Don't make this mistake

A common misconception is:

> "JWT is authentication."

Not exactly.

JWT is a **token format**.

Bearer is an **HTTP authentication scheme**.

For example:

```http
Authorization: Bearer <JWT>
```

contains:

```text
Authorization
      │
      ↓
Bearer authentication scheme
      │
      ↓
JWT credential
```

So these concepts are separate.

---

# 15. Access token

We're now ready for another important term:

**Access token**

An access token is a credential that a client presents when accessing protected resources.

For example:

```http
GET /orders HTTP/1.1
Host: api.example.com
Authorization: Bearer abc123
```

Here:

```text
abc123
```

is the access token.

It could be:

```text
opaque token
```

or:

```text
JWT
```

The term **access token** describes its purpose, not necessarily its format.

---

# 16. Why shouldn't the access token live forever?

Suppose:

```text
Access token = abc123
```

and it never expires.

Now someone steals it.

They may be able to use it indefinitely.

So we generally introduce expiration:

```text
Access token
     │
     ├── issued: 12:00
     └── expires: 13:00
```

After expiration:

```http
Authorization: Bearer abc123
```

→ authentication fails.

This gives us another problem:

> What happens when the access token expires while the user is still using the application?

We don't want the user to enter their password every hour.

---

# 17. The next problem leads to Refresh Tokens

We eventually arrive at:

```text
Username + Password
       │
       ↓
     Login
       │
       ├──────────────→ Access Token
       │
       └──────────────→ Refresh Token
```

The access token might be short-lived:

```text
Access token
    ↓
15 minutes
```

while the refresh token lasts much longer:

```text
Refresh token
    ↓
days/weeks/etc.
```

When the access token expires:

```text
Access token expired
       ↓
Client sends refresh token
       ↓
Authentication server
       ↓
New access token
```

The user doesn't need to log in again.

**But we'll earn this concept rather than memorize it.**

---

# 🧠 Current mental model

We have gone from:

```text
Username + Password
        ↓
       Login
        ↓
   Credential/Token
        ↓
Authorization: Bearer <token>
        ↓
Protected API
```

And discovered two broad ways to make that token meaningful:

```text
                    Token
                      │
             ┌────────┴────────┐
             │                 │
       Opaque token           JWT
             │                 │
       Server lookup      Verify signature
             │                 │
        Server state       Claims inside token
```

And one more concept:

```text
Access Token
    ↓
used to access APIs
    ↓
usually short-lived
```

which eventually leads to:

```text
Refresh Token
    ↓
obtain a new access token
```

---

## 🧪 Hands-on exercise

Modify your Python server to recognize this:

```http
POST /login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"username":"riyaz","password":"secret123"}
```

and return:

```json
{
    "access_token": "abc123"
}
```

Then require:

```http
Authorization: Bearer abc123
```

for:

```http
GET /users
```

You're essentially building a tiny authentication system:

```text
/login
   ↓
creates token

/users
   ↓
requires token
```

Don't worry about securely hashing passwords or generating cryptographically secure tokens yet—we're learning the HTTP/authentication mechanics first.

---

# Next → Lesson 21: Sessions vs Tokens

Before jumping into JWT, we need to understand an extremely important historical progression:

```text
Password
   ↓
Session
   ↓
Session ID
   ↓
Cookie
   ↓
Token
   ↓
Access Token
   ↓
JWT
```

We'll answer:

> **What exactly is a session, and how is it different from a token?**

This is the point where authentication starts becoming much easier to reason about.


---

# Lesson 21 — Sessions vs Tokens

This is one of the most important authentication concepts.

People often casually say:

> "We use sessions."

or:

> "We use tokens."

But **what does that actually mean?**

Let's build it from the original problem.

---

# 1. Start with the original problem

Suppose a user logs in:

```http
POST /login HTTP/1.1
Host: example.com
Content-Type: application/json

{
    "username": "riyaz",
    "password": "secret123"
}
```

The server verifies the password.

Now the user makes another request:

```http
GET /profile HTTP/1.1
Host: example.com
```

Problem:

> HTTP is stateless.

The second request doesn't inherently tell the server:

```text
"This is the same person who logged in earlier."
```

Each HTTP request is independent unless we introduce some mechanism for maintaining identity across requests.

---

# 2. The first big solution: Sessions

The server can create a **session** after successful login.

Imagine:

```text
Login
  ↓
user = Riyaz
  ↓
create session
  ↓
session_id = abc123
```

The server stores:

```text
Session Store
────────────────────────
abc123 → user_id=123
```

Then the client receives:

```text
abc123
```

Now future requests can identify the user using that session identifier.

---

# 3. The session itself lives on the server

This distinction is crucial.

Suppose:

```text
Session ID = abc123
```

The server stores:

```text
abc123
    ↓
user_id = 123
    ↓
name = Riyaz
    ↓
authenticated = true
```

The client only needs:

```text
abc123
```

So:

```text
Client                         Server
─────────────────             ─────────────────
abc123                         abc123 → user 123
                              
                               session data
                               lives here
```

That's why we call it **server-side session state**.

---

# 4. But how does the client send the session ID?

This is where **cookies** enter the picture.

The server can respond to login with:

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123
Content-Type: application/json

{
    "message": "Login successful"
}
```

The important header is:

```http
Set-Cookie: session_id=abc123
```

The server is effectively telling the client:

> "Store this cookie and send it back to me on future requests."

---

# 5. Future request

The client then sends:

```http
GET /profile HTTP/1.1
Host: example.com
Cookie: session_id=abc123
```

The server receives:

```text
session_id=abc123
```

and performs:

```text
abc123
   ↓
Session Store
   ↓
user_id = 123
   ↓
Riyaz
```

Now the server knows who is making the request.

---

# 6. The complete session flow

```text
                LOGIN

Client ── username/password ──→ Server
                                  │
                                  ↓
                             verify password
                                  │
                                  ↓
                            create session
                                  │
                                  ↓
                         session_id = abc123
                                  │
Client ←──── Set-Cookie ──────────┘


                FUTURE REQUEST

Client ───── Cookie: abc123 ─────→ Server
                                  │
                                  ↓
                           lookup session
                                  │
                                  ↓
                             user = 123
                                  │
                                  ↓
                            process request
```

This is the classic **session-based authentication** model.

---

# 7. Why use a cookie?

You could theoretically ask the application to send:

```http
X-Session-Id: abc123
```

on every request.

But browsers already have a standardized mechanism for storing and automatically sending cookies.

So browser applications commonly use:

```http
Set-Cookie
```

from the server and:

```http
Cookie
```

from the client.

---

# 8. Let's see it with raw HTTP

### Login response

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123
Content-Type: application/json
Content-Length: 29

{"message":"Login success"}
```

The browser stores:

```text
Cookie jar
────────────────────
session_id=abc123
```

Then:

### Future request

```http
GET /profile HTTP/1.1
Host: example.com
Cookie: session_id=abc123

```

Notice:

```text
Server → Client

Set-Cookie
```

versus:

```text
Client → Server

Cookie
```

That's an important distinction.

---

# 9. Session ≠ Cookie

This is another common interview misconception.

They are related, but they aren't the same thing.

### Session

Server-side authentication state:

```text
abc123 → user 123
```

### Cookie

A mechanism for storing/sending data between browser and server:

```http
Cookie: session_id=abc123
```

So:

```text
Cookie
   ↓
carries session identifier
   ↓
server finds session
```

A cookie doesn't necessarily contain a session.

Cookies can store many kinds of information.

---

# 10. Now compare this with Bearer tokens

Earlier we had:

```http
Authorization: Bearer abc123
```

The client explicitly sends the credential in the `Authorization` header.

With sessions, we commonly have:

```http
Cookie: session_id=abc123
```

So the transport mechanism differs:

```text
Session-based

Cookie
  ↓
session ID
  ↓
server-side lookup
```

versus:

```text
Bearer-token-based

Authorization header
  ↓
access token
  ↓
server validates token
```

---

# 11. But there's a subtle point

A session ID is itself technically a kind of **token-like credential**.

For example:

```text
abc123
```

doesn't inherently have meaning to the client.

The difference we're emphasizing is **where the state lives**.

### Traditional server-side session

```text
Client
  │
  │ session_id=abc123
  ↓
Server
  │
  └── abc123 → user 123
```

### Self-contained token such as JWT

```text
Client
  │
  │ JWT containing claims
  ↓
Server
  │
  └── verify token
```

This distinction is much more useful than simply saying:

> "Cookies are sessions and Authorization headers are tokens."

Because that's not always true.

---

# 12. Sessions can also be distributed

Suppose we have:

```text
             Load Balancer
              /    |    \
             ↓     ↓     ↓
          Server A B     C
```

User logs in through Server A.

Server A creates:

```text
session_id = abc123
```

Where does it store it?

If it stores the session only in Server A's memory:

```text
Server A
────────────
abc123 → user 123
```

then the next request might go to Server B:

```text
Client
   ↓
Load Balancer
   ↓
Server B
```

Server B doesn't know:

```text
abc123
```

because its memory doesn't contain the session.

---

# 13. Solution: Shared session store

We can introduce something like Redis:

```text
              Load Balancer
             /      |      \
            ↓       ↓       ↓
        Server A Server B Server C
             \       |       /
              \      |      /
                Redis
                  │
                  ↓
          abc123 → user 123
```

Now any application server can validate the session.

This is one reason authentication architecture becomes a **distributed-systems problem** at scale.

---

# 14. What happens during logout?

This is another nice property of server-side sessions.

Suppose:

```text
abc123 → user 123
```

User logs out.

Server can invalidate:

```text
abc123
```

Now:

```text
abc123 → invalid
```

or delete the session entirely.

The browser may also receive:

```http
Set-Cookie: session_id=; Max-Age=0
```

Then future requests using the old session ID fail.

---

# 15. Why not just put user ID in the cookie?

You might wonder:

> Why not simply do this?

```http
Cookie: user_id=123
```

Because the client controls the cookie.

A malicious client could change:

```text
user_id=123
```

to:

```text
user_id=999
```

The server must not blindly trust client-controlled identity information.

That's why the session identifier should be an **unguessable credential**, with the actual authentication state stored/validated server-side.

---

# 16. Why should session IDs be random?

Suppose session IDs were:

```text
1001
1002
1003
1004
```

An attacker might guess another user's session.

Instead, we want something like:

```text
8f1a7c9e3d... 
```

generated using a cryptographically secure random mechanism.

Conceptually:

```text
Unpredictable session ID
        ↓
Attacker can't simply guess
        ↓
Possession of ID grants session access
```

This is especially important because a session ID is effectively a credential.

---

# 17. The "Bearer" property appears again

If someone steals:

```text
session_id=abc123
```

they may be able to impersonate the user.

Similarly, if someone steals:

```http
Authorization: Bearer abc123
```

they may be able to use that access token.

That's why both session credentials and bearer access tokens must be protected.

---

# 18. Session vs JWT

Now we can make a more meaningful comparison.

|                            | Server-side session                  | JWT                                          |
| -------------------------- | ------------------------------------ | -------------------------------------------- |
| Client stores              | Session ID                           | JWT                                          |
| Main state                 | Server                               | Encoded/signed token                         |
| Server lookup              | Usually yes                          | Often no session lookup                      |
| Easy immediate revocation  | Yes                                  | More involved                                |
| Horizontal scaling         | Shared session store commonly needed | Can reduce session-state dependency          |
| Credential transport       | Often cookie                         | Often `Authorization: Bearer`                |
| Token contains user claims | Usually no                           | Usually yes                                  |
| Stateful server            | Yes                                  | Can be stateless for access-token validation |

One important caveat:

> **JWT does not automatically mean stateless authentication.**

A system can maintain JWT revocation lists, sessions, token state, etc.

"Stateless JWT authentication" is a specific architecture, not a property that magically applies to every JWT system.

---

# 19. Why did JWT become popular?

Imagine an API architecture with many services:

```text
                 API Gateway
                      │
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       Orders       Users       Payments
```

With server-side sessions, each service may need access to shared authentication state.

JWT provides another possibility:

```text
Client
  │
  │ JWT
  ↓
Orders ── verify ──→ valid
Users  ── verify ──→ valid
Payments ─ verify ─→ valid
```

Each service can potentially validate the token independently.

That can simplify some distributed architectures.

But it introduces other tradeoffs:

* token size
* key management
* expiration
* revocation
* stale claims
* refresh-token architecture

We'll study those rather than treating JWT as automatically "better."

---

# 20. The historical/conceptual progression

The authentication story now looks like:

```text
Password
   │
   ↓
Login
   │
   ↓
Server creates session
   │
   ↓
Session ID
   │
   ↓
Cookie
   │
   ↓
Future requests
```

Alternative:

```text
Password
   │
   ↓
Login
   │
   ↓
Access token
   │
   ↓
Authorization: Bearer <token>
   │
   ↓
Future requests
```

And the access token could be:

```text
Opaque token
```

or:

```text
JWT
```

---

# 🧠 The most important mental model

Don't memorize:

```text
Cookie = session
JWT = token
```

Instead think in layers:

```text
                    Authentication
                          │
             ┌────────────┴────────────┐
             │                         │
       Server-side state        Self-contained credential
             │                         │
          Session                      JWT
             │
       Session ID
             │
           Cookie
```

And separately:

```text
Credential transport

Cookie:
Cookie: session_id=abc123

Authorization header:
Authorization: Bearer abc123
```

These are **different concepts that can be combined in different ways**.

---

# 🧪 Hands-on exercise

Let's manually simulate a session.

Using:

```bash
nc localhost 8080
```

Pretend the server responded to login with:

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123
Content-Length: 0

```

Now make the next request manually:

```http
GET /profile HTTP/1.1
Host: localhost:8080
Cookie: session_id=abc123

```

Then change it:

```http
GET /profile HTTP/1.1
Host: localhost:8080
Cookie: session_id=wrong

```

Your server could conceptually do:

```python
session_id = self.headers.get("Cookie")

if session_id == "session_id=abc123":
    # authenticated
else:
    # 401
```

It's deliberately simplistic—we're focusing on understanding the protocol flow.

---

# Next → Lesson 22: Cookies

Now we'll slow down and understand **cookies themselves**.

We'll look at raw headers such as:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Lax
```

and answer:

* Who creates the cookie?
* Who stores it?
* When does the browser send it?
* What does `HttpOnly` actually do?
* What does `Secure` do?
* What does `SameSite` do?
* Why are cookies important for authentication?
* Why can a cookie be dangerous if configured incorrectly?

This will make browser authentication much less mysterious.


----

# Lesson 22 — Cookies

We've just learned:

```text
Login
  ↓
Server creates session
  ↓
session_id = abc123
  ↓
Client needs to send that ID on future requests
```

A very common mechanism for doing that in browsers is **HTTP cookies**.

Today we'll understand cookies from the raw HTTP level.

---

# 1. What problem do cookies solve?

HTTP itself is stateless.

Imagine:

```http
POST /login
```

The server authenticates you.

Then you make:

```http
GET /profile
```

The second request doesn't automatically know that you logged in.

We need some way for the server to tell the client:

> "Remember this piece of information and send it back to me on relevant future requests."

That's what cookies provide.

---

# 2. The two important headers

Cookies primarily involve two HTTP headers.

### Server → Client

```http
Set-Cookie
```

### Client → Server

```http
Cookie
```

Think:

```text
Server
  │
  │ Set-Cookie
  ↓
Browser
  │
  │ Cookie
  ↓
Server
```

This distinction is extremely important.

---

# 3. Creating a cookie

Suppose login succeeds.

Server responds:

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123
Content-Type: application/json
Content-Length: 29

{"message":"Login success"}
```

The browser sees:

```http
Set-Cookie: session_id=abc123
```

and stores the cookie.

Conceptually:

```text
Browser Cookie Jar
──────────────────────
session_id = abc123
```

---

# 4. Sending the cookie back

Later, the browser requests:

```http
GET /profile HTTP/1.1
Host: example.com
Cookie: session_id=abc123
```

Notice:

```text
Server → Browser

Set-Cookie
```

but:

```text
Browser → Server

Cookie
```

Not:

```http
Cookie: session_id=abc123
```

on the response.

And not:

```http
Set-Cookie: session_id=abc123
```

on every request.

---

# 5. Let's manually simulate it

You can use `nc`:

```bash
nc localhost 8080
```

Pretend we're logging in:

```http
POST /login HTTP/1.1
Host: localhost:8080
Content-Length: 0

```

Imagine the server responds:

```http
HTTP/1.1 200 OK
Set-Cookie: session_id=abc123
Content-Length: 0

```

The browser would store:

```text
session_id=abc123
```

Then a future request:

```http
GET /profile HTTP/1.1
Host: localhost:8080
Cookie: session_id=abc123

```

That's the entire basic cookie mechanism.

---

# 6. Cookies are not inherently authentication

This is another important distinction.

A cookie can contain:

```text
session_id=abc123
```

but it could also contain:

```text
language=en
```

or:

```text
theme=dark
```

or other application state.

So:

```text
Cookie ≠ authentication
```

Rather:

```text
Cookie
  ↓
transport/store small pieces of state
  ↓
one possible use
  ↓
session authentication
```

---

# 7. Cookie attributes

The interesting part is that `Set-Cookie` can contain additional attributes.

For example:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Lax
```

Let's break that down.

```text
session_id=abc123
       │
       └── cookie name/value

HttpOnly
       └── JavaScript access restriction

Secure
       └── send only over secure connections

SameSite=Lax
       └── controls cross-site sending behavior
```

These attributes are extremely important for authentication cookies.

---

# 8. `HttpOnly`

Suppose we have:

```http
Set-Cookie: session_id=abc123; HttpOnly
```

`HttpOnly` tells a browser that the cookie should not be accessible through JavaScript APIs such as:

```javascript
document.cookie
```

So conceptually:

```text
Browser
  │
  ├── HTTP requests → cookie available
  │
  └── JavaScript → cannot directly read HttpOnly cookie
```

Why is this useful?

Imagine a malicious script somehow executes in your page.

Without `HttpOnly`, it may be able to read a session cookie through JavaScript.

With:

```text
HttpOnly
```

the browser prevents JavaScript from directly reading that cookie.

---

# 9. Important security nuance

`HttpOnly` does **not** mean:

> "This cookie is completely protected from attacks."

It only prevents JavaScript from directly accessing the cookie.

If malicious JavaScript is executing in your page, it may still be able to make requests from the user's browser.

For example, JavaScript might cause:

```text
POST /change-email
```

The browser could still attach applicable cookies automatically.

So:

```text
HttpOnly
   ↓
protects cookie confidentiality from JS access
```

but it doesn't solve every browser security problem.

This distinction becomes important when we discuss **CSRF**.

---

# 10. `Secure`

Consider:

```http
Set-Cookie: session_id=abc123; Secure
```

`Secure` tells the browser:

> Send this cookie only over a secure connection.

In practice, that means HTTPS.

So:

```text
https://example.com
      ↓
cookie can be sent
```

while:

```text
http://example.com
      ↓
Secure cookie should not be sent
```

This protects against accidentally sending a sensitive cookie over plaintext HTTP.

---

# 11. Why does this matter?

Suppose your session cookie is:

```text
session_id=abc123
```

and someone can observe plaintext HTTP traffic.

Without appropriate transport security, the credential could potentially be exposed.

Remember:

```text
Cookie
   ↓
authentication credential
   ↓
protect it
```

That's why authentication cookies are normally used with HTTPS and commonly marked:

```text
Secure
```

---

# 12. `SameSite`

This one is more subtle.

Suppose you're logged into:

```text
bank.example
```

Your browser has:

```text
session_id=abc123
```

Now you visit:

```text
evil.example
```

That website might try to cause your browser to make a request to:

```text
bank.example/transfer
```

The question becomes:

> Should the browser automatically attach your `bank.example` cookie to that cross-site request?

This is part of the problem `SameSite` helps control.

---

# 13. `SameSite=Lax`

A common setting is:

```http
Set-Cookie: session_id=abc123; SameSite=Lax
```

This tells the browser to apply restrictions to cross-site cookie sending, while allowing cookies in certain top-level navigation scenarios.

The exact browser rules are more nuanced than:

```text
Lax = never cross-site
```

so don't memorize it that way.

The important concept is:

> `SameSite` controls when cookies are sent in cross-site contexts.

---

# 14. `SameSite=Strict`

```http
Set-Cookie: session_id=abc123; SameSite=Strict
```

This applies stricter cross-site restrictions.

Conceptually:

```text
Same-site request
    ↓
cookie available

Cross-site context
    ↓
cookie generally withheld
```

This can provide stronger CSRF protection, but can also affect some legitimate cross-site navigation flows.

---

# 15. `SameSite=None`

There's also:

```http
SameSite=None
```

This allows the cookie to be sent in cross-site contexts, subject to other browser requirements.

Modern browsers require `Secure` when using:

```text
SameSite=None
```

So you'll commonly see:

```http
Set-Cookie: something=value; SameSite=None; Secure
```

---

# 16. Why does SameSite matter for authentication?

Because cookies are **automatically attached by the browser** when their rules say they apply.

Compare this with:

```http
Authorization: Bearer abc123
```

JavaScript/API clients typically explicitly construct that header.

Cookies are different:

```text
Browser
   │
   │ automatically decides
   │ whether applicable cookies
   │ should accompany request
   ↓
Server
```

That automatic behavior is extremely convenient.

But it also creates security considerations.

---

# 17. Cookie authentication vs Authorization header

Let's compare.

### Cookie-based session

```http
GET /profile HTTP/1.1
Host: example.com
Cookie: session_id=abc123
```

Browser:

```text
stores cookie
     ↓
automatically attaches it
```

Server:

```text
session_id
    ↓
session store
    ↓
user
```

---

### Bearer token

```http
GET /profile HTTP/1.1
Host: api.example.com
Authorization: Bearer abc123
```

Client:

```text
stores access token
     ↓
explicitly adds Authorization header
```

Server:

```text
token
  ↓
validate token
  ↓
identity
```

---

# 18. Cookie attributes in one example

A production authentication cookie might look conceptually like:

```http
Set-Cookie: session_id=abc123; Path=/; HttpOnly; Secure; SameSite=Lax
```

We've seen:

```text
session_id=abc123
```

Now:

### `Path=/`

The cookie applies to requests under the `/` path.

For example:

```text
/profile
/orders
/api/users
```

---

### `HttpOnly`

JavaScript can't directly read the cookie.

---

### `Secure`

Send it only over secure connections.

---

### `SameSite=Lax`

Apply SameSite restrictions to cross-site contexts.

---

# 19. Cookie lifetime

Cookies can also have lifetime information.

For example:

```http
Set-Cookie: session_id=abc123; Max-Age=3600
```

means the cookie has a maximum lifetime of 3600 seconds from when it is set.

You may also encounter:

```http
Expires=...
```

which specifies an expiration date/time.

There are also **session cookies**, which don't specify a persistent expiration and are generally associated with the browser's session.

Important:

> Cookie lifetime and server-side session lifetime are related but are not automatically the same thing.

For example:

```text
Browser cookie expires in 1 hour
Server session expires in 30 minutes
```

The browser may still possess the cookie after 30 minutes, but the server can reject the corresponding session.

---

# 20. Deleting a cookie

A server can tell the browser to remove a cookie.

For example:

```http
Set-Cookie: session_id=; Max-Age=0
```

Conceptually:

```text
Browser
session_id=abc123
       ↓
Max-Age=0
       ↓
remove cookie
```

Logout often involves both:

```text
1. Invalidate server-side session
2. Remove/expire client-side cookie
```

Both matter.

---

# 21. Domain and Path

Cookies can have scope.

For example:

```http
Set-Cookie: session_id=abc123; Path=/api
```

The browser won't treat that cookie as applicable to every unrelated path.

There is also a `Domain` attribute, which controls which hosts can receive the cookie.

This matters when you have:

```text
app.example.com
api.example.com
admin.example.com
```

Cookie scope can determine where the browser sends the credential.

This is another reason cookie configuration is part of your security design, not merely a convenience setting.

---

# 22. A complete browser session flow

Now put everything together:

```text
                         LOGIN

Browser
   │
   │ POST /login
   │ username + password
   ↓
Server
   │
   │ verify credentials
   │
   │ create session
   │
   │ session_id = abc123
   ↓
Browser
   │
   │ Set-Cookie:
   │ session_id=abc123;
   │ HttpOnly;
   │ Secure;
   │ SameSite=Lax
   │
   ↓
Cookie Jar
```

Then:

```text
                    FUTURE REQUEST

Browser
   │
   │ GET /profile
   │ Cookie: session_id=abc123
   ↓
Server
   │
   │ lookup session
   ↓
user = Riyaz
   │
   ↓
return profile
```

---

# 23. Where CSRF enters the picture

Now we can understand why cookies have a special security concern.

Because the browser can automatically send:

```http
Cookie: session_id=abc123
```

a malicious site could potentially try to cause requests to your application using the user's authenticated browser context.

That's the basic idea behind **Cross-Site Request Forgery (CSRF)**.

Conceptually:

```text
User logged into bank
        ↓
Browser has session cookie
        ↓
User visits malicious site
        ↓
Malicious site causes request to bank
        ↓
Browser may attach bank cookie
```

`SameSite` can reduce this risk, but applications may also use explicit **CSRF tokens** and other defenses depending on the architecture.

We'll return to CSRF after we understand the authentication mechanisms more completely.

---

# 24. Cookies vs Sessions vs Tokens

At this point, keep these three concepts separate:

```text
Cookie
   ↓
Browser mechanism for storing/sending data
```

```text
Session
   ↓
Server-side state representing an authenticated interaction
```

```text
Token
   ↓
Credential presented to authenticate/access a resource
```

They can be combined:

```text
Cookie
   ↓
session ID
   ↓
server-side session
```

or:

```text
Cookie
   ↓
access token
```

or:

```text
Authorization header
   ↓
Bearer access token
```

There are many possible architectures.

---

# 🧠 The mental model

Remember this simple flow:

```text
Set-Cookie
    ↓
Browser stores cookie
    ↓
Browser decides when cookie applies
    ↓
Cookie header sent with request
    ↓
Server reads cookie
    ↓
Server uses cookie value
    ↓
possibly finds session/user
```

And for security-sensitive session cookies, you'll frequently encounter:

```text
HttpOnly
Secure
SameSite
```

Each solves a **different problem**.

---

# 🧪 Hands-on exercise

Start your server and use `curl`:

```bash
curl -v \
  -c cookies.txt \
  http://localhost:8080/login
```

The `-c` option tells curl to save received cookies.

Then inspect:

```bash
cat cookies.txt
```

Now send those cookies on another request:

```bash
curl -v \
  -b cookies.txt \
  http://localhost:8080/profile
```

This is useful because you're now seeing something very close to what a browser does:

```text
Response
   ↓
Set-Cookie
   ↓
cookie storage
   ↓
future request
   ↓
Cookie
```

You can also manually create a request:

```bash
nc localhost 8080
```

```http
GET /profile HTTP/1.1
Host: localhost:8080
Cookie: session_id=abc123

```

---

# Next → Lesson 23: Access Tokens vs Refresh Tokens

Now that we understand:

```text
password
   ↓
login
   ↓
session
   ↓
cookie
```

and:

```text
password
   ↓
login
   ↓
access token
   ↓
Authorization: Bearer ...
```

we can tackle the next real-world problem:

> **Why not make the access token last forever?**

We'll build the solution from the problem:

```text
Long-lived token
      ↓
stolen token is dangerous
      ↓
make access token short-lived
      ↓
but user shouldn't login repeatedly
      ↓
refresh token
      ↓
new access token
```

This is where **access tokens, refresh tokens, expiration, rotation, and logout/revocation** start fitting together.


---

# Lesson 23 — Access Tokens vs Refresh Tokens

We now have the pieces:

```text
Login
  ↓
username + password
  ↓
server verifies credentials
  ↓
?
```

The question is: **what should the server give the client after login, and how should that credential remain useful over time?**

---

## 1. The Problem With Sending the Password

Imagine we did this:

```http
POST /login

{
  "username": "riyaz",
  "password": "secret123"
}
```

Then every protected request sends the password:

```http
GET /profile

Authorization: Basic riyaz:secret123
```

That's obviously undesirable.

We want:

```text
Password
   ↓
used only during login
   ↓
credential/token
   ↓
used for normal API requests
```

So we introduce an **access token**.

---

# 2. Access Token

An access token is a credential that the client presents when accessing protected resources.

For example:

```http
GET /profile HTTP/1.1
Host: api.example.com
Authorization: Bearer abc123
```

The important part is:

```text
Authorization: Bearer abc123
                         ↑
                    access token
```

The server validates the token and determines which user it represents.

For example:

```text
abc123 → user_id = 123
```

Then:

```text
Request
   ↓
Access token
   ↓
Validate token
   ↓
User = 123
   ↓
Check authorization
   ↓
Return /profile
```

---

# 3. Why Not Make the Access Token Live Forever?

Suppose we create:

```text
access_token = abc123
```

and it never expires.

A user logs in on Monday.

On Tuesday:

```http
Authorization: Bearer abc123
```

Wednesday:

```http
Authorization: Bearer abc123
```

One month later:

```http
Authorization: Bearer abc123
```

The problem is obvious.

If someone steals:

```text
abc123
```

they potentially have access for a very long time.

So we make access tokens **short-lived**.

For example:

```text
Access token
    ↓
expires in 15 minutes
```

Now we have a new problem.

---

# 4. The New Problem

Suppose the user is using your application.

At 10:00:

```text
Access token
expires at 10:15
```

At 10:16 the user makes:

```http
GET /profile
Authorization: Bearer abc123
```

The server responds:

```http
HTTP/1.1 401 Unauthorized
```

Should the user have to enter their password again?

That would be terrible UX.

We need another mechanism.

---

# 5. Refresh Token

A **refresh token** is a longer-lived credential used to obtain a new access token.

The login response can contain both:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "access_token": "access-abc",
  "refresh_token": "refresh-xyz"
}
```

Conceptually:

```text
Login
  │
  ├── access token
  │      short-lived
  │
  └── refresh token
         longer-lived
```

The access token is used for normal API requests.

The refresh token is used to obtain a new access token.

---

# 6. Normal API Request

Client:

```http
GET /profile HTTP/1.1
Host: api.example.com
Authorization: Bearer access-abc
```

Server:

```text
Validate access token
        ↓
    valid?
      /   \
    yes    no
     ↓      ↓
return    401
data
```

---

# 7. Access Token Expires

Eventually:

```text
access-abc
     ↓
   expired
```

Client tries:

```http
GET /profile HTTP/1.1
Authorization: Bearer access-abc
```

Server:

```http
HTTP/1.1 401 Unauthorized
```

The client can use the refresh token:

```http
POST /token HTTP/1.1
Host: api.example.com
Content-Type: application/json

{
  "refresh_token": "refresh-xyz"
}
```

Server validates the refresh token.

If valid:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "access_token": "access-new"
}
```

Now the client continues:

```http
GET /profile HTTP/1.1
Authorization: Bearer access-new
```

The user didn't need to log in again.

---

# 8. Why Two Tokens?

This is the key idea.

### Access token

Used frequently:

```text
Client → API
```

Therefore we want it relatively short-lived.

### Refresh token

Used less frequently:

```text
Client → Authorization server
```

Therefore it can generally have a longer lifetime and can be subject to stronger protection and additional server-side controls.

Think:

```text
             LOGIN
               │
       ┌───────┴────────┐
       ↓                ↓
 Access Token      Refresh Token
       │                │
       │                │
       ↓                ↓
   API calls       Get new access
       │                token
       ↓                │
   expires ─────────────┘
```

---

# 9. Important: Refresh Token Is NOT Sent to Every API

This distinction is extremely important.

Normal request:

```http
GET /orders

Authorization: Bearer access-abc
```

Not:

```http
Authorization: Bearer refresh-xyz
```

The refresh token is generally only presented to the token/authorization endpoint to obtain a new access token.

So:

```text
Access token
     ↓
Protected API

Refresh token
     ↓
Token endpoint
     ↓
New access token
```

---

# 10. What Happens If the Refresh Token Is Stolen?

This is why refresh tokens need strong protection too.

Imagine:

```text
refresh-xyz
```

gets stolen.

An attacker may be able to obtain new access tokens.

So systems commonly use measures such as:

* secure storage
* HTTPS
* expiration
* server-side revocation
* refresh-token rotation
* detecting reuse of rotated refresh tokens
* appropriate cookie protections when cookies are used

The exact architecture depends on the application.

---

# 11. Refresh Token Rotation

Suppose the client has:

```text
refresh-1
```

It sends:

```http
POST /token

{
  "refresh_token": "refresh-1"
}
```

Server returns:

```text
access-2
refresh-2
```

The old refresh token:

```text
refresh-1
```

may now be invalidated.

So:

```text
refresh-1
     ↓
used
     ↓
access-2 + refresh-2
     ↓
refresh-1 invalid
```

This is called **refresh token rotation**.

It can help detect/reduce the impact of stolen refresh tokens.

---

# 12. Access Token vs Refresh Token

|                 | Access Token                                     | Refresh Token                                                              |
| --------------- | ------------------------------------------------ | -------------------------------------------------------------------------- |
| Purpose         | Access protected APIs                            | Obtain new access token                                                    |
| Lifetime        | Usually shorter                                  | Usually longer                                                             |
| Sent to         | Resource/API server                              | Token/authorization endpoint                                               |
| Used frequently | Yes                                              | No                                                                         |
| If stolen       | Attacker may access APIs until expiry/revocation | Potentially more serious because it can enable obtaining new access tokens |
| Typical format  | Opaque token or JWT                              | Often opaque, but architecture varies                                      |

The important thing is **purpose**, not the token's string format.

---

# 13. Where Does JWT Fit?

Remember our previous discussion:

```text
Bearer ≠ JWT
```

Bearer is the authentication scheme:

```http
Authorization: Bearer <credential>
```

The credential could be:

```text
abc123
```

or a JWT:

```text
eyJhbGciOi...
```

So you could have:

```text
Access token
    ↓
JWT
    ↓
Authorization: Bearer <JWT>
```

Or:

```text
Access token
    ↓
opaque random value
    ↓
Authorization: Bearer abc123
```

Both are possible.

Similarly, a refresh token does **not** have to be a JWT.

---

# 14. The Complete Flow

Here's the mental model I want you to remember:

```text
                    LOGIN
                      │
              username/password
                      │
                      ↓
                 Authenticate
                      │
             ┌────────┴─────────┐
             ↓                  ↓
       Access Token       Refresh Token
       short-lived        longer-lived
             │                  │
             ↓                  │
        API requests            │
             │                  │
             ↓                  │
          expires ──────────────┘
                                │
                                ↓
                         Token endpoint
                                │
                                ↓
                         New Access Token
```

The password is primarily used to establish the authenticated session/token relationship; it should not be repeatedly sent to ordinary APIs.

---

# 15. One More Important Distinction

Don't confuse:

```text
Session
Token
Cookie
Access Token
Refresh Token
JWT
```

They are different concepts.

```text
Session
→ server-side authentication state

Cookie
→ HTTP/browser mechanism for storing and sending data

Access token
→ credential used to access protected resources

Refresh token
→ credential used to obtain new access tokens

JWT
→ a token format

Bearer
→ an HTTP authentication scheme
```

And they can be combined.

For example:

```text
Browser
   │
   │ Cookie: refresh_token=...
   ↓
Server
   │
   ├── access token
   ↓
API
   │
   │ Authorization: Bearer <access-token>
   ↓
Resource
```

That's why saying simply **"we use tokens"** isn't enough when discussing authentication architecture.

---

# Hands-on Exercise

Let's make our toy server support:

### `POST /login`

Request:

```http
POST /login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "riyaz",
  "password": "secret123"
}
```

Response:

```json
{
  "access_token": "access-abc123",
  "refresh_token": "refresh-xyz789"
}
```

Then:

### `GET /profile`

```http
GET /profile HTTP/1.1
Host: localhost:8080
Authorization: Bearer access-abc123
```

And finally:

### `POST /refresh`

```http
POST /refresh HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "refresh_token": "refresh-xyz789"
}
```

Response:

```json
{
  "access_token": "access-new456"
}
```

For learning, you can keep the tokens hardcoded initially. The important thing is to implement the **flow**, not build production-grade token security yet.

### Next lesson

**Lesson 24 — What exactly is a JWT?**

We'll build a JWT by hand conceptually:

```text
header.payload.signature
```

and answer the important questions:

* Why does a JWT have three parts?
* Is the payload encrypted?
* How does the server know it wasn't modified?
* What does the signature actually do?
* Why can the server validate a JWT without a database lookup?
* What does `exp` mean?
* Why shouldn't you put secrets in a JWT?

----

# Lesson 24 — JWT: What Exactly Is It?

We just learned that an access token can be:

```text
opaque token
```

or:

```text
JWT
```

Now let's understand **what a JWT actually is**, without jumping into libraries.

---

## 1. Why Was JWT Introduced?

Consider an opaque token:

```text
access-abc123
```

The server receives:

```http
GET /profile HTTP/1.1
Authorization: Bearer access-abc123
```

How does the server know who this belongs to?

It might need a database/Redis lookup:

```text
access-abc123
       ↓
   Token Store
       ↓
   user_id = 123
   role = admin
   expires = ...
```

That works, but every request may require server-side state.

JWT provides another approach:

```text
Token itself contains claims
        ↓
Server verifies signature
        ↓
Server trusts verified claims
```

---

# 2. JWT Stands for JSON Web Token

A JWT typically looks like this:

```text
xxxxx.yyyyy.zzzzz
```

There are **three parts**:

```text
HEADER.PAYLOAD.SIGNATURE
```

For example:

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
.
eyJzdWIiOiIxMjMiLCJyb2xlIjoiYWRtaW4ifQ
.
abc123...
```

The three parts are:

```text
┌─────────────┐
│   Header    │
├─────────────┤
│   Payload   │
├─────────────┤
│ Signature   │
└─────────────┘
```

---

# 3. Part 1 — Header

The header contains metadata about the JWT.

Conceptually:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

`alg` means:

```text
Which cryptographic algorithm is being used?
```

`typ` indicates the token type.

The header is then encoded using **Base64URL encoding**.

So:

```text
JSON header
    ↓
Base64URL
    ↓
xxxxx
```

---

# 4. Part 2 — Payload

The payload contains **claims**.

For example:

```json
{
  "sub": "123",
  "role": "admin",
  "exp": 1789732800
}
```

These are statements/claims about the token.

Common registered claims include:

### `sub`

Subject.

Usually identifies the entity the token represents.

```json
{
  "sub": "123"
}
```

Could mean:

```text
user_id = 123
```

---

### `exp`

Expiration time.

```json
{
  "exp": 1789732800
}
```

This is a Unix timestamp.

After that time, the token should no longer be accepted.

---

### `iat`

Issued-at time.

```json
{
  "iat": 1789729200
}
```

---

### `iss`

Issuer.

```json
{
  "iss": "https://auth.example.com"
}
```

---

### `aud`

Audience.

```json
{
  "aud": "my-api"
}
```

This can indicate which service/API the token is intended for.

---

# 5. Is the JWT Payload Encrypted?

**No.**

This is one of the most important JWT concepts.

Suppose the payload is:

```json
{
  "sub": "123",
  "role": "admin"
}
```

JWT encoding makes it transportable:

```text
eyJzdWIiOiIxMjMiLCJyb2xlIjoiYWRtaW4ifQ
```

But this is **not encryption**.

Anyone who obtains the JWT can decode the header and payload.

For example:

```text
JWT
 ↓
Base64URL decode
 ↓
JSON
```

Therefore:

> **Do not put secrets/passwords/private information into a normal JWT payload merely because it is a JWT.**

---

# 6. Then What Prevents Someone From Changing It?

This is where the third part comes in.

```text
HEADER.PAYLOAD.SIGNATURE
```

The signature protects the integrity of the token.

Imagine the original payload:

```json
{
  "sub": "123",
  "role": "user"
}
```

An attacker changes it to:

```json
{
  "sub": "123",
  "role": "admin"
}
```

They can easily modify the payload because it's not encrypted.

But now the signature no longer matches.

The server detects that.

---

# 7. The Signature

For a simplified HMAC-based JWT such as HS256:

```text
signature =
HMAC(
    secret,
    base64url(header) + "." + base64url(payload)
)
```

So conceptually:

```text
HEADER
   +
PAYLOAD
   ↓
cryptographic algorithm + secret
   ↓
SIGNATURE
```

The final JWT is:

```text
encodedHeader
.
encodedPayload
.
signature
```

---

# 8. What Happens When the Server Receives It?

Client sends:

```http
GET /profile HTTP/1.1
Host: api.example.com
Authorization: Bearer <JWT>
```

Server performs roughly:

```text
             JWT
              │
       ┌──────┴───────┐
       ↓              ↓
    Header         Payload
       │              │
       └──────┬───────┘
              ↓
        Verify signature
              │
          ┌───┴───┐
          ↓       ↓
        valid   invalid
          │       │
          ↓       ↓
     validate     401
       claims
          │
          ↓
      authorize
          │
          ↓
       /profile
```

The crucial part is:

```text
The server does NOT simply trust the payload.
```

It first verifies the token's cryptographic integrity.

---

# 9. Why Is This Useful?

With an opaque token:

```text
access-abc123
       ↓
Redis / DB
       ↓
user = 123
```

With a self-contained JWT:

```text
JWT
 ↓
verify signature
 ↓
read claims
 ↓
user = 123
```

Therefore, a service can often validate the token **without looking up that access token in a database**.

This is one reason JWTs can be useful in distributed systems.

For example:

```text
                 API Gateway
                      │
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       Service A   Service B   Service C
          │           │           │
          └───────────┼───────────┘
                      │
                 JWT validation
```

Each service can potentially validate the JWT independently.

---

# 10. But Wait — How Does the Server Know the Secret?

This depends on the signing algorithm.

There are two important families to understand.

### Symmetric signing

Example:

```text
HS256
```

Same secret is used to sign and verify:

```text
           SECRET
          /      \
         ↓        ↓
      signing   verification
```

For example:

```text
Authorization Server
        │
        │ shared secret
        ↓
   API Service
```

Both sides need the secret.

---

### Asymmetric signing

Examples include:

```text
RS256
ES256
```

Now we have:

```text
Private key
    ↓
sign

Public key
    ↓
verify
```

Conceptually:

```text
Authorization Server
        │
        │ private key
        ↓
      SIGN
        │
        ↓
       JWT
        │
        ↓
API Service
        │
        │ public key
        ↓
      VERIFY
```

The API service doesn't need the private signing key.

This is particularly useful when many services need to verify tokens.

---

# 11. A Very Important Mental Model

Think about signing like this:

```text
Private secret/key
       +
    message
       ↓
   signature
```

The signature answers:

> "Was this token produced by someone possessing the appropriate signing key, and has the signed content been altered?"

It does **not** answer:

> "Is this user a good person?"

It does not make authorization decisions by itself.

The application still needs to decide:

```text
Is this authenticated identity
allowed to perform this operation?
```

---

# 12. Authentication vs Authorization With JWT

Suppose JWT contains:

```json
{
  "sub": "123",
  "role": "user"
}
```

After validation:

```text
JWT valid
   ↓
User = 123
Role = user
```

Now request:

```http
DELETE /users/456
Authorization: Bearer <JWT>
```

Authentication:

```text
Who is this?
→ user 123
```

Authorization:

```text
Can user 123 delete user 456?
→ application policy decides
```

So:

```text
JWT validation
      ↓
Authentication context
      ↓
Authorization decision
```

A valid JWT does **not** automatically mean the request is authorized.

---

# 13. What Does `exp` Actually Do?

Suppose:

```json
{
  "sub": "123",
  "exp": 1789732800
}
```

The server checks:

```text
current_time < exp ?
```

If:

```text
current_time < exp
```

the token hasn't expired.

If:

```text
current_time >= exp
```

the token is expired.

Then the API can return:

```http
HTTP/1.1 401 Unauthorized
```

This is one reason access tokens can be short-lived.

---

# 14. JWT Is Not Automatically Secure

This is another important point.

People sometimes say:

> "We're using JWT, so authentication is secure."

That's not enough.

Security depends on things such as:

```text
HTTPS
+
secure key management
+
correct signature validation
+
algorithm configuration
+
expiration
+
audience/issuer validation where appropriate
+
secure token storage
+
authorization rules
+
refresh-token handling
```

JWT is simply a token format plus mechanisms for signing/verification.

---

# 15. JWT vs Opaque Token

Now compare them.

|                                 | Opaque Token                    | JWT                            |
| ------------------------------- | ------------------------------- | ------------------------------ |
| Example                         | `abc123`                        | `eyJhbGci...`                  |
| Server can read claims directly | No                              | Yes                            |
| Usually requires token lookup   | Often                           | Often not for basic validation |
| Self-contained                  | No                              | Yes                            |
| Easily revoked server-side      | Usually yes                     | More involved                  |
| Payload visible to holder       | No meaningful payload           | Yes                            |
| Integrity protection            | Server-side lookup / validation | Cryptographic signature        |
| Format                          | Arbitrary                       | Standardized structure         |

But remember:

**JWT does not mean "no database ever."**

A JWT-based system might still maintain:

```text
revoked token IDs
sessions
user state
permissions
refresh tokens
```

So:

```text
JWT ≠ automatically stateless
```

---

# 16. The Complete Access-Token Picture

Now combine everything we've learned.

```text
                  LOGIN
                    │
             username/password
                    │
                    ↓
             Authentication
                    │
                    ↓
             Create access JWT
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
     Header + Payload      Signature
          │                   │
          └─────────┬─────────┘
                    ↓
                   JWT
                    │
                    ↓
              Client stores
                    │
                    ↓
       Authorization: Bearer JWT
                    │
                    ↓
                  API
                    │
                    ↓
            Verify signature
                    │
                    ↓
             Validate claims
                    │
                    ↓
             Authentication
                    │
                    ↓
             Authorization
                    │
                    ↓
                Response
```

---

# 17. One Subtle but Important Point

Suppose someone steals this JWT:

```text
HEADER.PAYLOAD.SIGNATURE
```

They don't need your password.

If the token is valid and hasn't expired, they may be able to use it as a **bearer credential**.

That's why:

```text
JWT
+
Bearer
```

should make you think:

> **Whoever possesses the valid credential may be able to use it.**

The signature prevents **modification**.

It does not prevent **theft**.

That's a very important distinction.

---

# 18. Our Authentication Vocabulary So Far

You should now be able to distinguish these:

```text
Password
   ↓
Used to authenticate/login

Access Token
   ↓
Credential used for API access

Refresh Token
   ↓
Credential used to obtain new access tokens

JWT
   ↓
A token format

Bearer
   ↓
HTTP authentication scheme

Cookie
   ↓
Browser mechanism for storing/sending data

Session
   ↓
Server-side authentication state
```

And these aren't mutually exclusive.

For example:

```text
Login
 ↓
Refresh token in HttpOnly cookie
 ↓
Access JWT
 ↓
Authorization: Bearer <JWT>
 ↓
API
```

That's a perfectly plausible architecture.

---

## Hands-on Exercise

You don't need a JWT library yet.

Take this payload:

```json
{
  "sub": "123",
  "role": "user",
  "exp": 1789732800
}
```

Answer these before moving on:

**1.** Can a user decode this payload if they have the JWT?

**2.** Can a user modify `"role": "user"` to `"role": "admin"`?

**3.** If they modify it, what happens to the signature?

**4.** What does the server need to check before trusting the claims?

**5.** Does a valid JWT automatically mean the user is authorized to perform every operation?

If those five are clear, the next important step is:

**Lesson 25 — How JWT verification actually works, including Base64URL, signing, public/private keys, and a JWT we build ourselves with Python.**

---

# Lesson 25 — How JWT Verification Actually Works

Now let's go one level deeper.

We know:

```text
JWT = Header.Payload.Signature
```

But **how does the server actually verify the signature?**

We'll build the idea from scratch.

---

## 1. Start With a Simple Message

Imagine the authorization server wants to issue this token:

```json
{
  "sub": "123",
  "role": "user"
}
```

It needs to somehow create proof that:

> "I, the trusted authorization server, created this token, and its contents haven't been modified."

That's what the signature provides.

---

# 2. Step 1 — Create the Header

Start with:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

`HS256` means:

```text
HMAC + SHA-256
```

We'll use HS256 first because it's easier to understand.

---

# 3. Step 2 — Create the Payload

For example:

```json
{
  "sub": "123",
  "role": "user",
  "exp": 1789732800
}
```

So we now have:

```text
Header
+
Payload
```

---

# 4. Step 3 — Base64URL Encode Them

JWT does not simply put raw JSON into the token.

It encodes both pieces.

Conceptually:

```text
JSON Header
     ↓
Base64URL
     ↓
encoded header
```

and:

```text
JSON Payload
     ↓
Base64URL
     ↓
encoded payload
```

So we get:

```text
encodedHeader
.
encodedPayload
```

Notice the dot.

---

# 5. Step 4 — Create the Signing Input

This is the exact data that gets signed:

```text
encodedHeader + "." + encodedPayload
```

For example:

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMiLCJyb2xlIjoidXNlciJ9
```

Call this:

```text
signing_input
```

---

# 6. Step 5 — Sign It

With HS256, we have a secret:

```text
my-super-secret-key
```

The authorization server computes:

```text
HMAC-SHA256(
    secret,
    signing_input
)
```

That produces bytes representing the signature.

Then the signature is Base64URL encoded.

Finally:

```text
JWT =
encodedHeader
.
encodedPayload
.
encodedSignature
```

So:

```text
             Header
                │
                ↓
          Base64URL
                │
                ↓
          encodedHeader
                │
                │
             Payload
                │
                ↓
          Base64URL
                │
                ↓
          encodedPayload
                │
                └──────┐
                       ↓
              header.payload
                       │
                       ↓
                HMAC-SHA256
                       ↑
                    secret
                       │
                       ↓
                  signature
                       │
                       ↓
        header.payload.signature
```

That's a JWT.

---

# 7. Now the Interesting Part — Verification

The client sends:

```http
GET /profile HTTP/1.1
Host: api.example.com
Authorization: Bearer eyJ...
```

The server receives the JWT.

It splits:

```text
header.payload.signature
```

into:

```text
header
payload
signature
```

Then it calculates a **new signature**:

```text
HMAC-SHA256(
    serverSecret,
    header + "." + payload
)
```

And compares:

```text
calculated_signature
        vs
received_signature
```

If they match:

```text
✓ Signature valid
```

If they don't:

```text
✗ Signature invalid
```

---

# 8. Why Can't an Attacker Change the Payload?

Suppose the legitimate token says:

```json
{
  "sub": "123",
  "role": "user"
}
```

An attacker decodes it and changes:

```json
{
  "sub": "123",
  "role": "admin"
}
```

They can absolutely do that.

Remember:

> JWT payloads aren't encrypted.

But now:

```text
header.payload
```

has changed.

The attacker doesn't know the server's secret.

Therefore they can't produce the correct new signature.

The original signature was calculated over:

```text
header.payload(user)
```

but the attacker is now sending:

```text
header.payload(admin)
```

The server calculates:

```text
HMAC(secret, header.payload(admin))
```

That doesn't match the original signature.

Therefore:

```text
❌ Invalid signature
```

---

# 9. This Is the Core Security Property

The signature gives us **integrity and authenticity of the signed data**.

Think:

```text
             SECRET
                │
                ↓
        ┌──────────────┐
        │ Sign content │
        └──────────────┘
                │
                ↓
            Signature
```

Later:

```text
Content + Signature
        │
        ↓
   Verify with key
        │
     ┌──┴──┐
     ↓     ↓
   valid invalid
```

If valid, the verifier has evidence that the signed content wasn't modified and was produced by a party possessing the signing key.

---

# 10. Let's Do This With Python

We can demonstrate the mechanics without using a JWT library.

Start:

```python
import base64
import hashlib
import hmac
import json
```

We'll create a helper:

```python
def base64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()
```

Why remove `=`?

JWT uses **Base64URL encoding without padding**.

---

# 11. Encode the Header

```python
header = {
    "alg": "HS256",
    "typ": "JWT"
}

header_json = json.dumps(
    header,
    separators=(",", ":")
).encode()

encoded_header = base64url_encode(header_json)

print(encoded_header)
```

The important part isn't the exact output.

It's:

```text
Python dict
    ↓
JSON
    ↓
UTF-8 bytes
    ↓
Base64URL
    ↓
JWT header
```

---

# 12. Encode the Payload

```python
payload = {
    "sub": "123",
    "role": "user",
    "exp": 1789732800
}

payload_json = json.dumps(
    payload,
    separators=(",", ":")
).encode()

encoded_payload = base64url_encode(payload_json)

print(encoded_payload)
```

Now:

```python
signing_input = (
    encoded_header
    + "."
    + encoded_payload
)

print(signing_input)
```

---

# 13. Create the Signature

```python
secret = b"my-super-secret-key"

signature = hmac.new(
    secret,
    signing_input.encode(),
    hashlib.sha256
).digest()

encoded_signature = base64url_encode(signature)

print(encoded_signature)
```

Finally:

```python
jwt = (
    encoded_header
    + "."
    + encoded_payload
    + "."
    + encoded_signature
)

print(jwt)
```

You just created the basic structure of a JWT.

---

# 14. Now Verify It

Take the JWT:

```python
parts = jwt.split(".")

encoded_header = parts[0]
encoded_payload = parts[1]
received_signature = parts[2]
```

Reconstruct the signing input:

```python
signing_input = (
    encoded_header
    + "."
    + encoded_payload
)
```

Calculate what the signature **should** be:

```python
expected_signature = hmac.new(
    secret,
    signing_input.encode(),
    hashlib.sha256
).digest()

expected_signature = base64url_encode(
    expected_signature
)
```

Compare:

```python
if hmac.compare_digest(
    received_signature,
    expected_signature
):
    print("Valid JWT")
else:
    print("Invalid JWT")
```

Notice something important:

```text
Server does NOT need to trust the payload.
```

It verifies the signature first.

---

# 15. Let's Attack Our JWT

Suppose we decode the payload:

```json
{
  "sub": "123",
  "role": "user",
  "exp": 1789732800
}
```

An attacker changes:

```json
{
  "sub": "123",
  "role": "admin",
  "exp": 1789732800
}
```

They can Base64URL encode the modified payload.

But they don't know:

```text
my-super-secret-key
```

Therefore they cannot calculate the correct signature.

So:

```text
Modified payload
      +
Original signature
      ↓
❌ verification failure
```

---

# 16. What About Decoding the JWT?

This is why websites can easily show JWT contents.

A JWT is roughly:

```text
Base64URL(header)
.
Base64URL(payload)
.
Base64URL(signature)
```

The first two components are simply encoded data.

You don't need the secret to decode them.

You need the appropriate key to **verify the signature**.

This distinction is fundamental:

```text
Decode
   ↓
Can anyone do it?

YES

Verify
   ↓
Requires cryptographic key
```

---

# 17. Now Let's Look at RS256

HS256 uses:

```text
             SAME SECRET
              /       \
             ↓         ↓
          Sign       Verify
```

This creates a problem in larger systems.

Imagine 50 services need to verify tokens.

If they all need the secret:

```text
Secret
  ↓
Service A
Service B
Service C
...
Service Z
```

Every service possesses a powerful secret.

Instead, we can use asymmetric cryptography.

---

# 18. Private Key + Public Key

With RS256:

```text
Private key
     ↓
   SIGN
     ↓
   JWT
     ↓
Public key
     ↓
  VERIFY
```

Only the authorization server needs the private key.

Other services only need the public key.

```text
             Authorization Server
                     │
               PRIVATE KEY
                     │
                     ↓
                   SIGN
                     │
                     ↓
                    JWT
                     │
       ┌─────────────┼─────────────┐
       ↓             ↓             ↓
   Service A      Service B     Service C
       │             │             │
   PUBLIC KEY     PUBLIC KEY    PUBLIC KEY
       │             │             │
       └─────────────┼─────────────┘
                     ↓
                  VERIFY
```

This is one reason asymmetric signing is common in distributed authentication systems.

---

# 19. Public Key Doesn't Mean Public Data

This distinction is subtle.

The public key can be distributed to services.

That's okay.

The private key must remain protected.

```text
Public key
→ verification

Private key
→ signing
```

If someone gets the public key:

```text
Can they create valid JWTs?

No.
```

If someone gets the private key:

```text
They may be able to create valid signatures.
```

That's why private-key protection is critical.

---

# 20. What Does JWT Verification Actually Mean?

When an API receives a JWT, verification isn't just:

```text
signature == valid
```

A proper implementation typically checks multiple things.

For example:

```text
JWT
 │
 ├─ Is it structurally valid?
 │
 ├─ Is the algorithm acceptable?
 │
 ├─ Is the signature valid?
 │
 ├─ Is it expired?
 │
 ├─ Is it not-before constraint satisfied, if used?
 │
 ├─ Is issuer correct?
 │
 ├─ Is audience correct?
 │
 └─ Are required claims present?
```

Then:

```text
Valid authentication context
        ↓
Authorization rules
        ↓
Allow / deny operation
```

The exact checks depend on the protocol and architecture.

---

# 21. One Dangerous Mistake

Never think:

```text
JWT payload says:

"role": "admin"

therefore

user is admin
```

Instead:

```text
JWT
 ↓
verify signature
 ↓
validate issuer/audience/time/etc.
 ↓
trusted claims
 ↓
authorization policy
 ↓
allow/deny
```

The signature is what gives the server confidence that the claims came from the trusted signer and weren't modified.

---

# 22. Our Authentication Architecture Is Now Becoming Clear

We've gone from:

```text
username + password
```

to:

```text
username + password
        ↓
      login
        ↓
   access token
        ↓
Authorization: Bearer <token>
```

Then:

```text
access token
      ↓
     JWT
      ↓
header.payload.signature
```

And:

```text
JWT
 ↓
signature verification
 ↓
claims validation
 ↓
authentication
 ↓
authorization
```

---

## The 5 Things You Should Remember

If you remember nothing else from this lesson:

```text
1. JWT has:
   header.payload.signature

2. Header and payload are encoded, not encrypted.

3. Signature protects the integrity/authenticity of the signed content.

4. The server verifies the signature before trusting claims.

5. A valid JWT proves a valid token, NOT that the user can perform every action.
```

### Next: Lesson 26 — JWT in a Real Distributed System

We'll connect this to the architecture you're likely to encounter in backend systems:

```text
                    User
                      │
                      ↓
               Login / Auth Server
                      │
              access JWT + refresh
                      │
                      ↓
                 API Gateway
                      │
          ┌───────────┼───────────┐
          ↓           ↓           ↓
       User API   Order API   Payment API
          │           │           │
          └───────────┼───────────┘
                      ↓
                 JWT validation
```

We'll cover **JWKS, public-key distribution, key rotation, issuer/audience, API Gateway vs service-level validation, and what actually happens when a signing key is rotated.**


----

# Lesson 26 — JWT in a Real Distributed System

So far, we looked at JWT from the inside:

```text
JWT
 ↓
Header
Payload
Signature
 ↓
Verify
 ↓
Trust claims
```

Now let's answer the backend/system-design question:

> **How does this actually work when we have an Auth Server, API Gateway, and multiple microservices?**

---

# 1. The Architecture

Imagine an e-commerce system:

```text
                         User
                           │
                           │ username/password
                           ↓
                    ┌──────────────┐
                    │ Auth Server  │
                    └──────────────┘
                           │
                           │ Access JWT
                           ↓
                    ┌──────────────┐
                    │ API Gateway  │
                    └──────────────┘
                       │    │    │
              ┌────────┘    │    └────────┐
              ↓             ↓             ↓
        User Service   Order Service  Payment Service
```

There are two fundamentally different responsibilities here.

### Auth Server

Responsible for:

```text
Login
Token issuance
Refresh tokens
Signing tokens
Key management
```

### Resource/API services

Responsible for:

```text
Validate access token
Identify user
Authorize operation
Serve request
```

---

# 2. Login

The user sends credentials to the authentication system:

```http
POST /login HTTP/1.1
Host: auth.example.com
Content-Type: application/json

{
  "username": "riyaz",
  "password": "secret123"
}
```

Auth server verifies the credentials.

Then creates an access JWT.

Conceptually:

```json
{
  "sub": "123",
  "iss": "https://auth.example.com",
  "aud": "my-api",
  "role": "user",
  "exp": 1789732800
}
```

The server signs it with its **private key**.

```text
Auth Server
     │
     │ private key
     ↓
    SIGN
     │
     ↓
    JWT
```

The client receives it.

---

# 3. Client Calls the API

Now the client wants:

```http
GET /orders HTTP/1.1
Host: api.example.com
Authorization: Bearer <JWT>
```

The request reaches the API Gateway.

The gateway needs to answer:

```text
Is this token valid?
```

---

# 4. Where Does the Gateway Get the Public Key?

Remember:

```text
Private key → sign
Public key  → verify
```

The Auth Server can publish its public keys through a standard mechanism called **JWKS**.

JWKS means:

> JSON Web Key Set

Conceptually:

```text
Auth Server
     │
     │ publishes public keys
     ↓
   JWKS
     │
     ↓
API Gateway
```

The JWKS endpoint might conceptually look like:

```text
https://auth.example.com/.well-known/jwks.json
```

The exact URL depends on the identity provider/protocol configuration.

The response contains public key information.

For example, conceptually:

```json
{
  "keys": [
    {
      "kid": "key-2026-01",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig"
    }
  ]
}
```

Don't worry about every field yet.

The important idea is:

```text
Auth Server
   ↓
publishes public verification keys
   ↓
API services
```

---

# 5. What Is `kid`?

Look back at the JWT header.

It can contain:

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-2026-01"
}
```

`kid` means:

```text
Key ID
```

Why do we need it?

Because the Auth Server may have multiple signing keys.

For example:

```text
key-2025
key-2026
key-2027
```

The JWT says:

```text
kid = key-2026
```

The API can therefore find the corresponding public key.

```text
JWT
 │
 └── kid = key-2026
          │
          ↓
        JWKS
          │
          ↓
    public key 2026
```

---

# 6. Why Would We Have Multiple Keys?

This leads to an important production concept:

**Key rotation.**

Suppose today we have:

```text
Private Key A
Public Key A
```

Auth Server signs new JWTs using A.

Eventually we want to replace the key.

Why?

Because cryptographic keys shouldn't necessarily remain in use forever.

So we introduce:

```text
Private Key B
Public Key B
```

Now:

```text
Old JWTs
   ↓
signed with A

New JWTs
   ↓
signed with B
```

The Auth Server changes:

```text
Current signing key
A → B
```

---

# 7. But What About Existing Tokens?

This is where `kid` becomes useful.

Suppose:

```text
Old JWT:
kid = A
```

and:

```text
New JWT:
kid = B
```

The API has:

```text
Public Key A
Public Key B
```

Therefore:

```text
JWT
 │
 ├── kid=A → Public Key A → verify
 │
 └── kid=B → Public Key B → verify
```

During the transition, the system can keep the old public key available long enough to validate still-valid tokens.

---

# 8. Key Rotation Timeline

Imagine:

```text
10:00
Current signing key = A

10:30
New key B published

11:00
Auth server starts signing with B

11:00+
Old JWTs signed with A still exist
```

So the JWKS may temporarily contain:

```text
A → public key
B → public key
```

Eventually, once old tokens can no longer be valid:

```text
A → removed
B → remains
```

This is an important principle:

> **You don't immediately remove an old verification key just because you stopped signing new tokens with it.**

Otherwise, valid old tokens could suddenly fail verification.

---

# 9. Why Doesn't Every Request Call the Auth Server?

You might initially imagine:

```text
User
 ↓
API Gateway
 ↓
Auth Server
 ↓
Is JWT valid?
 ↓
API
```

for every request.

That would create unnecessary dependency and latency.

With a signed JWT:

```text
User
 ↓
API Gateway
 ↓
verify JWT locally
 ↓
API
```

The gateway already has the public key.

It can perform cryptographic verification locally.

```text
No database lookup
No Auth Server request
```

for basic signature validation.

That's one of the major benefits of self-contained signed access tokens.

---

# 10. But Then How Does the Gateway Get New Keys?

It can retrieve the JWKS and cache the keys.

Conceptually:

```text
                 Auth Server
                     │
                    JWKS
                     │
                     ↓
              API Gateway cache
                     │
        ┌────────────┼────────────┐
        ↓            ↓            ↓
      key A        key B        ...
```

When a JWT contains:

```text
kid = B
```

the gateway looks up B in its cached key set.

If necessary, it can refresh its JWKS cache according to the identity provider's configuration.

The exact caching strategy depends on the implementation.

---

# 11. JWT Verification Is More Than Signature

Suppose the gateway receives a JWT.

It should conceptually perform:

```text
                 JWT
                  │
                  ↓
            Parse structure
                  │
                  ↓
           Read JWT header
                  │
                  ↓
            Find kid/key
                  │
                  ↓
          Verify signature
                  │
                  ↓
        Validate registered claims
                  │
          ┌───────┼────────┐
          ↓       ↓        ↓
         exp     iss      aud
                  │
                  ↓
        Authentication context
```

Let's understand the three particularly important claims.

---

# 12. `iss` — Issuer

Suppose your system trusts:

```text
https://auth.example.com
```

JWT says:

```json
{
  "iss": "https://auth.example.com"
}
```

The service can verify:

```text
Is this token issued by the expected authority?
```

Without appropriate issuer validation, accepting a token from an unintended issuer can be dangerous.

---

# 13. `aud` — Audience

Suppose we have:

```text
User API
Order API
Payment API
```

A token could specify:

```json
{
  "aud": "order-api"
}
```

This means, conceptually:

```text
This token is intended for Order API.
```

Then:

```text
Order API
   ↓
aud == order-api?
```

If not:

```text
401
```

Again, the exact validation rules depend on the token protocol and architecture, but the principle is:

> **Don't accept a token merely because its signature is valid; validate that it was issued for the service/resource you're protecting.**

---

# 14. `exp` — Expiration

We already saw this:

```json
{
  "exp": 1789732800
}
```

The API checks whether the token has expired.

```text
current time
     │
     ↓
 current < exp ?
    /     \
  yes      no
   ↓        ↓
continue   reject
```

---

# 15. Authentication vs Authorization Again

Suppose the JWT says:

```json
{
  "sub": "123",
  "role": "user"
}
```

The service verifies:

```text
Signature ✓
Issuer ✓
Audience ✓
Expiration ✓
```

Now it knows:

```text
Authenticated user = 123
```

But the user requests:

```http
DELETE /admin/users/456
```

The service still needs authorization:

```text
Is user 123 allowed to delete users?
```

Maybe:

```text
role = user
```

doesn't allow it.

So:

```text
JWT validation
     ↓
Authentication
     ↓
Authorization policy
     ↓
403 Forbidden
```

This is why **401 and 403 are different**.

---

# 16. Gateway vs Service Validation

Now we get an interesting architecture decision.

Should only the API Gateway validate JWTs?

```text
User
 ↓
Gateway
 ↓
Service
```

Or should services validate them too?

```text
User
 ↓
Gateway
 ↓
Service
 ↓
validate JWT
```

There isn't one universal answer.

A common defense-in-depth design is:

```text
             JWT
              │
              ↓
         API Gateway
         validate
              │
              ↓
         Service
         validate
              │
              ↓
          authorize
```

Why might a service validate independently?

Because the service shouldn't necessarily trust that every request reaching it came through the gateway.

For example, another internal service might call it directly.

The security boundary matters.

---

# 17. A Critical Distinction

Suppose the gateway validates the JWT and sends:

```http
X-User-Id: 123
```

to the backend.

Should the backend blindly trust:

```http
X-User-Id: 123
```

?

Not necessarily.

Remember our earlier lesson:

```text
Client-controlled headers are not proof of identity.
```

If a service trusts identity headers, the architecture must ensure those headers can only be injected by a trusted component and cannot be spoofed by untrusted clients.

One approach is to have the service validate the original credential itself.

Another is to establish a strong trusted internal boundary.

The architecture determines the correct mechanism.

---

# 18. JWT Doesn't Eliminate All Server State

This is another misconception.

You might hear:

> "JWT is stateless."

More precisely:

```text
Access-token validation can be stateless.
```

But the overall authentication system might still have:

```text
Users
Refresh tokens
Revoked tokens
Signing keys
Sessions
Permissions
Authorization data
```

For example:

```text
             Access JWT
                  │
             stateless
             validation
                  │
                  ↓
              API call

Refresh token
     │
     ↓
server-side storage
     │
     ↓
rotation/revocation
```

So a system can be:

```text
stateless access-token validation
+
stateful refresh-token management
```

That's very common conceptually.

---

# 19. What Happens During Logout?

This gets interesting.

Suppose access token expires in:

```text
15 minutes
```

and refresh token is still valid.

If the user logs out, what should happen?

The authentication system can invalidate/revoke the refresh credential.

Then:

```text
Existing access token
     ↓
may remain valid
until expiration
```

while:

```text
Refresh token
     ↓
revoked
```

Therefore the client can't obtain another access token.

This illustrates an important property:

> **JWTs aren't inherently instantly revocable.**

If an API only validates the JWT signature and expiration, it may not know that a specific token was "logged out" before expiry.

You can introduce server-side revocation/state when the requirements demand it.

---

# 20. The Full Production Mental Model

Put everything together:

```text
                         ┌─────────────────┐
                         │   Auth Server   │
                         │                 │
                         │ login           │
                         │ refresh         │
                         │ sign JWT        │
                         │ manage keys     │
                         └────────┬────────┘
                                  │
                         publishes public keys
                                  │
                                  ↓
                                JWKS
                                  │
                                  │
       ┌──────────────────────────┴─────────────────────────┐
       │                                                    │
       ↓                                                    ↓
    Client                                            API Gateway
       │                                                    │
       │ Authorization: Bearer JWT                         │
       └───────────────────────────────────────────────────→│
                                                            │
                                                     verify JWT
                                                            │
                                               ┌────────────┴──────────┐
                                               ↓                       ↓
                                           invalid                   valid
                                               ↓                       ↓
                                              401                   Service
                                                                       │
                                                                       ↓
                                                               authorization
                                                                       │
                                                                 ┌─────┴─────┐
                                                                 ↓           ↓
                                                               allow        deny
                                                                 ↓           ↓
                                                              response      403
```

---

# 21. The Most Important Mental Model

At this point, authentication should look like layers:

```text
                    REQUEST
                       │
                       ↓
             Authorization header
                       │
                       ↓
                  Bearer JWT
                       │
                       ↓
              ┌────────────────┐
              │ Cryptographic  │
              │ verification   │
              └───────┬────────┘
                      ↓
               Token claims
                      │
             ┌────────┼────────┐
             ↓        ↓        ↓
            exp      iss      aud
             │        │        │
             └────────┼────────┘
                      ↓
               Authentication
                      │
                      ↓
                Authorization
                      │
                 ┌────┴────┐
                 ↓         ↓
               allow      deny
```

The important distinction is:

```text
Signature
    ↓
"Was this token legitimately signed and unchanged?"

Claims validation
    ↓
"Is this token acceptable for this API and time?"

Authentication
    ↓
"Who does this token represent?"

Authorization
    ↓
"What is that identity allowed to do?"
```

These are **different questions**.

---

## One final concept before we move on

We now have:

```text
Password
   ↓
Login
   ↓
Access Token
   ↓
JWT
   ↓
Signature
   ↓
Public Key / JWKS
   ↓
API validation
   ↓
Authentication
   ↓
Authorization
```

But we haven't yet answered a very practical question:

> **Where should the browser/mobile app actually store these tokens?**

That's where security gets very interesting.

### Next: Lesson 27 — Token Storage: Cookies vs Local Storage vs Memory

We'll examine:

```text
HttpOnly Cookie
localStorage
sessionStorage
in-memory token
```

and understand **XSS, CSRF, why HttpOnly exists, why `Secure` exists, and why choosing token storage is an architectural security decision rather than just a frontend implementation detail.**

---

# Lesson 27 — Where Should We Store Tokens?

We've reached an important practical question.

Suppose login gives us:

```json
{
  "access_token": "eyJ...",
  "refresh_token": "abc..."
}
```

Where does the client put them?

Common possibilities include:

```text
HttpOnly Cookie
localStorage
sessionStorage
JavaScript memory
```

The answer matters because of two major browser security problems:

```text
XSS
CSRF
```

Let's understand them first.

---

# 1. Option 1 — localStorage

A frontend application can do:

```javascript
localStorage.setItem("access_token", token);
```

Later:

```javascript
const token = localStorage.getItem("access_token");
```

Then:

```http
GET /profile HTTP/1.1
Authorization: Bearer eyJ...
```

The JavaScript application explicitly reads the token and puts it into the `Authorization` header.

The flow is:

```text
localStorage
     ↓
JavaScript
     ↓
Authorization header
     ↓
API
```

This is convenient.

But there's a significant problem.

---

# 2. XSS

XSS means **Cross-Site Scripting**.

Imagine your application contains an XSS vulnerability.

An attacker manages to execute JavaScript in your application's origin.

That malicious JavaScript could potentially do:

```javascript
const token = localStorage.getItem("access_token");
```

If the access token is there:

```text
XSS
 ↓
JavaScript executes
 ↓
read localStorage
 ↓
steal token
```

The attacker can potentially use the stolen bearer token from somewhere else.

That's why storing sensitive bearer credentials in browser-readable storage requires careful consideration.

---

# 3. Why `HttpOnly` Exists

Now consider a cookie:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure
```

`HttpOnly` tells the browser:

> Don't expose this cookie through normal JavaScript cookie APIs.

So:

```javascript
document.cookie
```

doesn't give JavaScript access to that `HttpOnly` cookie.

Conceptually:

```text
                  Browser
                    │
        ┌───────────┴───────────┐
        │                       │
    JavaScript              HTTP layer
        │                       │
        X                  HttpOnly cookie
                                │
                                ↓
                              Server
```

This can significantly reduce the impact of token theft through scripts that simply read browser storage.

But there's a very important catch.

---

# 4. HttpOnly Does NOT Make XSS Harmless

Suppose the browser has:

```text
HttpOnly session cookie
```

Malicious JavaScript cannot simply do:

```javascript
document.cookie
```

and read it.

Good.

But JavaScript running in your application's origin can potentially make requests:

```javascript
fetch("/api/delete-account", {
    method: "POST"
});
```

The browser may automatically attach the relevant cookie.

So:

```text
XSS
 ↓
can't directly read HttpOnly cookie
 ↓
but may be able to make authenticated requests
```

This is a crucial distinction.

`HttpOnly` protects **cookie confidentiality from JavaScript**.

It does not magically prevent XSS from making requests as the user.

Therefore:

> **XSS prevention is still extremely important.**

---

# 5. Option 2 — Cookies

Instead of:

```javascript
localStorage
```

the server can issue:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Lax
```

Then the browser automatically sends:

```http
GET /profile HTTP/1.1
Host: example.com
Cookie: session_id=abc123
```

The application JavaScript doesn't need to manually attach the credential.

The flow becomes:

```text
Login
 ↓
Set-Cookie
 ↓
Browser cookie jar
 ↓
Browser automatically sends cookie
 ↓
Server
```

This is very convenient for browser-based applications.

But now we encounter another problem.

---

# 6. CSRF

Cookies are automatically attached by the browser.

Imagine you're logged into:

```text
bank.example.com
```

Your browser has:

```text
Cookie: session_id=abc123
```

Now you visit:

```text
evil.example
```

Suppose that malicious site somehow causes your browser to send a request to:

```http
POST https://bank.example.com/transfer
```

The browser's cookie behavior is why CSRF is a concern.

Conceptually:

```text
User logged into bank
       │
       ↓
Browser has cookie
       │
       ↓
User visits malicious site
       │
       ↓
Malicious site causes request
       │
       ↓
Browser may attach bank cookie
       │
       ↓
Bank sees authenticated request
```

The malicious site doesn't necessarily need to know the cookie value.

That's the key difference from token theft.

---

# 7. XSS vs CSRF

This distinction is worth memorizing.

### XSS

Attacker gets JavaScript to execute in your application's origin.

The concern is:

```text
Can attacker execute code as your application?
```

### CSRF

Attacker causes a user's browser to make an authenticated request to your application.

The concern is:

```text
Can attacker cause a request that carries the user's credentials?
```

Simplified:

```text
XSS
→ attacker runs JavaScript in your origin

CSRF
→ attacker causes authenticated requests from another origin
```

They are different attacks.

---

# 8. SameSite Cookies

One important defense against CSRF is:

```http
SameSite
```

For example:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Lax
```

The browser uses SameSite rules to restrict when cookies are sent in cross-site contexts.

Common values:

```text
Strict
Lax
None
```

### Strict

More restrictive.

```text
SameSite=Strict
```

The browser applies strong restrictions to cross-site cookie sending.

### Lax

A commonly used setting for many authentication cookies.

```text
SameSite=Lax
```

Allows certain cross-site navigation behavior while restricting many cross-site request scenarios.

### None

Allows cross-site cookie usage:

```text
SameSite=None; Secure
```

Modern browsers require `Secure` when using `SameSite=None`.

The exact browser behavior is nuanced, so don't reduce SameSite to simply:

```text
Strict = good
Lax = medium
None = bad
```

It's about the application's cross-site requirements.

---

# 9. `Secure`

Remember:

```http
Set-Cookie: session_id=abc123; Secure
```

`Secure` tells the browser to send the cookie only over secure connections.

In practice:

```text
HTTPS
  ↓
cookie can be sent

HTTP
  ↓
Secure cookie isn't sent
```

This protects against sending the cookie over an insecure HTTP connection.

---

# 10. Three Important Cookie Attributes

For authentication cookies, you'll frequently see:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure; SameSite=Lax
```

Think:

```text
HttpOnly
→ JavaScript can't directly read cookie

Secure
→ send only over secure connections

SameSite
→ controls cross-site cookie behavior
```

Each addresses a different concern.

---

# 11. Option 3 — sessionStorage

You might also encounter:

```javascript
sessionStorage.setItem("access_token", token);
```

It behaves similarly to `localStorage` from the perspective of JavaScript access.

JavaScript can read it:

```javascript
sessionStorage.getItem("access_token");
```

So XSS can potentially access it.

The major difference is lifetime/scope behavior.

Simplified:

```text
localStorage
→ persists across browser sessions

sessionStorage
→ associated with a browser tab/page session
```

But neither one becomes magically secure against XSS simply because it's sessionStorage.

---

# 12. Option 4 — In-Memory Storage

Another approach is:

```javascript
let accessToken = "...";
```

The token exists only in application memory.

Conceptually:

```text
Login
 ↓
access token
 ↓
JavaScript memory
 ↓
Authorization header
```

If the page is refreshed:

```text
page refresh
 ↓
JavaScript memory reset
 ↓
access token gone
```

This reduces the persistence of the credential in browser storage.

But now we have a UX problem:

> What happens after page refresh?

We need some mechanism to obtain a new access token.

This is where refresh tokens and cookies can work together.

---

# 13. A Common Architecture

One possible browser architecture is:

```text
                  Login
                    │
                    ↓
              Auth Server
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
    Access Token        Refresh Token
     short-lived          longer-lived
          │                   │
          ↓                   ↓
     JS memory          HttpOnly cookie
          │                   │
          ↓                   │
 Authorization               │
    header                   │
          │                   │
          ↓                   ↓
       API              refresh endpoint
```

Normal API request:

```http
GET /orders
Authorization: Bearer <access-token>
```

When access token expires:

```text
access token expired
       ↓
refresh endpoint
       ↓
HttpOnly refresh cookie
       ↓
new access token
       ↓
store in memory
```

This is one architecture used by browser applications.

It's not the only architecture.

---

# 14. Why Keep the Access Token in Memory?

Suppose:

```text
access token
→ JavaScript memory
```

Then normal API requests use:

```http
Authorization: Bearer <access-token>
```

A page refresh removes the token.

The application can then use its refresh mechanism to obtain another access token.

The goal is to avoid putting the long-lived credential into JavaScript-readable persistent storage.

Again, this doesn't eliminate XSS risk. Malicious JavaScript executing while the application is running may still be able to use an in-memory access token if it can interact with application APIs.

Security is about reducing attack surface, not finding one magical storage mechanism.

---

# 15. Why Not Put Everything in Cookies?

You absolutely can design an application around cookies.

For example:

```text
Browser
  │
  │ Cookie
  ↓
Backend
  │
  ↓
Session
```

This is the classic server-side session model we discussed earlier.

In that architecture:

```text
Cookie
→ session ID

Server
→ session state
```

You don't necessarily need JWT at all.

This is why it's important not to think:

```text
modern authentication = JWT
```

A perfectly valid architecture can be:

```text
HttpOnly cookie
+
server-side session
```

---

# 16. Session Cookie vs JWT

### Session-based

```text
Browser
   │
   │ Cookie: session_id=abc
   ↓
Server
   │
   ↓
Session Store
   │
   ↓
user=123
```

### JWT-based

```text
Browser
   │
   │ Authorization: Bearer JWT
   ↓
API
   │
   ↓
Verify JWT
   │
   ↓
claims
```

Neither model is automatically "more secure."

The security depends on the complete design and implementation.

---

# 17. Why Do We Keep Seeing `HttpOnly + Secure + SameSite`?

Because authentication cookies often need protection against multiple classes of attacks:

```text
                  Auth Cookie
                      │
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
    HttpOnly        Secure       SameSite
        │             │             │
        ↓             ↓             ↓
    JS access      HTTP leak     cross-site
    reduction       reduction     request
                                  reduction
```

They solve different problems.

---

# 18. A Practical Browser Flow

Let's put the concepts together.

### Step 1 — Login

```http
POST /login
Content-Type: application/json

{
  "username": "riyaz",
  "password": "secret123"
}
```

Server responds:

```http
HTTP/1.1 200 OK
Set-Cookie: refresh_token=xyz; HttpOnly; Secure; SameSite=Lax
Content-Type: application/json

{
  "access_token": "eyJ..."
}
```

The application keeps the access token in memory.

---

### Step 2 — API request

```http
GET /profile
Authorization: Bearer eyJ...
```

---

### Step 3 — Access token expires

API:

```http
HTTP/1.1 401 Unauthorized
```

Application calls:

```http
POST /refresh
Cookie: refresh_token=xyz
```

Browser automatically sends the cookie.

Server verifies the refresh token.

Response:

```json
{
  "access_token": "new-eyJ..."
}
```

Application puts the new access token into memory.

Then:

```http
GET /profile
Authorization: Bearer new-eyJ...
```

---

# 19. What Happens on Logout?

A good logout flow may involve both sides.

Client:

```http
POST /logout
```

Server:

```text
invalidate/revoke refresh credential
```

and responds with a cookie expiration:

```http
Set-Cookie: refresh_token=; Max-Age=0; HttpOnly; Secure; SameSite=Lax
```

The browser removes the cookie.

The access token that is already in memory is also discarded by the application.

Depending on the architecture, an already-issued access token may remain usable until its expiration unless the server maintains revocation state.

This is one reason short-lived access tokens are commonly paired with refresh-token mechanisms.

---

# 20. The Big Comparison

| Storage          |   JavaScript can read? | Automatically sent? | Main concern                             |
| ---------------- | ---------------------: | ------------------: | ---------------------------------------- |
| `localStorage`   |                    Yes |                  No | XSS can steal it                         |
| `sessionStorage` |                    Yes |                  No | XSS can steal it                         |
| In-memory        | Application can access |                  No | XSS can potentially use it while running |
| HttpOnly cookie  |    No direct JS access |                 Yes | CSRF/cross-site behavior                 |
| Normal cookie    |       Yes, via JS APIs |                 Yes | XSS + CSRF concerns                      |

The last column is intentionally simplified. Real security depends on the entire application.

---

# 21. The Most Important Trade-Off

You can think of the choice as:

```text
JavaScript-readable credential
        ↓
    XSS exposure
```

versus:

```text
Automatically attached credential
        ↓
    CSRF exposure
```

That's why secure browser authentication isn't simply:

> "Put the token somewhere safe."

Instead, you design defenses appropriate to the credential transport mechanism.

---

# 22. Our Authentication Story So Far

We've now gone from the original problem:

```text
HTTP is stateless
```

to:

```text
How does the server know who you are?
```

Then:

```text
Password
   ↓
Login
   ↓
Access token
   ↓
JWT
   ↓
Signature
   ↓
Public/private keys
   ↓
JWKS
   ↓
API validation
   ↓
Authentication
   ↓
Authorization
```

And on the browser side:

```text
Token
   ↓
Where do we store it?
   │
   ├── localStorage
   ├── sessionStorage
   ├── memory
   └── cookie
          │
          ├── HttpOnly
          ├── Secure
          └── SameSite
```

---

## One Mental Model to Keep

When you see:

```http
Authorization: Bearer eyJ...
```

don't just think:

> "JWT."

Think through the whole chain:

```text
Bearer
  ↓
authentication scheme

JWT
  ↓
token format

Signature
  ↓
integrity/authenticity

Claims
  ↓
identity/context

exp / iss / aud
  ↓
token validation

Authorization policy
  ↓
what the user can actually do
```

And if you see:

```http
Cookie: session_id=abc123
```

think:

```text
Cookie
  ↓
browser transport mechanism

Session ID
  ↓
server-side authentication state

HttpOnly / Secure / SameSite
  ↓
browser security controls
```

### Next: Lesson 28 — CSRF in Depth

We'll build a concrete attack with two local servers:

```text
localhost:8000 → legitimate bank
localhost:9000 → malicious website
```

and you'll see exactly **how the browser can send an authenticated cookie without the malicious site knowing its value**, why `SameSite` helps, and how CSRF tokens solve the problem.


---

# Lesson 28 — CSRF in Depth

We’ve reached an important point in authentication.

You now know:

```text
Cookie
   ↓
Browser automatically sends it
   ↓
Server authenticates the request
```

That automatic behavior is useful for sessions, but it creates a security problem:

> **CSRF — Cross-Site Request Forgery**

---

## 1. The problem

Suppose you are logged into your bank.

Your browser has:

```http
Set-Cookie: session_id=abc123; HttpOnly
```

Later, you visit:

```text
https://evil.example
```

The malicious website contains:

```html
<form action="https://bank.example/transfer" method="POST">
    <input type="hidden" name="to" value="attacker">
    <input type="hidden" name="amount" value="10000">
</form>

<script>
    document.forms[0].submit();
</script>
```

The browser sends:

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=attacker&amount=10000
```

The important part:

**The malicious website doesn't need to know `abc123`.**

The browser already knows the cookie and may automatically attach it when making a request to `bank.example`.

---

# 2. Why doesn't the browser just block this?

Because cookies were designed to work this way.

Imagine:

```text
Browser
   │
   ├── bank.example
   │      Cookie: session_id=abc123
   │
   └── evil.example
```

When JavaScript on `evil.example` submits a form to `bank.example`, the browser knows:

> "This request is going to bank.example, and I have a cookie belonging to bank.example."

So historically, it sends the cookie.

The browser isn't necessarily asking:

> "Did the user intentionally initiate this request?"

That's the fundamental CSRF problem.

---

# 3. Why can't evil.example simply read the response?

This is where CSRF and CORS are different.

The attacker may be able to cause:

```text
evil.example
     │
     │ POST /transfer
     ↓
bank.example
```

But the browser's same-origin policy generally prevents `evil.example` from reading the bank's response.

So:

```text
Can attacker cause request?
        YES, potentially

Can attacker read bank response?
        NO, generally
```

And CSRF doesn't necessarily require reading the response.

If the request itself performs:

```text
transfer $10,000
change email
delete account
change password
create order
```

the damage has already happened.

---

# 4. CSRF is primarily a cookie-authentication problem

Compare these two designs.

### Cookie authentication

```http
POST /transfer

Cookie: session_id=abc123
```

Browser automatically attaches the cookie.

Potential CSRF risk.

---

### Authorization header

```http
POST /transfer

Authorization: Bearer eyJ...
```

A malicious website cannot simply tell the browser:

```html
<form>
```

to add an arbitrary `Authorization` header.

The attacker would need access to the token.

So:

```text
Cookie
   ↓
automatically attached
   ↓
CSRF concern
```

versus:

```text
Authorization header
   ↓
application explicitly supplies credential
   ↓
different CSRF characteristics
```

This is one reason the choice between cookies and bearer tokens matters.

---

# 5. First defense — SameSite cookies

Modern cookies support:

```http
Set-Cookie: session_id=abc123; SameSite=Lax
```

`SameSite` tells the browser about cross-site cookie sending.

The important modes are:

```text
Strict
Lax
None
```

Conceptually:

### Strict

Very restrictive cross-site cookie behavior.

```text
bank.example
     ↑
     │
evil.example

Cookie generally not sent cross-site
```

### Lax

Allows some cross-site navigation scenarios while restricting many cross-site state-changing requests.

This is a common practical default.

### None

Allows cross-site cookie sending.

It must be combined with:

```text
Secure
```

in modern browsers.

---

# 6. Why SameSite helps

Suppose:

```text
User logged into bank
        ↓
session cookie exists
        ↓
User visits evil.example
        ↓
evil.example attempts POST /transfer
        ↓
Browser sees cross-site request
        ↓
SameSite policy
        ↓
Cookie may NOT be attached
```

Then the bank receives:

```http
POST /transfer

(no valid session cookie)
```

and responds:

```http
HTTP/1.1 401 Unauthorized
```

Attack stopped.

---

# 7. But SameSite isn't the only defense

A traditional defense is a **CSRF token**.

The idea is surprisingly simple.

Instead of relying only on:

```http
Cookie: session_id=abc123
```

the server gives the legitimate application a secret value:

```text
csrf_token = xyz789
```

The client sends it with state-changing requests.

For example:

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=attacker&amount=10000&csrf_token=xyz789
```

The server checks:

```text
Is session valid?
        ↓
YES

Is CSRF token valid?
        ↓
YES

Perform transfer
```

---

# 8. Why does this stop the attacker?

The attacker knows:

```text
bank.example
/transfer
```

and may cause the browser to send:

```text
Cookie: session_id=abc123
```

But the attacker does **not** know:

```text
csrf_token=xyz789
```

because the token is not automatically attached like a cookie.

So the attack becomes:

```http
POST /transfer

Cookie: session_id=abc123

to=attacker
amount=10000
```

Server:

```text
CSRF token missing
       ↓
403 Forbidden
```

---

# 9. The important security property

The session cookie is:

```text
automatically sent
```

The CSRF token is:

```text
not automatically sent cross-site
```

Therefore the attacker needs something they shouldn't possess.

This gives us:

```text
Authentication
      +
Proof that request came from legitimate application
```

---

# 10. A simple implementation

Imagine our server creates a session:

```python
sessions = {
    "abc123": {
        "user_id": 42,
        "csrf_token": "xyz789"
    }
}
```

The browser has:

```http
Cookie: session_id=abc123
```

The legitimate application gets:

```text
xyz789
```

Then:

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=123&amount=100&csrf_token=xyz789
```

Server:

```python
session = sessions.get(session_id)

if not session:
    return 401

if csrf_token != session["csrf_token"]:
    return 403

perform_transfer()
```

---

# 11. Important distinction: authentication vs CSRF

This is a common interview question.

Suppose:

```http
Cookie: session_id=abc123
```

is valid.

That answers:

> **Who is making this request?**

But it doesn't necessarily answer:

> **Did this request come from the legitimate application?**

CSRF protection adds another check.

```text
Session cookie
      ↓
Who are you?

CSRF token
      ↓
Was this request intentionally generated by the legitimate application?
```

Not literally a cryptographic proof of intent, but it provides a secret that an ordinary cross-site attacker cannot obtain.

---

# 12. CSRF token vs JWT signature

Don't confuse these.

JWT:

```text
header.payload.signature
```

protects the integrity/authenticity of the token.

CSRF token:

```text
csrf_token=xyz789
```

protects cookie-authenticated state-changing requests from cross-site request forgery.

They solve different problems.

---

# 13. What about HttpOnly?

You might think:

> "But we already made the cookie HttpOnly!"

For example:

```http
Set-Cookie: session_id=abc123; HttpOnly
```

`HttpOnly` prevents JavaScript from doing:

```javascript
document.cookie
```

to directly read the cookie.

That's useful against cookie theft through JavaScript.

But it does **not** stop CSRF.

Why?

Because the browser can still automatically attach:

```http
Cookie: session_id=abc123
```

to a request.

So:

```text
HttpOnly
   ↓
Protects cookie from direct JS access

SameSite / CSRF token
   ↓
Helps protect against CSRF
```

Different defenses.

---

# 14. XSS vs CSRF

These two are often confused.

### XSS

Attacker gets JavaScript executed inside your application's origin.

```text
attacker JavaScript
       ↓
bank.example
       ↓
runs as bank.example
```

Potentially severe.

### CSRF

Attacker causes the victim's browser to make a request to your application.

```text
evil.example
      ↓
victim's browser
      ↓
bank.example
```

The attacker doesn't necessarily execute JavaScript inside `bank.example`.

Mental model:

```text
XSS
"Make my code run in your origin."

CSRF
"Make your browser send a request to my target."
```

---

# 15. Practical architecture

A modern application might use:

```text
Browser
   │
   │ Login
   ↓
Auth Server
   │
   ├── access token → memory
   │
   └── refresh token → HttpOnly cookie
```

Then:

```text
API request
    ↓
Authorization: Bearer <access-token>
```

The refresh endpoint might use:

```http
POST /refresh
Cookie: refresh_token=...
```

Because the refresh token is cookie-based, the refresh endpoint itself should be designed with appropriate CSRF protection.

For example:

```text
SameSite cookie
       +
CSRF protection where appropriate
       +
Origin/Referer validation where appropriate
```

Security is layered.

---

# 16. One subtle point about our local demo

You may remember the earlier idea:

```text
localhost:8000 → bank
localhost:9000 → evil
```

That is **not a good real cross-site CSRF demonstration**.

Why?

Because cookies are primarily scoped by **domain**, not port.

Also, `localhost:8000` and `localhost:9000` are the same host:

```text
localhost
```

and browser "site" calculations don't treat the port as the key distinction.

So don't think:

```text
localhost:8000
      ≠
localhost:9000
```

means two different sites.

For conceptual testing, you can instead use different hostnames such as:

```text
bank.test
evil.test
```

mapped to your local machine.

But modern browser SameSite defaults can prevent the simplest CSRF attack anyway, which is actually a useful demonstration of why browser defaults have evolved.

---

# 17. Let's see the raw HTTP difference

### Legitimate request

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=123&amount=100
```

The server sees a valid authenticated session.

Now with CSRF protection:

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=123&amount=100&csrf_token=xyz789
```

The server accepts it.

Attacker-generated request:

```http
POST /transfer HTTP/1.1
Host: bank.example
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=attacker&amount=10000
```

Server:

```text
session → valid
csrf_token → missing
```

Response:

```http
HTTP/1.1 403 Forbidden
```

That's the entire basic mechanism.

---

# 18. The complete mental model

You now have several layers:

```text
                    HTTP
                     │
              ┌──────┴──────┐
              │             │
          Authentication  Authorization
              │             │
       "Who are you?"   "What can you do?"
              │
       ┌──────┴──────┐
       │             │
    Session         JWT
       │
     Cookie
       │
       ├── HttpOnly
       ├── Secure
       └── SameSite
              │
              ↓
             CSRF
              │
       ┌──────┴──────┐
       │             │
   SameSite      CSRF token
```

And remember:

```text
HttpOnly ≠ CSRF protection

Secure ≠ CSRF protection

JWT ≠ automatically CSRF-safe

Cookie ≠ Session

JWT ≠ Bearer

Authentication ≠ Authorization
```

These distinctions are extremely useful in backend interviews.

---

## Mini exercise

Consider:

```http
POST /change-email HTTP/1.1
Host: example.com
Cookie: session_id=abc123
Content-Type: application/json

{
  "email": "attacker@example.com"
}
```

Answer these mentally:

1. Why can CSRF potentially happen here?
2. Does `HttpOnly` prevent it?
3. Does `Secure` prevent it?
4. How does `SameSite` help?
5. How would a CSRF token change the request?
6. Why doesn't the attacker need to know `session_id`?

Once those are clear, the next step is **Lesson 29 — CORS**, where we'll connect CSRF, Same-Origin Policy, browser security, and the confusing difference between:

```text
"Can the browser send the request?"
```

and

```text
"Can JavaScript read the response?"
```


---

# Lesson 29 — CORS + Same-Origin Policy

CSRF introduced an important browser-security question:

> If `evil.com` can cause a request to `bank.com`, why can't it simply read the response too?

The answer leads us to **Same-Origin Policy (SOP)** and **CORS**.

---

## 1. The problem CORS solves

Imagine your frontend is:

```text
https://app.example.com
```

and your API is:

```text
https://api.example.com
```

Your frontend JavaScript wants:

```javascript
fetch("https://api.example.com/users");
```

But these are different **origins**.

The browser needs a security mechanism that says:

> "Is JavaScript running on `app.example.com` allowed to read responses from `api.example.com`?"

That's what **CORS** helps control.

---

# 2. First: what is an origin?

An origin consists of:

```text
scheme + host + port
```

For example:

```text
https://example.com:443
│       │           │
scheme  host        port
```

These are different origins:

```text
https://example.com
http://example.com
https://api.example.com
https://example.com:8080
```

Even though some of them may belong to the same broader organization/site.

---

## 3. Same-origin policy

Suppose JavaScript is running on:

```text
https://evil.com
```

and tries:

```javascript
fetch("https://bank.com/account");
```

The browser can potentially send the request depending on the request and credentials.

But the browser prevents the JavaScript from freely reading the response unless the target server permits it.

Conceptually:

```text
evil.com JavaScript
       │
       │ request
       ↓
   bank.com
       │
       │ response
       ↓
    Browser
       │
       X
       │
evil.com JavaScript
```

This is a core browser security boundary.

---

# 4. SOP existed before CORS

The browser's default security model is essentially:

> JavaScript from one origin shouldn't automatically be able to read arbitrary data from another origin.

Otherwise imagine visiting:

```text
evil.com
```

and its JavaScript doing:

```javascript
fetch("https://mybank.com/account");
```

If JavaScript could freely read the response, it could potentially access:

```json
{
  "balance": 500000
}
```

or:

```json
{
  "email": "riyaz@example.com",
  "transactions": [...]
}
```

That would be disastrous.

So browsers enforce cross-origin restrictions.

---

# 5. Then why do we need CORS?

Because legitimate applications frequently need cross-origin requests.

For example:

```text
Frontend
https://app.example.com

API
https://api.example.com
```

The API owner can explicitly say:

```http
Access-Control-Allow-Origin: https://app.example.com
```

Meaning:

> JavaScript from this origin is allowed to read the response.

So:

```text
SOP
 ↓
Default browser restriction

CORS
 ↓
Server explicitly grants cross-origin access
```

---

# 6. Let's see a simple request

Frontend:

```javascript
fetch("https://api.example.com/users");
```

Browser sends something like:

```http
GET /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
```

Notice:

```http
Origin: https://app.example.com
```

The browser tells the server:

> "This request originated from this origin."

The API can respond:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Access-Control-Allow-Origin: https://app.example.com

{"users":[]}
```

Now the browser allows the frontend JavaScript to read the response.

---

# 7. `Origin` is extremely important

You have already seen:

```http
Host: api.example.com
```

Now we introduce:

```http
Origin: https://app.example.com
```

They mean different things.

### Host

The destination server:

```http
Host: api.example.com
```

### Origin

The origin from which the browser initiated the request:

```http
Origin: https://app.example.com
```

For example:

```text
Browser
  │
  │ JavaScript running on app.example.com
  │
  └──────→ api.example.com
```

Request:

```http
Host: api.example.com
Origin: https://app.example.com
```

---

# 8. CORS is primarily enforced by the browser

This is a very important point.

Suppose you run:

```bash
curl https://api.example.com/users
```

The API might return:

```json
{"users":[]}
```

even if CORS isn't configured.

Why?

Because:

> **CORS is a browser security mechanism.**

`curl` doesn't enforce browser SOP.

Similarly:

```bash
curl -H "Origin: https://evil.com" https://api.example.com/users
```

doesn't make curl behave like a browser.

The server may return the response anyway.

The browser is the component that decides whether JavaScript is allowed to access it.

---

# 9. CORS does NOT mean "block the request"

This is one of the biggest misconceptions.

People often say:

> "CORS blocks cross-origin requests."

That's an oversimplification.

The important distinction is:

```text
Request sent
      ≠
JavaScript allowed to read response
```

For some requests, the browser may send the request and then block JavaScript from accessing the response.

For other requests, the browser performs a **preflight** first.

We'll get there.

---

# 10. Simple cross-origin request

Suppose:

```javascript
fetch("https://api.example.com/users");
```

The browser might send:

```http
GET /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
```

Server:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Access-Control-Allow-Origin: https://app.example.com

{"users":[]}
```

Browser sees:

```text
Origin matches
      ↓
CORS permission granted
      ↓
JavaScript gets response
```

---

# 11. What if the server doesn't send CORS headers?

Server:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"users":[]}
```

The network request may have reached the server.

But browser JavaScript gets something like:

```text
CORS error
```

The important mental model:

```text
Server may have processed request
             ↓
Browser receives response
             ↓
Browser checks CORS
             ↓
JavaScript may be denied access
```

This is why CORS errors can be confusing.

You can see the request in server logs even though your frontend says:

```text
CORS error
```

---

# 12. Preflight

Now imagine your frontend sends:

```http
POST /users
Content-Type: application/json
```

The browser may first send an **OPTIONS** request.

This is called a **preflight request**.

Example:

```http
OPTIONS /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type
```

The browser is asking:

> "If I send a POST with the Content-Type header, will you allow this origin?"

---

# 13. Server responds to preflight

Server:

```http
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: POST
Access-Control-Allow-Headers: Content-Type
```

Browser thinks:

```text
Origin allowed?       YES
POST allowed?         YES
Content-Type allowed? YES

Proceed.
```

Then:

```http
POST /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Content-Type: application/json

{"name":"Riyaz"}
```

---

# 14. Why does preflight exist?

Imagine an API:

```text
DELETE /account
```

A random website shouldn't be able to make arbitrary cross-origin requests with arbitrary headers and methods without the server having an opportunity to opt in.

So the browser asks first:

```text
OPTIONS
   ↓
"May I do this?"
   ↓
Server says yes
   ↓
Actual request
```

---

# 15. OPTIONS is an HTTP method

You already learned:

```text
GET
POST
PUT
PATCH
DELETE
```

Now add:

```text
OPTIONS
```

OPTIONS asks about the communication options for a target resource.

CORS uses OPTIONS for preflight.

Example:

```http
OPTIONS /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
```

---

# 16. Important CORS headers

### `Access-Control-Allow-Origin`

```http
Access-Control-Allow-Origin: https://app.example.com
```

Specifies allowed origin(s).

---

### `Access-Control-Allow-Methods`

```http
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
```

Methods allowed for cross-origin requests.

---

### `Access-Control-Allow-Headers`

```http
Access-Control-Allow-Headers: Content-Type, Authorization
```

Headers the browser may send in the cross-origin request.

---

### `Access-Control-Allow-Credentials`

This becomes important when cookies are involved:

```http
Access-Control-Allow-Credentials: true
```

We'll connect this to authentication shortly.

---

# 17. CORS + cookies

Suppose:

```text
Frontend
https://app.example.com

API
https://api.example.com
```

The API uses:

```http
Set-Cookie: session_id=abc123
```

The frontend wants:

```javascript
fetch("https://api.example.com/profile", {
    credentials: "include"
});
```

Now there are additional requirements.

Server might return:

```http
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
```

The browser can then allow credentialed cross-origin access, subject to cookie rules such as `SameSite`.

---

# 18. Why can't we use `*` with credentials?

You might see:

```http
Access-Control-Allow-Origin: *
```

That's useful for public APIs.

But credentialed CORS cannot simply say:

```http
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: true
```

The browser requires an explicit origin for credentialed access.

So instead:

```http
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
```

---

# 19. CORS vs CSRF

This distinction is extremely important.

### CSRF

Question:

> Can another site cause an authenticated state-changing request?

Example:

```text
evil.com
   ↓
POST /transfer
   ↓
bank.com
```

---

### CORS

Question:

> Can JavaScript from another origin read the response?

Example:

```text
evil.com JavaScript
       ↓
GET /account
       ↓
bank.com
       ↓
Can evil.com JS read response?
```

So:

```text
CSRF
→ unauthorized action

CORS
→ cross-origin response access
```

They are related to browser security, but they solve different problems.

---

# 20. A crucial example

Suppose:

```text
evil.com
```

causes:

```http
POST /transfer
Cookie: session_id=abc123
```

and the bank processes it.

Even if CORS is completely disabled:

```text
CSRF can still happen.
```

Why?

Because CSRF doesn't require reading the response.

Therefore:

> **CORS is not a CSRF defense.**

You still need appropriate CSRF protection for cookie-authenticated state-changing requests.

---

# 21. Another common misconception

Someone says:

> "Our API has CORS configured, so we're safe from CSRF."

Not necessarily.

CORS might prevent:

```text
evil.com JavaScript
       ↓
reading bank response
```

But it doesn't automatically prevent:

```text
evil.com
       ↓
causing browser request
       ↓
bank.com
```

That's why:

```text
CORS ≠ CSRF protection
```

---

# 22. Let's connect everything we've learned

We now have:

```text
                 Browser
                    │
       ┌────────────┴─────────────┐
       │                          │
Same-Origin Policy             Cookies
       │                          │
       ↓                          ↓
 Cross-origin                 Automatic
 restrictions                 credentials
       │                          │
       ↓                          ↓
      CORS                       CSRF
       │
       ↓
"Can JS read the response?"
```

And authentication:

```text
Cookie authentication
        │
        ├── HttpOnly
        ├── Secure
        ├── SameSite
        └── CSRF protection

Bearer authentication
        │
        └── Authorization header
```

---

# 23. Hands-on: see the Origin header

Let's return to our Python HTTP server.

Add:

```python
def do_GET(self):
    print("Origin:", self.headers.get("Origin"))

    body = b"Hello"

    self.send_response(200)
    self.send_header("Content-Type", "text/plain")
    self.send_header("Content-Length", str(len(body)))
    self.end_headers()

    self.wfile.write(body)
```

Run:

```bash
python3 server.py
```

Now:

```bash
curl \
  -H "Origin: https://app.example.com" \
  http://localhost:8080/
```

Your server receives:

```http
GET / HTTP/1.1
Host: localhost:8080
Origin: https://app.example.com
```

Notice that `curl` lets you manually construct the header.

The server doesn't automatically know that the request came from a browser.

---

# 24. Add a CORS response

Try:

```python
self.send_header(
    "Access-Control-Allow-Origin",
    "https://app.example.com"
)
```

Now response:

```http
HTTP/1.0 200 OK
Content-Type: text/plain
Access-Control-Allow-Origin: https://app.example.com
Content-Length: 5

Hello
```

That response header is the server telling a browser:

> "JavaScript from this origin may read this response."

---

# 25. The complete request lifecycle

This is the mental model I want you to remember:

```text
JavaScript
   │
   │ fetch()
   ↓
Browser
   │
   │ determines cross-origin?
   ↓
Same-Origin Policy / CORS rules
   │
   ├── simple request
   │       ↓
   │    actual request
   │
   └── needs preflight
           ↓
        OPTIONS
           ↓
      server permission
           ↓
       actual request
   │
   ↓
Server
   │
   ↓
Response
   │
   ↓
Browser checks CORS
   │
   ├── allowed → JS gets response
   │
   └── denied  → JS cannot access response
```

---

## One final distinction

You now have three very different browser-security questions:

| Mechanism              | Main question                                                  |
| ---------------------- | -------------------------------------------------------------- |
| **Same-Origin Policy** | Can one origin freely access another origin's data?            |
| **CORS**               | Has the target server explicitly allowed cross-origin access?  |
| **CSRF protection**    | Can an attacker cause an authenticated state-changing request? |

And three cookie attributes you've learned:

| Attribute  | Main purpose                             |
| ---------- | ---------------------------------------- |
| `HttpOnly` | Prevent JS from directly reading cookie  |
| `Secure`   | Send cookie only over secure connections |
| `SameSite` | Control cross-site cookie sending        |

These are **not interchangeable**.

---

### Next: Lesson 30 — HTTP Caching

We'll move from browser security into another major HTTP capability:

```text
Cache-Control
ETag
Last-Modified
If-None-Match
If-Modified-Since
304 Not Modified
```

We'll build a tiny server and see how the browser can avoid downloading the same resource repeatedly.


---

Absolutely. This is one of the **most important distinctions in web security**, and the confusion usually comes from treating these two questions as the same:

> **"Can another website send a request to my API?"**

vs.

> **"Can another website read the response from my API?"**

They are different.

---

# 1. Start with a concrete example

Suppose you have a banking application:

```text
https://bank.com
```

You log in.

The bank gives your browser a session cookie:

```http
Set-Cookie: session_id=abc123; HttpOnly; Secure
```

Your browser now stores:

```text
bank.com
    ↓
session_id=abc123
```

Whenever your browser makes a request to `bank.com`, it can automatically include:

```http
Cookie: session_id=abc123
```

The bank uses this cookie to identify you.

---

# 2. Now you visit evil.com

You are logged into:

```text
bank.com
```

Then you visit:

```text
evil.com
```

The malicious site contains:

```html
<form action="https://bank.com/transfer" method="POST">
    <input type="hidden" name="to" value="attacker">
    <input type="hidden" name="amount" value="10000">
</form>

<script>
    document.forms[0].submit();
</script>
```

The browser is now being instructed to make:

```http
POST /transfer HTTP/1.1
Host: bank.com
Cookie: session_id=abc123

to=attacker&amount=10000
```

Notice something extremely important:

### The malicious website does NOT need to know `abc123`.

The browser already has the cookie.

The browser may attach it automatically according to cookie rules.

---

# 3. This is CSRF

The bank sees:

```http
POST /transfer
Cookie: session_id=abc123
```

From the bank's perspective:

```text
session_id=abc123
        ↓
User #123
        ↓
Authenticated
        ↓
Transfer $10,000
```

The bank doesn't necessarily know that the request was initiated by:

```text
bank.com
```

rather than:

```text
evil.com
```

That's the fundamental CSRF problem.

---

# 4. Now where does CORS enter?

CORS primarily addresses a **different problem**.

Suppose evil.com does:

```javascript
fetch("https://bank.com/account")
```

The browser sees:

```text
JavaScript origin:
https://evil.com

Target:
https://bank.com
```

That's cross-origin.

The browser uses the CORS rules to determine whether JavaScript running on `evil.com` is allowed to **read the response**.

---

# 5. Imagine there is NO CORS permission

Bank responds:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "balance": 500000
}
```

But bank does not say:

```http
Access-Control-Allow-Origin: https://evil.com
```

The browser says:

```text
Response arrived
       ↓
CORS check
       ↓
evil.com is not allowed
       ↓
Do NOT expose response to evil.com JavaScript
```

So this JavaScript:

```javascript
fetch("https://bank.com/account")
    .then(response => response.json())
    .then(data => {
        console.log(data);
    });
```

cannot simply read the bank's response.

That's good.

---

# 6. But here's the critical part

CORS preventing **response reading** does not necessarily mean the **request never happened**.

Conceptually:

```text
             evil.com
                │
                │ request
                ↓
             bank.com
                │
                │ response
                ↓
             Browser
                │
                X
                │
        evil.com JavaScript
```

The browser can enforce:

```text
"evil.com cannot read this response."
```

without that meaning:

```text
"bank.com never received the request."
```

That's why CORS isn't a CSRF defense.

---

# 7. Let's compare two attacks

## Attack A — Stealing data

Attacker wants:

```text
Your bank balance
```

They try:

```javascript
fetch("https://bank.com/account")
```

They want:

```json
{
    "balance": 500000
}
```

CORS can prevent their JavaScript from reading that response.

So:

```text
CORS
 ↓
controls cross-origin response access
```

---

## Attack B — Performing an action

Attacker wants:

```text
Transfer $10,000
```

They don't care about the response.

They only need:

```http
POST /transfer
Cookie: session_id=abc123
```

If the bank processes the request, the damage is done.

It doesn't matter whether evil.com sees:

```http
HTTP/1.1 200 OK
```

or:

```http
HTTP/1.1 403 Forbidden
```

The important thing was the state-changing request.

So:

```text
CSRF
 ↓
protects state-changing actions
```

---

# 8. This is the key distinction

Think about these two questions:

### Question A

```text
Can evil.com READ bank.com's response?
```

CORS is relevant.

### Question B

```text
Can evil.com CAUSE the browser to send a request to bank.com?
```

CORS is **not the primary defense**.

CSRF defenses are relevant.

---

# 9. Why this is especially dangerous with cookies

Cookies have an important behavior:

```text
Cookie
    ↓
browser-managed
    ↓
automatically attached to matching requests
```

Suppose:

```http
Cookie: session_id=abc123
```

The attacker doesn't need:

```javascript
const cookie = ...
```

They don't need to read it.

The browser does the work.

That's why:

```text
Cookie-based authentication
        +
state-changing requests
        +
missing CSRF protection
```

can create CSRF vulnerabilities.

---

# 10. Compare this with Authorization headers

Consider a bearer-token API:

```http
POST /transfer HTTP/1.1
Host: bank.com
Authorization: Bearer eyJhbGci...
```

Now evil.com creates:

```html
<form action="https://bank.com/transfer" method="POST">
```

The browser doesn't automatically add:

```http
Authorization: Bearer eyJ...
```

because that's not a normal browser cookie.

The attacker doesn't know the token.

So the request reaches the server as something like:

```http
POST /transfer
Host: bank.com

to=attacker&amount=10000
```

The server says:

```text
No Authorization header
        ↓
401 Unauthorized
```

This is one reason bearer tokens sent explicitly in the `Authorization` header have different CSRF characteristics from cookie-based authentication.

---

# 11. But be careful: JWT doesn't magically solve everything

Suppose you store a JWT in:

```text
localStorage
```

and your application does:

```javascript
fetch("/transfer", {
    headers: {
        Authorization: `Bearer ${token}`
    }
});
```

CSRF is different because the browser doesn't automatically attach that `Authorization` header to an attacker's request.

However, now you have another major concern:

```text
XSS
```

If malicious JavaScript executes in your application's origin, it may be able to access the token.

So you're trading one class of risk for another.

This is why security architecture isn't:

```text
JWT = secure
Cookie = insecure
```

It's more nuanced.

---

# 12. Let's look at CORS more precisely

Suppose your API says:

```http
Access-Control-Allow-Origin: https://app.bank.com
```

This means:

```text
JavaScript from app.bank.com
        ↓
allowed to read response
```

It does **not** mean:

```text
Only app.bank.com can send requests.
```

That's the misconception.

A better interpretation is:

> **CORS tells the browser which origins may access the response from JavaScript.**

It is not an authentication mechanism.

---

# 13. CORS does not prove who made the request

Imagine the bank receives:

```http
POST /transfer HTTP/1.1
Host: bank.com
Cookie: session_id=abc123
Origin: https://evil.com
```

The bank should be thinking:

```text
Who is this?
       ↓
session_id

Is this user authorized?
       ↓
authorization checks

Is this request protected against CSRF?
       ↓
CSRF checks
```

CORS isn't the thing that should decide whether the transfer is legitimate.

---

# 14. `Origin` can actually help with CSRF defense

Here's an interesting connection.

Browsers can send:

```http
Origin: https://evil.com
```

The server can inspect it.

For example:

```text
Allowed origins:
https://bank.com
https://app.bank.com
```

Request:

```http
Origin: https://evil.com
```

Server:

```text
Origin isn't trusted
       ↓
Reject request
       ↓
403 Forbidden
```

This can be part of a CSRF defense strategy.

But notice:

```text
Origin checking
        ≠
CORS
```

The same `Origin` header participates in the browser's CORS protocol, but the server can independently use it as part of its security checks.

---

# 15. CSRF token makes the difference very obvious

Suppose the bank gives your legitimate application:

```text
CSRF token:

7f92a1c8...
```

The legitimate frontend sends:

```http
POST /transfer HTTP/1.1
Host: bank.com
Cookie: session_id=abc123
Content-Type: application/json

{
    "to": "123",
    "amount": 100,
    "csrf_token": "7f92a1c8..."
}
```

The attacker can potentially cause:

```http
POST /transfer HTTP/1.1
Host: bank.com
Cookie: session_id=abc123

{
    "to": "attacker",
    "amount": 100
}
```

But they don't have:

```text
csrf_token
```

So:

```text
Session valid
     ↓
YES

CSRF token valid
     ↓
NO

Reject
```

That's CSRF protection.

---

# 16. Why can't evil.com just fetch the CSRF token?

Excellent question.

Suppose the bank has:

```http
GET /transfer-page
```

which returns:

```html
<input type="hidden"
       name="csrf_token"
       value="7f92a1c8...">
```

If evil.com could freely read that response, it could simply do:

```text
GET /transfer-page
       ↓
read CSRF token
       ↓
POST /transfer
       ↓
CSRF token included
```

But the browser's same-origin policy/CORS restrictions prevent arbitrary evil.com JavaScript from reading protected cross-origin responses unless the bank explicitly allows it.

So the defenses work together:

```text
Same-Origin Policy / CORS
        ↓
attacker can't freely read protected data/token

CSRF token
        ↓
attacker can't forge state-changing request

Cookie
        ↓
authenticates legitimate user
```

This is a much better mental model.

---

# 17. A complete attack

Without CSRF protection:

```text
User logs into bank
        ↓
Cookie stored
        ↓
User visits evil.com
        ↓
evil.com creates POST /transfer
        ↓
Browser sends bank cookie
        ↓
Bank authenticates request
        ↓
Transfer happens
```

CORS doesn't necessarily stop this.

---

# 18. With CSRF protection

```text
User logs into bank
        ↓
Cookie stored
        ↓
User visits evil.com
        ↓
evil.com creates POST /transfer
        ↓
Browser may send cookie
        ↓
Bank authenticates user
        ↓
CSRF token missing
        ↓
403 Forbidden
        ↓
Transfer doesn't happen
```

Notice something very important:

**The cookie can still be present.**

Authentication succeeded.

But the request still fails because the application requires another security property.

---

# 19. So what exactly does each mechanism protect?

Here's the table I'd remember for interviews:

| Mechanism            | Protects against / controls                                 |
| -------------------- | ----------------------------------------------------------- |
| Same-Origin Policy   | Cross-origin access by browser JavaScript                   |
| CORS                 | Server-declared permission for cross-origin response access |
| CSRF token           | Forged state-changing requests                              |
| SameSite cookie      | Cross-site cookie sending                                   |
| HttpOnly             | Direct JavaScript access to cookies                         |
| Secure               | Cookie transmission over insecure HTTP                      |
| Authorization header | Explicitly supplied bearer credentials                      |
| Authentication       | Who is the user?                                            |
| Authorization        | What is the user allowed to do?                             |

---

# 20. The simplest mental model

Think of a bank.

### CORS asks:

> **"Can this other website see what the bank told the browser?"**

### CSRF asks:

> **"Can this other website trick the bank into doing something using the victim's identity?"**

That's the distinction.

Or even shorter:

```text
CORS
→ Can I READ the response?

CSRF
→ Can I MAKE an authenticated action happen?
```

That's not a perfect specification-level definition, but it's an excellent mental model.

---

# 21. One final subtlety

Don't interpret:

```text
CORS blocks evil.com
```

as:

```text
evil.com cannot communicate with my API.
```

Instead think:

```text
Browser
   │
   ├── Can request be sent?
   │
   └── Can JavaScript access response?
```

These are separate questions.

For a given request, you need to consider:

```text
┌──────────────────────────────┐
│       Browser Security       │
├──────────────────────────────┤
│ Same-Origin Policy           │
│ CORS                         │
│ Cookies / SameSite           │
│ CSRF                         │
│ Credential rules             │
└──────────────────────────────┘
```

Once you separate **sending**, **reading**, and **authentication**, CORS becomes much less confusing.

### The one sentence to remember

> **CORS controls whether cross-origin JavaScript can access a response; CSRF protection controls whether an attacker can successfully cause an authenticated state-changing action.**

And that's why:

```text
CORS ≠ CSRF protection
```

even though both involve cross-origin browser requests.

---

Yes — **sometimes**, but this is the subtle part.

Your reasoning is correct for **requests that trigger a CORS preflight**. But **not all cross-origin requests require a preflight**.

That's exactly why CORS is not a general CSRF defense.

---

## 1. Your reasoning

Suppose `evil.com` tries:

```javascript
fetch("https://bank.com/transfer", {
    method: "POST",
    headers: {
        "X-CSRF-Attack": "true"
    },
    body: "..."
});
```

Because of the custom header, the browser may first send:

```http
OPTIONS /transfer HTTP/1.1
Host: bank.com
Origin: https://evil.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: x-csrf-attack
```

Bank responds:

```http
HTTP/1.1 403 Forbidden
```

or simply doesn't grant permission.

Then:

```text
OPTIONS
   ↓
CORS permission denied
   ↓
Actual POST is NOT sent
```

So **in this particular case, yes, CORS preflight prevents the attack from reaching the actual POST.**

---

# 2. But here's the important exception

There are requests that **don't require preflight**.

For example, a normal HTML form can submit:

```html
<form action="https://bank.com/transfer" method="POST">
    <input name="to" value="attacker">
    <input name="amount" value="10000">
</form>
```

The browser can send the POST directly:

```http
POST /transfer HTTP/1.1
Host: bank.com
Cookie: session_id=abc123
Content-Type: application/x-www-form-urlencoded

to=attacker&amount=10000
```

There was no:

```http
OPTIONS /transfer
```

first.

Therefore:

```text
evil.com
   ↓
HTML form
   ↓
POST /transfer
   ↓
bank.com
   ↓
cookie attached
```

Potential CSRF.

---

# 3. Why does the browser allow this?

Because HTML forms have historically been allowed to submit cross-origin requests.

Otherwise normal web functionality would break.

For example, websites have always been able to submit forms to other sites.

CORS was designed primarily around controlling **cross-origin programmatic access**, such as JavaScript `fetch()`/XHR, not as a universal "nothing may ever be sent cross-origin" mechanism.

---

# 4. This is the key distinction

Think about these two requests.

### Case A — JavaScript with non-simple request

```javascript
fetch("https://bank.com/transfer", {
    method: "POST",
    headers: {
        "X-Custom-Header": "foo"
    }
});
```

Browser:

```text
fetch()
   ↓
preflight OPTIONS
   ↓
CORS check
   ↓
if rejected → actual request doesn't happen
```

So CORS can prevent this particular attack.

---

### Case B — HTML form

```html
<form action="https://bank.com/transfer" method="POST">
```

Browser:

```text
form submission
   ↓
POST directly
   ↓
Cookie may be attached
   ↓
bank.com processes request
```

No CORS preflight is required.

So:

```text
CORS preflight
      ↓
doesn't happen
      ↓
CSRF can still happen
```

---

# 5. "But can't we force the attacker to use a custom header?"

No.

That's the clever part.

The attacker controls `evil.com`, but they don't control how the victim's browser implements HTML forms.

They can simply use:

```html
<form>
```

instead of:

```javascript
fetch()
```

And forms don't need arbitrary custom headers.

---

# 6. What about JSON?

This is another important detail.

Your API might say:

```text
I only accept JSON.
```

For example:

```http
Content-Type: application/json
```

A JavaScript `fetch()` with JSON generally triggers CORS preflight because `application/json` isn't a CORS-safelisted content type.

So:

```text
evil.com
   ↓
fetch()
   ↓
POST application/json
   ↓
OPTIONS preflight
   ↓
CORS rejection
```

This can make CSRF harder through that particular mechanism.

But you should **not use CORS as your CSRF protection**.

Your security boundary shouldn't be:

> "Our endpoint happens to require JSON, therefore CSRF is impossible."

Instead explicitly protect state-changing requests.

---

# 7. Another important distinction: CORS doesn't know whether the user is authenticated

Imagine:

```http
OPTIONS /transfer HTTP/1.1
Origin: https://evil.com
```

The bank says:

```text
CORS denied.
```

Great.

But an attacker can potentially use another mechanism that doesn't involve a preflight:

```text
<form>
<img>
<a>
navigation
etc.
```

depending on the endpoint and browser behavior.

The fundamental problem remains:

```text
Browser automatically sends credentials
        +
Server accepts state-changing request
        =
potential CSRF
```

---

# 8. This is why CSRF defenses exist separately

For example:

```text
POST /transfer
```

requires:

```text
session cookie
+
CSRF token
```

So even if an attacker gets the browser to send:

```http
POST /transfer
Cookie: session_id=abc123
```

the server checks:

```text
CSRF token?
```

Missing:

```text
403 Forbidden
```

Now the security doesn't depend on whether the request happened to trigger CORS preflight.

---

# 9. Think of preflight as a permission check for certain requests

This mental model is better:

```text
Cross-origin request
        │
        ├── "Simple" request
        │       ↓
        │   No preflight
        │       ↓
        │   Request may be sent
        │
        └── Non-simple request
                ↓
            Preflight
                ↓
        CORS permission check
                ↓
          allowed?
           /     \
         yes      no
          ↓       ↓
       request   blocked
```

Therefore:

> **Preflight protects certain cross-origin requests from being sent without permission. It does not guarantee that every cross-origin state-changing request will be preflighted.**

---

# 10. And this explains the famous statement

When someone says:

> "Our API has CORS configured, so we're safe from CSRF."

The response is:

**Not necessarily.**

Because:

```text
CORS
 │
 ├── controls cross-origin JavaScript access
 │
 └── preflight protects certain non-simple requests
```

while:

```text
CSRF protection
 │
 └── protects state-changing authenticated operations
     regardless of whether the request came through
     a preflighted fetch or another browser mechanism
```

---

## The interview-level answer

If someone asks you:

> **"If CORS preflight rejects evil.com's request, how can CSRF happen?"**

A strong answer is:

> **"Preflight can prevent CSRF through requests that require preflight, but not all cross-origin requests require preflight. For example, an HTML form can submit a cross-origin POST without a CORS preflight, and browser-managed cookies may still be attached. Therefore CORS cannot be relied upon as the application's CSRF defense. State-changing cookie-authenticated endpoints should use defenses such as SameSite cookies, CSRF tokens, and/or appropriate Origin validation."**

That's the distinction I would keep in your mental model:

```text
             CORS
              │
       "Do I need permission
        for this cross-origin
        programmatic request?"
              │
              ↓
          Preflight
              │
        ┌─────┴─────┐
        │           │
      allow       reject
        │           │
     request     no request
                  (for that
                   fetch)

              CSRF
                │
                ↓
      "If an authenticated
       state-changing request
       reaches me, can I prove
       it is legitimate?"
```

That separation makes the whole topic click.


---

Exactly. Let's make this very concrete.

A **CORS preflight happens when the browser decides that a cross-origin request is not a "CORS-safelisted" request**.

The preflight is an `OPTIONS` request sent **before the actual request**.

---

# 1. The basic flow

Suppose:

```text
Frontend:
https://app.example.com

API:
https://api.example.com
```

JavaScript does:

```javascript
fetch("https://api.example.com/users", ...)
```

The browser first asks:

> "Is this cross-origin?"

If no:

```text
same-origin
   ↓
no CORS preflight
```

If yes:

```text
cross-origin
   ↓
Is this request CORS-safelisted?
   │
   ├── YES → usually send actual request directly
   │
   └── NO  → send OPTIONS preflight first
```

---

# 2. What makes a request "simple"?

For practical purposes, think about **three things**:

### ① HTTP method

The method must be one of:

```text
GET
HEAD
POST
```

These are CORS-safelisted methods.

So:

```text
GET     → potentially simple
HEAD    → potentially simple
POST    → potentially simple

PUT     → preflight
PATCH   → preflight
DELETE  → preflight
```

Important: `POST` doesn't automatically mean "no preflight." The headers/content type also matter.

---

# 3. Request headers

The browser allows only certain request headers without preflight.

Common safelisted ones include:

```text
Accept
Accept-Language
Content-Language
Content-Type
Range
```

But `Content-Type` itself has restrictions.

For example:

```javascript
fetch(url, {
    method: "POST",
    headers: {
        "Authorization": "Bearer abc123"
    }
});
```

`Authorization` is **not** a CORS-safelisted request header.

Therefore:

```text
POST
+
Authorization
        ↓
cross-origin
        ↓
preflight
```

Browser sends something like:

```http
OPTIONS /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Method: POST
Access-Control-Request-Headers: authorization
```

---

# 4. Content-Type is particularly important

For a cross-origin `POST`, these content types are CORS-safelisted:

```text
application/x-www-form-urlencoded
multipart/form-data
text/plain
```

For example:

```javascript
fetch("https://api.example.com/users", {
    method: "POST",
    headers: {
        "Content-Type": "application/x-www-form-urlencoded"
    },
    body: "name=Riyaz"
});
```

This can be a simple CORS request.

No preflight is necessarily required.

---

But:

```javascript
fetch("https://api.example.com/users", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify({
        name: "Riyaz"
    })
});
```

`application/json` is **not** one of the safelisted content types.

Therefore:

```text
POST
+
Content-Type: application/json
        ↓
preflight
```

---

# 5. PUT / PATCH / DELETE

Consider:

```javascript
fetch("https://api.example.com/users/123", {
    method: "DELETE"
});
```

`DELETE` is not a CORS-safelisted method.

So:

```text
DELETE
  ↓
cross-origin
  ↓
preflight
```

Browser sends:

```http
OPTIONS /users/123 HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Method: DELETE
```

The API might respond:

```http
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: DELETE
```

Then the browser sends:

```http
DELETE /users/123 HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
```

---

# 6. Custom headers

Suppose:

```javascript
fetch("https://api.example.com/users", {
    headers: {
        "X-Request-ID": "123"
    }
});
```

`X-Request-ID` is not a CORS-safelisted request header.

Therefore:

```text
custom header
      ↓
preflight
```

The browser asks:

```http
OPTIONS /users HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Headers: x-request-id
```

---

# 7. Authorization header

This is especially common in APIs.

You have:

```javascript
fetch("https://api.example.com/profile", {
    headers: {
        "Authorization": "Bearer abc123"
    }
});
```

Because `Authorization` isn't safelisted:

```text
cross-origin
+
Authorization header
        ↓
preflight
```

Browser:

```http
OPTIONS /profile HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Method: GET
Access-Control-Request-Headers: authorization
```

Server:

```http
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Headers: Authorization
```

Then actual request:

```http
GET /profile HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Authorization: Bearer abc123
```

---

# 8. Here's a useful table

Assuming the request is **cross-origin**:

| Request                      | Preflight? |
| ---------------------------- | ---------- |
| `GET /users`                 | Usually no |
| `HEAD /users`                | Usually no |
| `POST` + `text/plain`        | Usually no |
| `POST` + form-urlencoded     | Usually no |
| `POST` + multipart/form-data | Usually no |
| `POST` + `application/json`  | **Yes**    |
| `GET` + `Authorization`      | **Yes**    |
| `GET` + custom header        | **Yes**    |
| `PUT`                        | **Yes**    |
| `PATCH`                      | **Yes**    |
| `DELETE`                     | **Yes**    |

The word **usually** matters because CORS has some additional safelisting rules around header values, `Content-Type` parameters, and other details.

---

# 9. Very important: same-origin requests don't need CORS preflight

Suppose:

```text
Frontend:
https://app.example.com

API:
https://app.example.com/api/users
```

Same origin.

Even if you do:

```javascript
fetch("/api/users", {
    method: "DELETE"
});
```

there's no CORS problem.

Therefore:

```text
same-origin
    ↓
CORS isn't relevant
    ↓
no CORS preflight
```

---

# 10. The browser makes the decision

This is also important.

Your JavaScript doesn't explicitly say:

```javascript
preflight();
```

You don't normally write:

```javascript
OPTIONS /users
```

The browser automatically decides.

You write:

```javascript
fetch("https://api.example.com/users", {
    method: "PUT",
    headers: {
        "Content-Type": "application/json"
    },
    body: "..."
});
```

Browser internally decides:

```text
Different origin?
       ↓
YES

Is request CORS-safelisted?
       ↓
NO

Send preflight
       ↓
OPTIONS
```

---

# 11. What exactly is in a preflight?

Suppose your code is:

```javascript
fetch("https://api.example.com/users/123", {
    method: "PATCH",
    headers: {
        "Authorization": "Bearer abc",
        "Content-Type": "application/json",
        "X-Request-ID": "123"
    },
    body: JSON.stringify({
        name: "Riyaz"
    })
});
```

The browser might send:

```http
OPTIONS /users/123 HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Access-Control-Request-Method: PATCH
Access-Control-Request-Headers: authorization, content-type, x-request-id
```

Notice that it is basically asking:

> "Can `https://app.example.com` make a PATCH request to this resource using these headers?"

---

# 12. Server says yes

```http
HTTP/1.1 204 No Content
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Methods: PATCH
Access-Control-Allow-Headers: Authorization, Content-Type, X-Request-ID
```

Then:

```http
PATCH /users/123 HTTP/1.1
Host: api.example.com
Origin: https://app.example.com
Authorization: Bearer abc
Content-Type: application/json
X-Request-ID: 123

{"name":"Riyaz"}
```

---

# 13. Server says no

Suppose server responds:

```http
HTTP/1.1 403 Forbidden
```

or doesn't provide the required CORS headers.

Then:

```text
OPTIONS
   ↓
CORS permission denied
   ↓
Browser doesn't send the actual PATCH
```

This is the case you were thinking about in the previous question.

And **yes, in this particular situation, the preflight can prevent the state-changing request from being sent.**

---

# 14. But now connect it back to CSRF

Here's why the distinction matters.

### Attack 1

Attacker tries:

```javascript
fetch("https://bank.com/transfer", {
    method: "POST",
    headers: {
        "Content-Type": "application/json"
    }
});
```

Likely:

```text
POST JSON
   ↓
preflight
   ↓
CORS denied
   ↓
actual POST doesn't happen
```

Good.

### Attack 2

Attacker uses:

```html
<form action="https://bank.com/transfer" method="POST">
```

with:

```text
application/x-www-form-urlencoded
```

This can be a CORS-safelisted request.

So:

```text
form
 ↓
POST
 ↓
no preflight
 ↓
request may reach bank
 ↓
cookie may be attached
 ↓
potential CSRF
```

This is why **preflight ≠ CSRF protection**.

---

# 15. The simplest rule to remember

When you see a cross-origin request, ask:

### Step 1

```text
Is it cross-origin?
```

If no → no CORS preflight.

### Step 2

If yes:

```text
Is it a CORS-safelisted request?
```

If yes:

```text
No preflight
```

If no:

```text
OPTIONS preflight
```

Then:

```text
Server grants permission?
       │
   ┌───┴───┐
  YES      NO
   │        │
actual    blocked
request
```

---

## The mental picture

```text
                 Cross-origin request
                         │
                         ▼
                Is it CORS-safelisted?
                    /           \
                  YES            NO
                   │              │
                   │          OPTIONS
                   │          preflight
                   │              │
                   │       CORS permission?
                   │          /       \
                   │        YES       NO
                   │         │         │
                   ▼         ▼         ▼
              Actual      Actual     Blocked
              request     request
```

And the **big CSRF lesson** is:

> **Only some cross-origin requests get this preflight checkpoint. A request that qualifies as a CORS-safelisted request can go directly to the server, which is why applications still need explicit CSRF defenses when using cookie-based authentication.**


---

# Lesson 30 — HTTP Caching

Now let's move to one of the most useful parts of HTTP:

> **How can the browser avoid downloading the same resource repeatedly?**

Imagine:

```text
Browser
   │
   │ GET /profile
   ↓
Server
   │
   │ 50 KB response
   ↓
Browser
```

You refresh.

Without caching:

```text
Browser ── GET /profile ──→ Server
Browser ←── 50 KB ───────── Server
```

Refresh again:

```text
Browser ── GET /profile ──→ Server
Browser ←── 50 KB ───────── Server
```

If the data hasn't changed, we're wasting:

* network bandwidth
* server CPU
* latency
* battery
* money at scale

HTTP caching gives us mechanisms to avoid that.

---

# 1. The simplest possible cache

Suppose:

```http
GET /users/123
```

returns:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
    "id": 123,
    "name": "Riyaz"
}
```

The browser could remember:

```text
/users/123
        ↓
{
    "id": 123,
    "name": "Riyaz"
}
```

Next time:

```text
GET /users/123
```

the browser might simply use its cached response.

No network request.

```text
Browser
   │
   ├── cache hit
   │
   └── return cached response
```

This is the fastest possible scenario.

---

# 2. How does the browser know whether it can use the cache?

The server can tell the browser:

```http
Cache-Control: max-age=60
```

For example:

```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: max-age=60

{
    "id": 123,
    "name": "Riyaz"
}
```

This means roughly:

> "This response can be considered fresh for 60 seconds."

So:

```text
t = 0
GET /users/123
       ↓
200 OK
Cache-Control: max-age=60
       ↓
Browser caches response
```

At:

```text
t = 30 seconds
```

browser requests the same resource.

It can use the cached response:

```text
Browser
   │
   └── cache hit
          ↓
       response
```

No server request necessary.

---

# 3. What happens after 60 seconds?

The cached response becomes **stale**.

That doesn't necessarily mean:

> "Delete it immediately."

It means:

> "Don't blindly assume this cached response is still fresh."

The browser may contact the server again.

```text
Cache
  │
  │ stale
  ↓
Server
```

This leads to a very useful optimization.

---

# 4. The problem with simply downloading it again

Suppose we have:

```text
1 MB image
```

The browser cached it.

One minute later:

```text
GET /image.jpg
```

The server's image hasn't changed.

But if we simply download it again:

```text
1 MB
```

was transferred unnecessarily.

HTTP gives us **conditional requests**.

The browser can ask:

> "Has this resource changed since the version I have?"

---

# 5. ETag

The server can return:

```http
HTTP/1.1 200 OK
Content-Type: application/json
ETag: "abc123"

{
    "id": 123,
    "name": "Riyaz"
}
```

Think of the ETag as a version identifier for the representation.

Conceptually:

```text
Resource
   ↓
ETag = "abc123"
```

The browser caches:

```text
/users/123
   ↓
body
ETag: "abc123"
```

---

# 6. Browser comes back later

Suppose the cache is stale.

Browser sends:

```http
GET /users/123 HTTP/1.1
Host: api.example.com
If-None-Match: "abc123"
```

This means:

> "I already have version `abc123`. Has it changed?"

The server checks the current representation.

Suppose it hasn't changed.

Server responds:

```http
HTTP/1.1 304 Not Modified
ETag: "abc123"
```

Notice:

**There is no response body.**

---

# 7. Why 304 is useful

The browser already has:

```json
{
    "id": 123,
    "name": "Riyaz"
}
```

So the server doesn't need to send it again.

Instead:

```text
Browser
  │
  │ "Is abc123 still current?"
  ↓
Server
  │
  │ "Yes"
  ↓
304 Not Modified
```

The browser reuses its cached body.

Instead of:

```text
1 MB response
```

you might transfer only the request + tiny response headers.

Huge savings at scale.

---

# 8. This gives us two different caching concepts

### Freshness

```http
Cache-Control: max-age=60
```

answers:

> "How long can I use this cached response without asking the server?"

### Validation

```http
ETag: "abc123"
```

answers:

> "If my cached response is stale, can I ask the server whether it's still valid?"

These are complementary.

---

# 9. Complete lifecycle

Imagine:

```http
GET /users/123
```

Server:

```http
HTTP/1.1 200 OK
Cache-Control: max-age=60
ETag: "abc123"

{
    "id": 123,
    "name": "Riyaz"
}
```

Browser:

```text
cache:
    URL → response
    freshness → 60 seconds
    ETag → abc123
```

For the next 60 seconds:

```text
GET /users/123
       ↓
fresh cache
       ↓
use cache
```

After 60 seconds:

```text
GET /users/123
       ↓
stale cache
       ↓
send:
If-None-Match: "abc123"
```

Server:

```text
Has resource changed?
       ↓
NO
       ↓
304 Not Modified
```

Browser:

```text
reuse cached body
```

---

# 10. What if the resource DID change?

Suppose the server now has:

```json
{
    "id": 123,
    "name": "Mohammed"
}
```

with:

```text
ETag: "xyz789"
```

Browser sends:

```http
If-None-Match: "abc123"
```

Server sees:

```text
abc123 ≠ xyz789
```

So:

```http
HTTP/1.1 200 OK
ETag: "xyz789"
Content-Type: application/json

{
    "id": 123,
    "name": "Mohammed"
}
```

Browser replaces its cached response.

---

# 11. Why is the status `304` instead of `200`?

Because the server isn't sending a new representation.

It is effectively saying:

> "Your cached representation is still valid."

So:

```text
200 OK
→ here is a representation

304 Not Modified
→ your cached representation is still valid
```

This is why `304` has no response body.

---

# 12. Another validation mechanism: Last-Modified

Instead of an ETag, a server can provide:

```http
Last-Modified: Fri, 18 Sep 2026 10:00:00 GMT
```

Browser later sends:

```http
If-Modified-Since: Fri, 18 Sep 2026 10:00:00 GMT
```

Server checks:

```text
Has resource changed since then?
```

If not:

```http
304 Not Modified
```

---

# 13. ETag vs Last-Modified

Think:

```text
ETag
→ version/representation identifier

Last-Modified
→ timestamp
```

Example:

```http
ETag: "abc123"
Last-Modified: Fri, 18 Sep 2026 10:00:00 GMT
```

A server can provide both.

ETags are generally more precise because timestamps have limitations.

---

# 14. Why not just use timestamps?

Imagine:

```text
10:00:00
```

resource changes.

Then:

```text
10:00:00
```

again due to timestamp resolution or filesystem/application behavior.

A timestamp isn't necessarily a perfect representation identity.

ETag gives the server a more direct way to identify a particular representation.

---

# 15. Cache-Control

`Cache-Control` is one of the most important HTTP response headers.

You've already seen:

```http
Cache-Control: max-age=60
```

Some important directives:

```text
max-age
no-cache
no-store
public
private
must-revalidate
```

Let's understand them carefully.

---

# 16. `max-age`

```http
Cache-Control: max-age=60
```

Means approximately:

> The response can be considered fresh for 60 seconds.

Example:

```text
t=0
   GET
   ↓
   response
   ↓
   cache for 60 sec

t=30
   cache is fresh

t=60+
   cache becomes stale
```

---

# 17. `no-store`

This is stronger:

```http
Cache-Control: no-store
```

It means:

> Don't store this response in a cache.

Common example:

```text
sensitive/private response
```

For example:

```http
GET /bank/account
```

An application might choose:

```http
Cache-Control: no-store
```

depending on its security requirements.

---

# 18. `no-cache`

This one causes confusion.

People often think:

```http
Cache-Control: no-cache
```

means:

> "Don't cache this."

That's not quite right.

`no-cache` essentially means:

> **You may store the response, but you must validate it with the server before reusing it when required.**

So:

```text
no-store
→ don't store

no-cache
→ can store, but must revalidate
```

This distinction is important.

---

# 19. `private`

```http
Cache-Control: private
```

means the response is intended for a **private cache**, such as the user's browser, rather than a shared cache.

Useful for responses that depend on the individual user.

For example:

```text
GET /my-profile
```

might be:

```http
Cache-Control: private
```

because you don't want a shared intermediary cache to serve Riyaz's response to another user.

---

# 20. `public`

```http
Cache-Control: public
```

indicates the response can be stored by shared caches.

Useful for things like:

```text
static assets
public images
public API responses
```

depending on the application.

---

# 21. Important distinction: browser cache vs server cache

When we say:

```text
HTTP caching
```

don't think only about the browser.

There can be:

```text
Browser
   ↓
CDN
   ↓
Reverse proxy
   ↓
Application
   ↓
Database
```

Caching can happen at multiple layers.

For example:

```text
Browser cache
        ↓
CDN cache
        ↓
Application
```

A CDN can serve:

```text
GET /images/logo.png
```

without contacting your application server at all.

---

# 22. This is why HTTP caching is powerful

Imagine:

```text
1,000,000 users
```

request:

```text
GET /logo.png
```

Without caching:

```text
1,000,000 requests
        ↓
Application
```

With a CDN:

```text
1,000,000 requests
        ↓
CDN
        ↓
cached logo
```

The application may receive almost none of those requests.

HTTP caching isn't merely a browser optimization.

It is an important distributed-systems mechanism.

---

# 23. A useful mental model

Think of caching as two questions.

### Question 1 — Freshness

```text
Can I use what I already have?
```

Controlled by things like:

```http
Cache-Control: max-age=60
```

### Question 2 — Validation

```text
If I can't blindly use it,
has the resource actually changed?
```

Controlled by:

```http
ETag
If-None-Match
```

or:

```http
Last-Modified
If-Modified-Since
```

---

# 24. Full picture

```text
                  GET /users/123
                        │
                        ▼
                    Browser
                        │
                 Cache available?
                   /          \
                 YES           NO
                  │             │
             Is it fresh?       │
              /      \          │
            YES       NO        │
             │         │        │
             ▼         ▼        ▼
          use cache  conditional request
                         │
                 If-None-Match
                         │
                         ▼
                       Server
                       /    \
                    same    changed
                     │         │
                     ▼         ▼
                    304       200
                     │         │
                     ▼         ▼
                use cache   new body
```

---

# 25. One HTTP detail worth noticing

You started this course by learning:

```http
GET /users/123 HTTP/1.1
Host: example.com
```

Now look how much HTTP semantics we've accumulated around that same request:

```http
GET /users/123 HTTP/1.1
Host: example.com
Accept: application/json
If-None-Match: "abc123"
```

Response:

```http
HTTP/1.1 304 Not Modified
ETag: "abc123"
Cache-Control: max-age=60
```

The application didn't need a special caching API.

**HTTP itself provides the protocol-level semantics.**

That's one of the reasons understanding HTTP deeply is useful even when you're working with Spring, Node, Django, FastAPI, etc.

---

## The key things to remember

```text
Cache-Control
    ↓
How caching/freshness should work

ETag
    ↓
Identifier for a representation

If-None-Match
    ↓
"Is my cached ETag still valid?"

304
    ↓
"Yes, reuse your cached response"

Last-Modified
    ↓
When representation was last modified

If-Modified-Since
    ↓
"Has it changed since this time?"
```

And the most important distinction:

```text
Fresh cache
    ↓
No request needed

Stale cache
    ↓
Maybe validate

Validation succeeds
    ↓
304 + reuse cached body

Validation fails
    ↓
200 + new representation
```

Next, we'll go deeper into **HTTP caching semantics and `Cache-Control`**, including `no-cache` vs `no-store`, browser cache vs CDN cache, cache invalidation, and why caching authenticated API responses can become dangerous.


---
