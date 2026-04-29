package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ExtractWordsFromSentances {

    public static void main(String[] args) {
        List<String> sentences = Arrays.asList(
                "Java is awesome",
                "Streams are powerful",
                "EPAM loves Java");

        List<String> words = sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .collect(Collectors.toList());
        System.out.println(words);
        // Output: [Java, is, awesome, Streams, are, powerful, EPAM, loves, Java]

        // Unique words:
        Set<String> uniqueWords = sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .collect(Collectors.toSet());
    }

}
