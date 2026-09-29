# Topic 8 — `@Configuration`, `@Bean`, `@Import`, and Java-Based Configuration

We have already seen:

```java
@Service
public class PaymentService {
}
```

and:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }
}
```

Both can result in Spring-managed beans, but **they are not the same mechanism**.

This topic is important because it explains how Spring configuration works without XML, and it leads directly into **auto-configuration**, which is one of the most important Spring Boot concepts.

---

# 1. Why does `@Configuration` exist?

Imagine you want Spring to create a third-party object:

```java
ObjectMapper objectMapper = new ObjectMapper();
```

You generally can't add:

```java
@Component
```

to Jackson's `ObjectMapper` class because you don't own that class.

Or perhaps construction requires special configuration:

```java
DataSource dataSource = createDataSource(
    url,
    username,
    password
);
```

You want to tell Spring:

> "Here's how this object should be created."

That's what `@Bean` is for.

And `@Configuration` provides a natural place to define those beans.

---

# 2. What is `@Configuration`?

`@Configuration` is a **class-level annotation** indicating that a class is a source of bean definitions. Its methods can be annotated with `@Bean` to define those beans. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

Example:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }
}
```

Conceptually:

```text
@Configuration
      ↓
"This class contains Spring configuration"
      ↓
@Bean methods
      ↓
BeanDefinitions
      ↓
Spring Container
```

---

# 3. What is `@Bean`?

`@Bean` is a **method-level annotation**.

It tells Spring:

> The object returned by this method should be registered as a Spring bean. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com))

Example:

```java
@Bean
public PaymentService paymentService() {
    return new PaymentService();
}
```

Conceptually:

```text
paymentService()
      ↓
PaymentService object
      ↓
Spring container
      ↓
bean
```

By default, the bean name is the method name:

```text
paymentService
```

unless another name is configured. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com))

---

# 4. `@Configuration` vs `@Bean`

These are not alternatives.

They work together:

```text
@Configuration
      ↓
configuration class

@Bean
      ↓
bean definition method
```

Example:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new PaymentService();
    }
}
```

Think:

```text
@Configuration = "This class contains configuration"

@Bean = "This method defines a bean"
```

---

# 5. `@Configuration` vs `@Component`

This is a **very common interview question**.

Both can be discovered as Spring components.

But:

```java
@Component
public class MyComponent {
}
```

means:

> This class itself is a component.

Whereas:

```java
@Configuration
public class AppConfig {
}
```

means:

> This class is a configuration source containing bean definitions.

`@Configuration` is itself a specialized configuration stereotype, and Spring gives full configuration-class processing to it by default. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

So:

```text
@Component
    ↓
normal component

@Configuration
    ↓
configuration component
    +
special handling of @Bean methods
```

That last line is extremely important.

---

# 6. The Interview Trap: `@Bean` inside `@Configuration` vs `@Component`

Consider these two.

### Version A

```java
@Configuration
public class AppConfig {

    @Bean
    public Client client() {
        return new Client();
    }
}
```

### Version B

```java
@Component
public class AppConfig {

    @Bean
    public Client client() {
        return new Client();
    }
}
```

Both `@Bean` methods are valid.

But **the semantics of calls between `@Bean` methods differ**.

This is one of the most important advanced points in today's topic. Spring's docs call the latter **"lite" mode**. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com))

---

# 7. The Classic Example

Suppose:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentGateway paymentGateway() {
        return new PaymentGateway();
    }

    @Bean
    public PaymentService paymentService() {
        return new PaymentService(paymentGateway());
    }
}
```

Look at:

```java
paymentGateway()
```

A normal Java programmer might think:

```text
call method
   ↓
new PaymentGateway()
```

But with a full `@Configuration` class, that's not necessarily what happens.

---

# 8. What Does Full `@Configuration` Do?

Spring normally enhances a full `@Configuration` class using a generated subclass so that calls between `@Bean` methods can be intercepted and redirected through the container. The current Spring documentation describes this as CGLIB-based method interception. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

Conceptually:

```text
Your configuration class
        ↓
Spring enhances it
        ↓
generated subclass/proxy
        ↓
@Bean method calls intercepted
        ↓
container manages the bean
```

So:

```java
paymentGateway()
```

doesn't simply mean:

```java
new PaymentGateway()
```

when called through the enhanced configuration object.

---

# 9. Why Does Spring Intercept `@Bean` Calls?

Consider:

```java
@Bean
public PaymentGateway paymentGateway() {
    return new PaymentGateway();
}
```

and:

```java
@Bean
public PaymentService paymentService() {
    return new PaymentService(paymentGateway());
}
```

Without interception:

```text
paymentService()
     ↓
paymentGateway()
     ↓
new PaymentGateway()
```

Potentially every direct Java call would create a new object.

But the default Spring scope is singleton.

Spring therefore wants:

```text
paymentGateway bean
       ↓
same managed instance
```

The enhanced configuration class can effectively say:

```text
"Does the container already have the paymentGateway bean?"
       ↓
YES → return existing managed bean
       ↓
NO → create/register it
```

Spring's documentation gives essentially this explanation for configuration-class enhancement and singleton behavior. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

---

# 10. Very Important Example

```java
@Configuration
public class AppConfig {

    @Bean
    public Client client() {
        return new Client();
    }

    @Bean
    public Service service() {
        return new Service(client());
    }
}
```

You might logically expect:

```text
client() called
   ↓
Client A

service() calls client()
   ↓
Client B
```

But with full configuration processing:

```text
client bean requested
      ↓
Spring container
      ↓
Client A

service bean requested
      ↓
service() calls client()
      ↓
configuration method interception
      ↓
return existing Client A
```

So both use:

```text
same Client instance
```

for the singleton case. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

---

# 11. This Is NOT Ordinary Java Semantics

This is the key interview point.

Normally:

```java
public Client client() {
    return new Client();
}
```

means every call:

```text
call 1 → Client A
call 2 → Client B
call 3 → Client C
```

But a `@Bean` method in a full `@Configuration` class can behave differently when called through the enhanced configuration instance.

Spring intercepts the call and obtains the bean from the container when appropriate. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

---

# 12. `proxyBeanMethods`

Now we reach a **very important modern Spring/Boot interview topic**:

```java
@Configuration(proxyBeanMethods = false)
```

This tells Spring:

> Don't enhance this configuration class for inter-bean method interception.

The default is:

```java
@Configuration(proxyBeanMethods = true)
```

Spring documents that the default `true` enables method interception to preserve container-managed semantics for direct `@Bean` method calls. Setting it to `false` disables that interception and gives you "lite" mode behavior. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/SpringBootConfiguration.html?utm_source=chatgpt.com))

---

# 13. `proxyBeanMethods = true`

Default:

```java
@Configuration
```

is effectively:

```java
@Configuration(proxyBeanMethods = true)
```

Conceptually:

```text
@Configuration
      ↓
enhanced configuration class
      ↓
@Bean method calls intercepted
      ↓
container-managed inter-bean references
```

---

# 14. `proxyBeanMethods = false`

Now:

```java
@Configuration(proxyBeanMethods = false)
public class AppConfig {

    @Bean
    public Client client() {
        return new Client();
    }

    @Bean
    public Service service() {
        return new Service(client());
    }
}
```

The call:

```java
client()
```

has normal Java semantics.

So:

```text
service()
    ↓
client()
    ↓
new Client()
```

The call is **not redirected through the container simply because it is a `@Bean` method**. Spring's docs explicitly describe this behavior for lite configuration. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

---

# 15. Then Could This Create Two Clients?

Yes.

Consider singleton beans:

```java
@Configuration(proxyBeanMethods = false)
public class AppConfig {

    @Bean
    public Client client() {
        return new Client();
    }

    @Bean
    public Service service() {
        return new Service(client());
    }
}
```

The container has:

```text
client bean
    ↓
Client A
```

But when creating `service`:

```text
service()
   ↓
direct Java call to client()
   ↓
new Client()
   ↓
Client B
```

Now:

```text
Container's client bean → Client A

Service's internal reference → Client B
```

That is often **not what you intended**.

So with `proxyBeanMethods = false`, you should avoid direct cross-calls between `@Bean` methods when you need normal container-managed references.

---

# 16. Better Solution: Method Parameter Injection

Instead of:

```java
@Bean
public Service service() {
    return new Service(client());
}
```

use:

```java
@Bean
public Service service(Client client) {
    return new Service(client);
}
```

Now Spring resolves:

```text
Client
   ↓
inject into service() parameter
```

This works cleanly even when:

```java
@Configuration(proxyBeanMethods = false)
```

because you're using normal dependency injection rather than relying on interception of a Java method call.

This is one of the most useful modern configuration patterns.

---

# 17. Why Do We See `proxyBeanMethods = false` So Often?

You may encounter:

```java
@Configuration(proxyBeanMethods = false)
```

throughout modern Spring Boot code and especially framework/library code.

Why?

If each `@Bean` method is independent:

```java
@Bean
public Client client() {
    return new Client();
}

@Bean
public Repository repository() {
    return new Repository();
}
```

there may be no need for cross-method interception.

Disabling it can avoid CGLIB configuration-class enhancement and its limitations. Spring's current documentation explicitly recommends disabling it when inter-bean method interception isn't needed. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/SpringBootConfiguration.html?utm_source=chatgpt.com))

---

# 18. `proxyBeanMethods = false` — Interview Answer

If asked:

> **What does `proxyBeanMethods = false` mean?**

Say:

> It disables interception of direct calls between `@Bean` methods in the configuration class. The configuration behaves in Spring's "lite" mode, so those method calls use normal Java semantics. If one bean depends on another, it's preferable to express that through method-parameter injection or another dependency-injection mechanism rather than directly calling another `@Bean` method.

That's a strong answer.

---

# 19. `@Configuration` Proxy Restrictions

Because full configuration classes can be enhanced using CGLIB, there are restrictions.

For example, a full configuration class generally cannot be:

```java
final
```

because Spring needs to create a subclass for method interception. Likewise, relevant `@Bean` methods cannot be final if they need to be overridden/intercepted. Spring documents these CGLIB-related limitations. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

Conceptually:

```text
CGLIB
  ↓
subclass original class
  ↓
override/intercept @Bean methods
```

But:

```text
final class
   ↓
cannot subclass
```

Therefore:

```text
full @Configuration
+
CGLIB enhancement
=
certain proxy-related restrictions
```

---

# 20. This Connects to AOP

We haven't covered AOP yet, but notice the recurring pattern:

```text
Spring
   ↓
creates proxies/enhanced classes
```

You will eventually see:

```text
@Configuration
@Transactional
@Async
@Cacheable
Security
```

all involving proxy/interception concepts, though the exact mechanisms and proxy types differ.

This is why understanding configuration enhancement now will make AOP easier later.

---

# 21. `@Bean` Methods Can Have Dependencies

You don't have to manually call other configuration methods.

For example:

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        return createDataSource();
    }

    @Bean
    public UserRepository userRepository(
            DataSource dataSource) {

        return new UserRepository(dataSource);
    }
}
```

This is excellent Spring style.

Spring sees:

```text
userRepository requires DataSource
             ↓
resolve DataSource bean
             ↓
call @Bean method with it
```

This makes the dependency explicit.

---

# 22. Why Method Parameter Injection Is Nice

Compare:

```java
@Bean
public UserRepository userRepository() {
    return new UserRepository(dataSource());
}
```

with:

```java
@Bean
public UserRepository userRepository(
        DataSource dataSource) {

    return new UserRepository(dataSource);
}
```

The second one clearly expresses:

```text
UserRepository
      ↓
depends on
      ↓
DataSource
```

That fits perfectly with the DI model we've already learned.

It's also compatible with:

```java
@Configuration(proxyBeanMethods = false)
```

without relying on cross-method interception.

---

# 23. Can `@Bean` Be Used in a Normal `@Component`?

Yes.

Spring explicitly allows `@Bean` methods in both:

```java
@Configuration
```

and:

```java
@Component
```

classes. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com))

Example:

```java
@Component
public class MyComponent {

    @Bean
    public Client client() {
        return new Client();
    }
}
```

The bean can be registered.

But this is **lite configuration mode**.

---

# 24. Full Configuration vs Lite Configuration

### Full configuration

```java
@Configuration
public class AppConfig {
}
```

Default:

```text
proxyBeanMethods = true
```

Result:

```text
configuration class enhanced
+
@Bean calls intercepted
```

### Lite configuration

Examples:

```java
@Component
class Config {
}
```

or:

```java
@Configuration(proxyBeanMethods = false)
class Config {
}
```

Result:

```text
@Bean methods still register beans
but direct @Bean method calls follow normal Java semantics
```

Spring's current documentation explicitly describes these non-full configuration classes as **lite mode**. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com))

---

# 25. Interview Question: `@Configuration` vs `@Component`

A strong answer:

> Both can be Spring-managed components, but `@Configuration` specifically identifies a configuration class containing bean definitions. Full `@Configuration` classes receive special processing so that direct calls between `@Bean` methods are intercepted and routed through the container, preserving normal bean scope semantics. A regular `@Component` containing `@Bean` methods is processed in lite mode and those method calls use normal Java semantics. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com))

That's an **excellent interview answer**.

---

# 26. `@Import`

Now suppose configuration is growing.

You have:

```text
DatabaseConfig
SecurityConfig
MessagingConfig
CacheConfig
```

You could put everything in:

```text
AppConfig
```

but that becomes messy.

Spring gives us:

```java
@Import
```

`@Import` allows one configuration class to load another configuration class's bean definitions. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/composing-configuration-classes.html?utm_source=chatgpt.com))

Example:

```java
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        return createDataSource();
    }
}
```

Then:

```java
@Configuration
@Import(DatabaseConfig.class)
public class ApplicationConfig {
}
```

Conceptually:

```text
ApplicationConfig
       |
       ↓
@Import(DatabaseConfig)
       |
       ↓
DatabaseConfig bean definitions
       |
       ↓
same ApplicationContext
```

---

# 27. Why `@Import` Is Useful

It helps modularize configuration.

Instead of:

```text
giant AppConfig
```

you can have:

```text
Configuration
│
├── DatabaseConfig
├── SecurityConfig
├── MessagingConfig
└── CacheConfig
```

and compose them:

```java
@Configuration
@Import({
    DatabaseConfig.class,
    SecurityConfig.class,
    MessagingConfig.class
})
public class ApplicationConfig {
}
```

This is especially useful in libraries and modular applications. Spring's docs describe `@Import` specifically as a mechanism for composing Java-based configuration. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/composing-configuration-classes.html?utm_source=chatgpt.com))

---

# 28. `@Import` Can Import More Than Configuration Classes

This is an important advanced point.

`@Import` can import:

```text
@Configuration classes
@Component classes
ImportSelector
ImportBeanDefinitionRegistrar
```

depending on how it is used.

This becomes extremely important later when we discuss:

```text
@Enable... annotations
Auto-configuration
```

because Spring uses import mechanisms extensively to assemble configurations.

For now, remember:

```text
@Import
   ↓
explicitly bring configuration/components into the container
```

---

# 29. `@Import` vs `@ComponentScan`

Another interview question.

### `@ComponentScan`

Says:

```text
"Go search these packages."
```

### `@Import`

Says:

```text
"Load this specific class/configuration."
```

So:

```text
ComponentScan
     ↓
discovery

Import
     ↓
explicit composition
```

This can be a very useful distinction.

---

# 30. Example: Scan vs Import

### Scanning

```java
@ComponentScan("com.example.orders")
```

Spring searches the package.

### Import

```java
@Import(OrderConfig.class)
```

Spring knows exactly which configuration to include.

This means `@Import` can be more explicit and can be useful for modular configuration where you don't want broad scanning.

---

# 31. `@Import` and Testing

Suppose your production application has:

```text
DatabaseConfig
KafkaConfig
SecurityConfig
```

But one test only needs:

```text
DatabaseConfig
```

You can create/import targeted configuration.

Spring Boot's current testing documentation specifically describes separating configuration into distinct classes and using `@Import` to include a configuration in selected tests. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

This helps make tests smaller and more focused.

---

# 32. `@Bean` Naming

Suppose:

```java
@Bean
public PaymentGateway paymentGateway() {
    return new StripePaymentGateway();
}
```

By default:

```text
bean name = paymentGateway
```

You can specify:

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

Spring supports this explicit naming through the `name` attribute. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com))

You can even provide multiple names:

```java
@Bean({"stripeGateway", "primaryPaymentGateway"})
```

The first name is the primary bean name; the others are aliases.

---

# 33. Bean Scope with `@Bean`

You can configure:

```java
@Bean
@Scope("prototype")
public ReportBuilder reportBuilder() {
    return new ReportBuilder();
}
```

So `@Bean` doesn't mean:

```text
always singleton
```

It means:

```text
register this as a bean
```

Scope is a separate concern.

By default, the bean is singleton scoped, but you can configure otherwise. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html?utm_source=chatgpt.com))

---

# 34. Lifecycle with `@Bean`

You can also specify:

```java
@Bean(
    initMethod = "initialize",
    destroyMethod = "close"
)
public MyClient myClient() {
    return new MyClient();
}
```

Now:

```text
bean creation
    ↓
initialize()
```

and during destruction:

```text
close()
```

This connects directly to our Bean Lifecycle topic.

---

# 35. `@Bean` Is Extremely Useful for Third-Party Classes

Suppose:

```java
ObjectMapper mapper = new ObjectMapper();
```

You want:

```java
mapper.enable(...);
mapper.registerModule(...);
```

You can do:

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {

        ObjectMapper mapper =
                new ObjectMapper();

        // custom configuration

        return mapper;
    }
}
```

Now Spring manages the configured instance.

This is one of the most common real-world reasons for `@Bean`.

---

# 36. A Practical Rule

Use:

```text
@Component / @Service / @Repository / @Controller
```

when:

> You own the class and it naturally belongs to the Spring component model.

Use:

```text
@Bean
```

when:

> You need explicit control over creating/configuring an object, especially a third-party class or a class requiring special construction.

This isn't an absolute rule, but it's an excellent practical guideline.

---

# 37. Example: Third-Party Client

Suppose:

```java
ThirdPartyClient client =
        new ThirdPartyClient(apiKey);
```

You can't annotate the external class with:

```java
@Component
```

because you don't control its source.

So:

```java
@Configuration
public class ClientConfig {

    @Bean
    public ThirdPartyClient thirdPartyClient(
            AppProperties properties) {

        return new ThirdPartyClient(
            properties.getApiKey());
    }
}
```

Now the application has:

```text
ThirdPartyClient
       ↓
Spring bean
```

and other beans can inject it.

---

# 38. `@Configuration` Is Itself a Bean

This is another subtle point.

Consider:

```java
@Configuration
public class AppConfig {
}
```

The configuration class itself is registered as a bean-like managed component in the application context.

But it is special because Spring treats it as a configuration source and may enhance it for `@Bean` method interception.

So:

```text
@Configuration
      ↓
Spring-managed configuration class
      +
configuration metadata
      +
potential enhancement
```

---

# 39. `@Configuration(proxyBeanMethods = false)` — Why It Matters in Modern Boot

You may see modern code like:

```java
@Configuration(proxyBeanMethods = false)
public class MyConfiguration {
}
```

This often indicates:

> "These `@Bean` methods don't need cross-method interception."

Then:

```java
@Bean
public Client client() {
    return new Client();
}

@Bean
public Repository repository(Client client) {
    return new Repository(client);
}
```

is an ideal pattern.

The dependency is explicit:

```text
repository(Client client)
           ↑
           |
        injection
```

rather than:

```text
repository()
    ↓
client()
```

requiring interception.

---

# 40. Important Connection to Spring Boot Auto-Configuration

Now we're approaching one of the biggest Spring Boot concepts.

Spring Boot's modern `@AutoConfiguration` annotation is effectively configuration intended to be automatically applied, and its annotation definition uses:

```java
@Configuration(proxyBeanMethods = false)
```

for auto-configuration classes. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

So when you later inspect Spring Boot internals and see:

```java
@AutoConfiguration
```

you should recognize:

```text
It's a specialized configuration mechanism
+
usually proxyBeanMethods = false
+
conditional activation
```

This connects directly to our upcoming auto-configuration topic.

---

# 41. `@Import` and Auto-Configuration

You may wonder:

> "How does Spring Boot actually bring these configuration classes into the application?"

Import infrastructure is a major part of the answer.

Spring has mechanisms such as:

```text
@Import
ImportSelector
ImportBeanDefinitionRegistrar
```

Spring Boot's auto-configuration infrastructure uses import selection machinery to determine which auto-configuration classes should be applied.

We'll unpack this in the **Auto-Configuration** topic.

---

# 42. The `@Configuration` Mental Model

Think:

```text
@Configuration
      |
      +---- @Bean
      |       ↓
      |     Bean
      |
      +---- @Bean
      |       ↓
      |     Bean
      |
      +---- @Bean
              ↓
            Bean
```

The configuration class is essentially:

> **a factory/definition source for the application's infrastructure objects.**

---

# 43. The Most Important `@Configuration` Interview Trap

Suppose interviewer asks:

> Why doesn't calling a `@Bean` method twice necessarily create two singleton beans?

Don't say:

> "Because Java only allows one."

Wrong.

Say:

> In a full `@Configuration` class, Spring enhances the configuration class and intercepts calls to `@Bean` methods so that inter-bean references are routed through the container. Therefore, for singleton-scoped beans, the existing managed instance can be returned rather than creating a new Java object on each call. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

That's a **strong internals answer**.

---

# 44. The `proxyBeanMethods=false` Follow-up

Interviewer:

> What changes if `proxyBeanMethods = false`?

Answer:

> Cross-`@Bean` method calls are no longer intercepted. They follow normal Java semantics. Therefore, if one bean depends on another, I should express that dependency through method parameters or another injection mechanism rather than directly invoking the other `@Bean` method. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/SpringBootConfiguration.html?utm_source=chatgpt.com))

Excellent follow-up answer.

---

# 45. `@Import` Interview Answer

### Question:

**What is `@Import`?**

Answer:

> `@Import` allows configuration to be composed explicitly by bringing another configuration class or supported component/configuration contributor into the application context. It is useful for modularizing configuration and avoiding overly broad component scanning. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/composing-configuration-classes.html?utm_source=chatgpt.com))

---

# 46. `@Bean` Interview Answer

### Question:

**What is `@Bean`?**

Answer:

> `@Bean` is a method-level annotation that declares the object returned by the method as a Spring bean. It is useful when we need explicit control over bean construction or when registering third-party objects that we can't annotate with Spring stereotypes. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com))

---

# 47. `@Configuration` Interview Answer

### Question:

**What is `@Configuration`?**

Answer:

> `@Configuration` identifies a class as a source of bean definitions. Its `@Bean` methods define objects managed by the Spring container. Full configuration classes are specially processed so that inter-bean references through `@Bean` methods can be routed through the container. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/6.2/core/beans/java/configuration-annotation.html?utm_source=chatgpt.com))

---

# 48. `@Component` vs `@Configuration`

### Question:

> Can I replace `@Configuration` with `@Component`?

Answer:

> You can place `@Bean` methods on a `@Component`, and Spring will process them in lite mode, but it is not behaviorally identical to full `@Configuration`. In a full configuration class, Spring intercepts direct `@Bean` method calls; in a normal component those calls have standard Java semantics. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com))

That nuance is exactly what interviewers are often looking for.

---

# 49. Configuration Class + Dependency Injection

The clean modern pattern is:

```java
@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

    @Bean
    public Client client() {
        return new Client();
    }

    @Bean
    public Service service(Client client) {
        return new Service(client);
    }
}
```

Think:

```text
Client
  ↓
managed bean

Service(Client client)
  ↓
Spring resolves Client
  ↓
Service bean
```

No direct configuration-method calls are needed.

---

# 50. A Full Example

Let's build a small application infrastructure.

```java
@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

    @Bean
    public PaymentGateway paymentGateway(
            PaymentProperties properties) {

        return new StripePaymentGateway(
                properties.getApiKey());
    }

    @Bean
    public PaymentService paymentService(
            PaymentGateway paymentGateway) {

        return new PaymentService(paymentGateway);
    }
}
```

Object graph:

```text
PaymentProperties
        ↓
PaymentGateway
        ↓
PaymentService
```

Spring manages the entire graph.

And because we're using:

```java
proxyBeanMethods = false
```

we don't rely on CGLIB interception between the bean methods.

---

# 51. What We've Added to Our Spring Mental Model

Previously:

```text
Component scanning
      ↓
BeanDefinitions
      ↓
Beans
```

Now add:

```text
@Configuration
      ↓
configuration source

@Bean
      ↓
explicit BeanDefinition

@Import
      ↓
compose configuration
```

And:

```text
@Configuration
     |
     +---- proxyBeanMethods=true
     |        ↓
     |   method interception
     |
     +---- proxyBeanMethods=false
              ↓
         normal Java calls
```

---

# 52. Interview Revision Table

| Concept                  | Meaning                                                |
| ------------------------ | ------------------------------------------------------ |
| `@Configuration`         | Class that provides bean definitions                   |
| `@Bean`                  | Method whose return value becomes a Spring bean        |
| `@Import`                | Explicitly include configuration/components            |
| `proxyBeanMethods=true`  | Inter-bean `@Bean` calls can be intercepted            |
| `proxyBeanMethods=false` | Direct `@Bean` calls use normal Java semantics         |
| Full configuration       | Enhanced configuration class                           |
| Lite configuration       | `@Bean` methods without full configuration enhancement |
| Component scanning       | Discovers components automatically                     |

---

# 53. The Most Important Things to Remember

### `@Configuration`

```text
"This class defines configuration."
```

### `@Bean`

```text
"This method defines a bean."
```

### `@Import`

```text
"Include this configuration/component explicitly."
```

### Full `@Configuration`

```text
@Bean method calls
      ↓
may be intercepted
      ↓
container-managed references
```

### `proxyBeanMethods=false`

```text
@Bean method calls
      ↓
normal Java calls
```

### Best pattern with `false`

```java
@Bean
Service service(Client client)
```

rather than:

```java
@Bean
Service service() {
    return new Service(client());
}
```

---

# 54. EPAM-Level Questions From This Topic

You should now be ready for questions such as:

> What is `@Configuration`?

> What is `@Bean`?

> Difference between `@Configuration` and `@Component`?

> Can `@Bean` be used inside `@Component`?

> What is full vs lite configuration?

> What is `proxyBeanMethods`?

> Why does Spring use CGLIB for configuration classes?

> What happens when one `@Bean` method calls another?

> Why can direct `@Bean` method calls behave differently from normal Java methods?

> Why use `proxyBeanMethods=false`?

> How do you inject one `@Bean` into another?

> What is `@Import`?

> `@Import` vs `@ComponentScan`?

> Why use `@Bean` for third-party classes?

> How are `@Bean` names determined?

> Can a `@Bean` have prototype scope?

These are now all connected rather than isolated facts.

---

# 55. One Crucial Connection

We now have the entire chain:

```text
@SpringBootApplication
       |
       +---- @Configuration
       |
       +---- @ComponentScan
       |
       +---- @EnableAutoConfiguration
```

And inside configuration:

```text
@Configuration
      ↓
@Bean
      ↓
BeanDefinition
      ↓
IoC Container
      ↓
Bean
```

And:

```text
@Import
   ↓
additional configurations
   ↓
same application context
```

The **one piece we have mentioned repeatedly but haven't actually dissected yet** is:

```text
@EnableAutoConfiguration
```

That's where Spring Boot becomes much more interesting.

# Next Topic — Spring Boot Auto-Configuration

We'll go deeply into:

```text
@EnableAutoConfiguration
@SpringBootApplication
@Conditional
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnProperty
@Configuration
@AutoConfiguration
```

and answer the big interview question:

> **"How does Spring Boot automatically configure a DataSource, DispatcherServlet, Jackson, JPA, etc. without you explicitly creating all those beans?"**

We'll also trace:

```text
spring-boot-starter-web
        ↓
classpath detection
        ↓
auto-configuration candidates
        ↓
conditions
        ↓
matching configuration
        ↓
@Bean definitions
        ↓
actual infrastructure beans
```

That is one of the **highest-value Spring Boot topics for interviews**.

