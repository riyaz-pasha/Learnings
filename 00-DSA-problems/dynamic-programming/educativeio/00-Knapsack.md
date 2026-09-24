Absolutely. The **Knapsack family** is one of the most important Dynamic Programming patterns because it teaches several ideas that appear everywhere:

* choosing vs skipping
* state definition
* capacity constraints
* 0/1 vs unlimited reuse
* 1D DP optimization
* loop-direction tricks
* “exactly” vs “at most”
* reconstructing the chosen items
* recognizing knapsack hidden inside other problems

I’ll build it **from intuition → brute force → DP → optimization → 0/1 → unbounded → variations → interview recognition**.

---

# 1. What is the Knapsack Problem?

Imagine you have a bag with capacity `W`.

You have items:

| Item | Weight | Value |
| ---- | -----: | ----: |
| A    |      2 |     3 |
| B    |      3 |     4 |
| C    |      4 |     5 |
| D    |      5 |     6 |

You want to put items into the bag such that:

```text
total weight <= W
```

while maximizing:

```text
total value
```

For example, if:

```text
W = 7
```

you could choose:

```text
A + C
weight = 2 + 4 = 6
value  = 3 + 5 = 8
```

or:

```text
B + D
weight = 3 + 5 = 8 ❌
```

The goal is:

> **Maximize value without exceeding capacity.**

---

# 2. The First Important Distinction: 0/1 vs Unbounded

This is the most important thing to understand.

## 0/1 Knapsack

Each item can be used **at most once**.

If you have:

```text
A = weight 2, value 3
```

you can either:

```text
take A
```

or:

```text
don't take A
```

You cannot take A twice.

Hence:

```text
0/1
```

means:

```text
0 → don't take
1 → take
```

---

## Unbounded Knapsack

Each item can be used **any number of times**.

If:

```text
A = weight 2, value 3
```

you can do:

```text
A
A + A
A + A + A
...
```

as long as capacity allows.

---

# 3. Why Can't We Just Greedily Pick the Best Item?

Suppose:

| Item | Weight | Value |
| ---- | -----: | ----: |
| A    |      6 |    12 |
| B    |      5 |    10 |
| C    |      4 |     8 |

Capacity:

```text
W = 10
```

A greedy strategy might choose A:

```text
weight = 6
value = 12
```

Remaining:

```text
4
```

Then C:

```text
total weight = 10
total value = 20
```

Here it works.

But now:

| Item | Weight | Value |
| ---- | -----: | ----: |
| A    |      6 |    11 |
| B    |      5 |    10 |
| C    |      4 |     8 |

Greedy chooses A:

```text
11
```

and C:

```text
11 + 8 = 19
```

But:

```text
B + B
```

isn't allowed in 0/1.

Instead:

```text
B + C
weight = 9
value = 18
```

Not better.

Let's construct a more revealing example:

| Item | Weight | Value |
| ---- | -----: | ----: |
| A    |     10 |    60 |
| B    |     20 |   100 |
| C    |     30 |   120 |

Capacity:

```text
50
```

Greedy by value chooses C:

```text
120
```

then B:

```text
220
```

which happens to be optimal.

But greedy by value/weight can still fail in 0/1 problems.

The deeper reason is:

> **An item that looks good individually may prevent a better combination of other items.**

So we need to consider combinations.

That's where DP comes in.

---

# 4. Brute Force Thinking

For every item, we have two choices:

```text
take it
don't take it
```

For `n` items:

```text
2 choices × 2 choices × ... × 2 choices
```

Therefore:

```text
2^n
```

possibilities.

For example:

```text
             Item A
            /      \
         take      skip
         /           \
      Item B        Item B
      /   \          /   \
   take  skip     take  skip
```

This creates a binary decision tree.

So the recursive solution is conceptually:

```java
solve(i, capacity)
```

At item `i`:

```text
don't take i
+
take i
```

Therefore:

```java
solve(i, capacity) =
    max(
        solve(i + 1, capacity),
        value[i] + solve(i + 1, capacity - weight[i])
    )
```

provided the item fits.

---

# 5. The Key DP Observation

Look at the recursive calls.

You might calculate:

```text
solve(5, 10)
```

many times.

And:

```text
solve(5, 7)
```

many times.

These are overlapping subproblems.

So we memoize.

The state becomes:

```text
(i, capacity)
```

Meaning:

> What is the maximum value I can obtain using items from `i` onward with this much capacity remaining?

This is the fundamental knapsack state.

---

# 6. 0/1 Knapsack — Recursive DP

Let's define:

```text
dp(i, capacity)
```

as:

> Maximum value obtainable using items `[i ... n-1]` with remaining capacity `capacity`.

At item `i`, there are two possibilities.

### Don't take it

```text
dp(i + 1, capacity)
```

### Take it

If:

```text
weight[i] <= capacity
```

then:

```text
value[i] + dp(i + 1, capacity - weight[i])
```

Therefore:

```text
dp(i, capacity) =
    max(
        dp(i + 1, capacity),
        value[i] + dp(i + 1, capacity - weight[i])
    )
```

Base case:

```text
i == n
```

or:

```text
capacity == 0
```

Then:

```text
0
```

---

# 7. Java Implementation

```java
public static int knapsack(
        int[] weights,
        int[] values,
        int capacity
) {
    return solve(weights, values, 0, capacity);
}

private static int solve(
        int[] weights,
        int[] values,
        int i,
        int capacity
) {
    if (i == weights.length || capacity == 0) {
        return 0;
    }

    // Don't take
    int skip = solve(
        weights,
        values,
        i + 1,
        capacity
    );

    int take = 0;

    if (weights[i] <= capacity) {
        take = values[i] +
               solve(
                   weights,
                   values,
                   i + 1,
                   capacity - weights[i]
               );
    }

    return Math.max(skip, take);
}
```

Complexity:

```text
Time:  O(2^n)
Space: O(n) recursion stack
```

---

# 8. Memoization

There are only:

```text
n × W
```

possible states.

So:

```java
int[][] memo = new int[n][capacity + 1];
```

We can cache results.

```java
private static int solve(
        int[] weights,
        int[] values,
        int i,
        int capacity,
        int[][] memo
) {
    if (i == weights.length || capacity == 0) {
        return 0;
    }

    if (memo[i][capacity] != -1) {
        return memo[i][capacity];
    }

    int skip = solve(
        weights,
        values,
        i + 1,
        capacity,
        memo
    );

    int take = 0;

    if (weights[i] <= capacity) {
        take = values[i] +
               solve(
                   weights,
                   values,
                   i + 1,
                   capacity - weights[i],
                   memo
               );
    }

    return memo[i][capacity] =
        Math.max(skip, take);
}
```

Now:

```text
Time:  O(nW)
Space: O(nW)
```

This is already a very important interview solution.

---

# 9. Bottom-Up DP

Now let's reverse the thinking.

Instead of asking:

> What happens if I start at item `i`?

we build answers from smaller problems.

Define:

```text
dp[i][c]
```

as:

> Maximum value using the first `i` items with capacity `c`.

Notice the subtle difference.

We are now saying **first `i` items**, rather than items starting at index `i`.

---

# 10. The 2D DP Table

Suppose:

```text
weights = [2, 3, 4]
values  = [3, 4, 5]
capacity = 5
```

We create:

```text
             Capacity
          0  1  2  3  4  5
        +-------------------
item 0 | 0  0  0  0  0  0
item 1 | 0  0  3  3  3  3
item 2 | 0  0  3  4  4  7
item 3 | 0  0  3  4  5  7
```

The answer is:

```text
dp[n][W]
```

So:

```text
dp[3][5] = 7
```

which corresponds to:

```text
item A + item B
weight = 2 + 3 = 5
value  = 3 + 4 = 7
```

---

# 11. The Most Important Knapsack Formula

For item `i` with:

```text
weight = w
value = v
```

we ask:

### Does it fit?

If:

```text
w > capacity
```

we cannot take it:

```text
dp[i][capacity] = dp[i - 1][capacity]
```

Otherwise:

```text
dp[i][capacity] =
    max(
        dp[i - 1][capacity],              // skip
        v + dp[i - 1][capacity - w]       // take
    )
```

This is the core of **0/1 knapsack**.

Notice something extremely important:

```text
dp[i - 1]
```

appears in BOTH choices.

Why?

Because the item can only be used once.

---

# 12. Java 2D Solution

```java
public static int knapsack(
        int[] weights,
        int[] values,
        int capacity
) {
    int n = weights.length;

    int[][] dp = new int[n + 1][capacity + 1];

    for (int i = 1; i <= n; i++) {

        int weight = weights[i - 1];
        int value = values[i - 1];

        for (int c = 0; c <= capacity; c++) {

            // Skip
            dp[i][c] = dp[i - 1][c];

            // Take
            if (weight <= c) {
                dp[i][c] = Math.max(
                    dp[i][c],
                    value + dp[i - 1][c - weight]
                );
            }
        }
    }

    return dp[n][capacity];
}
```

Complexity:

```text
Time:  O(nW)
Space: O(nW)
```

---

# 13. Why Can We Reduce 2D → 1D?

Look at:

```text
dp[i][c]
```

We only depend on:

```text
dp[i - 1][...]
```

the previous row.

So theoretically we don't need the entire table.

We can maintain:

```text
dp[c]
```

where:

```text
dp[c] =
maximum value achievable with capacity c
```

Then:

```java
int[] dp = new int[capacity + 1];
```

---

# 14. 0/1 Knapsack 1D DP

```java
public static int knapsack(
        int[] weights,
        int[] values,
        int capacity
) {
    int[] dp = new int[capacity + 1];

    for (int i = 0; i < weights.length; i++) {

        int weight = weights[i];
        int value = values[i];

        for (int c = capacity; c >= weight; c--) {

            dp[c] = Math.max(
                dp[c],
                value + dp[c - weight]
            );
        }
    }

    return dp[capacity];
}
```

This looks deceptively simple.

But there is one **extremely important trick**:

```java
for (int c = capacity; c >= weight; c--)
```

Why backwards?

This is one of the most important things to understand in knapsack.

---

# 15. Why 0/1 Goes Backwards

Suppose:

```text
weight = 2
value = 3
capacity = 4
```

Initially:

```text
dp:
capacity  0 1 2 3 4
           0 0 0 0 0
```

Process item:

```text
weight = 2
value = 3
```

At:

```text
c = 4
```

we calculate:

```text
dp[4] = max(
    dp[4],
    3 + dp[2]
)
```

At this point:

```text
dp[2]
```

still represents the state **before processing the current item**.

Therefore we use the item once.

---

If we went forwards:

```java
for (int c = weight; c <= capacity; c++)
```

then:

```text
c = 2
dp[2] = 3
```

Then:

```text
c = 3
dp[3] = 3
```

Then:

```text
c = 4
dp[4] = 3 + dp[2]
      = 6
```

We've effectively taken the same item twice.

That's **not 0/1**.

So:

> **0/1 Knapsack → iterate capacity backwards.**

---

# 16. Unbounded Knapsack

Now change the rule.

You can use an item unlimited times.

Our previous recurrence was:

```text
take:
value + dp[i - 1][capacity - weight]
```

But that says:

> After taking this item, move to the previous items.

That prevents using the current item again.

For unbounded knapsack, after taking an item, we can stay on the same item:

```text
value + dp[i][capacity - weight]
```

Therefore:

```text
dp[i][c] =
max(
    dp[i - 1][c],
    value[i] + dp[i][c - weight[i]]
)
```

Compare:

### 0/1

```text
value + dp[i - 1][c - weight]
```

### Unbounded

```text
value + dp[i][c - weight]
```

That one difference is huge.

---

# 17. Unbounded 1D DP

And now something interesting happens.

We iterate capacity **forwards**:

```java
for (int c = weight; c <= capacity; c++)
```

Why?

Because we WANT the current item to be reused.

```java
public static int unboundedKnapsack(
        int[] weights,
        int[] values,
        int capacity
) {
    int[] dp = new int[capacity + 1];

    for (int i = 0; i < weights.length; i++) {

        int weight = weights[i];
        int value = values[i];

        for (int c = weight; c <= capacity; c++) {

            dp[c] = Math.max(
                dp[c],
                value + dp[c - weight]
            );
        }
    }

    return dp[capacity];
}
```

So remember this:

```text
0/1 Knapsack
    capacity → DOWN

Unbounded Knapsack
    capacity → UP
```

This is one of the most useful DP patterns to memorize **after understanding why**.

---

# 18. Why Forward Allows Reuse

Suppose:

```text
weight = 2
value = 3
capacity = 6
```

Start:

```text
dp = [0, 0, 0, 0, 0, 0, 0]
```

At:

```text
c = 2
```

we get:

```text
dp[2] = 3
```

Then:

```text
c = 4
```

we use:

```text
dp[4] = 3 + dp[2]
      = 6
```

Then:

```text
c = 6
```

we use:

```text
dp[6] = 3 + dp[4]
      = 9
```

So:

```text
item + item + item
```

is naturally created.

That's exactly what we want.

---

# 19. The Mental Model

Don't memorize:

```text
reverse = 0/1
forward = unbounded
```

Memorize the reason:

### 0/1

> "When I process this item, the state I'm reading from must belong to the **previous item iteration**."

Therefore:

```text
iterate backwards
```

so we don't overwrite the state before using it.

### Unbounded

> "I am allowed to use the same item again, so the state I'm reading can already include the current item."

Therefore:

```text
iterate forwards
```

---

# 20. A Fantastic Example

Suppose:

```text
weights = [2, 3, 4]
values  = [4, 5, 7]
W = 7
```

### 0/1

Possible:

```text
2 + 3 = 5
value = 9
```

```text
3 + 4 = 7
value = 12
```

So:

```text
answer = 12
```

---

### Unbounded

We can use weight `2` repeatedly:

```text
2 + 2 + 2 = 6
value = 12
```

Or:

```text
3 + 4 = 7
value = 12
```

Answer:

```text
12
```

Now change values:

```text
weights = [2, 3, 4]
values  = [5, 5, 7]
```

0/1:

```text
2 + 3 = 5
value = 10
```

Unbounded:

```text
2 + 2 + 2 = 6
value = 15
```

Now the difference is obvious.

---

# 21. A More General Way to Recognize Knapsack

Knapsack is usually hiding behind:

> **We have choices, each choice consumes some resource, and we want to maximize/minimize something.**

For example:

```text
Items → weights
Budget → capacity
Profit → value
```

Could become:

### Shopping

```text
budget = W
price = weight
happiness = value
```

### CPU scheduling

```text
CPU time = capacity
execution time = weight
profit = value
```

### Investment

```text
money = capacity
investment cost = weight
return = value
```

### Course selection

```text
available time = capacity
course hours = weight
benefit = value
```

Same underlying structure.

---

# 22. The Four Questions I Ask in an Interview

When I see a problem, I ask:

### Question 1

> Am I selecting items?

If yes, possibly knapsack.

### Question 2

> Does each selection consume some limited resource?

Examples:

```text
weight
money
time
capacity
characters
```

If yes, strong knapsack signal.

### Question 3

> Can I use an item once or repeatedly?

This determines:

```text
0/1
```

vs

```text
unbounded
```

### Question 4

> What am I optimizing?

Maybe:

```text
maximum value
minimum cost
number of ways
possible/impossible
```

This determines the DP operation.

---

# 23. Knapsack Isn't Always "Max Value"

This is important.

The basic pattern can produce several DP families.

---

## A. Maximum value

```text
dp[c] = maximum value
```

Transition:

```text
dp[c] = max(dp[c], value + dp[c - weight])
```

---

## B. Can we achieve the capacity?

Boolean DP:

```text
dp[c] = true/false
```

Example:

> Can I select some numbers whose sum equals target?

This is essentially **0/1 knapsack / subset sum**.

Transition:

```java
dp[c] = dp[c] || dp[c - weight];
```

Again:

```text
0/1 → backwards
```

---

## C. Minimum number of items

Example:

> Given coin denominations, what's the minimum number of coins needed to make amount X?

That's an **unbounded knapsack-style** problem.

```java
dp[c] = Math.min(
    dp[c],
    1 + dp[c - coin]
);
```

Capacity goes forward if coins can be reused.

---

## D. Number of ways

Example:

> How many ways can we make amount X?

```java
dp[c] += dp[c - coin];
```

This is another major variation.

---

# 24. The Coin Change Connection

Consider:

```text
coins = [1, 2, 5]
amount = 5
```

If coins can be reused indefinitely:

```text
1 + 1 + 1 + 1 + 1
2 + 1 + 1 + 1
2 + 2 + 1
5
...
```

This is essentially:

```text
unbounded knapsack
```

But there's another subtle issue:

> Are we counting combinations or permutations?

For example:

```text
1 + 2
```

and:

```text
2 + 1
```

Could either be considered the same or different.

That changes the loop structure.

---

# 25. Loop Ordering Can Change the Meaning

This is a deeper DP concept.

Suppose:

```text
coins = [1, 2]
amount = 3
```

If you want **combinations**, where:

```text
1 + 2
2 + 1
```

are considered the same:

```java
for (int coin : coins) {
    for (int amount = coin; amount <= target; amount++) {
        dp[amount] += dp[amount - coin];
    }
}
```

The coin loop is outside.

---

If you want **permutations**, where order matters:

```java
for (int amount = 1; amount <= target; amount++) {
    for (int coin : coins) {
        if (coin <= amount) {
            dp[amount] += dp[amount - coin];
        }
    }
}
```

The amount loop is outside.

This is why blindly memorizing code is dangerous.

---

# 26. 0/1 vs Unbounded — The Essential Table

| Property         | 0/1                   | Unbounded                |
| ---------------- | --------------------- | ------------------------ |
| Item usage       | Once                  | Unlimited                |
| Take transition  | previous item         | same item                |
| 2D transition    | `dp[i-1][c-w]`        | `dp[i][c-w]`             |
| 1D capacity loop | Backward              | Forward                  |
| Typical examples | subset sum, partition | coin change, rod cutting |
| Complexity       | `O(nW)`               | `O(nW)`                  |

The key:

```text
0/1:
dp[i - 1][c - w]

Unbounded:
dp[i][c - w]
```

---

# 27. Why 2D DP Is Actually Better for Learning

You might be tempted to jump immediately to:

```java
int[] dp
```

Don't.

First understand:

```text
dp[item][capacity]
```

because it makes the decision explicit:

```text
Have I already processed this item?
```

Then 1D optimization becomes easy to understand.

The 1D version is essentially exploiting:

```text
"I only need the previous row."
```

---

# 28. Exactly Capacity vs At Most Capacity

This causes lots of interview bugs.

Suppose:

```text
capacity = 10
```

Does the problem say:

> Weight must be **at most** 10?

or:

> Weight must be **exactly** 10?

These are different.

---

## At most capacity

For maximum value:

```text
weight <= W
```

Typically:

```text
answer = dp[W]
```

because `dp[W]` means:

> maximum value achievable with capacity up to W.

Unused capacity is okay.

---

## Exactly capacity

Now you need to distinguish:

```text
unreachable
```

from:

```text
value = 0
```

So initialize with negative infinity.

For example:

```java
Arrays.fill(dp, Integer.MIN_VALUE);
dp[0] = 0;
```

Then:

```java
dp[c] = Math.max(
    dp[c],
    value + dp[c - weight]
);
```

This prevents an impossible state from being treated as valid.

This initialization technique is extremely important.

---

# 29. Negative Values

Suppose values can be negative.

Then this:

```java
int[] dp = new int[capacity + 1];
```

may be wrong because Java initializes everything to:

```text
0
```

That implicitly says:

> Every capacity is achievable with value 0.

That may not be true.

You need to carefully define what your state means and initialize unreachable states appropriately.

This is a general DP lesson:

> **Initialization encodes which states are reachable.**

---

# 30. Reconstructing Which Items Were Selected

Sometimes the interviewer doesn't just ask:

> What's the maximum value?

They ask:

> Which items produce that maximum?

With 2D DP, this is easy.

Suppose:

```text
dp[i][c]
```

After filling the table, start:

```text
i = n
c = W
```

Compare:

```text
dp[i][c]
```

with:

```text
dp[i - 1][c]
```

If equal:

```text
item wasn't selected
```

Otherwise:

```text
item was selected
```

Then:

```text
c -= weight[i - 1]
i--
```

Continue.

This reconstructs the chosen set.

---

# 31. Space Complexity

For:

```text
n items
capacity W
```

2D:

```text
Time:  O(nW)
Space: O(nW)
```

1D:

```text
Time:  O(nW)
Space: O(W)
```

Notice:

> Space optimization does NOT improve time complexity.

It only reduces memory.

---

# 32. A Very Important Limitation

Knapsack's:

```text
O(nW)
```

looks polynomial.

But technically, this is called **pseudo-polynomial**.

Why?

Because `W` is a numerical value.

Suppose:

```text
W = 1,000,000,000
```

Then:

```text
O(nW)
```

is enormous.

The complexity is polynomial in the **numeric value** of `W`, not necessarily in the number of bits needed to represent W.

This distinction is often discussed in more advanced algorithm interviews.

---

# 33. Common Knapsack Variations

You should know these families:

### 0/1 Knapsack

```text
each item once
```

### Unbounded Knapsack

```text
item unlimited
```

### Bounded Knapsack

```text
item can be used k times
```

For example:

```text
A → at most 3 copies
B → at most 5 copies
```

This sits between 0/1 and unbounded.

---

### Multiple Knapsack

There may be multiple constraints:

```text
weight <= W
volume <= V
```

Then your DP might become:

```text
dp[weight][volume]
```

and complexity increases accordingly.

---

# 34. 0/1 Knapsack → Subset Sum

Suppose every item has:

```text
value = weight
```

Then maximizing value under capacity becomes:

> What's the largest subset sum ≤ W?

And if the question is:

> Can we achieve exactly W?

you get **subset sum**.

For example:

```text
nums = [2, 3, 7]
target = 5
```

We ask:

```text
Can some subset sum to 5?
```

Answer:

```text
2 + 3
```

DP:

```java
boolean[] dp = new boolean[target + 1];

dp[0] = true;

for (int num : nums) {
    for (int sum = target; sum >= num; sum--) {
        dp[sum] = dp[sum] || dp[sum - num];
    }
}
```

Notice:

```text
backwards
```

because each number can only be used once.

---

# 35. Partition Equal Subset Sum

Classic interview problem.

Given:

```text
[1, 5, 11, 5]
```

Can we split it into two subsets with equal sum?

Total:

```text
22
```

Each subset needs:

```text
11
```

So the problem becomes:

> Can I select a subset whose sum is 11?

That's subset sum.

Which is:

```text
0/1 knapsack
```

This is a powerful recognition trick.

---

# 36. Target Sum

Problems involving:

```text
+
-
```

can sometimes transform into subset sum.

Suppose:

```text
nums = [...]
```

and you assign `+` or `-`.

Let:

```text
P = positive subset
N = negative subset
```

Then:

```text
P - N = target
```

and:

```text
P + N = total
```

Adding:

```text
2P = target + total
```

so:

```text
P = (target + total) / 2
```

Now the problem becomes:

> Count subsets with sum P.

Again:

```text
0/1 knapsack
```

This is an important example where the problem doesn't look like knapsack at first.

---

# 37. Rod Cutting

Suppose a rod has length:

```text
8
```

and:

| Length | Price |
| ------ | ----: |
| 1      |     1 |
| 2      |     5 |
| 3      |     8 |
| 4      |     9 |

You can cut pieces repeatedly.

This is:

```text
unbounded knapsack
```

because a piece length can be selected multiple times.

For example:

```text
2 + 2 + 2 + 2
```

is allowed.

---

# 38. A Practical Recognition Cheat Sheet

When you see:

### "Each item can be selected once"

Think:

```text
0/1 knapsack
```

### "You can use an item unlimited times"

Think:

```text
unbounded knapsack
```

### "Can we make target sum?"

Think:

```text
subset sum
→ 0/1 knapsack
```

### "Can we partition into equal sums?"

Think:

```text
subset sum
→ 0/1 knapsack
```

### "Minimum coins"

Think:

```text
unbounded knapsack
```

### "Number of ways to make amount"

Think:

```text
unbounded knapsack
```

### "Maximum profit with limited capacity"

Think:

```text
0/1 knapsack
```

---

# 39. The Biggest Mistakes People Make

## Mistake 1: Using forward iteration for 0/1

Wrong:

```java
for (int c = weight; c <= W; c++)
```

This can reuse the same item.

Correct:

```java
for (int c = W; c >= weight; c--)
```

---

## Mistake 2: Using backward iteration for unbounded

Wrong:

```java
for (int c = W; c >= weight; c--)
```

This prevents the current item from being reused.

Correct:

```java
for (int c = weight; c <= W; c++)
```

---

## Mistake 3: Not determining "once vs unlimited"

Before writing DP, explicitly ask:

```text
Can I reuse an item?
```

---

## Mistake 4: Confusing "exactly" and "at most"

These require different initialization/state interpretation.

---

## Mistake 5: Jumping to 1D DP

If you don't understand the 2D recurrence, 1D code becomes memorization.

Learn:

```text
2D → understand
1D → optimize
```

---

# 40. The One Formula You Should Derive, Not Memorize

Whenever you're stuck, write:

```text
For item i and capacity c:

             ┌── skip item
             │
dp(i,c) = max
             │
             └── take item
```

Then ask:

### Skip

```text
dp(previous item, same capacity)
```

### Take

```text
value
+
dp(what items remain?, remaining capacity)
```

For 0/1:

```text
previous item
```

For unbounded:

```text
same item
```

That's the entire foundation.

---

# 41. The Ultimate Mental Picture

Think of every DP cell as answering:

```text
                 CAPACITY
             0   1   2   3   4   5
           +-------------------------
ITEM 0     |
           |
ITEM 1     |     "What is the best
           |      answer using the
ITEM 2     |      items I've seen
           |      so far?"
           |
ITEM 3     |
```

At every item, you make one decision:

```text
                 ITEM
                /    \
             SKIP    TAKE
```

For 0/1:

```text
TAKE
 ↓
previous row
```

For unbounded:

```text
TAKE
 ↓
same row
```

And when compressed into 1D:

```text
0/1       → right-to-left
unbounded → left-to-right
```

---

# 42. What I Would Learn Next

If your goal is **interview-level mastery**, don't just memorize the two implementations. Practice this progression:

```text
1. 0/1 Knapsack
       ↓
2. 0/1 Knapsack with 1D DP
       ↓
3. Subset Sum
       ↓
4. Partition Equal Subset Sum
       ↓
5. Target Sum
       ↓
6. Unbounded Knapsack
       ↓
7. Coin Change I
       ↓
8. Coin Change II
       ↓
9. Rod Cutting
       ↓
10. Bounded Knapsack
```

The really valuable skill is being able to look at a new problem and say:

> **"This isn't literally asking about a bag, but each choice consumes a resource, there is a limited capacity, and I can use each choice once/unlimited times. This is knapsack."**

Once that recognition becomes automatic, a surprisingly large class of DP problems becomes much easier.

