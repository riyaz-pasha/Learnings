# Chapter 5 — JWT & Stateless Security

This is one of the most important Spring Security interview topics.

The key question is:

> **How can we authenticate a user without storing their login session on the server?**

The answer is commonly **token-based authentication**, especially **JWT access tokens**.

---

# 1. The problem with traditional sessions

In Chapter 4, we saw:

```text
Client
  |
  | Login: username + password
  ↓
Spring Security
  |
  | creates Authentication
  ↓
HTTP Session
  |
  | JSESSIONID
  ↓
Client
```

For every subsequent request:

```text
GET /profile
Cookie: JSESSIONID=abc123
```

The server uses `abc123` to find the session.

So the server maintains state:

```text
Server
 ├── Session abc123 → User Riyaz
 ├── Session xyz456 → User John
 └── Session pqr789 → User Alice
```

This can become inconvenient in distributed systems.

Imagine:

```text
                 Load Balancer
                /      |      \
               ↓       ↓       ↓
             App A   App B   App C
```

User logs in through App A.

Session exists on App A.

Then the next request goes to App B.

```text
Request 1 → App A → session exists ✅

Request 2 → App B → session doesn't exist ❌
```

We can solve this with distributed sessions such as Redis, but another approach is:

> **Don't store the authentication session on the server.**

This leads to stateless authentication.

---

# 2. What does stateless authentication mean?

Instead of:

```text
Client
   ↓
JSESSIONID
   ↓
Server-side session
   ↓
User identity
```

we do:

```text
Client
   ↓
Access Token
   ↓
Server validates token
   ↓
User identity
```

The client sends the token with every request:

```http
GET /profile
Authorization: Bearer eyJhbGciOi...
```

The server validates the token and determines:

```text
User = Riyaz
Authorities = ROLE_USER
```

No traditional HTTP session is required.

---

# 3. What is JWT?

JWT stands for:

> **JSON Web Token**

A JWT is a compact token containing claims about a subject.

A typical JWT looks like:

```text
xxxxx.yyyyy.zzzzz
```

It has three parts:

```text
HEADER.PAYLOAD.SIGNATURE
```

For example:

```text
eyJhbGciOiJIUzI1NiJ9
.
eyJzdWIiOiJyaXlheiIsInJvbGUiOiJVU0VSIn0
.
abc123signature
```

---

# 4. JWT Header

The header contains metadata about the token.

Example:

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

Meaning:

```text
alg = signing algorithm
typ = token type
```

For example:

```text
HS256
RS256
ES256
```

---

# 5. JWT Payload

The payload contains **claims**.

Example:

```json
{
  "sub": "riyaz",
  "roles": ["USER"],
  "iat": 1760000000,
  "exp": 1760003600
}
```

Common claims:

| Claim | Meaning                 |
| ----- | ----------------------- |
| `sub` | Subject / user identity |
| `iss` | Issuer                  |
| `aud` | Audience                |
| `iat` | Issued-at time          |
| `exp` | Expiration time         |
| `nbf` | Not valid before        |

You can also have application-specific claims:

```json
{
  "sub": "riyaz",
  "roles": ["USER"],
  "department": "engineering"
}
```

---

# 6. Important: JWT payload is NOT encrypted

This is a very common interview question.

JWT is usually:

> **Signed, not encrypted.**

Anyone who possesses the token can decode the header and payload.

For example:

```text
JWT
 ↓
Base64URL decode
 ↓
Header + Payload
```

Therefore:

**Never put sensitive information inside a JWT unless you are deliberately using an encrypted JWT/JWE design.**

For normal signed JWTs, don't put things like:

```text
password
credit card number
private secrets
```

inside the payload.

---

# 7. Then what makes JWT trustworthy?

The **signature**.

Suppose the server creates:

```text
Header
+
Payload
+
Secret/private key
        ↓
    Signature
```

The resulting token is:

```text
HEADER.PAYLOAD.SIGNATURE
```

Later the server receives it.

It can verify:

```text
Is this signature valid?
        ↓
      YES
        ↓
Was the token modified?
        ↓
      NO
```

If somebody modifies:

```json
{
  "roles": ["ADMIN"]
}
```

the signature will no longer match.

Therefore:

```text
Original:

USER → valid signature ✅


Modified:

ADMIN → invalid signature ❌
```

---

# 8. JWT authentication flow

The overall flow looks like this:

```text
             LOGIN
               |
               ↓
       username + password
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
       Password verification
               |
               ↓
        Authentication SUCCESS
               |
               ↓
        Create JWT access token
               |
               ↓
            Client
```

The client then stores the token.

For subsequent requests:

```text
Client
   |
   | Authorization: Bearer JWT
   ↓
Spring Security
   |
   | Validate JWT
   ↓
Extract claims
   |
   ↓
Create Authentication
   |
   ↓
SecurityContext
   |
   ↓
Authorization
   |
   ↓
Controller
```

Notice something important:

> JWT authentication still ultimately produces an `Authentication` object and places it in the `SecurityContext`.

This connects JWT with everything we learned earlier.

---

# 9. Where does the JWT go?

Usually in the HTTP `Authorization` header:

```http
Authorization: Bearer <access-token>
```

For example:

```http
GET /api/profile HTTP/1.1
Host: example.com
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`Bearer` means:

> Whoever possesses this token can present it as the bearer credential.

That's why protecting the token is extremely important.

---

# 10. JWT vs Session

This is a very important interview comparison.

| Session                              | JWT                                            |
| ------------------------------------ | ---------------------------------------------- |
| Server maintains session state       | Authentication information is carried in token |
| Client usually sends `JSESSIONID`    | Client sends access token                      |
| Session ID references server state   | Token contains claims                          |
| Stateful                             | Commonly stateless                             |
| Server can easily invalidate session | Immediate revocation is more complicated       |
| Distributed session may be needed    | Easier horizontal scaling                      |
| Common for traditional web apps      | Common for APIs/microservices                  |

But remember:

> **JWT ≠ automatically stateless.**

You could technically store JWT-related state on the server.

And:

> **Stateless ≠ JWT.**

You can build stateless authentication using other token mechanisms.

---

# 11. Spring Security configuration

For production-style Spring Security applications, you generally **should not manually write a JWT parser/filter unless you actually need custom behavior**.

Spring Security already provides OAuth2 Resource Server support.

Add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Then configure:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
```

The important parts are:

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

and:

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(Customizer.withDefaults())
)
```

---

# 12. What does `STATELESS` actually do?

This is important.

```java
SessionCreationPolicy.STATELESS
```

basically tells Spring Security:

> Don't use an HTTP session to store authentication.

Instead:

```text
Request
  ↓
Bearer JWT
  ↓
Validate JWT
  ↓
Create Authentication
  ↓
SecurityContext
  ↓
Authorization
```

The authentication is reconstructed from the token for each request.

---

# 13. What happens internally?

Suppose:

```http
GET /profile
Authorization: Bearer eyJ...
```

Spring Security's resource-server infrastructure detects the bearer token.

Conceptually:

```text
HTTP Request
     |
     ↓
Bearer Token processing
     |
     ↓
Extract JWT
     |
     ↓
JwtDecoder
     |
     ↓
Validate signature
     |
     ↓
Validate claims
     |
     ↓
Create Authentication
     |
     ↓
SecurityContext
     |
     ↓
Authorization
     |
     ↓
Controller
```

A key component here is:

```text
JwtDecoder
```

Its job is essentially to decode/verify the JWT and produce a validated JWT representation.

---

# 14. JWT validation is more than signature validation

This is an important interview point.

The server should validate things such as:

### 1. Signature

Was the token signed by a trusted key?

```text
Signature valid?
```

### 2. Expiration

```text
exp > current time?
```

If expired:

```text
401 Unauthorized
```

### 3. Issuer

```text
iss == expected issuer?
```

### 4. Audience

```text
aud contains expected API?
```

Depending on your security configuration, other claims such as `nbf` can also matter.

So:

```text
JWT validation
    ├── Signature
    ├── Expiration
    ├── Issuer
    ├── Audience
    └── Other configured validations
```

---

# 15. Where does the signing key come from?

There are two common approaches.

## Symmetric signing

Example:

```text
HS256
```

Same secret is used to sign and verify.

```text
Issuer
   |
   | secret
   ↓
Sign JWT


API Server
   |
   | same secret
   ↓
Verify JWT
```

The problem:

> Both sides must possess the same secret.

---

## Asymmetric signing

Example:

```text
RS256
```

There are two keys:

```text
Private Key
    ↓
Sign


Public Key
    ↓
Verify
```

So:

```text
Authorization Server
        |
        | private key
        ↓
     Sign JWT

Resource Server
        |
        | public key
        ↓
    Verify JWT
```

This is very useful in distributed systems.

The resource server doesn't need the private signing key.

---

# 16. Real-world architecture

A very common architecture looks like:

```text
                Authentication
                  Server
                    |
                    | Access Token
                    ↓
                Client/App
                    |
                    | Bearer JWT
                    ↓
             ┌───────────────┐
             │ API Gateway   │
             └───────┬───────┘
                     |
          ┌──────────┼──────────┐
          ↓          ↓          ↓
       Service A  Service B  Service C
```

The authentication server might issue the token.

The APIs act as **resource servers**.

They validate the token.

---

# 17. JWT access token vs refresh token

This is another extremely common interview question.

Suppose:

```text
Access Token lifetime = 15 minutes
```

After 15 minutes:

```text
Access Token
     ↓
Expired
```

You don't want the user to enter their password every 15 minutes.

That's where a refresh token can be used.

Conceptually:

```text
Login
  ↓
Access Token + Refresh Token
  ↓
Client
```

Access token:

```text
short-lived
used for APIs
```

Refresh token:

```text
longer-lived
used to obtain a new access token
```

Flow:

```text
Client
  |
  | Access Token
  ↓
API
  |
  X expired
  |
  ↓
Client
  |
  | Refresh Token
  ↓
Authorization Server
  |
  ↓
New Access Token
```

Important distinction:

> A refresh token is generally **not sent to every API**.

It is normally presented to the authorization server/token endpoint.

---

# 18. Why make access tokens short-lived?

Because a bearer token is effectively a credential.

If someone steals:

```text
eyJhbGciOi...
```

they may be able to use it until it expires.

Short lifetime reduces the attack window:

```text
15-minute token
        ↓
Stolen
        ↓
Limited useful lifetime
```

But this creates the need for refresh tokens.

---

# 19. What happens to SecurityContext?

This is a subtle but important concept.

With session authentication:

```text
Request
 ↓
Session
 ↓
SecurityContext
 ↓
Authentication
```

With JWT:

```text
Request
 ↓
JWT
 ↓
Authentication
 ↓
SecurityContext
```

So the `SecurityContext` concept remains.

What changes is:

> **How authentication is obtained/persisted between requests.**

---

# 20. Custom JWT filter

You may see tutorials doing this:

```java
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header =
                request.getHeader("Authorization");

        // Extract token
        // Validate token
        // Create Authentication
        // Put it in SecurityContext

        filterChain.doFilter(request, response);
    }
}
```

The conceptual logic is:

```text
Authorization Header
        ↓
Extract Bearer token
        ↓
Validate JWT
        ↓
Extract user/claims
        ↓
Create Authentication
        ↓
SecurityContextHolder
        ↓
Continue filter chain
```

This is useful to understand for interviews.

But don't conclude:

> "Every Spring Boot JWT application needs a custom OncePerRequestFilter."

That's not true.

For standard JWT resource-server authentication, Spring Security already provides the infrastructure.

---

# 21. Custom filter vs Resource Server

### Custom `OncePerRequestFilter`

You manually implement:

```text
Extract token
      ↓
Parse token
      ↓
Validate token
      ↓
Create Authentication
      ↓
SecurityContext
```

More control, but more responsibility.

You can accidentally introduce:

* bad signature validation
* incorrect expiration handling
* authentication bugs
* security vulnerabilities
* duplicated framework functionality

### Resource Server

Spring Security handles the standard JWT authentication infrastructure.

Conceptually:

```text
Bearer Token
      ↓
Spring Security
      ↓
JWT validation
      ↓
Authentication
      ↓
SecurityContext
```

**Interview answer:**

> For standard JWT bearer-token authentication, I would prefer Spring Security OAuth2 Resource Server rather than implementing JWT authentication manually. A custom filter is appropriate when I have genuinely custom authentication requirements.

---

# 22. How roles/authorities come from JWT

Suppose JWT contains:

```json
{
  "sub": "riyaz",
  "roles": ["USER"]
}
```

You may want:

```java
.hasRole("USER")
```

But Spring Security needs to know how to convert JWT claims into authorities.

Conceptually:

```text
JWT
 |
 | roles = ["USER"]
 ↓
GrantedAuthority
 |
 ↓
ROLE_USER
```

Spring Security provides JWT-to-authority conversion mechanisms, and you can customize the `JwtAuthenticationConverter` when your token uses a custom claim such as:

```json
{
  "permissions": [
    "USER_READ",
    "USER_WRITE"
  ]
}
```

Then those can become:

```text
USER_READ
USER_WRITE
```

authorities.

---

# 23. Example authorization

After authentication:

```text
Authentication
    |
    ├── Principal = riyaz
    |
    └── Authorities
          ├── ROLE_USER
          └── USER_READ
```

Then:

```java
.requestMatchers("/admin/**")
.hasRole("ADMIN")
```

checks:

```text
ROLE_ADMIN
```

While:

```java
.requestMatchers("/users/**")
.hasAuthority("USER_READ")
```

checks:

```text
USER_READ
```

This connects directly to Chapter 3.

---

# 24. Complete mental model

Now combine Chapters 1–5:

```text
                    LOGIN
                      |
                      ↓
             username + password
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
               Issue JWT
                      |
                      ↓
                    Client
                      |
                      |
       ┌──────────────┴──────────────┐
       |                             |
       | Authorization: Bearer JWT   |
       ↓                             |
    API Request                      |
       |                             |
       ↓                             |
Bearer token processing              |
       |                             |
       ↓                             |
    JwtDecoder                       |
       |                             |
       ↓                             |
  Validate JWT                       |
       |                             |
       ↓                             |
Authentication                       |
       |                             |
       ↓                             |
SecurityContext                      |
       |                             |
       ↓                             |
Authorization                       |
       |                             |
       ↓                             |
Controller
```

---

# 25. Very important interview questions

### Q1. What is JWT?

> JWT is a compact token format containing claims that is commonly signed so that a server can verify its integrity and authenticity.

---

### Q2. Is JWT encrypted?

> Normally no. A signed JWT is not encrypted. Its payload can be decoded, but modifying it invalidates the signature.

---

### Q3. Why use JWT?

> JWT enables token-based authentication where the server can validate the token and reconstruct authentication information without relying on a traditional server-side HTTP session.

---

### Q4. Is JWT always stateless?

> No. JWT is a token format. Statelessness is an architectural property. JWT is commonly used for stateless authentication, but JWT itself does not guarantee statelessness.

---

### Q5. What is `SessionCreationPolicy.STATELESS`?

> It tells Spring Security not to use an HTTP session to maintain the authentication state. Authentication is instead established from each request's credentials, such as a bearer token.

---

### Q6. What happens when Spring receives a JWT request?

```text
Bearer token
    ↓
Extract token
    ↓
Decode/validate JWT
    ↓
Create Authentication
    ↓
SecurityContext
    ↓
Authorization
    ↓
Controller
```

---

### Q7. What is the difference between access token and refresh token?

> An access token is a short-lived credential used to access APIs. A refresh token is generally longer-lived and is used with the authorization server to obtain a new access token.

---

### Q8. Why shouldn't JWT contain passwords?

> JWT payloads in the normal signed-JWT case are readable by anyone who possesses the token. Sensitive secrets therefore should not be placed in the payload.

---

### Q9. What is the difference between HS256 and RS256?

```text
HS256
------
One shared secret

Sign    → secret
Verify  → same secret
```

```text
RS256
------
Public/private key pair

Sign    → private key
Verify  → public key
```

---

### Q10. Do we need `OncePerRequestFilter` for JWT?

> Not necessarily. Spring Security's OAuth2 Resource Server provides standard bearer-token JWT authentication. A custom `OncePerRequestFilter` is mainly needed when implementing custom authentication behavior.

---

# 26. The biggest conceptual takeaway

Don't think:

```text
JWT = Authentication
```

Instead think:

```text
JWT
 ↓
Credential/token presented by client
 ↓
Spring Security validates it
 ↓
Authentication created
 ↓
SecurityContext
 ↓
Authorization
```

And compare it with sessions:

```text
SESSION

JSESSIONID
    ↓
Server-side session
    ↓
Authentication
    ↓
SecurityContext
    ↓
Authorization
```

versus:

```text
JWT

Bearer JWT
    ↓
JWT validation
    ↓
Authentication
    ↓
SecurityContext
    ↓
Authorization
```

**The authorization part is largely the same. The major difference is how the authentication state is obtained between requests.**

---

## Chapter 5 summary

You should now understand these terms:

```text
JWT
├── Header
├── Payload / Claims
└── Signature

Stateless Security
├── No traditional HTTP session
├── Token sent with each request
└── Authentication reconstructed per request

Spring Security
├── Bearer token
├── JwtDecoder
├── Authentication
├── SecurityContext
├── Authorization
└── SessionCreationPolicy.STATELESS

Tokens
├── Access Token
└── Refresh Token
```

**Next chapter: Chapter 6 — CSRF, CORS & Security Protections**, where we'll connect browser security, cookies, JWT, sessions, CSRF attacks, CORS/preflight, and why disabling CSRF is appropriate in some stateless API architectures but dangerous in others.

