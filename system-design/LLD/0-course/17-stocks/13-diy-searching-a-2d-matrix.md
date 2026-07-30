# DIY: Searching a 2D Matrix

## Problem statement

Given an `m x n` integer matrix and a target value, determine if the target exists in the matrix.

> The matrix has the following properties:
> - Integers in each row are sorted from left to right.
> - The first integer of each row is greater than the last integer of the previous row.

### Input

```java
matrix = {{1,3,5,7},{10,11,16,20},{23,30,34,60}}
target = 30
```

### Output

```java
true
```

The number `30` is present in the `m x n` matrix.

## Coding exercise

Implement `searchMatrix(matrix, target)`.

The closest match in this chapter is [Feature #4: Milestone Reached](04-feature-4-milestone-reached.md) — both properties described above (rows sorted left-to-right, each row's first value bigger than the previous row's last) are exactly what let us treat the whole matrix as one flattened sorted array and binary search it directly, without ever materializing that flattened array.

## Solution

```java
class Solution {
    // Returns true if target exists anywhere in the row-major-sorted matrix.
    public static boolean searchMatrix(int[][] matrix, int target) {
        int rows = matrix.length, cols = matrix[0].length;
        int left = 0, right = rows * cols - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            int row = mid / cols, col = mid % cols;
            int value = matrix[row][col];

            if (value == target) {
                return true;
            } else if (value < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int[][] matrix = {{1, 3, 5, 7}, {10, 11, 16, 20}, {23, 30, 34, 60}};
        System.out.println(searchMatrix(matrix, 30));
        // true
    }
}
```

Each virtual index `mid` maps to `(mid / cols, mid % cols)` in the real matrix, so the loop is a completely ordinary binary search — it just does an extra division and modulo to look up each candidate value.

## Complexity measures

Let **m** be the number of rows and **n** be the number of columns.

### Time Complexity

`O(log(m × n))` — a standard binary search over `m × n` virtual positions.

### Space Complexity

`O(1)` — only a constant number of index variables are used; the matrix is never copied.
