/**
 * 0. ## Problem Statement
 * Design an algorithm that accepts a stream of integers and retrieves the product of the last k integers of the stream.
 * Implement `ProductOfNumbers`:
 * - `add(int num)`: Appends the integer to the stream.
 * - `getProduct(int k)`: Returns the product of the last k numbers in the current list.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `k` ever be greater than the total number of elements added so far?"
 *    -> Prevents IndexOutOfBounds. (Constraint says no, but knowing this avoids unnecessary bounds checks).
 * 2. "Will the final product or intermediate products exceed a 32-bit signed integer?"
 *    -> Prevents overflow bugs; tells us we don't need `long` or `BigInteger`.
 * 3. "Are the incoming numbers always positive?"
 *    -> The constraint 0 <= num <= 100 tells us we have to handle 0, but thankfully no negative numbers (which would make tracking max/min tricky in variations).
 * 4. "What is the expected read-to-write ratio?"
 *    -> If we call `getProduct` millions of times and `add` rarely, we heavily optimize `getProduct`. Here we want O(1) for both.
 * 5. "If a 0 is added, does it affect the products of future calls where `k` encompasses the 0?"
 *    -> Yes. Any product of a sequence containing 0 is strictly 0. This is the crucial mechanical hinge of the problem.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: A stream of up to 40,000 numbers and 40,000 product queries. A brute-force 
 * O(k) multiplication on every query will TLE (Time Limit Exceeded) because 40k * 40k = 1.6 billion operations.
 * 
 * --- Approach 1: The Brute Force List ---
 * Idea: Just keep all incoming numbers in an `ArrayList`. When `getProduct(k)` is called, loop through 
 * the last `k` elements and multiply them together.
 * - Why it works: Directly implements the problem statement.
 * - Why it's too slow/costly: We are repeatedly multiplying the same overlapping suffixes over and over.
 * Time Complexity: O(1) for `add`, O(k) for `getProduct` — because we iterate backward `k` times.
 * Space Complexity: O(N) — because we store all N elements in the list.
 * 
 * --- Approach 2: Standard Prefix Products ---
 * Idea: We know prefix sums allow O(1) range sums: `sum(i, j) = prefix[j] - prefix[i-1]`.
 * We can do the same for products: `product(last k) = prefix[last] / prefix[last - k]`.
 * We maintain a list where `prefix[i]` is the product of all elements up to `i`.
 * - Why it works: Removes the O(k) loop using basic division.
 * - Why it fails: Zero! If a `0` comes in, the prefix product becomes `0`, and all subsequent prefix 
 *   products become `0`. When we try to divide by a past prefix, we get a `DivideByZeroException`.
 * Time Complexity: O(1) for `add`, O(1) for `getProduct` (if no zeros existed).
 * Space Complexity: O(N) — because we store the prefix array.
 * 
 * --- Approach 3: Prefix Products with Zero-Reset (The Optimal Solution) ---
 * Idea: How do we fix the zero problem? By realizing that *any* product sequence containing a zero 
 * is just zero. Therefore, a zero acts as an absolute barrier.
 * If we see a 0, we simply clear out our prefix product list and start over with `[1]`.
 * When `getProduct(k)` is called, we check if `k` is greater than or equal to the size of our current 
 * prefix list. If it is, it means the sequence stretches back to or past the last zero we saw. So we just return 0.
 * Otherwise, we safely return `prefix[last] / prefix[last - k]`.
 * - Why it works: It completely bypasses storing 0s and avoids division by zero, leveraging the mathematical dominance of zero.
 * Time Complexity: O(1) for `add` — because appending to an ArrayList and multiplying is O(1).
 * Time Complexity: O(1) for `getProduct` — because size checking and division are O(1).
 * Space Complexity: O(N) — because we store at most N prefix products dynamically.
 * 
 * Verdict: Approach 3 is the undisputed optimal solution and the only one that satisfies the follow-up O(1) request. 
 * I would code Approach 3 immediately in an interview, clearly explaining the "zero reset" logic first.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Adding a `0`: Must gracefully reset the data structure.
 * - `k` exactly equals the number of elements added since the last 0: E.g., stream is `[0, 5, 2]`, `k=2`. 
 *   The prefix list needs a "dummy 1" at index 0 so we can correctly do `prefix[2] / prefix[0]` -> `10 / 1 = 10`.
 * - Consecutive zeros: E.g., `add(0), add(0)`. The logic handles this smoothly by repeatedly clearing.
 * - `k` strictly greater than the number of non-zero elements: Safely trapped and returns 0.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * A zero doesn't just ruin a prefix product array; it actually renders everything before it irrelevant 
 * for any future cross-boundary queries. The 0 is a "wall".
 * 
 * -- ASCII Diagram (Zero Reset Mechanism) --
 * Stream: [3, 0, 2, 5, 4], Dummy 1 is always prepended.
 * 
 * add(3): Prefix Array = [1, 3]
 * add(0): ZERO SEEN! -> Clear array -> Prefix Array = [1]
 * add(2): Prefix Array = [1, 2]
 * add(5): Prefix Array = [1, 2, 10]
 * add(4): Prefix Array = [1, 2, 10, 40]
 * 
 * getProduct(2): k=2. Size is 4. k < 4. return 40 / prefix[4-1-2] = 40 / prefix[1] = 40 / 2 = 20.
 * getProduct(4): k=4. Size is 4. k >= 4! We hit the zero-wall. Return 0.
 * 
 * -- Dry run --
 * Input: add(3), add(0), add(2), add(5), add(4), getProduct(2), getProduct(3), getProduct(4)
 * 1. Initialize: prefix = [1]
 * 2. add(3): num != 0. prefix.add(1 * 3). prefix = [1, 3]
 * 3. add(0): num == 0. Clear! prefix = [1]
 * 4. add(2): num != 0. prefix.add(1 * 2). prefix = [1, 2]
 * 5. add(5): num != 0. prefix.add(2 * 5). prefix = [1, 2, 10]
 * 6. add(4): num != 0. prefix.add(10 * 4). prefix = [1, 2, 10, 40]
 * 7. get(2): 2 < prefix.size(4). Return prefix(3) / prefix(3-2) = 40 / 2 = 20.
 * 8. get(3): 3 < prefix.size(4). Return prefix(3) / prefix(3-3) = 40 / 1 = 40.
 * 9. get(4): 4 >= prefix.size(4). Return 0. (Because k stretches into the cleared 0).
 * 
 * -- Pitfalls --
 * 1. Forgetting the Dummy `1`: If you don't seed the array with `1`, when you want the product of 
 *    the *entire* current non-zero block, your index calculation `size - 1 - k` will go out of bounds.
 * 2. Blind Prefix Array: Trying to implement standard prefix arrays and manually handling zero positions 
 *    using arrays of zero-counts. It's much harder to code and easy to get index offsets wrong.
 * 
 * -- Pattern recognition --
 * When you see "Query aggregate over last K elements / range [i,j] without updates": Think **Prefix Arrays**.
 * When you see "Product prefix arrays": Immediately think **"How do I handle 0?"**
 * 
 * -- Interview script --
 * "To do this in O(1), a prefix product array is perfect: product(last k) is `prefix[last] / prefix[last - k]`. 
 * But a zero destroys prefix products. The insight is that if a sequence contains a zero, the product is 0. 
 * So, anytime we see a 0, we can just wipe our prefix array and start fresh. If a query asks for more elements 
 * than we currently have in our fresh array, it means their requested range includes that zero, so we instantly 
 * return 0. This gives us O(1) time and O(N) space."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if the stream can also include point updates? (e.g., change the i-th added number to X)
 * A: Prefix arrays are static. If we update an element, we'd have to update all subsequent prefix values (O(N) time).
 *    To support O(log N) updates and O(log N) queries, we would need to switch to a Segment Tree or a Fenwick Tree 
 *    (Binary Indexed Tree). 
 * 
 * Q: What if the products could exceed 32-bit integers, but we still need exact values?
 * A: We'd have to use Java's `BigInteger`. However, `BigInteger` division isn't strictly O(1) (it scales with 
 *    the number of digits). The time complexity would quietly degrade to O(D^2) where D is the number of digits.
 * 
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.List;

public class ProductOfNumbersStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Prefix Products with Zero-Reset
    // ----------------------------------------------------------------------
    static class ProductOfNumbers {
        
        // This list will store the continuous running product of elements.
        // E.g., if we add 2, 5, 4, prefix becomes [1, 2, 10, 40]
        // The first element is always a dummy 1.
        private List<Integer> prefix;

        public ProductOfNumbers() {
            prefix = new ArrayList<>();
            // The dummy '1' ensures that if we want the product of all elements
            // since the last zero, we can cleanly divide by 1.
            prefix.add(1);
        }
        
        public void add(int num) {
            if (num == 0) {
                // The Zero-Wall. A zero destroys all previous context for products.
                // We wipe the slate clean.
                prefix = new ArrayList<>();
                prefix.add(1);
            } else {
                // Otherwise, multiply the incoming number with the last prefix product
                // and append it to the list.
                int lastProduct = prefix.get(prefix.size() - 1);
                prefix.add(lastProduct * num);
            }
        }
        
        public int getProduct(int k) {
            int n = prefix.size();
            // If k is greater than or equal to the size of our prefix array,
            // it means the user's requested window reaches back into a '0' that we 
            // previously cleared. Any product containing that '0' is exactly 0.
            if (k >= n) {
                return 0;
            }
            
            // Mathematically: Product of last k = (Product of all) / (Product up to last k)
            // Example: prefix = [1, 2, 10, 40]. n = 4. 
            // getProduct(2) -> prefix[3] / prefix[4 - 1 - 2] = prefix[3] / prefix[1]
            // -> 40 / 2 = 20.
            return prefix.get(n - 1) / prefix.get(n - 1 - k);
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        ProductOfNumbers productOfNumbers = new ProductOfNumbers();
        
        System.out.println("Adding 3, 0, 2, 5, 4...");
        productOfNumbers.add(3);        // [3] -> prefix: [1, 3]
        productOfNumbers.add(0);        // [3,0] -> ZERO! prefix resets: [1]
        productOfNumbers.add(2);        // [3,0,2] -> prefix: [1, 2]
        productOfNumbers.add(5);        // [3,0,2,5] -> prefix: [1, 2, 10]
        productOfNumbers.add(4);        // [3,0,2,5,4] -> prefix: [1, 2, 10, 40]
        
        // return 20 (5 * 4)
        System.out.println("getProduct(2): " + productOfNumbers.getProduct(2) + " (Expected: 20)"); 
        
        // return 40 (2 * 5 * 4)
        System.out.println("getProduct(3): " + productOfNumbers.getProduct(3) + " (Expected: 40)"); 
        
        // return 0 (0 * 2 * 5 * 4)
        // Internally: k (4) >= prefix.size() (4), so returns 0 instantly.
        System.out.println("getProduct(4): " + productOfNumbers.getProduct(4) + " (Expected: 0)"); 
        
        System.out.println("Adding 8...");
        productOfNumbers.add(8);        // [3,0,2,5,4,8] -> prefix: [1, 2, 10, 40, 320]
        
        // return 32 (4 * 8)
        System.out.println("getProduct(2): " + productOfNumbers.getProduct(2) + " (Expected: 32)"); 
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Prefix Products.
 * - Key observation: Zero breaks prefix multiplication because it propagates unconditionally. 
 *   We use this behavior to our advantage by letting 0 reset the state instead of ruining it.
 * - Worth memorizing: Prepending a dummy `1` to prefix arrays. It eliminates boundary 
 *   out-of-bounds checks and allows `prefix[i] / prefix[0]` to work naturally.
 * - Most common trap: Trying to store zeros and counting them to bypass division by zero.
 * - One-line mental trigger: "Product of last K? Prefix division, reset on zero."
 * =========================================================================================
 */








































import java.util.ArrayList;
import java.util.List;

class ProductOfNumbers {

    private final List<Integer> prefix;

    public ProductOfNumbers() {

        prefix = new ArrayList<>();

        // Sentinel value.
        //
        // prefix[0] = 1 allows us to calculate products
        // without special handling for ranges starting at index 0.
        //
        // It also represents an empty product, which is mathematically 1.
        prefix.add(1);
    }

    public void add(int num) {

        // CASE 1: A zero is encountered.
        if (num == 0) {

            // Any product containing this zero must be zero.
            //
            // Previous prefix products are no longer useful
            // because no valid non-zero suffix can cross this zero.
            //
            // Resetting also ensures that future queries
            // never perform division by zero.
            prefix.clear();

            // Start a fresh prefix-product sequence.
            prefix.add(1);

            return;
        }

        // CASE 2: Non-zero number.
        //
        // The latest prefix product represents the product
        // of all numbers since the most recent zero.
        int previousProduct = prefix.get(prefix.size() - 1);

        // Append the new cumulative product.
        //
        // Example:
        // prefix = [1, 2, 6]
        // add(4)
        // prefix = [1, 2, 6, 24]
        prefix.add(previousProduct * num);
    }

    public int getProduct(int k) {

        int n = prefix.size();

        // If k >= n, the requested range contains at least
        // one zero.
        //
        // Why?
        //
        // prefix contains:
        // [1, products after the most recent zero]
        //
        // Therefore, n - 1 is the number of non-zero elements
        // after the most recent zero.
        //
        // If k is greater than that count, the requested
        // suffix must include a zero.
        if (k >= n) {
            return 0;
        }

        // Both prefix indices are guaranteed to be valid.
        //
        // prefix[n - 1]     = product of all elements after last zero.
        // prefix[n - 1 - k] = product before the last k elements.
        //
        // Dividing cancels the earlier elements.
        //
        // Example:
        // prefix = [1, 2, 6, 24, 120]
        // k = 2
        //
        // answer = 120 / 6 = 20
        return prefix.get(n - 1) / prefix.get(n - 1 - k);
    }

    /*
     * TIME COMPLEXITY:
     *
     * add(num):
     * O(1) amortized.
     *
     * - Reading the last prefix product takes O(1).
     * - Appending to ArrayList takes O(1) amortized.
     * - Resetting the prefix list after zero is O(1) here,
     *   because ArrayList.clear() clears its logical elements
     *   without individually traversing them.
     *
     * getProduct(k):
     * O(1).
     *
     * - Checking the prefix list size takes O(1).
     * - Two ArrayList.get() operations take O(1).
     * - Integer division takes O(1).
     *
     * SPACE COMPLEXITY:
     * O(m), where m is the number of non-zero elements
     * after the most recent zero.
     *
     * Worst case:
     * O(n), if no zero ever appears.
     *
     * IMPORTANT:
     * The problem guarantees that the product of the entire
     * stream fits in a signed 32-bit integer.
     * Therefore, int is sufficient for the stored products.
     */
}

