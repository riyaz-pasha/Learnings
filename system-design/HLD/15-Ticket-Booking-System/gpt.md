# Consolidated functional flows — detailed summaries

> 1 — Venue creation

* Client (admin) requests presigned S3 URLs for venue images.
* Client uploads images directly to S3 using presigned URLs; receives `imageKey`s.
* Client calls `POST /api/v1/venues` with `{name, address, location, imageKeys, metadata}` and `Idempotency-Key`.
* API Gateway routes to Events\&Venues service.
* Service validates auth (admin/organizer), idempotency, and request schema.
* Service inserts `venue` row (id, name, address JSON, location POINT, image\_keys JSON, metadata, created\_by, created\_at) in DB inside a transaction.
* On commit, service publishes `venue.created` event to message bus.
* Service caches venue metadata and returns `201 { venueId, createdAt }` to client.

---

> 2 — Hall creation / SeatLayout upload

* Client prepares `seatLayout` JSON (sections → rows → seats) or uploads floorplan SVG; requests presigned URL for SVG if needed.
* Client calls `POST /api/v1/venues/{venueId}/halls` with `{hallName, seatLayout}` and `Idempotency-Key`.
* Gateway → Events\&Venues service; service validates permissions, idempotency, and runs JSON Schema validation for `seatLayout`: unique section ids, unique row labels per section, unique seat labels per row, coordinates within bounds, size limits.
* Service computes `seat_count` and deterministic `seat_uid`s for seats (e.g., `<hallId>::<row>::<seat>`).
* Service begins DB transaction:

  * Insert or get `hall` row (unique per venue,name).
  * Determine `newVersion = current_layout_version + 1`.
  * Insert `hall_layout (hall_id, version, layout_json, seat_count)`.
  * Bulk insert flattened `hall_seat` rows for that layout version (each with seat\_uid, section\_id, row\_label, seat\_label, x,y,attributes). Enforce uniqueness constraints; on any collision abort transaction and return validation error that pinpoints the seat.
  * Update `hall.current_layout_version = newVersion`.
* Commit transaction.
* After commit, publish `hall.layout.versioned` event with `{hallId, layoutVersion, seatCount}`.
* Cache layout JSON in Redis and optionally push layout to CDN (pre-generate compressed JSON/SVG).
* Return `201 { hallId, layoutVersion, createdAt }`.

---

> 3 — Event creation (master event record)

* Client calls `POST /api/v1/events` with `{name, slug, description, imageKey, metadata, organizerId}` and `Idempotency-Key`.
* API Gateway → Events\&Venues service. Validate auth (organizer/admin), idempotency, and schema.
* Service inserts `event` record in DB (id, name, slug, description, image\_key, metadata, created\_at).
* On commit, publish `event.created` event (`eventId`).
* Return `201 { eventId, createdAt }`.

---

> 4 — Show creation (attaching venues/halls to an event)

* Client uses resourceful endpoint `POST /api/v1/events/{eventId}/shows` with one or many `shows` entries: each `{venueId, hallId, startsAt, endsAt, priceTiers, metadata, capacityOverride}`.
* Gateway → Events\&Venues service; service validates: event exists, caller permission, venue/hall exist and belong, time zones & ISO datetimes, `startsAt < endsAt`.
* Service checks for conflicting shows in same hall (overlap detection); if conflict and not allowed, reject with `409` and conflict details.
* Service begins transaction:

  * Insert `show` rows (`showId, eventId, venueId, hallId, startsAt, endsAt, status=SCHEDULED, metadata`).
* Commit and return `202 { jobId, showsCreated }` if seat creation is queued async; or `201` with show details if processed synchronously for small halls.
* After DB commit, publish `show.created` event with `{showId, eventId, venueId, hallId, layoutVersion, priceTiers, metadata}`.

---

> 5 — ShowSeat (event\_seat) creation (SeatManager/ShowtimeInventory worker)

* SeatManager consumes `show.created` event. The event includes `showId`, `hallId`, `layoutVersion`, `priceTiers`, and metadata.
* Worker performs idempotency check: if `show_seat` rows already exist for `showId`, skip or reconcile. Optionally maintain `show_seat_job` with state (PENDING→RUNNING→DONE/FAILED).
* Worker fetches `hall_layout` JSON and flattened `hall_seat` rows for `hallId` and `layoutVersion`.
* For each `hall_seat`, worker resolves `price_tier_id` & `price_cents` using `priceTiers` selectors (selectors may be by section, seat label regex, coordinate ranges, or explicit seat lists). Apply first-match rule and default tier fallback. Apply blocked seats / admin overrides / capacityOverrides.
* Worker prepares `show_seat` records: `(show_id, event_id, venue_id, hall_id, hall_seat_id, seat_uid, section_id, row_label, seat_label, x,y, price_cents, price_tier_id, category, state=AVAILABLE|BLOCKED, metadata)`.
* Insert `show_seat` rows in batches (e.g., 500–2000 rows per batch) using `INSERT ... ON CONFLICT DO NOTHING` or upsert to remain idempotent. Use batching and parallelization per-section if needed.
* After all batches succeed, mark job DONE and publish `show.seats.created {showId, seatCount}`. If partial/failure, mark FAILED and allow retry with backoff.
* Reconciliation step ensures expected seat count matches actual inserted; trigger alerts if mismatch.

---

> 6 — Indexing (Search indexer flow)

* Indexer consumes domain events `event.created`, `show.created`, `show.updated`, `show.cancelled`, `hall.layout.versioned`.
* For `show` events, indexer transforms DB model to ES document structure: `{showId, eventId, venueId, hallId, title, description, startsAt, endsAt, location:geo_point, city, priceMin, priceMax, status, popularity, createdAt}`. Document id = `show_{showId}` to guarantee idempotency.
* Indexer issues bulk upserts to Elasticsearch. On index failure, retry with backoff; log indexer lag.
* ES mapping includes `geo_point`, date types, keywords & analyzers for text. Refresh interval and index settings tuned for near-real-time.
* On show cancel/update, update ES doc (soft-delete or status update).

---

> 7 — Discover & Search flow (user view events)

* Client requests nearby shows: `GET /api/v1/discover/nearby?lat&lon&radiusKm&start&end&limit&filters`. Or user searches: `GET /api/v1/search?q=...&filters...`.
* Gateway routes to Search Service. Search Service validates params and checks a local cache (Redis) for popular queries. If cache miss, Search Service queries Elasticsearch with geo\_distance + time range + filters using `bool` queries, `multi_match` for text, and `function_score` for ranking (proximity, time decay, popularity). Use `search_after` for cursor pagination.
* Search Service returns `items[]` with show summary (showId, eventId, venue summary, startsAt, distanceKm, priceMin/Max, posterUrl, availabilityHint). For detailed availability, client will call Booking service or `GET /shows/{showId}`.
* Search Service subscribes to indexer events or relies on indexer to keep ES near real-time. Cache results in Redis with TTL for repeated queries; serve CDN-hosted images and seatmap JSON/SVG.

---

> 8 — Get seatmap / authoritative availability

* Client requests `GET /api/v1/shows/{showId}/seatmap` to render seats.
* API Gateway → SeatService / Booking service (or SeatManager read endpoint). Service fetches `hall_layout` JSON (cached in Redis/CDN) and `show_seat` states from DB or from a cached availability snapshot. Combine layout + per-seat authoritative `state` (AVAILABLE/HELD/BOOKED/BLOCKED) and `price_cents`. Optionally include `availabilityToken` for quick server-side verification during hold creation.
* Return seatmap JSON (layout overlay + seat state + seat metadata + price) to client.

---

> 9 — Select seats → Create hold (atomic hold)

* Client selects seats and calls `POST /api/v1/shows/{showId}/holds` with `{userId, seatUids[], holdTtlSecs}` and `Idempotency-Key`.
* Gateway → Booking service → run Redis atomic hold Lua script:

  * Inputs: `showId`, `holdId`, `userId`, `ttl`, `seatUids`.
  * For each seat check: absence of `seat:lock:{showId}:{seatUid}` and absence of `seat:booked:{showId}:{seatUid}`.
  * If any conflict, return `409` with list of conflicting seatUids.
  * If no conflicts, set `seat:lock:{showId}:{seatUid} = {holdId,userId}` with TTL and create Redis set `hold:{holdId}` containing seatUids and `hold:{holdId}:meta` with `{userId, showId, amount, expiresAt}`.
* Booking service computes final price using `show_seat.price_cents` + taxes/fees + offers (call Pricing service). Update `hold:{holdId}:meta` with computed amount.
* Return `201 {holdId, expiresAt, seatsHeld[], totalAmountCents}` to client.

---

> 10 — Payment initiation & client payment flow

* Client requests payment initiation: `POST /api/v1/holds/{holdId}/payment-intent`. Booking service validates hold exists and belongs to user, reads amount from `hold:{holdId}:meta`.
* Booking service calls Payment Service / PSP (Stripe/Adyen/Paypal) to create a payment intent/session with amount, currency, and `metadata` that includes `holdId` and `userId`. Store PSP `paymentIntentId` and return `clientSecret` (or PSP redirect URL) to client.
* Client mounts PSP SDK and completes payment. For methods requiring redirection/3DS, handle the redirect flow; optionally extend hold TTL once at payment initiation.

---

> 11 — Payment webhook → confirm booking (idempotent confirm)

* PSP sends signed webhook `payment_intent.succeeded` with `paymentIntentId` and metadata `{holdId, userId}`.
* Booking service webhook handler verifies signature, ensures event not processed before (store PSP `eventId`).
* Booking service calls internal `POST /api/v1/holds/{holdId}/confirm` (idempotent) or runs confirm logic inline:

  * Validate hold exists and belongs to same user. Optionally ensure hold TTL has not expired or allow confirm if within a short grace window as defined by policy.
  * Begin DB transaction:

    * Load `show_seat` rows for seat\_uids `FOR UPDATE` or use optimistic `WHERE state IN ('AVAILABLE','HELD')` with version checks.
    * Verify none are already `BOOKED`. If any are booked: abort transaction, mark booking failed and trigger refund flow.
    * Update `show_seat` rows to `BOOKED`, set `booked_by=userId`, `booking_id=bkId`, increment `version`.
    * Insert `booking` record (bookingId, userId, showId, amount, paymentRef, status=CONFIRMED) and insert `ticket` rows (ticketId, bookingId, seatUid, QR/token).
  * Commit transaction.
  * Delete Redis `seat:lock` keys for those seat\_uids and delete `hold:{holdId}` metadata atomically.
  * Publish `booking.created` and `seat.booked` events.
  * Return booking details to client (and notify user via email/SMS).

---

> 12 — Hold expiry, release, and cleanup

* Redis auto-expires `seat:lock` keys at TTL expiry. Use keyspace notifications or a background reaper to detect expired holds.
* On hold expiry: publish `hold.expired {holdId, showId, seatUids}`; Booking service / SeatManager clears `hold:{holdId}` metadata and publishes `seat.released` events for UI refresh.
* Periodic reaper reconciles Redis vs DB state to avoid stale locks after Redis restarts: ensure no `seat:lock` exists for seats already `BOOKED` in DB; remove any leftover locks and notify.

---

> 13 — Cancellations, refunds & seat release after booking cancellation

* Booking cancellation initiated by user or admin triggers DB update: mark booking `CANCELLED` and tickets `REFUNDED/PENDING_REFUND`. Publish `booking.cancelled`.
* On refund completion (or policy-defined immediate release), update `show_seat` rows to `AVAILABLE` (or `BLOCKED` if admin). Publish `seat.released` events. If seats are reallocated, adjust analytics and notify interested users if required.

---

> 14 — Edge & consistency rules summarized in flows

* All-or-nothing semantics for multi-seat holds and confirms: atomic holds via Redis script; confirm transactionally books all seats or fails and triggers refund.
* Idempotency keys used for create actions (`venues`, `halls`, `events`, `shows`, `holds`, payment webhooks) to prevent duplicates.
* Async flows (show seat creation, indexing) are treated as eventual consistency; UI should show loading/queued state until `show.ready`.
* Price determination always authoritative from `show_seat.price_cents` and Pricing service; discounts validated at hold/payment time.
* Timezones: clients send ISO 8601 with timezone; system stores UTC and displays localized times on UI.
* Security: only authorized roles can create/modify venues/halls/events; holds tied to authenticated users; PSP webhooks verified and processed idempotently.

---

> 15 — Events & integrations in flows (message bus events used)

* `venue.created` — consumers: analytics, search indexer.
* `hall.layout.versioned` — consumers: seat indexer, CDN generation, Search updates.
* `event.created` — consumers: search indexer, notification.
* `show.created` — consumers: SeatManager (show\_seat creation), indexer.
* `show.seats.created` — consumers: Booking service (enable bookings), search indexer (update price range/availability hint).
* `booking.created` / `seat.booked` — consumers: Notification service, Analytics, Fraud service.
* `hold.expired` / `seat.released` — consumers: UI push service, cache invalidation.


---
---
---
---

# Venue & Hall (SeatLayout) — concise design

## 1. Overview (what and who)

* **Purpose:** Admins create **Venues** (cinemas, auditoria) and add **Halls / Screens** inside a venue with a seat layout.
* **Flow:** Client → **API Gateway** → **Events & Venues Service** → DB / S3 / background workers.
* **Storage:** Venue & Hall metadata in RDBMS; images & static assets in S3; derived/fast-read copies in Redis/CDN.

---

## 2. APIs (surface)

### Create Venue

**POST** `/api/v1/venues`
Headers: `Authorization: Bearer <admin-token>`, `Idempotency-Key: <uuid>` (recommended)

**Request body**

```json
{
  "name":"PVR Banjara Hills",
  "address": {
    "street":"Road X",
    "city":"Hyderabad",
    "district":"Hyderabad",
    "state":"Telangana",
    "pin":"500034",
    "country":"IN"
  },
  "location": { "lat": 17.4100, "lon": 78.4200 },
  "imageKeys": ["venues/venue-123/poster1.jpg"]
}
```

**Response (201)**

```json
{ "venueId":"venue_9a1f...", "createdAt":"2025-09-09T19:12:34Z" }
```

Errors: `400` validation, `401` unauthorized, `409` idempotency duplicate.

---

### Create Hall (with SeatLayout)

**POST** `/api/v1/venues/{venueId}/halls`
Headers: `Authorization`, `Idempotency-Key`

**Request body**

```json
{
  "hallName":"Hall 1 (IMAX)",
  "seatLayout": {
    "version": 1,
    "bounds": {"width":1000, "height":600},
    "sections":[
      {
        "id":"sec-stalls",
        "name":"Stalls",
        "rows":[
          {
            "label":"A",
            "seats":[
              {"id":"s-A1","label":"A1","coordinates":{"x":10,"y":20},"metadata":{"type":"regular"}},
              {"id":"s-A2","label":"A2","coordinates":{"x":20,"y":20},"metadata":{"type":"regular"}}
            ]
          }
        ]
      }
    ]
  }
}
```

**Response (201)**

```json
{ "hallId":"hall_4b2c...", "layoutVersion":1, "createdAt":"2025-09-09T19:15:01Z" }
```

Errors: `400` invalid layout, `404` venue not found, `422` seat collisions, `409` duplicate hall name.

---

## 3. Low-level storage model (simplified)

**Venue table**

```
venue (id, name, address JSON, location POINT, image_keys JSON, created_at, updated_at)
```

**Hall table**

```
hall (id, venue_id, name, current_layout_version, metadata JSON, created_at)
```

**Hall layout table (versioned)**

```
hall_layout (id, hall_id, version, layout_json JSON, seat_count, created_at)
```

**Flattened seats (for fast lookups)**

```
hall_seat (id, hall_id, layout_version, section_id, row_label, seat_label,
           seat_uid, x, y, attributes JSON)
```

Notes:

* `seat_uid` can be deterministic: `<hallId>::<row>::<seat>`.
* Use PostGIS `POINT` for geo queries if needed.

---

## 4. SeatLayout structure & validation (simple rules)

* `seatLayout` contains `sections[]` → each `section` has `rows[]` → each `row` has `seats[]`.
* Required: unique `section.id`, unique `row.label` within a section, unique `seat.label` within a row.
* Coordinates must be integers and inside `bounds` (0 ≤ x ≤ width).
* Enforce limits (configurable): e.g., max sections 50, max rows/section 200, max seats/row 200.
* Service will compute `seat_count` and reject empty/oversized layouts.
* If `seat.id` absent, service generates deterministic seat ids.

---

## 5. Image upload (S3 presigned flow)

1. Client requests presigned URL from `/api/v1/uploads/presign` with `keyPrefix` and `contentType`.
2. Client PUTs image directly to S3 using the presigned URL.
3. Client uses returned `imageKey` in `/venues` request.
4. Server validates keys and optionally triggers thumbnail generation (async).

Benefits: avoids large payloads through the API service.

---

## 6. Transactional creation & events

* Create hall + layout is one DB transaction:

  * Insert `hall` (or fetch existing), insert `hall_layout` version, bulk insert flattened `hall_seat`, update `hall.current_layout_version`.
  * Commit transaction.
* After commit, publish domain event (e.g., `hall.layout.versioned`) to message bus so downstream services (Search, SeatManager) can index or prepare per-event seats.

---

## 7. Caching & fast reads

* Cache `hall_layout` JSON in Redis with key `hall:layout:{hallId}:{version}`.
* Serve static pre-generated JSON or SVG via CDN for client to download quickly.
* Invalidate cache when a new layout version is created.

---

## 8. Authorization, idempotency & auditing

* Only `admin`/`organizer` roles allowed to create venues/halls.
* Use `Idempotency-Key` to prevent duplicate creation on retries.
* Store `created_by`, `created_at` and keep audit logs for layout changes.

---

## 9. Flattening algorithm (brief)

* Convert nested `seatLayout` → list of seat rows to insert into `hall_seat`:

  * For each section → each row → each seat:

    * create `seat_uid = hallId + "::" + row_label + "::" + seat_label`
    * validate uniqueness; collect to batch insert.
* If any collision or schema violation — abort and return detailed error pointing the problem seat(s).

---

## 10. Additional simple operational notes

* Validate S3 `imageKeys` belong to your bucket and correct prefix.
* Limit JSON payload size (e.g., 2–5 MB) for seatLayout uploads.
* Provide clear error messages: which seat / row / section failed validation.
* Provide UI-friendly warnings for overlapping coordinates or empty rows.
* After layout creation, background jobs:

  * Index seats to search,
  * Create event-seat templates when a new show is created,
  * Generate thumbnails / SVGs for CDN.

---

## 11. Example errors to return (short)

* `400` — `"error": "seatLayout.row.A.seat.A2: duplicate seat label"`
* `422` — `"error": "seat count exceeds limit (max 10,000)"`
* `404` — `"error": "venue not found"`
* `401` — `"error": "unauthorized"`

---
---

# Event creation — corrected, complete, simple flow

Yes — your idea is basically correct. Below I’ve added missing details and polished the final flow so it’s safe, production-ready and easy to implement. I kept it simple and low-level.

---

# 1) Key goals & constraints (short)

* Admin/organizer creates an **Event** (movie, concert, play).
* Admin attaches one or more **venues/halls** to that event with specific **show times** (may be single-show or recurring).
* For each venue-show we must prepare per-event **seat instances** (availability rows) and price tiers.
* Keep operations idempotent, permission-checked, and eventual-consistent where necessary (long-running tasks are async).

---

# 2) Recommended endpoints (cleaned up)

### Create event

**POST** `/api/v1/events`
Headers: `Authorization: Bearer <token>`, `Idempotency-Key: <uuid>`

Request body (example):

```json
{
  "name":"Avengers — Night Premiere",
  "slug":"avengers-night-premiere",
  "description":"Premiere show",
  "imageKey":"events/avengers/poster1.jpg",
  "metadata": { "language": "EN", "durationMin": 150 },
  "organizerId": "org_123"   // optional, inferred from auth if omitted
}
```

Success (201):

```json
{ "eventId":"ev_abc123", "createdAt":"2025-09-09T20:00:00Z" }
```

---

### Attach venues / create shows (preferred resourceful API)

**POST** `/api/v1/events/{eventId}/shows`
(use POST instead of PUT /events/venues — resource is a show)

Request body (one or many shows):

```json
{
  "shows": [
    {
      "venueId":"venue_1",
      "hallId":"hall_1",
      "startsAt":"2025-10-01T19:30:00+05:30",   // ISO 8601 with timezone
      "endsAt":"2025-10-01T22:00:00+05:30",     // optional (can be computed from duration)
      "priceTiers":[
         { "name":"VIP", "priceCents":1500, "seatSelector": {"sections":["VIP"]} },
         { "name":"Standard", "priceCents":700, "seatSelector":{"sections":["Stalls"]} }
      ],
      "capacityOverride": null, // optional to block seats for offline sale
      "metadata": {"screenType":"IMAX"}
    },
    { /* another show */ }
  ]
}
```

Response (202 Accepted or 201 for sync):

```json
{
  "jobId":"job_789",
  "showsCreated":[
    {"showId":"show_1","venueId":"venue_1","hallId":"hall_1","startsAt":"..."}
  ],
  "message":"Seat instance creation queued"
}
```

Notes:

* We return `202 Accepted` if per-show seat instance creation is done asynchronously (recommended for large halls).
* If fast (small hall), service may create show and seat instances synchronously and return `201`.

---

# 3) DB schema additions (simple)

```
event (id, name, slug, description, image_key, organizer_id, metadata, created_at)

show (id, event_id, venue_id, hall_id, starts_at timestamptz, ends_at timestamptz,
      status ENUM('SCHEDULED','CANCELLED','COMPLETED'), metadata, created_at)

show_price_tier (id, show_id, name, price_cents, selector_json)

event_seat (id, show_id, hall_seat_id, seat_uid, price_tier_id, state ENUM('AVAILABLE','HELD','BOOKED','BLOCKED'), created_at, version)
```

* `show` represents a specific screening/performace (your `EventVenues`).
* `event_seat` is created per `show` from `hall_seat` flattening.

---

# 4) Validation & rules (simple)

* Validate `eventId` exists and caller has permission to attach shows.
* Validate `venueId` and `hallId` exist and hall belongs to venue.
* Validate `startsAt`/`endsAt` are ISO datetimes and `startsAt < endsAt`. Use timezone-aware times (store in UTC in DB).
* Check for **conflicting shows** in the same hall (overlaps) unless organizer explicitly allows overlaps. Return `409` with conflict info.
* `priceTiers`: validate seat selectors (e.g., section names exist in the hall layout). If invalid, return `400` and show which selector failed.

---

# 5) Seat instance creation (important low-level flow)

When you attach a show to a hall you must create `event_seat` rows (per-seat availability for that show). Options:

**A. Async (recommended for scale)** — pattern:

1. Create `show` row in DB inside a quick transaction.
2. Publish `show.created` event to message bus with `{showId, hallId, layoutVersion, priceTiers}`.
3. A background worker (SeatManager) consumes the event and:

   * Reads flattened `hall_seat` for `hallId` + `layoutVersion`.
   * For each seat create `event_seat` with `state=AVAILABLE` and price\_tier based on `seatSelector` rules.
   * Bulk insert `event_seat` rows (batched).
   * Publish `show.ready` event when done.

Return to client: `202` with `jobId` that can be polled for status.

**B. Sync (small halls)**:

* Within the same request transaction, read `hall_seat` and bulk-insert `event_seat`. Return `201`. Be careful with long transactions—timeouts.

---

# 6) Idempotency & retries (simple)

* Use `Idempotency-Key` to avoid duplicate event/show creation on client retries. Store result mapping (`key -> createdEventId`) for a TTL.
* Background SeatManager jobs must be idempotent—use `showId` as the idempotency key for seat creation (skip if `event_seat` already exist for that show).

---

# 7) Domain events (light)

Emit these events (at-least-once, after DB commit):

* `event.created` `{eventId, organizerId}`
* `show.created` `{showId, eventId, hallId, startsAt}`
* `show.seats.created` `{showId, seatCount}`
* `show.cancelled` when show removed.

Consumers: Search indexer, Notification service (reminders), Pricing service, Analytics.

---

# 8) Timezone & recurrence notes (short)

* Always accept `startsAt`/`endsAt` as ISO 8601 strings with timezone (e.g., `2025-10-01T19:30:00+05:30`). Convert and store in DB as UTC.
* For recurring shows (daily/weekly), accept recurrence rules: `{recurrence: {freq:"DAILY", count:7}}` and create multiple `show` rows (or schedule them in a job). Validate conflicts for each generated show.

---

# 9) Example final flow (step-by-step)

1. **Client**: `POST /api/v1/events` with event metadata.
   → Service validates, inserts `event` row, returns `eventId`.

2. **Client**: `POST /api/v1/events/{eventId}/shows` with one or more shows (venueId, hallId, startsAt, priceTiers).
   → Service validates: event exists, venue/hall exists, no conflicts.
   → Service inserts `show` rows (quick DB transaction).
   → Service publishes `show.created` events and returns `202 { jobId, showsCreated }`.

3. **SeatManager (background worker)** consumes `show.created`:

   * Loads `hall.current_layout_version` and `hall_seat` rows.
   * Maps seats to price tiers using `seatSelector` rules.
   * Bulk-inserts `event_seat` rows (`state=AVAILABLE`).
   * Publishes `show.seats.created` when done.

4. **Client** polls job endpoint or listens to websocket for `show.ready`. Once ready the show is available for booking.

5. **Booking service** queries `GET /api/v1/shows/{showId}/seatmap` (reads cached `show` seat map or queries `event_seat` table) and users can begin holds/bookings.

---

# 10) Error examples (simple)

* `400` — invalid datetime format or price tier selector not matching layout.
* `404` — event/venue/hall not found.
* `409` — show time conflict in same hall.
* `422` — seat creation failed (seat count mismatch).
* `202` — accepted; seat creation queued.

---

# 11) Minimal pseudocode (handler)

```pseudo
POST /events/{eventId}/shows handler:
  auth = extractUser()
  assert auth.hasPermission('create_show', eventId)

  validateRequestBody(req)
  for each showRequest in req.shows:
    assert venueExists(showRequest.venueId)
    assert hallExists(showRequest.hallId)
    assert !overlapsExistingShows(hallId, showRequest.startsAt, showRequest.endsAt)

  begin transaction
    for showRequest in req.shows:
      showId = insert_show(eventId, venueId, hallId, startsAt, endsAt, metadata)
      record createdShowIds
  commit

  for showId in createdShowIds:
    produceKafka("show.created", {showId, hallId, layoutVersion, priceTiers})

  return 202 { jobId, showsCreated: createdShowIds }
```

---
---

That's a great starting point for thinking about how to manage events and their showtimes. Your proposed flow correctly identifies the need to separate event metadata from its venue associations. However, to make this design more scalable and robust for real-world scenarios, we need to add a crucial concept: **Showtimes**.

Let's rephrase and add the missing low-level details to create a more resilient and functional system.

-----

### Detailed Analysis of the Proposed Flow

Your initial design is a solid foundation, but the `EventVenues` table has a key limitation. It attempts to store a specific showing using just `date` and `time` and doesn't handle multiple daily showings well. For a single event that runs for several days, this model would require a new record for every single date and time combination. A better approach is to introduce a dedicated **`Showtime`** entity.

The `PUT /api/v1/events/venues` endpoint is also a bit ambiguous in its action. A more precise and RESTful approach would be to `POST` to a dedicated resource for creating showtimes.

-----

### Corrected and Finalized Flow

Here is a corrected, detailed flow that separates event creation from the management of specific showtimes, making the system more flexible and scalable.

#### Step 1: Event Creation - `POST /api/v1/events`

This endpoint's purpose is to create the core **event record**, which includes general metadata that remains constant across all showings.

**Request Flow:**

1.  An admin sends a `POST` request to the API Gateway with the event's high-level details.
2.  The request is routed to the Events & Venues service.
3.  The service validates the request payload and generates a unique **`eventId`** (typically a UUID).
4.  A new record is inserted into the **`Events` table**. This table acts as the master list of all events.

**Database Schema: `Events` Table**

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `id` | `VARCHAR(36)` | Unique identifier (UUID). Primary Key. |
| `name` | `VARCHAR(255)` | Name of the event. |
| `imageUrl` | `VARCHAR(255)` | S3 key for the event's promotional image. |
| `description` | `TEXT` | Detailed event description. |
| `category` | `VARCHAR(50)` | Event category (e.g., 'Movie', 'Concert'). |
| `duration` | `INT` | Event length in minutes. |
| `metadata` | `JSONB` | Additional flexible data. |

\<br\>

#### Step 2: Showtime Creation - `POST /api/v1/events/{eventId}/showtimes`

This new, dedicated endpoint is the crucial next step. It handles the creation of a specific showing of an event at a particular venue and hall. A **Showtime** is a unique instance of an event on a specific date and time.

**Request Flow:**

1.  An admin sends a `POST` request to the API Gateway, specifying the `eventId` in the URL and the showtime details in the body.
2.  The Events & Venues service receives the request and validates that the `eventId` and `hallId` exist in their respective tables.
3.  A unique **`showtimeId`** is generated.
4.  A new record is added to the **`Showtimes` table**, which links the event to a specific hall, and most importantly, provides a precise `timestamp`.

**Database Schema: `Showtimes` Table**

| Column | Data Type | Description |
| :--- | :--- | :--- |
| `id` | `VARCHAR(36)` | Unique identifier (UUID). Primary Key. |
| `eventId` | `VARCHAR(36)` | Foreign Key to the `Events` table. |
| `hallId` | `VARCHAR(36)` | Foreign Key to the `Halls` table. |
| `venueId` | `VARCHAR(36)` | Foreign Key to the `Venues` table. |
| `showtime` | `TIMESTAMP` | The precise date and time of the event showing. |
| `availableSeats` | `INT` | The total number of available seats (initial count). |
| `pricePerSeat` | `DECIMAL` | The price for a single seat. |

\<br\>

This revised flow and data model are more aligned with the principles of microservice architecture and ensure data consistency. With this structure, you can easily add multiple showtimes for the same event in different halls or on different days without creating redundant event records.

Now that we have a solid plan for events and showtimes, would you like to move on to designing the **Booking & Inventory Service**, which will use this information to manage seat availability?

---

# ShowSeat creation — corrected, simple, production-ready flow

Short summary: when a **show** is created (and published), a background worker (SeatManager / ShowtimeInventory) consumes the `show.created` (or `show.published`) event, reads the hall’s latest layout, maps seats to price tiers & attributes, and creates **ShowSeat** (a.k.a. `event_seat`) rows for that show. Make the worker **idempotent**, **batched**, and **observable**. Below are the missing details and a clear end-to-end flow.

---

## 1) Trigger & high-level requirements

* Trigger: `show.created` (or `show.published`) event with `{ showId, eventId, hallId, layoutVersion, priceTiers, metadata }`.
* Worker: `SeatManager` (consumer) creates a `ShowSeat` record for **every seat** in the hall layout for that show.
* Initial state: all seats default to `AVAILABLE` unless seat-level rules say otherwise (blocked, reserved).
* Idempotency: worker must be safe to re-run for same `showId` (no duplicates).

---

## 2) ShowSeat (event\_seat) model (simple)

Fields to store for each seat instance:

```
show_seat (
  id UUID PRIMARY KEY,
  show_id UUID,         -- references show
  event_id UUID,        -- parent event
  venue_id UUID,
  hall_id UUID,
  hall_seat_id UUID,    -- reference to flattened hall_seat
  seat_uid TEXT,        -- deterministic key: "<hallId>::<row>::<seat>"
  section_id TEXT,
  row_label TEXT,
  seat_label TEXT,
  x INT, y INT,         -- coordinates for client rendering
  price_cents BIGINT,
  price_tier_id UUID,   -- price tier applied to this seat
  category TEXT,        -- VIP, Regular, Accessible etc
  state TEXT,           -- AVAILABLE, HELD, BOOKED, BLOCKED
  metadata JSONB,
  created_at timestamptz,
  updated_at timestamptz,
  UNIQUE (show_id, seat_uid)
);
```

Indexes:

* `(show_id, state)` for availability scans
* `(show_id, seat_uid)` unique
* `(show_id, price_tier_id)` for reporting

---

## 3) Input event (example)

Worker consumes something like:

```json
{
  "type": "show.created",
  "payload": {
    "showId": "show_123",
    "eventId": "ev_abc",
    "venueId": "venue_1",
    "hallId": "hall_1",
    "layoutVersion": 3,
    "priceTiers": [
      { "id":"pt_vip", "name":"VIP", "priceCents":15000, "selector": {"sections":["VIP"]} },
      { "id":"pt_std", "name":"Standard", "priceCents":7000, "selector": {"sections":["Stalls"]} }
    ],
    "metadata": { "screenType":"IMAX" }
  },
  "meta": { "traceId":"t-456" }
}
```

---

## 4) Seat → price mapping rules (simple options)

`priceTiers` should include a **selector** rule to map hall seats to tiers. Simple selector DSL examples:

* by section: `{ "sections": ["VIP"] }`
* by seat label regex: `{ "seatLabelRegex": "^A[0-9]+" }`
* by coordinates/bounds: `{ "xRange":[0,400], "yRange":[0,200] }`
* by explicit seatUid list: `{ "seatUids": ["hall1::A::A1", ...] }`

Worker applies selectors in order (first-match wins). Provide a fallback `defaultPriceTier` if no selector matches.

---

## 5) End-to-end worker flow (step-by-step)

1. **Receive event** `show.created` with `showId`.
2. **Idempotency check**: if `show_seat` rows already present for `showId` (e.g., `SELECT 1 FROM show_seat WHERE show_id = ? LIMIT 1`), skip or reconcile.

   * Option: keep a `show_seat_job` table with `show_id, status (PENDING,RUNNING,DONE,FAILED), attempt_count, last_error`.
3. **Read hall layout**: fetch `hall_layout` JSON + flattened `hall_seat` rows for `hallId` & `layoutVersion`.
4. **Compute mapping**: for each hall seat, determine `price_tier_id`, `price_cents`, `category` based on selectors; apply any `capacityOverride` or admin `blockedSeats`.
5. **Batch insert** `show_seat` rows:

   * Insert in batches (e.g., 500–2000 rows per batch).
   * Use `INSERT ... ON CONFLICT (show_id, seat_uid) DO NOTHING` (or DO UPDATE to refresh metadata) to remain idempotent.
6. **Mark job DONE** and publish `show.seats.created` `{ showId, seatCount }`.
7. **If any failure**: mark job FAILED and emit error with details; the consumer can retry safely.

---

## 6) Idempotency and partial failure handling

* Use `show_id` as idempotency key. Worker must be able to detect partial inserts and continue. Strategies:

  * Maintain `show_seat_job` row with `status` and `last_processed_index`.
  * Use upserts `ON CONFLICT DO NOTHING` so retries won’t create duplicates.
  * When a batch fails, retry batch with exponential backoff. If fatal, mark job FAILED and notify ops.

---

## 7) Performance & scaling recommendations

* **Batch inserts** (500–2000 rows) to avoid transaction timeouts.
* **Parallelize per-section**: create seat batches per section in parallel threads to speed up large halls, but respect DB write throughput.
* **Backpressure**: limit parallel workers for a single DB shard to avoid overload.
* **Partitioning**: if you have many shows, partition `show_seat` by `show_id % N` or use `show_id` as partition key in distributed DB.
* **Async creation** (recommended): always create show seats in background and return `202` to UI.

---

## 8) Handling special seat states & admin blocks

* If hall layout had `blocked` seats (for sightline, staff, maintenance), carry that to `show_seat.state = BLOCKED`.
* If `capacityOverride` or box-office reserved seats are present, mark those seats as `BLOCKED` or `RESERVED` with metadata indicating the reason.
* For social distancing rules (auto-block neighbors), generate blocks as part of mapping step.

---

## 9) Reconciliation & consistency checks

* After job completion, run quick checks:

  * expected seat count vs actual inserted.
  * no duplicate seat\_uid.
  * every `show_seat.hall_seat_id` present.
* If mismatch, publish an alert and schedule reconciliation job:

  * delete partial and re-create OR add missing rows idempotently.

---

## 10) Eventing & downstream

* On success publish: `show.seats.created` `{ showId, seatCount, layoutVersion, generatedAt }`.
* Downstream consumers: Booking service (can enable bookings), Search indexer (index seats/prices), Notification service (notify subscribers of ticket availability).
* Ensure **at-least-once** semantics for events and clients handle idempotency.

---

## 11) Observability & metrics (keep it small)

Track:

* `show_seat_jobs_started`, `show_seat_jobs_succeeded`, `show_seat_jobs_failed`
* `show_seat_rows_inserted_total`
* batch insert latency, DB errors (constraint violations)
* number of retries per show job

Log `traceId` from event metadata for tracing.

---

## 12) Example worker pseudocode (compact)

```pseudo
function handleShowCreated(event):
  payload = event.payload
  showId = payload.showId

  if existsShowSeatRows(showId):
    log("already created", showId); return

  markJobRunning(showId)
  hallSeats = fetchHallSeats(payload.hallId, payload.layoutVersion)   // flattened hall_seat rows

  batches = chunk(hallSeats, BATCH_SIZE)
  totalInserted = 0
  for batch in batches:
    rows = []
    for hs in batch:
      tier = mapSeatToPriceTier(hs, payload.priceTiers)
      rows.append({
        show_id: showId, seat_uid: hs.seat_uid, hall_seat_id: hs.id,
        section_id: hs.section_id, row_label: hs.row_label, seat_label: hs.seat_label,
        x: hs.x, y: hs.y, price_cents: tier.priceCents, price_tier_id: tier.id,
        category: hs.attributes.type or tier.name,
        state: hs.attributes.blocked ? 'BLOCKED' : 'AVAILABLE',
        metadata: hs.attributes
      })
    result = batchInsertShowSeats(rows) // INSERT ... ON CONFLICT DO NOTHING
    totalInserted += result.insertedCount
  if totalInserted == len(hallSeats) or partial allowed:
    markJobDone(showId)
    publish("show.seats.created", {showId, seatCount: totalInserted})
  else:
    markJobFailed(showId, details)
```

---

## 13) Edge cases & notes (short)

* **Layout version drift**: create seats for the `layoutVersion` provided in event. If layout later changes, you must decide whether existing shows keep their seats or are migrated (usually shows keep original layout).
* **Show cancellation**: on `show.cancelled`, mark all related `show_seat` states as `BLOCKED/CANCELLED` and notify downstream.
* **Price changes**: once `show_seat` created, price changes should update `show_seat.price_cents` (use `ON CONFLICT` updates) and publish `show.price.updated` event.
* **Very large halls**: stream batches to DB and avoid single huge transactions.

---
---

Nice — I’ll tighten this up and add the missing low-level details so the **View Events / Search** experience is production-ready, simple to implement, and bulletproof for mobile/web clients. I’ll cover endpoints, the search flow, Elasticsearch mappings & example queries, indexing pipeline, caching, privacy/fallbacks, ranking & filters, and operational notes.

---

# View Events — final flow & low-level details

## 1) User experience summary

* When a user opens the app/home screen we ask for **location permission** (coordinates) or allow entering **city/name**.
* We show a nearby events feed (shows happening close to the user/time window) and offer search (events, venues, shows).
* Results should be fast (<200ms typical), paginated, and reflect recent creations/updates (near realtime).

---

## 2) Public APIs (examples)

### Home feed — nearby shows

**GET** `/api/v1/discover/nearby?lat=17.41&lon=78.42&radiusKm=10&start=2025-10-01T00:00Z&end=2025-10-07T23:59Z&pageCursor=<cursor>&limit=20&categories=movie,concert`

Response:

```json
{
  "items":[
    { "showId":"show_1","eventId":"ev_1","venueId":"venue_1","hallId":"hall_1",
      "startsAt":"2025-10-01T19:30:00Z","distanceKm":1.2,
      "title":"Avengers — Night Premiere","posterUrl":"...","priceMin":7000,"priceMax":15000
    }
  ],
  "nextCursor":"eyJvZmZzZXQiOjIw}",
  "totalEstimate": 123
}
```

### Search (text + filters)

**GET** `/api/v1/search?q=avengers&lat=...&radiusKm=50&filters=language:EN,category:movie&sort=relevance|time|distance&page=1&limit=20`

Response similar to above with relevance scores.

### Show detail

**GET** `/api/v1/shows/{showId}` — returns show + venue + seatmap URL + availability hint.

---

## 3) Where the request lands

* Requests go to **API Gateway** → **Search Service** (stateless).
* The Search Service queries **Elasticsearch** for fast geo + text queries. For heavy personalization it may call Recommendation Service or Recommender cache.

---

## 4) Elasticsearch index & mapping (minimal, important fields)

Use a `shows` index (one document per show). Key fields:

```json
{
  "mappings": {
    "properties": {
      "showId": {"type":"keyword"},
      "eventId": {"type":"keyword"},
      "venueId": {"type":"keyword"},
      "hallId": {"type":"keyword"},
      "title": {"type":"text", "analyzer":"standard"},
      "description": {"type":"text"},
      "startsAt": {"type":"date"},
      "endsAt": {"type":"date"},
      "location": {"type":"geo_point"},           // lat/lon
      "city": {"type":"keyword"},
      "categories": {"type":"keyword"},
      "priceMin": {"type":"integer"},
      "priceMax": {"type":"integer"},
      "status": {"type":"keyword"},               // SCHEDULED/CANCELLED
      "popularity": {"type":"float"},             // for ranking
      "createdAt": {"type":"date"}
    }
  }
}
```

Important: use `geo_point` and date types for geo + time filters.

---

## 5) Example Elasticsearch queries

### Nearby shows within radius and time window

```json
{
  "query": {
    "bool": {
      "must": [
        { "range": { "startsAt": { "gte": "now", "lte": "now+7d" } } }
      ],
      "filter": [
        { "geo_distance": { "distance": "10km", "location": { "lat":17.41, "lon":78.42 } } },
        { "term": { "status": "SCHEDULED" } }
      ]
    }
  },
  "sort": [
    { "_score": "desc" },
    { "_geo_distance": { "location": { "lat":17.41,"lon":78.42 }, "order":"asc", "unit":"km" } }
  ],
  "size": 20
}
```

### Full-text + filters + pagination

* Use `multi_match` on `title` and `description`.
* Use `bool` with `term` filters for categories, city, price range.
* Use cursor-based pagination (search\_after) for consistent paging at scale.

---

## 6) Indexing / ingestion pipeline (recommended)

1. **Event flow**: When `event` or `show` is created/updated/cancelled, **Events & Venues** service writes to DB → after DB commit publishes `show.created` / `show.updated` / `show.cancelled` on Kafka.
2. **Indexer**: A small **Indexer service** consumes events, transforms DB representation into ES document shape, and upserts the document into ES.

   * Use bulk requests for high throughput.
   * Include `routing` by `venueId` if needed.
3. **Soft-delete / retire**: On cancel, update `status` to CANCELLED and optionally remove from index (soft-delete recommended to retain history).
4. **Near real-time**: ES refresh interval (default 1s) controls visibility; ensure indexer handles transient failures and retries.

Idempotency: indexer must be idempotent (use doc ID = `show_{showId}`).

---

## 7) Caching & performance

* **Edge caching**: Serve poster images and static seatmap JSON/SVG from CDN.
* **Result caching**: Cache popular queries / tiles in Redis with TTL (e.g., 30s–2m) keyed by rounded lat/lon+radius+filters to reduce ES load.
* **Precomputed tiles**: For very large scale, precompute popular city tiles (grid) and their top events for instant responses.
* **Rate limits**: enforce per-IP and per-user rate limits at API gateway.

---

## 8) Privacy & fallback (location)

* **If user allows location**: use exact coordinates for best results (geo\_distance query). Respect OS-level privacy.
* **If user denies**: fallback to `city` or `postal code` typed by user and run city-level queries (filter by `city` or bounding box).
* **Approximate location**: If location precision is low, widen radius accordingly and disclose to user.

---

## 9) Ranking & relevance (simple)

Combine signals into a ranking score:

* Text relevance score (Elasticsearch)
* Proximity (distance) — closer shows prefered
* Time proximity — shows sooner may rank higher
* Popularity / bookings count / promotions — boost via `popularity` field
* Personalization — boost events user likes/follows via Recommendation Service

Example: Boost by `function_score` in ES using `gauss` on geo and decay on `startsAt`.

---

## 10) Filters & UX behavior

Offer client filters:

* Date/time range, categories, language, price range, tags, availability (has seats), venue facilities (parking, wheelchair).
* Sorting options: `relevance`, `distance`, `startsAt`, `priceAsc`, `priceDesc`.
* Use debounced search on client and show “Did you mean…” / auto-complete.

---

## 11) Real-time updates & consistency

* Seat availability is volatile. For the feed only show a price range and a quick `availabilityHint` (e.g., seatsLeft: "many/few") — not authoritative.
* Before booking, client should fetch authoritative availability from Booking/SeatManager.
* Use WebSockets/SSE to push critical updates (show cancelled, sold out) to active clients.

---

## 12) Error handling & edge cases

* On ES failure: fallback to DB query (slow) or return cached results. Return 503 if neither available.
* If index stale: show UI loader “Refreshing” and on confirm fetch live data.
* Handle timezone carefully: convert `startsAt` to UTC and present to user in their timezone.

---

## 13) Observability & metrics (minimal)

Track:

* search\_requests\_per\_min, cache\_hit\_rate, ES\_query\_latency\_p95, indexer\_lag\_seconds (time from DB commit -> ES visibility), error\_rate.
* Business metrics: clicks->bookings conversion, top queries, popular venues.

---

## 14) Security & data hygiene

* Sanitize input (search queries) to avoid injection.
* Rate-limit free-text queries to avoid expensive wildcard scans.
* Only index public fields; protect PII. Use secured ES cluster with ACLs.

---
---

Nice — I’ll rewrite and expand your **Select seats → Hold → Payment → Confirm** flow into a clear, low-level, production-ready design. I’ll keep it simple and concrete: endpoints, Redis patterns, atomic hold behaviour, payment integration, webhook handling, failure/retry cases, and observability. Markdown below.

---

# 5. Select seats — final flow (low-level)

## 1) User flow summary (short)

1. User picks event → show (time) → requests seatmap for that show.
2. Server returns seat-layout + **authoritative availability** (or availability hint + authoritative check on hold).
3. User selects seats and clicks **Proceed** → client requests a *hold* for the selected seats.
4. Backend atomically places a **hold** (in Redis) with TTL (e.g., 10 minutes) and returns a `holdId`, price, and expiration.
5. User proceeds to payment using a payment intent (clientSecret) from payment gateway.
6. Payment gateway notifies Booking service (webhook). On verified success the Booking service converts hold → booked (persist to DB) and issues ticket(s).
7. If payment fails or hold expires, seats are released.

---

## 2) Important principles & guarantees

* **Holds are temporary and NOT final** — booking is final only after payment and DB commit.
* **Atomicity** for multi-seat holds — either all seats in the request are held or none.
* **Authoritative availability**: Redis holds are authoritative for brief window; DB is the source of truth for confirmed bookings.
* **Idempotency**: payment and webhook flows must be idempotent.
* **Security**: holds tied to user session; only holder can confirm or release. Use auth & rate-limiting to prevent abuse.

---

## 3) Key endpoints (simple)

### Get seatmap + availability

**GET** `/api/v1/shows/{showId}/seatmap?layoutVersion=latest`
Response includes layout + per-seat `state` hint (AVAILABLE/HELD/BOOKED/BLOCKED) and a fast `availabilityToken` (optional) to validate before hold.

### Create hold (atomic)

**POST** `/api/v1/shows/{showId}/holds`
Headers: `Authorization`, `Idempotency-Key`
Body:

```json
{
  "userId":"user_123",
  "seatUids":["hall1::A::A1","hall1::A::A2"],
  "holdTtlSecs": 600
}
```

Success (201):

```json
{
  "holdId":"hold_xyz",
  "expiresAt":"2025-10-01T19:45:20Z",
  "seatsHeld":[{"seatUid":"...","priceCents":7000}],
  "totalAmountCents":14000
}
```

Errors: 409 conflict with list of conflicting seats.

### Extend hold (optional)

**POST** `/api/v1/holds/{holdId}/extend` — extend TTL if allowed (one-time, small duration).

### Release hold (user cancels)

**DELETE** `/api/v1/holds/{holdId}`

### Create payment intent (get clientSecret)

**POST** `/api/v1/holds/{holdId}/payment-intent`
Body: `{ paymentMethod: "card" }`
Returns: `{ paymentIntentId, clientSecret, amountCents }`

### Confirm hold → booking (internal via webhook)

Booking service verifies webhook and calls confirm:
**POST** `/api/v1/holds/{holdId}/confirm` (internal only, idempotent)
Body: `{ paymentProvider: "stripe", paymentIntentId: "...", idempotencyKey: "..." }`
Response: `{ bookingId:"bk_123", tickets:[{ticketId, qrCode}] }`

---

## 4) Redis hold design (keys & patterns)

Use Redis for fast atomic holds. Example patterns:

* `seat:lock:{showId}:{seatUid}` → value: JSON `{ holdId, userId, expiresAt }` set with TTL
* `hold:{holdId}` → Redis Set of `seatUid` (members) and metadata in a hash: `hold:{holdId}:meta` → `{userId, showId, amountCents, createdAt, expiresAt}`
* `show:availability:{showId}` → optional bitmap/hash for fast scans

**Atomic hold Lua script** (pseudocode summary):

* Input: `showId`, `holdId`, `userId`, `ttl`, `seatUids[]`
* For each seatUid:

  * Check `seat:lock:{showId}:{seatUid}` exists → if yes, return conflict list.
  * Check `seat:booked:{showId}:{seatUid}` exists → if yes, return conflict.
* If no conflicts:

  * For each seat set `seat:lock:{showId}:{seatUid} = holdId|userId` with TTL.
  * Create `hold:{holdId}` set of seatUids and `hold:{holdId}:meta`.
* Return success with `expiresAt`.

This guarantees all-or-nothing hold creation and avoids race conditions.

---

## 5) Price calculation & final amount

* Price per seat should come from `show_seat.price_cents` (authoritative).
* Apply taxes, fees, convenience charges, offers/coupons (Pricing Service) before finalizing hold.
* Final total included in `hold:{holdId}:meta` and returned to client when hold created.

Note: discounts might require validation and entitlement checks (coupons may have per-user limits).

---

## 6) Client UX & authoritative check

* After hold created, client shows short countdown (TTL from `expiresAt`).
* Client should re-fetch hold status before starting payment and show the exact amount returned by `POST /holds/{holdId}/payment-intent`.
* Before submitting payment, client may optionally request an availability refresh.

---

## 7) Payment integration (flow & webhooks)

1. Client asks Booking service for **paymentIntent** for the hold (server calls PSP to create payment intent / session).

2. Server returns `clientSecret` (or payment URL) to client. Client mounts PSP SDK (Stripe/Paypal/Adyen) and completes payment.

3. PSP calls your **webhook** when payment is `succeeded`/`failed`/`requires_action`. Webhook must be **signed** and verified.

4. On `payment_succeeded`, Booking service calls `/holds/{holdId}/confirm` to convert hold → booking transactionally:

   * Verify hold exists and belongs to payment user.
   * Start DB transaction:

     * For each seat in hold: re-check no `show_seat` with state `BOOKED` exists for that `seat_uid`. (Double-check DB state)
     * Update `show_seat` rows: `state = BOOKED`, `booked_by = userId`, `booking_id = bkId`, `version++` (use optimistic locking or `WHERE state = 'AVAILABLE' OR state = 'HELD'`).
     * Create `booking` and `ticket` rows (with QR/unique token).
   * Commit transaction.
   * Delete Redis locks for those seats (atomic cleanup).
   * Publish `booking.created` & `seat.booked` events.
   * Return booking details to user and send email/SMS ticket.

5. On `payment_failed` or `payment_canceled`, optionally release hold immediately (delete Redis keys) and notify user.

Important: keep webhook handler idempotent — process each PSP event once (store PSP `event_id` processed set).

---

## 8) Confirm logic — DB level (authoritative)

* Confirm must be transactionally safe. Example approach:

  * Fetch all `show_seat` rows for the seat\_uids `FOR UPDATE` inside a DB transaction OR use optimistic updates with `version` check.
  * Ensure none already `BOOKED`. If any already booked, abort: refund (or mark booking failed) and release other seats. Prefer to avoid partial bookings: if any seat fails, abort whole booking and refund. Return clear error to user.
  * Update seat states to `BOOKED`, insert booking row, and ticket rows.
  * Commit and publish events.

---

## 9) Edge & failure cases (keep them simple)

### Race between two holds

* Redis atomic hold prevents two users holding same seat(s). If second user tries to hold, return `409` with conflicting seats.

### Payment succeeds but DB confirm fails

* Use retry with backoff and alert. If unrecoverable, initiate refund and notify user. Keep booking in `FAILED` state and provide support ticket.

### Webhook is delayed

* Holds may expire before webhook arrives. Strategy:

  * When webhook arrives, if hold TTL expired, you can still attempt to book (if seats are still reserved in DB) OR treat as expired and refund. Prefer to require that payment completed within hold TTL; but for async PSPs, extend hold TTL when payment initiated or keep a soft hold in DB. Be explicit in UX.

### Partial booking allowed?

* Prefer **all-or-nothing** bookings. If some seats can't be booked at confirm, cancel whole booking and refund.

### Chargebacks & refunds

* Keep audit trail for payments & bookings. Provide refund flow, mark seats as `REFUNDED` then `AVAILABLE` after refund clears (or manual approval if needed).

---

## 10) Hold expiry & background cleanup

* Redis TTL will auto-expire seat lock keys. When a hold expires:

  * A background worker (or Redis keyspace notifications) publishes `hold.expired` event — consumer releases `hold:{holdId}` meta and notifies clients (if connected).
  * Clean up `hold:{holdId}` set and metadata. Publish `seat.released` events for UI refresh.
* Also run periodic reaper job to reconcile any stale holds (e.g., after Redis restart).

---

## 11) Extendable UX: hold extensions & 3DS

* For payment methods requiring 3DS or manual steps, extend hold TTL at payment-init time (one short extension) to allow time for user to complete auth. Limit extension attempts to prevent abuse.

---

## 12) Security & anti-fraud

* Rate-limit holds per user/IP to prevent sniping/DoS.
* Enforce per-user seat-hold limits (e.g., max 10 seats).
* Run fraud checks (velocity, unusual card patterns) during payment flow; pause or flag suspicious bookings for manual review.
* Verify webhook signatures from PSP.

---

## 13) Observability & metrics

Track these minimal metrics:

* `holds_created/sec`, `holds_conflicts/sec`, `holds_expired/sec`, `holds_extended/sec`
* `payment_intents_created/sec`, `payment_success_rate`, `webhook_latency`
* `booking_confirm_success/sec`, `booking_confirm_failures/sec`
* Latency of Redis-lua atomic hold script, DB confirm transaction latency.

Log `traceId`, `userId`, `holdId`, `showId` for traceability.

---

## 14) Example end-to-end timeline (compact)

1. User GET `/shows/{id}/seatmap` → gets layout + availability hint.
2. Client POST `/shows/{id}/holds` with seats → receives `holdId`, TTL, totalAmount.
3. Client POST `/holds/{holdId}/payment-intent` → server creates PSP payment intent and returns `clientSecret`.
4. Client completes payment via PSP SDK.
5. PSP calls webhook → server verifies and calls `/holds/{holdId}/confirm` internally.
6. Booking service confirms seats (DB update), deletes Redis locks, issues tickets, publishes events, notifies user.

---

## 15) Short sample payloads (quick reference)

**Create hold request**

```json
POST /api/v1/shows/SHOW_1/holds
{
  "seatUids":["hall1::A::A1","hall1::A::A2"],
  "userId":"user_123",
  "holdTtlSecs":600
}
```

**Create payment intent**

```json
POST /api/v1/holds/hold_xyz/payment-intent
{ "paymentMethod":"card" }
```

**Webhook (simplified)**

```json
{
  "provider":"stripe",
  "eventId":"evt_1",
  "type":"payment_intent.succeeded",
  "data":{ "paymentIntentId":"pi_123","metadata": {"holdId":"hold_xyz","userId":"user_123"} }
}
```

---

## 16) Testing checklist (short)

* Concurrency test: many clients try to hold same seat simultaneously. Verify only one success.
* Timeout test: hold expires and seats become available.
* Payment webhook idempotency: repeated webhook events processed once.
* Failure test: payment success + DB failure → refund & alert.
* UX test: client loses network while in payment flow; reconnection flows.

---