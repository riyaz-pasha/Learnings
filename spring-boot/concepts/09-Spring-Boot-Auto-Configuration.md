# Topic 9 — Spring Boot Auto-Configuration

This is one of the **highest-value Spring Boot interview topics**.

You've now learned:

```text
IoC
 ↓
ApplicationContext
 ↓
Beans
 ↓
Component Scanning
 ↓
@Configuration / @Bean
```

Now we answer the question that makes Spring Boot feel like "magic":

> **How can Spring Boot configure so much without us explicitly creating all those beans?**

For example, you add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

and suddenly Spring can configure JDBC infrastructure.

Or:

```xml
spring-boot-starter-web
```

and you get a web application with the relevant MVC infrastructure and embedded web server.

That's **auto-configuration**.

---

# 1. What is Auto-Configuration?

Spring Boot auto-configuration attempts to automatically configure an application based largely on the **dependencies on the classpath**, existing user configuration, and conditions. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

The simple definition:

> **Auto-configuration is Spring Boot's mechanism for automatically registering/configuring infrastructure beans when certain conditions are satisfied.**

Think:

```text
Dependencies
    +
Application configuration
    +
Existing beans
    +
Environment
    ↓
Spring Boot conditions
    ↓
Appropriate configuration
    ↓
Beans
```

---

# 2. Why Was Auto-Configuration Created?

Without auto-configuration, imagine every Spring Boot application having to manually configure:

```text
DataSource
TransactionManager
EntityManagerFactory
DispatcherServlet
Jackson
Validation
Message converters
Web server
etc.
```

That would mean lots of repetitive configuration.

Boot instead tries to provide sensible defaults.

For example:

```text
You add JDBC dependencies
          ↓
Boot recognizes JDBC is available
          ↓
Boot configures relevant JDBC infrastructure
```

The idea is:

> **Common setup should require little or no explicit configuration.**

---

# 3. Auto-Configuration Is NOT "Configure Everything"

This is a crucial distinction.

Spring Boot does **not** blindly create every possible bean.

Instead:

```text
Does the required technology exist?
        ↓
Does the application actually need this configuration?
        ↓
Has the developer already configured it?
        ↓
Are the relevant properties enabled?
        ↓
Do all conditions match?
```

Only then does the auto-configuration apply.

This is why the phrase:

> **conditional configuration**

is extremely important.

Spring Boot's documentation says auto-configurations are generally conditional, commonly using `@ConditionalOnClass` and `@ConditionalOnMissingBean`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

---

# 4. Where Does Auto-Configuration Come From?

Remember:

```java
@SpringBootApplication
```

Conceptually includes:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

The relevant piece here is:

```java
@EnableAutoConfiguration
```

Spring Boot's current API defines `@EnableAutoConfiguration` using an `@Import` of `AutoConfigurationImportSelector`. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/api/java/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html?utm_source=chatgpt.com))

So:

```text
@SpringBootApplication
        ↓
@EnableAutoConfiguration
        ↓
AutoConfigurationImportSelector
```

That selector is an important internal Spring Boot class to know.

---

# 5. What Does `@EnableAutoConfiguration` Mean?

The annotation basically says:

> "Enable Spring Boot's auto-configuration mechanism for this application context."

For example:

```java
@Configuration
@EnableAutoConfiguration
public class Application {
}
```

Spring Boot then considers the auto-configuration candidates available to the application.

When you're using:

```java
@SpringBootApplication
```

you don't normally add `@EnableAutoConfiguration` separately because Boot already includes it. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html?utm_source=chatgpt.com))

---

# 6. The Big Picture

Here's the important flow:

```text
@SpringBootApplication
        |
        +---------------------+
        |                     |
        ↓                     ↓
@ComponentScan        @EnableAutoConfiguration
        |                     |
        ↓                     ↓
your application       candidate auto-configurations
components                  |
                            ↓
                       conditions
                            |
                     ┌──────┴──────┐
                     ↓             ↓
                  matches       doesn't match
                     ↓             ↓
                 applied         skipped
```

That is the fundamental mechanism.

---

# 7. What Is an Auto-Configuration Class?

Current Spring Boot uses:

```java
@AutoConfiguration
```

for its auto-configuration classes.

`@AutoConfiguration` is itself a specialized `@Configuration` annotation and, importantly, has `proxyBeanMethods=false`. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

Example conceptually:

```java
@AutoConfiguration
public class MyAutoConfiguration {

    @Bean
    public SomeService someService() {
        return new SomeService();
    }
}
```

So:

```text
@AutoConfiguration
       ↓
@Configuration
       +
auto-configuration semantics
```

---

# 8. What Makes It Different From Normal `@Configuration`?

Normal:

```java
@Configuration
public class MyConfig {
}
```

means:

> "This configuration is explicitly part of my application."

Auto-configuration:

```java
@AutoConfiguration
public class MyAutoConfiguration {
}
```

means:

> "Spring Boot can automatically apply this configuration when its conditions are satisfied."

The important difference is **how the configuration is discovered and selected**.

---

# 9. How Does Boot Find Auto-Configuration Classes?

This is an important modern detail.

Current Spring Boot locates auto-configuration candidates through:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

inside JARs. Each listed configuration class is a candidate. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

For example:

```text
META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

might contain:

```text
com.example.MyDatabaseAutoConfiguration
com.example.MyWebAutoConfiguration
```

So conceptually:

```text
JAR
 |
 +-- META-INF/spring/
       |
       +-- AutoConfiguration.imports
                |
                +-- AutoConfigA
                +-- AutoConfigB
                +-- AutoConfigC
```

Spring Boot reads those candidate names and evaluates them.

---

# 10. Important Interview Trap: `spring.factories`

You will find lots of older Spring Boot tutorials saying:

> Auto-configuration classes are registered in `META-INF/spring.factories`.

That was the **older mechanism**.

Modern Spring Boot uses:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

for auto-configuration candidates. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

`spring.factories` still exists for other Spring Boot extension points in some areas, so don't claim that the file is universally obsolete.

For an EPAM interview today, say:

> "Modern Spring Boot discovers auto-configuration candidates through the `AutoConfiguration.imports` file."

That's current and precise.

---

# 11. Where Does the Starter Fit?

This is another major distinction.

Consider:

```text
spring-boot-starter-jdbc
```

A starter generally gives you the **dependencies** appropriate for a capability.

Auto-configuration gives you the **configuration logic**.

Think:

```text
Starter
   ↓
bring libraries onto classpath

Auto-configuration
   ↓
configure those libraries/framework components
```

They frequently work together, but they are not the same thing.

---

# 12. Starter vs Auto-Configuration

### Starter

```text
dependency convenience
```

Example:

```text
spring-boot-starter-web
```

### Auto-configuration

```text
conditional configuration
```

Example:

```text
WebMvcAutoConfiguration
```

So:

```text
starter
   ↓
dependencies available
   ↓
auto-configuration sees them
   ↓
conditions match
   ↓
infrastructure configured
```

This distinction is **very important in interviews**.

---

# 13. First Major Condition — `@ConditionalOnClass`

Suppose Boot has:

```java
@AutoConfiguration
@ConditionalOnClass(SomeLibrary.class)
public class SomeAutoConfiguration {
}
```

This means:

> Apply this configuration only if `SomeLibrary` is available on the classpath.

Spring Boot documents `@ConditionalOnClass` exactly this way. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1/api/java/org/springframework/boot/autoconfigure/condition/ConditionalOnClass.html?utm_source=chatgpt.com))

Conceptually:

```text
Is SomeLibrary on classpath?
        |
       / \
     yes  no
      |    |
      ↓    ↓
    apply  skip
```

This is one of the most fundamental Boot conditions.

---

# 14. Why Is `@ConditionalOnClass` Important?

Imagine your application does not use MongoDB.

There is no point configuring:

```text
MongoClient
MongoTemplate
Mongo repositories
```

So:

```text
MongoDB classes absent
       ↓
Mongo auto-configuration doesn't activate
```

Likewise:

```text
JPA classes absent
       ↓
JPA-specific auto-configuration doesn't activate
```

This keeps Boot adaptable to the dependencies actually present.

---

# 15. `@ConditionalOnMissingBean`

This is perhaps the **most important auto-configuration condition**.

Example:

```java
@Bean
@ConditionalOnMissingBean
public PaymentService paymentService() {
    return new DefaultPaymentService();
}
```

Meaning:

> Create this bean only if the application doesn't already contain a suitable bean. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

Conceptually:

```text
Does user already have PaymentService?
        |
       / \
     yes  no
      |    |
      ↓    ↓
    skip   create default
```

This is how Boot "backs away" from user configuration.

---

# 16. This Is the Heart of "Back Off"

Spring Boot documentation calls auto-configuration **non-invasive** and explains that if you provide your own configuration, auto-configuration can back away. For example, adding your own `DataSource` can cause the default database auto-configuration path to back off. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

That's an extremely important Spring Boot philosophy:

```text
Boot provides defaults
        ↓
Developer provides explicit configuration
        ↓
Boot gets out of the way where appropriate
```

---

# 17. Why Is This Better Than Hard-Coding Defaults?

Imagine Boot did:

```text
ALWAYS create DataSource
```

Then your custom configuration would conflict with Boot.

Instead:

```java
@ConditionalOnMissingBean(DataSource.class)
```

conceptually means:

```text
No DataSource?
    ↓
I'll create one.

You already have one?
    ↓
I'll back off.
```

That's one of the main reasons Spring Boot feels flexible despite being opinionated.

---

# 18. `@ConditionalOnProperty`

Another very common condition.

Example:

```java
@Bean
@ConditionalOnProperty(
    name = "feature.payment.enabled",
    havingValue = "true"
)
public PaymentService paymentService() {
    return new PaymentService();
}
```

Now:

```properties
feature.payment.enabled=true
```

means:

```text
condition matches
   ↓
bean created
```

Whereas:

```properties
feature.payment.enabled=false
```

means:

```text
condition doesn't match
   ↓
bean skipped
```

Spring Boot documents `@ConditionalOnProperty` as conditional configuration based on environment properties. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

---

# 19. `matchIfMissing`

Suppose:

```java
@ConditionalOnProperty(
    name = "feature.payment.enabled",
    havingValue = "true",
    matchIfMissing = true
)
```

Now if the property isn't present:

```text
property missing
      ↓
matchIfMissing=true
      ↓
condition can still match
```

This is useful for defining sensible defaults.

---

# 20. Other Common Conditional Annotations

You should know these names:

```text
@ConditionalOnClass
@ConditionalOnMissingClass

@ConditionalOnBean
@ConditionalOnMissingBean

@ConditionalOnProperty

@ConditionalOnWebApplication
@ConditionalOnNotWebApplication

@ConditionalOnResource

@ConditionalOnSingleCandidate
```

Spring Boot provides these and more. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/api/java/org/springframework/boot/autoconfigure/condition/package-summary.html?utm_source=chatgpt.com))

Don't memorize every one.

Understand the categories.

---

# 21. Condition Categories

Think:

```text
             Conditions
                 |
    ┌────────────┼────────────┐
    ↓            ↓            ↓
 Class         Bean         Property
    |            |            |
OnClass       OnBean       OnProperty
Missing...    Missing...   ...
```

Also:

```text
Resource
Web application
Expression
Single candidate
etc.
```

---

# 22. `@ConditionalOnBean`

Opposite idea to `@ConditionalOnMissingBean`.

```java
@Bean
@ConditionalOnBean(DataSource.class)
public JdbcTemplate jdbcTemplate(
        DataSource dataSource) {

    return new JdbcTemplate(dataSource);
}
```

Meaning:

```text
DataSource exists
      ↓
create JdbcTemplate
```

If there's no DataSource:

```text
skip
```

This allows auto-configuration components to build on one another.

---

# 23. `@ConditionalOnSingleCandidate`

This is a nice advanced condition.

Suppose there are multiple beans of a type:

```text
DataSource
   ├── dataSourceA
   └── dataSourceB
```

If Boot needs a single unambiguous candidate, it can use:

```java
@ConditionalOnSingleCandidate(DataSource.class)
```

This condition matches when an appropriate single candidate can be determined. Spring Boot's API documentation defines it specifically in terms of a bean type and a single candidate. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/api/java/org/springframework/boot/autoconfigure/condition/package-summary.html?utm_source=chatgpt.com))

---

# 24. A Real Example: DataSource Auto-Configuration

Let's look at actual Spring Boot architecture.

The current Boot API describes `DataSourceAutoConfiguration` as an auto-configuration for `DataSource`, with conditions including relevant JDBC classes being present. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/jdbc/autoconfigure/DataSourceAutoConfiguration.html?utm_source=chatgpt.com))

Conceptually:

```text
spring-jdbc / JDBC classes available
              ↓
DataSourceAutoConfiguration candidate
              ↓
conditions evaluated
              ↓
appropriate DataSource configuration
              ↓
DataSource bean
```

The actual auto-configuration is more sophisticated than this simplified picture and handles pooled vs embedded database scenarios, properties, imports, and related infrastructure.

---

# 25. Let's Walk Through a Database Example

Suppose your dependencies include:

```text
Spring JDBC
PostgreSQL JDBC driver
```

and configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/orders
spring.datasource.username=postgres
spring.datasource.password=password
```

Conceptually:

```text
PostgreSQL Driver
       ↓
class available
       ↓
JDBC/DataSource auto-configuration candidate
       ↓
properties available
       ↓
no conflicting user DataSource
       ↓
condition matches
       ↓
DataSource configured
```

Spring Boot then provides the infrastructure your application can inject:

```java
public OrderRepository(DataSource dataSource) {
}
```

---

# 26. What If I Define My Own `DataSource`?

Suppose:

```java
@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        return myCustomDataSource();
    }
}
```

Now Boot's default path may encounter:

```text
@ConditionalOnMissingBean(DataSource.class)
```

and see:

```text
DataSource already exists
```

Therefore:

```text
Boot default
    ↓
BACK OFF
```

That's exactly the desired behavior. Spring Boot documents this "gradually replacing auto-configuration" behavior. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

---

# 27. Very Important: User Configuration Wins

A useful mental model:

```text
Spring Boot
    |
    | "Here's a sensible default."
    ↓
Your application
    |
    | "Actually, I want my own implementation."
    ↓
Your bean
    |
    ↓
Boot's conditional auto-config backs off
```

This is why Spring Boot is **opinionated but non-invasive**.

---

# 28. Is Auto-Configuration Applied Before User Beans?

This is a subtle question.

The Boot API documentation says:

> **Auto-configuration is always applied after user-defined beans have been registered.** ([docs.spring.io](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/api/java/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html?utm_source=chatgpt.com))

This ordering is important because conditions such as:

```text
@ConditionalOnMissingBean
```

need to be able to observe user-defined configuration so that Boot can back off where appropriate.

---

# 29. But There's a Nuance

Don't interpret:

> "Auto-configuration is applied after user beans"

as:

> "Every auto-configuration bean is physically instantiated after every user bean."

That's too simplistic.

We're talking about **configuration registration/selection ordering**, not a universal statement that all bean instances are completely instantiated in two clean phases.

Dependency relationships, lazy beans, configuration processing, post-processors, and bean creation rules still matter.

This distinction is exactly the type of nuance that separates a good interview answer from an over-simplified one.

---

# 30. How Does `AutoConfigurationImportSelector` Fit In?

This class appears in:

```java
@EnableAutoConfiguration
```

Conceptually:

```text
@EnableAutoConfiguration
        ↓
@Import(AutoConfigurationImportSelector.class)
        ↓
find candidate auto-configurations
        ↓
filter/exclude candidates
        ↓
return configuration classes to import
```

The exact internal implementation is more involved, but **`AutoConfigurationImportSelector` is the key class name you should know**.

This is an excellent answer to:

> "How is auto-configuration imported?"

---

# 31. The Modern Auto-Configuration Pipeline

At a high level:

```text
@SpringBootApplication
        ↓
@EnableAutoConfiguration
        ↓
AutoConfigurationImportSelector
        ↓
read AutoConfiguration.imports
        ↓
candidate configurations
        ↓
apply exclusions
        ↓
evaluate conditions
        ↓
select matching configurations
        ↓
register/import configurations
        ↓
@Bean methods
        ↓
actual infrastructure beans
```

This is the most useful internal model.

---

# 32. Where Does Component Scanning Fit?

Don't mix these up.

### Component scanning

```text
@Component
@Service
@Repository
@Controller
```

finds **application components**.

### Auto-configuration

```text
@AutoConfiguration
```

provides **framework/library infrastructure configuration**.

Think:

```text
                   Spring Boot
                       |
          ┌────────────┴────────────┐
          ↓                         ↓
  Component Scanning         Auto-Configuration
          ↓                         ↓
   Your application          Boot/framework config
      beans                       beans
```

Both end up in the same `ApplicationContext`.

---

# 33. Why `@AutoConfiguration` Has `proxyBeanMethods=false`

Remember Topic 8.

We learned:

```java
@Configuration(proxyBeanMethods = false)
```

means no configuration-method interception.

Current `@AutoConfiguration` is defined as:

```text
@Configuration(proxyBeanMethods=false)
```

and Boot's API explicitly states that this is always false for auto-configuration classes. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

Why?

Auto-configuration classes are designed to declare infrastructure beans and should avoid relying on direct `@Bean` method interception.

Instead:

```java
@Bean
SomeService someService(Dependency dependency) {
    return new SomeService(dependency);
}
```

makes dependencies explicit.

---

# 34. Conditions on Configuration Classes vs Bean Methods

You might see:

```java
@ConditionalOnClass(SomeLibrary.class)
@Configuration(proxyBeanMethods = false)
class SomeConfiguration {
}
```

or:

```java
@Bean
@ConditionalOnMissingBean
SomeService someService() {
}
```

Both are possible.

But there is a subtle issue with:

```java
@ConditionalOnClass
```

on a `@Bean` method when the method signature references a class that may not be present.

Spring Boot's documentation warns about this and recommends isolating such conditions in a separate configuration class so the JVM doesn't try to load a missing class too early. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1/api/java/org/springframework/boot/autoconfigure/condition/ConditionalOnClass.html?utm_source=chatgpt.com))

This is an **advanced interview point**.

---

# 35. Why Does a Missing Class Matter?

Imagine:

```java
@Bean
@ConditionalOnClass(SomeMissingLibrary.class)
public SomeMissingLibrary create() {
    return new SomeMissingLibrary();
}
```

You might think:

```text
Class missing
    ↓
condition false
    ↓
everything fine
```

But the method signature itself references:

```text
SomeMissingLibrary
```

and the JVM may need to load/resolve that class before method-level condition processing has a chance to protect you.

That's why Boot recommends:

```text
Separate conditional configuration class
```

to isolate optional dependencies.

This demonstrates that auto-configuration isn't simply a set of annotations evaluated in arbitrary order.

---

# 36. Excluding Auto-Configuration

Suppose Boot automatically configures something you don't want.

You can exclude an auto-configuration.

For example:

```java
@SpringBootApplication(
    exclude = DataSourceAutoConfiguration.class
)
public class Application {
}
```

Spring Boot documents this mechanism explicitly. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

You can also configure exclusions through:

```properties
spring.autoconfigure.exclude=...
```

The current Boot property reference includes `spring.autoconfigure.exclude`. ([docs.spring.io](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

---

# 37. Why Would You Exclude Auto-Configuration?

Example:

```text
Application doesn't need a database
```

but something on the classpath causes database-related auto-configuration to be considered.

You may explicitly exclude it.

Another common scenario:

```text
Boot default configuration
        ↓
doesn't match your architecture
        ↓
custom configuration
        ↓
exclude/override specific Boot configuration
```

Usually, however, prefer configuring the system rather than excluding things blindly.

First understand *why* the auto-configuration is being applied.

---

# 38. Debugging Auto-Configuration

This is a **very practical interview question**.

Interviewer:

> "Spring Boot created a bean I don't understand. How would you debug it?"

Spring Boot provides a **Condition Evaluation Report**.

You can start the application with:

```bash
--debug
```

or:

```text
-Ddebug
```

and Boot logs its auto-configuration decisions. The report can also be exposed through Actuator's `conditions` endpoint when Actuator is enabled. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/application.html?utm_source=chatgpt.com))

---

# 39. The Condition Evaluation Report

It answers questions like:

```text
Why was this auto-configuration applied?

Why was that auto-configuration skipped?

Which condition matched?

Which condition didn't match?

Why did Boot back off?
```

That's incredibly useful.

Conceptually:

```text
AutoConfiguration A
    ↓
@ConditionalOnClass → matched
@ConditionalOnMissingBean → matched
    ↓
APPLIED
```

Another:

```text
AutoConfiguration B
    ↓
@ConditionalOnClass → did not match
    ↓
NOT APPLIED
```

---

# 40. Actuator `conditions`

With Actuator, the conditions endpoint can render the condition evaluation report.

Conceptually:

```text
/actuator/conditions
```

can help you inspect:

```text
positive matches
negative matches
unconditional configurations
```

Spring Boot's documentation specifically recommends the conditions endpoint for debugging auto-configuration. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/application.html?utm_source=chatgpt.com))

This is an excellent production-debugging answer.

---

# 41. How I'd Answer: "How Does Auto-Configuration Work?"

This is one of the most important interview answers.

> `@SpringBootApplication` enables `@EnableAutoConfiguration`. That imports Boot's auto-configuration selector, which discovers candidate auto-configuration classes from the application's dependencies and Boot's `AutoConfiguration.imports` metadata. Boot then evaluates conditions such as `@ConditionalOnClass`, `@ConditionalOnBean`, `@ConditionalOnMissingBean`, and `@ConditionalOnProperty`. Matching auto-configuration classes contribute configuration and bean definitions to the application context, while non-matching configurations are skipped. If the developer provides an appropriate custom bean, conditions such as `@ConditionalOnMissingBean` allow Boot to back off.

That's a **strong EPAM-level answer**.

---

# 42. Interview Question: "How does Spring Boot know what to configure?"

Don't answer:

> "Because of the starter."

Incomplete.

Better:

> Starters primarily bring the relevant dependencies onto the classpath. Spring Boot's auto-configuration mechanism detects those classes and evaluates conditional configuration. If the necessary classes are present and other conditions are satisfied, the relevant auto-configuration contributes beans.

That distinction is important.

---

# 43. Interview Question: "What is a starter?"

Answer:

> A Spring Boot starter is a curated dependency descriptor that brings together the typical libraries needed for a particular capability. It doesn't itself perform the configuration; Boot auto-configuration uses the resulting classpath and application configuration to decide what infrastructure to configure.

---

# 44. Interview Question: "What is the relationship between `@EnableAutoConfiguration` and `@SpringBootApplication`?"

Answer:

```text
@SpringBootApplication
       =
@SpringBootConfiguration
+
@EnableAutoConfiguration
+
@ComponentScan
```

Spring Boot's current documentation explicitly documents these three constituent features. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html?utm_source=chatgpt.com))

So:

```text
@SpringBootApplication
         ↓
@EnableAutoConfiguration
```

is why Boot auto-configuration is enabled automatically.

---

# 45. Interview Question: "Can I disable auto-configuration?"

Yes.

For example:

```java
@SpringBootApplication(
    exclude = DataSourceAutoConfiguration.class
)
```

or through:

```properties
spring.autoconfigure.exclude=...
```

Spring Boot documents both mechanisms. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

---

# 46. Interview Question: "Does auto-configuration override my beans?"

The better answer is:

> Auto-configuration is designed to be non-invasive. Its conditions commonly check whether the application has already supplied relevant beans. When an appropriate user-defined bean exists, auto-configuration can back off rather than replacing it. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

Don't say:

> "User beans always override Boot beans."

That's too broad.

The actual behavior depends on the specific auto-configuration and its conditions.

---

# 47. Interview Question: "Does auto-configuration happen because of component scanning?"

No.

They're separate mechanisms.

```text
Component scanning
   ↓
discover your components

Auto-configuration
   ↓
select Boot configuration
```

`@EnableAutoConfiguration` imports the relevant auto-configuration selector; it isn't simply scanning `@AutoConfiguration` classes as ordinary components. ([docs.spring.io](https://docs.spring.io/spring-boot/4.1-SNAPSHOT/api/java/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html?utm_source=chatgpt.com))

This is a very useful distinction.

---

# 48. Interview Question: "What is `@ConditionalOnMissingBean` used for?"

Answer:

> It allows auto-configuration to provide a default only when the application has not already supplied a matching bean.

Example:

```java
@Bean
@ConditionalOnMissingBean(CacheManager.class)
public CacheManager cacheManager() {
    return new DefaultCacheManager();
}
```

Conceptually:

```text
User CacheManager?
   |
  yes → don't create default
   |
   no → create default
```

That's the essence of Boot's "back off" behavior. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

---

# 49. A More Advanced Question: Why Conditions?

Imagine 100 possible auto-configurations.

If Boot blindly applied all of them:

```text
Database
Mongo
Redis
Kafka
JPA
WebFlux
MVC
Security
...
```

you'd have a giant mess.

Instead:

```text
AutoConfiguration A
    ↓
relevant class exists?
    ↓
yes

user already configured it?
    ↓
no

property enabled?
    ↓
yes

web app?
    ↓
yes

→ Apply
```

Conditional auto-configuration makes the system **composable**.

---

# 50. The "Magic" Isn't Magic

This is the mental breakthrough I want you to have.

When you write:

```java
@SpringBootApplication
```

and suddenly things work, Spring Boot isn't magically guessing.

It's doing approximately:

```text
Classpath
   ↓
Candidate auto-configurations
   ↓
Conditions
   ↓
Bean definitions
   ↓
Spring container
   ↓
Normal Spring bean lifecycle
```

So Boot's magic is really:

> **metadata + imports + conditions + normal Spring configuration**

Once you understand that, Boot becomes much less mysterious.

---

# 51. Creating Your Own Auto-Configuration

This is an advanced but valuable interview question.

Suppose your company creates a reusable library:

```text
company-payment-sdk
```

You want applications to automatically get:

```text
PaymentClient
PaymentMetrics
PaymentConfiguration
```

when the library is present.

You can create:

```java
@AutoConfiguration
public class PaymentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PaymentClient paymentClient(
            PaymentProperties properties) {
        return new PaymentClient(properties);
    }
}
```

Then register it in:

```text
META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

with:

```text
com.company.payment.PaymentAutoConfiguration
```

Spring Boot's documentation describes exactly this pattern for creating external auto-configurations. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

---

# 52. Custom Auto-Configuration Structure

A realistic library may look like:

```text
payment-autoconfigure
│
├── PaymentAutoConfiguration.java
├── PaymentProperties.java
└── META-INF
    └── spring
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

And often:

```text
payment-starter
    ↓
depends on
    ↓
payment-autoconfigure
+
payment-client
```

Conceptually:

```text
Application
   ↓
payment-starter
   ↓
dependencies arrive
   ↓
auto-configuration candidate discovered
   ↓
conditions
   ↓
PaymentClient bean
```

That's how you can build organization-wide starters.

---

# 53. Why Custom Auto-Configuration Is Useful

Imagine 50 microservices use your company's:

```text
Observability SDK
Security SDK
Audit SDK
Messaging SDK
```

Without auto-configuration:

```text
Service A → 50 lines configuration
Service B → 50 lines
Service C → 50 lines
...
```

With a custom starter:

```text
add dependency
    ↓
company auto-configuration
    ↓
common infrastructure
```

while each application can still override the defaults.

That's a very realistic enterprise use case.

---

# 54. `AutoConfigureBefore` and `AutoConfigureAfter`

Sometimes auto-configurations depend on an ordering relationship.

Current `@AutoConfiguration` supports:

```java
@AutoConfigureBefore(...)
@AutoConfigureAfter(...)
```

through its annotation metadata. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

Conceptually:

```text
Config A
    ↓
should be processed before
    ↓
Config B
```

or:

```text
Config B
    ↓
after
    ↓
Config A
```

This is about **auto-configuration ordering**, not ordinary bean invocation ordering.

---

# 55. Don't Confuse `@Order` and Auto-Configuration Ordering

We previously learned:

```text
@Order
```

can influence ordering in certain injection/processing contexts.

But:

```text
@AutoConfigureBefore
@AutoConfigureAfter
```

exist specifically for auto-configuration relationships. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

So:

```text
@Order
   ↓
general ordering use cases

@AutoConfigureBefore/After
   ↓
auto-configuration relationships
```

Don't mix them.

---

# 56. A Real Example of Conditions

You might see something like:

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
public class DatabaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(
            DataSourceProperties properties) {

        return properties.initializeDataSourceBuilder()
                .build();
    }
}
```

Read it as plain English:

> "If JDBC/DataSource support is available, and the user hasn't already supplied a DataSource, create the default DataSource."

That's auto-configuration.

The actual Spring Boot implementation is more complex, but this simplified form captures the key design pattern.

---

# 57. Conditions on Configuration vs Conditions on Beans

Consider:

```java
@ConditionalOnMissingBean
@Configuration
class MyConfig {
}
```

versus:

```java
@Configuration
class MyConfig {

    @Bean
    @ConditionalOnMissingBean
    MyService myService() {
        return new MyService();
    }
}
```

The distinction is:

```text
condition on configuration class
   ↓
entire configuration may not be registered

condition on bean method
   ↓
configuration exists
but particular bean isn't registered
```

Spring Boot's auto-configuration docs explicitly discuss this distinction. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

---

# 58. A Subtle Condition Ordering Issue

Here's an advanced fact worth remembering.

Conditions such as:

```text
@ConditionalOnBean
@ConditionalOnMissingBean
```

look at the beans that have been processed so far.

Spring Boot's documentation warns that this means bean-definition processing order matters, and recommends using these conditions appropriately on auto-configuration classes because those load after user-defined bean definitions have been added. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

So don't think:

```text
@ConditionalOnMissingBean
```

means:

> "Search some magical final list containing everything that will ever exist."

It evaluates against the relevant state available at that stage.

That's an advanced but excellent interview insight.

---

# 59. How to Debug "Why Did Boot Create This?"

Use:

```bash
java -jar app.jar --debug
```

or:

```text
-Ddebug
```

Then inspect:

```text
Condition Evaluation Report
```

or with Actuator:

```text
/actuator/conditions
```

Spring Boot recommends exactly these mechanisms. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/application.html?utm_source=chatgpt.com))

This is much better than randomly excluding configurations.

---

# 60. How to Debug "Why Didn't Boot Create This?"

Same process:

```text
--debug
   ↓
Condition Evaluation Report
   ↓
look at negative matches
   ↓
find condition that failed
```

For example:

```text
MyAutoConfiguration
   ↓
@ConditionalOnClass → no match
Reason:
Required class X not found
```

or:

```text
@ConditionalOnMissingBean → no match
Reason:
DataSource bean already exists
```

Now you know exactly why.

---

# 61. EPAM Scenario Question

> **My REST application suddenly stopped creating the expected bean after I added a custom configuration. What would you check?**

A good investigation:

```text
1. Check Condition Evaluation Report.
2. Identify the auto-configuration responsible.
3. Check which condition failed.
4. Check whether a user bean caused @ConditionalOnMissingBean to fail.
5. Check required dependencies/classes.
6. Check relevant properties.
7. Check whether the auto-configuration was explicitly excluded.
```

That's a production-oriented answer.

---

# 62. EPAM Scenario Question

> **How would you override Boot's default configuration?**

A strong answer:

> First I'd identify the auto-configuration and its conditions. Then, where the design allows it, I'd provide my own bean or configuration so the relevant auto-configuration backs off. If I genuinely don't want the auto-configuration at all, I can exclude it through `@SpringBootApplication(exclude = ...)` or `spring.autoconfigure.exclude`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html?utm_source=chatgpt.com))

This is much better than:

> "Disable auto-configuration."

---

# 63. EPAM Scenario Question

> **Why does adding a dependency sometimes change your application behavior even though you didn't write any code?**

Excellent answer:

> Because adding a dependency changes the classpath. Spring Boot's auto-configuration uses classpath conditions and other conditions to determine which configurations should apply. Therefore, adding a library can cause a previously inactive auto-configuration to become eligible.

This is one of the defining characteristics of Boot.

---

# 64. EPAM Scenario Question

> **What happens if I add both Web MVC and WebFlux dependencies?**

Don't simply answer:

> "Spring Boot gets confused."

The actual result depends on the exact dependencies/version/configuration and Boot's web-application conditions.

This is precisely the kind of scenario where you should inspect the relevant auto-configuration and condition report rather than memorize an oversimplified rule.

That is a valuable production-debugging mindset.

---

# 65. The Five Most Important Auto-Configuration Annotations

Know these extremely well:

```java
@ConditionalOnClass
```

> Required class exists.

```java
@ConditionalOnMissingClass
```

> Required class does not exist.

```java
@ConditionalOnBean
```

> Required bean exists.

```java
@ConditionalOnMissingBean
```

> Required bean doesn't exist.

```java
@ConditionalOnProperty
```

> Property satisfies the configured condition.

Then know:

```text
@ConditionalOnWebApplication
@ConditionalOnSingleCandidate
@ConditionalOnResource
```

as additional tools.

---

# 66. The Complete Auto-Configuration Mental Model

This is the diagram I want you to memorize:

```text
                 @SpringBootApplication
                          |
                          ↓
               @EnableAutoConfiguration
                          |
                          ↓
             AutoConfigurationImportSelector
                          |
                          ↓
       AutoConfiguration.imports metadata
                          |
                          ↓
                Candidate configurations
                          |
                 ┌────────┴─────────┐
                 ↓                  ↓
             Exclusions         Conditions
                                      |
                   ┌──────────────────┼──────────────────┐
                   ↓                  ↓                  ↓
             Classpath             Beans             Properties
             conditions           conditions          conditions
                   |                  |                  |
                   └──────────────────┼──────────────────┘
                                      ↓
                              matching configs
                                      ↓
                                @Bean methods
                                      ↓
                              BeanDefinitions
                                      ↓
                              Spring Container
                                      ↓
                            Actual infrastructure
```

That's Spring Boot auto-configuration in one picture.

---

# 67. The Bigger Picture: Everything We've Learned

Now we have a much more complete understanding of startup:

```text
@SpringBootApplication
        |
        +------------------------+
        |                        |
        ↓                        ↓
@ComponentScan            @EnableAutoConfiguration
        |                        |
        ↓                        ↓
Your components          Boot auto-config candidates
        |                        |
        ↓                        ↓
BeanDefinitions            Conditions evaluated
        |                        |
        |                 ┌──────┴──────┐
        |                 ↓             ↓
        |              match          no match
        |                 ↓             ↓
        |             imported        skipped
        |                 |
        +───────────┬─────+
                    ↓
               ApplicationContext
                    ↓
              Bean creation
                    ↓
             Dependency Injection
                    ↓
               Bean lifecycle
                    ↓
                Application
```

At this point the "Spring Boot magic" is becoming a normal sequence of understandable mechanisms.

---

# 68. Interview Revision

### What is auto-configuration?

> Automatic, conditional configuration of Spring application infrastructure based on classpath, existing beans, properties, and other conditions.

### What enables it?

> `@EnableAutoConfiguration`, which is already included in `@SpringBootApplication`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html?utm_source=chatgpt.com))

### What is `@AutoConfiguration`?

> A Spring Boot annotation used to define configuration intended to be automatically applied. It is a specialized `@Configuration` with `proxyBeanMethods=false`. ([docs.spring.io](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfiguration.html?utm_source=chatgpt.com))

### How are auto-configurations discovered?

> Modern Boot uses `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

### What is `@ConditionalOnClass`?

> Activate configuration when specified classes are present on the classpath.

### What is `@ConditionalOnMissingBean`?

> Activate configuration/bean creation only when an appropriate bean is not already present.

### What is "back off"?

> Boot's auto-configuration avoids supplying its default when the application has already provided the relevant configuration/bean.

### How do you debug auto-configuration?

> `--debug` / `-Ddebug`, then inspect the condition evaluation report; Actuator also exposes the `conditions` endpoint. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/application.html?utm_source=chatgpt.com))

---

# 69. One Very Important Interview Distinction

Keep these three separate:

```text
Starter
   ↓
brings dependencies

ComponentScan
   ↓
finds application components

AutoConfiguration
   ↓
conditionally configures infrastructure
```

For example:

```text
spring-boot-starter-web
        ↓
Web dependencies available
        ↓
Auto-configuration activates
        ↓
Web infrastructure configured
        ↓
Your @RestController discovered through component scanning
```

The starter, auto-configuration, and component scanning are **different pieces working together**.

---

# 70. What I'd Say in an EPAM Interview

If asked:

> **"Explain Spring Boot auto-configuration in detail."**

A strong 1–2 minute answer:

> Spring Boot auto-configuration reduces the need for explicit infrastructure configuration. `@SpringBootApplication` includes `@EnableAutoConfiguration`, which imports Boot's auto-configuration selection mechanism. Boot discovers auto-configuration candidates from its `AutoConfiguration.imports` metadata and evaluates conditions based on classpath classes, existing beans, application properties, web environment, and other criteria. For example, `@ConditionalOnClass` can ensure a library is present, while `@ConditionalOnMissingBean` allows Boot to provide a default only when the application hasn't supplied its own bean. Matching auto-configurations contribute bean definitions to the normal Spring ApplicationContext, while non-matching configurations are skipped. This allows Spring Boot to provide sensible defaults while remaining non-invasive and allowing developers to replace or exclude specific auto-configurations.

That's an answer that demonstrates actual understanding.

---

# 71. Important Current-Version Note

Because you're preparing for interviews **now**, don't rely on older Spring Boot explanations that say:

```text
spring.factories
```

is the central auto-configuration discovery mechanism.

For current Spring Boot 4.1.x, the official documentation and API point to:

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

and `@AutoConfiguration` is explicitly a `@Configuration(proxyBeanMethods=false)`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html?utm_source=chatgpt.com))

That's exactly the sort of version-sensitive detail worth knowing.

---

# Next Topic — `application.properties`, `application.yml`, Profiles, and Externalized Configuration

We've now learned:

```text
Beans
DI
Scopes
Lifecycle
Component Scanning
@Configuration
@Bean
Auto-Configuration
```

The next question becomes:

> **How does Spring Boot get configuration values such as database URLs, ports, credentials, feature flags, timeouts, and environment-specific settings?**

We'll go deep into:

```text
application.properties
application.yml
Environment
@Value
@ConfigurationProperties
Profiles
application-{profile}.yml
environment variables
command-line arguments
property precedence
config import
secret/config management
```

And especially the interview-heavy question:

> **`@Value` vs `@ConfigurationProperties` — when and why would you choose one over the other?**

