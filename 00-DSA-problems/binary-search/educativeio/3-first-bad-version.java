import java.util.stream.IntStream;

/**
 * Mock API Base Class (Simulates the environment for the problem)
 */
class VersionControl {
    // This variable simulates the backend system knowing the first bad version.
    protected int actualFirstBadVersion;

    public void setFirstBadVersion(int badVersion) {
        this.actualFirstBadVersion = badVersion;
    }

    /**
     * The mock API provided by the problem statement.
     * @param version the version number to check
     * @return true if the version is bad, false otherwise
     */
    public boolean isBadVersion(int version) {
        return version >= actualFirstBadVersion;
    }
}

/**
 * Problem Statement:
 * You have `n` versions [1, 2, ..., n]. If a version is bad, all versions after it are bad.
 * Find the FIRST bad version using the `isBadVersion(version)` API.
 * Minimize the number of API calls.
 * 
 * Constraints:
 * 1 <= bad <= n <= 10^5
 */
class FirstBadVersionSolutions extends VersionControl {

    /**
     * SOLUTION 1: Iterative Binary Search (Optimal)
     * 
     * Time Complexity: O(log N) - Minimizes API calls by halving the search space.
     * Space Complexity: O(1) - Constant space used.
     * 
     * VISUAL EXPLANATION:
     * n = 5, First Bad Version = 4
     * Array conceptually looks like: [F, F, F, T, T] (F = Good, T = Bad)
     * 
     * Iteration 1:
     * [ 1(F), 2(F), 3(F), 4(T), 5(T) ]   result = -1
     *   L             M           H
     * mid = 3, isBadVersion(3) == false.
     * All versions before and including 3 are good. Look right. L = 4.
     * 
     * Iteration 2:
     * [ 1(F), 2(F), 3(F), 4(T), 5(T) ]   result = -1
     *                       L/M   H
     * mid = 4, isBadVersion(4) == true.
     * This is a bad version, so store it as a potential answer (result = 4).
     * To see if it's the *first* bad version, look left. H = 3.
     * 
     * Loop ends because L (4) > H (3). 
     * Final Result: 4
     */
    public int firstBadVersionIterative(int n) {
        int low = 1;
        int high = n;
        
        int result = -1; // Explicit result variable as requested

        while (low <= high) {
            // Safe calculation to prevent integer overflow for large 'n'
            int mid = low + (high - low) / 2;

            if (isBadVersion(mid)) {
                result = mid;   // Record current bad version
                high = mid - 1; // Discard right half, search left for an earlier one
            } else {
                low = mid + 1;  // Discard left half, search right
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search (O(log N))
     * 
     * Time Complexity: O(log N)
     * Space Complexity: O(log N) - Call stack overhead.
     * 
     * EXPLANATION:
     * Translates the optimal iterative approach into a recursive one.
     * The `result` is passed along through the recursive calls.
     */
    public int firstBadVersionRecursiveWrapper(int n) {
        return firstBadVersionRecursive(1, n, -1);
    }

    private int firstBadVersionRecursive(int low, int high, int currentResult) {
        int result = currentResult; // Explicit result variable

        if (low > high) {
            return result; // Base case: search space is exhausted
        }

        int mid = low + (high - low) / 2;

        if (isBadVersion(mid)) {
            // Found a bad version, save it and search left
            result = firstBadVersionRecursive(low, mid - 1, mid);
        } else {
            // Version is good, carry current result and search right
            result = firstBadVersionRecursive(mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Linear Scan using Java Streams (Sub-optimal)
     * 
     * Time Complexity: O(N) - Makes an API call for every version until it hits a bad one.
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Showcases modern Java Streams logic. Checks versions strictly 1 by 1.
     * While correct, this will result in too many API calls for large 'n' 
     * and would likely cause a "Time Limit Exceeded" error on a coding platform.
     */
    public int firstBadVersionStream(int n) {
        int result = IntStream.rangeClosed(1, n)
                .filter(this::isBadVersion) // Method reference to the API
                .findFirst()
                .orElse(-1);
                
        return result;
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases.
     * n = total versions, expected = the actual first bad version.
     */
    public record TestCase(int n, int expected) {}

    public static void main(String[] args) {
        FirstBadVersionSolutions solver = new FirstBadVersionSolutions();

        TestCase[] testCases = {
            new TestCase(5, 4),           // Standard case
            new TestCase(1, 1),           // Single version, it is bad
            new TestCase(100000, 1),      // First version is bad in a large set
            new TestCase(100000, 100000), // Last version is bad in a large set
            new TestCase(10, 7)           // Random middle case
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            // Configure the mock API for this specific test case
            solver.setFirstBadVersion(tc.expected());
            
            // Run all solution variations
            int resIterative = solver.firstBadVersionIterative(tc.n());
            int resRecursive = solver.firstBadVersionRecursiveWrapper(tc.n());
            int resStream    = solver.firstBadVersionStream(tc.n());

            boolean passed = (resIterative == tc.expected()) &&
                             (resRecursive == tc.expected()) &&
                             (resStream == tc.expected());

            System.out.printf("Test %d | Total Versions: %-6d | Expected First Bad: %-6d | Passed: %b%n",
                    i + 1, tc.n(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] Iterative: %d, Recursive: %d, Stream: %d%n",
                        resIterative, resRecursive, resStream);
            }
        }
    }
}
