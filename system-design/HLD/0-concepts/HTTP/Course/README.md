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
