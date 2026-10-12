import algorithms.DPScheduler;
import algorithms.HungarianAlgorithm;
import algorithms.KMPAlgorithm;
import algorithms.MinCostMaxFlow;
import algorithms.RandomizedQuickSort;
import data.SampleData;
import models.Bed;
import models.Hospital;
import models.OperatingRoom;
import models.Patient;
import models.Staff;
import scheduling.HospitalRouter;
import scheduling.SchedulingEngine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class RegressionTests {
    private static int passed;

    public static void main(String[] args) {
        testMinCostMaxFlow();
        testHospitalPreferenceAndDistance();
        testExplicitIcuCapacity();
        testHungarianAssignments();
        testKmpBoundaries();
        testRandomizedSortAndLpt();
        testRandomizedSortCases();
        testDpExistingSlotsAlternativesAndWaiting();
        testClippedUtilization();
        testPipelineInvariantsAndBedIntervals();
        testBedAssignmentFailureRollsBackReservations();
        testManualDischargeKeepsProcedureHistory();
        testEmergencyValidationAndStartedWork();
        testEmergencyPlanningStartBound();
        testEmergencyPipelinePlanningStart();
        System.out.println("Regression tests passed: " + passed);
    }

    private static void testMinCostMaxFlow() {
        MinCostMaxFlow flow = new MinCostMaxFlow(4);
        flow.addEdge(0, 1, 1, 0);
        flow.addEdge(0, 2, 1, 0);
        flow.addEdge(1, 3, 1, 9);
        flow.addEdge(2, 3, 1, 2);
        int[] result = flow.solve(0, 3);
        check(result[0] == 2 && result[1] == 11, "min-cost maximum flow");
    }

    private static void testHospitalPreferenceAndDistance() {
        Hospital preferred = hospital(1, 17.50, 78.50, Bed.BedType.GENERAL);
        Hospital nearby = hospital(2, 17.386, 78.487, Bed.BedType.GENERAL);
        addGeneralSurgery(preferred, 1);
        addGeneralSurgery(nearby, 2);
        Patient patient = patient(1, Patient.SurgeryType.GENERAL, false, 2, 60);
        Map<Integer, List<Patient>> routed = HospitalRouter.routePatients(Arrays.asList(patient), Arrays.asList(preferred, nearby));
        check(routed.get(2).size() == 1, "preferred capable hospital has priority over distance");

        patient.setPreferredHospitalId(99);
        patient.setLocation(17.385, 78.4867);
        routed = HospitalRouter.routePatients(Arrays.asList(patient), Arrays.asList(preferred, nearby));
        check(routed.get(2).size() == 1, "nearest hospital wins when there is no preferred option");
    }

    private static void testExplicitIcuCapacity() {
        Hospital hospital = new Hospital(1, "ICU test", "test", 0, 0);
        hospital.addBed(new Bed(1, 1, Bed.BedType.ICU));
        hospital.addOperatingRoom(new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.CARDIAC)));
        hospital.addStaff(new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                Arrays.asList(Patient.SurgeryType.CARDIAC), 0, 1440));
        List<Patient> patients = Arrays.asList(
                patient(1, Patient.SurgeryType.CARDIAC, true, 1, 60),
                patient(2, Patient.SurgeryType.CARDIAC, true, 1, 60));
        Map<Integer, List<Patient>> routed = HospitalRouter.routePatients(patients, Arrays.asList(hospital));
        check(routed.get(1).size() == 1, "ICU route capacity is limited by ICU bed count");
    }

    private static void testHungarianAssignments() {
        double[][] square = {{4, 1, 3}, {2, 0, 5}, {3, 2, 2}};
        int[] assignment = new HungarianAlgorithm(square).execute();
        check(totalCost(square, assignment) == 5, "known square assignment optimum");

        double[][] rectangular = {{1, 9}, {2, 3}, {4, 1}};
        assignment = new HungarianAlgorithm(rectangular).execute();
        check(countAssigned(assignment) == 2 && totalCost(rectangular, assignment) == 2,
                "rectangular assignment leaves one row unmatched");

        double[][] forbidden = {{Double.POSITIVE_INFINITY, 4}, {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY}};
        assignment = new HungarianAlgorithm(forbidden).execute();
        check(assignment[0] == 1 && assignment[1] == -1, "forbidden edges return unmatched row");
    }

    private static void testKmpBoundaries() {
        check(KMPAlgorithm.search("Alice Brown", "ALICE"), "case-insensitive substring");
        check(KMPAlgorithm.search("abababca", "ababca"), "KMP prefix fallback");
        check(!KMPAlgorithm.search("Alice", "Bob"), "KMP absent substring");
        check(KMPAlgorithm.search("Alice", ""), "empty KMP pattern");
        check(!KMPAlgorithm.search("", "a"), "empty text");
    }

    private static void testRandomizedSortAndLpt() {
        List<Patient> patients = new ArrayList<>(Arrays.asList(
                patient(1, Patient.SurgeryType.GENERAL, false, 1, 20),
                patient(2, Patient.SurgeryType.GENERAL, false, 1, 90),
                patient(3, Patient.SurgeryType.GENERAL, false, 1, 50)));
        RandomizedQuickSort.sortDescendingByDuration(patients);
        check(patients.get(0).getSurgeryDuration() == 90 && patients.get(2).getSurgeryDuration() == 20,
                "randomized quicksort descending order");
        List<OperatingRoom> ors = Arrays.asList(
                new OperatingRoom(1, 1, "A", Arrays.asList(Patient.SurgeryType.GENERAL)),
                new OperatingRoom(2, 1, "B", Arrays.asList(Patient.SurgeryType.GENERAL)));
        Map<Integer, List<Patient>> allocation = algorithms.ApproxScheduler.balanceLoad(patients, ors);
        check(allocation.get(1).size() + allocation.get(2).size() == 3
                        && allocation.get(1).size() == 1 && allocation.get(2).size() == 2,
                "LPT assigns every compatible case to the least-loaded OR");
    }

    private static void testRandomizedSortCases() {
        checkSortedDurations(new int[0], "quicksort empty input");
        checkSortedDurations(new int[]{42}, "quicksort single input");
        checkSortedDurations(new int[]{10, 20, 30, 40, 50}, "quicksort already sorted input");
        checkSortedDurations(new int[]{50, 40, 30, 20, 10}, "quicksort reverse sorted input");
        checkSortedDurations(new int[]{5, 5, 5, 5, 5, 1, 9, 9}, "quicksort duplicate values");
        checkSortedDurations(new int[]{17, 3, 41, 8, 29, 11, 2, 37}, "quicksort mixed random input");
    }

    private static void checkSortedDurations(int[] durations, String name) {
        List<Patient> input = new ArrayList<>();
        for (int i = 0; i < durations.length; i++)
            input.add(patient(i + 1, Patient.SurgeryType.GENERAL, false, 1, durations[i]));
        RandomizedQuickSort.sortDescendingByDuration(input);
        for (int i = 1; i < input.size(); i++)
            check(input.get(i - 1).getSurgeryDuration() >= input.get(i).getSurgeryDuration(), name);
        if (input.size() < 2) check(true, name);
    }

    private static void testDpExistingSlotsAlternativesAndWaiting() {
        Hospital hospital = hospital(1, 0, 0, Bed.BedType.GENERAL);
        OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
        hospital.addOperatingRoom(or);
        or.scheduleSlot(480, 540, 90);
        Staff surgeon = new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                Arrays.asList(Patient.SurgeryType.GENERAL), 480, 800);
        surgeon.assignSlot(540, 600, 99);
        Patient later = patient(1, Patient.SurgeryType.GENERAL, false, 1, 60);
        later.setArrivalTime(480);
        Patient urgent = patient(2, Patient.SurgeryType.GENERAL, false, 1, 30);
        urgent.setArrivalTime(480);
        urgent.setPriority(Patient.Priority.EMERGENCY);
        List<DPScheduler.Job> jobs = DPScheduler.schedulePatients(Arrays.asList(later, urgent), or,
                Arrays.asList(surgeon), 480, 700);
        check(jobs.size() == 2, "DP schedules alternative sequential starts after existing OR slot");
        check(jobs.get(0).startTime == 600 && jobs.get(1).startTime == 630,
                "DP respects OR bookings, surgeon availability, and avoids overlap");
        check(jobs.get(0).patient == urgent, "priority and waiting cost influence DP order");
    }

    private static void testClippedUtilization() {
        OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
        or.scheduleSlot(420, 540, 1); // 07:00-09:00; only 60 minutes are inside 08:00-20:00.
        check(Math.abs(or.getUtilization(480, 1200) - (60.0 / 720.0 * 100.0)) < 0.001,
                "OR utilization clips work to reporting window");
        or.scheduleSlot(540, 1300, 2);
        check(or.getUtilization(480, 1200) <= 100.0, "utilization is capped at window capacity");
    }

    private static void testPipelineInvariantsAndBedIntervals() {
        List<Hospital> hospitals = SampleData.getHospitals();
        List<Patient> patients = SampleData.getPatients();
        runPipelineQuietly(patients, hospitals);
        int slots = countSlots(hospitals);
        int flags = (int) patients.stream().filter(Patient::isScheduled).count();
        check(slots == flags, "scheduled flags equal OR slot count");
        for (Patient patient : patients) {
            if (!patient.isScheduled()) continue;
            check(patient.getAssignedBedId() >= 0, "scheduled patient has a bed");
            check(patient.getAssignedStaffId() >= 0, "scheduled patient has assigned surgeon");
            Hospital hospital = hospitals.stream().filter(h -> h.getId() == patient.getAssignedHospitalId()).findFirst().get();
            Bed bed = hospital.getBeds().stream().filter(b -> b.getId() == patient.getAssignedBedId()).findFirst().get();
            check(bed.getOccupiedFrom() == patient.getScheduledStartTime(), "bed reservation starts at actual surgery time");
            check(bed.getOccupiedUntil() == patient.getScheduledStartTime() + patient.getSurgeryDuration() + 1440,
                    "bed reservation ends 24 hours after actual surgery end");
            if (patient.requiresICU()) check(bed.getType() == Bed.BedType.ICU, "ICU patient receives ICU bed");
        }
        for (Hospital hospital : hospitals) {
            List<Integer> assignedBedIds = patients.stream()
                    .filter(p -> p.isScheduled() && p.getAssignedHospitalId() == hospital.getId())
                    .map(Patient::getAssignedBedId).collect(java.util.stream.Collectors.toList());
            check(assignedBedIds.size() == new java.util.HashSet<>(assignedBedIds).size(),
                    "concurrent scheduled cases receive distinct beds");
        }
    }

    private static void testBedAssignmentFailureRollsBackReservations() {
        Hospital hospital = new Hospital(1, "Rollback", "test", 0, 0);
        Bed bed = new Bed(1, 1, Bed.BedType.GENERAL);
        hospital.addBed(bed);
        OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
        hospital.addOperatingRoom(or);
        or.scheduleSlot(480, 1000, 999); // forces the case to a start inside the bed's occupied interval
        Staff surgeon = new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                Arrays.asList(Patient.SurgeryType.GENERAL), 480, 1200);
        hospital.addStaff(surgeon);
        bed.assignPatient(999, 1000, 2200);
        Patient patient = patient(1, Patient.SurgeryType.GENERAL, false, 1, 60);
        runQuietly(() -> SchedulingEngine.runFullPipeline(Arrays.asList(patient), Arrays.asList(hospital)));
        check(!patient.isScheduled(), "failed bed assignment leaves patient unscheduled");
        check(or.getScheduledSlots().size() == 1 && or.getScheduledSlots().get(0)[2] == 999,
                "failed bed assignment rolls back only the patient's OR reservation");
        check(surgeon.getAssignedSlots().isEmpty(), "failed bed assignment rolls back staff reservation");
    }

    private static void testManualDischargeKeepsProcedureHistory() {
        List<Hospital> hospitals = SampleData.getHospitals();
        List<Patient> patients = SampleData.getPatients();
        runPipelineQuietly(patients, hospitals);
        Patient patient = patients.stream().filter(p -> p.isScheduled() && p.getAssignedBedId() >= 0).findFirst().get();
        int finish = patient.getScheduledStartTime() + patient.getSurgeryDuration();
        Hospital hospital = hospitals.stream().filter(h -> h.getId() == patient.getAssignedHospitalId()).findFirst().get();
        Bed bed = hospital.getBeds().stream().filter(b -> b.getId() == patient.getAssignedBedId()).findFirst().get();
        boolean slotBefore = hasPatientSlot(hospitals, patient.getId());
        String early = String.format("%d\n%02d:%02d\n", patient.getId(),
                Math.max(0, finish - 1) / 60, Math.max(0, finish - 1) % 60);
        expectIllegalQuietly(() -> SchedulingEngine.dischargePatient(patients, hospitals, new Scanner(early)),
                "discharge before surgery completion rejected");
        check(bed.isOccupied(), "rejected discharge leaves bed reserved");
        String valid = String.format("%d\n%02d:%02d\n", patient.getId(), finish / 60, finish % 60);
        runQuietly(() -> SchedulingEngine.dischargePatient(patients, hospitals, new Scanner(valid)));
        check(patient.isDischarged() && !bed.isOccupied(), "manual discharge releases the assigned bed");
        check(slotBefore && hasPatientSlot(hospitals, patient.getId()), "manual discharge preserves OR history");
        check(patient.isScheduled(), "discharged operation remains in throughput count");
    }

    private static void testEmergencyValidationAndStartedWork() {
        check(SchedulingEngine.parseClockTime("14:30") == 870, "HH:mm conversion");
        expectIllegal(() -> SchedulingEngine.parseClockTime("24:00"), "invalid clock hour rejected");
        List<Hospital> hospitals = SampleData.getHospitals();
        List<Patient> patients = SampleData.getPatients();
        runPipelineQuietly(patients, hospitals);
        Patient started = patients.stream().filter(Patient::isScheduled).findFirst().get();
        int start = started.getScheduledStartTime();
        String when = String.format("%02d:%02d", start / 60, start % 60);
        int before = countSlots(hospitals);
        expectIllegalQuietly(() -> SchedulingEngine.handleEmergency(patients, hospitals,
                new Scanner("Bad Patient\nnot-a-surgery\n30\nfalse\n09:00\n")), "invalid emergency surgery rejected");
        check(countSlots(hospitals) == before, "invalid emergency does not mutate schedules");
        expectIllegalQuietly(() -> SchedulingEngine.handleEmergency(patients, hospitals,
                new Scanner("Bad Patient\nGENERAL\n0\nfalse\n09:00\n")), "nonpositive emergency duration rejected");
        expectIllegalQuietly(() -> SchedulingEngine.handleEmergency(patients, hospitals,
                new Scanner("Bad Patient\nGENERAL\n30\nyes\n09:00\n")), "invalid ICU boolean rejected");
        expectIllegalQuietly(() -> SchedulingEngine.handleEmergency(patients, hospitals,
                new Scanner("Bad Patient\nGENERAL\n30\nfalse\n25:00\n")), "invalid emergency clock time rejected");

        // Set a point just after a selected case started, then ensure that case is preserved.
        int now = Math.max(480, start + 1);
        String input = String.format("Emergency Case\nGENERAL\n30\nfalse\n%02d:%02d\n", now / 60, now % 60);
        runQuietly(() -> SchedulingEngine.handleEmergency(patients, hospitals, new Scanner(input)));
        boolean originalStillPresent = hospitals.stream().flatMap(h -> h.getOperatingRooms().stream())
                .flatMap(or -> or.getScheduledSlots().stream()).anyMatch(slot -> slot[2] == started.getId());
        check(originalStillPresent, "emergency rescheduling preserves already-started operation");
        check(countSlots(hospitals) == (int) patients.stream().filter(Patient::isScheduled).count(),
                "emergency reschedule preserves schedule state invariant");
    }

    private static void testEmergencyPlanningStartBound() {
        Hospital hospital = new Hospital(1, "Emergency", "test", 0, 0);
        hospital.addBed(new Bed(1, 1, Bed.BedType.GENERAL));
        OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
        hospital.addOperatingRoom(or);
        Staff surgeon = new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                Arrays.asList(Patient.SurgeryType.GENERAL), 0, 1440);
        hospital.addStaff(surgeon);
        or.scheduleSlot(600, 630, 90);
        or.scheduleSlot(690, 720, 91);
        or.scheduleSlot(780, 810, 92);
        Patient started = patient(1, Patient.SurgeryType.GENERAL, false, 1, 20);
        started.setScheduled(true); started.setAssignedHospitalId(1); started.setAssignedORId(1);
        started.setAssignedStaffId(1); started.setScheduledStartTime(840); // 14:00, already started
        or.scheduleSlot(840, 860, started.getId()); surgeon.assignSlot(840, 860, started.getId());
        Bed postStartBed = new Bed(2, 1, Bed.BedType.GENERAL);
        hospital.addBed(postStartBed); postStartBed.assignPatient(started.getId(), 840, 2300);
        Patient pending = patient(2, Patient.SurgeryType.GENERAL, false, 1, 30);
        pending.setArrivalTime(480);
        Patient late = patient(3, Patient.SurgeryType.GENERAL, false, 1, 30);
        late.setArrivalTime(900);
        List<DPScheduler.Job> jobs = DPScheduler.schedulePatients(Arrays.asList(pending, late), or,
                Arrays.asList(surgeon), 480, 1200, 870);
        check(jobs.size() == 2 && jobs.stream().allMatch(job -> job.startTime >= 870),
                "emergency planning bound prevents using free 10:00, 11:30, and 13:00 slots");
        check(jobs.stream().filter(job -> job.patient == late).allMatch(job -> job.startTime >= 900),
                "later patient arrival remains a lower bound");
        List<DPScheduler.Job> equalStart = DPScheduler.schedulePatients(
                Arrays.asList(patient(4, Patient.SurgeryType.GENERAL, false, 1, 30)), or,
                Arrays.asList(surgeon), 480, 1200, 870);
        check(equalStart.get(0).startTime >= 870,
                "a surgery starting exactly at emergency time is treated as pending");
        check(started.getScheduledStartTime() == 840 && hasPatientSlot(Arrays.asList(hospital), started.getId()),
                "surgery already started before emergency remains unchanged");
        List<Patient> larger = new ArrayList<>();
        for (int i = 0; i < DPScheduler.MAX_EXACT_DP_PATIENTS + 1; i++)
            larger.add(patient(20 + i, Patient.SurgeryType.GENERAL, false, 1, 20 + i));
        List<DPScheduler.Job> fallback = DPScheduler.schedulePatients(larger, or,
                Arrays.asList(surgeon), 480, 1200, 870);
        check(!fallback.isEmpty() && fallback.stream().allMatch(job -> job.startTime >= 870),
                "large-workload candidate fallback enforces emergency planning bound");
    }

    private static void testEmergencyPipelinePlanningStart() {
        Hospital hospital = new Hospital(1, "Emergency pipeline", "test", 0, 0);
        for (int i = 1; i <= 4; i++) hospital.addBed(new Bed(i, 1, Bed.BedType.GENERAL));
        OperatingRoom or = new OperatingRoom(1, 1, "OR", Arrays.asList(Patient.SurgeryType.GENERAL));
        hospital.addOperatingRoom(or);
        Staff surgeon = new Staff(1, "Surgeon", Staff.Role.SURGEON, 1,
                Arrays.asList(Patient.SurgeryType.GENERAL), 480, 1200);
        hospital.addStaff(surgeon);
        Patient alreadyStarted = patient(1, Patient.SurgeryType.GENERAL, false, 1, 30);
        alreadyStarted.setScheduled(true); alreadyStarted.setAssignedHospitalId(1);
        alreadyStarted.setAssignedORId(1); alreadyStarted.setAssignedStaffId(1);
        alreadyStarted.setScheduledStartTime(840);
        or.scheduleSlot(840, 870, 1); surgeon.assignSlot(840, 870, 1);
        hospital.getBeds().get(0).assignPatient(1, 840, 2310);
        Patient beginsAtArrival = patient(2, Patient.SurgeryType.GENERAL, false, 1, 30);
        beginsAtArrival.setScheduled(true); beginsAtArrival.setAssignedHospitalId(1);
        beginsAtArrival.setAssignedORId(1); beginsAtArrival.setAssignedStaffId(1);
        beginsAtArrival.setScheduledStartTime(870);
        or.scheduleSlot(870, 900, 2); surgeon.assignSlot(870, 900, 2);
        hospital.getBeds().get(1).assignPatient(2, 870, 2340);
        Patient laterArrival = patient(3, Patient.SurgeryType.GENERAL, false, 1, 30);
        laterArrival.setArrivalTime(930);
        List<Patient> patients = new ArrayList<>(Arrays.asList(alreadyStarted, beginsAtArrival, laterArrival));
        runQuietly(() -> SchedulingEngine.processEmergency(patients, Arrays.asList(hospital), "New emergency",
                Patient.SurgeryType.GENERAL, 30, false, 870));
        check(alreadyStarted.getScheduledStartTime() == 840 && hasPatientSlot(Arrays.asList(hospital), 1),
                "pipeline keeps surgery that started before emergency arrival unchanged");
        check(beginsAtArrival.isScheduled() && beginsAtArrival.getScheduledStartTime() >= 870,
                "pipeline reschedules case beginning exactly at emergency arrival");
        check(laterArrival.isScheduled() && laterArrival.getScheduledStartTime() >= 930,
                "pipeline applies original later patient arrival after emergency");
        check(patients.stream().filter(p -> p.getId() != 1 && p.isScheduled())
                        .allMatch(p -> p.getScheduledStartTime() >= 870),
                "full emergency pipeline assigns no pending surgery before planning start");
    }

    private static Hospital hospital(int id, double lat, double lon, Bed.BedType type) {
        Hospital hospital = new Hospital(id, "H" + id, "test", lat, lon);
        hospital.addBed(new Bed(id, id, type));
        return hospital;
    }

    private static void addGeneralSurgery(Hospital hospital, int id) {
        hospital.addOperatingRoom(new OperatingRoom(id, hospital.getId(), "OR" + id,
                Arrays.asList(Patient.SurgeryType.GENERAL)));
        hospital.addStaff(new Staff(id, "Surgeon" + id, Staff.Role.SURGEON, hospital.getId(),
                Arrays.asList(Patient.SurgeryType.GENERAL), 0, 1440));
    }

    private static Patient patient(int id, Patient.SurgeryType type, boolean icu, int preferred, int duration) {
        return new Patient(id, "P" + id, 35, Patient.Priority.URGENT, type, duration, 480, preferred, icu);
    }

    private static int countSlots(List<Hospital> hospitals) {
        return hospitals.stream().flatMap(h -> h.getOperatingRooms().stream())
                .mapToInt(or -> or.getScheduledSlots().size()).sum();
    }

    private static boolean hasPatientSlot(List<Hospital> hospitals, int patientId) {
        return hospitals.stream().flatMap(h -> h.getOperatingRooms().stream())
                .flatMap(or -> or.getScheduledSlots().stream()).anyMatch(slot -> slot[2] == patientId);
    }

    private static int countAssigned(int[] values) {
        int result = 0;
        for (int value : values) if (value >= 0) result++;
        return result;
    }

    private static int totalCost(double[][] matrix, int[] assignment) {
        int total = 0;
        for (int row = 0; row < assignment.length; row++) if (assignment[row] >= 0) total += (int) matrix[row][assignment[row]];
        return total;
    }

    private static void runPipelineQuietly(List<Patient> patients, List<Hospital> hospitals) {
        runQuietly(() -> {
            for (Hospital hospital : hospitals) {
                hospital.getOperatingRooms().forEach(OperatingRoom::clearSchedule);
                hospital.getStaffMembers().forEach(Staff::clearAssignments);
                hospital.getBeds().forEach(Bed::release);
            }
            for (Patient patient : patients) patient.resetSchedule();
            SchedulingEngine.runFullPipeline(patients, hospitals);
        });
    }

    private static void runQuietly(Runnable action) {
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            action.run();
        } finally {
            System.setOut(old);
        }
    }

    private static void expectIllegal(Runnable action, String name) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { check(true, name); return; }
        throw new AssertionError("Expected IllegalArgumentException: " + name);
    }

    private static void expectIllegalQuietly(Runnable action, String name) {
        runQuietly(() -> expectIllegal(action, name));
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++;
    }
}
