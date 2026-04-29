package streams;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

class RemoveDuplicatedWithInsertionOrder {

    public static void main(String[] args) {

        List<String> items = Arrays.asList("c", "a", "b", "a", "c", "d", "b");

        List<String> unique = items.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println(unique);
        // [c, a, b, d] — distinct() preserves encounter order

        // To collect into LinkedHashSet explicitly:
        LinkedHashSet<String> linked = items.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        System.out.println(linked);
    }

}
