/*
 * Problem Statement: You are given a sorted array containing N integers and a
 * number X, you have to find the occurrences of X in the given array.
 */
class CountOccurrencesInSortedArray {

    public int firstOccurance(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid;
                // look for smaller index on the left
                high = mid - 1;
            } else if (nums[mid] > target) {
                // look on the left
                high = mid - 1;
            } else {
                // look on the right
                low = mid + 1;
            }
        }

        return result;
    }

    public int lastOccurance(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid;
                // look for larger index on the right
                low = mid + 1;
            } else if (nums[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return result;
    }

    public int[] firstAndLastPosition(int[] nums, int target) {
        int first = firstOccurance(nums, target);
        if (first == -1) {
            return new int[] { -1, -1 };
        }
        int last = lastOccurance(nums, target);
        return new int[] { first, last };
    }

    public int count(int[] nums, int target) {
        int[] answer = firstAndLastPosition(nums, target);
        if (answer[0] == -1)
            return 0;
        return (answer[1] - answer[0]) + 1;
    }

}
