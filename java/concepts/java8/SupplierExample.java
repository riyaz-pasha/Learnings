import java.util.Arrays;
import java.util.function.Supplier;

public class SupplierExample {
    public static void main(String[] args) {
        SupplierFunctionalInterfaceLambda supplierFunctionalInterfaceLambda = new SupplierFunctionalInterfaceLambda();
        supplierFunctionalInterfaceLambda.get();
    }
}

class SupplierFunctionalInterfaceLambda {
    public void get() {
        Supplier<String> defaultValue = () -> "Default Value";
        System.out.println(Arrays.asList().stream().findAny().orElseGet(defaultValue));
    }
}
