import java.util.Stack;

class PreviousSmallerElement {

    /*
     * 🧠 Time & Space Complexity
     * Time: O(n) → each element is pushed/popped at most once.
     * Space: O(n) → for the stack and result array.
     */
    public int[] previousSmallerElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = 0; index < n; index++) {

            // Pop elements greater than or equal to current
            while (!stack.isEmpty() && stack.peek() >= nums[index]) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                // If the stack is empty, there is no smaller element to the left.
                result[index] = -1;
            } else {
                // The element at the top of the stack is the previous smaller element.
                result[index] = stack.peek();
            }

            stack.push(nums[index]);
        }

        return result;
    }

    public int[] previousSmallerElementV2(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int index = 0; index < n; index++) {

            while (!stack.isEmpty() && nums[stack.peek()] >= nums[i]) {
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
