# Spring Boot Interview Prep: Concept by Concept

Here's the plan. We'll go one concept at a time, from easy to advanced:

1. **Spring Boot Basics** (we start here)
2. Starters and Auto-Configuration
3. IoC, Dependency Injection and Beans
4. REST APIs and Common Annotations
5. Configuration, Properties and Profiles
6. Spring Data JPA
7. Exception Handling and Validation
8. Spring Security
9. Actuator and Monitoring
10. Testing
11. AOP and Transactions
12. Microservices and Spring Cloud
13. Advanced Internals (startup process, conditionals, custom starters)
14. Performance, Deployment and Best Practices

---

# Concept 1: Spring Boot Basics

## Q1. What is Spring Boot?

**Short answer:** Spring Boot is a framework built on top of Spring that helps you create production-ready applications quickly, with very little setup.

**Deeper explanation:**
Before Spring Boot, building a Spring app meant a lot of boring setup. You wrote XML files, picked compatible library versions yourself, configured a web server, and wired everything by hand. Even a "Hello World" took time.

Spring Boot fixes this with one idea: **sensible defaults**. It looks at what you have in your project and configures most things automatically. You focus on business logic, not plumbing.

Spring Boot is **not** a replacement for Spring. It uses Spring underneath. Think of Spring as the engine, and Spring Boot as the car built around it, with the keys already in the ignition.

---

## Q2. What is the difference between Spring and Spring Boot?

| | Spring Framework | Spring Boot |
|---|---|---|
| Purpose | Core framework (DI, AOP, MVC, etc.) | Makes Spring easy to set up and run |
| Configuration | Mostly manual (XML or Java config) | Mostly automatic |
| Server | You deploy a WAR to an external server like Tomcat | Server is embedded, just run a JAR |
| Dependencies | You manage versions yourself | Starters manage compatible versions |
| Setup time | Slow | Fast |

**How to say it in an interview:**
"Spring gives you the tools. Spring Boot gives you those tools already configured. With Spring, I decide and configure everything. With Spring Boot, I get good defaults and change only what I need."

---

## Q3. What are the main advantages of Spring Boot?

- **Auto-configuration:** It configures beans based on the libraries on your classpath.
- **Starter dependencies:** One dependency brings a whole set of compatible libraries.
- **Embedded server:** Tomcat, Jetty or Undertow is built into your app.
- **No XML needed:** Almost everything is done with annotations and properties.
- **Production-ready features:** Health checks, metrics and monitoring come from Actuator.
- **Easy to run:** `java -jar myapp.jar` and it works.

**Why it matters:** Each of these removes a step that used to cause bugs or waste time. That's why Spring Boot became the default for microservices in Java.

---

## Q4. What does `@SpringBootApplication` do?

**Short answer:** It's a shortcut annotation that combines three annotations.

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

It is the same as writing:

1. **`@Configuration`**: This class can define beans.
2. **`@EnableAutoConfiguration`**: Turn on Spring Boot's automatic setup.
3. **`@ComponentScan`**: Scan this package and its sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` and so on.

**Important detail interviewers love:**
Component scanning starts from the package of the main class. If your main class is in `com.example.demo`, Spring scans `com.example.demo.*`. A class in `com.other.stuff` will **not** be found, and you'll get "bean not found" errors. This is a very common beginner mistake.

---

## Q5. What is an embedded server? Why is it useful?

**Short answer:** It's a web server (Tomcat by default) packaged inside your application.

**Deeper explanation:**
In the old style, you built a WAR file, installed Tomcat on a machine, and copied the WAR into it. Two things had to be managed: the server and the app.

With Spring Boot, the server starts as part of your app. When your app runs, the server runs. When it stops, the server stops.

**Benefits:**
- Same environment on your laptop and in production, so fewer "works on my machine" problems.
- Perfect for Docker and cloud, because you ship one JAR.
- Easy to change: swap Tomcat for Jetty by changing a dependency.

**Change the port:**
```properties
server.port=9090
```

---

## Q6. What happens when you call `SpringApplication.run()`?

**Simple version:**
1. Spring Boot creates the **application context**, which is the container that holds all your beans.
2. It sets up the **environment**, meaning it reads properties, profiles and so on.
3. It performs **component scanning** and **auto-configuration**, and creates the beans.
4. It starts the **embedded server**, if it's a web app.
5. Your app is now ready to take requests.

**Tip for interviews:** You don't need every internal detail at this level. Just show that you understand the flow: *context creation → configuration → bean creation → server start*. We'll go deeper on this in Concept 13.

---

## Q7. What is Spring Initializr?

It's a web tool at **start.spring.io** that generates a starter project for you. You choose:
- Build tool (Maven or Gradle)
- Language (Java, Kotlin)
- Spring Boot version
- Dependencies (Web, JPA, Security, etc.)

It gives you a ZIP file with the right folder structure and a working `pom.xml` or `build.gradle`. IDEs like IntelliJ and Eclipse (STS) use it behind the scenes too.

---

## Q8. What is the default project structure of a Spring Boot app?

```
src/main/java/com/example/demo/
    DemoApplication.java      ← main class
    controller/
    service/
    repository/
src/main/resources/
    application.properties    ← configuration
    static/                   ← CSS, JS, images
    templates/                ← HTML templates (Thymeleaf)
src/test/java/...             ← tests
pom.xml                       ← dependencies
```

**Good practice:** Keep the main class in the **root package**, with all other packages below it. This makes component scanning work without extra setup.

---

## Quick Recap

- Spring Boot = Spring + auto-setup + embedded server + starters.
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.
- Run it as a simple JAR. No external server is needed.
- Component scanning depends on the main class's package.

---

Reply **"next"** and I'll move to **Concept 2: Starters and Auto-Configuration**. That is the heart of Spring Boot and an interview favorite. Or tell me if you want any question here explained deeper.

---
---
---

# Concept 2: Starters and Auto-Configuration

This is the heart of Spring Boot. Almost every interview asks about it, so take your time here.

---

## Q1. What is a Starter in Spring Boot?

**Short answer:** A starter is a single dependency that pulls in a whole group of related libraries, with versions that already work well together.

**Deeper explanation:**
Imagine you want to build a REST API. Without starters, you would add Spring MVC, Jackson (for JSON), Tomcat, validation libraries, logging and more. You would also have to pick versions that don't clash with each other. That is painful and error-prone.

With a starter, you write this:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

That one line brings in Spring MVC, an embedded Tomcat, Jackson and logging, all with compatible versions.

A starter is basically a **shopping basket** of libraries. You say "I want to build a web app," and Boot fills the basket for you.

**Naming rule:** Official starters are named `spring-boot-starter-*`. Third-party starters usually use `*-spring-boot-starter`.

---

## Q2. Name some commonly used starters.

| Starter | What it gives you |
|---|---|
| `spring-boot-starter-web` | REST APIs and web apps (Spring MVC + Tomcat) |
| `spring-boot-starter-data-jpa` | Database access using JPA and Hibernate |
| `spring-boot-starter-security` | Authentication and authorization |
| `spring-boot-starter-test` | JUnit, Mockito, AssertJ, Spring Test |
| `spring-boot-starter-validation` | Bean Validation (`@NotNull`, `@Size`, etc.) |
| `spring-boot-starter-actuator` | Health checks and metrics |
| `spring-boot-starter-thymeleaf` | HTML templates |
| `spring-boot-starter-webflux` | Reactive web apps |

---

## Q3. What is `spring-boot-starter-parent`? Why do we use it?

**Short answer:** It's a special parent POM that manages dependency versions and gives good default build settings.

**Deeper explanation:**
Notice that in the starter example above, we did **not** write a version number. That works because of the parent:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>
```

The parent contains a big list of tested versions (called a **BOM**, Bill of Materials). When you say "I want Jackson," Boot already knows which Jackson version is safe for Spring Boot 3.3.0.

It also sets the Java version, UTF-8 encoding and plugin settings.

**Follow-up question: "What if my company already has its own parent POM?"**
Maven allows only one parent. In that case, skip the Boot parent and import the BOM instead:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.3.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

You get the same version management without needing the parent.

---

## Q4. What is Auto-Configuration?

**Short answer:** Auto-configuration means Spring Boot automatically creates and configures beans for you, based on what is on your classpath and what you have already defined.

**Deeper explanation:**
Think about what happens when you add `spring-boot-starter-web`. You never write code to create a `DispatcherServlet`, start Tomcat, or set up a JSON converter. But they all exist. Who created them? Auto-configuration.

Boot asks itself questions like:
- "Is Spring MVC on the classpath?" → then set up `DispatcherServlet`.
- "Is a database driver on the classpath, and is a `DataSource` missing?" → then create one.
- "Did the developer already define this bean?" → then back off and use theirs.

The last point is very important. **Auto-configuration is smart but polite.** It gives you defaults, but it always steps aside when you define your own.

---

## Q5. How does Auto-Configuration actually work?

This is the question that separates beginners from strong candidates. Here's the flow:

1. `@SpringBootApplication` includes `@EnableAutoConfiguration`.
2. That annotation tells Spring Boot to load a list of **auto-configuration classes**.
3. The list lives in a file inside the Spring Boot JARs:
   - **Spring Boot 3.x:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - **Older versions (before 2.7):** `META-INF/spring.factories`
4. Each class in that list is a normal `@Configuration` class, but full of **conditions**.
5. Spring checks each condition. If they pass, the beans get created. If not, the class is skipped.

So Boot has hundreds of possible configurations ready, but only the ones that match your project actually turn on.

**A simplified example of an auto-configuration class:**

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        // create a default DataSource
    }
}
```

Read it like English: "If `DataSource` class exists, and the user hasn't made their own `DataSource` bean, create one."

---

## Q6. What are Conditional annotations? Name some.

These are the "if" statements of Spring Boot. They decide whether a bean or configuration should be created.

| Annotation | Meaning |
|---|---|
| `@ConditionalOnClass` | Create only if a class is on the classpath |
| `@ConditionalOnMissingClass` | Create only if a class is **not** on the classpath |
| `@ConditionalOnBean` | Create only if another bean exists |
| `@ConditionalOnMissingBean` | Create only if no such bean exists yet |
| `@ConditionalOnProperty` | Create only if a property has a certain value |
| `@ConditionalOnWebApplication` | Create only in a web app |
| `@ConditionalOnResource` | Create only if a file/resource exists |

**Example with `@ConditionalOnProperty`:**

```java
@Bean
@ConditionalOnProperty(name = "feature.email.enabled", havingValue = "true")
public EmailService emailService() {
    return new EmailService();
}
```

If you set `feature.email.enabled=true` in `application.properties`, this bean is created. Otherwise it isn't. This is a neat way to build **feature toggles**.

---

## Q7. How can you override or customize auto-configuration?

You have three main options, from lightest to strongest:

**1. Change a property (easiest):**
```properties
server.port=9090
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
```

**2. Define your own bean (Boot backs off):**
```java
@Configuration
public class MyConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
```
Because the auto-configured `ObjectMapper` uses `@ConditionalOnMissingBean`, Boot sees yours and skips its own.

**3. Exclude the auto-configuration completely:**
```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DemoApplication { }
```
Or in properties:
```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

**When do you need option 3?**
A classic case: you add a JPA dependency but don't want a database yet, and the app fails at startup asking for a `DataSource`. Excluding that auto-configuration fixes it.

---

## Q8. How do you see which auto-configurations are being applied?

Turn on the debug flag:

```properties
debug=true
```

Or run with `--debug`. Boot prints a **Conditions Evaluation Report** with two important sections:

- **Positive matches:** configurations that were applied, and why.
- **Negative matches:** configurations that were skipped, and which condition failed.

**Why this matters:** When something "magical" is not working (or is working when you didn't expect it), this report tells you exactly what Boot decided. It's the first thing an experienced developer checks. Mentioning it in an interview shows real hands-on experience.

If you have Actuator, the `/actuator/conditions` endpoint gives the same information.

---

## Q9. What happens if you have two starters that conflict? For example, both Tomcat and Jetty?

`spring-boot-starter-web` brings Tomcat by default. If you add Jetty without removing Tomcat, both are on the classpath and Tomcat usually wins because its auto-configuration takes priority.

To switch properly, **exclude Tomcat** and add Jetty:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

This shows a key idea: **auto-configuration reacts to the classpath.** Change the libraries, and the behavior changes. No code needed.

---

## Q10. Is Auto-Configuration the same as component scanning?

No, and people often mix these up.

| | Component Scanning | Auto-Configuration |
|---|---|---|
| Finds | **Your** classes (`@Service`, `@Controller`, etc.) | **Boot's** predefined configuration classes |
| Where from | Your package and sub-packages | A fixed list in the Boot JARs |
| Decided by | Annotations on your classes | Conditions (classpath, beans, properties) |

Simple way to remember: **component scanning loads what you wrote; auto-configuration loads what Boot prepared for you.**

---

## Q11. (Advanced) What is the order of auto-configuration? Can you control it?

Auto-configurations don't run randomly. Sometimes one depends on another. For example, JPA needs a `DataSource` to exist first.

You can control order with:

```java
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
public class MyJpaConfig { }
```

- `after` means run after that class.
- `before` means run before that class.

**Important:** `@ConditionalOnBean` only works reliably in auto-configuration classes, because those are processed **after** your own beans are registered. That's why the ordering rules exist. If you use `@ConditionalOnBean` in a regular config class, the bean it's checking for might not be registered yet, and the condition can fail unexpectedly.

---

## Q12. (Advanced) Why does Boot still work if I never use half of those auto-configurations?

Because of the conditions. Boot has around 150+ auto-configuration classes, but most fail their `@ConditionalOnClass` check immediately, since the needed library isn't on your classpath. Skipping them is cheap: Boot reads the class metadata without even loading the class.

That's the design: **ship lots of options, activate only what matches.**

---

## Quick Recap

- **Starter** = one dependency that brings many compatible libraries.
- **`starter-parent` / BOM** = manages versions so you skip writing them.
- **Auto-configuration** = Boot creates beans for you based on classpath, existing beans and properties.
- It loads from `AutoConfiguration.imports` (Boot 3) or `spring.factories` (older).
- **`@Conditional...` annotations** decide if a configuration applies.
- Your own beans always win, thanks to `@ConditionalOnMissingBean`.
- Use `debug=true` to see what was applied and skipped.
- Component scanning = your code. Auto-configuration = Boot's code.

---

Reply **"next"** for **Concept 3: IoC, Dependency Injection and Beans**. This is the core of Spring itself, and it's where many interviews go deep. Or tell me if you want me to explain anything here again.

---
---
---

# Concept 3: IoC, Dependency Injection and Beans

This is the core of Spring itself. Even if you know Spring Boot well, interviewers use this topic to check whether you understand *why* the framework works the way it does.

---

## Q1. What is IoC (Inversion of Control)?

**Short answer:** IoC means you give the control of creating and managing objects to a framework, instead of doing it yourself.

**Deeper explanation:**
Normally, your code controls everything. If class A needs class B, then A creates B:

```java
public class OrderService {
    private PaymentService paymentService = new PaymentService(); // I create it myself
}
```

Here `OrderService` is in charge. It decides which class to use, and when to create it.

With IoC, the control is **inverted**. You only say "I need a `PaymentService`," and the framework creates it and hands it to you.

```java
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService; // Spring gives it to me
    }
}
```

**A real-life picture:** In a restaurant, you don't go into the kitchen to cook. You order, and the food arrives. The kitchen (Spring) controls how the food is made. You just use it.

**Why it matters:**
- Classes are not tied to one specific implementation.
- Testing is easier, because you can pass a fake object instead of the real one.
- Object creation logic stays in one place.

---

## Q2. What is Dependency Injection (DI)? How is it different from IoC?

**Short answer:** DI is the *way* Spring implements IoC. It means Spring "injects" the objects a class needs (its dependencies) from outside.

**The difference in one line:**
**IoC is the principle. DI is the technique.** IoC is the big idea ("framework controls objects"). DI is how it's done ("framework passes the needed objects into your class").

A **dependency** is simply another object your class needs to do its job. `OrderService` depends on `PaymentService`.

---

## Q3. What is the IoC Container?

**Short answer:** It's the part of Spring that creates objects, wires them together, and manages their whole life.

**Deeper explanation:**
The container reads your configuration (annotations, Java config), creates the objects (beans), figures out who needs whom, injects the dependencies, and keeps the beans until the app shuts down.

Spring has two main container types:

| | `BeanFactory` | `ApplicationContext` |
|---|---|---|
| Level | Basic container | Advanced container (extends `BeanFactory`) |
| Bean creation | Lazy (when first asked) | Eager for singletons (at startup) |
| Extra features | Very few | Events, internationalization, annotation support, easy AOP |
| Used in practice | Almost never | **Always** (Spring Boot uses this) |

Because `ApplicationContext` creates singletons at startup, **configuration errors show up immediately** when the app starts, not later at runtime. That is a big practical benefit.

---

## Q4. What is a Bean?

**Short answer:** A bean is an object that is created, managed and wired by the Spring container.

**Deeper explanation:**
Not every object in your app is a bean. If you write `new Student()` yourself, that's a normal Java object. Spring doesn't know about it. A bean is only an object that **Spring owns**.

Two common ways to make a bean:

```java
@Service                         // 1. Mark the class
public class EmailService { }

@Configuration
public class AppConfig {
    @Bean                        // 2. Write a method that returns the object
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

The difference matters. Objects you create with `new` don't get injection, transactions, AOP or lifecycle callbacks. Beans do.

---

## Q5. What are the types of Dependency Injection? Which one is best?

There are three types.

**1. Constructor injection**
```java
@Service
public class OrderService {
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

**2. Setter injection**
```java
@Service
public class OrderService {
    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

**3. Field injection**
```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService;
}
```

**Best choice: Constructor injection.** Here is why:

- **Fields can be `final`.** The object is fully ready and can't be changed later.
- **No half-built objects.** You cannot create `OrderService` without giving it a `PaymentService`.
- **Easy unit testing.** Just call `new OrderService(mockPayment)`. No Spring needed. With field injection, you need reflection or the Spring container to set private fields.
- **Problems show early.** Circular dependencies are detected at startup.
- **Too many constructor parameters is a warning sign** that your class does too much.

**When is setter injection fine?** For **optional** dependencies that have a sensible default.

**Why avoid field injection?** It looks short and simple, but it hides dependencies, blocks `final`, and makes tests harder.

**Small detail:** If a class has only **one constructor**, `@Autowired` is not needed. Spring uses it automatically (since Spring 4.3).

---

## Q6. What is the difference between `@Component`, `@Service`, `@Repository` and `@Controller`?

All four tell Spring "make a bean out of this class." They are called **stereotype annotations**.

| Annotation | Used for | Extra behavior |
|---|---|---|
| `@Component` | Any general class | None, it's the base annotation |
| `@Service` | Business logic layer | None (only meaning and clarity) |
| `@Repository` | Data access layer | **Translates database exceptions** into Spring's `DataAccessException` |
| `@Controller` | Web layer | Handles web requests; returns view names |

`@Service`, `@Repository` and `@Controller` are all built on top of `@Component`.

**The honest answer for interviews:** Technically, `@Service` behaves the same as `@Component`. We use it to make the code readable and to allow tools or AOP to target a layer. But `@Repository` really adds something: exception translation. So a raw `SQLException` from one database becomes a consistent Spring exception, and your service layer doesn't care which database you use.

---

## Q7. What is the difference between `@Component` and `@Bean`?

| | `@Component` | `@Bean` |
|---|---|---|
| Where it goes | On a **class** | On a **method** |
| Detected by | Component scanning | Inside a `@Configuration` class |
| Best for | Your own classes | **Third-party classes** you can't edit |
| Control | Less | Full control over how the object is built |

**Example:** You cannot add `@Component` to `RestTemplate` or `ObjectMapper`, because you don't own that code. So you write a `@Bean` method that creates and returns it.

Use `@Bean` also when object creation needs some logic (like setting timeouts or choosing values based on conditions).

---

## Q8. What does `@Autowired` do? How does Spring decide which bean to inject?

**Short answer:** `@Autowired` tells Spring to find a matching bean and inject it.

**How Spring finds the match:**
1. First, it looks **by type**. "I need a `PaymentService`. Which beans are of that type?"
2. If exactly one is found, done.
3. If more than one is found, it tries to narrow down using `@Primary`, `@Qualifier`, or **by name** (it compares the field or parameter name with the bean name).
4. If it still cannot decide, it throws `NoUniqueBeanDefinitionException`.
5. If none is found, it throws `NoSuchBeanDefinitionException`, unless the dependency is optional.

---

## Q9. What if there are two beans of the same type? (`@Primary` and `@Qualifier`)

Suppose you have two implementations:

```java
public interface PaymentService { void pay(); }

@Service
public class CardPaymentService implements PaymentService { ... }

@Service
public class UpiPaymentService implements PaymentService { ... }
```

Now Spring sees two candidates and gets confused. You have two ways to solve it.

**Option 1: `@Primary`, which sets the default**
```java
@Service
@Primary
public class CardPaymentService implements PaymentService { ... }
```
"When in doubt, use this one."

**Option 2: `@Qualifier`, which picks one exactly**
```java
public OrderService(@Qualifier("upiPaymentService") PaymentService paymentService) { ... }
```

The default bean name is the class name with a lowercase first letter (`upiPaymentService`).

**Which one wins if both are used?** `@Qualifier` wins. `@Primary` is the general default, and `@Qualifier` is the specific choice at the injection point.

**Bonus trick:** You can inject **all** implementations together:
```java
public OrderService(List<PaymentService> services) { ... }
// or Map<String, PaymentService>, where the key is the bean name
```
This is very useful for the Strategy pattern.

---

## Q10. What are bean scopes?

A scope decides **how many instances** Spring creates and how long they live.

| Scope | Meaning |
|---|---|
| **singleton** (default) | One instance for the whole application |
| **prototype** | A new instance every time it's requested |
| **request** | One instance per HTTP request (web apps) |
| **session** | One instance per HTTP session |
| **application** | One instance per `ServletContext` |
| **websocket** | One instance per WebSocket session |

```java
@Component
@Scope("prototype")
public class ShoppingCart { }
```

**Important point:** "Singleton" in Spring is **not** the same as the Singleton design pattern. Spring's singleton means one instance **per container** (per bean definition). The Java design pattern means one instance per JVM or class loader. You can have two Spring beans of the same class, each being a singleton in its own bean definition.

---

## Q11. Are singleton beans thread-safe?

**No. Spring does not make them thread-safe.**

Since one instance is shared by many requests at the same time, any **changeable state** inside it can cause bugs.

```java
@Service
public class CounterService {
    private int count = 0;               // DANGEROUS: shared by all threads

    public void increment() { count++; } // race condition
}
```

**How to stay safe:**
- Keep singleton beans **stateless**. Use only `final` dependencies and local variables.
- If you must keep state, use thread-safe types like `AtomicInteger` or `ConcurrentHashMap`.
- Use `request` scope or `prototype` scope for stateful objects.

Most services, controllers and repositories are stateless, and that's why singleton works well.

---

## Q12. Explain the Bean Lifecycle.

This is a classic interview question. Learn the order.

**Creation steps:**
1. **Instantiate:** Spring creates the object (calls the constructor).
2. **Populate properties:** Dependencies are injected.
3. **Aware callbacks:** If the bean implements interfaces like `BeanNameAware` or `ApplicationContextAware`, those methods run.
4. **`BeanPostProcessor` before-init:** `postProcessBeforeInitialization` runs.
5. **Init callbacks:** `@PostConstruct`, then `afterPropertiesSet()` (if it implements `InitializingBean`), then a custom `initMethod`.
6. **`BeanPostProcessor` after-init:** `postProcessAfterInitialization` runs. This is where **proxies** (AOP, `@Transactional`) are usually created.
7. **Bean is ready to use.**

**Destruction steps** (when the context closes):
8. `@PreDestroy`
9. `destroy()` (if it implements `DisposableBean`)
10. A custom `destroyMethod`

**Example:**
```java
@Component
public class CacheLoader {

    @PostConstruct
    public void init() {
        System.out.println("Loading cache...");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Clearing cache...");
    }
}
```

**Good points to mention:**
- In Spring Boot 3, these annotations come from `jakarta.annotation` (older versions used `javax.annotation`).
- For **prototype** beans, Spring creates and hands over the bean but **does not manage its destruction**. `@PreDestroy` will not be called. You must clean up yourself.

---

## Q13. Why use `@PostConstruct` instead of doing setup in the constructor?

Because in the constructor, **dependencies may not be injected yet** (with setter or field injection). By the time `@PostConstruct` runs, all injection is complete and it's safe to use them.

With constructor injection, dependencies *are* available in the constructor, but `@PostConstruct` is still a cleaner place for startup work like loading data or opening connections. It also runs **before** any proxy wraps the bean.

---

## Q14. What is a circular dependency? How do you fix it?

**Short answer:** A circular dependency happens when A needs B, and B needs A.

```java
@Service
public class A {
    public A(B b) { }
}

@Service
public class B {
    public B(A a) { }
}
```

To create A, Spring needs B. To create B, Spring needs A. It's a deadlock, like two people each waiting for the other to go through a door first.

**What happens:**
- With **constructor injection**, Spring fails at startup with `BeanCurrentlyInCreationException`. This is good, because you find the problem early.
- Since **Spring Boot 2.6**, circular references are **not allowed by default**, even for setter and field injection.

**How to fix it (best to worst):**
1. **Redesign.** A circular dependency usually means bad design. Move the shared logic into a third class C, and let both A and B depend on C.
2. **Use `@Lazy`** on one injection point. Spring injects a proxy and creates the real bean only when it's first used.
   ```java
   public A(@Lazy B b) { }
   ```
3. Use setter injection, or set `spring.main.allow-circular-references=true`. This is a **last resort** and hides the design problem.

---

## Q15. What does `@Lazy` do?

By default, singleton beans are created **at startup**. `@Lazy` delays creation until the bean is first needed.

```java
@Component
@Lazy
public class HeavyReportGenerator { }
```

**Pros:** Faster startup; saves memory if the bean is rarely used.
**Cons:** If the bean has a config error, you find out late, at runtime. Also, the first call is slower.

You can also make the whole app lazy with `spring.main.lazy-initialization=true`.

---

## Q16. (Advanced) What happens if a singleton bean depends on a prototype bean?

This is a tricky one.

```java
@Component
@Scope("prototype")
public class Task { }

@Component
public class TaskRunner {          // singleton
    private final Task task;

    public TaskRunner(Task task) { // injected only ONCE
        this.task = task;
    }
}
```

Spring creates `TaskRunner` **once**, so it injects **one** `Task` **once**. You expected a new `Task` each time, but you always get the same one. The prototype scope is effectively lost.

**Fixes:**

**1. `ObjectProvider` (recommended):**
```java
@Component
public class TaskRunner {
    private final ObjectProvider<Task> taskProvider;

    public TaskRunner(ObjectProvider<Task> taskProvider) {
        this.taskProvider = taskProvider;
    }

    public void run() {
        Task task = taskProvider.getObject(); // fresh instance every time
    }
}
```

**2. Scoped proxy:**
```java
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
```
Spring injects a proxy, and each method call goes to a new instance.

The same logic applies when injecting a `request` or `session` scoped bean into a singleton. You need a scoped proxy (or `ObjectProvider`).

---

## Q17. (Advanced) What is the difference between `BeanPostProcessor` and `BeanFactoryPostProcessor`?

| | `BeanFactoryPostProcessor` | `BeanPostProcessor` |
|---|---|---|
| Works on | **Bean definitions** (the blueprints) | **Bean instances** (the real objects) |
| Runs | Before any bean is created | Around the init step of each bean |
| Typical use | Change property values, register new definitions | Wrap beans in proxies, process annotations |

**Simple picture:** Think of building houses. `BeanFactoryPostProcessor` edits the **blueprint** before construction. `BeanPostProcessor` inspects or decorates each **finished house**.

**Real examples:**
- `PropertySourcesPlaceholderConfigurer` (a `BeanFactoryPostProcessor`) replaces `${...}` placeholders in bean definitions.
- `AutowiredAnnotationBeanPostProcessor` handles `@Autowired`.
- Proxies for `@Transactional`, `@Async` and AOP are created by `BeanPostProcessor`s.

That is why a lot of Spring's "magic" is really just post-processors working quietly.

---

## Q18. (Advanced) Why does `@Configuration` behave differently from a plain `@Component` with `@Bean` methods?

```java
@Configuration
public class AppConfig {

    @Bean
    public A a() { return new A(b()); }

    @Bean
    public B b() { return new B(); }
}
```

Look at `a()`. It calls `b()` directly. In plain Java, this would create a **new** `B` every time. But in a `@Configuration` class, Spring wraps your class in a **CGLIB proxy**. When you call `b()`, the proxy checks the container first: "Do I already have a `B` bean?" If yes, it returns the same one.

So you always get the **singleton**, even when calling `b()` many times.

**Full mode vs lite mode:**
- `@Configuration` → **full mode**, proxied, inter-bean calls return the singleton.
- `@Component` (or `@Configuration(proxyBeanMethods = false)`) → **lite mode**, no proxy, and `b()` would create a new object on each direct call.

**Why use `proxyBeanMethods = false`?** It skips proxy creation and gives slightly faster startup. It's safe when your `@Bean` methods don't call each other. Spring Boot's own auto-configurations use it for this reason.

---

## Quick Recap

- **IoC** = the framework controls object creation. **DI** = how it hands the objects to you.
- The **container** (`ApplicationContext`) creates, wires and manages **beans**.
- Prefer **constructor injection**: it's safer, testable and supports `final`.
- `@Service` and `@Controller` are for clarity. `@Repository` also translates DB exceptions.
- `@Component` is for your classes. `@Bean` is for third-party classes and custom creation.
- Multiple candidates? Use `@Primary` (default) or `@Qualifier` (exact).
- Default scope is **singleton**, and it's **not thread-safe** unless you keep it stateless.
- Lifecycle: constructor → injection → `@PostConstruct` → ready → `@PreDestroy`.
- Circular dependencies are a design smell. Fix them by redesigning; `@Lazy` is the quick fix.
- Singleton + prototype needs `ObjectProvider` or a scoped proxy.
- `BeanFactoryPostProcessor` edits blueprints. `BeanPostProcessor` decorates objects.

---

Reply **"next"** for **Concept 4: REST APIs and Common Annotations**. That covers `@RestController`, `@RequestMapping`, path variables, `ResponseEntity` and more, which come up in almost every Spring Boot interview. Or tell me if you want any part of this concept explained again.

---
---
---

# Concept 4: REST APIs and Common Annotations

Almost every Spring Boot job builds REST APIs, so this topic is asked in nearly every interview. Start with the basics, then we go into how requests actually flow.

---

## Q1. What is REST? What makes an API "RESTful"?

**Short answer:** REST (Representational State Transfer) is a style for designing web APIs where everything is a **resource**, and you use standard HTTP methods to work on it.

**Deeper explanation:**
A resource is a "thing" like a user, an order or a product. Each has a URL, and the HTTP method tells the server what to do.

```
GET    /users/5      → read user 5
POST   /users        → create a new user
PUT    /users/5      → replace user 5
DELETE /users/5      → delete user 5
```

**Key REST rules:**
- **Client-server:** The client and server are separate.
- **Stateless:** Every request has all the information needed. The server does not remember the previous request. (This is why tokens like JWT are sent with every call.)
- **Uniform interface:** Use standard HTTP methods and status codes.
- **Resource-based URLs:** Use nouns, not verbs. Write `/orders`, not `/getOrders`.
- **Cacheable:** Responses can say whether they can be cached.

**Why stateless matters:** Because the server keeps no session, you can run 10 copies of your app behind a load balancer. Any copy can handle any request.

---

## Q2. What is the difference between `@Controller` and `@RestController`?

| | `@Controller` | `@RestController` |
|---|---|---|
| Returns | A **view name** (like an HTML page) | **Data** (JSON/XML) written directly to the response |
| Needs `@ResponseBody`? | Yes, on each method | No, it's built in |
| Used for | Web pages (Thymeleaf, JSP) | REST APIs |

`@RestController` is simply `@Controller` + `@ResponseBody`.

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);   // converted to JSON automatically
    }
}
```

With a plain `@Controller`, returning `"home"` means "show the `home.html` template." With `@RestController`, returning `"home"` means "send the text `home` as the response body."

---

## Q3. What is `@RequestMapping`? What are its shortcuts?

`@RequestMapping` maps a URL and HTTP method to a Java method or class.

```java
@RequestMapping(value = "/users", method = RequestMethod.GET)
```

That is long, so Spring gives shortcuts:

| Shortcut | Same as |
|---|---|
| `@GetMapping` | `@RequestMapping(method = GET)` |
| `@PostMapping` | `@RequestMapping(method = POST)` |
| `@PutMapping` | `@RequestMapping(method = PUT)` |
| `@PatchMapping` | `@RequestMapping(method = PATCH)` |
| `@DeleteMapping` | `@RequestMapping(method = DELETE)` |

**Common pattern:** Put `@RequestMapping("/users")` on the class as the base path, and use the shortcuts on methods.

You can also narrow the mapping using `consumes`, `produces`, `params` and `headers`:

```java
@PostMapping(value = "/users", consumes = "application/json", produces = "application/json")
```

---

## Q4. What is the difference between `@PathVariable` and `@RequestParam`?

| | `@PathVariable` | `@RequestParam` |
|---|---|---|
| Reads from | The **path** of the URL | The **query string** |
| Example URL | `/users/5` | `/users?role=admin` |
| Best for | Identifying a **specific resource** | **Filtering, sorting, paging, optional inputs** |

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { ... }

@GetMapping("/users")
public List<User> search(@RequestParam(required = false) String role,
                         @RequestParam(defaultValue = "0") int page) { ... }
```

**Useful details:**
- `@RequestParam` is required by default. If it's missing, Spring returns `400 Bad Request`. Use `required = false` or `defaultValue` to change that.
- If the variable name differs from the URL name, write it explicitly: `@PathVariable("id") Long userId`.

**Simple rule:** Use the path for *"which one?"* and query params for *"how do you want it?"*

---

## Q5. What does `@RequestBody` do?

It tells Spring to read the **body** of the HTTP request and convert it (usually from JSON) into a Java object.

```java
@PostMapping("/users")
public User create(@RequestBody User user) {
    return userService.save(user);
}
```

Request:
```json
{ "name": "Asha", "email": "asha@example.com" }
```

Spring uses **Jackson** to turn this JSON into a `User` object. This work is done by classes called **HttpMessageConverters**.

**Related annotation:** `@ResponseBody` does the opposite. It converts the returned Java object into JSON and writes it to the response. (`@RestController` already includes it.)

**Common error:** If the JSON is broken or has wrong types, Spring returns `400 Bad Request` (`HttpMessageNotReadableException`). If you forget `@RequestBody`, the object will not be filled from the JSON.

---

## Q6. What is `ResponseEntity`? Why use it?

**Short answer:** `ResponseEntity` lets you control the **whole HTTP response**: status code, headers and body.

If you just return an object, Spring always sends `200 OK`. But real APIs need more control.

```java
@PostMapping("/users")
public ResponseEntity<User> create(@RequestBody User user) {
    User saved = userService.save(user);
    URI location = URI.create("/users/" + saved.getId());
    return ResponseEntity.created(location).body(saved);   // 201 + Location header
}

@GetMapping("/users/{id}")
public ResponseEntity<User> get(@PathVariable Long id) {
    return userService.find(id)
            .map(ResponseEntity::ok)                       // 200
            .orElse(ResponseEntity.notFound().build());    // 404
}
```

**Alternative for simple cases:** `@ResponseStatus(HttpStatus.CREATED)` on a method sets a fixed status code. But `ResponseEntity` is better when the status depends on the situation.

---

## Q7. Which HTTP status codes should you know?

| Code | Meaning | When to use |
|---|---|---|
| **200 OK** | Success | Normal GET, PUT |
| **201 Created** | New resource created | After POST |
| **204 No Content** | Success, nothing to return | After DELETE |
| **400 Bad Request** | Client sent bad data | Validation errors, broken JSON |
| **401 Unauthorized** | Not logged in / invalid token | Authentication failed |
| **403 Forbidden** | Logged in but not allowed | Authorization failed |
| **404 Not Found** | Resource doesn't exist | Wrong ID |
| **409 Conflict** | Conflicts with current state | Duplicate email |
| **500 Internal Server Error** | Server bug | Unexpected exception |

**Interview tip:** Many people mix up 401 and 403. Remember: **401 = "Who are you?"**, **403 = "I know who you are, but you can't do this."**

---

## Q8. What is the difference between PUT and PATCH? What does "idempotent" mean?

| | PUT | PATCH |
|---|---|---|
| Purpose | **Replace** the whole resource | **Update part** of the resource |
| You send | The complete object | Only changed fields |

**Idempotent** means: calling the same request many times gives the **same result** as calling it once.

| Method | Idempotent? | Safe (no change)? |
|---|---|---|
| GET | Yes | Yes |
| PUT | Yes | No |
| DELETE | Yes | No |
| POST | **No** | No |
| PATCH | Not guaranteed | No |

**Example:** `PUT /users/5` with the same data ten times leaves user 5 in the same state. But `POST /orders` ten times creates ten orders. That is why retrying a POST can be dangerous, and why payment systems use **idempotency keys**.

---

## Q9. What is `DispatcherServlet`? How does a request flow in Spring MVC?

**Short answer:** `DispatcherServlet` is the **front door** of Spring MVC. Every request enters through it, and it sends the request to the right controller.

**The flow:**
1. Request arrives at the embedded server (Tomcat).
2. It goes through **filters** first (like security filters).
3. `DispatcherServlet` receives it.
4. **`HandlerMapping`** finds which controller method matches the URL and HTTP method.
5. **Interceptors** run their `preHandle`.
6. **`HandlerAdapter`** calls the controller method. Argument resolvers fill in `@PathVariable`, `@RequestBody` and so on.
7. The controller returns a result.
8. For REST, **HttpMessageConverter** converts the object to JSON. (For web pages, a `ViewResolver` picks the template.)
9. Response goes back to the client.

**Picture it:** `DispatcherServlet` is like a receptionist. It reads your request, checks a directory (HandlerMapping), sends you to the correct desk (controller), and then returns the reply to you.

Spring Boot auto-configures `DispatcherServlet` and maps it to `/`, so you never set it up yourself.

---

## Q10. What is the difference between a Filter and an Interceptor?

| | Filter | Interceptor |
|---|---|---|
| Belongs to | **Servlet** specification (Jakarta EE) | **Spring MVC** |
| Runs | **Before** `DispatcherServlet` | **After** `DispatcherServlet`, around the controller |
| Knows about controllers? | No | Yes (it knows which handler will run) |
| Typical use | Security, logging raw requests, CORS, compression | Auth checks per controller, timing, audit logs |
| Hooks | `doFilter` | `preHandle`, `postHandle`, `afterCompletion` |

**Order:** Filter → DispatcherServlet → Interceptor → Controller → Interceptor → Filter.

**Rule of thumb:** If you need to touch the raw request/response, use a filter. If you need Spring-specific info like the handler method, use an interceptor. Spring Security itself is built as a chain of filters.

---

## Q11. How does Spring Boot convert Java objects to JSON?

Spring Boot includes **Jackson** by default (through `spring-boot-starter-web`). It provides an `ObjectMapper` bean that does the conversion.

**Common Jackson annotations:**

```java
public class User {
    private Long id;

    @JsonProperty("full_name")      // rename in JSON
    private String name;

    @JsonIgnore                     // never include in JSON
    private String password;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
```

**Useful properties:**
```properties
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false
```

**How does Spring choose JSON vs XML? (Content negotiation)**
It looks at the request's `Accept` header. If the client says `Accept: application/json`, you get JSON. To support XML, add `jackson-dataformat-xml` to the classpath.

---

## Q12. Why should you use DTOs instead of returning Entities directly?

An **Entity** maps to a database table. A **DTO** (Data Transfer Object) is a simple class made just for the API.

If you return entities directly, you can run into these problems:

- **Leaking data:** Fields like `password` or internal flags may go out by mistake.
- **Tight coupling:** A database change breaks your API contract.
- **Lazy loading errors:** JPA relationships can throw `LazyInitializationException` or cause **infinite loops** in JSON (User → Orders → User → ...).
- **Over-posting:** With `@RequestBody`, a client could send fields like `role: ADMIN` and set values you never meant to allow.

```java
public record UserResponse(Long id, String name, String email) { }

@GetMapping("/{id}")
public UserResponse get(@PathVariable Long id) {
    User u = userService.findById(id);
    return new UserResponse(u.getId(), u.getName(), u.getEmail());
}
```

Java **records** are great for DTOs, since they are short and immutable. Libraries like **MapStruct** can do the entity-to-DTO mapping for you.

---

## Q13. How do you read request headers and cookies?

```java
@GetMapping("/info")
public String info(@RequestHeader("User-Agent") String agent,
                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                   @CookieValue(value = "sessionId", required = false) String session) {
    return agent;
}
```

To read **all** headers, use `@RequestHeader Map<String, String> headers`.

---

## Q14. What is CORS? How do you enable it in Spring Boot?

**Short answer:** CORS (Cross-Origin Resource Sharing) is a browser security rule. By default, a web page from `site-a.com` cannot call an API on `site-b.com`, unless `site-b.com` says it's allowed.

**Important:** CORS is enforced by the **browser**, not the server. Tools like Postman ignore it. That's why "it works in Postman but not in my React app" is a very common problem.

**Option 1: On a controller or method**
```java
@CrossOrigin(origins = "https://myfrontend.com")
@RestController
public class UserController { }
```

**Option 2: Globally (better)**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://myfrontend.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

**With Spring Security:** You must also enable CORS in the security config, or the security filters may block the browser's **preflight** (`OPTIONS`) request before your CORS settings apply. Avoid `allowedOrigins("*")` together with credentials in production.

---

## Q15. How do you implement pagination and sorting?

Spring Data gives you `Pageable` out of the box:

```java
@GetMapping("/users")
public Page<UserResponse> list(Pageable pageable) {
    return userRepository.findAll(pageable).map(this::toDto);
}
```

Call it like: `GET /users?page=0&size=10&sort=name,asc`

**Why paginate?** Returning 100,000 rows in one call kills memory and speed. Always cap the size.

**Advanced note:** Page-based pagination gets slow for very large tables because the database still counts past all the skipped rows. For big data or infinite scroll, **keyset (cursor) pagination** is faster, where you say "give me 10 rows after ID 5000."

---

## Q16. What are the ways to version a REST API?

| Method | Example | Comment |
|---|---|---|
| **URI versioning** | `/api/v1/users` | Most common, simple and clear |
| **Header versioning** | `X-API-Version: 2` | Cleaner URLs, harder to test in a browser |
| **Media type versioning** | `Accept: application/vnd.myapp.v2+json` | Very "pure" REST, more complex |
| **Query parameter** | `/users?version=2` | Easy but a bit messy |

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 { }
```

Most teams pick **URI versioning** because it is easy to see, document and route. Versioning matters because breaking changes should never surprise existing clients.

---

## Q17. How do you call another REST API from Spring Boot?

You have three main options:

| Tool | Style | Status |
|---|---|---|
| `RestTemplate` | Blocking (synchronous) | In **maintenance mode**, not recommended for new code |
| `RestClient` | Blocking, modern fluent API | **Recommended** for blocking calls (Spring Framework 6.1 / Boot 3.2+) |
| `WebClient` | Non-blocking (reactive) | Best for reactive apps or many parallel calls |

**`RestClient` example:**
```java
RestClient client = RestClient.create("https://api.example.com");

User user = client.get()
        .uri("/users/{id}", 5)
        .retrieve()
        .body(User.class);
```

**Also consider:** Declarative HTTP clients (`@HttpExchange`) or **OpenFeign** in microservices. And always set **timeouts**. A call with no timeout can hang your threads forever.

---

## Q18. (Advanced) How do you handle file upload and download?

**Upload:**
```java
@PostMapping("/upload")
public String upload(@RequestParam("file") MultipartFile file) throws IOException {
    Files.copy(file.getInputStream(), Path.of("uploads", file.getOriginalFilename()));
    return "Uploaded";
}
```

Limit the size in properties:
```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB
```

**Safety tips:** Never trust `getOriginalFilename()`. It can contain `../` (path traversal). Generate your own file name, and check file type and size.

**Download:**
```java
@GetMapping("/download/{name}")
public ResponseEntity<Resource> download(@PathVariable String name) {
    Resource file = new FileSystemResource("uploads/" + name);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
            .body(file);
}
```
(Again, validate `name` in real code to block path traversal.)

---

## Q19. (Advanced) How do you make a REST endpoint asynchronous?

By default, one request holds one server thread until it finishes. For slow operations, you can free the thread:

```java
@GetMapping("/report")
public CompletableFuture<Report> report() {
    return CompletableFuture.supplyAsync(() -> reportService.generate());
}
```

Spring MVC will release the Tomcat thread and send the response when the future completes.

**Modern note:** With **Java 21 virtual threads**, you can simply set `spring.threads.virtual.enabled=true` (Boot 3.2+). Each request then runs on a lightweight thread, so even plain blocking code scales much better, without rewriting to reactive style.

---

## Quick Recap

- **REST** = resources + HTTP methods + stateless. Use nouns in URLs.
- `@RestController` = `@Controller` + `@ResponseBody`.
- `@PathVariable` = identify the resource. `@RequestParam` = filters and options.
- `@RequestBody` reads JSON into an object. Jackson does the conversion.
- `ResponseEntity` gives full control over status, headers and body.
- Know your codes: 200, 201, 204, 400, 401, 403, 404, 409, 500.
- **PUT** replaces, **PATCH** updates part. GET, PUT and DELETE are idempotent. POST is not.
- `DispatcherServlet` is the front controller. Flow: Filter → DispatcherServlet → Interceptor → Controller.
- Use **DTOs** to avoid leaking data and lazy-loading problems.
- **CORS** is a browser rule. Configure it globally, and remember it with Spring Security.
- Use `Pageable` for pagination. Version APIs, most commonly with `/v1/`.
- Prefer `RestClient` over `RestTemplate` for new code, and always set timeouts.

---

Reply **"next"** for **Concept 5: Configuration, Properties and Profiles**. That covers `application.properties` vs YAML, `@Value` vs `@ConfigurationProperties`, profiles, externalized config and secrets, which are asked a lot in real-world and DevOps-flavored interviews. Or tell me if you want any part of this concept explained again.

---
---
---

# Concept 5: Configuration, Properties and Profiles

Every real app runs in more than one place: your laptop, a test server, production. Each place needs different settings (database URL, ports, passwords). This concept is about how Spring Boot handles that cleanly.

---

## Q1. What is the difference between `application.properties` and `application.yml`?

Both do the same job: they hold your app's settings. Only the format is different.

**Properties format:**
```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
```

**YAML format:**
```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
```

| | `.properties` | `.yml` |
|---|---|---|
| Style | Flat, one line per key | Hierarchical, uses indentation |
| Repeated prefixes | Repeated on every line | Written once |
| Lists | `my.list[0]=a` (clumsy) | Clean `- a` style |
| Multiple profiles in one file | Not possible | Possible, using `---` |
| Risk | Very few | Wrong indentation breaks things |

**Things worth knowing:**
- If both files exist in the same place, **`.properties` wins** over `.yml`.
- YAML can surprise you. Values like `no`, `off` or `yes` can be read as booleans. A country code `NO` (Norway) may become `false`. **Quote strings** when in doubt: `"NO"`.
- `@PropertySource` cannot load YAML files. It only works with `.properties`.

Neither is "better." Pick one and stay consistent in the team.

---

## Q2. Where does Spring Boot look for configuration files?

By default, it checks these locations, in this order. **Later ones override earlier ones.**

1. Inside the JAR: the classpath root (`src/main/resources`)
2. Inside the JAR: a `/config` folder on the classpath
3. **Outside the JAR:** the current directory (where you run the app)
4. **Outside the JAR:** a `/config` folder in the current directory

**Why this is useful:** You build the JAR once with default settings inside. On the server, you drop an `application.properties` next to the JAR, and it overrides only the values you put in it. No rebuild needed.

You can also point to a custom location:
```
java -jar app.jar --spring.config.additional-location=file:/etc/myapp/
```

Use `additional-location` to **add** to the defaults. Using `spring.config.location` **replaces** them.

---

## Q3. What is externalized configuration? What is the order of precedence?

**Short answer:** Externalized configuration means keeping settings **outside your code**, so the same build can run in any environment.

Spring Boot reads settings from many sources. When the same key appears in more than one, the **higher-priority source wins**. From lowest to highest, the most useful ones are:

1. Default properties set in code
2. `@PropertySource` files
3. `application.properties` / `application.yml` (and profile-specific files)
4. **OS environment variables**
5. **Java system properties** (`-Dserver.port=9000`)
6. `SPRING_APPLICATION_JSON` (JSON in an environment variable)
7. **Command-line arguments** (`--server.port=9000`)
8. Test annotations like `@TestPropertySource`

**Easy way to remember:** The closer a setting is to the "moment of running the app," the stronger it is. Command-line arguments beat environment variables, which beat files.

**Example:**
```properties
# application.properties
server.port=8080
```
```
java -jar app.jar --server.port=9000
```
The app starts on **9000**, because the command line wins.

---

## Q4. How do environment variables map to Spring properties?

Environment variables cannot contain dots or dashes in most systems (like Linux shells). So Spring Boot has a simple conversion rule:

- Replace `.` with `_`
- Remove `-` (or turn it into `_`)
- Make it **UPPERCASE**

| Property | Environment variable |
|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
| `server.port` | `SERVER_PORT` |
| `app.mail.host` | `APP_MAIL_HOST` |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` |

This is exactly how Docker and Kubernetes usually configure Spring Boot apps:

```
docker run -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/shop myapp
```

No code change, no file change. The environment variable simply overrides the file.

---

## Q5. How does `@Value` work?

`@Value` injects a single property value into a field or constructor parameter.

```java
@Service
public class MailService {

    private final String host;
    private final int timeout;

    public MailService(@Value("${app.mail.host}") String host,
                       @Value("${app.mail.timeout:30}") int timeout) {
        this.host = host;
        this.timeout = timeout;
    }
}
```

**Key points:**
- `${key}` reads a property. If it's missing, the app **fails at startup**.
- `${key:default}` gives a **default value** after the colon. Here, `30` is used if `app.mail.timeout` isn't set.
- Spring converts the text to the right type (`int`, `boolean`, `List`, etc.).

**Two look-alike syntaxes, often confused:**

| Syntax | Name | Meaning |
|---|---|---|
| `${...}` | Property placeholder | Read a value from configuration |
| `#{...}` | SpEL (Spring Expression Language) | Evaluate an expression |

```java
@Value("#{2 * 30}")                       // 60
@Value("#{'${app.roles}'.split(',')}")    // turn "A,B,C" into a list
```

`@Value` is fine for one or two values. When you have many related settings, there is a better tool. Read on.

---

## Q6. What is `@ConfigurationProperties`? Why is it better than `@Value`?

**Short answer:** It binds a **whole group** of properties (with the same prefix) into one typed Java object.

**Properties:**
```properties
app.mail.host=smtp.example.com
app.mail.port=587
app.mail.timeout=5s
app.mail.admins=a@x.com,b@x.com
```

**Java class (using a record):**
```java
@ConfigurationProperties(prefix = "app.mail")
@Validated
public record MailProperties(
        @NotBlank String host,
        @Min(1) int port,
        Duration timeout,
        List<String> admins) { }
```

**Register it** in one of two ways:
```java
@SpringBootApplication
@ConfigurationPropertiesScan          // finds all @ConfigurationProperties classes
public class DemoApplication { }
// or: @EnableConfigurationProperties(MailProperties.class)
```

**Use it like any bean:**
```java
@Service
public class MailService {
    private final MailProperties props;
    public MailService(MailProperties props) { this.props = props; }
}
```

**Why it's better:**

| | `@Value` | `@ConfigurationProperties` |
|---|---|---|
| Groups many values | No, one at a time | Yes, one object |
| Type-safe | Partly | Fully |
| Validation (`@NotBlank`, `@Min`) | Not really | Yes, with `@Validated` |
| Relaxed binding | Limited | Full |
| Nested objects and lists | Awkward | Natural |
| IDE auto-complete | No | Yes (with the configuration processor) |
| Fails early on bad config | Sometimes | Yes, at startup |

**Rule of thumb:** One simple value → `@Value`. A group of settings → `@ConfigurationProperties`. Most teams prefer it for anything beyond a quick value.

**Good details:**
- `Duration` understands `5s`, `500ms`, `2m`. `DataSize` understands `10MB`.
- In **Spring Boot 3**, a class or record with a single constructor is bound through the constructor automatically, so `@ConstructorBinding` is not needed.
- `@Validated` needs `spring-boot-starter-validation`.
- Add `spring-boot-configuration-processor` to get IDE hints and metadata for your own properties.

---

## Q7. What is relaxed binding?

With `@ConfigurationProperties`, the same property can be written in different styles, and Spring Boot treats them as the same:

```
app.mail.smtp-host      ← kebab-case (recommended in files)
app.mail.smtpHost       ← camelCase
app.mail.smtp_host      ← underscore
APP_MAIL_SMTPHOST       ← environment variable style
```

All of these bind to the Java field `smtpHost`.

**Why it exists:** Different places have different naming limits. Files like kebab-case, environment variables need uppercase and underscores. Relaxed binding lets one Java field work with all of them.

**Catch:** `@Value` is **not** relaxed. It matches the key exactly. If you use `@Value`, write the key in the canonical **lowercase kebab-case** form (`app.smtp-host`), and this will work with environment variables too.

---

## Q8. What are Profiles? Why do we need them?

**Short answer:** A profile is a **named set of settings and beans** that you turn on for a specific environment, like `dev`, `test` or `prod`.

**Why:** The dev database is H2 in memory. The production database is MySQL with a strong password. You do not want to edit files before every deploy. Profiles let you keep both and switch with one setting.

**Profile-specific files:**
```
application.properties          ← always loaded (common settings)
application-dev.properties      ← loaded only when "dev" is active
application-prod.properties     ← loaded only when "prod" is active
```

`application-dev.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
logging.level.root=DEBUG
```

`application-prod.properties`:
```properties
spring.datasource.url=jdbc:mysql://prod-db:3306/shop
logging.level.root=WARN
```

**How Spring merges them:** The base file loads first. Then the active profile file loads on top and **overrides** matching keys. Keys that aren't repeated stay as they were.

---

## Q9. How do you activate a profile?

Several ways, and they follow the same precedence rules as Q3:

```properties
# 1. In application.properties (good for a default, like dev)
spring.profiles.active=dev
```
```bash
# 2. Command line
java -jar app.jar --spring.profiles.active=prod

# 3. Environment variable (most common in Docker/Kubernetes)
export SPRING_PROFILES_ACTIVE=prod

# 4. JVM system property
java -Dspring.profiles.active=prod -jar app.jar
```

**Good practice:** Do **not** hard-code `spring.profiles.active=prod` inside the JAR. Set the active profile from **outside** (environment variable), so the same JAR runs anywhere.

If no profile is set, Spring uses a profile named **`default`**.

You can activate more than one: `--spring.profiles.active=prod,metrics`.

---

## Q10. What does `@Profile` do?

It makes a **bean or configuration class** load only when a certain profile is active.

```java
@Configuration
public class DataConfig {

    @Bean
    @Profile("dev")
    public DataSource h2DataSource() { ... }

    @Bean
    @Profile("prod")
    public DataSource mysqlDataSource() { ... }
}
```

**Useful forms:**
```java
@Profile("!prod")            // everything except prod
@Profile({"dev", "test"})    // dev OR test
@Profile("prod & cloud")     // prod AND cloud (expression, Spring 5.1+)
```

**Common use:** A fake `EmailService` that only prints to the console in `dev`, and a real one that sends emails in `prod`.

**Tip:** Do not overuse `@Profile` for business logic. If code behaves very differently per environment, tests can miss real bugs. Keep profile differences to **infrastructure** (databases, external services, logging).

---

## Q11. Can I keep all profiles in one YAML file? What are profile groups?

**Yes, with multi-document YAML.** Separate sections with `---`:

```yaml
server:
  port: 8080

---
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 8081

---
spring:
  config:
    activate:
      on-profile: prod
server:
  port: 80
```

Each section after `---` applies only if its profile is active. (Older versions used `spring.profiles`; since Boot 2.4 it's `spring.config.activate.on-profile`.)

**Profile groups** let one profile switch on several others:

```properties
spring.profiles.group.prod=proddb,prodmq,metrics
```

Now `--spring.profiles.active=prod` activates all four. This keeps your deploy command simple, and the details stay in config.

---

## Q12. What is `Environment`? How do I read properties in code?

`Environment` is a Spring object that gives you access to all properties and profiles.

```java
@Service
public class InfoService {

    private final Environment env;

    public InfoService(Environment env) { this.env = env; }

    public void print() {
        String port = env.getProperty("server.port", "8080");   // with default
        String[] profiles = env.getActiveProfiles();
    }
}
```

Behind the scenes, `Environment` holds a list of **PropertySources** (files, environment variables, system properties, command-line args), checked in priority order. That's how the precedence in Q3 actually works.

Most of the time `@Value` or `@ConfigurationProperties` is nicer. Use `Environment` when the key is decided at runtime.

---

## Q13. How do you handle secrets like passwords and API keys?

**The rule: Never put real secrets in Git.** Not in `application.properties`, not even in a "private" repo.

**Better options, from simple to strong:**

1. **Environment variables**
   ```
   spring.datasource.password=${DB_PASSWORD}
   ```
   Simple and widely used. Set the variable in Docker, Kubernetes or your CI/CD tool.

2. **Kubernetes Secrets mounted as files**
   ```properties
   spring.config.import=optional:configtree:/etc/secrets/
   ```
   Each file in that folder becomes a property (file name = key, file content = value). Safer than environment variables, since they don't show up in process listings.

3. **A secrets manager**: HashiCorp Vault (Spring Cloud Vault), AWS Secrets Manager, Azure Key Vault, GCP Secret Manager. Best for production: central control, rotation, audit logs.

4. **Encrypted properties** (like Jasypt or Spring Cloud Config encryption). This works, but you still have to protect the decryption key somewhere.

**Also watch out for leaks:**
- The Actuator `/env` endpoint and `/configprops` can reveal values. In Boot 3, values are masked by default, but only expose these endpoints to trusted users.
- Do not log configuration objects that contain passwords.

---

## Q14. What is `spring.config.import`?

Since Spring Boot 2.4, this property lets you **pull in extra configuration** from other places, right from your config file.

```properties
# import another file
spring.config.import=optional:file:./extra.properties

# import from a config server
spring.config.import=optional:configserver:http://config-server:8888

# import secrets folder
spring.config.import=optional:configtree:/etc/secrets/
```

**The `optional:` prefix** means: "if it's not there, don't fail." Without it, a missing file stops the app at startup.

This replaced the old `bootstrap.properties` approach for Spring Cloud.

---

## Q15. How do you load a custom properties file?

Use `@PropertySource`:

```java
@Configuration
@PropertySource("classpath:payment.properties")
public class PaymentConfig { }
```

**Limits to remember:**
- It does **not** support YAML.
- It does not support profile-specific loading by itself.
- Its priority is quite **low**, so `application.properties`, environment variables and command-line args can override it.

For most cases today, a simpler path is to put everything in `application.yml` and use `spring.config.import` when you need extra files.

---

## Q16. (Advanced) How do you change configuration without restarting the app?

By default, Spring Boot reads configuration **once at startup**. Changing a file later does nothing.

In a microservices setup, **Spring Cloud Config** helps:

1. A **Config Server** stores configuration, usually in a **Git repo**.
2. Each service pulls its settings from the server at startup.
3. To update at runtime, mark beans with `@RefreshScope` and call `POST /actuator/refresh`. Those beans are recreated with new values. (For many instances, **Spring Cloud Bus** can broadcast the refresh.)

```java
@RestController
@RefreshScope
public class FeatureController {
    @Value("${feature.newCheckout:false}")
    private boolean newCheckout;
}
```

**Honest caveat:** `@RefreshScope` refreshes only those beans. Things like the `DataSource` or thread pools usually need a restart. In Kubernetes, many teams simply update a ConfigMap and do a rolling restart. It's simpler and more predictable.

---

## Q17. (Advanced) How do you configure properties in tests?

Some quick options (we'll cover testing fully in Concept 10):

```java
@SpringBootTest(properties = "app.mail.host=test-host")
class MailTest { }

@TestPropertySource(properties = "app.feature.enabled=true")
class FeatureTest { }
```

For values that are only known at runtime, like a random port from a **Testcontainers** database, use `@DynamicPropertySource`:

```java
@DynamicPropertySource
static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
}
```

Test properties have **very high priority**, so they safely override your normal config.

---

## Q18. (Advanced) What common mistakes do people make with configuration?

- **Committing secrets** to Git. Rotate them immediately if this happens.
- **Hard-coding the active profile** in the JAR, then accidentally running dev settings in production.
- **Using `@Value` everywhere** for the same prefix instead of one `@ConfigurationProperties` class.
- **Forgetting defaults** for optional values, so the app crashes on startup in one environment.
- **Too many profiles.** If you have `dev`, `dev2`, `qa`, `qa-eu`, `staging`, and so on, nobody knows what runs where. Prefer few profiles, and vary the details through environment variables.
- **YAML indentation errors** and unquoted values like `no` or `on`.
- **Assuming `spring.profiles.active` inside a profile-specific file works.** It doesn't. Profile activation must come from a non-profile-specific source.
- **Config differences hiding bugs.** Keep dev and prod as similar as possible, so you don't find surprises only in production.

---

## Quick Recap

- **`.properties` vs `.yml`:** same job, different format. YAML is cleaner but sensitive to indentation.
- Config is loaded from inside the JAR first, then outside, and outside wins.
- **Precedence (weak → strong):** files → environment variables → system properties → command-line args.
- Env variable names: uppercase, dots become underscores (`SPRING_DATASOURCE_URL`).
- **`@Value`** is for a single value (`${key:default}`). **`@ConfigurationProperties`** is for a group: typed, validated, relaxed binding.
- `${...}` reads properties. `#{...}` is SpEL.
- **Profiles** switch settings per environment: `application-{profile}.properties`, `@Profile`, and `spring.profiles.active`.
- Set the active profile **from outside** (environment variable), not inside the JAR.
- **Profile groups** activate several profiles together.
- **Secrets:** never in Git. Use environment variables, mounted files, or a secrets manager.
- `spring.config.import` pulls in extra config sources. Use `optional:` to avoid failing when missing.
- Runtime refresh: Spring Cloud Config with `@RefreshScope`. Often a rolling restart is simpler.

---

Reply **"next"** for **Concept 6: Spring Data JPA**. That covers entities, repositories, derived queries, the N+1 problem, lazy vs eager loading, `@Transactional` basics and more. It's one of the most asked topics in real interviews. Or tell me if you want any part of this concept explained again.

----
----
----

# Concept 6: Spring Data JPA

Almost every backend app talks to a database. This topic shows up in nearly every interview, and the tricky questions (N+1, lazy loading, transactions) separate people who have *used* JPA from people who *understand* it.

---

## Q1. What is JPA? What are Hibernate and Spring Data JPA?

These three are often mixed up. They sit on top of each other:

| Layer | What it is |
|---|---|
| **JPA** (Jakarta Persistence API) | A **specification**. Just interfaces and rules. It says *how* Java objects should map to tables. It has no working code. |
| **Hibernate** | An **implementation** of JPA. It does the real work: generates SQL, manages entities. |
| **Spring Data JPA** | A **layer on top** that removes boilerplate. You write an interface, and it creates the repository for you. |

**Simple picture:** JPA is the rulebook. Hibernate is the player who follows the rules. Spring Data JPA is the assistant who does the paperwork so you don't have to.

When you add `spring-boot-starter-data-jpa`, you get all three, with Hibernate as the default provider.

**ORM (Object-Relational Mapping)** is the general idea: a Java class maps to a table, an object maps to a row, and a field maps to a column. You work with objects, and the framework writes the SQL.

---

## Q2. How do you create an Entity?

An entity is a Java class that maps to a database table.

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true)
    private String email;

    protected User() { }   // JPA needs a no-arg constructor

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // getters
}
```

**Rules for entities:**
- Annotate with `@Entity` and have an `@Id` field.
- Have a **no-argument constructor** (it can be `protected`).
- The class should not be `final`, because Hibernate creates proxies by extending it.
- In Boot 3, the imports are `jakarta.persistence.*` (older: `javax.persistence.*`).

**ID generation strategies:**

| Strategy | How it works |
|---|---|
| `IDENTITY` | The database auto-increments the ID (MySQL, PostgreSQL). Simple, but **disables JDBC batch inserts**. |
| `SEQUENCE` | Uses a database sequence. **Best for PostgreSQL/Oracle**, and allows batching. |
| `AUTO` | Hibernate picks one. |
| `UUID` | Uses a random unique ID. |

---

## Q3. What is a Repository? What is the difference between `CrudRepository`, `JpaRepository` and `PagingAndSortingRepository`?

You just write an interface. Spring creates the implementation at startup:

```java
public interface UserRepository extends JpaRepository<User, Long> { }
```

That single line gives you `save`, `findById`, `findAll`, `deleteById`, `count` and more.

| Interface | What it adds |
|---|---|
| `CrudRepository` | Basic create, read, update, delete |
| `PagingAndSortingRepository` | Pagination and sorting |
| `JpaRepository` | Extends both, plus JPA extras like `flush()` and `saveAllAndFlush()` |

**In Spring Data 3.x**, `PagingAndSortingRepository` no longer extends `CrudRepository`. `JpaRepository` still includes both, so most people just use `JpaRepository`.

**How does an interface work without code?** Spring creates a **dynamic proxy** at startup. The proxy implements your interface, using `SimpleJpaRepository` under the hood. `@Repository` is not even needed.

---

## Q4. What are derived query methods?

You write a method name, and Spring builds the query from the name.

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    List<User> findByAgeGreaterThanAndActiveTrue(int age);

    boolean existsByEmail(String email);

    long countByActiveTrue();

    List<User> findTop5ByOrderByCreatedAtDesc();
}
```

**How to read them:** `findBy` + field + keyword. Keywords include `And`, `Or`, `Between`, `LessThan`, `Like`, `Containing`, `In`, `IsNull`, `OrderBy` and more.

**Trap:** A typo in a field name (`findByEmial`) makes the app **fail at startup**. That is good, because you find it early.

**Drawback:** Long names get ugly (`findByFirstNameAndLastNameAndCityAndActiveTrueOrderByCreatedAtDesc`). When it gets long, switch to `@Query`.

---

## Q5. How do you write custom queries with `@Query`?

```java
// JPQL (works on entities, not tables)
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmailJpql(@Param("email") String email);

// Native SQL
@Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
Optional<User> findByEmailNative(@Param("email") String email);
```

**JPQL vs native SQL:**
- **JPQL** uses class and field names (`User`, `u.email`). It's database independent.
- **Native SQL** uses table and column names. It's more powerful, but tied to one database.

**Update and delete queries** need `@Modifying`:
```java
@Modifying
@Query("UPDATE User u SET u.active = false WHERE u.lastLogin < :date")
int deactivateOld(@Param("date") LocalDate date);
```

Use **named parameters** (`:email`), not positional (`?1`). They are clearer and less error-prone. Always use parameters, never string concatenation, to avoid **SQL injection**.

---

## Q6. What is the difference between `save()`, `saveAndFlush()` and `flush()`?

- **`save()`** inserts a new entity or updates an existing one. It may **not** hit the database right away.
- **`flush()`** forces pending changes to be written to the database now (still inside the transaction, not committed).
- **`saveAndFlush()`** does both.

**How does `save()` know insert or update?** If the entity is **new** (ID is null, or `@Version` is null), it calls `persist`. Otherwise it calls `merge`, which first does a SELECT to load the row. That is an extra query many people don't expect.

---

## Q7. What is the Persistence Context? What are the entity states?

**Short answer:** The Persistence Context (also called the **first-level cache**) is a workspace where Hibernate keeps track of the entities you loaded or saved in a transaction. The `EntityManager` manages it.

**Entity states:**

| State | Meaning |
|---|---|
| **Transient** | New object made with `new`. Hibernate doesn't know it. |
| **Managed (Persistent)** | Tracked by the persistence context. Changes are watched. |
| **Detached** | Was managed, but the session/transaction ended. No longer tracked. |
| **Removed** | Marked for deletion. |

**Important behaviors:**

1. **Same ID = same object.** Loading user 5 twice in one transaction runs only **one** SQL query. The second call returns the cached object.

2. **Dirty checking.** Hibernate remembers the original state of managed entities. At commit, it compares and **automatically writes an UPDATE** for anything that changed.

```java
@Transactional
public void rename(Long id) {
    User u = userRepository.findById(id).orElseThrow();
    u.setName("New Name");     // no save() call needed!
}                              // UPDATE runs automatically at commit
```

Many beginners are surprised that this works with no `save()`. Now you know why.

---

## Q8. What are relationships in JPA? How do you map them?

| Annotation | Example |
|---|---|
| `@OneToOne` | User ↔ Profile |
| `@OneToMany` / `@ManyToOne` | Customer → Orders |
| `@ManyToMany` | Students ↔ Courses |

```java
@Entity
public class Customer {
    @Id @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

**Key ideas:**

- **Owning side:** The side with the foreign key (`Order` here, with `@JoinColumn`). It decides what gets saved.
- **`mappedBy`:** Placed on the **inverse side** (`Customer`). It says "the other class owns this relationship."
- **`cascade`:** Passes operations to children. `CascadeType.ALL` means saving a customer also saves its orders.
- **`orphanRemoval = true`:** If you remove an order from the list, it's deleted from the database.

**Bidirectional tip:** Keep both sides in sync with a helper method:
```java
public void addOrder(Order o) {
    orders.add(o);
    o.setCustomer(this);
}
```

**Best practices:**
- Prefer `@ManyToOne` and keep it **lazy**.
- Avoid `@ManyToMany` if you can. Use a join entity instead, so you can add extra columns later.
- Don't use `CascadeType.ALL` or `REMOVE` on `@ManyToMany`. It can delete shared data.

---

## Q9. What is the difference between Lazy and Eager loading?

| | LAZY | EAGER |
|---|---|---|
| Related data loaded | **Only when you access it** | **Immediately** with the parent |
| Query cost | Small first, extra queries later | One big load, even if unused |

**Defaults (important, often asked):**

| Relationship | Default |
|---|---|
| `@ManyToOne`, `@OneToOne` | **EAGER** |
| `@OneToMany`, `@ManyToMany` | **LAZY** |

**Best practice:** Make **everything LAZY**, then load what you need per query. Eager loading is a trap. It can pull huge chains of data you never asked for, and you can't turn it off later for a specific query.

```java
@ManyToOne(fetch = FetchType.LAZY)   // override the EAGER default
```

---

## Q10. What is `LazyInitializationException`? How do you fix it?

**When it happens:** You load an entity, the transaction ends, and later you touch a lazy field. Hibernate can't fetch it, because the session is closed.

```java
User u = userService.findById(5L);      // transaction ended here
u.getOrders().size();                    // 💥 LazyInitializationException
```

**Fixes (good to weak):**

1. **Fetch what you need in the query:** `JOIN FETCH` or `@EntityGraph`.
2. **Use a DTO projection.** Select only the data the API needs.
3. Access the data **inside** the `@Transactional` method and map to a DTO there.

**What about Open Session In View (OSIV)?**
Spring Boot enables `spring.jpa.open-in-view=true` by default. It keeps the session open during the whole web request, so lazy loading works in controllers. That hides the error, but it has costs: database connections stay held longer, and hidden queries run while rendering JSON. Many teams **turn it off** (`spring.jpa.open-in-view=false`) and fetch data properly in the service layer. Boot even logs a warning about it at startup.

---

## Q11. What is the N+1 problem? How do you solve it?

**This is the most asked JPA question.** Know it well.

**The problem:** You load **1** list of parents, then for each parent, Hibernate runs **1 extra query** to load its children.

```java
List<Customer> customers = customerRepository.findAll();   // 1 query
for (Customer c : customers) {
    System.out.println(c.getOrders().size());              // N queries (one per customer)
}
```

With 100 customers, that's **101 queries**. With 10,000, it's 10,001. The app is slow, and nobody sees why in the code.

**How to spot it:** Turn on SQL logging.
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```
Seeing the same SELECT repeated many times is the sign.

**Solutions:**

**1. `JOIN FETCH` (load everything in one query):**
```java
@Query("SELECT c FROM Customer c JOIN FETCH c.orders")
List<Customer> findAllWithOrders();
```

**2. `@EntityGraph` (same idea, cleaner):**
```java
@EntityGraph(attributePaths = "orders")
List<Customer> findAll();
```

**3. Batch fetching:** Load children for many parents at once using `IN (...)`.
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```
Now 101 queries become about 3. This is a great global safety net.

**4. DTO projection:** Select only the fields you need. No entities, no lazy loading.

**Warning about `JOIN FETCH` with pagination:** Fetching a collection with `Pageable` makes Hibernate load **everything into memory** and page it there (you'll see the warning `HHH000104`). For paged lists, use batch fetching or a two-step query: first page the parent IDs, then fetch children for those IDs.

**Also:** Fetching **two** `List` collections in one query can cause `MultipleBagFetchException` (and a huge cartesian product). Fetch one collection at a time, or use `Set`.

---

## Q12. What are projections? When do you use them?

A projection selects **only some columns** instead of the whole entity. It's faster and lighter.

**Interface-based:**
```java
public interface UserSummary {
    String getName();
    String getEmail();
}

List<UserSummary> findByActiveTrue();
```

**Record/DTO-based (class projection):**
```java
public record UserDto(String name, String email) { }

@Query("SELECT new com.example.UserDto(u.name, u.email) FROM User u")
List<UserDto> findAllDtos();
```

**When to use:** Read-only endpoints, lists, and reports. You skip the persistence context tracking, avoid lazy problems, and reduce data sent from the database.

---

## Q13. How does pagination work in Spring Data?

```java
Page<User> findByActiveTrue(Pageable pageable);

Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
Page<User> page = userRepository.findByActiveTrue(pageable);
```

| Return type | Runs a COUNT query? | Use for |
|---|---|---|
| `Page<T>` | **Yes** (total pages, total elements) | UIs that show "page 3 of 20" |
| `Slice<T>` | **No** (only knows if there is a next page) | "Load more" and infinite scroll |
| `List<T>` | No | Just limits results |

`Slice` is cheaper because it skips the extra COUNT query, which can be slow on big tables.

---

## Q14. What is `@Transactional`? (Basics)

A **transaction** is a group of database operations that succeed **together or fail together** (ACID).

```java
@Service
public class TransferService {

    @Transactional
    public void transfer(Long from, Long to, BigDecimal amount) {
        accountRepo.debit(from, amount);
        accountRepo.credit(to, amount);   // if this fails, the debit is rolled back
    }
}
```

**Key points:**
- Put `@Transactional` on the **service layer**, not on repositories or controllers.
- **Rollback rule:** By default, it rolls back on **unchecked exceptions** (`RuntimeException`, `Error`). It does **not** roll back on **checked exceptions** (like `IOException`) unless you say so: `@Transactional(rollbackFor = Exception.class)`.
- **`readOnly = true`** on read methods lets Hibernate skip dirty checking and helps the database optimize.

```java
@Transactional(readOnly = true)
public List<User> findAll() { ... }
```

- Repository methods like `save()` are already transactional on their own. But **you need `@Transactional` on the service** to group multiple calls into one unit.

We will go deeper (propagation, isolation, proxy pitfalls) in **Concept 11**.

---

## Q15. What is optimistic vs pessimistic locking?

Both solve **concurrent updates**, where two users edit the same row at the same time.

**Optimistic locking** ("conflicts are rare, check at save time"):
```java
@Entity
public class Product {
    @Id private Long id;

    @Version
    private Long version;
}
```
Hibernate adds `WHERE id=? AND version=?` to the update. If someone else changed it first, no row matches, and you get an `OptimisticLockException`. You then retry or show a message to the user.

**Pessimistic locking** ("conflicts are likely, lock the row now"):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findForUpdate(@Param("id") Long id);
```
This runs `SELECT ... FOR UPDATE`. Other transactions **wait** until you finish.

| | Optimistic | Pessimistic |
|---|---|---|
| Blocks others? | No | Yes |
| Best when | Conflicts are rare | Conflicts are frequent (like stock counts) |
| Cost | Retries on conflict | Waiting, and possible deadlocks |

Optimistic is the default choice for most web apps, because it scales better.

---

## Q16. How do you configure the database and DDL settings?

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/shop
spring.datasource.username=app
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

**`ddl-auto` values:**

| Value | What it does |
|---|---|
| `none` | Does nothing |
| `validate` | Checks that tables match entities. Fails if not. |
| `update` | Tries to alter tables to match entities |
| `create` | Drops and recreates tables at startup |
| `create-drop` | Like `create`, and drops on shutdown |

**Production rule:** Use **`validate` or `none`**, and manage the schema with **Flyway** or **Liquibase** (versioned migration scripts). `update` can miss changes, never drops columns, and gives you no history or rollback. `create` in production would **wipe your data**.

With an **embedded database** like H2, Boot defaults to `create-drop`. With a real database, the default is `none`.

**Connection pool:** Spring Boot uses **HikariCP**, a very fast pool. Key setting: `spring.datasource.hikari.maximum-pool-size` (default 10). A bigger pool is **not** always better. Too many connections can overload the database.

---

## Q17. (Advanced) What is the difference between `equals()`/`hashCode()` problems in entities?

Entities often end up in `Set`s and collections, so `equals` and `hashCode` matter.

**Common mistakes:**
- Using Lombok `@Data` on entities. It includes all fields, including lazy relationships, which can trigger extra queries or infinite loops (`Customer.hashCode` → `orders` → `Order.hashCode` → `customer` ...).
- Using the **generated ID** in `hashCode`. The ID is `null` before save, so the hash changes after saving, and the object gets "lost" inside a `HashSet`.

**Safer approaches:**
- Use a **business key** (like email or an assigned UUID) that never changes.
- Or use a fixed hash such as `getClass().hashCode()` with an ID-based `equals` that handles `null`.

Avoid `@Data` and `@ToString` on entities. Write only what you need.

---

## Q18. (Advanced) What are the second-level cache and the query cache?

- **First-level cache:** Per transaction/session. Always on.
- **Second-level cache:** Shared across sessions, for the whole app. Off by default. Needs a provider (Ehcache, Caffeine, Redis via Hibernate).

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Country { }
```

**Best for:** Data that is read often and rarely changed (countries, currencies).
**Caution:** In a multi-instance setup, you need a distributed or synchronized cache, or instances will see stale data. Caching adds complexity, so measure first.

Often, **Spring's `@Cacheable`** on a service method is simpler than Hibernate's second-level cache.

---

## Q19. (Advanced) How do you do batch inserts efficiently?

By default, saving 10,000 entities sends 10,000 separate INSERTs. Enable JDBC batching:

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

**Catch:** `GenerationType.IDENTITY` **disables batching**, because Hibernate needs the generated ID right after each insert. Use `SEQUENCE` instead.

Also, for very large loads, **flush and clear** the persistence context every batch, so it doesn't fill up memory:
```java
if (i % 50 == 0) {
    entityManager.flush();
    entityManager.clear();
}
```

For truly huge imports, consider `JdbcTemplate.batchUpdate` or database bulk-load tools.

---

## Q20. (Advanced) When would you use `JdbcTemplate` or jOOQ instead of JPA?

JPA is great for **normal CRUD and object graphs**. It's not always the best tool.

Choose **`JdbcTemplate`, jOOQ, or native SQL** when:
- You have **complex reports** with heavy joins, window functions or aggregations.
- You need **bulk operations** at high speed.
- You want **full control** over the exact SQL.
- The domain has little "object behavior," and is mostly tables and queries.

Many real projects **mix both**: JPA for writes and simple reads, and SQL-based tools for reports. It's not "one or the other."

---

## Quick Recap

- **JPA** = spec, **Hibernate** = implementation, **Spring Data JPA** = less boilerplate.
- Entity needs `@Entity`, `@Id`, and a no-arg constructor. Prefer `SEQUENCE` for batching.
- Repositories are just interfaces. Spring makes a proxy for them.
- **Derived queries** for simple cases, **`@Query`** for complex ones. Always use parameters.
- The **persistence context** gives you a first-level cache and **dirty checking** (updates happen without `save()`).
- `mappedBy` marks the inverse side. The side with `@JoinColumn` owns the relationship.
- Make relationships **LAZY**. `@ManyToOne` is EAGER by default, so change it.
- **`LazyInitializationException`:** fetch properly or use DTOs. Consider turning **OSIV off**.
- **N+1:** fix with `JOIN FETCH`, `@EntityGraph`, or batch fetching. Be careful mixing collection fetch with pagination.
- **Projections/DTOs** make read queries faster.
- `Page` runs a COUNT query. `Slice` doesn't.
- `@Transactional` goes on services. It rolls back on **unchecked** exceptions by default.
- **Optimistic** (`@Version`) for rare conflicts, **pessimistic** for frequent ones.
- Production: `ddl-auto=validate` plus **Flyway/Liquibase**.
- Avoid `@Data` on entities. Careful with `equals`/`hashCode`.
- Batch inserts need `batch_size` and a non-IDENTITY ID strategy.

---

Reply **"next"** for **Concept 7: Exception Handling and Validation**. That covers `@ControllerAdvice`, `@ExceptionHandler`, custom exceptions, `ProblemDetail` (RFC 7807), Bean Validation, custom validators and more. Or tell me if you want any part of this concept explained again.

---
---
---
---

# Concept 7: Exception Handling and Validation

A good API does not crash with an ugly stack trace when something goes wrong. It returns a clear, consistent error message with the right status code. This concept shows how to do that, and how to stop bad data before it reaches your business logic.

---

## Q1. Why do we need proper exception handling in a REST API?

Without it, Spring Boot returns its default error response. That works, but it has problems:

- The format is generic and may not match what your clients expect.
- Some exceptions turn into a `500 Internal Server Error`, even when the real cause is a client mistake (like a missing record, which should be `404`).
- You may accidentally leak internal details (class names, SQL errors) that help attackers.
- Each controller may handle errors in its own style, so the API feels messy.

**Goal:** Every error, from anywhere in the app, comes out in **one consistent format** with the **right status code** and a **safe message**.

---

## Q2. What is the default error behavior in Spring Boot?

Spring Boot has a built-in `BasicErrorController` mapped to `/error`. When an exception escapes a controller, the servlet container forwards to `/error`, and Boot returns a JSON like this:

```json
{
  "timestamp": "2026-09-30T10:15:30.123+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/users/5"
}
```

**Good to know:**
- In Boot 2.3+, the `message` and `stacktrace` are **hidden by default**. You can turn them on with `server.error.include-message=always`, but do not do this in production.
- Browsers get an HTML "Whitelabel Error Page" instead of JSON.

This is fine as a fallback, but for a real API you should take control.

---

## Q3. How do you create and use a custom exception?

Make a simple class that extends `RuntimeException`:

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
```

Use it in your service:

```java
public User findById(Long id) {
    return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
}
```

**Why `RuntimeException` (unchecked)?**
- No need to write `throws` on every method.
- `@Transactional` rolls back on unchecked exceptions by default (from Concept 6).
- Spring itself uses unchecked exceptions.

**Quick option:** You can add `@ResponseStatus(HttpStatus.NOT_FOUND)` directly on the exception class, and Spring will return `404` for it.

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException { ... }
```

It is quick, but you cannot control the response body. So most teams use the next approach.

---

## Q4. What are `@ExceptionHandler` and `@ControllerAdvice` (or `@RestControllerAdvice`)?

**`@ExceptionHandler`** marks a method that handles a specific exception.

If you put it **inside one controller**, it only works for that controller:

```java
@RestController
public class UserController {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handle(UserNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
```

**`@ControllerAdvice`** makes handlers **global**, so they work for all controllers. **`@RestControllerAdvice`** is `@ControllerAdvice` + `@ResponseBody`, and it's the one you use for REST APIs.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(404, "NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        log.error("Unexpected error", ex);                     // log the details
        ErrorResponse body = new ErrorResponse(500, "INTERNAL_ERROR", "Something went wrong");
        return ResponseEntity.status(500).body(body);          // hide the details from client
    }
}

public record ErrorResponse(int status, String code, String message) { }
```

**How Spring picks the handler:** It chooses the **most specific** match. If `UserNotFoundException` and `Exception` handlers both exist, the first one wins for a `UserNotFoundException`.

**Why the catch-all `Exception` handler is important:** It makes sure unexpected errors return a clean message. The real stack trace goes to the **log**, not to the client. Never send stack traces in responses.

---

## Q5. What is the difference between `@ControllerAdvice` and `@RestControllerAdvice`?

The same as `@Controller` vs `@RestController`:

- `@ControllerAdvice`: handler methods may return view names. For returning JSON, you need `@ResponseBody` on each method.
- `@RestControllerAdvice`: `@ControllerAdvice` + `@ResponseBody`. Handler results are written as JSON directly.

You can also limit advice to some controllers:

```java
@RestControllerAdvice(basePackages = "com.example.api")
@RestControllerAdvice(annotations = RestController.class)
```

---

## Q6. What is `ProblemDetail` (RFC 7807 / RFC 9457)?

Everyone invented their own error format, which was messy. **Problem Details** is a standard JSON format for HTTP errors. Spring Framework 6 / Boot 3 supports it with the `ProblemDetail` class.

```json
{
  "type": "https://example.com/errors/user-not-found",
  "title": "User not found",
  "status": 404,
  "detail": "User not found with id: 5",
  "instance": "/users/5"
}
```

**In code:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleNotFound(UserNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("User not found");
        pd.setProperty("timestamp", Instant.now());     // add custom fields
        return pd;
    }
}
```

**Turn it on for Spring's built-in exceptions too:**

```properties
spring.mvc.problemdetails.enabled=true
```

**Or the cleaner way:** Extend `ResponseEntityExceptionHandler`. It already handles standard Spring MVC exceptions (wrong method, bad JSON, missing parameter, and so on) and returns `ProblemDetail`. You just add your own handlers on top.

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    // add your own @ExceptionHandler methods here
}
```

**Why it matters:** A standard format means clients (and tools) can understand your errors without reading your documentation.

---

## Q7. What is Bean Validation? How do you use it?

Bean Validation (Jakarta Validation) lets you declare rules on fields using annotations. Hibernate Validator is the implementation Spring Boot uses.

Add the starter first (it's **not** included in `starter-web` anymore since Boot 2.3):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Put rules on the DTO:**

```java
public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @NotBlank
        @Email
        String email,

        @Min(18) @Max(120)
        Integer age,

        @NotNull
        @Past
        LocalDate birthDate
) { }
```

**Turn validation on in the controller with `@Valid`:**

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    ...
}
```

If validation fails, Spring throws `MethodArgumentNotValidException`, and the method body **never runs**.

**Common annotations:**

| Annotation | Checks |
|---|---|
| `@NotNull` | Not null (empty string is OK) |
| `@NotEmpty` | Not null and not empty (strings, collections) |
| `@NotBlank` | Not null and has at least one non-space character (**strings only**) |
| `@Size(min, max)` | Length of string or size of collection |
| `@Min` / `@Max` | Number limits |
| `@Positive` / `@PositiveOrZero` | Number checks |
| `@Email` | Email format |
| `@Pattern(regexp = ...)` | Regex match |
| `@Past` / `@Future` | Date checks |

**Very common interview question: `@NotNull` vs `@NotEmpty` vs `@NotBlank`?**

| Value | `@NotNull` | `@NotEmpty` | `@NotBlank` |
|---|---|---|---|
| `null` | fails | fails | fails |
| `""` | passes | fails | fails |
| `"   "` (spaces) | passes | passes | fails |
| `"abc"` | passes | passes | passes |

For text input, `@NotBlank` is usually what you want.

---

## Q8. How do you return friendly validation error messages?

When `@Valid` fails, the default response is not very readable. Handle `MethodArgumentNotValidException` yourself:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors()
      .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", 400);
    body.put("message", "Validation failed");
    body.put("errors", fieldErrors);

    return ResponseEntity.badRequest().body(body);
}
```

Response:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "must be a well-formed email address",
    "name": "Name is required"
  }
}
```

The client can now highlight the exact wrong field in the form.

---

## Q9. How do you validate `@PathVariable` and `@RequestParam`?

`@Valid` works for request bodies. For simple parameters, put **`@Validated` on the class**, and constraints on the parameters:

```java
@RestController
@Validated
public class UserController {

    @GetMapping("/users/{id}")
    public User get(@PathVariable @Positive Long id) { ... }

    @GetMapping("/users")
    public List<User> search(@RequestParam @Size(min = 3) String name) { ... }
}
```

**Which exception is thrown?**
- `@Valid @RequestBody` failure → `MethodArgumentNotValidException`
- Parameter-level failure (with `@Validated`) → `ConstraintViolationException` in older versions. In **Spring 6.1+ / Boot 3.2+**, built-in method validation throws `HandlerMethodValidationException`.

You should handle whichever applies to your version, and return `400`.

---

## Q10. What is the difference between `@Valid` and `@Validated`?

| | `@Valid` | `@Validated` |
|---|---|---|
| From | Jakarta Bean Validation (standard) | Spring |
| Validation groups | No | **Yes** |
| Where it goes | Method parameters, fields (for nested objects) | Classes, method parameters |
| Enables parameter validation on a class | No | **Yes** (on the class) |

**Use `@Valid` for:** request bodies, and on fields to say "also validate this nested object."

```java
public record OrderRequest(
        @Valid @NotNull AddressRequest address,     // without @Valid, address fields are NOT checked
        @Valid List<@Valid ItemRequest> items
) { }
```

**Very common bug:** Forgetting `@Valid` on a nested object. The outer object is checked, but the inner one is silently skipped.

---

## Q11. What are validation groups?

Sometimes the same DTO needs different rules in different situations. For example, `id` must be **null** on create, but **required** on update.

```java
public interface OnCreate { }
public interface OnUpdate { }

public class UserRequest {
    @Null(groups = OnCreate.class)
    @NotNull(groups = OnUpdate.class)
    private Long id;

    @NotBlank(groups = {OnCreate.class, OnUpdate.class})
    private String name;
}

@PostMapping
public User create(@Validated(OnCreate.class) @RequestBody UserRequest req) { ... }

@PutMapping
public User update(@Validated(OnUpdate.class) @RequestBody UserRequest req) { ... }
```

Only `@Validated` (not `@Valid`) supports groups. Many teams simply create separate DTOs (`CreateUserRequest`, `UpdateUserRequest`) instead. It's often simpler and easier to read.

---

## Q12. How do you create a custom validator?

**Example:** A rule that a username must not be already taken.

**Step 1: Create the annotation**
```java
@Documented
@Constraint(validatedBy = UniqueUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUsername {
    String message() default "Username already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Step 2: Write the validator**
```java
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    public UniqueUsernameValidator(UserRepository userRepository) {   // Spring can inject here
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;                 // let @NotNull handle null
        return !userRepository.existsByUsername(value);
    }
}
```

**Step 3: Use it**
```java
public record RegisterRequest(@NotBlank @UniqueUsername String username) { }
```

**Important points:**
- Validators are Spring-managed, so you can **inject beans** into them.
- Return `true` for `null` and let `@NotNull` handle null. Each annotation should do **one job**.
- **Do not rely on this alone** for uniqueness. Two requests can pass the check at the same moment (a race condition). Always add a **unique constraint in the database** too, and handle `DataIntegrityViolationException` as `409 Conflict`.

---

## Q13. How do you handle database exceptions like duplicate keys?

Spring converts low-level database errors into its own hierarchy (`DataAccessException`). Handle the useful ones globally:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleConflict(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation", ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The request conflicts with existing data");    // generic message
}

@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ProblemDetail handleLock(ObjectOptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The record was changed by someone else. Please reload and try again");
}
```

**Do not** return `ex.getMessage()` directly. It may contain table names, column names or SQL, which is sensitive information.

---

## Q14. Which common Spring exceptions map to which status codes?

| Exception | Cause | Status |
|---|---|---|
| `MethodArgumentNotValidException` | `@Valid` body failed | 400 |
| `HttpMessageNotReadableException` | Broken or wrong JSON | 400 |
| `MissingServletRequestParameterException` | Required param missing | 400 |
| `MethodArgumentTypeMismatchException` | `/users/abc` where a number is expected | 400 |
| `NoResourceFoundException` / `NoHandlerFoundException` | URL not found | 404 |
| `HttpRequestMethodNotSupportedException` | Wrong HTTP method | 405 |
| `HttpMediaTypeNotSupportedException` | Wrong `Content-Type` | 415 |
| `DataIntegrityViolationException` | Unique or FK violation | 409 (you choose) |
| `AccessDeniedException` | Not allowed (Spring Security) | 403 |

**Note:** Extending `ResponseEntityExceptionHandler` covers most of the web-related ones automatically.

---

## Q15. (Advanced) What is the order in which exception handlers are resolved?

Spring MVC uses a chain of `HandlerExceptionResolver`s:

1. **`ExceptionHandlerExceptionResolver`**: looks for `@ExceptionHandler` methods (in the controller first, then in `@ControllerAdvice` classes).
2. **`ResponseStatusExceptionResolver`**: handles `@ResponseStatus` and `ResponseStatusException`.
3. **`DefaultHandlerExceptionResolver`**: converts standard Spring exceptions to status codes.
4. If nothing handles it, the exception goes to the container, which forwards to `/error` (`BasicErrorController`).

**Ordering multiple advice classes:** Use `@Order` on the `@ControllerAdvice` classes. Lower number = higher priority. Put your catch-all `Exception` handler in the **lowest-priority** advice class.

---

## Q16. (Advanced) What is `ResponseStatusException`?

A quick way to throw an exception with a status **without** creating a custom class:

```java
throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
```

**Good for:** Small apps and quick prototypes.
**Downside:** Status codes leak into the service layer, mixing web concerns with business logic. In a bigger app, prefer **custom domain exceptions** plus a `@RestControllerAdvice`, so the service layer stays clean and knows nothing about HTTP.

---

## Q17. (Advanced) Why do exceptions thrown in filters not reach `@ControllerAdvice`?

**Because filters run before `DispatcherServlet`.** `@ControllerAdvice` is part of Spring MVC, which starts only **after** the request reaches `DispatcherServlet`.

So an exception from a servlet filter (for example, a JWT filter) skips your handlers and ends up in the container's `/error` handling.

**Fixes:**
- In a custom filter, write the error response **directly** (set status and write JSON).
- Or inject `HandlerExceptionResolver` into the filter and call `resolver.resolveException(request, response, null, ex)`. Now your `@ControllerAdvice` handles it.
- In **Spring Security**, use `AuthenticationEntryPoint` (for 401) and `AccessDeniedHandler` (for 403). We will cover them in Concept 8.

This is a great "experience" question. Many people never notice it until it bites them in production.

---

## Q18. (Advanced) What are good practices for error handling in production?

- **One global handler** (`@RestControllerAdvice`) and one standard format (`ProblemDetail` is a good choice).
- **Log server errors (5xx) with full stack trace.** Log client errors (4xx) at a lower level (`warn` or `info`), because they are usually not your bug.
- **Never log and rethrow the same exception in many layers.** You end up with the same stack trace 5 times in the logs. Log it **once**, in the global handler.
- **Do not expose internals:** no stack traces, SQL, class names or file paths in responses.
- **Add a correlation/trace ID** to logs and to the error response, so support can find the exact request quickly.
- **Use error codes** (`USER_NOT_FOUND`) along with messages, so clients can react in code without parsing text.
- **Validate at the edge** (controller with `@Valid`), and again in the **domain/database** (constraints) for rules that must never break.
- **Do not use exceptions for normal flow control.** For expected outcomes (like "optional value not found"), `Optional` is cleaner than throwing.
- **Translate messages** with `MessageSource` if you support many languages.

---

## Quick Recap

- Default errors come from `BasicErrorController`. Take control with a global handler.
- Create **custom unchecked exceptions** for business errors.
- **`@RestControllerAdvice`** + **`@ExceptionHandler`** = one global place for error handling. The most specific handler wins.
- Use **`ProblemDetail`** (RFC 7807) for a standard error format. Extend `ResponseEntityExceptionHandler` for built-in Spring exceptions.
- Add **`spring-boot-starter-validation`**, put constraints on DTOs, and use **`@Valid`** on `@RequestBody`.
- **`@NotBlank`** (text) is stricter than **`@NotEmpty`**, which is stricter than **`@NotNull`**.
- Use **`@Validated`** on the class for `@PathVariable`/`@RequestParam`, and for **validation groups**.
- Forgetting **`@Valid` on nested objects** silently skips validation.
- **Custom validators** can inject beans, but always back them up with a **database constraint**.
- Never leak stack traces or DB messages. Log them, and return a safe message.
- **Filter exceptions** don't reach `@ControllerAdvice`. Use `HandlerExceptionResolver` or Spring Security handlers.
- Log once, return a trace ID, and use clear error codes.

---

Reply **"next"** for **Concept 8: Spring Security**. That covers authentication vs authorization, the filter chain, `SecurityFilterChain`, password encoding, JWT, method security, CSRF, OAuth2 and more. It's a big topic and interviewers love it. Or tell me if you want any part of this concept explained again.

---
---
---


# Concept 8: Spring Security

Security is a big topic, and interviewers love it because mistakes here are expensive. We'll start with the basics, then build up to filters, JWT and OAuth2.

---

## Q1. What is the difference between Authentication and Authorization?

**Authentication** asks: **"Who are you?"** The system checks your identity, for example with a username and password.

**Authorization** asks: **"What are you allowed to do?"** The system checks whether you have permission for an action.

**Airport example:** Showing your passport and ticket at the gate is authentication. Being allowed into the business lounge only if you have the right ticket class is authorization.

**The order is always the same:** authenticate first, then authorize. It also connects to the status codes from Concept 4:

- Failed authentication → **401 Unauthorized**
- Failed authorization → **403 Forbidden**

---

## Q2. What happens when you just add `spring-boot-starter-security`?

With no code at all, Boot secures the whole app:

- **Every endpoint** requires login.
- A default login form and HTTP Basic authentication are turned on.
- A default user named **`user`** is created, with a **random password printed in the console** at startup (`Using generated security password: ...`).
- CSRF protection, session fixation protection and security headers are enabled.

You can set your own default user for quick tests:
```properties
spring.security.user.name=admin
spring.security.user.password=secret
```

**Why this matters:** Spring Security is **secure by default**. If you forget to configure anything, the app is locked, not open. That's why you see `401` on all your APIs right after adding the dependency, a very common beginner surprise.

As soon as you define your own `SecurityFilterChain` bean, Boot's default configuration backs off (the same `@ConditionalOnMissingBean` idea from Concept 2).

---

## Q3. How does Spring Security work internally? What is the filter chain?

**Short answer:** Spring Security is a **chain of servlet filters**. Every request passes through them before it reaches your controller.

Here is the structure:

1. **`DelegatingFilterProxy`** is a normal servlet filter registered in the container. Its only job is to hand the request over to a Spring bean.
2. That bean is **`FilterChainProxy`**. It holds one or more **`SecurityFilterChain`**s and picks the **first chain** that matches the request URL.
3. The chosen chain runs its filters one by one.

**Some important filters, in rough order:**

| Filter | Job |
|---|---|
| `SecurityContextHolderFilter` | Loads the security context (for example, from the session) |
| `HeaderWriterFilter` | Adds security headers to the response |
| `CorsFilter` | Handles CORS |
| `CsrfFilter` | Checks the CSRF token |
| `LogoutFilter` | Handles logout |
| `UsernamePasswordAuthenticationFilter` | Handles form login |
| `BasicAuthenticationFilter` | Handles HTTP Basic |
| `AnonymousAuthenticationFilter` | Marks unauthenticated users as "anonymous" |
| `ExceptionTranslationFilter` | Turns security exceptions into 401 or 403 responses |
| `AuthorizationFilter` | Final check: is this user allowed to access this URL? |

**Why it matters:** Everything happens **before** `DispatcherServlet` (from Concept 4). This is why `@ControllerAdvice` cannot catch security exceptions from the filters (Concept 7, Q17).

**Tip:** Turn on `logging.level.org.springframework.security=DEBUG` to see the exact filter list, and which filter did what to a request. It is the best way to debug a strange 401 or 403.

---

## Q4. What are the core building blocks?

| Component | What it is |
|---|---|
| **`SecurityContextHolder`** | Storage that holds the current user's details (uses a `ThreadLocal`) |
| **`SecurityContext`** | Holds the current `Authentication` |
| **`Authentication`** | Represents "who is logged in": principal, credentials, authorities, and an `authenticated` flag |
| **`GrantedAuthority`** | A permission or role, like `ROLE_ADMIN` or `orders:read` |
| **`AuthenticationManager`** | The main interface with one method: `authenticate(...)` |
| **`ProviderManager`** | The usual implementation. It loops over a list of `AuthenticationProvider`s |
| **`AuthenticationProvider`** | Does the real check for one type of login (password, JWT, LDAP...) |
| **`UserDetailsService`** | Loads a user by username, from a database, for example |
| **`UserDetails`** | Spring's representation of a user (username, password hash, authorities, enabled/locked flags) |
| **`PasswordEncoder`** | Hashes passwords and checks them |

Don't try to memorize these as a list. The next question shows how they work together.

---

## Q5. Explain the authentication flow step by step.

Take a username and password login as the example.

1. The request arrives and a filter (like `UsernamePasswordAuthenticationFilter`) reads the credentials.
2. It creates an **unauthenticated** `Authentication` object with the username and password.
3. It passes this to the **`AuthenticationManager`** (`ProviderManager`).
4. `ProviderManager` asks each **`AuthenticationProvider`**, "Can you handle this type?" It picks one, usually **`DaoAuthenticationProvider`**.
5. `DaoAuthenticationProvider` does two things:
   - Calls **`UserDetailsService.loadUserByUsername()`** to fetch the user.
   - Calls **`PasswordEncoder.matches()`** to compare the typed password with the stored hash.
6. If everything is correct, it returns a **fully authenticated** `Authentication` with the user's authorities.
7. The filter stores it in the **`SecurityContextHolder`**. For session-based apps, it's also saved in the `HttpSession`.
8. On failure, an `AuthenticationException` is thrown (like `BadCredentialsException`), and the user gets a 401.

**On later requests:** The context is restored (from the session, or by a JWT filter). Then `AuthorizationFilter` checks whether this user may access the URL.

**One-line summary for interviews:** *Filter → AuthenticationManager → AuthenticationProvider → UserDetailsService + PasswordEncoder → SecurityContext.*

---

## Q6. How do you configure security in Spring Boot 3? (What happened to `WebSecurityConfigurerAdapter`?)

**Old way (Spring Security 5):** You extended `WebSecurityConfigurerAdapter` and overrode `configure(HttpSecurity)`.

**New way (Spring Security 6 / Boot 3):** `WebSecurityConfigurerAdapter` was **removed**. You now declare a **`SecurityFilterChain` bean**. This is a very common interview trap. If someone shows you old code with `extends WebSecurityConfigurerAdapter`, say it no longer works in Boot 3.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/products/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

**Key points:**

- The style with lambdas is the modern **Lambda DSL**. The old chained style with `.and()` is deprecated and on its way out.
- **`authorizeHttpRequests`** replaces the old `authorizeRequests`. **`requestMatchers`** replaces `antMatchers` and `mvcMatchers`.
- **Rules are checked top to bottom, and the first match wins.** Put specific rules first and **`anyRequest()` last**. If you put `anyRequest().authenticated()` first, later rules never run.
- **Deny by default** is safest: end with `anyRequest().authenticated()` (or even `denyAll()`), so a new endpoint is never public by accident.

---

## Q7. How do you load users from a database?

Implement `UserDetailsService`:

```java
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())     // already hashed
                .roles(user.getRole())                // "ADMIN" becomes ROLE_ADMIN
                .build();
    }
}
```

Spring Boot finds this single `UserDetailsService` bean and uses it automatically. The default in-memory user backs off.

**Important:** The database must store the **hashed** password, never the plain one. That's the next question.

---

## Q8. How should passwords be stored? What is `PasswordEncoder`?

**Rule:** Never store plain-text passwords. Never use fast hashes like MD5 or SHA-1 either.

**Why not a fast hash?** Attackers can try billions of guesses per second on a stolen database. Password hashing should be **deliberately slow**.

**What good algorithms do:**
- **Salting:** A random value is added to each password before hashing. Two users with the same password get different hashes, which blocks "rainbow table" attacks. BCrypt does this automatically, and stores the salt inside the hash.
- **Work factor:** You can make hashing slower as computers get faster.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();      // default strength 10
}
```

```java
String hash = passwordEncoder.encode("myPassword");     // store this
boolean ok = passwordEncoder.matches("myPassword", hash);
```

**Options:** `BCryptPasswordEncoder` (the common choice), `Argon2PasswordEncoder` (modern and strong), `SCryptPasswordEncoder`, `Pbkdf2PasswordEncoder`.

**`DelegatingPasswordEncoder`:** Created with `PasswordEncoderFactories.createDelegatingPasswordEncoder()`. Hashes are stored with a prefix like `{bcrypt}$2a$10$...`. This lets you **change algorithms later** while old passwords keep working. You may see this prefix when you forget to define a `PasswordEncoder` bean.

**Good to know:**
- You **cannot decrypt** a hash. Hashing is one-way. "Forgot password" means resetting, never showing the old one.
- `NoOpPasswordEncoder` (plain text) is deprecated and only for demos.
- BCrypt only uses the first **72 bytes** of a password.

---

## Q9. What is the difference between a Role and an Authority?

Both are `GrantedAuthority` objects. The difference is only a **naming convention**.

| | Role | Authority |
|---|---|---|
| Meaning | A **group** or job type (`ADMIN`, `USER`) | A **specific permission** (`order:delete`) |
| Stored as | Authority with **`ROLE_`** prefix (`ROLE_ADMIN`) | Plain text (`order:delete`) |
| Check with | `hasRole("ADMIN")` | `hasAuthority("order:delete")` |

**The prefix trap:** `hasRole("ADMIN")` checks for the authority **`ROLE_ADMIN`**. Spring adds the prefix for you. If you write `hasRole("ROLE_ADMIN")`, Spring complains, because it would look for `ROLE_ROLE_ADMIN`. And with `hasAuthority`, you must write the full text: `hasAuthority("ROLE_ADMIN")`.

**Which one to use?** Roles are fine for small apps. For finer control, use authorities as permissions, and map roles to sets of permissions. Then adding a new rule doesn't need a new role.

---

## Q10. What is method-level security? (`@PreAuthorize`)

URL rules protect endpoints. **Method security** protects the **method itself**, so the rule holds even if that method is called from another place.

**Enable it:**
```java
@Configuration
@EnableMethodSecurity           // replaces @EnableGlobalMethodSecurity from older versions
public class MethodSecurityConfig { }
```

**Use it:**
```java
@Service
public class OrderService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long id) { ... }

    @PreAuthorize("#userId == authentication.principal.id")   // only your own data
    public List<Order> getOrders(Long userId) { ... }

    @PostAuthorize("returnObject.owner == authentication.name")
    public Order getOrder(Long id) { ... }
}
```

- **`@PreAuthorize`** checks **before** the method runs.
- **`@PostAuthorize`** checks **after**, using the returned object.
- `@Secured` and `@RolesAllowed` are older and simpler. They don't support expressions like SpEL.

**The proxy trap:** Method security works through **proxies** (same as `@Transactional`, from Concept 6). If one method calls another method **in the same class**, the call skips the proxy, and the security check is **not applied**. Call it from another bean, or restructure.

**Why use both URL and method security?** **Defense in depth.** URL rules are your first gate. Method rules protect the business logic and handle data-level rules like "only the owner can see this order," which URL patterns can't express.

---

## Q11. Session-based vs token-based authentication: what's the difference?

| | Session-based (stateful) | Token-based (stateless) |
|---|---|---|
| After login | Server creates a session, and sends a `JSESSIONID` cookie | Server creates a signed token, and the client keeps it |
| Each request | Browser sends the cookie; the server looks up the session | Client sends `Authorization: Bearer <token>`; the server verifies the token |
| Server memory | Keeps session data | Keeps nothing |
| Scaling | Needs sticky sessions or shared storage (Spring Session + Redis) | Easy: any instance can verify the token |
| Logout / revoke | Easy: delete the session | Harder: the token stays valid until it expires |
| Best for | Traditional web apps | REST APIs, mobile apps, microservices |

In Spring Security, you choose stateless mode with:
```java
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
Now Spring will not create or use `HttpSession` for security.

**Connection to REST (Concept 4):** REST says servers should be stateless. That's why token-based auth fits REST APIs so well.

---

## Q12. What is JWT? What does it contain?

**JWT (JSON Web Token)** is a compact string that carries **claims** (facts about the user) and is **digitally signed**, so nobody can change it without being noticed.

It has three parts, separated by dots:

```
xxxxx.yyyyy.zzzzz
header.payload.signature
```

| Part | Contains |
|---|---|
| **Header** | Algorithm and token type: `{"alg":"HS256","typ":"JWT"}` |
| **Payload** | Claims: `sub` (user), `iat` (issued at), `exp` (expiry), `roles`, and so on |
| **Signature** | A hash of header + payload made with a secret or private key |

**How verification works:** The server recomputes the signature and compares. If someone changed the payload (for example, to `role: ADMIN`), the signature won't match, so the token is rejected. It also checks that `exp` has not passed.

**Very important point that interviewers test:**
**A JWT is signed, not encrypted.** The header and payload are only Base64-encoded, so **anyone can read them**. Never put passwords, card numbers or other secrets in a JWT.

**Signing choices:**
- **HS256:** One shared secret signs and verifies. Simple, but every service that verifies also needs the secret (and could forge tokens).
- **RS256:** A private key signs, and a public key verifies. Better for microservices, because services can verify without being able to create tokens.

**Drawbacks and how to handle them:**
- **Revocation is hard,** since the server keeps no state. Use **short-lived access tokens** (like 5 to 15 minutes) plus a longer **refresh token**. For emergency revoke, keep a small blocklist.
- **Where to store it in the browser?** `localStorage` is exposed to XSS. An `HttpOnly` cookie is safer against XSS, but then you need to think about CSRF (Q14). There is no perfect answer. Know the trade-off.
- Use a **long random secret** (at least 256 bits) and keep it outside the code.

---

## Q13. How do you implement JWT authentication in Spring Boot?

**Two parts: a login endpoint that creates tokens, and a filter that checks them.**

**Part 1: Login endpoint**
```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // constructor omitted

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        return Map.of("token", jwtService.generateToken(auth.getName()));
    }
}
```

To inject `AuthenticationManager`, expose it as a bean:
```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

**Part 2: JWT filter**
```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // constructor omitted

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);        // no token: continue as anonymous
            return;
        }

        try {
            String username = jwtService.extractUsername(header.substring(7)); // verifies signature + expiry
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | UsernameNotFoundException e) {
            // invalid token: leave the context empty, so the entry point returns 401
        }
        chain.doFilter(request, response);
    }
}
```

**Part 3: Plug it into the chain**
```java
http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**Notes:**
- `OncePerRequestFilter` guarantees the filter runs once per request.
- The filter doesn't reject bad tokens itself. It just leaves the user unauthenticated, and Spring's own `ExceptionTranslationFilter` produces the 401.
- **A simpler, safer option in real projects:** Use Spring's built-in support with `spring-boot-starter-oauth2-resource-server`. It validates JWTs for you, with very little code:
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.example.com/realms/myrealm
```
```java
http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
```
Writing your own filter is a great way to learn, and it's asked in interviews. But well-tested library code is better in production.

---

## Q14. What is CSRF? When can you disable it?

**CSRF (Cross-Site Request Forgery):** An attacker tricks a logged-in user's **browser** into sending a request to your site. The browser **automatically attaches cookies**, so your server thinks it's a real request from the user.

**Example:** You are logged in to `bank.com`. You open `evil.com`, which has a hidden form that posts to `bank.com/transfer`. Your browser sends your session cookie along, and the money moves.

**How Spring protects you:** For state-changing requests (POST, PUT, DELETE), it requires a **CSRF token** that an attacker's site cannot read. It's on by default.

**When can you disable it?**
CSRF only works because browsers **automatically** send credentials (cookies). So:

| Your app | CSRF protection |
|---|---|
| Server-rendered pages with session cookies | **Keep it ON** |
| Stateless API where the client sends `Authorization: Bearer ...` manually | **Safe to disable** |
| API where the JWT is stored in a **cookie** | **Keep it ON** (or use `SameSite` cookies plus CSRF tokens) |

```java
http.csrf(csrf -> csrf.disable());     // only for stateless, header-token APIs
```

**A common mistake:** Turning off CSRF "because POST requests return 403" without understanding why, and then using cookie-based login. Also remember `SameSite=Lax/Strict` cookies give extra defense.

---

## Q15. How does CORS work with Spring Security?

From Concept 4, CORS is enforced by the browser. The extra Spring Security detail:

The browser sends a **preflight `OPTIONS` request** without credentials. If Spring Security sees it first and demands login, it returns 401, and the real request never happens.

Fix: enable CORS **in the security chain**, so `CorsFilter` runs early and answers the preflight:

```java
http.cors(Customizer.withDefaults());

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://myfrontend.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

Just adding `@CrossOrigin` or `WebMvcConfigurer` is often **not enough** when Security is on. Also, don't use `*` for origins together with `allowCredentials(true)`.

---

## Q16. How do you customize the 401 and 403 responses?

Two components, and each has a different job:

| Component | When it's called | Status |
|---|---|---|
| **`AuthenticationEntryPoint`** | User is **not authenticated** (no login or bad token) | **401** |
| **`AccessDeniedHandler`** | User **is authenticated** but not allowed | **403** |

```java
http.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Authentication required\"}");
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"You do not have permission\"}");
    })
);
```

Without a custom entry point, form login redirects to a login page, which is bad for a REST API that expects JSON.

**A tricky gotcha (from Concept 7):** If `@PreAuthorize` fails **inside** a controller or service, the `AccessDeniedException` is thrown during MVC processing. Your `@RestControllerAdvice` with `@ExceptionHandler(Exception.class)` can catch it and turn it into a **500**. Add specific handlers for `AccessDeniedException` (403) and `AuthenticationException` (401), or rethrow them, so Spring Security handles them properly.

---

## Q17. What are OAuth2 and OpenID Connect (OIDC)?

**OAuth2** is a standard for **delegated authorization**. It lets an app access something on your behalf **without knowing your password**. Example: "Let this app read my Google Calendar."

**Four roles:**
- **Resource Owner:** the user.
- **Client:** the app that wants access.
- **Authorization Server:** logs the user in and issues tokens (Google, Keycloak, Okta, Auth0).
- **Resource Server:** the API that holds the data and checks the tokens.

**Common flows:**

| Flow | Used for |
|---|---|
| **Authorization Code (with PKCE)** | Web apps, SPAs, mobile apps with a user login. **The standard choice.** |
| **Client Credentials** | **Service-to-service** calls with no user |
| **Refresh Token** | Getting a new access token without asking for login again |

The older **Implicit** and **Password** flows are outdated and removed in OAuth 2.1. Avoid them.

**OIDC (OpenID Connect)** is a thin layer on top of OAuth2 that adds **authentication**. It adds an **ID token** (a JWT that says who the user is). This is what "Login with Google" uses.

**Easy memory trick:** **OAuth2 = "what can this app do?"** **OIDC = "who is this user?"**

**In Spring Boot:**
- **`spring-boot-starter-oauth2-client`**: your app logs users in with an external provider (`http.oauth2Login(...)`).
- **`spring-boot-starter-oauth2-resource-server`**: your API validates incoming JWT access tokens.
- **Spring Authorization Server**: build your own authorization server. In many projects, teams use **Keycloak** or a cloud provider instead of building one.

**Microservices tip:** Usually, users log in once at the authorization server. The API gateway and each service act as **resource servers** and validate the same token.

---

## Q18. (Advanced) How do you have different security rules for different parts of the app?

Define **multiple `SecurityFilterChain` beans**, each with a `securityMatcher` and an `@Order`:

```java
@Bean
@Order(1)
public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
    return http.build();
}

@Bean
@Order(2)
public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.requestMatchers("/login", "/css/**").permitAll()
                                     .anyRequest().authenticated())
        .formLogin(Customizer.withDefaults());
    return http.build();
}
```

`FilterChainProxy` picks the **first chain whose matcher fits**. The last chain (no `securityMatcher`) acts as the catch-all.

**Use case:** A stateless JWT API and a session-based admin web page in the same app.

**Related question: `permitAll()` vs `web.ignoring()`?**
- `permitAll()`: the request **still goes through the filter chain** (headers, logging, CSRF), and is simply allowed. **Preferred.**
- `web.ignoring()`: the request **skips Spring Security entirely** (no security headers). Spring now warns against using it, even for static files.

---

## Q19. (Advanced) How do you get the current logged-in user?

**Option 1: Directly from the holder**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();
```

**Option 2: Inject it into a controller method (cleaner)**
```java
@GetMapping("/me")
public String me(@AuthenticationPrincipal UserDetails user, Principal principal) {
    return user.getUsername();
}
```

**Threads:** The `SecurityContext` is stored in a **`ThreadLocal`**, so it belongs to the **request thread**. If you start a new thread, or run `@Async` code, the context is **not** copied by default, and the user looks anonymous there. Solutions: use `DelegatingSecurityContextExecutor` / `DelegatingSecurityContextAsyncTaskExecutor`, or pass the needed user info as a method parameter. This is a rare but great "experience" question.

**Also:** For auditing (`createdBy` fields), Spring Data has `AuditorAware`, which reads the current user from the context.

---

## Q20. What common attacks does Spring Security protect against?

| Attack | Built-in protection |
|---|---|
| **CSRF** | CSRF tokens (on by default) |
| **Session fixation** | Session ID is changed after login |
| **Clickjacking** | `X-Frame-Options: DENY` header |
| **MIME sniffing** | `X-Content-Type-Options: nosniff` header |
| **Man-in-the-middle** | HSTS header (on HTTPS), plus you should enforce HTTPS |
| **Caching of secret pages** | `Cache-Control: no-store` for secured responses |
| **User enumeration** | Same error for "unknown user" and "wrong password" (default) |

**Things Spring does NOT do for you:**
- **XSS:** Escape output in templates, and consider a **Content-Security-Policy** header.
- **SQL injection:** Use parameters (Concept 6).
- **Brute force:** Add **rate limiting** and account lockout (with a gateway, bucket4j, or similar).
- **Broken access control (IDOR):** For example, `GET /orders/123` returning someone else's order. Always check that the resource **belongs to the current user**. This is one of the most common real-world security bugs.

---

## Q21. What are common mistakes and best practices?

**Common mistakes:**
- Storing passwords in plain text, or using a fast hash (MD5 / SHA).
- Disabling CSRF blindly while still using cookies.
- Wrong rule order. **First match wins.** `anyRequest()` must be last.
- Using `hasRole("ROLE_ADMIN")` with the prefix.
- Trusting **roles or user IDs sent by the client** instead of reading them from the authenticated user.
- Checking access only at URL level, not on the data itself (IDOR).
- Weak or hard-coded JWT secrets, very long-lived tokens, and secrets in Git.
- Putting sensitive data in the JWT payload.
- Exposing Actuator endpoints publicly (we cover this next).
- A catch-all `@ExceptionHandler(Exception.class)` that swallows `AccessDeniedException`.
- Leaving default users or passwords, and showing detailed errors.
- Copying old `WebSecurityConfigurerAdapter` code into a Boot 3 project.

**Best practices:**
- **Deny by default.** Open up only what is needed.
- Use **HTTPS everywhere**.
- Prefer **proven libraries and standards** (OAuth2/OIDC, resource server) over home-made security.
- **Short-lived access tokens** plus refresh tokens.
- Use **defense in depth**: URL rules, method security, and data-ownership checks.
- Log security events (failed logins, access denied) without logging passwords or tokens.
- **Keep dependencies updated.** Security fixes come often.
- Test your rules with `spring-security-test` (`@WithMockUser`). We cover this in Concept 10.

---

## Quick Recap

- **Authentication** = who you are (401). **Authorization** = what you can do (403).
- Adding the starter **locks everything**. Spring Security is secure by default.
- It's a **chain of filters** before `DispatcherServlet`: `DelegatingFilterProxy` → `FilterChainProxy` → `SecurityFilterChain`.
- Login flow: **Filter → AuthenticationManager → AuthenticationProvider → UserDetailsService + PasswordEncoder → SecurityContext**.
- In Boot 3, define a **`SecurityFilterChain` bean**. `WebSecurityConfigurerAdapter` is gone. Use the lambda style, `authorizeHttpRequests`, `requestMatchers`, and put `anyRequest()` last.
- Store passwords with **BCrypt** (or Argon2). Hashing is one-way and salted.
- `hasRole("ADMIN")` means the authority `ROLE_ADMIN`. Roles are groups, authorities are permissions.
- **`@PreAuthorize`** protects methods (works via proxies, so self-calls skip it).
- Session = stateful. **JWT = stateless**, and it's **signed, not encrypted**. Use short-lived tokens plus refresh tokens.
- JWT setup: a login endpoint, plus a `OncePerRequestFilter` added before `UsernamePasswordAuthenticationFilter`. In production, prefer the **OAuth2 resource server** support.
- **CSRF** protects cookie-based apps. It is safe to disable only for stateless, header-token APIs.
- With Security, enable **CORS inside the security chain**, so preflight requests pass.
- Use **`AuthenticationEntryPoint`** (401) and **`AccessDeniedHandler`** (403). Don't let a catch-all handler turn access errors into 500s.
- **OAuth2** = delegated access. **OIDC** = login and identity. Use **Authorization Code + PKCE** for users and **Client Credentials** for services.
- Multiple `SecurityFilterChain`s with `securityMatcher` and `@Order` let one app have different rules per area.
- The context lives in a **`ThreadLocal`**, so it doesn't automatically flow to new threads.
- Spring won't stop **IDOR, XSS, brute force or SQL injection** for you. Those are your job.

---

Reply **"next"** for **Concept 9: Actuator and Monitoring**. That covers health checks, metrics, Micrometer, Prometheus and Grafana, custom health indicators, securing Actuator endpoints, logging and tracing. It's a favorite in real-world and DevOps-flavored interviews. Or tell me if you want any part of this concept explained again.

---
------
---

# Concept 9: Actuator and Monitoring

Writing the app is half the job. The other half is knowing what it is doing once it runs in production. Is it healthy? Is it slow? Why did that one request fail? This concept covers how Spring Boot answers those questions.

---

## Q1. What is Spring Boot Actuator?

**Short answer:** Actuator adds **production-ready features** to your app: health checks, metrics, environment info, logger control and more. You get them as ready-made HTTP endpoints (or JMX beans).

**Deeper explanation:**
Think of a car dashboard. It shows speed, fuel, engine temperature and warning lights. Without it you can still drive, but you won't know something is wrong until the car stops. Actuator is the dashboard for your app.

Add it with one starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Now `http://localhost:8080/actuator/health` works. Because of auto-configuration (Concept 2), you write no code.

---

## Q2. What are the important Actuator endpoints?

| Endpoint | What it shows |
|---|---|
| `/actuator/health` | Is the app (and its dependencies) healthy? |
| `/actuator/info` | App info: version, Git commit, build time |
| `/actuator/metrics` | Numbers: memory, CPU, request counts, timings |
| `/actuator/prometheus` | Metrics in Prometheus format (needs an extra library) |
| `/actuator/loggers` | View and **change log levels at runtime** |
| `/actuator/env` | Environment and properties |
| `/actuator/configprops` | All `@ConfigurationProperties` values |
| `/actuator/beans` | Every bean in the context |
| `/actuator/mappings` | All URL mappings (every `@RequestMapping`) |
| `/actuator/conditions` | The auto-configuration report (Concept 2) |
| `/actuator/threaddump` | Current threads, useful for finding stuck threads |
| `/actuator/heapdump` | Downloads a JVM memory dump |
| `/actuator/scheduledtasks` | All `@Scheduled` tasks |
| `/actuator/httpexchanges` | Recent HTTP requests (needs an `HttpExchangeRepository` bean) |
| `/actuator/shutdown` | Shuts the app down (**off by default**) |

The names `httptrace` (older) and `httpexchanges` (Boot 3) are a small trap. If someone asks for `httptrace` in Boot 3, it was renamed.

---

## Q3. Why can I only see `/actuator/health` after adding Actuator?

Because of **safe defaults**. Two separate ideas are involved, and people mix them up:

| Idea | Meaning |
|---|---|
| **Enabled** | The endpoint exists inside the app |
| **Exposed** | The endpoint can be reached from outside (over HTTP or JMX) |

Over HTTP, Boot exposes **only `health`** by default. To expose more:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

`include=*` exposes all of them. That is fine on your laptop, but **dangerous in production** (see Q13).

**Useful settings:**

```properties
management.endpoints.web.base-path=/manage            # change /actuator to /manage
management.server.port=9001                           # run Actuator on a separate port
management.endpoint.shutdown.enabled=true             # turn on a disabled endpoint
```

In Boot 3.4 and later, the newer `management.endpoint.<name>.access` setting replaces `enabled`. The `enabled` property still works but is deprecated.

---

## Q4. How does the health endpoint work?

`/actuator/health` returns the overall status of your app:

```json
{ "status": "UP" }
```

With details turned on:

```properties
management.endpoint.health.show-details=when-authorized
```

```json
{
  "status": "UP",
  "components": {
    "db":        { "status": "UP", "details": { "database": "PostgreSQL" } },
    "diskSpace": { "status": "UP", "details": { "free": 84212379648 } },
    "redis":     { "status": "UP" }
  }
}
```

**How it works:** Actuator has many **`HealthIndicator`** beans, one for each thing it can check: database, disk space, Redis, RabbitMQ, Kafka and so on. Each one is auto-configured **only if the matching library is on your classpath** (the same condition idea from Concept 2). The results are combined into one overall status.

**Statuses and HTTP codes:**

| Status | HTTP code |
|---|---|
| `UP` | 200 |
| `DOWN` | **503** |
| `OUT_OF_SERVICE` | **503** |
| `UNKNOWN` | 200 |

**How are statuses combined?** The **worst status wins**. If the database is `DOWN` and everything else is `UP`, the overall status is `DOWN`.

**`show-details` values:** `never` (default), `when-authorized`, `always`. Health details reveal your infrastructure (database type, hostnames), so don't use `always` on a public endpoint.

---

## Q5. How do you create a custom health indicator?

Implement `HealthIndicator` and make it a bean:

```java
@Component
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    private final PaymentClient client;

    public PaymentGatewayHealthIndicator(PaymentClient client) {
        this.client = client;
    }

    @Override
    public Health health() {
        try {
            long ms = client.ping();
            return Health.up().withDetail("latencyMs", ms).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

**Naming:** The key in the JSON comes from the bean name **without** the `HealthIndicator` suffix. Here it becomes `paymentGateway`.

**Good practices:**
- Keep checks **fast**, with a short timeout. A slow health check makes the health endpoint itself slow, and monitoring tools may think your app is dead.
- Do not make expensive calls on every check. Health endpoints are hit every few seconds by load balancers and Kubernetes.
- Catch exceptions and return `DOWN`. Don't let them escape.

---

## Q6. What are liveness and readiness probes? How are they different?

This is a **very common** question in Kubernetes-era interviews.

| Probe | Question it answers | If it fails, Kubernetes... |
|---|---|---|
| **Liveness** | "Is the app **alive**, or stuck and broken?" | **Restarts** the container |
| **Readiness** | "Is the app **ready to take traffic** right now?" | **Stops sending** requests to it (no restart) |

Spring Boot supports both:

```
/actuator/health/liveness
/actuator/health/readiness
```

They turn on automatically when Boot detects Kubernetes. You can also force them:

```properties
management.endpoint.health.probes.enabled=true
```

**Real example:**
- The app is starting and loading a cache. It's **alive** but **not ready**. Kubernetes should wait, not restart it.
- During shutdown, the app finishes its current requests. It reports **not ready**, so no new traffic arrives.
- A deadlock has made the app permanently stuck. It should be **restarted**, so liveness fails.

**Big mistake to avoid:** Putting the **database check inside liveness**. If the database goes down, every pod fails liveness, and Kubernetes restarts **all** of them. A restart doesn't fix the database, and you get a "restart storm" that makes the outage worse. Liveness should only reflect the app's **own internal state**. Dependencies belong (carefully) in readiness, or in normal monitoring.

You can choose which indicators belong to which group with **health groups**:

```properties
management.endpoint.health.group.readiness.include=readinessState,db
```

---

## Q7. What is Micrometer?

**Short answer:** Micrometer is a **metrics facade**: one API that you code against, which can send data to many monitoring systems.

**Analogy:** **SLF4J** does this for logging. You write `log.info(...)`, and the actual logging library (Logback, Log4j2) sits behind it. **Micrometer is "SLF4J for metrics."** You write against `MeterRegistry`, and Prometheus, Datadog, New Relic, CloudWatch or others sit behind it.

**Why it matters:** You are not locked to one vendor. Switching from Prometheus to Datadog means changing a **dependency**, not your code.

Boot auto-configures a `MeterRegistry` bean and records many useful metrics without any work from you:

- JVM: memory, garbage collection, threads
- System: CPU, uptime
- HTTP server: `http.server.requests` (count, time, by URL and status)
- Database pool: `hikaricp.connections.active`
- Tomcat, caches, Kafka, and more

Look at them at `/actuator/metrics`, and drill in with `/actuator/metrics/http.server.requests`.

---

## Q8. What are the main meter types in Micrometer?

| Type | Meaning | Example |
|---|---|---|
| **Counter** | Only goes **up** | Orders created, errors, logins |
| **Gauge** | Goes **up and down**, a current value | Queue size, active users, cache size |
| **Timer** | Measures **how long** something takes, and how often | Payment call duration |
| **DistributionSummary** | Like a timer, but for **sizes**, not time | Request payload size |
| **LongTaskTimer** | Tracks tasks that are **still running** | A long batch job |

**Custom metrics example:**

```java
@Service
public class OrderService {

    private final Counter ordersCreated;
    private final Timer paymentTimer;

    public OrderService(MeterRegistry registry) {
        this.ordersCreated = Counter.builder("orders.created")
                .description("Number of orders created")
                .tag("channel", "web")
                .register(registry);

        this.paymentTimer = Timer.builder("payment.duration")
                .register(registry);
    }

    public void placeOrder(Order order) {
        paymentTimer.record(() -> paymentService.charge(order));   // measures time
        ordersCreated.increment();                                  // counts
    }
}
```

**Gauge example:**
```java
Gauge.builder("jobs.queue.size", queue, Queue::size).register(registry);
```
A gauge reads the value **when it is scraped**. It holds only a **weak reference** to the object, so keep that object alive somewhere, or the gauge will disappear.

**Annotations:** `@Timed` on a method times it (you must register a `TimedAspect` bean). `@Observed` is the newer, more general one (Q11).

---

## Q9. What are tags? What is "high cardinality"?

**Tags** (also called labels or dimensions) add detail to a metric, so you can slice it:

```
http.server.requests{method="GET", uri="/users/{id}", status="200"}
```

Now you can ask "how many 500 errors on `/orders`?"

**High cardinality** happens when a tag can have **too many different values**. Each unique combination creates a **separate time series** stored in memory (in your app and in Prometheus).

**Bad idea:**
```java
counter.tag("userId", user.getId());        // a million users = a million time series
counter.tag("uri", "/users/123");           // every ID becomes a new series
```

**Good:**
```java
counter.tag("status", "200");               // few possible values
counter.tag("uri", "/users/{id}");          // the URL template, not the real path
```

Note that Spring MVC already records the **template** (`/users/{id}`), not the real URL, exactly to avoid this problem.

**Rule:** Tags must have a **small, limited set of values**. Never use user IDs, emails, order IDs, timestamps or free text. Those belong in **logs or traces**, not metrics. Ignoring this is one of the most common ways to crash a monitoring system.

---

## Q10. How do Prometheus and Grafana fit in?

The usual setup has three pieces:

```
Your App  ←(scrapes every 15s)←  Prometheus  →  Grafana (dashboards + alerts)
```

**Step 1: Add the Prometheus registry**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Step 2: Expose the endpoint**
```properties
management.endpoints.web.exposure.include=health,prometheus
```

Now `/actuator/prometheus` returns text like:
```
orders_created_total{channel="web"} 152.0
jvm_memory_used_bytes{area="heap"} 1.2E8
```
Micrometer changes names to the Prometheus style: dots become underscores, and counters get `_total`.

**Step 3: Prometheus scrapes it** (a config file lists your app's address). **Step 4: Grafana** reads from Prometheus and draws graphs.

**Pull vs push:** Prometheus **pulls** (scrapes) metrics from your app. Some systems like Datadog or InfluxDB can have the app **push** data. Pull makes it easy to see if an app is down: the scrape fails.

**Percentiles matter more than averages:**
```properties
management.metrics.distribution.percentiles-histogram.http.server.requests=true
```
An **average** of 100 ms can hide the fact that 1% of users wait 5 seconds. Watch the **p95 and p99** (95% and 99% of requests are faster than this value). This setting lets Prometheus calculate percentiles across many instances.

---

## Q11. What are the three pillars of observability?

| Pillar | Answers | Tool examples |
|---|---|---|
| **Logs** | **What happened** in detail? | Logback, ELK, Loki |
| **Metrics** | **How much / how often / how fast?** (numbers over time) | Micrometer, Prometheus, Grafana |
| **Traces** | **Where** did one request spend its time across services? | Micrometer Tracing, OpenTelemetry, Zipkin, Jaeger |

**How they work together in a real problem:**
1. A **metric** alert fires: "p99 latency for `/checkout` is 6 seconds."
2. A **trace** shows one slow request: 5 seconds were spent in the payment service.
3. The **logs** for that trace ID show a timeout to the bank's API.

Each one alone is not enough. Metrics tell you **something is wrong**, traces tell you **where**, and logs tell you **why**.

**Observation API (Boot 3):** Spring Framework 6 has an **Observation API**. You instrument once, and it can produce **both metrics and traces**. `@Observed` on a method (with an `ObservationAspect` bean) is the annotation for it.

---

## Q12. How does logging work in Spring Boot?

- **SLF4J** is the API (facade) you code against. **Logback** is the default implementation. (You can switch to Log4j2.)
- The starter includes them, so no setup is needed.

```java
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public void place(Long id) {
        log.info("Placing order {}", id);                // parameterized: good
        // log.info("Placing order " + id);               // string concatenation: avoid
    }
}
```
(With Lombok, `@Slf4j` creates the `log` field.)

**Why `{}` placeholders?** The message string is built **only if** that log level is enabled. With `+`, the string is built every time, even if the message is never printed.

**Log levels (low to high):** `TRACE` < `DEBUG` < `INFO` < `WARN` < `ERROR`. The default level is **INFO**. Setting a level shows that level **and everything above it**.

**Configuration:**
```properties
logging.level.root=WARN
logging.level.com.example.orders=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.file.name=logs/app.log
```

**Change level at runtime with Actuator (no restart):**
```
POST /actuator/loggers/com.example.orders
{ "configuredLevel": "DEBUG" }
```
This is very useful for debugging a production issue for a few minutes, then setting it back. Secure this endpoint well, as it is a write operation.

**Good practices:**
- Never use `System.out.println`. It has no levels, no timestamps, and no control.
- **Never log secrets**: passwords, tokens, card numbers, personal data.
- Log **once** at the right place (see Concept 7, Q18). Don't log and rethrow at every layer.
- Use `ERROR` for real failures that need attention. Do not use `ERROR` for normal things like "user typed a wrong password."
- In containers, log to **console (stdout)**, and let the platform collect logs. Don't manage log files inside containers.

---

## Q13. What is structured logging? What is MDC?

**Plain text logs** are hard for machines to search. **Structured logs** are written as **JSON**, so tools like ELK or Loki can filter by field:

```json
{"@timestamp":"2026-09-30T10:15:30Z","level":"ERROR","message":"Payment failed","traceId":"a1b2c3","orderId":"981"}
```

**Spring Boot 3.4+ has built-in support:**
```properties
logging.structured.format.console=ecs
```
Supported formats include `ecs` (Elastic Common Schema), `logstash`, and `gelf`. Before 3.4, you used the `logstash-logback-encoder` library.

**MDC (Mapped Diagnostic Context):** A small map, stored per thread, that adds extra fields to **every** log line in that request.

```java
MDC.put("userId", user.getId());
try {
    // every log line here includes userId
} finally {
    MDC.clear();       // important: threads are reused in pools
}
```
Forgetting `MDC.clear()` leaks one user's ID into another request's logs, because thread pools **reuse threads**. Like `SecurityContext` (Concept 8, Q19), MDC is `ThreadLocal`, so it is **not passed automatically** to new threads or `@Async` tasks.

---

## Q14. What is distributed tracing? How does Boot 3 support it?

**The problem:** In microservices, one user click may pass through 6 services. When it is slow, which service caused it? Looking at 6 sets of logs is painful.

**The solution:** Give each request a **trace ID** at the entry point, and pass it along to every service.

- **Trace:** The whole journey of one request. It has one **traceId**.
- **Span:** One step inside the journey (for example, "call payment service" or "run SQL query"). Each has a **spanId** and a start and end time.

```
Trace 4bf92f...
 └─ API Gateway            (120 ms)
     └─ Order Service      (110 ms)
         ├─ DB query        (10 ms)
         └─ Payment Service (90 ms)   ← the slow one
```

**Propagation:** The trace ID travels in an HTTP header (the standard **W3C `traceparent`** header), so the next service continues the same trace.

**In Boot 3:** **Spring Cloud Sleuth is gone.** It is replaced by **Micrometer Tracing**. A common setup is:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```
```properties
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
```

**Key details:**
- **Sampling:** The default is `0.1` (10% of requests are traced). Use `1.0` in dev, and a lower number on busy production systems to save cost.
- Trace ID and span ID are added to your logs automatically (through MDC), so you can search logs by trace ID. To search a trace ID across all services, use it as the join key.
- For propagation to work, create HTTP clients from the **Boot-provided builders** (`RestClient.Builder`, `RestTemplateBuilder`, `WebClient.Builder`), not with `new RestTemplate()`. Otherwise Boot cannot add its tracing hooks.
- **OpenTelemetry (OTel)** is the industry standard for traces (and increasingly metrics and logs). Backends like Zipkin, Jaeger, Tempo, and most commercial tools accept it.

---

## Q15. What do `/info`, build info and Git info give you?

The `info` endpoint answers "**which version is running?**", very useful during incidents.

```properties
management.endpoints.web.exposure.include=health,info
management.info.env.enabled=true
info.app.name=Order Service
info.app.owner=payments-team
```

Add build and Git details automatically with the Maven plugin:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>build-info</goal></goals>
        </execution>
    </executions>
</plugin>
```
Plus the `git-commit-id-maven-plugin`, and then `/actuator/info` shows the **version, build time and Git commit**.

**Tip:** Since Boot 2.6, `info.*` properties are **not shown by default**. You need `management.info.env.enabled=true`. This surprises many people.

---

## Q16. How do you secure Actuator endpoints?

Actuator endpoints can show **very sensitive information**. Treat them like an admin panel.

| Endpoint | Risk |
|---|---|
| `/heapdump` | Full memory dump. Can contain **passwords, tokens and user data**. |
| `/env`, `/configprops` | Configuration values, and possibly secrets. |
| `/beans`, `/mappings` | Map of your whole app for an attacker. |
| `/loggers` (POST) | Someone could turn on DEBUG or change behavior. |
| `/shutdown` | Anyone can stop your app. |
| `/threaddump` | Internal details. |

**Protection layers (use several together):**

**1. Expose only what you need**
```properties
management.endpoints.web.exposure.include=health,info,prometheus
```

**2. Use a separate management port**
```properties
management.server.port=9001
```
Then only your internal network can reach port 9001. The public load balancer only exposes port 8080. This is one of the **most effective** protections, and a common production setup.

**3. Add Spring Security rules** using `EndpointRequest`:
```java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("OPS")
    .anyRequest().authenticated());
```
Rules are checked top to bottom, and the first match wins (Concept 8, Q6).

**4. Sanitized values:** In Boot 3, values in `/env` and `/configprops` are **hidden by default**. You can choose with `management.endpoint.env.show-values` (`never`, `when-authorized`, `always`).

**5. Keep details private:** `health.show-details=when-authorized`.

**A real-world lesson:** Many public breaches came from exposed `/actuator/env` and `/actuator/heapdump` endpoints. This is a favorite security question, so mention it.

---

## Q17. How do you create a custom Actuator endpoint?

Use `@Endpoint` with operation annotations:

```java
@Component
@Endpoint(id = "features")
public class FeatureFlagsEndpoint {

    private final Map<String, Boolean> flags = new ConcurrentHashMap<>();

    @ReadOperation                                        // GET /actuator/features
    public Map<String, Boolean> all() {
        return flags;
    }

    @ReadOperation                                        // GET /actuator/features/{name}
    public Boolean one(@Selector String name) {
        return flags.get(name);
    }

    @WriteOperation                                       // POST /actuator/features/{name}
    public void set(@Selector String name, boolean enabled) {
        flags.put(name, enabled);
    }

    @DeleteOperation                                      // DELETE /actuator/features/{name}
    public void remove(@Selector String name) {
        flags.remove(name);
    }
}
```

Don't forget to **expose** it: `management.endpoints.web.exposure.include=features`.

- `@Endpoint` works over both **HTTP and JMX**.
- `@WebEndpoint` works over HTTP only.
- `@ReadOperation` = GET, `@WriteOperation` = POST, `@DeleteOperation` = DELETE.

This is handy for runtime controls like feature flags, cache clearing, or a "drain this instance" switch. Secure it like any other management endpoint.

---

## Q18. How would you find why a Spring Boot app is slow in production?

Interviewers love this scenario question. A good answer shows a **method**, not just a list of tools.

**Step 1: Metrics first (what is slow?).**
Check `http.server.requests` by URL. Look at **p95/p99 latency**, not just averages. Find which endpoint or which time window is bad.

**Step 2: Check the usual suspects with metrics.**
- **JVM memory and GC:** high `jvm.gc.pause`, heap always near full → memory pressure or leak.
- **Thread pool:** `tomcat.threads.busy` at the max → requests are waiting for a thread.
- **DB pool:** `hikaricp.connections.pending` above 0 or `active` at the max → requests are waiting for a connection. This is a very common cause.
- **CPU:** `system.cpu.usage` and `process.cpu.usage`.

**Step 3: Traces (where is the time going?).**
Open a slow request's trace. Is it a slow SQL query? A slow downstream service? Time spent in your own code?

**Step 4: Logs (why?).**
Search by the trace ID. Turn on `DEBUG` for a specific package **temporarily** using `/actuator/loggers`. Check SQL logs for repeated queries (the **N+1 problem** from Concept 6).

**Step 5: Go deeper if needed.**
- `/actuator/threaddump`: many threads `BLOCKED` or waiting on the same lock or connection points to a **deadlock or contention**. Take two or three dumps a few seconds apart and compare.
- **Heap dump** (with care, it's sensitive and large) and a tool like Eclipse MAT for memory leaks.
- **Java Flight Recorder (JFR)** for low-overhead production profiling.

**Step 6: Fix and confirm.** After the fix, check that the metric really improved.

---

## Q19. (Advanced) How do alerts and SLOs relate to all this?

Metrics are only useful if someone **looks** at them. Nobody watches a dashboard all day, so we set **alerts**.

**Popular checklists for what to watch:**

**The Four Golden Signals (Google SRE):**
1. **Latency:** how long requests take
2. **Traffic:** how many requests
3. **Errors:** how many fail
4. **Saturation:** how full the resources are (CPU, memory, threads, DB connections)

**RED method** (for services): **R**ate, **E**rrors, **D**uration.
**USE method** (for resources): **U**tilization, **S**aturation, **E**rrors.

**Good alert habits:**
- Alert on **symptoms users feel** (high error rate, high latency), not on every small cause (one CPU spike).
- Every alert should need **a human action**. Too many noisy alerts lead to **alert fatigue**, and people start ignoring them.
- **SLO (Service Level Objective):** a target such as "99.9% of requests succeed, and 95% finish within 300 ms." **SLI** is the measured number. **SLA** is a promise to customers, often with penalties. Alerts are often based on how fast you are using up your **error budget**.

---

## Q20. (Advanced) What are common mistakes with Actuator and monitoring?

- **`include=*` in production**, exposing `env`, `heapdump` and `beans` to everyone.
- **Actuator on the public port** without authentication.
- **DB or external service checks in the liveness probe**, which causes restart storms.
- **Slow or heavy custom health checks** that are called every few seconds.
- **High-cardinality tags** (user ID, order ID) that overload Prometheus.
- **Only watching averages.** Percentiles show the real user experience.
- **Trace sampling at 100% on a very busy system**, which is expensive. Or the opposite: never turning tracing on.
- **Logging secrets or personal data**, and logging at `DEBUG` in production permanently.
- **No correlation ID** in logs, so nobody can follow one request across services.
- **`new RestTemplate()` instead of the builder**, so trace propagation silently breaks.
- **Forgetting `MDC.clear()`** in thread pools.
- **Measuring nothing about business** (orders per minute, failed payments). Technical metrics can be green while the business is broken. Add **business metrics** too.
- **Monitoring that goes down with the app.** Keep Prometheus, Grafana and alerting **outside** the app you are watching.

---

## Quick Recap

- **Actuator** = production-ready endpoints (health, metrics, info, loggers, and more). Only `health` is exposed over HTTP by default.
- **Enabled** means the endpoint exists. **Exposed** means it can be reached.
- **Health** combines many `HealthIndicator`s, and the **worst status wins**. `DOWN` returns **503**. Write custom indicators, and keep them fast.
- **Liveness** = "restart me if broken." **Readiness** = "send me traffic only when ready." **Don't put the database in liveness.**
- **Micrometer** = "SLF4J for metrics." Types: Counter, Gauge, Timer, DistributionSummary.
- Keep tags **low-cardinality**. Never use IDs or free text.
- **Prometheus scrapes** `/actuator/prometheus`, and **Grafana** draws dashboards. Watch **p95/p99**, not just averages.
- **Three pillars:** logs (why), metrics (how much), traces (where).
- Use **SLF4J with `{}` placeholders**, meaningful levels, JSON structured logs (Boot 3.4+), and never log secrets. Change levels at runtime with `/actuator/loggers`.
- **MDC** adds fields to logs, is per-thread, and needs `clear()`.
- **Boot 3 tracing = Micrometer Tracing + OpenTelemetry** (Sleuth is gone). Trace IDs travel in the `traceparent` header. Use Boot's client builders.
- **Secure Actuator:** expose the minimum, use a **separate management port**, add security rules, and never expose `heapdump` or `env` publicly.
- Custom endpoints: `@Endpoint` with `@ReadOperation`, `@WriteOperation`, `@DeleteOperation`.
- For a slow app: **metrics → traces → logs → thread dump / heap dump / JFR.**
- Alert on **symptoms** using the golden signals, and avoid alert fatigue.

---

Reply **"next"** for **Concept 10: Testing**. That covers unit vs integration tests, `@SpringBootTest`, test slices (`@WebMvcTest`, `@DataJpaTest`), Mockito, `MockMvc`, Testcontainers, security testing and more. Or tell me if you want any part of this concept explained again.

---
---
---

