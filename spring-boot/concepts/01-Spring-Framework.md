Absolutely. We’ll approach **Spring Boot as an interview course**, not as a list of annotations to memorize.

Based on recent EPAM interview reports, Spring Boot questions are commonly mixed with **Core Java, JPA/Hibernate, REST, multithreading, testing, microservices, databases, and real production scenarios**. Recent EPAM postings also explicitly emphasize Spring Boot, REST APIs, Spring Data JPA/Hibernate, testing, CI/CD, observability, and containers/cloud. ([EPAM][1])

One current-version note: as of September 2026, Spring Boot **4.1.1** is the latest stable release, while 4.0.x and 3.5.x are also available. For interview preparation, we should understand the **fundamental Spring/Spring Boot concepts first**, then call out Boot 3/4 differences where relevant. ([Home][2])

# Spring Boot — EPAM Interview Preparation Roadmap

We’ll cover these **one at a time**, and we won’t move to the next topic until you say **next**.

### Part 1 — Spring Fundamentals

1. **Spring Framework vs Spring Boot**
2. **IoC and Dependency Injection**
3. **Spring Container, `BeanFactory`, `ApplicationContext`**
4. **Beans and Bean Lifecycle**
5. **Bean Scopes**
6. **Dependency Injection types**
7. **`@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`**
8. **`@ComponentScan`**
9. **`@Configuration` and `@Bean`**
10. **`@Autowired`, `@Qualifier`, `@Primary`**
11. **Circular dependencies**
12. **Spring Boot auto-configuration**
13. **`@SpringBootApplication` internals**
14. **Spring Boot starters**
15. **Embedded server**
16. **`application.properties` / `application.yml`**
17. **Profiles and external configuration**
18. **Conditional configuration**
19. **Spring Boot startup lifecycle**

### Part 2 — Web / REST

20. Spring MVC architecture
21. DispatcherServlet
22. Request flow inside Spring Boot
23. `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.
24. `@PathVariable`, `@RequestParam`, `@RequestBody`
25. DTOs and entity separation
26. Validation: `@Valid`, `@Validated`
27. Global exception handling
28. `@ControllerAdvice`, `@ExceptionHandler`
29. HTTP status codes
30. REST principles
31. Idempotency
32. Pagination and sorting
33. CORS
34. Interceptors and filters
35. Servlet filter vs Spring interceptor
36. Content negotiation
37. API versioning

### Part 3 — Spring Data JPA / Hibernate

38. Spring Data JPA fundamentals
39. Entity lifecycle
40. `@Entity`, `@Id`, `@GeneratedValue`
41. Relationships
42. Lazy vs eager loading
43. Cascade types
44. `mappedBy`
45. Persistence context
46. First-level cache
47. Dirty checking
48. JPQL vs native SQL
49. Derived query methods
50. `@Query`
51. Specifications
52. Pagination
53. N+1 query problem
54. `JOIN FETCH`
55. `@EntityGraph`
56. Optimistic locking
57. Pessimistic locking

### Part 4 — Transactions

58. `@Transactional`
59. Commit / rollback
60. Checked vs unchecked exception rollback
61. Transaction propagation
62. `REQUIRED`
63. `REQUIRES_NEW`
64. `MANDATORY`, `SUPPORTS`, `NOT_SUPPORTED`, etc.
65. Isolation levels
66. Dirty read / non-repeatable read / phantom read
67. Transaction proxy mechanism
68. Self-invocation problem
69. Read-only transactions

### Part 5 — Spring AOP

70. What is AOP?
71. Aspect / Advice / Pointcut / Join Point
72. `@Aspect`
73. `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing`, `@Around`
74. Proxy-based AOP
75. JDK dynamic proxy vs CGLIB
76. Why some `@Transactional` calls don't work
77. Practical logging / auditing / performance use cases

### Part 6 — Spring Security

78. Authentication vs authorization
79. Spring Security architecture
80. Security filter chain
81. `UserDetails`
82. Password encoding
83. JWT
84. Stateless authentication
85. OAuth2 / OpenID Connect
86. Roles vs authorities
87. Method-level security
88. CORS / CSRF
89. Securing REST APIs
90. Common security mistakes

### Part 7 — Testing

91. JUnit 5
92. Mockito
93. Unit vs integration testing
94. `@SpringBootTest`
95. `@WebMvcTest`
96. `@DataJpaTest`
97. MockMvc
98. `@Mock` vs `@MockBean` / current alternatives
99. Testcontainers
100. Testing transactions and repositories

### Part 8 — Actuator / Production

101. Spring Boot Actuator
102. Health checks
103. Metrics
104. Readiness vs liveness
105. Logging
106. Monitoring
107. Slow application debugging
108. Production troubleshooting
109. Configuration management
110. Observability

Recent EPAM reports specifically mention **Actuator** and production-slowdown troubleshooting as interview topics. ([LinkedIn][3])

### Part 9 — Microservices with Spring

111. Monolith vs microservices
112. Service-to-service communication
113. RestTemplate vs WebClient vs OpenFeign
114. API Gateway
115. Service discovery
116. Configuration server
117. Circuit breaker
118. Retry
119. Rate limiting
120. Bulkhead
121. Distributed tracing
122. Kafka / RabbitMQ integration
123. Event-driven architecture

### Part 10 — Distributed Transactions

124. Why normal transactions don't work across services
125. Saga
126. Choreography vs orchestration
127. Outbox pattern
128. Idempotency
129. Exactly-once misconceptions
130. 2PC
131. Eventual consistency

These are particularly relevant because recent EPAM interview reports include **Saga, Outbox, 2PC, circuit breakers, API gateways, and JWT/security**. ([LinkedIn][4])

### Part 11 — Advanced / Internal Spring Boot

132. BeanPostProcessor
133. FactoryBean
134. ApplicationContext events
135. Environment abstraction
136. `@Conditional`
137. AutoConfiguration internals
138. ConfigurationProperties
139. Proxy creation
140. Reflection in Spring
141. Spring startup sequence
142. How Spring discovers beans
143. How dependency injection actually happens

### Part 12 — Modern Spring Boot

144. Jakarta namespace / Boot 3+
145. Java 17 / 21
146. Virtual threads
147. AOT
148. Native images
149. Observability
150. Modern HTTP clients
151. Boot 4 / Spring Framework 7 changes

Spring's current documentation recommends constructor injection, and explains that `ApplicationContext` is the main container abstraction used in normal applications. ([Home][5])

---

# Topic 1 — Spring Framework vs Spring Boot

This is one of the **first questions** you should be ready for.

A typical interviewer might ask:

> **What is Spring? What is Spring Boot? What is the difference between them?**

A weak answer:

> "Spring is a framework and Spring Boot makes Spring easier."

That is technically true, but not enough for an EPAM interview.

We want to understand **why Spring Boot exists**.

---

# 1. What problem did Spring solve?

Before Spring, Java enterprise applications often had a lot of tightly coupled code and configuration.

Suppose we have:

```java
public class OrderService {

    private OrderRepository repository = new MySqlOrderRepository();

    public void createOrder(Order order) {
        repository.save(order);
    }
}
```

The problem is this line:

```java
new MySqlOrderRepository();
```

`OrderService` directly decides:

* which implementation to use
* how to create it
* how to configure it

So the classes are tightly coupled.

Imagine tomorrow we want:

```text
MySqlOrderRepository
        ↓
PostgresOrderRepository
```

or:

```text
RealPaymentService
        ↓
MockPaymentService
```

We have to modify the consuming class.

Spring introduced **IoC / Dependency Injection** so that object creation and wiring could be handled by the framework rather than by application classes themselves. Spring's IoC container creates, configures, and assembles the application's beans. ([Home][5])

Instead:

```java
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Now:

```text
OrderService
      |
      v
OrderRepository
      |
      +---- MySqlOrderRepository
      |
      +---- PostgresOrderRepository
```

The class depends on an abstraction rather than constructing a concrete dependency itself.

That's the fundamental idea behind **Spring Framework**.

---

# 2. What exactly is Spring Framework?

Spring Framework is a large ecosystem/framework that provides infrastructure for building Java applications.

Some major areas include:

```text
Spring Framework
│
├── IoC / Dependency Injection
├── AOP
├── Spring MVC
├── Transaction Management
├── Data Access
├── Validation
└── Integration with other technologies
```

The most important foundation is:

```text
IoC Container
       ↓
creates and manages Beans
       ↓
injects dependencies
       ↓
application runs
```

The `ApplicationContext` represents the Spring IoC container and is responsible for instantiating, configuring, and assembling beans. ([Home][5])

---

# 3. Then why was Spring Boot created?

Spring Framework itself is powerful.

But historically, creating a production application using Spring required a lot of setup and configuration.

For example, you might need to configure:

```text
Database
Connection pool
JPA
Hibernate
Transaction management
DispatcherServlet
Web server
JSON serialization
Logging
Security
etc.
```

And historically this often involved lots of configuration.

Spring Boot's goal was essentially:

> **Take Spring and make it much easier to start, configure, run, and operate.**

Spring Boot describes itself as making it easy to create **stand-alone, production-grade Spring applications**, with opinionated defaults, starters, auto-configuration, embedded servers, and production-ready features. ([Home][2])

---

# 4. Spring vs Spring Boot

Think of it like this:

```text
Spring Framework
      ↓
Provides the building blocks

Spring Boot
      ↓
Uses Spring + sensible defaults
+ auto-configuration
+ starters
+ embedded server
+ external configuration
+ production features
```

### Spring Framework

Gives you:

```text
IoC
DI
AOP
MVC
Transactions
Security integration
Data access
etc.
```

### Spring Boot

Adds conventions and tooling around Spring:

```text
Auto-configuration
Starters
Embedded server
Externalized configuration
Actuator
Easy application startup
Production-oriented defaults
```

---

# 5. A simple analogy

Imagine building a house.

### Spring Framework

Gives you:

```text
Bricks
Cement
Steel
Wood
Tools
Blueprint components
```

You can build almost anything.

But you have to decide:

```text
What goes where?
How should everything be configured?
What defaults should I use?
```

### Spring Boot

Gives you:

```text
A well-organized construction setup
with sensible defaults
and common components already configured.
```

You can still customize it, but you don't have to configure everything from scratch.

---

# 6. What is "Convention over Configuration"?

This is an extremely important Spring Boot concept.

Instead of requiring developers to specify every single configuration detail, Spring Boot makes reasonable assumptions.

For example, you add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

Spring Boot recognizes that you're creating a web application and configures a large portion of the infrastructure for you.

This is one of the reasons Spring Boot applications can have surprisingly little explicit configuration.

---

# 7. What are Spring Boot Starters?

Suppose you want to build a REST API.

Without starters, you might manually select multiple dependencies.

For example:

```text
Spring MVC
Jackson
Validation
Embedded server
Logging
etc.
```

With Spring Boot:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

The starter provides a curated dependency set for the capability you need.

Common examples:

```text
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-test
```

This is an extremely common interview question:

> What is a Spring Boot starter?

Good answer:

> A starter is a dependency descriptor that brings together the typical dependencies required for a particular Spring Boot capability, reducing manual dependency management.

---

# 8. What is Auto-Configuration?

This is arguably the **most important Spring Boot-specific concept**.

Consider:

```java
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

There isn't a giant configuration file saying:

```text
Create DispatcherServlet
Create ObjectMapper
Create embedded Tomcat
Configure Jackson
...
```

So how does Spring Boot know what to configure?

### Auto-configuration.

Spring Boot examines:

```text
Classpath
+
Existing beans
+
Application properties
+
Environment
+
Configuration conditions
```

and determines what configuration should be applied.

Conceptually:

```text
Classpath contains JPA
       ↓
Spring Boot detects JPA
       ↓
Checks conditions
       ↓
Configures relevant JPA infrastructure
```

This is why auto-configuration is often described as:

> **Configuration based on what is available and what the application needs, using conditional configuration.**

This is one of the areas where interviewers can go deeper.

---

# 9. Example: DataSource auto-configuration

Suppose you add:

```xml
spring-boot-starter-data-jpa
```

and configure:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/orders
spring.datasource.username=postgres
spring.datasource.password=password
```

Spring Boot can automatically configure much of the database infrastructure.

Conceptually:

```text
JPA dependency present
        +
Database properties present
        ↓
Boot detects configuration
        ↓
DataSource
        ↓
EntityManagerFactory
        ↓
Transaction infrastructure
```

You don't normally create all those objects manually.

That is Spring Boot doing work for you.

---

# 10. What is `@SpringBootApplication`?

This is another **must-know interview question**.

```java
@SpringBootApplication
public class OrderApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

A very important interview point:

`@SpringBootApplication` is effectively a combination of:

```java
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

Conceptually:

```text
@SpringBootApplication
       |
       +---- @Configuration
       |
       +---- @EnableAutoConfiguration
       |
       +---- @ComponentScan
```

So when someone asks:

> What happens when `SpringApplication.run()` is executed?

This should immediately trigger a deeper explanation in your mind:

```text
Bootstrap application
       ↓
Create ApplicationContext
       ↓
Load configuration
       ↓
Component scanning
       ↓
Auto-configuration
       ↓
Bean definitions
       ↓
Dependency injection
       ↓
Bean initialization
       ↓
Embedded web server
       ↓
Application ready
```

We will later study this **startup process in detail** because it is a very common follow-up.

---

# 11. Embedded Server

Traditional Java web applications commonly involved deploying an application to an external application server.

Spring Boot popularized the model:

```text
Your application
      +
Embedded Tomcat / Jetty / Undertow
      ↓
java -jar application.jar
```

Spring Boot explicitly supports embedding Tomcat, Jetty, or Undertow, so the application can be run as a stand-alone application without requiring traditional WAR deployment. ([Home][2])

Example:

```bash
java -jar order-service.jar
```

That is a major difference in developer experience.

---

# 12. Externalized Configuration

Spring Boot also makes application configuration easier.

Instead of hardcoding:

```java
String url = "jdbc:postgresql://localhost:5432/order";
```

we use configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/order
```

and can override configuration using environments such as:

```text
application.properties
application.yml
environment variables
command-line arguments
profile-specific configuration
```

This becomes especially important in:

```text
Development
Testing
Staging
Production
```

because the application code shouldn't need to change simply because the database URL changes.

---

# 13. Spring Boot does NOT replace Spring

This is an extremely common trick question.

Wrong:

> Spring Boot is an alternative to Spring.

Correct:

> Spring Boot is built on top of the Spring ecosystem and simplifies the configuration, bootstrapping, and operational setup of Spring applications.

Think:

```text
Spring Framework
       ↑
       |
Spring Boot
```

Not:

```text
Spring Framework    VS    Spring Boot
```

They're not competing frameworks.

---

# 14. Spring vs Spring Boot — interview table

| Spring Framework                           | Spring Boot                            |
| ------------------------------------------ | -------------------------------------- |
| Core framework                             | Built around Spring                    |
| IoC / DI                                   | Uses Spring IoC/DI                     |
| More explicit configuration historically   | Convention + defaults                  |
| No automatic application setup by itself   | Auto-configuration                     |
| Doesn't inherently require embedded server | Embedded server support                |
| Flexible                                   | Opinionated defaults                   |
| Core building blocks                       | Simplified application development     |
| Can require more setup                     | Reduced boilerplate                    |
| Broader underlying framework               | Bootstrapping/production-focused layer |

---

# 15. The answer I'd give in an EPAM interview

Suppose the interviewer asks:

> **What is the difference between Spring and Spring Boot?**

A strong answer:

> Spring Framework provides the core infrastructure for Java applications, particularly IoC, dependency injection, AOP, web MVC, transaction management, and data access.
>
> Spring Boot is built on top of Spring and simplifies application development by providing convention-over-configuration, auto-configuration, starter dependencies, embedded servers, externalized configuration, and production-oriented features such as Actuator.
>
> So Spring provides the underlying framework capabilities, while Spring Boot reduces the amount of configuration and setup required to build and run a Spring application.

That is a good **30–40 second answer**.

Then stop.

Don't immediately dump 5 minutes of information unless they ask.

---

# 16. EPAM-style follow-up questions

Once you answer the basic question, the interviewer can move into these:

### Basic

> What is Spring?

> What is Spring Boot?

> Why do we use Spring Boot?

> What are the advantages of Spring Boot?

> Spring vs Spring Boot?

### Intermediate

> What is auto-configuration?

> How does Spring Boot perform auto-configuration?

> What is a starter?

> What does `@SpringBootApplication` contain?

> How does Spring Boot know which beans to configure?

> Why does Spring Boot use an embedded server?

### Advanced

> Explain `SpringApplication.run()`.

> How does Spring Boot create the `ApplicationContext`?

> How does component scanning work?

> How does auto-configuration decide whether to create a bean?

> What happens if a bean already exists?

> What happens if auto-configuration conflicts with your custom configuration?

> How would you debug unexpected auto-configuration?

These deeper questions are where **Spring Boot knowledge becomes Spring internals knowledge**.

---

# 17. One important distinction: Spring Bean vs Java Object

This will become extremely important in the next topic.

Suppose:

```java
OrderService service = new OrderService();
```

That's simply a Java object.

But:

```java
@Service
public class OrderService {
}
```

Spring can discover it during component scanning and register/manage it as a **Spring bean**.

Spring defines a bean as an object instantiated, assembled, and managed by the Spring IoC container. ([Home][5])

So:

```text
Java Object
    ↓
created with new
    ↓
ordinary object

Spring Bean
    ↓
created/managed by Spring IoC container
    ↓
dependencies + lifecycle + scope + post-processing
```

This distinction leads directly to our next major topic.

---

# 18. What you should remember from Topic 1

The mental model should be:

```text
                SPRING
                  │
        ┌─────────┴─────────┐
        │                   │
     IoC / DI              AOP
        │
        ↓
 ApplicationContext
        │
        ↓
      Beans
```

And:

```text
           SPRING BOOT
                │
        ┌───────┼────────┐
        ↓       ↓        ↓
   Auto-config Starters Embedded
                         Server
        │
        ↓
 External configuration
        │
        ↓
 Production features
```

The key sentence:

> **Spring provides the core application framework; Spring Boot simplifies using Spring by providing conventions, auto-configuration, starters, easy bootstrapping, and production-oriented features.**

---

## The depth level we'll use

For every topic from here on, I'll cover:

**1. What it is**
**2. Why it exists**
**3. What problem it solves**
**4. How it works internally**
**5. Code examples**
**6. Real-world usage**
**7. Common mistakes**
**8. EPAM-style interview questions**
**9. Follow-up questions interviewers may ask**
**10. A strong interview answer**
**11. Tricky scenarios / debugging questions**

That way, you're preparing to **explain Spring Boot**, not merely use it.

**Next topic: IoC (Inversion of Control) and Dependency Injection — the foundation of Spring.**

[1]: https://careers.epam.com/en/vacancy/back-end-java-developer-spring-boot-bltq4di0mrb6i6436z6_en?utm_source=chatgpt.com "Back-end Java Developer (Spring Boot, Microservices) | Top Projects At EPAM"
[2]: https://spring.io/projects/spring-boot/?utm_source=chatgpt.com "Spring Boot"
[3]: https://www.linkedin.com/posts/swapnil-pawar-180a6b22b_epam-java-springboot-activity-7449509660878970880-UjRn?utm_source=chatgpt.com "EPAM Interview Questions: Java and Spring Boot | Swapnil Pawar posted on the topic | LinkedIn"
[4]: https://www.linkedin.com/posts/jagdish-khetre_java-springboot-microservices-activity-7503523538000273409-iXrz?utm_source=chatgpt.com "EPAM Systems Java Spring Boot Microservices Interview Questions | Jagdish Khetre posted on the topic | LinkedIn"
[5]: https://docs.spring.io/spring-framework/reference/core/beans/introduction.html?utm_source=chatgpt.com "Introduction to the Spring IoC Container and Beans :: Spring Framework"

