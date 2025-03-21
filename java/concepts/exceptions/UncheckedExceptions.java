import java.util.Arrays;
import java.util.List;

public class UncheckedExceptions {
    public static void main(String[] args) {
        UncheckedExceptionExample example = new UncheckedExceptionExample();
        // example.testArithmeticException();
        // example.testNullPointerException();
        // example.testArrayIndexOutOfBoundsException();
        // example.testStringIndexOutOfBoundsException();
        // example.testNumberFormatException();
        // example.testClassCastException();
        example.testUnsupportedOperationExample();
    }
}

class UncheckedExceptionExample {
    public void testArithmeticException() {
        System.out.println(10 / 0);
    }

    public void testNullPointerException() {
        String str = null;
        System.out.println(str.length());
    }

    public void testArrayIndexOutOfBoundsException() {
        int[] arr = { 1, 2, 3 };
        System.out.println(arr[5]); // Causes ArrayIndexOutOfBoundsException
    }

    public void testStringIndexOutOfBoundsException() {
        String str = "hello";
        System.out.println(str.charAt(10)); // Causes StringIndexOutOfBoundsException
    }

    public void testNumberFormatException() {
        Integer.parseInt("Not A Number");
    }

    public void testClassCastException() {
        Object obj = "String";
        Integer a = (Integer) obj;
    }

    public void testUnsupportedOperationExample() {
        List<String> list = Arrays.asList("A", "B", "C");
        list.add("D");
    }

}
