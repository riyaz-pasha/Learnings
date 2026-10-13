# Letter Y in a Grid — Java Solution

The key idea is to count the frequencies of 0, 1, and 2 separately for Y-cells and non-Y-cells, then try every possible pair of values.

Since the grid contains only three possible values, there are just \\(3 \times 2 = 6\\) valid combinations to check.

## 1. Intuition: What are we trying to achieve?

We need two groups of cells:

- Y-cells: Every cell must contain the same value, say `yValue`.
- Non-Y-cells: Every cell must contain another value, say `otherValue`.
- These two values must be different.

For example, we could make all Y-cells `1` and all remaining cells `0`.

We want to choose the values that require the fewest changes.

### How do we identify the Y-cells?

[Tracing Alphabet letter Y, prewriting dotted line element for kindergarten, preschool and Montessori school kids worksheet for handwriting practice activity.](https://images.openai.com/static-rsc-4/fPRfjqXERVacVjABQ1ibGV08p4gYQCRSqAaUJpvzahbyFVqj0jAz5FyMn7DUN9uFJZ-J5JLZ0fPaevumuay4nUESfsw1e-wGX1oUCIA2sojpApewiv-H_sqCJ5fQNq7WpPvx5azNFnqx2cIl7vMcEK5-ryg3vT3ztvCjMA5bKQw7KzRiavO7SMD5cU4e2shx?purpose=inline)

1\. Upper-left diagonal

Cells where `r == c`, from the top-left corner to the center.

[Tracing Alphabet letter Y, Uppercase prewriting dotted line element for kindergarten, preschool kids worksheet for handwriting practice activity.](https://images.openai.com/static-rsc-4/yt-Fu_hxCGOpYADTifr7Vz7BLEaneWiuZEFUeRGRQdnlEXzhCzJscPknG4LOrou77nt9GIRkapIrUeIWZZmhu6wbZ1p23pgg4CvW-HLqqXsT9KLs6zPUjWtGQlAfjlksPwU2m3FmvM6avNYwumMpl_tc-qDPn31jn8IG3AeZ6f-S4V0zfXFETUE_cYv-4-E1?purpose=inline)

2\. Upper-right diagonal

Cells where `r + c == n - 1`, from the top-right corner to the center.

[Tracing Alphabet letter Y prewriting dotted line element for kindergarten, preschool and Montessori school kids worksheet for handwriting practice activity.](https://images.openai.com/static-rsc-4/Gb3t4ouBMK3d9MnB5SMnFbthmZIt5GP3G7abV4QcwJDVcy5Hn9Lvo0Msn6D5PtJV2DiPHSkjACK9rSGhYFpkofcrsNdKSqMkNTFIsL1zTvjYCsHhiJZUv-YpSY-kxxIA38cvZGStKrRqIpsrQNKy45WIn-9-JR2JbLQvmBtwQGQh92HGdksyd1ffI-eCJKP3?purpose=inline)

3\. Vertical stem

Cells where `c == n / 2`, from the center to the bottom row.

The center cell belongs to both diagonals and the vertical stem conceptually, but we count each cell only once.

The complete condition for a Y-cell is:

```
boolean isY = (r == c && r <= n / 2)
          || (r + c == n - 1 && r <= n / 2)
          || (c == n / 2 && r >= n / 2);
```

Here, `n / 2` gives the center index because `n` is odd.

## 2. Java solution — frequency counting

```
class Solution {

    public int minimumOperationsToWriteY(int[][] grid) {
        int n = grid.length;
        int center = n / 2;

        // yCount[v] = number of Y-cells currently containing v.
        // otherCount[v] = number of non-Y-cells currently containing v.
        int[] yCount = new int[3];
        int[] otherCount = new int[3];

        int yTotal = 0;
        int otherTotal = 0;

        // Step 1: Classify every cell and count its value.
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {

                // A cell belongs to Y if it is on either upper diagonal
                // or on the vertical stem below the center.
                boolean isY =
                        (r == c && r <= center)
                        || (r + c == n - 1 && r <= center)
                        || (c == center && r >= center);

                int value = grid[r][c];

                if (isY) {
                    yCount[value]++;
                    yTotal++;
                } else {
                    otherCount[value]++;
                    otherTotal++;
                }
            }
        }

        int answer = Integer.MAX_VALUE;

        // Step 2: Try all possible values for the Y and non-Y groups.
        // Their values must be different.
        for (int yValue = 0; yValue < 3; yValue++) {
            for (int otherValue = 0; otherValue < 3; otherValue++) {

                if (yValue == otherValue) {
                    continue;
                }

                // Keep cells already having the required value.
                // Change every other cell in its group.
                int operations =
                        (yTotal - yCount[yValue])
                        + (otherTotal - otherCount[otherValue]);

                answer = Math.min(answer, operations);
            }
        }

        return answer;
    }
}
```

## 3. Dry run with an example

Consider this grid:

```
grid = [
    [1, 0, 2],
    [0, 1, 0],
    [2, 2, 0]
]
```

The Y-shaped cells are marked `Y`, and the remaining cells are marked `N`.

# 1

Y

# 0

N

# 2

Y

# 0

N

# 1

Y

# 0

N

# 2

N

# 2

Y

# 0

N

Y-cell

Non-Y cell

### Step 1: Count the frequencies

Y-cells contain `1, 2, 1, 1, 2`, so their frequencies are:

| Value | Y-cells |
| ----- | ------- |
| 0     | 0       |
| 1     | 3       |
| 2     | 2       |

Non-Y cells contain `0, 0, 0, 2`, so their frequencies are:

| Value | Non-Y cells |
| ----- | ----------- |
| 0     | 3           |
| 1     | 0           |
| 2     | 1           |

There are 5 Y-cells and 4 non-Y cells.

### Step 2: Try different combinations

The formula is:

\\[ \begin{aligned} \text{operations}={}& (\text{Y total}-\text{Y count}[y])\\\ &+(\text{non-Y total}-\text{non-Y count}[o]) \end{aligned} \\]

where \\(y \ne o\\).

| Y value | Non-Y value | Y changes | Non-Y changes | Total |
| ------- | ----------- | --------- | ------------- | ----- |
| 0       | 1           | 5         | 4             | 9     |
| 0       | 2           | 5         | 3             | 8     |
| 1       | 0           | 2         | 1             | 3     |
| 1       | 2           | 2         | 3             | 5     |
| 2       | 0           | 3         | 1             | 4     |
| 2       | 1           | 3         | 4             | 7     |

The minimum is 3 operations.

Choose Y-value `1` and non-Y-value `0`. We change the two Y-cells containing `2` and the non-Y cell containing `2` to their required values.

## 4. Why does the formula work?

Suppose there are 10 Y-cells, and 7 already contain `1`.

If we choose `1` as the final Y-value, we must change only the remaining \\(10-7=3\\) cells.

Therefore:

\\[ \text{Y changes}=\text{Y total}-\text{Y count}[y] \\]

The same reasoning applies to non-Y cells:

\\[ \text{Non-Y changes}=\text{Non-Y total}-\text{Non-Y count}[o] \\]

Since the two groups do not overlap, their required changes can simply be added.

We examine every possible pair of distinct values, so the minimum across all six combinations must be optimal.

## 5. Complexity analysis

Let \\(n\\) be the grid dimension.

- Time complexity: \\(O(n^2)\\), because we visit every grid cell once. The six combinations take constant time.
- Auxiliary space complexity: \\(O(1)\\), because the two frequency arrays each contain exactly three counters.

## 6. Alternative solution: Try all value pairs directly

We can also try each of the six pairs and scan the entire grid to count the required changes for each pair.

```
class Solution {

    public int minimumOperationsToWriteY(int[][] grid) {
        int n = grid.length;
        int center = n / 2;

        int answer = Integer.MAX_VALUE;

        // Try all six pairs of distinct values.
        for (int yValue = 0; yValue < 3; yValue++) {
            for (int otherValue = 0; otherValue < 3; otherValue++) {

                if (yValue == otherValue) {
                    continue;
                }

                int operations = 0;

                for (int r = 0; r < n; r++) {
                    for (int c = 0; c < n; c++) {

                        boolean isY =
                                (r == c && r <= center)
                                || (r + c == n - 1 && r <= center)
                                || (c == center && r >= center);

                        int requiredValue = isY ? yValue : otherValue;

                        if (grid[r][c] != requiredValue) {
                            operations++;
                        }
                    }
                }

                answer = Math.min(answer, operations);
            }
        }

        return answer;
    }
}
```

- Time: \\(O(6n^2)=O(n^2)\\), since six is a constant.
- Auxiliary space: \\(O(1)\\).

Both approaches are correct. However, frequency counting is preferable in interviews because it separates the geometric problem (identifying Y-cells) from the optimization problem (choosing the best two values). It also avoids scanning the grid six times.

### Interview takeaway

When a problem asks you to minimize changes so that two groups each have a uniform value, ask yourself:

1. Can I identify the groups independently?
2. Can I count how many cells already have each possible value?
3. Can I enumerate all valid target-value combinations?

Here, the alphabet is tiny (`0`, `1`, `2`), making exhaustive enumeration combined with frequency counting a simple and optimal solution.



---













# Letter Y in a Grid — Understanding the Idea from First Principles

Let's build the solution from scratch, as if you're solving this problem in a coding interview. The goal is not just to understand the code, but to understand how to arrive at the solution yourself.

The most important insight is this:

We don't need to decide which individual cells to change. We need to decide which value the Y-group should have and which value the non-Y group should have.

Once we make those two decisions, the minimum number of changes becomes easy to calculate.

## 1. First, understand what the problem is asking

We're given a square grid containing only `0`, `1`, and `2`.

For example:

```
grid = [
    [1, 0, 2],
    [0, 1, 0],
    [2, 2, 0]
]
```

We need to transform it into a letter Y.

The rules are:

1. Every cell belonging to the letter Y must have the same value.
2. Every cell outside the letter Y must have the same value.
3. The value used for Y must be different from the value used outside Y.

Notice that we can change any cell to any of the three values in one operation.

For example, this is a valid final grid:

```
1 0 1
0 1 0
2 1 0
```

Here, all Y-cells are `1`, and all non-Y cells are `0` except for the bottom-left cell which belongs to the non-Y group in this grid? Let's classify it carefully before using an example: for a 3 × 3 grid, the Y-cells are the top-left, top-right, center, and bottom-center cells. The bottom-left cell is outside Y, so the example above needs its bottom-left value changed to `0` to be valid.

A valid target grid is therefore:

```
1 0 1
0 1 0
0 1 0
```

All Y-cells are `1`, and all non-Y cells are `0`.

The question is: How can we achieve a valid grid using the fewest operations?

## 2. Understand the shape before thinking about optimization

For an odd-sized grid, the center is at:

\\[ \text{center} = \left\lfloor \frac{n}{2} \right\rfloor \\]

For a 5 × 5 grid, the center index is `2`.

The Y consists of three parts:

Y

·

·

·

Y

·

Y

·

Y

·

·

·

Y

·

·

·

·

Y

·

·

·

·

Y

·

·

Green cells belong to Y. Gray cells are outside Y.

The three parts are:

- Upper-left diagonal: `r == c && r <= center`
- Upper-right diagonal: `r + c == n - 1 && r <= center`
- Vertical stem: `c == center && r >= center`

The center belongs to all three descriptions conceptually, but our Boolean condition identifies it as one cell, so it is counted only once.

This classification is important, but it is only the first part of the problem. The real challenge is deciding which values to assign to the two groups.

## 3. The key observation: There are only two groups

Forget the exact cell positions for a moment.

Imagine that we've already separated the grid into two baskets:

# Y

Group A: Y-cells

Every cell in this group must eventually become one common value.

# N

Group B: Non-Y cells

Every cell in this group must eventually become another common value.

The two groups must use different values.

Since the only available values are `0`, `1`, and `2`, our choices are very limited.

For example:

- Y = `0`, non-Y = `1`
- Y = `0`, non-Y = `2`
- Y = `1`, non-Y = `0`
- Y = `1`, non-Y = `2`
- Y = `2`, non-Y = `0`
- Y = `2`, non-Y = `1`

There are exactly six possible assignments.

Why six?

- Three choices for the Y-value.
- Two remaining choices for the non-Y value, because it cannot equal the Y-value.

Therefore:

\\[ 3 \times 2 = 6 \\]

This is the breakthrough: instead of trying to figure out which cells to modify individually, we can evaluate all six possible assignments and choose the cheapest one.

## 4. How do we calculate the number of operations?

Let's use our example again:

```
1 0 2
0 1 0
2 2 0
```

First, classify the cells correctly:

# 1

Y-cell

# 0

Non-Y

# 2

Y-cell

# 0

Non-Y

# 1

Y-cell

# 0

Non-Y

# 2

Non-Y

# 2

Y-cell

# 0

Non-Y

There are 4 Y-cells and 5 non-Y cells in a 3 × 3 grid.

Correction to the earlier dry run: the Y has four unique cells here, not five. The correct counts are:

| Value | Y-cells | Non-Y cells |
| ----- | ------- | ----------- |
| `0`   | 0       | 4           |
| `1`   | 2       | 0           |
| `2`   | 2       | 1           |
| Total | 4       | 5           |

Now let's derive the formula rather than memorizing it.

### Example A: Suppose Y must become `1`

The Y-cells currently contain:

```
1, 2, 1, 2
```

We want all of them to become `1`.

What should we do?

- The two cells already containing `1` need no changes.
- The two cells containing `2` must change to `1`.

So the number of Y-cell changes is `2`.

Mathematically:

\\[ \text{Y changes} = 4 - 2 = 2 \\]

In general:

\\[ \boxed{\text{Y changes}=\text{yTotal}-\text{yCount}[yValue]} \\]

Why subtract?

Because `yCount[yValue]` represents the cells that are already correct. Every other Y-cell must change.

### Example B: Suppose non-Y cells must become `0`

The non-Y cells currently contain:

```
0, 0, 0, 2, 0
```

We want all of them to become `0`.

- Four cells already contain `0`.
- One cell contains `2` and must change.

Therefore:

\\[ \text{Non-Y changes}=5-4=1 \\]

In general:

\\[ \boxed{\text{Non-Y changes}=\text{otherTotal}-\text{otherCount}[o]} \\]

### Example C: Combine both groups

We selected:

- Y-value = `1`
- Non-Y value = `0`

The total number of operations is:

\\[ \begin{aligned} \text{operations} &=(4-2)+(5-4)\\\ &=2+1\\\ &=\boxed{3} \end{aligned} \\]

We can achieve the required pattern in three changes.

The key idea is that each group can be optimized independently once we choose its target value. The only dependency between the groups is that their target values must be different.

## 5. Why do we count frequencies instead of changing cells directly?

Imagine the Y-cells contain:

```
1, 1, 1, 1, 2, 2, 0
```

Suppose we choose `1` as the target value.

Instead of visiting every cell to decide what to do, we can count:

```
Count of 0 = 1
Count of 1 = 4
Count of 2 = 2
```

There are seven cells, and four are already correct.

So:

\\[ \text{changes}=7-4=3 \\]

We don't need to know which three cells contain the wrong values. Their identities do not affect the minimum count.

This is a common algorithmic pattern:

> If we want to make every element in a group equal to one chosen value, keep the elements already matching that value and change everything else.

That is why frequency counting is useful.

## 6. How do we find the best pair?

Now let's calculate all six possible assignments using our example.

The counts are:

- Y: `0 → 0`, `1 → 2`, `2 → 2`
- Non-Y: `0 → 4`, `1 → 0`, `2 → 1`

The total formula is:

\\[ \begin{aligned} \text{operations}(y,o)={}& \text{yTotal}-\text{yCount}[y]\\\ &+\text{otherTotal}-\text{otherCount}[o] \end{aligned} \\]

Here, \\(y \ne o\\).

| Y target | Non-Y target | Y changes | Non-Y changes | Total |
| -------- | ------------ | --------- | ------------- | ----- |
| 0        | 1            | 4         | 5             | 9     |
| 0        | 2            | 4         | 4             | 8     |
| 1        | 0            | 2         | 1             | 3     |
| 1        | 2            | 2         | 4             | 6     |
| 2        | 0            | 2         | 1             | 3     |
| 2        | 1            | 2         | 5             | 7     |

There are two optimal choices:

- Y = `1`, non-Y = `0`: 3 operations.
- Y = `2`, non-Y = `0`: 3 operations.

Both are optimal. We only need to return the minimum number, which is `3`.

Notice that we are not required to find a unique final grid. Multiple configurations can achieve the same minimum.

## 7. How should you think of the algorithm in an interview?

I recommend following this reasoning sequence.

## 1

Separate the cells into two groups

Identify whether each cell belongs to Y or lies outside it.

## 2

Count the values in each group

Use two arrays of size three. One stores Y frequencies; the other stores non-Y frequencies.

## 3

Enumerate all valid target pairs

Try all combinations where `yValue != otherValue`. There are only six.

## 4

Calculate and minimize the cost

For each pair, count how many cells are already correct and subtract from the group totals. Keep the smallest number of changes.

### Why is this optimal?

We can prove it in two steps:

1. For a fixed pair of target values, the minimum changes are exactly the cells that do not match their group's target. There is no better way to achieve that particular pair.
2. We examine every valid pair. Since the values must differ and there are only three possible values, the six pairs cover every valid final configuration.

Therefore, taking the minimum across all six pairs guarantees the globally optimal answer.

## 8. Complexity and the pattern to remember

Let \\(n\\) be the number of rows and columns.

- Time complexity: \\(O(n^2)\\) to classify and count the cells, plus \\(O(1)\\) to examine six combinations.
- Auxiliary space complexity: \\(O(1)\\), because we use two arrays with three elements each.

The reusable pattern: When a problem asks you to make two groups uniform, and each group must use a different value from a small set, count frequencies first, then enumerate distinct target values.

You can apply the same idea to problems involving recoloring, partitioning, replacing characters, or minimizing edits when the number of possible target values is small.





