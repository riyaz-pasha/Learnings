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
