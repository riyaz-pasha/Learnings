# **Rate Limiter — A Complete System Design Course**

We will learn this by building the mental model from scratch.

The most important thing I want you to understand is that **rate limiting is not primarily about counting requests. It is about controlling how quickly a system is allowed to consume a resource.**

Once that becomes clear, the algorithms become different ways of answering one question:

> Given the requests received so far, should this new request be allowed to proceed?

We'll build up to answering that question in different ways.

## **Course roadmap**

| Chapter                                    | What we will learn                                                                                    |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| **1. Foundations + Fixed Window**          | Why rate limiting exists, what it protects, how policies work, and our first algorithm.               |
| **2. Sliding Window Log**                  | Why Fixed Window allows sudden bursts, and how to enforce a genuinely rolling time limit.             |
| **3. Sliding Window Counter**              | How to get a practical approximation of a sliding window without storing every request.               |
| **4. Leaky Bucket**                        | How to smooth traffic and process requests at a controlled rate.                                      |
| **5. Token Bucket**                        | How to allow controlled bursts while enforcing a long-term rate.                                      |
| **6. Comparing All Algorithms**            | Which algorithm to choose for different real-world problems.                                          |
| **7. Building a Distributed Rate Limiter** | Redis, atomicity, concurrent requests, multiple application instances, failures, and race conditions. |
| **8. Production-Level Design**             | Policies, multiple limits, API gateways, headers, monitoring, configuration, and scaling.             |

Let's begin.

# **Chapter 1 — Understanding Rate Limiting and Fixed Window**

## **1. First, why do we need a Rate Limiter?**

Imagine we have built a backend service that exposes an API:

```
GET /api/products
```

Normally, our application handles requests without any problem.

For example:

- User A makes 5 requests per minute.
- User B makes 10 requests per minute.
- User C makes 3 requests per minute.

Our application handles these requests, reads from the database, and returns responses.

Everything is fine.

Now imagine a different situation.

A client accidentally writes a bug:

```
while (true) {
    await fetch("/api/products");
}
```

It starts sending thousands of requests.

Or perhaps somebody writes a script to repeatedly call our API.

Or a popular application suddenly sends a huge amount of traffic because thousands of users perform the same operation simultaneously.

What happens?

Our backend receives more requests than it can reasonably handle.

It might cause:

1. Increased CPU and memory consumption.
2. Increased database connections and queries.
3. Database overload.
4. Increased response latency.
5. Request timeouts.
6. Eventually, service failure.

The interesting thing is that **the backend might be perfectly healthy when handling normal traffic. The problem is that clients are consuming resources faster than the backend can handle them.**

We need a mechanism to control that consumption.

That's where rate limiting comes in.

## **2. What exactly should a Rate Limiter do?**

Let's introduce a separate component.

Whenever a request arrives, we ask the Rate Limiter:

> Should this request be allowed to reach the actual application?

Our request flow becomes:

\#chatgpt-mermaid-\_r_gv\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_gv\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_gv\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_gv\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_gv\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_gv\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_gv\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_gv\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_gv\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_gv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_gv\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_gv\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_gv\_ p{margin:0;}#chatgpt-mermaid-\_r_gv\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_gv\_ .label text,#chatgpt-mermaid-\_r_gv\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .node rect,#chatgpt-mermaid-\_r_gv\_ .node circle,#chatgpt-mermaid-\_r_gv\_ .node ellipse,#chatgpt-mermaid-\_r_gv\_ .node polygon,#chatgpt-mermaid-\_r_gv\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .rough-node .label text,#chatgpt-mermaid-\_r_gv\_ .node .label text,#chatgpt-mermaid-\_r_gv\_ .image-shape .label,#chatgpt-mermaid-\_r_gv\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_gv\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .rough-node .label,#chatgpt-mermaid-\_r_gv\_ .node .label,#chatgpt-mermaid-\_r_gv\_ .image-shape .label,#chatgpt-mermaid-\_r_gv\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_gv\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_gv\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_gv\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_gv\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_gv\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_gv\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_gv\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_gv\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_gv\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_gv\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_gv\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_gv\_ .icon-shape,#chatgpt-mermaid-\_r_gv\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_gv\_ .icon-shape p,#chatgpt-mermaid-\_r_gv\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_gv\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_gv\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_gv\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_gv\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_gv\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_gv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_gv\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_gv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_gv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_gv\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_gv\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_gv\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_gv\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_gv\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_gv\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_gv\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .node rect,#chatgpt-mermaid-\_r_gv\_ .node circle,#chatgpt-mermaid-\_r_gv\_ .node ellipse,#chatgpt-mermaid-\_r_gv\_ .node polygon,#chatgpt-mermaid-\_r_gv\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_gv\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_gv\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_gv\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_gv\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_gv\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientRate LimiterAllowed?Backend ApplicationHTTP 429 Too ManyRequestsResponseYesNo

The Rate Limiter makes a decision before the expensive operation takes place.

For example, we might define this policy:

```
A user can make at most 100 requests per minute.
```

Suppose a user makes requests:

| Request | Total requests in the current period | Decision |
| ------- | ------------------------------------ | -------- |
| 1       | 1                                    | Allow    |
| 2       | 2                                    | Allow    |
| 3       | 3                                    | Allow    |
| ...     | ...                                  | ...      |
| 99      | 99                                   | Allow    |
| 100     | 100                                  | Allow    |
| 101     | 101                                  | Reject   |
| 102     | 102                                  | Reject   |

The Rate Limiter needs to maintain some information about the requests already received.

When a new request comes in, it checks that information and makes a decision.

Notice that we haven't discussed any algorithm yet.

We have only established three fundamental things:

- There is a **limit**.
- There is some **state** describing previous requests.
- There is a **decision** based on that state.

Every rate-limiting algorithm we will study is simply a different way of managing that state and making that decision.

## **3. Before algorithms, understand what we are limiting**

This is important because real-world rate limiting is not always based on individual users.

Imagine we have an application with the following APIs:

```
GET    /api/products
GET    /api/orders
POST   /api/orders
POST   /api/payments
POST   /api/login
```

Should every endpoint have the same rate limit?

Probably not.

Consider the following requirements:

| Scenario                | Example policy                                       |
| ----------------------- | ---------------------------------------------------- |
| General API usage       | 100 requests per user per minute                     |
| Login protection        | 5 attempts per IP per minute                         |
| Payment API             | 10 requests per user per minute                      |
| Tenant isolation        | 1,000 requests per tenant per minute                 |
| Third-party integration | 50 requests per API key per minute                   |
| Global protection       | 10,000 requests across the entire service per second |

These are different policies because they protect different resources and apply to different groups of requests.

### **3.1 What identifies a client?**

We need an identity or dimension to which a limit applies.

Possible identities include:

- User ID.
- API key.
- IP address.
- Tenant ID.
- Organization ID.
- Device ID.
- A combination of these.

For example:

```
Limit: 100 requests per minute

Identity: user_123
```

The Rate Limiter tracks the requests made by `user_123`.

But consider an unauthenticated login API.

We don't have a user ID yet.

We might instead use:

```
Limit: 5 login attempts per minute

Identity: client IP address
```

For a multi-tenant application, we might need both:

```
Limit 1: 100 requests/minute per user
Limit 2: 10,000 requests/minute per tenant
```

One request may need to satisfy multiple limits.

We'll return to this when designing the production system.

### **3.2 What makes a limit?**

A rate-limit policy generally contains:

```
Policy
 ├── Identity / Scope
 ├── Maximum allowed requests
 ├── Time period or rate
 └── Algorithm
```

For example:

```
{
  "policy": "product-api",
  "identity": "user_123",
  "limit": 100,
  "window_seconds": 60,
  "algorithm": "fixed-window"
}
```

This means that the user can make 100 requests in a 60-second window.

Different algorithms interpret the time period differently. That's precisely what we will explore throughout this course.

# **4. Our first algorithm: Fixed Window**

Let's start with the simplest possible algorithm.

Imagine a requirement:

> A user can make at most 5 requests every 10 seconds.

How would you implement this?

A natural first idea is:

1. Maintain a counter for the user.
2. Increment it whenever a request arrives.
3. Reject the request if the counter exceeds 5.
4. Reset the counter after 10 seconds.

That's essentially the Fixed Window algorithm.

But we need to understand exactly what a *window* means.

## **4.1 What is a Fixed Window?**

Imagine dividing time into separate, fixed-sized intervals.

Our window size is 10 seconds.

We can divide the timeline like this:

```
          Window 1              Window 2              Window 3
     |------------------|------------------|------------------|
Time 0                  10                 20                 30
```

Each window has its own counter.

For a user with a limit of 5 requests per window:

```
Window 1:
    counter = 0 initially

Window 2:
    counter = 0 initially

Window 3:
    counter = 0 initially
```

When a request arrives, we identify which window it belongs to and increment that window's counter.

For example:

```
Window 1 (0s - 10s)
    Request 1 -> counter = 1
    Request 2 -> counter = 2
    Request 3 -> counter = 3
    Request 4 -> counter = 4
    Request 5 -> counter = 5
    Request 6 -> REJECT
```

When time reaches 10 seconds, a new window starts.

```
Window 2 (10s - 20s)
    Request 1 -> counter = 1
    Request 2 -> counter = 2
```

The counter starts fresh because this is a different window.

That's the entire basic idea.

## **4.2 How do we identify the current window?**

We need to determine which window the current request belongs to.

Assume:

```
Window duration = 10 seconds
```

Use Unix time (seconds since January 1, 1970).

Let:

- `now` = current Unix timestamp.
- `W` = window duration.

We calculate:

\\[ windowId = \left\lfloor \frac{now}{W} \right\rfloor \\]

For example, suppose:

```
W = 10 seconds
```

Then:

| Current timestamp | Window ID |
| ----------------- | --------- |
| 100               | 10        |
| 101               | 10        |
| 105               | 10        |
| 109               | 10        |
| 110               | 11        |
| 111               | 11        |
| 119               | 11        |
| 120               | 12        |

Notice something important:

The window ID changes automatically when we cross a window boundary.

We don't actually need a background process that wakes up every 10 seconds to reset counters.

We can simply maintain a different counter for each window.

That observation will make our implementation much simpler.

## **4.3 How do we store the data?**

We need to store how many requests a particular identity has made in the current window.

For example:

```
User: user_123
Window ID: 10
Request count: 4
```

A simple key-value representation would be:

| Key           | Value |
| ------------- | ----- |
| `user_123:10` | 4     |
| `user_456:10` | 2     |
| `user_123:11` | 3     |

Here:

- `user_123:10` means user 123's counter for window 10.
- `user_123:11` means user 123's counter for window 11.

We are not storing every request.

We are only storing a counter for each identity and window.

This is one of the biggest advantages of Fixed Window.

### **A more realistic key structure**

In a real application, we need to consider the policy as well:

```
rate_limit:{policy_id}:{identity}:{window_id}
```

For example:

```
rate_limit:products:user_123:10
rate_limit:products:user_456:10
rate_limit:login:192.168.1.10:10
rate_limit:payments:user_123:10
```

Why include the policy?

Because the same user might have different limits for different endpoints.

We don't want product requests to accidentally consume the payment API's limit.

# **5. How does the Rate Limiter validate a request?**

Let's implement the decision process conceptually.

Our policy:

```
Maximum requests = 5
Window duration = 10 seconds
Identity = user_123
```

When a request arrives, the Rate Limiter does the following.

**Step 1: Identify the caller.**

```
identity = user_123
```

**Step 2: Get the current time and determine the window.**

```
now = 105
window_duration = 10

window_id = floor(105 / 10)
          = 10
```

**Step 3: Build the storage key.**

```
key = rate_limit:products:user_123:10
```

**Step 4: Increment the counter.**

Suppose its previous value was 3.

```
counter = 4
```

**Step 5: Compare it against the limit.**

```
limit = 5

counter <= limit
4 <= 5
```

The request is allowed.

The next request:

```
counter = 5

5 <= 5
```

Also allowed.

The next one:

```
counter = 6

6 <= 5
```

Rejected.

Our decision logic is:

```
function allowRequest(identity, policy):

    now = getCurrentTime()

    windowId = floor(now / policy.windowDuration)

    key = buildKey(
        policy.id,
        identity,
        windowId
    )

    count = incrementCounter(key)

    if count <= policy.limit:
        return ALLOW

    return REJECT
```

Notice that we increment *before* checking.

This ensures that the request which reaches the limit is allowed, while the next one is rejected.

# **6. What happens to old counters? Do we need to delete them?**

Suppose we store:

```
rate_limit:products:user_123:10 = 5
```

When window 10 ends, this key is no longer needed for making decisions about future windows.

Window 11 uses an entirely different key.

But we don't want to keep old keys forever.

We should give each counter a **TTL (Time To Live)**.

For example, if the window ends at timestamp 110 and it is currently timestamp 105:

```
Remaining window time = 110 - 105
                      = 5 seconds
```

We can set the key to expire after approximately 5 seconds, perhaps with a small cleanup grace period.

Conceptually:

```
SET counter key with expiration
```

Or, using Redis:

```
INCR key
EXPIRE key 10
```

The second example is only illustrative. In production, we must handle expiration and increment atomically, which we'll discuss next.

The important idea:

**The key only needs to survive long enough for its corresponding window to finish.**

Since every new window has a different key, the old counter cannot affect the new window.

# **7. Very important: What happens when multiple requests arrive simultaneously?**

So far, our implementation looks correct.

But imagine 10 requests arrive simultaneously for the same user.

The current counter is:

```
counter = 4
limit = 5
```

Only one more request should be allowed.

However, consider this implementation:

```
count = GET(key)

if count < limit:
    INCREMENT(key)
    return ALLOW

return REJECT
```

This creates a race condition.

Imagine two requests, A and B.

```
Initial counter = 4

Request A: GET counter -> 4
Request B: GET counter -> 4

Request A: 4 < 5 -> ALLOW
Request B: 4 < 5 -> ALLOW

Request A: INCREMENT -> 5
Request B: INCREMENT -> 6
```

Both requests were allowed, even though only one remaining request was available.

This violates our rate-limit policy.

### **The solution: Atomic operation**

We need the following operations to behave as one indivisible operation:

1. Increment the counter.
2. Set expiration when needed.
3. Compare the resulting counter with the limit.
4. Return the decision.

No other request should observe an intermediate state and make a conflicting decision.

For a Redis-based implementation, we commonly use a Lua script to perform these operations atomically.

Conceptually:

```
ATOMIC OPERATION:

    count = INCREMENT(key)

    if this is the first increment:
        SET EXPIRATION(key)

    if count <= limit:
        return ALLOW

    return REJECT
```

Now, with two simultaneous requests:

```
Initial counter = 4

Request A executes atomic operation:
    counter becomes 5
    ALLOW

Request B executes atomic operation:
    counter becomes 6
    REJECT
```

Correct!

**This is a fundamental distributed-systems principle: when correctness depends on multiple operations happening together, they must be atomic.**

We'll explore the distributed implementation in much greater detail in Chapter 7.

One additional production concern: all application instances should agree on the window boundary. Ideally, derive the time from a common authoritative clock, such as the rate-limit datastore, rather than trusting potentially different application-server clocks.

# **8. The biggest problem with Fixed Window**

At this point, you might think:

> We have a counter, a window, and atomic increments. What's wrong with this algorithm?

Let's examine a scenario.

Our policy:

```
5 requests per 10 seconds
```

Windows:

```
Window 1: 12:00:00 - 12:00:10
Window 2: 12:00:10 - 12:00:20
```

Imagine the following traffic:

```
12:00:09.800
    Request 1 -> ALLOW
    Request 2 -> ALLOW
    Request 3 -> ALLOW
    Request 4 -> ALLOW
    Request 5 -> ALLOW

12:00:10.100
    Request 6 -> ALLOW
    Request 7 -> ALLOW
    Request 8 -> ALLOW
    Request 9 -> ALLOW
    Request 10 -> ALLOW
```

Look at what happened.

| Time         | Window   | Requests allowed |
| ------------ | -------- | ---------------- |
| 12:00:09.800 | Window 1 | 5                |
| 12:00:10.100 | Window 2 | 5                |

We allowed:

**10 requests within just 300 milliseconds!**

But our policy says:

```
5 requests per 10 seconds
```

How did this happen?

Because Fixed Window doesn't actually measure requests over *any* 10-second period.

It measures requests within a particular predefined 10-second interval.

The boundary is the problem.

The counter resets at the boundary, even though only a fraction of a second has passed since the previous requests.

This is called the **boundary burst problem**.

### **Visualizing the problem**

```
        Fixed Window 1          Fixed Window 2
     |---------------------|---------------------|
Time 0                    10                    20
                          ^
                          |
                     Counter resets

Requests:
                      XXXXX XXXXX
                      5 requests 5 requests

                         300ms
                    <------------->
```

The algorithm is behaving correctly according to its own rules.

But its rules don't strictly enforce what many people intuitively mean by "5 requests in any 10-second period."

That's the key limitation.

# **9. Fixed Window — Advantages and Disadvantages**

Now we can evaluate the algorithm properly.

### **Advantages**

**1. Simple to understand and implement**

We only need a counter for each identity and time window.

**2. Very low storage requirements**

We don't store individual requests.

For every identity and active window, we generally store one counter.

**3. Efficient validation**

The common operation is an atomic increment followed by a comparison.

**4. Easy to distribute**

A shared datastore such as Redis can hold counters for multiple application instances.

**5. Natural expiration**

Counters can automatically expire after their window ends.

### **Disadvantages**

**1. Boundary burst problem**

The biggest drawback. A client can consume the limit immediately before and after a window boundary.

**2. Traffic can be uneven**

The algorithm doesn't control how quickly requests arrive within a window.

For example, 100 requests could arrive within one millisecond, provided the counter hasn't reached the limit.

**3. It doesn't control concurrent work**

Even if we allow only 100 requests per second, those 100 requests could all be long-running operations. Rate limiting alone does not limit how many operations are executing simultaneously.

**4. It does not smooth traffic**

It only allows or rejects requests. It doesn't spread accepted requests over time.

These limitations lead us to the next algorithms.

# **10. When should we use Fixed Window?**

Despite its limitations, Fixed Window is useful.

Consider these situations:

| Scenario                                             | Suitable?  | Reason                                                                             |
| ---------------------------------------------------- | ---------- | ---------------------------------------------------------------------------------- |
| Basic public API protection                          | Yes        | Simple and inexpensive protection against excessive request volume.                |
| Per-user daily API quota                             | Yes        | A daily quota often maps naturally to calendar-based windows.                      |
| Monthly usage limits                                 | Yes        | Easy to count usage across billing periods.                                        |
| Strict control over every rolling 10-second interval | No         | Boundary bursts violate the intended semantics.                                    |
| Smooth processing of background jobs                 | No         | It doesn't control the rate at which accepted work executes.                       |
| Strict third-party API throttling                    | Not always | Whether it suffices depends on how the provider defines its quota and burst rules. |

For example, a SaaS application might enforce:

```
Free plan:
    10,000 API requests per day

Pro plan:
    100,000 API requests per day

Enterprise plan:
    1,000,000 API requests per day
```

Fixed Window is a reasonable choice for these daily quotas.

But for a third-party service that says:

```
Maximum 10 requests in ANY 10-second period
```

we need something more sophisticated.

And that's what brings us to the next algorithm.

## **11. What have we actually learned?**

Let's reduce the entire chapter to the mental model.

We want to restrict how much a client can consume a resource.

Our first solution was:

1. Divide time into fixed intervals.
2. Maintain a counter for each identity and interval.
3. Increment the counter when a request arrives.
4. Allow the request if the counter is within the limit.
5. Reject it otherwise.
6. Expire the counter when the interval finishes.

The reason Fixed Window is so efficient is that it doesn't need to remember individual requests.

But that same simplification causes its major limitation: it forgets everything about previous windows, even when those requests happened just moments ago.

**The limitation is not an implementation bug. It's a consequence of how the algorithm defines time.**

That observation is the foundation for understanding the next algorithm.

## **Next: Chapter 2 — Sliding Window Log**

We'll take the exact problem we discovered:

```
5 requests per 10 seconds
```

And change the question from:

> How many requests happened in this predefined window?

To:

> How many requests happened during the 10 seconds immediately preceding this request?

We'll derive the algorithm from that question, then explore what data must be stored, how old requests are removed, how concurrent validation works, and why this approach is more accurate but more expensive.

The aim is to understand *why* each algorithm exists, so you can derive its implementation rather than memorize it.
