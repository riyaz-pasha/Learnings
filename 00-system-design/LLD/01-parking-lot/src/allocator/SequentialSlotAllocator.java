package allocator;

import parking.level.ParkingLevel;
import parking.lot.ParkingLot;
import parking.slot.ParkingSlot;
import vehicle.Vehicle;

import java.util.Optional;

public class SequentialSlotAllocator implements SlotAllocator {

    private final ParkingLot parkingLot;

    public SequentialSlotAllocator(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    @Override
    public Optional<Integer> allocateSlot(Vehicle vehicle) {
        for (ParkingLevel level : this.parkingLot.getLevels()) {
            level.findAndPark(vehicle);
            return Optional.empty();
        }
    }

    @Override
    public void releaseSlot(String slotId) {

    }

}
