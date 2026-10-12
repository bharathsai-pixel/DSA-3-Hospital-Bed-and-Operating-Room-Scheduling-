package models;

public class TimeSlot {
    private int startTime; // minutes from midnight
    private int endTime;
    private int orId;
    private int patientId;
    private boolean occupied;

    public TimeSlot(int startTime, int endTime, int orId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.orId = orId;
        this.patientId = -1;
        this.occupied = false;
    }

    // Getters
    public int getStartTime() { return startTime; }
    public int getEndTime() { return endTime; }
    public int getDuration() { return endTime - startTime; }
    public int getOrId() { return orId; }
    public int getPatientId() { return patientId; }
    public boolean isOccupied() { return occupied; }

    // Setters
    public void setPatientId(int patientId) {
        this.patientId = patientId;
        this.occupied = patientId >= 0;
    }

    public void release() {
        this.patientId = -1;
        this.occupied = false;
    }

    public String getTimeFormatted(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        return String.format("%d:%02d %s", hours, mins, ampm);
    }

    @Override
    public String toString() {
        String status = occupied ? String.format("Patient P%d", patientId) : "Available";
        return String.format("[%s - %s] OR%d | %s",
                getTimeFormatted(startTime), getTimeFormatted(endTime), orId, status);
    }
}
