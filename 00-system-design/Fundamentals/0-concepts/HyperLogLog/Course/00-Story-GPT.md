# HyperLogLog — Complete Course

Lesson 1 · Fundamentals and intuition

HyperLogLog is a particularly useful topic for backend engineering, distributed systems, database internals, system design, and technical interviews.

We'll learn not just what it does, but why it works, how it is implemented, and how to explain it confidently in an interview.

## Course roadmap

## 01

Fundamentals and intuition&#x20;

Current lesson



The distinct-counting problem, trade-offs, and real-world applications.

## 02

Hashing and probability

Why binary hashes contain clues about the number of unique elements.

## 03

Buckets and registers

How the data structure processes each element and stores a tiny summary.

## 04

The estimation algorithm

The harmonic mean, estimation formula, and corrections.

## 05

Accuracy and memory

Precision settings, standard error, and memory calculations.

## 06

Merging and distributed systems

Combining summaries across servers, time windows, and data partitions.

## 07

Implementation and production usage

Redis, Java examples, limitations, testing, and implementation choices.

## 08

Interview and system-design questions

Comparisons, common pitfalls, and practical design scenarios.

# Lesson 1: What is HyperLogLog?

## 1. The problem: counting unique elements

Imagine you're building an analytics service for a website.

You receive these visitor events:

```
Alice
Bob
Alice
Charlie
Bob
David
Alice
```

The total number of events is 7.

But the number of unique visitors is only 4:

```
Alice, Bob, Charlie, David
```

This is called cardinality estimation: estimating the number of distinct elements in a collection.

In set notation, cardinality means the number of elements in a set:

\\[ |S| = \text{number of distinct elements in } S \\]

For the example above:

\\[ S = \\{\text{Alice, Bob, Charlie, David}\\} \\]

\\[ |S| = 4 \\]

This sounds simple. However, counting distinct elements accurately becomes expensive when a system processes billions of events.

## 2. The traditional solution: HashSet

One straightforward approach is to store every unique visitor in a `HashSet`.

```
Set<String> uniqueVisitors = new HashSet<>();

uniqueVisitors.add("Alice");
uniqueVisitors.add("Bob");
uniqueVisitors.add("Alice");
uniqueVisitors.add("Charlie");
uniqueVisitors.add("Bob");
uniqueVisitors.add("David");
uniqueVisitors.add("Alice");

System.out.println(uniqueVisitors.size()); // 4
```

A Java `HashSet` avoids adding duplicate elements, so its size gives the exact distinct count.

### What happens at large scale?

Suppose your service receives events from 100 million unique users.

The set must retain information about those users so it can distinguish an already-seen user from a new one.

Exact counting with a HashSet

100 million unique user IDs

Individual identities and supporting hash-table data

# 100,000,000

Exact unique-user count

The actual memory requirement depends on string lengths, object overhead, table layout, and the JVM. It grows with the number of unique users.

A `HashSet` is still an excellent solution when exactness matters and the data fits comfortably in memory.

But some analytics workloads don't require an exact answer.

For example, a dashboard might display 100.2 million unique visitors instead of exactly 100,198,457.

That creates an opportunity to trade a small amount of accuracy for a large reduction in memory.

## 3. Enter HyperLogLog

HyperLogLog (HLL) is a probabilistic data structure that estimates the number of distinct elements using a compact summary rather than storing all the elements. It was introduced in a paper by Philippe Flajolet and co-authors in 2007.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://algo.inria.fr\&sz=32)

algo.inria.fr+1



Instead of remembering every visitor, it processes each visitor and updates a small collection of internal counters called registers.

The result is an estimate, not an exact count.

Incoming stream

`Alice → Bob → Alice → Charlie → Bob → David → ...`

Hash each element and update a register

Compact HLL summary

### 2

### 1

### 4

### 3

### 2

### 5

### 1

### 3

### 2

### 4

Illustrative register values only; not the actual encoding of the visitor stream above.

Estimated distinct visitors

# ≈ 4

The key idea is that HyperLogLog doesn't need to know who the visitors are. It retains statistical information from their hash values that helps estimate how many distinct visitors produced those values.

We'll derive exactly how this works in Lessons 2–4.

## 4. How much memory does HyperLogLog use?

A particularly important property is that its memory usage is governed mainly by its configured precision, rather than growing proportionally to the number of unique items observed.

For example, Redis documents an HLL implementation with a maximum memory usage of about 12 KB and a standard error of 0.81%. Actual usage can be lower for small cardinalities.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



Compare the general behavior:

| Property                         | `HashSet`                        | HyperLogLog                               |
| -------------------------------- | -------------------------------- | ----------------------------------------- |
| Distinct count                   | Exact                            | Approximate                               |
| Memory as unique items grow      | Grows with the set               | Bounded for a fixed configuration         |
| Need to retain item identities   | Yes                              | No                                        |
| Duplicate events                 | Ignored by set semantics         | Handled by the estimator's update process |
| Retrieve individual unique items | Possible                         | Not possible                              |
| Merge summaries from servers     | Requires set union or equivalent | Supported efficiently                     |

The trade-off is straightforward: a `HashSet` provides exact results and access to elements, whereas HyperLogLog provides a compact summary for estimating cardinality.

## 5. What does “probabilistic” actually mean?

It does not mean HyperLogLog randomly guesses a number.

The algorithm applies a hash function to each input and extracts patterns from the resulting bits. Under appropriate hash-distribution assumptions, those patterns have predictable statistical behavior.

The algorithm combines information from many observations to produce an estimate.

For instance, an HLL might estimate the count of a dataset whose true cardinality is 1,000,000 as 996,500 on one dataset and 1,004,000 on another. These are illustrative values, not guaranteed bounds.

The accuracy depends on its configuration and statistical variation.

Two important distinctions:

- Standard error is not a maximum-error guarantee. Some estimates will be farther from the true value than the standard error.
- More precision generally requires more memory. HLL's accuracy improves as you allocate more registers.&#x20;

  [image](https://www.google.com/s2/favicons?domain=https://algo.inria.fr\&sz=32)

  algo.inria.fr+1



We'll quantify these relationships later.

## 6. Where is HyperLogLog useful?

[We launched landingpagesexplained.com on Product Hunt two months ago.  Fast forward to today, and it still gets 100-200+ visits per day and is a consistent traffic source to senja.io  Launch more… | Wilson Wilson](https://images.openai.com/static-rsc-4/vNSpprAjowrihDxEv7jKPM39J-QTsN0O8y-wvCampJ-RpdfzSdVDUX9JXT9fIyaeS2cCbCwwl6V0xRXntPWhoPsnrl1qpWWqUAuA7wn9asJVI_wx9JBpaWXdWpsn_szzXJMkragcAcyLLlw6C8rwklWNzSsnTKoiYZizEOY32Z8?purpose=inline)

Website analytics

Estimate daily, weekly, or monthly unique visitors from a very large stream of page-view events.

[Microsoft, Amazon, and Google operate half the world’s 600 hyperscale data centers - DCD](https://images.openai.com/static-rsc-4/5IkdFdBYX5x3dnbJrXseluzg55GQtRxEQElpRp1UfMMkvABfBxmyNIN9VA8YRoRoup2tzAHDjtrHlkeONmgwdIMGR1yKwnurYwjT4OQb3C95unPCRqMWCKn1_5sr27FR-AnzJ80zi-xYJJSeQX8b1xrl2R290yuZH7NwEPYasEA?purpose=inline)

Distributed data processing

Each server maintains a summary of the distinct users it sees. Those summaries can be merged to estimate the global count without transferring every user ID.

[FlowStats - Advanced NetFlow Diagnostics](https://images.openai.com/static-rsc-4/j4t_YvETsXHQslQwvtghQbDz6RUl8S1yUeEbgWs4NOOrUMJMI94wGS8tWIRCk_qROboK2Re3D8lrf0E3UTHud6eJCpz44ca1xeCyPPm_AU0ZzPTkKuvkt8BXS-gZ0OIqcIMvGuyA7R3u_e4qGkdyC_319S6pPU8AXzuCfp9asb8?purpose=inline)

Network monitoring

Estimate distinct IP addresses, clients, or other identifiers observed during a measurement period.

These are good fits when approximate counts are acceptable and the cardinality is large. For billing, inventory, or business rules requiring an exact count, you should generally use an exact method instead.

## 7. HyperLogLog versus related data structures

A common interview question is why we need HLL when other compact probabilistic structures exist.

| Data structure   | Primary purpose                                                  |
| ---------------- | ---------------------------------------------------------------- |
| `HashSet`        | Store distinct values and count them exactly                     |
| Bloom filter     | Test set membership approximately, with possible false positives |
| Count-Min Sketch | Estimate item frequencies, such as how often a product appears   |
| HyperLogLog      | Estimate the number of distinct values                           |

Notice the difference between counting occurrences and counting distinct items.

Given this stream:

```
Apple, Apple, Apple, Banana, Orange, Banana
```

A frequency-counting system may tell you that Apple appeared three times.

HyperLogLog focuses on the number of different values:

```
Apple, Banana, Orange
```

So the estimated cardinality is approximately 3, regardless of the number of repeated events.

## 8. Interview-ready answer

Question: What is HyperLogLog, and why do we use it?

A strong answer would be:

> HyperLogLog is a probabilistic data structure used to estimate the cardinality, or number of distinct elements, in a large dataset. Instead of storing all unique elements like a HashSet, it maintains a compact set of registers updated using hashed values. This gives it bounded memory usage for a fixed precision, even as the dataset grows. The trade-off is that its answer is an estimate rather than an exact count. It is especially useful for analytics and distributed systems where low memory usage matters more than perfect accuracy.

That answer covers the purpose, mechanism at a high level, advantage, trade-off, and applications.

## Check your understanding

1\. A website receives 10 million events from 2 million unique users. What does HLL estimate?

10 million total events

2 million distinct users

Both values exactly

2\. Why is HLL more memory-efficient than a HashSet for large cardinalities?

It compresses and retains every original ID

It keeps a compact statistical summary instead of all IDs

It simply ignores duplicate events without hashing

3\. Can you use a standard HLL to retrieve the list of unique visitors?

Yes, it retains every distinct visitor

No, it estimates the count but doesn't retain their identities

Lesson 1 takeaway: HyperLogLog solves a specific problem: estimating the number of unique items with very little memory. The next lesson explains the central insight behind the algorithm — how random-looking hash bits reveal information about the number of distinct elements.

Say next when you're ready for Lesson 2.






















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Lesson 2: Hashing and Probability

In Lesson 1, we learned that HyperLogLog estimates the number of unique elements without storing every element.

Now let's understand the central idea behind how it works.

## 1. What is hashing?

A hash function takes an input and produces a fixed-size value.

For example, imagine a hash function that produces a 32-bit binary number:

```
Input       -> Hash value

Alice       -> 101101001010...
Bob         -> 001011101101...
Charlie     -> 000101110010...
David       -> 110100010011...
```

These are illustrative hash values, not actual outputs from a particular hash function.

Three properties matter for HyperLogLog:

1. The same input should produce the same hash value when using the same hash function and configuration.
2. Different inputs should produce hash values that are distributed approximately uniformly across the possible bit patterns.
3. Hash values should be sufficiently wide that collisions are rare for the expected dataset size.

A collision occurs when two different inputs produce the same hash value.

For a good hash function, we want collisions to be rare enough that they don't materially affect our estimate.

## 2. Why do binary hash values help us estimate cardinality?

Look at these binary numbers:

```
A: 101101...
B: 011010...
C: 001101...
D: 000101...
E: 000001...
```

Count the number of leading zeros before the first `1`.

```
A: 101101...  -> 0 leading zeros
B: 011010...  -> 1 leading zero
C: 001101...  -> 2 leading zeros
D: 000101...  -> 3 leading zeros
E: 000001...  -> 5 leading zeros
```

For a uniformly distributed hash, each bit is approximately equally likely to be `0` or `1`.

Therefore:

- The probability of one leading zero is \\(1/2\\).
- The probability of at least two leading zeros is \\(1/4\\).
- The probability of at least three leading zeros is \\(1/8\\).
- The probability of at least four leading zeros is \\(1/16\\).
- The probability of at least eight leading zeros is \\(1/256\\).

In general:

\\[ P(\text{at least } k \text{ leading zeros}) = \frac{1}{2^k} \\]

This is the key insight.

A hash value with many leading zeros is increasingly unlikely to appear in a small collection of independent, uniformly distributed hash values.

Therefore, observing such a value gives us statistical information about how many values we've processed.

## 3. A worked example

Suppose we process unique users and get these hash patterns:

```
User A -> 10101010
User B -> 11100101
User C -> 00101110
User D -> 00010111
User E -> 11010010
User F -> 00000101
```

Let's calculate the leading zeros:

```
User A -> 10101010 -> 0
User B -> 11100101 -> 0
User C -> 00101110 -> 2
User D -> 00010111 -> 3
User E -> 11010010 -> 0
User F -> 00000101 -> 5
```

The maximum number of leading zeros observed is 5.

What does this tell us?

Since a hash beginning with five zeros has probability \\(1/2^5 = 1/32\\), observing such a pattern suggests that we've processed enough values for a relatively rare event to occur.

A rough intuition is:

\\[ \text{Estimated cardinality} \approx 2^R \\]

where \\(R\\) is the maximum observed number of leading zeros.

Here:

\\[ 2^5 = 32 \\]

This suggests a scale of approximately 32 items, not an exact answer.

Important: This is an introductory intuition, not the HyperLogLog estimation formula. The actual number in our example is only six, and the maximum alone is a noisy estimator.

## 4. Why can't we use only the maximum number of leading zeros?

Consider two collections.

Collection A:

```
00101100
10101010
11101001
00011010
```

Collection B:

```
00000101
00101110
00010111
00001100
10101010
11100101
```

Both collections have a maximum of five leading zeros in this example, but Collection B contains more values.

The maximum alone does not adequately describe the collection.

A lucky hash could contain many leading zeros even when the collection is small. Conversely, a large collection may not yet contain an unusually long run of zeros.

This is statistical variation.

HyperLogLog addresses this problem by splitting the hashes across multiple groups, called buckets, and keeping information about each group separately. It then combines information across those groups instead of relying on one global maximum.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://dmtcs.episciences.org\&sz=32)

Discrete Math & Theoretical CS+1



We'll examine the bucket mechanics in the next lesson.

## 5. Understanding the probability more precisely

Suppose each hash has a probability of \\(1/256\\) of containing at least eight leading zeros.

If we process 256 independent, uniformly distributed hash values, the expected number of hashes with at least eight leading zeros is:

\\[ 256 \times \frac{1}{256} = 1 \\]

Notice the wording: expected number, not guaranteed number.

The probability of observing at least one such hash is:

\\[ P(\text{at least one}) = 1-\left(1-\frac{1}{256}\right)^{256} \\]

Approximately:

\\[ P(\text{at least one}) \approx 63.3\\% \\]

So even after processing 256 items, there is still roughly a 36.7% chance that none of their hashes has eight or more leading zeros.

This is why one hash pattern isn't enough to count accurately.

HyperLogLog uses many registers to reduce the influence of individual lucky or unlucky observations.

## 6. How the actual HyperLogLog algorithm uses hashes

So far, we've considered the leading zeros in the entire hash. Now let's introduce the next step.

A hash can be divided into two parts:

```
Example hash:  00101101
               |------|
                 split
```

The first part selects a bucket, and the remaining bits provide the pattern used to update that bucket's register.

Conceptually:

\#chatgpt-mermaid-\_r_1fg\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1fg\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1fg\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1fg\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1fg\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1fg\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1fg\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1fg\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1fg\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1fg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1fg\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1fg\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1fg\_ p{margin:0;}#chatgpt-mermaid-\_r_1fg\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1fg\_ .label text,#chatgpt-mermaid-\_r_1fg\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .node rect,#chatgpt-mermaid-\_r_1fg\_ .node circle,#chatgpt-mermaid-\_r_1fg\_ .node ellipse,#chatgpt-mermaid-\_r_1fg\_ .node polygon,#chatgpt-mermaid-\_r_1fg\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .rough-node .label text,#chatgpt-mermaid-\_r_1fg\_ .node .label text,#chatgpt-mermaid-\_r_1fg\_ .image-shape .label,#chatgpt-mermaid-\_r_1fg\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1fg\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .rough-node .label,#chatgpt-mermaid-\_r_1fg\_ .node .label,#chatgpt-mermaid-\_r_1fg\_ .image-shape .label,#chatgpt-mermaid-\_r_1fg\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1fg\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1fg\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1fg\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1fg\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1fg\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1fg\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1fg\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1fg\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1fg\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1fg\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1fg\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1fg\_ .icon-shape,#chatgpt-mermaid-\_r_1fg\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1fg\_ .icon-shape p,#chatgpt-mermaid-\_r_1fg\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1fg\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1fg\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1fg\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1fg\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1fg\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1fg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1fg\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1fg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1fg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1fg\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1fg\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1fg\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1fg\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1fg\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1fg\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1fg\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .node rect,#chatgpt-mermaid-\_r_1fg\_ .node circle,#chatgpt-mermaid-\_r_1fg\_ .node ellipse,#chatgpt-mermaid-\_r_1fg\_ .node polygon,#chatgpt-mermaid-\_r_1fg\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1fg\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1fg\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1fg\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1fg\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1fg\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Input elementHash the elementSplit hash into two partsFirst part selects bucketRemaining bits determinerankRead bucket registerCalculate rankKeep the maximum rank

Each register remembers the maximum rank it has observed from the hashes assigned to that register.

For now, remember three terms:

- Hash: The binary value produced from an input.
- Bucket: The group selected by part of the hash.
- Rank: A value derived from the zero pattern in the remaining hash bits. Common implementations define the rank as the number of leading zeros plus one.

The detailed mechanics of selecting buckets and updating registers are the subject of Lesson 3.

## 7. Lesson 2 summary

The central reasoning is:

1. A good hash function produces approximately uniformly distributed bits.
2. A pattern with \\(k\\) leading zeros occurs with probability \\(2^{-k}\\).
3. Rare patterns provide statistical information about how many distinct values have been processed.
4. Using only the rarest pattern is too noisy for reliable counting.
5. HyperLogLog divides the hash space into buckets, records a maximum rank for each bucket, and combines these observations to estimate cardinality.

The original HyperLogLog research builds on this probabilistic idea to obtain accurate cardinality estimates with very compact memory usage.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://dmtcs.episciences.org\&sz=32)

Discrete Math & Theoretical CS+1



### Quick check

Suppose a uniformly distributed hash has 10 leading zeros.

What is the probability of a random hash having at least 10 leading zeros?

A. \\(1/10\\)

B. \\(1/100\\)

C. \\(1/1024\\)

D. \\(1/2048\\)

Think about it before moving on. Say next when you're ready for Lesson 3: Buckets and Registers.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Lesson 3: Buckets and Registers

In Lesson 2, we learned that rare patterns in hash values help us estimate the number of distinct elements.

Now we'll see how HyperLogLog organizes those patterns into a practical data structure.

The key idea is simple:

Split the hash values into buckets, keep one register per bucket, and store the maximum rank observed in each register.

## 1. Why do we need buckets?

Imagine processing thousands of unique users.

If we look at the maximum number of leading zeros across all hashes, one unusually large value can dominate our estimate.

Instead, HyperLogLog divides the hash values into multiple groups.

For example, imagine four buckets:

```
Bucket 0 -> hashes assigned to bucket 0
Bucket 1 -> hashes assigned to bucket 1
Bucket 2 -> hashes assigned to bucket 2
Bucket 3 -> hashes assigned to bucket 3
```

Each bucket tracks its own observations. This gives us multiple pieces of statistical information instead of just one global maximum.

## 2. How does a hash select a bucket?

Let's use a small, artificial example with an 8-bit hash.

Suppose we reserve the first 2 bits for selecting a bucket. The remaining 6 bits determine the rank.

```
Hash:  01  000110
       |      |
       |      +-- Remaining 6 bits determine the rank
       +--------- First 2 bits select the bucket
```

Two bucket-selection bits give us:

\\[ m = 2^2 = 4 \text{ buckets} \\]

The mappings are:

```
00 -> Bucket 0
01 -> Bucket 1
10 -> Bucket 2
11 -> Bucket 3
```

This is a simplified example. Real implementations commonly use much wider hashes and many more registers.

## 3. What is a register?

A register is a small storage location associated with a bucket.

Each register stores the maximum rank observed so far for its bucket.

For example:

```
Bucket     Register
  0           0
  1           0
  2           0
  3           0
```

Initially, every register is zero.

As we process elements, their hashes update these values.

Remember:

- A bucket determines where an element's hash contributes.
- A register stores the maximum rank observed in that bucket.
- A register doesn't store the actual elements or their hash values.

The original HLL algorithm describes this process using an array of registers \\(M\\), where each update takes the maximum of the existing value and the new rank.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://research.google.com\&sz=32)

Google Research+1



## 4. How do we calculate rank?

For this simplified example, we use the remaining six bits after selecting the bucket.

Rank is calculated as:

\\[ \text{rank} = \text{number of leading zeros} + 1 \\]

Examples:

```
Remaining bits    Leading zeros    Rank
--------------    -------------    ----
101100                 0              1
010101                 1              2
001100                 2              3
000110                 3              4
000010                 4              5
```

Why add one?

Because a suffix starting with `1` has zero leading zeros, but its rank should be 1 rather than 0.

You can think of rank as the position of the first `1`, counted from the left, provided we haven't changed the bit ordering.

## 5. Let's process actual examples

We'll process three hash values, one at a time.

For this exercise, the first two bits select the bucket, and the remaining six bits determine the rank.

### Example 1

```
Hash: 01100101

Split:
01 | 100101
```

The first two bits are `01`, which select bucket 1.

The remaining bits are:

```
100101
```

There are zero leading zeros, so:

\\[ \text{rank} = 0 + 1 = 1 \\]

Register 1 is initially zero. We update it:

```
register[1] = max(0, 1) = 1
```

The registers now look like this:

```
Bucket     Register
  0           0
  1           1
  2           0
  3           0
```

### Example 2

Now process another hash:

```
Hash: 01000110

Split:
01 | 000110
```

Again, the bucket is 1.

The remaining bits are `000110`, containing three leading zeros.

Therefore:

\\[ \text{rank} = 3 + 1 = 4 \\]

The existing value of register 1 is 1, so:

```
register[1] = max(1, 4) = 4
```

The registers are now:

```
Bucket     Register
  0           0
  1           4
  2           0
  3           0
```

Notice that register 1 changed from 1 to 4.

### Example 3

Process one more hash:

```
Hash: 01110100

Split:
01 | 110100
```

The bucket is still 1.

The suffix `110100` has zero leading zeros, so its rank is 1.

We update:

```
register[1] = max(4, 1) = 4
```

The register remains 4.

This is the most important update rule in HyperLogLog:

\\[ \boxed{M[j] = \max(M[j], \text{rank})} \\]

Here, \\(M[j]\\) means the register associated with bucket \\(j\\).

A register can increase as new elements arrive, but never decreases during normal insertion.

## 6. Process multiple buckets

Let's put the idea together.

Suppose our stream generates these hashes:

```
Hash        Bucket bits    Remaining bits    Rank
--------    ------------   --------------    ----
00101100        00             101100           1
01100101        01             100101           1
01000110        01             000110           4
10010101        10             010101           2
11001100        11             001100           3
10100101        10             100101           1
```

After processing all six hashes:

```
Bucket 0 -> maximum rank 1
Bucket 1 -> maximum rank 4
Bucket 2 -> maximum rank 2
Bucket 3 -> maximum rank 3
```

Our register array is:

```
M = [1, 4, 2, 3]
```

We have processed six hash values, but our stored summary contains only four small numbers.

Of course, four registers are far too few for a practical estimator. This is a teaching example that makes the mechanics easy to see.

## 7. The whole update process

Here is the algorithm so far:

\#chatgpt-mermaid-\_r_1kn\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1kn\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1kn\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1kn\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1kn\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1kn\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1kn\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1kn\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1kn\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1kn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1kn\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1kn\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1kn\_ p{margin:0;}#chatgpt-mermaid-\_r_1kn\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1kn\_ .label text,#chatgpt-mermaid-\_r_1kn\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .node rect,#chatgpt-mermaid-\_r_1kn\_ .node circle,#chatgpt-mermaid-\_r_1kn\_ .node ellipse,#chatgpt-mermaid-\_r_1kn\_ .node polygon,#chatgpt-mermaid-\_r_1kn\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .rough-node .label text,#chatgpt-mermaid-\_r_1kn\_ .node .label text,#chatgpt-mermaid-\_r_1kn\_ .image-shape .label,#chatgpt-mermaid-\_r_1kn\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1kn\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .rough-node .label,#chatgpt-mermaid-\_r_1kn\_ .node .label,#chatgpt-mermaid-\_r_1kn\_ .image-shape .label,#chatgpt-mermaid-\_r_1kn\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1kn\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1kn\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1kn\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1kn\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1kn\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1kn\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1kn\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1kn\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1kn\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1kn\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1kn\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1kn\_ .icon-shape,#chatgpt-mermaid-\_r_1kn\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1kn\_ .icon-shape p,#chatgpt-mermaid-\_r_1kn\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1kn\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1kn\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1kn\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1kn\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1kn\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1kn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1kn\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1kn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1kn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1kn\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1kn\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1kn\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1kn\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1kn\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1kn\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1kn\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .node rect,#chatgpt-mermaid-\_r_1kn\_ .node circle,#chatgpt-mermaid-\_r_1kn\_ .node ellipse,#chatgpt-mermaid-\_r_1kn\_ .node polygon,#chatgpt-mermaid-\_r_1kn\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1kn\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1kn\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1kn\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1kn\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1kn\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Receive an elementCalculate its hashUse first p bits to selectbucketUse remaining bits tocalculate rankRead the selected registerCalculate leading zeros plusoneStore the maximum of oldvalue and new rank

In pseudocode:

```
Initialize all registers to 0

For every incoming element:
    hash = hashFunction(element)

    bucket = extractBucketIndex(hash)
    rank = calculateRank(hash)

    registers[bucket] = max(
        registers[bucket],
        rank
    )
```

We don't need to search the registers for a previously seen element. The hash deterministically selects a bucket and rank, so processing the same element again cannot increase any register beyond its already-observed rank.

## 8. How many buckets should we use?

Let \\(p\\) be the number of bits reserved for the bucket index.

The number of registers is:

\\[ m = 2^p \\]

Examples:

| Bucket bits (\\(p\\)) | Registers (\\(m\\)) |
| --------------------- | ------------------- |
| 4                     | 16                  |
| 8                     | 256                 |
| 10                    | 1,024               |
| 12                    | 4,096               |
| 14                    | 16,384              |

More registers generally improve estimation accuracy, but require more memory. In the standard HLL analysis, the relative standard error is approximately:

\\[ \frac{1.04}{\sqrt{m}} \\]

For example, with \\(m=1024\\), the standard error is approximately \\(3.25\\%\\). This is a statistical measure, not a guarantee that every answer will be within that percentage.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://research.google.com\&sz=32)

Google Research+1



## 9. What have we accomplished?

At this stage, we know how to turn an arbitrary stream into a compact register array.

```
Input elements
      |
      v
Hash each element
      |
      v
Select a bucket
      |
      v
Calculate its rank
      |
      v
Keep the maximum rank per bucket
      |
      v
Register array
```

But we have not yet answered the most important question:

How do we turn these register values into an estimated number of unique elements?

For example, given:

```
M = [1, 4, 2, 3]
```

How can four numbers tell us whether the original stream contained 10, 100, or 1,000 distinct elements?

That is the subject of Lesson 4: The HyperLogLog estimation formula, including why it uses a harmonic mean rather than simply taking the largest register value.

Say next when you're ready.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















# Lesson 4: The HyperLogLog Estimation Formula

In Lesson 3, we learned how HyperLogLog builds a register array by keeping the maximum rank observed in each bucket.

For example:

```
M = [1, 4, 2, 3]
```

Now we need to answer the central question:

How can these register values tell us the number of distinct elements?

HyperLogLog uses a mathematical estimator that combines information from all registers. The original algorithm uses a corrected harmonic-mean-based estimator.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://cermics.enpc.fr\&sz=32)

CERMICS+1



## 1. The intuition behind the formula

Recall the relationship between rank and probability.

```
Rank 1 -> A common pattern
Rank 2 -> A less common pattern
Rank 3 -> A still less common pattern
Rank 4 -> A rarer pattern
Rank 5 -> A much rarer pattern
```

A higher rank suggests that a less common hash pattern has appeared.

However, each register represents only one bucket. A few registers might have high ranks while other registers remain low.

So, we need to combine the observations across all buckets.

Consider these register arrays:

```
Array A: [1, 1, 1, 1, 1, 1, 1, 1]

Array B: [1, 2, 3, 4, 3, 2, 1, 3]

Array C: [4, 4, 4, 4, 4, 4, 4, 4]
```

Intuitively, Array C suggests a larger cardinality than Array A, because every bucket has observed a higher rank.

Array B contains a mixture of observations, so we need a formula that considers every register instead of simply selecting the largest one.

## 2. The raw estimation formula

The standard HyperLogLog estimator is:

\\[ E = \frac{\alpha_m m^2} {\displaystyle\sum\_{j=1}^{m}2^{-M[j]}} \\]

Where:

- \\(E\\) is the estimated number of distinct elements.
- \\(m\\) is the number of registers.
- \\(M[j]\\) is the rank stored in register \\(j\\).
- \\(\alpha_m\\) is a correction constant that depends on the number of registers.

Let's understand each part rather than memorizing the formula.

### Part A: The number of registers

Suppose we have 16 registers.

\\[ m=16 \\]

Then:

\\[ m^2=16^2=256 \\]

### Part B: Transform each register value

For each register, calculate:

\\[ 2^{-M[j]} \\]

For example:

| Register value \\(M[j]\\) | Calculation  | Result  |
| ------------------------- | ------------ | ------- |
| 1                         | \\(2^{-1}\\) | 0.5     |
| 2                         | \\(2^{-2}\\) | 0.25    |
| 3                         | \\(2^{-3}\\) | 0.125   |
| 4                         | \\(2^{-4}\\) | 0.0625  |
| 5                         | \\(2^{-5}\\) | 0.03125 |

Notice the pattern: a higher rank contributes a smaller value to the denominator.

### Part C: Add all the transformed values

For our 16 registers, suppose we have:

```
M = [0, 0, 0, 1, 1, 1, 1, 1,
     2, 2, 2, 2, 2, 3, 3, 4]
```

Transform and add their values:

```
3 registers with value 0: 3 × 1.00000 = 3.00000
5 registers with value 1: 5 × 0.50000 = 2.50000
5 registers with value 2: 5 × 0.25000 = 1.25000
2 registers with value 3: 2 × 0.12500 = 0.25000
1 register  with value 4: 1 × 0.06250 = 0.06250
```

Therefore:

\\[ \sum\_{j=1}^{16}2^{-M[j]}=7.0625 \\]

### Part D: Apply the correction constant

For the original HLL estimator with 16 registers:

\\[ \alpha\_{16}=0.673 \\]

The correction constants for the common register counts are:

\\[ \alpha\_{16}=0.673 \\]

\\[ \alpha\_{32}=0.697 \\]

\\[ \alpha\_{64}=0.709 \\]

For \\(m\geq128\\), the standard expression is:

\\[ \alpha_m=\frac{0.7213}{1+1.079/m} \\]

These constants help normalize the estimator.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://cermics.enpc.fr\&sz=32)

CERMICS+1



Now substitute the values:

\\[ E=\frac{0.673\times16^2}{7.0625} \\]

\\[ E=\frac{172.288}{7.0625} \\]

\\[ \boxed{E\approx24.4} \\]

The raw estimator produces approximately 24.4 distinct elements.

But we're not done yet.

Our example has three registers with value zero. For relatively small cardinalities, the raw estimator can be biased, and HyperLogLog uses a correction to improve the estimate.

## 3. Why do we use inverse powers?

Look at the transformation:

```
Register value       Contribution to denominator

1                    0.5
2                    0.25
3                    0.125
4                    0.0625
```

Suppose a few registers have very high values, while most have low values.

Using the inverse powers reduces the contribution of high ranks to the denominator. This helps combine observations from all buckets without letting one unusually high rank dominate the estimate.

The formula then divides a scaled constant by this sum:

\\[ E=\frac{\alpha_m m^2}{\sum\_{j=1}^{m}2^{-M[j]}} \\]

As the denominator becomes smaller, the estimated cardinality becomes larger.

The correction constant \\(\alpha_m\\) adjusts the scale of the estimate.

This estimator is related to the harmonic mean because it combines the inverse powers of the register values, rather than simply taking the arithmetic average of the ranks.

## 4. Small-range correction: Linear counting

Remember our register array:

```
M = [0, 0, 0, 1, 1, 1, 1, 1,
     2, 2, 2, 2, 2, 3, 3, 4]
```

We have:

```
Total registers:       m = 16
Zero-valued registers: V = 3
Raw estimate:          E ≈ 24.4
```

When the raw estimate is small relative to the number of registers and some registers are still zero, the original algorithm uses a different estimate called linear counting.

The formula is:

\\[ E\_{\text{small}}=m\ln\left(\frac{m}{V}\right) \\]

Where:

- \\(m\\) is the total number of registers.
- \\(V\\) is the number of registers that are still zero.
- \\(\ln\\) is the natural logarithm.

Substitute our values:

\\[ E\_{\text{small}}=16\ln\left(\frac{16}{3}\right) \\]

\\[ E\_{\text{small}}\approx16\times1.674 \\]

\\[ \boxed{E\_{\text{small}}\approx26.8} \\]

So, in this example:

```
Raw HLL estimate:          24.4
Linear counting estimate:  26.8
```

The original algorithm uses the linear-counting result in the small range when the required conditions hold. The values here are illustrative; actual accuracy depends on the input and hash distribution.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://cermics.enpc.fr\&sz=32)

CERMICS+1



### Why does counting empty registers work?

Think about placing elements randomly into buckets.

If we have processed very few distinct elements, many buckets will remain empty.

As we process more distinct elements, more buckets become occupied.

Therefore, the number of empty buckets provides information about the cardinality.

```
Few distinct elements:

[0] [0] [1] [0] [0] [2] [0] [0]
 |   |       |   |       |   |
 Many registers remain empty


More distinct elements:

[2] [3] [1] [4] [2] [3] [1] [2]
 Fewer or no empty registers
```

Here, `[0]` means an empty register, not an original element with value zero.

The expected fraction of empty registers is approximately \\(e^{-n/m}\\), where \\(n\\) is the number of distinct elements. Rearranging this relationship gives:

\\[ n\approx m\ln\left(\frac{m}{V}\right) \\]

That's the intuition behind linear counting.

## 5. What happens when cardinality becomes very large?

At very large cardinalities, a hash function with a finite output width also has a finite number of possible hash values.

For example, a 32-bit hash has:

\\[ 2^{32} \\]

possible hash values.

As the number of distinct inputs grows, hash collisions become increasingly likely. Some implementations therefore apply an additional large-range correction.

The exact correction depends on the hash width and the implementation. Modern implementations may use wider hashes or other estimation refinements.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://cermics.enpc.fr\&sz=32)

CERMICS+1



You don't need to memorize every implementation's correction formula yet. The important point is that the raw formula is not necessarily the final answer across every possible cardinality.

## 6. The complete estimation flow

Here's the process we have learned so far.

\#chatgpt-mermaid-\_r_1r5\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_1r5\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1r5\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_1r5\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_1r5\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_1r5\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_1r5\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_1r5\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_1r5\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_1r5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1r5\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1r5\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_1r5\_ p{margin:0;}#chatgpt-mermaid-\_r_1r5\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_1r5\_ .label text,#chatgpt-mermaid-\_r_1r5\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .node rect,#chatgpt-mermaid-\_r_1r5\_ .node circle,#chatgpt-mermaid-\_r_1r5\_ .node ellipse,#chatgpt-mermaid-\_r_1r5\_ .node polygon,#chatgpt-mermaid-\_r_1r5\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .rough-node .label text,#chatgpt-mermaid-\_r_1r5\_ .node .label text,#chatgpt-mermaid-\_r_1r5\_ .image-shape .label,#chatgpt-mermaid-\_r_1r5\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_1r5\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .rough-node .label,#chatgpt-mermaid-\_r_1r5\_ .node .label,#chatgpt-mermaid-\_r_1r5\_ .image-shape .label,#chatgpt-mermaid-\_r_1r5\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_1r5\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_1r5\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1r5\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1r5\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_1r5\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1r5\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1r5\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1r5\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_1r5\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_1r5\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_1r5\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_1r5\_ .icon-shape,#chatgpt-mermaid-\_r_1r5\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_1r5\_ .icon-shape p,#chatgpt-mermaid-\_r_1r5\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_1r5\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_1r5\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_1r5\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_1r5\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_1r5\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_1r5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_1r5\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_1r5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_1r5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1r5\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_1r5\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_1r5\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1r5\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_1r5\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_1r5\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_1r5\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .node rect,#chatgpt-mermaid-\_r_1r5\_ .node circle,#chatgpt-mermaid-\_r_1r5\_ .node ellipse,#chatgpt-mermaid-\_r_1r5\_ .node polygon,#chatgpt-mermaid-\_r_1r5\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_1r5\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_1r5\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_1r5\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_1r5\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_1r5\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Input elementsHash and update registersCalculate raw HLL estimateSmall estimate and zeroregisters?Use linear countingcorrectionUse raw estimate orapplicable correctionReturn estimated cardinalityYesNo

The exact conditions and additional corrections vary by implementation, but the central process remains the same.

## 7. What you should remember for interviews

The core formula is:

\\[ \boxed{E=\frac{\alpha_m m^2}{\sum\_{j=1}^{m}2^{-M[j]}}} \\]

Remember these four points:

1. Each register stores the maximum rank observed for its bucket.
2. The raw estimator combines all registers using inverse powers of two.
3. The correction constant depends on the number of registers.
4. Linear counting can improve estimates when the cardinality is small and some registers remain empty.

You do not need to derive the entire mathematical estimator from scratch in most interviews, but you should be able to explain why it combines many registers and why small-cardinality corrections are needed.

Next, we should study how the number of registers determines memory usage and estimation error, including the famous formula:

\\[ \text{Standard error}\approx\frac{1.04}{\sqrt{m}} \\]

Say next for Lesson 5: Accuracy, precision, and memory trade-offs.




















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Lesson 5: Accuracy, Precision, and Memory

In the previous lesson, we learned how HyperLogLog calculates an estimate from its registers.

Now we need to answer three practical questions:

1. How accurate is the estimate?
2. How does increasing precision improve accuracy?
3. How much memory do we need?

These are important interview questions because HyperLogLog is useful mainly due to its memory-efficiency and accuracy trade-off.

## 1. The standard error formula

The standard error of HyperLogLog is approximately:

\\[ \boxed{\text{Standard error} \approx \frac{1.04}{\sqrt{m}}} \\]

Where \\(m\\) is the number of registers. This is a relative error, expressed as a fraction; multiply by 100 to get a percentage.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://research.google.com\&sz=32)

Google Research+1



Let's calculate some examples.

For 1,024 registers:

\\[ \frac{1.04}{\sqrt{1024}} =\frac{1.04}{32} =0.0325 \\]

Therefore:

\\[ \text{Standard error} \approx 3.25\\% \\]

For 16,384 registers:

\\[ \frac{1.04}{\sqrt{16384}} =\frac{1.04}{128} =0.008125 \\]

Therefore:

\\[ \text{Standard error} \approx 0.8125\\% \\]

Notice that the number of registers increased, and the expected relative error decreased.

## 2. What does standard error mean in practice?

Suppose the actual number of unique users is:

\\[ N=1,000,000 \\]

For an HLL configuration with a relative standard error of 3.25%, the corresponding absolute standard deviation is approximately:

\\[ 1,000,000 \times 0.0325 = 32,500 \\]

This describes the scale of the estimator's statistical variation.

An estimate might be 990,000, 1,020,000, or another value. These are illustrative results, not guarantees.

Important: Standard error is not a maximum-error limit. A 3.25% standard error does not guarantee that every estimate is within 3.25% of the truth.

Under the usual normal approximation for estimation errors:

- About 68% of results fall within one standard error.
- About 95% fall within two standard errors.
- About 99.7% fall within three standard errors.

These are approximate statistical expectations, not deterministic guarantees for every dataset.

## 3. How does precision affect accuracy?

In HyperLogLog, we commonly define the precision parameter \\(p\\) as the number of hash bits used to select a register.

The number of registers is:

\\[ \boxed{m=2^p} \\]

For example:

```
p = 4  -> 16 registers
p = 8  -> 256 registers
p = 10 -> 1,024 registers
p = 12 -> 4,096 registers
p = 14 -> 16,384 registers
```

Increasing \\(p\\) increases the number of registers exponentially.

The standard error formula becomes:

\\[ \text{Standard error} \approx \frac{1.04}{\sqrt{2^p}} \\]

Let's compare several configurations.

| Precision \\(p\\) | Registers \\(m\\) | Approximate standard error |
| ----------------- | ----------------- | -------------------------- |
| 8                 | 256               | 6.50%                      |
| 10                | 1,024             | 3.25%                      |
| 12                | 4,096             | 1.63%                      |
| 14                | 16,384            | 0.81%                      |
| 16                | 65,536            | 0.41%                      |

The exact memory footprint depends on the implementation and register representation, but the relationship between register count and standard error follows the formula above.

## 4. A very important observation

Suppose you want to improve your accuracy from roughly 3.25% to 1.625%.

That is half the relative standard error.

How many additional registers do you need?

You need four times as many.

```
1,024 registers -> approximately 3.25% error

4,096 registers -> approximately 1.625% error
```

Why four times?

The error is inversely proportional to the square root of the number of registers:

\\[ e \propto \frac{1}{\sqrt{m}} \\]

Therefore, if you want to halve the error:

\\[ \frac{1}{\sqrt{m\_{\text{new}}}} =\frac{1}{2\sqrt{m\_{\text{old}}}} \\]

Squaring both sides gives:

\\[ m\_{\text{new}}=4m\_{\text{old}} \\]

So halving the error requires approximately four times the number of registers and, in a fixed-width register implementation, approximately four times the register memory.

This is one of the most useful facts to remember for interviews.

## 5. How much memory does HyperLogLog use?

A register typically stores a small integer representing the maximum observed rank.

Some practical implementations pack registers into small bit fields. For example, the dense representation discussed in modern HLL implementations can use 6 bits per register.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://www.vldb.org\&sz=32)

vldb.org+1



Using 6-bit registers as an example:

\\[ \text{Memory in bytes} \approx \frac{6m}{8} \\]

Let's calculate:

For 4,096 registers:

\\[ \frac{6\times4096}{8}=3072\text{ bytes} \\]

That's approximately 3 KB of raw register storage.

For 16,384 registers:

\\[ \frac{6\times16384}{8}=12288\text{ bytes} \\]

That's 12 KB of raw register storage.

Redis documents an HLL implementation with a maximum memory usage of approximately 12 KB and a standard error of 0.81%.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



This is a useful real-world example of how a compact summary can estimate a very large cardinality without storing all the distinct elements.

Actual memory consumption can differ because of encoding, metadata, and implementation details.

## 6. How do we choose the right precision?

Suppose you're building a web analytics service.

You need to estimate unique visitors, and you have the following requirements:

```
Expected cardinality: 100 million unique visitors
Target standard error: approximately 1%
```

We can rearrange the error formula:

\\[ e \approx \frac{1.04}{\sqrt{m}} \\]

Solving for \\(m\\):

\\[ m \approx \left(\frac{1.04}{e}\right)^2 \\]

For a target error of 1%, use \\(e=0.01\\):

\\[ m\approx\left(\frac{1.04}{0.01}\right)^2 \\]

\\[ m\approx104^2 \\]

\\[ m\approx10816 \\]

The number of registers is normally a power of two, so we choose a suitable value at least this large:

\\[ m=16384=2^{14} \\]

That corresponds to precision \\(p=14\\), with approximately 0.81% standard error.

```
Requirement
    |
    v
Choose target standard error
    |
    v
Calculate m = (1.04 / error)^2
    |
    v
Choose a supported register count
    |
    v
Calculate expected memory consumption
```

In a real implementation, also check which precision settings are supported and whether sparse encoding or other optimizations affect memory usage.

## 7. The memory-accuracy trade-off

Here's the central trade-off:

```
More registers
      |
      +--> More memory
      |
      +--> Lower standard error
```

Conversely:

```
Fewer registers
      |
      +--> Less memory
      |
      +--> Higher standard error
```

This makes HyperLogLog particularly useful for analytics systems that need approximate distinct counts across large datasets.

The number of unique elements processed can grow substantially while the register array remains a fixed size for a given configuration.

However, a fixed-size sketch is not infinitely accurate. Hash width, hash quality, register representation, implementation corrections, and the intended cardinality range still matter.

## 8. Interview questions

Q1. What is HyperLogLog's standard error?

Approximately \\(1.04/\sqrt{m}\\), where \\(m\\) is the number of registers.

Q2. How can we improve its accuracy?

Increase the number of registers. The standard error decreases in proportion to the inverse square root of the register count.

Q3. If we want half the error, how much more register memory do we generally need?

Approximately four times as much.

Q4. Does a standard error of 1% guarantee that every result is within 1% of the true count?

No. Standard error describes statistical variation; it is not a hard bound on every estimate.

Q5. Why not always use the maximum possible precision?

Higher precision requires more memory. The appropriate configuration balances memory cost and the accuracy the application needs.

## 9. Lesson summary

Remember these three formulas:

\\[ m=2^p \\]

\\[ \text{Standard error}\approx\frac{1.04}{\sqrt{m}} \\]

\\[ m\approx\left(\frac{1.04}{e}\right)^2 \\]

Here, \\(p\\) is the precision parameter, \\(m\\) is the register count, and \\(e\\) is the target relative standard error expressed as a fraction.

You now know how to estimate HLL memory requirements and how precision affects accuracy.

Next, we'll learn how HyperLogLog becomes especially useful in distributed systems: merging sketches from multiple servers to estimate the distinct count across the entire system. Say next when you're ready for Lesson 6.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Lesson 6: Merging HyperLogLog in Distributed Systems

In the previous lesson, we learned how HyperLogLog trades memory for estimation accuracy.

Now let's understand one of its most powerful features: merging HyperLogLog summaries from multiple servers to estimate the number of unique elements across the entire system.

## 1. The distributed counting problem

Imagine you're building an analytics platform that receives events from three servers.

```
Server A       Server B       Server C
--------       --------       --------
Alice          Charlie        Alice
Bob            David          Eve
Charlie        Eve            Frank
```

You want to calculate the total number of unique users across all three servers.

The users are:

```
Alice, Bob, Charlie, David, Eve, Frank
```

The answer is 6.

But suppose each server processes millions of events per second. Sending every user ID to one central server just to calculate the global unique count could consume substantial network bandwidth and memory.

Instead, each server can maintain its own HyperLogLog.

\#chatgpt-mermaid-\_r_204\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_204\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_204\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_204\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_204\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_204\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_204\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_204\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_204\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_204\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_204\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_204\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_204\_ p{margin:0;}#chatgpt-mermaid-\_r_204\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_204\_ .label text,#chatgpt-mermaid-\_r_204\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .node rect,#chatgpt-mermaid-\_r_204\_ .node circle,#chatgpt-mermaid-\_r_204\_ .node ellipse,#chatgpt-mermaid-\_r_204\_ .node polygon,#chatgpt-mermaid-\_r_204\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .rough-node .label text,#chatgpt-mermaid-\_r_204\_ .node .label text,#chatgpt-mermaid-\_r_204\_ .image-shape .label,#chatgpt-mermaid-\_r_204\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_204\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .rough-node .label,#chatgpt-mermaid-\_r_204\_ .node .label,#chatgpt-mermaid-\_r_204\_ .image-shape .label,#chatgpt-mermaid-\_r_204\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_204\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_204\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_204\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_204\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_204\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_204\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_204\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_204\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_204\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_204\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_204\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_204\_ .icon-shape,#chatgpt-mermaid-\_r_204\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_204\_ .icon-shape p,#chatgpt-mermaid-\_r_204\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_204\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_204\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_204\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_204\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_204\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_204\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_204\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_204\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_204\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_204\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_204\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_204\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_204\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_204\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_204\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_204\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_204\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .node rect,#chatgpt-mermaid-\_r_204\_ .node circle,#chatgpt-mermaid-\_r_204\_ .node ellipse,#chatgpt-mermaid-\_r_204\_ .node polygon,#chatgpt-mermaid-\_r_204\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_204\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_204\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_204\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_204\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_204\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Events for Server AHLL AEvents for Server BHLL BEvents for Server CHLL CMerge HLL summariesEstimate global distinctcount

Each server processes its own stream and sends a compact summary rather than all its individual user IDs.

The central service merges those summaries and estimates the global count.

## 2. Why can't we just add the counts?

Suppose the servers report these results:

```
Server A: 3 unique users
Server B: 3 unique users
Server C: 3 unique users
```

Can we calculate the global count as:

\\[ 3+3+3=9 \\]

No. The same user may appear on multiple servers.

For example:

```
Server A: Alice, Bob, Charlie
Server B: Charlie, David, Eve
Server C: Alice, Eve, Frank
```

Adding the local counts gives 9, but the actual union contains only 6 unique users.

This problem occurs because the sum of local cardinalities is not generally equal to the cardinality of their union.

HyperLogLog solves it by merging the underlying sketches instead of adding their estimates.

## 3. How do we merge two HyperLogLogs?

Remember from Lesson 3 that each register stores the maximum rank observed for a bucket.

Suppose two servers have these register arrays:

```
HLL A = [1, 3, 2, 1]
HLL B = [2, 1, 4, 1]
```

To merge them, take the maximum value of each corresponding register.

\\[ M\_{\text{merged}}[j]=\max(M_A[j],M_B[j]) \\]

Let's calculate it:

```
Register       HLL A       HLL B       Merged
--------       -----       -----       ------
    0             1           2           2
    1             3           1           3
    2             2           4           4
    3             1           1           1
```

The merged sketch is:

```
HLL merged = [2, 3, 4, 1]
```

That's the entire merging operation for a dense register-based HLL.

In pseudocode:

```
function merge(hllA, hllB):
    for i from 0 to m - 1:
        hllA.registers[i] =
            max(hllA.registers[i], hllB.registers[i])

    return hllA
```

This assumes both sketches use compatible register layouts, precision, hashing, and rank conventions. Practical implementations may use other representations, but the same register-wise merge principle applies.

## 4. Why does taking the maximum work?

This follows directly from how registers are constructed.

Each register stores the maximum rank seen among all elements assigned to its bucket.

Suppose Server A has processed some of the users and recorded a maximum rank of 3 in bucket 1.

Server B has processed a different set of users and recorded a maximum rank of 5 in that same bucket.

When we combine the two datasets, the maximum rank observed across the entire union must be:

\\[ \max(3,5)=5 \\]

Nothing is lost by combining those two maxima for this register.

The same reasoning applies to every register.

Therefore, merging two sketches register by register gives the same register values we would obtain by processing the union of their inputs into a single compatible sketch.

This is why HLL merging is so useful for distributed computation.

## 5. Important merge properties

Register-wise maximum gives HyperLogLog several useful properties.

### Commutativity

The order of the two sketches doesn't matter.

\\[ \max(a,b)=\max(b,a) \\]

Therefore:

```
merge(A, B) = merge(B, A)
```

### Associativity

We can merge sketches in any grouping.

\\[ \max(\max(a,b),c)=\max(a,\max(b,c)) \\]

Therefore:

```
merge(merge(A, B), C)
    =
merge(A, merge(B, C))
```

This matters when many servers contribute to the same estimate. Intermediate results can be merged hierarchically.

### Idempotence

Merging the same sketch with itself doesn't change its registers.

\\[ \max(a,a)=a \\]

Therefore:

```
merge(A, A) = A
```

These properties are especially helpful when building reliable distributed aggregation systems. Merging the same summary repeatedly doesn't inflate the count merely because the summary was duplicated.

## 6. A practical distributed architecture

Consider an analytics system with hundreds of application servers.

Each server processes incoming events and maintains an HLL for the current time window.

\#chatgpt-mermaid-\_r_222\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_222\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_222\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_222\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_222\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_222\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_222\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_222\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_222\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_222\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_222\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_222\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_222\_ p{margin:0;}#chatgpt-mermaid-\_r_222\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_222\_ .label text,#chatgpt-mermaid-\_r_222\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .node rect,#chatgpt-mermaid-\_r_222\_ .node circle,#chatgpt-mermaid-\_r_222\_ .node ellipse,#chatgpt-mermaid-\_r_222\_ .node polygon,#chatgpt-mermaid-\_r_222\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .rough-node .label text,#chatgpt-mermaid-\_r_222\_ .node .label text,#chatgpt-mermaid-\_r_222\_ .image-shape .label,#chatgpt-mermaid-\_r_222\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_222\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .rough-node .label,#chatgpt-mermaid-\_r_222\_ .node .label,#chatgpt-mermaid-\_r_222\_ .image-shape .label,#chatgpt-mermaid-\_r_222\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_222\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_222\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_222\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_222\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_222\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_222\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_222\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_222\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_222\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_222\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_222\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_222\_ .icon-shape,#chatgpt-mermaid-\_r_222\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_222\_ .icon-shape p,#chatgpt-mermaid-\_r_222\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_222\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_222\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_222\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_222\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_222\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_222\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_222\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_222\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_222\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_222\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_222\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_222\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_222\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_222\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_222\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_222\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_222\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .node rect,#chatgpt-mermaid-\_r_222\_ .node circle,#chatgpt-mermaid-\_r_222\_ .node ellipse,#chatgpt-mermaid-\_r_222\_ .node polygon,#chatgpt-mermaid-\_r_222\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_222\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_222\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_222\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_222\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_222\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Incoming user eventsApplication serversServer-local HLL summariesPeriodic summaryaggregationMerge compatible HLLsEstimate global unique usersAnalytics dashboard

For example, each server may report a daily sketch.

The aggregation service merges the daily sketches from all servers and estimates the number of unique users for that day.

The system doesn't need to transfer every user's ID to a central counting service.

This design is useful in:

- Website analytics.
- Distributed log processing.
- Large-scale event pipelines.
- Unique customer or device metrics.
- Distributed data processing jobs.

## 7. What about Redis?

Redis provides native commands for HyperLogLog operations.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



The main commands are:

| Command   | Purpose                               |
| --------- | ------------------------------------- |
| `PFADD`   | Add elements to a HyperLogLog         |
| `PFCOUNT` | Estimate the distinct count           |
| `PFMERGE` | Merge sketches into a destination key |

Here's an example.

First, add users observed by one source:

```
PFADD users:serverA Alice Bob Charlie
```

Add users observed by another source:

```
PFADD users:serverB Charlie David Eve
```

Count users on each server:

```
PFCOUNT users:serverA
PFCOUNT users:serverB
```

Each command returns an approximate count of the elements observed by that sketch.

Now merge both sketches into a new key:

```
PFMERGE users:global users:serverA users:serverB
```

Estimate the global distinct count:

```
PFCOUNT users:global
```

There are five unique users across the two servers:

```
Alice, Bob, Charlie, David, Eve
```

Redis should estimate a count near 5, although tiny datasets and approximate-counting behavior mean you should not assume all real-world estimates will always equal the exact count.

Redis also supports counting the union of multiple sketches directly:

```
PFCOUNT users:serverA users:serverB
```

Unlike `PFMERGE`, this obtains the union estimate without requiring you to save a separately named merged sketch.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



## 8. What can go wrong in a real system?

Although merging is straightforward, the surrounding system must be designed carefully.

Incompatible configurations: You shouldn't blindly merge sketches with different precision settings, hash functions, input normalization, or register conventions. Use a compatible sketch format and merging strategy.

Different representations of the same user: If one server hashes `Alice@example.com` and another hashes `alice@example.com`, they may be treated as different users. Normalize identifiers consistently before hashing.

Time-window confusion: If you merge yesterday's sketch and today's sketch, you're estimating the union across both days, not just today's count. Use separate sketches for separate windows when needed.

Double-counting local estimates: Don't add the estimated cardinalities from individual servers. Merge the sketches or use an appropriate union-count operation.

Missing events: HyperLogLog can't count events it never receives. Data loss and delivery problems must be handled elsewhere in the pipeline.

## 9. One important distinction: union versus intersection

HyperLogLog naturally supports estimating the cardinality of a union by merging compatible sketches.

For example:

\\[ |A\cup B| \\]

means the number of distinct elements found in either dataset.

But suppose you want to answer:

> How many users visited both the product page and the checkout page?

That's an intersection query:

\\[ |A\cap B| \\]

A standard HLL cannot directly calculate the exact intersection of two sets. Intersection estimation requires additional techniques, and estimates can be particularly unreliable when the overlap is small relative to the sets.

This distinction matters in analytics systems where you need more than total unique counts.

## 10. Lesson summary

Here are the key points from this lesson:

1. Each server can maintain its own HyperLogLog sketch.
2. You must merge sketches rather than add their estimated cardinalities.
3. For compatible dense sketches, merging takes the maximum value in each corresponding register.
4. The merge operation is associative, commutative, and idempotent.
5. This makes HLL useful for distributed aggregation and systems such as Redis.

The next lesson is Lesson 7: Implementation and production usage, where we'll build a simple HyperLogLog implementation in Java and connect the mathematical ideas to code.

Say next when you're ready.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Lesson 7: HyperLogLog Implementation in Java

In the previous lessons, we learned how HyperLogLog hashes elements, updates registers, estimates cardinality, and merges summaries from multiple servers.

Now we'll connect those concepts to Java code.

We'll build an educational implementation, test it, and then look at how production systems use HLL.

Our implementation will focus on the core algorithm. It will not include every optimization found in Redis or specialized HyperLogLog libraries.

## 1. What are we going to implement?

We'll build a class with three main operations:

```
hll.add("Alice");           // Add an element
hll.add("Bob");
hll.add("Alice");           // Duplicate

long estimate = hll.estimate();  // Estimate unique count
```

We'll also support merging two compatible sketches:

```
hllA.merge(hllB);
```

Our implementation will follow this flow:

\#chatgpt-mermaid-\_r_25e\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_25e\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_25e\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_25e\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_25e\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_25e\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_25e\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_25e\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_25e\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_25e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_25e\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_25e\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_25e\_ p{margin:0;}#chatgpt-mermaid-\_r_25e\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_25e\_ .label text,#chatgpt-mermaid-\_r_25e\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .node rect,#chatgpt-mermaid-\_r_25e\_ .node circle,#chatgpt-mermaid-\_r_25e\_ .node ellipse,#chatgpt-mermaid-\_r_25e\_ .node polygon,#chatgpt-mermaid-\_r_25e\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .rough-node .label text,#chatgpt-mermaid-\_r_25e\_ .node .label text,#chatgpt-mermaid-\_r_25e\_ .image-shape .label,#chatgpt-mermaid-\_r_25e\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_25e\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .rough-node .label,#chatgpt-mermaid-\_r_25e\_ .node .label,#chatgpt-mermaid-\_r_25e\_ .image-shape .label,#chatgpt-mermaid-\_r_25e\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_25e\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_25e\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_25e\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_25e\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_25e\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_25e\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_25e\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_25e\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_25e\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_25e\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_25e\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_25e\_ .icon-shape,#chatgpt-mermaid-\_r_25e\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_25e\_ .icon-shape p,#chatgpt-mermaid-\_r_25e\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_25e\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_25e\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_25e\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_25e\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_25e\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_25e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_25e\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_25e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_25e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_25e\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_25e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_25e\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_25e\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_25e\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_25e\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_25e\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .node rect,#chatgpt-mermaid-\_r_25e\_ .node circle,#chatgpt-mermaid-\_r_25e\_ .node ellipse,#chatgpt-mermaid-\_r_25e\_ .node polygon,#chatgpt-mermaid-\_r_25e\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_25e\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_25e\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_25e\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_25e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_25e\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Input elementHash elementSelect registerCalculate rankUpdate register withmaximum rankEstimate cardinality from allregisters

We'll use a fixed-size array of registers. For simplicity, we'll use a 64-bit hash and reserve some of its bits for selecting the register. The other bits will be used to calculate the rank.

## 2. Complete Java implementation

We'll implement three methods:

- `add(String value)` — update the registers.
- `estimate()` — estimate the cardinality.
- `merge(HyperLogLog other)` — combine two compatible sketches.

Create a file called `HyperLogLog.java`.

```
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class HyperLogLog {

    private final int precision;
    private final int registerCount;
    private final byte[] registers;

    public HyperLogLog(int precision) {
        if (precision < 4 || precision > 16) {
            throw new IllegalArgumentException(
                "Precision must be between 4 and 16"
            );
        }

        this.precision = precision;
        this.registerCount = 1 << precision;
        this.registers = new byte[registerCount];
    }

    // Add an element to the sketch.
    public void add(String value) {
        Objects.requireNonNull(value, "value");

        long hash = hash64(value);

        // Use the first 'precision' bits to select a register.
        int index = (int) (
            hash >>> (Long.SIZE - precision)
        );

        // Shift the bucket bits out of the way.
        long remainingBits = hash << precision;

        // Rank = leading zeros in the remaining bits + 1.
        int rank =
            Long.numberOfLeadingZeros(remainingBits)
            - precision + 1;

        int currentRank = registers[index] & 0xFF;

        // Keep the maximum rank observed by this register.
        registers[index] = (byte) Math.max(
            currentRank,
            rank
        );
    }

    // Estimate the number of distinct elements.
    public long estimate() {
        double sum = 0.0;
        int zeroRegisters = 0;

        for (byte register : registers) {
            int rank = register & 0xFF;

            if (rank == 0) {
                zeroRegisters++;
            }

            sum += Math.scalb(1.0, -rank);
        }

        double alpha = alphaMM(registerCount);

        double estimate =
            alpha * registerCount * registerCount / sum;

        // Small-range correction using linear counting.
        if (estimate <= 2.5 * registerCount
                && zeroRegisters > 0) {

            estimate = registerCount
                * Math.log(
                    (double) registerCount / zeroRegisters
                );
        }

        return Math.round(estimate);
    }

    // Merge another compatible sketch into this one.
    public void merge(HyperLogLog other) {
        Objects.requireNonNull(other, "other");

        if (this.precision != other.precision) {
            throw new IllegalArgumentException(
                "Cannot merge sketches with different precision"
            );
        }

        for (int i = 0; i < registerCount; i++) {
            int current = this.registers[i] & 0xFF;
            int incoming = other.registers[i] & 0xFF;

            this.registers[i] = (byte) Math.max(
                current,
                incoming
            );
        }
    }

    private static double alphaMM(int m) {
        switch (m) {
            case 16:
                return 0.673;
            case 32:
                return 0.697;
            case 64:
                return 0.709;
            default:
                return 0.7213 / (1.0 + 1.079 / m);
        }
    }

    // Educational hash function: use the first 64 bits
    // of the SHA-256 digest.
    private static long hash64(String value) {
        try {
            byte[] digest = MessageDigest
                .getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.wrap(digest).getLong();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 is unavailable", e
            );
        }
    }
}
```

This implementation demonstrates the main mechanics of HLL, including the raw estimator and small-range correction. It omits some refinements used by production implementations, such as large-range corrections, compact register encoding, and optimized hashing.

## 3. Understand the important Java details

### A. Why use `1 << precision`?

```
this.registerCount = 1 << precision;
```

The left-shift operation calculates a power of two.

For example:

```
precision = 4  -> 16 registers
precision = 10 -> 1,024 registers
precision = 14 -> 16,384 registers
```

This follows:

\\[ m=2^p \\]

### B. Why use `>>>` instead of `>>`?

```
int index = (int) (
    hash >>> (Long.SIZE - precision)
);
```

Java's `>>>` operator performs an unsigned right shift. It shifts bits to the right and fills the newly opened positions with zeros.

That matters because a `long` may be negative when its highest bit is `1`. We need the actual bit pattern, not sign extension.

### C. Why use `& 0xFF` for registers?

```
int currentRank = registers[index] & 0xFF;
```

A Java `byte` is signed and ranges from -128 to 127. Our registers hold nonnegative rank values.

The expression `& 0xFF` interprets the stored byte as an unsigned value from 0 to 255.

### D. Why use SHA-256?

A good HLL hash function should distribute its output bits approximately uniformly and consistently.

For an educational implementation, taking 64 bits from SHA-256 is straightforward. However, creating a new `MessageDigest` for every element is relatively expensive.

Production implementations generally use optimized, well-distributed hashing and carefully designed representations.

## 4. Try the implementation

Create `Main.java`:

```
public class Main {

    public static void main(String[] args) {

        HyperLogLog hll = new HyperLogLog(14);

        // Add 100,000 distinct identifiers.
        for (int i = 0; i < 100_000; i++) {
            hll.add("user-" + i);
        }

        // Add duplicates.
        for (int i = 0; i < 50_000; i++) {
            hll.add("user-" + i);
        }

        System.out.println(
            "Estimated unique users: " + hll.estimate()
        );
    }
}
```

Compile and run:

```
javac HyperLogLog.java Main.java
java Main
```

The true distinct count is 100,000. The estimate should generally be near that value, but it will not necessarily equal it.

With precision 14, the theoretical standard error under the usual HLL assumptions is approximately:

\\[ \frac{1.04}{\sqrt{16384}}\times100 \approx 0.81\\% \\]

That is a statistical expectation, not a guarantee for this particular run.

## 5. Test merging

Let's simulate two servers processing overlapping sets of users.

```
public class MergeExample {

    public static void main(String[] args) {

        HyperLogLog serverA = new HyperLogLog(14);
        HyperLogLog serverB = new HyperLogLog(14);

        // Server A sees users 0 through 49,999.
        for (int i = 0; i < 50_000; i++) {
            serverA.add("user-" + i);
        }

        // Server B sees users 25,000 through 99,999.
        for (int i = 25_000; i < 100_000; i++) {
            serverB.add("user-" + i);
        }

        serverA.merge(serverB);

        System.out.println(
            "Estimated global users: " + serverA.estimate()
        );
    }
}
```

The servers collectively observe 100,000 distinct identifiers, not 125,000, because 25,000 identifiers appear in both datasets.

After merging, `serverA` contains a sketch representing the union of both datasets. The estimate should be near 100,000.

Notice that we never need to send every original identifier from Server B to Server A. We merge only their register arrays.

## 6. Memory usage of our implementation

Our Java class stores one byte per register:

```
private final byte[] registers;
```

Therefore, its register array consumes approximately \\(m\\) bytes.

| Precision | Registers | Register-array memory |
| --------- | --------- | --------------------- |
| 10        | 1,024     | 1 KB                  |
| 12        | 4,096     | 4 KB                  |
| 14        | 16,384    | 16 KB                 |

These figures exclude object overhead.

The array uses more memory than a compact implementation that packs registers into fewer bits. For comparison, Redis documents an HLL implementation with a maximum memory usage of approximately 12 KB and a standard error of 0.81%.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



## 7. What should change before production?

Our implementation is useful for learning, but it isn't a complete production-ready library.

Here are the main considerations:

1. Hashing performance: Avoid creating a new SHA-256 digest instance for every event. Use an appropriate optimized hash implementation.
2. Memory efficiency: Pack registers into fewer bits or use a mature HLL implementation.
3. Corrections: Add the appropriate estimator refinements for the intended cardinality range.
4. Concurrency: This class isn't thread-safe. Use separate sketches per worker and merge them, or implement appropriate synchronization.
5. Serialization: If sketches are persisted or exchanged between services, include compatible version, precision, and hashing configuration information.
6. Testing: Validate estimates over many input sizes and datasets, check duplicate handling, and verify that merging agrees statistically with processing the union directly.

When using Redis, you can use its built-in `PFADD`, `PFCOUNT`, and `PFMERGE` commands rather than implementing the data structure yourself.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs



## 8. Lesson summary

You now have an educational Java implementation of HyperLogLog.

The core operations are:

```
add(element):
    hash element
    select register
    calculate rank
    register = max(register, rank)

estimate():
    calculate raw HLL estimate
    apply small-range correction when appropriate

merge(other):
    for each register:
        register = max(register, other.register)
```

The most important interview takeaway is that the algorithm doesn't require remembering every distinct element. It builds a compact summary and uses statistical estimation to count unique values.

Next, we'll cover Lesson 8: Interview questions and system-design scenarios, including when to choose HyperLogLog over a HashSet, Bloom filter, or Count-Min Sketch.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















# Lesson 8: HyperLogLog Interview Questions and System Design

In the previous lessons, we covered HyperLogLog's internal algorithm, Java implementation, memory usage, estimation formula, and merging.

Now let's prepare for technical interviews. We'll focus on when to use HyperLogLog, how it compares with other data structures, and how to design systems that use it.

## 1. What is HyperLogLog?

Interview question: What is HyperLogLog, and why do we need it?

HyperLogLog (HLL) is a probabilistic data structure used to estimate the number of distinct elements in a dataset.

Unlike a `HashSet`, which stores individual unique elements, HLL maintains a compact collection of registers updated using hashed values.

Its main advantages are:

- Memory usage remains bounded for a fixed configuration as more elements arrive.
- It estimates cardinality efficiently, even for very large datasets.
- Compatible sketches can be merged to estimate the distinct count across multiple data sources.

The trade-off is that the result is approximate rather than exact.

Example:

```
Input events:
Alice, Bob, Alice, Charlie, Bob, David

Exact distinct count: 4
HLL estimate:         Approximately 4
```

HLL is useful when approximate distinct counts are acceptable, such as in website analytics, log processing, and distributed metrics.

## 2. What is cardinality?

Cardinality is the number of distinct elements in a set.

For example:

```
Events = [A, B, A, C, B, D, A]

Total events       = 7
Distinct elements  = 4
Cardinality        = 4
```

Formally:

\\[ |S|=\text{number of distinct elements in }S \\]

Counting total events is usually straightforward. Counting distinct elements becomes more expensive when a system processes billions of events and needs to retain enough information to identify duplicates.

HLL addresses that challenge by estimating cardinality from a compact summary.

## 3. Why not use a HashSet?

A `HashSet` is often the best solution when you need an exact count and the unique elements fit into memory.

```
Set<String> users = new HashSet<>();

users.add("Alice");
users.add("Bob");
users.add("Alice");

System.out.println(users.size()); // 2
```

HLL trades exactness for lower memory usage.

| Property                            | HashSet                 | HyperLogLog                        |
| ----------------------------------- | ----------------------- | ---------------------------------- |
| Count                               | Exact                   | Approximate                        |
| Retains individual elements         | Yes                     | No                                 |
| Memory grows with distinct elements | Yes                     | Bounded for fixed precision        |
| Can list unique elements            | Yes                     | No                                 |
| Can merge datasets                  | Set union               | Sketch merge                       |
| Best use                            | Exact distinct counting | Large-scale cardinality estimation |

Interview takeaway: Don't say HLL is always better than a HashSet. The right choice depends on whether exactness, element retrieval, memory, and scale matter most.

## 4. HyperLogLog vs Bloom Filter vs Count-Min Sketch

This is a common interview comparison because all three are probabilistic data structures, but they solve different problems.

| Data structure   | Question it answers                                         | Example                                   |
| ---------------- | ----------------------------------------------------------- | ----------------------------------------- |
| HyperLogLog      | How many distinct items have appeared?                      | How many unique users visited my website? |
| Bloom filter     | Might this item be present in the set?                      | Have we possibly seen this email before?  |
| Count-Min Sketch | How frequently has an item appeared?                        | How many times was a product viewed?      |
| HashSet          | Which distinct items exist, and how many are there exactly? | Store every unique user ID                |

These purposes are documented in Redis's descriptions of its probabilistic data structures.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+2



### Bloom filter

A Bloom filter answers membership questions.

```
Is user-123 possibly in the set?

Bloom filter -> Possibly present
              -> Definitely not present
```

A standard Bloom filter can produce false positives: it may report that an unseen item is present. With ordinary insertion and no deletion mechanism, it doesn't produce false negatives for inserted items.

It does not give you the number of unique users.

### Count-Min Sketch

A Count-Min Sketch estimates the frequency of an individual item.

```
Events:
Apple, Apple, Apple, Banana, Orange

Count-Min Sketch:
Estimated frequency of Apple ≈ 3
```

Because hash collisions can add counts from other items, the standard Count-Min Sketch with nonnegative updates tends to overestimate frequencies.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



### HyperLogLog

HLL estimates the number of distinct items.

```
Events:
Apple, Apple, Apple, Banana, Orange

HyperLogLog:
Estimated cardinality ≈ 3
```

The interview trick is to focus on the question being asked: membership, frequency, cardinality, or exact membership and counting.

## 5. When should you use HyperLogLog?

Imagine you're building several backend features.

| Requirement                                             | Recommended approach          |
| ------------------------------------------------------- | ----------------------------- |
| Count unique website visitors across billions of events | HyperLogLog                   |
| Check whether a value might already exist               | Bloom filter                  |
| Estimate the number of purchases per product            | Count-Min Sketch              |
| Maintain an exact set of unique customer IDs            | HashSet or database           |
| Merge unique counts across servers                      | Merge compatible HLL sketches |
| Retrieve a list of all distinct visitors                | A set or database, not HLL    |

A useful rule:

Use HLL when the dataset is large, approximate counts are acceptable, and storing all distinct identifiers would be unnecessarily expensive.

Avoid HLL when you need an exact result, must retrieve individual elements, or need reliable arbitrary set-intersection or set-difference queries. HLL does not intrinsically support those set operations accurately.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://datasketches.incubator.apache.org\&sz=32)

datasketches.incubator.apache.org+1



## 6. System design: Build a unique-visitor analytics service

Let's design a service that estimates daily unique visitors across multiple application servers.

### Requirements

Assume the system must:

- Process millions of visitor events.
- Count unique visitors per day.
- Merge results across servers.
- Use bounded memory per sketch.
- Return approximate rather than exact counts.

### Architecture

\#chatgpt-mermaid-\_r_2be\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_2be\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2be\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_2be\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_2be\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_2be\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_2be\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_2be\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_2be\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_2be\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2be\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2be\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_2be\_ p{margin:0;}#chatgpt-mermaid-\_r_2be\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_2be\_ .label text,#chatgpt-mermaid-\_r_2be\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .node rect,#chatgpt-mermaid-\_r_2be\_ .node circle,#chatgpt-mermaid-\_r_2be\_ .node ellipse,#chatgpt-mermaid-\_r_2be\_ .node polygon,#chatgpt-mermaid-\_r_2be\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .rough-node .label text,#chatgpt-mermaid-\_r_2be\_ .node .label text,#chatgpt-mermaid-\_r_2be\_ .image-shape .label,#chatgpt-mermaid-\_r_2be\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_2be\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .rough-node .label,#chatgpt-mermaid-\_r_2be\_ .node .label,#chatgpt-mermaid-\_r_2be\_ .image-shape .label,#chatgpt-mermaid-\_r_2be\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_2be\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_2be\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2be\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2be\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_2be\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2be\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2be\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2be\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_2be\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_2be\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_2be\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_2be\_ .icon-shape,#chatgpt-mermaid-\_r_2be\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_2be\_ .icon-shape p,#chatgpt-mermaid-\_r_2be\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_2be\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_2be\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_2be\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_2be\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_2be\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_2be\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_2be\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_2be\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_2be\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2be\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_2be\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_2be\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2be\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_2be\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_2be\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_2be\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .node rect,#chatgpt-mermaid-\_r_2be\_ .node circle,#chatgpt-mermaid-\_r_2be\_ .node ellipse,#chatgpt-mermaid-\_r_2be\_ .node polygon,#chatgpt-mermaid-\_r_2be\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_2be\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_2be\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_2be\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_2be\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_2be\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Visitor eventsApplication serversNormalize visitor IDUpdate server-local dailyHLLPersist or publish HLLsummariesMerge sketches for the dayEstimate daily unique visitorsAnalytics dashboard

### Step 1: Receive and normalize an event

Suppose a visitor arrives:

```
visitorId = user-123
```

Before hashing, the system ensures the identifier is represented consistently across servers.

For example, if email addresses are used as identifiers, the normalization rules must be the same everywhere.

Otherwise, the same logical user could be counted as different elements.

### Step 2: Update a daily sketch

Each server updates its own HLL for the current day.

Conceptually:

```
dailyHll.add(visitorId);
```

The sketch updates its registers; it doesn't retain a list of every visitor ID.

### Step 3: Merge summaries

At the end of the processing interval, the aggregation service merges the compatible sketches from all servers.

```
HLL server A ──┐
               │
HLL server B ──┼──> Merge ──> Daily distinct count
               │
HLL server C ──┘
```

The service must merge the sketches, not add their individual cardinality estimates. The same visitor may appear on multiple servers.

### Step 4: Extend to a rolling seven-day count

Suppose the dashboard needs unique visitors across the last seven days.

Create one sketch per day:

```
HLL day 1
HLL day 2
HLL day 3
HLL day 4
HLL day 5
HLL day 6
HLL day 7
```

Merge the seven sketches and estimate the cardinality of their union.

Do not sum the daily counts. A visitor who appears on all seven days must count only once in the seven-day total.

There is an important limitation: standard HLL doesn't support removing an individual element from a sketch. Maintaining separate daily sketches lets you exclude an expired day and recompute the rolling-window estimate from the remaining sketches.

### Design considerations

In a real service, you should also consider:

- Consistency: Every server must hash identifiers using compatible settings.
- Retries: Reprocessing an event should not systematically inflate the estimate, although it can still consume processing resources.
- Time boundaries: Define the timezone and daily-window boundaries consistently.
- Data loss: HLL cannot recover events that never reached the system.
- Accuracy: Choose the register count based on the application's accuracy and memory requirements.

This is a practical application of the algorithm's compact representation and merge properties.

## 7. Using HyperLogLog in Redis

You don't necessarily need to write your own HLL implementation. Redis provides native support through three main commands.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



### Add visitors

```
PFADD visitors:day1 user-1 user-2 user-3
PFADD visitors:day2 user-2 user-3 user-4
```

### Estimate distinct visitors for each day

```
PFCOUNT visitors:day1
PFCOUNT visitors:day2
```

### Estimate the union without saving a merged sketch

```
PFCOUNT visitors:day1 visitors:day2
```

This estimates the distinct count across both sketches.

### Merge the sketches into a destination

```
PFMERGE visitors:combined visitors:day1 visitors:day2
PFCOUNT visitors:combined
```

The merged sketch represents the union of the two source sketches.

One important interview detail: `PFADD` returns whether its internal representation changed. That result does not tell you reliably whether the submitted element was previously unseen.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://redis.io\&sz=32)

Docs+1



## 8. Common pitfalls interviewers look for

### Pitfall 1: Adding individual estimates

Incorrect:

```
Server A estimate = 10,000
Server B estimate = 12,000

Global estimate = 22,000
```

This can overcount overlapping users.

Correct: merge the sketches and estimate their union.

### Pitfall 2: Assuming the result is exact

HLL estimates cardinality. Even when the result looks plausible, it should not be treated as a guaranteed exact value.

### Pitfall 3: Assuming more events require more registers

For a fixed configuration, the register array has a fixed size. It doesn't grow proportionally to the number of distinct elements processed.

However, finite hash width and statistical limitations still matter.

### Pitfall 4: Using HLL to retrieve elements

HLL cannot reconstruct the list of unique values because it stores a compact statistical summary, not the original elements.

### Pitfall 5: Forgetting the error trade-off

The standard error is approximately:

\\[ e=\frac{1.04}{\sqrt{m}} \\]

where \\(m\\) is the number of registers.

More registers generally mean lower standard error but greater memory use.

### Pitfall 6: Ignoring the limitations of merging

Sketches must use compatible hashing and configuration, or be converted through a supported merge mechanism. Arbitrarily merging incompatible internal representations is unsafe.

## 9. Interview questions to practise

Try answering these without looking at the earlier lessons.

Question 1: How does HyperLogLog estimate cardinality without storing every unique element?

Expected answer: It hashes each element, updates a register associated with the hash, and estimates cardinality from the distribution of register ranks.

Question 2: Why does HyperLogLog use multiple registers rather than keeping only the largest observed rank?

Expected answer: Multiple registers provide several observations of hash patterns. Combining them reduces the influence of any one unusually high or low observation.

Question 3: Why can't we add the cardinality estimates from different servers?

Expected answer: The servers may have processed overlapping elements. Adding their estimates counts overlaps multiple times. Merging compatible sketches estimates the union instead.

Question 4: What happens to standard error when we quadruple the number of registers?

Expected answer: It is approximately halved, since relative standard error decreases in proportion to \\(1/\sqrt{m}\\).

Question 5: Why is HyperLogLog unsuitable for exact billing calculations?

Expected answer: Its estimates are probabilistic, so the system cannot guarantee the exact distinct count needed for an exact billing rule.

Question 6: Can HyperLogLog estimate the intersection of two sets directly?

Expected answer: Standard HLL supports union estimation naturally, but intersection estimation is not an intrinsic, reliably accurate operation. Another data structure or an additional estimation technique may be required.

Question 7: What is the time complexity of updating an HLL?

Expected answer: For a fixed configuration, updating one element takes constant time, assuming the hash calculation itself takes constant time for the input size under consideration. Estimation and merging may require processing the register arrays, so their costs depend on the register count.

## 10. Final revision summary

Here is the complete HyperLogLog concept in one sequence:

```
1. Receive an element.
       |
       v
2. Hash the element.
       |
       v
3. Select a register using part of the hash.
       |
       v
4. Calculate a rank from the remaining bits.
       |
       v
5. Store the maximum rank in the register.
       |
       v
6. Repeat for incoming elements.
       |
       v
7. Estimate cardinality using the register values.
       |
       v
8. Merge compatible sketches to estimate unions.
```

The principal formulae are:

Number of registers:

\\[ m=2^p \\]

Approximate standard error:

\\[ e\approx\frac{1.04}{\sqrt{m}} \\]

Raw cardinality estimate:

\\[ E=\frac{\alpha_m m^2} {\displaystyle\sum\_{j=1}^{m}2^{-M[j]}} \\]

Small-range linear-counting estimate:

\\[ E\_{\text{small}}=m\ln\left(\frac{m}{V}\right) \\]

The final estimate can use applicable corrections depending on the algorithm and cardinality range.

The most important takeaway is that HyperLogLog exchanges exactness for a compact, mergeable representation that estimates distinct counts efficiently at scale.

We've now covered the planned eight lessons, from the fundamentals through Java implementation and system design.

The best next exercise is a mock interview: explain HyperLogLog from scratch, then work through questions about its estimator, precision, merging, and production trade-offs.




















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
















