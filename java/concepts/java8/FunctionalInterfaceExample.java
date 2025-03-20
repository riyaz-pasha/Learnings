class FunctionalInterfaceExample {
    public static void main(String[] args) {
        CalculatorImplementationTraditional calculatorImplementationTraditional = new CalculatorImplementationTraditional();
        System.out.println(calculatorImplementationTraditional.add(10, 20));

        CalculatorImplementationAnonymousClass calculatorImplementationAnonymousClass = new CalculatorImplementationAnonymousClass();
        calculatorImplementationAnonymousClass.add(10, 20);

        CalculatorImplementationFunctional calculatorImplementationFunctional = new CalculatorImplementationFunctional();
        calculatorImplementationFunctional.add(10, 20);
    }
}

@FunctionalInterface
interface Calculator {
    public int add(int firstNumber, int secondNumber);
}

class CalculatorImplementationTraditional implements Calculator {
    @Override
    public int add(int firstNumber, int secondNumber) {
        return firstNumber + secondNumber;
    }
}

class CalculatorImplementationAnonymousClass {
    public void add(int firstNumber, int secondNumber) {
        Calculator calculator = new Calculator() {
            @Override
            public int add(int firstNumber, int secondNumber) {
                return firstNumber + secondNumber;
            }
        };
        System.out.println(calculator.add(firstNumber, secondNumber));
    }
}

class CalculatorImplementationFunctional {
    public void add(int firstNumber, int secondNumber) {
        Calculator calculator = (int first, int second) -> first + second;
        System.out.println(calculator.add(firstNumber, secondNumber));
    }
}
