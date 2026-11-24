# **Chapter 4 — Leaky Bucket Algorithm**

So far, we have studied three algorithms:

1. Fixed Window
2. Sliding Window Log
3. Sliding Window Counter

All three primarily solve the same problem:

> How many requests should we allow from a client within a particular time interval?

Now let's introduce a slightly different problem.

Imagine we are building a web crawler.

Our crawler needs to visit thousands of websites. Each website has its own rate limit.

For example:

```
Website A:
    Maximum 2 requests per second

Website B:
    Maximum 5 requests per second

Website C:
    Maximum 10 requests per second
```

Our crawler might discover 1,000 URLs for Website A almost simultaneously.

Should we send all 1,000 requests immediately and let the website reject them?

Obviously not.

We want to accept those URLs as work, but send requests to Website A gradually.

For example:

```
Incoming URLs:

U1 U2 U3 U4 U5 U6 U7 U8 U9 U10

                  |
                  v

        Rate-controlled processing

                  |
                  v

Time 0:   U1, U2
Time 1:   U3, U4
Time 2:   U5, U6
Time 3:   U7, U8
Time 4:   U9, U10
```

We are no longer just counting requests.

We are controlling **how quickly requests leave our system and reach another system.**

This is the problem Leaky Bucket helps solve.

# **1. Understanding the Leaky Bucket analogy**

Imagine a bucket with a small hole at its bottom.

Water enters the bucket from the top.

Water leaves through the hole at a constant rate.

```
             Incoming water
                   |
                   v
             ___________
            |           |
            |           |
            |           |
            |           |
            |___________|
                  |
                  |
                  v
             Water leaks
             at a constant
                rate
```

There are three important properties.

**1. Water can enter at any rate.**

Sometimes a small amount of water arrives.

Sometimes a large amount arrives suddenly.

**2. Water leaves at a controlled, constant rate.**

Regardless of how quickly water enters, the hole releases water at a fixed rate.

**3. The bucket has limited capacity.**

If water arrives faster than it can leave, the bucket fills up.

Once the bucket is full, additional water overflows.

Now translate this into our software system:

| Physical bucket | Software equivalent                        |
| --------------- | ------------------------------------------ |
| Water           | Incoming requests or jobs                  |
| Bucket          | Queue                                      |
| Hole            | Background worker or controlled dispatcher |
| Water leaving   | Requests sent to downstream service        |
| Leak rate       | Maximum processing rate                    |
| Bucket capacity | Maximum queue size                         |
| Overflow        | Reject, drop, or defer incoming work       |

The mental model is:

**Accept work into a bounded queue, then release it at a controlled rate.**

This is the queue-based form of Leaky Bucket, also called traffic shaping.

Let's build it from scratch.

# **2. Why is a queue necessary?**

Consider this requirement:

```
Maximum downstream request rate = 2 requests per second
```

Suppose our application receives 10 requests simultaneously.

If we use the algorithms from previous chapters, we might allow or reject requests based on their recent history.

But we have another possibility.

Instead of rejecting requests just because they arrived together, we can temporarily store them.

For example:

```
Incoming requests:

R1 R2 R3 R4 R5 R6 R7 R8 R9 R10

                |
                v

             Queue

        +----------------+
        | R1             |
        | R2             |
        | R3             |
        | R4             |
        | R5             |
        | R6             |
        | R7             |
        | R8             |
        | R9             |
        | R10            |
        +----------------+
```

A worker processes requests from this queue at a controlled rate.

```
Queue
  |
  | Worker processes at 2 requests/second
  v
Downstream Service
```

Our downstream service never receives all 10 requests simultaneously.

Instead:

```
Time       Requests sent

0s         R1
0.5s       R2
1s         R3
1.5s       R4
2s         R5
2.5s       R6
3s         R7
3.5s       R8
4s         R9
4.5s       R10
```

We accepted all 10 requests, but we controlled when they were executed.

This is the important difference from our previous algorithms.

**We are separating the rate at which work enters our system from the rate at which work leaves our system.**

That's the core of traffic shaping.

# **3. Understand the two important configurations**

Before implementing anything, let's identify the two things we need to configure.

### **3.1 Leak rate**

The leak rate determines how quickly requests leave the queue.

For example:

```
Leak rate = 2 requests per second
```

This means we should send requests to the downstream service at a rate of approximately 2 per second.

The interval between consecutive requests is:

\\[ Interval = \frac{1}{Rate} \\]

Therefore:

\\[ Interval = \frac{1}{2} = 0.5 \text{ seconds} \\]

So, we schedule one request every 500 milliseconds.

Other examples:

| Leak rate        | Interval between requests |
| ---------------- | ------------------------- |
| 1 request/sec    | 1 second                  |
| 2 requests/sec   | 500 ms                    |
| 5 requests/sec   | 200 ms                    |
| 10 requests/sec  | 100 ms                    |
| 100 requests/sec | 10 ms                     |

The leak rate is what protects the downstream service.

### **3.2 Bucket capacity**

Now consider a situation where 100 requests arrive immediately.

We cannot necessarily store all 100 requests.

Our queue also consumes resources:

- Memory.
- Database or message-broker storage.
- Worker capacity.
- Processing time.
- Potentially significant end-to-end latency.

Therefore, we define a maximum queue capacity.

For example:

```
Leak rate = 2 requests/second

Queue capacity = 10 pending requests
```

This means:

- We process at 2 requests per second.
- We can temporarily hold up to 10 pending requests.
- If the queue is full, we cannot accept more work without applying some additional policy.

The bucket capacity provides backpressure.

Imagine 15 requests arrive simultaneously:

```
Incoming:

R1 R2 R3 R4 R5 R6 R7 R8 R9 R10 R11 R12 R13 R14 R15

                         |
                         v

                    Queue capacity = 10

                    +----------------+
                    | R1             |
                    | R2             |
                    | R3             |
                    | R4             |
                    | R5             |
                    | R6             |
                    | R7             |
                    | R8             |
                    | R9             |
                    | R10            |
                    +----------------+

                    R11-R15 cannot be
                    accepted into this queue.
```

We must decide what to do with the excess requests.

Possible strategies:

1. Reject them immediately.
2. Return a retry-later response.
3. Apply backpressure to the producer.
4. Persist them in a larger durable queue, if the business permits longer delays.
5. Drop them, but only when losing the work is acceptable.

The right strategy depends on the application.

For example:

- An interactive HTTP API might return `429 Too Many Requests`.
- A background job producer might receive an acknowledgement only after the job is durably stored.
- An optional analytics event might be dropped during overload.

The important thing is that **a finite bucket must have a defined overflow policy**.

# **4. Let's understand the algorithm with a complete example**

Suppose we have:

```
Leak rate = 2 requests/second
Queue capacity = 5 pending requests
```

At time 0, five requests arrive.

```
Time = 0

Incoming:
    R1, R2, R3, R4, R5

Queue:
    [R1, R2, R3, R4, R5]
```

Our worker starts processing them.

Assuming our first request leaves immediately:

```
Time = 0.0:
    Send R1

Time = 0.5:
    Send R2

Time = 1.0:
    Send R3

Time = 1.5:
    Send R4

Time = 2.0:
    Send R5
```

The queue gradually becomes empty.

Now imagine three more requests arrive at time 1.

At that moment:

```
R1 has already been sent at time 0.

Pending queue:
    [R2, R3, R4, R5]
```

New arrivals:

```
R6, R7, R8
```

The updated queue:

```
[R2, R3, R4, R5, R6, R7, R8]
```

But wait!

Our capacity is 5, and we now have 7 pending requests.

So we cannot accept all three.

We need to reject or defer some of them.

For this example, assume the queue has capacity for five pending requests. We can accept R6 and reject R7 and R8.

The queue becomes:

```
[R2, R3, R4, R5, R6]
```

This illustrates three distinct properties:

1. Requests can arrive in bursts.
2. The queue absorbs a limited amount of that burst.
3. The output rate remains controlled.

## **4.1 What if the queue becomes empty?**

Suppose we have processed all pending requests.

There is no reason for the worker to keep sending requests.

It becomes idle until more work arrives.

```
Queue:
    []

Worker:
    IDLE
```

When a new request arrives, processing resumes.

The worker should not send artificial requests just to maintain a continuous flow.

The leak rate is a maximum scheduled output rate, not a requirement to send requests when no work exists.

# **5. A second implementation perspective: Virtual queue level**

So far, we have imagined an actual FIFO queue containing request objects.

There is another way to implement the Leaky Bucket concept.

Instead of storing an actual queue, we maintain a virtual bucket level.

The level represents how much work has accumulated.

This version is useful when we only need to make an allow/reject decision, rather than actually queue and process requests.

Imagine:

```
Capacity = 10 units

Current level = 7 units
```

Work enters the bucket:

```
New request cost = 1 unit

New level = 8
```

As time passes, the bucket drains continuously.

For example:

```
Leak rate = 2 units/second
```

After one second:

```
Previous level = 8

Drained = 2

New level = 6
```

After another second:

```
Previous level = 6

Drained = 2

New level = 4
```

This gives us a simple mathematical model.

Let:

- \\(L\\) = Current bucket level.
- \\(R\\) = Leak rate, in units per second.
- \\(t\\) = Time elapsed since the last update.
- \\(C\\) = Bucket capacity.
- \\(q\\) = Cost of the incoming request.

First, calculate how much work has leaked out.

\\[ Drained = R \times t \\]

The new level before receiving the request is:

\\[ L' = \max(0, L - R \times t) \\]

Now consider the incoming request.

\\[ NewLevel = L' + q \\]

The request can be admitted when:

\\[ NewLevel \leq C \\]

If it is admitted:

\\[ L = NewLevel \\]

Otherwise, reject it.

### **Example**

Suppose:

```
Capacity = 10
Leak rate = 2 units/second

Previous level = 8
Elapsed time = 1 second

Incoming request cost = 1
```

First, calculate the drained amount:

\\[ Drained = 2 \times 1 = 2 \\]

Remaining level:

\\[ L' = 8 - 2 = 6 \\]

Incoming request:

\\[ NewLevel = 6 + 1 = 7 \\]

Since:

\\[ 7 \leq 10 \\]

We allow the request.

But if the previous level were 10:

\\[ L' = 10 - 2 = 8 \\]

\\[ NewLevel = 8 + 1 = 9 \\]

Still allowed.

If no time had elapsed:

\\[ NewLevel = 10 + 1 = 11 \\]

Rejected.

The algorithm can be represented as:

```
function allowRequest(state, request):

    now = currentTime()

    elapsed = now - state.lastUpdated

    drained = leakRate * elapsed

    currentLevel = max(
        0,
        state.level - drained
    )

    newLevel = currentLevel + request.cost

    if newLevel > capacity:

        state.level = currentLevel
        state.lastUpdated = now

        return REJECT

    state.level = newLevel
    state.lastUpdated = now

    return ALLOW
```

Why update the state even on rejection?

Because some work may have drained since the previous update. We must not continue treating that expired work as occupying capacity.

### **An important distinction**

This virtual-level implementation does not actually preserve pending requests.

It only tracks how much of the rate budget is currently occupied.

It is useful for policing admission, but it cannot tell a worker which job to execute next.

For actual background processing, we need a real queue.

We'll use the queue-based model for our production system design, and keep this virtual-level model as another way to understand the mathematics.

# **6. What exactly should we store?**

Let's design the state for both forms.

## **6.1 Actual queue-based Leaky Bucket**

Suppose we are processing jobs for a downstream website.

We need to know:

```
Policy:
    target = website_A
    leakRate = 2 requests/second
    capacity = 100 pending requests

Queue:
    [job_1, job_2, job_3, ...]
```

Conceptually, we need:

| Information          | Purpose                                            |
| -------------------- | -------------------------------------------------- |
| Queue contents       | Jobs waiting to be processed                       |
| Queue length         | Number of pending jobs                             |
| Leak rate            | Maximum output rate                                |
| Queue capacity       | Maximum pending work                               |
| Ordering information | Which job should leave next                        |
| Retry information    | How failed jobs are handled                        |
| Target identity      | Which downstream service the rate limit applies to |

The queue is the actual work storage.

The rate and capacity are policy configuration.

We do not need to store a separate timestamp for every admission merely to calculate the leak rate.

## **6.2 Virtual-level implementation**

For the virtual queue, we need very little information:

```
identity = user_123

level = 7.5
last_updated = timestamp
```

The policy provides:

```
capacity = 10
leak_rate = 2
```

Each new request updates the level using elapsed time.

The important difference is:

```
Queue-based:
    Stores actual work.

Virtual-level:
    Stores accumulated work mathematically.
```

Choose based on whether you need to delay and execute requests or simply decide whether to admit them.

# **7. Building a real-world queue-based implementation**

Let's now design something practical.

Our system:

```
A service receives tasks and calls an external API.

External API limit:
    2 requests per second.

Our goal:
    Never overwhelm that external API.
```

We might have multiple application instances.

\#chatgpt-mermaid-\_r_1ha\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1ha\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ha\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ha\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1ha\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1ha\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1ha\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1ha\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1ha\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1ha\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ha\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ha\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1ha\_ p{margin:0;}#chatgpt-mermaid-\_r_1ha\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1ha\_ .label text,#chatgpt-mermaid-\_r_1ha\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .node rect,#chatgpt-mermaid-\_r_1ha\_ .node circle,#chatgpt-mermaid-\_r_1ha\_ .node ellipse,#chatgpt-mermaid-\_r_1ha\_ .node polygon,#chatgpt-mermaid-\_r_1ha\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .rough-node .label text,#chatgpt-mermaid-\_r_1ha\_ .node .label text,#chatgpt-mermaid-\_r_1ha\_ .image-shape .label,#chatgpt-mermaid-\_r_1ha\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1ha\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .rough-node .label,#chatgpt-mermaid-\_r_1ha\_ .node .label,#chatgpt-mermaid-\_r_1ha\_ .image-shape .label,#chatgpt-mermaid-\_r_1ha\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1ha\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1ha\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ha\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ha\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1ha\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ha\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ha\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ha\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1ha\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1ha\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ha\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1ha\_ .icon-shape,#chatgpt-mermaid-\_r_1ha\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ha\_ .icon-shape p,#chatgpt-mermaid-\_r_1ha\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1ha\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1ha\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ha\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1ha\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1ha\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1ha\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1ha\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1ha\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1ha\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ha\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1ha\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ha\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ha\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ha\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1ha\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1ha\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .node rect,#chatgpt-mermaid-\_r_1ha\_ .node circle,#chatgpt-mermaid-\_r_1ha\_ .node ellipse,#chatgpt-mermaid-\_r_1ha\_ .node polygon,#chatgpt-mermaid-\_r_1ha\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1ha\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1ha\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1ha\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1ha\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ha\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task ProducersApplication APIDurable QueueRate Controlled DispatcherExternal APIResult Handler

Let's examine each component.

### **Component 1: Task producers**

These could be:

- API requests.
- Web crawler discovery.
- Scheduled tasks.
- Internal services.
- Event consumers.

They submit work.

### **Component 2: Durable queue**

The queue stores pending jobs.

Possible choices:

- AWS SQS.
- Kafka.
- RabbitMQ.
- PostgreSQL-backed jobs table.

We choose based on the requirements.

For example:

**SQS**

Useful when we need managed task queues, visibility timeouts, retries, and DLQs.

**Kafka**

Useful when we need high-throughput event streams, partitioning, retention, and replay.

**PostgreSQL**

Useful when job state and application transactions need to be stored together, and the workload is moderate.

Remember, our rate-limiting algorithm does not force us to choose a particular queue technology.

### **Component 3: Rate-controlled dispatcher**

This is the important component.

It receives jobs from the queue but doesn't immediately execute them all.

Instead, it releases jobs at the configured rate.

For example:

```
External API:
    2 requests/second

Dispatcher:
    Send one request every 500ms
```

### **Component 4: External API**

Our dispatcher calls the external service.

We should still handle:

- Timeouts.
- Network failures.
- HTTP 429 responses.
- Retryable 5xx responses.
- Permanent failures.
- Downstream unavailability.

Rate limiting does not remove the need for ordinary reliability mechanisms.

# **8. How should the dispatcher release requests?**

Let's start with the simplest possible implementation.

Suppose a dispatcher has a FIFO queue.

It repeatedly:

1. Retrieves the next job.
2. Sends it to the downstream service.
3. Waits for the next permitted dispatch time.
4. Repeats.

For 2 requests per second:

```
interval = 500 milliseconds
```

Pseudocode:

```
function processQueue():

    while true:

        job = queue.getNextJob()

        if job does not exist:
            waitForNewJob()
            continue

        sendToDownstream(job)

        sleep(500 milliseconds)
```

This is useful for understanding the concept.

But there are two problems.

**Problem 1: The dispatcher sends requests sequentially.**

If the downstream call takes 2 seconds, the actual processing rate becomes one request every 2 seconds, not two requests per second.

**Problem 2: Multiple dispatchers can violate the limit.**

Imagine we start five worker instances, each following the 500 ms interval.

Each worker can send two requests per second.

Our actual aggregate rate could become:

\\[ 5 \times 2 = 10 \text{ requests/second} \\]

But the external service only allows 2 requests per second.

We need to solve both problems.

# **9. Separating request rate from request concurrency**

This is an extremely important concept for production system design.

Consider these two requirements:

```
Requirement A:
    Send at most 2 requests per second.

Requirement B:
    Have at most 5 requests executing simultaneously.
```

They are not the same.

**Rate** controls how frequently operations start.

**Concurrency** controls how many operations are in progress at the same time.

For example:

```
Request 1 starts at 0.0s
Request 2 starts at 0.5s
Request 3 starts at 1.0s
Request 4 starts at 1.5s
```

Suppose every downstream request takes 2 seconds.

At time 1.5 seconds:

```
Request 1: still executing
Request 2: still executing
Request 3: still executing
Request 4: just started
```

There are four requests in progress.

We started only two requests per second, but concurrency has increased.

If response times become very long, concurrency can continue to grow unless we place another limit on it.

Therefore, we may need both:

```
Rate limit:
    2 requests per second.

Concurrency limit:
    Maximum 5 active requests.
```

A dispatcher can schedule starts at the correct rate while a worker pool limits the number of simultaneous executions.

This distinction is important when calling slow external APIs.

# **10. Handling multiple workers correctly**

Let's return to the distributed problem.

Suppose we have:

\#chatgpt-mermaid-\_r_1id\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1id\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1id\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1id\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1id\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1id\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1id\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1id\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1id\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1id\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1id\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1id\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1id\_ p{margin:0;}#chatgpt-mermaid-\_r_1id\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1id\_ .label text,#chatgpt-mermaid-\_r_1id\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .node rect,#chatgpt-mermaid-\_r_1id\_ .node circle,#chatgpt-mermaid-\_r_1id\_ .node ellipse,#chatgpt-mermaid-\_r_1id\_ .node polygon,#chatgpt-mermaid-\_r_1id\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .rough-node .label text,#chatgpt-mermaid-\_r_1id\_ .node .label text,#chatgpt-mermaid-\_r_1id\_ .image-shape .label,#chatgpt-mermaid-\_r_1id\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1id\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .rough-node .label,#chatgpt-mermaid-\_r_1id\_ .node .label,#chatgpt-mermaid-\_r_1id\_ .image-shape .label,#chatgpt-mermaid-\_r_1id\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1id\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1id\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1id\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1id\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1id\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1id\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1id\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1id\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1id\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1id\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1id\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1id\_ .icon-shape,#chatgpt-mermaid-\_r_1id\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1id\_ .icon-shape p,#chatgpt-mermaid-\_r_1id\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1id\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1id\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1id\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1id\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1id\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1id\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1id\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1id\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1id\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1id\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1id\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1id\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1id\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .node rect,#chatgpt-mermaid-\_r_1id\_ .node circle,#chatgpt-mermaid-\_r_1id\_ .node ellipse,#chatgpt-mermaid-\_r_1id\_ .node polygon,#chatgpt-mermaid-\_r_1id\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1id\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1id\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1id\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1id\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1id\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Durable QueueDispatcher / CoordinatorWorker 1Worker 2Worker 3External API

How do we ensure that the aggregate output rate is limited to 2 requests per second?

We need some shared mechanism that controls the schedule.

One straightforward solution is to maintain:

```
next_available_time
```

For example:

```
External API:
    Rate = 2 requests/second

Interval = 500ms
```

Whenever a job needs to be scheduled, we atomically reserve its next execution time.

Suppose the current time is:

```
now = 1000ms
```

The previous next-available time is:

```
nextAvailable = 900ms
```

We calculate:

\\[ dispatchTime = \max(now, nextAvailable) \\]

Therefore:

```
dispatchTime = max(1000, 900)
             = 1000ms
```

Then advance the next available time:

\\[ nextAvailable = dispatchTime + interval \\]

```
nextAvailable = 1000 + 500
              = 1500ms
```

The next request arrives immediately.

Its dispatch time becomes:

```
dispatchTime = max(1000, 1500)
             = 1500ms
```

Then:

```
nextAvailable = 2000ms
```

The schedule looks like:

```
Request 1: 1000ms
Request 2: 1500ms
Request 3: 2000ms
Request 4: 2500ms
Request 5: 3000ms
```

This guarantees evenly spaced dispatch slots, assuming the scheduling operation is atomic and dispatches happen at their assigned times.

We can store this state in Redis:

```
rate_limit:website_A

next_available_time = 3000
```

But we need to reserve execution times atomically.

Otherwise, multiple workers might read the same `next_available_time` and reserve the same slot.

We can use a Redis Lua script that:

1. Reads the current time.
2. Reads `next_available_time`.
3. Calculates the new dispatch time.
4. Advances `next_available_time`.
5. Returns the reservation.

This is a useful distributed scheduling pattern.

**One subtlety:** reservations must be connected reliably to actual queue publication and execution. If a worker reserves a slot and then crashes before publishing or dispatching the corresponding job, the slot can be wasted. Production systems need a durable scheduling mechanism, leases, or reconciliation to handle these gaps.

# **11. How do we implement the scheduling mechanism with Redis?**

Let's make the scheduling logic concrete.

Our requirements:

```
Target: website_A

Rate: 2 requests/second
Interval: 500ms

Queue capacity: 100 pending jobs
```

We can maintain a Redis Hash:

```
rate_limit:website_A

next_available_time = 1700000000500
pending_count = 12
```

Here:

- `next_available_time` tells us when the next request may be dispatched.
- `pending_count` tracks how much work is waiting.

We also need an actual queue to retain the jobs.

For example:

```
website_A_queue:

job_1
job_2
job_3
job_4
...
```

The Redis state alone doesn't contain the actual work.

It only helps coordinate the scheduling policy.

## **11.1 Atomic scheduling operation**

Conceptually, our Redis Lua script does this:

```
function reserveDispatchSlot(target):

    now = getRedisServerTime()

    state = getState(target)

    dispatchTime = max(
        now,
        state.nextAvailableTime
    )

    state.nextAvailableTime =
        dispatchTime + interval

    saveState(state)

    return dispatchTime
```

For a rate of 2 requests per second:

```
interval = 500ms
```

Five workers might simultaneously request dispatch slots.

Redis processes their scheduling scripts atomically.

They receive different slots:

```
Worker 1 -> 1000ms
Worker 2 -> 1500ms
Worker 3 -> 2000ms
Worker 4 -> 2500ms
Worker 5 -> 3000ms
```

Each worker can now wait until its assigned dispatch time before sending the request.

However, there is an important production detail:

**Reserving a time slot is not the same as successfully executing a request.**

For example:

```
Worker 1 reserves 1000ms.
Worker 1 crashes.

Worker 2 is scheduled for 1500ms.
```

We might waste the first slot.

Worse, if a worker reserves a slot and waits locally, it may crash and lose the job unless the job is stored durably elsewhere.

This is why a production scheduler generally combines:

- A durable queue.
- Atomic scheduling or rate coordination.
- Worker leases or message visibility timeouts.
- Retry and recovery mechanisms.

The scheduling state should not be the only copy of our job.

# **12. How should we store the actual queue?**

This is an important system design decision.

Let's examine some choices.

### **Option A: Redis List**

We can use Redis Lists:

```
LPUSH website_A_queue job_1
LPUSH website_A_queue job_2
```

Workers retrieve jobs using the appropriate FIFO consumption pattern.

We can also use Redis Streams when we need consumer groups and message acknowledgement.

Advantages:

- Fast operations.
- Convenient when the queue is relatively small.
- Redis can combine rate coordination and queue-related state.

Disadvantages:

- Redis memory can become expensive for large backlogs.
- We need to carefully design acknowledgement, recovery, persistence, and dead-letter handling.
- A Redis restart or failure can affect both queueing and rate coordination unless persistence and high availability are configured appropriately.

### **Option B: AWS SQS**

We can use SQS as the durable job queue.

\#chatgpt-mermaid-\_r_1ks\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1ks\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ks\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1ks\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1ks\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1ks\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1ks\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1ks\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1ks\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1ks\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ks\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ks\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1ks\_ p{margin:0;}#chatgpt-mermaid-\_r_1ks\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1ks\_ .label text,#chatgpt-mermaid-\_r_1ks\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .node rect,#chatgpt-mermaid-\_r_1ks\_ .node circle,#chatgpt-mermaid-\_r_1ks\_ .node ellipse,#chatgpt-mermaid-\_r_1ks\_ .node polygon,#chatgpt-mermaid-\_r_1ks\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .rough-node .label text,#chatgpt-mermaid-\_r_1ks\_ .node .label text,#chatgpt-mermaid-\_r_1ks\_ .image-shape .label,#chatgpt-mermaid-\_r_1ks\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1ks\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .rough-node .label,#chatgpt-mermaid-\_r_1ks\_ .node .label,#chatgpt-mermaid-\_r_1ks\_ .image-shape .label,#chatgpt-mermaid-\_r_1ks\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1ks\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1ks\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ks\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ks\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1ks\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ks\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ks\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ks\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1ks\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1ks\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1ks\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1ks\_ .icon-shape,#chatgpt-mermaid-\_r_1ks\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1ks\_ .icon-shape p,#chatgpt-mermaid-\_r_1ks\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1ks\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1ks\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1ks\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1ks\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1ks\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1ks\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ks\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1ks\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1ks\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ks\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1ks\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1ks\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1ks\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .node rect,#chatgpt-mermaid-\_r_1ks\_ .node circle,#chatgpt-mermaid-\_r_1ks\_ .node ellipse,#chatgpt-mermaid-\_r_1ks\_ .node polygon,#chatgpt-mermaid-\_r_1ks\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1ks\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1ks\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1ks\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1ks\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1ks\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Task ProducersSQSRate Controlled WorkersExternal APISuccess HandlerRetry / DLQ

Advantages:

- Managed durable queue.
- Visibility timeout.
- Retry handling.
- Dead-letter queues.
- Easy horizontal scaling.

Disadvantages:

- We still need to coordinate the aggregate request rate across workers.
- SQS does not automatically know the rate limit of our downstream service.
- Delayed execution and per-target ordering require additional design.

### **Option C: Kafka**

Kafka is suitable when we have high-volume streams of tasks and want partition-based ordering and consumption.

We could partition work using the target identity:

```
Partition key = website_A
```

This helps ensure that records for a particular website are processed in partition order by a consumer group.

However, partition ordering alone does not enforce a request rate.

A consumer might receive 1,000 records and process them all very quickly.

We still need a paced dispatcher.

Also, a single Kafka partition is consumed by one consumer within a consumer group at a time. This gives us a useful serialization boundary, but it can create a throughput bottleneck for a particularly busy target.

### **Which would I choose?**

For an ordinary background integration service:

```
SQS + Redis rate coordinator + workers
```

is a reasonable architecture.

For a high-volume event-processing system:

```
Kafka + partitioning by target + rate-controlled consumers
```

can be appropriate.

The key insight is:

**The queue stores pending work. The rate coordinator controls when that work is allowed to leave.**

These are two separate responsibilities.

# **13. The complete production flow — A web crawler example**

Let's bring everything together with a practical example.

We are building a distributed web crawler.

It has three crawler instances:

```
Crawler 1
Crawler 2
Crawler 3
```

They discover URLs from different sources.

Suppose all three discover URLs belonging to the same website.

```
Crawler 1:
    website_A/page1
    website_A/page2

Crawler 2:
    website_A/page3
    website_A/page4

Crawler 3:
    website_A/page5
    website_A/page6
```

Website A allows:

```
2 requests per second
```

Our design:

\#chatgpt-mermaid-\_r_1lr\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1lr\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1lr\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1lr\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1lr\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1lr\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1lr\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1lr\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1lr\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1lr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lr\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lr\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1lr\_ p{margin:0;}#chatgpt-mermaid-\_r_1lr\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1lr\_ .label text,#chatgpt-mermaid-\_r_1lr\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .node rect,#chatgpt-mermaid-\_r_1lr\_ .node circle,#chatgpt-mermaid-\_r_1lr\_ .node ellipse,#chatgpt-mermaid-\_r_1lr\_ .node polygon,#chatgpt-mermaid-\_r_1lr\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .rough-node .label text,#chatgpt-mermaid-\_r_1lr\_ .node .label text,#chatgpt-mermaid-\_r_1lr\_ .image-shape .label,#chatgpt-mermaid-\_r_1lr\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1lr\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .rough-node .label,#chatgpt-mermaid-\_r_1lr\_ .node .label,#chatgpt-mermaid-\_r_1lr\_ .image-shape .label,#chatgpt-mermaid-\_r_1lr\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1lr\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1lr\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lr\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lr\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1lr\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1lr\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lr\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lr\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1lr\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1lr\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1lr\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1lr\_ .icon-shape,#chatgpt-mermaid-\_r_1lr\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1lr\_ .icon-shape p,#chatgpt-mermaid-\_r_1lr\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1lr\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1lr\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1lr\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1lr\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1lr\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1lr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1lr\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1lr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1lr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lr\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1lr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1lr\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1lr\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1lr\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1lr\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1lr\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .node rect,#chatgpt-mermaid-\_r_1lr\_ .node circle,#chatgpt-mermaid-\_r_1lr\_ .node ellipse,#chatgpt-mermaid-\_r_1lr\_ .node polygon,#chatgpt-mermaid-\_r_1lr\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1lr\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1lr\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1lr\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1lr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1lr\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Crawler 1Job QueueCrawler 2Crawler 3Target-aware DispatcherRate Coordinator / RedisWorker PoolWebsite AWebsite BWebsite C

Let's follow one job end to end.

### **Step 1: Discover a URL**

Crawler 1 discovers:

```
https://website-a.com/page1
```

It creates a crawl job:

```
{
  "jobId": "job_001",
  "target": "website_A",
  "url": "https://website-a.com/page1"
}
```

### **Step 2: Add the job to the queue**

The job becomes pending.

```
website_A_queue:

[job_001]
```

### **Step 3: Identify the applicable rate-limit policy**

The dispatcher looks up:

```
website_A:
    rate = 2 requests/second
    queueCapacity = 100
```

Different websites have different policies.

For example:

```
website_A:
    2 requests/second

website_B:
    5 requests/second

website_C:
    10 requests/second
```

The rate limit must be scoped to the actual downstream limit.

If the limit is per hostname, our coordination identity might be:

```
crawler:{hostname}
```

If the website has a global limit across all its hostnames, we may need a broader identity.

This is a policy decision, not something the algorithm can decide automatically.

### **Step 4: Reserve a dispatch slot**

The dispatcher requests the next available slot from Redis.

Suppose the returned timestamp is:

```
dispatch_at = 1700000000500
```

### **Step 5: Wait until the job is eligible**

The dispatcher makes the job available to a worker when its scheduled time arrives.

### **Step 6: Call the website**

The worker performs:

```
GET /page1
```

### **Step 7: Handle the result**

If successful:

- Store the crawl result.
- Extract further URLs.
- Acknowledge the completed job.
- Add newly discovered jobs to the queue.

If it fails:

- Apply retry rules.
- Respect the downstream's `Retry-After` information when provided.
- Use exponential backoff and jitter where appropriate.
- Send permanently failed jobs to a dead-letter queue.

One important point: **retries are also outgoing requests**.

If we retry without rate coordination, our retry traffic could overwhelm the same service we were trying to protect.

Therefore, every actual HTTP attempt must pass through the applicable rate-control mechanism.

# **14. What happens when the downstream service returns HTTP 429?**

This is a particularly important real-world scenario.

Imagine our configuration says:

```
2 requests per second
```

But the downstream service responds:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
```

This means the downstream is telling us to wait 30 seconds before trying again.

What should our system do?

We should not blindly continue sending requests at our configured rate.

Instead:

1. Recognize the 429 response.
2. Determine the applicable retry delay.
3. Pause or defer work for the affected rate-limit scope.
4. Reschedule the failed job.
5. Avoid immediately retrying all other jobs for that same scope if the provider's restriction applies to them too.

For example:

```
website_A:

Configured rate:
    2 requests/second

Downstream returns:
    Retry-After: 30
```

We can temporarily set:

```
blocked_until = currentTime + 30 seconds
```

The dispatcher should not release work for that affected scope before the block expires.

A Redis Hash might contain:

```
rate_limit:website_A

next_available_time = ...
blocked_until = ...
```

This allows us to combine our configured pacing policy with dynamic downstream feedback.

Be careful about interpreting the response:

- Is the 429 limit per API key?
- Per user?
- Per IP?
- Per hostname?
- Global across the whole provider?

We must pause the correct scope, not unnecessarily stop unrelated traffic.

Also, some providers use `Retry-After` as a minimum delay while others expose additional quota-reset information. The integration should follow that provider's documented semantics.

# **15. What happens when the worker crashes?**

Let's consider reliability.

Suppose:

```
Job queue:

[job_1, job_2, job_3]
```

A worker receives `job_1`.

It begins processing:

```
Worker:
    job_1 = IN_PROGRESS
```

But then the worker crashes before acknowledging the job.

We don't want to lose the job.

With SQS, for example, the message becomes invisible for the configured visibility timeout.

If it is not acknowledged before the timeout expires, it becomes available again.

Another worker can retry it.

However, this creates a subtle issue.

The original worker might have sent the HTTP request successfully before crashing.

The retry could send the same HTTP request again.

Therefore, the downstream operation might be executed twice.

For safe job processing, consider:

- Idempotency keys, when the downstream supports them.
- Deduplication.
- Persisted job state.
- Acknowledgement only after the required completion condition.
- Retry limits and DLQs.

Also remember that each retry must obtain a new dispatch slot.

We should never let retries bypass the rate limiter.

# **16. How do we handle queue capacity correctly in a distributed system?**

Let's return to our queue capacity.

Suppose:

```
website_A:

Maximum pending jobs = 100
```

We have three crawler instances adding jobs concurrently.

We cannot allow each crawler to independently believe the queue has 100 free slots.

Imagine:

```
Crawler 1 believes: 100 slots available
Crawler 2 believes: 100 slots available
Crawler 3 believes: 100 slots available
```

All three might add 100 jobs.

Our actual queue could contain 300 jobs.

We need a shared capacity mechanism.

One approach is a Redis counter:

```
website_A_pending_count = 95
```

When a new job arrives, an atomic operation checks:

```
if pending_count < 100:

    pending_count += 1

    ACCEPT

else:

    REJECT / DEFER
```

When a job reaches a terminal state, decrement the pending count.

But don't use a simple increment/decrement design without considering crashes.

For example:

```
Job accepted.
Pending count incremented.
Worker processes job.
Worker crashes before decrementing.
```

Our counter becomes inaccurate.

A more robust design maintains explicit job states and uses reservations with recovery.

For example:

```
job_001:
    status = PENDING

job_002:
    status = IN_PROGRESS

job_003:
    status = COMPLETED
```

We can combine:

- Atomic enqueue reservations.
- Durable job records.
- Worker leases.
- Idempotent state transitions.
- Periodic reconciliation.

For large systems, the actual durable queue often becomes the source of truth for pending work, while Redis provides fast coordination and admission state.

There is no universally correct queue-capacity implementation; the right one depends on whether we need an exact hard cap or an approximate operational backlog limit.

# **17. What happens if our queue is not FIFO?**

Leaky Bucket is usually described with a FIFO queue.

That means:

```
First In -> First Out
```

Suppose:

```
Queue:

R1 R2 R3 R4 R5
```

The dispatcher sends them in exactly that order.

But real systems may need priorities.

Imagine a job queue containing:

```
Critical payment notification
Normal email
Low-priority analytics event
```

Perhaps we should prioritize the payment notification.

We can use multiple queues or a priority queue.

However, remember that changing the scheduling order doesn't remove the need for rate coordination.

For example:

```
High priority queue
Normal priority queue
Low priority queue
          |
          v
   Shared scheduler
          |
          v
   Downstream service
```

Every request still passes through the same rate-control policy.

Otherwise, the high-priority queue might independently consume the entire downstream quota.

We should also be careful about starvation: a permanently busy high-priority queue could prevent lower-priority jobs from being processed.

# **18. Advantages and disadvantages of Leaky Bucket**

Now that we understand the algorithm from both the mathematical and implementation perspectives, let's evaluate it.

## **Advantages**

**1. Smooths outgoing traffic**

This is its biggest benefit.

Incoming bursts don't immediately translate into downstream traffic bursts.

**2. Protects downstream systems**

We can control how quickly requests leave our application.

This is useful when integrating with APIs that have strict throughput limits.

**3. Can absorb temporary bursts**

The queue allows us to accept temporary increases in incoming traffic, provided there is enough capacity and the work can wait.

**4. Provides backpressure**

When the queue fills, the system can reject additional work or signal the producer to slow down.

This prevents unlimited backlog growth.

**5. Predictable processing rate**

We can configure a fixed output rate and schedule execution accordingly.

**6. Natural fit for asynchronous workloads**

Background jobs, web crawlers, notifications, and data-processing pipelines often benefit from queue-based traffic shaping.

## **Disadvantages**

**1. Additional latency**

Requests might wait in the queue before being processed.

For example:

```
2 requests/second
100 pending jobs
```

The last jobs could wait approximately 50 seconds or more, depending on whether a request can be dispatched immediately and how the queue is defined.

**2. Queue storage overhead**

Unlike simple counter-based rate limiters, we need to store the actual pending work.

**3. Queue overflow**

A queue has finite capacity.

When incoming work consistently exceeds the processing rate, the queue eventually fills.

**4. More infrastructure**

We need queue management, workers, acknowledgements, retries, and scheduling coordination.

**5. Not ideal for synchronous APIs**

Imagine a user calls an API and expects an immediate response.

If we put the request in a queue and wait 30 seconds, the user experience may be terrible.

For these cases, a token bucket or a direct admission limiter may be more appropriate.

**6. Failure and recovery complexity**

Worker crashes, duplicate deliveries, scheduling gaps, downstream failures, and retries all need to be handled.

**7. Fixed output rate can underutilize capacity**

Suppose the downstream can actually handle 10 requests per second, but our policy allows only 2.

We will intentionally use only a portion of its capacity.

We need to configure the rate appropriately and potentially adjust it using downstream feedback.

# **19. Leaky Bucket vs the algorithms we've studied**

Let's compare the fundamental behaviors.

| Property                | Fixed Window                      | Sliding Window Log     | Sliding Window Counter     | Leaky Bucket                              |
| ----------------------- | --------------------------------- | ---------------------- | -------------------------- | ----------------------------------------- |
| Primary purpose         | Limit requests per fixed interval | Enforce rolling limits | Approximate rolling limits | Smooth outgoing traffic                   |
| State                   | Counter                           | Timestamps             | Two counters               | Queue or virtual level                    |
| Allows incoming bursts? | Yes, up to quota                  | Yes, up to quota       | Yes, up to estimated quota | Yes, up to available queue capacity       |
| Controls outgoing rate? | No                                | No                     | No                         | Yes                                       |
| Storage                 | Low                               | High                   | Low                        | Depends on queued work                    |
| Adds waiting time?      | No                                | No                     | No                         | Yes, when queueing                        |
| Typical use             | Basic API quotas                  | Strict rolling quotas  | General API rate limiting  | Background jobs and downstream protection |

The most important difference:

```
Sliding Window:

    Request arrives
         |
         v
    Check request history
         |
         v
    ALLOW or REJECT


Leaky Bucket:

    Request arrives
         |
         v
    Put into queue
         |
         v
    Wait for dispatch slot
         |
         v
    Send at controlled rate
```

One important terminology detail: Leaky Bucket is also used to describe a virtual-queue *metering* algorithm that admits or rejects traffic based on accumulated backlog. That version doesn't necessarily buffer actual jobs. We covered that in Section 5. The actual FIFO queue version is traffic shaping.

# **20. When should we use Leaky Bucket?**

Let's connect this to real-world scenarios.

| Scenario                     | Suitability              | Reason                                                           |
| ---------------------------- | ------------------------ | ---------------------------------------------------------------- |
| Web crawler                  | Excellent                | Crawl websites at controlled rates.                              |
| Background job workers       | Excellent                | Process queued jobs gradually.                                   |
| Third-party API integration  | Excellent                | Protect external systems against sudden traffic spikes.          |
| Email/SMS delivery           | Excellent                | Smooth provider requests and respect throughput limits.          |
| Data ingestion pipelines     | Good                     | Prevent downstream processors from being overwhelmed.            |
| Synchronous public REST APIs | Depends                  | Queueing introduces latency; rejection may be better.            |
| Strict rolling API quotas    | Not sufficient by itself | A queue's output rate isn't the same as a rolling request quota. |

For example, imagine an application sends 100,000 notification jobs.

The notification provider allows:

```
Maximum 50 requests per second.
```

We can accept notification jobs into a durable queue and dispatch them at the configured rate.

That is a very natural Leaky Bucket use case.

# **21. A subtle but important question: Is Leaky Bucket always the best for downstream rate limiting?**

Not necessarily.

Consider two requirements.

**Requirement A:**

```
The external API allows 2 requests per second.

We can accept work and process it gradually.
```

Leaky Bucket is a natural choice.

**Requirement B:**

```
The external API allows bursts of 10 requests.

After that, it refills its capacity at
2 requests per second.
```

A fixed-rate Leaky Bucket would not take full advantage of the allowed bursts.

For this second requirement, Token Bucket is more appropriate.

This brings us to a very important distinction.

With Leaky Bucket:

```
Incoming traffic:
    Can be bursty.

Outgoing traffic:
    Smooth and controlled.
```

With Token Bucket:

```
Incoming traffic:
    Can be bursty.

Outgoing traffic:
    Can also be bursty, as long as
    sufficient tokens are available.
```

Token Bucket allows bursts intentionally, while Leaky Bucket smooths them.

We will derive exactly why in the next chapter.

# **22. Final mental model**

Let's summarize everything through the story.

We started with an application receiving requests faster than a downstream system could handle.

We didn't necessarily want to reject every request arriving during a burst.

So we introduced a queue.

But an ordinary queue alone doesn't protect the downstream service. Workers could consume it as quickly as possible and create another sudden traffic spike.

Therefore, we introduced a dispatcher that releases jobs at a controlled rate.

The queue has a maximum capacity to prevent unlimited backlog growth.

The result:

```
Bursty incoming traffic
          |
          v
   Bounded FIFO Queue
          |
          v
   Rate-controlled
      Dispatcher
          |
          v
   Smooth outgoing traffic
          |
          v
    Downstream Service
```

The two fundamental configuration parameters are:

```
Leak rate:
    How quickly work leaves.

Bucket capacity:
    How much pending work we can hold.
```

The essential idea to remember:

**Leaky Bucket separates how quickly work arrives from how quickly work is processed.**

You now have four distinct mental models:

1. Fixed Window: Count requests in predefined intervals.
2. Sliding Window Log: Remember actual request timestamps.
3. Sliding Window Counter: Approximate request history using counters.
4. Leaky Bucket: Buffer work and release it at a controlled rate.

# **Next Chapter — Token Bucket**

This is arguably the most important algorithm to understand for practical API rate limiting.

We'll start with a situation where Leaky Bucket is unnecessarily restrictive.

Imagine a provider allows:

```
Average rate = 2 requests per second

But it also permits an immediate burst of 10 requests.
```

A system using Leaky Bucket might send requests evenly, one every 500 ms.

But the provider actually allows us to use 10 requests immediately!

How can we take advantage of that capacity without allowing unlimited traffic?

We'll derive Token Bucket from the idea of a virtual container of tokens:

- Tokens are generated over time.
- Each request consumes a token.
- Tokens accumulate when the client is idle.
- Accumulated tokens permit future bursts.
- The bucket has a maximum capacity, so tokens cannot accumulate indefinitely.

We'll cover the complete mathematics, Redis implementation, atomic validation, distributed workers, burst behavior, and practical policy configuration.

**The key question for that chapter: How can we allow bursts while still controlling the long-term average request rate?**
