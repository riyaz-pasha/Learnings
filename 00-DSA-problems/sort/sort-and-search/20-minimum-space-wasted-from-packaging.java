/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We have 'n' packages and 'm' suppliers. Each supplier offers an infinite 
 * amount of boxes in various distinct sizes. 
 * We must choose EXACTLY ONE supplier to fulfill all packages. 
 * A package can only fit into a box if boxSize >= packageSize.
 * To minimize waste, each package should be placed into the smallest possible 
 * box that can fit it from the chosen supplier.
 * The total wasted space is the sum of (boxSize - packageSize) for all packages.
 * 
 * Our goal is to find the minimum total wasted space achievable by picking the 
 * best supplier. If no supplier can fit all packages, return -1.
 * Since the answer can be large, return it modulo 10^9 + 7.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can a supplier's box array be unsorted? 
 *    (Yes, we should sort both the packages and each supplier's boxes first).
 * 2. Is it possible that NO supplier has a box large enough for the biggest package?
 *    (Yes, if a supplier's maximum box size is smaller than the maximum package 
 *     size, that supplier is invalid. If all are invalid, return -1).
 * 3. Does the result modulo 10^9+7 apply during the minimum comparison or only at the end?
 *    (Only at the end! If we modulo during comparison, a larger number might wrap 
 *     around to become a falsely "smaller" number. We must use 64-bit integers 
 *     (`long`) to keep track of the absolute minimum sum, and apply modulo at the very end.)
 * 4. Can we optimize the sum calculation?
 *    (Yes! Sum of Wasted Space = Sum of (Box_i - Package_i). 
 *     This can be rewritten as (Sum of Box_i) - (Sum of Package_i). 
 *     Since the sum of packages is constant regardless of the supplier, we only 
 *     need to find the supplier that minimizes the (Sum of Box sizes used).)
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (Acceptable for given constraints N, M <= 50):
 *    - Sort packages. 
 *    - For each supplier, sort their boxes.
 *    - For each package, iterate through the supplier's boxes to find the first 
 *      box that fits. Add to total box sum.
 *    - Time: O(M * N * B), where B is the number of boxes for a supplier.
 * 
 * 2. Binary Search (The Optimal "Expected" Approach for Large Constraints):
 *    - Rewrite the target: Minimize `totalBoxSum` across suppliers.
 *    - Sort the `packages` array. Calculate `totalPackageSum`.
 *    - For each supplier:
 *      - Sort their boxes.
 *      - If their largest box < our largest package, skip this supplier.
 *      - Otherwise, use Binary Search to find how many packages fit into each box.
 *      - For a box of size `B`, find the first package that is strictly greater 
 *        than `B` using an `upperBound` binary search. 
 *      - All packages between our previous search index and this new index will 
 *        use this box `B`. Add `count * B` to this supplier's `totalBoxSum`.
 *      - Track the minimum `totalBoxSum` found across all valid suppliers.
 *    - Finally, Wasted Space = minBoxSum - totalPackageSum.
 *    - Time Complexity: O(N log N + M * K log K + M * K log N) where K is box array length.
 *    - Space Complexity: O(1) auxiliary space (ignoring sort overhead).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: packages = [2, 3, 5], boxes = [[4, 8], [2, 8]]
 * 
 * 1. Sort packages: [2, 3, 5]. PackageSum = 10.
 * 
 * 2. Supplier 1: [4, 8]
 *    - Box = 4: Upper bound in packages for 4 is index 2 (value 5).
 *      Packages fitting in box 4: [2, 3]. Count = 2.
 *      Cost added: 2 * 4 = 8.
 *    - Box = 8: Upper bound in packages for 8 is index 3 (out of bounds).
 *      Packages fitting in box 8: [5]. Count = 1.
 *      Cost added: 1 * 8 = 8.
 *    - Total Box Sum for Supplier 1 = 16.
 * 
 * 3. Supplier 2: [2, 8]
 *    - Box = 2: Upper bound in packages for 2 is index 1 (value 3).
 *      Packages fitting in box 2: [2]. Count = 1.
 *      Cost added: 1 * 2 = 2.
 *    - Box = 8: Upper bound in packages for 8 is index 3.
 *      Packages fitting in box 8: [3, 5]. Count = 2.
 *      Cost added: 2 * 8 = 16.
 *    - Total Box Sum for Supplier 2 = 18.
 * 
 * 4. Min Box Sum = min(16, 18) = 16 (Supplier 1).
 * 5. Minimum Wasted Space = 16 - 10 = 6. 
 *    Return 6 % (10^9 + 7).
 */

import java.util.Arrays;

public class MinimumWastedSpace {

    private static final int MOD = 1_000_000_007;

    public static void main(String[] args) {
        int[] packages = {2, 3, 5};
        int[][] boxes = {{4, 8}, {2, 8}};
        
        System.out.println("Test Case 1: packages = [2,3,5], boxes = [[4,8],[2,8]]");
        System.out.println("Optimal Solution (Binary Search): " + minWastedSpace(packages.clone(), clone2DArray(boxes)));
        System.out.println("--------------------------------------------------");
        
        int[] packages2 = {2, 3, 5};
        int[][] boxes2 = {{1, 4}, {2, 3}, {3, 4}}; // None can fit 5
        System.out.println("Test Case 2: No valid supplier");
        System.out.println("Optimal Solution (Binary Search): " + minWastedSpace(packages2.clone(), clone2DArray(boxes2)));
    }

    /**
     * Optimal Solution: Binary Search (Upper Bound)
     * 
     * Idea: Instead of mapping each package to a box, we map each box to a range 
     * of packages. We use Binary Search to efficiently find how many packages 
     * a specific box can cover.
     * 
     * Time Complexity: O(N log N + M * K log K + M * K log N)
     *                  where N = packages.length, M = number of suppliers, 
     *                  K = max boxes per supplier.
     * Space Complexity: O(1) beyond the space required for in-place sorting.
     */
    public static int minWastedSpace(int[] packages, int[][] boxes) {
        // 1. Sort the packages
        Arrays.sort(packages);
        
        int n = packages.length;
        long totalPackageSum = 0;
        for (int p : packages) {
            totalPackageSum += p;
        }

        long minBoxSum = Long.MAX_VALUE;

        // 2. Evaluate each supplier
        for (int[] supplierBoxes : boxes) {
            // Sort this supplier's boxes
            Arrays.sort(supplierBoxes);
            
            // If the supplier's largest box cannot fit our largest package, 
            // this supplier is entirely invalid.
            if (supplierBoxes[supplierBoxes.length - 1] < packages[n - 1]) {
                continue;
            }
            
            long currentSupplierBoxSum = 0;
            int prevPackageIndex = 0;
            
            // For each box this supplier has
            for (int box : supplierBoxes) {
                // If we've already covered all packages, break early
                if (prevPackageIndex >= n) {
                    break;
                }
                
                // Find the first package that is strictly greater than the current box size
                int currPackageIndex = upperBound(packages, box, prevPackageIndex);
                
                // The number of packages that will use this box
                long count = currPackageIndex - prevPackageIndex;
                
                // Accumulate the total box size used
                currentSupplierBoxSum += count * box;
                
                // Update our starting pointer for the next box
                prevPackageIndex = currPackageIndex;
            }
            
            // Track the absolute minimum box sum across all valid suppliers
            minBoxSum = Math.min(minBoxSum, currentSupplierBoxSum);
        }

        // 3. Return result
        // If minBoxSum is still Long.MAX_VALUE, it means no supplier was valid
        if (minBoxSum == Long.MAX_VALUE) {
            return -1;
        }

        // Calculate waste and apply modulo. 
        // We do (minBoxSum - totalPackageSum) % MOD.
        return (int) ((minBoxSum - totalPackageSum) % MOD);
    }

    /**
     * Custom Binary Search method to find the "Upper Bound".
     * Finds the index of the first element in the sorted array `arr` 
     * that is strictly greater than `target`.
     * 
     * @param arr The sorted array to search.
     * @param target The value to bound.
     * @param start The index to start searching from (optimizes subsequent searches).
     * @return The index of the first element > target. If all are <= target, returns arr.length.
     */
    private static int upperBound(int[] arr, int target, int start) {
        int low = start;
        int high = arr.length - 1;
        int result = arr.length;
        
        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            if (arr[mid] <= target) {
                // Number fits in the box, search to the right for bigger packages
                low = mid + 1;
            } else {
                // Number doesn't fit, mark as potential upper bound and search left
                result = mid;
                high = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * Utility method to deep clone a 2D array for testing purposes, ensuring 
     * in-place modifications (like sorting) don't affect subsequent tests.
     */
    private static int[][] clone2DArray(int[][] original) {
        int[][] cloned = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            cloned[i] = original[i].clone();
        }
        return cloned;
    }
}
