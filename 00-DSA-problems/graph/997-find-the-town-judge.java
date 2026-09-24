import java.util.*;
/*
 * In a town, there are n people labeled from 1 to n. There is a rumor that one
 * of these people is secretly the town judge.
 * 
 * If the town judge exists, then:
 * 
 * The town judge trusts nobody.
 * Everybody (except for the town judge) trusts the town judge.
 * There is exactly one person that satisfies properties 1 and 2.
 * You are given an array trust where trust[i] = [ai, bi] representing that the
 * person labeled ai trusts the person labeled bi. If a trust relationship does
 * not exist in trust array, then such a trust relationship does not exist.
 * 
 * Return the label of the town judge if the town judge exists and can be
 * identified, or return -1 otherwise.
 * 
 * Example 1:
 * Input: n = 2, trust = [[1,2]]
 * Output: 2
 *
 * Example 2:
 * Input: n = 3, trust = [[1,3],[2,3]]
 * Output: 3
 * 
 * Example 3:
 * Input: n = 3, trust = [[1,3],[2,3],[3,1]]
 * Output: -1
 */

class TownJudgeSolutions {

    /**
     * Solution 1: Using Arrays to count trusts
     * Time Complexity: O(n + trust.length)
     * Space Complexity: O(n)
     */
    public int findJudge1(int n, int[][] trust) {
        // trustCount[i] represents how many people trust person i
        // trustedBy[i] represents how many people person i trusts
        int[] trustCount = new int[n + 1];
        int[] trustedBy = new int[n + 1];

        // Count trust relationships
        for (int[] t : trust) {
            trustedBy[t[0]]++; // t[0] trusts someone
            trustCount[t[1]]++; // t[1] is trusted by someone
        }

        // Find the judge: trusted by n-1 people and trusts nobody
        for (int i = 1; i <= n; i++) {
            if (trustCount[i] == n - 1 && trustedBy[i] == 0) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Solution 2: Optimized using single array (in-degree - out-degree)
     * Time Complexity: O(n + trust.length)
     * Space Complexity: O(n)
     */
    public int findJudge2(int n, int[][] trust) {
        // score[i] = (number of people who trust i) - (number of people i trusts)
        // Judge should have score = n - 1
        int[] score = new int[n + 1];

        for (int[] t : trust) {
            score[t[0]]--; // t[0] trusts someone (decrease score)
            score[t[1]]++; // t[1] is trusted by someone (increase score)
        }

        for (int i = 1; i <= n; i++) {
            if (score[i] == n - 1) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Solution 3: Using HashSet for clear logic
     * Time Complexity: O(n + trust.length)
     * Space Complexity: O(n)
     */
    public int findJudge3(int n, int[][] trust) {
        Set<Integer> candidates = new HashSet<>();
        Set<Integer> trustsSomeone = new HashSet<>();
        int[] trustCount = new int[n + 1];

        // Initialize all people as potential judges
        for (int i = 1; i <= n; i++) {
            candidates.add(i);
        }

        // Process trust relationships
        for (int[] t : trust) {
            trustsSomeone.add(t[0]); // t[0] trusts someone, can't be judge
            trustCount[t[1]]++; // Count how many trust t[1]
        }

        // Remove people who trust someone from candidates
        candidates.removeAll(trustsSomeone);

        // Check remaining candidates
        for (int candidate : candidates) {
            if (trustCount[candidate] == n - 1) {
                return candidate;
            }
        }

        return -1;
    }

    /**
     * Solution 4: Early termination with candidate tracking
     * Time Complexity: O(trust.length)
     * Space Complexity: O(1) - not counting input
     */
    public int findJudge4(int n, int[][] trust) {
        if (n == 1 && trust.length == 0)
            return 1;

        // Use map to track potential judges and their trust counts
        Map<Integer, Integer> trustCount = new HashMap<>();
        Set<Integer> trustsSomeone = new HashSet<>();

        for (int[] t : trust) {
            trustsSomeone.add(t[0]);
            trustCount.put(t[1], trustCount.getOrDefault(t[1], 0) + 1);
        }

        // Find person who is trusted by n-1 people and doesn't trust anyone
        for (Map.Entry<Integer, Integer> entry : trustCount.entrySet()) {
            int person = entry.getKey();
            int count = entry.getValue();

            if (count == n - 1 && !trustsSomeone.contains(person)) {
                return person;
            }
        }

        return -1;
    }

    /**
     * Solution 5: Most space-efficient approach
     * Time Complexity: O(trust.length + n)
     * Space Complexity: O(n)
     */
    public int findJudge5(int n, int[][] trust) {
        int[] netTrust = new int[n + 1];

        // Calculate net trust: incoming trust - outgoing trust
        for (int[] relation : trust) {
            netTrust[relation[0]]--; // Person who trusts loses a point
            netTrust[relation[1]]++; // Person who is trusted gains a point
        }

        // Judge must have net trust of n-1 (trusted by everyone except themselves)
        for (int i = 1; i <= n; i++) {
            if (netTrust[i] == n - 1) {
                return i;
            }
        }

        return -1;
    }

    // Test method
    public static void main(String[] args) {
        TownJudgeSolutions solution = new TownJudgeSolutions();

        // Test case 1: n=2, trust=[[1,2]]
        // Expected: 2 (person 2 is trusted by person 1 and trusts nobody)
        int[][] trust1 = { { 1, 2 } };
        System.out.println("Test 1 - Expected: 2");
        System.out.println("Solution 1: " + solution.findJudge1(2, trust1));
        System.out.println("Solution 2: " + solution.findJudge2(2, trust1));
        System.out.println("Solution 3: " + solution.findJudge3(2, trust1));
        System.out.println("Solution 4: " + solution.findJudge4(2, trust1));
        System.out.println("Solution 5: " + solution.findJudge5(2, trust1));

        // Test case 2: n=3, trust=[[1,3],[2,3]]
        // Expected: 3 (person 3 is trusted by persons 1 and 2, trusts nobody)
        int[][] trust2 = { { 1, 3 }, { 2, 3 } };
        System.out.println("\nTest 2 - Expected: 3");
        System.out.println("Solution 1: " + solution.findJudge1(3, trust2));
        System.out.println("Solution 2: " + solution.findJudge2(3, trust2));
        System.out.println("Solution 3: " + solution.findJudge3(3, trust2));
        System.out.println("Solution 4: " + solution.findJudge4(3, trust2));
        System.out.println("Solution 5: " + solution.findJudge5(3, trust2));

        // Test case 3: n=3, trust=[[1,3],[2,3],[3,1]]
        // Expected: -1 (person 3 trusts person 1, so can't be judge)
        int[][] trust3 = { { 1, 3 }, { 2, 3 }, { 3, 1 } };
        System.out.println("\nTest 3 - Expected: -1");
        System.out.println("Solution 1: " + solution.findJudge1(3, trust3));
        System.out.println("Solution 2: " + solution.findJudge2(3, trust3));
        System.out.println("Solution 3: " + solution.findJudge3(3, trust3));
        System.out.println("Solution 4: " + solution.findJudge4(3, trust3));
        System.out.println("Solution 5: " + solution.findJudge5(3, trust3));

        // Test case 4: n=1, trust=[]
        // Expected: 1 (single person is automatically the judge)
        int[][] trust4 = {};
        System.out.println("\nTest 4 - Expected: 1");
        System.out.println("Solution 1: " + solution.findJudge1(1, trust4));
        System.out.println("Solution 2: " + solution.findJudge2(1, trust4));
        System.out.println("Solution 3: " + solution.findJudge3(1, trust4));
        System.out.println("Solution 4: " + solution.findJudge4(1, trust4));
        System.out.println("Solution 5: " + solution.findJudge5(1, trust4));
    }

}

/*
 * Algorithm Explanation:
 * 
 * The problem is essentially finding a node in a directed graph where:
 * 1. The node has in-degree = n-1 (everyone trusts the judge)
 * 2. The node has out-degree = 0 (judge trusts nobody)
 * 
 * Key Insights:
 * 1. There can be at most one judge
 * 2. Judge must be trusted by exactly n-1 people
 * 3. Judge must trust exactly 0 people
 * 4. Net trust score for judge = (n-1) - 0 = n-1
 * 
 * Solution Comparison:
 * 
 * Solution 1 (Two Arrays):
 * - Most straightforward approach
 * - Separately tracks incoming and outgoing trust
 * - Easy to understand but uses more space
 * 
 * Solution 2 (Net Trust Score):
 * - Most efficient and elegant
 * - Uses the insight that judge's net trust = n-1
 * - Single pass through trust array, single pass through people
 * 
 * Solution 3 (HashSet):
 * - Good for understanding the logic step by step
 * - Uses sets to eliminate candidates who trust others
 * - More memory overhead but very clear logic
 * 
 * Solution 4 (HashMap with early termination):
 * - Good for sparse trust relationships
 * - Can potentially terminate early
 * - Uses HashMap which might be overkill for this problem
 * 
 * Solution 5 (Space-efficient):
 * - Similar to Solution 2 but with clearer variable naming
 * - Optimal balance of readability and efficiency
 * 
 * Recommended: Solution 2 or Solution 5 for interviews due to their elegance
 * and efficiency.
 */
