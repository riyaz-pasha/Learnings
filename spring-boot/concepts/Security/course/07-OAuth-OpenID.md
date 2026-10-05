# Chapter 7 — OAuth 2.0 & OpenID Connect (OIDC)

This is one of the most important Spring Security topics for backend interviews.

The biggest thing to understand first:

> **OAuth 2.0 is primarily an authorization framework. OIDC adds authentication on top of OAuth 2.0.**

A lot of developers incorrectly say:

> "OAuth is a login protocol."

That's not quite correct.

---

# 1. The problem OAuth solves

Imagine you build an application:

```text
PhotoApp
```

You want users to be able to access their Google photos.

A naive approach would be:

```text
PhotoApp
   |
   | "Give me your Google username/password"
   ↓
User
```

That's terrible.

Now PhotoApp knows the user's Google password.

It could potentially:

* read everything
* modify everything
* impersonate the user

Instead, we want:

```text
User
  |
  | "Allow PhotoApp to access my photos"
  ↓
Google
  |
  | limited permission
  ↓
PhotoApp
```

This is the problem OAuth solves.

---

# 2. OAuth's core idea

Instead of giving your password to another application:

```text
❌

Google password
      ↓
PhotoApp
```

you authorize the application through the authorization server:

```text
✅

User
  ↓
Authorization Server
  ↓
"Allow PhotoApp to read photos?"
  ↓
Access Token
  ↓
PhotoApp
```

The application gets a limited credential.

---

# 3. OAuth is about delegated authorization

Suppose:

```text
Google account
```

has:

```text
photos
emails
calendar
contacts
drive
```

You might authorize:

```text
PhotoApp → read photos
```

but not:

```text
PhotoApp → delete emails
```

This is **delegated authorization**.

The user delegates limited access.

---

# 4. The important OAuth actors

You should know these four roles for interviews.

## 1. Resource Owner

Usually:

> The user.

Example:

```text
Riyaz
```

owns the data.

---

## 2. Client

The application requesting access.

Example:

```text
PhotoApp
```

---

## 3. Authorization Server

The system that:

* authenticates the user
* asks for consent
* issues authorization codes/tokens

Example:

```text
Google Identity
```

---

## 4. Resource Server

The API containing protected resources.

Example:

```text
Google Photos API
```

---

# 5. Architecture

```text
                    User
                      |
                      |
                      ↓
                 ┌─────────┐
                 │ Client  │
                 │ PhotoApp│
                 └────┬────┘
                      |
                      |
              Authorization
                 Request
                      |
                      ↓
          ┌──────────────────────┐
          │ Authorization Server │
          │       Google         │
          └──────────┬───────────┘
                     |
                     | Access Token
                     ↓
          ┌──────────────────────┐
          │   Resource Server    │
          │   Photos API         │
          └──────────────────────┘
```

---

# 6. OAuth access token

After authorization, the client gets an:

> **Access Token**

For example:

```text
access_token = eyJhbGciOi...
```

The client uses it:

```http
GET /photos

Authorization: Bearer eyJhbGciOi...
```

The resource server validates the token and allows access.

---

# 7. What is a scope?

A scope represents what the client is allowed to do.

For example:

```text
scope=photos.read
```

or:

```text
scope=photos.read photos.write
```

Think:

```text
Token
  |
  └── scopes
        ├── photos.read
        └── photos.write
```

The resource server can then enforce:

```text
photos.read → GET /photos
photos.write → POST /photos
```

This is closely related to the authorities we learned earlier.

---

# 8. OAuth 2.0 Authorization Code flow

For user-facing web applications, one of the most important flows to understand is:

> **Authorization Code Flow**

Let's walk through it.

---

## Step 1 — User accesses application

```text
User
  ↓
MyApp
```

MyApp says:

> "You need to authorize with the identity provider."

---

## Step 2 — Redirect to authorization server

The application redirects the browser:

```text
MyApp
  ↓
Authorization Server
```

Conceptually:

```text
https://auth.example.com/authorize
    ?client_id=my-app
    &redirect_uri=https://myapp.com/callback
    &response_type=code
    &scope=photos.read
```

---

# 9. User authenticates

The authorization server displays:

```text
Login

Username:
Password:
```

The user authenticates **with the authorization server**, not your application.

This is important.

```text
MyApp
   ❌ doesn't receive password

Auth Server
   ✅ receives password
```

---

# 10. User gives consent

The authorization server may show:

```text
PhotoApp wants:

☑ Read your photos
```

User clicks:

```text
Allow
```

---

# 11. Authorization code

The authorization server redirects back to the client:

```text
https://myapp.com/callback?code=ABC123
```

The client receives:

```text
ABC123
```

This is an:

> **Authorization Code**

Important:

> The authorization code is not normally the API access token.

It's an intermediate credential used to obtain tokens.

---

# 12. Client exchanges code for tokens

The backend sends the authorization code to the authorization server.

Conceptually:

```http
POST /token

grant_type=authorization_code
code=ABC123
client_id=my-app
client_secret=...
redirect_uri=...
```

The authorization server validates the request.

Then returns:

```json
{
  "access_token": "...",
  "refresh_token": "...",
  "token_type": "Bearer",
  "expires_in": 900
}
```

---

# 13. Client calls resource server

Now:

```text
MyApp
   |
   | Authorization: Bearer ACCESS_TOKEN
   ↓
Photos API
```

The resource server validates the token.

```text
Valid?
  ↓
YES
  ↓
Scope includes photos.read?
  ↓
YES
  ↓
Allow
```

---

# 14. Complete flow

```text
┌──────────┐
│   User   │
└────┬─────┘
     |
     | 1. Access app
     ↓
┌──────────┐
│  Client  │
└────┬─────┘
     |
     | 2. Redirect
     ↓
┌────────────────────┐
│ Authorization      │
│ Server              │
└────────┬───────────┘
         |
         | 3. Login + consent
         |
         | 4. Authorization Code
         ↓
┌──────────┐
│  Client  │
└────┬─────┘
     |
     | 5. Code → Token
     ↓
┌────────────────────┐
│ Authorization      │
│ Server              │
└────────┬───────────┘
         |
         | 6. Access Token
         ↓
┌───────────────┐
│ Resource      │
│ Server        │
└───────────────┘
```

---

# 15. Why use an authorization code?

You might ask:

> Why not directly return the access token in the browser redirect?

The authorization code provides an intermediate step.

Modern OAuth Authorization Code flow also uses:

> **PKCE — Proof Key for Code Exchange**

PKCE is extremely important for modern OAuth clients, especially public clients such as SPAs and mobile applications.

---

# 16. PKCE

The client creates a secret random value:

```text
code_verifier
```

Then derives:

```text
code_challenge
```

The authorization request includes:

```text
code_challenge
```

Later, when exchanging the authorization code, the client sends:

```text
code_verifier
```

The authorization server verifies:

```text
code_challenge
        ↔
code_verifier
```

Conceptually:

```text
Client
  |
  | code_challenge
  ↓
Authorization Server
  |
  | authorization code
  ↓
Client
  |
  | code + code_verifier
  ↓
Authorization Server
  |
  ↓
Token
```

If an attacker steals only the authorization code, they cannot successfully redeem it without the verifier.

---

# 17. OAuth vs OIDC

This is probably the **most important interview distinction in this chapter**.

### OAuth 2.0

Primarily answers:

> **What is this application allowed to access?**

Authorization.

### OpenID Connect

Adds:

> **Who is the user?**

Authentication.

So:

```text
OAuth 2.0
    ↓
Authorization

OIDC
    ↓
Authentication + OAuth 2.0
```

---

# 18. What does OIDC add?

OIDC introduces an:

> **ID Token**

Usually a JWT.

Example:

```json
{
  "iss": "https://identity.example.com",
  "sub": "123456",
  "aud": "my-client",
  "name": "Riyaz",
  "email": "riyaz@example.com"
}
```

The ID token tells the client about the authenticated user.

---

# 19. Access Token vs ID Token

This is an extremely common interview question.

### Access Token

Intended for:

> **Resource server / API**

Example:

```text
Client
  |
  | Access Token
  ↓
Photos API
```

### ID Token

Intended for:

> **Client application**

It tells the client:

> "This is the user who authenticated."

So:

```text
Access Token
→ API authorization


ID Token
→ User authentication information
```

Do **not** blindly send an ID token to an API expecting an access token.

---

# 20. OIDC flow

With OIDC:

```text
User
 ↓
Client
 ↓
Authorization Server
 ↓
Login
 ↓
Authorization Code
 ↓
Token Endpoint
 ↓
Access Token + ID Token
```

The ID token contains identity information.

The access token is used to access protected resources.

---

# 21. Where does Spring Security fit?

Spring Security can act as an:

### OAuth2 Client

Your application uses an external identity provider.

For example:

```text
Spring Boot
     ↓
Google
```

Useful for:

```text
Login with Google
Login with Microsoft
Login with GitHub
```

---

### OAuth2 Resource Server

Your Spring Boot API receives access tokens.

```text
Client
  |
  | Bearer token
  ↓
Spring Boot API
```

Spring validates the token.

This is what we discussed in Chapter 5.

---

# 22. OAuth2 Client vs Resource Server

Remember this:

```text
OAuth2 Client
→ "I need to obtain/use tokens."

Resource Server
→ "I receive and validate access tokens."
```

Example:

```text
             Google
               |
               |
       ┌───────┴────────┐
       ↓                ↓
    Frontend          Spring API
       |
       |
 OAuth2 Client       Resource Server
```

---

# 23. Spring Security OAuth2 Login

Suppose you want:

```text
Login with Google
```

Spring Security can configure OAuth2 login.

Conceptually:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/public").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(Customizer.withDefaults());

    return http.build();
}
```

Now Spring Security handles much of the OAuth/OIDC flow.

You configure the client registration and provider information.

---

# 24. What happens internally?

Conceptually:

```text
Browser
   |
   ↓
Spring Boot
   |
   | redirect
   ↓
Identity Provider
   |
   | Login + consent
   ↓
Authorization Code
   |
   ↓
Spring Boot
   |
   | code exchange
   ↓
Identity Provider
   |
   | tokens
   ↓
Spring Boot
   |
   ↓
Authenticated User
```

Spring Security handles the protocol details.

---

# 25. `oauth2Login()` vs `oauth2ResourceServer()`

Very important.

```java
.oauth2Login()
```

means:

> My application wants users to log in through an OAuth2/OIDC provider.

Whereas:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt())
```

means:

> My application is an API that receives bearer access tokens and validates them.

So:

```text
oauth2Login()
    ↓
User login


oauth2ResourceServer()
    ↓
API token validation
```

---

# 26. Real-world microservice architecture

A common enterprise architecture:

```text
                    ┌────────────────────┐
                    │ Identity Provider  │
                    │                    │
                    │ Auth0 / Okta /     │
                    │ Keycloak / etc.    │
                    └─────────┬──────────┘
                              |
                         Access Token
                              |
                              ↓
                         API Gateway
                              |
              ┌───────────────┼───────────────┐
              ↓               ↓               ↓
          User Service    Order Service   Payment Service
              |               |               |
          Resource         Resource        Resource
           Server           Server          Server
```

Each API can validate the access token.

---

# 27. Authentication vs Authorization in OAuth/OIDC

This is where all the previous chapters connect.

Suppose user logs into Google.

OIDC answers:

```text
Who is the user?
```

```text
Riyaz
```

Then OAuth scopes answer:

```text
What can the client access?
```

```text
photos.read
```

Then your Spring Security authorization can answer:

```text
What can this user do inside our application?
```

For example:

```text
ROLE_ADMIN
USER_READ
USER_DELETE
```

So there can be multiple layers:

```text
OIDC
 ↓
Who is the user?

OAuth
 ↓
What resource access was delegated?

Spring Security
 ↓
What can this authenticated user do?
```

---

# 28. OAuth scopes vs Spring authorities

They are related but not identical.

OAuth scope:

```text
photos.read
```

is usually a permission granted to a client/token.

Spring authority:

```text
USER_DELETE
```

is an authority Spring Security can use for authorization.

Spring Security can map token claims/scopes to authorities.

For example:

```text
scope: photos.read
       ↓
SCOPE_photos.read
       ↓
hasAuthority("SCOPE_photos.read")
```

This is a very common pattern with Spring Security resource servers.

---

# 29. Common OAuth grant types

You should know these names for interviews.

### Authorization Code

Used for user authorization flows.

Modern applications generally use:

```text
Authorization Code + PKCE
```

---

### Client Credentials

Used for:

> Machine-to-machine authentication.

There is no end user involved.

Example:

```text
Order Service
      |
      | client credentials
      ↓
Authorization Server
      |
      ↓
Access Token
      |
      ↓
Payment Service
```

This is very common in microservices.

---

### Refresh Token

Used to obtain a new access token without requiring the user to authenticate again.

---

# 30. What about Password Grant?

You may encounter:

```text
Resource Owner Password Credentials
```

Historically it allowed:

```text
username + password
       ↓
client
       ↓
authorization server
```

Modern OAuth guidance discourages this flow.

For modern applications, prefer appropriate flows such as:

```text
Authorization Code + PKCE
```

or:

```text
Client Credentials
```

depending on the use case.

---

# 31. Client Credentials flow

This one deserves a simple example.

Suppose:

```text
Reporting Service
```

needs to call:

```text
Analytics Service
```

There is no user.

So:

```text
Reporting Service
       |
       | client_id + client_secret
       ↓
Authorization Server
       |
       | access token
       ↓
Reporting Service
       |
       | Bearer token
       ↓
Analytics Service
```

This is:

> **Machine-to-machine authentication/authorization.**

---

# 32. OAuth security concepts you should know

### Redirect URI

The authorization server redirects the browser back to the registered URI.

Example:

```text
https://myapp.com/oauth/callback
```

It must be carefully controlled.

---

### Client ID

Identifies the application.

```text
client_id = my-application
```

A client ID is generally not a secret.

---

### Client Secret

Used to authenticate a confidential client.

It **must be kept secret**.

Don't put it in frontend JavaScript.

---

### State

Used in OAuth authorization requests to help prevent request/response correlation attacks such as login CSRF.

Conceptually:

```text
Generate state
    ↓
Authorization request
    ↓
Callback
    ↓
Verify state
```

---

### PKCE

Protects the authorization code exchange by binding the code to the client-generated verifier.

---

# 33. Confidential vs public clients

### Confidential client

Can securely store credentials.

Example:

```text
Backend server
```

Can have:

```text
client_secret
```

---

### Public client

Cannot safely keep a client secret.

Examples:

```text
Mobile app
SPA
```

Therefore PKCE is especially important.

---

# 34. OAuth interview scenario

### Interviewer:

> "Our React application wants users to log in using Google. Would you use OAuth or OIDC?"

Good answer:

> "I'd use OpenID Connect because we need authentication and user identity. OAuth 2.0 provides authorization, while OIDC adds an identity layer through the ID token."

---

# 35. Another interview scenario

### Interviewer:

> "Our Spring Boot API receives a bearer access token issued by Keycloak. What is the Spring Security component?"

Answer:

> "The API acts as an OAuth2 Resource Server. Spring Security validates the bearer access token, typically as a JWT, and establishes an authenticated SecurityContext."

---

# 36. Another interview scenario

### Interviewer:

> "Service A needs to call Service B without a user."

Answer:

> "I'd typically use an OAuth2 Client Credentials flow. Service A authenticates as a client to the authorization server, obtains an access token, and sends that bearer token to Service B."

---

# 37. The most important vocabulary

Memorize these:

```text
Resource Owner
    → User who owns the data

Client
    → Application requesting access

Authorization Server
    → Authenticates/authorizes and issues tokens

Resource Server
    → API protecting resources

Access Token
    → Credential used to access APIs

Scope
    → Delegated permission

Authorization Code
    → Temporary code exchanged for tokens

Refresh Token
    → Used to obtain new access tokens

OIDC
    → Identity/authentication layer on OAuth 2.0

ID Token
    → Identity information about authenticated user

PKCE
    → Protects authorization code flow

Client Credentials
    → Machine-to-machine flow
```

---

# 38. Final mental model

The whole OAuth/OIDC world can be reduced to:

```text
                         User
                           |
                           ↓
                    ┌─────────────┐
                    │   Client    │
                    │   My App    │
                    └──────┬──────┘
                           |
                           | Authorization
                           ↓
                 ┌────────────────────┐
                 │ Authorization      │
                 │ Server / IdP        │
                 └─────────┬──────────┘
                           |
                    ┌──────┴──────┐
                    ↓             ↓
              ID Token       Access Token
                    |             |
                    ↓             ↓
              "Who are you?"  "What can
                               you access?"
                                  |
                                  ↓
                         ┌────────────────┐
                         │ Resource Server│
                         │ Spring Boot API│
                         └────────────────┘
```

And Spring Security sits around these concepts:

```text
OAuth2 Login
    ↓
Authentication


OAuth2 Resource Server
    ↓
Access Token validation
    ↓
Authentication
    ↓
SecurityContext
    ↓
Authorization
```

---

## Interview cheat sheet

| Question                  | Short answer                                 |
| ------------------------- | -------------------------------------------- |
| OAuth 2.0?                | Authorization framework                      |
| OIDC?                     | Authentication/identity layer on OAuth 2.0   |
| Access token?             | Used to access protected APIs                |
| ID token?                 | Contains identity information for the client |
| Scope?                    | Delegated permission                         |
| Authorization code?       | Temporary code exchanged for tokens          |
| PKCE?                     | Protects authorization code flow             |
| Resource server?          | API that validates access tokens             |
| Authorization server?     | Authenticates/authorizes and issues tokens   |
| Client Credentials?       | Machine-to-machine                           |
| `oauth2Login()`?          | User login via OAuth2/OIDC                   |
| `oauth2ResourceServer()`? | Validate bearer access tokens                |
| OAuth = login?            | **No**                                       |
| OIDC = authentication?    | **Yes**                                      |

### One sentence to remember

> **OAuth 2.0 answers "what may this application access?", while OIDC answers "who is the user?"**

**Next: Chapter 8 — Spring Security Architecture & Interview Revision**, where we'll bring all 7 chapters together: filters, `SecurityFilterChain`, authentication, authorization, `SecurityContext`, sessions, JWT, OAuth2/OIDC, and the complete request lifecycle.

