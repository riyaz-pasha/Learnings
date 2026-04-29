package streams;

import java.util.Arrays;
import java.util.stream.Collectors;

class ReverseWords {

    public static void main(String[] args) {
        String sentence = "Hello World from Java Streams";

        String reversed = Arrays.stream(sentence.split(" "))
                .map(w -> new StringBuilder(w).reverse().toString())
                .collect(Collectors.joining(" "));
        System.out.println(reversed);
        // "olleH dlroW morf avaJ smaertS"

        // Reverse word order (not chars):
        String wordOrderReversed = Arrays.stream(sentence.split(" "))
                .reduce((a, b) -> b + " " + a)
                .orElse("");
        System.out.println(wordOrderReversed);
        // "Streams Java from World Hello"
    }

}
