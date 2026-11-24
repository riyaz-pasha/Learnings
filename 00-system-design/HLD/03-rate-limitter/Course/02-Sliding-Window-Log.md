# **Chapter 2 — Sliding Window Log**

In the previous chapter, we discovered a fundamental problem with Fixed Window.

Let's start with that exact problem rather than introducing a new algorithm out of nowhere.

## **1. Why do we need Sliding Window Log?**

Imagine our requirement is:

```
A user can make at most 5 requests in any 10-second period.
```

Notice the wording:

**Any 10-second period.**

This is different from saying:

```
A user can make at most 5 requests in each fixed 10-second interval.
```

With Fixed Window, we divided time into predefined intervals:

```
Window 1                 Window 2
|------------------------|------------------------|
0                       10                       20
```

The problem was that a user could make 5 requests at 9.8 seconds and another 5 requests at 10.1 seconds.

We allowed 10 requests within 300 milliseconds.

Why?

Because the second window started at 10 seconds and its counter was reset.

What if we didn't reset the counter at predefined boundaries?

What if, instead, we remembered *when each request happened*?

That gives us a much more powerful approach.

## **2. The fundamental idea**

Let's imagine a user makes these requests:

```
Time (seconds)

1      Request 1
2      Request 2
3      Request 3
5      Request 4
8      Request 5
```

Instead of storing just a counter like:

```
counter = 5
```

We store the actual timestamps:

```
[
    1,
    2,
    3,
    5,
    8
]
```

Now, suppose another request arrives at time `9`.

Our policy is:

```
Maximum requests = 5
Window duration = 10 seconds
```

We ask:

> How many requests has this user made in the last 10 seconds?

The current time is 9.

The relevant time interval is:

```
-1 ------------------------------ 9
              10 seconds
```

All five previous timestamps fall within this interval.

Therefore:

```
Existing requests = 5
Limit             = 5

New request       = REJECT
```

Now imagine another request arrives at time 12.

The relevant interval becomes:

```
2 ------------------------------- 12
              10 seconds
```

Which previous requests fall within this interval?

```
Timestamp 1 -> Outside the window
Timestamp 2 -> Inside the window
Timestamp 3 -> Inside the window
Timestamp 5 -> Inside the window
Timestamp 8 -> Inside the window
```

We discard timestamp 1 because it is too old.

The remaining request history is:

```
[
    2,
    3,
    5,
    8
]
```

We have four requests within the last 10 seconds.

Therefore:

```
4 < 5

New request = ALLOW
```

We add its timestamp:

```
[
    2,
    3,
    5,
    8,
    12
]
```

That's the fundamental idea behind **Sliding Window Log**.

Instead of counting requests within predefined intervals, we maintain a log of recent request timestamps.

Whenever a new request arrives:

1. Remove timestamps that are outside the current rolling window.
2. Count the remaining timestamps.
3. If the count is below the limit, accept the request and record its timestamp.
4. Otherwise, reject it.

Let's understand every part of this properly.

# **3. How exactly does the window slide?**

This is probably the most important concept to understand.

Suppose:

```
Limit = 5 requests
Window = 10 seconds
```

A request arrives at time `T`.

Its sliding window is:

\\[ (T-10,\ T] \\]

This means:

- The window starts 10 seconds before the current request.
- It ends at the current time.
- Requests older than or exactly 10 seconds old are excluded.

For example:

```
Current time = 20 seconds

Sliding window:

10 ------------------------------- 20
                 10 seconds
```

Now, when a new request arrives at 21 seconds:

```
Old window:

10 -------------------------------- 20

New window:

11 -------------------------------- 21
```

When another request arrives at 25 seconds:

```
Old window:

11 -------------------------------- 21

New window:

15 -------------------------------- 25
```

Notice that there are no predefined boundaries.

**The window is recalculated relative to the arrival time of every new request.**

This is why we call it a Sliding Window.

The window moves forward with time.

It doesn't matter whether the request arrives at:

- 9.8 seconds.
- 10.1 seconds.
- 10.5 seconds.
- 15.7 seconds.

Every request gets its own 10-second observation interval.

This eliminates the Fixed Window boundary problem.

# **4. Let's revisit the boundary burst problem**

We need to make sure we understand exactly how Sliding Window Log solves it.

Our policy:

```
5 requests per 10 seconds
```

Consider this traffic:

```
Time 9.8 seconds:

Request 1 -> ALLOW
Request 2 -> ALLOW
Request 3 -> ALLOW
Request 4 -> ALLOW
Request 5 -> ALLOW
```

Now, at 10.1 seconds, another request arrives.

Its sliding window is:

```
0.1 -------------------------------- 10.1
```

All five previous requests at 9.8 are inside this interval.

Therefore:

```
Existing requests = 5
Limit = 5

New request = REJECT
```

This is exactly what we wanted!

We can visualize the difference:

**Fixed Window**

```
Window 1                     Window 2
|----------------------------|---------------------|
0                           10                    20

                       9.8     10.1
                        XXXXX   XXXXX
                         5       5

                    10 requests in 300ms!
```

**Sliding Window Log**

```
At 10.1 seconds:

0.1 --------------------------------------- 10.1
                         XXXXX
                    5 existing requests

                    6th request = REJECT
```

The second algorithm enforces the limit over every rolling 10-second period.

That's the reason we introduced it.

# **5. Now let's design the actual algorithm**

We understand the concept. Let's translate it into an implementation.

First, identify what information we need to maintain.

For each identity, we need:

1. The timestamps of its recent requests.
2. The maximum allowed requests.
3. The window duration.

For example:

```
Policy:
    ID = products-api
    Limit = 5
    Window = 10 seconds

Identity:
    user_123

Request history:
    [2, 3, 5, 8, 12]
```

We don't need to store the complete request payload, URL, response, headers, or other application information.

We only need timestamps.

## **5.1 Algorithm step by step**

Let's assume a request arrives at time 20.

Our existing request history is:

```
[
    2,
    5,
    8,
    12,
    15,
    18
]
```

Our limit is 5 requests per 10 seconds.

**Step 1: Get the current time.**

```
now = 20
```

**Step 2: Calculate the window start.**

\\[ windowStart = now - windowDuration \\]

```
windowStart = 20 - 10
            = 10
```

**Step 3: Remove old timestamps.**

We only care about timestamps greater than 10.

```
Before cleanup:

[2, 5, 8, 12, 15, 18]

After cleanup:

[12, 15, 18]
```

The timestamps 2, 5, and 8 are too old.

**Step 4: Count the remaining requests.**

```
count = 3
limit = 5
```

**Step 5: Make the decision.**

```
if count < limit:
    ALLOW
else:
    REJECT
```

Since 3 is less than 5, we allow it.

**Step 6: Record the new request timestamp.**

```
Before:

[12, 15, 18]

After:

[12, 15, 18, 20]
```

That's it.

Our implementation is:

```
function allowRequest(identity, policy):

    now = getCurrentTime()

    windowStart = now - policy.windowDuration

    key = buildKey(policy.id, identity)

    removeTimestampsAtOrBefore(
        key,
        windowStart
    )

    count = getTimestampCount(key)

    if count >= policy.limit:
        return REJECT

    addTimestamp(key, now)

    return ALLOW
```

This is the conceptual algorithm.

We'll discuss how to implement each storage operation efficiently.

# **6. What data structure should we use?**

This is where the algorithm gets interesting from an implementation perspective.

Our requirements are:

- Store timestamps.
- Add new timestamps.
- Remove timestamps older than a particular time.
- Count how many timestamps remain.
- Perform these operations efficiently.

There are several possible data structures.

### **Option A: Queue / Deque**

Imagine storing timestamps in chronological order:

```
Front                         Rear

[2, 5, 8, 12, 15, 18]
```

New timestamps are appended to the rear.

Old timestamps are removed from the front.

For example, if the window starts at 10:

```
Remove 2
Remove 5
Remove 8

Remaining:

[12, 15, 18]
```

A queue or deque works naturally because requests are normally processed in chronological order.

Operations:

| Operation               | Complexity                  |
| ----------------------- | --------------------------- |
| Append timestamp        | O(1)                        |
| Remove oldest timestamp | O(1)                        |
| Count elements          | O(1), if size is maintained |
| Find oldest timestamp   | O(1)                        |

This is excellent for a single-process implementation.

However, there are problems when we distribute the rate limiter across multiple servers.

For example:

```
                  Load Balancer
                  /           \
                 /             \
          Server A            Server B
             |                    |
          Queue A              Queue B
```

If user 123 sends requests to both servers, each queue maintains a different history.

Server A might believe:

```
user_123 = 3 requests
```

Server B might also believe:

```
user_123 = 3 requests
```

But the actual total is 6.

We can no longer enforce a global limit of 5.

We need shared state.

### **Option B: Redis Sorted Set**

Redis provides a useful data structure called a **Sorted Set (ZSET)**.

A Sorted Set stores members along with numerical scores and automatically maintains their ordering by score.

For Sliding Window Log, we can use:

```
Score  = Request timestamp
Member = Unique request ID
```

For example:

```
Redis Key:

rate_limit:products:user_123

Sorted Set:

Member       Score
--------------------
request_001    2
request_002    5
request_003    8
request_004   12
request_005   15
request_006   18
```

Because the score represents a timestamp, Redis maintains the requests in timestamp order.

Now we can efficiently:

- Add a request timestamp.
- Remove all timestamps older than the sliding window.
- Count the remaining requests.
- Find the oldest request.

This is a natural fit for Sliding Window Log.

One important detail: Redis Sorted Set members must be unique.

We therefore shouldn't use the timestamp itself as the member. Multiple requests can arrive during the same millisecond.

Instead, use a unique request ID as the member and the timestamp as the score.

For example:

```
ZADD rate_limit:products:user_123 1700000000000 request_001
ZADD rate_limit:products:user_123 1700000000000 request_002
```

Both requests have the same timestamp, but both are stored because their members are different.

# **7. How to implement Sliding Window Log using Redis?**

Let's now connect the algorithm to actual Redis operations.

Assume:

```
Limit = 5
Window = 10 seconds
Identity = user_123
```

Our key:

```
rate_limit:products:user_123
```

We will use milliseconds for timestamp precision.

## **7.1 Remove old requests**

Redis provides:

```
ZREMRANGEBYSCORE key min max
```

This removes all members whose scores fall within the given range.

Suppose:

```
Current time = 20,000 ms
Window = 10,000 ms

Window start = 10,000 ms
```

We need to remove requests with timestamps less than or equal to 10,000.

Conceptually:

```
ZREMRANGEBYSCORE rate_limit:products:user_123 -inf 10000
```

Our Sorted Set now contains only recent requests.

## **7.2 Count existing requests**

Use:

```
ZCARD rate_limit:products:user_123
```

This gives us the number of members in the Sorted Set.

For example:

```
count = 3
```

## **7.3 Check the limit**

```
if count < 5:
    allow
else:
    reject
```

## **7.4 Insert the new request**

For an allowed request:

```
ZADD rate_limit:products:user_123 20000 request_007
```

Now the request history includes the newly admitted request.

### **The complete flow**

\#chatgpt-mermaid-\_r_uh\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_uh\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_uh\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_uh\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_uh\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_uh\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_uh\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_uh\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_uh\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_uh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uh\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uh\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_uh\_ p{margin:0;}#chatgpt-mermaid-\_r_uh\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_uh\_ .label text,#chatgpt-mermaid-\_r_uh\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .node rect,#chatgpt-mermaid-\_r_uh\_ .node circle,#chatgpt-mermaid-\_r_uh\_ .node ellipse,#chatgpt-mermaid-\_r_uh\_ .node polygon,#chatgpt-mermaid-\_r_uh\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .rough-node .label text,#chatgpt-mermaid-\_r_uh\_ .node .label text,#chatgpt-mermaid-\_r_uh\_ .image-shape .label,#chatgpt-mermaid-\_r_uh\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_uh\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .rough-node .label,#chatgpt-mermaid-\_r_uh\_ .node .label,#chatgpt-mermaid-\_r_uh\_ .image-shape .label,#chatgpt-mermaid-\_r_uh\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_uh\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_uh\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uh\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uh\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_uh\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_uh\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uh\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uh\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_uh\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_uh\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_uh\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_uh\_ .icon-shape,#chatgpt-mermaid-\_r_uh\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_uh\_ .icon-shape p,#chatgpt-mermaid-\_r_uh\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_uh\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_uh\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_uh\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_uh\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_uh\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_uh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_uh\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_uh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_uh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uh\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_uh\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_uh\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_uh\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_uh\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_uh\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_uh\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .node rect,#chatgpt-mermaid-\_r_uh\_ .node circle,#chatgpt-mermaid-\_r_uh\_ .node ellipse,#chatgpt-mermaid-\_r_uh\_ .node polygon,#chatgpt-mermaid-\_r_uh\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_uh\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_uh\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_uh\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_uh\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_uh\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}New RequestIdentify UserGet Current TimeCalculate Window StartRemove Older TimestampsCount RemainingTimestampsCount < Limit?Insert New TimestampALLOWREJECTYesNo

This is our first practical shared-storage implementation.

But there is one very important problem we still need to solve.

# **8. The concurrency problem — Atomicity again!**

You might notice something familiar from Fixed Window.

We have multiple operations:

```
1. Remove old timestamps.
2. Count remaining timestamps.
3. Check the limit.
4. Insert the new timestamp.
```

What happens when multiple requests arrive simultaneously?

Consider:

```
Limit = 5
Existing request count = 4
```

Two requests arrive at approximately the same time.

Let's see what can happen.

```
Request A:
    Remove old timestamps

Request B:
    Remove old timestamps

Request A:
    Count = 4

Request B:
    Count = 4

Request A:
    4 < 5
    ALLOW

Request B:
    4 < 5
    ALLOW

Request A:
    Insert timestamp

Request B:
    Insert timestamp
```

We now have six requests in the window.

We violated the policy.

The algorithm is correct, but our implementation is not concurrency-safe.

### **Solution: Execute the entire validation atomically**

We need these operations to behave as one indivisible operation:

```
ATOMIC OPERATION:

    now = getCurrentTime()

    windowStart = now - windowDuration

    removeOldTimestamps(key, windowStart)

    count = getCount(key)

    if count >= limit:
        return REJECT

    insertNewTimestamp(key, now)

    return ALLOW
```

Redis Lua scripts are one common way of achieving this.

Redis executes a Lua script atomically with respect to other commands.

Therefore, another request cannot execute its validation in the middle of our current script.

The two requests will effectively be processed one after another.

```
Initial count = 4

Request A executes:
    Remove old timestamps
    Count = 4
    Insert timestamp
    ALLOW

Request B executes:
    Remove old timestamps
    Count = 5
    REJECT
```

Correct!

**Important distinction:** Redis Lua scripts are atomic, but they block other Redis commands while executing. Therefore, keep the script small and avoid unbounded work.

In this case, the amount of work depends on the number of expired entries being deleted, which itself is one of the performance considerations of this algorithm.

# **9. A more complete Redis implementation**

Let's put the pieces together.

This is illustrative production-oriented Lua logic for the admission decision:

```
-- KEYS[1] = rate limiter key
--
-- ARGV[1] = window duration in milliseconds
-- ARGV[2] = request limit
-- ARGV[3] = unique request ID

local key = KEYS[1]

local windowMs = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local requestId = ARGV[3]

-- Use Redis server time as the shared clock.
local time = redis.call('TIME')

local now =
    tonumber(time[1]) * 1000 +
    math.floor(tonumber(time[2]) / 1000)

local windowStart = now - windowMs

-- Remove timestamps outside the sliding window.
redis.call(
    'ZREMRANGEBYSCORE',
    key,
    '-inf',
    windowStart
)

-- Count requests remaining in the window.
local count = redis.call('ZCARD', key)

-- Reject when the limit has been reached.
if count >= limit then

    local oldest = redis.call(
        'ZRANGE',
        key,
        0,
        0,
        'WITHSCORES'
    )

    local retryAt =
        tonumber(oldest[2]) + windowMs

    local retryAfter =
        math.max(0, math.ceil((retryAt - now) / 1000))

    return {
        0,
        0,
        retryAfter
    }
end

-- Record the newly admitted request.
redis.call(
    'ZADD',
    key,
    now,
    requestId
)

-- Let Redis clean up the key when it is no longer useful.
redis.call(
    'PEXPIRE',
    key,
    windowMs + 1000
)

local remaining = limit - count - 1

return {
    1,
    remaining,
    0
}
```

The script returns three values:

```
{
    allowed,
    remainingRequests,
    retryAfterSeconds
}
```

For example:

```
{1, 3, 0}
```

Means:

```
Allowed = true
Remaining = 3
```

And:

```
{0, 0, 4}
```

Means:

```
Allowed = false
Remaining = 0
Retry after = 4 seconds
```

A few implementation details are worth understanding:

**Why Redis TIME?**

If we have ten application servers, their clocks might differ slightly.

Since all requests for this key use the same Redis server clock, the window calculations are consistent.

**Why PEXPIRE?**

We don't need to retain timestamps forever.

When the last recent request becomes irrelevant, Redis can delete the entire key.

We refresh the TTL whenever we accept a request.

**Why don't we insert rejected requests?**

The normal purpose of an API admission limiter is to track admitted requests. Rejected requests don't consume additional admission capacity.

There are exceptions. For example, a login-abuse protection system may deliberately track every authentication attempt, including failed attempts, using a separate security counter.

**Why use a unique request ID?**

Two requests may arrive in the same millisecond. Using unique members ensures that we count both of them.

In a real system, the request ID must be unique for each arrival, not merely a potentially reused business idempotency key.

# **10. What about the key's expiration?**

Let's understand this separately because it's a common implementation question.

Suppose our Sorted Set contains:

```
[
    request at 100,
    request at 105,
    request at 108
]
```

The window duration is 10 seconds.

When time reaches 118:

```
window = (108, 118]
```

All three requests are now outside the window.

The next incoming request will remove them.

But what happens if no further requests arrive?

Without expiration, the Redis key could remain forever.

We don't want this.

So, whenever we accept a request at time `T`, we set the expiration to approximately:

```
window duration + small cleanup grace period
```

For example:

```
Window = 10 seconds
TTL = 11 seconds
```

If no more requests arrive, Redis eventually deletes the key.

Why is this safe?

Because after 10 seconds, all timestamps associated with the key are too old to influence the rate-limit decision.

The extra second is simply a cleanup allowance.

This also means we don't need a scheduled cleanup job that periodically scans every user.

# **11. What happens when a request is rejected?**

There is another useful piece of information we can calculate.

Suppose:

```
Limit = 5
Window = 10 seconds

Existing timestamps:
[10, 12, 13, 15, 18]
```

A request arrives at time 19.

The window is:

```
(9, 19]
```

All five requests are still within the window.

Therefore, we reject the request.

But for how long should the client wait?

Look at the oldest request:

```
10, 12, 13, 15, 18
^
Oldest request
```

At time 20, the request at time 10 becomes exactly 10 seconds old and falls outside our window.

At that moment:

```
Remaining timestamps:
[12, 13, 15, 18]
```

Now the client can be allowed another request.

Therefore:

```
Retry time = Oldest request timestamp + Window duration

           = 10 + 10

           = 20 seconds
```

At current time 19:

```
Retry after = 20 - 19
            = 1 second
```

We can use this to return a helpful response:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 1
Content-Type: application/json
```

```
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests",
  "retryAfterSeconds": 1
}
```

The idea is simple:

**When the limit is reached, the oldest request is the first one that will free up capacity.**

We can use its expiration time to calculate when the next request might be accepted.

Note that `Retry-After` tells the client the earliest time that one slot should become available. The client should still handle the possibility of another concurrent request consuming that slot.

# **12. Advantages of Sliding Window Log**

Now that we understand the implementation, let's evaluate it.

### **1. Accurate rolling-window enforcement**

This is the biggest benefit.

If the policy says:

```
Maximum 100 requests in any 60-second period.
```

We can enforce that policy against the actual timestamps.

There is no Fixed Window boundary burst problem.

### **2. Fine-grained control**

We track individual request timestamps.

We know exactly when requests happened, so we can determine which requests are still inside the current window.

### **3. Flexible window durations**

The window doesn't have to align with a minute, an hour, or any particular calendar boundary.

For example:

```
20 requests in any 7 seconds
```

Works just as naturally as:

```
100 requests in any 60 seconds
```

### **4. Useful additional information**

Because we know the actual request timestamps, we can calculate:

- Remaining capacity.
- Earliest retry time.
- How frequently a client is sending requests.
- Recent request history.

# **13. Disadvantages of Sliding Window Log**

The algorithm is accurate, but that accuracy comes at a cost.

### **1. High memory consumption**

This is the biggest drawback.

Remember Fixed Window?

For Fixed Window:

```
user_123 -> counter = 50
```

One counter represents 50 requests.

For Sliding Window Log:

```
user_123 -> [
    timestamp_1,
    timestamp_2,
    timestamp_3,
    ...
    timestamp_50
]
```

We store every admitted request.

Imagine our application has:

```
1 million active users

Each user:
    100 requests per minute
```

In the worst case, we could need to retain:

\\[ 1,000,000 \times 100 \\]

\\[ = 100,000,000 \\]

That is 100 million timestamp entries!

And this is before considering the overhead of Redis data structures and unique member IDs.

Compare that with Fixed Window, which might need roughly one counter per active identity and policy.

The storage difference can become enormous.

### **2. More expensive writes**

Every accepted request requires an individual insertion.

We also need to remove expired timestamps.

This is more work than incrementing a counter.

With Redis Sorted Sets, adding and removing members generally involves logarithmic data-structure operations.

For example, inserting one member into a set of \\(N\\) entries costs approximately:

\\[ O(\log N) \\]

Removing \\(K\\) expired members costs approximately:

\\[ O(\log N + K) \\]

The precise practical cost depends on the number of expired entries and datastore implementation.

### **3. Cleanup can be expensive**

Imagine a popular API key has accumulated a very large number of request timestamps.

When a new request arrives after a quiet period, the cleanup operation may need to remove a large number of old entries.

That creates additional work during request processing.

### **4. More expensive distributed storage**

If Redis is shared among multiple application servers, all those timestamp insertions, removals, and lookups become datastore operations.

At high traffic volumes, the Redis workload itself can become a bottleneck.

### **5. It still permits bursts**

This is an important distinction.

Sliding Window Log prevents boundary bursts, but it does not smooth traffic.

Suppose the policy is:

```
100 requests in any 60 seconds
```

A client could send all 100 requests simultaneously.

They would all be allowed, assuming no other requests occurred in that window.

The next request would be rejected.

So this algorithm guarantees a maximum number of requests, not a maximum instantaneous request rate.

That's a very important distinction that will help us understand Leaky Bucket and Token Bucket later.

# **14. Sliding Window Log vs Fixed Window**

Let's compare the two algorithms directly.

| Property                  | Fixed Window                | Sliding Window Log                   |
| ------------------------- | --------------------------- | ------------------------------------ |
| What is stored?           | Counter                     | Individual timestamps                |
| Window                    | Predefined interval         | Relative to current request          |
| Boundary burst            | Possible                    | Prevented                            |
| Accuracy                  | Limited by fixed boundaries | Exact for the defined rolling window |
| Storage                   | Low                         | Potentially high                     |
| Write operation           | Increment counter           | Insert timestamp                     |
| Cleanup                   | Key expiration              | Remove old timestamps and expire key |
| Implementation complexity | Low                         | Higher                               |
| Burst smoothing           | No                          | No                                   |

An important observation:

Both algorithms allow a client to consume the entire quota immediately.

The difference is that Sliding Window Log ensures the client cannot exploit a fixed time boundary to consume two quotas almost instantly.

# **15. When should we use Sliding Window Log?**

Let's connect it to actual applications.

| Scenario                             | Suitability            | Reason                                              |
| ------------------------------------ | ---------------------- | --------------------------------------------------- |
| Small API with strict rolling quotas | Good                   | Accurate enforcement is valuable.                   |
| High-volume public API               | Depends                | Per-request storage can become expensive.           |
| Security-sensitive attempt tracking  | Good                   | Individual attempt timestamps can be useful.        |
| Daily or monthly quotas              | Usually unnecessary    | A simple counter is often sufficient.               |
| Strict third-party API quotas        | Good                   | Useful when the provider specifies a rolling limit. |
| Smooth background job execution      | Not suitable by itself | It doesn't spread requests over time.               |

For example, suppose we integrate with a third-party service that specifies:

```
Maximum 20 requests in any 10-second period.
```

We need to make sure we don't exceed that rolling limit.

Sliding Window Log gives us an accurate mechanism to track the requests admitted during the last 10 seconds.

However, if our system needs to enforce:

```
Do not send more than 2 requests per second.
```

And we need to distribute traffic smoothly, we should investigate another approach.

That's where Leaky Bucket becomes interesting.

# **16. A very important mental model: What exactly does each algorithm control?**

Before finishing, let's distinguish three different requirements that often get confused.

**Requirement A: Limit the total number of requests during a period.**

```
Maximum 100 requests in any 60 seconds.
```

This is a counting problem.

Fixed Window and Sliding Window algorithms solve this in different ways.

**Requirement B: Prevent sudden bursts from crossing a boundary.**

```
Maximum 100 requests in any rolling 60 seconds.
```

Sliding Window Log is more accurate than Fixed Window.

**Requirement C: Make sure requests are processed at a controlled rate.**

```
Process requests at 2 requests per second.
```

This is a traffic-smoothing problem.

Neither Fixed Window nor Sliding Window Log solves this by itself.

That is the problem we will eventually address with Leaky Bucket.

These distinctions are more important than memorizing algorithm names.

# **Chapter 2 — Final mental model**

Imagine a user sends requests to our application.

With Fixed Window, we remember:

```
How many requests happened in this predefined interval?
```

With Sliding Window Log, we remember:

```
Exactly when did each recent request happen?
```

When a new request arrives, we move the observation window forward, discard requests that are too old, count the remaining requests, and decide whether to accept the new one.

The algorithm is accurate because it maintains the actual request history.

Its weakness is equally straightforward:

**Remembering every request is expensive.**

We have now encountered a natural tradeoff:

```
Fixed Window:
    Low storage
    Less accurate around boundaries

Sliding Window Log:
    High storage
    Accurate rolling-window enforcement
```

The next natural question is:

> Can we achieve something close to Sliding Window Log's accuracy without remembering every single request?

That's precisely why the next algorithm exists.

## **Next Chapter — Sliding Window Counter**

We will start with the same requirement:

```
5 requests in any 10-second period.
```

But instead of keeping every timestamp, we'll maintain counters for adjacent windows and calculate how much of the previous window should still contribute to the current limit.

We'll derive the calculation visually, understand why the weighting formula works, implement it with Redis, and examine its accuracy limitations.

The key idea will be understanding how we can approximate a rolling window using only a couple of counters.
