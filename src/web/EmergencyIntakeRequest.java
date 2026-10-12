package web;

public record EmergencyIntakeRequest(
        String name,
        String surgeryType,
        int durationMinutes,
        boolean requiresICU,
        String arrivalTime
) { }
