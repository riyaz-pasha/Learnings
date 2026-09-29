# Topic 12 — Spring Boot Starters and Dependency Management

This topic looks simple:

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
```

But interviewers can take it surprisingly deep:

> What exactly is a starter?

> Is a starter a JAR containing code?

> Why don't we specify versions for every dependency?

> What is `spring-boot-dependencies`?

> What is a BOM?

> What does `spring-boot-starter-parent` do?

> Parent POM vs BOM?

> How does Maven resolve transitive dependencies?

> What happens when two libraries require different versions of the same dependency?

> How do you debug dependency conflicts?

And because you're preparing for current interviews, there's a useful modern-version detail: **Spring Boot 4.1.1 currently lists newer dedicated starters such as `spring-boot-starter-webmvc`, while `spring-boot-starter-web` is deprecated in favor of it.** ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 1. First: What Is a Dependency?

Suppose you write:

```java
import org.springframework.web.bind.annotation.RestController;
```

Your application needs the Spring Web libraries that contain:

```text
@RestController
DispatcherServlet
Spring MVC
HTTP infrastructure
```

Those libraries are dependencies of your application.

In Maven, you declare them in:

```xml
<dependencies>
    ...
</dependencies>
```

For example:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Maven then resolves the dependency and its transitive dependencies from repositories such as Maven Central.

---

# 2. What Is a Spring Boot Starter?

Spring Boot defines starters as **convenient dependency descriptors** that bring together the dependencies commonly needed for a particular capability. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

For example:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

means roughly:

> "I want to use Spring Data JPA/Hibernate."

The starter brings together the relevant dependencies.

So instead of manually adding a collection like:

```text
Spring Data JPA
Hibernate
Spring ORM
transaction support
etc.
```

you can use one starter.

Spring Boot calls this a **one-stop shop** for the dependencies needed for a capability. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 3. Is a Starter a Special Runtime Library?

This is an important interview trick.

A starter is primarily a **dependency descriptor**, not a magical runtime container.

Think:

```text
starter
   ↓
declares useful dependencies
   ↓
Maven/Gradle resolves them
   ↓
actual libraries end up on classpath
```

So:

```text
spring-boot-starter-webmvc
```

is mainly there to **pull together other dependencies**.

It doesn't mean:

```text
"Web functionality exists inside this one JAR."
```

The actual functionality comes from the dependencies it brings in.

---

# 4. Starter vs Auto-Configuration

This is probably the most important distinction in this topic.

### Starter

```text
Dependency convenience
```

### Auto-configuration

```text
Configuration convenience
```

Think:

```text
spring-boot-starter-data-jpa
          ↓
brings JPA/Hibernate dependencies
          ↓
classes become available
          ↓
Boot auto-configuration detects them
          ↓
conditional JPA configuration
          ↓
beans
```

So:

```text
Starter
    =
"bring the libraries"

Auto-configuration
    =
"configure the libraries"
```

They often work together, but they are completely different mechanisms.

Spring Boot's build documentation describes starters as dependency descriptors, while its auto-configuration documentation describes conditional configuration based on the application's classpath and other conditions. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 5. Example: Web Application

Suppose you add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Conceptually:

```text
starter-webmvc
      ↓
Spring MVC dependencies
Tomcat
JSON infrastructure
validation-related web support
etc.
      ↓
classpath populated
      ↓
Boot auto-configuration
      ↓
web infrastructure
```

The exact dependency graph depends on the Boot version.

That last point is important because dependency composition can change between Boot releases.

---

# 6. Current Boot Starter Naming

Since we're preparing for **current** Spring Boot interviews, don't blindly memorize older tutorials.

In current Spring Boot 4.1.1 documentation:

```text
spring-boot-starter-webmvc
```

is the dedicated Spring MVC + Tomcat starter.

The older:

```text
spring-boot-starter-web
```

is currently listed as deprecated in favor of:

```text
spring-boot-starter-webmvc
```

The same current docs list dedicated `webclient` and `webflux` starters. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

So when we later write examples, I'll distinguish **classic Boot 3-era naming** from **current Boot 4 naming** when it matters.

For interview purposes, you should recognize both.

---

# 7. Common Starters

Some important ones:

```text
spring-boot-starter
spring-boot-starter-webmvc
spring-boot-starter-webflux
spring-boot-starter-data-jpa
spring-boot-starter-jdbc
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-test
spring-boot-starter-actuator
spring-boot-starter-kafka
spring-boot-starter-data-redis
```

The current Boot starter list includes these and many others. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

Don't memorize the entire list.

Understand the pattern:

```text
spring-boot-starter-<technology>
```

---

# 8. Third-Party Starters

Official Spring Boot starters use:

```text
spring-boot-starter-*
```

Third-party projects shouldn't normally use the `spring-boot` prefix for their own starter because that namespace is reserved for official Boot artifacts. Spring Boot recommends names such as:

```text
thirdpartyproject-spring-boot-starter
```

for a project named `thirdpartyproject`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

This is a useful advanced detail when designing company libraries.

---

# 9. Why Don't We Specify Versions?

Look at a modern Maven project:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Notice:

```text
no version
```

Why does Maven know what version to use?

Because Spring Boot provides **dependency management**.

Each Boot release provides a curated list of supported dependency versions, exposed as a standard **BOM** named:

```text
spring-boot-dependencies
```

Spring Boot's build-system documentation explicitly states that you normally don't need to provide versions for dependencies managed by Boot. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 10. What Is a BOM?

BOM =

> **Bill of Materials**

A BOM is essentially a coordinated set of dependency versions.

Imagine your project needs:

```text
Spring Framework
Hibernate
Jackson
Tomcat
JUnit
Mockito
Logback
...
```

Instead of choosing each version independently:

```text
Spring = X
Hibernate = Y
Jackson = Z
Tomcat = A
```

the BOM says:

```text
For this Boot release,
use this compatible set of versions.
```

Conceptually:

```text
Spring Boot BOM
      |
      +-- Spring Framework → version A
      +-- Hibernate        → version B
      +-- Jackson          → version C
      +-- Tomcat           → version D
      +-- JUnit            → version E
```

This gives you **coordinated dependency versions**.

Spring Boot publishes `spring-boot-dependencies` as a standard BOM usable by Maven and Gradle. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 11. Why Is Dependency Management Important?

Imagine:

```text
Library A
   ↓
Jackson 2.x

Library B
   ↓
Jackson another-version
```

Now your application has competing version requirements.

Without central dependency management, large projects become difficult to keep consistent.

Spring Boot's curated dependency set gives you:

```text
consistent versions
+
tested combinations
+
easier upgrades
```

That's the real value.

---

# 12. Important: Dependency Management Does NOT Mean "No Versions Ever"

Spring Boot says:

> You generally don't need to specify versions for managed dependencies.

But you **can override** a managed version when necessary. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

For example, Maven might contain:

```xml
<properties>
    <jackson.version>...</jackson.version>
</properties>
```

or explicit dependency management.

But overriding Boot's recommendations can introduce compatibility problems. Boot's current documentation explicitly warns about this. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

So the practical rule:

> **Use Boot's managed versions unless you have a good reason to override one.**

---

# 13. What Is `spring-boot-starter-parent`?

This is where Maven-specific knowledge enters.

A typical Boot Maven project may have:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
    <relativePath/>
</parent>
```

Current Spring Boot documentation describes `spring-boot-starter-parent` as providing useful Maven defaults and dependency management. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

---

# 14. What Does the Parent POM Give You?

Conceptually:

```text
spring-boot-starter-parent
       |
       +-- dependency management
       +-- sensible Maven defaults
       +-- plugin configuration
       +-- compiler/default settings
       +-- resource filtering defaults
       +-- executable JAR repackaging configuration
       +-- other build conventions
```

The exact inherited configuration depends on the Boot version.

This is why a Boot Maven project can stay surprisingly small.

---

# 15. Parent POM vs BOM

This is a **very important interview question**.

They are related, but not the same.

### Parent POM

```text
spring-boot-starter-parent
```

provides:

```text
Maven inheritance
+
dependency management
+
plugin configuration/defaults
+
other Maven defaults
```

### BOM

```text
spring-boot-dependencies
```

provides:

```text
dependency version management
```

Think:

```text
Parent
   =
build configuration + dependency management

BOM
   =
dependency management only
```

That's the clean distinction.

---

# 16. Why Would I Not Use the Boot Parent?

This is a good real-world question.

Suppose your company already has:

```xml
<parent>
    <groupId>com.company</groupId>
    <artifactId>company-parent</artifactId>
</parent>
```

Maven supports only one parent.

You can't also do:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
</parent>
```

because a Maven POM can have only one parent.

So you can instead import Boot's BOM:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>4.1.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Now you get Boot's dependency version management without inheriting its parent.

This is an extremely useful enterprise pattern.

---

# 17. This Is a Common EPAM Question

### Interviewer:

> Can you use Spring Boot dependency management without using `spring-boot-starter-parent`?

Answer:

> Yes. Maven can import the `spring-boot-dependencies` BOM through `dependencyManagement`, so you can use Boot's curated dependency versions without inheriting from `spring-boot-starter-parent`.

That is an excellent answer.

Spring Boot explicitly provides `spring-boot-dependencies` as a standard BOM. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 18. Maven Example Without Boot Parent

For example:

```xml
<project>

    <parent>
        <groupId>com.company</groupId>
        <artifactId>company-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <dependencyManagement>
        <dependencies>

            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>4.1.1</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

        </dependencies>
    </dependencyManagement>

</project>
```

Then:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

can omit the version because the BOM manages it.

---

# 19. What Does `dependencyManagement` Actually Do?

This distinction is crucial.

Suppose:

```xml
<dependencyManagement>
    ...
</dependencyManagement>
```

contains:

```text
Hibernate → version X
Jackson  → version Y
```

That does **not automatically mean those dependencies are added to your project**.

Dependency management says:

> "If you use this dependency, use this version by default."

So:

```text
dependencyManagement
       ↓
controls versions

dependencies
       ↓
actually brings dependencies into the project
```

This distinction is frequently asked in Maven interviews.

---

# 20. Example

Suppose:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>...</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

This doesn't necessarily put Jackson on the classpath.

You still need:

```xml
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

The first controls the version; the second declares the actual dependency.

---

# 21. Transitive Dependencies

Now we hit another major interview concept.

Suppose your project directly declares:

```text
spring-boot-starter-data-jpa
```

but that starter itself depends on:

```text
Spring Data JPA
Hibernate
Spring ORM
other libraries
```

These are **transitive dependencies**.

Conceptually:

```text
Your App
   ↓
starter-data-jpa
   ↓
┌───────────────┬───────────────┐
↓               ↓               ↓
Spring Data   Hibernate      Spring ORM
```

Maven resolves these transitively.

This is why adding one starter can add many JARs.

---

# 22. Why Should You Care About Transitive Dependencies?

Because conflicts often happen **indirectly**.

Imagine:

```text
Your app
   |
   +-- Library A
   |      ↓
   |    Jackson 2.19
   |
   +-- Library B
          ↓
        Jackson 2.17
```

You never explicitly requested Jackson twice.

But you have two dependency paths.

This creates a dependency conflict.

---

# 23. Maven Dependency Tree

This is one of the most important troubleshooting commands:

```bash
mvn dependency:tree
```

Spring Boot's own tutorial uses `mvn dependency:tree` to inspect the project dependency graph. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

You'll see something conceptually like:

```text
com.example:order-service
+- spring-boot-starter-webmvc
|  +- spring-webmvc
|  +- ...
|
+- some-third-party-library
   \- jackson-databind
```

This tells you:

```text
who brought the dependency?
what version?
through which path?
```

---

# 24. Gradle Dependency Inspection

For Gradle, useful commands include:

```bash
./gradlew dependencies
```

and more targeted dependency insight:

```bash
./gradlew dependencyInsight \
    --dependency jackson-databind
```

This is very useful for identifying why a particular version was selected.

The exact Gradle commands depend on the project and configuration being inspected, but the concept is:

```text
dependency graph
       ↓
find selected version
       ↓
find who requested it
```

---

# 25. Version Conflict Scenario

Suppose:

```text
Library A
  ↓
Guava 30

Library B
  ↓
Guava 32
```

Maven has to choose a version.

You need to understand that dependency resolution is not simply:

> "Take every version."

A given classpath generally can't safely contain arbitrary multiple versions of the same library artifact.

Build tools apply dependency mediation/resolution rules.

This is why knowing the dependency tree matters.

---

# 26. "Nearest Dependency" in Maven

A classic Maven concept is:

> When multiple versions of the same dependency are encountered, Maven's dependency mediation favors the version that is nearest in the dependency tree.

For example:

```text
your app
├── A
│   └── X 1.0
└── B
    └── C
        └── X 2.0
```

`X 1.0` is closer to your project than `X 2.0`.

But don't stop there.

Maven can also use explicit dependency management to control the version.

That's where Spring Boot's BOM becomes powerful.

---

# 27. Direct Dependency Override

Suppose Boot's BOM manages:

```text
Jackson = version X
```

but your application directly declares:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>Y</version>
</dependency>
```

Now you are explicitly overriding the managed version.

This is possible, but Boot warns that overriding its recommendations may cause compatibility issues. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

So:

```text
Boot managed version
        ↓
safe default

explicit override
        ↓
your responsibility
```

---

# 28. Why Does Boot Manage Versions?

Imagine Boot 4.1.1 supports:

```text
Spring Framework X
Hibernate Y
Jackson Z
Tomcat A
```

These versions have been selected as a compatible stack.

If every application independently chooses:

```text
Spring 7.x
Hibernate arbitrary version
Jackson arbitrary version
```

you lose much of the benefit of the platform.

Boot's BOM gives:

```text
known combination
```

rather than:

```text
random combination of libraries
```

The current Boot documentation explicitly describes the dependency list as a **curated list of dependencies that it supports**. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 29. Spring Framework Version Management

Another important interview question:

> Should I specify the Spring Framework version separately?

Generally:

> **No.**

Spring Boot associates each Boot release with a base Spring Framework version and strongly recommends not specifying that version independently. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

For example, current Boot 4.1.1 requires Spring Framework 7.0.9 or later. ([docs.spring.io](https://docs.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

So don't write:

```xml
<version>some-random-spring-version</version>
```

for Spring Framework artifacts unless you have a very specific reason and understand the compatibility implications.

---

# 30. Spring Boot Parent and Plugin Management

The parent POM doesn't only manage dependency versions.

It can also manage/configure Maven plugins.

For example:

```text
compiler plugin
surefire
resources
Spring Boot Maven plugin
etc.
```

This reduces repetitive configuration.

Current Spring Boot's getting-started documentation notes that the `spring-boot-starter-parent` provides useful Maven defaults and also configures dependency management. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

---

# 31. The Spring Boot Maven Plugin

Don't confuse:

```text
spring-boot-starter-parent
```

with:

```text
spring-boot-maven-plugin
```

They are different.

### Parent

```text
Maven build defaults
+
dependency management
```

### Plugin

```text
Spring Boot-specific build operations
```

Most importantly:

```text
package/repackage
run
build-image
etc.
```

The Boot plugin can create an executable/fat JAR. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

---

# 32. Executable JAR

Suppose you run:

```bash
mvn package
```

with the Boot Maven plugin configured.

It can produce an executable JAR that contains:

```text
your application classes
+
dependencies
+
Boot loader
```

Then:

```bash
java -jar application.jar
```

can launch the application.

Spring Boot's build documentation describes this as creating an executable/fat JAR. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

---

# 33. Why Does the Parent Matter for Repackaging?

If you're using:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

the parent includes configuration that binds the plugin's `repackage` goal.

If you **don't** use the Boot parent, you can still use the plugin, but you need to configure its execution yourself. Spring Boot's current build documentation explicitly describes this. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

So:

```text
Boot Parent
    ↓
convenient plugin configuration

No Boot Parent
    ↓
you can still use plugin
but configure required execution explicitly
```

---

# 34. Maven Example — Typical Boot Project

A simplified current project might look like:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>

<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

</dependencies>

<build>
    <plugins>

        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

    </plugins>
</build>
```

Notice:

```text
no version on starters
```

because Boot manages them.

Current Boot 4.1.1's official documentation uses this style. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

---

# 35. Gradle

Gradle works differently.

A current Boot Gradle project can use:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.1'
    id 'io.spring.dependency-management' version '1.1.7'
}
```

and then:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

without specifying dependency versions because Boot's dependency-management plugin imports the corresponding BOM. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

---

# 36. Gradle Dependency Management

Current Spring Boot documentation says Gradle can manage dependencies in two main ways:

```text
1. io.spring.dependency-management plugin
2. Gradle's native BOM support
```

The former offers property-based customization of managed versions, while native BOM support may provide faster builds. ([docs.spring.io](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html?utm_source=chatgpt.com))

So if an interviewer asks:

> "Does Spring Boot require the Spring dependency-management plugin?"

The modern answer is:

> **No.** With Gradle, you can use Spring's dependency-management plugin or Gradle's native BOM support.

---

# 37. Gradle Native BOM

Conceptually:

```groovy
dependencies {
    implementation platform(
        'org.springframework.boot:spring-boot-dependencies:4.1.1'
    )

    implementation(
        'org.springframework.boot:spring-boot-starter-data-jpa'
    )
}
```

The exact Gradle style can vary between projects and Boot plugin configuration.

The important concept:

```text
BOM
 ↓
dependency versions
```

is not inherently Maven-only.

Gradle understands BOMs too. ([docs.spring.io](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html?utm_source=chatgpt.com))

---

# 38. Spring Boot Plugin vs Dependency Management Plugin

Another common trap.

### Spring Boot Gradle plugin

```text
org.springframework.boot
```

handles Spring Boot build behavior such as executable packaging.

### Dependency Management plugin

```text
io.spring.dependency-management
```

helps manage dependency versions through imported Maven BOMs.

They can be used together, but they solve different problems. ([docs.spring.io](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html?utm_source=chatgpt.com))

---

# 39. Starter Does Not Mean "Everything We Might Need"

Suppose:

```text
spring-boot-starter-data-jpa
```

is present.

It does **not** mean you automatically get:

```text
Kafka
Redis
AWS SDK
MongoDB
```

Starters are capability-specific.

Think:

```text
starter-data-jpa
    ↓
JPA-related dependency set
```

not:

```text
starter-data-jpa
    ↓
all of Spring
```

---

# 40. What Is `spring-boot-starter`?

The core starter provides a base set of Boot functionality, including auto-configuration support, logging, and YAML support in current Boot documentation. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

You may encounter:

```xml
<artifactId>spring-boot-starter</artifactId>
```

as a foundational dependency.

---

# 41. What Happens When You Add a Starter?

Let's trace it:

```text
You add:
spring-boot-starter-data-jpa
         ↓
Maven resolves starter POM
         ↓
transitive dependencies resolved
         ↓
JPA/Hibernate classes on classpath
         ↓
Boot auto-configuration sees them
         ↓
conditions evaluated
         ↓
JPA infrastructure configured
```

This is one of the most important complete mental models from today's topic.

---

# 42. Dependency Graph vs Bean Graph

This is another subtle distinction.

### Dependency graph

Build-time concern:

```text
your-app
   ↓
starter
   ↓
library
   ↓
another library
```

This happens at Maven/Gradle dependency resolution time.

### Bean graph

Runtime Spring concern:

```text
OrderController
   ↓
OrderService
   ↓
OrderRepository
   ↓
DataSource
```

Spring creates and wires this at runtime.

So:

```text
Maven/Gradle
    ↓
JAR dependencies

Spring
    ↓
application beans
```

Don't mix them up.

---

# 43. A Dependency Does Not Automatically Become a Bean

This is another important interview trap.

Suppose you add:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-library</artifactId>
</dependency>
```

That puts classes on the classpath.

It does **not automatically mean**:

```text
Every class in the library
    ↓
Spring Bean
```

Some libraries provide their own Spring configuration/auto-configuration, but merely having a JAR on the classpath does not make every class a bean.

Remember:

```text
dependency
    ≠
Spring bean
```

This connects directly to our auto-configuration discussion.

---

# 44. Dependency Conflict — Real Example

Imagine:

```text
Your Application
       |
       +── Library A
       |      ↓
       |   Jackson 2.18
       |
       +── Library B
              ↓
           Jackson 2.20
```

You notice a runtime error:

```text
NoSuchMethodError
```

or:

```text
ClassNotFoundException
```

or:

```text
NoClassDefFoundError
```

A common cause is a binary compatibility mismatch caused by inconsistent library versions.

Your investigation:

```bash
mvn dependency:tree
```

Then:

```text
Which Jackson version?
Who brought it?
Which dependency path?
```

That's the correct debugging mindset.

---

# 45. Why Can Compile Succeed but Runtime Fail?

This is another useful Java/Spring interview scenario.

You can have:

```text
compile-time classpath
    ↓
one version
```

but runtime packaging/classpath can contain:

```text
different version
```

or another library may be incompatible.

Then:

```text
compile succeeds
      ↓
application starts
      ↓
specific method invoked
      ↓
NoSuchMethodError
```

This is why dependency conflicts often appear as runtime linkage errors rather than compile errors.

---

# 46. Excluding a Transitive Dependency

Suppose:

```text
Library A
   ↓
old-json-library
```

but you want your application to use another compatible version.

Maven supports:

```xml
<exclusions>
    <exclusion>
        <groupId>...</groupId>
        <artifactId>...</artifactId>
    </exclusion>
</exclusions>
```

Conceptually:

```text
Library A
    |
    +-- unwanted transitive dependency ❌
```

Then you can add the version you actually want explicitly/through dependency management.

But don't use exclusions randomly.

First identify the **dependency graph** and the compatibility reason.

---

# 47. Dependency Management vs Exclusion

Different tools solve different problems.

### Dependency Management

```text
"I want dependency X at version Y."
```

### Exclusion

```text
"I don't want this transitive dependency brought in through this path."
```

Often used together:

```text
exclude old dependency
+
declare/manage desired version
```

---

# 48. `mvn dependency:tree` Is Your Friend

When a dependency issue occurs:

```bash
mvn dependency:tree
```

You may also focus on one artifact:

```bash
mvn dependency:tree \
    -Dincludes=com.fasterxml.jackson.core:jackson-databind
```

The exact filtering syntax can be expanded for more targeted analysis.

The point is:

```text
don't guess
↓
inspect dependency graph
```

---

# 49. Maven Dependency Management vs Spring Dependency Injection

These two terms look dangerously similar:

```text
Dependency Management
Dependency Injection
```

They are completely different.

### Maven Dependency Management

```text
Which library versions should my build use?
```

### Spring Dependency Injection

```text
Which application object should Spring inject here?
```

So:

```text
Maven
    ↓
JAR dependencies

Spring
    ↓
Object dependencies
```

This is an excellent interview distinction.

---

# 50. A Very Common Interview Question

### "What's the difference between a starter and a dependency?"

Answer:

> A dependency is a specific library artifact required by the application. A Spring Boot starter is a convenient dependency descriptor that groups a set of related dependencies for a particular capability.

Excellent.

---

# 51. Another Common Question

### "What is a BOM?"

Answer:

> A BOM, or Bill of Materials, is a dependency-management artifact that defines a coordinated set of dependency versions. Spring Boot publishes `spring-boot-dependencies`, which provides the curated versions supported by a particular Boot release. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 52. Another Common Question

### "Why don't you specify versions for Spring Boot dependencies?"

Answer:

> Because Spring Boot provides dependency management through its curated BOM. When using the Boot parent or importing the `spring-boot-dependencies` BOM, managed dependencies can generally omit their individual version tags. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 53. Another Common Question

### "Can you override a Boot-managed dependency version?"

Answer:

> Yes, but it should be done carefully because Boot's versions form a curated compatibility set. Overriding one can introduce compatibility issues. ([docs.spring.io](https://docs.spring.io/spring-boot/how-to/build.html?utm_source=chatgpt.com))

---

# 54. Another Common Question

### "What is the difference between `spring-boot-starter-parent` and `spring-boot-dependencies`?"

Strong answer:

> `spring-boot-starter-parent` is a Maven parent POM that provides Maven defaults, plugin configuration, and dependency management. `spring-boot-dependencies` is a BOM focused on dependency version management. You can import the BOM without inheriting from the Boot parent, which is useful when your project already has its own parent POM. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

---

# 55. Another Common Question

### "How would you troubleshoot a dependency conflict?"

A strong answer:

> I'd inspect the resolved dependency graph with `mvn dependency:tree` or Gradle's dependency reporting/`dependencyInsight`, identify which paths introduce the conflicting artifact, determine the version actually selected, and then use dependency management, an explicit compatible version, or a targeted exclusion where appropriate. I'd also verify that the selected version is compatible with the Spring Boot release rather than overriding versions blindly.

That's a production-quality answer.

---

# 56. EPAM Scenario

### Interviewer:

> "Your application was upgraded to a new Spring Boot version and now a third-party library throws `NoSuchMethodError`. What could be happening?"

Think:

```text
Spring Boot upgrade
      ↓
dependency versions changed
      ↓
third-party library may expect another version
      ↓
binary incompatibility
      ↓
NoSuchMethodError
```

Your first investigation:

```text
dependency tree
```

Then determine:

```text
old library expectation
vs
resolved runtime version
```

Don't immediately downgrade Spring Boot.

---

# 57. EPAM Scenario

> "Why did adding one library unexpectedly change the version of another library?"

Potential explanation:

```text
new dependency
   ↓
transitive dependencies
   ↓
dependency graph changed
   ↓
version mediation/management
   ↓
resolved version changed
```

The correct answer depends on the build tool and dependency graph, but the underlying concept is **transitive dependency resolution**.

---

# 58. EPAM Scenario

> "Our organization already has a corporate Maven parent. How can we still use Spring Boot's dependency versions?"

Answer:

```text
don't use Boot parent
      ↓
import spring-boot-dependencies BOM
      ↓
retain company parent
+
Boot dependency management
```

That's a very realistic enterprise setup.

---

# 59. EPAM Scenario

> "What is the difference between a Spring Boot starter and auto-configuration?"

Answer:

```text
starter
  → dependency grouping

auto-configuration
  → conditional runtime/application configuration
```

Then explain:

```text
starter adds libraries
      ↓
classes become available
      ↓
auto-configuration detects them
      ↓
conditions
      ↓
beans
```

---

# 60. EPAM Scenario

> "We have `spring-boot-starter-data-jpa`. Does that mean Hibernate automatically exists as a Spring bean?"

No.

Better answer:

> The starter brings Hibernate and related dependencies onto the classpath. Spring Boot's JPA auto-configuration then conditionally creates and configures the relevant Spring infrastructure beans. A dependency being on the classpath and an object being a Spring bean are different concepts.

That's a very strong answer.

---

# 61. One Major Mental Model

This is the diagram to remember:

```text
                    Maven / Gradle
                         |
                         ↓
                 Spring Boot Starter
                         |
                         ↓
              Transitive Dependencies
                         |
                         ↓
                      Classpath
                         |
                         ↓
               Spring Boot Auto-Config
                         |
                     Conditions
                         |
                         ↓
                Spring Bean Definitions
                         |
                         ↓
                  Spring ApplicationContext
                         |
                         ↓
                     Actual Beans
```

And separate from it:

```text
BOM
 ↓
Which versions?

Starter
 ↓
Which dependency group?

Auto-configuration
 ↓
How should those dependencies be configured?
```

That's the entire topic in one picture.

---

# 62. Current Spring Boot 4.1.1 — Useful Interview Note

The current official Spring Boot documentation says:

```text
Spring Boot = 4.1.1
```

and currently requires:

```text
Java 17+
```

with Maven 3.6.3+ and Gradle 8.14+/9.x supported. ([docs.spring.io](https://docs.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

The current starter catalog also contains some newer dedicated starters and deprecations—for example, `spring-boot-starter-web` is deprecated in favor of `spring-boot-starter-webmvc`. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

For interviews, however, the underlying concepts remain:

```text
starter
BOM
dependency management
transitive dependencies
dependency conflicts
parent POM
Maven/Gradle plugin
```

---

# 63. Interview Revision Sheet

### Starter

> Convenient dependency descriptor grouping related dependencies. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

### BOM

> Bill of Materials containing coordinated dependency versions.

### `spring-boot-dependencies`

> Spring Boot's dependency-management BOM. ([docs.spring.io](https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com))

### `spring-boot-starter-parent`

> Maven parent providing useful build defaults, plugin configuration, and dependency management. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

### `dependencyManagement`

> Controls versions/management; does not by itself mean the dependency is added to the classpath.

### Transitive dependency

> A dependency brought in indirectly by another dependency.

### `mvn dependency:tree`

> Inspect the resolved Maven dependency graph. ([docs.spring.io](https://docs.spring.io/spring-boot/tutorial/first-application/))

### Dependency conflict

> Multiple dependency paths require different versions of the same artifact.

### Dependency Injection

> Runtime object wiring in Spring.

### Dependency Management

> Build-time dependency version/control.

Don't confuse the last two.

---

# 64. What I Want You to Be Able to Explain

Given:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

you should mentally see:

```text
                   starter-data-jpa
                          |
              ┌───────────┼───────────┐
              ↓           ↓           ↓
        Spring Data JPA  Hibernate   related libs
                          |
                          ↓
                    Classpath
                          |
                          ↓
                JPA Auto-Configuration
                          |
                          ↓
              EntityManagerFactory
              Transaction infrastructure
              Repository infrastructure
                          |
                          ↓
                    Spring Beans
```

And above the entire dependency graph:

```text
Spring Boot BOM
      ↓
compatible dependency versions
```

That's the understanding interviewers are looking for.

---

# 65. The 8 Questions You Should Know Cold

**1. What is a Spring Boot starter?**

A convenient dependency descriptor grouping related dependencies.

**2. Starter vs auto-configuration?**

Starter manages **dependencies**; auto-configuration manages **application configuration**.

**3. What is a BOM?**

A coordinated dependency-version catalog.

**4. Why don't Boot dependencies need versions?**

Because Boot's dependency management/BOM supplies them.

**5. Parent vs BOM?**

Parent = Maven inheritance + build defaults + dependency management.

BOM = dependency version management.

**6. Can you use Boot's BOM without the parent?**

Yes.

**7. How do you inspect Maven dependency conflicts?**

```bash
mvn dependency:tree
```

**8. Why can adding one dependency change another dependency's version?**

Because of transitive dependency resolution and version-management/mediation rules.

---

# Next Topic — Spring MVC and Request Lifecycle

We've now covered the **Spring Boot foundation** very thoroughly:

```text
1. Spring vs Spring Boot
2. IoC / DI
3. IoC Container
4. Bean Lifecycle
5. Bean Scopes
6. Dependency Resolution
7. Component Scanning
8. @Configuration / @Bean
9. Auto-Configuration
10. External Configuration
11. SpringApplication Startup
12. Starters / Dependency Management
```

Now we're moving into one of the **most important practical areas for a Spring Boot developer**:

# Topic 13 — Spring MVC Architecture + DispatcherServlet + HTTP Request Lifecycle

We'll trace an actual request:

```text
Client
  ↓
HTTP Request
  ↓
Embedded Tomcat
  ↓
Servlet Filter
  ↓
DispatcherServlet
  ↓
HandlerMapping
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
return Object
  ↓
HttpMessageConverter
  ↓
JSON
  ↓
HTTP Response
```

And we'll go deep into the EPAM-style questions:

> **What exactly is `DispatcherServlet`?**

> **How does Spring find the correct controller method?**

> **What is `HandlerMapping`?**

> **What is `HandlerAdapter`?**

> **Where does Jackson fit?**

> **Filter vs Interceptor?**

> **What happens before the controller?**

> **What happens after the controller?**

> **How does `@RequestBody` actually work?**

> **How does Spring convert a Java object into JSON?**

This is the point where your Spring Boot knowledge starts becoming **real REST API internals knowledge**.

