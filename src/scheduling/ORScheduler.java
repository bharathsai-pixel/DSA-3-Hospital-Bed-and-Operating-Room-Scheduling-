package scheduling;

import algorithms.DPScheduler;
import models.OperatingRoom;
import models.Patient;
import models.Staff;

import java.util.List;

public class ORScheduler {
    /** Schedules procedures using subset DP, existing OR bookings, waiting cost, and surgeon shifts. */
    public static void scheduleOR(OperatingRoom or, List<Patient> patients, List<Staff> staff,
                                  int earliestStart, int shiftEnd) {
        scheduleOR(or, patients, staff, earliestStart, shiftEnd, earliestStart);
    }

    public static void scheduleOR(OperatingRoom or, List<Patient> patients, List<Staff> staff,
                                  int earliestStart, int shiftEnd, int planningStart) {
        List<DPScheduler.Job> jobs = DPScheduler.schedulePatients(patients, or, staff,
                earliestStart, shiftEnd, planningStart);
        for (DPScheduler.Job job : jobs) {
            Patient patient = job.patient;
            Staff surgeon = staff.stream()
                    .filter(member -> member.getRole() == Staff.Role.SURGEON
                            && member.canPerformSurgery(patient.getSurgeryType())
                            && member.isAvailableDuring(job.startTime, job.endTime))
                    .findFirst().orElse(null);
            if (surgeon == null) continue;

            or.scheduleSlot(job.startTime, job.endTime, patient.getId());
            surgeon.assignSlot(job.startTime, job.endTime, patient.getId());
            patient.setScheduled(true);
            patient.setAssignedORId(or.getId());
            patient.setAssignedStaffId(surgeon.getId());
            patient.setScheduledStartTime(job.startTime);
            patient.setWaitingTime(job.startTime - patient.getArrivalTime());
        }
    }
}
