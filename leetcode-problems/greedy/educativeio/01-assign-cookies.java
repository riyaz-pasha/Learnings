import java.util.*;

/**
 * ============================================================================
 * INTERVIEW GUIDE: ASSIGN COOKIES
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can we combine two small cookies to satisfy one child?" 
 *      (Assumption: No, each child gets at most one cookie).
 *    - "Are the input arrays guaranteed to be sorted?" 
 *      (Assumption: No, we must handle unsorted inputs).
 *    - "Can either array be empty?" 
 *      (Assumption: Yes, if so, the answer is immediately 0).
 *    - "Will there be negative greed factors or cookie sizes?"
 *      (Assumption: Based on standard constraints, sizes are >= 0 and greed >= 1).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Maximize the number of satisfied children.
 *    - Observation 1 (Greedy Choice on Children): A child with a smaller greed 
 *      factor is inherently easier to satisfy. We should try to satisfy the 
 *      least greedy children first to maximize the total count.
 *    - Observation 2 (Greedy Choice on Cookies): To save larger cookies for 
 *      greedier children, we should always give a child the *smallest* 
 *      available cookie that still meets their greed requirement.
 *    - Approach: Sort both arrays. Iterate through the cookies, matching them 
 *      to the least greedy unsatisfied child.
 * 
 * 3. VISUAL EXPLANATION:
 *    Greed Factors: [1, 2, 3]
 *    Cookie Sizes:  [1, 1]
 *    
 *    Sorted Greed:  [1, 2, 3]
 *                    ^ (Child pointer: childIndex)
 *    Sorted Cookies:[1, 1]
 *                    ^ (Cookie pointer: cookieIndex)
 *    
 *    Step 1: Cookie[0] (1) >= Greed[0] (1). 
 *            Child 0 is happy! Move both pointers. (Satisfied = 1)
 *            
 *    Step 2: childIndex=1 (Greed=2), cookieIndex=1 (Cookie=1).
 *            Cookie[1] < Greed[1]. Cookie is too small. 
 *            Move cookieIndex to find a bigger cookie.
 *            
 *    Step 3: cookieIndex=2. Out of bounds. End. 
 *            Total Satisfied: 1.
 * 
 * ============================================================================
 */
class AssignCookies {

    /**
     * APPROACH 1: Greedy with Sorting and Two Pointers (Optimal)
     * 
     * Time Complexity: O(N log N + M log M) where N and M are the lengths of the arrays.
     * Space Complexity: O(1) or O(log N) depending on the sorting algorithm used under the hood.
     */
    public int findContentChildrenOptimal(int[] greedFactors, int[] cookieSizes) {
        // Step 1: Sort both arrays to enable the greedy approach
        Arrays.sort(greedFactors);
        Arrays.sort(cookieSizes);
        
        int childIndex = 0;
        int cookieIndex = 0;
        
        // Step 2: Iterate through both arrays
        while (childIndex < greedFactors.length && cookieIndex < cookieSizes.length) {
            // If the current cookie is large enough for the current child
            if (cookieSizes[cookieIndex] >= greedFactors[childIndex]) {
                // The child is satisfied, move to the next child
                childIndex++;
            }
            // Regardless of whether the cookie was used or too small, move to the next cookie.
            // (If it was too small for this child, it will be too small for any greedier child).
            cookieIndex++;
        }
        
        // The childIndex represents exactly how many children were satisfied
        return childIndex;
    }

    /**
     * APPROACH 2: Greedy with Min-Heaps (Alternative / Streaming Data Concept)
     * 
     * If data was coming in as a stream or we wanted to show off Collection 
     * knowledge, we could use Priority Queues (Min-Heaps). 
     * Note: This is practically slower due to Object boxing and heap overhead, 
     * but demonstrates strong data structure knowledge in an interview.
     * 
     * Time Complexity: O(N log N + M log M)
     * Space Complexity: O(N + M) for the heaps.
     */
    public int findContentChildrenMinHeap(int[] greedFactors, int[] cookieSizes) {
        // Using Java Streams to cleanly box primitives and load them into Min-Heaps
        PriorityQueue<Integer> greedHeap = Arrays.stream(greedFactors)
                                                 .boxed()
                                                 .collect(Collectors.toCollection(PriorityQueue::new));
                                                 
        PriorityQueue<Integer> cookieHeap = Arrays.stream(cookieSizes)
                                                  .boxed()
                                                  .collect(Collectors.toCollection(PriorityQueue::new));
        
        int satisfiedCount = 0;
        
        while (!greedHeap.isEmpty() && !cookieHeap.isEmpty()) {
            int currentCookie = cookieHeap.poll();
            int currentGreed = greedHeap.peek();
            
            if (currentCookie >= currentGreed) {
                // Child is satisfied. Remove them from the queue and increment count.
                greedHeap.poll();
                satisfiedCount++;
            }
            // If cookie < greed, we just drop the cookie (it was polled and not used)
        }
        
        return satisfiedCount;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14/16) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] greed, int[] cookies, int expected) {}

    public static void main(String[] args) {
        AssignCookies solver = new AssignCookies();
        
        // Defining test cases using our Record
        var testCases = java.util.List.of(
            new TestCase(new int[]{1, 2, 3}, new int[]{1, 1}, 1),
            new TestCase(new int[]{1, 2}, new int[]{1, 2, 3}, 2),
            new TestCase(new int[]{10, 9, 8, 7}, new int[]{5, 6, 7, 8}, 2),
            new TestCase(new int[]{}, new int[]{1, 2, 3}, 0),
            new TestCase(new int[]{1, 2, 3}, new int[]{}, 0)
        );
        
        System.out.println("--- Running Optimal Two-Pointer Tests ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.findContentChildrenOptimal(tc.greed(), tc.cookies());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Min-Heap Tests ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.findContentChildrenMinHeap(tc.greed(), tc.cookies());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}

class AssignCookies2 {

    public static void main(String[] args) {
        int[] greedFactors = {1, 2, 3};
        int[] cookieSizes = {1, 1};

        System.out.println(findContentChildren(greedFactors, cookieSizes));
    }

    /**
     * ============================================================
     * 🔥 GREEDY SOLUTION — ASSIGN COOKIES
     * ============================================================
     *
     * 🧠 INTUITION (How to think in interview):
     * ----------------------------------------
     * We want to maximize number of satisfied children.
     *
     * Key Observations:
     * 1. A child is satisfied only if:
     *        cookieSize >= greedFactor
     *
     * 2. If we give a BIG cookie to a SMALL greed child:
     *        → we may waste it
     *        → and fail to satisfy a bigger greed child later
     *
     * ✅ So optimal strategy:
     *    Always satisfy the LEAST greedy child using the SMALLEST possible cookie.
     *
     * This is a classic GREEDY choice.
     *
     * ------------------------------------------------------------
     * ⚙️ ALGORITHM
     * ------------------------------------------------------------
     * 1. Sort greedFactors (ascending)
     * 2. Sort cookieSizes (ascending)
     *
     * 3. Use two pointers:
     *      child  → tracks current child
     *      cookie → tracks current cookie
     *
     * 4. If cookie can satisfy child:
     *        assign → move both pointers
     *    else:
     *        try bigger cookie → move cookie pointer
     *
     * ------------------------------------------------------------
     * ⏱️ TIME COMPLEXITY:
     * Sorting:
     *    O(n log n + m log m)
     * Traversal:
     *    O(n + m)
     *
     * Total:
     *    O(n log n + m log m)
     *
     * 🧠 SPACE COMPLEXITY:
     *    O(1) (ignoring sorting internal space)
     *
     * ------------------------------------------------------------
     * 🎯 WHY GREEDY WORKS:
     * ------------------------------------------------------------
     * - If a small cookie can't satisfy a child,
     *   it can't satisfy ANY future child (since greed increases)
     *
     * - If a small cookie CAN satisfy a child,
     *   using a larger cookie is wasteful
     *
     * → Local optimal decision leads to global optimal solution
     *
     * ============================================================
     */
    public static int findContentChildren(int[] greedFactors, int[] cookieSizes) {

        // Step 1: Sort both arrays to process from smallest → largest
        Arrays.sort(greedFactors);
        Arrays.sort(cookieSizes);

        int child = 0;   // Pointer for children (greedFactors)
        int cookie = 0;  // Pointer for cookies (cookieSizes)

        int contentChildren = 0; // Result: number of satisfied children

        // Step 2: Try assigning cookies
        while (child < greedFactors.length && cookie < cookieSizes.length) {

            /*
             * CASE 1:
             * Current cookie can satisfy current child
             *
             * greedFactors[child] <= cookieSizes[cookie]
             *
             * → Assign cookie
             * → Move both pointers (child + cookie)
             */
            if (cookieSizes[cookie] >= greedFactors[child]) {
                contentChildren++; // one child satisfied
                child++;           // move to next child
                cookie++;          // move to next cookie
            }
            /*
             * CASE 2:
             * Cookie is too small
             *
             * → This cookie cannot satisfy this child
             * → It also cannot satisfy any future child (since greed increases)
             *
             * → So discard this cookie and try a bigger one
             */
            else {
                cookie++; // try next larger cookie
            }
        }

        return contentChildren;
    }
}
