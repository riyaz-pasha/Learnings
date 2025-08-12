package parking.lot;

import parking.level.ParkingLevel;

import java.util.ArrayList;
import java.util.List;

public class ParkingLot {

    private final String id;
    private final List<ParkingLevel> levels = new ArrayList<>();

    public ParkingLot(String id) {
        this.id = id;
    }

    public void addLevel(int smallSpots, int mediumSpots, int largeSpots) {
        ParkingLevel level = new ParkingLevel(levels.size() + 1, smallSpots, mediumSpots, largeSpots);
        levels.add(level);
    }

    public List<ParkingLevel> getLevels() {
        return this.levels;
    }

}
