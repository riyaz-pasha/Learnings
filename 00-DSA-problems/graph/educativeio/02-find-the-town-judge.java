import java.util.*;

/*
 * ==========================================================================================
 *                                   FIND THE TOWN JUDGE
 * ==========================================================================================
 *
 * 1. PROBLEM RESTATEMENT IN SIMPLE TERMS
 * ------------------------------------------------------------------------------------------
 * There are N people in a town, numbered 1 to N. You are given a list of pairs where 
 * [a, b] means "person 'a' trusts person 'b'". 
 * We need to find if there is a Town Judge. The Town Judge is special because:
 *   1. They trust NO ONE.
 *   2. EVERYONE ELSE trusts them.
 * If such a person exists, return their number. Otherwise, return -1.
 *
 *
 * 2. IDEA, INTUITION, AND KEY OBSERVATIONS
 * ------------------------------------------------------------------------------------------
 * - Graph Representation: This problem is perfectly modeled as a Directed Graph.
 *   Each person is a node, and a trust relationship [a, b] is a directed edge a -> b.
 * - Out-Degree: The number of outgoing edges a node has (how many people they trust).
 * - In-Degree: The number of incoming edges a node has (how many people trust them).
 * 
 * - The Judge's Profile:
 *   Rule 1 (Trusts no one): Out-Degree must be exactly 0.
 *   Rule 2 (Trusted by everyone else): In-Degree must be exactly N - 1.
 *
 * - Therefore, identifying the judge is simply finding the node in a directed graph 
 *   that has an Out-Degree of 0 and an In-Degree of N - 1.
 *
 *
 * 3. HOW TO IDENTIFY THE RIGHT APPROACH
 * ------------------------------------------------------------------------------------------
 * - Whenever a problem involves "relationships" (who likes who, who trusts who, who follows 
 *   who), think of Graphs.
 * - Whenever you need to count how many relationships point TO a node vs. FROM a node, 
 *   think of In-Degree and Out-Degree arrays.
 * - Because the nodes are sequentially numbered 1 to N, an array of size N + 1 is the 
 *   most efficient way to store these counts (O(1) access time).
 *
 *
 * 4. CLARIFYING QUESTIONS TO ASK THE INTERVIEWER (And why they matter)
 * ------------------------------------------------------------------------------------------
 * Q1: What happens if N = 1 and the trust array is empty?
 *     Why: This is a classic edge case. If there's only 1 person, they automatically trust 
 *          no one (0 out-degree) and everyone else (0 people) trusts them. So, 1 is the judge.
 * Q2: Can there be duplicate trust relationships in the input array?
 *     Why: If duplicates are allowed, we'd need to use a HashSet to deduplicate relationships 
 *          before counting. The constraints say all pairs are unique, so we can just count.
 * Q3: Can a person trust themselves?
 *     Why: If they can, it messes up the N-1 in-degree calculation. The constraints say 
 *          ai != bi, so this won't happen.
 * Q4: Can there be multiple town judges?
 *     Why: Good for understanding graph mechanics. Logically, if there were two judges, 
 *          they would have to trust each other (violating rule 1) or not trust each other 
 *          (violating rule 2). Hence, there can be AT MOST one judge.
 *
 *
 * 5. ASCII VISUALS / TRACING
 * ------------------------------------------------------------------------------------------
 * Let N = 4, trust = [[1,3], [1,4], [2,3], [2,4], [4,3]]
 * 
 * Graph View:
 *      1 ----> 4
 *      |      /|
 *      v    /  |
 *      3 <L-   |
 *      ^       |
 *      |       |
 *      2 -------
 *
 * Node Degrees:
 * Node 1: Out = 2, In = 0
 * Node 2: Out = 2, In = 0
 * Node 3: Out = 0, In = 3  <--- Judge! (Out = 0, In = N - 1)
 * Node 4: Out = 1, In = 2
 *
 * Array Representation (Net Trust Score = In - Out):
 * Index:  0   1   2   3   4
 * Score: [0, -2, -2,  3,  1] 
 * Since Score[3] == N - 1 (which is 3), Person 3 is the judge.
 *
 *
 * 6. EXAMPLES AND IMPORTANT EDGE CASES
 * ------------------------------------------------------------------------------------------
 * - Edge Case 1: N = 1, trust = []
 *   Output: 1 (The only person trivially satisfies both conditions).
 * - Edge Case 2: N = 3, trust = [[1,2], [2,3]]
 *   Output: -1 (Nobody is trusted by everyone else. Node 3 trusts no one, but only Node 2 trusts Node 3).
 * - Edge Case 3: N = 3, trust = [[1,3], [2,3], [3,1]]
 *   Output: -1 (Node 3 is trusted by everyone, but Node 3 trusts Node 1. Violates rule 1).
 *
 *
 * 7. COMMON MISTAKES AND PITFALLS
 * ------------------------------------------------------------------------------------------
 * 1. Using a HashMap<Integer, List<Integer>> to build an actual graph. While correct, it is 
 *    massive overkill and wastes space and time. We only care about the *counts* (degrees), 
 *    not the actual connections.
 * 2. Forgetting the N = 1 edge case.
 * 3. Array Indexing: Nodes are 1-indexed (1 to N). If you make an array of size N, you'll 
 *    get an ArrayIndexOutOfBoundsException or have to do messy "node - 1" math everywhere. 
 *    Just use an array of size N + 1 and ignore index 0.
 *
 *
 * 8. INTERVIEW STRATEGY: HOW TO APPROACH AND EXPLAIN
 * ------------------------------------------------------------------------------------------
 * 1. Acknowledge the graph nature: "This looks like a directed graph where we are looking 
 *    for a node with an out-degree of 0 and an in-degree of N - 1."
 * 2. Start with the 2-Array approach (Approach 1 below) to explain your logic clearly.
 * 3. Immediately optimize: "Actually, we don't need two arrays. We can use a single array 
 *    to track a 'net trust score'. Trusting someone decreases your score, being trusted 
 *    increases it. The judge will have a score of exactly N - 1."
 * 4. Write Approach 2 (Single Array). It shows strong optimization skills and deep understanding.
 * ==========================================================================================
 */

public class FindTheTownJudge {

    /*
     * APPROACH 1: TWO ARRAYS (In-Degree and Out-Degree)
     * --------------------------------------------------------------------------------------
     * Idea: Maintain two separate arrays of size N + 1. 
     * Iterate through the trust pairs:
     *   - Increment out-degree for the person trusting.
     *   - Increment in-degree for the person being trusted.
     * Finally, scan the arrays from 1 to N to find the person with outDegree == 0 
     * and inDegree == N - 1.
     *
     * Complexity:
     * - Time: O(E + N) where E is the number of edges (length of trust array).
     * - Space: O(N) for the two arrays.
     */
    public int findJudgeTwoArrays(int n, int[][] trust) {
        int[] inDegree = new int[n + 1];
        int[] outDegree = new int[n + 1];

        for (int[] relation : trust) {
            int truster = relation[0];
            int trustee = relation[1];
            outDegree[truster]++;
            inDegree[trustee]++;
        }

        for (int i = 1; i <= n; i++) {
            if (inDegree[i] == n - 1 && outDegree[i] == 0) {
                return i;
            }
        }

        return -1;
    }


    /*
     * APPROACH 2: SINGLE ARRAY OPTIMIZATION (Net Trust Score) - RECOMMENDED
     * --------------------------------------------------------------------------------------
     * Idea: Instead of two arrays, we can combine them into a single `trustScores` array.
     * - If person A trusts someone, they CANNOT be the judge. We decrement their score.
     * - If person B is trusted, they are closer to being the judge. We increment their score.
     * - The judge must be trusted by N - 1 people and trust 0 people. 
     * - Therefore, the judge's final score must be exactly N - 1.
     * - No one else can possibly reach a score of N - 1 because if someone is trusted by 
     *   everyone else (N-1), but they trust even one person, their score becomes (N-1) - 1 = N-2.
     *
     * Why this is better: Reduces space complexity by half, keeping code cleaner and caching better.
     * (We use basic arrays instead of modern Java Streams because simple array iteration 
     * is drastically faster and more idiomatic for low-level counting problems).
     *
     * Complexity:
     * - Time: O(E + N) where E is trust.length. We iterate over the edges, then over N nodes.
     * - Space: O(N) for the single array.
     */
    public int findJudgeSingleArray(int n, int[][] trust) {
        // Edge case: if the graph is incomplete logically, fail early.
        // For N people, we need at least N - 1 directed edges for anyone to be a judge.
        if (trust.length < n - 1) {
            return -1;
        }

        // 1-indexed array to match person labels directly.
        int[] trustScores = new int[n + 1];

        // Process all trust relationships
        for (int[] relation : trust) {
            int truster = relation[0];
            int trustee = relation[1];
            
            // Truster loses a point (disqualifies them from reaching N-1)
            trustScores[truster]--;
            // Trustee gains a point
            trustScores[trustee]++;
        }

        // Find the person with a score of exactly N - 1
        for (int i = 1; i <= n; i++) {
            if (trustScores[i] == n - 1) {
                return i;
            }
        }

        // No one met the criteria
        return -1;
    }


    /*
     * ==========================================================================================
     * INTERVIEWER FOLLOW-UP QUESTIONS & ANSWERS
     * ==========================================================================================
     * 
     * F1: "Can we solve this using O(1) space?"
     * Ans: No. We are accumulating relationships across up to 10^4 edges for up to 1000 nodes.
     *      Because edges arrive in random order, we must store state (counts) for all N nodes 
     *      simultaneously. Therefore, O(N) space is the mathematical lower bound.
     * 
     * F2: "What if the input array is extremely large (e.g., millions of edges), but N is small?"
     * Ans: The single array approach handles this perfectly. The time complexity scales linearly 
     *      with edges O(E), and space remains small O(N). Memory locality is excellent.
     *      One small optimization: we added a check `if (trust.length < n - 1) return -1;`.
     *      This instantly catches cases where there aren't even enough edges to form a judge,
     *      saving millions of iterations if the input is malformed.
     *
     * F3: "What if 'trust' is given as an Adjacency Matrix instead of an Edge List?"
     * Ans: If given an N x N matrix `matrix[i][j]` (where 1 means i trusts j):
     *      We would check each person. For person i to be a judge:
     *      - Row i must be all 0s (trusts no one).
     *      - Column i must be all 1s except matrix[i][i] (trusted by everyone).
     *      We can optimize this to O(N) by traversing the matrix intelligently (like the 
     *      'Find the Celebrity' problem logic: eliminate candidates sequentially).
     *
     * F4: "Why did you use standard for-loops instead of Java Streams?"
     * Ans: For primitive arrays and tight loops, standard Java for-loops are highly optimized 
     *      by the JIT compiler, have zero object allocation overhead, and are strictly faster. 
     *      Streams here would require boxing/unboxing or complex collectors, hurting readability 
     *      and performance without adding value.
     * ==========================================================================================
     */


    // --------------------------------------------------------------------------------------
    // TEST RUNNER CODE
    // --------------------------------------------------------------------------------------
    public static void main(String[] args) {
        FindTheTownJudge solution = new FindTheTownJudge();

        // Test Case 1: Simple valid judge
        int n1 = 2;
        int[][] trust1 = {{1, 2}};
        System.out.println("Test 1 (Expected 2): " + solution.findJudgeSingleArray(n1, trust1));

        // Test Case 2: Missing trust (No one trusts 3)
        int n2 = 3;
        int[][] trust2 = {{1, 3}, {2, 3}, {3, 1}};
        System.out.println("Test 2 (Expected -1): " + solution.findJudgeSingleArray(n2, trust2));

        // Test Case 3: N = 1 (Trivial judge)
        int n3 = 1;
        int[][] trust3 = {};
        System.out.println("Test 3 (Expected 1): " + solution.findJudgeSingleArray(n3, trust3));

        // Test Case 4: Complete valid example
        int n4 = 4;
        int[][] trust4 = {{1, 3}, {1, 4}, {2, 3}, {2, 4}, {4, 3}};
        System.out.println("Test 4 (Expected 3): " + solution.findJudgeSingleArray(n4, trust4));
        
        // Test Case 5: Early termination trigger (Not enough edges)
        int n5 = 5;
        int[][] trust5 = {{1, 2}, {2, 3}, {3, 4}}; // Only 3 edges, need at least 4
        System.out.println("Test 5 (Expected -1): " + solution.findJudgeSingleArray(n5, trust5));
    }
}


