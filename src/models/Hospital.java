package models;

import java.util.ArrayList;
import java.util.List;

public class Hospital {
    private int id;
    private String name;
    private String location;
    private double latitude;
    private double longitude;
    private List<Bed> beds;
    private List<OperatingRoom> operatingRooms;
    private List<Staff> staffMembers;

    public Hospital(int id, String name, String location, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.beds = new ArrayList<>();
        this.operatingRooms = new ArrayList<>();
        this.staffMembers = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public List<Bed> getBeds() { return beds; }
    public List<OperatingRoom> getOperatingRooms() { return operatingRooms; }
    public List<Staff> getStaffMembers() { return staffMembers; }

    public void addBed(Bed bed) { beds.add(bed); }
    public void addOperatingRoom(OperatingRoom or) { operatingRooms.add(or); }
    public void addStaff(Staff staff) { staffMembers.add(staff); }

    // Capacity queries
    public int getTotalBeds() { return beds.size(); }
    public int getAvailableBeds() {
        int count = 0;
        for (Bed bed : beds) if (!bed.isOccupied()) count++;
        return count;
    }

    public int getAvailableBedsAt(int time) {
        int count = 0;
        for (Bed bed : beds) if (bed.isAvailableAt(time)) count++;
        return count;
    }

    public int getICUBeds() {
        int count = 0;
        for (Bed bed : beds) if (bed.getType() == Bed.BedType.ICU) count++;
        return count;
    }

    public int getAvailableICUBeds() {
        int count = 0;
        for (Bed bed : beds)
            if (bed.getType() == Bed.BedType.ICU && !bed.isOccupied()) count++;
        return count;
    }

    public int getAvailableICUBedsAt(int time) {
        int count = 0;
        for (Bed bed : beds)
            if (bed.getType() == Bed.BedType.ICU && bed.isAvailableAt(time)) count++;
        return count;
    }

    public int getTotalORs() { return operatingRooms.size(); }

    public int getAvailableORs() {
        int count = 0;
        for (OperatingRoom or : operatingRooms) if (or.isAvailable()) count++;
        return count;
    }

    public boolean hasCapacity() {
        return getAvailableBeds() > 0 && getAvailableORs() > 0;
    }

    public boolean hasICUCapacity() {
        return getAvailableICUBeds() > 0;
    }

    /**
     * Calculate distance to another hospital in km (Haversine formula).
     */
    public double distanceTo(Hospital other) {
        return distanceFrom(other.latitude, other.longitude);
    }

    public double distanceFrom(double latitude, double longitude) {
        double R = 6371.0; // Earth radius in km
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(latitude);
        double dLat = Math.toRadians(latitude - this.latitude);
        double dLon = Math.toRadians(longitude - this.longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Check if a specific surgery type can be performed here.
     */
    public boolean canPerformSurgery(Patient.SurgeryType type) {
        for (OperatingRoom or : operatingRooms) {
            if (or.supportsSurgery(type)) return true;
        }
        return false;
    }

    public boolean hasQualifiedSurgeon(Patient.SurgeryType type) {
        for (Staff staff : staffMembers) {
            if (staff.getRole() == Staff.Role.SURGEON && staff.canPerformSurgery(type)) return true;
        }
        return false;
    }

    /** Percentage of beds reserved by scheduled patients in the current plan. */
    public double getBedReservationRate() {
        if (beds.isEmpty()) return 0.0;
        int occupied = 0;
        for (Bed bed : beds) if (bed.isOccupied()) occupied++;
        return (double) occupied / beds.size() * 100.0;
    }

    @Override
    public String toString() {
        return String.format("H%d | %-30s | %-20s | Beds: %d/%d | ORs: %d | Staff: %d",
                id, name, location, getAvailableBeds(), getTotalBeds(),
                getTotalORs(), staffMembers.size());
    }
}
