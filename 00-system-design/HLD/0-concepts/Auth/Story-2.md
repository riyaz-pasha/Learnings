Yes. This is a very good topic to learn **historically and causally**, rather than as a collection of OAuth/JWT definitions.

We’ll treat it like an evolving engineering story:

> **People built system A → it solved problem X → then problem Y appeared → they invented B → B created problem Z → then C...**

And we’ll go **one concept at a time**. I’ll stop at natural boundaries rather than dumping OAuth, JWT, refresh tokens, OpenID Connect, etc. all at once.

## The learning journey

The eventual story will roughly look like:

```text
A user logs into a website
        ↓
How does the server know who they are?
        ↓
Authentication
        ↓
How does the server remember them?
        ↓
Sessions + Cookies
        ↓
Now we have multiple applications/services
        ↓
"Don't make every app know the user's password"
        ↓
Delegation
        ↓
OAuth
        ↓
"How does the API know what the client is allowed to do?"
        ↓
Access Tokens
        ↓
"Let's make tokens self-contained"
        ↓
JWT
        ↓
"JWTs don't naturally expire/revoke the way we want"
        ↓
Refresh Tokens + rotation + revocation
        ↓
"Authentication ≠ authorization"
        ↓
Scopes, roles, claims, policies
        ↓
"Login with Google isn't quite OAuth"
        ↓
OpenID Connect
        ↓
Modern architectures
        ↓
SPA/mobile/backend/microservices/service-to-service
        ↓
PKCE, BFF, token exchange, mTLS, DPoP, etc.
```

We'll also repeatedly ask a crucial question:

> **What guarantee does this mechanism actually provide?**

Because this is where people often get confused.

For example:

* A password proves something about **knowledge**.
* A session cookie proves possession of a **session credential**.
* An access token is an **authorization credential**.
* A JWT being signed does **not** mean the user is trustworthy.
* OAuth does **not** mean "authentication."
* HTTPS does **not** authenticate the user.
* A refresh token does **not** give an API permission to access your resources in the same way an access token does.

Those distinctions become much clearer when we build them from first principles.

---

# Part 1 — Why Authentication exists at all

Let's forget OAuth, JWT, cookies, tokens, everything.

Imagine we build the world's simplest website.

```text
Browser
   |
   |  GET /my-account
   |
   v
Server
```

The server receives:

```http
GET /my-account
```

The server has a database:

```text
Users

id    name       email
-----------------------------
101   Alice      alice@example.com
102   Bob        bob@example.com
```

Alice asks:

```http
GET /my-account
```

The server thinks:

> "Okay... which user is making this request?"

And that's our first fundamental problem.

---

## Problem #1: The server doesn't know who is talking

An HTTP request basically gives us something like:

```http
GET /my-account HTTP/1.1
Host: example.com
```

The server knows:

> Somebody requested `/my-account`.

But it does **not** inherently know:

> Alice requested `/my-account`.

The server needs some evidence.

So we introduce the first concept:

# Authentication

Authentication answers:

> **"Who are you?"**

More precisely:

> **Can I verify an identity claim made by the requester?**

For example:

```text
Client: "I am Alice."

Server: "Prove it."

Client: "Here is something only Alice should know."

Server: "That checks out. You are Alice."
```

That's authentication.

---

# The first obvious solution: username + password

Suppose Alice registered:

```text
username = alice
password = swordfish
```

Now:

```text
Alice
  |
  | username=alice
  | password=swordfish
  v
Server
  |
  | look up Alice
  | verify password
  v
Authenticated
```

The server can store a password verifier and compare what Alice supplies.

So we solved our first problem.

But notice something important.

### Authentication isn't yet authorization.

Suppose Alice successfully proves she's Alice.

Does that mean she can:

```text
DELETE /users/102
```

?

Not necessarily.

The server may say:

```text
Alice
   ↓
Authenticated? YES
   ↓
Allowed to delete Bob? NO
```

So we already have two separate questions:

```text
Authentication
    ↓
Who are you?

Authorization
    ↓
What are you allowed to do?
```

This distinction is foundational, and we'll keep returning to it.

---

# But now another problem appears

Imagine Alice visits:

```text
https://example.com
```

and submits:

```http
POST /login

username=alice
password=swordfish
```

The server verifies it.

Great.

Alice then clicks:

```text
My Account
```

Browser sends:

```http
GET /my-account
```

And we're back to the original problem.

The second request doesn't contain:

```text
username=alice
password=swordfish
```

So the server asks:

> "Who are you again?"

We could send the username/password on **every request**.

Like:

```http
GET /my-account

Authorization: alice:swordfish
```

Then:

```http
GET /orders
Authorization: alice:swordfish
```

Then:

```http
GET /settings
Authorization: alice:swordfish
```

Technically, this could work.

But we've created a horrible new problem.

---

# Problem #2: We don't want to repeatedly expose the primary credential

Your password is your **long-term identity credential**.

If every request carries it around, then every component touching the request becomes a potential place where that credential can leak.

Imagine:

```text
Browser
   |
   | password
   v
Load Balancer
   |
   v
Web Server
   |
   v
Logging middleware
   |
   v
Reverse proxy
   |
   v
Application
```

If something logs:

```text
Authorization: alice:swordfish
```

you've leaked Alice's password.

And there's another problem.

Imagine a third-party application wants to access something on Alice's behalf.

For example:

```text
Alice uses Calendar App
Alice also uses Photo App
Alice wants Calendar App to access her photos
```

We don't want Alice to say:

> "Here is my photo-service username and password. I trust you!"

Because the Calendar application now has Alice's **master credential**.

That means Calendar could potentially impersonate Alice everywhere.

This leads to a much deeper requirement:

> **Can I give someone limited access without giving them my primary credential?**

That question is one of the major roads eventually leading to OAuth.

But we're **not going there yet**.

---

# Another problem: How does the server remember login state?

Suppose Alice logs in once:

```text
username = alice
password = swordfish
```

Authentication succeeds.

Ideally, Alice shouldn't have to type the password for every request.

So the server needs some way to say:

> "I've already authenticated you. For the next requests, I'll recognize you."

Conceptually:

```text
              LOGIN
Alice ----------------------> Server
      username + password

Alice <---------------------- Server
           "Authenticated"
```

Now future requests:

```text
Alice ----------------------> Server
          ????

Server:
"How do I know this is still Alice?"
```

We need some **ongoing credential**.

And this is where an important historical mechanism appears:

# Sessions

The server can create a session:

```text
session_id = 8f92a7...
```

and maintain:

```text
Session Store

session_id       user
--------------------------------
8f92a7...        Alice
71ab3c...        Bob
```

Then the server gives Alice something identifying that session.

Typically:

```text
Set-Cookie: session_id=8f92a7...
```

The browser stores it.

Later:

```http
GET /my-account
Cookie: session_id=8f92a7...
```

The server receives:

```text
session_id=8f92a7...
        ↓
look up session
        ↓
Alice
```

So:

```text
Password
   ↓
used during login
   ↓
Session
   ↓
used for subsequent requests
```

This is a huge conceptual step.

---

# What guarantee does a session provide?

This is where I want you to be precise.

A session does **not** magically prove:

> "This request definitely came from Alice."

It proves something closer to:

> "This request possesses a valid session credential that the server associates with Alice."

That's a subtle but extremely important distinction.

Suppose somebody steals Alice's session cookie:

```text
Cookie: session_id=8f92a7...
```

They can send:

```http
GET /my-account
Cookie: session_id=8f92a7...
```

The server may say:

```text
session_id → Alice
```

Therefore:

```text
Attacker becomes Alice
```

from the server's perspective.

This is called **session hijacking**.

So we've already learned an important security principle:

> **Authentication credentials are valuable because possession of them can be enough to act as the authenticated principal.**

That principle will show up repeatedly with:

* cookies
* bearer access tokens
* refresh tokens
* JWTs
* API keys

---

# Why cookies?

You might reasonably ask:

> Why not simply put `session_id` into every request ourselves?

You could.

For example:

```http
GET /my-account?session_id=8f92a7
```

But browsers have a built-in mechanism specifically designed to carry state between requests:

```http
Cookie: session_id=8f92a7...
```

Cookies also gave web applications useful security controls such as:

```text
Secure
HttpOnly
SameSite
Domain
Path
Expiration
```

We'll later go deeply into why these exist, because each one addresses a particular problem.

---

# So where are we now?

We've reached a stable architecture:

```text
                 First time
                    │
                    ▼
            username + password
                    │
                    ▼
                Server
                    │
              authenticate
                    │
                    ▼
             create session
                    │
                    ▼
                Cookie
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
   GET /account            GET /orders
   Cookie: S1              Cookie: S1
        │                       │
        └───────────┬───────────┘
                    ▼
                  Server
                    │
              S1 → Alice
                    │
                    ▼
               Alice's data
```

This works extremely well.

And importantly:

> **You do not need OAuth or JWT to build a secure traditional web application.**

A normal server-rendered application can happily use:

```text
Username/password
+
Server-side session
+
Secure cookie
+
HTTPS
```

for its entire life.

That's an important correction to the common modern misconception that "authentication means JWT."

It doesn't.

---

# But the story gets interesting now

The session model has a fundamental architectural property:

> **The server owns the state.**

For example:

```text
session_id = ABC123

Server database/cache:

ABC123 → Alice
ABC124 → Bob
ABC125 → Charlie
```

That's called a **stateful session**.

And that was perfectly reasonable when applications looked roughly like:

```text
Browser
   |
   v
One application
   |
   v
One backend
   |
   v
One session store
```

But software architecture started changing.

Applications became:

```text
Browser
   |
   +-------- Web App
   |
   +-------- Mobile App
   |
   +-------- API
   |
   +-------- Different services
   |
   +-------- Third-party integrations
```

And then people wanted things like:

```text
Google Calendar
     |
     | access my Google data
     v
Third-party application
```

Now we have a new problem:

> **How do we securely let one application access another application's resources on behalf of a user, without handing over the user's password?**

That problem is the doorway into **delegated authorization** and eventually **OAuth**.

But before we move there, I want you to be completely comfortable with the first layer.

---

# The mental model to keep

For now, remember only this:

```text
Authentication
    =
    "Who are you?"

Authorization
    =
    "What are you allowed to do?"

Password
    =
    primary authentication credential

Session
    =
    server-maintained authenticated state

Cookie
    =
    one common mechanism for carrying the session credential

Session ID
    =
    identifier/credential associated with authenticated state
```

And the security chain is:

```text
Password
   ↓
authenticate once
   ↓
Session created
   ↓
Session credential carried by browser
   ↓
Server maps credential → user
```

The key vulnerability is:

```text
Steal the session credential
        ↓
Possess the credential
        ↓
Server may treat attacker as user
```

So already, our story has produced the central recurring problem of authentication systems:

> **How do we securely issue, transport, validate, expire, revoke, and constrain credentials?**

Almost everything we'll study later is, in one way or another, an answer to some variation of that question.

---

## Before we move to OAuth

The next step should be **not OAuth yet**.

I recommend we next go deeply into:

**Sessions + Cookies + HTTPS**

because this gives you the foundation for understanding why later systems introduce:

* bearer tokens
* access tokens
* expiration
* refresh tokens
* JWT
* CSRF
* XSS
* SameSite
* revocation
* token storage

Once that foundation is solid, OAuth becomes much easier to understand as an answer to a real problem rather than as a protocol to memorize.

We can take that next as the **"why did sessions/cookies become complicated?"** chapter.

---

Good. Now we’ll stay with the **session + cookie model** and push it until we understand exactly why each security feature exists.

# Part 2 — Sessions, Cookies, HTTPS, and the problems they created

We ended with:

```text
Alice
  |
  | username + password
  v
Server
  |
  | authenticate
  v
session_id = ABC123
  |
  v
Cookie
```

Then:

```http
GET /account
Cookie: session_id=ABC123
```

Server:

```text
ABC123 → Alice
```

This is a **stateful session**.

It works.

So naturally, engineers asked:

> "What can go wrong?"

And quite a lot.

---

# Problem 1: Someone can steal the session ID in transit

Imagine we're in a coffee shop.

Alice logs into:

```text
http://example.com
```

and sends:

```http
POST /login

username=alice
password=secret
```

The request travels roughly through:

```text
Browser
   ↓
Wi-Fi
   ↓
Router
   ↓
ISP
   ↓
Internet
   ↓
Server
```

If communication is unencrypted, someone positioned on the network may be able to observe it.

They could potentially see:

```text
username=alice
password=secret
```

But even after login, the problem continues.

Alice sends:

```http
GET /account
Cookie: session_id=ABC123
```

An attacker who captures:

```text
ABC123
```

can potentially replay it:

```http
GET /account
Cookie: session_id=ABC123
```

The server doesn't know:

> "This is the attacker."

It sees:

```text
ABC123 → Alice
```

So we have our first major realization:

> **The session ID itself is a credential.**

This is important.

A session ID may look harmless:

```text
ABC123
```

but functionally it is:

```text
"Whoever possesses this can act as Alice."
```

This is the **bearer credential** idea.

We'll later see the exact same idea with:

```text
Bearer access tokens
```

---

# The solution: encrypt the communication

This is where **HTTPS/TLS** enters.

Instead of:

```text
HTTP
```

we use:

```text
HTTPS
```

Conceptually:

```text
Browser <====== encrypted ======> Server
```

Now someone sniffing traffic sees something like:

```text
8f 23 a7 91 ...
```

rather than:

```text
Cookie: session_id=ABC123
```

The goal is not merely "encryption."

TLS provides several important properties.

## 1. Confidentiality

An attacker observing the connection shouldn't be able to read the application data.

```text
Alice ───── encrypted ───── Server

Attacker sees ciphertext
```

## 2. Integrity

An attacker shouldn't be able to silently modify data while it travels.

For example:

```text
Alice sends:

amount=100
```

An attacker shouldn't be able to change it to:

```text
amount=100000
```

without the server detecting tampering.

## 3. Authentication of the server

This one is extremely important.

Suppose Alice visits:

```text
https://bank.com
```

How does her browser know it's actually talking to the real bank?

TLS certificates and the browser's certificate validation process provide a mechanism for the browser to authenticate the server's identity.

So HTTPS isn't simply:

> "Encrypt HTTP."

It gives us a channel with:

```text
confidentiality
+
integrity
+
server authentication
```

We'll later revisit this when discussing OAuth because developers sometimes incorrectly say:

> "OAuth is secure because HTTPS encrypts the token."

HTTPS is important, but it solves a **different layer of the problem**.

---

# But HTTPS didn't solve everything

Suppose Alice's browser is already communicating securely:

```text
HTTPS
```

An attacker isn't sniffing the connection.

Can the session still be stolen?

Yes.

Imagine malicious JavaScript executing inside Alice's page.

For example:

```javascript
document.cookie
```

If the session cookie is accessible to JavaScript, malicious code may be able to read it.

Then:

```text
session_id = ABC123
```

gets stolen.

HTTPS doesn't help here.

Why?

Because the attacker isn't between Alice and the server.

The attacker is **inside Alice's browser execution environment**.

This leads us to another major problem.

---

# Problem 2: JavaScript can access the session cookie

Suppose:

```http
Set-Cookie: session_id=ABC123
```

The browser stores it.

By default, cookies can potentially be exposed to JavaScript.

Imagine the application has an XSS vulnerability:

```html
<script>
    // malicious code
</script>
```

The malicious script could potentially do:

```javascript
document.cookie
```

and obtain:

```text
session_id=ABC123
```

Then the attacker can send that credential to their own server.

Now:

```text
Attacker
   |
   | stolen session ID
   v
Server
   |
   | ABC123 → Alice
   v
Alice's account
```

So engineers needed another defense.

---

# `HttpOnly`

A cookie can be marked:

```http
Set-Cookie: session_id=ABC123; HttpOnly
```

Meaning, conceptually:

> "This cookie is for HTTP-level cookie handling; normal JavaScript should not be able to read it."

Then:

```javascript
document.cookie
```

doesn't expose that cookie.

Notice what this does **not** mean.

It does not mean:

> "XSS is impossible."

The malicious JavaScript may still perform requests from the browser.

For example:

```javascript
fetch("/transfer", {
    method: "POST"
});
```

The browser may automatically attach the session cookie.

So:

```text
HttpOnly
```

primarily helps protect against **cookie theft through JavaScript**.

It does not magically eliminate XSS.

That's a recurring theme in security:

> **One defense generally solves one class of problem, not all problems.**

---

# Problem 3: We don't want the cookie sent over HTTP

Suppose Alice visits:

```text
http://example.com
```

instead of:

```text
https://example.com
```

The browser might send:

```http
Cookie: session_id=ABC123
```

over an unencrypted connection.

We're back to the original problem.

So we tell the browser:

> "Never send this cookie over an insecure connection."

That's the purpose of:

```http
Secure
```

Example:

```http
Set-Cookie: session_id=ABC123; Secure
```

Now the browser is instructed to send the cookie only over a secure channel such as HTTPS.

So:

```text
HttpOnly
   ↓
protect against JavaScript reading cookie

Secure
   ↓
protect against sending cookie over insecure HTTP
```

Two different problems.

---

# Problem 4: CSRF

Now we encounter one of the most important concepts in web authentication.

Imagine Alice is logged into:

```text
bank.com
```

Her browser has:

```text
Cookie: session_id=ABC123
```

Now Alice visits a malicious website:

```text
evil.com
```

That page contains something that causes her browser to make:

```http
POST https://bank.com/transfer
```

The browser may automatically attach:

```http
Cookie: session_id=ABC123
```

So Bank sees:

```text
POST /transfer
Cookie: session_id=ABC123
```

and thinks:

> "This request came from Alice's authenticated browser."

But Alice didn't intentionally initiate that transaction.

The malicious website caused the request.

This is:

# CSRF — Cross-Site Request Forgery

The key insight is subtle:

> **The browser automatically sends the credential because cookies are attached to requests based on the destination.**

The attacker doesn't necessarily need to **steal** the cookie.

They can sometimes **cause the victim's browser to use it**.

Compare the two attacks:

### Session theft

```text
Attacker
   ↓
steals cookie
   ↓
uses cookie themselves
```

### CSRF

```text
Attacker
   ↓
tricks victim's browser
   ↓
browser automatically sends cookie
   ↓
server sees authenticated request
```

Very different attacks.

---

# Engineers needed another mechanism

One classic defense is a **CSRF token**.

The server gives Alice:

```text
csrf_token = XYZ789
```

The page contains:

```html
<form>
    ...
    <input type="hidden"
           name="csrf_token"
           value="XYZ789">
</form>
```

When Alice submits:

```http
POST /transfer

csrf_token=XYZ789
amount=100
```

the server verifies:

```text
CSRF token correct?
    YES → continue
    NO  → reject
```

Why does this help?

The malicious site may be able to cause Alice's browser to send:

```text
Cookie: session_id=ABC123
```

but it doesn't necessarily know:

```text
csrf_token=XYZ789
```

So it cannot construct a valid request.

Conceptually:

```text
Authentication credential:
    Cookie

Request authenticity proof:
    CSRF token
```

That separation is useful.

---

# Then browsers evolved further: SameSite

Cookies eventually gained another important attribute:

```text
SameSite
```

For example:

```http
Set-Cookie: session_id=ABC123; SameSite=Lax
```

The browser can use SameSite rules to restrict when cookies are sent in cross-site contexts.

This reduces many CSRF scenarios.

Common values:

```text
Strict
Lax
None
```

Rough intuition:

```text
Strict
   ↓
very restrictive cross-site cookie sending

Lax
   ↓
some cross-site scenarios allowed

None
   ↓
allow cross-site usage
   ↓
requires Secure
```

The exact browser behavior is more nuanced, but the conceptual reason is:

> **Control when a site's cookies are automatically sent in cross-site requests.**

Again:

```text
HttpOnly → JavaScript access
Secure   → transport security
SameSite → cross-site sending behavior
CSRF token → explicit request authenticity defense
```

They're not interchangeable.

---

# Now let's revisit the session itself

We're currently storing:

```text
session_id → Alice
```

Where?

Possibly:

```text
Redis
Database
In-memory store
Distributed cache
```

Suppose we have:

```text
10 million users
```

and:

```text
10 million active sessions
```

That's okay.

But now our system becomes:

```text
                 ┌── Server A
Browser ─────────┼── Server B
                 ├── Server C
                 └── Server D
```

Every server needs to answer:

```text
ABC123 → Alice?
```

If sessions are stored only in server memory:

```text
Server A:
ABC123 → Alice

Server B:
????
```

Alice's next request might hit Server B.

Server B doesn't know the session.

---

# Problem 5: Scaling stateful sessions

One solution:

```text
               ┌── Server A
               │
Browser ───────┼── Server B
               │
               └── Server C
                       |
                       v
                   Redis
```

All servers share a central session store.

Now:

```text
ABC123 → Alice
```

is available everywhere.

That works.

But we've introduced another dependency:

```text
Application
      |
      v
Session Store
```

Now we care about:

* availability
* latency
* replication
* failover
* session cleanup
* scaling
* regional architecture

Imagine we have services in:

```text
India
Europe
US
```

and the session store is centralized in one region.

Suddenly session lookup becomes a distributed-systems problem.

This does **not** mean stateful sessions are bad.

It means:

> **State has operational consequences.**

And this is one reason people became interested in more self-contained credentials later.

---

# A tempting idea appears

An engineer says:

> "Why do we need to ask the session store every time?"

We currently have:

```text
session_id = ABC123

Redis:
ABC123 → Alice
```

What if we simply put the information into the credential itself?

Something conceptually like:

```text
credential = {
    user: Alice,
    expiration: 10:30,
    role: USER
}
```

Then a server could inspect the credential without asking Redis.

But immediately a problem appears:

> **How do I know the client didn't modify it?**

Alice could change:

```text
role=USER
```

to:

```text
role=ADMIN
```

So we need a way for the server to detect tampering.

That leads to:

# Cryptographic signatures

Conceptually:

```text
Data
  +
Private key
  ↓
Signature
```

The server can later verify:

```text
Data
  +
Signature
  +
Public key
  ↓
Valid?
```

Now the client can't simply modify:

```text
role=USER
```

to:

```text
role=ADMIN
```

without producing a valid signature.

And **this** is the conceptual seed of JWT.

But notice where we are in the story:

```text
Session
   ↓
scaling/state problem
   ↓
"Can the credential carry information?"
   ↓
Need integrity protection
   ↓
digital signatures
   ↓
JWT becomes attractive
```

We haven't learned JWT yet.

That's deliberate.

---

# One more crucial distinction

At this point, you should have three different things in your head:

### Password

```text
Alice → server

"Here is my long-term secret."
```

Used primarily to establish authentication.

### Session ID

```text
ABC123
```

A reference to server-side authenticated state:

```text
ABC123 → Alice
```

### Cookie

```text
Cookie: session_id=ABC123
```

A **transport/storage mechanism in the browser** for carrying the session credential.

This distinction becomes incredibly important later.

A cookie is **not inherently an authentication mechanism**.

You can put many things in cookies.

For example:

```text
session IDs
access tokens
refresh tokens
preferences
CSRF-related values
```

Likewise:

> JWT is not synonymous with cookie.

A JWT can be sent:

```text
Cookie
Authorization header
POST body
etc.
```

depending on the architecture.

---

# Where we are now

We've gone from:

```text
Username/password
```

to:

```text
Password
   ↓
Session
   ↓
Cookie
```

and encountered:

```text
Problem                         Defense

Network sniffing                HTTPS/TLS

JavaScript stealing cookie      HttpOnly

Cookie over HTTP                Secure

Cross-site requests             SameSite / CSRF defenses

Multiple application servers    Shared session store

Central session state           Question of self-contained credentials
                                ↓
                           signatures
```

And this naturally creates our next historical question:

> **Can we make a credential that contains the user's information, can be verified without looking up server-side session state, and cannot be modified by the client?**

That takes us into **signed tokens → JWT**, where we'll carefully separate:

```text
encoding
vs
signing
vs
encryption
vs
verification
```

because these four concepts are frequently mixed up.

### One thing to make sure is solid before we continue

The most important mental model from this chapter is:

```text
Cookie ≠ Session
Cookie ≠ JWT
Cookie ≠ Authentication

Cookie = a browser mechanism for carrying/storing a value.

Session = server-side authenticated state.

Session ID = credential/reference used to retrieve that state.
```

And:

```text
Authentication:
    "Who is this?"

Authorization:
    "What may this identity do?"

HTTPS:
    "Can we communicate securely?"

Cookie security:
    "When/how should the browser carry this credential?"

Session:
    "What authenticated state does this credential represent?"
```

Next we can follow the story into **why signed tokens and JWT appeared, what a JWT actually guarantees, and why "JWT = secure" is a dangerously incomplete statement.**

---

# Part 3 — The birth of signed tokens and JWT

Now we reach an important turning point.

We have a working authentication system:

```text
Browser
   |
   | Cookie: session_id=ABC123
   v
Application
   |
   | lookup ABC123
   v
Session Store
   |
   | Alice
   v
Application
```

It works, but the server has to maintain this mapping:

```text
ABC123 → Alice
ABC124 → Bob
ABC125 → Charlie
```

And every request potentially involves:

```text
request
   ↓
find session
   ↓
session store lookup
   ↓
get user
```

That isn't inherently bad. But engineers started building systems where they wanted something different.

---

# The new dream: "Can the credential carry the information?"

Imagine instead of giving Alice:

```text
ABC123
```

we give her something conceptually like:

```json
{
  "user": "alice",
  "role": "USER",
  "expires": 10:30
}
```

Then when Alice makes a request:

```text
GET /account
Credential: <that object>
```

the server can inspect it directly.

No lookup:

```text
Credential
   ↓
user = Alice
role = USER
expires = 10:30
```

This sounds great.

But a giant problem appears immediately.

---

# Problem: the client controls the credential

Alice's browser has:

```json
{
  "user": "alice",
  "role": "USER"
}
```

What's stopping her from changing it to:

```json
{
  "user": "alice",
  "role": "ADMIN"
}
```

Nothing.

The server cannot trust data merely because the client sent it.

This is one of the deepest principles in application security:

> **Anything controlled by the client must be treated as untrusted until independently verified.**

So we need two things:

```text
Information in the credential
+
Proof that the information hasn't been tampered with
```

---

# Enter cryptographic signatures

Suppose the server has a secret signing key:

```text
SERVER_SECRET
```

It takes:

```text
data = {
    user: Alice,
    role: USER
}
```

and computes:

```text
signature = Sign(SERVER_SECRET, data)
```

The credential becomes conceptually:

```text
data + signature
```

For example:

```text
{
    user: Alice,
    role: USER,
    signature: ABCDEF...
}
```

Now Alice changes:

```text
role=USER
```

to:

```text
role=ADMIN
```

but keeps the old signature.

The server recomputes the expected signature:

```text
Sign(SERVER_SECRET, modified_data)
```

It won't match.

Therefore:

```text
modified data
+
old signature
      ↓
INVALID
```

So we have gained something powerful:

> **Integrity/authenticity of the signed data, as long as the signing key remains secret and the verification algorithm is used correctly.**

---

# Very important: what does the signature actually prove?

Suppose the server receives:

```text
user=Alice
role=ADMIN
signature=XYZ
```

and the signature verifies.

What can the server conclude?

Something like:

> "This exact data was signed by whoever possesses the signing key, and it has not been modified since it was signed."

It does **not** automatically prove:

> "Alice is currently trustworthy."

It does **not** prove:

> "The person sending this request is physically Alice."

It does **not** prove:

> "Alice should still have admin access right now."

And it does not provide secrecy.

This distinction is extremely important.

---

# Signature vs encryption

People often mix these up.

Suppose we have:

```text
user=Alice
role=ADMIN
```

A **signature** is primarily about:

```text
"Was this data altered?"
"Who signed it?"
```

Encryption is about:

```text
"Can outsiders read this data?"
```

For example:

```text
Signing:

Alice, USER
   ↓
signature
```

Anybody who can see the message may still read:

```text
Alice, USER
```

They just can't alter it without invalidating the signature.

Encryption instead aims for:

```text
Alice, USER
   ↓
ciphertext
```

so outsiders cannot understand the contents without the decryption key.

Therefore:

```text
Signing       → integrity / authenticity
Encryption    → confidentiality
```

JWTs are usually **signed**, not encrypted.

That's going to matter a lot.

---

# "But how do we actually format this?"

We need a standard representation.

People don't want every company inventing:

```text
my-token-v3-format
company-token-format
special-enterprise-token-format
```

So the industry eventually standardized token formats.

One particularly important one is:

# JWT — JSON Web Token

A JWT is a compact representation of claims that can be digitally signed and, in some variants/uses, encrypted.

Most JWTs you'll encounter are **signed JWTs**.

A typical JWT looks like:

```text
xxxxx.yyyyy.zzzzz
```

Three parts separated by dots:

```text
HEADER.PAYLOAD.SIGNATURE
```

Let's understand each part.

---

# Part 1: Header

Example:

```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

This tells the receiver things about the token, such as the signature algorithm.

For example:

```text
alg = RS256
```

means a particular RSA-based signature scheme.

The header is itself encoded.

---

# Part 2: Payload

Example:

```json
{
  "sub": "123",
  "name": "Alice",
  "role": "USER",
  "exp": 1787906400
}
```

These pieces of information are generally called:

# Claims

Claims are simply statements about something.

Examples:

```text
sub → subject
iss → issuer
aud → audience
exp → expiration time
iat → issued-at time
nbf → not-before time
```

And applications may add their own claims:

```text
role
permissions
tenant
email
organization
```

But here's a very important warning:

> **A JWT payload is not inherently secret.**

The payload is normally only encoded, not encrypted.

We'll see why this matters when deciding what information to place inside JWTs.

---

# Part 3: Signature

Conceptually:

```text
signature =
    Sign(
        signing_key,
        base64url(header) + "." + base64url(payload)
    )
```

The receiver verifies:

```text
Verify(
    verification_key,
    header + "." + payload,
    signature
)
```

If verification succeeds:

```text
Token integrity valid
```

If someone changes:

```json
"role": "USER"
```

to:

```json
"role": "ADMIN"
```

the signature no longer matches.

---

# So what problem did JWT solve?

Let's compare.

## Stateful session

```text
Client:

session_id=ABC123

Server:
ABC123 → Alice
```

The server needs the session state.

## Signed token

```text
Client:

{
   user: Alice,
   role: USER,
   exp: ...
}
+
signature
```

The server can validate the token without necessarily maintaining a corresponding session entry.

That's the big attraction:

> **Some authorization/authentication-related information can travel with the credential itself.**

This is why people call JWTs **self-contained tokens**.

But be careful:

> **"Self-contained" does not mean "server no longer needs any state."**

We'll come back to this because it's a common interview mistake.

---

# Now imagine a larger system

Suppose your company has:

```text
API Gateway
     |
     +---- User Service
     |
     +---- Order Service
     |
     +---- Payment Service
     |
     +---- Notification Service
```

With server-side sessions, every service that needs to recognize the user may need access to centralized session state.

With a signed token:

```text
                JWT
                 |
        +--------+--------+
        |        |        |
        v        v        v
      User     Order    Payment
    Service   Service   Service
```

Each service can verify the signature.

If the JWT contains:

```json
{
  "sub": "123",
  "role": "USER",
  "exp": 1787906400
}
```

the services can independently inspect it.

This is particularly useful in distributed systems.

---

# But now we created new problems

And this is where the story becomes interesting again.

JWT solved one class of problems.

It created or exposed several others.

Let's take them one by one.

---

# New Problem #1 — "How do I revoke it?"

Suppose Alice gets a JWT:

```text
expires in 1 hour
```

At 10:00:

```text
Alice has valid token
```

At 10:05:

```text
Admin disables Alice's account.
```

But Alice's JWT is still cryptographically valid.

At 10:06:

```text
Alice → API
        JWT
```

The API verifies:

```text
Signature valid
exp valid
```

So:

```text
ACCESS GRANTED
```

Oops.

Why?

Because cryptographic validity and business validity are different things.

The signature answers:

> "Was this token legitimately issued and unmodified?"

It does **not** answer:

> "Does the organization still want this token to work?"

This creates the classic JWT revocation problem.

---

# New Problem #2 — "How do I change someone's permissions?"

Suppose Alice receives:

```json
{
  "sub": "123",
  "role": "USER",
  "exp": "11:00"
}
```

At 10:10 she gets promoted:

```text
USER → ADMIN
```

But her existing JWT still says:

```text
role=USER
```

So her permissions may remain stale until that token expires.

The opposite case is more dangerous.

Suppose:

```text
ADMIN → USER
```

But Alice still holds a JWT containing:

```text
role=ADMIN
```

The old token may continue granting admin access.

This means:

> **Long-lived self-contained credentials create stale authorization information.**

That is a recurring tradeoff.

---

# New Problem #3 — "Where do I store the token?"

A JWT can be sent in:

```http
Authorization: Bearer <JWT>
```

or sometimes:

```http
Cookie: access_token=<JWT>
```

Now we have to ask:

> Where does the client keep it?

For browser applications, possibilities include:

```text
memory
localStorage
sessionStorage
cookies
```

Each creates different security tradeoffs.

For example:

```text
localStorage
```

can be accessible to JavaScript.

A malicious XSS payload could potentially read:

```text
localStorage.getItem("access_token")
```

and exfiltrate the token.

So now our nice stateless JWT architecture has brought us back to:

```text
How do we protect credentials from theft?
```

The problem never disappeared.

It just changed shape.

---

# New Problem #4 — Token size

A session ID might be:

```text
ABC123...
```

A JWT may contain:

```json
{
  "sub": "123456",
  "iss": "auth.example.com",
  "aud": "api.example.com",
  "scope": "...",
  ...
}
```

plus encoding and signature.

So instead of:

```text
small session ID
```

we have:

```text
larger credential on every request
```

If you're making millions of requests, that extra data isn't necessarily free.

Again:

> JWT is not automatically better than sessions.

It's a tradeoff.

---

# New Problem #5 — Key management

Now suppose ten services verify JWTs.

Who has the signing key?

You could use one shared secret:

```text
AUTH SERVER
   |
   | shared secret
   +---- Service A
   +---- Service B
   +---- Service C
```

But now every service possessing the secret can potentially create valid tokens.

That's uncomfortable.

Instead, asymmetric cryptography is often useful.

For example:

```text
Authorization Server
    |
    | private key
    | signs
    v
    JWT
```

Services get:

```text
public key
```

and verify:

```text
JWT
 +
public key
 ↓
valid?
```

They cannot create valid signatures merely from the public key.

This gives us another important distinction.

---

# Symmetric signing

Example conceptually:

```text
       SAME SECRET
        /       \
       /         \
signer           verifier
```

The same secret is used to sign and verify.

Algorithms include HMAC-based approaches such as:

```text
HS256
```

Advantage:

```text
simple
fast
```

Problem:

```text
every verifier needs the secret
```

and therefore can potentially sign tokens too.

---

# Asymmetric signing

Conceptually:

```text
PRIVATE KEY
    ↓
sign

PUBLIC KEY
    ↓
verify
```

For example:

```text
RS256
ES256
EdDSA
```

Now:

```text
Auth Server
   |
   | private key
   ↓
 signs JWT

Services
   |
   | public key
   ↓
 verify JWT
```

This is a very useful architecture for distributed systems.

---

# An extremely important correction

At this point you might think:

> "Okay, JWT replaces sessions."

No.

JWT and sessions solve different aspects of the problem.

You can even have:

```text
JWT
+
server-side session/state
```

or:

```text
session ID
+
cookie
```

or:

```text
JWT
+
Authorization header
```

They are architectural components, not mutually exclusive religions.

---

# Let's understand JWT through a real request

Suppose an authorization server issues:

```json
{
  "sub": "123",
  "iss": "https://auth.example.com",
  "aud": "https://api.example.com",
  "scope": "orders:read",
  "exp": 1787906400
}
```

signed by the authorization server.

Client sends:

```http
GET /orders
Host: api.example.com
Authorization: Bearer eyJ...
```

API receives it.

It should conceptually check things like:

```text
1. Is the JWT structurally valid?
2. Is the signature valid?
3. Is the signing algorithm acceptable?
4. Is the issuer the expected issuer?
5. Is the audience intended for this API?
6. Is it expired?
7. Is it not-before valid, if relevant?
8. Does the token have the required scope/permissions?
```

Only then should the API decide:

```text
ALLOW
```

This is another major lesson:

> **Verifying a JWT signature is only one step in validating a token.**

A valid signature does not automatically mean:

```text
"Allow everything."
```

---

# What does a signed JWT actually guarantee?

This is perhaps the most important section of today's lesson.

Suppose:

```text
JWT signature verifies
```

Assuming the implementation and key handling are correct, you can generally establish that:

### 1. The signed contents haven't been altered

If somebody changes:

```json
"sub": "123"
```

to:

```json
"sub": "456"
```

the signature should fail.

### 2. The token was signed by whoever controls the signing key

With asymmetric crypto:

```text
private key → signer
public key → verifier
```

### 3. The claims were true/authorized at issuance time, according to the issuer's intent

This one requires careful wording.

The token is an attestation from the issuer.

For example:

```text
"Issuer says subject 123 has scope orders:read."
```

It doesn't independently prove that this statement remains true forever.

---

# What does a JWT NOT guarantee?

A JWT does not inherently guarantee:

```text
❌ the user is currently logged in
❌ the account hasn't been disabled
❌ the claims are still current
❌ the token wasn't stolen
❌ the token is confidential
❌ the token came directly from the user
❌ the holder is the original recipient
❌ the request itself is safe
```

This is why saying:

> "JWT provides authentication"

is incomplete.

A JWT is fundamentally a **token format/representation**.

How you use it determines what security properties it contributes.

---

# The biggest mental model from this chapter

Don't think:

```text
JWT = authentication
```

Think:

```text
JWT
=
a standardized way to represent claims,
often with a cryptographic signature
```

Then separately ask:

```text
Who issued it?
Why was it issued?
Who is it intended for?
What permissions does it represent?
How long is it valid?
How is it transported?
Where is it stored?
How can it be revoked?
```

Those questions lead directly into the next chapter.

---

# And now the story reaches the really important problem

We've now got:

```text
username/password
       ↓
authentication
       ↓
session
       ↓
cookies
       ↓
secure cookies + HTTPS + CSRF defenses
       ↓
distributed systems
       ↓
self-contained signed tokens
       ↓
JWT
```

But notice something:

All of this assumes the user is interacting with **our own application**.

Now imagine:

```text
Alice
  |
  | uses
  v
Photo Printing App
  |
  | wants to access
  v
Alice's Google Photos
```

The Photo Printing App needs permission to access Alice's photos.

Should Alice give the application:

```text
Google username
+
Google password
```

Absolutely not.

And if Google instead says:

> "We'll give the app a token."

we immediately need to answer:

```text
Who issues the token?
What may the app do with it?
How does Google know what Alice authorized?
How does the app prove which client it is?
What does "authorization" actually mean?
Why isn't this simply authentication?
```

**That is the problem OAuth was created to solve.**

And this distinction is the bridge from everything we've learned so far:

```text
Authentication:
    Alice proves who she is.

Authorization:
    Alice delegates some permission
    to another application.
```

Next we should start **OAuth from the original password-sharing problem**, not from OAuth terminology. That is where concepts like **client, resource owner, authorization server, resource server, authorization code, access token, scopes, and redirect URI** will stop feeling like vocabulary you have to memorize and start feeling inevitable.

---

# Part 4 — Why OAuth was needed: "I want this app to access my data"

Now we move to one of the most important transitions in the whole story.

So far, we've mostly been solving:

> **"How does my application know who Alice is?"**

OAuth starts with a different problem:

> **"How can Alice give another application limited access to her data without giving that application her password?"**

That distinction is the foundation of OAuth.

---

# The problem that forced OAuth

Imagine a service called:

```text
PhotoBox
```

Alice has thousands of photos in her Google Photos account.

She finds a new service:

```text
PrintMyPhotos
```

which can print selected photos and ship them to her.

PrintMyPhotos needs access to Alice's Google Photos.

Without OAuth, someone might design this:

```text
PrintMyPhotos asks:

"Give me your Google email and password."
```

Alice enters:

```text
alice@gmail.com
password=secret123
```

PrintMyPhotos sends those credentials to Google:

```text
PrintMyPhotos
      |
      | Alice's Google credentials
      v
    Google
```

Google authenticates Alice.

PrintMyPhotos can now access her photos.

It appears to work.

But we've created a terrible security model.

---

# Problem #1 — The third-party application now knows Alice's password

This is the biggest problem.

Alice's password isn't a credential for "photos."

It's normally a credential for her **entire Google account**.

So PrintMyPhotos potentially has the ability to do things such as:

```text
read email
change profile information
access contacts
access drive
change settings
```

depending on what that password unlocks.

Alice wanted:

```text
"Let PrintMyPhotos read my photos."
```

but she accidentally gave:

```text
"Let PrintMyPhotos impersonate me on Google."
```

Those are radically different permissions.

---

# Problem #2 — No least privilege

Suppose Google had resources:

```text
Photos
Email
Calendar
Contacts
Drive
```

Alice wants only:

```text
Photos → READ
```

But with her password, PrintMyPhotos may get far more.

This violates an important security principle:

# Least privilege

> Give a component only the authority it needs to perform its task.

We want:

```text
PrintMyPhotos
      ↓
photos:read
```

not:

```text
PrintMyPhotos
      ↓
Alice's entire Google identity
```

---

# Problem #3 — Password revocation is too coarse

Suppose Alice says:

> "I no longer trust PrintMyPhotos."

With password sharing, what can she do?

Change her Google password.

But that affects:

```text
Gmail
Drive
Calendar
YouTube
Photos
every other application
```

And if she doesn't change the password, PrintMyPhotos can potentially continue using it.

We need something more precise:

```text
Revoke PrintMyPhotos' access
```

without:

```text
changing Alice's entire Google password
```

---

# Problem #4 — The application must store Alice's password

Even worse, the application now needs to handle:

```text
Alice's Google password
```

That means every third-party application becomes a potential password-storage/security disaster.

Imagine Alice uses:

```text
10 different services
```

and all of them ask for her Google password.

Now there are:

```text
10 places
```

where her primary credential might be leaked.

This is exactly what we want to avoid.

---

# So engineers changed the question

Instead of:

> "Can Alice give PrintMyPhotos her Google password?"

ask:

> "Can Google authenticate Alice and then issue PrintMyPhotos a limited credential?"

Now the architecture becomes:

```text
Alice
   |
   | "I want PrintMyPhotos to access my photos"
   v
Google
   |
   | authenticate Alice
   |
   | obtain Alice's consent
   |
   | issue limited credential
   v
PrintMyPhotos
   |
   | limited credential
   v
Google Photos API
```

This is the core idea behind **delegated authorization**.

---

# This is the key sentence to remember

OAuth is fundamentally about:

> **Delegating authorization without sharing the user's primary credentials with the client application.**

Not:

> "OAuth is how we log users in."

OAuth **can be involved in login architectures**, but OAuth itself is about authorization/delegation.

That distinction will become extremely important when we later reach **OpenID Connect**.

---

# Let's build the simplest possible OAuth idea ourselves

Forget OAuth terminology for a moment.

We need four things.

### 1. Alice owns the data

```text
Alice
 ↓
Google Photos
```

### 2. PrintMyPhotos is the application requesting access

```text
PrintMyPhotos
```

### 3. Google controls the photos

Google needs to make the final decision:

```text
"Is this application allowed to access Alice's photos?"
```

### 4. Google must give PrintMyPhotos something safer than Alice's password

For example:

```text
access_token = XYZ123
```

Now PrintMyPhotos can say:

```http
GET /photos
Authorization: Bearer XYZ123
```

Google's API examines the token and decides:

```text
Token XYZ123
    ↓
issued to PrintMyPhotos
    ↓
for Alice
    ↓
scope = photos:read
    ↓
valid
```

Then:

```text
ALLOW
```

That's the basic idea.

---

# Notice what we've created

We now have two credentials:

```text
Alice's password
        ↓
used by Google to authenticate Alice

access token
        ↓
used by PrintMyPhotos to access Alice's resource
```

This separation is extremely powerful.

Before:

```text
password
   ↓
everything
```

Now:

```text
password
   ↓
authenticate Alice

access token
   ↓
specific delegated authority
```

This is one of the most important concepts in modern authorization systems.

---

# What exactly is an access token?

At the conceptual level:

> An **access token** is a credential that represents granted authorization to access some protected resource.

For example:

```text
Alice
PrintMyPhotos
Google
```

After Alice authorizes:

```text
access_token = ABC123
```

PrintMyPhotos presents:

```http
Authorization: Bearer ABC123
```

to Google's resource server.

The resource server asks:

```text
Is ABC123 valid?
Who issued it?
For whom?
For what resource?
What permissions does it contain?
Has it expired?
```

If everything checks out:

```text
ALLOW
```

---

# Now we need to distinguish two types of server

This is one of the places where OAuth terminology starts becoming useful.

Imagine Google has:

```text
Authorization Server
```

and:

```text
Resource Server
```

They may be implemented as separate services, even if they belong to the same organization.

## Authorization Server

Its job is essentially:

> **Issue credentials after authorization has been granted.**

For example:

```text
authenticate Alice
↓
ask for consent
↓
issue access token
```

## Resource Server

Its job is:

> **Protect the actual resource/API.**

For example:

```text
GET /photos
Authorization: Bearer ABC123
```

The Photos API is the resource server.

---

# So the architecture becomes

```text
                 ┌──────────────────────┐
                 │ Authorization Server │
                 │                      │
Alice ──────────>│ authenticate/consent │
                 │                      │
                 └──────────┬───────────┘
                            │
                       access token
                            │
                            v
                    ┌───────────────┐
PrintMyPhotos ─────>│ Resource      │
                    │ Server        │
                    │               │
                    │ Photos API    │
                    └───────────────┘
```

This separation is central to OAuth.

---

# But here's the next problem

We said:

```text
PrintMyPhotos wants access
```

How does Google know:

> "This really is PrintMyPhotos"?

An attacker could create:

```text
FakePrintMyPhotos
```

and say:

> "Alice authorized me."

Google needs some way of identifying the application.

So OAuth introduces another participant:

# The Client

In OAuth terminology, the application requesting delegated access is called the:

> **Client**

Here:

```text
Client = PrintMyPhotos
```

The user granting access is commonly called:

> **Resource Owner**

Here:

```text
Resource Owner = Alice
```

The server that issues tokens:

> **Authorization Server**

And the API holding the protected data:

> **Resource Server**

So our cast of characters becomes:

```text
Alice
   ↓
Resource Owner

PrintMyPhotos
   ↓
Client

Google Authorization Service
   ↓
Authorization Server

Google Photos API
   ↓
Resource Server
```

Don't memorize this yet.

The names make sense once you understand the story:

```text
Who owns the resource?
Who wants access?
Who grants/mediates authorization?
Who actually serves the resource?
```

---

# But now the client needs to identify itself

Imagine Google gives PrintMyPhotos credentials:

```text
client_id
client_secret
```

Conceptually:

```text
PrintMyPhotos
    ↓
client_id = abc123
client_secret = xyz789
```

Google can recognize:

```text
"Oh, this is the registered PrintMyPhotos application."
```

But immediately we get another problem.

What if the client is a browser app?

Can we safely put:

```text
client_secret
```

inside JavaScript?

Suppose:

```javascript
const clientSecret = "xyz789";
```

Anyone can inspect the browser code.

So:

```text
public browser application
        +
secret
        =
not actually secret
```

This eventually leads to important distinctions between:

```text
confidential clients
vs
public clients
```

and later:

```text
PKCE
```

But don't jump there yet.

We're still building the story.

---

# Another critical problem: How does Alice give consent?

Suppose PrintMyPhotos displays:

```text
Give us your Google password
```

We've already rejected that.

Instead, the application should send Alice to Google:

```text
PrintMyPhotos
      |
      | "Please authorize me"
      v
Google
```

Alice sees Google's own authorization UI:

```text
PrintMyPhotos wants:

✓ View your photos

[Allow] [Deny]
```

Alice chooses:

```text
Allow
```

Google records the authorization and issues a code/credential flow that eventually allows PrintMyPhotos to obtain an access token.

The critical security idea is:

> **Alice authenticates directly with the authorization server, not by handing her password to the client.**

That is a gigantic improvement.

---

# Compare the old world with OAuth

### Password sharing

```text
Alice
   |
   | username + password
   v
PrintMyPhotos
   |
   | password
   v
Google
```

PrintMyPhotos sees the password.

### OAuth-style delegation

```text
Alice
   |
   | authenticate directly
   v
Google Authorization Server
   |
   | authorization result
   v
PrintMyPhotos
   |
   | access token
   v
Google Photos API
```

PrintMyPhotos never needs Alice's Google password.

That's the core breakthrough.

---

# But we still haven't solved one major security problem

Imagine PrintMyPhotos redirects Alice's browser to:

```text
https://google.com/authorize?...
```

Alice logs in.

Google then needs to send the result back to:

```text
https://printmyphotos.com/callback
```

How does Google know this is a legitimate destination?

What if the attacker says:

```text
"Send the authorization result to:
https://evil.com/callback"
```

This is extremely dangerous.

So the authorization server needs registered **redirect URIs**.

The client registers something like:

```text
https://printmyphotos.com/oauth/callback
```

Google only redirects authorization results to an allowed destination.

This is one of OAuth's core security boundaries.

---

# And now something interesting happens

At this point, you might say:

> "Why not just give the access token directly to the browser and let the browser send it to PrintMyPhotos?"

That seems simple.

But it creates opportunities for:

* token leakage
* malicious redirects
* replay
* token injection
* browser history/referrer issues
* compromised clients

So OAuth introduced a safer intermediary mechanism.

Instead of:

```text
Alice → authorization server → access token → browser → client
```

the modern standard flow generally uses:

```text
Alice
  ↓
Authorization Server
  ↓
Authorization Code
  ↓
Client
  ↓
Access Token
```

That little thing called the:

# Authorization Code

is one of the most important ideas in OAuth 2.x.

We'll study it carefully next.

---

# Before moving on, let's lock down today's concepts

You should now be able to explain why OAuth exists without mentioning JWT.

The story is:

```text
Third-party application wants access
          ↓
"Give me your password"
          ↓
BAD:
- password exposure
- excessive privilege
- poor revocation
- third party can impersonate user
          ↓
"We need delegated access"
          ↓
Authorization Server
          ↓
issues limited access credential
          ↓
Access Token
          ↓
Resource Server accepts token
```

And the actors:

```text
Resource Owner
    = Alice

Client
    = PrintMyPhotos

Authorization Server
    = server that authenticates/gets consent
      and issues tokens

Resource Server
    = API holding protected resources
```

The central separation is:

```text
Alice's password
        ↓
authenticate Alice to Authorization Server

Access token
        ↓
authorize Client to access specific resources
```

And the most important OAuth guarantee at this point is:

> **A client can obtain delegated access without needing the resource owner's primary credentials.**

OAuth does **not** inherently guarantee:

```text
❌ the client is trustworthy
❌ the user is authenticated to your application
❌ the access token is a JWT
❌ the token cannot be stolen
❌ the token can never be replayed
```

Those are separate concerns.

---

## The next piece: Authorization Code

The next step in the story is where OAuth starts looking complicated:

```text
Alice → Client → Authorization Server
                       ↓
                Authorization Code
                       ↓
                     Client
                       ↓
                  Access Token
```

We'll walk through **every HTTP request and response**, including the browser redirects, `client_id`, `redirect_uri`, `scope`, `state`, and eventually **PKCE**—and for each piece, we'll answer:

> **"What attack/problem forced OAuth to introduce this?"**

---

# Part 5 — The Authorization Code: why OAuth doesn't just give the access token directly

We now have the basic OAuth problem:

```text
Alice wants:

PrintMyPhotos → read my Google Photos

without:

PrintMyPhotos → knowing Alice's Google password
```

So we created:

```text
Alice
  ↓
Authorization Server
  ↓
grant permission
  ↓
Access Token
  ↓
PrintMyPhotos
```

But now let's ask:

> **Why can't the Authorization Server simply send the access token back to the browser?**

That question leads us directly to the **Authorization Code**.

---

# Start with the naive design

Suppose PrintMyPhotos sends Alice to Google:

```http
GET /authorize
    ?client_id=printmyphotos
    &redirect_uri=https://printmyphotos.com/callback
    &scope=photos:read
```

Alice authenticates with Google and clicks **Allow**.

The simplest possible thing would be for Google to redirect the browser back with:

```text
https://printmyphotos.com/callback?access_token=ABC123
```

So:

```text
               browser
                  |
                  | 1. go to Google
                  v
        Authorization Server
                  |
                  | 2. login + consent
                  |
                  | 3. redirect with token
                  v
             PrintMyPhotos
```

Looks wonderfully simple.

But now think carefully.

The **browser is carrying the access token**.

And browsers are complicated environments.

---

# Problem #1 — The token is exposed to the front channel

OAuth calls the browser-redirect side of this interaction the:

> **front channel**

Conceptually:

```text
Authorization Server
        ↓
      Browser
        ↓
      Client
```

Anything travelling through the browser is more exposed to things like:

* browser history
* redirects
* malicious browser extensions
* referrer-related leakage in some scenarios
* accidental logging
* compromised browser-side code
* badly implemented clients

The access token is extremely valuable because it is a credential.

Remember:

```text
Bearer access token
        ↓
whoever possesses it
        ↓
may be able to use it
```

So OAuth designers wanted to avoid putting the actual access token into this browser-facing redirect whenever possible.

---

# The clever solution

Instead of returning:

```text
access_token=ABC123
```

Google returns a **short-lived authorization code**:

```text
code=XYZ789
```

So now:

```text
Browser
   |
   | code=XYZ789
   v
PrintMyPhotos
```

PrintMyPhotos then makes a **back-channel** request directly to the Authorization Server:

```text
PrintMyPhotos
      |
      | code=XYZ789
      | client authentication
      v
Authorization Server
      |
      | access_token=ABC123
      v
PrintMyPhotos
```

The access token never needs to travel through the browser redirect.

That's the fundamental purpose of the Authorization Code.

---

# Front channel vs back channel

This distinction is worth knowing extremely well.

## Front channel

Usually involves the user's browser:

```text
Browser
  ↕
Authorization Server
  ↕
Client
```

It's convenient for user interaction, redirects, login, consent.

But it's a less desirable place for highly valuable long-lived credentials.

## Back channel

Direct server-to-server communication:

```text
Client Server
     |
     | HTTPS
     v
Authorization Server
```

This is a much better place to exchange a short-lived code for an access token.

So the architecture becomes:

```text
         FRONT CHANNEL
             
Browser ───────────────> Authorization Server
   ↑                            |
   |                            |
   |       authorization        |
   |          code              |
   └────────────────────────────┘


         BACK CHANNEL

Client ─────── code ───────────> Authorization Server
Client <──── access token ────── Authorization Server
```

This is a major security improvement.

---

# Let's do the entire flow slowly

Suppose:

```text
Alice
Client: PrintMyPhotos
Authorization Server: Google
Resource Server: Google Photos API
```

---

## Step 1 — Client wants authorization

Alice clicks:

```text
Connect Google Photos
```

PrintMyPhotos creates an authorization request.

Conceptually:

```http
GET https://accounts.google.com/authorize?
    client_id=printmyphotos
    &redirect_uri=https://printmyphotos.com/callback
    &response_type=code
    &scope=photos:read
    &state=RANDOM_VALUE
```

Don't worry about every parameter yet.

We're going to derive them.

---

# `client_id`

Google needs to know:

> Which application is requesting access?

So PrintMyPhotos has a registered identity:

```text
client_id = printmyphotos
```

This is essentially:

```text
"Which OAuth client are you?"
```

It is **not** usually a secret.

For example:

```text
client_id = 84736291
```

It's completely reasonable for the browser to know it.

This distinction will become important later:

```text
client_id
    ↓
identifier

client_secret
    ↓
credential
```

---

# `redirect_uri`

Next:

```text
redirect_uri=https://printmyphotos.com/callback
```

This tells Google:

> "After authorization, send the result here."

But Google doesn't simply trust whatever URI the request contains.

The client will generally have previously registered allowed redirect URIs.

For example:

```text
Registered:

https://printmyphotos.com/callback
```

Then an attacker cannot simply change the request to:

```text
https://evil.com/steal
```

and have Google redirect the authorization response there.

This gives us a very important security rule:

> **The authorization result must return only to a trusted location associated with the registered client.**

---

# `response_type=code`

This says:

> "I want an authorization code as the result."

Not the access token.

So we explicitly choose:

```text
response_type=code
```

Conceptually:

```text
Authorization Request
        ↓
Authorization Code
        ↓
Token Request
        ↓
Access Token
```

---

# `scope`

Suppose PrintMyPhotos only needs to read photos.

It asks for:

```text
scope=photos:read
```

This is where **least privilege** comes back.

Instead of:

```text
"Give me everything Alice can access."
```

the client asks:

```text
"Give me only the permission I need."
```

Alice can then consent to something like:

```text
PrintMyPhotos wants:

✓ View your photos

Not requested:
✗ Delete photos
✗ Read email
✗ Access contacts
```

This is the authorization model becoming explicit.

---

# Now Google shows Alice a consent screen

Alice gets something like:

```text
PrintMyPhotos wants access to your Google Photos

Permission requested:
    View your photos

                 [Cancel] [Allow]
```

This is an important boundary:

```text
PrintMyPhotos
      |
      | asks for permission
      v
Authorization Server
      |
      | asks Alice
      v
Alice
```

The client shouldn't simply declare:

> "Alice gave me permission."

The authorization server is responsible for establishing the authorization decision.

---

# Alice logs in

Suppose Alice wasn't already logged into Google.

She enters:

```text
alice@gmail.com
password=...
```

Important:

> **PrintMyPhotos never sees this password.**

The browser is interacting with:

```text
Google Authorization Server
```

not:

```text
PrintMyPhotos
```

This is the security boundary OAuth was designed around.

---

# Alice clicks Allow

Google now knows:

```text
User:
    Alice

Client:
    PrintMyPhotos

Requested scope:
    photos:read

Decision:
    ALLOW
```

Now comes the clever part.

Google does **not** redirect:

```text
access_token=ABC123
```

Instead:

```text
code=XYZ789
```

For example:

```http
HTTP/1.1 302 Found

Location:
https://printmyphotos.com/callback
?code=XYZ789
&state=RANDOM_VALUE
```

The browser follows that redirect.

---

# The browser now hits PrintMyPhotos

```http
GET /callback?code=XYZ789&state=RANDOM_VALUE
```

PrintMyPhotos receives:

```text
authorization_code = XYZ789
```

What does it do?

It **doesn't use the code to access photos**.

Instead:

```text
code
  ↓
exchange at Authorization Server
  ↓
access token
```

---

# Step 2 — Token exchange

PrintMyPhotos's backend sends:

```http
POST /token
```

Conceptually:

```text
grant_type=authorization_code
code=XYZ789
redirect_uri=https://printmyphotos.com/callback
```

And, for a confidential client, it may also authenticate itself:

```text
client_id=printmyphotos
client_secret=...
```

We'll dig into client authentication later.

Google checks:

```text
Is code valid?
Is it expired?
Has it already been used?
Was it issued to this client?
Is redirect_uri correct?
Is client authentication valid?
```

If everything is correct:

```json
{
  "access_token": "ABC123",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "photos:read"
}
```

The client now possesses the access token.

---

# Why is the code so useful?

Think of the authorization code as a temporary receipt.

Google is effectively saying:

```text
"Browser, you've completed the authorization interaction.

Here's a short-lived code.

Client server, come to me directly and prove that you're the right client.
Then I'll give you the actual access token."
```

So:

```text
Authorization Code
    =
temporary intermediate credential used to obtain tokens
```

It isn't the final permission credential.

---

# Why not make the authorization code itself the access token?

Because the code is designed to be:

```text
short-lived
single-use
bound to an authorization transaction
```

while the access token is the credential actually presented to the Resource Server.

So we separate:

```text
Authorization Code
        ↓
"exchange me"

Access Token
        ↓
"use me to access the API"
```

This is a very important distinction.

---

# Now let's see an attack

Suppose an attacker somehow steals:

```text
code=XYZ789
```

Can they necessarily use it?

Not necessarily.

The authorization server can require additional checks.

For a traditional confidential client, the attacker may still need:

```text
client authentication
```

for example:

```text
client_id
client_secret
```

So:

```text
Attacker has:
    authorization_code

But doesn't have:
    client credentials

Therefore:
    token exchange fails
```

This is one reason the code flow is safer than directly exposing the access token in the browser.

---

# But then the Internet changed again

OAuth originally assumed a certain kind of client:

```text
Server-side web application
```

Something like:

```text
Browser
   |
   v
PrintMyPhotos backend
   |
   v
Authorization Server
```

The backend could safely keep:

```text
client_secret
```

But then we got:

```text
Single-page applications
Mobile applications
Desktop applications
```

Consider a React app.

Your JavaScript runs on Alice's machine.

If you put:

```javascript
const clientSecret = "SUPER_SECRET";
```

into the frontend...

Alice can inspect it.

So the "secret" isn't secret.

This creates the next big problem.

---

# Public vs confidential clients

OAuth therefore distinguishes client types.

## Confidential client

A client capable of keeping credentials confidential.

Typical example:

```text
Backend web application
```

Architecture:

```text
Browser
   ↓
Backend
   ↓
Authorization Server
```

The backend may have:

```text
client_id
client_secret
```

stored securely on the server.

## Public client

A client that cannot reliably keep a secret confidential.

Examples:

```text
SPA
Mobile app
Desktop app
```

Because the code executes on an environment controlled by the user.

Therefore:

```text
client_secret in frontend
       ↓
not actually secret
```

And now we have a new security problem:

> **If the client cannot authenticate itself using a secret, how do we prevent an attacker from stealing or injecting authorization codes?**

This is the problem that led to:

# PKCE

But before PKCE, there's one more parameter we need to understand.

---

# `state`: a surprisingly important parameter

Suppose Alice starts an OAuth flow for:

```text
PrintMyPhotos
```

An attacker somehow causes Alice's browser to hit:

```text
https://printmyphotos.com/callback?code=ATTACKER_CODE
```

PrintMyPhotos might accidentally associate that code with Alice's session.

This is a form of **authorization response injection / login CSRF-style attack**.

So the client generates a random value before starting the flow:

```text
state = 8f9a2c...
```

It sends:

```text
state=8f9a2c...
```

in the authorization request.

When the browser comes back:

```text
code=XYZ789
state=8f9a2c...
```

the client checks:

```text
Did I generate this state?
        ↓
YES → continue
NO  → reject
```

Think of `state` as:

> **"Prove that this authorization response belongs to the authorization flow I started."**

This is a different security property from the user's authentication.

---

# Now we can see the full evolution

Our story has reached:

```text
Password sharing
       ↓
BAD
       ↓
Delegated authorization
       ↓
Access Tokens
       ↓
But don't expose token in browser redirect
       ↓
Authorization Code
       ↓
But browser/public clients cannot keep client secret
       ↓
PKCE
```

And each piece exists because something went wrong with the previous idea.

---

# The OAuth flow in one picture

```text
                        ┌─────────────────────────┐
                        │ Authorization Server    │
                        │                         │
                        │ login                   │
                        │ consent                 │
                        │ issue tokens            │
                        └───────────┬─────────────┘
                                    ^
                                    |
                             1. Authorization
                                    |
                                    |
Alice's Browser                    |
       |                            |
       |                            |
       | 2. redirect with code      |
       v                            |
PrintMyPhotos                       |
       |                            |
       | 3. backend token request   |
       └───────────────────────────>┘
                                    |
                             4. access token
                                    |
                                    v
                           PrintMyPhotos Backend
                                    |
                             Authorization:
                             Bearer <token>
                                    |
                                    v
                           Google Photos API
```

---

# Let's connect every parameter to the problem it solves

This is the way I want you to remember OAuth—not as a list of fields.

| Parameter / concept  | Why it exists                                                           |
| -------------------- | ----------------------------------------------------------------------- |
| `client_id`          | Identify which application is requesting access                         |
| `redirect_uri`       | Control where authorization results may be sent                         |
| `scope`              | Limit what the client is asking permission to do                        |
| `response_type=code` | Ask for an intermediate authorization code rather than the access token |
| `state`              | Bind the response to the authorization request initiated by the client  |
| authorization code   | Short-lived intermediate credential for token exchange                  |
| access token         | Credential actually used to access the resource                         |
| client secret        | Authenticate a confidential client                                      |
| PKCE                 | Protect authorization code exchange for public clients                  |

We're not done with several of these yet. Especially `state`, `redirect_uri`, and PKCE deserve much deeper treatment.

---

# One subtle but extremely important insight

Notice what OAuth is doing.

OAuth doesn't make the client:

```text
"become Alice"
```

Instead, it gives the client:

```text
"permission to perform specific actions on Alice's behalf"
```

That's a very different security model.

For example:

```text
Alice
  ↓
owns photos

PrintMyPhotos
  ↓
has photos:read

Google Photos
  ↓
enforces that permission
```

So the access token conceptually represents:

```text
"this client has been granted this authority"
```

rather than:

```text
"this client is Alice"
```

That distinction is going to become **very important** when we reach OAuth vs authentication and OpenID Connect.

---

# One final mental model

At this point, don't memorize "OAuth 2.0."

Remember this story:

```text
Alice doesn't want to give her password
            ↓
Use delegated authorization
            ↓
Client asks Authorization Server
            ↓
Alice authenticates + consents
            ↓
Authorization Server creates authorization result
            ↓
Don't expose valuable access token through browser
            ↓
Give client a short-lived authorization code
            ↓
Client exchanges code through back channel
            ↓
Authorization Server issues access token
            ↓
Client sends access token to Resource Server
            ↓
Resource Server enforces the granted authority
```

And now we have reached the next major problem:

> **For a browser/mobile/public client, there is no trustworthy `client_secret`. An attacker who steals an authorization code might be able to exchange it. How do we bind that code to the legitimate client instance without relying on a secret?**

That is exactly why **PKCE** was introduced.

Next we'll derive PKCE from an actual attack, including the **code verifier**, **code challenge**, and why PKCE is not "just another password."

---

# Part 6 — PKCE: solving the "stolen authorization code" problem

We have reached a very important point in OAuth.

Our flow currently looks like:

```text
Alice
  ↓
Authorization Server
  ↓
authorization code
  ↓
Client
  ↓
access token
```

For a traditional backend application, the client can authenticate itself with a `client_secret`.

But suppose the client is a mobile app:

```text
Alice's phone
   ↓
MyMobileApp
```

There is nowhere genuinely secret to put:

```text
client_secret = ???
```

Anything embedded in the app can potentially be extracted.

So we have a new problem.

---

# The attack

Let's first ignore PKCE completely.

Suppose Alice uses:

```text
MyBankApp
```

which wants access to:

```text
Bank Authorization Server
```

The app starts OAuth:

```text
MyBankApp
    ↓
Authorization Server
```

Alice logs in and approves.

The Authorization Server sends:

```text
https://mybank.com/callback?code=ABC123
```

through Alice's browser.

The legitimate app receives:

```text
code = ABC123
```

and normally does:

```text
POST /token

code=ABC123
client_id=my-mobile-app
```

and receives:

```text
access_token=XYZ789
```

---

# Now imagine an attacker

The attacker somehow gets hold of:

```text
code=ABC123
```

Maybe through a malicious application registering a competing custom URL scheme, or some other code interception scenario.

Now the attacker sends:

```http
POST /token

code=ABC123
client_id=my-mobile-app
```

If the authorization server has no additional protection, it may respond:

```text
access_token=XYZ789
```

The attacker now has Alice's access.

This is the core problem PKCE addresses.

---

# Why can't we just use `client_secret`?

For a backend:

```text
MyWebServer
    |
    | client_secret
    v
Authorization Server
```

The secret can be kept on the server.

But for a mobile application:

```text
Mobile App
    |
    | client_secret
    v
Alice's phone
```

Alice controls the environment.

She can inspect the application package, decompile it, debug it, instrument it, etc.

So this:

```text
client_secret = SECRET
```

isn't actually a reliable secret.

We need another mechanism.

---

# The key idea behind PKCE

PKCE asks:

> **Can the legitimate client create some secret at the beginning of this particular authorization flow, but never send that secret through the browser?**

Yes.

The client generates a random value:

```text
code_verifier
```

For example:

```text
code_verifier = rA7x9K...very-random...
```

The app keeps it locally.

Then it calculates a derived value:

```text
code_challenge = BASE64URL(
    SHA256(code_verifier)
)
```

Conceptually:

```text
random secret
     |
     | SHA-256
     v
code challenge
```

The Authorization Server receives the **challenge**.

The client keeps the **verifier**.

That is the central idea of PKCE.

---

# Step 1 — Client creates the verifier

MyBankApp generates:

```text
code_verifier = V
```

Where `V` is a high-entropy random value.

Important:

> This value is generated for this particular authorization transaction.

It is not the user's password.

It is not the client secret.

It is a temporary proof known by this particular client instance.

---

# Step 2 — Client creates the challenge

The app calculates:

```text
C = BASE64URL(SHA256(V))
```

So:

```text
V
 ↓
SHA-256
 ↓
C
```

Then the app starts authorization:

```http
GET /authorize?
    client_id=my-mobile-app
    &response_type=code
    &redirect_uri=myapp://callback
    &scope=photos:read
    &code_challenge=C
    &code_challenge_method=S256
```

Notice:

```text
code_verifier = V
```

is **not** sent.

Only:

```text
code_challenge = C
```

is sent.

---

# Why do this?

Because the challenge isn't enough to reconstruct the verifier.

You can think of it as:

```text
Verifier V
     ↓
 one-way transformation
     ↓
Challenge C
```

Knowing:

```text
C
```

should not allow someone to practically recover:

```text
V
```

assuming the cryptographic construction and randomness are sound.

---

# Step 3 — Authorization Server remembers the challenge

The Authorization Server associates:

```text
authorization_code = ABC123
```

with:

```text
code_challenge = C
```

So conceptually:

```text
ABC123
   ↓
issued for:
   client = my-mobile-app
   challenge = C
```

Then Alice authenticates and approves.

The server redirects:

```text
myapp://callback?code=ABC123
```

The legitimate app receives:

```text
ABC123
```

---

# Step 4 — Legitimate client exchanges the code

The app now sends:

```http
POST /token
```

with:

```text
grant_type=authorization_code
code=ABC123
client_id=my-mobile-app
redirect_uri=myapp://callback
code_verifier=V
```

Notice what happened.

Earlier the client sent:

```text
C
```

Now it sends:

```text
V
```

The Authorization Server computes:

```text
SHA256(V)
```

and checks whether the resulting challenge equals the one associated with the authorization code:

```text
SHA256(V) == C ?
```

If yes:

```text
VALID
```

and the server issues:

```text
access_token=XYZ789
```

---

# What happens to the attacker?

Remember our attacker stole:

```text
code=ABC123
```

But they do not have:

```text
V
```

They attempt:

```http
POST /token

code=ABC123
code_verifier=???
```

The Authorization Server computes:

```text
SHA256(???)
```

It doesn't match:

```text
C
```

Therefore:

```text
TOKEN EXCHANGE FAILED
```

The attacker has the authorization code, but not the proof associated with it.

That is the core security property.

---

# The whole thing visually

```text
                CLIENT

          generate random V
                 |
                 v
        V = code_verifier
                 |
                 | SHA-256
                 v
        C = code_challenge
                 |
                 |
                 v
        Authorization Server
                 |
                 | remembers:
                 |
                 | code ABC123
                 | challenge C
                 |
                 v
             user login
                 +
              consent
                 |
                 v
       redirect with code ABC123
                 |
                 v
              CLIENT
                 |
                 | code ABC123
                 | verifier V
                 v
        Authorization Server
                 |
             SHA-256(V)
                 |
          compare with C
                 |
              MATCH
                 |
                 v
           access token
```

That is PKCE.

---

# Why isn't PKCE simply encryption?

Because PKCE isn't trying to hide the authorization code.

It's establishing:

> **"The party presenting this code also possesses the secret verifier that was committed to when the authorization request started."**

It's a kind of proof-of-possession relationship.

The important sequence is:

```text
Start:
    V exists only at client

Tell server:
    C = hash(V)

Later:
    prove possession of V

Server:
    hash(V) == C
```

---

# A subtle but important distinction

PKCE does **not** mean:

```text
authorization code is encrypted
```

It means:

```text
authorization code
+
code verifier
```

are cryptographically linked through the earlier challenge.

The code can still be intercepted.

The difference is:

```text
Without PKCE:

steal code
   ↓
possibly exchange code


With PKCE:

steal code
   ↓
need verifier too
   ↓
can't exchange
```

That's the essence.

---

# Why `S256`?

PKCE supports a method called:

```text
S256
```

which means roughly:

```text
code_challenge = BASE64URL(SHA256(code_verifier))
```

There was also an older method:

```text
plain
```

where the challenge is essentially the verifier itself.

That doesn't provide the same protection against someone seeing the challenge and therefore knowing the verifier.

Modern OAuth deployments generally prefer:

```text
S256
```

because it gives the intended one-way transformation.

---

# PKCE doesn't replace `state`

This is a VERY common interview question.

We previously discussed:

```text
state
```

Now we have:

```text
code_verifier
code_challenge
```

People sometimes think:

> "PKCE replaces state."

It doesn't.

They defend different things.

## `state`

Think:

> **"Does this authorization response belong to the browser/client flow that I initiated?"**

It binds the response to the initiating transaction.

## PKCE

Think:

> **"Is the party exchanging this authorization code the same party that started the authorization request?"**

It protects the code exchange using proof of possession.

So:

```text
state
    ↓
protect authorization response / transaction correlation

PKCE
    ↓
protect authorization code exchange
```

They are complementary.

For a browser-based application, you generally want to understand both rather than treating either as a substitute for the other.

---

# PKCE doesn't replace HTTPS either

Another important distinction.

PKCE does not make:

```text
HTTP
```

safe.

You still need:

```text
HTTPS
```

because HTTPS protects the communication channel.

Think of the defenses as layers:

```text
HTTPS
  ↓
protects communication in transit

state
  ↓
binds response to expected transaction

PKCE
  ↓
binds token exchange to client-held verifier

redirect URI validation
  ↓
controls where authorization responses can go
```

Different attack → different defense.

---

# Let's connect this back to our historical story

Look at how each piece emerged:

```text
Password sharing
       ↓
BAD
       ↓
Delegated authorization
       ↓
Access tokens
       ↓
Don't expose access token through browser redirect
       ↓
Authorization Code
       ↓
But mobile/SPAs can't keep client_secret
       ↓
Authorization code can potentially be intercepted
       ↓
PKCE
```

This is exactly the way I want you to understand security protocols.

Not:

> "`code_challenge` is a required OAuth field."

But:

> "Somebody could steal the authorization code, and public clients can't rely on a secret, so we need proof that the exchanger possesses something from the original flow."

That's much harder to forget.

---

# One more important question: Why can't the attacker steal the verifier?

They might.

And that's worth understanding.

PKCE protects against a particular class of attack:

```text
Attacker gets authorization code
but not verifier
```

It does **not** magically protect against a fully compromised client.

For example, if malware completely controls the same mobile application environment and can observe:

```text
code_verifier
```

then PKCE can't save you.

Likewise, PKCE doesn't prevent:

```text
XSS
malware
stolen access tokens
compromised devices
bad application logic
```

Security mechanisms are specific defenses.

---

# Now our OAuth flow is substantially more complete

For a public client using Authorization Code + PKCE:

```text
1. Client generates:
       state
       code_verifier

2. Client derives:
       code_challenge = SHA256(code_verifier)

3. Client → Authorization Server:
       client_id
       redirect_uri
       scope
       state
       response_type=code
       code_challenge
       code_challenge_method=S256

4. User authenticates

5. User grants consent

6. Authorization Server → Client:
       code
       state

7. Client verifies:
       state

8. Client → Authorization Server:
       code
       code_verifier
       redirect_uri
       client_id

9. Authorization Server verifies:
       code
       redirect_uri
       client
       code_verifier

10. Authorization Server → Client:
       access_token
```

There are additional details and security constraints, but this is the mental backbone.

---

# Now stop and notice something profound

We now have **three different things** that are easy to confuse:

```text
Authorization Code
        ↓
temporary artifact used to obtain tokens

Access Token
        ↓
credential used to access protected APIs

Code Verifier
        ↓
temporary secret proving possession by
the client that initiated the OAuth flow
```

And separately:

```text
client_id
        ↓
identifies the client

client_secret
        ↓
authenticates a confidential client

state
        ↓
correlates the authorization response
with the initiating flow
```

These are not interchangeable.

---

# The next problem is where the OAuth story becomes much more interesting

We now have an access token:

```text
access_token = XYZ789
```

PrintMyPhotos sends:

```http
GET /photos
Authorization: Bearer XYZ789
```

Now the **Resource Server** receives it.

And it has to answer:

> Who issued this token?

> Is it intended for me?

> Is it expired?

> What permissions does it carry?

> Is it a JWT?

> Do I need to call the Authorization Server to validate it?

> If it's a JWT, how do I validate the signature?

> What does `scope=photos:read` actually mean?

> What prevents a token issued for API A from being used against API B?

> What happens when the token is stolen?

This takes us into the distinction between **access tokens, JWTs, opaque tokens, scopes, claims, audiences, issuers, expiration, and introspection**.

And this is where OAuth and JWT finally meet.

---

# Part 7 — Access Tokens: what exactly did OAuth give us?

We've reached the point where the authorization flow has completed.

Let's pick up exactly here.

```text
Alice
   ↓
Authorization Server
   ↓
Authorization Code
   ↓
Client
   ↓
Token Exchange
   ↓
Access Token
```

Now the client has something like:

```text
access_token = XYZ123
```

and sends:

```http
GET /photos
Authorization: Bearer XYZ123
```

The **Resource Server** receives it.

And now we have a new set of questions.

> What exactly is an access token?

> Who trusts it?

> How does the API validate it?

> Does it contain Alice's information?

> Is it a JWT?

> Why can't the API simply trust whatever the client says?

These are foundational questions.

---

# 1. First: what is an access token?

The OAuth specification intentionally keeps the definition fairly abstract.

An access token is essentially:

> **A credential representing authorization issued to a client.** ([RFC Editor][1])

Notice the word:

**authorization**.

Not:

**identity**.

That distinction is extremely important.

Suppose Alice authorizes:

```text
PrintMyPhotos
```

to:

```text
photos:read
```

The resulting token represents something like:

```text
"PrintMyPhotos has been authorized
to access certain photo resources."
```

It does not necessarily mean:

```text
"PrintMyPhotos is Alice."
```

Think:

```text
             AUTHENTICATION

Alice ───────────────> Google
            "Who are you?"

                         ↓

             AUTHORIZATION

Alice ───────────────> Grant:
                         PrintMyPhotos
                         photos:read

                         ↓

                    ACCESS TOKEN

                         ↓

                  Google Photos API
```

This distinction is one of the biggest things people get wrong in OAuth interviews.

---

# 2. The Resource Server has to make a decision

PrintMyPhotos sends:

```http
GET /photos
Authorization: Bearer XYZ123
```

The Photos API cannot simply say:

> "The client sent a token, therefore allow."

It needs to determine whether the token is acceptable.

Conceptually:

```text
                access token
                     ↓
              Resource Server
                     ↓
         ┌───────────┴──────────┐
         │                      │
      Valid?                 Invalid?
         │                      │
        YES                     NO
         │                      │
      continue                 401
```

The OAuth bearer-token specification describes exactly this model: the client presents the access token to the protected resource, and the resource server validates it before serving the request. ([RFC Editor][1])

But **how** does it validate it?

That's where things get interesting.

---

# 3. Two broad kinds of access tokens

There are two major ways to design an access token.

### Option A — Opaque token

The token is basically an identifier:

```text
XYZ123ABC...
```

The client doesn't know what is inside.

The Resource Server might ask the Authorization Server:

```text
Resource Server
      |
      | "What is XYZ123?"
      v
Authorization Server
      |
      | "It belongs to Alice,
      |  scope=photos:read,
      |  expires at 11:00..."
      v
Resource Server
```

This is commonly associated with **token introspection**.

---

### Option B — Self-contained token

The token contains claims that the Resource Server can validate itself.

For example:

```json
{
  "iss": "https://auth.example.com",
  "sub": "alice",
  "aud": "photos-api",
  "scope": "photos:read",
  "exp": 1787906400
}
```

A signed JWT is the most common form you'll encounter.

Now:

```text
Resource Server
      |
      | verify token locally
      |
      +--> signature
      +--> issuer
      +--> audience
      +--> expiration
      +--> scope
```

No introspection call is necessarily required.

This is the point where **OAuth and JWT meet**.

But here's the critical sentence:

> **An access token does not have to be a JWT.**

OAuth defines the concept of an access token, not "access token = JWT." The bearer-token specification explicitly treats the token as a credential/string and does not require the JWT format. ([RFC Editor][1])

---

# 4. This gives us a very useful taxonomy

Think of these as separate concepts:

```text
OAuth
  ↓
protocol/framework for delegated authorization

Access Token
  ↓
credential representing granted authorization

JWT
  ↓
a token representation/format containing claims,
typically digitally signed

Bearer Token
  ↓
a token that whoever possesses it can use
```

Therefore:

```text
OAuth Access Token
       |
       +---- opaque bearer token
       |
       +---- JWT bearer token
       |
       +---- potentially sender-constrained token
```

Don't collapse these concepts into one.

---

# 5. Let's start with opaque tokens

Imagine the Authorization Server issues:

```text
XYZ123
```

That's all PrintMyPhotos sees.

It cannot look at it and determine:

```text
user = Alice
scope = photos:read
expires = 11:00
```

The information lives on the server side.

Something conceptually like:

```text
Authorization Server

Token       Subject     Client          Scope
---------------------------------------------------
XYZ123      Alice       PrintMyPhotos   photos:read
ABC456      Bob         CalendarApp     calendar:read
```

Then Photos API receives:

```http
Authorization: Bearer XYZ123
```

and calls the authorization/introspection endpoint:

```http
POST /introspect
token=XYZ123
```

The Authorization Server might respond:

```json
{
  "active": true,
  "sub": "alice",
  "client_id": "printmyphotos",
  "scope": "photos:read",
  "exp": 1787906400
}
```

Then:

```text
active = true
scope contains photos:read
audience is correct
not expired
```

→ allow.

---

# 6. Why would anyone want this?

Because the server retains control.

Suppose Alice's access token is:

```text
XYZ123
```

and at 10:15 we revoke it.

Server state becomes:

```text
XYZ123 → revoked
```

At 10:16:

```text
PrintMyPhotos → /photos
Bearer XYZ123
```

The Resource Server asks:

```text
Is XYZ123 active?
```

Answer:

```text
NO
```

Request rejected.

That makes revocation relatively straightforward.

---

# 7. But now we introduced another problem

Every API request could potentially require:

```text
Photos API
      ↓
Authorization Server
      ↓
token lookup/introspection
      ↓
Photos API
```

Imagine:

```text
100,000 requests/sec
```

Suddenly your authorization server becomes part of the critical path.

You now care about:

```text
latency
availability
network traffic
caching
authorization-server scalability
failure modes
```

Suppose the Authorization Server goes down.

Does the Photos API suddenly become unable to validate every token?

You have to design the answer.

---

# 8. Engineers ask: "Why not validate locally?"

This brings us back to our earlier JWT idea.

Instead of:

```text
token = XYZ123
```

we create something containing claims:

```json
{
  "sub": "alice",
  "scope": "photos:read",
  "exp": 1787906400
}
```

and sign it.

Then:

```text
Photos API
    |
    | receive JWT
    ↓
verify signature
    ↓
check claims
    ↓
ALLOW / DENY
```

No central lookup is required for every request.

This is the main operational attraction of self-contained signed tokens.

---

# 9. But now revocation gets hard again

Remember our earlier problem.

Suppose we issue:

```text
JWT
expires in 1 hour
```

At:

```text
10:00
```

Alice has:

```text
scope = photos:read
```

At:

```text
10:15
```

we revoke her access.

But the JWT still cryptographically verifies.

The API checks:

```text
signature valid? YES
exp valid? YES
```

So without some additional state/check:

```text
ALLOW
```

We've traded:

```text
central state
```

for:

```text
local validation
```

and in the process made immediate revocation more difficult.

This is a classic distributed-systems tradeoff.

---

# 10. Stateless vs stateful isn't "secure vs insecure"

This is another misconception worth removing.

You might hear:

> "JWT is stateless and sessions are stateful, therefore JWT is better."

No.

The real tradeoff is closer to:

```text
Stateful session / opaque token

Central state
     ↓
Easy to change/revoke centrally
     ↓
More server-side coordination


Self-contained JWT

More local validation
     ↓
Less per-request central coordination
     ↓
Claims can become stale
     ↓
Revocation is harder
```

Neither is universally superior.

---

# 11. Let's look very carefully at a JWT access token

Suppose we receive:

```text
eyJhbGciOiJSUzI1NiIs...
```

After decoding the JWT payload, we might get:

```json
{
  "iss": "https://auth.example.com",
  "sub": "12345",
  "aud": "https://photos.example.com",
  "scope": "photos:read",
  "iat": 1787902800,
  "exp": 1787906400
}
```

Now the Resource Server has useful information.

Let's derive what each claim is solving.

---

# `iss` — issuer

```json
"iss": "https://auth.example.com"
```

This answers:

> **Who issued this token?**

Suppose your API trusts:

```text
https://auth.example.com
```

but receives a perfectly validly signed JWT from:

```text
https://evil.example.com
```

The signature might be valid relative to Evil's key.

But your API shouldn't trust Evil.

Therefore:

```text
signature valid
+
issuer is trusted
```

are separate checks.

---

# `sub` — subject

```json
"sub": "12345"
```

This tells you the subject the token refers to.

Often this identifies a user or another principal.

But don't blindly interpret:

```text
sub = user
```

as a universal rule.

The exact semantics depend on the token's issuer and context.

Conceptually:

```text
sub
 ↓
"Who/what is this authorization about?"
```

---

# `aud` — audience

This one is incredibly important.

Suppose your company has:

```text
Orders API
Payments API
Photos API
```

Imagine a token intended for:

```text
Payments API
```

is stolen.

Why should the attacker be able to send it to:

```text
Photos API
```

?

They shouldn't.

So the authorization server can issue a token whose audience identifies the intended resource server.

For example:

```json
"aud": "payments-api"
```

Payments API:

```text
aud == payments-api
      ↓
accept
```

Photos API:

```text
aud == payments-api
      ↓
wrong audience
      ↓
reject
```

Current OAuth security best practice specifically recommends restricting access tokens to their intended resource server(s), because this limits the damage from token leakage. ([RFC Editor][2])

This is an extremely good interview concept.

> **A valid token isn't necessarily valid for my API.**

You need to validate **audience**.

---

# `exp` — expiration

Example:

```json
"exp": 1787906400
```

This tells the Resource Server when the token expires.

Why?

Because an access token is a credential.

If it never expired:

```text
token stolen
      ↓
attacker can potentially use it forever
```

With expiration:

```text
token stolen
      ↓
limited lifetime
      ↓
eventually useless
```

This is damage containment.

It doesn't prevent theft.

It limits how long the stolen credential remains usable.

---

# `iat` — issued at

```json
"iat": 1787902800
```

Conceptually:

> When was this token created?

This can help with validation, auditing, token freshness policies, etc.

---

# `scope`

Example:

```json
"scope": "photos:read"
```

This is where authorization becomes concrete.

Suppose API endpoints are:

```text
GET    /photos
DELETE /photos/:id
UPLOAD /photos
```

and scopes are:

```text
photos:read
photos:delete
photos:write
```

A token containing:

```text
photos:read
```

should not authorize:

```http
DELETE /photos/123
```

The API checks:

```text
required permission:
    photos:delete

token permission:
    photos:read
```

→ reject.

---

# This gives us two different levels of authorization

Consider:

```text
Token
  |
  | scope = photos:read
  v
Resource Server
  |
  | checks endpoint
  v
GET /photos
```

This is coarse-grained authorization.

But an application may need:

```text
Alice can read photos
BUT
only photos belonging to tenant 123
AND
not private photos
```

Now scopes alone aren't enough.

This eventually takes us into:

```text
roles
claims
attributes
policies
resource-level authorization
ABAC
RBAC
```

We'll get there later.

For now, understand:

> **An access token provides input to authorization; it doesn't magically implement your entire authorization policy.**

---

# 12. JWT signature validation isn't enough

This deserves a concrete example.

Suppose the API receives:

```json
{
  "iss": "https://auth.example.com",
  "sub": "alice",
  "aud": "payments-api",
  "scope": "payments:read",
  "exp": 1787906400
}
```

and the signature is valid.

Your API is:

```text
photos-api
```

Can it accept the token?

No.

Because:

```text
aud = payments-api
```

while:

```text
API = photos-api
```

The token may be authentic but **not intended for this API**.

Therefore:

```text
valid signature
        ≠
valid request
```

You have to perform the semantic checks too.

---

# 13. The "bearer" part is especially important

Most OAuth access tokens historically have been bearer tokens.

The model is:

```text
Authorization: Bearer XYZ123
```

"Bearer" essentially means:

> **Whoever possesses the token can use it, without separately proving possession of a cryptographic key.** ([RFC Editor][1])

Think about your physical metro ticket.

If the ticket is bearer-style:

```text
Whoever holds ticket
        ↓
can use ticket
```

It isn't tied to your identity.

Likewise:

```text
Bearer Access Token

Alice has token
       ↓
works

Attacker steals token
       ↓
potentially works too
```

This is one of the biggest weaknesses of bearer tokens.

---

# 14. This explains why token leakage is such a big deal

Imagine:

```text
access_token = XYZ123
```

gets accidentally written to logs:

```text
2026-08-28:
Authorization: Bearer XYZ123
```

If an attacker gets the log:

```text
Attacker
   ↓
XYZ123
   ↓
GET /photos
Authorization: Bearer XYZ123
```

the Resource Server might accept it.

This is why OAuth's current security best-practice guidance treats access tokens as sensitive secrets and recommends measures such as **audience restriction** and, where appropriate, **sender-constrained access tokens** to reduce replay risk. ([RFC Editor][2])

---

# 15. So people asked: "Can we make the token non-bearer?"

That leads to **sender-constrained tokens**.

Instead of:

```text
Token alone
   ↓
access granted
```

we want:

```text
Token
+
proof that you're the authorized holder
   ↓
access granted
```

For example, a token could be bound to a key.

Then:

```text
Attacker steals token
       ↓
has token
       ↓
doesn't have private key
       ↓
cannot use it
```

One standardized mechanism is **DPoP**; another is mutual TLS-based sender-constrained tokens. OAuth security guidance recommends sender-constraining where appropriate to reduce misuse of stolen access tokens. ([RFC Editor][2])

We're not going deeply into DPoP yet because it belongs later in the "advanced OAuth security" part of our story.

But it's important that you see **why it exists**.

---

# 16. Compare our two big access-token designs

Here's the conceptual tradeoff.

|                                       | Opaque Token                    | Signed JWT      |
| ------------------------------------- | ------------------------------- | --------------- |
| Contains claims directly?             | Usually no                      | Yes             |
| Resource server can validate locally? | Usually no                      | Often yes       |
| Needs introspection?                  | Commonly                        | Not necessarily |
| Central revocation                    | Easier                          | Harder          |
| Per-request network dependency        | Potentially yes                 | Potentially no  |
| Token size                            | Often smaller                   | Often larger    |
| Key/signature management              | Less exposed to resource server | Important       |
| Can carry structured claims           | Not directly                    | Yes             |

This is not:

```text
opaque = old/bad
JWT = modern/good
```

It is an architectural choice.

---

# 17. A very important architecture decision

Suppose you have:

```text
                Authorization Server
                       |
             access token
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
      API A          API B          API C
```

With opaque tokens:

```text
API A → introspection
API B → introspection
API C → introspection
```

With signed JWTs:

```text
API A → local verification
API B → local verification
API C → local verification
```

The second architecture can be operationally attractive because resource servers don't have to contact the authorization server on every request.

But you've moved more responsibility to the APIs:

```text
key distribution
key rotation
claim validation
issuer validation
audience validation
clock handling
algorithm configuration
```

Security has not disappeared.

The responsibility moved.

---

# 18. Key rotation introduces another problem

Suppose your Authorization Server signs JWTs with:

```text
private_key_A
```

Resource servers use:

```text
public_key_A
```

Eventually you need to rotate the key.

Why?

Because cryptographic keys have lifecycles and should not be treated as permanent.

Now we need:

```text
Key A
   ↓
Key B
```

while tokens signed with A may still be valid.

So the resource server needs to know:

```text
Which public key verifies this token?
```

JWT headers commonly include:

```json
{
  "alg": "RS256",
  "kid": "key-2026-08"
}
```

`kid` can identify the signing key.

The authorization server can publish its public keys through a key-set mechanism.

Then resource servers can retrieve:

```text
key-2026-08 → public key
key-2026-09 → public key
```

This is another example of something that sounds like "JWT complexity" but actually comes from a real operational problem:

> **Distributed services need to know which verification key is currently valid.**

---

# 19. Let's build the full validation mentally

Suppose:

```http
GET /photos
Authorization: Bearer <JWT>
```

The Photos API should conceptually go through something like:

```text
        Receive token
             ↓
       parse token
             ↓
      verify signature
             ↓
        verify issuer
             ↓
       verify audience
             ↓
     verify time validity
             ↓
      check permissions
             ↓
      application policy
             ↓
          ALLOW
```

Notice that:

```text
signature
```

is only one step.

This is probably the single most useful correction to your mental model from this chapter.

---

# 20. An interesting consequence: "stateless" is not absolute

People often say:

> "JWT makes the system stateless."

That's an oversimplification.

Imagine you add:

```text
revoked_token_id → database
```

Now your system has state again.

Or:

```text
user_disabled → cache
```

Again, state.

Or:

```text
permission service
```

Again, state.

Therefore:

> **JWT can remove the need for a particular kind of central session lookup; it does not magically make a distributed application stateless.**

That is a much stronger interview answer.

---

# Where we've arrived

Our original OAuth story was:

```text
Alice doesn't give password to PrintMyPhotos
                 ↓
delegated authorization
                 ↓
authorization code
                 ↓
access token
```

Now we've unpacked that access token:

```text
Access Token
    |
    +--> opaque
    |      |
    |      +--> introspection / server-side state
    |
    +--> structured
           |
           +--> often JWT
                  |
                  +--> claims
                  +--> signature
                  +--> local validation
```

And we discovered another chain of problems:

```text
Bearer token
    ↓
stolen token can be replayed
    ↓
audience restriction helps limit where it works
    ↓
short expiration limits lifetime
    ↓
sender-constrained tokens can reduce replay
```

Current OAuth security guidance explicitly recommends minimizing token privilege, restricting audience, and considering sender-constrained access tokens because stolen bearer tokens can otherwise be replayed. ([RFC Editor][2])

---

# The next problem in the story

There's now one elephant in the room.

Our access token expires.

Suppose:

```text
Access token lifetime = 15 minutes
```

That's good security.

But imagine Alice is using an application for eight hours.

At 15 minutes:

```text
access token
      ↓
EXPIRED
```

Should Alice be forced to log in again every 15 minutes?

Obviously not.

So we need another credential.

And this creates one of the most misunderstood concepts in OAuth:

# **Refresh Tokens**

The interesting part isn't just what a refresh token is.

The interesting story is:

> **Why is it safe—or supposedly safer—to have another, potentially longer-lived credential? Why don't we simply make the access token last 8 hours? What exactly does the refresh token buy us? What happens when the refresh token is stolen? Why does refresh-token rotation exist?**

That's the next layer of the story.

[1]: https://www.rfc-editor.org/info/rfc6750/?utm_source=chatgpt.com "RFC 6750: The OAuth 2.0 Authorization Framework: Bearer Token Usage | RFC Editor"
[2]: https://www.rfc-editor.org/info/rfc9700/?utm_source=chatgpt.com "RFC 9700: Best Current Practice for OAuth 2.0 Security | RFC Editor"


---

# Part 8 — Refresh Tokens: "Why not just make the access token last longer?"

We have a good OAuth model now.

Alice authorized:

```text
PrintMyPhotos
      ↓
photos:read
```

The Authorization Server issued:

```text
access_token = A123
```

PrintMyPhotos uses it:

```http
GET /photos
Authorization: Bearer A123
```

But we deliberately made the access token short-lived.

Let's understand **why**.

---

# The problem with long-lived access tokens

Suppose we say:

> "Alice doesn't want to log in repeatedly. Let's just make the access token valid for 30 days."

Then:

```text
access_token = A123
valid for = 30 days
```

Everything is convenient.

But remember:

```text
Bearer token
    ↓
whoever possesses it can potentially use it
```

Suppose A123 leaks on day 2.

```text
Day 2
Alice's token stolen
        ↓
Attacker has A123
        ↓
Can potentially access resources
        ↓
for another 28 days
```

That's a huge blast radius.

So we have a tradeoff:

```text
Long-lived access token
    ↓
good user experience
    ↓
bad if stolen


Short-lived access token
    ↓
better security
    ↓
but user would need to re-authorize/re-authenticate frequently
```

We want both:

```text
short-lived access token
+
long-lived way to obtain a new access token
```

And that's where the refresh token comes in.

---

# The basic idea

Instead of giving the client only:

```text
Access Token
```

the Authorization Server can return:

```json
{
  "access_token": "A123",
  "refresh_token": "R456",
  "expires_in": 900
}
```

Meaning roughly:

```text
A123
 ↓
use this to call APIs
 ↓
expires quickly

R456
 ↓
use this to obtain another access token
```

So the relationship is:

```text
Refresh Token
       ↓
new Access Token
       ↓
Resource Server
```

The refresh token is **not normally sent to the Resource Server**.

That's an important distinction.

---

# Why two tokens?

Let's make the access token lifetime:

```text
15 minutes
```

Then:

```text
10:00
Access Token A

10:15
Access Token A expires

Client:
Refresh Token R

        ↓

Authorization Server

        ↓

Access Token B

10:15–10:30
Access Token B
```

No need for Alice to log in again.

So we've separated two jobs:

```text
Access Token
    = authorization to access resources now

Refresh Token
    = credential used to obtain a new access token
```

This separation is one of the most important concepts in OAuth.

---

# Why don't we just refresh with the access token?

Because it is already expired.

And more fundamentally, we want different security properties.

Think of:

```text
Access Token:
    short-lived
    frequently exposed to APIs
```

versus:

```text
Refresh Token:
    longer-lived
    used only with Authorization Server
```

The refresh token has much narrower usage.

If a refresh token is valid, it doesn't mean:

```text
"Call the Photos API with me."
```

Instead:

```text
"Present me to the Authorization Server
and request a new access token."
```

That's a very different capability.

---

# The architecture

Before refresh tokens:

```text
Authorization Server
        |
        | Access Token
        v
     Client
        |
        | Access Token
        v
  Resource Server
```

With refresh tokens:

```text
                   ┌─────────────────────┐
                   │ Authorization       │
                   │ Server              │
                   └─────────┬───────────┘
                             │
                    Access + Refresh
                          Tokens
                             │
                             v
                           Client
                          /      \
                         /        \
                Access Token    Refresh Token
                    |                |
                    v                |
             Resource Server        |
                                     |
                                     └──────> Authorization Server
                                               |
                                               v
                                         New Access Token
```

Notice:

```text
Refresh Token
       X
       |
       X----> Resource Server
```

It goes to the Authorization Server.

---

# Let's walk through a complete example

Suppose Alice logs into PrintMyPhotos and authorizes access.

The Authorization Server returns:

```json
{
  "access_token": "A123",
  "refresh_token": "R456",
  "token_type": "Bearer",
  "expires_in": 900
}
```

The client stores the credentials appropriately.

For the next 15 minutes:

```http
GET /photos
Authorization: Bearer A123
```

At 15 minutes:

```text
A123 expired
```

The client sends:

```http
POST /token

grant_type=refresh_token
refresh_token=R456
```

The Authorization Server validates R456.

If valid, it returns:

```json
{
  "access_token": "B789",
  "expires_in": 900
}
```

The client then resumes:

```http
GET /photos
Authorization: Bearer B789
```

Alice doesn't have to do anything.

---

# So why isn't the refresh token itself short-lived?

Now we have an obvious question.

If access tokens are risky because they're bearer credentials, then:

> "Isn't the refresh token also dangerous?"

Yes.

Potentially **very** dangerous.

In fact, in many architectures, a refresh token is more valuable than a single access token because it can be used to obtain new access tokens.

So the security goal isn't:

> "Refresh tokens are safe."

It is:

> **Give the long-lived credential a narrower usage path and protect it more carefully.**

This is a general security pattern:

```text
High-value credential
       ↓
restrict where it can be used
       ↓
reduce exposure
```

---

# Why not just make access tokens 8 hours?

Now the tradeoff should be clear.

### Design A

```text
Access Token
8 hours
```

If stolen:

```text
attacker has API access
for potentially hours
```

### Design B

```text
Access Token
15 minutes

Refresh Token
longer-lived
```

If the access token is stolen:

```text
damage window ≈ short
```

The legitimate client can continue obtaining new access tokens through:

```text
Refresh Token → Authorization Server
```

So refresh tokens let us keep the **API-facing credential short-lived** without making the user's experience terrible.

---

# But now we've created a new problem

Let's say:

```text
Refresh Token = R456
```

An attacker steals R456.

They send:

```http
POST /token

grant_type=refresh_token
refresh_token=R456
```

The Authorization Server says:

```text
valid refresh token
        ↓
new access token
```

The attacker can keep doing that.

So we've moved the problem:

```text
Long-lived access token
        ↓
stolen
        ↓
direct API access
```

to:

```text
Long-lived refresh token
        ↓
stolen
        ↓
continuous access-token generation
```

Now we need to solve **refresh-token theft**.

This leads to one of the most important mechanisms:

# Refresh Token Rotation

---

# First, imagine non-rotating refresh tokens

Suppose:

```text
R1 = refresh token
```

Client uses it:

```text
R1 → Authorization Server
```

and receives:

```text
A2
```

But R1 remains valid.

Later:

```text
R1 → Authorization Server
```

again.

Still valid.

So if an attacker steals R1:

```text
Attacker
   |
   | R1
   v
Authorization Server
   |
   | new access token
   v
Attacker
```

There is no easy way to know which party is legitimate.

---

# Rotation changes the model

Instead:

```text
R1
 ↓
used once
 ↓
A2 + R2
```

Now:

```text
R1 → INVALID
R2 → VALID
```

Client next time uses:

```text
R2
 ↓
A3 + R3
```

and then:

```text
R2 → INVALID
R3 → VALID
```

So:

```text
R1 → R2 → R3 → R4 → ...
```

This is **refresh token rotation**.

---

# Why is rotation useful?

Consider this attack.

Alice has:

```text
R1
```

Attacker somehow steals a copy:

```text
Attacker → R1
```

Alice's real client uses R1 first:

```text
Alice's client
     |
     | R1
     v
Authorization Server
     |
     | R2
     v
Alice's client
```

Now R1 is invalid.

Later attacker tries:

```text
Attacker
   |
   | R1
   v
Authorization Server
```

The server sees:

```text
R1 has already been used
```

That is a signal that something is wrong.

The authorization server can potentially revoke the refresh-token family/session.

So rotation gives us something powerful:

> **Replay detection.**

Not merely:

> "Make the token expire."

---

# Think of refresh-token rotation as a chain

```text
R1
 │
 ├── used by legitimate client
 │
 ▼
R2
 │
 ├── used
 ▼
R3
 │
 ├── used
 ▼
R4
```

Each token is consumed to obtain its successor.

Now imagine:

```text
          R1
         /  \
        /    \
 Alice uses   Attacker uses
      |            |
      v            v
     R2          ????
```

The Authorization Server can recognize:

```text
R1 was already consumed
```

and treat that as suspicious.

The exact response depends on the implementation and token-family/session policy, but the general purpose is to detect replay and contain the compromise.

---

# A subtle point: rotation does not magically make theft impossible

Suppose the attacker steals:

```text
R3
```

before Alice uses it.

They may race Alice.

If attacker gets there first:

```text
Attacker
   |
   | R3
   v
Authorization Server
   |
   | R4
   v
Attacker
```

Now Alice's subsequent use of R3 may be detected as a replay.

So rotation is about:

```text
reducing replay window
+
detecting reuse
+
containing compromise
```

It does not mean:

```text
"stolen refresh token is harmless."
```

---

# Now we encounter an important security pattern

Compare access-token expiration and refresh-token rotation.

### Expiration

Limits:

> **How long a credential remains valid.**

### Rotation

Limits:

> **How many times a refresh credential can be successfully reused.**

### Revocation

Allows:

> **Central invalidation of a credential/session.**

These are different mechanisms.

---

# Who can issue a refresh token?

The Authorization Server.

This is important.

A Resource Server generally doesn't create refresh tokens.

The basic relationship is:

```text
Authorization Server
       |
       | issues
       +--------> Access Token
       |
       +--------> Refresh Token
```

Then:

```text
Client
   |
   | Access Token
   v
Resource Server
```

and:

```text
Client
   |
   | Refresh Token
   v
Authorization Server
```

So the Resource Server doesn't need to understand how to refresh.

---

# Why shouldn't the Resource Server accept a refresh token?

Because its job is:

```text
"Serve protected resources."
```

A refresh token's job is:

```text
"Obtain a new access token."
```

These are deliberately separated capabilities.

If a Photos API accepted refresh tokens directly, you've widened the attack surface and blurred responsibilities.

---

# Another important distinction: authentication vs token renewal

Suppose Alice's access token expires.

The client sends:

```text
refresh_token=R3
```

Does that mean Alice just "logged in again"?

Not exactly.

The client is using an existing authorization grant/refresh credential to obtain another access token.

The Authorization Server can apply its own policies during refresh.

For example, it may:

```text
verify refresh token
verify client
check grant state
check consent/authorization status
check whether the grant has been revoked
issue new access token
rotate refresh token
```

So:

```text
refresh
≠
user entering password again
```

---

# Why would the Authorization Server deny a refresh?

Because the underlying authorization may no longer be valid.

For example:

```text
Alice authorized PrintMyPhotos
```

Later:

```text
Alice revokes PrintMyPhotos
```

Authorization Server marks the grant/relevant refresh credential as invalid.

Then:

```http
POST /token
grant_type=refresh_token
refresh_token=R3
```

returns an error rather than another access token.

This is another reason refresh tokens are useful:

> **They provide a server-controlled long-lived relationship between the client and the authorization grant, without forcing the access token itself to remain long-lived.**

---

# Now let's revisit JWT

Suppose the access token is:

```text
JWT
```

and expires after:

```text
15 minutes
```

The refresh token might be:

```text
opaque random value
```

Notice:

> **There is no requirement that access and refresh tokens have the same format.**

You could have:

```text
Access Token → JWT
Refresh Token → opaque string
```

This is actually a very common design because the refresh token doesn't need to be exposed to every Resource Server.

Conceptually:

```text
JWT Access Token
       ↓
many APIs see it


Opaque Refresh Token
       ↓
only Authorization Server sees it
```

That's a very sensible separation of responsibilities.

---

# Where should refresh tokens be stored?

This is where things become architecture-dependent.

A refresh token is sensitive.

For example, in a browser application, putting a long-lived refresh token somewhere easily accessible to arbitrary JavaScript creates significant risk.

This is one reason browser architectures often use designs such as:

```text
Browser
   ↕
Backend for Frontend (BFF)
   ↕
Authorization Server
   ↕
APIs
```

where the browser may only hold a secure session cookie, while the backend handles tokens.

Other architectures use carefully protected browser storage and additional defenses.

The important lesson at this stage is not to memorize "always use X."

It is:

> **The longer-lived the credential, the more carefully you need to consider its storage, transport, theft, replay, and revocation.**

---

# Let's connect the whole story

We've now evolved the system through several generations:

```text
1. Password
      ↓
   primary credential

2. Session ID
      ↓
   server-side authenticated state

3. Signed JWT
      ↓
   self-contained claims

4. Access Token
      ↓
   delegated authorization

5. Short-lived Access Token
      ↓
   reduce damage from theft

6. Refresh Token
      ↓
   obtain new access tokens without
   asking user to authenticate repeatedly

7. Refresh Token Rotation
      ↓
   reduce replay and detect reuse
```

Every step exists because the previous design created a problem.

---

# The most useful comparison so far

| Credential         | Main purpose                 | Typical lifetime | Where it's used                 |
| ------------------ | ---------------------------- | ---------------: | ------------------------------- |
| Password           | Authenticate user            |             Long | Authorization/identity provider |
| Session ID         | Identify server-side session |    Session-based | Application                     |
| Access Token       | Access protected resource    |            Short | Resource Server                 |
| Refresh Token      | Obtain new access token      |             Long | Authorization Server            |
| Authorization Code | Exchange for tokens          |       Very short | Authorization Server            |

This table is worth learning conceptually, not memorizing mechanically.

---

# One subtle issue we haven't covered yet

We've been saying:

```text
Refresh Token → new Access Token
```

But **why is the Authorization Server willing to trust the refresh token?**

And what exactly is it remembering about that token?

For example:

```text
R3
 ↓
which user?
which client?
which authorization grant?
which scopes?
which audience?
still active?
rotated?
revoked?
```

This takes us into an important concept that sits underneath OAuth:

# The Authorization Grant

The Authorization Server isn't merely thinking:

> "R3 is a random string, therefore issue another token."

It has an underlying record of the relationship:

```text
Alice
   +
PrintMyPhotos
   +
photos:read
   +
authorization granted
```

The refresh token is tied to that state/grant.

Understanding this will make the next pieces—**consent, scopes, revocation, token lifetime, grant lifecycle, and why refresh tokens are not simply "permanent passwords"**—much easier.

And after that, we're ready for one of the biggest conceptual traps in the entire subject:

> **OAuth gives us authorization, but how does a website use Google to answer "Who is this user?"**

That's where we'll finally separate **OAuth from OpenID Connect (OIDC)** and understand why "Login with Google" is not merely "OAuth login."

---

# Part 9 — OAuth is authorization, so how did "Login with Google" happen?

This is one of the most important points in the entire topic.

We've spent all this time building OAuth:

```text
Alice
  ↓
Authorization Server
  ↓
"Allow PrintMyPhotos to read my photos"
  ↓
Access Token
  ↓
Photos API
```

Everything makes sense.

Then developers started saying:

> "Great! We'll use OAuth to let users log into our application with Google."

And that sounds reasonable.

But there's a conceptual problem.

---

# OAuth answers the wrong question for login

Remember:

```text
OAuth:
    "What may this client access?"
```

But login asks:

```text
"Who is this user?"
```

These are different.

Suppose Google gives PrintMyPhotos this access token:

```text id="q2os9r"
ABC123
```

PrintMyPhotos may know:

```text
this token has:
    photos:read
```

But that doesn't necessarily tell PrintMyPhotos, in a standardized and trustworthy way:

```text
"This is Alice."
```

And even if a token contains:

```json id="3vqu8g"
{
    "sub": "123",
    "scope": "photos:read"
}
```

there are still questions:

```text
Who authenticated the user?
When?
Which authentication event?
Was this intended as an identity assertion?
What is the canonical user identity?
```

OAuth itself was not designed to answer all of those questions.

So another specification was built on top of OAuth.

# OpenID Connect — OIDC

The historical problem is:

```text
OAuth
  ↓
delegated authorization

But applications also want:
  ↓
"user authentication / identity"
```

OIDC fills that gap.

---

# Let's rewind to the moment this became necessary

Imagine PrintMyPhotos says:

> "Don't make me create another username/password for every user. Let people use their Google account."

User clicks:

```text
Continue with Google
```

PrintMyPhotos sends the user through an OAuth Authorization Code flow.

Google authenticates Alice.

Alice consents.

PrintMyPhotos gets:

```text
authorization code
```

and exchanges it for:

```text
access token
```

Now what?

PrintMyPhotos wants to know:

```text
Who just logged in?
```

It could call some Google API:

```text
GET /userinfo
Authorization: Bearer <access_token>
```

and potentially get user information.

That can work.

But we still have a semantic problem.

The application is treating an authorization credential as an identity mechanism.

We need a standardized statement from the identity provider saying:

> **"This user authenticated here, and this identifier represents that user."**

That is the purpose of the:

# ID Token

---

# Access Token vs ID Token

This distinction is absolutely fundamental.

## Access Token

Intended for:

```text
Resource Server
```

It answers approximately:

> **"What can this client access?"**

Example:

```text id="n18bu3"
access_token

scope:
    photos:read
```

Destination:

```text id="h9f7kz"
Photos API
```

---

## ID Token

Intended for:

```text
Client application
```

It answers approximately:

> **"Who authenticated, and what authentication event occurred?"**

It's an OIDC identity assertion.

Example:

```json id="a8mql5"
{
  "iss": "https://accounts.example.com",
  "sub": "248289761001",
  "aud": "printmyphotos-client",
  "nonce": "...",
  "iat": 1787906400,
  "exp": 1787906700
}
```

The client uses this to establish the authenticated user's identity according to the issuer's claims and OIDC rules.

---

# Notice the destination difference

This is one of the best ways to remember it.

```text id="wz4tca"
Access Token
     ↓
Resource Server

ID Token
     ↓
Client / Relying Party
```

Or:

```text id="f9k5gx"
                   Authorization Server
                         /       \
                        /         \
                       /           \
              ID Token             Access Token
                 ↓                      ↓
              Client              Resource Server
```

This is not a minor distinction.

Sending an ID token to an API as though it were an access token is a category mistake.

---

# Let's introduce the OIDC actors

We already had OAuth actors:

```text id="53zj1o"
Resource Owner
Client
Authorization Server
Resource Server
```

OIDC introduces another useful term:

> **OpenID Provider (OP)**

This is basically an OAuth Authorization Server that also provides OpenID Connect identity services.

And the application relying on that identity information is called:

> **Relying Party (RP)**

So:

```text id="5e9l9z"
Google
    ↓
OpenID Provider

PrintMyPhotos
    ↓
Relying Party
```

Don't get hung up on terminology yet. The roles matter more than the labels.

---

# Let's build "Login with Google" properly

Suppose Alice visits:

```text id="8o54jp"
printmyphotos.com
```

She clicks:

```text id="q5vql2"
Continue with Google
```

PrintMyPhotos starts an OIDC Authorization Code flow.

The request is conceptually:

```http id="0z4f9g"
GET /authorize?
    client_id=printmyphotos
    &redirect_uri=https://printmyphotos.com/callback
    &response_type=code
    &scope=openid profile email
    &state=RANDOM
    &nonce=RANDOM
```

There's a new thing:

```text id="7qifjc"
scope=openid
```

This is the signal that:

> "This OAuth transaction is also requesting OpenID Connect authentication."

That's an important conceptual marker.

---

# `openid` scope

In ordinary OAuth:

```text id="82q4n7"
scope=photos:read
```

means:

> "I want permission to read photos."

In OIDC:

```text id="2r9hrf"
scope=openid
```

means:

> "I want OpenID Connect authentication information."

Additional scopes can request standardized user-information claims, such as:

```text id="cc3zq0"
profile
email
address
phone
```

The exact claims returned depend on the provider and flow.

---

# Alice authenticates at Google

Alice sees Google's authentication UI.

Important:

```text id="dqsh0p"
Alice's password
        ↓
Google

NOT:

Alice's password
        ↓
PrintMyPhotos
```

This is the OAuth security boundary we already established.

Google authenticates Alice.

Then Alice authorizes the requested scopes.

---

# Google returns the authorization code

Browser redirects:

```text id="f1qr42"
https://printmyphotos.com/callback
    ?code=ABC123
    &state=XYZ
```

PrintMyPhotos validates:

```text id="pp5l6b"
state
```

and exchanges:

```text id="4ybq5u"
code=ABC123
```

for tokens.

---

# But OIDC gives us more than OAuth

The token response may contain:

```json id="2sox6w"
{
  "access_token": "AT123",
  "token_type": "Bearer",
  "expires_in": 3600,
  "id_token": "eyJ..."
}
```

Now we have:

```text id="1j5coq"
Access Token
+
ID Token
```

Two completely different credentials.

---

# What's inside the ID Token?

Typically, an ID token is a JWT.

This is where JWT finally becomes connected to identity.

Suppose we decode it:

```json id="h64rzi"
{
  "iss": "https://accounts.google.com",
  "sub": "109238472398472",
  "aud": "printmyphotos-client",
  "iat": 1787906400,
  "exp": 1787906700,
  "nonce": "RANDOM123"
}
```

Let's understand these claims from the problem they solve.

---

# `iss` — who made this identity assertion?

```text id="x1ouh7"
iss = https://accounts.google.com
```

PrintMyPhotos needs to know:

> "Who is making this statement?"

This prevents blindly trusting a token from an unexpected identity provider.

---

# `sub` — which user?

```text id="x0e7ki"
sub = 109238472398472
```

This is the important identity identifier.

Think:

```text id="7c8j3n"
iss + sub
```

as the stable identity reference within that issuer's namespace.

An important practical lesson:

> Don't use the user's email address as the fundamental identity key just because it is convenient.

Email addresses can change.

The OIDC `sub` claim is designed to provide a stable subject identifier within the issuer's context.

---

# `aud` — intended client

Suppose the ID token says:

```text id="wkf3u1"
aud = printmyphotos-client
```

Why?

Because the ID token is an assertion intended for a particular relying party.

Imagine an attacker obtains a valid ID token issued to:

```text id="bjku72"
SomeOtherApp
```

and tries to present it to:

```text id="p0gxei"
PrintMyPhotos
```

The signature may be valid.

But:

```text id="wfouqf"
aud != printmyphotos-client
```

Therefore PrintMyPhotos should reject it.

This is analogous to why access tokens need the correct audience, but the semantic destination is different.

---

# `exp`

The ID token is temporary.

```text id="hcb7gc"
exp = expiration time
```

Again:

```text
valid now
≠
valid forever
```

---

# `iat`

```text id="x0gcyf"
iat = issued-at time
```

This tells the client when the assertion was created.

---

# And now we encounter one of the most important OIDC protections: `nonce`

Suppose an attacker somehow gets a previously valid ID token.

They try to replay it during a new login.

The relying party needs a way to correlate:

```text id="cugx7l"
"this ID token was created for the authentication request I just started."
```

So the client generates:

```text id="b5j4w3"
nonce = RANDOM123
```

and sends it in the authentication request.

The identity provider includes the nonce in the ID token.

The client checks:

```text id="fyh0vt"
received nonce
      ==
the nonce I generated
```

If not:

```text id="jg4ay2"
REJECT
```

This is conceptually similar to `state`, but it serves a different purpose.

---

# `state` vs `nonce`

This is another common interview question.

Think of:

```text id="o64lk0"
state
    ↓
OAuth transaction / redirect response correlation

nonce
    ↓
bind ID token to the authentication request
```

They're not the same thing.

A useful mental model:

```text id="6y6d3c"
STATE
"Did this response belong to the flow I started?"

NONCE
"Did this identity assertion belong to the authentication
request I started?"
```

In browser-based OIDC flows, using and validating these values appropriately is an important security defense.

---

# Now the login application can establish its own session

This is another beautiful piece of the architecture.

After validating the ID token:

```text id="l3ffnz"
PrintMyPhotos
     ↓
"Google authenticated subject 109238..."
     ↓
Find/create local account
     ↓
Create local session
```

Then PrintMyPhotos can issue:

```http id="zp6xye"
Set-Cookie: session_id=LOCAL123; HttpOnly; Secure; SameSite=Lax
```

Now the application's own requests don't necessarily need to carry the Google access token.

The browser just uses:

```text id="y26m9e"
Cookie: session_id=LOCAL123
```

And PrintMyPhotos knows:

```text id="9g5dvy"
LOCAL123 → local user account
```

This is an extremely common and clean architecture.

---

# So "Login with Google" may actually be:

```text id="2gxq4e"
             Google
                |
         OIDC authentication
                |
            ID Token
                |
                v
       PrintMyPhotos Backend
                |
        create local session
                |
                v
             Browser
                |
         session cookie
```

While separately:

```text id="1ai0zy"
Google Access Token
        |
        v
Google APIs
```

This is why:

> **The token used to log the user into your application and the token used to call Google's APIs can have different purposes.**

---

# This is a huge conceptual milestone

Let's now compare the three things we've studied:

## Session

```text id="b4v9lj"
"Browser has authenticated to my application."
```

## Access Token

```text id="qxyui5"
"This client has authority to access this resource."
```

## ID Token

```text id="c8i0t9"
"This identity provider says this subject
authenticated, and provides identity/authentication claims
for this relying party."
```

And:

## JWT

```text id="9fpw3d"
A token representation format,
often used for ID tokens and sometimes access tokens.
```

These are four different concepts.

---

# The hierarchy is becoming much clearer

```text id="skq0sg"
                     Security
                         |
          +--------------+--------------+
          |                             |
   Authentication                Authorization
          |                             |
      "Who are you?"              "What can you do?"
          |                             |
       OIDC                        OAuth
          |                             |
      ID Token                    Access Token
          |                             |
          +-------------+-------------+
                        |
                      JWT?
                        |
             Often the representation
```

But don't interpret this as:

```text
OIDC always = JWT
OAuth always = JWT
```

That's too simplistic.

The format and the protocol are separate concepts.

---

# A particularly important mistake

Suppose a developer receives an ID token:

```text id="e0p8cp"
eyJ...
```

and sends it to:

```http id="uvr63x"
GET /api/orders
Authorization: Bearer eyJ...
```

The API might see:

> "It is a valid JWT."

That is **not enough**.

An ID token is intended for the client/RP, not as a general-purpose API access token.

The API should expect an appropriate access token intended for it.

This protects the separation:

```text id="b5b2qv"
Identity assertion
        ≠
API authorization credential
```

---

# Now let's update our historical story

We started with:

```text id="j9n5n1"
Password
   ↓
Authentication
```

Then:

```text id="l7d4bw"
Session
   ↓
remember authenticated browser
```

Then:

```text id="9zhwmk"
Cookies + HTTPS + CSRF defenses
```

Then:

```text id="9rlqj5"
Distributed systems
   ↓
self-contained signed tokens
   ↓
JWT
```

Then:

```text id="jcz0e5"
Third-party access
   ↓
OAuth
   ↓
Authorization Code
   ↓
PKCE
   ↓
Access Token
   ↓
Refresh Token
```

Then:

```text id="a76i6b"
OAuth isn't actually login
   ↓
OIDC
   ↓
ID Token
   ↓
standardized identity authentication
```

Now the pieces finally fit together.

---

# But there is still a major missing piece

We've been talking about:

```text
scope=photos:read
```

and:

```text
role=ADMIN
```

and:

```text
permissions
```

But who actually decides:

> **Is Alice allowed to do THIS specific action on THIS specific resource?**

For example:

```text id="h0scxu"
Alice
  |
  | owns project 123
  |
  +--> GET /projects/123       ✅
  |
  +--> DELETE /projects/123   ✅
  |
  +--> GET /projects/456       ❌
```

A token can say:

```text id="g0njwq"
scope=project:read
```

but scope alone doesn't know which specific project Alice may read.

That brings us to the distinction between:

```text id="f4x6t1"
Scopes
Roles
Claims
Permissions
RBAC
ABAC
Resource-level authorization
Policy engines
```

And this is where **Authorization itself** becomes a much deeper subject than merely "check the JWT."

The next chapter should therefore be:

# **Part 10 — Authorization in depth: scopes vs roles vs permissions, RBAC vs ABAC, and what the API actually checks after authentication**

That is the piece that will connect OAuth tokens to the authorization logic you write in a real Spring Boot service.

---

# Part 10 — Authorization: the part developers often mistakenly reduce to "check the JWT"

We've reached an important boundary.

We now understand:

```text
Authentication
    ↓
Who is this?

OAuth
    ↓
What authority has been delegated?

Access Token
    ↓
Credential carrying/representing that authority

JWT
    ↓
One possible representation of the token

OIDC
    ↓
Identity/authentication on top of OAuth
```

Now let's ask the practical question:

> **A request arrives at my Spring Boot API. How do I decide whether I should actually allow it?**

This is where **authorization** really begins.

And there is a crucial realization:

> **Authentication can tell you who the caller is. It does not tell you whether the caller may perform this particular operation on this particular resource.**

---

# Start with the simplest possible system

Imagine our application has users:

```text
Alice
Bob
Charlie
```

and an endpoint:

```http
DELETE /users/123
```

Alice is properly authenticated.

The server knows:

```text
user = Alice
```

Can Alice delete user 123?

We still need another decision.

```text
Authentication:
    Alice ✅

Authorization:
    Can Alice delete user 123?
```

Those are separate checks.

---

# The first naive authorization model

An early/simple application might have:

```text
user.role
```

For example:

```text
Alice    → ADMIN
Bob      → USER
Charlie  → USER
```

And code like:

```java
if (user.getRole().equals("ADMIN")) {
    allow();
} else {
    deny();
}
```

This gives us our first authorization model:

# RBAC — Role-Based Access Control

The idea is:

```text
User
  ↓
Role
  ↓
Permissions
```

For example:

```text
ADMIN
 ├── user:read
 ├── user:create
 ├── user:update
 └── user:delete

USER
 ├── profile:read
 └── profile:update
```

Then:

```text
Alice → ADMIN
```

means Alice inherits:

```text
user:read
user:create
user:update
user:delete
```

---

# Why roles are useful

They're simple.

Suppose a company has:

```text
Admin
Manager
Employee
```

and permissions are fairly stable.

Then:

```text
Admin
   ↓
everything

Manager
   ↓
manage team

Employee
   ↓
normal employee actions
```

is easy to reason about.

In Spring Security, you often encounter concepts such as:

```text
ROLE_ADMIN
ROLE_USER
```

and checks like:

```java
@PreAuthorize("hasRole('ADMIN')")
```

That works nicely.

But now the real-world problems begin.

---

# Problem #1 — "Alice is an admin, but only for one organization"

Suppose our SaaS application has:

```text
Acme Corp
Globex Corp
```

Alice works for Acme.

She's an administrator of:

```text
Acme
```

but an ordinary user of:

```text
Globex
```

A global role:

```text
Alice → ADMIN
```

is too coarse.

Because this would imply:

```text
Alice
 ↓
ADMIN everywhere
```

which is wrong.

We need authorization to consider context.

Something like:

```text
Alice
 +
organization = Acme
 +
resource = Project123
 +
action = DELETE
```

Now roles alone are not enough.

---

# Problem #2 — "Alice can edit projects she owns"

Suppose:

```text
Project 1 → Alice
Project 2 → Bob
```

Both are normal users.

But:

```text
Alice
   ↓
edit Project 1 ✅

Alice
   ↓
edit Project 2 ❌
```

There may be no useful global role that captures this.

What we really care about is:

> **Who owns this resource?**

That leads us toward more fine-grained authorization.

---

# RBAC vs resource-based authorization

RBAC asks:

```text
"What role does Alice have?"
```

Resource-level authorization asks:

```text
"What relationship does Alice have
with THIS resource?"
```

For example:

```text
Alice ──owner──> Project 1
Bob   ──owner──> Project 2
```

Now:

```text
DELETE /projects/1
```

can be evaluated using:

```text
authenticated_user = Alice
project.owner = Alice
```

→ allow.

Whereas:

```text
DELETE /projects/2
```

gives:

```text
authenticated_user = Alice
project.owner = Bob
```

→ deny.

This is authorization that actually understands the resource.

---

# This gives us a hierarchy

Think of authorization as increasingly contextual.

### Level 1 — Authentication

```text
Alice
```

### Level 2 — Role

```text
Alice → ADMIN
```

### Level 3 — Permission

```text
Alice → project:delete
```

### Level 4 — Resource relationship

```text
Alice → owner of Project 123
```

### Level 5 — Context/policy

```text
Alice
  +
owner of Project 123
  +
request from approved network
  +
during working hours
  +
project belongs to her organization
```

As systems become more complex, authorization often moves down this hierarchy.

---

# Now let's return to OAuth `scope`

Earlier we saw:

```text
scope=photos:read
```

This is another authorization concept.

You might think:

> "Scopes are basically roles."

No.

They are related, but they're not the same thing.

---

# What is a scope?

A scope is essentially a coarse-grained authorization value associated with an access token.

For example:

```text
photos:read
photos:write
photos:delete
```

A client asks:

```text
scope=photos:read
```

and the authorization server may grant:

```text
photos:read
```

The Resource Server can then say:

```text
GET /photos
requires photos:read
```

Therefore:

```text
token.scope contains photos:read
        ↓
continue
```

But:

```text
DELETE /photos/123
requires photos:delete
        ↓
token only has photos:read
        ↓
reject
```

---

# Then why not put every permission into scopes?

Because scopes are usually most useful for **delegated API authority**, not representing every possible business rule.

Suppose:

```text
scope = projects:read
```

Can we conclude:

```text
Alice can read Project 123?
```

Not necessarily.

We still need to know:

```text
Does Alice have access to Project 123?
Does her organization own the project?
Is the project deleted?
Is it private?
```

So:

```text
scope
    ↓
coarse-grained authority

application authorization policy
    ↓
fine-grained decision
```

This is an extremely useful distinction.

---

# Example: GitHub-like application

Suppose an access token says:

```text
repo:read
```

Does that mean the caller can read:

```text
every repository on earth?
```

Obviously not.

The token's scope may say:

> "This credential is allowed to access repositories."

The API still determines:

```text
Can this subject access THIS repository?
```

For example:

```text
Alice
   ↓
repo:read
   ↓
repository = private-project
   ↓
Alice is collaborator?
   ↓
YES
```

Only then:

```text
ALLOW
```

So authorization often happens in layers.

---

# Layered authorization

A realistic request can look like:

```text
Request
  ↓
Is caller authenticated?
  ↓
Is access token valid?
  ↓
Is token intended for this API?
  ↓
Does token contain required scope?
  ↓
Does user have required role/permission?
  ↓
Can this user access THIS resource?
  ↓
Does business policy allow the action?
  ↓
ALLOW
```

This is far more realistic than:

```text
JWT valid → ALLOW
```

---

# The biggest security mistake

A developer writes:

```java
if (jwtSignatureValid) {
    return userData;
}
```

That's not authorization.

Signature validation tells you something like:

> "The token was signed by the trusted issuer and wasn't modified."

It doesn't tell you:

```text
what endpoint?
which operation?
which resource?
which tenant?
which business rule?
```

Therefore:

```text
Valid JWT
    ≠
Authorized request
```

This distinction is crucial.

---

# Now let's understand claims

Suppose our JWT contains:

```json
{
  "sub": "alice",
  "role": "ADMIN",
  "tenant": "acme"
}
```

These are **claims**.

A claim is essentially:

> A statement/assertion carried in the token.

The application can use claims as inputs to authorization.

For example:

```text
role = ADMIN
```

might satisfy:

```text
requires ADMIN
```

while:

```text
tenant = ACME
```

might help enforce:

```text
resource.tenant == token.tenant
```

But remember:

> **Putting a value in a token does not make the value inherently true in the business sense.**

The application trusts it only because it trusts the issuer and has decided to trust that particular claim for that purpose.

---

# Now something subtle: who should decide permissions?

Imagine:

```text
Authorization Server
       |
       | token:
       | role=ADMIN
       ↓
Resource Server
```

Suppose ten minutes later:

```text
Alice is demoted
ADMIN → USER
```

The old JWT still says:

```text
role=ADMIN
```

That's the stale-token problem we discussed.

So the application has a choice.

### Strategy A — Trust token claims until expiration

Simple and fast.

```text
JWT
 ↓
role=ADMIN
 ↓
allow
```

But authorization changes may not be immediate.

### Strategy B — Consult current authorization state

For sensitive operations:

```text
JWT
 ↓
identify Alice
 ↓
authorization service/database
 ↓
current role?
 ↓
current permissions?
```

More current, but more state and latency.

### Strategy C — Hybrid

Use token claims for coarse checks:

```text
scope=orders:read
```

and current application state for resource-specific checks:

```text
Does Alice own Order 123?
```

This hybrid model is extremely common and often sensible.

---

# Now RBAC evolves

Simple role systems eventually become messy.

Suppose we have:

```text
ADMIN
MANAGER
EDITOR
SUPPORT
ANALYST
```

and users start needing combinations:

```text
Alice → Manager + Billing + ReadOnlyAudit
Bob   → Editor + Support
```

We start separating:

```text
Role
  ↓
Permissions
```

For example:

```text
Manager
    ↓
team:read
team:update
reports:read
```

Now Alice inherits these permissions.

This is still RBAC, but more sophisticated.

---

# Permission-based authorization

Instead of asking:

```text
Does Alice have ADMIN?
```

we can ask:

```text
Does Alice have user:delete?
```

This is often a better abstraction.

For example:

```text
ADMIN
     ↓
user:read
user:create
user:update
user:delete
```

The endpoint cares about:

```text
user:delete
```

not necessarily:

```text
ADMIN
```

That makes the authorization rules more explicit.

---

# But even permissions aren't enough

Suppose Alice has:

```text
invoice:approve
```

Can she approve:

```text
Invoice #100 owned by Acme India
```

while working for:

```text
Acme US
```

Maybe not.

So:

```text
permission
+
resource attributes
+
user attributes
```

may be required.

And now we arrive at:

# ABAC — Attribute-Based Access Control

Instead of:

```text
Alice → ADMIN
```

we evaluate attributes.

For example:

```text
User:
    department = FINANCE
    country = IN

Resource:
    type = INVOICE
    region = IN
    amount = ₹20,000

Action:
    APPROVE
```

Policy:

```text
allow if:

user.department == "FINANCE"
AND
user.country == resource.region
AND
resource.amount < ₹100,000
```

Now authorization isn't just:

```text
role == ADMIN
```

It's a policy over attributes.

---

# RBAC vs ABAC

Think of them like this:

### RBAC

```text
Who are you in the organization?

Alice → MANAGER
```

### ABAC

```text
What are the relevant properties
of the user, resource, and context?

department = finance
region = india
resource amount = 20k
time = business hours
```

RBAC is generally simpler.

ABAC is more expressive.

But more expressive means:

```text
more policy complexity
more testing
more debugging difficulty
```

---

# A real example combining everything

Suppose:

```http
DELETE /documents/123
Authorization: Bearer <access-token>
```

Token contains:

```json
{
  "sub": "alice",
  "scope": "documents:delete",
  "tenant": "acme"
}
```

Now the API can evaluate.

### Step 1 — Is the token valid?

```text
signature ✅
issuer ✅
audience ✅
expiration ✅
```

### Step 2 — Does the token permit deletion?

```text
scope = documents:delete
```

✅

### Step 3 — Does the user belong to this tenant?

Token:

```text
tenant = acme
```

Document:

```text
tenant = acme
```

✅

### Step 4 — Is Alice actually allowed to delete this document?

Database:

```text
document.owner = Bob
```

Maybe the business policy says only:

```text
owner OR document-admin
```

Alice is neither.

Therefore:

```text
❌ DENY
```

Notice:

> **The token was completely valid.**

And yet:

> **The request was correctly denied.**

That's authorization.

---

# This is why authorization usually belongs at the Resource Server

The Authorization Server may say:

```text
"This client/user was granted some authority."
```

But the Resource Server owns the actual resource.

Therefore it often makes the final authorization decision.

For example:

```text
Authorization Server
      ↓
issues:
scope=documents:delete

Resource Server
      ↓
owns Document #123

Resource Server decides:
Can Alice delete #123?
```

This is a very important architectural separation.

---

# OAuth scope is not your complete business authorization model

This is worth burning into memory:

```text
OAuth scope
    =
what authority the client has been delegated

Application authorization
    =
whether THIS request on THIS resource
is allowed under the application's policies
```

So:

```text
scope=read
```

doesn't mean:

```text
"read anything."
```

It means something closer to:

```text
"the client has been granted the ability to perform
some read-related operations within the defined resource scope."
```

The exact semantics are determined by the authorization system/application.

---

# Now let's revisit multi-tenancy

This is where real enterprise systems get interesting.

Suppose:

```text
Alice → tenant=A
Bob   → tenant=B
```

Alice requests:

```http
GET /orders/999
```

Suppose Order 999 belongs to tenant B.

A token like:

```json
{
  "sub": "alice",
  "scope": "orders:read",
  "tenant": "A"
}
```

is not enough by itself.

The API needs to enforce:

```text
token.tenant == order.tenant
```

Otherwise you can get the classic:

# Broken Object Level Authorization

The caller is authenticated.

The token is valid.

The endpoint is allowed.

But the caller is accessing the **wrong object**.

This is one of the most important real-world authorization vulnerabilities.

Conceptually:

```text
Authentication ✅
Token validity ✅
Scope ✅

Resource ownership ❌
```

Therefore:

```text
DENY
```

This is why resource-level authorization is so important.

---

# A useful authorization formula

For many APIs, you can mentally model:

```text
ALLOW =
    authenticated
    AND token_valid
    AND audience_valid
    AND scope_sufficient
    AND role/permission_sufficient
    AND resource_access_allowed
    AND business_policy_allows
```

Not every application needs every term, but this is a useful mental framework.

---

# And now we have another problem

Our authorization logic is becoming complicated:

```java
if (
    user.getTenant().equals(resource.getTenant())
    && user.hasPermission("DOCUMENT_DELETE")
    && resource.getOwner().equals(user)
    && ...
) {
    allow();
}
```

Then you repeat variations of this across:

```text
Orders
Documents
Invoices
Projects
Users
Reports
```

Soon authorization logic is scattered everywhere.

Developers start asking:

> **Can we make authorization itself a separate policy system?**

And that leads toward:

```text
Policy engines
OPA
Cedar
relationship-based authorization
Zanzibar-style systems
```

We'll eventually reach those, because they become very relevant in microservices and large-scale systems.

But before that, there's something more fundamental we need to understand.

---

# Who decides what goes into the token?

This connects everything we've learned.

Suppose Authorization Server issues:

```json
{
  "sub": "alice",
  "role": "ADMIN",
  "scope": "documents:delete",
  "tenant": "acme"
}
```

Why should the Resource Server trust those claims?

Because:

```text
Resource Server
   ↓
trusts Authorization Server
   ↓
verifies issuer/signature
   ↓
accepts claims according to policy
```

So the trust chain looks like:

```text
Authorization Server
       |
       | signs assertion
       v
     Token
       |
       | presented by client
       v
Resource Server
       |
       | validates issuer/signature/claims
       v
authorization decision
```

This is a powerful idea:

> **The token is an assertion from a trusted authority, not truth magically created by the client.**

---

# But there's a dangerous implication

If the Authorization Server makes a mistake and issues:

```text
role=ADMIN
```

to the wrong person, the Resource Server may trust it.

Or if a service incorrectly interprets:

```text
scope=admin
```

as:

```text
full administrative access
```

then you've created a security problem.

So authorization isn't merely cryptography.

It is:

```text
cryptography
+
protocol correctness
+
identity semantics
+
authorization policy
+
application logic
```

This is why authentication/authorization systems can be deceptively difficult.

---

# Where we are now

We've gone from:

```text
"Is Alice authenticated?"
```

to:

```text
"Does Alice have authority?"
```

and then further:

```text
"Does Alice have the right authority
for THIS operation on THIS resource
under CURRENT business conditions?"
```

The evolution looks like:

```text
Authentication
      ↓
Identity
      ↓
Role
      ↓
Permission
      ↓
Scope
      ↓
Resource relationship
      ↓
Attributes
      ↓
Policy
```

Not every system needs all of these.

The trick is choosing the simplest model that correctly expresses the security requirements.

---

# One subtle distinction worth locking in

There are now **three different authorization layers** you should be able to distinguish:

```text
1. OAuth authorization

"Has Alice/client been granted authority
by the Authorization Server?"

        ↓

2. API authorization

"Is this token allowed to call this endpoint?"

        ↓

3. Business/resource authorization

"Can this principal perform this operation
on THIS specific object?"
```

Example:

```text
OAuth:
    scope = orders:read              ✅

API:
    GET /orders                      ✅

Business:
    Order 123 belongs to tenant B
    Alice belongs to tenant A       ❌
```

Final result:

```text
DENY
```

Even though all the cryptographic/OAuth pieces are valid.

That is the mental model I want you to carry into real backend development.

---

# The next problem in our story

We now have a sophisticated authorization system.

But there's another huge practical question:

> **Where should these permissions live?**

Should we put:

```json
{
  "role": "ADMIN",
  "tenant": "acme",
  "permissions": [...]
}
```

inside the JWT?

Or should the API query a database?

Or should there be a separate authorization service?

Or should we use short-lived tokens?

Or should we cache permissions?

And suddenly we are back to the same tension we've encountered repeatedly:

```text
JWT/self-contained
      ↕
centralized/current state
```

The next chapter can tie this together by going deeply into **claims, scopes, roles, JWT payload design, and the dangerous temptation to put "everything" into a JWT**—including concrete examples of what belongs there, what should stay in your database, and why.


---

# Part 11 — What should actually go inside a JWT?

This is where JWT starts becoming practical.

We've established that a JWT can carry claims:

```json
{
  "sub": "alice",
  "scope": "orders:read",
  "tenant": "acme",
  "role": "USER",
  "exp": 1787906400
}
```

And we've seen that an API can use those claims as inputs to authorization.

So a developer naturally asks:

> **"Why not just put all the user's permissions into the JWT?"**

It sounds attractive.

But this is where many real systems go wrong.

---

# Start with the dream

Imagine Alice logs in.

The Authorization Server creates:

```json
{
  "sub": "alice",
  "role": "ADMIN",
  "permissions": [
    "user:read",
    "user:create",
    "user:update",
    "user:delete",
    "invoice:read",
    "invoice:approve",
    "report:read"
  ],
  "tenant": "acme"
}
```

Now every API can make decisions locally.

```text
JWT
 ↓
Orders API
 ↓
check permissions

JWT
 ↓
Invoice API
 ↓
check permissions

JWT
 ↓
Reports API
 ↓
check permissions
```

No database.

No authorization service.

Very fast.

It feels like the perfect architecture.

Until Alice changes.

---

# Problem #1 — Information becomes stale

Suppose the token says:

```text
role = ADMIN
```

and Alice leaves the company.

An administrator changes:

```text
Alice
  ADMIN → DISABLED
```

The database now says:

```text
Alice = disabled
```

But Alice's JWT still says:

```text
role = ADMIN
```

The JWT hasn't been modified.

The signature is still valid.

So the API sees:

```text
signature ✅
exp ✅
role=ADMIN ✅
```

and potentially allows access.

This illustrates one of the most important properties of JWT:

> **A valid signature tells you the token hasn't been tampered with. It does not tell you that the claims represent the current state of the world.**

---

# This creates a fundamental tradeoff

You can make the JWT:

```text id="j7ra0d"
long-lived
```

and reduce token refresh frequency.

But then:

```text id="w7e4p3"
permissions become stale for longer
```

Or:

```text id="mytqx7"
short-lived
```

and reduce stale-claim lifetime.

But now:

```text id="smf3bg"
tokens need refreshing more frequently
```

So:

```text id="juifj4"
JWT lifetime
    ↕
authorization freshness
```

This isn't a JWT bug.

It's a consequence of putting changing state into a credential.

---

# Problem #2 — Token size

Imagine Alice belongs to:

```text id="r8irjj"
500 projects
```

and has permissions for each.

You might be tempted to put:

```json
{
  "projects": {
    "1": ["read", "write"],
    "2": ["read"],
    "3": ["read", "write"],
    "...": "..."
  }
}
```

into the JWT.

Now the token becomes huge.

And this token might be sent on:

```text id="4j9t09"
every API request
```

Imagine:

```text id="n2b0z5"
100 requests
×
20 KB JWT
```

That's unnecessary traffic.

More importantly, the token becomes difficult to manage and reason about.

So a useful principle emerges:

> **JWT claims should generally represent relatively stable, useful authorization/context information—not an entire database snapshot.**

---

# Problem #3 — Changing permissions means issuing new tokens

Suppose:

```text id="l1x3cn"
Alice:
    USER
```

Token:

```json
{
  "sub": "alice",
  "role": "USER"
}
```

Admin promotes Alice:

```text id="w7v9t3"
USER → ADMIN
```

What happens?

The old token still says:

```text id="ln5x5h"
USER
```

So Alice won't necessarily see the new permissions until she gets another token.

Conversely:

```text id="q3s0y4"
ADMIN → USER
```

creates the dangerous case:

```text old token
   ↓
still says ADMIN
```

So you need a strategy for authorization changes.

Common choices include:

```text
short access-token lifetime
+
refresh
```

or:

```text
token claims for coarse authorization
+
current server-side checks for sensitive operations
```

or:

```text
centralized introspection/state
```

depending on the architecture.

---

# So what belongs in a JWT?

A useful rule is:

> Put information in a JWT when the receiver needs it frequently, trusts the issuer for it, and can tolerate the information being valid only for the token's lifetime.

Let's break that down.

Good candidates often include:

```text id="v1avng"
subject identifier
issuer
audience
expiration
issued-at time
scope
stable tenant/context identifiers
sometimes roles or coarse permissions
```

Potentially useful:

```text id="3oq3ef"
authentication-related context
client/application identifier
authorization context
```

But whether a specific claim belongs there depends heavily on the system.

---

# What usually should NOT be in the JWT?

Consider:

```text id="3bh0mw"
password
```

Obviously no.

But less obviously, don't treat a JWT as a place to put:

```text id="vwx0nd"
entire user profile
large collections
rapidly changing permissions
secrets
sensitive data that the client does not need to see
```

Why?

Because a normal signed JWT payload is **readable by whoever possesses the token**.

Remember:

```text id="q5m4yb"
Base64URL encoding
      ≠
encryption
```

---

# Let's prove that

Suppose the JWT payload contains:

```json
{
  "sub": "alice",
  "email": "alice@example.com"
}
```

Someone who has the JWT can decode that payload.

They don't need the signing key to read it.

The signing key is needed to create a valid signature—not to decode the payload.

So:

```text id="psw3cu"
JWT payload
   ↓
readable

JWT signature
   ↓
protects integrity/authenticity
```

Therefore:

> **Never put confidential information in a JWT just because "it's signed."**

Signed ≠ encrypted.

---

# "What about the user's email?"

This is more subtle.

Suppose:

```json
{
  "sub": "12345",
  "email": "alice@example.com"
}
```

Is that forbidden?

No.

It depends on the architecture.

The question is:

> **Does the receiver need it, and is it acceptable for the token holder to see it?**

If yes, it may be reasonable.

But frequently:

```text
sub = 12345
```

is enough to identify Alice.

Then the application can retrieve the current profile from its own data store when necessary.

This gives us a useful design distinction:

```text
JWT:
    compact authorization/context

Database:
    current application state
```

---

# A good practical pattern

Suppose your token contains:

```json
{
  "sub": "12345",
  "tenant": "acme",
  "scope": "orders:read"
}
```

Then:

```http
GET /orders/987
Authorization: Bearer <JWT>
```

Your Orders API can use the token to determine:

```text
caller = 12345
tenant = acme
allowed action = orders:read
```

Then it queries:

```sql
SELECT *
FROM orders
WHERE id = 987
  AND tenant_id = 'acme';
```

The database provides:

```text
current resource state
```

The token provides:

```text
caller identity + delegated authority
```

This is a very natural split.

---

# The important distinction: Identity vs application data

Imagine a JWT contains:

```json
{
  "sub": "12345",
  "name": "Alice",
  "department": "Finance"
}
```

Six months later, Alice changes department:

```text
Finance → Engineering
```

Your user database updates immediately.

But the JWT might still say:

```text
department = Finance
```

Therefore:

```text
JWT claim
    =
claim made at token issuance time

Database value
    =
current application state
```

Don't confuse the two.

---

# Now let's talk about scopes vs roles inside the token

Suppose token contains:

```text
scope = orders:read
role = admin
```

What is the difference?

## Scope

Usually represents:

> **Authority granted to the client/token within the protected API/resource context.**

For example:

```text
orders:read
orders:write
```

Think:

```text
"What API capabilities was this token granted?"
```

## Role

Usually represents:

> **A role assigned to the subject within some application/domain.**

For example:

```text
ADMIN
MANAGER
SUPPORT
```

Think:

```text
"What kind of principal is this?"
```

These can overlap in implementation, but conceptually they're different.

---

# Here's an important OAuth example

Suppose Alice uses:

```text
AnalyticsApp
```

The app itself is only supposed to read reports.

Alice happens to be:

```text
ADMIN
```

A token could theoretically carry:

```text
role = ADMIN
scope = reports:read
```

Now what should the Analytics API do?

It should not say:

```text
role == ADMIN
→ allow everything
```

It should enforce the authority appropriate to the API and token.

For example:

```text
GET /reports
requires reports:read
```

The token has:

```text
reports:read ✅
```

But:

```text
DELETE /users
```

shouldn't suddenly become allowed just because the token also says:

```text
role=ADMIN
```

unless the API's authorization model explicitly makes that role relevant.

This illustrates:

> **Claims are inputs. The API's authorization policy gives them meaning.**

---

# Another critical issue: audience

Suppose we issue one giant JWT:

```json
{
  "sub": "alice",
  "role": "ADMIN",
  "scope": "everything",
  "aud": ["orders", "payments", "reports"]
}
```

Now every service may be able to process it.

That's convenient.

But it increases the blast radius if the token leaks.

A more restrictive design could issue a token intended specifically for:

```text id="qfnsx9"
orders-api
```

with only:

```text id="vp2h43"
orders:read
```

Then:

```text
aud = orders-api
```

The Payments API rejects it.

This is why **audience restriction** is important.

A token should generally be usable only where it is intended to be used.

---

# Now consider a microservices architecture

Suppose:

```text id="j2n9fy"
             API Gateway
                  |
       +----------+----------+
       |          |          |
       v          v          v
    Orders     Payments    Reports
```

You have two broad options.

## Option A — One broad token

```text id="g1v5d7"
JWT:
    aud = all APIs
    lots of permissions
```

Simple.

But compromise of the token can affect many services.

## Option B — Narrow tokens

```text id="h07fqt"
Orders token:
    aud=orders
    scope=orders:read

Payments token:
    aud=payments
    scope=payments:read
```

More complex.

But compromise of one token has a smaller blast radius.

This is a recurring security tradeoff:

```text id="qzq3sp"
convenience
    ↕
blast-radius reduction
```

Modern OAuth security guidance recommends restricting access tokens to intended resource servers and minimizing their privilege for precisely this reason.

---

# Now let's revisit revocation

Suppose you have:

```text id="z1n0w4"
JWT access token
expires in 15 minutes
```

Alice's account is disabled.

The API can continue accepting the token until:

```text id="gc1clq"
expiration
```

unless there is an additional revocation mechanism.

How can we handle high-security cases?

Several architectures exist.

### Short-lived access tokens

Reduce the maximum stale period.

```text id="2vq1u0"
15 min
```

instead of:

```text id="j7bvr0"
24 hours
```

### Introspection

API asks the Authorization Server:

```text id="45jb2b"
Is this token still active?
```

### Authorization-state lookup

API checks current user/account state.

### Centralized session/revocation state

Keep state that allows immediate blocking.

### Sender-constrained tokens

Reduce the usefulness of a stolen token.

None is universally correct.

---

# Why access tokens are usually short-lived

Now the reason should be much more intuitive.

Suppose a JWT says:

```json
{
  "sub": "alice",
  "scope": "payments:write",
  "exp": 1787906400
}
```

If someone steals it, you want:

```text id="u9rpx9"
small lifetime
+
narrow scope
+
narrow audience
```

rather than:

```text id="xbgrap"
long lifetime
+
all permissions
+
all APIs
```

This gives you **blast-radius reduction**.

That phrase is useful in system-design/security interviews.

---

# The principle underneath all of this

Think in terms of:

```text id="agyr9c"
Credential power
      =
scope
×
audience
×
lifetime
×
replayability
```

This isn't a mathematical security formula, but it's an excellent design intuition.

A credential becomes dangerous when it has:

```text
broad scope
+
many destinations
+
long lifetime
+
easy replay
```

A safer credential generally has:

```text
narrow scope
+
limited audience
+
short lifetime
+
stronger possession requirements
```

This principle explains many OAuth recommendations.

---

# Now let's connect this to refresh tokens

Suppose:

```text id="j0d8q3"
Access Token:
    10 min
    aud=orders-api
    scope=orders:read
```

and:

```text id="h3z7ra"
Refresh Token:
    longer-lived
    tightly controlled
```

The access token can be safely-ish exposed to the Orders API because:

```text
short lifetime
+
narrow audience
+
narrow scope
```

while the refresh token stays away from the Resource Server.

This is the architecture we spent previous chapters building toward.

---

# But we still have a tricky question

Suppose the access token is a JWT.

Who decides what's inside it?

There are at least three possibilities:

```text id="1u1cso"
Authorization Server
      ↓
issues claims


Application
      ↓
decides how claims map to permissions


Resource Server
      ↓
decides whether current request is allowed
```

Each layer has a different responsibility.

This is the clean mental model:

```text id="2r9g6n"
Authorization Server
    "Here is the authority I am asserting."

Resource Server
    "I trust this issuer and validate the token."

Application
    "Given this request, resource, and policy,
     should I allow it?"
```

---

# A concrete Spring Boot example

Imagine the JWT contains:

```json
{
  "sub": "12345",
  "scope": "orders:read"
}
```

Your controller:

```java
@GetMapping("/orders")
public List<Order> getOrders(Authentication authentication) {
    ...
}
```

Spring Security may already have validated:

```text id="jkh17b"
signature
issuer
expiration
audience
etc.
```

But your application still needs:

```text id="jyaqnc"
Can subject 12345 read these orders?
```

And perhaps:

```java
@PreAuthorize("hasAuthority('SCOPE_orders:read')")
```

handles the scope requirement.

Then your service/database logic handles:

```text id="jvi7mt"
tenant isolation
ownership
resource access
business rules
```

This illustrates our three-layer authorization model:

```text id="h0kj5g"
Token validation
      ↓
OAuth scope/authority
      ↓
business/resource authorization
```

---

# Now the critical question: "Should I put roles in JWT?"

The answer is:

**Sometimes.**

It's reasonable when:

```text
role changes relatively infrequently
+
stale role information is acceptable for token lifetime
+
the receiver needs the role frequently
```

It's less attractive when:

```text
permissions change constantly
+
revocation must be immediate
+
authorization depends heavily on live resource state
```

In those cases, consider:

```text
short-lived tokens
+
current authorization lookup
```

or an authorization service/introspection model.

---

# The mental model I want you to retain

Don't think:

```text id="e2n2vz"
"JWT = user information"
```

Think:

```text id="9xk20g"
JWT
  =
signed claims issued by a trusted authority
```

Then ask:

```text id="nv5f0d"
Which claims?

Why do I need them?

How long can they be trusted?

Who is the intended audience?

What happens when the claim changes?

What happens when the token is stolen?

What information must remain server-side?
```

That way you avoid the "put everything into JWT" trap.

---

# Our story so far

We started with:

```text id="n2clpl"
Password
   ↓
Authentication
```

Then:

```text id="lrx0tq"
Session
   ↓
remember authentication
```

Then:

```text id="c7d9kn"
Cookie + HTTPS + CSRF defenses
```

Then:

```text id="3y3b5j"
Distributed applications
   ↓
self-contained signed tokens
   ↓
JWT
```

Then:

```text id="mkv2q8"
Third-party access
   ↓
OAuth
```

Then:

```text id="txiq4a"
Authorization Code
   ↓
PKCE
```

Then:

```text id="w6hq3u"
Access Token
   ↓
Refresh Token
   ↓
Rotation
```

Then:

```text id="n6rlai"
OAuth ≠ login
   ↓
OIDC
   ↓
ID Token
```

Then:

```text id="w5mj6x"
Authorization
   ↓
scopes
roles
permissions
resource policies
```

And now:

```text id="byqyq7"
JWT claims
   ↓
what belongs in the token?
   ↓
what must remain live server state?
```

---

# The next missing piece

There is one topic we haven't deeply explored yet, and it ties **everything** together:

## What exactly happens when a JWT arrives at your API?

Not just:

```text
"verify the signature."
```

I mean the complete security pipeline:

```text
HTTP request
    ↓
Bearer token extraction
    ↓
JWT parsing
    ↓
algorithm validation
    ↓
key discovery
    ↓
signature verification
    ↓
issuer validation
    ↓
audience validation
    ↓
time validation
    ↓
scope/claim extraction
    ↓
Spring Security Authentication object
    ↓
authorization checks
    ↓
controller
    ↓
resource-level authorization
```

We'll go through an actual JWT byte-by-byte conceptually and then map every step to **Spring Security**, including what `SecurityFilterChain`, `JwtDecoder`, `Authentication`, `GrantedAuthority`, `hasRole`, and `hasAuthority` are really doing underneath.

That will make the whole authentication pipeline feel much less magical.


---
