package streams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class FindDuplicateInList {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Alice", "Charlie", "Bob", "Dave");

        Set<String> duplicates = names.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        System.out.println(duplicates);

        Set<String> seen = new HashSet<>();
        Set<String> duplicates2 = names.stream()
                .filter(n -> !seen.add(n)) // add returns false if already present
                .collect(Collectors.toSet());
        System.out.println(duplicates2);
        // Output: [Alice, Bob]

    }

}
