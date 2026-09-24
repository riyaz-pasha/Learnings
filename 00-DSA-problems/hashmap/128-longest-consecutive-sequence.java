import java.util.*;
/*
 * Given an unsorted array of integers nums, return the length of the longest
 * consecutive elements sequence.
 * 
 * You must write an algorithm that runs in O(n) time.
 * 
 * Example 1:
 * Input: nums = [100,4,200,1,3,2]
 * Output: 4
 * Explanation: The longest consecutive elements sequence is [1, 2, 3, 4].
 * Therefore its length is 4.
 * 
 * Example 2:
 * Input: nums = [0,3,7,2,5,8,4,6,0,1]
 * Output: 9
 * 
 * Example 3:
 * Input: nums = [1,0,1,2]
 * Output: 3
 */

class LongestConsecutiveSequence {

    // Solution 1: Optimal HashSet Approach (O(n) time, O(n) space)
    // This is the most efficient and commonly expected solution
    public int longestConsecutive(int[] nums) {
        if (nums.length == 0)
            return 0;

        Set<Integer> numSet = new HashSet<>();
        // Add all numbers to set for O(1) lookup
        for (int num : nums) {
            numSet.add(num);
        }

        int maxLength = 0;

        for (int num : numSet) {
            // Only start counting from the beginning of a sequence
            // If num-1 exists, then num is not the start of a sequence
            if (!numSet.contains(num - 1)) {
                int currentNum = num;
                int currentLength = 1;

                // Count consecutive numbers
                while (numSet.contains(currentNum + 1)) {
                    currentNum++;
                    currentLength++;
                }

                maxLength = Math.max(maxLength, currentLength);
            }
        }

        return maxLength;
    }

    // Solution 2: HashMap with memoization (O(n) time, O(n) space)
    // Tracks the length of sequence ending at each number
    public int longestConsecutiveHashMap(int[] nums) {
        if (nums.length == 0)
            return 0;

        Map<Integer, Integer> map = new HashMap<>();
        int maxLength = 0;

        for (int num : nums) {
            if (!map.containsKey(num)) {
                // Check lengths of adjacent sequences
                int leftLength = map.getOrDefault(num - 1, 0);
                int rightLength = map.getOrDefault(num + 1, 0);

                // Current sequence length
                int currentLength = leftLength + rightLength + 1;

                // Update the boundaries of the sequence
                map.put(num, currentLength);
                map.put(num - leftLength, currentLength);
                map.put(num + rightLength, currentLength);

                maxLength = Math.max(maxLength, currentLength);
            }
        }

        return maxLength;
    }

    // Solution 3: Union-Find approach (O(n) amortized time, O(n) space)
    public int longestConsecutiveUnionFind(int[] nums) {
        if (nums.length == 0)
            return 0;

        UnionFind uf = new UnionFind();
        Set<Integer> numSet = new HashSet<>();

        // Add all numbers to set and union find
        for (int num : nums) {
            if (!numSet.contains(num)) {
                numSet.add(num);
                uf.add(num);

                // Union with adjacent numbers if they exist
                if (numSet.contains(num - 1)) {
                    uf.union(num, num - 1);
                }
                if (numSet.contains(num + 1)) {
                    uf.union(num, num + 1);
                }
            }
        }

        return uf.getMaxComponentSize();
    }

    // Helper class for Union-Find
    class UnionFind {
        private Map<Integer, Integer> parent;
        private Map<Integer, Integer> size;

        public UnionFind() {
            parent = new HashMap<>();
            size = new HashMap<>();
        }

        public void add(int x) {
            if (!parent.containsKey(x)) {
                parent.put(x, x);
                size.put(x, 1);
            }
        }

        public int find(int x) {
            if (parent.get(x) != x) {
                parent.put(x, find(parent.get(x))); // Path compression
            }
            return parent.get(x);
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                // Union by size
                if (size.get(rootX) < size.get(rootY)) {
                    parent.put(rootX, rootY);
                    size.put(rootY, size.get(rootX) + size.get(rootY));
                } else {
                    parent.put(rootY, rootX);
                    size.put(rootX, size.get(rootX) + size.get(rootY));
                }
            }
        }

        public int getMaxComponentSize() {
            return size.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
    }

    // Solution 4: Sorting approach (O(n log n) time, O(1) space)
    // Not optimal time complexity but included for comparison
    public int longestConsecutiveSorting(int[] nums) {
        if (nums.length == 0)
            return 0;

        Arrays.sort(nums);

        int maxLength = 1;
        int currentLength = 1;

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == nums[i - 1]) {
                // Skip duplicates
                continue;
            } else if (nums[i] == nums[i - 1] + 1) {
                // Consecutive number
                currentLength++;
            } else {
                // Reset sequence
                maxLength = Math.max(maxLength, currentLength);
                currentLength = 1;
            }
        }

        return Math.max(maxLength, currentLength);
    }

    // Debug version to show the process
    public int longestConsecutiveDebug(int[] nums) {
        System.out.println("Input array: " + Arrays.toString(nums));

        if (nums.length == 0)
            return 0;

        Set<Integer> numSet = new HashSet<>();
        for (int num : nums) {
            numSet.add(num);
        }
        System.out.println("Unique numbers: " + numSet);

        int maxLength = 0;
        List<List<Integer>> sequences = new ArrayList<>();

        for (int num : numSet) {
            if (!numSet.contains(num - 1)) {
                System.out.println("\nStarting sequence from: " + num);
                List<Integer> currentSequence = new ArrayList<>();
                int currentNum = num;
                int currentLength = 1;
                currentSequence.add(currentNum);

                while (numSet.contains(currentNum + 1)) {
                    currentNum++;
                    currentLength++;
                    currentSequence.add(currentNum);
                    System.out.println("  Extended to: " + currentSequence);
                }

                sequences.add(new ArrayList<>(currentSequence));
                System.out.println("Final sequence: " + currentSequence + " (length: " + currentLength + ")");

                if (currentLength > maxLength) {
                    maxLength = currentLength;
                    System.out.println("New max length: " + maxLength);
                }
            }
        }

        System.out.println("\nAll consecutive sequences found: " + sequences);
        System.out.println("Longest sequence length: " + maxLength);

        return maxLength;
    }

    // Test method with comprehensive test cases
    public static void main(String[] args) {
        LongestConsecutiveSequence solution = new LongestConsecutiveSequence();

        System.out.println("=== Example Test Cases ===\n");

        // Example 1
        int[] nums1 = { 100, 4, 200, 1, 3, 2 };
        System.out.println("Test 1: " + Arrays.toString(nums1));
        System.out.println("Result: " + solution.longestConsecutive(nums1));
        System.out.println("Expected: 4\n");

        // Example 2
        int[] nums2 = { 0, 3, 7, 2, 5, 8, 4, 6, 0, 1 };
        System.out.println("Test 2: " + Arrays.toString(nums2));
        System.out.println("Result: " + solution.longestConsecutive(nums2));
        System.out.println("Expected: 9\n");

        // Example 3
        int[] nums3 = { 1, 0, 1, 2 };
        System.out.println("Test 3: " + Arrays.toString(nums3));
        System.out.println("Result: " + solution.longestConsecutive(nums3));
        System.out.println("Expected: 3\n");

        // Additional test cases
        System.out.println("=== Additional Test Cases ===\n");

        // Empty array
        int[] nums4 = {};
        System.out.println("Test 4 (empty): " + Arrays.toString(nums4));
        System.out.println("Result: " + solution.longestConsecutive(nums4));
        System.out.println("Expected: 0\n");

        // Single element
        int[] nums5 = { 5 };
        System.out.println("Test 5 (single): " + Arrays.toString(nums5));
        System.out.println("Result: " + solution.longestConsecutive(nums5));
        System.out.println("Expected: 1\n");

        // All duplicates
        int[] nums6 = { 1, 1, 1, 1 };
        System.out.println("Test 6 (duplicates): " + Arrays.toString(nums6));
        System.out.println("Result: " + solution.longestConsecutive(nums6));
        System.out.println("Expected: 1\n");

        // Negative numbers
        int[] nums7 = { -1, -2, -3, 1, 2, 3 };
        System.out.println("Test 7 (negative): " + Arrays.toString(nums7));
        System.out.println("Result: " + solution.longestConsecutive(nums7));
        System.out.println("Expected: 3\n");

        // Large gap
        int[] nums8 = { 1, 2, 3, 100, 101, 102, 103, 104 };
        System.out.println("Test 8 (large gap): " + Arrays.toString(nums8));
        System.out.println("Result: " + solution.longestConsecutive(nums8));
        System.out.println("Expected: 5\n");

        // Debug example
        System.out.println("=== Debug Example ===");
        solution.longestConsecutiveDebug(nums1);

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int[] largeArray = new int[100000];
        Random rand = new Random(42); // Fixed seed for reproducibility
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(50000); // Create some consecutive sequences
        }

        long startTime, endTime;

        // HashSet approach (optimal)
        startTime = System.nanoTime();
        int result1 = solution.longestConsecutive(largeArray);
        endTime = System.nanoTime();
        System.out.printf("HashSet approach: %d (Time: %.2f ms)\n",
                result1, (endTime - startTime) / 1_000_000.0);

        // HashMap approach
        startTime = System.nanoTime();
        int result2 = solution.longestConsecutiveHashMap(largeArray);
        endTime = System.nanoTime();
        System.out.printf("HashMap approach: %d (Time: %.2f ms)\n",
                result2, (endTime - startTime) / 1_000_000.0);

        // Sorting approach (for comparison)
        startTime = System.nanoTime();
        int result3 = solution.longestConsecutiveSorting(largeArray);
        endTime = System.nanoTime();
        System.out.printf("Sorting approach: %d (Time: %.2f ms)\n",
                result3, (endTime - startTime) / 1_000_000.0);

        System.out.println("\n=== Algorithm Summary ===");
        System.out.println("1. HashSet (RECOMMENDED): O(n) time, O(n) space - Most efficient");
        System.out.println("2. HashMap: O(n) time, O(n) space - Alternative O(n) approach");
        System.out.println("3. Union-Find: O(n) amortized time, O(n) space - Advanced technique");
        System.out.println("4. Sorting: O(n log n) time, O(1) space - Simple but slower");
    }

}
