Perfect — let’s make this concrete.
I’ll give you a **Redis command glossary** for the ride-sharing app we’ve been designing (fare → ride creation → driver matching → offer/accept → location tracking). You can paste these into `redis-cli` and play with them.

---

# 🔹 1. Driver Location & Availability

### Add/update driver location

```bash
# store driver coordinates in GEOSET
GEOADD driver:geo:HYD 78.3772 17.4435 driver:123
```

### Query nearby drivers

```bash
# find 10 drivers within 3km of pickup
GEOSEARCH driver:geo:HYD FROMLONLAT 78.38 17.44 BYRADIUS 3 KM COUNT 10 ASC
```

### Driver state (hash per driver)

```bash
# when driver goes online
HSET driver:state:123 session_status online_idle last_active 1693832334 vehicle_type GO
EXPIRE driver:state:123 180   # auto-expire if no heartbeat

# when driver goes offline
DEL driver:state:123
```

### Update last heartbeat

```bash
HSET driver:state:123 last_active 1693832455
EXPIRE driver:state:123 180
```

---

# 🔹 2. Ride Lifecycle

### Candidates list (drivers considered for a ride)

```bash
# add driver to candidate set for rideId=987
SADD ride:candidates:987 driver:123
EXPIRE ride:candidates:987 900   # keep 15min
```

### Assign driver soft-lock (offer)

```bash
# create an offer from ride 987 to driver 123
SETNX offer:ride:987:driver:123 offer-uuid-1
EXPIRE offer:ride:987:driver:123 15   # auto-expire if no response
```

---

# 🔹 3. Offer / Accept / Decline

### Mark driver in offer\_pending

```bash
HSET driver:state:123 session_status offer_pending current_ride_id 987
```

### Driver accepts (validated on server)

```bash
# check if offer exists
GET offer:ride:987:driver:123
# → should return offer-uuid-1

# if valid, mark driver assigned
HSET driver:state:123 session_status assigned current_ride_id 987
DEL offer:ride:987:driver:123
```

### Cancel offers for other drivers (once one accepts)

```bash
# get all drivers in ride’s candidate set
SMEMBERS ride:candidates:987
# delete their offers & reset their state
DEL offer:ride:987:driver:124
HSET driver:state:124 session_status online_idle current_ride_id ""
```

### Driver declines or times out

```bash
DEL offer:ride:987:driver:123
HSET driver:state:123 session_status online_idle current_ride_id ""
```

---

# 🔹 4. Rider → Driver Binding

You usually keep a direct mapping while ride is active:

```bash
SET ride:driver:987 123 EX 3600    # ride 987 assigned to driver 123
SET driver:ride:123 987 EX 3600    # inverse mapping
```

---

# 🔹 5. Fare Quotes (ephemeral)

If you want to store short-lived fare estimates in cache:

```bash
SETEX quote:uuid-1 120 '{"rideId":"987","price":228,"currency":"INR"}'
```

---

# 🔹 6. Example Flow

1. **Driver heartbeat**

```bash
GEOADD driver:geo:HYD 78.3772 17.4435 driver:123
HSET driver:state:123 session_status online_idle last_active 1693832334
EXPIRE driver:state:123 180
```

2. **Rider requests ride** → match worker runs:

```bash
GEOSEARCH driver:geo:HYD FROMLONLAT 78.38 17.44 BYRADIUS 3 KM COUNT 5 ASC
```

3. **Offer to driver:123**

```bash
SETNX offer:ride:987:driver:123 offer-uuid-1
EXPIRE offer:ride:987:driver:123 15
HSET driver:state:123 session_status offer_pending current_ride_id 987
SADD ride:candidates:987 driver:123
```

4. **Driver accepts** (within 15s)

```bash
GET offer:ride:987:driver:123   # validate
HSET driver:state:123 session_status assigned current_ride_id 987
DEL offer:ride:987:driver:123
SET ride:driver:987 123 EX 3600
SET driver:ride:123 987 EX 3600
```

5. **Cancel others**

```bash
SMEMBERS ride:candidates:987  # suppose {123,124,125}
DEL offer:ride:987:driver:124 offer:ride:987:driver:125
HSET driver:state:124 session_status online_idle current_ride_id ""
HSET driver:state:125 session_status online_idle current_ride_id ""
```

---

# 🔹 7. Optional Helpers

* **Expire inactive drivers automatically:**

```bash
# run in background every minute
SCAN 0 MATCH driver:state:* COUNT 100
# check last_active, if >90s → set offline
```

* **Surge multipliers in Redis**

```bash
SET surge:cell:hexid 1.3 EX 60
GET surge:cell:hexid
```

---

👉 This covers **all Redis operations** you’d use: `GEOADD`, `GEOSEARCH`, `HSET`, `SETNX`, `SETEX`, `EXPIRE`, `SADD`, `SMEMBERS`, `DEL`, `GET`.
You can now simulate: driver online → ride request → match worker offer → driver accept → cancel others.

---

Do you want me to also prepare a **sample Lua script** that atomically (1) checks driver idle, (2) sets them `offer_pending`, and (3) creates the `offer:*` key in one shot? That way you avoid race conditions when testing this yourself.
