### Detailed Functional Flows

### 1. Venue & Hall Creation Flow
This flow describes how an admin populates the system with new venues and their corresponding halls.

* **Actor:** Admin
* **Process:**
    1.  The admin uses a client-side application to submit details (name, address, location, images) for a new venue. The request is sent to the **Events & Venues Service** via a `POST /api/v1/venue` endpoint.
    2.  The service validates the input and creates a new record in the `Venues` table, generating a unique `venueId`.
    3.  The admin then creates a hall or screen under the newly created venue. A `POST /api/v1/venue/{venueId}/hall` request is sent to the Events & Venues Service.
    4.  The service validates the request, including the complex `seatLayout` JSON, and creates a new record in the `Halls` table, linking it to the `venueId`.

---

### 2. Event & Showtime Creation Flow
This flow details how admins create a core event and then schedule specific showings of that event.

* **Actor:** Admin
* **Process:**
    1.  The admin first creates a core event record (e.g., "A specific movie" or "A concert"). A `POST /api/v1/events` request is sent to the **Events & Venues Service** with the event's metadata. The service creates a new record in the `Events` table.
    2.  Next, the admin schedules a specific showing of that event at a particular hall on a given date and time. A `POST /api/v1/events/{eventId}/showtimes` request is made.
    3.  The service creates a new record in the `Showtimes` table, which serves as a unique instance of an event showing. This record links the `eventId` to a specific `hallId`, `venueId`, and a precise `timestamp`.

---

### 3. Showtime Inventory Initialization Flow
This is an automated, event-driven flow that prepares the real-time seat inventory for a new showtime. 

* **Actor:** Internal Worker
* **Process:**
    1.  When a new showtime is created, the **Events & Venues Service** publishes a `ShowtimeCreatedEvent` message to a message queue.
    2.  A dedicated `ShowtimeInventoryInitializer` worker in the **Booking & Inventory Service** consumes this event.
    3.  The worker retrieves the `seatLayout` from the Events & Venues Service.
    4.  It dynamically generates a `JSONB` object containing all the seats for that hall, setting the initial `status` of every seat to `"available"`.
    5.  The worker then creates a single record in the `ShowtimeInventory` table, storing this `JSONB` `seatMap` against the `showtimeId`. This single record will be the source of truth for all subsequent seat status updates.

---

### 4. Event Discovery & Search Flow
This flow outlines how a user finds events to book, using a specialized search service.

* **Actor:** User
* **Process:**
    1.  The user lands on the client application's homepage. The application attempts to get the user's location via geolocation.
    2.  The client sends a `GET /api/v1/search/shows` request to the **Search Service**, passing the user's `lat` and `lon` coordinates.
    3.  The Search Service uses **Elasticsearch** to perform a `geo_distance` query to find all showtimes within a given radius.
    4.  For added functionality, the user can also provide keywords (`query`), and the service will use a `multi_match` query to search across the denormalized event, venue, and showtime data.
    5.  The Search Service returns a list of relevant showtimes, which the client application displays to the user.

---

### 5. Seat Selection & Booking Flow
This multi-step, transactional flow manages seat holds, payment, and final booking confirmation.

* **Actor:** User
* **Process:**
    1.  **View Inventory:** The user selects a showtime, and the client application fetches the real-time `seatMap` for that show from the **Booking & Inventory Service** via a `GET /inventory` endpoint.
    2.  **Reserve Seats:** When the user selects seats and proceeds, a `POST /reserve` request is sent. The Booking & Inventory Service uses an **atomic database transaction** to change the seat status from `"available"` to `"held"` and sets a `heldUntil` timestamp.
    3.  **Initiate Payment:** The client then requests to prepare for payment. The Booking & Inventory Service makes an internal call to a **Payment Service**, which communicates with a payment gateway to create a `paymentIntent` and `clientSecret`.
    4.  **Payment & Webhook:** The client uses the `clientSecret` to complete the payment on the client side. The payment gateway then asynchronously sends a **webhook notification** to the Payment Service, confirming the transaction.
    5.  **Confirm Booking:** The Payment Service's webhook handler, upon successful payment, makes an internal API call to the **Booking & Inventory Service's** `PUT /confirm` endpoint. This finalizes the booking by converting the seat status from `"held"` to `"booked"` and creating a permanent record in the `Bookings` table.
---
---
---
---
-----

### 1\. Venue & Hall Creation Flow

This flow allows administrators to onboard new physical locations and define their seat layouts. The process is handled by the **Events & Venues Service**.

#### Flow Description

1.  The admin client sends a `POST` request to `/api/v1/venue`. The request body contains the venue's core details, including its address and geospatial coordinates (`lat`, `lon`).
2.  The **Events & Venues Service** receives the request, validates the data, and stores it as a new document in the `venues` collection. A unique `venueId` is generated.
3.  For each screen or hall within the venue, the admin sends a separate `POST` request to `/api/v1/venue/{venueId}/hall`, including a detailed `seatLayout` JSON object.
4.  The service validates the request and stores this information as a new document in the `halls` collection, linking it to its parent `venueId`.

#### Simple DB Schema (using Firestore or a NoSQL equivalent)

  * **`venues` collection**
      * `id` (string)
      * `name` (string)
      * `address` (map)
      * `location` (geopoint)
      * `imageUrls` (array)
  * **`halls` collection**
      * `id` (string)
      * `venueId` (string, indexed)
      * `hallName` (string)
      * `seatLayout` (JSON object)

#### Service-Level Optimizations

  * **API Validation:** Implement strict schema validation on all incoming requests to ensure data integrity from the start.
  * **Data Partitioning:** For geospatial data, services like Firestore's geopoint or a specialized geodatabase are used to allow for efficient "within a radius" queries, which are essential for the search service.
  * **Image Handling:** Store images in a dedicated object storage service like **S3** and save only the S3 keys in the database. This keeps database records small and allows for fast retrieval via a CDN.

-----

### 2\. Event & Showtime Creation Flow

This flow separates the creation of event metadata from the scheduling of its specific showings.

#### Flow Description

1.  The admin client sends a `POST` request to `/api/v1/events` to create the core event metadata (e.g., a movie title, a concert artist). The **Events & Venues Service** creates a new document in the `events` collection.
2.  To schedule a showing, the admin sends a `POST` request to `/api/v1/events/{eventId}/showtimes`, specifying the `venueId`, `hallId`, and a precise `timestamp`.
3.  The service creates a new document in the `showtimes` collection.
4.  Crucially, once the showtime document is saved, the service **asynchronously publishes a `ShowtimeCreatedEvent`** to a message queue (like Kafka or RabbitMQ). This decouples the event creation process from inventory management and search indexing.

#### Simple DB Schema

  * **`events` collection**
      * `id` (string)
      * `name` (string)
      * `imageUrl` (string)
      * `metadata` (JSON object)
  * **`showtimes` collection**
      * `id` (string)
      * `eventId` (string)
      * `venueId` (string)
      * `hallId` (string)
      * `showtime` (timestamp)

#### Service-Level Optimizations

  * **Event-Driven Architecture:** The use of a message queue is a powerful optimization. It allows other services (like the Inventory and Search services) to react to new showtimes without the Events & Venues Service needing to know about them, improving scalability and reliability.
  * **Idempotency:** Implement idempotency keys on `POST` requests to handle retries without creating duplicate records.

-----

### 3\. Showtime Inventory Initialization Flow

This is an automated, event-driven flow that ensures every new showtime has its own real-time seat inventory.

#### Flow Description

1.  A dedicated `ShowtimeInventoryInitializer` worker in the **Booking & Inventory Service** is a consumer listening for the `ShowtimeCreatedEvent` messages.
2.  Upon consuming a message, the worker makes a synchronous call to the **Events & Venues Service** to fetch the `seatLayout` for the specified `hallId`.
3.  The worker then constructs a single, comprehensive `JSONB` object (the `seatMap`). It iterates through every seat in the layout and initializes its `status` to `"available"`.
4.  Finally, the worker creates a single document in the `showtime_inventory` collection, with the `seatMap` as the main data field. This avoids creating thousands of individual seat records.

#### Simple DB Schema

  * **`showtime_inventory` collection**
      * `showtimeId` (string, primary key)
      * `seatMap` (JSON object)

#### Service-Level Optimizations

  * **Single-Document Atomic Updates:** Storing the entire seat map in a single `JSONB` document is a major performance optimization. Instead of updating hundreds of individual seat records, all status changes for a showtime happen on a single document, which is ideal for atomic transactions and concurrency control.
  * **Asynchronous Processing:** This entire flow happens in the background. The admin's request to create a showtime is not blocked while the inventory is being prepared, resulting in a faster user experience.

-----

### 4\. Event Discovery & Search Flow

This flow enables users to quickly find events by leveraging a specialized search engine.

#### Flow Description

1.  When a user lands on the homepage, the client application sends a `GET` request to `/api/v1/search/shows`, providing the user's `lat`, `lon`, and an optional text `query`.
2.  The **Search Service** is backed by **Elasticsearch** (or a similar engine). It does not use a traditional database. Instead, it relies on a denormalized, up-to-date index.
3.  The service translates the user's request into a powerful Elasticsearch query that combines a **`geo_distance` filter** for proximity and a **`multi_match` query** for full-text search across various fields (event name, venue name, keywords, etc.).
4.  The search results, already ranked by relevance and proximity, are returned to the client.

#### Elasticsearch Document Schema (Example)

```json
{
  "showtimeId": "uuid-123",
  "showtime": "2025-09-09T20:00:00Z",
  "event": {
    "name": "The Dark Knight Rises",
    "category": "Movie"
  },
  "venue": {
    "name": "Cineplex 101",
    "location": { "lat": 17.4431, "lon": 78.4772 }
  }
}
```

#### Service-Level Optimizations

  * **Denormalized Indexing:** The `SearchIndexer` worker proactively builds and updates the Elasticsearch index by fetching data from other services. This means the search service never needs to perform slow joins at query time.
  * **Geospatial Indexing:** Elasticsearch's built-in geospatial capabilities are highly optimized for "find nearby" queries, making the user's initial discovery experience near-instantaneous.
  * **Asynchronous Indexing:** Index updates are handled by a background worker, ensuring the primary Events & Venues service remains fast and responsive.

-----

### 5\. Seat Selection & Booking Flow

This is a multi-step, transactional flow that ensures a seat can only be booked by one person at a time.

#### Flow Description

1.  **View Inventory:** The client fetches the `seatMap` for a specific showtime from the **Booking & Inventory Service** via a `GET` request. The service returns the `JSONB` document from its `showtime_inventory` collection.
2.  **Reserve Seats:** When the user selects seats, the client sends a `POST` request to `/api/v1/showtimes/{showtimeId}/seats/reserve`. The Booking & Inventory Service performs an **atomic database transaction**.
3.  Inside the transaction, it checks that the selected seats are `"available"`, then updates their status to `"held"`, and sets a temporary `heldUntil` timestamp. This prevents other users from selecting the same seats.
4.  **Payment & Confirmation:** The service hands off payment processing to a separate **Payment Service**. The payment gateway notifies the Payment Service of the payment status via a **webhook**.
5.  Upon a successful payment webhook, the Payment Service calls an internal `PUT /api/v1/bookings/{bookingId}/confirm` endpoint on the Booking & Inventory Service. This triggers a final, atomic update to change the seat statuses from `"held"` to `"booked"` and creates a final booking record.

#### Simple DB Schema

  * **`bookings` collection**
      * `id` (string)
      * `showtimeId` (string)
      * `userId` (string)
      * `bookedSeats` (array of strings)
      * `totalAmount` (number)
      * `timestamp` (timestamp)

#### Service-Level Optimizations

  * **Concurrency Control:** The use of **database transactions** or distributed locks is critical to prevent race conditions during the seat reservation phase.
  * **Seat Hold Timeout:** A separate background process or scheduled job automatically releases seats that have been in the `"held"` state for too long.
  * **Asynchronous Webhooks:** Relying on webhooks for payment confirmation is a crucial optimization. It means the user doesn't have to wait for the payment gateway's response, and the booking system is resilient to network issues on the client side.

---
---
---
---

# High-Level Design: Ticket Booking System

This document outlines the core services for a scalable ticket booking platform. The architecture is based on a microservices approach, where each service is responsible for a specific domain of the application.

Core Services
1. User Service
Responsibility: Manages all user-related functions, including registration, authentication (login/logout), user profiles, and security.

2. Event & Venue Service
Responsibility: Handles event-related data, such as event details (name, description, date, time), and venue information (location, seating layout, capacity).

3. Search Service
Responsibility: Provides a fast and efficient search functionality, allowing users to discover events by various criteria like name, genre, location, and date.

4. Booking & Inventory Service
Responsibility: The core of the system; it manages the entire booking process, including temporary seat reservations, checking real-time availability, and confirming ticket allocations.

5. Payment Service
Responsibility: Facilitates secure payment processing by integrating with various payment gateways and handling transactions, refunds, and payment status.

6. Notification Service
Responsibility: Sends automated communications to users, such as booking confirmations, payment receipts, event reminders, and cancellations via email and SMS.

7. Reviews & Ratings Service
Responsibility: Manages user-generated content, allowing users to submit ratings and written reviews for events they have attended.

---

Venue and Hall Creation: Low-Level Details
This document details the low-level technical flow for creating a new venue and subsequently adding a hall or screen to it. This process is restricted to administrative users.

Venue Creation: POST /api/v1/venue
This endpoint is the entry point for an administrator to register a new physical location, such as a movie theater or an auditorium. The process involves multiple steps to ensure data integrity and proper storage.

Request Flow
API Gateway: The POST request, containing the venue data, first hits the API Gateway. The gateway's primary role is to act as a single, secure entry point. It performs initial validation, authenticates the administrative user, and then routes the request to the Events & Venues microservice.

Service Validation: Upon receiving the request, the Events & Venues service performs several layers of validation:

Schema Validation: It verifies that the request body conforms to the expected JSON schema, ensuring all required fields like name, address, and location are present and correctly formatted.

Business Rule Validation: It checks for business-specific rules, such as ensuring the venue name is unique within a given city to prevent duplicates.

Data Persistence: Once validated, the service prepares to persist the data.

A unique identifier (venueId) is generated, typically a Universally Unique Identifier (UUID), to ensure it's globally unique across the system.

The service then constructs a new record for the Venues table.

The imageUrls array is processed. The images themselves are not part of the request payload; they have been pre-uploaded by the admin to a dedicated S3 bucket, and only their unique object keys are sent in the request. The service stores these S3 keys in the database record.

Database Schema: Venues Table
This table stores the high-level information for each venue.
| Column | Data Type | Description |
| :--- | :--- | :--- |
| id | VARCHAR(36) | Unique identifier (UUID). Primary Key. |
| name | VARCHAR(255) | Name of the venue. |
| address | JSONB | A structured JSON object containing street, city, district, state, zip, etc. Storing this as JSONB allows for flexible schema. |
| location | JSONB | Latitude and longitude coordinates. |
| imageUrls | VARCHAR(255)[] | An array of S3 object keys. |

Hall/Screen Creation: POST /api/v1/venue/{venueId}/hall
After a venue is created, an administrator can define the individual halls or screens within it. This process links the new hall to an existing venue.

Request Flow
Endpoint Routing: The request is routed to the Events & Venues service, with the venueId extracted directly from the URL path.

Parent Venue Validation: The service first checks if a venue with the provided venueId actually exists in the Venues table. If not, it returns a 404 Not Found error.

Data Persistence: A unique UUID is generated for the new hall. The service then inserts a new record into the Halls table, referencing the parent venue.

Database Schema: Halls Table
This table stores the details for each hall or screen.
| Column | Data Type | Description |
| :--- | :--- | :--- |
| id | VARCHAR(36) | Unique identifier (UUID). Primary Key. |
| venueId | VARCHAR(36) | Foreign Key referencing Venues.id. |
| hallName | VARCHAR(255) | The name of the hall or screen. |
| seatLayout | JSONB | A structured JSON object defining the seating arrangement. |

Detailed seatLayout Structure
The seatLayout is a critical part of the hall record, stored as a flexible JSONB data type to accommodate various seating configurations. It is a hierarchical structure that defines every seat, row, and section.

{
  "sections": [
    {
      "id": "premium-section-1",
      "name": "Premium",
      "rows": [
        {
          "label": "A",
          "seats": [
            { "id": "A1", "label": "A1", "coordinates": { "x": 10, "y": 20 }, "seatType": "premium" },
            { "id": "A2", "label": "A2", "coordinates": { "x": 30, "y": 20 }, "seatType": "premium" }
          ]
        },
        {
          "label": "B",
          "seats": [
            { "id": "B1", "label": "B1", "coordinates": { "x": 10, "y": 40 }, "seatType": "premium" },
            { "id": "B2", "label": "B2", "coordinates": { "x": 30, "y": 40 }, "seatType": "premium" }
          ]
        }
      ]
    },
    {
      "id": "regular-section-1",
      "name": "Regular",
      "rows": [
        {
          "label": "C",
          "seats": [
            { "id": "C1", "label": "C1", "coordinates": { "x": 10, "y": 60 }, "seatType": "standard" },
            { "id": "C2", "label": "C2", "coordinates": { "x": 30, "y": 60 }, "seatType": "standard" }
          ]
        }
      ]
    }
  ]
}

This JSON structure is flexible enough to handle complex layouts, including different sections and specific seat metadata like seatType (e.g., 'premium', 'standard', 'wheelchair accessible'). This data is later used by the Booking Service to render an interactive seat map for users.

---

Detailed Design: Booking & Inventory Service
The Booking & Inventory Service is a mission-critical microservice responsible for managing real-time seat availability, handling the booking lifecycle, and ensuring transactional integrity. Its primary function is to prevent double-booking and provide a seamless reservation experience for users.

Core Concepts
Inventory: The service maintains a live inventory for every single showtime. This inventory is a list of all seats in a given hall, each with a specific status.

Seat States: To manage concurrency, seats have several distinct states:

Available: The seat is open for reservation.

Held: A user has selected the seat and is in the process of booking. The seat is locked for a short period (e.g., 5-10 minutes) to prevent other users from selecting it.

Booked: The booking is complete, payment has been confirmed, and the ticket has been issued.

Booking Lifecycle: A booking moves through several states, from an initial reservation to a final, confirmed ticket.

Data Models
The service will primarily manage two key entities: ShowtimeInventory and Booking.

ShowtimeInventory: This entity represents the real-time status of all seats for a specific showtime. It is initialized by fetching the seatLayout from the Events & Venues Service.

showtimeId (string): The unique identifier for the showtime. Primary Key.

lastUpdated (timestamp): The last time the inventory was modified.

seatMap (JSONB): A flexible object representing the seats. Each seat is an object containing its unique seatId, current status (e.g., 'available', 'held', 'booked'), and holderId (if held).

{
  "seats": [
    {
      "seatId": "A1",
      "status": "available"
    },
    {
      "seatId": "A2",
      "status": "held",
      "holderId": "user-abc-123",
      "heldUntil": "2025-09-09T23:45:00Z"
    },
    {
      "seatId": "A3",
      "status": "booked",
      "bookingId": "booking-xyz-789"
    }
  ]
}

Booking: This entity represents a confirmed transaction.

bookingId (string): A unique identifier for the booking. Primary Key.

showtimeId (string): Foreign key linking to the ShowtimeInventory.

userId (string): The unique identifier of the user who made the booking.

bookedSeats (array): A list of seatIds that were booked in this transaction.

totalAmount (float): The final total price of the booking.

paymentStatus (enum): The status of the payment (e.g., 'pending', 'paid', 'failed').

timestamp (timestamp): The time the booking was confirmed.

API Endpoints
Method

Endpoint

Description

GET

/api/v1/showtimes/{showtimeId}/inventory

Retrieves the real-time seat availability for a specific showtime. This is a read-only endpoint used by the frontend to render the seat map.

POST

/api/v1/showtimes/{showtimeId}/seats/reserve

Initiates a seat reservation. A user sends a list of desired seatIds. The service locks these seats by setting their status to 'held'.

POST

/api/v1/bookings

Finalizes a booking. The user sends a request to confirm the held seats. The service verifies the hold, communicates with the Payment Service, and if successful, updates the seat statuses to 'booked'.

GET

/api/v1/bookings/{bookingId}

Retrieves a detailed record of a specific booking.

PUT

/api/v1/bookings/{bookingId}/cancel

Allows a user or admin to cancel a booking. The service will release the seats back to 'available'.

Key Responsibilities and Transactional Flow
Inventory Management: The service acts as the source of truth for all seat statuses.

Concurrency Control: When a reserve request is received, the service uses a database transaction or a distributed lock mechanism to atomically check the seat status and update it to 'held', preventing race conditions.

Hold Timeouts: A background worker or scheduled job periodically scans for seats that have been in the 'held' state for longer than the timeout period and releases them back to 'available'.

Payment Integration: After a successful reservation, the service communicates with a dedicated Payment Service to process the payment. This communication should be idempotent to handle retries safely.

Booking Confirmation: Only after the Payment Service confirms a successful payment will the Booking & Inventory Service finalize the transaction by creating a new Booking record and changing the seats' status to 'booked'.

Interaction with Other Services
Events & Venues Service: The Booking Service calls this service to get the seatLayout of a hall when a new showtime is created, which it then uses to initialize its ShowtimeInventory.

Payment Service: This is the most crucial interaction. The Booking Service hands off the payment details to this service and awaits a success or failure response.

User Service: When a booking is finalized, the userId is stored, linking the transaction to the user's profile.

---

Showtime Inventory Initialization: Low-Level Flow
This document details the precise, event-driven process by which a dedicated worker in the Booking & Inventory Service initializes the seat inventory for a newly created showtime.

Event-Driven Architecture
The entire process is triggered by an event message, ensuring that the creation of a showtime and the initialization of its inventory are decoupled and resilient.

Event Publishing: When an administrator successfully creates a new showtime via the POST /api/v1/events/{eventId}/showtimes endpoint in the Events & Venues Service, this service immediately publishes a message to a message queue (e.g., RabbitMQ, Kafka, Pub/Sub). The message should contain key identifiers, such as:

showtimeId

hallId

Worker Listening: A dedicated background worker within the Booking & Inventory Service, which we'll call the ShowtimeInventoryInitializer, is constantly listening for new messages on this queue.

The Worker's Process
Once the ShowtimeInventoryInitializer consumes a ShowtimeCreatedEvent message, it performs the following steps:

Retrieve Hall Layout: The worker uses the hallId from the message to make a synchronous, internal API call to the Events & Venues Service. Its goal is to retrieve the canonical seatLayout for that specific hall.

Initialize Seat Map: Using the retrieved seatLayout, the worker dynamically constructs a new seatMap JSON object. It iterates through every section, row, and seat, creating a new entry for each. The initial status of every seat is set to "available".

Create Inventory Record: The worker then inserts a new record into its ShowtimeInventory table. This record serves as the single source of truth for the showtime's seat status.

showtimeId (from the message)

lastUpdated (current timestamp)

seatMap (the newly constructed JSONB object)

This design is highly efficient because it creates only one record per showtime, regardless of the number of seats. All seat status updates will happen on this single record, making transactions faster and more atomic. It is far more scalable than creating a separate record for every single seat.

Example seatMap Initialization
Based on the seatLayout received from the Events & Venues Service:

// Retrieved from Events & Venues Service
{
  "sections": [
    {
      "name": "Regular",
      "rows": [
        {
          "label": "A",
          "seats": [
            { "id": "A1" },
            { "id": "A2" }
          ]
        }
      ]
    }
  ]
}

// Initialized JSONB object in ShowtimeInventory table
{
  "seats": [
    {
      "seatId": "A1",
      "status": "available",
      "holderId": null,
      "heldUntil": null
    },
    {
      "seatId": "A2",
      "status": "available",
      "holderId": null,
      "heldUntil": null
    }
  ]
}

This streamlined process ensures that the inventory is created automatically and efficiently as soon as a show is published, ready for users to start reserving seats.

---

Detailed Design: Search Service & Event Discovery
The Search Service is a dedicated microservice responsible for providing fast, relevant, and comprehensive search capabilities. It is the primary interface for users to discover events and shows, leveraging Elasticsearch as its core technology for high-performance, full-text, and geospatial queries.

1. Data Ingestion & Indexing Pipeline
This is the most critical component. Unlike a traditional database, Elasticsearch requires data to be ingested and indexed from other services. This process must be event-driven to ensure the search index is always up-to-date with the source of truth.

Event Publishing: When any of the following events occur in the Events & Venues Service, a message is published to a message queue:

ShowtimeCreatedEvent (most frequent)

EventUpdatedEvent (e.g., a change to the event name or image)

VenueUpdatedEvent (e.g., a change to the venue's address or coordinates)

Indexing Worker: A dedicated background worker, the SearchIndexer, consumes these messages. Upon receiving a message, it performs the following:

Denormalization: It makes API calls to the Events & Venues Service to fetch all the necessary details for the showtime, event, and venue. This process is crucial because the final Elasticsearch document must be a single, denormalized record.

Document Creation: It creates a new document for the Elasticsearch index. This document is a flattened representation of a showtime with all relevant information pre-joined.

Indexing: It then sends this document to Elasticsearch's _index endpoint.

Elasticsearch Document Structure
To support fast and efficient searching, the Elasticsearch document for each showtime will look like this:

{
  "showtimeId": "uuid-123",
  "showtime": "2025-09-09T20:00:00Z",
  "event": {
    "eventId": "event-abc",
    "name": "The Dark Knight Rises",
    "category": "Movie",
    "imageUrl": "s3-key-123",
    "keywords": ["movie", "action", "batman", "superhero"]
  },
  "venue": {
    "venueId": "venue-xyz",
    "name": "Cineplex 101",
    "city": "Hyderabad",
    "address": {
      "street": "123 Main St",
      "district": "Madhapur"
    },
    "location": {
      "lat": 17.4431,
      "lon": 78.4772
    }
  }
}

2. The Search API & Logic
The Search Service exposes a single, powerful API endpoint that handles all user-facing queries.

Endpoint: GET /api/v1/search/shows

Query Parameters:

lat (number): User's latitude. Required for location-based search.

lon (number): User's longitude. Required for location-based search.

distance (string): The search radius, e.g., "10km". Defaults to a reasonable distance.

query (string): Optional. Keywords for full-text search (e.g., "Batman", "concerts").

startDate (date): Optional. Filters shows on or after this date.

endDate (date): Optional. Filters shows on or before this date.

Search Service Logic (Elasticsearch Query):
The service constructs a bool query in Elasticsearch to combine different search criteria.

Location Filter (geo_distance): This is the key component for finding nearby shows. It efficiently finds all documents where the venue.location field is within the specified distance of the provided lat and lon.

Keyword Match (multi_match): If a query is provided, a multi_match query is used to search across multiple fields simultaneously (e.g., event.name, event.keywords, venue.name, venue.city).

Date Range Filter (range): If startDate or endDate are provided, a range query is used on the showtime field to filter for shows within the specified time period.

3. User Interface (UI) Flow
Geolocation Request: When the user lands on the homepage, the client-side application requests the user's location using the browser's Geolocation API.

Manual Input (Fallback): If the user denies the geolocation request, a fallback UI element (e.g., a city search box) is shown, allowing them to manually enter their location.

API Call: The client makes a GET request to the Search Service with the user's coordinates.

Display Results: The Search Service returns a list of matching showtime documents. The client-side application renders this data in a user-friendly format, displaying event posters, titles, venue names, and showtimes, ordered by proximity or a relevance score.

---

Your proposed flow is excellent; it captures the essential steps of a real-world seat booking system. You've correctly identified the need for a temporary hold, the role of a payment gateway, and the final booking confirmation.

To make this design production-ready, we'll refine the process by adding a few critical low-level details. The key improvements will be to formalize the transactional nature of the seat hold, introduce a dedicated Payment Service to keep our microservices decoupled, and detail the asynchronous payment confirmation via webhooks.

-----

### Phase 1: Seat Selection

This phase is about presenting the user with the real-time seat map and allowing them to choose their seats.

1.  **Fetch Inventory:** The client application makes a `GET` request to the **Booking & Inventory Service** to retrieve the `ShowtimeInventory` for the selected showtime.

      * **Endpoint:** `GET /api/v1/showtimes/{showtimeId}/inventory`
      * **Response:** The service returns the `showtimeId` and the `seatMap` JSON object, which contains the live status of every seat (available, held, or booked).

2.  **Render UI:** The client uses the `seatMap` to render a visual representation of the hall. Seats with a `status` of `"available"` are clickable, while those that are `"held"` or `"booked"` are disabled.

-----

### Phase 2: Seat Hold (The Transactional Core)

This is the most critical step, where we prevent other users from booking the same seat. This must be an **atomic operation**.

1.  **Request to Reserve:** When the user selects their seats and clicks "Proceed," the client sends a `POST` request to the **Booking & Inventory Service**.

      * **Endpoint:** `POST /api/v1/showtimes/{showtimeId}/seats/reserve`
      * **Request Body:**
        ```json
        {
          "userId": "user-abc-123",
          "seatIds": ["A1", "A2", "A3"],
          "holdTimeoutSeconds": 600 // 10 minutes
        }
        ```

2.  **Atomic Lock & Update:**

      * The Booking & Inventory Service uses a **database transaction** or a **distributed lock** on the `ShowtimeInventory` record to ensure only one request can modify the seat map at a time.
      * Inside the lock, the service checks if all requested seats are `status: "available"`.
      * If they are, it updates the status of each selected seat to `"held"`, sets the `holderId` to the `userId`, and sets a `heldUntil` timestamp.
      * A unique `bookingId` is generated and returned to the client. This ID represents the hold, not a finalized booking.

3.  **Response:** The service returns a success message to the client, including the `bookingId` and the calculated `finalAmount`.

-----

### Phase 3: Payment Initiation

With the seats successfully held, the user can now proceed to payment.

1.  **Request for Payment Intent:** The client sends a `POST` request to the **Booking & Inventory Service** to begin the payment process.

      * **Endpoint:** `POST /api/v1/bookings/{bookingId}/preparePayment`
      * **Request Body:**
        ```json
        {
          "finalAmount": 45.00,
          "currency": "USD"
        }
        ```

2.  **Service-to-Service Communication:** The Booking & Inventory Service **does not** talk directly to the payment gateway. Instead, it calls a dedicated **Payment Service**.

      * The Payment Service generates a `paymentIntent` and `clientSecret` from the payment gateway (e.g., Stripe, Braintree).
      * It also registers a **webhook listener** with the payment gateway, so the gateway can notify our system when the payment is complete.

3.  **Return to Client:** The Booking & Inventory Service returns the `paymentIntent` and `clientSecret` from the Payment Service back to the client.

### Phase 4: Client-Side Payment & Webhook Notification

This is where the user enters their payment information.

1.  **Render Payment UI:** The client uses the `clientSecret` to render the secure payment form from the payment gateway's SDK.

2.  **Payment Completion:** When the user successfully pays, the payment gateway's SDK notifies the client of success, but more importantly, it sends an asynchronous, server-to-server **webhook notification** to our Payment Service's dedicated webhook endpoint.

-----

### Phase 5: Booking Finalization (Asynchronous)

This is the final, non-blocking step that converts the temporary hold into a permanent booking.

1.  **Webhook Handler:** The **Payment Service's** webhook handler receives the notification from the payment gateway.

      * It verifies the webhook's signature to ensure authenticity.
      * It extracts the `paymentIntentId` and the `status` (e.g., `"succeeded"`).

2.  **Confirmation API Call:** The Payment Service then makes an internal API call to the **Booking & Inventory Service** to confirm the booking.

      * **Endpoint:** `PUT /api/v1/bookings/{bookingId}/confirm`
      * **Request Body:**
        ```json
        {
          "paymentStatus": "paid"
        }
        ```

3.  **Finalizing the Booking:**

      * The Booking & Inventory Service updates the `seatMap` for the `showtimeId`, changing the `status` of the seats from `"held"` to `"booked"`.
      * It then creates a new, permanent record in the **`Bookings` table**. This record now represents a confirmed, non-refundable ticket.

4.  **Notification:** The Booking & Inventory Service can now send a final confirmation to the user (e.g., a push notification or email).
---
---
