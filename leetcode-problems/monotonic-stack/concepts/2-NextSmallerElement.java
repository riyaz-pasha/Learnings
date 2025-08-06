import java.util.Stack;

class NextSmallerElement {

    /*
     * 📦 Time & Space Complexity
     * Time: O(n) → Each element is pushed and popped at most once.
     * Space: O(n) for the stack and result array.
     */
    public int[] nextSmallerElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = n - 1; index >= 0; index--) {
            // Pop all elements greater than or equal to current
            while (!stack.isEmpty() && stack.peek() >= nums[index]) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                // If the stack is empty, there is no smaller element to the right.
                result[index] = -1;
            } else {
                // The element at the top of the stack is the next smaller element.
                result[index] = stack.peek();
            }

            stack.push(nums[index]);
        }

        return result;
    }

    public int[] nextSmallerElementV2(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        // Stack stores indices of elements
        Stack<Integer> stack = new Stack<>();

        for (int index = n - 1; index >= 0; index--) {
            // Pop all elements greater than or equal to current
            while (!stack.isEmpty() && nums[stack.peek()] >= nums[index]) {
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
