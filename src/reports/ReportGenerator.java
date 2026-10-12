package reports;

import models.Hospital;
import models.OperatingRoom;
import models.Patient;
import models.Staff;

import java.util.List;

public class ReportGenerator {

    public static void printOverallKPIs(List<Hospital> hospitals, List<Patient> patients) {
        System.out.println("\n============ SYSTEM KPI REPORT ============");

        int totalPatients = patients.size();
        int scheduledPatients = 0;
        int totalWaitTime = 0;
        int latestEndTime = 0;

        for (Patient p : patients) {
            if (p.isScheduled()) {
                scheduledPatients++;
                totalWaitTime += p.getWaitingTime();
                int endTime = p.getScheduledStartTime() + p.getSurgeryDuration();
                if (endTime > latestEndTime) {
                    latestEndTime = endTime;
                }
            }
        }

        System.out.println("Processing Rate: " + scheduledPatients + "/" + totalPatients + " patients scheduled ("
                + (totalPatients == 0 ? 0 : (scheduledPatients * 100 / totalPatients)) + "%)");

        if (scheduledPatients > 0) {
            System.out.println("Avg Waiting Time: " + (totalWaitTime / scheduledPatients) + " minutes");
            System.out.println("Makespan (Latest completion): " + formatTime(latestEndTime));
        }

        System.out.println("\n--- Hospital Resource Utilization ---");
        for (Hospital h : hospitals) {
            System.out.println(h.getName() + ":");
            System.out.printf("  Bed Reservations: %.1f%%\n", h.getBedReservationRate());

            System.out.print("  OR Utilization: ");
            for (OperatingRoom or : h.getOperatingRooms()) {
                System.out.printf("[%s: %.1f%%] ", or.getName(), or.getUtilization(480, 1200)); // 8am to 8pm
            }
            System.out.println();
        }
        System.out.println("===========================================\n");
    }

    public static void printSchedules(List<Hospital> hospitals, List<Patient> patients) {
        System.out.println("\n============ PATIENT SCHEDULES ============");
        for (Hospital h : hospitals) {
            System.out.println("\n[" + h.getName() + "]");
            for (OperatingRoom or : h.getOperatingRooms()) {
                if (or.getScheduledSlots().isEmpty()) continue;
                System.out.println("  " + or.getName() + " Schedule:");
                for (int[] slot : or.getScheduledSlots()) {
                    Patient mappedP = patients.stream().filter(p -> p.getId() == slot[2]).findFirst().orElse(null);
                    if (mappedP != null) {
                        Staff surgeon = h.getStaffMembers().stream()
                                .filter(s -> s.getId() == mappedP.getAssignedStaffId())
                                .findFirst().orElse(null);
                        System.out.printf("    %s - %s : Patient P%d (%s, %s; Surgeon: %s)\n",
                                formatTime(slot[0]), formatTime(slot[1]),
                                mappedP.getId(), mappedP.getName(), mappedP.getPriority().getLabel(),
                                surgeon == null ? "Unassigned" : surgeon.getName());
                    }
                }
            }
        }
        System.out.println("===========================================\n");
    }

    private static String formatTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        return String.format("%d:%02d %s", hours, mins, ampm);
    }
}
