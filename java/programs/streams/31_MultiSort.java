package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class MultiSort {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("banana", "fig", "apple", "kiwi", "cat", "bee");

        List<String> sorted = words.stream()
                .sorted(
                        Comparator.comparingInt(String::length)
                                .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());
        System.out.println(sorted);
        // Output: [bee, cat, fig, kiwi, apple, banana]

    }

}
