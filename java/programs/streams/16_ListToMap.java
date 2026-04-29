package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ListToMap {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

        Map<String, Integer> nameToLenMap1 = names.stream()
                .collect(Collectors.toMap(name -> name, name -> name.length()));
        System.out.println(nameToLenMap1);

        Map<String, Integer> nameToLenMap2 = names.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        String::length));
        System.out.println(nameToLenMap2);

        // Handle duplicate keys:
        Map<String, Integer> safe = names.stream()
                .collect(Collectors.toMap(
                        s -> s,
                        String::length,
                        (v1, v2) -> v1 // keep first
                ));
        System.out.println(safe);
    }

}
