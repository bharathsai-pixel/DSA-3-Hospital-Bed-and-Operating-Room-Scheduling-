package models;

import java.util.ArrayList;
import java.util.List;

public class OperatingRoom {
    private int id;
    private int hospitalId;
    private String name;
    private List<Patient.SurgeryType> supportedSurgeries;
    private boolean available;
    private List<int[]> scheduledSlots; // each entry: [startTime, endTime, patientId]
    private int totalOperatingMinutes;
    private int totalIdleMinutes;

    public OperatingRoom(int id, int hospitalId, String name, List<Patient.SurgeryType> supportedSurgeries) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.name = name;
        this.supportedSurgeries = supportedSurgeries;
        this.available = true;
        this.scheduledSlots = new ArrayList<>();
        this.totalOperatingMinutes = 0;
        this.totalIdleMinutes = 0;
    }

    // Getters
    public int getId() { return id; }
    public int getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public List<Patient.SurgeryType> getSupportedSurgeries() { return supportedSurgeries; }
    public boolean isAvailable() { return available; }
    public List<int[]> getScheduledSlots() { return scheduledSlots; }
    public int getTotalOperatingMinutes() { return totalOperatingMinutes; }
    public int getTotalIdleMinutes() { return totalIdleMinutes; }

    public void setAvailable(boolean available) { this.available = available; }

    public boolean supportsSurgery(Patient.SurgeryType type) {
        return supportedSurgeries.contains(type);
    }

    /**
     * Find the earliest available start time at or after 'earliest'.
     * Returns the start time, or -1 if no slot can fit before 'deadline'.
     */
    public int findEarliestSlot(int earliest, int duration, int deadline) {
        if (scheduledSlots.isEmpty()) {
            return (earliest + duration <= deadline) ? earliest : -1;
        }

        // Sort slots by start time
        scheduledSlots.sort((a, b) -> a[0] - b[0]);

        // Check before first slot
        if (earliest + duration <= scheduledSlots.get(0)[0]) {
            return earliest;
        }

        // Check gaps between slots
        for (int i = 0; i < scheduledSlots.size() - 1; i++) {
            int gapStart = Math.max(earliest, scheduledSlots.get(i)[1]);
            int gapEnd = scheduledSlots.get(i + 1)[0];
            if (gapStart + duration <= gapEnd) {
                return gapStart;
            }
        }

        // Check after last slot
        int afterLast = Math.max(earliest, scheduledSlots.get(scheduledSlots.size() - 1)[1]);
        if (afterLast + duration <= deadline) {
            return afterLast;
        }

        return -1;
    }

    public void scheduleSlot(int startTime, int endTime, int patientId) {
        scheduledSlots.add(new int[]{startTime, endTime, patientId});
        totalOperatingMinutes += (endTime - startTime);
        scheduledSlots.sort((a, b) -> a[0] - b[0]);
    }

    public void removeSlot(int patientId) {
        scheduledSlots.removeIf(slot -> slot[2] == patientId);
        recalculateMetrics();
    }

    public void clearSchedule() {
        scheduledSlots.clear();
        totalOperatingMinutes = 0;
        totalIdleMinutes = 0;
    }

    public void recalculateMetrics() {
        totalOperatingMinutes = 0;
        for (int[] slot : scheduledSlots) {
            totalOperatingMinutes += (slot[1] - slot[0]);
        }
    }

    /**
     * Calculate utilization for a given time window.
     */
    public double getUtilization(int windowStart, int windowEnd) {
        int windowDuration = windowEnd - windowStart;
        if (windowDuration <= 0) return 0.0;
        int clippedMinutes = 0;
        for (int[] slot : scheduledSlots) {
            clippedMinutes += Math.max(0, Math.min(slot[1], windowEnd) - Math.max(slot[0], windowStart));
        }
        return Math.min(100.0, (double) clippedMinutes / windowDuration * 100.0);
    }

    public boolean isAvailableAt(int startTime, int duration) {
        for (int[] slot : scheduledSlots) {
            if (startTime < slot[1] && startTime + duration > slot[0]) {
                return false; // overlap detected
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("OR %-3d | Hospital H%d | %-15s | Surgeries: ", id, hospitalId, name));
        for (int i = 0; i < supportedSurgeries.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(supportedSurgeries.get(i));
        }
        sb.append(String.format(" | Scheduled: %d procedures", scheduledSlots.size()));
        return sb.toString();
    }
}
