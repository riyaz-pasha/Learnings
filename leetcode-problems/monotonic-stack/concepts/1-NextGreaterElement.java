import java.util.Arrays;
import java.util.Stack;

class NextGreaterElement {

    /*
     * 📦 Time & Space Complexities
     * Time: O(n)
     * Each element is pushed and popped at most once.
     * 
     * Space: O(n)
     * For the stack and possibly result array.
     */
    public int[] nextGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = n - 1; index >= 0; index--) {

            // Pop all elements that are less than or equal to the current element
            while (!stack.isEmpty() && stack.peek() <= nums[index]) {
                stack.pop();
            }

            // The top of the stack is the next greater element
            if (stack.isEmpty()) {
                result[index] = -1;
            } else {
                result[index] = stack.peek(); // No greater element found
            }

            // Push the current element onto the stack
            stack.push(nums[index]);
        }

        return result;
    }

    public int[] nextGreaterElementV2(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>(); // store indexes in stack instead values

        for (int index = n - 1; index >= 0; index--) {

            while (!stack.isEmpty() && nums[stack.peek()] <= nums[index]) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                result[index] = -1;
            } else {
                result[index] = nums[stack.peek()];
            }

            stack.push(index);
        }

        return result;
    }

}

class CircularNextGreaterElement {

    /*
     * 🛠 You simulate a circular array by looping twice (i.e., 2 * n) using i % n
     * as the real index.
     * So:
     * - Traverse backward from 2n - 1 to 0
     * - Use a Monotonic Decreasing Stack (like usual)
     * - Use modulo: arr[i % n] to access actual index
     */
    public int[] nextGreaterElements(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1); // Default if no greater element

        Stack<Integer> stack = new Stack<>(); // Stack stores indices

        // Loop from 2n-1 to 0 (simulate circular array)
        for (int i = 2 * n - 1; i >= 0; i--) {
            int index = i % n;

            while (!stack.isEmpty() && nums[stack.peek()] <= nums[index]) {
                stack.pop();
            }

            // Only assign result during first pass
            if (i < n) {
                result[index] = stack.isEmpty() ? -1 : nums[stack.peek()];
            }

            stack.push(index);
        }

        return result;
    }

}
