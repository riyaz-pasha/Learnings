/**
 * CORE BINARY SEARCH PROBLEMS — JAVA
 *
 * Consistent Pattern:
 *   - Always use: int ans = <default>;
 *   - Never return lo or hi directly
 *   - Narrow the window and capture the answer as you go
 *   - Loop invariant: lo <= hi (inclusive on both ends)
 */
class BinarySearch {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CLASSIC BINARY SEARCH
    //    Find the index of target. Return -1 if not found.
    // ─────────────────────────────────────────────────────────────────────────
    public int search(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;
                break;              // exact match — no need to keep searching
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. FIRST OCCURRENCE (Left-most position)
    //    Find the first index where nums[i] == target.
    //    Key: on match, record ans and keep going LEFT (hi = mid - 1).
    // ─────────────────────────────────────────────────────────────────────────
    public int firstOccurrence(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;
                hi = mid - 1;       // don't stop — push left
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. LAST OCCURRENCE (Right-most position)
    //    Find the last index where nums[i] == target.
    //    Key: on match, record ans and keep going RIGHT (lo = mid + 1).
    // ─────────────────────────────────────────────────────────────────────────
    public int lastOccurrence(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;
                lo = mid + 1;       // don't stop — push right
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. COUNT OCCURRENCES
    //    Count how many times target appears.
    //    = lastOccurrence - firstOccurrence + 1
    // ─────────────────────────────────────────────────────────────────────────
    public int countOccurrences(int[] nums, int target) {
        int first = firstOccurrence(nums, target);
        if (first == -1) return 0;
        int last = lastOccurrence(nums, target);
        return last - first + 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. SEARCH INSERT POSITION / LOWER BOUND
    //    First index where nums[i] >= target  (insertion point).
    //    Key: record ans whenever nums[mid] >= target, keep going LEFT.
    // ─────────────────────────────────────────────────────────────────────────
    public int searchInsert(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = nums.length;      // default: insert at end

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] >= target) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. UPPER BOUND
    //    First index where nums[i] > target.
    //    Key: record ans whenever nums[mid] > target, keep going LEFT.
    // ─────────────────────────────────────────────────────────────────────────
    public int upperBound(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = nums.length;      // default: all elements <= target

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] > target) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. SEARCH IN ROTATED SORTED ARRAY
    //    Array was sorted then rotated at some pivot. Find target.
    //    Key: at least one half is always sorted — figure out which half,
    //         check if target fits in it, then discard the other half.
    // ─────────────────────────────────────────────────────────────────────────
    public int searchRotated(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;
                break;
            }
            // Left half is sorted
            if (nums[lo] <= nums[mid]) {
                if (nums[lo] <= target && target < nums[mid]) {
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                }
            }
            // Right half is sorted
            else {
                if (nums[mid] < target && target <= nums[hi]) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. FIND MINIMUM IN ROTATED SORTED ARRAY
    //    Key: minimum is always in the unsorted (rotated) half.
    //         Record ans when nums[mid] could be the min, shrink right half.
    // ─────────────────────────────────────────────────────────────────────────
    public int findMin(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        int ans = nums[0];

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            // Left half is fully sorted — minimum is already in ans or to the right
            if (nums[lo] <= nums[mid]) {
                ans = Math.min(ans, nums[lo]);
                lo = mid + 1;
            }
            // Right half is fully sorted — nums[mid] could be the minimum
            else {
                ans = Math.min(ans, nums[mid]);
                hi = mid - 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. FIND PEAK ELEMENT
    //    A peak is nums[i] > nums[i-1] and nums[i] > nums[i+1].
    //    Key: always move toward the larger neighbor — a peak must exist there.
    // ─────────────────────────────────────────────────────────────────────────
    public int findPeakElement(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        int ans = 0;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            // Check if mid is a peak
            boolean leftOk  = (mid == 0)              || nums[mid] > nums[mid - 1];
            boolean rightOk = (mid == nums.length - 1) || nums[mid] > nums[mid + 1];

            if (leftOk && rightOk) {
                ans = mid;
                break;
            } else if (mid > 0 && nums[mid - 1] > nums[mid]) {
                hi = mid - 1;       // slope goes up to the left
            } else {
                lo = mid + 1;       // slope goes up to the right
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. SQRT(X) — Integer Square Root
    //     Largest integer k such that k*k <= x.
    //     Key: binary search over the answer space [1..x].
    // ─────────────────────────────────────────────────────────────────────────
    public int mySqrt(int x) {
        if (x < 2) return x;
        int lo = 1, hi = x / 2;
        int ans = 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            long sq = (long) mid * mid;
            if (sq == x) {
                ans = mid;
                break;
            } else if (sq < x) {
                ans = mid;          // mid could be the floor sqrt
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. KOKO EATING BANANAS
    //     Find the minimum eating speed k so Koko finishes all piles in h hours.
    //     Key: binary search over speed [1..max(piles)].
    //          For a given speed, hours needed = sum of ceil(pile/speed).
    // ─────────────────────────────────────────────────────────────────────────
    public int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = 0;
        for (int p : piles) hi = Math.max(hi, p);
        int ans = hi;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (canFinish(piles, mid, h)) {
                ans = mid;          // mid works — try slower
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    private boolean canFinish(int[] piles, int speed, int h) {
        long hours = 0;
        for (int p : piles) hours += (p + speed - 1) / speed;
        return hours <= h;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. CAPACITY TO SHIP PACKAGES WITHIN D DAYS
    //     Find minimum ship capacity so all packages are shipped in d days.
    //     Key: binary search over capacity [max(weights)..sum(weights)].
    //          For a given capacity, simulate how many days it takes.
    // ─────────────────────────────────────────────────────────────────────────
    public int shipWithinDays(int[] weights, int days) {
        int lo = 0, hi = 0;
        for (int w : weights) {
            lo = Math.max(lo, w);   // must be at least the heaviest package
            hi += w;                // upper bound: ship everything in one day
        }
        int ans = hi;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (canShip(weights, mid, days)) {
                ans = mid;          // mid works — try smaller capacity
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    private boolean canShip(int[] weights, int cap, int days) {
        int usedDays = 1, load = 0;
        for (int w : weights) {
            if (load + w > cap) { usedDays++; load = 0; }
            load += w;
        }
        return usedDays <= days;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. FIRST BAD VERSION
    //     Given n versions, find the first bad one (isBadVersion API).
    //     Key: when mid is bad, it could be the first — record ans, go left.
    // ─────────────────────────────────────────────────────────────────────────
    // boolean isBadVersion(int version) { ... }   // provided by the problem
    public int firstBadVersion(int n) {
        int lo = 1, hi = n;
        int ans = n;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (isBadVersion(mid)) {
                ans = mid;
                hi = mid - 1;       // mid is bad but might not be the first
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    private boolean isBadVersion(int version) { return false; } // stub

    // ─────────────────────────────────────────────────────────────────────────
    // 14. FIND SMALLEST LETTER GREATER THAN TARGET
    //     Array of characters (circular). Find the next letter > target.
    //     Key: record ans when letters[mid] > target, keep going left.
    //          Wrap around using modulo if nothing found.
    // ─────────────────────────────────────────────────────────────────────────
    public char nextGreatestLetter(char[] letters, char target) {
        int lo = 0, hi = letters.length - 1;
        int ans = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (letters[mid] > target) {
                ans = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        // Wrap around: if no letter > target found, return first letter
        return ans == -1 ? letters[0] : letters[ans];
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 15. FIND IN MOUNTAIN ARRAY
    //     Mountain: strictly increases then strictly decreases.
    //     Step 1: find peak index.
    //     Step 2: binary search ascending half.
    //     Step 3: binary search descending half.
    // ─────────────────────────────────────────────────────────────────────────
    public int findInMountainArray(int target, int[] mountain) {
        int peak = findPeakInMountain(mountain);

        // Try ascending half first
        int ans = binarySearchAsc(mountain, target, 0, peak);
        if (ans != -1) return ans;

        // Try descending half
        return binarySearchDesc(mountain, target, peak + 1, mountain.length - 1);
    }

    private int findPeakInMountain(int[] arr) {
        int lo = 0, hi = arr.length - 1;
        int ans = 0;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (mid < arr.length - 1 && arr[mid] < arr[mid + 1]) {
                lo = mid + 1;
            } else {
                ans = mid;
                hi = mid - 1;
            }
        }
        return ans;
    }

    private int binarySearchAsc(int[] arr, int target, int lo, int hi) {
        int ans = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target)      { ans = mid; break; }
            else if (arr[mid] < target)  { lo = mid + 1; }
            else                         { hi = mid - 1; }
        }
        return ans;
    }

    private int binarySearchDesc(int[] arr, int target, int lo, int hi) {
        int ans = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target)      { ans = mid; break; }
            else if (arr[mid] > target)  { lo = mid + 1; }  // reversed: descending
            else                         { hi = mid - 1; }
        }
        return ans;
    }
}
