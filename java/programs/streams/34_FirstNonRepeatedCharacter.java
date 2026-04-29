package streams;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class FirstNonRepeatedCharacter {

    public static void main(String[] args) {
        String s = "aabbcdeeff";

        Optional<Character> first = s.chars()
                .mapToObj(c -> (char) c)
                .collect(
                        Collectors.groupingBy(
                                c -> c,
                                LinkedHashMap::new,
                                Collectors.counting()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst();
        first.ifPresent(System.out::println);

    }

}
