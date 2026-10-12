import reports.ReportGenerator;
import models.Hospital;
import models.Patient;
import data.SampleData;
import scheduling.SchedulingEngine;
import models.Staff;
import algorithms.KMPAlgorithm;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing Hospital Bed and Operating Room Scheduling System...");
        List<Hospital> hospitals = SampleData.getHospitals();
        List<Patient> patients = SampleData.getPatients();
        Scanner scanner = new Scanner(System.in);

        boolean running = true;
        while (running) {
            System.out.println("\n============= MAIN MENU =============");
            System.out.println("1. View Hospitals & Capacity");
            System.out.println("2. View Patients");
            System.out.println("3. Run Full Scheduling Pipeline");
            System.out.println("4. Handle Emergency Intake");
            System.out.println("5. View KPI Reports");
            System.out.println("6. View Generated Schedules");
            System.out.println("7. Search Name (KMP Algorithm)");
            System.out.println("8. Discharge Patient");
            System.out.println("0. Exit");
            System.out.print("Choice: ");

            String input = scanner.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (Exception e) {
                System.out.println("Invalid input.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.println("\n--- Hospitals ---");
                    for (Hospital h : hospitals) System.out.println(h.toString());
                    break;
                case 2:
                    System.out.println("\n--- Pending & Scheduled Patients ---");
                    for (Patient p : patients) System.out.println(p.toString());
                    break;
                case 3:
                    // Reset schedules before full re-run
                    for (Patient p : patients) p.resetSchedule();
                    for (Hospital h : hospitals) {
                        h.getOperatingRooms().forEach(models.OperatingRoom::clearSchedule);
                        h.getStaffMembers().forEach(models.Staff::clearAssignments);
                        h.getBeds().forEach(models.Bed::release);
                    }
                    SchedulingEngine.runFullPipeline(patients, hospitals);
                    break;
                case 4:
                    try {
                        SchedulingEngine.handleEmergency(patients, hospitals, scanner);
                    } catch (Exception e) {
                        System.out.println("Error processing emergency input: " + e.getMessage());
                    }
                    break;
                case 5:
                    ReportGenerator.printOverallKPIs(hospitals, patients);
                    break;
                case 6:
                    ReportGenerator.printSchedules(hospitals, patients);
                    break;
                case 7:
                    System.out.print("Enter name to search (Patient/Staff): ");
                    String query = scanner.nextLine();
                    System.out.println("\n--- Search Results ---");
                    boolean found = false;
                    for (Patient p : patients) {
                        if (KMPAlgorithm.search(p.getName(), query)) {
                            System.out.println("[Patient Match] " + p.toString());
                            found = true;
                        }
                    }
                    for (Hospital h : hospitals) {
                        for (Staff s : h.getStaffMembers()) {
                            if (KMPAlgorithm.search(s.getName(), query)) {
                                System.out.println("[Staff Match] " + s.toString());
                                found = true;
                            }
                        }
                    }
                    if (!found) System.out.println("No matching records found.");
                    break;
                case 8:
                    try {
                        SchedulingEngine.dischargePatient(patients, hospitals, scanner);
                    } catch (Exception e) {
                        System.out.println("Error processing discharge: " + e.getMessage());
                    }
                    break;
                case 0:
                    running = false;
                    System.out.println("Shutting down...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
        scanner.close();
    }
}
