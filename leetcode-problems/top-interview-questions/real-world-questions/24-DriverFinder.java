/* Given coordinates, return available drivers within K kilometers. */

import java.util.ArrayList;
import java.util.List;

class Location {

    double longitude; // X-axis
    double latitude; // Y-axis

    Location(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

}

class Driver {

    int id;
    Location location;

}

class DriverFinder {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /*
     * Let N be the number of drivers.
     * Loop through all drivers → O(N)
     * Haversine formula per driver → O(1)
     * 👉 Total Time Complexity: O(N)
     */
    public List<Driver> findNearbyDrivers(List<Driver> drivers, Location userLocation, double maxDistanceKm) {
        List<Driver> nearbyDrivers = new ArrayList<>();

        for (Driver driver : drivers) {
            double distance = this.haversineDistance(userLocation, driver.location);
            if (distance <= maxDistanceKm) {
                nearbyDrivers.add(driver);
            }
        }

        return nearbyDrivers;
    }

    private double haversineDistance(Location location1, Location location2) {
        double latDistance = Math.toRadians(location2.latitude - location1.latitude);
        double longDistance = Math.toRadians(location2.longitude - location1.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(location1.latitude)) * Math.cos(Math.toRadians(location2.latitude))
                        * Math.sin(longDistance / 2) * Math.sin(longDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

}

// ---------------------------------------------------------------------------------------

class BoundingBox {

    Location min;
    Location max;

    BoundingBox(Location min, Location max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(Driver driver) {
        return driver.location.latitude >= this.min.latitude
                && driver.location.latitude <= this.max.latitude
                && driver.location.longitude >= this.min.longitude
                && driver.location.longitude <= this.max.longitude;
    }

}

class QuadTreeNode {

    private BoundingBox boundary;
    private List<Driver> drivers;
    private int capacity;
    private boolean isDivided = false; // isLeaf
    private QuadTreeNode[] children; // 0: NE, 1: NW, 2: SW, 3: SE

    public QuadTreeNode(BoundingBox boundary, int capacity) {
        this.boundary = boundary;
        this.capacity = capacity;
        this.isDivided = false;
    }

    public boolean insert(Driver driver) {
        if (!this.boundary.contains(driver)) {
            return false;
        }

        if (drivers.size() < this.capacity) {
            this.drivers.add(driver);
            return true;
        }

        if (!this.isDivided) {
            this.subdivide();
        }

        for (QuadTreeNode child : children) {
            if (child.insert(driver)) {
                return true;
            }
        }
        return false;
    }

    private void subdivide() {
        double midLat = (this.boundary.min.latitude + this.boundary.max.latitude) / 2;
        double midLon = (this.boundary.min.longitude + this.boundary.max.longitude) / 2;

        children[0] = new QuadTreeNode(new BoundingBox(
                new Location(midLon, midLat),
                new Location(this.boundary.max.longitude, this.boundary.max.latitude)), this.capacity);
        children[1] = new QuadTreeNode(new BoundingBox(
                new Location(this.boundary.min.longitude, midLat),
                new Location(midLat, this.boundary.max.latitude)), this.capacity);
        children[2] = new QuadTreeNode(new BoundingBox(
                new Location(midLon, this.boundary.min.latitude),
                new Location(this.boundary.max.longitude, midLat)), this.capacity);
        children[3] = new QuadTreeNode(new BoundingBox(
                new Location(this.boundary.min.longitude, this.boundary.min.latitude),
                new Location(midLon, midLat)), this.capacity);

        this.isDivided = true;

        // Redistribute points to children
        for (Driver driver : drivers) {
            for (QuadTreeNode child : children) {
                if (child.insert(driver)) {
                    break;
                }
            }
        }
        drivers.clear(); // Clear drivers from parent node
    }

    public List<Driver> query(Location center, double radiusKm) {
        List<Driver> results = new ArrayList<>();

        if (!this.intersects(center, radiusKm)) {
            return results;
        }

        if (!this.isDivided) {
            for (Driver driver : this.drivers) {
                if ((driver.location.longitude >= (center.longitude - radiusKm))
                        && (driver.location.longitude <= (center.longitude + radiusKm))
                        && (driver.location.latitude >= (center.latitude - radiusKm))
                        && (driver.location.latitude <= (center.latitude + radiusKm))) {
                    results.add(driver);
                }
            }
        } else {
            for (QuadTreeNode child : this.children) {
                results.addAll(child.query(center, radiusKm));
            }
        }

        return results;
    }

    private boolean intersects(Location center, double radiusKm) {
        return !((this.boundary.min.longitude > (center.longitude + radiusKm))
                || (this.boundary.max.longitude < (center.longitude - radiusKm))
                || (this.boundary.min.latitude > (center.latitude + radiusKm))
                || (this.boundary.max.latitude > (center.latitude - radiusKm)));
    }

}
