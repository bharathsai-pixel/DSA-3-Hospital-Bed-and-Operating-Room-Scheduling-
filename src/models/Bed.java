package models;

public class Bed {
    public enum BedType {
        ICU("ICU"),
        GENERAL("General"),
        POST_OP("Post-Op");

        private final String label;

        BedType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private int id;
    private int hospitalId;
    private BedType type;
    private boolean occupied;
    private int assignedPatientId;
    private int occupiedUntil; // time in minutes when bed becomes free
    private int occupiedFrom;

    public Bed(int id, int hospitalId, BedType type) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.type = type;
        this.occupied = false;
        this.assignedPatientId = -1;
        this.occupiedUntil = 0;
        this.occupiedFrom = 0;
    }

    // Getters
    public int getId() { return id; }
    public int getHospitalId() { return hospitalId; }
    public BedType getType() { return type; }
    public boolean isOccupied() { return occupied; }
    public int getAssignedPatientId() { return assignedPatientId; }
    public int getOccupiedUntil() { return occupiedUntil; }
    public int getOccupiedFrom() { return occupiedFrom; }

    // Setters
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
    public void setAssignedPatientId(int patientId) { this.assignedPatientId = patientId; }
    public void setOccupiedUntil(int time) { this.occupiedUntil = time; }

    public void assignPatient(int patientId, int from, int until) {
        this.occupied = true;
        this.assignedPatientId = patientId;
        this.occupiedFrom = from;
        this.occupiedUntil = until;
    }

    /** Backward-compatible reservation starting at midnight. */
    public void assignPatient(int patientId, int until) { assignPatient(patientId, 0, until); }

    public void release() {
        this.occupied = false;
        this.assignedPatientId = -1;
        this.occupiedUntil = 0;
        this.occupiedFrom = 0;
    }

    public boolean isAvailableAt(int time) {
        return !occupied || time < occupiedFrom || time >= occupiedUntil;
    }

    public boolean isAvailableDuring(int start, int end) {
        return !occupied || end <= occupiedFrom || start >= occupiedUntil;
    }

    @Override
    public String toString() {
        String status = occupied
                ? String.format("Occupied (Patient P%d, until %d:%02d)", assignedPatientId, occupiedUntil / 60, occupiedUntil % 60)
                : "Available";
        return String.format("Bed %-3d | Hospital H%d | %-8s | %s", id, hospitalId, type.getLabel(), status);
    }
}
