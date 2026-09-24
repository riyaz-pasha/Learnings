## The Story of the Saga Pattern for Distributed Transactions

The very first guide in this series left a thread dangling: when the bookstore split into microservices, it lost the one free thing a monolith gives you — **one transaction spanning the whole operation.** This guide picks that thread back up and resolves it.

---

## Interview Cheat Sheet

**The Saga pattern** keeps a business transaction consistent across multiple services by replacing one big all-or-nothing lock with a sequence of small, real, committed local transactions, each paired with a **compensating action** (a separate transaction that undoes an earlier one's effect) to run if a later step fails.

**Good fit when:**
- The transaction spans multiple services/databases that each need to keep operating and scaling independently.
- Every step's effect can realistically be compensated — refunded, released, cancelled.
- The team can tolerate other parts of the system seeing an in-progress, not-yet-finished state.

**Bad fit when:**
- Everything fits inside one service, one database — a normal local ACID transaction is simpler and free.
- A step is truly irreversible and can't be reordered to run last (a package already handed to a courier, an email already sent).
- The business genuinely cannot tolerate any visible in-between state, or needs a hard guarantee instead of an eventual, compensated one.

**Core trade-off:** you give up true, hidden-until-commit atomicity and cross-service locking — in exchange for full service independence, an *approximate* atomicity built from compensations, and in-between states that are visible to the rest of the system.

**Quick recall:** order the saga's steps so the hardest-to-undo action happens last.

---

## Chapter 1: The Transaction That No Longer Fits in One Box

Placing an order now touches three independent services, each with its own database: **Inventory** (reserve the stock), **Payments** (charge the card), **Shipping** (create the shipment).

```mermaid
flowchart LR
    Order["Place Order"] --> Inv["Inventory Service\nreserve stock"]
    Order --> Pay["Payments Service\ncharge card"]
    Order --> Ship["Shipping Service\ncreate shipment"]
    Inv --> IDB[("Inventory DB")]
    Pay --> PDB[("Payments DB")]
    Ship --> SDB[("Shipping DB")]
```

In the monolith, this was one `BEGIN ... COMMIT` block — either all three happened, or none did, guaranteed by the database engine. Now, there is no database engine that spans all three. If Inventory's reservation succeeds and Payments' charge succeeds but Shipping then fails, you have a paid, reserved order with no shipment — a state that was **structurally impossible** before, and now has to be handled deliberately.

---

## Chapter 2: The First Instinct — Just Force a Distributed Transaction

The obvious idea: use a protocol that makes all three services commit or abort together, the same way a single database does internally. That protocol exists — it's called **Two-Phase Commit (2PC)**, and it's covered in full technical depth in [`database/DistributedTransactions/README.md`](../database/DistributedTransactions/README.md) in this repository. The short version, enough to understand why it doesn't fit here:

```mermaid
sequenceDiagram
    participant Coordinator
    participant Inventory
    participant Payments
    participant Shipping
    Coordinator->>Inventory: PREPARE
    Coordinator->>Payments: PREPARE
    Coordinator->>Shipping: PREPARE
    Inventory-->>Coordinator: YES (locks held, waiting)
    Payments-->>Coordinator: YES (locks held, waiting)
    Shipping--xCoordinator: ... slow to respond ...
    Note over Inventory,Payments: Both are STILL HOLDING LOCKS,\nblocking other requests, waiting\non Shipping to answer
```

2PC requires every participant to hold its locks — blocking other work — from the moment it says "yes" until the coordinator finally tells everyone to commit or abort. Across a network, with independently owned, independently scaled services, that means: a central coordinator that everyone depends on (a single point of failure), locks held across a network round-trip (slow, and fragile if any one participant is slow or down), and services no longer able to operate independently — the exact coupling this whole series has been trying to remove since the first guide. **2PC was designed for machines in the same data center under one operator's control. It doesn't hold up well across independently-deployed microservices owned by different teams.**

---

## Chapter 3: The Core Insight — Don't Lock. Do It, and Undo It If Needed.

The alternative, and the one that actually works at microservices scale: give up on a true all-or-nothing lock, and instead do each step as its own small, real, committed transaction — and if a later step fails, **run a compensating action** that undoes the effect of the earlier steps.

This sequence of local transactions, each with a matching undo action, is a **saga**.

```mermaid
flowchart LR
    T1["Reserve Inventory\n(commits immediately)"] --> T2["Charge Payment\n(commits immediately)"] --> T3["Create Shipment\n(commits immediately)"]
    T3 -.if this fails.-> C2["Compensate: Refund Payment"]
    C2 -.-> C1["Compensate: Release Inventory"]
```

Nothing is held open waiting for permission. Inventory's reservation genuinely commits the moment it happens — it's real, visible, and done. If a later step fails, you don't roll back a lock that was never held; you run a **new**, separate transaction whose entire purpose is to undo the earlier one's effect (release the reservation, refund the charge). This is the trade at the heart of the pattern: **you give up true atomicity, and get independence and no cross-service locking in return.**

---

## Chapter 4: Two Ways to Run a Saga — Choreography and Orchestration

### Choreography — Each Service Reacts to the Last One's Event

This connects directly to the Event-Driven Architecture guide: each service does its local transaction, then publishes an event; the next service in line is simply subscribed to that event and reacts to it. No one is in charge — the sequence emerges from who's listening for what.

```mermaid
sequenceDiagram
    participant Orders
    participant Broker
    participant Inventory
    participant Payments
    participant Shipping
    Orders->>Broker: publish OrderCreated
    Broker->>Inventory: OrderCreated
    Inventory->>Inventory: reserve stock (commits)
    Inventory->>Broker: publish StockReserved
    Broker->>Payments: StockReserved
    Payments->>Payments: charge card (commits)
    Payments->>Broker: publish PaymentCharged
    Broker->>Shipping: PaymentCharged
    Shipping->>Shipping: create shipment (commits)
```

This is simple to start with — no new component to build, just events and subscribers. It gets hard to reason about once you have six or seven steps: the actual sequence of "what happens after what" is scattered across every service's event handlers, and there's no one place to read it end to end.

### Orchestration — One Component Directs Every Step

The alternative: a dedicated **saga orchestrator** that explicitly calls each service in order, and explicitly calls the matching compensation if something fails.

```mermaid
sequenceDiagram
    participant Orchestrator as Saga Orchestrator
    participant Inventory
    participant Payments
    participant Shipping
    Orchestrator->>Inventory: reserve stock
    Inventory-->>Orchestrator: reserved (committed)
    Orchestrator->>Payments: charge card
    Payments-->>Orchestrator: FAILED (card declined)
    Orchestrator->>Inventory: compensate: release stock
    Inventory-->>Orchestrator: released
    Note over Orchestrator: Saga ended in a\nknown, controlled failure state
```

The whole sequence — including every compensation — lives in one place, in code you can actually read top to bottom. The trade-off: the orchestrator now knows about every service in the flow, which is a bit of the coupling the first guide moved away from — though notably, it's coupling contained to *one component whose entire job is coordination*, not spread across every business service's code.

```mermaid
flowchart TB
    subgraph Choreo["Choreography"]
        direction TB
        A1["No central coordinator"] --> A2["Simple for 2-3 steps"] --> A3["Hard to see the whole flow\nonce it grows"]
    end
    subgraph Orch["Orchestration"]
        direction TB
        B1["One orchestrator owns the sequence"] --> B2["Easy to read, easy to add\nnew steps/compensations"] --> B3["Orchestrator is a new\nsingle point of coordination"]
    end
```

This isn't just a whiteboard idea. **Netflix built and open-sourced Conductor**, a **workflow orchestration engine** (software that plays the same role as the saga orchestrator above, but built to run and track thousands of workflows at once) specifically to manage orchestration-based sagas across its microservices — content ingestion and encoding pipelines with many steps, each with defined rollback and retry behavior. Uber's trip-booking flow is a real saga too: match a driver, calculate the fare, authorize payment, send a notification — and if payment authorization fails, Uber runs a compensation that releases the matched driver back into the available pool, the exact same idea as releasing reserved inventory above.

### Deciding the Order: Which Step Goes Last?

There's a trick worth seeing concretely here — it comes back in Chapter 6, but it's easier to internalize with an example: rank each candidate step by how hard it would be to compensate, then order the saga so the hardest one runs last.

```mermaid
flowchart TD
    subgraph Ease["How hard is each step to undo?"]
        direction LR
        E1["Reserve Inventory:\neasy, just release the hold"]
        E2["Create Draft Shipment:\neasy, cancel the draft,\nnothing has shipped yet"]
        E3["Apply Discount Code:\neasy-ish, remove the code\n(watch for single-use codes)"]
        E4["Charge Payment:\nhard, refund is slow and\nvisible to the customer"]
    end
    Ease --> Order["Recommended order:\nhardest-to-undo step last"]
    Order --> O1["1. Reserve Inventory"] --> O2["2. Create Draft Shipment"] --> O3["3. Apply Discount Code"] --> O4["4. Charge Payment"]
```

---

## Chapter 5: The Cost — Compensations Are Not Real Rollbacks

### Cost 1 — You Can't Always Truly Undo Something

A database rollback is perfect — it's as if the write never happened. A compensating transaction is not that. If Shipping already printed a shipping label and handed the package to a courier before the saga fails at a later step, you cannot "un-hand" the package to the courier. The compensation becomes "intercept the package" or "issue a return," which is a real-world business process, not a clean database undo. **Compensations only work if you design each step to have a realistic, defined undo action — and some actions, once done, genuinely don't have a clean one.**

### Cost 2 — Other Transactions Can See the In-Between State

Because each step commits for real and immediately, there's a window where the saga is only partially done — inventory is reserved, payment hasn't happened yet — and any other process reading that data right now sees this half-finished state. In a single database transaction, nobody outside sees any of it until the whole thing commits. In a saga, the in-between states are real and visible, and other parts of your system need to be written with that in mind (for example, "reserved but not yet paid" inventory should probably still count as unavailable to other shoppers, even though the saga hasn't finished).

### Cost 3 — You Must Design a Compensation for Every Step, Up Front

Every step in the saga needs its undo counterpart designed and built *before* you need it in production — and thinking through every possible failure point, for every step, in every order they could occur, is real design work that a single ACID transaction gave you for free.

### Cost 4 — Testing Every Failure Combination Is Genuinely Hard

With three steps, there are already several distinct places a saga can fail (fail at step 1, fail at step 2 after step 1 committed, fail at step 3 after both committed, fail *during* a compensation itself). Each of these needs its own test. This complexity grows with every step you add to the saga.

### Seeing It at Five Steps

The three-step example earlier only needed to undo two prior steps. Here's what the same idea looks like with five steps, where the failure happens later and there's more to unwind — order placed, stock reserved, a shipment draft created, payment charged, and now the shipment needs to be confirmed with the carrier before a notification goes out:

```mermaid
sequenceDiagram
    participant Orchestrator as Saga Orchestrator
    participant Inventory
    participant Shipping
    participant Payments
    participant Notifications
    Orchestrator->>Inventory: reserve stock
    Inventory-->>Orchestrator: reserved (committed)
    Orchestrator->>Shipping: create shipment draft
    Shipping-->>Orchestrator: draft created (committed)
    Orchestrator->>Payments: charge payment
    Payments-->>Orchestrator: charged (committed)
    Orchestrator->>Shipping: confirm shipment
    Shipping--xOrchestrator: FAILED (carrier rejected package)
    Note over Orchestrator: Step 4 failed - everything before\nit already committed, so reverse\nit all, in reverse order
    Orchestrator->>Payments: compensate: refund payment
    Payments-->>Orchestrator: refunded
    Orchestrator->>Shipping: compensate: cancel shipment draft
    Shipping-->>Orchestrator: cancelled
    Orchestrator->>Inventory: compensate: release stock
    Inventory-->>Orchestrator: released
    Note over Orchestrator: Saga ended in a known,\ncontrolled failure state -\nSend Notification never runs at all
```

Notice the order of the undo: step 3 (payment) is compensated first, then step 2 (shipment draft), then step 1 (inventory) — the exact reverse of the order they committed in. Step 5, Send Notification, never runs, because the saga never got there.

---

## Chapter 6: When Do You Reach for a Saga?

```mermaid
flowchart TD
    Q1{"Does this transaction\nspan more than one service\n/ more than one database?"}
    Q1 -->|No, one service,\none database| Local["Just use a normal local\nACID transaction"]
    Q1 -->|Yes| Q2{"Can every step's effect\nrealistically be compensated\n(refunded, released, cancelled)?"}
    Q2 -->|No — some step is\ntruly irreversible| Rethink["Redesign the step order so\nirreversible actions happen LAST\n(e.g. charge the card only\nafter shipment is confirmed ready)"]
    Q2 -->|Yes| Q3{"How many steps,\nand who should own\nthe sequence logic?"}
    Q3 -->|"Few steps, simple,\nteam is comfortable\nwith event-driven flows"| Choreo["Choreography"]
    Q3 -->|"Several steps, or you want\none readable place to see\nthe whole flow"| Orch["Orchestration"]
```

A practical trick worth internalizing: **order your saga's steps so the hardest-to-undo action happens last.** Reserving inventory and creating a draft shipment are both easy to reverse. Charging a customer's card is the hardest to cleanly undo (refunds are possible, but they're never invisible to the customer, and they take days to process). Put the payment charge as the last step, after everything else has already succeeded, and you dramatically shrink the number of scenarios where you need a compensation at all.

---

## The Full Story, End to End

```mermaid
flowchart TB
    A["Order spans 3 services,\n3 databases — no shared transaction"] --> B["2PC doesn't fit: locks held\nacross the network, single\ncoordinator, blocks independent scaling"]
    B --> C["Saga: each step is its own\nreal, committed local transaction"]
    C --> D["If a later step fails,\nrun compensations to undo\nearlier steps"]
    D --> E["Choose choreography (event-driven,\nsimple, no central owner) or\norchestration (one readable flow,\nnew coordination point)"]
    E --> F["Cost: compensations aren't perfect\nrollbacks, in-between states are\nvisible, every step needs a\ndesigned undo path"]
```

| | Single DB Transaction | Saga |
|---|---|---|
| Atomicity | True — all or nothing | Approximate — via compensations |
| Locking | Locks held until commit | No cross-service locks held |
| Visibility of in-progress state | Hidden until commit | Visible — other reads can see partial progress |
| Failure recovery | Automatic rollback | Explicit compensating transactions you must design |
| Coordination | The database engine | Choreography (events) or Orchestration (a coordinator) |
| Best for | One service, one database | A business transaction spanning multiple services |

**Where would you like to go next?** Natural threads from here:

- **Circuit Breaker Pattern** — protecting each individual step's network call within a saga from a slow or failing dependency
- **Event-Driven Architecture** (earlier guide) — the exact mechanism choreography-based sagas are built on
- **Distributed Transactions deep dive** — the full 2PC, three-phase commit, and consensus-algorithm story, in [`database/DistributedTransactions/README.md`](../database/DistributedTransactions/README.md)
