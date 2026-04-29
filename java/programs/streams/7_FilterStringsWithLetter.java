package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class FilterStringsWithLetter {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Anna", "Charlie", "Andrew");

        List<String> result1 = names.stream()
                .filter(name -> name.startsWith("A"))
                .collect(Collectors.toList());
        System.out.println(result1);
    }

}
