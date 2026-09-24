# **Redis Bloom Filter – Complete Guide**

---

## **1. What is a Bloom Filter?**

A **Bloom Filter** is a **probabilistic data structure** used to test **whether an element is *definitely not* in a set** or **possibly in the set**.

* **"Definitely not"** → 100% correct
* **"Possibly in set"** → Could be false positive
* **No false negatives**, only **false positives** possible.
* Extremely **memory efficient** — especially for very large sets.

---

## **2. Why use Bloom Filters?**

Traditional `SET` in Redis:

* Stores every item → memory grows with dataset size.

Bloom Filter:

* Doesn’t store actual elements, just **bit patterns**.
* Memory stays **small & predictable**.
* Great for **fast membership checks**.

---

## **3. How Bloom Filters Work (Concept)**

1. **Multiple hash functions** map each element to multiple bit positions in a bit array.
2. On **insert**, all those bits are set to 1.
3. On **check**, if any of those bits are 0 → item is **definitely not present**.
4. If all are 1 → item is **possibly present** (could be false positive).

---

## **4. RedisBloom Module**

Bloom filters in Redis come from the **RedisBloom module**, which is included in **Redis Stack**.

```bash
docker run -p 6379:6379 redis/redis-stack:latest
```

---

## **5. Core Commands**

| Command                              | Description             |
| ------------------------------------ | ----------------------- |
| `BF.RESERVE key error_rate capacity` | Create a Bloom filter   |
| `BF.ADD key item`                    | Add an item             |
| `BF.MADD key item [item ...]`        | Add multiple items      |
| `BF.EXISTS key item`                 | Check if an item exists |
| `BF.MEXISTS key item [item ...]`     | Check multiple items    |

---

## **6. Creating a Bloom Filter**

```redis
BF.RESERVE bf:users 0.01 100000
```

* `0.01` → error rate = 1%
* `100000` → expected number of items
* Memory is pre-allocated to fit these constraints.

---

## **7. Adding & Checking Items**

```redis
# Add items
BF.ADD bf:users "user:1001"   # returns 1 (new)
BF.ADD bf:users "user:1001"   # returns 0 (probably already there)

# Check existence
BF.EXISTS bf:users "user:1001"  # returns 1 (possibly there)
BF.EXISTS bf:users "user:9999"  # returns 0 (definitely not there)
```

---

## **8. Real-World Applications**

| Use Case                        | Why Bloom Filter?                                          |
| ------------------------------- | ---------------------------------------------------------- |
| **Duplicate request detection** | Quickly check if an API request ID was already processed.  |
| **Spam email filtering**        | Maintain a large set of known spam sender addresses.       |
| **Malware URL blacklist**       | Check if a URL is blacklisted without storing all URLs.    |
| **Web crawler visited URLs**    | Avoid re-visiting the same URLs.                           |
| **Fraud detection**             | Track already-used credit card numbers or transaction IDs. |
| **E-commerce inventory check**  | Quickly check if a product ID exists before querying DB.   |
| **Recommendation system**       | Avoid recommending already seen content.                   |

---

## **9. Example: Prevent Duplicate Signups**

```redis
BF.RESERVE bf:emails 0.001 1000000

# On signup
BF.ADD bf:emails "test@example.com"   # if returns 0 → already exists
```

* If result is `0` → probably already seen (block signup).
* If `1` → definitely new.

---

## **10. Example: Crawler Unique URL Tracker**

```redis
BF.RESERVE bf:urls 0.0001 50000000

# Before visiting a URL:
BF.EXISTS bf:urls "https://example.com"
# If 0 → definitely not visited → visit & add
BF.ADD bf:urls "https://example.com"
```

---

## **11. Error Rate & Capacity**

* **Error rate** is the probability of false positives.
* Lower error rate → higher memory use.
* **Capacity** should match your expected unique items.
* If capacity is exceeded → error rate increases.

---

## **12. Memory Efficiency Example**

| Items | SET Memory | Bloom Filter Memory (1% error) |
| ----- | ---------- | ------------------------------ |
| 1M    | \~96 MB    | \~1.2 MB                       |
| 10M   | \~960 MB   | \~12 MB                        |
| 100M  | \~9.6 GB   | \~120 MB                       |

---

## **13. Limitations**

1. **False positives possible** (tunable via error rate).
2. **No delete** (standard Bloom) — removing items breaks accuracy.

   * If you need delete, use **Cuckoo Filter** instead (also in RedisBloom).
3. Can’t retrieve stored items — only membership test.

---

## **14. Best Practices**

* Pick **error\_rate** and **capacity** carefully at creation.
* Separate Bloom filters for different datasets.
* Use `BF.MADD` and `BF.MEXISTS` for batch operations to reduce round-trips.
* For deleting, consider `CF.*` commands (Cuckoo Filter).

---

## **15. Full Workflow Example: API Duplicate Detection**

```redis
# Create filter for 10M requests with 0.01% false positive rate
BF.RESERVE bf:requests 0.0001 10000000

# On API request
# 1. Check
BF.EXISTS bf:requests "req-12345"  # 0 → process, 1 → reject

# 2. Add
BF.ADD bf:requests "req-12345"
```

---

✅ **Summary:**
Bloom Filters in Redis are ideal for **large-scale membership checks** where:

* You **can tolerate small false positives**
* You need **low memory footprint**
* You don’t need to store actual elements
