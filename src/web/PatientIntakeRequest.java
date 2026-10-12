package web;

public record PatientIntakeRequest(String name, int age, String priority, String surgeryType,
                                   int durationMinutes, String arrivalTime, int preferredHospitalId,
                                   boolean requiresICU, Double latitude, Double longitude) { }
