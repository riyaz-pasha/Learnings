# Chapter 1 — Spring Security Fundamentals

Spring Security becomes much easier once you understand **what problem it is solving and the few core objects involved**.

---

# 1. What is Spring Security?

**Spring Security is a framework for securing Spring applications by handling authentication, authorization, and other common security concerns.**

In simple terms:

```text
Client
  |
  | HTTP Request
  ↓
Spring Security
  |
  ├── Who are you?       → Authentication
  |
  ├── Are you allowed?  → Authorization
  |
  └── Should request continue?
          |
          ↓
     Controller
```

For example:

```http
GET /api/accounts/123
Authorization: Bearer eyJ...
```

Spring Security can determine:

1. Is this user authenticated?
2. Who is this user?
3. What permissions does this user have?
4. Can this user access `/api/accounts/123`?
5. If yes → allow the request.
6. If no → reject it.

---

# 2. Authentication vs Authorization

This is one of the **most important Spring Security interview questions**.

### Authentication

> **Authentication answers: "Who are you?"**

Example:

```text
Username: riyaz
Password: ********
```

Spring Security verifies the credentials.

If valid:

```text
User = riyaz
```

---

### Authorization

> **Authorization answers: "What are you allowed to do?"**

Suppose:

```text
User: riyaz

Authorities:
    USER
```

Request:

```http
DELETE /api/users/10
```

If deleting users requires:

```text
ADMIN
```

the request is rejected.

So:

```text
Authentication
       ↓
Who are you?
       ↓
Authorization
       ↓
What can you do?
```

### Interview answer

> **Authentication verifies the identity of the user, while authorization determines whether the authenticated user has permission to perform a particular operation.**

---

# 3. Where does Spring Security run?

This is where many beginners initially get confused.

Spring Security doesn't normally start by putting security checks inside every controller.

Instead, it sits **before your controller in the request processing pipeline**.

```text
Client
  |
  | HTTP Request
  ↓
┌─────────────────────────┐
│   Spring Security       │
│                         │
│ Security Filter Chain   │
└────────────┬────────────┘
             |
             | allowed
             ↓
       DispatcherServlet
             |
             ↓
        Controller
             |
             ↓
         Service
             |
             ↓
        Repository
```

Therefore, a request can be rejected **before it reaches your controller**.

---

# 4. SecurityFilterChain ⭐⭐⭐

The most important Spring Security concept to understand early is:

> **SecurityFilterChain is a chain of servlet filters through which incoming HTTP requests pass before reaching the application.**

Example configuration:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
```

Conceptually:

```text
HTTP Request
     |
     ↓
Security Filter 1
     |
     ↓
Security Filter 2
     |
     ↓
Security Filter 3
     |
     ↓
Security Filter N
     |
     ↓
Controller
```

Each filter can inspect or modify the request.

Some filters perform authentication.

Some handle authorization.

Some handle CSRF.

Some handle exceptions.

Some manage the security context.

---

# 5. Why are there multiple filters?

Security is not one single operation.

For example, a request might need:

```text
Request
  |
  ├── Load existing authentication
  |
  ├── Extract credentials/token
  |
  ├── Authenticate user
  |
  ├── Store authentication
  |
  ├── Check authorization
  |
  ├── Handle security exceptions
  |
  └── Continue to application
```

Therefore Spring Security uses multiple filters, each responsible for a particular part of the security process.

You **don't need to memorize every filter** for interviews.

You should understand the concept:

> **Spring Security implements much of its web security through a chain of servlet filters.**

---

# 6. SecurityFilterChain is configured using HttpSecurity

You'll frequently see:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
        );

    return http.build();
}
```

Think of `HttpSecurity` as the object through which we configure Spring Security.

For example:

```java
http
    .csrf(...)
    .cors(...)
    .sessionManagement(...)
    .authorizeHttpRequests(...)
    .formLogin(...)
    .httpBasic(...)
    .oauth2ResourceServer(...);
```

Eventually:

```java
http.build()
```

produces the configured:

```text
SecurityFilterChain
```

---

# 7. What is Authentication?

Spring Security represents the result of authentication using an:

```java
Authentication
```

object.

Conceptually:

```text
Authentication
│
├── Principal
├── Credentials
└── Authorities
```

For example:

```text
Authentication

Principal:
    riyaz

Credentials:
    ********

Authorities:
    ROLE_USER
    READ_ACCOUNT
```

After successful authentication, Spring Security knows:

```text
"Who is making this request?"
```

---

# 8. What is Principal?

A **principal represents the currently authenticated identity**.

For example:

```java
Authentication authentication
```

might contain:

```text
Principal = UserDetails(username="riyaz")
```

You may see code such as:

```java
Authentication authentication =
        SecurityContextHolder
            .getContext()
            .getAuthentication();

String username =
        authentication.getName();
```

Conceptually:

```text
SecurityContext
       |
       ↓
Authentication
       |
       ↓
Principal
       |
       ↓
Current user
```

---

# 9. What is GrantedAuthority?

An authenticated user usually has permissions.

Spring Security represents these using:

```java
GrantedAuthority
```

For example:

```text
User: riyaz

Authorities:
    ROLE_USER
    READ_ACCOUNT
    WRITE_ACCOUNT
```

Then authorization can ask:

```text
Does this user have
WRITE_ACCOUNT?
```

If yes:

```text
ALLOW
```

Otherwise:

```text
DENY
```

We'll cover roles and authorities in detail in Chapter 3.

---

# 10. What is SecurityContext?

Spring Security needs somewhere to store the security information for the **current request**.

That's the job of:

```java
SecurityContext
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

You can access it using:

```java
SecurityContextHolder
```

Example:

```java
Authentication authentication =
    SecurityContextHolder
        .getContext()
        .getAuthentication();
```

Then:

```java
authentication.getName();
```

could return:

```text
riyaz
```

---

# 11. SecurityContextHolder

`SecurityContextHolder` provides access to the current `SecurityContext`.

Think:

```text
SecurityContextHolder
        |
        ↓
SecurityContext
        |
        ↓
Authentication
        |
        ↓
Current authenticated user
```

This is an important relationship to remember.

### Interview question

**How do you get the currently authenticated user in Spring Security?**

One common approach:

```java
Authentication authentication =
    SecurityContextHolder
        .getContext()
        .getAuthentication();
```

Then:

```java
String username = authentication.getName();
```

---

# 12. Complete Mental Model

You should now have this picture in your head:

```text
                    HTTP Request
                         |
                         ↓
              ┌─────────────────────┐
              │ SecurityFilterChain │
              │                     │
              │  Security Filters   │
              └──────────┬──────────┘
                         |
                         ↓
                  Authentication
                         |
                         ↓
                  SecurityContext
                         |
                         ↓
                   Authentication
                         |
              ┌──────────┴──────────┐
              ↓                     ↓
          Principal           Authorities
              |                     |
          "Who?"                "What?"
              |                     |
              └──────────┬──────────┘
                         ↓
                    Authorization
                         |
                  ┌──────┴──────┐
                  ↓             ↓
                Allow          Deny
                  |
                  ↓
              Controller
```

This is the foundation for everything else.

---

# 13. A Simple Example

Suppose we have:

```http
GET /api/profile
```

and this endpoint requires authentication.

The request enters:

```text
Client
   |
   ↓
GET /api/profile
   |
   ↓
SecurityFilterChain
   |
   ↓
Is user authenticated?
   |
   ├── No → 401 Unauthorized
   |
   └── Yes
         |
         ↓
     Authorization
         |
         ├── Not allowed → 403 Forbidden
         |
         └── Allowed
                |
                ↓
           Controller
```

This gives us another important interview distinction.

### `401 Unauthorized`

Usually means:

> Authentication is missing or invalid.

Example:

```text
No valid JWT
```

### `403 Forbidden`

Usually means:

> The user is authenticated, but doesn't have sufficient permission.

Example:

```text
Authenticated user
       +
ROLE_USER
       ↓
tries ADMIN operation
       ↓
403 Forbidden
```

Remember:

```text
401 → Authentication problem

403 → Authorization problem
```

---

# 14. Public vs Protected Endpoints

Spring Security lets us define which endpoints require authentication.

Example:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/login").permitAll()
        .requestMatchers("/public/**").permitAll()
        .anyRequest().authenticated()
    );
```

Meaning:

```text
/login
    ↓
PUBLIC

/public/**
    ↓
PUBLIC

Everything else
    ↓
AUTHENTICATION REQUIRED
```

This is **authorization configuration**, even though it talks about whether authentication is required.

---

# 15. What happens when a request arrives?

At a high level:

```text
1. HTTP request arrives
          ↓
2. Security filters process it
          ↓
3. Authentication information is obtained
          ↓
4. User is authenticated
          ↓
5. Authentication is placed in SecurityContext
          ↓
6. Authorization rules are evaluated
          ↓
7. Request allowed/rejected
          ↓
8. Controller executes if allowed
```

The exact sequence differs depending on whether you're using:

* session authentication
* HTTP Basic
* form login
* JWT
* OAuth2

We'll explore those separately.

---

# 16. Important Classes to Remember

Don't memorize hundreds of Spring Security classes.

For interviews, start with these:

| Class / Interface        | Purpose                                        |
| ------------------------ | ---------------------------------------------- |
| `SecurityFilterChain`    | Defines web security filter chain              |
| `HttpSecurity`           | Configures HTTP security                       |
| `Authentication`         | Represents authenticated identity              |
| `Principal`              | Represents the current identity                |
| `GrantedAuthority`       | Represents permissions/authorities             |
| `SecurityContext`        | Holds authentication information               |
| `SecurityContextHolder`  | Provides access to current security context    |
| `AuthenticationManager`  | Coordinates authentication                     |
| `AuthenticationProvider` | Performs a particular authentication mechanism |
| `UserDetailsService`     | Loads user information                         |
| `PasswordEncoder`        | Safely hashes/verifies passwords               |

The last four become especially important in the next chapter.

---

# 17. Interview Questions

### Q1. What is Spring Security?

**Answer:**

> Spring Security is a framework for securing Spring applications. It primarily provides authentication and authorization mechanisms and integrates security into the HTTP request processing pipeline using security filters.

---

### Q2. What is `SecurityFilterChain`?

> `SecurityFilterChain` is a chain of servlet filters that processes incoming HTTP requests before they reach the application. These filters handle things such as authentication, authorization, CSRF protection, exception handling, and other security concerns.

---

### Q3. Authentication vs Authorization?

> Authentication determines **who the user is**, while authorization determines **what the authenticated user is allowed to do**.

---

### Q4. What is `SecurityContext`?

> `SecurityContext` holds the authentication information associated with the current execution/request.

---

### Q5. What is `SecurityContextHolder`?

> `SecurityContextHolder` provides access to the current `SecurityContext`, and therefore allows application code or Spring Security components to access the current authentication.

---

### Q6. What is `Authentication`?

> `Authentication` represents the identity and authentication state of the current user and contains information such as the principal and granted authorities.

---

### Q7. What is `GrantedAuthority`?

> `GrantedAuthority` represents a permission or authority granted to an authenticated user and is used during authorization decisions.

---

### Q8. Difference between 401 and 403?

> **401** generally indicates that authentication is missing or invalid, while **403** indicates that the request was understood and the user is authenticated but does not have sufficient permission.

---

# 18. The 7 Things to Remember

If you remember only these from this chapter:

```text
1. Spring Security secures Spring applications.

2. Authentication = Who are you?

3. Authorization = What are you allowed to do?

4. SecurityFilterChain processes requests before controllers.

5. Authentication represents the current user's identity
   and authorities.

6. SecurityContext holds the current Authentication.

7. SecurityContextHolder provides access to the
   current SecurityContext.
```

And the core relationship:

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

### Next: Chapter 2 — Authentication Internals ⭐

We'll go deeper into the most interview-important flow:

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
        ↓
SecurityContext
```

This is where you'll understand **what actually happens when a user logs in**.

