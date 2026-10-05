# Chapter 6 — CSRF, CORS & Security Protections

This chapter is important because **CSRF and CORS are frequently confused in interviews**.

The easiest way to understand them is to first understand the problem each one solves.

---

# 1. Two completely different problems

### CSRF

CSRF asks:

> **Can another website trick the user's browser into performing an authenticated action on my application?**

### CORS

CORS asks:

> **Is a browser allowed to let JavaScript from one origin read a response from another origin?**

So:

```text
CSRF → unwanted authenticated actions

CORS → browser cross-origin access rules
```

They are **not the same thing**.

---

# 2. First understand "origin"

An origin consists of:

```text
scheme + host + port
```

For example:

```text
https://example.com:443
```

Compare:

```text
https://example.com
https://api.example.com
```

These are different origins because the hosts differ.

Similarly:

```text
http://example.com
https://example.com
```

are different origins because the schemes differ.

And:

```text
https://example.com:443
https://example.com:8080
```

are different origins because the ports differ.

---

# 3. Why browsers have same-origin policy

Imagine you are logged into:

```text
https://bank.com
```

Your browser has:

```text
Cookie: SESSION_ID=abc123
```

Now you visit:

```text
https://evil.com
```

Suppose `evil.com` contains JavaScript:

```javascript
fetch("https://bank.com/transfer", {
    method: "POST",
    body: "..."
});
```

If browsers had no security restrictions, websites could freely interact with other websites using your browser.

That's why browsers enforce the **Same-Origin Policy (SOP)**.

Conceptually:

```text
evil.com
   |
   | JavaScript
   ↓
bank.com
   X
Browser restricts cross-origin access
```

CORS is a controlled mechanism for relaxing some of these browser restrictions.

---

# 4. CSRF — the actual problem

Suppose your banking application uses session authentication.

You log in:

```text
POST /login
```

Server creates:

```text
JSESSIONID=abc123
```

Browser stores it.

Now:

```text
You → bank.com
```

You are authenticated.

Then you visit:

```text
evil.com
```

The malicious site tries:

```html
<form action="https://bank.com/transfer"
      method="POST">

    <input name="amount" value="10000">
    <input name="to" value="attacker">
</form>
```

The browser may send the bank's cookie along with the request.

Conceptually:

```text
evil.com
   |
   | malicious request
   ↓
Browser
   |
   | Cookie: JSESSIONID=abc123
   ↓
bank.com
```

The bank sees:

```text
JSESSIONID=abc123
```

and thinks:

> "This is an authenticated user."

The attacker didn't need to know the session ID.

That's CSRF.

---

# 5. Why is CSRF possible?

The important property is:

> **The browser automatically sends authentication credentials such as cookies.**

The attacker doesn't need to steal the cookie.

They simply cause the browser to make a request.

```text
Attacker
   ↓
tricks browser
   ↓
browser automatically attaches cookie
   ↓
server sees authenticated request
```

---

# 6. CSRF protection

The classic solution is a **CSRF token**.

The server gives the legitimate application a random token:

```text
CSRF token = X7a9kP...
```

The client sends it with state-changing requests.

For example:

```http
POST /transfer

Cookie: JSESSIONID=abc123
X-CSRF-TOKEN: X7a9kP...
```

The server verifies:

```text
Session valid?       ✅
CSRF token valid?    ✅
```

Then:

```text
Allow
```

But the malicious site doesn't know the CSRF token:

```text
evil.com
   ↓
POST /transfer

Cookie: JSESSIONID=abc123
X-CSRF-TOKEN: ????
```

Therefore:

```text
CSRF validation → FAIL
```

---

# 7. Why can't the attacker simply read the CSRF token?

Because browser same-origin restrictions prevent an unrelated origin from freely reading your application's protected data.

That's one reason CSRF tokens work together with browser security mechanisms.

---

# 8. Spring Security CSRF

Spring Security enables CSRF protection by default for many browser-oriented applications.

You can explicitly configure it:

```java
http
    .csrf(csrf -> csrf
        .csrfTokenRepository(
            CookieCsrfTokenRepository.withHttpOnlyFalse()
        )
    );
```

For a traditional form-based application:

```text
Browser
  ↓
Session Cookie
  ↓
CSRF Token
  ↓
Spring Security
  ↓
Controller
```

---

# 9. Why do APIs sometimes disable CSRF?

You'll often see:

```java
http.csrf(csrf -> csrf.disable());
```

People sometimes say:

> "CSRF is not needed for APIs."

That's too simplistic.

The correct question is:

> **How is the authentication credential sent and automatically attached?**

---

# 10. Cookie authentication vs Authorization header

Consider a session/cookie application:

```http
Cookie: JSESSIONID=abc123
```

The browser automatically sends the cookie.

Therefore:

```text
Cross-site request
      ↓
Browser
      ↓
Cookie automatically attached
      ↓
Authenticated request
```

CSRF is relevant.

Now consider:

```http
Authorization: Bearer eyJ...
```

The browser does **not normally automatically attach your bearer token to arbitrary cross-site requests** in the same way it automatically handles cookies.

Your application JavaScript must explicitly add it:

```javascript
fetch("/api/profile", {
    headers: {
        "Authorization": "Bearer " + accessToken
    }
});
```

Therefore the classic cookie-based CSRF threat model is different.

For a properly designed stateless bearer-token API, CSRF protection may not be required.

But this depends on the actual architecture.

---

# 11. Important interview distinction

Don't say:

> "JWT doesn't need CSRF."

Say:

> **CSRF protection depends on whether the authentication credential is automatically attached to cross-site requests. A bearer token explicitly supplied in the Authorization header has a different CSRF threat model from an authentication cookie.**

That's a much better interview answer.

---

# 12. CORS

Now let's switch to CORS.

Suppose frontend:

```text
http://localhost:3000
```

calls:

```text
http://localhost:8080/api/users
```

Different origins.

The browser may block the frontend from reading the response unless the server permits that origin.

That's where CORS comes in.

CORS =

> **Cross-Origin Resource Sharing**

---

# 13. Simple CORS example

Frontend:

```text
http://frontend.example.com
```

Backend:

```text
https://api.example.com
```

Frontend sends:

```http
GET /users HTTP/1.1
Origin: http://frontend.example.com
```

Server can respond:

```http
Access-Control-Allow-Origin: http://frontend.example.com
```

The browser sees:

```text
Origin allowed?
     ↓
   YES
     ↓
JavaScript can access response
```

---

# 14. What if server doesn't allow it?

Suppose the server doesn't permit:

```text
http://frontend.example.com
```

The browser may prevent JavaScript from reading the response.

Important:

> CORS is primarily enforced by the **browser**.

It is not a general server-to-server security mechanism.

For example:

```text
Browser
   ↓
CORS rules apply
```

But:

```text
Backend A
   ↓
Backend B
```

doesn't have browser CORS enforcement.

---

# 15. Preflight request

This is another very common interview question.

For certain cross-origin requests, the browser first sends:

```http
OPTIONS
```

This is called a **preflight request**.

Example:

```text
Frontend
   |
   | OPTIONS /api/users
   ↓
Backend
```

The browser asks:

```http
Origin: http://localhost:3000
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Authorization, Content-Type
```

The server responds:

```http
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST
Access-Control-Allow-Headers: Authorization, Content-Type
```

Then the browser decides whether the actual request is allowed.

Flow:

```text
Browser
   |
   | OPTIONS /api/users
   ↓
Server
   |
   | CORS permission
   ↓
Browser
   |
   | POST /api/users
   ↓
Server
```

---

# 16. Why does `Authorization` often trigger preflight?

Suppose frontend sends:

```http
Authorization: Bearer eyJ...
```

This is a non-simple request header.

The browser may therefore perform a preflight before sending the actual request.

```text
OPTIONS
    ↓
CORS validation
    ↓
POST/GET/etc.
```

This is why frontend applications often suddenly encounter:

```text
CORS error
```

when adding Authorization headers.

---

# 17. Configuring CORS in Spring Security

Example:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .cors(Customizer.withDefaults())

        .csrf(csrf -> csrf.disable())

        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )

        .oauth2ResourceServer(oauth2 -> oauth2.jwt());

    return http.build();
}
```

Then configure allowed origins:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration =
            new CorsConfiguration();

    configuration.setAllowedOrigins(
            List.of("http://localhost:3000")
    );

    configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
    );

    configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type")
    );

    UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
            "/**",
            configuration
    );

    return source;
}
```

---

# 18. Never blindly use `*`

You might see:

```java
configuration.setAllowedOrigins(
    List.of("*")
);
```

This means any origin can be allowed for the relevant CORS configuration.

That's generally too permissive for production applications.

Prefer explicitly trusted origins:

```text
https://app.example.com
https://admin.example.com
```

instead of:

```text
*
```

Especially when credentials/cookies are involved.

---

# 19. CORS with credentials

Suppose your frontend uses cookies.

Then:

```javascript
fetch("https://api.example.com/profile", {
    credentials: "include"
});
```

The server needs appropriate CORS configuration.

Conceptually:

```http
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
```

An important rule:

> You cannot use `Access-Control-Allow-Origin: *` together with credentialed CORS in the way people commonly expect.

When credentials are involved, explicitly specify trusted origins.

---

# 20. CSRF vs CORS

This table is worth memorizing.

| CSRF                                            | CORS                                             |
| ----------------------------------------------- | ------------------------------------------------ |
| Cross-Site Request Forgery                      | Cross-Origin Resource Sharing                    |
| Protects against unwanted authenticated actions | Controls browser cross-origin access             |
| Mainly a request-forgery problem                | Mainly a browser response-sharing/access problem |
| Important with cookie-based authentication      | Important for browser frontend → API calls       |
| Uses CSRF tokens and other defenses             | Uses HTTP CORS headers                           |
| Not replaced by CORS                            | Not replaced by CSRF                             |

---

# 21. The easiest way to remember

Ask two questions.

### Question 1

> "Can another website cause an authenticated action?"

Think:

```text
CSRF
```

### Question 2

> "Can JavaScript from another origin read my API response?"

Think:

```text
CORS
```

---

# 22. CSRF vs XSS

Another interview favorite.

### XSS

Attacker gets malicious JavaScript executed in your application.

```text
Attacker
   ↓
malicious JavaScript
   ↓
your website
```

### CSRF

Attacker tricks your browser into making an authenticated request.

```text
Attacker website
      ↓
Browser
      ↓
Authenticated request
      ↓
Your website
```

They are different attacks.

And importantly:

> Strong CSRF protection does not automatically protect against XSS.

You need separate defenses.

---

# 23. Cookie security

If authentication uses cookies, you should understand these attributes.

### `HttpOnly`

```http
Set-Cookie: SESSION=abc123; HttpOnly
```

JavaScript cannot normally access the cookie through:

```javascript
document.cookie
```

This helps reduce cookie theft through certain XSS scenarios.

---

### `Secure`

```http
Set-Cookie: SESSION=abc123; Secure
```

Cookie should only be sent over HTTPS.

---

### `SameSite`

Examples:

```text
SameSite=Strict
SameSite=Lax
SameSite=None
```

This controls when cookies are sent in cross-site contexts and is an important part of modern CSRF defense.

---

# 24. Defense in depth

Security should not depend on one mechanism.

For a browser/session application:

```text
HTTPS
  +
Secure cookies
  +
HttpOnly
  +
SameSite
  +
CSRF protection
  +
Authentication
  +
Authorization
  +
Input validation
  +
XSS defenses
```

Each addresses different risks.

---

# 25. What should a typical API do?

For a stateless REST API using bearer access tokens:

```text
Client
   |
   | Authorization: Bearer JWT
   ↓
API
```

Typical configuration might be:

```java
http
    .csrf(csrf -> csrf.disable())

    .sessionManagement(session -> session
        .sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        )
    )

    .cors(Customizer.withDefaults())

    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/public/**").permitAll()
        .anyRequest().authenticated()
    )

    .oauth2ResourceServer(oauth2 ->
        oauth2.jwt()
    );
```

The important thing is **why** CSRF is disabled.

Not:

> "Because REST APIs don't need CSRF."

Instead:

> "Because this API uses stateless bearer-token authentication rather than browser-managed authentication cookies, so the traditional CSRF threat model does not apply in the same way."

---

# 26. What about JWT stored in a cookie?

This is where people get into trouble.

Suppose you put:

```text
JWT
 ↓
Cookie
```

instead of:

```text
Authorization: Bearer JWT
```

The browser automatically sends the cookie.

Now the CSRF threat model can come back.

So:

```text
JWT + Authorization header
        ↓
Different CSRF characteristics


JWT + Cookie
        ↓
CSRF becomes relevant
```

This is an excellent interview point.

**JWT itself does not determine whether CSRF matters. How the credential is transported matters.**

---

# 27. 401 vs 403 in this context

Suppose JWT is missing:

```http
GET /profile
```

No authentication.

Usually:

```text
401 Unauthorized
```

Suppose JWT is valid:

```text
User = USER
```

but endpoint requires:

```text
ROLE_ADMIN
```

Then:

```text
403 Forbidden
```

So:

```text
Missing/invalid authentication
        ↓
       401


Authenticated but insufficient permission
        ↓
       403
```

---

# 28. Interview scenario

### Interviewer:

> "We have a React frontend and Spring Boot backend. The frontend is hosted on `app.example.com`, and backend on `api.example.com`. The browser is reporting CORS errors. What would you check?"

Good answer:

> I'd first verify that the request is actually cross-origin and inspect the browser's preflight request if one exists. On the Spring side, I'd configure CORS with the trusted frontend origin, allowed HTTP methods, and required headers such as `Authorization` and `Content-Type`. I'd also make sure the CORS configuration is integrated with Spring Security and that OPTIONS requests aren't unintentionally blocked by authorization rules.

---

# 29. Another interview scenario

### Interviewer:

> "Why did we disable CSRF in our JWT API?"

Weak answer:

> "Because JWT doesn't need CSRF."

Better:

> "Our API uses stateless bearer authentication where the access token is explicitly supplied in the Authorization header rather than automatically attached by the browser as a cookie. Therefore the traditional cookie-based CSRF attack doesn't apply in the same way. If we moved authentication into cookies, we'd need to reassess CSRF protection."

That's the answer I'd give in an interview.

---

# 30. Final mental model

Keep these three concepts separate:

```text
                 Browser Security
                       |
          ┌────────────┼────────────┐
          ↓            ↓            ↓
        SOP          CORS          CSRF
          |            |            |
          |            |            |
    Same-origin    Cross-origin   Forged
      rules        permissions    requests
```

And Spring Security:

```text
                    HTTP Request
                         |
              ┌──────────┴──────────┐
              ↓                     ↓
          Authentication        Security
              |                   protections
              ↓                     |
       SecurityContext        ┌─────┴─────┐
              |               ↓           ↓
              ↓             CSRF        CORS
        Authorization
              |
              ↓
         Controller
```

---

## Interview cheat sheet

```text
CSRF
→ Prevents forged authenticated requests.

CORS
→ Controls browser cross-origin access.

SOP
→ Browser's same-origin security model.

Preflight
→ Browser's OPTIONS request used to check CORS permissions.

CSRF Token
→ Secret/unpredictable value required with state-changing requests.

HttpOnly
→ JavaScript cannot normally read the cookie.

Secure
→ Cookie should be sent over HTTPS.

SameSite
→ Controls cross-site cookie sending.

JWT + Authorization header
→ Different CSRF threat model from cookies.

JWT + Cookie
→ CSRF can become relevant.

CORS ≠ CSRF
→ They solve different problems.
```

**Next: Chapter 7 — OAuth2 & OpenID Connect (OIDC)**. This is where we'll connect login with Google/GitHub, authorization servers, resource servers, access tokens, scopes, OAuth2 flows, and the difference between **OAuth2 authorization** and **OIDC authentication**.

