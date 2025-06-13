/*
 * Given an array of non-negative integers arr, you are initially positioned at
 * start index of the array. When you are at index i, you can jump to i + arr[i]
 * or i - arr[i], check if you can reach any index with value 0.
 * 
 * Notice that you can not jump outside of the array at any time.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: arr = [4,2,3,0,3,1,2], start = 5
 * Output: true
 * Explanation:
 * All possible ways to reach at index 3 with value 0 are:
 * index 5 -> index 4 -> index 1 -> index 3
 * index 5 -> index 6 -> index 4 -> index 1 -> index 3
 * 
 * Example 2:
 * 
 * Input: arr = [4,2,3,0,3,1,2], start = 0
 * Output: true
 * Explanation:
 * One possible way to reach at index 3 with value 0 is:
 * index 0 -> index 4 -> index 1 -> index 3
 * 
 * Example 3:
 * 
 * Input: arr = [3,0,2,1,2], start = 2
 * Output: false
 * Explanation: There is no way to reach at index 1 with value 0.
 */

import java.util.*;

// Time: O(n) where n is array length - each position visited at most once
// Space: O(n) for visited array and queue/stack
class JumpToZero {

    // Solution 1: BFS (Breadth-First Search)
    public boolean canReachBFS(int[] arr, int start) {
        if (arr[start] == 0)
            return true;

        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[arr.length];

        queue.offer(start);
        visited[start] = true;

        while (!queue.isEmpty()) {
            int curr = queue.poll();

            // Try both possible jumps
            int[] nextPositions = { curr + arr[curr], curr - arr[curr] };

            for (int next : nextPositions) {
                if (next >= 0 && next < arr.length && !visited[next]) {
                    if (arr[next] == 0)
                        return true;

                    visited[next] = true;
                    queue.offer(next);
                }
            }
        }

        return false;
    }

    // Solution 2: DFS (Depth-First Search) - Recursive
    public boolean canReachDFS(int[] arr, int start) {
        boolean[] visited = new boolean[arr.length];
        return dfs(arr, start, visited);
    }

    private boolean dfs(int[] arr, int curr, boolean[] visited) {
        if (arr[curr] == 0)
            return true;

        visited[curr] = true;

        // Try both possible jumps
        int[] nextPositions = { curr + arr[curr], curr - arr[curr] };

        for (int next : nextPositions) {
            if (next >= 0 && next < arr.length && !visited[next]) {
                if (dfs(arr, next, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Solution 3: DFS (Iterative with Stack)
    public boolean canReachDFSIterative(int[] arr, int start) {
        if (arr[start] == 0)
            return true;

        Stack<Integer> stack = new Stack<>();
        boolean[] visited = new boolean[arr.length];

        stack.push(start);
        visited[start] = true;

        while (!stack.isEmpty()) {
            int curr = stack.pop();

            // Try both possible jumps
            int[] nextPositions = { curr + arr[curr], curr - arr[curr] };

            for (int next : nextPositions) {
                if (next >= 0 && next < arr.length && !visited[next]) {
                    if (arr[next] == 0)
                        return true;

                    visited[next] = true;
                    stack.push(next);
                }
            }
        }

        return false;
    }

    // Optimized Solution: Early termination for zero values
    public boolean canReachOptimized(int[] arr, int start) {
        // Quick check: if start position has 0
        if (arr[start] == 0)
            return true;

        // Check if there are any zeros in the array
        boolean hasZero = false;
        for (int val : arr) {
            if (val == 0) {
                hasZero = true;
                break;
            }
        }
        if (!hasZero)
            return false;

        // Use BFS for shortest path
        Queue<Integer> queue = new LinkedList<>();
        boolean[] visited = new boolean[arr.length];

        queue.offer(start);
        visited[start] = true;

        while (!queue.isEmpty()) {
            int curr = queue.poll();

            // Calculate next positions
            int forward = curr + arr[curr];
            int backward = curr - arr[curr];

            // Check forward jump
            if (forward < arr.length && !visited[forward]) {
                if (arr[forward] == 0)
                    return true;
                visited[forward] = true;
                queue.offer(forward);
            }

            // Check backward jump
            if (backward >= 0 && !visited[backward]) {
                if (arr[backward] == 0)
                    return true;
                visited[backward] = true;
                queue.offer(backward);
            }
        }

        return false;
    }

    // Test method
    public static void main(String[] args) {
        JumpToZero solution = new JumpToZero();

        // Test cases
        int[] arr1 = { 4, 2, 3, 0, 3, 1, 2 };
        int start1 = 5;
        System.out.println("Test 1 - BFS: " + solution.canReachBFS(arr1, start1)); // true
        System.out.println("Test 1 - DFS: " + solution.canReachDFS(arr1, start1)); // true

        int[] arr2 = { 4, 2, 3, 0, 3, 1, 2 };
        int start2 = 0;
        System.out.println("Test 2 - BFS: " + solution.canReachBFS(arr2, start2)); // true

        int[] arr3 = { 3, 0, 2, 1, 2 };
        int start3 = 2;
        System.out.println("Test 3 - BFS: " + solution.canReachBFS(arr3, start3)); // false

        int[] arr4 = { 0 };
        int start4 = 0;
        System.out.println("Test 4 - BFS: " + solution.canReachBFS(arr4, start4)); // true
    }

}

class JumpGameIII {

    public boolean canReach(int[] arr, int start) {
        boolean[] visited = new boolean[arr.length];
        return dfs(arr, start, visited);
    }

    private boolean dfs(int[] arr, int i, boolean[] visited) {
        if (i < 0 || i >= arr.length || visited[i]) {
            return false;
        }
        if (arr[i] == 0) {
            return true;
        }
        visited[i] = true;
        return dfs(arr, i + arr[i], visited) || dfs(arr, i - arr[i], visited);
    }

}
