# **Streaming Algorithms – Complete DSA Interview Roadmap (Beginner to Advanced)**

Streaming Algorithms are an important DSA pattern, particularly for interviews at Google, Meta, Amazon, and other product-based companies.

I recommend learning this as a **collection of related patterns rather than one single pattern**.

The central idea is:

> Process elements one at a time, maintain only the necessary state, and produce an answer without repeatedly processing the entire input.

For example, suppose you receive a stream of one million integers.

You might need to:

- Find the minimum and maximum seen so far.
- Calculate the average of all elements.
- Find the average of the last `K` elements.
- Count frequencies without storing the entire stream.
- Find the most frequent element.
- Detect whether a particular pattern has appeared.
- Find the median of all elements received so far.
- Find the top K frequent elements.
- Detect duplicates within the last K elements.

These problems look similar, but they require different data structures and invariants.

There is also an important distinction:

**Streaming algorithms are not synonymous with sliding window.**

Sliding window is one category of streaming algorithms. Other categories include running aggregates, frequency tracking, order statistics, approximate counting, and online pattern matching.

Let's organize the patterns properly.

## **1. Complete pattern classification**

I recommend dividing Streaming Algorithms into the following 14 categories.

| #  | Pattern                                    | What you maintain                              | Typical data structures                              |
| -- | ------------------------------------------ | ---------------------------------------------- | ---------------------------------------------------- |
| 1  | Streaming + Min / Max                      | Minimum and maximum so far                     | Variables, Monotonic Deque                           |
| 2  | Streaming + Sum / Product                  | Running aggregate                              | Variables                                            |
| 3  | Streaming + Average / Statistics           | Count, sum, statistical state                  | Variables, Welford's algorithm                       |
| 4  | Streaming + Frequency / Count              | Element frequencies and occurrence counts      | HashMap, HashSet                                     |
| 5  | Streaming + Sliding Window                 | State for the last K elements or time interval | Deque, Queue, HashMap                                |
| 6  | Streaming + Top K / Heavy Hitters          | Most frequent or highest-ranked elements       | Heap, HashMap, Misra-Gries                           |
| 7  | Streaming + Median / Quantiles             | Dynamic order statistics                       | Two Heaps, Balanced BST                              |
| 8  | Streaming + Distinct / Cardinality         | Number of unique elements                      | HashSet, HyperLogLog                                 |
| 9  | Streaming + Matching / Detection           | Whether a pattern occurs in the stream         | KMP, Rolling Hash, Automata                          |
| 10 | Streaming + Subarray / Prefix State        | Information about prefixes seen so far         | HashMap, Prefix Sum                                  |
| 11 | Streaming + Monotonicity / Next Greater    | Relevant previous or future candidates         | Monotonic Stack, Deque                               |
| 12 | Streaming + Sampling                       | Representative samples from a large stream     | Reservoir Sampling                                   |
| 13 | Streaming + Approximation / Sketching      | Approximate answers with bounded memory        | Count-Min Sketch, Bloom Filter                       |
| 14 | Streaming + Multiple Queries / Composition | Multiple aggregates or queries simultaneously  | Segment Tree, Fenwick Tree, Multiple Data Structures |

One more distinction is important:

- **Exact streaming:** You maintain enough information to produce an exact answer.
- **Approximate streaming:** You deliberately use bounded memory and accept a controlled error.
- **Online processing:** You must make decisions as elements arrive, potentially without knowing future elements.

For DSA interviews, prioritize exact streaming first. Approximate streaming is an excellent advanced topic, especially for system design and large-scale data processing.

## **2. The most important distinction: true streaming vs streaming-compatible**

Before selecting problems, understand three different types.

### **Type A: True data stream problems**

These explicitly simulate an incoming stream. You receive elements one at a time, and your data structure must support operations such as `add()`, `query()`, or `get()`.

Examples:

- [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)
- [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)
- [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)
- [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com)
- [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)
- [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)

These should be your highest priority when studying the streaming pattern.

### **Type B: Streaming-compatible array problems**

These are conventional array or string problems, but they can be solved by processing the input from left to right while maintaining a compact state.

Examples:

- [643. Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/?utm_source=chatgpt.com)
- [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/?utm_source=chatgpt.com)
- [525. Contiguous Array](https://leetcode.com/problems/contiguous-array/?utm_source=chatgpt.com)
- [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/?utm_source=chatgpt.com)
- [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)

These are important because interviewers frequently test the underlying streaming technique without explicitly mentioning a data stream.

### **Type C: Advanced streaming algorithm problems**

These involve processing potentially unbounded data with strict memory constraints.

Examples:

- Finding heavy hitters in massive streams.
- Estimating the number of distinct elements.
- Approximate frequency counting.
- Reservoir sampling.
- Approximate quantiles.
- Detecting duplicates with bounded memory.

These are more closely related to theoretical streaming algorithms and large-scale data processing than conventional LeetCode questions.

The distinction matters: a one-pass algorithm that stores an entire `HashMap` is online, but it is not necessarily a memory-efficient streaming algorithm.

# **3. Detailed pattern-wise problem roadmap**

I'll use these classifications:

- **Core:** Directly teaches the pattern.
- **Advanced:** An important extension or combination of the pattern.
- **Related:** Uses the technique but is not primarily a streaming problem.
- **Recommended:** A useful additional problem worth solving for mastery.

## **Pattern 1: Streaming + Minimum / Maximum**

**Core idea:** Maintain the smallest or largest value encountered so far without revisiting previous elements.

There are two different subpatterns:

1. Global min/max: Maintain the minimum or maximum across the entire stream.
2. Window min/max: Maintain the minimum or maximum among the last K elements.

### **Problems**

| Problem                                                                                                                                                                                                              | Difficulty | Classification | What you learn                                               |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------------------------------------ |
| [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)                                                                                        | Easy       | Core           | Maintain top K elements using a min-heap                     |
| [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/?utm_source=chatgpt.com)                                                                                                          | Hard       | Core           | Maintain maximum in a sliding window using a monotonic deque |
| [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/?utm_source=chatgpt.com) | Medium     | Advanced       | Simultaneously maintain window minimum and maximum           |
| [2762. Continuous Subarrays](https://leetcode.com/problems/continuous-subarrays/?utm_source=chatgpt.com)                                                                                                             | Medium     | Advanced       | Maintain min/max constraints dynamically                     |
| [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                                                                                                 | Hard       | Advanced       | Maintain ordered partitions and discard extremes             |
| [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                                                                                                            | Hard       | Recommended    | Maintain dynamic order statistics as elements expire         |

**Important concepts to master:**

- Running minimum and maximum.
- Monotonic increasing and decreasing deque.
- Lazy deletion in heaps.
- Maintaining min/max simultaneously.
- Handling elements leaving a sliding window.

**My recommendation:** Do not treat global min/max and sliding-window min/max as the same technique. They have different invariants and data structure requirements.

## **Pattern 2: Streaming + Sum / Product**

**Core idea:** Maintain an aggregate as new elements arrive.

For sum:

\\[ S_i = S\_{i-1} + x_i \\]

For product:

\\[ P_i = P\_{i-1} \times x_i \\]

The interesting part is supporting queries over the last K elements without recalculating everything.

### **Problems**

| Problem                                                                                                                       | Difficulty | Classification | What you learn                      |
| ----------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ----------------------------------- |
| [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)    | Medium     | Core           | Prefix product and handling zero    |
| [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com) | Easy       | Core           | Running sum with a fixed-size queue |
| [303. Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/?utm_source=chatgpt.com)           | Easy       | Related        | Prefix sum fundamentals             |
| [1480. Running Sum of 1d Array](https://leetcode.com/problems/running-sum-of-1d-array/?utm_source=chatgpt.com)                | Easy       | Related        | Running aggregate                   |
| [238. Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/?utm_source=chatgpt.com)       | Medium     | Related        | Prefix and suffix products          |
| [1732. Find the Highest Altitude](https://leetcode.com/problems/find-the-highest-altitude/?utm_source=chatgpt.com)            | Easy       | Related        | Running sum and running maximum     |

### **Particularly important: Product of the Last K Numbers**

This is one of the best streaming problems because it introduces an important issue: **zero invalidates ordinary prefix-product division.**

Consider:

```
Stream:  3, 4, 0, 2, 5
```

You cannot simply divide a global prefix product to obtain the last K products because the product becomes zero.

The standard solution resets the prefix-product structure whenever zero arrives.

This teaches you how to maintain a streaming aggregate when special values invalidate previous state.

## **Pattern 3: Streaming + Average / Statistical Aggregation**

**Core idea:** Maintain sufficient statistics instead of storing all observations.

For a simple running average:

\\[ Average = \frac{Sum}{Count} \\]

For a moving average, maintain:

\\[ WindowSum = WindowSum + Incoming - Outgoing \\]

### **Problems**

| Problem                                                                                                                              | Difficulty | Classification | What you learn                                             |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------- | ---------------------------------------------------------- |
| [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)        | Easy       | Core           | Moving average using a queue                               |
| [643. Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/?utm_source=chatgpt.com)                  | Easy       | Core           | Fixed-size sliding window                                  |
| [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Advanced       | Trimmed average using ordered partitions                   |
| [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Advanced       | Maintain multiple statistics under insertions and removals |
| [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)              | Hard       | Advanced       | Maintain an online statistical measure                     |
| [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                            | Hard       | Advanced       | Median when old observations expire                        |

### **Additional concepts beyond LeetCode**

These are particularly valuable:

**1. Running mean**

Maintain `sum` and `count`.

**2. Running variance and standard deviation**

Learn Welford's algorithm.

Instead of storing every observation, update the mean and sum of squared deviations incrementally.

**3. Exponential Moving Average (EMA)**

\\[ EMA_t = \alpha x_t + (1-\alpha)EMA\_{t-1} \\]

Useful in financial data, monitoring systems, and time-series analysis.

**4. Weighted moving average**

Recent observations can receive more weight than older observations.

**5. Trimmed mean**

Remove the smallest and largest K elements before calculating the average. Finding MK Average is an excellent implementation exercise.

**My recommendation:** Learn Welford's algorithm and EMA even though they are not commonly represented by dedicated LeetCode problems. They make your understanding of streaming statistics much stronger.

## **Pattern 4: Streaming + Frequency / Counting**

**Core idea:** Maintain the frequency of elements as they arrive.

Typical state:

```
Map<Integer, Integer> frequency = new HashMap<>();
```

For every incoming element:

```
frequency.merge(value, 1, Integer::sum);
```

However, frequency tracking has several variations:

- Count every occurrence.
- Find the first unique element.
- Find the majority element.
- Count consecutive occurrences.
- Maintain frequencies within a sliding window.
- Support frequency queries.

### **Problems**

| Problem                                                                                                                                                  | Difficulty | Classification | What you learn                                    |
| -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------------------------- |
| [169. Majority Element](https://leetcode.com/problems/majority-element/?utm_source=chatgpt.com)                                                          | Easy       | Core           | Boyer-Moore voting                                |
| [229. Majority Element II](https://leetcode.com/problems/majority-element-ii/?utm_source=chatgpt.com)                                                    | Medium     | Advanced       | Generalized majority voting                       |
| [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                                                   | Medium     | Core           | Dynamic frequency and insertion order             |
| [387. First Unique Character in a String](https://leetcode.com/problems/first-unique-character-in-a-string/?utm_source=chatgpt.com)                      | Easy       | Related        | Frequency counting                                |
| [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                                                    | Easy       | Core           | Track previously observed messages and timestamps |
| [2620. Find Consecutive Integers from a Data Stream](https://leetcode.com/problems/find-consecutive-integers-from-a-data-stream/?utm_source=chatgpt.com) | Medium     | Core           | Track consecutive matching values                 |
| [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                                                      | Medium     | Core           | Count events within a time window                 |
| [992. Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/?utm_source=chatgpt.com)                    | Hard       | Related        | Frequency map with sliding window                 |
| [930. Binary Subarrays With Sum](https://leetcode.com/problems/binary-subarrays-with-sum/?utm_source=chatgpt.com)                                        | Medium     | Related        | Prefix frequency counting                         |

### **Special techniques to learn**

**Boyer-Moore Majority Vote**

When one element appears more than half the time, you can find its candidate using:

- One candidate variable.
- One counter variable.
- One pass.
- O(1) auxiliary space.

**First Unique Number**

You need more than a frequency map. You also need to maintain the order in which elements arrived.

Common implementation:

- HashMap for frequencies.
- Queue or LinkedHashSet for arrival order.
- Remove elements when they are no longer unique.

This is an excellent example of combining multiple streaming states.

## **Pattern 5: Streaming + Sliding Window**

**Core idea:** Maintain information about a continuously changing window of recent elements.

The window can be defined by:

1. Number of elements: Last K elements.
2. Time: Last T seconds/minutes.
3. A condition: Longest valid contiguous sequence.

This is arguably the most important subpattern for coding interviews.

### **A. Fixed-size sliding window**

| Problem                                                                                                                                                        | Difficulty | Classification | What you learn                 |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------ |
| [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)                                  | Easy       | Core           | Queue + running sum            |
| [643. Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/?utm_source=chatgpt.com)                                            | Easy       | Core           | Fixed-size window sum          |
| [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/?utm_source=chatgpt.com)                                                    | Hard       | Core           | Monotonic deque                |
| [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                                                      | Hard       | Advanced       | Two heaps or ordered multisets |
| [2461. Maximum Sum of Distinct Subarrays With Length K](https://leetcode.com/problems/maximum-sum-of-distinct-subarrays-with-length-k/?utm_source=chatgpt.com) | Medium     | Advanced       | Window sum + frequency map     |
| [438. Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/?utm_source=chatgpt.com)                                      | Medium     | Related        | Window frequency matching      |

### **B. Variable-size sliding window**

| Problem                                                                                                                                                                                                              | Difficulty | Classification | What you learn                       |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------------ |
| [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/?utm_source=chatgpt.com)                                                            | Medium     | Core           | Dynamic window + last seen index     |
| [209. Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/?utm_source=chatgpt.com)                                                                                                    | Medium     | Core           | Expand/shrink window                 |
| [1004. Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/?utm_source=chatgpt.com)                                                                                                     | Medium     | Core           | Constraint-based window              |
| [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/?utm_source=chatgpt.com)                                                                        | Medium     | Advanced       | Window frequency + maximum frequency |
| [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/?utm_source=chatgpt.com) | Medium     | Advanced       | Two monotonic deques                 |
| [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/?utm_source=chatgpt.com)                                                                                                       | Hard       | Advanced       | Required vs current frequency        |
| [992. Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/?utm_source=chatgpt.com)                                                                                | Hard       | Advanced       | AtMost(K) - AtMost(K-1)              |

### **C. Time-based sliding window**

| Problem                                                                                                                       | Difficulty | Classification | What you learn                         |
| ----------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | -------------------------------------- |
| [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com)                   | Easy       | Core           | Queue with timestamp expiration        |
| [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                           | Medium     | Core           | Count requests in the last 300 seconds |
| [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                         | Easy       | Core           | Time-based duplicate suppression       |
| [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com) | Easy       | Related        | Element-count window                   |

**Important distinction:** A time-based window and a fixed-size window are not the same.

For example, a request-rate counter might receive 10,000 requests within one second and only 5 requests within the next second. A queue must expire elements based on timestamps, not based on a fixed number of elements.

This distinction is especially useful for rate limiters and event-processing systems.

## **Pattern 6: Streaming + Top K / Heavy Hitters**

**Core idea:** Find elements that appear frequently or maintain the K most important elements while processing a stream.

There are two different problems:

1. Top K by value: Find the K largest elements.
2. Top K by frequency: Find the K most frequently occurring elements.

Do not confuse them.

### **Problems**

| Problem                                                                                                                              | Difficulty | Classification | What you learn                          |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------- | --------------------------------------- |
| [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)        | Easy       | Core           | Min-heap of size K                      |
| [347. Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/?utm_source=chatgpt.com)                        | Medium     | Related        | Frequency map + heap/bucket sort        |
| [692. Top K Frequent Words](https://leetcode.com/problems/top-k-frequent-words/?utm_source=chatgpt.com)                              | Medium     | Related        | Frequency + ranking                     |
| [451. Sort Characters By Frequency](https://leetcode.com/problems/sort-characters-by-frequency/?utm_source=chatgpt.com)              | Medium     | Related        | Frequency-based ordering                |
| [767. Reorganize String](https://leetcode.com/problems/reorganize-string/?utm_source=chatgpt.com)                                    | Medium     | Related        | Greedy scheduling by frequency          |
| [215. Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/?utm_source=chatgpt.com)        | Medium     | Related        | Quickselect and heap comparison         |
| [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Advanced       | Maintain ordered partitions             |
| [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Advanced       | Maintain frequency and order statistics |

### **Advanced algorithms you should know**

This is where traditional DSA ends and actual streaming algorithms become interesting.

| Algorithm        | Purpose                                        | Memory            |
| ---------------- | ---------------------------------------------- | ----------------- |
| Boyer-Moore      | Find majority element                          | O(1)              |
| Misra-Gries      | Find frequent elements                         | O(1/ε) counters   |
| Space-Saving     | Approximate heavy hitters                      | O(1/ε) counters   |
| Count-Min Sketch | Estimate item frequencies                      | Fixed sketch size |
| Count Sketch     | Estimate frequencies, including signed updates | Fixed sketch size |

**Recommended additional exercises:**

1. Given an infinite stream, find all elements appearing more than N/3 times.
2. Find the most frequent 10 IP addresses from billions of requests.
3. Find all items whose frequency exceeds 1% of the total stream.
4. Maintain the top K frequent items as new events arrive.
5. Maintain top K frequent items in the last 10 minutes.

The last two are particularly challenging because frequencies change continuously.

## **Pattern 7: Streaming + Median / Order Statistics / Quantiles**

**Core idea:** Maintain the relative ordering of incoming elements to answer queries such as:

- What is the median?
- What is the Kth smallest element?
- What is the Kth largest element?
- What is the 90th percentile?
- What is the median of the last K elements?

### **Problems**

| Problem                                                                                                                              | Difficulty | Classification | What you learn                                                |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------- | ------------------------------------------------------------- |
| [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)              | Hard       | Core           | Two heaps                                                     |
| [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)        | Easy       | Core           | Maintain K largest elements                                   |
| [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                            | Hard       | Advanced       | Dynamic median with deletion                                  |
| [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Advanced       | Three ordered partitions                                      |
| [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)   | Hard       | Advanced       | Maintain ordered rankings                                     |
| [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                       | Medium     | Advanced       | Maintain minimum, maximum and current price under corrections |
| [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Advanced       | Multiple online statistical queries                           |

### **Important data structures**

| Data structure | Best use                           |
| -------------- | ---------------------------------- |
| Two Heaps      | Running median                     |
| Min Heap       | K largest elements                 |
| Max Heap       | K smallest elements                |
| TreeMap        | Ordered values with duplicates     |
| Balanced BST   | Dynamic order statistics           |
| Fenwick Tree   | Kth element using frequency counts |
| Segment Tree   | Dynamic frequency/order statistics |

### **Recommended advanced problem**

**480. Sliding Window Median**

This is significantly more challenging than Find Median from Data Stream.

Why?

In 295, you only insert new elements.

In 480, you must:

- Insert a new element.
- Remove an old element.
- Maintain the median.
- Handle duplicates.
- Maintain correct heap sizes.

This teaches you the difference between insertion-only streaming and sliding-window streaming.

## **Pattern 8: Streaming + Distinct Elements / Deduplication**

**Core idea:** Track how many unique elements have arrived or determine whether an element has already appeared.

There are two different versions.

**Exact distinct counting:** Store enough information to identify every unique item.

**Approximate distinct counting:** Use a compact probabilistic structure to estimate cardinality.

### **Problems**

| Problem                                                                                                                                                   | Difficulty | Classification | What you learn                              |
| --------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------------------- |
| [217. Contains Duplicate](https://leetcode.com/problems/contains-duplicate/?utm_source=chatgpt.com)                                                       | Easy       | Related        | Set-based duplicate detection               |
| [219. Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/?utm_source=chatgpt.com)                                                 | Easy       | Core           | Duplicate detection within a sliding window |
| [220. Contains Duplicate III](https://leetcode.com/problems/contains-duplicate-iii/?utm_source=chatgpt.com)                                               | Hard       | Advanced       | Sliding window + ordered set                |
| [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                                                    | Medium     | Core           | Dynamic uniqueness tracking                 |
| [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/?utm_source=chatgpt.com) | Medium     | Related        | Distinct elements within a window           |
| [187. Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/?utm_source=chatgpt.com)                                               | Medium     | Related        | Repeated sequence detection                 |
| [2670. Find the Distinct Difference Array](https://leetcode.com/problems/find-the-distinct-difference-array/?utm_source=chatgpt.com)                      | Easy       | Related        | Prefix/suffix distinct counts               |

### **Advanced algorithms to learn**

**1. Bloom Filter**

Answers:

- Definitely not seen before.
- Possibly seen before.

It can produce false positives but not false negatives under the standard insertion-only model.

**2. HyperLogLog**

Estimates the number of unique elements using a very small amount of memory.

Example:

You have billions of website visitors and want to estimate daily unique visitors without storing every visitor ID.

**3. Flajolet-Martin**

A foundational probabilistic algorithm for distinct-element estimation.

**Recommended exercises:**

- Implement a Bloom Filter.
- Implement a basic Flajolet-Martin estimator.
- Understand HyperLogLog at the algorithmic level.
- Compare exact HashSet counting against approximate cardinality estimation.

## **Pattern 9: Streaming + String Matching / Pattern Detection**

**Core idea:** Characters arrive one at a time. Detect whether the sequence received so far contains a particular pattern.

This is an important category because the pattern-matching state must survive between incoming characters.

### **Problems**

| Problem                                                                                                                                                            | Difficulty | Classification | What you learn                     |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------- | ---------------------------------- |
| [1032. Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)                                                           | Hard       | Core           | Online suffix matching using Trie  |
| [28. Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/?utm_source=chatgpt.com) | Easy       | Related        | KMP / pattern matching             |
| [459. Repeated Substring Pattern](https://leetcode.com/problems/repeated-substring-pattern/?utm_source=chatgpt.com)                                                | Easy       | Related        | Prefix-function concepts           |
| [686. Repeated String Match](https://leetcode.com/problems/repeated-string-match/?utm_source=chatgpt.com)                                                          | Medium     | Related        | Matching across repeated sequences |
| [187. Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/?utm_source=chatgpt.com)                                                        | Medium     | Related        | Rolling hash / encoding            |
| [1392. Longest Happy Prefix](https://leetcode.com/problems/longest-happy-prefix/?utm_source=chatgpt.com)                                                           | Hard       | Related        | KMP prefix function                |
| [214. Shortest Palindrome](https://leetcode.com/problems/shortest-palindrome/?utm_source=chatgpt.com)                                                              | Hard       | Related        | KMP and prefix matching            |

### **Important algorithms**

- KMP.
- Z Algorithm.
- Rabin-Karp / Rolling Hash.
- Trie-based matching.
- Aho-Corasick automaton.

### **The most important problem: Stream of Characters**

Suppose you have:

```
words = ["cd", "f", "kl"]
```

Characters arrive:

```
a -> b -> c -> d
```

After every character, you must determine whether any suffix of the characters received so far matches a word.

This is not ordinary substring search.

You are continuously receiving characters, and the data structure must retain enough state to answer every query.

This is an excellent problem for understanding online pattern matching.

## **Pattern 10: Streaming + Prefix State / Prefix Sum / Subarray Counting**

**Core idea:** Maintain information about previous prefixes so that you can answer questions about subarrays ending at the current position.

This category is extremely important for interviews.

Suppose you receive:

```
2, 1, 3, 2, 1
```

You maintain a running prefix sum.

For a target sum K:

\\[ Prefix[j] - Prefix[i] = K \\]

Rearranging:

\\[ Prefix[i] = Prefix[j] - K \\]

Therefore, you only need to maintain the frequency of previously encountered prefix sums.

### **Problems**

| Problem                                                                                                                          | Difficulty | Classification | What you learn                       |
| -------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------------ |
| [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/?utm_source=chatgpt.com)                        | Medium     | Core           | Prefix sum + frequency map           |
| [525. Contiguous Array](https://leetcode.com/problems/contiguous-array/?utm_source=chatgpt.com)                                  | Medium     | Core           | Prefix balance + first occurrence    |
| [930. Binary Subarrays With Sum](https://leetcode.com/problems/binary-subarrays-with-sum/?utm_source=chatgpt.com)                | Medium     | Core           | Prefix sum frequency                 |
| [974. Subarray Sums Divisible by K](https://leetcode.com/problems/subarray-sums-divisible-by-k/?utm_source=chatgpt.com)          | Medium     | Advanced       | Prefix remainder frequency           |
| [1248. Count Number of Nice Subarrays](https://leetcode.com/problems/count-number-of-nice-subarrays/?utm_source=chatgpt.com)     | Medium     | Advanced       | Prefix count of odd numbers          |
| [1124. Longest Well-Performing Interval](https://leetcode.com/problems/longest-well-performing-interval/?utm_source=chatgpt.com) | Medium     | Advanced       | Prefix balance + monotonic reasoning |
| [523. Continuous Subarray Sum](https://leetcode.com/problems/continuous-subarray-sum/?utm_source=chatgpt.com)                    | Medium     | Advanced       | Prefix remainder + earliest index    |

**Important:** These are not all strict bounded-memory streaming algorithms because their HashMaps can grow with the number of distinct prefix states.

However, they are highly relevant to streaming-style one-pass processing and interview problem solving.

## **Pattern 11: Streaming + Monotonic Stack / Next Greater Element**

**Core idea:** As new elements arrive, maintain only the previous elements that can still influence future answers.

A monotonic stack removes elements that are no longer useful.

This is particularly powerful for online problems.

### **Problems**

| Problem                                                                                                                                | Difficulty | Classification | What you learn                 |
| -------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ------------------------------ |
| [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)                                      | Medium     | Core           | Online monotonic stack         |
| [739. Daily Temperatures](https://leetcode.com/problems/daily-temperatures/?utm_source=chatgpt.com)                                    | Medium     | Related        | Next greater element           |
| [496. Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/?utm_source=chatgpt.com)                            | Easy       | Related        | Monotonic stack                |
| [503. Next Greater Element II](https://leetcode.com/problems/next-greater-element-ii/?utm_source=chatgpt.com)                          | Medium     | Related        | Circular monotonic stack       |
| [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/?utm_source=chatgpt.com)             | Hard       | Related        | Monotonic stack                |
| [1944. Number of Visible People in a Queue](https://leetcode.com/problems/number-of-visible-people-in-a-queue/?utm_source=chatgpt.com) | Hard       | Advanced       | Monotonic stack and visibility |

### **Why Online Stock Span is special**

You receive stock prices one by one.

For every new price, return the number of consecutive previous days whose prices were less than or equal to today's price.

You cannot know the future prices.

The monotonic stack efficiently removes previous prices that are no longer relevant.

This is a genuinely important online algorithm.

## **Pattern 12: Streaming + Sampling**

**Core idea:** You cannot store an entire massive stream, but you want to retain a representative sample.

This is particularly important when:

- The stream length is unknown.
- You cannot revisit previous elements.
- Memory is limited.
- You want an unbiased sample.

### **Problems and exercises**

There are fewer standard LeetCode problems specifically dedicated to this category, so I recommend algorithmic implementation exercises.

| Problem / Exercise                                                                                                                                  | Difficulty | Classification | What you learn               |
| --------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------- | ---------------------------- |
| Implement Reservoir Sampling (K = 1)                                                                                                                | Medium     | Core           | Uniform random sampling      |
| Implement Reservoir Sampling (K elements)                                                                                                           | Medium     | Core           | Maintain a fixed-size sample |
| [398. Random Pick Index](https://leetcode.com/problems/random-pick-index/?utm_source=chatgpt.com)                                                   | Medium     | Core           | Reservoir sampling           |
| [497. Random Point in Non-overlapping Rectangles](https://leetcode.com/problems/random-point-in-non-overlapping-rectangles/?utm_source=chatgpt.com) | Medium     | Related        | Weighted random selection    |
| [528. Random Pick with Weight](https://leetcode.com/problems/random-pick-with-weight/?utm_source=chatgpt.com)                                       | Medium     | Related        | Weighted sampling            |

### **Most important algorithm: Reservoir Sampling**

Imagine an infinite stream:

```
10, 20, 30, 40, 50, 60, ...
```

You don't know how many elements will arrive.

You want to select one element uniformly at random.

Reservoir sampling guarantees that every element has the same probability of being selected.

For K = 1:

1. Select the first element.
2. When the second element arrives, replace the selected element with probability 1/2.
3. When the third arrives, replace it with probability 1/3.
4. When the fourth arrives, replace it with probability 1/4.
5. Continue.

At the end, every element has an equal probability of being selected.

For K elements, extend the algorithm to maintain a reservoir of size K.

**My recommendation:** Implement both versions yourself. They are fundamental streaming algorithms and excellent interview discussion topics.

## **Pattern 13: Streaming + Approximation / Probabilistic Data Structures**

This is the most advanced category in this roadmap.

**Core idea:** When the stream is enormous, exact answers may require too much memory.

Instead of storing all elements, maintain a compact summary that provides an answer with a mathematically bounded error or a controlled probability of error.

### **Algorithms you should master**

| Algorithm             | Problem solved          | Important property                                             |
| --------------------- | ----------------------- | -------------------------------------------------------------- |
| Morris Counter        | Approximate counting    | Logarithmic counter storage                                    |
| Bloom Filter          | Membership testing      | False positives possible                                       |
| Count-Min Sketch      | Frequency estimation    | Overestimates frequencies in the standard insertion-only model |
| Count Sketch          | Frequency estimation    | Supports signed updates                                        |
| Misra-Gries           | Heavy hitters           | Bounded candidate counters                                     |
| Space-Saving          | Heavy hitters           | Compact frequency tracking                                     |
| Flajolet-Martin       | Distinct counting       | Probabilistic cardinality estimation                           |
| HyperLogLog           | Distinct counting       | Very compact cardinality estimation                            |
| AMS Sketch            | Frequency moments       | Approximate statistical moments                                |
| Greenwald-Khanna      | Quantiles               | Approximate quantile queries                                   |
| T-Digest              | Quantiles               | Particularly useful for percentile estimation                  |
| Exponential Histogram | Sliding-window counting | Approximate recent-window counts                               |

### **Recommended implementation exercises**

These are not standard LeetCode questions. Treat them as algorithm implementation assignments.

| Exercise                        | Difficulty | Priority  |
| ------------------------------- | ---------- | --------- |
| Implement Bloom Filter          | Medium     | Very High |
| Implement Morris Counter        | Medium     | High      |
| Implement Misra-Gries           | Medium     | Very High |
| Implement Count-Min Sketch      | Hard       | Very High |
| Implement HyperLogLog           | Hard       | High      |
| Implement Reservoir Sampling    | Medium     | Very High |
| Implement Exponential Histogram | Hard       | Medium    |
| Implement Greenwald-Khanna      | Hard       | Medium    |
| Understand T-Digest             | Hard       | Medium    |

Why are these important?

Consider a system receiving 500 million requests per day.

You might need:

- Approximate number of unique users.
- Most frequently accessed APIs.
- Frequency of individual IP addresses.
- Approximate 95th percentile latency.
- Duplicate detection.
- Number of events within a recent time window.

Storing every request in a HashMap is often impractical.

These algorithms are designed for precisely these situations.

For theoretical foundations, the distinction between frequency moments, heavy hitters, cardinality estimation, and sampling is covered in streaming-algorithm literature.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://doi.org\&sz=32)

DOI+2



## **Pattern 14: Streaming + Multiple Operations / Composite Data Structures**

**Core idea:** A single incoming event can affect several different statistics, and you must support multiple queries efficiently.

This is one of the most valuable categories for design-oriented DSA interviews.

For example, imagine receiving stock prices continuously.

You need to support:

- Update the price.
- Get the current price.
- Get the maximum price.
- Get the minimum price.
- Handle corrections to previous prices.

One data structure is not necessarily sufficient.

### **Problems**

| Problem                                                                                                                              | Difficulty | Classification | What you learn                                |
| ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------- | --------------------------------------------- |
| [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                       | Medium     | Core           | Multiple ordered views of streaming data      |
| [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Advanced       | Multiple statistics in one design             |
| [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Core           | Composite ordered partitions                  |
| [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)              | Hard       | Core           | Efficient insertion and median query          |
| [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)   | Hard       | Advanced       | Dynamic ordered ranking                       |
| [352. Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)    | Hard       | Core           | Maintain a dynamic summary of received values |
| [1656. Design an Ordered Stream](https://leetcode.com/problems/design-an-ordered-stream/?utm_source=chatgpt.com)                     | Easy       | Core           | Incremental ordered output                    |
| [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)    | Hard       | Advanced       | Trie + frequency + top K suggestions          |
| [981. Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/?utm_source=chatgpt.com)                  | Medium     | Related        | Time-indexed queries                          |
| [170. Two Sum III - Data Structure Design](https://leetcode.com/problems/two-sum-iii-data-structure-design/?utm_source=chatgpt.com)  | Easy       | Related        | Dynamic insertions and queries                |

### **What you should learn from this category**

- Multiple indexes over the same data.
- Maintaining consistent state across data structures.
- Handling corrections and deletions.
- Lazy deletion.
- Ordered maps and heaps.
- Maintaining running statistics.
- Amortized complexity.

This category is particularly useful because it teaches you to design a streaming data structure rather than simply apply a known algorithm.

# **4. Additional unique problems I strongly recommend**

The previous sections cover the main patterns. However, there are some problems that I would specifically add to your preparation because they expose you to less-obvious streaming concepts.

These are worth solving even if you don't initially recognize them as streaming problems.

| #  | Problem                                                                                                                                                                                                              | Difficulty | Pattern                           | Why it is important                      |
| -- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------- | ---------------------------------------- |
| 1  | [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                                                                                                                | Easy       | Time-based counting               | Event suppression and timestamp tracking |
| 2  | [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                                                                                                                  | Medium     | Time-based window                 | Event counting with expiration           |
| 3  | [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com)                                                                                                          | Easy       | Time-based window                 | Queue-based expiration                   |
| 4  | [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                                                                                                               | Medium     | Frequency + ordering              | Multiple synchronized data structures    |
| 5  | [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)                                                                                           | Medium     | Running product                   | Prefix product with zero handling        |
| 6  | [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)                                                                                                                    | Medium     | Monotonic stack                   | Online monotonic processing              |
| 7  | [1032. Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)                                                                                                             | Hard       | Pattern matching                  | Incremental string matching              |
| 8  | [352. Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)                                                                                    | Hard       | Ordered state                     | Maintain compressed intervals            |
| 9  | [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                                                                                                       | Medium     | Composite state                   | Handle updates and corrections           |
| 10 | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                                                                                                 | Hard       | Sliding window + order statistics | Maintain a trimmed average               |
| 11 | [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)                                                                                   | Hard       | Online ranking                    | Efficiently maintain ranked results      |
| 12 | [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)                                                                                    | Hard       | Frequency + matching + top K      | Combine multiple streaming techniques    |
| 13 | [398. Random Pick Index](https://leetcode.com/problems/random-pick-index/?utm_source=chatgpt.com)                                                                                                                    | Medium     | Sampling                          | Reservoir sampling                       |
| 14 | [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                                                                                                            | Hard       | Window + order statistics         | Dynamic deletion and median              |
| 15 | [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/?utm_source=chatgpt.com) | Medium     | Window + min/max                  | Two monotonic deques                     |

These are particularly good for interviews because they test the ability to maintain state across operations.

# **5. Problems that are genuinely streaming vs problems that merely use streaming techniques**

I want to make this distinction especially clear so that you don't end up classifying every sliding-window or prefix-sum problem as a streaming problem.

Here is my recommended separation.

## **A. Genuine streaming / online data structure problems**

These explicitly involve receiving new data and maintaining a stateful data structure.

**Highest priority:**

| #    | Problem                                      | Main pattern              |
| ---- | -------------------------------------------- | ------------------------- |
| 346  | Moving Average from Data Stream              | Running average           |
| 703  | Kth Largest Element in a Stream              | Top K                     |
| 295  | Find Median from Data Stream                 | Median                    |
| 933  | Number of Recent Calls                       | Time window               |
| 362  | Design Hit Counter                           | Time-window counting      |
| 359  | Logger Rate Limiter                          | Deduplication             |
| 1429 | First Unique Number                          | Frequency + ordering      |
| 1352 | Product of the Last K Numbers                | Running product           |
| 901  | Online Stock Span                            | Monotonic stack           |
| 1032 | Stream of Characters                         | Online matching           |
| 1656 | Design an Ordered Stream                     | Incremental ordering      |
| 2620 | Find Consecutive Integers from a Data Stream | Consecutive counting      |
| 352  | Data Stream as Disjoint Intervals            | Interval maintenance      |
| 295  | Find Median from Data Stream                 | Dynamic order statistics  |
| 2034 | Stock Price Fluctuation                      | Multiple ordered queries  |
| 1825 | Finding MK Average                           | Sliding-window statistics |
| 2102 | Sequentially Ordinal Rank Tracker            | Online ranking            |
| 642  | Design Search Autocomplete System            | Incremental search        |

For the complete data-stream problem family, the LeetCode data-stream index is a useful supplementary reference.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://walkccc.me\&sz=32)

LeetCode Solutions+1



## **B. Streaming-compatible problems**

These are primarily array/string problems, but the same state-maintenance principles apply.

| Problem                                           | Main pattern            |
| ------------------------------------------------- | ----------------------- |
| 1480. Running Sum of 1d Array                     | Running aggregate       |
| 643. Maximum Average Subarray I                   | Moving average          |
| 239. Sliding Window Maximum                       | Window maximum          |
| 480. Sliding Window Median                        | Window order statistics |
| 560. Subarray Sum Equals K                        | Prefix frequency        |
| 525. Contiguous Array                             | Prefix balance          |
| 974. Subarray Sums Divisible by K                 | Prefix remainder        |
| 930. Binary Subarrays With Sum                    | Prefix frequency        |
| 3. Longest Substring Without Repeating Characters | Dynamic distinct window |
| 76. Minimum Window Substring                      | Frequency window        |
| 438. Find All Anagrams in a String                | Window matching         |
| 424. Longest Repeating Character Replacement      | Window frequency        |
| 992. Subarrays with K Different Integers          | Distinct-count window   |
| 739. Daily Temperatures                           | Monotonic stack         |
| 496. Next Greater Element I                       | Monotonic stack         |
| 169. Majority Element                             | Boyer-Moore             |
| 229. Majority Element II                          | Generalized voting      |

**My advice:** Don't spend equal preparation time on both groups. Master the genuine streaming problems first, and then use the streaming-compatible problems to strengthen the underlying techniques.

# **6. The actual learning order I recommend**

I would not study all 14 categories independently and randomly.

Some patterns build on others.

For example:

- Moving Average teaches basic state maintenance.
- Sliding Window Maximum introduces monotonic deques.
- Median from Data Stream introduces two heaps.
- Sliding Window Median combines heaps with deletion.
- Finding MK Average combines window management, ordering, and statistics.

Therefore, follow this progression.

## **Phase 1: Basic streaming fundamentals**

**Goal:** Become comfortable maintaining state as elements arrive.

| Order | Problem                                                                                                                                                  | Pattern              | Priority  |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------- | --------- |
| 1     | [1480. Running Sum of 1d Array](https://leetcode.com/problems/running-sum-of-1d-array/?utm_source=chatgpt.com)                                           | Running aggregate    | High      |
| 2     | [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)                            | Moving average       | Very High |
| 3     | [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)                               | Running product      | Very High |
| 4     | [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)                            | Top K                | Very High |
| 5     | [2620. Find Consecutive Integers from a Data Stream](https://leetcode.com/problems/find-consecutive-integers-from-a-data-stream/?utm_source=chatgpt.com) | Consecutive counting | High      |
| 6     | [1656. Design an Ordered Stream](https://leetcode.com/problems/design-an-ordered-stream/?utm_source=chatgpt.com)                                         | Incremental ordering | High      |

## **Phase 2: Frequency and event processing**

**Goal:** Learn to maintain frequencies, uniqueness, and time-based information.

| Order | Problem                                                                                                     | Pattern              | Priority  |
| ----- | ----------------------------------------------------------------------------------------------------------- | -------------------- | --------- |
| 7     | [169. Majority Element](https://leetcode.com/problems/majority-element/?utm_source=chatgpt.com)             | Majority voting      | Very High |
| 8     | [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)      | Frequency + ordering | Very High |
| 9     | [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)       | Rate limiting        | Very High |
| 10    | [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com) | Time window          | Very High |
| 11    | [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)         | Time-based counting  | Very High |
| 12    | [219. Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/?utm_source=chatgpt.com)   | Window deduplication | High      |

## **Phase 3: Sliding-window streaming**

**Goal:** Master maintaining information about a continuously changing window.

| Order | Problem                                                                                                                                                                                                              | Pattern                | Priority  |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- | --------- |
| 13    | [643. Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/?utm_source=chatgpt.com)                                                                                                  | Fixed window sum       | Very High |
| 14    | [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/?utm_source=chatgpt.com)                                                                                                          | Monotonic deque        | Very High |
| 15    | [2461. Maximum Sum of Distinct Subarrays With Length K](https://leetcode.com/problems/maximum-sum-of-distinct-subarrays-with-length-k/?utm_source=chatgpt.com)                                                       | Sum + frequency        | High      |
| 16    | [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/?utm_source=chatgpt.com) | Min + max              | Very High |
| 17    | [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)                                                                                                                    | Online monotonic stack | Very High |

## **Phase 4: Order statistics and advanced aggregates**

**Goal:** Maintain more complex information about incoming data.

| Order | Problem                                                                                                                              | Pattern              | Priority  |
| ----- | ------------------------------------------------------------------------------------------------------------------------------------ | -------------------- | --------- |
| 18    | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)              | Two heaps            | Very High |
| 19    | [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                       | Multiple indexes     | Very High |
| 20    | [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)   | Online ranking       | High      |
| 21    | [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                            | Dynamic median       | Very High |
| 22    | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Trimmed average      | Very High |
| 23    | [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Composite statistics | High      |

## **Phase 5: Pattern detection and advanced structures**

**Goal:** Handle streams of characters, intervals, and complex queries.

| Order | Problem                                                                                                                             | Pattern                | Priority  |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------- | --------- |
| 24    | [1032. Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)                            | Online string matching | Very High |
| 25    | [352. Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)   | Interval maintenance   | High      |
| 26    | [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)   | Trie + Top K           | Very High |
| 27    | [170. Two Sum III - Data Structure Design](https://leetcode.com/problems/two-sum-iii-data-structure-design/?utm_source=chatgpt.com) | Dynamic frequency      | High      |

## **Phase 6: Theoretical streaming algorithms**

**Goal:** Understand what happens when the stream becomes too large to store.

Implement these independently:

1. Reservoir Sampling.
2. Boyer-Moore Majority Vote.
3. Misra-Gries.
4. Morris Counter.
5. Bloom Filter.
6. Count-Min Sketch.
7. Flajolet-Martin.
8. HyperLogLog.
9. Count Sketch.
10. Greenwald-Khanna.
11. Exponential Histogram.
12. T-Digest.

This phase is important for developing a deeper understanding of streaming algorithms beyond typical coding interviews.

# **7. Master templates: What you should be able to recognize**

When you encounter a streaming problem in an interview, ask yourself these questions.

| Question                                                        | Possible pattern                | Likely data structure      |
| --------------------------------------------------------------- | ------------------------------- | -------------------------- |
| Do I need the smallest/largest value so far?                    | Running min/max                 | Variables                  |
| Do I need the largest/smallest K values?                        | Top K                           | Heap                       |
| Do I need an aggregate of all incoming values?                  | Running aggregate               | Variables                  |
| Do I need an aggregate of the last K values?                    | Fixed window                    | Queue + aggregate          |
| Do old elements expire based on time?                           | Time window                     | Deque / Queue              |
| Do I need frequencies?                                          | Frequency tracking              | HashMap                    |
| Do I need the first unique element?                             | Dynamic uniqueness              | HashMap + LinkedHashSet    |
| Do I need the median?                                           | Online order statistics         | Two Heaps                  |
| Do I need the median of the last K elements?                    | Sliding-window order statistics | Two Heaps / TreeMap        |
| Do I need to identify frequent elements?                        | Heavy hitters                   | Misra-Gries / Space-Saving |
| Do I need distinct counts with limited memory?                  | Cardinality estimation          | HyperLogLog                |
| Do I need to detect previously seen items?                      | Membership testing              | HashSet / Bloom Filter     |
| Do I need to identify a matching suffix?                        | Online matching                 | Trie / KMP                 |
| Do I need a random representative sample?                       | Sampling                        | Reservoir Sampling         |
| Do I need previous elements that can influence future elements? | Monotonic processing            | Stack / Deque              |
| Do I need several statistics simultaneously?                    | Composite streaming             | Multiple data structures   |

## **8. Important interview concepts beyond individual problems**

To truly master streaming algorithms, learn these concepts as well.

### **A. Streaming models**

Understand these three models:

| Model          | Meaning                                                           |
| -------------- | ----------------------------------------------------------------- |
| Insertion-only | Elements can only be added                                        |
| Turnstile      | Elements can be added or removed, potentially with signed updates |
| Sliding window | Only recent elements are relevant                                 |

These models affect which algorithms and data structures are appropriate.

### **B. Memory constraints**

Always ask:

- Can I store every element?
- Can I store a frequency map?
- Must memory remain constant?
- Can I use O(K) memory?
- Is approximate output acceptable?

For example, a HashMap-based frequency solution may be perfectly acceptable for a LeetCode problem but unsuitable for a stream containing billions of distinct identifiers.

### **C. Query types**

Understand whether the interviewer requires:

- Answer after every insertion.
- Answer only when queried.
- Answer for the entire history.
- Answer for the last K elements.
- Answer for the last T seconds.
- Answer approximately.
- Answer with guaranteed exactness.

These distinctions often determine the entire solution.

### **D. Complexity analysis**

For every implementation, analyze:

1. Time per incoming element.
2. Amortized time per incoming element.
3. Query time.
4. Insertion time.
5. Deletion/expiration time.
6. Auxiliary memory.
7. Whether memory grows with stream length.

One particularly important distinction:

**O(1) amortized time per insertion does not mean O(1) memory.**

A HashMap can have O(1) average insertion time and still grow indefinitely.

# **9. My final recommendations: What actually matters for Google / Meta interviews?**

I would prioritize the following.

| Priority | Patterns                               | Importance          |
| -------- | -------------------------------------- | ------------------- |
| P0       | Running aggregates and moving averages | Fundamental         |
| P0       | Sliding window + frequency             | Extremely important |
| P0       | Monotonic deque / stack                | Extremely important |
| P0       | Top K using heaps                      | Extremely important |
| P0       | Prefix state and frequency counting    | Extremely important |
| P0       | Two Heaps / online median              | Extremely important |
| P1       | Time-based windows and event counting  | Very important      |
| P1       | Dynamic frequency and first unique     | Very important      |
| P1       | Composite streaming data structures    | Very important      |
| P1       | Online string matching                 | Important           |
| P2       | Reservoir sampling                     | Advanced knowledge  |
| P2       | Misra-Gries / Heavy Hitters            | Advanced knowledge  |
| P2       | Bloom Filter / Count-Min Sketch        | Advanced knowledge  |
| P2       | HyperLogLog / Approximate Quantiles    | Advanced knowledge  |

For Google and Meta-style DSA rounds, I would spend most of your preparation time on P0 and P1.

P2 is useful for strengthening algorithmic knowledge and preparing for discussions involving large-scale data processing. It is not necessary to implement every probabilistic streaming algorithm in a standard coding interview.

# **10. Recommended learning resources**

These are useful references for the more theoretical side of streaming algorithms.

1\. [**Streaming Model – Algorithm**](https://algorithm.tamnd.com/streaming-model/?utm_source=chatgpt.com) — A useful overview of streaming concepts, including frequency estimation, heavy hitters, sampling, and distinct counting.

2\. [**University of Illinois – Streaming Algorithms Lecture Notes**](https://courses.grainger.illinois.edu/cs574/fa2025/notes/main.pdf?utm_source=chatgpt.com) — Covers frequency moments, reservoir sampling, and streaming algorithm fundamentals.

3\. [**MIT – Streaming Algorithms Lecture Notes**](https://courses.csail.mit.edu/6.856/current/Notes/n16-streaming.html?utm_source=chatgpt.com) — Particularly useful for Count-Min Sketch, frequency estimation, and distinct-element counting.

4\. [**Finding the Frequent Items in Streams of Data**](https://doi.org/10.1145/1562764.1562789?utm_source=chatgpt.com) — An important research survey on heavy hitters.

5\. [**Algorithms and Data Structures for Massive Datasets**](https://www.oreilly.com/library/view/algorithms-and-data/9781617298035/Text/ch11.htm?utm_source=chatgpt.com) — Covers Count-Min Sketch, HyperLogLog, reservoir sampling, and approximate quantiles.

6\. [**LeetCode Data Stream Problem Collection**](https://walkccc.me/LeetCode/topics/system-design/data-stream/?utm_source=chatgpt.com) — Useful for discovering additional online data-structure problems.

# **11. How I would approach mastering this pattern**

My recommendation is to maintain three separate collections in your DSA preparation.

**Collection 1 – Core streaming problems**

Solve the genuine online problems first. Understand how to maintain state, update it, and answer queries.

**Collection 2 – Streaming-compatible problems**

Connect the concepts to sliding window, prefix sum, monotonic stack, frequency counting, and heaps.

**Collection 3 – Advanced streaming algorithms**

Implement probabilistic and approximate algorithms independently.

For every problem, document:

- What information must be retained?
- What information can be discarded?
- What is the invariant?
- What happens when a new element arrives?
- What happens when an old element expires?
- What data structure supports the required operations?
- Can the solution be implemented with less memory?
- Is the solution exact or approximate?

**One final recommendation:** Since you are using Java for DSA preparation, implement the core patterns using Java's `ArrayDeque`, `HashMap`, `HashSet`, `PriorityQueue`, and `TreeMap`. For advanced problems, understand how to combine these structures rather than looking for one universal streaming data structure.

If you master the first five phases and understand the algorithms in Phase 6, you will have a strong foundation in streaming-related DSA problems, from ordinary online queries to advanced data-stream processing.

































































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
























# Streaming Algorithms: Pattern-Wise Mastery Guide

**What "streaming" means in interviews:** elements arrive one at a time, you can't (or shouldn't) re-read the past, memory is bounded, and the answer must be available at any moment. You keep a small **state** and update it per element.

**Legend:** 🔒 = LeetCode Premium · **C** = Core (truly online or stream-style API) · **A** = Applicable (array given, but the same single-pass technique solves it) · E/M/H = difficulty

---

## Pattern Map

| # | Pattern | State you keep | Per-element cost |
|---|---|---|---|
| 1 | Running aggregates (sum/min/max/avg/count/variance) | a few scalars | O(1) |
| 2 | Prefix state + HashMap | running prefix → count/first index | O(1) avg |
| 3 | Sliding window (fixed/variable) | window counts, sum, distinct | O(1) amortized |
| 4 | Monotonic deque/stack | ordered candidates | O(1) amortized |
| 5 | Heap: Top-K, Kth, Median | heap(s) of size K | O(log K) |
| 6 | Frequency / Majority / Heavy hitters | counters | O(1) |
| 7 | Sampling (reservoir, weighted) | sample + count | O(1) |
| 8 | Matching (brackets, pattern, subsequence, trie) | stack / automaton / pointers | O(1) amortized |
| 9 | Interval/range maintenance | sorted map / tree of intervals | O(log n) |
| 10 | Time-based / rate-limiting design | queue or buckets by time | O(1) amortized |
| 11 | Dedup / membership / cycle (O(1) space) | pointers, XOR, hash set | O(1) |
| 12 | Online DP / greedy state | DP vars (Kadane, stock) | O(1) |
| 13 | Order statistics (BIT / segment tree) | tree over values/indices | O(log n) |
| 14 | Iterator / buffered stream | buffer + cursor | O(1) |
| 15 | Probabilistic sketches (theory) | hashed summaries | O(1) |

---

## 1. Running Aggregates (Min / Max / Sum / Avg / Count / Variance)

**Idea:** update a few scalars per element. For the average, keep sum and count. For variance, use **Welford's algorithm** (mean and M2). Min and max are just a running compare.

| Problem | Lvl | Type | Twist |
|---|---|---|---|
| [Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/) 🔒 | E | C | fixed window + running sum |
| [Running Sum of 1d Array](https://leetcode.com/problems/running-sum-of-1d-array/) | E | A | basic prefix |
| [Min Stack](https://leetcode.com/problems/min-stack/) | M | C | running min with rollback |
| [Max Stack](https://leetcode.com/problems/max-stack/) 🔒 | H | C | running max + pop-max |
| [Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/) | M | C | prefix product, reset on 0 |
| [Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/) | M | C | out-of-order updates, two heaps/map |
| [Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | E | A | running min + best diff |
| [Maximum Sum Circular Subarray](https://leetcode.com/problems/maximum-sum-circular-subarray/) | M | A | running max and min together |

## 2. Prefix State + HashMap (Streaming + Count/Match)

**Idea:** while scanning, store `prefix → count` or `prefix → first index`. Then "subarray with property X ending here" becomes a lookup.

| Problem | Lvl | Type |
|---|---|---|
| [Two Sum](https://leetcode.com/problems/two-sum/) | E | A |
| [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | M | A (**the** template) |
| [Contiguous Array](https://leetcode.com/problems/contiguous-array/) | M | A |
| [Subarray Sums Divisible by K](https://leetcode.com/problems/subarray-sums-divisible-by-k/) | M | A |
| [Count Number of Nice Subarrays](https://leetcode.com/problems/count-number-of-nice-subarrays/) | M | A |
| [Binary Subarrays With Sum](https://leetcode.com/problems/binary-subarrays-with-sum/) | M | A |
| [Two Sum III – Data Structure Design](https://leetcode.com/problems/two-sum-iii-data-structure-design/) 🔒 | E | C |

## 3. Sliding Window (Streaming + Window)

**Idea:** two variants.
- **Fixed size:** add the new element, evict the old one.
- **Variable size:** expand right, shrink left while the invariant is broken. Use "at most K" minus "at most K-1" for exact counts.

| Problem | Lvl | Type | Twist |
|---|---|---|---|
| [Maximum Average Subarray I](https://leetcode.com/problems/maximum-average-subarray-i/) | E | A | fixed, avg |
| [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | M | A | variable, distinct |
| [Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/) | M | A | window − maxFreq ≤ k |
| [Fruit Into Baskets](https://leetcode.com/problems/fruit-into-baskets/) | M | A | at most 2 distinct |
| [Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/) | M | A | at most k zeros |
| [Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/) | M | A | shrink-to-minimize |
| [Find All Anagrams in a String](https://leetcode.com/problems/find-all-anagrams-in-a-string/) | M | A | fixed + count matching |
| [Permutation in String](https://leetcode.com/problems/permutation-in-string/) | M | A | same as above |
| [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) | H | A | `need`/`have` counters |
| [Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) | H | A | atMost(K) − atMost(K−1) |
| [Sliding Window Median](https://leetcode.com/problems/sliding-window-median/) | H | C | window + two heaps/multiset |

## 4. Monotonic Deque / Stack (Streaming + Max/Min over a Window)

**Idea:** keep candidates in sorted order and drop dominated ones. Each element is pushed and popped at most once, so the total cost is O(n).

| Problem | Lvl | Type | Twist |
|---|---|---|---|
| [Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | H | C | **the** monotonic deque problem |
| [Online Stock Span](https://leetcode.com/problems/online-stock-span/) | M | C | true streaming monotonic stack |
| [Longest Continuous Subarray With Abs Diff ≤ Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/) | M | A | two deques (min and max) |
| [Shortest Subarray with Sum at Least K](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) | H | A | deque over prefix sums |
| [Constrained Subsequence Sum](https://leetcode.com/problems/constrained-subsequence-sum/) | H | A | DP + deque |
| [Jump Game VI](https://leetcode.com/problems/jump-game-vi/) | M | A | DP + deque |
| [Daily Temperatures](https://leetcode.com/problems/daily-temperatures/) | M | A | next greater |
| [Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/) / [II](https://leetcode.com/problems/next-greater-element-ii/) | E/M | A | circular variant |
| [Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) | H | A | |
| [Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/) | H | A | stack or two pointers |
| [Remove K Digits](https://leetcode.com/problems/remove-k-digits/) | M | A | greedy stack |
| [132 Pattern](https://leetcode.com/problems/132-pattern/) | M | A | |
| [Sum of Subarray Minimums](https://leetcode.com/problems/sum-of-subarray-minimums/) | M | A | contribution technique |
| [Car Fleet](https://leetcode.com/problems/car-fleet/) | M | A | |

## 5. Heaps: Top-K, Kth, Median (Streaming + Order)

**Idea:**
- Kth largest: a min-heap of size K.
- Median: max-heap (lower half) + min-heap (upper half), balanced.
- Top-K frequent: counts + heap, or bucket sort.

| Problem | Lvl | Type |
|---|---|---|
| [Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) | E | C |
| [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | H | C (**must-know**) |
| [Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) | E | A |
| [Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) | M | A |
| [Top K Frequent Words](https://leetcode.com/problems/top-k-frequent-words/) | M | A |
| [IPO](https://leetcode.com/problems/ipo/) | H | A (two-heap pipeline) |
| [Seat Reservation Manager](https://leetcode.com/problems/seat-reservation-manager/) | M | C |
| [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | H | C (k-way stream merge) |
| [Find K Pairs with Smallest Sums](https://leetcode.com/problems/find-k-pairs-with-smallest-sums/) | M | A |
| [Kth Smallest Element in a Sorted Matrix](https://leetcode.com/problems/kth-smallest-element-in-a-sorted-matrix/) | M | A |
| [Smallest Range Covering Elements from K Lists](https://leetcode.com/problems/smallest-range-covering-elements-from-k-lists/) | H | A |
| [Finding MK Average](https://leetcode.com/problems/finding-mk-average/) | H | C (window + 3 ordered groups) |

## 6. Frequency / Count / Majority / Heavy Hitters

**Idea:** counters in O(1) space where possible. **Boyer–Moore** finds a majority element in O(1) space, and its generalization is **Misra–Gries**, which finds elements with frequency > n/k using k−1 counters.

| Problem | Lvl | Type |
|---|---|---|
| [Majority Element](https://leetcode.com/problems/majority-element/) | E | C (Boyer–Moore) |
| [Majority Element II](https://leetcode.com/problems/majority-element-ii/) | M | C (Misra–Gries, k=3) |
| [First Unique Character in a String](https://leetcode.com/problems/first-unique-character-in-a-string/) | E | A |
| [First Unique Number](https://leetcode.com/problems/first-unique-number/) 🔒 | M | C |
| [Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/) | H | C |
| [All O`one Data Structure](https://leetcode.com/problems/all-oone-data-structure/) | H | C |
| [LFU Cache](https://leetcode.com/problems/lfu-cache/) | H | C |
| [Design Underground System](https://leetcode.com/problems/design-underground-system/) | M | C (running avg per key) |
| [Design a Leaderboard](https://leetcode.com/problems/design-a-leaderboard/) 🔒 | M | C |

## 7. Sampling (Reservoir and Weighted)

**Idea:** pick uniformly from a stream of unknown length. Keep the i-th item with probability k/i.

| Problem | Lvl | Type |
|---|---|---|
| [Linked List Random Node](https://leetcode.com/problems/linked-list-random-node/) | M | C (**reservoir, k=1**) |
| [Random Pick Index](https://leetcode.com/problems/random-pick-index/) | M | C (reservoir) |
| [Random Pick with Weight](https://leetcode.com/problems/random-pick-with-weight/) | M | A (prefix + binary search) |
| [Shuffle an Array](https://leetcode.com/problems/shuffle-an-array/) | M | A (Fisher–Yates) |
| [Insert Delete GetRandom O(1)](https://leetcode.com/problems/insert-delete-getrandom-o1/) | M | C |

## 8. Matching (Streaming + Matching)

Four sub-types:

- **Bracket/pair matching (stack):** [Valid Parentheses](https://leetcode.com/problems/valid-parentheses/) (E), [Minimum Add to Make Parentheses Valid](https://leetcode.com/problems/minimum-add-to-make-parentheses-valid/) (M, counter only, O(1) space), [Minimum Remove to Make Valid Parentheses](https://leetcode.com/problems/minimum-remove-to-make-valid-parentheses/) (M), [Longest Valid Parentheses](https://leetcode.com/problems/longest-valid-parentheses/) (H).
- **Online pattern matching (automaton/trie):** [Stream of Characters](https://leetcode.com/problems/stream-of-characters/) (H, **C**; reversed trie or Aho–Corasick), [Find the Index of the First Occurrence in a String](https://leetcode.com/problems/find-the-index-of-the-first-occurrence-in-a-string/) (E, **KMP**).
- **Subsequence matching (pointers):** [Is Subsequence](https://leetcode.com/problems/is-subsequence/) (E), [Number of Matching Subsequences](https://leetcode.com/problems/number-of-matching-subsequences/) (M, stream the text once and advance many waiting pointers).
- **Rolling hash:** [Repeated DNA Sequences](https://leetcode.com/problems/repeated-dna-sequences/) (M), [Longest Duplicate Substring](https://leetcode.com/problems/longest-duplicate-substring/) (H).

## 9. Interval / Range Maintenance

| Problem | Lvl | Type |
|---|---|---|
| [Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/) 🔒 | H | C |
| [My Calendar I](https://leetcode.com/problems/my-calendar-i/) / [II](https://leetcode.com/problems/my-calendar-ii/) / [III](https://leetcode.com/problems/my-calendar-iii/) | M/M/H | C |
| [Range Module](https://leetcode.com/problems/range-module/) | H | C |
| [Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/) 🔒 | M | A (heap of end times) |

## 10. Time-Based / Rate-Limiting Design

**Idea:** a queue of timestamps, or fixed buckets, and evict expired items on each call.

| Problem | Lvl | Type |
|---|---|---|
| [Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/) | E | C |
| [Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/) 🔒 | E | C |
| [Design Hit Counter](https://leetcode.com/problems/design-hit-counter/) 🔒 | M | C (queue vs 300 buckets) |
| [Tweet Counts Per Frequency](https://leetcode.com/problems/tweet-counts-per-frequency/) | M | C |
| [Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/) | M | C |
| [Snapshot Array](https://leetcode.com/problems/snapshot-array/) | M | C |
| [Design Twitter](https://leetcode.com/problems/design-twitter/) | M | C (k-way merge of feeds) |
| [LRU Cache](https://leetcode.com/problems/lru-cache/) | M | C |

## 11. Dedup / Membership / Cycle in O(1) Space

| Problem | Lvl | Type |
|---|---|---|
| [Single Number](https://leetcode.com/problems/single-number/) | E | A (XOR) |
| [Missing Number](https://leetcode.com/problems/missing-number/) | E | A |
| [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) | M | A (Floyd) |
| [First Missing Positive](https://leetcode.com/problems/first-missing-positive/) | H | A |
| [Contains Duplicate II](https://leetcode.com/problems/contains-duplicate-ii/) / [III](https://leetcode.com/problems/contains-duplicate-iii/) | E/H | C (window of seen values; III needs ordered set or buckets) |
| [Longest Consecutive Sequence](https://leetcode.com/problems/longest-consecutive-sequence/) | M | A |
| [Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/) / [II](https://leetcode.com/problems/linked-list-cycle-ii/) | E/M | A |
| [Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/) | E | A |
| [Remove Nth Node From End of List](https://leetcode.com/problems/remove-nth-node-from-end-of-list/) | M | A |

## 12. Online DP / Greedy State

| Problem | Lvl |
|---|---|
| [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) (Kadane) | M |
| [Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/) (track max and min) | M |
| [House Robber](https://leetcode.com/problems/house-robber/) | M |
| [Gas Station](https://leetcode.com/problems/gas-station/) | M |
| [Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/) (patience tails array) | M |

## 13. Order Statistics with BIT / Segment Tree

| Problem | Lvl |
|---|---|
| [Range Sum Query – Mutable](https://leetcode.com/problems/range-sum-query-mutable/) | M |
| [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) | H |
| [Reverse Pairs](https://leetcode.com/problems/reverse-pairs/) | H |

## 14. Iterator / Buffered Stream

| Problem | Lvl | Twist |
|---|---|---|
| [Peeking Iterator](https://leetcode.com/problems/peeking-iterator/) | M | lookahead buffer |
| [Flatten Nested List Iterator](https://leetcode.com/problems/flatten-nested-list-iterator/) | M | lazy evaluation |
| [Binary Search Tree Iterator](https://leetcode.com/problems/binary-search-tree-iterator/) | M | O(h) memory |
| [Zigzag Iterator](https://leetcode.com/problems/zigzag-iterator/) 🔒 | M | k-way round robin |
| [Read N Characters Given Read4 II](https://leetcode.com/problems/read-n-characters-given-read4-ii-call-multiple-times/) 🔒 | H | leftover buffer across calls |

## 15. Probabilistic Sketches (Theory for Senior and System-Design Rounds)

These aren't LeetCode problems, but they come up as "what if the data doesn't fit in memory?" follow-ups.

| Structure | Solves | Reference |
|---|---|---|
| Reservoir sampling | uniform sample | [Wikipedia](https://en.wikipedia.org/wiki/Reservoir_sampling) |
| Bloom filter | approximate membership | [Wikipedia](https://en.wikipedia.org/wiki/Bloom_filter) |
| Count-Min Sketch | approximate frequency | [Wikipedia](https://en.wikipedia.org/wiki/Count%E2%80%93min_sketch) |
| HyperLogLog | approximate distinct count | [Wikipedia](https://en.wikipedia.org/wiki/HyperLogLog) |
| Misra–Gries / Space-Saving | top-K heavy hitters | [Wikipedia](https://en.wikipedia.org/wiki/Misra%E2%80%93Gries_summary) |
| Boyer–Moore voting | majority | [Wikipedia](https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_majority_vote_algorithm) |
| General overview | | [Streaming algorithm](https://en.wikipedia.org/wiki/Streaming_algorithm) |

---

## Recommended "Must-Master" Core List (in order)

Do these in sequence. Each one teaches a distinct idea, and together they cover nearly every streaming question you'll meet.

| # | Problem | What it teaches |
|---|---|---|
| 1 | [Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/) 🔒 | fixed window + running sum |
| 2 | [Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/) | time-window queue |
| 3 | [Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/) | size-K heap |
| 4 | [Min Stack](https://leetcode.com/problems/min-stack/) | running min with state history |
| 5 | [Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/) | running min + best |
| 6 | [Maximum Subarray](https://leetcode.com/problems/maximum-subarray/) | Kadane's online state |
| 7 | [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/) | prefix + hashmap |
| 8 | [Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/) | variable window |
| 9 | [Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/) | window with need/have |
| 10 | [Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/) | monotonic deque |
| 11 | [Online Stock Span](https://leetcode.com/problems/online-stock-span/) | online monotonic stack |
| 12 | [Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/) | two heaps |
| 13 | [Sliding Window Median](https://leetcode.com/problems/sliding-window-median/) | window + two heaps + lazy deletion |
| 14 | [Majority Element II](https://leetcode.com/problems/majority-element-ii/) | Misra–Gries |
| 15 | [Linked List Random Node](https://leetcode.com/problems/linked-list-random-node/) | reservoir sampling |
| 16 | [Stream of Characters](https://leetcode.com/problems/stream-of-characters/) | online matching with a trie |
| 17 | [Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/) 🔒 | interval maintenance |
| 18 | [Design Hit Counter](https://leetcode.com/problems/design-hit-counter/) 🔒 | bucketed time window |
| 19 | [Finding MK Average](https://leetcode.com/problems/finding-mk-average/) | window + ordered partitions |
| 20 | [Merge k Sorted Lists](https://leetcode.com/problems/merge-k-sorted-lists/) | k-way stream merge |
| 21 | [Subarrays with K Different Integers](https://leetcode.com/problems/subarrays-with-k-different-integers/) | atMost(K) trick |
| 22 | [Shortest Subarray with Sum at Least K](https://leetcode.com/problems/shortest-subarray-with-sum-at-least-k/) | deque over prefix sums |
| 23 | [Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/) | monotonic stack boundaries |
| 24 | [Maximum Frequency Stack](https://leetcode.com/problems/maximum-frequency-stack/) | frequency → stack buckets |
| 25 | [LFU Cache](https://leetcode.com/problems/lfu-cache/) | O(1) frequency bookkeeping |
| 26 | [Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/) | out-of-order corrections |
| 27 | [Read N Characters Given Read4 II](https://leetcode.com/problems/read-n-characters-given-read4-ii-call-multiple-times/) 🔒 | stateful buffering |
| 28 | [Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/) | BIT for online ranks |

---

## Interview Recognition Cheat Sheet

| If the prompt says… | Reach for… |
|---|---|
| "last K / last N seconds" | sliding window (queue, or buckets for time) |
| "max/min in the last K" | monotonic deque |
| "next/previous greater/smaller", "span" | monotonic stack |
| "Kth largest/smallest so far" | min-heap of size K |
| "median so far" | two heaps |
| "subarray with sum/count = K" | prefix + hashmap |
| "at most K distinct / exactly K" | variable window, atMost(K) − atMost(K−1) |
| "most frequent / appears > n/k times" | Boyer–Moore / Misra–Gries |
| "random element from unknown length" | reservoir sampling |
| "constant extra space" | two pointers, XOR, Floyd, in-place marking |
| "stream of characters, match words" | trie or Aho–Corasick, KMP |
| "merge/insert into ranges online" | sorted map / TreeMap |
| "doesn't fit in memory" | sketches (Bloom, Count-Min, HLL) |

## Follow-Up Questions to Prepare For

1. "What if the data is infinite?" → bounded state, so name what you evict.
2. "What if elements arrive out of order?" → buffer plus a watermark, or an ordered map.
3. "What if there are multiple threads/consumers?" → locks, or sharded counters merged on read.
4. "Can you do it in O(1) space?" → Boyer–Moore, Floyd, XOR, in-place marking.
5. "What if it can't fit on one machine?" → partition by key, then merge summaries (sketches are mergeable).
6. "Exact vs approximate?" → exact needs O(n) in the worst case, and sketches trade accuracy for memory.

## Study Plan

- **Week 1:** Patterns 1–3 (aggregates, prefix+map, windows). Core list #1–9.
- **Week 2:** Patterns 4–5 (monotonic, heaps). Core list #10–14, 20, 22, 23.
- **Week 3:** Patterns 6–10 (frequency, sampling, matching, intervals, time-based). Core list #15–19, 24–26.
- **Week 4:** Patterns 11–15 plus redo. Core list #27–28, then re-solve every problem you got wrong without hints.

After each problem, write one line: *"State = ___, update rule = ___, invariant = ___."* If you can fill that in for any new problem within a minute, you've mastered the pattern.

I can turn this into an editable doc with a checkbox tracker, or write template code in Python/Java/C++ for the key patterns (deque, two heaps, reservoir, Boyer–Moore, prefix map), if you'd like.






































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


































# **Streaming Algorithms – Consolidated Final Problem Roadmap**

This is the consolidated list I recommend following for your DSA preparation.

I've applied a stricter filter than the previous list:

1. Include problems that genuinely involve incoming data, incremental processing, online queries, or maintaining state across operations.
2. Separate pure streaming problems from problems that merely use a similar technique on a given array.
3. Do not force ordinary sliding-window, prefix-sum, monotonic-stack, or heap problems into the streaming category.
4. Prioritize problems that teach a distinct streaming concept. Avoid redundant problems.
5. Provide one final must-solve sequence.

**Classification:**

- **P0 – Must solve:** Essential streaming pattern and interview-level implementation.
- **P1 – Strongly recommended:** Important extensions, combinations, and advanced data structures.
- **P2 – Advanced:** Specialized streaming algorithms and less common interview problems.

The LeetCode Data Stream collection supports most of the genuine online problem selections below.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://walkccc.me\&sz=32)

LeetCode Solutions+2



# **1. Final pattern classification**

| #  | Pattern                                     | Priority | What it covers                                             |
| -- | ------------------------------------------- | -------- | ---------------------------------------------------------- |
| 1  | Streaming + Running Aggregates              | P0       | Sum, product, average, min/max                             |
| 2  | Streaming + Frequency / Counting / Majority | P0       | Frequency, uniqueness, consecutive counts, majority        |
| 3  | Streaming + Fixed / Time-Based Window       | P0       | Last K elements, last T seconds, expiration                |
| 4  | Streaming + Monotonic Stack / Deque         | P0       | Online next greater, span, window min/max                  |
| 5  | Streaming + Top K / Kth Largest             | P0       | Maintain top K elements                                    |
| 6  | Streaming + Median / Order Statistics       | P0       | Running median, sliding median, ranked elements            |
| 7  | Streaming + Pattern Matching                | P1       | Character streams, suffix matching                         |
| 8  | Streaming + Interval / Range Maintenance    | P1       | Dynamic intervals and ordered summaries                    |
| 9  | Streaming + Sampling                        | P1       | Reservoir sampling                                         |
| 10 | Streaming + Composite Data Structures       | P1       | Multiple queries, corrections, combined state              |
| 11 | Streaming + Iterator / Buffered Processing  | P1       | Stateful reading, buffering, incremental output            |
| 12 | Streaming + Approximation / Sketching       | P2       | Bloom Filter, Count-Min Sketch, HyperLogLog, heavy hitters |

**Important classification decision:** Prefix Sum + HashMap is not included as a core streaming category. It is an excellent one-pass array technique, but its state can grow with the number of distinct prefix values. Similarly, ordinary array sliding-window problems are not automatically streaming problems.

# **2. Consolidated problem list – Pattern-wise and priority-wise**

## **Pattern 1: Streaming + Running Aggregates**

**Core concept:** Maintain an aggregate as new elements arrive without recalculating the entire history.

| Priority | Problem                                                                                                                                                  | Difficulty | What it teaches                                         | Applicability                     |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------- | --------------------------------- |
| P0       | [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)                            | Easy       | Running sum + fixed-size queue                          | True streaming                    |
| P0       | [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)                               | Medium     | Prefix product + zero handling                          | True streaming                    |
| P1       | [2526. Find Consecutive Integers from a Data Stream](https://leetcode.com/problems/find-consecutive-integers-from-a-data-stream/?utm_source=chatgpt.com) | Medium     | Maintain consecutive matching count                     | True streaming                    |
| P1       | [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                                           | Medium     | Maintain current, minimum and maximum under corrections | Stateful updates, not append-only |
| P1       | [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com)                     | Hard       | Maintain multiple statistics                            | Stateful online design            |

**Concepts to master:**

- Running sum and count.
- Running average.
- Running product.
- Zero-reset handling.
- Running min/max.
- Incremental variance using Welford's algorithm.

**Not applicable / exclude:**

-

1480. Running Sum of 1d Array – ordinary array traversal.

1)

643. Maximum Average Subarray I – array-based fixed window.

1)

121. Best Time to Buy and Sell Stock – array optimization, not a stream API.

1)

53. Maximum Subarray – Kadane's algorithm, not a streaming data structure.

## **Pattern 2: Streaming + Frequency / Counting / Majority**

**Core concept:** Maintain occurrence counts or a compact representation of frequently occurring elements.

| Priority | Problem                                                                                                                                                  | Difficulty | What it teaches                       | Applicability                 |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------- | ----------------------------- |
| P0       | [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                                                   | Medium     | Frequency + insertion order           | True streaming                |
| P0       | [169. Majority Element](https://leetcode.com/problems/majority-element/?utm_source=chatgpt.com)                                                          | Easy       | Boyer-Moore voting                    | One-pass streaming-compatible |
| P1       | [229. Majority Element II](https://leetcode.com/problems/majority-element-ii/?utm_source=chatgpt.com)                                                    | Medium     | Generalized Boyer-Moore / Misra-Gries | One-pass streaming-compatible |
| P1       | [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                                                    | Easy       | Track message and last timestamp      | True online design            |
| P1       | [2526. Find Consecutive Integers from a Data Stream](https://leetcode.com/problems/find-consecutive-integers-from-a-data-stream/?utm_source=chatgpt.com) | Medium     | Consecutive count                     | True streaming                |

**Concepts to master:**

- HashMap frequency tracking.
- First unique element.
- HashMap + LinkedHashSet / Queue.
- Boyer-Moore majority vote.
- Misra-Gries heavy hitters.
- Constant-space frequency approximation.

**Not applicable / exclude:**

-

347. Top K Frequent Elements – primarily batch frequency + heap.

1)

387. First Unique Character in a String – batch string frequency.

1)

451. Sort Characters By Frequency – batch frequency sorting.

1)

992. Subarrays with K Different Integers – variable sliding window, not a stream API.

## **Pattern 3: Streaming + Fixed / Time-Based Window**

**Core concept:** Retain only the relevant recent elements and expire old information.

| Priority | Problem                                                                                                                       | Difficulty | What it teaches                     | Applicability             |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- | ---------- | ----------------------------------- | ------------------------- |
| P0       | [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com)                   | Easy       | Timestamp queue + expiration        | True streaming            |
| P0       | [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com) | Easy       | Fixed-size window                   | True streaming            |
| P0       | [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                           | Medium     | Time-based event counting           | True streaming            |
| P1       | [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                         | Easy       | Time-based event suppression        | True online design        |
| P1       | [1348. Tweet Counts Per Frequency](https://leetcode.com/problems/tweet-counts-per-frequency/?utm_source=chatgpt.com)          | Medium     | Time-bucket aggregation             | Stateful event processing |
| P1       | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                          | Hard       | Sliding window + ordered statistics | True streaming            |

**Concepts to master:**

- Fixed-size queue.
- Timestamp-based expiration.
- Time buckets.
- Lazy expiration.
- Queue vs circular array.
- Sliding-window aggregate.
- Handling multiple events with identical timestamps.

**Not applicable / exclude:**

-

239. Sliding Window Maximum – array-based problem, although it teaches the same deque technique.

1)

643. Maximum Average Subarray I – array-based window.

1)

3. Longest Substring Without Repeating Characters – array/string window.

1)

76. Minimum Window Substring – array/string window.

These belong to the general Sliding Window pattern, not the strict Streaming Data Structure category.

## **Pattern 4: Streaming + Monotonic Stack / Deque**

**Core concept:** Maintain only useful candidates and discard elements that can no longer affect future answers.

| Priority | Problem                                                                                                                                                                                                              | Difficulty | What it teaches                   | Applicability    |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------- | ---------------- |
| P0       | [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)                                                                                                                    | Medium     | Online monotonic decreasing stack | True streaming   |
| P1       | [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/?utm_source=chatgpt.com)                                                                                                          | Hard       | Monotonic deque                   | Array-compatible |
| P1       | [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/?utm_source=chatgpt.com) | Medium     | Two monotonic deques              | Array-compatible |

**Concepts to master:**

- Monotonic increasing/decreasing stack.
- Monotonic deque.
- Removing dominated candidates.
- Amortized O(1) operations.
- Maintaining minimum and maximum simultaneously.

**Not applicable / exclude:**

-

739. Daily Temperatures.

1)

496. Next Greater Element I.

1)

503. Next Greater Element II.

1)

84. Largest Rectangle in Histogram.

1)

42. Trapping Rain Water.

These are useful monotonic-stack problems, but they are not inherently streaming data-structure problems. Don't include them in your final streaming collection.

## **Pattern 5: Streaming + Top K / Kth Largest**

**Core concept:** Maintain the K most relevant elements while new elements arrive.

| Priority | Problem                                                                                                                            | Difficulty | What it teaches                      | Applicability          |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------ | ---------------------- |
| P0       | [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)      | Easy       | Min-heap of size K                   | True streaming         |
| P1       | [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com) | Hard       | Incremental ranked retrieval         | True online design     |
| P1       | [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)  | Hard       | Trie + frequency + top K suggestions | Stateful online design |

**Concepts to master:**

- Min-heap of size K.
- Maintaining top K dynamically.
- Heap insertion and removal.
- Ranking with tie-breaking.
- Combining Trie and heap.
- Frequency-based top K.

**Not applicable / exclude:**

-

347. Top K Frequent Elements.

1)

692. Top K Frequent Words.

1)

215. Kth Largest Element in an Array.

1)

973. K Closest Points to Origin.

These are batch-processing problems, not genuine streaming APIs.

## **Pattern 6: Streaming + Median / Order Statistics**

**Core concept:** Maintain the ordering of incoming elements and answer median or rank queries without sorting the entire stream.

| Priority | Problem                                                                                                                              | Difficulty | What it teaches                  | Applicability               |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | -------------------------------- | --------------------------- |
| P0       | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)              | Hard       | Two heaps                        | True streaming              |
| P1       | [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                            | Hard       | Median + deletion + lazy removal | Streaming-compatible window |
| P1       | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Three ordered partitions         | True streaming              |
| P1       | [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Multiple order statistics        | Stateful online design      |

**Concepts to master:**

- Two heaps.
- Heap balancing.
- Lazy deletion.
- TreeMap as a multiset.
- Maintaining Kth smallest/largest.
- Three-partition ordered structures.
- Median and trimmed average.

**Not applicable / exclude:**

-

215. Kth Largest Element in an Array.

1)

378. Kth Smallest Element in a Sorted Matrix.

1)

973. K Closest Points to Origin.

These are static input problems.

## **Pattern 7: Streaming + Matching / Pattern Detection**

**Core concept:** Process incoming characters or events incrementally and identify matches without restarting the search from scratch.

| Priority | Problem                                                                                                                       | Difficulty | What it teaches                                        | Applicability        |
| -------- | ----------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------ | -------------------- |
| P0       | [1032. Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)                      | Hard       | Reversed Trie + suffix matching                        | True streaming       |
| P1       | [792. Number of Matching Subsequences](https://leetcode.com/problems/number-of-matching-subsequences/?utm_source=chatgpt.com) | Medium     | Multiple waiting pointers while scanning a text stream | Streaming-compatible |

**Concepts to master:**

- Trie-based online matching.
- Reversed Trie.
- KMP prefix-function.
- Aho-Corasick automaton.
- Multiple subsequence pointers.

**Not applicable / exclude:**

-

28. Find the Index of the First Occurrence in a String.

1)

459. Repeated Substring Pattern.

1)

187. Repeated DNA Sequences.

1)

1392. Longest Happy Prefix.

These are ordinary string-matching problems, not inherently online stream APIs.

## **Pattern 8: Streaming + Interval / Range Maintenance**

**Core concept:** Maintain a compact representation of intervals as new values or events arrive.

| Priority | Problem                                                                                                                           | Difficulty | What it teaches                                 | Applicability          |
| -------- | --------------------------------------------------------------------------------------------------------------------------------- | ---------- | ----------------------------------------------- | ---------------------- |
| P0       | [352. Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com) | Hard       | Merge incoming values into intervals            | True streaming         |
| P1       | [715. Range Module](https://leetcode.com/problems/range-module/?utm_source=chatgpt.com)                                           | Hard       | Dynamic interval insertion, removal and queries | Stateful online design |
| P1       | [729. My Calendar I](https://leetcode.com/problems/my-calendar-i/?utm_source=chatgpt.com)                                         | Medium     | Online interval insertion and overlap checking  | Stateful online design |
| P2       | [731. My Calendar II](https://leetcode.com/problems/my-calendar-ii/?utm_source=chatgpt.com)                                       | Medium     | Overlap counting and interval management        | Stateful online design |
| P2       | [732. My Calendar III](https://leetcode.com/problems/my-calendar-iii/?utm_source=chatgpt.com)                                     | Hard       | Dynamic maximum overlap                         | Stateful online design |

**Concepts to master:**

- TreeMap.
- Interval merging.
- Floor and ceiling entries.
- Overlap detection.
- Dynamic interval updates.
- Ordered range queries.

**Not applicable / exclude:**

-

56. Merge Intervals.

1)

57. Insert Interval.

1)

986. Interval List Intersections.

These are batch interval problems, not streaming data structures.

## **Pattern 9: Streaming + Sampling**

**Core concept:** Select a representative random sample from a stream without knowing its final size or storing the entire stream.

| Priority | Problem                                                                                                       | Difficulty | What it teaches           | Applicability        |
| -------- | ------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------- | -------------------- |
| P0       | [398. Random Pick Index](https://leetcode.com/problems/random-pick-index/?utm_source=chatgpt.com)             | Medium     | Reservoir sampling        | Streaming-compatible |
| P1       | [382. Linked List Random Node](https://leetcode.com/problems/linked-list-random-node/?utm_source=chatgpt.com) | Medium     | Reservoir sampling, K = 1 | Streaming-compatible |

**Additional implementation exercises:**

| Priority | Algorithm                   | What to implement                 |
| -------- | --------------------------- | --------------------------------- |
| P0       | Reservoir Sampling K = 1    | Uniformly select one element      |
| P1       | Reservoir Sampling K        | Uniformly select K elements       |
| P2       | Weighted Reservoir Sampling | Maintain a weighted random sample |

**Not applicable / exclude:**

-

528. Random Pick with Weight – prefix sums + binary search.

1)

384. Shuffle an Array – Fisher-Yates shuffle.

1)

380. Insert Delete GetRandom O(1) – dynamic collection with random access.

These are randomization problems, not reservoir sampling.

## **Pattern 10: Streaming + Composite Data Structures**

**Core concept:** Maintain multiple pieces of information and support several operations efficiently.

| Priority | Problem                                                                                                                              | Difficulty | What it teaches                          | Applicability          |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------ | ---------- | ---------------------------------------- | ---------------------- |
| P0       | [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                       | Medium     | Current value + min + max + corrections  | Stateful online design |
| P1       | [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                               | Medium     | Frequency + ordered uniqueness           | True streaming         |
| P1       | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                 | Hard       | Queue + ordered partitions + running sum | True streaming         |
| P1       | [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)    | Hard       | Trie + frequency + ranking               | Stateful online design |
| P1       | [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com) | Hard       | Multiple statistics maintained together  | Stateful online design |
| P1       | [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)   | Hard       | Incremental ranking and retrieval        | True online design     |

**Concepts to master:**

- Multiple indexes over the same data.
- HashMap + Heap.
- HashMap + TreeMap.
- Queue + TreeMap.
- Lazy deletion.
- Handling corrections and duplicate values.
- Maintaining invariants across multiple structures.

Some of these problems appear in earlier categories because they genuinely combine multiple patterns. That is intentional; they should only be solved once.

## **Pattern 11: Streaming + Iterator / Buffered Processing**

**Core concept:** Read data incrementally while retaining unread data or a small lookahead buffer across multiple calls.

| Priority | Problem                                                                                                                                                              | Difficulty | What it teaches                                   | Applicability           |
| -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------- | ----------------------- |
| P0       | [1586. Binary Search Tree Iterator II](https://leetcode.com/problems/binary-search-tree-iterator-ii/?utm_source=chatgpt.com)                                         | Medium     | Stateful iterator with backward movement          | Stateful iterator       |
| P1       | [284. Peeking Iterator](https://leetcode.com/problems/peeking-iterator/?utm_source=chatgpt.com)                                                                      | Medium     | Lookahead buffering                               | True iterator design    |
| P1       | [281. Zigzag Iterator](https://leetcode.com/problems/zigzag-iterator/?utm_source=chatgpt.com)                                                                        | Medium     | Multiple input streams and round-robin processing | Multi-stream processing |
| P1       | [604. Design Compressed String Iterator](https://leetcode.com/problems/design-compressed-string-iterator/?utm_source=chatgpt.com)                                    | Easy       | Lazy character generation                         | Stateful iterator       |
| P1       | [Flatten Nested List Iterator](https://leetcode.com/problems/flatten-nested-list-iterator/?utm_source=chatgpt.com)                                                   | Medium     | Lazy traversal                                    | Stateful iterator       |
| P1       | [Read N Characters Given Read4 II – Call Multiple Times](https://leetcode.com/problems/read-n-characters-given-read4-ii-call-multiple-times/?utm_source=chatgpt.com) | Hard       | Persistent buffer across calls                    | True buffered streaming |

**Concepts to master:**

- Iterator state.
- Lazy evaluation.
- Lookahead buffering.
- Maintaining unread data.
- Multiple input stream merging.
- Handling partial reads.

## **Pattern 12: Streaming + Approximation / Probabilistic Sketches**

This is an advanced algorithmic category. These are implementation exercises rather than conventional LeetCode problems.

| Priority | Algorithm             | Purpose                 | What to learn                        |
| -------- | --------------------- | ----------------------- | ------------------------------------ |
| P0       | Boyer-Moore           | Majority element        | Constant-space candidate tracking    |
| P0       | Reservoir Sampling    | Random sampling         | Uniform selection                    |
| P1       | Misra-Gries           | Heavy hitters           | Bounded frequency counters           |
| P1       | Bloom Filter          | Membership testing      | False positives and bit arrays       |
| P1       | Count-Min Sketch      | Frequency estimation    | Hash-based approximate counting      |
| P2       | Morris Counter        | Approximate counting    | Probabilistic increment              |
| P2       | HyperLogLog           | Distinct cardinality    | Probabilistic unique counting        |
| P2       | Count Sketch          | Frequency estimation    | Signed hash counters                 |
| P2       | Greenwald-Khanna      | Quantile estimation     | Bounded-memory approximate quantiles |
| P2       | Exponential Histogram | Sliding-window counting | Approximate expiration               |
| P2       | T-Digest              | Percentile estimation   | Approximate quantiles                |

For implementation and theory, MIT's streaming lecture notes and the University of Illinois streaming lecture notes are particularly useful.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://courses.csail.mit.edu\&sz=32)

courses.csail.mit.edu+3



# **3. Problems I would exclude from the Streaming collection**

These problems are useful for DSA, but I would not put them in your dedicated Streaming Algorithms problem list.

| General DSA pattern       | Problems to keep in their original pattern                                                                                                                              |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Prefix Sum + HashMap      | 560. Subarray Sum Equals K; 525. Contiguous Array; 974. Subarray Sums Divisible by K; 930. Binary Subarrays With Sum                                                    |
| Ordinary Sliding Window   | 3. Longest Substring Without Repeating Characters; 76. Minimum Window Substring; 424. Longest Repeating Character Replacement; 992. Subarrays with K Different Integers |
| Ordinary Monotonic Stack  | 739. Daily Temperatures; 84. Largest Rectangle in Histogram; 496. Next Greater Element                                                                                  |
| Batch Top K               | 347. Top K Frequent Elements; 692. Top K Frequent Words; 215. Kth Largest Element in an Array                                                                           |
| Batch Interval Processing | 56. Merge Intervals; 57. Insert Interval; 986. Interval List Intersections                                                                                              |
| General DP / Greedy       | 53. Maximum Subarray; 121. Best Time to Buy and Sell Stock; 198. House Robber                                                                                           |
| General Randomization     | 384. Shuffle an Array; 528. Random Pick with Weight                                                                                                                     |

These problems can be solved in one pass or use similar data structures, but that alone does not make them streaming problems.

**Important distinction:** 239 Sliding Window Maximum and 480 Sliding Window Median are streaming-compatible algorithms, but their LeetCode interfaces provide an entire array. They are useful extensions, not strict online stream APIs.

# **4. Final must-solve list – My recommended sequence**

This is the list I would actually maintain in your DSA tracker.

I've removed duplicate problems across categories and prioritized learning a new streaming concept with each problem.

**Total: 24 problems.**

| Order | Problem                                                                                                                                                  | Difficulty | Pattern                             | Priority |
| ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | ----------------------------------- | -------- |
| 1     | [346. Moving Average from Data Stream](https://leetcode.com/problems/moving-average-from-data-stream/?utm_source=chatgpt.com)                            | Easy       | Running aggregate + fixed window    | P0       |
| 2     | [1352. Product of the Last K Numbers](https://leetcode.com/problems/product-of-the-last-k-numbers/?utm_source=chatgpt.com)                               | Medium     | Running product                     | P0       |
| 3     | [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/?utm_source=chatgpt.com)                                              | Easy       | Time-based sliding window           | P0       |
| 4     | [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/?utm_source=chatgpt.com)                            | Easy       | Top K / Kth largest                 | P0       |
| 5     | [2620. Find Consecutive Integers from a Data Stream](https://leetcode.com/problems/find-consecutive-integers-from-a-data-stream/?utm_source=chatgpt.com) | Medium     | Consecutive counting                | P0       |
| 6     | [1429. First Unique Number](https://leetcode.com/problems/first-unique-number/?utm_source=chatgpt.com)                                                   | Medium     | Frequency + ordering                | P0       |
| 7     | [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/?utm_source=chatgpt.com)                                                        | Medium     | Monotonic stack                     | P0       |
| 8     | [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/?utm_source=chatgpt.com)                                  | Hard       | Two heaps                           | P0       |
| 9     | [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/?utm_source=chatgpt.com)                                                      | Medium     | Time-based counting                 | P0       |
| 10    | [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/?utm_source=chatgpt.com)                                                    | Easy       | Event deduplication + timestamps    | P1       |
| 11    | [169. Majority Element](https://leetcode.com/problems/majority-element/?utm_source=chatgpt.com)                                                          | Easy       | Boyer-Moore                         | P1       |
| 12    | [229. Majority Element II](https://leetcode.com/problems/majority-element-ii/?utm_source=chatgpt.com)                                                    | Medium     | Misra-Gries                         | P1       |
| 13    | [1032. Stream of Characters](https://leetcode.com/problems/stream-of-characters/?utm_source=chatgpt.com)                                                 | Hard       | Online pattern matching             | P1       |
| 14    | [352. Data Stream as Disjoint Intervals](https://leetcode.com/problems/data-stream-as-disjoint-intervals/?utm_source=chatgpt.com)                        | Hard       | Interval maintenance                | P1       |
| 15    | [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/?utm_source=chatgpt.com)                                           | Medium     | Composite state + corrections       | P1       |
| 16    | [1656. Design an Ordered Stream](https://leetcode.com/problems/design-an-ordered-stream/?utm_source=chatgpt.com)                                         | Easy       | Incremental ordering                | P1       |
| 17    | [480. Sliding Window Median](https://leetcode.com/problems/sliding-window-median/?utm_source=chatgpt.com)                                                | Hard       | Sliding window + order statistics   | P1       |
| 18    | [1825. Finding MK Average](https://leetcode.com/problems/finding-mk-average/?utm_source=chatgpt.com)                                                     | Hard       | Sliding window + ordered partitions | P1       |
| 19    | [2102. Sequentially Ordinal Rank Tracker](https://leetcode.com/problems/sequentially-ordinal-rank-tracker/?utm_source=chatgpt.com)                       | Hard       | Online ranking                      | P1       |
| 20    | [642. Design Search Autocomplete System](https://leetcode.com/problems/design-search-autocomplete-system/?utm_source=chatgpt.com)                        | Hard       | Trie + frequency + Top K            | P1       |
| 21    | [3369. Design an Array Statistics Tracker](https://leetcode.com/problems/design-an-array-statistics-tracker/?utm_source=chatgpt.com)                     | Hard       | Composite statistics                | P1       |
| 22    | [398. Random Pick Index](https://leetcode.com/problems/random-pick-index/?utm_source=chatgpt.com)                                                        | Medium     | Reservoir sampling                  | P1       |
| 23    | [284. Peeking Iterator](https://leetcode.com/problems/peeking-iterator/?utm_source=chatgpt.com)                                                          | Medium     | Buffered iterator                   | P1       |
| 24    | [Read N Characters Given Read4 II](https://leetcode.com/problems/read-n-characters-given-read4-ii-call-multiple-times/?utm_source=chatgpt.com)           | Hard       | Stateful buffered reading           | P1       |

A few classification notes:

- Problems 11 and 12 are one-pass streaming-compatible algorithms, not dedicated online APIs.
- Problem 17 is a batch-array interface, but teaches the important sliding-window insertion/deletion problem.
- Problem 22 is a streaming-compatible reservoir-sampling exercise.
- Problems 23 and 24 belong to iterator/buffered-stream processing rather than numeric stream aggregation.
- Problems 15, 19, 20 and 21 are stateful online designs, not necessarily append-only streams.

The data-stream index also identifies the central online problems such as 295, 703, 901, 933, 1032, 1352, 1429 and 1825.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://walkccc.me\&sz=32)

LeetCode Solutions+2



# **5. Advanced implementation list (outside LeetCode)**

After the 24 problems, implement these algorithms independently.

| Order | Algorithm                       | Priority | Main objective                      |
| ----- | ------------------------------- | -------- | ----------------------------------- |
| 1     | Reservoir Sampling (K = 1)      | P0       | Uniform random selection            |
| 2     | Reservoir Sampling (K elements) | P1       | Fixed-size uniform sample           |
| 3     | Misra-Gries                     | P1       | Bounded-memory heavy hitters        |
| 4     | Bloom Filter                    | P1       | Approximate membership              |
| 5     | Count-Min Sketch                | P1       | Approximate frequency               |
| 6     | Morris Counter                  | P2       | Approximate counting                |
| 7     | HyperLogLog                     | P2       | Approximate distinct count          |
| 8     | Count Sketch                    | P2       | Frequency estimation                |
| 9     | Greenwald-Khanna                | P2       | Approximate quantiles               |
| 10    | Exponential Histogram           | P2       | Sliding-window approximate counting |

These are important if you want to understand actual streaming algorithms rather than just online data structures.

# **6. What I recommend you prioritize**

For your interview preparation, I would divide your effort like this:

| Area                      | Preparation focus                        |
| ------------------------- | ---------------------------------------- |
| Running aggregates        | Master                                   |
| Frequency and counting    | Master                                   |
| Fixed/time-based windows  | Master                                   |
| Monotonic stack/deque     | Master                                   |
| Top K and median          | Master                                   |
| Pattern matching          | Strong understanding                     |
| Interval maintenance      | Strong understanding                     |
| Composite data structures | Strong understanding                     |
| Sampling                  | Understand and implement                 |
| Iterator/buffering        | Understand and implement                 |
| Probabilistic sketches    | Understand theory and implement key ones |

## **7. Your mastery checklist**

After solving the problems, make sure you can independently answer these questions:

1. How do I maintain state without storing the entire stream?
2. How do I expire old elements efficiently?
3. How do I support insertions and deletions simultaneously?
4. How do I maintain min/max/average/median?
5. How do I maintain top K elements?
6. How do I identify frequent elements using bounded memory?
7. How do I process out-of-order updates?
8. How do I detect patterns in an incoming character stream?
9. How do I maintain multiple statistics simultaneously?
10. How do I replace exact storage with probabilistic data structures?
11. What are the memory and time guarantees?
12. What happens if the stream is infinite?

**My final recommendation:** Treat the 24-problem list as your main Streaming DSA collection. Solve P0 first, then P1. Keep the probabilistic algorithms as a separate advanced collection.

This gives you a focused streaming roadmap without mixing it unnecessarily with your existing Sliding Window, Prefix Sum, Monotonic Stack, Heap, and HashMap pattern collections.
