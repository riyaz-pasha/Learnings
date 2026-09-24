class MinHeapChecker {

    /*
     * | Metric | Complexity |
     * | ------ | ---------- |
     * | Time | **O(n)** |
     * | Space | **O(1)** |
     */
    public static boolean isMinHeap(int[] nums) {
        int n = nums.length;

        // Check only non-leaf nodes
        for (int i = 0; i <= (n / 2) - 1; i++) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;

            // If left child exists and violates min-heap property
            if (left < n && nums[i] > nums[left]) {
                return false;
            }

            // If right child exists and violates min-heap property
            if (right < n && nums[i] > nums[right]) {
                return false;
            }
        }

        return true;
    }

    // Example usage
    public static void main(String[] args) {
        int[] heap1 = { 1, 3, 5, 7, 9, 6 };
        int[] heap2 = { 10, 3, 5, 7, 9 };

        System.out.println(isMinHeap(heap1)); // true
        System.out.println(isMinHeap(heap2)); // false
    }
}

class MinHeapChecker2 {

    public static boolean isMinHeap(int[] nums) {
        return check(nums, 0);
    }

    private static boolean check(int[] nums, int i) {
        int n = nums.length;

        // Base case: leaf node
        if (i >= n / 2) {
            return true;
        }

        int left = 2 * i + 1;
        int right = 2 * i + 2;

        // If left child violates min-heap
        if (left < n && nums[i] > nums[left]) {
            return false;
        }

        // If right child violates min-heap
        if (right < n && nums[i] > nums[right]) {
            return false;
        }

        // Recursively check left and right subtrees
        return check(nums, left) && check(nums, right);
    }
    /*
     * | Metric | Value |
     * | ------ | --------------------------------------- |
     * | Time | **O(n)** |
     * | Space | **O(h)** (recursion stack, `h = log n`) |
     */
}
