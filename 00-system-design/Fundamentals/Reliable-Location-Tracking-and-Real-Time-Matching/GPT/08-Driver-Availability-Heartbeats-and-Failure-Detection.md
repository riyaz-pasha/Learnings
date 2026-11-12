# Chapter 8 — Driver Availability, Heartbeats and Failure Detection

This chapter addresses one of the most important real-world problems in location tracking:

How do we know whether a driver is actually online, reachable, and eligible for a new ride?

So far, we've assumed that drivers continuously send location updates.

But real mobile applications don't behave that perfectly.

Phones lose connectivity. Applications go into the background. GPS permissions get disabled. WebSocket connections break. Backend servers crash. Drivers force-close applications.

Sometimes the driver is perfectly fine, but our server temporarily cannot communicate with them.

We need to design a system that handles all these scenarios without incorrectly assigning work to unavailable drivers.

Let's start with a simple example.

## 8.1 First, understand why a single `ONLINE` flag is insufficient

Imagine our driver record contains:

```
D101:
    online = true
    latitude = 17.3850
    longitude = 78.4867
```

Is D101 currently eligible for a ride?

We don't know.

Consider these situations:

| Situation | WebSocket    | Location updates   | Business availability |
| --------- | ------------ | ------------------ | --------------------- |
| A         | Connected    | Fresh              | Available             |
| B         | Disconnected | Fresh through HTTP | Available             |
| C         | Connected    | Stale              | Available             |
| D         | Connected    | Fresh              | Busy                  |
| E         | Disconnected | Stale              | Available             |
| F         | Connected    | Fresh              | Reserved for an offer |

All six situations are different.

Yet a simplistic system might represent all of them using:

```
online = true
```

That's not sufficient.

We need to separate different dimensions of driver state.

## 8.2 Model driver state using independent dimensions

I recommend separating at least four concepts.

### A. Connection state

Tells us whether the driver currently has an active communication channel.

```
CONNECTED
DISCONNECTED
UNKNOWN
```

Example:

```
D101:
    WebSocket = CONNECTED
    Gateway = Gateway-A
```

This is relevant for delivering immediate offers.

But a WebSocket disconnection doesn't necessarily mean the driver is unavailable.

They might still be sending location updates through HTTP.

### B. Location freshness

Tells us whether the driver's reported location is recent enough to trust.

```
FRESH
STALE
UNKNOWN
```

Example:

```
D101:
    Last location received = 10:00:00
    Current time = 10:00:05
    Location status = FRESH
```

Suppose our threshold is 15 seconds.

At 10:00:20:

```
Location status = STALE
```

This is independent of WebSocket connectivity.

### C. Business availability

Tells us whether the driver is willing and permitted to take new work.

```
OFFLINE
AVAILABLE
RESERVED_FOR_OFFER
BUSY
SUSPENDED
```

This is business state.

For example:

```
D101:
    Location = FRESH
    Connection = CONNECTED
    Availability = BUSY
```

The driver is perfectly reachable and reporting location but must not receive another ride.

### D. Assignment state

Tells us whether the driver already has an active business assignment.

```
NO_ACTIVE_ASSIGNMENT
ASSIGNED
IN_PROGRESS
```

This is related to availability, but keeping the assignment relationship explicit is useful for consistency and recovery.

For example:

```
D101:
    Availability = BUSY
    Active Ride = R501
    Assignment Status = IN_PROGRESS
```

The availability state should be derived or transitioned consistently with the assignment.

### Putting everything together

Our driver might look like:

```
D101
--------------------------------
Connection:
    CONNECTED

Location:
    FRESH

Availability:
    AVAILABLE

Assignment:
    NONE

Last Location:
    10:00:03

Last App Heartbeat:
    10:00:04

Last Connection Pong:
    10:00:05
```

This is much more meaningful.

Our matching system can now define eligibility explicitly.

For example:

\\[ Eligible = Authenticated \land LocationFresh \land BusinessAvailable \land NoConflictingAssignment \land ServiceCompatible \\]

Connection state is not necessarily a mandatory condition if we support another delivery mechanism such as push notifications.

This is an important distinction.

# 8.3 What exactly is a heartbeat?

A heartbeat is a periodic signal sent to indicate that some component is still responsive.

Imagine D101 has an active WebSocket connection.

The gateway periodically sends:

```
PING
```

The client responds:

```
PONG
```

This tells us that the communication path is responsive.

Conceptually:

```
Driver App                    Gateway
    |                            |
    |<--------- PING ------------|
    |                            |
    |---------- PONG ----------->|
    |                            |
    |       Wait interval        |
    |                            |
    |<--------- PING ------------|
    |                            |
    |---------- PONG ----------->|
```

Suppose we send a ping every 20 seconds.

We expect a pong.

If several consecutive pings receive no response, we consider the connection unhealthy and eventually close it.

But here's an important question:

Does a successful heartbeat prove that the driver is sending location updates?

No.

Let's introduce three separate heartbeat signals.

# 8.4 Three different liveness signals

## Signal 1 — Connection heartbeat

Purpose:

Detect whether an established communication connection is responsive.

For WebSockets, we can use protocol-level Ping/Pong frames.

For example:

```
Gateway -> PING
Driver  -> PONG
```

This is primarily useful to detect broken or unresponsive connections.

Suppose:

```
WebSocket connected = true
Last successful pong = 10:00:00
Current time = 10:00:50
```

If our connection timeout policy is 40 seconds, we can consider the connection dead and close it.

The client can then reconnect.

### Who sends the heartbeat?

Usually, the gateway sends periodic pings to the client.

Why?

Because a dead mobile connection might not immediately generate a clean TCP disconnect event.

A network may silently disappear.

Examples:

- Wi-Fi gets disabled.
- Mobile signal disappears.
- Phone switches networks.
- NAT state expires.
- The device goes to sleep.

Without heartbeats, the server may continue believing the connection exists.

### TCP keepalive vs WebSocket Ping/Pong

These are different mechanisms.

TCP keepalive operates at the transport layer.

WebSocket Ping/Pong operates at the application protocol layer.

WebSocket Ping/Pong lets our application infrastructure actively test whether the connection is responding.

Operating-system TCP keepalive can also help detect dead connections, but its default timing may not match our application's requirements.

For a real-time dispatch system, explicit WebSocket heartbeat handling is usually easier to control.

## Signal 2 — Application heartbeat

Now imagine the application is alive, but its location-reporting component has stopped working.

We need a separate application-level heartbeat.

For example:

```
POST /drivers/heartbeat
```

Payload:

```
{
  "sessionEpoch": 12,
  "sequenceNumber": 51
}
```

This tells us that the authenticated application is still active.

The server updates:

```
D101:
    lastAppHeartbeatAt = 10:00:05
```

Why do we need this if WebSocket Ping/Pong already exists?

Because they answer different questions.

```
WebSocket Ping/Pong:
    Is the connection responsive?

Application heartbeat:
    Is the application actively communicating with us?
```

For an HTTP-based location ingestion design, the application heartbeat can also be sent over HTTP.

However, don't automatically send both a WebSocket heartbeat and an HTTP heartbeat if one already provides the signal you actually need.

We should avoid unnecessary traffic.

A location update can also act as evidence of application activity, but a separate heartbeat may be useful when we need to distinguish an idle driver from a malfunctioning location-reporting component.

## Signal 3 — Location heartbeat

A location update itself is a form of liveness signal.

Suppose D101 sends location updates every three seconds.

```
10:00:00 -> Location A
10:00:03 -> Location B
10:00:06 -> Location C
10:00:09 -> Location D
```

Every accepted update refreshes:

```
lastLocationReceivedAt
```

We now know:

- The driver application is reporting location.
- The backend received a fresh location observation.
- The live location index can be updated.

This is generally the most important signal for nearby matching.

Consider:

```
Connection = CONNECTED
Application Heartbeat = FRESH
Location = STALE
```

The application may be alive, but its GPS reporting has stopped.

We should not automatically treat the driver as a fresh nearby candidate.

### Comparison

| Signal                | What it detects           | What it does not guarantee                    |
| --------------------- | ------------------------- | --------------------------------------------- |
| WebSocket Ping/Pong   | Connection responsiveness | Fresh GPS location                            |
| Application heartbeat | Application communication | Valid GPS position                            |
| Location update       | Fresh reported position   | WebSocket connectivity or driver availability |

This separation prevents a large number of false assumptions.

# 8.5 How do we decide that a driver is offline?

This is more subtle than it initially appears.

Suppose we're sending heartbeats every 20 seconds.

```
10:00:00 -> Heartbeat received
10:00:20 -> Expected heartbeat
10:00:40 -> Expected heartbeat
```

What if the heartbeat at 10:00:20 doesn't arrive?

Should we mark the driver offline immediately?

No.

Maybe:

- The mobile network is experiencing temporary packet loss.
- The request is delayed.
- The backend is overloaded.
- The device is switching networks.
- The client application is temporarily suspended.

A missing heartbeat doesn't prove the driver is offline.

It only tells us that we haven't observed recent communication.

This is a classic distributed systems problem called failure detection.

## The failure detector mental model

We cannot reliably distinguish these two situations merely from a missing message:

```
Situation A:
    Driver's phone is dead.

Situation B:
    Driver's phone is alive,
    but the network is temporarily unavailable.
```

From the server's perspective, both may look identical.

Therefore, we use timeouts and suspicion rather than claiming certainty.

For example:

```
No heartbeat for 20 seconds:
    Connection is suspicious.

No heartbeat for 40 seconds:
    Connection is probably unhealthy.

No heartbeat for 60 seconds:
    Close connection and remove its registry lease.
```

These are illustrative timings, not universal recommendations.

The key idea:

We use a timeout policy to decide when a missing signal is operationally significant.

There is no perfect timeout that distinguishes every dead device from every temporarily unreachable device.

# 8.6 Choose separate freshness thresholds for separate signals

We should not use one timeout for everything.

For example, consider:

```
Location update interval = 3 seconds
WebSocket ping interval = 20 seconds
Application heartbeat = 15 seconds
```

We might initially configure:

| Signal                | Expected frequency | Example stale threshold |
| --------------------- | ------------------ | ----------------------- |
| Location update       | 3 seconds          | 12–15 seconds           |
| Application heartbeat | 15 seconds         | 45 seconds              |
| WebSocket Ping/Pong   | 20 seconds         | 40–60 seconds           |

Why are the thresholds different?

Because the signals have different purposes.

We care about location freshness much more aggressively than connection freshness.

For example:

```
WebSocket = CONNECTED
Last location update = 30 seconds ago
```

The connection might still be healthy.

But the driver isn't a trustworthy candidate for a nearby ride.

Our matching engine should reject the driver based on location freshness without waiting for the WebSocket connection to die.

### How do we calculate location freshness?

Use the backend's receive timestamp.

For example:

```
lastLocationReceivedAt = 10:00:00

Current server time = 10:00:10
```

Then:

\\[ LocationAge = CurrentTime - LastLocationReceivedAt \\]

\\[ LocationAge = 10 \text{ seconds} \\]

If the freshness threshold is 15 seconds:

```
Location = FRESH
```

At 20 seconds:

```
Location = STALE
```

We should use a consistent clock source and preferably UTC timestamps.

For distributed systems, remember that different application servers can have slightly different clocks.

Clock synchronization helps, but correctness should not depend on extremely precise agreement between all server clocks.

Use database or authoritative server time for business deadlines. Use backend-received timestamps and bounded clock-skew policies for location freshness.

# 8.7 Implementing liveness with Redis TTL

Now let's introduce a practical implementation.

Suppose we store:

```
driver:D101:last-location
driver:D101:last-app-heartbeat
driver:D101:connection
```

We could use Redis timestamps and TTLs.

For example:

```
SET driver:D101:last-app-heartbeat 1794477600 EX 45
```

Every application heartbeat refreshes this key.

If no heartbeat arrives within 45 seconds, the key expires.

Similarly, our connection registry could maintain a gateway lease:

```
driver:D101:connection
    gatewayId = Gateway-A
    connectionId = Conn-101
```

The gateway periodically refreshes the lease.

If the gateway dies and stops refreshing, the key expires.

This is convenient because Redis handles expiration automatically.

But there is something we must understand.

### TTL is good for temporary state, but it doesn't automatically solve driver availability

Suppose:

```
driver:D101:app-heartbeat
```

expires.

Should we automatically execute:

```
drivers:D101 = OFFLINE
```

Not necessarily.

The driver might still be reporting GPS updates through another channel.

Or the driver might be busy completing a ride.

Business availability and communication liveness are separate.

A better model is:

```
Connection lease expires:
    Connection = DISCONNECTED

Location freshness threshold exceeded:
    Location = STALE

Explicit offline action:
    Availability = OFFLINE
```

Then the matching system combines these signals according to policy.

### Important: Don't put TTL on the entire GEO index

Remember our earlier Redis GEO discussion.

Suppose:

```
geo:{p3}
    D101
    D102
    D103
```

We cannot set a TTL on the entire key to expire D101 individually.

That would delete all three drivers.

Instead:

- Keep per-driver location timestamps in metadata.
- Maintain a per-driver freshness index.
- Use conditional cleanup for stale GEO members.

We'll implement that next.

# 8.8 Efficient stale-driver detection using a sorted set

Suppose we have one million drivers.

We don't want to scan every driver every second to find stale locations.

That would be wasteful.

We can maintain a Redis sorted set for last-seen timestamps.

For example:

```
ZSET: lastseen:{p3}
```

Members:

```
D101 -> 1794477600
D102 -> 1794477603
D103 -> 1794477580
D104 -> 1794477595
```

The sorted-set score represents the latest accepted server-received timestamp.

Every valid location update refreshes the corresponding score.

For example:

```
ZADD lastseen:{p3} 1794477605 D101
```

Now the stale-driver cleanup worker can efficiently identify drivers whose last-seen time is older than a threshold.

Suppose:

```
Current time = 10:00:30

Freshness threshold = 15 seconds

Cutoff = 10:00:15
```

We query:

```
ZRANGEBYSCORE lastseen:{p3} -inf 1794477615
```

The actual timestamp must correspond to the selected cutoff; the example score is illustrative.

This returns drivers whose recorded last-seen time is at or before the cutoff.

The cleanup worker processes those drivers.

```
Cleanup Worker
      |
      v
Find stale D101, D103, D104
      |
      v
Verify each driver's current last-seen version
      |
      v
Remove stale drivers from GEO
      |
      v
Remove stale metadata
      |
      v
Remove last-seen entry
```

### Why do we need to verify the timestamp again?

Imagine:

```
10:00:10 -> D101 becomes stale candidate

10:00:11 -> Cleanup worker reads D101

10:00:12 -> D101 sends a fresh location

10:00:13 -> Cleanup worker deletes D101
```

Disaster!

We just deleted a fresh location.

To prevent this, cleanup must recheck the current timestamp or location version atomically before deleting anything.

Conceptually:

```
ATOMIC CLEANUP:

if storedLastSeen <= cutoff:
    remove D101 from GEO
    delete metadata
    remove D101 from lastseen
else:
    do nothing
```

Because location and cleanup metadata must be updated together, we should perform the operation atomically within the relevant Redis partition.

Also, the cleanup worker should be idempotent.

Running the same cleanup twice must not corrupt state.

### Does this mean our driver is permanently offline?

Not necessarily.

It means the old location has been removed from the live geographical index.

The driver's business state might still be:

```
AVAILABILITY = ONLINE
LOCATION = STALE
```

The driver can recover simply by sending a new valid location update.

The important thing is that the driver must not be considered matchable until its location becomes fresh again.

# 8.9 Location freshness should not depend only on cleanup workers

This is an important correctness rule.

Suppose:

```
Last update = 10:00:00
Current time = 10:00:20
Freshness threshold = 15 seconds
```

Our cleanup worker runs every 30 seconds.

It hasn't executed yet.

The driver is still physically present in Redis GEO.

Can nearby discovery return that driver?

Yes, Redis GEO might return it.

Should matching select that driver?

No.

The matching service must verify location freshness independently.

In other words:

```
GEO index:
    Helps discover geographical candidates.

Freshness validation:
    Determines whether candidate location is trustworthy.
```

The two responsibilities are separate.

A delayed cleanup process should never make an old location eligible for dispatch.

### A useful implementation

During candidate validation:

```
boolean isLocationFresh(
        long lastReceivedAt,
        long now,
        long freshnessThresholdMillis) {

    return now - lastReceivedAt <= freshnessThresholdMillis;
}
```

The actual implementation should also reject impossible future timestamps and account for clock behavior.

For example, if the last received timestamp comes from a shared authoritative time source, freshness evaluation becomes more predictable.

# 8.10 What happens when the driver reconnects?

Now imagine:

```
D101

10:00:00 -> Last location received
10:00:15 -> Location considered stale
10:00:20 -> Driver network reconnects
10:00:21 -> New location update arrives
```

What should happen?

We need to transition the operational view back to fresh.

```
Before:

Location = STALE
Availability = AVAILABLE
```

After accepting the new location:

```
Location = FRESH
Availability = AVAILABLE
```

The new location is inserted back into the GEO index.

The driver becomes a valid geographical candidate again.

But be careful.

A reconnect must not automatically reset every business state.

Suppose the driver was:

```
Availability = BUSY
Active Ride = R501
Location = STALE
```

After reconnect:

```
Availability = BUSY
Active Ride = R501
Location = FRESH
```

They should not suddenly become available simply because they reconnected.

Similarly, a driver with a valid pending reservation should not lose it merely because their WebSocket session changed.

Session and connection recovery must be independent of business assignment state.

# 8.11 Session epochs — Prevent old connections from corrupting new state

Now let's introduce an important distributed systems concept: session epochs.

Imagine D101 connects to Gateway A.

```
D101:
    sessionEpoch = 10
    gateway = A
```

The driver disconnects and reconnects to Gateway B.

```
D101:
    sessionEpoch = 11
    gateway = B
```

Now suppose Gateway A receives a delayed heartbeat from the old session.

```
Gateway A:
    sessionEpoch = 10
```

Should this heartbeat refresh D101's liveness?

No.

The server must reject it because session 10 is no longer the current session.

Why?

Otherwise, an old connection might:

- Refresh a stale connection registry entry.
- Overwrite new location metadata.
- Extend an old reservation lease.
- Change connection ownership.
- Make an old session appear active.

We need a session identifier or monotonically increasing epoch.

For every valid driver session:

```
D101:

Session 10:
    Old connection

Session 11:
    Current connection

Session 12:
    Future connection
```

Every heartbeat and location update carries its session information.

The server verifies that the update belongs to the current valid session.

A robust session-start process can look like:

```
Driver reconnects
       |
       v
Authenticate driver
       |
       v
Create new session epoch
       |
       v
Register new connection
       |
       v
Reject messages from old sessions
```

For Redis, if the session metadata and location GEO index share a hash slot, we can validate the session epoch inside the atomic update script.

For example:

```
ATOMIC LOCATION UPDATE:

if incomingEpoch < storedEpoch:
    REJECT

if incomingEpoch == storedEpoch
   and incomingSequence <= storedSequence:
    REJECT

otherwise:
    UPDATE session
    UPDATE location
    UPDATE GEO index
    UPDATE lastSeen
```

The exact session creation policy must also be protected from concurrent reconnect attempts.

For example, use an authoritative session-generation operation rather than trusting a device to arbitrarily submit a larger epoch.

### What about delayed disconnect events?

This is another common failure.

```
10:00:00 -> Session 10 connected to Gateway A

10:00:10 -> Session 10 disconnected

10:00:11 -> Session 11 connected to Gateway B

10:00:12 -> Delayed disconnect event for Session 10 arrives
```

The disconnect handler should perform:

```
Set DISCONNECTED only if:

storedSessionEpoch == 10
AND storedConnectionId == oldConnectionId
```

Since the current session is 11, the operation fails.

Gateway A cannot accidentally disconnect Gateway B's new connection.

This is the same compare-and-set principle we used for driver reservations.

# 8.12 Should a driver automatically become offline when their location becomes stale?

This is a business decision, not simply a technical one.

Let's distinguish two possible meanings.

Meaning 1: The driver explicitly wants to be online.

The driver toggles:

```
GO ONLINE
```

We store:

```
businessAvailability = AVAILABLE
```

Then the driver loses connectivity.

Should the business status immediately become `OFFLINE`?

Not necessarily.

We might retain the explicit online intent for a short recovery window while marking the location stale.

Meaning 2: The driver is operationally matchable.

This is a derived condition:

```
Matchable =
    Business Availability = AVAILABLE
    AND Location = FRESH
    AND No Active Reservation
    AND No Active Assignment
```

So even if the driver's explicit online intent is retained, a stale driver is not matchable.

### My recommendation

Keep explicit online intent separate from effective matchability.

For example:

```
Driver D101

onlineIntent = ONLINE

locationStatus = STALE

connectionStatus = DISCONNECTED

businessAvailability = AVAILABLE

effectiveMatchability = NOT_MATCHABLE
```

When a fresh location arrives:

```
effectiveMatchability = MATCHABLE
```

When the driver explicitly goes offline:

```
onlineIntent = OFFLINE

effectiveMatchability = NOT_MATCHABLE
```

This is useful because a temporary network disruption doesn't necessarily mean the driver intended to stop working.

But the system still protects customers by refusing to offer rides based on stale locations.

# 8.13 How should an explicit online/offline transition work?

Consider a driver opening the application.

They tap:

```
GO ONLINE
```

The backend should perform a controlled business transition.

Conceptually:

```
OFFLINE
   |
   | Driver requests GO ONLINE
   v
ACTIVATING
   |
   | Authentication and eligibility checks
   | Fresh location received
   v
AVAILABLE
```

Why introduce `ACTIVATING`?

Because a driver shouldn't become matchable before we have a valid location.

Suppose:

```
Driver taps GO ONLINE
```

But GPS permission is disabled.

We know the driver's intent, but we don't know their location.

We should not publish them into the available-driver geographical index yet.

A possible process:

1. Authenticate the driver.
2. Check account and service eligibility.
3. Establish a new session.
4. Obtain a valid location.
5. Validate the location.
6. Update the live GEO index.
7. Transition business availability to `AVAILABLE`.
8. Acknowledge the online transition.

The authoritative availability update and any durable event publication should be performed consistently.

### Going offline

Now the driver taps:

```
GO OFFLINE
```

We should immediately make them ineligible for new assignments.

The exact behavior depends on whether the driver has:

- A pending offer.
- A reservation.
- An accepted ride.
- An active delivery.

For an unassigned available driver, the transition is straightforward.

```
AVAILABLE -> OFFLINE
```

We remove them from the available-driver index and invalidate relevant connection/dispatch eligibility.

For a driver already carrying an active order, going offline should not silently erase the active assignment.

That requires a separate business policy.

For example:

```
BUSY -> Cannot go offline until assignment is handled
```

Or:

```
BUSY -> Request offline after completing current delivery
```

These are business rules, not transport-level decisions.

# 8.14 Mobile applications introduce another complication: Background execution

A driver's application may not remain fully active all the time.

For example:

```
Driver opens app
       |
       v
Goes online
       |
       v
Receives a call
       |
       v
Application moves to background
       |
       v
Operating system restricts background activity
```

Now what?

The application might:

- Stop sending WebSocket heartbeats.
- Reduce location update frequency.
- Lose network connectivity.
- Be suspended by the operating system.

We must account for platform restrictions.

### Don't assume a mobile application can maintain an unrestricted background WebSocket forever

Mobile operating systems manage battery and background execution.

An application that is backgrounded might not be able to maintain continuous networking in the same way as a foreground application.

This is one reason a production mobile app needs a platform-appropriate background location strategy.

Depending on the platform and permissions, it may use:

- Background location services.
- Platform-supported foreground execution modes.
- Appropriate location permissions.
- Push notifications for incoming events.

The backend should not assume that a WebSocket connection is a permanent property of a driver.

It is temporary connectivity.

### A useful design

Use two independent mechanisms:

```
LOCATION REPORTING:
    Platform-appropriate location service

OFFER DELIVERY:
    WebSocket while actively connected
    Push notification fallback where appropriate
```

When the mobile app resumes, it reconciles its state with the backend.

For example:

```
GET /drivers/session-state
```

Response:

```
{
  "availability": "AVAILABLE",
  "activeRideId": null,
  "validOffers": [],
  "sessionEpoch": 11
}
```

The application should trust the backend's business state rather than assuming that its local memory reflects what happened while it was suspended.

# 8.15 What if the driver is connected but GPS is unavailable?

Let's look at another real-world situation.

```
D101:

WebSocket = CONNECTED
Application = ACTIVE
GPS Permission = DISABLED
```

The driver might still be able to communicate.

But we don't have a reliable current location.

Should we keep them in the matching index?

No.

The last known location becomes stale according to the freshness policy.

Our service should:

1. Keep the connection alive if appropriate.
2. Notify the application that location updates are required.
3. Mark the location stale after the freshness deadline.
4. Exclude the driver from nearby matching.
5. Allow recovery when fresh location reporting resumes.

This is better than marking the whole driver offline immediately.

Why?

Because perhaps the driver can fix GPS permissions without reconnecting.

We should separate the failure of a particular subsystem from the failure of the entire application session.

# 8.16 Heartbeat scaling — What if we have one million drivers?

Now let's calculate the heartbeat load.

Suppose:

```
Online connected drivers = 1,000,000

WebSocket heartbeat interval = 20 seconds
```

Each connection sends one application-visible heartbeat response per interval.

Approximate heartbeat responses per second:

\\[ \frac{1,000,000}{20} = 50,000 \\]

That's 50,000 responses per second.

If we also send a separate application heartbeat every 15 seconds:

\\[ \frac{1,000,000}{15} \approx 66,667 \\]

Now we potentially have over 116,000 heartbeat-related messages per second, in addition to location updates.

We should not generate redundant heartbeats without a clear purpose.

### Optimization A: Reuse existing signals

A successfully received location update already indicates application communication activity.

We may use it to update the last-application-activity timestamp.

We might not need a separate application heartbeat when location updates are arriving regularly.

Similarly, WebSocket Ping/Pong can be handled by the gateway without generating an additional database write for every pong.

### Optimization B: Avoid database writes for every heartbeat

Don't do:

```
Every heartbeat
      |
      v
UPDATE drivers SET last_heartbeat_at = NOW()
```

on the transactional database.

That would turn a simple liveness signal into a massive relational write workload.

Instead, use:

- In-memory connection state inside gateways.
- Redis leases or timestamps.
- Batched operational updates where appropriate.
- Periodic aggregation.

The durable driver profile database generally does not need every heartbeat.

### Optimization C: Stagger heartbeat intervals

Imagine one million drivers all connect simultaneously.

If every driver sends its heartbeat exactly every 20 seconds from connection time, traffic may become periodic and bursty.

We can introduce jitter.

For example:

```
Heartbeat interval:
20 seconds ± randomized offset
```

This spreads work across time.

The same technique helps with reconnects and scheduled cleanup.

# 8.17 How should we detect disconnected drivers at scale?

Let's put the connection registry and heartbeat mechanism together.

Suppose we have:

```
Gateway A:
    D101
    D102
    D103

Gateway B:
    D104
    D105
    D106

Gateway C:
    D107
    D108
    D109
```

Each gateway owns its local connections.

It maintains local connection state:

```
D101:
    connectionId = Conn-101
    sessionEpoch = 11
    lastPongAt = 10:00:05
```

When the connection is healthy, the gateway periodically refreshes its shared connection lease.

For example:

```
driver:connection:D101
```

Could contain:

```
gatewayId = Gateway-A
connectionId = Conn-101
sessionEpoch = 11
```

With a TTL.

Suppose:

```
Heartbeat/lease refresh interval = 20 seconds
Lease TTL = 60 seconds
```

Gateway A refreshes the lease periodically.

If Gateway A crashes:

```
No more lease refreshes.
```

Eventually:

```
Redis automatically expires the connection lease.
```

Now the shared registry no longer advertises D101 as connected.

### Why should the lease TTL be larger than the refresh interval?

Because a single missed refresh shouldn't immediately remove the driver.

For example:

```
Refresh every 20 seconds
TTL = 60 seconds
```

If one refresh is delayed, we have time to recover.

The ratio should be determined by network behavior, gateway load, and acceptable detection delay.

### Should we use a Redis TTL per connection?

Yes, this is a natural use case for TTL.

Each driver connection has an independent lease.

For example:

```
connection:D101 -> TTL 60 seconds
connection:D102 -> TTL 60 seconds
connection:D103 -> TTL 60 seconds
```

One connection expiring doesn't affect other drivers.

Remember our earlier discussion:

- Individual connection registry entries are good TTL candidates.
- Individual heartbeat leases are good TTL candidates.
- A shared GEO index containing thousands of drivers is not a good candidate for one collective TTL.

### Is the gateway responsible for deleting all driver state when it crashes?

No.

A gateway crash should invalidate its connection ownership, not automatically delete:

- The driver's business availability.
- Their active ride.
- Their assignment history.
- Their durable reservation.

Those states have different lifecycles.

The expired connection lease merely indicates that the previous gateway no longer owns an active connection.

# 8.18 What should happen when a driver becomes stale?

Let's design an explicit stale-driver workflow.

Suppose:

```
D101:

Availability = AVAILABLE
Location = FRESH
Connection = CONNECTED
```

The driver stops reporting GPS.

After our freshness threshold:

```
Location = STALE
```

Now what?

We should make the driver unmatchable immediately.

The operational process can be:

```
Location Updates Stop
        |
        v
Freshness Deadline Passes
        |
        v
Mark Location Stale
        |
        v
Exclude from Matching
        |
        v
Remove from Available GEO Index
        |
        v
Notify / Reconcile Operational State
```

Notice that there are two levels:

Level 1 — Logical exclusion

Matching checks the age of the latest location and refuses to select the stale driver.

This must happen even if the GEO index hasn't been cleaned.

Level 2 — Physical cleanup

The cleanup process removes the stale member from the available-driver GEO index.

This reduces unnecessary search candidates.

The order of importance is:

```
Correct matching behavior
        >
Background index cleanup
```

The cleanup process improves performance.

It must never be our only mechanism for ensuring correctness.

### What happens if the driver becomes fresh again?

Suppose the driver reconnects and sends a valid location update.

We:

1. Validate their current session.
2. Validate the location version.
3. Update the latest location.
4. Reinsert them into the GEO index.
5. Check their authoritative business availability.
6. Make them available for matching if eligible.

Do not automatically insert every fresh driver into an available-driver index.

A busy driver can also have a fresh location.

The index must respect its purpose.

# 8.19 An important choice — Should every online driver be in GEO or only available drivers?

There are two possible designs.

### Option A: GEO contains all recently reporting drivers

```
geo:online-drivers
    |
    +---- D101
    +---- D102
    +---- D103
    +---- D104
```

The matching engine searches this index and filters availability afterward.

Advantages:

- Simpler location indexing.
- Availability changes don't require immediate GEO updates.
- Easier separation of location from business state.

Disadvantages:

- Busy and reserved drivers appear in searches.
- More candidates need filtering.

### Option B: GEO contains only effectively available drivers

```
geo:available-drivers
    |
    +---- D101
    +---- D103
    +---- D108
```

When a driver becomes busy:

```
Remove from available GEO
```

When they become available:

```
Add back into available GEO
```

Advantages:

- Fewer irrelevant search results.
- Faster candidate filtering in dense areas.
- Nearby search more closely reflects supply.

Disadvantages:

- Every availability transition must update the spatial index.
- Redis can temporarily disagree with database state.
- A busy driver's location still needs to be updated somewhere.
- Reconciliation becomes more important.

### My recommendation

For a simple first implementation, maintain one index of fresh online locations and filter business availability during discovery.

As scale increases, a separate available-driver index can be introduced to reduce candidate counts.

In either design, the database remains authoritative for reservation and assignment.

# 8.20 A difficult failure — The driver's application becomes unreachable during a pending offer

Consider:

```
D101:

Availability = RESERVED_FOR_OFFER
Offer = O101
ExpiresAt = 10:00:15
```

The driver loses connectivity at 10:00:05.

What should happen?

We must distinguish:

- The connection disappeared.
- The offer is still pending.
- The reservation is still held.
- The driver might reconnect before the deadline.

There are multiple possible policies.

### Policy A: Keep the reservation until the offer expires

```
Disconnect
    |
    v
Keep offer O101 pending
    |
    v
Wait until expiry
    |
    v
Expire offer
    |
    v
Release reservation
```

This is simple and prevents another ride being offered while the original offer is technically valid.

Disadvantage: We may unnecessarily hold a driver who is definitely unreachable.

### Policy B: Cancel immediately when disconnection is confirmed

```
Connection lost
    |
    v
Cancel pending offers
    |
    v
Release reservations
    |
    v
Continue dispatch
```

This reduces waiting time.

But what if the driver experienced a very short network switch and reconnects immediately?

We may prematurely cancel an offer.

### Policy C: Short grace period

This is often a useful compromise.

```
Connection becomes disconnected
          |
          v
Mark driver temporarily unreachable
          |
          v
Wait small grace period
          |
          +---- Reconnects -> Continue offer if still valid
          |
          +---- No reconnect -> Cancel offer and release reservation
```

For example:

```
Grace period = 3 seconds
```

The value should be determined by mobile network behavior and dispatch latency requirements.

One important rule:

We never extend the original offer indefinitely just because a driver disconnected.

The offer still has its authoritative expiry deadline.

When the grace period ends, the workflow checks the actual offer state and deadline before releasing the reservation.

### How does a driver acceptance race with disconnection cancellation?

Both operations must conditionally transition the same offer state.

If acceptance commits first:

```
PENDING -> ACCEPTED
```

Disconnection cleanup cannot cancel it as a pending offer.

If cancellation commits first:

```
PENDING -> CANCELLED
```

The delayed acceptance is rejected.

Again, the database state transition resolves the race.

# 8.21 What if the driver is busy and stops sending location?

We should not confuse liveness with business assignment.

Suppose:

```
D101:

Availability = BUSY
Active ride = R501
Last location = 10:00:00
```

Then:

```
10:00:20 -> Location becomes stale
```

Should we automatically mark the driver AVAILABLE because they are no longer reporting location?

Definitely not.

The driver still owns R501 until the business workflow says otherwise.

Possible actions:

- Mark location stale.
- Alert the operational system.
- Notify the customer that live tracking is temporarily unavailable.
- Retry communication through available channels.
- Escalate if the driver remains unreachable.
- Apply business-specific cancellation or recovery procedures.

But we must not silently delete the active assignment.

This distinction is particularly important for delivery systems.

A delivery partner might lose connectivity while physically carrying a food order.

The order still exists.

The assignment still exists.

Only the latest location is unknown.

Loss of location does not imply completion, cancellation, or reassignment.

Those require separate business events.

# 8.22 Heartbeat and freshness state machine

Let's formalize the operational view.

Instead of combining everything into one status, think of it as a collection of independent state machines.

### Connection state

```
CONNECTED
    |
    | Missed heartbeat / connection close
    v
SUSPECTED
    |
    | Timeout reached
    v
DISCONNECTED
    |
    | Successful reconnect
    v
CONNECTED
```

### Location state

```
UNKNOWN
   |
   | Valid location update
   v
FRESH
   |
   | Freshness threshold exceeded
   v
STALE
   |
   | New valid location
   v
FRESH
```

### Business availability

```
OFFLINE
   |
   | Driver activates
   v
ACTIVATING
   |
   | Eligible and location available
   v
AVAILABLE
   |
   v
RESERVED_FOR_OFFER
   |
   v
BUSY
   |
   v
AVAILABLE
```

These state machines evolve independently, with constraints between them.

For example:

```
LOCATION = STALE
```

means the driver is not matchable.

It does not necessarily mean:

```
AVAILABILITY = OFFLINE
```

Likewise:

```
CONNECTION = DISCONNECTED
```

doesn't necessarily mean the driver wants to go offline.

And:

```
AVAILABILITY = AVAILABLE
```

doesn't override:

```
ASSIGNMENT = BUSY
```

This modeling approach avoids creating a huge number of combined statuses such as:

```
ONLINE_FRESH_AVAILABLE
ONLINE_STALE_AVAILABLE
OFFLINE_FRESH_BUSY
CONNECTED_FRESH_RESERVED
...
```

Those combinations would be difficult to maintain.

Keep the dimensions independent and define a clear matchability predicate.

# 8.23 Complete driver matchability calculation

We now have enough information to define exactly when a driver can participate in matching.

Suppose:

```
D101

Authenticated           = true
Online intent           = true
Connection              = DISCONNECTED
Location                = FRESH
Availability            = AVAILABLE
Active assignment       = NONE
Pending reservation     = NONE
Vehicle compatibility   = true
```

Should we match D101?

It depends on our delivery policy.

If the system requires an active WebSocket for immediate offer delivery, then no.

If we support push notification or another delivery mechanism, we might still consider D101, subject to the additional delivery constraints.

Let's define two different concepts.

### Geographically and operationally eligible

\\[ Eligible = Authenticated \land OnlineIntent \land LocationFresh \land Available \land NoActiveAssignment \land NoActiveReservation \land ServiceCompatible \\]

### Immediately dispatchable

\\[ Dispatchable = Eligible \land CommunicationAvailable \\]

Alternatively, communication availability can include an accepted fallback such as push notification.

This distinction is valuable because it separates the decision to identify a suitable driver from the decision to deliver an offer.

For example:

```
D101:
    Eligible = true
    WebSocket = disconnected
    Push fallback = available

Result:
    Eligible for fallback dispatch
```

But:

```
D102:
    Eligible = false
    WebSocket = connected

Result:
    Not eligible
```

An active connection should never override business availability or location freshness.

### A practical Java-style implementation

```
public boolean isEligibleForMatching(DriverState driver) {

    if (!driver.authenticated()) {
        return false;
    }

    if (!driver.onlineIntent()) {
        return false;
    }

    if (!driver.hasFreshLocation()) {
        return false;
    }

    if (driver.availability() != Availability.AVAILABLE) {
        return false;
    }

    if (driver.activeRideId() != null) {
        return false;
    }

    if (driver.activeReservation() != null) {
        return false;
    }

    if (!driver.supportsRequiredService()) {
        return false;
    }

    return true;
}
```

Notice that we did not require WebSocket connectivity inside this method.

That is intentional.

We evaluate the communication/delivery strategy separately.

# 8.24 Where should each liveness field be stored?

Let's make the storage responsibilities explicit.

| Information                 | Suggested storage                                 | Reason                           |
| --------------------------- | ------------------------------------------------- | -------------------------------- |
| Latest location             | Redis GEO + metadata                              | Fast location lookup             |
| Last location received time | Redis metadata / sorted set                       | Freshness and cleanup            |
| Session epoch               | Authoritative session state + Redis projection    | Reject old sessions              |
| WebSocket local connection  | Gateway memory                                    | Actual socket reference          |
| Gateway ownership           | Redis connection registry                         | Cross-server routing             |
| Gateway lease               | Redis TTL                                         | Detect dead gateways/connections |
| Application heartbeat       | Redis or operational state store                  | Fast liveness evaluation         |
| Business availability       | Transactional database                            | Correct state transitions        |
| Active assignment           | Transactional database                            | Authoritative ownership          |
| Temporary offer reservation | Transactional database, optionally Redis-assisted | Prevent conflicting assignment   |

Why does the actual WebSocket connection stay in gateway memory?

Because you cannot serialize an operating system socket into Redis and use it from another server.

Redis stores the mapping:

```
D101 -> Gateway A
```

Gateway A keeps the actual socket object locally.

Why does business availability belong in the transactional database?

Because it participates in assignment correctness.

We need to guarantee that two dispatchers cannot reserve the same driver for conflicting work.

Why does the latest coordinate belong in Redis?

Because it is extremely high-frequency data that benefits from in-memory geographical indexing.

This is how we avoid forcing all different workloads into one datastore.

# 8.25 Full liveness architecture

Let's combine everything.

\#chatgpt-mermaid-\_r_6ai\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_6ai\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6ai\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_6ai\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_6ai\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_6ai\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_6ai\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_6ai\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_6ai\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_6ai\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6ai\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6ai\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_6ai\_ p{margin:0;}#chatgpt-mermaid-\_r_6ai\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_6ai\_ .label text,#chatgpt-mermaid-\_r_6ai\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .node rect,#chatgpt-mermaid-\_r_6ai\_ .node circle,#chatgpt-mermaid-\_r_6ai\_ .node ellipse,#chatgpt-mermaid-\_r_6ai\_ .node polygon,#chatgpt-mermaid-\_r_6ai\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .rough-node .label text,#chatgpt-mermaid-\_r_6ai\_ .node .label text,#chatgpt-mermaid-\_r_6ai\_ .image-shape .label,#chatgpt-mermaid-\_r_6ai\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_6ai\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .rough-node .label,#chatgpt-mermaid-\_r_6ai\_ .node .label,#chatgpt-mermaid-\_r_6ai\_ .image-shape .label,#chatgpt-mermaid-\_r_6ai\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_6ai\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_6ai\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6ai\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6ai\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_6ai\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6ai\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6ai\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6ai\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_6ai\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_6ai\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_6ai\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_6ai\_ .icon-shape,#chatgpt-mermaid-\_r_6ai\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_6ai\_ .icon-shape p,#chatgpt-mermaid-\_r_6ai\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_6ai\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_6ai\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_6ai\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_6ai\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_6ai\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_6ai\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_6ai\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_6ai\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_6ai\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6ai\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_6ai\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_6ai\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6ai\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_6ai\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_6ai\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_6ai\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .node rect,#chatgpt-mermaid-\_r_6ai\_ .node circle,#chatgpt-mermaid-\_r_6ai\_ .node ellipse,#chatgpt-mermaid-\_r_6ai\_ .node polygon,#chatgpt-mermaid-\_r_6ai\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_6ai\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_6ai\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_6ai\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_6ai\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_6ai\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Driver Mobile AppLocation UpdatesApplication HeartbeatWebSocket ConnectionLocation IngestionHeartbeat IngestionRedis Location andFreshness IndexWebSocket GatewayConnection Registry withLeaseOperational Liveness StateGateway Heartbeat MonitorStale Driver Cleanup WorkerMatching EngineTransactional Driver StateAssignment Service

Let's walk through it.

1\. Driver starts working.

The application authenticates and starts a session.

2\. Driver reports location.

Location ingestion updates Redis.

3\. Application heartbeat arrives.

The server refreshes application liveness information.

4\. WebSocket connection is established.

The owning gateway maintains the local socket and publishes its connection lease.

5\. Gateway monitors connection health.

It periodically checks Ping/Pong responses.

6\. Location freshness is evaluated independently.

If location updates stop, the driver becomes unmatchable after the freshness deadline.

7\. Stale-driver cleanup removes outdated spatial entries.

It uses version-aware atomic deletion.

8\. Matching checks all relevant dimensions.

It never assumes that a fresh location automatically means an available driver.

9\. Assignment service makes the final decision.

The durable driver state determines whether the driver can be reserved and assigned.

# 8.26 Different heartbeat strategies for different products

Let's adapt this design to different businesses.

| Scenario                         | Location reporting                    | Connection heartbeat     | Liveness policy                                 |
| -------------------------------- | ------------------------------------- | ------------------------ | ----------------------------------------------- |
| Ride-hailing, driver available   | Periodic                              | Periodic if connected    | Freshness-based matching                        |
| Ride-hailing, active trip        | More frequent                         | Periodic                 | Stronger tracking expectations                  |
| Food delivery, waiting for order | Adaptive                              | Periodic                 | Fresh location before assignment                |
| Food delivery, carrying food     | Frequent enough for customer tracking | Periodic where connected | Stale location triggers operational alert       |
| Logistics fleet                  | Configurable tracking interval        | Device/network-specific  | Longer and more configurable offline thresholds |
| Home-service technician          | Periodic or event-based               | Optional                 | Appointment and service-area eligibility        |

We should not reuse one universal heartbeat and freshness configuration for every product.

The business determines how quickly we need to notice that a location is no longer trustworthy.

For example:

- A delivery partner carrying hot food might require aggressive location freshness monitoring.
- A long-haul truck might report every 30 seconds or several minutes, depending on the product.
- A technician waiting for an appointment may not need second-by-second location updates.

# 8.27 What happens during a massive reconnect event?

One final real-world failure scenario.

Imagine a mobile network outage affected 100,000 drivers.

At 10:00:

```
100,000 drivers lose connectivity.
```

They reconnect when the network recovers.

If every client immediately reconnects and sends all buffered updates, we create a huge traffic spike.

This is called a thundering herd.

Our recovery strategy should include:

1\. Exponential backoff with jitter

Don't let every mobile application reconnect at exactly the same time.

2\. Latest-value recovery

For live matching, upload the latest location rather than every obsolete buffered location.

3\. Bounded connection admission

Gateways should limit connection establishment rates when overloaded.

4\. Gradual index recovery

Don't immediately expose every reconnecting driver to matching until their current session and location have been validated.

5\. Partition isolation

A recovery burst in one region should not overwhelm unrelated regions.

6\. Liveness-aware matching

Drivers should become eligible individually as their fresh locations arrive.

We don't need to wait for all 100,000 drivers to reconnect.

The system can gradually restore supply.

# 8.28 Chapter 8 — Final mental model

The most important thing is to stop thinking of driver liveness as a single boolean.

Instead:

```
                 DRIVER D101
                      |
          +-----------+-----------+
          |           |           |
          v           v           v
      Connection    Location    Business
        State       Freshness  Availability
          |           |           |
          v           v           v
      CONNECTED      FRESH     AVAILABLE
                      |           |
                      +-----+-----+
                            |
                            v
                     MATCHABILITY
                            |
                            v
                     ELIGIBLE DRIVER
```

And keep assignment as a separate authoritative concern.

## Important concepts to remember

| Concept                | What problem does it solve?                           |
| ---------------------- | ----------------------------------------------------- |
| WebSocket Ping/Pong    | Detects unresponsive connections                      |
| Application heartbeat  | Detects application-level communication               |
| Location heartbeat     | Confirms recent location reporting                    |
| Freshness threshold    | Prevents matching with old locations                  |
| Redis TTL              | Automatically expires temporary leases                |
| Redis sorted set       | Efficiently identifies stale drivers                  |
| Session epoch          | Rejects messages from old sessions                    |
| Connection registry    | Routes messages to the correct gateway                |
| Grace period           | Avoids unnecessarily reacting to brief disconnections |
| Explicit online intent | Represents what the driver wants                      |
| Effective matchability | Determines whether the driver can receive work        |

### My recommended production approach

For the initial ride-hailing implementation:

- Use WebSocket Ping/Pong for connection health.
- Use location-update timestamps for location freshness.
- Reuse location updates as application activity signals where appropriate.
- Maintain gateway connection leases in Redis.
- Maintain business availability in the transactional database.
- Use a freshness threshold to exclude stale drivers immediately.
- Clean stale drivers from the GEO index asynchronously.
- Use session epochs to protect against old connections.
- Never let a reconnect automatically clear an active assignment.

The most important principle: A driver being online, connected, reporting location, available for work, and free from assignments are five different facts. Never collapse them into one `isOnline` boolean.

# Next: Chapter 9 — Failure Handling, Retries, Duplicates, Durability and Recovery

We now have all the major components:

```
Location Ingestion
Redis GEO
Spatial Indexing
Nearby Discovery
Matching Engine
Dispatch Orchestrator
WebSocket Gateways
Driver Reservations
Transactional Assignment
Heartbeats
Liveness Detection
```

But production systems don't operate under perfect conditions.

Let's intentionally break our architecture.

For example:

```
1. Driver sends a location update.
2. Redis successfully stores it.
3. Ingestion server crashes before acknowledging.
4. Driver retries the same update.
```

Or:

```
1. Offer is created.
2. Outbox publisher publishes it.
3. Publisher crashes before marking it published.
4. Same offer event is published again.
```

Or:

```
1. Driver accepts.
2. Database commits assignment.
3. Assignment service crashes.
4. Driver retries acceptance.
```

Or:

```
1. Redis is completely unavailable.
2. Kafka is healthy.
3. Matching service is healthy.
4. Drivers are still connected.
```

We need to reason about each failure independently.

### Chapters and concepts

| Topic                           | What we'll solve                                                           |
| ------------------------------- | -------------------------------------------------------------------------- |
| 9.1 Failure classification      | Understand transient, permanent, partial, and cascading failures           |
| 9.2 Retries                     | Exponential backoff, jitter, retry budgets, and retry storms               |
| 9.3 Idempotency                 | Make duplicate location updates, offers, acceptances, and events safe      |
| 9.4 Message delivery guarantees | At-most-once, at-least-once, and practical exactly-once business effects   |
| 9.5 Queue vs Kafka revisited    | When to use durable queues, event streams, or direct calls                 |
| 9.6 Outbox and inbox patterns   | Avoid dual-write problems and duplicate event processing                   |
| 9.7 Ordering                    | Handle old location events, duplicated messages, and out-of-order commands |
| 9.8 Durability                  | Decide what must survive crashes and what can be reconstructed             |
| 9.9 Redis recovery              | Rebuild live indexes after data loss                                       |
| 9.10 Dispatch recovery          | Resume pending offers after service crashes                                |
| 9.11 Reconciliation             | Detect and repair inconsistent operational state                           |
| 9.12 Backpressure               | Protect the system during overload                                         |
| 9.13 Disaster scenarios         | Redis outage, database outage, Kafka outage, and regional failure          |

We'll also build a complete failure matrix covering every major component.

The central question:

When a component fails, how does the system recover without losing business correctness, assigning a ride twice, or overwhelming the remaining healthy components?

Say next to continue.
