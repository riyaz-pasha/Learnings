# Topic 7 — Component Scanning and Stereotype Annotations

Now we're getting into the annotations you see in almost **every Spring Boot project**:

```java
@Component
@Service
@Repository
@Controller
@RestController
@ComponentScan
```

A lot of developers know what these annotations *look like*, but interviewers often ask the more important questions:

> How does Spring find these classes?

> How does `@Service` actually become a bean?

> Is `@Service` different from `@Component`?

> What's special about `@Repository`?

> Why does putting the main class in the wrong package break dependency injection?

> How does Spring choose the bean name?

Let's build this from the ground up.

---

# 1. What problem does Component Scanning solve?

Imagine an application containing:

```text
com.example
│
├── controller
│   └── OrderController.java
│
├── service
│   └── OrderService.java
│
├── repository
│   └── OrderRepository.java
│
└── config
    └── DatabaseConfig.java
```

Without component scanning, you could manually tell Spring:

```java
@Bean
public OrderService orderService(...) {
    return new OrderService(...);
}

@Bean
public OrderController orderController(...) {
    return new OrderController(...);
}
```

For a large application, that becomes extremely tedious.

Component scanning lets Spring discover eligible classes automatically and register their bean definitions. Spring's documentation describes classpath scanning as a mechanism for detecting candidate components and registering corresponding `BeanDefinition`s in the container. ([Home][1])

So:

```text
Classpath
   ↓
Component scanning
   ↓
Find candidate classes
   ↓
Register BeanDefinitions
   ↓
Spring creates/manages beans
```

---

# 2. What exactly is being scanned?

This is important.

Spring isn't blindly doing:

```text
"Find every .class file"
```

and creating objects.

It scans configured package locations and applies **filters** to identify candidate components.

By default, Spring detects classes annotated or meta-annotated with things such as:

```text
@Component
@Repository
@Service
@Controller
@Configuration
@RestController
```

among other component-model annotations. ([Home][1])

So:

```java
public class OrderService {
}
```

by itself:

```text
not automatically discovered
```

Whereas:

```java
@Service
public class OrderService {
}
```

is a component-scanning candidate.

---

# 3. `@Component`

This is the generic stereotype.

```java
@Component
public class EmailSender {
}
```

It essentially tells Spring:

> "This class is a Spring-managed component."

Spring's documentation describes `@Component` as the generic stereotype for any Spring-managed component. ([Home][1])

Conceptually:

```text
@Component
    ↓
component scanning
    ↓
BeanDefinition
    ↓
Spring Bean
```

---

# 4. `@Service`

Now:

```java
@Service
public class OrderService {
}
```

You can think:

```text
@Service
    ↓
specialized Component
```

`@Service` is itself meta-annotated with `@Component`, so Spring treats it as a component during scanning. Spring's documentation explicitly shows `@Service` as a specialization of `@Component`. ([Home][1])

So technically, for basic bean registration:

```java
@Component
public class OrderService {}
```

and:

```java
@Service
public class OrderService {}
```

both can become Spring beans.

But semantically:

```text
@Component
    → generic component

@Service
    → service/business-layer component
```

That semantic distinction matters.

---

# 5. Why not just use `@Component` everywhere?

Because architecture should communicate intent.

Compare:

```java
@Component
public class OrderService {
}
```

with:

```java
@Service
public class OrderService {
}
```

The second immediately tells another developer:

> This class represents application/service-layer logic.

Spring's documentation specifically recommends the specialized stereotypes because they better communicate the component's role and can serve as targets for tools/aspects; Spring also notes that specialized stereotypes may acquire additional semantics over time. ([Home][1])

So:

```text
@Repository
    → persistence role

@Service
    → service/business role

@Controller
    → web/presentation role
```

This is partly semantic and partly extensibility.

---

# 6. `@Repository`

Example:

```java
@Repository
public class OrderRepository {
}
```

It is also a specialization of `@Component`.

But it has an important additional meaning.

Spring provides **exception translation** support for repository components. Data-access exceptions from underlying persistence technologies can be translated into Spring's `DataAccessException` hierarchy through the relevant infrastructure. Spring explicitly calls out exception translation as one of the uses of the repository stereotype. ([Home][1])

So `@Repository` is not merely:

```text
@Component with a different name
```

It communicates:

```text
"This is a persistence/data-access component"
+
"it participates in Spring's repository semantics"
```

---

# 7. `@Controller`

Now we're moving into Spring MVC.

```java
@Controller
public class OrderController {
}
```

It is also a component stereotype.

Its purpose is to identify a class as a **web/presentation controller**.

For example:

```java
@Controller
public class OrderController {

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }
}
```

The controller can participate in Spring MVC's request-handling infrastructure.

---

# 8. `@RestController`

This one is very frequently asked.

```java
@RestController
public class OrderController {
}
```

A useful mental model is:

```text
@RestController
    =
@Controller
    +
@ResponseBody
```

Spring's documentation describes `@RestController` as a composed annotation combining `@Controller` and `@ResponseBody`. ([Home][1])

So:

```java
@RestController
public class OrderController {

    @GetMapping("/orders")
    public Order getOrder() {
        return order;
    }
}
```

The returned object is written to the HTTP response body rather than interpreted as a view name.

---

# 9. `@Controller` vs `@RestController`

Classic interview question.

### `@Controller`

Usually used when returning views:

```java
@Controller
public class PageController {

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }
}
```

Conceptually:

```text
return "orders"
     ↓
view resolution
```

### `@RestController`

Usually used for REST APIs:

```java
@RestController
public class OrderController {

    @GetMapping("/orders")
    public Order getOrder() {
        return order;
    }
}
```

Conceptually:

```text
Order object
    ↓
message conversion
    ↓
JSON/XML/etc.
    ↓
HTTP response
```

The annotation-level distinction comes from `@RestController = @Controller + @ResponseBody`. ([Home][1])

---

# 10. Stereotype Annotations

You should know the word **stereotype**.

In Spring, a stereotype annotation communicates the general role of a class.

```text
@Component
    ↓
generic Spring component

@Repository
    ↓
data access

@Service
    ↓
service/business layer

@Controller
    ↓
web/presentation layer
```

They're not four unrelated mechanisms.

They're part of the same component model. ([Home][1])

---

# 11. Meta-Annotations

This is an important concept.

An annotation can itself have another annotation.

For example, conceptually:

```java
@Component
public @interface Service {
}
```

More precisely, Spring's `@Service` annotation is meta-annotated with `@Component`.

So:

```text
@Service
   ↓
@Component
   ↓
component scanning recognizes it
```

Spring calls this a **meta-annotation** relationship. ([Home][1])

---

# 12. Composed Annotations

Spring goes one step further.

An annotation can be composed from multiple annotations.

For example:

```text
@RestController
      |
      +---- @Controller
      |
      +---- @ResponseBody
```

This is called a **composed annotation**. Spring explicitly documents `@RestController` as an example. ([Home][1])

This pattern is extremely useful when designing custom annotations.

---

# 13. Creating Your Own Stereotype

Suppose your organization has a special component type:

```text
@ExternalClient
```

You could build a composed annotation around Spring's component model.

Conceptually:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ExternalClient {
}
```

Then:

```java
@ExternalClient
public class PaymentClient {
}
```

Spring can treat it as a component because your custom annotation is itself meta-annotated with `@Component`. Spring supports this meta-annotation approach. ([Home][1])

This is an excellent advanced interview answer if they ask:

> Can we create custom stereotype annotations?

Yes.

---

# 14. What is `@ComponentScan`?

Now we reach the mechanism responsible for scanning.

Example:

```java
@Configuration
@ComponentScan("com.example")
public class AppConfig {
}
```

This tells Spring approximately:

> Scan `com.example` and its relevant subpackages for candidate components.

Spring's documentation shows `@ComponentScan(basePackages = "org.example")` as the way to configure classpath scanning. ([Home][1])

---

# 15. `@SpringBootApplication` and `@ComponentScan`

Remember our very first topic?

```java
@SpringBootApplication
public class Application {
}
```

One reason this works without explicitly writing:

```java
@ComponentScan
```

is that `@SpringBootApplication` includes component scanning.

Spring Boot's current documentation explicitly states that `@SpringBootApplication` implicitly includes `@ComponentScan`. ([Home][2])

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

So when your application starts:

```text
@SpringBootApplication
        ↓
@ComponentScan
        ↓
find @Component / @Service / @Repository / @Controller
        ↓
register beans
```

---

# 16. The Most Important Package Question

Suppose your project is:

```text
com.example
│
├── Application.java
│
├── controller
│   └── OrderController.java
│
├── service
│   └── OrderService.java
│
└── repository
    └── OrderRepository.java
```

with:

```java
package com.example;

@SpringBootApplication
public class Application {
}
```

This works naturally.

Why?

Because Spring Boot recommends putting the application class in a **top-level package** so component scanning can cover the application underneath it. ([Home][2])

Conceptually:

```text
com.example
    ↓
scan
    ↓
com.example.controller
com.example.service
com.example.repository
```

---

# 17. The Classic Broken Package Structure

Suppose instead:

```text
com.example
│
├── service
│   └── OrderService.java
│
└── application
    └── Application.java
```

and:

```java
package com.example.application;

@SpringBootApplication
public class Application {
}
```

By default, scanning starts based on the package of the application configuration class.

So:

```text
com.example.application
        ↓
scan underneath
```

but:

```text
com.example.service
```

is a sibling package, not a subpackage.

Therefore:

```text
OrderService
    ↓
may not be discovered
```

and you can get an error such as:

```text
NoSuchBeanDefinitionException
```

when something expects the service.

---

# 18. Why Is Package Placement So Important?

Because component scanning is fundamentally package-based.

Think:

```text
Root package
   |
   +---- controller
   +---- service
   +---- repository
   +---- config
```

Your application class acts as a convenient scanning anchor in normal Spring Boot applications. Spring Boot's documentation explicitly recommends locating the main application class in a top package. ([Home][2])

---

# 19. Fixing Incorrect Package Scanning

You can explicitly configure:

```java
@SpringBootApplication
@ComponentScan("com.example")
public class Application {
}
```

or:

```java
@ComponentScan(
    basePackages = {
        "com.example.service",
        "com.example.repository",
        "com.example.controller"
    }
)
```

Spring supports one or more base packages and other package-pattern configuration. ([Home][1])

However, don't use a giant explicit scan list merely to compensate for poor package organization.

Usually:

```text
com.mycompany.orders
    ↓
Application
    ↓
controller/service/repository/config
```

is cleaner.

---

# 20. How Does Spring Actually Find the Classes?

At a high level:

```text
@ComponentScan
      ↓
configured package(s)
      ↓
classpath scanning
      ↓
discover candidate classes
      ↓
apply include/exclude filters
      ↓
register BeanDefinitions
```

Spring supports various scanning filter mechanisms, including:

```text
annotation
assignable
AspectJ
regex
custom
```

and can customize the default filters. ([Home][1])

So component scanning is actually a configurable mechanism, not just:

> "Find all `@Component`s."

---

# 21. Default Component Scan Filters

By default, Spring detects component stereotypes such as:

```text
@Component
@Service
@Repository
@Controller
@Configuration
```

and custom annotations meta-annotated with `@Component`. ([Home][1])

So:

```java
@MyComponent
public class Something {
}
```

can also be discovered if:

```java
@MyComponent
@Component
public @interface MyComponent {
}
```

---

# 22. Include and Exclude Filters

Suppose you want to exclude repositories:

```java
@ComponentScan(
    basePackages = "com.example",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = Repository.class
    )
)
```

Now:

```text
@Service → included
@Controller → included
@Repository → excluded
```

Spring supports include and exclude filters on `@ComponentScan`. ([Home][1])

This is more advanced, but knowing the capability demonstrates strong understanding.

---

# 23. `useDefaultFilters = false`

You can even disable the normal component filters:

```java
@ComponentScan(
    basePackages = "com.example",
    useDefaultFilters = false
)
```

Now Spring will not automatically detect the standard component stereotypes unless your explicit filters include them.

Spring's current documentation describes this behavior explicitly. ([Home][1])

This is rarely necessary in ordinary Boot applications.

---

# 24. Bean Naming

Now another classic interview question:

> **What bean name does Spring assign to a `@Service` by default?**

Suppose:

```java
@Service
public class OrderService {
}
```

The default bean name is generally:

```text
orderService
```

Spring's default component-name generator typically uses the uncapitalized non-qualified class name. ([Home][1])

So:

```text
OrderService
   ↓
orderService
```

and:

```text
PaymentService
   ↓
paymentService
```

---

# 25. Explicit Bean Name

You can specify one:

```java
@Service("paymentProcessor")
public class PaymentService {
}
```

Now the bean name is:

```text
paymentProcessor
```

Spring's documentation confirms that supplying a name through the stereotype annotation's name/value attribute sets the corresponding bean definition's name. ([Home][1])

You can then refer to that bean using the appropriate name-based mechanisms.

---

# 26. What if Two Classes Have the Same Simple Name?

Suppose:

```text
com.example.stripe.PaymentService
com.example.paypal.PaymentService
```

Both are discovered.

By default, their simple class names imply:

```text
paymentService
paymentService
```

That creates a naming conflict.

This is one of the reasons the package structure and bean naming strategy matter.

Spring supports custom `BeanNameGenerator`s for such cases, including a fully qualified naming strategy. ([Home][1])

In ordinary application code, explicitly naming or restructuring the components is often simpler.

---

# 27. Important: Bean Name != Type

Suppose:

```java
@Service("stripe")
public class StripePaymentService
        implements PaymentService {
}
```

The bean has:

```text
name = stripe
type = StripePaymentService
```

These are different concepts.

You can have:

```text
bean name
   ↓
stripe
```

while the dependency type is:

```text
PaymentService
```

Spring's autowiring is fundamentally type-oriented and can use qualifiers and other rules to narrow candidates. This is why you shouldn't think of every injection operation as a simple string lookup. ([Home][2])

---

# 28. `@Component` vs `@Bean`

This is **another extremely common interview question**.

### `@Component`

Placed on a class:

```java
@Component
public class EmailSender {
}
```

Spring discovers the class through component scanning.

### `@Bean`

Placed on a method:

```java
@Configuration
public class Config {

    @Bean
    public EmailSender emailSender() {
        return new EmailSender();
    }
}
```

You explicitly tell Spring how to create the bean.

Conceptually:

```text
@Component
    ↓
scan class
    ↓
register bean

@Bean
    ↓
call configuration method
    ↓
register returned object as bean
```

Spring's documentation distinguishes component scanning from Java-based bean definitions using `@Configuration` and `@Bean`. ([Home][1])

---

# 29. When Would You Use `@Bean` Instead of `@Component`?

This is very practical.

Suppose the class is:

```java
public class ObjectMapper {
}
```

You may not want to modify that class or make it a Spring-specific component.

So:

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

This is especially useful for:

```text
third-party libraries
custom construction
conditional construction
complex initialization
multiple differently configured instances
```

---

# 30. `@Component` vs `@Service` vs `@Repository`

This is the answer interviewers want:

```text
@Component
    → generic stereotype

@Service
    → specialization for service/business layer

@Repository
    → specialization for persistence/data access
       + repository semantics such as exception translation

@Controller
    → specialization for web/presentation layer

@RestController
    → @Controller + @ResponseBody
       for REST-style responses
```

Spring explicitly defines these specialization relationships. ([Home][1])

---

# 31. A Very Common Interview Trick

### Question:

> "Is `@Service` functionally different from `@Component`?"

The nuanced answer is:

> For basic component scanning and bean registration, `@Service` is treated as a specialization of `@Component`, so both can register a bean. The important difference is semantic: `@Service` communicates that the class belongs to the service layer, and Spring/tooling can associate semantics with that stereotype.

This is better than saying:

> "`@Service` does nothing."

Because that's too absolute.

Spring itself notes that specialized stereotypes can carry additional semantics and already do in cases such as `@Repository` exception translation. ([Home][1])

---

# 32. Another Common Trick

### Question:

> "Is `@Repository` just an alias for `@Component`?"

Answer:

> It is a specialization of `@Component`, but it also identifies persistence components and participates in Spring's exception-translation mechanism.

That's the important distinction. ([Home][1])

---

# 33. What Happens if We Remove `@Service`?

Suppose:

```java
public class OrderService {
}
```

and no other bean definition/configuration registers it.

Then component scanning doesn't automatically discover it as a component.

So:

```text
OrderService
   ↓
not registered
   ↓
not a Spring bean
```

Then:

```java
public OrderController(OrderService orderService)
```

cannot have that dependency resolved from the container.

Result: bean creation/application context failure.

---

# 34. What Happens if We Put `@Component` on Everything?

Technically, you can:

```java
@Component
class OrderController {}

@Component
class OrderService {}

@Component
class OrderRepository {}
```

Spring can register them.

But architecturally you've lost useful semantic information:

```text
"What role does this class play?"
```

Compare:

```java
@Controller
class OrderController {}

@Service
class OrderService {}

@Repository
class OrderRepository {}
```

The second communicates the architecture much more clearly.

This is part of why stereotypes exist.

---

# 35. Component Scan and Dependency Injection Are Different Steps

This distinction is extremely important.

Suppose:

```java
@Service
public class OrderService {
}
```

Component scanning does:

```text
discover OrderService
       ↓
register BeanDefinition
```

Dependency injection happens later:

```text
create OrderService
       ↓
resolve dependencies
       ↓
inject dependencies
```

So:

```text
Component scanning
    ≠
Dependency injection
```

They are connected but separate responsibilities.

---

# 36. A Complete Example

Suppose our project is:

```text
com.example.orders
│
├── OrdersApplication.java
│
├── controller
│   └── OrderController.java
│
├── service
│   └── OrderService.java
│
└── repository
    └── OrderRepository.java
```

Main class:

```java
package com.example.orders;

@SpringBootApplication
public class OrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
```

Service:

```java
package com.example.orders.service;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Repository:

```java
package com.example.orders.repository;

@Repository
public class OrderRepository {
}
```

Controller:

```java
package com.example.orders.controller;

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
@SpringBootApplication
        ↓
@ComponentScan
        ↓
com.example.orders
        ↓
discover:
    OrderController
    OrderService
    OrderRepository
        ↓
register BeanDefinitions
        ↓
create beans
        ↓
resolve dependencies
        ↓
inject:
    OrderRepository → OrderService
    OrderService    → OrderController
        ↓
application ready
```

That's the whole process coming together.

---

# 37. What Exactly Does `@SpringBootApplication` Scan?

This is frequently misunderstood.

People sometimes say:

> "`@SpringBootApplication` scans the entire project."

Not necessarily.

It uses component scanning starting from the package of the application class unless you configure it otherwise.

So if:

```text
Application package:
com.example
```

then:

```text
com.example.*
```

is naturally within the scan hierarchy.

But:

```text
org.other.service
```

is not automatically included just because it exists on the classpath.

---

# 38. `basePackages` vs `basePackageClasses`

You can configure:

```java
@ComponentScan(
    basePackages = "com.example.orders"
)
```

or use a type-safe anchor:

```java
@ComponentScan(
    basePackageClasses = OrdersApplication.class
)
```

The latter avoids hardcoding package strings and is useful in some modular designs.

You don't need to memorize every scanning attribute, but know that `@ComponentScan` supports both package names and class-based package anchors.

---

# 39. What if Component Scanning Finds Two Beans with the Same Type?

Example:

```text
PaymentService
   ├── StripePaymentService
   └── RazorpayPaymentService
```

Scanning registers both:

```text
StripePaymentService → bean
RazorpayPaymentService → bean
```

Component scanning itself doesn't decide which implementation should be injected.

That happens later during:

```text
dependency resolution
```

using mechanisms such as:

```text
@Primary
@Qualifier
@Fallback
other candidate-resolution rules
```

So:

```text
Scanning
    ↓
"What beans exist?"

Dependency resolution
    ↓
"Which bean should satisfy this dependency?"
```

This distinction is very useful.

---

# 40. A Strong EPAM Interview Scenario

### Interviewer:

> "My application starts, but Spring says it cannot find `OrderService`. What would you check?"

You should systematically think:

```text
1. Is OrderService actually registered as a bean?
2. Does it have @Service/@Component/etc.?
3. Is it included in component scanning?
4. Is the package under the application's scan root?
5. Is component scanning restricted by custom filters?
6. Is the configuration class correct?
7. Is there another application context involved?
8. Is the bean conditionally disabled?
```

For the basic case:

```text
@Service missing
       OR
package outside scan
```

are the first things to inspect.

That's much better than immediately adding:

```java
@Autowired
```

everywhere.

---

# 41. A More Advanced Scenario

Suppose:

```text
com.example.app.Application
com.example.shared.service.OrderService
```

and:

```java
@SpringBootApplication
public class Application {
}
```

but the service isn't found.

You might fix:

```java
@ComponentScan({
    "com.example.app",
    "com.example.shared"
})
```

But before doing that, question the package structure.

A cleaner root might be:

```text
com.example
    ├── app
    └── shared
```

or a deliberate modular structure with explicit configuration.

Interviewers like seeing that you understand the architectural implication rather than merely knowing the annotation syntax.

---

# 42. Component Scanning Is Not the Same as Auto-Configuration

This is another common confusion.

### Component scanning

Finds **your application components**:

```text
@Service
@Repository
@Controller
@Component
```

### Auto-configuration

Configures appropriate Spring infrastructure based on classpath/configuration/conditions.

```text
DataSource
MVC infrastructure
Jackson
JPA infrastructure
etc.
```

Conceptually:

```text
Spring Boot
    |
    +---- Component Scan
    |       ↓
    |   Your components
    |
    +---- Auto-Configuration
            ↓
       Framework infrastructure
```

They often happen during the same application startup, but they solve different problems.

---

# 43. Interview Question: "Does `@ComponentScan` create beans?"

A nuanced answer:

> Component scanning discovers candidate classes and registers their bean definitions with the container. The actual bean instances are then created and managed by the container according to their scope and lifecycle.

This is a very good answer because it connects back to Topic 3.

```text
scan
 ↓
BeanDefinition
 ↓
bean creation
```

---

# 44. Interview Question: "What is the relationship between `@ComponentScan` and `@Autowired`?"

Answer:

> `@ComponentScan` helps Spring discover and register component classes as bean definitions. `@Autowired` identifies dependency injection points. Scanning answers "which components exist?", while autowiring helps determine "what dependency should be supplied here?"

Excellent distinction.

---

# 45. Interview Question: "How does Spring generate default bean names?"

For an autodetected stereotype component:

```java
@Service
public class OrderService {
}
```

Spring's default naming strategy generally produces:

```text
orderService
```

using the uncapitalized non-qualified class name unless an explicit name is supplied. Spring's documentation also notes that custom `BeanNameGenerator`s can be configured. ([Home][1])

For a `@Bean` method:

```java
@Bean
public PaymentService paymentService() {
}
```

the default bean name is generally the **method name**:

```text
paymentService
```

Spring's current documentation explicitly describes `@Bean` method names as the default bean names and supports overriding the name. ([Home][3])

---

# 46. `@Bean("foo")`

You can explicitly control it:

```java
@Bean("stripeGateway")
public PaymentGateway paymentGateway() {
    return new StripePaymentGateway();
}
```

Now:

```text
bean name = stripeGateway
```

Spring supports this direct naming form. ([Home][3])

---

# 47. Current Spring 6.1+ Naming Detail

Since we're preparing you for **current** Spring interviews rather than old tutorial material, there's a useful newer detail.

Spring Framework 6.1 deprecated relying on convention-based attribute names for custom stereotype annotations; custom stereotypes should explicitly use `@AliasFor` when exposing a bean-name attribute. ([Home][1])

This probably won't appear in a normal EPAM interview, but it demonstrates why blindly memorizing old Spring tutorials can be dangerous.

---

# 48. Custom Stereotype with Explicit Name Attribute

Conceptually, a modern custom stereotype may look like:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface ExternalClient {

    @AliasFor(annotation = Component.class, attribute = "value")
    String name() default "";
}
```

Then:

```java
@ExternalClient(name = "paymentClient")
public class PaymentClient {
}
```

This uses Spring's meta-annotation/alias machinery properly for modern versions. ([Home][1])

Again, this is **advanced**, not something you need for day-to-day Boot development.

---

# 49. `@Repository` Exception Translation — Deeper Understanding

Imagine your repository calls Hibernate/JDBC and an underlying persistence exception occurs.

Spring can translate technology-specific data access exceptions into its common:

```text
DataAccessException
```

hierarchy.

That's one reason:

```java
@Repository
```

carries more semantic meaning than:

```java
@Component
```

Spring's documentation directly identifies automatic exception translation as a use of the repository stereotype. ([Home][1])

This is a good interview follow-up:

> "What extra functionality does `@Repository` provide?"

Answer:

> It identifies a persistence component and can participate in Spring's exception-translation infrastructure.

---

# 50. Important Architecture Connection

Now our architecture should look like:

```text
                  Spring Container
                        |
           Component Scanning
                        |
        ┌───────────────┼───────────────┐
        ↓               ↓               ↓
    Controller        Service       Repository
        |               |               |
        └───────────────┼───────────────┘
                        ↓
                Dependency Injection
```

And:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

This is the typical layered structure you see in Spring Boot applications.

---

# 51. What Not to Do

### Don't use `@Component` everywhere just because it works

Use the semantic stereotype where appropriate:

```text
@Service
@Repository
@Controller
```

### Don't manually instantiate Spring-managed dependencies

Avoid:

```java
OrderService service =
    new OrderService(new PaymentService());
```

inside application code when those should be Spring-managed.

### Don't blindly add `@ComponentScan` everywhere

`@SpringBootApplication` already provides component scanning in the normal Boot setup. ([Home][2])

### Don't fix package problems by randomly expanding scan packages

First understand why the package structure is wrong.

---

# 52. EPAM Interview Questions — Basic

You should be able to answer:

### What is component scanning?

> The process by which Spring scans configured classpath packages for candidate components and registers their bean definitions.

### What is `@Component`?

> A generic stereotype indicating a Spring-managed component.

### What is `@Service`?

> A specialized component stereotype intended for the service/business layer.

### What is `@Repository`?

> A specialized persistence stereotype that also participates in repository-related semantics such as exception translation.

### What is `@Controller`?

> A Spring MVC presentation-layer component.

### What is `@RestController`?

> A composed annotation equivalent conceptually to `@Controller` plus `@ResponseBody`.

### What is `@ComponentScan`?

> An annotation that configures which packages Spring should scan for components.

---

# 53. EPAM Interview Questions — Intermediate

### Q: Why should the main class be in the root package?

> Because Spring Boot's default component scanning is based on the package of the application class, so placing it in a top-level package naturally allows scanning of the application's subpackages. ([Home][2])

### Q: What happens if a service is outside the scan package?

> It may not be registered as a bean, so dependencies requiring it cannot be resolved.

### Q: Can we create our own stereotype annotation?

> Yes, using Spring's meta-annotation mechanism, typically by meta-annotating the custom annotation with `@Component`. ([Home][1])

### Q: `@Component` vs `@Bean`?

> `@Component` marks a class for component scanning; `@Bean` declares a bean through a configuration method.

### Q: `@Controller` vs `@RestController`?

> `@RestController` combines controller semantics with response-body semantics.

---

# 54. EPAM Interview Questions — Advanced

### Q: How does component scanning actually work?

A strong answer:

> Spring scans configured package locations using classpath metadata and scanning filters. It identifies candidate component classes, registers `BeanDefinition`s for them, and the container later creates and manages their bean instances.

Spring documents candidate filtering and BeanDefinition registration as part of the component-scanning process. ([Home][1])

---

### Q: How does `@Service` work internally?

> `@Service` is meta-annotated with `@Component`, so component scanning recognizes it as a candidate component. The distinction is primarily semantic, although specialized stereotypes can carry additional framework/tooling semantics. ([Home][1])

---

### Q: Why is `@Repository` special?

> It is a `@Component` specialization for data-access components and participates in Spring's exception-translation support. ([Home][1])

---

### Q: How does Spring generate bean names?

> For scanned stereotype components, the default strategy generally uses the uncapitalized non-qualified class name unless an explicit name is provided. For `@Bean` methods, the method name is normally used unless overridden. ([Home][1])

---

# 55. The Most Important Distinctions So Far

At this point, you should be able to distinguish:

```text
@Component
     ↓
generic stereotype

@Service
     ↓
service stereotype

@Repository
     ↓
repository stereotype + exception translation semantics

@Controller
     ↓
MVC controller

@RestController
     ↓
@Controller + @ResponseBody

@ComponentScan
     ↓
find candidate classes

BeanDefinition
     ↓
metadata describing a bean

Bean
     ↓
actual managed object
```

---

# 56. Final Mental Model

This is the picture I'd want you to visualize in an interview:

```text
                 Spring Boot Application
                          |
                          ↓
                 @SpringBootApplication
                          |
              ┌───────────┴───────────┐
              ↓                       ↓
       @ComponentScan         @EnableAutoConfiguration
              |                       |
              ↓                       ↓
      scan application          configure infrastructure
          packages
              |
       ┌──────┼─────────┐
       ↓      ↓         ↓
   @Controller @Service @Repository
       |        |          |
       └────────┼──────────┘
                ↓
          BeanDefinitions
                ↓
          Spring Container
                ↓
        Bean Creation / DI
                ↓
         Managed Objects
```

And the most important conceptual separation:

```text
Component Scanning
        ↓
"What classes become Spring beans?"

Dependency Injection
        ↓
"What dependencies should those beans receive?"

Bean Scope
        ↓
"How many instances / how long?"

Bean Lifecycle
        ↓
"How are those instances created, initialized and destroyed?"
```

Those four concepts now fit together cleanly.

---

# Next Topic — `@Configuration` and `@Bean`

We've mentioned `@Bean` several times, but we haven't yet looked at **how Java configuration actually works**.

That's the next important topic:

# Topic 8 — `@Configuration`, `@Bean`, `@Import`, and Java-based Configuration

We'll go deeply into:

```java
@Configuration
@Bean
@Import
```

and especially the interview trap:

> **What is the difference between `@Configuration` and `@Component`?**

Then the really interesting one:

> **What is the difference between a `@Bean` method inside `@Configuration` and a `@Bean` method inside a normal `@Component`?**

That leads into:

```text
@Configuration
      ↓
@Configuration proxy/enhancement
      ↓
@Bean method interception
      ↓
singleton semantics
```

which is a very good **EPAM-level Spring internals question**.

[1]: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com "Classpath Scanning and Managed Components :: Spring Framework"
[2]: https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html?utm_source=chatgpt.com "Spring Beans and Dependency Injection :: Spring Boot"
[3]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation :: Spring Framework"

