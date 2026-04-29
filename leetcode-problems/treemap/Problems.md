# TreeMap & TreeSet — Interview Problem Set

> 20 unique real-world problems covering ordered maps, range queries, floor/ceiling operations, and streaming data.

---

## Mental Model: When to Reach for TreeMap/TreeSet

| Trigger Phrase | Operation | Example Pattern |
|---|---|---|
| "Find the nearest X" | `floor()` / `ceiling()` | Matchmaking, dispatch, anomaly detection |
| "All elements in range [a,b]" | `subMap()` / `headMap()` / `tailMap()` | Alerts, log windowing, calendar overlap |
| "Min/max of a dynamic set" | `firstKey()` / `lastKey()` | LFU cache, auction, scheduler |
| "Maintain sorted order" | Sorted histogram | Sliding median, leaderboard, duplicates |
| "Best fit among options" | `ceilingKey(X)` | Bin packing, dispatch, discount bands |
| "Value at a past timestamp" | `floorEntry(t)` | Time-series, versioned KV stores |

> **The most common mistake:** Reaching for `PriorityQueue` because it feels like a "find closest" problem. A heap gives you the global min/max — not the element nearest to a *specific target*. That's the floor/ceiling distinction. A `HashMap` loses ordering entirely.

---

## Quick API Reference

```java
floorKey(k)       // Largest key ≤ k
ceilingKey(k)     // Smallest key ≥ k
lowerKey(k)       // Largest key < k  (strictly)
higherKey(k)      // Smallest key > k (strictly)
firstKey()        // Global minimum key
lastKey()         // Global maximum key
subMap(a, b)      // Keys in range [a, b)
headMap(k)        // Keys < k
tailMap(k)        // Keys ≥ k
floorEntry(k)     // Map.Entry for largest key ≤ k
firstEntry()      // Map.Entry for minimum key
pollFirstEntry()  // Remove and return minimum entry
```

---

## Problems

---

### 01. Real-Time Player Matchmaking

**Difficulty:** Hard | **Patterns:** Floor/Ceiling, Real-Time

**Problem:** Players join a queue with Elo ratings. Match each new player with the closest-rated available opponent within ±50. If multiple opponents are equally close, pick the one waiting longest. Once matched, both are removed.

**Why TreeMap?** A heap only gives the global min/max. A HashMap loses ordering. `TreeMap` gives `floorKey` and `ceilingKey` in O(log n), making nearest-neighbor queries fast on a constantly changing set.

**Approach:** `TreeMap<Integer, Queue<Player>>` rating→waiting players. For player with rating R, check `floorKey(R)` and `ceilingKey(R)`. Pick whichever is within ±50 and closer; on tie prefer the lower rating. Match and remove. If no match, insert the player.

**Key Operations:** `floorKey(r)`, `ceilingKey(r)`, `put`, `remove`

---

### 02. Ride Allocation — Nearest Driver Dispatch

**Difficulty:** Hard | **Patterns:** Floor/Ceiling, Real-Time

**Problem:** Drivers register at positions on a 1D road. When a rider requests pickup at position X, assign the closest available driver. Drivers go offline during trips and rejoin later.

**Why TreeSet?** Same nearest-neighbor pattern as matchmaking — `floor(X)` and `ceiling(X)` together give you the two candidates in O(log n). Compare distances, break ties by taking the higher value.

**Approach:** `TreeSet<Integer>` of available positions. On request at X: `lo = floor(X)`, `hi = ceiling(X)`, pick closer, remove from set. On return, add back. If multiple drivers share a position, use `TreeMap<Integer, Queue<Driver>>`.

**Key Operations:** `floor(x)`, `ceiling(x)`, `add`, `remove`

---

### 03. Meeting Room Availability Checker

**Difficulty:** Medium | **Patterns:** Floor/Ceiling, Scheduling

**Problem:** Given booked meeting intervals, determine if a new interval `[start, end]` is free. Support: book a slot, cancel a booking, query how many bookings overlap `[a, b]`.

**Why TreeMap?** `floorKey(start)` finds the booking that starts just before yours; `ceilingKey(start)` finds the one just after. Only two lookups needed to determine any overlap — no scanning.

**Approach:** `TreeMap<Integer, Integer>` start→end. For new interval `[s, e]`: `floorKey(s)` → check if its end > s (left overlap); `ceilingKey(s)` → check if it starts < e (right overlap). If neither, it's free. Book = `put(s, e)`, cancel = `remove(s)`.

**Key Operations:** `floorKey(s)`, `ceilingKey(s)`, `subMap(a, b)`, `put`, `remove`

---

### 04. Sliding Window Median

**Difficulty:** Hard | **Patterns:** Streaming, Order Statistics

**Problem:** Given a stream of integers and window size K, output the median of the last K elements after each insertion.

**Why TreeMap?** Two TreeMaps acting as an order-statistic structure support O(log n) insert/delete and O(1) median access. A heap can't efficiently delete arbitrary elements leaving the window.

**Approach:** Two `TreeMap<Integer, Integer>` value→count: `lo` (max-half) and `hi` (min-half). Keep `|lo.size - hi.size| ≤ 1`. Median = `lo`'s max when sizes differ, else average of `lo`'s max and `hi`'s min. On slide-out, decrement count and rebalance.

**Key Operations:** `firstKey()`, `lastKey()`, `pollFirstEntry()`, `put(v, count)`

---

### 05. Stock Price Range Alert System

**Difficulty:** Medium | **Patterns:** Range Query, Floor/Ceiling

**Problem:** Users set price alerts like "notify me if stock crosses below $150 or above $200." Given a real-time price feed, trigger all matching alerts efficiently.

**Why TreeMap?** Alerts are thresholds — `headMap`/`tailMap` gives all alerts below or above the current price in O(log n + k) where k is the number triggered.

**Approach:** `TreeMap<Double, List<Alert>>` threshold→alerts. Price drops to P → trigger `headMap(P, inclusive)`. Price rises to P → trigger `tailMap(P, inclusive)`. Remove triggered alerts. Use `subMap` for band-based alerts.

**Key Operations:** `headMap(p, true)`, `tailMap(p, true)`, `subMap(lo, hi)`, `firstEntry()`

---

### 06. LFU Cache (Least Frequently Used Eviction)

**Difficulty:** Hard | **Patterns:** Caching, Ranking

**Problem:** Design a cache that evicts the least frequently used item. On ties, evict the least recently used. Support `get(key)` and `put(key, value)` in O(log n).

**Why TreeMap?** `TreeMap<Integer, LinkedHashSet<Integer>>` freq→keys always exposes the minimum-frequency bucket via `firstEntry()`. A heap can't efficiently support arbitrary-frequency updates.

**Approach:** `TreeMap<Integer, LinkedHashSet>` freq→keySet. `HashMap` key→value and key→freq. On access: move key from old freq bucket to freq+1 bucket; delete old bucket if empty. Evict = `firstEntry().getValue().iterator().next()`.

**Key Operations:** `firstKey()`, `firstEntry()`, `remove(freq)`, `put(freq+1, set)`

---

### 07. Live Leaderboard with Rank Queries

**Difficulty:** Medium | **Patterns:** Ranking, Range Query

**Problem:** Players update scores. Support: update score, get rank (1 = highest), get top-K players, get all players in score range [a, b].

**Why TreeMap?** Auto-sorted scores + `tailMap` for rank counting. No re-sorting needed on each update; insertion and rank queries are both O(log n).

**Approach:** `TreeMap<Integer, Set<String>>` score→players. `HashMap` player→score for O(1) lookup. Update = remove from old bucket, add to new. Rank = total players with score > current = sum of sizes in `tailMap(score, false)`. Top-K = iterate `descendingMap()`.

**Key Operations:** `tailMap(score, false)`, `descendingMap()`, `lastEntry()`

---

### 08. Temperature Anomaly Detector

**Difficulty:** Medium | **Patterns:** Streaming, Range Query

**Problem:** A sensor emits readings. A reading is anomalous if it differs from all readings in the last K values by more than D degrees. Flag anomalies in real time.

**Why TreeMap?** `floor(T)` and `ceiling(T)` in the current window give the two nearest readings in O(log n). Without an ordered structure, every new reading requires O(K) comparison.

**Approach:** `TreeMap<Double, Integer>` temp→count (count handles duplicates). `Queue` to track insertion order. For new reading T, check `floor(T)` and `ceiling(T)`. If `abs(closest − T) > D`, it's anomalous. Add T, evict oldest from queue and map.

**Key Operations:** `floor(t)`, `ceiling(t)`, `put(t, count)`, `remove`

---

### 09. IP Address Range Router

**Difficulty:** Medium | **Patterns:** Range Query, Floor/Ceiling

**Problem:** A router stores rules as IP ranges `[startIP, endIP]` → destination. Given an incoming IP, find the matching rule. Rules are added and removed dynamically.

**Why TreeMap?** Find the largest `startIP ≤ incoming IP`, then verify `incomingIP ≤ that rule's endIP`. A HashMap requires probing all rules; TreeMap does it in one `floorEntry` call.

**Approach:** `TreeMap<Long, long[]>` startIP → `[endIP, destination]`. For IP X: `entry = floorEntry(X)`. If entry exists and `X ≤ entry.getValue()[0]`, route found. Else no route. Add/remove = `put`/`remove`.

**Key Operations:** `floorEntry(ip)`, `ceilingEntry(ip)`, `put`, `remove`

---

### 10. Auction Bid Management

**Difficulty:** Medium | **Patterns:** Ranking, Floor/Ceiling

**Problem:** Support: place bid, retract bid, get current winning bid, get all bids above reserve price R, find closest bid to a target price T.

**Why TreeMap?** Five operations in one structure. A `PriorityQueue` can't support retract or `tailMap`. `TreeMap<Integer, List<Bidder>>` covers all five efficiently.

**Approach:** Winning = `lastEntry()`. Reserve filter = `tailMap(R, true)`. Closest to T = compare `floorKey(T)` and `ceilingKey(T)`. Retract = find entry, remove bidder from list, remove key if list is empty.

**Key Operations:** `lastEntry()`, `tailMap(R, true)`, `floorKey(T)`, `ceilingKey(T)`

---

### 11. Task Scheduler with Deadlines

**Difficulty:** Hard | **Patterns:** Scheduling, Floor/Ceiling

**Problem:** Tasks arrive with a deadline and a profit. Execute one task per time unit. Maximize profit. Also query: which tasks have a deadline within the next T units?

**Why TreeSet?** Greedy scheduling needs the latest free slot ≤ deadline (`floor`). At-risk task queries need `subMap(now, now+T)`. Both are O(log n) on a TreeSet.

**Approach:** `TreeSet<Integer>` of free time slots. Sort tasks by profit descending. For each task (deadline D): `slot = floor(D)`. If found, assign and remove slot. At-risk query: `subMap(now, now+T)`.

**Key Operations:** `floor(deadline)`, `subMap(now, now+T)`, `remove(slot)`, `add(slot)`

---

### 12. Discount Band Pricing Engine

**Difficulty:** Easy–Medium | **Patterns:** Floor/Ceiling

**Problem:** Discount bands: buy ≥100 → 5% off, ≥500 → 12% off, ≥1000 → 20% off. Bands change dynamically. Given quantity Q, return applicable discount instantly.

**Why TreeMap?** Pure `floorEntry` — find the largest threshold ≤ Q. A switch/if chain breaks when bands become dynamic; TreeMap handles it in O(log n).

**Approach:** `TreeMap<Integer, Double>` qty→discountRate. Query: `floorEntry(Q)` gives the applicable band. If null, no discount. Add/update = `put(qty, rate)`. Remove = `remove(qty)`.

**Key Operations:** `floorEntry(Q)`, `put`, `remove`

---

### 13. Concurrent Log Event Merger

**Difficulty:** Hard | **Patterns:** Streaming, Order Statistics

**Problem:** N services emit timestamped log events out of order. Merge into a globally sorted stream and output events in window `[T1, T2]`. Late arrivals must land in the correct position.

**Why TreeMap?** Single `TreeMap<Long, List<Event>>` maintains a globally sorted view across all services with O(log n) insert. Late arrivals self-sort automatically — no re-sorting needed.

**Approach:** `TreeMap<Long, List<Event>>` timestamp→events. Each arriving event inserts into its bucket. Window query: `subMap(T1, T2).values()` streams events in order. Poll with `pollFirstEntry()` for consume-in-order workflows.

**Key Operations:** `subMap(T1, T2)`, `put(ts, events)`, `firstKey()`, `pollFirstEntry()`

---

### 14. Power Grid Load Balancing

**Difficulty:** Hard | **Patterns:** Range Query, Floor/Ceiling

**Problem:** N generators each have current load and capacity. For a new power request of X, assign the generator with the smallest available headroom that can still handle X (best-fit). Generators dynamically update their loads.

**Why TreeMap?** Smallest headroom ≥ X is a `ceilingKey` problem. A heap needs full rebuild on each load update; TreeMap handles it in O(log n).

**Approach:** `TreeMap<Integer, Queue<Generator>>` headroom→generators. Request X: `ceilingKey(X)` finds the best fit. Take generator, recompute headroom, remove from old bucket, insert into new. If `ceilingKey` returns null, request cannot be served.

**Key Operations:** `ceilingKey(X)`, `put(headroom, gen)`, `remove(headroom)`

---

### 15. Time-Based Key-Value Store

**Difficulty:** Medium | **Patterns:** Versioning, Floor/Ceiling

**Problem:** Design a data store supporting multiple values per key at different timestamps. `get(key, timestamp)` should return the value from the *latest* timestamp ≤ the requested one (i.e., the most recent version).

**Why TreeMap?** `floorKey` is the exact definition of "latest timestamp ≤ requested time." No other structure provides this efficiently.

**Approach:** `HashMap<String, TreeMap<Integer, String>>` key → (timestamp → value). On `set(key, ts, val)`: get or create the inner TreeMap, `put(ts, val)`. On `get(key, ts)`: `floorEntry(ts)` on the inner map; return its value or null.

**Key Operations:** `floorEntry(ts)`, `put(ts, val)`

---

### 16. Fraud Detection in Transaction Stream

**Difficulty:** Medium | **Patterns:** Sliding Window, Proximity

**Problem:** Given a stream of transaction amounts, flag fraud if any two transactions within a time window of K have amounts differing by at most V dollars.

**Why TreeSet?** A sliding window HashSet only finds exact duplicates. `TreeSet` lets you query whether a value exists within a specific *value range* inside the window.

**Approach:** `TreeSet<Integer>` of last K amounts. For new amount X, call `ceiling(X − V)`. If the result is ≤ X + V, fraud detected. Add X to set; when window exceeds K, remove the oldest element.

**Key Operations:** `ceiling(X - V)`, `add`, `remove`

---

### 17. Exam Room Social Distancing

**Difficulty:** Medium | **Patterns:** Gap Maximization, Floor/Ceiling

**Problem:** N seats in a row. Each student maximizes distance to the nearest occupied seat. Support `seat()` and `leave(seat)`.

**Why TreeSet?** Occupied seat positions must stay sorted to efficiently find gaps between adjacent students. `lower` and `higher` locate gap boundaries in O(log n).

**Approach:** `TreeSet<Integer>` of occupied seats (seed with virtual seats at -1 and N). On `seat()`, iterate gaps between adjacent occupied seats, place student in the middle of the largest gap. On `leave(seat)`, simply `remove(seat)`.

**Key Operations:** `lower(seat)`, `higher(seat)`, `add`, `remove`

---

### 18. Order Book Matching Engine

**Difficulty:** Hard | **Patterns:** Two-Sided Market, Min/Max

**Problem:** Users submit Buy (bid) and Sell (ask) orders. A trade executes when the highest bid ≥ lowest ask. Orders are partially filled and removed when exhausted.

**Why TreeMap?** You need the global max of bids and global min of asks at all times, with O(log n) insert/delete as orders are fulfilled. Two TreeMaps with opposite sort orders expose each side's best price via `firstKey()`.

**Approach:** `TreeMap<Integer, Integer>` askPrice→quantity (natural order). `TreeMap<Integer, Integer>` bidPrice→quantity (reverse order via `Collections.reverseOrder()`). To match: compare `bids.firstKey()` and `asks.firstKey()`. If bid ≥ ask, execute trade and update quantities.

**Key Operations:** `firstKey()` on both maps, `put`, `remove`

---

### 19. Memory Allocator (Best-Fit Malloc)

**Difficulty:** Hard | **Patterns:** Best-Fit Allocation, Floor/Ceiling

**Problem:** Simulate a memory manager with blocks. Requests to allocate size S must be fulfilled by the *smallest free block* that fits (best fit). Freed blocks by block ID must be returned to the pool.

**Why TreeMap?** `ceilingKey(S)` finds the smallest valid free block in O(log n). A heap requires O(n) rebuild when freed blocks of varying sizes are returned.

**Approach:** `TreeMap<Integer, TreeSet<Integer>>` size→set of start indices. Allocate S: `ceilingKey(S)` finds the block. Remove it from the map, give `[start, start+S)` to caller, re-insert leftover `[start+S, end)` under its new size. Free: look up block by ID, re-insert into the map.

**Key Operations:** `ceilingKey(S)`, `firstKey()`, `put`, `remove`

---

### 20. Network Bandwidth Slot Allocator

**Difficulty:** Medium | **Patterns:** Best-Fit Allocation

**Problem:** A network switch has a pool of available bandwidth slots (e.g., 10 Mbps, 50 Mbps, 100 Mbps). Requests require a minimum bandwidth. Assign the smallest slot that satisfies the request, freeing it afterward.

**Why TreeMap?** `ceilingKey(request)` gives the smallest sufficient slot instantly. A sorted array requires binary search + O(n) deletion; TreeMap does both in O(log n).

**Approach:** `TreeMap<Integer, Integer>` bandwidth→count. For request R: `ceilingKey(R)` finds the slot. Decrement count; remove key if count reaches zero. On release, `put(bandwidth, count+1)`.

**Key Operations:** `ceilingKey(R)`, `put`, `remove`

---

## Pattern Summary

| # | Problem | Core Operation | Complexity |
|---|---|---|---|
| 01 | Player matchmaking | `floorKey` + `ceilingKey` | O(log n) per match |
| 02 | Ride dispatch | `floor` + `ceiling` on TreeSet | O(log n) per request |
| 03 | Meeting room checker | `floorKey` + `ceilingKey` | O(log n) per booking |
| 04 | Sliding window median | `firstKey` + `lastKey` on two maps | O(log n) per element |
| 05 | Stock price alerts | `headMap` + `tailMap` | O(log n + k) per tick |
| 06 | LFU cache | `firstEntry` for min-freq | O(log n) per operation |
| 07 | Live leaderboard | `tailMap` for rank count | O(log n) per update |
| 08 | Anomaly detection | `floor` + `ceiling` on sliding window | O(log n) per reading |
| 09 | IP range router | `floorEntry` | O(log n) per packet |
| 10 | Auction bids | `lastEntry` + `tailMap` | O(log n) per bid |
| 11 | Deadline scheduler | `floor` on free slots | O(n log n) total |
| 12 | Discount bands | `floorEntry` | O(log n) per query |
| 13 | Log event merger | `subMap` | O(log n + k) per window |
| 14 | Power grid dispatch | `ceilingKey` | O(log n) per request |
| 15 | Time-based KV store | `floorEntry` on timestamps | O(log n) per get |
| 16 | Fraud detection | `ceiling(x - V)` in sliding window | O(log n) per transaction |
| 17 | Exam room seating | `lower` + `higher` for gap finding | O(n log n) total |
| 18 | Order book engine | `firstKey` on two reversed maps | O(log n) per order |
| 19 | Memory allocator | `ceilingKey` + re-insert leftover | O(log n) per alloc |
| 20 | Bandwidth allocator | `ceilingKey` | O(log n) per request |

---

## The Five Core Patterns (Cheat Sheet)

**1. Nearest Element** — `floor(x)` + `ceiling(x)`, compare both candidates
> Matchmaking (#1), Ride Dispatch (#2), Fraud Detection (#16)

**2. Interval Neighbor Check** — `floorEntry(start)` + `ceilingEntry(start)`
> Meeting Room (#3), Anomaly Detection (#8), Time-Based KV Store (#15)

**3. Range Extraction** — `subMap(a, b)` / `headMap` / `tailMap`
> Stock Alerts (#5), Leaderboard (#7), Log Merger (#13)

**4. Best-Fit Allocation** — `ceilingKey(required)`
> Power Grid (#14), Memory Allocator (#19), Bandwidth (#20)

**5. Dynamic Min/Max** — `firstKey()` / `lastKey()` / `firstEntry()`
> LFU Cache (#6), Order Book (#18), Sliding Median (#4)
>
