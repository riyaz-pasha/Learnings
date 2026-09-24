# DIY: Flood Fill

## Problem statement

An image is a 2D array of integers, each representing a pixel value between 0 and 65535. Given a starting pixel `(sr, sc)` and a new color `newColor`, flood fill the image: recolor the starting pixel, plus every pixel connected to it 4-directionally (directly or transitively) that shares the starting pixel's original color, to `newColor`. Return the modified image.

### Input

```java
image = {{1, 1, 1}, {1, 1, 0}, {1, 0, 1}}
sr = 1
sc = 1
newColor = 2
```

### Output

```
{{2, 2, 2}, {2, 2, 0}, {2, 0, 1}}
```

## Coding exercise

Implement `floodFill(image, sr, sc, newColor)`, returning the modified image.

This is the exact same pattern as [Feature #5: Update VLAN ID](05-feature-5-update-vlan-id.md) — there, a VLAN ID change propagated to every connected switch sharing the old ID; here it's the bare pattern, no networking story. DFS from the starting pixel, recoloring it and recursing into same-colored 4-directional neighbors; the color change itself doubles as the visited check.

## Solution

```java
class Solution {
    public static int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
        int oldColor = image[sr][sc];
        if (oldColor != newColor) {
            fill(image, sr, sc, oldColor, newColor);
        }
        return image;
    }

    private static void fill(int[][] image, int r, int c, int oldColor, int newColor) {
        if (r < 0 || c < 0 || r >= image.length || c >= image[0].length || image[r][c] != oldColor) {
            return;
        }
        image[r][c] = newColor;
        fill(image, r - 1, c, oldColor, newColor);
        fill(image, r + 1, c, oldColor, newColor);
        fill(image, r, c - 1, oldColor, newColor);
        fill(image, r, c + 1, oldColor, newColor);
    }

    public static void main(String[] args) {
        int[][] image = {{1, 1, 1}, {1, 1, 0}, {1, 0, 1}};
        int[][] result = floodFill(image, 1, 1, 2);
        for (int[] row : result) {
            System.out.println(java.util.Arrays.toString(row));
        }
        // [2, 2, 2]
        // [2, 2, 0]
        // [2, 0, 1]
    }
}
```

## Complexity measures

Let **n** be the number of pixels in the image.

- **Time:** `O(n)` — in the worst case, every pixel shares the starting color and gets visited once.
- **Space:** `O(n)` — the recursion stack can grow as deep as the number of connected same-colored pixels.
