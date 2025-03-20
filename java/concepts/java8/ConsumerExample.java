import java.util.Arrays;
import java.util.function.Consumer;

public class ConsumerExample {
    public static void main(String[] args) {
        ConsumerFunctionalInterfaceImplementationLambda consumerFunctionalInterfaceImplementationLambda = new ConsumerFunctionalInterfaceImplementationLambda();
        consumerFunctionalInterfaceImplementationLambda.accept();

        ConsumerFunctionalInterfaceImplementationAnonymous consumerFunctionalInterfaceImplementationAnonymous = new ConsumerFunctionalInterfaceImplementationAnonymous();
        consumerFunctionalInterfaceImplementationAnonymous.accept();
    }
}

class ConsumerFunctionalInterfaceImplementationLambda {
    public void accept() {
        Consumer<String> printNumber = (String s) -> System.out.println(s);
        Arrays.asList("one", "two", "three").forEach(printNumber);
    }
}

class ConsumerFunctionalInterfaceImplementationAnonymous {
    public void accept() {
        Consumer<String> printNumber = new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println(s);
            }
        };
        Arrays.asList("one", "two", "three").forEach(printNumber);
    }
}
