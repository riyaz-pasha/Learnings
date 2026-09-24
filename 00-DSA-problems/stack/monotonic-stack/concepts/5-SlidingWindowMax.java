
import java.util.ArrayDeque;
import java.util.Deque;

class SlidingWindowMax {

    /*
     * .
     * 
     * 🧠 Time and Space Complexity
     * Time: O(n) Each element is added and removed at most once.
     * Space: O(k) for the deque + output of size n - k + 1.
     */
    public int[] maxSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) {
            return new int[0];
        }

        int n = nums.length;
        int[] result = new int[n - (k - 1)];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int index = 0; index < n; index++) {

            // 1. Remove elements from the front of the deque that are outside the current
            // window
            if (!deque.isEmpty() && deque.peekFirst() <= index - k) {
                deque.pollFirst();
            }

            // Remove elements from the back of the deque that are smaller than the current
            // element
            while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[index]) {
                deque.pollLast();
            }

            // 3. Add the current element's index to the back of the deque
            deque.offerLast(index);

            // 4. Record the maximum element for the current window
            if (index >= k - 1) {
                result[index - (k - 1)] = nums[deque.peekFirst()];
            }
        }

        return result;
    }

    /*
     * 🧠 Brute Force (Not Efficient)
     * For every window of size k, scan all k elements.
     * Time: O(n * k)
     * Fails for large n.
     */

    /*
     * ✅ Optimal Solution: Monotonic Deque
     * Use a double-ended queue (Deque) to maintain a monotonic decreasing queue of
     * indices.
     * 
     * ✅ Why Use Deque?
     * - It allows push/pop from both ends.
     * - We can efficiently maintain window and max ordering in O(n).
     * 
     * 💡 Intuition
     * - Keep indices of potential max values in the window.
     * - Maintain a decreasing deque: the front always holds the max of the current
     * window.
     * - When you move the window:
     * -- Remove elements outside the window
     * -- Remove elements smaller than the current from the back.
     * 
     * 🔍 How It Works
     * For each i:
     * - deque.peekFirst() holds the index of the maximum in current window.
     * - All elements smaller than nums[i] are removed from the back.
     * - All elements outside window are removed from the front.
     */

}
