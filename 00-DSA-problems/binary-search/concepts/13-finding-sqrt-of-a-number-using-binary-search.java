/*
 * Problem Statement: You are given a positive integer n. Your task is to find
 * and return its square root. If ‘n’ is not a perfect square, then return the
 * floor value of 'sqrt(n)'.
 * 
 * Note: The question explicitly states that if the given number, n, is not a
 * perfect square, our objective is to find the maximum number, x, such that x
 * squared is less than or equal to n (x*x <= n). In other words, we need to
 * determine the floor value of the square root of n.
 */

class FindingSqrt {

    public int floorSqrt(int n) {
        int low = 1, high = n;
        // Binary search on the answers:
        while (low <= high) {
            long mid = (low + high) / 2;
            long val = mid * mid;
            if (val <= (long) (n)) {
                // eliminate the left half:
                low = (int) (mid + 1);
            } else {
                // eliminate the right half:
                high = (int) (mid - 1);
            }
        }
        return high;
    }

    public double sqrt(double x, int precision) {
        double low = 0, high = x, mid = 0;

        double eps = Math.pow(10, -precision); // e.g., 0.00001 for 5 decimals

        while ((high - low) > eps) {
            mid = (low + high) / 2;
            if (mid * mid < x) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return low;
    }

    // Floor
    public int mySqrt(int x) {
        if (x == 0 || x == 1)
            return x;

        int low = 1, high = x, ans = 0;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (mid <= x / mid) { // Avoid overflow
                ans = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return ans;
    }

    public int floorSqrt2(int n) {
        int low = 1, high = n;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (mid <= (n / mid)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return high;
    }

}
