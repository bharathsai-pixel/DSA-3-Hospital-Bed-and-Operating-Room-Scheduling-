package web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EmergencyIntakeRequest(
        @NotBlank(message = "Patient name is required.")
        @Size(max = 100, message = "Patient name must be 100 characters or fewer.")
        String name,
        @NotNull(message = "Age is required.")
        @Min(value = 0, message = "Age must be at least 0.")
        @Max(value = 120, message = "Age must be 120 or younger.")
        Integer age,
        @Pattern(regexp = "^(|Male|Female|Other)$", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Choose Male, Female, or Other.")
        String gender,
        @Pattern(regexp = "^(|A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-)$",
                message = "Choose a valid blood group.")
        String bloodGroup,
        @Size(max = 500, message = "Allergies must be 500 characters or fewer.")
        String allergies,
        @NotBlank(message = "Surgery type is required.")
        String surgeryType,
        @NotNull(message = "Surgery duration is required.")
        @Positive(message = "Surgery duration must be a positive number of minutes.")
        Integer durationMinutes,
        @NotBlank(message = "Arrival time is required.")
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "Enter arrival time in HH:mm format.")
        String arrivalTime,
        @NotNull(message = "Select whether the patient requires an ICU bed.")
        Boolean requiresICU
) { }
