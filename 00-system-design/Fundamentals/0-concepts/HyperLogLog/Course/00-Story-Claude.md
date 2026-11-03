# Chapter 1: The Problem, Counting *Different* Things

## The story begins

Imagine you run a news website. Your boss walks in and asks:

> "How many **unique visitors** did we have today?"

This sounds simple, but notice what it's *not* asking. It's not "how many page views?" If one person refreshes the page 50 times, that's 50 views but **1 unique visitor**. In computer science this is the **count-distinct problem**, also called **cardinality estimation**.

- **Stream**: the sequence of events coming in, e.g. `alice, bob, alice, carol, bob, alice, ...`
- **Cardinality**: the number of *distinct* elements in it. For the stream above, it's 3.

Let's try to solve it like an engineer would.

---

## Attempt 1: Just keep a counter

```
visitors = 0
on every visit: visitors += 1
```

Stream `alice, bob, alice, carol, bob, alice` gives 6. But the real answer is 3. The counter can't tell whether it has seen Alice before, so it counts repeats. **Fails.**

---

## Attempt 2: Remember everyone you've seen

To know whether Alice is new, you need to remember Alice. So keep a **set**:

```
seen = set()
on every visit: seen.add(user_id)
answer = len(seen)
```

This is **exactly correct**. Duplicates are ignored automatically because sets don't store the same thing twice. For a small site, this is perfect. Now let's see what happens when you grow.

### The memory problem

Suppose you have **1 billion** unique visitors, each identified by a 16-byte ID (a UUID):

```
1,000,000,000 × 16 bytes = 16 GB
```

And that's the raw data. A real hash set has overhead (pointers, load-factor slack, buckets), so it's more like **30-50 GB**. For *one* number.

Now your boss gets greedy: "Unique visitors **per page**, per country, per hour, per device type, per campaign..." If you have 1 million such breakdowns, each needing its own set, the memory is astronomical. The memory grows **linearly with the number of distinct items**, and that is the core issue.

---

## Attempt 3: Let the database do it

```sql
SELECT COUNT(DISTINCT user_id) FROM visits WHERE day = '2026-10-28';
```

This works, but internally the database does what Attempt 2 does: it builds a giant hash table or sorts everything. On billions of rows it's slow and memory-hungry. And if your data is spread over 100 servers, each server holds a different subset of users. You can't just add up each server's count, because the same user may have visited several servers. You'd have to ship *all the raw IDs* to one place and merge them.

---

## Attempt 4: A bitmap (the clever trick)

If your IDs are integers from `0` to `N-1`, keep **one bit per possible user**:

```
user 0 → bit 0, user 1 → bit 1, ...
Visit by user 5? Set bit 5 to 1.
Answer = number of 1-bits.
```

For 1 billion possible users: 1,000,000,000 bits = **125 MB**. That's much better than 16+ GB.

But there are problems:
1. IDs must be small dense integers. UUIDs, emails, and IP addresses don't qualify.
2. The size depends on the **ID universe**, not on how many users actually showed up. A page with 10 visitors still costs 125 MB.
3. 1 million breakdowns × 125 MB = 125 TB. Still hopeless.

---

## The uncomfortable truth

Every approach so far has the same property: memory ∝ number of distinct items. This isn't a lack of cleverness. It's been proven that **any algorithm that returns the *exact* count must use memory proportional to the number of distinct items** (in the worst case). To know with certainty whether you've seen Alice before, you must have kept *something* about Alice.

So exactness itself is the expensive part.

---

## The pivot: do we really need exactness?

Go back to your boss's question: "How many unique visitors today?"

Does the boss care whether the answer is **1,203,481** or **1,200,000**? Almost certainly not. For dashboards, capacity planning, anomaly detection (e.g. "are 10 million distinct IPs hitting me? DDoS!"), and analytics, being off by 1% is fine.

That opens a trade:

> **Give up a tiny bit of accuracy → get an enormous reduction in memory.**

Here's the kind of result we're heading toward: HyperLogLog can estimate cardinalities in the **billions** using about **12 KB** of memory, with an error of about **0.8%**. Compare that to the 30-50 GB above.

---

## What we want (our wish list)

Any good solution should be:

| Requirement | Why |
|---|---|
| **One pass, streaming** | Data is too big to store and re-read |
| **Tiny, fixed memory** | Memory should *not* grow with the number of distinct items |
| **Duplicates don't matter** | Alice visiting 1 time or 1,000 times counts once |
| **Fast per item** | Millions of events per second |
| **Mergeable** | Combine results from 100 servers without shipping raw data |

---

## Where this is used in the real world

- **Redis**: `PFADD` / `PFCOUNT` (the "PF" is for Philippe Flajolet, one of the inventors)
- **Google BigQuery**: `APPROX_COUNT_DISTINCT`
- **PostgreSQL, Presto, Spark, Druid, ClickHouse**: all have approximate distinct counts
- Network monitoring, ad-tech (unique reach), and database query optimizers

---

## Chapter 1 summary

1. The **count-distinct problem** asks how many *different* items are in a stream.
2. Exact solutions (sets, bitmaps, DB distinct) need memory that grows with the number of distinct items, which is too much at scale.
3. This is a fundamental limit for *exact* answers.
4. If we accept a small, controlled error, perhaps memory can become tiny and fixed.

**Next chapter preview:** we'll meet the strange and beautiful idea that started it all. Back in the 1980s, Flajolet and Martin asked: *"What if we just look at how rare the rarest-looking item is?"* It involves coin flips, hash functions, and long runs of zeros. It's wrong in an instructive way, and its failure is what leads to HyperLogLog.

---

**Quick check before we move on:** in your own words, why can't we get the exact distinct count using less memory than the number of distinct items? If that's clear (or you have any questions about this chapter), tell me and I'll start Chapter 2.






















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Chapter 2: The Coin-Flip Trick (Flajolet-Martin, 1983)

## The story continues

It's the early 1980s. Databases are growing, and memory is expensive. Philippe Flajolet and G. Nigel Martin are looking at the problem from Chapter 1 and thinking:

> "Exact counting needs memory proportional to the number of distinct items. What if we don't store the items at all, but just keep one tiny clue about them?"

The clue they chose is strange but clever. To see why it works, start with coin flips.

---

## Step 1: The coin-flip intuition

Suppose I tell you: "I flipped a fair coin many times, and at some point I got **10 tails in a row**." How many times do you think I flipped?

The chance of a specific run of 10 tails is `(1/2)^10 = 1/1024`. So you'd guess I flipped roughly **1,000 times**. If I'd flipped only 5 times, a streak of 10 would be impossible. If I'd flipped 10 million times, I'd likely have seen even longer streaks.

> **A rare event tells you how many chances there were.**

Here's the same idea as a game. A thousand people each flip a coin until it lands heads, and you only get told who had the **longest streak of tails**. If the best streak is 3, there were probably very few people (~8). If it's 10, there were probably ~1000. You never saw anyone's individual flips, just one number, yet you can estimate the crowd size.

---

## Step 2: Turn items into coin flips using a hash function

How do we make "visitors" behave like coin flips? With a **hash function**, which converts any item into a fixed-length string of bits that looks random.

```
hash("alice") = 10110010
hash("bob")   = 00101101
hash("carol") = 01100111
```

A good hash function has two properties we need:

1. **Uniform**: every bit is like a fair coin flip (50% 0, 50% 1), independent of the others.
2. **Deterministic**: the same input *always* gives the same output.

Property 2 is the magic for count-distinct. Alice visiting 1 time or 1,000 times produces the **same hash**, so she contributes exactly one "coin flip sequence." **Duplicates are handled for free.**

> **Why not use the raw user IDs?** Raw IDs aren't random. Sequential IDs like 1, 2, 3, 4 in binary are `00000001, 00000010, 00000011, ...`, full of leading zeros in a very non-random pattern. Hashing scrambles them so the coin-flip assumption holds.

---

## Step 3: Count leading zeros

Treat each hash as a sequence of coin flips and ask: **how many leading zeros does it have?**

| Hash | Leading zeros | Probability of this many or more |
|---|---|---|
| `1xxxxxxx` | 0 | 1 (always) |
| `01xxxxxx` | 1 | 1/2 |
| `001xxxxx` | 2 | 1/4 |
| `0001xxxx` | 3 | 1/8 |
| `00001xxx` | 4 | 1/16 |
| ... | k | **1 / 2^k** |

So a hash with at least `k` leading zeros appears about once every `2^k` distinct items.

---

## Step 4: The algorithm

```
max_zeros = 0
for each item in stream:
    h = hash(item)
    max_zeros = max(max_zeros, leading_zeros(h))

estimate = 2 ^ max_zeros
```

That's the entire algorithm. **One number** of memory.

### Worked example

Stream: `alice, bob, alice, carol, dave, bob, alice` (true distinct count = 4)

| Item | Hash | Leading zeros | max so far |
|---|---|---|---|
| alice | `10110010` | 0 | 0 |
| bob | `00101101` | 2 | 2 |
| alice | `10110010` | 0 (same hash!) | 2 |
| carol | `01100111` | 1 | 2 |
| dave | `00010110` | 3 | **3** |
| bob | `00101101` | 2 | 3 |
| alice | `10110010` | 0 | 3 |

Estimate = 2³ = **8**. The true answer is 4. Not great, but we used *one tiny number* instead of a set, and the repeated Alice and Bob didn't change anything.

---

## Why this is so memory-efficient

We only store `max_zeros`. With a 64-bit hash, it's a number between 0 and 64, which needs just **6 bits**. In general, counting up to N distinct items requires storing a number up to about `log₂ N`, and storing *that* needs `log₂ log₂ N` bits. This is where the later names **LogLog** and **Hyper*LogLog*** come from.

| Distinct items | Exact set | This trick |
|---|---|---|
| 1 billion | ~16+ GB | **~6 bits** |

---

## It's even mergeable

Say server A saw max zeros = 7 and server B saw 9. What's the max zeros across the combined data? Just `max(7, 9) = 9`. And overlapping users between servers don't matter, since the same user has the same hash on both. **Merging = taking the max.** No raw data shipped.

So against our wish list from Chapter 1: streaming ✅, tiny memory ✅, duplicates ignored ✅, fast ✅, mergeable ✅. It all works on paper, but there's a catch.

---

## The catch: it's wildly noisy

Look at what the estimator really does: it bets everything on **one single extreme item**. Three problems follow.

### Problem 1: One lucky hash ruins everything

Suppose the stream has only **1 distinct item**, and its hash happens to be `00000001`. That's 7 leading zeros, so the estimate is 2⁷ = **128**, off by 128×. The probability of this particular bad luck for one item is 1/128, which is small, but nothing guards against it. With a million users you'd expect plenty of rare hashes, and the single most extreme one dominates.

### Problem 2: Estimates jump in powers of 2

The answer can only be 1, 2, 4, 8, 16, 32... Nothing in between. Whether the truth is 600 or 1,000, you'd likely see 512 or 1,024.

### Problem 3: High variance, even for large N

Take a true count of n = 1,000. The chance that the max leading zeros is at least `k` is `1 − (1 − 2⁻ᵏ)ⁿ`:

| k | P(max ≥ k) | Estimate 2ᵏ |
|---|---|---|
| 7 | ~100% | 128 |
| 10 | ~62% | 1,024 |
| 13 | ~11.5% | 8,192 |
| 15 | ~3% | 32,768 |

So for a true value of 1,000, there's a real chance of answering 8,000 or 32,000. The error is typically a factor of **2 to 4**, and occasionally far worse. Running it again with a different hash function gives a totally different answer.

> **The core flaw:** the estimator depends on a single extreme value. Extremes are the *least* stable statistic you can pick.

---

## A note on the original paper

The 1983 paper (published in 1985) was a bit more refined than what I showed. It kept a **bitmap** of which zero-counts had ever been seen, looked at the first position that was *never* seen, and divided by a magic correction constant `φ ≈ 0.77351` to remove systematic bias. That reduces some noise but doesn't fix the fundamental issue. I've shown the simplified "track the max" version because it contains the entire core idea, and HyperLogLog later returns to exactly this "max leading zeros" statistic, but fixes the noise in a smarter way.

---

## Chapter 2 summary

1. **A rare event reveals how many trials there were.** A long streak of tails implies many flips.
2. A **hash function** turns items into random-looking bits and makes duplicates identical.
3. Tracking the **maximum number of leading zeros** gives an estimate of `2^max` using almost no memory, duplicates ignored, and trivially mergeable via `max`.
4. But it's **extremely noisy**: it relies on a single extreme observation, giving answers off by factors of 2-4 or more.

**Next chapter preview:** how do you make a noisy estimator reliable? The classic statistical answer is: *take many independent measurements and average them.* But hashing every item 1,000 times is far too slow. The trick Flajolet's team found is to **split the stream into buckets**, let each bucket make its own noisy guess, and combine them, giving many measurements for the price of one hash. This is the road to LogLog and finally HyperLogLog.

---

**Quick check:** why does the same user visiting 1,000 times not inflate the estimate? And why is relying on the *maximum* a risky choice? Tell me if that's clear (or ask anything), and I'll start Chapter 3.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Chapter 3: Many Small Guesses Beat One Big Guess (Stochastic Averaging and LogLog)

## Where we left off

Our estimator from Chapter 2 tracked one number, the maximum leading zeros, and it was very noisy because it bet everything on a single extreme item. Flajolet and his colleagues then asked the question any statistician would ask:

> "A single measurement is noisy. What if we take **many independent measurements** and average them?"

---

## Step 1: Why averaging works

Say you want to know someone's true weight, but your scale is bad and each reading is off by ±5 kg randomly. Weigh them once: ±5 kg error. Weigh them 100 times and average: the errors cancel out, and your error shrinks to about ±5/√100 = **±0.5 kg**.

> **Averaging m independent measurements cuts the error by a factor of √m.**

So the plan is: run our noisy estimator many times and average the results.

---

## Step 2: The naive way (and why it's too slow)

Use m different hash functions, keep m different max-zeros values, and average:

```
for each item:
    for i in 1..m:
        h = hash_i(item)          # m hash computations per item!
        max_zeros[i] = max(max_zeros[i], leading_zeros(h))
```

With m = 1,000, every incoming item costs 1,000 hash computations. At millions of events per second, that is unacceptable. We want many measurements for the price of **one** hash.

---

## Step 3: The trick, stochastic averaging

Instead of hashing 1,000 times, hash **once** and use the hash itself in two roles:

1. The **first few bits** choose a bucket (a "register").
2. The **remaining bits** are the coin flips we count zeros on.

Say we want m = 4 buckets, so we need 2 bits to pick one (2² = 4). Here's a stream of 8-bit hashes:

```
hash = [ 2 bucket bits | 6 "coin flip" bits ]
```

Look at how each item gets processed. I'll define **rank = (leading zeros) + 1**, i.e., the position of the first 1-bit. This is the convention HyperLogLog uses, since it avoids the awkward "zero zeros" case.

| Item | Hash | Bucket bits | Remaining bits | Rank |
|---|---|---|---|---|
| alice | `10 110010` | 10 → bucket 2 | `110010` | 1 |
| bob | `00 101101` | 00 → bucket 0 | `101101` | 1 |
| carol | `01 100111` | 01 → bucket 1 | `100111` | 1 |
| dave | `00 010110` | 00 → bucket 0 | `010110` | 2 |
| eve | `11 000001` | 11 → bucket 3 | `000001` | 6 |

Each bucket keeps the **maximum rank** it has seen:

```
Bucket 0: max(1, 2) = 2
Bucket 1: 1
Bucket 2: 1
Bucket 3: 6
```

One hash per item, but we now have 4 separate measurements. The hash is random, so each item lands in a random bucket, and each bucket becomes an independent mini-experiment. This is called **stochastic averaging**, and it was already in the original 1985 paper (the PCSA algorithm).

### Two properties that still hold

- **Duplicates**: Alice always hashes to the same bucket with the same rank, so repeats change nothing.
- **Mergeable**: for two servers with the same bucket layout, merge **bucket by bucket** with `max`. Still trivial.

---

## Step 4: Turning buckets into an estimate

Here's the subtle part. Each bucket only sees about **n/m** distinct items (the items are spread evenly). So each bucket's max rank estimates `2^rank ≈ n/m`, not n.

To get the total, we combine the buckets and multiply by m:

```
estimate ≈ m × (typical value of 2^rank across buckets)
```

Example with m = 1,024 buckets and n = 1,000,000 distinct items: each bucket sees ~977 items, so a typical max rank is around log₂(977) ≈ 10, and the estimate is 1,024 × 2¹⁰ ≈ 1,000,000 ✅.

Notice that **each individual bucket is still as noisy as before**. The improvement comes entirely from combining 1,024 of them.

---

## Step 5: LogLog (Durand and Flajolet, 2003)

By 2003, Marianne Durand and Flajolet worked out how to combine the buckets properly. **LogLog** takes the **arithmetic mean of the register values** (the ranks, which are exponents) and puts it back in the power of 2:

```
estimate = α × m × 2^( (1/m) × Σ register[j] )
```

where α ≈ 0.397 is a correction constant that removes systematic bias, similar to the φ constant in the original paper.

### What did we gain?

- **Accuracy**: standard error ≈ **1.30 / √m**. With m = 1,024 that's 1.30/32 ≈ **4%**, a huge improvement over "off by 2-4×".
- **Memory**: each register stores a rank up to ~64, so it needs only **5-6 bits**. That's 1,024 × 5 bits = **640 bytes** to count into the billions.

That's the "LogLog" name: registers store values of size log N, and storing those takes log log N bits.

---

## Step 6: The new problem with LogLog

You can already smell it. We're taking the average of **exponents**, and exponents are explosive.

Imagine 4 registers, where three say 5 and one unlucky bucket got an extreme hash and says 15:

```
Registers: [5, 5, 5, 5]   → mean = 5.0  → 2^5.0  = 32
Registers: [5, 5, 5, 15]  → mean = 7.5  → 2^7.5  ≈ 181
```

**One outlier register changed the answer by 5.7×**, even though the other three are perfectly consistent. In the exponent, the arithmetic mean is the wrong tool: a single big number drags it hard, and then `2^` amplifies the damage.

The researchers tried patches. **SuperLogLog** (also 2003) simply threw away the largest ~30% of registers before averaging, improving error to about 1.05/√m. It worked, but it was a hack, since discarding data feels wasteful, and there's still the risk that outliers are not all in the top 30%.

There's also a second weakness: with **small** counts (say, 50 items spread over 1,024 buckets), most buckets are empty and the formula breaks down. We'll deal with that later.

---

## Chapter 3 summary

1. **Averaging many independent measurements** shrinks error by √m.
2. Using m hash functions is too slow. **Stochastic averaging** uses one hash: the first bits pick a bucket, the rest are the coin flips. One hash gives m measurements.
3. Each bucket keeps the **max rank** it has seen. Merging two sketches = bucket-wise `max`.
4. **LogLog** averages the register values (exponents) arithmetically, giving ~**1.30/√m** error with ~5 bits per register.
5. Problem: the arithmetic mean of exponents is **hypersensitive to outliers**. One huge register inflates the estimate exponentially.

**Next chapter preview:** the crown jewel. In 2007, Flajolet, Fusy, Gandouet, and Meunier asked: *"What if we use a different kind of average, one that naturally ignores huge outliers?"* The answer is the **harmonic mean**, and that single change gives HyperLogLog. We'll walk through the full algorithm, the formula, and why the harmonic mean is the right fit, with a complete worked example.

---

**Quick check:** (1) Why does using the first few bits of a *single* hash as a bucket selector give us the same effect as multiple hash functions? (2) Why is the arithmetic mean of exponents dangerous? Let me know if it's clear, or ask any questions, and I'll start Chapter 4.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















Good questions. Two of the choices in my explanation are arbitrary, and I didn't say so.

---

## 1. Nothing is special about "leading" or about "zeros"

All we need is **a pattern whose probability halves each time we ask for one more step**. Leading zeros is just one such pattern.

| Pattern you measure | P(at least k) |
|---|---|
| k leading zeros (`000…1`) | 1/2^k |
| k leading ones (`111…0`) | 1/2^k |
| k trailing zeros | 1/2^k |
| k trailing ones | 1/2^k |

Each bit of a good hash is a fair coin: 0 and 1 are equally likely. Asking "how many 1s in a row?" is exactly as rare as "how many 0s in a row?", so you could use ones and the algorithm would work identically.

We use **leading zeros** only because it is convenient. CPUs have a single instruction (`clz`, count leading zeros) that computes it instantly. The one rule is that you must **pick one pattern and use it consistently** for every item.

The same goes for the bucket bits. I said "first bits pick the bucket," but the *last* bits would work just as well. We only need the bucket bits and the counting bits to be **different parts of the hash**, so the bucket choice doesn't influence the zero-count. Because the hash bits are independent, they don't interfere.

---

## 2. How does a rare pattern tell us the total?

Here is the chain of reasoning:

**Fact:** each distinct item has probability 1/2^k of showing at least k leading zeros.

So if there are **n distinct items**, the expected number of items showing at least k leading zeros is **n / 2^k**.

Take n = 1,024:

| k (leading zeros) | Expected items with ≥ k zeros |
|---|---|
| 0 | 1024 |
| 1 | 512 |
| 2 | 256 |
| 5 | 32 |
| 8 | 4 |
| 10 | **1** |
| 11 | 0.5 |
| 12 | 0.25 |

Look at where the number crosses 1. At k = 10 you expect about one item, and beyond that you expect none. Since 2^10 = 1,024 = n, the point where it crosses 1 is exactly **k = log₂ n**.

Now turn it around. Say you don't know n, but you observe that the best hash in your stream had 10 leading zeros. That tells you "the expected count crosses 1 around k = 10," so:

```
n / 2^10 ≈ 1   →   n ≈ 2^10 ≈ 1,024
```

That is the whole trick. The **maximum zeros acts as a ruler** for the stream size:
- Seeing a maximum of 3 zeros means a small stream was enough to produce that.
- Seeing a maximum of 20 zeros means you needed about a million distinct items to get that lucky.

---

## 3. Why only the leading bits and not the whole hash?

You might ask why we don't just compare the whole hash, or look at "the smallest hash." Each bit of the hash is a coin flip, and **the pattern of k zeros in a row is the one event whose probability is exactly 1/2^k and is trivially cheap to check**. That clean power-of-2 relationship is what lets us convert "observed rarity" into "number of items" with a simple formula.

---

## 4. Why do we need the bucket bits?

This part belongs to Chapter 3's idea. A single max is noisy (one lucky item ruins it). So we split items into, say, 4 groups using a few bits, and each group does its own "max zeros" experiment:

```
hash = [ bucket bits | bits used for counting zeros ]
         "which group?"    "how rare is this one?"
```

If n = 1,000,000 and there are 1,024 buckets, each bucket gets ~1,000 items and its max rank is around 10. Each bucket gives its own ruler reading, and we combine the readings, which cancels the noise of any single lucky item.

---

## Summary in one picture

1. Hash each item to random bits, so the same item always gives the same bits.
2. Use some bits to choose a bucket, and the rest to measure rarity.
3. Rarity (leading zeros, or ones, or anything with 1/2^k odds) tells you how many distinct items were needed to see it.
4. Many buckets each give a reading, and combining them gives a stable answer.

---

**Quick check:** if I told you a bucket's best hash had 7 leading zeros, roughly how many distinct items would you guess that bucket has seen? And if there are 512 buckets, what is the total estimate? Try it, and if it's clear we'll move on to Chapter 4 (HyperLogLog and the harmonic mean).




















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------



















# Chapter 4: HyperLogLog (2007)

## First, the answer to the last quick check

A bucket whose best hash had 7 leading zeros (rank 8) has probably seen on the order of 2⁷ to 2⁸, so about **128-256 items**. With 512 buckets, the total is roughly 512 × 128-256, or **65k-130k**. Chapter 4 makes this combination precise.

---

## The problem we are fixing

LogLog averaged the register values, which are exponents, and then raised 2 to the result. One outlier register therefore got amplified exponentially:

```
Registers [5,5,5,5]   → estimate (×m) = 4 × 2^5.0 = 128
Registers [5,5,5,15]  → estimate (×m) = 4 × 2^7.5 ≈ 724   (5.7× too high)
```

Flajolet, Fusy, Gandouet and Meunier asked: **is there an average that naturally ignores huge values?**

---

## The key idea: the harmonic mean

The **arithmetic mean** of numbers is `(a + b + c) / 3`. The **harmonic mean** is the reciprocal of the average of reciprocals:

```
harmonic mean = n / (1/a + 1/b + 1/c + ...)
```

Reciprocals of big numbers are tiny, so big numbers barely contribute. Compare the two means on `[10, 10, 10, 1000]`:

| Mean | Result |
|---|---|
| Arithmetic | 257.5 (dragged up by 1000) |
| Harmonic | 4 / (0.1+0.1+0.1+0.001) ≈ **13.3** (barely notices 1000) |

That's exactly the behavior we want.

### Applying it to registers

Each register `M_j` gives its own estimate of "items in this bucket": `2^(M_j)`. Instead of averaging exponents, HyperLogLog takes the **harmonic mean of the `2^(M_j)` values** and multiplies by m (since each bucket holds about n/m items):

```
E = α_m × m² / Σ_j 2^(−M_j)
```

Where:
- `Σ 2^(−M_j)` is the sum of reciprocals. A huge register contributes almost nothing, e.g. `2^−15 ≈ 0.00003`.
- `m²` is `m` (number of buckets) times `m` (from the harmonic mean's numerator).
- `α_m` is a bias-correction constant: ≈ 0.673 for m=16, 0.697 for m=32, 0.709 for m=64, and `0.7213 / (1 + 1.079/m)` for m ≥ 128. For large m it's about **0.7213**.

### Same outlier test, now with HyperLogLog

```
[5,5,5,5]:   Σ = 4/32 = 0.125       → m²/Σ = 16/0.125   = 128
[5,5,5,15]:  Σ = 3/32 + 1/32768 ≈ 0.09378 → m²/Σ = 16/0.09378 ≈ 171
```

The outlier moved the answer from 128 to 171 (**1.3×**) instead of LogLog's 5.7×. The giant register is almost ignored, and the estimate behaves as if it were computed from the 3 sane registers.

(An outlier that makes the estimate a little higher is still possible, which is why α and the later corrections exist. But the damage is bounded.)

---

## The complete algorithm

Choose a precision `p`. Then `m = 2^p` registers, all starting at 0.

**Add an item:**
```
h     = hash(item)                  # 64-bit hash
j     = first p bits of h           # bucket index
w     = remaining (64 - p) bits
rank  = position of first 1-bit in w   (= leading zeros + 1)
M[j]  = max(M[j], rank)
```

**Estimate:**
```
E = α_m × m² / Σ 2^(−M[j])
```

**Merge two sketches** (same p): `M[j] = max(M1[j], M2[j])` for every j.

### Worked example (tiny, m = 8, p = 3)

Suppose after processing a stream the registers are:

```
M = [3, 4, 2, 5, 3, 3, 4, 2]
```

Sum of reciprocals:
```
1/8 + 1/16 + 1/4 + 1/32 + 1/8 + 1/8 + 1/16 + 1/4 = 33/32 = 1.03125
```

With α₈ ≈ 0.636:
```
E = 0.636 × 64 / 1.03125 ≈ 39
```

(With only 8 registers the answer is rough, and this is the regime where the small-range correction below would apply. The mechanics are what matter here.)

---

## The numbers that made it famous

With `p = 14`:

- Registers: `m = 2^14 = 16,384`
- Each register stores a rank ≤ 64 − 14 + 1 = 51, which fits in **6 bits**
- Memory: `16,384 × 6 bits = 98,304 bits = 12,288 bytes ≈ 12 KB`
- Standard error: **1.04 / √m = 1.04 / 128 ≈ 0.81%**

So **12 KB counts billions of distinct items to within about 1%**. Compare that to 30-50 GB for an exact set.

### What "standard error 0.81%" really means

It does **not** mean "never more than 0.81% off." The error is roughly bell-shaped around the truth:

- ~68% of the time, within **1 × 0.81% ≈ 0.8%**
- ~95% of the time, within **2 × ≈ 1.6%**
- ~99.7% of the time, within **3 × ≈ 2.4%**

You trade memory for accuracy with a clean rule: error ∝ 1/√m. To halve the error, quadruple the memory.

| p | Registers | Memory (6 bit) | Std error |
|---|---|---|---|
| 10 | 1,024 | 768 B | 3.25% |
| 12 | 4,096 | 3 KB | 1.6% |
| 14 | 16,384 | 12 KB | 0.81% |
| 16 | 65,536 | 48 KB | 0.4% |

---

## Guarantees (and non-guarantees)

**You get:**
- Error is independent of how many items there are (same ~0.8% at 10 million or 10 billion).
- Memory is fixed and does not grow with the data.
- Duplicates have zero effect.
- The sketch is mergeable, and the merge result is **identical** to sketching the combined data directly.
- Order of insertion doesn't matter.

**You don't get:**
- Exactness, ever.
- A hard worst-case bound, only a probabilistic one.
- Support for deletions: a register stores a max, so you can't "un-see" an item.
- The ability to list or look up items: it can't answer "did Alice visit?" (that's a different structure, a Bloom filter).
- Good accuracy if the hash function is bad.

---

## The new problems HyperLogLog introduced

The raw formula has weaknesses at the edges, and the 2007 paper added patches:

**1. Small counts (n much less than m).** With 50 items and 16,384 buckets, nearly all registers are 0, and the harmonic formula **overestimates badly**, even though it should be easy to be accurate here. The fix is **Linear Counting**: let `V` be the number of registers still at 0, and estimate `m × ln(m / V)`. It behaves like the bitmap from Chapter 1 and is excellent for small n. HyperLogLog switches to it when the raw estimate is below about `2.5m`.

**2. Very large counts (with a 32-bit hash).** Once n approaches 2³², hash collisions become common and the estimate saturates, so the original paper added a correction. Using a **64-bit hash** pushes that limit out of reach and the correction becomes unnecessary.

**3. A systematic bias in the middle range.** Right around the switch between linear counting and the raw formula, the estimate is measurably biased, a small bump in the error curve.

**4. Wasted memory for small sets.** A sketch always costs 12 KB, even if it has seen 3 items. If you have a million breakdowns where most have just a few users, that adds up.

These are what the next chapter is about.

---

## Chapter 4 summary

1. LogLog's arithmetic mean of exponents was outlier-sensitive. The **harmonic mean** ignores huge values.
2. **HyperLogLog**: `E = α_m · m² / Σ 2^(−M_j)`, with the same bucket-and-max structure as before.
3. With `p = 14`: **12 KB, ~0.81% standard error**, error `≈ 1.04/√m`.
4. Mergeable via register-wise `max`, duplicates harmless, no deletions, probabilistic guarantee.
5. New problems: **small-range overestimation**, **large-range hash collisions**, **mid-range bias**, and **fixed memory even for tiny sets**.

**Next chapter preview:** Google engineers took HyperLogLog to real production data (billions of events per day) and found these edge problems were costing them accuracy and memory. Their upgrade, **HyperLogLog++**, uses a 64-bit hash, an empirical bias-correction table, and a clever **sparse representation** so small sketches cost bytes instead of kilobytes.

---

**Quick check:** (1) Why does a single huge register barely change a harmonic-mean estimate? (2) If you wanted 0.4% error, how much memory would you need compared to 0.8%? Tell me if it's clear, or ask anything, and I'll start Chapter 5.



















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















# Chapter 5: HyperLogLog++ (Google, 2013)

## First, the answers to the last quick check

1. **Why does a huge register barely matter?** The estimator sums `2^(−M_j)`. A register of 15 contributes `2^−15 ≈ 0.00003`, which is nothing next to the other terms. Big values get squashed toward zero before they are added.
2. **0.4% error vs 0.8%:** error ∝ 1/√m, so halving the error needs **4× the registers**, or **48 KB instead of 12 KB**.

---

## The story: HyperLogLog meets real data

Fast-forward to around 2012. Google engineers were running HyperLogLog on production systems such as ad analytics and their internal analysis tools, counting things like distinct users across billions of events per day. The paper had promised ~1% error, but at Google's scale the edge cases from Chapter 4 stopped being theoretical.

Heule, Nunkesser and Hall studied the algorithm carefully and asked: **"What exactly is wrong with HyperLogLog at the edges, and can we fix each thing with engineering?"** They fixed four problems.

---

## Problem 1: The 32-bit hash ran out of room

The original paper used a **32-bit hash**. There are only 2³² ≈ 4.3 billion possible hash values. Once your true count approaches a few hundred million, hash **collisions** (two different items getting the same hash) become common, and the estimator starts undercounting. The original paper bolted on a "large range correction" formula to patch this, but it was a patch.

**Fix: use a 64-bit hash.**

With 2⁶⁴ ≈ 18 quintillion possible values, collisions are negligible for any realistic dataset, so the large-range correction is **deleted entirely**. The cost is small: ranks can now go up to 64 − p + 1, which still fits in **6 bits** per register instead of 5.

This was the easiest win. A simpler algorithm and better accuracy.

---

## Problem 2: Small counts were badly wrong (and the switch point was crude)

Recall from Chapter 4 that for small n, the harmonic formula overestimates. Let's see just how bad. With p = 14 (m = 16,384) and only **50 distinct items**:

- About 50 registers are nonzero, and 16,334 are still 0.
- Raw HyperLogLog formula: the sum Σ 2^(−M) is about 16,350 (each zero register contributes 1), so
  `E ≈ 0.7213 × 16384² / 16350 ≈ 11,800`.

**The raw estimate says ~11,800 when the truth is 50. That's 236× too high.** Even an empty sketch would produce `0.7213 × m ≈ 11,800`. The formula simply cannot produce small numbers.

The original fix was **Linear Counting**: with V = number of zero registers,

```
E = m × ln(m / V)
E = 16384 × ln(16384 / 16334) ≈ 50.08  ✅
```

Beautiful, nearly exact. The original paper switched to Linear Counting when the raw estimate fell below **2.5 m**. But Google measured it and found that this switching point wasn't optimal.

**Fix: an empirically chosen threshold per precision.** For each p, they ran experiments to find the cardinality at which Linear Counting stops being more accurate than HyperLogLog, and stored it in a table. For p = 14 it's roughly **11,500**. Below that, use Linear Counting. Above it, use HyperLogLog.

---

## Problem 3: The bias bump in the mid-range

Even with the right switch point, there was a leftover problem. For n between about **m and 5m** (roughly 16k to 80k for p = 14), the raw HyperLogLog estimate is **systematically too high**. This isn't random noise. It's a consistent, repeatable overestimate, a bump in the error curve.

Think about why this is fixable. Random noise can't be corrected, because you don't know which way it went. But **systematic bias** is different. If the raw estimator always reads 5% high when the truth is around 30,000, you can just subtract that bias.

**Fix: an empirical bias-correction table.** The recipe:

1. For each precision p, and for many different true cardinalities n, generate random data with exactly n distinct items.
2. Run the sketch thousands of times and average the raw estimates.
3. Record: *"when the raw estimate comes out as X, the typical error is +B."*
4. Store about 200 `(raw estimate, bias)` points per precision.

At query time:

```
raw = α·m² / Σ 2^(−M_j)
if raw ≤ 5m:
    estimate = raw − interpolated_bias(raw)    # look up nearest table points
else:
    estimate = raw                              # bias is negligible here
```

It's not elegant mathematics, since it's a lookup table measured from experiments, but it flattens the bump. The full decision flow becomes:

```
count zero registers V
if V > 0 and linear_counting(V) < threshold(p):
    return linear_counting(V)
else:
    return bias_corrected(raw_estimate)
```

---

## Problem 4: A 12 KB cost even for 3 items

This is the one that matters most for real systems, and the fix is the most interesting.

Remember the motivating scenario from Chapter 1: a million breakdowns (per page, per country, per campaign...). Most of those breakdowns have only a handful of visitors. But every HyperLogLog sketch is 12 KB, even if it saw 3 items:

```
1,000,000 sketches × 12 KB = 12 GB    (to store mostly-empty sketches!)
```

Look at a sketch that has seen 3 items. It's 16,384 registers, **16,381 of them zero**. We're storing 12 KB to say "three registers are nonzero."

**Fix: a sparse representation.** Instead of the full array, store only the nonzero registers as a list of pairs:

```
Dense (12,288 bytes):  [0,0,0,0,0,3,0,0,0,0,2,0,0,...,0,5,0,0 ... 16,384 entries]

Sparse (~12 bytes):    [(5, 3), (10, 2), (9411, 5)]    # (register index, value)
```

Three items cost about 12 bytes instead of 12,288. That's **1,000× smaller**.

The paper adds clever engineering on top of this:

- **Compact encoding:** each (index, rank) pair is packed into one 32-bit integer, and the sorted list is **delta-encoded and variable-length compressed**, so even a few thousand entries take little space.
- **Write buffer:** new items go into a small temporary set and are merged into the sorted compressed list in batches, so inserts stay fast.
- **Automatic switch:** once the sparse list would take more space than the dense array (about 12 KB), it converts to dense once and stays dense forever.

### The bonus: sparse mode is *more accurate*

This is the neat part. Since the sparse list isn't limited to 16,384 slots, they use a **higher precision, p′ = 25**, for the sparse representation. That means 2²⁵ ≈ **33 million virtual registers**.

With 33 million buckets and, say, 1,000 items, collisions are essentially impossible, and Linear Counting at that resolution is almost **exact**. So small sets get nearly exact answers (not a 0.8% error), at tiny memory cost. When converting sparse to dense, the higher-precision index just gets truncated back to the top 14 bits and the rank is recomputed, which is why the encoding stores a few extra bits.

| Distinct items | Dense HLL | HLL++ sparse |
|---|---|---|
| 3 | 12 KB, ~0.8% error | **~12 bytes**, nearly exact |
| 1,000 | 12 KB, ~0.8% error | **a few KB**, nearly exact |
| 100,000 | 12 KB, ~0.8% error | switched to dense, ~0.8% error |

This gives a graceful property: **small sketches are small and precise, big sketches are bounded at 12 KB.**

---

## HyperLogLog++ in one picture

```
add(item):
    h = 64-bit hash(item)
    sparse mode?  → insert (index at p'=25, rank) into compressed list
                    (convert to dense if the list gets too big)
    dense mode?   → M[first 14 bits] = max(M[...], rank)

count():
    sparse mode   → Linear Counting at p'=25   (nearly exact)
    dense mode    → Linear Counting if small (below threshold(p))
                    else raw estimate with bias correction (if ≤ 5m)
                    else raw estimate
```

Same core idea as Chapter 4 (buckets, max rank, harmonic mean), with four engineering upgrades:

| Weakness in HLL | HLL++ fix |
|---|---|
| 32-bit hash collisions at large n | 64-bit hash, drop the large-range hack |
| Crude small/large switching | Empirically tuned thresholds per p |
| Bias bump for n ≈ m to 5m | Lookup-table bias correction |
| 12 KB even for tiny sets | Sparse mode at higher precision |

---

## What's still not solved

- **The bias table is empirical.** It's a pile of measured constants with no theory behind it, which is ugly. Later work by Otmar Ertl (2017) derived a **mathematically principled estimator** that needs no lookup tables and no switching, and it is more accurate. Redis, for instance, adopted this improved approach in later versions.
- **Still no deletions** and still no way to list members.
- **Intersections are awkward.** Unions are trivial (register-wise max), but "how many users visited both page A and page B" has no direct merge operation. We'll discuss why.

---

## Chapter 5 summary

1. Google found real-world weaknesses in HyperLogLog and fixed them in **HyperLogLog++**.
2. **64-bit hash** removes large-range collision problems and the large-range correction.
3. **Tuned thresholds** pick the best of Linear Counting vs HLL per precision.
4. **Empirical bias correction** removes the systematic overestimate for n between m and 5m.
5. **Sparse representation** makes small sketches cost bytes, not kilobytes, and also makes them nearly exact. It switches to dense once that is cheaper.

**Next chapter preview:** now that we understand the whole machine, Chapter 6 is about **using it**: merging sketches across servers and time windows (with a worked example), why **intersections** are hard (and the inclusion-exclusion trick and its pitfalls), what Redis `PFADD/PFCOUNT/PFMERGE` do under the hood, and a decision guide for when to use HyperLogLog versus a Bloom filter, a set, or a bitmap.

---

**Quick check:** (1) Why can we correct *bias* with a lookup table but not *random error*? (2) Why does sparse mode use p′ = 25 instead of p = 14, and what does that buy us? Tell me if it's clear, or ask anything, and I'll start Chapter 6.




















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------

















# Chapter 6: Using HyperLogLog (Merging, Intersections, Redis, and When to Use It)

## First, the answers to the last quick check

1. **Why can bias be corrected but not random error?** Bias is *systematic*: the raw estimator is high by about the same amount every time for a given situation, so you can measure it once and subtract it. Random error goes a different direction each run, and you can't know which way this particular sketch went.
2. **Why p′ = 25 in sparse mode?** It gives about 33 million virtual registers. With only a few thousand items, almost no two items share a register, so Linear Counting at that resolution is nearly exact. Sparse mode is both smaller and more accurate than dense for small sets.

---

## Part 1: Merging, the superpower

Back in Chapter 1, our wish list said "mergeable: combine results from 100 servers without shipping raw data." Here is that in action.

### Merging across servers

Use tiny sketches with m = 8 registers. Two servers each saw some users, and some users visited both:

```
Server A: [3, 4, 2, 5, 3, 3, 4, 2]
Server B: [4, 1, 2, 3, 5, 3, 2, 2]
Merged:   [4, 4, 2, 5, 5, 3, 4, 2]     ← register-wise max
```

Why is this *exactly* right? Take register 0. A says "the best rank among my items in bucket 0 is 3." B says "mine is 4." The best rank among everyone in bucket 0 is obviously 4. And if the same user visited both servers, they hash to the same bucket with the same rank on both, so they just show up twice in a `max`, which changes nothing.

> **The merged sketch is bit-for-bit identical to the sketch you'd get by feeding all the raw data into one HyperLogLog.** Nothing is lost in merging.

This works because `max` is:
- **Commutative**: order doesn't matter.
- **Associative**: you can merge in any grouping (a tree of 100 servers is fine).
- **Idempotent**: `max(x, x) = x`, so merging the same data twice is harmless.

### Merging across time (the rollup pattern)

This is the most common production use. Keep **one sketch per day**, 12 KB each:

```
Mon, Tue, Wed, Thu, Fri, Sat, Sun   → 7 sketches = 84 KB

Unique visitors this week   = merge(all 7) → estimate
Unique visitors Mon-Wed     = merge(Mon, Tue, Wed) → estimate
Unique visitors last 30 days= merge(30 sketches) → estimate
```

Note the crucial point: you **cannot** add daily unique counts. If Alice visits Monday and Tuesday, summing the daily counts counts her twice. But merging the *sketches* counts her once. That is the difference between "sum of distincts" and "distinct of the union."

---

## Part 2: The weakness, intersections

Unions are free. But product people constantly ask: "How many users visited **both** page A and page B?" or "How many users came from the ad campaign **and** purchased?"

There's no register-wise operation that gives you the intersection sketch. Taking `min` doesn't work, since a register stores only the best item's rank and tells you nothing about *which* items are shared.

### The workaround: inclusion-exclusion

```
|A ∩ B| = |A| + |B| − |A ∪ B|
```

Each term is available: `|A|` and `|B|` from their sketches, and `|A ∪ B|` from the merged sketch.

### Why it breaks down

Say the true values are `|A| = 1,000,000`, `|B| = 1,000,000`, and `|A ∪ B| = 1,900,000`, so the true intersection is **100,000**. Each estimate has ~0.8% error:

- `|A|` is off by about ±8,000
- `|B|` is off by about ±8,000
- `|A ∪ B|` is off by about ±15,000

Those errors add up (roughly in quadrature): √(8² + 8² + 15²) ≈ **±19,000**, which is **19% error on a 100,000 answer**. Now suppose the intersection is only 1,000: the answer is still wobbling by ±19,000, and the result is **meaningless**, possibly even negative.

> **The rule:** the error is proportional to the size of the *big* sets, not the size of the answer. Inclusion-exclusion is fine when the intersection is a large fraction of the sets, and useless when it's small.

**Alternatives when you need intersections:**
- **MinHash** estimates the *Jaccard similarity* (overlap fraction) directly.
- **Theta sketches** (Apache DataSketches) are cardinality sketches designed to support intersection and difference natively.
- Exact bitmaps (e.g. Roaring bitmaps) if the IDs are integers and you can afford them.

---

## Part 3: HyperLogLog in Redis

Redis is where most engineers first meet HyperLogLog. Three commands:

```
PFADD   visitors:2026-10-28 alice bob carol
PFADD   visitors:2026-10-28 alice dave         # alice again: ignored
PFCOUNT visitors:2026-10-28                    # → 4

PFADD   visitors:2026-10-29 bob erin
PFCOUNT visitors:2026-10-28 visitors:2026-10-29   # → 5 (union count)
PFMERGE visitors:week visitors:2026-10-28 visitors:2026-10-29
```

Everything we've built lines up:

| Redis | Our concept |
|---|---|
| `PFADD` | hash item → bucket → `max(register, rank)` |
| `PFCOUNT` (one key) | estimate from the registers |
| `PFCOUNT` (many keys) | merge on the fly, then estimate (union!) |
| `PFMERGE` | register-wise max into a destination key |

The "PF" honors Philippe Flajolet.

Under the hood Redis uses:
- **p = 14** (16,384 registers) and **6 bits** per register, so at most about **12 KB** per key, with a standard error of **0.81%**.
- A **64-bit hash**.
- A **sparse encoding** for small sets (the same motivation as HLL++, though Redis's byte format is its own run-length-style design). It converts to dense automatically past a size threshold.
- In modern versions, an improved estimator based on Ertl's work (more on that in a moment).

The practical punchline: a Redis key counting **billions** of unique items still costs about 12 KB.

---

## Part 4: When should you use HyperLogLog?

**Great fit** when all of these are true:
- You need the **count** of distinct things, not the things themselves.
- ~1% error is acceptable.
- The cardinality is large (thousands to billions), or you have *many* counters.
- Data is distributed or arrives over time and you need to **merge**.

Typical uses: unique visitors, distinct IPs (DDoS detection), distinct search queries, ad reach, distinct values per column for database query planners.

**Poor fit** when:
- You need **exact** numbers (billing, compliance, money).
- The cardinality is **small** (say under a few thousand): just use a set, since it's exact and cheap.
- You need to know *who*, not *how many*.
- You need small-overlap **intersections** or **deletions**.

### Choosing the right structure

| Your question | Use |
|---|---|
| "How many distinct items?" (approx OK) | **HyperLogLog** |
| "How many distinct items?" (exact, small data) | Hash set |
| "Exact distinct count, dense integer IDs" | Bitmap / Roaring bitmap |
| "Have I seen *this specific* item?" | **Bloom filter** (approximate membership) |
| "How many times did *this item* occur?" | **Count-Min sketch** (approximate frequency) |
| "Overlap between two sets?" | MinHash / Theta sketch |

A way to remember it: HyperLogLog answers **"how many?"**, Bloom filters answer **"is it in there?"**, and Count-Min answers **"how often?"** All three trade a little accuracy for a huge saving in memory.

---

## Chapter 6 summary

1. Merging is register-wise `max`, giving a sketch identical to sketching the combined data, with duplicates across sources harmless.
2. Time rollups: keep one sketch per period and merge any range. Never add distinct counts.
3. Intersections via inclusion-exclusion have error proportional to the *large* sets, so small overlaps are unreliable. Use Theta sketches or MinHash for that.
4. Redis exposes it as `PFADD` / `PFCOUNT` / `PFMERGE`: 12 KB max, 0.81% error.
5. Use HLL for approximate "how many distinct" at scale. Avoid it for exact counts, tiny sets, or membership questions.

---

**Quick check:** (1) Your dashboard shows daily uniques of 100, 120, 90 for three days. Why is the weekly unique count *not* 310, and how would you compute it with HLL? (2) Why can't we just take `min` of two sketches to get an intersection?

**Next chapter (the finale):** we could (a) **build HyperLogLog from scratch in Python** and watch the accuracy numbers appear on real data, and (b) look at **Ertl's modern estimator**, which removes the ugly lookup tables from Chapter 5. Tell me "next" for both, or ask me anything about this chapter first.





















-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















# Chapter 7: Build It Yourself, Then Meet the Modern Estimator

## First, the answers to the last quick check

1. **Why isn't the weekly count 310?** Anyone who visited on more than one day is counted once per day. 100 + 120 + 90 counts *visits-by-day*, not people. With HLL you keep one sketch per day and **merge the three sketches** (register-wise max), then estimate. Repeat visitors collapse to one.
2. **Why not `min` for an intersection?** A register stores only the single best rank among *all* items in that bucket, so it can't tell you which items are shared. Suppose A's register is 7 because of some item only A has, and B's is 5 because of some item only B has. `min` gives 5, but that item may belong to neither set's overlap. The result is not the sketch of A ∩ B and doesn't estimate anything meaningful.

---

## Part 1: HyperLogLog from scratch

I'll build it and run it so the numbers below are real measurements.

The code is in `hll.py`. The core is about 15 lines:

```python
def add(self, item):
    h = hash64(item)
    j = h >> self.q                      # first p bits -> bucket
    w = h & ((1 << self.q) - 1)          # remaining bits
    rank = self.q - w.bit_length() + 1   # leading zeros + 1
    self.M[j] = max(self.M[j], rank)
```

That is the whole algorithm: hash, split, count leading zeros, keep the max.

### What the run shows

With p = 14 (16,384 registers), each row averaged over 5 trials:

| True n | Raw HLL error | Ertl error |
|---|---|---|
| 10 | **118,117%** | 0.03% |
| 100 | 11,765% | 0.42% |
| 1,000 | 1,131% | 0.41% |
| 10,000 | 73% | 0.72% |
| 20,000 | 21% | 1.01% |
| 50,000 | 1.0% | 0.72% |
| 100,000 | 0.40% | 0.39% |
| 1,000,000 | 0.92% | 0.92% |

- **This is Chapter 5's small-range problem, measured.** The raw formula is absurdly wrong below ~50k items. Even for 10 items it answers about 11,800, the `0.7213 × m` floor.
- **Above ~100k, the error sits near the theoretical 0.81%.** The measurements (0.39% and 0.92%) are noisy because of only 5 trials, but they bracket it.
- **Duplicates and merging behave as promised.** The merged sketch was *identical* to the one built directly from all the data, even with 20,000 users overlapping and 60,000 duplicate inserts. The estimate was 99,314 against a true 100,000 (0.7% off, within one standard error).

---

## Part 2: Ertl's estimator (2017)

Look at the Ertl column. It's accurate everywhere, with **no Linear Counting switch, no threshold table, and no bias table**.

### The idea

HLL++ patches a flawed formula with empirical corrections. Otmar Ertl asked a more basic question:

> "The registers are random variables with a known distribution. Instead of a formula that works only in the middle and gets patched at the ends, what does the register data *actually* imply about n?"

The raw estimator loses information. It turns all registers into one sum `Σ 2^(−M_j)`, which ignores things such as *how many* registers are zero or saturated. Ertl's estimator uses the **histogram of register values**, `C_k` = how many registers equal k. That's what the code does:

```python
C = [0] * (q + 2)
for r in M: C[r] += 1
```

It then computes

```
z = m·τ(1 − C_{q+1}/m)            # handles the very-large end (saturated registers)
for k = q down to 1:  z = (z + C_k)/2
z += m·σ(C_0/m)                    # handles the very-small end (empty registers)
E = m² / (2·ln2 · z)
```

- The **middle loop** is the harmonic-mean idea from Chapter 4, rewritten over the histogram.
- **σ** (sigma) corrects for the empty registers. It replaces the Linear Counting switch. For small n it blows up in just the right way, and the result converges to the Linear Counting answer on its own.
- **τ** (tau) does the same job for saturated registers at the large end, replacing the old "large range correction."
- The constant becomes `1/(2 ln 2) ≈ 0.7213` for all m, so no per-m α table.

The key point is that this is derived from the *statistics of the sketch*, not tuned from experiments. It handles the small, middle, and large range with **one formula**, and it's at least as accurate as HLL++. Redis adopted it in version 5, which is why modern `PFCOUNT` has no visible accuracy dip as a key grows.

---

## The full story

| Year | Idea | Problem it solved | Problem it left |
|---|---|---|---|
| exact set | Remember everyone | Correct | Memory ∝ distinct count |
| 1983-85 | Flajolet-Martin: max leading zeros | Tiny memory | Hugely noisy |
| 1985 | Stochastic averaging: many buckets, one hash | Noise down by √m | Needs a good way to combine |
| 2003 | LogLog / SuperLogLog | ~5 bits/register, ~1.3/√m error | Arithmetic mean sensitive to outliers |
| 2007 | **HyperLogLog**: harmonic mean | 1.04/√m error, 12 KB for billions | Bad at small and large n, mid-range bias |
| 2013 | **HLL++**: 64-bit hash, sparse mode, bias tables | Practical at Google scale | Empirical, no theory behind the tables |
| 2017 | **Ertl**: estimator from the register histogram | One principled formula everywhere | Intersections, deletions still not supported |

The recurring pattern is that each step was a **simple core idea that failed in one specific, understandable way**, and the next idea targeted exactly that failure.

---

## Final recap: what you should now be able to explain

1. **Why:** exact distinct counting needs memory ∝ n. Approximate counting breaks that link.
2. **Core trick:** hash items to random bits. A rare pattern (long run of leading zeros) implies many items were seen. Duplicates vanish because hashing is deterministic.
3. **Buckets:** use some hash bits to pick a bucket, so one hash gives m independent measurements. Error drops by √m.
4. **Harmonic mean:** it ignores outlier registers, which is the change that made HyperLogLog out of LogLog.
5. **Guarantees:** ~1.04/√m standard error, fixed memory (12 KB at p = 14), mergeable via `max`, independent of n. No exactness, no deletions, no membership, weak intersections.
6. **Fixes along the way:** Linear Counting for small n, 64-bit hashes, bias correction, sparse mode, and Ertl's unified estimator.
7. **When to use it:** large-scale "how many distinct?" where ~1% error is fine and merging matters.

You can experiment by changing `p` in `hll.py` and watching the error scale as 1/√m.

If you'd like to go further, I can cover **how Theta sketches or MinHash handle intersections**, how **Count-Min sketch** and **Bloom filters** work in the same story format, or give you practice questions to test your understanding of HLL.

The finished implementation is in `hll.py`. Run it with `python3 hll.py` to reproduce the table above, and change `p` to see how memory and error trade off.

Which would you like next: Theta sketches and MinHash for intersections, Bloom filters and Count-Min sketch, or practice questions on HyperLogLog?


```py
import hashlib, math, random

def hash64(item):
    return int.from_bytes(hashlib.blake2b(str(item).encode(), digest_size=8).digest(), "big")

def sigma(x):
    if x == 1.0: return float("inf")
    y, z = 1.0, x
    while True:
        x = x * x
        z_old = z
        z += x * y
        y += y
        if z_old == z: return z

def tau(x):
    if x == 0.0 or x == 1.0: return 0.0
    y, z = 1.0, 1 - x
    while True:
        x = math.sqrt(x)
        z_old = z
        y *= 0.5
        z -= (1 - x) ** 2 * y
        if z_old == z: return z / 3

class HLL:
    def __init__(self, p=14):
        self.p, self.m = p, 1 << p
        self.q = 64 - p
        self.M = [0] * self.m

    def add(self, item):
        h = hash64(item)
        j = h >> self.q                      # first p bits -> bucket
        w = h & ((1 << self.q) - 1)          # remaining q bits
        rank = self.q - w.bit_length() + 1   # leading zeros + 1
        if rank > self.M[j]:
            self.M[j] = rank

    def merge(self, other):
        for j in range(self.m):
            self.M[j] = max(self.M[j], other.M[j])

    def raw(self):                            # Chapter 4 formula
        alpha = 0.7213 / (1 + 1.079 / self.m)
        return alpha * self.m ** 2 / sum(2.0 ** -r for r in self.M)

    def ertl(self):                           # Part 2 below
        C = [0] * (self.q + 2)
        for r in self.M: C[r] += 1
        m = self.m
        z = m * tau(1 - C[self.q + 1] / m)
        for k in range(self.q, 0, -1):
            z = 0.5 * (z + C[k])
        z += m * sigma(C[0] / m)
        return m ** 2 / (2 * math.log(2) * z)

if __name__ == "__main__":
    random.seed(1)
    print(f"{'true n':>10} {'raw err%':>10} {'ertl err%':>10}")
    for n in [10, 100, 1000, 10000, 20000, 50000, 100000, 1000000]:
        raw_e, ertl_e = [], []
        for t in range(5):
            h = HLL(14)
            base = random.getrandbits(40)
            for i in range(n): h.add(base + i)
            raw_e.append((h.raw() - n) / n * 100)
            ertl_e.append((h.ertl() - n) / n * 100)
        rms = lambda v: math.sqrt(sum(x*x for x in v) / len(v))
        print(f"{n:>10} {rms(raw_e):>10.2f} {rms(ertl_e):>10.2f}")

    # duplicates + merge check
    a, b, both = HLL(14), HLL(14), HLL(14)
    for i in range(0, 60000): a.add(i); both.add(i)
    for i in range(40000, 100000): b.add(i); both.add(i)
    for i in range(0, 60000): a.add(i)       # duplicates
    a.merge(b)
    print("\nmerge identical to direct sketch:", a.M == both.M)
    print("merged estimate (true 100000):", round(a.ertl()))
```




















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







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------







































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
















