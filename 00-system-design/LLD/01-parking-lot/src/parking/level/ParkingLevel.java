package parking.level;

import parking.slot.LargeSlot;
import parking.slot.MediumSlot;
import parking.slot.ParkingSlot;
import parking.slot.SlotType;
import parking.slot.SmallSlot;
import vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ParkingLevel {

    private final int levelNumber;
    private final Map<Integer, ParkingSlot> allSlots; // Maps slot number to slot object
    private Map<SlotType, ConcurrentLinkedQueue<ParkingSlot>> availableSlots;

    public ParkingLevel(int levelNumber, int smallSpots, int mediumSpots, int largeSpots) {
        this.levelNumber = levelNumber;
        this.allSlots = new HashMap<>();
        this.availableSlots = new HashMap<>();

        this.availableSlots.put(SlotType.SMALL, new ConcurrentLinkedQueue<>());
        this.availableSlots.put(SlotType.MEDIUM, new ConcurrentLinkedQueue<>());
        this.availableSlots.put(SlotType.LARGE, new ConcurrentLinkedQueue<>());

        for (int i = 0; i < smallSpots; i++) {
            this.allSlots.put(i, new SmallSlot(i));
        }

        for (int i = 0; i < mediumSpots; i++) {
            this.allSlots.put(i, new MediumSlot(i));
        }

        for (int i = 0; i < largeSpots; i++) {
            this.allSlots.put(i, new LargeSlot(i));
        }
    }

    public synchronized Optional<ParkingSlot> findAndPark(Vehicle vehicle) {
        SlotType requiredType;
        switch (vehicle.getType()) {
            case MOTORBIKE:
                requiredType = SlotType.SMALL;
                break;
            case CAR:
                requiredType = SlotType.MEDIUM;
                break;
            case TRUCK:
                requiredType = SlotType.LARGE;
                break;
            default:
                return Optional.empty();
        }

        // Search for a suitable slot, including larger ones
        List<SlotType> searchOrder = new ArrayList<>();
        if (requiredType == SlotType.SMALL) {
            searchOrder.add(SlotType.SMALL);
            searchOrder.add(SlotType.MEDIUM);
            searchOrder.add(SlotType.LARGE);
        } else if (requiredType == SlotType.MEDIUM) {
            searchOrder.add(SlotType.MEDIUM);
            searchOrder.add(SlotType.LARGE);
        } else {
            searchOrder.add(SlotType.LARGE);
        }

        for (SlotType type : searchOrder) {
            ParkingSlot slot = availableSlots.get(type).poll();
            if (slot != null) {
                if (slot.park(vehicle)) {
                    return Optional.of(slot);
                } else {
                    // when failed, continue searching
                    // the slot may have been concurrently occupied; try next
                    continue;
                }
            }
        }
        return Optional.empty();
    }
}
