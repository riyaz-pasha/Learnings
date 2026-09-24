/*
 * There are n rooms labeled from 0 to n - 1 and all the rooms are locked except for room 0. 
 * Your goal is to visit all the rooms. 
 * However, you cannot enter a locked room without having its key.
 * When you visit a room, you may find a set of distinct keys in it. 
 * Each key has a number on it, denoting which room it unlocks, 
 * and you can take all of them with you to unlock the other rooms.
 * Given an array rooms where rooms[i] is the set of keys that you can obtain if you visited room i,
 * return true if you can visit all the rooms, or false otherwise.
 * 
 * Example 1:
 * Input: rooms = [[1],[2],[3],[]]
 * Output: true
 * Explanation: 
 * We visit room 0 and pick up key 1.
 * We then visit room 1 and pick up key 2.
 * We then visit room 2 and pick up key 3.
 * We then visit room 3.
 * Since we were able to visit every room, we return true.
 * 
 * Example 2:
 * Input: rooms = [[1,3],[3,0,1],[2],[0]]
 * Output: false
 * Explanation: We can not enter room number 2 since the only key that unlocks it is in that room.
 */

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

class KeysAndRooms {

    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        boolean[] visited = new boolean[rooms.size()];
        dfs(rooms, 0, visited);
        for (boolean isVisited : visited) {
            if (!isVisited)
                return false;
        }
        return true;
    }

    private void dfs(List<List<Integer>> rooms, int room, boolean[] visited) {
        if (visited[room])
            return;
        visited[room] = true;
        for (int key : rooms.get(room)) {
            dfs(rooms, key, visited);
        }
    }

}

// Solution 1: DFS (Depth-First Search) - Recursive
// Time Complexity: O(N + K) where N = number of rooms, K = total number of keys
// Space Complexity: O(N) for visited array + O(N) for recursion stack
class Solution {
    /*
     * APPROACH: Recursive DFS
     * 
     * Key Insights:
     * 1. We start in room 0 (unlocked)
     * 2. From each room, we collect all keys and visit corresponding rooms
     * 3. We need to track visited rooms to avoid infinite loops
     * 4. If we can visit all n rooms, return true; otherwise false
     * 
     * Process:
     * 1. Start DFS from room 0
     * 2. Mark current room as visited
     * 3. For each key in current room, if the room it unlocks hasn't been visited,
     * visit it
     * 4. Count total visited rooms and check if equals n
     */
    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        int n = rooms.size();
        boolean[] visited = new boolean[n];

        // Start DFS from room 0
        dfs(rooms, 0, visited);

        // Check if all rooms were visited
        for (boolean wasVisited : visited) {
            if (!wasVisited) {
                return false;
            }
        }

        return true;
    }

    private void dfs(List<List<Integer>> rooms, int room, boolean[] visited) {
        /*
         * DFS to visit all reachable rooms from current room
         * 
         * Parameters:
         * - rooms: the input array of rooms and their keys
         * - room: current room number we're visiting
         * - visited: boolean array to track which rooms we've visited
         */
        visited[room] = true; // Mark current room as visited

        // Get all keys from current room and visit corresponding rooms
        for (int key : rooms.get(room)) {
            if (!visited[key]) { // Only visit if we haven't been there
                dfs(rooms, key, visited);
            }
        }
    }

}

// Solution 2: BFS (Breadth-First Search) - Iterative
// Time Complexity: O(N + K)
// Space Complexity: O(N) for visited array + O(N) for queue
class Solution2 {
    /*
     * APPROACH: Iterative BFS using Queue
     * 
     * Similar logic to DFS but uses iterative approach:
     * 1. Use queue to store rooms to visit
     * 2. Start with room 0 in queue
     * 3. While queue not empty:
     * - Visit current room and mark as visited
     * - Add all keys (unvisited rooms) to queue
     * 4. Count visited rooms
     */
    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        int n = rooms.size();
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        // Start with room 0
        queue.offer(0);
        visited[0] = true;
        int visitedCount = 1;

        while (!queue.isEmpty()) {
            int currentRoom = queue.poll();

            // Collect all keys from current room
            for (int key : rooms.get(currentRoom)) {
                if (!visited[key]) {
                    visited[key] = true;
                    queue.offer(key);
                    visitedCount++;
                }
            }
        }

        return visitedCount == n;
    }

}

// Solution 3: DFS with HashSet (Alternative approach)
// Time Complexity: O(N + K)
// Space Complexity: O(N) for HashSet + O(N) for recursion
class Solution3 {
    /*
     * APPROACH: DFS using HashSet to track visited rooms
     * 
     * Uses HashSet instead of boolean array for visited tracking.
     * Slightly different implementation but same logic.
     */
    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        Set<Integer> visited = new HashSet<>();
        dfs(rooms, 0, visited);
        return visited.size() == rooms.size();
    }

    private void dfs(List<List<Integer>> rooms, int room, Set<Integer> visited) {
        visited.add(room);

        for (int key : rooms.get(room)) {
            if (!visited.contains(key)) {
                dfs(rooms, key, visited);
            }
        }
    }

}

// Solution 4: Stack-based DFS (Iterative DFS)
// Time Complexity: O(N + K)
// Space Complexity: O(N) for HashSet + O(N) for recursion
class Solution4 {
    /*
     * APPROACH: Iterative DFS using Stack
     * 
     * Same as recursive DFS but uses explicit stack instead of recursion.
     * Good for avoiding potential stack overflow with large inputs.
     */
    public boolean canVisitAllRooms(List<List<Integer>> rooms) {
        int n = rooms.size();
        boolean[] visited = new boolean[n];
        Stack<Integer> stack = new Stack<>();

        // Start with room 0
        stack.push(0);

        while (!stack.isEmpty()) {
            int room = stack.pop();

            if (visited[room])
                continue; // Skip if already visited

            visited[room] = true;

            // Add all keys (rooms) to stack
            for (int key : rooms.get(room)) {
                if (!visited[key]) {
                    stack.push(key);
                }
            }
        }

        // Check if all rooms visited
        for (boolean wasVisited : visited) {
            if (!wasVisited)
                return false;
        }

        return true;
    }

}
