# **Chapter 3 — Sliding Window Counter**

In the previous chapter, we discovered that Sliding Window Log solves the Fixed Window boundary problem by remembering the timestamp of every request.

But we also discovered a major drawback:

**Remembering every request is expensive.**

Imagine we have 1 million active users, each making 100 requests per minute.

We could end up storing 100 million individual timestamps.

That creates a significant memory and Redis operation overhead.

So let's ask a natural question:

> Can we get most of the benefits of Sliding Window Log without storing every individual request?

That's exactly what Sliding Window Counter tries to achieve.

We'll derive it from the previous algorithms.

# **1. Start with what we already know**

Let's establish our requirement:

```
Maximum 10 requests in any 10-second period.
```

We have two algorithms so far.

**Fixed Window**

We store one counter per fixed interval.

```
Window 1                 Window 2
|------------------------|------------------------|
0                       10                       20

Counter = 8              Counter = 3
```

Problem: it doesn't account for requests from the previous window when the new window starts.

**Sliding Window Log**

We store the timestamps of individual requests.

```
user_123 = [
    2,
    3,
    5,
    8,
    11,
    13
]
```

Problem: storing every timestamp can become expensive.

What if we combine the two ideas?

Instead of storing every timestamp, what if we store counters for the previous and current fixed windows?

Something like:

```
Previous window:
    requestCount = 8

Current window:
    requestCount = 3
```

We already know how to maintain these counters using Fixed Window.

But how do we use them to approximate the actual sliding-window count?

That's the interesting part.

# **2. The central idea: Give different weights to different windows**

Let's understand this visually.

Our window duration is 10 seconds.

Imagine the fixed windows are:

```
Previous Window             Current Window
0 ------------------------ 10 ------------------------ 20
```

A new request arrives at time 13.

What is the actual sliding window for this request?

It is:

```
3 --------------------------------------------- 13
                   10 seconds
```

Look carefully.

Our sliding window overlaps two fixed windows.

```
Previous Window             Current Window
0 ------------------------ 10 ------------------------ 20
          |------------------------------|
          3                             13
                 Sliding Window
```

It contains:

- Only the last 7 seconds of the previous fixed window.
- The first 3 seconds of the current fixed window.

This observation is the foundation of Sliding Window Counter.

**We should not treat all requests from the previous fixed window as equally recent. Only some portion of that window overlaps with our current sliding window.**

But remember, we don't store the individual timestamps.

We only know the total count in the previous window.

So how can we estimate the number of requests that are still relevant?

We use a weighting factor.

## **2.1 Calculate the overlap**

Assume:

```
Window duration = 10 seconds
Current time = 13 seconds
Current fixed window starts = 10 seconds
```

How much time has elapsed in the current fixed window?

\\[ elapsed = 13 - 10 = 3 \\]

How much of the previous fixed window overlaps with the sliding window?

\\[ overlap = 10 - 3 = 7 \\]

Therefore:

\\[ overlapFraction = \frac{7}{10} = 0.7 \\]

This means approximately 70% of the previous window's duration is still relevant.

Now imagine the previous window had 10 requests.

We don't know when those 10 requests actually happened.

But suppose they were distributed uniformly across the previous window.

Then approximately 70% of them should fall within the overlapping 70% of that window.

Therefore:

\\[ previousWindowContribution = 10 \times 0.7 \\]

\\[ = 7 \\]

We estimate that approximately 7 requests from the previous window are still inside our sliding window.

That's the central trick!

# **3. Deriving the actual formula**

Let's generalize what we just did.

We have:

- \\(W\\): Fixed window duration.
- \\(t\\): Current time.
- \\(elapsed\\): Time elapsed since the current fixed window started.
- \\(P\\): Number of requests in the previous fixed window.
- \\(C\\): Number of requests in the current fixed window.

The previous window's remaining overlap fraction is:

\\[ Weight = \frac{W-elapsed}{W} \\]

The estimated contribution from the previous window is:

\\[ PreviousContribution = P \times Weight \\]

Since every request in the current fixed window occurred after the current window started, all those requests are inside the current rolling window.

Therefore:

\\[ \boxed{ EstimatedCount = C + P \times \frac{W-elapsed}{W} } \\]

This is the Sliding Window Counter formula.

Let's understand every term:

| Term                | Meaning                                                |
| ------------------- | ------------------------------------------------------ |
| \\(C\\)             | All requests in the current fixed window               |
| \\(P\\)             | All requests in the previous fixed window              |
| \\(W\\)             | Fixed window duration                                  |
| \\(elapsed\\)       | Time elapsed in the current fixed window               |
| \\((W-elapsed)/W\\) | Estimated fraction of previous requests still relevant |

The algorithm uses this estimated count to make the rate-limit decision.

# **4. Walk through a complete example**

Let's use a real example with numbers.

Our policy:

```
Maximum requests = 10
Window duration = 10 seconds
```

Suppose the request history is:

```
Previous window: 0 - 10 seconds
    Requests = 8

Current window: 10 - 20 seconds
    Requests = 2
```

A new request arrives at time 13.

Let's calculate.

**Step 1: Determine the elapsed time.**

```
Current time = 13
Current window started = 10

Elapsed = 13 - 10 = 3 seconds
```

**Step 2: Calculate the previous-window weight.**

\\[ Weight = \frac{10-3}{10} \\]

\\[ = 0.7 \\]

**Step 3: Calculate the previous-window contribution.**

```
Previous window count = 8
Weight = 0.7
```

\\[ PreviousContribution = 8 \times 0.7 \\]

\\[ = 5.6 \\]

**Step 4: Add the current-window count.**

```
Current window count = 2
Previous contribution = 5.6
```

\\[ EstimatedCount = 2 + 5.6 \\]

\\[ = 7.6 \\]

**Step 5: Decide whether to admit the new request.**

Our limit is 10.

The estimated usage from previously admitted requests is 7.6.

The proposed request would bring the estimate to:

\\[ 7.6 + 1 = 8.6 \\]

Since:

\\[ 8.6 \leq 10 \\]

We allow the request.

We then increment the current window's counter:

```
Previous window count = 8
Current window count = 3
```

We don't modify the previous window.

We only record newly admitted requests in the current window.

# **5. Let's understand how the weight changes with time**

This is probably the most important intuition to develop.

Imagine our fixed window duration is 10 seconds.

The current fixed window starts at time 10.

As time moves forward, the previous window becomes progressively less relevant.

Let's calculate the weights.

| Current time | Elapsed in current window | Previous-window weight |
| ------------ | ------------------------- | ---------------------- |
| 10           | 0 seconds                 | 100%                   |
| 11           | 1 second                  | 90%                    |
| 12           | 2 seconds                 | 80%                    |
| 13           | 3 seconds                 | 70%                    |
| 14           | 4 seconds                 | 60%                    |
| 15           | 5 seconds                 | 50%                    |
| 16           | 6 seconds                 | 40%                    |
| 17           | 7 seconds                 | 30%                    |
| 18           | 8 seconds                 | 20%                    |
| 19           | 9 seconds                 | 10%                    |
| 20           | 10 seconds                | 0%                     |

Visualize this:

```
Time:                 10       11       12       13       ...      20

Previous weight:     100%     90%      80%      70%       ...       0%

Current weight:        0%      10%      20%      30%       ...     100%
```

The previous window's contribution decreases as time advances.

At time 10, we consider the entire previous window relevant.

At time 15, we consider approximately half of it relevant.

At time 19, we consider approximately 10% relevant.

At time 20, the previous window contributes nothing.

Why?

Because at time 20, the sliding window is:

```
10 ----------------------------------------- 20
```

The previous fixed window is now completely outside the sliding interval.

A new fixed window starts.

This is why we only need the current and previous counters.

We don't need information about windows from two, three, or ten intervals ago.

They are already outside the relevant rolling window.

# **6. What exactly do we store?**

Now that we understand the mathematics, let's design the storage.

Remember that Sliding Window Log stored:

```
user_123 = [
    timestamp_1,
    timestamp_2,
    timestamp_3,
    ...
]
```

Sliding Window Counter stores only two counters.

For each identity and policy:

```
Previous window count
Current window count
Current window ID
```

For example:

```
Identity: user_123

Policy:
    limit = 10
    window = 10 seconds

State:
    window_id = 1
    previous_count = 8
    current_count = 2
```

We need `window_id` to know which fixed interval our counters represent.

Without it, we cannot reliably distinguish a current counter from an outdated one.

## **6.1 A suitable storage structure**

We can represent this using a Redis Hash.

For example:

```
Key:
rate_limit:products:user_123

Hash fields:

window_id       = 1
previous_count  = 8
current_count   = 2
```

The key identifies the combination of:

- Rate-limit policy.
- Identity.

An alternative is to use two Redis keys, one for each window:

```
rate_limit:products:user_123:window_0 = 8

rate_limit:products:user_123:window_1 = 2
```

Both approaches are valid.

I generally like the single-hash representation when implementing this particular algorithm because both counters and the current window ID can be updated together atomically.

We'll use that representation for the implementation.

# **7. How do we calculate the current window ID?**

We already used this concept in Fixed Window.

The formula is:

\\[ windowId = \left\lfloor \frac{now}{W} \right\rfloor \\]

Where:

- `now` is the current Unix timestamp.
- `W` is the window duration.

For example:

```
Window duration = 10 seconds
```

| Timestamp | Window ID |
| --------- | --------- |
| 0–9       | 0         |
| 10–19     | 1         |
| 20–29     | 2         |
| 30–39     | 3         |

We can also calculate the elapsed time:

\\[ elapsed = now \bmod W \\]

For example:

```
now = 13

W = 10

windowId = floor(13 / 10)
         = 1

elapsed = 13 % 10
        = 3 seconds
```

Notice that these two calculations tell us:

- Which fixed window the request belongs to.
- How far we are into that window.

We need both for Sliding Window Counter.

In a real distributed system, use an authoritative shared clock for these calculations, rather than relying on application servers whose clocks might differ.

# **8. What happens when a new fixed window starts?**

This deserves special attention.

Suppose our stored state is:

```
window_id = 1
previous_count = 5
current_count = 8
```

This means:

```
Window 0: 5 requests
Window 1: 8 requests
```

Now time reaches 20 seconds.

A request arrives in window 2.

We need to transition our state.

Our new state should become:

```
Window 1: 8 requests  -> Previous
Window 2: 0 requests  -> Current
```

In other words:

```
previous_count = old current_count
current_count = 0
window_id = new window ID
```

The old previous count is discarded.

Let's see the state transition:

```
BEFORE:

Window 0          Window 1          Window 2
    5                 8                 0
 Previous           Current

                       |
                       | Time reaches 20
                       v

AFTER:

Window 0          Window 1          Window 2
    X                 8                 0
 Discarded          Previous          Current
```

What if the user was inactive for multiple windows?

For example:

```
Stored window ID = 1
Current window ID = 5
```

The user skipped windows 2, 3, and 4.

We must not carry the old window 1 count into window 5.

By now, those requests are far outside the relevant sliding interval.

Therefore:

```
previous_count = 0
current_count = 0
window_id = 5
```

The general transition logic is:

```
if storedWindowId == currentWindowId:

    // Still in the same window.
    // No transition needed.

else if storedWindowId == currentWindowId - 1:

    // Moved forward by exactly one window.
    previousCount = currentCount
    currentCount = 0

    windowId = currentWindowId

else:

    // Skipped one or more complete windows.
    previousCount = 0
    currentCount = 0

    windowId = currentWindowId
```

This is an important piece of the implementation.

# **9. Complete algorithm — Putting everything together**

Let's now combine all the pieces.

Our policy:

```
Limit = 10 requests
Window = 10 seconds
```

Whenever a request arrives:

**Step 1: Identify the identity and policy.**

```
identity = user_123
policy = products
```

**Step 2: Get the current time.**

```
now = 13
```

**Step 3: Determine the current window.**

```
windowId = floor(13 / 10)
         = 1

elapsed = 13 % 10
        = 3
```

**Step 4: Load the stored counters.**

```
previous_count = 8
current_count = 2
stored_window_id = 1
```

**Step 5: Handle a window transition if necessary.**

Here the stored and current window IDs are both 1.

Nothing needs to change.

**Step 6: Calculate the estimated request count.**

\\[ EstimatedCount = C + P \times \frac{W-elapsed}{W} \\]

\\[ = 2 + 8 \times \frac{10-3}{10} \\]

\\[ = 2 + 5.6 \\]

\\[ = 7.6 \\]

**Step 7: Account for the new request.**

\\[ ProjectedCount = EstimatedCount + 1 \\]

\\[ = 8.6 \\]

**Step 8: Make the decision.**

```
if projectedCount <= limit:
    currentCount += 1
    ALLOW

else:
    REJECT
```

Since 8.6 is less than 10:

```
current_count = 3

Decision = ALLOW
```

The complete pseudocode:

```
function allowRequest(identity, policy):

    now = getCurrentTime()

    W = policy.windowDuration

    currentWindowId = floor(now / W)

    elapsed = now % W

    state = getState(identity, policy)

    if state does not exist:

        previousCount = 0
        currentCount = 0

    else if state.windowId == currentWindowId:

        previousCount = state.previousCount
        currentCount = state.currentCount

    else if state.windowId == currentWindowId - 1:

        previousCount = state.currentCount
        currentCount = 0

    else:

        previousCount = 0
        currentCount = 0

    previousWeight = (W - elapsed) / W

    estimatedCount =
        currentCount + previousCount * previousWeight

    projectedCount = estimatedCount + 1

    if projectedCount <= policy.limit:

        currentCount += 1

        saveState(
            currentWindowId,
            previousCount,
            currentCount
        )

        return ALLOW

    saveState(
        currentWindowId,
        previousCount,
        currentCount
    )

    return REJECT
```

Notice that we save the state even when rejecting, because a new window may have started and the old counters need to be rotated.

# **10. Implementing it with Redis**

Let's translate our algorithm into a practical distributed implementation.

Our application might have several instances:

\#chatgpt-mermaid-\_r_19c\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_19c\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_19c\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_19c\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_19c\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_19c\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_19c\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_19c\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_19c\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_19c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_19c\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_19c\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_19c\_ p{margin:0;}#chatgpt-mermaid-\_r_19c\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_19c\_ .label text,#chatgpt-mermaid-\_r_19c\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .node rect,#chatgpt-mermaid-\_r_19c\_ .node circle,#chatgpt-mermaid-\_r_19c\_ .node ellipse,#chatgpt-mermaid-\_r_19c\_ .node polygon,#chatgpt-mermaid-\_r_19c\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .rough-node .label text,#chatgpt-mermaid-\_r_19c\_ .node .label text,#chatgpt-mermaid-\_r_19c\_ .image-shape .label,#chatgpt-mermaid-\_r_19c\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_19c\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .rough-node .label,#chatgpt-mermaid-\_r_19c\_ .node .label,#chatgpt-mermaid-\_r_19c\_ .image-shape .label,#chatgpt-mermaid-\_r_19c\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_19c\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_19c\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_19c\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_19c\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_19c\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_19c\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_19c\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_19c\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_19c\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_19c\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_19c\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_19c\_ .icon-shape,#chatgpt-mermaid-\_r_19c\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_19c\_ .icon-shape p,#chatgpt-mermaid-\_r_19c\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_19c\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_19c\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_19c\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_19c\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_19c\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_19c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_19c\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_19c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_19c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_19c\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_19c\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_19c\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_19c\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_19c\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_19c\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_19c\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .node rect,#chatgpt-mermaid-\_r_19c\_ .node circle,#chatgpt-mermaid-\_r_19c\_ .node ellipse,#chatgpt-mermaid-\_r_19c\_ .node polygon,#chatgpt-mermaid-\_r_19c\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_19c\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_19c\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_19c\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_19c\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_19c\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientsLoad BalancerApplication Server 1Application Server 2Application Server 3Shared Redis

All instances use the same rate-limit state in Redis.

This means a user's requests are counted together, even if they reach different application servers.

## **10.1 Redis storage structure**

We use a Redis Hash:

```
Key:
rate_limit:{products:user_123}

Fields:
    window_id
    previous_count
    current_count
```

The `{...}` portion can be used as a Redis Cluster hash tag, helping ensure that the key is assigned to a consistent cluster slot.

We only need one Redis key for a particular user and policy.

## **10.2 Atomic validation is essential**

Remember the concurrency problem from our earlier chapters?

We cannot implement this as separate Redis commands:

```
HGET previous_count
HGET current_count
Calculate estimated count
HSET current_count
```

Imagine two requests arrive simultaneously.

Both might read:

```
previous_count = 8
current_count = 2
```

Both calculate the same estimated count.

Both decide to allow.

Both increment the counter.

This can cause us to exceed the intended admission threshold.

We need to perform the complete operation atomically.

Redis Lua scripts are a good solution.

## **10.3 Redis Lua implementation**

Here is a representative implementation.

The script performs the entire decision against one Redis Hash.

```
-- KEYS[1] = Rate limiter Redis key
--
-- ARGV[1] = Window duration (milliseconds)
-- ARGV[2] = Maximum allowed requests

local key = KEYS[1]

local windowMs = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

-- Use Redis server time.
local time = redis.call('TIME')

local now =
    tonumber(time[1]) * 1000 +
    math.floor(tonumber(time[2]) / 1000)

local currentWindowId = math.floor(now / windowMs)
local elapsed = now % windowMs

-- Read previous state.
local storedWindowId =
    tonumber(redis.call('HGET', key, 'window_id'))

local previousCount =
    tonumber(redis.call('HGET', key, 'previous_count')) or 0

local currentCount =
    tonumber(redis.call('HGET', key, 'current_count')) or 0

-- Handle initial state and window transitions.
if storedWindowId == nil then

    previousCount = 0
    currentCount = 0

elseif storedWindowId == currentWindowId then

    -- Same window. Keep existing counters.

elseif storedWindowId == currentWindowId - 1 then

    -- Previous window becomes the new previous window.
    previousCount = currentCount
    currentCount = 0

else

    -- More than one window was skipped.
    previousCount = 0
    currentCount = 0

end

-- Calculate the estimated count using integer-scaled
-- arithmetic to avoid floating-point comparisons.

local estimatedScaled =
    currentCount * windowMs +
    previousCount * (windowMs - elapsed)

-- Include the incoming request.
local projectedScaled =
    estimatedScaled + windowMs

local allowed = 0

if projectedScaled <= limit * windowMs then

    currentCount = currentCount + 1

    allowed = 1

end

-- Persist the normalized state.
redis.call(
    'HSET',
    key,
    'window_id', currentWindowId,
    'previous_count', previousCount,
    'current_count', currentCount
)

-- Keep the state only while it can be relevant.
redis.call('PEXPIRE', key, 2 * windowMs)

local remaining = 0

if allowed == 1 then

    local usedScaled =
        estimatedScaled + windowMs

    remaining = math.max(
        0,
        limit - math.ceil(usedScaled / windowMs)
    )

end

return {
    allowed,
    remaining
}
```

A few important observations:

**1. Why use integer-scaled arithmetic?**

Instead of directly comparing fractional values, we calculate:

\\[ EstimatedScaled = C \times W + P \times (W-elapsed) \\]

Then we check:

\\[ EstimatedScaled + W \leq Limit \times W \\]

This is mathematically equivalent to checking whether the estimated usage, including the proposed request, is within the limit.

It also avoids floating-point comparisons.

**2. Why store just one Redis Hash?**

We can update the window ID and both counters together.

We avoid coordinating separate keys.

**3. Why expire after twice the window duration?**

The current bucket becomes the previous bucket in the next interval.

It must remain available throughout that next interval.

A TTL of approximately two window durations from initial creation is a safe, simple retention strategy. It may retain some state slightly longer than strictly necessary, which is acceptable.

**4. Why is the script atomic?**

Redis executes the script without interleaving other commands.

Two concurrent requests cannot both read the old counter state and independently admit themselves based on that same state.

One will execute after the other.

One important implementation consideration: this script assumes the window duration and limit are valid positive integers, and it uses a single Redis key. Production code should also validate configuration, handle datastore failures, and define whether failures should fail open or fail closed.

# **11. The biggest limitation of Sliding Window Counter**

So far, this algorithm looks excellent.

We use just two counters, and we solve much of the boundary problem.

But there is a fundamental assumption.

Let's revisit the formula:

\\[ EstimatedCount = C + P \times \frac{W-elapsed}{W} \\]

Why did we multiply the previous count by the overlap fraction?

We assumed that requests were distributed approximately uniformly throughout the previous window.

But do we actually know that?

No!

We only know the total count.

We don't know when those requests happened.

**That's the fundamental accuracy limitation of Sliding Window Counter.**

Let's understand it with two opposite examples.

## **11.1 Scenario A: We overestimate the actual number of requests**

Our requirement:

```
10 requests in any 10 seconds.
```

Suppose the previous window contains 10 requests.

But all 10 requests happened very early:

```
Previous window:

0      2             5             10
|------XXXXXXXX------|--------------|
       10 requests
```

Current time is 13.

Our actual sliding window is:

```
3 -------------------------------- 13
```

All previous requests occurred before time 3.

Therefore, none of those 10 requests actually belongs to the current sliding window.

The true previous-window contribution is:

```
0 requests
```

But Sliding Window Counter calculates:

```
Previous count = 10

Elapsed = 3

Weight = 0.7

Estimated contribution = 10 * 0.7
                       = 7
```

It estimates 7 requests, even though the actual contribution is zero.

Suppose the current window has 2 requests.

```
Estimated count = 2 + 7
                = 9
```

The algorithm might reject an additional request because the projected estimate would be 10, or more depending on the admission threshold.

But the actual rolling-window count is only 2.

We unnecessarily restrict the user.

This is called overestimation.

## **11.2 Scenario B: We underestimate the actual number of requests**

Now let's reverse the traffic distribution.

Previous window still contains 10 requests.

But this time, all 10 requests happened near the end:

```
Previous window:

0              5          8        10
|--------------|----------XXXXXXXX--|
                       10 requests
```

Current time is 13.

Our sliding window is:

```
3 --------------------------------- 13
```

All 10 requests are still within this sliding window.

The actual previous-window contribution is:

```
10 requests
```

But Sliding Window Counter calculates:

```
Previous count = 10

Elapsed = 3

Weight = 0.7

Estimated contribution = 7
```

It underestimates by 3.

Suppose the current window has no requests and our limit is 10.

```
Estimated count = 7
```

The incoming request would bring the estimated count to 8.

So the algorithm allows it.

But the actual rolling-window count would now be:

```
10 previous requests + 1 new request
= 11 requests
```

We have exceeded the strict limit!

This is the critical distinction:

**Sliding Window Counter is an approximation. It does not guarantee strict rolling-window enforcement.**

It works best when request traffic is reasonably distributed across fixed windows.

A highly bursty workload can produce significant overestimation or underestimation.

# **12. Sliding Window Counter vs Sliding Window Log**

Let's compare their fundamental differences.

| Property                | Sliding Window Log         | Sliding Window Counter                     |
| ----------------------- | -------------------------- | ------------------------------------------ |
| Data stored             | Individual timestamps      | Two counters                               |
| Accuracy                | Exact rolling-window count | Approximate rolling-window count           |
| Memory                  | High                       | Very low                                   |
| Write operations        | Insert timestamp           | Increment counter                          |
| Cleanup                 | Remove expired timestamps  | Rotate previous/current counters           |
| Complexity              | Higher                     | Moderate                                   |
| Boundary burst problem  | Solved                     | Greatly reduced, but approximation can err |
| Useful for high traffic | Can be expensive           | Generally more efficient                   |

Consider 1 million active identities.

If every identity has a rolling window containing 100 requests:

**Sliding Window Log**

Potentially 100 million timestamp entries.

**Sliding Window Counter**

Approximately two counters and a window identifier per active identity and policy.

That's a substantial reduction in state.

# **13. When should we use Sliding Window Counter?**

This is where the tradeoffs become useful.

| Scenario                                   | Suitability    | Reason                                                               |
| ------------------------------------------ | -------------- | -------------------------------------------------------------------- |
| General public API rate limiting           | Very good      | Efficient approximate rolling-window enforcement.                    |
| Large SaaS applications                    | Very good      | Much lower storage than individual timestamp logs.                   |
| Per-user request limits                    | Very good      | Simple shared counter state.                                         |
| High-volume API gateway                    | Very good      | Efficient counter updates in Redis.                                  |
| Strict contractual rolling quotas          | Depends        | Approximation may exceed or unnecessarily restrict the actual quota. |
| Financial or security-critical enforcement | Use caution    | Incorrect admission decisions may have serious consequences.         |
| Smooth background job processing           | Not sufficient | It doesn't control when accepted jobs execute.                       |

A practical example:

Suppose we build a SaaS application.

Our API usage policy is:

```
Free plan:
    100 requests per minute

Pro plan:
    1,000 requests per minute

Enterprise plan:
    10,000 requests per minute
```

We have many users and tenants.

We want something better than Fixed Window's boundary behavior, but we don't want to store every request timestamp.

Sliding Window Counter is a reasonable choice.

However, imagine a third-party payment provider explicitly requires:

```
Never exceed 10 requests in any rolling 10-second interval.
```

We should not blindly use Sliding Window Counter if underestimation could violate that requirement.

Sliding Window Log or a stricter traffic-shaping mechanism may be more appropriate.

The semantics of the limit matter more than the algorithm's popularity.

# **14. We now have three different approaches**

Let's step back and look at the evolution so far.

```
PROBLEM:
    A client sends too many requests.

SOLUTION 1: FIXED WINDOW

    Store one counter per fixed interval.

    Advantage:
        Very simple and inexpensive.

    Problem:
        Boundary bursts.


SOLUTION 2: SLIDING WINDOW LOG

    Store every request timestamp.

    Advantage:
        Accurate rolling-window calculation.

    Problem:
        High memory and write overhead.


SOLUTION 3: SLIDING WINDOW COUNTER

    Store previous and current window counters.

    Advantage:
        Much lower storage and approximate
        rolling-window enforcement.

    Problem:
        Uniform-distribution assumption can
        cause overestimation or underestimation.
```

Notice how each algorithm emerged because of a limitation in the previous one.

You don't need to memorize three unrelated algorithms.

You just need to remember how our representation of request history evolved:

```
Count requests
     |
     v
Remember timestamps
     |
     v
Remember summarized counts
```

The algorithms are different tradeoffs in how much information we retain about request history.

# **15. An important distinction before the next chapter**

We have been discussing how many requests a user can make during a time interval.

But there is another kind of problem.

Imagine our application runs a background job worker.

We receive 10,000 tasks.

Our downstream service says:

```
Process no more than 2 requests per second.
```

We don't necessarily want to reject 9,998 tasks.

We might want to accept all 10,000 tasks into our queue and process them gradually.

For example:

```
Incoming tasks:

T1 T2 T3 T4 T5 T6 T7 T8 T9 T10
```

Instead of immediately calling the downstream service for all of them:

```
T1, T2, T3, T4, ... T10
        |
        v
   Sudden traffic spike
```

We want something like:

```
Second 1: T1, T2
Second 2: T3, T4
Second 3: T5, T6
Second 4: T7, T8
Second 5: T9, T10
```

This is a different problem.

We are no longer merely deciding whether to accept an incoming request.

We are controlling the rate at which accepted work proceeds.

This introduces our next algorithm.

# **Chapter 3 — Final mental model**

Sliding Window Counter is built on a simple observation:

A rolling window overlaps at most two adjacent fixed windows.

Instead of remembering all requests, we maintain:

```
Previous window count
Current window count
```

When a request arrives:

1. Identify the current fixed window.
2. Rotate the counters if the window changed.
3. Calculate how much of the previous window overlaps with the rolling window.
4. Weight the previous counter by that overlap.
5. Add the current counter.
6. Include the incoming request and compare against the limit.
7. Increment the current counter if allowed.

The tradeoff:

**We significantly reduce storage at the cost of exact knowledge about when the previous requests happened.**

That's the entire algorithm.

## **Next Chapter — Leaky Bucket**

We'll now move from *counting requests* to *controlling the rate at which requests leave our system*.

We'll derive Leaky Bucket using a real-world analogy, understand:

- Why a queue is necessary.
- How incoming traffic gets buffered.
- How requests are processed at a fixed rate.
- What happens when the queue becomes full.
- What state we need to store.
- How to implement it with Redis and background workers.
- How multiple workers maintain a consistent processing rate.
- What happens when workers crash or downstream services become unavailable.
- Why this is useful for web crawlers, external API integrations, and job-processing systems.

The key distinction to take forward is:

**Sliding Window algorithms control admission based on request history. Leaky Bucket controls the rate at which work is released for processing.**

