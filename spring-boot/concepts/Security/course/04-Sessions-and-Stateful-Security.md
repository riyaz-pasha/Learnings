# Chapter 4 — Sessions & Stateful Security ⭐⭐⭐

Now we know:

```text
Authentication
    ↓
Who is the user?

Authorization
    ↓
What can the user do?
```

But there's an important question:

> **After a user successfully logs in, how does Spring Security remember that user on the next HTTP request?**

The answer, in traditional web applications, is **HTTP session-based authentication**.

---

# 1. The Problem: HTTP is Stateless

HTTP itself is stateless.

Imagine:

```http
GET /profile
```

The server receives the request.

Then later:

```http
GET /orders
```

The server doesn't inherently know:

> "This is the same user who requested `/profile`."

Each HTTP request is independent.

```text
Request 1
    ↓
Server
    ↓
Response

Request 2
    ↓
Server
    ↓
Response
```

So we need a mechanism to maintain the user's authenticated state.

---

# 2. Session-Based Authentication

A traditional Spring application can use an HTTP session.

Conceptually:

```text
                 Login
                   |
                   ↓
             Authentication
                   |
                   ↓
            SecurityContext
                   |
                   ↓
             HTTP Session
                   |
                   ↓
              JSESSIONID
                   |
                   ↓
                Browser
```

The browser then sends the session identifier with subsequent requests.

---

# 3. What is `JSESSIONID`?

`JSESSIONID` is commonly the cookie used by a Java web application to identify an HTTP session.

After authentication, the server might send:

```http
Set-Cookie: JSESSIONID=ABC123...
```

The browser stores it.

Then subsequent requests contain:

```http
Cookie: JSESSIONID=ABC123...
```

The server uses that ID to find the corresponding session.

---

# 4. The Complete Flow ⭐⭐⭐

Suppose the user logs in.

```text
Browser
   |
   | Login credentials
   ↓
Spring Security
   |
   ↓
Authentication
   |
   ↓
Success
   |
   ↓
SecurityContext
   |
   ↓
HTTP Session
   |
   ↓
JSESSIONID
   |
   ↓
Browser
```

Later:

```http
GET /profile
Cookie: JSESSIONID=ABC123
```

The server can associate:

```text
JSESSIONID=ABC123
        ↓
HTTP Session
        ↓
SecurityContext
        ↓
Authentication
        ↓
User = Riyaz
```

So the application knows who the user is without asking for the username/password again.

---

# 5. Why Doesn't the Browser Send the Password Every Time?

Because that would be terrible design.

We don't want:

```text
Request 1:
username + password

Request 2:
username + password

Request 3:
username + password
```

Instead:

```text
Login
  ↓
Authenticate once
  ↓
Create session
  ↓
Set session cookie
```

Then:

```text
Request 1 → JSESSIONID
Request 2 → JSESSIONID
Request 3 → JSESSIONID
```

The session represents the authenticated state.

---

# 6. Where Does Authentication Live?

Conceptually:

```text
HTTP Session
     |
     ↓
SecurityContext
     |
     ↓
Authentication
     |
     ├── Principal
     └── Authorities
```

So:

```text
JSESSIONID
    ↓
Session
    ↓
SecurityContext
    ↓
Authentication
    ↓
Current User
```

This relationship is extremely important.

---

# 7. Example

Suppose:

```text
User:
    riyaz

Authorities:
    ROLE_USER
```

After login:

```text
Session ID:
    ABC123
```

Conceptually:

```text
Session ABC123
      |
      ↓
SecurityContext
      |
      ↓
Authentication
      |
      ├── Principal = riyaz
      └── Authority = ROLE_USER
```

Then:

```http
GET /profile
Cookie: JSESSIONID=ABC123
```

Spring Security can restore the authentication.

---

# 8. Spring Security's Role

Spring Security handles much of this automatically when using stateful authentication.

A simplified flow:

```text
Request
   ↓
SecurityFilterChain
   ↓
Load security context
   ↓
Find existing Authentication
   ↓
Authorization
   ↓
Controller
```

You don't manually do:

```java
session.getAttribute(...)
```

for normal Spring Security authentication.

Spring Security manages the security context lifecycle.

---

# 9. Session-Based Authentication Example

Let's modify our earlier application.

Instead of focusing on HTTP Basic, suppose we use form login:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin();

    return http.build();
}
```

Now Spring Security provides a login page.

Conceptually:

```text
GET /login
    ↓
Login page
    ↓
Username + Password
    ↓
Authentication
    ↓
Success
    ↓
Session created
```

---

# 10. What Happens After Login?

Suppose:

```text
username = riyaz
password = secret123
```

Authentication succeeds.

Spring Security establishes the authenticated state.

The browser receives a session cookie:

```http
Set-Cookie: JSESSIONID=...
```

Then:

```http
GET /profile
Cookie: JSESSIONID=...
```

The application knows:

```text
User = riyaz
```

---

# 11. Why is This Called Stateful?

Because the server maintains state about the client.

```text
Client
  |
  | JSESSIONID
  ↓
Server
  |
  └── Session state
        |
        └── Authentication
```

The server needs to remember:

```text
JSESSIONID ABC123
       ↓
User Riyaz
       ↓
ROLE_USER
```

Therefore:

> **Session-based authentication is stateful because the server maintains authentication state between requests.**

---

# 12. Stateful vs Stateless ⭐⭐⭐

This is a very common interview question.

### Stateful

```text
Client
   |
   | Session ID
   ↓
Server
   |
   ↓
Server-side session
   |
   ↓
Authentication
```

The server stores session state.

### Stateless

```text
Client
   |
   | Access Token
   ↓
Server
   |
   ↓
Validate token
   |
   ↓
Authentication
```

The server doesn't need to maintain a traditional login session for the client.

JWT commonly uses this model.

We'll cover it in Chapter 5.

---

# 13. Stateful Example

Suppose 1 million users are logged in.

Conceptually:

```text
Server
│
├── Session A → User A
├── Session B → User B
├── Session C → User C
├── ...
└── Session N → User N
```

The server/session infrastructure maintains those sessions.

With multiple application instances:

```text
                Load Balancer
                     |
          ┌──────────┴──────────┐
          ↓                     ↓
       Server 1              Server 2
          |                     |
       Sessions              Sessions
```

Now we have a distributed session problem.

We'll discuss why this matters shortly.

---

# 14. What Happens in a Multi-Server Environment?

Suppose:

```text
Request 1
   ↓
Load Balancer
   ↓
Server A
   ↓
Session created
```

Then:

```text
Request 2
   ↓
Load Balancer
   ↓
Server B
```

Server B might not know about the session created on Server A.

This creates a problem.

```text
Server A
   ↓
Session ABC
   ↓
User = Riyaz

Server B
   ↓
"I don't know Session ABC"
```

---

# 15. Sticky Sessions

One solution is **sticky sessions**.

The load balancer tries to send the same client to the same server:

```text
Riyaz
  |
  ↓
Load Balancer
  |
  └────→ Server A
            |
            └── Session ABC
```

Future requests:

```text
Riyaz
  |
  ↓
Load Balancer
  |
  └────→ Server A
```

This can work, but it has drawbacks.

If Server A goes down:

```text
Server A
   ↓
💥
```

the session may be lost unless session state is replicated elsewhere.

---

# 16. Distributed Session

Another solution is to store sessions in shared infrastructure.

For example:

```text
              Load Balancer
                    |
          ┌─────────┴─────────┐
          ↓                   ↓
       Server A            Server B
          |                   |
          └─────────┬─────────┘
                    ↓
             Shared Session Store
                    |
                  Redis
```

Now:

```text
Server A ──┐
           ↓
         Redis
           ↑
           |
Server B ──┘
```

Both servers can access the same session.

This is one reason distributed session management is relevant in larger applications.

---

# 17. Session Fixation ⭐⭐⭐

This is an important Spring Security security concept.

Imagine an attacker somehow knows a session ID:

```text
JSESSIONID=ATTACKER_KNOWS_THIS
```

Then the victim logs in using that session.

If the session ID remains unchanged:

```text
Before login:
ABC123

After login:
ABC123
```

the attacker may potentially exploit the known session identifier.

This is called **session fixation**.

---

# 18. Session Fixation Protection

The basic defense is:

> **Change the session identifier after successful authentication.**

Conceptually:

```text
Before authentication:

JSESSIONID = ABC123

       ↓
     LOGIN

       ↓

After authentication:

JSESSIONID = XYZ789
```

The attacker who knew:

```text
ABC123
```

can no longer use it to represent the authenticated session.

Spring Security provides session fixation protection as part of its session management support.

---

# 19. Session Management Configuration

You can configure session behavior:

```java
http
    .sessionManagement(session -> session
        .sessionFixation(fixation ->
            fixation.changeSessionId()
        )
    );
```

The exact strategy can vary, but the important interview concept is:

```text
Authentication
      ↓
Session ID should not remain attacker-controlled
      ↓
Session fixation protection
```

---

# 20. Session Timeout

Sessions shouldn't necessarily live forever.

For example:

```properties
server.servlet.session.timeout=30m
```

means the session can expire after inactivity according to the configured timeout.

Conceptually:

```text
Login
  ↓
Session created
  ↓
30 minutes inactive
  ↓
Session expires
  ↓
Authentication no longer available
```

The user must authenticate again.

---

# 21. Logout

What should happen when the user logs out?

Conceptually:

```text
POST /logout
      ↓
Invalidate authentication/session
      ↓
Session removed/invalidated
      ↓
User is no longer authenticated
```

Spring Security can handle logout.

For example:

```java
http
    .logout(logout -> logout
        .logoutSuccessUrl("/login?logout")
    );
```

The important concept is:

> **Logout terminates the authenticated session/security context so subsequent requests are no longer authenticated.**

---

# 22. Concurrent Sessions

Suppose the same account logs in from:

```text
Laptop
Phone
Tablet
```

You may want to limit how many sessions can exist simultaneously.

For example:

```text
Maximum sessions = 1
```

Then:

```text
Laptop
   ↓
Session A
   ↓
ACTIVE

Phone logs in
   ↓
Session B
   ↓
Session A may be invalidated
```

This is called **concurrent session control**.

It can be useful for applications with requirements such as:

> Only one active login per user.

---

# 23. SecurityContext and Sessions

This relationship is worth memorizing:

```text
             HTTP Session
                   |
                   ↓
           SecurityContext
                   |
                   ↓
            Authentication
                   |
          ┌────────┴────────┐
          ↓                 ↓
      Principal        Authorities
          ↓                 ↓
       Riyaz             ROLE_USER
```

But be careful:

> Don't think `SecurityContext` is inherently the same thing as an HTTP session.

`SecurityContext` is a Spring Security abstraction for holding authentication.

It can be persisted using different mechanisms depending on the application architecture.

---

# 24. Stateful Authentication Flow ⭐⭐⭐

Let's put everything together.

```text
             LOGIN
               |
               ↓
      Username + Password
               |
               ↓
      AuthenticationManager
               |
               ↓
      AuthenticationProvider
               |
               ↓
        UserDetailsService
               |
               ↓
         PasswordEncoder
               |
               ↓
        Authentication
               |
               ↓
        SecurityContext
               |
               ↓
          HTTP Session
               |
               ↓
          JSESSIONID
               |
               ↓
            Browser
```

Next request:

```text
Browser
   |
   | Cookie: JSESSIONID=...
   ↓
SecurityFilterChain
   |
   ↓
Load SecurityContext
   |
   ↓
Authentication restored
   |
   ↓
Authorization
   |
   ↓
Controller
```

---

# 25. Stateful vs Stateless — Interview Comparison ⭐⭐⭐

|                           | Stateful                  | Stateless                                  |
| ------------------------- | ------------------------- | ------------------------------------------ |
| Common mechanism          | Session                   | JWT/access token                           |
| Server stores login state | Yes                       | Usually no traditional session             |
| Client sends              | Session ID                | Access token                               |
| Common cookie             | `JSESSIONID`              | Often token in cookie/header               |
| Scaling                   | Requires session strategy | Easier horizontally                        |
| Logout                    | Invalidate session        | Token lifecycle/revocation strategy needed |
| Server-side session       | Yes                       | No traditional session                     |
| Typical use               | Traditional web apps      | APIs/microservices                         |

Important:

> **Stateless does not automatically mean JWT.**

JWT is one way to implement stateless authentication, but statelessness is the broader concept.

---

# 26. Does JWT Make an Application More Secure?

Not automatically.

This is an important interview point.

JWT solves a different architectural problem:

```text
How can we represent authentication information
without maintaining traditional server-side session state?
```

It does **not** automatically make authentication more secure.

You still need to handle:

* Token theft
* Token expiration
* Refresh tokens
* HTTPS
* XSS
* CSRF depending on token transport
* Revocation
* Secure token storage

We'll cover this in the next chapter.

---

# 27. Common Interview Questions

### Q1. What is a session?

> A session is server-side state associated with a client, typically identified by a session ID such as `JSESSIONID`. It can be used to maintain authentication state across multiple HTTP requests.

---

### Q2. What is `JSESSIONID`?

> It is commonly the identifier of an HTTP session in Java web applications. The client sends it with subsequent requests so the server can locate the associated session.

---

### Q3. Why is session-based authentication stateful?

> Because the server maintains authentication-related state associated with the client's session between requests.

---

### Q4. What is session fixation?

> Session fixation is an attack where an attacker attempts to make a victim use a session ID known to the attacker, and then takes advantage of that session after the victim authenticates.

---

### Q5. How does Spring Security prevent session fixation?

> Spring Security can change the session identifier when authentication succeeds, preventing an attacker from continuing to use a previously known session ID.

---

### Q6. What is the difference between stateful and stateless authentication?

> Stateful authentication stores authentication state on the server, typically in a session, while stateless authentication carries the information needed to authenticate the request, commonly through an access token, so the server doesn't need a traditional session for each client.

---

### Q7. What is sticky session?

> Sticky session is a load-balancer technique that routes subsequent requests from a client to the same application instance that holds that client's session.

---

# 28. Interview Scenario

### Question:

> Your Spring Boot application works with one server but authentication randomly disappears when deployed behind a load balancer with three instances. Why?

Think:

```text
                 Load Balancer
                 /     |     \
                /      |      \
           Server A Server B Server C
```

User logs in:

```text
Login
 ↓
Server A
 ↓
Session created
```

Next request:

```text
Request
 ↓
Load Balancer
 ↓
Server B
```

Server B doesn't have the session.

Possible solutions include:

```text
1. Sticky sessions
2. Shared/distributed session store
3. Move to stateless authentication where appropriate
```

That's a very realistic system-design/interview question.

---

# 29. One Important Distinction

Don't say:

> "The JSESSIONID contains the username and password."

Usually, it does **not**.

Think of it as:

```text
JSESSIONID
    ↓
Identifier
    ↓
Server-side session
    ↓
SecurityContext
    ↓
Authentication
```

The session ID is essentially a reference to server-side state.

---

# 30. Chapter 4 Cheat Sheet

```text
HTTP is stateless
       ↓
Need to maintain login state
       ↓
HTTP Session
       ↓
JSESSIONID
       ↓
SecurityContext
       ↓
Authentication
       ↓
Current user
```

### Stateful

```text
Client
  |
  | JSESSIONID
  ↓
Server
  |
  ↓
Session
  |
  ↓
Authentication
```

### Stateless

```text
Client
  |
  | Access Token
  ↓
Server
  |
  ↓
Validate Token
  |
  ↓
Authentication
```

### Security concepts to remember

```text
JSESSIONID
Session fixation
Session timeout
Logout
Concurrent sessions
Sticky sessions
Distributed sessions
Stateful vs stateless
```

---

## ⭐ The key interview answer

If asked:

> **"How does Spring Security maintain authentication across multiple requests?"**

A good answer is:

> In a traditional stateful application, after successful authentication Spring Security associates the authenticated `Authentication` with the `SecurityContext`, which can be persisted in the HTTP session. The client receives a session identifier such as `JSESSIONID` and sends it on subsequent requests. Spring Security uses that session to restore the security context and identify the authenticated user for each request.

---

### Next — Chapter 5: JWT & Stateless Security ⭐⭐⭐

We'll now take the exact problem we just solved with sessions and ask:

> **What if we don't want the server to maintain a session for every logged-in client?**

We'll build the flow:

```text
Login
  ↓
Authentication
  ↓
Access Token / JWT
  ↓
Client
  ↓
Authorization: Bearer <token>
  ↓
JWT validation
  ↓
Authentication
  ↓
SecurityContext
  ↓
Authorization
```

We'll also cover **access token vs refresh token, `OncePerRequestFilter`, JWT validation, `STATELESS`, and common JWT mistakes**.

