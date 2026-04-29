package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class CountOccurancesOfEachElement {

    public static void main(String[] args) {

        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry", "banana", "apple");

        Map<String, Long> countOccurances1 = words.stream()
                .collect(Collectors.groupingBy(
                        word -> word,
                        Collectors.counting()));
        System.out.println(countOccurances1);
        // Output: {apple=3, banana=2, cherry=1}

        Map<String, Long> counts = words.stream()
                .collect(Collectors.toMap(
                        Function.identity(), // Key: the element itself
                        e -> 1L, // Value: start with 1
                        Long::sum // Merge: if key exists, sum the values
                ));
        System.out.println(counts);
    }

}
