Absolutely. This is one of the **most important Spring Security interview topics** because these lines look simple:

```java
http
    .authorizeHttpRequests(...)
    .oauth2ResourceServer(...);
```

but they configure a fairly large security pipeline behind the scenes.

The key thing to understand is:

> **You normally configure one `SecurityFilterChain` bean, but that chain contains many Spring Security filters.**

Let's build the mental model first, then go line by line.

---

# 1. What is this `http` object?

When you write:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .authorizeHttpRequests(...)
        .oauth2ResourceServer(...);

    return http.build();
}
```

`http` is an instance of:

```java
HttpSecurity
```

Think of `HttpSecurity` as a **configuration builder**.

It is not itself the security filter.

It is used to tell Spring Security:

> "How should I configure security for HTTP requests?"

Eventually:

```java
http.build()
```

creates:

```text
SecurityFilterChain
```

So:

```text
HttpSecurity
    │
    │ configuration
    ▼
http.build()
    │
    ▼
SecurityFilterChain
    │
    ▼
Many Security Filters
```

---

# 2. Very important distinction

There are three different things here:

```text
HttpSecurity
      │
      │ builds
      ▼
SecurityFilterChain
      │
      │ contains
      ▼
Multiple Security Filters
```

### `HttpSecurity`

Configuration API.

### `SecurityFilterChain`

The resulting chain of filters.

### Security filters

Actual components that process HTTP requests.

This distinction is a **very common interview question**.

---

# 3. Do we create multiple filters or a single filter?

The answer is:

> **A `SecurityFilterChain` contains multiple filters.**

For example, conceptually:

```text
SecurityFilterChain
│
├── DisableEncodeUrlFilter
├── WebAsyncManagerIntegrationFilter
├── SecurityContextHolderFilter
├── HeaderWriterFilter
├── CorsFilter
├── CsrfFilter
├── LogoutFilter
├── BearerTokenAuthenticationFilter
├── RequestCacheAwareFilter
├── AnonymousAuthenticationFilter
├── ExceptionTranslationFilter
└── AuthorizationFilter
```

The exact list and ordering depends on your configuration and Spring Security version.

So don't memorize the exact list.

Understand their **responsibilities and ordering**.

---

# 4. Your configuration

Let's take your example:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/swagger-ui/**",
            "/v3/api-docs/**"
        ).permitAll()
        .anyRequest().authenticated()
    );
```

This configures **authorization rules**.

And:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt());
```

configures **Bearer JWT authentication**.

Together:

```text
                    HttpSecurity
                        │
           ┌────────────┴────────────┐
           │                         │
 authorizeHttpRequests()    oauth2ResourceServer()
           │                         │
           ▼                         ▼
    Authorization rules       JWT authentication
           │                         │
           └────────────┬────────────┘
                        │
                        ▼
                 http.build()
                        │
                        ▼
              SecurityFilterChain
```

---

# 5. What does `http.build()` actually do?

This:

```java
return http.build();
```

is extremely important.

Before `build()`:

```text
HttpSecurity
   │
   ├── authorization configuration
   ├── JWT configuration
   ├── CSRF configuration
   ├── CORS configuration
   ├── session configuration
   └── etc.
```

After:

```java
http.build();
```

Spring creates/configures the actual:

```text
SecurityFilterChain
```

which contains filters.

So:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
```

means:

> "Create and expose one SecurityFilterChain for Spring Security to use."

---

# 6. What happens when an HTTP request arrives?

Suppose:

```http
GET /api/orders
Authorization: Bearer eyJ...
```

The request doesn't directly go:

```text
Controller
```

Instead:

```text
HTTP Request
      │
      ▼
Servlet container
      │
      ▼
Spring Security FilterChain
      │
      ▼
Security filters
      │
      ▼
DispatcherServlet
      │
      ▼
Controller
```

So Spring Security gets a chance to process the request **before your controller**.

---

# 7. Where does `SecurityFilterChain` come from?

Spring Security integrates with the Servlet filter mechanism.

Conceptually:

```text
Servlet Container
       │
       ▼
DelegatingFilterProxy
       │
       ▼
FilterChainProxy
       │
       ▼
SecurityFilterChain
       │
       ├── Filter 1
       ├── Filter 2
       ├── Filter 3
       ├── ...
       └── Filter N
       │
       ▼
Application
```

This is another very important interview concept.

---

# 8. `DelegatingFilterProxy`

At the Servlet level, Spring Security uses:

```text
DelegatingFilterProxy
```

Its job is essentially:

> Delegate the servlet filter request into Spring's application context.

Conceptually:

```text
Servlet Container
       │
       ▼
DelegatingFilterProxy
       │
       ▼
Spring Security
```

You generally don't manually create this in a modern Spring Boot application.

Spring Boot/Spring Security configures the integration for you.

---

# 9. `FilterChainProxy`

Then Spring Security has:

```text
FilterChainProxy
```

This is responsible for selecting the appropriate:

```text
SecurityFilterChain
```

for the request.

This becomes especially important when an application has **multiple security filter chains**.

For example:

```java
@Bean
SecurityFilterChain apiChain(HttpSecurity http) {
    ...
}
```

and:

```java
@Bean
SecurityFilterChain adminChain(HttpSecurity http) {
    ...
}
```

You can have multiple chains.

But inside each chain:

```text
SecurityFilterChain
      │
      ├── Filter
      ├── Filter
      ├── Filter
      └── Filter
```

---

# 10. Single SecurityFilterChain vs multiple

Most applications can have:

```text
One SecurityFilterChain
```

For example:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .authorizeHttpRequests(...)
        .oauth2ResourceServer(...);

    return http.build();
}
```

That's perfectly normal.

But you **can** have multiple chains.

For example:

```text
Request
   │
   ▼
FilterChainProxy
   │
   ├── /api/** ─────────► API SecurityFilterChain
   │
   └── /admin/** ───────► Admin SecurityFilterChain
```

Each chain can have different security configuration.

This is an advanced topic, but important for interviews.

---

# 11. Now let's understand `authorizeHttpRequests`

Your code:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/swagger-ui/**",
        "/v3/api-docs/**"
    ).permitAll()
    .anyRequest().authenticated()
)
```

is basically defining:

```text
IF request matches /swagger-ui/**
    → allow

IF request matches /v3/api-docs/**
    → allow

OTHERWISE
    → require authentication
```

Conceptually:

```text
                    Request
                       │
                       ▼
             ┌──────────────────┐
             │ Authorization    │
             │ rules            │
             └────────┬─────────┘
                      │
             ┌────────┴────────┐
             │                 │
     Swagger/OpenAPI?       Other?
             │                 │
            YES               YES
             │                 │
             ▼                 ▼
         permitAll()     authenticated()
```

---

# 12. Does `permitAll()` mean "skip all security filters"?

**No.**

This is a very important interview trap.

If you have:

```java
.requestMatchers("/swagger-ui/**").permitAll()
```

it does **not** mean:

```text
Swagger request
   ↓
Skip Spring Security completely
   ↓
Controller
```

Instead:

```text
Swagger request
      │
      ▼
Security filters
      │
      ▼
Authorization decision
      │
      ▼
permitAll
      │
      ▼
continue
```

Other security filters may still run.

`permitAll()` primarily means:

> **The authorization decision for this request does not require an authenticated user.**

It doesn't mean "disable security processing."

---

# 13. What does `.authenticated()` mean?

This:

```java
.anyRequest().authenticated()
```

means:

> Any request that hasn't matched an earlier rule must have an authenticated `Authentication`.

For example:

```http
GET /api/orders
```

Spring checks:

```text
Is there an authenticated Authentication?
          │
      ┌───┴────┐
      │        │
     YES      NO
      │        │
      ▼        ▼
   continue    401
```

---

# 14. Why is ordering important?

Suppose you write:

```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().authenticated()
    .requestMatchers("/swagger-ui/**").permitAll()
);
```

This is wrong/unusable because the broad rule:

```java
anyRequest()
```

already matches the request.

Authorization rules are generally evaluated in declaration order.

Think:

```text
First matching rule wins
```

So write:

```java
.requestMatchers("/swagger-ui/**").permitAll()
.anyRequest().authenticated()
```

not the other way around.

---

# 15. Now the interesting part: `oauth2ResourceServer`

You have:

```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt()
);
```

This tells Spring Security:

> "This application is an OAuth2 Resource Server, and bearer access tokens are JWTs."

This configures the infrastructure needed to process:

```http
Authorization: Bearer <JWT>
```

---

# 16. What does this add to the filter chain?

One of the key filters involved is:

```text
BearerTokenAuthenticationFilter
```

Its responsibility is roughly:

```text
HTTP Request
      │
      ▼
Authorization header
      │
      ▼
Bearer token
      │
      ▼
Authentication process
```

For example:

```http
Authorization: Bearer eyJhbGciOi...
```

The filter extracts:

```text
eyJhbGciOi...
```

and passes it into Spring Security's authentication infrastructure.

---

# 17. Very important: the Bearer filter doesn't do everything

A common beginner misconception is:

> "BearerTokenAuthenticationFilter validates the JWT."

That's an oversimplification.

The flow is closer to:

```text
BearerTokenAuthenticationFilter
          │
          │ extracts token
          ▼
BearerTokenAuthenticationToken
          │
          ▼
AuthenticationManager
          │
          ▼
JwtAuthenticationProvider
          │
          ▼
JwtDecoder
          │
          ▼
JWT validation
          │
          ▼
Authentication
```

This is a beautiful interview flow to remember.

---

# 18. Full JWT authentication flow

Suppose:

```http
GET /api/orders
Authorization: Bearer eyJ...
```

The flow is approximately:

```text
HTTP Request
     │
     ▼
SecurityFilterChain
     │
     ▼
BearerTokenAuthenticationFilter
     │
     │ extract JWT
     ▼
BearerTokenAuthenticationToken
     │
     ▼
AuthenticationManager
     │
     ▼
JwtAuthenticationProvider
     │
     ▼
JwtDecoder
     │
     ├── signature
     ├── exp
     ├── iss
     ├── aud (when configured)
     └── other validators
     │
     ▼
Jwt
     │
     ▼
JwtAuthenticationConverter
     │
     ▼
Authentication
     │
     ▼
SecurityContext
```

Then authorization happens.

---

# 19. Authentication vs Authorization in this exact configuration

This is extremely important.

You configured:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt())
```

This deals primarily with:

```text
AUTHENTICATION
```

And:

```java
.authorizeHttpRequests(...)
```

deals primarily with:

```text
AUTHORIZATION
```

So:

```text
                  HTTP Request
                       │
                       ▼
                JWT Authentication
                       │
                       ▼
             "Who is this user?"
                       │
                       ▼
                Authentication
                       │
                       ▼
               Authorization
                       │
                       ▼
           "Can they access this?"
```

---

# 20. Example: valid JWT but insufficient permission

Suppose the JWT is valid:

```text
sub = user123
scope = orders:read
```

And endpoint:

```java
.requestMatchers(HttpMethod.DELETE, "/api/orders/**")
    .hasAuthority("SCOPE_orders:delete")
```

Request:

```http
DELETE /api/orders/123
Authorization: Bearer <valid JWT>
```

Authentication:

```text
JWT valid?
    ↓
YES
    ↓
Authentication created
```

Authorization:

```text
Has orders:delete?
    ↓
NO
    ↓
403 Forbidden
```

This demonstrates:

> **Valid authentication does not imply authorization.**

---

# 21. What if the JWT is invalid?

Suppose:

```http
Authorization: Bearer invalid-token
```

Then:

```text
BearerTokenAuthenticationFilter
       ↓
AuthenticationManager
       ↓
JwtAuthenticationProvider
       ↓
JwtDecoder
       ↓
Validation fails
       ↓
Authentication failure
       ↓
401 Unauthorized
```

The request doesn't reach your controller.

---

# 22. What if there is no JWT?

Suppose:

```http
GET /api/orders
```

No Authorization header.

Then:

```text
Request
   ↓
Security filters
   ↓
No authenticated identity
   ↓
Authorization rule:
authenticated()
   ↓
Authentication required
   ↓
401
```

Again, controller isn't called.

---

# 23. What about Swagger?

Request:

```http
GET /swagger-ui/index.html
```

Rule:

```java
.requestMatchers("/swagger-ui/**").permitAll()
```

Therefore:

```text
Request
   ↓
Security filters
   ↓
Authorization
   ↓
permitAll()
   ↓
Allowed
   ↓
Swagger UI
```

No login is required.

---

# 24. The actual filter chain is more complex

Conceptually you can think:

```text
HTTP Request
      │
      ▼
SecurityFilterChain
      │
      ├── SecurityContextHolderFilter
      │
      ├── HeaderWriterFilter
      │
      ├── CorsFilter
      │
      ├── CsrfFilter
      │
      ├── LogoutFilter
      │
      ├── BearerTokenAuthenticationFilter
      │
      ├── RequestCacheAwareFilter
      │
      ├── AnonymousAuthenticationFilter
      │
      ├── ExceptionTranslationFilter
      │
      └── AuthorizationFilter
      │
      ▼
DispatcherServlet
      │
      ▼
Controller
```

Again, **the exact filters depend on configuration**.

For example, if CSRF is disabled:

```java
.csrf(csrf -> csrf.disable())
```

the CSRF filter won't be part of the effective chain.

If you're using form login, additional filters are involved.

If you're using HTTP Basic, another authentication filter is involved.

If you're using OAuth2 login, more OAuth-related filters appear.

---

# 25. Why doesn't every request need every authentication mechanism?

Spring Security can support multiple authentication mechanisms.

For example:

```text
                    SecurityFilterChain
                           │
          ┌────────────────┼────────────────┐
          │                │                │
      HTTP Basic          JWT          OAuth2 Login
```

But your application doesn't necessarily configure all of them.

If you configure:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt())
```

you're saying:

```text
Bearer JWT authentication
```

is supported.

You aren't automatically enabling:

```text
HTTP Basic
Form Login
OAuth2 Login
```

---

# 26. `oauth2ResourceServer` vs `oauth2Login`

Very common interview question.

### Resource Server

```java
.oauth2ResourceServer(oauth2 ->
    oauth2.jwt()
)
```

Means:

> "I'm an API. Clients send me access tokens."

Architecture:

```text
Client
  │
  │ Bearer Token
  ▼
Spring API
```

### OAuth2 Login

```java
.oauth2Login(...)
```

Means:

> "My application wants users to log in using an OAuth2/OIDC provider."

Architecture:

```text
Browser
   │
   ▼
My Application
   │
   │ redirect
   ▼
Auth0 / Google / Okta
   │
   ▼
Login
   │
   ▼
My Application
```

Don't confuse these.

---

# 27. One `SecurityFilterChain` can contain many filters

This is the direct answer to your question.

You write:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
```

You don't normally write:

```java
@Bean
Filter filter1()

@Bean
Filter filter2()

@Bean
Filter filter3()
```

for standard Spring Security functionality.

Instead, you configure:

```java
HttpSecurity
```

and Spring Security builds the necessary filters.

```text
Your configuration
       │
       ▼
HttpSecurity
       │
       ▼
http.build()
       │
       ▼
SecurityFilterChain
       │
       ├── Filter A
       ├── Filter B
       ├── Filter C
       ├── Filter D
       └── Filter E
```

---

# 28. Can we create our own filter?

Yes.

For example, a custom filter:

```java
@Component
public class MyCustomFilter
        extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // custom logic

        filterChain.doFilter(request, response);
    }
}
```

And then:

```java
http.addFilterBefore(
    myCustomFilter,
    BearerTokenAuthenticationFilter.class
);
```

Now the chain becomes conceptually:

```text
Filter A
   ↓
MyCustomFilter
   ↓
BearerTokenAuthenticationFilter
   ↓
Filter B
   ↓
...
```

But **don't create custom JWT filters just because tutorials do it**.

For standard JWT bearer authentication:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt())
```

is generally the preferred Spring Security approach.

---

# 29. Why `OncePerRequestFilter`?

You will frequently see:

```java
extends OncePerRequestFilter
```

in custom Spring Security implementations.

It provides a convenient base for a filter intended to execute once per request dispatch in the normal servlet processing model.

Example use cases:

```text
Correlation ID
Custom authentication
Request auditing
Custom security checks
```

But again:

```text
Standard JWT
     ↓
Use OAuth2 Resource Server
```

rather than:

```text
Write custom JWT filter
     ↓
Extract token
     ↓
Parse JWT
     ↓
Verify signature
     ↓
Create Authentication
```

unless you genuinely have custom requirements.

---

# 30. Multiple `SecurityFilterChain`s

Now the advanced interview question.

Can we have:

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) {
    ...
}
```

and:

```java
@Bean
SecurityFilterChain adminSecurity(HttpSecurity http) {
    ...
}
```

?

**Yes.**

For example:

```text
                 FilterChainProxy
                       │
             ┌─────────┴──────────┐
             │                    │
          /api/**              /admin/**
             │                    │
             ▼                    ▼
      API Filter Chain      Admin Filter Chain
             │                    │
       JWT authentication    different rules
```

Spring Security selects the appropriate chain based on the request matcher.

This is useful when genuinely different areas of an application require substantially different security mechanisms.

---

# 31. Don't confuse multiple filters with multiple chains

This is a classic interview distinction.

### One chain

```text
SecurityFilterChain
│
├── Filter 1
├── Filter 2
├── Filter 3
├── Filter 4
└── Filter 5
```

### Multiple chains

```text
FilterChainProxy
│
├── SecurityFilterChain A
│     ├── Filter 1
│     ├── Filter 2
│     └── Filter 3
│
└── SecurityFilterChain B
      ├── Filter 4
      ├── Filter 5
      └── Filter 6
```

---

# 32. Your configuration mapped to the architecture

Your code:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/swagger-ui/**",
            "/v3/api-docs/**"
        ).permitAll()
        .anyRequest().authenticated()
    )
    .oauth2ResourceServer(oauth2 ->
        oauth2.jwt()
    );
```

Think:

```text
                 HttpSecurity
                     │
       ┌─────────────┴──────────────┐
       │                            │
       ▼                            ▼
Authorization                 OAuth2 Resource
Configuration                 Server Configuration
       │                            │
       │                            ▼
       │                 Bearer Token Authentication
       │                            │
       │                            ▼
       │                    AuthenticationManager
       │                            │
       │                            ▼
       │                    JwtAuthenticationProvider
       │                            │
       │                            ▼
       │                       JwtDecoder
       │
       ▼
AuthorizationFilter
       │
       ▼
Rules:
  /swagger-ui/** → permitAll
  /v3/api-docs/** → permitAll
  everything else → authenticated
```

---

# 33. The complete request flow

For:

```http
GET /api/orders
Authorization: Bearer <JWT>
```

you can explain it in an interview like this:

```text
1. HTTP request arrives
          ↓
2. Servlet container invokes Spring Security
          ↓
3. SecurityFilterChain processes request
          ↓
4. BearerTokenAuthenticationFilter extracts token
          ↓
5. AuthenticationManager receives authentication request
          ↓
6. JwtAuthenticationProvider handles JWT authentication
          ↓
7. JwtDecoder validates token
          ↓
8. Authentication is created
          ↓
9. Authentication is placed in SecurityContext
          ↓
10. AuthorizationFilter evaluates authorization rules
          ↓
11. authenticated() succeeds
          ↓
12. Request continues
          ↓
13. DispatcherServlet
          ↓
14. Controller
```

That is a **very strong interview answer**.

---

# 34. What exactly is `SecurityContext` doing?

After successful authentication:

```text
JWT
 │
 ▼
Authentication
 │
 ▼
SecurityContext
```

The context contains something like:

```text
SecurityContext
      │
      └── Authentication
             │
             ├── Principal
             ├── Authorities
             └── authenticated = true
```

Then authorization can ask:

```java
authentication.getAuthorities()
```

and determine:

```text
Can this user access /api/orders?
```

---

# 35. Why do we need both authentication and authorization filters?

Because they solve different problems.

Authentication:

```text
WHO are you?
```

Authorization:

```text
WHAT are you allowed to do?
```

Example:

```text
JWT
 │
 ▼
Authentication
 │
 └── user = riyaz
     authorities = [SCOPE_orders:read]
 │
 ▼
Authorization
 │
 └── DELETE /orders/123 requires orders:delete
 │
 ▼
DENY
```

The user is authenticated.

But not authorized.

Therefore:

```text
403 Forbidden
```

---

# 36. Interview traps

### Q: Is `HttpSecurity` itself a filter?

**No.**

It is a configuration builder.

---

### Q: Is `SecurityFilterChain` one filter?

**No.**

It is a chain containing multiple filters.

---

### Q: Does `authorizeHttpRequests()` create an authorization filter?

Conceptually yes, it configures authorization infrastructure, including the `AuthorizationFilter` used to enforce the request authorization rules.

---

### Q: Does `oauth2ResourceServer()` create one filter?

It configures multiple pieces of infrastructure, including bearer-token authentication processing. A key filter is `BearerTokenAuthenticationFilter`.

---

### Q: Does `permitAll()` bypass the entire SecurityFilterChain?

**No.**

It means the authorization rule permits the request without requiring authentication.

---

### Q: Does `authenticated()` authenticate the user?

Not exactly.

It is an **authorization rule** saying:

> The current request must have an authenticated `Authentication`.

The actual authentication might already have been performed by a bearer token filter, session mechanism, HTTP Basic, etc.

---

### Q: Does `BearerTokenAuthenticationFilter` validate everything itself?

No.

It extracts the bearer token and delegates into Spring Security's authentication infrastructure, which can involve:

```text
AuthenticationManager
 → JwtAuthenticationProvider
 → JwtDecoder
```

---

### Q: Does JWT authentication happen in the controller?

No.

It happens in the security filter chain **before the controller**.

---

### Q: Do we need a custom JWT filter?

For a standard OAuth2 Resource Server:

**Usually no.**

Use:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt())
```

---

# 37. The 5 objects you should be able to explain

For interviews, memorize this relationship:

```text
HttpSecurity
     │
     │ builds
     ▼
SecurityFilterChain
     │
     │ contains
     ▼
Security Filters
     │
     │ authentication
     ▼
AuthenticationManager
     │
     ▼
AuthenticationProvider
     │
     ▼
Authentication
     │
     ▼
SecurityContext
```

And then:

```text
SecurityContext
      │
      ▼
AuthorizationFilter
      │
      ▼
Authorization decision
```

---

# 38. The simplest mental model

If you remember only one diagram, remember this:

```text
                    HttpSecurity
                         │
                         │ build()
                         ▼
                SecurityFilterChain
                         │
             ┌───────────┴───────────┐
             │                       │
       Authentication          Authorization
             │                       │
             ▼                       ▼
     BearerTokenFilter       AuthorizationFilter
             │                       │
             ▼                       ▼
     AuthenticationManager      Rules
             │                       │
             ▼                       │
     JwtAuthenticationProvider       │
             │                       │
             ▼                       │
        JwtDecoder                   │
             │                       │
             ▼                       │
       Authentication ───────────────┘
             │
             ▼
      SecurityContext
             │
             ▼
         Controller
```

---

## One-line interview answer

If the interviewer asks:

> **"When I write `http.authorizeHttpRequests().oauth2ResourceServer()`, do I create one filter or multiple filters?"**

Say:

> **"`HttpSecurity` is a configuration builder. Calling `http.build()` creates a `SecurityFilterChain`, and that chain contains multiple security filters. `authorizeHttpRequests()` configures authorization, which is enforced by the authorization infrastructure/filter, while `oauth2ResourceServer().jwt()` configures bearer-token authentication, including the `BearerTokenAuthenticationFilter` and JWT authentication infrastructure. So we're configuring one filter chain containing multiple filters, not creating one single security filter."**

That's the core answer interviewers are looking for.

