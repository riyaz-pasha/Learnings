package streams;

import java.util.Arrays;
import java.util.List;

class FirstEvenNumbersSquare {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 3, 5, 4, 7);

        Integer evenNumSquare = nums.stream()
                .filter(num -> num % 2 == 0)
                .findFirst()
                .map(evenNum -> evenNum * evenNum)
                .orElse(-1);
        System.out.println(evenNumSquare);

        // Optional methods to know:
        // .isPresent() - check if value exists
        // .get() - get value (throws if empty!)
        // .orElse(val) - default if empty
        // .orElseGet(supplier) - lazy default
        // .orElseThrow() - throw if empty
        // .map(fn) - transform if present
        // .filter(pred) - filter if present
        // .ifPresent(consumer) - execute if present
    }

}
