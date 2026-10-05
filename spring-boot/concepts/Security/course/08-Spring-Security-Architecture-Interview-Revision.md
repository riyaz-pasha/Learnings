# Chapter 8 — Spring Security Architecture & Interview Revision

This is the final chapter of the core course.

The goal here is to connect everything we've learned into **one mental model** that you can use in interviews and real projects.

---

# 1. The most important Spring Security idea

If you remember only one thing:

> **Spring Security is primarily a filter-based security framework.**

A request doesn't normally go directly to your controller.

It goes through the security filter chain first.

```text
HTTP Request
     ↓
Security Filters
     ↓
Authentication
     ↓
SecurityContext
     ↓
Authorization
     ↓
Controller
     ↓
HTTP Response
```

This is the foundation for almost everything we've discussed.

---

# 2. What is `SecurityFilterChain`?

You typically configure:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        );

    return http.build();
}
```

`SecurityFilterChain` represents the collection of security filters that process incoming HTTP requests.

Conceptually:

```text
                    SecurityFilterChain
                           |
       ┌───────────────────┼───────────────────┐
       ↓                   ↓                   ↓
 Authentication       CSRF/CORS          Authorization
   filters             filters              filters
       |                   |                   |
       └───────────────────┼───────────────────┘
                           ↓
                       Controller
```

There are many filters, each responsible for a particular part of security processing.

---

# 3. Why filters?

Suppose you have 100 controllers.

You don't want:

```java
@GetMapping("/users")
public ...
```

to manually perform:

```text
check authentication
check token
check session
check roles
check CSRF
...
```

for every endpoint.

Instead:

```text
HTTP Request
     ↓
SecurityFilterChain
     ↓
Security processing
     ↓
Controller
```

Security becomes centralized.

---

# 4. Authentication and authorization happen at different stages

This distinction should be automatic in your mind now.

### Authentication

```text
Who are you?
```

Example:

```text
JWT → Riyaz
```

### Authorization

```text
What are you allowed to do?
```

Example:

```text
Riyaz → ROLE_USER
/admin → ROLE_ADMIN required
```

Flow:

```text
Request
  ↓
Authentication
  ↓
"Who is this?"
  ↓
SecurityContext
  ↓
Authorization
  ↓
"Can this user access this?"
  ↓
Controller
```

---

# 5. `SecurityContext`

After successful authentication, Spring Security needs somewhere to keep the current authentication information.

That's:

```java
SecurityContext
```

It contains:

```text
Authentication
```

which contains things such as:

```text
Principal
Credentials
Authorities
Authenticated status
```

Conceptually:

```text
SecurityContext
      |
      ↓
Authentication
      |
      ├── Principal
      ├── Credentials
      └── Authorities
```

---

# 6. `SecurityContextHolder`

How does your application access the current authentication?

Usually:

```java
Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();
```

Then:

```java
String username =
        authentication.getName();
```

Or:

```java
authentication.getAuthorities();
```

For example:

```text
SecurityContextHolder
       ↓
SecurityContext
       ↓
Authentication
       ↓
Principal = riyaz
Authorities = ROLE_USER
```

---

# 7. Where does the `Authentication` come from?

This depends on your authentication mechanism.

### Username/password

```text
Username + Password
        ↓
AuthenticationManager
        ↓
AuthenticationProvider
        ↓
UserDetailsService
        ↓
PasswordEncoder
        ↓
Authentication
```

### Session

```text
JSESSIONID
    ↓
Session
    ↓
SecurityContext
    ↓
Authentication
```

### JWT

```text
Bearer JWT
    ↓
JWT validation
    ↓
Authentication
    ↓
SecurityContext
```

### OAuth2/OIDC login

```text
Authorization Server
        ↓
OAuth2/OIDC flow
        ↓
Authenticated user
        ↓
Authentication
        ↓
SecurityContext
```

Different mechanisms ultimately converge toward:

```text
Authentication
      ↓
SecurityContext
      ↓
Authorization
```

That's a very important architectural insight.

---

# 8. AuthenticationManager

For username/password authentication:

```text
AuthenticationManager
```

is the main authentication coordinator.

Conceptually:

```text
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
```

You should remember:

> **AuthenticationManager coordinates authentication; AuthenticationProvider performs a specific authentication mechanism.**

---

# 9. AuthenticationProvider

A provider knows how to authenticate a particular kind of credential.

For example:

```text
Username/password
       ↓
DaoAuthenticationProvider
```

Another authentication mechanism can have another provider.

Conceptually:

```text
AuthenticationManager
        |
        ├── DaoAuthenticationProvider
        ├── OAuth-related provider
        └── Other providers
```

The exact provider chain depends on the application's configuration.

---

# 10. UserDetailsService

This component answers:

> "Where do I get the user's information?"

For example:

```java
public interface UserDetailsService {

    UserDetails loadUserByUsername(String username);
}
```

It might retrieve:

```text
Database
   ↓
User
   ↓
UserDetails
```

Important:

> `UserDetailsService` doesn't normally verify the password.

The authentication provider uses the loaded password information together with `PasswordEncoder`.

---

# 11. PasswordEncoder

Never do:

```java
password.equals(databasePassword)
```

Instead:

```java
passwordEncoder.matches(
    rawPassword,
    encodedPassword
);
```

For example:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

The database should contain a password hash, not the plaintext password.

```text
User enters:
secret123

       ↓

PasswordEncoder

       ↓

matches database hash?
```

---

# 12. Authorization

Once authentication succeeds:

```text
Authentication
      |
      └── Authorities
```

For example:

```text
ROLE_USER
USER_READ
USER_UPDATE
```

Spring Security can use these in:

```java
.hasRole("ADMIN")
```

or:

```java
.hasAuthority("USER_READ")
```

or:

```java
@PreAuthorize("hasRole('ADMIN')")
```

---

# 13. Complete request lifecycle — username/password

Let's put everything together.

```text
POST /login
username=riyaz
password=secret
        |
        ↓
SecurityFilterChain
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
Database
        |
        ↓
UserDetails
        |
        ↓
PasswordEncoder
        |
        ↓
Password matches?
       / \
     No   Yes
     |     |
    401    ↓
      Authentication
             |
             ↓
      SecurityContext
             |
             ↓
       Session/JWT/etc.
```

---

# 14. Complete request lifecycle — JWT

Now compare that with JWT.

```text
GET /profile

Authorization: Bearer JWT
        |
        ↓
SecurityFilterChain
        |
        ↓
Bearer token processing
        |
        ↓
JWT validation
        |
        ↓
Authentication
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

Notice:

> The authentication mechanism changed, but authorization still works using the resulting `Authentication`.

---

# 15. Stateful vs stateless architecture

### Stateful

```text
Login
 ↓
Authentication
 ↓
Session
 ↓
JSESSIONID
```

Later:

```text
JSESSIONID
 ↓
Server session
 ↓
SecurityContext
 ↓
Authentication
```

---

### Stateless

```text
Login
 ↓
Access Token
 ↓
Client
```

Later:

```text
Bearer Token
 ↓
Validate
 ↓
Authentication
 ↓
SecurityContext
```

---

# 16. 401 vs 403

This should be one of your fastest interview answers.

### 401 Unauthorized

Authentication is missing or invalid.

Examples:

```text
No token
Invalid token
Expired token
Invalid credentials
```

Conceptually:

```text
"Who are you?"
      ↓
Cannot establish identity
      ↓
401
```

---

### 403 Forbidden

Authentication succeeded, but authorization failed.

```text
User = Riyaz
Role = USER

Endpoint requires:
ROLE_ADMIN
```

Therefore:

```text
Authenticated ✅
Authorized ❌

403
```

---

# 17. `permitAll()` vs `authenticated()`

```java
.requestMatchers("/public/**")
.permitAll()
```

means:

> No authentication required.

Whereas:

```java
.anyRequest()
.authenticated()
```

means:

> Authentication is required.

And:

```java
.requestMatchers("/admin/**")
.hasRole("ADMIN")
```

means:

> Authentication + required authority.

---

# 18. URL authorization vs method authorization

### URL-level

```java
.requestMatchers("/admin/**")
.hasRole("ADMIN")
```

Good for broad HTTP endpoint rules.

### Method-level

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
}
```

Good when authorization depends on business logic.

For example:

```java
@PreAuthorize("#userId == authentication.principal.id")
```

This can express:

> A user can access only their own resource.

---

# 19. Roles vs authorities

Remember:

```java
.hasRole("ADMIN")
```

typically means:

```text
ROLE_ADMIN
```

while:

```java
.hasAuthority("ROLE_ADMIN")
```

checks the exact authority.

So:

```java
.roles("ADMIN")
```

typically results in:

```text
ROLE_ADMIN
```

while:

```java
.authorities("USER_READ")
```

creates:

```text
USER_READ
```

---

# 20. CSRF

CSRF is primarily a concern when credentials such as cookies are automatically sent by the browser.

Traditional session:

```text
Cookie: JSESSIONID=abc
```

Attack:

```text
evil.com
   ↓
browser
   ↓
bank.com
   +
JSESSIONID
```

CSRF protection prevents forged requests from being accepted.

---

# 21. CORS

CORS deals with:

> Browser cross-origin access.

Example:

```text
Frontend
https://app.example.com

        ↓

Backend
https://api.example.com
```

The browser checks CORS rules.

Spring Security can configure:

```java
http.cors(Customizer.withDefaults());
```

and an appropriate `CorsConfigurationSource`.

Remember:

```text
CSRF → forged authenticated request

CORS → cross-origin browser access
```

---

# 22. OAuth2 and OIDC

The conceptual difference:

```text
OAuth 2.0
    ↓
Authorization

OIDC
    ↓
Authentication / identity
    +
OAuth 2.0
```

### OAuth

> What can this application access?

### OIDC

> Who is the user?

---

# 23. Resource Server

A Spring Boot API that receives:

```http
Authorization: Bearer <access-token>
```

can act as:

> OAuth2 Resource Server.

Configuration:

```java
http.oauth2ResourceServer(
    oauth2 -> oauth2.jwt()
);
```

Conceptually:

```text
Access Token
    ↓
Resource Server
    ↓
Validate
    ↓
Authentication
    ↓
Authorization
```

---

# 24. OAuth2 Client

A Spring application that wants to log users in through an external provider can act as an OAuth2 client.

```java
http.oauth2Login(
    Customizer.withDefaults()
);
```

Conceptually:

```text
Your App
   ↓
Google / Keycloak / Okta / etc.
   ↓
Authentication
   ↓
Your App
```

---

# 25. The most important Spring Security classes

For interviews, know these relationships:

```text
HttpSecurity
    ↓
configures
    ↓
SecurityFilterChain
```

```text
AuthenticationManager
    ↓
coordinates
    ↓
AuthenticationProvider
```

```text
AuthenticationProvider
    ↓
may use
    ↓
UserDetailsService
    +
PasswordEncoder
```

```text
SecurityContextHolder
    ↓
SecurityContext
    ↓
Authentication
    ↓
Principal + Authorities
```

---

# 26. Full architecture

Here's the architecture you should have in your head during an interview:

```text
                         HTTP Request
                              |
                              ↓
                 ┌────────────────────────┐
                 │   SecurityFilterChain  │
                 └────────────┬───────────┘
                              |
             ┌────────────────┼────────────────┐
             |                |                |
             ↓                ↓                ↓
      Authentication        CSRF            CORS
         filters           checks           checks
             |
             ↓
    ┌─────────────────────┐
    │ Authentication       │
    │ mechanism            │
    └──────────┬──────────┘
               |
       ┌───────┼────────┐
       ↓       ↓        ↓
   Session    JWT     OAuth2
       |       |        |
       └───────┼────────┘
               ↓
       Authentication
               |
               ↓
       SecurityContext
               |
               ↓
        Authorization
               |
       ┌───────┴─────────┐
       ↓                 ↓
    Allowed            Denied
       |                 |
       ↓                 ↓
 Controller             403
```

---

# 27. How the pieces fit together

Let's say you build:

```text
E-commerce API
```

A request arrives:

```http
GET /orders/123
Authorization: Bearer eyJ...
```

### Step 1 — Security filter chain

Spring Security intercepts the request.

### Step 2 — Authentication

JWT is validated.

```text
User = Riyaz
```

### Step 3 — SecurityContext

Spring stores:

```text
Authentication
    ↓
Riyaz
ROLE_USER
```

### Step 4 — Authorization

Suppose:

```java
@PreAuthorize("hasRole('USER')")
```

passes.

### Step 5 — Controller

```java
@GetMapping("/orders/{id}")
public Order getOrder(...) {
}
```

executes.

### Step 6 — Business logic

Application retrieves the order.

---

# 28. What happens if authentication fails?

```text
GET /orders/123
Authorization: Bearer invalid-token
```

Flow:

```text
Request
 ↓
SecurityFilterChain
 ↓
JWT validation
 ↓
FAIL
 ↓
401
```

Controller is never reached.

---

# 29. What happens if authorization fails?

```text
User = ROLE_USER
```

but:

```java
@PreAuthorize("hasRole('ADMIN')")
```

Flow:

```text
Request
 ↓
Authentication
 ↓
SUCCESS
 ↓
SecurityContext
 ↓
Authorization
 ↓
FAIL
 ↓
403
```

Again, the controller doesn't execute.

---

# 30. Why security should happen before the controller

Imagine:

```java
@GetMapping("/admin")
public String admin() {
    // check authentication
    // check role
    // check token
}
```

You'd have to repeat this everywhere.

Instead:

```text
Request
 ↓
SecurityFilterChain
 ↓
Authentication
 ↓
Authorization
 ↓
Controller
```

The controller can focus on business logic.

This is one of the biggest benefits of Spring Security's architecture.

---

# 31. Common interview traps

## Trap 1

> "AuthenticationManager authenticates the user directly."

Better:

> It coordinates authentication through one or more `AuthenticationProvider`s.

---

## Trap 2

> "UserDetailsService checks passwords."

Better:

> It loads user information. Password verification is performed using the configured authentication provider and `PasswordEncoder`.

---

## Trap 3

> "JWT is encrypted."

Better:

> A normal signed JWT is not encrypted. Its payload is readable, while the signature protects integrity/authenticity.

---

## Trap 4

> "JWT means stateless."

Better:

> JWT is a token format. It is commonly used for stateless authentication, but the concepts aren't identical.

---

## Trap 5

> "CORS protects APIs from attackers."

Better:

> CORS is primarily a browser mechanism controlling cross-origin access. It isn't a replacement for authentication or authorization.

---

## Trap 6

> "CSRF and CORS are the same."

Definitely not.

```text
CSRF → forged authenticated request

CORS → browser cross-origin access
```

---

## Trap 7

> "OAuth is authentication."

Better:

> OAuth 2.0 is primarily an authorization framework. OIDC adds an authentication/identity layer.

---

## Trap 8

> "Every JWT application needs OncePerRequestFilter."

No.

For standard JWT bearer-token authentication:

```text
OAuth2 Resource Server
```

already provides the necessary infrastructure.

---

# 32. Production-style configuration

A common API configuration might look roughly like:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**")
                    .permitAll()

                .requestMatchers("/admin/**")
                    .hasRole("ADMIN")

                .anyRequest()
                    .authenticated()
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
```

This configuration tells a story:

```text
CSRF
 ↓
disabled because architecture uses bearer-token API authentication

CORS
 ↓
browser frontend is allowed according to configured origins

STATELESS
 ↓
don't maintain traditional HTTP authentication sessions

Authorization
 ↓
public / admin / authenticated rules

Resource Server
 ↓
validate bearer JWTs
```

The important interview skill is not memorizing the configuration.

It's being able to explain **why each part exists**.

---

# 33. Complete Spring Security mental model

At this point, you can reduce Spring Security to:

```text
                    REQUEST
                       |
                       ↓
             SecurityFilterChain
                       |
                       ↓
              AUTHENTICATION
                       |
              "Who are you?"
                       |
                       ↓
               SecurityContext
                       |
                       ↓
               AUTHORIZATION
                       |
          "What can you access?"
                       |
                ┌──────┴──────┐
                ↓             ↓
              ALLOW          DENY
                |             |
                ↓             ↓
           Controller         403
```

And authentication can come from:

```text
Username + Password
        ↓
AuthenticationManager
        ↓
AuthenticationProvider

OR

Session
   ↓
Authentication

OR

JWT
   ↓
Bearer token validation
   ↓
Authentication

OR

OAuth2/OIDC
   ↓
Authentication
```

---

# 34. The 15 questions I would expect in a Spring Security interview

### Fundamentals

**1. What is Spring Security?**

A framework providing authentication, authorization, and security protections for Spring applications.

**2. Authentication vs authorization?**

Authentication identifies the user; authorization determines what that authenticated user is allowed to do.

**3. What is `SecurityFilterChain`?**

The chain of servlet filters through which requests pass before reaching application endpoints, performing security processing.

---

### Authentication

**4. What is `AuthenticationManager`?**

The main authentication coordinator.

**5. What is `AuthenticationProvider`?**

A component that performs a particular authentication mechanism.

**6. What is `UserDetailsService`?**

A service used to load user information, commonly from a database.

**7. What is `PasswordEncoder`?**

Used to securely hash passwords and verify raw passwords against stored hashes.

---

### Authorization

**8. Role vs authority?**

A role is conventionally an authority with the `ROLE_` prefix.

```java
hasRole("ADMIN")
```

typically means:

```text
ROLE_ADMIN
```

---

### Sessions/JWT

**9. Stateful vs stateless?**

Stateful authentication maintains authentication state on the server, typically through an HTTP session. Stateless authentication reconstructs authentication from credentials supplied with each request, commonly a bearer token.

**10. Why use `SessionCreationPolicy.STATELESS`?**

To prevent Spring Security from using an HTTP session for authentication state.

**11. Is JWT encrypted?**

Normally no. A signed JWT is readable but protected against unauthorized modification by its signature.

---

### Security protections

**12. CSRF vs CORS?**

CSRF protects against forged authenticated requests; CORS controls browser cross-origin access.

**13. Why disable CSRF for some APIs?**

When the API uses stateless bearer-token authentication rather than automatically attached browser cookies, the traditional CSRF threat model doesn't apply in the same way.

---

### OAuth

**14. OAuth2 vs OIDC?**

OAuth2 provides authorization/delegated access; OIDC adds authentication and identity.

**15. OAuth2 Resource Server vs OAuth2 Client?**

```text
Resource Server
→ validates access tokens

OAuth2 Client
→ participates in OAuth login/token flows
```

---

# 35. One final interview answer

If the interviewer asks:

> **"Explain how Spring Security works internally."**

A strong concise answer would be:

> "Spring Security is primarily filter-based. Incoming HTTP requests pass through a `SecurityFilterChain`. Depending on the authentication mechanism, Spring Security authenticates the request using credentials such as username/password, a session, or a bearer token. Successful authentication produces an `Authentication` object, which is stored in the `SecurityContext`. Authorization then evaluates the user's authorities against configured URL or method-level rules. If authorization succeeds, the request reaches the controller; otherwise Spring Security returns an appropriate security response such as 401 or 403. Additional filters and mechanisms handle protections such as CSRF, CORS, session management, and OAuth2/JWT authentication."

That answer covers most of the architecture without getting lost in implementation details.

---

# Final course map

You have now covered the core Spring Security concepts:

```text
1. Fundamentals
   │
   ├── Authentication
   ├── Authorization
   └── SecurityFilterChain
   │
2. Authentication Internals
   │
   ├── AuthenticationManager
   ├── AuthenticationProvider
   ├── UserDetailsService
   └── PasswordEncoder
   │
3. Authorization
   │
   ├── Roles
   ├── Authorities
   ├── URL security
   └── Method security
   │
4. Sessions
   │
   ├── JSESSIONID
   ├── SecurityContext
   ├── Session fixation
   └── Stateful authentication
   │
5. JWT
   │
   ├── Access tokens
   ├── Stateless authentication
   ├── Bearer tokens
   └── JWT validation
   │
6. CSRF / CORS
   │
   ├── CSRF
   ├── CORS
   ├── Preflight
   └── Browser security
   │
7. OAuth2 / OIDC
   │
   ├── Authorization Server
   ├── Resource Server
   ├── OAuth2 Client
   ├── Access Token
   ├── ID Token
   └── PKCE
   │
8. Architecture
   │
   └── Everything connected
```

The **next useful step** would be a dedicated **Spring Security interview Q&A round**, going question-by-question from basic → intermediate → advanced, with the kind of answers you'd actually give in an interview.


---
---
---










































