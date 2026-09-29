# 3. Builder Design Pattern

The **Builder Pattern** is a creational design pattern used when creating an object involves **many optional parameters, multiple construction steps, or complicated configuration**.

Its core idea is:

> **Separate the process of constructing an object from the final object itself.**

We'll build this from the problem first, because Builder becomes very easy once the problem is clear.

---

# 1. The problem Builder solves

Imagine this class:

```java
public class User {

    private String firstName;
    private String lastName;
    private int age;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private boolean active;
}
```

Now suppose we have this constructor:

```java
public User(
        String firstName,
        String lastName,
        int age,
        String email,
        String phone,
        String address,
        String city,
        String country,
        boolean active) {

    // ...
}
```

Creating an object becomes ugly:

```java
User user = new User(
        "John",
        "Doe",
        30,
        "john@example.com",
        "1234567890",
        "Street 1",
        "Hyderabad",
        "India",
        true
);
```

Immediately we have problems:

* What does each argument mean?
* Which arguments are optional?
* What if we don't have a phone?
* What if we don't have an address?
* What happens when we add another parameter?

This is called the **telescoping constructor problem**.

---

# 2. Telescoping constructors

One traditional attempt is to provide multiple constructors:

```java
public User(String firstName, String lastName) {
    // ...
}

public User(String firstName, String lastName, int age) {
    // ...
}

public User(String firstName, String lastName, int age, String email) {
    // ...
}

public User(
        String firstName,
        String lastName,
        int age,
        String email,
        String phone) {
    // ...
}
```

This quickly becomes messy.

Imagine 10 optional parameters.

The number of possible constructor combinations grows dramatically.

That's where Builder comes in.

---

# 3. Basic Builder idea

Instead of:

```java
new User(
    "John",
    "Doe",
    30,
    "john@example.com",
    ...
);
```

we can write:

```java
User user = User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .email("john@example.com")
        .phone("1234567890")
        .city("Hyderabad")
        .country("India")
        .active(true)
        .build();
```

Now the code is much more readable.

You can immediately see what each value represents.

---

# 4. Simplest Builder implementation

Let's implement it manually.

```java
public class User {

    private String firstName;
    private String lastName;
    private int age;
    private String email;

    private User() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String firstName;
        private String lastName;
        private int age;
        private String email;

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public User build() {

            User user = new User();

            user.firstName = this.firstName;
            user.lastName = this.lastName;
            user.age = this.age;
            user.email = this.email;

            return user;
        }
    }
}
```

Usage:

```java
User user = User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .email("john@example.com")
        .build();
```

---

# 5. Let's understand the structure

There are really two objects involved during construction.

```text
             User
              ▲
              │ build()
              │
          User.Builder
```

The `Builder` holds the values being assembled.

Eventually:

```java
build()
```

creates the final:

```java
User
```

So:

```text
Builder
   │
   │ collects values
   ▼
build()
   │
   ▼
User
```

---

# 6. Why does each setter return `this`?

Consider:

```java
public Builder firstName(String firstName) {
    this.firstName = firstName;
    return this;
}
```

We return the current Builder object.

That enables:

```java
User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .email("john@example.com")
        .build();
```

This is called **method chaining**.

Conceptually:

```text
builder()
   ↓
.firstName(...)
   ↓
.lastName(...)
   ↓
.age(...)
   ↓
.email(...)
   ↓
.build()
```

Each intermediate call returns the same Builder.

---

# 7. Why not just use setters?

You might ask:

> "Why not make `User` mutable and just call setters?"

For example:

```java
User user = new User();

user.setFirstName("John");
user.setLastName("Doe");
user.setAge(30);
user.setEmail("john@example.com");
```

This can work, but Builder has important advantages.

### 1. Better construction readability

```java
User.builder()
    .firstName("John")
    .lastName("Doe")
    .age(30)
    .build();
```

### 2. Can keep the final object immutable

The `User` can have:

```java
private final String firstName;
```

and no setters.

### 3. Validation can happen before construction

```java
build()
```

can validate everything.

### 4. Prevent partially constructed objects

With setters, someone can have:

```text
User exists
but only half of its state is initialized
```

Builder allows the object to be created only at the end.

---

# 8. The preferred immutable Builder

A much better design is:

```java
public class User {

    private final String firstName;
    private final String lastName;
    private final int age;
    private final String email;

    private User(Builder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.age = builder.age;
        this.email = builder.email;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String firstName;
        private String lastName;
        private int age;
        private String email;

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```

Now:

```java
User user = User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .email("john@example.com")
        .build();
```

The resulting `User` is immutable because:

* fields are `final`
* no setters exist
* state is assigned during construction

---

# 9. Why does Builder have its own fields?

Notice:

```java
private String firstName;
```

exists in `Builder`.

Then:

```java
private final String firstName;
```

exists in `User`.

They serve different purposes.

During construction:

```text
Builder.firstName
       ↓
User.firstName
```

The builder stores the temporary configuration.

`build()` transfers it into the final object.

---

# 10. Validation inside `build()`

This is one of Builder's biggest practical advantages.

Suppose:

```text
firstName → required
email     → required
age       → must be >= 18
```

We can write:

```java
public User build() {

    if (firstName == null || firstName.isBlank()) {
        throw new IllegalArgumentException(
                "First name is required"
        );
    }

    if (email == null || email.isBlank()) {
        throw new IllegalArgumentException(
                "Email is required"
        );
    }

    if (age < 18) {
        throw new IllegalArgumentException(
                "User must be at least 18"
        );
    }

    return new User(this);
}
```

Now invalid objects cannot easily be created through the Builder.

---

# 11. Builder is especially useful for optional parameters

Suppose:

```text
Required:
    name

Optional:
    age
    email
    phone
    address
    city
    country
```

Instead of:

```java
new User("John", null, 0, null, null, null, null)
```

we can write:

```java
User.builder()
        .name("John")
        .email("john@example.com")
        .city("Hyderabad")
        .build();
```

Only the values we need are provided.

This is one of the main reasons Builder exists.

---

# 12. Builder and immutability

Builder and immutable objects are often used together.

For example:

```java
public final class User {

    private final String name;
    private final int age;

    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    // ...
}
```

After construction:

```text
User
├── name = John
└── age = 30
```

The object's state doesn't change.

This makes immutable objects easier to reason about.

However:

> **Builder itself does not automatically make an object immutable.**

You could build a mutable object too.

The Builder is a **construction mechanism**, not an immutability mechanism by itself.

---

# 13. What is the "Director"?

If you study the classic GoF Builder pattern, you'll encounter a component called a **Director**.

Classic structure:

```text
Director
    │
    ▼
Builder
    │
    ▼
Product
```

The Director controls the construction process.

For example:

```java
public class ComputerDirector {

    private final ComputerBuilder builder;

    public ComputerDirector(ComputerBuilder builder) {
        this.builder = builder;
    }

    public Computer buildGamingComputer() {
        return builder
                .cpu("Intel i9")
                .ram(32)
                .storage(2000)
                .gpu("RTX")
                .build();
    }
}
```

Then:

```java
ComputerBuilder builder = new ComputerBuilder();

ComputerDirector director =
        new ComputerDirector(builder);

Computer computer =
        director.buildGamingComputer();
```

---

# 14. Is Director mandatory?

No.

This is important.

Modern Java Builder implementations usually don't have a separate Director.

For example:

```java
User.builder()
    .name("John")
    .age(30)
    .build();
```

is still commonly called the Builder pattern.

The classic GoF pattern describes a Director, but practical implementations often omit it because the fluent Builder itself is enough.

---

# 15. Classic Builder structure

The textbook pattern looks like:

```text
                 Director
                    │
                    │ controls construction
                    ▼
              Builder interface
                 /       \
                /         \
               ▼           ▼
      ConcreteBuilder1  ConcreteBuilder2
               │
               ▼
             Product
```

The Director can use the builder to construct different variants.

---

# 16. Why was Builder originally introduced?

Imagine a complex object:

```text
Computer
├── CPU
├── RAM
├── Storage
├── GPU
├── Motherboard
├── Cooling
├── Power supply
└── Operating system
```

Different configurations:

```text
Basic Computer
Gaming Computer
Workstation
Server
```

The construction steps may be similar, but the configuration differs.

Builder can separate:

```text
Construction process
        from
Object representation/configuration
```

---

# 17. Example: Computer Builder

```java
public class Computer {

    private final String cpu;
    private final int ram;
    private final int storage;
    private final String gpu;

    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.storage = builder.storage;
        this.gpu = builder.gpu;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String cpu;
        private int ram;
        private int storage;
        private String gpu;

        public Builder cpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public Builder ram(int ram) {
            this.ram = ram;
            return this;
        }

        public Builder storage(int storage) {
            this.storage = storage;
            return this;
        }

        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Computer build() {
            return new Computer(this);
        }
    }
}
```

Usage:

```java
Computer gamingPc = Computer.builder()
        .cpu("Intel i9")
        .ram(32)
        .storage(2000)
        .gpu("RTX")
        .build();
```

Basic PC:

```java
Computer basicPc = Computer.builder()
        .cpu("Intel i5")
        .ram(16)
        .storage(512)
        .build();
```

Same object type:

```text
Computer
```

Different configurations.

---

# 18. Builder vs Factory

This is a very common interview question.

### Factory

Factory is primarily about:

> **Which object should be created?**

Example:

```java
Notification notification =
        NotificationFactory.create("EMAIL");
```

Possible products:

```text
EmailNotification
SmsNotification
PushNotification
```

### Builder

Builder is primarily about:

> **How should this object be configured/built?**

Example:

```java
User user = User.builder()
        .name("John")
        .age(30)
        .email("john@example.com")
        .build();
```

The product is:

```text
User
```

but there are many possible configurations.

So:

```text
Factory
→ choose product

Builder
→ configure/build product
```

---

# 19. Factory + Builder together

They can absolutely be combined.

For example:

```java
Notification notification =
        NotificationFactory
                .builderFor("EMAIL")
                .recipient("john@example.com")
                .message("Hello")
                .priority(HIGH)
                .build();
```

Conceptually:

```text
Factory
   ↓
chooses type
   ↓
Builder
   ↓
configures object
   ↓
final object
```

This is common in real systems.

---

# 20. Builder vs Constructor

### Constructor

Best when:

```text
few parameters
mostly required values
simple construction
```

Example:

```java
User user = new User("John", 30);
```

### Builder

Useful when:

```text
many parameters
many optional parameters
complex validation
different configurations
readability matters
```

Example:

```java
User user = User.builder()
        .name("John")
        .age(30)
        .email("john@example.com")
        .city("Hyderabad")
        .build();
```

A constructor isn't bad.

Don't introduce Builder merely because a class has two fields.

---

# 21. Builder and inheritance

Builder gets more complicated when inheritance is involved.

Suppose:

```java
class Vehicle {
    String brand;
}

class Car extends Vehicle {
    int doors;
}
```

A normal Builder can become awkward because the child builder needs to support parent fields.

In such cases, designs such as **self-referencing generic builders** can be used.

Example:

```java
public class Vehicle {

    protected String brand;

    public static class Builder<T extends Builder<T>> {

        protected String brand;

        public T brand(String brand) {
            this.brand = brand;
            return self();
        }

        protected T self() {
            return (T) this;
        }
    }
}
```

This is more advanced and usually isn't necessary unless the interviewer specifically asks about Builder + inheritance.

The important lesson is:

> Builder is simplest and cleanest for standalone immutable classes.

---

# 22. Builder and collections

Suppose a `Team` contains players.

You might write:

```java
Team team = Team.builder()
        .name("Warriors")
        .player("Player 1")
        .player("Player 2")
        .player("Player 3")
        .build();
```

The Builder can internally maintain:

```java
private final List<String> players =
        new ArrayList<>();
```

and:

```java
public Builder player(String player) {
    players.add(player);
    return this;
}
```

Then:

```java
public Team build() {
    return new Team(
            name,
            List.copyOf(players)
    );
}
```

`List.copyOf()` is useful for creating an unmodifiable snapshot.

This is a common approach when building immutable objects containing collections.

---

# 23. Builder can provide domain-specific methods

Instead of generic setters:

```java
.width(100)
.height(200)
.color("RED")
```

you can create meaningful methods:

```java
.withDimensions(100, 200)
.red()
.large()
.withPremiumFeatures()
```

For example:

```java
Order order = Order.builder()
        .customer(customer)
        .addItem(item1)
        .addItem(item2)
        .applyDiscount()
        .priority()
        .build();
```

That can make the construction API express the domain better.

---

# 24. Staged Builder

A more advanced technique is the **Staged Builder**.

It can enforce required fields at compile time.

Suppose:

```text
name → required
email → required
age → optional
```

Instead of allowing:

```java
User.builder().age(30).build();
```

we can design the Builder API so `build()` is not available until the required fields are supplied.

Conceptually:

```java
User.builder()
    .name("John")
    .email("john@example.com")
    .age(30)
    .build();
```

but:

```java
User.builder()
    .age(30)
    .build();
```

doesn't compile.

This can be powerful, but it adds considerable complexity.

For most applications, runtime validation inside `build()` is simpler.

---

# 25. Lombok's `@Builder`

In Spring Boot projects, you'll often encounter:

```java
@Builder
public class User {

    private String name;
    private int age;
    private String email;
}
```

Lombok generates builder-related code for you.

Then:

```java
User user = User.builder()
        .name("John")
        .age(30)
        .email("john@example.com")
        .build();
```

This is extremely common in Java projects.

But in an interview, don't confuse:

```text
Lombok's @Builder
```

with:

```text
understanding Builder Pattern
```

You should still understand the manual implementation.

---

# 26. Builder and Lombok `@Value`

A very common combination is:

```java
@Value
@Builder
public class User {

    String name;
    int age;
    String email;
}
```

Conceptually:

```text
@Value
    ↓
immutable object

@Builder
    ↓
readable object construction
```

This is a common pattern in modern Java codebases.

---

# 27. Builder and records

Java records already provide concise immutable data carriers:

```java
public record User(
        String name,
        int age,
        String email
) {
}
```

Creation is simple:

```java
User user =
        new User("John", 30, "john@example.com");
```

For small records, that's often enough.

Builder becomes more useful when you have:

* many optional fields
* validation
* complex construction
* multiple configuration paths

You can even define a custom Builder for a record, but don't add one automatically.

---

# 28. Common mistake: Builder with a mutable product

Imagine:

```java
public class User {

    private String name;

    public void setName(String name) {
        this.name = name;
    }
}
```

and:

```java
User user = User.builder()
        .name("John")
        .build();

user.setName("Mike");
```

Builder was used, but `User` is still mutable.

That's not inherently wrong.

It simply means:

> **Builder does not imply immutability.**

Builder controls **construction**.

---

# 29. Common mistake: Builder without `build()`

A proper Builder normally has some final operation:

```java
.build();
```

That operation produces the product.

For example:

```java
public User build() {
    validate();
    return new User(this);
}
```

This provides a clear boundary:

```text
Building phase
     ↓
build()
     ↓
Finished object
```

---

# 30. Common mistake: Builder does everything

You don't want a Builder becoming a giant business-logic class.

For example:

```java
builder.calculateTax()
       .chargeCreditCard()
       .sendEmail()
       .createDatabaseRecord()
       .build();
```

That's a smell.

Builder should primarily deal with:

```text
object construction/configuration
```

not unrelated business operations.

---

# 31. Advantages of Builder

### Readability

Compare:

```java
new User("John", "Doe", 30, "x@y.com", null, null, "India", true);
```

with:

```java
User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .email("x@y.com")
        .country("India")
        .active(true)
        .build();
```

The second clearly communicates intent.

---

### Handles optional parameters

You don't need dozens of constructors.

---

### Immutability

Builder works very naturally with immutable objects.

---

### Validation

`build()` provides a single place to validate object state.

---

### Different configurations

One Builder can produce many valid configurations.

---

# 32. Disadvantages of Builder

Builder isn't free.

### More code

Without Lombok, a Builder requires:

```text
Builder class
fields
methods
build()
validation
```

### More classes/objects

You temporarily create the Builder object before creating the actual product.

Usually this isn't an important problem, but it's still an additional abstraction.

### Overkill for simple classes

For:

```java
Point(int x, int y)
```

Builder would usually be unnecessary.

This would be enough:

```java
Point point = new Point(10, 20);
```

---

# 33. Builder and Single Responsibility Principle

Builder can separate two responsibilities:

```text
User
   ↓
represents User

User.Builder
   ↓
constructs User
```

Instead of putting complicated construction logic into the product class itself.

This can improve maintainability, although Builder should not be introduced solely to satisfy SRP mechanically.

---

# 34. A realistic e-commerce example

Suppose we have:

```text
Order
├── customer
├── address
├── items
├── coupon
├── paymentMethod
├── shippingMethod
├── giftWrap
└── priority
```

A Builder lets you express:

```java
Order order = Order.builder()
        .customer(customer)
        .shippingAddress(address)
        .addItem(item1)
        .addItem(item2)
        .coupon("SAVE20")
        .paymentMethod("UPI")
        .shippingMethod("EXPRESS")
        .giftWrap(true)
        .priority(true)
        .build();
```

Compare that with a giant constructor:

```java
new Order(
    customer,
    address,
    items,
    "SAVE20",
    "UPI",
    "EXPRESS",
    true,
    true
);
```

The Builder version is much easier to understand and maintain.

---

# 35. How Builder works internally

This:

```java
User user = User.builder()
        .name("John")
        .age(30)
        .email("john@example.com")
        .build();
```

roughly behaves like:

```text
Step 1:
Create Builder

Step 2:
Set name = John

Step 3:
Set age = 30

Step 4:
Set email = john@example.com

Step 5:
build()

Step 6:
Validate

Step 7:
Create User

Step 8:
Copy Builder state into User

Step 9:
Return immutable User
```

So Builder is essentially a **controlled object-construction workflow**.

---

# 36. Interview question: "Why use Builder instead of constructor?"

Good answer:

> "A Builder is useful when an object has many parameters, especially optional ones, because it avoids telescoping constructors and improves readability. It also allows validation during construction and works well with immutable objects."

---

# 37. Interview question: "Why return `this` from Builder methods?"

Answer:

> "Returning `this` enables fluent method chaining, allowing multiple configuration methods to be called in sequence before `build()`."

Example:

```java
builder.name("John")
       .age(30)
       .email("...")
       .build();
```

---

# 38. Interview question: "Can Builder create immutable objects?"

Answer:

> "Yes. A Builder is commonly used to construct immutable objects. The final class can have private final fields, no setters, and a constructor that receives the completed Builder state."

Important:

> Builder itself does not guarantee immutability.

---

# 39. Interview question: "Is Director mandatory?"

Answer:

> "No. The original GoF Builder structure includes a Director, but modern Java implementations commonly use a fluent Builder directly and omit the Director when it isn't necessary."

That's a strong answer.

---

# 40. Interview question: "Builder vs Factory?"

A polished interview answer:

> "Factory focuses primarily on selecting or creating the appropriate product implementation, while Builder focuses on constructing and configuring a particular product, especially when it has many optional properties or construction steps."

Example:

```text
Factory
→ EmailNotification vs SmsNotification

Builder
→ configure one Notification/User/Order
```

---

# 41. Interview question: "Why is Builder especially useful with immutable objects?"

Because immutable objects often need all their state supplied at construction time.

Instead of:

```text
create empty object
      ↓
set field
      ↓
set field
      ↓
set field
```

Builder lets us do:

```text
configure Builder
      ↓
validate
      ↓
create fully initialized object
```

The final object never needs setters.

---

# 42. Interview question: "Does Builder always use a nested static class?"

No.

This is just a common Java implementation:

```java
public static class Builder {
}
```

The Builder can be:

* nested
* separate
* an interface
* implemented by another class

The pattern is about the **construction approach**, not specifically the syntax.

---

# 43. Builder mental model

Remember this:

```text
              BUILDER
                 │
      ┌──────────┼──────────┐
      ▼          ▼          ▼
    name       age        email
      │          │          │
      └──────────┼──────────┘
                 ▼
               build()
                 │
                 ▼
              PRODUCT
```

Factory:

```text
        Factory
           │
     Which product?
           ▼
   ┌───────┼───────┐
   ▼       ▼       ▼
   A       B       C
```

Builder:

```text
       Builder
          │
    How configure it?
          ▼
    ┌─────┼─────┐
    ▼     ▼     ▼
  name   age  email
          │
          ▼
        build()
          │
          ▼
       Product
```

---

# 44. Builder Pattern — interview cheat sheet

### What?

A creational pattern that separates **object construction** from the final representation.

### When?

Use it when an object has:

```text
many parameters
many optional parameters
complex construction
multiple configurations
validation requirements
```

### Main benefits

```text
Readable construction
Optional parameters
Immutable objects
Centralized validation
Avoids telescoping constructors
```

### Typical Java implementation

```java
public class User {

    private final String name;
    private final int age;

    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;
        private int age;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```

Usage:

```java
User user = User.builder()
        .name("John")
        .age(30)
        .build();
```

---

# 45. The three creational patterns we've covered

You now have:

```text
CREATIONAL
│
├── Singleton
│     └── Control instance count
│
├── Factory
│     └── Encapsulate/select object creation
│
└── Builder
      └── Configure and construct complex objects
```

A useful interview mental model is:

```text
Singleton
→ HOW MANY?

Factory
→ WHICH ONE?

Builder
→ HOW TO CONFIGURE?
```

That's not a complete technical definition, but it's an excellent memory aid.

---

## Before Structural Patterns

At this point, you should be able to explain why this:

```java
User user = new User(
    "John",
    "Doe",
    30,
    null,
    null,
    "Hyderabad",
    "India"
);
```

may be better expressed as:

```java
User user = User.builder()
        .firstName("John")
        .lastName("Doe")
        .age(30)
        .city("Hyderabad")
        .country("India")
        .build();
```

and explain **what problem Builder is solving**, rather than merely recognizing the `.builder().xxx().build()` syntax.

Say **`next`** and we'll move into the **Structural Patterns**, starting with **Adapter**.

