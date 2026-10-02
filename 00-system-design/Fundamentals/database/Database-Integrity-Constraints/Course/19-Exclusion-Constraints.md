# 19. Exclusion Constraints

Exclusion constraints are one of PostgreSQL's most powerful integrity features.

The simple idea is:

> **An exclusion constraint prevents two rows from being "in conflict" according to rules you define.**

This is especially useful for **overlapping time ranges**.

---

# 1. The problem with `UNIQUE`

Imagine a hotel booking system.

```text
Room 101

Booking A
Oct 1 → Oct 5

Booking B
Oct 3 → Oct 7
```

These bookings overlap:

```text
Oct 1    Oct 3    Oct 5    Oct 7
  |--------A--------|
           |--------B--------|
           ↑
        conflict
```

We want the database to reject Booking B.

You might try:

```sql
UNIQUE (room_id, start_date, end_date)
```

But that doesn't work.

Why?

Because these are different combinations:

```text
(room 101, Oct 1, Oct 5)
(room 101, Oct 3, Oct 7)
```

`UNIQUE` only asks:

> "Are these values exactly the same?"

But our business rule is:

> "Do these bookings overlap?"

That's a different kind of relationship.

---

# 2. Exclusion constraints solve this

PostgreSQL provides:

```sql
EXCLUDE
```

An exclusion constraint says:

> **For any two rows, the specified comparison operators cannot all be true simultaneously.**

That's a little abstract, so let's simplify it.

Suppose we want:

```text
Same room
AND
Overlapping dates
```

to be forbidden.

We can express:

```text
room_id WITH =
AND
booking_period WITH &&
```

where:

```text
=   → values are equal
&&  → ranges overlap
```

So:

```text
same room
    +
overlapping period
    ↓
CONFLICT
```

---

# 3. First example: room bookings

PostgreSQL has a `daterange` type.

We can create:

```sql
CREATE TABLE room_bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    room_id BIGINT NOT NULL,

    booking_period DATERANGE NOT NULL,

    CONSTRAINT excl_room_booking_overlap
        EXCLUDE USING gist (
            room_id WITH =,
            booking_period WITH &&
        )
);
```

The important part is:

```sql
EXCLUDE USING gist (
    room_id WITH =,
    booking_period WITH &&
)
```

It means:

> Two rows cannot have the same `room_id` **and** overlapping `booking_period`.

---

# 4. Insert the first booking

```sql
INSERT INTO room_bookings (
    room_id,
    booking_period
)
VALUES (
    101,
    '[2026-10-01, 2026-10-05)'
);
```

Allowed.

We now have:

```text
Room 101
Oct 1 ───────── Oct 5
```

---

# 5. Try an overlapping booking

```sql
INSERT INTO room_bookings (
    room_id,
    booking_period
)
VALUES (
    101,
    '[2026-10-03, 2026-10-07)'
);
```

PostgreSQL rejects it.

Why?

```text
room_id:
101 = 101
   ↓
TRUE

booking_period:
Oct 1–5 && Oct 3–7
   ↓
TRUE

Both conflict conditions are TRUE
   ↓
EXCLUSION VIOLATION
```

---

# 6. What about a different room?

Consider:

```sql
INSERT INTO room_bookings (
    room_id,
    booking_period
)
VALUES (
    102,
    '[2026-10-03, 2026-10-07)'
);
```

This is allowed.

Because:

```text
101 = 102
   ↓
FALSE
```

The exclusion condition requires **all specified comparisons** to be true to constitute a conflict.

Therefore:

```text
Room 101 + overlapping period → conflict

Room 102 + overlapping period → no conflict
```

---

# 7. What about adjacent bookings?

This is an important detail.

Suppose we have:

```text
Booking A:
[Oct 1, Oct 5)

Booking B:
[Oct 5, Oct 10)
```

Notice the `)`.

PostgreSQL range:

```text
[Oct 1, Oct 5)
```

means:

```text
includes Oct 1
does not include Oct 5
```

Therefore the two ranges don't overlap.

```text
Oct 1       Oct 5       Oct 10
 |-----------|------------|
      A              B
```

So this is allowed.

That's exactly what we usually want for hotel bookings:

```text
Guest A checks out Oct 5
Guest B checks in Oct 5
```

No overlap.

---

# 8. Why range types are so useful

Without range types, we might store:

```sql
start_date DATE,
end_date DATE
```

and then need complicated logic for overlap.

With:

```sql
booking_period DATERANGE
```

the database understands the interval as a first-class value.

For example:

```text
[2026-10-01, 2026-10-05)
```

You can then use PostgreSQL's range operators:

```text
&&   overlaps
@>   contains
<@   contained by
-|-  adjacent
```

For example:

```sql
SELECT *
FROM room_bookings
WHERE booking_period
      && '[2026-10-03, 2026-10-07)'::daterange;
```

This asks:

> Which bookings overlap this period?

---

# 9. Why `GIST`?

You might have noticed:

```sql
EXCLUDE USING gist
```

`GiST` stands for:

> **Generalized Search Tree**

It's an index framework in PostgreSQL that supports several types of searches that ordinary B-tree indexes aren't designed for.

Range operations such as:

```text
overlaps
contains
adjacent
```

are a major use case.

Conceptually:

```text
EXCLUDE
   ↓
needs efficient conflict detection
   ↓
GiST index
   ↓
range/operator support
```

---

# 10. Another example: employee schedules

Suppose employees can have scheduled work periods.

Business rule:

> An employee cannot have two overlapping shifts.

We can model:

```sql
CREATE TABLE employee_shifts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    employee_id BIGINT NOT NULL,

    shift_period TSTZRANGE NOT NULL,

    CONSTRAINT excl_employee_shift_overlap
        EXCLUDE USING gist (
            employee_id WITH =,
            shift_period WITH &&
        )
);
```

Now:

```text
Employee 42

09:00 ─────── 13:00
```

and:

```text
Employee 42

12:00 ─────── 16:00
```

conflict.

But:

```text
Employee 43

12:00 ─────── 16:00
```

doesn't conflict.

---

# 11. Another example: vehicle rentals

Suppose:

```text
Vehicle 500
```

is rented from:

```text
Oct 1 → Oct 5
```

We don't want another rental for the same vehicle during an overlapping period.

The rule becomes:

```text
vehicle_id =
AND
rental_period overlaps
```

So:

```sql
EXCLUDE USING gist (
    vehicle_id WITH =,
    rental_period WITH &&
)
```

This is much more expressive than `UNIQUE`.

---

# 12. Exclusion constraints vs `UNIQUE`

This distinction is important for interviews.

| `UNIQUE`                               | `EXCLUDE`                                    |
| -------------------------------------- | -------------------------------------------- |
| Prevents duplicate values/combinations | Prevents conflicting rows                    |
| Usually uses equality                  | Can use different operators                  |
| `a = a` conflict                       | `a = a`, `range && range`, etc.              |
| Good for exact uniqueness              | Good for overlapping/conflicting data        |
| Common for usernames, emails, seats    | Common for schedules, bookings, reservations |

Think:

```text
UNIQUE
"These values cannot be the same."

EXCLUDE
"These rows cannot conflict according to these operators."
```

---

# 13. `UNIQUE` for seats vs `EXCLUDE` for time

Consider movie seats.

If one seat can only be booked once for a show:

```sql
UNIQUE (show_id, seat_id)
```

is perfect.

Why?

Because the rule is:

```text
same show
+
same seat
=
duplicate
```

But hotel rooms have:

```text
same room
+
overlapping time
=
conflict
```

That's not simple equality.

Therefore:

```text
Movie seat booking → UNIQUE

Room/time booking → EXCLUDE
```

---

# 14. Multiple operators

The power of exclusion constraints comes from the fact that you aren't limited to equality.

Conceptually:

```sql
EXCLUDE USING gist (
    column_a WITH operator_a,
    column_b WITH operator_b
)
```

Two rows conflict when **every specified comparison evaluates to true**.

For example:

```text
room_id WITH =
booking_period WITH &&
```

means:

```text
room A = room B
AND
period A overlaps period B
```

If both are true:

```text
CONFLICT
```

If either is false:

```text
NO CONFLICT
```

---

# 15. `btree_gist`

You may encounter an additional PostgreSQL extension in examples:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

Why?

Our example compares:

```sql
room_id WITH =
```

while also using:

```sql
booking_period WITH &&
```

GiST has native support for many data types, but equality comparison for some ordinary scalar types may require the `btree_gist` extension.

For example:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

Then:

```sql
CREATE TABLE room_bookings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    room_id BIGINT NOT NULL,

    booking_period DATERANGE NOT NULL,

    CONSTRAINT excl_room_booking_overlap
        EXCLUDE USING gist (
            room_id WITH =,
            booking_period WITH &&
        )
);
```

This is a common PostgreSQL pattern.

---

# 16. `DEFERRABLE` and exclusion constraints

Remember our previous lesson about:

```text
DEFERRABLE
```

Exclusion constraints can also be deferrable.

For example:

```sql
CONSTRAINT excl_room_booking_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        booking_period WITH &&
    )
    DEFERRABLE INITIALLY IMMEDIATE
```

Then you can defer it:

```sql
SET CONSTRAINTS excl_room_booking_overlap DEFERRED;
```

This means PostgreSQL can postpone checking the exclusion constraint until the appropriate constraint-checking point, often transaction commit.

So the concepts connect:

```text
EXCLUDE
    +
DEFERRABLE
    +
TRANSACTION
```

can be useful when temporarily conflicting intermediate states are necessary.

---

# 17. Important caveat: boundaries matter

When working with ranges, you must understand whether boundaries are inclusive or exclusive.

For example:

```text
[1, 5)
```

means:

```text
1 ≤ x < 5
```

while:

```text
[1, 5]
```

means:

```text
1 ≤ x ≤ 5
```

This affects whether two ranges overlap.

For scheduling systems, choosing the correct boundary semantics is extremely important.

A common convention is:

```text
[start, end)
```

because adjacent intervals can meet cleanly:

```text
[09:00, 10:00)
[10:00, 11:00)
```

without overlapping.

---

# 18. The real-world pattern

When you see a requirement like:

> "For the same X, Y cannot overlap."

Immediately think:

```text
EXCLUDE
```

Examples:

```text
For the same room:
    bookings cannot overlap

For the same employee:
    shifts cannot overlap

For the same vehicle:
    rentals cannot overlap

For the same resource:
    reservations cannot overlap
```

This is one of the strongest PostgreSQL-specific integrity patterns to know.

---

# 19. Interview question

### Q: Why can't a normal `UNIQUE` constraint prevent overlapping bookings?

Because `UNIQUE` checks equality of indexed values.

For example:

```text
Room 101, Oct 1–Oct 5
Room 101, Oct 3–Oct 7
```

have different start/end values, so:

```text
UNIQUE(room_id, start_date, end_date)
```

doesn't consider them duplicates.

The business rule isn't:

> "The values must be different."

It's:

> "The periods must not overlap for the same resource."

Therefore PostgreSQL's exclusion constraint is appropriate:

```sql
EXCLUDE USING gist (
    room_id WITH =,
    booking_period WITH &&
)
```

---

# 20. Mental model

Remember the progression:

```text
NOT NULL
    ↓
Value must exist

CHECK
    ↓
Value/row must satisfy a condition

UNIQUE
    ↓
Values/combinations cannot duplicate

FOREIGN KEY
    ↓
Referenced row must exist

EXCLUDE
    ↓
Rows cannot conflict according to operators
```

The most important sentence:

> **`UNIQUE` prevents duplicates; `EXCLUDE` prevents conflicts.**

And the classic example is:

```text
Same resource
+
Overlapping time range
=
EXCLUSION CONSTRAINT
```

---

## Next

**20. Real-World Schema Design + Interview Questions**

We'll bring the entire course together by designing a realistic PostgreSQL schema and identifying **which integrity constraint belongs to each business rule**, followed by common interview questions and traps.

