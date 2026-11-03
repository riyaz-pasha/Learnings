**Interpretation:** a single online store (Amazon-style) where users check out a cart, pay, and then track, cancel, or return orders. I treat checkout and the order lifecycle as the scope, not catalog or search.

## 1. Pattern and main design forks

- **Pattern:** Payments plus inventory reservation (order workflow). It is close to the booking pattern, with a money step in the middle.
- **Forks:**
  - Hold stock at checkout start vs. decide only at payment success. (Lost sales vs. oversell.)
  - Strict inventory count vs. allow small oversell with backorder or cancel.
  - Synchronous "order confirmed" vs. asynchronous "order received, confirming".
  - Single seller vs. marketplace. (One order or split orders, one payment or split payouts.)
  - Our own payment handling vs. an external payment provider with webhooks.

## 2. Clarifying questions

| # | Bucket | Priority | Question, as I would say it out loud | If A, then... | If B, then... | Design decision it drives |
|---|---|---|---|---|---|---|
| 1 | S | P0 | "I will assume the scope is cart to checkout, payment, order tracking, cancel, and refund. Catalog, search, and shipping internals are out. Is that right?" | Yes: I focus on the order and payment flow, and treat inventory and shipping as black-box services. | Shipping or warehouse is in scope: I add fulfillment events and a warehouse integration. | Service boundaries |
| 2 | S | P0 | "Is this one seller, like a first-party store, or a marketplace with many sellers?" | One seller: one order = one payment = one fulfillment. | Marketplace: split orders per seller, split payments and payouts, partial refunds. | Order model and payment flow |
| 3 | C | P0 | "What must never happen? I will assume we never charge twice, never lose a paid order, and never sell more than we have. If we must pick between overselling and losing a sale, which is worse?" | Oversell is worse: strict stock reservation with strong consistency on inventory. | Lost sale is worse: soft reservation, small oversell allowed, cancel or backorder later. | Inventory consistency model |
| 4 | C | P0 | "When do we hold stock: when the user starts checkout, or only when payment succeeds? If we hold it, for about 10 minutes?" | Hold at checkout: a reservation with a TTL and a release job. | Decide at payment: no holds, but a payment-succeeded-but-out-of-stock case needs auto-refund. | Reservation flow and compensation |
| 5 | C | P0 | "I will assume we use an external payment provider and never store card data. Payment results can arrive late through webhooks. Is that fine? And can users retry or double-click safely?" | Yes: idempotency keys, a payment state machine, and webhook reconciliation. | We handle cards ourselves: PCI scope, a vault, and a much heavier security design. | Payment flow and idempotency |
| 6 | A | P1 | "I will assume reads are mostly order history and status, and writes are mostly checkout. Do we have flash sales where one hot item gets most traffic?" | No hot items: normal partitioning by user or order ID. | Yes: a hot-SKU path with queueing or a counter, and a waiting room. | Inventory hot-key handling |
| 7 | L | P1 | "I will assume 10M daily users, 1M orders a day, and a peak of about 10x on sale days, so roughly 100 to 150 orders per second on average and over 1,000 at peak. Checkout p99 under 1 second excluding the payment provider, and 99.99% availability for checkout. Does that sound right?" | Matches: standard sharded stores with queues for async work. | Much larger, e.g. 50K orders per second: heavy sharding, a queue-first checkout, and multi-region active-active. | Scale and availability design |
| 8 | E | P1 | "Do we need multiple countries, currencies, and tax rules, or one region for now?" | One region and currency: a single deployment and a simple tax step. | Multi-region: regional data, a currency and tax service, and compliance on data location. | Regions and tax integration |
| 9 | D | P0 | "If the payment provider or inventory service is down, should checkout fail fast, or accept the order as 'pending' and finish later? And after paying, can users cancel until shipped? Do orders need zero data loss?" | Fail fast, cancel allowed until shipped: simple, with an order state machine. | Accept as pending: durable queue, retries, and a visible pending state. Zero data loss: synchronous replication for orders. | Failure handling, order states, RPO/RTO |

**How I would say it out loud:** Block 1 is questions 1 to 5, asked as "scope and what must never go wrong". Block 2 is questions 6 to 9, asked as "scale, regions, and failure".

## 3. Assumptions to state out loud

- **Auth, cart, and catalog exist already.** If wrong, I add a cart service and price validation.
- **Pricing and promos come from a pricing service.** If wrong, I add a rules engine and promo-abuse checks.
- **Fraud checks are a black-box call.** If wrong, I add a risk-scoring step before payment.
- **Notifications (email, push) are async and best effort.** If wrong, I add guaranteed delivery.
- **Card data never touches us (provider tokens only).** If wrong, PCI scope grows a lot.
- **Web and mobile clients share the same APIs.** If wrong, I add an offline or retry layer for mobile.
- **No guest checkout.** If wrong, I add anonymous order lookup.
- **Budget is normal, with no special cost cap.** If wrong, I trade latency for cheaper storage and fewer regions.
- **Data retention is about 7 years for orders.** If wrong, I change the archival tiering.

## 4. Questions I deliberately did NOT ask

- **"Which payment provider should I use?"** This is answer-seeking. I choose it myself, and the design is the same for any provider with webhooks.
- **"Should the UI have a guest checkout page?"** This is a UI detail, and it is not an architecture fork.
- **"Is the system secure and does it support HTTPS?"** This is obvious, so it is an assumption.
- **"Do orders have an ID, status, and timestamp fields?"** This is schema detail and belongs to a later step.

## 5. Interviewer cheat sheet

| # | Good answer | Tricky answer | How the design changes |
|---|---|---|---|
| 1 | "Yes, the order and payment flow only." | "Include returns, refunds, and warehouse updates." | I add return and refund workflows and consume fulfillment events. |
| 2 | "First-party seller only." | "Marketplace with thousands of sellers." | Parent order with child orders per seller, split payments, per-seller state machines. |
| 3 | "Never double-charge. Small oversell is acceptable." | "Never oversell, even for limited drops." | Strongly consistent stock decrement, with reservations that are atomic per SKU. |
| 4 | "Hold for 10 minutes at checkout start." | "No holds. Decide at payment." | Auto-refund and apology flow when payment succeeds but stock is gone. |
| 5 | "External provider, retries must be safe." | "Provider webhooks may arrive hours late or out of order." | A reconciliation job and a state machine that tolerates reordering. |
| 6 | "Mostly normal traffic, some promotions." | "One drop sells 100K units in a minute." | Virtual queue, rate-limited admission, and a dedicated hot-SKU path. |
| 7 | "As you assumed." | "Black Friday is 50x, and checkout must stay up." | Queue-first checkout, autoscaling, load shedding, multi-region. |
| 8 | "One country for now." | "20 countries, local taxes, data residency." | Regional cells, tax service, and region-pinned order data. |
| 9 | "Fail fast. Zero loss for paid orders." | "Never block a sale. Accept now, resolve later." | Durable intake queue, pending order state, retries with backoff, and clear user messaging. |

## 6. Board summary template

**FUNCTIONAL**
- Checkout a cart (address, shipping choice, promo, tax) and pay.
- Create and track an order (placed, paid, shipped, delivered).
- Cancel (until shipped), return, and refund.

**NON-FUNCTIONAL**
- 10M DAU, about 1M orders per day, 10x peak on sale days (over 1,000 orders per second).
- Checkout p99 under 1 second (excluding the payment provider).
- 99.99% availability for checkout. Zero data loss for paid orders.
- Reads (order history) heavier than writes.

**INVARIANT**
- Never charge twice for one order.
- Never lose a paid order.
- Never sell more than available stock (small oversell is tolerated only if the interviewer confirms).

**ASSUMPTIONS**
- Single seller, one region, one currency to start.
- External payment provider, no card data stored, webhooks can be late.
- Stock held at checkout start for about 10 minutes.
- Catalog, cart, pricing, fraud, and notifications are separate services.
- If a dependency is down, checkout fails fast.

