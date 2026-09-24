# **Redis Geospatial – Complete Guide**

Redis supports **geospatial indexes** using sorted sets under the hood, with special commands to store, query, and calculate distances between geographic points (latitude/longitude).

---

## **1. Why Geospatial in Redis?**

You can store locations and:

* Query all places **within a given radius**.
* Find **distance between two points**.
* Get **sorted results by proximity**.
* Do this **in-memory and fast** — without setting up a separate GIS database.

---

## **2. Real-World Applications**

| Use Case              | How Redis Geospatial Helps                             |
| --------------------- | ------------------------------------------------------ |
| **Ride-hailing apps** | Find nearest drivers to a rider’s location.            |
| **Food delivery**     | Find restaurants within delivery radius.               |
| **Store locator**     | Find the closest store to a user’s location.           |
| **Logistics**         | Match packages to nearest warehouses.                  |
| **Geofencing**        | Trigger alerts when moving objects enter/exit an area. |

---

## **3. Core Commands**

### **3.1 GEOADD** – Add locations

```redis
GEOADD key longitude latitude member
```

Example:

```redis
GEOADD cities 77.5946 12.9716 "Bangalore"
GEOADD cities 72.8777 19.0760 "Mumbai"
GEOADD cities 88.3639 22.5726 "Kolkata"
```

---

### **3.2 GEODIST** – Distance between two locations

```redis
GEODIST key member1 member2 [unit]
```

Units: `m` (meters), `km`, `mi` (miles), `ft`.

Example:

```redis
GEODIST cities Bangalore Mumbai km
# → 845.1123 (approx)
```

---

### **3.3 GEORADIUS** (Legacy) / **GEOSEARCH** (New) – Find locations within radius

#### Old way:

```redis
GEORADIUS cities 77.5946 12.9716 500 km
```

#### New way (Redis 6.2+):

```redis
GEOSEARCH cities FROMLONLAT 77.5946 12.9716 BYRADIUS 500 km
```

---

### **3.4 GEOSEARCHSTORE** – Store search results in a new key

```redis
GEOSEARCHSTORE nearby_cities cities FROMLONLAT 77.5946 12.9716 BYRADIUS 500 km
```

---

### **3.5 GEOPOS** – Get stored coordinates

```redis
GEOPOS cities Bangalore
# → [77.5946, 12.9716]
```

---

### **3.6 GEOHASH** – Get GeoHash strings

```redis
GEOHASH cities Bangalore Mumbai
# → ["tdr1h8h7h3", "t2f8r7j1db"]
```

---

## **4. Example – Find Nearby Restaurants**

### Add data:

```redis
GEOADD restaurants 77.5946 12.9716 "PizzaHut"
GEOADD restaurants 77.5800 12.9720 "Dominos"
GEOADD restaurants 77.6100 12.9700 "KFC"
```

### Search within 2 km:

```redis
GEOSEARCH restaurants FROMLONLAT 77.5946 12.9716 BYRADIUS 2 km WITHDIST WITHCOORD ASC
```

* `WITHDIST` → include distance
* `WITHCOORD` → include coordinates
* `ASC` → sort nearest first

---

## **5. How It Works Under the Hood**

* Redis stores geospatial data in a **Sorted Set (ZSET)**.
* The **score** is a GeoHash (52-bit integer encoding lat/lon).
* GEO commands are optimized lookups on that ZSET.

---

## **6. Limitations**

1. No polygon queries (only radius/box searches).
2. Earth curvature handled for short distances — not suitable for exact GIS needs.
3. No automatic updates for moving objects — you must re-add them.

---

## **7. Best Practices**

* Use `GEOSEARCH` instead of deprecated `GEORADIUS`/`GEORADIUSBYMEMBER`.
* For moving objects (e.g., delivery drivers), update their location periodically with `GEOADD`.
* Combine with **Pub/Sub** or **Streams** for real-time tracking.
* Store **extra metadata** separately in a HASH keyed by member name.

Example:

```redis
HSET restaurant:PizzaHut name "Pizza Hut" rating 4.2
```

---

## **8. Real-World Design Example – Ride Matching**

1. Rider requests ride → backend calls:

   ```redis
   GEOSEARCH drivers FROMLONLAT rider_lon rider_lat BYRADIUS 5 km ASC
   ```
2. Get nearest driver ID.
3. Fetch driver details from HASH.
4. Assign and notify driver.

---

✅ **Summary:**
Redis Geospatial is perfect when you need **fast, radius-based lookups** for location-based applications and can store location as a single lat/lon per member.
