import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
/*
 * A city's skyline is the outer contour of the silhouette formed by all the
 * buildings in that city when viewed from a distance. Given the locations and
 * heights of all the buildings, return the skyline formed by these buildings
 * collectively.
 * 
 * The geometric information of each building is given in the array buildings
 * where buildings[i] = [lefti, righti, heighti]:
 * 
 * lefti is the x coordinate of the left edge of the ith building.
 * righti is the x coordinate of the right edge of the ith building.
 * heighti is the height of the ith building.
 * You may assume all buildings are perfect rectangles grounded on an absolutely
 * flat surface at height 0.
 * 
 * The skyline should be represented as a list of "key points" sorted by their
 * x-coordinate in the form [[x1,y1],[x2,y2],...]. Each key point is the left
 * endpoint of some horizontal segment in the skyline except the last point in
 * the list, which always has a y-coordinate 0 and is used to mark the skyline's
 * termination where the rightmost building ends. Any ground between the
 * leftmost and rightmost buildings should be part of the skyline's contour.
 * 
 * Note: There must be no consecutive horizontal lines of equal height in the
 * output skyline. For instance, [...,[2 3],[4 5],[7 5],[11 5],[12 7],...] is
 * not acceptable; the three lines of height 5 should be merged into one in the
 * final output as such: [...,[2 3],[4 5],[12 7],...]
 * 
 * Example 1:
 * Input: buildings = [[2,9,10],[3,7,15],[5,12,12],[15,20,10],[19,24,8]]
 * Output: [[2,10],[3,15],[7,12],[12,0],[15,10],[20,8],[24,0]]
 * Explanation:
 * Figure A shows the buildings of the input.
 * Figure B shows the skyline formed by those buildings. The red points in
 * figure B represent the key points in the output list.
 * 
 * Example 2:
 * Input: buildings = [[0,2,3],[2,5,3]]
 * Output: [[0,3],[5,0]]
 */

class SkylineSolutions {

    /**
     * Solution 1: Sweep Line Algorithm with TreeMap
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     * 
     * This is the most efficient and commonly used approach.
     * Uses a sweep line algorithm with events and a TreeMap to track active
     * heights.
     */
    public List<List<Integer>> getSkyline1(int[][] buildings) {
        List<List<Integer>> result = new ArrayList<>();

        // Create events: [position, height, type]
        // type: 0 = start, 1 = end
        List<int[]> events = new ArrayList<>();

        for (int[] building : buildings) {
            // Start event: negative height to prioritize higher buildings at same position
            events.add(new int[] { building[0], -building[2], 0 });
            // End event: positive height
            events.add(new int[] { building[1], building[2], 1 });
        }

        // Sort events by position, then by type (start before end), then by height
        events.sort((a, b) -> {
            if (a[0] != b[0])
                return a[0] - b[0];
            if (a[2] != b[2])
                return a[2] - b[2];
            return a[1] - b[1];
        });

        // TreeMap to maintain active heights with their frequencies
        TreeMap<Integer, Integer> heightMap = new TreeMap<>();
        heightMap.put(0, 1); // Ground level

        for (int[] event : events) {
            int pos = event[0];
            int height = Math.abs(event[1]);
            int type = event[2];

            if (type == 0) { // Start event
                heightMap.put(height, heightMap.getOrDefault(height, 0) + 1);
            } else { // End event
                int count = heightMap.get(height);
                if (count == 1) {
                    heightMap.remove(height);
                } else {
                    heightMap.put(height, count - 1);
                }
            }

            // Check if max height changed
            int maxHeight = heightMap.lastKey();
            if (result.isEmpty() || result.get(result.size() - 1).get(1) != maxHeight) {
                result.add(Arrays.asList(pos, maxHeight));
            }
        }

        return result;
    }

    /**
     * Solution 2: Divide and Conquer
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     * 
     * Classic divide and conquer approach similar to merge sort.
     * Recursively solve left and right halves, then merge the skylines.
     */
    public List<List<Integer>> getSkyline2(int[][] buildings) {
        return divideAndConquer(buildings, 0, buildings.length - 1);
    }

    private List<List<Integer>> divideAndConquer(int[][] buildings, int left, int right) {
        if (left == right) {
            // Single building
            List<List<Integer>> result = new ArrayList<>();
            result.add(Arrays.asList(buildings[left][0], buildings[left][2]));
            result.add(Arrays.asList(buildings[left][1], 0));
            return result;
        }

        int mid = left + (right - left) / 2;
        List<List<Integer>> leftSkyline = divideAndConquer(buildings, left, mid);
        List<List<Integer>> rightSkyline = divideAndConquer(buildings, mid + 1, right);

        return mergeSkylines(leftSkyline, rightSkyline);
    }

    private List<List<Integer>> mergeSkylines(List<List<Integer>> left, List<List<Integer>> right) {
        List<List<Integer>> result = new ArrayList<>();
        int i = 0, j = 0;
        int leftHeight = 0, rightHeight = 0;

        while (i < left.size() && j < right.size()) {
            int leftX = left.get(i).get(0);
            int rightX = right.get(j).get(0);
            int x, maxHeight;

            if (leftX < rightX) {
                leftHeight = left.get(i).get(1);
                x = leftX;
                i++;
            } else if (leftX > rightX) {
                rightHeight = right.get(j).get(1);
                x = rightX;
                j++;
            } else {
                leftHeight = left.get(i).get(1);
                rightHeight = right.get(j).get(1);
                x = leftX;
                i++;
                j++;
            }

            maxHeight = Math.max(leftHeight, rightHeight);

            if (result.isEmpty() || result.get(result.size() - 1).get(1) != maxHeight) {
                result.add(Arrays.asList(x, maxHeight));
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
     * Solution 3: Brute Force with Coordinate Compression
     * Time Complexity: O(n²)
     * Space Complexity: O(n)
     * 
     * This approach is less efficient but easier to understand.
     * It processes each unique x-coordinate and finds the maximum height at that
     * point.
     */
    public List<List<Integer>> getSkyline3(int[][] buildings) {
        List<List<Integer>> result = new ArrayList<>();

        // Get all unique x-coordinates
        Set<Integer> xCoords = new TreeSet<>();
        for (int[] building : buildings) {
            xCoords.add(building[0]);
            xCoords.add(building[1]);
        }

        List<Integer> sortedX = new ArrayList<>(xCoords);

        for (int i = 0; i < sortedX.size(); i++) {
            int x = sortedX.get(i);
            int maxHeight = 0;

            // Find maximum height at position x
            for (int[] building : buildings) {
                if (building[0] <= x && x < building[1]) {
                    maxHeight = Math.max(maxHeight, building[2]);
                }
            }

            // Add key point if height changes
            if (result.isEmpty() || result.get(result.size() - 1).get(1) != maxHeight) {
                result.add(Arrays.asList(x, maxHeight));
            }
        }

        return result;
    }

    /**
     * Solution 4: Line Sweep with Priority Queue
     * Time Complexity: O(n log n)
     * Space Complexity: O(n)
     * 
     * Alternative sweep line approach using PriorityQueue instead of TreeMap.
     * Uses a different event representation.
     */
    public List<List<Integer>> getSkyline4(int[][] buildings) {
        List<List<Integer>> result = new ArrayList<>();

        // Create events: [position, height, isStart]
        List<int[]> events = new ArrayList<>();
        for (int[] building : buildings) {
            events.add(new int[] { building[0], building[2], 1 }); // start
            events.add(new int[] { building[1], building[2], 0 }); // end
        }

        // Sort events
        events.sort((a, b) -> {
            if (a[0] != b[0])
                return a[0] - b[0];
            if (a[2] != b[2])
                return b[2] - a[2]; // start events before end events
            if (a[2] == 1)
                return b[1] - a[1]; // for start events, higher buildings first
            else
                return a[1] - b[1]; // for end events, lower buildings first
        });

        // Max heap to store active building heights
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.offer(0); // ground level

        for (int[] event : events) {
            int pos = event[0];
            int height = event[1];
            int isStart = event[2];

            if (isStart == 1) {
                maxHeap.offer(height);
            } else {
                maxHeap.remove(height); // O(n) operation - this makes it less efficient
            }

            int maxHeight = maxHeap.peek();
            if (result.isEmpty() || result.get(result.size() - 1).get(1) != maxHeight) {
                result.add(Arrays.asList(pos, maxHeight));
            }
        }

        return result;
    }

    // Test method
    public static void main(String[] args) {
        SkylineSolutions solution = new SkylineSolutions();

        // Test case 1
        int[][] buildings1 = { { 2, 9, 10 }, { 3, 7, 15 }, { 5, 12, 12 }, { 15, 20, 10 }, { 19, 24, 8 } };
        System.out.println("Test 1 - Solution 1: " + solution.getSkyline1(buildings1));
        System.out.println("Test 1 - Solution 2: " + solution.getSkyline2(buildings1));
        System.out.println("Test 1 - Solution 3: " + solution.getSkyline3(buildings1));
        System.out.println("Test 1 - Solution 4: " + solution.getSkyline4(buildings1));

        // Test case 2
        int[][] buildings2 = { { 0, 2, 3 }, { 2, 5, 3 } };
        System.out.println("\nTest 2 - Solution 1: " + solution.getSkyline1(buildings2));
        System.out.println("Test 2 - Solution 2: " + solution.getSkyline2(buildings2));
        System.out.println("Test 2 - Solution 3: " + solution.getSkyline3(buildings2));
        System.out.println("Test 2 - Solution 4: " + solution.getSkyline4(buildings2));
    }

}

class TheSkylineProblem {

    public List<List<Integer>> getSkyline(int[][] buildings) {
        List<Event> events = new ArrayList<>();

        // Create start and end events
        for (int[] building : buildings) {
            events.add(new Event(building[0], building[2], true));
            events.add(new Event(building[1], building[2], false));
        }

        // Sort the events
        Collections.sort(events);

        // Max-heap to keep track of active building heights
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.offer(0); // ground level

        List<List<Integer>> result = new ArrayList<>();
        int prevMaxHeight = 0;

        for (Event event : events) {
            if (event.isStart) {
                maxHeap.offer(event.height);
            } else {
                maxHeap.remove(event.height);
            }

            int currentMaxHeight = maxHeap.peek();
            if (currentMaxHeight != prevMaxHeight) {
                result.add(Arrays.asList(event.x, currentMaxHeight));
                prevMaxHeight = currentMaxHeight;
            }
        }

        return result;
    }

    // Helper class for events
    static class Event implements Comparable<Event> {
        int x;
        int height;
        boolean isStart;

        Event(int x, int height, boolean isStart) {
            this.x = x;
            this.height = height;
            this.isStart = isStart;
        }

        @Override
        public int compareTo(Event other) {
            if (this.x != other.x) {
                return Integer.compare(this.x, other.x);
            }

            // Sort start events before end events
            if (this.isStart && other.isStart) {
                return Integer.compare(other.height, this.height); // higher height first
            }
            if (!this.isStart && !other.isStart) {
                return Integer.compare(this.height, other.height); // lower height first
            }

            return this.isStart ? -1 : 1; // start comes before end
        }
    }

    // Sample usage
    public static void main(String[] args) {
        TheSkylineProblem solution = new TheSkylineProblem();
        int[][] buildings = {
                { 2, 9, 10 },
                { 3, 7, 15 },
                { 5, 12, 12 },
                { 15, 20, 10 },
                { 19, 24, 8 }
        };

        List<List<Integer>> skyline = solution.getSkyline(buildings);
        for (List<Integer> point : skyline) {
            System.out.println(point);
        }
    }

}

/*
 * COMPLEXITY ANALYSIS:
 * 
 * Solution 1 (Sweep Line with TreeMap):
 * - Time: O(n log n) - sorting events + TreeMap operations
 * - Space: O(n) - storing events and height map
 * - Best overall solution - most efficient and handles all edge cases well
 * 
 * Solution 2 (Divide and Conquer):
 * - Time: O(n log n) - T(n) = 2T(n/2) + O(n) for merging
 * - Space: O(n) - recursion stack + temporary skylines
 * - Classic approach, good for understanding divide and conquer
 * 
 * Solution 3 (Brute Force with Coordinate Compression):
 * - Time: O(n²) - for each x-coordinate, check all buildings
 * - Space: O(n) - storing unique coordinates
 * - Simplest to understand but least efficient
 * 
 * Solution 4 (Line Sweep with Priority Queue):
 * - Time: O(n²) in worst case due to PriorityQueue.remove() being O(n)
 * - Space: O(n) - storing events and priority queue
 * - Less efficient than TreeMap approach due to removal operation
 * 
 * RECOMMENDED SOLUTION:
 * Solution 1 (Sweep Line with TreeMap) is the most efficient and widely used
 * approach.
 * It handles all edge cases correctly and has optimal time complexity.
 */
