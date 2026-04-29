package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class FlatMapWithOptional {

    public static void main(String[] args) {
        List<Optional<String>> optionals = Arrays.asList(
                Optional.of("Alice"),
                Optional.empty(),
                Optional.of("Bob"),
                Optional.empty(),
                Optional.of("Charlie"));

        // Java 9+:
        List<String> values = optionals.stream()
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        // Java 8 way:
        List<String> values8 = optionals.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // Output: [Alice, Bob, Charlie]
    }

}
