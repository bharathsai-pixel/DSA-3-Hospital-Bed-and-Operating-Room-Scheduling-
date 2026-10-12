package scheduling;

import algorithms.HungarianAlgorithm;
import models.Bed;
import models.Hospital;
import models.Patient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reserves beds for the complete interval from surgery start through 24h post-op. */
public class BedAssigner {
    public static void assignBeds(Hospital hospital, List<Patient> patientsToAssign, int ignoredDefaultTime) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : patientsToAssign)
            if (patient.isScheduled() && patient.getAssignedBedId() == -1) patients.add(patient);
        patients.sort(Comparator.comparingInt(Patient::getScheduledStartTime));
        if (patients.isEmpty() || hospital.getBeds().isEmpty()) return;

        List<Bed> beds = hospital.getBeds();
        double[][] costs = new double[patients.size()][beds.size()];
        for (int i = 0; i < patients.size(); i++) {
            Patient patient = patients.get(i);
            int from = patient.getScheduledStartTime();
            int until = from + patient.getSurgeryDuration() + 1440;
            for (int j = 0; j < beds.size(); j++) {
                Bed bed = beds.get(j);
                boolean icuMismatch = patient.requiresICU() && bed.getType() != Bed.BedType.ICU;
                boolean timeConflict = !bed.isAvailableDuring(from, until);
                if (icuMismatch || timeConflict) {
                    costs[i][j] = Double.POSITIVE_INFINITY;
                } else {
                    double cost = patient.getPriority().getLevel() * 100.0 - patient.getWaitingTime();
                    if (!patient.requiresICU() && bed.getType() == Bed.BedType.ICU) cost += 1000;
                    costs[i][j] = Math.max(0, cost);
                }
            }
        }
        int[] assignments = new HungarianAlgorithm(costs).execute();
        for (int i = 0; i < assignments.length; i++) {
            int bedIndex = assignments[i];
            if (bedIndex < 0) continue;
            Patient patient = patients.get(i);
            Bed bed = beds.get(bedIndex);
            int start = patient.getScheduledStartTime();
            int until = start + patient.getSurgeryDuration() + 1440;
            patient.setAssignedBedId(bed.getId());
            patient.setAssignedHospitalId(hospital.getId());
            bed.assignPatient(patient.getId(), start, until);
        }
    }
}
