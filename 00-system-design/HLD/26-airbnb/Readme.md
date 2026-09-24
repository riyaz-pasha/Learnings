# High Level Design: Airbnb-like Booking System

Let me walk you through this the way you'd present it in a real interview — starting with requirements, then progressively zooming into each layer. Think of it as peeling an onion: each layer reveals more detail.

---

## Step 1: Clarify Requirements (Always do this first in interviews)

Before drawing a single box, you need to *negotiate scope*. In an interview, this signals maturity.

**Functional Requirements** are the things the system *must do*:
- Hosts can list properties with photos, pricing, availability calendars
- Guests can search properties by location, date, filters (price, amenities, etc.)
- Guests can book a property for specific dates
- Both parties receive notifications (booking confirmed, cancelled, etc.)
- Guests can leave reviews; hosts can review guests
- Payment processing with hold/release mechanics (money held until check-in)

**Non-Functional Requirements** are the *quality attributes* — this is where you show senior-level thinking:
- The system serves ~150M users globally (Airbnb's real scale)
- Search must be fast: **< 200ms** for query responses
- Booking must be **strongly consistent** — two guests cannot book the same room for the same night (this is a critical constraint, remember it)
- The system must be **highly available** — 99.99% uptime
- Read-heavy system: searches far outnumber bookings (roughly 100:1 ratio)

**Capacity Estimation** — back-of-envelope:
- ~10M listings globally
- ~5M searches/day → ~58 searches/second
- ~500K bookings/day → ~6 bookings/second
- Each listing ~50KB (text) + ~5MB (photos) → ~50TB for text, photos stored in object storage

---

## Step 2: High-Level Architecture

The first diagram you'd draw on the whiteboard — the 10,000-foot view.

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTS                              │
│         Web Browser        Mobile App (iOS/Android)         │
└─────────────────┬───────────────────────┬───────────────────┘
                  │                       │
                  ▼                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      CDN (CloudFront)                       │
│         Static assets, images, cached API responses        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway / Load Balancer               │
│         Rate limiting, Auth, SSL termination, Routing       │
└──────┬───────────┬────────────┬──────────┬──────────────────┘
       │           │            │          │
       ▼           ▼            ▼          ▼
  ┌─────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
  │ Search  │ │Listing  │ │Booking │ │  User    │
  │Service  │ │Service  │ │Service │ │  Service │
  └────┬────┘ └────┬────┘ └───┬────┘ └────┬─────┘
       │           │          │            │
       ▼           ▼          ▼            ▼
  ┌─────────┐ ┌─────────┐ ┌────────┐ ┌──────────┐
  │Elastic  │ │Postgres │ │Postgres│ │Postgres  │
  │ Search  │ │(Listings│ │(Book-  │ │(Users DB)│
  │ Cluster │ │  DB)    │ │ings DB)│ └──────────┘
  └─────────┘ └─────────┘ └────────┘
```

Notice how each service has its *own* database. This is the **Database-per-Service** pattern, which is fundamental in microservices. The Search Service doesn't share a DB with the Booking Service — they communicate via APIs or events, never via direct DB joins. This gives you independent scaling and fault isolation.

---

## Step 3: Core Data Model

The data model reveals how deeply you've thought about the domain. Let me walk you through the key entities.

```
┌──────────────┐        ┌──────────────────┐       ┌──────────────┐
│    USERS     │        │    LISTINGS      │       │  BOOKINGS    │
│──────────────│        │──────────────────│       │──────────────│
│ user_id (PK) │1─────N │listing_id (PK)   │1────N │booking_id(PK)│
│ name         │        │host_id (FK→Users)│       │listing_id(FK)│
│ email        │        │title             │       │guest_id  (FK)│
│ phone        │        │description       │       │check_in      │
│ is_host      │        │location_id (FK)  │       │check_out     │
│ profile_pic  │        │price_per_night   │       │total_price   │
│ created_at   │        │max_guests        │       │status        │
└──────────────┘        │amenities (JSONB) │       │payment_id(FK)│
                        │avg_rating        │       │created_at    │
                        └──────────────────┘       └──────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
            ┌───────────────┐       ┌─────────────────┐
            │  AVAILABILITY │       │    REVIEWS      │
            │───────────────│       │─────────────────│
            │ listing_id(FK)│       │ review_id (PK)  │
            │ date          │       │ booking_id (FK) │
            │ is_available  │       │ reviewer_id(FK) │
            │ price_override│       │ listing_id (FK) │
            └───────────────┘       │ rating (1-5)    │
                                    │ comment         │
                                    └─────────────────┘
```

The **Availability table** deserves special attention. Instead of storing "booked ranges", you store *individual date rows per listing*. This makes date-range queries extremely fast — you're just doing `WHERE listing_id = X AND date BETWEEN checkin AND checkout AND is_available = TRUE`. It also handles price overrides per night (e.g., weekend pricing).

---

## Step 4: The Search Service — The Heart of Airbnb

Search is the most complex and performance-critical piece. A guest types "Goa, Dec 20–25, 2 guests" — you need to return relevant, available listings in under 200ms.

```
        Guest Search Request
               │
               ▼
     ┌──────────────────┐
     │   Search Service │
     └────────┬─────────┘
              │
     ┌────────▼─────────┐      ┌──────────────────────┐
     │ Elasticsearch    │◄─────│  Listing Indexer     │
     │ (Text + Geo      │      │  (Syncs from         │
     │  Search Index)   │      │   Listings DB via    │
     └────────┬─────────┘      │   Change Data        │
              │                │   Capture / CDC)     │
     ┌────────▼─────────┐      └──────────────────────┘
     │ Availability     │
     │ Filter Layer     │──────► Redis Cache
     │ (Check Booking   │        (Hot listing
     │  DB for dates)   │         availability)
     └────────┬─────────┘
              │
     ┌────────▼─────────┐
     │  Ranking Engine  │
     │  (Price, Rating, │
     │  Relevance Score)│
     └──────────────────┘
```

Here's the key insight: **you don't query the Bookings database during search**. That would be too slow and would couple two separate services. Instead, you maintain a **pre-computed availability cache in Redis**. When a booking is confirmed or cancelled, an event is published to a message queue, and the Availability Cache is updated asynchronously.

Elasticsearch handles the geospatial + text part. It has a native `geo_distance` query — you give it a lat/lng and radius, and it finds all listings within that area. The full query looks conceptually like:

```json
{
  "query": {
    "bool": {
      "must": [
        { "geo_distance": { "distance": "10km", "location": { "lat": 15.3, "lon": 73.8 } }},
        { "range": { "price_per_night": { "gte": 2000, "lte": 8000 } }},
        { "term": { "max_guests": { "gte": 2 } }}
      ]
    }
  }
}
```

---

## Step 5: The Booking Service — Where Consistency is Critical

This is the most technically interesting part of the design. Two guests searching simultaneously might *both* see a listing as available, and both try to book it. You need to prevent **double booking** — this is a classic **race condition**.

```
  Guest A                      Guest B
     │                            │
     │ POST /book listing_123      │ POST /book listing_123
     │ Dec 20–25                  │ Dec 20–25
     ▼                            ▼
┌─────────────────────────────────────────────────┐
│              Booking Service                    │
│                                                 │
│  Step 1: Validate availability (READ)           │
│  Step 2: Lock the dates (PESSIMISTIC LOCK)      │
│  Step 3: Process payment (call Payment Service) │
│  Step 4: Confirm booking (WRITE)                │
│  Step 5: Release lock                           │
└────────────────────┬────────────────────────────┘
                     │
                     ▼
          ┌─────────────────────┐
          │    Bookings DB      │
          │  (PostgreSQL with   │
          │   Row-level Locking)│
          └─────────────────────┘
```

The safest solution uses **database-level locking** with a transaction:

```sql
BEGIN TRANSACTION;

-- Lock the availability rows for this listing and date range
-- SELECT FOR UPDATE prevents any other transaction from reading 
-- these rows until this transaction commits or rolls back
SELECT * FROM availability 
WHERE listing_id = 'listing_123'
  AND date BETWEEN '2024-12-20' AND '2024-12-25'
  AND is_available = TRUE
FOR UPDATE;  -- <--- This is the magic line

-- If we got all 5 rows (Dec 20, 21, 22, 23, 24), proceed
-- If any row is missing (already booked), ROLLBACK

UPDATE availability 
SET is_available = FALSE 
WHERE listing_id = 'listing_123' 
  AND date BETWEEN '2024-12-20' AND '2024-12-25';

INSERT INTO bookings (...) VALUES (...);

COMMIT;
```

Guest B's transaction will *wait* at the `SELECT FOR UPDATE` line until Guest A's transaction completes. Then Guest B will find those rows are no longer available and the booking will fail cleanly.

An alternative approach that scales better under very high load is **Optimistic Locking** with a version number — but pessimistic locking is simpler to reason about and perfectly fine at Airbnb's write scale (~6 bookings/second is not that high).

---

## Step 6: Payment Flow — The Escrow Model

Airbnb holds your money and only releases it to the host 24 hours after check-in. This is an **escrow-like payment flow** and it's architecturally important.

```
Guest                  Booking Service         Payment Service        Host
  │                          │                       │                 │
  │──── Book Listing ────────►│                       │                 │
  │                          │── Charge Card ────────►│                 │
  │                          │◄── Payment Auth ───────│                 │
  │                          │                       │                 │
  │◄── Booking Confirmed ────│                       │                 │
  │                   [Money held in escrow]          │                 │
  │                          │                       │                 │
  │                   [Guest checks in]              │                 │
  │                          │                       │                 │
  │             [24 hrs after check-in]              │                 │
  │                          │── Release Funds ──────►│── Payout ───────►│
  │                          │                       │                 │
```

This is implemented with a **scheduled job** (a cron or a delayed queue like SQS with delay) that runs 24 hours after each check-in date. If the guest raises a dispute within that window, the payment is held further for manual review.

---

## Step 7: Async Communication with Message Queues

Not everything needs to happen synchronously. When a booking is confirmed, many downstream things need to happen — send an email, update search indexes, notify the host, update analytics. Doing all of this in the booking request's response chain would make it slow and fragile.

```
Booking Service
      │
      │ Publishes "BookingConfirmed" event
      ▼
┌─────────────────────┐
│   Message Queue     │  (Kafka or AWS SNS/SQS)
│  "BookingConfirmed" │
└──┬──────┬───────┬───┘
   │      │       │
   ▼      ▼       ▼
Email   Search  Analytics
Service  Index  Service
(Notify Updater (Track
 guest)  (Mark   revenue)
         unavail)
```

Kafka is ideal here because multiple consumers can independently read the same event stream. The Search Index Updater and the Email Service both process the same `BookingConfirmed` event without interfering with each other — this is the **fan-out** pattern.

---

## Step 8: Caching Strategy

Caching is how you survive the 100:1 read-to-write ratio.

```
Request ──► Redis Cache ──► (Cache HIT) ──► Return Response
                │
           (Cache MISS)
                │
                ▼
          Postgres DB ──► Populate Cache ──► Return Response
```

What you cache and for how long: listing details (TTL: 1 hour, invalidated on update), search results for popular queries like "Goa this weekend" (TTL: 5 minutes), user profiles (TTL: 30 minutes), and availability data (TTL: 30 seconds, kept short because it changes with bookings).

The critical insight is that **availability data must have a short TTL**. A stale search result is acceptable — showing a listing that was booked 10 minutes ago is not catastrophic because the booking step will catch it. But availability should be refreshed often enough that the user experience isn't jarring.

---

## Step 9: Final Complete Architecture

```
                          ┌──────────────────┐
                          │   CDN + WAF      │
                          └────────┬─────────┘
                                   │
                          ┌────────▼─────────┐
                          │   API Gateway    │
                          │ (Auth, Rate Lim) │
                          └──┬──┬──┬──┬──────┘
                             │  │  │  │
              ┌──────────────┘  │  │  └──────────────┐
              │         ┌───────┘  └──────┐           │
              ▼         ▼                 ▼           ▼
        ┌──────────┐ ┌──────────┐  ┌──────────┐ ┌─────────┐
        │  User    │ │ Listing  │  │ Booking  │ │ Search  │
        │ Service  │ │ Service  │  │ Service  │ │ Service │
        └────┬─────┘ └────┬─────┘  └────┬─────┘ └────┬────┘
             │            │             │             │
        ┌────▼────┐  ┌────▼────┐  ┌────▼────┐  ┌────▼────┐
        │UserDB   │  │Listing  │  │Booking  │  │Elastic  │
        │(Postgres│  │DB       │  │DB       │  │Search   │
        └─────────┘  │(Postgres│  │(Postgres│  └─────────┘
                     └────┬────┘  └─────────┘
                          │ CDC
                          ▼
                   ┌─────────────┐
                   │   Kafka     │◄─── Booking Service events
                   └──┬──┬───┬───┘
                      │  │   │
                   ┌──┘  │   └──────────────┐
                   ▼     ▼                  ▼
             ┌───────┐ ┌────────┐    ┌──────────┐
             │ Email │ │Search  │    │Analytics │
             │Service│ │Indexer │    │(Redshift)│
             └───────┘ └────────┘    └──────────┘

           ┌─────────────────────────────────────┐
           │          Redis Cache                │
           │  (Sessions, Listing cache,          │
           │   Availability cache, Rate limits)  │
           └─────────────────────────────────────┘

           ┌─────────────────────────────────────┐
           │       S3 / Object Storage           │
           │   (Listing photos, user avatars)    │
           └─────────────────────────────────────┘
```

---

## Step 10: Key Design Decisions to Articulate in an Interview

An interviewer listens not just for *what* you designed but *why*. Here are the trade-off discussions that will make you stand out.

**Why Elasticsearch for search instead of Postgres?** Postgres can do full-text search and even geo queries, but Elasticsearch is optimized for exactly this — distributed, horizontal scaling, relevance ranking, and millisecond geo queries at the scale of 10M listings are all first-class features. For a smaller startup, Postgres with PostGIS extension would be totally fine.

**Why pessimistic locking instead of a distributed lock (like Redis Redlock)?** Distributed locks introduce network hops and failure modes — what if Redis goes down mid-booking? Database-level locks are transactional and atomic by definition. Since our booking volume is low (~6/sec), the lock contention is minimal and the simplicity is worth it.

**Why Kafka instead of RabbitMQ?** Kafka retains messages on disk, allowing consumers to replay events. If the Search Indexer crashes and comes back up, it can re-process all `ListingUpdated` events from where it left off. RabbitMQ deletes messages after delivery, making recovery harder.

**Single point of failure concern on the API Gateway?** You run multiple instances of the API Gateway behind a hardware/cloud load balancer (e.g., AWS ALB). The API Gateway layer is stateless, so horizontal scaling is trivial.

---

This design covers all the ground an interviewer expects: requirements → data model → core services → the hard problems (search, double-booking, payments) → async architecture → caching → trade-offs. Practice walking through it top-to-bottom in about 45 minutes, spending the most time on the Booking Service consistency section — that's where most interviewers probe the hardest.

---

# Airbnb Booking System — Functional Requirements Deep Dive

The goal here is to think like an engineer who's been asked to *build this from scratch*, then asked six months later to *scale it to 150 million users*. Each requirement gets that full arc — from "what would a 10-person startup ship" to "what does the real Airbnb actually need."

---

## Requirement 1: Property Listing Management

### What this means in plain English

A host needs to be able to create a listing, upload photos, set a price, describe the place, define which dates are available, and edit or deactivate the listing later. Think of it as a rich "create + manage" CRUD interface, but with some genuinely hard sub-problems hiding underneath.

---

### Basic Version — The Startup Implementation

At the simplest level, a listing is just a database row. You store all the text fields (title, description, amenities) directly in a Postgres table, save uploaded photos to the same server's disk, and write a simple REST API around it.

```
Host uploads listing
         │
         ▼
┌─────────────────┐        ┌─────────────────────┐
│   Web Server    │──Save──►   Local Disk         │
│ (Node / Django) │  photos │ /uploads/listing_id │
└────────┬────────┘        └─────────────────────┘
         │
    Save text data
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │
│                 │
│ listings table  │
│ ───────────     │
│ id, title,      │
│ description,    │
│ price, host_id  │
└─────────────────┘
```

This works perfectly fine for your first 1,000 listings. The entire system runs on a single server. A junior engineer can build and understand it in a weekend.

The **availability calendar** at this stage is equally simple. You store a row per day per listing in an `availability` table:

```sql
CREATE TABLE availability (
  listing_id  UUID,
  date        DATE,
  is_available BOOLEAN DEFAULT TRUE,
  price_override DECIMAL,  -- null means use base price
  PRIMARY KEY (listing_id, date)
);
```

This row-per-day model is deceptively smart. It looks verbose (a listing available for a year = 365 rows), but it makes availability queries — which you'll run *constantly* — dead simple: just a `WHERE date BETWEEN X AND Y AND is_available = TRUE`.

---

### The Cracks That Appear at Scale

Three problems emerge when you start growing.

**Photo storage.** Storing photos on the web server's local disk breaks the moment you add a second server. Server A handles the upload and saves the file. Server B handles the later read request and has no idea where the file is. Even before multiple servers, a single server's disk fills up fast — a listing with 20 photos at 5MB each means 100MB per listing, and 100,000 listings = 10TB on a single disk.

**Availability table size.** At 10M listings × 365 days = 3.65 billion rows. Simple `WHERE` queries start taking seconds instead of milliseconds without very careful indexing.

**Schema rigidity.** Amenities are a perfect example. You start with a few checkboxes (WiFi, Pool, Kitchen). Six months later you have 80 amenity types. A rigid column-per-amenity schema means running `ALTER TABLE` in production every time marketing wants to add "Hot Tub" — which locks the entire table on most databases.

---

### Scalable Version — Production Design

**Photo storage moves to object storage (S3)**. The web server only receives the file, generates a unique key, uploads it to S3, and stores the S3 URL in the database. The server itself stays stateless — it never touches the file again. A CDN (like CloudFront) sits in front of S3 so users download photos from an edge node near them, not from S3 in us-east-1.

```
Host uploads photo
        │
        ▼
┌───────────────┐    1. Upload to S3     ┌──────────┐
│ Listing       │───────────────────────►│   S3     │
│ Service       │                        │ (Object  │
│               │◄───────────────────────│ Storage) │
└───────┬───────┘    2. Get back URL     └────┬─────┘
        │                                     │
        │ 3. Store URL in DB                  │ Replicated
        ▼                                     │ globally
┌───────────────┐                        ┌────▼─────┐
│  Listings DB  │                        │   CDN    │
│  (URL stored) │                        │(CloudFnt)│
└───────────────┘                        └──────────┘
```

**Amenities become JSONB.** PostgreSQL's `JSONB` column type lets you store a flexible JSON blob while still being able to index and query into it. You get schema flexibility without sacrificing queryability.

```sql
-- The listings table now looks like this
CREATE TABLE listings (
  id              UUID PRIMARY KEY,
  host_id         UUID REFERENCES users(id),
  title           VARCHAR(255),
  description     TEXT,
  price_per_night DECIMAL(10,2),
  location        GEOGRAPHY(POINT, 4326),  -- PostGIS spatial type
  amenities       JSONB,   -- {"wifi": true, "pool": false, "hot_tub": true}
  photos          JSONB,   -- [{"url": "...", "order": 1}, ...]
  max_guests      INT,
  status          ENUM('active', 'inactive', 'pending_review'),
  avg_rating      DECIMAL(3,2),  -- denormalized for search performance
  review_count    INT,
  created_at      TIMESTAMPTZ,
  updated_at      TIMESTAMPTZ
);

-- You can still query inside JSONB efficiently
CREATE INDEX idx_amenities_wifi ON listings ((amenities->>'wifi'));
```

**The availability table gets partitioned.** Postgres table partitioning lets you split that 3.65B row table by `listing_id` hash. Each partition holds a subset of listings. A query for a specific listing only scans its partition, not the entire table.

```
Availability Table (Partitioned by listing_id hash)
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Partition 0    │ │  Partition 1    │ │  Partition N    │
│  listing_ids    │ │  listing_ids    │ │  listing_ids    │
│  hash % N = 0   │ │  hash % N = 1   │ │  hash % N = N   │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

**Listing updates publish events.** When a host edits their listing, the Listing Service doesn't just update the database — it also publishes a `ListingUpdated` event to Kafka. The Search Index consumer (Elasticsearch) picks this up and re-indexes the listing asynchronously. This keeps search results fresh without coupling the Listing Service to the Search Service.

```
Host edits listing
        │
        ▼
┌───────────────┐
│ Listing       │──── UPDATE listings SET ... ────► Postgres
│ Service       │
│               │──── Publish "ListingUpdated" ───► Kafka
└───────────────┘                                       │
                                                        ▼
                                              ┌──────────────────┐
                                              │ Search Indexer   │
                                              │ (re-indexes in   │
                                              │  Elasticsearch)  │
                                              └──────────────────┘
```

---

## Requirement 2: Property Search

### What this means in plain English

A guest types "Bali, Dec 20–27, 2 guests, budget under ₹5000/night" — and within 200 milliseconds, they should see a map of beautiful villas sorted by relevance. This requirement hides enormous complexity because it's actually *three problems in one*: geospatial filtering, text/attribute filtering, and availability filtering, all happening simultaneously.

---

### Basic Version

At small scale, you can get away with a single SQL query against Postgres with the PostGIS extension. PostGIS adds spatial types and functions to Postgres — it understands latitude/longitude and can calculate distances natively.

```sql
SELECT 
    l.*,
    ST_Distance(l.location, ST_MakePoint(115.1889, -8.4095)) AS distance_meters
FROM listings l
WHERE 
    -- Geo filter: within 20km of Bali center
    ST_DWithin(
        l.location,
        ST_MakePoint(115.1889, -8.4095)::geography,
        20000  -- 20km in meters
    )
    -- Price filter
    AND l.price_per_night BETWEEN 1000 AND 5000
    
    -- Availability filter: all requested dates must be available
    AND NOT EXISTS (
        SELECT 1 FROM availability a
        WHERE a.listing_id = l.id
          AND a.date BETWEEN '2024-12-20' AND '2024-12-26'
          AND a.is_available = FALSE
    )
    
    -- Guest capacity filter
    AND l.max_guests >= 2
    
ORDER BY l.avg_rating DESC, distance_meters ASC
LIMIT 50;
```

This single query handles everything. With proper indexes (a GiST index on `location`, a B-tree index on `price_per_night`), this works fine up to maybe 100K listings and moderate traffic.

---

### The Cracks That Appear at Scale

With 10M listings and 58 searches/second, this SQL query becomes your bottleneck in two ways. First, the `NOT EXISTS` subquery on the availability table is doing a nested lookup for every candidate listing — with billions of availability rows, this is slow. Second, Postgres is powerful but it's a single node (or a primary + read replicas), and it wasn't designed to be a distributed search engine. Relevance ranking, fuzzy text matching ("Goa" also matching "North Goa", "South Goa"), and complex multi-filter queries with facets are clunky.

---

### Scalable Version — The Two-Phase Search Architecture

The key insight in scaling search is to **separate the search problem from the availability problem**. You let Elasticsearch handle the fast, flexible, geo+text+filter querying, and you handle availability as a separate filtering step afterwards.

```
Guest sends search request
          │
          ▼
┌─────────────────────────────────────────┐
│           Search Service                │
│                                         │
│  Phase 1: Elasticsearch Query           │
│  ┌──────────────────────────────────┐   │
│  │ - Geo filter (within X km)       │   │
│  │ - Price range filter             │   │
│  │ - Amenities filter               │   │
│  │ - Guest capacity filter          │   │
│  │ → Returns top 200 candidate IDs  │   │
│  └──────────────────────────────────┘   │
│                  │                      │
│                  ▼                      │
│  Phase 2: Availability Filter           │
│  ┌──────────────────────────────────┐   │
│  │ - Check Redis cache for those    │   │
│  │   200 listing IDs + date range   │   │
│  │ - Filter out unavailable ones    │   │
│  │ → Returns final ~50 results      │   │
│  └──────────────────────────────────┘   │
│                  │                      │
│                  ▼                      │
│  Phase 3: Ranking                       │
│  ┌──────────────────────────────────┐   │
│  │ - Re-rank by relevance score,    │   │
│  │   rating, price, photos quality  │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

Why get 200 candidates first and then filter? Because you can't know in advance how many will be unavailable. If you ask Elasticsearch for exactly 50 and 30 of them are booked, you return only 20 results to the user. By over-fetching (200) and then filtering, you ensure you can always serve a full page of results.

**The Availability Cache in Redis** is the key performance trick. Instead of hitting the Postgres bookings table on every search, you maintain a Redis set for each listing that stores the booked dates. It looks like this:

```
Redis Key: "booked_dates:listing_uuid_123"
Redis Type: Sorted Set
Value: { "2024-12-20", "2024-12-21", "2024-12-22" }  ← booked dates
TTL: 24 hours (refreshed on any booking change)
```

Checking availability for 200 listings against a date range becomes 200 parallel Redis lookups — each one is a sub-millisecond operation. The whole availability filter takes maybe 10ms total.

**The Elasticsearch index** is carefully structured. You denormalize data aggressively — you store the `avg_rating` and `review_count` directly in the listing document even though they "belong" to the reviews service, because you need them at search time without a join.

```json
{
  "listing_id": "abc-123",
  "title": "Cozy Villa in North Goa",
  "location": { "lat": 15.4909, "lon": 73.8278 },
  "price_per_night": 3500,
  "max_guests": 4,
  "amenities": ["wifi", "pool", "kitchen", "ac"],
  "avg_rating": 4.87,
  "review_count": 143,
  "photos_count": 18,
  "host_response_rate": 0.98,
  "instant_book": true
}
```

Notice `host_response_rate` and `instant_book` are in the search index. These feed into the ranking algorithm — a host who responds to 98% of inquiries within an hour should rank higher, because they're more likely to complete the booking.

---

## Requirement 3: Booking a Property

### What this means in plain English

A guest picks dates, clicks "Book Now," and the system must guarantee exactly one thing above everything else: **no two guests can book the same listing for the same dates**. This is the core correctness requirement of the entire system. Everything else can be eventually consistent; this cannot.

---

### Basic Version

At small scale, you can use a simple database transaction with a uniqueness check:

```sql
BEGIN;

-- Check if available (naive version - has race condition!)
SELECT COUNT(*) FROM availability
WHERE listing_id = $1
  AND date BETWEEN $2 AND $3
  AND is_available = FALSE;

-- If count = 0, mark as booked
UPDATE availability SET is_available = FALSE
WHERE listing_id = $1 AND date BETWEEN $2 AND $3;

INSERT INTO bookings (...) VALUES (...);

COMMIT;
```

The problem here is subtle and important. Between the `SELECT COUNT(*)` and the `UPDATE`, another transaction can sneak in and also see `is_available = TRUE`. Both transactions read "available", both proceed to book, and you have a double booking. This is called a **TOCTOU race condition** (Time Of Check To Time Of Use).

---

### The Correct Basic Version — Pessimistic Locking

The fix is `SELECT FOR UPDATE`, which is a pessimistic lock. It says "I am about to modify these rows — lock them so nobody else can read them until I'm done."

```sql
BEGIN;

-- SELECT FOR UPDATE acquires a row-level lock.
-- Any other transaction trying to SELECT FOR UPDATE 
-- on the SAME rows will WAIT here until we COMMIT or ROLLBACK.
SELECT * FROM availability
WHERE listing_id = $1
  AND date BETWEEN $2 AND $3
  AND is_available = TRUE
FOR UPDATE;

-- If we got back N rows where N = (checkout - checkin in days),
-- all dates are available. Proceed.
-- If we got back fewer rows, some dates are taken. ROLLBACK.

UPDATE availability 
SET is_available = FALSE
WHERE listing_id = $1 AND date BETWEEN $2 AND $3;

INSERT INTO bookings (
  id, listing_id, guest_id, 
  check_in, check_out, 
  total_price, status
) VALUES (
  gen_random_uuid(), $1, $4,
  $2, $3,
  $5, 'pending_payment'
);

COMMIT;
```

This is **safe, correct, and simple**. Guest B's transaction literally freezes at the `SELECT FOR UPDATE` line until Guest A's transaction finishes. If Guest A completes the booking, Guest B's query returns fewer rows than needed (because `is_available = FALSE` now) and the booking correctly fails.

---

### The Full Booking State Machine

A booking isn't just "booked or not." It goes through states, and each state transition must be explicit and auditable. Here's why this matters: if a user's card is declined mid-booking, you need to release the availability. If a host cancels, you need to refund and release.

```
                    ┌─────────────────────────────────────────┐
                    │           BOOKING STATE MACHINE         │
                    └─────────────────────────────────────────┘

    Guest clicks Book
           │
           ▼
    ┌─────────────┐    Payment fails      ┌─────────────┐
    │  INITIATED  │──────────────────────►│   FAILED    │
    └──────┬──────┘                       └─────────────┘
           │
    Payment authorized
           │
           ▼
    ┌─────────────┐    Host rejects /     ┌─────────────┐
    │  CONFIRMED  │    auto-cancel ──────►│  CANCELLED  │◄──── Guest cancels
    └──────┬──────┘                       └──────┬──────┘
           │                                     │
    Check-in date                        Release availability
           │                             Trigger refund policy
           ▼
    ┌─────────────┐
    │  CHECKED_IN │
    └──────┬──────┘
           │
    24hrs after check-in
           │
           ▼
    ┌─────────────┐
    │  COMPLETED  │──── Release payment to host
    └─────────────┘──── Trigger review request
```

Each state transition is a database update wrapped in a transaction. The `CANCELLED` state triggers different logic depending on *who* cancelled and *when* — this is where the cancellation policy kicks in (flexible, moderate, strict), which governs how much of the payment is refunded.

---

### Scalable Version — Handling High Contention

At Airbnb's scale, a few hot listings (a villa in Santorini during peak summer, say) might receive hundreds of simultaneous booking attempts. Pessimistic locking still works here — it serializes those 100 requests through the lock, one at a time. The first succeeds, the other 99 fail quickly and gracefully. The throughput concern at ~6 bookings/second globally is actually quite manageable for a lock-based approach.

However, if you were designing a system with *much* higher booking rates (think concert ticket sales, where 100,000 people try to buy the same ticket in 1 second), you'd switch to an **idempotency + queue** model: put all booking requests in a FIFO queue per listing, and process them serially. For Airbnb's scale, this is over-engineering.

What *is* worth adding at scale is **idempotency keys**. If a guest's network drops after they click "Book" but before they receive the confirmation, they might click again. Without idempotency, they'd get double-charged and double-booked. With idempotency keys, the client generates a unique `idempotency_key` (a UUID generated on the frontend) and sends it with every booking request. The server stores this key on the booking record. If the same key arrives again, it returns the existing booking instead of creating a new one.

```
Client                              Server
  │                                    │
  │── POST /bookings                   │
  │   Idempotency-Key: uuid-abc-123    │
  │   { listing_id, dates, ... }  ────►│
  │                                    │── Creates booking, stores key
  │◄── 201 Created, booking_id: 789 ───│
  │                                    │
  [Network drops, client retries]
  │                                    │
  │── POST /bookings (RETRY)           │
  │   Idempotency-Key: uuid-abc-123    │── SAME KEY
  │   { listing_id, dates, ... }  ────►│── Finds existing booking 789
  │                                    │── Does NOT create a new one
  │◄── 200 OK, booking_id: 789 ────────│
```

---

## Requirement 4: Notifications

### What this means in plain English

Both hosts and guests need to know what's happening: booking confirmation, payment receipt, upcoming check-in reminder, review request, cancellation notice. This sounds simple — just "send an email" — but it becomes an architectural challenge at scale because notifications must be **reliable** (you can't miss a booking confirmation), **timely** (the check-in reminder must go out 24 hours before, not "sometime that day"), and **multi-channel** (email, SMS, push notification, in-app).

---

### Basic Version

The simplest implementation is synchronous — when a booking is confirmed, the Booking Service directly calls an email library (like Nodemailer or SendGrid's API) before returning the response to the user.

```
Booking Service confirms booking
        │
        │── sendEmail(guest@email.com, "Booking Confirmed!")
        │── sendEmail(host@email.com, "New Booking!")
        │
        ▼ (only now returns response to user)
Guest sees confirmation
```

This works, but it's fragile. If SendGrid is down, your booking fails. The user's 5-second booking flow now depends on a third-party email provider. And if you want to add SMS notifications later, you have to modify the Booking Service — a violation of the Single Responsibility Principle.

---

### Scalable Version — Event-Driven Notification Pipeline

The fix is to make the Booking Service oblivious to notifications entirely. It publishes an event; a dedicated Notification Service handles the rest.

```
Booking confirmed
        │
        ▼
┌─────────────────┐
│  Booking        │──── Publish "BookingConfirmed" event ────► Kafka
│  Service        │                                               │
│  (returns 201)  │                                               │
└─────────────────┘                                               │
                                                                  ▼
                                                     ┌────────────────────┐
                                                     │ Notification       │
                                                     │ Service            │
                                                     │                    │
                                                     │ 1. Look up user    │
                                                     │    preferences     │
                                                     │    (email? SMS?    │
                                                     │     push?)         │
                                                     │                    │
                                                     │ 2. Render template │
                                                     │                    │
                                                     │ 3. Route to        │
                                                     │    providers:      │
                                                     └────────┬───────────┘
                                                              │
                              ┌───────────────┬──────────────┴───────────────┐
                              ▼               ▼                              ▼
                        ┌──────────┐   ┌──────────┐                  ┌──────────┐
                        │ SendGrid │   │  Twilio  │                  │  FCM /   │
                        │ (Email)  │   │  (SMS)   │                  │  APNs    │
                        └──────────┘   └──────────┘                  │  (Push)  │
                                                                      └──────────┘
```

**Scheduled notifications** are the other interesting problem. "Send a check-in reminder 24 hours before arrival" cannot be handled by an event trigger because there's no event at booking time that corresponds to "24 hours before check-in." You need a scheduler.

The clean solution is a **Delayed Job Queue**. When a booking is confirmed, you enqueue a job with a `run_at` timestamp set to 24 hours before check-in. A worker process continuously polls for jobs where `run_at <= NOW()` and executes them.

```
Booking confirmed for Dec 20 check-in
        │
        │── INSERT INTO scheduled_jobs (
        │       type = 'check_in_reminder',
        │       booking_id = '789',
        │       run_at = '2024-12-19 14:00:00'  ← 24hrs before check-in
        │   )
        │
[Time passes...]
        │
Dec 19, 2:00 PM ──► Scheduler polls: "Any jobs due now?" ──► YES
                            │
                            ▼
                     Notification Service sends
                     "Your Goa trip is tomorrow!" 
                     to guest's phone
```

AWS SQS with message delay, or a cron job hitting a `scheduled_jobs` table, both work for this. The `scheduled_jobs` approach gives you visibility and the ability to cancel a reminder if the booking is cancelled before the reminder fires.

---

## Requirement 5: Reviews

### What this means in plain English

After a stay, guests can rate and review the listing, and hosts can review guests. The interesting constraint here is that Airbnb uses **double-blind reviews** — neither party sees the other's review until both have submitted, or until the review window closes (14 days). This prevents retaliation (a guest giving a bad review, then the host retaliating with a bad guest review).

---

### Basic Version

A straightforward reviews table with a constraint that a review can only be created against a completed booking:

```sql
CREATE TABLE reviews (
  id              UUID PRIMARY KEY,
  booking_id      UUID REFERENCES bookings(id),
  reviewer_id     UUID REFERENCES users(id),
  reviewee_id     UUID REFERENCES users(id),  -- who is being reviewed
  listing_id      UUID REFERENCES listings(id),
  rating          SMALLINT CHECK (rating BETWEEN 1 AND 5),
  comment         TEXT,
  review_type     ENUM('guest_to_host', 'host_to_guest'),
  created_at      TIMESTAMPTZ,
  UNIQUE (booking_id, reviewer_id)  -- one review per person per booking
);
```

After a completed booking, both parties get a notification with a "Write a Review" link. Simple, works well at small scale.

---

### Scalable Version — Double-Blind Reviews and Denormalization

**Double-blind** requires one additional column: `is_published`. A review is written but `is_published = FALSE` until either both parties submit or 14 days pass.

```sql
ALTER TABLE reviews ADD COLUMN is_published BOOLEAN DEFAULT FALSE;
ALTER TABLE reviews ADD COLUMN review_window_closes_at TIMESTAMPTZ;
```

A scheduled job runs nightly and publishes reviews where `review_window_closes_at <= NOW()`. When *both* reviews exist for a booking, they're both published immediately regardless of the window.

**The Ratings Denormalization Problem** is the scalability challenge here. Every listing shows an average rating. If you compute this with `SELECT AVG(rating) FROM reviews WHERE listing_id = X` at query time, and you have 10K reviews for a popular listing, this aggregation runs on every page load. At Airbnb's traffic, that's millions of AVG computations per second.

The solution is to **denormalize** — maintain a running average directly on the `listings` table. Every time a new review is submitted, you update `avg_rating` and `review_count` on the listing row using an incremental formula:

```sql
-- When a new review with rating R is submitted for a listing:
UPDATE listings
SET 
  avg_rating = (avg_rating * review_count + R) / (review_count + 1),
  review_count = review_count + 1
WHERE id = listing_id;
```

This is a constant-time operation regardless of how many reviews exist. The math works because `(old_avg * old_count + new_rating) / new_count` is exactly the new average. You never have to scan the reviews table to display a rating.

---

## Requirement 6: Payments — The Escrow Model

### What this means in plain English

Airbnb takes your money when you book, holds it, and pays the host 24 hours after your check-in. This protects guests (if the listing is fake or doesn't match the description, they can dispute before money moves) and gives hosts confidence that they'll be paid. This is a **payment escrow** model, and it has several distinct technical challenges: securely charging cards, handling partial refunds based on cancellation policies, and reliably releasing funds days or weeks after the initial charge.

---

### Basic Version

For a startup, you integrate Stripe and use their PaymentIntent API. Stripe handles all the PCI compliance complexity — you never touch raw card numbers. The flow is simple:

```
Guest submits payment
        │
        ▼
┌─────────────────┐    1. Create PaymentIntent    ┌──────────┐
│ Payment Service │─────────────────────────────►│  Stripe  │
│                 │◄─────────────────────────────│          │
│                 │    2. Return client_secret    └──────────┘
└────────┬────────┘
         │
         │ 3. Return client_secret to frontend
         ▼
┌─────────────────┐
│    Frontend     │──── 4. Stripe.js confirms card ────► Stripe
│  (Guest's       │                                    (Charges card)
│   browser)      │◄─── 5. Payment confirmed ──────────
└─────────────────┘
         │
         │ 6. Webhook: payment_intent.succeeded
         ▼
┌─────────────────┐
│ Payment Service │──── Mark booking as CONFIRMED
└─────────────────┘
```

Critically, you should **always confirm booking via Stripe's webhook**, not via the frontend's response. A frontend can lie (someone could manipulate JavaScript) or fail to send the confirmation. Stripe's webhook is a server-to-server call that you can trust.

---

### Scalable Version — The Escrow Flow and Cancellation Policies

The full payment lifecycle has several steps that need to be handled reliably:

```
Timeline of a Booking Payment:

 BOOKING DAY              CHECK-IN DAY           CHECK-IN + 24HRS
      │                        │                        │
      ▼                        ▼                        ▼
 ┌─────────┐             ┌─────────┐             ┌─────────────┐
 │ CHARGE  │             │ MARK AS │             │  RELEASE    │
 │  CARD   │────────────►│CHECKED_IN────────────►│  FUNDS TO   │
 │(Capture │             │         │             │   HOST      │
 │ funds)  │             └─────────┘             └─────────────┘
 └─────────┘
      │
      │ (If guest cancels before check-in)
      ▼
 ┌─────────────────────────────────────────────┐
 │          CANCELLATION POLICY ENGINE         │
 │                                             │
 │  Flexible: Full refund if >24hrs before     │
 │  Moderate: Full refund if >5 days before    │
 │  Strict:   50% refund if >7 days before     │
 │            No refund if <7 days before      │
 └─────────────────────────────────────────────┘
      │
      ▼
 Stripe Refund API (partial or full)
```

**The payment release (escrow unlock)** is implemented as a delayed job, exactly like the check-in reminder notification. When a booking is confirmed, you schedule a job for `check_in_datetime + 24 hours` that calls Stripe's Transfer API to move funds to the host's connected Stripe account.

**The Idempotency Problem in Payments** deserves special attention. If the payment service crashes *after* charging Stripe but *before* saving the booking confirmation, what happens? When the service restarts and retries, you must not double-charge. The solution is to store Stripe's `payment_intent_id` before making the charge, and always check if a `payment_intent_id` already exists before creating a new charge. Stripe also accepts idempotency keys directly — the same key always returns the same result.

```
┌─────────────────────────────────────────────────────────────┐
│              PAYMENT FAILURE RECOVERY PATTERN               │
│                                                             │
│  1. Generate idempotency_key = hash(booking_id + "charge") │
│  2. Call Stripe with idempotency_key                        │
│  3a. Success → save payment_intent_id → confirm booking     │
│  3b. Crash → on retry, same idempotency_key → Stripe        │
│       returns the ORIGINAL charge result, not a new charge  │
│  3c. Card declined → mark booking as FAILED, release        │
│       availability lock                                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Putting It All Together — How the Requirements Interconnect

The most important thing to communicate in an interview is that these six requirements aren't independent — they form a *system*. Here's the interaction map that ties everything together:

```
┌──────────────────────────────────────────────────────────────────┐
│                         EVENT FLOW                               │
│                                                                  │
│  ListingCreated ──────────────────► Update Search Index          │
│                                                                  │
│  BookingConfirmed ─────────────────► Mark dates unavailable      │
│                    ─────────────────► Update Search Index        │
│                    ─────────────────► Send notifications         │
│                    ─────────────────► Schedule payment release   │
│                    ─────────────────► Schedule review request    │
│                                                                  │
│  BookingCancelled ─────────────────► Release dates              │
│                    ─────────────────► Process refund             │
│                    ─────────────────► Update Search Index        │
│                    ─────────────────► Send notifications         │
│                                                                  │
│  ReviewSubmitted ──────────────────► Update listing avg_rating   │
│                    ─────────────────► Check if both reviewed     │
│                    ─────────────────► Publish if double-blind    │
│                                       window satisfied           │
└──────────────────────────────────────────────────────────────────┘
```

The discipline of modeling your system as **events flowing through a pipeline** rather than "services calling services" is what separates a mid-level design from a senior-level one in an interview. It makes the system easier to extend (adding a new consumer of `BookingConfirmed` requires zero changes to the Booking Service), more resilient (consumers can fail and replay), and easier to reason about (the Booking Service's job ends at publishing the event).

---

## Interview Cheat Sheet — The One-Sentence Principle for Each Requirement

You can anchor your entire design around these six statements, one per requirement:

**Listings** — "Store text in Postgres with JSONB for flexible amenities, photos in S3 via CDN, and sync changes to Elasticsearch via CDC events."

**Search** — "Two-phase search: Elasticsearch for geo+filter to get 200 candidates, then Redis availability cache to filter down to 50 — never hit the Bookings DB during search."

**Booking** — "Pessimistic locking via SELECT FOR UPDATE guarantees no double-booking; idempotency keys on both booking and payment calls make retries safe."

**Notifications** — "Booking Service publishes events; Notification Service consumes them and routes to email/SMS/push; scheduled reminders use a delayed job queue."

**Reviews** — "Double-blind with is_published flag and a scheduler; maintain running avg_rating on listings table using incremental math to avoid aggregation queries."

**Payments** — "Capture at booking; release via scheduled job 24 hours after check-in; never trust the frontend for payment confirmation, only Stripe webhooks."

---
