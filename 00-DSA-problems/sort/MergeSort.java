class MergeSort1 {

    public void merge(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int n = arr.length;
        int[] temp = new int[n];
        mergeSort(arr, temp, 0, n - 1);
    }

    private void mergeSort(int[] arr, int[] temp, int left, int right) {
        if (left >= right)
            return;

        int mid = left + (right - left) / 2;
        mergeSort(arr, temp, left, mid);
        mergeSort(arr, temp, mid + 1, right);

        merge(arr, temp, left, mid, right);
    }

    private void merge(int[] arr, int[] temp, int left, int mid, int right) {
        for (int i = left; i <= right; i++) {
            temp[i] = arr[i];
        }

        int lp = left;
        int rp = mid + 1;
        int ap = left;

        while (lp <= mid && rp <= right) {
            if (temp[lp] < temp[rp]) {
                arr[ap] = temp[lp];
                lp++;
            } else {
                arr[ap] = temp[rp];
                rp++;
            }
            ap++;
        }

        while (lp <= mid) {
            arr[ap] = temp[lp];
            lp++;
            ap++;
        }

        while (rp <= right) {
            arr[ap] = temp[rp];
            rp++;
            ap++;
        }
    }

}

public class MergeSort {

    // Main method to sort an array
    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1) {
            return;
        }
        int[] temp = new int[arr.length];
        mergeSortHelper(arr, temp, 0, arr.length - 1);
    }

    // Recursive helper method
    private static void mergeSortHelper(int[] arr, int[] temp, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;

            // Sort left half
            mergeSortHelper(arr, temp, left, mid);

            // Sort right half
            mergeSortHelper(arr, temp, mid + 1, right);

            // Merge the sorted halves
            merge(arr, temp, left, mid, right);
        }
    }

    // Merge two sorted subarrays
    private static void merge(int[] arr, int[] temp, int left, int mid, int right) {
        // Copy elements to temp array
        for (int i = left; i <= right; i++) {
            temp[i] = arr[i];
        }

        int i = left; // Index for left subarray
        int j = mid + 1; // Index for right subarray
        int k = left; // Index for merged array

        // Merge temp arrays back into arr
        while (i <= mid && j <= right) {
            if (temp[i] <= temp[j]) {
                arr[k] = temp[i];
                i++;
            } else {
                arr[k] = temp[j];
                j++;
            }
            k++;
        }

        // Copy remaining elements from left subarray
        while (i <= mid) {
            arr[k] = temp[i];
            i++;
            k++;
        }

        // Copy remaining elements from right subarray (if any)
        while (j <= right) {
            arr[k] = temp[j];
            j++;
            k++;
        }
    }

    // Utility method to print array
    public static void printArray(int[] arr) {
        for (int num : arr) {
            System.out.print(num + " ");
        }
        System.out.println();
    }

    // Test the implementation
    public static void main(String[] args) {
        int[] arr = { 38, 27, 43, 3, 9, 82, 10 };

        System.out.println("Original array:");
        printArray(arr);

        mergeSort(arr);

        System.out.println("Sorted array:");
        printArray(arr);
    }
}

class MergeSort2 {

    public static void mergeSort(int[] arr) {
        if (arr == null || arr.length <= 1)
            return;
        mergeSort(arr, 0, arr.length - 1);
    }

    private static void mergeSort(int[] arr, int left, int right) {
        if (left >= right)
            return;

        int mid = left + (right - left) / 2;

        // Sort left half
        mergeSort(arr, left, mid);

        // Sort right half
        mergeSort(arr, mid + 1, right);

        // Merge sorted halves
        merge(arr, left, mid, right);
    }

    private static void merge(int[] arr, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;

        int[] leftArr = new int[n1];
        int[] rightArr = new int[n2];

        // Copy data
        for (int i = 0; i < n1; i++)
            leftArr[i] = arr[left + i];

        for (int j = 0; j < n2; j++)
            rightArr[j] = arr[mid + 1 + j];

        int i = 0, j = 0, k = left;

        // Merge two sorted arrays
        while (i < n1 && j < n2) {
            if (leftArr[i] <= rightArr[j]) {
                arr[k++] = leftArr[i++];
            } else {
                arr[k++] = rightArr[j++];
            }
        }

        // Copy remaining elements
        while (i < n1) {
            arr[k++] = leftArr[i++];
        }

        while (j < n2) {
            arr[k++] = rightArr[j++];
        }
    }

    // Test
    public static void main(String[] args) {
        int[] arr = { 5, 2, 9, 1, 6, 3 };

        mergeSort(arr);

        for (int num : arr) {
            System.out.print(num + " ");
        }
    }
}
