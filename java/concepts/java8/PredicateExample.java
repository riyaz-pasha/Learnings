import java.util.Arrays;
import java.util.function.Predicate;

public class PredicateExample {
    public static void main(String[] args) {
        PredicateFunctionalInterfaceImplementationLambda predicateFunctionalInterfaceImplementation = new PredicateFunctionalInterfaceImplementationLambda();
        predicateFunctionalInterfaceImplementation.test();
    }
}

class PredicateFunctionalInterfaceImplementationLambda {
    public void test() {
        Predicate<Integer> isEven = (integer) -> integer % 2 == 0;
        Predicate<Integer> isOdd = (Integer integer) -> integer % 2 != 0;

        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .stream()
                .filter(isEven)
                .forEach(System.out::println);
    }
}

class PredicateFunctionalInterfaceImplementationAnonymous {
    public void test() {
        Predicate<Integer> isEven = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                return integer % 2 == 0;
            }
        };
        Predicate<Integer> isOdd = new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) {
                return integer % 2 != 0;
            }
        };

        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .stream()
                .filter(isEven)
                .forEach(System.out::println);
    }
}
