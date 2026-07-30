# DIY: Range Sum Query 2D — Immutable

## Problem statement

Given an m x n matrix, handle multiple queries of the following type: calculate the sum of the elements of the matrix inside the rectangle defined by its upper-left corner `(row1, col1)` and lower-right corner `(row2, col2)`.

### Constraints

- `m == matrix.length`, `n == matrix[i].length`
- `1 <= m, n <= 200`
- `-10^5 <= matrix[i][j] <= 10^5`
- `0 <= row1 <= row2 < m`
- `0 <= col1 <= col2 < n`

### Input

```java
// NumMatrix numMatrix = new NumMatrix(new int[][]{
//     {1, 3, 5},
//     {2, 4, 6},
//     {7, 8, 2},
//     {9, 3, 6}
// });
// numMatrix.sumRegion(0, 0, 2, 2);
// numMatrix.sumRegion(0, 1, 3, 2);
// numMatrix.sumRegion(2, 1, 3, 1);
```

### Output

```java
// 38
// 37
// 11
```

## Coding exercise

Implement the `NumMatrix` class with:

- `NumMatrix(int[][] matrix)`: initializes the object with the integer matrix.
- `int sumRegion(int row1, int col1, int row2, int col2)`: returns the sum of the elements inside the given rectangle.

This is exactly [Feature #4: Query Peak Users](04-feature-4-query-peak-users.md)'s `CacheSmart` approach — the same 2D prefix sum with inclusion-exclusion, just renamed away from the "peak users" framing to a generic numeric matrix.

## Solution

```java
class NumMatrix {
    private final int[][] cache;

    public NumMatrix(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        cache = new int[rows + 1][cols + 1];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cache[i + 1][j + 1] = cache[i + 1][j] + cache[i][j + 1] + matrix[i][j] - cache[i][j];
            }
        }
    }

    public int sumRegion(int row1, int col1, int row2, int col2) {
        return cache[row2 + 1][col2 + 1] - cache[row1][col2 + 1] - cache[row2 + 1][col1] + cache[row1][col1];
    }

    public static void main(String[] args) {
        NumMatrix numMatrix = new NumMatrix(new int[][]{
            {1, 3, 5},
            {2, 4, 6},
            {7, 8, 2},
            {9, 3, 6}
        });
        System.out.println(numMatrix.sumRegion(0, 0, 2, 2)); // 38
        System.out.println(numMatrix.sumRegion(0, 1, 3, 2)); // 37
        System.out.println(numMatrix.sumRegion(2, 1, 3, 1)); // 11
    }
}
```

## Solution walkthrough

The constructor builds `cache[i+1][j+1]` as the total sum of everything from the origin up to `(i, j)` inclusive, using previously-computed cells so each new cell costs constant work. Each query then applies inclusion-exclusion over the four corner sums, in constant time — a live run reproduces `38`, `37`, and `11`, matching the stated outputs.

## Complexity measures

Let **m** and **n** be the number of rows and columns.

### Time Complexity

`O(m * n)` to build the cache once in the constructor; `O(1)` per `sumRegion` query afterward.

### Space Complexity

`O(m * n)` — the cache grid has one extra row and column beyond the input matrix's dimensions.
