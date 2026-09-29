# Topic 23 — JPA Entity Identity, `equals()` and `hashCode()`

This topic looks like ordinary Java:

```java
equals()
hashCode()
==
```

But with JPA/Hibernate, it becomes surprisingly subtle.

The central problem is that we have **three different notions of identity**:

```text
Java object identity
        ↓
     ==

Java logical equality
        ↓
   equals()

Database/JPA identity
        ↓
   entity type + primary key
```

Jakarta Persistence defines an entity's persistent identity in terms of its entity type within the inheritance hierarchy and its primary-key value. The persistence context also guarantees identity for managed entities. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

Hibernate additionally has to deal with **proxies**, generated identifiers, detached entities, and Java collections such as `Set`.

This topic is therefore extremely common in senior Java/JPA interviews.

---

# 1. Start with `==`

Suppose:

```java
User user1 = new User();
User user2 = new User();
```

Then:

```java
user1 == user2
```

is:

```text
false
```

because they are two different Java object references.

`==` asks:

> **Are these two references pointing to the exact same Java object?**

It does not care about:

```text
id
name
email
```

---

# 2. `equals()`

Now:

```java
user1.equals(user2)
```

depends on the implementation.

If you haven't overridden `equals()`, `Object.equals()` is effectively reference equality, so:

```text
user1.equals(user2)
```

is also false.

But you can define logical equality:

```java
@Override
public boolean equals(Object o) {
    ...
}
```

For example:

```java
User u1 = ...; // id = 10
User u2 = ...; // id = 10
```

You might decide:

```text
u1.equals(u2) = true
```

because both represent the same persistent user.

---

# 3. Database identity

Suppose:

```text
users
----------------
id    name
10    John
11    Alice
```

In JPA:

```java
@Id
private Long id;
```

defines the entity's persistent identity.

Jakarta Persistence states that an entity's primary-key value uniquely identifies its entity instance within a persistence context, and persistent identity is based on the entity type plus primary-key value. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

So conceptually:

```text
User + id=10
```

represents one persistent identity.

---

# 4. The first big JPA surprise

Suppose:

```java
User user1 =
        entityManager.find(User.class, 10L);

entityManager.clear();

User user2 =
        entityManager.find(User.class, 10L);
```

Now:

```java
user1 == user2
```

may be:

```text
false
```

Why?

Because after:

```java
entityManager.clear();
```

the first entity became detached.

A later `find()` can load another Java object representing the same database row.

So:

```text
same database identity
        ≠
same Java object reference
```

This distinction is central to JPA.

The persistence-context identity guarantee applies to managed instances within that persistence context. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 5. Within the same persistence context

Now compare:

```java
User user1 =
        entityManager.find(User.class, 10L);

User user2 =
        entityManager.find(User.class, 10L);
```

within the same persistence context.

JPA's persistence-context identity model means that the same persistent identity corresponds to the same managed entity instance.

Conceptually:

```text
Persistence Context

User#10
   ↑
   |
user1
user2
```

so:

```java
user1 == user2
```

can be true.

That's why the persistence context is often described as providing an identity map.

---

# 6. Why `equals()` becomes important

Now imagine:

```text
Transaction A
    ↓
loads User#10

Transaction ends

Transaction B
    ↓
loads User#10
```

You could have:

```text
userA
```

and:

```text
userB
```

that are different Java objects:

```java
userA != userB
```

but logically:

```text
same User
same database identity
```

If your application places those objects into collections, reference-based equality may produce surprising behavior.

Hibernate's documentation specifically notes that entities used outside a `Session`, especially in Java collections, should have carefully designed `equals()`/`hashCode()` implementations. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 7. Why `hashCode()` matters

Java collections such as:

```java
HashSet
HashMap
```

depend on:

```text
equals()
+
hashCode()
```

For example:

```java
Set<User> users = new HashSet<>();
```

When you call:

```java
users.contains(user);
```

the collection uses the object's hash code to locate the appropriate bucket and equality to determine the matching object.

Therefore:

> **If an object's hash code changes while it is inside a `HashSet`, the collection can effectively lose track of that object.**

This is the key reason generated IDs create trouble.

Hibernate explicitly discusses this issue and warns against including database-generated fields in `hashCode()` because the value isn't available until persistence and can therefore change after the entity is already inside a hashed collection. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 8. The classic broken implementation

Consider:

```java
@Entity
public class User {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof User)) {
            return false;
        }

        User other = (User) o;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

Looks reasonable.

But now:

```java
User user = new User();
```

Initially:

```text
id = null
```

Therefore:

```java
user.hashCode()
```

is based on:

```text
null
```

Add it to a Set:

```java
Set<User> users = new HashSet<>();
users.add(user);
```

Then persist it.

Hibernate generates:

```text
id = 100
```

Now:

```java
user.hashCode()
```

is based on:

```text
100
```

So the hash code changed.

---

# 9. The HashSet disaster

Before persistence:

```text
hashCode(user) = H1
```

`HashSet` stores the object in the bucket determined by:

```text
H1
```

After persistence:

```text
hashCode(user) = H2
```

where:

```text
H1 != H2
```

Now:

```java
users.contains(user)
```

may return:

```text
false
```

even though the exact same Java object is physically inside the set.

Why?

Because `HashSet` looks in the bucket corresponding to `H2`, while the object was stored in the bucket corresponding to `H1`.

Hibernate demonstrates this exact problem with generated identifiers and `Set`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

This is one of the most important JPA collection pitfalls.

---

# 10. The Java rule behind it

The standard `hashCode()` contract says:

> If two objects are equal according to `equals()`, they must have the same hash code.

But there's another practical requirement for hashed collections:

> **The hash code must remain stable while the object is being used as a key/member of the collection.**

So this is dangerous:

```text
transient entity
    ↓
hash based on generated ID
    ↓
ID generated
    ↓
hash changes
```

---

# 11. What should `equals()` use?

There are two common approaches.

### Approach A

Use an immutable, unique **natural/business key**.

### Approach B

Use the generated ID carefully with a **constant hash code** and special handling for transient objects.

Hibernate's current documentation actually recommends a natural key where one exists and explains how a generated identifier can be used safely with the right workaround. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

Let's understand both.

---

# 12. Natural/business key

Suppose:

```text
Book
---------
id
isbn
title
```

and:

```text
ISBN
```

is:

```text
unique
immutable
```

This can be an excellent equality candidate.

```java
@NaturalId
private String isbn;
```

Then:

```java
@Override
public boolean equals(Object o) {

    if (this == o) {
        return true;
    }

    if (!(o instanceof Book other)) {
        return false;
    }

    return Objects.equals(isbn, other.isbn);
}

@Override
public int hashCode() {
    return Objects.hash(isbn);
}
```

Because:

```text
isbn
```

is available before persistence and doesn't change, the hash remains stable.

Hibernate's documentation recommends an immutable natural key that corresponds to a unique database constraint as a strong basis for entity equality. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 13. What makes a good natural key?

Ideally:

```text
unique
immutable
available from entity creation
```

Example:

```text
ISBN
external business identifier
country code + account number
immutable employee number
```

Bad candidates:

```text
name
address
status
updatedAt
```

because they can change.

Hibernate explicitly warns not to include mutable fields in `hashCode()`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 14. Why mutable fields are dangerous

Suppose:

```java
@Override
public int hashCode() {
    return Objects.hash(name);
}
```

Then:

```java
User user = new User();
user.setName("John");

Set<User> set = new HashSet<>();
set.add(user);
```

Initially:

```text
hash = hash("John")
```

Then:

```java
user.setName("Alice");
```

Now:

```text
hash = hash("Alice")
```

The object has moved logically from one bucket to another, but `HashSet` doesn't rehash it automatically.

So:

```java
set.contains(user)
```

can fail.

That's why:

> **Never base `hashCode()` on mutable entity fields.**

Hibernate explicitly recommends this. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 15. Why database-generated ID is also problematic

This:

```java
@Override
public int hashCode() {
    return Objects.hash(id);
}
```

has a different problem.

The ID isn't mutable in the normal business sense, but:

```text
before persistence:
id = null

after persistence:
id = 123
```

So the hash changes as part of the entity lifecycle.

That violates the stability requirement for hashed collections.

Hibernate explicitly advises against using generated database identifiers directly in `hashCode()` because of this lifecycle transition. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 16. Generated ID + constant hash code

When you don't have a natural key, Hibernate documents another viable approach:

```java
@Override
public boolean equals(Object o) {

    if (this == o) {
        return true;
    }

    if (!(o instanceof User other)) {
        return false;
    }

    return id != null
            && id.equals(other.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

Conceptually:

```text
equals:
    same reference → true

    otherwise:
        IDs must both represent a persistent identity
        and be equal

hashCode:
    constant for the entity type
```

Hibernate specifically describes this pattern as a way to safely use generated identifiers: compare IDs only for non-transient entities and use a constant hash-code value so the hash doesn't change when the ID is generated. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 17. Why is a constant hash code acceptable?

You might say:

> "Isn't a constant hash code terrible for performance?"

It can reduce hash distribution.

But it preserves correctness.

And correctness is more important than an ideal hash distribution for entity equality.

Hibernate's documentation explicitly presents this constant-hash approach as a valid solution when only a generated identifier is available. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

The typical situation is:

```text
entity collections
are relatively small
```

compared with arbitrarily large hash tables where distribution becomes a larger concern.

---

# 18. The subtle part: don't equate two transient entities

Consider:

```java
User user1 = new User();
User user2 = new User();
```

Both have:

```text
id = null
```

If you simply write:

```java
return Objects.equals(id, other.id);
```

then:

```text
null == null
```

means:

```text
user1.equals(user2) = true
```

That's wrong.

Two brand-new users aren't automatically the same entity.

Therefore:

```java
return id != null && id.equals(other.id);
```

is crucial.

Conceptually:

```text
both IDs null
   ↓
not equal

both IDs non-null + equal
   ↓
equal
```

Hibernate explicitly calls out this requirement in its generated-ID approach. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 19. Hibernate proxies

Now we reach the really interesting part.

Hibernate can use proxies for lazy-loading.

Suppose:

```java
Customer customer = ...
```

The runtime object might not literally be:

```java
Customer
```

Instead, Hibernate may give you a proxy/subclass representing the `Customer`.

Conceptually:

```text
Customer
   ↑
   |
HibernateProxy
```

The proxy can defer loading of the real entity state.

Hibernate's documentation specifically warns that entity `equals()` implementations must accommodate the possibility that the compared object is a proxy. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 20. The dangerous `getClass()` implementation

A common implementation is:

```java
@Override
public boolean equals(Object o) {

    if (this == o) {
        return true;
    }

    if (o == null || getClass() != o.getClass()) {
        return false;
    }

    User other = (User) o;

    return id.equals(other.id);
}
```

This looks perfectly normal for ordinary Java.

But with Hibernate proxies:

```text
User.class
```

and:

```text
Hibernate-generated User proxy class
```

can be different runtime classes.

So:

```java
getClass() != other.getClass()
```

can produce:

```text
false
```

even though both represent the same entity type and identifier.

Hibernate's current guidance specifically recommends using `instanceof` rather than `getClass()` for this reason. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 21. `instanceof` and proxies

A Hibernate-friendly implementation can use:

```java
if (!(o instanceof User other)) {
    return false;
}
```

instead of:

```java
if (getClass() != o.getClass()) {
    return false;
}
```

This allows a Hibernate proxy representing `User` to participate in equality.

Hibernate explicitly recommends `instanceof` for entity equality when proxies are possible. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 22. Why accessors matter

Hibernate's guide also makes another subtle recommendation:

> When comparing entity properties, use accessor methods rather than directly accessing fields of the other entity.

For example:

```java
return id != null
        && id.equals(other.getId());
```

rather than blindly:

```java
return id != null
        && id.equals(other.id);
```

Why?

Because the other object could be a proxy, and accessor-based interaction allows Hibernate's proxy behavior to work correctly.

Hibernate explicitly mentions this in its equality guidance. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 23. A good generated-ID implementation

A practical pattern is:

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof User other)) {
            return false;
        }

        return id != null &&
               id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

The important parts are:

```text
same reference → true

must be same entity type/interface-compatible → true

ID must be non-null

IDs equal → equal

hashCode stays constant
```

Hibernate documents this style as a valid generated-identifier solution. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 24. But what about proxy classes and `getClass().hashCode()`?

This is a nuanced issue.

Suppose:

```text
actual User class
    hash = User.class.hashCode()

proxy subclass
    hash = ProxyClass.class.hashCode()
```

could produce different values.

Hibernate's own recommended natural-id implementation avoids this by using the stable natural key for both equality and hash code. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

For generated-ID approaches, equality/proxy behavior must be designed consistently with the inheritance/proxy model. In a concrete project, follow the equality strategy recommended for your Hibernate version and inheritance setup rather than copying a generic snippet blindly.

The broader lesson:

> **Entity equality is a design decision, not a Lombok-generated boilerplate method.**

---

# 25. Why Lombok `@Data` can be dangerous

Suppose someone writes:

```java
@Entity
@Data
public class User {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user")
    private List<Order> orders;
}
```

This is dangerous for JPA entities because Lombok-generated:

```text
equals()
hashCode()
toString()
```

can include all fields and relationships.

Now imagine:

```text
User
  ↓
Orders
  ↓
User
  ↓
Orders
  ↓
...
```

You can get:

```text
recursive toString
huge equality operations
lazy loading
proxy problems
hash instability
```

It's usually safer to explicitly design:

```java
equals()
hashCode()
toString()
```

for entities.

---

# 26. Never put collections in `equals()`/`hashCode()`

Avoid:

```java
@Override
public boolean equals(Object o) {
    return Objects.equals(
        orders,
        other.orders
    );
}
```

And especially avoid:

```java
@Override
public int hashCode() {
    return Objects.hash(
        id,
        name,
        orders
    );
}
```

Why?

Because collections are:

```text
mutable
potentially large
lazy
bidirectional
```

You can trigger:

```text
database access
recursion
performance problems
hash instability
```

Equality should generally be based on a stable identity/natural key, not an entire entity graph.

---

# 27. Bidirectional relationship + `equals()`

Suppose:

```text
Customer
   |
   +---- orders
              |
              +---- customer
```

If `equals()` considers:

```text
Customer.orders
```

and:

```text
Order.customer
```

you create a circular object graph.

Then:

```text
customer.equals(customer2)
```

might traverse:

```text
orders
 → customer
 → orders
 → customer
 → ...
```

This is another reason relationships should normally be excluded from entity equality.

---

# 28. Entity equality should be stable

This is perhaps the best principle in this entire topic:

> **The properties participating in `equals()` and `hashCode()` should have stable values for the period in which the entity is used in collections.**

That's why:

```text
mutable name
```

is bad.

And:

```text
generated ID that changes from null → generated value
```

is tricky.

While:

```text
immutable natural key
```

is excellent.

Hibernate's current equality guidance is built around exactly this stability requirement. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 29. A natural-key example

Suppose:

```java
@Entity
public class Book {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn;

    private String title;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Book other)) {
            return false;
        }

        return Objects.equals(
                isbn,
                other.getIsbn()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }
}
```

This is good when:

```text
ISBN is unique
+
ISBN is immutable
+
ISBN is known before persistence
```

Hibernate's guide uses a natural-ID-based approach like this and recommends the corresponding database uniqueness constraint. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 30. Why `@NaturalId` is useful

Hibernate provides:

```java
@NaturalId
```

for a property representing the entity's natural identifier.

Example:

```java
@NaturalId
@Column(nullable = false, unique = true)
private String isbn;
```

Now both the domain concept and database constraint communicate:

```text
ISBN uniquely identifies Book
```

Hibernate's documentation explicitly demonstrates using a natural ID in equality/hash-code when it is an immutable unique attribute. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 31. What if there is no natural key?

This is common.

For example:

```text
Employee
---------
id
firstName
lastName
salary
```

There may be no immutable business identifier suitable for equality.

Then you need an ID-based strategy.

One Hibernate-documented approach is:

```java
@Override
public boolean equals(Object o) {
    if (this == o) {
        return true;
    }

    if (!(o instanceof Employee other)) {
        return false;
    }

    return id != null &&
           id.equals(other.getId());
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

The key is:

```text
ID equality only once persistent identity exists
+
constant hash code
```

([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 32. Why ID equality makes sense after persistence

Suppose:

```text
User A
id = 10

User B
id = 10
```

and both are instances of the same entity type.

They represent:

```text
same persistent identity
```

So:

```java
A.equals(B)
```

can reasonably be true.

But:

```text
User A
id = null

User B
id = null
```

should not be equal merely because both IDs are null.

Hence:

```java
id != null &&
id.equals(other.getId())
```

---

# 33. Entity equality across transactions

Suppose:

```text
Transaction 1
    ↓
load User#10
    ↓
detach

Transaction 2
    ↓
load User#10
```

Now:

```text
u1 != u2
```

but:

```java
u1.equals(u2)
```

can be:

```text
true
```

with an appropriate identity-based equality implementation.

This is exactly where proper `equals()` becomes useful.

---

# 34. `Set` relationships

Suppose:

```java
@OneToMany
private Set<Order> orders;
```

A `Set` relies on:

```text
equals()
hashCode()
```

to determine uniqueness.

So incorrect entity equality can produce bizarre behavior:

```java
customer.getOrders().add(order);
```

then later:

```java
customer.getOrders().contains(order);
```

returns:

```text
false
```

or an equivalent logical entity isn't recognized.

Hibernate specifically notes that proper `equals()`/`hashCode()` implementations are important for entities represented in sets. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 35. `List` vs `Set` and equality

This gives another reason to understand the previous relationship topic.

For:

```java
List<Order>
```

equality isn't used to enforce uniqueness in the same way as a `Set`.

For:

```java
Set<Order>
```

it is fundamental.

So:

```text
@Entity
+
equals/hashCode
+
Set
```

is a particularly important combination.

---

# 36. `Map` keys are even more sensitive

Consider:

```java
Map<User, String> roles;
```

Now `User` is a hash-based key.

Changing its hash code after insertion can make the entry effectively unreachable.

The same generated-ID problem becomes even more obvious:

```text
new User
   ↓
put(user, "ADMIN")
   ↓
generated ID changes
   ↓
hash changes
   ↓
map.get(user) may fail
```

This is why entity hash stability is critical.

---

# 37. What about Java records?

You might wonder:

```java
public record User(...) {}
```

Records generate value-based:

```text
equals()
hashCode()
```

from all components.

That can be excellent for:

```text
DTOs
value objects
read models
projections
```

but JPA entities have lifecycle, identity, proxying, and mutation requirements that make ordinary entity records a separate design discussion.

For JPA entities:

> Don't blindly apply record-style value equality to an entity.

---

# 38. Entity equality vs value-object equality

This distinction is extremely useful.

### Value object

Example:

```java
Address
```

Its meaning may be entirely determined by:

```text
street
city
postcode
country
```

So value-based equality is natural.

### Entity

Example:

```java
User
```

Its identity persists independently of mutable state.

Therefore:

```text
entity identity
```

is different from:

```text
value equality
```

This is a fundamental Domain Driven Design distinction.

---

# 39. Don't use every field for entity equality

Suppose:

```text
User
---------
id
name
email
passwordHash
status
createdAt
updatedAt
orders
roles
```

Doing:

```java
Objects.hash(
    id,
    name,
    email,
    passwordHash,
    status,
    createdAt,
    updatedAt,
    orders,
    roles
);
```

would be disastrous.

Any change to:

```text
status
updatedAt
orders
roles
```

could change the hash.

And equality becomes unnecessarily expensive.

Entity equality should be narrow.

---

# 40. A major Hibernate proxy trap

Consider:

```java
User user1 = entityManager.find(User.class, 1L);
User user2 = entityManager.getReference(User.class, 1L);
```

`getReference()` may return a lazy proxy rather than a fully initialized entity.

Conceptually:

```text
user1 → User
user2 → Hibernate proxy
```

Both represent:

```text
User#1
```

A `getClass()`-based equality implementation can incorrectly say:

```text
false
```

because the runtime classes differ.

A proxy-aware implementation based on `instanceof` can recognize the relationship.

Hibernate's documentation explicitly uses proxy-aware equality as a reason to prefer `instanceof` over `getClass()`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 41. Why does `getClass()` sometimes make sense in ordinary Java?

In ordinary Java, you might intentionally want:

```java
Animal.class != Dog.class
```

to mean they aren't equal.

So:

```java
getClass()
```

can be perfectly valid in normal object-oriented programming.

The problem is:

```text
Hibernate proxies
+
entity inheritance
```

change the assumptions.

That's why JPA entity equality needs special consideration.

---

# 42. Inheritance makes equality even harder

Suppose:

```text
Person
  ↑
Employee
  ↑
Manager
```

What does:

```text
Employee(id=10)
```

vs:

```text
Manager(id=10)
```

mean?

Are they equal?

The JPA persistent identity model includes the class of the root entity in the identity definition, not merely the numeric ID. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

So entity inheritance makes equality design another reason to avoid generic boilerplate.

The details depend on your inheritance mapping strategy.

---

# 43. Composite IDs

Another case:

```java
@EmbeddedId
private OrderItemId id;
```

or:

```java
@IdClass(OrderItemId.class)
```

The **primary-key class itself** must implement:

```java
equals()
hashCode()
```

Jakarta Persistence explicitly requires composite primary-key classes to define `equals()` and `hashCode()`, with equality semantics consistent with the database types to which the key is mapped. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This is different from the tricky entity-equality question.

For a composite ID object:

```text
OrderItemId
   ↓
value object
```

value-based equality is exactly what you generally expect.

---

# 44. Entity ID vs ID class

Suppose:

```java
@Embeddable
public record OrderItemId(
        Long orderId,
        Long productId
) {}
```

The identifier is:

```text
(orderId, productId)
```

Therefore:

```java
new OrderItemId(1L, 10L)
```

should equal:

```java
new OrderItemId(1L, 10L)
```

because those represent the same composite key.

The Jakarta Persistence specification requires this equality behavior for primary-key classes. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 45. Entity equality vs primary-key equality

This is another important distinction.

For an ID class:

```text
OrderItemId
```

value equality is straightforward.

For the entity:

```text
OrderItem
```

you still have to decide how entity equality behaves across:

```text
new
managed
detached
proxy
```

So:

```text
ID object's equals/hashCode
```

and:

```text
Entity's equals/hashCode
```

are related but not the same design problem.

---

# 46. What about mutable natural keys?

Suppose:

```java
@NaturalId
private String email;
```

but the email can change.

Then:

```text
before:
john@example.com

after:
john2@example.com
```

If `email` is used in `hashCode()`:

```text
hash changes
```

and the same `HashSet` problem returns.

Therefore:

> **A natural key used for equality should preferably be immutable.**

Hibernate's guidance specifically recommends immutable natural-key fields. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 47. Database uniqueness matters too

Suppose:

```java
@Column(unique = true)
private String isbn;
```

and you use:

```text
isbn
```

for entity equality.

That's much more convincing because:

```text
Java equality
      +
database unique constraint
```

express the same domain invariant.

But remember that database uniqueness should be enforced explicitly by the schema/migration rather than relying only on application logic or annotation assumptions.

---

# 48. Equality and lazy loading

Suppose:

```java
User user1
User user2
```

and equality is based on:

```java
email
```

Accessing:

```java
other.getEmail()
```

might be fine.

But if you base equality on:

```java
orders
```

you can trigger lazy loading.

So:

```text
good equality field
   ↓
small + stable + already intrinsic to entity

bad equality field
   ↓
relationship / collection
```

---

# 49. Why `toString()` has similar problems

Consider:

```java
@Override
public String toString() {
    return "User{" +
        "id=" + id +
        ", orders=" + orders +
        '}';
}
```

Calling:

```java
System.out.println(user);
```

could trigger lazy loading of:

```text
orders
```

and then:

```text
orders → each order → customer → ...
```

This isn't an `equals()` problem, but it comes from the same fundamental rule:

> **Don't casually traverse JPA relationships from utility methods such as `equals()`, `hashCode()`, and `toString()`.**

---

# 50. The safest mental model

For entities, think:

```text
equals()
    ↓
"What persistent identity or immutable business identity does this represent?"

hashCode()
    ↓
"Can this value remain stable for the object's lifetime in a HashSet/HashMap?"
```

Not:

```text
"Which fields are in the entity?"
```

---

# 51. `==` vs `equals()` vs persistence identity

Let's summarize with an example.

```java
User a = ...; // User#10
User b = ...; // User#10
```

Possible:

```text
a == b
    → false

a.equals(b)
    → true

persistent identity
    → same User#10
```

Now:

```java
User c = ...; // User#11
```

Then:

```text
a.equals(c)
    → false
```

That's the distinction.

---

# 52. The lifecycle problem

This is the hardest part.

Suppose:

```text
NEW User
   id = null
```

then:

```text
persist
```

then:

```text
MANAGED User
   id = 100
```

If equality/hash code depends directly on:

```text
id
```

the value has changed:

```text
null → 100
```

That is why this topic cannot be separated from the lifecycle topic you just learned.

---

# 53. The generated-ID strategy visually

```text
          NEW
           |
           | id = null
           |
           v
   hashCode must be stable
           |
       persist()
           |
           v
        MANAGED
           |
           | id = 100
           |
           v
   hashCode must STILL be same
```

Hence:

```text
equals → ID once non-null
hashCode → constant
```

when using the generated-ID approach.

Hibernate explicitly describes this workaround. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 54. Natural-key strategy visually

```text
NEW
 |
 | isbn = "978..."
 |
 v
MANAGED
 |
 | id generated
 v
DATABASE

isbn never changes
```

Therefore:

```text
equals → isbn
hashCode → isbn
```

works naturally.

This is one reason Hibernate considers natural-key equality preferable when a truly immutable natural key exists. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 55. The practical recommendation

When designing an entity, ask:

### Question 1

Do I have a genuinely immutable, unique business key?

Example:

```text
ISBN
immutable external identifier
```

Then:

```text
use natural key
```

### Question 2

No natural key?

Then:

```text
use generated ID carefully
+
don't let hashCode change
+
don't treat null IDs as equal
```

### Question 3

Does the entity participate in:

```text
Set
Map
detached workflows
multiple persistence contexts
```

Then equality becomes particularly important.

---

# 56. A complete natural-key entity

```java
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    @Column(nullable = false, unique = true)
    private String isbn;

    private String title;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Book other)) {
            return false;
        }

        return Objects.equals(
                isbn,
                other.getIsbn()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }
}
```

This is appropriate when:

```text
isbn is immutable
isbn is unique
isbn identifies the business entity
```

Hibernate documents this natural-id pattern. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 57. A generated-ID entity

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof User other)) {
            return false;
        }

        return id != null &&
               id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

The key properties:

```text
transient A != transient B

persistent A with id=10
==
persistent B with id=10

hash code doesn't depend on generated ID
```

Again, exact implementation details should be aligned with your entity inheritance/proxy strategy. Hibernate documents this general generated-ID approach. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 58. Don't copy `getClass()` blindly

You will find many Java examples like:

```java
if (o == null || getClass() != o.getClass()) {
    return false;
}
```

That's perfectly normal in ordinary Java classes.

But for Hibernate entities, Hibernate explicitly recommends proxy-compatible equality using:

```java
instanceof
```

rather than:

```java
getClass()
```

and accessing the other object's properties through accessors. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

This is one of those places where:

```text
ordinary Java best practice
```

and:

```text
JPA entity best practice
```

can differ.

---

# 59. Don't use `instanceof` blindly in inheritance-heavy models either

There's a deeper nuance.

Suppose:

```text
Payment
  ↑
CreditCardPayment
BankTransferPayment
```

Should:

```text
CreditCardPayment#10
```

equal:

```text
BankTransferPayment#10
```

?

Probably not.

So entity equality must respect your domain/inheritance model.

The persistence identity model itself includes the root entity type along with the ID. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

This is why equality design gets more complicated with inheritance.

---

# 60. EPAM interview question: "Why shouldn't we use all entity fields in equals/hashCode?"

Answer:

> Because entity fields may be mutable, relationships may be lazy and large, and bidirectional associations can cause recursion. Changes to fields used in `hashCode()` can also make an entity unreachable inside `HashSet` or `HashMap`. Entity equality should generally use a stable immutable business key or carefully designed persistent identity.

Hibernate explicitly warns about mutable fields and generated IDs in `hashCode()`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 61. EPAM question: "Why is generated ID dangerous in hashCode?"

Answer:

> A generated ID can be `null` while an entity is transient and then become non-null when the entity becomes persistent. If `hashCode()` directly depends on that ID, the hash value changes while the entity may already be in a `HashSet` or used as a `HashMap` key, violating collection lookup expectations. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

Excellent answer.

---

# 62. EPAM question: "Why not use getClass() in entity equals?"

Answer:

> Hibernate may return proxies whose runtime class differs from the concrete entity class. A strict `getClass()` comparison can therefore report two objects as unequal even when they represent the same entity identity. Hibernate's current guidance recommends proxy-aware equality using `instanceof`. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 63. EPAM question: "Should entity equality use ID or business key?"

Good answer:

> If there is a genuinely immutable and unique natural/business key, it is often a strong choice because it is stable before and after persistence. If there isn't one, a generated ID can be used with care: transient entities must not compare equal merely because both IDs are null, and `hashCode()` must remain stable across ID generation. Hibernate documents both approaches. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

This is much better than:

> "Always use ID."

or:

> "Never use ID."

There isn't a universal one-line rule.

---

# 64. EPAM question: "Why does HashSet lose my JPA entity?"

Answer:

> Most likely the entity's `hashCode()` changed after insertion. A common cause is using a generated ID in `hashCode()`: the ID changes from null to a generated value when the entity becomes persistent. The object remains in the old hash bucket, so later lookups can fail. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 65. EPAM question: "Why can two detached instances represent the same entity?"

Because:

```text
persistent identity
```

is not the same thing as:

```text
Java object identity
```

You can have:

```text
Java object A
   ↓
User#10

Java object B
   ↓
User#10
```

where:

```java
A == B
```

is false.

That's normal when different persistence contexts are involved. ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4))

---

# 66. EPAM question: "What happens if I don't override equals/hashCode?"

For ordinary entity references:

```text
reference equality
```

is used.

This can be fine while everything remains inside one persistence context.

But problems can appear when entities are:

```text
detached
merged
compared across contexts
stored in Sets/Maps
```

Hibernate specifically calls out these scenarios as reasons to provide an appropriate implementation. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/userguide/html_single/?utm_source=chatgpt.com))

---

# 67. EPAM question: "Why is Lombok `@Data` dangerous on entities?"

Because it generates:

```text
equals()
hashCode()
toString()
```

automatically, and those generated methods may include mutable fields and associations.

That can cause:

```text
lazy loading
recursive relationships
hash instability
large equality operations
proxy issues
```

A safer approach is to deliberately define the methods for the entity's identity model.

---

# 68. The big-picture connection

You've now learned:

```text
Topic 17
Entity Relationships
        ↓
Topic 18
Persistence Context
        ↓
Topic 19
Transactions
        ↓
Topic 21
Fetching / Lazy Loading
        ↓
Topic 22
Concurrency
        ↓
Topic 23
Entity Equality
```

These topics aren't independent.

For example:

```text
@ManyToMany
    ↓
Set
    ↓
equals/hashCode
    ↓
entity identity
    ↓
generated ID
    ↓
transient → managed
    ↓
hash-code stability
```

That's why this topic is much more than just Java `equals()`.

---

# 69. The ultimate mental model

Think about an entity using these four identities:

```text
                 ENTITY
                    |
       +------------+------------+
       |            |            |
       v            v            v
   Java identity  Equality   Persistent identity
       |            |            |
      ==         equals()    type + @Id
                    |
                    v
                hashCode()
                    |
                    v
             HashSet / HashMap
```

And remember the lifecycle:

```text
NEW
 |
 | id = null
 v
persist
 |
 v
MANAGED
 |
 | generated ID appears
 v
database identity established
```

So equality must remain logically consistent through that transition.

---

# 70. The three rules I want you to memorize

### Rule 1

```text
== 
```

means:

> same Java object.

---

### Rule 2

```text
equals()
```

for an entity should represent:

> same logical/persistent identity or stable business identity.

---

### Rule 3

```text
hashCode()
```

must remain stable while the entity is used inside a hashed collection.

Hibernate's equality guidance revolves around these stability and identity requirements. ([docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

# 71. Final comparison

| Concept             | Meaning                                                              |
| ------------------- | -------------------------------------------------------------------- |
| `==`                | Same Java object reference                                           |
| `equals()`          | Logical equality defined by your class                               |
| `@Id`               | Persistent identity                                                  |
| Persistence Context | Maintains identity among managed entities                            |
| `hashCode()`        | Hashing contract used by `HashSet`/`HashMap`                         |
| Natural key         | Stable business identity suitable for equality when immutable/unique |
| Generated ID        | Database identity that may not exist while transient                 |
| Hibernate proxy     | Runtime object representing/delegating to an entity                  |

---

# 72. What you should know cold for EPAM

You should be able to explain all of these:

```text
== vs equals()
equals() / hashCode() contract

JPA entity identity
Persistence-context identity

managed vs detached equality

Hibernate proxies

getClass() vs instanceof

generated ID problem

HashSet problem

constant hashCode strategy

natural/business key

@NaturalId

mutable fields in hashCode

relationships in equals/hashCode

Lombok @Data on entities

Set / Map implications

composite ID equals/hashCode

entity inheritance + equality
```

And especially these scenarios:

```text
1. Why does HashSet.contains(entity) return false
   after the entity is persisted?

2. Why can two objects with the same @Id be unequal?

3. Why can two objects with the same @Id be equal
   but == false?

4. Why can Hibernate proxies break getClass()-based equals?

5. Why shouldn't transient entities with null IDs
   be considered equal?

6. When is a natural key preferable?

7. How can you safely use a generated ID for equality?

8. Why shouldn't associations be included in equals/hashCode?

9. Why is Lombok @Data dangerous for JPA entities?

10. Why must composite ID classes implement equals/hashCode?
```

---

# The interview answer to remember

> **JPA entity identity is different from Java object identity. Two Java objects can represent the same persistent entity even though `==` is false, especially across persistence contexts. Entity `equals()`/`hashCode()` therefore need to be designed around a stable identity. An immutable unique natural key is often ideal; with generated IDs, transient entities must not become equal merely because both IDs are null, and `hashCode()` must not change when the generated ID is assigned. Hibernate proxies also mean entity equality should be proxy-aware, which is why Hibernate recommends `instanceof` rather than a strict `getClass()` check.** ([jakarta.ee](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4), [docs.hibernate.org](https://docs.hibernate.org/orm/7.1/introduction/html_single/?utm_source=chatgpt.com))

---

## Topic 23 revision drill

Take this code:

```java
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
}
```

Now answer these without looking back:

```text
1. What should equals() compare?

2. What should hashCode() use?

3. What happens if hashCode() uses id directly?

4. What happens if the Product is already inside a HashSet
   when Hibernate generates its ID?

5. Why should two Products with id == null not be equal?

6. Why can getClass() be problematic with Hibernate?

7. Why is instanceof often recommended?

8. Why shouldn't name be used if name is mutable?

9. Why shouldn't products be compared using their entire
   object graph?

10. What changes if Product has an immutable unique
    business key such as SKU?
```

**Next topic: JPA Inheritance & Polymorphism — `@Inheritance`, `SINGLE_TABLE`, `JOINED`, `TABLE_PER_CLASS`, discriminator columns, polymorphic queries, `@MappedSuperclass`, and when each strategy makes sense.**

