import java.util.Arrays;
import java.util.PriorityQueue;

/*
 * Problem Statement: You are given a sorted array ‘arr’ of length ‘n’, which
 * contains positive integer positions of ‘n’ gas stations on the X-axis. You
 * are also given an integer ‘k’. You have to place 'k' new gas stations on the
 * X-axis. You can place them anywhere on the non-negative side of the X-axis,
 * even on non-integer positions. Let 'dist' be the maximum value of the
 * distance between adjacent gas stations after adding k new gas stations.
 * Find the minimum value of ‘dist’.
 * 
 * Note: Answers within 10^-6 of the actual answer will be accepted. For
 * example, if the actual answer is 0.65421678124, it is okay to return
 * 0.654216. Our answer will be accepted if that is the same as the actual
 * answer up to the 6th decimal place.
 * 
 * Examples
 * 
 * Example 1:
 * Input Format: N = 5, arr[] = {1,2,3,4,5}, k = 4
 * Result: 0.5
 * Explanation: One of the possible ways to place 4 gas stations is
 * {1,1.5,2,2.5,3,3.5,4,4.5,5}. Thus the maximum difference between adjacent gas
 * stations is 0.5. Hence, the value of ‘dist’ is 0.5. It can be shown that
 * there is no possible way to add 4 gas stations in such a way that the value
 * of ‘dist’ is lower than this.
 * Example 2:
 * Input Format: N = 10, arr[] = {1,2,3,4,5,6,7,8,9,10}, k = 1
 * Result: 1
 * Explanation: One of the possible ways to place 1 gas station is
 * {1,1.5,2,3,4,5,6,7,8,9,10}. Thus the maximum difference between adjacent gas
 * stations is still 1. Hence, the value of ‘dist’ is 1. It can be shown that
 * there is no possible way to add 1 gas station in such a way that the value of
 * ‘dist’ is lower than this.
 */


class MinimiseMaximumDistanceBetweenGasStations4 {

    /**
     * We store each gap segment info:
     *
     * gapIndex represents the gap between:
     *   arr[gapIndex] and arr[gapIndex + 1]
     *
     * maxSegmentLength = current maximum segment length for this gap
     * after inserting some extra stations in this gap.
     */
    static class Gap implements Comparable<Gap> {

        double maxSegmentLength;
        int gapIndex;

        Gap(double maxSegmentLength, int gapIndex) {
            this.maxSegmentLength = maxSegmentLength;
            this.gapIndex = gapIndex;
        }

        /**
         * MaxHeap based on maxSegmentLength.
         * We always want to pick the gap that currently has the largest segment,
         * because that is the bottleneck.
         */
        @Override
        public int compareTo(Gap other) {
            return Double.compare(other.maxSegmentLength, this.maxSegmentLength);
        }
    }

    /**
     * Heap-based greedy solution:
     *
     * Idea:
     * Always insert the next gas station into the gap that currently has
     * the largest maximum segment length.
     *
     * Example:
     * arr = [1, 10], k = 2
     *
     * gap = 9
     * insert 1 station -> segments = 2 -> max segment = 9/2 = 4.5
     * insert 2 station -> segments = 3 -> max segment = 9/3 = 3.0
     *
     * Time Complexity:
     *   O((n + k) log n)
     *
     * Space Complexity:
     *   O(n)
     */
    public double minimizeMaxDistanceBetter(int[] arr, int k) {

        int n = arr.length;

        // how many extra stations inserted in each gap
        int[] inserted = new int[n - 1];

        PriorityQueue<Gap> pq = new PriorityQueue<>();

        // Initially, each gap has 0 inserted stations,
        // so max segment length = full gap length
        for (int i = 0; i < n - 1; i++) {
            double gapLength = arr[i + 1] - arr[i];
            pq.offer(new Gap(gapLength, i));
        }

        // Insert k new gas stations
        for (int station = 1; station <= k; station++) {

            // Pick the gap with largest current segment
            Gap worstGap = pq.poll();
            int idx = worstGap.gapIndex;

            // Insert one station into this gap
            inserted[idx]++;

            // Original gap length remains same
            double gapLength = arr[idx + 1] - arr[idx];

            // If inserted[idx] = x, gap is split into (x+1) segments
            double newMaxSegment = gapLength / (inserted[idx] + 1);

            // Push updated gap back
            pq.offer(new Gap(newMaxSegment, idx));
        }

        // After k insertions, the answer is the maximum segment length among all gaps
        return pq.peek().maxSegmentLength;
    }
}


class MinimiseMaximumDistanceBetweenGasStations {

    class DistanceBetweenStations implements Comparable<DistanceBetweenStations> {

        double distance;
        int stationStartIndex;

        public DistanceBetweenStations(double distance, int stationStartIndex) {
            this.distance = distance;
            this.stationStartIndex = stationStartIndex;
        }

        @Override
        public int compareTo(DistanceBetweenStations other) {
            return Double.compare(other.distance, this.distance);
        }
    }

    public double minimizeMaxDistanceBetter(int[] arr, int k) {
        int n = arr.length;
        int[] gasStationsToBeAddedInBetween = new int[n - 1];

        PriorityQueue<DistanceBetweenStations> pq = new PriorityQueue<>();

        for (int i = 0; i < n - 1; i++) {
            pq.offer(new DistanceBetweenStations(arr[i + 1] - arr[i], i));
        }

        for (int nextGasStation = 1; nextGasStation <= k; nextGasStation++) {
            DistanceBetweenStations nextGasStationInsertAt = pq.poll();
            int nextGasStationInsertStartIndex = nextGasStationInsertAt.stationStartIndex;

            gasStationsToBeAddedInBetween[nextGasStationInsertStartIndex]++;

            double intialDifferenceBetweenStations = arr[nextGasStationInsertStartIndex + 1]
                    - arr[nextGasStationInsertStartIndex];
            double newDistance = intialDifferenceBetweenStations / (double) (nextGasStationInsertAt.distance + 1);
            pq.offer(new DistanceBetweenStations(newDistance, nextGasStationInsertStartIndex));
        }
        return pq.peek().distance;
    }

}

class MinimiseMaximumDistanceBetweenGasStations2 {

    class DistanceBetweenStations implements Comparable<DistanceBetweenStations> {
        double maxSubSegmentDistance;
        int stationStartIndex;
        int numDivisions;

        public DistanceBetweenStations(double maxSubSegmentDistance, int stationStartIndex, int numDivisions) {
            this.maxSubSegmentDistance = maxSubSegmentDistance;
            this.stationStartIndex = stationStartIndex;
            this.numDivisions = numDivisions;
        }

        @Override
        public int compareTo(DistanceBetweenStations other) {
            return Double.compare(other.maxSubSegmentDistance, this.maxSubSegmentDistance); // max-heap
        }
    }

    public double minimizeMaxDistanceBetter(int[] arr, int k) {
        int n = arr.length;

        PriorityQueue<DistanceBetweenStations> pq = new PriorityQueue<>();

        for (int i = 0; i < n - 1; i++) {
            double initialGap = arr[i + 1] - arr[i];
            pq.offer(new DistanceBetweenStations(initialGap, i, 1)); // 1 segment initially
        }

        for (int j = 0; j < k; j++) {
            DistanceBetweenStations segment = pq.poll();
            segment.numDivisions++; // Add one gas station, so one more segment

            double totalGap = arr[segment.stationStartIndex + 1] - arr[segment.stationStartIndex];
            double newMaxSubSegmentDistance = totalGap / segment.numDivisions;

            pq.offer(new DistanceBetweenStations(newMaxSubSegmentDistance, segment.stationStartIndex,
                    segment.numDivisions));
        }

        return pq.peek().maxSubSegmentDistance;
    }

}

class MinimiseMaximumDistanceBetweenGasStations3 {

    /**
     * Problem:
     * Given positions of gas stations (sorted array arr),
     * add k more stations such that the maximum distance between
     * adjacent stations is minimized.
     *
     * ---------------------------------------------------------
     * Why Binary Search on Answer?
     *
     * We want to MINIMIZE the maximum distance.
     *
     * If a distance D is possible (can place <= k stations),
     * then any bigger distance also works.
     *
     * So feasibility is monotonic:
     *    false false false true true true ...
     *
     * Hence binary search on distance.
     *
     * ---------------------------------------------------------
     * Time Complexity:
     *   O(n log(maxGap / precision))
     *
     * Space Complexity:
     *   O(1)
     */
    public static double minimizeMaxDistanceOptimal(int[] arr, int k) {

        int n = arr.length;

        double low = 0.0;
        double high = 0.0;

        // maximum gap is the upper bound
        for (int i = 0; i < n - 1; i++) {
            high = Math.max(high, arr[i + 1] - arr[i]);
        }

        double precision = 1e-6;

        while (high - low > precision) {

            double mid = (low + high) / 2.0;

            int requiredStations = numberOfGasStationsRequired(arr, mid);

            // If we need more than k stations, mid is too small (too strict)
            if (requiredStations > k) {
                low = mid;
            }
            // Otherwise mid is feasible, try smaller distance
            else {
                high = mid;
            }
        }

        return high;
    }

    /**
     * Returns how many extra stations are needed so that
     * no adjacent distance exceeds "dist".
     *
     * For each gap = arr[i] - arr[i-1]:
     *
     * segments needed = ceil(gap / dist)
     * stations needed = segments - 1
     *
     * Example:
     * gap = 10, dist = 3
     * ceil(10/3) = 4 segments
     * stations = 4 - 1 = 3
     */
    private static int numberOfGasStationsRequired(int[] arr, double dist) {

        int count = 0;

        for (int i = 1; i < arr.length; i++) {

            double gap = arr[i] - arr[i - 1];

            // ceil(gap / dist) - 1
            int stationsNeeded = (int) Math.ceil(gap / dist) - 1;

            count += stationsNeeded;
        }

        return count;
    }
}


class MinimizeMaxDistance {

    /**
     * BRUTE FORCE APPROACH
     * Algorithm:
     * 1. For each of the k gas stations to place:
     * 2. Find the section with maximum distance between consecutive stations
     * 3. Place one gas station in the middle of that section
     * 4. Repeat until all k stations are placed
     * 5. Return the maximum distance after placing all stations
     * 
     * Time Complexity: O(k * n) - for each of k stations, we scan n gaps
     * Space Complexity: O(n-1) - to store gap counts
     */
    public static double minimizeMaxDistanceBruteForce(int[] arr, int k) {
        int n = arr.length;
        int[] howMany = new int[n - 1]; // howMany[i] = number of stations between arr[i] and arr[i+1]

        // Place k gas stations one by one
        for (int gasStations = 1; gasStations <= k; gasStations++) {
            double maxSection = -1;
            int maxInd = -1;

            // Find the maximum section
            for (int i = 0; i < n - 1; i++) {
                double diff = arr[i + 1] - arr[i];
                double sectionLength = diff / (double) (howMany[i] + 1);
                if (sectionLength > maxSection) {
                    maxSection = sectionLength;
                    maxInd = i;
                }
            }

            // Insert the gas station
            howMany[maxInd]++;
        }

        // Find the maximum distance after placing all stations
        double maxAns = -1;
        for (int i = 0; i < n - 1; i++) {
            double diff = arr[i + 1] - arr[i];
            double sectionLength = diff / (double) (howMany[i] + 1);
            maxAns = Math.max(maxAns, sectionLength);
        }

        return maxAns;
    }

    /**
     * BETTER APPROACH - Using Priority Queue (Max Heap)
     * Algorithm:
     * 1. Calculate initial gaps between consecutive stations
     * 2. Use a max heap to always get the section with maximum current distance
     * 3. For each of k stations, place it in the section with max distance
     * 4. Update the heap after each placement
     * 5. Return the maximum distance after all placements
     * 
     * Time Complexity: O(n log n + k log n) - heap operations
     * Space Complexity: O(n) - for heap and arrays
     */
    public static double minimizeMaxDistanceBetter(int[] arr, int k) {
        int n = arr.length;
        int[] howMany = new int[n - 1];

        // Priority queue to store {current_max_distance, index}
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(b[0], a[0]));

        // Insert the first n-1 gaps into the priority queue
        for (int i = 0; i < n - 1; i++) {
            pq.add(new double[] { arr[i + 1] - arr[i], i });
        }

        // Pick and place k gas stations
        for (int gasStations = 1; gasStations <= k; gasStations++) {
            double[] top = pq.poll();
            int secInd = (int) top[1];

            // Insert the current gas station
            howMany[secInd]++;

            double inidiff = arr[secInd + 1] - arr[secInd];
            double newSecLen = inidiff / (double) (howMany[secInd] + 1);
            pq.add(new double[] { newSecLen, secInd });
        }

        return pq.peek()[0];
    }

    /**
     * OPTIMAL APPROACH - Binary Search on Answer
     * Algorithm:
     * 1. Binary search on the possible answer (maximum distance)
     * 2. For each mid value, check if it's possible to place k stations
     * 3. To check: for each gap, calculate how many stations needed to make max
     * distance <= mid
     * 4. If total stations needed <= k, then mid is achievable
     * 5. Use binary search to find the minimum achievable maximum distance
     * 
     * Time Complexity: O(n * log(max_distance * 10^6)) - binary search with
     * precision
     * Space Complexity: O(1) - only using variables
     */
    public static double minimizeMaxDistanceOptimal(int[] arr, int k) {
        int n = arr.length;
        double low = 0;
        double high = 0;

        // Find the maximum gap to set the upper bound
        for (int i = 0; i < n - 1; i++) {
            high = Math.max(high, (double) (arr[i + 1] - arr[i]));
        }

        // Apply binary search
        double diff = 1e-6; // precision requirement
        while (high - low > diff) {
            double mid = (low + high) / 2.0;
            int cnt = numberOfGasStationsRequired(arr, mid);
            if (cnt > k) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return high;
    }

    /**
     * Helper function to count gas stations required for a given maximum distance
     */
    private static int numberOfGasStationsRequired(int[] arr, double dist) {
        int n = arr.length;
        int cnt = 0;
        for (int i = 1; i < n; i++) {
            int numberInBetween = (int) ((arr[i] - arr[i - 1]) / dist);
            if ((arr[i] - arr[i - 1]) == (dist * numberInBetween)) {
                numberInBetween--;
            }
            cnt += numberInBetween;
        }
        return cnt;
    }

    private static int numberOfGasStationsRequired2(int[] arr, double dist) {
        int count = 0;
        for (int i = 1; i < arr.length; i++) {
            double gap = arr[i] - arr[i - 1];
            count += (int) Math.ceil(gap / dist) - 1;
        }
        return count;
    }

    // Demonstration and test cases
    public static void main(String[] args) {
        System.out.println("=== Minimize Maximum Distance Between Gas Stations ===\n");

        // Test case 1
        int[] arr1 = { 1, 2, 3, 4, 5 };
        int k1 = 4;
        System.out.println("Test Case 1:");
        System.out.println("Array: " + Arrays.toString(arr1));
        System.out.println("K = " + k1);
        System.out.println("Expected: 0.5");

        double result1_brute = minimizeMaxDistanceBruteForce(arr1.clone(), k1);
        double result1_better = minimizeMaxDistanceBetter(arr1.clone(), k1);
        double result1_optimal = minimizeMaxDistanceOptimal(arr1.clone(), k1);

        System.out.printf("Brute Force: %.6f\n", result1_brute);
        System.out.printf("Better: %.6f\n", result1_better);
        System.out.printf("Optimal: %.6f\n", result1_optimal);
        System.out.println();

        // Test case 2
        int[] arr2 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        int k2 = 1;
        System.out.println("Test Case 2:");
        System.out.println("Array: " + Arrays.toString(arr2));
        System.out.println("K = " + k2);
        System.out.println("Expected: 1.0");

        double result2_brute = minimizeMaxDistanceBruteForce(arr2.clone(), k2);
        double result2_better = minimizeMaxDistanceBetter(arr2.clone(), k2);
        double result2_optimal = minimizeMaxDistanceOptimal(arr2.clone(), k2);

        System.out.printf("Brute Force: %.6f\n", result2_brute);
        System.out.printf("Better: %.6f\n", result2_better);
        System.out.printf("Optimal: %.6f\n", result2_optimal);
        System.out.println();

        // Test case 3 - Custom
        int[] arr3 = { 1, 7 };
        int k3 = 2;
        System.out.println("Test Case 3:");
        System.out.println("Array: " + Arrays.toString(arr3));
        System.out.println("K = " + k3);
        System.out.println("Expected: 2.0");

        double result3_brute = minimizeMaxDistanceBruteForce(arr3.clone(), k3);
        double result3_better = minimizeMaxDistanceBetter(arr3.clone(), k3);
        double result3_optimal = minimizeMaxDistanceOptimal(arr3.clone(), k3);

        System.out.printf("Brute Force: %.6f\n", result3_brute);
        System.out.printf("Better: %.6f\n", result3_better);
        System.out.printf("Optimal: %.6f\n", result3_optimal);
        System.out.println();

        // Performance comparison
        System.out.println("=== Time Complexity Analysis ===");
        System.out.println("Brute Force: O(k * n) - For each station, scan all gaps");
        System.out.println("Better (Heap): O(n log n + k log n) - Heap operations");
        System.out.println("Optimal (Binary Search): O(n * log(range)) - Most efficient");
        System.out.println();

        // Explanation of optimal approach
        System.out.println("=== Binary Search Logic ===");
        System.out.println("1. Search space: [0, max_gap] where max_gap is largest distance");
        System.out.println("2. For each mid value, check if achievable with k stations");
        System.out.println("3. To check: count stations needed to make all gaps <= mid");
        System.out.println("4. If stations_needed <= k, then mid is achievable");
        System.out.println("5. Binary search finds minimum achievable maximum distance");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * BRUTE FORCE APPROACH:
 * 1. Use an array howMany[] to track stations between each pair of consecutive
 * original stations
 * 2. For each of k stations to place:
 * - Find the section with maximum current distance
 * - Place one station in that section (increment howMany[i])
 * 3. After placing all stations, find the maximum distance
 * 4. Time: O(k*n), Space: O(n)
 * 
 * BETTER APPROACH (Priority Queue):
 * 1. Use a max heap to always get the section with maximum distance
 * 2. Initially add all gaps to the heap
 * 3. For each station to place:
 * - Extract section with max distance
 * - Place station there (update count)
 * - Recalculate new distance and add back to heap
 * 4. Time: O(n log n + k log n), Space: O(n)
 * 
 * OPTIMAL APPROACH (Binary Search):
 * 1. Binary search on the answer (maximum distance)
 * 2. Search range: [0, maximum_initial_gap]
 * 3. For each mid value, check if it's achievable:
 * - For each gap, calculate stations needed: floor(gap_length / mid)
 * - If total stations needed <= k, mid is achievable
 * 4. Find minimum achievable maximum distance
 * 5. Time: O(n * log(range)), Space: O(1)
 * 
 * KEY INSIGHT for Binary Search:
 * - If we can achieve maximum distance 'd', we can also achieve any distance >
 * d
 * - This monotonic property allows binary search
 * - We're searching for the minimum value of maximum distance
 * 
 * The optimal approach is most efficient for large inputs!
 */
