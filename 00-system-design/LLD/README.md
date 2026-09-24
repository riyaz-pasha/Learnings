# ✅ LLD Interview Prep – Markdown Checklist (5 Weeks)

---

## 🗓️ **Week 1 – OOP, SOLID, Design Patterns**

| Day   | Problem            | ✅ Status | Prompt                                                                                                                                                                                                     |
| ----- | ------------------ | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | Parking Lot System | \[ ]     | `Design a scalable Parking Lot system in OOP style. Include vehicle types, slot types, entry/exit, pricing strategies, and support multiple levels. Use proper abstractions and design for extensibility.` |
| Day 2 | Tic Tac Toe Game   | \[ ]     | `Design a modular and extensible Tic Tac Toe game using OOP principles. Support multiple players, game board size, win validation, and separation of concerns.`                                            |
| Day 3 | Vending Machine    | \[ ]     | `Design a vending machine using OOP and design patterns. Include item selection, coin insertion, change return, and inventory management. Emphasize extensibility and code reuse using Factory pattern.`   |
| Day 4 | Snake and Ladder   | \[ ]     | `Design a Snake and Ladder game using object-oriented design. Support multiple players, snakes/ladders, dice, and game rules. Focus on class responsibilities and modularity.`                             |
| Day 5 | Review & Diagrams  | \[ ]     | `Generate class and sequence diagrams for Parking Lot and Snake & Ladder systems. Improve based on SOLID principles and design patterns.`                                                                  |

---

## 🗓️ **Week 2 – Concurrency, Patterns, UML**

| Day   | Problem              | ✅ Status | Prompt                                                                                                                                                                                                    |
| ----- | -------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | Movie Ticket Booking | \[ ]     | `Design a Movie Ticket Booking system (BookMyShow) with seat selection, lock mechanism, shows, theaters, payments. Handle concurrency in seat booking. Use proper class design and thread-safe patterns.` |
| Day 2 | Notification System  | \[ ]     | `Design a Notification Service to send messages via SMS, Email, Push. Use the Observer or Strategy pattern to support pluggable channels and retry mechanism.`                                            |
| Day 3 | Elevator System      | \[ ]     | `Design an Elevator System for a building with multiple floors. Use OOP and design patterns. Consider elevator states, requests, and scheduling.`                                                         |
| Day 4 | Cab Booking (Uber)   | \[ ]     | `Design a cab booking system like Uber. Support driver-rider matching, location updates, trip management, and surge pricing. Focus on modular, testable, and extensible design.`                          |
| Day 5 | UML + Review         | \[ ]     | `Generate UML diagrams and evaluate concurrency control for the Movie Booking and Elevator systems. Highlight critical sections and lock usage.`                                                          |

---

## 🗓️ **Week 3 – Caching, Storage, Rate Limiting**

| Day   | Problem                | ✅ Status | Prompt                                                                                                                                                   |
| ----- | ---------------------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Day 1 | LRU/LFU Cache          | \[ ]     | `Implement an LRU cache system using OOP. Design for extensibility so eviction strategy (e.g., LFU, FIFO) can be swapped using Strategy pattern.`        |
| Day 2 | File System            | \[ ]     | `Design a simplified in-memory file system with support for folders, files, create/read/delete operations. Structure using Composite pattern.`           |
| Day 3 | API Rate Limiter       | \[ ]     | `Design a Rate Limiter service for APIs using OOP. Support multiple algorithms like fixed window, sliding window, and token bucket. Handle concurrency.` |
| Day 4 | Dropbox / File Sharing | \[ ]     | `Design a simplified Dropbox-like file storage and sharing system. Include file versioning, metadata, user access control, and sharing mechanism.`       |
| Day 5 | Interface Review       | \[ ]     | `Refactor and create clean interfaces for Cache, FileSystem, and RateLimiter. Focus on code modularity, testability, and loose coupling.`                |

---

## 🗓️ **Week 4 – Product-Like Features**

| Day   | Problem          | ✅ Status | Prompt                                                                                                                                           |
| ----- | ---------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Day 1 | Amazon Cart      | \[ ]     | `Design an e-commerce cart system like Amazon. Include item addition, coupon application, cart merging, pricing logic, and concurrency support.` |
| Day 2 | Splitwise        | \[ ]     | `Design a Splitwise-like expense management system. Support expense splits, group settlements, debt simplification, and history tracking.`       |
| Day 3 | Instagram Feed   | \[ ]     | `Design a feed system like Instagram or Twitter. Support followers, post visibility, real-time ranking, pagination. Focus on modular design.`    |
| Day 4 | YouTube          | \[ ]     | `Design a simplified YouTube-like video hosting system. Support uploads, video metadata, thumbnails, search, and basic streaming logic.`         |
| Day 5 | Trade-off Review | \[ ]     | `Compare designs of Amazon Cart and Instagram Feed. List trade-offs for consistency, latency, and availability. Refactor code for readability.`  |

---

## 🗓️ **Week 5 – Patterns, Scaling, Mock Interviews**

| Day   | Problem                           | ✅ Status | Prompt                                                                                                                                     |
| ----- | --------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Day 1 | Strategy-based Cache              | \[ ]     | `Refactor LRU Cache to use the Strategy Pattern so the eviction policy can be dynamically injected (e.g., LFU, FIFO, custom).`             |
| Day 2 | Decorator + Limiter               | \[ ]     | `Implement a Decorator-based Rate Limiter system that wraps APIs and enforces token-bucket or fixed-window rate control.`                  |
| Day 3 | Payment Gateway (Stripe/Razorpay) | \[ ]     | `Design a simplified payment gateway like Stripe. Include payment methods, webhooks, retries, settlements, and error handling.`            |
| Day 4 | Messaging Queue                   | \[ ]     | `Design a lightweight message queue like Kafka. Support publish/subscribe, topics, partitions, and acknowledgments. Focus on scalability.` |
| Day 5 | Mock Interviews                   | \[ ]     | `Run a mock interview simulation for Cab Booking and Rate Limiter. Evaluate clarity, trade-off explanation, and UML usage.`                |

---

#### 1. Design a Parking Lot

* **Requirements:**
    * The parking lot can have multiple floors and different types of parking spots (e.g., small, medium, large).
    * It should support different vehicle types (e.g., car, motorcycle, truck).
    * The system should be able to park a vehicle, unpark a vehicle, and show available spots.
    * It should handle ticket generation and payment.

* **Plan:**
    * **Entities:** `ParkingLot`, `Floor`, `ParkingSpot` (with subtypes for different sizes), `Vehicle` (with subtypes), `Ticket`, `Gate` (for entry and exit), `Payment`.
    * **Relationships:** `ParkingLot` contains `Floors`. Each `Floor` has many `ParkingSpots`. `Ticket` is associated with a `Vehicle` and a `ParkingSpot`. `Payment` processes the `Ticket`.
    * **Design Patterns:**
        * **Strategy:** Use a `ParkingStrategy` interface to define different parking logic (e.g., `FirstAvailableStrategy`).
        * **Factory:** A `VehicleFactory` could create different types of `Vehicle` objects.

#### 2. Design a Vending Machine

* **Requirements:**
    * The machine should have a selection of products with prices and quantities.
    * It should accept currency (e.g., coins, notes).
    * The user can select a product, insert money, and receive the product and change.
    * It should handle out-of-stock products and insufficient funds.

* **Plan:**
    * **Entities:** `VendingMachine`, `Product`, `Coin`, `Note`, `Inventory`, `State` (e.g., `IdleState`, `SelectionState`, `DispensingState`).
    * **Relationships:** `VendingMachine` has an `Inventory` of `Products` and a list of accepted `Coins` and `Notes`. The machine's behavior is defined by its `State`.
    * **Design Patterns:**
        * **State:** This is the most crucial pattern here. The vending machine's behavior changes based on its current state.
        * **Singleton:** The `VendingMachine` could be a Singleton to ensure only one instance.

#### 3. Design a Movie Ticket Booking System

* **Requirements:**
    * Users can search for movies by name, genre, etc.
    * They can view showtimes and available seats for a specific movie and theater.
    * Users can select seats and book tickets.
    * The system should handle concurrency and prevent overbooking.

* **Plan:**
    * **Entities:** `Movie`, `Theater`, `Showtime`, `Seat`, `Booking`, `User`.
    * **Relationships:** `Movie` can be shown in multiple `Theaters`. Each `Theater` has many `Showtime`s. A `Showtime` has a collection of `Seats`. A `Booking` is associated with a `User`, a `Showtime`, and specific `Seats`.
    * **Concurrency:** Use a `Lock` or similar mechanism to ensure that when a seat is being selected or booked, it's not available to other users.
    * **Design Patterns:**
        * **Observer:** A `SeatObserver` could notify clients if a seat becomes unavailable.
        * **Facade:** A `BookingFacade` could provide a simple API for users to interact with the complex booking process.

#### 4. Design a Caching System (e.g., LRU Cache)

* **Requirements:**
    * The cache should have a fixed capacity.
    * It should support `get(key)` and `put(key, value)` operations.
    * When the cache is full and a new item needs to be added, the Least Recently Used (LRU) item should be evicted.

* **Plan:**
    * **Data Structures:**
        * **`HashMap`:** To store key-value pairs for quick lookups ($O(1)$). The value in the hash map will be a reference to a node in a linked list.
        * **`Doubly LinkedList`:** To maintain the order of usage. The most recently used items are at the head, and the least recently used are at the tail.
    * **Plan:**
        * `get(key)`: If the key exists in the `HashMap`, move its corresponding node to the head of the `Doubly LinkedList` and return the value. If not, return `null`.
        * `put(key, value)`: If the key already exists, update its value and move the node to the head. If it's a new key, check if the cache is full. If so, evict the tail node from both the `HashMap` and the `Doubly LinkedList`. Then, add the new node to the head and the `HashMap`.

#### 5. Design a Rate Limiter

* **Requirements:**
    * Limit the number of requests a user can make to a service within a given time window (e.g., 100 requests per minute).
    * The system should be scalable and work in a distributed environment.

* **Plan:**
    * **Algorithm:** The most common approach is the **Token Bucket Algorithm**.
        * A bucket has a fixed capacity.
        * Tokens are added to the bucket at a fixed rate.
        * Each incoming request consumes a token.
        * If the bucket is empty, the request is rejected.
    * **Plan:**
        * Use a distributed cache like Redis to store the number of tokens for each user.
        * A background process can add tokens to each user's bucket at a regular interval.
        * When a request comes in, check the user's token count in the cache. If it's greater than 0, decrement it and allow the request. Otherwise, reject it.
