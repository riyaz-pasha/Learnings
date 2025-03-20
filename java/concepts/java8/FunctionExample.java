import java.util.Arrays;
import java.util.function.Function;

public class FunctionExample {
    public static void main(String[] args) {
        FunctionFunctionalInterfaceLambda functionFunctionalInterfaceLambda = new FunctionFunctionalInterfaceLambda();
        functionFunctionalInterfaceLambda.apply();

        FunctionFunctionalInterfaceImplementationAnonymous functionFunctionalInterfaceImplementationAnonymous = new FunctionFunctionalInterfaceImplementationAnonymous();
        functionFunctionalInterfaceImplementationAnonymous.apply();
    }
}

class FunctionFunctionalInterfaceLambda {
    public void apply() {
        Function<String, String> capitalize = (str) -> str.toUpperCase();
        Arrays.asList("a", "b", "c")
                .stream()
                .map(capitalize)
                .forEach(System.out::println);
    }
}

class FunctionFunctionalInterfaceImplementationAnonymous {
    public void apply() {
        Function<String, String> capitalize = new Function<String, String>() {
            @Override
            public String apply(String str) {
                return str.toUpperCase();
            }
        };
        Arrays.asList("a", "b", "c")
                .stream()
                .map(capitalize)
                .forEach(System.out::println);
    }
}
