
import java.util.Arrays;

/*
 * Assume you are an awesome parent and want to give your children some cookies.
 * But, you should give each child at most one cookie.
 * 
 * Each child i has a greed factor g[i], which is the minimum size of a cookie
 * that the child will be content with; and each cookie j has a size s[j]. If
 * s[j] >= g[i], we can assign the cookie j to the child i, and the child i will
 * be content. Your goal is to maximize the number of your content children and
 * output the maximum number.
 * 
 * Example 1:
 * Input: g = [1,2,3], s = [1,1]
 * Output: 1
 * Explanation: You have 3 children and 2 cookies. The greed factors of 3
 * children are 1, 2, 3.
 * And even though you have 2 cookies, since their size is both 1, you could
 * only make the child whose greed factor is 1 content.
 * You need to output 1.
 * 
 * Example 2:
 * Input: g = [1,2], s = [1,2,3]
 * Output: 2
 * Explanation: You have 2 children and 3 cookies. The greed factors of 2
 * children are 1, 2.
 * You have 3 cookies and their sizes are big enough to gratify all of the
 * children,
 * You need to output 2.
 */

class AssignCookies {


    public int findContentChildren(int[] g, int[] s) {
        Arrays.sort(g);
        Arrays.sort(s);

        Integer[][] memo = new Integer[g.length][s.length];
        return helper(memo, g, s, 0, 0);
    }

    private int helper(Integer[][] memo, int[] g, int[] s, int greedIndex, int sizeIndex) {
        if (sizeIndex >= s.length || greedIndex >= g.length) {
            return 0;
        }
        if (memo[greedIndex][sizeIndex] != null) {
            return memo[greedIndex][sizeIndex];
        }

        int result = 0;
        if (s[sizeIndex] >= g[greedIndex]) {
            result = Math.max(result, 1 + helper(memo, g, s, greedIndex + 1, sizeIndex + 1));
        }
        result = Math.max(result, helper(memo, g, s, greedIndex, sizeIndex + 1));
        return memo[greedIndex][sizeIndex] = result;
    }

}

class Solution {
    
    /**
     * Greedy approach: Satisfy least greedy children first with smallest adequate cookies
     * Time Complexity: O(n log n + m log m) where n = children, m = cookies
     * Space Complexity: O(1) excluding sorting space
     */
    public int findContentChildren(int[] g, int[] s) {
        // Sort both arrays
        Arrays.sort(g); // Sort children by greed factor
        Arrays.sort(s); // Sort cookies by size
        
        int childIndex = 0;    // Pointer for children
        int cookieIndex = 0;   // Pointer for cookies
        int contentChildren = 0;
        
        // Try to satisfy each child with available cookies
        while (childIndex < g.length && cookieIndex < s.length) {
            // If current cookie can satisfy current child
            if (s[cookieIndex] >= g[childIndex]) {
                contentChildren++;  // Child is content
                childIndex++;       // Move to next child
            }
            // Move to next cookie (either way)
            cookieIndex++;
        }
        
        return contentChildren;
    }
    
    /**
     * Alternative implementation with more explicit logic
     */
    public int findContentChildrenVerbose(int[] g, int[] s) {
        Arrays.sort(g);
        Arrays.sort(s);
        
        int satisfied = 0;
        int cookieIdx = 0;
        
        // For each child (starting from least greedy)
        for (int childIdx = 0; childIdx < g.length; childIdx++) {
            // Find a cookie that can satisfy this child
            while (cookieIdx < s.length) {
                if (s[cookieIdx] >= g[childIdx]) {
                    // Found a suitable cookie
                    satisfied++;
                    cookieIdx++; // Use this cookie
                    break;
                }
                cookieIdx++; // Try next cookie
            }
            
            // If no more cookies available, break
            if (cookieIdx >= s.length) {
                break;
            }
        }
        
        return satisfied;
    }
    
    /**
     * Dry run explanation for Example 1
     */
    public void dryRunExample1() {
        int[] g = {1, 2, 3};
        int[] s = {1, 1};
        
        System.out.println("=== Example 1 Dry Run ===");
        System.out.println("Children greed: " + Arrays.toString(g));
        System.out.println("Cookie sizes: " + Arrays.toString(s));
        System.out.println();
        
        Arrays.sort(g);
        Arrays.sort(s);
        
        System.out.println("After sorting:");
        System.out.println("Children: " + Arrays.toString(g));
        System.out.println("Cookies: " + Arrays.toString(s));
        System.out.println();
        
        int childIdx = 0, cookieIdx = 0, content = 0;
        
        System.out.println("Matching process:");
        while (childIdx < g.length && cookieIdx < s.length) {
            System.out.printf("Child[%d] needs %d, Cookie[%d] has %d -> ",
                childIdx, g[childIdx], cookieIdx, s[cookieIdx]);
            
            if (s[cookieIdx] >= g[childIdx]) {
                content++;
                System.out.println("✓ Satisfied!");
                childIdx++;
            } else {
                System.out.println("✗ Too small");
            }
            cookieIdx++;
        }
        
        System.out.println("\nResult: " + content + " content children\n");
    }
    
    /**
     * Dry run explanation for Example 2
     */
    public void dryRunExample2() {
        int[] g = {1, 2};
        int[] s = {1, 2, 3};
        
        System.out.println("=== Example 2 Dry Run ===");
        System.out.println("Children greed: " + Arrays.toString(g));
        System.out.println("Cookie sizes: " + Arrays.toString(s));
        System.out.println();
        
        Arrays.sort(g);
        Arrays.sort(s);
        
        System.out.println("After sorting:");
        System.out.println("Children: " + Arrays.toString(g));
        System.out.println("Cookies: " + Arrays.toString(s));
        System.out.println();
        
        int childIdx = 0, cookieIdx = 0, content = 0;
        
        System.out.println("Matching process:");
        while (childIdx < g.length && cookieIdx < s.length) {
            System.out.printf("Child[%d] needs %d, Cookie[%d] has %d -> ",
                childIdx, g[childIdx], cookieIdx, s[cookieIdx]);
            
            if (s[cookieIdx] >= g[childIdx]) {
                content++;
                System.out.println("✓ Satisfied!");
                childIdx++;
            } else {
                System.out.println("✗ Too small");
            }
            cookieIdx++;
        }
        
        System.out.println("\nResult: " + content + " content children\n");
    }
    
    // Test cases
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        // Run dry runs first
        sol.dryRunExample1();
        sol.dryRunExample2();
        
        // Test case 1
        int[] g1 = {1, 2, 3};
        int[] s1 = {1, 1};
        System.out.println("Test 1: " + sol.findContentChildren(g1, s1)); // Output: 1
        
        // Test case 2
        int[] g2 = {1, 2};
        int[] s2 = {1, 2, 3};
        System.out.println("Test 2: " + sol.findContentChildren(g2, s2)); // Output: 2
        
        // Test case 3: No cookies
        int[] g3 = {1, 2, 3};
        int[] s3 = {};
        System.out.println("Test 3: " + sol.findContentChildren(g3, s3)); // Output: 0
        
        // Test case 4: All cookies too small
        int[] g4 = {5, 10, 15};
        int[] s4 = {1, 2, 3};
        System.out.println("Test 4: " + sol.findContentChildren(g4, s4)); // Output: 0
        
        // Test case 5: More cookies than children
        int[] g5 = {1, 2};
        int[] s5 = {1, 1, 1, 2, 2, 2};
        System.out.println("Test 5: " + sol.findContentChildren(g5, s5)); // Output: 2
        
        // Test case 6: Exact match
        int[] g6 = {1, 2, 3};
        int[] s6 = {1, 2, 3};
        System.out.println("Test 6: " + sol.findContentChildren(g6, s6)); // Output: 3
        
        // Test case 7: All children very greedy
        int[] g7 = {10, 9, 8, 7};
        int[] s7 = {5, 6, 7, 8};
        System.out.println("Test 7: " + sol.findContentChildren(g7, s7)); // Output: 2
    }
}