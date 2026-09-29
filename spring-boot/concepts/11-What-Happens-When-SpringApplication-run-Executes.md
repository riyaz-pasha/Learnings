# Topic 11 — What Happens When `SpringApplication.run()` Executes?

This is one of the **best Spring Boot interview questions** because a good answer lets you connect almost everything we've learned so far:

```text
Spring Boot
   ↓
SpringApplication
   ↓
Environment
   ↓
ApplicationContext
   ↓
BeanDefinitions
   ↓
Component scanning
   ↓
Auto-configuration
   ↓
Bean creation
   ↓
Dependency Injection
   ↓
Bean lifecycle
   ↓
Embedded Web Server
   ↓
Runners
   ↓
ApplicationReadyEvent
```

The official Spring Boot API describes `SpringApplication` as the class that bootstraps and launches a Spring application and, at a high level, creates an appropriate `ApplicationContext`, exposes command-line arguments as properties, refreshes the context, and invokes runners. ([Home][1])

Let's understand what actually happens.

---

# 1. The Code We Usually Start With

Every Spring Boot application usually begins with something like:

```java
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                OrderApplication.class,
                args
        );
    }
}
```

The important line is:

```java
SpringApplication.run(OrderApplication.class, args);
```

A beginner sees this as:

```text
"Start Spring Boot"
```

That's true, but not useful enough for an interview.

We want to understand the sequence behind it.

---

# 2. First Understand the Two Objects

There are two major players:

```text
SpringApplication
       |
       ↓
ApplicationContext
```

### `SpringApplication`

Responsible for **bootstrapping** the application.

Think:

```text
prepare environment
create context
configure context
load sources
refresh context
run startup callbacks
```

### `ApplicationContext`

The actual Spring IoC container that manages:

```text
beans
dependencies
lifecycle
events
resources
etc.
```

So:

```text
SpringApplication
      ↓
starts/configures
      ↓
ApplicationContext
      ↓
runs application
```

The `SpringApplication` API explicitly provides methods for creating, preparing, refreshing, and customizing the application context. ([Home][1])

---

# 3. What Does `SpringApplication.run()` Return?

This is another nice interview question.

It returns:

```java
ConfigurableApplicationContext
```

So you can technically do:

```java
ConfigurableApplicationContext context =
        SpringApplication.run(
                OrderApplication.class,
                args
        );
```

The returned context represents the running application context. ([Home][1])

Usually you don't need the reference in your `main()` method, which is why most applications simply call:

```java
SpringApplication.run(...);
```

---

# 4. High-Level Startup Flow

Before going into details, memorize this:

```text
main()
  ↓
SpringApplication.run()
  ↓
SpringApplication startup
  ↓
prepare Environment
  ↓
create ApplicationContext
  ↓
prepare/configure Context
  ↓
load BeanDefinitions
  ↓
refresh Context
  ↓
instantiate/configure singleton beans
  ↓
embedded web server becomes ready
  ↓
ApplicationStartedEvent
  ↓
CommandLineRunner / ApplicationRunner
  ↓
ApplicationReadyEvent
  ↓
application ready
```

The official Boot lifecycle events document this ordering in detail. ([Home][2])

---

# 5. Step 1 — `main()` Executes

The JVM starts your application:

```java
public static void main(String[] args)
```

At this point:

```text
JVM
 ↓
main()
```

Nothing Spring-specific has happened yet except loading your application classes.

Then:

```java
SpringApplication.run(...)
```

is called.

---

# 6. Step 2 — SpringApplication Is Bootstrapped

Conceptually:

```java
SpringApplication application =
        new SpringApplication(OrderApplication.class);
```

followed by:

```java
application.run(args);
```

The convenience static method essentially combines these steps for you. The `SpringApplication` API documents both the static `run(...)` helper and explicit construction/customization. ([Home][1])

You can customize it:

```java
SpringApplication application =
        new SpringApplication(OrderApplication.class);

application.setWebApplicationType(
        WebApplicationType.NONE
);

application.run(args);
```

This is occasionally useful for command-line applications and tests.

---

# 7. `ApplicationStartingEvent`

Very early in startup, Spring Boot publishes:

```text
ApplicationStartingEvent
```

This happens before the `Environment` or `ApplicationContext` is available, other than early listener/initializer registration. ([Home][2])

Conceptually:

```text
SpringApplication.run()
       ↓
ApplicationStartingEvent
```

This is an important event to know because it demonstrates that the application lifecycle starts **before the ApplicationContext exists**.

---

# 8. Why Does That Matter?

Suppose someone says:

> "I'll create a normal `@Bean` listener for every Spring Boot startup event."

Not quite.

Some events occur **before the context exists**, so they cannot be registered as ordinary beans in that context.

The Boot documentation explicitly calls this out for early events such as `ApplicationStartingEvent` and `ApplicationEnvironmentPreparedEvent`. ([Home][2])

That leads to special listener registration mechanisms.

---

# 9. Step 3 — Environment Is Prepared

Next, Boot prepares the application `Environment`.

This is where the things we learned in Topic 10 become relevant:

```text
application.yaml
application.properties
profile-specific config
environment variables
system properties
command-line arguments
config imports
```

The `ApplicationEnvironmentPreparedEvent` is published when the `Environment` is available for inspection/modification but before the application context is created. ([Home][3])

Conceptually:

```text
Property Sources
       ↓
Environment
       ↓
active profiles
       ↓
application configuration
```

---

# 10. Command-Line Arguments Enter the Environment

Suppose you start:

```bash
java -jar order-service.jar \
    --server.port=8085
```

Boot registers a `CommandLinePropertySource`, exposing command-line arguments as Spring properties. The `SpringApplication` API explicitly lists this as one of its bootstrap steps. ([Home][1])

So:

```text
--server.port=8085
        ↓
CommandLinePropertySource
        ↓
Environment
        ↓
server.port
```

This connects directly to the property-precedence discussion from Topic 10.

---

# 11. Profiles Are Determined During Configuration Processing

Suppose:

```bash
java -jar app.jar --spring.profiles.active=prod
```

Boot sees:

```text
spring.profiles.active=prod
```

and configures the environment appropriately.

So:

```text
command line
    ↓
Environment
    ↓
active profile = prod
    ↓
application-prod.yaml
```

The exact configuration-data processing is more involved, but the important interview concept is:

> **Environment and profile setup happens very early, before the ApplicationContext is created.**

This is why early configuration can influence which context and beans get created.

---

# 12. Step 4 — Determine Web Application Type

Spring Boot tries to determine what kind of application you're running.

Current Boot supports:

```text
SERVLET
REACTIVE
NONE
```

The `WebApplicationType` is determined from the classpath unless explicitly configured. ([Home][4])

The current Boot algorithm is:

```text
Spring MVC present?
    ↓ yes
SERVLET

otherwise WebFlux present?
    ↓ yes
REACTIVE

otherwise
NONE
```

The official Boot documentation states that MVC takes precedence when both MVC and WebFlux are present. ([Home][5])

---

# 13. Why Does Boot Care About Web Application Type?

Because it must know which `ApplicationContext` to create.

For example:

```text
SERVLET
   ↓
servlet web ApplicationContext

REACTIVE
   ↓
reactive web ApplicationContext

NONE
   ↓
non-web ApplicationContext
```

Boot's API describes `WebApplicationType.NONE` as an application that should not run as a web application or start an embedded server. ([Home][6])

---

# 14. Example

### REST application

You have:

```xml
spring-boot-starter-web
```

Boot sees:

```text
Spring MVC
+
servlet web infrastructure
```

and creates a servlet web application context.

### CLI application

Suppose:

```text
no web dependencies
```

Boot can use:

```text
WebApplicationType.NONE
```

and create a normal application context without a web server.

---

# 15. Can We Override It?

Yes.

```java
SpringApplication application =
        new SpringApplication(OrderApplication.class);

application.setWebApplicationType(
        WebApplicationType.NONE
);

application.run(args);
```

Boot explicitly provides `setWebApplicationType(...)`. ([Home][1])

You can also use:

```properties
spring.main.web-application-type=none
```

The `SpringApplication` API documents this property as one of the application properties that can configure the bootstrapping behavior. ([Home][4])

---

# 16. Step 5 — Create the ApplicationContext

Once Boot has enough information about the environment and application type, it creates the appropriate context.

Conceptually:

```text
WebApplicationType
       ↓
ApplicationContextFactory
       ↓
appropriate ApplicationContext
```

The `SpringApplication` API explicitly describes `createApplicationContext()` as the strategy used to create the context, falling back to an appropriate default based on application type. ([Home][1])

---

# 17. Example Context Types

Depending on application type, Boot can create contexts such as:

```text
AnnotationConfigApplicationContext
```

for non-web applications,

or servlet/reactive web application contexts for web applications. The current Boot documentation describes these choices explicitly. ([Home][5])

You don't need to memorize every implementation class.

For interviews, know the concept:

> **Boot chooses an ApplicationContext appropriate for the application's web type.**

---

# 18. `ApplicationContextInitializedEvent`

After the context is created and prepared, Boot publishes:

```text
ApplicationContextInitializedEvent
```

This event occurs after `ApplicationContextInitializers` have been called but **before bean definitions are loaded**. ([Home][7])

This is a useful event because it marks a very specific point in startup:

```text
Environment ready
      ↓
ApplicationContext exists
      ↓
Initializers execute
      ↓
ContextInitializedEvent
      ↓
bean definitions not loaded yet
```

---

# 19. What Is an `ApplicationContextInitializer`?

This is another useful interview concept.

An initializer can customize the ApplicationContext **before it is refreshed**.

Conceptually:

```text
ApplicationContext created
        ↓
ApplicationContextInitializer
        ↓
custom context configuration
        ↓
bean definition loading
```

Spring Boot's `SpringApplication` API explicitly supports registering `ApplicationContextInitializer`s and applying them before context refresh. ([Home][1])

This is different from a:

```text
BeanPostProcessor
```

because the initializer operates on the **context itself before normal bean creation**, whereas `BeanPostProcessor` operates on beans.

---

# 20. Step 6 — Load Sources / Bean Definitions

Now Boot loads the application sources.

Your source is:

```java
@SpringBootApplication
public class OrderApplication {
}
```

Because `@SpringBootApplication` combines:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

your application configuration participates in:

```text
component scanning
+
auto-configuration
```

The current Boot API confirms this composition. ([Home][8])

---

# 21. Component Scanning Happens

Your classes:

```java
@Service
public class OrderService {}

@Repository
public class OrderRepository {}

@RestController
public class OrderController {}
```

are discovered through component scanning.

Conceptually:

```text
@ComponentScan
       ↓
configured package
       ↓
find candidate components
       ↓
register BeanDefinitions
```

This is the topic we covered earlier.

---

# 22. Auto-Configuration Is Also Brought In

At the same stage, the `@EnableAutoConfiguration` part of `@SpringBootApplication` causes Boot's auto-configuration selection mechanism to consider the relevant auto-configurations.

So now:

```text
Your configuration
        +
component scanning
        +
auto-configuration
        ↓
BeanDefinitions
```

The exact ordering and implementation involve Spring's configuration-class processing and import infrastructure.

---

# 23. Very Important: No Actual Beans Yet?

At this stage, we should carefully distinguish:

```text
BeanDefinition registration
```

from:

```text
bean instance creation
```

The framework has now accumulated the metadata describing what beans are available.

Think:

```text
BeanDefinition
"OrderService exists"

BeanDefinition
"DataSource exists"

BeanDefinition
"PaymentService exists"
```

This is not identical to saying every object has already been instantiated.

Bean instantiation happens as part of context refresh, subject to scope/lazy behavior.

---

# 24. `ApplicationPreparedEvent`

Once the context has been prepared and bean definitions are loaded, but **before refresh starts**, Boot publishes:

```text
ApplicationPreparedEvent
```

The official API describes the context at this point as fully prepared but not yet refreshed; bean definitions have been loaded and the `Environment` is ready. ([Home][9])

So:

```text
Environment
   ↓
Context created
   ↓
Initializers
   ↓
Bean definitions loaded
   ↓
ApplicationPreparedEvent
   ↓
Context refresh
```

This event boundary is extremely useful when explaining startup internals.

---

# 25. Step 7 — `ApplicationContext.refresh()`

Now we reach one of the most important operations in Spring:

```java
context.refresh();
```

`SpringApplication` explicitly provides a `refresh(...)` method for refreshing the underlying application context. ([Home][1])

This is where much of the actual Spring machinery kicks into gear.

Think:

```text
refresh()
   ↓
prepare BeanFactory
   ↓
load/process BeanDefinitions
   ↓
register BeanPostProcessors
   ↓
initialize infrastructure
   ↓
instantiate non-lazy singleton beans
   ↓
initialize them
   ↓
finish refresh
```

The exact internal sequence is substantial, but these are the major ideas.

---

# 26. Why Is `refresh()` So Important?

Because this is where the context transitions from:

```text
"configuration metadata loaded"
```

to:

```text
"fully initialized Spring container"
```

This is where the beans we studied earlier are actually created and initialized.

---

# 27. BeanFactory Gets Prepared

The `ApplicationContext` has a bean factory underneath it.

During refresh, Spring configures the bean factory with things such as:

```text
class loader
expression resolver
property editors/conversion
registered dependencies
post-processors
```

This prepares the factory to create and manage beans.

You don't need to memorize every internal method for an EPAM interview.

Know:

> **Context refresh prepares the underlying BeanFactory and then performs bean creation/initialization.**

---

# 28. `BeanFactoryPostProcessor`

Remember this from Topic 3?

Now it becomes relevant.

Before ordinary beans are instantiated, Spring can run:

```text
BeanFactoryPostProcessor
```

These operate on **bean definitions / container metadata**.

Conceptually:

```text
BeanDefinitions
      ↓
BeanFactoryPostProcessor
      ↓
modified/processed metadata
      ↓
bean creation
```

This is why:

```text
BeanFactoryPostProcessor
```

is different from:

```text
BeanPostProcessor
```

---

# 29. `BeanPostProcessor` Gets Registered

Now Spring registers the `BeanPostProcessor`s that will participate in bean creation.

Examples include infrastructure responsible for processing:

```text
@Autowired
@PostConstruct
AOP/proxies
```

depending on which features are enabled.

Then actual beans can pass through the post-processing chain during creation.

---

# 30. Singleton Beans Are Usually Pre-Instantiated

This is one of the most important refresh behaviors.

By default:

```text
ApplicationContext refresh
       ↓
pre-instantiate non-lazy singleton beans
```

Spring's documentation explicitly states that `ApplicationContext` implementations eagerly create and configure singleton beans during initialization by default. ([Home][10])

That's why many startup errors happen **during application startup**, not on the first request.

---

# 31. Example

Suppose:

```java
@Service
public class PaymentService {

    public PaymentService(PaymentGateway gateway) {
        // ...
    }
}
```

If there is no `PaymentGateway` bean:

```text
Context refresh
      ↓
create PaymentService
      ↓
need PaymentGateway
      ↓
no candidate
      ↓
startup failure
```

So you may see errors such as:

```text
NoSuchBeanDefinitionException
NoUniqueBeanDefinitionException
```

during startup.

---

# 32. Bean Creation Happens Here

This brings together Topics 2–6:

```text
BeanDefinition
      ↓
instantiate
      ↓
dependency resolution
      ↓
Dependency Injection
      ↓
Aware callbacks
      ↓
BeanPostProcessor
      ↓
@PostConstruct
      ↓
afterPropertiesSet()
      ↓
custom init
      ↓
BeanPostProcessor
      ↓
ready bean
```

That's why the startup lifecycle topic is so useful: **all your previous topics converge here**.

---

# 33. Lazy Beans Are an Exception

Suppose:

```java
@Lazy
@Service
public class ExpensiveService {
}
```

Then it normally isn't pre-instantiated during startup.

Instead:

```text
refresh()
   ↓
ExpensiveService skipped
   ↓
first request/getBean()
   ↓
create ExpensiveService
```

Spring's documentation explicitly describes this behavior. ([Home][10])

But remember the earlier caveat:

```text
lazy bean
   +
non-lazy singleton depends on it
```

may still result in creation during startup because the singleton's dependency has to be satisfied. ([Home][10])

---

# 34. Embedded Web Server

For a web application, the startup process also creates and starts the embedded web server.

Conceptually:

```text
ApplicationContext
      ↓
Web server factory
      ↓
create WebServer
      ↓
start server
```

The actual implementation depends on the chosen web stack:

```text
Servlet
    → embedded servlet server

Reactive
    → embedded reactive server
```

Boot's current web infrastructure supports servlet and reactive server integrations including Tomcat, Jetty, and Reactor Netty. ([Home][11])

---

# 35. `WebServerInitializedEvent`

After the web server is ready, Spring publishes:

```text
WebServerInitializedEvent
```

with servlet and reactive variants.

The Spring Boot lifecycle documentation explicitly places this event after the web server is ready and before `ApplicationStartedEvent`. ([Home][2])

So:

```text
create beans
   ↓
start web server
   ↓
WebServerInitializedEvent
```

---

# 36. `ContextRefreshedEvent`

During context refresh, Spring publishes:

```text
ContextRefreshedEvent
```

when the application context is initialized/refreshed. ([Home][12])

So:

```text
context.refresh()
       ↓
ContextRefreshedEvent
```

This is a Spring Framework event, not specifically a Spring Boot `SpringApplicationEvent`.

That's a useful distinction.

---

# 37. `ApplicationStartedEvent`

Once:

```text
ApplicationContext
```

has been refreshed, Boot publishes:

```text
ApplicationStartedEvent
```

At this point:

```text
context = refreshed
beans = initialized according to lifecycle rules
```

but:

```text
CommandLineRunner
ApplicationRunner
```

have **not yet been called**.

The Boot documentation explicitly defines `ApplicationStartedEvent` at this point. ([Home][2])

---

# 38. Why Is `ApplicationStartedEvent` Not the Same as `ApplicationReadyEvent`?

This is a very common interview question.

### `ApplicationStartedEvent`

```text
context refreshed
      +
application started
      ↓
but runners haven't executed yet
```

### `ApplicationReadyEvent`

```text
context refreshed
      +
runners executed
      ↓
application ready
```

The official event ordering makes this distinction explicit. ([Home][2])

---

# 39. Liveness State

After:

```text
ApplicationStartedEvent
```

Boot publishes an availability event indicating:

```text
LivenessState.CORRECT
```

The application is considered alive.

This comes before the final readiness state. ([Home][2])

Think:

```text
STARTED
  ↓
alive
```

but not necessarily yet:

```text
READY TO RECEIVE TRAFFIC
```

---

# 40. Step 8 — `CommandLineRunner` and `ApplicationRunner`

Now Boot executes startup runners.

You can implement:

```java
@Component
public class StartupRunner
        implements CommandLineRunner {

    @Override
    public void run(String... args) {

        System.out.println("Startup task");
    }
}
```

Or:

```java
@Component
public class StartupRunner
        implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {

        System.out.println("Startup task");
    }
}
```

Spring Boot defines these as callbacks that run as part of application startup. `ApplicationRunner` receives parsed `ApplicationArguments`. ([Home][13])

---

# 41. `CommandLineRunner` vs `ApplicationRunner`

This is another classic question.

### `CommandLineRunner`

```java
void run(String... args)
```

Gets raw command-line arguments.

### `ApplicationRunner`

```java
void run(ApplicationArguments args)
```

Gets parsed application arguments.

Conceptually:

```text
--name=orders
--debug
```

can be accessed more structurally through `ApplicationArguments`.

The two are both startup callback mechanisms.

---

# 42. When Do Runners Execute?

Important:

```text
ApplicationStartedEvent
      ↓
CommandLineRunner/ApplicationRunner
      ↓
ApplicationReadyEvent
```

That's explicitly documented by Spring Boot. ([Home][2])

So don't say:

> "`ApplicationReadyEvent` happens when the context is refreshed."

Technically that's too early.

---

# 43. Can We Order Runners?

Yes.

Multiple `ApplicationRunner` beans can be ordered using:

```java
@Order
```

or:

```text
Ordered
```

The Boot API explicitly documents that multiple `ApplicationRunner`s can be ordered this way. ([Home][13])

Same principle applies to `CommandLineRunner`.

---

# 44. What Should Runners Be Used For?

Good examples:

```text
seed data
startup validation
one-time startup tasks
cache warm-up
migration-related application setup
```

But don't use them for arbitrary work that should happen asynchronously after the application is ready.

Also, lengthy startup runners can delay:

```text
ApplicationReadyEvent
```

and therefore delay the point at which Boot declares the application ready.

---

# 45. Step 9 — `ApplicationReadyEvent`

After:

```text
CommandLineRunner
ApplicationRunner
```

finish, Boot publishes:

```text
ApplicationReadyEvent
```

The official docs describe this as occurring after application and command-line runners have been called. ([Home][2])

This is a major milestone:

```text
ApplicationReadyEvent
       ↓
application is considered ready
```

---

# 46. Readiness State

Immediately after `ApplicationReadyEvent`, Boot publishes:

```text
AvailabilityChangeEvent
```

with:

```text
ReadinessState.ACCEPTING_TRAFFIC
```

This marks the application as ready to service requests. ([Home][2])

So the lifecycle is:

```text
ApplicationStartedEvent
       ↓
Liveness = CORRECT
       ↓
runners execute
       ↓
ApplicationReadyEvent
       ↓
Readiness = ACCEPTING_TRAFFIC
```

This distinction becomes especially important in container orchestration/Kubernetes environments.

---

# 47. Startup Failure

What happens if something goes wrong?

For example:

```text
missing required bean
database initialization fails
invalid configuration
port already in use
```

Boot can publish:

```text
ApplicationFailedEvent
```

The official lifecycle documentation explicitly lists this as the failure event when startup throws an exception. ([Home][2])

Conceptually:

```text
startup
  ↓
exception
  ↓
ApplicationFailedEvent
  ↓
context cleanup / application exits
```

The exact cleanup depends on where the failure happened.

---

# 48. The Complete Boot Event Sequence

This is **very worth memorizing**.

According to current Spring Boot documentation:

```text
1. ApplicationStartingEvent
       ↓
2. ApplicationEnvironmentPreparedEvent
       ↓
3. ApplicationContextInitializedEvent
       ↓
4. ApplicationPreparedEvent
       ↓
5. Context refresh
       ↓
   WebServerInitializedEvent (web apps)
       ↓
   ContextRefreshedEvent
       ↓
6. ApplicationStartedEvent
       ↓
7. AvailabilityChangeEvent
   LivenessState.CORRECT
       ↓
8. ApplicationRunner /
   CommandLineRunner
       ↓
9. ApplicationReadyEvent
       ↓
10. AvailabilityChangeEvent
    ReadinessState.ACCEPTING_TRAFFIC
```

On failure:

```text
ApplicationFailedEvent
```

may be published. ([Home][2])

---

# 49. Don't Memorize Events Without Understanding the Stages

Think in **three major phases**.

## Phase A — Bootstrap

```text
Starting
Environment
Context creation
Context preparation
```

## Phase B — Spring Initialization

```text
BeanDefinitions
Context refresh
Bean creation
Dependency Injection
Bean lifecycle
Web server
```

## Phase C — Application Startup Completion

```text
ApplicationStartedEvent
       ↓
Runners
       ↓
ApplicationReadyEvent
```

This is much easier to remember than ten event names independently.

---

# 50. The Complete Startup Picture

Here's the diagram I'd want you to reproduce in an EPAM interview:

```text
                    main()
                       |
                       ↓
             SpringApplication.run()
                       |
                       ↓
             ApplicationStartingEvent
                       |
                       ↓
              Prepare Environment
                       |
                       ↓
        ApplicationEnvironmentPreparedEvent
                       |
                       ↓
             Determine Web Type
             SERVLET / REACTIVE / NONE
                       |
                       ↓
             Create ApplicationContext
                       |
                       ↓
        ApplicationContextInitializers
                       |
                       ↓
         ApplicationContextInitializedEvent
                       |
                       ↓
          Load Bean Definitions / Sources
                       |
          ┌────────────┴────────────┐
          ↓                         ↓
 Component Scanning          Auto-Configuration
          ↓                         ↓
       Your Beans              Boot Beans
          └────────────┬────────────┘
                       ↓
             ApplicationPreparedEvent
                       |
                       ↓
                context.refresh()
                       |
        ┌──────────────┼───────────────┐
        ↓              ↓               ↓
 BeanFactory      Bean Creation    Infrastructure
PostProcessors        |             / Web Server
                       ↓
                Dependency Injection
                       ↓
                Bean Lifecycle
                       ↓
            BeanPostProcessors / Proxies
                       ↓
             ContextRefreshedEvent
                       |
                       ↓
            WebServerInitializedEvent
                       |
                       ↓
             ApplicationStartedEvent
                       |
                       ↓
              Liveness = CORRECT
                       |
                       ↓
             Runners execute
          ┌────────────┴────────────┐
          ↓                         ↓
 CommandLineRunner          ApplicationRunner
          └────────────┬────────────┘
                       ↓
              ApplicationReadyEvent
                       |
                       ↓
          Readiness = ACCEPTING_TRAFFIC
```

That is the **big picture**.

---

# 51. What Happens to Your `@RestController`?

Let's make the process concrete.

You have:

```java
@RestController
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }
}
```

Startup:

```text
@ComponentScan
    ↓
find OrderController
    ↓
register BeanDefinition
    ↓
context.refresh()
    ↓
create OrderController
    ↓
needs OrderService
    ↓
resolve OrderService
    ↓
inject it
    ↓
initialize controller
```

Now the controller is part of the running context.

---

# 52. What Happens to `@Service`?

Same basic process:

```text
@Service
OrderService
```

becomes:

```text
BeanDefinition
    ↓
bean creation
    ↓
dependency injection
    ↓
initialization
    ↓
managed bean
```

If it has:

```java
@Transactional
```

there can additionally be proxy-related infrastructure involved.

We'll cover that later.

---

# 53. What Happens to Auto-Configured `DataSource`?

Conceptually:

```text
JDBC classes on classpath
       ↓
DataSource auto-configuration candidate
       ↓
conditions evaluated
       ↓
no custom DataSource
       ↓
configuration matches
       ↓
@Bean definition registered
       ↓
context.refresh()
       ↓
DataSource bean created
```

This is how our previous auto-configuration topic connects to the startup lifecycle.

---

# 54. What Happens If a Bean Has `@PostConstruct`?

During bean creation:

```text
instantiate
    ↓
inject dependencies
    ↓
BeanPostProcessor
    ↓
@PostConstruct
    ↓
other initialization callbacks
    ↓
after-initialization post-processors
    ↓
bean ready
```

So startup is not just:

```text
"create beans"
```

It's:

```text
"create + wire + initialize + post-process beans"
```

---

# 55. What Happens If a Bean Is `@Lazy`?

Instead of:

```text
refresh
 ↓
create bean
```

you get:

```text
refresh
 ↓
don't instantiate
 ↓
later request
 ↓
create bean
```

This is why adding lazy initialization can change both:

```text
startup time
+
when configuration errors become visible
```

Spring explicitly notes that lazy initialization can defer configuration/environment errors until the bean is first needed. ([Home][10])

---

# 56. Startup Time vs Readiness

This is a useful production distinction.

Suppose startup takes:

```text
10 seconds
```

Then:

```text
ApplicationStartedEvent
```

might occur around the point that the context has been refreshed.

But if:

```java
@Component
class StartupRunner implements CommandLineRunner {
    public void run(String... args) {
        doExpensiveWork();
    }
}
```

takes another:

```text
20 seconds
```

then:

```text
ApplicationReadyEvent
```

will happen later.

So:

```text
ApplicationStartedEvent
     ≠
ApplicationReadyEvent
```

in terms of when startup work has fully completed. ([Home][2])

---

# 57. EPAM Scenario Question

> **The application has created its Spring context successfully, but `ApplicationReadyEvent` hasn't occurred. What could be happening?**

Think:

```text
ApplicationStartedEvent already happened
        ↓
CommandLineRunner/ApplicationRunner executing
        ↓
runner still running or blocked
        ↓
ApplicationReadyEvent hasn't fired yet
```

This is a realistic debugging scenario.

---

# 58. EPAM Scenario Question

> **The application fails before `ApplicationContext` is created. Which event might you see?**

Possible answer:

```text
ApplicationStartingEvent
```

or failure handling very early in the lifecycle.

The key point is that some Boot events occur before the context exists, and `ApplicationFailedEvent` can have a null context when failure happens that early. The `SpringApplicationRunListener` API explicitly documents that possibility. ([Home][14])

---

# 59. EPAM Scenario Question

> **Why can I not use a normal `@EventListener` bean for `ApplicationStartingEvent`?**

Because:

```text
ApplicationStartingEvent
       ↓
happens before ApplicationContext exists
```

Therefore the listener isn't available as an ordinary bean in that context yet.

Boot's documentation explicitly notes that some early events require listener registration through `SpringApplication`/`SpringApplicationBuilder` or appropriate early listener loading mechanisms. ([Home][2])

---

# 60. EPAM Scenario Question

> **What's the difference between `ContextRefreshedEvent` and `ApplicationReadyEvent`?**

Strong answer:

> `ContextRefreshedEvent` is a Spring Framework context lifecycle event emitted when an `ApplicationContext` is initialized or refreshed. `ApplicationReadyEvent` is a Spring Boot lifecycle event emitted later, after the context has been refreshed and application/command-line runners have completed. ([Home][12])

Excellent distinction.

---

# 61. EPAM Scenario Question

> **When would you use `ApplicationReadyEvent` instead of `@PostConstruct`?**

Use:

```text
@PostConstruct
```

for:

```text
individual bean initialization
```

Use:

```text
ApplicationReadyEvent
```

for:

```text
application-level work that should occur after
the application is fully started according to Boot's readiness lifecycle
```

For expensive post-initialization work, Spring also recommends mechanisms such as `SmartInitializingSingleton` or `ContextRefreshedEvent` rather than putting arbitrary external activity inside individual initialization callbacks. ([Home][15])

---

# 62. EPAM Scenario Question

> **Why does the application fail during startup when a required bean is missing?**

Because:

```text
context.refresh()
    ↓
non-lazy singleton creation
    ↓
dependency resolution
    ↓
missing dependency
    ↓
bean creation failure
    ↓
context startup fails
```

Spring's default eager singleton initialization is a key part of this behavior. ([Home][10])

---

# 63. EPAM Scenario Question

> **How would you investigate a slow Spring Boot startup?**

A strong high-level answer:

```text
1. Measure startup phases.
2. Check whether configuration loading is slow.
3. Identify expensive bean initialization.
4. Look for database/network calls in initialization callbacks.
5. Check auto-configuration and unnecessary dependencies.
6. Check expensive @PostConstruct logic.
7. Check CommandLineRunner/ApplicationRunner execution.
8. Use Spring's startup instrumentation/metrics where appropriate.
```

Spring's `SpringApplication` supports an `ApplicationStartup` mechanism for collecting startup metrics. ([Home][1])

And the Spring documentation specifically warns about expensive activity inside `@PostConstruct`/initialization callbacks. ([Home][15])

---

# 64. `ApplicationStartup`

This is a useful advanced concept.

Spring Boot exposes:

```java
ApplicationStartup
```

as a way to collect startup information/metrics. `SpringApplication` has a `setApplicationStartup(...)` method and exposes the configured startup mechanism. ([Home][1])

This is useful when you want to understand:

```text
Which startup step is slow?
Which beans take time?
Where is application startup spending time?
```

This is especially valuable for production troubleshooting.

---

# 65. What Does `refresh()` Actually Mean?

This is subtle.

People sometimes think:

```text
refresh()
    =
restart application
```

No.

For an `ApplicationContext`, refresh means roughly:

> **Initialize or reinitialize the context's internal state and make its bean definitions and infrastructure ready for use.**

Spring's `ContextRefreshedEvent` is specifically published when the context is initialized or refreshed. ([Home][12])

A context can, depending on the implementation and lifecycle, support refresh semantics without being equivalent to restarting the JVM.

---

# 66. Is `SpringApplication.run()` Synchronous?

The startup call is synchronous in the sense that it doesn't simply return immediately while the context is still being initialized.

Conceptually:

```text
main()
 ↓
run()
 ↓
startup
 ↓
context refresh
 ↓
runners
 ↓
ready
 ↓
run() returns
```

The `SpringApplicationRunListener.ready(...)` callback is documented as occurring immediately before `run` finishes, after the context is refreshed and runners have executed. ([Home][14])

This is an excellent detail for interviews.

---

# 67. What Happens to the JVM After `run()` Returns?

For a normal web application:

```text
SpringApplication.run()
      ↓
returns context
      ↓
embedded server continues running
      ↓
main thread doesn't need to stay inside run()
```

The server and other non-daemon infrastructure keep the process alive.

So:

```java
public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
    System.out.println("main finished");
}
```

can still leave the application running.

This often surprises beginners.

---

# 68. What Happens When the Application Shuts Down?

Eventually:

```text
SIGTERM
JVM shutdown
context.close()
```

and Spring begins bean destruction.

The lifecycle goes toward:

```text
@PreDestroy
   ↓
DisposableBean.destroy()
   ↓
custom destroy
```

from Topic 4.

So the lifecycle is really:

```text
START
 ↓
BOOTSTRAP
 ↓
REFRESH
 ↓
RUN
 ↓
READY
 ↓
SHUTDOWN
 ↓
DESTROY
```

---

# 69. The Interview Answer You Should Memorize

### Question:

> **Explain what happens when `SpringApplication.run()` executes.**

A strong answer:

> `SpringApplication.run()` bootstraps the application. It starts by preparing the application environment, including configuration, profiles, and command-line properties, and determines the web application type. It then creates and prepares an appropriate `ApplicationContext`, loads the application sources and bean definitions, including component scanning and auto-configuration, and refreshes the context. During refresh, Spring processes the bean factory, creates and initializes non-lazy singleton beans, resolves dependencies, applies post-processors, and starts the embedded web server for web applications. After the context is refreshed, Boot publishes `ApplicationStartedEvent`, runs any `ApplicationRunner` and `CommandLineRunner` beans, and then publishes `ApplicationReadyEvent`, after which the application is considered ready to accept traffic. ([Home][1])

That is an excellent **EPAM interview answer**.

---

# 70. If They Ask for More Detail

Expand it:

```text
main()
  ↓
SpringApplication.run()
  ↓
ApplicationStartingEvent
  ↓
prepare Environment
  ↓
ApplicationEnvironmentPreparedEvent
  ↓
determine WebApplicationType
  ↓
create ApplicationContext
  ↓
ApplicationContextInitializers
  ↓
ApplicationContextInitializedEvent
  ↓
load sources
  ↓
component scanning
  ↓
auto-configuration
  ↓
register BeanDefinitions
  ↓
ApplicationPreparedEvent
  ↓
refresh()
  ↓
BeanFactoryPostProcessors
  ↓
BeanPostProcessors
  ↓
bean creation
  ↓
dependency injection
  ↓
initialization callbacks
  ↓
web server initialization
  ↓
ContextRefreshedEvent
  ↓
ApplicationStartedEvent
  ↓
runners
  ↓
ApplicationReadyEvent
```

This is enough depth to handle most follow-up questions.

---

# 71. The Most Important Distinctions From This Topic

Make sure these are clear:

```text
SpringApplication
    ≠
ApplicationContext
```

```text
BeanDefinition loading
    ≠
bean instantiation
```

```text
ApplicationStartedEvent
    ≠
ApplicationReadyEvent
```

```text
ContextRefreshedEvent
    ≠
ApplicationReadyEvent
```

```text
Component scanning
    ≠
Auto-configuration
```

```text
BeanFactoryPostProcessor
    ≠
BeanPostProcessor
```

```text
@PostConstruct
    ≠
ApplicationReadyEvent
```

These distinctions are extremely useful in interviews.

---

# 72. Final Startup Cheat Sheet

### Phase 1 — Bootstrap

```text
main()
 ↓
SpringApplication.run()
 ↓
ApplicationStartingEvent
 ↓
Environment
 ↓
ApplicationEnvironmentPreparedEvent
```

### Phase 2 — Context creation

```text
WebApplicationType
 ↓
create ApplicationContext
 ↓
ApplicationContextInitializer
 ↓
ApplicationContextInitializedEvent
```

### Phase 3 — Configuration loading

```text
@ComponentScan
+
@EnableAutoConfiguration
+
@Bean / @Configuration / @Import
 ↓
BeanDefinitions
 ↓
ApplicationPreparedEvent
```

### Phase 4 — Context refresh

```text
refresh()
 ↓
BeanFactory setup
 ↓
BeanFactoryPostProcessor
 ↓
BeanPostProcessor registration
 ↓
singleton creation
 ↓
DI
 ↓
lifecycle callbacks
 ↓
proxies
 ↓
web server
```

### Phase 5 — Startup completion

```text
ApplicationStartedEvent
 ↓
Liveness = CORRECT
 ↓
CommandLineRunner
ApplicationRunner
 ↓
ApplicationReadyEvent
 ↓
Readiness = ACCEPTING_TRAFFIC
```

([Home][2])

---

# 73. Where We Are Now

You've now built the core Spring Boot foundation:

```text
1. Spring vs Spring Boot
        ↓
2. IoC / Dependency Injection
        ↓
3. IoC Container
        ↓
4. Bean Lifecycle
        ↓
5. Bean Scopes
        ↓
6. Dependency Resolution
        ↓
7. Component Scanning
        ↓
8. @Configuration / @Bean
        ↓
9. Auto-Configuration
        ↓
10. Externalized Configuration
        ↓
11. SpringApplication Startup
```

That's a **strong Spring Boot foundation**.

---

# Next Topic — Spring Boot Starters and Dependency Management

Next we'll cover something that looks simple but has important interview depth:

```text
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-test
```

We'll understand:

> What exactly is a starter?

> Starter vs dependency?

> Why don't we specify versions for every dependency?

> What is Spring Boot dependency management?

> What are BOMs?

> How does `spring-boot-starter-parent` work?

> What changed with modern Maven/Gradle Boot projects?

> Why can two dependencies pull in different versions of the same library?

> How would you troubleshoot a dependency conflict?

That topic connects Spring Boot with **Maven/Gradle**, which is another area interviewers frequently probe in Java/Spring interviews.

[1]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/SpringApplication.html?utm_source=chatgpt.com "SpringApplication (Spring Boot 4.1.1 API)"
[2]: https://docs.spring.io/spring-boot/4.1-SNAPSHOT/reference/features/spring-application.html?utm_source=chatgpt.com "SpringApplication :: Spring Boot"
[3]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/event/ApplicationEnvironmentPreparedEvent.html?utm_source=chatgpt.com "ApplicationEnvironmentPreparedEvent (Spring Boot 4.1.1 API)"
[4]: https://docs.spring.io/spring-boot/4.1/api/java/org/springframework/boot/class-use/WebApplicationType.html?utm_source=chatgpt.com "Uses of Enum Class org.springframework.boot.WebApplicationType (Spring Boot 4.1.0-RC1 API)"
[5]: https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com "SpringApplication :: Spring Boot"
[6]: https://docs.spring.io/spring-boot/4.2/api/java/org/springframework/boot/WebApplicationType.html?utm_source=chatgpt.com "WebApplicationType (Spring Boot 4.2.0-M2 API)"
[7]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/event/ApplicationContextInitializedEvent.html?utm_source=chatgpt.com "ApplicationContextInitializedEvent (Spring Boot 4.1.1 API)"
[8]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/SpringBootApplication.html?utm_source=chatgpt.com "SpringBootApplication (Spring Boot 4.1.1 API)"
[9]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/event/ApplicationPreparedEvent.html?utm_source=chatgpt.com "ApplicationPreparedEvent (Spring Boot 4.1.1 API)"
[10]: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-lazy-init.html?utm_source=chatgpt.com "Lazy-initialized Beans :: Spring Framework"
[11]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/web/server/servlet/package-summary.html?utm_source=chatgpt.com "org.springframework.boot.web.server.servlet (Spring Boot 4.1.1 API)"
[12]: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/event/ContextRefreshedEvent.html?utm_source=chatgpt.com "ContextRefreshedEvent (Spring Framework 7.0.8 API)"
[13]: https://docs.spring.io/spring-boot/4.0/api/java/org/springframework/boot/ApplicationRunner.html?utm_source=chatgpt.com "ApplicationRunner (Spring Boot 4.0.8 API)"
[14]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/SpringApplicationRunListener.html?utm_source=chatgpt.com "SpringApplicationRunListener (Spring Boot 4.1.1 API)"
[15]: https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html?utm_source=chatgpt.com "Customizing the Nature of a Bean :: Spring Framework"

