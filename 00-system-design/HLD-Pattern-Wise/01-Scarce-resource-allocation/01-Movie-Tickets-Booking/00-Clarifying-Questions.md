**Interpretation:** A BookMyShow-style platform where users browse movies and shows in a city, pick seats, pay, and receive a ticket.

## 1. Pattern and main design forks

- **Pattern:** Booking / limited-inventory reservation, with a temporary seat hold and a payment step.
- **Forks:**
  - Strict "never double-sell" (single writer per show, atomic seat state) vs. eventual consistency with oversell and compensation.
  - Hold seats with a timeout during payment vs. no hold (pay first, conflict later).
  - We own seat inventory vs. theaters' external systems own it and we integrate.
  - Normal traffic vs. flash-crowd releases (waiting room and heavy caching vs. plain autoscaling).
  - Payment is synchronous vs. async callbacks that can arrive late (reconciliation vs. simple flow).

## 2. Clarifying questions

| # | Bucket | Priority | Question, as I would say it out loud | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I'll assume we are an aggregator, but theaters upload shows and seat layouts to us, so we own the inventory. Is that right, or do theater systems stay the source of truth?" | We own inventory, so I can lock and decide seat state ourselves. | External source of truth: I need an adapter layer, sync, and a hold or confirm call to the theater. We can't guarantee the seat. | Who owns seat state |
| 2 | S | P1 | "Do users pick exact seats on a seat map, or is it general admission or auto-assigned seats?" | Seat-level state per show, a seat map API, and per-seat locking. | Just a counter per show/category. Far simpler and cheaper, with no per-seat contention. | Data granularity, locking unit |
| 3 | C | P0 | "The one thing that must never happen is the same seat sold to two people for the same show. Is that a hard rule, or is some overselling acceptable?" | Strong consistency per show: single writer or atomic conditional update on seat state. Availability can drop for that show. | Oversell allowed: optimistic path with compensation, upgrade, or refund. Higher availability. | Consistency model |
| 4 | C | P0 | "While the user pays, do we hold the seats for about 10 minutes, then release them?" | Temporary hold with a TTL, an expiry mechanism, and a pending state. | No hold: pay first, and a conflict means refund. Simpler state, worse user experience. | Hold/lock lifecycle |
| 5 | C | P0 | "Payment goes through an external gateway. If money is taken but our hold already expired, do we still give the ticket if the seat is free, or always refund?" | Re-check and confirm if free, otherwise refund. Needs reconciliation and idempotent callbacks. | Always refund: simpler, but more refunds and unhappy users. | Payment and booking state machine |
| 6 | A | P1 | "I'll assume browsing is far heavier than booking, around 100:1. Also, do some big releases open at a fixed time and cause a rush on one show?" | Hot shows: waiting room or queue, rate limit, cache the seat map, protect the hot partition. | Steady traffic: normal autoscaling, simple caching. | Hot-key and flash-crowd handling |
| 7 | L | P1 | "I'll assume 10M daily users, 1M bookings a day, and a peak of 10x to 20x average. Does that sound right? And how fresh must the seat map be: real-time, or a few seconds stale is fine?" | Real-time seat map: push or short polling backed by the source of truth. | A few seconds stale is OK: cache heavily, and the final check happens at hold time. | Cache strategy and read path cost |
| 8 | L | P1 | "For targets, I'll assume browse p99 under 300 ms with 99.9% availability, and for booking I'd choose correctness over availability. Is that okay?" | Booking fails closed during partitions or failures. | Booking must stay up always: we need to accept the oversell risk from Q3. | CAP trade-off per path |
| 9 | E | P1 | "Is this one country with many cities, or multi-country? And can payments, including card data, stay with the gateway?" | One country: regional deployment is optional, one time zone per theater, gateway handles PCI. | Multi-country: regional data placement, multi-currency, local compliance. | Region topology, compliance scope |
| 10 | D | P1 | "If the payment gateway is down, do we stop new bookings or keep holds longer? And do we support cancellation and refunds?" | Fail closed for booking. Browse stays up, and holds expire normally. | Extend holds or queue payments, which locks seats longer and needs a retry and pending flow. | Failure handling, pending states |

**Spoken block 1 (scope and correctness):** "I'll assume theaters upload their shows and we own seat inventory. Users pick exact seats on a map. The one rule I want to protect is that the same seat is never sold twice. I'd hold seats for about 10 minutes during payment. If payment lands late, I'd re-check the seat and refund if it's gone. Does that match what you want?"

**Spoken block 2 (scale and failure):** "For scale, I'll assume 10M daily users, 1M bookings a day, and a peak of 10 to 20 times average. Browsing is read-heavy, and a few seconds of stale seat map is fine. Are there fixed-time blockbuster releases? Booking should prefer correctness over availability. If the payment gateway is down, I'd stop new bookings but keep browsing alive. Is that okay?"

## 3. Assumptions to state out loud

- Login is handled, with standard auth. If wrong, we need guest checkout and abuse controls.
- Search and browse are by city, movie, and date, with basic filters. If wrong (full-text or geo search), we add a search component.
- Max 10 seats per booking. If wrong (group or bulk sales), the hold and lock logic changes.
- Fixed pricing, no dynamic pricing. If wrong, we add a pricing service and price-lock rules.
- Tickets are delivered by email, SMS, or app, sent asynchronously. If wrong (must be instant and guaranteed), we need a stricter delivery path.
- Food, offers, reviews, and recommendations are out of scope. If wrong, they become separate services.
- Data retention is a few years for bookings. If wrong, we change the archive and storage plan.
- Single currency and standard payment methods. If wrong, we add multi-currency and local payment handling.
- Show data changes (cancel or reschedule a show) are rare. If wrong, we need bulk refund and notify workflows.

## 4. Questions I deliberately did NOT ask

- **"Which database should I use?"** This is answer-seeking. The candidate decides it after the consistency answer in Q3.
- **"What payment methods do we support?"** Card, UPI, and wallets all go through the gateway, so the design is the same.
- **"Do we need email or SMS notifications?"** Both are one async notification path. The channel doesn't change the architecture.
- **"What fields does a booking or seat have?"** This is schema detail. It belongs to a later step.

## 5. Interviewer cheat sheet

| # | Good answer | Tricky answer | How the design changes |
|---|---|---|---|
| 1 | "You own inventory. Theaters upload shows." | "Half the theaters have their own systems, and they are the source of truth." | Add an integration layer. Seat state must be confirmed with the theater, so hold success isn't guaranteed. Use sync jobs, timeouts, and fallback behavior. |
| 2 | "Exact seat selection on a map." | "Both: assigned seating for some halls, general admission for others." | The model supports two inventory types per show: per-seat state and a counter. |
| 3 | "Never double-sell." | "Small oversell is fine for general admission, but never for reserved seats." | Strict locking for seat-level shows. Looser, counter-based admission for the others. |
| 4 | "Yes, hold 10 minutes." | "Hold 5 minutes, but extend while the payment page is open." | Hold becomes renewable, so I need a heartbeat and a cap on total hold time to stop seat hoarding. |
| 5 | "Refund if the seat is gone." | "Never refund automatically. Tickets must be honored." | We must guarantee the seat during payment, which means longer holds or reserving at payment start. This costs capacity. |
| 6 | "Yes, big releases open at a fixed time." | "Some shows get 100x traffic in seconds, and bots are common." | Waiting room, per-user rate limits, bot protection, and per-show queueing before the seat map is exposed. |
| 7 | "A few seconds stale is OK." | "Seat map must be live, since users get angry when a seat vanishes at pay time." | Push updates (WebSocket or SSE) from seat state changes, and less caching on hot shows. |
| 8 | "Correctness over availability for booking." | "Booking must have 99.99% availability." | Multi-AZ writers with replication per show. Accept the failover complexity, and plan for rare conflicts. |
| 9 | "One country, many cities." | "Launching in three countries soon." | Regional stacks, data residency rules, and per-region payment gateways. |
| 10 | "Stop bookings, keep browse up. Cancellation is simple." | "Bookings must continue even if the gateway fails. We have two gateways." | Add a gateway abstraction with failover and idempotency across providers. Reconciliation is harder. |

## 6. Board summary template

**FUNCTIONAL**
- Browse movies, theaters, and shows by city and date.
- View the seat map and pick exact seats.
- Hold seats for about 10 minutes, then pay.
- Receive a confirmed ticket, and cancel for a refund.
- Theater admins upload shows and seat layouts.

**NON-FUNCTIONAL**
- 10M daily users, 1M bookings a day, peak 10x to 20x average.
- Read-heavy, around 100:1.
- Browse p99 under 300 ms, with 99.9% availability.
- Booking favors correctness over availability.
- Seat map may be a few seconds stale.
- Single country, multi-city.

**INVARIANT**
- A seat for a given show is never sold to two users.
- Money taken always ends in a ticket or a refund.

**ASSUMPTIONS**
- We own seat inventory.
- Payments go through an external gateway, with callbacks that may arrive late.
- Fixed pricing, max 10 seats per booking.
- Notifications are async.
- Out of scope: food, offers, reviews, recommendations, dynamic pricing.

