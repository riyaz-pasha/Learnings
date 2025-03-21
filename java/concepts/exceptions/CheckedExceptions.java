import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CheckedExceptions {
    public static void main(String[] args) {
        IOExceptionExample ioExceptionExample = new IOExceptionExample();
        ioExceptionExample.test();

        ThrowingExceptionFromMethodSignature throwingExceptionFromMethodSignature = new ThrowingExceptionFromMethodSignature();
        throwingExceptionFromMethodSignature.testArithmeticException();
    }
}

class IOExceptionExample {
    public void test() {
        try {
            File file = new File("nonexistent.txt");
            FileReader fr = new FileReader(file); // Causes FileNotFoundException
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            System.out.println("Execution completed.");
        }
    }
}

class ThrowingExceptionFromMethodSignature {
    public void testArithmeticException() throws ArithmeticException {
        throw new ArithmeticException("Maybe age not 0, can not devide by 0 etc");
    }
}
