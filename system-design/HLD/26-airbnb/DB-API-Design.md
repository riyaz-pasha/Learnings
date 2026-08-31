Absolutely. Let’s design an **Airbnb-like application** as a system-design/interview exercise, starting from the two most important foundations:

1. **Database design**
2. **API design**

We’ll intentionally start simple, identify problems, and evolve the design—similar to how you'd explain it in an interview.

---

# 1. First define the scope

An Airbnb-like system has a lot of functionality, so let's constrain the first version.

### Core functionality

**Guest**

* Sign up / login
* Search properties
* Filter by location, dates, guests, price
* View property details
* Check availability
* Reserve/book
* Pay
* Cancel booking
* Review property

**Host**

* Create property
* Add rooms/listing details
* Set price
* Manage availability
* See reservations

We'll initially ignore:

* Messaging
* Recommendations
* Promotions
* Dynamic pricing
* Experiences
* Identity verification
* Complex cancellation policies

---

# 2. Start with the naive database

The first instinct might be:

```text
User
----
id
name
email
password
role

Property
--------
id
host_id
title
description
location
price
available_from
available_to

Booking
-------
id
property_id
guest_id
start_date
end_date
status
```

This looks reasonable.

Let's see how we'd use it.

### Search

```sql
SELECT *
FROM property
WHERE location = 'Hyderabad'
  AND price <= 5000;
```

### Create booking

```sql
INSERT INTO booking (
    property_id,
    guest_id,
    start_date,
    end_date,
    status
)
VALUES (
    10,
    100,
    '2026-09-10',
    '2026-09-15',
    'CONFIRMED'
);
```

But now we have a **huge problem**.

---

# 3. Problem: How do we know whether a property is available?

Suppose:

```text
Property = 10

Existing booking:
Sep 10 → Sep 15
```

Another user wants:

```text
Sep 12 → Sep 14
```

Our database doesn't stop this.

We could query:

```sql
SELECT *
FROM booking
WHERE property_id = 10
  AND start_date < '2026-09-14'
  AND end_date > '2026-09-12';
```

If this returns a row, there is an overlap.

The important condition is:

```text
existing.start < requested.end
AND
existing.end > requested.start
```

So:

```text
Existing:   |--------|
Requested:      |--------|
                OVERLAP
```

But there's an even bigger problem.

---

# 4. Race condition

Imagine two users:

```text
User A                         User B

Check availability             Check availability
       ↓                              ↓
   Available                      Available
       ↓                              ↓
Create booking                  Create booking
```

Both saw availability.

Now:

```text
Booking A: Sep 10 → Sep 15
Booking B: Sep 12 → Sep 14
```

**Double booking.**

This is one of the most important problems in an Airbnb-like system.

And this is where database design becomes interesting.

---

# 5. Separate "listing" from "availability"

Instead of putting availability directly inside `Property`, let's introduce concepts properly.

```text
User
 │
 ├── Guest
 │
 └── Host
       │
       ▼
    Property
       │
       ▼
    Listing
       │
       ├── Availability
       │
       └── Booking
```

For a simplified Airbnb model, we can actually treat a `Property` as the bookable listing.

So:

```text
users
properties
bookings
reviews
```

Let's build these properly.

---

# 6. Users

```sql
CREATE TABLE users (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

Notice something important:

We **don't** store:

```text
password
```

We store:

```text
password_hash
```

Never store plaintext passwords.

---

# 7. Properties

```sql
CREATE TABLE properties (
    id              BIGINT PRIMARY KEY,
    host_id         BIGINT NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    property_type   VARCHAR(50) NOT NULL,
    address         TEXT NOT NULL,
    city            VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    max_guests      INT NOT NULL,
    base_price      DECIMAL(12,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,

    FOREIGN KEY (host_id) REFERENCES users(id)
);
```

Example:

```text
Property
----------------------------
id             = 101
host_id        = 20
title          = "Beautiful 2BHK"
city           = "Hyderabad"
max_guests     = 4
base_price     = 3500
latitude       = ...
longitude      = ...
```

---

# 8. Bookings

```sql
CREATE TABLE bookings (
    id              BIGINT PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    guest_id        BIGINT NOT NULL,

    check_in       DATE NOT NULL,
    check_out      DATE NOT NULL,

    guests         INT NOT NULL,

    status          VARCHAR(30) NOT NULL,

    total_amount    DECIMAL(12,2) NOT NULL,

    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,

    FOREIGN KEY (property_id) REFERENCES properties(id),
    FOREIGN KEY (guest_id) REFERENCES users(id),

    CHECK (check_in < check_out)
);
```

Example:

```text
Booking
---------------------------------
id          5001
property    101
guest       900
check_in    2026-09-10
check_out   2026-09-15
guests      2
status      CONFIRMED
amount      17500
```

---

# 9. Reviews

```sql
CREATE TABLE reviews (
    id              BIGINT PRIMARY KEY,
    booking_id      BIGINT NOT NULL UNIQUE,
    property_id     BIGINT NOT NULL,
    guest_id        BIGINT NOT NULL,

    rating          INT NOT NULL,
    comment         TEXT,

    created_at      TIMESTAMP NOT NULL,

    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (property_id) REFERENCES properties(id),
    FOREIGN KEY (guest_id) REFERENCES users(id),

    CHECK (rating BETWEEN 1 AND 5)
);
```

Why `booking_id UNIQUE`?

Because we don't want:

```text
Booking 5001
    ↓
Review 1
Review 2
Review 3
```

A guest should review a booking once.

---

# 10. Property images

Don't put images directly inside `properties`.

Bad:

```text
properties
-----------
image1
image2
image3
```

Instead:

```sql
CREATE TABLE property_images (
    id              BIGINT PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    image_url       TEXT NOT NULL,
    display_order   INT NOT NULL,

    FOREIGN KEY (property_id) REFERENCES properties(id)
);
```

Now:

```text
Property
   │
   ├── Image 1
   ├── Image 2
   ├── Image 3
   └── Image 4
```

The actual image would normally live in object storage such as S3, while the database stores metadata/URL/key.

---

# 11. Property amenities

We shouldn't do this:

```text
properties
-----------
wifi = true
pool = true
parking = true
gym = false
...
```

Because amenities will keep changing.

Instead:

```sql
CREATE TABLE amenities (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(100) UNIQUE NOT NULL
);
```

and:

```sql
CREATE TABLE property_amenities (
    property_id BIGINT NOT NULL,
    amenity_id  BIGINT NOT NULL,

    PRIMARY KEY(property_id, amenity_id),

    FOREIGN KEY(property_id) REFERENCES properties(id),
    FOREIGN KEY(amenity_id) REFERENCES amenities(id)
);
```

Now:

```text
Property 101
     │
     ├── WiFi
     ├── Parking
     └── Swimming Pool
```

This is a classic **many-to-many relationship**.

---

# 12. The database now looks like this

```text
                       ┌──────────────┐
                       │    users     │
                       │--------------│
                       │ id           │
                       │ name         │
                       │ email        │
                       └──────┬───────┘
                              │
                     host_id / guest_id
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
       ┌──────────────┐                ┌──────────────┐
       │  properties  │                │   bookings   │
       │--------------│                │--------------│
       │ id           │◄───────────────│ property_id  │
       │ host_id      │                │ guest_id     │
       │ title        │                │ check_in     │
       │ city         │                │ check_out    │
       │ price        │                │ status       │
       └──────┬───────┘                └──────┬───────┘
              │                               │
       ┌──────┴───────┐                       │
       │              │                       ▼
       ▼              ▼                ┌──────────────┐
 property_images  property_amenities   │   reviews    │
                       │                │--------------│
                       ▼                │ booking_id   │
                  ┌───────────┐         │ rating       │
                  │ amenities │         └──────────────┘
                  └───────────┘
```

---

# 13. Now design the APIs

We can expose REST APIs.

## Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

Example:

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "riyaz@example.com",
  "password": "..."
}
```

Response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "expiresIn": 900
}
```

---

# 14. Property APIs

### Create property

```http
POST /api/v1/properties
```

```json
{
  "title": "Beautiful 2BHK Apartment",
  "description": "Modern apartment...",
  "propertyType": "APARTMENT",
  "city": "Hyderabad",
  "country": "India",
  "latitude": 17.385,
  "longitude": 78.486,
  "maxGuests": 4,
  "basePrice": 3500
}
```

The server gets:

```text
host_id
```

from the authenticated user.

**Don't accept `hostId` from the client.**

That's an important security principle.

---

### Get property

```http
GET /api/v1/properties/{propertyId}
```

---

### Update property

```http
PATCH /api/v1/properties/{propertyId}
```

---

### Delete property

```http
DELETE /api/v1/properties/{propertyId}
```

Obviously, only the host owning that property should be allowed to perform this.

---

# 15. Search API

This is probably the most important API.

```http
GET /api/v1/properties
```

Query parameters:

```text
city
checkIn
checkOut
guests
minPrice
maxPrice
amenities
page
size
sort
```

Example:

```http
GET /api/v1/properties
    ?city=Hyderabad
    &checkIn=2026-09-10
    &checkOut=2026-09-15
    &guests=2
    &minPrice=2000
    &maxPrice=5000
    &page=0
    &size=20
```

Notice we're using:

```text
GET /properties
```

rather than:

```text
GET /searchProperties
```

because search is effectively a query over the property resource.

---

# 16. Availability API

We could expose:

```http
GET /api/v1/properties/{propertyId}/availability
```

Parameters:

```text
checkIn
checkOut
```

Example:

```http
GET /api/v1/properties/101/availability
    ?checkIn=2026-09-10
    &checkOut=2026-09-15
```

Response:

```json
{
  "available": true
}
```

But here's an important interview point:

> **Availability check is not a reservation.**

The client might call:

```text
GET availability
```

and receive:

```text
available = true
```

but another user can book it immediately afterward.

Therefore we **cannot rely on this API for concurrency control**.

---

# 17. Booking API

```http
POST /api/v1/bookings
```

Request:

```json
{
  "propertyId": 101,
  "checkIn": "2026-09-10",
  "checkOut": "2026-09-15",
  "guests": 2
}
```

Again:

**Don't send:**

```json
{
  "guestId": 900
}
```

The server gets the guest from:

```text
JWT → authenticated user
```

---

# 18. Booking lifecycle

We need a state machine.

```text
PENDING
   │
   ├──→ CONFIRMED
   │
   └──→ CANCELLED

CONFIRMED
   │
   ├──→ COMPLETED
   │
   └──→ CANCELLED
```

Potential statuses:

```text
PENDING
CONFIRMED
CANCELLED
COMPLETED
EXPIRED
```

This becomes important when we introduce payment.

---

# 19. Booking APIs

```http
POST   /api/v1/bookings
GET    /api/v1/bookings/{bookingId}
GET    /api/v1/bookings
POST   /api/v1/bookings/{bookingId}/cancel
```

For example:

```http
GET /api/v1/bookings
```

could return the authenticated user's bookings.

We don't need:

```http
GET /api/v1/users/{userId}/bookings
```

for normal user access because the server already knows the current user.

---

# 20. Review API

```http
POST /api/v1/properties/{propertyId}/reviews
```

Request:

```json
{
  "bookingId": 5001,
  "rating": 5,
  "comment": "Great place!"
}
```

But the backend must verify:

```text
booking exists
        ↓
booking belongs to this guest
        ↓
booking belongs to this property
        ↓
booking is completed
        ↓
review doesn't already exist
```

This is a great example of why **API authorization cannot simply be "is the user logged in?"**

---

# 21. Host APIs

Hosts need their own operations.

```http
GET  /api/v1/host/properties
GET  /api/v1/host/bookings
POST /api/v1/properties
PATCH /api/v1/properties/{id}
```

We might eventually introduce:

```http
GET /api/v1/host/dashboard
```

which provides:

```text
Total properties
Upcoming reservations
Revenue
Occupancy
Reviews
```

---

# 22. One important API design decision

Should booking be:

```http
POST /properties/{id}/book
```

or:

```http
POST /bookings
```

I'd generally prefer:

```http
POST /bookings
```

because **Booking is a first-class resource**.

We're creating a booking.

```text
POST /bookings
```

is therefore more natural than treating booking as merely an action on property.

Similarly:

```http
POST /bookings/{id}/cancel
```

is reasonable because cancellation represents a **state-changing action**.

---

# 23. Initial API structure

```text
/api/v1
│
├── /auth
│   ├── POST /register
│   ├── POST /login
│   ├── POST /refresh
│   └── POST /logout
│
├── /properties
│   ├── GET    /
│   ├── POST   /
│   ├── GET    /{id}
│   ├── PATCH  /{id}
│   ├── DELETE /{id}
│   ├── GET    /{id}/availability
│   └── POST   /{id}/reviews
│
├── /bookings
│   ├── POST   /
│   ├── GET    /
│   ├── GET    /{id}
│   └── POST   /{id}/cancel
│
└── /host
    ├── GET /properties
    ├── GET /bookings
    └── GET /dashboard
```

---

# 24. But our design still has major problems

This is where I'd take the interview discussion.

Our current design doesn't properly solve:

### Problem 1 — Double booking

Two users can book the same property simultaneously.

### Problem 2 — Payment consistency

What happens if:

```text
Booking created
       ↓
Payment fails
```

Do we keep the booking?

What if:

```text
Payment succeeds
       ↓
Server crashes
```

before booking becomes confirmed?

### Problem 3 — Price changes

Suppose property price is:

```text
₹3,500/night
```

User starts booking.

Host changes price:

```text
₹5,000/night
```

What price should the existing booking use?

Obviously, the booking should retain the **agreed price**.

Therefore storing only:

```text
property.base_price
```

is insufficient.

We need a price snapshot.

### Problem 4 — Different prices by date

Real Airbnb-like systems don't necessarily have:

```text
property → one fixed price
```

You might have:

```text
Sep 10 → ₹3000
Sep 11 → ₹3000
Sep 12 → ₹4500
Sep 13 → ₹5000
```

So pricing needs its own model.

### Problem 5 — Availability

We need to decide whether availability is:

```text
computed from bookings
```

or:

```text
explicitly maintained
```

Both have tradeoffs.

### Problem 6 — Search scalability

This query:

```sql
WHERE city = ?
AND price BETWEEN ? AND ?
AND ...
```

is easy initially.

But Airbnb-scale search involving:

```text
location
dates
guests
price
amenities
rating
availability
sorting
```

can become a completely different system.

---

# 25. The learning path I'd recommend

Instead of jumping directly to "Airbnb at 100M users", let's evolve this system.

We'll do it like an interview.

### Stage 1 — Basic relational model

```text
User
Property
Booking
Review
Amenity
Image
```

We've done this.

### Stage 2 — SQL problems

We'll write queries for:

```text
1. Find available properties
2. Find overlapping bookings
3. Find user's bookings
4. Find host's revenue
5. Find most popular properties
6. Find properties with rating > 4
7. Pagination
8. Sorting
9. Filtering by amenities
10. Find properties with no bookings
```

Then we'll intentionally create inefficient queries and fix them using:

```text
indexes
JOINs
EXISTS
composite indexes
transactions
constraints
```

### Stage 3 — Booking correctness

We'll tackle:

```text
double booking
      ↓
race condition
      ↓
transactions
      ↓
locking
      ↓
optimistic vs pessimistic locking
      ↓
database constraints
```

This is one of the **best interview portions** of this design.

### Stage 4 — Pricing

We'll evolve:

```text
base_price
```

into:

```text
nightly_prices
```

and eventually:

```text
pricing rules
fees
taxes
discounts
price snapshots
```

### Stage 5 — Payment

Then:

```text
Booking
   ↓
Payment
   ↓
Payment Gateway
   ↓
Webhook
   ↓
Booking confirmation
```

We'll discuss:

```text
idempotency
payment retries
webhooks
failure scenarios
distributed transactions
sagas
```

### Stage 6 — Scale

Finally:

```text
PostgreSQL
    ↓
Read replicas
    ↓
Redis
    ↓
Search engine
    ↓
Kafka
    ↓
Microservices
```

and determine **why each component becomes necessary**, rather than blindly adding components.

---

## The key mental model

For interviews, don't memorize an Airbnb architecture diagram.

Think:

```text
Requirement
    ↓
Data model
    ↓
API
    ↓
SQL
    ↓
Problem
    ↓
Concurrency / consistency issue
    ↓
Solution
    ↓
New problem introduced
    ↓
Next solution
```

That's the approach I'd use for this entire design.

**Next, I'd suggest we take the current database and write the important SQL queries one by one—starting with the simplest "find available properties" query, deliberately make it wrong/slow, then evolve it.** That will also naturally lead us into indexes, transactions, and the double-booking problem.

