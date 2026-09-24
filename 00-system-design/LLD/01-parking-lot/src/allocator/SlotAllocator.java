package allocator;

import vehicle.Vehicle;

import java.util.Optional;

public interface SlotAllocator {

    /**
     * Allocate suitable slot id for given vehicle. Returns slotId if allocated.
     */
    Optional<Integer> allocateSlot(Vehicle vehicle);

    void releaseSlot(String slotId);

}
