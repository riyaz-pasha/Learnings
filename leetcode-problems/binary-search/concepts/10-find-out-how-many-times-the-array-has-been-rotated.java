class FindKRotation {

    public int findKRotation2(int[] arr) {

        int low = 0;
        int high = arr.length - 1;

        while (low < high) {

            int mid = low + (high - low) / 2;

            // If mid element is greater than high element,
            // minimum must be in right half
            if (arr[mid] > arr[high]) {
                low = mid + 1;
            }

            // Otherwise minimum is in left half (including mid)
            else {
                high = mid;
            }
        }

        // low == high points to the minimum element
        // index of minimum = number of rotations
        return low;
    }


    public int findKRotation(int[] arr) {

        int low = 0;
        int high = arr.length - 1;

        // ans stores the minimum value found so far
        int ans = Integer.MAX_VALUE;

        // index stores the position of that minimum value
        int index = -1;

        while (low <= high) {

            int mid = (low + high) / 2;

            // ---------------------------------------------------------
            // Case 1: Current search space [low..high] is already sorted
            //
            // Example:
            //   arr = [0,1,2,3,4,5]
            //
            // If arr[low] <= arr[high], then the whole range is sorted.
            // In a sorted range, the minimum is always at arr[low].
            //
            // So we can directly update ans/index and break.
            // ---------------------------------------------------------
            if (arr[low] <= arr[high]) {
                if (arr[low] < ans) {
                    ans = arr[low];
                    index = low;
                }
                break;
            }

            // ---------------------------------------------------------
            // Case 2: Left half [low..mid] is sorted
            //
            // Condition: arr[low] <= arr[mid]
            //
            // Example:
            //   arr = [4,5,6,7,0,1,2]
            //   low=0 (4), mid=3 (7), high=6 (2)
            //
            // Left half [4,5,6,7] is sorted.
            // Minimum of this half is arr[low].
            //
            // But global minimum might still be in right half
            // (because pivot is there).
            //
            // So:
            //   - store arr[low] as candidate minimum
            //   - discard left half and search right half
            // ---------------------------------------------------------
            if (arr[low] <= arr[mid]) {

                if (arr[low] < ans) {
                    ans = arr[low];
                    index = low;
                }

                // eliminate sorted left half
                low = mid + 1;
            }

            // ---------------------------------------------------------
            // Case 3: Right half [mid..high] is sorted
            //
            // This happens when left half is NOT sorted,
            // meaning pivot/minimum lies in left half.
            //
            // Example:
            //   arr = [7,0,1,2,3,4,5]
            //   low=0 (7), mid=3 (2), high=6 (5)
            //
            // Right half [2,3,4,5] is sorted.
            // Minimum of this half is arr[mid].
            //
            // So:
            //   - store arr[mid] as candidate minimum
            //   - discard right half and search left half
            // ---------------------------------------------------------
            else {

                if (arr[mid] < ans) {
                    ans = arr[mid];
                    index = mid;
                }

                // eliminate sorted right half
                high = mid - 1;
            }
        }

        // index of minimum element = number of rotations
        return index;
    }


}
