package algorithms;

import models.Patient;
import models.OperatingRoom;

import java.util.*;

/**
 * Approximation Algorithm implementation (LPT - Longest Processing Time).
 * Solves the multiprocessor scheduling problem (NP-Hard) to minimize makespan
 * across multiple Operating Rooms.
 */
public class ApproxScheduler {

    /**
     * Distributes surgeries across available ORs to minimize the maximum completion time (makespan).
     * Works well provided the ORs support the necessary surgery types.
     * Returns a map of OR ID -> List of assigned Patients.
     */
    public static Map<Integer, List<Patient>> balanceLoad(List<Patient> patients, List<OperatingRoom> ors) {
        // Use Module 6: Randomised Quicksort to sort patients descending by surgery duration (Longest Processing Time first)
        RandomizedQuickSort.sortDescendingByDuration(patients);

        // Initialize workloads for each OR
        Map<Integer, Integer> workloads = new HashMap<>();
        Map<Integer, List<Patient>> assignments = new HashMap<>();

        for (OperatingRoom or : ors) {
            workloads.put(or.getId(), 0);
            assignments.put(or.getId(), new ArrayList<>());
        }

        for (Patient p : patients) {
            // Find the OR with the minimum current workload that supports the surgery
            OperatingRoom bestOr = null;
            int minWorkload = Integer.MAX_VALUE;

            for (OperatingRoom or : ors) {
                if (or.supportsSurgery(p.getSurgeryType())) {
                    int currentLoad = workloads.get(or.getId());
                    if (currentLoad < minWorkload) {
                        minWorkload = currentLoad;
                        bestOr = or;
                    }
                }
            }

            // Assign to best OR
            if (bestOr != null) {
                assignments.get(bestOr.getId()).add(p);
                workloads.put(bestOr.getId(), minWorkload + p.getSurgeryDuration());
            } else {
                // If it can't be assigned safely fall back (or handle differently)
                System.out.println("Warning: Could not assign Patient P" + p.getId() + " - No OR supports " + p.getSurgeryType());
            }
        }

        return assignments;
    }
}
