# Chapter 8: Locking in High-Level Design and Production Systems

In this chapter, we'll apply everything we've learned to real production scenarios.

For each problem, we'll identify:

1. The business invariant.
2. What can go wrong under concurrency.
3. The simplest correct solution.
4. Whether PostgreSQL locks, Redis locks, optimistic locking, or queues are appropriate.
5. Failure scenarios and production safeguards.
6. How to explain the design in a system design interview.

The most important principle is:

Start with the invariant you must protect, not with the locking technology you want to use.

## 1. Inventory management and flash sales

Imagine an e-commerce platform has 100 units of a product. During a flash sale, thousands of requests arrive within seconds.

The business invariant is:

> Available inventory must never become negative, and the system must not confirm more reservations than the inventory permits.

### 1.1 The naive implementation

```
1. Read stock.
2. If stock > 0:
       Create order.
       Decrease stock.
3. Return success.
```

Suppose stock is 1.

Two requests read the value before either updates it:

```
Request A reads stock = 1
Request B reads stock = 1

Request A creates order
Request B creates order
```

The application has oversold the item.

The problem is that the application performed a check and a state change separately without correctly coordinating them.

### 1.2 Recommended starting point: Atomic conditional update

For a straightforward inventory decrement, use PostgreSQL:

```
UPDATE inventory
SET stock = stock - 1
WHERE product_id = :product_id
  AND stock > 0
RETURNING stock;
```

The application treats a returned row as a successful inventory reservation.

If no row is returned, the product is unavailable or doesn't exist.

If the order must be persisted as part of the same operation, combine the inventory update and order creation in a database transaction:

```
BEGIN;

UPDATE inventory
SET stock = stock - 1
WHERE product_id = :product_id
  AND stock > 0
RETURNING product_id;
```

If the update succeeds, insert the corresponding order or reservation. If it doesn't, roll back or return an unavailable response as appropriate. Commit only when all required database changes succeed.

### 1.3 Why not Redis locking?

You could acquire a Redis lock for the product, check the stock, update the database, and release the lock.

But that introduces several concerns:

- What if the Redis lease expires during the operation?
- What if Redis fails over and loses the lock state?
- What if the application crashes after updating the database but before releasing the lock?
- What if a request reaches the database without acquiring the lock?

For a database-owned inventory invariant, PostgreSQL is already the authoritative place to enforce correctness.

Redis might still be useful for other coordination tasks, but it shouldn't be the only protection against overselling.

### 1.4 What changes during a flash sale?

At very high traffic, the single inventory row can become a contention hotspot.

Possible improvements include:

- Queue-based reservation processing.
- Partitioned inventory counters, if the inventory model supports them.
- Preallocated inventory reservations.
- Rate limiting and admission control.
- Short transactions and efficient indexes.
- A separate reservation state with expiration and recovery.

Each approach has trade-offs. For example, preallocating inventory across partitions can improve throughput but makes inventory rebalancing more complicated.

Interview answer: Start with an atomic conditional update and a transaction. Introduce queues or inventory partitioning only when contention or throughput requirements justify the additional complexity.

## 2. Movie-ticket booking

Suppose 10,000 users are trying to book seats for the same movie show. Each seat can be booked by only one user.

The invariant is:

> A seat must not be confirmed for two different bookings for the same show.

This is a classic system design locking problem.

### 2.1 Database schema

```
CREATE TABLE shows (
    show_id BIGINT PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE bookings (
    booking_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id BIGINT NOT NULL REFERENCES shows(show_id),
    seat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status TEXT NOT NULL,

    CONSTRAINT unique_seat_per_show
        UNIQUE (show_id, seat_id)
);
```

For this simplified schema, a booking row represents an occupied seat. If cancelled seats must become available again, the data model and uniqueness strategy must account for the booking lifecycle—for example, by maintaining a separate seat-inventory row or using a suitable partial unique index for active bookings.

### 2.2 Option A: Unique constraint

Attempt to insert the booking:

```
INSERT INTO bookings (show_id, seat_id, user_id, status)
VALUES (:show_id, :seat_id, :user_id, 'CONFIRMED');
```

If another transaction has already booked that seat, PostgreSQL's unique constraint prevents a conflicting booking from committing.

This is a valuable last line of defense because it protects the invariant even if application instances race.

### 2.3 Option B: Lock a seat row

If you maintain a row for every seat in every show, you can lock the specific seat:

```
BEGIN;

SELECT status
FROM show_seats
WHERE show_id = :show_id
  AND seat_id = :seat_id
FOR UPDATE;
```

After acquiring the row lock, check the status and transition the seat to the desired state. Create the booking in the same transaction, then commit.

The lock serializes competing attempts for that seat. The transaction must remain short, and the database should still enforce the appropriate uniqueness or state constraints.

### 2.4 What about payment?

This is where the design gets more interesting.

You generally should not hold a database row lock while waiting for a payment provider to respond over the network.

A better high-level workflow is:

\#chatgpt-mermaid-\_r_4bt\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4bt\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4bt\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4bt\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4bt\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4bt\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4bt\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4bt\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4bt\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4bt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4bt\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4bt\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4bt\_ p{margin:0;}#chatgpt-mermaid-\_r_4bt\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4bt\_ .label text,#chatgpt-mermaid-\_r_4bt\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .node rect,#chatgpt-mermaid-\_r_4bt\_ .node circle,#chatgpt-mermaid-\_r_4bt\_ .node ellipse,#chatgpt-mermaid-\_r_4bt\_ .node polygon,#chatgpt-mermaid-\_r_4bt\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .rough-node .label text,#chatgpt-mermaid-\_r_4bt\_ .node .label text,#chatgpt-mermaid-\_r_4bt\_ .image-shape .label,#chatgpt-mermaid-\_r_4bt\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4bt\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .rough-node .label,#chatgpt-mermaid-\_r_4bt\_ .node .label,#chatgpt-mermaid-\_r_4bt\_ .image-shape .label,#chatgpt-mermaid-\_r_4bt\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4bt\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4bt\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4bt\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4bt\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4bt\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4bt\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4bt\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4bt\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4bt\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4bt\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4bt\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4bt\_ .icon-shape,#chatgpt-mermaid-\_r_4bt\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4bt\_ .icon-shape p,#chatgpt-mermaid-\_r_4bt\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4bt\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4bt\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4bt\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4bt\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4bt\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4bt\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4bt\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4bt\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4bt\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4bt\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4bt\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4bt\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4bt\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .node rect,#chatgpt-mermaid-\_r_4bt\_ .node circle,#chatgpt-mermaid-\_r_4bt\_ .node ellipse,#chatgpt-mermaid-\_r_4bt\_ .node polygon,#chatgpt-mermaid-\_r_4bt\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4bt\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4bt\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4bt\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4bt\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4bt\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}User selects seatReserve seat in databaseReservation succeeded?Tell user seat is unavailableCommit reservation withexpiryProcess payment withidempotencyPayment successful?Confirm bookingRelease or expire reservationNoYesYesNo

A practical implementation might use a short-lived reservation state, an expiration timestamp, and a background process to recover expired reservations.

The important design questions are:

- How long can a reservation remain pending?
- Can two payment requests race?
- What happens if payment succeeds but the response is lost?
- What happens if the reservation expires while payment is in progress?
- How do we prevent a late payment response from confirming an expired reservation?

The database should validate the allowed state transition before confirming a booking. Payment requests should use idempotency, and uncertain outcomes should be reconciled rather than blindly retried.

Interview takeaway: Use database constraints and transactional seat reservation for correctness. Do not depend solely on Redis locks to prevent double booking.

## 3. Payment processing

Payment systems require careful handling because duplicate execution can have financial consequences.

Imagine a client sends a payment request. The payment provider charges the customer, but the response is lost.

The application doesn't know whether the payment succeeded.

If it simply retries the charge without an idempotency mechanism, the customer could be charged twice.

### 3.1 The invariant

> One logical payment request must not produce duplicate financial effects.

This is not merely a locking problem. It is a problem involving retries, uncertain outcomes, durable state, and idempotency.

### 3.2 Recommended approach

Use a combination of:

- A stable idempotency key.
- A durable payment record.
- Transactional state transitions.
- Provider-side idempotency where available.
- Reconciliation for uncertain outcomes.

For example:

```
UPDATE payments
SET status = 'PROCESSING'
WHERE payment_id = :payment_id
  AND status = 'PENDING'
RETURNING payment_id;
```

This allows one transaction to claim a payment that is still pending. However, claiming the payment is not the same as completing it.

The application should persist the outcome of the external payment operation and handle crashes between the provider response and the local database update.

### 3.3 Why isn't `SELECT FOR UPDATE` enough?

Suppose you hold a PostgreSQL row lock and call the payment provider while the transaction remains open.

This can create several problems:

- The transaction holds database resources while waiting on a network call.
- Slow provider responses increase lock duration.
- Database connections remain occupied.
- A timeout or crash can leave the payment outcome uncertain.
- The database transaction cannot automatically roll back an external charge.

Even if the local transaction rolls back, a successful external payment may still have occurred.

A safer design usually separates the short database transaction from the external call, using durable state and idempotency to handle retries.

### 3.4 Should we use a Redis lock?

A Redis lock might reduce simultaneous attempts to process the same payment, but it does not guarantee that the provider will receive only one charge request.

The lock can expire, an application can crash, or a request can be retried.

The payment provider's idempotency mechanism and your durable payment state are more important for preventing duplicate financial effects.

Interview answer: For payment processing, prioritize durable state transitions, idempotency, and reconciliation. A lock may be supplementary, but it is not the primary correctness guarantee.

## 4. Cache stampede and cache refresh coordination

Suppose a popular product page is cached in Redis. The cache entry expires, and 5,000 requests arrive almost simultaneously.

All 5,000 requests discover a cache miss and query the database or an expensive downstream service.

This is called a cache stampede.

### 4.1 Without coordination

\#chatgpt-mermaid-\_r_4cd\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4cd\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cd\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cd\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4cd\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4cd\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4cd\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4cd\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4cd\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4cd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cd\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cd\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4cd\_ p{margin:0;}#chatgpt-mermaid-\_r_4cd\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4cd\_ .label text,#chatgpt-mermaid-\_r_4cd\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .node rect,#chatgpt-mermaid-\_r_4cd\_ .node circle,#chatgpt-mermaid-\_r_4cd\_ .node ellipse,#chatgpt-mermaid-\_r_4cd\_ .node polygon,#chatgpt-mermaid-\_r_4cd\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .rough-node .label text,#chatgpt-mermaid-\_r_4cd\_ .node .label text,#chatgpt-mermaid-\_r_4cd\_ .image-shape .label,#chatgpt-mermaid-\_r_4cd\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4cd\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .rough-node .label,#chatgpt-mermaid-\_r_4cd\_ .node .label,#chatgpt-mermaid-\_r_4cd\_ .image-shape .label,#chatgpt-mermaid-\_r_4cd\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4cd\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4cd\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cd\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cd\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4cd\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4cd\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cd\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cd\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4cd\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4cd\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cd\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4cd\_ .icon-shape,#chatgpt-mermaid-\_r_4cd\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4cd\_ .icon-shape p,#chatgpt-mermaid-\_r_4cd\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4cd\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4cd\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cd\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4cd\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4cd\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4cd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4cd\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4cd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4cd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cd\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4cd\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cd\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4cd\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4cd\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4cd\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4cd\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .node rect,#chatgpt-mermaid-\_r_4cd\_ .node circle,#chatgpt-mermaid-\_r_4cd\_ .node ellipse,#chatgpt-mermaid-\_r_4cd\_ .node polygon,#chatgpt-mermaid-\_r_4cd\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4cd\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4cd\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4cd\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4cd\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cd\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Cache entry expires5,000 concurrent requestsAll requests miss cacheAll query databaseDatabase experiences loadspike

The issue is duplicate work rather than necessarily incorrect data.

### 4.2 Option A: Single-flight request coalescing

Within one application instance, use a mechanism that allows one request to perform the refresh while other requests share its result.

This is often called single-flight or request coalescing.

However, an in-memory mechanism coordinates only requests that reach that particular process. It does not coordinate every instance in a horizontally scaled service.

### 4.3 Option B: Redis lock

A distributed lock can coordinate refreshes across multiple application instances.

A typical flow is:

1. Check the cache.
2. On a miss, attempt to acquire a refresh lock.
3. If acquired, load the data and populate the cache.
4. If not acquired, briefly wait and recheck the cache, or use an acceptable stale value.

\#chatgpt-mermaid-\_r_4cm\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4cm\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cm\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4cm\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4cm\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4cm\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4cm\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4cm\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4cm\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4cm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cm\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cm\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4cm\_ p{margin:0;}#chatgpt-mermaid-\_r_4cm\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4cm\_ .label text,#chatgpt-mermaid-\_r_4cm\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .node rect,#chatgpt-mermaid-\_r_4cm\_ .node circle,#chatgpt-mermaid-\_r_4cm\_ .node ellipse,#chatgpt-mermaid-\_r_4cm\_ .node polygon,#chatgpt-mermaid-\_r_4cm\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .rough-node .label text,#chatgpt-mermaid-\_r_4cm\_ .node .label text,#chatgpt-mermaid-\_r_4cm\_ .image-shape .label,#chatgpt-mermaid-\_r_4cm\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4cm\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .rough-node .label,#chatgpt-mermaid-\_r_4cm\_ .node .label,#chatgpt-mermaid-\_r_4cm\_ .image-shape .label,#chatgpt-mermaid-\_r_4cm\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4cm\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4cm\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cm\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cm\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4cm\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4cm\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cm\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cm\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4cm\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4cm\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4cm\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4cm\_ .icon-shape,#chatgpt-mermaid-\_r_4cm\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4cm\_ .icon-shape p,#chatgpt-mermaid-\_r_4cm\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4cm\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4cm\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4cm\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4cm\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4cm\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4cm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4cm\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4cm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4cm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cm\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4cm\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4cm\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4cm\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4cm\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4cm\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4cm\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .node rect,#chatgpt-mermaid-\_r_4cm\_ .node circle,#chatgpt-mermaid-\_r_4cm\_ .node ellipse,#chatgpt-mermaid-\_r_4cm\_ .node polygon,#chatgpt-mermaid-\_r_4cm\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4cm\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4cm\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4cm\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4cm\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4cm\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Cache missAcquired refresh lock?Load source dataPopulate cacheRelease lock safelyWait briefly or use stale dataRecheck cacheYesNo

This is a reasonable use case for a Redis lock because duplicate refreshes are often expensive but not inherently dangerous to business correctness.

However, the implementation must still handle refresh failures, lease expiry, and stale results.

### 4.4 Other useful techniques

TTL jitter

Instead of expiring thousands of entries at exactly the same time, add a small random variation to their TTLs.

Stale-while-revalidate

Serve a still-acceptable cached value while refreshing it in the background.

Early refresh

Refresh popular entries before they expire, based on traffic or an expiration policy.

Request coalescing

Let concurrent requests share a single in-flight refresh result.

Interview takeaway: A Redis lock can help prevent duplicate cache refreshes across instances, but stale-while-revalidate and TTL jitter may reduce the need for locking altogether.

## 5. Distributed job schedulers

Imagine a service is deployed with five replicas, and every replica runs the same scheduled task at midnight.

```
Instance A: Run monthly report
Instance B: Run monthly report
Instance C: Run monthly report
Instance D: Run monthly report
Instance E: Run monthly report
```

If the report is expensive, duplicate execution wastes resources.

### 5.1 Option A: Distributed lease

One instance acquires a shared lease and runs the job. Other instances skip that run.

This can work when duplicate execution is tolerable or the job has additional safeguards against overlapping execution.

But what if the instance loses its lease halfway through the job?

Another instance might start the same job while the first continues running.

For correctness-sensitive jobs, you need more than an ownership key.

### 5.2 Option B: Durable job claiming

Represent scheduled work in a database table:

```
CREATE TABLE jobs (
    job_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_type TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMPTZ NOT NULL,
    claimed_by TEXT,
    claimed_at TIMESTAMPTZ
);
```

Workers can claim available jobs in short transactions:

```
BEGIN;

WITH candidate AS (
    SELECT job_id
    FROM jobs
    WHERE status = 'PENDING'
      AND scheduled_at <= now()
    ORDER BY scheduled_at, job_id
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE jobs AS j
SET status = 'PROCESSING',
    claimed_by = :worker_id,
    claimed_at = now()
FROM candidate AS c
WHERE j.job_id = c.job_id
RETURNING j.*;

COMMIT;
```

This example illustrates a basic claim operation. A production queue also needs a lease or recovery policy for abandoned `PROCESSING` jobs, ownership checks when completing work, retry limits, and idempotent side effects.

The short transaction claims the job, then the worker performs the longer-running task outside that transaction.

### 5.3 Why use `SKIP LOCKED`?

Without it, workers might wait for the same candidate row.

With `SKIP LOCKED`, a worker can skip rows already locked by other workers and claim a different available job.

This is useful for throughput, but it does not guarantee strict global ordering. A job may be skipped temporarily while another worker holds its lock.

### 5.4 Should we use Redis or PostgreSQL?

Use a Redis lease when the primary need is lightweight coordination across application instances.

Use durable job state and database claiming when the work itself needs reliable tracking, recovery, retries, and auditability.

Many production systems use a dedicated message broker or workflow engine instead. The right choice depends on throughput, delivery guarantees, scheduling requirements, and operational complexity.

Interview takeaway: For durable background work, prefer a job queue or durable job state over using a single global lock as the entire job-processing mechanism.

## 6. Leader election and scheduled maintenance

Consider a service that periodically performs maintenance, such as refreshing shared metadata or coordinating a background reconciliation process.

You want one active leader at a time.

### 6.1 The basic approach

A coordination service grants a lease to one instance:

\#chatgpt-mermaid-\_r_4dj\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4dj\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4dj\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4dj\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4dj\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4dj\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4dj\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4dj\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4dj\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4dj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4dj\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4dj\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4dj\_ p{margin:0;}#chatgpt-mermaid-\_r_4dj\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4dj\_ .label text,#chatgpt-mermaid-\_r_4dj\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .node rect,#chatgpt-mermaid-\_r_4dj\_ .node circle,#chatgpt-mermaid-\_r_4dj\_ .node ellipse,#chatgpt-mermaid-\_r_4dj\_ .node polygon,#chatgpt-mermaid-\_r_4dj\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .rough-node .label text,#chatgpt-mermaid-\_r_4dj\_ .node .label text,#chatgpt-mermaid-\_r_4dj\_ .image-shape .label,#chatgpt-mermaid-\_r_4dj\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4dj\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .rough-node .label,#chatgpt-mermaid-\_r_4dj\_ .node .label,#chatgpt-mermaid-\_r_4dj\_ .image-shape .label,#chatgpt-mermaid-\_r_4dj\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4dj\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4dj\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4dj\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4dj\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4dj\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4dj\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4dj\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4dj\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4dj\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4dj\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4dj\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4dj\_ .icon-shape,#chatgpt-mermaid-\_r_4dj\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4dj\_ .icon-shape p,#chatgpt-mermaid-\_r_4dj\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4dj\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4dj\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4dj\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4dj\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4dj\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4dj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4dj\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4dj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4dj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4dj\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4dj\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4dj\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4dj\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4dj\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4dj\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4dj\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .node rect,#chatgpt-mermaid-\_r_4dj\_ .node circle,#chatgpt-mermaid-\_r_4dj\_ .node ellipse,#chatgpt-mermaid-\_r_4dj\_ .node polygon,#chatgpt-mermaid-\_r_4dj\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4dj\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4dj\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4dj\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4dj\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4dj\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Service instancesCoordination serviceCurrent leaderPerform leaderresponsibilitiesOther instancesStand by or performnon-leader work

When the leader's lease expires or its session ends, another instance can become leader.

For simple tasks where duplicate execution is harmless, a lease may be sufficient.

But consider a task that modifies authoritative shared state. The old leader may be paused, lose its lease, and resume after a new leader has taken over.

### 6.2 How do we make leadership safer?

Use a coordination system that provides the required ownership guarantees, and consider fencing tokens or epochs for protected writes.

For example:

```
Epoch 20: Instance A becomes leader
Epoch 21: Instance B becomes leader

Instance A resumes and attempts a write with epoch 20
Protected resource rejects the stale epoch
```

The resource must enforce the epoch rule. Merely assigning a new leader does not stop the old leader's code from executing.

### 6.3 When should you use a dedicated coordinator?

Consider a system such as etcd or ZooKeeper when you need coordination primitives and failure semantics beyond what a simple Redis lease provides.

However, avoid deploying a separate coordination service without a clear requirement. If a PostgreSQL transaction, a unique constraint, or a queue already solves the problem, adding another distributed dependency may increase operational complexity without improving correctness.

## 7. Lock contention, deadlocks, and production observability

Correct locking logic is only part of the problem. In production, locks can also become a performance bottleneck.

### 7.1 Lock contention

Lock contention occurs when multiple operations need the same lock and some must wait.

Imagine 1,000 requests updating the same inventory row:

```
Request 1 ──┐
Request 2 ──┤
Request 3 ──┼──> Same database row
Request 4 ──┤
Request 5 ──┘
```

Even if the operations are correct, throughput may be limited by how quickly that row can be updated.

Common symptoms include:

- Increased database query latency.
- Requests waiting on locks.
- Rising connection-pool utilization.
- Reduced throughput despite additional application instances.
- Timeouts and retries that increase the load further.

### 7.2 How do we reduce contention?

Keep transactions short. Don't call external APIs or perform lengthy computations while holding database locks.

Lock only what you need. Avoid locking an entire table when a row-level operation is sufficient.

Use atomic updates. Express simple state transitions in one SQL statement.

Keep lock ordering consistent. For example, always lock account IDs in ascending order when transferring between accounts.

Partition hot resources where possible. If the business rules permit it, divide a single high-contention resource into independently updateable units.

Avoid uncontrolled retries. A large number of clients retrying immediately can amplify contention.

### 7.3 Deadlocks

A deadlock occurs when transactions wait on each other in a cycle.

For example:

\#chatgpt-mermaid-\_r_4e8\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_4e8\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4e8\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_4e8\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_4e8\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_4e8\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_4e8\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_4e8\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_4e8\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_4e8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e8\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e8\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_4e8\_ p{margin:0;}#chatgpt-mermaid-\_r_4e8\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_4e8\_ .label text,#chatgpt-mermaid-\_r_4e8\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .node rect,#chatgpt-mermaid-\_r_4e8\_ .node circle,#chatgpt-mermaid-\_r_4e8\_ .node ellipse,#chatgpt-mermaid-\_r_4e8\_ .node polygon,#chatgpt-mermaid-\_r_4e8\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .rough-node .label text,#chatgpt-mermaid-\_r_4e8\_ .node .label text,#chatgpt-mermaid-\_r_4e8\_ .image-shape .label,#chatgpt-mermaid-\_r_4e8\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_4e8\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .rough-node .label,#chatgpt-mermaid-\_r_4e8\_ .node .label,#chatgpt-mermaid-\_r_4e8\_ .image-shape .label,#chatgpt-mermaid-\_r_4e8\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_4e8\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_4e8\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e8\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e8\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_4e8\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4e8\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e8\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e8\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_4e8\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_4e8\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_4e8\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_4e8\_ .icon-shape,#chatgpt-mermaid-\_r_4e8\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_4e8\_ .icon-shape p,#chatgpt-mermaid-\_r_4e8\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_4e8\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_4e8\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_4e8\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_4e8\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_4e8\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_4e8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_4e8\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_4e8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_4e8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e8\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_4e8\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_4e8\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4e8\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_4e8\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_4e8\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_4e8\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .node rect,#chatgpt-mermaid-\_r_4e8\_ .node circle,#chatgpt-mermaid-\_r_4e8\_ .node ellipse,#chatgpt-mermaid-\_r_4e8\_ .node polygon,#chatgpt-mermaid-\_r_4e8\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_4e8\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_4e8\_ .node.mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_4e8\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_4e8\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_4e8\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Transaction A holds row 1A waits for row 2Transaction B holds row 2B waits for row 1Deadlock cycle

PostgreSQL detects deadlocks and aborts one of the transactions.

The application should handle the failure appropriately and, where safe, retry the entire transaction in a fresh transaction.

Consistent lock ordering reduces the likelihood of deadlocks but doesn't eliminate every possible deadlock.

### 7.4 Monitoring PostgreSQL lock waits

To identify blocked sessions and their blockers:

```
SELECT
    blocked.pid AS blocked_pid,
    blocked.wait_event_type,
    blocked.wait_event,
    now() - blocked.query_start AS blocked_query_age,
    blocked.query AS blocked_query,
    blocker.pid AS blocker_pid,
    now() - blocker.xact_start AS blocker_transaction_age,
    blocker.query AS blocker_query
FROM pg_stat_activity AS blocked
CROSS JOIN LATERAL
    unnest(pg_blocking_pids(blocked.pid)) AS p(blocker_pid)
JOIN pg_stat_activity AS blocker
    ON blocker.pid = p.blocker_pid
ORDER BY blocker.xact_start NULLS LAST;
```

This helps identify the waiting query and the sessions blocking it.

Remember that `blocked_query_age` measures time since the query started, not necessarily the exact duration of its lock wait. Inspect the wait events and other session details when diagnosing an incident.

### 7.5 Useful timeout settings

PostgreSQL offers several settings that can help contain lock-related problems:

- `lock_timeout`: Maximum time a statement waits to acquire a lock.
- `statement_timeout`: Maximum duration of a statement.
- `idle_in_transaction_session_timeout`: Terminates sessions that remain idle while a transaction is open beyond the configured duration.

These settings serve different purposes. Choose values based on workload latency, transaction behavior, and operational requirements rather than applying arbitrary low limits.

For Redis-based coordination, monitor:

- Lock acquisition success and failure rates.
- Acquisition latency.
- Lease-renewal failures.
- Lock hold durations.
- Job duration relative to the lease.
- Failed releases and ownership mismatches.
- Recovery behavior after Redis failover.

A lock-acquisition timeout is not necessarily proof that the lock is broken. It may indicate contention, network delay, or coordinator unavailability.

## 8. Retries: When are they safe?

Retries are a common source of subtle concurrency bugs.

Suppose PostgreSQL aborts a transaction because of a deadlock. Should the application retry?

Often, yes—but it must retry the entire transaction in a fresh transaction, and the operation must be safe to repeat.

Common PostgreSQL error codes include:

| SQLSTATE | Meaning               | Typical handling                                                      |
| -------- | --------------------- | --------------------------------------------------------------------- |
| `40P01`  | Deadlock detected     | Consider a bounded retry of the entire transaction                    |
| `40001`  | Serialization failure | Retry the entire transaction when appropriate                         |
| `23505`  | Unique violation      | Usually handle as a business conflict or duplicate, not blindly retry |

### Why retrying the entire transaction matters

Suppose a transaction reads a balance, performs calculations, and updates several rows.

If the transaction fails, retrying only its final `UPDATE` may reuse decisions based on stale data.

A correct retry generally needs to:

1. Start a new transaction.
2. Read the current state again.
3. Re-evaluate business rules.
4. Repeat the complete database operation.
5. Commit only if all conditions still hold.

Use bounded retries with backoff and jitter to avoid repeatedly sending all competing requests at the same time.

### What about external side effects?

Suppose the transaction sends an email or charges a payment provider before failing.

Retrying the whole operation could send the email twice or charge the customer twice.

For external side effects, consider idempotency keys, a transactional outbox, and durable state transitions. The transaction retry strategy must account for effects that cannot be rolled back by PostgreSQL.

## 9. Final system design comparison

Let's consolidate the decisions across the examples.

| Use case                | Recommended starting point                                  | Why                                                                  |
| ----------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------- |
| Inventory decrement     | Atomic conditional SQL update                               | Enforces stock availability in the database                          |
| Seat booking            | Unique constraint and transactional reservation             | Prevents duplicate bookings                                          |
| Payment processing      | Durable state, idempotency, reconciliation                  | Handles retries and uncertain external outcomes                      |
| Cache stampede          | Request coalescing, stale-while-revalidate, or Redis lease  | Reduces duplicate expensive refreshes                                |
| PostgreSQL job queue    | `FOR UPDATE SKIP LOCKED` and durable job state              | Allows workers to claim different jobs                               |
| Leader election         | Appropriate coordination service, with fencing where needed | Coordinates ownership and handles leadership changes                 |
| Hot database row        | Atomic updates, partitioning, or queue-based serialization  | Reduces contention where possible                                    |
| User editing a document | Optimistic concurrency with a version column                | Detects stale updates without locking throughout the editing process |

There is no universally best locking technology. The right design follows from the invariant, the failure model, and the acceptable trade-offs.

## Chapter 8 summary

For production systems, remember these principles:

1. Enforce correctness at the resource that owns the state. For database invariants, start with PostgreSQL transactions, constraints, and atomic updates.
2. Don't hold locks across slow external operations unless there is a compelling reason.
3. Use idempotency for duplicate requests and retries.
4. Use queues when work needs durable tracking or controlled ordering.
5. Use distributed leases for coordination, but account for expiration and stale owners.
6. Measure lock contention and deadlocks. Correct code can still perform poorly under high contention.
7. Retry only when safe. Retry complete transactions, use bounded backoff, and protect external side effects.

## Next chapter — Chapter 9: Locking Interview Questions and Design Trade-offs

The final chapter will consolidate the course into interview-ready answers, covering:

- Pessimistic vs optimistic locking.
- Row locks vs table locks.
- PostgreSQL MVCC and isolation levels.
- Redis locks, TTLs, ownership tokens, and Redlock.
- Distributed locking, quorum, leases, and fencing tokens.
- Deadlocks, lock contention, and retry strategies.
- Real-world HLD scenarios and common interviewer follow-up questions.

Type `next` to continue.

