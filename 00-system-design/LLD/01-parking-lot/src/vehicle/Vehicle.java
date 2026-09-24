package vehicle;

public abstract class Vehicle {

    private final String registrationNumber;
    private final VehicleType vehicleType;

    protected Vehicle(String registrationNumber, VehicleType vehicleType) {
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
    }


    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getType() {
        return vehicleType;
    }

}
