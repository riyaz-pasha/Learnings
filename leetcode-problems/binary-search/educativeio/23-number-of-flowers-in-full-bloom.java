import java.util.*;

/**
 * ================================================================
 * 🌸 Number of Flowers in Full Bloom — ALL APPROACHES (INTERVIEW KIT)
 * ================================================================
 *
 * PROBLEM:
 * Given intervals [start, end] and query times,
 * return how many intervals are active at each query.
 *
 * ================================================================
 * 🔥 APPROACHES INCLUDED:
 * 1. Brute Force
 * 2. Binary Search (BEST FOR INTERVIEWS ⭐)
 * 3. Sweep Line (Events)
 * 4. Min Heap (Active intervals tracking)
 * ================================================================
 */
class FullBloomFlowersAllApproaches {

    /* ============================================================
     * 1️⃣ BRUTE FORCE (Baseline)
     * ============================================================
     * Check every flower for every person.
     */
    public static int[] bruteForce(int[][] flowers, int[] people) {

        int[] result = new int[people.length];

        for (int i = 0; i < people.length; i++) {
            int t = people[i];
            int count = 0;

            for (int[] f : flowers) {
                if (f[0] <= t && t <= f[1]) {
                    count++;
                }
            }

            result[i] = count;
        }

        return result;
    }
    // Time: O(N * M)
    // Space: O(1)



    /* ============================================================
     * 2️⃣ BINARY SEARCH (⭐ BEST INTERVIEW SOLUTION)
     * ============================================================
     *
     * IDEA:
     * active = (#starts <= t) - (#ends < t)
     *
     * WHY?
     * Instead of checking intervals, convert to counts.
     */
    public static int[] binarySearch(int[][] flowers, int[] people) {

        int n = flowers.length;

        int[] starts = new int[n];
        int[] ends = new int[n];

        for (int i = 0; i < n; i++) {
            starts[i] = flowers[i][0];
            ends[i] = flowers[i][1];
        }

        Arrays.sort(starts);
        Arrays.sort(ends);

        int[] result = new int[people.length];

        for (int i = 0; i < people.length; i++) {

            int t = people[i];

            int started = countLessThanEqual(starts, t); // starts <= t
            int ended = countLessThan(ends, t);          // ends < t

            result[i] = started - ended;
        }

        return result;
    }

    /**
     * 🔍 LAST TRUE: arr[mid] <= target
     */
    private static int countLessThanEqual(int[] arr, int target) {

        int low = 0, high = arr.length - 1;
        int answerIndex = -1;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (arr[mid] <= target) {
                answerIndex = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return answerIndex + 1;
    }

    /**
     * 🔍 LAST TRUE: arr[mid] < target
     */
    private static int countLessThan(int[] arr, int target) {

        int low = 0, high = arr.length - 1;
        int answerIndex = -1;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (arr[mid] < target) {
                answerIndex = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return answerIndex + 1;
    }

    // Time: O((N + M) log N)
    // Space: O(N)



    /* ============================================================
     * 3️⃣ SWEEP LINE (EVENT BASED)
     * ============================================================
     *
     * IDEA:
     * Convert intervals into events:
     *   (start, +1)
     *   (end+1, -1)
     *
     * Process timeline in sorted order.
     */
    public static int[] sweepLine(int[][] flowers, int[] people) {

        List<int[]> events = new ArrayList<>();

        for (int[] f : flowers) {
            events.add(new int[]{f[0], 1});
            events.add(new int[]{f[1] + 1, -1});
        }

        events.sort(Comparator.comparingInt(a -> a[0]));

        int n = people.length;

        int[][] queries = new int[n][2];

        for (int i = 0; i < n; i++) {
            queries[i] = new int[]{people[i], i};
        }

        Arrays.sort(queries, Comparator.comparingInt(a -> a[0]));

        int[] result = new int[n];

        int active = 0;
        int j = 0;

        for (int[] q : queries) {

            int time = q[0];
            int idx = q[1];

            while (j < events.size() && events.get(j)[0] <= time) {
                active += events.get(j)[1];
                j++;
            }

            result[idx] = active;
        }

        return result;
    }

    // Time: O((N + M) log N)
    // Space: O(N)



    /* ============================================================
     * 4️⃣ MIN HEAP (ACTIVE INTERVAL TRACKING)
     * ============================================================
     *
     * IDEA:
     * Sort flowers by start time
     * Sort people (queries)
     *
     * For each time t:
     *   1. Add all flowers with start <= t → push end into heap
     *   2. Remove all flowers with end < t → pop from heap
     *   3. Heap size = active flowers
     *
     * WHY HEAP?
     * → Always remove the earliest ending flower first
     */
    public static int[] heapSolution(int[][] flowers, int[] people) {

        // Sort flowers by start time
        Arrays.sort(flowers, Comparator.comparingInt(a -> a[0]));

        int n = people.length;

        // Store (time, original index)
        int[][] queries = new int[n][2];
        for (int i = 0; i < n; i++) {
            queries[i] = new int[]{people[i], i};
        }

        Arrays.sort(queries, Comparator.comparingInt(a -> a[0]));

        int[] result = new int[n];

        // Min Heap → stores end times of active flowers
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        int i = 0; // pointer for flowers

        for (int[] q : queries) {

            int time = q[0];
            int idx = q[1];

            // ✅ Step 1: Add all flowers starting before or at time
            while (i < flowers.length && flowers[i][0] <= time) {
                minHeap.offer(flowers[i][1]); // store end
                i++;
            }

            // ✅ Step 2: Remove flowers already ended
            while (!minHeap.isEmpty() && minHeap.peek() < time) {
                minHeap.poll();
            }

            // ✅ Step 3: Remaining heap size = active flowers
            result[idx] = minHeap.size();
        }

        return result;
    }

    // Time: O((N + M) log N)
    // Space: O(N)



    /* ============================================================
     * 🧪 MAIN METHOD (FOR QUICK TESTING)
     * ============================================================
     */
    public static void main(String[] args) {

        int[][] flowers = {
                {1, 6},
                {3, 7},
                {9, 12},
                {4, 13}
        };

        int[] people = {2, 3, 7, 11};

        System.out.println("Brute Force  : " + Arrays.toString(bruteForce(flowers, people)));
        System.out.println("Binary Search: " + Arrays.toString(binarySearch(flowers, people)));
        System.out.println("Sweep Line   : " + Arrays.toString(sweepLine(flowers, people)));
        System.out.println("Heap         : " + Arrays.toString(heapSolution(flowers, people)));
    }
}

/**
 * Problem Statement:
 * Given a 2D array `flowers` where flowers[i] = [start, end] and an array `people` 
 * where people[i] is the arrival time of a person.
 * Return an array of the number of blooming flowers for each person at their arrival time.
 * 
 * Constraints:
 * - 1 <= flowers.length, people.length <= 10^3
 * - 1 <= start <= end <= 10^4
 * - 1 <= people[i] <= 10^4
 */
class NumberOfFlowersInFullBloom {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(F log F + P log F) where F is flowers length, P is people length.
     * Space Complexity: O(F) to store separate start and end arrays.
     * 
     * VISUAL EXPLANATION & LOGIC:
     * If a person arrives at time `t`, the number of blooming flowers is:
     * (Total flowers that STARTED blooming <= t) - (Total flowers that ENDED blooming < t).
     * 
     * Flowers: [1, 6], [3, 7], [9, 12], [4, 13]
     * Starts: [1, 3, 4, 9]
     * Ends:   [6, 7, 12, 13]
     * 
     * Person arrives at t = 5:
     * How many started <= 5? [1, 3, 4] -> Count is 3.
     * How many ended < 5? None -> Count is 0.
     * Blooming = 3 - 0 = 3.
     * 
     * Person arrives at t = 8:
     * How many started <= 8? [1, 3, 4] -> Count is 3.
     * How many ended < 8? [6, 7] -> Count is 2.
     * Blooming = 3 - 2 = 1.
     */
    public static int[] fullBloomFlowersIterativeBS(int[][] flowers, int[] people) {
        int n = flowers.length;
        int[] starts = new int[n];
        int[] ends = new int[n];
        
        for (int i = 0; i < n; i++) {
            starts[i] = flowers[i][0];
            ends[i] = flowers[i][1];
        }
        
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        int[] ans = new int[people.length];
        for (int i = 0; i < people.length; i++) {
            int t = people[i];
            int started = countLessOrEqualIterative(starts, t);
            int ended = countStrictlyLessIterative(ends, t);
            ans[i] = started - ended;
        }
        
        return ans;
    }

    private static int countLessOrEqualIterative(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = 0; // Explicit result variable initialized to 0 (default if none match)

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                result = mid + 1; // Record count of elements up to mid
                low = mid + 1;    // Search for more valid elements to the right
            } else {
                high = mid - 1;   // Value too high, search left
            }
        }
        return result;
    }

    private static int countStrictlyLessIterative(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = 0; // Explicit result variable

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                result = mid + 1; // Record count of elements up to mid
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (Optimal)
     * 
     * Time Complexity: O(F log F + P log F)
     * Space Complexity: O(F + log F) - Call stack overhead.
     * 
     * EXPLANATION:
     * Functional recursion of the binary search algorithm, explicitly propagating 
     * the `result` variable back up the call stack.
     */
    public static int[] fullBloomFlowersRecursiveBS(int[][] flowers, int[] people) {
        int n = flowers.length;
        int[] starts = new int[n];
        int[] ends = new int[n];
        for (int i = 0; i < n; i++) {
            starts[i] = flowers[i][0];
            ends[i] = flowers[i][1];
        }
        Arrays.sort(starts);
        Arrays.sort(ends);
        
        int[] ans = new int[people.length];
        for (int i = 0; i < people.length; i++) {
            int started = countLessOrEqualRecursive(starts, people[i], 0, n - 1, 0);
            int ended = countStrictlyLessRecursive(ends, people[i], 0, n - 1, 0);
            ans[i] = started - ended;
        }
        return ans;
    }

    private static int countLessOrEqualRecursive(int[] arr, int target, int low, int high, int currentResult) {
        int result = currentResult;
        if (low > high) return result; // Base case
        
        int mid = low + (high - low) / 2;
        if (arr[mid] <= target) {
            result = countLessOrEqualRecursive(arr, target, mid + 1, high, mid + 1);
        } else {
            result = countLessOrEqualRecursive(arr, target, low, mid - 1, result);
        }
        return result;
    }

    private static int countStrictlyLessRecursive(int[] arr, int target, int low, int high, int currentResult) {
        int result = currentResult;
        if (low > high) return result;
        
        int mid = low + (high - low) / 2;
        if (arr[mid] < target) {
            result = countStrictlyLessRecursive(arr, target, mid + 1, high, mid + 1);
        } else {
            result = countStrictlyLessRecursive(arr, target, low, mid - 1, result);
        }
        return result;
    }

    /**
     * SOLUTION 3: Difference Array / Sweep Line (Highly Optimal for small Max Time)
     * 
     * Time Complexity: O(F + MaxTime + P)
     * Space Complexity: O(MaxTime)
     * 
     * EXPLANATION:
     * Because the time constraint is small (<= 10^4), we can create an array simulating 
     * the timeline. When a flower blooms, we increment at `start`. When it dies, we 
     * decrement at `end + 1`. A prefix sum of this array gives the active flowers at any time.
     */
    public static int[] fullBloomFlowersSweepLine(int[][] flowers, int[] people) {
        int maxTime = 0;
        for (int p : people) maxTime = Math.max(maxTime, p);
        for (int[] f : flowers) maxTime = Math.max(maxTime, f[1]);
        
        int[] diff = new int[maxTime + 2];
        for (int[] f : flowers) {
            diff[f[0]]++;
            diff[f[1] + 1]--;
        }
        
        // Calculate prefix sums directly in the diff array to save space
        for (int i = 1; i < diff.length; i++) {
            diff[i] += diff[i - 1];
        }
        
        int[] ans = new int[people.length];
        for (int i = 0; i < people.length; i++) {
            ans[i] = diff[people[i]];
        }
        return ans;
    }

    /**
     * SOLUTION 4: Brute Force (O(F * P))
     * 
     * Time Complexity: O(F * P)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * For every person, loops through every flower and checks if the arrival time 
     * falls between the start and end. Since F <= 10^3 and P <= 10^3, this takes 
     * up to 10^6 operations, which easily runs within a second in Java.
     */
    public static int[] fullBloomFlowersBruteForce(int[][] flowers, int[] people) {
        int[] ans = new int[people.length];
        for (int i = 0; i < people.length; i++) {
            int time = people[i];
            int count = 0;
            for (int[] flower : flowers) {
                if (time >= flower[0] && time <= flower[1]) {
                    count++;
                }
            }
            ans[i] = count;
        }
        return ans;
    }

    /**
     * SOLUTION 5: Java Streams (Functional Brute Force)
     * 
     * Time Complexity: O(F * P)
     * Space Complexity: O(1) Overhead
     * 
     * EXPLANATION:
     * Represents the O(F * P) brute force approach using modern Java functional programming.
     */
    public static int[] fullBloomFlowersStream(int[][] flowers, int[] people) {
        return Arrays.stream(people)
                .map(p -> (int) Arrays.stream(flowers)
                        .filter(f -> p >= f[0] && p <= f[1])
                        .count())
                .toArray();
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[][] flowers, int[] people, int[] expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on boundaries and examples
        TestCase[] testCases = {
            new TestCase(new int[][]{{1, 6}, {3, 7}, {9, 12}, {4, 13}}, new int[]{2, 3, 7, 11}, new int[]{1, 2, 2, 2}),
            new TestCase(new int[][]{{1, 10}, {3, 3}}, new int[]{3, 3, 2}, new int[]{2, 2, 1}),
            new TestCase(new int[][]{{5, 5}}, new int[]{4, 5, 6}, new int[]{0, 1, 0}), // Exact match bounds
            new TestCase(new int[][]{{1, 10000}}, new int[]{5000, 10001}, new int[]{1, 0}) // Maximum range
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int[] resIterativeBS = fullBloomFlowersIterativeBS(tc.flowers(), tc.people());
            int[] resRecursiveBS = fullBloomFlowersRecursiveBS(tc.flowers(), tc.people());
            int[] resSweepLine   = fullBloomFlowersSweepLine(tc.flowers(), tc.people());
            int[] resBruteForce  = fullBloomFlowersBruteForce(tc.flowers(), tc.people());
            int[] resStream      = fullBloomFlowersStream(tc.flowers(), tc.people());

            boolean passed = Arrays.equals(resIterativeBS, tc.expected()) &&
                             Arrays.equals(resRecursiveBS, tc.expected()) &&
                             Arrays.equals(resSweepLine, tc.expected()) &&
                             Arrays.equals(resBruteForce, tc.expected()) &&
                             Arrays.equals(resStream, tc.expected());

            System.out.printf("Test %d | People: %-15s -> Expected: %-15s | Passed: %b%n",
                    i + 1, Arrays.toString(tc.people()), Arrays.toString(tc.expected()), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iter: %s, Rec: %s, Sweep: %s, Brute: %s, Stream: %s%n",
                        Arrays.toString(resIterativeBS), Arrays.toString(resRecursiveBS), 
                        Arrays.toString(resSweepLine), Arrays.toString(resBruteForce), 
                        Arrays.toString(resStream));
            }
        }
    }
}
