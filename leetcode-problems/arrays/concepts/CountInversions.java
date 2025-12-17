
class CountInversions {

    // ==================== BRUTE FORCE APPROACH ====================
    // Time Complexity: O(N^2)
    // Space Complexity: O(1)
    public int countInversionsBruteForce(int[] arr) {
        int n = arr.length;
        int count = 0;

        // Check all pairs (i, j) where i < j
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                // If arr[i] > arr[j], it's an inversion
                if (arr[i] > arr[j]) {
                    count++;
                }
            }
        }

        return count;
    }

    // ==================== OPTIMAL APPROACH (MERGE SORT) ====================
    // Time Complexity: O(N log N)
    // Space Complexity: O(N)

    /**
     * Main function to count inversions using merge sort
     */
    public int numberOfInversions(int[] arr) {
        return mergeSort(arr, 0, arr.length - 1);
    }

    /**
     * Recursive merge sort function that counts inversions
     * 
     * @param arr  The array to sort
     * @param low  Starting index
     * @param high Ending index
     * @return Count of inversions in the range [low, high]
     */
    private int mergeSort(int[] arr, int low, int high) {
        int cnt = 0;

        // Base case: single element or invalid range
        if (low >= high) {
            return cnt;
        }

        int mid = low + (high - low) / 2;

        // Count inversions in left half
        cnt += mergeSort(arr, low, mid);

        // Count inversions in right half
        cnt += mergeSort(arr, mid + 1, high);

        // Count inversions during merge (cross inversions)
        cnt += merge(arr, low, mid, high);

        return cnt;
    }

    /**
     * Merge two sorted halves and count inversions
     * 
     * @param arr  The array
     * @param low  Starting index of left half
     * @param mid  Ending index of left half
     * @param high Ending index of right half
     * @return Count of inversions between the two halves
     */
    private int merge(int[] arr, int low, int mid, int high) {
        // Temporary array to store merged result
        int[] temp = new int[high - low + 1];

        int left = low; // Pointer for left half
        int right = mid + 1; // Pointer for right half
        int k = 0; // Pointer for temp array

        int cnt = 0; // Inversion count

        // Merge elements in sorted order
        while (left <= mid && right <= high) {
            if (arr[left] <= arr[right]) {
                // No inversion: left element is smaller
                temp[k++] = arr[left++];
            } else {
                // Inversion found!
                // All remaining elements in left half (left to mid)
                // are greater than arr[right]
                temp[k++] = arr[right++];
                cnt += (mid - left + 1);
            }
        }

        // Copy remaining elements from left half
        while (left <= mid) {
            temp[k++] = arr[left++];
        }

        // Copy remaining elements from right half
        while (right <= high) {
            temp[k++] = arr[right++];
        }

        // Copy merged elements back to original array
        for (int i = low; i <= high; i++) {
            arr[i] = temp[i - low];
        }

        return cnt;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Print all inversion pairs (for debugging/understanding)
     */
    public void printInversions(int[] arr) {
        int n = arr.length;
        System.out.println("Inversion pairs:");

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (arr[i] > arr[j]) {
                    System.out.println("(" + arr[i] + ", " + arr[j] + ")");
                }
            }
        }
    }
}
