/*
 * Given a sorted array of N integers, write a program to find the index of the
 * last occurrence of the target key. If the target is not found then return -1.
 */

class FirstOccurrenceInASortedArray {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid;
                high = mid - 1;
            } else if (nums[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}

class LastOccurrenceInASortedArray {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (nums[mid] == target) {
                result = mid;
                low = mid + 1;
            } else if (nums[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}

// First occurance can be found with lower bound
// last occurance can be found with (upper bound -1)

class AllFirstOccurrenceVariations {

    /**
     * First Occurrence: Find the first (leftmost) index of target in sorted array
     * Returns -1 if target is not found
     */

    // ============================================================
    // VARIATION 1: low <= high with result variable (STANDARD)
    // ============================================================
    public int firstOccurrence1(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                result = mid;
                high = mid - 1; // continue searching left
            } else if (arr[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 2: low < high with high = n
    // ============================================================
    public int firstOccurrence2(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        // Check if target found at position low
        if (low < arr.length && arr[low] == target) {
            return low;
        }
        return -1;
    }

    // ============================================================
    // VARIATION 3: Simplified condition with >= comparison
    // ============================================================
    public int firstOccurrence3(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] >= target) {
                if (arr[mid] == target) {
                    result = mid;
                }
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 4: Recursive with result parameter
    // ============================================================
    public int firstOccurrence4(int[] arr, int target) {
        return firstOccurrenceRecursive(arr, target, 0, arr.length - 1, -1);
    }

    private int firstOccurrenceRecursive(int[] arr, int target, int low, int high, int result) {
        if (low > high)
            return result;

        int mid = low + (high - low) / 2;

        if (arr[mid] == target) {
            return firstOccurrenceRecursive(arr, target, low, mid - 1, mid);
        } else if (arr[mid] > target) {
            return firstOccurrenceRecursive(arr, target, low, mid - 1, result);
        } else {
            return firstOccurrenceRecursive(arr, target, mid + 1, high, result);
        }
    }

    // ============================================================
    // VARIATION 5: Recursive with low < high
    // ============================================================
    public int firstOccurrence5(int[] arr, int target) {
        int index = firstOccurrenceRecursive2(arr, target, 0, arr.length);
        return (index < arr.length && arr[index] == target) ? index : -1;
    }

    private int firstOccurrenceRecursive2(int[] arr, int target, int low, int high) {
        if (low >= high)
            return low;

        int mid = low + (high - low) / 2;

        if (arr[mid] < target) {
            return firstOccurrenceRecursive2(arr, target, mid + 1, high);
        } else {
            return firstOccurrenceRecursive2(arr, target, low, mid);
        }
    }

    // ============================================================
    // VARIATION 6: Using bit manipulation for mid
    // ============================================================
    public int firstOccurrence6(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1; // unsigned right shift

            if (arr[mid] == target) {
                result = mid;
                high = mid - 1;
            } else if (arr[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 7: Compact style without else-if
    // ============================================================
    public int firstOccurrence7(int[] arr, int target) {
        int low = 0, high = arr.length - 1, result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                result = mid;
                high = mid - 1;
            } else if (arr[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 8: Using ternary operators
    // ============================================================
    public int firstOccurrence8(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            boolean found = arr[mid] == target;
            boolean greater = arr[mid] > target;

            result = found ? mid : result;
            high = (found || greater) ? mid - 1 : high;
            low = (!found && !greater) ? mid + 1 : low;
        }
        return result;
    }

    // ============================================================
    // VARIATION 9: Linear scan after finding any occurrence
    // ============================================================
    public int firstOccurrence9(int[] arr, int target) {
        int low = 0, high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                // Found target, scan left for first occurrence
                while (mid > 0 && arr[mid - 1] == target) {
                    mid--;
                }
                return mid;
            } else if (arr[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return -1;
    }

    // ============================================================
    // VARIATION 10: Using lower bound approach
    // ============================================================
    public int firstOccurrence10(int[] arr, int target) {
        // Find lower bound (first >= target)
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        // Verify if element at low is target
        return (low < arr.length && arr[low] == target) ? low : -1;
    }

    // ============================================================
    // VARIATION 11: Optimized with early termination
    // ============================================================
    public int firstOccurrence11(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                // If it's the first element or previous element is different
                if (mid == 0 || arr[mid - 1] != target) {
                    return mid;
                }
                high = mid - 1;
            } else if (arr[mid] > target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 12: Java Collections binarySearch style
    // ============================================================
    public int firstOccurrence12(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = (low + high) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        if (low < arr.length && arr[low] == target) {
            return low;
        }
        return -1;
    }

    // ============================================================
    // TEST ALL VARIATIONS
    // ============================================================
    public static void main(String[] args) {
        AllFirstOccurrenceVariations fo = new AllFirstOccurrenceVariations();

        int[] arr1 = { 1, 2, 2, 2, 2, 3, 4, 5 };
        int[] arr2 = { 1, 3, 5, 7, 9 };
        int[] arr3 = { 2, 2, 2, 2, 2 };

        System.out.println("=== Test 1: arr=[1,2,2,2,2,3,4,5], target=2 (Expected: 1) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr1, 2));
        System.out.println("Variation 2:  " + fo.firstOccurrence2(arr1, 2));
        System.out.println("Variation 3:  " + fo.firstOccurrence3(arr1, 2));
        System.out.println("Variation 4:  " + fo.firstOccurrence4(arr1, 2));
        System.out.println("Variation 5:  " + fo.firstOccurrence5(arr1, 2));
        System.out.println("Variation 6:  " + fo.firstOccurrence6(arr1, 2));
        System.out.println("Variation 7:  " + fo.firstOccurrence7(arr1, 2));
        System.out.println("Variation 8:  " + fo.firstOccurrence8(arr1, 2));
        System.out.println("Variation 9:  " + fo.firstOccurrence9(arr1, 2));
        System.out.println("Variation 10: " + fo.firstOccurrence10(arr1, 2));
        System.out.println("Variation 11: " + fo.firstOccurrence11(arr1, 2));
        System.out.println("Variation 12: " + fo.firstOccurrence12(arr1, 2));
        System.out.println();

        System.out.println("=== Test 2: arr=[1,3,5,7,9], target=5 (Expected: 2) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr2, 5));
        System.out.println("Variation 2:  " + fo.firstOccurrence2(arr2, 5));
        System.out.println("Variation 3:  " + fo.firstOccurrence3(arr2, 5));
        System.out.println("Variation 4:  " + fo.firstOccurrence4(arr2, 5));
        System.out.println("Variation 5:  " + fo.firstOccurrence5(arr2, 5));
        System.out.println();

        System.out.println("=== Test 3: arr=[1,3,5,7,9], target=4 (Expected: -1) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr2, 4));
        System.out.println("Variation 2:  " + fo.firstOccurrence2(arr2, 4));
        System.out.println("Variation 3:  " + fo.firstOccurrence3(arr2, 4));
        System.out.println();

        System.out.println("=== Test 4: arr=[2,2,2,2,2], target=2 (Expected: 0) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr3, 2));
        System.out.println("Variation 2:  " + fo.firstOccurrence2(arr3, 2));
        System.out.println("Variation 3:  " + fo.firstOccurrence3(arr3, 2));
        System.out.println();

        System.out.println("=== Test 5: arr=[1,2,2,2,2,3,4,5], target=1 (Expected: 0) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr1, 1));
        System.out.println("Variation 9:  " + fo.firstOccurrence9(arr1, 1));
        System.out.println("Variation 11: " + fo.firstOccurrence11(arr1, 1));
        System.out.println();

        System.out.println("=== Test 6: arr=[1,2,2,2,2,3,4,5], target=5 (Expected: 7) ===");
        System.out.println("Variation 1:  " + fo.firstOccurrence1(arr1, 5));
        System.out.println("Variation 9:  " + fo.firstOccurrence9(arr1, 5));
        System.out.println("Variation 11: " + fo.firstOccurrence11(arr1, 5));
        System.out.println();

        System.out.println("=== RECOMMENDED VARIATIONS ===");
        System.out.println("✅ Variation 1: Standard and most readable (with result variable)");
        System.out.println("✅ Variation 10: Clean lower bound approach");
        System.out.println("✅ Variation 11: Optimized with early termination");
        System.out.println();
        System.out.println("=== KEY CONCEPT ===");
        System.out.println("First Occurrence = Leftmost position of target");
        System.out.println("When target found: result = mid, search left (high = mid - 1)");
        System.out.println("Equivalent to Lower Bound when target exists");
        System.out.println("Time Complexity: O(log n)");
    }
}
