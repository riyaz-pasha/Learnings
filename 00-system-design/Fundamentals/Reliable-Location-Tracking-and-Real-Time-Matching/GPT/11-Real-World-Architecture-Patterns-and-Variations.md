# Chapter 11 — Real-World Architecture Patterns and Business Variations

We've designed a generic location tracking and matching platform.

It can:

- Track millions of moving drivers.
- Find nearby candidates.
- Filter unavailable or stale drivers.
- Calculate ETA.
- Send offers in real time.
- Handle concurrent acceptance.
- Guarantee one successful assignment.
- Recover from failures.
- Scale across multiple geographical regions.

Now let's understand something important:

Uber, Ola, Rapido, Swiggy, Zomato, logistics platforms, and home-service platforms can use the same underlying location infrastructure, but they should not necessarily use the same matching and dispatch algorithms.

Why?

Because their business objectives are different.

For example:

```
Uber-style:
    Get a suitable driver to the pickup location quickly.

Food delivery:
    Get an appropriate delivery partner to the restaurant
    at approximately the right time.

Logistics:
    Assign a suitable vehicle that can fulfil the shipment
    within its operational constraints.

Home services:
    Find a qualified technician who can reach the customer
    during the requested appointment window.
```

If we use the same simplistic algorithm everywhere:

```
Find nearest available driver -> Assign
```

We'll make poor decisions.

Let's build each pattern independently.

One important clarification: These are representative industry architecture patterns, not claims about the proprietary internal implementations of any named company.

# 11.1 Pattern A — Ride-hailing systems (Uber/Ola/Rapido-style)

Let's start with the simplest business.

A customer requests a ride.

```
Customer:
    Pickup = P
    Destination = D
    Vehicle = Bike
```

Our system needs to find a suitable driver.

The primary objective is:

Minimize the time required to successfully pick up the customer while maintaining assignment correctness and acceptable driver utilization.

## What factors matter?

| Factor                 | Why it matters                                       |
| ---------------------- | ---------------------------------------------------- |
| Pickup ETA             | How quickly can the driver reach the customer?       |
| Vehicle type           | Can the driver fulfil the requested ride?            |
| Location freshness     | Is the driver's reported position trustworthy?       |
| Availability           | Can the driver accept a ride?                        |
| Acceptance probability | How likely is the driver to accept?                  |
| Reliability            | How likely is the driver to complete the assignment? |
| Traffic                | Does the road network make the route slow?           |

## Complete flow

```
Customer Requests Ride
         |
         v
Persist Ride Request
         |
         v
Find Nearby Drivers
         |
         v
Filter Eligible Drivers
         |
         v
Calculate ETA
         |
         v
Rank Candidates
         |
         v
Dispatch Small Batch
         |
         v
Driver Accepts
         |
         v
Atomic Assignment
         |
         v
Notify Customer and Driver
```

### Which dispatch policy should we use?

Suppose:

```
D101 -> ETA 2 minutes
D102 -> ETA 3 minutes
D103 -> ETA 4 minutes
D104 -> ETA 5 minutes
```

We have several options.

Sequential:

Contact D101, then D102 if needed.

Parallel:

Contact D101, D102, and D103 together.

Staggered:

Contact D101 first, then D102 shortly afterward if needed.

Small batches:

Contact D101 and D102 together, followed by D103 and D104 if needed.

A production policy could choose among these based on:

- Expected acceptance latency.
- Driver response behavior.
- Driver experience.
- Customer waiting-time requirements.
- Number of available candidates.

### Should we always choose the nearest driver?

No.

Consider:

| Driver | Straight distance | Pickup ETA |
| ------ | ----------------- | ---------- |
| D101   | 500 m             | 12 min     |
| D102   | 1 km              | 3 min      |
| D103   | 1.5 km            | 4 min      |

D102 is probably preferable.

We may also consider the probability of successful pickup.

For example:

```
D101:
    ETA = 3 minutes
    Acceptance probability = 20%

D102:
    ETA = 4 minutes
    Acceptance probability = 90%
```

A platform may prefer D102 because it has a much better chance of successfully securing the ride.

This becomes a ranking/optimization problem.

We'll formalize it later in this chapter.

### What location durability do we need?

For immediate matching:

```
Latest location -> Redis
```

For active-trip tracking:

```
Selected location events -> Historical event pipeline
```

For assignment:

```
Ride assignment -> Transactional database
```

These responsibilities remain separate.

# 11.2 Pattern B — Food delivery systems (Swiggy/Zomato-style)

Now let's make things more interesting.

In ride-hailing, the customer is generally ready to be picked up.

In food delivery, the restaurant must prepare the order.

This creates a new dimension:

We need to coordinate delivery-partner arrival with order preparation.

Consider:

```
Customer places an order.

Restaurant:
    Takes 20 minutes to prepare food.

Delivery partner:
    Takes 8 minutes to reach the restaurant.
```

Should we dispatch immediately?

Possibly, but it might cause the delivery partner to wait at the restaurant for 12 minutes.

That is undesirable.

We might want the partner to arrive near the time the food is ready.

This changes our matching algorithm.

## 11.2.1 Introduce preparation time

Our order now has:

```
Order O501
-------------------------
Restaurant = R101
Customer = C101

Preparation Time = 20 min

Pickup Location = Restaurant
Delivery Location = Customer
```

We also have:

```
Partner D101:
    ETA to restaurant = 8 min

Partner D102:
    ETA to restaurant = 18 min

Partner D103:
    ETA to restaurant = 12 min
```

Which partner should we choose?

A naive algorithm picks D101 because they are nearest.

But let's consider preparation time.

```
Restaurant ready in 20 min.

D101 arrives in 8 min.
D102 arrives in 18 min.
D103 arrives in 12 min.
```

Waiting times:

| Partner | Arrival ETA | Expected food-ready time | Partner waiting time |
| ------- | ----------- | ------------------------ | -------------------- |
| D101    | 8 min       | 20 min                   | 12 min               |
| D102    | 18 min      | 20 min                   | 2 min                |
| D103    | 12 min      | 20 min                   | 8 min                |

D102 might be the better choice.

However, we cannot simply ignore ETA and choose any partner arriving near preparation completion.

The customer still wants the food delivered quickly.

We need to balance several constraints:

- Restaurant preparation time.
- Partner arrival time.
- Partner availability.
- Customer delivery time.
- Food quality.
- Delivery deadline.

## 11.2.2 Estimate the pickup-ready time

We can define:

\\[ T\_{ready} = T\_{order} + T\_{prep} \\]

Where:

- \\(T\_{order}\\) is the order creation time.
- \\(T\_{prep}\\) is the estimated preparation duration.

For a driver:

\\[ T\_{arrival} = T\_{now} + ETA\_{restaurant} \\]

The expected waiting time is:

\\[ Waiting = \max(0,T\_{ready}-T\_{arrival}) \\]

The driver might be dispatched immediately, scheduled for later, or selected based on the expected arrival window.

### But preparation time isn't perfectly predictable

Suppose the restaurant initially says:

```
Preparation time = 20 minutes
```

But after 10 minutes:

```
Updated preparation time = 30 minutes total
```

Our original partner selection might no longer be optimal.

We may need to:

- Recalculate ETA.
- Delay offer creation.
- Keep a partner in a temporary reservation.
- Reassign the work if the original offer hasn't been accepted.
- Adjust the expected pickup time.

This introduces dynamic matching.

Our matching engine must respond to changes in both driver location and order readiness.

# 11.3 Multi-order delivery batching — Assign multiple orders to one partner

This is one of the biggest differences between ride-hailing and food delivery.

A delivery partner might carry multiple orders simultaneously.

For example:

```
Order A:
Restaurant A -> Customer A

Order B:
Restaurant B -> Customer B
```

Suppose one partner can serve both without excessive detour.

We might assign both to the same delivery partner.

This increases delivery efficiency and can improve fleet utilization.

But now matching becomes a constrained route-optimization problem.

## 11.3.1 A simple example

Imagine:

```
                    Customer A
                        ^
                        |
                  Restaurant A
                        |
                        |
Partner D101 ----------+
                        |
                  Restaurant B
                        |
                        v
                    Customer B
```

Assume:

```
D101:
    Available capacity = 2 orders
```

We have two orders.

Should we assign both to D101?

Potentially.

But we need to compare routes.

### Route option 1: Deliver A completely, then B

```
D101
  |
  v
Restaurant A
  |
  v
Customer A
  |
  v
Restaurant B
  |
  v
Customer B
```

### Route option 2: Pick up both, then deliver A and B

```
D101
  |
  v
Restaurant A
  |
  v
Restaurant B
  |
  +----> Customer A
  |
  +----> Customer B
```

Which route is better?

It depends on:

- Restaurant locations.
- Customer locations.
- Food preparation times.
- Pickup windows.
- Delivery deadlines.
- Vehicle capacity.
- Maximum acceptable detour.
- Food quality constraints.

We need to calculate a suitable route.

## 11.3.2 Introduce route compatibility

Suppose D101 already has Order A.

A new Order B arrives.

We can estimate:

```
Current route:
Restaurant A -> Customer A

New route:
Restaurant A -> Restaurant B
             -> Customer B
             -> Customer A
```

We compare:

\\[ ExtraDetour = NewRouteETA - ExistingRouteETA \\]

And:

\\[ MaxDeliveryDelay = NewDeliveryTime - OriginalExpectedDeliveryTime \\]

If the additional delay is acceptable, batching might be possible.

Example:

| Metric                 | Value  |
| ---------------------- | ------ |
| Existing delivery ETA  | 12 min |
| ETA with new order     | 16 min |
| Extra detour           | 4 min  |
| Maximum allowed detour | 5 min  |

The new order could be compatible.

But consider:

```
Existing ETA = 12 min
New ETA = 25 min
Extra detour = 13 min
Allowed detour = 5 min
```

We should not batch these orders.

### The matching algorithm changes

Instead of:

```
Find nearest available delivery partner.
```

We now do:

```
Find nearby delivery partners
           |
           v
Check remaining capacity
           |
           v
Check current assigned orders
           |
           v
Evaluate route compatibility
           |
           v
Calculate additional delivery delay
           |
           v
Rank possible assignments
           |
           v
Reserve partner and assign order
```

Notice that a partner with one active order is not automatically unavailable.

They might have capacity for another compatible order.

This is why our earlier generic availability model needs to evolve.

## 11.3.3 The important algorithm — Insertion heuristic

We don't need to solve the entire route optimization problem from scratch every time a new order arrives.

A practical approach is to insert a new pickup and delivery into an existing route.

Suppose our partner has:

```
Current route:

1. Restaurant A
2. Customer A
```

A new order arrives:

```
Restaurant B
Customer B
```

We evaluate possible insertion positions.

For example:

```
Option 1:
Restaurant B
Restaurant A
Customer A
Customer B

Option 2:
Restaurant A
Restaurant B
Customer A
Customer B

Option 3:
Restaurant A
Customer A
Restaurant B
Customer B

Option 4:
Restaurant A
Restaurant B
Customer B
Customer A
```

We calculate the impact of each route.

We reject routes violating constraints:

- Pickup must happen before the corresponding delivery.
- Vehicle capacity must not be exceeded.
- Pickup and delivery time windows must be satisfied.
- Maximum delivery delay must be respected.
- Food or product handling constraints must be satisfied.

Among the feasible routes, we select one with the lowest incremental cost.

For example:

\\[ IncrementalCost = NewRouteCost - ExistingRouteCost \\]

The cost can include:

- Additional travel time.
- Additional distance.
- Delivery delay.
- Fuel consumption.
- Food-quality penalties.

We choose the feasible insertion with the lowest cost.

This is called an insertion heuristic.

It is useful because solving the globally optimal pickup-and-delivery route problem can become computationally expensive.

For a very large fleet, we generally prefer practical heuristics that produce good results quickly over attempting an exact global optimum for every incoming order.

## 11.3.4 Another problem — The partner might accept another order while we're calculating

Suppose D101 has capacity for two orders.

Two new orders arrive simultaneously.

```
Order A -> Finds D101 has one available slot.
Order B -> Also finds D101 has one available slot.
```

Both matching operations might conclude that D101 has capacity.

Now we could accidentally assign three orders to a partner whose capacity is two.

This is the same concurrency problem we solved in Chapter 7, but the invariant changes.

For ride-hailing:

```
Active assignments <= 1
```

For delivery batching:

```
Assigned workload <= Partner capacity
```

We need an atomic capacity reservation or route-plan version check.

For example:

```
D101:
    capacity = 2
    assignedOrders = 1
    reservedSlots = 0
```

Order A tries to reserve one slot.

Order B tries simultaneously.

Only one should succeed.

Conceptually:

```
UPDATE delivery_partners
SET reserved_slots = reserved_slots + 1
WHERE driver_id = 'D101'
  AND assigned_orders + reserved_slots < capacity;
```

Check the affected row count.

Only one request should successfully claim the remaining capacity.

For more complex batching, we'd also need to reserve against a particular route-plan version.

For example:

```
Partner D101:

Route version = 105
```

A matching operation evaluates version 105.

Before committing the new task, it checks that the route version hasn't changed.

If another operation has already modified the route to version 106, the first operation must recompute or retry against the new route.

This prevents two concurrent matching operations from overwriting each other's route plans.

### Important difference from ride-hailing

We shouldn't blindly reuse the Chapter 7 constraint:

```
One driver -> One active assignment
```

That is appropriate for a ride-hailing model where a driver cannot accept another ride while carrying a passenger.

It is not appropriate for a delivery model supporting multiple orders.

The actual invariant must reflect the business:

```
Ride hailing:
    One active ride per driver.

Food delivery:
    Limited number of compatible active delivery tasks.

Logistics:
    Capacity, route, time-window and vehicle constraints.
```

The assignment model is business-dependent, even though the concurrency principles remain the same.

# 11.4 Pattern C — Delivery partner matching with deferred dispatch

Let's introduce another useful food-delivery optimization.

Suppose:

```
Order created at 10:00.

Restaurant preparation time = 20 minutes.

Expected readiness = 10:20.
```

A partner is currently:

```
8 minutes from restaurant.
```

If we dispatch immediately, they'll arrive at 10:08 and wait.

What if we wait before dispatching?

We could schedule matching to begin closer to 10:12.

Now the partner may arrive at 10:20.

This is called deferred dispatch or dispatch timing optimization.

### Basic flow

```
Order Created
     |
     v
Estimate Preparation Time
     |
     v
Determine Expected Pickup-Ready Time
     |
     v
Estimate Partner Arrival Times
     |
     v
Choose Dispatch Time
     |
     v
Dispatch Partner
     |
     v
Partner Arrives Near Food Readiness
```

But we shouldn't always wait.

Suppose:

```
Preparation time = 20 minutes
Partner availability = Very low
```

If we delay dispatch, there might be no suitable partner later.

Or the selected partner might become unavailable.

Therefore, dispatch timing should consider:

- Preparation-time uncertainty.
- Local driver supply.
- Expected acceptance time.
- Restaurant readiness reliability.
- Pickup deadlines.
- Partner scarcity.

### A useful model

Let:

```
T_ready = Estimated food readiness time
T_offer = Time offer is sent
T_accept = Expected time until driver acceptance
T_arrival = ETA from partner to restaurant
```

We want approximately:

\\[ T\_{offer}+T\_{accept}+T\_{arrival} \approx T\_{ready} \\]

But we must account for uncertainty.

For example, a restaurant's actual preparation time might be:

```
Expected: 20 minutes
Actual:   15-28 minutes
```

A system can use historical preparation-time distributions rather than relying only on one estimate.

This becomes a predictive optimization problem.

### A practical dispatch policy

```
High partner supply:
    Defer dispatch closer to readiness.

Low partner supply:
    Dispatch earlier to secure a partner.

High preparation uncertainty:
    Use periodic readiness reassessment.

Urgent delivery:
    Prefer a policy that minimizes total customer delay.
```

This is a significant difference from immediate ride-hailing dispatch.

# 11.5 Pattern D — Logistics and fleet management

Now consider a logistics system.

A customer wants to ship goods from:

```
Source:
    Warehouse A

Destination:
    Warehouse B
```

We have:

```
Truck D101:
    Location = 3 km away
    Capacity = 5 tons

Truck D102:
    Location = 7 km away
    Capacity = 20 tons

Truck D103:
    Location = 2 km away
    Capacity = 2 tons
```

Shipment weight:

```
8 tons
```

Which truck should we select?

D103 is geographically closest.

But it cannot carry the shipment.

D101 is also unsuitable.

D102 is the only suitable option.

This is a good example of why proximity alone is insufficient.

## 11.5.1 Introduce hard constraints

Some requirements must be satisfied before ranking.

For example:

```
Vehicle capacity >= Shipment weight

Vehicle type supports cargo type

Driver is available

Truck can access required roads

Vehicle is operational

Driver can meet delivery deadline
```

These are hard constraints.

We should eliminate unsuitable vehicles before ranking.

## 11.5.2 Introduce soft optimization objectives

Among the feasible trucks, we can optimize:

- Pickup time.
- Total route distance.
- Fuel cost.
- Estimated delivery time.
- Driver working hours.
- Fleet utilization.
- Empty return distance.

For example:

```
Truck A:
    Pickup ETA = 10 min
    Delivery cost = ₹5,000
    Expected delivery = 4 hours

Truck B:
    Pickup ETA = 15 min
    Delivery cost = ₹3,500
    Expected delivery = 5 hours
```

Which is better?

Depends on the shipment's deadline and cost priorities.

For urgent shipments, A might be preferable.

For flexible shipments, B may be more economical.

We can model:

\\[ Score = w_1(DeliveryTime) + w_2(Cost) + w_3(EmptyDistance) + w_4(OperationalRisk) \\]

We first eliminate infeasible candidates.

Then we rank the remaining ones.

### How is location tracking different?

A truck might report location every 30 seconds or every few minutes, depending on operational requirements.

The system may need to retain a much longer historical trajectory.

So compared with ride-hailing, logistics may have stronger requirements for:

- Durable location history.
- Audit trails.
- Geofence events.
- Long-distance route reconstruction.
- Delivery proof.
- Regulatory reporting.

The same Redis/Kafka/database separation still applies, but the durability policy changes.

# 11.6 Pattern E — Fleet tracking with geofencing

Let's look at another related use case.

A fleet operator wants to know when trucks enter or leave specific regions.

For example:

```
Warehouse A
    Geofence radius = 500 metres

Truck D101 enters the area.
```

The system should generate:

```
TRUCK_ENTERED_GEOFENCE
```

Later:

```
Truck D101 leaves.
```

Generate:

```
TRUCK_EXITED_GEOFENCE
```

This is different from nearby-driver matching.

We're not looking for the nearest available driver.

We're detecting geographical transitions.

### How does it work?

For a circular geofence:

```
Warehouse:
    Center = C
    Radius = 500 m
```

For every valid location update:

1. Identify relevant geofences.
2. Calculate the truck's distance from each relevant geofence.
3. Determine whether it is inside or outside.
4. Compare with the previous state.
5. Emit an event only if a transition occurred.

Conceptually:

```
Previously: OUTSIDE

New location: INSIDE

Emit:
TRUCK_ENTERED_GEOFENCE
```

### What about GPS noise?

Imagine a truck is near the boundary.

GPS coordinates fluctuate slightly.

We might repeatedly observe:

```
INSIDE
OUTSIDE
INSIDE
OUTSIDE
```

This causes event flapping.

A common solution is hysteresis.

For example:

```
Enter geofence:
    Distance <= 450 m

Exit geofence:
    Distance >= 550 m
```

Between 450 m and 550 m, retain the previous state.

This creates a dead band.

It reduces false transitions caused by GPS noise.

We can also require multiple consecutive observations or a minimum time inside the region.

### How does this relate to our core location system?

We can reuse:

- Location ingestion.
- Session/version validation.
- Geographical indexes.
- Durable event processing.
- Reconciliation.

But the query and business logic are different.

# 11.7 Pattern F — Home-service matching

Imagine an application that connects customers with:

- Plumbers.
- Electricians.
- Technicians.
- Cleaning professionals.
- Appliance repair specialists.

A customer requests:

```
Service:
    AC Repair

Location:
    Customer Address

Preferred time:
    4 PM - 6 PM
```

We have three technicians:

```
T101:
    Distance = 1 km
    Skill = Plumbing

T102:
    Distance = 3 km
    Skill = AC Repair

T103:
    Distance = 2 km
    Skill = AC Repair
```

Who should we select?

T103 is suitable.

T101 is the nearest, but cannot perform the requested service.

### What changes compared with ride-hailing?

The matching process needs a skill and scheduling engine.

For example:

```
Customer Request
       |
       v
Find Nearby Technicians
       |
       v
Filter by Required Skills
       |
       v
Check Certifications
       |
       v
Check Availability Slots
       |
       v
Check Appointment Compatibility
       |
       v
Calculate Travel ETA
       |
       v
Rank Technicians
       |
       v
Dispatch
```

### Appointment matching introduces time-window constraints

Suppose:

```
Customer appointment:
    4 PM - 6 PM

Technician T103:
    Current task ends at 4:30 PM
    Travel ETA = 20 minutes
```

The technician may be suitable even though they are currently busy.

Why?

Because their existing task finishes before the appointment window.

We need to account for:

- Existing scheduled tasks.
- Expected completion time.
- Travel time.
- Skill requirements.
- Appointment duration.
- Time-window overlap.

This is a scheduling and spatial matching problem.

A technician shouldn't simply be marked `BUSY` forever until their current task completes.

We need a future availability calendar.

For example:

```
T103:

12:00 - 13:00 -> Existing appointment
13:00 - 14:00 -> Travel / buffer
14:00 - 15:00 -> Available
16:00 - 18:00 -> Requested appointment
```

The system may reserve a future slot rather than an immediately available driver.

That requires transactional slot reservation, preventing overlapping bookings.

This is another variation of our assignment correctness model.

### Important difference

For ride-hailing:

```
Reserve current driver availability.
```

For scheduled services:

```
Reserve a future time slot.
```

Both need atomic reservation, but the state model is different.

# 11.8 Pattern G — Driver repositioning and demand prediction

So far, our matching system reacts to customer requests.

But mature platforms may also optimize where drivers should position themselves.

Imagine:

```
Area A:
    100 available drivers
    10 expected ride requests

Area B:
    5 available drivers
    100 expected ride requests
```

What happens if we do nothing?

Customers in Area B may wait longer, even though the city has plenty of drivers overall.

We need to balance geographic supply and expected demand.

This leads to driver repositioning.

## 11.8.1 Calculate regional supply and demand

For each geographical cell:

```
Cell A:
    Available drivers = 100
    Expected requests = 10

Cell B:
    Available drivers = 5
    Expected requests = 100

Cell C:
    Available drivers = 30
    Expected requests = 25
```

We can estimate supply-demand pressure.

A simple ratio:

\\[ Pressure = \frac{ExpectedDemand}{AvailableSupply} \\]

For example:

```
Cell A:
    10 / 100 = 0.1

Cell B:
    100 / 5 = 20

Cell C:
    25 / 30 = 0.83
```

Cell B has much higher demand pressure.

We could recommend that some drivers move toward B.

### But don't reposition every driver!

Moving drivers has a cost.

The system should consider:

- Distance to reposition.
- Fuel or battery consumption.
- Expected demand.
- Probability that demand materializes.
- Driver willingness.
- Traffic.
- Nearby competing supply.
- Platform incentives.

We might generate suggestions:

```
D101:
    Current location = Cell A
    Suggested area = Cell B
    Expected travel time = 8 minutes
    Predicted demand = High
```

This can be useful for driver experience and platform supply balancing.

### This is not ordinary dispatch

A customer request is not required.

We're optimizing supply distribution proactively.

The system can run:

- Periodically.
- When demand predictions change.
- When a geographical cell becomes severely undersupplied.
- When a major event ends.

### A more useful model

We can predict:

\\[ ExpectedDemand(cell, timeWindow) \\]

And:

\\[ ExpectedSupply(cell, timeWindow) \\]

Then optimize driver positioning.

This is a forecasting and operations-research problem, not simply a nearest-driver lookup.

# 11.9 Pattern H — Supply-demand surge handling

Imagine a stadium event ends.

Thousands of customers simultaneously request rides.

The platform has:

```
Available drivers = 500

Expected requests = 5,000
```

We cannot create drivers out of nowhere.

The system should recognize this as a supply shortage.

Possible strategies:

1. Expand geographical search radius.
2. Prioritize requests based on waiting time or business rules.
3. Increase supply through driver repositioning.
4. Offer incentives for drivers to move toward the area.
5. Adjust estimated pickup times.
6. Avoid repeatedly sending the same request to the same unavailable drivers.
7. Introduce admission control or demand shaping where necessary.

### Avoid excessive overlapping offers

Suppose ten customers all search for the same 50 drivers.

Without reservations, all ten requests might send offers to many of the same drivers.

We create:

- Excessive notifications.
- High conflict rates.
- Many failed acceptance attempts.

Our temporary reservation mechanism reduces this.

But it also introduces competition for the limited driver supply.

A fair dispatch strategy should consider how long each customer has waited and which drivers are already reserved for other customers.

### A useful fairness policy

Consider:

```
Customer A:
    Waiting 2 minutes

Customer B:
    Waiting 10 seconds
```

Both request rides from the same area.

A simplistic nearest-driver policy might prioritize B because of a slightly better match.

But the platform may want to account for A's longer waiting time.

Possible policies:

- Older request first.
- Maximum acceptable customer wait.
- Priority classes.
- Weighted fairness.
- Business-specific service-level guarantees.

We should separate:

Which driver is suitable?

from:

Which customer request should receive scarce supply first?

This is a marketplace allocation problem.

# 11.10 Pattern I — Better driver ranking: From nearest driver to best expected outcome

Let's now unify the ranking strategies.

Suppose our matching engine identifies:

```
D101
D102
D103
D104
```

We have several factors:

```
Pickup ETA
Acceptance probability
Driver reliability
Location accuracy
Driver workload
Customer waiting time
```

How do we combine them?

One approach is a weighted scoring function.

For illustration:

\\[ Score = w_1 \times ETA + w_2 \times ExpectedWait + w_3 \times Risk - w_4 \times AcceptanceProbability \\]

Here:

- Lower ETA is better.
- Lower expected wait is better.
- Lower risk is better.
- Higher acceptance probability is better.

The weights depend on the business objective.

But this needs care.

## Don't mix incomparable values directly

For example:

```
ETA = 4 minutes

Acceptance probability = 0.9

Reliability = 95%
```

These have different scales.

We need to normalize features or use a model that handles their distributions appropriately.

A simpler initial approach:

```
Stage 1:
    Hard eligibility filtering.

Stage 2:
    ETA ranking.

Stage 3:
    Tie-break using acceptance probability and reliability.
```

This is easier to explain, debug, and operate.

## Optimize successful assignment time, not only ETA

Imagine:

```
D101:
    ETA = 2 minutes
    Acceptance probability = 20%

D102:
    ETA = 3 minutes
    Acceptance probability = 90%
```

D101 is closer, but D102 is more likely to accept.

We can think about expected assignment latency.

A simplified model:

\\[ ExpectedAssignmentTime = ETA\_{response} + ExpectedPickupTime \\]

But acceptance is probabilistic.

A richer model may estimate:

\\[ P(\text{successful pickup} \mid driver, request) \\]

And:

\\[ ExpectedTotalWait \\]

We might select D102 because its probability of a successful assignment is much higher.

This is one reason real matching algorithms can become sophisticated.

However, always maintain an explainable fallback policy.

A complex predictive model should not be required for the correctness of the assignment process.

# 11.11 Different dispatch strategies — A final comparison

Let's put the major strategies together.

| Strategy                | How it works                                     | When it is useful                         |
| ----------------------- | ------------------------------------------------ | ----------------------------------------- |
| Sequential              | One driver receives an offer at a time           | Minimize contention and notifications     |
| Parallel                | Multiple drivers receive an offer simultaneously | Very low assignment latency               |
| Small batch             | A few drivers receive offers together            | Balance speed and contention              |
| Staggered               | Offers are sent progressively                    | Reduce unnecessary notifications          |
| Radius expansion        | Increase geographical search area                | Insufficient nearby supply                |
| Deferred dispatch       | Wait until the right assignment time             | Restaurant preparation and scheduled work |
| Batch assignment        | Assign compatible tasks together                 | Food delivery and logistics               |
| Scheduled assignment    | Reserve a future time slot                       | Home services and logistics               |
| Proactive repositioning | Move idle supply toward predicted demand         | Marketplace balancing                     |

An important observation:

These strategies are not mutually exclusive.

For example, food delivery might use:

```
Deferred dispatch
       |
       v
Nearby partner discovery
       |
       v
Multi-order route compatibility
       |
       v
Small-batch offer delivery
       |
       v
Atomic capacity reservation
```

Ride-hailing might use:

```
Nearby discovery
       |
       v
ETA ranking
       |
       v
Staggered dispatch
       |
       v
Atomic single-ride assignment
```

The underlying platform remains similar.

The orchestration policy changes.

# 11.12 Durability requirements differ across business models

Let's return to an earlier question:

> Should every location update be persisted?

There isn't one universal answer.

| Business requirement     | Latest location                   | Intermediate history                   | Assignment durability                |
| ------------------------ | --------------------------------- | -------------------------------------- | ------------------------------------ |
| Idle ride-hailing driver | High-value operational state      | Often unnecessary                      | High                                 |
| Active taxi trip         | High                              | May be needed                          | High                                 |
| Active food delivery     | High                              | Often useful for tracking and disputes | High                                 |
| Fleet monitoring         | High                              | Often important                        | High                                 |
| Shipment tracking        | High                              | Often important                        | High                                 |
| Home-service technician  | High near appointment             | Usually policy-dependent               | High                                 |
| Geofence monitoring      | High enough to detect transitions | Selected events may matter             | Depends on triggered business action |

For example, suppose a delivery customer complains:

> The food was marked delivered, but I never received it.

The system might need:

- Delivery partner's latest recorded locations.
- Historical route.
- Pickup and drop-off timestamps.
- Geofence-entry events.
- Delivery confirmation.
- Relevant audit events.

In this scenario, historical location and event retention have real business value.

For an idle driver who is simply waiting for a ride, retaining every three-second GPS update forever may provide little value.

This is why the event-retention policy should depend on business state and legal/product requirements.

### A useful hybrid design

```
Driver Location Update
         |
         v
     Ingestion
         |
         +------> Redis
         |          |
         |          +------> Live matching
         |
         +------> Kafka when history is required
                       |
                       v
                Historical Storage
```

We can use different retention and sampling policies for:

```
Idle
Assigned
En Route
In Progress
Completed
```

Be explicit about the point at which each policy changes.

# 11.13 Should all these business models use the same backend?

This is an important architecture question.

Imagine we're building a platform that supports:

- Bike rides.
- Taxi rides.
- Food delivery.
- Grocery delivery.
- Logistics.
- Home services.

Should we build completely separate location tracking systems?

Usually not.

We can share common infrastructure.

For example:

```
                 DRIVER / PROVIDER APPS
                           |
                           v
                  LOCATION INGESTION
                           |
                           v
                  LIVE LOCATION INDEX
                           |
                           v
                  SPATIAL DISCOVERY
                           |
              +------------+------------+
              |            |            |
              v            v            v
          Ride Matching  Food Matching  Fleet Matching
              |            |            |
              v            v            v
          Ride Dispatch  Order Dispatch Logistics Dispatch
              |            |            |
              v            v            v
          Assignment     Assignment    Shipment
          Workflow       Workflow      Workflow
```

Shared capabilities:

- Authentication.
- Location ingestion.
- Latest location storage.
- Geographic indexing.
- Location freshness.
- Connection management.
- Event delivery infrastructure.

Business-specific capabilities:

- Ride matching.
- Delivery batching.
- Fleet capacity planning.
- Appointment scheduling.
- Dispatch policy.
- Assignment state machines.

### Don't over-generalize the assignment model

We should not build one universal function:

```
assignDriver(request, driver);
```

and assume every business has identical assignment semantics.

A better design is to share the infrastructure while keeping business-specific policy and invariants explicit.

For example:

```
Ride Assignment:
    One active ride per driver.

Delivery Assignment:
    Multiple compatible orders within capacity.

Home Service:
    One or more scheduled appointments
    without time-slot conflicts.

Logistics:
    Vehicle capacity and shipment constraints.
```

We can share a reservation infrastructure with business-specific reservation policies.

# 11.14 How to choose the right architecture in an interview

Imagine the interviewer gives you one of these requirements:

### Question A: Design Uber

Your first thoughts should be:

```
1. Continuous driver location ingestion.
2. Latest location in Redis.
3. Geospatial discovery.
4. Driver freshness and availability.
5. ETA-based ranking.
6. Real-time offer delivery.
7. Single-driver assignment.
8. Atomic reservations.
9. Trip tracking and history.
```

Important algorithms:

- Redis GEO / H3.
- Radius expansion.
- Top-K candidate selection.
- ETA ranking.
- Small-batch dispatch.
- Conditional database updates.

### Question B: Design Swiggy/Zomato

Your thoughts should evolve:

```
1. Continuous partner location tracking.
2. Restaurant preparation-time estimation.
3. Partner discovery.
4. Pickup ETA prediction.
5. Deferred dispatch.
6. Multi-order route compatibility.
7. Partner capacity reservation.
8. Reliable assignment.
9. Customer tracking.
```

Additional algorithms:

- Route insertion heuristics.
- Pickup/delivery time-window constraints.
- Capacity-constrained assignment.
- Dynamic dispatch timing.

### Question C: Design a logistics fleet system

Think:

```
1. Long-lived fleet location tracking.
2. Historical route storage.
3. Geofencing.
4. Vehicle capacity filtering.
5. Route and delivery-window optimization.
6. Fleet utilization.
7. Shipment assignment.
8. Audit and delivery proof.
```

Additional algorithms:

- Vehicle routing problem heuristics.
- Geofence detection.
- Time-window scheduling.
- Capacity-constrained optimization.

### Question D: Design a technician-booking platform

Think:

```
1. Technician location.
2. Skill-based discovery.
3. Service-area constraints.
4. Appointment availability.
5. Travel-time calculation.
6. Future time-slot reservation.
7. Atomic booking.
```

Additional algorithms:

- Skill matching.
- Calendar scheduling.
- Interval conflict detection.
- Time-window optimization.

# 11.15 The final pattern selection table

This is a useful reference when approaching a new problem.

| Requirement                               | Concept/pattern to consider                    |
| ----------------------------------------- | ---------------------------------------------- |
| Millions of continuous location updates   | High-throughput ingestion + latest-value store |
| Latest location lookup                    | Key-value store                                |
| Nearby geographic search                  | Redis GEO / H3 / S2                            |
| Search over large geographic areas        | Spatial partitioning                           |
| Dense urban hotspots                      | Dynamic subdivision / subsharding              |
| Very frequent similar nearby queries      | Short-lived candidate caching                  |
| Driver stops sending updates              | Location freshness and failure detection       |
| Real-time offers                          | WebSocket gateway                              |
| Background notifications                  | Push notification fallback                     |
| Gateway owns connection                   | Connection registry                            |
| Delivery guarantee                        | Durable offer state + retryable messaging      |
| Driver receives duplicate offer           | Event ID and client deduplication              |
| Only one driver wins a ride               | Transactional conditional update               |
| One driver cannot take two rides          | Atomic driver reservation                      |
| Multiple concurrent delivery orders       | Capacity reservation + route compatibility     |
| Orders need to arrive near food readiness | Deferred dispatch                              |
| Drivers need to be moved toward demand    | Supply-demand prediction and repositioning     |
| Truck capacity constraints                | Hard eligibility filtering                     |
| Appointments have time windows            | Scheduling and slot reservation                |
| Preserve historical locations             | Kafka + historical storage                     |
| Recover lost live indexes                 | Rebuild from fresh updates or retained events  |
| Multi-region location searches            | Regional projections and federated search      |
| Multi-region assignment correctness       | Authoritative assignment owner and fencing     |

# 11.16 My recommended design philosophy

I want to highlight a few principles that apply across almost every real-world implementation.

1\. Don't confuse location tracking with matching.

Location tracking maintains the latest geographical information.

Matching uses that information together with business constraints.

2\. Don't confuse matching with assignment.

Matching identifies suitable candidates.

Assignment grants exclusive or capacity-constrained ownership.

3\. Don't confuse a notification with a business offer.

The offer exists in durable business state.

WebSocket or push notification only communicates it.

4\. Don't confuse proximity with suitability.

The nearest driver might have the wrong vehicle, lack the required skill, be unavailable, or have a poor ETA.

5\. Don't force every business into a single assignment model.

A taxi driver generally has one active ride.

A food delivery partner may carry multiple compatible orders.

A technician might have future appointments.

6\. Don't make every location update durable by default.

Use the durability level justified by the product.

7\. Don't introduce complicated algorithms before identifying the bottleneck.

Redis GEO may be enough initially.

H3, Quadtree, Kafka, regional sharding, and multi-stage assignment can be introduced when requirements justify them.

# Chapter 11 — Complete mental model

We can now visualize our platform as a reusable foundation with business-specific matching and dispatch policies.

\#chatgpt-mermaid-\_r_5dl\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_5dl\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5dl\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_5dl\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_5dl\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_5dl\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_5dl\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_5dl\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_5dl\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_5dl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5dl\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5dl\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_5dl\_ p{margin:0;}#chatgpt-mermaid-\_r_5dl\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_5dl\_ .label text,#chatgpt-mermaid-\_r_5dl\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .node rect,#chatgpt-mermaid-\_r_5dl\_ .node circle,#chatgpt-mermaid-\_r_5dl\_ .node ellipse,#chatgpt-mermaid-\_r_5dl\_ .node polygon,#chatgpt-mermaid-\_r_5dl\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .rough-node .label text,#chatgpt-mermaid-\_r_5dl\_ .node .label text,#chatgpt-mermaid-\_r_5dl\_ .image-shape .label,#chatgpt-mermaid-\_r_5dl\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_5dl\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .rough-node .label,#chatgpt-mermaid-\_r_5dl\_ .node .label,#chatgpt-mermaid-\_r_5dl\_ .image-shape .label,#chatgpt-mermaid-\_r_5dl\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_5dl\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_5dl\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5dl\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5dl\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_5dl\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5dl\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5dl\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5dl\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_5dl\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_5dl\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_5dl\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_5dl\_ .icon-shape,#chatgpt-mermaid-\_r_5dl\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_5dl\_ .icon-shape p,#chatgpt-mermaid-\_r_5dl\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_5dl\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_5dl\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_5dl\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_5dl\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_5dl\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_5dl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_5dl\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_5dl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_5dl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5dl\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_5dl\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_5dl\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5dl\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_5dl\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_5dl\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_5dl\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .node rect,#chatgpt-mermaid-\_r_5dl\_ .node circle,#chatgpt-mermaid-\_r_5dl\_ .node ellipse,#chatgpt-mermaid-\_r_5dl\_ .node polygon,#chatgpt-mermaid-\_r_5dl\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_5dl\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_5dl\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_5dl\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_5dl\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_5dl\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Mobile Providers / VehiclesShared Location IngestionLive Location Store andSpatial IndexOptional Durable LocationEventsGeographical DiscoveryBusiness-Specific EligibilityBusiness-Specific Rankingand OptimizationBusiness-Specific DispatchAuthoritative Reservationand AssignmentTransactional DatabaseReliable Event DeliveryWebSocket / Push / OtherChannels

The shared foundation handles geographical and communication infrastructure.

The business-specific components handle the actual matching objective.

This gives us a flexible architecture without pretending that every industry has identical requirements.

# Next: Chapter 12 — Complete End-to-End Architecture and System Design Interview Walkthrough

We've completed the individual technical patterns.

Now we'll bring everything together into one coherent production design.

We'll take a concrete problem:

```
Build a reliable, real-time location tracking
and driver matching platform.

1 million online drivers.
Hundreds of thousands of location updates per second.
Thousands of concurrent ride requests.
Multiple cities.
Multiple regions.
```

We'll construct the architecture from an empty system.

### What we'll do

1. Ask the right requirements and clarifying questions.
2. Estimate write throughput, reads, storage, and connections.
3. Define data models and business invariants.
4. Draw the complete architecture.
5. Trace one location update from mobile app to Redis.
6. Trace one nearby search from customer request to candidate selection.
7. Trace one ride offer from dispatch to WebSocket.
8. Trace simultaneous acceptance from multiple drivers.
9. Trace timeout, cancellation, and reassignment.
10. Walk through Redis, Kafka, database, gateway, and regional failures.
11. Identify bottlenecks and scaling strategies.
12. Compare alternative architectures and explain trade-offs.
13. Build a final decision framework for adapting the design to Uber, Swiggy, Zomato, logistics, or another platform.

We'll also create a final architecture decision matrix covering:

- HTTP vs WebSockets vs SSE vs push.
- Direct Redis vs Kafka-first.
- Redis GEO vs H3 vs geohash.
- Driver-ID vs geographical partitioning.
- Queue vs Kafka vs RPC.
- Redis locks vs database transactions.
- Sequential vs parallel vs batch dispatch.
- Ephemeral vs durable location history.
- Single-region vs multi-region.

The objective of the final chapter is that you should be able to receive a completely new location-based system design question, derive its requirements, select the appropriate patterns, and defend every major architectural decision without memorizing a predefined architecture.

Say next to continue.
