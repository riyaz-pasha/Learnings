import java.util.List;

public class MethodReferenceExample {
    public static void main(String[] args) {
        List.of("john", "doe", "jane").stream()
                .map(Util::capitalizeFirstChar) // static method reference
                .map(User::new) // constructor reference
                .map(User::getName) // instance method reference
                .forEach(System.out::println); // instance method reference
    }
}

class User {
    private String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class Util {
    public static String capitalizeFirstChar(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
