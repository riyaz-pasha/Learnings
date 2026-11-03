# **Redis HyperLogLog – Complete Guide**

---

## **1. What is HyperLogLog?**

HyperLogLog (HLL) is a **probabilistic data structure** used to **estimate the cardinality** of a dataset (i.e., count of unique items) with **very low memory usage**.

* **Memory cost:** \~12 KB **fixed**, no matter how many elements you add (millions or billions).
* **Accuracy:** Error margin \~0.81%.
* **Tradeoff:** You don’t get the actual items, just the **approximate number of distinct elements**.

---

## **2. Why use HyperLogLog?**

### Traditional approach:

* Storing all unique items (e.g., in a `SET`) → Memory usage grows with dataset size.

### HyperLogLog approach:

* Doesn’t store actual elements — just a statistical fingerprint.
* Memory stays constant.

---

## **3. Real-World Applications**

| Use Case                                                  | Why HyperLogLog?                                               |
| --------------------------------------------------------- | -------------------------------------------------------------- |
| **Unique website visitors**                               | Track unique IPs or user IDs per day without storing each one. |
| **Daily active users (DAU) / Monthly active users (MAU)** | Count unique users interacting with your app.                  |
| **Unique search queries**                                 | Count unique searches without storing every term.              |
| **API unique request tracking**                           | Estimate unique client IDs hitting an API endpoint.            |
| **Event analytics**                                       | Track unique participants in events/contests.                  |
| **Ad impressions**                                        | Count unique viewers for an ad.                                |

---

## **4. Core Commands**

### **4.1 PFADD** – Add elements

```redis
PFADD visitors "user1" "user2" "user3"
```

* Creates a HyperLogLog at key `visitors` if it doesn’t exist.
* Adding duplicates has no effect.

---

### **4.2 PFCOUNT** – Count unique elements

```redis
PFCOUNT visitors
```

* Returns **approximate** count of unique elements.

---

### **4.3 PFMERGE** – Merge multiple HyperLogLogs

```redis
PFMERGE total_visitors visitors:day1 visitors:day2 visitors:day3
```

* Combines multiple HLLs into one.

---

## **5. Example Workflow – Unique Visitors per Day**

### Add daily visitors:

```redis
# Day 1
PFADD visitors:2025-08-15 "ip1" "ip2" "ip3"

# Day 2
PFADD visitors:2025-08-16 "ip2" "ip3" "ip4"
```

### Count:

```redis
PFCOUNT visitors:2025-08-15     # ≈ 3
PFCOUNT visitors:2025-08-16     # ≈ 3
```

### Monthly unique visitors:

```redis
PFMERGE visitors:month visitors:2025-08-15 visitors:2025-08-16
PFCOUNT visitors:month          # ≈ 4
```

---

## **6. Memory Advantage**

| Unique Elements | Using SET | Using HyperLogLog |
| --------------- | --------- | ----------------- |
| 10,000          | \~1.2 MB  | \~12 KB           |
| 1,000,000       | \~120 MB  | \~12 KB           |
| 100,000,000     | \~12 GB   | \~12 KB           |

---

## **7. Limitations**

1. **Approximation** – ±0.81% error margin.
2. **No element retrieval** – Can’t get actual values, only counts.
3. **Not for small datasets** – For small sets (<10k), memory saving is negligible compared to a `SET`.

---

## **8. Real-World Design Example**

### Scenario: Track Daily Active Users in a SaaS App

We want:

* Unique users per day
* Unique users per month
* Low memory usage

#### Data Flow:

```redis
# On user login event
PFADD dau:2025-08-15 user123
```

#### Reports:

```redis
# Daily active users
PFCOUNT dau:2025-08-15

# Monthly active users
PFMERGE mau:2025-08 dau:2025-08-*   # merge all daily logs
PFCOUNT mau:2025-08
```

**Benefits**:

* Each daily HLL uses only \~12 KB.
* Monthly aggregation also stays at \~12 KB.

---

## **9. Performance Tips**

* Use **time-based keys** (`visitors:YYYY-MM-DD`) for easy aggregation.
* Use `PFMERGE` sparingly — merging is cheap but avoid merging thousands of keys unnecessarily.
* If you need **exact counts**, use `SET` or `BITMAPS` instead.

---

## **10. When NOT to use HyperLogLog**

* When you **need exact counts** (e.g., legal/financial reporting).
* When you **need to retrieve items** (e.g., get the list of visitors).
* For very small datasets — a regular `SET` might be simpler.

---

## **11. Full Example: Web Analytics Tracker**

```redis
# Add visitors for each page view event
PFADD pageviews:2025-08-15 "user:1001"
PFADD pageviews:2025-08-15 "user:1002"
PFADD pageviews:2025-08-15 "user:1001"  # duplicate ignored

# Get daily unique visitors
PFCOUNT pageviews:2025-08-15  # ≈ 2

# Merge for weekly count
PFMERGE pageviews:week33 pageviews:2025-08-15 pageviews:2025-08-16 ...
PFCOUNT pageviews:week33
```

---

✅ **Summary:**
HyperLogLog in Redis is perfect when:

* You need **unique counts**
* You can tolerate a **small error margin**
* You care about **memory efficiency** over exact storage of elements
