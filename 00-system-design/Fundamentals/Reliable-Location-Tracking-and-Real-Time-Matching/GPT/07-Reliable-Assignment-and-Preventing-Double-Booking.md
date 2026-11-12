# Chapter 7 — Reliable Assignment: Preventing Double Booking

This is arguably the most important correctness chapter in the entire architecture.

We've built a system that:

- Tracks drivers.
- Finds nearby drivers.
- Filters and ranks candidates.
- Sends offers through WebSockets.
- Receives accept/reject responses.
- Expires offers when drivers don't respond.

Now we need to solve a fundamental distributed systems problem:

How do we guarantee that one ride is assigned to at most one driver, even when multiple drivers accept simultaneously and multiple backend servers process their requests?

We'll build this from first principles.

We'll also solve another easily overlooked problem:

How do we guarantee that one driver isn't assigned to two different rides simultaneously?

These are two separate concurrency problems.

# 7.1 Understand the race condition first

Imagine:

```
Ride R501

Offers sent to:
    D101
    D102
    D103
```

All three drivers receive the offer.

Now imagine they all accept within a few milliseconds.

```
D101 ---- ACCEPT ----\
D102 ---- ACCEPT -----+---- Assignment Service
D103 ---- ACCEPT ----/
```

Our backend has three application servers:

```
                 Load Balancer
                       |
             +---------+---------+
             |         |         |
             v         v         v
          Server A  Server B  Server C
             |         |         |
             v         v         v
          Assignment Service Instances
```

Suppose:

```
D101's acceptance -> Server A
D102's acceptance -> Server B
D103's acceptance -> Server C
```

All three servers execute something like:

```
if (ride.getAssignedDriverId() == null) {
    ride.setAssignedDriverId(driverId);
}
```

Is this safe?

Absolutely not.

Let's see why.

### The race condition

Initially:

```
R501.assignedDriverId = null
```

Three servers read the same value.

```
Server A reads null
Server B reads null
Server C reads null
```

All three conclude that the ride is available.

Then:

```
Server A writes D101
Server B writes D102
Server C writes D103
```

Depending on execution order, the final value might be:

```
R501.assignedDriverId = D102
```

But D101 and D103 might also have received successful responses.

Now we have a serious correctness problem.

We must never allow the application to decide the winner using an ordinary read followed by an ordinary write.

The read-and-claim operation must be atomic.

That is the central idea of this chapter.

# 7.2 Why an in-memory lock doesn't solve this

A natural first thought:

```
synchronized (rideId.intern()) {
    if (ride.getAssignedDriverId() == null) {
        assign(driverId);
    }
}
```

This protects concurrent requests inside one JVM.

But remember our architecture.

We have:

```
Server A
Server B
Server C
```

Each server has its own memory.

Consider:

```
Server A:
    synchronized(R501)
    -> Acquires its local lock

Server B:
    synchronized(R501)
    -> Also acquires its local lock
```

Both can enter their respective critical sections.

Why?

Because:

```
Server A's lock != Server B's lock
```

An in-memory lock has no knowledge of locks acquired by another server.

So:

In-memory locks protect concurrency inside one process, not across distributed application instances.

They are still useful for local optimizations, but they cannot enforce a system-wide assignment invariant.

# 7.3 Could we use a Redis distributed lock?

Now let's introduce Redis.

We could try:

```
SET ride:R501:lock D101 NX PX 10000
```

Meaning:

- Create a lock for Ride R501.
- Store D101 as the lock owner.
- Only create it if no lock exists.
- Expire it after 10 seconds.

Suppose Server A executes this first.

```
Server A -> SET NX -> SUCCESS
Server B -> SET NX -> FAILURE
Server C -> SET NX -> FAILURE
```

Only one server acquires the lock.

This is useful!

But let's examine its limitations.

### Problem A: Lock expiration

Suppose Server A acquires the lock.

```
Lock expiry = 10 seconds
```

But Server A pauses due to:

- JVM garbage collection.
- CPU starvation.
- Network problems.
- A long-running operation.

The lock expires.

Server B acquires the same lock.

Now:

```
Server A still believes it owns the lock.
Server B believes it owns the lock.
```

Both might continue executing.

A lease expiration doesn't magically stop the old lock holder.

### Problem B: Redis failover

Redis replication is generally asynchronous.

Imagine:

```
Primary Redis:
    Lock created successfully.

Replica:
    Hasn't received the latest lock update yet.

Primary crashes.

Replica becomes primary.
```

The new primary might not contain the previously acknowledged lock.

A second server may now acquire the same lock.

Again, we can have multiple apparent owners.

### Problem C: Old lock holder deletes a new owner's lock

Consider:

```
Server A acquires lock with token A.
Lock expires.

Server B acquires lock with token B.

Server A wakes up and executes DEL lock.
```

Now Server A accidentally deletes Server B's lock.

This is why distributed lock release must compare the ownership token before deleting.

### Can we make Redis locks safer?

Yes.

Use:

- Unique ownership tokens.
- Atomic compare-and-delete.
- Appropriate lease handling.
- Fencing tokens when stale lock holders could continue performing operations.

But here's the important architectural decision:

We should not depend on a Redis lock as the final guarantee that a ride is assigned exactly once.

Redis can coordinate and reduce contention.

The authoritative assignment decision should be enforced by a durable transactional system.

# 7.4 The right solution — Database conditional updates

Let's look at the simplest powerful solution.

Suppose our `rides` table contains:

```
rides
-------------------------------
ride_id
status
assigned_driver_id
```

Initially:

```
ride_id = R501
status = SEARCHING
assigned_driver_id = NULL
```

Instead of:

```
Read assigned_driver_id
Check whether NULL
Write driver ID
```

We perform one conditional SQL update.

```
UPDATE rides
SET
    status = 'ASSIGNED',
    assigned_driver_id = 'D101'
WHERE
    ride_id = 'R501'
    AND status = 'SEARCHING'
    AND assigned_driver_id IS NULL;
```

Then inspect the number of affected rows.

### Case 1: D101 wins

```
Rows affected = 1
```

The assignment succeeded.

The ride is now:

```
R501:
    status = ASSIGNED
    assigned_driver_id = D101
```

### Case 2: D102 tries afterward

It executes:

```
UPDATE rides
SET
    status = 'ASSIGNED',
    assigned_driver_id = 'D102'
WHERE
    ride_id = 'R501'
    AND status = 'SEARCHING'
    AND assigned_driver_id IS NULL;
```

But the ride is no longer in `SEARCHING` with a null driver.

```
Rows affected = 0
```

D102 loses.

### Case 3: D103 also tries

Same result.

```
Rows affected = 0
```

Now we have:

```
R501 -> D101
```

Only one driver wins.

Why does this work?

Because the database executes the conditional update as an atomic operation.

Concurrent updates contend on the same database row.

The database ensures that only one operation can successfully satisfy the original condition and perform the transition.

This is a form of compare-and-set (CAS).

Conceptually:

```
Compare:
    Is the ride still SEARCHING and unassigned?

Set:
    Assign it to D101.
```

The comparison and modification happen together.

This is much safer than checking in application memory.

# 7.5 But there's another problem — One driver must not get two rides

So far, we've guaranteed:

```
One ride -> At most one driver
```

But imagine:

```
Ride A -> Customer A
Ride B -> Customer B
```

Both requests discover D101.

They are processed simultaneously.

```
Server A:
    Assign D101 to Ride A

Server B:
    Assign D101 to Ride B
```

Our previous SQL protects the ride, not the driver.

Both rides have different database rows.

Therefore, both conditional updates could succeed.

Now:

```
Ride A -> D101
Ride B -> D101
```

This is also incorrect.

We need two invariants.

Invariant 1: A ride can have at most one assigned driver.

Invariant 2: A driver can have at most one active assignment.

We must enforce both.

## Solution: Store driver operational state in the database

For example:

```
drivers
--------------------------------------
driver_id
availability_status
reserved_offer_id
reservation_expires_at
active_ride_id
```

Initially:

```
D101:
    availability_status = AVAILABLE
    active_ride_id = NULL
```

When we temporarily offer work to D101:

```
D101:
    availability_status = RESERVED_FOR_OFFER
    reserved_offer_id = O101
```

When D101 successfully accepts:

```
D101:
    availability_status = BUSY
    reserved_offer_id = NULL
    active_ride_id = R501
```

Now the database has an authoritative view of the driver's operational state.

We can use a conditional update:

```
UPDATE drivers
SET
    availability_status = 'BUSY',
    active_ride_id = 'R501',
    reserved_offer_id = NULL,
    reservation_expires_at = NULL
WHERE
    driver_id = 'D101'
    AND availability_status = 'RESERVED_FOR_OFFER'
    AND reserved_offer_id = 'O101'
    AND active_ride_id IS NULL;
```

Again:

- One row updated: successful driver state transition.
- Zero rows updated: driver is no longer eligible for this offer.

But we still have two independent updates:

1. Update the ride.
2. Update the driver.

What happens if the ride update succeeds but the driver update fails?

Or the driver update succeeds but the ride update fails?

We need both operations to succeed or neither should take effect.

This leads us to database transactions.

# 7.6 The most important assignment operation — One database transaction

Let's define our initial state.

Ride

```
R501:
    status = SEARCHING
    assigned_driver_id = NULL
```

Driver

```
D101:
    status = RESERVED_FOR_OFFER
    reserved_offer_id = O101
    active_ride_id = NULL
```

Offer

```
O101:
    ride_id = R501
    driver_id = D101
    status = PENDING
    expires_at = 10:00:15
```

Now D101 accepts.

Our assignment operation should perform the following actions inside one database transaction.

```
BEGIN TRANSACTION

1. Validate the offer.
2. Validate the ride.
3. Validate driver reservation.
4. Claim the ride.
5. Claim the driver.
6. Mark the offer ACCEPTED.
7. Cancel other pending offers.
8. Record resulting events.

COMMIT
```

If any mandatory operation fails:

```
ROLLBACK
```

Nothing should be partially committed.

### Why does this solve the problem?

Suppose two different drivers accept the same ride.

```
Transaction A -> D101 accepts R501
Transaction B -> D102 accepts R501
```

Both transactions contend on the same ride record.

Only one transaction can successfully transition the ride from:

```
SEARCHING -> ASSIGNED
```

The other transaction discovers that the ride is no longer assignable and rolls back.

Now consider two different rides trying to assign the same driver.

```
Transaction A -> R501 assigns D101
Transaction B -> R502 assigns D101
```

Both transactions contend on D101's driver record.

Only one can successfully transition:

```
RESERVED_FOR_OFFER -> BUSY
```

The other transaction cannot claim the driver.

Both conditions must succeed in the same transaction.

This is the central correctness guarantee.

# 7.7 Let's design the actual database schema

We'll use a relational database as the authoritative assignment store.

The following is a logical schema; PostgreSQL-style SQL is used in examples.

## A. Rides

```
CREATE TABLE rides (
    ride_id VARCHAR(100) PRIMARY KEY,

    customer_id VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    assigned_driver_id VARCHAR(100),

    created_at TIMESTAMP NOT NULL,
    assigned_at TIMESTAMP
);
```

Relevant states:

```
SEARCHING
ASSIGNED
IN_PROGRESS
COMPLETED
CANCELLED
UNFULFILLED
```

## B. Drivers

```
CREATE TABLE drivers (
    driver_id VARCHAR(100) PRIMARY KEY,

    availability_status VARCHAR(40) NOT NULL,

    reserved_offer_id VARCHAR(100),

    reservation_expires_at TIMESTAMP,

    active_ride_id VARCHAR(100),

    updated_at TIMESTAMP NOT NULL
);
```

Relevant states:

```
OFFLINE
AVAILABLE
RESERVED_FOR_OFFER
BUSY
```

Location information can remain in Redis.

We don't need to write every GPS update into this database row.

This table contains business-critical operational state transitions, not every coordinate update.

## C. Driver offers

```
CREATE TABLE driver_offers (
    offer_id VARCHAR(100) PRIMARY KEY,

    ride_id VARCHAR(100) NOT NULL,

    driver_id VARCHAR(100) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMP NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    accepted_at TIMESTAMP
);
```

Possible states:

```
PENDING
ACCEPTED
REJECTED
EXPIRED
CANCELLED
```

## D. Assignment history

We can also introduce a separate assignment table.

```
CREATE TABLE ride_assignments (
    assignment_id VARCHAR(100) PRIMARY KEY,

    ride_id VARCHAR(100) NOT NULL UNIQUE,

    driver_id VARCHAR(100) NOT NULL,

    assigned_at TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL
);
```

The `UNIQUE(ride_id)` constraint guarantees that one ride cannot have more than one assignment record in this model.

We also need to protect against one driver having multiple active assignments.

In PostgreSQL, one option is:

```
CREATE UNIQUE INDEX one_active_assignment_per_driver
ON ride_assignments(driver_id)
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
```

This ensures that a driver cannot have two active assignment rows.

When a ride is completed or cancelled, its assignment status transitions out of the active set.

A production schema can use different structures depending on how assignment history, reassignment, and cancellation are modeled.

### Why do we need both conditional updates and unique constraints?

Because they provide defense at different levels.

Conditional updates express the business transition.

Unique constraints protect invariants even if some application code path is incorrect.

For example:

```
Application-level checks:
    Helpful for early rejection.

Conditional database update:
    Enforces atomic state transition.

Unique constraint:
    Enforces the invariant at storage level.
```

Don't rely only on the Java service to ensure there are no duplicate assignments.

The database should reject invalid states independently.

# 7.8 The complete acceptance transaction

Now let's put it together.

Suppose D101 accepts offer O101 for Ride R501.

We can implement the logical operation as follows.

Step 1: Begin the transaction.

```
BEGIN;
```

Step 2: Lock and validate the offer.

```
SELECT *
FROM driver_offers
WHERE offer_id = 'O101'
FOR UPDATE;
```

Check:

```
Offer belongs to D101?
Offer belongs to R501?
Status is PENDING?
Offer hasn't expired?
```

Use the database clock for expiry decisions, not the mobile device's clock.

Step 3: Lock the ride row.

```
SELECT *
FROM rides
WHERE ride_id = 'R501'
FOR UPDATE;
```

Check:

```
Ride status = SEARCHING?
assigned_driver_id IS NULL?
```

If not, another driver has already won or the ride is no longer assignable.

Step 4: Atomically claim the driver.

```
UPDATE drivers
SET
    availability_status = 'BUSY',
    active_ride_id = 'R501',
    reserved_offer_id = NULL,
    reservation_expires_at = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE
    driver_id = 'D101'
    AND availability_status = 'RESERVED_FOR_OFFER'
    AND reserved_offer_id = 'O101'
    AND active_ride_id IS NULL;
```

Check affected rows.

If zero:

```
Driver is no longer reserved for this offer.
```

Rollback.

Step 5: Claim the ride.

```
UPDATE rides
SET
    status = 'ASSIGNED',
    assigned_driver_id = 'D101',
    assigned_at = CURRENT_TIMESTAMP
WHERE
    ride_id = 'R501'
    AND status = 'SEARCHING'
    AND assigned_driver_id IS NULL;
```

Again, check affected rows.

If zero, rollback.

Step 6: Mark the offer accepted.

```
UPDATE driver_offers
SET
    status = 'ACCEPTED',
    accepted_at = CURRENT_TIMESTAMP
WHERE
    offer_id = 'O101'
    AND status = 'PENDING';
```

Ensure exactly one row was updated.

Step 7: Record the assignment.

```
INSERT INTO ride_assignments (
    assignment_id,
    ride_id,
    driver_id,
    assigned_at,
    status
)
VALUES (
    'ASG-501',
    'R501',
    'D101',
    CURRENT_TIMESTAMP,
    'ASSIGNED'
);
```

The unique constraints provide additional protection.

Step 8: Cancel other pending offers.

For R501, any other active offers are no longer valid.

Conceptually:

```
O102 -> D102 -> CANCELLED
O103 -> D103 -> CANCELLED
```

Also release their temporary reservations, but only if those reservations still belong to the corresponding offer IDs.

Step 9: Write downstream events to the outbox.

For example:

```
RIDE_ASSIGNED
OFFER_CANCELLED
DRIVER_BECAME_BUSY
```

These events will be published after the transaction commits.

Step 10: Commit.

```
COMMIT;
```

Now our authoritative state is:

```
Ride R501:
    status = ASSIGNED
    assigned_driver_id = D101

Driver D101:
    status = BUSY
    active_ride_id = R501

Offer O101:
    status = ACCEPTED
```

Everything is committed together.

If an essential operation fails before commit, the entire transaction rolls back.

### One important implementation detail: Lock ordering

In a production implementation, define a consistent order for acquiring database row locks.

For example:

```
Offer -> Ride -> Driver
```

And use that order consistently across acceptance, cancellation, and expiration workflows.

When multiple driver rows must be locked, process them in a deterministic order.

This reduces deadlock risk.

Also, handle database deadlock and serialization failures through bounded transaction retries.

Do not retry indefinitely.

# 7.9 What happens when two drivers accept simultaneously?

Let's simulate the actual database behavior.

Initial state:

```
Ride R501:
    SEARCHING
    assigned_driver_id = NULL

Driver D101:
    RESERVED_FOR_OFFER
    offer_id = O101

Driver D102:
    RESERVED_FOR_OFFER
    offer_id = O102
```

Both drivers accept.

```
Transaction A:
    D101 accepts O101

Transaction B:
    D102 accepts O102
```

Both transactions try to lock the same ride.

```
                    rides table
                         |
                    Row R501
                         |
                 +-------+-------+
                 |               |
                 v               v
            Transaction A   Transaction B
                 |               |
              Acquires         WAITING
               lock               |
                 |                 |
                 v                 |
             Claim R501            |
                 |                 |
              COMMIT               |
                 |                 |
                 +---------------->+
                                   |
                              Acquires lock
                                   |
                                   v
                            Reads current state
                                   |
                                   v
                         R501 is already ASSIGNED
                                   |
                                   v
                              ROLLBACK
```

Final result:

```
R501 -> D101
```

D102 receives:

```
RIDE_ALREADY_ASSIGNED
```

The exact winner is determined by which valid transaction successfully claims the ride first.

It doesn't matter which driver is geographically closer anymore.

It doesn't matter which server receives the request first at the application layer.

It doesn't matter whether the two requests arrive within the same millisecond.

The database serializes the conflicting state transition.

The successful conditional transaction is our assignment decision's linearization point.

This is the operation that turns a potentially concurrent decision into one authoritative result.

# 7.10 What happens when two rides try to assign the same driver?

Now test the other invariant.

```
Ride R501 -> D101
Ride R502 -> D101
```

Suppose both offers were somehow sent.

Both drivers' acceptance transactions execute.

```
Transaction A:
    R501 + D101

Transaction B:
    R502 + D101
```

The transactions lock different ride rows.

But they eventually compete for the same driver row.

Initially:

```
D101:
    status = RESERVED_FOR_OFFER
```

Only one transaction can successfully change the driver state to:

```
BUSY
active_ride_id = R501
```

When the second transaction examines the driver, the reservation no longer matches its offer.

Its conditional update affects zero rows.

It rolls back.

Final result:

```
D101 -> R501
R501 -> D101

R502 -> Still SEARCHING
```

This is why protecting only the ride row isn't enough.

We need both invariants enforced inside the same transaction.

# 7.11 Why do we need a reservation before sending an offer?

Now let's return to an earlier question.

Can we simply discover a driver, send an offer, and wait until they accept before reserving them?

Technically, the final assignment can still be made safe using database constraints.

But the user experience and dispatch efficiency would be poor.

Imagine:

```
Customer A -> Finds D101
Customer B -> Finds D101
Customer C -> Finds D101
```

All three send offers.

D101 sees three different rides simultaneously.

Even if the database ensures only one successful assignment, we're creating:

- Unnecessary notifications.
- Driver confusion.
- Acceptance conflicts.
- More failed transactions.
- Poorer dispatch efficiency.

Therefore, we temporarily reserve the driver when creating the offer.

### Where should that reservation live?

We now have two choices.

Option A: Redis-only temporary reservation

```
SET driver:D101:reservation O101 NX PX 15000
```

Advantages:

- Very fast.
- Automatic TTL.
- Reduces conflicting dispatch attempts.

Disadvantages:

- Reservation can be lost during Redis failover.
- Not part of the durable assignment transaction.
- Cannot be the final correctness boundary.

Option B: Database-backed offer reservation

When creating the offer, run a conditional database update:

```
UPDATE drivers
SET
    availability_status = 'RESERVED_FOR_OFFER',
    reserved_offer_id = 'O101',
    reservation_expires_at = :expiry
WHERE
    driver_id = 'D101'
    AND availability_status = 'AVAILABLE'
    AND active_ride_id IS NULL;
```

If one row is updated, we can create the offer.

Both operations should be in the same database transaction:

```
BEGIN

1. Reserve driver D101.
2. Create offer O101.
3. Create outbox delivery event.

COMMIT
```

This prevents the system from creating a durable offer without successfully reserving the driver.

If the driver update fails, we don't create the offer.

### Which would I choose?

For strict operational coordination, I prefer a database-backed reservation as the authoritative reservation state.

Redis may additionally provide a fast advisory reservation or reduce contention, but it shouldn't be the only protection.

One important observation:

Our location ingestion happens hundreds of thousands of times per second.

Our business assignment operations happen much less frequently.

We don't need to involve the transactional database in every GPS update.

We only use it for meaningful business state transitions such as:

- Reserve a driver.
- Accept an offer.
- Release a reservation.
- Complete an assignment.

This is an important way of keeping the database workload manageable.

# 7.12 Handling the Redis lock and database transaction together

Suppose we still use Redis as a fast reservation mechanism.

The workflow might be:

```
Nearby Discovery
       |
       v
Rank Drivers
       |
       v
Redis SET NX
       |
       v
Database Reservation Transaction
       |
       v
Create Durable Offer
       |
       v
Publish Delivery Event
```

Why do both?

Redis helps prevent unnecessary competing dispatch attempts.

The database reservation is the authoritative business state.

If Redis succeeds but the database reservation fails:

```
Release Redis reservation
Skip driver
```

If Redis fails because another dispatcher reserved the driver:

```
Skip driver
```

If Redis suffers a failover and two dispatchers temporarily believe they have a reservation:

```
Database conditional update decides the winner.
```

This is defense in depth.

However, avoid making this more complicated than needed.

If the database reservation operation already has acceptable latency and throughput, you may not need a Redis lock at all.

Never add a distributed lock simply because the system is distributed. First see whether a conditional database update already solves the problem.

# 7.13 Assignment state machine — The heart of reliable matching

Let's now formalize the business state.

A ride might have:

```
REQUESTED
    |
    v
SEARCHING
    |
    v
ASSIGNED
    |
    v
DRIVER_ARRIVING
    |
    v
IN_PROGRESS
    |
    v
COMPLETED
```

Other transitions:

```
SEARCHING -> CANCELLED
SEARCHING -> UNFULFILLED

ASSIGNED -> CANCELLED
ASSIGNED -> REASSIGNMENT_PENDING
```

The exact states depend on business requirements.

For example, a driver cancelling after assignment might trigger reassignment rather than simply marking the customer request as unfulfilled.

### Driver state machine

```
OFFLINE
   |
   v
AVAILABLE
   |
   v
RESERVED_FOR_OFFER
   |
   +---- Reject / Expire / Cancel ---> AVAILABLE
   |
   v
BUSY
   |
   v
AVAILABLE
```

### Offer state machine

```
CREATED
   |
   v
PENDING
   |
   +---- Driver Accepts ---> ACCEPTED
   |
   +---- Driver Rejects ---> REJECTED
   |
   +---- Deadline Reached -> EXPIRED
   |
   +---- Ride Assigned ----> CANCELLED
   |
   +---- Customer Cancels -> CANCELLED
```

These state machines are interconnected.

For example:

```
Offer O101 ACCEPTED
          |
          v
Ride R501 ASSIGNED
          |
          v
Driver D101 BUSY
```

We should not update these records independently in unrelated code paths.

The state transitions must be coordinated by the assignment workflow.

### A very important rule

Don't allow arbitrary transitions.

For example:

```
AVAILABLE -> BUSY
```

should not happen merely because some server sends an update.

It should happen only as part of a valid assignment transaction.

Similarly:

```
RESERVED_FOR_OFFER -> AVAILABLE
```

should only happen if we are releasing the reservation belonging to that specific offer.

Otherwise, an old timeout worker might accidentally release a newer reservation.

# 7.14 The expiration vs acceptance race

This is one of the trickiest scenarios.

Suppose:

```
Offer O101

expiresAt = 10:00:15
status = PENDING
```

Two things happen almost simultaneously.

```
Timeout Worker:
    Expire O101

Driver:
    Accept O101
```

Which one should win?

The answer must be determined by an atomic state transition and a clearly defined deadline policy.

### Scenario A: Acceptance wins before expiration

```
10:00:14.900 -> Acceptance transaction claims O101
10:00:15.000 -> Expiration worker executes
```

The acceptance changes:

```
PENDING -> ACCEPTED
```

The expiration operation subsequently sees that the offer is no longer pending.

It does nothing.

### Scenario B: Expiration wins

```
10:00:15.001 -> Expiration transaction claims O101
10:00:15.002 -> Driver acceptance arrives
```

The offer changes:

```
PENDING -> EXPIRED
```

The acceptance sees an expired offer and fails.

### Scenario C: Both execute concurrently

Both transactions contend on the same offer row.

Only one can successfully transition the offer from `PENDING`.

The other observes the new state and fails.

### Use database time, not mobile time

The device might have an incorrect clock.

We must use authoritative server/database time to validate expiration.

Also, in PostgreSQL, `CURRENT_TIMESTAMP` represents the transaction start time. After waiting for a row lock, it might not reflect the current time.

For expiration checks that must reflect the moment the transition is evaluated, use an appropriate database clock function such as `clock_timestamp()` after acquiring the required locks.

The business rule should explicitly define the acceptance deadline.

For example:

> An acceptance is valid if the backend successfully claims the pending offer while the offer is still before its authoritative expiry time.

The timeout scheduler doesn't define validity.

The offer state and deadline together define validity.

# 7.15 A subtle but important issue — Old timeout workers must not release new reservations

Consider:

```
10:00:00 -> D101 reserved for O101
10:00:15 -> O101 expires
10:00:16 -> D101 reserved for O202
10:00:17 -> Delayed cleanup for O101 runs
```

Suppose the cleanup worker executes:

```
UPDATE drivers
SET
    availability_status = 'AVAILABLE',
    reserved_offer_id = NULL
WHERE driver_id = 'D101';
```

Disaster!

It releases O202's reservation.

The correct query is:

```
UPDATE drivers
SET
    availability_status = 'AVAILABLE',
    reserved_offer_id = NULL,
    reservation_expires_at = NULL
WHERE
    driver_id = 'D101'
    AND availability_status = 'RESERVED_FOR_OFFER'
    AND reserved_offer_id = 'O101';
```

Now:

```
Stored reservation = O202

Incoming cleanup = O101
```

The update affects zero rows.

The new reservation is safe.

This is another example of conditional state transitions.

Always include the identity of the state transition you're trying to reverse.

# 7.16 Duplicate acceptance requests and idempotency

Now consider a very realistic failure.

D101 accepts O101.

```
Driver App
    |
    | ACCEPT O101
    v
Backend
    |
    v
Database Transaction
    |
    v
Assignment SUCCESS
```

The database commits successfully.

But the backend response gets lost.

The driver application doesn't know whether the ride was assigned.

What will it do?

It retries:

```
ACCEPT O101
```

Our system must recognize this as the same logical operation.

Otherwise, we might:

- Attempt to create a second assignment.
- Return an incorrect failure.
- Send confusing notifications.
- Execute downstream side effects twice.

This is where idempotency comes in.

## Solution: Idempotency key

The mobile application generates a request ID for each logical acceptance operation.

For example:

```
{
  "requestId": "ACCEPT-991",
  "offerId": "O101",
  "action": "ACCEPT"
}
```

Every retry uses the same `requestId`.

The backend records the processing result.

Conceptually:

```
accept_requests

request_id       offer_id    result
-------------------------------------------
ACCEPT-991      O101        ACCEPTED
```

We enforce:

```
UNIQUE(request_id)
```

Now consider a duplicate request.

```
First request:
    ACCEPT-991 -> SUCCESS

Second request:
    ACCEPT-991 -> Already processed
```

We return the original result.

We do not execute a new assignment.

### What if the application accidentally generates a different request ID?

The offer itself also provides a natural idempotency boundary.

Suppose:

```
O101:
    status = ACCEPTED
    accepted_driver = D101
    ride = R501
```

D101 sends another acceptance request for O101 with a different request ID.

We check the persisted offer state.

If:

```
O101 is ACCEPTED
AND R501 is ASSIGNED to D101
```

We can return the existing successful result.

We must not create another assignment.

However, if O101 was lost to another driver, we return the losing result.

### Recommended acceptance behavior

| Situation                                | Response                          |
| ---------------------------------------- | --------------------------------- |
| First valid acceptance                   | `ACCEPTED`                        |
| Same request retried                     | Return previous result            |
| Same offer accepted again by same driver | Return previous successful result |
| Offer expired                            | `OFFER_EXPIRED`                   |
| Another driver won                       | `RIDE_ALREADY_ASSIGNED`           |
| Offer doesn't belong to driver           | `INVALID_OFFER`                   |
| Driver reservation was lost              | `DRIVER_NOT_AVAILABLE`            |

This makes the mobile application much easier to implement.

It can retry uncertain requests without worrying about creating duplicate business effects.

# 7.17 What if the assignment succeeds but the server crashes?

Let's examine a particularly important scenario.

```
1. D101 accepts O101.
2. Assignment database transaction commits.
3. Server crashes.
4. Driver never receives success response.
5. Driver retries.
```

What happens?

The new backend server queries the database:

```
O101 = ACCEPTED
R501 = ASSIGNED to D101
D101 = BUSY
```

It returns the already-recorded result.

The assignment is not repeated.

This illustrates an important property:

The database commit is the durable boundary, not the HTTP/WebSocket response.

The response simply communicates the result of an operation that has already happened.

We must design the system around this fact.

# 7.18 What if the database commits but WebSocket notifications fail?

Suppose assignment succeeds.

Now we need to:

1. Notify D101 that the ride is assigned.
2. Notify D102 and D103 that their offers are cancelled.
3. Notify the customer that a driver has been assigned.

Should we send all these messages while the database transaction is open?

No.

Imagine:

```
BEGIN TRANSACTION

Update ride
Update driver

Send WebSocket notification

WebSocket network hangs for 5 seconds

COMMIT
```

This is a bad design.

External network calls inside database transactions:

- Keep database locks open longer.
- Increase transaction duration.
- Increase deadlock probability.
- Couple database availability to network availability.

Instead, use the transactional outbox pattern.

### Write the assignment and events in one transaction

```
BEGIN;

UPDATE rides ...;
UPDATE drivers ...;
UPDATE driver_offers ...;

INSERT INTO outbox_events (
    event_id,
    event_type,
    aggregate_id,
    payload
)
VALUES
(
    'EVT-501',
    'RIDE_ASSIGNED',
    'R501',
    '{...}'
);

COMMIT;
```

The transaction guarantees:

```
Assignment committed
    AND
Required outbox events committed
```

Or neither.

A separate publisher reads the outbox and delivers the events.

```
              Assignment Database
                       |
             +---------+---------+
             |                   |
             v                   v
        Assignment          Outbox Events
                                 |
                                 v
                          Event Publisher
                                 |
                    +------------+------------+
                    |            |            |
                    v            v            v
                 Driver       Customer     Other Drivers
                 Assigned     Notified     Cancelled
```

If the publisher crashes after sending a message but before marking it published, it may send it again.

That's expected.

Every event should contain a stable event ID.

Consumers should process events idempotently.

For example:

```
EVT-501 received
    |
    v
Process RIDE_ASSIGNED
    |
    v
Mark event processed

EVT-501 received again
    |
    v
Already processed
    |
    v
Ignore duplicate
```

This is how we achieve reliable downstream processing without requiring exactly-once network delivery.

# 7.19 What happens if Redis and the database disagree?

Let's consider:

```
Redis:
    D101 -> AVAILABLE

Database:
    D101 -> BUSY
    active_ride_id = R500
```

Which one should win?

The database.

Redis is an operational projection.

It might be stale because:

- The database assignment committed but the Redis update failed.
- An event is delayed.
- A Redis failover lost recent state.
- A consumer is behind.

Therefore:

```
Redis discovery:
    Finds D101 as a potential candidate.

Database reservation:
    Rejects D101 because they are BUSY.
```

The matching engine should eventually remove D101 from the Redis available-driver index.

But it must never assign D101 based only on Redis availability.

Similarly:

```
Redis:
    D101 -> RESERVED_FOR_OFFER O101

Database:
    D101 -> AVAILABLE
```

The Redis reservation might be stale or orphaned.

Our authoritative business reservation state is the database.

The system should reconcile Redis operational state with database state through events or periodic repair.

### This gives us a useful hierarchy

| Data                           | Authority                                                   |
| ------------------------------ | ----------------------------------------------------------- |
| Latest geographical location   | Live location store, subject to freshness rules             |
| Nearby candidate index         | Redis/spatial index, eventually consistent                  |
| Temporary business reservation | Transactional assignment store                              |
| Successful ride assignment     | Transactional database                                      |
| WebSocket connection ownership | Current connection registry and gateway state               |
| Offer delivery status          | Delivery system, with offer validity determined by database |

There should be no ambiguity about which system decides whether a ride is assigned.

# 7.20 What if a driver accepts but the database is unavailable?

Suppose Redis works, WebSockets work, and the assignment database is unavailable.

D101 accepts.

Should we temporarily mark the ride assigned in Redis?

No.

That creates a dangerous situation.

Redis might claim:

```
R501 -> D101
```

But the authoritative durable assignment has not happened.

The system could crash, lose the Redis state, or create inconsistent assignments.

Our correct behavior should be:

```
Acceptance received
       |
       v
Assignment database unavailable
       |
       v
Do not declare success
       |
       v
Return retryable / processing failure
       |
       v
Driver retries using same idempotency key
```

The driver may see:

```
Unable to confirm assignment.
Please try again.
```

We might also maintain an internal pending-processing mechanism, but we must not show a confirmed assignment until the durable transaction succeeds.

This is called failing closed for assignment correctness.

We prefer temporarily delaying assignment rather than risking two drivers being assigned to the same ride.

# 7.21 What about the database becoming a bottleneck?

Now you might ask:

> Aren't we putting enormous load on the database by using it for assignment and reservations?

Let's compare the workloads.

Location updates:

```
333,333 updates/sec
```

Ride acceptance:

```
Much smaller volume
```

Driver reservations:

```
Proportional to selected offers
```

We deliberately do not route every GPS update through the transactional assignment database.

Only meaningful business transitions use durable transactions.

Also:

1. Keep transactions short.
2. Use indexed conditional updates.
3. Avoid external network calls inside transactions.
4. Avoid unnecessary reads before writes.
5. Use outbox publishing asynchronously.
6. Partition operational data appropriately as scale grows.
7. Retry deadlocks and transient database failures with bounded backoff.

We should also avoid making every nearby driver a database candidate.

First filter through Redis.

Only a small number of selected drivers should enter the reservation workflow.

For example:

```
1,000 geographical candidates
           |
           v
100 eligible candidates
           |
           v
20 ranked candidates
           |
           v
2 selected for offer
           |
           v
2 database reservations
```

The transactional database handles business state changes rather than raw geographical discovery.

# 7.22 One more protection — What if our application code is wrong?

Let's assume someone accidentally writes buggy code:

```
UPDATE rides
SET assigned_driver_id = 'D102'
WHERE ride_id = 'R501';
```

No condition.

They overwrite the existing assignment.

Can our database schema protect us?

A unique constraint alone cannot prevent this if the same ride row is overwritten.

We should use several layers of protection:

Layer 1: Conditional state transition

```
WHERE status = 'SEARCHING'
AND assigned_driver_id IS NULL
```

Layer 2: Unique active assignment constraint

Prevent duplicate active assignment records.

Layer 3: Database permissions and repository boundaries

Restrict direct assignment updates to the assignment service or stored procedure where appropriate.

Layer 4: State-machine validation

Do not allow arbitrary transitions.

Layer 5: Reconciliation and monitoring

Detect impossible states, such as:

```
Ride R501 -> D101
Ride R502 -> D101
```

or:

```
Ride R501 -> D101
Driver D101 -> AVAILABLE
```

These violations should generate alerts and enter a repair workflow.

The goal is to make invalid states difficult to create and easy to detect.

# 7.23 Why do we need database locks if conditional updates already exist?

You may have noticed that we've discussed two approaches:

1. Conditional updates.
2. Row-level database locks using `SELECT ... FOR UPDATE`.

Do we need both?

Not necessarily.

They solve related problems in slightly different ways.

## Approach A: Optimistic concurrency using conditional updates

```
UPDATE rides
SET status = 'ASSIGNED',
    assigned_driver_id = 'D101'
WHERE ride_id = 'R501'
  AND status = 'SEARCHING'
  AND assigned_driver_id IS NULL;
```

We don't explicitly lock the row beforehand.

The database handles the necessary locking during the update.

This is useful when:

- Contention is relatively low.
- The condition is straightforward.
- We can express the transition as one atomic update.

This is often called optimistic concurrency control.

## Approach B: Pessimistic concurrency using row locks

```
SELECT *
FROM rides
WHERE ride_id = 'R501'
FOR UPDATE;
```

Now the transaction explicitly locks the ride row.

Other transactions attempting to acquire a conflicting lock must wait.

We then inspect the state and perform related updates.

This is useful when:

- Multiple records must be inspected.
- Several related conditions must be validated.
- We need to perform a multi-step transition inside one transaction.

## Which approach should we use?

For our acceptance workflow, I would use a short transaction with explicit row locking and conditional updates.

Why both?

- Row locks help serialize the multi-record workflow.
- Conditional updates express the exact business preconditions.
- Unique constraints provide an additional invariant at the database level.

We do not need to lock the entire database or the entire `rides` table.

We only need to coordinate operations involving the particular ride and driver.

Also, PostgreSQL's `READ COMMITTED` isolation level can be sufficient for this design when we correctly use row locks, conditional updates, and constraints. We do not automatically need serializable transactions for every acceptance request.

Deadlocks and retryable transaction failures must still be handled.

# 7.24 A useful refinement — How to implement lock ordering

There is one practical detail when implementing this in production.

Different operations might touch:

```
Ride
Offer
Driver
```

Suppose acceptance locks the offer first and then the ride, while cancellation locks the ride first and then the offer.

We can create a deadlock:

```
Transaction A:
    Locks Offer
    Waits for Ride

Transaction B:
    Locks Ride
    Waits for Offer
```

Neither can proceed.

To reduce this risk, establish a consistent lock order.

One possible order:

```
1. Ride
2. Offer
3. Driver
```

The acceptance service can first perform a non-locking lookup to obtain the immutable `ride_id` for an `offer_id`.

Then:

```
BEGIN

Lock Ride
Lock Offer
Lock Driver

Validate all conditions
Perform transitions

COMMIT
```

Other workflows involving the same records should follow the same order.

For example:

- Offer cancellation.
- Customer cancellation.
- Offer expiration.
- Driver acceptance.

If multiple drivers must be locked, acquire their rows in a deterministic order.

This doesn't eliminate every possible deadlock, but it significantly reduces avoidable lock cycles.

The database must still be configured to detect deadlocks, and the application should retry transactions that fail for retryable concurrency reasons.

# 7.25 Assignment history and reassignment

One additional real-world scenario:

Suppose D101 successfully accepts R501.

The driver then encounters a problem and cancels the assignment.

Should we permanently prevent R501 from being assigned to another driver?

No.

Many real systems need reassignment.

We should distinguish:

```
Assignment History
```

from:

```
Currently Active Assignment
```

For example:

```
R501:

Assignment 1:
    D101
    CANCELLED

Assignment 2:
    D103
    ASSIGNED
```

Our assignment table stores history.

The database constraint should prevent multiple active assignments, not necessarily multiple historical attempts.

For PostgreSQL:

```
CREATE UNIQUE INDEX one_active_assignment_per_ride
ON ride_assignments(ride_id)
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
```

And:

```
CREATE UNIQUE INDEX one_active_assignment_per_driver
ON ride_assignments(driver_id)
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');
```

Now:

- A ride can have multiple historical assignment records.
- Only one assignment can be active for a ride.
- A driver can have only one active assignment.

When D101 cancels:

```
Assignment 1 -> CANCELLED
```

In the same authoritative transaction, we:

1. Update the ride's reassignment state.
2. Mark the current assignment cancelled.
3. Release D101 from the old ride.
4. Create an outbox event for dispatch to restart.

Now a new matching attempt can begin.

This is also how we avoid losing the history of why a particular driver was originally assigned and why that assignment ended.

# 7.26 Complete assignment flow — Everything together

Let's now build the complete workflow from the moment an offer is created to the moment the ride is successfully assigned.

\#chatgpt-mermaid-\_r_5og\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5og\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5og\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5og\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5og\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5og\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5og\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5og\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5og\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5og\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5og\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5og\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5og\_ p{margin:0;}#chatgpt-mermaid-\_r_5og\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5og\_ .label text,#chatgpt-mermaid-\_r_5og\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .node rect,#chatgpt-mermaid-\_r_5og\_ .node circle,#chatgpt-mermaid-\_r_5og\_ .node ellipse,#chatgpt-mermaid-\_r_5og\_ .node polygon,#chatgpt-mermaid-\_r_5og\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .rough-node .label text,#chatgpt-mermaid-\_r_5og\_ .node .label text,#chatgpt-mermaid-\_r_5og\_ .image-shape .label,#chatgpt-mermaid-\_r_5og\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5og\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .rough-node .label,#chatgpt-mermaid-\_r_5og\_ .node .label,#chatgpt-mermaid-\_r_5og\_ .image-shape .label,#chatgpt-mermaid-\_r_5og\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5og\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5og\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5og\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5og\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5og\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5og\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5og\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5og\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5og\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5og\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5og\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5og\_ .icon-shape,#chatgpt-mermaid-\_r_5og\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5og\_ .icon-shape p,#chatgpt-mermaid-\_r_5og\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5og\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5og\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5og\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5og\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5og\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5og\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5og\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5og\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5og\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5og\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5og\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5og\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5og\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5og\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5og\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5og\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .node rect,#chatgpt-mermaid-\_r_5og\_ .node circle,#chatgpt-mermaid-\_r_5og\_ .node ellipse,#chatgpt-mermaid-\_r_5og\_ .node polygon,#chatgpt-mermaid-\_r_5og\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5og\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5og\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5og\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5og\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5og\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Matching Engine SelectsDriverGenerate Offer IDDatabase TransactionDriver Available?Try Another DriverReserve DriverCreate Pending OfferCreate Outbox EventCommitDeliver OfferDriver AcceptsAssignment TransactionLock Ride, Offer, DriverOffer Valid and RideUnassigned?Reject AcceptanceClaim RideClaim DriverMark Offer AcceptedCancel Other OffersWrite Assignment Events toOutboxCommitAssigned SuccessfullyPublish NotificationsNoYesNoYes

Let's examine the guarantees.

| Requirement                                  | Mechanism                                                    |
| -------------------------------------------- | ------------------------------------------------------------ |
| Driver isn't offered conflicting assignments | Authoritative temporary reservation                          |
| Two drivers cannot win one ride              | Conditional ride transition + uniqueness                     |
| One driver cannot win two rides              | Conditional driver transition + active-assignment uniqueness |
| Expired offers cannot be accepted            | Authoritative expiry check                                   |
| Duplicate acceptance is safe                 | Idempotency key + offer state                                |
| Server crash after commit is recoverable     | Durable database transaction                                 |
| Notifications aren't lost permanently        | Transactional outbox                                         |
| Duplicate notifications are safe             | Event IDs + idempotent consumers                             |
| Old cleanup cannot remove new reservation    | Compare reservation ID                                       |
| Reassignment is supported                    | Assignment history + one-active-assignment constraint        |

# 7.27 Exactly-once assignment — What does that really mean?

You'll often hear statements like:

> We need exactly-once assignment.

Let's be precise.

We cannot guarantee that a driver sends exactly one network request.

A client might retry.

A gateway might redeliver a message.

An outbox publisher might publish the same event twice.

A consumer might process an event and crash before committing its offset.

Those things happen.

Instead, we ensure:

There can be at most one successful active assignment for a ride, and at most one active assignment for a driver.

The system may process the same command multiple times, but the durable business effect happens only once.

For example:

```
ACCEPT O101
ACCEPT O101
ACCEPT O101
ACCEPT O101
```

All four requests refer to one logical operation.

The database state becomes:

```
R501 -> D101
```

Not four assignments.

This is the practical way to build reliable distributed workflows.

You don't try to make the network exactly-once.

You make your business operations idempotent, atomic, and durable.

# 7.28 What if the database has replicas?

One final important point before we leave assignment correctness.

Suppose the assignment database has:

```
Primary DB
     |
     +---- Replica 1
     |
     +---- Replica 2
```

All assignment writes must go to the authoritative writer.

Do not perform this:

```
Read ride status from a lagging replica
    |
    v
Decide that ride is unassigned
    |
    v
Attempt assignment based on that stale state
```

Reads used for a correctness decision should use the authoritative transactional path.

The conditional update itself must execute against the database that serializes the assignment.

Also, database replication durability matters.

If an acknowledged transaction can disappear entirely after failover because replication was asynchronous, the system could lose an assignment it already told the driver was successful.

We should select database durability and failover guarantees that match the business requirement.

For a large multi-region deployment, we must also ensure that competing assignment writes for the same ride have a clearly defined authoritative region or consistency mechanism.

We'll address that in Chapter 10.

# 7.29 Chapter 7 — Final mental model

There are three different mechanisms, and each has a distinct responsibility.

```
Redis Lock / Reservation
        |
        v
Reduce unnecessary contention
and coordinate dispatch attempts

Database Conditional Updates
        |
        v
Atomically enforce business
state transitions

Database Constraints
        |
        v
Protect critical invariants
even if application code is wrong
```

Don't confuse these mechanisms.

| Mechanism                      | What it guarantees                                                   |
| ------------------------------ | -------------------------------------------------------------------- |
| Java synchronized / local lock | Concurrency inside one process                                       |
| Redis `SET NX`                 | Fast distributed coordination, subject to lease/failover limitations |
| Database row lock              | Serializes conflicting database transactions                         |
| Conditional update             | Atomically checks and changes state                                  |
| Unique constraint              | Rejects invalid duplicate records                                    |
| Database transaction           | Commits related state changes together                               |
| Idempotency key                | Makes repeated requests safe                                         |
| Transactional outbox           | Makes downstream event publication recoverable                       |

## The architecture I would recommend

For our ride-hailing system:

- Redis maintains the fast live location index.
- Redis GEO helps discover nearby drivers.
- The matching engine selects and ranks candidates.
- A transactional database maintains authoritative driver reservations and assignments.
- WebSockets deliver offers to active drivers.
- A transactional outbox makes notifications recoverable.
- Conditional database updates and unique constraints prevent double assignment.
- Idempotency makes retries safe.

The most important principle: Redis helps us find and coordinate drivers. The transactional database decides who actually owns the ride.

# Next: Chapter 8 — Driver Availability, Heartbeats and Failure Detection

Now let's address another major problem from your original question:

> What happens when drivers stop sending location updates or stop responding to heartbeats?

Consider these scenarios:

```
Scenario A:
    WebSocket disconnected.
    HTTP location updates still working.

Scenario B:
    WebSocket connected.
    GPS updates stopped.

Scenario C:
    GPS updates working.
    Application heartbeat stopped.

Scenario D:
    Driver's phone has no network.

Scenario E:
    Driver force-closes the application.

Scenario F:
    Driver is moving but GPS updates are delayed.

Scenario G:
    Driver's backend gateway crashes.
```

Should all these situations make the driver offline?

No.

We'll build the liveness architecture by separating different signals:

1. Connection heartbeat.
2. Application heartbeat.
3. Location freshness.
4. Driver availability.
5. Assignment state.

We'll also cover:

- Heartbeat intervals and timeouts.
- TTL-based liveness detection.
- Redis sorted sets and expiration.
- Why a missed heartbeat doesn't immediately mean offline.
- Network partitions and false failure detection.
- Reconnection and session epochs.
- Stale driver cleanup.
- Temporary GPS failures.
- How to avoid matching with disconnected or stale drivers.
- What happens during massive regional reconnections.

The central concept:

A driver can be connected but not reporting location, reporting location but disconnected from WebSockets, or online but unavailable for work. These must be modeled independently.

Say next to continue.
