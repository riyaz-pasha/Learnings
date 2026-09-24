/*
 * Problem Statement: Given a sorted array of N integers and an integer x, write
 * a program to find the lower bound of x.
 * 
 * 
 * What is Lower Bound?
 * The lower bound algorithm finds the first or the smallest index in a sorted
 * array where the value at that index is greater than or equal to a given key
 * i.e. x.
 * 
 * The lower bound is the smallest index, ind, where arr[ind] >= x. But if any
 * such index is not found, the lower bound algorithm returns n i.e. size of the
 * given array.
 * 
 */

class LowerBound {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] >= target) {
                high = mid - 1;
                result = mid;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    public static int lowerBound(int[] arr, int target) {
        int low = 0, high = arr.length; // note: high = n

        while (low < high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low; // first index where arr[index] >= target
    }

}

/*
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index, and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, we will calculate the value of mid using the
 * following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Compare arr[mid] with x: With comparing arr[mid] to x, we can observe 2
 * different cases:
 * Case 1 - If arr[mid] >= x: This condition means that the index mid may be an
 * answer. So, we will update the ‘ans’ variable with mid and search in the left
 * half if there is any smaller index that satisfies the same condition. Here,
 * we are eliminating the right half.
 * Case 2 - If arr[mid] < x: In this case, mid cannot be our answer and we need
 * to find some bigger element. So, we will eliminate the left half and search
 * in the right half for the answer.
 */

class AllLowerBoundVariations {

    /**
     * Lower Bound: Returns first index where arr[i] >= target
     * Returns n if target > all elements
     */

    // ============================================================
    // VARIATION 1: low <= high with result variable
    // ============================================================
    public int lowerBound1(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = arr.length;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 2: low < high with high = n (MOST ELEGANT)
    // ============================================================
    public int lowerBound2(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    // ============================================================
    // VARIATION 3: low <= high without result variable
    // ============================================================
    public int lowerBound3(int[] arr, int target) {
        int low = 0, high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    // ============================================================
    // VARIATION 4: Recursive with low < high
    // ============================================================
    public int lowerBound4(int[] arr, int target) {
        return lowerBoundRecursive(arr, target, 0, arr.length);
    }

    private int lowerBoundRecursive(int[] arr, int target, int low, int high) {
        if (low >= high)
            return low;

        int mid = low + (high - low) / 2;
        if (arr[mid] < target) {
            return lowerBoundRecursive(arr, target, mid + 1, high);
        } else {
            return lowerBoundRecursive(arr, target, low, mid);
        }
    }

    // ============================================================
    // VARIATION 5: Recursive with low <= high
    // ============================================================
    public int lowerBound5(int[] arr, int target) {
        return lowerBoundRecursive2(arr, target, 0, arr.length - 1, arr.length);
    }

    private int lowerBoundRecursive2(int[] arr, int target, int low, int high, int result) {
        if (low > high)
            return result;

        int mid = low + (high - low) / 2;
        if (arr[mid] >= target) {
            return lowerBoundRecursive2(arr, target, low, mid - 1, mid);
        } else {
            return lowerBoundRecursive2(arr, target, mid + 1, high, result);
        }
    }

    // ============================================================
    // VARIATION 6: Using bit manipulation for mid
    // ============================================================
    public int lowerBound6(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = (low + high) >>> 1; // unsigned right shift
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    // ============================================================
    // VARIATION 7: Compact ternary operator
    // ============================================================
    public int lowerBound7(int[] arr, int target) {
        int low = 0, high = arr.length;
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target)
                low = mid + 1;
            else
                high = mid;
        }
        return low;
    }

    // ============================================================
    // VARIATION 8: Using comparison result
    // ============================================================
    public int lowerBound8(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            boolean moveRight = arr[mid] < target;
            low = moveRight ? mid + 1 : low;
            high = moveRight ? high : mid;
        }
        return low;
    }

    // ============================================================
    // VARIATION 9: With early return if exact match
    // ============================================================
    public int lowerBound9(int[] arr, int target) {
        int low = 0, high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) {
                // Found exact match, search left for first occurrence
                while (mid > 0 && arr[mid - 1] == target) {
                    mid--;
                }
                return mid;
            } else if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    // ============================================================
    // VARIATION 10: Using Java Collections binary search style
    // ============================================================
    public int lowerBound10(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = (low + high) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    // ============================================================
    // TEST ALL VARIATIONS
    // ============================================================
    public static void main(String[] args) {
        AllLowerBoundVariations lb = new AllLowerBoundVariations();

        int[] arr1 = { 1, 2, 2, 2, 3, 4, 5 };
        int[] arr2 = { 1, 3, 5, 7, 9 };

        System.out.println("=== Test 1: arr=[1,2,2,2,3,4,5], target=2 (Expected: 1) ===");
        System.out.println("Variation 1:  " + lb.lowerBound1(arr1, 2));
        System.out.println("Variation 2:  " + lb.lowerBound2(arr1, 2));
        System.out.println("Variation 3:  " + lb.lowerBound3(arr1, 2));
        System.out.println("Variation 4:  " + lb.lowerBound4(arr1, 2));
        System.out.println("Variation 5:  " + lb.lowerBound5(arr1, 2));
        System.out.println("Variation 6:  " + lb.lowerBound6(arr1, 2));
        System.out.println("Variation 7:  " + lb.lowerBound7(arr1, 2));
        System.out.println("Variation 8:  " + lb.lowerBound8(arr1, 2));
        System.out.println("Variation 9:  " + lb.lowerBound9(arr1, 2));
        System.out.println("Variation 10: " + lb.lowerBound10(arr1, 2));
        System.out.println();

        System.out.println("=== Test 2: arr=[1,3,5,7,9], target=4 (Expected: 2) ===");
        System.out.println("Variation 1:  " + lb.lowerBound1(arr2, 4));
        System.out.println("Variation 2:  " + lb.lowerBound2(arr2, 4));
        System.out.println("Variation 3:  " + lb.lowerBound3(arr2, 4));
        System.out.println("Variation 4:  " + lb.lowerBound4(arr2, 4));
        System.out.println("Variation 5:  " + lb.lowerBound5(arr2, 4));
        System.out.println("Variation 6:  " + lb.lowerBound6(arr2, 4));
        System.out.println("Variation 7:  " + lb.lowerBound7(arr2, 4));
        System.out.println("Variation 8:  " + lb.lowerBound8(arr2, 4));
        System.out.println("Variation 9:  " + lb.lowerBound9(arr2, 4));
        System.out.println("Variation 10: " + lb.lowerBound10(arr2, 4));
        System.out.println();

        System.out.println("=== Test 3: arr=[1,3,5,7,9], target=0 (Expected: 0) ===");
        System.out.println("Variation 1:  " + lb.lowerBound1(arr2, 0));
        System.out.println("Variation 2:  " + lb.lowerBound2(arr2, 0));
        System.out.println("Variation 3:  " + lb.lowerBound3(arr2, 0));
        System.out.println();

        System.out.println("=== Test 4: arr=[1,3,5,7,9], target=10 (Expected: 5) ===");
        System.out.println("Variation 1:  " + lb.lowerBound1(arr2, 10));
        System.out.println("Variation 2:  " + lb.lowerBound2(arr2, 10));
        System.out.println("Variation 3:  " + lb.lowerBound3(arr2, 10));
        System.out.println();

        System.out.println("=== RECOMMENDED VARIATIONS ===");
        System.out.println("✅ Variation 2: Most elegant and widely used");
        System.out.println("✅ Variation 1: Clear with explicit result tracking");
        System.out.println("✅ Variation 3: Clean without extra variable");
    }
}
