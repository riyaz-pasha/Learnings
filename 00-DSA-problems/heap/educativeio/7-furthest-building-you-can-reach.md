This is a classic **Greedy + Heap** problem. The key interview insight is:

> **Ladders should be used for the largest climbs, because a ladder saves the most bricks when used on a large height difference.**

There are a couple of ways to implement this. I'll start with the approach I would recommend in an interview.

---

# 1. Greedy + Min Heap — Recommended

### Core idea

For every upward jump:

```text
difference = heights[i] - heights[i - 1]
```

We need to decide:

* use bricks for this difference, or
* use a ladder.

Since ladders have no "cost", we want them to cover the **largest differences**.

So we can maintain the climbs currently assigned to ladders in a **min-heap**.

### Why a min-heap?

Suppose:

```text
climbs = [4, 2, 7]
ladders = 2
```

The ladders should cover:

```text
7 and 4
```

and bricks should cover:

```text
2
```

If we add every positive climb to the heap:

```text
heap = [2, 4, 7]
```

But we only have 2 ladders.

So remove the **smallest** climb:

```text
remove 2
```

That means:

```text
ladder -> 4
ladder -> 7
brick  -> 2
```

This is exactly what a min-heap gives us.

---

## Java 24 Solution

```java
import java.util.*;

class Solution {

    public int furthestBuilding(int[] heights, int bricks, int ladders) {

        // Min-heap containing the climbs currently assigned to ladders.
        // The smallest climb is at the top.
        PriorityQueue<Integer> ladderClimbs = new PriorityQueue<>();

        for (int i = 1; i < heights.length; i++) {

            int climb = heights[i] - heights[i - 1];

            // Going down or staying at the same height costs nothing.
            if (climb <= 0) {
                continue;
            }

            // Initially assume that this climb will use a ladder.
            ladderClimbs.offer(climb);

            /*
             * If we have more ladder-assigned climbs than ladders,
             * we must convert one of them to a brick climb.
             *
             * Which one should become a brick climb?
             *
             * The SMALLEST one.
             *
             * Why?
             * Because ladders should be reserved for the largest climbs.
             */
            if (ladderClimbs.size() > ladders) {
                bricks -= ladderClimbs.poll();
            }

            /*
             * We couldn't afford the climb to building i.
             *
             * Therefore building i is NOT reachable.
             * The furthest reachable building is i - 1.
             */
            if (bricks < 0) {
                return i - 1;
            }
        }

        // We successfully reached the last building.
        return heights.length - 1;
    }
}
```

---

# Let's understand the algorithm with an example

```text
heights = [4, 2, 7, 6, 9, 14, 12]
bricks = 5
ladders = 1
```

Positive climbs:

```text
4 -> 2   = 0
2 -> 7   = 5
6 -> 9   = 3
9 -> 14  = 5
```

We have only **1 ladder**.

### Building 2

Climb = `5`

```text
heap = [5]
```

One ladder is available, so:

```text
ladder -> 5
bricks = 5
```

---

### Building 4

Climb = `3`

```text
heap = [3, 5]
```

We have 2 ladder candidates but only 1 ladder.

Remove the smallest:

```text
remove 3
```

Therefore:

```text
ladder -> 5
bricks -> 3
```

```text
bricks = 5 - 3 = 2
```

---

### Building 5

Climb = `5`

```text
heap = [5, 5]
```

Again, only one ladder.

Remove smallest `5`:

```text
bricks -= 5
```

```text
bricks = 2 - 5 = -3
```

We can't reach building `5`.

Therefore answer:

```text
4
```

---

# The Interview Way to Think About It

This is the most important part.

Imagine every upward jump is a bill:

```text
jump = 2   → bill of 2 bricks
jump = 8   → bill of 8 bricks
jump = 3   → bill of 3 bricks
jump = 10  → bill of 10 bricks
```

Ladders are like **free coupons**.

If you have 2 coupons, obviously you want to use them on:

```text
10 and 8
```

not:

```text
2 and 3
```

So the problem becomes:

> **Keep the largest `ladders` climbs free, and pay for everything else with bricks.**

That's the entire greedy insight.

The min-heap helps us maintain exactly those `ladders` largest climbs.

---

# Why Is This Greedy Correct?

Suppose we have:

```text
climbs = [2, 5, 10]
ladders = 2
```

There are only two possible climbs that can use ladders.

If we use a ladder on `2` and bricks on `10`, we're wasting the ladder.

Instead:

```text
ladder -> 5
ladder -> 10
brick  -> 2
```

Using a ladder on `10` instead of `2` saves:

```text
10 bricks instead of 2
```

So we gain an additional:

```text
10 - 2 = 8 bricks
```

This exchange argument tells us that whenever a smaller climb is using a ladder while a larger climb is using bricks, swapping them can never make the solution worse.

Therefore:

> **The optimal solution always uses ladders on the largest climbs.**

---

# Complexity

Let `n = heights.length`.

Each positive climb is inserted into the heap.

The heap contains at most `ladders` elements after adjustment.

Therefore:

### Time

```text
O(n log ladders)
```

If `ladders` can be `n`, this becomes:

```text
O(n log n)
```

### Space

```text
O(ladders)
```

This is better than maintaining all climbs when the number of ladders is small.

---

# 2. Alternative: Max Heap — Also Very Good

Another way to think about the problem:

> Initially pay for every climb using bricks. If we run out of bricks, convert the **largest brick expenditure** into a ladder.

This is also a very intuitive interview solution.

```java
import java.util.*;

class Solution {

    public int furthestBuilding(int[] heights, int bricks, int ladders) {

        // Largest climb currently paid for using bricks.
        PriorityQueue<Integer> brickClimbs =
                new PriorityQueue<>(Comparator.reverseOrder());

        for (int i = 1; i < heights.length; i++) {

            int climb = heights[i] - heights[i - 1];

            if (climb <= 0) {
                continue;
            }

            // Initially pay for this climb using bricks.
            bricks -= climb;

            // Remember this brick expenditure.
            brickClimbs.offer(climb);

            /*
             * If bricks are insufficient, use a ladder
             * on the largest climb we have paid for so far.
             *
             * This refunds the largest brick expenditure.
             */
            if (bricks < 0 && ladders > 0) {
                bricks += brickClimbs.poll();
                ladders--;
            }

            // Even after using a ladder, we don't have enough bricks.
            if (bricks < 0) {
                return i;
            }
        }

        return heights.length - 1;
    }
}
```

### Example

Suppose:

```text
climbs = [5, 3, 8]
bricks = 7
ladders = 1
```

Initially:

```text
pay 5 → bricks = 2
pay 3 → bricks = -1
```

We're out of bricks.

Largest brick expenditure:

```text
5
```

Convert that to a ladder:

```text
bricks = -1 + 5 = 4
```

Now:

```text
ladder -> 5
brick  -> 3
```

Then climb `8`:

```text
bricks = 4 - 8 = -4
```

Use ladder on `8`? We already used our only ladder.

So we stop.

---

# 3. Comparison of the Two Heap Approaches

| Approach     | Heap           | Main Idea                                                    | Complexity              |
| ------------ | -------------- | ------------------------------------------------------------ | ----------------------- |
| **Min Heap** | Smallest climb | Keep largest `ladders` climbs for ladders                    | `O(n log ladders)`      |
| **Max Heap** | Largest climb  | Pay with bricks first, then refund largest climb with ladder | `O(n log n)` worst case |

I prefer the **Min Heap solution** because the invariant is very clean:

> **The heap contains exactly the climbs currently assigned to ladders.**

Whenever there are too many:

```java
if (ladderClimbs.size() > ladders) {
    bricks -= ladderClimbs.poll();
}
```

We remove the smallest one and pay for it with bricks.

---

# 4. Could We Use Sorting?

You might think:

```text
1. Find all positive climbs
2. Sort them
3. Give ladders to the largest climbs
4. Use bricks for the rest
```

The problem is that we need to stop at the **first building we cannot reach**.

For example:

```text
heights = [1, 5, 2, 100]
```

You can't simply decide globally which climbs get ladders without considering the prefix.

A heap allows us to make the optimal decision **online as we move from left to right**.

So:

```text
Sorting → possible but awkward / inefficient for this problem
Heap    → natural
```

---

# 5. Could We Use Dynamic Programming?

Technically, you could formulate states around:

```text
building index
bricks remaining
ladders remaining
```

But this is a terrible fit because:

```text
bricks <= 10^9
```

The state space involving bricks is enormous.

The greedy property eliminates the need for DP entirely.

---

# 6. Could We Use Binary Search?

There is another interesting approach.

We can binary-search the answer:

```text
Can I reach building mid?
```

For a given prefix, calculate all positive climbs and determine whether the largest `ladders` climbs can be covered by ladders while the rest fit within `bricks`.

That gives roughly:

```text
O(n log n)
```

for each feasibility check if sorting is used, resulting in approximately:

```text
O(n log² n)
```

Overall, this is inferior to the heap solution.

---

# What I Would Say in an Interview

A concise explanation would be:

> "For every upward jump, I need either bricks equal to the height difference or one ladder. Since ladders have a fixed count and no cost, they should be assigned to the largest climbs. I'll maintain the climbs currently assigned to ladders in a min-heap. Whenever the number of ladder candidates exceeds the number of ladders, I remove the smallest climb from the heap and pay for it with bricks. This guarantees that the ladders are always reserved for the largest climbs seen so far. If bricks become negative, we cannot reach the current building."

Then code:

```java
import java.util.*;

class Solution {

    public int furthestBuilding(int[] heights, int bricks, int ladders) {

        // The heap contains the climbs currently covered by ladders.
        // Smallest climb is at the top.
        PriorityQueue<Integer> ladderClimbs = new PriorityQueue<>();

        for (int i = 1; i < heights.length; i++) {

            int climb = heights[i] - heights[i - 1];

            // No resources are needed when moving down or staying level.
            if (climb <= 0) {
                continue;
            }

            // Tentatively assign this climb to a ladder.
            ladderClimbs.offer(climb);

            /*
             * We have more ladder candidates than available ladders.
             * Remove the smallest climb and pay for it with bricks.
             *
             * This leaves the largest `ladders` climbs in the heap.
             */
            if (ladderClimbs.size() > ladders) {
                bricks -= ladderClimbs.poll();
            }

            // We cannot pay for this climb, so stop here.
            if (bricks < 0) {
                return i;
            }
        }

        return heights.length - 1;
    }
}
```

### The pattern to remember

This problem is part of a very useful interview pattern:

```text
"Choose K largest/smallest things dynamically"
                    ↓
                 Heap
```

More specifically:

```text
Limited free resources
        +
Different costs
        +
Want to reserve free resources for expensive operations
        ↓
Use free resources on the largest costs
        ↓
Heap
```

That same thought process appears in many **Greedy + Heap** interview problems.

