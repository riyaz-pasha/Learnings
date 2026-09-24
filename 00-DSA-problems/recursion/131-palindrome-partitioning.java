import java.util.ArrayList;
import java.util.List;

class PalindromePartitioning {

    public List<List<String>> partition(String s) {
        List<List<String>> result = new ArrayList<>();
        List<String> current = new ArrayList<>();
        backtrack(s, 0, current, result);
        return result;
    }

    private void backtrack(String s, int start, List<String> current, List<List<String>> result) {
        // Base case: reached the end of string
        if (start == s.length()) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Try all possible partitions starting from 'start'
        for (int end = start + 1; end <= s.length(); end++) {
            String substring = s.substring(start, end);

            // Only continue if current substring is a palindrome
            if (isPalindrome(substring)) {
                current.add(substring);
                backtrack(s, end, current, result);
                current.remove(current.size() - 1); // backtrack
            }
        }
    }

    private boolean isPalindrome(String s) {
        int left = 0, right = s.length() - 1;
        while (left < right) {
            if (s.charAt(left) != s.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    // Test method
    public static void main(String[] args) {
        PalindromePartitioning solution = new PalindromePartitioning();

        // Test Example 1
        String s1 = "aab";
        List<List<String>> result1 = solution.partition(s1);
        System.out.println("Input: s = \"" + s1 + "\"");
        System.out.println("Output: " + result1);
        System.out.println();

        // Test Example 2
        String s2 = "a";
        List<List<String>> result2 = solution.partition(s2);
        System.out.println("Input: s = \"" + s2 + "\"");
        System.out.println("Output: " + result2);
        System.out.println();

        // Additional test
        String s3 = "aabb";
        List<List<String>> result3 = solution.partition(s3);
        System.out.println("Input: s = \"" + s3 + "\"");
        System.out.println("Output: " + result3);
    }
}
