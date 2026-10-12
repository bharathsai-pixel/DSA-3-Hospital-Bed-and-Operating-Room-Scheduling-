package scheduling;

import algorithms.ApproxScheduler;
import models.Hospital;
import models.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class SchedulingEngine {

    public static void runFullPipeline(List<Patient> patients, List<Hospital> hospitals) {
        runFullPipeline(patients, hospitals, 480);
    }

    private static void runFullPipeline(List<Patient> patients, List<Hospital> hospitals, int routingTime) {
        runFullPipeline(patients, hospitals, routingTime, 480);
    }

    private static void runFullPipeline(List<Patient> patients, List<Hospital> hospitals,
                                       int routingTime, int planningStartTime) {
        System.out.println("Starting Full Scheduling Pipeline...\n");

        List<Patient> activePatients = new ArrayList<>();
        for (Patient patient : patients) {
            if (!patient.isDischarged()) activePatients.add(patient);
        }

        System.out.println("Step 1: Network Flow -> Routing Patients across Hospitals...");
        Map<Integer, List<Patient>> hospitalRouting = HospitalRouter.routePatients(activePatients, hospitals, routingTime);

        for (Hospital h : hospitals) {
            List<Patient> routedPatients = hospitalRouting.get(h.getId());
            if (routedPatients == null || routedPatients.isEmpty()) continue;

            System.out.println("--> Hospital " + h.getName() + " received " + routedPatients.size() + " patients.");

            System.out.println("    Step 2: Approximation Algorithm -> Load Balancing across ORs...");
            // Use LPT to roughly assign patients to specific ORs fairly
            Map<Integer, List<Patient>> orAssignments = ApproxScheduler.balanceLoad(routedPatients, h.getOperatingRooms());

            System.out.println("    Step 3: Dynamic Programming -> Optimizing Time Slots within ORs...");
            List<Map.Entry<Integer, List<Patient>>> roomOrder = new ArrayList<>(orAssignments.entrySet());
            roomOrder.sort((left, right) -> {
                int byLoad = Integer.compare(left.getValue().size(), right.getValue().size());
                return byLoad != 0 ? byLoad : Integer.compare(left.getKey(), right.getKey());
            });
            for (Map.Entry<Integer, List<Patient>> assignment : roomOrder) {
                h.getOperatingRooms().stream()
                        .filter(or -> or.getId() == assignment.getKey()).findFirst()
                        .ifPresent(or -> ORScheduler.scheduleOR(or, assignment.getValue(), h.getStaffMembers(),
                                480, 1200, planningStartTime));
            }

            System.out.println("    Step 4: Hungarian Algorithm -> Assigning Beds to scheduled patients...");
            List<Patient> toAssignBeds = new ArrayList<>();
            for (Patient p : routedPatients) {
                if (p.isScheduled()) toAssignBeds.add(p);
            }
            BedAssigner.assignBeds(h, toAssignBeds, 480);

            // A procedure is committed only if both its OR/staff and its complete bed interval fit.
            for (Patient patient : routedPatients) {
                if (patient.isScheduled() && patient.getAssignedBedId() == -1) {
                    h.getOperatingRooms().forEach(or -> or.removeSlot(patient.getId()));
                    h.getStaffMembers().forEach(staff -> staff.removeSlot(patient.getId()));
                    patient.resetSchedule();
                }
            }
        }

        System.out.println("\nPipeline execution complete.\n");
    }

    public static void handleEmergency(List<Patient> patients, List<Hospital> hospitals, Scanner scanner) {
        System.out.println("\n--- Emergency Intake ---");
        System.out.print("Patient Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Patient name cannot be empty.");
        System.out.print("Surgery Type (CARDIAC, ORTHOPEDIC, NEUROLOGICAL, GENERAL, OPHTHALMIC, ENT): ");
        Patient.SurgeryType type;
        try {
            type = Patient.SurgeryType.valueOf(scanner.nextLine().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Choose one of the listed surgery types.");
        }
        System.out.print("Estimated Duration (mins): ");
        int duration = Integer.parseInt(scanner.nextLine());
        if (duration <= 0) throw new IllegalArgumentException("Duration must be a positive number of minutes.");
        System.out.print("Requires ICU? (true/false): ");
        String icuInput = scanner.nextLine().trim();
        if (!icuInput.equalsIgnoreCase("true") && !icuInput.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("Enter true or false for ICU requirement.");
        boolean requiresICU = Boolean.parseBoolean(icuInput);
        System.out.print("Current/arrival time (HH:mm, 24-hour format, e.g. 14:30): ");
        int arrivalTime = parseClockTime(scanner.nextLine());

        System.out.println("\nRe-evaluating schedules (Network Flow + DP) dynamically...");
        Patient emergencyPatient = processEmergency(patients, hospitals, name, type, duration, requiresICU, arrivalTime);
        System.out.println("\n[!] Emergency Patient Added: " + emergencyPatient.getName());
    }

    public static Patient processEmergency(List<Patient> patients, List<Hospital> hospitals,
                                           String name, Patient.SurgeryType type, int duration,
                                           boolean requiresICU, int arrivalTime) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Patient name cannot be empty.");
        if (type == null) throw new IllegalArgumentException("Choose one of the listed surgery types.");
        if (duration <= 0) throw new IllegalArgumentException("Duration must be a positive number of minutes.");
        if (arrivalTime < 0 || arrivalTime >= 1440) throw new IllegalArgumentException("Arrival time must be within one day.");

        int newId = patients.stream().mapToInt(Patient::getId).max().orElse(0) + 1;
        Patient emergencyPatient = new Patient(newId, name.trim(), 40, Patient.Priority.EMERGENCY,
                type, duration, arrivalTime, 1, requiresICU);
        patients.add(emergencyPatient);
        // Re-run the pipeline to slot the emergency patient in
        // A robust system would clear future schedules and re-run for pending patients.
        for (Patient p : patients) {
            // At the arrival instant, a case has not started yet; it is eligible for rescheduling.
            // Only cases with start < arrivalTime are immutable.
            if (!p.isDischarged() && p.isScheduled() && p.getScheduledStartTime() >= arrivalTime) {
                Hospital h = hospitals.stream()
                        .filter(hospital -> hospital.getId() == p.getAssignedHospitalId())
                        .findFirst().orElse(null);

                if (h != null) {
                    h.getOperatingRooms().forEach(or -> or.removeSlot(p.getId()));
                    h.getStaffMembers().forEach(staff -> staff.removeSlot(p.getId()));
                    h.getBeds().forEach(b -> { if(b.getAssignedPatientId() == p.getId()) b.release(); });
                }
                p.resetSchedule();
            }
        }

        // Re-run for unscheduled patients
        List<Patient> pending = new ArrayList<>();
        for (Patient p : patients) {
            if (!p.isScheduled() && !p.isDischarged()) pending.add(p);
        }

        runFullPipeline(pending, hospitals, arrivalTime, arrivalTime);
        return emergencyPatient;
    }

    public static void dischargePatient(List<Patient> patients, List<Hospital> hospitals, Scanner scanner) {
        System.out.println("\n--- Patient Discharge ---");
        System.out.print("Patient ID: ");
        int patientId = Integer.parseInt(scanner.nextLine());
        System.out.print("Discharge time (HH:mm, 24-hour format, e.g. 16:45): ");
        dischargePatient(patients, hospitals, patientId, scanner.nextLine());
    }

    public static Patient dischargePatient(List<Patient> patients, List<Hospital> hospitals,
                                           int patientId, String dischargeTimeText) {
        Patient patient = patients.stream()
                .filter(p -> p.getId() == patientId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No patient found with ID P" + patientId + "."));

        if (patient.isDischarged()) {
            throw new IllegalArgumentException("Patient P" + patientId + " has already been discharged.");
        }
        if (!patient.isScheduled() || patient.getAssignedBedId() == -1) {
            throw new IllegalArgumentException("Patient P" + patientId + " has no assigned bed to discharge.");
        }

        int dischargeTime = parseClockTime(dischargeTimeText);
        int surgeryEnd = patient.getScheduledStartTime() + patient.getSurgeryDuration();
        if (dischargeTime < surgeryEnd) {
            throw new IllegalArgumentException("Discharge time cannot be before surgery completion at "
                    + formatClockTime(surgeryEnd) + ".");
        }

        Hospital hospital = hospitals.stream()
                .filter(h -> h.getId() == patient.getAssignedHospitalId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Assigned hospital could not be found."));
        int bedId = patient.getAssignedBedId();
        hospital.getBeds().stream()
                .filter(b -> b.getId() == bedId && b.getAssignedPatientId() == patientId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Assigned bed record could not be found."))
                .release();

        patient.discharge(dischargeTime);
        System.out.println("Patient P" + patientId + " (" + patient.getName() + ") discharged from "
                + hospital.getName() + "; Bed " + bedId + " is now available.");
        return patient;
    }

    public static int parseClockTime(String value) {
        try {
            if (value == null) throw new DateTimeParseException("Missing time", "", 0);
            LocalTime time = LocalTime.parse(value.trim(),
                    DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT));
            return time.getHour() * 60 + time.getMinute();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Enter a valid 24-hour time in HH:mm format, such as 14:30.");
        }
    }

    private static String formatClockTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
