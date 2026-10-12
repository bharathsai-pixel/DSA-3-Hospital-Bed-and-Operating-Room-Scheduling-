package web;

/** Original emergency API payload retained for existing clients. */
public record LegacyEmergencyIntakeRequest(String name, String surgeryType, int durationMinutes,
                                           boolean requiresICU, String arrivalTime) { }
