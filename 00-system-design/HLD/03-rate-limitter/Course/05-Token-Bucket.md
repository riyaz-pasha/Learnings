# **Chapter 5 — Token Bucket Algorithm**

This is one of the most useful rate-limiting algorithms in real-world backend systems.

We'll derive it from the limitation we discovered in Leaky Bucket.

In particular, we'll understand why Token Bucket is commonly used for API gateways, public REST APIs, microservices, and third-party API integrations.

## **1. Why do we need Token Bucket?**

Let's revisit our previous example.

Imagine an external API has the following policy:

```
Average allowed rate = 2 requests/second

Maximum burst = 10 requests
```

This means:

- The API generally wants us to send 2 requests per second.
- However, it permits us to send 10 requests immediately when sufficient capacity has accumulated.
- After consuming that burst capacity, we must wait for capacity to replenish.

Now imagine we use Leaky Bucket.

We configure:

```
Leak rate = 2 requests/second
```

Our dispatcher sends:

```
Request 1 -> 0.0 seconds
Request 2 -> 0.5 seconds
Request 3 -> 1.0 seconds
Request 4 -> 1.5 seconds
Request 5 -> 2.0 seconds
...
```

This produces smooth traffic.

But there's a problem.

**We're not taking advantage of the 10-request burst capacity allowed by the downstream API.**

The downstream service permits us to send 10 requests immediately, but our Leaky Bucket unnecessarily spreads those requests over 5 seconds.

What if we could accumulate some permission to send requests whenever we're not using the API?

For example:

```
When idle:
    Accumulate permission.

When requests arrive:
    Consume that permission.

When permission is exhausted:
    Wait until more permission becomes available.
```

This is the idea behind Token Bucket.

# **2. Understanding the token analogy**

Imagine a bucket that contains tokens instead of water.

```
              Tokens are added
                 over time
                     |
                     v
               ___________
              |  Token    |
              |  Token    |
              |  Token    |
              |  Token    |
              |  Token    |
              |___________|
                     |
                     |
              Requests consume
                  tokens
```

There are three fundamental rules.

**Rule 1: Tokens are generated over time.**

For example:

```
Refill rate = 2 tokens/second
```

The bucket receives 2 new tokens every second.

**Rule 2: Each incoming request consumes tokens.**

For example:

```
One request = 1 token
```

When a request arrives:

- If at least one token is available, consume one token and allow the request.
- If no token is available, reject the request or make it wait.

**Rule 3: The bucket has a maximum capacity.**

Suppose:

```
Bucket capacity = 10 tokens
```

Even if tokens are generated continuously, the bucket cannot hold more than 10.

When the bucket is already full, additional generated tokens are discarded.

Why?

Because we don't want an idle client to accumulate unlimited permission to send requests later.

These three rules are the entire foundation of Token Bucket.

Let's see what they mean in practice.

# **3. Understand the two configuration parameters**

Token Bucket has two particularly important parameters.

### **3.1 Refill rate**

The refill rate defines how quickly tokens are generated.

For example:

```
Refill rate = 2 tokens/second
```

This means:

```
Every second:
    Generate 2 tokens.
```

We can express it in different ways:

| Refill rate    | Equivalent token generation |
| -------------- | --------------------------- |
| 1 token/sec    | 1 token every second        |
| 2 tokens/sec   | 1 token every 500 ms        |
| 5 tokens/sec   | 1 token every 200 ms        |
| 10 tokens/sec  | 1 token every 100 ms        |
| 100 tokens/sec | 1 token every 10 ms         |

An important implementation detail:

We don't actually need a background process generating tokens every 500 milliseconds.

We can calculate how many tokens should have accumulated whenever a new request arrives.

We'll derive this shortly.

### **3.2 Bucket capacity**

The capacity defines the maximum number of tokens that can accumulate.

For example:

```
Capacity = 10 tokens
```

This allows a client to accumulate up to 10 tokens while idle.

The capacity determines the maximum immediate burst when the bucket is full.

Consider:

```
Refill rate = 2 tokens/second
Capacity = 10 tokens
```

The client can:

- Send 10 requests immediately if the bucket is full.
- Once empty, receive one token every 500 milliseconds.
- Send another request whenever a token becomes available.

This is how we support bursts without allowing unlimited long-term consumption.

Let's visualize the two parameters:

```
Refill rate:
    How quickly tokens become available.

Bucket capacity:
    How many unused tokens we can save.
```

Notice the difference:

- Refill rate controls long-term consumption.
- Bucket capacity controls burst allowance.

This distinction is extremely important.

# **4. Walk through a complete example**

Let's use a concrete scenario.

Our policy:

```
Token refill rate = 2 tokens/second
Bucket capacity = 10 tokens

One request = 1 token
```

For this example, assume the bucket starts completely full.

That means:

```
Initial tokens = 10
```

Why start full?

Because we want the client to be able to use the entire configured burst capacity immediately, even when the application has just started.

There are other initialization policies, which we'll discuss later.

## **4.1 Client sends 10 requests immediately**

Our bucket initially contains:

```
[ T T T T T T T T T T ]

Available tokens = 10
```

Now the client sends requests.

```
Request 1:
    Consume 1 token.
    Remaining = 9.
    ALLOW.

Request 2:
    Consume 1 token.
    Remaining = 8.
    ALLOW.

Request 3:
    Consume 1 token.
    Remaining = 7.
    ALLOW.
```

Continue the same way.

After 10 requests:

```
Available tokens = 0
```

All 10 requests were allowed.

## **4.2 Client sends another request immediately**

The bucket is empty.

```
Available tokens = 0
```

Another request arrives.

```
Required tokens = 1
Available tokens = 0

Decision = REJECT
```

We cannot allow it because no token is available.

## **4.3 Wait for 500 milliseconds**

Our refill rate is:

```
2 tokens/second
```

After 500 milliseconds:

\\[ TokensGenerated = 2 \times 0.5 \\]

\\[ = 1 \\]

The bucket now contains:

```
Available tokens = 1
```

The next request arrives.

```
Required tokens = 1
Available tokens = 1

Decision = ALLOW
```

We consume the token.

The bucket becomes empty again.

## **4.4 Wait for another 1.5 seconds**

Our refill rate is still 2 tokens per second.

After 1.5 seconds:

\\[ TokensGenerated = 2 \times 1.5 \\]

\\[ = 3 \\]

The bucket contains three tokens.

The client can immediately send three requests.

After those requests:

```
Available tokens = 0
```

## **4.5 What if the client remains idle?**

Imagine the bucket is empty and the client does not send any requests for 10 seconds.

Tokens generated:

\\[ 2 \times 10 = 20 \\]

But our capacity is only 10.

Therefore:

\\[ AvailableTokens = \min(10, 20) \\]

\\[ = 10 \\]

The bucket becomes full.

It cannot accumulate more than 10 tokens.

The client can now send another burst of 10 requests immediately.

This is the behavior we wanted!

# **5. Deriving the Token Bucket algorithm mathematically**

Now let's move from the analogy to the actual implementation.

We need to answer two questions:

1. How many tokens should be available right now?
2. Should we allow the incoming request?

The important observation is that **we don't need to generate tokens continuously. We can calculate how many tokens should have accumulated whenever a request arrives.**

This makes the implementation much simpler.

## **5.1 What information do we need to store?**

Suppose our bucket has:

```
Capacity = 10 tokens
Refill rate = 2 tokens/second
```

At some point, the bucket contains:

```
Available tokens = 4
```

And the last time we updated its token count was:

```
Last refill time = 100 seconds
```

Now a request arrives at:

```
Current time = 102 seconds
```

We know 2 seconds have elapsed.

Since we generate 2 tokens per second:

\\[ GeneratedTokens = 2 \times 2 \\]

\\[ = 4 \\]

Therefore:

```
Previously available tokens = 4
Newly generated tokens = 4

Total = 8
```

We cap it at the bucket capacity:

```
Available tokens = min(10, 8)

                  = 8
```

Now we can process the incoming request.

This gives us the information we need to store:

```
Token Bucket State:

    available_tokens
    last_refill_time
```

The policy configuration contains:

```
Token Bucket Policy:

    capacity
    refill_rate
```

That's it!

We don't store individual request timestamps.

We don't need a queue for the basic Token Bucket algorithm.

We only maintain a token balance and a timestamp.

## **5.2 Derive the formula**

Let's define:

- \\(B\\): Maximum bucket capacity.
- \\(T\\): Previously available tokens.
- \\(r\\): Refill rate in tokens per second.
- \\(now\\): Current timestamp.
- \\(last\\): Last refill timestamp.
- \\(\Delta t\\): Time elapsed.

First, calculate elapsed time:

\\[ \Delta t = now - last \\]

Next, calculate generated tokens:

\\[ GeneratedTokens = r \times \Delta t \\]

Add them to the previous balance:

\\[ T + GeneratedTokens \\]

But we cannot exceed capacity.

Therefore:

\\[ \boxed{ T\_{available} = \min(B,\ T + r \times \Delta t) } \\]

This is the most important Token Bucket formula.

Once we calculate the available tokens, we check the incoming request.

Let:

\- \\(C\\): Token cost of the incoming request.

The request is allowed when:

\\[ T\_{available} \geq C \\]

If allowed:

\\[ \boxed{ T\_{new} = T\_{available} - C } \\]

Otherwise:

\\[ T\_{new} = T\_{available} \\]

The request is rejected, but the bucket must still retain the tokens that have accumulated since its previous update.

We also update:

```
last_refill_time = now
```

These formulas are the entire algorithm.

# **6. Complete implementation walkthrough**

Let's now follow a request from beginning to end.

Our policy:

```
Bucket capacity = 10
Refill rate = 2 tokens/second
Request cost = 1 token
```

Our stored state:

```
available_tokens = 3
last_refill_time = 100
```

A new request arrives at time 102.

### **Step 1: Calculate elapsed time**

```
now = 102
last_refill_time = 100

elapsed = 2 seconds
```

### **Step 2: Calculate generated tokens**

\\[ GeneratedTokens = 2 \times 2 = 4 \\]

### **Step 3: Calculate the new available balance**

\\[ Available = \min(10,\ 3+4) \\]

\\[ = 7 \\]

### **Step 4: Check the incoming request**

```
Request cost = 1

Available tokens = 7
```

Since:

```
7 >= 1
```

We allow the request.

### **Step 5: Consume tokens**

\\[ NewBalance = 7-1 \\]

\\[ = 6 \\]

### **Step 6: Update state**

```
available_tokens = 6
last_refill_time = 102
```

The next request will calculate its refill starting from this updated state.

## **Complete pseudocode**

```
function allowRequest(identity, policy):

    now = getCurrentTime()

    state = getBucketState(identity, policy)

    if state does not exist:

        availableTokens = policy.capacity
        lastRefillTime = now

    else:

        elapsed = now - state.lastRefillTime

        generatedTokens =
            elapsed * policy.refillRate

        availableTokens = min(
            policy.capacity,
            state.availableTokens + generatedTokens
        )

        lastRefillTime = now

    requestCost = getRequestCost()

    if availableTokens >= requestCost:

        availableTokens -= requestCost

        saveState(
            availableTokens,
            lastRefillTime
        )

        return ALLOW

    saveState(
        availableTokens,
        lastRefillTime
    )

    return REJECT
```

Notice how simple the algorithm is.

The entire request validation requires:

- One clock reading.
- One state lookup.
- A few arithmetic operations.
- One state update.

This is why Token Bucket is so practical for high-throughput rate limiting.

# **7. A subtle detail: Tokens don't necessarily need whole numbers**

You might be wondering:

> We generate 2 tokens every second. What happens after only 200 milliseconds?

We generated:

\\[ 2 \times 0.2 = 0.4 \\]

We have 0.4 tokens, not one complete token.

That's perfectly valid.

We should preserve fractional token balances.

Consider:

```
Refill rate = 2 tokens/second
Initial balance = 0
```

As time progresses:

| Elapsed time | Generated tokens |
| ------------ | ---------------- |
| 100 ms       | 0.2              |
| 200 ms       | 0.4              |
| 300 ms       | 0.6              |
| 400 ms       | 0.8              |
| 500 ms       | 1.0              |
| 1 second     | 2.0              |

At 400 milliseconds, a request costing one token cannot be admitted.

At 500 milliseconds, one token is available.

This means we must preserve fractional balances instead of rounding down during every update.

Otherwise, we might lose accumulated refill credit repeatedly.

For example, if we rounded 0.4 down to zero at every 200 ms update, tokens might never accumulate correctly.

# **8. What happens when the bucket is already full?**

Let's examine another important scenario.

Our configuration:

```
Capacity = 10 tokens
Refill rate = 2 tokens/second
```

The bucket currently contains:

```
available_tokens = 10
```

The user waits for 5 seconds.

Generated tokens:

\\[ 2 \times 5 = 10 \\]

But the capacity is still 10.

Therefore:

\\[ Available = \min(10,\ 10+10) \\]

\\[ = 10 \\]

We discard the extra generated tokens.

Now imagine the user sends one request.

```
Available = 10 - 1
          = 9
```

The bucket has 9 tokens.

This is exactly why the capacity matters.

Without capacity, an idle client could accumulate unlimited tokens and eventually send an enormous burst.

With a bounded bucket, the maximum accumulated burst is predictable.

# **9. Calculate the maximum burst and long-term rate**

Let's examine the two parameters together.

Suppose:

```
Capacity = 20 tokens
Refill rate = 5 tokens/second
Request cost = 1 token
```

What does this configuration mean?

**Maximum immediate burst:**

20 requests, provided the bucket is full.

**Long-term refill:**

5 requests' worth of tokens per second.

If the bucket is initially full, a client can send:

```
Time 0:
    20 requests immediately.

Time 1:
    5 additional requests.

Time 2:
    5 additional requests.

Time 3:
    5 additional requests.
```

Assuming the client spends every available token, this is the allowed behavior.

We can summarize the parameters:

| Parameter    | Controls                                |
| ------------ | --------------------------------------- |
| Capacity     | Maximum accumulated burst               |
| Refill rate  | Long-term average allowance             |
| Request cost | How much capacity each request consumes |

An important mathematical property:

Over an interval of duration \\(t\\), a Token Bucket with capacity \\(B\\) and refill rate \\(r\\) permits at most approximately:

\\[ \boxed{B+r \times t} \\]

tokens to be consumed, assuming the bucket starts full.

For unit-cost requests, this corresponds to the maximum number of admitted requests, with integer request-count constraints.

For our example:

```
Capacity = 20
Refill rate = 5/sec
Duration = 10 seconds
```

Maximum token consumption:

\\[ 20 + 5 \times 10 = 70 \\]

So the bucket can permit up to 70 requests over 10 seconds if it starts full.

This is not the same as a strict rolling-window policy of 50 requests in every 10-second interval.

That's an important distinction.

**Token Bucket controls sustained consumption while permitting a configured burst. It does not enforce a strict maximum request count in every arbitrary time interval.**

# **10. How do we choose capacity and refill rate?**

This is a common real-world configuration question.

Imagine a service with:

```
Maximum sustained request rate = 100 requests/second

Desired immediate burst = 500 requests
```

We configure:

```
Refill rate = 100 tokens/second
Capacity = 500 tokens
```

Each request consumes one token.

This means:

- When full, the client can send 500 requests immediately.
- Once empty, it receives 100 tokens per second.
- If it remains idle, the bucket gradually refills.
- It can never accumulate more than 500 tokens.

Now consider a different configuration:

```
Refill rate = 100 tokens/second
Capacity = 10 tokens
```

The sustained rate remains the same, but the maximum immediate burst is now only 10 requests.

And:

```
Refill rate = 100 tokens/second
Capacity = 1 token
```

This allows very little bursting.

The rate is still 100 requests/second over time, but requests must wait for individual tokens to become available after each consumption.

Therefore, we should choose these values based on the downstream service's actual capacity and burst tolerance.

# **11. What should we store in Redis?**

Now let's build a distributed Token Bucket.

Imagine our application has multiple servers:

\#chatgpt-mermaid-\_r_21t\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_21t\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_21t\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_21t\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_21t\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_21t\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_21t\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_21t\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_21t\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_21t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_21t\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_21t\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_21t\_ p{margin:0;}#chatgpt-mermaid-\_r_21t\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_21t\_ .label text,#chatgpt-mermaid-\_r_21t\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .node rect,#chatgpt-mermaid-\_r_21t\_ .node circle,#chatgpt-mermaid-\_r_21t\_ .node ellipse,#chatgpt-mermaid-\_r_21t\_ .node polygon,#chatgpt-mermaid-\_r_21t\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .rough-node .label text,#chatgpt-mermaid-\_r_21t\_ .node .label text,#chatgpt-mermaid-\_r_21t\_ .image-shape .label,#chatgpt-mermaid-\_r_21t\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_21t\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .rough-node .label,#chatgpt-mermaid-\_r_21t\_ .node .label,#chatgpt-mermaid-\_r_21t\_ .image-shape .label,#chatgpt-mermaid-\_r_21t\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_21t\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_21t\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_21t\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_21t\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_21t\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_21t\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_21t\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_21t\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_21t\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_21t\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_21t\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_21t\_ .icon-shape,#chatgpt-mermaid-\_r_21t\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_21t\_ .icon-shape p,#chatgpt-mermaid-\_r_21t\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_21t\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_21t\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_21t\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_21t\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_21t\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_21t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_21t\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_21t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_21t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_21t\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_21t\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_21t\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_21t\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_21t\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_21t\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_21t\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .node rect,#chatgpt-mermaid-\_r_21t\_ .node circle,#chatgpt-mermaid-\_r_21t\_ .node ellipse,#chatgpt-mermaid-\_r_21t\_ .node polygon,#chatgpt-mermaid-\_r_21t\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_21t\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_21t\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_21t\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_21t\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_21t\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerAPI Server 1API Server 2API Server 3Shared Redis

Why do we need Redis?

Because if each application instance maintains its own token balance, each one would independently grant the configured capacity.

For example:

```
Configured capacity = 100 tokens

Server 1:
    100 tokens

Server 2:
    100 tokens

Server 3:
    100 tokens
```

Collectively, the client might get 300 tokens!

That violates the intended limit of 100 tokens for that client.

We need shared state.

## **11.1 Redis data model**

For each policy and identity, maintain:

```
rate_limit:{policy_id}:{identity}
```

For example:

```
rate_limit:products:user_123
```

The Redis Hash contains:

```
available_tokens = 6.5
last_refill_time = 1700000002000
```

The policy configuration contains:

```
capacity = 10
refill_rate = 2 tokens/second
```

Notice that we only maintain two state values.

We don't store:

- Every request.
- A counter for every second.
- A list of timestamps.
- A scheduled token-generation task.

We calculate the token balance lazily when a request arrives.

This is one of the most attractive properties of Token Bucket.

# **12. Implementing Token Bucket with Redis**

Let's go through the Redis execution flow.

Our requirements:

```
Policy = products

Identity = user_123

Capacity = 10 tokens

Refill rate = 2 tokens/second

Request cost = 1 token
```

When a request arrives:

1. Read the current token balance.
2. Read the last refill timestamp.
3. Calculate elapsed time.
4. Calculate newly generated tokens.
5. Cap the balance at the bucket capacity.
6. Check whether the request has sufficient tokens.
7. Deduct the request cost if allowed.
8. Save the updated state.

The important requirement is that all these steps must be atomic.

### **Why atomicity matters**

Imagine:

```
Available tokens = 1
Request cost = 1
```

Two requests arrive simultaneously.

Without atomicity:

```
Request A reads tokens = 1

Request B reads tokens = 1

Request A decides ALLOW

Request B decides ALLOW
```

Both requests consume the same token!

This is the same concurrency problem we encountered with Fixed Window and Sliding Window Log.

The solution is also familiar:

**Execute the complete validation and update as a single atomic operation.**

Redis Lua scripts are suitable here.

### **12.1 Redis Lua implementation**

Here is a complete illustrative implementation.

```
-- KEYS[1] = Rate limiter Redis key
--
-- ARGV[1] = Bucket capacity
-- ARGV[2] = Refill rate (tokens per second)
-- ARGV[3] = Request token cost

local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local requestCost = tonumber(ARGV[3])

-- Read authoritative Redis server time.
local time = redis.call('TIME')

local now =
    tonumber(time[1]) * 1000 +
    math.floor(tonumber(time[2]) / 1000)

-- Read stored state.
local tokens =
    tonumber(redis.call('HGET', key, 'tokens'))

local lastRefill =
    tonumber(redis.call('HGET', key, 'last_refill_ms'))

-- A new bucket starts full.
if tokens == nil or lastRefill == nil then

    tokens = capacity
    lastRefill = now

end

-- Calculate elapsed time.
local elapsedMs = math.max(0, now - lastRefill)

-- Replenish tokens.
local generatedTokens =
    elapsedMs * refillRate / 1000

tokens = math.min(
    capacity,
    tokens + generatedTokens
)

-- Decide whether to admit the request.
local allowed = 0
local retryAfterMs = 0

if tokens >= requestCost then

    tokens = tokens - requestCost

    allowed = 1

else

    local missingTokens = requestCost - tokens

    retryAfterMs = math.ceil(
        missingTokens / refillRate * 1000
    )

end

-- Save the updated state even when rejected.
redis.call(
    'HSET',
    key,
    'tokens', tostring(tokens),
    'last_refill_ms', tostring(now)
)

-- Retain the state long enough for an empty bucket
-- to refill completely, plus a cleanup allowance.
local ttlMs = math.ceil(
    capacity / refillRate * 1000
) + 1000

redis.call('PEXPIRE', key, ttlMs)

local remaining = math.floor(tokens)

return {
    allowed,
    remaining,
    retryAfterMs
}
```

The script returns:

```
{
    allowed,
    remainingTokens,
    retryAfterMs
}
```

For example:

```
{1, 6, 0}
```

Means:

```
Allowed = true
Remaining whole tokens = 6
```

And:

```
{0, 0, 500}
```

Means:

```
Allowed = false
Retry after = 500 milliseconds
```

A few important implementation details:

**1. Why Redis TIME?**

All application instances should calculate token replenishment using a consistent clock.

**2. Why preserve fractional tokens?**

Because token generation is continuous mathematically. Rounding prematurely could reduce the effective refill rate.

**3. Why save state when a request is rejected?**

Because tokens may have accumulated since the previous request. We must preserve the newly calculated balance and timestamp.

**4. Why use TTL?**

When no requests arrive, the bucket eventually becomes completely full.

After that, retaining its state is unnecessary. A new bucket can safely start full once enough time has passed to refill the previous one.

The TTL above is based on the time needed to fill an empty bucket, plus a small grace period.

**5. What should we validate before running the script?**

The configuration should ensure:

```
capacity > 0
refillRate > 0
requestCost > 0
requestCost <= capacity
```

If a request permanently costs more tokens than the bucket can ever hold, waiting will never make it eligible.

## **12.2 One important production improvement: Clock handling**

There is a small but important edge case in timestamp-based implementations.

Suppose the Redis clock moves backwards due to clock adjustment.

```
Previous timestamp = 2000 ms
Current timestamp  = 1900 ms
```

We must not interpret this as negative elapsed time and move our stored refill timestamp backwards.

A safer calculation is:

```
local effectiveNow = math.max(now, lastRefill)

local elapsedMs = effectiveNow - lastRefill
```

Then persist `effectiveNow` as the new timestamp.

This prevents a backwards clock adjustment from causing an artificially large refill later.

For a production implementation, we should also consider using integer-scaled token units if we need particularly precise accounting, rather than relying on floating-point representations.

# **13. What happens when a request is rejected?**

One very useful property of Token Bucket is that we can calculate exactly how long the client should wait before trying again.

Suppose:

```
Capacity = 10 tokens
Refill rate = 2 tokens/second

Available tokens = 0

Incoming request cost = 1
```

We need one token.

Since we generate two tokens per second:

\\[ WaitTime = \frac{1}{2} \\]

\\[ = 0.5 \text{ seconds} \\]

Therefore:

```
Retry after = 500 milliseconds
```

Now consider:

```
Available tokens = 0.2
Request cost = 1
Refill rate = 2 tokens/second
```

Missing tokens:

\\[ 1-0.2=0.8 \\]

Time required:

\\[ WaitTime = \frac{0.8}{2} \\]

\\[ = 0.4 \text{ seconds} \\]

So the client should wait 400 milliseconds.

### **General formula**

Let:

- \\(C\\) = Request cost.
- \\(T\\) = Current available tokens.
- \\(r\\) = Refill rate.

When the request is rejected:

\\[ \boxed{ RetryAfter = \frac{C-T}{r} } \\]

We can round this up to the next millisecond or second, depending on the response format.

For example, an HTTP response:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 1
Content-Type: application/json
```

```
{
  "error": "rate_limit_exceeded",
  "message": "Insufficient rate-limit tokens",
  "retryAfterSeconds": 1
}
```

A practical caution: `Retry-After` is calculated from the current token balance. By the time the client retries, another request might have consumed the newly generated token.

Clients should also use backoff and jitter rather than having thousands of rejected clients retry simultaneously.

# **14. An important extension: Different requests can consume different numbers of tokens**

So far, we have assumed:

```
One request = One token
```

But is every API request equally expensive?

Consider an application:

```
GET /products
GET /reports
POST /generate-large-export
POST /run-expensive-analytics
```

These operations could have very different resource requirements.

For example:

| Operation           | Token cost |
| ------------------- | ---------- |
| Get product         | 1          |
| Get order           | 1          |
| Generate report     | 5          |
| Large export        | 10         |
| Expensive analytics | 20         |

We can configure Token Bucket to charge a different number of tokens for each operation.

Suppose:

```
Capacity = 100 tokens
Refill rate = 10 tokens/second
```

A simple product request consumes:

```
1 token
```

An expensive analytics request consumes:

```
20 tokens
```

Both requests use the same bucket.

Why is this useful?

Because we're no longer limiting only request count.

We're approximating the consumption of a resource.

The resource might represent:

- CPU budget.
- Database query cost.
- API usage units.
- AI model tokens.
- Third-party API quota.
- Computational complexity.

For example, an AI application might use different token costs for different model operations, although actual provider billing and quotas may require additional accounting rules.

The general algorithm doesn't change.

Instead of:

```
requestCost = 1
```

We calculate:

```
requestCost = costForThisOperation
```

Then check:

\\[ AvailableTokens \geq RequestCost \\]

One restriction:

\\[ RequestCost \leq Capacity \\]

Otherwise, the request can never be admitted, even if the client waits indefinitely.

# **15. Can Token Bucket also queue requests?**

Yes.

This is an important distinction between the algorithm and the system built around it.

The basic Token Bucket algorithm is a **policer**:

```
Request arrives
      |
      v
Check tokens
      |
      +---- Enough tokens ----> ALLOW
      |
      +---- Not enough -------> REJECT
```

But we can also build a waiting mechanism around it.

For example:

```
Request arrives
      |
      v
Check tokens
      |
      +---- Enough tokens ----> Process immediately
      |
      +---- Not enough -------> Put into queue
                                      |
                                      v
                              Wait for tokens
                                      |
                                      v
                                Process request
```

This is useful for background jobs.

Imagine a worker receives 1,000 jobs, but only has enough tokens to dispatch 10 immediately.

It can wait for tokens to replenish and gradually process the remaining jobs.

So:

- Token Bucket determines when work has enough permission to proceed.
- The queue determines where pending work waits.
- The worker executes it when eligible.

This is a useful combination for systems integrating with third-party APIs.

But we shouldn't automatically queue everything.

For a synchronous public API, returning HTTP 429 might be much better than making the client wait indefinitely.

# **16. Token Bucket vs Leaky Bucket — Understand the difference completely**

These two algorithms are often confused because both involve a bucket, and both can control traffic.

Let's compare them using the same configuration.

Suppose:

```
Rate = 2 requests/second

Burst capacity = 10 requests
```

### **Leaky Bucket**

Our dispatcher sends requests at a steady rate.

```
Request 1 -> 0.0s
Request 2 -> 0.5s
Request 3 -> 1.0s
Request 4 -> 1.5s
Request 5 -> 2.0s
...
```

Even if 10 requests arrive simultaneously, the dispatcher gradually sends them.

```
Incoming burst:
R1 R2 R3 R4 R5 R6 R7 R8 R9 R10
                  |
                  v
                Queue
                  |
                  v
        R1  R2  R3  R4  R5 ...
        |   |   |   |   |
        0 .5  1  1.5  2 seconds
```

The objective is smooth output.

### **Token Bucket**

Now consider the same rate and capacity.

The bucket starts with 10 tokens.

All 10 requests can be allowed immediately.

```
Time 0:
    R1 R2 R3 R4 R5 R6 R7 R8 R9 R10

Time 0.5:
    R11

Time 1.0:
    R12

Time 1.5:
    R13
```

The objective is to allow bursts while controlling sustained consumption.

### **Side-by-side comparison**

| Property           | Leaky Bucket                                | Token Bucket                                         |
| ------------------ | ------------------------------------------- | ---------------------------------------------------- |
| Core idea          | Queue work and release at a controlled rate | Accumulate permission and spend it                   |
| Main configuration | Leak rate + queue capacity                  | Refill rate + token capacity                         |
| Incoming burst     | Buffered up to capacity                     | Can be admitted if tokens exist                      |
| Immediate burst    | Generally smoothed                          | Explicitly permitted                                 |
| Outgoing behavior  | Steady when continuously backlogged         | Can be bursty                                        |
| State              | Queue / virtual backlog                     | Token balance + last refill time                     |
| Waiting            | Common in queue-based shaping               | Optional                                             |
| Typical use        | Background job dispatch, traffic shaping    | API gateways, request admission, burst-tolerant APIs |

Here's the mental shortcut:

```
LEAKY BUCKET:

    "I will process at a controlled rate."


TOKEN BUCKET:

    "You may proceed when you have
     accumulated enough permission."
```

Neither is universally better.

The downstream system's requirements determine which one we should use.

# **17. Fixed Window vs Sliding Window vs Token Bucket**

Let's compare the three algorithm families we might use for an ordinary API gateway.

Imagine our requirement is:

```
Maximum average rate = 10 requests/second
Desired burst capacity = 30 requests
```

Which algorithm should we choose?

**Fixed Window**

```
10 requests per second in each fixed interval.
```

It has a boundary burst problem.

**Sliding Window Log**

```
10 requests in any rolling 1-second interval.
```

This is stricter than the requested average-plus-burst policy.

**Sliding Window Counter**

```
Approximate 10 requests in a rolling 1-second interval.
```

This is still a rolling quota approximation, not a configurable accumulated burst policy.

**Token Bucket**

```
Refill rate = 10 tokens/second
Capacity = 30 tokens
```

This matches the intended policy much more naturally.

The client can use up to 30 requests immediately when the bucket is full, then consume newly generated tokens over time.

This is why Token Bucket is particularly useful for APIs where occasional bursts are acceptable.

# **18. What are the advantages and disadvantages?**

Let's evaluate it properly.

### **Advantages**

**1. Allows controlled bursts**

This is its biggest advantage.

An idle client can accumulate tokens and use them later.

**2. Controls long-term consumption**

The refill rate determines the sustained allowance.

**3. Very little storage**

We only need:

```
available_tokens
last_refill_time
```

Per identity and policy.

**4. Efficient validation**

The algorithm requires simple arithmetic and a small number of atomic state operations.

**5. Supports different request costs**

We can charge more tokens for expensive operations.

**6. Works well for distributed APIs**

A centralized state store such as Redis can coordinate multiple application instances.

**7. No background refill process required**

Tokens are calculated when requests arrive.

### **Disadvantages**

**1. Does not enforce a strict rolling-window quota**

The algorithm intentionally allows bursts.

If the requirement says "never more than 100 requests in any 60-second interval," Token Bucket alone doesn't guarantee that.

**2. Does not automatically smooth traffic**

If the bucket contains 1,000 tokens, 1,000 requests might be allowed nearly simultaneously.

The downstream service must be able to tolerate the configured burst.

**3. Requires shared state for globally consistent limits**

Local in-memory buckets on multiple servers can multiply the effective allowance.

**4. Requires careful configuration**

If capacity is too large, a client may overwhelm a downstream service with an unexpected burst.

If capacity is too small, we might unnecessarily reject legitimate traffic.

**5. Does not control concurrency**

Allowing 100 requests per second doesn't mean we can safely have 10,000 long-running requests executing simultaneously.

A separate concurrency limit may be necessary.

**6. Redis becomes an important dependency**

When a shared Redis-based limiter is unavailable, we need a deliberate fail-open or fail-closed strategy.

We'll explore this in the production architecture chapter.

# **19. When should we use Token Bucket?**

Let's map it to real-world systems.

| Scenario                                | Suitability                   | Reason                                                      |
| --------------------------------------- | ----------------------------- | ----------------------------------------------------------- |
| Public REST API                         | Excellent                     | Allows normal bursts while controlling sustained usage.     |
| API Gateway                             | Excellent                     | Efficient per-user or per-key rate limiting.                |
| SaaS tenant limits                      | Excellent                     | Different capacities and refill rates for different plans.  |
| Third-party API integration             | Excellent, if semantics match | Supports burst allowances and sustained quotas.             |
| Microservice protection                 | Excellent                     | Controls request admission to downstream services.          |
| AI inference API                        | Good                          | Request costs can represent different computation costs.    |
| Strict rolling-window contractual quota | Not sufficient alone          | Can permit more than the rolling-window threshold.          |
| Smooth background job execution         | Not sufficient alone          | It permits bursts rather than forcing evenly spaced output. |

### **Example: A SaaS application**

Suppose we have three subscription plans.

```
Free:
    Capacity = 10
    Refill = 1 token/second

Pro:
    Capacity = 100
    Refill = 10 tokens/second

Enterprise:
    Capacity = 500
    Refill = 50 tokens/second
```

Each authenticated request consumes one token.

Now consider a user from the Pro plan.

Their bucket starts with 100 tokens.

They can make 100 requests immediately.

After exhausting the bucket, they receive 10 tokens every second.

The exact same algorithm supports all three plans.

Only the policy configuration changes.

That's a clean and flexible design.

# **20. One important production concern: Multiple simultaneous limits**

So far, we have considered one bucket per identity.

But real systems often need several limits simultaneously.

Imagine an API request from a user.

We might have:

```
Limit 1:
    100 requests/second per user.

Limit 2:
    1,000 requests/second per tenant.

Limit 3:
    10,000 requests/second globally.
```

One request must satisfy all three.

Conceptually:

\#chatgpt-mermaid-\_r_280\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_280\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_280\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_280\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_280\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_280\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_280\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_280\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_280\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_280\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_280\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_280\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_280\_ p{margin:0;}#chatgpt-mermaid-\_r_280\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_280\_ .label text,#chatgpt-mermaid-\_r_280\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .node rect,#chatgpt-mermaid-\_r_280\_ .node circle,#chatgpt-mermaid-\_r_280\_ .node ellipse,#chatgpt-mermaid-\_r_280\_ .node polygon,#chatgpt-mermaid-\_r_280\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .rough-node .label text,#chatgpt-mermaid-\_r_280\_ .node .label text,#chatgpt-mermaid-\_r_280\_ .image-shape .label,#chatgpt-mermaid-\_r_280\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_280\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .rough-node .label,#chatgpt-mermaid-\_r_280\_ .node .label,#chatgpt-mermaid-\_r_280\_ .image-shape .label,#chatgpt-mermaid-\_r_280\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_280\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_280\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_280\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_280\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_280\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_280\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_280\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_280\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_280\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_280\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_280\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_280\_ .icon-shape,#chatgpt-mermaid-\_r_280\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_280\_ .icon-shape p,#chatgpt-mermaid-\_r_280\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_280\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_280\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_280\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_280\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_280\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_280\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_280\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_280\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_280\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_280\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_280\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_280\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_280\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_280\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_280\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_280\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_280\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .node rect,#chatgpt-mermaid-\_r_280\_ .node circle,#chatgpt-mermaid-\_r_280\_ .node ellipse,#chatgpt-mermaid-\_r_280\_ .node polygon,#chatgpt-mermaid-\_r_280\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_280\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_280\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_280\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_280\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_280\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestUser BucketEnough tokens?RejectTenant BucketEnough tokens?Global BucketEnough tokens?Deduct Tokens and AllowNoYesNoYesNoYes

But there is an important implementation issue.

Suppose the user bucket has enough tokens, but the tenant bucket doesn't.

We must not consume a user token and then reject the request because of the tenant limit.

Otherwise, users can lose quota because of a limit they don't control.

We need to ensure that all relevant buckets are checked and deducted together.

For Redis, one approach is a single Lua script operating on all relevant keys, provided the keys are colocated in the same Redis Cluster hash slot.

Otherwise, coordinating them across different cluster slots requires a more complicated design.

We'll examine this properly in the production-design chapter.

For now, remember:

**A real rate limiter often evaluates multiple independent policies for the same request.**

# **21. A few important production considerations**

Before concluding, let's clarify some decisions that affect correctness.

### **A. Should a new bucket start full or empty?**

We have been initializing it with:

```
available_tokens = capacity
```

This allows the client to consume the entire burst allowance immediately.

But sometimes we might prefer:

```
available_tokens = 0
```

This forces the client to wait for tokens to accumulate.

For example:

| Initialization | Behavior                                              |
| -------------- | ----------------------------------------------------- |
| Full           | Allows an immediate burst after creation              |
| Empty          | Starts conservatively and gradually becomes available |
| Partially full | Allows a controlled initial burst                     |

Full is a common choice for ordinary API rate limiting, but the right choice depends on the protection requirements.

Also, be careful when Redis loses state or evicts keys. Recreating buckets as full can grant unexpected bursts. Production systems must protect important limiter state from premature eviction and consider restart and recovery behavior.

### **B. What happens when Redis is unavailable?**

Suppose Redis is down.

Our application cannot check the shared token balance.

We have two possible policies.

**Fail open:**

```
Redis unavailable:
    Allow requests.
```

Advantages:

\- Application availability is preserved.

Disadvantages:

\- Downstream services might get overwhelmed.

**Fail closed:**

```
Redis unavailable:
    Reject requests.
```

Advantages:

\- Protects the downstream service.

Disadvantages:

\- Our API might reject legitimate users even when the backend is healthy.

We choose based on what we're protecting.

For example, an internal analytics API might tolerate fail-open behavior, while a system protecting a fragile third-party dependency might prefer fail-closed behavior.

We can also design a bounded local fallback, but it must not accidentally multiply the global allowance across application instances.

### **C. Does Token Bucket solve concurrency?**

No.

Imagine:

```
Rate = 100 requests/second

Concurrency limit = 10 active requests
```

Both policies can be applied together.

The Token Bucket decides whether the request has sufficient rate-limit tokens.

A separate concurrency limiter decides whether the system has capacity to execute another operation.

This matters for slow database queries and long-running external API calls.

### **D. Does Token Bucket need a scheduled refill job?**

No.

This is worth remembering.

We don't need:

```
Every 500ms:
    Generate 1 token.
```

Instead:

```
When a request arrives:

    Calculate elapsed time.

    Calculate how many tokens should
    have accumulated.

    Update the token balance.
```

The bucket is replenished mathematically, when needed.

This makes it efficient and removes the need for a background token-generation process.

# **22. Final comparison: All five algorithms**

We have now completed all five core algorithms.

Let's put them together.

| Algorithm              | What it remembers                  | Main purpose                                 | Major limitation                                         |
| ---------------------- | ---------------------------------- | -------------------------------------------- | -------------------------------------------------------- |
| Fixed Window           | One counter per fixed interval     | Simple quotas                                | Boundary bursts                                          |
| Sliding Window Log     | Individual request timestamps      | Exact rolling-window limits                  | Memory and write overhead                                |
| Sliding Window Counter | Previous and current counters      | Efficient approximate rolling limits         | Estimation errors                                        |
| Leaky Bucket           | Pending queue or virtual backlog   | Smooth outgoing traffic                      | Added latency and queue management                       |
| Token Bucket           | Token balance and refill timestamp | Control sustained rate while allowing bursts | Does not smooth traffic or enforce strict rolling quotas |

## **The most important thing to understand**

These are not five interchangeable implementations of the same requirement.

They solve different variations of the problem.

```
                    RATE LIMITING
                         |
          +--------------+--------------+
          |                             |
          v                             v
   CONTROL ADMISSION              CONTROL OUTPUT
          |                             |
          |                             |
    +-----+------+                 Leaky Bucket
    |            |                 (Traffic shaping)
    |            |
    v            v
 Fixed Window  Sliding Window
    |              |
    |              +---- Log
    |              |
    |              +---- Counter
    |
    +--------------------------+
                               |
                               v
                         Token Bucket
                    (Burst-tolerant admission)
```

Token Bucket can also be used to control when queued work is released, but its defining behavior is accumulated permission and burst tolerance.

# **23. Your mental models — What you should be able to derive now**

Imagine somebody gives you a new system design requirement.

You should be able to reason about it naturally.

**Problem 1:**

```
A user should make at most 1,000 API calls per day.
```

Think:

> I need a simple daily counter.

Fixed Window is a reasonable choice.

**Problem 2:**

```
A user should never make more than
100 requests in any rolling 60-second period.
```

Think:

> I need to consider actual recent requests, not predefined boundaries.

Sliding Window Log provides exact rolling-window tracking. Sliding Window Counter is an efficient approximation when that is acceptable.

**Problem 3:**

```
We receive 10,000 jobs, but should process
only 50 jobs per second.
```

Think:

> I need to buffer incoming work and control when it leaves.

Leaky Bucket is a natural fit.

**Problem 4:**

```
An API allows 10 requests per second on average,
but allows bursts of up to 100 requests.
```

Think:

> I need to accumulate permission when idle and spend it when requests arrive.

Token Bucket is a natural fit.

**Problem 5:**

```
We have a public API with expensive endpoints.
```

Think:

> Requests should have different costs.

Token Bucket with weighted request costs is useful.

These decisions should come from the requirement, not from memorizing algorithm names.

## **Chapter 5 — Final mental model**

Token Bucket maintains a balance of available tokens.

Tokens accumulate over time at a configured refill rate, but the balance cannot exceed the bucket capacity.

Each incoming request consumes tokens.

If sufficient tokens exist, the request is allowed. Otherwise, it must be rejected or wait until enough tokens accumulate.

The algorithm's two most important parameters are:

```
Capacity:
    Maximum burst allowance.

Refill rate:
    Sustained consumption allowance.
```

The fundamental formulas are:

**Token replenishment:**

\\[ T\_{available} = \min(B,\ T\_{previous}+r \times \Delta t) \\]

**Admission:**

\\[ T\_{available} \geq RequestCost \\]

**Updated balance:**

\\[ T\_{new}=T\_{available}-RequestCost \\]

This is the entire Token Bucket algorithm.

# **Next Chapter — Chapter 6: Choosing the Right Algorithm**

We've learned the individual algorithms. Now we need to develop the ability to choose between them.

We'll take real-world requirements and reason through:

- API Gateway rate limiting.
- Per-user and per-tenant quotas.
- Login and authentication protection.
- Third-party API integrations.
- Web crawlers.
- Background job processing.
- AI/LLM API usage.
- Database protection.
- Global service protection.

We'll also explore combinations of algorithms, because real production systems often use more than one limiter.

**After that, Chapter 7 will focus on building a truly distributed Rate Limiter using Redis, multiple application instances, atomicity, Redis Cluster, multi-region considerations, and failure handling.**
