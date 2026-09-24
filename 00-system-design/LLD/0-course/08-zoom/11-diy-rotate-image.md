# DIY: Rotate Image

## Problem statement

Given a 2D array representing an image, implement a function that rotates the image 90 degrees clockwise, in place.

### Constraints

- `matrix.length == n`
- `matrix[i].length == n`
- `1 <= n <= 20`
- `-1000 <= matrix[i][j] <= 1000`

### Input

```java
matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}
```

### Output

```java
{{7, 4, 1}, {8, 5, 2}, {9, 6, 3}}
```

## Coding exercise

Implement `autoRotate(matrix)`, returning the matrix rotated 90 degrees clockwise.

This is the exact same pattern as [Feature #5: Auto Rotate in Mobile Devices](05-feature-5-auto-rotate-in-mobile-devices.md) — there, Zoom rotated a participant's profile picture when a phone flipped orientation; here it's the bare pattern, no story attached. Rotate ring by ring, from the outside in, swapping four corresponding cells at a time so no extra matrix is needed.

## Solution

```java
import java.util.*;

class Solution {
    public static int[][] autoRotate(int[][] matrix) {
        int left = 0;
        int right = matrix.length - 1;

        while (left < right) {
            for (int i = 0; i < right - left; i++) {
                int top = left;
                int bottom = right;
                int topLeft = matrix[top][left + i];

                matrix[top][left + i] = matrix[bottom - i][left];
                matrix[bottom - i][left] = matrix[bottom][right - i];
                matrix[bottom][right - i] = matrix[top + i][right];
                matrix[top + i][right] = topLeft;
            }
            left++;
            right--;
        }
        return matrix;
    }

    public static void main(String[] args) {
        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int[][] rotated = autoRotate(matrix);
        for (int[] row : rotated) {
            System.out.println(Arrays.toString(row));
        }
        // [7, 4, 1]
        // [8, 5, 2]
        // [9, 6, 3]
    }
}
```

## Complexity measures

Let **n** be the number of cells in the matrix.

- **Time:** `O(n)` — every cell is moved exactly once, across all rings.
- **Space:** `O(1)` — the rotation is done in place with a single temp variable per swap.
