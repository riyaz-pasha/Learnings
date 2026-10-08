/**
 * ============================================================================
 * 1086. HIGH FIVE (TOP FIVE AVERAGE SCORES)
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Are the IDs contiguous (e.g., 1, 2, 3) or can they be sparse (e.g., 1, 15, 1000)?" 
 *    (Determines whether we can safely iterate from 1 to 1000, or if we should explicitly collect and sort the unique IDs).
 * 2. "Are the scores strictly bounded between 0 and 100?" 
 *    (Crucial for space limits. A small bounded range allows Counting Sort instead of standard comparison sorting).
 * 3. "Is it guaranteed every student has at least 5 scores?" 
 *    (Yes, per constraints. This means we don't need to add defensive checks for dividing by 0 or fewer than 5 elements).
 * 4. "How should ties in the final averages be handled, or fractions?" 
 *    (Prompt states integer division by 5, meaning we just drop the decimal. 99.8 becomes 99).
 * 5. "If a student has the exact same score multiple times, do we count each instance?"
 *    (Yes, the top 5 scores could be [100, 100, 100, 100, 100]).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to group scores by student ID, extract exactly the top 5 highest values for each, 
 * > and return them sorted by ID. The bottleneck is sorting the scores per student.
 * 
 * ### Approach 1: Group and Full Sort (The Intuitive Brute Force)
 * 1. What I'd naturally try: I would create a `HashMap<Integer, List<Integer>>` to group all scores by ID. 
 *    Once grouped, I'd iterate through each student's list, sort it in descending order, take the first 5 elements, 
 *    sum them up, and divide by 5. Finally, I'd extract all IDs, sort them, and build the output array.
 * 2. Why it works: It directly translates the problem statement into code. Group, sort, calculate, return.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(N log N) — because if a single student has N scores, sorting that student's list takes 
 *      O(N log N). We sort the entire history of scores even though we only care about the top 5.
 *    - Space Complexity: O(N) auxiliary — because we store every single score inside our HashMap's lists.
 * 4. What work is being repeated: Sorting elements at index 6, 7, 8... all the way to N is completely useless 
 *    since we only ever look at the top 5.
 * 5. What property removes the bottleneck: A Min-Heap (PriorityQueue). A heap can maintain exactly the top 5 
 *    elements as we stream through the data, discarding small numbers instantly.
 * 
 * ### Approach 2: TreeMap + Min-Heap (The Standard Developer Way)
 * 1. What I'd naturally try: Use a `TreeMap<Integer, PriorityQueue<Integer>>`. The TreeMap automatically keeps 
 *    the student IDs sorted. The PriorityQueue (size bounded to 5) keeps track of the 5 highest scores.
 * 2. Why it works: As we process a score, we add it to the student's heap. If the heap size exceeds 5, 
 *    we `poll()` (remove) the smallest element. At the end, the heap contains only the 5 largest elements.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N log 5 + K log K) -> O(N + K log K) — where N is the total number of scores and K is 
 *      the number of unique students. Adding/removing from a size-5 heap is O(1). Sorting the K unique IDs in 
 *      the TreeMap takes O(K log K).
 *    - Space Complexity: O(K) auxiliary — because we store at most 5 integers per unique student, requiring 
 *      5 * K heap nodes.
 * 4. What work is being repeated: We are creating objects for Map entries, PriorityQueues, and Integer wrappers. 
 *    For small constraints (IDs up to 1000, scores up to 100), this heavy object allocation causes garbage 
 *    collection overhead.
 * 5. What property removes the bottleneck: The constraints explicitly bound both IDs (1-1000) and scores (0-100). 
 *    We can use a fixed-size 2D primitive array to group and sort everything implicitly!
 * 
 * ### Approach 3: 2D Frequency Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Create an `int[1001][101] freq` array. `freq[id][score]++` records a score. 
 *    To find the top 5, I just look at a student's bucket and scan backwards from score 100 down to 0 until 
 *    I've picked 5 scores.
 * 2. Why it works: Array indices naturally sort the data. Scanning from 100 downwards guarantees we encounter 
 *    the highest scores first. We completely bypass comparative sorting (like Quicksort or Heapsort).
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N + MAX_ID * MAX_SCORE) -> O(N + 100,000) -> O(N) — because we process the input 
 *      once (O(N)), then scan a fixed 100x1000 grid to compute averages.
 *    - Space Complexity: O(1) auxiliary — because we allocate a fixed 1001x101 integer array (~400 KB) 
 *      regardless of how large N grows. (If N was 1 million, space remains 400 KB).
 * 
 * > **Interview Strategy:** In a real interview, Approach 2 (TreeMap + MinHeap) is the "textbook" scalable 
 * > solution if bounds were unknown. However, writing Approach 3 (2D Array) demonstrates exceptional constraint 
 * > awareness and mechanical sympathy. I would code Approach 3 and explain why the bounds make it vastly superior.
 * 
 * 
 * ## 3. Edge cases
 * - Sparse IDs: Input has ID 1 and ID 1000, nobody in between. Approach 3 handles this efficiently (a quick 
 *   check if an ID has any scores before doing the 100-to-0 scan).
 * - Exact same scores: ID 1 has scores [100, 100, 100, 100, 100, 100]. The frequency array handles this natively 
 *   (`freq[1][100] = 6`), and our inner loop easily pulls exactly 5 of them.
 * - Score of 0: Must ensure our loop covers index 0 (`score >= 0`), not just > 0.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "Bounded Data = Counting Sort." Whenever you see maximum values limited to a tiny range 
 * (like test scores 0-100 or ages 0-120), discard TreeMaps, HashMaps, and sorting algorithms. Use an array 
 * where the *index* represents the value.
 * 
 * **Dry Run of Approach 3 (2D Array):**
 * Input: `[[1, 91], [1, 92], [1, 92], [1, 60], [1, 95], [1, 97]]`
 * 1. Build Phase:
 *    - freq[1][91] = 1
 *    - freq[1][92] = 2
 *    - freq[1][60] = 1
 *    - freq[1][95] = 1
 *    - freq[1][97] = 1
 * 2. Extraction Phase (Scanning from 100 down to 0 for ID 1):
 *    - score 100 to 98: freq is 0. Skip.
 *    - score 97: freq is 1. count=1, sum=97.
 *    - score 96: freq is 0. Skip.
 *    - score 95: freq is 1. count=2, sum=192.
 *    - score 94 to 93: freq is 0. Skip.
 *    - score 92: freq is 2. count=4, sum=376.
 *    - score 91: freq is 1. count=5, sum=467. BREAK (reached 5).
 * 3. Average Calculation: 467 / 5 = 93. 
 *    Result for ID 1 = `[1, 93]`.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q: What if the scores were unbounded (e.g., floating point values or going up to 10^9)?**
 *   **A:** The 2D array counting sort approach would fail due to an OutOfMemoryError. We would pivot to 
 *   Approach 2 (TreeMap + PriorityQueue), which bounds space to O(K * 5) regardless of score magnitude.
 * - **Q: What if we needed the Top 'K' scores instead of top 5, where K is dynamic?**
 *   **A:** Approach 3 still works flawlessly (just change the limit condition from `< 5` to `< K`). 
 *   Approach 2 also works by bounding the PriorityQueue size to K.
 * - **Q: What if the input is a massive, unbounded streaming dataset distributed across multiple servers?**
 *   **A:** We would use a MapReduce architecture. Each Mapper maintains a PriorityQueue of size 5 for 
 *   each ID in its chunk of data. The Mappers emit their top 5. The Reducer collects the top 5s from 
 *   all Mappers for a specific ID, merges them into a final PriorityQueue of size 5, and computes the average.
 * 
 * ============================================================================
 */

import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class HighFiveStudy {

    /**
     * Interface to run both approaches interchangeably.
     */
    interface TopFiveAveragesSolver {
        int[][] highFive(int[][] items);
    }

    // ========================================================================
    // APPROACH 3: 2D FREQUENCY ARRAY (The Optimal Hardware-Friendly Solution)
    // ========================================================================
    static class CountingSortSolver implements TopFiveAveragesSolver {
        @Override
        public int[][] highFive(int[][] items) {
            // Constraints: 1 <= ID <= 1000, 0 <= score <= 100
            // freq[id][score] stores how many times a student got a specific score
            int[][] freq = new int[1001][101];
            int uniqueStudents = 0;
            boolean[] seenStudent = new boolean[1001];

            // 1. Build the frequency map and count unique students
            for (int[] item : items) {
                int id = item[0];
                int score = item[1];
                freq[id][score]++;
                
                // Track how many unique students we have so we can size the result array
                if (!seenStudent[id]) {
                    seenStudent[id] = true;
                    uniqueStudents++;
                }
            }

            // 2. Compute averages
            int[][] result = new int[uniqueStudents][2];
            int resultIdx = 0;

            // Iterate through all possible IDs (1 to 1000) to implicitly sort by ID
            for (int id = 1; id <= 1000; id++) {
                if (!seenStudent[id]) {
                    continue; // Skip IDs that don't exist in our data
                }

                int sum = 0;
                int count = 0;
                
                // Scan scores from 100 down to 0 to inherently pick the highest first
                for (int score = 100; score >= 0 && count < 5; score--) {
                    // While we have instances of this score, and we haven't reached 5 yet
                    while (freq[id][score] > 0 && count < 5) {
                        sum += score;
                        count++;
                        freq[id][score]--;
                    }
                }
                
                result[resultIdx][0] = id;
                result[resultIdx][1] = sum / 5;
                resultIdx++;
            }

            return result;
        }
    }

    // ========================================================================
    // APPROACH 2: TREEMAP + MIN-HEAP (The General Scalable Solution)
    // Use this if scores were unbounded (e.g., 0 to 10^9)
    // ========================================================================
    static class HeapSolver implements TopFiveAveragesSolver {
        @Override
        public int[][] highFive(int[][] items) {
            // TreeMap keeps the IDs sorted. 
            // PriorityQueue acts as a Min-Heap to keep the top 5 scores.
            Map<Integer, PriorityQueue<Integer>> studentScores = new TreeMap<>();

            for (int[] item : items) {
                int id = item[0];
                int score = item[1];

                studentScores.computeIfAbsent(id, k -> new PriorityQueue<>()).add(score);

                // If we exceed 5 scores, kick out the smallest one.
                // This ensures the heap only ever contains the top 5 scores.
                if (studentScores.get(id).size() > 5) {
                    studentScores.get(id).poll();
                }
            }

            // Build result array
            int[][] result = new int[studentScores.size()][2];
            int idx = 0;

            for (Map.Entry<Integer, PriorityQueue<Integer>> entry : studentScores.entrySet()) {
                int id = entry.getKey();
                PriorityQueue<Integer> top5 = entry.getValue();

                int sum = 0;
                while (!top5.isEmpty()) {
                    sum += top5.poll();
                }

                result[idx][0] = id;
                result[idx][1] = sum / 5;
                idx++;
            }

            return result;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        TopFiveAveragesSolver[] solvers = {
            new CountingSortSolver(),
            new HeapSolver()
        };

        // Format: Input array of items
        int[][][] testCases = {
            // Test 1: Standard case with two students
            {
                {1, 91}, {1, 92}, {2, 93}, {2, 97}, {1, 60}, {2, 77}, 
                {1, 65}, {1, 87}, {1, 100}, {2, 100}, {2, 76}
            },
            // Test 2: Identical scores
            {
                {1, 100}, {1, 100}, {1, 100}, {1, 100}, {1, 100}, {1, 100}
            },
            // Test 3: Sparse IDs with minimum required (5) scores
            {
                {1000, 10}, {1000, 20}, {1000, 30}, {1000, 40}, {1000, 50}
            }
        };

        // Expected averages mapping corresponding to test cases
        int[][][] expectedResults = {
            {{1, 87}, {2, 88}},
            {{1, 100}},
            {{1000, 30}}
        };

        for (TopFiveAveragesSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[][] result = solver.highFive(testCases[i]);
                int[][] expected = expectedResults[i];
                
                // Compare arrays
                boolean passed = result.length == expected.length;
                if (passed) {
                    for (int j = 0; j < result.length; j++) {
                        if (result[j][0] != expected[j][0] || result[j][1] != expected[j][1]) {
                            passed = false;
                            break;
                        }
                    }
                }
                
                if (!passed) {
                    System.out.println("  [FAIL] Test " + (i + 1));
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Grouping + Top-K extraction.
 * - Key observation: Bounded value constraints (scores 0-100) mean you can skip 
 *   heavy sorting algorithms or Heaps entirely by using a Counting Sort Array.
 * - What's worth memorizing: The bounded Min-Heap pattern for Top-K tracking:
 *   `pq.add(val); if(pq.size() > K) pq.poll();`. It drops time complexity of 
 *   sorting massive lists down to O(N log K).
 * - Most common trap: Trying to group all scores into full lists and then calling 
 *   `Collections.sort()`. It works, but wastes CPU cycles sorting the bottom 90% 
 *   of scores you will never use.
 * - Mental trigger: "Top K elements per category? Use a Map of Min-Heaps. 
 *   Small score bounds? Use a 2D frequency array."
 * ============================================================================
 */


import java.util.*;

public class HighFiveSolutions {

    /**
     * APPROACH 1: TreeMap (Automatic Sorting)
     * 
     * Rationale: Uses a balanced binary search tree (TreeMap) to keep the student IDs 
     * sorted automatically as elements are inserted, eliminating the need for a manual sort step.
     * 
     * Time Complexity: O(N log K) 
     *   - We iterate through all N items. Inserting a key into a TreeMap takes O(log K) time, 
     *     where K is the number of unique students. 
     *   - The Min-Heap (`PriorityQueue`) operations (offer/poll) are bounded to a maximum size of 5, 
     *     making heap modifications O(log 5) which is O(1) constant time.
     *   - Iterating through the final map takes O(K) time.
     * 
     * Space Complexity: O(K)
     *   - Storing K unique student IDs in the map. Each student stores at most 5 scores in their heap.
     */
    public static int[][] highFiveTreeMap(int[][] items) {
        // TreeMap keeps keys naturally sorted by Student ID
        Map<Integer, PriorityQueue<Integer>> map = new TreeMap<>();
        
        for (int[] item : items) {
            int id = item[0];
            int score = item[1];
            
            PriorityQueue<Integer> pq = map.computeIfAbsent(id, k -> new PriorityQueue<>());
            pq.offer(score);
            
            // Maintain top 5 highest scores by evicting the smallest
            if (pq.size() > 5) {
                pq.poll();
            }
        }

        int[][] result = new int[map.size()][2];
        int i = 0;
        
        for (var entry : map.entrySet()) {
            int id = entry.getKey();
            PriorityQueue<Integer> pq = entry.getValue();
            
            int sum = 0;
            while (!pq.isEmpty()) {
                sum += pq.poll();
            }
            
            result[i++] = new int[] {id, sum / 5};
        }
        
        return result;
    }

    /**
     * APPROACH 2: HashMap + Manual Sorting at the End
     * 
     * Rationale: Prioritizes fast O(1) lookups during data ingestion using a HashMap, 
     * and defers the sorting work to a single operations phase at the very end.
     * 
     * Time Complexity: O(N + K log K)
     *   - We iterate through N items. HashMap insertion takes O(1) average time.
     *   - Bounded Min-Heap operations take O(1) constant time. 
     *   - Building the map takes O(N) time.
     *   - Sorting the K unique student IDs at the end takes O(K log K) time.
     * 
     * Space Complexity: O(K)
     *   - The HashMap stores K unique students, each holding a maximum of 5 scores.
     */
    public static int[][] highFiveHashMap(int[][] items) {
        Map<Integer, PriorityQueue<Integer>> map = new HashMap<>();
        
        // Phase 1: Build the map (Fast O(1) average insertions)
        for (int[] item : items) {
            int id = item[0];
            int score = item[1];
            
            PriorityQueue<Integer> pq = map.computeIfAbsent(id, k -> new PriorityQueue<>());
            pq.offer(score);
            
            if (pq.size() > 5) {
                pq.poll();
            }
        }

        // Phase 2: Manually sort the student IDs
        List<Integer> sortedIds = new ArrayList<>(map.keySet());
        Collections.sort(sortedIds);

        // Phase 3: Construct the final result
        int[][] result = new int[sortedIds.size()][2];
        int i = 0;
        
        for (int id : sortedIds) {
            PriorityQueue<Integer> pq = map.get(id);
            int sum = 0;
            while (!pq.isEmpty()) {
                sum += pq.poll();
            }
            result[i++] = new int[] {id, sum / 5};
        }
        
        return result;
    }

    /**
     * APPROACH 3: Direct Array Optimization (Ultimate Speed)
     * 
     * Rationale: Replaces high-overhead map structures with a fixed-size bucket array.
     * This relies on LeetCode constraints specifying student IDs fall between 1 and 1000.
     * 
     * Time Complexity: O(N + MAX_ID) -> effectively O(N)
     *   - We iterate through N items. Accessing an array index takes strict O(1) time.
     *   - Heap adjustments take O(1) constant time.
     *   - Iterating through the fixed 1001-element array to collect results takes O(MAX_ID) time,
     *     which behaves as a constant.
     * 
     * Space Complexity: O(MAX_ID) -> effectively O(1) auxiliary space
     *   - The array size is statically fixed at 1001 elements regardless of how large N grows.
     */
    public static int[][] highFiveArray(int[][] items) {
        // Statically sized array where index = Student ID (Supports IDs from 1 to 1000)
        PriorityQueue<Integer>[] studentHeaps = new PriorityQueue[1001];
        int uniqueStudentsCount = 0;

        // Phase 1: Ingest data into the array buckets
        for (int[] item : items) {
            int id = item[0];
            int score = item[1];
            
            if (studentHeaps[id] == null) {
                studentHeaps[id] = new PriorityQueue<>();
                uniqueStudentsCount++;
            }
            
            studentHeaps[id].offer(score);
            if (studentHeaps[id].size() > 5) {
                studentHeaps[id].poll();
            }
        }

        // Phase 2: Collect results natively in ascending order by scanning the array left-to-right
        int[][] result = new int[uniqueStudentsCount][2];
        int idx = 0;
        
        for (int id = 1; id <= 1000; id++) {
            if (studentHeaps[id] != null) {
                PriorityQueue<Integer> pq = studentHeaps[id];
                int sum = 0;
                while (!pq.isEmpty()) {
                    sum += pq.poll();
                }
                result[idx++] = new int[] {id, sum / 5};
            }
        }
        
        return result;
    }

    public static void main(String[] args) {
        // Sample dataset: Student 1 has 6 scores, Student 2 has 5 scores
        int[][] items = {
            {1, 91}, {1, 92}, {2, 93}, {2, 97}, {1, 60},
            {2, 77}, {1, 65}, {1, 87}, {1, 100}, {2, 100}, {2, 76}
        };

        System.out.println("--- Testing Approach 1: TreeMap ---");
        printResult(highFiveTreeMap(items));

        System.out.println("\n--- Testing Approach 2: HashMap + Sort ---");
        printResult(highFiveHashMap(items));

        System.out.println("\n--- Testing Approach 3: Bounded Array ---");
        printResult(highFiveArray(items));
    }

    private static void printResult(int[][] result) {
        for (int[] row : result) {
            System.out.println("ID: " + row[0] + " | Average: " + row[1]);
        }
    }
}

/*
Yes, there is a distinct trade-off in time complexity between using a TreeMap versus sorting a HashMap later.
For the High Five problem, the TreeMap approach is generally preferred because it simplifies the code, but let's break down exactly how both options stack up structurally and computationally.
------------------------------
## Time Complexity Comparison
Let:

* N be the total number of logs/items in the input array.
* K be the unique number of students (keys).
* Since each student's heap is capped at a maximum size of 5, heap insertions take $O(\log 5) = O(1)$ constant time.

| Operation | HashMap + Sorting at the End | TreeMap (Insertion-based sorting) |
|---|---|---|
| Phase 1: Building the Map | O(N) Each of the N insertions takes O(1) constant time. | $O(N \log K)$ Each of the N insertions requires searching the tree, taking $O(\log K)$ time. |
| Phase 2: Sorting the IDs | $O(K \log K)$ Sorting the K student entries at the end. | O(0) Already sorted natively by the tree structure. |
| Phase 3: Calculating Averages | O(K) Iterating through K students to empty heaps of size 5. | O(K) Iterating through K students to empty heaps of size 5. |
| Total Time Complexity | $O(N + K \log K)$ | $O(N \log K)$ |

------------------------------
## Which one is actually faster?

* Why we switched to TreeMap: It keeps your code extremely readable and prevents messy syntax errors while trying to sort map entry lists in Java.
* Performance Realities:
* If the number of items (N) is massive but the number of students (K) is small, HashMap is technically faster because O(N) beats $O(N \log K)$.
   * However, in LeetCode environments, the constraints for this specific problem usually cap the student IDs to a very small range (e.g., 1 ≤ ID ≤ 1000). Because K is so small, $\log K$ is practically a constant, meaning both approaches run in milliseconds and pass easily.

## The Ultimate Optimization (Direct Array)
If you want to achieve the absolute fastest runtime on LeetCode (O(N) time and O(1) auxiliary space), you can bypass both maps completely. Because student IDs are positive integers, you can use a fixed-size array of PriorityQueue objects where the array index acts directly as the student ID:

// If constraints state IDs are between 1 and 1000
PriorityQueue<Integer>[] studentHeaps = new PriorityQueue[1001]; 

This gives you the O(1) lookup speeds of a HashMap while maintaining index-based sorting for free.
Would you like to see how to implement this fixed-size array optimization, or are you ready to test the TreeMap solution?


*/
