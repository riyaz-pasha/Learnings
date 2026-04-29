package streams;

import java.util.stream.IntStream;

class StringPalindromeCheck {

    public static void main(String[] args) {
        String s = "racecar";

        boolean isPalindrome = IntStream.range(0, s.length() / 2)
                .allMatch(index -> s.charAt(index) == s.charAt(s.length() - 1 - index));
        System.out.println(isPalindrome);

        String reversed = new StringBuilder(s).reverse().toString();
        boolean isPalin2 = s.equals(reversed); // true
        System.out.println(isPalin2);
    }

}
