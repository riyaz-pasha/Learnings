
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class FindMax {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(10, 40, 20, 50, 30);

        int max = Collections.max(numbers);

        Optional<Integer> max2 = numbers.stream()
                .max(Integer::compareTo);

        int max3 = numbers.stream()
                .mapToInt(Integer::intValue)
                .max()
                .getAsInt();

        int[] arr = { 10, 40, 20, 50, 30 };
        int max4 = Arrays.stream(arr)
                .max()
                .getAsInt();
    }

}
