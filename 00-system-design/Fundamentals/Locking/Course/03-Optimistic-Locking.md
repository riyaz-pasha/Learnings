# Chapter 3 — Optimistic Locking

Focus: Version columns, compare-and-swap, conditional SQL updates, JPA `@Version`, conflict detection, retries, internal behavior, and when to choose optimistic versus pessimistic locking.

In the previous chapter, we learned that pessimistic locking prevents conflicting operations from proceeding simultaneously by making them wait for locks.

Optimistic locking takes a different approach:

Let concurrent operations proceed, but detect when an operation is trying to update a version of the data that has already changed.

This is especially useful for systems where conflicts are relatively rare, but correctness still matters.

## 1. Why do we need optimistic locking?

Imagine an application that lets users edit their profiles.

The database contains:

| user_id | display_name | city      |
| ------- | ------------ | --------- |
| 101     | Alex         | Hyderabad |

Two requests load the same record:

- Request A changes the city to Bengaluru.
- Request B changes the display name to Alexander.

Both read the same original record before either saves its changes.

If both requests overwrite the entire row without detecting changes, the later write can overwrite the earlier one.

This is the lost-update problem we encountered in Chapter 1.

One solution is to lock the row before editing. But holding a database transaction open while a user edits a form is usually a terrible design. A user might take several minutes to click Save.

Optimistic locking lets the application read the record, release the database transaction, allow the user to edit, and then check whether the record has changed when the user saves.

No long-lived database lock is required.

## 2. The core idea: a version number

Add a version column to the table.

```
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,
    display_name TEXT NOT NULL,
    city TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
```

Example data:

| user_id | display_name | city      | version |
| ------- | ------------ | --------- | ------- |
| 101     | Alex         | Hyderabad | 0       |

The version identifies the revision of the record that the application read.

Whenever a successful update changes the record, the version increases.

For example:

```
Initial record:
display_name = Alex
city         = Hyderabad
version      = 0
```

After Request A updates the city:

```
display_name = Alex
city         = Bengaluru
version      = 1
```

Request B still has the old version, `0`.

When Request B attempts to save, it must verify that the current version is still `0`. Because the database now contains version `1`, the update fails instead of silently overwriting newer data.

Notice that the version is not a lock. It is a way to detect stale writes.

## 3. How optimistic locking works step by step

Let's examine the complete lifecycle.

Invalid or unsupported diagram

The important sequence is:

1. Read the record and its version.
2. Perform the application's work without holding a database lock for the entire editing period.
3. Submit an update conditioned on the version previously read.
4. Increment the version as part of the update.
5. If no row is updated, treat the operation as a concurrency conflict.

This approach does not prevent concurrent requests from reading the same data. It prevents a stale update from silently succeeding.

## 4. Implementing optimistic locking in PostgreSQL

Let's implement it directly in SQL before introducing any framework.

### 4.1 Read the record

```
SELECT user_id, display_name, city, version
FROM users
WHERE user_id = 101;
```

Suppose the result is:

```
user_id       = 101
display_name  = Alex
city          = Hyderabad
version       = 0
```

The application retains `version = 0` along with the data it loaded.

### 4.2 Update using the version

Suppose the user changes the city to Bengaluru.

```
UPDATE users
SET city = 'Bengaluru',
    version = version + 1
WHERE user_id = 101
  AND version = 0;
```

There are two possible outcomes.

Outcome A: The version still matches.

The update affects one row, and the version becomes `1`. The operation succeeds.

Outcome B: Another request has already updated the record.

The version is no longer `0`. The update affects zero rows.

The application must recognize that zero affected rows means the expected version did not match, or that the target record no longer exists.

This is the essential implementation of optimistic locking.

### 4.3 Why is the version check atomic?

You might wonder whether two requests could both check `version = 0` and both succeed.

They cannot both successfully execute the same conditional update against the same row with the same expected version.

PostgreSQL coordinates concurrent updates to the row. Under `READ COMMITTED`, if one update waits for another transaction to finish, PostgreSQL re-evaluates the applicable `WHERE` condition against the updated row version.

Once the first successful update changes the version to `1`, the second update's condition `version = 0` no longer matches.

This gives us an important distinction:

- Optimistic locking does not avoid all database-level locking.
- It avoids acquiring an explicit pessimistic lock in advance.
- PostgreSQL still coordinates conflicting writes internally.
- The version condition determines whether the update is allowed to succeed.

Interview takeaway: Optimistic locking is conflict detection implemented using an atomic conditional write, not a guarantee that the database never uses locks.

### 4.4 Use `RETURNING` to simplify the application

PostgreSQL supports returning values from an update:

```
UPDATE users
SET city = 'Bengaluru',
    version = version + 1
WHERE user_id = 101
  AND version = 0
RETURNING user_id, display_name, city, version;
```

If a row is returned, the update succeeded.

If no row is returned, the update did not match the expected version or the record no longer exists.

For an API, you might translate a detected stale-write conflict into HTTP `409 Conflict`. If distinguishing a deleted record from a stale version matters, design that behavior explicitly rather than assuming every zero-row update has the same cause.

## 5. Optimistic locking with JPA and Hibernate

Since optimistic locking is common in Java backend systems, let's see how it works with JPA.

Consider this entity:

```
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "users")
public class User {

    @Id
    private Long userId;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String city;

    @Version
    private Long version;

    protected User() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCity() {
        return city;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
```

The important annotation is:

```
@Version
private Long version;
```

JPA uses this field to implement version-based optimistic concurrency control.

The version field is managed by the persistence provider. For a new entity, Hibernate initializes or assigns the appropriate initial version according to the mapping and provider behavior. For existing entities, Hibernate checks the version when applying updates.

### 5.1 What happens when Hibernate saves an entity?

Suppose Hibernate loads the entity with `version = 0`.

Your service changes its city:

```
@Transactional
public void updateCity(Long userId, String city) {
    User user = entityManager.find(User.class, userId);

    user.setCity(city);
}
```

You do not need to call an explicit update method for this managed entity. Hibernate tracks the change and synchronizes it with the database during flush, commonly at transaction commit.

Conceptually, Hibernate generates SQL resembling:

```
UPDATE users
SET city = ?,
    version = 1
WHERE user_id = ?
  AND version = 0;
```

The exact generated SQL depends on the mapping, provider, and configuration.

If the update affects zero rows because another transaction changed the version, Hibernate detects the stale update and reports an optimistic locking failure, commonly through `OptimisticLockException` or a provider-specific exception such as `StaleObjectStateException`, sometimes wrapped by Spring.

### 5.2 Why is `@Version` useful?

Without it, an ORM might overwrite a newer value with a stale value, depending on how the entity is updated.

With `@Version`, the ORM can detect that the database record has changed since it was read.

Typical uses include:

- Editing user profiles.
- Updating order metadata.
- Managing product descriptions.
- Editing administrative records.
- Updating configuration objects.

It is particularly useful when users can keep a page open while other users modify the same record.

### 5.3 Important JPA details

Do not manually increment the version field. Let the persistence provider manage it.

A managed entity's version is not a substitute for validation. Business rules must still be checked.

The failure can occur at flush or commit time. A service method may appear to finish its Java statements before Hibernate actually sends the update.

The transaction must be rolled back after a persistence failure. Catching an optimistic locking exception does not mean the existing transaction can safely continue.

Bulk JPQL updates require special care. A bulk update such as `UPDATE User u SET u.city = ...` bypasses normal managed-entity dirty checking. Do not assume that `@Version` automatically protects arbitrary bulk operations. Explicit version predicates, version updates, and persistence-context management may be necessary.

## 6. Compare-and-swap (CAS)

Optimistic locking is closely related to an important concurrency primitive called compare-and-swap, or CAS.

The idea is simple:

> Update the value only if the current value still matches the value I previously observed.

Conceptually:

```
compare(current_version, expected_version)

if they match:
    update the resource
    increment the version
else:
    report a conflict
```

In PostgreSQL, the conditional `UPDATE` we wrote earlier implements this pattern at the database level.

```
UPDATE users
SET city = 'Bengaluru',
    version = version + 1
WHERE user_id = 101
  AND version = 0;
```

The `version = 0` predicate is the comparison. The update is the conditional swap.

### CAS versus a normal read-modify-write

Consider a counter whose current value is 10.

An unsafe application-side operation might be:

```
Read value = 10
Calculate 10 + 1
Write value = 11
```

Two concurrent requests can both read 10 and both write 11, losing one increment.

A conditional update instead checks the expected value:

```
UPDATE counters
SET value = 11
WHERE counter_id = 1
  AND value = 10;
```

Only one concurrent request using the same expected value can successfully make that transition. The other must reread the value and try again if its business operation is still valid.

For a simple counter, however, you should normally use:

```
UPDATE counters
SET value = value + 1
WHERE counter_id = 1;
```

That operation lets PostgreSQL increment the value atomically without requiring application-managed retries.

Lesson: CAS is useful when you need to validate an expected state before making a transition. It is not always necessary for straightforward arithmetic operations.

### Where else is CAS used?

The same general concept appears in:

- CPU atomic instructions.
- Java's `AtomicInteger` and `AtomicReference`.
- Concurrent data structures.
- Redis conditional operations.
- Version-based database updates.
- Distributed coordination algorithms.

The implementations differ, but the underlying idea is to make a state change conditional on the state still being what you expected.

## 7. What if the optimistic update fails?

A conflict is not necessarily a system failure. It means the assumption that the data was unchanged turned out to be false.

There are three common responses.

### Strategy A: Return a conflict to the user

This is often appropriate for user-driven editing.

Example:

```
You opened this record at 10:00.
Another user updated it at 10:02.
You attempted to save at 10:05.
```

The application can return `409 Conflict` and explain that the record has changed. The user can reload the latest version and decide how to apply their edits.

This avoids silently overwriting another user's work.

### Strategy B: Reread and retry

This works when the operation can safely be recalculated against the latest state.

For example, a service might attempt a conditional update, detect a conflict, reread the record, recompute the intended change, and try again.

But retries are safe only if the business operation remains valid against the new state.

Suppose the original operation was "subtract ₹500 if the balance is sufficient." You cannot blindly retry based on the old balance. You must validate the current balance again.

### Strategy C: Merge the changes

For some applications, both users' changes can be preserved.

For example:

- Request A changes `city`.
- Request B changes `display_name`.

If these are independent fields and the business semantics permit merging, the application can load the latest record and apply only the intended field change.

However, merging is not always safe. If both requests change the same field, or if one field's value depends on another, the application needs a clear conflict-resolution rule.

## 8. Should we always retry optimistic locking failures?

No. Blind retries can make correctness worse.

Consider a limited coupon that can be redeemed only once.

Two requests both read:

```
redemption_count = 0
version = 0
```

The first request successfully redeems the coupon and increments the version.

The second request receives an optimistic locking failure.

If the second request rereads the record and blindly retries without rechecking eligibility, it could violate the business rule.

A correct retry must repeat the business validation against the latest state.

Use this decision process:

```
Optimistic locking conflict
          |
          v
Is the operation still valid
against the latest state?
          |
      +---+---+
      |       |
     Yes      No
      |       |
 Recompute   Reject or
 and retry   resolve conflict
```

In production, use bounded retries with backoff where appropriate. Avoid unbounded retry loops, especially under heavy contention, because they can increase database load and worsen the problem.

For Spring applications, make sure a retry starts a fresh transaction when the failed transaction has been marked rollback-only. A retry loop inside the same failed transaction is usually not sufficient.

Also consider side effects. If a failed database transaction already triggered an email, external API call, or payment request, repeating the operation can duplicate those effects. Prefer transactionally recorded intent, an outbox pattern, and idempotent downstream operations where appropriate.

## 9. Optimistic vs pessimistic locking: the real trade-off

Both approaches can preserve correctness, but they handle conflicts differently.

| Dimension                            | Optimistic locking                           | Pessimistic locking                                         |
| ------------------------------------ | -------------------------------------------- | ----------------------------------------------------------- |
| Main strategy                        | Detect conflicting changes                   | Prevent conflicting operations from proceeding concurrently |
| Typical mechanism                    | Version column or expected-value predicate   | Explicit database lock                                      |
| Initial behavior                     | Concurrent operations may proceed            | Conflicting operations wait or fail                         |
| Conflict detection                   | At update or commit                          | When acquiring or using the lock                            |
| Retry requirements                   | Often important                              | Still needed for deadlocks and transient failures           |
| Long user editing sessions           | Usually suitable                             | Usually unsuitable                                          |
| High contention                      | Can cause repeated conflicts and retries     | Can cause long lock waits                                   |
| Short, critical multi-step operation | Can work if conflicts are detected correctly | Often useful                                                |
| Simple counter increment             | Usually unnecessary                          | Usually unnecessary; atomic SQL is simpler                  |

### A practical example

Imagine an order-management system.

Scenario A: Users edit product descriptions.

Conflicts are relatively rare. A version column is usually appropriate.

Scenario B: Many requests compete for the same scarce inventory.

A conditional update or pessimistic lock may be appropriate, depending on whether the reservation can be expressed atomically.

Scenario C: A service increments a counter.

An atomic SQL update is generally simpler than either explicit locking or version-based retries.

The correct choice depends on the operation and its contention pattern, not merely on the table or service.

## 10. Common optimistic locking mistakes

These are particularly useful for backend engineering interviews.

### Mistake 1: Adding a version column but not checking it

This is not optimistic locking:

```
UPDATE users
SET city = 'Bengaluru'
WHERE user_id = 101;
```

Even if the table has a `version` column, the update above does not use it to detect stale writes.

A version column helps only when the update verifies the expected version and changes it atomically.

### Mistake 2: Checking the version separately

This is unsafe:

```
SELECT version
FROM users
WHERE user_id = 101;

-- Application checks version = 0.

UPDATE users
SET city = 'Bengaluru'
WHERE user_id = 101;
```

Another transaction can update the record between the `SELECT` and `UPDATE`.

The expected version must be part of the update condition itself.

### Mistake 3: Assuming every conflict should be retried

Sometimes the correct response is to reject the operation or ask the user to reconcile changes.

Retrying makes sense only when the operation can be recalculated safely against the latest state.

### Mistake 4: Assuming `@Version` protects every database write

JPA's normal versioned entity updates support optimistic locking, but native SQL, bulk updates, and writes performed outside the ORM need their own concurrency-control design.

### Mistake 5: Treating a version as a security mechanism

A version column prevents certain stale updates. It does not authorize a user to edit a record, validate business rules, or prevent malicious requests.

Authentication, authorization, validation, and concurrency control solve different problems.

## 11. When optimistic locking is the right choice

Use optimistic locking when:

- Conflicts are relatively infrequent.
- You want to avoid making transactions wait for an explicit lock before performing work.
- Users may edit a record over an extended period.
- Stale updates must be detected rather than silently overwriting newer data.
- Retrying or resolving conflicts is practical.

Avoid relying on it alone when:

- Conflicts are extremely frequent and retries would cause substantial wasted work.
- The operation requires exclusive coordination across multiple steps and cannot tolerate conflicting decisions.
- The business rule spans several records but your version checks protect only one of them.
- The application needs a guarantee that multiple changes occur atomically, but those changes are not enclosed in an appropriate transaction.

One important qualification: optimistic locking does not automatically protect multi-row invariants. For example, if two transactions independently check that fewer than 10 reservations exist and then insert a new reservation, versioning an unrelated row will not necessarily prevent the limit from being exceeded. You may need a database constraint, a shared coordination row, explicit locking, or a serializable transaction.

## 12. Interview-ready answer: Explain optimistic locking

A strong answer would be:

> Optimistic locking is a concurrency-control strategy that allows multiple transactions to read and work with the same data, but checks for conflicting updates when they attempt to write.
>
> A common implementation uses a version column. When an application reads a record, it captures the current version. When updating the record, it includes that version in the `WHERE` clause and increments it as part of the update. If another transaction has already changed the record, the version no longer matches, so the update affects zero rows and the application detects a conflict.
>
> It is particularly useful for low-contention workloads, such as editing user profiles or product information, where making every transaction wait for an explicit lock would be unnecessary. The trade-off is that conflicts require handling, potentially including retries or user intervention.
>
> In Java applications, JPA supports this pattern through `@Version`. In PostgreSQL, we can implement it directly with a conditional `UPDATE`. For simple operations like incrementing a counter, an atomic SQL statement may be simpler than explicit optimistic locking.

## 13. Chapter 3 summary

The key points to remember are:

1. Optimistic locking detects stale writes instead of explicitly locking a resource in advance.
2. A version column records which revision the application read.
3. The expected version must be checked atomically during the update.
4. PostgreSQL still uses internal locking to coordinate conflicting writes.
5. JPA's `@Version` provides version-based optimistic locking for supported entity operations.
6. Conflicts can be rejected, merged, or retried, depending on the business operation.
7. Blind retries, especially around payments and external side effects, can introduce duplicate operations.
8. Optimistic locking is not a replacement for transactions, database constraints, or authorization.

### What comes next?

Type `next` for Chapter 4 — PostgreSQL concurrency internals.

We'll go deeper into how PostgreSQL actually coordinates concurrent transactions:

- MVCC and row versions.
- Transaction snapshots and visibility.
- `READ COMMITTED`, `REPEATABLE READ`, and `SERIALIZABLE`.
- Why ordinary reads usually do not block writes.
- Row-lock implementation and `pg_locks`.
- Predicate locks, serialization failures, and write skew.
- How to choose the right isolation level for a production application.

