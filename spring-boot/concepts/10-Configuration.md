# Topic 10 — Externalized Configuration, `application.properties`, YAML, `@Value`, `@ConfigurationProperties`, Profiles, and Property Precedence

This is another **very high-value Spring Boot interview topic**.

A real Spring Boot application should not hardcode things like:

```java
String dbUrl = "jdbc:postgresql://prod-db:5432/orders";
String username = "admin";
int timeout = 5000;
```

because the same application should normally run in:

```text
DEV
TEST
UAT
PROD
```

with different configuration.

Spring Boot's **externalized configuration** mechanism lets the same application code use configuration from properties/YAML files, environment variables, system properties, command-line arguments, and other sources. ([Home][1])

The most important concepts here are:

```text
Environment
PropertySource
application.properties
application.yaml
@Value
@ConfigurationProperties
Profiles
Property precedence
spring.config.import
```

---

# 1. Why Externalized Configuration?

Suppose your application needs:

```text
database URL
database username
database password
server port
API timeout
Kafka broker
feature flags
external service URLs
```

These values are **environment-specific**.

For example:

```text
DEV
DB = localhost

TEST
DB = test-db

PROD
DB = prod-db
```

You don't want:

```java
if (environment.equals("prod")) {
    ...
}
```

inside your business code.

Instead:

```text
same Java code
       +
different configuration
       ↓
different environment behavior
```

That's the idea behind externalized configuration.

---

# 2. `application.properties`

The simplest configuration file is:

```text
src/main/resources/application.properties
```

Example:

```properties
server.port=8081

app.name=order-service
app.timeout=5s

spring.datasource.url=jdbc:postgresql://localhost:5432/orders
spring.datasource.username=postgres
spring.datasource.password=secret
```

Spring Boot automatically loads application configuration files from its standard config locations. ([Home][1])

---

# 3. `application.yaml`

You can express the same information in YAML:

```yaml
server:
  port: 8081

app:
  name: order-service
  timeout: 5s

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders
    username: postgres
    password: secret
```

YAML is often easier to read for hierarchical configuration.

Conceptually:

```text
properties:

app.name=order-service
app.timeout=5s

yaml:

app:
  name: order-service
  timeout: 5s
```

Spring Boot recommends sticking to one configuration format for an application; when `.properties` and YAML are present in the same location, `.properties` takes precedence. ([Home][1])

---

# 4. Properties vs YAML

There is no fundamental difference in what they represent.

Both become entries available through Spring's configuration environment.

For example:

```yaml
app:
  name: orders
  timeout: 5s
```

corresponds conceptually to:

```properties
app.name=orders
app.timeout=5s
```

Spring Boot flattens YAML structures into property names internally; lists are represented with indexed property notation when exposed through the environment. ([Home][1])

---

# 5. What Is `Environment`?

This is a very important Spring concept.

Spring's `Environment` provides access to:

```text
properties
+
profiles
```

Conceptually:

```text
Environment
    |
    +---- PropertySources
    |
    +---- Active Profiles
    |
    +---- Default Profiles
```

You can inject it:

```java
@Service
public class OrderService {

    private final Environment environment;

    public OrderService(Environment environment) {
        this.environment = environment;
    }

    public void printConfig() {
        String url =
            environment.getProperty("app.url");
    }
}
```

So:

```text
Environment
     ↓
"Give me the value of app.url"
```

---

# 6. What Is a `PropertySource`?

This is an excellent advanced interview concept.

A `PropertySource` represents one source of configuration values.

Conceptually:

```text
PropertySources
    |
    +---- application.properties
    +---- application-prod.properties
    +---- environment variables
    +---- system properties
    +---- command line
    +---- ...
```

Spring Boot combines these sources into the `Environment`.

If the same property exists in multiple sources:

```text
app.timeout=5
```

the source with higher precedence wins.

Spring Boot explicitly defines a `PropertySource` ordering for this purpose. ([Home][1])

---

# 7. Property Precedence — VERY IMPORTANT

This is one of the most common Spring Boot interview questions:

> **"What happens if the same property is defined in multiple places?"**

Spring Boot uses an ordered list of property sources, and **later sources override earlier ones**. In the current Spring Boot 4.1.1 documentation, the broad order is: default properties, `@PropertySource`, config data, random values, OS environment variables, Java system properties, JNDI, servlet context/config parameters, `SPRING_APPLICATION_JSON`, command-line arguments, then certain test-specific sources and devtools settings. ([Home][1])

The important interview-level portion is:

```text
application/config files
        ↓
environment variables
        ↓
system properties
        ↓
SPRING_APPLICATION_JSON
        ↓
command-line arguments
```

with higher entries in the precedence chain overriding lower ones according to the documented ordering. ([Home][1])

### The key thing to memorize:

> **Command-line properties have very high precedence and can override file-based configuration.** ([Home][1])

---

# 8. Example: Same Property Everywhere

Suppose:

### `application.properties`

```properties
server.port=8080
```

### Environment variable

```text
SERVER_PORT=8081
```

### Command line

```bash
java -jar app.jar --server.port=8082
```

The application ends up using:

```text
8082
```

because command-line properties have higher precedence. Spring Boot explicitly converts `--key=value` command-line options into properties in the `Environment`. ([Home][1])

---

# 9. Why Is This Useful?

You can build the same artifact:

```text
order-service.jar
```

and run it in different environments:

```text
DEV:
--server.port=8080

TEST:
--server.port=8085

PROD:
--server.port=9000
```

No recompilation.

No code modification.

That's the whole point.

---

# 10. External Configuration Outside the JAR

Suppose you have:

```text
order-service.jar
```

and next to it:

```text
application.properties
```

Spring Boot can load configuration outside the packaged JAR, allowing deployment-specific overrides. Its default search locations include classpath locations and external locations such as the current directory and `config/` directories. ([Home][1])

Conceptually:

```text
deployment/
│
├── order-service.jar
└── application.properties
```

So your JAR can remain unchanged while configuration changes between environments.

---

# 11. Why Is External Configuration Important in Docker/Kubernetes?

Imagine:

```text
Docker image
   ↓
same image deployed everywhere
```

Then:

```text
DEV → environment variables
TEST → environment variables
PROD → environment variables / mounted config
```

You don't want:

```text
one Docker image per environment
```

Instead:

```text
same artifact
+
different environment configuration
```

Spring Boot directly supports environment variables, command-line options, config imports, and configuration trees, which fits cloud/container deployments well. ([Home][1])

---

# 12. Environment Variables

Suppose you have:

```properties
spring.datasource.url=...
```

Environment variables generally cannot use dots on many operating systems, so Spring Boot supports the underscore form:

```text
SPRING_DATASOURCE_URL
```

The Boot documentation describes the environment-variable conversion rules and gives examples such as `SPRING_CONFIG_NAME` for `spring.config.name`. ([Home][1])

So:

```text
spring.datasource.url
        ↓
SPRING_DATASOURCE_URL
```

---

# 13. Relaxed Binding

This is one of the most important features of `@ConfigurationProperties`.

Suppose:

```java
@ConfigurationProperties("my.main-project.person")
public class PersonProperties {

    private String firstName;

    // getter/setter
}
```

Spring Boot supports several representations:

```text
my.main-project.person.first-name
my.main-project.person.firstName
my.main-project.person.first_name
MY_MAINPROJECT_PERSON_FIRSTNAME
```

through its relaxed-binding rules. ([Home][1])

This makes configuration easier across:

```text
.properties
YAML
environment variables
system properties
```

---

# 14. `@Value`

The simplest way to inject an individual property is:

```java
@Service
public class PaymentService {

    @Value("${payment.api-url}")
    private String apiUrl;
}
```

Spring resolves:

```text
${payment.api-url}
```

from the `Environment`.

Spring Boot's external configuration docs show `@Value` as one of the mechanisms for accessing external properties. ([Home][1])

---

# 15. Constructor Injection with `@Value`

You don't need field injection.

You can write:

```java
@Service
public class PaymentService {

    private final String apiUrl;

    public PaymentService(
            @Value("${payment.api-url}")
            String apiUrl) {

        this.apiUrl = apiUrl;
    }
}
```

This fits the constructor-injection approach we've already discussed.

---

# 16. `@Value` with a Default

You can specify:

```java
@Value("${payment.timeout:5000}")
private int timeout;
```

Meaning:

```text
payment.timeout exists?
      ↓
yes → use configured value

no → use 5000
```

This is very convenient for one-off settings.

---

# 17. `@Value` Can Use SpEL

This is an important distinction.

For example:

```java
@Value("#{2 * 60}")
private int timeout;
```

Spring Expression Language can be used with `@Value`.

Spring Boot's documentation explicitly lists **SpEL evaluation as supported by `@Value` but not by `@ConfigurationProperties`**. ([Home][1])

So:

```text
@Value
   ↓
supports SpEL

@ConfigurationProperties
   ↓
doesn't evaluate SpEL
```

---

# 18. The Problem with Too Much `@Value`

Imagine:

```java
@Value("${payment.url}")
private String paymentUrl;

@Value("${payment.timeout}")
private Duration timeout;

@Value("${payment.retry-count}")
private int retryCount;

@Value("${payment.enabled}")
private boolean enabled;

@Value("${payment.api-key}")
private String apiKey;

@Value("${payment.region}")
private String region;
```

This becomes cumbersome.

And suppose you have:

```text
payment
 ├── url
 ├── timeout
 ├── retry-count
 ├── enabled
 ├── api-key
 └── region
```

A better design is often:

```java
@ConfigurationProperties("payment")
public class PaymentProperties {
    // ...
}
```

This is why Spring Boot provides type-safe configuration properties. ([Home][1])

---

# 19. `@ConfigurationProperties`

Suppose:

```properties
payment.url=https://api.example.com
payment.timeout=5s
payment.retry-count=3
payment.enabled=true
```

Create:

```java
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private String url;
    private Duration timeout;
    private int retryCount;
    private boolean enabled;

    // getters/setters
}
```

Now the configuration is represented as one structured object.

Conceptually:

```text
Environment
    ↓
payment.*
    ↓
Binder
    ↓
PaymentProperties
```

Spring Boot's `@ConfigurationProperties` mechanism provides type-safe binding of external configuration to structured objects. ([Home][2])

---

# 20. Why Is `@ConfigurationProperties` Better for Groups of Settings?

Suppose your service has:

```text
payment.url
payment.timeout
payment.retry-count
payment.enabled
payment.region
```

Instead of scattering them across the codebase:

```text
@Value
@Value
@Value
@Value
@Value
```

you get:

```text
PaymentProperties
   |
   +-- url
   +-- timeout
   +-- retryCount
   +-- enabled
   +-- region
```

This gives you:

```text
centralized configuration
+
type safety
+
structured hierarchy
+
relaxed binding
+
metadata support
+
validation support
```

Spring Boot explicitly recommends grouping related application configuration into `@ConfigurationProperties` objects. ([Home][1])

---

# 21. Type Conversion

This is one of the biggest advantages of `@ConfigurationProperties`.

Suppose:

```properties
payment.timeout=5s
```

and:

```java
private Duration timeout;
```

Spring Boot can convert the configuration value into:

```java
Duration.ofSeconds(5)
```

Spring Boot provides dedicated conversion support for types such as `Duration`, `Period`, and `DataSize`. ([Home][1])

So you don't have to manually do:

```java
Duration.ofSeconds(
    Long.parseLong(timeout)
);
```

---

# 22. `Duration` Example

```java
@ConfigurationProperties("payment")
public class PaymentProperties {

    private Duration timeout;

    // getter/setter
}
```

Configuration:

```yaml
payment:
  timeout: 5s
```

Now:

```java
properties.getTimeout()
```

returns:

```text
PT5S
```

as a Java `Duration`.

This is cleaner and less error-prone than representing everything as a raw number.

---

# 23. `DataSize`

Similarly:

```properties
app.buffer-size=10MB
```

can bind to:

```java
private DataSize bufferSize;
```

Spring Boot supports units such as:

```text
B
KB
MB
GB
TB
```

for `DataSize`. ([Home][1])

This is a good example of why structured configuration is useful.

---

# 24. `@ConfigurationProperties` with Nested Objects

Suppose:

```yaml
payment:
  enabled: true
  timeout: 5s
  retry:
    count: 3
    backoff: 2s
```

You can model:

```java
@ConfigurationProperties("payment")
public class PaymentProperties {

    private boolean enabled;
    private Duration timeout;
    private Retry retry;

    // getters/setters

    public static class Retry {
        private int count;
        private Duration backoff;

        // getters/setters
    }
}
```

Now the hierarchy maps naturally.

This is one of the main reasons Boot provides `@ConfigurationProperties`. ([Home][1])

---

# 25. Constructor Binding

Modern Spring Boot supports immutable configuration binding.

For example:

```java
@ConfigurationProperties("payment")
public class PaymentProperties {

    private final String url;
    private final Duration timeout;

    public PaymentProperties(
            String url,
            Duration timeout) {

        this.url = url;
        this.timeout = timeout;
    }

    public String getUrl() {
        return url;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
```

With a single parameterized constructor, current Spring Boot can infer constructor binding for a configuration-properties class. Records are also supported. ([Home][1])

This is a great match for immutable application configuration.

---

# 26. Records

You can also use:

```java
@ConfigurationProperties("payment")
public record PaymentProperties(
        String url,
        Duration timeout,
        int retryCount) {
}
```

Current Spring Boot supports constructor binding with records without requiring `@ConstructorBinding` when there is a single constructor. ([Home][1])

This is a particularly clean modern Java approach.

---

# 27. How Does a `@ConfigurationProperties` Class Become a Bean?

This is a common interview follow-up.

There are several registration mechanisms.

### `@ConfigurationPropertiesScan`

For example:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {
}
```

This tells Boot to scan for configuration-properties classes.

### `@EnableConfigurationProperties`

For example:

```java
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig {
}
```

This explicitly enables/registers the properties class.

Spring Boot's documentation describes both scanning and `@EnableConfigurationProperties` as registration mechanisms. ([Home][1])

---

# 28. `@ConfigurationProperties` Alone Is Not Necessarily a Component

This is an important interview trap.

Consider:

```java
@ConfigurationProperties("payment")
public class PaymentProperties {
}
```

Don't assume:

```text
@ComponentScan
    ↓
automatically finds it
```

The class needs to be registered through:

```text
@ConfigurationPropertiesScan
```

or:

```text
@EnableConfigurationProperties
```

unless you deliberately make it a component in a compatible JavaBean-binding design.

Spring Boot's current documentation explicitly distinguishes configuration-properties registration from ordinary component registration. ([Home][1])

---

# 29. `@ConfigurationProperties` vs `@Value`

This is perhaps **the most important question from this topic**.

Spring Boot's official documentation compares them directly. `@ConfigurationProperties` supports relaxed binding and metadata, while `@Value` supports SpEL and has more limited relaxed-binding behavior. ([Home][1])

|                     | `@Value`            | `@ConfigurationProperties`       |
| ------------------- | ------------------- | -------------------------------- |
| Single property     | Excellent           | Possible                         |
| Group of properties | Cumbersome          | Excellent                        |
| Hierarchical config | Less convenient     | Excellent                        |
| Type-safe structure | Limited             | Strong                           |
| Relaxed binding     | Limited             | Yes                              |
| Metadata support    | No                  | Yes                              |
| SpEL                | Yes                 | No                               |
| Validation          | Less natural        | Strong fit                       |
| Immutable config    | Possible            | Excellent                        |
| Typical use         | Small/simple values | Application configuration groups |

---

# 30. What I Would Choose

For:

```java
@Value("${server.port}")
```

or:

```java
@Value("${feature.enabled:false}")
```

`@Value` can be perfectly fine.

For:

```text
payment.*
database.*
external-service.*
security.*
kafka.*
```

with many related settings:

```text
@ConfigurationProperties
```

is generally the better design.

Spring Boot itself recommends grouping your own configuration keys into `@ConfigurationProperties` classes. ([Home][1])

---

# 31. A Strong Interview Answer

### "When would you use `@Value` vs `@ConfigurationProperties`?"

> I'd use `@Value` for a small number of simple individual properties where I just need a value at a particular injection point. For a group of related configuration properties, especially hierarchical or typed configuration, I'd prefer `@ConfigurationProperties` because it provides structured type-safe binding, relaxed binding, validation and configuration metadata. `@Value` also supports SpEL, while `@ConfigurationProperties` does not. ([Home][1])

That's a strong answer.

---

# 32. Validation of Configuration

This is another important production concept.

Imagine:

```yaml
payment:
  timeout: -5s
```

That doesn't make sense for your application.

You can use validation with configuration properties.

For example:

```java
@ConfigurationProperties("payment")
@Validated
public class PaymentProperties {

    @NotBlank
    private String url;

    @Positive
    private int retryCount;

    // ...
}
```

Then bad configuration can fail startup rather than causing an unexpected runtime failure.

This is one of the major advantages of treating configuration as a typed object.

---

# 33. Why Fail at Startup?

Consider:

```text
Production starts
     ↓
configuration invalid
     ↓
application starts anyway
     ↓
first payment request
     ↓
failure
```

That's undesirable.

Better:

```text
Production starts
     ↓
configuration validation
     ↓
invalid
     ↓
startup fails immediately
```

This is the general principle of **fail fast**.

---

# 34. Profiles

Now let's move to another critical concept:

```text
@Profile
spring.profiles.active
application-dev.yaml
application-prod.yaml
```

Spring Profiles provide a mechanism for segregating configuration/components for different environments. They can be used on `@Component`, `@Configuration`, and `@ConfigurationProperties` classes. ([Home][3])

Think:

```text
DEV
TEST
PROD
```

as different application configurations.

---

# 35. Profile-Specific Files

You can have:

```text
application.yaml
application-dev.yaml
application-test.yaml
application-prod.yaml
```

Suppose:

### application.yaml

```yaml
server:
  port: 8080

app:
  timeout: 5s
```

### application-prod.yaml

```yaml
app:
  timeout: 30s
```

When `prod` is active:

```text
application.yaml
        +
application-prod.yaml
        ↓
prod values override common values
```

Spring Boot explicitly states that profile-specific files override non-profile-specific files. ([Home][1])

---

# 36. Activating a Profile

In `application.properties`:

```properties
spring.profiles.active=prod
```

or:

```yaml
spring:
  profiles:
    active: prod
```

You can also activate it from the command line:

```bash
java -jar app.jar --spring.profiles.active=prod
```

or with an environment variable:

```text
SPRING_PROFILES_ACTIVE=prod
```

Spring Boot documents all of these approaches. ([Home][3])

---

# 37. Profile Activation Precedence

Because `spring.profiles.active` itself is a property, normal property precedence applies.

For example:

```properties
spring.profiles.active=dev
```

inside your file can be overridden by:

```bash
--spring.profiles.active=prod
```

because command-line properties have higher precedence. Spring's profile documentation explicitly notes this. ([Home][4])

---

# 38. What if No Profile Is Active?

Spring uses:

```text
default
```

as the default profile name when no explicit active profile exists. The default can itself be changed through `spring.profiles.default`. ([Home][3])

Conceptually:

```text
no active profiles
        ↓
default profile
```

You can set:

```properties
spring.profiles.default=dev
```

if that suits the application.

---

# 39. `@Profile`

Profiles don't only control property files.

You can control which beans are registered.

Example:

```java
@Configuration
@Profile("dev")
public class DevConfiguration {

    @Bean
    public PaymentGateway paymentGateway() {
        return new MockPaymentGateway();
    }
}
```

Then:

```text
dev active
   ↓
DevConfiguration loaded
```

But:

```text
prod active
   ↓
DevConfiguration skipped
```

Spring Profiles can be applied to configuration components and other supported component types. ([Home][3])

---

# 40. Very Practical Example

Suppose in development you want:

```text
mock payment gateway
```

while production should use:

```text
real payment gateway
```

You could have:

```java
@Configuration
@Profile("dev")
public class DevPaymentConfig {

    @Bean
    public PaymentGateway paymentGateway() {
        return new MockPaymentGateway();
    }
}
```

and:

```java
@Configuration
@Profile("prod")
public class ProdPaymentConfig {

    @Bean
    public PaymentGateway paymentGateway(
            PaymentProperties properties) {

        return new StripePaymentGateway(
                properties.getApiKey());
    }
}
```

Now:

```text
dev
 ↓
MockPaymentGateway

prod
 ↓
StripePaymentGateway
```

This is a very realistic use of profiles.

---

# 41. Profiles vs Environment Variables

Don't confuse them.

### Environment variable

```text
SPRING_DATASOURCE_URL=...
```

supplies a **value**.

### Profile

```text
SPRING_PROFILES_ACTIVE=prod
```

chooses a **configuration environment/activation set**.

Think:

```text
Environment variable
       ↓
property value

Profile
       ↓
which configuration/beans are active
```

You frequently use them together.

---

# 42. Multiple Profiles

You can activate:

```properties
spring.profiles.active=prod,metrics
```

Spring supports multiple active profiles and a last-wins strategy applies in relevant profile-specific property ordering. ([Home][3])

Conceptually:

```text
prod
+
metrics
```

can both be active.

---

# 43. `spring.profiles.include`

Suppose:

```text
prod
```

should always include:

```text
common
metrics
```

You can use:

```properties
spring.profiles.include=common,metrics
```

Included profiles are added on top of the profiles activated through `spring.profiles.active`. Spring Boot documents this mechanism explicitly. ([Home][4])

---

# 44. Profile Groups

This is a useful modern feature.

Suppose:

```text
production
```

should activate:

```text
proddb
prodmq
```

You can define:

```properties
spring.profiles.group.production[0]=proddb
spring.profiles.group.production[1]=prodmq
```

Then:

```bash
--spring.profiles.active=production
```

activates:

```text
production
proddb
prodmq
```

Spring Boot supports profile groups specifically to avoid requiring users to remember many fine-grained profiles. ([Home][4])

---

# 45. Modern Multi-Document Configuration

You can also put multiple configuration documents in one file.

Example YAML:

```yaml
app:
  timeout: 5s

---
spring:
  config:
    activate:
      on-profile: prod

app:
  timeout: 30s
```

The second document is applied only when the appropriate profile is active.

Spring Boot supports conditional activation through `spring.config.activate.on-profile`. ([Home][5])

This can be useful, although separate profile-specific files are often easier to maintain.

---

# 46. Important Profile Restriction

A common interview trap:

You cannot use:

```text
spring.profiles.active
spring.profiles.default
```

inside a profile-specific document or a document activated by `spring.config.activate.on-profile`. Spring Boot explicitly documents this restriction. ([Home][4])

So don't do:

```yaml
spring:
  config:
    activate:
      on-profile: prod

  profiles:
    active: something-else
```

That's invalid configuration.

---

# 47. `spring.config.import`

Now we're entering a more modern Spring Boot feature.

You can import external configuration:

```properties
spring.config.import=optional:file:./dev.properties
```

Then Boot loads that configuration as additional config data. Imported configuration has precedence over the document that imported it. ([Home][1])

Conceptually:

```text
application.properties
       |
       +---- import dev.properties
                     ↓
              additional config
```

---

# 48. Why Is `spring.config.import` Useful?

It can help split configuration into:

```text
common configuration
+
environment-specific configuration
+
external configuration
```

For example:

```text
application.yaml
      ↓
common settings

external.properties
      ↓
deployment-specific settings
```

This is useful in more complex deployments.

---

# 49. `optional:`

Suppose:

```properties
spring.config.import=optional:file:./local.properties
```

means:

```text
file exists
   ↓
load it

file doesn't exist
   ↓
continue startup
```

Without `optional:`, a missing specified config location can cause a `ConfigDataLocationNotFoundException` and prevent startup. ([Home][1])

That's a useful operational detail.

---

# 50. Config Trees — Kubernetes/Cloud

Modern Spring Boot supports configuration trees.

Imagine Kubernetes mounts:

```text
/etc/config/myapp/
    username
    password
```

Then:

```properties
spring.config.import=optional:configtree:/etc/config/
```

can expose those values as configuration properties. ([Home][1])

Conceptually:

```text
mounted file
   ↓
configuration tree
   ↓
Spring Environment
   ↓
@ConfigurationProperties / @Value
```

This is particularly useful for mounted configuration/secrets.

---

# 51. Environment Variables vs Configuration Trees

Spring Boot's documentation points out that environment variables can have drawbacks when values are sensitive and describes configuration trees as an alternative for mounted files, including Kubernetes `ConfigMap`/`Secret` patterns. ([Home][1])

The architectural idea is:

```text
Kubernetes Secret
       ↓
mounted file
       ↓
configtree
       ↓
Spring configuration
```

rather than hardcoding the secret in the application artifact.

For real production systems, secret-management practices depend on your platform and security architecture.

---

# 52. `SPRING_APPLICATION_JSON`

Spring Boot supports putting several properties into one JSON structure:

```bash
SPRING_APPLICATION_JSON='{"my":{"name":"orders","timeout":5}}'
```

which becomes conceptually:

```text
my.name=orders
my.timeout=5
```

in the Spring `Environment`. ([Home][1])

This can be useful when an environment imposes restrictions on property names.

---

# 53. Debugging Configuration Problems

This is a **very common real-world interview scenario**:

> "The value is configured as 30 seconds, but my application is using 10 seconds. What do you check?"

Don't just open `application.properties`.

Think:

```text
1. Which PropertySource supplied the value?
2. Is an environment variable overriding it?
3. Is a system property overriding it?
4. Is a command-line argument overriding it?
5. Is a profile active?
6. Is application-prod.yaml overriding application.yaml?
7. Is an imported config file overriding it?
8. Is a test-specific property overriding it?
```

Spring Boot provides Actuator endpoints such as:

```text
/env
/configprops
```

which can help diagnose unexpected property values; the Boot docs specifically recommend them for determining why a property has a particular value. ([Home][1])

---

# 54. `@Value` vs `Environment`

These are also different.

### `@Value`

```java
@Value("${payment.timeout}")
private Duration timeout;
```

Spring injects the value.

### `Environment`

```java
environment.getProperty("payment.timeout")
```

Your code explicitly asks the environment for the value.

Usually:

```text
normal dependency/configuration
    → @Value / @ConfigurationProperties

dynamic/manual lookup
    → Environment
```

Don't inject `Environment` everywhere just to avoid designing proper configuration objects.

---

# 55. `@ConfigurationProperties` vs `Environment`

Consider:

```java
@Service
public class PaymentService {

    private final PaymentProperties properties;

    public PaymentService(
            PaymentProperties properties) {

        this.properties = properties;
    }
}
```

This is typically cleaner than:

```java
@Service
public class PaymentService {

    private final Environment environment;

    public PaymentService(Environment environment) {
        this.environment = environment;
    }

    public void execute() {
        String url =
            environment.getProperty("payment.url");
    }
}
```

Why?

The first gives the service a clear dependency:

```text
PaymentProperties
```

rather than:

```text
generic configuration system
```

That improves readability and type safety.

---

# 56. Property Names — Canonical Form

Spring Boot recommends using canonical kebab-case when writing property names in `@Value`.

For example:

```java
@Value("${demo.item-price}")
```

is better than:

```java
@Value("${demo.itemPrice}")
```

because the canonical form participates better in Boot's relaxed-binding behavior. Spring's documentation explicitly discusses this distinction. ([Home][1])

This is a subtle current-version interview point.

---

# 57. `@PropertySource`

Spring also has:

```java
@PropertySource
```

Example:

```java
@Configuration
@PropertySource("classpath:custom.properties")
public class Config {
}
```

But don't treat it as the main Spring Boot configuration mechanism.

It has an important limitation: property sources added through `@PropertySource` are added during context refresh, which is too late for certain early-read Boot properties such as `logging.*` and `spring.main.*`. Spring Boot's current documentation explicitly warns about this. ([Home][1])

For normal Boot configuration, prefer:

```text
application.properties/yaml
config imports
environment
etc.
```

---

# 58. Common Interview Question

### Q: `@Value` vs `@ConfigurationProperties`?

Best answer:

> `@Value` is convenient for individual properties and supports SpEL. `@ConfigurationProperties` is better for groups of related configuration because it provides structured type-safe binding, relaxed binding, metadata, and better support for validation and immutable configuration. ([Home][1])

---

# 59. Common Interview Question

### Q: What is the Spring `Environment`?

Answer:

> `Environment` is Spring's abstraction for accessing configuration properties and profile information. It aggregates values from multiple property sources and resolves them according to property-source precedence.

---

# 60. Common Interview Question

### Q: What is relaxed binding?

Answer:

> It allows configuration property names to map to Java property names across different naming conventions. For example, `first-name`, `firstName`, `first_name`, and suitable environment-variable forms can bind to `firstName`. ([Home][1])

---

# 61. Common Interview Question

### Q: What happens if the same property exists in multiple places?

Answer:

> Spring Boot uses ordered `PropertySource`s, and higher-precedence sources override lower-precedence values. In the current Boot ordering, command-line arguments are among the highest-precedence application property sources. ([Home][1])

---

# 62. Common Interview Question

### Q: How do you configure different environments?

Answer:

> I'd typically use profiles with common configuration in `application.yaml` and environment-specific overrides in files such as `application-dev.yaml` and `application-prod.yaml`, then activate the desired profile through `spring.profiles.active`, an environment variable, or a command-line argument. ([Home][3])

---

# 63. Common Interview Question

### Q: Can profiles control beans as well as properties?

Yes.

Example:

```java
@Profile("prod")
@Configuration
public class ProductionConfig {
}
```

Only loaded when the relevant profile is active. Spring profiles can apply to components, configuration classes, and configuration-properties classes. ([Home][3])

---

# 64. Common Interview Question

### Q: Can `@ConfigurationProperties` bind nested objects?

Yes.

Example:

```yaml
payment:
  retry:
    count: 3
    backoff: 2s
```

can bind naturally into nested Java objects. Spring Boot's configuration-properties binder is designed for structured hierarchical configuration. ([Home][1])

---

# 65. Common Interview Question

### Q: How do you debug an unexpected configuration value?

A strong answer:

> I'd inspect the property sources and precedence first, then check active profiles, profile-specific files, environment variables, system properties, command-line arguments, and config imports. In a running application, Actuator's `/env` and `/configprops` endpoints can help diagnose the resolved values and configuration-properties bindings. ([Home][1])

That's a good production-oriented answer.

---

# 66. EPAM Scenario

### Interviewer:

> We deploy the same JAR to DEV and PROD. How would you configure different database URLs?

A strong design:

```text
same JAR
   +
external configuration
   +
profiles/environment variables
```

For example:

```text
DEV
SPRING_PROFILES_ACTIVE=dev

PROD
SPRING_PROFILES_ACTIVE=prod
```

and:

```text
application-dev.yaml
application-prod.yaml
```

with separate database configuration.

Or externalize the DB settings through environment variables/container configuration.

The important architectural principle is:

> **Don't rebuild application code merely because deployment configuration changes.**

---

# 67. EPAM Scenario

### Interviewer:

> We changed an environment variable but the application still uses the old value. What do you check?

Think:

```text
Did the process restart?
        ↓
Was the variable actually injected into the process?
        ↓
Is the property name mapped correctly?
        ↓
Is another higher-precedence source overriding it?
        ↓
Is an active profile supplying another value?
        ↓
Is the application reading the same property key?
```

This is much better than saying:

> "Restart Spring Boot."

---

# 68. EPAM Scenario

### Interviewer:

> Why would you prefer `@ConfigurationProperties` over reading properties directly from `Environment`?

Answer:

> Because a configuration-properties class creates a typed boundary around configuration. The consuming service depends on a meaningful object rather than generic string-based property lookups, which improves type safety, readability, validation, structured configuration, and testing.

---

# 69. EPAM Scenario

### Interviewer:

> Why not put everything in `application.properties`?

Because:

```text
one giant file
    ↓
hard to manage
hard to understand
hard to override
environment concerns mixed together
```

A better organization might be:

```text
application.yaml
application-dev.yaml
application-test.yaml
application-prod.yaml
```

plus external/config-import mechanisms where needed.

---

# 70. Important Current Spring Boot Detail

You're preparing for interviews in **2026**, so pay attention to modern Boot rather than only old tutorials.

Current Spring Boot 4.1.1 documentation uses:

```text
application.properties / application.yaml
spring.config.import
@ConfigurationProperties
@ConfigurationPropertiesScan
profile groups
configtree:
```

and the current property-source ordering is documented in detail by Boot. ([Home][1])

Also, constructor binding for configuration properties and record support are part of the modern model. ([Home][1])

---

# 71. Complete Mental Model

This is the picture to memorize:

```text
                  Configuration Sources
                          |
      ┌───────────────────┼────────────────────┐
      ↓                   ↓                    ↓
 application.yaml     Environment          Command line
      ↓                   ↓                    ↓
application-prod.yaml System properties   --key=value
      |
      └───────────────┬────────────────────────┘
                      ↓
               PropertySources
                      ↓
                 Environment
                      |
          ┌───────────┴───────────┐
          ↓                       ↓
       @Value             @ConfigurationProperties
          ↓                       ↓
   individual value       structured typed object
                                  ↓
                              Service
```

And profiles:

```text
                    Active Profiles
                         |
              ┌──────────┼──────────┐
              ↓          ↓          ↓
             dev        test       prod
              |          |          |
      application-dev application-test application-prod
```

---

# 72. The 10 Things You Should Know Cold

### 1. Externalized configuration

Same application code, different environment-specific configuration. ([Home][1])

### 2. `Environment`

Central abstraction for configuration properties and profiles.

### 3. `PropertySource`

A source of property values.

### 4. `@Value`

Good for simple/individual properties.

### 5. `@ConfigurationProperties`

Best for structured groups of configuration. ([Home][1])

### 6. Relaxed binding

Different naming conventions can bind to the same configuration property. ([Home][1])

### 7. Profiles

Separate configuration/beans for different environments. ([Home][3])

### 8. Profile-specific files

```text
application-dev.yaml
application-prod.yaml
```

override common configuration when active. ([Home][1])

### 9. Property precedence

Higher-priority property sources override lower-priority values. ([Home][1])

### 10. Configuration debugging

Use `/actuator/env`, `/actuator/configprops`, and the relevant property-source/profile information to investigate surprising values. ([Home][1])

---

# Next Topic — `SpringApplication` and Spring Boot Startup Lifecycle

We've now covered:

```text
Spring
IoC / DI
Container
Bean lifecycle
Scopes
Component scanning
@Configuration / @Bean
Auto-configuration
Externalized configuration
Profiles
```

The next topic ties **almost everything together**:

# Topic 11 — What Happens When `SpringApplication.run()` Executes?

We'll trace:

```java
SpringApplication.run(Application.class, args);
```

from beginning to end:

```text
main()
  ↓
SpringApplication
  ↓
Environment preparation
  ↓
Configuration data
  ↓
ApplicationContext creation
  ↓
BeanDefinition loading
  ↓
Component scanning
  ↓
Auto-configuration
  ↓
BeanFactoryPostProcessors
  ↓
Bean creation
  ↓
Dependency injection
  ↓
BeanPostProcessors
  ↓
Embedded web server
  ↓
Application events
  ↓
ApplicationReadyEvent
```

And we'll tackle one of the best Spring Boot interview questions:

> **"Explain what happens internally when you run a Spring Boot application."**

That question can combine almost every topic we've covered so far.

[1]: https://docs.spring.io/spring-boot/reference/features/external-config.html "Externalized Configuration :: Spring Boot"
[2]: https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/properties/ConfigurationProperties.html?utm_source=chatgpt.com "ConfigurationProperties (Spring Boot 4.1.1 API)"
[3]: https://docs.spring.io/spring-boot/reference/features/profiles.html?utm_source=chatgpt.com "Profiles :: Spring Boot"
[4]: https://docs.spring.io/spring-boot/reference/features/profiles.html "Profiles :: Spring Boot"
[5]: https://docs.spring.io/spring-boot/how-to/properties-and-configuration.html?utm_source=chatgpt.com "Properties and Configuration :: Spring Boot"

