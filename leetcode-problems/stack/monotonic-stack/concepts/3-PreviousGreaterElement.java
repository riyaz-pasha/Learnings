import java.util.Stack;

class PreviousGreaterElement {

    /*
     * 🧠 Time & Space Complexity
     * Time: O(n) → each element is pushed/popped once.
     * Space: O(n) → for stack and result array.
     */
    public int[] previousGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = 0; index < n; index++) {
            // Pop all elements smaller than or equal to current
            while (!stack.isEmpty() && stack.peek() <= nums[index]) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                // If the stack is empty, there is no greater element to the left.
                result[index] = -1;
            } else {
                // The element at the top of the stack is the previous greater element.
                result[index] = stack.peek();
            }

            // Push the current element onto the stack.
            stack.push(nums[index]);
        }

        return result;
    }

    public int[] previousGreaterElementV2(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = 0; index < n; index++) {
            // Pop all elements smaller than or equal to current
            while (!stack.isEmpty() && nums[stack.peek()] <= nums[index]) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                // If the stack is empty, there is no greater element to the left.
                result[index] = -1;
            } else {
                // The element at the top of the stack is the previous greater element.
                result[index] = nums[stack.peek()];
            }

            // Push the current index onto the stack.
            stack.push(index);
        }

        return result;
    }

}
