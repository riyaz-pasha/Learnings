# 10. Composite Keys and Composite Constraints

So far, we've mostly used constraints on a **single column**:

```sql
id BIGINT PRIMARY KEY
```

or:

```sql
email TEXT UNIQUE
```

But sometimes **one column isn't enough to identify something**.

That's where composite keys and composite constraints come in.

---

# 1. What does "composite" mean?

In database terminology:

> **Composite means involving multiple columns.**

For example:

```sql
PRIMARY KEY (student_id, course_id)
```

uses two columns together.

Or:

```sql
UNIQUE (country_code, phone_number)
```

uses two columns together.

The important idea is:

```text
Column A alone
      +
Column B alone
      ↓
may not be unique

Column A + Column B
      ↓
must be unique
```

---

# 2. The classic example: Student and Course

Imagine a university.

A student can enroll in multiple courses:

```text
Student
   │
   ├── Course A
   ├── Course B
   └── Course C
```

And a course can have multiple students:

```text
Course A
   │
   ├── Student 1
   ├── Student 2
   └── Student 3
```

This is a **many-to-many relationship**.

We normally represent it using a junction table:

```sql
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE courses (
    course_id BIGINT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE enrollments (
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    PRIMARY KEY (student_id, course_id),

    FOREIGN KEY (student_id)
        REFERENCES students(student_id),

    FOREIGN KEY (course_id)
        REFERENCES courses(course_id)
);
```

The interesting part is:

```sql
PRIMARY KEY (student_id, course_id)
```

This is a **composite primary key**.

---

# 3. Why do we need two columns?

Consider:

```text
student_id    course_id
-----------------------
1             101
1             102
2             101
2             102
```

Can `student_id` alone be the primary key?

No.

Because:

```text
student_id = 1
```

appears multiple times.

Can `course_id` alone be the primary key?

No.

Because:

```text
course_id = 101
```

appears multiple times.

But:

```text
(student_id, course_id)
```

together identify an enrollment.

```text
(1, 101)
(1, 102)
(2, 101)
(2, 102)
```

Each combination is unique.

Therefore:

```text
student_id + course_id
          ↓
    enrollment identity
```

---

# 4. What does a composite primary key actually mean?

This:

```sql
PRIMARY KEY (student_id, course_id)
```

means:

> No two rows can have the same combination of `student_id` and `course_id`.

It does **not** mean each column must individually be unique.

For example:

```text
student_id    course_id
-----------------------
1             101       ✓
1             102       ✓
1             103       ✓
2             101       ✓
```

All of these are valid.

But:

```text
1    101
1    101
```

is invalid because the combination:

```text
(1, 101)
```

is duplicated.

---

# 5. Composite `UNIQUE`

The same concept applies to `UNIQUE`.

Suppose a company has stores:

```sql
CREATE TABLE stores (
    store_id BIGINT PRIMARY KEY,
    city TEXT NOT NULL,
    store_code TEXT NOT NULL,

    UNIQUE (city, store_code)
);
```

This means:

> A `store_code` only needs to be unique within a city.

For example:

```text
city        store_code
----------------------
Hyderabad   H001
Hyderabad   H002
Mumbai      H001
Mumbai      H002
```

This is valid.

Why?

Because:

```text
(Hyderabad, H001)
(Hyderabad, H002)
(Mumbai, H001)
(Mumbai, H002)
```

are all different combinations.

But:

```text
Hyderabad   H001
Hyderabad   H001
```

is invalid.

---

# 6. Composite constraint vs individual constraints

This distinction is extremely important.

Suppose you write:

```sql
UNIQUE (city, store_code)
```

This does **not** mean:

```text
city must be unique
AND
store_code must be unique
```

It means:

```text
(city, store_code)
must be unique
```

These are completely different:

### Composite unique

```sql
UNIQUE (city, store_code)
```

### Two individual unique constraints

```sql
UNIQUE (city),
UNIQUE (store_code)
```

The second one is much stricter.

---

# 7. Example showing the difference

### Composite unique

```sql
CREATE TABLE stores (
    city TEXT,
    store_code TEXT,

    UNIQUE (city, store_code)
);
```

Allowed:

```text
Hyderabad   A
Hyderabad   B
Mumbai      A
Mumbai      B
```

### Individual unique constraints

```sql
CREATE TABLE stores (
    city TEXT UNIQUE,
    store_code TEXT UNIQUE
);
```

Now:

```text
Hyderabad   A
Mumbai      B
```

is fine.

But:

```text
Hyderabad   A
Hyderabad   B
```

fails because `city` itself is duplicated.

And:

```text
Hyderabad   A
Mumbai      A
```

fails because `store_code` itself is duplicated.

So always pay attention to the parentheses.

---

# 8. Composite primary keys are automatically NOT NULL

Consider:

```sql
PRIMARY KEY (student_id, course_id)
```

Both columns are part of the primary key.

Therefore both cannot be `NULL`.

Conceptually:

```sql
student_id BIGINT NOT NULL
course_id BIGINT NOT NULL
```

is enforced as part of the primary key.

So this fails:

```sql
INSERT INTO enrollments (student_id, course_id)
VALUES (1, NULL);
```

because the primary key cannot contain `NULL`.

---

# 9. Composite foreign keys

Composite keys aren't limited to `PRIMARY KEY` and `UNIQUE`.

A foreign key can also contain multiple columns.

Example:

```sql
CREATE TABLE regions (
    country_code TEXT,
    region_code TEXT,

    PRIMARY KEY (country_code, region_code)
);
```

Now another table can reference the **combination**:

```sql
CREATE TABLE offices (
    office_id BIGINT PRIMARY KEY,

    country_code TEXT NOT NULL,
    region_code TEXT NOT NULL,

    FOREIGN KEY (country_code, region_code)
        REFERENCES regions(country_code, region_code)
);
```

Notice:

```text
offices
(country_code, region_code)
          │
          ▼
regions
(country_code, region_code)
```

The relationship depends on both values.

---

# 10. Why would a foreign key need multiple columns?

Suppose regions are identified only within a country.

For example:

```text
country_code    region_code
---------------------------
IN              TS
IN              KA
US              CA
US              NY
```

`region_code` alone isn't globally unique.

For example:

```text
CA
```

could mean California in the US.

Another country could also have a region code `CA`.

So:

```text
country_code + region_code
```

together identify the region.

Therefore the foreign key must also contain both columns.

---

# 11. Composite foreign key must match a suitable referenced key

Suppose:

```sql
CREATE TABLE regions (
    country_code TEXT,
    region_code TEXT,

    PRIMARY KEY (country_code, region_code)
);
```

Then this is valid:

```sql
FOREIGN KEY (country_code, region_code)
REFERENCES regions(country_code, region_code)
```

because:

```text
regions(country_code, region_code)
```

is a primary key.

But this would not be equivalent:

```sql
FOREIGN KEY (region_code)
REFERENCES regions(region_code)
```

because `region_code` alone isn't necessarily unique.

The referenced columns need a suitable unique constraint/index according to PostgreSQL's foreign-key rules.

---

# 12. Composite `CHECK` constraints

"Composite" can also mean a constraint involving multiple columns.

For example:

```sql
CREATE TABLE bookings (
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,

    CHECK (start_time < end_time)
);
```

The check involves:

```text
start_time
     +
end_time
```

This is not a composite key, but it is a **multi-column constraint**.

The rule is:

> The relationship between these two values must be valid.

---

# 13. Another example: price range

```sql
CREATE TABLE products (
    min_price NUMERIC NOT NULL,
    max_price NUMERIC NOT NULL,

    CHECK (min_price <= max_price)
);
```

Valid:

```text
min_price    max_price
---------------------
10           20
100          100
0            50
```

Invalid:

```text
min_price    max_price
---------------------
50           20
```

The individual columns are perfectly valid:

```text
50 ✓
20 ✓
```

But their **relationship** is invalid.

That's why the constraint needs to involve both columns.

---

# 14. Composite constraints are useful for business invariants

Suppose we have an employee:

```sql
CREATE TABLE employees (
    salary NUMERIC NOT NULL,
    bonus NUMERIC NOT NULL,
    total_compensation NUMERIC NOT NULL,

    CHECK (total_compensation = salary + bonus)
);
```

The database can enforce the relationship between columns.

Or:

```sql
CHECK (discounted_price <= original_price)
```

Or:

```sql
CHECK (available_from <= available_until)
```

Or:

```sql
CHECK (minimum_age <= maximum_age)
```

The common pattern is:

```text
multiple columns
       ↓
relationship between them
       ↓
CHECK constraint
```

---

# 15. Composite key vs composite constraint

Don't mix these concepts.

### Composite key

Multiple columns together identify a row.

```sql
PRIMARY KEY (student_id, course_id)
```

### Composite unique constraint

Multiple columns together must be unique.

```sql
UNIQUE (country_code, phone_number)
```

### Multi-column check

Multiple columns participate in a validation rule.

```sql
CHECK (start_time < end_time)
```

So "composite" describes the fact that multiple columns participate. The actual constraint type tells us what rule is being enforced.

---

# 16. Real-world ecommerce example

Consider order items.

```sql
CREATE TABLE orders (
    order_id BIGINT PRIMARY KEY
);

CREATE TABLE products (
    product_id BIGINT PRIMARY KEY
);

CREATE TABLE order_items (
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),

    PRIMARY KEY (order_id, product_id),

    FOREIGN KEY (order_id)
        REFERENCES orders(order_id)
        ON DELETE CASCADE,

    FOREIGN KEY (product_id)
        REFERENCES products(product_id)
);
```

Why:

```sql
PRIMARY KEY (order_id, product_id)
```

?

Because an order can contain many products:

```text
Order 100
 ├── Product 10
 ├── Product 20
 └── Product 30
```

and a product can appear in many orders:

```text
Product 10
 ├── Order 100
 ├── Order 101
 └── Order 102
```

But within a particular order, we might want a product to appear only once.

So:

```text
(order_id, product_id)
```

uniquely identifies an order-item relationship.

---

# 17. Composite primary key vs surrogate key

There's an important design decision here.

You could model `order_items` like this:

```sql
CREATE TABLE order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    UNIQUE (order_id, product_id)
);
```

Now:

```text
id
↓
PRIMARY KEY
```

and:

```text
(order_id, product_id)
↓
UNIQUE business rule
```

Alternatively:

```sql
PRIMARY KEY (order_id, product_id)
```

could be used directly.

Both approaches can be valid.

The choice depends on how the table is used, how other tables reference it, ORM considerations, key size, and whether the relationship itself is the natural identity.

The important thing is to distinguish:

```text
technical/surrogate identity
```

from:

```text
business uniqueness
```

---

# 18. Column order matters for composite indexes

A composite primary key or unique constraint creates a supporting index.

For example:

```sql
PRIMARY KEY (student_id, course_id)
```

is backed by an index whose leading order is effectively:

```text
student_id → course_id
```

This matters for query performance.

For example, queries involving:

```sql
WHERE student_id = 10
```

can benefit from this index.

But a query only involving:

```sql
WHERE course_id = 101
```

may not benefit in the same way from the `(student_id, course_id)` index.

This is an important connection between:

```text
constraints
```

and:

```text
index design
```

We'll go deeper into this when discussing production schema design.

---

# 19. A common mistake

Someone might write:

```sql
CREATE TABLE enrollments (
    student_id BIGINT PRIMARY KEY,
    course_id BIGINT PRIMARY KEY
);
```

This is wrong because you're trying to define two primary key constraints.

Instead:

```sql
CREATE TABLE enrollments (
    student_id BIGINT,
    course_id BIGINT,

    PRIMARY KEY (student_id, course_id)
);
```

The parentheses are critical.

---

# 20. Another common mistake

Suppose the requirement is:

> A user can have the same phone number in different countries, but not twice within the same country.

Correct:

```sql
UNIQUE (country_code, phone_number)
```

Incorrect:

```sql
UNIQUE (country_code),
UNIQUE (phone_number)
```

The first says:

```text
(country + phone)
must be unique
```

The second says:

```text
country must be unique
AND
phone must be unique
```

which is a completely different business rule.

---

# 21. Mental model

Think of a normal key:

```text
id
↓
"Which row?"
```

A composite key:

```text
column A + column B
          ↓
     "Which row?"
```

For example:

```text
student_id + course_id
          ↓
     enrollment
```

For composite uniqueness:

```text
country + phone
       ↓
"Can this combination already exist?"
```

For multi-column `CHECK`:

```text
start + end
    ↓
"Does their relationship make sense?"
```

---

# 22. Interview questions

### Q1. What is a composite primary key?

A primary key consisting of multiple columns:

```sql
PRIMARY KEY (student_id, course_id)
```

The **combination** uniquely identifies the row.

---

### Q2. Does a composite primary key make each column individually unique?

No.

```sql
PRIMARY KEY (A, B)
```

means:

```text
(A, B) must be unique
```

not:

```text
A must be unique
B must be unique
```

---

### Q3. Can a composite foreign key exist?

Yes.

```sql
FOREIGN KEY (country_code, region_code)
REFERENCES regions(country_code, region_code)
```

---

### Q4. Can a table have both a composite primary key and other unique constraints?

Yes.

For example:

```sql
CREATE TABLE example (
    a BIGINT,
    b BIGINT,
    email TEXT UNIQUE,

    PRIMARY KEY (a, b)
);
```

---

### Q5. What is the difference between:

```sql
UNIQUE (a, b)
```

and:

```sql
UNIQUE (a),
UNIQUE (b)
```

First:

```text
(a, b) must be unique
```

Second:

```text
a must be unique
AND
b must be unique
```

They enforce very different rules.

---

# Key takeaways

```text
Composite
    ↓
Multiple columns participate
```

### Composite primary key

```sql
PRIMARY KEY (a, b)
```

> `(a,b)` together identify the row.

### Composite unique

```sql
UNIQUE (a, b)
```

> `(a,b)` together cannot be duplicated.

### Composite foreign key

```sql
FOREIGN KEY (a, b)
REFERENCES parent(a, b)
```

> The pair `(a,b)` must reference a valid parent key.

### Multi-column check

```sql
CHECK (start_time < end_time)
```

> A relationship between multiple columns must satisfy a rule.

The most important mental model:

> **With a composite constraint, PostgreSQL evaluates the specified columns as a group according to the constraint's rule.**

## Next concept

**11. Multi-column `UNIQUE` Constraints**


