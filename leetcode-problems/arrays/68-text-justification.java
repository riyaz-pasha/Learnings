import java.util.*;
/*
* Given an array of strings words and a width maxWidth, format the text such
* that each line has exactly maxWidth characters and is fully (left and right)
* justified.
* 
* You should pack your words in a greedy approach; that is, pack as many words
* as you can in each line. Pad extra spaces ' ' when necessary so that each
* line has exactly maxWidth characters.
* 
* Extra spaces between words should be distributed as evenly as possible. If
* the number of spaces on a line does not divide evenly between words, the
* empty slots on the left will be assigned more spaces than the slots on the
* right.
* 
* For the last line of text, it should be left-justified, and no extra space is
* inserted between words.
* 
* Note:
* A word is defined as a character sequence consisting of non-space characters
* only.
* Each word's length is guaranteed to be greater than 0 and not exceed
* maxWidth.
* The input array words contains at least one word.
* 
* 
* Example 1:
* Input: words = ["This", "is", "an", "example", "of", "text",
* "justification."], maxWidth = 16
* Output:
* [
* "This    is    an",
* "example  of text",
* "justification.  "
* ]
* 
* Example 2:
* Input: words = ["What","must","be","acknowledgment","shall","be"], maxWidth =
* 16
* Output:
* [
* "What   must   be",
* "acknowledgment  ",
* "shall be        "
* ]
* Explanation: Note that the last line is "shall be    " instead of
* "shall     be", because the last line must be left-justified instead of
* fully-justified.
* Note that the second line is also left-justified because it contains only one
* word.
* 
* Example 3:
* Input: words =
* ["Science","is","what","we","understand","well","enough","to","explain","to",
* "a","computer.","Art","is","everything","else","we","do"], maxWidth = 20
* Output:
* [
* "Science  is  what we",
* "understand      well",
* "enough to explain to",
* "a  computer.  Art is",
* "everything  else  we",
* "do                  "
* ]
*/

class TextJustification {

    /**
     * Solution 1: Clean and readable approach
     * Time Complexity: O(n * maxWidth) where n is the number of words
     * Space Complexity: O(maxWidth) for each line construction
     */
    public List<String> fullJustify(String[] words, int maxWidth) {
        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < words.length) {
            // Find words that can fit in current line
            List<String> currentLine = new ArrayList<>();
            int totalLength = 0;

            // Pack words greedily
            while (i < words.length) {
                // Check if we can add current word (word length + at least 1 space for each
                // existing word)
                int spaceNeeded = currentLine.isEmpty() ? 0 : 1;
                if (totalLength + spaceNeeded + words[i].length() <= maxWidth) {
                    currentLine.add(words[i]);
                    totalLength += words[i].length();
                    i++;
                } else {
                    break;
                }
            }

            // Format the current line
            if (i == words.length) {
                // Last line - left justify
                result.add(leftJustify(currentLine, maxWidth));
            } else {
                // Regular line - full justify
                result.add(fullJustifyLine(currentLine, maxWidth));
            }
        }

        return result;
    }

    /**
     * Full justify a line (distribute spaces evenly)
     */
    private String fullJustifyLine(List<String> words, int maxWidth) {
        if (words.size() == 1) {
            // Single word - left justify
            return leftJustify(words, maxWidth);
        }

        // Calculate total character length of words
        int totalChars = 0;
        for (String word : words) {
            totalChars += word.length();
        }

        // Calculate spaces to distribute
        int totalSpaces = maxWidth - totalChars;
        int gaps = words.size() - 1;
        int spacesPerGap = totalSpaces / gaps;
        int extraSpaces = totalSpaces % gaps;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            sb.append(words.get(i));

            if (i < words.size() - 1) { // Not the last word
                // Add regular spaces
                for (int j = 0; j < spacesPerGap; j++) {
                    sb.append(' ');
                }
                // Add extra space if needed (left gaps get more spaces)
                if (i < extraSpaces) {
                    sb.append(' ');
                }
            }
        }

        return sb.toString();
    }

    /**
     * Left justify a line (for last line or single word lines)
     */
    private String leftJustify(List<String> words, int maxWidth) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.size(); i++) {
            sb.append(words.get(i));
            if (i < words.size() - 1) {
                sb.append(' ');
            }
        }

        // Pad with spaces to reach maxWidth
        while (sb.length() < maxWidth) {
            sb.append(' ');
        }

        return sb.toString();
    }

    /**
     * Solution 2: More compact approach
     */
    public List<String> fullJustifyCompact(String[] words, int maxWidth) {
        List<String> result = new ArrayList<>();
        int i = 0;

        while (i < words.length) {
            int j = i, len = 0;

            // Find how many words can fit in current line
            while (j < words.length && len + words[j].length() + j - i <= maxWidth) {
                len += words[j++].length();
            }

            StringBuilder sb = new StringBuilder();
            int gaps = j - i - 1;

            // Last line or single word - left justify
            if (j == words.length || gaps == 0) {
                for (int k = i; k < j; k++) {
                    sb.append(words[k]);
                    if (k < j - 1)
                        sb.append(' ');
                }
                while (sb.length() < maxWidth)
                    sb.append(' ');
            } else {
                // Regular line - full justify
                int spaces = (maxWidth - len) / gaps;
                int extraSpaces = (maxWidth - len) % gaps;

                for (int k = i; k < j; k++) {
                    sb.append(words[k]);
                    if (k < j - 1) {
                        for (int s = 0; s <= spaces + (k - i < extraSpaces ? 1 : 0); s++) {
                            sb.append(' ');
                        }
                    }
                }
            }

            result.add(sb.toString());
            i = j;
        }

        return result;
    }

    /**
     * Test method with provided examples
     */
    public static void main(String[] args) {
        TextJustification tj = new TextJustification();

        // Example 1
        String[] words1 = { "This", "is", "an", "example", "of", "text", "justification." };
        List<String> result1 = tj.fullJustify(words1, 16);
        System.out.println("Example 1:");
        for (String line : result1) {
            System.out.println("\"" + line + "\"");
        }
        System.out.println();

        // Example 2
        String[] words2 = { "What", "must", "be", "acknowledgment", "shall", "be" };
        List<String> result2 = tj.fullJustify(words2, 16);
        System.out.println("Example 2:");
        for (String line : result2) {
            System.out.println("\"" + line + "\"");
        }
        System.out.println();

        // Example 3
        String[] words3 = { "Science", "is", "what", "we", "understand", "well", "enough", "to", "explain", "to", "a",
                "computer.", "Art", "is", "everything", "else", "we", "do" };
        List<String> result3 = tj.fullJustify(words3, 20);
        System.out.println("Example 3:");
        for (String line : result3) {
            System.out.println("\"" + line + "\"");
        }
    }

}

/**
 * Key Algorithm Steps:
 * 
 * 1. Greedy Packing: For each line, pack as many words as possible
 * - Keep adding words while total length + spaces between them <= maxWidth
 * 
 * 2. Space Distribution:
 * - Calculate total spaces needed = maxWidth - sum of word lengths
 * - Distribute evenly among gaps between words
 * - Extra spaces go to leftmost gaps first
 * 
 * 3. Edge Cases:
 * - Last line: Left-justified only
 * - Single word in line: Left-justified with trailing spaces
 * - Multiple words: Full justification with even space distribution
 * 
 * Time Complexity: O(n * maxWidth) where n is number of words
 * Space Complexity: O(maxWidth) for line construction
 */
