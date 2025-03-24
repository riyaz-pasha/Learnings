import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ReflectionExample {
    public static void main(String[] args) throws Exception {
        Class<?> MyTestClass = Class.forName(TestClass.class.getName());
        Constructor<?> MyTestClassConstructor = MyTestClass.getDeclaredConstructor();
        Object myTestClassObj = MyTestClassConstructor.newInstance();

        print(MyTestClass.getFields(), "getFields");
        print(MyTestClass.getDeclaredFields(), "getDeclaredFields");
        print(MyTestClass.getMethods(), "getMethods");
        print(MyTestClass.getDeclaredMethods(), "getDeclaredMethods");

        Field privateEmailField = MyTestClass.getDeclaredField("email");
        privateEmailField.setAccessible(true);
        System.out.println("Private Email: " + privateEmailField.get(myTestClassObj));

        Method privateSetEmailMethod = MyTestClass.getDeclaredMethod("setEmail", String.class);
        privateSetEmailMethod.setAccessible(true);
        privateSetEmailMethod.invoke(myTestClassObj, "newTest@email.com");

        System.out.println("Private Email: " + privateEmailField.get(myTestClassObj));

        Class<?> MathUtilsClass = Class.forName("MathUtils");
        Method squareMethod = MathUtilsClass.getMethod("square", int.class);
        System.out.println("Square : " + squareMethod.invoke(null, 3));

    }

    private static void print(Field[] fields, String type) {
        System.out.println(type);
        List.of(fields).stream().map(obj -> obj.getName()).forEach(System.out::println);
        System.out.println("=".repeat(25));
    }

    private static void print(Method[] methods, String type) {
        System.out.println(type);
        List.of(methods).stream().map(obj -> obj.getName()).forEach(System.out::println);
        System.out.println("=".repeat(25));
    }
}

class TestClass {
    int number = 0;
    String name = "Riyaz";
    private int age = 26;
    private String email = "test@email.com";
    public long id = 1234567890;
    protected List<String> interesets = List.of("Java", "Python", "JavaScript");

    int getNumber() {
        return number;
    }

    private void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }
}

class MathUtils {
    public static int square(int num) {
        return num * num;
    }
}
