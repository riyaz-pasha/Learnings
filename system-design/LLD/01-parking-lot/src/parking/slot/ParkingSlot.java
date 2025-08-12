package parking.slot;

import vehicle.Vehicle;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ParkingSlot {

    protected final int slotNumber;
    protected final SlotType type;
    private final AtomicBoolean isOccupied = new AtomicBoolean(false);
    private Vehicle parkedVehicle; // check where it goes

    public ParkingSlot(int slotNumber, SlotType type) {
        this.slotNumber = slotNumber;
        this.type = type;
    }

    public boolean park(Vehicle vehicle) {
        return this.isOccupied.compareAndSet(false, true);
    }

}
