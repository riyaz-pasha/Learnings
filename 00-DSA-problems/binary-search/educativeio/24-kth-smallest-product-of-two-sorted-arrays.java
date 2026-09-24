import java.util.*;

/**
 * ================================================================
 * 🔥 K-th Smallest Product of Two Sorted Arrays
 * ================================================================
 *
 * IDEA:
 * Binary Search on Answer (product value)
 *
 * For a candidate "mid", count how many pairs have:
 *      nums1[i] * nums2[j] <= mid
 *
 * If count >= k → mid is a valid answer → move LEFT
 * Else → move RIGHT
 *
 * ================================================================
 */
class KthSmallestProduct {

    public static long kthSmallestProduct(int[] nums1, int[] nums2, long k) {

        long low = -10_000_000_000L;   // minimum possible product
        long high = 10_000_000_000L;   // maximum possible product

        long answer = 0; // 🔥 explicit answer tracking (your preferred style)

        while (low <= high) {

            long mid = low + (high - low) / 2;

            long count = countPairs(nums1, nums2, mid);

            // 🔥 Decision Boundary (First TRUE pattern)
            if (count >= k) {
                answer = mid;  // possible answer
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return answer;
    }

    /**
     * Counts number of pairs such that:
     *      nums1[i] * nums2[j] <= target
     *
     * Uses binary search for each element in nums1
     *
     * Time: O(n log m)
     */
    private static long countPairs(int[] nums1, int[] nums2, long target) {

        long count = 0;

        for (int a : nums1) {

            if (a > 0) {
                // a * b <= target → b <= target / a
                long limit = target / a;

                int validCount = upperBound(nums2, limit);
                count += validCount;

            } else if (a < 0) {
                // a * b <= target → b >= ceil(target / a)
                long limit = ceilDiv(target, a);

                int idx = lowerBound(nums2, limit);
                count += (nums2.length - idx);

            } else {
                // a == 0
                if (target >= 0) {
                    count += nums2.length;
                }
            }
        }

        return count;
    }

    /**
     * First index where nums[idx] >= target
     */
    private static int lowerBound(int[] nums, long target) {
        int low = 0, high = nums.length - 1;
        int answerIndex = nums.length;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] >= target) {
                answerIndex = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return answerIndex;
    }

    /**
     * First index where nums[idx] > target
     * So count = index
     */
    private static int upperBound(int[] nums, long target) {
        int low = 0, high = nums.length - 1;
        int answerIndex = nums.length;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] > target) {
                answerIndex = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return answerIndex;
    }

    /**
     * Proper ceil division handling negatives
     */
    private static long ceilDiv(long a, long b) {
        long res = a / b;
        if (a % b != 0 && ((a ^ b) > 0)) {
            res++;
        }
        return res;
    }
}

/**
 * Problem Statement:
 * Given two sorted 0-indexed integer arrays nums1 and nums2, and an integer k.
 * Return the k-th smallest product among all pairs nums1[i] * nums2[j].
 * 
 * Constraints:
 * - 1 <= nums1.length, nums2.length <= 5 * 10^4
 * - -10^5 <= nums1[i], nums2[j] <= 10^5
 * - 1 <= k <= nums1.length * nums2.length
 * - Arrays are sorted in non-decreasing order.
 */
class KthSmallestProduct {

    /**
     * Helper Method: Counts how many products (nums1[i] * nums2[j]) are <= target.
     * 
     * LOGIC & VISUAL EXPLANATION:
     * We iterate through `nums1` and use Binary Search on `nums2`.
     * Because `nums1[i]` can be negative, zero, or positive, the behavior of multiplication changes:
     * 
     * Case 1: nums1[i] > 0
     * Multiplying by a positive number preserves order.
     * We want nums1[i] * nums2[j] <= target  =>  nums2[j] <= target / nums1[i].
     * We binary search for the LAST element in nums2 that satisfies this.
     * 
     * Case 2: nums1[i] < 0
     * Multiplying by a negative number flips the inequality!
     * We want nums1[i] * nums2[j] <= target.
     * Since nums2 is sorted ascending, multiplying it by a negative makes it descending.
     * We binary search for the FIRST element in nums2 where the product is <= target.
     * 
     * Case 3: nums1[i] == 0
     * The product is exactly 0. 
     * If target >= 0, all elements in nums2 satisfy 0 <= target.
     * If target < 0, no elements satisfy 0 <= target.
     */
    private static long countPairsLessOrEqual(int[] nums1, int[] nums2, long target) {
        long count = 0;
        
        for (long x : nums1) {
            if (x > 0) {
                // Positive x: find the last index 'mid' where x * nums2[mid] <= target
                int low = 0;
                int high = nums2.length - 1;
                int result = -1; // Defaults to -1 (no elements valid)

                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (x * nums2[mid] <= target) {
                        result = mid;  // Valid, save it and search right for a larger valid index
                        low = mid + 1;
                    } else {
                        high = mid - 1;
                    }
                }
                count += (result + 1);

            } else if (x < 0) {
                // Negative x: find the first index 'mid' where x * nums2[mid] <= target
                int low = 0;
                int high = nums2.length - 1;
                int result = nums2.length; // Defaults to out-of-bounds (no elements valid)

                while (low <= high) {
                    int mid = low + (high - low) / 2;
                    if (x * nums2[mid] <= target) {
                        result = mid;   // Valid, save it and search left for a smaller valid index
                        high = mid - 1;
                    } else {
                        low = mid + 1;
                    }
                }
                count += (nums2.length - result);

            } else {
                // x == 0
                if (target >= 0) {
                    count += nums2.length;
                }
            }
        }
        
        return count;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(M * log N * log(MaxProduct - MinProduct))
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * The absolute minimum product and absolute maximum product can only be formed by 
     * the extremes of the two arrays (the first and last elements).
     * We binary search the range [min_product, max_product].
     * For a given `mid` product, we count how many pairs are <= `mid`.
     * If count >= k, then `mid` is a potential answer, and we search for smaller ones.
     */
    public static long kthSmallestProductIterativeBS(int[] nums1, int[] nums2, long k) {
        int m = nums1.length;
        int n = nums2.length;
        
        // Find boundaries of our answer space
        long p1 = (long) nums1[0] * nums2[0];
        long p2 = (long) nums1[0] * nums2[n - 1];
        long p3 = (long) nums1[m - 1] * nums2[0];
        long p4 = (long) nums1[m - 1] * nums2[n - 1];
        
        long low = Math.min(Math.min(p1, p2), Math.min(p3, p4));
        long high = Math.max(Math.max(p1, p2), Math.max(p3, p4));
        
        long result = high; // Explicit result variable initialized to max boundary

        while (low <= high) {
            long mid = low + (high - low) / 2;
            
            if (countPairsLessOrEqual(nums1, nums2, mid) >= k) {
                // There are at least k pairs <= mid, meaning the k-th smallest is <= mid.
                result = mid; // Save it as a potential answer
                high = mid - 1;
            } else {
                // There are fewer than k pairs <= mid, so we need a larger product
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search on Answer Space
     * 
     * Time Complexity: O(M * log N * log(Range))
     * Space Complexity: O(log(Range)) - Call stack overhead.
     * 
     * EXPLANATION:
     * Translates the optimal iterative approach into a recursive implementation, 
     * explicitly propagating the `result` back up the call stack.
     */
    public static long kthSmallestProductRecursiveWrapper(int[] nums1, int[] nums2, long k) {
        int m = nums1.length;
        int n = nums2.length;
        
        long p1 = (long) nums1[0] * nums2[0];
        long p2 = (long) nums1[0] * nums2[n - 1];
        long p3 = (long) nums1[m - 1] * nums2[0];
        long p4 = (long) nums1[m - 1] * nums2[n - 1];
        
        long low = Math.min(Math.min(p1, p2), Math.min(p3, p4));
        long high = Math.max(Math.max(p1, p2), Math.max(p3, p4));
        
        return kthSmallestProductRecursiveBS(nums1, nums2, k, low, high, high);
    }

    private static long kthSmallestProductRecursiveBS(int[] nums1, int[] nums2, long k, long low, long high, long currentResult) {
        long result = currentResult; // Explicitly track result

        if (low > high) {
            return result; // Base case: search space exhausted
        }

        long mid = low + (high - low) / 2;

        if (countPairsLessOrEqual(nums1, nums2, mid) >= k) {
            // Valid upper bound for k-th element, store it and search lower
            result = kthSmallestProductRecursiveBS(nums1, nums2, k, low, mid - 1, mid);
        } else {
            // Not enough elements, search higher
            result = kthSmallestProductRecursiveBS(nums1, nums2, k, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Brute Force with Sorting (Sub-optimal, only for small arrays)
     * 
     * Time Complexity: O((M * N) log (M * N))
     * Space Complexity: O(M * N)
     * 
     * EXPLANATION:
     * Calculates every single product, stores them in an array, sorts the array, 
     * and picks the (k-1)th element. 
     * NOTE: Will result in Memory Limit Exceeded (MLE) or Time Limit Exceeded (TLE) 
     * for arrays of size 50,000, but proves the correctness mathematically.
     */
    public static long kthSmallestProductBruteForce(int[] nums1, int[] nums2, long k) {
        long[] allProducts = new long[nums1.length * nums2.length];
        int index = 0;
        
        for (int x : nums1) {
            for (int y : nums2) {
                allProducts[index++] = (long) x * y;
            }
        }
        
        Arrays.sort(allProducts);
        return allProducts[(int) (k - 1)];
    }

    /**
     * SOLUTION 4: Java Streams (Functional Brute Force)
     * 
     * Time Complexity: O((M * N) log (M * N))
     * Space Complexity: O(M * N)
     * 
     * EXPLANATION:
     * Beautiful and highly concise functional application of the brute force logic.
     */
    public static long kthSmallestProductStream(int[] nums1, int[] nums2, long k) {
        return Arrays.stream(nums1)
                .asLongStream()
                .flatMap(x -> Arrays.stream(nums2).asLongStream().map(y -> x * y))
                .sorted()
                .skip(k - 1)
                .findFirst()
                .orElse(-1);
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[] nums1, int[] nums2, long k, long expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on standard logic, boundaries, and negatives
        TestCase[] testCases = {
            new TestCase(new int[]{2, 5}, new int[]{3, 4}, 2, 8),            // Simple positive case
            new TestCase(new int[]{-4, -2, 0, 3}, new int[]{2, 4}, 6, 0),    // Mixed signs
            new TestCase(new int[]{-2, -1, 0, 1, 2}, new int[]{-3, -1, 2, 4}, 3, -6), // Heavy mixed signs
            new TestCase(new int[]{0, 0}, new int[]{0, 0}, 1, 0),            // All zeros
            new TestCase(new int[]{-10, -5}, new int[]{-10, -5}, 1, 25)      // Two negative arrays (flips to positive)
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            long resIterativeBS = kthSmallestProductIterativeBS(tc.nums1(), tc.nums2(), tc.k());
            long resRecursiveBS = kthSmallestProductRecursiveWrapper(tc.nums1(), tc.nums2(), tc.k());
            
            // Execute brute force methods only for small tests to keep test suite fast
            long resBruteForce = kthSmallestProductBruteForce(tc.nums1(), tc.nums2(), tc.k());
            long resStream     = kthSmallestProductStream(tc.nums1(), tc.nums2(), tc.k());

            boolean passed = (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resBruteForce == tc.expected()) &&
                             (resStream == tc.expected());

            // Limit array printing length for neat terminal output
            String arr1Str = Arrays.toString(tc.nums1());
            String arr2Str = Arrays.toString(tc.nums2());
            if (arr1Str.length() > 15) arr1Str = arr1Str.substring(0, 12) + "...]";
            if (arr2Str.length() > 15) arr2Str = arr2Str.substring(0, 12) + "...]";

            System.out.printf("Test %d | k: %-2d | nums1: %-15s | nums2: %-15s -> Expected: %-3d | Passed: %b%n",
                    i + 1, tc.k(), arr1Str, arr2Str, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] IterBS: %d, RecBS: %d, Brute: %d, Stream: %d%n",
                        resIterativeBS, resRecursiveBS, resBruteForce, resStream);
            }
        }
    }
}
