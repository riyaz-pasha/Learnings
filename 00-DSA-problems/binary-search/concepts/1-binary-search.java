class BinarySearch {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                return mid;
            } else if (target > nums[mid]) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

}

class BinarySearchRecursive {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        return this.binarySearch(nums, target, 0, n - 1);
    }

    private int binarySearch(int[] nums, int target, int low, int high) {
        if (low > high) {
            return -1;
        }
        int mid = low + (high - low) / 2;
        if (nums[mid] == target) {
            return mid;
        }
        if (target > nums[mid]) {
            return this.binarySearch(nums, target, mid + 1, high);
        }
        return this.binarySearch(nums, target, low, mid - 1);
    }

}
