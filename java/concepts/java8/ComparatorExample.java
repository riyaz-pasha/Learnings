import java.util.Comparator;
import java.util.List;

public class ComparatorExample {
    public static void main(String[] args) {
        ComparatorLambda comparatorLambda = new ComparatorLambda();
        comparatorLambda.compareTo();
    }
}

class ComparatorLambda {
    public void compareTo() {
        Comparator<String> nameComparator = (String name1, String name2) -> name1.compareTo(name2);
        List.of("John", "Doe", "Jane").stream()
                .sorted(nameComparator)
                // .sorted((name1, name2) -> name1.compareTo(name2))
                .forEach(System.out::println);
    }
}
