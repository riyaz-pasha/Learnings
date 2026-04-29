package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

class FindLongest {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("cat", "elephant", "dog", "hippopotamus", "bee");

        String longest = words.stream()
                .max(Comparator.comparing(String::length))
                .orElse("");
        System.out.println(longest);
        // Output: "hippopotamus"

        // Alternatively using reduce:
        String longest2 = words.stream()
                .reduce("", (a, b) -> a.length() >= b.length() ? a : b);
        System.out.println(longest2);
    }

}
