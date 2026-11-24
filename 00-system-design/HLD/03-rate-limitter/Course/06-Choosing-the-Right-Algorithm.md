# **Chapter 6 — Choosing the Right Rate Limiting Algorithm**

We've now understood all five algorithms individually.

But knowing how an algorithm works is only half the job.

In a real system design interview, the important question is:

> Given a particular problem, how do I identify which algorithm is appropriate, and why?

We don't want to choose Token Bucket simply because it's popular, or Sliding Window because it sounds more accurate.

We want to understand the actual requirements, derive the necessary behavior, and then choose the algorithm.

In this chapter, we'll do exactly that using real-world systems.

## **1. First, understand the five different requirements**

Before looking at applications, let's establish a decision-making framework.

Imagine someone gives you a rate-limiting requirement.

The first thing you should ask is:

**What exactly are we trying to control?**

There are several fundamentally different requirements.

### **Requirement A: Limit usage within a predefined period**

Example:

```
A user can make 10,000 API requests per day.
```

We don't particularly care whether requests arrive evenly or in bursts.

We only care about the total number of requests in the day's quota period.

Think:

```
Count requests in a fixed interval.
```

Suitable algorithm: **Fixed Window**.

### **Requirement B: Enforce a rolling time limit**

Example:

```
A user can make at most 100 requests
in any 60-second period.
```

We don't want the user to exploit window boundaries.

Think:

```
Count requests in the time interval
immediately preceding the current request.
```

Suitable algorithm: **Sliding Window Log**, or **Sliding Window Counter** when approximation is acceptable.

### **Requirement C: Allow bursts but control sustained consumption**

Example:

```
Allow 100 requests immediately.

After that, replenish permission at
10 requests per second.
```

Think:

```
Allow clients to accumulate permission
and spend it when they send requests.
```

Suitable algorithm: **Token Bucket**.

### **Requirement D: Smooth incoming traffic before sending it downstream**

Example:

```
Accept background jobs, but send
requests to the external API at
a controlled rate.
```

Think:

```
Store work in a queue and release it gradually.
```

Suitable algorithm: **Leaky Bucket**.

### **Requirement E: Protect the number of simultaneously executing operations**

Example:

```
At most 20 database queries should
execute concurrently.
```

Think:

```
Limit in-flight operations, not requests
per time interval.
```

This is a **concurrency limiter**, not one of our five rate-limiting algorithms.

We can combine it with Token Bucket or another admission limiter.

That last distinction is particularly important.

## **2. The decision-making framework**

When you encounter a new requirement, reason through these questions in order.

\#chatgpt-mermaid-\_r_2ca\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2ca\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2ca\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2ca\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2ca\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2ca\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2ca\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2ca\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2ca\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2ca\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ca\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ca\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2ca\_ p{margin:0;}#chatgpt-mermaid-\_r_2ca\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2ca\_ .label text,#chatgpt-mermaid-\_r_2ca\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .node rect,#chatgpt-mermaid-\_r_2ca\_ .node circle,#chatgpt-mermaid-\_r_2ca\_ .node ellipse,#chatgpt-mermaid-\_r_2ca\_ .node polygon,#chatgpt-mermaid-\_r_2ca\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .rough-node .label text,#chatgpt-mermaid-\_r_2ca\_ .node .label text,#chatgpt-mermaid-\_r_2ca\_ .image-shape .label,#chatgpt-mermaid-\_r_2ca\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2ca\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .rough-node .label,#chatgpt-mermaid-\_r_2ca\_ .node .label,#chatgpt-mermaid-\_r_2ca\_ .image-shape .label,#chatgpt-mermaid-\_r_2ca\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2ca\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2ca\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ca\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ca\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2ca\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2ca\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ca\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ca\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2ca\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2ca\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2ca\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2ca\_ .icon-shape,#chatgpt-mermaid-\_r_2ca\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2ca\_ .icon-shape p,#chatgpt-mermaid-\_r_2ca\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2ca\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2ca\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2ca\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2ca\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2ca\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2ca\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2ca\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2ca\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2ca\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ca\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2ca\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2ca\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2ca\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2ca\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2ca\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2ca\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .node rect,#chatgpt-mermaid-\_r_2ca\_ .node circle,#chatgpt-mermaid-\_r_2ca\_ .node ellipse,#chatgpt-mermaid-\_r_2ca\_ .node polygon,#chatgpt-mermaid-\_r_2ca\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2ca\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2ca\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2ca\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2ca\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2ca\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Understand the requirementNeed a fixed-period quota?Fixed WindowNeed strict rolling-windowenforcement?Is approximation acceptable?Sliding Window LogSliding Window CounterShould bursts be allowed?Token BucketNeed to queue and smoothwork?Leaky BucketEvaluate other controlsYesNoYesNoYesNoYesNoYesNo

This is a starting point, not a rigid algorithm-selection rule.

For example, a system may need a daily quota and a per-second burst limit simultaneously.

In that case, we combine algorithms.

Let's explore actual systems.

# **3. Scenario 1 — Public REST API / API Gateway**

Imagine we're building a SaaS application exposing APIs to customers.

```
GET  /api/products
GET  /api/orders
POST /api/orders
POST /api/reports
```

We want to prevent one customer from consuming excessive resources.

Our subscription plans are:

```
Free:
    10 requests/second
    Burst capacity = 20

Pro:
    100 requests/second
    Burst capacity = 200

Enterprise:
    1,000 requests/second
    Burst capacity = 2,000
```

### **Step 1: Understand the actual requirement**

We aren't asking:

```
How many requests happened during
the previous 60 seconds?
```

Instead, we want:

```
Allow normal bursts, but prevent
customers from continuously consuming
more than their allocated capacity.
```

This maps naturally to Token Bucket.

### **Step 2: Define the policy**

For a Pro customer:

```
Identity = tenant_123

Algorithm = Token Bucket

Capacity = 200 tokens

Refill rate = 100 tokens/second

Request cost = 1 token
```

The customer starts with 200 tokens.

They can make an initial burst of up to 200 requests.

After consuming those tokens, they receive 100 tokens per second.

### **Step 3: Where should the Rate Limiter live?**

A common architecture:

\#chatgpt-mermaid-\_r_2d5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2d5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2d5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2d5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2d5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2d5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2d5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2d5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2d5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2d5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2d5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2d5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2d5\_ p{margin:0;}#chatgpt-mermaid-\_r_2d5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2d5\_ .label text,#chatgpt-mermaid-\_r_2d5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .node rect,#chatgpt-mermaid-\_r_2d5\_ .node circle,#chatgpt-mermaid-\_r_2d5\_ .node ellipse,#chatgpt-mermaid-\_r_2d5\_ .node polygon,#chatgpt-mermaid-\_r_2d5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .rough-node .label text,#chatgpt-mermaid-\_r_2d5\_ .node .label text,#chatgpt-mermaid-\_r_2d5\_ .image-shape .label,#chatgpt-mermaid-\_r_2d5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2d5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .rough-node .label,#chatgpt-mermaid-\_r_2d5\_ .node .label,#chatgpt-mermaid-\_r_2d5\_ .image-shape .label,#chatgpt-mermaid-\_r_2d5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2d5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2d5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2d5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2d5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2d5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2d5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2d5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2d5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2d5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2d5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2d5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2d5\_ .icon-shape,#chatgpt-mermaid-\_r_2d5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2d5\_ .icon-shape p,#chatgpt-mermaid-\_r_2d5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2d5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2d5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2d5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2d5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2d5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2d5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2d5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2d5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2d5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2d5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2d5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2d5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2d5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .node rect,#chatgpt-mermaid-\_r_2d5\_ .node circle,#chatgpt-mermaid-\_r_2d5\_ .node ellipse,#chatgpt-mermaid-\_r_2d5\_ .node polygon,#chatgpt-mermaid-\_r_2d5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2d5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2d5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2d5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2d5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2d5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}ClientAPI GatewayAuthenticationRate LimiterAllowed?Backend ServiceHTTP 429YesNo

The gateway identifies the tenant or API key and checks the relevant token bucket.

The backend only receives requests that pass admission.

### **Step 4: What should we store?**

```
rate_limit:tenant_123

tokens = 150
last_refill_time = ...
```

The policy configuration is maintained separately:

```
tenant_123:

capacity = 200
refill_rate = 100
```

### **Step 5: What if the customer exceeds the limit?**

Return:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 1
```

We can also expose standard rate-limit information:

```
RateLimit-Limit: 200
RateLimit-Remaining: 0
RateLimit-Reset: 1
```

Header conventions vary between API specifications and providers, so we should choose and document a consistent contract.

### **Why not use Sliding Window?**

Because we explicitly want to allow bursts.

A strict rolling limit of 100 requests per second would prevent some bursts that our policy intentionally permits.

**Decision: Token Bucket.**

This is one of the strongest default use cases for Token Bucket.

# **4. Scenario 2 — Login API and Brute-Force Protection**

Now imagine we're building authentication APIs.

```
POST /api/login
POST /api/password-reset
POST /api/otp/verify
```

An attacker might repeatedly try passwords or OTPs.

We want to restrict attempts.

But there's an interesting complication.

What identifies the attacker?

It might be:

- An IP address.
- An account.
- A device.
- An IP-and-account combination.

We need to think carefully about this.

### **Step 1: Understand the requirement**

Suppose we define:

```
Maximum 5 login attempts
in any 60-second interval.
```

We want a rolling limit.

For example, a client shouldn't be able to make five attempts at 59 seconds and another five immediately at 60 seconds.

This rules out basic Fixed Window if we need strict rolling enforcement.

### **Step 2: Choose the algorithm**

For relatively low authentication-attempt volumes, Sliding Window Log is a reasonable choice.

It records individual attempt timestamps.

For an account:

```
login_attempts:user_123

[10:00:01, 10:00:08, 10:00:17, 10:00:25, 10:00:39]
```

When a new attempt arrives, we remove timestamps outside the last 60 seconds.

If five attempts remain, we reject or apply additional authentication controls.

For very high-volume systems, Sliding Window Counter may offer a lower-storage approximation, but exact security policies need careful evaluation.

### **Step 3: Use multiple independent limits**

A single IP-based limit is insufficient.

Consider:

```
Attacker 1:
    Makes 100 login attempts
    against the same account from different IPs.

Attacker 2:
    Makes attempts against 1,000 accounts
    from the same IP.
```

A good design might combine:

```
Limit A:
    Per account

Limit B:
    Per IP

Limit C:
    Per device or other risk identity

Limit D:
    Global authentication service protection
```

Each limit protects against a different attack pattern.

\#chatgpt-mermaid-\_r_2eg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2eg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2eg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2eg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2eg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2eg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2eg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2eg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2eg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2eg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2eg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2eg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2eg\_ p{margin:0;}#chatgpt-mermaid-\_r_2eg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2eg\_ .label text,#chatgpt-mermaid-\_r_2eg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .node rect,#chatgpt-mermaid-\_r_2eg\_ .node circle,#chatgpt-mermaid-\_r_2eg\_ .node ellipse,#chatgpt-mermaid-\_r_2eg\_ .node polygon,#chatgpt-mermaid-\_r_2eg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .rough-node .label text,#chatgpt-mermaid-\_r_2eg\_ .node .label text,#chatgpt-mermaid-\_r_2eg\_ .image-shape .label,#chatgpt-mermaid-\_r_2eg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2eg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .rough-node .label,#chatgpt-mermaid-\_r_2eg\_ .node .label,#chatgpt-mermaid-\_r_2eg\_ .image-shape .label,#chatgpt-mermaid-\_r_2eg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2eg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2eg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2eg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2eg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2eg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2eg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2eg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2eg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2eg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2eg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2eg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2eg\_ .icon-shape,#chatgpt-mermaid-\_r_2eg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2eg\_ .icon-shape p,#chatgpt-mermaid-\_r_2eg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2eg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2eg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2eg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2eg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2eg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2eg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2eg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2eg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2eg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2eg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2eg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2eg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2eg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2eg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2eg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2eg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .node rect,#chatgpt-mermaid-\_r_2eg\_ .node circle,#chatgpt-mermaid-\_r_2eg\_ .node ellipse,#chatgpt-mermaid-\_r_2eg\_ .node polygon,#chatgpt-mermaid-\_r_2eg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2eg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2eg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2eg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2eg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2eg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Login RequestAccount LimitAllowed?Reject / ChallengeIP LimitAllowed?Global ProtectionAllowed?AuthenticateNoYesNoYesNoYes

### **Step 4: Do we lock the account after five attempts?**

Not necessarily.

Rate limiting and account lockout are different security mechanisms.

A permanent or long-lived account lockout can itself be abused to deny service to legitimate users.

We might instead use:

- Temporary progressive delays.
- CAPTCHA or additional verification.
- Risk-based authentication.
- Security notifications.
- Separate account recovery procedures.

We should also avoid revealing whether an account exists through different rate-limit responses.

### **Step 5: Important IP-address consideration**

When we deploy behind an API Gateway or reverse proxy, the backend may see the gateway's IP rather than the actual client IP.

We must obtain the client IP from trusted proxy metadata and configure which proxies are allowed to supply that information.

Blindly trusting an arbitrary `X-Forwarded-For` header lets attackers spoof their apparent IP address.

**Decision: Sliding Window Log for strict attempt tracking, combined with multiple identity-based security policies.**

Token Bucket can also be useful for limiting authentication request bursts, but it should not replace broader brute-force protections.

# **5. Scenario 3 — SaaS Daily and Monthly Usage Quotas**

Imagine our application has subscription plans.

```
Free:
    10,000 API requests/day

Pro:
    1,000,000 API requests/month

Enterprise:
    Custom monthly quota
```

A customer asks:

> Why can't I make requests after I've consumed my monthly allowance?

This isn't exactly the same problem as protecting our servers from sudden traffic spikes.

We're enforcing a usage entitlement.

### **Step 1: Understand the requirement**

Suppose a customer has:

```
Monthly quota = 1,000,000 requests
```

We need to count all billable requests in the relevant billing period.

We don't care how many occurred in a rolling 60-second period.

We care about total consumption during the billing cycle.

This maps naturally to Fixed Window.

### **Step 2: Define the window**

Suppose our billing cycle is a calendar month.

```
November 1, 00:00
    |
    v
November usage accumulates
    |
    v
December 1, 00:00
    |
    v
New billing period
```

We can store:

```
usage:tenant_123:2026-11

request_count = 750000
```

When a billable request arrives:

```
INCREMENT usage counter
```

If it exceeds the applicable quota, we reject or apply the relevant overage policy.

### **Step 3: A subtle difference — Usage metering versus admission**

We should distinguish:

**Usage metering**

How much did the customer consume?

**Rate limiting**

Should the customer be allowed to make this request now?

These are separate responsibilities.

For example:

```
Monthly quota:
    1,000,000 requests/month

Burst limit:
    100 requests/second

Concurrency limit:
    20 simultaneous requests
```

All three might apply simultaneously.

A customer could have 900,000 monthly requests remaining but still exceed their per-second rate limit.

Conversely, they could be making just 1 request per second but have exhausted their monthly quota.

### **Step 4: What should we store?**

For usage metering:

```
tenant_id
billing_period
consumed_units
```

For a high-value billing system, we may also retain a durable usage ledger or event records.

Redis can be useful for fast enforcement counters, but it should not automatically be treated as the authoritative billing record.

We need to think about:

- Duplicate events.
- Idempotency.
- Late-arriving usage events.
- Corrections.
- Billing boundaries.
- Data recovery.

**Decision: Fixed Window for period-based quotas, potentially combined with Token Bucket for short-term API protection.**

# **6. Scenario 4 — Calling Third-Party APIs**

This is an especially important real-world scenario.

Imagine our application integrates with an external provider.

```
Our Service
     |
     v
External Provider
```

The external provider tells us:

```
Maximum 100 requests per minute.

Maximum burst = 20 requests.
```

What should we implement?

This is where we need to be careful.

We shouldn't immediately select an algorithm based on the words "100 requests per minute."

We need to understand exactly what the provider means.

### **Step 1: Understand the provider's actual policy**

There are several possible interpretations.

**Possibility A: Fixed window**

```
100 requests per predefined minute.
```

**Possibility B: Rolling window**

```
100 requests in any 60-second period.
```

**Possibility C: Token-bucket-like policy**

```
Burst capacity = 20

Sustained refill rate = approximately
100 requests/minute.
```

These are not equivalent.

We should follow the provider's documented semantics, rather than assuming all rate limits have the same meaning.

### **Step 2: Identify the limit scope**

The provider might enforce limits:

```
Per API key

Per customer account

Per endpoint

Per IP

Across the entire organization
```

Suppose the API key has a limit of 100 requests per minute.

Our coordination identity should be:

```
provider_A:api_key_123
```

Not:

```
application_server_1
```

Because all our application servers share the same provider quota.

### **Step 3: Choose the algorithm**

Suppose the provider explicitly allows:

```
20 immediate requests

Then replenishes permission at
100 requests/minute.
```

We can model this using Token Bucket.

```
Capacity = 20

Refill rate = 100 / 60 tokens per second
```

Every outbound API call consumes one token.

If the bucket is empty, we must wait or defer work.

But if the provider instead requires smooth dispatch with no immediate burst, a paced dispatcher using Leaky Bucket-style scheduling may be more suitable.

If the provider requires a strict rolling quota, use an appropriate rolling-window limiter.

### **Step 4: What if multiple services call the same provider?**

Imagine:

\#chatgpt-mermaid-\_r_2gr\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2gr\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gr\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2gr\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2gr\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2gr\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2gr\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2gr\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2gr\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2gr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gr\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gr\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2gr\_ p{margin:0;}#chatgpt-mermaid-\_r_2gr\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2gr\_ .label text,#chatgpt-mermaid-\_r_2gr\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .node rect,#chatgpt-mermaid-\_r_2gr\_ .node circle,#chatgpt-mermaid-\_r_2gr\_ .node ellipse,#chatgpt-mermaid-\_r_2gr\_ .node polygon,#chatgpt-mermaid-\_r_2gr\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .rough-node .label text,#chatgpt-mermaid-\_r_2gr\_ .node .label text,#chatgpt-mermaid-\_r_2gr\_ .image-shape .label,#chatgpt-mermaid-\_r_2gr\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2gr\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .rough-node .label,#chatgpt-mermaid-\_r_2gr\_ .node .label,#chatgpt-mermaid-\_r_2gr\_ .image-shape .label,#chatgpt-mermaid-\_r_2gr\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2gr\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2gr\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gr\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gr\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2gr\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gr\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gr\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gr\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2gr\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2gr\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2gr\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2gr\_ .icon-shape,#chatgpt-mermaid-\_r_2gr\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2gr\_ .icon-shape p,#chatgpt-mermaid-\_r_2gr\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2gr\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2gr\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2gr\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2gr\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2gr\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2gr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2gr\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2gr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2gr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gr\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2gr\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2gr\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gr\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2gr\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2gr\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2gr\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .node rect,#chatgpt-mermaid-\_r_2gr\_ .node circle,#chatgpt-mermaid-\_r_2gr\_ .node ellipse,#chatgpt-mermaid-\_r_2gr\_ .node polygon,#chatgpt-mermaid-\_r_2gr\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2gr\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2gr\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2gr\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2gr\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2gr\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Order ServiceShared Rate CoordinatorNotification ServiceReporting ServiceExternal Provider

All three services share the same API key.

They must share the same rate-limit budget.

If each service independently creates a bucket with 20 tokens and a refill rate of 100 requests per minute, we could consume three times the permitted quota.

Therefore, our rate-limit state must be shared at the correct scope.

### **Step 5: Account for retries**

Suppose the provider responds:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
```

We should:

1. Respect the provider's retry instruction.
2. Pause requests for the applicable quota scope.
3. Reschedule the failed job.
4. Ensure retries pass through the same limiter.
5. Apply backoff and jitter to avoid synchronized retries.

**Decision: Match the provider's documented algorithm and scope. Token Bucket is excellent when the provider explicitly permits bursts. Leaky Bucket-style pacing is useful when we need smooth output.**

One final distinction: the provider's nominal request limit does not tell us its concurrency limit. We may need to control both.

# **7. Scenario 5 — Distributed Web Crawler**

Now let's apply everything to a web crawler.

We have thousands of URLs belonging to thousands of different websites.

Our crawler instances discover URLs continuously.

Example:

```
Crawler 1:
    website_A/page1
    website_B/page1

Crawler 2:
    website_A/page2
    website_C/page1

Crawler 3:
    website_A/page3
    website_B/page2
```

Different websites may have different crawl-rate constraints.

```
website_A:
    2 requests/second

website_B:
    5 requests/second

website_C:
    1 request every 2 seconds
```

### **Step 1: Identify the requirement**

We don't want to reject URLs simply because many were discovered together.

We want to store them and visit them gradually.

This is a traffic-shaping problem.

### **Step 2: Choose Leaky Bucket**

We create queues scoped to the relevant downstream identity.

```
website_A_queue
website_B_queue
website_C_queue
```

Each queue has its own processing schedule.

\#chatgpt-mermaid-\_r_2hi\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2hi\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2hi\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2hi\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2hi\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2hi\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2hi\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2hi\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2hi\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2hi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2hi\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2hi\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2hi\_ p{margin:0;}#chatgpt-mermaid-\_r_2hi\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2hi\_ .label text,#chatgpt-mermaid-\_r_2hi\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .node rect,#chatgpt-mermaid-\_r_2hi\_ .node circle,#chatgpt-mermaid-\_r_2hi\_ .node ellipse,#chatgpt-mermaid-\_r_2hi\_ .node polygon,#chatgpt-mermaid-\_r_2hi\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .rough-node .label text,#chatgpt-mermaid-\_r_2hi\_ .node .label text,#chatgpt-mermaid-\_r_2hi\_ .image-shape .label,#chatgpt-mermaid-\_r_2hi\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2hi\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .rough-node .label,#chatgpt-mermaid-\_r_2hi\_ .node .label,#chatgpt-mermaid-\_r_2hi\_ .image-shape .label,#chatgpt-mermaid-\_r_2hi\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2hi\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2hi\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2hi\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2hi\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2hi\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2hi\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2hi\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2hi\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2hi\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2hi\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2hi\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2hi\_ .icon-shape,#chatgpt-mermaid-\_r_2hi\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2hi\_ .icon-shape p,#chatgpt-mermaid-\_r_2hi\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2hi\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2hi\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2hi\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2hi\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2hi\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2hi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2hi\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2hi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2hi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2hi\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2hi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2hi\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2hi\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2hi\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2hi\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2hi\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .node rect,#chatgpt-mermaid-\_r_2hi\_ .node circle,#chatgpt-mermaid-\_r_2hi\_ .node ellipse,#chatgpt-mermaid-\_r_2hi\_ .node polygon,#chatgpt-mermaid-\_r_2hi\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2hi\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2hi\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2hi\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2hi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2hi\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}URL DiscoveryJob StoreDispatcherRate Coordinatorwebsite_A workerswebsite_B workerswebsite_C workers

For example:

```
website_A:
    Dispatch one request every 500ms.

website_B:
    Dispatch one request every 200ms.

website_C:
    Dispatch one request every 2 seconds.
```

### **Step 3: Distributed coordination**

Our crawler may run on 20 servers.

The URL for `website_A` could be processed by any of them.

We need shared coordination per host, otherwise every crawler instance might independently send two requests per second.

A suitable coordination key:

```
crawl_rate:website_A
```

It could maintain a shared next-available dispatch time, a token balance, or another appropriate scheduler state.

### **Step 4: What about robots.txt?**

A crawler should also respect applicable crawling directives, including `robots.txt` rules.

Rate limiting and crawl permissions are separate concerns.

We shouldn't assume a request is permitted merely because the rate limiter allows it.

**Decision: Leaky Bucket-style pacing with bounded per-target queues and shared rate coordination.**

Token Bucket can supplement this if the target explicitly permits bursts.

# **8. Scenario 6 — Background Job Processing**

Imagine a service receives 100,000 tasks.

We need to call an external service for every task.

Our downstream provider allows:

```
50 requests/second.
```

We don't want to reject the incoming 100,000 jobs.

We want to process them gradually.

This is another natural Leaky Bucket use case.

### **Architecture**

\#chatgpt-mermaid-\_r_2i5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2i5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2i5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2i5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2i5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2i5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2i5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2i5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2i5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2i5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2i5\_ p{margin:0;}#chatgpt-mermaid-\_r_2i5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2i5\_ .label text,#chatgpt-mermaid-\_r_2i5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .node rect,#chatgpt-mermaid-\_r_2i5\_ .node circle,#chatgpt-mermaid-\_r_2i5\_ .node ellipse,#chatgpt-mermaid-\_r_2i5\_ .node polygon,#chatgpt-mermaid-\_r_2i5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .rough-node .label text,#chatgpt-mermaid-\_r_2i5\_ .node .label text,#chatgpt-mermaid-\_r_2i5\_ .image-shape .label,#chatgpt-mermaid-\_r_2i5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2i5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .rough-node .label,#chatgpt-mermaid-\_r_2i5\_ .node .label,#chatgpt-mermaid-\_r_2i5\_ .image-shape .label,#chatgpt-mermaid-\_r_2i5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2i5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2i5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2i5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2i5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2i5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2i5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2i5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2i5\_ .icon-shape,#chatgpt-mermaid-\_r_2i5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2i5\_ .icon-shape p,#chatgpt-mermaid-\_r_2i5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2i5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2i5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2i5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2i5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2i5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2i5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2i5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2i5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2i5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2i5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2i5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2i5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2i5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2i5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2i5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .node rect,#chatgpt-mermaid-\_r_2i5\_ .node circle,#chatgpt-mermaid-\_r_2i5\_ .node ellipse,#chatgpt-mermaid-\_r_2i5\_ .node polygon,#chatgpt-mermaid-\_r_2i5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2i5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2i5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2i5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2i5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2i5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Job ProducersDurable QueueRate Controlled DispatcherWorker PoolExternal APISuccess / Failure Handler

The queue stores the actual work.

The dispatcher determines when work becomes eligible.

Workers execute it.

### **What if we use SQS?**

We can:

1. Store jobs in SQS.
2. Have workers receive jobs.
3. Check shared rate-limit state before calling the provider.
4. Defer or delay jobs that cannot yet be dispatched.
5. Acknowledge successfully processed jobs.
6. Retry failures with appropriate backoff.

However, simply retrieving a message and then sleeping is not always a good design.

It holds a worker and may complicate visibility-timeout management.

For heavy workloads, a dedicated dispatcher that schedules eligible jobs can be more effective.

### **What if we use Kafka?**

We can partition jobs by the downstream identity.

For example:

```
Kafka key = provider_account_id
```

This helps maintain ordering for a particular account within its partition.

But Kafka partition ordering alone does not enforce the provider's request rate.

We still need a paced consumer or dispatcher.

**Decision: Leaky Bucket-style scheduling, with a durable queue. Add a concurrency limit to the worker pool.**

# **9. Scenario 7 — Protecting a Database**

Imagine we have an application with a PostgreSQL database.

Our backend exposes several endpoints:

```
GET  /products
GET  /reports
POST /export
POST /analytics
```

Some queries are cheap.

Others are expensive.

For example:

```
GET /products:
    Simple indexed query.

POST /export:
    Scans millions of records.

POST /analytics:
    Executes expensive aggregations.
```

We want to prevent expensive operations from overwhelming the database.

Which algorithm should we use?

First, let's identify the actual problem.

### **Step 1: Is it request rate or concurrency?**

Imagine each analytics request takes 10 seconds.

Our system allows:

```
100 requests/second
```

This sounds like a reasonable rate.

But if every request takes 10 seconds, we could have approximately:

\\[ 100 \times 10 = 1000 \\]

concurrent operations under sustained traffic, assuming similar execution times and no other bottlenecks.

Our database might not be able to handle that many simultaneously executing queries.

So a request-rate limit alone isn't sufficient.

We need at least two controls:

```
Rate limit:
    How frequently can clients start operations?

Concurrency limit:
    How many operations can execute simultaneously?
```

### **Step 2: Introduce a concurrency limiter**

Suppose our database can safely handle 50 expensive operations concurrently.

We configure:

```
Maximum concurrent expensive operations = 50
```

When all 50 slots are occupied, additional operations must wait or be rejected.

Conceptually:

\#chatgpt-mermaid-\_r_2j4\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2j4\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2j4\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2j4\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2j4\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2j4\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2j4\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2j4\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2j4\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2j4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j4\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j4\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2j4\_ p{margin:0;}#chatgpt-mermaid-\_r_2j4\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2j4\_ .label text,#chatgpt-mermaid-\_r_2j4\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .node rect,#chatgpt-mermaid-\_r_2j4\_ .node circle,#chatgpt-mermaid-\_r_2j4\_ .node ellipse,#chatgpt-mermaid-\_r_2j4\_ .node polygon,#chatgpt-mermaid-\_r_2j4\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .rough-node .label text,#chatgpt-mermaid-\_r_2j4\_ .node .label text,#chatgpt-mermaid-\_r_2j4\_ .image-shape .label,#chatgpt-mermaid-\_r_2j4\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2j4\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .rough-node .label,#chatgpt-mermaid-\_r_2j4\_ .node .label,#chatgpt-mermaid-\_r_2j4\_ .image-shape .label,#chatgpt-mermaid-\_r_2j4\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2j4\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2j4\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j4\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j4\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2j4\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2j4\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2j4\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2j4\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2j4\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2j4\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2j4\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2j4\_ .icon-shape,#chatgpt-mermaid-\_r_2j4\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2j4\_ .icon-shape p,#chatgpt-mermaid-\_r_2j4\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2j4\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2j4\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2j4\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2j4\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2j4\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2j4\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j4\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2j4\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2j4\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2j4\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2j4\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2j4\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2j4\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .node rect,#chatgpt-mermaid-\_r_2j4\_ .node circle,#chatgpt-mermaid-\_r_2j4\_ .node ellipse,#chatgpt-mermaid-\_r_2j4\_ .node polygon,#chatgpt-mermaid-\_r_2j4\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2j4\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2j4\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2j4\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2j4\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2j4\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming RequestToken BucketRate permitted?RejectConcurrency LimiterExecution slot available?Wait or RejectExecute Database QueryRelease SlotNoYesNoYes

### **Step 3: Should we also use weighted request costs?**

Yes.

For example:

```
Simple indexed query = 1 token

Report generation = 10 tokens

Expensive analytics = 20 tokens
```

We could use Token Bucket to control the resource budget.

But if the real concern is simultaneous database pressure, concurrency remains important.

### **Step 4: What should we store?**

For a distributed concurrency limiter, we need to track active reservations or leases.

For example:

```
db:expensive_queries

active_operations:
    operation_001
    operation_002
    operation_003
```

Each reservation needs an identifier, expiration or lease information, and a safe release mechanism.

We also need recovery when a server crashes without releasing its reservation.

This is a different state-management problem from maintaining a token balance.

**Decision: Token Bucket for admission rate or weighted resource usage, combined with a distributed concurrency limiter for database protection.**

The important lesson:

A database can be overwhelmed by a small number of very expensive concurrent operations, even when the incoming request rate looks reasonable.

# **10. Scenario 8 — AI/LLM API Usage**

Imagine we're building an AI application.

Users can perform:

```
Simple chat completion
Document summarization
Large document analysis
Embedding generation
Agentic workflows
```

These operations have very different costs.

Suppose we only limit:

```
100 requests/minute per user.
```

Is that sufficient?

No.

Consider these two users:

```
User A:
    100 requests
    Each generates 50 tokens.

User B:
    100 requests
    Each generates 20,000 tokens.
```

Both consume the same number of requests, but their resource consumption is very different.

We need more meaningful controls.

### **Step 1: Separate the dimensions**

We might have:

```
Requests per minute (RPM)

Input tokens per minute (TPM)

Output tokens per minute (TPM)

Concurrent model requests

Daily or monthly usage budget
```

These are different constraints.

### **Step 2: Use weighted Token Bucket**

We can assign token costs based on the operation.

For example:

```
Simple completion:
    Cost based on estimated input/output tokens.

Large summarization:
    Higher cost.

Embedding generation:
    Cost based on input token volume.
```

A token bucket can represent a computational or usage budget.

But we need to distinguish two meanings of "tokens":

1. Rate-limiter tokens, which are our internal accounting units.
2. Actual LLM tokens, which are model input/output tokens.

They are not automatically the same thing, even though we can choose to make them correspond.

For example:

```
Rate limiter:
    1 internal credit = 100 estimated model tokens
```

We may reserve estimated cost before execution and reconcile it against actual usage afterward, depending on the quota semantics.

### **Step 3: Combine independent policies**

A request might need to pass:

```
User RPM bucket

Tenant TPM bucket

Global provider RPM bucket

Model concurrency limit

Monthly spending budget
```

Why so many?

Because they protect different things.

- RPM protects request throughput.
- TPM protects token throughput.
- Concurrency protects active execution capacity.
- Monthly budget protects spending.

### **Step 4: Important production distinction**

Suppose an application uses an external model provider.

The provider may impose limits at the organization, project, model, or API-key level.

Our own internal rate limiter must not assume that the provider's limit is identical to our tenant's limit.

We need a separate shared outbound limiter matching the provider's quota scope.

**Decision: Weighted Token Bucket for resource budgets, separate request-rate limits, concurrency limits, and durable usage metering.**

This is a particularly good example of why real systems often require several kinds of limits together.

# **11. Scenario 9 — Protecting an Entire Service During Traffic Spikes**

Now imagine we operate a backend service.

Our normal traffic is:

```
1,000 requests/second.
```

Suddenly, due to a viral event, it becomes:

```
100,000 requests/second.
```

We want to protect the entire service.

Should we create one global rate limiter?

Not necessarily.

A single limiter may become a bottleneck itself.

We should think in layers.

### **Layer 1: Per-client protection**

For example:

```
100 requests/second per API key.
```

A Token Bucket works well here if bursts are acceptable.

### **Layer 2: Per-tenant protection**

For example:

```
1,000 requests/second per tenant.
```

This ensures that one large customer cannot consume the entire service capacity.

### **Layer 3: Global admission protection**

For example:

```
Maximum aggregate admission rate = 20,000 requests/second.
```

This protects the overall application.

### **Layer 4: Concurrency protection**

For example:

```
Maximum 2,000 active expensive operations.
```

This prevents the service from accumulating too many in-flight operations.

### **Layer 5: Load shedding**

During severe overload, we might reject low-priority or optional work.

We shouldn't necessarily treat all requests equally.

For example:

```
Critical payment operations:
    High priority.

Normal product browsing:
    Normal priority.

Optional analytics:
    Lower priority.
```

We can apply priority-aware admission, provided we preserve fairness and essential service guarantees.

Architecture:

\#chatgpt-mermaid-\_r_2lb\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2lb\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2lb\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2lb\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2lb\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2lb\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2lb\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2lb\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2lb\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2lb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2lb\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2lb\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2lb\_ p{margin:0;}#chatgpt-mermaid-\_r_2lb\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2lb\_ .label text,#chatgpt-mermaid-\_r_2lb\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .node rect,#chatgpt-mermaid-\_r_2lb\_ .node circle,#chatgpt-mermaid-\_r_2lb\_ .node ellipse,#chatgpt-mermaid-\_r_2lb\_ .node polygon,#chatgpt-mermaid-\_r_2lb\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .rough-node .label text,#chatgpt-mermaid-\_r_2lb\_ .node .label text,#chatgpt-mermaid-\_r_2lb\_ .image-shape .label,#chatgpt-mermaid-\_r_2lb\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2lb\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .rough-node .label,#chatgpt-mermaid-\_r_2lb\_ .node .label,#chatgpt-mermaid-\_r_2lb\_ .image-shape .label,#chatgpt-mermaid-\_r_2lb\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2lb\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2lb\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2lb\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2lb\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2lb\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2lb\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2lb\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2lb\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2lb\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2lb\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2lb\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2lb\_ .icon-shape,#chatgpt-mermaid-\_r_2lb\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2lb\_ .icon-shape p,#chatgpt-mermaid-\_r_2lb\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2lb\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2lb\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2lb\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2lb\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2lb\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2lb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2lb\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2lb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2lb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2lb\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2lb\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2lb\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2lb\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2lb\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2lb\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2lb\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .node rect,#chatgpt-mermaid-\_r_2lb\_ .node circle,#chatgpt-mermaid-\_r_2lb\_ .node ellipse,#chatgpt-mermaid-\_r_2lb\_ .node polygon,#chatgpt-mermaid-\_r_2lb\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2lb\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2lb\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2lb\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2lb\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2lb\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming TrafficPer-client LimiterPer-tenant LimiterGlobal Admission LimiterConcurrency LimiterBackend ServicesDatabase

**Decision: Combine Token Bucket admission, multiple scoped quotas, concurrency protection, and load shedding.**

This is more realistic than expecting one algorithm to solve every overload problem.

# **12. The most important concept: Combining algorithms**

Let's pause.

You may have noticed something.

In our real-world examples, we often needed multiple mechanisms.

This is normal.

A production system rarely has only one rate limit.

Let's work through a complete example.

Imagine we have an API platform.

We need to enforce:

```
1. 100 requests/second per user.

2. 1,000 requests/second per tenant.

3. 20,000 requests/second globally.

4. Maximum 500 concurrent expensive operations.

5. 1 million requests per month per tenant.
```

What should we use?

| Requirement                         | Mechanism                                          | Reason                             |
| ----------------------------------- | -------------------------------------------------- | ---------------------------------- |
| Per-user sustained rate with bursts | Token Bucket                                       | Controls per-user consumption.     |
| Tenant sustained rate with bursts   | Token Bucket                                       | Protects tenant fairness.          |
| Global request protection           | Token Bucket or another global admission mechanism | Protects overall system capacity.  |
| Concurrent expensive operations     | Concurrency limiter                                | Controls in-flight work.           |
| Monthly usage quota                 | Fixed Window or durable usage metering             | Tracks billing-period consumption. |

The request-processing decision becomes:

```
Incoming Request
       |
       v
Authenticate
       |
       v
Check Monthly Quota
       |
       v
Check User Bucket
       |
       v
Check Tenant Bucket
       |
       v
Check Global Budget
       |
       v
Check Concurrency Availability
       |
       v
Execute Operation
       |
       v
Release Concurrency Slot
       |
       v
Return Response
```

There is one subtle but important implementation requirement.

**All applicable rate-limit budgets should be consumed consistently.**

Suppose:

```
User bucket:
    Has 10 tokens.

Tenant bucket:
    Has 0 tokens.
```

If we deduct one token from the user bucket and subsequently discover that the tenant bucket is empty, the user loses a token even though the request was rejected.

That may be undesirable.

We should design the admission operation so that checking and deducting multiple rate-limit budgets is atomic where practical.

For Redis, colocating the relevant keys in one cluster hash slot allows a Lua script to perform coordinated updates.

For multiple stores or independent regions, this becomes a more complicated distributed coordination problem.

Concurrency reservations also need their own lifecycle management.

We will study these challenges in Chapter 7.

# **13. Common mistakes when choosing rate-limiting algorithms**

These are worth understanding because they frequently lead to flawed system designs.

### **Mistake 1: Thinking every rate limit means requests per minute**

Not every requirement is about a fixed time window.

Compare:

```
100 requests per minute.
```

With:

```
2 requests every second, smoothly.
```

And:

```
Allow a burst of 100,
then replenish at 10 per second.
```

These requirements are different.

The first might use a window counter.

The second suggests pacing with Leaky Bucket.

The third suggests Token Bucket.

### **Mistake 2: Assuming Token Bucket enforces a strict rolling quota**

Suppose:

```
Capacity = 100
Refill rate = 10 tokens/second
```

A client may spend the initial 100 tokens and then consume another 10 tokens per second.

Therefore, the maximum allowed usage over 10 seconds, starting with a full bucket, can reach:

```
100 + 10 * 10 = 200 tokens
```

It does not enforce 100 requests in every rolling 10-second period.

### **Mistake 3: Using Leaky Bucket for synchronous requests without considering latency**

Suppose a user submits a request and we put it in a queue.

It may wait 30 seconds.

Is that acceptable?

For background jobs, often yes.

For an interactive API, often no.

Sometimes immediate rejection is better.

### **Mistake 4: Confusing rate limiting with concurrency limiting**

These are different:

```
Maximum 100 requests per second.

Maximum 20 simultaneous requests.
```

A system can obey the first and still have thousands of active operations.

We often need both.

### **Mistake 5: Giving every application instance its own global budget**

Suppose:

```
Desired global limit = 1,000 requests/second.

Application instances = 10.
```

If each instance independently allows 1,000 requests per second, the actual aggregate allowance becomes:

```
10 * 1,000 = 10,000 requests/second.
```

We need shared coordination or carefully partitioned quotas.

### **Mistake 6: Ignoring retries**

Suppose a downstream service is failing.

Our application retries failed operations.

If every retry bypasses the limiter, we can create a retry storm.

Every actual outbound attempt must respect the relevant rate limit.

### **Mistake 7: Applying only per-user limits**

A system can still be overwhelmed by many individually compliant users.

We need to consider tenant-level and global limits too.

### **Mistake 8: Ignoring how the limit is scoped**

A provider might have a shared API-key quota.

Creating independent buckets per worker defeats the purpose.

Always identify the actual quota identity.

# **14. Quick algorithm selection reference**

Use this table as a practical reference when solving system design problems.

| Real-world requirement                       | Recommended approach                                |
| -------------------------------------------- | --------------------------------------------------- |
| API requests per calendar day                | Fixed Window                                        |
| API requests per billing month               | Fixed Window / usage metering                       |
| Strict requests in any rolling 60 seconds    | Sliding Window Log                                  |
| Approximate rolling API quotas at high scale | Sliding Window Counter                              |
| Public API with burst allowance              | Token Bucket                                        |
| Per-user and per-tenant burst protection     | Multiple Token Buckets                              |
| Smooth outbound third-party requests         | Leaky Bucket / paced dispatcher                     |
| Web crawler respecting host-specific pacing  | Leaky Bucket-style scheduling                       |
| Background job processing                    | Durable queue + Leaky Bucket-style scheduling       |
| Expensive API operations                     | Weighted Token Bucket                               |
| Simultaneous database query limits           | Concurrency limiter                                 |
| AI request and token budgets                 | Weighted Token Bucket + usage metering              |
| Protect the entire backend                   | Global admission control + concurrency limiter      |
| Strict provider rolling quota                | Sliding Window Log or provider-compatible mechanism |

## **15. How to approach any rate-limiting system design interview**

Now let's turn everything into a reusable workflow.

Imagine an interviewer says:

> Design a distributed rate-limiting service.

Don't immediately start drawing Redis and API servers.

First establish the requirements.

### **Step 1: Identify what is being protected**

Ask:

```
Are we protecting:

- Our own backend?
- A database?
- An external API?
- A particular tenant?
- A particular user?
- An entire service?
```

### **Step 2: Identify the identity/scope**

Ask:

```
What should the limit apply to?

- User ID?
- API key?
- IP address?
- Tenant ID?
- External provider account?
- Global service?
```

### **Step 3: Understand the limit semantics**

Ask:

```
Is it:

- Requests per fixed period?
- Requests in any rolling period?
- Average rate with a burst allowance?
- Smooth outgoing traffic?
- Maximum concurrent operations?
- Resource consumption rather than request count?
```

### **Step 4: Select the algorithm**

Choose based on the actual behavior:

```
Fixed Window
Sliding Window Log
Sliding Window Counter
Leaky Bucket
Token Bucket
Concurrency Limiter
```

Remember that multiple mechanisms may be needed.

### **Step 5: Determine the state**

Ask:

```
What information does this algorithm need?

Where should it be stored?

Does it need shared storage?

How long should it be retained?
```

### **Step 6: Handle concurrency**

Ask:

```
What happens when multiple application instances
receive requests for the same identity simultaneously?

How do we ensure that requests don't consume
the same available capacity?
```

Think about atomic Redis operations, Lua scripts, and distributed coordination.

### **Step 7: Handle failures**

Ask:

```
What if Redis is unavailable?

What if a worker crashes?

What if the downstream returns HTTP 429?

What if the queue becomes full?

What if retries create additional traffic?
```

### **Step 8: Think about scale**

Ask:

```
How many identities are active?

How many rate-limit checks per second?

How many Redis operations?

Is a single Redis instance sufficient?

Do we need Redis Cluster?

What happens across multiple regions?
```

This workflow works for most rate-limiting system design problems.

# **16. The complete story so far**

Let's look at the evolution we've gone through.

\#chatgpt-mermaid-\_r_2nu\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2nu\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2nu\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2nu\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2nu\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2nu\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2nu\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2nu\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2nu\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2nu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nu\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nu\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2nu\_ p{margin:0;}#chatgpt-mermaid-\_r_2nu\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2nu\_ .label text,#chatgpt-mermaid-\_r_2nu\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .node rect,#chatgpt-mermaid-\_r_2nu\_ .node circle,#chatgpt-mermaid-\_r_2nu\_ .node ellipse,#chatgpt-mermaid-\_r_2nu\_ .node polygon,#chatgpt-mermaid-\_r_2nu\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .rough-node .label text,#chatgpt-mermaid-\_r_2nu\_ .node .label text,#chatgpt-mermaid-\_r_2nu\_ .image-shape .label,#chatgpt-mermaid-\_r_2nu\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2nu\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .rough-node .label,#chatgpt-mermaid-\_r_2nu\_ .node .label,#chatgpt-mermaid-\_r_2nu\_ .image-shape .label,#chatgpt-mermaid-\_r_2nu\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2nu\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2nu\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nu\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nu\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2nu\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2nu\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2nu\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2nu\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2nu\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2nu\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2nu\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2nu\_ .icon-shape,#chatgpt-mermaid-\_r_2nu\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2nu\_ .icon-shape p,#chatgpt-mermaid-\_r_2nu\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2nu\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2nu\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2nu\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2nu\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2nu\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2nu\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nu\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2nu\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2nu\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2nu\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2nu\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2nu\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2nu\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .node rect,#chatgpt-mermaid-\_r_2nu\_ .node circle,#chatgpt-mermaid-\_r_2nu\_ .node ellipse,#chatgpt-mermaid-\_r_2nu\_ .node polygon,#chatgpt-mermaid-\_r_2nu\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2nu\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2nu\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2nu\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2nu\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2nu\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}We need to control requestconsumptionFixed WindowBoundary burst problemSliding Window LogToo much timestamp storageSliding Window CounterStill only controls admissionLeaky BucketOutgoing traffic is smoothbut bursts are restrictedToken BucketControlled sustainedconsumption with burstallowance

Each algorithm emerged because we wanted to solve a particular limitation.

Here's what I want you to retain:

| Algorithm              | Your mental model                                        |
| ---------------------- | -------------------------------------------------------- |
| Fixed Window           | Count requests in a fixed period.                        |
| Sliding Window Log     | Remember exactly when requests happened.                 |
| Sliding Window Counter | Approximate request history using two counters.          |
| Leaky Bucket           | Buffer work and release it gradually.                    |
| Token Bucket           | Accumulate permission and spend it when requests arrive. |

You should now be able to derive each algorithm from its underlying behavior.

# **Next Chapter — Chapter 7: Building a Distributed Rate Limiter from Scratch**

This is where we bring everything together and build an actual distributed service.

So far, we've discussed Redis implementations of individual algorithms.

Now we need to design the complete system around them.

We'll start with a simple rate limiter running inside one application instance, then evolve it into a distributed service.

The chapters within that design will cover:

| Section | Topic                                                             |
| ------- | ----------------------------------------------------------------- |
| 1       | Why an in-memory rate limiter fails when we have multiple servers |
| 2       | Introducing Redis as shared state                                 |
| 3       | Designing the Rate Limiter service and request flow               |
| 4       | Redis data models for all five algorithms                         |
| 5       | Atomicity, Lua scripts, and concurrent requests                   |
| 6       | Redis Cluster, sharding, and hot keys                             |
| 7       | Multiple simultaneous policies: user, tenant, endpoint, global    |
| 8       | Application-side local caching and distributed counters           |
| 9       | Fail-open vs fail-closed behavior                                 |
| 10      | Redis failures, retries, and operational reliability              |
| 11      | Multi-region rate limiting                                        |
| 12      | Monitoring, metrics, HTTP headers, and production deployment      |
| 13      | Complete architecture and interview-ready design                  |

One particularly important question we'll investigate is:

> How can we enforce a global limit of 1,000 requests per second when requests are distributed across 20 application instances, potentially across multiple regions, without turning Redis into a bottleneck?

That is where the algorithms meet real distributed-systems engineering.
