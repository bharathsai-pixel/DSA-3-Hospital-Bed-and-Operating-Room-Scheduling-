package models;

public class Patient {
    public enum Priority {
        EMERGENCY(1, "Emergency"),
        URGENT(2, "Urgent"),
        ELECTIVE(3, "Elective");

        private final int level;
        private final String label;

        Priority(int level, String label) {
            this.level = level;
            this.label = label;
        }

        public int getLevel() { return level; }
        public String getLabel() { return label; }
    }

    public enum SurgeryType {
        CARDIAC, ORTHOPEDIC, NEUROLOGICAL, GENERAL, OPHTHALMIC, ENT
    }

    private int id;
    private String name;
    private int age;
    private Priority priority;
    private SurgeryType surgeryType;
    private int surgeryDuration; // in minutes
    private int arrivalTime;     // minutes from start of day (e.g., 480 = 8:00 AM)
    private int preferredHospitalId;
    private boolean requiresICU;
    private boolean scheduled;
    private int assignedBedId;
    private int assignedORId;
    private int assignedHospitalId;
    private int assignedStaffId;
    private int scheduledStartTime;
    private int waitingTime;
    private boolean discharged;
    private int dischargeTime;
    private double latitude;
    private double longitude;

    public Patient(int id, String name, int age, Priority priority, SurgeryType surgeryType,
                   int surgeryDuration, int arrivalTime, int preferredHospitalId, boolean requiresICU) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.priority = priority;
        this.surgeryType = surgeryType;
        this.surgeryDuration = surgeryDuration;
        this.arrivalTime = arrivalTime;
        this.preferredHospitalId = preferredHospitalId;
        this.requiresICU = requiresICU;
        this.scheduled = false;
        this.assignedBedId = -1;
        this.assignedORId = -1;
        this.assignedHospitalId = -1;
        this.assignedStaffId = -1;
        this.scheduledStartTime = -1;
        this.waitingTime = 0;
        this.discharged = false;
        this.dischargeTime = -1;
        this.latitude = 17.3850;
        this.longitude = 78.4867;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public Priority getPriority() { return priority; }
    public SurgeryType getSurgeryType() { return surgeryType; }
    public int getSurgeryDuration() { return surgeryDuration; }
    public int getArrivalTime() { return arrivalTime; }
    public int getPreferredHospitalId() { return preferredHospitalId; }
    public boolean requiresICU() { return requiresICU; }
    public boolean isScheduled() { return scheduled; }
    public int getAssignedBedId() { return assignedBedId; }
    public int getAssignedORId() { return assignedORId; }
    public int getAssignedHospitalId() { return assignedHospitalId; }
    public int getAssignedStaffId() { return assignedStaffId; }
    public int getScheduledStartTime() { return scheduledStartTime; }
    public int getWaitingTime() { return waitingTime; }
    public boolean isDischarged() { return discharged; }
    public int getDischargeTime() { return dischargeTime; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // Setters
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setPreferredHospitalId(int hospitalId) { this.preferredHospitalId = hospitalId; }
    public void setArrivalTime(int arrivalTime) {
        if (arrivalTime < 0 || arrivalTime >= 1440) throw new IllegalArgumentException("Arrival time must be within one day.");
        this.arrivalTime = arrivalTime;
    }
    public void setScheduled(boolean scheduled) { this.scheduled = scheduled; }
    public void setAssignedBedId(int bedId) { this.assignedBedId = bedId; }
    public void setAssignedORId(int orId) { this.assignedORId = orId; }
    public void setAssignedHospitalId(int hospitalId) { this.assignedHospitalId = hospitalId; }
    public void setAssignedStaffId(int staffId) { this.assignedStaffId = staffId; }
    public void setScheduledStartTime(int startTime) { this.scheduledStartTime = startTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }
    public void setLocation(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Invalid latitude/longitude.");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void discharge(int time) {
        this.discharged = true;
        this.dischargeTime = time;
        this.assignedBedId = -1;
    }

    public void resetSchedule() {
        this.scheduled = false;
        this.assignedBedId = -1;
        this.assignedORId = -1;
        this.assignedHospitalId = -1;
        this.assignedStaffId = -1;
        this.scheduledStartTime = -1;
        this.waitingTime = 0;
    }

    public String getStartTimeFormatted() {
        if (scheduledStartTime < 0) return "Not Scheduled";
        int hours = scheduledStartTime / 60;
        int mins = scheduledStartTime % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        return String.format("%d:%02d %s", hours, mins, ampm);
    }

    public String getArrivalTimeFormatted() {
        int hours = arrivalTime / 60;
        int mins = arrivalTime % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        return String.format("%d:%02d %s", hours, mins, ampm);
    }

    @Override
    public String toString() {
        String status = discharged
                ? "Discharged at " + formatTime(dischargeTime)
                : (scheduled ? "Scheduled" : "Pending");
        return String.format("P%-2d | %-18s | Age: %-3d | %-9s | %-12s | %3d min | Arrival: %s | ICU: %s | %s",
                id, name, age, priority.getLabel(), surgeryType, surgeryDuration,
                getArrivalTimeFormatted(), requiresICU ? "Yes" : "No", status);
    }

    private String formatTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }
}
