class StringToIntegerSolution {

    // Approach 1: Clean and straightforward implementation
    public int myAtoi(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }

        int i = 0;
        int n = s.length();

        // Step 1: Skip leading whitespace
        while (i < n && s.charAt(i) == ' ') {
            i++;
        }

        // Check if we've reached the end
        if (i == n) {
            return 0;
        }

        // Step 2: Determine sign
        int sign = 1;
        if (s.charAt(i) == '-' || s.charAt(i) == '+') {
            sign = (s.charAt(i) == '-') ? -1 : 1;
            i++;
        }

        // Step 3: Convert digits and handle overflow
        long result = 0;
        while (i < n && Character.isDigit(s.charAt(i))) {
            result = result * 10 + (s.charAt(i) - '0');

            // Early exit if overflow detected
            if (sign == 1 && result > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (sign == -1 && -result < Integer.MIN_VALUE) {
                return Integer.MIN_VALUE;
            }

            i++;
        }

        return (int) (sign * result);
    }

    // Approach 2: Without using long (overflow check before multiplication)
    public int myAtoiNoLong(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }

        int i = 0;
        int n = s.length();

        // Skip whitespace
        while (i < n && s.charAt(i) == ' ') {
            i++;
        }

        if (i == n) {
            return 0;
        }

        // Determine sign
        int sign = 1;
        if (s.charAt(i) == '-' || s.charAt(i) == '+') {
            sign = (s.charAt(i) == '-') ? -1 : 1;
            i++;
        }

        int result = 0;
        while (i < n && Character.isDigit(s.charAt(i))) {
            int digit = s.charAt(i) - '0';

            // Check for overflow before adding digit
            // If result > INT_MAX/10, then result*10 will overflow
            // If result == INT_MAX/10 and digit > 7, then result*10 + digit will overflow
            if (result > Integer.MAX_VALUE / 10 ||
                    (result == Integer.MAX_VALUE / 10 && digit > 7)) {
                return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }

            result = result * 10 + digit;
            i++;
        }

        return sign * result;
    }

    // Test cases
    public static void main(String[] args) {
        StringToIntegerSolution sol = new StringToIntegerSolution();

        // Test cases
        System.out.println(sol.myAtoi("42")); // 42
        System.out.println(sol.myAtoi("   -042")); // -42
        System.out.println(sol.myAtoi("1337c0d3")); // 1337
        System.out.println(sol.myAtoi("0-1")); // 0
        System.out.println(sol.myAtoi("words and 987")); // 0
        System.out.println(sol.myAtoi("-91283472332")); // -2147483648 (INT_MIN)
        System.out.println(sol.myAtoi("91283472332")); // 2147483647 (INT_MAX)
        System.out.println(sol.myAtoi("  +0 123")); // 0
        System.out.println(sol.myAtoi("")); // 0
        System.out.println(sol.myAtoi("   ")); // 0
        System.out.println(sol.myAtoi("-")); // 0
        System.out.println(sol.myAtoi("+")); // 0
        System.out.println(sol.myAtoi("  -0012a42")); // -12
    }
}

/**
 * Time Complexity: O(n) where n is the length of the string
 * Space Complexity: O(1) - only using constant extra space
 * 
 * Key Points:
 * 1. Handle edge cases: null, empty string, whitespace-only
 * 2. Skip leading whitespace
 * 3. Handle sign (only one '+' or '-' allowed)
 * 4. Read digits until non-digit or end of string
 * 5. Clamp result to [INT_MIN, INT_MAX] range
 * 
 * Integer Range:
 * INT_MIN = -2^31 = -2147483648
 * INT_MAX = 2^31 - 1 = 2147483647
 */
