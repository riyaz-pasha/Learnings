/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * You have a browser of one tab where you start on the homepage and can visit 
 * another url, get back in the history number of steps, or move forward in the 
 * history number of steps.
 * 
 * Implement the BrowserHistory class:
 * - BrowserHistory(string homepage): Initializes with the homepage.
 * - void visit(string url): Visits url from the current page. Clears forward history.
 * - string back(int steps): Move steps back. Returns the current URL.
 * - string forward(int steps): Move steps forward. Returns the current URL.
 * 
 * Constraints:
 * - 1 <= homepage.length, url.length <= 20
 * - 1 <= steps <= 100
 * - At most 5000 calls will be made to visit, back, and forward.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Are we keeping this history indefinitely in memory, or is there a maximum limit?
 *    (Real browsers truncate history after N entries. If there's a limit, a circular array is better).
 * 2. Does `visit` to the exact same URL we are currently on add a new history state?
 *    (The prompt implies yes, standard string duplication is fine, but good to clarify).
 * 3. What is the ratio of `visit` vs `back`/`forward` operations?
 *    (If `back`/`forward` involves jumping large numbers of steps frequently, O(1) random access is critical).
 * 4. Are we operating in a multi-threaded environment where tabs share history?
 *    (Assuming single-threaded, avoiding the need for `ReentrantReadWriteLock`).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Maintain a chronological sequence of visited states where 
 *   jumping backward and forward is possible, and branching (visiting a new page 
 *   from the middle of history) truncates the "future".
 * 
 * --- Approach 1: Two Stacks (The Naive Simulation) ---
 * 1. Intuition: Use a `backStack` and a `forwardStack`, plus a `currentUrl` variable.
 * 2. Try: 
 *    `visit`: push `currentUrl` to `backStack`, update `currentUrl`, clear `forwardStack`.
 *    `back(steps)`: pop from `backStack`, push `currentUrl` to `forwardStack`, repeat `steps` times.
 * 3. Why it works: It perfectly mirrors the mental model of pressing the back/forward buttons.
 * 4. Why it's too costly: Moving back 100 steps requires popping and pushing 100 times. 
 * 5. Bottleneck: O(S) time complexity for navigation, where S is the number of steps.
 * 
 * Time Complexity: visit: O(1). back/forward: O(S) where S is steps.
 * Space Complexity: O(N) where N is total visited URLs.
 * 
 * --- Approach 2: Doubly Linked List (The Pointers Method) ---
 * 1. Intuition: Stacks are slow for deep traversal. A doubly linked list maps to history perfectly.
 * 2. Try: Create a `Node(url, prev, next)`. Keep a `curr` pointer.
 *    `visit`: create a new node, `curr.next = newNode`, `newNode.prev = curr`, then move `curr = newNode`. (The old forward history is naturally severed and garbage collected).
 *    `back/forward`: traverse `.prev` or `.next` in a while loop `steps` times.
 * 3. Why it works: Extremely clean `visit` logic. No manual clearing of forward history needed.
 * 4. Why it's still suboptimal: We STILL have to traverse node-by-node. Jumping 100 steps takes 100 pointer hops.
 * 5. Bottleneck: Lack of random access. O(S) traversal time.
 * 
 * Time Complexity: visit: O(1). back/forward: O(S).
 * Space Complexity: O(N) plus object overhead for DLL nodes.
 * 
 * --- Approach 3: Dynamic Array with Index Pointers (The Optimal Method) ---
 * 1. Intuition: What data structure provides O(1) random access? An Array. 
 *    Instead of physically deleting forward history, we just maintain a `bound` index!
 * 2. Try: `ArrayList<String> history`. Variables `curr` and `bound`.
 *    `visit`: increment `curr`. Overwrite index `curr` if it exists, else append. Set `bound = curr`.
 *    `back(steps)`: `curr = Math.max(0, curr - steps)`.
 *    `forward(steps)`: `curr = Math.min(bound, curr + steps)`.
 * 3. Why it works: Math replaces traversal. We jump instantly to the target index.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It reduces time complexity from O(S) to strict O(1) for all operations and 
 *    avoids the memory overhead of Linked List nodes.
 * 
 * Time Complexity: O(1) for ALL operations.
 * Space Complexity: O(N) — storing N strings in an array, re-using indices on new visits.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Moving back more steps than history exists: `Math.max(0, curr - steps)` safely bounds it to 0.
 * - Moving forward more steps than history allows: `Math.min(bound, curr + steps)` clamps to `bound`.
 * - Visiting a new URL from the middle of history: Overwrites the array at `curr + 1` and updates `bound`, 
 *   leaving the garbage URLs physically in the array but logically unreachable (lazy deletion).
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Array overwriting replaces physical deletion." 
 *   When a user clicks "back" 5 times and visits a new page, the 5 forward pages are lost. 
 *   Instead of resizing the array or deleting them, we just update a `bound` variable. 
 *   Future visits will naturally overwrite those slots.
 * 
 * ASCII DRY RUN (Array + Pointers)
 * 1. Init("leetcode.com")
 *    Arr: ["leetcode.com"]
 *    curr = 0, bound = 0
 * 
 * 2. visit("google.com"), visit("facebook.com")
 *    Arr: ["leetcode.com", "google.com", "facebook.com"]
 *    curr = 2, bound = 2
 * 
 * 3. back(1)
 *    curr = Math.max(0, 2 - 1) = 1
 *    Arr: ["leetcode.com", "google.com", "facebook.com"]
 *    curr = 1, bound = 2  (We are at google.com)
 * 
 * 4. visit("linkedin.com")
 *    curr = 2. Overwrite index 2!
 *    Arr: ["leetcode.com", "google.com", "linkedin.com"]
 *    curr = 2, bound = 2
 *    (Notice "facebook.com" is gone effortlessly).
 * 
 * Pitfalls:
 * - Truncating the `ArrayList` manually: e.g. `list.subList(curr + 1, list.size()).clear()`. 
 *   This is an O(N) operation in Java! Modifying a `bound` variable is O(1) and achieves the exact same logical result.
 * 
 * Pattern Recognition: "Chronological history with jumps and branching" -> Array with clamping bounds.
 * 
 * Interview Script snippet:
 * "While a Doubly Linked List maps intuitively to browser history, jumping `N` steps backward 
 * takes O(N) time. To optimize this, I'll use an ArrayList to get O(1) random access. I'll maintain 
 * a `current` index and a `bound` index. Jumping back and forth becomes simple clamped addition 
 * and subtraction. When visiting a new page, I just increment `current`, overwrite the array at 
 * that index, and update `bound`, achieving O(1) across the board."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you optimize memory if the browser runs for months and visits 10 million pages?
 * A1: The ArrayList will grow indefinitely, consuming massive memory. Real browsers cap history 
 *     (e.g., max 1000 items). I would replace the `ArrayList` with a fixed-size `String[]` of size 1000, 
 *     and treat it as a Circular Array (using modulo `% 1000`). If `curr` wraps around and hits 
 *     the oldest item, the oldest item is naturally overwritten.
 * 
 * Q2: What if we needed to clear all history older than 1 hour?
 * A2: We would store an object `Node { url, timestamp }`. To clear, we could use Binary Search 
 *     to find the index of the 1-hour mark, and shift elements, or maintain a secondary queue 
 *     to track expirations.
 */

import java.util.ArrayList;
import java.util.List;

public class BrowserHistoryStudy {

    static class BrowserHistory {
        
        // The dynamic array holding our history timeline
        private final List<String> history;
        
        // The index of the page we are currently viewing
        private int curr;
        
        // The logical end of our forward history.
        // Everything past 'bound' is considered deleted garbage.
        private int bound;

        /**
         * Initializes the object with the homepage of the browser.
         * Time: O(1)
         */
        public BrowserHistory(String homepage) {
            history = new ArrayList<>();
            history.add(homepage);
            curr = 0;
            bound = 0;
        }

        /**
         * Visits url from the current page. It clears up all the forward history.
         * Time: O(1) amortized
         */
        public void visit(String url) {
            // Move forward to the new page slot
            curr++;
            
            // If our list is large enough, we overwrite the "garbage" forward history.
            // This is strictly O(1) and avoids the O(N) cost of list truncation.
            if (curr < history.size()) {
                history.set(curr, url);
            } else {
                // We are at the physical end of the allocated array, so we append.
                history.add(url);
            }
            
            // Visiting a new page establishes the new physical boundary of the "future"
            bound = curr;
        }

        /**
         * Move steps back in history. 
         * Time: O(1)
         */
        public String back(int steps) {
            // Clamped subtraction: Ensure we don't go below index 0
            curr = Math.max(0, curr - steps);
            return history.get(curr);
        }

        /**
         * Move steps forward in history. 
         * Time: O(1)
         */
        public String forward(int steps) {
            // Clamped addition: Ensure we don't go past our logical forward bound
            curr = Math.min(bound, curr + steps);
            return history.get(curr);
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        BrowserHistory browserHistory = new BrowserHistory("leetcode.com");
        
        // Visit standard pages
        browserHistory.visit("google.com");       // [leetcode.com, google.com*]
        browserHistory.visit("facebook.com");     // [leetcode.com, google.com, facebook.com*]
        browserHistory.visit("youtube.com");      // [leetcode.com, google.com, facebook.com, youtube.com*]
        
        // Back 1
        System.out.println("Back 1: " + browserHistory.back(1)); // Expected: "facebook.com"
        
        // Back 1 again
        System.out.println("Back 1: " + browserHistory.back(1)); // Expected: "google.com"
        
        // Forward 1
        System.out.println("Forward 1: " + browserHistory.forward(1)); // Expected: "facebook.com"
        
        // Visit a new page from the middle of history
        // This overwrites "youtube.com" logically
        browserHistory.visit("linkedin.com");     // [leetcode.com, google.com, facebook.com, linkedin.com*]
        
        // Forward 2 - Can't go forward, we are at the bound
        System.out.println("Forward 2: " + browserHistory.forward(2)); // Expected: "linkedin.com"
        
        // Back 2
        System.out.println("Back 2: " + browserHistory.back(2)); // Expected: "google.com"
        
        // Back 7 - Clamped to 0
        System.out.println("Back 7: " + browserHistory.back(7)); // Expected: "leetcode.com"
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: ArrayList + Bounding Pointers.
 * Key Observation: Jumping steps backward and forward is a mathematical operation, 
 * not a physical traversal one. Therefore, arrays completely outclass linked lists 
 * for this problem. Forward history truncation can be simulated purely by maintaining 
 * a maximum bound index, achieving strict O(1) performance across all methods.
 * What to Memorize: `curr = Math.max(0, curr - steps)` and `curr = Math.min(bound, curr + steps)`.
 * Most Common Trap: Using `history.subList(curr + 1, size).clear()` during `visit`. 
 * While logically correct, physical deletion shifts memory or forces array bounds 
 * checking underneath the hood, degrading performance compared to simple overwriting.
 * Mental Trigger: "History traversal + Jumps" -> "Array with Math.max/min clamping".
 * ============================================================================
 */









































import java.util.ArrayDeque;
import java.util.Deque;

class BrowserHistory {

    // The top of backStack is the immediately previous page.
    private final Deque<String> backStack = new ArrayDeque<>();

    // The top of forwardStack is the immediately next page.
    private final Deque<String> forwardStack = new ArrayDeque<>();

    private String current;

    public BrowserHistory(String homepage) {
        current = homepage;
    }

    public void visit(String url) {
        // The old current page becomes the previous page.
        backStack.push(current);

        // Visiting a new URL invalidates every forward entry.
        forwardStack.clear();

        // Display the new URL.
        current = url;
    }

    public String back(int steps) {
        // Move backward until we exhaust the requested steps
        // or reach the homepage (an empty backStack).
        while (steps-- > 0 && !backStack.isEmpty()) {
            // The page we leave becomes the next forward page.
            forwardStack.push(current);

            // The most recent previous page becomes current.
            current = backStack.pop();
        }

        return current;
    }

    public String forward(int steps) {
        // Move forward until we exhaust the requested steps
        // or there are no more forward pages.
        while (steps-- > 0 && !forwardStack.isEmpty()) {
            // The page we leave becomes available for going back.
            backStack.push(current);

            // The closest forward page becomes current.
            current = forwardStack.pop();
        }

        return current;
    }
}

/*
 * COMPLEXITY
 *
 * visit(url):
 *   O(F) time in the worst case because forwardStack.clear()
 *   discards F forward entries. Other stack operations are O(1).
 *
 * back(steps):
 *   O(min(steps, B)) time, where B is the number of previous pages.
 *
 * forward(steps):
 *   O(min(steps, F)) time, where F is the number of forward pages.
 *
 * Space: O(H), where H is the total number of retained history entries.
 *
 * This is a natural model when you think in terms of undo/redo:
 * going backward moves a page from one stack to the other, while
 * making a new visit clears the redo stack.
 */










































class BrowserHistory {

    // Each page knows its immediate predecessor and successor.
    private static final class Page {
        final String url;
        Page previous;
        Page next;

        Page(String url) {
            this.url = url;
        }
    }

    private Page current;

    public BrowserHistory(String homepage) {
        current = new Page(homepage);
    }

    public void visit(String url) {
        // Discard the entire forward chain.
        // Detaching the first forward node makes it unreachable
        // from the current browsing path.
        current.next = null;

        // Append the new page after the current page.
        Page nextPage = new Page(url);
        nextPage.previous = current;
        current.next = nextPage;

        // The new page is now displayed.
        current = nextPage;
    }

    public String back(int steps) {
        // Follow previous pointers until the requested distance
        // is covered or the homepage boundary is reached.
        while (steps-- > 0 && current.previous != null) {
            current = current.previous;
        }

        return current.url;
    }

    public String forward(int steps) {
        // Follow next pointers until the requested distance
        // is covered or no next page exists.
        while (steps-- > 0 && current.next != null) {
            current = current.next;
        }

        return current.url;
    }
}

/*
 * COMPLEXITY
 *
 * visit(url):
 *   O(1) time, excluding garbage collection of discarded nodes.
 *   A single pointer update disconnects the forward chain.
 *
 * back(steps):
 *   O(min(steps, B)) time, where B is the available backward distance.
 *
 * forward(steps):
 *   O(min(steps, F)) time, where F is the available forward distance.
 *
 * Space: O(H) for the nodes in the active history.
 *
 * Advantage:
 *   - Visiting a page is O(1), with no loop to delete forward entries.
 *   - Previous/next navigation is directly represented by pointers.
 *
 * Trade-off:
 *   - More code and pointer management.
 *   - Back and forward require traversal rather than a direct index jump.
 *   - Discarded nodes become eligible for garbage collection, but
 *     reclaiming their memory is not guaranteed to happen immediately.
 */

