/*
 * Problem Statement: You're given an sorted array arr of n integers and an
 * integer x. Find the floor and ceiling of x in arr[0..n-1].
 * The floor of x is the largest element in the array which is smaller than or
 * equal to x.
 * The ceiling of x is the smallest element in the array greater than or equal
 * to x.
 */

/*
 * The floor of x is the largest element in the array which is smaller than or
 * equal to x( i.e. largest element in the array <= x).
 */

class Floor {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] <= target) {
                result = mid;
                // look for smaller index on the left
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return result;
    }

}

/*
 * The ceiling of x is the smallest element in the array greater than or equal
 * to x( i.e. smallest element in the array >= x).
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the pointers
 * like this: low will point to the first index and high will point to the last
 * index.
 * Calculate the ‘mid’: Now, we will calculate the value of mid using the
 * following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Compare arr[mid] with x: With comparing arr[mid] to x, we can observe 2
 * different cases:
 * Case 1 - If arr[mid] >= x: This condition means that the index arr[mid] may
 * be an answer. So, we will update the ‘ans’ variable with arr[mid] and search
 * in the left half if there is any smaller number that satisfies the same
 * condition. Here, we are eliminating the right half.
 * Case 2 - If arr[mid] < x: In this case, arr[mid] cannot be our answer and we
 * need to find some bigger element. So, we will eliminate the left half and
 * search in the right half for the answer.
 */

class Ceil {

    public int binarySearch(int[] nums, int target) {
        int n = nums.length;
        int low = 0, high = n - 1, result = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] >= target) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

}

class AllFloorVariations {

    /**
     * Floor: Largest element in array that is <= target
     * Returns -1 if no such element exists (all elements > target)
     */

    // ============================================================
    // VARIATION 1: low <= high with result variable
    // ============================================================
    public int floor1(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 2: low <= high returning index
    // ============================================================
    public int floor2(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) {
                return arr[mid]; // exact match
            } else if (arr[mid] < target) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 3: Using high pointer final position
    // ============================================================
    public int floor3(int[] arr, int target) {
        int low = 0, high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        // high points to floor element after loop
        return (high >= 0) ? arr[high] : -1;
    }

    // ============================================================
    // VARIATION 4: Recursive with result parameter
    // ============================================================
    public int floor4(int[] arr, int target) {
        return floorRecursive(arr, target, 0, arr.length - 1, -1);
    }

    private int floorRecursive(int[] arr, int target, int low, int high, int result) {
        if (low > high)
            return result;

        int mid = low + (high - low) / 2;
        if (arr[mid] <= target) {
            return floorRecursive(arr, target, mid + 1, high, arr[mid]);
        } else {
            return floorRecursive(arr, target, low, mid - 1, result);
        }
    }

    // ============================================================
    // VARIATION 5: Recursive returning index
    // ============================================================
    public int floor5(int[] arr, int target) {
        int index = floorIndexRecursive(arr, target, 0, arr.length - 1, -1);
        return (index != -1) ? arr[index] : -1;
    }

    private int floorIndexRecursive(int[] arr, int target, int low, int high, int resultIdx) {
        if (low > high)
            return resultIdx;

        int mid = low + (high - low) / 2;
        if (arr[mid] <= target) {
            return floorIndexRecursive(arr, target, mid + 1, high, mid);
        } else {
            return floorIndexRecursive(arr, target, low, mid - 1, resultIdx);
        }
    }

    // ============================================================
    // VARIATION 6: Using bit manipulation for mid
    // ============================================================
    public int floor6(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1; // unsigned right shift
            if (arr[mid] <= target) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 7: Compact ternary style
    // ============================================================
    public int floor7(int[] arr, int target) {
        int low = 0, high = arr.length - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 8: Using comparison boolean
    // ============================================================
    public int floor8(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            boolean isFloorCandidate = arr[mid] <= target;

            if (isFloorCandidate) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 9: With early exact match return
    // ============================================================
    public int floor9(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                return arr[mid]; // exact match is the floor
            } else if (arr[mid] < target) {
                result = arr[mid];
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 10: STL lower_bound style approach
    // ============================================================
    public int floor10(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        // low-1 points to floor element
        return (low > 0 && low <= arr.length) ? arr[low - 1] : -1;
    }

    // ============================================================
    // VARIATION 11: Returning index instead of value
    // ============================================================
    public int floorIndex(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 12: Handle duplicates - rightmost floor
    // ============================================================
    public int floorRightmost(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                result = arr[mid];
                // Continue searching right for potential duplicates
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ============================================================
    // TEST ALL VARIATIONS
    // ============================================================
    public static void main(String[] args) {
        AllFloorVariations floor = new AllFloorVariations();

        int[] arr1 = { 1, 2, 8, 10, 10, 12, 19 };
        int[] arr2 = { 1, 3, 5, 7, 9 };
        int[] arr3 = { 5, 10, 15, 20 };

        System.out.println("=== Test 1: arr=[1,2,8,10,10,12,19], target=5 (Expected: 2) ===");
        System.out.println("Variation 1:  " + floor.floor1(arr1, 5));
        System.out.println("Variation 2:  " + floor.floor2(arr1, 5));
        System.out.println("Variation 3:  " + floor.floor3(arr1, 5));
        System.out.println("Variation 4:  " + floor.floor4(arr1, 5));
        System.out.println("Variation 5:  " + floor.floor5(arr1, 5));
        System.out.println("Variation 6:  " + floor.floor6(arr1, 5));
        System.out.println("Variation 7:  " + floor.floor7(arr1, 5));
        System.out.println("Variation 8:  " + floor.floor8(arr1, 5));
        System.out.println("Variation 9:  " + floor.floor9(arr1, 5));
        System.out.println("Variation 10: " + floor.floor10(arr1, 5));
        System.out.println();

        System.out.println("=== Test 2: arr=[1,2,8,10,10,12,19], target=10 (Expected: 10) ===");
        System.out.println("Variation 1:  " + floor.floor1(arr1, 10));
        System.out.println("Variation 2:  " + floor.floor2(arr1, 10));
        System.out.println("Variation 3:  " + floor.floor3(arr1, 10));
        System.out.println("Variation 4:  " + floor.floor4(arr1, 10));
        System.out.println("Variation 5:  " + floor.floor5(arr1, 10));
        System.out.println();

        System.out.println("=== Test 3: arr=[1,3,5,7,9], target=0 (Expected: -1) ===");
        System.out.println("Variation 1:  " + floor.floor1(arr2, 0));
        System.out.println("Variation 2:  " + floor.floor2(arr2, 0));
        System.out.println("Variation 3:  " + floor.floor3(arr2, 0));
        System.out.println();

        System.out.println("=== Test 4: arr=[1,3,5,7,9], target=100 (Expected: 9) ===");
        System.out.println("Variation 1:  " + floor.floor1(arr2, 100));
        System.out.println("Variation 2:  " + floor.floor2(arr2, 100));
        System.out.println("Variation 3:  " + floor.floor3(arr2, 100));
        System.out.println();

        System.out.println("=== Test 5: arr=[5,10,15,20], target=8 (Expected: 5) ===");
        System.out.println("Variation 1:  " + floor.floor1(arr3, 8));
        System.out.println("Variation 2:  " + floor.floor2(arr3, 8));
        System.out.println("Variation 3:  " + floor.floor3(arr3, 8));
        System.out.println();

        System.out.println("=== Test 6: Floor Index ===");
        System.out.println("arr=[1,2,8,10,10,12,19], target=5");
        System.out.println("Floor index: " + floor.floorIndex(arr1, 5) + " (value: " +
                (floor.floorIndex(arr1, 5) != -1 ? arr1[floor.floorIndex(arr1, 5)] : -1) + ")");
        System.out.println();

        System.out.println("=== RECOMMENDED VARIATIONS ===");
        System.out.println("✅ Variation 1: Clear with explicit result tracking");
        System.out.println("✅ Variation 3: Elegant using high pointer position");
        System.out.println("✅ Variation 9: Efficient with early exact match return");
        System.out.println();
        System.out.println("=== KEY CONCEPT ===");
        System.out.println("Floor = Largest element <= target");
        System.out.println("After binary search, 'high' points to floor element");
        System.out.println("Returns -1 if all elements > target");
    }
}

class AllCeilVariations {

    /**
     * Ceil: Smallest element in array that is >= target
     * Returns -1 if no such element exists (all elements < target)
     */

    // ============================================================
    // VARIATION 1: low <= high with result variable
    // ============================================================
    public int ceil1(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 2: low <= high with exact match check
    // ============================================================
    public int ceil2(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) {
                return arr[mid]; // exact match
            } else if (arr[mid] > target) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 3: Using low pointer final position
    // ============================================================
    public int ceil3(int[] arr, int target) {
        int low = 0, high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        // low points to ceil element after loop
        return (low < arr.length) ? arr[low] : -1;
    }

    // ============================================================
    // VARIATION 4: Recursive with result parameter
    // ============================================================
    public int ceil4(int[] arr, int target) {
        return ceilRecursive(arr, target, 0, arr.length - 1, -1);
    }

    private int ceilRecursive(int[] arr, int target, int low, int high, int result) {
        if (low > high)
            return result;

        int mid = low + (high - low) / 2;
        if (arr[mid] >= target) {
            return ceilRecursive(arr, target, low, mid - 1, arr[mid]);
        } else {
            return ceilRecursive(arr, target, mid + 1, high, result);
        }
    }

    // ============================================================
    // VARIATION 5: Recursive returning index
    // ============================================================
    public int ceil5(int[] arr, int target) {
        int index = ceilIndexRecursive(arr, target, 0, arr.length - 1, -1);
        return (index != -1) ? arr[index] : -1;
    }

    private int ceilIndexRecursive(int[] arr, int target, int low, int high, int resultIdx) {
        if (low > high)
            return resultIdx;

        int mid = low + (high - low) / 2;
        if (arr[mid] >= target) {
            return ceilIndexRecursive(arr, target, low, mid - 1, mid);
        } else {
            return ceilIndexRecursive(arr, target, mid + 1, high, resultIdx);
        }
    }

    // ============================================================
    // VARIATION 6: Using bit manipulation for mid
    // ============================================================
    public int ceil6(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) >>> 1; // unsigned right shift
            if (arr[mid] >= target) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 7: Compact ternary style
    // ============================================================
    public int ceil7(int[] arr, int target) {
        int low = 0, high = arr.length - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 8: Using comparison boolean
    // ============================================================
    public int ceil8(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            boolean isCeilCandidate = arr[mid] >= target;

            if (isCeilCandidate) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 9: With early exact match return
    // ============================================================
    public int ceil9(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (arr[mid] == target) {
                return arr[mid]; // exact match is the ceil
            } else if (arr[mid] > target) {
                result = arr[mid];
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // VARIATION 10: STL lower_bound style approach
    // ============================================================
    public int ceil10(int[] arr, int target) {
        int low = 0, high = arr.length;

        while (low < high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        // low points to ceil element (same as lower_bound)
        return (low < arr.length) ? arr[low] : -1;
    }

    // ============================================================
    // VARIATION 11: Returning index instead of value
    // ============================================================
    public int ceilIndex(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

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
    // VARIATION 12: Handle duplicates - leftmost ceil
    // ============================================================
    public int ceilLeftmost(int[] arr, int target) {
        int low = 0, high = arr.length - 1;
        int result = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                result = arr[mid];
                // Continue searching left for potential duplicates
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    // ============================================================
    // TEST ALL VARIATIONS
    // ============================================================
    public static void main(String[] args) {
        AllCeilVariations ceil = new AllCeilVariations();

        int[] arr1 = { 1, 2, 8, 10, 10, 12, 19 };
        int[] arr2 = { 1, 3, 5, 7, 9 };
        int[] arr3 = { 5, 10, 15, 20 };

        System.out.println("=== Test 1: arr=[1,2,8,10,10,12,19], target=5 (Expected: 8) ===");
        System.out.println("Variation 1:  " + ceil.ceil1(arr1, 5));
        System.out.println("Variation 2:  " + ceil.ceil2(arr1, 5));
        System.out.println("Variation 3:  " + ceil.ceil3(arr1, 5));
        System.out.println("Variation 4:  " + ceil.ceil4(arr1, 5));
        System.out.println("Variation 5:  " + ceil.ceil5(arr1, 5));
        System.out.println("Variation 6:  " + ceil.ceil6(arr1, 5));
        System.out.println("Variation 7:  " + ceil.ceil7(arr1, 5));
        System.out.println("Variation 8:  " + ceil.ceil8(arr1, 5));
        System.out.println("Variation 9:  " + ceil.ceil9(arr1, 5));
        System.out.println("Variation 10: " + ceil.ceil10(arr1, 5));
        System.out.println();

        System.out.println("=== Test 2: arr=[1,2,8,10,10,12,19], target=10 (Expected: 10) ===");
        System.out.println("Variation 1:  " + ceil.ceil1(arr1, 10));
        System.out.println("Variation 2:  " + ceil.ceil2(arr1, 10));
        System.out.println("Variation 3:  " + ceil.ceil3(arr1, 10));
        System.out.println("Variation 4:  " + ceil.ceil4(arr1, 10));
        System.out.println("Variation 5:  " + ceil.ceil5(arr1, 10));
        System.out.println();

        System.out.println("=== Test 3: arr=[1,3,5,7,9], target=0 (Expected: 1) ===");
        System.out.println("Variation 1:  " + ceil.ceil1(arr2, 0));
        System.out.println("Variation 2:  " + ceil.ceil2(arr2, 0));
        System.out.println("Variation 3:  " + ceil.ceil3(arr2, 0));
        System.out.println();

        System.out.println("=== Test 4: arr=[1,3,5,7,9], target=100 (Expected: -1) ===");
        System.out.println("Variation 1:  " + ceil.ceil1(arr2, 100));
        System.out.println("Variation 2:  " + ceil.ceil2(arr2, 100));
        System.out.println("Variation 3:  " + ceil.ceil3(arr2, 100));
        System.out.println();

        System.out.println("=== Test 5: arr=[5,10,15,20], target=12 (Expected: 15) ===");
        System.out.println("Variation 1:  " + ceil.ceil1(arr3, 12));
        System.out.println("Variation 2:  " + ceil.ceil2(arr3, 12));
        System.out.println("Variation 3:  " + ceil.ceil3(arr3, 12));
        System.out.println();

        System.out.println("=== Test 6: Ceil Index ===");
        System.out.println("arr=[1,2,8,10,10,12,19], target=5");
        System.out.println("Ceil index: " + ceil.ceilIndex(arr1, 5) + " (value: " +
                (ceil.ceilIndex(arr1, 5) != -1 ? arr1[ceil.ceilIndex(arr1, 5)] : -1) + ")");
        System.out.println();

        System.out.println("=== RECOMMENDED VARIATIONS ===");
        System.out.println("✅ Variation 1: Clear with explicit result tracking");
        System.out.println("✅ Variation 3: Elegant using low pointer position");
        System.out.println("✅ Variation 9: Efficient with early exact match return");
        System.out.println("✅ Variation 10: STL-style (same as lower_bound)");
        System.out.println();
        System.out.println("=== KEY CONCEPT ===");
        System.out.println("Ceil = Smallest element >= target");
        System.out.println("After binary search, 'low' points to ceil element");
        System.out.println("Returns -1 if all elements < target");
        System.out.println();
        System.out.println("=== RELATIONSHIP ===");
        System.out.println("Floor: Largest element <= target (use 'high' pointer)");
        System.out.println("Ceil:  Smallest element >= target (use 'low' pointer)");
        System.out.println("Ceil is equivalent to Lower Bound!");
    }
}
