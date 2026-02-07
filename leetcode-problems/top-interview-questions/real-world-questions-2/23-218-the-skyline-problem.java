import java.util.*;

class Solution {

    record Event(int x, int height, boolean isStart) {}

    /**
     * Time Complexity: O(N log N)
     *   - 2N events sorted: O(N log N)
     *   - Each event insert/remove TreeMap: O(log N)
     *
     * Space Complexity: O(N)
     *   - Events + TreeMap heights
     */
    public List<List<Integer>> getSkyline(int[][] buildings) {

        List<Event> events = new ArrayList<>();

        // Convert buildings into events
        // Start event: (L, H, true)
        // End event:   (R, H, false)
        for (int[] b : buildings) {
            events.add(new Event(b[0], b[2], true));
            events.add(new Event(b[1], b[2], false));
        }

        // Sort events:
        // 1) x ascending
        // 2) start before end
        // 3) if both start -> higher height first
        // 4) if both end   -> lower height first
        Collections.sort(events, (a, b) -> {

            if (a.x != b.x) {
                return Integer.compare(a.x, b.x);
            }

            // start should come before end
            if (a.isStart != b.isStart) {
                return Boolean.compare(b.isStart, a.isStart);
            }

            // both are start -> higher height first
            if (a.isStart) {
                return Integer.compare(b.height, a.height);
            }

            // both are end -> lower height first
            return Integer.compare(a.height, b.height);
        });

        // activeHeights stores counts of currently active building heights
        // reverse order so firstKey() gives maximum height
        TreeMap<Integer, Integer> activeHeights = new TreeMap<>(Collections.reverseOrder());
        activeHeights.put(0, 1); // ground level always present

        int prevMaxHeight = 0;
        List<List<Integer>> result = new ArrayList<>();

        // Process sweep line
        for (Event event : events) {
            int height = event.height();

            if (event.isStart()) {
                activeHeights.put(height, activeHeights.getOrDefault(height, 0) + 1);
            } else {
                int count = activeHeights.get(height);
                if (count == 1) activeHeights.remove(height);
                else activeHeights.put(height, count - 1);
            }

            int currMaxHeight = activeHeights.firstKey();

            // Skyline changes only when max height changes
            if (currMaxHeight != prevMaxHeight) {
                result.add(List.of(event.x(), currMaxHeight));
                prevMaxHeight = currMaxHeight;
            }
        }

        return result;
    }
}


/**
 * THE SKYLINE PROBLEM - COMPREHENSIVE SOLUTION GUIDE
 * 
 * PROBLEM UNDERSTANDING:
 * =====================
 * Given buildings as [left, right, height], find the outer contour of the skyline.
 * 
 * Example: buildings = [[2,9,10],[3,7,15],[5,12,12],[15,20,10],[19,24,8]]
 * 
 * Visual representation:
 *        15|    ___
 *          |   |   |
 *        12|   |   |___
 *          |   |   |   |
 *        10|___|___|   |___
 *          |   |   |   |   |
 *         8|   |   |   |   |___
 *          |___|___|___|___|___|
 *          2   5   7   12  19  24
 * 
 * Output: [[2,10],[3,15],[7,12],[12,0],[15,10],[20,8],[24,0]]
 * 
 * KEY INSIGHTS FOR INTERVIEWS:
 * ============================
 * 1. This is a SWEEP LINE algorithm problem
 * 2. Critical points are building START and END positions
 * 3. At any x-coordinate, the skyline height = MAX of all active buildings
 * 4. Height changes only at critical points (building edges)
 * 5. Need to track active buildings efficiently
 * 
 * HOW TO APPROACH IN AN INTERVIEW:
 * ================================
 * Step 1: Start with brute force to show understanding
 * Step 2: Identify inefficiencies (hint: we don't need every x-coordinate)
 * Step 3: Realize we only care about building edges (events)
 * Step 4: At each event, track max height of active buildings
 * Step 5: Choose appropriate data structure (priority queue/multiset)
 */

class SkylineProblem {
    
    /**
     * APPROACH 1: SWEEP LINE WITH PRIORITY QUEUE (OPTIMAL)
     * =====================================================
     * Time Complexity: O(n log n) where n = number of buildings
     * Space Complexity: O(n)
     * 
     * INTUITION:
     * - Treat each building edge as an "event" in timeline
     * - At each event, determine current max height
     * - When max height changes, add a key point
     * 
     * STEP-BY-STEP REASONING:
     * 1. Create events for building starts and ends
     * 2. Sort events by x-coordinate
     * 3. Use a data structure to track active building heights
     * 4. For each event:
     *    - Add/remove heights from active set
     *    - Check if max height changed
     *    - If changed, record key point
     */
    public List<List<Integer>> getSkyline(int[][] buildings) {
        List<List<Integer>> result = new ArrayList<>();
        
        // STEP 1: Create events
        // Why? We only care about positions where buildings start or end
        List<int[]> events = new ArrayList<>();
        
        for (int[] building : buildings) {
            int left = building[0];
            int right = building[1];
            int height = building[2];
            
            // Critical: Use negative height for start events
            // Why? To differentiate start from end when x-coordinates are same
            // Also helps with sorting: starts before ends at same position
            events.add(new int[]{left, -height}); // Start event (negative height)
            events.add(new int[]{right, height});  // End event (positive height)
        }
        
        // STEP 2: Sort events
        // Primary: by x-coordinate
        // Secondary: by height (with special rules)
        events.sort((a, b) -> {
            // If x-coordinates differ, sort by x
            if (a[0] != b[0]) {
                return a[0] - b[0];
            }
            
            // If x-coordinates are same, sort by height
            // Why this matters:
            // - If both are starts (negative): taller first (to avoid intermediate point)
            // - If both are ends (positive): shorter first (to avoid missing point)
            // - If one start, one end: start first (to avoid dip to 0)
            return a[1] - b[1];
        });
        
        // STEP 3: Track active building heights
        // Why TreeMap? Need to:
        // - Get max height efficiently: O(log n)
        // - Add/remove heights: O(log n)
        // - Handle duplicate heights (multiple buildings same height)
        TreeMap<Integer, Integer> heightMap = new TreeMap<>();
        
        // Initially, ground level (height 0) is always present
        // This handles the case when all buildings end
        heightMap.put(0, 1);
        
        int prevMaxHeight = 0; // Track previous max to detect changes
        
        // STEP 4: Process each event
        for (int[] event : events) {
            int x = event[0];
            int h = event[1];
            
            if (h < 0) {
                // START event: building begins
                // Add this height to active set
                h = -h; // Convert back to positive
                heightMap.put(h, heightMap.getOrDefault(h, 0) + 1);
            } else {
                // END event: building ends
                // Remove this height from active set
                int count = heightMap.get(h);
                if (count == 1) {
                    heightMap.remove(h); // Last building of this height
                } else {
                    heightMap.put(h, count - 1); // Still have buildings of this height
                }
            }
            
            // Get current max height
            // lastKey() gives the maximum key in TreeMap (our tallest active building)
            int currentMaxHeight = heightMap.lastKey();
            
            // STEP 5: Detect height change
            // Only add key point when height actually changes
            if (currentMaxHeight != prevMaxHeight) {
                // This is a critical point in our skyline
                result.add(Arrays.asList(x, currentMaxHeight));
                prevMaxHeight = currentMaxHeight;
            }
            
            // Why this works:
            // - At any point, skyline height = max of all active buildings
            // - We only record when this max changes
            // - TreeMap keeps heights sorted, so lastKey() is O(log n)
        }
        
        return result;
    }
    
    /**
     * APPROACH 2: DIVIDE AND CONQUER (ALTERNATIVE - GOOD FOR DISCUSSION)
     * ===================================================================
     * Time Complexity: O(n log n)
     * Space Complexity: O(n) for recursion stack
     * 
     * INTUITION:
     * - Similar to merge sort
     * - Divide buildings into two halves
     * - Recursively solve each half
     * - Merge the two skylines
     * 
     * This approach is interesting to mention in interviews to show you
     * understand multiple algorithmic paradigms, but sweep line is cleaner.
     */
    public List<List<Integer>> getSkylineDivideConquer(int[][] buildings) {
        if (buildings == null || buildings.length == 0) {
            return new ArrayList<>();
        }
        return divideAndConquer(buildings, 0, buildings.length - 1);
    }
    
    private List<List<Integer>> divideAndConquer(int[][] buildings, int start, int end) {
        // Base case: single building
        if (start == end) {
            List<List<Integer>> result = new ArrayList<>();
            result.add(Arrays.asList(buildings[start][0], buildings[start][2])); // Start point
            result.add(Arrays.asList(buildings[start][1], 0));                    // End point
            return result;
        }
        
        // Divide
        int mid = start + (end - start) / 2;
        List<List<Integer>> left = divideAndConquer(buildings, start, mid);
        List<List<Integer>> right = divideAndConquer(buildings, mid + 1, end);
        
        // Conquer: merge two skylines
        return mergeSkylines(left, right);
    }
    
    /**
     * Merge two skylines
     * This is the tricky part - need to handle overlaps correctly
     */
    private List<List<Integer>> mergeSkylines(List<List<Integer>> left, List<List<Integer>> right) {
        List<List<Integer>> result = new ArrayList<>();
        int i = 0, j = 0;
        int h1 = 0, h2 = 0; // Current heights from left and right skylines
        
        while (i < left.size() && j < right.size()) {
            int x, maxH;
            
            // Choose the point with smaller x-coordinate
            if (left.get(i).get(0) < right.get(j).get(0)) {
                x = left.get(i).get(0);
                h1 = left.get(i).get(1);
                i++;
            } else if (left.get(i).get(0) > right.get(j).get(0)) {
                x = right.get(j).get(0);
                h2 = right.get(j).get(1);
                j++;
            } else {
                // Same x-coordinate, advance both
                x = left.get(i).get(0);
                h1 = left.get(i).get(1);
                h2 = right.get(j).get(1);
                i++;
                j++;
            }
            
            // Max height at this point
            maxH = Math.max(h1, h2);
            
            // Add point only if height changed
            if (result.isEmpty() || maxH != result.get(result.size() - 1).get(1)) {
                result.add(Arrays.asList(x, maxH));
            }
        }
        
        // Add remaining points
        while (i < left.size()) {
            result.add(left.get(i++));
        }
        while (j < right.size()) {
            result.add(right.get(j++));
        }
        
        return result;
    }
    
    /**
     * APPROACH 3: BRUTE FORCE (FOR UNDERSTANDING ONLY)
     * =================================================
     * Time Complexity: O(n * m) where n = buildings, m = max x-coordinate
     * Space Complexity: O(m)
     * 
     * NOT RECOMMENDED for interviews, but helps understand the problem
     */
    public List<List<Integer>> getSkylineBruteForce(int[][] buildings) {
        if (buildings == null || buildings.length == 0) {
            return new ArrayList<>();
        }
        
        // Find range
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (int[] b : buildings) {
            minX = Math.min(minX, b[0]);
            maxX = Math.max(maxX, b[1]);
        }
        
        // For each x-coordinate, find max height
        int[] heights = new int[maxX - minX + 1];
        for (int[] b : buildings) {
            for (int x = b[0]; x < b[1]; x++) {
                heights[x - minX] = Math.max(heights[x - minX], b[2]);
            }
        }
        
        // Extract key points
        List<List<Integer>> result = new ArrayList<>();
        int prevHeight = 0;
        for (int i = 0; i < heights.length; i++) {
            if (heights[i] != prevHeight) {
                result.add(Arrays.asList(i + minX, heights[i]));
                prevHeight = heights[i];
            }
        }
        result.add(Arrays.asList(maxX, 0));
        
        return result;
    }
    
    /**
     * INTERVIEW STRATEGY GUIDE
     * ========================
     * 
     * 1. CLARIFY THE PROBLEM (2-3 minutes)
     *    - Confirm input format: [left, right, height]
     *    - Confirm output format: [[x, y], ...]
     *    - Ask about edge cases:
     *      * Empty input?
     *      * Single building?
     *      * Overlapping buildings?
     *      * Buildings with same height?
     *      * All buildings same position?
     * 
     * 2. DISCUSS APPROACH (5 minutes)
     *    - Start with brute force: "We could check every x-coordinate..."
     *    - Identify inefficiency: "But we don't need every point, only where height changes"
     *    - Propose sweep line: "Height changes at building edges"
     *    - Explain data structure choice: "Need to track max of active buildings"
     * 
     * 3. WALK THROUGH EXAMPLE (3-5 minutes)
     *    - Use given example or create simple one
     *    - Show events list
     *    - Show how heightMap changes
     *    - Show when key points are added
     * 
     * 4. IMPLEMENT (15-20 minutes)
     *    - Start with main logic
     *    - Handle edge cases as you code
     *    - Explain your reasoning out loud
     * 
     * 5. TEST (5 minutes)
     *    - Test with given examples
     *    - Test edge cases:
     *      * [[0, 2, 3], [2, 5, 3]] - adjacent same height
     *      * [[0, 3, 3], [1, 4, 4]] - overlapping different heights
     *      * [[0, 2, 3], [0, 2, 3]] - duplicate buildings
     * 
     * 6. ANALYZE COMPLEXITY (2 minutes)
     *    - Time: O(n log n) for sorting + O(n log n) for TreeMap operations
     *    - Space: O(n) for events and heightMap
     * 
     * COMMON MISTAKES TO AVOID:
     * ========================
     * 1. Not handling buildings at same x-coordinate correctly
     * 2. Forgetting to initialize heightMap with 0
     * 3. Not using negative heights for start events (causes sorting issues)
     * 4. Adding duplicate key points (forgetting to check prevMaxHeight)
     * 5. Not handling duplicate heights (using Set instead of Map with counts)
     * 
     * FOLLOW-UP QUESTIONS TO PREPARE FOR:
     * ===================================
     * Q: What if buildings are given in streaming fashion?
     * A: Use the same approach, process events as they arrive
     * 
     * Q: What if we need to handle 3D buildings (with y-coordinate too)?
     * A: Extension of 2D approach with more complex merging
     * 
     * Q: Can we optimize space?
     * A: Not significantly - we need to store events and active heights
     * 
     * Q: What if heights can be very large (> Integer.MAX_VALUE)?
     * A: Use long instead of int
     */
    
    // TEST CASES
    public static void main(String[] args) {
        SkylineProblem solution = new SkylineProblem();
        
        System.out.println("=== SKYLINE PROBLEM TEST CASES ===\n");
        
        // Test Case 1: Example from problem
        System.out.println("Test 1: Standard example");
        int[][] buildings1 = {{2,9,10},{3,7,15},{5,12,12},{15,20,10},{19,24,8}};
        System.out.println("Input: " + Arrays.deepToString(buildings1));
        System.out.println("Output: " + solution.getSkyline(buildings1));
        System.out.println("Expected: [[2,10],[3,15],[7,12],[12,0],[15,10],[20,8],[24,0]]");
        System.out.println();
        
        // Test Case 2: Single building
        System.out.println("Test 2: Single building");
        int[][] buildings2 = {{0,2,3}};
        System.out.println("Input: " + Arrays.deepToString(buildings2));
        System.out.println("Output: " + solution.getSkyline(buildings2));
        System.out.println("Expected: [[0,3],[2,0]]");
        System.out.println();
        
        // Test Case 3: Adjacent buildings same height
        System.out.println("Test 3: Adjacent buildings with same height");
        int[][] buildings3 = {{0,2,3},{2,5,3}};
        System.out.println("Input: " + Arrays.deepToString(buildings3));
        System.out.println("Output: " + solution.getSkyline(buildings3));
        System.out.println("Expected: [[0,3],[5,0]] (no dip at x=2)");
        System.out.println();
        
        // Test Case 4: Overlapping buildings
        System.out.println("Test 4: Overlapping buildings");
        int[][] buildings4 = {{0,3,3},{1,4,4},{2,5,2}};
        System.out.println("Input: " + Arrays.deepToString(buildings4));
        System.out.println("Output: " + solution.getSkyline(buildings4));
        System.out.println("Expected: [[0,3],[1,4],[4,2],[5,0]]");
        System.out.println();
        
        // Test Case 5: Building completely inside another
        System.out.println("Test 5: Smaller building inside larger");
        int[][] buildings5 = {{0,5,3},{1,3,4}};
        System.out.println("Input: " + Arrays.deepToString(buildings5));
        System.out.println("Output: " + solution.getSkyline(buildings5));
        System.out.println("Expected: [[0,3],[1,4],[3,3],[5,0]]");
        System.out.println();
        
        // Test Case 6: Same start position, different heights
        System.out.println("Test 6: Same start position");
        int[][] buildings6 = {{0,2,3},{0,3,2}};
        System.out.println("Input: " + Arrays.deepToString(buildings6));
        System.out.println("Output: " + solution.getSkyline(buildings6));
        System.out.println("Expected: [[0,3],[2,2],[3,0]]");
        System.out.println();
        
        // Performance test hint
        System.out.println("=== COMPLEXITY ANALYSIS ===");
        System.out.println("Time Complexity: O(n log n)");
        System.out.println("  - Sorting events: O(n log n)");
        System.out.println("  - Processing events with TreeMap: O(n log n)");
        System.out.println("Space Complexity: O(n)");
        System.out.println("  - Events list: O(n)");
        System.out.println("  - TreeMap: O(n) in worst case");
    }
}

/**
 * ADDITIONAL INSIGHTS FOR MASTERY
 * ================================
 * 
 * 1. WHY NEGATIVE HEIGHTS FOR START EVENTS?
 *    When two buildings start at same x with heights h1 and h2:
 *    - We want to process taller building first
 *    - Using negative: -h1 vs -h2, if h1 > h2, then -h1 < -h2
 *    - Sorting ascending puts taller building first!
 *    - Avoids creating intermediate point at lower height
 * 
 * 2. WHY TREEMAP INSTEAD OF PRIORITYQUEUE?
 *    - PriorityQueue doesn't support efficient removal of arbitrary elements
 *    - TreeMap allows O(log n) insertion, deletion, and max query
 *    - We need to track COUNT of each height (duplicate heights possible)
 * 
 * 3. EDGE CASE: WHAT IF BUILDINGS OVERLAP EXACTLY?
 *    Example: [[0,2,3],[0,2,3]]
 *    - Both create same start and end events
 *    - HeightMap counts: 0->1, then 0->2, then back to 0->1, then 0->0
 *    - Works correctly! Output: [[0,3],[2,0]]
 * 
 * 4. WHY CHECK prevMaxHeight != currentMaxHeight?
 *    - Prevents duplicate key points
 *    - Example: Building ends but another of same height continues
 *    - Max height doesn't change, so no new key point needed
 * 
 * 5. RELATIONSHIP TO OTHER PROBLEMS:
 *    - Merge Intervals: Similar event processing
 *    - Meeting Rooms II: Similar heap-based approach
 *    - Rectangle Overlap: Similar coordinate geometry
 * 
 * 6. VARIATIONS YOU MIGHT SEE:
 *    - Return total visible area of skyline
 *    - Find gaps in skyline
 *    - 3D version with depth
 *    - Circular buildings
 * 
 * Remember: This is a HARD problem on LeetCode for good reason.
 * If you can explain the sweep line approach clearly and implement it
 * correctly, you're demonstrating strong problem-solving skills!
 */
