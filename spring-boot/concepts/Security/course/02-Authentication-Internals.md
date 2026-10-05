# Chapter 2 — Authentication Internals ⭐⭐⭐

This is one of the **most important Spring Security interview topics**.

In Chapter 1, we learned:

```text
Authentication = Who are you?
Authorization  = What are you allowed to do?
```

Now let's answer:

> **How does Spring Security actually authenticate a username/password?**

---

# 1. The Problem

Suppose a user sends:

```http
POST /login
Content-Type: application/json

{
    "username": "riyaz",
    "password": "secret123"
}
```

We need to:

1. Find the user.
2. Get the stored password hash.
3. Compare the supplied password with the stored hash.
4. Determine whether authentication succeeds.
5. Create an `Authentication` object.
6. Store it in the `SecurityContext`.
7. Use that authentication for subsequent authorization.

Spring Security separates these responsibilities into different components.

---

# 2. The Big Picture ⭐⭐⭐

The simplified authentication flow is:

```text
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
UserDetails
        |
        ↓
PasswordEncoder
        |
        ↓
Password matches?
      /     \
    No       Yes
    |         |
    ↓         ↓
Failure    Success
              |
              ↓
       Authentication
              |
              ↓
       SecurityContext
```

There are **four components you should know extremely well**:

```text
AuthenticationManager
AuthenticationProvider
UserDetailsService
PasswordEncoder
```

---

# 3. AuthenticationManager

`AuthenticationManager` is the main entry point for authentication.

Its key method is:

```java
Authentication authenticate(Authentication authentication)
```

Conceptually:

```text
Application
    |
    ↓
AuthenticationManager
    |
    ↓
"Please authenticate this user"
```

For example:

```java
Authentication authentication =
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            username,
            password
        )
    );
```

The important point:

> `AuthenticationManager` generally **coordinates authentication**; it doesn't necessarily contain the actual username/password verification logic itself.

That responsibility is usually delegated to an `AuthenticationProvider`.

---

# 4. AuthenticationProvider ⭐⭐⭐

An `AuthenticationProvider` performs a particular type of authentication.

For example:

```text
AuthenticationProvider
       |
       ├── Username/password authentication
       |
       ├── JWT authentication
       |
       ├── LDAP authentication
       |
       └── Other mechanisms
```

For username/password authentication, Spring Security commonly uses:

```text
DaoAuthenticationProvider
```

The important relationship is:

```text
AuthenticationManager
        |
        ↓
AuthenticationProvider
        |
        ↓
actual authentication logic
```

---

# 5. Why AuthenticationManager + AuthenticationProvider?

This is an important architectural design.

Imagine your application supports:

```text
Username/password
       +
LDAP
       +
OAuth2
```

Instead of putting everything into one giant authentication class:

```text
AuthenticationManager
       |
       ├── Provider 1
       ├── Provider 2
       └── Provider 3
```

Each provider knows how to authenticate a particular type of credential.

So:

> **AuthenticationManager delegates authentication to appropriate AuthenticationProviders.**

---

# 6. UserDetailsService ⭐⭐⭐

Now we need to find the user.

That's where:

```java
UserDetailsService
```

comes in.

Its main method is:

```java
UserDetails loadUserByUsername(String username)
```

Conceptually:

```text
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
```

For example:

```java
@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) {

        User user = userRepository.findByUsername(username);

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .build();
    }
}
```

The important point is:

> `UserDetailsService` is responsible for **loading user information**, not for checking the password.

That's a very common interview question.

---

# 7. What does UserDetails contain?

`UserDetails` represents the information Spring Security needs about a user.

Conceptually:

```text
UserDetails
│
├── username
├── password
├── authorities
├── accountNonExpired
├── accountNonLocked
├── credentialsNonExpired
└── enabled
```

For example:

```text
username:
    riyaz

password:
    $2a$10$...

authorities:
    ROLE_USER

enabled:
    true
```

---

# 8. Where is the password stored?

This is critical.

You should **never** store:

```text
secret123
```

directly in your database.

Instead, store a password hash:

```text
$2a$10$N9qo8uLOickgx2ZMRZoMye...
```

For example:

```text
Database

username     password
--------------------------------
riyaz        $2a$10$....
```

The user enters:

```text
secret123
```

Spring Security uses:

```java
PasswordEncoder
```

to verify it.

---

# 9. PasswordEncoder ⭐⭐⭐

`PasswordEncoder` provides password hashing and verification.

For example:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

When creating a user:

```java
String encoded =
    passwordEncoder.encode("secret123");
```

Store:

```text
$2a$10$...
```

in the database.

Later, during login:

```java
passwordEncoder.matches(
    "secret123",
    storedHash
);
```

returns:

```text
true
```

if the password is correct.

---

# 10. Very Important: Hashing ≠ Encryption

This is a common interview question.

### Encryption

```text
Plaintext
   ↓
Encryption
   ↓
Ciphertext
```

Can be reversed with the appropriate key:

```text
Ciphertext
   ↓
Decryption
   ↓
Plaintext
```

### Password hashing

```text
Password
   ↓
Hash
   ↓
Stored hash
```

You don't decrypt the hash.

Instead:

```text
Entered password
      ↓
PasswordEncoder.matches()
      ↓
Compare against stored hash
```

Therefore:

> **Passwords should be hashed, not encrypted for storage.**

Modern Spring Security commonly uses adaptive password encoders such as BCrypt, PBKDF2, or Argon2.

---

# 11. Why can't we simply hash the password again?

This is where **salt** becomes important.

Suppose:

```text
password = secret123
```

If hashing always produced exactly the same output:

```text
secret123 → ABC123
```

then two users with the same password would have the same hash.

Attackers could use precomputed lookup tables.

Password hashing algorithms therefore use a **salt**.

Conceptually:

```text
Password + Random Salt
        ↓
     Hashing
        ↓
   Stored result
```

For BCrypt, the encoded password contains the information needed to perform verification.

That's why:

```java
passwordEncoder.matches(rawPassword, storedHash)
```

can verify the password without decrypting it.

---

# 12. Complete Username/Password Authentication Flow ⭐⭐⭐

Let's put everything together.

User sends:

```text
username = riyaz
password = secret123
```

### Step 1 — Create authentication request

Spring Security represents the credentials as an `Authentication`.

Commonly:

```java
UsernamePasswordAuthenticationToken
```

Conceptually:

```text
UsernamePasswordAuthenticationToken
        |
        ├── username
        └── password
```

---

### Step 2 — AuthenticationManager receives it

```text
AuthenticationManager
```

receives:

```text
username = riyaz
password = secret123
```

---

### Step 3 — Provider is selected

The manager delegates to an appropriate:

```text
AuthenticationProvider
```

Typically:

```text
DaoAuthenticationProvider
```

for database-backed username/password authentication.

---

### Step 4 — Load the user

Provider calls:

```text
UserDetailsService
```

which might execute:

```sql
SELECT *
FROM users
WHERE username = 'riyaz';
```

It returns:

```text
UserDetails
```

---

### Step 5 — Verify password

Provider obtains:

```text
stored password hash
```

Then:

```java
passwordEncoder.matches(
    rawPassword,
    storedHash
);
```

If:

```text
true
```

authentication succeeds.

If:

```text
false
```

authentication fails.

---

### Step 6 — Create authenticated Authentication

After successful authentication, Spring Security creates/returns an authenticated `Authentication`.

Conceptually:

```text
Authentication

Principal:
    riyaz

Authorities:
    ROLE_USER

Authenticated:
    true
```

---

### Step 7 — Store authentication

The authentication is associated with:

```text
SecurityContext
```

which is accessible through:

```text
SecurityContextHolder
```

Now Spring Security knows:

```text
Current user = riyaz
```

---

# 13. Complete Diagram ⭐⭐⭐

Memorize this:

```text
              Username + Password
                       |
                       ↓
        UsernamePasswordAuthenticationToken
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
                  UserDetails
                       |
                       ↓
                PasswordEncoder
                       |
                ┌──────┴──────┐
                ↓             ↓
             invalid        valid
                |             |
                ↓             ↓
           Authentication   Authenticated
              Failure       Authentication
                                |
                                ↓
                        SecurityContext
                                |
                                ↓
                       Current User
```

---

# 14. Authentication vs UserDetails

Another subtle interview question.

They are **not the same thing**.

### `UserDetails`

Represents user information loaded from your user store.

```text
UserDetails
    |
    ├── username
    ├── password
    └── authorities
```

### `Authentication`

Represents the authentication state of the current request/user.

```text
Authentication
    |
    ├── principal
    ├── credentials
    ├── authorities
    └── authenticated
```

Think:

```text
UserDetails
    ↓
"Here is the information about this user."

Authentication
    ↓
"This user has successfully authenticated."
```

---

# 15. AuthenticationManager vs AuthenticationProvider

Very common interview question.

### AuthenticationManager

> Coordinates authentication.

### AuthenticationProvider

> Performs authentication for a particular authentication mechanism.

Diagram:

```text
AuthenticationManager
       |
       ├── AuthenticationProvider A
       |
       ├── AuthenticationProvider B
       |
       └── AuthenticationProvider C
```

---

# 16. UserDetailsService vs AuthenticationProvider

Another common question.

### UserDetailsService

Loads the user:

```text
username
   ↓
database
   ↓
UserDetails
```

### AuthenticationProvider

Uses that information to authenticate the user.

For username/password:

```text
AuthenticationProvider
       |
       ├── UserDetailsService
       |
       └── PasswordEncoder
```

So:

```text
UserDetailsService
    = "Find the user"

PasswordEncoder
    = "Verify the password"

AuthenticationProvider
    = "Perform the authentication"

AuthenticationManager
    = "Coordinate authentication"
```

This distinction is **very interview-friendly**.

---

# 17. What happens when authentication fails?

Several things can cause authentication failure.

For example:

```text
User doesn't exist
       ↓
UsernameNotFoundException
```

or:

```text
Password doesn't match
       ↓
BadCredentialsException
```

or:

```text
Account disabled
       ↓
DisabledException
```

or:

```text
Account locked
       ↓
LockedException
```

At a high level:

```text
Authentication fails
       ↓
AuthenticationException
       ↓
Spring Security handles failure
       ↓
401 / login failure response
```

The exact HTTP response depends on how your application is configured.

---

# 18. Does UserDetailsService Verify Passwords?

**No.**

This is an important interview trap.

Incorrect:

> "`UserDetailsService` checks whether the password is correct."

Correct:

> "`UserDetailsService` loads user information, while an `AuthenticationProvider`, typically using a `PasswordEncoder`, performs password verification."

---

# 19. Does PasswordEncoder Encrypt Passwords?

**No.**

It hashes passwords and provides matching/verification functionality.

```java
passwordEncoder.encode(rawPassword);
```

creates a stored password representation.

```java
passwordEncoder.matches(rawPassword, storedHash);
```

verifies it.

---

# 20. What if the Database Stores Plaintext Passwords?

Bad design:

```text
users

username | password
-------------------------
riyaz    | secret123
```

If the database is compromised:

```text
Attacker
   ↓
Database
   ↓
secret123
```

The password is immediately exposed.

Better:

```text
username | password
------------------------------
riyaz    | $2a$10$...
```

Even if the database is leaked, the attacker doesn't directly obtain the original password.

This is why:

> **Password hashing is a defense-in-depth measure, not a replacement for database security.**

---

# 21. Authentication Object Before vs After Authentication

This is a useful mental model.

### Before authentication

```text
Authentication

principal:
    riyaz

credentials:
    secret123

authenticated:
    false
```

### After successful authentication

```text
Authentication

principal:
    riyaz

authorities:
    ROLE_USER

authenticated:
    true
```

The exact internal object and credential handling can vary by authentication mechanism, but conceptually this is what you should understand.

---

# 22. Interview Questions ⭐

### Q1. What is `AuthenticationManager`?

> It is the main authentication interface used by Spring Security to authenticate an `Authentication` request. It delegates the actual authentication to one or more `AuthenticationProvider`s.

---

### Q2. What is `AuthenticationProvider`?

> It performs authentication for a specific authentication mechanism. For example, `DaoAuthenticationProvider` handles username/password authentication using a `UserDetailsService` and `PasswordEncoder`.

---

### Q3. What is `UserDetailsService`?

> It is a service used by Spring Security to load user-specific data, usually from a database or another user store, based on a username.

---

### Q4. Does `UserDetailsService` authenticate the user?

> No. It loads the user's details. The `AuthenticationProvider` uses those details along with a `PasswordEncoder` to authenticate the credentials.

---

### Q5. What is `PasswordEncoder`?

> `PasswordEncoder` provides password hashing and verification functionality. It is used to avoid storing plaintext passwords and to verify a supplied password against the stored password hash.

---

### Q6. What is `DaoAuthenticationProvider`?

> It is an `AuthenticationProvider` used for username/password authentication where user information is loaded through a `UserDetailsService`.

---

### Q7. What happens during username/password authentication?

A strong interview answer:

> The username and password are represented as an `Authentication` request and passed to the `AuthenticationManager`. The manager delegates to an appropriate `AuthenticationProvider`. The provider loads the user using `UserDetailsService`, verifies the supplied password using `PasswordEncoder`, and, if successful, returns an authenticated `Authentication`. The authentication is then associated with the `SecurityContext`.

---

# 23. One Important Correction to a Common Mental Model

Don't think:

```text
Login
 ↓
Controller
 ↓
UserDetailsService
 ↓
PasswordEncoder
```

Instead, think:

```text
HTTP Request
      ↓
Security Filters
      ↓
Authentication mechanism
      ↓
AuthenticationManager
      ↓
AuthenticationProvider
      ↓
UserDetailsService
      ↓
PasswordEncoder
      ↓
Authenticated Authentication
      ↓
SecurityContext
```

The exact path varies depending on whether you're using form login, HTTP Basic, JWT, OAuth2, etc.

---

# 24. Interview Cheat Sheet

| Component                   | Main responsibility                 |
| --------------------------- | ----------------------------------- |
| `AuthenticationManager`     | Coordinates authentication          |
| `AuthenticationProvider`    | Performs authentication             |
| `DaoAuthenticationProvider` | Username/password authentication    |
| `UserDetailsService`        | Loads user information              |
| `UserDetails`               | Represents loaded user data         |
| `PasswordEncoder`           | Hashes/verifies passwords           |
| `Authentication`            | Represents authentication state     |
| `SecurityContext`           | Holds current authentication        |
| `SecurityContextHolder`     | Provides access to security context |

### The easiest way to remember:

```text
AuthenticationManager
        ↓
"Who can authenticate this?"

AuthenticationProvider
        ↓
"Let me authenticate this."

UserDetailsService
        ↓
"Let me find this user."

PasswordEncoder
        ↓
"Let me verify this password."

Authentication
        ↓
"Authentication succeeded."

SecurityContext
        ↓
"Remember who this user is."
```

---

## ⭐ Most important interview flow

If an interviewer asks:

> **"Explain username/password authentication in Spring Security."**

You can draw:

```text
Username + Password
        ↓
Authentication
        ↓
AuthenticationManager
        ↓
AuthenticationProvider
        ↓
UserDetailsService
        ↓
UserDetails
        ↓
PasswordEncoder
        ↓
Password matches?
      /       \
    No         Yes
    ↓           ↓
 Failure    Authenticated
                ↓
        SecurityContext
```

If you can explain this diagram clearly, you understand the core of Spring Security authentication.

---

### Next — Chapter 3: Authorization ⭐⭐⭐

We'll cover:

```text
Authentication
       ↓
Authorities / Roles
       ↓
Authorization
       ↓
requestMatchers()
       ↓
hasRole()
       ↓
hasAuthority()
       ↓
@PreAuthorize
```

and, importantly, **why `ROLE_ADMIN`, `ADMIN`, `hasRole("ADMIN")`, and `hasAuthority("ROLE_ADMIN")` are not all the same thing.**


---

---
---



Absolutely. Before moving to authorization, let's build a **small working Spring Boot example** for the username/password authentication flow we just discussed.

We'll keep it minimal and focus only on the pieces from Chapter 2.

# 1. What we're building

```text
POST /login
      |
      ↓
Spring Security
      |
      ↓
AuthenticationManager
      |
      ↓
DaoAuthenticationProvider
      |
      ├── UserDetailsService → loads user
      |
      └── PasswordEncoder    → verifies password
      |
      ↓
Authenticated
      |
      ↓
SecurityContext
      |
      ↓
GET /hello
```

We'll use:

* Spring Boot
* Spring Security
* In-memory user initially
* `UserDetailsService`
* `PasswordEncoder`
* `SecurityFilterChain`
* HTTP Basic authentication

I'm intentionally using **HTTP Basic first**, because it makes the authentication mechanism easy to see. Later, JWT will build on the same core concepts.

---

# 2. Dependencies

For Maven:

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

</dependencies>
```

Spring Boot automatically configures many Spring Security components for us.

---

# 3. Project structure

Keep it simple:

```text
src/main/java/com/example/securitydemo/
│
├── SecurityDemoApplication.java
│
├── config/
│   └── SecurityConfig.java
│
└── controller/
    └── HelloController.java
```

---

# 4. Main Application

```java
package com.example.securitydemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecurityDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityDemoApplication.class, args);
    }
}
```

Nothing special here.

---

# 5. SecurityConfig

This is the important part.

```java
package com.example.securitydemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails user = User.builder()
                .username("riyaz")
                .password(passwordEncoder.encode("secret123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic();

        return http.build();
    }
}
```

There are **three important beans** here:

```text
PasswordEncoder
UserDetailsService
SecurityFilterChain
```

Let's understand each one.

---

# 6. PasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

This gives Spring Security a password encoder.

When we create our user:

```java
.password(passwordEncoder.encode("secret123"))
```

we don't store:

```text
secret123
```

Instead, something similar to:

```text
$2a$10$...
```

is stored.

So conceptually:

```text
"secret123"
      |
      ↓
BCryptPasswordEncoder
      |
      ↓
"$2a$10$..."
```

---

# 7. UserDetailsService

For now we're using:

```java
InMemoryUserDetailsManager
```

instead of a database.

```java
@Bean
public UserDetailsService userDetailsService(
        PasswordEncoder passwordEncoder) {

    UserDetails user = User.builder()
            .username("riyaz")
            .password(passwordEncoder.encode("secret123"))
            .roles("USER")
            .build();

    return new InMemoryUserDetailsManager(user);
}
```

This effectively says:

```text
username: riyaz
password: <BCrypt hash>
role: USER
```

Think of it as our temporary user database:

```text
InMemoryUserDetailsManager
          |
          ↓
     ┌─────────────┐
     │ riyaz       │
     │ password    │
     │ ROLE_USER   │
     └─────────────┘
```

Later, we'll replace this with:

```text
UserDetailsService
       ↓
UserRepository
       ↓
PostgreSQL
```

The rest of the authentication architecture remains largely the same.

---

# 8. SecurityFilterChain

Now:

```java
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    http
        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        )
        .httpBasic();

    return http.build();
}
```

This says:

```text
Every request
     ↓
Authentication required
```

And:

```java
.httpBasic()
```

enables HTTP Basic authentication.

---

# 9. Controller

Create:

```java
package com.example.securitydemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from secured endpoint!";
    }
}
```

---

# 10. Start the application

Run:

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

# 11. Try the endpoint WITHOUT credentials

```bash
curl http://localhost:8080/hello
```

You should get an authentication failure, typically:

```text
401 Unauthorized
```

Because we configured:

```java
.anyRequest().authenticated()
```

---

# 12. Try with the correct credentials

```bash
curl -u riyaz:secret123 http://localhost:8080/hello
```

Now:

```text
Hello from secured endpoint!
```

The `-u` option means:

```text
username = riyaz
password = secret123
```

---

# 13. What actually happened?

This is the important part.

When you execute:

```bash
curl -u riyaz:secret123 http://localhost:8080/hello
```

the request contains an HTTP Basic `Authorization` header.

Conceptually:

```text
HTTP Request
     |
     ↓
Authorization: Basic <credentials>
     |
     ↓
Spring Security Filter Chain
     |
     ↓
Basic Authentication Filter
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
Find "riyaz"
     |
     ↓
UserDetails
     |
     ↓
PasswordEncoder
     |
     ↓
Does "secret123" match BCrypt hash?
     |
     ├── No → Authentication failure
     |
     └── Yes
          |
          ↓
     Authenticated Authentication
          |
          ↓
     SecurityContext
          |
          ↓
     Authorization
          |
          ↓
     Controller
          |
          ↓
     /hello
```

That's the Chapter 2 diagram happening in a real application.

---

# 14. But where is AuthenticationManager?

You may notice something interesting.

We didn't write:

```java
AuthenticationManager
```

anywhere.

That's because Spring Boot/Spring Security can automatically configure the authentication infrastructure from the beans we've provided.

Conceptually, Spring creates something like:

```text
AuthenticationManager
        |
        ↓
DaoAuthenticationProvider
        |
        ├──────────────┐
        ↓              ↓
UserDetailsService  PasswordEncoder
```

We can explicitly configure these ourselves when needed, but **don't add unnecessary configuration just to make the example longer**.

---

# 15. Let's explicitly expose AuthenticationManager

For learning purposes, we can also configure it explicitly.

```java
@Bean
public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {

    DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(userDetailsService);

    provider.setPasswordEncoder(passwordEncoder);

    return new ProviderManager(provider);
}
```

Then the configuration becomes:

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails user = User.builder()
                .username("riyaz")
                .password(passwordEncoder.encode("secret123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic();

        return http.build();
    }
}
```

Now the architecture is much more visible:

```text
                 AuthenticationManager
                         |
                         ↓
               DaoAuthenticationProvider
                    /            \
                   /              \
                  ↓                ↓
       UserDetailsService    PasswordEncoder
                  |
                  ↓
              UserDetails
```

---

# 16. What is `DaoAuthenticationProvider`?

This is the class connecting the pieces together.

```java
DaoAuthenticationProvider provider =
        new DaoAuthenticationProvider(userDetailsService);

provider.setPasswordEncoder(passwordEncoder);
```

It essentially says:

> "For username/password authentication, load the user using this `UserDetailsService`, and verify the password using this `PasswordEncoder`."

So:

```text
DaoAuthenticationProvider
       |
       ├── UserDetailsService
       |
       └── PasswordEncoder
```

This is an extremely useful interview diagram.

---

# 17. Database Version

In a real application, you probably won't use:

```java
new InMemoryUserDetailsManager(...)
```

Instead:

```text
                     Spring Security
                           |
                           ↓
                  UserDetailsService
                           |
                           ↓
                    UserRepository
                           |
                           ↓
                       PostgreSQL
```

For example:

```java
@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "User not found"
                    )
                );

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}
```

Now the flow becomes:

```text
Username
   |
   ↓
UserDetailsService
   |
   ↓
UserRepository
   |
   ↓
PostgreSQL
   |
   ↓
User
   |
   ↓
UserDetails
```

The authentication provider doesn't care whether the user came from:

```text
PostgreSQL
MySQL
MongoDB
LDAP
REST API
In-memory storage
```

That's the beauty of the abstraction.

---

# 18. One Important Point: HTTP Basic ≠ Form Login

We're using:

```java
.httpBasic()
```

This means the client sends credentials through the HTTP Basic authentication mechanism.

It does **not** mean:

```text
Spring Security always works this way.
```

Other authentication mechanisms include:

```text
HTTP Basic
    ↓
username/password in Authorization header

Form Login
    ↓
HTML login form

JWT
    ↓
Bearer token

OAuth2
    ↓
OAuth2 access token
```

The underlying Spring Security concepts we learned still apply:

```text
Authentication
AuthenticationManager
AuthenticationProvider
SecurityContext
Authorities
```

but the mechanism that obtains the credentials can differ.

---

# 19. A Useful Debugging Trick

When learning Spring Security, enable:

```properties
logging.level.org.springframework.security=DEBUG
```

in:

```text
src/main/resources/application.properties
```

Then make:

```bash
curl -u riyaz:secret123 http://localhost:8080/hello
```

Spring Security will log a lot more information about the request and security processing.

This is extremely useful for understanding:

```text
Which filter ran?
Was authentication attempted?
Was authentication successful?
Which endpoint was matched?
Why was a request rejected?
```

Don't memorize the debug output. Use it to understand the flow.

---

# 20. Final Code — Minimal Version

For actual learning, I'd start with this version:

```java
@Configuration
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

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic();

        return http.build();
    }
}
```

Then:

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from secured endpoint!";
    }
}
```

Test:

```bash
curl http://localhost:8080/hello
```

→ `401`

```bash
curl -u riyaz:secret123 http://localhost:8080/hello
```

→ `Hello from secured endpoint!`

---

## The key thing to understand

Don't focus on memorizing the configuration yet.

Understand this:

```text
                 HTTP Request
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
                ┌──────┴──────┐
                ↓             ↓
       UserDetailsService  PasswordEncoder
                ↓             ↓
             User data    Password verification
                └──────┬──────┘
                       ↓
               Authentication
                       ↓
                SecurityContext
                       ↓
                 Authorization
                       ↓
                  Controller
```

**This architecture is much more important for an interview than remembering individual configuration lines.**

When you're comfortable with this, we can move to **Chapter 3 — Authorization**, where we'll use this same application and add `ROLE_USER`, `ROLE_ADMIN`, `hasRole()`, `hasAuthority()`, `@PreAuthorize`, and endpoint-level authorization.


