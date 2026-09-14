import java.util.ArrayDeque;
import java.util.Deque;

class SlidingWindowMinimum {

    /*
     * 🧠 Time and Space Complexity
     * Time: O(n) — each element is added/removed from deque at most once.
     * Space: O(k) for deque + output array of size n - k + 1.
     */
    public int[] minSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }

        int n = nums.length;
        int[] result = new int[n - (k - 1)];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int index = 0; index < n; index++) {

            // 1. Remove elements from the front of the deque that are outside the current
            // window
            if (!deque.isEmpty() && deque.peekFirst() <= (index - k)) {
                deque.pollFirst();
            }

            // 2. Maintain a monotonic increasing queue:
            // Remove elements from the back of the deque that are larger than the current
            // element
            while (!deque.isEmpty() && nums[deque.peekLast()] >= nums[index]) {
                deque.pollLast();
            }

            // 3. Add the current element's index to the back of the deque
            deque.addLast(index);

            // 4. Record the minimum element for the current window
            if (index >= (k - 1)) {
                result[index - (k - 1)] = nums[deque.peekFirst()];
            }
        }

        return result;
    }

    /*
     * 🔁 Strategy
     * - Use a monotonic increasing deque (i.e., front always has the smallest
     * value’s index).
     * 
     * - Traverse the array from left to right.
     * 
     * - For each index i:
     * -- Remove indices that are out of window.
     * -- Pop from back all indices whose values are greater than current, as they
     * can’t be minimums.
     * -- Add the current index to the back.
     * -- The front of the deque holds the index of the minimum in current window.
     */

}
