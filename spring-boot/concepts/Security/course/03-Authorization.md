# Chapter 3 — Authorization ⭐⭐⭐

Now that we understand authentication, we can answer the next question:

> **The user is authenticated. What is this user allowed to do?**

That's **authorization**.

---

# 1. Authentication vs Authorization

Suppose:

```text
User: riyaz
```

has successfully logged in.

Authentication says:

```text
✅ This is Riyaz.
```

Authorization asks:

```text
❓ What can Riyaz do?
```

For example:

```text
                 Riyaz
                   |
          ┌────────┼────────┐
          ↓        ↓        ↓
       View      Create    Delete
       users     users     users
          ✅        ✅        ❌
```

Maybe Riyaz has:

```text
ROLE_USER
```

while an administrator has:

```text
ROLE_ADMIN
```

---

# 2. Where Does Authorization Happen?

Remember our previous flow:

```text
HTTP Request
     ↓
SecurityFilterChain
     ↓
Authentication
     ↓
SecurityContext
     ↓
Authorization
     ↓
Controller
```

The user must generally be authenticated **before Spring Security can make an authorization decision based on that user's authorities**.

Example:

```text
GET /admin/users
        |
        ↓
Is user authenticated?
        |
        ↓
What authorities does user have?
        |
        ↓
Does user have ROLE_ADMIN?
        |
     ┌──┴──┐
    Yes    No
     |      |
     ↓      ↓
  Allow    403
```

---

# 3. What is an Authority?

Spring Security represents permissions using:

```java
GrantedAuthority
```

For example:

```text
ROLE_USER
ROLE_ADMIN
READ_ACCOUNT
WRITE_ACCOUNT
DELETE_ACCOUNT
```

An authenticated user can have multiple authorities:

```text
User: riyaz

Authorities:
    ROLE_USER
    READ_ACCOUNT
    WRITE_ACCOUNT
```

Authorization checks these authorities.

---

# 4. Roles vs Authorities ⭐⭐⭐

This causes a lot of confusion in Spring Security.

Technically:

> **A role is represented as a `GrantedAuthority`.**

But Spring Security gives special treatment to role names.

For example:

```text
ROLE_ADMIN
```

is an authority.

But when you write:

```java
hasRole("ADMIN")
```

Spring Security generally checks for:

```text
ROLE_ADMIN
```

not:

```text
ADMIN
```

So:

```java
hasRole("ADMIN")
```

is conceptually similar to:

```java
hasAuthority("ROLE_ADMIN")
```

### Important

```text
hasRole("ADMIN")
       ↓
checks
       ↓
ROLE_ADMIN
```

Whereas:

```java
hasAuthority("ADMIN")
```

checks exactly:

```text
ADMIN
```

---

# 5. `hasRole()` vs `hasAuthority()` ⭐⭐⭐

Suppose the user has:

```text
ROLE_ADMIN
```

Then:

```java
hasRole("ADMIN")
```

✅ matches.

And:

```java
hasAuthority("ROLE_ADMIN")
```

✅ matches.

But:

```java
hasAuthority("ADMIN")
```

❌ does not match.

Because the actual authority is:

```text
ROLE_ADMIN
```

### Easy rule

```text
hasRole("ADMIN")
       ↓
Spring adds ROLE_
       ↓
ROLE_ADMIN
```

```text
hasAuthority("ROLE_ADMIN")
       ↓
exact authority
       ↓
ROLE_ADMIN
```

---

# 6. URL-Based Authorization

Let's modify our previous configuration.

Suppose we have:

```text
GET /hello
GET /admin
GET /profile
```

We want:

```text
/hello
    → anyone

/profile
    → authenticated users

/admin
    → ADMIN only
```

Configuration:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/hello").permitAll()
            .requestMatchers("/admin").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .httpBasic();

    return http.build();
}
```

Now:

```text
/hello
   ↓
PUBLIC

/profile
   ↓
AUTHENTICATED

/admin
   ↓
ROLE_ADMIN required
```

---

# 7. `permitAll()`

```java
.requestMatchers("/hello").permitAll()
```

means:

> Anyone can access this endpoint.

Authentication isn't required.

Example:

```text
GET /hello

Anonymous user
      ↓
permitAll()
      ↓
✅ allowed
```

Typical public endpoints:

```text
/login
/signup
/forgot-password
/health
/public/**
```

Although the exact choice depends on the application.

---

# 8. `authenticated()`

```java
.anyRequest().authenticated()
```

means:

> The request requires an authenticated user.

Example:

```text
GET /profile

Anonymous
    ↓
❌ 401

Authenticated USER
    ↓
✅ allowed
```

It doesn't require a particular role.

---

# 9. `hasRole()`

Suppose:

```java
.requestMatchers("/admin/**")
    .hasRole("ADMIN")
```

This means:

```text
/admin/**
     ↓
ROLE_ADMIN required
```

So:

```text
ROLE_ADMIN
    ↓
✅ allowed

ROLE_USER
    ↓
❌ 403
```

---

# 10. `hasAuthority()`

Suppose we define:

```java
.requestMatchers("/reports/**")
    .hasAuthority("REPORT_READ")
```

Then the user must have exactly:

```text
REPORT_READ
```

For example:

```text
User authorities:

ROLE_USER
REPORT_READ
```

→ allowed.

But:

```text
ROLE_USER
```

→ denied.

---

# 11. Roles Are Usually Coarse-Grained

Imagine an application:

```text
ADMIN
USER
MANAGER
```

These are relatively broad categories.

```text
ROLE_ADMIN
ROLE_MANAGER
ROLE_USER
```

You can think of them as **job/access categories**.

Authorities can be more granular:

```text
USER_READ
USER_CREATE
USER_UPDATE
USER_DELETE

REPORT_READ
REPORT_EXPORT

PAYMENT_READ
PAYMENT_REFUND
```

So a common model is:

```text
Roles
  ↓
coarse-grained access

Authorities/permissions
  ↓
fine-grained access
```

Spring Security supports both.

---

# 12. Example

Suppose:

```text
Alice
ROLE_USER

Bob
ROLE_ADMIN
```

Authorization:

```java
.requestMatchers("/users")
    .hasRole("USER")

.requestMatchers("/admin")
    .hasRole("ADMIN")
```

Then:

```text
Alice
  |
  ├── /users → ✅
  └── /admin → ❌

Bob
  |
  ├── /users → maybe ❌
  └── /admin → ✅
```

Whether Bob can access `/users` depends on whether he also has `ROLE_USER`.

**Roles aren't automatically hierarchical.**

That's important.

---

# 13. Role Hierarchy

You might want:

```text
ADMIN
  ↓
MANAGER
  ↓
USER
```

meaning:

```text
ADMIN can do everything MANAGER can do.

MANAGER can do everything USER can do.
```

Spring Security can support role hierarchies.

Conceptually:

```text
ROLE_ADMIN
    ↓
ROLE_MANAGER
    ↓
ROLE_USER
```

But don't assume this happens automatically.

Without explicit configuration:

```text
ROLE_ADMIN
```

does **not automatically mean**:

```text
ROLE_USER
```

This is a common misconception.

---

# 14. Method-Level Authorization ⭐⭐⭐

URL-level authorization isn't the only option.

We can also protect individual service/controller methods.

For example:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
    ...
}
```

Now the method itself requires:

```text
ROLE_ADMIN
```

---

# 15. Enabling Method Security

Modern Spring Security commonly uses:

```java
@EnableMethodSecurity
```

For example:

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    ...
}
```

Then:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
    ...
}
```

---

# 16. Why Method Security?

Imagine:

```text
Controller
    ↓
Service
    ↓
Repository
```

You could protect only the controller:

```text
Controller
    ↓
"ADMIN required"
    ↓
Service
```

But another controller might accidentally call the same service method without the same check.

Method-level security can protect the actual method:

```text
Controller A ──┐
               ↓
           Service
               ↓
       @PreAuthorize
               ↓
          Authorization
```

This can provide an additional security boundary.

---

# 17. `@PreAuthorize`

One of the most commonly used annotations:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long userId) {
    ...
}
```

Or:

```java
@PreAuthorize("hasAuthority('USER_DELETE')")
public void deleteUser(Long userId) {
    ...
}
```

You can also combine conditions.

For example:

```java
@PreAuthorize(
    "hasRole('ADMIN') or hasAuthority('USER_DELETE')"
)
public void deleteUser(Long userId) {
    ...
}
```

---

# 18. Authorization Based on the Current User

Method security can also use the authenticated user.

For example:

```java
@PreAuthorize("#userId == authentication.principal.id")
public User getUser(Long userId) {
    ...
}
```

Conceptually:

```text
Current user ID
      |
      ↓
Does it equal requested user ID?
      |
   ┌──┴──┐
  Yes    No
   |      |
   ↓      ↓
 Allow   Deny
```

This is useful for rules like:

> A user can access their own profile but not someone else's.

---

# 19. `authentication` in SpEL

In:

```java
@PreAuthorize(...)
```

you can access the current authentication.

For example:

```java
@PreAuthorize(
    "authentication.name == #username"
)
public User getProfile(String username) {
    ...
}
```

Here:

```text
authentication
       ↓
current Authentication
       ↓
current user
```

This is another reason understanding `SecurityContext` from Chapter 2 is important.

---

# 20. URL Authorization vs Method Authorization

### URL-level

```java
.requestMatchers("/admin/**")
    .hasRole("ADMIN")
```

Protects based on:

```text
HTTP request
URL
HTTP method
```

### Method-level

```java
@PreAuthorize("hasRole('ADMIN')")
```

Protects based on:

```text
Java method invocation
```

A real application can use **both**.

For example:

```text
HTTP Request
     ↓
SecurityFilterChain
     ↓
URL authorization
     ↓
Controller
     ↓
Service
     ↓
Method authorization
     ↓
Database
```

---

# 21. HTTP Method-Based Authorization

You can also distinguish:

```text
GET
POST
PUT
DELETE
```

For example:

```java
http.authorizeHttpRequests(auth -> auth

    .requestMatchers(HttpMethod.GET, "/users/**")
        .hasAuthority("USER_READ")

    .requestMatchers(HttpMethod.POST, "/users/**")
        .hasAuthority("USER_CREATE")

    .requestMatchers(HttpMethod.DELETE, "/users/**")
        .hasAuthority("USER_DELETE")

    .anyRequest().authenticated()
);
```

Now:

```text
GET /users
    ↓
USER_READ

POST /users
    ↓
USER_CREATE

DELETE /users/10
    ↓
USER_DELETE
```

This is more fine-grained than simply checking a role.

---

# 22. Important Ordering Rule ⭐

Consider:

```java
http.authorizeHttpRequests(auth -> auth

    .anyRequest().authenticated()

    .requestMatchers("/admin/**")
        .hasRole("ADMIN")
);
```

This is problematic because:

```text
anyRequest()
```

is a catch-all rule.

You generally want specific rules first:

```java
http.authorizeHttpRequests(auth -> auth

    .requestMatchers("/admin/**")
        .hasRole("ADMIN")

    .requestMatchers("/public/**")
        .permitAll()

    .anyRequest()
        .authenticated()
);
```

Think:

```text
Specific rules
      ↓
General fallback
```

---

# 23. Complete Example

Let's extend our previous application.

### Security configuration

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails user = User.builder()
                .username("riyaz")
                .password(passwordEncoder.encode("secret123"))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/hello")
                    .permitAll()

                .requestMatchers("/admin/**")
                    .hasRole("ADMIN")

                .anyRequest()
                    .authenticated()
            )
            .httpBasic();

        return http.build();
    }
}
```

---

# 24. Controllers

```java
@RestController
public class UserController {

    @GetMapping("/hello")
    public String hello() {
        return "Public endpoint";
    }

    @GetMapping("/profile")
    public String profile() {
        return "Authenticated user profile";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "Admin dashboard";
    }
}
```

Now:

```text
/hello
   ↓
permitAll
   ↓
Anyone
```

```text
/profile
   ↓
authenticated
   ↓
USER or ADMIN
```

```text
/admin/dashboard
   ↓
ROLE_ADMIN
   ↓
ADMIN only
```

---

# 25. Test It

### Public endpoint

```bash
curl http://localhost:8080/hello
```

Result:

```text
Public endpoint
```

No credentials required.

---

### Profile without authentication

```bash
curl http://localhost:8080/profile
```

Result:

```text
401 Unauthorized
```

---

### Profile as USER

```bash
curl -u riyaz:secret123 \
     http://localhost:8080/profile
```

Result:

```text
Authenticated user profile
```

---

### Admin endpoint as USER

```bash
curl -u riyaz:secret123 \
     http://localhost:8080/admin/dashboard
```

Result:

```text
403 Forbidden
```

Why?

```text
Authentication
    ↓
SUCCESS

Authorization
    ↓
ROLE_ADMIN?
    ↓
NO
    ↓
403
```

---

### Admin endpoint as ADMIN

```bash
curl -u admin:admin123 \
     http://localhost:8080/admin/dashboard
```

Result:

```text
Admin dashboard
```

---

# 26. 401 vs 403 — Very Important ⭐⭐⭐

This should become automatic in interviews.

### No authentication

```text
GET /admin/dashboard

No credentials
      ↓
Authentication fails
      ↓
401 Unauthorized
```

### Wrong authority

```text
USER
  ↓
GET /admin/dashboard
  ↓
Authentication succeeds
  ↓
Authorization fails
  ↓
403 Forbidden
```

Therefore:

```text
401 → "I don't know/accept who you are."

403 → "I know who you are, but you're not allowed."
```

Technically, the exact semantics can vary by authentication scheme and configuration, but this is the standard Spring Security interview mental model.

---

# 27. Important Interview Trap: `ROLE_`

Suppose you create:

```java
.roles("ADMIN")
```

Spring Security treats this as:

```text
ROLE_ADMIN
```

So:

```java
.hasRole("ADMIN")
```

✅ works.

But:

```java
.hasAuthority("ADMIN")
```

❌ doesn't.

Because the actual authority is:

```text
ROLE_ADMIN
```

To use `hasAuthority()`:

```java
.hasAuthority("ROLE_ADMIN")
```

✅ works.

### Remember

```text
.roles("ADMIN")
        ↓
ROLE_ADMIN
```

```text
.hasRole("ADMIN")
        ↓
ROLE_ADMIN
```

```text
.hasAuthority("ROLE_ADMIN")
        ↓
ROLE_ADMIN
```

---

# 28. Roles vs Permissions — Interview Answer

**Question:**

> What's the difference between a role and an authority in Spring Security?

Good answer:

> A `GrantedAuthority` represents an authority or permission used during authorization. Roles are also represented as authorities, but Spring Security conventionally prefixes role names with `ROLE_`. Therefore `hasRole("ADMIN")` typically checks for `ROLE_ADMIN`, while `hasAuthority("ROLE_ADMIN")` checks the authority directly.

---

# 29. The Complete Authorization Picture

```text
                    HTTP Request
                         |
                         ↓
               SecurityFilterChain
                         |
                         ↓
                  Authentication
                         |
                         ↓
                  SecurityContext
                         |
                         ↓
             ┌─────────────────────┐
             │ Authorization Rules │
             └──────────┬──────────┘
                        |
          ┌─────────────┼─────────────┐
          ↓             ↓             ↓
      permitAll()   authenticated()  hasRole()
                                      |
                                      ↓
                               hasAuthority()
                                      |
                                      ↓
                                Allow / Deny
                                  /       \
                                ↓           ↓
                           Controller      403
```

---

# 30. Interview Questions

### Q1. What is authorization?

> Authorization determines whether an authenticated user has sufficient permissions or authorities to access a resource or perform an operation.

---

### Q2. What is `GrantedAuthority`?

> It represents an authority or permission granted to an authenticated user and is used by Spring Security during authorization.

---

### Q3. `hasRole()` vs `hasAuthority()`?

> `hasRole("ADMIN")` uses the role convention and typically checks for `ROLE_ADMIN`, whereas `hasAuthority("ROLE_ADMIN")` checks the authority directly.

---

### Q4. Does `roles("ADMIN")` create `ADMIN`?

No.

It creates:

```text
ROLE_ADMIN
```

---

### Q5. Does `ROLE_ADMIN` automatically include `ROLE_USER`?

**No.**

Unless you explicitly configure a role hierarchy or grant both authorities.

---

### Q6. What does `authenticated()` mean?

> The request requires an authenticated user, but doesn't require a specific role or authority.

---

### Q7. What does `permitAll()` mean?

> The request is accessible without authentication.

---

### Q8. What is `@PreAuthorize`?

> It is a method-security annotation that allows authorization rules to be evaluated before a method executes.

Example:

```java
@PreAuthorize("hasRole('ADMIN')")
```

---

### Q9. Can we use both URL and method-level authorization?

Yes.

A common architecture is:

```text
URL authorization
       ↓
Controller
       ↓
Method authorization
       ↓
Service
```

---

# 31. Interview Scenario

**Interviewer:**

> A user has successfully logged in but receives HTTP 403 when accessing an admin API. What would you check?

Good answer:

```text
1. Authentication succeeded?
       ↓
   Yes

2. What authorities are present?
       ↓
   ROLE_USER

3. What does endpoint require?
       ↓
   ROLE_ADMIN

4. USER != ADMIN
       ↓
   Authorization denied
       ↓
   403
```

Then check:

* Is the correct role/authority being loaded?
* Is `ROLE_` being handled correctly?
* Is `hasRole()` vs `hasAuthority()` correct?
* Is the authorization rule matching the intended endpoint?
* Is there a method-level `@PreAuthorize` denying the request?

That's a very realistic interview answer.

---

# 32. Chapter 3 Cheat Sheet

```text
AUTHENTICATION
    ↓
Who are you?

AUTHORIZATION
    ↓
What can you do?

GrantedAuthority
    ↓
Permission/authority assigned to user

Role
    ↓
Conventionally an authority beginning with ROLE_

hasRole("ADMIN")
    ↓
checks ROLE_ADMIN

hasAuthority("ROLE_ADMIN")
    ↓
checks exact authority

authenticated()
    ↓
Any authenticated user

permitAll()
    ↓
Anyone

@PreAuthorize(...)
    ↓
Method-level authorization

401
    ↓
Authentication problem

403
    ↓
Authorization denied
```

## The most important mental model

```text
User logs in
     ↓
Authentication succeeds
     ↓
SecurityContext contains:
     |
     ├── Principal
     |
     └── Authorities
              |
              ├── ROLE_USER
              ├── READ_ACCOUNT
              └── WRITE_ACCOUNT
                       |
                       ↓
              Authorization rule
                       |
                 ┌─────┴─────┐
                 ↓           ↓
               Allow        Deny
                 |           |
                 ↓           ↓
            Controller      403
```

**Next chapter: Chapter 4 — Sessions & Stateful Security.**

We'll answer an important question that naturally comes after this:

> **After I successfully authenticate, how does Spring Security remember that I'm logged in for the next request?**

That leads to **HTTP session, `JSESSIONID`, `SecurityContext`, session fixation, stateful vs stateless authentication**, and why this matters when we later move to JWT.

