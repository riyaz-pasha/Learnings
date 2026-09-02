Yes — this becomes much easier once you see the **coordinate pattern** instead of memorizing formulas.

Assume every cell has coordinates:

```text
(row, col)
```

For a `4 x 4` board:

```text
        col
         0       1       2       3
      +-------+-------+-------+-------+
row 0 | (0,0) | (0,1) | (0,2) | (0,3) |
      +-------+-------+-------+-------+
row 1 | (1,0) | (1,1) | (1,2) | (1,3) |
      +-------+-------+-------+-------+
row 2 | (2,0) | (2,1) | (2,2) | (2,3) |
      +-------+-------+-------+-------+
row 3 | (3,0) | (3,1) | (3,2) | (3,3) |
      +-------+-------+-------+-------+
```

Now look at diagonals.

## `row - col` stays constant on one diagonal

Consider this diagonal:

```text
(0,0)
    \
     (1,1)
         \
          (2,2)
              \
               (3,3)
```

Calculate `row - col`:

```text
(0,0) → 0 - 0 = 0
(1,1) → 1 - 1 = 0
(2,2) → 2 - 2 = 0
(3,3) → 3 - 3 = 0
```

Every cell gives:

```text
row - col = 0
```

So all of these cells belong to the **same diagonal**.

Another example:

```text
(1,0)
    \
     (2,1)
         \
          (3,2)
```

Calculate:

```text
(1,0) → 1 - 0 = 1
(2,1) → 2 - 1 = 1
(3,2) → 3 - 2 = 1
```

Again:

```text
row - col = 1
```

Why does this happen?

When you move diagonally down-right:

```text
row += 1
col += 1
```

So:

```text
(row + 1) - (col + 1)

= row + 1 - col - 1

= row - col
```

The `+1` and `-1` cancel.

That is the mathematical reason.

---

## `row + col` stays constant on the other diagonal

Now consider:

```text
          (0,3)
           /
       (1,2)
       /
   (2,1)
   /
(3,0)
```

Calculate `row + col`:

```text
(0,3) → 0 + 3 = 3
(1,2) → 1 + 2 = 3
(2,1) → 2 + 1 = 3
(3,0) → 3 + 0 = 3
```

Every position has:

```text
row + col = 3
```

Why?

When moving down-left:

```text
row += 1
col -= 1
```

Therefore:

```text
(row + 1) + (col - 1)

= row + 1 + col - 1

= row + col
```

Again, the changes cancel.

---

## See the entire board

For `row - col`:

```text
             col
          0    1    2    3

row 0     0   -1   -2   -3

row 1     1    0   -1   -2

row 2     2    1    0   -1

row 3     3    2    1    0
```

Look at equal numbers:

```text
 0
     0
         0
             0
```

That's one diagonal.

Similarly:

```text
     -1
          -1
               -1
```

That's another diagonal.

So you can think:

```text
same row - col
        ↓
same diagonal
```

Now look at `row + col`:

```text
             col
          0    1    2    3

row 0     0    1    2    3

row 1     1    2    3    4

row 2     2    3    4    5

row 3     3    4    5    6
```

Again, equal numbers form diagonals:

```text
             3
         3
     3
 3
```

Therefore:

```text
same row + col
        ↓
same opposite diagonal
```

## One important correction

The names **diagonal** and **anti-diagonal** can vary depending on the explanation.

What's important is the formulas:

```java
// One diagonal direction:  \
// row and col increase together
row - col

// Other diagonal direction: /
// row increases while col decreases
row + col
```

So I'd prefer remembering the **movement**, not the names.

## Why do we write `row - col + (n - 1)`?

There is one small problem with:

```java
row - col
```

It can become negative.

For `n = 4`:

```text
minimum = 0 - 3 = -3
maximum = 3 - 0 =  3
```

So possible values are:

```text
-3 -2 -1 0 1 2 3
```

But Java arrays cannot have negative indexes:

```java
diagonal[-2] // ❌
```

Therefore we shift everything by `n - 1`.

For `n = 4`:

```text
n - 1 = 3
```

So:

```text
original       shifted

-3 + 3 = 0
-2 + 3 = 1
-1 + 3 = 2
 0 + 3 = 3
 1 + 3 = 4
 2 + 3 = 5
 3 + 3 = 6
```

Now every index is valid:

```text
0 1 2 3 4 5 6
```

That's why N-Queens often uses:

```java
int diagonal1 = row + col;

int diagonal2 = row - col + (n - 1);
```

For an `n × n` board, there are exactly:

```text
2n - 1
```

diagonals in each direction.

So ideally the arrays can be:

```java
boolean[] diagonal1 = new boolean[2 * n - 1];
boolean[] diagonal2 = new boolean[2 * n - 1];
```

rather than `2 * n`.

### The easiest interview memory trick

Don't memorize:

```text
↘ = ?
↙ = ?
```

Remember how coordinates change:

```text
Down-right \

row + 1
col + 1

difference stays same
→ row - col
```

```text
Down-left /

row + 1
col - 1

sum stays same
→ row + col
```

So:

```java
// \  → same difference
row - col

// /  → same sum
row + col
```

That is the underlying reason the N-Queens optimization works.

