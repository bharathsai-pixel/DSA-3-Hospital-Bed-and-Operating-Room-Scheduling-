package models;

import java.util.ArrayList;
import java.util.List;

public class Staff {
    public enum Role {
        SURGEON("Surgeon"),
        ANESTHETIST("Anesthetist"),
        NURSE("Nurse");

        private final String label;

        Role(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private int id;
    private String name;
    private Role role;
    private int hospitalId;
    private List<Patient.SurgeryType> specializations;
    private int shiftStart; // minutes from midnight
    private int shiftEnd;
    private List<int[]> assignedSlots; // [startTime, endTime, patientId]

    public Staff(int id, String name, Role role, int hospitalId,
                 List<Patient.SurgeryType> specializations, int shiftStart, int shiftEnd) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.hospitalId = hospitalId;
        this.specializations = specializations;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.assignedSlots = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public int getHospitalId() { return hospitalId; }
    public List<Patient.SurgeryType> getSpecializations() { return specializations; }
    public int getShiftStart() { return shiftStart; }
    public int getShiftEnd() { return shiftEnd; }
    public List<int[]> getAssignedSlots() { return assignedSlots; }

    public boolean isAvailableDuring(int startTime, int endTime) {
        // Check if within shift hours
        if (startTime < shiftStart || endTime > shiftEnd) return false;

        // Check for conflicts with existing assignments
        for (int[] slot : assignedSlots) {
            if (startTime < slot[1] && endTime > slot[0]) {
                return false; // overlap
            }
        }
        return true;
    }

    public boolean canPerformSurgery(Patient.SurgeryType type) {
        return specializations.contains(type);
    }

    public void assignSlot(int startTime, int endTime, int patientId) {
        assignedSlots.add(new int[]{startTime, endTime, patientId});
        assignedSlots.sort((a, b) -> a[0] - b[0]);
    }

    public void removeSlot(int patientId) {
        assignedSlots.removeIf(slot -> slot[2] == patientId);
    }

    public void clearAssignments() {
        assignedSlots.clear();
    }

    public String getShiftFormatted() {
        return formatTime(shiftStart) + " - " + formatTime(shiftEnd);
    }

    private String formatTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        return String.format("%d:%02d %s", hours, mins, ampm);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("S%-2d | %-18s | %-12s | Hospital H%d | Shift: %s | Specializations: ",
                id, name, role.getLabel(), hospitalId, getShiftFormatted()));
        for (int i = 0; i < specializations.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(specializations.get(i));
        }
        return sb.toString();
    }
}
