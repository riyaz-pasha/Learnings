class ReversePairs {

    // Approach 1: Brute Force - O(n^2) time, O(1) space
    public int reversePairsBruteForce(int[] nums) {
        int count = 0;
        int n = nums.length;

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // Check if arr[i] > 2 * arr[j]
                // Use long to avoid integer overflow
                if ((long) nums[i] > 2L * (long) nums[j]) {
                    count++;
                }
            }
        }

        return count;
    }

    // Approach 2: Merge Sort Based - O(n log n) time, O(n) space
    public int reversePairsMergeSort(int[] nums) {
        return mergeSort(nums, 0, nums.length - 1);
    }

    private int mergeSort(int[] nums, int left, int right) {
        if (left >= right) {
            return 0;
        }

        int mid = left + (right - left) / 2;
        int count = 0;

        // Count reverse pairs in left and right halves
        count += mergeSort(nums, left, mid);
        count += mergeSort(nums, mid + 1, right);

        // Count reverse pairs across the two halves
        count += countReversePairs(nums, left, mid, right);

        // Merge the two sorted halves
        merge(nums, left, mid, right);

        return count;
    }

    private int countReversePairs(int[] nums, int left, int mid, int right) {
        int count = 0;
        int j = mid + 1;

        // For each element in left half, count elements in right half
        // that satisfy the condition
        for (int i = left; i <= mid; i++) {
            while (j <= right && (long) nums[i] > 2L * (long) nums[j]) {
                j++;
            }
            count += (j - (mid + 1));
        }

        return count;
    }

    private void merge(int[] nums, int left, int mid, int right) {
        int[] temp = new int[right - left + 1];
        int i = left, j = mid + 1, k = 0;

        while (i <= mid && j <= right) {
            if (nums[i] <= nums[j]) {
                temp[k++] = nums[i++];
            } else {
                temp[k++] = nums[j++];
            }
        }

        while (i <= mid) {
            temp[k++] = nums[i++];
        }

        while (j <= right) {
            temp[k++] = nums[j++];
        }

        for (i = left, k = 0; i <= right; i++, k++) {
            nums[i] = temp[k];
        }
    }

    // Approach 3: Using Binary Indexed Tree (Fenwick Tree)
    // O(n log n) time, O(n) space
    public int reversePairsBIT(int[] nums) {
        int n = nums.length;
        if (n == 0)
            return 0;

        // Create array with all values and 2*values for coordinate compression
        long[] allValues = new long[n * 2];
        for (int i = 0; i < n; i++) {
            allValues[i] = nums[i];
            allValues[i + n] = 2L * nums[i];
        }

        // Sort and compress coordinates
        java.util.Arrays.sort(allValues);
        java.util.Map<Long, Integer> compress = new java.util.HashMap<>();
        int idx = 0;
        for (long val : allValues) {
            if (!compress.containsKey(val)) {
                compress.put(val, ++idx);
            }
        }

        int[] bit = new int[idx + 2];
        int count = 0;

        // Process from right to left
        for (int i = n - 1; i >= 0; i--) {
            // Query how many numbers are less than nums[i]
            count += query(bit, compress.get((long) nums[i]) - 1);
            // Update with 2*nums[i]
            update(bit, compress.get(2L * nums[i]));
        }

        return count;
    }

    private void update(int[] bit, int idx) {
        while (idx < bit.length) {
            bit[idx]++;
            idx += idx & (-idx);
        }
    }

    private int query(int[] bit, int idx) {
        int sum = 0;
        while (idx > 0) {
            sum += bit[idx];
            idx -= idx & (-idx);
        }
        return sum;
    }

    // Test cases
    public static void main(String[] args) {
        ReversePairs solution = new ReversePairs();

        // Test case 1
        int[] nums1 = { 1, 3, 2, 3, 1 };
        System.out.println("Test 1: " + solution.reversePairsMergeSort(nums1)); // Expected: 2

        // Test case 2
        int[] nums2 = { 2, 4, 3, 5, 1 };
        System.out.println("Test 2: " + solution.reversePairsMergeSort(nums2)); // Expected: 3

        // Test case 3
        int[] nums3 = { 5, 4, 3, 2, 1 };
        System.out.println("Test 3: " + solution.reversePairsMergeSort(nums3)); // Expected: 4

        // Test case 4 - Edge case with large numbers
        int[] nums4 = { 2147483647, 2147483647, 2147483647, 2147483647, 2147483647, 2147483647 };
        System.out.println("Test 4: " + solution.reversePairsMergeSort(nums4)); // Expected: 0
    }
}
