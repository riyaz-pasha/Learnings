import java.util.*;
/*
 * There is an infrastructure of n cities with some number of roads connecting
 * these cities. Each roads[i] = [ai, bi] indicates that there is a
 * bidirectional road between cities ai and bi.
 * 
 * The network rank of two different cities is defined as the total number of
 * directly connected roads to either city. If a road is directly connected to
 * both cities, it is only counted once.
 * 
 * The maximal network rank of the infrastructure is the maximum network rank of
 * all pairs of different cities.
 * 
 * Given the integer n and the array roads, return the maximal network rank of
 * the entire infrastructure.
 */

class Solution {

    /**
     * Approach 1: Brute Force - Check all pairs
     * Time: O(n^2), Space: O(n^2)
     */
    public int maximalNetworkRank(int n, int[][] roads) {
        // Count degree of each city
        int[] degree = new int[n];

        // Track direct connections between cities
        boolean[][] connected = new boolean[n][n];

        // Process all roads
        for (int[] road : roads) {
            int a = road[0], b = road[1];
            degree[a]++;
            degree[b]++;
            connected[a][b] = true;
            connected[b][a] = true;
        }

        int maxRank = 0;

        // Check all pairs of cities
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Network rank = degree[i] + degree[j] - (direct connection ? 1 : 0)
                int rank = degree[i] + degree[j];
                if (connected[i][j]) {
                    rank--; // Subtract 1 if directly connected (avoid double counting)
                }
                maxRank = Math.max(maxRank, rank);
            }
        }

        return maxRank;
    }

    /**
     * Approach 2: Optimized using HashSet for connections
     * Time: O(n^2), Space: O(roads.length)
     */
    public int maximalNetworkRankV2(int n, int[][] roads) {
        // Count degree of each city
        int[] degree = new int[n];

        // Use set to store connections as strings
        Set<String> roadSet = new HashSet<>();

        for (int[] road : roads) {
            int a = road[0], b = road[1];
            degree[a]++;
            degree[b]++;

            // Store connection in both directions
            roadSet.add(a + "," + b);
            roadSet.add(b + "," + a);
        }

        int maxRank = 0;

        // Check all pairs of cities
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int rank = degree[i] + degree[j];

                // Check if directly connected
                if (roadSet.contains(i + "," + j)) {
                    rank--;
                }

                maxRank = Math.max(maxRank, rank);
            }
        }

        return maxRank;
    }

    /**
     * Approach 3: Most Optimized - Focus on highest degree cities
     * Time: O(n^2) worst case, but often better in practice
     * Space: O(n^2)
     */
    public int maximalNetworkRankV3(int n, int[][] roads) {
        int[] degree = new int[n];
        boolean[][] connected = new boolean[n][n];

        for (int[] road : roads) {
            int a = road[0], b = road[1];
            degree[a]++;
            degree[b]++;
            connected[a][b] = connected[b][a] = true;
        }

        // Find the two highest degrees
        int max1 = 0, max2 = 0;
        for (int i = 0; i < n; i++) {
            if (degree[i] > max1) {
                max2 = max1;
                max1 = degree[i];
            } else if (degree[i] > max2) {
                max2 = degree[i];
            }
        }

        int maxRank = 0;

        // Check all pairs, but prioritize high-degree cities
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Early pruning: if current degrees can't beat maxRank, skip
                if (degree[i] + degree[j] <= maxRank) {
                    continue;
                }

                int rank = degree[i] + degree[j];
                if (connected[i][j]) {
                    rank--;
                }
                maxRank = Math.max(maxRank, rank);
            }
        }

        return maxRank;
    }

    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1: n = 4, roads = [[0,1],[0,3],[1,2],[1,3]]
        // Expected: 4 (cities 0 and 1: degree[0]=2, degree[1]=3, connected=true,
        // rank=2+3-1=4)
        int[][] roads1 = { { 0, 1 }, { 0, 3 }, { 1, 2 }, { 1, 3 } };
        System.out.println("Test 1: " + sol.maximalNetworkRank(4, roads1));

        // Test case 2: n = 5, roads = [[0,1],[0,3],[1,2],[1,3],[2,3],[2,4]]
        // Expected: 5 (cities 1 and 2: degree[1]=3, degree[2]=3, connected=true,
        // rank=3+3-1=5)
        int[][] roads2 = { { 0, 1 }, { 0, 3 }, { 1, 2 }, { 1, 3 }, { 2, 3 }, { 2, 4 } };
        System.out.println("Test 2: " + sol.maximalNetworkRank(5, roads2));

        // Test case 3: n = 8, roads = [[0,1],[1,2],[2,3],[2,4],[5,6],[5,7]]
        // Expected: 5 (cities 2 and 5: degree[2]=3, degree[5]=2, not connected,
        // rank=3+2=5)
        int[][] roads3 = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 2, 4 }, { 5, 6 }, { 5, 7 } };
        System.out.println("Test 3: " + sol.maximalNetworkRank(8, roads3));
    }
}

/**
 * Algorithm Explanation:
 * 
 * Network Rank Formula:
 * rank(city_i, city_j) = degree[i] + degree[j] - (directly_connected ? 1 : 0)
 * 
 * Key Insights:
 * 1. We need to check all pairs of cities to find the maximum
 * 2. For each pair, sum their degrees and subtract 1 if they're directly
 * connected
 * 3. The subtraction avoids double-counting the direct road between them
 * 
 * Time Complexity: O(n^2) - we check all pairs of cities
 * Space Complexity: O(n^2) for the connection matrix, or O(roads.length) with
 * HashSet
 * 
 * Optimizations:
 * - Approach 1: Uses boolean matrix for O(1) connection lookup
 * - Approach 2: Uses HashSet to save space when roads << n^2
 * - Approach 3: Adds early pruning for better practical performance
 */
