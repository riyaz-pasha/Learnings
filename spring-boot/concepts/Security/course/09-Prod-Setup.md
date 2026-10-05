Yes. For a **real production Spring Boot REST API**, Spring Security should be only one part of the security architecture.

A good production setup looks roughly like this:

```text
                         ┌──────────────────────┐
                         │       Auth0          │
                         │ Identity Provider    │
                         └──────────┬───────────┘
                                    │
                              Access Token
                                    │
                                    ▼
┌──────────┐      HTTPS       ┌───────────────┐
│ Frontend │ ────────────────►│ API Gateway / │
│ / Mobile │                  │ Load Balancer │
└──────────┘                  └───────┬───────┘
                                      │
                         Rate limiting│
                         WAF          │
                         TLS          │
                                      ▼
                              ┌───────────────┐
                              │ Spring Boot   │
                              │ API           │
                              ├───────────────┤
                              │ CORS          │
                              │ JWT validation│
                              │ Authorization │
                              │ CSRF*         │
                              │ Audit logging │
                              │ Metrics       │
                              │ Tracing       │
                              └───────┬───────┘
                                      │
                         ┌────────────┼────────────┐
                         ▼            ▼            ▼
                      Postgres      Redis       Other APIs
```

`*` CSRF depends on your authentication architecture. A bearer-token API is different from a cookie/session-based application.

Spring Security itself provides the authentication/authorization layer, while OAuth2 Resource Server support handles standard bearer JWT validation. ([Home][1])

---

# 1. Production stack I would recommend

For a typical Spring Boot REST API:

| Concern            | Recommended approach                             |
| ------------------ | ------------------------------------------------ |
| Authentication     | Auth0 / Okta / Cognito / another IdP             |
| Protocol           | OAuth 2.0 + OIDC                                 |
| API authentication | JWT Bearer Access Token                          |
| Spring integration | OAuth2 Resource Server                           |
| Authorization      | Spring Security + roles/scopes/permissions       |
| API docs           | OpenAPI + Swagger UI                             |
| Logging            | SLF4J + Logback                                  |
| Audit logging      | Dedicated structured audit events                |
| Metrics            | Micrometer + Prometheus                          |
| Tracing            | Micrometer Tracing + OpenTelemetry               |
| Rate limiting      | API Gateway / Redis / Bucket4j                   |
| Secrets            | AWS Secrets Manager / Vault / Kubernetes Secrets |
| Health             | Spring Boot Actuator                             |
| TLS                | Load balancer/API gateway                        |
| Error handling     | Consistent JSON error responses                  |
| Security headers   | Spring Security                                  |
| Database           | PostgreSQL + least-privilege DB user             |
| Deployment         | Container/Kubernetes/ECS/Lambda/etc.             |

The important idea is:

> **Don't put everything inside `SecurityConfig`.**

Security configuration, observability, rate limiting, API documentation, secrets, and infrastructure protection are separate concerns.

---

# 2. Start with Auth0 as the Identity Provider

For your API, I would use the architecture:

```text
                    Auth0
                     │
             authenticates user
                     │
                     ▼
              Access Token
                     │
                     ▼
Frontend ──────► Spring API
                 │
                 │ validate JWT
                 ▼
            Spring Security
                 │
                 ▼
            Authorization
```

Your Spring application becomes an **OAuth2 Resource Server**.

Auth0's own Spring API documentation follows this model: Auth0 issues the access token and the Spring API validates the bearer token. ([Auth0 Developer Center][2])

---

# 3. Dependencies

For a REST API:

```xml
<dependencies>

    <!-- REST API -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- OAuth2 / JWT Resource Server -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- OpenAPI + Swagger UI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>...</version>
    </dependency>

</dependencies>
```

The Spring Security resource-server starter is the standard Spring Boot approach for OAuth2 bearer-token APIs. ([Home][1])

For Swagger/OpenAPI, `springdoc-openapi-starter-webmvc-ui` provides Swagger UI and OpenAPI endpoints. ([springdoc-openapi][3])

---

# 4. Configure Auth0

Suppose Auth0 gives you:

```text
Issuer:
https://my-company.us.auth0.com/

Audience:
https://api.my-company.com
```

Configure:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH0_ISSUER_URI}
```

Environment:

```bash
AUTH0_ISSUER_URI=https://my-company.us.auth0.com/
```

Spring uses the issuer to discover the authorization server metadata and configure JWT validation. ([Home][4])

But there is an important production point:

## Validate the audience too

You don't want to accept:

```text
JWT issued by Auth0
```

merely because the issuer is correct.

You want:

```text
issuer == my Auth0 tenant
AND
audience == my API
AND
signature valid
AND
token not expired
AND
other required claims valid
```

Conceptually:

```text
                  JWT
                   │
        ┌──────────┼───────────┐
        ▼          ▼           ▼
    Signature    Issuer     Expiration
        │          │           │
        └──────────┼───────────┘
                   ▼
               Audience
                   │
                   ▼
             Authentication
```

This is a very common production security consideration.

---

# 5. Production SecurityConfig

A reasonable starting point:

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

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Health
                .requestMatchers("/actuator/health").permitAll()

                // OpenAPI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Public API
                .requestMatchers("/api/public/**").permitAll()

                // Admin
                .requestMatchers("/api/admin/**")
                    .hasAuthority("SCOPE_admin")

                // Everything else
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
```

But **don't blindly copy this configuration**.

For example, whether Swagger should be public in production depends on your organization.

---

# 6. Swagger UI in production

Swagger is extremely useful:

```text
/v3/api-docs
/swagger-ui/index.html
```

But it also exposes information about your API.

For production, you have several choices.

### Option A — Swagger only in development

```text
DEV
 └── Swagger enabled

STAGING
 └── Swagger enabled/restricted

PRODUCTION
 └── Swagger disabled
```

This is my preferred approach for sensitive APIs.

---

### Option B — Protect Swagger

For example:

```java
.requestMatchers(
    "/swagger-ui/**",
    "/v3/api-docs/**"
)
.hasAuthority("SCOPE_api.docs")
```

Then only authorized users can access it.

---

### Option C — Internal network only

Swagger might be available only through:

```text
VPN
   ↓
Internal Load Balancer
   ↓
Swagger
```

This is often better than making it publicly accessible.

---

# 7. Swagger + JWT

You also want Swagger UI to understand:

```http
Authorization: Bearer <JWT>
```

OpenAPI can describe this using a bearer security scheme.

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }
}
```

Then endpoints can declare security requirements.

Swagger UI can then provide:

```text
Authorize 🔒

Bearer Token:
________________________

[Authorize]
```

---

# 8. Logging

Production security needs **good logging**, but logging authentication incorrectly can itself create a security problem.

You want logs like:

```text
2026-10-05 10:32:12 INFO
request completed
requestId=abc123
userId=auth0|12345
method=GET
path=/api/orders
status=200
duration=42ms
```

Not:

```text
Authorization: Bearer eyJhbGciOi...
```

Never log:

* access tokens
* refresh tokens
* passwords
* client secrets
* API keys
* session IDs
* sensitive personal data

---

# 9. Structured logging

Instead of:

```text
User login failed
```

prefer structured information:

```json
{
  "event": "authentication_failed",
  "requestId": "abc123",
  "path": "/api/orders",
  "reason": "invalid_token"
}
```

Typical production stack:

```text
Spring Boot
     │
     ▼
SLF4J
     │
     ▼
Logback
     │
     ▼
JSON logs
     │
     ▼
CloudWatch / ELK / Datadog / Splunk
```

---

# 10. Correlation ID

This is extremely important in microservices.

Suppose:

```text
Request
  │
  ▼
API Gateway
  │ requestId=abc123
  ▼
Order Service
  │
  ├── Payment Service
  │
  └── Inventory Service
```

Every log should allow you to follow:

```text
abc123
```

across services.

Example:

```text
Order Service:
requestId=abc123 order=123

Payment Service:
requestId=abc123 payment=456

Inventory Service:
requestId=abc123 product=789
```

Without correlation IDs, production debugging becomes painful.

---

# 11. Distributed tracing

Logging tells you:

```text
WHAT happened?
```

Tracing tells you:

```text
WHERE did the request spend time?
```

Example:

```text
HTTP request
     │
     ├── 20ms API Gateway
     │
     ├── 50ms Order Service
     │       │
     │       ├── 30ms PostgreSQL
     │       │
     │       └── 100ms Payment Service
     │
     └── Response
```

Typical production stack:

```text
Spring Boot
     │
     ▼
Micrometer Tracing
     │
     ▼
OpenTelemetry
     │
     ▼
Collector
     │
     ├── Jaeger
     ├── Grafana Tempo
     ├── Datadog
     └── other backend
```

Spring Security also integrates with Spring Observability and can create observations around the filter chain, `AuthenticationManager`, and `AuthorizationManager`. ([Home][5])

---

# 12. Metrics

You also need metrics.

For example:

```text
HTTP requests/sec
HTTP 4xx
HTTP 5xx
authentication failures
authorization failures
request latency
database latency
JVM memory
CPU
GC
active connections
```

Actuator:

```text
/actuator/health
/actuator/metrics
/prometheus
```

Typical architecture:

```text
Spring Boot
     │
  Micrometer
     │
     ▼
 Prometheus
     │
     ▼
 Grafana
```

Do not expose every actuator endpoint publicly.

Instead:

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - prometheus
```

And ideally expose management endpoints on a separate management port/network.

---

# 13. Rate limiting

This is another important distinction:

> **Rate limiting is not primarily a Spring Security responsibility.**

You can implement it in Spring, but in production I usually prefer:

```text
Internet
   │
   ▼
WAF
   │
   ▼
API Gateway
   │
   │ Rate limiting
   ▼
Spring Boot
```

For example:

```text
IP:
100 requests/minute

User:
1000 requests/minute

Expensive endpoint:
10 requests/minute
```

---

# 14. Why rate limit before Spring?

Imagine an attacker sends:

```text
100,000 requests/sec
```

If the request reaches your Spring application first:

```text
Internet
   ↓
Spring
   ↓
Rate limiter
```

your application is already doing work.

Better:

```text
Internet
   ↓
WAF
   ↓
Gateway
   ↓
Rate limiter
   X
```

Most malicious traffic gets rejected before reaching your application.

---

# 15. Application-level rate limiting

Sometimes you still need application-level limits.

For example:

```text
POST /api/orders
```

might have:

```text
10 requests / minute / user
```

while:

```text
GET /api/products
```

might have:

```text
1000 requests / minute / user
```

You could use:

* Redis
* Bucket4j
* Spring Cloud Gateway
* API Gateway
* Redis-backed distributed limiter

For multiple application instances:

```text
             ┌── Spring A
             │
Gateway ─────┼── Spring B
             │
             └── Spring C
                    │
                    ▼
                  Redis
```

You generally don't want:

```text
Spring A → local counter
Spring B → local counter
Spring C → local counter
```

because each instance has a different view of the rate.

---

# 16. Authorization: scopes vs roles

With Auth0, I'd strongly recommend thinking in terms of **permissions/scopes**, rather than putting all authorization logic into controllers.

For example:

```text
orders:read
orders:create
orders:update
orders:delete
```

Then:

```java
.requestMatchers(HttpMethod.GET, "/api/orders/**")
    .hasAuthority("SCOPE_orders:read")
```

and:

```java
.requestMatchers(HttpMethod.POST, "/api/orders")
    .hasAuthority("SCOPE_orders:create")
```

You can also use roles:

```text
ADMIN
MANAGER
USER
```

Auth0 can be integrated with Spring Security to map Auth0 roles into Spring authorities. ([Auth0 Developer Center][6])

A useful distinction:

```text
Role
  ↓
ADMIN

Permission / Scope
  ↓
orders:read
orders:create
orders:delete
```

For large systems, fine-grained permissions are often easier to reason about.

---

# 17. Method-level authorization

Don't rely only on URLs.

For example:

```java
@PreAuthorize("hasAuthority('SCOPE_orders:delete')")
public void deleteOrder(Long orderId) {
    ...
}
```

And potentially:

```java
@PreAuthorize(
    "@orderSecurity.canDelete(authentication, #orderId)"
)
public void deleteOrder(Long orderId) {
    ...
}
```

This allows business-level authorization:

```text
Is authenticated?
       ↓
Has orders:delete?
       ↓
Does this user own the order?
       ↓
Can delete
```

That's much stronger than:

```text
/admin/**
```

alone.

---

# 18. Don't trust the frontend for authorization

This is a major production rule.

Frontend:

```javascript
if (user.isAdmin) {
    showDeleteButton();
}
```

is useful for UX.

But this:

```text
POST /api/users/123/delete
```

must still be authorized by the backend.

An attacker can simply call:

```bash
curl ...
```

without your UI.

Therefore:

```text
Frontend authorization
        ↓
UX only

Backend authorization
        ↓
REAL SECURITY
```

---

# 19. CORS

Suppose:

```text
Frontend:
https://app.example.com

API:
https://api.example.com
```

Configure only trusted origins:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration =
        new CorsConfiguration();

    configuration.setAllowedOrigins(
        List.of("https://app.example.com")
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

Don't do this casually in production:

```java
allowedOrigins("*")
```

especially when credentials/cookies are involved.

---

# 20. CSRF

For a pure API using:

```http
Authorization: Bearer <access-token>
```

CSRF has a different threat model from cookie-based authentication.

That's why you will often see:

```java
.csrf(csrf -> csrf.disable())
```

in stateless bearer-token APIs.

But don't memorize:

> "REST API = disable CSRF."

Instead remember:

```text
Are credentials automatically attached by browser?

        │
        ├── YES → CSRF needs careful consideration
        │
        └── NO / explicit Authorization header
                    ↓
             different CSRF model
```

---

# 21. Security headers

Spring Security can provide standard security headers.

For example:

```text
X-Content-Type-Options
Content-Security-Policy
X-Frame-Options
Strict-Transport-Security
```

For a browser-facing application, CSP deserves particular attention.

Example conceptually:

```text
Content-Security-Policy:
    default-src 'self'
```

The exact policy depends heavily on your frontend.

---

# 22. HTTPS

Never design production authentication around plain HTTP.

Production:

```text
Client
  │
 HTTPS
  ▼
Load Balancer
  │
 HTTPS / internal TLS
  ▼
Spring Boot
```

At minimum:

```text
TLS 1.2+
Secure cookies where applicable
HSTS
No sensitive data over HTTP
```

Usually TLS termination happens at:

```text
Cloudflare
AWS ALB
Nginx
API Gateway
etc.
```

rather than configuring certificates inside every Spring instance.

---

# 23. Secrets

Never:

```yaml
auth0:
  client-secret: my-secret
```

committed to Git.

Use:

```text
Environment variables
       +
Secret Manager
```

For example:

```text
AWS Secrets Manager
HashiCorp Vault
Kubernetes Secrets
Azure Key Vault
GCP Secret Manager
```

And:

```yaml
auth0:
  client-secret: ${AUTH0_CLIENT_SECRET}
```

---

# 24. Error responses

Don't expose internal exceptions.

Bad:

```json
{
  "error": "org.postgresql.util.PSQLException...",
  "stackTrace": "...",
  "database": "production-db"
}
```

Instead:

```json
{
  "type": "https://api.example.com/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have permission to perform this operation",
  "traceId": "abc123"
}
```

For authentication:

```http
401 Unauthorized
```

For authorization:

```http
403 Forbidden
```

And keep internal details in logs, not responses.

---

# 25. Audit logging

Normal application logs:

```text
GET /orders/123 → 200
```

are not enough for security-sensitive systems.

You may need security audit events:

```json
{
  "event": "ORDER_DELETED",
  "userId": "auth0|123",
  "orderId": "987",
  "timestamp": "...",
  "ip": "...",
  "traceId": "abc123"
}
```

Examples:

```text
LOGIN_SUCCESS
LOGIN_FAILURE
PASSWORD_CHANGED
ROLE_CHANGED
PERMISSION_CHANGED
ORDER_DELETED
USER_DELETED
API_KEY_CREATED
```

Audit logs should be:

* structured
* tamper-resistant where required
* retained according to policy
* access-controlled
* separated from ordinary debug logs when appropriate

---

# 26. Production filter architecture

Your Spring Security chain may conceptually look like:

```text
HTTP Request
     │
     ▼
┌──────────────────────────┐
│ Security Filter Chain    │
├──────────────────────────┤
│ CORS                     │
│ Security Headers         │
│ Bearer Token Filter      │
│ JWT Validation           │
│ SecurityContext          │
│ Authorization            │
│ Exception Handling       │
└─────────────┬────────────┘
              │
              ▼
       Controller
              │
              ▼
          Service
              │
              ▼
          Database
```

Around that, infrastructure provides:

```text
                Internet
                   │
                   ▼
                 WAF
                   │
                   ▼
            Load Balancer
                   │
                   ▼
             API Gateway
             /         \
       Rate limit     Routing
             │
             ▼
        Spring Security
```

And observability surrounds everything:

```text
                 ┌───────────────┐
                 │ Logs          │
                 │ Metrics       │
                 │ Traces        │
                 │ Audit events  │
                 └───────┬───────┘
                         │
                         ▼
                    Observability
```

---

# 27. A more realistic project structure

I would organize a production application something like:

```text
src/main/java/com/example/api/

├── config/
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   └── ObservabilityConfig.java
│
├── security/
│   ├── JwtAuthoritiesConverter.java
│   ├── SecurityErrorHandler.java
│   └── AuthorizationService.java
│
├── controller/
│   ├── OrderController.java
│   └── UserController.java
│
├── service/
│   ├── OrderService.java
│   └── UserService.java
│
├── repository/
│
├── audit/
│   ├── AuditEvent.java
│   └── AuditService.java
│
└── exception/
    └── GlobalExceptionHandler.java
```

Don't create:

```text
SecurityConfig.java
```

with 1,500 lines containing everything.

---

# 28. The production request lifecycle

This is probably the most important diagram to remember for interviews.

```text
                    HTTP Request
                         │
                         ▼
                ┌────────────────┐
                │ WAF             │
                └───────┬────────┘
                        │
                        ▼
                ┌────────────────┐
                │ API Gateway    │
                │                │
                │ Rate limiting  │
                │ TLS            │
                │ Routing        │
                └───────┬────────┘
                        │
                        ▼
                ┌────────────────┐
                │ Spring Boot    │
                └───────┬────────┘
                        │
                        ▼
             ┌──────────────────────┐
             │ SecurityFilterChain  │
             └──────────┬───────────┘
                        │
                        ▼
                 Bearer JWT
                        │
                        ▼
                  Auth0 validation
                        │
             ┌──────────┴───────────┐
             │                      │
          Invalid                 Valid
             │                      │
             ▼                      ▼
           401              Authentication
                                    │
                                    ▼
                              SecurityContext
                                    │
                                    ▼
                              Authorization
                                    │
                          ┌─────────┴─────────┐
                          │                   │
                       Denied              Allowed
                          │                   │
                          ▼                   ▼
                         403              Controller
                                             │
                                             ▼
                                           Service
                                             │
                                             ▼
                                          Database
```

Meanwhile:

```text
Request
  │
  ├── Logs ───────────────► Log system
  │
  ├── Metrics ────────────► Prometheus
  │
  ├── Trace ──────────────► OpenTelemetry
  │
  └── Audit ──────────────► Audit store
```

---

# 29. What I would actually deploy

For a typical enterprise Spring Boot REST API:

```text
                   ┌──────────────┐
                   │    Auth0     │
                   └──────┬───────┘
                          │
                     JWT Access Token
                          │
                          ▼
Client ──HTTPS──► CloudFront/WAF
                          │
                          ▼
                     API Gateway
                     ├── Rate limit
                     ├── Request size
                     ├── IP rules
                     └── Routing
                          │
                          ▼
                  Load Balancer
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
          Spring        Spring       Spring
          Instance      Instance     Instance
             │            │            │
             └────────────┼────────────┘
                          │
                     PostgreSQL
                          │
                        Redis
```

Spring:

```text
Auth0 JWT
   ↓
Spring Security
   ↓
Authorization
   ↓
Controller
   ↓
Service
   ↓
Repository
```

Observability:

```text
Spring
 ├── Logs ──► CloudWatch / ELK
 ├── Metrics ──► Prometheus ──► Grafana
 └── Traces ──► OpenTelemetry ──► Tempo/Datadog/etc.
```

---

# 30. Production checklist

Before calling a Spring Security API production-ready, I'd check:

### Authentication

* [ ] OAuth2/OIDC provider configured
* [ ] JWT signature validated
* [ ] issuer validated
* [ ] audience validated
* [ ] expiration validated
* [ ] HTTPS enforced
* [ ] no custom JWT parsing unless genuinely necessary
* [ ] short-lived access tokens
* [ ] refresh-token strategy handled by the IdP/client architecture

### Authorization

* [ ] endpoint authorization
* [ ] method authorization
* [ ] scopes/permissions defined
* [ ] roles mapped correctly
* [ ] ownership/business authorization where needed
* [ ] frontend is not trusted for authorization

### API protection

* [ ] CORS restricted
* [ ] CSRF decision explicitly made
* [ ] rate limiting
* [ ] request size limits
* [ ] WAF/API gateway
* [ ] security headers
* [ ] HTTPS
* [ ] secure error responses

### Observability

* [ ] structured logs
* [ ] correlation/request ID
* [ ] distributed tracing
* [ ] metrics
* [ ] authentication failure metrics
* [ ] authorization failure metrics
* [ ] audit logs

### Operations

* [ ] Actuator health checks
* [ ] secrets outside Git
* [ ] Swagger controlled/restricted
* [ ] database least-privilege account
* [ ] dependency vulnerability scanning
* [ ] container/image scanning
* [ ] security alerts
* [ ] log retention policy

---

## The key interview answer

If an interviewer asks:

> **"How would you set up Spring Security for a production REST API?"**

A strong answer is:

> "I would use Spring Security as an OAuth2 Resource Server and delegate identity management to an external IdP such as Auth0. The client obtains an access token from Auth0 and sends it as a Bearer token. Spring Security validates the JWT signature, issuer, audience and time-based claims, creates an Authentication and stores it in the SecurityContext. Authorization is then enforced using scopes, roles and method-level permissions. I'd keep the API stateless and avoid custom JWT filters unless there is a specific requirement.
>
> Around Spring Security, I'd put rate limiting and WAF protections at the gateway layer, configure restrictive CORS and appropriate security headers, expose only necessary Actuator endpoints, and keep Swagger restricted or disabled in production depending on the API. For observability I'd use structured logging, correlation IDs, Micrometer metrics and OpenTelemetry tracing. Security-sensitive operations would additionally generate audit events. Secrets would be stored in a secret manager rather than configuration files or Git."

That is the **production mindset** rather than simply:

```java
http
    .authorizeHttpRequests(...)
    .oauth2ResourceServer(...)
```

The configuration is the easy part. The difficult part is designing the **entire security boundary** correctly.

If you're learning this for interviews, the next useful step would be to take this architecture **one layer at a time**: **Auth0 → JWT validation → roles/scopes → Swagger → CORS/CSRF → rate limiting → logging/tracing → Actuator → production deployment**, with a complete Spring Boot project built along the way.

[1]: https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html?utm_source=chatgpt.com "OAuth2 :: Spring Security"
[2]: https://developer.auth0.com/resources/guides/api/spring/basic-authorization?utm_source=chatgpt.com "Authorization in Spring Boot"
[3]: https://springdoc.org/?utm_source=chatgpt.com "springdoc-openapi — OpenAPI 3 & Swagger UI for Spring Boot"
[4]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html?utm_source=chatgpt.com "OAuth 2.0 Resource Server JWT :: Spring Security"
[5]: https://docs.spring.io/spring-security/reference/7.0/servlet/integrations/observability.html?utm_source=chatgpt.com "Observability :: Spring Security"
[6]: https://developer.auth0.com/resources/guides/web-app/spring/basic-role-based-access-control?utm_source=chatgpt.com "Role-Based Access Control in Spring Boot"

