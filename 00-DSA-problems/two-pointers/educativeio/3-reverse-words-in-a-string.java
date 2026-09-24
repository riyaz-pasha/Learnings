/*
================================================================================
    GOOGLE DSA INTERVIEW: REVERSE WORDS IN A STRING
    Complete Walkthrough with All Solutions & Analysis
================================================================================

TABLE OF CONTENTS:
  Line    50  - Section 1:  Problem Restatement
  Line   100  - Section 2:  Clarifying Questions & Assumed Answers
  Line   160  - Section 3:  Examples & Edge Cases
  Line   230  - Section 4:  All Possible Approaches
  Line   670  - Section 5:  Approaches Comparison Table
  Line   710  - Section 6:  Recommended Approach for Interview
  Line   760  - Section 7:  Deep Dive - Optimal Solution
  Line   830  - Section 8:  Dry Run / Trace Example
  Line   920  - Section 9:  Closing Summary & Trade-offs
  Line   990  - Section 10: Follow-Up Questions
  Line  1040  - Section 11: What Candidates Typically Miss
  Line  1100  - Section 12: Test Cases & Main Method

================================================================================
 SECTION 1: PROBLEM RESTATEMENT
================================================================================

PROBLEM:
Given a string `sentence` that may contain:
  - Leading or trailing spaces
  - Multiple spaces between words
  
TASK:
Reverse the order of words in the sentence without changing the order of
characters within each word.

KEY CONSTRAINTS & REQUIREMENTS:
  - Input:  A string that may have leading/trailing/multiple spaces
  - Output: A single string with words in reversed order
  - Rules:
      * A word is a continuous sequence of non-space characters
      * Words must be separated by exactly ONE space
      * No leading or trailing spaces in the result
      * Characters within each word remain unchanged (only word order reverses)
  
EXAMPLE:
  Input:  "  hello   world  " 
  Output: "world hello"

IMPLICIT ASSUMPTIONS:
  - Words can be any characters (letters, digits, special chars) as long as
    they are not spaces
  - An empty string or a string with only spaces should return an empty string
  - Case sensitivity is preserved
  - This is NOT about reversing character order within the entire string

*/


/*
================================================================================
 SECTION 2: CLARIFYING QUESTIONS & ASSUMED ANSWERS
================================================================================

Q1: What is the maximum length of the input string?
A1: Assume up to 10^6 characters. This affects whether we can use extra space.

Q2: Can the input string be null?
A2: No, but it can be empty or contain only spaces. Handle gracefully.

Q3: Are there any constraints on what constitutes a "word"?
A3: A word is any continuous sequence of non-space characters. This includes
    digits, punctuation, and special characters (e.g., "hello," and "world!"
    are separate words).

Q4: Can there be Unicode/multi-byte characters?
A4: Yes, treat them as regular characters. Java String handles this natively.

Q5: Is the output expected to be a new string or can we modify input?
A5: Return a new string. The input string is immutable in Java anyway.

Q6: Can we use extra space (i.e., is space complexity a hard constraint)?
A6: Extra space is allowed. O(n) space is acceptable in practice.

Q7: Do we need to handle concurrent access or thread safety?
A7: No, assume single-threaded context.

Q8: Should we optimize for time or space, or balance both?
A8: Prefer a balanced approach: O(n) time, O(n) space is ideal. Time is more
    critical than space in most interview scenarios.

*/


/*
================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
================================================================================

EXAMPLE 1: Normal Case with Multiple Spaces
  Input:  "  hello   world  "
  Output: "world hello"
  
  Explanation:
    - Words extracted: ["hello", "world"]
    - Reversed order: ["world", "hello"]
    - Joined with single spaces: "world hello"
    - No leading/trailing spaces

EXAMPLE 2: Single Word
  Input:  "hello"
  Output: "hello"
  
  Explanation:
    - Only one word, so reversing order returns the same string

EXAMPLE 3: Empty String or Only Spaces (Edge Case)
  Input:  "   " or ""
  Output: ""
  
  Explanation:
    - No words extracted, return empty string

EXAMPLE 4: Words with Punctuation and Digits (Boundary Case)
  Input:  "the123  quick-brown  fox!"
  Output: "fox! quick-brown the123"
  
  Explanation:
    - Each continuous non-space sequence is a word
    - "the123", "quick-brown", and "fox!" are three separate words

EXAMPLE 5: Single Space Between Words (Minimal Case)
  Input:  "a b c"
  Output: "c b a"
  
  Explanation:
    - Three words, reverse them
    - Result has single spaces between each

*/


/*
================================================================================
 SECTION 4: ALL POSSIBLE APPROACHES
================================================================================

Approach 1: Split, Reverse Array, and Join (Built-in Methods)
───────────────────────────────────────────────────────────────

Core Idea:
  1. Split the string by spaces (handling empty strings from multiple spaces)
  2. Reverse the resulting array of words
  3. Join them back with single spaces

Data Structures & Algorithm:
  - String.split() with regex: O(n)
  - ArrayList or array reversal: O(w) where w = number of words
  - String.join(): O(n)

Implementation:
*/

class Solution1_SplitReverseJoin {
    public String reverseWords(String sentence) {
        // Split by one or more spaces, which removes empty strings
        // The regex "\\s+" matches one or more whitespace characters
        String[] words = sentence.trim().split("\\s+");
        
        // Reverse the words array in-place
        int left = 0, right = words.length - 1;
        while (left < right) {
            String temp = words[left];
            words[left] = words[right];
            words[right] = temp;
            left++;
            right--;
        }
        
        // Join with single space
        return String.join(" ", words);
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - trim():       O(n)
  - split():      O(n) - scans entire string to find delimiters
  - reverse:      O(w) where w = number of words, w <= n
  - join():       O(n)
  Total: O(n)

Space Complexity: O(n)
  - words array:  O(w) for word count, each word is O(1) to O(n)
  - String objects stored in array
  Total: O(n)

Pros:
  + Very simple and readable code
  + Leverages well-tested Java built-ins
  + Easy to understand and debug
  + Handles edge cases naturally (trim removes leading/trailing spaces)

Cons:
  - Creates multiple intermediate String objects (split creates array)
  - O(n) extra space due to string storage
  - Regex operations can have overhead for small strings
  - Not ideal for memory-constrained environments

When to Use:
  + Interview setting where clarity is paramount
  + Quick prototyping
  + When performance is not critical
  - NOT suitable if space complexity must be o(n)
  - NOT suitable if string immutability is problematic
*/


/*
Approach 2: Use Stack (LIFO Data Structure)
────────────────────────────────────────────

Core Idea:
  1. Iterate through the string and identify words
  2. Push each word onto a stack
  3. Pop words off the stack (LIFO gives reverse order)
  4. Join them with spaces

Data Structures & Algorithm:
  - Stack (or Deque): Last-In-First-Out behavior
  - Linear scan of input string: O(n)

Implementation:
*/

import java.util.*;

class Solution2_Stack {
    public String reverseWords(String sentence) {
        Stack<String> stack = new Stack<>();
        StringBuilder currentWord = new StringBuilder();
        
        // Traverse the string and build words
        for (int i = 0; i < sentence.length(); i++) {
            char ch = sentence.charAt(i);
            
            if (ch != ' ') {
                // Non-space character, add to current word
                currentWord.append(ch);
            } else {
                // Space encountered
                if (currentWord.length() > 0) {
                    // We have a complete word, push it to stack
                    stack.push(currentWord.toString());
                    currentWord = new StringBuilder();
                }
                // Skip multiple spaces (do nothing)
            }
        }
        
        // Don't forget the last word if it exists
        if (currentWord.length() > 0) {
            stack.push(currentWord.toString());
        }
        
        // Pop words from stack (gives reverse order) and build result
        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
            if (!stack.isEmpty()) {
                result.append(" ");
            }
        }
        
        return result.toString();
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - Single pass through string: O(n)
  - Stack operations (push/pop): O(w) where w = number of words, w <= n
  - StringBuilder append: O(1) amortized
  Total: O(n)

Space Complexity: O(n)
  - Stack stores w words: O(w) = O(n) in worst case (one char per word)
  - StringBuilder for result: O(n)
  Total: O(n)

Pros:
  + Conceptually clear: stack naturally reverses order
  + No need for trim() or regex
  + Handles multiple spaces automatically

Cons:
  - More verbose than split/join approach
  - Still requires O(n) extra space for stack and StringBuilder
  - Stack overhead for each word object

When to Use:
  + Educational purpose (demonstrates stack LIFO behavior)
  + Interview to show understanding of data structures
  - NOT more efficient than split/join
  - NOT suitable for memory-constrained environments
*/


/*
Approach 3: Two-Pointer In-Place Reversal (Character Level)
───────────────────────────────────────────────────────────

Core Idea:
  1. Convert string to character array (needed for in-place operations)
  2. Reverse the ENTIRE character array
  3. Reverse EACH WORD individually to restore character order within words
  4. Clean up extra spaces

Example Walkthrough:
  Input: "  hello   world  "
  
  Step 1: Trim and convert to array: ['h','e','l','l','o','w','o','r','l','d']
  Step 2: Reverse entire array:     ['d','l','r','o','w','o','l','l','e','h']
  Step 3: Reverse each word:        ['w','o','r','l','d','h','e','l','l','o']
  Step 4: Join with spaces:         "world hello"

Data Structures & Algorithm:
  - Character array for in-place manipulation
  - Two-pointer technique for reversal

Implementation:
*/

class Solution3_TwoPointerReversal {
    public String reverseWords(String sentence) {
        // Convert to character array (easier for in-place operations)
        char[] chars = sentence.toCharArray();
        
        // Step 1: Reverse the entire array
        reverse(chars, 0, chars.length - 1);
        
        // Step 2: Reverse each individual word
        int start = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != ' ') {
                // We are inside a word
                if (i == chars.length - 1 || chars[i + 1] == ' ') {
                    // End of a word (either last char or space after)
                    reverse(chars, start, i);
                }
            } else {
                // Space character
                start = i + 1;
            }
        }
        
        // Step 3: Clean up: remove extra spaces and build result
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < chars.length) {
            if (chars[i] != ' ') {
                // Start of a word
                int wordStart = i;
                while (i < chars.length && chars[i] != ' ') {
                    i++;
                }
                // Add word to result
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(new String(chars, wordStart, i - wordStart));
            } else {
                i++;
            }
        }
        
        return result.toString();
    }
    
    // Helper method to reverse a portion of the array
    private void reverse(char[] chars, int start, int end) {
        while (start < end) {
            char temp = chars[start];
            chars[start] = chars[end];
            chars[end] = temp;
            start++;
            end--;
        }
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - First reverse:    O(n)
  - Reverse each word: O(n) - each character reversed once
  - Build result:     O(n)
  Total: O(n)

Space Complexity: O(n)
  - Character array:  O(n)
  - Result StringBuilder: O(n)
  Total: O(n)
  
  NOTE: If we could modify input and return as array, this could be O(1)
        space for the reversal operations themselves, but we still need
        space for the output string.

Pros:
  + Elegant algorithm: demonstrates deep understanding
  + In-place reversal aspect (good for interviews)
  + Single pass through data after transformations

Cons:
  - More complex to implement and explain
  - Easy to make off-by-one errors
  - Still requires O(n) space for final string output
  - More error-prone than split/join

When to Use:
  + Interview: to showcase algorithmic thinking
  + When space optimization is a discussion point
  - NOT practical in real code (split/join is clearer)
  - NOT faster than simpler approaches
*/


/*
Approach 4: Deque-Based Reversal
────────────────────────────────

Core Idea:
  1. Use a Deque (double-ended queue)
  2. Add words to the FRONT of the deque as we scan the string
  3. Words added to front naturally creates reverse order
  4. Join words from deque

Data Structures & Algorithm:
  - Deque (ArrayDeque is efficient)
  - LinkedList data structure with O(1) operations at both ends
  - Linear scan

Implementation:
*/

class Solution4_Deque {
    public String reverseWords(String sentence) {
        Deque<String> deque = new ArrayDeque<>();
        StringBuilder currentWord = new StringBuilder();
        
        // Scan string and identify words
        for (int i = 0; i < sentence.length(); i++) {
            char ch = sentence.charAt(i);
            
            if (ch != ' ') {
                currentWord.append(ch);
            } else {
                // Space encountered
                if (currentWord.length() > 0) {
                    // Add word to FRONT of deque (reverse order)
                    deque.addFirst(currentWord.toString());
                    currentWord = new StringBuilder();
                }
            }
        }
        
        // Don't forget the last word
        if (currentWord.length() > 0) {
            deque.addFirst(currentWord.toString());
        }
        
        // Build result from deque (already in correct order)
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String word : deque) {
            if (!first) {
                result.append(" ");
            }
            result.append(word);
            first = false;
        }
        
        return result.toString();
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - Single pass through string: O(n)
  - Deque operations (addFirst): O(1) per operation
  - Building result: O(n)
  Total: O(n)

Space Complexity: O(n)
  - Deque stores w words: O(n)
  - StringBuilder: O(n)
  Total: O(n)

Pros:
  + Clean and readable
  + Deque provides efficient bidirectional access
  + No explicit reversal step needed

Cons:
  - Not significantly faster than Stack approach
  - Still O(n) space
  - Slightly more overhead than simple split/join

When to Use:
  + When you want to demonstrate Deque knowledge
  + Educational purposes
  - NOT faster than split/join in practice
*/


/*
Approach 5: Stream API with Collections.reverse()
──────────────────────────────────────────────────

Core Idea:
  1. Use Java 8+ Stream API for functional approach
  2. Filter empty strings from split result
  3. Collect into a list
  4. Reverse the list
  5. Join back

Data Structures & Algorithm:
  - Stream processing
  - List and Collections.reverse()
  - Functional programming paradigm

Implementation:
*/

class Solution5_StreamAPI {
    public String reverseWords(String sentence) {
        // Split, filter empty strings, collect to list
        List<String> words = Arrays.stream(sentence.trim().split("\\s+"))
            .filter(word -> !word.isEmpty())
            .collect(Collectors.toList());
        
        // Reverse the list in-place
        Collections.reverse(words);
        
        // Join with space
        return String.join(" ", words);
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - trim() + split(): O(n)
  - Stream operations: O(w) where w = number of words
  - reverse(): O(w)
  - join(): O(n)
  Total: O(n)

Space Complexity: O(n)
  - List of words: O(w) = O(n)
  - Strings in list: O(n)
  Total: O(n)

Pros:
  + Elegant, modern Java style
  + Functional programming approach
  + Concise and expressive

Cons:
  - Not faster than Approach 1
  - Stream overhead for simple operations
  - Less familiar to all interviewers
  - filter() adds unnecessary step (trim() already handles spaces)

When to Use:
  + If you want to showcase modern Java knowledge
  + Code review or production code
  - NOT ideal for technical interviews
  - NOT for performance-critical code
*/


/*
Approach 6: Manual Iteration with StringBuilder (Most Practical)
───────────────────────────────────────────────────────────────

Core Idea:
  1. Iterate through string from end to beginning
  2. Skip spaces at the end
  3. For each word (going backwards), add it to result
  4. Handle spaces between words carefully

Data Structures & Algorithm:
  - Single backward pass through string
  - StringBuilder for efficient result building
  - Two-pointer technique (implicit)

Implementation:
*/

class Solution6_BackwardIteration {
    public String reverseWords(String sentence) {
        StringBuilder result = new StringBuilder();
        int i = sentence.length() - 1;
        
        // Traverse string from right to left
        while (i >= 0) {
            // Skip spaces at the current position
            while (i >= 0 && sentence.charAt(i) == ' ') {
                i--;
            }
            
            // Now i points to the last character of a word (or -1 if done)
            if (i < 0) {
                break;
            }
            
            // Find the start of the current word
            int wordEnd = i;
            while (i >= 0 && sentence.charAt(i) != ' ') {
                i--;
            }
            int wordStart = i + 1;
            
            // Append the word to result
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(sentence.substring(wordStart, wordEnd + 1));
        }
        
        return result.toString();
    }
}

/*
Time Complexity:  O(n) where n = length of input string
  - Single pass through string (right to left): O(n)
  - substring() operations: O(w) where w = length of words
  - StringBuilder append: O(1) amortized
  Total: O(n)

Space Complexity: O(n)
  - StringBuilder for result: O(n)
  Total: O(n)
  (Note: substring() may create temporary string objects)

Pros:
  + Single pass through string
  + No regex or built-in splits needed
  + Clean logic for backward iteration

Cons:
  - substring() creates new String objects
  - Slightly more complex to understand
  - Need to be careful with index boundaries

When to Use:
  + When you want to show custom iteration skills
  + Good for interviews to demonstrate mastery
  - Not faster than split/join in practice
*/

/*

================================================================================
 SECTION 5: APPROACHES COMPARISON TABLE
================================================================================

/*
┌──────────────────────────┬────────┬────────┬──────────────────┬─────────────────┐
│ Approach                 │ Time   │ Space  │ Best For         │ Key Limitation  │
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 1. Split/Reverse/Join    │ O(n)   │ O(n)   │ Clarity,Readability
                           │        │        │ Interview clarity│ Multiple objects│
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 2. Stack LIFO            │ O(n)   │ O(n)   │ Teaching concept │ Not optimal    │
│                          │        │        │ Data structures  │ More verbose   │
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 3. Two-Pointer Reversal  │ O(n)   │ O(n)   │ Algorithm demos  │ Complex logic  │
│                          │        │        │ Interview depth  │ Error-prone    │
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 4. Deque-Based           │ O(n)   │ O(n)   │ Data structures  │ Not faster     │
│                          │        │        │ Clean code       │ More verbose   │
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 5. Stream API            │ O(n)   │ O(n)   │ Modern Java demo │ Overhead       │
│                          │        │        │ Production code  │ Less familiar  │
├──────────────────────────┼────────┼────────┼──────────────────┼─────────────────┤
│ 6. Backward Iteration    │ O(n)   │ O(n)   │ Custom logic     │ substring()    │
│                          │        │        │ Interview skills │ overhead       │
└──────────────────────────┴────────┴────────┴──────────────────┴─────────────────┘
*/

/*

================================================================================
 SECTION 6: RECOMMENDED APPROACH FOR INTERVIEW
================================================================================

BEST CHOICE: Approach 1 (Split, Reverse, Join)

RATIONALE:

1. CLARITY & CONFIDENCE
   - Simplest to code correctly under interview pressure
   - Leverages built-in Java methods that are well-tested
   - Less chance of off-by-one errors or logic bugs
   - Easier to explain to interviewer

2. TIME EFFICIENCY
   - No slower than other approaches (all are O(n))
   - Writing time in interview is critical

3. INTERVIEWER EXPECTATIONS
   - Shows understanding of problem requirements
   - Demonstrates knowledge of Java API
   - Not overly complex (doesn't raise suspicion of over-engineering)

4. EXTENSIBILITY
   - Easy to discuss optimizations if asked
   - Can pivot to Approach 3 (two-pointer) if asked about space optimization
   - Can discuss trade-offs with clarity

INTERVIEW EXECUTION PLAN:

Step 1: Clearly restate the problem (30 seconds)
Step 2: Ask clarifying questions (1 minute)
Step 3: Describe approach verbally (1 minute)
Step 4: Code the solution (3-5 minutes)
Step 5: Test with examples (2 minutes)
Step 6: Discuss time/space complexity (1 minute)
Step 7: If time permits, discuss other approaches or optimizations

WHAT TO SAY:
"I'm going to use a straightforward approach: split the string by spaces
to get individual words, filter out empty strings, reverse the order of
words, and join them back with single spaces. This is O(n) time and O(n)
space, which is optimal for this problem since we need to return a new
string anyway."

*/


/*
================================================================================
 SECTION 7: DEEP DIVE - OPTIMAL SOLUTION (PRODUCTION READY)
================================================================================

This is the polished, interview-ready version with comprehensive comments.
*/

class OptimalSolution {
    
    /**
     * Reverses the order of words in a string while preserving word content.
     *
     * Algorithm:
     *   1. Trim leading/trailing whitespace to eliminate edge cases
     *   2. Split by one or more whitespace chars using regex "\\s+"
     *      This automatically handles multiple consecutive spaces
     *   3. Reverse the resulting array of words using two-pointer technique
     *   4. Join back with single space separator
     *
     * Time Complexity:  O(n) where n = length of input string
     *   - trim():       O(n) - scans to find first/last non-space
     *   - split():      O(n) - single pass to identify delimiters
     *   - reverse:      O(w) where w = number of words (w <= n)
     *   - join():       O(n) - concatenates all words and spaces
     *
     * Space Complexity: O(n)
     *   - words array holds references to w String objects
     *   - Each String is a view of original chars
     *   - Result string is O(n)
     *
     * @param sentence The input string that may contain leading/trailing/extra spaces
     * @return A new string with words in reversed order, single spaces between words
     *
     * @throws NullPointerException if sentence is null (can add null check if needed)
     *
     * @example
     *   reverseWords("  hello   world  ") -> "world hello"
     *   reverseWords("a") -> "a"
     *   reverseWords("   ") -> ""
     */
    public String reverseWords(String sentence) {
        // Edge case: null input (not specified in problem, but good practice)
        if (sentence == null) {
            return "";
        }
        
        // Step 1: Remove leading and trailing spaces
        // This is critical because split("\\s+") on "  hello  " would not
        // produce empty strings at the ends due to how split() works
        String trimmed = sentence.trim();
        
        // Step 2: Check for empty string after trimming
        // If we have only spaces, trimmed will be empty
        if (trimmed.isEmpty()) {
            return "";
        }
        
        // Step 3: Split by one or more whitespace characters
        // The regex "\\s+" matches any sequence of whitespace (space, tab, etc.)
        // Unlike split(" ") which only splits on single space,
        // this handles multiple spaces gracefully
        String[] words = trimmed.split("\\s+");
        
        // Step 4: Reverse the words array in-place using two pointers
        // This is O(w) where w = number of words (w << n)
        // We use two-pointer approach for efficiency
        int left = 0;
        int right = words.length - 1;
        
        while (left < right) {
            // Swap words at left and right pointers
            String temp = words[left];
            words[left] = words[right];
            words[right] = temp;
            
            // Move pointers toward center
            left++;
            right--;
        }
        
        // Step 5: Join words back with single space separator
        // String.join() is efficient and handles the delimiter properly
        // It uses StringBuilder internally, so performance is O(n)
        return String.join(" ", words);
    }
}

/*

================================================================================
 SECTION 8: DRY RUN / TRACE THROUGH EXAMPLE
================================================================================

Tracing the optimal solution with input: "  hello   world  "

INITIAL STATE:
  sentence = "  hello   world  " (length = 16)

STEP 1: trim()
  trimmed = "hello   world" (leading/trailing spaces removed)
  State:
    Left:  "hello   world"
    Right: (none yet)

STEP 2: isEmpty() check
  trimmed.isEmpty() = false, so continue

STEP 3: split("\\s+")
  words = ["hello", "world"]
  Explanation:
    - Regex "\\s+" matches 1+ consecutive spaces
    - "hello" followed by 3 spaces -> creates one split point
    - Result is array with 2 elements
  State:
    words[0] = "hello"
    words[1] = "world"

STEP 4: Reverse the array in-place
  
  Iteration 1:
    left = 0, right = 1
    Condition: 0 < 1? YES
    temp = words[0] = "hello"
    words[0] = words[1] = "world"
    words[1] = temp = "hello"
    left = 1, right = 0
    State after iteration 1:
      words[0] = "world"
      words[1] = "hello"
  
  Iteration 2:
    left = 1, right = 0
    Condition: 1 < 0? NO
    Exit loop
  
  Final array state:
    words = ["world", "hello"]

STEP 5: String.join(" ", words)
  Join array ["world", "hello"] with " " separator
  result = "world" + " " + "hello" = "world hello"

FINAL OUTPUT:
  return "world hello"

VERIFICATION:
  Input:   "  hello   world  "
  Output:  "world hello"
  Expected: "world hello" ✓
  
  Checks:
    - Leading spaces removed ✓
    - Trailing spaces removed ✓
    - Multiple spaces between words condensed ✓
    - Words reversed ✓
    - Single space between words ✓

*/


/*
================================================================================
 SECTION 9: CLOSING SUMMARY & TRADE-OFFS
================================================================================

WHAT WE'VE COVERED:

1. Problem Understanding
   ✓ String reversal (word order, not character order)
   ✓ Space handling (leading, trailing, multiple)
   ✓ Word definition (continuous non-space sequences)

2. Six Different Approaches
   ✓ Each with complete working implementation
   ✓ Time/Space complexity analysis
   ✓ Pros and cons discussed
   ✓ Use cases identified

3. Clear Recommendation
   ✓ Split/Reverse/Join for interviews
   ✓ Reasoning: clarity, speed, confidence

KEY TRADE-OFFS:

Clarity vs. Algorithmic Complexity:
  → For interviews, choose clarity (Approach 1)
  → In system design, might optimize further

Time vs. Space:
  → All approaches are O(n) time and O(n) space
  → Cannot do better than O(n) for output string
  → Space optimization is minimal gain

Built-ins vs. Custom Logic:
  → Built-ins (split, join) are easier and safer
  → Custom logic (two-pointer) shows deep knowledge
  → In interviews: prefer built-ins for first solution

Single Pass vs. Multiple Passes:
  → Multiple passes (split then reverse) is simpler
  → Single pass (backward iteration) is less intuitive
  → Choose based on comfort level

LIMITATIONS OF ALL APPROACHES:

1. Space Complexity Bound at O(n)
   - Must return a new String object
   - String is immutable in Java
   - Cannot reduce output space below O(n)

2. String Object Creation
   - split() creates multiple String objects
   - Join also creates intermediates
   - Garbage collection overhead in long-running apps

3. Unicode Handling
   - All approaches handle Unicode correctly
   - Character.isSpace() vs space char comparison
   - Regex "\\s+" handles all whitespace types

4. Performance Characteristics
   - No approach is significantly faster
   - Regex overhead might matter for tiny strings
   - Negligible for typical interview constraints

*/


/*
================================================================================
 SECTION 10: FOLLOW-UP QUESTIONS
================================================================================

Q1: Can you solve this problem while modifying only the input and not
    creating any new String objects?

    Model Answer:
    "If we could use a char array directly (which we can convert to in Java),
    we could do the two-pointer reversal approach (Approach 3) without creating
    intermediate strings during reversal. However, we must return a String, so
    we ultimately need O(n) space. The best we can do is minimize intermediate
    objects during processing. If this were C++, we could potentially return
    the modified char array."

Q2: What if the input string is extremely large (e.g., 10 GB in a stream)?
    How would you handle it?

    Model Answer:
    "For streaming scenarios, we couldn't load everything into memory.
    We'd need to:
    1. Read chunks of the stream
    2. Process words within chunks
    3. Handle word boundaries between chunks (word might span chunk boundary)
    4. Output words in reverse order
    This would require a circular buffer or queue to track words as we read,
    and careful management of partial words at chunk boundaries."

Q3: What if we need to reverse only specific words (e.g., words at odd
    positions)?

    Model Answer:
    "We'd modify the reversal logic to check word position:
    - During split: keep track of word index
    - Only reverse if index satisfies condition
    - This would still be O(n) time but with different control flow
    - Implementation would use conditional reversals in array or during join"

Q4: How would this solution perform with concurrency? Can we parallelize it?

    Model Answer:
    "For parallelization:
    - Split phase could be parallelized if we're careful about word boundaries
    - Reverse phase is inherently sequential (can't parallelize array reversal)
    - Join phase could be parallelized by dividing into chunks
    - Overhead would likely exceed benefits for typical string sizes
    - For very large strings (GB+), map-reduce style approach might help"

Q5: What's the best approach if we can only use O(1) extra space (excluding
    the output)?

    Model Answer:
    "If we could modify the input string:
    1. Two-pointer reversal of entire char array: O(n)
    2. Reverse each word individually: O(n)
    3. Clean up spaces: O(n)
    Total: O(n) time, O(1) extra space (just pointers)
    
    However, Java Strings are immutable, so we'd need char[] input.
    If input must remain unchanged, it's impossible due to String immutability."

Q6: What if words can be up to 10^6 characters long? Does that change anything?

    Model Answer:
    "The solution remains O(n) where n is total string length.
    However:
    - Each word pointer is still O(1)
    - substring() or similar operations on giant words would be O(word_length)
    - Stack or memory fragmentation could become issues
    - We'd need to benchmark with realistic data
    - Approach might change if we're processing from a file or network stream
    - Would need to ensure StringBuilder doesn't hit memory limits"

*/


/*
================================================================================
 SECTION 11: WHAT CANDIDATES TYPICALLY MISS
================================================================================

COMMON MISTAKE 1: Not Handling Multiple Spaces
──────────────────────────────────────────────
Problem:
  String[] words = sentence.split(" ");  // WRONG: splits on single space only
  
Issue:
  Input: "  hello   world  " 
  Result: ["", "", "hello", "", "", "world", "", ""]
  This creates empty strings in the array!
  Joining produces: "  hello   world" (extra spaces remain)

Solution:
  Use regex split("\\s+") which matches one or more whitespace characters
  OR use trim() before split() to remove leading/trailing spaces
  
LESSON: Always think about whitespace edge cases


COMMON MISTAKE 2: Off-by-One Error in Manual Reversal
──────────────────────────────────────────────────────
Problem:
  while (left <= right) { ... }  // WRONG: includes middle when odd length
  
Issue:
  For array of length 3: [a, b, c]
    When left = 1, right = 1, we swap b with itself (waste)
    Condition should be left < right to stop before middle
  
Also wrong:
  reverse(words, 0, words.length);  // WRONG: length is out of bounds
  Should be: reverse(words, 0, words.length - 1);

Solution:
  Always use left < right for two-pointer reversal
  Always use 0 to length-1 for array boundaries

LESSON: Carefully test loop termination conditions


COMMON MISTAKE 3: Forgetting to Trim Before Processing
───────────────────────────────────────────────────────
Problem:
  String[] words = sentence.split("\\s+");  // Split without trim
  
Issue:
  Input: "  hello  "
  Result: ["", "hello", ""] if split() isn't careful about leading/trailing
  Even with "\\s+", leading/trailing empty strings might appear
  When joined: " hello " might have extra spaces

Solution:
  ALWAYS trim() before processing:
  String trimmed = sentence.trim();
  String[] words = trimmed.split("\\s+");

LESSON: Handle leading/trailing spaces explicitly


COMMON MISTAKE 4: Not Verifying Edge Cases
───────────────────────────────────────────
Problem:
  Testing only with "hello world" and assuming all cases work
  
Issue:
  Edge cases fail:
    - Empty string: ""
    - Only spaces: "   " 
    - Single word: "hello"
    - Single char: "a"
    - Very long spaces: "a          b"
  
Solution:
  In interview, explicitly state you'll test:
    1. Normal case
    2. Single word
    3. Empty string / only spaces
    4. Extra spaces
    5. Single character

LESSON: Always test edge cases before submitting


BONUS MISTAKE 5: Inefficient String Concatenation in Loop
──────────────────────────────────────────────────────────
Problem:
  String result = "";
  for (String word : words) {
      result = result + " " + word;  // O(n^2) due to String immutability!
  }

Issue:
  Each concatenation creates a new String object
  With w words, this is O(w^2) operation
  For 1000 words, you're doing millions of copies

Solution:
  Use StringBuilder:
  StringBuilder result = new StringBuilder();
  for (String word : words) {
      result.append(word).append(" ");
  }

LESSON: Always use StringBuilder for string building in loops

*/


/*
================================================================================
 SECTION 12: TEST CASES & MAIN METHOD
================================================================================
*/

public class ReverseWordsInStringInterview {
    
    // We'll test with the optimal solution
    private static final OptimalSolution solution = new OptimalSolution();
    
    public static void main(String[] args) {
        System.out.println("=== Reverse Words in String - Test Suite ===\n");
        
        // Test Case 1: Normal case with multiple spaces
        testCase(1,
            "  hello   world  ",
            "world hello",
            "Normal case: multiple leading, trailing, and between spaces");
        
        // Test Case 2: Single word
        testCase(2,
            "hello",
            "hello",
            "Single word with no spaces");
        
        // Test Case 3: Empty string or only spaces
        testCase(3,
            "   ",
            "",
            "Only spaces - should return empty string");
        
        // Test Case 4: Two words with single space
        testCase(4,
            "a b",
            "b a",
            "Minimal case: two single-char words");
        
        // Test Case 5: Words with punctuation
        testCase(5,
            "the123  quick-brown  fox!",
            "fox! quick-brown the123",
            "Words with digits and punctuation");
        
        // Test Case 6: Empty string
        testCase(6,
            "",
            "",
            "Empty input string");
        
        // Test Case 7: Tabs and mixed whitespace
        testCase(7,
            "hello\t\t\tworld",
            "world hello",
            "Mixed whitespace (tabs handled by \\s+)");
        
        // Test Case 8: Single character
        testCase(8,
            "a",
            "a",
            "Single character");
        
        // Test Case 9: Many words
        testCase(9,
            " a b c d e ",
            "e d c b a",
            "Multiple single-char words");
        
        // Test Case 10: Real sentence
        testCase(10,
            "Hello,   World!  This  is  a  test.",
            "test. a is This World! Hello,",
            "Real sentence with punctuation");
        
        System.out.println("\n=== All tests completed ===");
    }
    
    /**
     * Helper method to run and verify test cases
     */
    private static void testCase(int number, String input, String expected, String description) {
        String result = solution.reverseWords(input);
        boolean passed = result.equals(expected);
        
        String status = passed ? "✓ PASS" : "✗ FAIL";
        System.out.printf("%s Test %d: %s%n", status, number, description);
        
        if (!passed) {
            System.out.printf("  Input:    \"%s\"%n", input);
            System.out.printf("  Expected: \"%s\"%n", expected);
            System.out.printf("  Got:      \"%s\"%n", result);
        }
        System.out.println();
    }
}

/*
================================================================================
 END OF INTERVIEW WALKTHROUGH
================================================================================

This file demonstrates the complete approach to solving the "Reverse Words
in String" problem as it would be presented in a Google technical interview.

Key takeaways:
  1. Always restate and clarify the problem
  2. Ask questions about constraints and edge cases
  3. Discuss multiple approaches before coding
  4. Implement the clearest, most reliable solution first
  5. Test with comprehensive examples
  6. Be prepared to discuss optimizations and trade-offs

Good luck with your interview!

================================================================================
*/
