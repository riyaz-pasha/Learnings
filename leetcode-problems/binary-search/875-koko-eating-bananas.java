class KokoEatingBananas {

    // Approach 1: Binary Search on Speed (Optimal)
    // Time: O(n * log(max)), Space: O(1)
    public int minEatingSpeed1(int[] piles, int h) {
        int left = 1;
        int right = getMax(piles);

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canFinish(piles, h, mid)) {
                // Can finish with speed mid, try slower
                right = mid;
            } else {
                // Cannot finish, need faster speed
                left = mid + 1;
            }
        }

        return left;
    }

    // Helper: Check if Koko can finish with speed k
    private boolean canFinish(int[] piles, int h, int k) {
        long hours = 0;
        for (int pile : piles) {
            // Ceiling division: how many hours to eat this pile
            hours += (pile + k - 1) / k;
            // Early termination if already exceeded h
            if (hours > h)
                return false;
        }
        return hours <= h;
    }

    // Helper: Find maximum pile size
    private int getMax(int[] piles) {
        int max = piles[0];
        for (int pile : piles) {
            max = Math.max(max, pile);
        }
        return max;
    }

    // Approach 2: Binary Search with Math.ceil
    // Time: O(n * log(max)), Space: O(1)
    public int minEatingSpeed2(int[] piles, int h) {
        int left = 1;
        int right = 1_000_000_000; // or getMax(piles)

        int result = right;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (canFinishWithCeil(piles, h, mid)) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    private boolean canFinishWithCeil(int[] piles, int h, int k) {
        long hours = 0;
        for (int pile : piles) {
            hours += Math.ceil((double) pile / k);
            if (hours > h)
                return false;
        }
        return true;
    }

    // Approach 3: Binary Search with Optimized Range
    // Time: O(n * log(max)), Space: O(1)
    public int minEatingSpeed3(int[] piles, int h) {
        // Optimization: minimum speed is total bananas / h (rounded up)
        long totalBananas = 0;
        int maxPile = 0;

        for (int pile : piles) {
            totalBananas += pile;
            maxPile = Math.max(maxPile, pile);
        }

        // Minimum possible speed (if she can spread eating evenly)
        int left = (int) ((totalBananas + h - 1) / h);
        int right = maxPile;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canFinish(piles, h, mid)) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    // Approach 4: Binary Search with Detailed Comments
    // Time: O(n * log(max)), Space: O(1)
    public int minEatingSpeed4(int[] piles, int h) {
        // Search space: [1, max pile size]
        // Why max pile? Because eating at max pile speed means
        // each pile takes at most 1 hour
        int left = 1;
        int right = getMax(piles);

        // Binary search to find minimum valid speed
        while (left < right) {
            int mid = left + (right - left) / 2;
            long totalHours = calculateHours(piles, mid);

            if (totalHours <= h) {
                // Can finish in time, try slower speed
                right = mid;
            } else {
                // Too slow, need faster speed
                left = mid + 1;
            }
        }

        return left;
    }

    private long calculateHours(int[] piles, int speed) {
        long hours = 0;
        for (int pile : piles) {
            // For each pile, calculate hours needed
            // If pile = 10, speed = 3: takes ceil(10/3) = 4 hours
            hours += (pile + speed - 1) / speed;
        }
        return hours;
    }

    // Approach 5: Linear Search (Brute Force - Not Optimal)
    // Time: O(n * max), Space: O(1)
    public int minEatingSpeed5(int[] piles, int h) {
        int maxPile = getMax(piles);

        // Try each speed from 1 to max
        for (int speed = 1; speed <= maxPile; speed++) {
            if (canFinish(piles, h, speed)) {
                return speed;
            }
        }

        return maxPile;
    }

    // Test cases with detailed output
    public static void main(String[] args) {
        KokoEatingBananas solution = new KokoEatingBananas();

        // Test Case 1
        int[] piles1 = { 3, 6, 7, 11 };
        int h1 = 8;
        System.out.println("Test 1: " + solution.minEatingSpeed1(piles1, h1)); // Output: 4
        explainSolution(piles1, h1, 4);

        // Test Case 2
        int[] piles2 = { 30, 11, 23, 4, 20 };
        int h2 = 5;
        System.out.println("\nTest 2: " + solution.minEatingSpeed1(piles2, h2)); // Output: 30
        explainSolution(piles2, h2, 30);

        // Test Case 3
        int[] piles3 = { 30, 11, 23, 4, 20 };
        int h3 = 6;
        System.out.println("\nTest 3: " + solution.minEatingSpeed1(piles3, h3)); // Output: 23
        explainSolution(piles3, h3, 23);

        // Test Case 4: Single pile
        int[] piles4 = { 1000000000 };
        int h4 = 2;
        System.out.println("\nTest 4: " + solution.minEatingSpeed1(piles4, h4)); // Output: 500000000

        // Test Case 5: Many small piles
        int[] piles5 = { 1, 1, 1, 1, 1 };
        int h5 = 5;
        System.out.println("Test 5: " + solution.minEatingSpeed1(piles5, h5)); // Output: 1
    }

    private static void explainSolution(int[] piles, int h, int speed) {
        System.out.println("Piles: " + java.util.Arrays.toString(piles));
        System.out.println("Hours available: " + h);
        System.out.println("Speed: " + speed + " bananas/hour");
        System.out.println("Breakdown:");

        int totalHours = 0;
        for (int i = 0; i < piles.length; i++) {
            int hoursNeeded = (piles[i] + speed - 1) / speed;
            totalHours += hoursNeeded;
            System.out.printf("  Pile %d (%d bananas): %d hours%n",
                    i + 1, piles[i], hoursNeeded);
        }
        System.out.println("Total hours: " + totalHours);
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM ANALYSIS:
 * - Koko needs to eat all bananas within h hours
 * - She picks one pile per hour and eats k bananas from it
 * - If pile < k, she eats all but doesn't eat more that hour
 * - Find minimum k to finish all piles within h hours
 * 
 * KEY INSIGHT: Binary Search on Answer
 * Instead of searching through piles, search through possible speeds!
 * - Minimum speed: 1 banana/hour
 * - Maximum speed: max(piles) - any pile can be finished in 1 hour
 * - If speed k works, any speed > k also works (monotonic property)
 * - We want the MINIMUM speed that works
 * 
 * ALGORITHM:
 * 1. Binary search on speed k in range [1, max(piles)]
 * 2. For each speed, check if Koko can finish in h hours
 * 3. If yes, try slower speed (search left)
 * 4. If no, try faster speed (search right)
 * 
 * TIME CALCULATION FOR SPEED k:
 * For each pile p, hours needed = ceil(p/k) = (p + k - 1) / k
 * - Example: pile=10, k=3 → ceil(10/3) = 4 hours
 * - Sum up hours for all piles and check if ≤ h
 * 
 * EXAMPLE WALKTHROUGH: piles=[3,6,7,11], h=8
 * 
 * Initial range: [1, 11]
 * 
 * Iteration 1: mid=6
 * - Pile 3: ceil(3/6)=1 hour
 * - Pile 6: ceil(6/6)=1 hour
 * - Pile 7: ceil(7/6)=2 hours
 * - Pile 11: ceil(11/6)=2 hours
 * - Total: 6 hours ≤ 8 ✓
 * - Try slower: right=6
 * 
 * Iteration 2: left=1, right=6, mid=3
 * - Pile 3: 1 hour
 * - Pile 6: 2 hours
 * - Pile 7: 3 hours
 * - Pile 11: 4 hours
 * - Total: 10 hours > 8 ✗
 * - Need faster: left=4
 * 
 * Iteration 3: left=4, right=6, mid=5
 * - Total: 7 hours ≤ 8 ✓
 * - Try slower: right=5
 * 
 * Iteration 4: left=4, right=5, mid=4
 * - Pile 3: 1 hour
 * - Pile 6: 2 hours
 * - Pile 7: 2 hours
 * - Pile 11: 3 hours
 * - Total: 8 hours ≤ 8 ✓
 * - Try slower: right=4
 * 
 * Result: left=4, right=4 → Answer: 4
 * 
 * COMPLEXITY ANALYSIS:
 * - Time: O(n * log(max)) where n = number of piles, max = largest pile
 * - Binary search: log(max) iterations
 * - Each iteration: O(n) to check all piles
 * - Space: O(1) - only use constant extra space
 * 
 * EDGE CASES:
 * 1. h = number of piles: must eat fastest pile in 1 hour → answer = max(piles)
 * 2. h >> number of piles: can eat very slowly → answer = 1 or close to 1
 * 3. Single pile: answer = ceil(pile / h)
 * 4. All piles same size: answer = ceil(pile_size / (h / n))
 * 
 * WHY BINARY SEARCH WORKS:
 * The key property is MONOTONICITY:
 * - If speed k allows finishing in h hours, speed k+1 also allows it
 * - If speed k doesn't allow finishing, speed k-1 also doesn't
 * This creates a pattern: [NO, NO, ..., NO, YES, YES, ..., YES]
 * We want the first YES, which binary search finds efficiently!
 */
