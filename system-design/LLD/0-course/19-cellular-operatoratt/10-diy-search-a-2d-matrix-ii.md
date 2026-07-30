# DIY: Search a 2D Matrix II

## Problem statement

Write an algorithm that searches for a target value in an m x n integer matrix.

The matrix has the following properties:

- Integers in each row are sorted in ascending order from left to right.
- Integers in each column are sorted in ascending order from top to bottom.

### Input

```java
// matrix = {{1,2,3,4,5},{6,7,8,9,10},{11,12,13,14,15},{16,17,18,19,20},{21,22,23,24,25}}
// target = 25
```

### Output

```java
// true — the number 25 is present in the matrix
```

## Coding exercise

Implement the `searchMatrix(matrix, target)` function, where `matrix` is the m x n matrix of integers and `target` is the value that needs to be searched. The function returns a Boolean representing whether `target` exists in `matrix`.

This is exactly [Feature #1: Determine Location](01-feature-1-determine-location.md) — the same staircase search starting from the bottom-left corner, just renamed away from the signal-loss framing and generalized to any target value instead of a fixed threshold.

## Solution

```java
class Solution {
    public static boolean searchMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0) {
            return false;
        }
        int row = matrix.length - 1;
        int col = 0;
        int maxCol = matrix[0].length - 1;

        while (row >= 0 && col <= maxCol) {
            int cur = matrix[row][col];
            if (cur == target) {
                return true;
            } else if (cur > target) {
                row--;
            } else {
                col++;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int[][] matrix = {
            {1, 2, 3, 4, 5},
            {6, 7, 8, 9, 10},
            {11, 12, 13, 14, 15},
            {16, 17, 18, 19, 20},
            {21, 22, 23, 24, 25}
        };
        System.out.println(searchMatrix(matrix, 25)); // true
    }
}
```

Starting from the bottom-left corner, every step eliminates either a whole row (when the current value is too big, move up) or a whole column (when it's too small, move right) — exactly the elimination rule from Feature #1, just searching for an arbitrary `target` instead of a fixed `threshold`.

## Complexity measures

Let **m** be the number of rows and **n** be the number of columns.

### Time Complexity

`O(m + n)` — each step advances the row pointer up or the column pointer right, and each can happen at most `m` and `n` times respectively.

### Space Complexity

`O(1)` — only two pointers are used regardless of matrix size.
