/*
 * Problem Statement: Given two numbers N and M, find the Nth root of M. The nth
 * root of a number M is defined as a number X when raised to the power N equals
 * M. If the 'nth root is not an integer, return -1.
 */

class NthRootOfANumber {

    public int nthRoot(int n, int m) {
        int low = 1, high = m;

        while (low <= high) {

            int mid = low + (high - low) / 2;
            int midN = powerHelper(mid, n, m);
            switch (midN) {
                case 1 -> {
                    return mid;
                }
                case 0 -> low = mid + 1;
                default -> high = mid - 1;
            }
        }

        return -1;
    }

    // Return 1 if mid == m
    // Return 0 if mid < m
    // Return 2 if mid > m
    private int powerHelper(int mid, int n, int m) {
        long ans = 1;
        for (int i = 1; i <= n; i++) {
            ans = ans * mid;
            if (ans > m)
                return 2;
        }
        if (answer == m)
            return 1;
        return 0;
    }
}

/*
 * Algorithm:
 * 
 * Place the 2 pointers i.e. low and high: Initially, we will place the
 * pointers. The pointer low will point to 1 and the high will point to m.
 * Calculate the ‘mid’: Now, inside a loop, we will calculate the value of ‘mid’
 * using the following formula:
 * mid = (low+high) // 2 ( ‘//’ refers to integer division)
 * Eliminate the halves accordingly:
 * 
 * If func(n, m, mid) == 1: On satisfying this condition, we can conclude that
 * the number ‘mid’ is our answer. So, we will return to ‘mid’.
 * 
 * If func(n, m, mid) == 0: On satisfying this condition, we can conclude that
 * the number ‘mid’ is smaller than our answer. So, we will eliminate the left
 * half and consider the right half(i.e. low = mid+1).
 * 
 * If func(n, m, mid) == 2: the value mid is larger than the number we want.
 * This means the numbers greater than ‘mid’ will not be our answers and the
 * right half of ‘mid’ consists of such numbers. So, we will eliminate the right
 * half and consider the left half(i.e. high = mid-1).
 * 
 * Finally, if we are outside the loop, this means no answer exists. So, we will
 * return -1.
 */

class NthRootFinder {

    public static int nthRoot(int N, int M) {
        int low = 1, high = M;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            long midPow = power(mid, N);

            if (midPow == M) {
                return mid;
            } else if (midPow < M) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return -1; // If no integer root exists
    }

    // Computes base^exp using long to prevent overflow
    private static long power(int base, int exp) {
        long result = 1;
        for (int i = 1; i <= exp; i++) {
            result *= base;
            if (result > Integer.MAX_VALUE)
                break; // optional safety
        }
        return result;
    }

    public static void main(String[] args) {
        System.out.println(nthRoot(3, 27)); // 3
        System.out.println(nthRoot(4, 16)); // 2
        System.out.println(nthRoot(3, 15)); // -1
    }

}
