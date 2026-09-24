Teach me everything about REST practices. 
Why we need it? How to use it properly? How to not?  

What are the production best practices. 
Teach me everything about from scratch. Basic to advanced. 
Teach me chapter by chapter if required.

---

Good — REST is a big topic, so let's do this properly: sequential chapters, one concept at a time, with a checkpoint before moving on (like we've done with distributed systems and DSA). Here's the roadmap, then we start Chapter 1.

## The Roadmap

**Part 1 — Foundations**
1. What problem REST actually solves (the pre-REST world, and why Fielding wrote his dissertation)
2. The 6 constraints of REST (and what "RESTful" really means vs. what people call REST)
3. Resources, URIs, and the resource-oriented mindset

**Part 2 — Using HTTP Properly**
4. HTTP methods and idempotency (GET/POST/PUT/PATCH/DELETE — the subtle traps)
5. Status codes done right (and the common misuses)
6. Headers, content negotiation, and media types

**Part 3 — Designing Good APIs**
7. URI design conventions (nesting, filtering, pagination, sorting)
8. Statelessness and why it matters for scaling
9. HATEOAS — the most misunderstood constraint (and why almost nobody does it)
10. Versioning strategies (you've touched this before — we'll connect it here)

**Part 4 — Production Concerns**
11. Error handling design (problem+json, consistent error shapes)
12. Rate limiting, throttling, and idempotency keys
13. Caching (ETags, Cache-Control, conditional requests)
14. Security practices (auth patterns, CORS, input validation)
15. Performance patterns (N+1 avoidance, partial responses, batching, compression)

**Part 5 — Anti-Patterns & Alternatives**
16. Common REST anti-patterns seen in real codebases
17. When REST is the wrong tool (GraphQL, gRPC, event-driven — honest tradeoffs)

We'll go one chapter at a time, with a concrete example running throughout (I'll build a small **"Library/Book Lending" API** as our running case study so every concept has a real anchor). I'll pause after each chapter for you to confirm before we move on.

---

## Chapter 1: What Problem Does REST Actually Solve?

### The world before REST

In the late 1990s, distributed systems mostly talked to each other through **RPC-style protocols** — things like CORBA, DCOM, and SOAP-RPC. The mental model was: "call a remote function as if it were local."

```
client.getUserBalance(userId=42)
client.transferMoney(from=42, to=17, amount=500)
client.deleteUserAccount(userId=42)
```

This felt natural to programmers, but it created real problems at scale:

1. **Tight coupling** — the client had to know the exact method names, exact parameter order/types, and exact semantics of every operation. Every new "verb" required client and server to agree on a new function signature.
2. **No uniform interface** — every service invented its own set of operations. There was no shared vocabulary a generic client (like a browser, a cache, or a proxy) could understand.
3. **Poor cacheability** — since operations were arbitrary function calls, intermediaries (caches, proxies, CDNs) had no way to know whether a call was safe to cache or repeat.
4. **Hard to evolve** — adding a new operation meant both sides shipping new code. There was no way to discover new capabilities at runtime.
5. **Statefulness leaked in** — many RPC systems kept session state on the server between calls, which made horizontal scaling (spinning up more servers) painful — a client's next call had to hit the *same* server that remembered its session.

### Enter Roy Fielding (2000)

Fielding was one of the authors of the HTTP/1.1 specification. He noticed that the **web itself** — browsers, servers, caches, proxies — had already solved distributed-system problems at a scale nobody's RPC framework had touched. Billions of documents, thousands of independent server implementations, caches everywhere, and it *just worked*.

His PhD dissertation asked: **what architectural properties made the web work so well, and can we describe them as a general style for building distributed systems?** That style is REST — **RE**presentational **S**tate **T**ransfer.

The core insight: **stop thinking in verbs (functions), start thinking in nouns (resources) with a small, fixed set of operations.**

```
RPC mindset:              REST mindset:
getUserBalance(42)    →   GET /users/42/balance
transferMoney(...)    →   POST /transfers
deleteUserAccount(42) →   DELETE /users/42
```

Instead of an infinite vocabulary of custom methods, you get a **small, uniform set of verbs** (GET, POST, PUT, PATCH, DELETE) applied to an **unlimited set of resources** (nouns). This one shift is what unlocks everything else — caching, statelessness, generic clients, intermediaries that don't need to understand your business logic to still be useful.

### Our running example: Library API

Throughout this series we'll design a **Book Lending API** for a library system. Rough domain:
- Books (catalog items)
- Members (people who borrow)
- Loans (a member borrowing a book)
- Reservations (holds on a book that's checked out)

In the RPC world, you might have imagined:
```
checkOutBook(bookId, memberId)
returnBook(loanId)
reserveBook(bookId, memberId)
```

Over the next chapters we'll turn this into resource-oriented thinking:
```
POST /loans          (create a loan = "check out")
PATCH /loans/{id}     (mark returned)
POST /reservations
```

### Checkpoint

Before we move to Chapter 2 (the 6 formal constraints of REST — this is where "RESTful" gets its real technical meaning, and where most APIs called "REST" actually fail the test), quick check: does the RPC-vs-resource distinction feel solid? Want me to give you a broken/good pair exercise on this before advancing, or move straight to Chapter 2?

---

## Chapter 2: The 6 Constraints of REST (What "RESTful" Actually Means)

Most APIs people call "REST" are really just **"HTTP APIs that return JSON."** True REST, per Fielding, means satisfying six architectural constraints. Let's go through each — with what it means, why it matters, and how it applies to our Library API.

### 1. Client-Server

Separation of concerns: the client handles UI/UX, the server handles data storage and business logic. They evolve independently as long as the interface between them stays stable.

**Library API:** A mobile app, a web portal, and a librarian's desktop tool can all be totally different clients hitting the same `/books`, `/loans` endpoints. None of them need to know how the server stores data (Postgres? DynamoDB? doesn't matter to them).

### 2. Statelessness

**Every request must contain all the information needed to process it.** The server stores no client session context between requests.

```
BAD (stateful):
1. Client: POST /login → server stores "session 123 is logged in as member 42" in memory
2. Client: GET /my-loans → server looks up session 123 in its memory to know who's asking

GOOD (stateless):
1. Client: POST /login → server returns a token
2. Client: GET /my-loans  Authorization: Bearer <token> → server decodes token on every request, no memory needed
```

Why this matters enormously in production: if state lives in server memory, that client is **pinned** to that specific server instance. You can't freely load-balance across a fleet, you can't kill and replace an unhealthy instance without breaking active users, and autoscaling becomes painful. Statelessness is *the* constraint that makes horizontal scaling trivial.

### 3. Cacheability

Responses must declare, implicitly or explicitly, whether they're cacheable. This lets clients, proxies, and CDNs reuse responses instead of hitting your server every time.

```http
GET /books/9781/reviews
Cache-Control: public, max-age=3600
```

A book's reviews don't change every second — telling intermediaries "cache this for an hour" can eliminate massive amounts of redundant server load. We'll go deep on this in the Caching chapter (13).

### 4. Uniform Interface

This is the constraint that most defines "REST-ness," and it has four sub-parts:

- **Identification of resources** — every resource has a unique URI (`/books/9781`, not a function call).
- **Manipulation through representations** — the client gets a *representation* (JSON, XML) of the resource and can modify it by sending back a modified representation, not by calling a custom method.
- **Self-descriptive messages** — each message contains enough info to be understood (Content-Type, status codes) without out-of-band knowledge.
- **HATEOAS** (Hypermedia As The Engine Of Application State) — responses include links to related actions/resources, so clients discover what they can do next rather than hardcoding it. (Almost nobody actually does this — Chapter 9 is dedicated to it, including *why* it's skipped in practice.)

### 5. Layered System

The client shouldn't need to know whether it's talking directly to the origin server or through a chain of intermediaries (load balancer, CDN, API gateway, auth proxy). Each layer only knows about the layer immediately adjacent to it.

**Library API:** Your client hits `api.library.com/books/9781`. In reality that might pass through a CDN → WAF → load balancer → API gateway → app server. The client neither knows nor cares.

### 6. Code on Demand (optional)

The server can optionally send executable code (like JavaScript) that the client runs to extend functionality. This is the only *optional* constraint — most APIs skip it entirely, and that's fine. (Web pages sending JS to the browser is the classic example — this is really a browser-era constraint, rarely relevant to JSON APIs.)

### The uncomfortable truth

If an API violates statelessness (e.g., server-side session cookies for API auth) or skips HATEOAS entirely (which ~95% of "REST APIs" do), it's technically **not fully RESTful** — it's often more accurately described as an **HTTP API** or "REST-ish." That's not a criticism — it's usually a deliberate, pragmatic trade-off (HATEOAS in particular has real costs we'll cover in Ch. 9). But it's worth knowing the vocabulary so you understand what you're actually building and why.

### Quick self-check exercise

Look at this endpoint from a real-ish API and tell me which constraints it violates:

```http
POST /getBookDetails
Content-Type: application/json
{ "sessionId": "abc123", "bookId": 9781 }

Server keeps sessionId → user mapping in an in-memory HashMap.
```

Take a guess at what's wrong here before I confirm — it'll cement the constraint better than me just telling you.

---

Quick answer since it's fast: that endpoint violates **Uniform Interface** (using a verb-style URI `/getBookDetails` instead of a resource noun like `/books/9781`, and it's a GET-semantic operation forced through POST) and **Statelessness** (server keeps `sessionId → user` in memory instead of the client sending self-contained auth on every request). Good pattern to recognize — we'll see it again.

Moving on.

## Chapter 3: Resources, URIs, and the Resource-Oriented Mindset

### Everything is a noun

The first design decision in any REST API: **identify your resources before you think about endpoints.** A resource is any "thing" that can be addressed and manipulated — an object, a collection of objects, or even a computed/derived concept.

For the Library API:

| Resource | Represents |
|---|---|
| `Book` | a catalog entry |
| `Member` | a library user |
| `Loan` | a book currently borrowed by a member |
| `Reservation` | a hold on a book |
| `Author` | (maybe — depends how deep your model goes) |

### Collections vs. single resources

Every resource type gets **two URI shapes**:

```
/books           → collection (GET = list, POST = create)
/books/{id}       → single resource (GET/PUT/PATCH/DELETE)
```

This is one of the most important conventions in REST — it's predictable enough that once a client understands `/books`, they can *guess* how `/members` and `/loans` behave without reading fresh docs each time. That predictability is the whole point of a "uniform interface."

### Nesting resources: when and how deep

Nesting expresses **ownership or containment**, not just relatedness:

```
GET /members/42/loans        → loans belonging to member 42   ✅ good — clear ownership
GET /books/9781/reservations  → reservations on book 9781      ✅ good — clear ownership
```

But nesting has a trap: **it's tempting to nest infinitely**, and that becomes unusable:

```
BAD:
GET /members/42/loans/17/book/9781/author/55/books

GOOD:
GET /loans/17          (returns bookId, memberId as fields/links)
GET /authors/55/books   (a fresh top-level lookup)
```

**Rule of thumb: nest one level deep, maximum two.** Beyond that, treat the resource as top-level and let the client compose lookups, or use query parameters instead of path nesting (see below).

### Nouns, not verbs, in the path

This is the single most common REST anti-pattern in real codebases:

```
BAD (verb-based, RPC leaking through):
POST /createBook
POST /books/9781/markAsReturned
GET  /getBooksByAuthor?authorId=55

GOOD (noun-based, verb comes from HTTP method):
POST /books
PATCH /loans/17          { "status": "returned" }
GET  /books?authorId=55
```

The HTTP method *is* the verb. If your URI contains a verb, you're usually modeling an RPC call and wearing REST's clothing.

### Actions that don't map to CRUD

Sometimes a real-world action isn't naturally "create/read/update/delete" — e.g., "check out a book," "renew a loan," "cancel a reservation." Two accepted patterns:

**Pattern A — model the action as a sub-resource (preferred):**
```
POST /loans/17/renewals        → creates a "renewal" record, side effect: extends due date
POST /reservations/9/cancellation
```

**Pattern B — treat it as a state transition via PATCH:**
```
PATCH /loans/17   { "status": "renewed" }
```

Both are legitimate; Pattern A is more RESTful (renewal becomes an auditable resource with its own history), Pattern B is simpler when you don't need to track the action itself. Avoid Pattern C:

```
BAD:
POST /loans/17/renew
```
— this is a verb bolted onto a noun path. It works, plenty of real APIs do it (pragmatism wins sometimes), but recognize it as a compromise, not the ideal.

### Naming conventions

- Plural nouns for collections: `/books` not `/book`
- Lowercase, hyphenated for multi-word resources: `/loan-history` not `/loanHistory` or `/LoanHistory`
- Consistent casing in JSON bodies — pick camelCase or snake_case and never mix

### Checkpoint

Try this: how would you design the URI (and method) for **"a member wants to see all books currently reserved by them, and cancel one specific reservation"**? Give it a shot, then we'll move to Chapter 4 (HTTP methods and idempotency — where a lot of subtle production bugs live).

---

/members/{memberId}/reservations/{reservationId}/cancel
or
/reservations/{reservationId}/cancel (if we can get member id from header, jwt token or something)

---

Good instinct on both, and the JWT point is sharp — that's exactly the right question to ask. Let me refine both:

**On the two options:** you don't actually need `memberId` in the path at all for security — the JWT/token tells the server who's asking, and the server should **verify** that reservation 9 actually belongs to that member before cancelling (never trust a client-supplied ownership claim, only use it for display/UX). So:

```
GET /reservations/{reservationId}     → server checks: does this belong to caller's member ID? If not, 403.
```

Path nesting like `/members/{memberId}/reservations` is still useful for the **list** endpoint (member wants to see *all* their reservations), but for a specific-item operation, the flatter `/reservations/{id}` is preferred — you don't need the parent in the path once you have a unique child ID.

**On `/cancel` itself:** this is the verb-in-path anti-pattern from Chapter 3. The more RESTful version:

```
DELETE /reservations/{reservationId}
```

Cancelling a reservation *is* deleting it — DELETE already means "this resource should cease to exist," no extra verb needed. If you need to keep a record of the cancellation (audit trail, "why was it cancelled"), that's when Pattern A comes back:

```
POST /reservations/{reservationId}/cancellation   { "reason": "..." }
```

So the two clean options, both without `memberId` in the path:

```
GET    /members/me/reservations          → list (using "me" or JWT-derived identity, not raw ID)
DELETE /reservations/{reservationId}      → cancel one (ownership checked server-side)
```

Nice catch on the auth question — that's exactly the kind of thing that trips people up in production. Ready for Chapter 4 (HTTP methods and idempotency)?

---

when to use plurals (book vs books)
also
POST /reservations/{reservationId}/cancellation vs POST /reservations/{reservationId}/cancel

---

Good questions — both are about being precise with conventions rather than just "vibing" them.

### Plural vs. singular

**Rule: always plural for the resource name, regardless of whether you're addressing the collection or a single item.**

```
GET /books           → collection (many books)
GET /books/9781       → still under the plural "books" resource type, just one instance

NOT:
GET /book              (collection — wrong, sounds like a single thing)
GET /book/9781         (single item — technically "reads fine" but breaks consistency)
```

The reasoning: the **resource type never changes** just because you're addressing one instance of it vs. many. `/books/9781` reads as "the books collection, entry 9781" — consistent and predictable. If you mix singular and plural depending on cardinality, clients can't guess your convention (`/book/9781` but `/reservations` — now they have to memorize which is which per resource, defeating the whole point of a uniform interface).

**The one exception:** singleton resources — things where there's conceptually only ever *one* instance, no collection at all.

```
GET /users/42/profile        → singular "profile" is correct, a user has exactly one
GET /cart                    → singular, if a session/user has exactly one active cart
```

Here plural would be misleading (`/profiles` implies a user has many, browsable profiles — they don't).

For your Library API: `books`, `members`, `loans`, `reservations` — all plural, all standard collections. No singletons in this domain that I can see yet.

### `/cancellation` vs `/cancel`

This is really the **noun vs. verb** distinction from Chapter 3, applied narrowly:

```
POST /reservations/{id}/cancellation    ✅ noun — you're creating a "cancellation" resource/event
POST /reservations/{id}/cancel          ⚠️ verb — you're calling a "cancel" action
```

Both will *work* identically in practice — this is a style/philosophy distinction, not a functional one. But they imply different mental models:

- **`/cancellation` (noun form)** says: "cancellation is a **thing** — a record with its own identity." This pays off if you ever want to:
  - `GET /reservations/9/cancellation` → see when/why/who cancelled it
  - Track multiple cancellation *attempts* as separate audit entries
  - Treat it like any other resource (consistent CRUD semantics)

- **`/cancel` (verb form)** says: "this is just an RPC-style action, fire and forget." Simpler to write, but it's an acknowledged escape hatch outside REST's uniform interface — you're back to inventing custom verbs per resource, which is the exact problem REST was designed to avoid (Chapter 1).

**Practical guidance:** if the action produces something worth remembering (audit trail, timestamp, reason, who did it) → noun form. If it's a pure, fire-and-forget state flip with nothing to record → just use `DELETE` on the resource directly instead of either:

```
DELETE /reservations/{id}     → simplest, most RESTful, no action-naming debate needed at all
```

Most production teams actually reach for `DELETE` here and skip the whole cancel/cancellation question — it's usually the cleanest answer once you realize "cancel a reservation" and "delete a reservation" are the same real-world event.

Ready for Chapter 4 — HTTP methods and idempotency? This is where GET/POST/PUT/PATCH/DELETE get precise, and where a surprising number of production bugs come from teams getting idempotency wrong.

---

## Chapter 4: HTTP Methods and Idempotency

This chapter matters more than it looks — idempotency bugs cause real production incidents (double charges, duplicate orders, broken retries).

### The five methods you actually need

| Method | Purpose | Safe? | Idempotent? |
|---|---|---|---|
| GET | Read a resource | ✅ | ✅ |
| POST | Create a resource / non-idempotent action | ❌ | ❌ |
| PUT | Replace a resource entirely | ❌ | ✅ |
| PATCH | Partially update a resource | ❌ | ❌ (usually) |
| DELETE | Remove a resource | ❌ | ✅ |

Two properties you need to actually understand, not just memorize:

**Safe** = the request doesn't change server state at all. Only GET (and technically HEAD, OPTIONS) are safe. This matters because browsers, crawlers, and proxies assume GET is free to call repeatedly, prefetch, or cache — if your GET endpoint has side effects (e.g., `GET /books/9781/increment-view-count` that also deletes something), you've broken a contract every HTTP-aware tool on the internet relies on.

**Idempotent** = calling it once has the same effect as calling it N times. This is about **retry safety**, which is the real-world reason this matters.

### Why idempotency is a production concern, not academic trivia

Networks fail. A client sends a request, the response gets lost on the way back (timeout, connection drop), and the client doesn't know: did the server actually process it, or not? The client's only safe move is to **retry**.

```
Client: POST /loans { bookId: 9781, memberId: 42 }
... network times out, no response ...
Client: retries → POST /loans { bookId: 9781, memberId: 42 } again
```

If `/loans` creation isn't handled carefully, that retry creates **two loan records** for one checkout — a real bug, not hypothetical. This is why POST is *not* idempotent by default: two identical POSTs are allowed to create two distinct resources.

Compare with PUT:
```
PUT /books/9781  { "title": "Dune", "copies": 5 }
```
Calling this once or five times in a row leaves the book in the exact same state. That's what "idempotent" buys you: **safe to retry blindly, no ambiguity.**

### GET — read-only, cacheable, safe

```http
GET /books/9781
GET /books?author=Herbert&available=true
```
Never put side effects here. If you find yourself wanting `GET /books/9781/delete` — stop, that's Chapter 3's anti-pattern again, wearing a different hat.

### POST — create, or non-idempotent actions

```http
POST /loans
{ "bookId": 9781, "memberId": 42 }

→ 201 Created
Location: /loans/17
{ "id": 17, "bookId": 9781, "memberId": 42, "status": "active", "dueDate": "2026-09-29" }
```

Two things production APIs get right here that beginners often miss:
1. **Return `201 Created`**, not `200 OK`, when something new was made.
2. **Return a `Location` header** pointing to the new resource's URI — this is part of self-descriptive messages (Uniform Interface, Ch. 2).

**Solving the retry problem for POST — idempotency keys:**
```http
POST /loans
Idempotency-Key: 7f3e9c2a-...
{ "bookId": 9781, "memberId": 42 }
```
The client generates a unique key per *logical* operation (not per HTTP attempt). Server stores `key → result` the first time it sees the key; if the same key arrives again (retry), it returns the *original* result instead of creating a second loan. This is the standard production pattern for making POST retry-safe — we'll go deeper on it in Chapter 12.

### PUT — full replacement

```http
PUT /books/9781
{ "title": "Dune", "author": "Frank Herbert", "copies": 5, "isbn": "9781441" }
```
PUT means **"this is now the complete state of the resource."** Any field you omit is implicitly cleared/reset — that's the contract. This trips people up constantly:

```
BAD usage of PUT:
PUT /books/9781
{ "copies": 4 }
→ if the server takes this literally, title/author/isbn might get wiped out!
```

If you only want to change one field, that's not PUT's job — that's PATCH.

### PATCH — partial update

```http
PATCH /loans/17
{ "status": "returned" }
```
Only the specified fields change; everything else stays as-is. PATCH is technically **not guaranteed idempotent** in the general case (e.g., `{"op": "increment", "field": "copies"}` — applying twice ≠ applying once), but in the common case of "set field X to value Y," it behaves idempotently in practice. Be precise about which kind you're building.

### DELETE — remove

```http
DELETE /loans/17
→ 204 No Content
```
Idempotent by convention: deleting an already-deleted resource should not be treated as an error requiring special handling by the client — typically return `204` again (or `404`, both are defensible — we'll settle this precisely in Chapter 5 on status codes).

### Common production mistake: PUT used as PATCH

This is extremely common and worth calling out explicitly:

```
Client wants to renew a loan (extend due date only):
BAD:  PUT /loans/17  { "dueDate": "2026-10-15" }   → wipes out bookId, memberId, status if server is strict
GOOD: PATCH /loans/17  { "dueDate": "2026-10-15" }
```

### Exercise

The library wants an endpoint where a librarian marks a book as "lost" (removes it from circulation but keeps the record for reporting). Which method would you use, and why — PATCH, PUT, or DELETE? Take a guess before I confirm.

---

Quick answer since you moved on: **PATCH** is right here — `PATCH /books/9781 { "status": "lost" }`. Not DELETE, because the record needs to persist for reporting (DELETE implies removal from existence, not a state change). Not PUT, because you're changing one field, not replacing the whole resource. This is the same shape as the loan example — status transitions are almost always PATCH.

## Chapter 5: Status Codes Done Right

Status codes are part of REST's "self-descriptive messages" constraint (Ch. 2) — a well-designed API should be mostly understandable just from method + status code + body, without reading docs. Most production APIs misuse a shockingly small set of codes. Let's fix that.

### The five classes

| Range | Meaning |
|---|---|
| 2xx | Success |
| 3xx | Redirection |
| 4xx | Client made a mistake |
| 5xx | Server made a mistake |

The client/server distinction in 4xx vs 5xx matters a lot in production — it tells your monitoring/alerting system *who's at fault*. A spike in 4xx usually means a client bug or bad input; a spike in 5xx means **your** system is broken and should page someone.

### The 2xx codes you actually need

```
200 OK              → generic success, has a response body (GET, PATCH, PUT)
201 Created          → POST that created a new resource (include Location header)
202 Accepted         → request accepted but processing is async (Ch. 12 territory — queued jobs)
204 No Content       → success, no body to return (DELETE, sometimes PUT)
```

**Common mistake:** returning `200 OK` for every successful POST regardless of whether something was created. If `/loans` POST succeeds, that's `201`, full stop.

### The 4xx codes people get wrong

```
400 Bad Request        → malformed request (invalid JSON, missing required field, wrong type)
401 Unauthorized        → NOT "forbidden" — this means "who are you? I don't know" (missing/invalid credentials)
403 Forbidden           → "I know who you are, and you're not allowed" (valid auth, insufficient permission)
404 Not Found            → resource doesn't exist
409 Conflict             → request conflicts with current state (e.g., double-booking a reservation, version mismatch)
422 Unprocessable Entity → syntactically valid but semantically invalid (e.g., valid JSON, but "copies": -5)
429 Too Many Requests    → rate limit exceeded (Ch. 12)
```

**The single most common mistake: confusing 401 and 403.** They get swapped constantly:

```
Client sends no auth token at all:
BAD:  403 Forbidden        (implies identity is known, which it isn't)
GOOD: 401 Unauthorized      (means "I can't verify who you are")

Client is authenticated as Member 42, tries to cancel Member 17's reservation:
BAD:  401 Unauthorized      (identity IS known — this is wrong)
GOOD: 403 Forbidden          (known identity, insufficient permission)
```

Mnemonic: **401 = "authenticate yourself first"**, **403 = "I know you, but no."**

**400 vs. 422** — subtler distinction, less universally agreed on, but the useful mental model:
```
POST /books  { "title": "Dune" }           → missing required "isbn" field → 400 (structurally incomplete)
POST /books  { "title": "Dune", "isbn": "abc" }  → isbn present but fails format validation → 422 (semantically wrong)
```
Many APIs just use 400 for both and that's fine too — 422 is a nice-to-have precision, not a hard requirement. Consistency within your own API matters more than which exact convention you pick.

**404 vs. 410** — a smaller one worth knowing: `410 Gone` means "this used to exist and was intentionally, permanently removed" (vs. 404's "never existed or don't know"). Rarely used, but shows up in APIs with deprecation policies.

### The 5xx codes

```
500 Internal Server Error   → generic catch-all, something broke on your end
502 Bad Gateway              → your server is a proxy/gateway and the upstream service failed
503 Service Unavailable      → server is temporarily down (maintenance, overload) — pair with Retry-After header
504 Gateway Timeout          → upstream service took too long to respond
```

**Never leak internal details in a 500 response body** (stack traces, SQL errors, internal hostnames) — that's a security anti-pattern we'll flag again in Chapter 14.

### Redirects (3xx) — rare in APIs, but worth knowing

```
301 Moved Permanently   → resource URI changed forever (update your bookmarks/clients)
304 Not Modified         → response to a conditional GET (ETag matched) — no body, saves bandwidth (Ch. 13)
```

### DELETE idempotency, revisited (from Ch. 4's open question)

```
DELETE /loans/17   (first call)  → 204 No Content
DELETE /loans/17   (retry)       → 204 No Content  (preferred — stays truly idempotent)
                                  or 404 Not Found   (also defensible — "it's not there")
```
**Production recommendation: return 204 on the retry too.** Treating "already deleted" as an error forces every retrying client to special-case it. True idempotency means the *end state* is what matters, and after both calls the end state is identical — no reason to surface an error.

### Applying this to our Library API — a realistic response set

```
POST /loans (success)              → 201 Created
POST /loans (book already loaned)   → 409 Conflict
GET /books/999999 (doesn't exist)    → 404 Not Found
PATCH /loans/17 (not your loan)       → 403 Forbidden
GET /loans (no auth token)             → 401 Unauthorized
POST /books { }  (missing fields)       → 400 Bad Request
```

### Exercise

A member tries to reserve a book that's already available (no need to reserve — they can just borrow it directly). What status code fits best, and why — `400`, `409`, or `422`?

---

**409 Conflict** fits best — the request itself is well-formed (valid JSON, valid book ID), but it conflicts with the *current state* of the resource: you can't reserve something that doesn't need reserving. That's exactly what 409 is for — "your request is structurally fine, but the state of the world doesn't allow it right now." (422 is a defensible second choice if your team treats it as "business rule violation," but 409's definition — conflict with current resource state — is the more precise fit here.)

## Chapter 6: Headers, Content Negotiation, and Media Types

Headers are where a lot of REST's "self-descriptive messages" constraint actually lives — they let a message be understood without out-of-band docs.

### The headers you'll use constantly

```http
Content-Type: application/json          → "this is what I'm sending you"
Accept: application/json                 → "this is what I want back"
Authorization: Bearer <token>             → identity/credentials
Location: /loans/17                       → where the newly created resource lives (with 201)
```

### Content negotiation — letting clients choose the representation

REST's name literally contains "Representational" — the same underlying resource can have multiple representations, and the client should be able to ask for the one it wants:

```http
GET /books/9781
Accept: application/json          → server returns JSON

GET /books/9781
Accept: application/xml            → server returns XML (if supported)

GET /books/9781
Accept: text/csv                    → server returns CSV (if supported)
```

Most modern APIs only support JSON and that's a completely reasonable simplification — content negotiation is a *capability*, not an obligation. But if you do support multiple formats, `Accept` is how the client asks, and `Content-Type` in the response confirms what it got.

**A subtlety production teams get wrong:** if a client sends `Accept: application/xml` and you only support JSON, the correct response is `406 Not Acceptable` — not silently returning JSON anyway. Silent mismatches make debugging painful ("why does my client's XML parser choke on this?").

### Versioning in headers (preview of Chapter 10)

```http
Accept: application/vnd.library.v2+json
```
This is one legitimate way to version an API — through content negotiation rather than the URL. We'll compare this against URL-based and header-based versioning properly in Chapter 10; just flagging that `Accept` isn't only for format, it can carry version info too.

### Custom headers — the `X-` convention is dead

Older APIs used `X-` prefixed custom headers:
```
BAD (deprecated convention):
X-RateLimit-Remaining: 42
X-Request-Id: abc-123
```
The `X-` prefix was **officially deprecated in RFC 6648 (2012)** — the reasoning: once a de facto "X-" header becomes standard enough, it often gets promoted to a real spec'd header, and then you're stuck supporting both the "X-" and non-"X-" versions forever for compatibility. Modern practice: just use a clear, descriptive name without the prefix.

```
GOOD (current convention):
RateLimit-Remaining: 42
Request-Id: abc-123
```

### Idempotency-Key header (from Chapter 4, formalized)

```http
POST /loans
Idempotency-Key: 7f3e9c2a-4b1e-4a3f-9c2a-1e4b3f9c2a1e
```
Not yet a hard RFC standard as of now, but it's become a de facto industry convention (Stripe popularized it) — expect to see and use this in production payment/transaction-style endpoints.

### ETag and conditional requests (preview of Chapter 13)

```http
GET /books/9781
→ 200 OK
  ETag: "a1b2c3"

GET /books/9781
  If-None-Match: "a1b2c3"
→ 304 Not Modified   (nothing changed, don't resend the body)
```
We'll build this out fully in the Caching chapter — just want the vocabulary in your head now since it connects directly to headers.

### CORS headers — a quick production note (full treatment in Ch. 14)

If your API is called from a browser-based frontend on a different domain, you'll hit CORS (Cross-Origin Resource Sharing). The key headers:
```http
Access-Control-Allow-Origin: https://library-app.com
Access-Control-Allow-Methods: GET, POST, PATCH, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
```
We'll cover the security implications of getting `Access-Control-Allow-Origin: *` wrong in Chapter 14 — it's a common and dangerous shortcut.

### Checkpoint

That closes Part 1 & 2 (Foundations + Using HTTP Properly). Before Part 3 (Designing Good APIs — pagination, filtering, statelessness deep-dive, HATEOAS, versioning), want to pause for a recap/quiz on Chapters 1–6, or keep going straight into Chapter 7?

---

## Chapter 7: URI Design Conventions — Pagination, Filtering, Sorting

This is where API design meets real-world scale. A `/books` collection with 3 items doesn't need any of this. One with 3 million does.

### Pagination — never return an unbounded list

```http
GET /books?page=2&limit=20
```

Response should tell the client where it stands:
```json
{
  "data": [ /* 20 books */ ],
  "pagination": {
    "page": 2,
    "limit": 20,
    "totalItems": 4821,
    "totalPages": 242
  }
}
```

**Production trap with page/limit (offset-based pagination):** if books are being added/removed while a client pages through, results can shift — a book might appear twice or get skipped entirely between page 2 and page 3. Offset pagination is also **slow at scale** — `OFFSET 500000` still means the database scans and discards 500,000 rows first.

**The production-grade alternative: cursor-based pagination**
```http
GET /books?limit=20&cursor=eyJpZCI6OTc4MX0
```
The cursor is an opaque token (often base64-encoded last-seen ID or sort key) — the server uses it as a stable "resume point" rather than a raw offset. This avoids both the shifting-results problem and the slow-offset problem. Response includes the next cursor:
```json
{
  "data": [ /* 20 books */ ],
  "nextCursor": "eyJpZCI6OTgwMX0",
  "hasMore": true
}
```

**Rule of thumb:** offset pagination (`page`/`limit`) is fine for admin dashboards, small datasets, or "jump to page 5" UX needs. Cursor pagination is what you want for large, frequently-changing, or infinite-scroll-style collections. Many production APIs (Stripe, GitHub, Twitter) default to cursor-based for exactly this reason.

### Filtering — query parameters, not path segments

```http
GET /books?author=Herbert&available=true&publishedAfter=1960
```

Filters go in the query string because they're **optional and combinable** — that's what query params are for. Never do this:
```
BAD: GET /books/author/Herbert/available/true
```
This looks like nested resources (Ch. 3) but isn't — it implies a fixed hierarchy that doesn't actually exist, and it doesn't compose (what if you want three filters, or none?).

### Sorting

```http
GET /books?sort=publishedYear
GET /books?sort=-publishedYear        (leading "-" = descending)
GET /books?sort=author,-publishedYear  (multi-field: author asc, then year desc)
```

This `-` prefix convention (popularized by JSON:API) is widely recognized — using it means developers familiar with other APIs can guess your sorting syntax without reading docs.

### Field selection / sparse fieldsets (partial responses)

For large resources, let clients ask for only the fields they need — reduces payload size, especially valuable on mobile:
```http
GET /books/9781?fields=title,author
→ { "title": "Dune", "author": "Frank Herbert" }
```
This becomes genuinely important at scale — we'll revisit it in Chapter 15 (Performance) alongside the N+1 problem.

### Searching — a step beyond filtering

```http
GET /books?q=dune+herbert
```
A generic `q` param for free-text search is a common convention when filtering by exact field match isn't enough (fuzzy/relevance-based search). If search gets sophisticated (multi-field relevance ranking, typo tolerance), many production teams actually route this to a dedicated search endpoint or a separate search service (Elasticsearch, etc.) rather than trying to force it into the same `/books` collection semantics — worth knowing as a scaling signal, not a REST rule per se.

### Combining it all — a realistic production query

```http
GET /books?author=Herbert&available=true&sort=-publishedYear&limit=20&cursor=eyJpZCI6OTc4MX0&fields=title,author,copies
```

Every piece is a query param, all optional, all composable — this is the uniform-interface payoff from Chapter 2 in action: one predictable pattern handles filtering, sorting, pagination, and field selection across *every* collection in your API, not just `/books`.

### Exercise

A librarian wants: "show me all overdue loans, sorted by how overdue they are (most overdue first), 10 per page, starting from a specific cursor." Write the full query string.

---

```http
GET /loans?status=overdue&sort=-dueDate&limit=10&cursor=eyJpZCI6MTd9
```

Breaking down why: `status=overdue` is the filter, `sort=-dueDate` gives most-overdue-first (oldest due date = most overdue, so descending on... actually let's be precise here — sorting by `-dueDate` puts the *most recent* due date first, which is the *least* overdue. To get most-overdue-first, you want the oldest due dates first, i.e. **ascending**: `sort=dueDate`, no minus). Corrected:

```http
GET /loans?status=overdue&sort=dueDate&limit=10&cursor=eyJpZCI6MTd9
```

Good instinct to combine all four pieces in one query — that's exactly the composability this chapter was making the case for.

## Chapter 8: Statelessness — A Deeper Look

We touched statelessness in Chapter 2 as one of the six constraints. Now let's go past the definition into what it actually costs and buys you in production.

### The core rule, restated precisely

> Every request must carry everything needed to process it. The server holds no per-client context between requests.

"Context" here specifically means anything about *this particular client's conversation history* — not the resource data itself (the database is allowed to have state; that's not what this constraint is about).

### What breaks if you violate it

```
STATEFUL (bad):
1. POST /login { username, password }
   Server: creates session, stores in memory: { sessionId: "abc" → memberId: 42 }
   Server: sets cookie sessionId=abc

2. GET /my-loans
   Cookie: sessionId=abc
   Server: looks up "abc" in its in-memory session store → finds memberId 42
```

This looks convenient, but request #2 can **only be handled by the exact server instance that created the session**. Now trace what that costs you:

- **Load balancing breaks** — you need "sticky sessions" (routing a client to the same server every time), which defeats the purpose of load balancing (spreading load evenly).
- **Autoscaling breaks** — if you spin up new server instances under load, they don't have the old sessions. If you scale down (kill an instance), every session on it is lost — users get logged out mid-task.
- **Deploys become risky** — rolling out a new version means restarting servers, which wipes their in-memory sessions.

### The stateless fix

```
STATELESS (good):
1. POST /login { username, password }
   Server: validates, returns a self-contained token (JWT)
   Token payload: { memberId: 42, exp: 1735689600, ... } signed by server's secret key

2. GET /my-loans
   Authorization: Bearer <token>
   Server: verifies signature, decodes memberId=42 directly from the token — no lookup needed
```

Any server instance — old, new, freshly scaled — can handle request #2, because **the token itself carries the identity**. No shared memory, no sticky routing, no session store to keep in sync across a fleet.

### "But don't JWTs need to be checked against a database sometimes?"

Good production nuance: pure stateless JWTs have a real downside — you **can't revoke them early**. If a token is stolen, or a user is banned, the token stays valid until it naturally expires, because there's no server-side record to invalidate.

Common production compromise: **short-lived access tokens + a stateful refresh token**.
```
Access token: JWT, expires in 15 minutes, fully stateless — handles almost all API calls
Refresh token: opaque string, stored server-side (DB/Redis), used only to mint new access tokens
```
This is a deliberate, small reintroduction of state — but it's isolated to the *login/refresh flow*, not smeared across every single API call. The 99% of requests (reads, writes to books/loans/etc.) remain fully stateless and freely load-balanceable; only the rare refresh call touches shared state.

### What's *not* a statelessness violation

A common confusion: **the database having data is not a violation.** Statelessness is about the server not remembering *this specific client's conversation*. The Library API's Postgres database full of books/members/loans is completely fine — every request re-reads what it needs from that shared store, no request depends on the server "remembering" the previous request from that same client.

```
FINE: GET /books/9781 → server queries DB fresh every time. Not a violation.
VIOLATION: server remembers "client X was just looking at /books/9781" and uses that to change behavior on client X's next unrelated request without X sending that context again.
```

### Checkpoint

We're now through Ch. 1–8. Before Chapter 9 (HATEOAS — the constraint almost nobody implements, and the honest reasons why), want a short recap of Chapters 1–8, or keep moving?

---

## Chapter 9: HATEOAS — The Constraint Almost Nobody Implements

This is the most misunderstood, most skipped, and most *argued about* part of REST. Worth understanding properly rather than dismissing.

### What it actually means

**HATEOAS = Hypermedia As The Engine Of Application State.** The idea: a client shouldn't need to hardcode API structure (which endpoints exist, what transitions are possible from a given state). Instead, **every response tells the client what it can do next**, via links embedded in the response itself.

```json
GET /loans/17

{
  "id": 17,
  "bookId": 9781,
  "memberId": 42,
  "status": "active",
  "dueDate": "2026-09-29",
  "_links": {
    "self": { "href": "/loans/17" },
    "book": { "href": "/books/9781" },
    "renew": { "href": "/loans/17/renewals", "method": "POST" },
    "return": { "href": "/loans/17", "method": "PATCH" }
  }
}
```

The client doesn't need to know in advance "you can renew an active loan by POSTing to `/loans/{id}/renewals`." The server tells it, right there in the response. If the loan is already returned, the server simply **omits** the `renew` link — the client doesn't need business-rule logic hardcoded ("only show renew button if status === active"); it just checks whether the link exists.

### Why this was the whole point, originally

Think back to the web browser analogy from Chapter 1. You don't hardcode into your browser "after a Google search, the next URL pattern is `/search?q=...`." The search results page just **contains links** — the browser (and you) discover what's possible by reading the page. Fielding's argument: APIs should work the same way. A truly RESTful client only needs to know **one entry-point URL** — everything else is discovered by following links in responses, the same way you navigate the web by clicking, not by memorizing URL patterns.

### Why almost nobody actually does this

Be honest about the trade-offs, because they're real:

1. **Client complexity goes up, not down.** Instead of a client dev hardcoding `POST /loans/{id}/renewals` (simple, fast to write), they now need generic logic to parse `_links`, find the one named "renew," and use its `href`/`method` dynamically. Most frontend teams find this *more* work, not less, for typical apps with a known, stable set of screens.
2. **Mobile app release cycles kill the benefit.** HATEOAS's promise is "server can change URL structure without breaking clients." But mobile apps take weeks to get through app-store review — even if the server *could* evolve safely, the app can't consume new capabilities until a new build ships anyway. Much of the benefit assumes a web-style client that reloads fresh code every time (which a browser does, but a compiled app doesn't).
3. **Most APIs have fixed, known clients.** HATEOAS shines when *many unknown third-party clients* consume your API and need to adapt to change without recompiling (this is closer to public web scale). Most companies build APIs primarily for their *own* known frontend/mobile teams — the discovery problem HATEOAS solves barely exists when the client and server ship from the same team, often in lockstep.
4. **Tooling and ecosystem never caught up.** Most API design/documentation tools (Swagger/OpenAPI's dominant style, most client SDK generators) are built around a fixed, documented set of endpoints — not runtime discovery. You'd be swimming against your whole tooling ecosystem.

### The honest verdict

Stripe, GitHub, Twitter, Shopify — virtually every major production API — are **not HATEOAS-compliant**, and they're wildly successful. They document a fixed set of endpoints (often gorgeously, via OpenAPI/Swagger), and clients hardcode paths. This is so common that the term **"REST" in industry practice basically means "HTTP + JSON + resource URIs + proper status codes,"** *without* HATEOAS — sometimes people say "**Level 2 REST**" (see box below) to be precise about this.

**Richardson Maturity Model** — a useful way to place where an API sits:
```
Level 0: One URI, one HTTP method (basically RPC-over-HTTP, e.g. everything via POST /api)
Level 1: Multiple URIs (resources exist) but still one method (mostly POST) — the RPC leak from Ch.1
Level 2: Proper HTTP methods + status codes used correctly — most production "REST APIs" stop here
Level 3: HATEOAS — full hypermedia-driven discovery — rare in practice
```

Nearly everything we've built in this series so far (Ch. 3–8) gets you to **Level 2**, which is the honest, accepted industry target for "a good REST API." Level 3 is a legitimate architectural choice for specific cases (public APIs with many unpredictable third-party clients, or where API evolution needs to be silent/backward-compatible without version bumps), but it's not something to feel guilty about skipping.

### When HATEOAS *is* worth it

- Public, third-party-consumed APIs where you can't coordinate deploys with every client (e.g., a hypermedia-driven API for an ecosystem of unknown integrators)
- Workflow/state-machine-heavy domains where "what can I do next" genuinely varies a lot per resource state (e.g., an order that can be `paid → shipped → delivered`, each with different valid next actions) — links naturally express "here's what's legal from here," reducing client-side business logic duplication
- HAL, JSON:API, and Siren are real media-type specs if you do want to do this properly, rather than inventing your own `_links` format

### Checkpoint

Chapter 10 is next — versioning strategies. You've actually already gone deep on this topic with me before (it's in the recent-work notes — global vs per-API versioning, migration/deprecation). Want me to do a **compressed recap** connecting what we already covered to everything since (resources, HATEOAS, statelessness), or **skip Chapter 10 entirely** and jump to Part 4 (Production Concerns — error handling, rate limiting, caching, security, performance)?

---

Taking "next" as: keep the momentum, so here's the **compressed connector** version of Chapter 10, then straight into Part 4.

## Chapter 10: Versioning — Quick Connector

You already have the deep version of this (strategies, migration, global vs per-API). The one thing worth bolting on now that we've covered HATEOAS: **versioning and HATEOAS are two competing philosophies for handling API change.**

- **Versioning (what you learned before)** says: "when the contract changes, bump a version — `/v2/books` or `Accept: application/vnd.library.v2+json` — and let old clients keep hitting the old version until they migrate."
- **HATEOAS (Ch. 9)** says: "if clients discover capabilities via links instead of hardcoding URLs, many changes don't need a version bump at all — the server just stops including a link, and well-behaved clients naturally stop offering that action."

In practice, most production APIs (Level 2, no HATEOAS) rely entirely on versioning for change management — which is exactly why versioning became such a heavily discussed topic in the industry, and why you went deep on it already. If you want, later I can do a dedicated side-by-side chapter contrasting them — flag it if so. For now, moving to Part 4.

---

## Chapter 11: Error Handling Design

A wildly underrated part of API design. Bad error responses are one of the top causes of wasted integration time for API consumers.

### The anti-pattern: inconsistent, ad-hoc errors

```json
Endpoint A: { "error": "Book not found" }
Endpoint B: { "message": "invalid input", "code": 4 }
Endpoint C: { "errors": ["title is required", "isbn is invalid"] }
```

Three different shapes for "something went wrong" means every client has to write custom error-parsing logic *per endpoint*. This violates the uniform interface principle just as much as inconsistent URIs do.

### The production standard: RFC 7807 (`application/problem+json`)

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "https://api.library.com/errors/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "The book could not be created due to validation errors.",
  "instance": "/books",
  "errors": [
    { "field": "isbn", "message": "must be a valid ISBN-13" },
    { "field": "copies", "message": "must be a positive integer" }
  ]
}
```

Field meanings:
- `type` — a URI identifying the *category* of error (can even be a docs page)
- `title` — short, human-readable summary, stable across occurrences of this error type
- `status` — repeats the HTTP status code (redundant but useful when logs strip headers)
- `detail` — specific, human-readable explanation for *this* occurrence
- `instance` — the specific URI that caused the error
- Anything else (like our `errors` array) — extension fields specific to your API

This is an actual IETF standard (RFC 7807, and its 2023 successor RFC 9457), not a convention someone invented — meaning tooling and libraries across many languages already know how to parse it.

### One consistent shape, every endpoint, every error

```json
{
  "type": "https://api.library.com/errors/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "No book found with id 999999.",
  "instance": "/books/999999"
}
```
```json
{
  "type": "https://api.library.com/errors/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Book 9781 is not available for reservation because it is currently available for direct loan.",
  "instance": "/reservations"
}
```

Same shape every time → clients write **one** error-handling code path for your entire API, not one per endpoint.

### What never belongs in an error response

- Stack traces
- SQL queries or ORM internals
- Internal hostnames, file paths, or infra details
- Anything that helps an attacker map your system (this connects to Ch. 14, security)

```
BAD:
{ "error": "org.postgresql.util.PSQLException: relation \"books\" does not exist at Book.java:142" }

GOOD:
{ "type": "...", "title": "Internal Server Error", "status": 500, "detail": "Something went wrong processing your request." }
```
Log the detailed version server-side (with a request ID for correlation), return the sanitized version to the client.

### Validation errors specifically — be exhaustive, not sequential

```
BAD (stops at first error, client fixes one, resubmits, hits the next one):
{ "detail": "isbn is invalid" }
→ client fixes isbn, resubmits
{ "detail": "copies must be positive" }
→ client fixes copies, resubmits again — 3 round trips for 3 errors

GOOD (report everything at once):
{ "errors": [
    { "field": "isbn", "message": "must be a valid ISBN-13" },
    { "field": "copies", "message": "must be a positive integer" }
]}
```

### Exercise

A client sends `POST /loans` for a book that has zero copies available. Design the full error response — status code and full RFC 7807 body.

---

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://api.library.com/errors/no-copies-available",
  "title": "No Copies Available",
  "status": 409,
  "detail": "Book 9781 has zero copies available for loan.",
  "instance": "/loans"
}
```

Why 409 and not 422: the request is well-formed (valid book ID, valid member ID) — the problem is purely that the *current state* of the resource (zero available copies) conflicts with what's being requested. Same reasoning as the reservation example back in Chapter 5.

## Chapter 12: Rate Limiting, Throttling, and Idempotency Keys (Deep Dive)

### Why rate limiting exists

Without limits, one misbehaving client (buggy retry loop, scraper, or malicious actor) can degrade service for everyone else sharing your infrastructure. Rate limiting protects fairness and stability, not just security.

### The standard headers (RFC-track, widely adopted)

```http
GET /books
RateLimit-Limit: 100
RateLimit-Remaining: 87
RateLimit-Reset: 1735689600
```

When the limit is hit:
```http
HTTP/1.1 429 Too Many Requests
RateLimit-Limit: 100
RateLimit-Remaining: 0
RateLimit-Reset: 1735689600
Retry-After: 42

{
  "type": "https://api.library.com/errors/rate-limited",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Rate limit exceeded. Retry after 42 seconds."
}
```
`Retry-After` (in seconds, or an HTTP date) tells a well-behaved client *exactly* when to try again — this is what lets clients implement smart backoff instead of guessing.

### Common algorithms (know the tradeoffs, not just names)

- **Fixed window** (e.g., "100 requests per minute, resetting on the minute") — simple, but has a burst problem at window boundaries: a client can send 100 requests at 11:59:59 and another 100 at 12:00:01, effectively 200 in 2 seconds.
- **Sliding window** — smooths this out by tracking a rolling time range instead of fixed buckets. More accurate, slightly more expensive to compute.
- **Token bucket** — a bucket refills at a steady rate; each request consumes a token. Naturally allows short bursts (if tokens are saved up) while enforcing a steady average rate. This is the most common production choice — it balances burst tolerance with average-rate control.

You don't need to implement these yourself in most production setups — API gateways (Kong, AWS API Gateway, Cloudflare) or libraries handle this. What you need is to **understand which behavior you want** (strict vs. burst-tolerant) so you configure it correctly.

### Per-what? Scoping your rate limits

```
Per API key / per authenticated user   → most common, fair, ties to your billing/plan tiers
Per IP address                          → useful for unauthenticated endpoints (login, public search)
Per endpoint                            → expensive endpoints (search, reports) often get tighter limits than cheap ones (GET /books/{id})
```

Production APIs typically layer these — e.g., "1000 req/hour per API key, but `/reports/generate` specifically capped at 10 req/hour regardless."

### Idempotency keys — the full production pattern (promised back in Chapter 4)

```http
POST /loans
Idempotency-Key: 7f3e9c2a-4b1e-4a3f-9c2a-1e4b3f9c2a1e
Content-Type: application/json

{ "bookId": 9781, "memberId": 42 }
```

**Server-side implementation sketch:**
1. Client generates a UUID once, per logical operation (not regenerated on retry).
2. Server receives the request. Checks: have I seen this `Idempotency-Key` before (in a fast store — Redis, with a TTL of maybe 24h)?
   - **No** → process normally, store `{key: result}`, return result.
   - **Yes, and original request is still processing** → return `409 Conflict` or make the client wait (avoids a race where two identical requests both start processing simultaneously).
   - **Yes, and original completed** → return the *stored* result again, without reprocessing. Same status code, same body, as if it just happened — the client can't tell the difference between "first time" and "replay."

**Important subtlety:** the idempotency key should be scoped to the request *body* too, ideally — if a client reuses a key with a *different* body, that's a client bug, and the server should reject it (`422` with a clear message) rather than silently returning the old, mismatched result.

### Checkpoint

Ready for Chapter 13 — Caching (ETags, Cache-Control, conditional requests)? This is where ETags from Chapter 6 get fully built out, plus the performance payoff of doing it right.

---

## Chapter 13: Caching — ETags, Cache-Control, Conditional Requests

### Why caching matters more than people think

Every request your server *doesn't* have to fully process is free capacity for everything else. Caching isn't an optimization you bolt on later — done right, it's core to how a RESTful API is supposed to behave (recall Cacheability, Ch. 2, is one of the six formal constraints).

### `Cache-Control` — the primary directive

```http
GET /books/9781
→ 200 OK
Cache-Control: public, max-age=3600
```

Common directives:
```
public              → can be cached by any intermediary (CDN, shared proxy), not just the browser
private              → only the end client may cache it (e.g., user-specific data)
max-age=3600         → cache is fresh for 3600 seconds; after that, treat as stale
no-cache              → can be cached, but MUST revalidate with the server before reuse (misleading name — it does allow caching!)
no-store               → never cache this at all, anywhere (use for sensitive data — auth tokens, payment info)
```

**A common trap:** thinking `no-cache` means "don't cache." It actually means "cache it, but check with me first every time before using the cached copy." If you truly mean "never store this," you want `no-store`.

### Applying it to the Library API

```
GET /books/9781                      → Cache-Control: public, max-age=3600   (book metadata rarely changes)
GET /loans/17                         → Cache-Control: private, no-store      (personal, changes often, sensitive-ish)
GET /members/42/loans                → Cache-Control: private, max-age=60     (personal but okay to cache briefly client-side)
```

### ETags and conditional requests — validate without re-downloading

`max-age` alone has a problem: once it expires, the client has to assume the data is stale and either refetch, or risk showing old data. **ETags solve this** by letting the client ask "has this actually changed?" cheaply.

```http
GET /books/9781
→ 200 OK
ETag: "a1b2c3d4"
Cache-Control: public, max-age=3600
{ "id": 9781, "title": "Dune", "copies": 5 }
```

The ETag is a fingerprint of the resource's current state (often a hash of the content, or a version number). Later, once the cache is stale:

```http
GET /books/9781
If-None-Match: "a1b2c3d4"

→ 304 Not Modified          (empty body — nothing changed, keep using your cached copy)
   OR
→ 200 OK, new ETag: "e5f6g7h8"   (something changed, here's the fresh data)
```

**Why this matters in production:** a `304` response has essentially no body — you save the full payload's bandwidth while still confirming freshness. For large resources (a book's full catalog listing, a big report), this is a massive efficiency win over either "always trust the cache blindly" or "always refetch everything."

### ETags for writes too — optimistic concurrency control

This is the part people miss: ETags aren't just for GET caching, they solve a real write-safety problem — **the lost update problem.**

```
Scenario: two librarians edit the same book record at the same time.

Librarian A: GET /books/9781 → ETag: "v1", copies: 5
Librarian B: GET /books/9781 → ETag: "v1", copies: 5

Librarian A: PATCH /books/9781 { "copies": 4 }   → succeeds, new ETag: "v2"
Librarian B: PATCH /books/9781 { "copies": 6 }   → without ETag checking, this OVERWRITES A's change silently!
```

Fix — require the client to prove it's editing the version it thinks it's editing:
```http
PATCH /books/9781
If-Match: "v1"

→ if server's current ETag is still "v1": proceed, succeed
→ if server's current ETag is now "v2" (A already changed it): 412 Precondition Failed
```

`412 Precondition Failed` tells Librarian B: "the book changed since you last read it — refetch and decide how to proceed" instead of silently clobbering A's update. This is a lightweight, standard alternative to full pessimistic locking, and it's genuinely important in any API where concurrent writes to the same resource are plausible.

### `Last-Modified` / `If-Modified-Since` — the older, coarser cousin

```http
GET /books/9781
→ Last-Modified: Tue, 15 Sep 2026 08:00:00 GMT

GET /books/9781
If-Modified-Since: Tue, 15 Sep 2026 08:00:00 GMT
→ 304 Not Modified
```
Same idea as ETags, but timestamp-based instead of content-hash-based — coarser (only second-level precision, so two changes within the same second are indistinguishable) and largely superseded by ETags in modern APIs. Worth recognizing since you'll see it in older systems, but ETags are the better tool to reach for today.

### CDN-level caching — a production-scale note

For fully public, non-personalized resources (`GET /books/9781` for an anonymous browsing endpoint), `Cache-Control: public, max-age=3600` lets a CDN (Cloudflare, CloudFront) serve the response **without your origin server even being hit** for most requests. This is often the single highest-leverage performance change in a production API — correctly cacheable GETs offload the vast majority of read traffic away from your servers entirely.

### Exercise

Two clients GET the same loan record and get `ETag: "v3"`. One of them PATCHes it to mark it returned. What should the *other* client's subsequent PATCH attempt (still holding `"v3"`, unaware of the first change) receive if it also sends `If-Match: "v3"`, and why?

---

It should receive **`412 Precondition Failed`** — because by the time the second PATCH arrives, the server's actual current ETag is no longer `"v3"` (the first PATCH already changed the loan and bumped it to something like `"v4"`). The `If-Match: "v3"` header is the client's way of saying "only apply my change if the resource is still in the state I last saw it" — since it isn't, the server refuses the write rather than silently overwriting the first client's update. The second client then knows to `GET` the fresh state (new ETag, current status) and decide what to do — rather than blindly clobbering someone else's change, which is exactly the lost-update problem this mechanism exists to prevent.

## Chapter 14: Security Practices

This is the chapter where "it works" and "it's production-safe" diverge the most. Let's go through the practical, high-leverage items.

### Authentication patterns

```
API Key           → simplest, good for server-to-server, no per-user identity, easy to leak if not handled carefully
Bearer Token (JWT) → most common for user-facing APIs, stateless (Ch. 8), carries claims (memberId, roles, expiry)
OAuth 2.0           → standard for "let a third-party app act on a user's behalf without seeing their password" (e.g., "Sign in with Google")
```

**Never put credentials in the URL:**
```
BAD:  GET /books?apiKey=sk_live_abc123
```
URLs get logged everywhere — server access logs, browser history, proxy logs, `Referer` headers sent to third parties. A leaked log file becomes a leaked credential. Always use headers:
```
GOOD: Authorization: Bearer sk_live_abc123
```

### Authorization — distinct from authentication (Ch. 5 recap, deeper here)

Authentication answers "who are you." Authorization answers "what are you allowed to do." A shockingly common production bug: checking authentication, but forgetting to check authorization per-resource.

```
BAD (classic IDOR — Insecure Direct Object Reference):
GET /loans/17
Authorization: Bearer <Member 42's valid token>

Server logic: "token is valid → return loan 17"
             (doesn't check: does loan 17 actually belong to Member 42?)

→ Member 42 can read/modify ANY loan just by guessing/incrementing IDs!
```

**Fix:** every resource-level operation must check ownership/permission, not just "is this token valid":
```
GOOD:
Server logic: "token valid, decode memberId=42. Does loan 17.memberId == 42? If not → 403."
```
This is one of the most common real vulnerabilities in production APIs (it's consistently in OWASP's API security top 10) precisely because it's easy to forget — auth middleware checks the token and everyone assumes that's "handled," but per-resource ownership needs its own explicit check on every relevant endpoint.

### Input validation — never trust the client

```
BAD: trust that "copies" is always a positive integer because "the frontend validates it"
GOOD: validate on the server too — frontends can be bypassed entirely (curl, Postman, a modified app)
```
Validate: types, ranges, string lengths, formats (email, ISBN), and reject unknown/unexpected fields rather than silently ignoring them (silently ignoring can mask client bugs, or in worse cases, let attackers probe your schema).

### Mass assignment — a specific, common trap

```json
BAD:
PATCH /members/42
{ "name": "Alice", "role": "admin" }
```
If the server naively maps every JSON field straight onto the database record, a client could sneak in `"role": "admin"` on a field never intended to be client-editable. **Fix:** explicitly whitelist which fields an endpoint accepts — never blindly deserialize the whole request body onto your data model.

### CORS — getting `Access-Control-Allow-Origin` right (promised in Ch. 6)

```
BAD (common shortcut, dangerous for authenticated APIs):
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: true    ← browsers actually reject this combination, but people try
```
`*` means "any website in the world can call this API from a user's browser." For public, unauthenticated, read-only data — fine. For anything using cookies/credentialed requests, you must specify exact allowed origins:
```
GOOD:
Access-Control-Allow-Origin: https://library-app.com
Access-Control-Allow-Credentials: true
```

### Rate limiting as a security tool, not just fairness (connects to Ch. 12)

Login endpoints especially need aggressive per-IP rate limiting — without it, `/login` becomes a free brute-force target.

### Don't leak existence via error messages

```
BAD:
POST /login { "email": "x@y.com", "password": "wrong" }
→ "No account found with that email"       (confirms the email exists or not — helps attackers enumerate valid accounts)

GOOD:
→ "Invalid email or password"               (same message regardless of which part was wrong)
```

### TLS — non-negotiable

Every production API must be HTTPS-only. Reject or redirect plain HTTP. This isn't really a "REST" topic specifically, but it's foundational enough that skipping it here would be a real gap.

### Checkpoint

One chapter left before the anti-patterns/alternatives wrap-up: Chapter 15 — Performance Patterns (N+1 avoidance, batching, compression). Ready?

---

## Chapter 15: Performance Patterns

### The N+1 problem — the most common REST performance bug

```http
GET /members/42/loans
→ [ { "id": 17, "bookId": 9781 }, { "id": 18, "bookId": 9782 }, { "id": 19, "bookId": 9783 } ]
```

If the client now wants book titles, the naive approach:
```http
GET /books/9781
GET /books/9782
GET /books/9783
```
**3 loans → 4 total requests (1 + 3).** With 50 loans, that's 51 requests. This is the "N+1 problem" — 1 request to get a list, N more to get details for each item — and it's brutal at scale (network latency compounds, especially on mobile).

**Fix 1 — embed related data (a controlled denormalization):**
```json
GET /members/42/loans?expand=book

[
  { "id": 17, "book": { "id": 9781, "title": "Dune" } },
  { "id": 18, "book": { "id": 9782, "title": "Foundation" } }
]
```
The `expand` (or `include`) query param convention lets the client opt into embedding related resources in one round trip, without forcing every response to *always* carry that weight.

**Fix 2 — batch endpoint:**
```http
GET /books?ids=9781,9782,9783
```
One request, multiple resources by ID — simple, and reusable across many list-then-fetch scenarios.

**Fix 3 — let the client compose in parallel, not sequentially:** if you don't want to build `expand` support, at minimum make sure the client fires the N requests concurrently (`Promise.all`, not a for-loop with `await` inside) rather than serially. Not as good as fixing it server-side, but far better than sequential N+1.

### Response payload size — send less

- **Sparse fieldsets** (Chapter 7's `?fields=title,author`) — don't make a mobile client download full book descriptions when it's rendering a list view that only shows titles.
- **Pagination** (Chapter 7) — never return unbounded collections; a `/books` with no limit returning 50,000 records is a self-inflicted performance incident.
- **Compression:**
```http
GET /books
Accept-Encoding: gzip, br

→ 200 OK
Content-Encoding: gzip
```
Enabling gzip/brotli compression on JSON responses is often a 70-90% size reduction for essentially free — almost always handled at the infrastructure layer (load balancer, gateway, framework middleware) rather than per-endpoint code.

### Batching writes

```http
POST /loans/batch
{ "loans": [ { "bookId": 9781, "memberId": 42 }, { "bookId": 9782, "memberId": 42 } ] }
```
When a client legitimately needs to create/update many resources at once, a dedicated batch endpoint avoids N round trips of network overhead — but be deliberate about it (it's a deviation from pure resource-per-request REST, done for a specific performance reason, not a default pattern to reach for everywhere).

### Async processing for slow operations

```http
POST /reports/circulation-summary
→ 202 Accepted
{ "reportId": "abc123", "status": "processing" }
Location: /reports/abc123

GET /reports/abc123
→ 200 OK, { "status": "completed", "downloadUrl": "..." }
   or
→ 200 OK, { "status": "processing" }
```
If an operation takes seconds-to-minutes (report generation, bulk import), don't hold the HTTP connection open — return `202 Accepted` immediately with a way to poll (or use webhooks) for the result. This connects back to `202` from Chapter 5.

### Database-level awareness (brief, since it's adjacent, not core REST)

Two things worth knowing even though they're implementation, not API contract: **indexing the fields you filter/sort by** (an unindexed `sort=publishedYear` on millions of rows is slow no matter how clean your API design is), and **connection pooling** (each API request that hits the DB should reuse pooled connections, not open a fresh one — a classic scaling killer under load).

---

## Chapter 16: Common REST Anti-Patterns — The Full Recap List

Consolidating everything we've flagged across 15 chapters into one reference list:

1. **Verbs in URIs** — `/getBooks`, `/createLoan` instead of `GET /books`, `POST /loans`
2. **Ignoring HTTP methods, using POST for everything** — Level 0/1 on the Richardson Maturity Model
3. **Wrong status codes** — `200` for everything, or swapping `401`/`403`
4. **PUT used as PATCH** — silently wiping fields the client didn't intend to touch
5. **Inconsistent error shapes** — different JSON structure per endpoint
6. **Leaking internals in errors** — stack traces, SQL, infra details in responses
7. **No pagination** — returning unbounded collections
8. **Deeply nested URIs** — `/members/42/loans/17/book/9781/author/55`
9. **Stateful sessions in server memory** — breaks load balancing and autoscaling
10. **No rate limiting** — one bad client can degrade service for everyone
11. **Credentials in URLs** — `?apiKey=...`, leaked via logs
12. **Missing per-resource authorization checks (IDOR)** — checking auth but not ownership
13. **Mass assignment** — blindly mapping request JSON onto the full data model
14. **`Access-Control-Allow-Origin: *` on authenticated endpoints**
15. **No idempotency handling on POST** — retries create duplicate resources
16. **N+1 request patterns** — no way to fetch related data in one round trip
17. **Ignoring caching entirely** — every GET hits the origin server, even for near-static data

If you only remember one meta-lesson from this whole series: **almost every anti-pattern above is really "treating REST as RPC in disguise" or "trusting the client too much."** Nearly everything traces back to one of those two roots.

## Chapter 17: When REST Is the Wrong Tool

Being honest about alternatives is part of actually understanding REST, not just defending it.

| Use case | Better fit | Why |
|---|---|---|
| Internal microservice-to-microservice calls, high performance needs | **gRPC** | Binary protocol (protobuf), much lower overhead than JSON/HTTP, strongly-typed contracts, built-in streaming |
| Client needs to fetch exactly the fields it wants across multiple resource types in one request | **GraphQL** | Solves over-fetching/under-fetching and N+1-across-resource-types elegantly; trade-off: harder to cache (no uniform GET URIs), more complex server implementation |
| Real-time, bidirectional updates (chat, live dashboards, notifications) | **WebSockets** | REST's request-response model doesn't fit server-initiated pushes; you'd otherwise resort to inefficient polling |
| Decoupled services reacting to state changes, not directly calling each other | **Event-driven / message queues** (Kafka, SQS) | REST assumes a direct request-response relationship; event systems let producers and consumers evolve independently without either knowing the other exists |
| Public API where you want third-party developers to browse/discover capabilities over years without version coordination | **HATEOAS-heavy REST or GraphQL introspection** | The discovery problem Ch. 9 covered — genuinely matters here |

REST's sweet spot remains: **CRUD-shaped resources, consumed over HTTP, by a reasonably-sized set of clients, where caching and simplicity matter more than flexibility of query shape.** That's most APIs — which is why REST (specifically, Level 2 REST) remains the default choice, not because it's "best" in the abstract, but because it fits the most common shape of problem.

---

That's the full series — 17 chapters, Chapter 1 through here. You now have the vocabulary and reasoning to both build a solid production REST API for something like your Library system, and to critically evaluate any existing API's design choices rather than just copying conventions blindly.

Want a **condensed one-page cheat sheet** pulling the highest-leverage rules from all 17 chapters, or want to pick one chapter and go deeper with a hands-on build (e.g., actually scaffold the Library API in a framework of your choice, applying everything)?

---

Absolutely. Let’s treat this as a **REST API course from zero → production-grade architecture**.

The goal isn't just to memorize `GET`, `POST`, `PUT`, etc. You want to understand **why REST exists, what makes an API RESTful, how HTTP actually works, what good API design looks like, and the mistakes that hurt production systems**.

---

# REST API — Zero to Production

## What you'll learn

We'll build understanding in this order:

1. **What an API is**
2. **What REST actually means**
3. **HTTP fundamentals**
4. **Resources and URLs**
5. **HTTP methods**
6. **Status codes**
7. **Request/response design**
8. **CRUD API design**
9. **REST naming conventions**
10. **Validation and errors**
11. **Pagination, filtering, sorting, searching**
12. **Authentication & authorization**
13. **Idempotency**
14. **Concurrency and optimistic locking**
15. **Caching**
16. **Rate limiting**
17. **Versioning**
18. **Transactions and consistency**
19. **Webhooks and async operations**
20. **Security**
21. **Observability**
22. **Performance**
23. **Distributed systems concerns**
24. **Production API architecture**
25. **Common REST anti-patterns**
26. **Testing**
27. **Documentation**
28. **Advanced design principles**
29. **A complete production-grade example**

We'll use a running example:

> **E-commerce API**

with resources such as:

```text
users
products
orders
payments
```

---

# 1. First: What is an API?

Before REST, understand the problem APIs solve.

Suppose you have:

```text
Mobile App
     |
     |
     v
Backend Server
     |
     v
Database
```

The mobile application needs to ask the backend:

> "Give me my orders."

The application cannot simply reach into your database.

You need a **contract** between the client and server.

That's an API.

For example:

```http
GET /orders
```

The server might respond:

```json
{
  "data": [
    {
      "id": "ord_123",
      "status": "shipped",
      "total": 2499
    }
  ]
}
```

The API defines:

* what the client can ask for
* how it asks
* what the server returns
* what errors mean
* what authentication is required
* what rules clients must follow

So:

> **An API is a contract for communication between software systems.**

---

# 2. Why REST?

There are many ways to design APIs.

For example:

### RPC-style

```http
POST /getUser
POST /createUser
POST /deleteUser
POST /cancelOrder
```

Or:

### REST-style

```http
GET    /users/123
POST   /users
DELETE /users/123
POST   /orders/123/cancellation
```

REST gives us a set of architectural principles for using HTTP in a consistent way.

REST stands for:

> **Representational State Transfer**

The name sounds complicated.

The basic idea is much simpler:

> **Treat things in your system as resources and use standard HTTP semantics to operate on them.**

For example:

```text
User
Product
Order
Payment
Comment
Invoice
```

become resources.

---

# 3. The most important REST concept: Resources

This is probably the single most important concept.

Imagine your system contains:

```text
Users
Products
Orders
```

These are **resources**.

We identify resources with URLs.

```http
/users
/products
/orders
```

A particular resource:

```http
/users/123
/products/456
/orders/789
```

Think:

```text
/users
    └── collection

/users/123
    └── individual resource
```

Similarly:

```text
/orders
/orders/123
/orders/123/items
/orders/123/items/456
```

---

# 4. URLs should represent nouns, not actions

This is a classic REST rule.

### ❌ Bad

```http
GET /getUsers
POST /createUser
POST /deleteUser
POST /updateUser
```

The URL contains the operation.

HTTP already provides the operation.

### ✅ Better

```http
GET    /users
POST   /users
GET    /users/123
PATCH  /users/123
DELETE /users/123
```

The URL identifies **what** you're working with.

The HTTP method describes **what you want to do**.

Think:

```text
HTTP method = action
URL         = resource
```

---

# 5. HTTP methods

The core methods you'll use are:

| Method | Meaning                     |
| ------ | --------------------------- |
| GET    | Retrieve                    |
| POST   | Create / trigger processing |
| PUT    | Replace                     |
| PATCH  | Partially modify            |
| DELETE | Delete                      |

Let's understand them deeply.

---

# 6. GET

Used to retrieve a resource.

```http
GET /users/123
```

Response:

```json
{
  "id": "123",
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

GET should generally be:

### Safe

It shouldn't intentionally change server state.

This is bad:

```http
GET /users/123/delete
```

or:

```http
GET /orders/123/cancel
```

because merely visiting the URL changes state.

---

# 7. POST

POST is generally used when you're asking the server to process something where the server determines the resulting resource or operation.

For example:

```http
POST /users
```

Body:

```json
{
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

Server:

```http
201 Created
```

```json
{
  "id": "usr_123",
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

Often:

```http
Location: /users/usr_123
```

---

# 8. PUT

PUT means:

> Replace the representation of a resource at this URI.

For example:

```http
PUT /users/123
```

```json
{
  "name": "Riyaz",
  "email": "new@example.com"
}
```

Conceptually, you're saying:

> "Make `/users/123` look like this."

This is different from PATCH.

---

# 9. PATCH

PATCH means:

> Apply a partial modification.

```http
PATCH /users/123
```

```json
{
  "email": "new@example.com"
}
```

Only email changes.

Name remains unchanged.

For most modern APIs, PATCH is useful for partial updates.

---

# 10. PUT vs PATCH

This causes a lot of confusion.

Imagine:

```json
{
  "name": "Riyaz",
  "email": "a@example.com",
  "phone": "123"
}
```

### PUT

Conceptually:

```http
PUT /users/123
```

```json
{
  "name": "Riyaz",
  "email": "b@example.com",
  "phone": "456"
}
```

You're providing the replacement representation.

### PATCH

```http
PATCH /users/123
```

```json
{
  "email": "b@example.com"
}
```

You're changing only email.

---

# 11. DELETE

```http
DELETE /users/123
```

Usually:

```http
204 No Content
```

But don't blindly assume DELETE means physical deletion.

Production systems frequently use:

```text
soft delete
```

For example:

```text
deleted_at = timestamp
```

The API may still expose:

```http
DELETE /users/123
```

while the database keeps the record.

That's perfectly compatible with a resource-oriented API.

---

# 12. CRUD mapped to REST

A common mapping:

| Operation | HTTP                |
| --------- | ------------------- |
| List      | GET `/users`        |
| Get       | GET `/users/123`    |
| Create    | POST `/users`       |
| Replace   | PUT `/users/123`    |
| Update    | PATCH `/users/123`  |
| Delete    | DELETE `/users/123` |

This gives us:

```text
GET    /products
GET    /products/123
POST   /products
PUT    /products/123
PATCH  /products/123
DELETE /products/123
```

---

# 13. Collections vs individual resources

This distinction is extremely important.

### Collection

```http
GET /users
```

Means:

> Give me a collection of users.

### Individual resource

```http
GET /users/123
```

Means:

> Give me user 123.

Same with:

```text
/products
/products/123

/orders
/orders/123
```

---

# 14. Nested resources

Suppose an order contains items.

You could represent:

```http
GET /orders/123/items
```

This means:

> Items belonging to order 123.

Specific item:

```http
GET /orders/123/items/456
```

This can be useful when the relationship itself matters.

But don't over-nest.

### ❌ Bad

```text
/users/123/orders/456/items/789/payments/999/transactions/111
```

That's horrible to work with.

Prefer flatter resource URLs when appropriate:

```http
GET /transactions/111
```

The relationship can be represented in the response.

---

# 15. Naming URLs

Prefer plural nouns:

```text
/users
/products
/orders
/invoices
```

rather than:

```text
/user
/product
/order
```

There isn't a universal law requiring plurals, but consistency matters more than the specific choice.

I strongly recommend:

```text
/users
/users/{id}
```

because it makes collection/resource semantics obvious.

---

# 16. Avoid verbs in URLs

### ❌

```http
POST /createOrder
POST /updateOrder
POST /deleteOrder
GET /getOrder
```

### Better

```http
POST   /orders
GET    /orders/123
PATCH  /orders/123
DELETE /orders/123
```

But there's an important exception.

Some operations aren't naturally CRUD.

For example:

> Cancel an order.

You have several reasonable designs.

```http
POST /orders/123/cancellations
```

or, depending on your domain model:

```http
POST /orders/123/cancel
```

The first is more resource-oriented.

The important thing isn't blindly eliminating every verb.

It's avoiding RPC disguised as REST everywhere.

---

# 17. HTTP status codes

Don't just return:

```http
200
```

for everything.

Status codes communicate important semantics.

The major categories:

```text
1xx informational
2xx success
3xx redirection
4xx client error
5xx server error
```

You should know these especially well:

```text
200 OK
201 Created
202 Accepted
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
405 Method Not Allowed
409 Conflict
412 Precondition Failed
415 Unsupported Media Type
422 Unprocessable Content
429 Too Many Requests

500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout
```

---

# 18. 200 OK

Normal successful request.

```http
GET /users/123
```

```http
200 OK
```

```json
{
  "id": "123",
  "name": "Riyaz"
}
```

---

# 19. 201 Created

Use when a new resource has been created.

```http
POST /users
```

Response:

```http
201 Created
Location: /users/123
```

```json
{
  "id": "123",
  "name": "Riyaz"
}
```

This is much more expressive than always returning 200.

---

# 20. 204 No Content

Successful operation with no response body.

Example:

```http
DELETE /users/123
```

```http
204 No Content
```

Don't send:

```json
{}
```

with 204.

204 means:

> There is no response content.

---

# 21. 400 Bad Request

The request itself is malformed or invalid in a general HTTP/request sense.

Example:

```http
GET /users/abc%ZZ
```

or malformed JSON:

```json
{
  "name": "Riyaz"
```

Could result in:

```http
400 Bad Request
```

---

# 22. 401 vs 403

This is extremely important.

### 401

Authentication is missing or invalid.

Think:

> **Who are you?**

Example:

```http
GET /admin/reports
Authorization: Bearer invalid-token
```

Response:

```http
401 Unauthorized
```

### 403

The server knows who you are, but you aren't allowed to perform the action.

Think:

> **I know who you are, but you can't do this.**

```http
403 Forbidden
```

Example:

```text
User A tries to access User B's private admin report.
```

---

# 23. 404 Not Found

Resource doesn't exist.

```http
GET /users/999999
```

```http
404 Not Found
```

Don't return:

```http
200 OK
```

```json
{
  "error": "user not found"
}
```

unless you have a very specific reason.

HTTP already has the appropriate status.

---

# 24. 409 Conflict

Useful when the request conflicts with the current state.

Example:

```http
POST /users
```

```json
{
  "email": "existing@example.com"
}
```

If email must be unique:

```http
409 Conflict
```

Another example:

```text
Attempting to modify an order that has already been shipped.
```

---

# 25. 422 Unprocessable Content

Useful when the request is syntactically valid but semantically invalid.

For example:

```json
{
  "age": -20
}
```

JSON is valid.

But your business validation rejects it.

Potential response:

```http
422 Unprocessable Content
```

There is some debate around 400 vs 422. The key is **consistency**.

---

# 26. 429 Too Many Requests

Rate limiting.

```http
429 Too Many Requests
Retry-After: 30
```

Meaning:

> You're sending requests too quickly. Try again later.

---

# 27. 500 Internal Server Error

Something went wrong on your server.

The client shouldn't receive:

```json
{
  "stackTrace": "...",
  "databasePassword": "...",
  "sql": "..."
}
```

Never leak internal implementation details.

---

# 28. Design your error responses

A production API needs a consistent error format.

For example:

```json
{
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "An account with this email already exists.",
    "details": []
  }
}
```

Validation:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      {
        "field": "email",
        "code": "INVALID_EMAIL",
        "message": "Must be a valid email address."
      }
    ]
  }
}
```

This is far better than:

```json
{
  "error": "something went wrong"
}
```

---

# 29. Never make clients parse human-readable messages

Bad:

```javascript
if (error.message === "Email already exists") {
   ...
}
```

Why?

Because someone eventually changes:

```text
"Email already exists"
```

to:

```text
"This email is already registered"
```

and your frontend breaks.

Use stable machine-readable codes:

```json
{
  "code": "EMAIL_ALREADY_EXISTS"
}
```

Humans get:

```json
{
  "message": "An account with this email already exists."
}
```

---

# 30. Request body

For creating a product:

```http
POST /products
Content-Type: application/json
```

```json
{
  "name": "MacBook Pro",
  "price": 199999,
  "currency": "INR"
}
```

The server validates it.

---

# 31. Don't trust client input

Everything coming from the client is untrusted.

Even if the frontend validates:

```javascript
price > 0
```

the backend must validate it again.

Attackers can bypass your frontend completely.

Your backend should validate:

```text
type
required fields
length
range
format
relationships
authorization
business rules
```

---

# 32. Query parameters

Query parameters are excellent for collection operations.

Example:

```http
GET /products?category=electronics
```

Filtering:

```http
GET /products?status=active
```

Sorting:

```http
GET /products?sort=price
```

Descending:

```http
GET /products?sort=-price
```

Pagination:

```http
GET /products?page=2&limit=20
```

---

# 33. Filtering

Suppose:

```text
/products
```

You might support:

```http
GET /products?category=laptop
```

Multiple filters:

```http
GET /products?category=laptop&brand=apple
```

More complex filtering requires a deliberate syntax.

Don't accidentally create an SQL-like language in your query parameters.

---

# 34. Searching

Search is generally a query parameter:

```http
GET /products?q=macbook
```

or:

```http
GET /products?search=macbook
```

Pick one and document it.

Don't create:

```http
GET /searchProducts/macbook
```

unless search itself is modeled as a resource/operation for a particular reason.

---

# 35. Pagination

Never return 5 million records:

```http
GET /users
```

Production APIs need pagination.

Basic offset pagination:

```http
GET /users?page=1&limit=20
```

Response:

```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 4872
  }
}
```

---

# 36. Cursor pagination

For large/high-write systems, cursor pagination is often better.

Example:

```http
GET /users?limit=20&after=eyJpZCI6MTIzfQ
```

Response:

```json
{
  "data": [...],
  "pagination": {
    "next_cursor": "eyJpZCI6MTQzfQ"
  }
}
```

Why?

Offset pagination:

```text
page=100000
```

can become expensive and unstable when data changes.

Cursor pagination generally behaves better for large datasets and feeds.

---

# 37. Don't return unlimited collections

Bad:

```http
GET /users
```

with potentially:

```text
10,000,000 users
```

Always establish sensible limits.

For example:

```text
default = 20
maximum = 100
```

And enforce it server-side.

---

# 38. Sorting

Example:

```http
GET /products?sort=price
```

Descending:

```http
GET /products?sort=-price
```

Multiple:

```http
GET /products?sort=-created_at,name
```

But **whitelist sortable fields**.

Don't blindly turn:

```text
?sort=<anything>
```

into SQL.

---

# 39. Field selection

Sometimes clients don't need every field.

For example:

```http
GET /users?fields=id,name,email
```

This can reduce payload size.

But don't implement this prematurely.

It's useful in high-scale APIs, but adds complexity.

---

# 40. API versioning

Eventually your API changes.

Suppose:

```http
GET /v1/users/123
```

Then you need a breaking change.

You could introduce:

```http
GET /v2/users/123
```

Common strategies include:

### URL versioning

```text
/api/v1/users
/api/v2/users
```

### Header versioning

```http
Accept: application/vnd.company.v2+json
```

There isn't one universally correct choice.

For most teams, **URL versioning is simple and operationally clear**.

---

# 41. Don't version every tiny change

Adding a response field usually doesn't require:

```text
v2
```

For example:

v1:

```json
{
  "id": 1,
  "name": "Riyaz"
}
```

Later:

```json
{
  "id": 1,
  "name": "Riyaz",
  "avatar_url": "..."
}
```

That's generally backward compatible.

Breaking:

```text
name → full_name
```

could require a versioning strategy.

---

# 42. Authentication

A REST API usually needs authentication.

A common mechanism:

```http
Authorization: Bearer <access-token>
```

The server validates the token.

Important distinction:

```text
Authentication
    ↓
Who are you?

Authorization
    ↓
What are you allowed to do?
```

---

# 43. Don't put tokens in URLs

### ❌

```http
GET /users?token=secret123
```

URLs can appear in:

* logs
* browser history
* proxies
* monitoring systems
* analytics

Use:

```http
Authorization: Bearer ...
```

instead.

---

# 44. Authorization belongs on the server

Imagine:

```http
GET /users/123
```

A user changes:

```text
123 → 124
```

The backend must verify:

> Is this authenticated user allowed to access user 124?

Never assume:

> "The frontend won't show that button."

Frontend restrictions are not security.

---

# 45. Object-level authorization

This is one of the most dangerous API security problems.

Example:

```http
GET /orders/123
```

Attacker changes it:

```http
GET /orders/124
```

If your server simply returns order 124:

**security vulnerability.**

Your authorization must evaluate:

```text
current_user
+
requested_resource
+
requested_action
```

---

# 46. HTTPS

Production REST APIs should use HTTPS.

Not:

```http
http://api.example.com
```

but:

```http
https://api.example.com
```

HTTPS protects data in transit and is foundational for authentication and security.

---

# 47. Idempotency

Now we reach an important advanced concept.

An operation is **idempotent** if performing it multiple times has the same intended effect as performing it once.

For example:

```http
PUT /users/123
```

with:

```json
{
  "name": "Riyaz"
}
```

Sending it:

```text
once
```

or:

```text
five times
```

should leave the resource in the same state.

---

# 48. Why idempotency matters

Imagine payment processing.

Client sends:

```http
POST /payments
```

Server processes payment.

But response gets lost because of a network timeout.

Client thinks:

> "Maybe it didn't work."

It retries.

Now:

```text
₹10,000
```

might be charged twice.

That's disastrous.

---

# 49. Idempotency keys

Payment APIs commonly support:

```http
POST /payments
Idempotency-Key: 8c1e...
```

Request:

```json
{
  "amount": 10000,
  "currency": "INR"
}
```

If the client retries with the same key:

```http
Idempotency-Key: 8c1e...
```

the server recognizes:

> "I've already processed this request."

and returns the previous result rather than performing the operation again.

This is one of the most important production API patterns.

---

# 50. PUT vs POST and idempotency

Generally:

```text
GET     idempotent
PUT     idempotent
DELETE  idempotent
POST    not necessarily idempotent
PATCH   depends on operation
```

Don't interpret this as:

> "POST can never be idempotent."

A POST endpoint can absolutely implement idempotency.

---

# 51. Caching

HTTP has powerful caching mechanisms.

For example:

```http
GET /products/123
```

Response:

```http
Cache-Control: max-age=300
```

means clients/intermediaries can cache it for a period.

Caching can dramatically reduce:

```text
database load
network traffic
latency
server CPU
```

---

# 52. ETags

A powerful HTTP feature:

```http
ETag: "abc123"
```

Client later sends:

```http
If-None-Match: "abc123"
```

If nothing changed:

```http
304 Not Modified
```

No need to resend the entire representation.

---

# 53. Conditional updates

ETags can also help with concurrency.

Suppose:

User A retrieves:

```http
GET /products/123
```

ETag:

```text
"v10"
```

User B modifies product.

Now version becomes:

```text
"v11"
```

User A tries:

```http
PATCH /products/123
If-Match: "v10"
```

Server says:

```http
412 Precondition Failed
```

because the resource has changed.

This prevents **lost updates**.

---

# 54. Optimistic concurrency

This pattern is incredibly useful in production.

Without concurrency control:

```text
A reads version 10
B reads version 10

A writes version 11
B writes version 11
```

B accidentally overwrites A.

With optimistic locking:

```text
A reads v10
B reads v10

A updates if version = 10
→ success → v11

B updates if version = 10
→ fails
```

You can implement this with:

```text
ETag / If-Match
```

or explicit:

```text
version
```

fields.

---

# 55. Transactions

REST itself doesn't define database transactions.

Your API layer still needs to handle them correctly.

Example:

```http
POST /orders
```

might need to:

```text
create order
reserve inventory
create payment intent
```

You can't casually perform half of these and assume everything worked.

Your service needs a consistency strategy.

---

# 56. REST doesn't mean everything is synchronous

Suppose:

```http
POST /reports
```

generates a report that takes 30 minutes.

Don't hold the HTTP connection for 30 minutes.

Instead:

```http
POST /reports
```

Response:

```http
202 Accepted
```

```json
{
  "id": "report_123",
  "status": "processing"
}
```

Then:

```http
GET /reports/report_123
```

returns:

```json
{
  "id": "report_123",
  "status": "completed",
  "download_url": "..."
}
```

---

# 57. Webhooks

Sometimes your API isn't the only system involved.

Example:

```text
Your application
      |
      v
Payment provider
      |
      v
Payment completed
```

The payment provider can call your webhook:

```http
POST /webhooks/payments
```

Your webhook endpoint should be:

* authenticated
* signature-verified
* idempotent
* observable
* resilient to retries

Never assume webhook delivery happens exactly once.

---

# 58. Rate limiting

Production APIs should protect themselves.

Example:

```text
100 requests/minute/user
```

Response when exceeded:

```http
429 Too Many Requests
Retry-After: 60
```

Possible limits:

```text
per IP
per user
per API key
per tenant
per endpoint
```

Be careful with IP-only limits because many legitimate users can share an IP.

---

# 59. API security

A production REST API should consider:

### Authentication

Who are you?

### Authorization

What can you access?

### Input validation

Is this input valid?

### Output filtering

Are you returning fields the caller should see?

### Rate limiting

Are you abusing the service?

### Transport security

Is communication encrypted?

### Secrets

Are credentials protected?

### Logging

Are sensitive fields excluded?

### Injection

Are database/command queries safely parameterized?

---

# 60. Don't expose database models directly

Suppose your database contains:

```json
{
  "id": 123,
  "email": "...",
  "password_hash": "...",
  "internal_notes": "...",
  "stripe_customer_id": "...",
  "created_at": "..."
}
```

Never blindly serialize the database entity.

Instead create an API representation:

```json
{
  "id": "123",
  "email": "...",
  "created_at": "..."
}
```

This gives you a clean boundary:

```text
Database model
      ↓
Domain model
      ↓
API response DTO
```

The exact layers vary by architecture, but the principle is extremely valuable.

---

# 61. Don't expose internal IDs blindly

You may have:

```text
PostgreSQL integer ID = 1837291
```

Public APIs may instead use:

```text
usr_01J...
```

or UUIDs.

This can reduce enumeration risk and decouple public identifiers from internal storage.

But UUIDs aren't automatically "secure." Authorization is still mandatory.

---

# 62. API response consistency

Avoid APIs where every endpoint invents a completely different structure.

For example:

```json
{
  "users": [...]
}
```

then:

```json
{
  "results": [...]
}
```

then:

```json
{
  "items": [...]
}
```

Consistency matters.

A common pattern:

```json
{
  "data": [...]
}
```

with:

```json
{
  "meta": {...}
}
```

But don't blindly wrap every response if it doesn't provide value. The important principle is a predictable contract.

---

# 63. Don't over-engineer response wrappers

This:

```json
{
  "success": true,
  "status": 200,
  "message": "Success",
  "data": {
    "id": 123
  }
}
```

often duplicates HTTP semantics.

You already have:

```http
200 OK
```

You don't necessarily need:

```json
"success": true
```

Similarly, HTTP already gives you status codes.

Use your response body for useful application data.

---

# 64. Dates and times

This is a classic production problem.

Prefer unambiguous formats.

For example:

```text
2026-09-15T11:30:00Z
```

or an explicit offset:

```text
2026-09-15T17:00:00+05:30
```

Don't send:

```text
09/15/26 5:00 PM
```

because interpretation depends on locale/timezone.

---

# 65. Money

Don't represent money as floating-point numbers casually.

Bad:

```json
{
  "price": 19.99
}
```

Depending on language/database semantics, floating point can introduce precision issues.

Better approaches include:

```json
{
  "amount": 1999,
  "currency": "USD"
}
```

where amount is in minor units.

For INR:

```json
{
  "amount": 199999,
  "currency": "INR"
}
```

meaning ₹1,999.99.

Or use an appropriate decimal type throughout your system.

---

# 66. Boolean fields

Keep them predictable.

```json
{
  "is_active": true
}
```

or:

```json
{
  "active": true
}
```

Avoid bizarre semantics such as:

```json
{
  "status": "yes"
}
```

when a boolean is actually intended.

---

# 67. Enum evolution

Suppose:

```json
{
  "status": "pending"
}
```

Later you add:

```text
processing
completed
failed
```

Clients should ideally handle unknown enum values gracefully.

Never assume the server will never add another valid value.

This is important for forward compatibility.

---

# 68. Partial responses and backwards compatibility

A well-designed client should generally tolerate:

```text
new optional fields
```

being added.

But removing or renaming existing fields can break clients.

Think about API evolution as:

```text
Existing clients must continue working.
```

This is why API contracts are so important.

---

# 69. Database pagination trap

Suppose:

```http
GET /orders?page=2
```

You query:

```sql
OFFSET 20 LIMIT 20
```

Meanwhile new orders are being inserted.

Now page boundaries can shift.

Users may see:

```text
duplicates
```

or:

```text
missing records
```

Cursor pagination often solves this better for feeds and rapidly changing datasets.

---

# 70. API performance

A slow API isn't necessarily caused by HTTP.

Common problems:

```text
N+1 database queries
missing indexes
huge response payloads
slow downstream services
unbounded queries
unnecessary serialization
synchronous expensive work
```

Example N+1:

```text
GET /orders

1 query → orders

for every order:
    query customer
    query items
    query product
```

100 orders could result in:

```text
201+ queries
```

Fix with appropriate joins, batching, eager loading, data loaders, etc.

---

# 71. Timeouts

Never assume network calls complete quickly.

Your API calls:

```text
Service A
   ↓
Service B
   ↓
Service C
```

If C hangs, B hangs.

Then A hangs.

Eventually your entire system gets clogged.

Use explicit timeouts.

For example:

```text
connect timeout
read timeout
overall request timeout
```

The exact values depend on the operation.

---

# 72. Retries

Retries are dangerous.

Imagine:

```text
POST /payments
```

Client times out.

Retrying blindly may duplicate the payment.

Retries should be used with:

* idempotency
* exponential backoff
* jitter
* sensible retry limits
* knowledge of which failures are retryable

Don't retry every 500 blindly.

---

# 73. Exponential backoff

Instead of:

```text
retry immediately
retry immediately
retry immediately
```

use something like:

```text
1 sec
2 sec
4 sec
8 sec
...
```

with jitter.

This prevents many clients from retrying simultaneously and overwhelming a recovering service.

---

# 74. Observability

Production APIs need:

### Logs

What happened?

### Metrics

How often and how fast?

### Traces

Where did the request spend time?

A useful request correlation ID:

```http
X-Request-ID: 7f4...
```

or standardized tracing headers through your observability stack.

Then you can trace:

```text
API Gateway
   ↓
Order Service
   ↓
Inventory Service
   ↓
Database
```

for a single request.

---

# 75. Don't log secrets

Never log:

```text
Authorization headers
passwords
API keys
refresh tokens
credit card data
sensitive personal data
```

For example, don't log:

```text
POST /login
password=SuperSecret123
```

Even debug logging can end up in long-lived log systems.

---

# 76. API documentation

A production API needs a machine-readable contract.

The common standard is:

> **OpenAPI**

It describes:

```text
paths
methods
parameters
request bodies
responses
authentication
schemas
errors
```

Then tools can generate:

```text
documentation
client SDKs
server stubs
validation
tests
```

This is one of the best practices I'd recommend.

---

# 77. Contract-first vs code-first

Two common approaches.

### Code-first

Write implementation:

```text
controller
service
models
```

Then generate API documentation.

### Contract-first

Design OpenAPI contract first:

```text
GET /orders/{id}
```

Define request/response.

Then implement it.

Contract-first is particularly valuable when:

```text
frontend + backend teams
```

work independently.

---

# 78. Testing REST APIs

You need several levels.

### Unit tests

Test business logic.

### Integration tests

Test:

```text
API + database
```

### Contract tests

Verify API matches expected contract.

### End-to-end tests

Test complete user workflows.

Example:

```text
create account
→ login
→ create order
→ pay
→ retrieve order
```

Don't rely entirely on E2E tests—they're expensive and brittle.

---

# 79. Test error cases

A common mistake is testing only:

```text
200 OK
```

Production bugs often occur in:

```text
401
403
404
409
422
429
500
timeouts
duplicate requests
concurrent updates
```

Test them deliberately.

---

# 80. REST anti-patterns

Now let's talk about how **not** to design REST APIs.

---

## Anti-pattern #1

```http
GET /getUser?id=123
```

Better:

```http
GET /users/123
```

---

## Anti-pattern #2

```http
POST /deleteUser
```

Better:

```http
DELETE /users/123
```

---

## Anti-pattern #3

Everything returns 200.

Bad:

```http
HTTP 200
```

```json
{
  "success": false,
  "error": "User not found"
}
```

Better:

```http
404 Not Found
```

---

## Anti-pattern #4

Huge responses

```json
{
  "user": {
    "...": "...",
    "1000 fields": "..."
  }
}
```

Return only what clients need.

---

## Anti-pattern #5

Database schema exposed directly

Don't make your API:

```text
PostgreSQL → JSON serializer → Internet
```

You need a deliberate public contract.

---

## Anti-pattern #6

No authorization check on IDs

```http
GET /users/123
```

just because the user is authenticated.

Authentication isn't authorization.

---

## Anti-pattern #7

No pagination

```http
GET /users
```

returning millions of rows.

---

## Anti-pattern #8

No idempotency for financial operations

Potential:

```text
double charge
double order
double shipment
```

---

## Anti-pattern #9

Leaking internal errors

Bad:

```json
{
  "error": "SQLException: relation users does not exist at db.internal:5432"
}
```

Return a safe public error.

Log the detailed internal error privately.

---

## Anti-pattern #10

Business logic inside controllers

Bad architecture:

```text
Controller
  ├── validate
  ├── calculate price
  ├── query DB
  ├── call payment provider
  ├── send email
  ├── update inventory
  └── construct response
```

Controllers should generally coordinate rather than contain your entire business domain.

---

# 81. A better architecture

A common structure:

```text
HTTP Request
     |
     v
Controller / Handler
     |
     v
Application / Service Layer
     |
     v
Domain Logic
     |
     +----> Repository
     |
     +----> External Services
     |
     v
Database
```

For example:

```text
POST /orders
      |
      v
OrderController
      |
      v
CreateOrderService
      |
      +--> InventoryService
      |
      +--> PaymentService
      |
      +--> OrderRepository
```

The exact architecture can vary, but separating HTTP concerns from domain/business logic is highly valuable.

---

# 82. REST is not your architecture

This is important.

REST describes how clients interact with resources.

It doesn't tell you:

```text
whether to use PostgreSQL
whether to use Redis
whether to use Kafka
whether to use microservices
whether to use monolith
whether to use Node
whether to use Java
```

You can have:

```text
REST + monolith
REST + microservices
REST + serverless
REST + PostgreSQL
REST + MongoDB
```

REST is the **API architectural style**, not your entire system architecture.

---

# 83. REST vs GraphQL

REST:

```http
GET /users/123
GET /users/123/orders
GET /orders/456
```

GraphQL:

```graphql
query {
  user(id: "123") {
    name
    orders {
      id
      total
    }
  }
}
```

REST gives you HTTP semantics and resource-oriented endpoints.

GraphQL gives clients more control over what data they request.

Neither is universally superior.

---

# 84. REST vs gRPC

REST:

```text
HTTP/JSON
```

gRPC:

```text
HTTP/2
Protocol Buffers
RPC-style contracts
```

A common architecture is:

```text
Mobile/Web
    |
   REST
    |
API Gateway
    |
 gRPC/internal APIs
    |
Microservices
```

REST is often excellent at public boundaries.

gRPC can be excellent for internal service-to-service communication.

Again, not a universal rule.

---

# 85. REST maturity

There is a concept called the Richardson Maturity Model.

Roughly:

### Level 0

One endpoint, RPC-like behavior.

```http
POST /api
```

### Level 1

Resources.

```text
/users
/orders
/products
```

### Level 2

HTTP verbs + status codes.

```http
GET
POST
PUT
DELETE
```

and meaningful:

```text
200
201
404
409
```

### Level 3

Hypermedia / HATEOAS.

Responses include links describing available transitions.

Example:

```json
{
  "id": "123",
  "status": "pending",
  "links": {
    "self": "/orders/123",
    "cancel": "/orders/123/cancellation"
  }
}
```

Most production APIs today live around Levels 1–2 rather than fully embracing Level 3.

---

# 86. REST constraints

The original REST architectural style has several constraints.

The major ones are:

### Client-server

Client and server have separate responsibilities.

### Stateless

Each request should contain the information necessary to process it.

### Cacheable

Responses can indicate whether they can be cached.

### Uniform interface

Use consistent resource representations and HTTP semantics.

### Layered system

Client doesn't necessarily know whether it's talking directly to the application server.

There can be:

```text
Client
 ↓
CDN
 ↓
Load Balancer
 ↓
API Gateway
 ↓
Service
 ↓
Database
```

### Code-on-demand

Optional.

Servers can theoretically send executable code to clients.

This isn't central to modern REST API development.

---

# 87. What "stateless" actually means

This is often misunderstood.

Stateless doesn't mean:

> "The server can't store anything."

Of course the server stores:

```text
users
orders
sessions
payments
```

Stateless means the server doesn't rely on hidden conversational state from a previous HTTP request to understand the next one.

For example, ideally:

```http
GET /orders/123
Authorization: Bearer ...
```

contains enough context for authorization and processing.

You don't want:

```text
Request 1 establishes mysterious server state
Request 2 assumes it
Request 3 assumes something else
```

---

# 88. Stateless doesn't necessarily mean JWT

Another common misconception:

> REST = JWT.

No.

You can build a REST API using:

```text
session cookies
opaque tokens
JWTs
OAuth
mTLS
API keys
```

depending on your use case.

REST's statelessness and your authentication/session mechanism are related design concerns, but they are not synonymous.

---

# 89. A production-grade endpoint

Let's design:

```http
POST /api/v1/orders
```

Request:

```http
Authorization: Bearer ...
Content-Type: application/json
Idempotency-Key: 7c1f...
```

```json
{
  "items": [
    {
      "product_id": "prod_123",
      "quantity": 2
    }
  ],
  "shipping_address_id": "addr_456"
}
```

Server performs:

```text
authenticate
      ↓
authorize
      ↓
validate request
      ↓
check idempotency key
      ↓
validate products
      ↓
check inventory
      ↓
calculate price
      ↓
create order transactionally
      ↓
return response
```

Response:

```http
201 Created
Location: /api/v1/orders/ord_789
```

```json
{
  "data": {
    "id": "ord_789",
    "status": "pending",
    "currency": "INR",
    "total": 399800,
    "items": [
      {
        "product_id": "prod_123",
        "quantity": 2,
        "unit_price": 199900
      }
    ],
    "created_at": "2026-09-15T11:45:00Z"
  }
}
```

That's a real API contract.

---

# 90. What happens internally?

The request travels roughly:

```text
                    INTERNET
                       |
                       v
                 ┌───────────┐
                 │   CDN /   │
                 │ WAF       │
                 └─────┬─────┘
                       |
                       v
                 ┌───────────┐
                 │ Load      │
                 │ Balancer  │
                 └─────┬─────┘
                       |
                       v
                 ┌───────────┐
                 │ API       │
                 │ Gateway   │
                 └─────┬─────┘
                       |
                       v
                 ┌───────────┐
                 │ Order     │
                 │ Service   │
                 └─────┬─────┘
                       |
            ┌──────────┼───────────┐
            v          v           v
        PostgreSQL   Redis     Inventory
                                  |
                                  v
                               Payment
```

REST is primarily the contract at the HTTP boundary.

Everything underneath can be much more complicated.

---

# 91. Production API checklist

When designing an endpoint, ask:

### Resource

* What resource does this represent?
* Is the URL intuitive?
* Is the URL noun-oriented?

### HTTP

* Is the method semantically appropriate?
* Is it safe/idempotent where appropriate?
* Are status codes meaningful?

### Input

* Is validation strict?
* Are unknown fields handled intentionally?
* Are limits enforced?

### Authentication

* Who is calling?

### Authorization

* Are they allowed to perform this operation on this resource?

### Errors

* Are errors predictable?
* Are machine-readable error codes provided?
* Are sensitive internals hidden?

### Performance

* Is pagination implemented?
* Are database queries indexed?
* Are response payloads reasonable?

### Reliability

* What happens if the client retries?
* Do you need an idempotency key?
* What happens if a downstream service times out?

### Concurrency

* What happens if two clients update the resource simultaneously?

### Security

* HTTPS?
* Rate limiting?
* Input validation?
* Object-level authorization?
* Secret handling?

### Observability

* Logs?
* Metrics?
* Traces?
* Request correlation?

### Evolution

* Can you add fields without breaking clients?
* What's the versioning strategy?

### Documentation

* OpenAPI?
* Examples?
* Error contracts?

---

# 92. The REST mental model I want you to remember

When designing an API, think in this order:

```text
                    RESOURCE
                       |
                       v
                  /orders/123
                       |
                       v
                  HTTP METHOD
                       |
                       v
                    PATCH
                       |
                       v
                 REPRESENTATION
                       |
                       v
              {"status":"paid"}
                       |
                       v
                  HTTP STATUS
                       |
                       v
                    200 OK
```

And around that:

```text
Authentication
Authorization
Validation
Concurrency
Idempotency
Caching
Rate limiting
Observability
Security
Versioning
```

That's production REST.

---

# 93. The most important rules

If you remember only these, remember these:

### 1. URLs identify resources

```text
/users/123
/orders/456
```

### 2. HTTP methods express operations

```text
GET
POST
PUT
PATCH
DELETE
```

### 3. Use HTTP status codes correctly

```text
201 ≠ 200
404 ≠ 200
401 ≠ 403
409 ≠ 422
```

### 4. Never trust client input

Validate everything server-side.

### 5. Authentication ≠ authorization

Knowing who someone is doesn't mean they can access everything.

### 6. Design for retries

Especially payments, orders, and other side effects.

### 7. Make important operations idempotent

Use idempotency keys where necessary.

### 8. Paginate collections

Never casually return unlimited data.

### 9. Don't expose database models

Create a deliberate API contract.

### 10. Design for evolution

APIs live for years.

### 11. Make failures explicit

Timeouts, retries, conflicts, validation failures, rate limits—all need deliberate behavior.

### 12. Production APIs are distributed systems

The HTTP endpoint is only the beginning.

---

# 94. Your learning path from here

If you genuinely want to become **very strong at REST**, I recommend we don't try to memorize everything at once.

We should build a single API together.

For example:

```text
E-Commerce REST API
```

and progressively evolve it:

### Stage 1 — Beginner

```text
GET /products
GET /products/{id}
POST /products
PATCH /products/{id}
DELETE /products/{id}
```

Learn:

```text
HTTP
URLs
methods
JSON
status codes
CRUD
```

### Stage 2 — Intermediate

Add:

```text
validation
errors
pagination
filtering
sorting
search
authentication
authorization
```

### Stage 3 — Advanced

Add:

```text
idempotency
ETags
optimistic locking
transactions
caching
rate limiting
webhooks
async jobs
```

### Stage 4 — Production

Add:

```text
OpenAPI
logging
metrics
tracing
timeouts
retries
circuit breakers
security
load testing
API gateways
CDNs
database performance
```

### Stage 5 — Architecture

Then we can examine:

```text
REST vs GraphQL
REST vs gRPC
monolith vs microservices
API gateway patterns
event-driven architecture
distributed transactions
sagas
eventual consistency
CQRS
HATEOAS
API versioning
backward compatibility
multi-tenancy
```

At that point you'll understand **not merely how to write REST endpoints, but how to design APIs that survive real production systems**.

If you want, I can take you through this as a **hands-on REST masterclass**, starting with **Lesson 1: HTTP from the wire level**, using `curl` and a small backend, and progressively build the production API with you.

