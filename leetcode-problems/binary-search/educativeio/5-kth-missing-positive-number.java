import java.util.*;

/**
 * ================================================================
 * 🔥 Kth Missing Positive — ULTIMATE INTERVIEW FILE
 * ================================================================
 *
 * This file is designed for:
 *   ✔ Deep understanding
 *   ✔ Interview recall
 *   ✔ Debugging clarity
 *
 * ---------------------------------------------------------------
 * 🧠 PROBLEM RECALL (ALWAYS START HERE IN INTERVIEW)
 * ---------------------------------------------------------------
 *
 * Given:
 *   - Sorted array of POSITIVE integers
 *   - Strictly increasing
 *
 * Goal:
 *   Find the Kth missing positive number
 *
 * ---------------------------------------------------------------
 * 🧠 THINKING STEP 1 — BRUTE FORCE IDEA
 * ---------------------------------------------------------------
 *
 * Missing sequence:
 *   1,2,3,4,5,6,...
 *
 * Compare with array:
 *   arr = [2,3,4,7,11]
 *
 * Missing:
 *   [1,5,6,8,9,10,...]
 *
 * Brute force:
 *   Walk numbers → skip present → count missing
 *   ❌ O(n + k)
 *
 * ---------------------------------------------------------------
 * 🧠 THINKING STEP 2 — OPTIMIZATION IDEA
 * ---------------------------------------------------------------
 *
 * Instead of generating missing numbers,
 * we COUNT how many are missing BEFORE a position.
 *
 * ---------------------------------------------------------------
 * 🔥 KEY FORMULA (MOST IMPORTANT LINE IN THIS PROBLEM)
 *
 *   missing(i) = arr[i] - (i + 1)
 *
 * ---------------------------------------------------------------
 * WHY THIS WORKS?
 *
 * If NO numbers were missing:
 *   arr[i] should be (i + 1)
 *
 * But actual is arr[i]
 *
 * So difference = missing count
 *
 * ---------------------------------------------------------------
 * 🧠 EXAMPLE
 *
 * arr = [2,3,4,7,11]
 *
 * index   expected   actual   missing
 * -----------------------------------
 *   0        1         2        1
 *   1        2         3        1
 *   2        3         4        1
 *   3        4         7        3
 *   4        5        11        6
 *
 * Notice:
 *   missing array = [1,1,1,3,6]
 *
 * 👉 MONOTONIC (non-decreasing)
 *
 * ---------------------------------------------------------------
 * 🧠 KEY INSIGHT → BINARY SEARCH
 * ---------------------------------------------------------------
 *
 * We search on INDEX, not values.
 *
 * We want:
 *
 *   FIRST index where missing(i) >= k
 *
 * OR
 *
 *   LAST index where missing(i) < k
 *
 * ---------------------------------------------------------------
 * 🎯 INVARIANT (VERY IMPORTANT)
 * ---------------------------------------------------------------
 *
 * During binary search:
 *
 *   Left side → missing < k
 *   Right side → missing >= k
 *
 * ---------------------------------------------------------------
 */
class KthMissingPositiveMaster {

    /**
     * ============================================================
     * ✅ APPROACH 1:
     * FIRST index where missing >= k
     * ============================================================
     *
     * 🔥 MINDSET:
     * "Find boundary where missing crosses k"
     *
     * ------------------------------------------------------------
     * LOOP INVARIANT:
     *
     *   low  → region where answer is NOT yet found
     *   high → region where answer MAY exist
     *
     * answerIndex:
     *   stores best candidate seen so far
     *
     * ------------------------------------------------------------
     */
    public static int findKthPositive_V1(int[] arr, int k) {

        // Start of search space
        int low = 0;

        // End of search space
        int high = arr.length - 1;

        /**
         * WHY default = arr.length ?
         *
         * Means:
         *   If we never find missing >= k inside array,
         *   answer lies AFTER the array.
         */
        int answerIndex = arr.length;

        while (low <= high) {

            /**
             * WHY this formula?
             *
             * Prevents overflow vs (low + high)/2
             */
            int mid = low + (high - low) / 2;

            /**
             * 🔥 CORE LINE
             *
             * missing numbers BEFORE index mid
             */
            int missing = arr[mid] - (mid + 1);

            /**
             * CASE 1:
             * missing >= k
             *
             * Means:
             *   We have already crossed kth missing
             *
             * So:
             *   mid is a VALID candidate
             *   but maybe earlier index also works
             *
             * 👉 Move LEFT
             */
            if (missing >= k) {

                // Store candidate
                answerIndex = mid;

                // Try to find smaller index
                high = mid - 1;
            }

            /**
             * CASE 2:
             * missing < k
             *
             * Means:
             *   We haven't reached kth missing yet
             *
             * 👉 Move RIGHT
             */
            else {
                low = mid + 1;
            }
        }

        /**
         * 🔥 FINAL FORMULA
         *
         * answer = k + answerIndex
         *
         * WHY?
         *
         * Before answer:
         *   present numbers = answerIndex
         *   missing numbers = k
         *
         * position = present + missing
         */
        return k + answerIndex;
    }


    /**
     * ============================================================
     * ✅ APPROACH 2:
     * LAST index where missing < k
     * ============================================================
     *
     * 🔥 MINDSET:
     * "Find last safe position before kth missing"
     *
     * ------------------------------------------------------------
     * LOOP INVARIANT:
     *
     *   answerIndex always holds:
     *     last index where missing < k
     *
     * ------------------------------------------------------------
     */
    public static int findKthPositive_V2(int[] arr, int k) {

        int low = 0;
        int high = arr.length - 1;

        /**
         * WHY -1?
         *
         * Means:
         *   No element satisfies missing < k
         *   → kth missing lies BEFORE array
         */
        int answerIndex = -1;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            int missing = arr[mid] - (mid + 1);

            /**
             * CASE 1:
             * missing < k
             *
             * Means:
             *   kth missing NOT reached yet
             *
             * So:
             *   mid is valid
             *   but maybe we can go further right
             */
            if (missing < k) {

                answerIndex = mid;

                low = mid + 1;
            }

            /**
             * CASE 2:
             * missing >= k
             *
             * Means:
             *   kth missing already reached
             *
             * 👉 Move LEFT
             */
            else {
                high = mid - 1;
            }
        }

        /**
         * EDGE CASE:
         *
         * If answerIndex = -1:
         *   kth missing is before arr[0]
         *
         * Example:
         *   arr = [5,6,7], k = 3
         *   missing = [1,2,3]
         *
         * Answer = 3
         */
        if (answerIndex == -1) {
            return k;
        }

        /**
         * Missing count till answerIndex
         */
        int missingAtIndex = arr[answerIndex] - (answerIndex + 1);

        /**
         * Remaining missing numbers needed
         */
        int remaining = k - missingAtIndex;

        /**
         * FINAL STEP:
         *
         * Extend from arr[answerIndex]
         */
        return arr[answerIndex] + remaining;
    }


    /**
     * ============================================================
     * 🧪 DRY RUN (MANDATORY FOR INTERVIEW)
     * ============================================================
     *
     * arr = [2,3,4,7,11], k = 5
     *
     * missing:
     *   [1,1,1,3,6]
     *
     * We want first index where missing >= 5
     *
     * That is index = 4
     *
     * answer = k + index = 5 + 4 = 9
     *
     * ------------------------------------------------------------
     */


    public static void main(String[] args) {

        int[] arr = {2, 3, 4, 7, 11};
        int k = 5;

        System.out.println(findKthPositive_V1(arr, k));
        System.out.println(findKthPositive_V2(arr, k));
    }
}
